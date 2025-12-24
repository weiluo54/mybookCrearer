package com.novel.service;

import com.novel.entity.SummaryCollection;
import com.novel.repository.SummaryCollectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 章节汇总服务层
 * 处理章节汇总的CRUD操作和业务逻辑
 * 
 * @author NovelCreator
 */
@Service
public class SummaryCollectionService {

    private final SummaryCollectionRepository summaryCollectionRepository;

    /**
     * 构造函数注入
     * @param summaryCollectionRepository 章节汇总数据访问层
     */
    public SummaryCollectionService(SummaryCollectionRepository summaryCollectionRepository) {
        this.summaryCollectionRepository = summaryCollectionRepository;
    }

    /**
     * 获取所有汇总（按序号排序）
     * @return 汇总列表
     */
    public List<SummaryCollection> getAllCollections() {
        return summaryCollectionRepository.findAllByOrderByCollectionOrderAsc();
    }

    /**
     * 根据ID获取汇总
     * @param id 汇总ID
     * @return 汇总
     */
    public Optional<SummaryCollection> getCollectionById(Long id) {
        return summaryCollectionRepository.findById(id);
    }

    /**
     * 创建汇总
     * @param collection 汇总信息
     * @return 保存后的汇总
     */
    @Transactional
    public SummaryCollection createCollection(SummaryCollection collection) {
        // 自动设置序号
        if (collection.getCollectionOrder() == null || collection.getCollectionOrder() == 0) {
            Integer maxOrder = summaryCollectionRepository.findMaxCollectionOrder();
            collection.setCollectionOrder(maxOrder + 1);
        }
        collection.setCreateTime(LocalDateTime.now());
        collection.setUpdateTime(LocalDateTime.now());
        return summaryCollectionRepository.save(collection);
    }

    /**
     * 创建汇总（简化版）
     * @param title 汇总标题
     * @param content 汇总内容
     * @param chapterRange 章节范围描述
     * @return 保存后的汇总
     */
    @Transactional
    public SummaryCollection createCollection(String title, String content, String chapterRange) {
        SummaryCollection collection = new SummaryCollection();
        collection.setTitle(title);
        collection.setContent(content);
        collection.setChapterRange(chapterRange);
        Integer maxOrder = summaryCollectionRepository.findMaxCollectionOrder();
        collection.setCollectionOrder(maxOrder + 1);
        collection.setCreateTime(LocalDateTime.now());
        collection.setUpdateTime(LocalDateTime.now());
        return summaryCollectionRepository.save(collection);
    }

    /**
     * 更新汇总
     * @param id 汇总ID
     * @param collection 汇总信息
     * @return 更新后的汇总
     */
    @Transactional
    public SummaryCollection updateCollection(Long id, SummaryCollection collection) {
        Optional<SummaryCollection> existingOpt = summaryCollectionRepository.findById(id);
        if (existingOpt.isPresent()) {
            SummaryCollection existing = existingOpt.get();
            if (collection.getTitle() != null) {
                existing.setTitle(collection.getTitle());
            }
            if (collection.getContent() != null) {
                existing.setContent(collection.getContent());
            }
            if (collection.getChapterRange() != null) {
                existing.setChapterRange(collection.getChapterRange());
            }
            if (collection.getCollectionOrder() != null) {
                existing.setCollectionOrder(collection.getCollectionOrder());
            }
            existing.setUpdateTime(LocalDateTime.now());
            return summaryCollectionRepository.save(existing);
        }
        throw new RuntimeException("汇总不存在，ID: " + id);
    }

    /**
     * 删除汇总
     * @param id 汇总ID
     */
    @Transactional
    public void deleteCollection(Long id) {
        if (summaryCollectionRepository.existsById(id)) {
            summaryCollectionRepository.deleteById(id);
        } else {
            throw new RuntimeException("汇总不存在，ID: " + id);
        }
    }

    /**
     * 获取所有汇总的文本（用于组装提示词）
     * @return 汇总文本
     */
    public String getAllCollectionsText() {
        List<SummaryCollection> collections = summaryCollectionRepository.findAllByOrderByCollectionOrderAsc();
        StringBuilder sb = new StringBuilder();
        sb.append("【历史剧情汇总】\n");
        for (SummaryCollection collection : collections) {
            sb.append("【").append(collection.getTitle()).append("】\n");
            sb.append(collection.getContent()).append("\n\n");
        }
        return sb.toString();
    }

    /**
     * 获取汇总总数
     * @return 汇总数量
     */
    public Long getCollectionCount() {
        return summaryCollectionRepository.getCollectionCount();
    }

    /**
     * 获取指定书籍的汇总数量
     * @param bookId 书籍ID
     * @return 汇总数量
     */
    public Long getCollectionCountByBookId(Long bookId) {
        return summaryCollectionRepository.countByBookId(bookId);
    }

    /**
     * 获取最新的汇总
     * @return 最新汇总
     */
    public SummaryCollection getLatestCollection() {
        return summaryCollectionRepository.findTopByOrderByCreateTimeDesc();
    }
}
