package com.gallen.vos.team;


import com.gallen.vos.user.SafetyUser;
import lombok.Data;

import java.util.Date;

@Data
/**
 * 队伍-用户信息封装类(脱敏)
 */
public class TeamUserVO{
//    private static final long serialVersionUID = -3233884772741566524L;
    /**
     *
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
     * 创建者id
     */
    private Long userId;

    /**
     * 0 - 公开， 1 - 私有， 2 - 加密
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 入队用户列表
     */
//    List<UserVO> userList;

    /**
     * 队伍创建人的信息
     */
    private SafetyUser createUser;

    /**
     * 是否加入队伍
     */
    private boolean hasJoin = false;

    /**
     * 已加入队伍的人数
     */
    private Integer hasJoinNum;
}
