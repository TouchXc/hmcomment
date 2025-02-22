package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.IShopService;
import com.hmdp.utils.RedisConstants;
import org.assertj.core.error.ShouldBeInThePast;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SpringBootTest
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
}
