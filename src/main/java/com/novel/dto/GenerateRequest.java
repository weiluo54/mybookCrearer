package com.novel.dto;

import java.util.List;

/**
 * AI生成请求DTO
 * 用于接收前端的生成请求参数
 * 
 * @author NovelCreator
 */
public class GenerateRequest {

    /**
     * 当前章节ID（如果是更新现有章节）
     */
    private Long chapterId;

    /**
     * 所属书籍ID
     */
    private Long bookId;

    /**
     * 用户提示词
     */
    private String userPrompt;

    /**
     * 使用的提示词模板ID
     */
    private Long templateId;

    /**
     * 使用的AI配置ID
     */
    private Long configId;

    /**
     * 是否生成新章节
     */
    private Boolean createNewChapter;

    /**
     * 新章节标题（如果创建新章节）
     */
    private String newChapterTitle;

    /**
     * 选中的角色ID列表（用于组装提示词）
     */
    private List<Long> characterIds;

    /**
     * 是否包含历史章节概括
     */
    private Boolean includeHistory;

    /**
     * 包含的历史章节数量
     */
    private Integer historyCount;

    /**
     * 自定义系统提示词（覆盖模板中的系统提示词）
     */
    private String customSystemPrompt;

    /**
     * 生成类型：CONTENT(正文)、SUMMARY(概括)、COLLECTION(汇总)
     */
    private String generateType;

    /**
     * 前一章节的正文内容（用于生成新章节时提供上下文）
     */
    private String previousChapterContent;

    /**
     * 无参构造函数
     */
    public GenerateRequest() {
        this.createNewChapter = false;
        this.includeHistory = true;
        this.historyCount = 5;
        this.generateType = "CONTENT";
    }

    // ==================== Getters and Setters ====================

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getUserPrompt() {
        return userPrompt;
    }

    public void setUserPrompt(String userPrompt) {
        this.userPrompt = userPrompt;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public Long getConfigId() {
        return configId;
    }

    public void setConfigId(Long configId) {
        this.configId = configId;
    }

    public Boolean getCreateNewChapter() {
        return createNewChapter;
    }

    public void setCreateNewChapter(Boolean createNewChapter) {
        this.createNewChapter = createNewChapter;
    }

    public String getNewChapterTitle() {
        return newChapterTitle;
    }

    public void setNewChapterTitle(String newChapterTitle) {
        this.newChapterTitle = newChapterTitle;
    }

    public List<Long> getCharacterIds() {
        return characterIds;
    }

    public void setCharacterIds(List<Long> characterIds) {
        this.characterIds = characterIds;
    }

    public Boolean getIncludeHistory() {
        return includeHistory;
    }

    public void setIncludeHistory(Boolean includeHistory) {
        this.includeHistory = includeHistory;
    }

    public Integer getHistoryCount() {
        return historyCount;
    }

    public void setHistoryCount(Integer historyCount) {
        this.historyCount = historyCount;
    }

    public String getCustomSystemPrompt() {
        return customSystemPrompt;
    }

    public void setCustomSystemPrompt(String customSystemPrompt) {
        this.customSystemPrompt = customSystemPrompt;
    }

    public String getGenerateType() {
        return generateType;
    }

    public void setGenerateType(String generateType) {
        this.generateType = generateType;
    }

    public String getPreviousChapterContent() {
        return previousChapterContent;
    }

    public void setPreviousChapterContent(String previousChapterContent) {
        this.previousChapterContent = previousChapterContent;
    }

    @Override
    public String toString() {
        return "GenerateRequest{" +
                "chapterId=" + chapterId +
                ", generateType='" + generateType + '\'' +
                ", createNewChapter=" + createNewChapter +
                '}';
    }
}
