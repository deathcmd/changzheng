package com.changzheng.rank;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
@MapperScan("com.changzheng.rank.mapper")
public class RankApplication {
    public static void main(String[] args) {
        SpringApplication.run(RankApplication.class, args);
    }
}
