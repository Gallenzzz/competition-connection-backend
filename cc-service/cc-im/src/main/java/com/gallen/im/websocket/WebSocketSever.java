package com.gallen.im.websocket;


import com.gallen.im.utils.SpringUtil;
import com.gallen.im.dao.MessageDao;
import com.gallen.pojos.im.Message;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint("/ws/{userId}")
@Component
@CrossOrigin
@Slf4j
public class WebSocketSever {

    private MessageDao messageDao = SpringUtil.getBean(MessageDao.class);

    private RabbitTemplate rabbitTemplate = SpringUtil.getBean(RabbitTemplate.class);

    // 与某个客户端的连接会话，需要通过它来给客户端发送数据
    private Session session;

    // session集合,存放对应的session
    private static ConcurrentHashMap<Long, Session> sessionPool = new ConcurrentHashMap<>();

    // concurrent包的线程安全Set,用来存放每个客户端对应的WebSocket对象。
    private static CopyOnWriteArraySet<WebSocketSever> webSocketSet = new CopyOnWriteArraySet<>();

    /**
     * 建立WebSocket连接
     *
     * @param session
     * @param userId 用户ID
     */
    @OnOpen
    public void onOpen(Session session, @PathParam(value = "userId") Long userId) {
        log.info("WebSocket建立连接中,连接用户ID：{}", userId);
        try {
            Session historySession = sessionPool.get(userId);
            // historySession不为空,说明已经有人登陆账号,应该删除登陆的WebSocket对象
            if (historySession != null) {
                webSocketSet.remove(historySession);
                historySession.close();
            }
        } catch (IOException e) {
            log.error("重复登录异常,错误信息：" + e.getMessage(), e);
        }
        // 建立连接
        this.session = session;
        webSocketSet.add(this);
        sessionPool.put(userId, session);
        log.info("建立连接完成,当前在线人数为：{}", webSocketSet.size());
    }

    /**
     * 发生错误
     *
     * @param throwable e
     */
    @OnError
    public void onError(Throwable throwable) {
        throwable.printStackTrace();
    }

    /**
     * 连接关闭
     */
    @OnClose
    public void onClose() {
        webSocketSet.remove(this);
        log.info("连接断开,当前在线人数为：{}", webSocketSet.size());
    }

    /**
     * 接收客户端消息
     *
     * @param message 接收的消息
     */
    @OnMessage
    public void onMessage(String message) {
        log.info("收到客户端发来的消息：{}", message);
        Gson gson = new Gson();
        Map map = gson.fromJson(message, Map.class);
        Long fromId = Long.parseLong((String) map.getOrDefault("fromId", "1000"));
        Long toId = Long.parseLong((String) map.getOrDefault("toId", "1000"));

        String msg = (String) map.getOrDefault("msg", "");

        Message messageObj = new Message();
        messageObj.setMessage(msg);
        messageObj.setSendDate(new Date());
        messageObj.setState(0);
        messageObj.setFromId(fromId);
        messageObj.setToId(toId);
        messageObj = messageDao.saveMessage(messageObj);

        String messageJson = gson.toJson(messageObj);

        // 判断to用户是否在线
        Session toSession = sessionPool.get(toId);
        if(toSession != null && toSession.isOpen()){
            // 发送消息给用户
            sendMessageByUser(toId, messageJson);
            // 将消息状态改为已读
            messageDao.updateMessageState(messageObj.getId(), 1);
        }else{
            // 用户不在线，或不在本JVM中，把消息发送给消息队列
            rabbitTemplate.convertAndSend("cc.fanout", "", messageJson);
        }

        // 告诉消息发送者消息发送成功
        sendMessageByUser(fromId, messageJson);
    }

    /**
     * 推送消息到指定用户
     *
     * @param userId  用户ID
     * @param message 发送的消息
     */
    public static void sendMessageByUser(Long userId, String message) {
        log.info("用户ID：" + userId + ",推送内容：" + message);
        Session session = sessionPool.get(userId);
        try {
            session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            log.error("推送消息到指定用户发生错误：" + e.getMessage(), e);
        }
    }

    /**
     * 群发消息
     *
     * @param message 发送的消息
     */
    public static void sendAllMessage(String message) {
        log.info("发送消息：{}", message);
        for (WebSocketSever webSocket : webSocketSet) {
            try {
                webSocket.session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                log.error("群发消息发生错误：" + e.getMessage(), e);
            }
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(),
            exchange = @Exchange(value = "cc.fanout", type = ExchangeTypes.FANOUT)
    ))
    public void listenRabbitMQ(String message){
        // 从信息中解析出userId
        Gson gson = new Gson();
        Message messageObj = gson.fromJson(message, Message.class);
        Long userId = messageObj.getToId();

        // 查询本JVM中是否有在线
        Session session = sessionPool.get(userId);
        if(session == null){
            return;
        }
        try {
            session.getBasicRemote().sendText(message);
            log.info("to用户ID：" + userId + ",推送内容：" + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

