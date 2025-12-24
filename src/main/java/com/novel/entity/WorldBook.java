package com.novel.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 世界书实体类
 * 存储世界观设定、地点、物品、规则等世界书信息
 * 
 * @author NovelCreator
 */
@Entity
@Table(name = "world_books", indexes = {
    @Index(name = "idx_worldbook_book", columnList = "bookId"),
    @Index(name = "idx_worldbook_title", columnList = "title")
})
public class WorldBook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 所属书籍ID
     */
    @Column(nullable = false)
    private Long bookId;

    /**
     * 世界书标题/关键词（用于匹配）
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * 世界书内容
     */
    @Column(columnDefinition = "CLOB")
    private String content;

    /**
     * 排序顺序
     */
    @Column(nullable = false)
    private Integer sortOrder;

    /**
     * 是否启用
     */
    @Column(nullable = false)
    private Boolean isActive;

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
    public WorldBook() {
        this.isActive = true;
        this.sortOrder = 0;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
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

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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
        return "WorldBook{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", bookId=" + bookId +
                '}';
    }
}
