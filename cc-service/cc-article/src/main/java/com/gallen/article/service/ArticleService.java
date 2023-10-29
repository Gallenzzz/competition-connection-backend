package com.gallen.article.service;

import com.gallen.dtos.article.ArticleAddRequest;
import com.gallen.common.common.ResponseResult;
import com.gallen.pojos.article.Article;
import com.baomidou.mybatisplus.extension.service.IService;


public interface ArticleService extends IService<Article> {

    /**
     * 添加文章
     * @param articleAddRequest
     * @param token
     * @return
     */
    ResponseResult addArticle(ArticleAddRequest articleAddRequest, String token);

    /**
     * 分页搜索查询文章列表
     * @param searchText
     * @param channelId
     * @param page
     * @param size
     * @return
     */
    ResponseResult pageQueryArticles(String searchText, Long channelId, Long page, Long size);

    /**
     * 根据id查询文章
     * @param articleId
     * @param token
     * @return
     */
    ResponseResult getArticleById(Long articleId, String token);

    /**
     * 用户点赞某篇文章
     * @param token
     * @param articleId
     * @return
     */
    ResponseResult like(String token, Long articleId);
}
