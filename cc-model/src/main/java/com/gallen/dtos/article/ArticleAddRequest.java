package com.gallen.dtos.article;

import lombok.Data;



@Data
public class ArticleAddRequest {

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
     * 频道编号
     */
    private Long channelId;

    /**
     * 定时发布时间
     */
    private String publishTime;

}
