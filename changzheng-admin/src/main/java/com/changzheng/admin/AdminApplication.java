package com.changzheng.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * 管理后台启动类
 */
@SpringBootApplication
@MapperScan("com.changzheng.admin.mapper")
@ComponentScan(basePackages = {"com.changzheng.admin", "com.changzheng.common"})
public class AdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
