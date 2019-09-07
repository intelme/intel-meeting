package com.intel.meeting.controller;

import com.intel.meeting.po.MeetingRoom;
import com.intel.meeting.po.Record;
import com.intel.meeting.po.ReserveMeeting;
import com.intel.meeting.po.User;
import com.intel.meeting.service.MeetingRoomService;
import com.intel.meeting.service.RecordService;
import com.intel.meeting.service.ReserveMeetingService;
import com.intel.meeting.service.UserService;
import com.intel.meeting.utils.DateUtils;
import com.intel.meeting.utils.ReserveMeetingUtiles;
import com.intel.meeting.utils.SessionUtils;
import com.intel.meeting.utils.UserUtils;
import com.intel.meeting.vo.MainMr;
import com.intel.meeting.vo.ReserveMeetingInfo;
import com.intel.meeting.vo.RtnIdInfo;
import com.intel.meeting.vo.SessionUser;
import com.intel.meeting.vo.UserIndex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 预定会议室
 *
 * @author Ranger
 * @create 2019-09-03 20:53
 */
@Controller
public class ReserveMeetingController {

    @Autowired
    private MeetingRoomService mrService;
    @Autowired
    private UserService userService;
    @Autowired
    private ReserveMeetingService rmService;
    @Autowired
    private RecordService recordService;

    @Value("${INIT_SIGN_TIME}")
    private String INIT_SIGN_TIME;

    /**
     * 前往用户预定中心
     *
     * @return
     */
    @GetMapping("/to/reserve/center")
    public String toReserveCenter(Model model,
                                  HttpServletRequest request) {
        //从session中获取用户
        SessionUser userIndex = (SessionUser) SessionUtils.getObjectFromSession(request, "sessionUser");

        User user = userService.findUserById(userIndex.getUserId());
        Set<ReserveMeeting> reserveSet = user.getReserveSet();
        List<ReserveMeetingInfo> rmiList = ReserveMeetingUtiles.reserveSetToRMList(reserveSet);
        //根据预定顺序排序
        Collections.sort(rmiList);
        model.addAttribute("rmiList", rmiList);
        UserUtils.setUserIndex(model, request);

        return "user/reserve-center";
    }

    /**
     * 预定会议室
     *
     * @param meetingId
     * @param reserveDay
     * @param startTime
     * @param endTime
     * @param request
     * @return
     */
    @PostMapping("/meeting/reserve")
    @ResponseBody
    public MainMr reserveMeeting(Integer meetingId,
                                 String reserveDay,
                                 String startTime,
                                 String endTime,
                                 HttpServletRequest request) {
        SessionUser sessionUser = (SessionUser) request.getSession().getAttribute("sessionUser");
        User user = userService.findUserById(sessionUser.getUserId());
        MeetingRoom meetingRoom = mrService.findMrById(meetingId);
        ReserveMeeting reserveMeeting = new ReserveMeeting(user,
                meetingRoom,
                reserveDay + " " + startTime,
                reserveDay + " " + endTime,
                1,
                INIT_SIGN_TIME);

        return rmService.save(reserveMeeting);
    }

    /**
     * 取消预定
     *
     * @param reserveId
     * @return
     */
    @PostMapping("/meeting/reserve/cancel")
    @ResponseBody
    public String cancelReserveMeeting(Integer reserveId) {
        String result = rmService.cancelReserveMeeting(reserveId);
        //可以取消
        if ("success".equals(result)) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    rmMoveToRecord(reserveId,
                            rmService,
                            INIT_SIGN_TIME,
                            userService,
                            recordService);
                }
            }).start();
        }

        return result;
    }

    /**
     * 签到
     *
     * @param reserveId
     * @return
     */
    @PostMapping("/meeting/reserve/sign")
    @ResponseBody
    public String signReserveMeeting(Integer reserveId) {

        return rmService.signReserveMeeting(reserveId);
    }

    /**
     * reserveUser.getUserAuth().getRealName()
     * nodone:尚未完成认证，存储记录没法实名，先使用用户名
     *
     * @param reserveId
     * @return
     */
    @PostMapping("/meeting/reserve/over")
    @ResponseBody
    public String overReserveMeeting(Integer reserveId) {

        String result = rmService.overReserveMeeting(reserveId);
        //创建新线程，以免影响响应
        new Thread(new Runnable() {
            @Override
            public void run() {
                //当成功结束会议时，将reserve_meeting表中信息迁移至t_record;
                if ("success".equals(result)) {
                    rmMoveToRecord(reserveId,
                            rmService,
                            INIT_SIGN_TIME,
                            userService,
                            recordService);
                }
            }
        }).start();

        return result;
    }

    /**
     * 抽象出来的公用方法
     * 用在提前结束会议和取消会议中
     * 将reserve_meeting数据转移至t_record
     *
     * @param reserveId
     * @param rmService
     * @param INIT_SIGN_TIME
     * @param userService
     * @param recordService
     */
    private static void rmMoveToRecord(Integer reserveId,
                                       ReserveMeetingService rmService,
                                       String INIT_SIGN_TIME,
                                       UserService userService,
                                       RecordService recordService) {
        //转换对象
        ReserveMeeting reserveMeeting = rmService.findOneById(reserveId);
        //获取预约会议室的用户
        User reserveUser = reserveMeeting.getUser();
        Record record = new Record(reserveUser.getUsername(),
                reserveMeeting.getMeetingRoom().getMeetingName(),
                reserveMeeting.getStartTime(),
                reserveMeeting.getEndTime(),
                reserveMeeting.getSignTime(),
                reserveMeeting.getUsageStatus());
        if (INIT_SIGN_TIME.equals(reserveMeeting.getSignTime())) {
            //没有签到
            userService.doNoSign(reserveUser);
        }
        //保存记录，删除预定
        recordService.saveRecord(record);
        rmService.delReserveMeetingById(reserveId);
    }
}
