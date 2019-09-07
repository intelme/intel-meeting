package com.intel.meeting.service;

import com.intel.meeting.po.User;
import org.springframework.data.domain.Page;

/**
 * @author ranger
 * @create 2019-09 -03-13:53
 */


public interface UserService {

    String saveUser(User user);
    String getVCode(String username,String email);
    String register(User user,String vcode);

    //发送邮件
    String sendMail(String email);

    String login(String usernameoremail, String password);
    Page<User> findUserByPage(Integer page, Integer size);

    String delUser(Integer userId);

    User findUserByUsernameOrEmailAndPassword(String usernameoremail, String password);

    User findUserById(Integer userId);

    void doNoSign(User reserveUser);

    void saveUserAuth(User user);
}
