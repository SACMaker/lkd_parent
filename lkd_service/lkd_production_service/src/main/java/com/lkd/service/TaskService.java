package com.lkd.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lkd.entity.TaskEntity;
import com.lkd.entity.TaskStatusTypeEntity;
import com.lkd.exception.LogicException;
import com.lkd.http.viewModel.CancelTaskViewModel;
import com.lkd.http.viewModel.TaskViewModel;
import com.lkd.viewmodel.Pager;

import java.util.List;

/*在mbp中服务层接口除了提供mbp的服务层接口,也是自定义接口的地方,在其对应的实现类实现*/
public interface TaskService extends IService<TaskEntity> {


    /**
     * 服务层接口:创建工单
     *
     * @param taskViewModel
     * @return
     */
    boolean createTask(TaskViewModel taskViewModel) throws LogicException;
    /**
     * 接受工单
     * @param id
     * @return
     */
    boolean accept(Long id);

    /**
     * 取消工单
     * @param id
     * @return
     */
    boolean cancelTask(long id, CancelTaskViewModel cancelVM);


    /**
     * 完成工单
     *
     * @param id
     * @return
     */
    boolean completeTask(long id);

    /**
     * 完成工单重载
     * @param id
     * @param lat
     * @param lon
     * @param addr
     * @return
     */
    boolean completeTask(long id, Double lat, Double lon, String addr);

    /**
     * 获取所有状态类型
     *
     * @return
     */
    List<TaskStatusTypeEntity> getAllStatus();

    /**
     * 通过条件搜索工单列表
     *
     * @param pageIndex
     * @param pageSize
     * @param innerCode
     * @param userId
     * @param taskCode
     * @param isRepair  是否是运维工单
     * @return
     */
    Pager<TaskEntity> search(Long pageIndex, Long pageSize, String innerCode, Integer userId, String taskCode, Integer status, Boolean isRepair, String start, String end);

    /**
     * 获取同一天内分配的工单最少的人
     *
     * @param innerCode
     * @param isRepair  是否是维修工单
     * @return
     */
    Integer getLeastUser(String innerCode, Boolean isRepair);

    /**
     * 获取同一天内分配的工单最少的人
     * @param regionId 区域id
     * @param isRepair 是否是维修工单
     * @return
     */
    Integer getLeastUser(Integer regionId, Boolean isRepair);
}
