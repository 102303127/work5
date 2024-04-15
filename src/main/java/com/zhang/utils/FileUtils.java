package com.zhang.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

/**
 * @author zhang
 * &#064;date  2024/2/10
 * &#064;Description  上传文件工具类
 */
@Component
public class FileUtils {

    @Value("${image.basePath}")
    private String UPLOAD_PATH;

    @Value("${image.urlPath}")
    private String urlPrefix;


    public String uploads(MultipartFile file) throws IOException {
        String fileName = Objects.requireNonNull(file.getOriginalFilename()).substring(0,file.getOriginalFilename().lastIndexOf("."));
        final String fileSuffix = Objects.requireNonNull(file.getOriginalFilename()).substring(file.getOriginalFilename().lastIndexOf(".")+1);
        //文件名
        String filename = System.currentTimeMillis() + "." + fileName + "." + fileSuffix;

        //文件写入
        File descFile = new File(UPLOAD_PATH, filename);
        file.transferTo(descFile);

        return urlPrefix + "/image/" + filename;

    }
}
