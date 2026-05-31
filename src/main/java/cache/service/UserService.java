package cache.service;

import cache.entity.User;
import cache.repository.UserRepository;
import io.netty.channel.SimpleUserEventChannelHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserService {

    // read from DB, read it from cache first, if not present then read from DB
    private UserRepository userRepository;
    private CacheManager cacheManager;
    private RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_KEY_PREFIX = "user:";
    private static final long REDIS_TTL_MINUTES = 30;

    public User createUser(User user){
        User saved = userRepository.save(user);
        System.out.println("User is saved");
        Cache cache = cacheManager.getCache("users");
        if(cache != null){
            cache.put(saved.getId(), saved);
            System.out.println("User has been saved in caffeine cache");
        }

        redisTemplate.opsForValue().set(
                REDIS_KEY_PREFIX + saved.getId(),
                saved,
                REDIS_TTL_MINUTES,
                TimeUnit.MINUTES
        );
        System.out.println("User is set in redis");
        return saved;
    }

    public User getUserById(Long id){
        // L1 + L2
        Cache cache = cacheManager.getCache("users");
        if(cache != null){
            User cachedUser = cache.get(id, User.class);
            if(cachedUser != null){
                return cachedUser;
            }
        }

        Object redisValue = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + id);
        // objectMapper.convert(redisValue, User.class);
        if(redisValue instanceof User redisUser){
           return redisUser;
        }

        return userRepository.findById(id).orElse(null);
    }


}
