package com.zhang.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhang.entity.Chat;
import com.zhang.entity.User;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * @author zhang
 * @date 2024/3/24
 * @Description
 */

@Transactional
public interface ChatService extends IService<Chat> {

    void privateMessage(User user, User toUser, String content) throws IOException, TimeoutException;

    void groupMessage(User fromUser, Integer group, String content) throws IOException, TimeoutException;

    List<Chat> getPmHistory(User fromUser, User toUser, Integer pageNum, Integer pageSize);

    List<Chat> getNotReadHistory(User fromUser, User toUser) throws IOException;

    List<Chat> getGroupHistory(Integer group, Integer pageNum, Integer pageSize);

    boolean isFriend(User fromUser, User toUser);
}
