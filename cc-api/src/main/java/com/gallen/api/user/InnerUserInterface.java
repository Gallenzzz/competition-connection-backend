package com.gallen.api.user;

import com.gallen.pojos.user.User;
import com.gallen.vos.user.SafetyUser;

public interface InnerUserInterface {
    /**
     * 根据id查询用户
     * @param userId
     * @return
     */
    User getById(Long userId);

    /**
     * 获取用户脱敏信息
     * @param user
     * @return
     */
    SafetyUser getSafetyUser(User user);

    /**
     * 判断用户是否是管理员
     * @param user
     * @return
     */
    Boolean isAdmin(User user);

    /**
     * 根据token获取登录用户信息
     * @param token
     * @return
     */
    User getLoginUser(String token);
}
