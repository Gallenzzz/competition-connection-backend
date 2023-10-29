package com.gallen.article.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gallen.article.mapper.TaskInfoMapper;
import com.gallen.article.service.TaskInfoService;
import com.gallen.pojos.article.TaskInfo;
import org.springframework.stereotype.Service;


@Service
public class TaskInfoServiceImpl extends ServiceImpl<TaskInfoMapper, TaskInfo>
    implements TaskInfoService {

}




