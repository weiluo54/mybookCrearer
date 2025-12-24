package com.novel.repository;

import com.novel.entity.SummaryCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 章节汇总数据访问层
 * 
 * @author NovelCreator
 */
@Repository
public interface SummaryCollectionRepository extends JpaRepository<SummaryCollection, Long> {

    /**
     * 按汇总序号排序获取所有汇总
     * @return 汇总列表
     */
    List<SummaryCollection> findAllByOrderByCollectionOrderAsc();

    /**
     * 根据书籍ID按汇总序号排序获取所有汇总
     * @param bookId 书籍ID
     * @return 汇总列表
     */
    List<SummaryCollection> findByBookIdOrderByCollectionOrderAsc(Long bookId);

    /**
     * 获取最大汇总序号
     * @return 最大序号
     */
    @Query("SELECT COALESCE(MAX(s.collectionOrder), 0) FROM SummaryCollection s")
    Integer findMaxCollectionOrder();

    /**
     * 获取指定书籍的最大汇总序号
     * @param bookId 书籍ID
     * @return 最大序号
     */
    @Query("SELECT COALESCE(MAX(s.collectionOrder), 0) FROM SummaryCollection s WHERE s.bookId = :bookId")
    Integer findMaxCollectionOrderByBookId(Long bookId);

    /**
     * 根据章节范围查找汇总
     * @param startChapterId 起始章节ID
     * @param endChapterId 结束章节ID
     * @return 汇总列表
     */
    List<SummaryCollection> findByStartChapterIdAndEndChapterId(Long startChapterId, Long endChapterId);

    /**
     * 获取汇总总数
     * @return 汇总数量
     */
    @Query("SELECT COUNT(s) FROM SummaryCollection s")
    Long getCollectionCount();

    /**
     * 根据书籍ID获取汇总数量
     * @param bookId 书籍ID
     * @return 汇总数量
     */
    Long countByBookId(Long bookId);

    /**
     * 获取最近的汇总记录
     * @return 最新的汇总
     */
    SummaryCollection findTopByOrderByCreateTimeDesc();
}
