package com.novel.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 章节汇总实体类
 * 存储多个章节概括的汇总信息
 * 
 * @author NovelCreator
 */
@Entity
@Table(name = "summary_collections", indexes = {
    @Index(name = "idx_collection_order", columnList = "collectionOrder"),
    @Index(name = "idx_collection_create_time", columnList = "createTime"),
    @Index(name = "idx_collection_book", columnList = "bookId")
})
public class SummaryCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 所属书籍ID
     */
    @Column(nullable = false)
    private Long bookId;

    /**
     * 汇总标题
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * 汇总内容
     */
    @Column(columnDefinition = "CLOB", nullable = false)
    private String content;

    /**
     * 汇总的起始章节ID
     */
    @Column
    private Long startChapterId;

    /**
     * 汇总的结束章节ID
     */
    @Column
    private Long endChapterId;

    /**
     * 汇总的章节范围描述
     */
    @Column(length = 200)
    private String chapterRange;

    /**
     * 汇总序号，用于排序
     */
    @Column(nullable = false)
    private Integer collectionOrder;

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
    public SummaryCollection() {
        this.collectionOrder = 0;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 带参数构造函数
     */
    public SummaryCollection(String title, String content, Integer collectionOrder) {
        this();
        this.title = title;
        this.content = content;
        this.collectionOrder = collectionOrder;
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
    }

    public Long getStartChapterId() {
        return startChapterId;
    }

    public void setStartChapterId(Long startChapterId) {
        this.startChapterId = startChapterId;
    }

    public Long getEndChapterId() {
        return endChapterId;
    }

    public void setEndChapterId(Long endChapterId) {
        this.endChapterId = endChapterId;
    }

    public String getChapterRange() {
        return chapterRange;
    }

    public void setChapterRange(String chapterRange) {
        this.chapterRange = chapterRange;
    }

    public Integer getCollectionOrder() {
        return collectionOrder;
    }

    public void setCollectionOrder(Integer collectionOrder) {
        this.collectionOrder = collectionOrder;
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
        return "SummaryCollection{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", chapterRange='" + chapterRange + '\'' +
                ", collectionOrder=" + collectionOrder +
                '}';
    }
}
