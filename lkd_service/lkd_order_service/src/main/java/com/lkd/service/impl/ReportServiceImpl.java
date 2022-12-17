package com.lkd.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.base.Strings;
import com.lkd.entity.OrderCollectEntity;
import com.lkd.service.OrderCollectService;
import com.lkd.service.OrderService;
import com.lkd.service.ReportService;
import com.lkd.viewmodel.Pager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final OrderCollectService orderCollectService;
    private final OrderService orderService;

    /**
     * 获取合作商分账汇总信息
     * @param pageIndex
     * @param pageSize
     * @param name
     * @param start
     * @param end
     * @return
     */
    @Override
    public Pager<OrderCollectEntity> getPartnerCollect(Long pageIndex,
                                                       Long pageSize,
                                                       String name,
                                                       LocalDate start,
                                                       LocalDate end) {
        Page<OrderCollectEntity> page = new Page<>(pageIndex, pageSize);
        var qw = new QueryWrapper<OrderCollectEntity>();
        //数据汇总:订单总数/分成金额总数/总金额/分成比例/owner_name/date
        qw.select("IFNULL( sum( order_count ),0) as order_count",
                "IFNULL( sum( total_bill ),0) as total_bill",
                "IFNULL( sum( order_total_money ),0) as order_total_money  ",
                "IFNULL( min(ratio),0) as ratio ",
                "owner_name", "date");
        //名称模拟查询
        if (!Strings.isNullOrEmpty(name)) {
            qw.lambda().like(OrderCollectEntity::getOwnerName, name);
        }
        //时间范围查询
        qw.lambda()
                .ge(OrderCollectEntity::getDate, start)
                .le(OrderCollectEntity::getDate, end)
                .groupBy(OrderCollectEntity::getOwnerName, OrderCollectEntity::getDate)
                .orderByDesc(OrderCollectEntity::getDate);


        return Pager.build(orderCollectService.page(page, qw));
    }
}
