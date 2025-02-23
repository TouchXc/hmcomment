package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {
    /**
     * 查询热点博客
     * @param current
     * @return
     */
    Result queryHotBlog(Integer current);

    /**
     * 根据id查询博客
     * @param id
     * @return
     */
    Result queryBlogById(Long id);

    /**
     * 点赞博客
     * @param id
     * @return
     */
    Result likeBlog(Long id);

    /**
     * 查询点赞排名列表
     * @param id
     * @return
     */
    Result queryBlogLikes(Long id);

    /**
     * 保存成博客
     * @param blog
     * @return
     */
    Result saveBlog(Blog blog);

    /**
     * 查询关注人发布的笔记
     * @param max
     * @param offset
     * @return
     */
    Result queryBlogOfFollow(Long max, Integer offset);

    /**
     * 用户签到
     * @return
     */
    Result sign();

    /**
     * 统计签到情况
     * @return
     */
    Result signCount();
}
