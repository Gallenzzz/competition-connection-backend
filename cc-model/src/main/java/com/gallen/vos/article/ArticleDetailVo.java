package com.gallen.vos.article;

import com.gallen.vos.user.SafetyUser;
import lombok.Data;

import java.util.Date;

@Data
public class ArticleDetailVo {

    /**
     * 文章标题
     */
    private String title;

    /**
     * 封面
     */
    private String cover;

    /**
     * 文章html内容
     */
    private String content;

    /**
     * 阅读量
     */
    private Long views;

    /**
     * 评论数量
     */
    private Integer comments;

    /**
     * 点赞数量
     */
    private Integer likes;

    /**
     * 收藏数量
     */
    private Integer collections;

    private Date publishTime;


    /**
     * 发布者用户信息
     */
    private SafetyUser creator;

    /**
     * 当前用户是否已点赞
     */
    private Boolean like;
}
