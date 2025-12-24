package com.novel.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 角色实体类
 * 存储小说角色的基本信息：姓名、外貌、性格、背景等
 * 
 * @author NovelCreator
 */
@Entity
@Table(name = "characters", indexes = {
    @Index(name = "idx_character_name", columnList = "name"),
    @Index(name = "idx_character_chapter", columnList = "chapterId"),
    @Index(name = "idx_character_book", columnList = "bookId")
})
public class Character {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 所属书籍ID
     */
    @Column(nullable = false)
    private Long bookId;

    /**
     * 角色姓名
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 角色信息（包含外貌、性格、背景、能力等所有信息）
     */
    @Column(columnDefinition = "CLOB")
    private String info;

    /**
     * 角色首次出现的章节ID
     */
    @Column
    private Long chapterId;

    /**
     * 是否为主要角色
     */
    @Column(nullable = false)
    private Boolean isMain;

    /**
     * 角色关系（如：主角的母亲、反派的助手等）
     */
    @Column(length = 200)
    private String roleRelation;

    /**
     * 角色类型（如：主角、配角、反派等）
     */
    @Column(length = 100)
    private String role;

    /**
     * 角色性别（男/女/未知）
     */
    @Column(length = 20)
    private String gender;

    /**
     * 角色代号/别名
     */
    @Column(length = 100)
    private String codeName;

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
    public Character() {
        this.isMain = false;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 带参数构造函数
     */
    public Character(String name) {
        this();
        this.name = name;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    public Boolean getIsMain() {
        return isMain;
    }

    public void setIsMain(Boolean isMain) {
        this.isMain = isMain;
    }

    public String getRoleRelation() {
        return roleRelation;
    }

    public void setRoleRelation(String roleRelation) {
        this.roleRelation = roleRelation;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getCodeName() {
        return codeName;
    }

    public void setCodeName(String codeName) {
        this.codeName = codeName;
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

    /**
     * 获取角色信息的完整描述，用于组装AI提示词
     * @return 角色完整描述字符串
     */
    public String getFullDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(name).append("】\n");
        if (info != null && !info.isEmpty()) {
            sb.append(info).append("\n");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Character{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", isMain=" + isMain +
                '}';
    }
}
