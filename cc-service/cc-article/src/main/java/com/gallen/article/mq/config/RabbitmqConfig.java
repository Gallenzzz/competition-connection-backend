package com.gallen.article.mq.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitmqConfig {

    //死信交换机，队列，路由相关配置
    public static final String DLK_EXCHANGE = "dlk.exchange";
    public static final String DLK_ROUTEKEY = "dlk.routeKey";
    public static final String DLK_QUEUE = "dlk.queue";

    //业务交换机，队列，路由相关配置
    public static final String DEMO_EXCHANGE = "demo.exchange";
    public static final String DEMO_QUEUE = "demo.queue";
    public static final String DEMO_ROUTEKEY = "demo.routeKey";

    //处理当前业务交换机，队列，路由相关配置
    public static final String CURRENT_EXCHANGE = "current.exchange";
    public static final String CURRENT_QUEUE = "current.queue";
    public static final String CURRENT_ROUTEKEY = "current.routeKey";

    @Bean
    public DirectExchange demoExchange(){
        return new DirectExchange(DEMO_EXCHANGE,true,false);
    }

    @Bean
    public Queue demoQueue(){
        //只需要在声明业务队列时添加x-dead-letter-exchange，值为死信交换机
        Map<String,Object> map = new HashMap<>(1);
        map.put("x-dead-letter-exchange",DLK_EXCHANGE);
        //该参数x-dead-letter-routing-key可以修改该死信的路由key，不设置则使用原消息的路由key
        map.put("x-dead-letter-routing-key",DLK_ROUTEKEY);
        return new Queue(DEMO_QUEUE,true,false,false, map);
    }

    @Bean
    public Binding demoBind(){
        return BindingBuilder.bind(demoQueue()).to(demoExchange()).with(DEMO_ROUTEKEY);
    }

    @Bean
    public DirectExchange dlkExchange(){
        return new DirectExchange(DLK_EXCHANGE,true,false);
    }

    @Bean
    public Queue dlkQueue(){
        return new Queue(DLK_QUEUE,true,false,false);
    }

    @Bean
    public Binding dlkBind(){
        return BindingBuilder.bind(dlkQueue()).to(dlkExchange()).with(DLK_ROUTEKEY);
    }

    @Bean
    public DirectExchange currentExchange(){
        return new DirectExchange(CURRENT_EXCHANGE,true,false);
    }

    @Bean
    public Queue currentQueue(){
        return new Queue(CURRENT_QUEUE,true,false,false);
    }

    @Bean
    public Binding currentBind(){
        return BindingBuilder.bind(currentQueue()).to(currentExchange()).with(CURRENT_ROUTEKEY);
    }
}
