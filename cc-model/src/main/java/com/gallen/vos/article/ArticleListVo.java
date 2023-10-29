package com.gallen.vos.article;

import com.gallen.common.annotation.IdEncrypt;
import lombok.Data;

import java.util.Date;

@Data
public class ArticleListVo {
    @IdEncrypt
    private Long id;
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
     * 点赞数量
     */
    private Integer likes;

    /**
     * 发布时间
     */
    private Date publishTime;

}
