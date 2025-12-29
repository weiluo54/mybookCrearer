package com.novel.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * AI配置实体类
 * 存储OpenAI兼容接口的配置信息，支持多套配置切换
 * 
 * @author NovelCreator
 */
@Entity
@Table(name = "ai_configs", indexes = {
    @Index(name = "idx_config_default", columnList = "isDefault"),
    @Index(name = "idx_config_active", columnList = "isActive")
})
public class AIConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 配置名称
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * API地址（OpenAI兼容接口地址）
     */
    @Column(nullable = false, length = 500)
    private String apiUrl;

    /**
     * API密钥
     */
    @Column(nullable = false, length = 500)
    private String apiKey;

    /**
     * 模型名称（如：gpt-3.5-turbo, gpt-4等）
     */
    @Column(nullable = false, length = 100)
    private String model;

    /**
     * 温度参数（控制随机性，0-2之间）
     */
    @Column(nullable = false)
    private Double temperature;

    /**
     * Top P参数（核采样，0-1之间）
     */
    @Column(nullable = false)
    private Double topP;

    /**
     * 最大token数
     */
    @Column(nullable = false)
    private Integer maxTokens;

    /**
     * 请求超时时间（秒）
     */
    @Column(nullable = false)
    private Integer timeout;

    /**
     * 频率惩罚（-2到2之间）
     */
    @Column
    private Double frequencyPenalty;

    /**
     * 存在惩罚（-2到2之间）
     */
    @Column
    private Double presencePenalty;

    /**
     * 是否为默认配置
     */
    @Column(nullable = false)
    private Boolean isDefault;

    /**
     * 是否启用
     */
    @Column(nullable = false)
    private Boolean isActive;

    /**
     * 配置备注
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
     * 是否只更新默认值字段
     */
    private Boolean doSetDefaultOpt;
    /**
     * 无参构造函数，设置默认值
     */
    public AIConfig() {
        this.apiUrl = null;
        this.model = null;
        this.temperature = null;
        this.topP = null;
        this.maxTokens = null;
        this.timeout = 12000;
        this.frequencyPenalty =null;
        this.presencePenalty = null;
        this.isDefault = null;
        this.isActive = null;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 带参数构造函数
     */
    public AIConfig(String name, String apiUrl, String apiKey) {
        this();
        this.name = name;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
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

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getTopP() {
        return topP;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public void setTimeout(Integer timeout) {
        this.timeout = timeout;
    }

    public Double getFrequencyPenalty() {
        return frequencyPenalty;
    }

    public void setFrequencyPenalty(Double frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
    }

    public Double getPresencePenalty() {
        return presencePenalty;
    }

    public void setPresencePenalty(Double presencePenalty) {
        this.presencePenalty = presencePenalty;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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

    @Override
    public String toString() {
        return "AIConfig{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", model='" + model + '\'' +
                ", isDefault=" + isDefault +
                ", isActive=" + isActive +
                '}';
    }
}
