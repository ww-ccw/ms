package com.chw;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @Author CHW
 * @Date 2022/9/13
 **/
@SpringBootApplication
@MapperScan("com.chw.mapper")
public class MSServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MSServiceApplication.class, args);
    }

}
