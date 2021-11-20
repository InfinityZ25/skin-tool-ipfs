package us.jcedeno.skin.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import lombok.Getter;

/**
 * A controller for the redis side of the application.
 * 
 * @author jcedeno
 */
public class RedisController {
    private final RedisClient redisClient;
    private final @Getter StatefulRedisConnection<String, String> redisConnection;

    public RedisController(String redisUri) {
        this.redisClient = RedisClient.create(redisUri);
        this.redisConnection = redisClient.connect();
    }

}
