package com.gallen.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gallen.common.common.ResponseResult;
import com.gallen.common.enums.AppHttpCodeEnum;
import com.gallen.dtos.user.UserLoginDto;
import com.gallen.jwt.AppJwtUtil;
import com.gallen.pojos.user.Tag;
import com.gallen.pojos.user.User;
import com.gallen.pojos.user.UserTag;
import com.gallen.service.TagService;
import com.gallen.common.exception.CustomException;
import com.gallen.mapper.UserMapper;

import com.gallen.service.UserService;
import com.gallen.service.UserTagService;
import com.gallen.utils.AlgorithmUtils;
import com.gallen.vos.user.SafetyUser;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.jsonwebtoken.Claims;
import javafx.util.Pair;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.gallen.common.constants.UserConstants.*;


@Service

public class UserServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserService {
    @Resource
    private UserMapper userMapper;
    public static final String SALT = "hello";
    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 登录
     *
     * @param userLoginDto
     * @return
     */
    @Override
    public ResponseResult login(UserLoginDto userLoginDto) {
        // 校验参数
        if (userLoginDto == null || StringUtils.isBlank(userLoginDto.getUsername())
                || StringUtils.isBlank(userLoginDto.getPassword())) {
            throw new CustomException(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        // 校验账号密码是否正确
        //    根据用户名查询用户信息
        String username = userLoginDto.getUsername();
        User dbUser = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, username));
        if (dbUser == null) {
            throw new CustomException(AppHttpCodeEnum.USER_NOT_EXIT);
        }
        //    使用MD5对密码进行加密
        String salt = dbUser.getSalt();
        String mdPassword = DigestUtils.md5DigestAsHex((salt + userLoginDto.getPassword()).getBytes(StandardCharsets.UTF_8));
        //    比较密码是否正确
        if (!mdPassword.equals(dbUser.getPassword())) {
            throw new CustomException(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR);
        }
        // 登录成功，返回JWT和脱敏后的用户信息
        //    生成JWT
        String token = AppJwtUtil.getToken(dbUser.getId());
        //    脱敏用户信息
        SafetyUser safetyUser = getSafetyUser(dbUser);
        // 将脱敏后的用户信息保存在Redis中
        redisTemplate.opsForValue().set(USER_LOGIN_STATE_KEY + safetyUser.getId(), safetyUser, 7, TimeUnit.DAYS);

        Map<String, Object> result = new HashMap();
        result.put("token", token);
        result.put("user", safetyUser);
        return ResponseResult.okResult(result);
    }

    /**
     * 用户脱敏
     *
     * @param token
     * @return
     */
    @Override
    public SafetyUser getSafetyUser(String token) {
        // 从token中解析出userId
        if (StringUtils.isNotBlank(token)) {
            Claims claimsBody = AppJwtUtil.getClaimsBody(token);
            //是否是过期
            int result = AppJwtUtil.verifyToken(claimsBody);
            // -1：有效，0：有效，1：过期，2：过期
            if (result == 1 || result == 2) {
                // 过期，请重新登录
                return null;
            }
            long userId = claimsBody.get("id") instanceof Long ? (Long) claimsBody.get("id") : (Integer) claimsBody.get("id");

            // 根据userId从Redis中查询
            SafetyUser cacheSafetyUser = (SafetyUser) redisTemplate.opsForValue().get(USER_LOGIN_STATE_KEY + userId);
            if (cacheSafetyUser == null) {
                User user = getById(userId);
                cacheSafetyUser = getSafetyUser(user);
                // 把登录用户的数据缓存到Redis
                redisTemplate.opsForValue().set(USER_LOGIN_STATE_KEY + user.getId(), cacheSafetyUser, 7, TimeUnit.DAYS);
            }

            return cacheSafetyUser;
        }
        return null;
    }

    /**
     * 用户脱敏
     *
     * @param user 脱敏前的用户信息
     * @return 脱敏后的用户信息
     */
    @Override
    public SafetyUser getSafetyUser(User user) {
        if (user == null) {
            return null;
        }
        SafetyUser safetyUser = new SafetyUser();
        BeanUtils.copyProperties(user, safetyUser);
        if (user.getGender().equals(0)) {
            safetyUser.setGender("男");
        } else if (user.getGender().equals(1)) {
            safetyUser.setGender("女");
        } else if (user.getGender().equals(2)) {
            safetyUser.setGender("保密");
        }
        return safetyUser;
    }

    /**
     * 根据标签搜索用户(内存过滤)
     *
     * @param tagNameList 标签列表
     * @param token
     * @param current
     * @param pageSize
     * @return
     */
    @Resource
    private TagService tagService;
    @Resource
    private UserTagService userTagService;

    @Override
    public List<SafetyUser> searchUsersByTags(List<String> tagNameList, String token, Integer current, Integer pageSize) {
        //判断标签列表是否为空
        if (CollectionUtils.isEmpty(tagNameList)) {
            throw new CustomException(AppHttpCodeEnum.PARAM_REQUIRE);
        }

        // 从token中解析出userId
        Long userId = null;
        if (StringUtils.isNotBlank(token)) {
            Claims claimsBody = AppJwtUtil.getClaimsBody(token);
            //是否是过期
            int result = AppJwtUtil.verifyToken(claimsBody);
            // -1：有效，0：有效，1：过期，2：过期
            if (result == 1 || result == 2) {
                // 过期，请重新登录
                return null;
            }
            userId = claimsBody.get("id") instanceof Long ? (Long) claimsBody.get("id") : (Integer) claimsBody.get("id");
        }
        final Long finalUserId = userId == null ? 0L : userId;
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne(userId != null, "id", userId);
        // 内存查询
        // 根据标签名列表查询对应的标签id
        List<Long> tagIds = new ArrayList<>();
        tagNameList.stream().forEach(tagName -> {
            Tag tagObj = tagService.getOne(Wrappers.<Tag>lambdaQuery()
                    .eq(Tag::getTagName, tagName));
            tagIds.add(tagObj.getId());
        });
        // 从用户标签关系表中查询标签id in tagIds的数据
        List<UserTag> userTags = userTagService.list(Wrappers.<UserTag>lambdaQuery()
                .select(UserTag::getUserId)
                .in(UserTag::getTagId, tagIds)
                .ne(userId != null, UserTag::getUserId, userId)
                .groupBy(UserTag::getUserId));

        List<SafetyUser> safetyUserList = userTags.stream().map(userTag -> {
            // 根据用户id查询用户信息
            User user = getById(userTag.getUserId());
            return getSafetyUser(user);
        }).limit(current * pageSize).collect(Collectors.toList());
        // 1.查询所有用户到内存
//        List<User> userList = userMapper.selectList(queryWrapper);
//        Gson gson = new Gson();
//        // 2.判断每个用户是否包含标签
//        List<SafetyUser> safetyUserList = userList.parallelStream().filter(user -> {
//            String tagsStr = user.getTag();
//            // 如果用户没有标签
//            if (StringUtils.isBlank(tagsStr)) {
//                return false;
//            }
//            // 将标签名列表json解析成list
//            Set<String> tempTagNameSet = gson.fromJson(tagsStr, new TypeToken<Set<String>>() {
//            }.getType());
//            // 判断tempTagNameSet是否为空
//            tempTagNameSet = Optional.ofNullable(tempTagNameSet).orElse(new HashSet<>());
//            // 判断用户是否包含搜索的标签
//            for (String tagName : tagNameList) {
//                if (!tempTagNameSet.contains(tagName)) {
//                    //用户不包含要搜索的标签
//                    return false;
//                }
//            }
//            return true;
//        }).map(this::getSafetyUser).limit(current * pageSize).collect(Collectors.toList());
        return safetyUserList;
    }

    /**
     * 获取当前登录用户
     *
     * @param token
     * @return
     */
    @Override
    public User getLoginUser(String token) {

        // 从token中解析出userId
        if (StringUtils.isNotBlank(token)) {
            Claims claimsBody = AppJwtUtil.getClaimsBody(token);
            //是否是过期
            int result = AppJwtUtil.verifyToken(claimsBody);
            // -1：有效，0：有效，1：过期，2：过期
            if (result == 1 || result == 2) {
                // 过期，请重新登录
                return null;
            }
            long userId = claimsBody.get("id") instanceof Long ? (Long) claimsBody.get("id") : (Integer) claimsBody.get("id");
            // 根据userId查询用户信息
            User user = getById(userId);

            return user;
        }
        return null;
    }

    /**
     * 分页查询首页推荐用户
     *
     * @param current  当前页码
     * @param pageSize 每页记录数
     * @param request  请求对象
     * @return
     */
    @Override
    public List<SafetyUser> getRecommendedUser(long current, long pageSize, HttpServletRequest request) {
        // 从请求头中获取token
        String token = request.getHeader("Token");
        // 因为每个用户的推荐不同，获取登录用户id，用于拼接key，从Redis中查询缓存
        User loginUser = getLoginUser(token);
        Long userId = loginUser.getId();
        String redisKey = "yupao:user:recommend:" + userId;
        Page<SafetyUser> pageSafetyUser = new Page<>();
        // Redis中没有缓存，从数据库中查询
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(User::getId, userId);
        Page<User> userPage = new Page<>(current, pageSize);

        Page<User> pageUser = page(userPage, queryWrapper);
        // 用户信息脱敏
        List<SafetyUser> list = pageUser.getRecords().stream().map(this::getSafetyUser).collect(Collectors.toList());
        BeanUtils.copyProperties(pageUser, pageSafetyUser);
        pageSafetyUser.setRecords(list);

        return pageSafetyUser.getRecords();
    }


    @Override
    public List<SafetyUser> matchUsers(long num, User loginUser) {
        // 获取登录用户的标签列表
        String tags = loginUser.getTag();

        Gson gson = new Gson();
        List<String> loginUserTasList = gson.fromJson(tags, new TypeToken<List<String>>() {
        }.getType());
//        List<String> loginUserTagList = new ArrayList<>(tempTagNameSet);

        // 查询除了自己，且标签不为空的用户
        Long loginUserId = loginUser.getId();
        QueryWrapper<User> userQueryWrapper = new QueryWrapper<>();
        userQueryWrapper.isNotNull("tag");
        userQueryWrapper.ne("id", loginUserId);
        userQueryWrapper.select("id", "tag"); // 优化: 只查询需要的字段
        List<User> userList = this.list(userQueryWrapper);

        // 遍历用户列表，分别计算相似率
        List<Pair<User, Double>> list = new ArrayList<>();
        // 依次计算登录用户和所有用户编辑距离
        for (int i = 0; i < userList.size(); i++) {
            User user = userList.get(i);
            String userTags = user.getTag();
            // 无标签
            if (StringUtils.isBlank(userTags)) {
                continue;
            }

            List<String> userTagList = gson.fromJson(userTags, new TypeToken<List<String>>() {
            }.getType());

            // 计算相似率
//            double similarRate = AlgorithmUtils.evaluate(loginUserTasList, userTagList);
//            indexSimilarMap.put(i, similarRate);
            double distance = AlgorithmUtils.minDistance(loginUserTasList, userTagList);
            list.add(new Pair(user, distance));
        }

        // 取出相似度Top n
        List<Pair<User, Double>> topUserPairList = list.stream()
                .sorted((a, b) -> (int) (a.getValue() - b.getValue()))
                .limit(num)
                .collect(Collectors.toList());
        List<Long> userVOListIds = topUserPairList.stream().map(pair -> {
            return pair.getKey().getId();
        }).collect(Collectors.toList());
        userQueryWrapper.clear();
        if (!userVOListIds.isEmpty()) {
            userQueryWrapper.in("id", userVOListIds);
            String idStr = StringUtils.join(userVOListIds, ',');
            userQueryWrapper.last("ORDER BY FIELD(id," + idStr + ")");
        }

        List<SafetyUser> users = this.list(userQueryWrapper).stream().map(user -> {
            return getSafetyUser(user);
        }).collect(Collectors.toList());
        return users;
    }

    /**
     * 根据用户id查询用户信息
     *
     * @param userId
     * @return
     */
    @Override
    public SafetyUser getUserById(Long userId) {
        User user = getById(userId);
        SafetyUser safetyUser = getSafetyUser(user);
        return safetyUser;
    }

    @Override
    public ResponseResult userRegister(String userAccount, String userPassword, String checkPassword, String nickname) {

        //1.校验不为空
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE, "请求参数为空");
        }
        //2.账户不小于4位
        if (userAccount.length() < 4) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "用户名应不少于4位");
        }
        //3.密码小于8位
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "密码应不少于8位");

        }

        //5.校验账户不包含特殊字符
        String validPattern = "^[a-zA-Z_]([a-zA-Z0-9_])";
        Matcher matcher = Pattern.compile(validPattern).matcher(userAccount);
        if (!matcher.find()) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "密码应只包含字母、数字、下划线!");

        }
        //6.校验密码
        if (!userPassword.equals(checkPassword)) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "两次密码不一致!");

        }


        //4.账户不重复(此操作需要查数据库，放在其他校验之后，优化性能)
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", userAccount);
        long count = this.count(queryWrapper);
        if (count > 0) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID, "用户名已存在!");

        }

        // 生成盐:使用用户名和系统盐加密
        String salt = DigestUtils.md5DigestAsHex((SALT + userAccount).getBytes(StandardCharsets.UTF_8));

        //加密密码

        String encryptPassword = DigestUtils.md5DigestAsHex((salt + userPassword).getBytes(StandardCharsets.UTF_8));

        //插入数据
        User user = new User();
        user.setUsername(userAccount);
        user.setSalt(salt);
        user.setPassword(encryptPassword);
        user.setNickname(nickname);
        user.setAvatar("http://8.134.128.138:9000/competition-connection/2023/09/18/0e9b35b568294c6c8bb40751f8075de7.png");
        boolean result = this.save(user);
        if (!result) {
            return ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR, "服务器出错，请稍后重试");

        }
        return ResponseResult.okResult(user.getId());
    }

    /**
     * 退出登录
     *
     * @param request
     */
    @Override
    public Integer userLogout(HttpServletRequest request) {

        return 1;
    }

    /**
     * 更新用户信息
     *
     * @param user
     * @param loginUser
     * @return
     */
    @Override
    public int updateUser(User user, User loginUser) {
        // 判断要修改用户信息的参数是否有效
        Long userId = user.getId();
        if (userId == null || userId.longValue() <= 0) {
            throw new CustomException(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        // todo 补充校验：如果没有传任何要修改的字段，就直接报错，不用执行更新语句
        // 如果是管理员，可以修改任意用户
        // 如果不是管理员，可以修改自己的信息
        if (!isAdmin(loginUser) && !user.getId().equals(loginUser.getId())) {
            throw new CustomException(AppHttpCodeEnum.NO_OPERATOR_AUTH);
        }

        // 更新用户信息
        // 判断要更新的用户信息是否存在
        User oldUser = userMapper.selectById(userId);
        if (oldUser == null) {
            throw new CustomException(AppHttpCodeEnum.DATA_NOT_EXIST);
        }
        int affected = userMapper.updateById(user);
        if (affected > 0) {
            // 更新成功，删除Redis中已缓存的数据
            redisTemplate.delete(USER_LOGIN_STATE_KEY + loginUser.getId());
        }
        return affected;
    }

    /**
     * 根据登录用户判断是否是管理员
     *
     * @param loginUser 当前登录用户
     * @return
     */
    @Override
    public boolean isAdmin(User loginUser) {
        if (loginUser == null || loginUser.getRole() != ADMIN_ROLE) {
            return false;
        }
        return true;
    }

}


