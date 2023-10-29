package com.gallen.article.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gallen.article.mapper.SensitiveWordMapper;
import com.gallen.article.service.ArticleCheckService;
import com.gallen.article.service.ArticleService;
import com.gallen.pojos.article.Article;
import com.gallen.pojos.article.ArticleCheck;
import com.gallen.pojos.sensitive.SensitiveWord;
import com.gallen.article.service.SensitiveWordService;
import com.gallen.article.utils.SensitiveWordUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.gallen.common.constants.ArticleConstants.ARTICLE_STATUS_PASS;
import static com.gallen.common.constants.ArticleConstants.ARTICLE_STATUS_UNPASS;


@Service
public class SensitiveWordServiceImpl extends ServiceImpl<SensitiveWordMapper, SensitiveWord>
    implements SensitiveWordService {
    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleCheckService articleCheckService;
    /**
     * 敏感词过滤
     *
     * @param content
     * @param article
     * @return
     */
    @Override
    public boolean handleSensitiveScan(String content, Article article) {
        boolean flag = true;
        // 查询所有敏感词
        List<SensitiveWord> sensitiveWords = sensitiveWordMapper.selectList(null);
        // 转换成String类型的敏感词列表
        List<String> stringList = sensitiveWords.stream().map(SensitiveWord::getWord).collect(Collectors.toList());

        // 初始化敏感词库
        SensitiveWordUtil.initMap(stringList);
        // 查看文本是否包含敏感词
        Map<String, Integer> map = SensitiveWordUtil.matchWords(content);
        if(map.size() > 0){
            // 审核不通过，修改文章状态
            article.setStatus(ARTICLE_STATUS_UNPASS);

            flag = false;
        }else{
            article.setStatus(ARTICLE_STATUS_PASS);
        }
        // 更新文章状态
        boolean update = articleService.update(Wrappers.<Article>lambdaUpdate()
                .eq(Article::getId, article.getId())
                .set(Article::getStatus, article.getStatus()));
        // 更新文章审核任务状态
        articleCheckService.update(Wrappers.<ArticleCheck>lambdaUpdate()
                .eq(ArticleCheck::getArticleId, article.getId())
                .set(update, ArticleCheck::getStatus, article.getStatus()));
        return flag;
    }
}




