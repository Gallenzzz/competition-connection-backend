package com.gallen.service.dubbo;

import com.gallen.api.user.InnerUserInterface;
import com.gallen.pojos.user.User;
import com.gallen.service.UserService;
import com.gallen.vos.user.SafetyUser;
import org.apache.dubbo.config.annotation.DubboService;


import javax.annotation.Resource;


@DubboService
public class InnerUserServiceImpl implements InnerUserInterface {
    @Resource
    private UserService userService;

    /**
     * 用户脱敏
     *
     * @param user
     * @return
     */
    @Override
    public SafetyUser getSafetyUser(User user) {
        return userService.getSafetyUser(user);
    }

    @Override
    public User getLoginUser(String token) {
        return userService.getLoginUser(token);
    }

    /**
     * 判断用户是否是管理员
     *
     * @param user
     * @return
     */
    @Override
    public Boolean isAdmin(User user) {
       return userService.isAdmin(user);
    }

    /**
     * 根据id查询用户
     *
     * @param userId
     * @return
     */
    @Override
    public User getById(Long userId) {
        return userService.getById(userId);
    }
}
