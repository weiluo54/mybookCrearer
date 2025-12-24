package com.novel.repository;

import com.novel.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 章节数据访问层
 * 
 * @author NovelCreator
 */
@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    /**
     * 按章节序号排序获取所有章节
     * @return 章节列表
     */
    List<Chapter> findAllByOrderByChapterOrderAsc();

    /**
     * 按书籍ID和章节序号排序获取章节
     * @param bookId 书籍ID
     * @return 章节列表
     */
    List<Chapter> findByBookIdOrderByChapterOrderAsc(Long bookId);

    /**
     * 根据章节序号查找章节
     * @param chapterOrder 章节序号
     * @return 章节
     */
    Optional<Chapter> findByChapterOrder(Integer chapterOrder);

    /**
     * 根据书籍ID和章节序号查找章节
     * @param bookId 书籍ID
     * @param chapterOrder 章节序号
     * @return 章节
     */
    Optional<Chapter> findByBookIdAndChapterOrder(Long bookId, Integer chapterOrder);

    /**
     * 获取最大章节序号
     * @return 最大序号
     */
    @Query("SELECT COALESCE(MAX(c.chapterOrder), 0) FROM Chapter c")
    Integer findMaxChapterOrder();

    /**
     * 获取指定书籍的最大章节序号
     * @param bookId 书籍ID
     * @return 最大序号
     */
    @Query("SELECT COALESCE(MAX(c.chapterOrder), 0) FROM Chapter c WHERE c.bookId = :bookId")
    Integer findMaxChapterOrderByBookId(Long bookId);

    /**
     * 获取总字数统计
     * @return 总字数
     */
    @Query("SELECT COALESCE(SUM(c.wordCount), 0) FROM Chapter c")
    Long getTotalWordCount();

    /**
     * 获取指定书籍的总字数统计
     * @param bookId 书籍ID
     * @return 总字数
     */
    @Query("SELECT COALESCE(SUM(c.wordCount), 0) FROM Chapter c WHERE c.bookId = :bookId")
    Long getTotalWordCountByBookId(Long bookId);

    /**
     * 根据标题模糊查询
     * @param title 标题关键词
     * @return 章节列表
     */
    List<Chapter> findByTitleContaining(String title);

    /**
     * 获取指定范围内的章节
     * @param startOrder 起始序号
     * @param endOrder 结束序号
     * @return 章节列表
     */
    @Query("SELECT c FROM Chapter c WHERE c.chapterOrder >= :startOrder AND c.chapterOrder <= :endOrder ORDER BY c.chapterOrder ASC")
    List<Chapter> findByChapterOrderBetween(Integer startOrder, Integer endOrder);

    /**
     * 获取章节总数
     * @return 章节数量
     */
    @Query("SELECT COUNT(c) FROM Chapter c")
    Long getChapterCount();
}
