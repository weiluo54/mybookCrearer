package com.novel.service;

import com.novel.entity.Chapter;
import com.novel.repository.ChapterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 章节服务层
 * 处理章节的CRUD操作和业务逻辑
 * 
 * @author NovelCreator
 */
@Service
public class ChapterService {

    private final ChapterRepository chapterRepository;

    /**
     * 构造函数注入
     * @param chapterRepository 章节数据访问层
     */
    public ChapterService(ChapterRepository chapterRepository) {
        this.chapterRepository = chapterRepository;
    }

    /**
     * 获取所有章节（按序号排序）
     * @return 章节列表
     */
    public List<Chapter> getAllChapters() {
        return chapterRepository.findAllByOrderByChapterOrderAsc();
    }

    /**
     * 根据书籍ID获取章节（按序号排序）
     * @param bookId 书籍ID
     * @return 章节列表
     */
    public List<Chapter> getChaptersByBookId(Long bookId) {
        return chapterRepository.findByBookIdOrderByChapterOrderAsc(bookId);
    }

    /**
     * 获取指定书籍的章节数量
     * @param bookId 书籍ID
     * @return 章节数量
     */
    public Long getChapterCountByBookId(Long bookId) {
        return (long) chapterRepository.findByBookIdOrderByChapterOrderAsc(bookId).size();
    }

    /**
     * 获取指定书籍的总字数
     * @param bookId 书籍ID
     * @return 总字数
     */
    public Long getTotalWordCountByBookId(Long bookId) {
        return chapterRepository.getTotalWordCountByBookId(bookId);
    }

    /**
     * 根据ID获取章节
     * @param id 章节ID
     * @return 章节
     */
    public Optional<Chapter> getChapterById(Long id) {
        return chapterRepository.findById(id);
    }

    /**
     * 根据章节序号获取章节
     * @param chapterOrder 章节序号
     * @return 章节
     */
    public Optional<Chapter> getChapterByOrder(Integer chapterOrder) {
        return chapterRepository.findByChapterOrder(chapterOrder);
    }

    /**
     * 创建新章节
     * @param chapter 章节信息
     * @return 保存后的章节
     */
    @Transactional
    public Chapter createChapter(Chapter chapter) {
        if (chapter.getBookId() == null) {
            throw new RuntimeException("创建章节失败：未指定书籍ID");
        }
        // 如果没有指定章节序号，自动设置为该书籍的最大序号+1
        if (chapter.getChapterOrder() == null || chapter.getChapterOrder() == 0) {
            Integer maxOrder = chapterRepository.findMaxChapterOrderByBookId(chapter.getBookId());
            chapter.setChapterOrder(maxOrder + 1);
        }
        chapter.setCreateTime(LocalDateTime.now());
        chapter.setUpdateTime(LocalDateTime.now());
        return chapterRepository.save(chapter);
    }

    /**
     * 创建新章节（指定标题和书籍ID）
     * @param title 章节标题
     * @param bookId 书籍ID
     * @return 保存后的章节
     */
    @Transactional
    public Chapter createChapter(String title, Long bookId) {
        Chapter chapter = new Chapter();
        chapter.setTitle(title);
        chapter.setBookId(bookId);
        Integer maxOrder = chapterRepository.findMaxChapterOrderByBookId(bookId);
        chapter.setChapterOrder(maxOrder + 1);
        return chapterRepository.save(chapter);
    }

    /**
     * 更新章节
     * @param id 章节ID
     * @param chapter 章节信息
     * @return 更新后的章节
     */
    @Transactional
    public Chapter updateChapter(Long id, Chapter chapter) {
        Optional<Chapter> existingOpt = chapterRepository.findById(id);
        if (existingOpt.isPresent()) {
            Chapter existing = existingOpt.get();
            if (chapter.getTitle() != null) {
                existing.setTitle(chapter.getTitle());
            }
            if (chapter.getContent() != null) {
                existing.setContent(chapter.getContent());
            }
            if (chapter.getSummary() != null) {
                existing.setSummary(chapter.getSummary());
            }
            if (chapter.getUserPrompt() != null) {
                existing.setUserPrompt(chapter.getUserPrompt());
            }
            if (chapter.getChapterOrder() != null) {
                existing.setChapterOrder(chapter.getChapterOrder());
            }
            existing.setUpdateTime(LocalDateTime.now());
            return chapterRepository.save(existing);
        }
        throw new RuntimeException("章节不存在，ID: " + id);
    }

    /**
     * 更新章节内容
     * @param id 章节ID
     * @param content 正文内容
     * @return 更新后的章节
     */
    @Transactional
    public Chapter updateChapterContent(Long id, String content) {
        Optional<Chapter> existingOpt = chapterRepository.findById(id);
        if (existingOpt.isPresent()) {
            Chapter existing = existingOpt.get();
            existing.setContent(content);
            existing.setUpdateTime(LocalDateTime.now());
            return chapterRepository.save(existing);
        }
        throw new RuntimeException("章节不存在，ID: " + id);
    }

    /**
     * 更新章节概括
     * @param id 章节ID
     * @param summary 章节概括
     * @return 更新后的章节
     */
    @Transactional
    public Chapter updateChapterSummary(Long id, String summary) {
        Optional<Chapter> existingOpt = chapterRepository.findById(id);
        if (existingOpt.isPresent()) {
            Chapter existing = existingOpt.get();
            existing.setSummary(summary);
            existing.setUpdateTime(LocalDateTime.now());
            return chapterRepository.save(existing);
        }
        throw new RuntimeException("章节不存在，ID: " + id);
    }

    /**
     * 更新章节标题
     * @param id 章节ID
     * @param title 章节标题
     * @return 更新后的章节
     */
    @Transactional
    public Chapter updateChapterTitle(Long id, String title) {
        Optional<Chapter> existingOpt = chapterRepository.findById(id);
        if (existingOpt.isPresent()) {
            Chapter existing = existingOpt.get();
            existing.setTitle(title);
            existing.setUpdateTime(LocalDateTime.now());
            return chapterRepository.save(existing);
        }
        throw new RuntimeException("章节不存在，ID: " + id);
    }

    /**
     * 删除章节
     * @param id 章节ID
     */
    @Transactional
    public void deleteChapter(Long id) {
        if (chapterRepository.existsById(id)) {
            chapterRepository.deleteById(id);
        } else {
            throw new RuntimeException("章节不存在，ID: " + id);
        }
    }

    /**
     * 获取指定范围内的章节概括
     * @param count 数量
     * @return 概括列表字符串
     */
    public String getRecentSummaries(int count,Long bookId) {
        List<Chapter> chapters = chapterRepository.findByBookIdOrderByChapterOrderAsc(bookId);
        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, chapters.size() - count);
        for (int i = start; i < chapters.size(); i++) {
            Chapter chapter = chapters.get(i);
            if (chapter.getSummary() != null && !chapter.getSummary().isEmpty()) {
                sb.append("【第").append(chapter.getChapterOrder()).append("章 ")
                  .append(chapter.getTitle()).append("】\n")
                  .append(chapter.getSummary()).append("\n\n");
            }
        }
        return sb.toString();
    }

    /**
     * 获取所有章节概括
     * @return 概括列表字符串
     */
    public String getAllSummaries() {
        List<Chapter> chapters = chapterRepository.findAllByOrderByChapterOrderAsc();
        StringBuilder sb = new StringBuilder();
        for (Chapter chapter : chapters) {
            if (chapter.getSummary() != null && !chapter.getSummary().isEmpty()) {
                sb.append("【第").append(chapter.getChapterOrder()).append("章 ")
                  .append(chapter.getTitle()).append("】\n")
                  .append(chapter.getSummary()).append("\n\n");
            }
        }
        return sb.toString();
    }

    /**
     * 获取总字数统计
     * @return 总字数
     */
    public Long getTotalWordCount() {
        return chapterRepository.getTotalWordCount();
    }

    /**
     * 获取章节总数
     * @return 章节数量
     */
    public Long getChapterCount() {
        return chapterRepository.getChapterCount();
    }

    /**
     * 搜索章节
     * @param keyword 关键词
     * @return 章节列表
     */
    public List<Chapter> searchChapters(String keyword) {
        return chapterRepository.findByTitleContaining(keyword);
    }

    /**
     * 获取下一个章节序号
     * @return 下一个序号
     */
    public Integer getNextChapterOrder() {
        return chapterRepository.findMaxChapterOrder() + 1;
    }
}
