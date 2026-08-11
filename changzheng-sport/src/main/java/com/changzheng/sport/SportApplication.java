package com.changzheng.sport;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 运动服务启动类
 */
@SpringBootApplication(scanBasePackages = "com.changzheng")
@EnableDiscoveryClient
@EnableScheduling
@MapperScan("com.changzheng.sport.mapper")
public class SportApplication {

    public static void main(String[] args) {
        SpringApplication.run(SportApplication.class, args);
    }
}
