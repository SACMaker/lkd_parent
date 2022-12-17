package com.lkd.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.lkd.common.VMSystem;
import com.lkd.conf.OrderConfig;
import com.lkd.config.TopicConfig;
import com.lkd.contract.VendoutReq;
import com.lkd.contract.VendoutReqData;
import com.lkd.contract.VendoutResp;
import com.lkd.contract.server.OrderCheck;
import com.lkd.dao.OrderDao;
import com.lkd.emq.MqttProducer;
import com.lkd.entity.OrderEntity;
import com.lkd.feignService.UserService;
import com.lkd.feignService.VMService;
import com.lkd.http.viewModel.CreateOrderReq;
import com.lkd.http.viewModel.OrderResp;
import com.lkd.service.OrderCollectService;
import com.lkd.service.OrderService;
import com.lkd.viewmodel.CreateOrder;
import com.lkd.viewmodel.PartnerViewModel;
import com.lkd.viewmodel.SkuViewModel;
import com.lkd.viewmodel.VendingMachineViewModel;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    private MqttProducer mqttProducer;


    @Autowired
    private OrderCollectService orderCollectService;
    @Autowired
    private VMService vmService;
    @Autowired
    private UserService userService;

    @Override
    public OrderResp createOrder(CreateOrderReq req) {
//        VendingMachineViewModel vendingMachineViewModel = vmService.getCompanyId(req.getInnerCode()).getData();
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderNo(req.getOrderNo());
        orderEntity.setAmount(req.getAmount());
        orderEntity.setInnerCode(req.getInnerCode());
//        orderEntity.setAreaId(vendingMachineViewModel.getAreaId());
        orderEntity.setSkuId(req.getSkuId());
        orderEntity.setSkuName(vmService.getSkuById(req.getSkuId()).getSkuName());
        orderEntity.setPrice(req.getPrice());
        orderEntity.setThirdNo(req.getThirdNO());
        orderEntity.setPayStatus(0);
        orderEntity.setStatus(0);
        orderEntity.setPayType(req.getPayType());

        this.save(orderEntity);

        OrderResp resp = new OrderResp();
        resp.setAmount(req.getAmount());
        resp.setInnerCode(req.getInnerCode());
        resp.setOrderNo(req.getOrderNo());
        resp.setPrice(req.getPrice());
        resp.setSkuId(req.getSkuId());
        resp.setThirdNO(req.getThirdNO());

        return resp;
    }

    /**
     * 创建订单
     *
     * @param createOrder
     * @return
     */
    @Override
    public OrderEntity createOrder(CreateOrder createOrder) {
        //获取机器info
        VendingMachineViewModel vm = vmService.getVMInfo(createOrder.getInnerCode());
        //获取商品info
        SkuViewModel sku = vmService.getSku(createOrder.getInnerCode(), createOrder.getSkuId());
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setAddr(vm.getNodeAddr());
        orderEntity.setNodeId(vm.getNodeId());
        orderEntity.setNodeName(vm.getNodeName());
        orderEntity.setSkuId(sku.getSkuId());
        orderEntity.setSkuName(sku.getSkuName());
        orderEntity.setAmount(sku.getRealPrice());
        orderEntity.setClassId(sku.getClassId());
        orderEntity.setPrice(sku.getPrice());
        orderEntity.setBusinessId(vm.getBusinessId());
        orderEntity.setBusinessName(vm.getBusinessName());
        orderEntity.setInnerCode(createOrder.getInnerCode());
        orderEntity.setOpenId(createOrder.getOpenId());
        orderEntity.setPayStatus(VMSystem.PAY_STATUS_NOPAY);
        orderEntity.setRegionId(vm.getRegionId());
        orderEntity.setRegionName(vm.getRegionName());
        //orderEntity.setOrderNo(createOrder.getInnerCode()+createOrder.getSkuId()+System.nanoTime());
        //设置订单号
        orderEntity.setOrderNo(createOrder.getInnerCode() + System.nanoTime());
        //设置支付类型
        orderEntity.setPayType(createOrder.getPayType());
        orderEntity.setStatus(VMSystem.ORDER_STATUS_CREATE);
        orderEntity.setOwnerId(vm.getOwnerId());

        //在创建订单业务中计算合作商的分成数据并且存到tb_order表中
        //拿到机器对应的合作商INFO
        PartnerViewModel partner = userService.getPartner(vm.getOwnerId());
        BigDecimal price = new BigDecimal(sku.getPrice());
        //计算合作商的分成数据,四舍五入
        BigDecimal bill = price.multiply(new BigDecimal(partner.getRatio()).divide(new BigDecimal(100), 0, RoundingMode.HALF_UP));
        orderEntity.setBill(bill.intValue());

        //保存订单
        this.save(orderEntity);

        //将订单放到延迟队列中，10分钟后检查支付状态
        OrderCheck orderCheck = new OrderCheck();
        orderCheck.setOrderNo(orderEntity.getOrderNo());
        try {
            mqttProducer.send("$delayed/60/" + OrderConfig.ORDER_DELAY_CHECK_TOPIC, 2, orderCheck);
        } catch (JsonProcessingException e) {
            log.error("send to emq error", e);
        }
        return orderEntity;
    }

    /**
     * 订单服务处理出货结果
     * @param vendoutResp 出货请求参数
     * @return
     */
    @Override
    public boolean vendoutResult(VendoutResp vendoutResp) {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setOrderNo(vendoutResp.getVendoutResult().getOrderNo());
        UpdateWrapper<OrderEntity> uw = new UpdateWrapper<>();
        LambdaUpdateWrapper<OrderEntity> lambdaUpdateWrapper = uw.lambda();
        lambdaUpdateWrapper.set(OrderEntity::getPayStatus, 1);
        if (vendoutResp.getVendoutResult().isSuccess()) {
            lambdaUpdateWrapper.set(OrderEntity::getStatus, VMSystem.ORDER_STATUS_VENDOUT_SUCCESS);
        } else {
            lambdaUpdateWrapper.set(OrderEntity::getStatus, VMSystem.ORDER_STATUS_VENDOUT_FAIL);
        }
        lambdaUpdateWrapper.eq(OrderEntity::getOrderNo, vendoutResp.getVendoutResult().getOrderNo());
        //更新出货info,出货成功或者失败
        return this.update(lambdaUpdateWrapper);
    }

    @Override
    public boolean payComplete(String orderNo, String thirdNo) {
        OrderEntity order = this.getByOrderNo(orderNo);
        if (order == null) return false;

        // TODO:2.0项目没有原来的公司概念了
        float bill = 0f;
//        float bill = (float) order.getAmount() * (((float)companyService.findById(order.getCompanyId()).getData().getDivide())/100);

        UpdateWrapper<OrderEntity> uw = new UpdateWrapper<>();
        uw.lambda()
                .eq(OrderEntity::getOrderNo, orderNo)
                .set(OrderEntity::getThirdNo, thirdNo)
                .set(OrderEntity::getPayStatus, 1)
                .set(OrderEntity::getBill, (int) bill);

        //向售货机发起出货请求
        sendVendout(orderNo);

        return this.update(uw);
    }

    /**
     * 支付完成
     * @param orderNo
     * @return
     */
    @Override
    public boolean payComplete(String orderNo) {
        //通知售货机出货
        sendVendout(orderNo);
        return true;
    }


    @Override
    public OrderEntity getByOrderNo(String orderNo) {
        QueryWrapper<OrderEntity> qw = new QueryWrapper<>();
        qw.lambda()
                .eq(OrderEntity::getOrderNo, orderNo);

        return this.getOne(qw);
    }

    /**
     * 取消订单
     *
     * @param orderNo
     * @return
     */
    @Override
    public Boolean cancel(String orderNo) {
        var order = this.getByOrderNo(orderNo);
        if (order.getStatus() > VMSystem.ORDER_STATUS_CREATE)
            return true;

        order.setStatus(VMSystem.ORDER_STATUS_INVALID);
        order.setCancelDesc("用户取消");

        return true;
    }


    /**
     * 通知售货机出货
     *
     * @param orderNo
     */
    private void sendVendout(String orderNo) {
        OrderEntity orderEntity = this.getByOrderNo(orderNo);

        VendoutReqData reqData = new VendoutReqData();
        reqData.setOrderNo(orderNo);
        reqData.setPayPrice(orderEntity.getAmount());
        reqData.setPayType(Integer.parseInt(orderEntity.getPayType()));
        reqData.setSkuId(orderEntity.getSkuId());
        reqData.setTimeout(60);
        reqData.setRequestTime(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));

        //向售货机发送出货请求
        VendoutReq req = new VendoutReq();
        req.setVendoutData(reqData);
        req.setSn(System.nanoTime());
        req.setInnerCode(orderEntity.getInnerCode());
        req.setNeedResp(true);
        //向售货机发送出货请求
        try {
            /**
             * @see TopicConfig#getVendoutTopic(String)
             */
            mqttProducer.send(TopicConfig.getVendoutTopic(orderEntity.getInnerCode()), 2, req);
        } catch (JsonProcessingException e) {
            log.error("send vendout req error.", e);
        }
    }
}
