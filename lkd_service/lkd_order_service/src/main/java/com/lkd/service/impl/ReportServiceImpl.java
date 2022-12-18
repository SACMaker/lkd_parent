package com.lkd.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.google.common.base.Strings;
import com.lkd.entity.OrderCollectEntity;
import com.lkd.service.OrderCollectService;
import com.lkd.service.OrderService;
import com.lkd.service.ReportService;
import com.lkd.viewmodel.BarCharCollect;
import com.lkd.viewmodel.Pager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final OrderCollectService orderCollectService;
    private final OrderService orderService;

    /**
     * 获取合作商分账汇总信息
     *
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

    @Override
    public List<OrderCollectEntity> getTop12(Integer partnerId) {
        var qw = new LambdaQueryWrapper<OrderCollectEntity>();
        qw.select(OrderCollectEntity::getDate, OrderCollectEntity::getNodeName, OrderCollectEntity::getOrderCount, OrderCollectEntity::getTotalBill)
                .eq(OrderCollectEntity::getOwnerId, partnerId)
                .orderByDesc(OrderCollectEntity::getDate)
                .last("limit 12");

        return orderCollectService.list(qw);
    }

    @Override
    public Pager<OrderCollectEntity> search(Long pageIndex,
                                            Long pageSize,
                                            Integer partnerId,
                                            String nodeName,
                                            LocalDate start,
                                            LocalDate end) {
        var qw = new LambdaQueryWrapper<OrderCollectEntity>();
        qw.select(OrderCollectEntity::getDate, OrderCollectEntity::getNodeName, OrderCollectEntity::getOrderCount, OrderCollectEntity::getTotalBill)
                .eq(OrderCollectEntity::getOwnerId, partnerId);
        if (!Strings.isNullOrEmpty(nodeName)) {
            qw.like(OrderCollectEntity::getNodeName, nodeName);
        }
        if (start != null && end != null) {
            qw.ge(OrderCollectEntity::getDate, start)
                    .le(OrderCollectEntity::getDate, end);
        }
        qw.orderByDesc(OrderCollectEntity::getDate);
        var page = new Page<OrderCollectEntity>(pageIndex, pageSize);
        return Pager.build(orderCollectService.page(page, qw));
    }

    @Override
    public List<OrderCollectEntity> getList(Integer partnerId, String nodeName, LocalDate start, LocalDate end) {

        var qw = new LambdaQueryWrapper<OrderCollectEntity>();

        qw.select(OrderCollectEntity::getDate, OrderCollectEntity::getNodeName,
                        OrderCollectEntity::getOrderCount, OrderCollectEntity::getTotalBill)
                .eq(OrderCollectEntity::getOwnerId, partnerId);

        if (!Strings.isNullOrEmpty(nodeName)) {
            qw.like(OrderCollectEntity::getNodeName, nodeName);
        }
        if (start != null && end != null) {
            qw.ge(OrderCollectEntity::getDate, start)
                    .le(OrderCollectEntity::getDate, end);
        }
        qw.orderByDesc(OrderCollectEntity::getDate);

        return orderCollectService.list(qw);
    }

    @Override
    public BarCharCollect getCollect(Integer partnerId, LocalDate start, LocalDate end) {

        var qw = new QueryWrapper<OrderCollectEntity>();
        qw.select(" IFNULL( sum(total_bill),0) as total_bill", "date")
                .lambda()
                .ge(OrderCollectEntity::getDate, start)
                .le(OrderCollectEntity::getDate, end)
                .eq(OrderCollectEntity::getOwnerId, partnerId)
                .groupBy(OrderCollectEntity::getDate);

        var mapCollect = orderCollectService.list(qw).stream()
                .collect(Collectors.toMap(OrderCollectEntity::getDate, OrderCollectEntity::getTotalBill));

        var result = new BarCharCollect();

        //LocalDate的datesUntil方法：用户希望返回两个给定端点之间的日期流
        start.datesUntil(end.plusDays(1), Period.ofDays(1))
                .forEach(date -> {
                    result.getXAxis().add(date.format(DateTimeFormatter.ISO_LOCAL_DATE));
                    if (mapCollect.containsKey(date)) {
                        result.getSeries().add(mapCollect.get(date));
                    } else {
                        result.getSeries().add(0);
                    }
                });

        return result;
    }
}
