package gateway.controller;

import gateway.services.RedisService;

public class RedisController {

    static RedisService redis = RedisService.getIstance();

    public static void flushDB(){
        redis.flushDB();
    }
}
