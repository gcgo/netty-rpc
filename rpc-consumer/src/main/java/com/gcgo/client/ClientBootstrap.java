package com.gcgo.client;

import com.gcgo.service.UserService;

public class ClientBootstrap {
    private static final String providerName = "UserService#sayHello#";

    public static void main(String[] args) throws InterruptedException {
        RpcConsumer rpcConsumer = new RpcConsumer();
        UserService proxy = (UserService) rpcConsumer.createProxy(UserService.class, providerName);
        while (true) {
            Thread.sleep(1500);
            System.out.println(proxy.sayHello("success"));
        }
    }
}
