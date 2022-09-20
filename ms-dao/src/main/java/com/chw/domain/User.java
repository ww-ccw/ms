package com.chw.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @Author CHW
 * @Date 2022/9/13
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class User {
    private Long id;
    
    private String userName;
    
}
