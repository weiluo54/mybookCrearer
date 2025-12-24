package com.novel.controller;

import com.novel.dto.AIGeneratedContent;
import com.novel.dto.ApiResponse;
import com.novel.dto.GenerateRequest;
import com.novel.entity.*;
import com.novel.entity.Character;
import com.novel.service.*;
import com.novel.entity.Book;
import com.novel.entity.WorldBook;
import com.novel.service.BookService;
import com.novel.service.WorldBookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * AI网文创作系统主控制器
 * 整合章节、角色、AI生成、配置等所有API接口
 * 
 * @author NovelCreator
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class NovelController {

    private static final Logger logger = LoggerFactory.getLogger(NovelController.class);

    private final ChapterService chapterService;
    private final CharacterService characterService;
    private final AIService aiService;
    private final PromptTemplateService promptTemplateService;
    private final AIConfigService aiConfigService;
    private final SummaryCollectionService summaryCollectionService;
    private final BookService bookService;
    private final WorldBookService worldBookService;

    /**
     * 构造函数注入所有服务
     */
    public NovelController(ChapterService chapterService,
                          CharacterService characterService,
                          AIService aiService,
                          PromptTemplateService promptTemplateService,
                          AIConfigService aiConfigService,
                          SummaryCollectionService summaryCollectionService,
                          BookService bookService,
                          WorldBookService worldBookService) {
        this.chapterService = chapterService;
        this.characterService = characterService;
        this.aiService = aiService;
        this.promptTemplateService = promptTemplateService;
        this.aiConfigService = aiConfigService;
        this.summaryCollectionService = summaryCollectionService;
        this.bookService = bookService;
        this.worldBookService = worldBookService;
    }

    // ==================== 统计信息接口 ====================

    /**
     * 获取系统统计信息
     * @param bookId 书籍ID（可选，不传则返回当前激活书籍的统计）
     * @return 统计数据
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats(@RequestParam(required = false) Long bookId) {
        // 如果没传bookId，使用当前激活的书籍
        if (bookId == null) {
            Optional<Book> activeBook = bookService.getActiveBook();
            if (activeBook.isPresent()) {
                bookId = activeBook.get().getId();
            }
        }
        
        Map<String, Object> stats = new HashMap<>();
        if (bookId != null) {
            stats.put("chapterCount", chapterService.getChapterCountByBookId(bookId));
            stats.put("totalWordCount", chapterService.getTotalWordCountByBookId(bookId));
            stats.put("characterCount", characterService.getCharacterCountByBookId(bookId));
            stats.put("collectionCount", summaryCollectionService.getCollectionCountByBookId(bookId));
        } else {
            stats.put("chapterCount", 0);
            stats.put("totalWordCount", 0);
            stats.put("characterCount", 0);
            stats.put("collectionCount", 0);
        }
        return ApiResponse.success(stats);
    }

    // ==================== 章节管理接口 ====================

    /**
     * 获取所有章节（支持按书籍筛选）
     * @param bookId 书籍ID（可选，不传则返回当前激活书籍的章节）
     */
    @GetMapping("/chapters")
    public ApiResponse<List<Chapter>> getAllChapters(@RequestParam(required = false) Long bookId) {
        // 如果没传bookId，使用当前激活的书籍
        if (bookId == null) {
            Optional<Book> activeBook = bookService.getActiveBook();
            if (activeBook.isPresent()) {
                bookId = activeBook.get().getId();
            }
        }
        if (bookId != null) {
            return ApiResponse.success(chapterService.getChaptersByBookId(bookId));
        }
        return ApiResponse.success(java.util.Collections.emptyList());
    }

    /**
     * 根据ID获取章节
     */
    @GetMapping("/chapters/{id}")
    public ApiResponse<Chapter> getChapter(@PathVariable Long id) {
        Optional<Chapter> chapter = chapterService.getChapterById(id);
        return chapter.map(ApiResponse::success)
                .orElse(ApiResponse.notFound("章节不存在"));
    }

    /**
     * 创建章节
     */
    @PostMapping("/chapters")
    public ApiResponse<Chapter> createChapter(@RequestBody Chapter chapter) {
        try {
            Chapter created = chapterService.createChapter(chapter);
            return ApiResponse.success("章节创建成功", created);
        } catch (Exception e) {
            logger.error("创建章节失败", e);
            return ApiResponse.error("创建章节失败: " + e.getMessage());
        }
    }

    /**
     * 更新章节
     */
    @PutMapping("/chapters/{id}")
    public ApiResponse<Chapter> updateChapter(@PathVariable Long id, @RequestBody Chapter chapter) {
        try {
            Chapter updated = chapterService.updateChapter(id, chapter);
            return ApiResponse.success("章节更新成功", updated);
        } catch (Exception e) {
            logger.error("更新章节失败", e);
            return ApiResponse.error("更新章节失败: " + e.getMessage());
        }
    }

    /**
     * 删除章节
     */
    @DeleteMapping("/chapters/{id}")
    public ApiResponse<Void> deleteChapter(@PathVariable Long id) {
        try {
            chapterService.deleteChapter(id);
            return ApiResponse.success("章节删除成功", null);
        } catch (Exception e) {
            logger.error("删除章节失败", e);
            return ApiResponse.error("删除章节失败: " + e.getMessage());
        }
    }

    // ==================== 角色管理接口 ====================

    /**
     * 获取所有角色（支持按书籍筛选）
     * @param bookId 书籍ID（可选，不传则返回当前激活书籍的角色）
     */
    @GetMapping("/characters")
    public ApiResponse<List<Character>> getAllCharacters(@RequestParam(required = false) Long bookId) {
        // 如果没传bookId，使用当前激活的书籍
        if (bookId == null) {
            Optional<Book> activeBook = bookService.getActiveBook();
            if (activeBook.isPresent()) {
                bookId = activeBook.get().getId();
            }
        }
        if (bookId != null) {
            return ApiResponse.success(characterService.getCharactersByBookId(bookId));
        }
        return ApiResponse.success(java.util.Collections.emptyList());
    }

    /**
     * 根据ID获取角色
     */
    @GetMapping("/characters/{id}")
    public ApiResponse<Character> getCharacter(@PathVariable Long id) {
        Optional<Character> character = characterService.getCharacterById(id);
        return character.map(ApiResponse::success)
                .orElse(ApiResponse.notFound("角色不存在"));
    }

    /**
     * 创建角色
     */
    @PostMapping("/characters")
    public ApiResponse<Character> createCharacter(@RequestBody Character character) {
        try {
            Character created = characterService.createCharacter(character);
            return ApiResponse.success("角色创建成功", created);
        } catch (Exception e) {
            logger.error("创建角色失败", e);
            return ApiResponse.error("创建角色失败: " + e.getMessage());
        }
    }

    /**
     * 更新角色
     */
    @PutMapping("/characters/{id}")
    public ApiResponse<Character> updateCharacter(@PathVariable Long id, @RequestBody Character character) {
        try {
            Character updated = characterService.updateCharacter(id, character);
            return ApiResponse.success("角色更新成功", updated);
        } catch (Exception e) {
            logger.error("更新角色失败", e);
            return ApiResponse.error("更新角色失败: " + e.getMessage());
        }
    }

    /**
     * 删除角色
     */
    @DeleteMapping("/characters/{id}")
    public ApiResponse<Void> deleteCharacter(@PathVariable Long id) {
        try {
            characterService.deleteCharacter(id);
            return ApiResponse.success("角色删除成功", null);
        } catch (Exception e) {
            logger.error("删除角色失败", e);
            return ApiResponse.error("删除角色失败: " + e.getMessage());
        }
    }

    // ==================== AI生成接口 ====================

    /**
     * 生成正文内容（核心接口）
     */
    @PostMapping("/generate")
    public ApiResponse<AIGeneratedContent> generateContent(@RequestBody GenerateRequest request) {
        try {
            logger.info("收到生成请求: {}", request);
            AIGeneratedContent content = aiService.generateContent(request);
            return ApiResponse.success("内容生成成功", content);
        } catch (Exception e) {
            logger.error("生成内容失败", e);
            return ApiResponse.error("生成内容失败: " + e.getMessage());
        }
    }

    /**
     * 流式生成正文内容（SSE）
     */
    @PostMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateContentStream(@RequestBody GenerateRequest request) {
        logger.info("收到流式生成请求: {}", request);
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时
        
        // 异步执行流式生成
        new Thread(() -> {
            try {
                aiService.generateContentStream(request, emitter);
            } catch (Exception e) {
                logger.error("流式生成内容失败", e);
                try {
                    emitter.send(SseEmitter.event().data("[ERROR] " + e.getMessage()));
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        }).start();
        
        return emitter;
    }

    /**
     * 重新生成章节内容
     */
    @PostMapping("/chapters/{id}/regenerate")
    public ApiResponse<AIGeneratedContent> regenerateContent(@PathVariable Long id, @RequestBody GenerateRequest request) {
        try {
            AIGeneratedContent content = aiService.regenerateContent(id, request);
            return ApiResponse.success("内容重新生成成功", content);
        } catch (Exception e) {
            logger.error("重新生成内容失败", e);
            return ApiResponse.error("重新生成内容失败: " + e.getMessage());
        }
    }

    /**
     * 生成章节概括
     */
    @PostMapping("/chapters/{id}/summary")
    public ApiResponse<String> generateSummary(
            @PathVariable Long id,
            @RequestParam(required = false) Long templateId,
            @RequestParam(required = false) Long configId) {
        try {
            String summary = aiService.generateSummary(id, templateId, configId);
            return ApiResponse.success("概括生成成功", summary);
        } catch (Exception e) {
            logger.error("生成概括失败", e);
            return ApiResponse.error("生成概括失败: " + e.getMessage());
        }
    }

    /**
     * 流式生成章节概括（SSE）
     */
    @PostMapping(value = "/chapters/{id}/summary/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateSummaryStream(
            @PathVariable Long id,
            @RequestParam(required = false) Long templateId,
            @RequestParam(required = false) Long configId) {
        logger.info("收到流式生成章节概括请求，章节ID: {}", id);
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时
        
        // 异步执行流式生成
        new Thread(() -> {
            try {
                aiService.generateSummaryStream(id, templateId, configId, emitter);
            } catch (Exception e) {
                logger.error("流式生成概括失败", e);
                try {
                    emitter.send(SseEmitter.event().data("[ERROR] " + e.getMessage()));
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        }).start();
        
        return emitter;
    }

    /**
     * 生成章节汇总
     * @param templateId 模板ID（可选）
     * @param configId 配置ID（可选）
     * @param chapterIds 选中的章节ID列表，逗号分隔（可选，为空则汇总所有章节）
     */
    @PostMapping("/collections/generate")
    public ApiResponse<SummaryCollection> generateCollection(
            @RequestParam(required = false) Long templateId,
            @RequestParam(required = false) Long configId,
            @RequestParam(required = false) String chapterIds) {
        try {
            String collectionContent;
            String chapterRange;
            
            if (chapterIds != null && !chapterIds.isEmpty()) {
                // 解析章节ID列表
                java.util.List<Long> idList = new java.util.ArrayList<>();
                for (String idStr : chapterIds.split(",")) {
                    try {
                        idList.add(Long.parseLong(idStr.trim()));
                    } catch (NumberFormatException e) {
                        logger.warn("无效的章节ID: {}", idStr);
                    }
                }
                if (idList.isEmpty()) {
                    return ApiResponse.error("没有有效的章节ID");
                }
                collectionContent = aiService.generateCollectionByChapterIds(idList, templateId, configId);
                chapterRange = "已选择 " + idList.size() + " 章";
            } else {
                // 汇总所有章节
                collectionContent = aiService.generateCollectionByChapterIds(null, templateId, configId);
                chapterRange = "全部章节";
            }
            
            SummaryCollection collection = summaryCollectionService.createCollection(
                "剧情汇总", collectionContent, chapterRange
            );
            return ApiResponse.success("汇总生成成功", collection);
        } catch (Exception e) {
            logger.error("生成汇总失败", e);
            return ApiResponse.error("生成汇总失败: " + e.getMessage());
        }
    }

    /**
     * 流式生成章节汇总（SSE）
     */
    @PostMapping(value = "/collections/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateCollectionStream(
            @RequestParam(required = false) Long templateId,
            @RequestParam(required = false) Long configId,
            @RequestParam(required = false) String chapterIds) {
        logger.info("收到流式生成章节汇总请求");
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时
        
        // 解析章节ID列表
        java.util.List<Long> idList = new java.util.ArrayList<>();
        if (chapterIds != null && !chapterIds.isEmpty()) {
            for (String idStr : chapterIds.split(",")) {
                try {
                    idList.add(Long.parseLong(idStr.trim()));
                } catch (NumberFormatException e) {
                    logger.warn("无效的章节ID: {}", idStr);
                }
            }
        }
        
        // 异步执行流式生成
        new Thread(() -> {
            try {
                aiService.generateCollectionStream(idList.isEmpty() ? null : idList, templateId, configId, emitter);
            } catch (Exception e) {
                logger.error("流式生成汇总失败", e);
                try {
                    emitter.send(SseEmitter.event().data("[ERROR] " + e.getMessage()));
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        }).start();
        
        return emitter;
    }

    /**
     * AI生成角色信息
     */
    @PostMapping("/characters/{id}/generate")
    public ApiResponse<String> generateCharacterInfo(
            @PathVariable Long id,
            @RequestParam(required = false) String userPrompt,
            @RequestParam(required = false) Long templateId,
            @RequestParam(required = false) Long configId) {
        try {
            String result = aiService.generateCharacterInfo(id, userPrompt, templateId, configId);
            return ApiResponse.success("角色信息生成成功", result);
        } catch (Exception e) {
            logger.error("生成角色信息失败", e);
            return ApiResponse.error("生成角色信息失败: " + e.getMessage());
        }
    }

    /**
     * AI生成新角色
     */
    @PostMapping("/characters/generate")
    public ApiResponse<String> generateNewCharacter(
            @RequestParam String userPrompt,
            @RequestParam(required = false) Long templateId,
            @RequestParam(required = false) Long configId) {
        try {
            String result = aiService.generateCharacterInfo(null, userPrompt, templateId, configId);
            return ApiResponse.success("角色信息生成成功", result);
        } catch (Exception e) {
            logger.error("生成角色信息失败", e);
            return ApiResponse.error("生成角色信息失败: " + e.getMessage());
        }
    }

    /**
     * 流式AI生成新角色（SSE）
     */
    @PostMapping(value = "/characters/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateNewCharacterStream(
            @RequestParam String userPrompt,
            @RequestParam(required = false) Long templateId,
            @RequestParam(required = false) Long configId) {
        logger.info("收到流式生成新角色请求");
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时
        
        // 异步执行流式生成
        new Thread(() -> {
            try {
                aiService.generateCharacterInfoStream(null, userPrompt, templateId, configId, emitter);
            } catch (Exception e) {
                logger.error("流式生成新角色失败", e);
                try {
                    emitter.send(SseEmitter.event().data("[ERROR] " + e.getMessage()));
                    emitter.complete();
                } catch (Exception ex) {
                    emitter.completeWithError(ex);
                }
            }
        }).start();
        
        return emitter;
    }

    /**
     * 解析内容中的角色信息标记（【@ 内容 @】格式）
     * 流式输出完成后调用此接口解析并更新角色信息
     */
    @PostMapping("/parse-character-info")
    public ApiResponse<Void> parseCharacterInfo(@RequestBody Map<String, String> request) {
        try {
            String content = request.get("content");
            if (content != null && !content.isEmpty()) {
                aiService.parseAndUpdateCharacterInfo(content);
                return ApiResponse.success("角色信息解析完成", null);
            }
            return ApiResponse.success("无内容需要解析", null);
        } catch (Exception e) {
            logger.error("解析角色信息失败", e);
            return ApiResponse.error("解析角色信息失败: " + e.getMessage());
        }
    }

    /**
     * 测试AI连接
     */
    @PostMapping("/ai/test")
    public ApiResponse<String> testAIConnection(@RequestParam(required = false) Long configId) {
        try {
            String result = aiService.testConnection(configId);
            return ApiResponse.success(result);
        } catch (Exception e) {
            logger.error("测试连接失败", e);
            return ApiResponse.error("测试连接失败: " + e.getMessage());
        }
    }

    // ==================== 提示词模板接口 ====================

    /**
     * 获取所有模板
     */
    @GetMapping("/templates")
    public ApiResponse<List<PromptTemplate>> getAllTemplates() {
        return ApiResponse.success(promptTemplateService.getAllTemplates());
    }

    /**
     * 根据类型获取模板
     */
    @GetMapping("/templates/type/{type}")
    public ApiResponse<List<PromptTemplate>> getTemplatesByType(@PathVariable String type) {
        return ApiResponse.success(promptTemplateService.getTemplatesByType(type));
    }

    /**
     * 根据ID获取模板
     */
    @GetMapping("/templates/{id}")
    public ApiResponse<PromptTemplate> getTemplate(@PathVariable Long id) {
        Optional<PromptTemplate> template = promptTemplateService.getTemplateById(id);
        return template.map(ApiResponse::success)
                .orElse(ApiResponse.notFound("模板不存在"));
    }

    /**
     * 创建模板
     */
    @PostMapping("/templates")
    public ApiResponse<PromptTemplate> createTemplate(@RequestBody PromptTemplate template) {
        try {
            PromptTemplate created = promptTemplateService.createTemplate(template);
            return ApiResponse.success("模板创建成功", created);
        } catch (Exception e) {
            logger.error("创建模板失败", e);
            return ApiResponse.error("创建模板失败: " + e.getMessage());
        }
    }

    /**
     * 更新模板
     */
    @PutMapping("/templates/{id}")
    public ApiResponse<PromptTemplate> updateTemplate(@PathVariable Long id, @RequestBody PromptTemplate template) {
        try {
            PromptTemplate updated = promptTemplateService.updateTemplate(id, template);
            return ApiResponse.success("模板更新成功", updated);
        } catch (Exception e) {
            logger.error("更新模板失败", e);
            return ApiResponse.error("更新模板失败: " + e.getMessage());
        }
    }

    /**
     * 删除模板
     */
    @DeleteMapping("/templates/{id}")
    public ApiResponse<Void> deleteTemplate(@PathVariable Long id) {
        try {
            promptTemplateService.deleteTemplate(id);
            return ApiResponse.success("模板删除成功", null);
        } catch (Exception e) {
            logger.error("删除模板失败", e);
            return ApiResponse.error("删除模板失败: " + e.getMessage());
        }
    }

    /**
     * 设置默认模板
     */
    @PostMapping("/templates/{id}/default")
    public ApiResponse<PromptTemplate> setDefaultTemplate(@PathVariable Long id) {
        try {
            PromptTemplate template = promptTemplateService.setAsDefault(id);
            return ApiResponse.success("设置默认模板成功", template);
        } catch (Exception e) {
            logger.error("设置默认模板失败", e);
            return ApiResponse.error("设置默认模板失败: " + e.getMessage());
        }
    }

    // ==================== AI配置接口 ====================

    /**
     * 获取所有配置
     */
    @GetMapping("/configs")
    public ApiResponse<List<AIConfig>> getAllConfigs() {
        return ApiResponse.success(aiConfigService.getAllConfigs());
    }

    /**
     * 根据ID获取配置
     */
    @GetMapping("/configs/{id}")
    public ApiResponse<AIConfig> getConfig(@PathVariable Long id) {
        Optional<AIConfig> config = aiConfigService.getConfigById(id);
        return config.map(ApiResponse::success)
                .orElse(ApiResponse.notFound("配置不存在"));
    }

    /**
     * 创建配置
     */
    @PostMapping("/configs")
    public ApiResponse<AIConfig> createConfig(@RequestBody AIConfig config) {
        try {
            AIConfig created = aiConfigService.createConfig(config);
            return ApiResponse.success("配置创建成功", created);
        } catch (Exception e) {
            logger.error("创建配置失败", e);
            return ApiResponse.error("创建配置失败: " + e.getMessage());
        }
    }

    /**
     * 更新配置
     */
    @PutMapping("/configs/{id}")
    public ApiResponse<AIConfig> updateConfig(@PathVariable Long id, @RequestBody AIConfig config) {
        try {
            AIConfig updated = aiConfigService.updateConfig(id, config);
            return ApiResponse.success("配置更新成功", updated);
        } catch (Exception e) {
            logger.error("更新配置失败", e);
            return ApiResponse.error("更新配置失败: " + e.getMessage());
        }
    }

    /**
     * 删除配置
     */
    @DeleteMapping("/configs/{id}")
    public ApiResponse<Void> deleteConfig(@PathVariable Long id) {
        try {
            aiConfigService.deleteConfig(id);
            return ApiResponse.success("配置删除成功", null);
        } catch (Exception e) {
            logger.error("删除配置失败", e);
            return ApiResponse.error("删除配置失败: " + e.getMessage());
        }
    }

    /**
     * 设置默认配置
     */
    @PostMapping("/configs/{id}/default")
    public ApiResponse<AIConfig> setDefaultConfig(@PathVariable Long id) {
        try {
            AIConfig config = aiConfigService.setAsDefault(id);
            return ApiResponse.success("设置默认配置成功", config);
        } catch (Exception e) {
            logger.error("设置默认配置失败", e);
            return ApiResponse.error("设置默认配置失败: " + e.getMessage());
        }
    }

    // ==================== 章节汇总接口 ====================

    /**
     * 获取所有汇总
     */
    @GetMapping("/collections")
    public ApiResponse<List<SummaryCollection>> getAllCollections() {
        return ApiResponse.success(summaryCollectionService.getAllCollections());
    }

    /**
     * 根据ID获取汇总
     */
    @GetMapping("/collections/{id}")
    public ApiResponse<SummaryCollection> getCollection(@PathVariable Long id) {
        Optional<SummaryCollection> collection = summaryCollectionService.getCollectionById(id);
        return collection.map(ApiResponse::success)
                .orElse(ApiResponse.notFound("汇总不存在"));
    }

    /**
     * 创建汇总
     */
    @PostMapping("/collections")
    public ApiResponse<SummaryCollection> createCollection(@RequestBody SummaryCollection collection) {
        try {
            SummaryCollection created = summaryCollectionService.createCollection(collection);
            return ApiResponse.success("汇总创建成功", created);
        } catch (Exception e) {
            logger.error("创建汇总失败", e);
            return ApiResponse.error("创建汇总失败: " + e.getMessage());
        }
    }

    /**
     * 更新汇总
     */
    @PutMapping("/collections/{id}")
    public ApiResponse<SummaryCollection> updateCollection(@PathVariable Long id, @RequestBody SummaryCollection collection) {
        try {
            SummaryCollection updated = summaryCollectionService.updateCollection(id, collection);
            return ApiResponse.success("汇总更新成功", updated);
        } catch (Exception e) {
            logger.error("更新汇总失败", e);
            return ApiResponse.error("更新汇总失败: " + e.getMessage());
        }
    }

    /**
     * 删除汇总
     */
    @DeleteMapping("/collections/{id}")
    public ApiResponse<Void> deleteCollection(@PathVariable Long id) {
        try {
            summaryCollectionService.deleteCollection(id);
            return ApiResponse.success("汇总删除成功", null);
        } catch (Exception e) {
            logger.error("删除汇总失败", e);
            return ApiResponse.error("删除汇总失败: " + e.getMessage());
        }
    }

    // ==================== 书籍管理接口 ====================

    /**
     * 获取所有书籍
     */
    @GetMapping("/books")
    public ApiResponse<List<Book>> getAllBooks() {
        return ApiResponse.success(bookService.findAll());
    }

    /**
     * 根据ID获取书籍
     */
    @GetMapping("/books/{id}")
    public ApiResponse<Book> getBook(@PathVariable Long id) {
        Optional<Book> book = bookService.findById(id);
        return book.map(ApiResponse::success)
                .orElse(ApiResponse.notFound("书籍不存在"));
    }

    /**
     * 获取当前激活的书籍
     */
    @GetMapping("/books/active")
    public ApiResponse<Book> getActiveBook() {
        Optional<Book> book = bookService.getActiveBook();
        return book.map(ApiResponse::success)
                .orElse(ApiResponse.notFound("暂无激活的书籍"));
    }

    /**
     * 创建书籍
     */
    @PostMapping("/books")
    public ApiResponse<Book> createBook(@RequestBody Book book) {
        try {
            Book created = bookService.create(book);
            return ApiResponse.success("书籍创建成功", created);
        } catch (Exception e) {
            logger.error("创建书籍失败", e);
            return ApiResponse.error("创建书籍失败: " + e.getMessage());
        }
    }

    /**
     * 更新书籍
     */
    @PutMapping("/books/{id}")
    public ApiResponse<Book> updateBook(@PathVariable Long id, @RequestBody Book book) {
        try {
            Book updated = bookService.update(id, book);
            return ApiResponse.success("书籍更新成功", updated);
        } catch (Exception e) {
            logger.error("更新书籍失败", e);
            return ApiResponse.error("更新书籍失败: " + e.getMessage());
        }
    }

    /**
     * 删除书籍
     */
    @DeleteMapping("/books/{id}")
    public ApiResponse<Void> deleteBook(@PathVariable Long id) {
        try {
            bookService.delete(id);
            return ApiResponse.success("书籍删除成功", null);
        } catch (Exception e) {
            logger.error("删除书籍失败", e);
            return ApiResponse.error("删除书籍失败: " + e.getMessage());
        }
    }

    /**
     * 切换到指定书籍
     */
    @PostMapping("/books/{id}/switch")
    public ApiResponse<Book> switchBook(@PathVariable Long id) {
        try {
            Book book = bookService.switchTo(id);
            return ApiResponse.success("已切换到书籍: " + book.getName(), book);
        } catch (Exception e) {
            logger.error("切换书籍失败", e);
            return ApiResponse.error("切换书籍失败: " + e.getMessage());
        }
    }

    // ==================== 世界书管理接口 ====================

    /**
     * 获取指定书籍的世界书列表
     * @param bookId 书籍ID（可选，不传则返回当前激活书籍的世界书）
     */
    @GetMapping("/worldbooks")
    public ApiResponse<List<WorldBook>> getWorldBooks(@RequestParam(required = false) Long bookId) {
        // 如果没传bookId，使用当前激活的书籍
        if (bookId == null) {
            Optional<Book> activeBook = bookService.getActiveBook();
            if (activeBook.isPresent()) {
                bookId = activeBook.get().getId();
            }
        }
        if (bookId != null) {
            return ApiResponse.success(worldBookService.getWorldBooksByBookId(bookId));
        }
        return ApiResponse.success(java.util.Collections.emptyList());
    }

    /**
     * 根据ID获取世界书
     */
    @GetMapping("/worldbooks/{id}")
    public ApiResponse<WorldBook> getWorldBook(@PathVariable Long id) {
        Optional<WorldBook> worldBook = worldBookService.getWorldBookById(id);
        return worldBook.map(ApiResponse::success)
                .orElse(ApiResponse.notFound("世界书不存在"));
    }

    /**
     * 创建世界书
     */
    @PostMapping("/worldbooks")
    public ApiResponse<WorldBook> createWorldBook(@RequestBody WorldBook worldBook) {
        try {
            // 如果没有指定书籍ID，使用当前激活的书籍
            if (worldBook.getBookId() == null) {
                Optional<Book> activeBook = bookService.getActiveBook();
                if (activeBook.isPresent()) {
                    worldBook.setBookId(activeBook.get().getId());
                } else {
                    return ApiResponse.error("请先选择或创建一本书籍");
                }
            }
            WorldBook created = worldBookService.createWorldBook(worldBook);
            return ApiResponse.success("世界书创建成功", created);
        } catch (Exception e) {
            logger.error("创建世界书失败", e);
            return ApiResponse.error("创建世界书失败: " + e.getMessage());
        }
    }

    /**
     * 更新世界书
     */
    @PutMapping("/worldbooks/{id}")
    public ApiResponse<WorldBook> updateWorldBook(@PathVariable Long id, @RequestBody WorldBook worldBook) {
        try {
            WorldBook updated = worldBookService.updateWorldBook(id, worldBook);
            return ApiResponse.success("世界书更新成功", updated);
        } catch (Exception e) {
            logger.error("更新世界书失败", e);
            return ApiResponse.error("更新世界书失败: " + e.getMessage());
        }
    }

    /**
     * 删除世界书
     */
    @DeleteMapping("/worldbooks/{id}")
    public ApiResponse<Void> deleteWorldBook(@PathVariable Long id) {
        try {
            worldBookService.deleteWorldBook(id);
            return ApiResponse.success("世界书删除成功", null);
        } catch (Exception e) {
            logger.error("删除世界书失败", e);
            return ApiResponse.error("删除世界书失败: " + e.getMessage());
        }
    }
}
