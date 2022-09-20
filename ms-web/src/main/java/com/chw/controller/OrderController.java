package com.chw.controller;

import com.chw.service.OrderService;
import com.chw.service.StockService;
import com.chw.service.UserService;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.concurrent.*;

/**
 * @Author CHW
 * @Date 2022/9/13
 **/
@Controller
@ResponseBody
@Log4j2
public class OrderController {
    
    @Resource
    OrderService orderService;
    
    @Resource
    StockService stockService;
    
    @Resource
    UserService userService;
    
    /**
     * rabbitMQ模板
     */
    @Resource
    private AmqpTemplate rabbitTemplate;
    
    /**
     * 令牌桶，每秒放10个
     */
    RateLimiter rateLimiter = RateLimiter.create(10);
    
    /**
     * 延时双删线程池
     */
    private static ExecutorService cacheThreadPool = new ThreadPoolExecutor(10, 100, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), Executors.defaultThreadFactory(), new ThreadPoolExecutor.DiscardOldestPolicy());
    /**
     * 延时时间
     */
    private static final int DELAY_MILLISECONDS = 1000;
    
    /**
     * 执行异步任务删除
     */
    private class delCacheByThread implements Runnable {
        
        private int sid;
        
        public delCacheByThread(int sid) {
            this.sid = sid;
        }
        
        @Override
        public void run() {
            try {
                log.info("异步执行缓存再删除，商品[{}]，首先休眠[{}]秒", sid, DELAY_MILLISECONDS);
                Thread.sleep(DELAY_MILLISECONDS);
                stockService.delStockCountCache(sid);
                log.info("异步删除商品[{}]缓存成功", sid);
            } catch (InterruptedException e) {
                log.error("异步删除失败[{}]", e.getMessage());
            }
        }
    }
    
    
    /**
     * v1
     * 不做限制的卖东西，容易出现超卖的现象
     *
     * @param sid
     * @return
     */
    @RequestMapping("/createWrongOrder/{sid}")
    public String createWrongOrder(@PathVariable int sid) {
        log.info("购买物品编号sid=[{}]", sid);
        int id = 0;
        try {
            id = orderService.createWrongOrder(sid);
            log.info("创建订单id: [{}]", id);
        } catch (Exception e) {
            log.error("Exception", e);
        }
        return String.valueOf(id);
    }
    
    /**
     * v2
     * 使用乐观锁的方式，比较版本，防止超卖
     * v3
     * 使用令牌桶限流 Guava插件实现
     *
     * @param sid
     * @return
     */
    @RequestMapping("/createOptimisticOrder/{sid}")
    public String createOptimisticOrder(@PathVariable int sid) {
        // 阻塞式获取令牌
        //log.info("等待时间" + rateLimiter.acquire());
        //非阻塞式获取令牌
        if (!rateLimiter.tryAcquire(1000, TimeUnit.MILLISECONDS)) {
            log.error("你被限流了，待会再试");
            return new String("你被限流了，待会再试");
        }
        int id;
        try {
            id = orderService.createOptimisticOrder(sid);
            log.info("购买成功，剩余库存为: [{}]", id);
        } catch (Exception e) {
            log.error("购买失败：[{}]", e.getMessage());
            return new String("购买失败，库存不足");
        }
        return String.format("购买成功，剩余库存为：%d", id);
    }
    
    /**
     * v5
     * 再读取频繁的地方使用乐观锁固然更好，但是如果频繁的修改，那么乐观锁的上层应用不断的cas则影响性能，且卖出失败率高，明明有货却失败
     * 需要注意的是，如果请求量巨大，悲观锁会导致后续的请求阻塞等待时间太久，需要使用令牌桶限流
     * 使用悲观锁实现,成功率提升
     *
     * @param sid
     * @return
     */
    @RequestMapping("/createPessimisticOrder/{sid}")
    public String createPessimisticOrder(@PathVariable int sid) {
        int id;
        try {
            id = orderService.createPessimisticOrder(sid);
            log.info("购买成功,剩余库存为:[{}]", sid);
        } catch (Exception e) {
            log.error("购买失败:[{}]", e.getMessage());
            return String.format("购买失败，库存不足");
        }
        return String.format("购买成功，剩余库存为：%d", id);
    }
    
    
    /**
     * 获取验证码
     *
     * @param sid
     * @param userId
     * @return
     */
    @RequestMapping(value = "/getVerifyHash", method = {RequestMethod.GET})
    @ResponseBody
    public String getVerifyHash(@RequestParam(value = "sid") Integer sid, @RequestParam(value = "userId") Integer userId) {
        String hash;
        try {
            hash = userService.getVerifyHash(sid, userId);
        } catch (Exception e) {
            log.error("获取验证hash失败，原因是[{}]", e.getMessage());
            return "获取验证码失败";
        }
        return String.format("请求抢购验证hash为:%s", hash);
    }
    
    /**
     * 需要验证的抢购接口+
     *
     * @param sid
     * @param userId
     * @param verifyHash
     * @return
     */
    @RequestMapping(value = "/createOrderWithVerifiedUrl", method = {RequestMethod.GET})
    public String createOrderWithVerifiedUrl(@RequestParam(value = "sid") Integer sid,
                                             @RequestParam(value = "userId") Integer userId,
                                             @RequestParam(value = "verifyHash") String verifyHash) {
        int stockLeft;
        try {
            stockLeft = orderService.createVerifiedOrder(sid, userId, verifyHash);
            log.info("购买成功，剩余库存为：[{}]", stockLeft);
        } catch (Exception e) {
            log.error("购买失败：[{}]", e.getMessage());
            return e.getMessage();
        }
        
        return String.format("购买成功，剩余库存为：%d", stockLeft);
    }
    
    /**
     * 使用验证码 + 访问次数
     *
     * @param sid
     * @param userId
     * @param verifyHash
     * @return
     */
    @RequestMapping(value = "/createOrderWithVerifiedUrlAndLimit", method = {RequestMethod.GET})
    public String createOrderWithVerifiedUrlAndLimit(@RequestParam(value = "sid") Integer sid,
                                                     @RequestParam(value = "userId") Integer userId,
                                                     @RequestParam(value = "verifyHash") String verifyHash) {
        int stockLeft;
        try {
            int count = userService.addUserCount(userId);
            log.info("[{}]用户截至到目前位置已经访问了[{}]次", userId, count);
            boolean isBanned = userService.getUserIsBanned(userId);
            if (isBanned) {
                log.error("购买失败,超过访频率限制");
                return String.format("购买失败,超过访频率限制");
            }
            stockLeft = orderService.createVerifiedOrder(sid, userId, verifyHash);
            log.info("购买成功,剩余库存：[{}]", stockLeft);
        } catch (Exception e) {
            log.error("购买失败：[{}]", e.getMessage());
            return e.getMessage();
        }
        return String.format("购买成功，剩余库存 %d", stockLeft);
    }
    
    
    /**
     * 查询库存：通过数据库查询
     */
    @RequestMapping("/getStockByDB/{sid}")
    public String getStockByDB(@PathVariable("sid") int sid) {
        int count;
        try {
            count = stockService.getStockCountByDB(sid);
        } catch (Exception e) {
            log.error("查询库存失败：[{}]", e.getMessage());
            return String.format("查询库存失败");
        }
        log.info("商品[{}]剩余库存：[{}]", sid, count);
        return String.format("商品%d剩余库存：%d", sid, count);
    }
    
    /**
     * 查询库存：先通过缓存查询，如果没有查到则通过数据库查询，并且写入缓存
     */
    @RequestMapping("/getStockByCache/{sid}")
    public String getStockByCache(@PathVariable("sid") int sid) {
        int count;
        try {
            count = stockService.getStockCount(sid);
        } catch (Exception e) {
            log.error("查询库存失败：[{}]", e.getMessage());
            return String.format("查询库存失败");
        }
        log.info("商品[{}]剩余库存：[{}]", sid, count);
        return String.format("商品%d剩余库存：%d", sid, count);
    }


//    -----------------------------数据库及缓存同步--------------------------------------------------------
    
    
    /**
     * 先删除缓存 再更新数据库
     */
    @RequestMapping("/createOrderWithCacheV1/{sid}")
    public String createOrderWithCacheV1(@PathVariable("sid") int sid) {
        int count = 0;
        try {
            //删除缓存
            stockService.delStockCountCache(sid);
            //下单并更新数据库
            count = orderService.createPessimisticOrder(sid);
        } catch (Exception e) {
            log.error("购买失败[{}]", e.getMessage());
            return String.format("购买失败，库存不足");
        }
        log.info("购买成功，剩余库存为：[{}]", count);
        return String.format("购买成功，剩余库存：%d", count);
    }
    
    /**
     * 先更新数据库 再删除缓存
     */
    @RequestMapping("/createOrderWithCacheV2/{sid}")
    public String createOrderWithCacheV2(@PathVariable("sid") int sid) {
        int count = 0;
        try {
            //更新数据库
            orderService.createPessimisticOrder(sid);
            //删除缓存
            stockService.delStockCountCache(sid);
        } catch (Exception e) {
            log.error("购买失败[{}]", e.getMessage());
            return String.format("购买失败，库存不足");
        }
        log.info("购买成功，剩余库存为：[{}]", count);
        return String.format("购买成功，剩余库存为：%d", count);
    }
    
    /**
     * 缓存延时双删
     * 先删除缓存,再更新数据库,一段时间后再开启其他线程执行删除缓存任务
     */
    @RequestMapping("/createOrderWithCacheV3/{sid}")
    public String createOrderWithCacheV3(@PathVariable("sid") int sid) {
        int count = 0;
        try {
            //删除缓存
            stockService.delStockCountCache(sid);
            //更新数据库
            count = orderService.createPessimisticOrder(sid);
            //开启新线程在一段时间后删除缓存
            cacheThreadPool.execute(new delCacheByThread(sid));
        } catch (Exception e) {
            log.error("出错了:[{}]", e.getMessage());
        }
        log.info("购买成功，剩余库存为%d", count);
        return String.format("购买成功，剩余库存为 %d", count);
    }
    
    /**
     * 下单接口: 先更新数据库，再删缓存，删除缓存重试机制
     * 使用rabbitMQ
     */
    @RequestMapping("/createOrderWithCacheV4/{sid}")
    public String createOrderWithCacheV4(@PathVariable("sid") int sid) {
        int count;
        try {
            // 更新数据库
            count = orderService.createPessimisticOrder(sid);
            log.info("更新数据库成功");
            // 删除缓存
            stockService.delStockCountCache(sid);
            // 延时指定时间再次删除
            cacheThreadPool.execute(new delCacheByThread(sid));
            // 假设没有成功则通知消息队列进行删除缓存
            sendToDelCache(sid);
        } catch (Exception e) {
            log.error("购买失败：[{}]", e.getMessage());
            return String.format("购买失败，库存不足");
        }
        log.info("购买成功，剩余库存为: [{}]", count);
        return String.format("购买成功");
    }
    
    
    /**
     * 向消息队列delCache发送消息
     */
    private void sendToDelCache(Integer sid) {
        log.info("通知消息队列删除缓存[{}]", sid);
        this.rabbitTemplate.convertAndSend("delCache", sid);
    }
    
    /**
     * 向消息队列orderQueue发送消息
     */
    /*private void sendToOrderQueue(Integer sid) {
        log.info("通知消息队列下单:[{}]" , sid);
        this.rabbitTemplate.convertAndSend("orderQueue" , sid);
    }*/
    
}







