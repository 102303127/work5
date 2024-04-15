package com.zhang.controller;


import com.zhang.exception.UserException;
import com.zhang.entity.User;
import com.zhang.service.UserService;
import com.zhang.utils.FileUtils;
import com.zhang.vo.result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author zhang
 * &#064;date  2024/2/10
 * &#064;Description  文件上传并校验Controller层
 */
@RestController
@Slf4j
public class FileController {

    @Autowired
    FileUtils fileUtils;
    @Autowired
    UserService userService;
    /**
     * 先对文件头像进行检查是否是图片，在上传到本地仓库
     *
     * @param file
     * @return
     * @throws IOException
     */
    @PutMapping("/avatar/uploads")
    public result update(@RequestBody MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            log.error("用户未上传文件");
            throw new UserException("请选中文件");
        }
        // 原始文件名称
        String fileName = file.getOriginalFilename();

        // 解析到文件后缀
        int index = fileName.lastIndexOf(".");
        String suffix;
        if (index == -1 || (suffix = fileName.substring(index + 1)).isEmpty()) {
            log.error("文件格式类型错误");
            String msg= "文件后缀不能为空";
            return new result(-1,msg);
        }
        // 允许上传的文件后缀列表
        Set<String> allowSuffix = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "gif"));
        if (!allowSuffix.contains(suffix.toLowerCase())) {
            log.error("文件格式类型错误");
            String msg="非法的文件，不允许的文件类型：" + suffix;
            return new result(-1,msg);
        }

        String url=fileUtils.uploads(file);
        boolean flag= url!=null;
        if(flag){
            User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            user.setAvatarUrl(url);
            userService.update(user);
            log.info("用户头像上传成功");
            return result.OK(url);
        }

        return result.Fail("文件上传失败");
    }

}
