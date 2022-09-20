package com.chw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @Author CHW
 * @Date 2022/9/13
 **/
@SpringBootApplication(scanBasePackages="com.chw")
public class MSApplication {
    public static void main(String[] args) {
        SpringApplication.run(MSApplication.class, args);
    }
}
