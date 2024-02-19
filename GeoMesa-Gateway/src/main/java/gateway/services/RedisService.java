package gateway.services;

import redis.clients.jedis.*;

public class RedisService {

    private static RedisService instance;
    private Jedis jedis;

    private RedisService(){
        this.jedis = new Jedis("redis://localhost:6379");
    }

    public static synchronized RedisService getIstance(){
        if(instance == null){
            instance = new RedisService();
        }
        return instance;
    }

    public void insert(String key, String value){
        jedis.set(key, value);
    }

    public String retrieve(String key){
        return jedis.get(key);
    }

    public void flushDB() {
        jedis.flushDB();
    }
}