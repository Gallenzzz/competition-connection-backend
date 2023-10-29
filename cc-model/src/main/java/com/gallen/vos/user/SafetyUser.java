package com.gallen.vos.user;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.gallen.common.annotation.IdEncrypt;
import lombok.Data;

import java.io.Serializable;
@Data
public class SafetyUser implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 主键
     */
    @IdEncrypt
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 用户名唯一
     */
    private String username;

    /**
     * 昵称
     */
    private String nickname;

    /**
     * 手机号码
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 性别
     */
    private String gender;

    /**
     * 标签JSON列表字符串
     */
    private String tag;

    /**
     * 0：正常，1：禁用
     */
    private Integer status;

    /**
     * 0：管理员，1：普通用户
     */
    private Integer role;
}
