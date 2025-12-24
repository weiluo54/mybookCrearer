package com.novel.repository;

import com.novel.entity.AIConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * AI配置数据访问层
 * 
 * @author NovelCreator
 */
@Repository
public interface AIConfigRepository extends JpaRepository<AIConfig, Long> {

    /**
     * 获取默认配置
     * @return 默认配置
     */
    Optional<AIConfig> findByIsDefaultTrue();

    /**
     * 获取所有启用的配置
     * @return 启用的配置列表
     */
    List<AIConfig> findByIsActiveTrue();

    /**
     * 根据名称查找配置
     * @param name 配置名称
     * @return 配置
     */
    Optional<AIConfig> findByName(String name);

    /**
     * 获取所有配置，按默认配置优先排序
     * @return 配置列表
     */
    List<AIConfig> findAllByOrderByIsDefaultDescCreateTimeDesc();

    /**
     * 重置所有配置为非默认
     */
    @Modifying
    @Transactional
    @Query("UPDATE AIConfig a SET a.isDefault = false")
    void resetAllDefault();

    /**
     * 获取启用且为默认的配置
     * @return 默认启用配置
     */
    Optional<AIConfig> findByIsDefaultTrueAndIsActiveTrue();
}
