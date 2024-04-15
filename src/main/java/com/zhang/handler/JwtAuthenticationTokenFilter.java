package com.zhang.handler;

import com.zhang.exception.UserException;
import com.zhang.entity.User;
import com.zhang.utils.JwtUtils;
import com.zhang.utils.RedisUtil;
import io.jsonwebtoken.Claims;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;


import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws javax.servlet.ServletException, IOException {

        //获取accessToken
        String accessToken = request.getHeader("accessToken");

        //排除登录(login)+注册请求(register)请求，排除accessToken没有内容的
        if (!StringUtils.hasText(accessToken)
                ||Objects.equals(request.getServletPath(), "/user/login")
                ||Objects.equals(request.getServletPath(), "/user/register")) {
            //放行
            filterChain.doFilter(request, response);
            return;
        }
        //未登录异常
        if (Objects.equals(accessToken,"undefined")){
            throw new UserException("请先登录");
        }

        //检查token时效性
        if (JwtUtils.isTokenExpired(accessToken)){
            String refreshToken = request.getHeader("refreshToken");
            if (JwtUtils.isTokenExpired(refreshToken)){
                request.setAttribute("errorMessage","过久未登录，请重新登录");
                throw new UserException("过久未登录，请重新登录");
            }else {
                //利用refreshToken生成accessToken和refreshToken
                //将新Token存到Api fox的Header中
                try {
                    accessToken = JwtUtils.createAccessTokenByRefresh(refreshToken);
                    response.setHeader("accessToken",accessToken);
                    String newRefreshToken=JwtUtils.createRefreshTokenByRefresh(refreshToken);
                    response.setHeader("refreshToken",newRefreshToken);
                } catch (Exception e) {
                    logger.error(e);
                    throw new UserException("token非法");
                }
            }
        }

        //解析token
        String userid;
        try {
            Claims claims = JwtUtils.parseJWT(accessToken);
            userid = claims.getSubject();
        } catch (Exception e) {
            logger.error(e);
            throw new UserException("token非法");
        }

        //从redis中获取用户信息
        String redisKey = "login:" + userid;
        User user = (User) RedisUtil.get(redisKey);
        if(Objects.isNull(user)){
            throw new UserException("用户未登录");
        }

        //把用户相关信息存入SecurityContextHolder
        //TODO 获取权限信息封装到Authentication中
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(user,null,null);
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);

        //放行
        filterChain.doFilter(request, response);
    }



}
