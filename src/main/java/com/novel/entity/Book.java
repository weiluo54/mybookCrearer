package com.novel.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 书籍实体类
 * 支持多本小说管理
 */
@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 书籍名称
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * 书籍描述/简介
     */
    @Column(columnDefinition = "CLOB")
    private String description;

    /**
     * 作者名称
     */
    @Column(length = 100)
    private String author;

    /**
     * 书籍类型/分类
     */
    @Column(length = 50)
    private String genre;

    /**
     * 是否为当前激活的书籍
     */
    @Column(nullable = false)
    private Boolean isActive = false;

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

    public Book() {
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
        this.isActive = false;
    }

    public Book(String name) {
        this();
        this.name = name;
    }

    // Getters and Setters
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
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
}
