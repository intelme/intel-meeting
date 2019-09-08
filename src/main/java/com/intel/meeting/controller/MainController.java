package com.intel.meeting.controller;

import com.intel.meeting.po.MeetingRoom;
import com.intel.meeting.service.MeetingRoomService;
import com.intel.meeting.service.UserAuthService;
import com.intel.meeting.service.UserService;
import com.intel.meeting.utils.MainMrUtils;
import com.intel.meeting.utils.UserUtils;
import com.intel.meeting.vo.MRPage;
import com.intel.meeting.vo.MainMr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author Ranger
 * @create 2019-08-30 21:57
 */
@Controller
public class MainController {

    @Autowired
    private MeetingRoomService mrService;
    @Autowired
    private UserAuthService userAuthService;
    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String toI() {
        return "redirect:/index";
    }

    /**
     * 接收Get请求，URL：http://www.intel.com
     * 跳转至页面：index/index.html
     *
     * @return
     */
    @GetMapping("/index")
    public String toIndex(Model model,
                          @RequestParam(required = false) Integer page,
                          HttpServletRequest request) {
        if (page == null) {
            page = 0;
        }
        Page<MeetingRoom> mrPage = mrService.findMeetingRoomByPage(page, 6);
        List<MainMr> mainMrList = MainMrUtils.mrListToMainMrList(mrPage.getContent());
        MRPage mrPageInfo = new MRPage(mainMrList,
                page + 1,
                mrPage.getTotalPages(),
                (int) mrPage.getTotalElements(),
                2);
        model.addAttribute("mainMrPage", mrPageInfo);
        UserUtils.setUserIndex(model, request);
        return "index/index";
    }

    /**
     * 接收URL:http://www.intel.com/to/control
     * 跳转至页面control/index.html
     *
     * @return
     */
    @GetMapping("/to/control")
    public String toControl(Model model,
                            HttpServletRequest request) {
        //查询各状态会议室的数量
        int freeNum = mrService.countByEnableStatus("空闲");
        int downNum = mrService.countByEnableStatus("故障");
        model.addAttribute("freeNum", freeNum);
        model.addAttribute("downNum", downNum);

        UserUtils.setUserIndex(model, request);
        return "control/index";
    }

    /**
     * 接收URL:http://www.intel.com/to/usermgn
     * 跳转至页面usermgn/index.html
     *
     * @return
     */
    @GetMapping("/to/usermgn")
    public String toUsermgn(Model model,
                            HttpServletRequest request) {

        //统计注册人数
        int rn = userService.countRegistNum();
        model.addAttribute("registNum", rn);
        
        // 统计认证各状态的人数
        Integer n1 = userAuthService.countUserAuthStatus(1);
        Integer n2 = userAuthService.countUserAuthStatus(2);
        Integer n3 = userAuthService.countUserAuthStatus(3);
        model.addAttribute("authStatus1", n1);
        model.addAttribute("authStatus2", n2);
        model.addAttribute("authStatus3", n3);
        UserUtils.setUserIndex(model, request);
        return "usermgn/index";
    }

}
