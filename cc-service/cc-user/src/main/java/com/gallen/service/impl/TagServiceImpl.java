package com.gallen.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gallen.common.common.ResponseResult;
import com.gallen.jwt.AppJwtUtil;
import com.gallen.mapper.TagMapper;
import com.gallen.pojos.user.Tag;
import com.gallen.pojos.user.User;
import com.gallen.pojos.user.UserTag;
import com.gallen.service.TagService;
import com.gallen.service.UserService;
import com.gallen.service.UserTagService;
import com.gallen.vos.user.SafetyUser;
import com.gallen.vos.user.TagVo;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import io.jsonwebtoken.Claims;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
        implements TagService {
    @Resource
    private UserService userService;
    /**
     * 查询所有标签的树形结构
     *
     * @return
     */
    @Override
    public ResponseResult listTag() {
        // 查询所有标签
        List<Tag> tagList = list(null);
        // 转换成tagVoList
        List<TagVo> tagVoList = new ArrayList<>();
        for (Tag tag : tagList) {
            TagVo tagVo = new TagVo();
            BeanUtils.copyProperties(tag, tagVo);
            tagVo.setText(tag.getTagName());
            tagVo.setTagId(tag.getId());
            tagVoList.add(tagVo);
        }
        List<TagVo> parentList = tagVoList.stream()
                .filter(tagVo -> tagVo.getParentId().equals(0L))
                .peek(tagVo -> tagVo.setChildren(listChildren(tagVo, tagVoList)))
                .collect(Collectors.toList());
//        parentList.stream().forEach(tagVo -> tagVo.setChildren(listChildren(tagVo, tagVoList)));
        return ResponseResult.okResult(parentList);
    }

    /**
     * 查询当前登录用户的标签
     *
     * @param token
     * @return
     */
    @Override
    public List<TagVo> getMyTag(String token) {
        // 从token中解析出userId
        Long userId = null;
        if (StringUtils.isNotBlank(token)) {
            Claims claimsBody = AppJwtUtil.getClaimsBody(token);
            //是否是过期
            int result = AppJwtUtil.verifyToken(claimsBody);
            // -1：有效，0：有效，1：过期，2：过期
            if (result == 1 || result == 2) {
                // 过期，请重新登录
                return null;
            }
            userId = claimsBody.get("id") instanceof Long ? (Long) claimsBody.get("id") : (Integer) claimsBody.get("id");

        }
        // 查询当前用户信息
        SafetyUser safetyUser = userService.getSafetyUser(token);
        String tag = safetyUser.getTag();
        Gson gson = new Gson();
        List<TagVo> tagList = gson.fromJson(tag, new TypeToken<List<String>>() {
        }.getType());
        return tagList;
    }

    /**
     * 更新我的标签
     *
     * @param tagNameList
     * @param token
     * @return
     */
    @Resource
    private UserTagService userTagService;
    @Override
    public ResponseResult updateMyTag(String tagNameList, String token) {
        User loginUser = userService.getLoginUser(token);

        User user = new User();
        user.setId(loginUser.getId());
        user.setTag(tagNameList);
        // 更新用户信息表中的标签JSON字符串
        int update = userService.updateUser(user, loginUser);

        // 删除用户标签关系表中该用户的所有标签信息
        userTagService.remove(Wrappers.<UserTag>lambdaQuery()
                .eq(UserTag::getUserId, loginUser.getId()));

        // 重新向用户标签关系表中插入标签
        Gson gson = new Gson();
        List<String> tags = gson.fromJson(tagNameList, new TypeToken<List<String>>() {
        }.getType());
        List<UserTag> userTagList = new ArrayList<>();
        tags.stream().forEach(tag -> {
            UserTag userTag = new UserTag();
            userTag.setUserId(loginUser.getId());
            // 查询标签id
            Tag tagObj = getOne(Wrappers.<Tag>lambdaQuery()
                    .eq(Tag::getTagName, tag));
            userTag.setTagId(tagObj.getId());
            userTagList.add(userTag);
        });

        userTagService.saveBatch(userTagList);

        return ResponseResult.okResult(update);

    }

    public List<TagVo> listChildren(TagVo parent, List<TagVo> tagVoList) {
        List<TagVo> collect = tagVoList.stream()
                .filter(tagVo -> tagVo.getParentId().equals(parent.getTagId()))
                .peek(tagVo -> {
                    tagVo.setChildren(listChildren(tagVo, tagVoList));
                    tagVo.setId(tagVo.getText());
                })
                .collect(Collectors.toList());
        return collect;
    }
}




