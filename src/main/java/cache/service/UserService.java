package cache.service;

import cache.Repository.UserRepository;
import cache.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CacheManager caffeineCacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    private static final String REDIS_KEY_PREFIX = "user:";
    private static final long REDIS_TTL_MINUTES = 30;

    /**
     * Save user to MySQL, then populate both caches.
     */
    public User createUser(User user) {
        User saved = userRepository.save(user);
        log.info("SAVED to MySQL: id={}", saved.getId());

        // Populate L1 (Caffeine)
        Cache caffeine = caffeineCacheManager.getCache("users");
        if (caffeine != null) {
            caffeine.put(saved.getId(), saved);
            log.info("CACHED in L1 Caffeine: id={}", saved.getId());
        }

        // Populate L2 (Redis)
        redisTemplate.opsForValue().set(
                REDIS_KEY_PREFIX + saved.getId(),
                saved,
                REDIS_TTL_MINUTES,
                TimeUnit.MINUTES
        );
        log.info("CACHED in L2 Redis: id={}", saved.getId());

        return saved;
    }

    /**
     * 3-layer read-through cache:
     *   1. L1: Caffeine (local memory) — fastest
     *   2. L2: Redis (distributed)      — network call
     *   3. L3: MySQL (database)         — slowest
     *
     * On miss, backfills all upper layers so the next read is faster.
     */
    public User getUserById(Long id) {
        // ── L1: Caffeine ──────────────────────────────────────────
        Cache caffeine = caffeineCacheManager.getCache("users");
        if (caffeine != null) {
            User cached = caffeine.get(id, User.class);
            if (cached != null) {
                log.info("✅ L1 HIT (Caffeine): id={}", id);
                return cached;
            }
        }
        log.info("❌ L1 MISS (Caffeine): id={}", id);

        // ── L2: Redis ────────────────────────────────────────────
        Object redisValue = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + id);
        if (redisValue instanceof User redisUser) {
            log.info("✅ L2 HIT (Redis): id={} — backfilling L1", id);
            if (caffeine != null) {
                caffeine.put(id, redisUser);
            }
            return redisUser;
        }
        log.info("❌ L2 MISS (Redis): id={}", id);

        // ── L3: MySQL ────────────────────────────────────────────
        User dbUser = userRepository.findById(id).orElse(null);
        if (dbUser != null) {
            log.info("✅ L3 HIT (MySQL): id={} — backfilling L1 + L2", id);

            // Backfill both caches
            if (caffeine != null) {
                caffeine.put(id, dbUser);
            }
            redisTemplate.opsForValue().set(
                    REDIS_KEY_PREFIX + id,
                    dbUser,
                    REDIS_TTL_MINUTES,
                    TimeUnit.MINUTES
            );

            return dbUser;
        }

        log.info("❌ L3 MISS (MySQL): id={} — user not found", id);
        return null;
    }
}
