package com.novel.service;

import com.novel.entity.PromptTemplate;
import com.novel.repository.PromptTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 提示词模板服务层
 * 处理提示词模板的CRUD操作和业务逻辑
 * 
 * @author NovelCreator
 */
@Service
public class PromptTemplateService {

    private final PromptTemplateRepository promptTemplateRepository;

    /**
     * 构造函数注入
     * @param promptTemplateRepository 提示词模板数据访问层
     */
    public PromptTemplateService(PromptTemplateRepository promptTemplateRepository) {
        this.promptTemplateRepository = promptTemplateRepository;
    }

    /**
     * 获取所有模板
     * @return 模板列表
     */
    public List<PromptTemplate> getAllTemplates() {
        return promptTemplateRepository.findAllByOrderByTemplateTypeAscCreateTimeDesc();
    }

    /**
     * 根据ID获取模板
     * @param id 模板ID
     * @return 模板
     */
    public Optional<PromptTemplate> getTemplateById(Long id) {
        return promptTemplateRepository.findById(id);
    }

    /**
     * 根据类型获取模板列表
     * @param templateType 模板类型
     * @return 模板列表
     */
    public List<PromptTemplate> getTemplatesByType(String templateType) {
        return promptTemplateRepository.findByTemplateType(templateType);
    }

    /**
     * 获取指定类型的默认模板
     * @param templateType 模板类型
     * @return 默认模板
     */
    public PromptTemplate getDefaultTemplate(String templateType) {
        Optional<PromptTemplate> templateOpt = promptTemplateRepository.findByTemplateTypeAndIsDefaultTrue(templateType);
        if (templateOpt.isPresent()) {
            return templateOpt.get();
        }
        // 如果没有默认模板，返回该类型的第一个模板，或创建一个默认模板
        List<PromptTemplate> templates = promptTemplateRepository.findByTemplateType(templateType);
        if (!templates.isEmpty()) {
            return templates.get(0);
        }
        // 创建默认模板
        return createDefaultTemplate(templateType);
    }

    /**
     * 创建模板
     * @param template 模板信息
     * @return 保存后的模板
     */
    @Transactional
    public PromptTemplate createTemplate(PromptTemplate template) {
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        
        // 如果设置为默认，重置其他同类型模板的默认状态
        if (template.getIsDefault() != null && template.getIsDefault()) {
            promptTemplateRepository.resetDefaultByType(template.getTemplateType());
        }
        
        return promptTemplateRepository.save(template);
    }

    /**
     * 更新模板
     * @param id 模板ID
     * @param template 模板信息
     * @return 更新后的模板
     */
    @Transactional
    public PromptTemplate updateTemplate(Long id, PromptTemplate template) {
        Optional<PromptTemplate> existingOpt = promptTemplateRepository.findById(id);
        if (existingOpt.isPresent()) {
            PromptTemplate existing = existingOpt.get();
            
            if (template.getName() != null) {
                existing.setName(template.getName());
            }
            if (template.getTemplateType() != null) {
                existing.setTemplateType(template.getTemplateType());
            }
            if (template.getSystemPrompt() != null) {
                existing.setSystemPrompt(template.getSystemPrompt());
            }
            if (template.getPromptTemplate() != null) {
                existing.setPromptTemplate(template.getPromptTemplate());
            }
            if (template.getDescription() != null) {
                existing.setDescription(template.getDescription());
            }
            
            // 处理默认状态更新
            if (template.getIsDefault() != null && template.getIsDefault()) {
                promptTemplateRepository.resetDefaultByType(existing.getTemplateType());
                existing.setIsDefault(true);
            } else if (template.getIsDefault() != null) {
                existing.setIsDefault(template.getIsDefault());
            }
            
            existing.setUpdateTime(LocalDateTime.now());
            return promptTemplateRepository.save(existing);
        }
        throw new RuntimeException("模板不存在，ID: " + id);
    }

    /**
     * 删除模板
     * @param id 模板ID
     */
    @Transactional
    public void deleteTemplate(Long id) {
        if (promptTemplateRepository.existsById(id)) {
            promptTemplateRepository.deleteById(id);
        } else {
            throw new RuntimeException("模板不存在，ID: " + id);
        }
    }

    /**
     * 设置默认模板
     * @param id 模板ID
     * @return 设置后的模板
     */
    @Transactional
    public PromptTemplate setAsDefault(Long id) {
        Optional<PromptTemplate> templateOpt = promptTemplateRepository.findById(id);
        if (templateOpt.isPresent()) {
            PromptTemplate template = templateOpt.get();
            promptTemplateRepository.resetDefaultByType(template.getTemplateType());
            template.setIsDefault(true);
            template.setUpdateTime(LocalDateTime.now());
            return promptTemplateRepository.save(template);
        }
        throw new RuntimeException("模板不存在，ID: " + id);
    }

    /**
     * 创建默认模板
     * @param templateType 模板类型
     * @return 创建的默认模板
     */
    @Transactional
    public PromptTemplate createDefaultTemplate(String templateType) {
        PromptTemplate template = new PromptTemplate();
        template.setTemplateType(templateType);
        template.setIsDefault(true);
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        
        switch (templateType) {
            case PromptTemplate.TYPE_CONTENT:
                template.setName("默认正文生成模板");
                template.setSystemPrompt(getDefaultContentSystemPrompt());
                template.setPromptTemplate(getDefaultContentPromptTemplate());
                template.setDescription("用于生成小说正文内容的默认模板");
                break;
            case PromptTemplate.TYPE_SUMMARY:
                template.setName("默认章节概括模板");
                template.setSystemPrompt("你是一位专业的小说编辑，擅长总结和概括文章内容。");
                template.setPromptTemplate("请对以下章节内容进行概括总结，提取关键情节、人物动态和重要事件：\n\n【章节标题】{{title}}\n\n【章节内容】\n{{content}}\n\n请用200-500字概括这一章节的主要内容。");
                template.setDescription("用于生成章节概括的默认模板");
                break;
            case PromptTemplate.TYPE_COLLECTION:
                template.setName("默认汇总模板");
                template.setSystemPrompt("你是一位专业的小说编辑，擅长整理和汇总多章节内容。");
                template.setPromptTemplate("请对以下多个章节的概括进行汇总，生成一份整体的剧情梗概：\n\n{{summaries}}\n\n请生成一份500-1000字的剧情汇总。");
                template.setDescription("用于汇总多章节概括的默认模板");
                break;
            case PromptTemplate.TYPE_CHARACTER:
                template.setName("默认角色生成模板");
                template.setSystemPrompt("你是一位专业的小说创作助手，擅长创造丰富立体的角色形象。");
                template.setPromptTemplate("请根据以下要求创建一个小说角色：\n\n{{userPrompt}}\n\n请以JSON格式返回角色信息，包含name(姓名)、appearance(外貌)、personality(性格)、background(背景)、role(定位)字段。");
                template.setDescription("用于生成角色信息的默认模板");
                break;
            default:
                template.setName("默认模板");
                template.setSystemPrompt("你是一位专业的AI助手。");
                template.setPromptTemplate("{{userPrompt}}");
                template.setDescription("默认通用模板");
        }
        
        return promptTemplateRepository.save(template);
    }

    /**
     * 获取默认正文生成的系统提示词
     */
    private String getDefaultContentSystemPrompt() {
        return "你是一位才华横溢的网络小说作家，擅长创作引人入胜的故事情节。\n" +
               "你的输出必须是严格的JSON格式，包含以下字段：\n" +
               "- content: 正文内容（必填，3000字以上）\n" +
               "- title: 章节标题（必填）\n" +
               "- role: 角色信息数组（选填，包含name、appearance、personality、background、role字段）\n" +
               "- chapter: 章节概括（必填，200-500字）\n\n" +
               "注意：只返回JSON，不要有任何其他内容。";
    }

    /**
     * 获取默认正文生成的用户提示词模板
     */
    private String getDefaultContentPromptTemplate() {
        return "请根据以下信息创作小说章节：\n\n" +
               "【前情概要】\n{{prevSummary}}\n\n" +
               "【角色信息】\n{{characters}}\n\n" +
               "【创作要求】\n{{userPrompt}}\n\n" +
               "请创作一章3000字以上的精彩内容，注意：\n" +
               "1. 保持情节连贯，承接前文\n" +
               "2. 对话生动自然\n" +
               "3. 注重场景描写和心理刻画\n" +
               "4. 如有新角色出现，请在role字段中提供详细信息";
    }

    /**
     * 获取所有默认模板
     * @return 默认模板列表
     */
    public List<PromptTemplate> getAllDefaultTemplates() {
        return promptTemplateRepository.findByIsDefaultTrue();
    }
}
