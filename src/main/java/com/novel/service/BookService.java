package com.novel.service;

import com.novel.entity.Book;
import com.novel.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    @Autowired
    private BookRepository bookRepository;

    /**
     * 获取所有书籍
     */
    public List<Book> findAll() {
        return bookRepository.findAllByOrderByCreateTimeDesc();
    }

    /**
     * 根据ID获取书籍
     */
    public Optional<Book> findById(Long id) {
        return bookRepository.findById(id);
    }

    /**
     * 获取当前激活的书籍
     */
    public Optional<Book> getActiveBook() {
        return bookRepository.findByIsActiveTrue();
    }

    /**
     * 创建新书籍
     */
    @Transactional
    public Book create(Book book) {
        // 如果是第一本书，自动设为激活
        if (bookRepository.count() == 0) {
            book.setIsActive(true);
        }
        return bookRepository.save(book);
    }

    /**
     * 更新书籍
     */
    @Transactional
    public Book update(Long id, Book bookData) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("书籍不存在"));
        
        if (bookData.getName() != null) {
            book.setName(bookData.getName());
        }
        if (bookData.getDescription() != null) {
            book.setDescription(bookData.getDescription());
        }
        if (bookData.getAuthor() != null) {
            book.setAuthor(bookData.getAuthor());
        }
        if (bookData.getGenre() != null) {
            book.setGenre(bookData.getGenre());
        }
        
        return bookRepository.save(book);
    }

    /**
     * 删除书籍
     */
    @Transactional
    public void delete(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("书籍不存在"));
        
        boolean wasActive = book.getIsActive();
        bookRepository.delete(book);
        
        // 如果删除的是激活书籍，自动激活另一本
        if (wasActive) {
            List<Book> remaining = bookRepository.findAllByOrderByCreateTimeDesc();
            if (!remaining.isEmpty()) {
                Book first = remaining.get(0);
                first.setIsActive(true);
                bookRepository.save(first);
            }
        }
    }

    /**
     * 切换到指定书籍
     */
    @Transactional
    public Book switchTo(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("书籍不存在"));
        
        // 将所有书籍设为非激活
        bookRepository.deactivateAll();
        
        // 激活指定书籍
        book.setIsActive(true);
        return bookRepository.save(book);
    }

    /**
     * 获取或创建默认书籍
     */
    @Transactional
    public Book getOrCreateDefault() {
        Optional<Book> activeBook = bookRepository.findByIsActiveTrue();
        if (activeBook.isPresent()) {
            return activeBook.get();
        }
        
        List<Book> allBooks = bookRepository.findAll();
        if (!allBooks.isEmpty()) {
            Book first = allBooks.get(0);
            first.setIsActive(true);
            return bookRepository.save(first);
        }
        
        // 创建默认书籍
        Book defaultBook = new Book("我的小说");
        defaultBook.setDescription("默认创建的书籍");
        defaultBook.setIsActive(true);
        return bookRepository.save(defaultBook);
    }
}
