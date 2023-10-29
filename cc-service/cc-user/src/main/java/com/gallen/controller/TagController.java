package com.gallen.controller;

import com.gallen.common.common.ResponseResult;
import com.gallen.service.TagService;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/user/tag")
@CrossOrigin(origins = "*", allowCredentials = "true")
public class TagController {

    @Resource
    private TagService tagService;
    @GetMapping("/list")
    public ResponseResult listTag(){
        return tagService.listTag();
    }

    @GetMapping("/mine")
    public ResponseResult getMyTag(HttpServletRequest request){
        String token = request.getHeader("Token");
        return ResponseResult.okResult(tagService.getMyTag(token));
    }

    @PutMapping("/update/my/tag")
    public ResponseResult updateMyTag(@RequestBody
                                                  Map<String, Object> tagListUpdate,
                                      HttpServletRequest request){
        String token = request.getHeader("Token");
        String tagNameListJson = (String) tagListUpdate.get("tagNameListJson");
        return tagService.updateMyTag(tagNameListJson, token);
    }
}
