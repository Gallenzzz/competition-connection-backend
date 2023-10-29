package com.gallen.article.controller;

import com.gallen.article.aop.annotation.ReadCount;
import com.gallen.article.service.ArticleService;
import com.gallen.article.service.ChannelService;
import com.gallen.dtos.article.ArticleAddRequest;
import com.gallen.common.common.ResponseResult;
import com.gallen.common.enums.AppHttpCodeEnum;
import com.gallen.pojos.article.Channel;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/article")
@CrossOrigin(origins = "*", allowCredentials = "true")
public class ArticleController {
    @Resource
    private ChannelService channelService;

    @Resource
    private ArticleService articleService;


    @PostMapping("add")
    public ResponseResult addArticle(@RequestBody ArticleAddRequest articleAddRequest,
                                     HttpServletRequest request){
        String token = request.getHeader("Token");
        if(StringUtils.isBlank(token)){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        return articleService.addArticle(articleAddRequest, token);
    }

    @GetMapping("channels")
    public ResponseResult getChannels(){
        List<Channel> channels = channelService.list();
        return ResponseResult.okResult(channels);
    }

    @GetMapping("/articles")
    public ResponseResult pageQueryArticles(@RequestParam(value = "searchText", required = false)String searchText,
                                            @RequestParam(value = "channelId", required = false) Long channelId,
                                            @RequestParam(value = "page", required = false) Long page,
                                            @RequestParam(value = "size", required = false) Long size){
        return articleService.pageQueryArticles(searchText, channelId,page, size);
    }

    @GetMapping("/articleDetail")
    @ReadCount
    public ResponseResult getArticleById(@RequestParam("articleId") Long articleId, HttpServletRequest request){
        String token = request.getHeader("Token");
        return articleService.getArticleById(articleId, token);
    }

    @PostMapping("/like")
    public ResponseResult likeArticle(Long articleId, HttpServletRequest request){
        String token = request.getHeader("Token");
        return articleService.like(token, articleId);
    }
}
