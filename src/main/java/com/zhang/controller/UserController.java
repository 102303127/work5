package com.zhang.controller;


import com.zhang.exception.UserException;
import com.zhang.entity.User;
import com.zhang.service.UserService;
import com.zhang.vo.base;
import com.zhang.vo.result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * @author zhang
 * &#064;date  2024/2/10
 * &#064;Description  用户Controller层
 */
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

/*    @GetMapping("/user")
    public result getCurrentUser() {
        Authentication authentication= SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();
        return result.OK(userDetails);
    }*/

    /**
     * 注册方法
     *
     * @param username
     * @param password
     * @return
     */
    @PostMapping("/register")
    public result register(@RequestParam("username") String username,
                           @RequestParam("password") String password){
        if (username.isEmpty()||password.isEmpty()) throw new UserException("请补全注册信息");
        boolean flag=userService.register(username,password);
        if(flag) {
            log.info("用户注册成功");
            return result.OK();
        }
        log.error("注册失败");
        return result.Fail();
    }

    /**
     * 登录方法
     *
     * @param username
     * @param password
     * @return
     */
    @PostMapping("/login")
    public result login(@RequestParam("username") String username,
                        @RequestParam("password") String password){
        if (username.isEmpty()||password.isEmpty()) throw new UserException("请补全登录信息");
        //利用Map返回accessToken和refreshToken
        Map<String, String> map = userService.login(username, password);
        log.info("用户登录成功");
        return result.OK(map);
    }

    /**
     * 查询用户操作
     *
     * @param userId
     * @return
     */
    @GetMapping("/info")
    public result getUser(@RequestParam("userId") String userId){
        if (userId.isEmpty())throw new UserException("用户ID信息不可为空");
        User user = userService.getById(userId);
        boolean flag= user != null;
        return new result(new base(flag?10000:-1,flag ? "success":"用户查询失败，请重试"),user);
    }

}
