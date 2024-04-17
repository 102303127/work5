package com.zhang.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhang.entity.Chat;
import com.zhang.entity.User;
import com.zhang.exception.UserException;
import com.zhang.service.ChatService;
import com.zhang.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeoutException;

/**
 *  websocket私聊服务
 */
@ServerEndpoint("/chat")
@Component
@Slf4j
public class WebSocketServer {
    /**
     * 直接注入得到为空，已经遇到很多次这样的情况：有时候可以注入成功，有时候失败
     * 解决办法也有很多：1.比如下面这种，用set注入就可以成功
     * 2.可以用BeanUtils类获得诸如对象的Bean也可以
     */
    private static UserService userService;
    @Autowired
    public void setUserService(UserService userService){
        WebSocketServer.userService=userService;
    }
    private static ChatService chatService;
    @Autowired
    public void setChatService(ChatService chatService) {
        WebSocketServer.chatService = chatService;
    }

    /**
     * 记录当前在线连接数
     */
    public static final Map<String, Session> sessionMap = new ConcurrentHashMap<>();
    /**
     * 记录群组和成员
     */
    private static final Map<Integer, CopyOnWriteArraySet<WebSocketServer>> groups = new HashMap<>();
    User fromUser = null;
    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session) {
        String name = session.getUserPrincipal().getName();
        UserDetails userDetails = userService.loadUserByUsername(session.getUserPrincipal().getName());
        this.fromUser= (User) userService.loadUserByUsername(session.getUserPrincipal().getName());
        sessionMap.put(this.fromUser.getUsername(), session);
        // 后台发送消息给所有的客户端
        log.info("有新用户加入，username={}, 当前在线人数为：{}", this.fromUser.getUsername(), sessionMap.size());
        sendAllMessage(session,"用户{"+ this.fromUser.getUsername()+"}加入连接");
    }


    /**
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(Session session, String message) throws IOException, TimeoutException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> jsonMessage = objectMapper.readValue(message, Map.class);
        String messageType = (String) jsonMessage.get("type");
        Map<String, Object> data = (Map<String, Object>) jsonMessage.get("data");
        String content = (String) data.get("content");
        User toUser;
        Integer group;
        Integer pageNum;
        Integer pageSize;
        try {
            switch (messageType){
                case "private message":

                    toUser = (User) userService.loadUserByUsername((String) data.get("toUser"));
                    //判断两人好友关系
                    if (!chatService.isFriend(fromUser, toUser)){
                        sendMessage("发送私信失败,没有好友关系", session);
                        break;
                    }

                    log.info("用户{}向用户{}私发消息:{}", fromUser.getUsername(), toUser.getUsername(), content);
                    chatService.privateMessage(fromUser, toUser, content);
                    break;

                case "pm history":
                    toUser = (User) userService.loadUserByUsername((String) data.get("toUser"));
                    //判断两人好友关系


                    pageNum = (Integer) data.get("page_num");
                    pageSize = (Integer) data.get("page_size");
                    List<Chat> pmHistory = chatService.getPmHistory(fromUser, toUser, pageNum, pageSize);
                    for (Chat chat : pmHistory) {
                        sendMessage(String.valueOf(chat), session);
                    }
                    break;

                case "pm notRead history":
                    toUser = (User) userService.loadUserByUsername((String) data.get("toUser"));
                    //判断两人好友关系


                    List<Chat> notReadHistory = chatService.getNotReadHistory(fromUser, toUser);
                    for (Chat chat : notReadHistory) {
                        sendMessage(String.valueOf(chat), session);
                    }
                    break;

                case "group message" :
                    group = (Integer) data.get("group");
                    /**
                     * 创建容器存储群聊用户
                     * 可以实现即时传递消息
                     */
/*                    CopyOnWriteArraySet<WebSocketServer> friends = groups.get(group);
                    if (friends == null) {
                        synchronized (groups) {
                            if (!groups.containsKey(group)) {
                                friends = new CopyOnWriteArraySet<>();
                                groups.put(group, friends);
                            }
                        }
                    }
                    friends.add(this);*/
                    log.info("用户{}向群组{}发消息:{}", fromUser.getUsername(), group, content);
                    chatService.groupMessage(fromUser, group, content);
/*                    for (WebSocketServer item : friends) {
                        if (Objects.equals(item.session, session)) continue;
                        sendMessage(content, item.session);
                    }*/
                    break;

                case "group history":
                    group = (Integer) data.get("group");
                    pageNum = (Integer) data.get("page_num");
                    pageSize = (Integer) data.get("page_size");
                    List<Chat> groupHistory = chatService.getGroupHistory(group, pageNum, pageSize);
                    for (Chat chat : groupHistory) {
                        sendMessage(String.valueOf(chat), session);
                    }
                    break;
                default:
                    throw new UserException("输入type异常");
            }
        }catch (Exception ex){
            log.error(ex.getMessage());
            throw new UserException("输入异常请检查参数");
        }

    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose(Session session) {
        sessionMap.remove(fromUser.getUsername());
        log.info("有连接关闭，移除username={}的用户session, 当前在线人数为：{}", fromUser.getUsername(), sessionMap.size());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("发生错误");
        log.error(String.valueOf(error));
    }


//=================================== send ===========================================
    /**
     * 服务端发送消息给客户端
     */
    private void sendMessage(String message, Session toSession) {
        try {
            log.info("服务端给客户端[{}]发送消息:{}", toSession.getId(), message);
            toSession.getBasicRemote().sendText(message);
        } catch (Exception e) {
            log.error("服务端发送消息给客户端失败", e);
        }
    }
    /**
     * 服务端发送消息给所有客户端
     */
    private void sendAllMessage(Session fromSession, String message) {
        try {
            for (Session session : sessionMap.values()) {
                //发送除了自己所有人
                if (Objects.equals(session, fromSession)) continue;
                log.info("服务端给客户端[{}]发送消息:{}", session.getId() , message);
                session.getBasicRemote().sendText(message);
            }
        } catch (Exception e) {
            log.error("服务端发送消息给客户端失败", e);
        }
    }
}