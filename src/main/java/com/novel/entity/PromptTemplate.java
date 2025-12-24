package com.novel.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 提示词模板实体类
 * 存储不同栏目使用的AI提示词模板，支持变量替换
 * 
 * @author NovelCreator
 */
@Entity
@Table(name = "prompt_templates", indexes = {
    @Index(name = "idx_template_type", columnList = "templateType"),
    @Index(name = "idx_template_default", columnList = "isDefault")
})
public class PromptTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 模板名称
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 模板类型：CONTENT(正文)、TITLE(标题)、SUMMARY(章节概括)、CHARACTER(角色)、COLLECTION(汇总)
     */
    @Column(nullable = false, length = 50)
    private String templateType;

    /**
     * 系统提示词（System Prompt）
     */
    @Column(columnDefinition = "CLOB")
    private String systemPrompt;

    /**
     * 用户提示词模板（支持变量：{{title}}, {{content}}, {{summary}}, {{characters}}, {{userPrompt}}, {{prevSummary}}等）
     */
    @Column(columnDefinition = "CLOB", nullable = false)
    private String promptTemplate;

    /**
     * 是否为默认模板
     */
    @Column(nullable = false)
    private Boolean isDefault;

    /**
     * 模板描述说明
     */
    @Column(length = 500)
    private String description;

    /**
     * 创建时间
     */
    @Column(nullable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Column(nullable = false)
    private LocalDateTime updateTime;

    /**
     * 无参构造函数
     */
    public PromptTemplate() {
        this.isDefault = false;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 带参数构造函数
     */
    public PromptTemplate(String name, String templateType, String promptTemplate) {
        this();
        this.name = name;
        this.templateType = templateType;
        this.promptTemplate = promptTemplate;
    }

    // ==================== Getters and Setters ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getPromptTemplate() {
        return promptTemplate;
    }

    public void setPromptTemplate(String promptTemplate) {
        this.promptTemplate = promptTemplate;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 模板类型枚举常量
     */
    public static final String TYPE_CONTENT = "CONTENT";
    public static final String TYPE_TITLE = "TITLE";
    public static final String TYPE_SUMMARY = "SUMMARY";
    public static final String TYPE_CHARACTER = "CHARACTER";
    public static final String TYPE_COLLECTION = "COLLECTION";

    @Override
    public String toString() {
        return "PromptTemplate{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", templateType='" + templateType + '\'' +
                ", isDefault=" + isDefault +
                '}';
    }
}
