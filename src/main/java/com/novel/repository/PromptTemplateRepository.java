package com.novel.repository;

import com.novel.entity.PromptTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 提示词模板数据访问层
 * 
 * @author NovelCreator
 */
@Repository
public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, Long> {

    /**
     * 根据模板类型获取所有模板
     * @param templateType 模板类型
     * @return 模板列表
     */
    List<PromptTemplate> findByTemplateType(String templateType);

    /**
     * 获取指定类型的默认模板
     * @param templateType 模板类型
     * @return 默认模板
     */
    Optional<PromptTemplate> findByTemplateTypeAndIsDefaultTrue(String templateType);

    /**
     * 根据名称查找模板
     * @param name 模板名称
     * @return 模板
     */
    Optional<PromptTemplate> findByName(String name);

    /**
     * 获取所有模板，按类型和创建时间排序
     * @return 模板列表
     */
    List<PromptTemplate> findAllByOrderByTemplateTypeAscCreateTimeDesc();

    /**
     * 重置指定类型的所有模板为非默认
     * @param templateType 模板类型
     */
    @Modifying
    @Transactional
    @Query("UPDATE PromptTemplate p SET p.isDefault = false WHERE p.templateType = :templateType")
    void resetDefaultByType(String templateType);

    /**
     * 获取所有默认模板
     * @return 默认模板列表
     */
    List<PromptTemplate> findByIsDefaultTrue();
}
