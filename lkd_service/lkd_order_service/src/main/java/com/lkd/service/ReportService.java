package com.lkd.service;

import com.lkd.entity.OrderCollectEntity;
import com.lkd.viewmodel.Pager;

import java.time.LocalDate;

public interface ReportService {

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
    Pager<OrderCollectEntity> getPartnerCollect(Long pageIndex,
                                                Long pageSize,
                                                String name,
                                                LocalDate start,
                                                LocalDate end);
}
