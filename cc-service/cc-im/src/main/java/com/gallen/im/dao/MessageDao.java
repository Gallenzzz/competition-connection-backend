package com.gallen.im.dao;


import com.gallen.pojos.im.Message;
import com.gallen.vos.im.ConversationVo;

import java.util.List;

public interface MessageDao {


    /**
     * 查询登录用户的聊天记录
     * @param fromId
     * @param page
     * @param size
     * @return
     */
    List<ConversationVo> findListByFrom(Long fromId, Integer page, Integer size);

    /**
     * 根据id查询消息
     * @param id
     * @return
     */
    Message findMessageById(String id);

    /**
     * 根据id更新消息状态
     * @param id
     * @return
     */
    Integer updateMessageState(Long id, Integer state);

    /**
     * 新增消息
     * @param message
     * @return
     */
    Message saveMessage(Message message);

}
