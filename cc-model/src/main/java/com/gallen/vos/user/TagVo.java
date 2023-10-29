package com.gallen.vos.user;

import com.gallen.common.annotation.IdEncrypt;
import lombok.Data;

import java.util.List;

@Data
public class TagVo {
    /**
     * 唯一标识
     */
    @IdEncrypt
    private Long tagId;

    /**
     * 标签名称
     */
    private String text;

    /**
     * 父标签id
     */
    private Long parentId;

    /**
     * 子标签
     */
    private List<TagVo> children;

    private String id;
}
