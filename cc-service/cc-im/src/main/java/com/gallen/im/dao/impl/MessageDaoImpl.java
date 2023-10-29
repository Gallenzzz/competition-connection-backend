package com.gallen.im.dao.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.gallen.im.mapper.MessageMapper;
import com.gallen.pojos.im.Message;
import com.gallen.pojos.user.User;
import com.gallen.api.user.InnerUserInterface;
import com.gallen.im.dao.MessageDao;
import com.gallen.vos.im.ConversationVo;
import com.gallen.vos.user.SafetyUser;

import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class MessageDaoImpl implements MessageDao {

    @DubboReference
    private InnerUserInterface userService;

    @Autowired
    private MessageMapper messageMapper;


    /**
     * 查询登录用户的聊天记录
     *
     * @param fromId
     * @param page
     * @param size
     * @return
     */
    @Override
    public List<ConversationVo> findListByFrom(Long fromId, Integer page, Integer size) {
        // 查询当前登录用户与其他所有用户的会话记录
        // 查询条件
        LambdaQueryWrapper<Message> queryWrapper = new LambdaQueryWrapper<Message>().eq(Message::getFromId, fromId)
                .or().eq(Message::getToId, fromId);
        queryWrapper.orderByAsc(Message::getSendDate);
        List<Message> messages = messageMapper.selectList(queryWrapper);

        // key: 对方的id value: message, sendDate, unreadNum
        Map<Long, Object> conversationPartnerMap = new HashMap<>();
        messages.stream().map(message -> {
            Long partnerId = null;
            if(message.getFromId().equals(fromId)){
                partnerId = message.getToId();
            }else if(message.getToId().equals(fromId)){
                partnerId = message.getFromId();
            }
            // message, sendDate, unreadNum
            Map<String, Object> lastMessageMap = new HashMap<>();
            lastMessageMap.put("message", message.getMessage());
            lastMessageMap.put("sendDate", message.getSendDate());
            Map<String, Object> conversationMap= (Map<String, Object>) conversationPartnerMap.get(partnerId);
            Integer unreadNum = 0;
            if(message.getState() == 0 && message.getToId().equals(fromId)){
                // 有消息未读，统计未读消息数量
                if(conversationMap != null ){
                    unreadNum = (Integer) conversationMap.getOrDefault("unreadNum", 1) + 1;
                    lastMessageMap.put("unreadNum", unreadNum);
                }else{
                    lastMessageMap.put("unreadNum", 1);
                }
            }else{
                lastMessageMap.put("unreadNum", unreadNum);
            }

            conversationPartnerMap.put(partnerId, lastMessageMap);
            return null;
        }).collect(Collectors.toList());
        // 遍历 conversationPartnerMap 获取keySet
        Set<Long> partnerIds = conversationPartnerMap.keySet();
        List<ConversationVo> conversationVoList = new ArrayList<>();
        for (Long partnerId : partnerIds) {
            // 查询用户信息
            if(partnerId != null){
                User user = userService.getById(partnerId);
                SafetyUser safetyUser = userService.getSafetyUser(user);
                ConversationVo conversationVo = new ConversationVo();
                Map<String, Object> lastMessage = (Map<String, Object>) conversationPartnerMap.get(partnerId);
                conversationVo.setMessage(lastMessage.get("message").toString());
                conversationVo.setSendDate((Date) lastMessage.get("sendDate"));
                conversationVo.setUnreadNum((Integer) lastMessage.get("unreadNum"));
                conversationVo.setUser(safetyUser);
                conversationVoList.add(conversationVo);
            }
        }

        return conversationVoList;
    }

    /**
     * 根据id查询消息
     *
     * @param id
     * @return
     */
    @Override
    public Message findMessageById(String id) {

        return null;
    }

    /**
     * 根据id更新消息状态
     *
     * @param id
     * @param state
     * @return
     */
    @Override
    public Integer updateMessageState(Long id, Integer state) {

        Message message = new Message();
        message.setState(state);
        return messageMapper.update(message, Wrappers.<Message>lambdaUpdate().eq(Message::getId, id));

    }

    /**
     * 新增消息
     *
     * @param
     * @return
     */
    @Override
    public Message saveMessage(Message message) {
        // 设置发送时间
        message.setSendDate(new Date());
        // 设置消息状态为未读
        message.setState(0);
        messageMapper.insert(message);
        return message;
    }

}
