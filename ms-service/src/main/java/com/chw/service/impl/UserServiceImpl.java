package com.chw.service.impl;

import com.chw.domain.Stock;
import com.chw.domain.User;
import com.chw.mapper.UserMapper;
import com.chw.service.StockService;
import com.chw.service.UserService;
import com.chw.util.CacheKey;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @Author CHW
 * @Date 2022/9/13
 **/
@Service
@Log4j2
public class UserServiceImpl implements UserService {
    
    private static final String SALT = "MS";
    
    private int maxVisits = 30;
    
    @Resource
    private UserMapper userMapper;
    
    @Resource
    private StockService stockService;
    
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    
    
    /**
     * 检查商品合法性、用户合法性并获取由该用户及对应商品的验证码
     * 同时有加盐SALT为前缀的key，避免被猜到
     * 将其写入redis中
     *
     * @param sid
     * @param userId
     * @return
     * @throws Exception
     */
    @Override
    public String getVerifyHash(Integer sid, Integer userId) throws Exception {
        log.info("验证是否在抢购时间内");
        
        //检查用户合法性
        User user = userMapper.selectByPrimaryKey(userId.longValue());
        if (user == null) {
            throw new Exception("用户不存在");
        }
        log.info("用户信息：[{}]", user.toString());
        
        //检查商品合法性
        Stock stock = stockService.getStockById(sid);
        if (stock == null) {
            throw new Exception("商品不存在");
        }
        log.info("商品信息：[{}]", stock.toString());
        
        //生成hash
        String verify = SALT + sid + userId;
        String verifyHash = DigestUtils.md5DigestAsHex(verify.getBytes());
        
        //将hash和用户商品信息存入redis
        String hashKey = CacheKey.HASH_KEY.getKey() + "_" + sid + "_" + userId;
        stringRedisTemplate.opsForValue().set(hashKey, verifyHash, 3600, TimeUnit.SECONDS);
        log.info("redis写入：[{}] [{}]", hashKey, verifyHash);
        return verifyHash;
    }
    
    @Override
    public int addUserCount(Integer userId) throws Exception {
        String limitKey = CacheKey.LIMIT_KEY.getKey() + "_" + userId;
        String limitNum = stringRedisTemplate.opsForValue().get(limitKey);
        int limit = 0;
        if (limitNum == null) {
            stringRedisTemplate.opsForValue().set(limitKey, "" + limit + 1, 3600, TimeUnit.SECONDS);
        } else {
            limit = Integer.parseInt(limitNum) + 1;
            stringRedisTemplate.opsForValue().set(limitKey, "" + limit, 3600, TimeUnit.SECONDS);
        }
        return limit;
    }
    
    @Override
    public boolean getUserIsBanned(Integer userId) {
        String limitKey = CacheKey.LIMIT_KEY.getKey() + "_" + userId;
        Integer limitNum = Integer.valueOf(stringRedisTemplate.opsForValue().get(limitKey));
        if (limitNum == null) {
            log.error("该用户没有访问记录");
            return true;
        }
        return limitNum > maxVisits;
    }
}
