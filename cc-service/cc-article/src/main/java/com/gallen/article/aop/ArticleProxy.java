package com.gallen.article.aop;

import com.gallen.jwt.AppJwtUtil;
import io.jsonwebtoken.Claims;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.Objects;

// 增强的类
@Component
@Aspect
public class ArticleProxy {
    @Autowired
    private RedisTemplate redisTemplate;
    // 在增强的类中添加增强逻辑
//    @Pointcut("execution(* com.gallen.article.controller.ArticleController.getArticleById(..))")
//    public void ArticleProxy(){}


    @After("@annotation(com.gallen.article.aop.annotation.ReadCount)")
    public void countRead(JoinPoint joinPoint){
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        // 获取articleId和userId
        HttpServletRequest request = Objects.requireNonNull(attributes).getRequest();
        Long articleId = Long.parseLong(request.getParameter("articleId"));
        String token = request.getHeader("Token");
        Long userId = null;
        if (StringUtils.isNotBlank(token)) {
            Claims claimsBody = AppJwtUtil.getClaimsBody(token);
            //是否是过期
            int result = AppJwtUtil.verifyToken(claimsBody);
            // -1：有效，0：有效，1：过期，2：过期
            if (result == 1 || result == 2) {
                // 过期，请重新登录
                return;
            }
            userId = claimsBody.get("id") instanceof Long ? (Long) claimsBody.get("id") : (Integer) claimsBody.get("id");
        }

        if(userId != null && articleId != null){
            // 把数据保存到Redis
            // key: cc:article:read:articleId
            // value: userId
            // score: 当前时间戳
            String key = "cc:article:read:" + articleId;

            Boolean add = redisTemplate.opsForZSet().add(key, userId, new Date().getTime());
        }
        System.out.println("after");
    }
}
