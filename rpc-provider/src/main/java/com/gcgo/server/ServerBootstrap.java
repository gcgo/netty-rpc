package com.gcgo.server;

import com.gcgo.service.UserServiceImpl;

public class ServerBootstrap {
    public static void main(String[] args) throws InterruptedException {
//        Map<String, Object> ioc = IocContainer.getIoc();

        UserServiceImpl.startService("127.0.0.1", 9998);
    }
}
