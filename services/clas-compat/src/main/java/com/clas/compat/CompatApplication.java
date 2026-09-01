package com.clas.compat;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan("com.clas.mapper")
@SpringBootApplication(scanBasePackages = "com.clas")
public class CompatApplication {
    public static void main(String[] args) {
        SpringApplication.run(CompatApplication.class, args);
    }
}
