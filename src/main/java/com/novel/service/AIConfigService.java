package com.novel.service;

import com.novel.entity.AIConfig;
import com.novel.repository.AIConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * AI配置服务层
 * 处理AI配置的CRUD操作和业务逻辑
 * 
 * @author NovelCreator
 */
@Service
public class AIConfigService {

    private final AIConfigRepository aiConfigRepository;

    /**
     * 构造函数注入
     * @param aiConfigRepository AI配置数据访问层
     */
    public AIConfigService(AIConfigRepository aiConfigRepository) {
        this.aiConfigRepository = aiConfigRepository;
    }

    /**
     * 获取所有配置
     * @return 配置列表
     */
    public List<AIConfig> getAllConfigs() {
        return aiConfigRepository.findAllByOrderByIsDefaultDescCreateTimeDesc();
    }

    /**
     * 根据ID获取配置
     * @param id 配置ID
     * @return 配置
     */
    public Optional<AIConfig> getConfigById(Long id) {
        return aiConfigRepository.findById(id);
    }

    /**
     * 获取默认配置
     * @return 默认配置
     */
    public AIConfig getDefaultConfig() {
        Optional<AIConfig> configOpt = aiConfigRepository.findByIsDefaultTrueAndIsActiveTrue();
        if (configOpt.isPresent()) {
            return configOpt.get();
        }
        // 如果没有默认配置，返回第一个启用的配置，或创建一个默认配置
        List<AIConfig> configs = aiConfigRepository.findByIsActiveTrue();
        if (!configs.isEmpty()) {
            return configs.get(0);
        }
        // 创建默认配置
        return createDefaultConfig();
    }

    /**
     * 创建配置
     * @param config 配置信息
     * @return 保存后的配置
     */
    @Transactional
    public AIConfig createConfig(AIConfig config) {
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        
        // 如果设置为默认，重置其他配置的默认状态
        if (config.getIsDefault() != null && config.getIsDefault()) {
            aiConfigRepository.resetAllDefault();
        }
        
        return aiConfigRepository.save(config);
    }

    /**
     * 更新配置
     * @param id 配置ID
     * @param config 配置信息
     * @return 更新后的配置
     */
    @Transactional
    public AIConfig updateConfig(Long id, AIConfig config) {
        Optional<AIConfig> existingOpt = aiConfigRepository.findById(id);
        if (existingOpt.isPresent()) {
            AIConfig existing = existingOpt.get();
            
            if (config.getName() != null) {
                existing.setName(config.getName());
            }
            if (config.getApiUrl() != null) {
                existing.setApiUrl(config.getApiUrl());
            }
            if (config.getApiKey() != null) {
                existing.setApiKey(config.getApiKey());
            }
            if (config.getModel() != null) {
                existing.setModel(config.getModel());
            }
            if (config.getTemperature() != null) {
                existing.setTemperature(config.getTemperature());
            }
            if (config.getTopP() != null) {
                existing.setTopP(config.getTopP());
            }
            if (config.getMaxTokens() != null) {
                existing.setMaxTokens(config.getMaxTokens());
            }
            if (config.getTimeout() != null) {
                existing.setTimeout(config.getTimeout());
            }
            if (config.getFrequencyPenalty() != null) {
                existing.setFrequencyPenalty(config.getFrequencyPenalty());
            }
            if (config.getPresencePenalty() != null) {
                existing.setPresencePenalty(config.getPresencePenalty());
            }
            if (config.getIsActive() != null) {
                existing.setIsActive(config.getIsActive());
            }
            if (config.getDescription() != null) {
                existing.setDescription(config.getDescription());
            }
            
            // 处理默认状态更新
            if (config.getIsDefault() != null && config.getIsDefault()) {
                aiConfigRepository.resetAllDefault();
                existing.setIsDefault(true);
            } else if (config.getIsDefault() != null) {
                existing.setIsDefault(config.getIsDefault());
            }
            
            existing.setUpdateTime(LocalDateTime.now());
            return aiConfigRepository.save(existing);
        }
        throw new RuntimeException("配置不存在，ID: " + id);
    }

    /**
     * 删除配置
     * @param id 配置ID
     */
    @Transactional
    public void deleteConfig(Long id) {
        if (aiConfigRepository.existsById(id)) {
            aiConfigRepository.deleteById(id);
        } else {
            throw new RuntimeException("配置不存在，ID: " + id);
        }
    }

    /**
     * 设置默认配置
     * @param id 配置ID
     * @return 设置后的配置
     */
    @Transactional
    public AIConfig setAsDefault(Long id) {
        Optional<AIConfig> configOpt = aiConfigRepository.findById(id);
        if (configOpt.isPresent()) {
            AIConfig config = configOpt.get();
            aiConfigRepository.resetAllDefault();
            config.setIsDefault(true);
            config.setUpdateTime(LocalDateTime.now());
            return aiConfigRepository.save(config);
        }
        throw new RuntimeException("配置不存在，ID: " + id);
    }

    /**
     * 创建默认配置
     * @return 创建的默认配置
     */
    @Transactional
    public AIConfig createDefaultConfig() {
        AIConfig config = new AIConfig();
        config.setName("默认配置");
        config.setApiUrl("https://api.openai.com/v1/chat/completions");
        config.setApiKey("your-api-key-here");
        config.setModel("gpt-3.5-turbo");
        config.setTemperature(0.7);
        config.setTopP(0.9);
        config.setMaxTokens(4096);
        config.setTimeout(1200);
        config.setFrequencyPenalty(0.0);
        config.setPresencePenalty(0.0);
        config.setIsDefault(true);
        config.setIsActive(true);
        config.setDescription("系统默认AI配置");
        config.setCreateTime(LocalDateTime.now());
        config.setUpdateTime(LocalDateTime.now());
        return aiConfigRepository.save(config);
    }

    /**
     * 获取所有启用的配置
     * @return 启用的配置列表
     */
    public List<AIConfig> getActiveConfigs() {
        return aiConfigRepository.findByIsActiveTrue();
    }

    /**
     * 启用/禁用配置
     * @param id 配置ID
     * @param active 是否启用
     * @return 更新后的配置
     */
    @Transactional
    public AIConfig toggleActive(Long id, boolean active) {
        Optional<AIConfig> configOpt = aiConfigRepository.findById(id);
        if (configOpt.isPresent()) {
            AIConfig config = configOpt.get();
            config.setIsActive(active);
            config.setUpdateTime(LocalDateTime.now());
            return aiConfigRepository.save(config);
        }
        throw new RuntimeException("配置不存在，ID: " + id);
    }
}
