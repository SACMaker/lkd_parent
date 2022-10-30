package com.lkd.http.controller;

import com.lkd.entity.SkuClassEntity;
import com.lkd.entity.SkuEntity;
import com.lkd.exception.LogicException;
import com.lkd.feignService.OrderService;
import com.lkd.file.FileManager;
import com.lkd.service.SkuService;
import com.lkd.viewmodel.Pager;
import com.lkd.viewmodel.SkuViewModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;
@Slf4j
@RestController
@RequestMapping("/sku")
public class SkuController {
    private String folder = "image";
    @Autowired
    private SkuService skuService;
    @Autowired
    private FileManager fileManager;
    @Autowired
    private OrderService orderService;

    /**
     * 根据skuId查询
     *
     * @param skuId
     * @return 实体
     */
    @GetMapping("/{skuId}")
    public SkuEntity findById(@PathVariable Long skuId) {
        return skuService.getById(skuId);
    }

    /**
     * 微服务调用接口
     *
     * @param skuId
     * @return
     */
    @GetMapping("/skuViewModel/{skuId}")
    public SkuViewModel getVmById(@PathVariable long skuId) {
        SkuViewModel vm = new SkuViewModel();
        SkuEntity skuEntity = this.findById(skuId);
        vm.setPrice(skuEntity.getPrice());
        vm.setSkuId(skuEntity.getSkuId());
        vm.setSkuName(skuEntity.getSkuName());
        vm.setImage(skuEntity.getSkuImage());
        vm.setClassId(skuEntity.getClassId());

        return vm;
    }

    /**
     * 新增
     *
     * @param sku
     * @return 是否成功
     */
    @PostMapping
    public boolean add(@RequestBody SkuEntity sku) throws LogicException {
        return skuService.save(sku);
    }

    /**
     * 修改
     *
     * @param sku
     * @return 是否成功
     */
    @PutMapping("/{skuId}")
    public boolean update(@PathVariable String skuId, @RequestBody SkuEntity sku) throws LogicException {
        sku.setSkuId(Long.valueOf(skuId));

        return skuService.update(sku);
    }


    /**
     * 分页查询
     *
     * @param pageIndex 页码
     * @param pageSize  页大小
     * @param classId   商品类别Id
     * @param skuName   商品名称
     * @return 分页结果
     */
    @GetMapping("/search")
    public Pager<SkuEntity> findPage(
            @RequestParam(value = "pageIndex", required = false, defaultValue = "1") long pageIndex,
            @RequestParam(value = "pageSize", required = false, defaultValue = "10") long pageSize,
            @RequestParam(value = "skuName", required = false, defaultValue = "") String skuName,
            @RequestParam(value = "classId", required = false, defaultValue = "0") Integer classId) {
        return skuService.findPage(pageIndex, pageSize, classId, skuName);
    }

    /**
     * 控制层:处理文件上传
     *
     * @param file
     * @return
     */
    @PostMapping(value = "/fileUpload")
    @ResponseBody
    public String uploadSkuImage(@RequestParam("fileName") MultipartFile file) {
        log.error("/fileUpload");
        return fileManager.uploadFile(file);
    }

    /**
     * 获取商品品类列表
     *
     * @return
     */
    @GetMapping("/classes")
    public List<SkuClassEntity> getAllSkuClasses() {
        return skuService.getAllClass();
    }

    /**
     * 获取商圈下销量前10的商品
     *
     * @return
     */
    @GetMapping("/businessTop10/{businessId}")
    public List<SkuViewModel> getBusinessTop10Skus(@PathVariable Integer businessId) {
        return orderService
                .getBusinessTop10Skus(businessId)
                .stream()
                .map(s -> this.getVmById(s))
                .collect(Collectors.toList());
    }


}
