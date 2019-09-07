package com.intel.meeting.controller;

import com.intel.meeting.utils.FastDFSUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author Ranger
 * @create 2019-09-07 16:44
 */
@Controller
public class UserAuthController {

    @Value("${FDFSDFS_CLIENT_PAHT}")
    private String FDFSDFS_CLIENT_PAHT;
    @Value("${FDFSDFS_ADDRESS}")
    private String FDFSDFS_ADDRESS;

    @PostMapping("/user/auth")
    @ResponseBody
    public String userAuth(String realname,
                           String jobNum,
                           MultipartFile authImg) {
        System.out.println("realname = " + realname);
        System.out.println("jobNum = " + jobNum);
        System.out.println("authImg = " + authImg);
        try {
            String s = FastDFSUtils.uploadFile(FDFSDFS_CLIENT_PAHT,
                    FDFSDFS_ADDRESS,
                    authImg);
            System.out.println("s = " + s);
        }catch (Exception e){
            e.printStackTrace();
            return "fail";
        }

        return "success";
    }

}
