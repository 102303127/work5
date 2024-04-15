package com.zhang.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import com.zhang.entity.Chat;
import com.zhang.entity.Follow;
import com.zhang.entity.User;
import com.zhang.mapper.ChatMapper;
import com.zhang.mapper.FollowMapper;
import com.zhang.service.ChatService;
import com.zhang.service.UserService;
import com.zhang.utils.RedisUtil;
import com.zhang.utils.SnowFlakeUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

/**
 * @author zhang
 * @date 2024/3/24
 * @Description
 */

@Service
@Slf4j
public class ChatServiceImpl extends ServiceImpl<ChatMapper, Chat> implements ChatService {

    @Autowired
    private ChatMapper chatMapper;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RedisUtil redisUtil;
    @Autowired
    private SnowFlakeUtils snowFlakeUtils;
    @Autowired
    private FollowMapper followMapper;

    /**
     * 发送私聊
     * @param user
     * @param toUser
     * @param content
     * @throws IOException
     * @throws TimeoutException
     */
    @Override
    public void privateMessage(User user, User toUser, String content) throws IOException, TimeoutException {
        Chat chat = new Chat();
        chat.setId(snowFlakeUtils.getNextId());
        chat.setFromUserId(user.getId());
        chat.setToUserId(toUser.getId());
        chat.setContent(content);
        chat.setCreatedAt(new Date());
        String queueName = user.getUsername() + "->" + toUser.getUsername();
        //发送
        sendPrivateMessage(user.getUsername(),queueName, chat);
        //监听
        directListener(queueName);
    }

    /**
     * 拉取私聊历史消息
     * @param user
     * @param toUser
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public List<Chat> getPmHistory(User user, User toUser, Integer pageNum, Integer pageSize) {
        ArrayList<Chat> chats = new ArrayList<>();
        /**
         * 在数据库中
         */
        QueryWrapper<Chat> qw = new QueryWrapper<>();
        qw.eq("from_user_id",user.getId())
                .eq("to_user_id",toUser.getId())
                .eq("status",true);
        List<Chat> chats1 = chatMapper.selectList(qw);
        chats.addAll(chats1);
        /**
         * 在redis中
         */
        // 获取存储在redis中聊天记录的条数
        long messageListSize = redisUtil.lGetListSize("chat");
        for (int i = 0; i < messageListSize; i++) {
            // 从头到尾取出链表中的元素
            Chat chat = (Chat) redisUtil.lGetIndex("chat", i);
            if (chat.isStatus()
                    &&
                    (Objects.equals(user.getId(), chat.getToUserId())
                    && Objects.equals(toUser.getId(), chat.getFromUserId()))
                    ||
                    (Objects.equals(toUser.getId(), chat.getToUserId())
                    && Objects.equals(user.getId(), chat.getFromUserId()))){
                chats.add(chat);
            }
        }
        //实现对List的分页查询
        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, chats.size());
        return chats.subList(startIndex, endIndex);
    }

    /**
     * 拉取私聊未读消息
     * 未读消息只会存储在redis中，不会同步到mysql
     * (可以用redis的主从复制和哨兵模式防止redis宕机造成数据丢失)
     * @param user
     * @param toUser
     * @return
     * @throws IOException
     */
    @Override
    public List<Chat> getNotReadHistory(User user, User toUser){
        ArrayList<Chat> chats = new ArrayList<>();
        /**
         * 在redis中
         */
        // 获取存储在redis中聊天记录的条数
        long messageListSize = redisUtil.lGetListSize("chat");
        for (int i = 0; i < messageListSize; i++) {
            // 从头到尾取出链表中的元素
            Chat chat = (Chat) redisUtil.lGetIndex("chat", i);
            if (!chat.isStatus()
                    && Objects.equals(user.getId(), chat.getToUserId())
                    && Objects.equals(toUser.getId(), chat.getFromUserId())){
                //更改状态
                chat.setStatus(true);
                redisUtil.lUpdateIndex("chat", i, chat);
                chats.add(chat);
            }
        }
        return chats;
    }

    /**
     * 判断私聊是否是好友关系
     * @param fromUser
     * @param toUser
     * @return
     */
    @Override
    public boolean isFriend(User fromUser, User toUser) {
        QueryWrapper<Follow> eq1 = new QueryWrapper<Follow>().eq("user_id", fromUser.getId()).eq("follow_user_id", toUser.getId());
        QueryWrapper<Follow> eq2 = new QueryWrapper<Follow>().eq("user_id", toUser.getId()).eq("follow_user_id", fromUser.getId());
        return followMapper.selectCount(eq1) != 0 && followMapper.selectCount(eq2) != 0;
    }
    /**
     * 发送群聊消息
     * @param user
     * @param group
     * @param content
     * @throws IOException
     * @throws TimeoutException
     */
    @Override
    public void groupMessage(User user, Integer group, String content) throws TimeoutException, IOException {
        Chat chat = new Chat();
        chat.setId(snowFlakeUtils.getNextId());
        chat.setFromUserId(user.getId());
        chat.setGroupId(group);
        chat.setContent(content);
        chat.setCreatedAt(new Date());
        chat.setStatus(true);
        String queueName = "group" + group;
        //发送
        sendGroupMessage(group, queueName, chat);
        //监听消息
        directListener(queueName);
    }

    /**
     * 获得群聊聊天记录
     * @param group
     * @param pageNum
     * @param pageSize
     * @return
     */
    @Override
    public List<Chat> getGroupHistory(Integer group, Integer pageNum, Integer pageSize) {
        ArrayList<Chat> chats = new ArrayList<>();
        /**
         * 在数据库中
         */
        QueryWrapper<Chat> qw = new QueryWrapper<>();
        qw.eq("group_id", group);
        List<Chat> chats1 = chatMapper.selectList(qw);
        chats.addAll(chats1);
        /**
         * 在redis中
         */
        // 获取存储在redis中聊天记录的条数
        long messageListSize = redisUtil.lGetListSize("chat");
        for (int i = 0; i < messageListSize; i++) {
            // 从头到尾取出链表中的元素
            Chat chat = (Chat) redisUtil.lGetIndex("chat", i);
            if (chat != null && Objects.equals(chat.getGroupId(), group)) {
                chats.add(chat);
            }
        }
        //实现对List的分页查询
        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, chats.size());
        return chats.subList(startIndex, endIndex);
    }



    /**
     * 发送消息，会自动创建不存在的消息队列
     * @param toUserName
     * @param chat
     * @throws IOException
     */
    public void sendPrivateMessage(String toUserName, String queueName, Chat chat) throws IOException {
        ConnectionFactory connectionFactory = rabbitTemplate.getConnectionFactory();
        Channel channel = connectionFactory
                .createConnection()
                .createChannel(true);
        //声明队列
        String exchangeName = "private.direct";
        channel.queueDeclare(queueName, true, false, false, null);
        //绑定交换机
        //toUserName设置为key
        channel.queueBind(queueName, exchangeName, toUserName);
        //消息队列可以发送 字符串、字节数组、序列化对象
        ObjectMapper objectMapper = new ObjectMapper();
        String msg = objectMapper.writeValueAsString(chat);
        //发送消息
        rabbitTemplate.convertAndSend(exchangeName, toUserName, msg);
    }

    /**
     * 发送群聊消息，会自动创建不存在的消息队列
     * @param group
     * @param queueName
     * @param chat
     * @throws IOException
     */
    private void sendGroupMessage(Integer group, String queueName, Chat chat) throws IOException {
        ConnectionFactory connectionFactory = rabbitTemplate.getConnectionFactory();
        Channel channel = connectionFactory
                .createConnection()
                .createChannel(true);
        //声明队列
        String key = String.valueOf(group);
        String exchangeName = "group.direct";
        channel.queueDeclare(queueName, true, false, false, null);
        //绑定交换机
        //groupId设置为key
        channel.queueBind(queueName, exchangeName, key);
        //消息队列可以发送 字符串、字节数组、序列化对象
        ObjectMapper objectMapper = new ObjectMapper();
        String msg = objectMapper.writeValueAsString(chat);
        //发送消息
        rabbitTemplate.convertAndSend(exchangeName, key, msg);
    }

    /**
     * 监听指定渠道的消息
     * 可以是私聊，也可以是群聊
     * @return
     * @throws IOException
     */
    public void directListener(String queueName) throws IOException, TimeoutException {
        // 收到消息的回调接口
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            processMessage(message);
            log.info("收到消息"+message);
        };
        // 取消发送的回调接口
        CancelCallback cancelCallback = (consumerTag) -> {
            log.error("消息消费被中断");
        };
        //监听队列
        ConnectionFactory connectionFactory = rabbitTemplate.getConnectionFactory();
        Channel channel = connectionFactory
                .createConnection()
                .createChannel(true);
        channel.basicConsume(queueName, true, deliverCallback, cancelCallback);
        channel.close();
    }

    /**
     * 处理监听到的消息
     * 私聊，群聊
     * @param message
     * @throws JsonProcessingException
     */
    private void processMessage(String message) throws JsonProcessingException {
        //把消息转换成对象形式
        ObjectMapper objectMapper = new ObjectMapper();
        Chat chat = objectMapper.readValue(message, Chat.class);
        //把消息存储到redis中
        redisUtil.lSet("chat",chat);
    }

}
