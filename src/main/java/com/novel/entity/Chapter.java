package com.novel.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 章节实体类
 * 存储小说章节的标题、正文内容、章节概括等信息
 * 
 * @author NovelCreator
 */
@Entity
@Table(name = "chapters", indexes = {
    @Index(name = "idx_chapter_order", columnList = "chapterOrder"),
    @Index(name = "idx_create_time", columnList = "createTime"),
    @Index(name = "idx_book_id", columnList = "bookId")
})
public class Chapter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 所属书籍ID
     */
    @Column(nullable = false)
    private Long bookId;

    /**
     * 章节标题
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * 章节正文内容
     */
    @Column(columnDefinition = "CLOB")
    private String content;

    /**
     * 章节概括/总结
     */
    @Column(columnDefinition = "CLOB")
    private String summary;

    /**
     * 章节序号，用于排序
     */
    @Column(nullable = false)
    private Integer chapterOrder;

    /**
     * 字数统计
     */
    @Column(nullable = false)
    private Integer wordCount;

    /**
     * 用户提示词（生成该章节时使用的提示词）
     */
    @Column(columnDefinition = "CLOB")
    private String userPrompt;

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
    public Chapter() {
        this.wordCount = 0;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 带参数构造函数
     */
    public Chapter(String title, Integer chapterOrder) {
        this();
        this.title = title;
        this.chapterOrder = chapterOrder;
    }

    // ==================== Getters and Setters ====================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        // 自动计算字数
        if (content != null) {
            this.wordCount = content.replaceAll("\\s+", "").length();
        } else {
            this.wordCount = 0;
        }
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Integer getChapterOrder() {
        return chapterOrder;
    }

    public void setChapterOrder(Integer chapterOrder) {
        this.chapterOrder = chapterOrder;
    }

    public Integer getWordCount() {
        return wordCount;
    }

    public void setWordCount(Integer wordCount) {
        this.wordCount = wordCount;
    }

    public String getUserPrompt() {
        return userPrompt;
    }

    public void setUserPrompt(String userPrompt) {
        this.userPrompt = userPrompt;
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
        return "Chapter{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", chapterOrder=" + chapterOrder +
                ", wordCount=" + wordCount +
                '}';
    }
}
