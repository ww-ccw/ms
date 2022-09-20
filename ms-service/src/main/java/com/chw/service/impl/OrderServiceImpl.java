package com.chw.service.impl;

import com.chw.domain.Stock;
import com.chw.domain.StockOrder;
import com.chw.domain.User;
import com.chw.mapper.StockOrderMapper;
import com.chw.mapper.UserMapper;
import com.chw.service.OrderService;
import com.chw.service.StockService;
import com.chw.util.CacheKey;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

/**
 * @Author CHW
 * @Date 2022/9/13
 **/
@Service
@Log4j2
public class OrderServiceImpl implements OrderService {
    
    @Resource
    StockService stockService;
    @Resource
    StockOrderMapper orderMapper;
    @Resource
    StringRedisTemplate stringRedisTemplate;
    @Resource
    UserMapper userMapper;
    
    @Override
    public int createWrongOrder(int sid) throws RuntimeException {
        //检查库存
        Stock stock = checkStock(sid);
        //扣库存
        saleStock(stock);
        //创建订单
        int id = createOrder(stock);
        return id;
    }
    
    @Override
    public int createOptimisticOrder(int sid) throws RuntimeException {
        //校验库存
        Stock stock = checkStock(sid);
        //乐观锁更新库存
        saleStockOptimistic(stock);
        //创建订单
        int id = createOrder(stock);
        
        return stock.getCount() - (stock.getSale() + 1);
    }
    

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    @Override
    public int createPessimisticOrder(int sid) {
        //校验库存(悲观锁for update)
        Stock stock = checkStockForUpdate(sid);
        //更新库存
        saleStock(stock);
        //创建订单
        createOrder(stock);
        return stock.getCount() - stock.getSale();
    }
    
    /**
     * 悲观锁实现
     *
     * @param sid
     * @return
     */
    private Stock checkStockForUpdate(int sid) {
        Stock stock = stockService.getStockByIdForUpdate(sid);
        if (stock.getSale().equals(stock.getCount())) {
            throw new RuntimeException("库存不足");
        }
        return stock;
    }
    
    
    @Override
    public int createVerifiedOrder(Integer sid, Integer userId, String verifyHash) throws Exception {
        //验证是否在抢购时间内
        log.info("验证抢购时间");
       
        //获取验证码
        String hashKey = CacheKey.HASH_KEY.getKey() + "_" + sid + "_" + userId;
        String verifyHashInRedis = stringRedisTemplate.opsForValue().get(hashKey);
        if (!verifyHashInRedis.equals(verifyHashInRedis)) {
            throw new Exception("hash值与Redis中不符合");
        }
        log.info("验证hash验证码匹配");
        
        //验证用户合法性
        User user = userMapper.selectByPrimaryKey(userId.longValue());
        if (user == null) {
            throw new Exception("用户不存在");
        }
        
        //验证商品合法性
        Stock stock = stockService.getStockById(sid);
        if (stock == null) {
            throw new Exception("商品不存在");
        }
        
        //乐观锁更新库存
        saleStockOptimistic(stock);
        log.info("乐观锁更新库存成功");
        
        //创建订单
        createOrderWithUserInfo(stock, userId);
        log.info("创建订单成功");
        
        return stock.getCount() - stock.getSale();
    }
    
    /**
     * 使用stock_order_user来存储信息
     * @param stock
     * @param userId
     * @return
     */
    private int createOrderWithUserInfo(Stock stock, Integer userId) {
        StockOrder order = new StockOrder();
        order.setSid(stock.getId());
        order.setName(stock.getName());
        order.setUserId(userId);
        return orderMapper.insertSelectiveUser(order);
    }
    
    @Override
    public void createOrderByMq(Integer sid, Integer userId) throws Exception {
    
    }
    
    @Override
    public Boolean checkUserOrderInfoInCache(Integer sid, Integer userId) throws Exception {
        return null;
    }
    
    /**
     * 检查库存
     */
    private Stock checkStock(int sid) {
        Stock stock = stockService.getStockById(sid);
        if (stock.getSale() >= stock.getCount()) {
            throw new RuntimeException("库存不足");
        }
        return stock;
    }
    
    /**
     * 销售
     */
    private int saleStock(Stock stock) {
        stock.setSale(stock.getSale() + 1);
        return stockService.updateStockById(stock);
    }
    
    /**
     * 创建订单
     */
    private int createOrder(Stock stock) {
        StockOrder order = new StockOrder();
        order.setSid(stock.getId());
        order.setName(stock.getName());
        int id = orderMapper.insertSelective(order);
        return id;
    }
    
    /**
     * 更新数据库 销售
     */
    private void saleStockOptimistic(Stock stock) {
        log.info("查询数据库，尝试更新数据库");
        int count = stockService.updateStockByOptimistic(stock);
        if (count == 0) {
            throw new RuntimeException("并发更新库存失败，version不匹配");
        }
    }
}
