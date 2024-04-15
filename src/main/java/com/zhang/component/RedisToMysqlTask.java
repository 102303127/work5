package com.zhang.component;

import com.zhang.entity.Chat;
import com.zhang.service.ChatService;
import com.zhang.utils.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;


// 将redis数据放进mysql中
@Slf4j
@Component
public class RedisToMysqlTask extends QuartzJobBean {

    @Autowired
    private RedisUtil redisUtil;

    @Autowired
    private ChatService chatService;

    /**
     * 把redis中用户已读的消息，同步到数据库，未读的消息存储在redis中
     * redis采用主从复制，哨兵模式，防止redis宕机造成数据丢失
     *
     * @param jobExecutionContext
     */
    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) {
        // 获取存储在redis中聊天记录的条数
        long messageListSize = redisUtil.lGetListSize("chat");
        // 写入数据库的数据总条数
        long resultCount = 0;
        for (int i = 0; i < messageListSize; i++) {
            // 从头到尾取出链表中的元素
            Chat chat = (Chat) redisUtil.lGetIndex("chat", i);
            //数据库中已有的不再写入
            //或者未读消息也不写入
            if ((chatService.getById(chat.getId()) != null) || !chat.isStatus()) continue;
            // 向数据库写入数据
            boolean result = chatService.save(chat);

            if (result) {
                // 写入成功
                resultCount++;
                //写入数据库的在redis删除
                redisUtil.lRemove("chat", 1, chat);
            }
        }
        log.info(resultCount+ "条聊天记录，已写入数据库");
    }
}
