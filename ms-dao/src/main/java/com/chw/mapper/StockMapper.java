package com.chw.mapper;

import com.chw.domain.Stock;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Author CHW
 * @Date 2022/9/13
 **/
@Mapper
public interface StockMapper {
    
    int deleteByPrimaryKey(Integer id);
    
    int insert(Stock record);
    
    int insertSelective(Stock record);
    
    Stock selectByPrimaryKey(Integer id);
    
    Stock selectByPrimaryKeyForUpdate(Integer id);
    
    int updateByPrimaryKeySelective(Stock record);
    
    int updateByPrimaryKey(Stock record);
    
    int updateByOptimistic(Stock record);}
