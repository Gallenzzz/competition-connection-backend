package com.gallen.article.mq.consumer;

import com.gallen.article.service.ArticleService;
import com.gallen.article.mq.config.RabbitmqConfig;
import com.gallen.article.service.SensitiveWordService;
import com.gallen.pojos.article.Article;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

@Component
@Slf4j
public class MessageReceiver {

    @Resource
    private SensitiveWordService sensitiveWordService;

    @Resource
    private ArticleService articleService;
    @RabbitHandler
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(
                    value = RabbitmqConfig.DLK_QUEUE,
                    durable = "true",
                    ignoreDeclarationExceptions = "true"),
            exchange = @Exchange(
                    value = RabbitmqConfig.DLK_EXCHANGE,
                    type = ExchangeTypes.DIRECT,
                    durable = "true"),
            key = RabbitmqConfig.DLK_ROUTEKEY,
            ignoreDeclarationExceptions = "true"
    ))
    public void onFutureMessage(Message message,Channel channel){

        try {
            handleScanText(message);
            log.info("使用死信队列，收到消息:{}",new String(message.getBody()));
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (IOException e) {
            e.printStackTrace();
            try {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }finally {
            try {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @RabbitListener(queues = RabbitmqConfig.CURRENT_QUEUE, ackMode = "MANUAL")
    public void onCurrentMessage(Message message, Channel channel){
        try {
            handleScanText(message);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, false);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }finally {
            try {
                channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        log.info("使用正常队列，收到消息：{}", new String(message.getBody()));
    }

    public void handleScanText(Message message){
        String messageStr = new String(message.getBody());
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String,Object>>(){}.getType();
        Map<String, Object> map = gson.fromJson(messageStr, type);
        String text = (String) map.get("text");
        String articleId = (String) map.get("article");
        Article article = articleService.getById(Long.valueOf(articleId));
        // 审核

        sensitiveWordService.handleSensitiveScan(text, article);
    }
}
