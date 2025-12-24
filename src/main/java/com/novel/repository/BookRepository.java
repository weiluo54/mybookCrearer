package com.novel.repository;

import com.novel.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    
    /**
     * 查找当前激活的书籍
     */
    Optional<Book> findByIsActiveTrue();
    
    /**
     * 按创建时间降序获取所有书籍
     */
    List<Book> findAllByOrderByCreateTimeDesc();
    
    /**
     * 将所有书籍设为非激活状态
     */
    @Modifying
    @Transactional
    @Query("UPDATE Book b SET b.isActive = false")
    void deactivateAll();
    
    /**
     * 根据名称查找书籍
     */
    Optional<Book> findByName(String name);
}
