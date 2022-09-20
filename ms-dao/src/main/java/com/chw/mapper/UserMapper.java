package com.chw.mapper;

import com.chw.domain.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Author CHW
 * @Date 2022/9/13
 **/
@Mapper
public interface UserMapper {
    int deleteByPrimaryKey(Long id);
    
    int insert(User record);
    
    int insertSelective(User record);
    
    User selectByPrimaryKey(Long id);
    
    int updateByPrimaryKeySelective(User record);
    
    int updateByPrimaryKey(User record);
}
