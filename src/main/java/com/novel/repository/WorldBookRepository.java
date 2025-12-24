package com.novel.repository;

import com.novel.entity.WorldBook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 世界书数据访问层
 */
@Repository
public interface WorldBookRepository extends JpaRepository<WorldBook, Long> {

    /**
     * 根据书籍ID获取世界书列表（按排序顺序）
     */
    List<WorldBook> findByBookIdOrderBySortOrderAsc(Long bookId);

    /**
     * 根据书籍ID获取启用的世界书列表
     */
    List<WorldBook> findByBookIdAndIsActiveTrueOrderBySortOrderAsc(Long bookId);

    /**
     * 根据书籍ID删除所有世界书
     */
    void deleteByBookId(Long bookId);
}
