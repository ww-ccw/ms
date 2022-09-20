package com.chw.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Date;

/**
 * @Author CHW
 * @Date 2022/9/13
 **/
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class StockOrder {
    private Integer id;
    
    private Integer sid;
    
    private String name;
    
    private Integer userId;
    
    private Date createTime;
    
}
