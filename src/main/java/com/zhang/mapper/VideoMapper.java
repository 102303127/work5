package com.zhang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhang.entity.Video;
import org.apache.ibatis.annotations.*;


@Mapper
public interface VideoMapper extends BaseMapper<Video>{
    /*@Results(id="VideoMapper",value = {
            @Result(column="created_at",property="created_at",jdbcType= JdbcType.TIMESTAMP),
            @Result(column="updated_at",property="updated_at",jdbcType= JdbcType.TIMESTAMP),
            @Result(column="deleted_at",property="deleted_at",jdbcType= JdbcType.TIMESTAMP),
            @Result(column="video_url",property="video_url",jdbcType= JdbcType.VARCHAR),
            @Result(column="cover_url",property="cover_url",jdbcType= JdbcType.VARCHAR),
            @Result(column="user_id",property="user_id",jdbcType= JdbcType.BIGINT),
            @Result(column="visit_count",property="visit_count",jdbcType= JdbcType.INTEGER),
            @Result(column="like_count",property="like_count",jdbcType= JdbcType.INTEGER),
            @Result(column="comment_count",property="comment_count",jdbcType= JdbcType.INTEGER)
    })
    @Select("select * from video ;")
    List<Video> getAll();

    @ResultMap(value="VideoMapper")
    @Select("SELECT * FROM video WHERE created_at > FROM_UNIXTIME(#{timestamp} / 1000)")
    List<Video> getVideosAfterTimestamp(@Param("timestamp") String timestamp);


    @ResultMap(value="VideoMapper")
    @Select("SELECT * FROM video WHERE (user_id = #{user_id}) LIMIT #{page_num},#{page_size}")
    List<Video> getVideoByPage(long user_id, Integer page_num, Integer page_size);*/
}
