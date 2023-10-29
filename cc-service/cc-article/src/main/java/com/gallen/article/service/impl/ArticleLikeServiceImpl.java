package com.gallen.article.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gallen.article.service.ArticleLikeService;
import com.gallen.article.mapper.ArticleLikeMapper;
import com.gallen.pojos.article.ArticleLike;
import org.springframework.stereotype.Service;


@Service
public class ArticleLikeServiceImpl extends ServiceImpl<ArticleLikeMapper, ArticleLike>
    implements ArticleLikeService {

}




