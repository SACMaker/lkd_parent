package com.lkd.filter;

import com.google.common.base.Strings;
import com.lkd.common.VMSystem;
import com.lkd.config.GatewayConfig;
import com.lkd.http.view.TokenObject;
import com.lkd.service.UserService;
import com.lkd.utils.JWTUtil;
import com.lkd.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * JWT filter 用于网关鉴权
 * 注意:所有请求都需要经过网关,配置文件中设置的不用经过网关(eg:/login-登录)直接放行
 */
@Component
@Slf4j
public class JwtTokenFilter implements GlobalFilter, Ordered{

    @Autowired
    private GatewayConfig gatewayConfig;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String url = exchange.getRequest().getURI().getPath();
        //跳过不需要验证的路径
        boolean matchUrl = Arrays.stream(gatewayConfig.getUrls())
                .anyMatch(url::contains);
        if(matchUrl){
            return chain.filter(exchange);
        }
        if(null != gatewayConfig.getUrls()&& Arrays.asList(gatewayConfig.getUrls()).contains(url)){
            return chain.filter(exchange);
        }
        //从请求的"Authorization"拿JWT
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        ServerHttpResponse resp = exchange.getResponse();
        if(Strings.isNullOrEmpty(token)) return authError(resp);

        try {
            //解码JWT
            TokenObject tokenObject = JWTUtil.decode(token);
            //校验JWT
            JWTUtil.VerifyResult verifyResult = JWTUtil.verifyJwt(token,tokenObject.getMobile()+VMSystem.JWT_SECRET);
            if(!verifyResult.isValidate()) return authError(resp);
        } catch (IOException e) {
            return authError(resp);
        }
        //校验JWT通过放行
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -100;
    }

    /**
     * 认证错误输出
     * @param resp 响应对象
     * @return
     */
    private Mono<Void> authError(ServerHttpResponse resp) {
        resp.setStatusCode(HttpStatus.UNAUTHORIZED);
        resp.getHeaders().add("Content-Type","application/json;charset=UTF-8");
        String returnStr = "token校验失败";
        DataBuffer buffer = resp.bufferFactory().wrap(returnStr.getBytes(StandardCharsets.UTF_8));
        return resp.writeWith(Flux.just(buffer));
    }
}
