package com.lkd.feignService;

import com.lkd.feignService.fallback.TaskServiceFallbackFactory;
import com.lkd.viewmodel.UserWork;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.LocalDateTime;

@FeignClient(value = "task-service",fallbackFactory = TaskServiceFallbackFactory.class)
public interface TaskService {
    @GetMapping("/task/supplyAlertValue")
    Integer getSupplyAlertValue();
    @GetMapping("/userWork/{userId}/{start}/{end}")
    UserWork getUserWork(@PathVariable Integer userId, @PathVariable LocalDateTime start, @PathVariable LocalDateTime end);
}
