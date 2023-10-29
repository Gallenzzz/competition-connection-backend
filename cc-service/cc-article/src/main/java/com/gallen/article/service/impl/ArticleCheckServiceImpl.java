package com.gallen.article.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gallen.article.service.ArticleCheckService;
import com.gallen.article.mapper.ArticleCheckMapper;
import com.gallen.pojos.article.ArticleCheck;
import org.springframework.stereotype.Service;

@Service
public class ArticleCheckServiceImpl extends ServiceImpl<ArticleCheckMapper, ArticleCheck>
    implements ArticleCheckService {

}




