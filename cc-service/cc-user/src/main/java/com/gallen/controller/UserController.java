package com.gallen.controller;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gallen.common.common.ResponseResult;
import com.gallen.common.enums.AppHttpCodeEnum;
import com.gallen.common.exception.CustomException;
import com.gallen.dtos.user.UserLoginDto;
import com.gallen.dtos.user.UserRegisterDto;
import com.gallen.ipcounter.service.IpCountService;
import com.gallen.pojos.user.User;
import com.gallen.service.UserService;
import com.gallen.vos.user.SafetyUser;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/user")
@Api("用户控制器")
@CrossOrigin(origins = "*", allowCredentials = "true")
public class UserController {
    @Resource
    private UserService userService;

    @PostMapping("/login")
    @ApiOperation("用户登录")
    public ResponseResult login(@RequestBody UserLoginDto userLoginDto) {

        return userService.login(userLoginDto);
    }

    @PostMapping("/register")
    public ResponseResult<Long> userRegister(@RequestBody UserRegisterDto userRegisterDto) {
        if (userRegisterDto == null) {
            throw new CustomException(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        String userAccount = userRegisterDto.getUsername();
        String userPassword = userRegisterDto.getPassword();
        String checkPassword = userRegisterDto.getCheckPassword();
        String nickname = userRegisterDto.getNickname();
        return userService.userRegister(userAccount, userPassword, checkPassword, nickname);
    }

    @GetMapping("/name")
    public ResponseResult getName() {
        return ResponseResult.okResult("name");
    }

    /**
     * 获取当前用户登录态
     *
     * @param request 请求
     * @return 脱敏后的用户信息
     */
    @GetMapping("/current")
    public ResponseResult<User> getCurrentUser(HttpServletRequest request) {
        // 从请求头中获取token
        String token = request.getHeader("Token");
        SafetyUser safetyUser = userService.getSafetyUser(token);
        // 脱敏返回
        if(safetyUser != null){
            return ResponseResult.okResult(safetyUser);
        }

        return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
    }

    @GetMapping("/search/tags")
    public ResponseResult<List<SafetyUser>> searchUsersByTags(@RequestParam(required = false)
                                                                      List<String> tagNameList,
                                                              HttpServletRequest request,
                                                              Integer current,
                                                              Integer pageSize) {
        if (CollectionUtils.isEmpty(tagNameList)) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        String token = request.getHeader("Token");
        List<SafetyUser> users = userService.searchUsersByTags(tagNameList, token, current, pageSize);
        return ResponseResult.okResult(users);
    }

    /**
     * 推荐用户列表
     * @param current 当前页码
     * @param pageSize 每页记录数
     * @return
     */
    @GetMapping("/recommend")
    public ResponseResult<Page<SafetyUser>> recommendUsers(long current, long pageSize, HttpServletRequest request) {
        // 获取登录用户id，作于拼接key，从Redis中查询
        List<SafetyUser> userList = userService.getRecommendedUser(current, pageSize, request);
        return ResponseResult.okResult(userList);
    }

    @GetMapping("matches")
    public ResponseResult<List<SafetyUser>> matchUsers(int num, HttpServletRequest request){
        // 从请求头中获取token
        String token = request.getHeader("Token");
        User loginUser = userService.getLoginUser(token);
        List<SafetyUser> result = userService.matchUsers(num, loginUser);
        return ResponseResult.okResult(result);
    }

    @PutMapping("/update")
    public ResponseResult<Integer> updateUser(@RequestBody User user, HttpServletRequest request){
        // 判断参数是否为空
        if(user == null){
            throw new CustomException(AppHttpCodeEnum.PARAM_REQUIRE);
        }

        // 获取当前登录用户
        String token = request.getHeader("token");
        User loginUser = userService.getLoginUser(token);
        // 调用Service更新用户信息
        return ResponseResult.okResult(userService.updateUser(user, loginUser));
    }

    @GetMapping("/{userId}")
    public ResponseResult<SafetyUser> getUserById(@PathVariable("userId") Long userId){
        return ResponseResult.okResult(userService.getUserById(userId));
    }

    @Autowired
    private IpCountService ipCountService;
    @GetMapping("/mysql/{userId}")
    public ResponseResult<SafetyUser> getUserFromMySQL(@PathVariable("userId") Long userId){
        User user = userService.getById(userId);
        SafetyUser safetyUser = userService.getSafetyUser(user);
        ipCountService.count();
        return ResponseResult.okResult(safetyUser);
    }
    @Autowired
    private RedisTemplate redisTemplate;
    @GetMapping("/redis/{userId}")
    public ResponseResult<SafetyUser> getUserFromRedis(@PathVariable("userId") Long userId){
        String key = "cc:user:loginUser:" + userId;
        String  o = (String) redisTemplate.opsForValue().get(key);
        SafetyUser safetyUser = JSONUtil.toBean(o, SafetyUser.class);
        return ResponseResult.okResult(safetyUser);
    }
}
