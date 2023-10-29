package com.gallen.im.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gallen.pojos.user.User;
import com.gallen.api.user.InnerUserInterface;
import com.gallen.im.dao.MessageDao;
import com.gallen.im.mapper.MessageMapper;
import com.gallen.im.service.MessageService;
import com.gallen.pojos.im.Message;
import com.gallen.vos.im.ConversationVo;
import com.gallen.vos.im.MessageVo;
import com.gallen.vos.user.SafetyUser;

import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl extends ServiceImpl<MessageMapper, Message>
        implements MessageService {
    @Autowired
    private MessageDao messageDao;

    @Autowired
    private MessageMapper messageMapper;

    @DubboReference
    private InnerUserInterface userService;
    /**
     * 查询历史信息
     *
     * @param fromId
     * @param toId
     * @param page
     * @param size
     * @return
     */
    @Override
    public List<MessageVo> queryMessageList(Long fromId, Long toId, Integer page, Integer size) {
        LambdaQueryWrapper<Message> messageLambdaQueryWrapper = new LambdaQueryWrapper<>();
        messageLambdaQueryWrapper.eq(Message::getFromId, fromId);
        messageLambdaQueryWrapper.eq(Message::getToId, toId);
        messageLambdaQueryWrapper.or();
        messageLambdaQueryWrapper.eq(Message::getToId, fromId);
        messageLambdaQueryWrapper.eq(Message::getFromId, toId);
        Page<Message> messagePage = page(new Page<>(page, size), messageLambdaQueryWrapper);
        List<Message> messageList = messagePage.getRecords();
//        List<Message> messageList = messageDao.findListByFromAndTo(fromId, toId, page, size);
        if(messageList.size() == 0){
            return null;
        }
        List<MessageVo> messages = messageList.stream().map(message -> {
            // 根据userId查询用户信息
            User fromUser = userService.getById(message.getFromId());
            SafetyUser fromSafetyUser = userService.getSafetyUser(fromUser);
            User toUser = userService.getById(message.getToId());
            SafetyUser toSafetyUser = userService.getSafetyUser(toUser);

            if (message.getState() == 0 && message.getToId().equals(fromId)) {
                // 如果消息未读，修改成已读
//                messageDao.updateMessageState(message.getId(), 1);
                message.setState(1);
                messageMapper.update(message, Wrappers.<Message>lambdaUpdate().eq(Message::getId, message.getId()));
            }
            MessageVo messageVo = new MessageVo();
            messageVo.setFromUser(fromSafetyUser);
            messageVo.setToUser(toSafetyUser);
            messageVo.setId(message.getId());
            messageVo.setMessage(message.getMessage());
            messageVo.setReadDate(message.getReadTime());
            messageVo.setSendDate(message.getSendDate());
            messageVo.setState(message.getState());
            messageVo.setId(message.getId());
            return messageVo;
        }).collect(Collectors.toList());
        return messages;
    }

    /**
     * 查询会话列表
     *
     * @param fromId
     * @param page
     * @param size
     * @return
     */
    @Override
    public List<ConversationVo> queryConversationList(Long fromId, Integer page, Integer size) {
        return messageDao.findListByFrom(fromId, page, size);
    }
}
