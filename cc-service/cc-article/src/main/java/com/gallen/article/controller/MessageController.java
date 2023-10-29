package com.gallen.article.controller;
import com.gallen.article.mq.sender.MessageSender;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("dlx")
public class MessageController {

    @Autowired
    public MessageSender messageSender;

    //死信队列controller
    @GetMapping("/sendfuture")
    public String send(@RequestParam String msg,Integer time){
        messageSender.sendFuture(msg,time);
        return "ok future";
    }

    //正常队列controller
    @GetMapping("/sendcurrent")
    public String sendCurrent(@RequestParam String msg,Integer time){
        messageSender.sendCurrent(msg);
        return "ok current";
    }

    //延迟插件controller
//    @GetMapping("/send2")
//    public String sendByPlugin(@RequestParam String msg,Integer time){
//        messageSender.send2(msg,time);
//        return "ok";
//    }

}
