package com.gallen.team.service;

import com.gallen.dtos.team.TeamJoinRequest;
import com.gallen.dtos.team.TeamQuitRequest;
import com.gallen.dtos.team.TeamUpdateRequest;
import com.gallen.pojos.user.User;
import com.gallen.dtos.team.TeamQuery;
import com.gallen.pojos.team.Team;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gallen.vos.team.TeamUserVO;

import java.util.List;


public interface TeamService extends IService<Team> {
    /**
     * 添加队伍
     * @param team
     * @param loginUser
     * @return
     */
    long addTeam(Team team, User loginUser);

    /**
     * 搜索查询队伍列表
     * @param teamQuery
     * @param isAdmin
     * @return
     */
    List<TeamUserVO> listTeam(TeamQuery teamQuery, boolean isAdmin);

    /**
     * 更新队伍信息
     * @param teamUpdateRequest
     * @param loginUser
     * @return
     */
    boolean updateTeam(TeamUpdateRequest teamUpdateRequest, User loginUser);

    /**
     * 加入队伍
     * @param teamJoinRequest
     * @param loginUser
     * @return
     */
    boolean joinTeam(TeamJoinRequest teamJoinRequest, User loginUser);

    /**
     * 退出队伍
     * @param teamQuitRequest
     * @param loginUser
     * @return
     */
    boolean quitTeam(TeamQuitRequest teamQuitRequest, User loginUser);

    /**
     * 解散队伍
     * @param teamId
     * @param loginUser
     * @return
     */
    boolean disbandTeam(long teamId, User loginUser);
}
