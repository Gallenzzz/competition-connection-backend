package com.gallen.service;

import com.gallen.common.common.ResponseResult;
import com.gallen.pojos.user.Tag;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gallen.vos.user.TagVo;

import java.util.List;


public interface TagService extends IService<Tag> {

    /**
     * 查询所有标签的树形结构
     * @return
     */
    ResponseResult listTag();

    /**
     * 查询当前登录用户的标签
     * @param token
     * @return
     */
    List<TagVo> getMyTag(String token);

    /**
     * 更新我的标签
     * @param tagNameList
     * @param token
     * @return
     */
    ResponseResult updateMyTag(String tagNameList, String token);
}
