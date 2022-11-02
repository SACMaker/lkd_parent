package com.lkd.job;

import com.lkd.common.VMSystem;
import com.lkd.entity.UserEntity;
import com.lkd.service.UserService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import com.xxl.job.core.log.XxlJobLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@Slf4j
public class UserJob {
    //测试xxx-job
    /*@XxlJob("testHandler")
    public ReturnT<String> demoJobHandler(String param) throws Exception {
        log.info("立可得集成xxl-job");
        return ReturnT.SUCCESS;
    }*/

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 每日工单量列表初始化
     *
     * @param param
     * @return
     * @throws Exception
     */
    @XxlJob("workCountInitJobHandler")
    public ReturnT<String> workCountInitJobHandler(String param) throws Exception {
        //查询用户列表
        List<UserEntity> userList = userService.list();

        //构建数据（zset),zset可自动排序
        //zset数据结构
        //                      key                                value
        //                      key                           value      score
        //region.task.20210105.1339752425761804289.1003        6           1
        userList.forEach(user -> {
            if (user.getRoleId().intValue() != 0) { //只考虑非管理员
                //固定字符串（前缀）+时间+区域+工单类别（运营/运维）为大key
                //eg:region.task.20210105.1339752425761804289.1003
                String key = VMSystem.REGION_TASK_KEY_PREF
                        //天数加一天,在admin页面配置每天下午2点生成次日的工单量初始化,for test use plusDays(0)直接生成单天的初始化数据
                        + LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                        + "." + user.getRegionId() + "." + user.getRoleCode();
                redisTemplate.opsForZSet().add(key, user.getId(), 0);//人员id做小key,score为工单量计数量,初始化数据都为0
                XxlJobLogger.log("初始化" + key + ":" + user.getId());
                redisTemplate.expire(key, Duration.ofDays(2));//2天后过期
            }
        });
        return ReturnT.SUCCESS;
    }

}
