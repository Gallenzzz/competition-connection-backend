package com.gallen.article.mapper;

import com.gallen.pojos.article.Article;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;


@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
    void incrViews(Long articleId, Integer incr);

    void updateLikes(Long articleId, Integer likes);
}




