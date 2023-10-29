package com.gallen.team.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gallen.common.enums.AppHttpCodeEnum;
import com.gallen.common.enums.TeamStatusEnum;

import com.gallen.dtos.team.TeamJoinRequest;
import com.gallen.dtos.team.TeamQuitRequest;
import com.gallen.dtos.team.TeamUpdateRequest;
import com.gallen.pojos.team.UserTeam;
import com.gallen.pojos.user.User;
import com.gallen.team.exception.CustomException;
import com.gallen.team.service.UserTeamService;
import com.gallen.api.user.InnerUserInterface;

import com.gallen.dtos.team.TeamQuery;
import com.gallen.pojos.team.Team;
import com.gallen.team.mapper.TeamMapper;
import com.gallen.team.service.TeamService;
import com.gallen.vos.team.TeamUserVO;
import com.gallen.vos.user.SafetyUser;
import org.apache.commons.lang3.StringUtils;


import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;



@Service
public class TeamServiceImpl extends ServiceImpl<TeamMapper, Team>
    implements TeamService {
    @Resource
    private UserTeamService userTeamService;

    @DubboReference
    private InnerUserInterface innerUserInterface;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 创建队伍
     *
     * @param team
     * @param loginUser
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public long addTeam(Team team, User loginUser) {
        //1. 请求参数是否为空？
        if (team == null) {
            throw new CustomException(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        //2. 是否登录，不登陆不允许创建
        if (loginUser == null) {
            throw new CustomException(AppHttpCodeEnum.NEED_LOGIN);
        }
        final long userId = loginUser.getId();
        //3. 校验信息
        //  a. 队伍人数> 1 且 <= 20
        if (team.getMaxNum() < 1 || team.getMaxNum() > 20) {
            throw new CustomException(AppHttpCodeEnum.PARAM_INVALID);
        }
        //  b. 队伍标题 <= 20
        String name = team.getName();
        if (StringUtils.isBlank(name) || name.length() > 20) {
            throw new CustomException(AppHttpCodeEnum.PARAM_INVALID);
        }
        //  c. 描述<=512
        String description = team.getDescription();
        if (StringUtils.isBlank(description) && description.length() > 512) {
            throw new CustomException(AppHttpCodeEnum.PARAM_INVALID);
        }
        //  d. status是否公开，默认为0(公开)
        int status = Optional.ofNullable(team.getStatus()).orElse(0);
        TeamStatusEnum statusEnum = TeamStatusEnum.getEnumByValue(status);
        if (statusEnum == null) {
            throw new CustomException(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        //  e. 如果status是加密，一定要有密码，且密码 <= 32
        String password = team.getPassword();
        if (TeamStatusEnum.SECRET.equals(statusEnum)) {
            if (StringUtils.isBlank(password) || password.length() > 32) {
                throw new CustomException(AppHttpCodeEnum.PARAM_INVALID);
            }
        }
        //  f. 超时时间 > 当前时间
        if (new Date().after(team.getExpireTime())) {
            throw new CustomException(AppHttpCodeEnum.PARAM_INVALID);
        }
        //  g. 校验用户最多创建5个队伍
        // todo 有bug，可能同时创建100个队伍
//        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
//        queryWrapper.eq("user_id", userId);
//        long hasTeamNum = this.count(queryWrapper);
//        if (hasTeamNum >= 5) {
//            throw new CustomException(AppHttpCodeEnum.PARAM_INVALID);
//        }

        //4. 插入队伍信息到队伍表
        team.setId(null);
        team.setUserId(userId);
        boolean result = this.save(team);
        if (!result) {
            throw new CustomException(AppHttpCodeEnum.SERVER_ERROR);
        }
        Long teamId = team.getId();
        //5. 插入用户 => 队伍关系到关系表
        UserTeam userTeam = new UserTeam();
        userTeam.setUserId(userId);
        userTeam.setTeamId(teamId);
        userTeam.setJoinTime(new Date());
        result = userTeamService.save(userTeam);
        if (!result) {
            throw new CustomException(AppHttpCodeEnum.SERVER_ERROR);
        }
        return teamId;
    }

    /**
     * 搜索查询队伍列表
     *
     * @param teamQuery
     * @param isAdmin
     * @return
     */
    @Override
    public List<TeamUserVO> listTeam(TeamQuery teamQuery, boolean isAdmin) {
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        // 组合查询条件
        if (teamQuery != null) {
            Long id = teamQuery.getId();
            if (id != null && id > 0) {
                queryWrapper.eq("id", id);
            }
            String name = teamQuery.getName();

            List<Long> teamIdList = teamQuery.getTeamIdList();
            if (CollectionUtils.isNotEmpty(teamIdList) && teamIdList.size() > 0) {
                queryWrapper.in("id", teamIdList);
            }
            if (StringUtils.isNotBlank(name)) {
                queryWrapper.like("name", name);
            }
            String description = teamQuery.getDescription();
            if (StringUtils.isNotBlank(description)) {
                queryWrapper.like("description", description);
            }
            String searchText = teamQuery.getSearchText();
            if (StringUtils.isNotBlank(searchText)) {
                queryWrapper.and(qw -> qw.like("name", searchText).or().like("description", searchText));
            }
            Integer maxNum = teamQuery.getMaxNum();
            if (maxNum != null && maxNum > 0) {
                queryWrapper.eq("max_num", maxNum);
            }
            Long userid = teamQuery.getUserid();
            if (userid != null && userid > 0) {
                queryWrapper.eq("user_id", userid);
            }
            Integer status = teamQuery.getStatus();
            // 利用枚举类判断是否查询公开的
            TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
            if (teamStatusEnum == null) {
                teamStatusEnum = TeamStatusEnum.PUBLIC;
            }
            // 只有管理员才可以查询非公开的队伍,否则报无权限异常
            if (!isAdmin && !TeamStatusEnum.PUBLIC.equals(teamStatusEnum)) {
                throw new CustomException(AppHttpCodeEnum.NO_OPERATOR_AUTH);
            }
            if (status != null && status > -1) {
                queryWrapper.eq("status", teamStatusEnum.getValue());
            }
        }

        // 不查询已经过期的队伍
        queryWrapper.and(qw -> qw.isNull("expire_time").or().gt("expire_time", new Date()));
        List<Team> teamList = this.list(queryWrapper);
        if (CollectionUtils.isEmpty(teamList)) {
            return new ArrayList<>();
        }
        // 关联查询用户信息
        // 1.自己写SQL
        // 查询队伍和创建人的信息
        // select * from team t left join user u on t.userId = u.id
        // 查询队伍和已加入队伍的成员信息
        // select * from team t
        //  left join user_team ut on t.id = ut.team_id
        //  left join user u on ut.userId= u.id


        // 2.通过代码实现关联查询
        // 关联查询队伍创建者信息
        List<TeamUserVO> teamUserVOList = new ArrayList<>();
        for (Team team : teamList) {
            Long userid = team.getUserId();
            User user = innerUserInterface.getById(userid);
            TeamUserVO teamUserVO = new TeamUserVO();
            // 对用户信息进行脱敏
            SafetyUser safetyUser = innerUserInterface.getSafetyUser(user);
//            if (safetyUser != null) {
//                SafetyUser userVO = new SafetyUser();
//                BeanUtils.copyProperties(safetyUser, userVO);
//                teamUserVO.setCreateUser(userVO);
//            }

            // 对队伍信息进行脱敏
            BeanUtils.copyProperties(team, teamUserVO);
//            teamUserVO.setMaxNum(team.getMaxNum());
//            teamUserVO.setExpireTime(team.getExpireTime());
//            teamUserVO.setUserid(team.getUserId());
//            teamUserVO.setCreateTime(team.getCreateTime());
//            teamUserVO.setUpdateTime(team.getUpdateTime());
//            teamUserVO.setCreateUser(safetyUser);

            teamUserVOList.add(teamUserVO);
        }


        return teamUserVOList;
    }

    @Override
    public boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser) {
        // 判断请求数据是否为空
        if (teamUpdateRequest == null) {
            throw new CustomException(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        // 获取队伍id
        Long teamId = teamUpdateRequest.getId();
        if (teamId == null || teamId < 0) {
            throw new CustomException(AppHttpCodeEnum.PARAM_INVALID);
        }
        // 根据teamId 查询队伍老信息
        Team teamGetById = this.getById(teamId);
        if (teamGetById == null) {
            throw new CustomException(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        // 只有管理员或者队伍的创建者可以修改，否则抛异常
        Long loginUserId = loginUser.getId();
        Long createUserId = teamGetById.getUserId();
        if (!loginUserId.equals(createUserId) && !innerUserInterface.isAdmin(loginUser)) {
            throw new CustomException(AppHttpCodeEnum.NEED_ADMIND);
        }

        // 如果用户传入的新值和老值一致，就不要update了(降低数据库使用次数)
        if (teamUpdateRequest.getName().equals(teamGetById.getName()) &&
                teamUpdateRequest.getDescription().equals(teamGetById.getDescription()) &&
                teamUpdateRequest.getMaxNum().equals(teamGetById.getMaxNum()) &&
                teamUpdateRequest.getExpireTime().equals(teamGetById.getExpireTime()) &&
                teamUpdateRequest.getPassword().equals(teamGetById.getPassword()) &&
                teamUpdateRequest.getStatus().equals(teamGetById.getStatus())) {
            return true;
        }

        // 如果队伍状态改为加密，必须要有密码
        Integer teamStatus = teamUpdateRequest.getStatus();
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(teamStatus);
        if (teamStatusEnum.equals(TeamStatusEnum.SECRET)) {
            if (StringUtils.isBlank(teamUpdateRequest.getPassword())) {
                throw new CustomException(AppHttpCodeEnum.PARAM_REQUIRE);
            }
        }

        // 执行更新
        Team team = new Team();
        BeanUtils.copyProperties(teamUpdateRequest, team);
        return this.updateById(team);
    }

    @Override
    public boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser) {
        if (teamJoinRequest == null) {
            throw new CustomException(AppHttpCodeEnum.PARAM_REQUIRE);
        }

        Long teamId = teamJoinRequest.getId();
        Team team = this.getById(teamId);
        if (team == null) {
            throw new CustomException(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        // 用户只能加入未过期的队伍
        Date expireTime = team.getExpireTime();
        if (expireTime == null && expireTime.before(new Date())) {
            throw new CustomException(AppHttpCodeEnum.PARAM_INVALID);
        }

        // 禁止加入私人队伍
        Integer status = team.getStatus();
        TeamStatusEnum teamStatusEnum = TeamStatusEnum.getEnumByValue(status);
        if (TeamStatusEnum.PRIVATE.equals(teamStatusEnum)) {
            throw new CustomException(AppHttpCodeEnum.PARAM_INVALID);
        }

        // 如果加入的队伍是加密的，必须密码匹配才可以加入
        if (TeamStatusEnum.SECRET.equals(teamStatusEnum)) {
            String password = teamJoinRequest.getPassword();
            if (StringUtils.isBlank(password) || !password.equals(team.getPassword())) {
                throw new CustomException(AppHttpCodeEnum.PASSWORD_ERROR);
            }
        }

        RLock lock = redissonClient.getLock("cc:team:join" + loginUser.getId() + ":" + teamId);
        try {
            while(true){
                if(lock.tryLock(0, -1, TimeUnit.MILLISECONDS)){
                    // 用户只能加入未满的队伍
                    QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
                    Integer maxNum = team.getMaxNum();
                    userTeamQueryWrapper.eq("team_id", teamId);
                    long teamHasJoinNum = userTeamService.count(userTeamQueryWrapper);
                    if (teamHasJoinNum >= maxNum) {
                        throw new CustomException(AppHttpCodeEnum.PARAM_INVALID);
                    }

                    Long userId = loginUser.getId();
                    // 校验最多只能加入5个队伍
//                    userTeamQueryWrapper.eq("user_id", userId);
//                    long hasJoinTeamNum = userTeamService.count(userTeamQueryWrapper);
//                    if (hasJoinTeamNum >= 5) {
//                        throw new CustomException(AppHttpCodeEnum.PARAM_INVALID);
//                    }


                    // 不能重复加入已加入的队伍
                    userTeamQueryWrapper = new QueryWrapper<>();
                    userTeamQueryWrapper.eq("team_id", teamId);
                    userTeamQueryWrapper.eq("user_id", userId);
                    long hasUserJoinTeam = userTeamService.count(userTeamQueryWrapper);
                    if (hasUserJoinTeam > 0) {
                        throw new CustomException(AppHttpCodeEnum.PARAM_INVALID);
                    }

                    UserTeam userTeam = new UserTeam();
                    userTeam.setUserId(userId);
                    userTeam.setTeamId(teamId);
                    userTeam.setJoinTime(new Date());
                    return userTeamService.save(userTeam);
                }
            }
        } catch (CustomException ce) {
            throw ce;
        } catch (Exception e) {
            log.error("join team error" + e.getMessage());
            throw new CustomException(AppHttpCodeEnum.PARAM_INVALID);
        } finally {
            // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unlock :" + Thread.currentThread().getId());
                lock.unlock();
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser) {
        if (teamQuitRequest == null) {
            throw new CustomException(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        // 判断队伍是否存在
        Long teamId = teamQuitRequest.getId();
        Team team = this.getById(teamId);
        if (team == null) {
            throw new CustomException(AppHttpCodeEnum.DATA_NOT_EXIST);
        }

        // 判断要退出队伍的用户是否已加入队伍
        Long userId = loginUser.getId();

        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("user_id", userId);
        userTeamQueryWrapper.eq("team_id", teamId);
        long countJoinTeamNum = userTeamService.count(userTeamQueryWrapper);
        if (countJoinTeamNum == 0) {
            throw new CustomException(AppHttpCodeEnum.PARAM_INVALID);
        }

        // 获取队伍加入人数
        userTeamQueryWrapper.clear();
        userTeamQueryWrapper.eq("team_id", teamId);
        long teamMemberNum = userTeamService.count(userTeamQueryWrapper);
        if (teamMemberNum == 1) {
            // 队伍只剩一个人
            // 删除队伍
            return this.removeById(teamId);
        } else {
            // 队伍还剩至少两个人
            // 判断是否是队长
            if (team.getUserId().equals(userId)) {
                // 是队长，将队长交给第二个加入的用户
                // id是自增的，所以查询前两条记录即可
                List<UserTeam> list = userTeamService.list(userTeamQueryWrapper.last("order by id limit 2"));
                if (CollectionUtils.isEmpty(list) || list.size() <= 1) {
                    throw new CustomException(AppHttpCodeEnum.SERVER_ERROR);
                }
                UserTeam userTeam = list.get(1);
                team.setUserId(userTeam.getUserId());
                boolean result = this.updateById(team);
                if (!result) {
                    throw new CustomException(AppHttpCodeEnum.SERVER_ERROR);
                }
            }
        }
        // 将用户从用户-队伍关系中删除
        userTeamQueryWrapper.clear();
        userTeamQueryWrapper.eq("user_id", userId);
        userTeamQueryWrapper.eq("team_id", teamId);
        return userTeamService.remove(userTeamQueryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disbandTeam(long teamId, User loginUser) {
        // 判断队伍是否存在
        Team team = this.getById(teamId);
        if (team == null) {
            throw new CustomException(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        // 判断是否为队长
        Long userId = loginUser.getId();
        if (!userId.equals(team.getUserId())) {
            throw new CustomException(AppHttpCodeEnum.NO_OPERATOR_AUTH);
        }

        // 删除队伍信息
        boolean removeTeamResult = this.removeById(teamId);
        if (!removeTeamResult) {
            throw new CustomException(AppHttpCodeEnum.SERVER_ERROR);
        }

        // 删除用户-队伍关系信息
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("team_id", teamId);
        boolean removeUserTeamResult = userTeamService.remove(userTeamQueryWrapper);
        if (!removeUserTeamResult) {
            throw new CustomException(AppHttpCodeEnum.SERVER_ERROR);
        }
        return removeUserTeamResult;
    }
}




