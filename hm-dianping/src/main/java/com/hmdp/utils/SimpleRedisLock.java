package com.hmdp.utils;

import cn.hutool.core.lang.UUID;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class SimpleRedisLock implements ILock {

    private final String name;

    private final StringRedisTemplate stringRedisTemplate;

    private static final String KEY_PREFIX = "lock:";
    private static final String ID_PREFIX = UUID.randomUUID().toString(true) + "-";
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;
    static{
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
        UNLOCK_SCRIPT.setResultType(Long.class);
    }

    public SimpleRedisLock(String name, StringRedisTemplate stringRedisTemplate) {
        this.name = name;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean tryLock(long timeoutSec) {
        // 获取线程标示，因为我们要区分到底是哪个线程拿到了这个锁，方便之后unLock的时候进行判断
        String threadId = ID_PREFIX+Thread.currentThread().getId();
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(KEY_PREFIX + name, threadId, timeoutSec, TimeUnit.MINUTES);
        return Boolean.TRUE.equals(success);
    }

    @Override
    public void unlock() {
        // 调用lua脚本
        stringRedisTemplate.execute(
                UNLOCK_SCRIPT,
                Collections.singletonList(KEY_PREFIX+name),
                ID_PREFIX+Thread.currentThread().getId()
        );
    }


//    @Override
//    public void unlock() {
//        // 获取线程标识
//        String threadId = ID_PREFIX+Thread.currentThread().getId();
//        // 获取锁的标识
//        String id = stringRedisTemplate.opsForValue().get(KEY_PREFIX + threadId);
//        if (threadId.equals(id)) {
//            stringRedisTemplate.delete(KEY_PREFIX + name);
//        }
//    }
}
