package com.gallen.dtos.team;

import lombok.Data;

import java.util.Date;

@Data
public class TeamUpdateRequest {
    /**
     * 队伍id
     */
    private Long id;
    /**
     * 队伍名称
     */
    private String name;

    /**
     * 队伍描述
     */
    private String description;

    /**
     * 队伍最大人数
     */
    private Integer maxNum;

    /**
     * 队伍过期时间
     */
    private Date expireTime;


    /**
     * 0 - 公开， 1 - 私有， 2 - 加密
     */
    private Integer status;

    /**
     * 密码
     */
    private String password;
}
