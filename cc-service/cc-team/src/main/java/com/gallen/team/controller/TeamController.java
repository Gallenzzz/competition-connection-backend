package com.gallen.team.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gallen.api.user.InnerUserInterface;
import com.gallen.common.common.DeleteRequest;
import com.gallen.common.common.ResponseResult;
import com.gallen.common.enums.AppHttpCodeEnum;
import com.gallen.common.exception.CustomException;
import com.gallen.pojos.team.UserTeam;
import com.gallen.pojos.user.User;
import com.gallen.team.service.TeamService;
import com.gallen.team.service.UserTeamService;
import com.gallen.dtos.team.*;
import com.gallen.pojos.team.Team;
import com.gallen.vos.team.TeamUserVO;
import com.gallen.vos.user.SafetyUser;

import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*", allowCredentials = "true")
@RequestMapping("/api/team")
//@DubboComponentScan(basePackages = "com.gallen.service.dubbo")
public class TeamController {

    @DubboReference
    private InnerUserInterface userService;
    @Resource
    private TeamService teamService;

    @Resource
    private UserTeamService userTeamService;
    @PostMapping("add")
    public ResponseResult<Long> addTeam(@RequestBody TeamAddRequest teamAddRequest, HttpServletRequest request){
        if(teamAddRequest == null){
            throw new CustomException(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        // 从请求头中获取token
        String token = request.getHeader("Token");
        User loginUser = userService.getLoginUser(token);
        Team team = new Team();
        BeanUtils.copyProperties(teamAddRequest, team);
        long teamId = teamService.addTeam(team,loginUser);
        return ResponseResult.okResult(teamId);
    }

    @DeleteMapping("delete")
    public ResponseResult<Boolean> deleteTeam(long id){
        if(id <= 0){
            throw new CustomException(AppHttpCodeEnum.PARAM_INVALID);
        }

        boolean success = teamService.removeById(id);
        if(!success){
            throw new CustomException(AppHttpCodeEnum.SERVER_ERROR);
        }
        return ResponseResult.okResult(true);
    }

    @PutMapping("update")
    public ResponseResult<Boolean> updateTeam(@RequestBody TeamUpdateRequest teamUpdateRequest, HttpServletRequest request){
        if(teamUpdateRequest == null){
            throw new CustomException(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        // 从请求头中获取token
        String token = request.getHeader("Token");
        User loginUser = userService.getLoginUser(token);
        boolean success = teamService.updateTeam(teamUpdateRequest, loginUser);
        if(!success){
            throw new CustomException(AppHttpCodeEnum.SERVER_ERROR);
        }
        return ResponseResult.okResult(true);
    }
    @GetMapping("get")
    public ResponseResult<Team> getTeamById(long id){
        if(id <= 0){
            throw new CustomException(AppHttpCodeEnum.PARAM_INVALID);
        }

        Team team = teamService.getById(id);
        if(team == null){
            throw new CustomException(AppHttpCodeEnum.SERVER_ERROR);
        }
        return ResponseResult.okResult(team);
    }

    @GetMapping("list")
    public ResponseResult<List<TeamUserVO>> getTeamList(TeamQuery teamQuery, HttpServletRequest request){
        if(teamQuery == null){
            throw new CustomException(AppHttpCodeEnum.PARAM_INVALID);
        }
        // 从请求头中获取token
        String token = request.getHeader("Token");
        User loginUser = userService.getLoginUser(token);
        boolean isAdmin = userService.isAdmin(loginUser);
        // 1.查询队伍列表
        List<TeamUserVO> teamUserVOList = teamService.listTeam(teamQuery, isAdmin);
        if(teamUserVOList == null || teamUserVOList.size() == 0){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "数据不存在");
        }
        // 2.判断当前用户是否加入队伍
        List<Long> teamIds = teamUserVOList.stream().map(teamUserVO -> teamUserVO.getId()).collect(Collectors.toList());
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        try{

            // 查询以加入的队伍
            userTeamQueryWrapper.eq("user_id", loginUser.getId());
            userTeamQueryWrapper.in("team_id", teamIds);
            List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);

            if(!CollectionUtils.isEmpty(userTeamList)){
                // 已加入的队伍id集合
                Set<Long> hasJoinTeamIds = userTeamList.stream().map(userTeam -> userTeam.getTeamId()).collect(Collectors.toSet());
                // 对以加入的队伍返回对象设置 hasJoin = true
                teamUserVOList.forEach(teamUserVO -> {
                    if(hasJoinTeamIds.contains(teamUserVO.getId())){
                        teamUserVO.setHasJoin(true);
                    }
                });
            }
        }catch (Exception e){

        }

        // 3.获取各队伍已加入队伍人数
        userTeamQueryWrapper.clear();
        userTeamQueryWrapper.in("team_id", teamIds);
        List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
        // key:teamId value: List<加入队伍User>
        Map<Long, List<UserTeam>> teamIdUserMap = userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        teamUserVOList.forEach(teamUserVO -> {
            teamUserVO.setHasJoinNum(Optional.ofNullable(teamIdUserMap.get(teamUserVO.getId()) == null ? 0 : teamIdUserMap.get(teamUserVO.getId()).size()).orElse(0));
        });

        return ResponseResult.okResult(teamUserVOList);
    }

    @GetMapping("list/page")
    public ResponseResult<Page<Team>> pageQueryTeam(TeamQuery teamQuery){
        if(teamQuery == null){
            throw new CustomException(AppHttpCodeEnum.PARAM_INVALID);
        }
        // 把teamQuery映射为Team
        Team team = new Team();
        BeanUtils.copyProperties(teamQuery, team);
        QueryWrapper<Team> queryWrapper = new QueryWrapper<>();
        Page<Team> result = teamService.page(new Page<>(teamQuery.getPage(), teamQuery.getSize()),
                queryWrapper);
        if(result == null){
            throw new CustomException(AppHttpCodeEnum.SERVER_ERROR);
        }
        return ResponseResult.okResult(result);
    }

    @PostMapping("join")
    public ResponseResult<Boolean> joinTeam(@RequestBody TeamJoinRequest teamJoinRequest, HttpServletRequest request){
        if(teamJoinRequest == null){
            throw new CustomException(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        // 从请求头中获取token
        String token = request.getHeader("Token");
        User loginUser = userService.getLoginUser(token);
        boolean result = teamService.joinTeam(teamJoinRequest, loginUser);
        return ResponseResult.okResult(result);
    }

    @PostMapping("/quit")
    public ResponseResult<Boolean> quitTeam(@RequestBody TeamQuitRequest teamQuitRequest, HttpServletRequest request){
        if(teamQuitRequest == null){
            throw new CustomException(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        // 从请求头中获取token
        String token = request.getHeader("Token");
        User loginUser = userService.getLoginUser(token);
        boolean result = teamService.quitTeam(teamQuitRequest, loginUser);
        return ResponseResult.okResult(result);
    }

    @PostMapping("disband")
    public ResponseResult<Boolean> disbandTeam(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request){
        if(deleteRequest == null){
            throw new CustomException(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        Long teamId = deleteRequest.getId();
        // 从请求头中获取token
        String token = request.getHeader("Token");
        User loginUser = userService.getLoginUser(token);
        boolean result = teamService.disbandTeam(teamId, loginUser);
        return ResponseResult.okResult(result);
    }

    /**
     * 获取我创建的队伍
     * @param teamQuery
     * @param request
     * @return
     */
    @GetMapping("list/my/create")
    public ResponseResult<List<TeamUserVO>> getMyCreateTeamList(TeamQuery teamQuery, HttpServletRequest request){
        if(teamQuery == null){
            throw new CustomException(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        // 获取登录用户id
        // 从请求头中获取token
        String token = request.getHeader("Token");
        User loginUser = userService.getLoginUser(token);
        teamQuery.setUserid(loginUser.getId());

        List<TeamUserVO> result = teamService.listTeam(teamQuery, true);
        if(result == null || result.size() == 0){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        hasJoinAndNumber(result, loginUser);
        return ResponseResult.okResult(result);
    }

    /**
     * 获取我加入的队伍
     * @param teamQuery
     * @param request
     * @return
     */
    @GetMapping("list/my/join")
    public ResponseResult<List<TeamUserVO>> getMyJoinTeamList(TeamQuery teamQuery, HttpServletRequest request){
        if(teamQuery == null){
            throw new CustomException(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        // 获取登录用户id
        // 从请求头中获取token
        String token = request.getHeader("Token");
        User loginUser = userService.getLoginUser(token);
        Long userId = loginUser.getId();
        // 从用户-队伍关系表中查询我加入的队伍
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        userTeamQueryWrapper.eq("user_id", userId);
        List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
        if(CollectionUtils.isEmpty(userTeamList)){
            return ResponseResult.okResult(null);
        }
        Map<Long, List<UserTeam>> listMap = userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        List<Long> teamIdList = new ArrayList<>(listMap.keySet());
        teamQuery.setTeamIdList(teamIdList);

        List<TeamUserVO> result = teamService.listTeam(teamQuery, true);
        if(result == null || result.size() == 0){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        hasJoinAndNumber(result, loginUser);
        return ResponseResult.okResult(result);
    }

    public void hasJoinAndNumber(List<TeamUserVO> teamUserVOList, User loginUser){
        // 2.判断当前用户是否加入队伍
        List<Long> teamIds = teamUserVOList.stream().map(teamUserVO -> teamUserVO.getId()).collect(Collectors.toList());
        QueryWrapper<UserTeam> userTeamQueryWrapper = new QueryWrapper<>();
        try{

            // 查询以加入的队伍
            userTeamQueryWrapper.eq("user_id", loginUser.getId());
            userTeamQueryWrapper.in("team_id", teamIds);
            List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);

            if(!CollectionUtils.isEmpty(userTeamList)){
                // 已加入的队伍id集合
                Set<Long> hasJoinTeamIds = userTeamList.stream().map(userTeam -> userTeam.getTeamId()).collect(Collectors.toSet());
                // 对以加入的队伍返回对象设置 hasJoin = true
                teamUserVOList.forEach(teamUserVO -> {
                    if(hasJoinTeamIds.contains(teamUserVO.getId())){
                        teamUserVO.setHasJoin(true);
                    }
                });
            }
        }catch (Exception e){

        }

        // 3.获取各队伍已加入队伍人数
        userTeamQueryWrapper.clear();
        userTeamQueryWrapper.in("team_id", teamIds);
        List<UserTeam> userTeamList = userTeamService.list(userTeamQueryWrapper);
        // key:teamId value: List<加入队伍User>
        Map<Long, List<UserTeam>> teamIdUserMap = userTeamList.stream().collect(Collectors.groupingBy(UserTeam::getTeamId));
        teamUserVOList.forEach(teamUserVO -> {
            // 队伍已加入人数
            teamUserVO.setHasJoinNum(Optional.ofNullable(teamIdUserMap.get(teamUserVO.getId()) == null ? 0 : teamIdUserMap.get(teamUserVO.getId()).size()).orElse(0));

            Long createUserId = teamUserVO.getUserId();
            User user = userService.getById(createUserId);
            SafetyUser safetyUser = userService.getSafetyUser(user);
            teamUserVO.setCreateUser(safetyUser);
        });
    }
}
