package com.novel.dto;

import java.util.List;

/**
 * AI生成内容DTO
 * 用于解析AI返回的JSON格式内容
 * 
 * @author NovelCreator
 */
public class AIGeneratedContent {

    /**
     * 正文内容
     */
    private String content;

    /**
     * 章节标题
     */
    private String title;

    /**
     * 角色信息列表
     */
    private List<RoleInfo> role;

    /**
     * 章节概括
     */
    private String chapter;

    /**
     * 无参构造函数
     */
    public AIGeneratedContent() {
    }

    // ==================== Getters and Setters ====================

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<RoleInfo> getRole() {
        return role;
    }

    public void setRole(List<RoleInfo> role) {
        this.role = role;
    }

    public String getChapter() {
        return chapter;
    }

    public void setChapter(String chapter) {
        this.chapter = chapter;
    }

    /**
     * 角色信息内部类
     */
    public static class RoleInfo {
        /**
         * 角色名称
         */
        private String name;

        /**
         * 角色外貌
         */
        private String appearance;

        /**
         * 角色性格
         */
        private String personality;

        /**
         * 角色背景
         */
        private String background;

        /**
         * 角色定位
         */
        private String role;

        /**
         * 角色备注
         */
        private String notes;

        /**
         * 无参构造函数
         */
        public RoleInfo() {
        }

        // ==================== Getters and Setters ====================

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAppearance() {
            return appearance;
        }

        public void setAppearance(String appearance) {
            this.appearance = appearance;
        }

        public String getPersonality() {
            return personality;
        }

        public void setPersonality(String personality) {
            this.personality = personality;
        }

        public String getBackground() {
            return background;
        }

        public void setBackground(String background) {
            this.background = background;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }

        @Override
        public String toString() {
            return "RoleInfo{" +
                    "name='" + name + '\'' +
                    ", role='" + role + '\'' +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "AIGeneratedContent{" +
                "title='" + title + '\'' +
                ", contentLength=" + (content != null ? content.length() : 0) +
                ", roleCount=" + (role != null ? role.size() : 0) +
                '}';
    }
}
