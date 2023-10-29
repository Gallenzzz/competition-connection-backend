package com.gallen.service;

import com.gallen.common.common.ResponseResult;
import com.gallen.dtos.user.UserLoginDto;
import com.gallen.pojos.user.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gallen.vos.user.SafetyUser;

import javax.servlet.http.HttpServletRequest;
import java.util.List;


public interface UserService extends IService<User> {

    /**
     * 登录
     * @param userLoginDto
     * @return
     */
    ResponseResult login(UserLoginDto userLoginDto);

    /**
     * 用户脱敏
     * @param originalUser
     * @return
     */
    SafetyUser getSafetyUser(User originalUser);

    /**
     * 用户脱敏
     * @param token
     * @return
     */
    SafetyUser getSafetyUser(String token);

    /**
     * 根据标签搜索用户
     * @param tagNameList 标签列表
     * @param token
     * @param current
     * @param pageSize
     * @return
     */
    List<SafetyUser> searchUsersByTags(List<String> tagNameList, String token, Integer current, Integer pageSize);


    /**
     * 获取当前登录用户
     * @return
     * @param token
     */
    User getLoginUser(String token);

    /**
     * 分页查询首页推荐用户
     * @param current 当前页码
     * @param pageSize 每页记录数
     * @param request 请求对象
     * @return
     */
    List<SafetyUser> getRecommendedUser(long current, long pageSize, HttpServletRequest request);

    /**
     * 根据标签推荐相似的Top n 用户
     * @param num
     * @param loginUser
     * @return
     */
    List<SafetyUser> matchUsers(long num, User loginUser);

    /**
     * 更新用户信息
     * @return 更新记录数
     * @param user
     * @param loginUser
     */
    int updateUser(User user, User loginUser);

    /**
     * 退出登录
     * @param request
     */
    Integer userLogout(HttpServletRequest request);

    /**
     * 判断是否为管理员
     * @param loginUser 当前登录用户
     * @return
     */
    boolean isAdmin(User loginUser);

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 二次密码
     * @param nickname
     * @return 新用户id
     */
    ResponseResult userRegister(String userAccount, String userPassword, String checkPassword, String nickname);

    /**
     * 根据用户id查询用户信息
     * @param userId
     * @return
     */
    SafetyUser getUserById(Long userId);
}
