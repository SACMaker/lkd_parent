package com.lkd.http.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lkd.entity.VendingMachineEntity;
import com.lkd.service.VendingMachineService;
import lombok.RequiredArgsConstructor;
import lombok.var;
import org.elasticsearch.common.Strings;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("acl")
@RequiredArgsConstructor
public class AclController {
    private final VendingMachineService vmService;
    /**
     * 连接ACL控制
     * @param username
     * @param password
     * @param clientid
     * @return
     */
    @PostMapping("/auth")
    public ResponseEntity<?> auth(@RequestParam String username,
                                  @RequestParam String password,
                                  @RequestParam String clientid){
        System.out.println("userName:"+ username+" password:"+password);
        if(Strings.isNullOrEmpty(clientid)){
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        //服务器端连接
        if(clientid.startsWith("monitor")){
            return new ResponseEntity<>(null, HttpStatus.OK);
        }
        var qw = new LambdaQueryWrapper<VendingMachineEntity>();
        qw.eq(VendingMachineEntity::getClientId,clientid);
        var vm = vmService.getOne(qw);
        if(vm == null){
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    /**
     * 发布订阅控制
     * @param clientid
     * @param topic
     * @param access 动作2：publish、1：subscribe、3：pubsub
     * @param username
     * @return
     */
    @PostMapping("/pubsub")
    public ResponseEntity<?> acl(@RequestParam String clientid,
                                 @RequestParam String topic,
                                 @RequestParam int access,
                                 @RequestParam String username,
                                 @RequestParam String ipaddr,
                                 @RequestParam String mountpoint){
        System.out.println("acl clientid:"+clientid+" username:"+username+" ipaddr:"+ipaddr+" topic:"+topic+" access:"+access+" mountpoint:"+mountpoint);
        if(Strings.isNullOrEmpty(clientid)){
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        if(clientid.startsWith("monitor")){
            return new ResponseEntity<>(null, HttpStatus.OK);
        }
        var qw = new LambdaQueryWrapper<VendingMachineEntity>();
        qw.eq(VendingMachineEntity::getClientId,clientid);
        var vm = vmService.getOne(qw);
        if(vm == null){
            return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
        }
        if(access == 2 && topic.equals("server/"+vm.getInnerCode())){
            return new ResponseEntity<>(null, HttpStatus.OK);
        }
        if(access == 1 && topic.equals("vm/"+vm.getInnerCode())){
            return new ResponseEntity<>(null, HttpStatus.OK);
        }
        return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
    }
}
