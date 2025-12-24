package com.novel.service;

import com.novel.entity.WorldBook;
import com.novel.repository.WorldBookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 世界书服务层
 */
@Service
public class WorldBookService {

    private final WorldBookRepository worldBookRepository;

    public WorldBookService(WorldBookRepository worldBookRepository) {
        this.worldBookRepository = worldBookRepository;
    }

    /**
     * 获取指定书籍的所有世界书
     */
    public List<WorldBook> getWorldBooksByBookId(Long bookId) {
        return worldBookRepository.findByBookIdOrderBySortOrderAsc(bookId);
    }

    /**
     * 获取指定书籍的启用世界书
     */
    public List<WorldBook> getActiveWorldBooksByBookId(Long bookId) {
        return worldBookRepository.findByBookIdAndIsActiveTrueOrderBySortOrderAsc(bookId);
    }

    /**
     * 根据ID获取世界书
     */
    public Optional<WorldBook> getWorldBookById(Long id) {
        return worldBookRepository.findById(id);
    }

    /**
     * 创建世界书
     */
    @Transactional
    public WorldBook createWorldBook(WorldBook worldBook) {
        worldBook.setCreateTime(LocalDateTime.now());
        worldBook.setUpdateTime(LocalDateTime.now());
        if (worldBook.getSortOrder() == null) {
            worldBook.setSortOrder(0);
        }
        if (worldBook.getIsActive() == null) {
            worldBook.setIsActive(true);
        }
        return worldBookRepository.save(worldBook);
    }

    /**
     * 更新世界书
     */
    @Transactional
    public WorldBook updateWorldBook(Long id, WorldBook worldBook) {
        Optional<WorldBook> existingOpt = worldBookRepository.findById(id);
        if (existingOpt.isPresent()) {
            WorldBook existing = existingOpt.get();
            if (worldBook.getTitle() != null) {
                existing.setTitle(worldBook.getTitle());
            }
            if (worldBook.getContent() != null) {
                existing.setContent(worldBook.getContent());
            }
            if (worldBook.getSortOrder() != null) {
                existing.setSortOrder(worldBook.getSortOrder());
            }
            if (worldBook.getIsActive() != null) {
                existing.setIsActive(worldBook.getIsActive());
            }
            existing.setUpdateTime(LocalDateTime.now());
            return worldBookRepository.save(existing);
        }
        throw new RuntimeException("世界书不存在，ID: " + id);
    }

    /**
     * 删除世界书
     */
    @Transactional
    public void deleteWorldBook(Long id) {
        if (worldBookRepository.existsById(id)) {
            worldBookRepository.deleteById(id);
        } else {
            throw new RuntimeException("世界书不存在，ID: " + id);
        }
    }

    /**
     * 根据用户提示词匹配世界书，返回匹配到的世界书内容
     * 匹配规则：用户提示词中包含世界书标题
     * 
     * @param userPrompt 用户提示词
     * @param bookId 书籍ID
     * @return 匹配到的世界书内容，拼接在一起
     */
    public String getMatchedWorldBookContent(String userPrompt, Long bookId) {
        if (userPrompt == null || userPrompt.isEmpty() || bookId == null) {
            return "";
        }
        
        List<WorldBook> activeWorldBooks = getActiveWorldBooksByBookId(bookId);
        List<String> matchedContents = new ArrayList<>();
        
        for (WorldBook worldBook : activeWorldBooks) {
            if (worldBook.getTitle() != null && !worldBook.getTitle().isEmpty()) {
                // 检查用户提示词是否包含世界书标题
                if (userPrompt.contains(worldBook.getTitle())) {
                    if (worldBook.getContent() != null && !worldBook.getContent().isEmpty()) {
                        matchedContents.add("【" + worldBook.getTitle() + "】\n" + worldBook.getContent());
                    }
                }
            }
        }
        
        if (matchedContents.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n【世界书设定】\n");
        for (String content : matchedContents) {
            sb.append(content).append("\n\n");
        }
        return sb.toString();
    }
}
