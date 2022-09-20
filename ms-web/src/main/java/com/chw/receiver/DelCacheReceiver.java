package com.chw.receiver;

import com.chw.service.StockService;
import lombok.extern.log4j.Log4j2;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @Author CHW
 * @Date 2022/9/17
 * Component实现bean的注入
 **/
@Log4j2
@Component
@RabbitListener(queues = "delCache")
public class DelCacheReceiver {
    
    @Resource
    private StockService stockService;
    
    @RabbitHandler
    public void process(Integer message) {
        log.info("DelCacheReceiver开始删除缓存[{}]", message);
        stockService.delStockCountCache(message);
        log.info("DelCacheReceiver删除成功[{}]",message);
    }
    
    
}
