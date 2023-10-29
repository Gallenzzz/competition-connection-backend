package com.gallen.team.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gallen.pojos.team.UserTeam;
import com.gallen.team.mapper.UserTeamMapper;
import com.gallen.team.service.UserTeamService;
import org.springframework.stereotype.Service;


@Service
public class UserTeamServiceImpl extends ServiceImpl<UserTeamMapper, UserTeam>
    implements UserTeamService {

}




