package com.gallen.article.service.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gallen.article.service.*;
import com.gallen.dtos.article.ArticleAddRequest;
import com.gallen.pojos.user.User;
import com.gallen.api.user.InnerUserInterface;
import com.gallen.article.mq.sender.MessageSender;
import com.gallen.common.common.ResponseResult;
import com.gallen.common.enums.AppHttpCodeEnum;
import com.gallen.pojos.article.Article;
import com.gallen.article.mapper.ArticleMapper;
import com.gallen.pojos.article.ArticleCheck;
import com.gallen.pojos.article.ArticleContent;
import com.gallen.pojos.article.ArticleLike;
import com.gallen.vos.article.ArticleDetailVo;
import com.gallen.vos.article.ArticleListVo;
import com.gallen.vos.user.SafetyUser;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import static com.gallen.common.constants.ArticleConstants.ARTICLE_CHECK_AFTER_MILLISECONDS;
import static com.gallen.common.constants.ArticleConstants.ARTICLE_STATUS_PASS;


@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article>
        implements ArticleService {
    @DubboReference
    private InnerUserInterface userService;

    @Resource
    private SensitiveWordService sensitiveWordService;

    @Resource
    private ArticleContentService articleContentService;

    @Resource
    private ArticleCheckService articleCheckService;

    @Resource
    private MessageSender messageSender;

    @Resource
    private RedisTemplate redisTemplate;

    /**
     * 添加文章
     *
     * @param articleAddRequest
     * @param token
     * @return
     */
    @Override
    public ResponseResult addArticle(ArticleAddRequest articleAddRequest, String token) {
        // 校验参数
        if (articleAddRequest == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        if (StringUtils.isBlank(articleAddRequest.getTitle())) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE, "文章标题不能为空");
        }
        if (StringUtils.isBlank(articleAddRequest.getContent())) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE, "文章内容不能为空");
        }
        if (articleAddRequest.getChannelId() == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE, "频道不能为空");
        }
        User loginUser = userService.getLoginUser(token);
        if (loginUser == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }


        Article article = new Article();

        article.setCreateTime(new Date());
        article.setUpdateTime(new Date());
        article.setCreateUser(loginUser.getId());
        article.setUpdateUser(loginUser.getId());
        String publishTimeStr = articleAddRequest.getPublishTime();
        if (StringUtils.isNotBlank(publishTimeStr)) {
            Date publishTime = dateStrToDate(articleAddRequest.getPublishTime());
            article.setPublishTime(publishTime);
        } else {
            article.setPublishTime(new Date());
        }

        BeanUtils.copyProperties(articleAddRequest, article);

        // 富文本编辑器的img标签是原始标签，在此处需要手动把img标签的style属性style=""替换为style="width:100%;"
        String content = articleAddRequest.getContent();
        String newContent = content.replaceAll("style=\"\"", "style=\"width:100%;\"");
        article.setContent(newContent);
        // 保存文章信息
        save(article);

        // 保存文章详情
        ArticleContent articleContent = new ArticleContent();
        articleContent.setArticleId(article.getId());
        articleContent.setContent(articleAddRequest.getContent());
        articleContentService.save(articleContent);

        // 异步调用文章审核
        // 拼接文章标题和文本内容
        String text = article.getTitle() + article.getContent();
        Map<String, Object> map = new HashMap<>();
        map.put("text", text);
        map.put("article", article.getId() + "");
        Gson gson = new Gson();
        String json = gson.toJson(map);

        // 判断是否是十分钟内要发布，十分钟内要审核发布，发送到即时队列，否则保存到数据库
        long now = new Date().getTime();
        if(StringUtils.isNotBlank(publishTimeStr)
            && dateStrToDate(publishTimeStr).before(new Date(now + ARTICLE_CHECK_AFTER_MILLISECONDS))){
            // 发送到死信队列
            long publishTime = dateStrToDate(articleAddRequest.getPublishTime()).getTime();
            messageSender.sendFuture(json, (int) (publishTime - now));
        } else if(StringUtils.isBlank(articleAddRequest.getPublishTime())){
            // 发送到及时处理队列
            messageSender.sendCurrent(json);
        } else if(StringUtils.isNotBlank(publishTimeStr)
            && dateStrToDate(publishTimeStr).after(new Date(now + ARTICLE_CHECK_AFTER_MILLISECONDS))){
            // 保存到数据库
            ArticleCheck articleCheck = new ArticleCheck();
            articleCheck.setText(json);
            articleCheck.setArticleId(article.getId());
            long publishTime = dateStrToDate(articleAddRequest.getPublishTime()).getTime();
            articleCheck.setPublishTime(new Date(publishTime));
            articleCheckService.save(articleCheck);
        }
//        sensitiveWordService.handleSensitiveScan(text, article);

        // 保存到数据库
        return ResponseResult.okResult(article.getId());
    }

    private Date dateStrToDate(String publishTimeStr) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime localDateTime = LocalDateTime.parse(publishTimeStr, formatter);
        ZoneId zoneId = ZoneId.systemDefault();
        ZonedDateTime zdt = localDateTime.atZone(zoneId);
        Date publishTime = java.sql.Date.from(zdt.toInstant());
        return publishTime;
    }

    /**
     * 分页搜索查询文章列表
     *
     * @param searchText
     * @param channelId
     * @param page
     * @param size
     * @return
     */
    @Override
    public ResponseResult pageQueryArticles(String searchText, Long channelId, Long page, Long size) {
        if (page == null || page <= 0) {
            page = 1L;
        }
        if (size == null || size <= 0) {
            size = 10L;
        }
        // 设置查询条件
        LambdaQueryWrapper<Article> articleLambdaQueryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(searchText)) {
            // 标题或内容包含搜索关键词
            articleLambdaQueryWrapper.like(Article::getTitle, searchText).or()
                            .like(Article::getContent, searchText);
        }

        // 只查询审核通过的
        articleLambdaQueryWrapper.eq(Article::getStatus, ARTICLE_STATUS_PASS);

        // 频道条件
        if (channelId != null && channelId.equals(-1L) && StringUtils.isBlank(searchText)) {
            // 如果channelId是-1，表示查询热点频道的文章
            String hotArticleKey = "cc:article:hot";
            long start = (page - 1) * size;
            long end = start + size;
            Set<Long> articleIds = redisTemplate.opsForZSet().range(hotArticleKey, start, end);
            if(articleIds.size() > 0){
//                String ids = StringUtils.join(articleIds, ',');
//                articleLambdaQueryWrapper.last("ORDER BY FIELD(id," + ids + ")");
                articleLambdaQueryWrapper.in(Article::getId, articleIds);
            }
        }else if(channelId != null && !channelId.equals(0L) && !channelId.equals(-1)){

            articleLambdaQueryWrapper.eq(Article::getChannelId, channelId);
        }
        // 发布时间
        articleLambdaQueryWrapper.le(Article::getPublishTime, new Date());
        articleLambdaQueryWrapper.orderByDesc(Article::getPublishTime);
        IPage<Article> articlePage = new Page<>(page, size);

        // 查询文章列表
        articlePage = page(articlePage, articleLambdaQueryWrapper);
        // 转换为articleVo

        List<ArticleListVo> articleVos = articlePage.getRecords().stream().map(article -> {
            ArticleListVo articleListVo = new ArticleListVo();
            BeanUtils.copyProperties(article, articleListVo);
            return articleListVo;
        }).collect(Collectors.toList());
        // 返回结果
        return ResponseResult.okResult(articleVos);
    }

    /**
     * 根据id查询文章
     *
     * @param articleId
     * @param token
     * @return
     */
    @Override
    public ResponseResult getArticleById(Long articleId, String token) {
        if (articleId == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE);
        }
        Article article = getById(articleId);
        if (article == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "文章不存在");
        }
        // 关联查询发布者的用户信息
        User user = userService.getById(article.getCreateUser());
        SafetyUser creator = userService.getSafetyUser(user);
        ArticleDetailVo articleDetailVo = new ArticleDetailVo();
        articleDetailVo.setCreator(creator);
        articleDetailVo.setPublishTime(article.getPublishTime());
        BeanUtils.copyProperties(article, articleDetailVo);
        // 判断当前用户是否已点赞
        User loginUser = userService.getLoginUser(token);
        if (loginUser != null) {
            articleDetailVo.setLike(isCurrentUserLike(articleId, loginUser.getId()));
        }

        return ResponseResult.okResult(articleDetailVo);
    }

    @Resource
    private ArticleLikeService articleLikeService;

    /**
     * 用户点赞某篇文章
     *
     * @param token
     * @param articleId
     * @return
     */
    @Override
    public ResponseResult like(String token, Long articleId) {
        User loginUser = userService.getLoginUser(token);
        if (loginUser == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN, "请登录后再点赞");
        }
        Long userId = loginUser.getId();
        if (articleId == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_REQUIRE, "缺失参数");
        }

        // 查询文章是否存在
        Article article = getById(articleId);
        if (article == null) {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST, "文章不存在");
        }

        // 判断是否已经点赞过
        // 从Redis中查询
        String key = "cc:article:like:" + articleId;
        // 在内存中判断是否已点赞
        Boolean isCurrentUserLikeAtCache = isCurrentUserLikeAtCache(articleId, userId);
        if (isCurrentUserLikeAtCache) {
            // 最近一段时间内已经点赞过，取消点赞
            redisTemplate.opsForZSet().remove(key, userId);
            return ResponseResult.okResult("cancel like success");
        }
        // 从数据库判断是否已点赞
        Boolean isCurrentUserLikeAtDb = isCurrentUserLikeAtDb(articleId, userId);
        if (isCurrentUserLikeAtDb) {
            articleLikeService.remove(Wrappers.<ArticleLike>lambdaQuery()
                    .eq(ArticleLike::getArticleId, article.getId())
                    .eq(ArticleLike::getUserId, userId));
            return ResponseResult.okResult("cancel like success");
        }

        // 没有点赞过，将数据先保存到Redis
        // key: cc:article:like:articleId
        // value: userId
        // score: 当前时间戳
        redisTemplate.opsForZSet().add(key, userId, new Date().getTime());

        return ResponseResult.okResult("like success");
    }

    private Boolean isCurrentUserLike(Long articleId, Long userId) {
        return isCurrentUserLikeAtCache(articleId, userId) || isCurrentUserLikeAtDb(articleId, userId);
    }

    public Boolean isCurrentUserLikeAtCache(Long articleId, Long userId) {
        String key = "cc:article:like:" + articleId;
        Double score = redisTemplate.opsForZSet().score(key, userId);
        if (score != null) {
            // 返回结果
            return true;
        }
        return false;
    }

    public Boolean isCurrentUserLikeAtDb(Long articleId, Long userId) {
        // 查询数据库是否已经点赞
        LambdaQueryWrapper<ArticleLike> articleLikeQueryMapper = new LambdaQueryWrapper<>();
        articleLikeQueryMapper.eq(ArticleLike::getArticleId, articleId);
        articleLikeQueryMapper.eq(ArticleLike::getUserId, userId);
        ArticleLike articleLike = articleLikeService.getOne(articleLikeQueryMapper);
        if (articleLike != null) {
            // 点赞过
            // 返回结果
            return true;
        }
        return false;
    }
}




