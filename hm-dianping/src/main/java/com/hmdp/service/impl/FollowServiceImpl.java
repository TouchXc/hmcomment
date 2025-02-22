package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Follow;
import com.hmdp.mapper.FollowMapper;
import com.hmdp.service.IFollowService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class FollowServiceImpl extends ServiceImpl<FollowMapper, Follow> implements IFollowService {
    @Resource
    private FollowMapper followMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private IUserService userService;
    @Override
    public Result follow(Long followUserId, Boolean isFollow) {
        // 1. 获取当前登录用户
        long userId = UserHolder.getUser().getId();
        String key = RedisConstants.FOLLOW_USER_KEY + userId;
        // 2. 判断是否关注
        if (isFollow) {
            // 未关注，进行关注操作
            Follow follow = new Follow().setUserId(userId).setFollowUserId(followUserId).setCreateTime(LocalDateTime.now());
            boolean isSuccess = save(follow);
            if (isSuccess) {
                // 成功的话就放在redis里面
                stringRedisTemplate.opsForSet().add(key, followUserId.toString());
            } else {
                return Result.fail("关注失败，请稍后再试！");
            }
        } else {
            // 关注，进行取关操作  :数据库--redis
            remove(new QueryWrapper<Follow>().eq("user_id", userId).eq("follow_user_id", followUserId));
            stringRedisTemplate.opsForSet().remove(key, followUserId.toString());
        }
        return Result.ok();
    }

    @Override
    public Result isFollow(Integer followUserId) {
        // 1. 获取当前登录用户
        long userId = UserHolder.getUser().getId();
        // 2. 从数据库中查是否有关注数据   --> 从redis中查
        // Integer count = query().eq("user_id", userId).eq("follow_user_id", followUserId).count();
        Boolean member = stringRedisTemplate.opsForSet().isMember(RedisConstants.FOLLOW_USER_KEY + userId, followUserId.toString());
        return Result.ok(member);
    }

    @Override
    public Result followCommon(Long otherUserId) {
        // 1. 获取当前登录用户
        long userId = UserHolder.getUser().getId();
        String userKey = RedisConstants.FOLLOW_USER_KEY + userId;
        String otherKey = RedisConstants.FOLLOW_USER_KEY + otherUserId;

        //求交集
        Set<String> intersect = stringRedisTemplate.opsForSet().intersect(userKey, otherKey);
        // 没有的话直接返回空列表
        if (intersect == null || intersect.isEmpty()) {
            return Result.ok(Collections.emptyList());
        }

        // 解析id集合 用stream流
        List<Long> ids = intersect.stream().map(Long::valueOf).collect(Collectors.toList());

        // 查询用户
        List<UserDTO> users = userService.listByIds(ids).stream().map(user -> BeanUtil.copyProperties(user, UserDTO.class)).collect(Collectors.toList());
        return Result.ok(users);
    }
}
