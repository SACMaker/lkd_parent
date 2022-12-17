package com.lkd.http.controller;

import com.lkd.entity.OrderCollectEntity;
import com.lkd.service.OrderService;
import com.lkd.service.ReportService;
import com.lkd.viewmodel.Pager;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RequiredArgsConstructor
@RequestMapping("/report")
@RestController
public class ReportController {


    private final ReportService reportService;
    private final OrderService orderService;


    /**
     * 获取一定日期范围之内的合作商分成汇总数据
     *
     * @param pageIndex
     * @param pageSize
     * @param partnerName
     * @param start
     * @param end
     * @return
     */
    @GetMapping("/partnerCollect")
    public Pager<OrderCollectEntity> getPartnerCollect(
            @RequestParam(value = "pageIndex", required = false, defaultValue = "1") Long pageIndex,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") Long pageSize,
            @RequestParam(value = "partnerName", required = false, defaultValue = "") String partnerName,
            @RequestParam(value = "start", required = true, defaultValue = "")
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
            @RequestParam(value = "end", required = true, defaultValue = "")
            @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end
    ) {
        return reportService.getPartnerCollect(pageIndex, pageSize, partnerName, start, end);
    }
}
