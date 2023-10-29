package com.gallen.article.service;

import com.gallen.pojos.article.Article;
import com.gallen.pojos.sensitive.SensitiveWord;
import com.baomidou.mybatisplus.extension.service.IService;


public interface SensitiveWordService extends IService<SensitiveWord> {
    /**
     * 敏感词过滤
     * @param content
     * @param article
     * @return
     */
    boolean handleSensitiveScan(String content, Article article);
}
