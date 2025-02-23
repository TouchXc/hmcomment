package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.RedisConstants;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootTest
@Slf4j
class HmDianPingApplicationTests {
    @Resource
    private IShopService shopService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Test
    void loadData(){
        List<Shop> list = shopService.list();
        Map<Long, List<Shop>> map = list.stream().collect(Collectors.groupingBy(Shop::getTypeId));
        for(Map.Entry<Long,List<Shop>> entry:map.entrySet()){
            Long key = entry.getKey();
            List<Shop> value = entry.getValue();
            for (Shop shop : value) {
                stringRedisTemplate.opsForGeo()
                        .add(RedisConstants.SHOP_GEO_KEY+key,new Point(shop.getX(),shop.getY()),shop.getId().toString());
            }
        }
    }

    @Test
    void testHyperLogLog() {
        String[] values = new String[1000];
        int count = 0;
        for (int i = 0; i < 1000000; i++) {
            count = i % 1000;
            values[count] = "user_" + i;
            if (count == 999) {
                //存入redis
                stringRedisTemplate.opsForHyperLogLog().add("testHyperLogLog", values);
            }
        }
        //统计数量
        Long res = stringRedisTemplate.opsForHyperLogLog().size("testHyperLogLog");
        log.debug("数量为：{}", res);
    }


}
