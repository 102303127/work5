package com.zhang.service.Impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhang.exception.UserException;
import com.zhang.mapper.UserMapper;
import com.zhang.mapper.VideoMapper;
import com.zhang.entity.User;
import com.zhang.entity.Video;
import com.zhang.service.VideoService;
import com.zhang.utils.DataUtils;
import com.zhang.utils.RedisUtil;
import com.zhang.utils.timeChangeUtils;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.*;

@Service
public class VideoServiceImpl implements VideoService {

    @Autowired
    private VideoMapper videoMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private RedisUtil redisUtil;

    @Value("${image.urlPath}")
    private String urlPath;
    /**
     * 获取首页视频流
     *
     * @param timestamp
     * @return
     */
    @Override
    public List<Video> feed(String timestamp) {
        QueryWrapper<Video> queryWrapper=new QueryWrapper<>();

        if (!Objects.equals(timestamp, "")) {
            String time = timeChangeUtils.timeStampDate(timestamp);
            queryWrapper.gt("created_at",time);
            return videoMapper.selectList(queryWrapper);
        } else {
            return videoMapper.selectList(null);
        }
    }

    /**
     * 投稿
     *
     * @param userId
     * @param video_url
     * @param title
     * @param description
     * @return
     */
    @Override
    public boolean publish(Long userId, String video_url, String title, String description) {
        //检测视频是否上传过
        QueryWrapper<Video> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("video_url", video_url);
        queryWrapper.eq("user_id",userId);
        if (videoMapper.selectOne(queryWrapper)!=null) {
            throw new UserException("您已经上传过该视频");
        }
        //生成新的视频
        Video video = new Video();
        video.setUserId(userId);
        video.setVideoUrl(video_url);
        video.setTitle(title);
        video.setDescription(description);
        video.setCreatedAt(new Date());
        return videoMapper.insert(video) != 0;
    }

    /**
     * 根据 user_id 查看指定人的发布列表
     *
     * @param userId
     * @param page_num
     * @param page_size
     * @return
     */
    @Override
    public List<Video> list(long userId, Integer page_num, Integer page_size) {
        QueryWrapper<Video> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        IPage<Video> page = new Page<>(page_num, page_size);
        IPage<Video> iPage = videoMapper.selectPage(page, queryWrapper);
        return iPage.getRecords();
    }

    /**
     * 根据用户ID获得用户发布视频列表
     *
     * @param userId
     * @return
     */
    @Override
    public Integer getTotal(long userId) {
        QueryWrapper<Video> queryWrapper=new QueryWrapper<>();
        queryWrapper.eq("user_id",userId);
        return videoMapper.selectCount(queryWrapper);
    }

    /**
     * 搜索指定关键字的视频，将会从以下字段进行搜索
     * 标题（title）
     * 描述（description）
     *
     * @param keywords
     * @param page_num
     * @param page_size
     * @param from_date
     * @param to_date
     * @param username
     * @return
     */
    @Override
    public List<Video> search(String keywords, Integer page_num,
                              Integer page_size, String from_date,
                              String to_date, String username) {
        QueryWrapper<Video> queryWrapper=new QueryWrapper<>();
        queryWrapper.like("title",keywords).or().
                like("description",keywords);
        if (DataUtils.judge(from_date)) {
            String fromTime = timeChangeUtils.timeStampDate(from_date);
            queryWrapper.gt("created_at",fromTime);
        }
        if (DataUtils.judge(to_date)) {
            String toTime = timeChangeUtils.timeStampDate(to_date);
            queryWrapper.lt("created_at",toTime);
        }
        if (DataUtils.judge(username)){
            User user = userMapper.selectByName(username);
            queryWrapper.eq("user_id",user.getId());
        }
        IPage<Video> page = new Page<>(page_num, page_size);
        IPage<Video> iPage = videoMapper.selectPage(page, queryWrapper);
        return iPage.getRecords();
    }

    /**
     * 存储到redis的排序方法
     * @param page_size
     * @param page_num
     * @return
     */
    @Override
    public List<Video> popular(Integer page_size, Integer page_num) {
        //存储所有视频id到redis，已经存在的更新覆盖
        String key = "热门排行榜-video";
        List<Video> videos = videoMapper.selectList(null);
        for (Video video : videos) {
            redisUtil.addScore(key,String.valueOf(video.getId()),video.getVisitCount());
        }
        //提取信息
        Set<Object> videoIds = redisUtil.getAllMembers(key);
        ArrayList<Video> videos1 = new ArrayList<>();
        for (Object videoId : videoIds) {
            Video video = videoMapper.selectById((Serializable) videoId);
            videos1.add(video);
        }
        //实现对List的分页查询
        int startIndex = (page_num - 1) * page_size;
        int endIndex = Math.min(startIndex + page_size, videos1.size());
        return videos1.subList(startIndex, endIndex);
    }

    @Override
    public void addVisitCount(String pathInfo) {
        QueryWrapper<Video> queryWrapper = new QueryWrapper<>();
        String videoUrl = urlPath + pathInfo;
        queryWrapper.eq("video_url", videoUrl);
        List<Video> videos = videoMapper.selectList(queryWrapper);
        for (Video video : videos) {
            video.setVisitCount(video.getVisitCount()+1);
            video.setUpdatedAt(new Date());
            videoMapper.updateById(video);
        }
    }


}