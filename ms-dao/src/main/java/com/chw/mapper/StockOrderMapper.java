package com.chw.mapper;

import com.chw.domain.StockOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Author CHW
 * @Date 2022/9/13
 **/
@Mapper
public interface StockOrderMapper {
    int deleteByPrimaryKey(Integer id);
    
    int insert(StockOrder record);
    
    /**
     * 使用最初的stock_order表存储信息
     * @param record
     * @return
     */
    int insertSelective(StockOrder record);
    
    int insertSelectiveUser(StockOrder order);
    
    StockOrder selectByPrimaryKey(Integer id);
    
    int updateByPrimaryKeySelective(StockOrder record);
    
    int updateByPrimaryKey(StockOrder record);
}
