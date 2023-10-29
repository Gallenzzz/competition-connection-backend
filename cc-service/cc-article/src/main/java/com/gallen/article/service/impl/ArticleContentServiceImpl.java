package com.gallen.article.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gallen.article.mapper.ArticleContentMapper;
import com.gallen.article.service.ArticleContentService;
import com.gallen.pojos.article.ArticleContent;
import org.springframework.stereotype.Service;


@Service
public class ArticleContentServiceImpl extends ServiceImpl<ArticleContentMapper, ArticleContent>
    implements ArticleContentService {

}




