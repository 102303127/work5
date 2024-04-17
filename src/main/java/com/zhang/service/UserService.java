package com.zhang.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhang.entity.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Transactional
public interface UserService extends IService<User> {

    boolean register(String username,String password);
    Map<String, String> login(String username, String password);
    UserDetails loadUserByUsername(String username);
    void update(User user);

    User getUserById(String userId);
}
