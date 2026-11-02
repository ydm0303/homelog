package com.homelog;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@EnableAsync
@EnableScheduling
@EnableJpaRepositories(basePackages = "com.homelog.mapper")
@EntityScan(basePackages = "com.homelog.entity")
@MapperScan("com.homelog.mapper")
@ComponentScan(basePackages = "com.homelog")
public class HomeLogApplication {

    public static void main(String[] args) {
        SpringApplication.run(HomeLogApplication.class, args);
    }
}