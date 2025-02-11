package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisIdWork;
import com.hmdp.utils.SimpleRedisLock;
import com.hmdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Autowired
    private RedisIdWork redisIdWork;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result seckillVoucher(Long voucherId) {
        // 查询优惠券
        SeckillVoucher seckillVoucher = seckillVoucherService.getById(voucherId);
        // 判断秒杀时间段是否符合
        if (seckillVoucher.getBeginTime().isAfter(LocalDateTime.now())) {
            return Result.fail("秒杀活动尚未开始");
        }
        if (seckillVoucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀活动已结束");
        }
        // 判断库存是否充足
        if (seckillVoucher.getStock()<1) {
            return Result.fail("库存不足");
        }
        // 限制用户仅购买一次
        Long userId = UserHolder.getUser().getId();

        /*
          未考虑集群模式下的锁 只锁了当前的jvm
        synchronized (userId.toString().intern()){
            // 获取事务代理对象
            IVoucherOrderService proxy = (IVoucherOrderService)AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId,userId);
        }
         */
        // redis分布式锁
        SimpleRedisLock lock = new SimpleRedisLock(RedisConstants.ORDER_KEY, stringRedisTemplate);
        boolean isLock = lock.tryLock(1200);
        if (!isLock) {
            return Result.fail("不允许重复下单");
        }
        try {
            // 获取事务代理对象
            IVoucherOrderService proxy = (IVoucherOrderService)AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId,userId);
        } finally {
            lock.unlock();
        }
    }

    public Result createVoucherOrder(Long voucherId,Long userId) {
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        if (count > 0) {
            return Result.fail("用户已购买过一次！");
        }
        // 下单判断
        boolean res = seckillVoucherService.update()
                .setSql("stock = stock - 1")
                .eq("voucher_id", voucherId)
                .gt("stock",0)
                .update();
        if (!res) {
            return Result.fail("库存不足！");
        }

        // 创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        long orderId = redisIdWork.nextId("order");
        voucherOrder.setVoucherId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setUpdateTime(LocalDateTime.now());
        save(voucherOrder);
        return Result.ok(orderId);
    }
}
