package com.gallen.dtos.team;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.gallen.common.common.PageRequestDto;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class TeamQuery extends PageRequestDto {
    /**
     *
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 队伍id列表
     */
    private List<Long> teamIdList;

    /**
     * 队伍名称
     */
    private String name;

    /**
     * 队伍描述
     */
    private String description;

    /**
     * 搜素的关键词(同时搜索名称和描述)
     */
    private String searchText;

    /**
     * 队伍最大人数
     */
    private Integer maxNum;

    /**
     * 队伍过期时间
     */
    private Date expireTime;

    /**
     * 创建者id
     */
    private Long userid;

    /**
     * 0 - 公开， 1 - 私有， 2 - 加密
     */
    private Integer status;
}
