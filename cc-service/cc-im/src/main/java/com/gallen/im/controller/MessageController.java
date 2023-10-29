package com.gallen.im.controller;

import com.gallen.common.common.ResponseResult;
import com.gallen.pojos.user.User;
import com.gallen.api.user.InnerUserInterface;
import com.gallen.im.service.MessageService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/message")
@CrossOrigin(origins = "*", allowCredentials = "true")
public class MessageController {
    @Autowired
    private MessageService messageService;

    @DubboReference
    private InnerUserInterface userInterface;
    @GetMapping("/messages")
    public ResponseResult queryMessageList(@RequestParam("fromId") Long fromId,
                                           @RequestParam("toId") Long toId,
                                           @RequestParam(value = "page", defaultValue = "1") Integer page,
                                           @RequestParam(value = "size",defaultValue = "10") Integer size){
        return ResponseResult.okResult(messageService.queryMessageList(fromId, toId, page, size));
    }

    @GetMapping("/dubbo")
    public ResponseResult dubboTest(){
        User user = userInterface.getById(1001L);
        return ResponseResult.okResult(user);
    }

    @GetMapping("/conversation")
    public ResponseResult queryConversationList(@RequestParam("fromId") Long fromId){
        return ResponseResult.okResult(messageService.queryConversationList(fromId, 1, 10));
    }
}
