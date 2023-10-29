package com.gallen.dtos.team;

import lombok.Data;

@Data
public class TeamJoinRequest {
    /**
     * 队伍id
     */
    private Long id;

    /**
     * 密码
     */
    private String password;
}
