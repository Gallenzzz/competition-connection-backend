package com.gallen.article.mq.sender;

import com.gallen.article.mq.config.RabbitmqConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MessageSender {
    @Autowired
    private RabbitTemplate rabbitTemplate;

    //使用死信队列发送消息方法封装
    public void sendFuture(String message, Integer time){
        String ttl = time < 0 ? "0" : String.valueOf(time);
        //exchange和routingKey都为业务的就可以，只需要设置消息的过期时间
        rabbitTemplate.convertAndSend(RabbitmqConfig.DEMO_EXCHANGE, RabbitmqConfig.DEMO_ROUTEKEY,message, new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                //设置消息的过期时间，是以毫秒为单位的
                message.getMessageProperties().setExpiration(ttl);
                return message;
            }
        });
        log.info("使用死信队列消息:{}发送成功,过期时间:{}秒。",message,time);
    }
    // 使用正常队列发送消息
    public void sendCurrent(String message){
        rabbitTemplate.convertAndSend(RabbitmqConfig.CURRENT_EXCHANGE, RabbitmqConfig.CURRENT_ROUTEKEY, message);
        log.info("使用正常队列消息：{}发送成功", message);
    }
}
