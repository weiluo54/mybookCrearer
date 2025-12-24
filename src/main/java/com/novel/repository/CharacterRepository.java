package com.novel.repository;

import com.novel.entity.Character;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 角色数据访问层
 * 
 * @author NovelCreator
 */
@Repository
public interface CharacterRepository extends JpaRepository<Character, Long> {

    /**
     * 获取所有角色，按主要角色优先排序
     * @return 角色列表
     */
    List<Character> findAllByOrderByIsMainDescCreateTimeAsc();

    /**
     * 根据书籍ID获取所有角色，按主要角色优先排序
     * @param bookId 书籍ID
     * @return 角色列表
     */
    List<Character> findByBookIdOrderByIsMainDescCreateTimeAsc(Long bookId);

    /**
     * 根据角色名称查找
     * @param name 角色名称
     * @return 角色
     */
    Optional<Character> findByName(String name);

    /**
     * 根据角色名称查找
     * @param name 角色名称
     * @return 角色
     */
    Optional<Character> findByNameAndBookId(String name, Long bookId);

    /**
     * 根据名称模糊查询
     * @param name 名称关键词
     * @return 角色列表
     */
    List<Character> findByNameContaining(String name);

    /**
     * 获取所有主要角色
     * @return 主要角色列表
     */
    List<Character> findByIsMainTrue();

//    /**
//     * 根据角色定位查询
//     * @param role 角色定位
//     * @return 角色列表
//     */
//    List<Character> findByRole(String role);

    /**
     * 获取指定章节首次出现的角色
     * @param chapterId 章节ID
     * @return 角色列表
     */
    List<Character> findByChapterId(Long chapterId);

    /**
     * 获取角色总数
     * @return 角色数量
     */
    @Query("SELECT COUNT(c) FROM Character c")
    Long getCharacterCount();

    /**
     * 获取主要角色数量
     * @return 主要角色数量
     */
    @Query("SELECT COUNT(c) FROM Character c WHERE c.isMain = true")
    Long getMainCharacterCount();
}
