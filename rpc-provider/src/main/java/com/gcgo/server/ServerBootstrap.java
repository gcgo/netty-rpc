package com.gcgo.server;

import com.gcgo.BootStrap;
import com.gcgo.ioc.SpringApplication2;
import com.gcgo.service.UserServiceImpl;

public class ServerBootstrap {
    public static void main(String[] args) throws InterruptedException {

        SpringApplication2.run(BootStrap.class);
        UserServiceImpl.startService("127.0.0.1", 9888);
    }
}
