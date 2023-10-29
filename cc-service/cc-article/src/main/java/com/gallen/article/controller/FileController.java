package com.gallen.article.controller;

import com.gallen.common.common.ResponseResult;
import com.gallen.common.enums.AppHttpCodeEnum;
import com.gallen.file.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/file")
@CrossOrigin(origins = "*")
@Slf4j
public class FileController {
    @Resource
    private FileStorageService fileStorageService;
    @PostMapping("/image")
    public ResponseResult uploadImage(MultipartFile file){
        log.error("file image - start");
        //1.检查参数
        if(file == null || file.getSize() == 0){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.上传图片到minIO中
        String fileName = UUID.randomUUID().toString().replace("-", "");
        //aa.jpg
        String originalFilename = file.getOriginalFilename();
        String postfix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileId = null;
        try {
            fileId = fileStorageService.uploadImgFile("", fileName + postfix, file.getInputStream());
            log.info("上传图片到MinIO中，fileId:{}",fileId);
        } catch (IOException e) {
            e.printStackTrace();
            log.error("WmMaterialServiceImpl-上传文件失败");
        }
        log.error("file image - start");
        return ResponseResult.okResult(fileId);
    }

    @PostMapping("/save")
    public ResponseResult save(@RequestBody Map<String, Object> html){
        System.out.println("html = " + html);
        return ResponseResult.okResult(html);
    }
}
