package com.gallen.im.service;

import com.baomidou.mybatisplus.extension.service.IService;

import com.gallen.pojos.im.Message;
import com.gallen.vos.im.ConversationVo;
import com.gallen.vos.im.MessageVo;

import java.util.List;

public interface MessageService extends IService<Message> {
    /**
     * 查询历史信息
     * @param fromId
     * @param toId
     * @param page
     * @param size
     * @return
     */
    List<MessageVo> queryMessageList(Long fromId, Long toId, Integer page, Integer size);

    /**
     * 查询会话列表
     * @param fromId
     * @param page
     * @param size
     * @return
     */
    List<ConversationVo> queryConversationList(Long fromId, Integer page, Integer size);
}
