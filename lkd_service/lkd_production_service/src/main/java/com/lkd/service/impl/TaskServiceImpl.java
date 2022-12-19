package com.lkd.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.lkd.common.VMSystem;
import com.lkd.config.TopicConfig;
import com.lkd.contract.SupplyCfg;
import com.lkd.contract.SupplyChannel;
import com.lkd.contract.TaskCompleteContract;
import com.lkd.dao.TaskDao;
import com.lkd.emq.MqttProducer;
import com.lkd.entity.TaskDetailsEntity;
import com.lkd.entity.TaskEntity;
import com.lkd.entity.TaskStatusTypeEntity;
import com.lkd.exception.LogicException;
import com.lkd.feignService.UserService;
import com.lkd.feignService.VMService;
import com.lkd.http.viewModel.CancelTaskViewModel;
import com.lkd.http.viewModel.TaskReportInfo;
import com.lkd.http.viewModel.TaskViewModel;
import com.lkd.service.TaskDetailsService;
import com.lkd.service.TaskService;
import com.lkd.service.TaskStatusTypeService;
import com.lkd.viewmodel.Pager;
import com.lkd.viewmodel.UserViewModel;
import com.lkd.viewmodel.UserWork;
import com.lkd.viewmodel.VendingMachineViewModel;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TaskServiceImpl extends ServiceImpl<TaskDao, TaskEntity> implements TaskService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private TaskDetailsService taskDetailsService;

    @Autowired
    private VMService vmService;

    @Autowired
    private TaskStatusTypeService statusTypeService;

    @Autowired
    private UserService userService;

    @Autowired
    private MqttProducer mqttProducer;

    /**
     * 服务层实现类:创建工单
     *
     * @param taskViewModel
     * @return
     * @throws LogicException
     */
    @Override
    //事务:发生Exception.class类型的异常回滚,发生LogicException.class类型的异常不回滚
    @Transactional(rollbackFor = {Exception.class}, noRollbackFor = {LogicException.class})
    public boolean createTask(TaskViewModel taskViewModel) throws LogicException {
        //RPC获得机器状态进行工单状态校验
        this.checkCreateTask(taskViewModel.getInnerCode(), taskViewModel.getProductType());
        //sql查询-是否有相同工单
        if (this.hasTask(taskViewModel.getInnerCode(), taskViewModel.getProductType())) {
            throw new LogicException("该机器有未完成的同类型工单");
        }
        TaskEntity taskEntity = new TaskEntity();
        //BeanUtil转化VO到DTO
        BeanUtils.copyProperties(taskViewModel, taskEntity);
        //生成工单编号-工单编号是自动生成(利用redis的自增属性生成)
        taskEntity.setTaskCode(this.generateTaskCode());
        taskEntity.setTaskStatus(VMSystem.TASK_STATUS_CREATE);
        //taskEntity.setCreateType(taskViewModel.getCreateType());
        //taskEntity.setDesc(taskViewModel.getDesc());
        taskEntity.setProductTypeId(taskViewModel.getProductType());
        /*String userName = userService.getUser(taskViewModel.getUserId()).getUserName();
        taskEntity.setUserName(userName)*/
        //taskEntity.setInnerCode(taskViewModel.getInnerCode());
        //taskEntity.setAssignorId(taskViewModel.getAssignorId());
        //taskEntity.setUserId(taskViewModel.getUserId());
        taskEntity.setAddr(vmService.getVMInfo(taskViewModel.getInnerCode()).getNodeAddr());
        //sql插入-把创建的工单数据插入到DB
        this.save(taskEntity);
        //补货工单的补货详情-维修/投放/撤机工单是没有补货详情选项的
        if (taskEntity.getProductTypeId() == VMSystem.TASK_TYPE_SUPPLY) {
            taskViewModel.getDetails().forEach(d -> {
                TaskDetailsEntity detailsEntity = new TaskDetailsEntity();
                BeanUtils.copyProperties(taskViewModel, taskEntity);
                //detailsEntity.setChannelCode(d.getChannelCode());
                //detailsEntity.setExpectCapacity(d.getExpectCapacity());
                detailsEntity.setTaskId(taskEntity.getTaskId());
                //detailsEntity.setSkuId(d.getSkuId());
                //detailsEntity.setSkuName(d.getSkuName());
                //detailsEntity.setSkuImage(d.getSkuImage());
                //sql插入-保存补货详情
                taskDetailsService.save(detailsEntity);
            });
        }
        //工单量分值+1
        updateTaskZSet(taskEntity, 1);
        return true;
    }

    /**
     * 服务层实现类:接受工单
     *
     * @param id
     * @return
     */
    @Override
    public boolean accept(Long id) {
        TaskEntity task = this.getById(id);  //查询工单
        if (task.getTaskStatus() != VMSystem.TASK_STATUS_CREATE) {
            throw new LogicException("工单状态不是待处理");
        }
        task.setTaskStatus(VMSystem.TASK_STATUS_PROGRESS);//修改工单状态为进行
        return this.updateById(task);
    }

    /**
     * 取消工单
     * @param id
     * @param cancelVM
     * @return
     */
    @Override
    public boolean cancelTask(long id, CancelTaskViewModel cancelVM) {
        TaskEntity task = this.getById(id);
        if (task.getTaskStatus() == VMSystem.TASK_STATUS_FINISH || task.getTaskStatus() == VMSystem.TASK_STATUS_CANCEL) {
            throw new LogicException("该工单已经结束");
        }
        task.setTaskStatus(VMSystem.TASK_STATUS_CANCEL);
        task.setDesc(cancelVM.getDesc());
        //工单量分值-1
        updateTaskZSet(task, -1);
        return this.updateById(task);
    }


    @Override
    @Transactional
    public boolean completeTask(long id) {
        return completeTask(id, 0d, 0d, "");
    }

    /**
     * 完成工单
     * @param id
     * @param lat
     * @param lon
     * @param addr
     * @return
     */
    @Override
    public boolean completeTask(long id, Double lat, Double lon, String addr) {
        TaskEntity taskEntity = this.getById(id);
        if(taskEntity.getTaskStatus()== VMSystem.TASK_STATUS_FINISH  || taskEntity.getTaskStatus()== VMSystem.TASK_STATUS_CANCEL ){
            throw new LogicException("工单已经结束");
        }
        taskEntity.setTaskStatus(VMSystem.TASK_STATUS_FINISH);
        taskEntity.setAddr(addr);
        this.updateById(taskEntity);

        //如果是补货工单
        if (taskEntity.getProductTypeId() == VMSystem.TASK_TYPE_SUPPLY) {
            //补货协议封装与下发
            noticeVMServiceSupply(taskEntity);
        }

        //如果是投放工单或撤机工单
        if (taskEntity.getProductTypeId() == VMSystem.TASK_TYPE_DEPLOY
                || taskEntity.getProductTypeId() == VMSystem.TASK_TYPE_REVOKE) {
            //运维工单封装与下发
            noticeVMServiceStatus(taskEntity, lat, lon);
        }

        return true;
    }

    /**
     * 补货协议封装与下发
     *
     * @param taskEntity
     */
    private void noticeVMServiceSupply(TaskEntity taskEntity) {

        //协议内容封装
        //1.根据工单id查询工单明细表
        QueryWrapper<TaskDetailsEntity> qw = new QueryWrapper<>();
        qw.lambda().eq(TaskDetailsEntity::getTaskId, taskEntity.getTaskId());
        List<TaskDetailsEntity> details = taskDetailsService.list(qw);
        //2.构建协议内容
        SupplyCfg supplyCfg = new SupplyCfg();
        supplyCfg.setInnerCode(taskEntity.getInnerCode());//售货机编号
        List<SupplyChannel> supplyChannels = Lists.newArrayList();//补货数据
        //从工单明细表提取数据加到补货数据中
        details.forEach(d -> {
            SupplyChannel channel = new SupplyChannel();
            channel.setChannelId(d.getChannelCode());
            channel.setCapacity(d.getExpectCapacity());
            supplyChannels.add(channel);
        });
        supplyCfg.setSupplyData(supplyChannels);

        //2.下发补货协议
        //发送到emq
        try {
            mqttProducer.send(TopicConfig.COMPLETED_TASK_TOPIC, 2, supplyCfg);
        } catch (Exception e) {
            log.error("发送工单完成协议出错");
            throw new LogicException("发送工单完成协议出错");
        }

    }

    /**
     * 运维工单封装与下发
     *
     * @param taskEntity
     */
    private void noticeVMServiceStatus(TaskEntity taskEntity, Double lat, Double lon) {
        //向消息队列发送消息，通知售货机更改状态
        //封装协议
        TaskCompleteContract taskCompleteContract = new TaskCompleteContract();
        taskCompleteContract.setInnerCode(taskEntity.getInnerCode());//售货机编号
        taskCompleteContract.setTaskType(taskEntity.getProductTypeId());//工单类型
        taskCompleteContract.setLat(lat);//纬度
        taskCompleteContract.setLon(lon);//经度
        //发送到emq
        try {
            mqttProducer.send(TopicConfig.COMPLETED_TASK_TOPIC, 2, taskCompleteContract);
        } catch (Exception e) {
            log.error("发送工单完成协议出错");
            throw new LogicException("发送工单完成协议出错");
        }
    }


    @Override
    public List<TaskStatusTypeEntity> getAllStatus() {
        QueryWrapper<TaskStatusTypeEntity> qw = new QueryWrapper<>();
        qw.lambda().ge(TaskStatusTypeEntity::getStatusId, VMSystem.TASK_STATUS_CREATE);
        return statusTypeService.list(qw);
    }

    @Override
    public Pager<TaskEntity> search(Long pageIndex,
                                    Long pageSize,
                                    String innerCode,
                                    Integer userId,
                                    String taskCode,
                                    Integer status,
                                    Boolean isRepair,
                                    String start,
                                    String end) {
        Page<TaskEntity> page = new Page<>(pageIndex, pageSize);
        LambdaQueryWrapper<TaskEntity> qw = new LambdaQueryWrapper<>();
        if (!Strings.isNullOrEmpty(innerCode)) {
            qw.eq(TaskEntity::getInnerCode, innerCode);
        }
        if (userId != null && userId > 0) {
            qw.eq(TaskEntity::getAssignorId, userId);
        }
        if (!Strings.isNullOrEmpty(taskCode)) {
            qw.like(TaskEntity::getTaskCode, taskCode);
        }
        if (status != null && status > 0) {
            qw.eq(TaskEntity::getTaskStatus, status);
        }
        if (isRepair != null) {
            if (isRepair) {
                qw.ne(TaskEntity::getProductTypeId, VMSystem.TASK_TYPE_SUPPLY);
            } else {
                qw.eq(TaskEntity::getProductTypeId, VMSystem.TASK_TYPE_SUPPLY);
            }
        }
        if (!Strings.isNullOrEmpty(start) && !Strings.isNullOrEmpty(end)) {
            qw.ge(TaskEntity::getCreateTime, LocalDate.parse(start, DateTimeFormatter.ISO_LOCAL_DATE))
                    .le(TaskEntity::getCreateTime, LocalDate.parse(end, DateTimeFormatter.ISO_LOCAL_DATE));
        }
        //根据最后更新时间倒序排序
        qw.orderByDesc(TaskEntity::getUpdateTime);

        return Pager.build(this.page(page, qw));
    }


    /**
     * 获取同一天内分配的工单最少的人
     * 取消数据库查询的方案,采取从redis查的方案
     *
     * @param innerCode
     * @param isRepair  是否是维修工单
     * @return
     */
    @Override
    @Deprecated
    public Integer getLeastUser(String innerCode, Boolean isRepair) {
        List<UserViewModel> userList = null;
        if (true) {
            userList = userService.getRepairerListByInnerCode(innerCode);
        } else {
            userList = userService.getOperatorListByInnerCode(innerCode);
        }
        if (userList == null) return null;
        QueryWrapper<TaskEntity> qw = new QueryWrapper<>();
        //按人分组，取工作量。将工单数暂存到了user_id列里
        qw.select("assignor_id,count(1) as user_id");
        if (isRepair) {
            qw.lambda().ne(TaskEntity::getProductTypeId, VMSystem.TASK_TYPE_SUPPLY);
        } else {
            qw.lambda().eq(TaskEntity::getProductTypeId, VMSystem.TASK_TYPE_SUPPLY);
        }
        qw
                .lambda()
                //.le(TaskEntity::getTaskStatus,VMSystem.TASK_STATUS_PROGRESS) //根据未完成的工单
                .ne(TaskEntity::getTaskStatus, VMSystem.TASK_STATUS_CANCEL) //根据所有未被取消的工单做统计
                .ge(TaskEntity::getCreateTime, LocalDate.now())
                .in(TaskEntity::getAssignorId, userList.stream().map(UserViewModel::getUserId).collect(Collectors.toList()))
                .groupBy(TaskEntity::getAssignorId)
                .orderByAsc(TaskEntity::getUserId);
        List<TaskEntity> result = this.list(qw);

        List<TaskEntity> taskList = Lists.newArrayList();
        Integer userId = 0;
        for (UserViewModel user : userList) {
            Optional<TaskEntity> taskEntity = result.stream().filter(r -> r.getAssignorId() == user.getUserId()).findFirst();

            //当前人员今日没有分配工单
            //if(taskEntity.isEmpty()){
            //    return user.getUserId();
            //}
            TaskEntity item = new TaskEntity();
            item.setAssignorId(user.getUserId());
            item.setUserId(taskEntity.get().getUserId());
            taskList.add(item);
        }
        //取最少工单的人
        taskList.stream().sorted(Comparator.comparing(TaskEntity::getUserId));

        return taskList.get(0).getAssignorId();
    }

    /**
     * 获取同一天内分配的工单最少的人
     * @param regionId 区域id
     * @param isRepair 是否是维修工单
     * @return
     */
    @Override
    public Integer getLeastUser(Integer regionId, Boolean isRepair) {
        String roleCode = "1002";
        if (isRepair) { //如果是维修工单
            roleCode = "1003";
        }
        String key = VMSystem.REGION_TASK_KEY_PREF + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "." + regionId + "." + roleCode;
        //从小到大取0和1的范围的元素
        Set<Object> set = redisTemplate.opsForZSet().range(key, 0, 1);
        if (set == null || set.isEmpty()) {
            throw new LogicException("该区域暂时没有相关人员");
        }
        //返回人员id
        return (Integer) set.stream().collect(Collectors.toList()).get(0);
    }

    /**
     * 使用CompletableFuture进行并发异步编程获取汇总数据
     *
     * @param start
     * @param end
     * @return
     */
    @Override
    public List<TaskReportInfo> getTaskReportInfo(LocalDateTime start, LocalDateTime end) {
        //运营工单总数
        var supplyTotalFuture = CompletableFuture.supplyAsync(() -> this.taskCount(start, end, false, null));
        //运维工单总数
        var repairTotalFuture = CompletableFuture.supplyAsync(() -> this.taskCount(start, end, true, null));
        //完成的运营工单数
        var completedSupplyFuture = CompletableFuture.supplyAsync(() -> this.taskCount(start, end, false, VMSystem.TASK_STATUS_FINISH));
        //完成的运维工单数
        var completedRepairFuture = CompletableFuture.supplyAsync(() -> this.taskCount(start, end, true, VMSystem.TASK_STATUS_FINISH));
        //拒绝的运营工单数
        var cancelSupplyFuture = CompletableFuture.supplyAsync(() -> this.taskCount(start, end, false, VMSystem.TASK_STATUS_CANCEL));
        //拒绝的运维工单数
        var cancelRepairFuture = CompletableFuture.supplyAsync(() -> this.taskCount(start, end, true, VMSystem.TASK_STATUS_CANCEL));
        //运营人员数量
        var operatorCountFuture = CompletableFuture.supplyAsync(() -> userService.getOperatorCount());
        //运营人员数量
        var repairerCountFuture = CompletableFuture.supplyAsync(() -> userService.getRepairerCount());

        //并行处理
        CompletableFuture.allOf(supplyTotalFuture,//allOf返回一个新的 CompletableFuture
                repairTotalFuture,
                completedSupplyFuture,
                completedRepairFuture,
                cancelSupplyFuture,
                cancelRepairFuture,
                operatorCountFuture,
                repairerCountFuture)
                .join();//join完成时返回结果值

        List<TaskReportInfo> result = Lists.newArrayList();
        //构建返回的运营/运维VO
        var supplyTaskInfo = new TaskReportInfo();//运营
        var repairTaskInfo = new TaskReportInfo();//运维

        try {
            //分别处理运营/运维的dto to vo
            //运营
            supplyTaskInfo.setTotal(supplyTotalFuture.get());
            supplyTaskInfo.setCancelTotal(cancelSupplyFuture.get());
            supplyTaskInfo.setCompletedTotal(completedSupplyFuture.get());
            supplyTaskInfo.setRepair(false);
            supplyTaskInfo.setWorkerCount(operatorCountFuture.get());
            result.add(supplyTaskInfo);

            //运维
            repairTaskInfo.setTotal(repairTotalFuture.get());
            repairTaskInfo.setCancelTotal(cancelRepairFuture.get());
            repairTaskInfo.setCompletedTotal(completedRepairFuture.get());
            repairTaskInfo.setRepair(true);
            repairTaskInfo.setWorkerCount(repairerCountFuture.get());

            result.add(repairTaskInfo);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("构建工单统计数据失败", e);
        }

        return result;
    }

    @Override
    public UserWork getUserWork(Integer userId, LocalDateTime start, LocalDateTime end) {

        var userWork = new UserWork();
        userWork.setUserId(userId);

        //获取用户完成工单数
        var workCountFuture = CompletableFuture.supplyAsync(() -> this.getCountByUserId(userId, VMSystem.TASK_STATUS_FINISH, start, end))
                //r=result e=exception
                .whenComplete((r, e) -> {
                    if (e != null) {
                        userWork.setWorkCount(0);
                        log.error("user work error", e);
                    } else {
                        userWork.setWorkCount(r);
                    }
                });
        //获取用户总工单数
        var totalFuture = CompletableFuture.supplyAsync(() -> this.getCountByUserId(userId, null, start, end))
                .whenComplete((r, e) -> {
                    if (e != null) {
                        userWork.setTotal(0);
                        log.error("user work error", e);
                    } else {
                        userWork.setTotal(r);
                    }
                });
        //获取用户拒绝工单数
        var cancelCountFuture = CompletableFuture.supplyAsync(() -> this.getCountByUserId(userId, VMSystem.TASK_STATUS_CANCEL, start, end))
                .whenComplete((r, e) -> {
                    if (e != null) {
                        userWork.setCancelCount(0);
                        log.error("user work error", e);
                    } else {
                        userWork.setCancelCount(r);
                    }
                });
        //获取用户进行中工单数
        var progressTotalFuture = CompletableFuture.supplyAsync(() -> this.getCountByUserId(userId, VMSystem.TASK_STATUS_PROGRESS, start, end))
                .whenComplete((r, e) -> {
                    if (e != null) {
                        userWork.setProgressTotal(0);
                        log.error("user work error", e);
                    } else {
                        userWork.setProgressTotal(r);
                    }
                });

        //并发执行
        CompletableFuture.allOf(workCountFuture,
                totalFuture, cancelCountFuture,
                progressTotalFuture).join();

        return userWork;
    }

    /**
     * 根据用户id、工单状态查询工单数
     *
     * @param userId
     * @param taskStatus
     * @param start
     * @param end
     * @return
     */
    private Integer getCountByUserId(Integer userId, Integer taskStatus, LocalDateTime start, LocalDateTime end) {

        var qw = new LambdaQueryWrapper<TaskEntity>();
        qw.ge(TaskEntity::getUpdateTime, start)
                .le(TaskEntity::getUpdateTime, end);

        if (taskStatus != null) {
            qw.eq(TaskEntity::getTaskStatus, taskStatus);//匹配订单状态
        }
        if (userId != null) {
            qw.eq(TaskEntity::getAssignorId, userId);//匹配用户id
        }
        return this.count(qw);
    }


    /**
     * 统计工单数量
     *
     * @param start
     * @param end
     * @param repair
     * @param taskStatus
     * @return
     */
    private int taskCount(LocalDateTime start, LocalDateTime end, Boolean repair, Integer taskStatus) {

        LambdaQueryWrapper<TaskEntity> qw = new LambdaQueryWrapper<>();
        qw.ge(TaskEntity::getUpdateTime, start)
                .le(TaskEntity::getUpdateTime, end);//时间段
        if (taskStatus != null) {
            qw.eq(TaskEntity::getTaskStatus, taskStatus);//匹配工单状态
        }
        //匹配工单类型
        if (repair) {//如果是运维
            qw.ne(TaskEntity::getProductTypeId, VMSystem.TASK_TYPE_SUPPLY);
        } else {
            qw.eq(TaskEntity::getProductTypeId, VMSystem.TASK_TYPE_SUPPLY);
        }
        return this.count(qw);
    }


    /**
     * 查询同一台设备下是否存在未完成的工单
     *
     * @param innerCode
     * @param productionType
     * @return
     */
    private boolean hasTask(String innerCode, int productionType) {
        QueryWrapper<TaskEntity> qw = new QueryWrapper<>();
        qw.lambda().select(TaskEntity::getTaskId)
                .eq(TaskEntity::getInnerCode, innerCode)
                .eq(TaskEntity::getProductTypeId, productionType)
                .le(TaskEntity::getTaskStatus, VMSystem.TASK_STATUS_PROGRESS);
        return this.count(qw) > 0;
    }

    /**
     * 工单状态的校验:校验工单类型和对应的机器状态是否匹配
     *
     * @param innerCode
     * @param productType
     * @throws LogicException
     */
    private void checkCreateTask(String innerCode, int productType) throws LogicException {
        /**
         * 远程调用的实际调用
         * @see com.lkd.http.controller.VendingMachineController#getByInnerCode(String)
         */
        VendingMachineViewModel vmInfo = vmService.getVMInfo(innerCode);
        //设备校验失败
        if (vmInfo == null) {
            throw new LogicException("设备校验失败");
        }
        //投放工单但是机器在运营
        if (productType == VMSystem.TASK_TYPE_DEPLOY && vmInfo.getVmStatus() == VMSystem.VM_STATUS_RUNNING) {
            throw new LogicException("该设备已在运营");
        }
        //补货工单但是机器不在运营
        if (productType == VMSystem.TASK_TYPE_SUPPLY && vmInfo.getVmStatus() != VMSystem.VM_STATUS_RUNNING) {
            throw new LogicException("该设备不在运营状态");
        }
        //撤机工单但是机器不在运营
        if (productType == VMSystem.TASK_TYPE_REVOKE && vmInfo.getVmStatus() != VMSystem.VM_STATUS_RUNNING) {
            throw new LogicException("该设备不在运营状态");
        }
    }

    /**
     * 生成工单编号
     * 工单编码的组成:yyyyMMddHH+xxxx(四位数字)-20201218+0002
     *
     * @return
     */
    private String generateTaskCode() {
        //日期(年+月+日)序号
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));  //日期字符串
        String key = "lkd.task.code." + date; //redis key
        Object obj = redisTemplate.opsForValue().get(key);
        //第一次生成工单编号
        if (obj == null) {
            redisTemplate.opsForValue().set(key, 1L, Duration.ofDays(1));
            return date + "0001";
        }
        //不是第一次,就在原有工单编号基础上+1
        return date + Strings.padStart(redisTemplate.opsForValue().increment(key, 1).toString(), 4, '0');
    }

    /**
     * 更新工单统计量列表
     *
     * @param taskEntity
     * @param score
     */
    private void updateTaskZSet(TaskEntity taskEntity, int score) {
        String roleCode = "1003";//运维工单
        if (taskEntity.getProductTypeId().intValue() == 2) { //运营工单
            roleCode = "1002";
        }
        String key = VMSystem.REGION_TASK_KEY_PREF
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "." + taskEntity.getRegionId() + "." + roleCode;
        //原子增
        redisTemplate.opsForZSet().incrementScore(key, taskEntity.getAssignorId(), score);
    }
}
