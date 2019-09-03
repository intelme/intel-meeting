package com.intel.meeting.service.impl;

import com.intel.meeting.po.User;
import com.intel.meeting.repository.UserRepository;
import com.intel.meeting.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author ranger
 * @create 2019-09 -03-13:54
 */

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    //注入redis,虽报错，但没错；一定需要<String,String>不然就给定具体序列化
    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @Override
    public String getVCode(String username, String email) {
        User user = userRepository.findByEmail(email);
        if (user != null) {
            return "emailExist";
        }
        User user1 = userRepository.findByUsername(username);
        if (user1 != null) {
            return "userNameExist";
        }

        /**
         * 用户名和邮箱不冲突，将用户名、邮箱以及随机生成的6位验证码
         * 以key:用户名-邮箱      value:验证码  的格式存在redis数据库
         * 过期时间设置为10min
         * 注册需要根据key取value，然后校对，可参考test下com.intel.meeting.TestRedis
         */
        String vcode = (int) ((Math.random() * 9 + 1) * 100000) + "";
        String key = new String((username + "-" + email).getBytes());
        System.out.println("key = " + key);
        redisTemplate.opsForValue().set(key, vcode, 10L, TimeUnit.MINUTES);

        /**
         * 下面是发送邮件
         */
        new Thread(new Runnable() {
            @Override
            public void run() {
                //这里完成
            }
        }).start();

        return "success";
    }

    @Override
    public String saveUser(User user) {
        List<User> userList = userRepository.findDistinctByUsernameOrEmail(user.getUsername(), user.getEmail());
        if (user.getUserId() == null || "".equals(user.getUserId())) {
            for (User user1 : userList) {
                if (user1.getUsername().equals(user.getUsername()) || user1.getEmail().equals(user.getEmail()))
                    //1. 判断该用户是否存在
                    return "exist";
            }
            User save = userRepository.save(user);
            System.out.println("save = " + save);
            return "save";
        } else {
            //修改用户名
            for (User user1 : userList) {
                //如果有用户名重复情况
                if (user1.getUsername().equals(user.getUsername()) && user1.getEmail().equals(user.getEmail())) {
                    if (user1.getUserId() == user.getUserId()) {
                        userRepository.save(user);
                        return "save";
                    } else {
                        //出现重复情况
                        return "exist";
                    }
                }
            }
            userRepository.save(user);
            return "save";
        }
    }
}
