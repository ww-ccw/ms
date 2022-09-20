package com.chw.util;

/**
 * @Author CHW
 * @Date 2022/9/15
 **/
public enum CacheKey {
    HASH_KEY("miaosha_hash"),
    LIMIT_KEY("miaosha_limit"),
    STOCK_COUNT("miaosha_stock_count");
    
    private String key;
    
    CacheKey(String key) {
        this.key = key;
    }
    
    public String getKey() {
        return key;
    }
}
