package com.gallen.dtos.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求体
 */
@Data
public class UserRegisterDto implements Serializable {
    private String username;
    private String password;
    private String checkPassword;
    private String nickname;
}
