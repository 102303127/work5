package com.zhang.controller;


import com.zhang.exception.UserException;
import com.zhang.entity.User;
import com.zhang.entity.Video;
import com.zhang.service.UserLikeService;
import com.zhang.utils.DataUtils;
import com.zhang.vo.result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author zhang
 * &#064;date  2024/2/10
 * &#064;Description  实现点赞喜欢Controller层
 */
@RestController
@RequestMapping("/like")
@Slf4j
public class LikeController {

    @Autowired
    private UserLikeService userLikeService;

    @PostMapping("/action")
    public result action(@RequestParam(value = "video_id",required = false)String video_id,
                         @RequestParam(value = "comment_id",required = false)String comment_id,
                         @RequestParam("action_type") int action){
        if ((video_id.isEmpty()&&comment_id.isEmpty()))throw new UserException("请补全点赞信息");
        User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        //数据校验设置值
        String videoId = DataUtils.validation(video_id);
        String commentId = DataUtils.validation(comment_id);
        boolean flag=( action== 0) ? userLikeService.add(String.valueOf(user.getId()), videoId, commentId)
                : userLikeService.delete(String.valueOf(user.getId()), videoId, commentId);
        if (flag) {
            log.info("点赞成功");
            return result.OK();
        }
        log.error("点赞失败");
        return result.Fail();
    }

    @GetMapping("/list")
    public result list(@RequestParam("user_id") String userId,
                       @RequestParam("page_size") Integer page_size,
                       @RequestParam("page_num") Integer page_num){
        if (userId.isEmpty()) throw new UserException("请补全用户信息");
        List<Video> list = userLikeService.list(userId, page_size, page_num);
        boolean flag=list!=null;
        if(flag) {
            log.info("查询成功");
            return result.OK(list);
        }
        log.error("查询失败");
        return result.Fail();
    }
}
