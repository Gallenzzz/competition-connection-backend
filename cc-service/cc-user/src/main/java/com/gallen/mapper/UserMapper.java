package com.gallen.mapper;

import com.gallen.pojos.user.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.mapping.ResultSetType;
import org.apache.ibatis.session.ResultHandler;


@Mapper
public interface UserMapper extends BaseMapper<User> {
    @Select("select id, tag from tb_user where id != #{loginUserId}")
    @Options(resultSetType = ResultSetType.FORWARD_ONLY, fetchSize = 1000)
    @ResultType(User.class)
    void streamQuery(@Param("loginUserId") Long loginUserId, ResultHandler<User> handler);
}




