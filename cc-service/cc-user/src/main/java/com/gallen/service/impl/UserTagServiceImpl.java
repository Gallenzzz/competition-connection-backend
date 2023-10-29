package com.gallen.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gallen.mapper.UserTagMapper;
import com.gallen.pojos.user.UserTag;
import com.gallen.service.UserTagService;
import org.springframework.stereotype.Service;


@Service
public class UserTagServiceImpl extends ServiceImpl<UserTagMapper, UserTag>
    implements UserTagService {

}




