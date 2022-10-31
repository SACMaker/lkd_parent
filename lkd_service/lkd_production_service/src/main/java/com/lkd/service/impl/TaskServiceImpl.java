package com.lkd.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.lkd.common.VMSystem;
import com.lkd.dao.TaskDao;
import com.lkd.entity.TaskDetailsEntity;
import com.lkd.entity.TaskEntity;
import com.lkd.entity.TaskStatusTypeEntity;
import com.lkd.exception.LogicException;
import com.lkd.feignService.UserService;
import com.lkd.feignService.VMService;
import com.lkd.http.viewModel.CancelTaskViewModel;
import com.lkd.http.viewModel.TaskViewModel;
import com.lkd.service.TaskDetailsService;
import com.lkd.service.TaskService;
import com.lkd.service.TaskStatusTypeService;
import com.lkd.viewmodel.Pager;
import com.lkd.viewmodel.UserViewModel;
import com.lkd.viewmodel.VendingMachineViewModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
        //this.checkCreateTask(taskViewModel.getInnerCode(), taskViewModel.getProductType());
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
        ;
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
        return true;
    }

    /**
     * 服务层实现类:接受工单
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

    @Override
    public boolean cancelTask(long id, CancelTaskViewModel cancelVM) {
        TaskEntity task = this.getById(id);
        if(task.getTaskStatus() == VMSystem.TASK_STATUS_FINISH || task.getTaskStatus() == VMSystem.TASK_STATUS_CANCEL){
            throw new LogicException("该工单已经结束");
        }
        task.setTaskStatus(VMSystem.TASK_STATUS_CANCEL);
        task.setDesc(cancelVM.getDesc());

        return this.updateById(task);
    }


    @Override
    public boolean completeTask(long id) {
        TaskEntity taskEntity = this.getById(id);
        taskEntity.setTaskStatus(VMSystem.TASK_STATUS_FINISH);
        this.updateById(taskEntity);
        //todo: 向消息队列发送消息，通知售货机更改状态
        return true;
    }


    @Override
    public List<TaskStatusTypeEntity> getAllStatus() {
        QueryWrapper<TaskStatusTypeEntity> qw = new QueryWrapper<>();
        qw.lambda()
                .ge(TaskStatusTypeEntity::getStatusId, VMSystem.TASK_STATUS_CREATE);

        return statusTypeService.list(qw);
    }

    @Override
    public Pager<TaskEntity> search(Long pageIndex, Long pageSize, String innerCode, Integer userId, String taskCode, Integer status, Boolean isRepair, String start, String end) {
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
            qw
                    .ge(TaskEntity::getCreateTime, LocalDate.parse(start, DateTimeFormatter.ISO_LOCAL_DATE))
                    .le(TaskEntity::getCreateTime, LocalDate.parse(end, DateTimeFormatter.ISO_LOCAL_DATE));
        }
        //根据最后更新时间倒序排序
        qw.orderByDesc(TaskEntity::getUpdateTime);

        return Pager.build(this.page(page, qw));
    }


    /**
     * 获取同一天内分配的工单最少的人
     *
     * @param innerCode
     * @param isRepair  是否是维修工单
     * @return
     */
    @Override
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
}
