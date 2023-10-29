package com.gallen.article.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.gallen.article.mq.sender.MessageSender;
import com.gallen.article.service.ArticleLikeService;
import com.gallen.pojos.article.ArticleLike;
import com.gallen.article.mapper.ArticleMapper;
import com.gallen.article.service.ArticleCheckService;
import com.gallen.common.constants.ArticleConstants;
import com.gallen.pojos.article.ArticleCheck;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.gallen.common.constants.ArticleConstants.ARTICLE_HOT_ARTICLE_SCHEDULE_LOCK;


@Component
public class ArticleSchedule {
    private static DateTimeFormatter pattern=DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private RedissonClient redissonClient;
    @Resource
    private ArticleLikeService articleLikeService;
    @Resource
    private RedisTemplate redisTemplate;
    @Resource
    private ArticleMapper articleMapper;
    @Resource
    private MessageSender messageSender;

    @Resource
    private ArticleCheckService articleCheckService;

    @Scheduled(cron = "0 0/10 * * * ?")// 每10分钟计算一次文章热度
    public void computeHotArticle(){
        RLock lock = redissonClient.getLock(ARTICLE_HOT_ARTICLE_SCHEDULE_LOCK);
        try {
            if(lock.tryLock(0, -1, TimeUnit.MILLISECONDS)){
                Set<String> likeKeySet = redisTemplate.keys("cc:article:like:*");

                long now = new Date().getTime();
                Map<Long, Double> hotArticleMap = new HashMap<>();

                // 从Redis中读取文章点赞数据
                List<ArticleLike> articleLikeList = new ArrayList<>();
                Set<Long> articleIdSet = new HashSet<>();

                likeKeySet.stream().forEach(likeKey -> {
                    // 获取文章id
                    String articleIdStr = likeKey.substring(likeKey.lastIndexOf(":") + 1);
                    Long articleId = Long.parseLong(articleIdStr);
                    articleIdSet.add(articleId);

                    // 遍历点赞某篇文章的用户id，用集合记录文章id、用户id、点赞时间
                    Set<DefaultTypedTuple> likeValues = redisTemplate.opsForZSet().rangeWithScores(likeKey, 0, now);
                    likeValues.stream().forEach(value -> {
                        ArticleLike articleLike = new ArticleLike();
                        articleLike.setArticleId(articleId);
                        articleLike.setUserId(Long.parseLong(value.getValue().toString()));
                        articleLike.setLikeTime(new Date(value.getScore().longValue()));
                        articleLikeList.add(articleLike);
                    });

                    // 把之前的点赞数据清空
                    redisTemplate.opsForZSet().removeRangeByScore(likeKey, 0, now);
                    // 计算文章分数，每个点赞算3分
                    hotArticleMap.put(articleId,
                            hotArticleMap.getOrDefault(articleId, new Double(0)) + likeValues.size() * 3);
                });
                // 保存点赞记录到数据库
                articleLikeList.stream().forEach(articleLike -> {
                    articleLikeService.saveOrUpdate(articleLike,
                            Wrappers.<ArticleLike>lambdaUpdate()
                                    .isNull(ArticleLike::getArticleId)
                                    .isNull(ArticleLike::getUserId));
                });
                // 将文章的点赞量更新到文章信息表
                articleIdSet.stream().forEach(articleId -> {
                    // 查询文章点赞数
                    int count = articleLikeService.count(Wrappers.<ArticleLike>lambdaQuery()
                            .eq(ArticleLike::getArticleId, articleId));
                    articleMapper.updateLikes(articleId, count);
                });

                // 从Redis中读取文章浏览数
                Set<String> readKeySet = redisTemplate.keys("cc:article:read:*");
                readKeySet.stream().forEach(readKey -> {
                    // 获取文章id
                    String articleIdStr = readKey.substring(readKey.lastIndexOf(":") + 1);
                    Long articleId = Long.parseLong(articleIdStr);

                    Set<DefaultTypedTuple> readValues = redisTemplate.opsForZSet().rangeWithScores(readKey, 0, now);
                    // 计算文章的热度，每个阅读次数算1分
                    hotArticleMap.put(articleId,
                            hotArticleMap.getOrDefault(articleId, new Double(0)) + readValues.size());
                    // 将文章某段时间的浏览量累加到文章信息表
                    articleMapper.incrViews(articleId, readValues.size());
                    // 清空之前的阅读数据
                    redisTemplate.opsForZSet().removeRangeByScore(readKey, 0, now);
                });

                // 把热点文章数据保存到Redis中
                String hotArticleKey = "cc:article:hot";
                // 删除之前的热点文章id数据
                redisTemplate.delete(hotArticleKey);
                hotArticleMap.keySet().stream().forEach(articleId -> {
                    redisTemplate.opsForZSet().add(hotArticleKey, articleId
                            , hotArticleMap.getOrDefault(articleId, new Double(0)));
                });
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unlock :" + Thread.currentThread().getId());
                lock.unlock();
            }
        }

    }

    @Scheduled(cron = "0 0/10 * * * ?")// 每10分钟保存一次
    public void articleSchedulePublish(){
        LocalDateTime ldt = LocalDateTime.now();
        RLock lock = redissonClient.getLock(ArticleConstants.ARTICLE_SCHEDULE_PUBLISH_LOCK);
        try {
            if(lock.tryLock(0, -1, TimeUnit.MILLISECONDS)){
                // 查询十分钟内发布的文章
                long now = new Date().getTime();
                LambdaQueryWrapper<ArticleCheck> articleCheckLambdaQueryWrapper = new LambdaQueryWrapper<>();
                articleCheckLambdaQueryWrapper.lt(ArticleCheck::getPublishTime,
                        new Date(now + ArticleConstants.ARTICLE_CHECK_AFTER_MILLISECONDS));
                articleCheckLambdaQueryWrapper.eq(ArticleCheck::getStatus, ArticleConstants.ARTICLE_STATUS_UNCHECKED);
                List<ArticleCheck> articleCheckList = articleCheckService.list(articleCheckLambdaQueryWrapper);

                // 发送到死信队列
                articleCheckList.stream().forEach(articleCheck -> {
                    messageSender.sendFuture(articleCheck.getText(), (int) (articleCheck.getPublishTime().getTime() - now));
                });

            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            // 只能释放自己的锁
            if (lock.isHeldByCurrentThread()) {
                System.out.println("unlock :" + Thread.currentThread().getId());
                lock.unlock();
            }
        }

    }
}
