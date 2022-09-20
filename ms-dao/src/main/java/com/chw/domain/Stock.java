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
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Stock {
    private Integer id;
    
    private String name;
    
    private Integer count;
    
    private Integer sale;
    
    private Integer version;
}
