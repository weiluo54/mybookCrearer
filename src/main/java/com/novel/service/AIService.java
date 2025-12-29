package com.novel.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.novel.dto.AIGeneratedContent;
import com.novel.dto.GenerateRequest;
import com.novel.entity.AIConfig;
import com.novel.entity.Chapter;
import com.novel.entity.Character;
import com.novel.entity.PromptTemplate;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI服务层
 * 处理与AI接口的交互，包括请求构建、内容生成、响应解析等
 * 
 * @author NovelCreator
 */
@Service
public class AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIService.class);
    private static final MediaType JSON_MEDIA_TYPE = MediaType.parse("application/json; charset=utf-8");

    private final ObjectMapper objectMapper;
    private final ChapterService chapterService;
    private final CharacterService characterService;
    private final PromptTemplateService promptTemplateService;
    private final AIConfigService aiConfigService;
    private final WorldBookService worldBookService;

    /**
     * 构造函数注入
     */
    public AIService(ObjectMapper objectMapper, 
                     ChapterService chapterService,
                     CharacterService characterService,
                     PromptTemplateService promptTemplateService,
                     AIConfigService aiConfigService,
                     WorldBookService worldBookService) {
        this.objectMapper = objectMapper;
        this.chapterService = chapterService;
        this.characterService = characterService;
        this.promptTemplateService = promptTemplateService;
        this.aiConfigService = aiConfigService;
        this.worldBookService = worldBookService;
    }

    /**
     * 生成正文内容
     * 核心方法：调用AI接口生成章节内容，解析响应并更新数据库
     * 
     * @param request 生成请求
     * @return 生成的内容对象
     */
    @Transactional
    public AIGeneratedContent generateContent(GenerateRequest request) {
        logger.info("开始生成内容，请求参数: {}", request);

        // 1. 获取AI配置
        AIConfig config = getAIConfig(request.getConfigId());
        
        // 2. 获取提示词模板
        PromptTemplate template = getPromptTemplate(request.getTemplateId(), request.getGenerateType());
        
        // 3. 构建提示词
        String systemPrompt = buildSystemPrompt(template, request);
        String userPrompt = buildUserPrompt(template, request);
        
        // 4. 调用AI接口
        String aiResponse = callAIApi(config, systemPrompt, userPrompt);
        
        // 5. 解析AI响应
        AIGeneratedContent content = parseAIResponse(aiResponse);
        
        // 6. 保存生成的内容到数据库
        saveGeneratedContent(request, content);
        
        // 7. 解析并更新角色信息（【@ 内容 @】格式）
        if (content.getContent() != null) {
            parseAndUpdateCharacterInfo(content.getContent());
        }
        
        logger.info("内容生成完成: {}", content);
        return content;
    }

    /**
     * 重新生成正文内容
     * 
     * @param chapterId 章节ID
     * @param request 生成请求
     * @return 生成的内容对象
     */
    @Transactional
    public AIGeneratedContent regenerateContent(Long chapterId, GenerateRequest request) {
        request.setChapterId(chapterId);
        request.setCreateNewChapter(false);
        return generateContent(request);
    }

    /**
     * 生成章节概括
     * 
     * @param chapterId 章节ID
     * @return 生成的概括内容
     */
    @Transactional
    public String generateSummary(Long chapterId) {
        return generateSummary(chapterId, null, null);
    }

    /**
     * 生成章节概括（支持自定义模板和配置）
     * 
     * @param chapterId 章节ID
     * @param templateId 模板ID（可选，为空则使用默认模板）
     * @param configId 配置ID（可选，为空则使用默认配置）
     * @return 生成的概括内容
     */
    @Transactional
    public String generateSummary(Long chapterId, Long templateId, Long configId) {
        logger.info("开始生成章节概括，章节ID: {}, 模板ID: {}, 配置ID: {}", chapterId, templateId, configId);
        
        Optional<Chapter> chapterOpt = chapterService.getChapterById(chapterId);
        if (!chapterOpt.isPresent()) {
            throw new RuntimeException("章节不存在，ID: " + chapterId);
        }
        
        Chapter chapter = chapterOpt.get();
        if (chapter.getContent() == null || chapter.getContent().isEmpty()) {
            throw new RuntimeException("章节内容为空，无法生成概括");
        }
        
        // 获取配置和模板
        AIConfig config = configId != null ? 
            aiConfigService.getConfigById(configId).orElse(aiConfigService.getDefaultConfig()) : 
            aiConfigService.getDefaultConfig();
        PromptTemplate template = templateId != null ?
            promptTemplateService.getTemplateById(templateId).orElse(promptTemplateService.getDefaultTemplate(PromptTemplate.TYPE_SUMMARY)) :
            promptTemplateService.getDefaultTemplate(PromptTemplate.TYPE_SUMMARY);
        
        // 构建提示词（支持多种变量）
        String systemPrompt = template.getSystemPrompt();
        String userPrompt = buildSummaryPrompt(template.getPromptTemplate(), chapter);
        
        // 调用AI
        String aiResponse = callAIApi(config, systemPrompt, userPrompt);
        String summary = extractTextContent(aiResponse);
        
        // 保存概括
        chapterService.updateChapterSummary(chapterId, summary);
        
        logger.info("章节概括生成完成，长度: {}", summary.length());
        return summary;
    }

    /**
     * 构建章节概括提示词
     * 支持变量: {{title}}, {{content}}, {{chapterOrder}}, {{prevSummary}}, {{characters}}
     */
    private String buildSummaryPrompt(String promptTemplate, Chapter chapter) {
        String prompt = promptTemplate;
        
        // 替换章节相关变量
        prompt = prompt.replace("{{title}}", chapter.getTitle() != null ? chapter.getTitle() : "");
        prompt = prompt.replace("{{content}}", chapter.getContent() != null ? chapter.getContent() : "");
        prompt = prompt.replace("{{chapterOrder}}", String.valueOf(chapter.getChapterOrder()));
        
        // 替换历史概括
        String prevSummary = chapterService.getRecentSummaries(5,chapter.getBookId());
        prompt = prompt.replace("{{prevSummary}}", prevSummary);
        
        // 替换角色信息
        String charactersDesc = characterService.getAllCharactersDescription();
        prompt = prompt.replace("{{characters}}", charactersDesc);
        
        // 清理未使用的变量
        prompt = prompt.replaceAll("\\{\\{\\w+\\}\\}", "");
        
        return prompt;
    }

    /**
     * 生成章节汇总
     * 
     * @param startChapterId 起始章节ID
     * @param endChapterId 结束章节ID
     * @return 生成的汇总内容
     */
    public String generateCollection(Long startChapterId, Long endChapterId) {
        return generateCollection(startChapterId, endChapterId, null, null);
    }

    /**
     * 生成章节汇总（支持自定义模板和配置）
     * 
     * @param startChapterId 起始章节ID
     * @param endChapterId 结束章节ID
     * @param templateId 模板ID（可选，为空则使用默认模板）
     * @param configId 配置ID（可选，为空则使用默认配置）
     * @return 生成的汇总内容
     */
    public String generateCollection(Long startChapterId, Long endChapterId, Long templateId, Long configId) {
        return generateCollectionByChapterIds(null, templateId, configId);
    }

    /**
     * 根据选中的章节ID列表生成汇总（核心方法）
     * 
     * @param chapterIds 章节ID列表（按顺序）
     * @param templateId 模板ID（可选，为空则使用默认模板）
     * @param configId 配置ID（可选，为空则使用默认配置）
     * @return 生成的汇总内容
     */
    public String generateCollectionByChapterIds(List<Long> chapterIds, Long templateId, Long configId) {
        logger.info("开始生成章节汇总，章节IDs: {}, 模板ID: {}, 配置ID: {}", chapterIds, templateId, configId);
        
        // 根据章节ID获取内容
        String selectedContents = "";
        String selectedSummaries = "";
        String allSummaries = "";
        
        if (chapterIds != null && !chapterIds.isEmpty()) {
            // 按选中的章节ID获取内容和概括
            StringBuilder contentBuilder = new StringBuilder();
            StringBuilder summaryBuilder = new StringBuilder();
            for (Long chapterId : chapterIds) {
                Optional<Chapter> chapterOpt = chapterService.getChapterById(chapterId);
                if (chapterOpt.isPresent()) {
                    Chapter chapter = chapterOpt.get();
                    // 拼接正文内容
                    if (chapter.getContent() != null && !chapter.getContent().isEmpty()) {
                        contentBuilder.append("【第").append(chapter.getChapterOrder()).append("章 ")
                            .append(chapter.getTitle()).append("】\n")
                            .append(chapter.getContent()).append("\n\n");
                    }
                    // 拼接概括
                    if (chapter.getSummary() != null && !chapter.getSummary().isEmpty()) {
                        summaryBuilder.append("第").append(chapter.getChapterOrder()).append("章 ")
                            .append(chapter.getTitle()).append(": ")
                            .append(chapter.getSummary()).append("\n");
                    }
                }
            }
            selectedContents = contentBuilder.toString();
            selectedSummaries = summaryBuilder.toString();
            allSummaries = selectedSummaries; // 如果选中了特定章节，则使用选中章节的概括
        } else {
            // 没有选中特定章节，使用所有章节概括
            allSummaries = chapterService.getAllSummaries();
        }
        
        if (allSummaries.isEmpty() && selectedContents.isEmpty()) {
            throw new RuntimeException("没有可用的章节内容或概括");
        }
        
        // 获取配置和模板
        AIConfig config = configId != null ? 
            aiConfigService.getConfigById(configId).orElse(aiConfigService.getDefaultConfig()) : 
            aiConfigService.getDefaultConfig();
        PromptTemplate template = templateId != null ?
            promptTemplateService.getTemplateById(templateId).orElse(promptTemplateService.getDefaultTemplate(PromptTemplate.TYPE_COLLECTION)) :
            promptTemplateService.getDefaultTemplate(PromptTemplate.TYPE_COLLECTION);
        
        // 构建提示词（支持多种变量）
        String systemPrompt = template.getSystemPrompt();
        String userPrompt = buildCollectionPrompt(template.getPromptTemplate(), allSummaries, selectedContents, selectedSummaries);
        
        // 调用AI
        String aiResponse = callAIApi(config, systemPrompt, userPrompt);
        String collection = extractTextContent(aiResponse);
        
        logger.info("章节汇总生成完成，长度: {}", collection.length());
        return collection;
    }

    /**
     * 构建章节汇总提示词
     * 支持变量: {{summaries}}, {{selectedContents}}, {{selectedSummaries}}, {{characters}}, {{chapterCount}}, {{totalWords}}
     */
    private String buildCollectionPrompt(String promptTemplate, String summaries, String selectedContents, String selectedSummaries) {
        String prompt = promptTemplate;
        
        // 替换选中章节的正文内容
        prompt = prompt.replace("{{selectedContents}}", selectedContents != null ? selectedContents : "");
        
        // 替换选中章节的概括
        prompt = prompt.replace("{{selectedSummaries}}", selectedSummaries != null ? selectedSummaries : "");
        
        // 替换所有章节概括
        prompt = prompt.replace("{{summaries}}", summaries != null ? summaries : "");
        
        // 替换角色信息
        String charactersDesc = characterService.getAllCharactersDescription();
        prompt = prompt.replace("{{characters}}", charactersDesc);
        
        // 替换统计信息
        prompt = prompt.replace("{{chapterCount}}", String.valueOf(chapterService.getChapterCount()));
        prompt = prompt.replace("{{totalWords}}", String.valueOf(chapterService.getTotalWordCount()));
        
        // 清理未使用的变量
        prompt = prompt.replaceAll("\\{\\{\\w+\\}\\}", "");
        
        return prompt;
    }

    /**
     * AI生成角色信息
     * 
     * @param characterId 角色ID
     * @param userPrompt 用户提示词（描述角色需求）
     * @return 生成的角色信息描述
     */
    @Transactional
    public String generateCharacterInfo(Long characterId, String userPrompt,Long bookId) {
        return generateCharacterInfo(characterId, userPrompt, null, null,bookId);
    }

    /**
     * AI生成角色信息（支持自定义模板和配置）
     * 
     * @param characterId 角色ID（可选，为空则生成新角色）
     * @param userPrompt 用户提示词（描述角色需求）
     * @param templateId 模板ID（可选，为空则使用默认模板）
     * @param configId 配置ID（可选，为空则使用默认配置）
     * @return 生成的角色信息JSON字符串
     */
    @Transactional
    public String generateCharacterInfo(Long characterId, String userPrompt, Long templateId, Long configId, Long bookId) {
        logger.info("开始AI生成角色信息，角色ID: {}, 模板ID: {}, 配置ID: {}", characterId, templateId, configId);
        
        // 获取配置和模板
        AIConfig config = configId != null ? 
            aiConfigService.getConfigById(configId).orElse(aiConfigService.getDefaultConfig()) : 
            aiConfigService.getDefaultConfig();
        PromptTemplate template = templateId != null ?
            promptTemplateService.getTemplateById(templateId).orElse(promptTemplateService.getDefaultTemplate(PromptTemplate.TYPE_CHARACTER)) :
            promptTemplateService.getDefaultTemplate(PromptTemplate.TYPE_CHARACTER);
        
        // 构建提示词
        String systemPrompt = template.getSystemPrompt();
        String promptText = buildCharacterPrompt(template.getPromptTemplate(), characterId, userPrompt,bookId);
        
        // 调用AI
        String aiResponse = callAIApi(config, systemPrompt, promptText);
        String result = extractTextContent(aiResponse);
        
        logger.info("角色信息生成完成");
        return result;
    }

    /**
     * 构建角色生成提示词
     * 支持变量: {{userPrompt}}, {{characterName}}, {{characterInfo}}, {{existingCharacters}}, {{recentSummary}}
     */
    private String buildCharacterPrompt(String promptTemplate, Long characterId, String userPrompt,Long bookId) {
        String prompt = promptTemplate;
        
        // 替换用户提示词
        prompt = prompt.replace("{{userPrompt}}", userPrompt != null ? userPrompt : "");
        
        // 如果有角色ID，获取现有角色信息
        if (characterId != null) {
            Optional<Character> charOpt = characterService.getCharacterById(characterId);
            if (charOpt.isPresent()) {
                Character character = charOpt.get();
                prompt = prompt.replace("{{characterName}}", character.getName() != null ? character.getName() : "");
                String charInfo = buildCharacterInfoStr(character);
                prompt = prompt.replace("{{characterInfo}}", charInfo);
            }
        }
        
        // 替换已有角色列表
        String existingChars = characterService.getAllCharactersDescription();
        prompt = prompt.replace("{{existingCharacters}}", existingChars);
        
        // 替换最近章节概括（提供故事背景）
        String recentSummary = chapterService.getRecentSummaries(3,bookId);
        prompt = prompt.replace("{{recentSummary}}", recentSummary);
        
        // 清理未使用的变量
        prompt = prompt.replaceAll("\\{\\{\\w+\\}\\}", "");
        
        return prompt;
    }

    /**
     * 构建角色信息字符串
     */
    private String buildCharacterInfoStr(Character character) {
        StringBuilder sb = new StringBuilder();
        if (character.getName() != null) {
            sb.append("名称: ").append(character.getName()).append("\n");
        }
        if (character.getInfo() != null && !character.getInfo().isEmpty()) {
            sb.append(character.getInfo()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 获取AI配置
     */
    private AIConfig getAIConfig(Long configId) {
        if (configId != null) {
            Optional<AIConfig> configOpt = aiConfigService.getConfigById(configId);
            if (configOpt.isPresent()) {
                return configOpt.get();
            }
        }
        return aiConfigService.getDefaultConfig();
    }

    /**
     * 获取提示词模板
     */
    private PromptTemplate getPromptTemplate(Long templateId, String generateType) {
        if (templateId != null) {
            Optional<PromptTemplate> templateOpt = promptTemplateService.getTemplateById(templateId);
            if (templateOpt.isPresent()) {
                return templateOpt.get();
            }
        }
        // 根据生成类型获取默认模板
        String type = generateType != null ? generateType : PromptTemplate.TYPE_CONTENT;
        return promptTemplateService.getDefaultTemplate(type);
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(PromptTemplate template, GenerateRequest request) {
        // 优先使用自定义系统提示词
        if (request.getCustomSystemPrompt() != null && !request.getCustomSystemPrompt().isEmpty()) {
            return request.getCustomSystemPrompt();
        }
        return template.getSystemPrompt();
    }

    /**
     * 构建用户提示词
     * 支持变量替换：{{userPrompt}}, {{characters}}, {{prevSummary}}, {{title}}, {{content}}, {{previousChapterContent}}
     * 
     * 注意：{{characters}} 变量会根据 userPrompt 中出现的角色名称进行匹配过滤，
     * 只有在 userPrompt 中提到的角色才会被加入到提示词中
     */
    private String buildUserPrompt(PromptTemplate template, GenerateRequest request) {
        String promptText = template.getPromptTemplate();
        
        // 替换用户提示词
        String userPrompt = request.getUserPrompt();
        if (userPrompt != null) {
            promptText = promptText.replace("{{userPrompt}}", userPrompt);
        } else {
            promptText = promptText.replace("{{userPrompt}}", "");
            userPrompt = "";
        }
        
        // 替换前一章节的正文内容（用于新章节生成时提供上下文）
        if (request.getPreviousChapterContent() != null && !request.getPreviousChapterContent().isEmpty()) {
            promptText = promptText.replace("{{previousChapterContent}}", request.getPreviousChapterContent());
        } else {
            promptText = promptText.replace("{{previousChapterContent}}", "");
        }
        
        // 替换历史章节概括
        if (request.getIncludeHistory() != null && request.getIncludeHistory()) {
            int count = request.getHistoryCount() != null ? request.getHistoryCount() : 5;
            String summaries = chapterService.getRecentSummaries(count,request.getBookId());
            promptText = promptText.replace("{{prevSummary}}", summaries);
        } else {
            promptText = promptText.replace("{{prevSummary}}", "");
        }
        
        // 替换当前章节信息
        if (request.getChapterId() != null) {
            Optional<Chapter> chapterOpt = chapterService.getChapterById(request.getChapterId());
            if (chapterOpt.isPresent()) {
                Chapter chapter = chapterOpt.get();
                promptText = promptText.replace("{{title}}", chapter.getTitle() != null ? chapter.getTitle() : "");
                promptText = promptText.replace("{{content}}", chapter.getContent() != null ? chapter.getContent() : "");
            }
        }

        // 替换角色信息 - 根据 userPrompt 中出现的角色名称进行匹配
        if (request.getCharacterIds() != null && !request.getCharacterIds().isEmpty()) {
            // 如果明确指定了角色ID，则使用指定的角色
            String charactersDesc = characterService.getCharactersDescription(request.getCharacterIds());
            promptText = promptText.replace("{{characters}}", charactersDesc);
        } else {
            // 根据 userPrompt 匹配角色名称，只包含在 userPrompt 中出现的角色
            String matchedCharsDesc = "";
            if("继续".equals(userPrompt)){
                matchedCharsDesc = characterService.getMatchedCharactersDescription(promptText);
            }else{
                matchedCharsDesc = characterService.getMatchedCharactersDescription(userPrompt);
            }

            if (matchedCharsDesc != null && !matchedCharsDesc.isEmpty()) {
                promptText = promptText.replace("{{characters}}", matchedCharsDesc);
                logger.info("根据userPrompt匹配到的角色信息: {}", matchedCharsDesc);
            } else {
                // 如果没有匹配到任何角色，则不添加角色信息
                promptText = promptText.replace("{{characters}}", "");
                logger.info("userPrompt中未匹配到任何角色名称，不添加角色信息");
            }
        }
        
        // 清理未使用的变量
        promptText = promptText.replaceAll("\\{\\{\\w+\\}\\}", "");

        // 匹配世界书内容并附加到提示词中
        if (request.getBookId() != null) {
            String worldBookContent = worldBookService.getMatchedWorldBookContent(userPrompt, request.getBookId());
            if (worldBookContent != null && !worldBookContent.isEmpty()) {
                promptText = promptText + worldBookContent;
                logger.info("匹配到世界书内容并已附加到提示词");
            }
        }

        return promptText;
    }

    /**
     * 调用AI API
     */
    private String callAIApi(AIConfig config, String systemPrompt, String userPrompt) {
        logger.debug("调用AI API, URL: {}, Model: {}", config.getApiUrl(), config.getModel());
        
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(config.getTimeout(), TimeUnit.SECONDS)
                .readTimeout(config.getTimeout(), TimeUnit.SECONDS)
                .writeTimeout(config.getTimeout(), TimeUnit.SECONDS)
                .build();
        
        try {
            // 构建请求体
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", config.getModel());
            requestBody.put("temperature", config.getTemperature());
            requestBody.put("top_p", config.getTopP());
            requestBody.put("max_tokens", config.getMaxTokens());
            
            if (config.getFrequencyPenalty() != null) {
                requestBody.put("frequency_penalty", config.getFrequencyPenalty());
            }
            if (config.getPresencePenalty() != null) {
                requestBody.put("presence_penalty", config.getPresencePenalty());
            }
            
            // 构建消息数组
            ArrayNode messages = objectMapper.createArrayNode();
            
            // 系统消息
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                ObjectNode systemMessage = objectMapper.createObjectNode();
                systemMessage.put("role", "system");
                systemMessage.put("content", systemPrompt);
                messages.add(systemMessage);
            }
            
            // 用户消息
            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);
            messages.add(userMessage);
            
            requestBody.set("messages", messages);
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            logger.debug("AI请求体: {}", jsonBody);
            
            // 发送请求
            Request httpRequest = new Request.Builder()
                    .url(config.getApiUrl())
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .post(RequestBody.create(jsonBody, JSON_MEDIA_TYPE))
                    .build();
            
            try (Response response = client.newCall(httpRequest).execute()) {
                if (!response.isSuccessful()) {
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    logger.error("AI API调用失败: {} - {}", response.code(), errorBody);
                    throw new RuntimeException("AI API调用失败: " + response.code() + " - " + errorBody);
                }
                
                String responseBody = response.body().string();
                logger.debug("AI响应: {}", responseBody);
                return responseBody;
            }
            
        } catch (IOException e) {
            logger.error("AI API调用异常", e);
            throw new RuntimeException("AI API调用异常: " + e.getMessage(), e);
        }
    }

    /**
     * 解析AI响应，提取生成的内容
     */
    private AIGeneratedContent parseAIResponse(String aiResponse) {
        try {
            JsonNode responseJson = objectMapper.readTree(aiResponse);
            
            // 提取content内容
            JsonNode choices = responseJson.get("choices");
            if (choices == null || !choices.isArray() || choices.size() == 0) {
                throw new RuntimeException("AI响应格式错误：缺少choices字段");
            }
            
            JsonNode message = choices.get(0).get("message");
            if (message == null) {
                throw new RuntimeException("AI响应格式错误：缺少message字段");
            }
            
            String contentStr = message.get("content").asText();
            logger.debug("AI返回内容: {}", contentStr);
            
            // 尝试解析为JSON
            AIGeneratedContent content = parseContentAsJson(contentStr);
            if (content != null) {
                return content;
            }
            
            // 如果不是JSON格式，直接作为正文内容
            content = new AIGeneratedContent();
            content.setContent(contentStr);
            return content;
            
        } catch (Exception e) {
            logger.error("解析AI响应失败", e);
            throw new RuntimeException("解析AI响应失败: " + e.getMessage(), e);
        }
    }

    /**
     * 尝试将内容解析为JSON格式
     */
    private AIGeneratedContent parseContentAsJson(String contentStr) {
        try {
            // 提取JSON部分（可能被markdown代码块包裹）
            String jsonStr = extractJsonFromContent(contentStr);
            if (jsonStr != null) {
                return objectMapper.readValue(jsonStr, AIGeneratedContent.class);
            }
        } catch (Exception e) {
            logger.debug("内容不是JSON格式，将作为纯文本处理");
        }
        return null;
    }

    /**
     * 从内容中提取JSON字符串
     */
    private String extractJsonFromContent(String content) {
        // 尝试直接解析
        if (content.trim().startsWith("{")) {
            return content.trim();
        }
        
        // 尝试从markdown代码块中提取
        Pattern pattern = Pattern.compile("```(?:json)?\\s*\\n?([\\s\\S]*?)\\n?```");
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        
        // 尝试找到JSON对象
        int start = content.indexOf("{");
        int end = content.lastIndexOf("}");
        if (start != -1 && end > start) {
            return content.substring(start, end + 1);
        }
        
        return null;
    }

    /**
     * 从AI响应中提取纯文本内容
     */
    private String extractTextContent(String aiResponse) {
        try {
            JsonNode responseJson = objectMapper.readTree(aiResponse);
            JsonNode choices = responseJson.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null) {
                    return message.get("content").asText();
                }
            }
        } catch (Exception e) {
            logger.error("提取文本内容失败", e);
        }
        return aiResponse;
    }

    /**
     * 解析内容中的【@ 内容 @】格式文字，并更新角色信息
     * 格式: 【@[#角色名#]描述内容@】
     * 将匹配到的内容以 ##**内容**## 格式添加到角色信息中
     * 支持正文中存在多个【@ 内容 @】格式的内容，全部解析并存放
     * 
     * @param contentText 生成的正文内容
     */
    @Transactional
    public void parseAndUpdateCharacterInfo(String contentText) {
        if (contentText == null || contentText.isEmpty()) {
            return;
        }
        logger.info("============正文内容匹配更新角色信息================");
        // 匹配【@ 内容 @】格式
        Pattern pattern = Pattern.compile("【@([^@]+)@】");
        Matcher matcher = pattern.matcher(contentText);
        
        // 收集所有匹配到的内容，按角色分组
        List<Character> allCharacters = characterService.getAllCharacters();
        java.util.Map<Long, List<String>> characterContentsMap = new java.util.HashMap<>();
        
        while (matcher.find()) {
            String matchedContent = matcher.group(1).trim();
            logger.info("匹配到角色信息标记: {}", matchedContent);
            
            // 使用 [#姓名#] 格式匹配角色名
            Pattern namePattern = Pattern.compile("\\[#([^#]+)#\\]");
            Matcher nameMatcher = namePattern.matcher(matchedContent);
            
            if (nameMatcher.find()) {
                String characterName = nameMatcher.group(1).trim();
                logger.info("从标记中提取到角色名: {}", characterName);
                
                // 查找对应的角色
                for (Character character : allCharacters) {
                    if (character.getName().equals(characterName)) {
                        // 收集该角色的所有匹配内容
                        characterContentsMap.computeIfAbsent(character.getId(), k -> new ArrayList<>())
                            .add(matchedContent);
                        logger.info("为角色 {} (ID:{}) 收集到内容", characterName, character.getId());
                        break;
                    }
                }
            } else {
                logger.warn("标记内容中未找到 [#姓名#] 格式的角色名: {}", matchedContent);
            }
        }
        
        // 批量更新每个角色的信息
        for (java.util.Map.Entry<Long, List<String>> entry : characterContentsMap.entrySet()) {
            Long characterId = entry.getKey();
            List<String> contents = entry.getValue();
            
            Optional<Character> charOpt = characterService.getCharacterById(characterId);
            if (charOpt.isPresent()) {
                updateCharacterWithMatchedContent(charOpt.get(), contents);
            }
        }
    }
    
    /**
     * 更新角色信息，将匹配到的多个内容以 ##**内容**## 格式添加或替换
     * 
     * @param character 角色对象
     * @param matchedContents 匹配到的内容列表
     */
    @Transactional
    public void updateCharacterWithMatchedContent(Character character, List<String> matchedContents) {
        if (matchedContents == null || matchedContents.isEmpty()) {
            return;
        }
        
        String currentInfo = character.getInfo();
        if (currentInfo == null) {
            currentInfo = "";
        }
        
        // 构建所有新内容的格式化字符串
        StringBuilder newContentsBuilder = new StringBuilder();
        for (String content : matchedContents) {
            if (newContentsBuilder.length() > 0) {
                newContentsBuilder.append("\n");
            }
            newContentsBuilder.append("【@").append(content).append("@】");
        }
        String formattedContents = newContentsBuilder.toString();
        
        // 移除现有的所有 ##**...**## 格式内容
        Pattern existingPattern = Pattern.compile("【@([^@]+)@】");
        String cleanedInfo = existingPattern.matcher(currentInfo).replaceAll("").trim();
        
        // 追加新内容
        String newInfo;
        if (cleanedInfo.isEmpty()) {
            newInfo = formattedContents;
        } else {
            newInfo = cleanedInfo + "\n" + formattedContents;
        }
        
        logger.info("更新角色 {} 的标记内容，共 {} 条: {}", character.getName(), matchedContents.size(), formattedContents);
        
        character.setInfo(newInfo);
        characterService.updateCharacter(character.getId(), character);
    }
    
    /**
     * 更新角色信息，将单个匹配到的内容以 ##**内容**## 格式添加
     * 保留此方法用于向后兼容
     * 
     * @param character 角色对象
     * @param matchedContent 匹配到的内容
     */
    @Transactional
    public void updateCharacterWithMatchedContent(Character character, String matchedContent) {
        List<String> contents = new ArrayList<>();
        contents.add(matchedContent);
        updateCharacterWithMatchedContent(character, contents);
    }

    /**
     * 保存生成的内容到数据库
     */
    @Transactional
    public void saveGeneratedContent(GenerateRequest request, AIGeneratedContent content) {
        Chapter chapter;
        
        // 创建新章节或更新现有章节
        if (request.getCreateNewChapter() != null && request.getCreateNewChapter()) {
            // 创建新章节
            String title = content.getTitle();
            if (title == null || title.isEmpty()) {
                title = request.getNewChapterTitle();
            }
            if (title == null || title.isEmpty()) {
                title = "第" + chapterService.getNextChapterOrder() + "章";
            }
            Chapter newChapter = new Chapter();
            newChapter.setTitle(title);
            newChapter.setBookId(request.getBookId());
            chapter = chapterService.createChapter(newChapter);
        } else if (request.getChapterId() != null) {
            // 更新现有章节
            Optional<Chapter> chapterOpt = chapterService.getChapterById(request.getChapterId());
            if (!chapterOpt.isPresent()) {
                throw new RuntimeException("章节不存在，ID: " + request.getChapterId());
            }
            chapter = chapterOpt.get();
        } else {
            // 创建新章节（默认行为）
            String title = content.getTitle();
            if (title == null || title.isEmpty()) {
                title = "第" + chapterService.getNextChapterOrder() + "章";
            }
            Chapter newChapter = new Chapter();
            newChapter.setTitle(title);
            newChapter.setBookId(request.getBookId());
            chapter = chapterService.createChapter(newChapter);
        }
        
        // 更新章节内容
        if (content.getContent() != null) {
            chapter.setContent(content.getContent());
        }
        if (content.getTitle() != null && !content.getTitle().isEmpty()) {
            chapter.setTitle(content.getTitle());
        }
        if (content.getChapter() != null) {
            chapter.setSummary(content.getChapter());
        }
        if (request.getUserPrompt() != null) {
            chapter.setUserPrompt(request.getUserPrompt());
        }
        chapter.setUpdateTime(LocalDateTime.now());
        
        // 保存章节
        chapterService.updateChapter(chapter.getId(), chapter);
        
        // 保存角色信息
        if (content.getRole() != null && !content.getRole().isEmpty()) {
            for (AIGeneratedContent.RoleInfo roleInfo : content.getRole()) {
                saveOrUpdateCharacter(roleInfo, chapter.getId());
            }
        }
    }

    /**
     * 保存或更新角色信息
     */
    @Transactional
    public void saveOrUpdateCharacter(AIGeneratedContent.RoleInfo roleInfo, Long chapterId) {
        if (roleInfo.getName() == null || roleInfo.getName().isEmpty()) {
            return;
        }
        
        Optional<Character> existingOpt = characterService.getCharacterByName(roleInfo.getName());
        Character character;
        
        if (existingOpt.isPresent()) {
            // 更新现有角色
            character = existingOpt.get();
        } else {
            // 创建新角色
            character = new Character();
            character.setName(roleInfo.getName());
            character.setChapterId(chapterId);
        }
        
        // 整合所有角色信息到info字段
        StringBuilder infoBuilder = new StringBuilder();
        if (roleInfo.getRole() != null && !roleInfo.getRole().isEmpty()) {
            infoBuilder.append("【角色定位】").append(roleInfo.getRole()).append("\n");
        }
        if (roleInfo.getAppearance() != null && !roleInfo.getAppearance().isEmpty()) {
            infoBuilder.append("【外貌描述】").append(roleInfo.getAppearance()).append("\n");
        }
        if (roleInfo.getPersonality() != null && !roleInfo.getPersonality().isEmpty()) {
            infoBuilder.append("【性格特点】").append(roleInfo.getPersonality()).append("\n");
        }
        if (roleInfo.getBackground() != null && !roleInfo.getBackground().isEmpty()) {
            infoBuilder.append("【背景故事】").append(roleInfo.getBackground()).append("\n");
        }
        if (roleInfo.getNotes() != null && !roleInfo.getNotes().isEmpty()) {
            infoBuilder.append("【备注】").append(roleInfo.getNotes()).append("\n");
        }
        
        String info = infoBuilder.toString();
        if (!info.isEmpty()) {
            character.setInfo(info);
        }
        
        if (existingOpt.isPresent()) {
            characterService.updateCharacter(character.getId(), character);
        } else {
            characterService.createCharacter(character);
        }
    }

    /**
     * 测试AI连接
     * 
     * @param configId 配置ID
     * @return 测试结果
     */
    public String testConnection(Long configId) {
        AIConfig config = getAIConfig(configId);
        try {
            String response = callAIApi(config, "You are a helpful assistant.", "Say 'Hello, connection successful!' in Chinese.");
            return "连接成功！AI响应: " + extractTextContent(response);
        } catch (Exception e) {
            return "连接失败: " + e.getMessage();
        }
    }

    /**
     * 流式生成内容
     * 
     * @param request 生成请求
     * @param emitter SSE发射器
     */
    public void generateContentStream(GenerateRequest request, SseEmitter emitter) {
        logger.info("开始流式生成内容，请求参数: {}", request);

        try {
            // 1. 获取AI配置
            AIConfig config = getAIConfig(request.getConfigId());
            
            // 2. 获取提示词模板
            PromptTemplate template = getPromptTemplate(request.getTemplateId(), request.getGenerateType());
            
            // 3. 构建提示词
            String systemPrompt = buildSystemPrompt(template, request);
            String userPrompt = buildUserPrompt(template, request);
            
            // 4. 调用流式AI接口（启用角色信息解析）
            callAIApiStreamWithParse(config, systemPrompt, userPrompt, emitter, true);
            
        } catch (Exception e) {
            logger.error("流式生成内容失败", e);
            try {
                emitter.send(SseEmitter.event().data("[ERROR] " + e.getMessage()));
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        }
    }

    /**
     * 流式生成章节概括
     * 
     * @param chapterId 章节ID
     * @param templateId 模板ID（可选）
     * @param configId 配置ID（可选）
     * @param emitter SSE发射器
     */
    public void generateSummaryStream(Long chapterId, Long templateId, Long configId, SseEmitter emitter) {
        logger.info("开始流式生成章节概括，章节ID: {}", chapterId);
        
        try {
            Optional<Chapter> chapterOpt = chapterService.getChapterById(chapterId);
            if (!chapterOpt.isPresent()) {
                throw new RuntimeException("章节不存在，ID: " + chapterId);
            }
            
            Chapter chapter = chapterOpt.get();
            if (chapter.getContent() == null || chapter.getContent().isEmpty()) {
                throw new RuntimeException("章节内容为空，无法生成概括");
            }
            
            // 获取配置和模板
            AIConfig config = configId != null ? 
                aiConfigService.getConfigById(configId).orElse(aiConfigService.getDefaultConfig()) : 
                aiConfigService.getDefaultConfig();
            PromptTemplate template = templateId != null ?
                promptTemplateService.getTemplateById(templateId).orElse(promptTemplateService.getDefaultTemplate(PromptTemplate.TYPE_SUMMARY)) :
                promptTemplateService.getDefaultTemplate(PromptTemplate.TYPE_SUMMARY);
            
            // 构建提示词
            String systemPrompt = template.getSystemPrompt();
            String userPrompt = buildSummaryPrompt(template.getPromptTemplate(), chapter);
            
            // 调用流式AI
            callAIApiStream(config, systemPrompt, userPrompt, emitter);
            
        } catch (Exception e) {
            logger.error("流式生成章节概括失败", e);
            try {
                emitter.send(SseEmitter.event().data("[ERROR] " + e.getMessage()));
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        }
    }

    /**
     * 流式生成章节汇总
     * 
     * @param chapterIds 章节ID列表（可选，为空则汇总所有章节）
     * @param templateId 模板ID（可选）
     * @param configId 配置ID（可选）
     * @param emitter SSE发射器
     */
    public void generateCollectionStream(List<Long> chapterIds, Long templateId, Long configId, SseEmitter emitter) {
        logger.info("开始流式生成章节汇总，章节IDs: {}", chapterIds);
        
        try {
            // 根据章节ID获取内容
            String selectedContents = "";
            String selectedSummaries = "";
            String allSummaries = "";
            
            if (chapterIds != null && !chapterIds.isEmpty()) {
                // 按选中的章节ID获取内容和概括
                StringBuilder contentBuilder = new StringBuilder();
                StringBuilder summaryBuilder = new StringBuilder();
                for (Long cid : chapterIds) {
                    Optional<Chapter> chapterOpt = chapterService.getChapterById(cid);
                    if (chapterOpt.isPresent()) {
                        Chapter chapter = chapterOpt.get();
                        // 拼接正文内容
                        if (chapter.getContent() != null && !chapter.getContent().isEmpty()) {
                            contentBuilder.append("【第").append(chapter.getChapterOrder()).append("章 ")
                                .append(chapter.getTitle()).append("】\n")
                                .append(chapter.getContent()).append("\n\n");
                        }
                        // 拼接概括
                        if (chapter.getSummary() != null && !chapter.getSummary().isEmpty()) {
                            summaryBuilder.append("第").append(chapter.getChapterOrder()).append("章 ")
                                .append(chapter.getTitle()).append(": ")
                                .append(chapter.getSummary()).append("\n");
                        }
                    }
                }
                selectedContents = contentBuilder.toString();
                selectedSummaries = summaryBuilder.toString();
                allSummaries = selectedSummaries;
            } else {
                // 没有选中特定章节，使用所有章节概括
                allSummaries = chapterService.getAllSummaries();
            }
            
            if (allSummaries.isEmpty() && selectedContents.isEmpty()) {
                throw new RuntimeException("没有可用的章节内容或概括");
            }
            
            // 获取配置和模板
            AIConfig config = configId != null ? 
                aiConfigService.getConfigById(configId).orElse(aiConfigService.getDefaultConfig()) : 
                aiConfigService.getDefaultConfig();
            PromptTemplate template = templateId != null ?
                promptTemplateService.getTemplateById(templateId).orElse(promptTemplateService.getDefaultTemplate(PromptTemplate.TYPE_COLLECTION)) :
                promptTemplateService.getDefaultTemplate(PromptTemplate.TYPE_COLLECTION);
            
            // 构建提示词
            String systemPrompt = template.getSystemPrompt();
            String userPrompt = buildCollectionPrompt(template.getPromptTemplate(), allSummaries, selectedContents, selectedSummaries);
            
            // 调用流式AI
            callAIApiStream(config, systemPrompt, userPrompt, emitter);
            
        } catch (Exception e) {
            logger.error("流式生成章节汇总失败", e);
            try {
                emitter.send(SseEmitter.event().data("[ERROR] " + e.getMessage()));
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        }
    }

    /**
     * 流式AI生成角色信息
     * 
     * @param characterId 角色ID（可选，为空则生成新角色）
     * @param userPrompt 用户提示词（描述角色需求）
     * @param templateId 模板ID（可选）
     * @param configId 配置ID（可选）
     * @param emitter SSE发射器
     */
    public void generateCharacterInfoStream(Long characterId, String userPrompt, Long templateId, Long configId, SseEmitter emitter,Long bookId) {
        logger.info("开始流式AI生成角色信息，角色ID: {}", characterId);
        
        try {
            // 获取配置和模板
            AIConfig config = configId != null ? 
                aiConfigService.getConfigById(configId).orElse(aiConfigService.getDefaultConfig()) : 
                aiConfigService.getDefaultConfig();
            PromptTemplate template = templateId != null ?
                promptTemplateService.getTemplateById(templateId).orElse(promptTemplateService.getDefaultTemplate(PromptTemplate.TYPE_CHARACTER)) :
                promptTemplateService.getDefaultTemplate(PromptTemplate.TYPE_CHARACTER);
            
            // 构建提示词
            String systemPrompt = template.getSystemPrompt();
            String promptText = buildCharacterPrompt(template.getPromptTemplate(), characterId, userPrompt,bookId);
            
            // 调用流式AI
            callAIApiStream(config, systemPrompt, promptText, emitter);
            
        } catch (Exception e) {
            logger.error("流式生成角色信息失败", e);
            try {
                emitter.send(SseEmitter.event().data("[ERROR] " + e.getMessage()));
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        }
    }

    /**
     * 流式调用AI API（带角色信息解析）
     */
    private void callAIApiStreamWithParse(AIConfig config, String systemPrompt, String userPrompt, SseEmitter emitter, boolean parseCharacterInfo) {
        logger.debug("流式调用AI API, URL: {}, Model: {}, 解析角色信息: {}", config.getApiUrl(), config.getModel(), parseCharacterInfo);
        
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(config.getTimeout(), TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(config.getTimeout(), TimeUnit.SECONDS)
                .build();
        
        // 用于收集完整内容
        StringBuilder fullContentBuilder = new StringBuilder();
        
        try {
            // 构建请求体
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", config.getModel());
            requestBody.put("temperature", config.getTemperature());
            requestBody.put("top_p", config.getTopP());
            requestBody.put("max_tokens", config.getMaxTokens());
            requestBody.put("stream", true); // 启用流式输出
            
            if (config.getFrequencyPenalty() != null) {
                requestBody.put("frequency_penalty", config.getFrequencyPenalty());
            }
            if (config.getPresencePenalty() != null) {
                requestBody.put("presence_penalty", config.getPresencePenalty());
            }
            
            // 构建消息数组
            ArrayNode messages = objectMapper.createArrayNode();
            
            // 系统消息
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                ObjectNode systemMessage = objectMapper.createObjectNode();
                systemMessage.put("role", "system");
                systemMessage.put("content", systemPrompt);
                messages.add(systemMessage);
            }
            
            // 用户消息
            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);
            messages.add(userMessage);
            
            requestBody.set("messages", messages);
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            logger.debug("AI流式请求体: {}", jsonBody);
            
            // 发送请求
            Request httpRequest = new Request.Builder()
                    .url(config.getApiUrl())
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Accept", "text/event-stream")
                    .post(RequestBody.create(jsonBody, JSON_MEDIA_TYPE))
                    .build();
            
            Response response = client.newCall(httpRequest).execute();
            
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                logger.error("AI API流式调用失败: {} - {}", response.code(), errorBody);
                emitter.send(SseEmitter.event().data("[ERROR] AI API调用失败: " + response.code()));
                emitter.complete();
                return;
            }
            
            // 读取流式响应
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);
                        if ("[DONE]".equals(data)) {
                            emitter.send(SseEmitter.event().data("[DONE]"));
                            break;
                        }
                        // 转发SSE数据
                        emitter.send(SseEmitter.event().data(data));
                        
                        // 收集内容用于后续解析
                        if (parseCharacterInfo) {
                            try {
                                JsonNode jsonNode = objectMapper.readTree(data);
                                if (jsonNode.has("choices") && jsonNode.get("choices").isArray() && jsonNode.get("choices").size() > 0) {
                                    JsonNode delta = jsonNode.get("choices").get(0).get("delta");
                                    if (delta != null && delta.has("content")) {
                                        fullContentBuilder.append(delta.get("content").asText());
                                    }
                                }
                            } catch (Exception e) {
                                // 忽略解析错误，继续处理
                            }
                        }
                    }
                }
            }
            
            // 流式输出完成后，解析角色信息
//            if (parseCharacterInfo && fullContentBuilder.length() > 0) {
//                String fullContent = fullContentBuilder.toString();
//                logger.info("流式输出完成，开始解析角色信息，内容长度: {}", fullContent.length());
//                parseAndUpdateCharacterInfo(fullContent);
//            }
            
            emitter.complete();
            logger.info("流式生成完成");
            
        } catch (IOException e) {
            logger.error("AI API流式调用异常", e);
            try {
                emitter.send(SseEmitter.event().data("[ERROR] " + e.getMessage()));
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        }
    }

    /**
     * 流式调用AI API（不解析角色信息）
     */
    private void callAIApiStream(AIConfig config, String systemPrompt, String userPrompt, SseEmitter emitter) {
        logger.debug("流式调用AI API, URL: {}, Model: {}", config.getApiUrl(), config.getModel());
        
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(config.getTimeout(), TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(config.getTimeout(), TimeUnit.SECONDS)
                .build();
        
        try {
            // 构建请求体
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", config.getModel());
            requestBody.put("temperature", config.getTemperature());
            requestBody.put("top_p", config.getTopP());
            requestBody.put("max_tokens", config.getMaxTokens());
            requestBody.put("stream", true); // 启用流式输出
            
            if (config.getFrequencyPenalty() != null) {
                requestBody.put("frequency_penalty", config.getFrequencyPenalty());
            }
            if (config.getPresencePenalty() != null) {
                requestBody.put("presence_penalty", config.getPresencePenalty());
            }
            
            // 构建消息数组
            ArrayNode messages = objectMapper.createArrayNode();
            
            // 系统消息
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                ObjectNode systemMessage = objectMapper.createObjectNode();
                systemMessage.put("role", "system");
                systemMessage.put("content", systemPrompt);
                messages.add(systemMessage);
            }
            
            // 用户消息
            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("role", "user");
            userMessage.put("content", userPrompt);
            messages.add(userMessage);
            
            requestBody.set("messages", messages);
            
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            logger.debug("AI流式请求体: {}", jsonBody);
            
            // 发送请求
            Request httpRequest = new Request.Builder()
                    .url(config.getApiUrl())
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Authorization", "Bearer " + config.getApiKey())
                    .addHeader("Accept", "text/event-stream")
                    .post(RequestBody.create(jsonBody, JSON_MEDIA_TYPE))
                    .build();
            
            Response response = client.newCall(httpRequest).execute();
            
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                logger.error("AI API流式调用失败: {} - {}", response.code(), errorBody);
                emitter.send(SseEmitter.event().data("[ERROR] AI API调用失败: " + response.code()));
                emitter.complete();
                return;
            }
            
            // 读取流式响应
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("data: ")) {
                        String data = line.substring(6);
                        if ("[DONE]".equals(data)) {
                            emitter.send(SseEmitter.event().data("[DONE]"));
                            break;
                        }
                        // 转发SSE数据
                        emitter.send(SseEmitter.event().data(data));
                    }
                }
            }
            
            emitter.complete();
            logger.info("流式生成完成");
            
        } catch (IOException e) {
            logger.error("AI API流式调用异常", e);
            try {
                emitter.send(SseEmitter.event().data("[ERROR] " + e.getMessage()));
                emitter.complete();
            } catch (Exception ex) {
                emitter.completeWithError(ex);
            }
        }
    }
}
