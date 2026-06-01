package com.clas;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.clas.mapper")
@SpringBootApplication
public class ClasApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClasApplication.class, args);
    }
}
