package com.zhang.controller;

import com.zhang.exception.UserException;
import com.zhang.entity.User;
import com.zhang.entity.Video;
import com.zhang.service.VideoService;
import com.zhang.utils.DataUtils;
import com.zhang.utils.FileUtils;
import com.zhang.vo.result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.List;

/**
 * @author zhang
 * &#064;date  2024/2/10
 * &#064;Description  视频Controller层
 */
@RestController
@RequestMapping("/video")
@Slf4j
public class VideoController {
    @Autowired
    private VideoService videoService;
    @Autowired
    private FileUtils fileUtils;

    /**
     * 视频流
     *
     * @param timestamp
     * @return
     */
    @GetMapping("/feed")
    public result feed(@RequestParam(value = "latest_time",required = false)String timestamp){
        List<Video> video = videoService.feed(timestamp);
        boolean flag=video!=null;
        if(flag) {
            log.info("拉取全部视频流");
            return result.OK(video);
        }
        log.error("拉取视频流失败");
        return result.Fail();
    }
    /**
     * 投稿
     * @param file
     * @param title
     * @param description
     * @return
     * @throws IOException
     */
    @PostMapping("/publish")
    public result publish(MultipartFile file,
                          @RequestParam("title") String title,
                          @RequestParam("description") String description) throws IOException {
        if (file.isEmpty()){
            log.error("用户传入文件有问题");
            throw new UserException("请传入正确的文件流");
        }
        String url = fileUtils.uploads(file);
        boolean flag = url != null;
        if(flag){
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User user = (User) authentication.getPrincipal();
            videoService.publish(user.getId(), url, title, description);
            log.info("投稿成功");
            return result.OK(url);
        }
        log.error("投稿失败");
        return result.Fail();
    }
    /**
     * 发布列表
     *
     * @param userId
     * @param page_num
     * @param page_size
     * @return
     */

    @GetMapping("/list")
    public result list(@RequestParam("user_id") String userId,
                       @RequestParam("page_num") Integer page_num,
                       @RequestParam("page_size") Integer page_size){
        if (userId.isEmpty()){
            log.error("指定用户为空");
            throw new UserException("指定用户ID不可为空");
        }
        List<Video> list = videoService.list(Long.parseLong(userId), page_num, page_size);
        Long total = Long.valueOf(videoService.getTotal(Long.parseLong(userId)));
        boolean flag=list!=null;
        if(flag) {
            log.info("查询用户发布列表成功");
            return result.OK(list,total);
        }
        log.info("查询用户发布列表失败");
        return result.Fail();
    }

    /**
     * 搜索视频
     * @param keywords
     * @param page_size
     * @param page_num
     * @param from_date
     * @param to_date
     * @param username
     * @return
     */
    @PostMapping("/search")
    public result search(@RequestParam("keywords") String keywords,
                         @RequestParam("page_size") Integer page_size,
                         @RequestParam("page_num") Integer page_num,
                         @RequestParam(value = "from_date",required = false) String from_date,
                         @RequestParam(value = "to_date",required = false) String to_date,
                         @RequestParam(value = "username",required = false) String username){
        String fromDate = DataUtils.validation(from_date);
        String toDate = DataUtils.validation(to_date);
        String userName = DataUtils.validation(username);
        List<Video> video = videoService.search(keywords, page_num, page_size,
                fromDate,toDate, userName);
        boolean flag=video!=null;
        if(flag) {
            log.info("搜索视频成功");
            return result.OK(video, (long) video.size());
        }
        log.error("搜索视频失败");
        return result.Fail();

    }

    /**
     * 视频热门排行榜
     * @param page_size
     * @param page_num
     * @return
     */
    @GetMapping("/popular")
    public result popular(@RequestParam("page_size") Integer page_size,
                          @RequestParam("page_num") Integer page_num){
        List<Video> video = videoService.popular(page_size, page_num);
        boolean flag=video!=null;
        if (flag) {
            log.info("查询热门排行榜成功");
            return result.OK(video);
        }
        log.error("查询热门排行榜失败");
        return result.Fail();
    }



}
