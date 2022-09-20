package com.chw.service.impl;

import com.chw.domain.Stock;
import com.chw.mapper.StockMapper;
import com.chw.service.StockService;
import com.chw.util.CacheKey;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @Author CHW
 * @Date 2022/9/13
 **/
@Service
@Log4j2
public class StockServiceImpl implements StockService {
    @Resource
    private StockMapper stockMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    
    
    @Override
    public Integer getStockCount(int id) {
        Integer stockLeft = getStockCountByCache(id);
        log.info("查询缓存得到商品[{}]库存为:[{}]", id, stockLeft);
        if (stockLeft == null) {
            stockLeft = getStockCountByDB(id);
            log.info("缓存未命中，查询数据库，并写入数据库");
            setStockCountCache(id, stockLeft);
        }
        return stockLeft;
    }
    
    @Override
    public int getStockCountByDB(int id) {
        Stock stock = stockMapper.selectByPrimaryKey(id);
        return stock.getCount() - stock.getSale();
    }
    
    @Override
    public Integer getStockCountByCache(int id) {
        String hashKey = CacheKey.STOCK_COUNT.getKey() + "_" + id;
        String countStr = stringRedisTemplate.opsForValue().get(hashKey);
        if (countStr != null) {
            return Integer.parseInt(countStr);
        }
        return null;
    }
    
    @Override
    public void setStockCountCache(int id, int count) {
        String keyHash = CacheKey.STOCK_COUNT.getKey() + "_" + id;
        String countStr = String.valueOf(count);
        log.info("写入商品库存缓存：[{}] [{}]", keyHash, countStr);
        stringRedisTemplate.opsForValue().set(keyHash, countStr, 3600, TimeUnit.SECONDS);
    }
    
    @Override
    public void delStockCountCache(int id) {
        String keyHash = CacheKey.STOCK_COUNT.getKey() + "_" + id;
        stringRedisTemplate.delete(keyHash);
        log.info("删除商品[{}]库存的缓存" , id);
        
    }
    
    @Override
    public Stock getStockById(int id) {
        return stockMapper.selectByPrimaryKey(id);
    }
    
    @Override
    public Stock getStockByIdForUpdate(int id) {
        return stockMapper.selectByPrimaryKeyForUpdate(id);
    }
    
    @Override
    public int updateStockById(Stock stock) {
        return stockMapper.updateByPrimaryKeySelective(stock);
    }
    
    @Override
    public int updateStockByOptimistic(Stock stock) {
        return stockMapper.updateByOptimistic(stock);
    }
}
