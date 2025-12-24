package com.novel.service;

import com.novel.entity.Character;
import com.novel.repository.CharacterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 角色服务层
 * 处理角色的CRUD操作和业务逻辑
 * 
 * @author NovelCreator
 */
@Service
public class CharacterService {

    private final CharacterRepository characterRepository;

    /**
     * 构造函数注入
     * @param characterRepository 角色数据访问层
     */
    public CharacterService(CharacterRepository characterRepository) {
        this.characterRepository = characterRepository;
    }

    /**
     * 获取所有角色（主要角色优先）
     * @return 角色列表
     */
    public List<Character> getAllCharacters() {
        return characterRepository.findAllByOrderByIsMainDescCreateTimeAsc();
    }

    /**
     * 根据书籍ID获取角色（主要角色优先）
     * @param bookId 书籍ID
     * @return 角色列表
     */
    public List<Character> getCharactersByBookId(Long bookId) {
        return characterRepository.findByBookIdOrderByIsMainDescCreateTimeAsc(bookId);
    }

    /**
     * 获取指定书籍的角色数量
     * @param bookId 书籍ID
     * @return 角色数量
     */
    public Long getCharacterCountByBookId(Long bookId) {
        return (long) characterRepository.findByBookIdOrderByIsMainDescCreateTimeAsc(bookId).size();
    }

    /**
     * 根据ID获取角色
     * @param id 角色ID
     * @return 角色
     */
    public Optional<Character> getCharacterById(Long id) {
        return characterRepository.findById(id);
    }

    /**
     * 根据名称获取角色
     * @param name 角色名称
     * @return 角色
     */
    public Optional<Character> getCharacterByName(String name) {
        return characterRepository.findByName(name);
    }

    /**
     * 创建新角色
     * @param character 角色信息
     * @return 保存后的角色
     */
    @Transactional
    public Character createCharacter(Character character) {
        // 检查名称是否重复
        Optional<Character> existing = characterRepository.findByNameAndBookId(character.getName(),character.getBookId());
        if (existing.isPresent()) {
            throw new RuntimeException("角色名称已存在: " + character.getName());
        }
        character.setCreateTime(LocalDateTime.now());
        character.setUpdateTime(LocalDateTime.now());
        return characterRepository.save(character);
    }

    /**
     * 更新角色
     * @param id 角色ID
     * @param character 角色信息
     * @return 更新后的角色
     */
    @Transactional
    public Character updateCharacter(Long id, Character character) {
        Optional<Character> existingOpt = characterRepository.findById(id);
        if (existingOpt.isPresent()) {
            Character existing = existingOpt.get();
            if (character.getName() != null) {
                existing.setName(character.getName());
            }
            if (character.getInfo() != null) {
                existing.setInfo(character.getInfo());
            }
            if (character.getIsMain() != null) {
                existing.setIsMain(character.getIsMain());
            }
            if (character.getChapterId() != null) {
                existing.setChapterId(character.getChapterId());
            }
            // 更新新增的四个字段
            if (character.getRoleRelation() != null) {
                existing.setRoleRelation(character.getRoleRelation());
            }
            if (character.getRole() != null) {
                existing.setRole(character.getRole());
            }
            if (character.getGender() != null) {
                existing.setGender(character.getGender());
            }
            if (character.getCodeName() != null) {
                existing.setCodeName(character.getCodeName());
            }
            existing.setUpdateTime(LocalDateTime.now());
            return characterRepository.save(existing);
        }
        throw new RuntimeException("角色不存在，ID: " + id);
    }

    /**
     * 删除角色
     * @param id 角色ID
     */
    @Transactional
    public void deleteCharacter(Long id) {
        if (characterRepository.existsById(id)) {
            characterRepository.deleteById(id);
        } else {
            throw new RuntimeException("角色不存在，ID: " + id);
        }
    }

    /**
     * 获取所有主要角色
     * @return 主要角色列表
     */
    public List<Character> getMainCharacters() {
        return characterRepository.findByIsMainTrue();
    }

    /**
     * 获取指定角色的描述信息（用于组装提示词）
     * @param characterIds 角色ID列表
     * @return 角色描述字符串
     */
    public String getCharactersDescription(List<Long> characterIds) {
        StringBuilder sb = new StringBuilder();
        sb.append("【出场角色信息】\n");
        for (Long id : characterIds) {
            Optional<Character> charOpt = characterRepository.findById(id);
            if (charOpt.isPresent()) {
                Character character = charOpt.get();
                sb.append(character.getFullDescription()).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 获取所有角色的描述信息
     * @return 角色描述字符串
     */
    public String getAllCharactersDescription() {
        List<Character> characters = characterRepository.findAllByOrderByIsMainDescCreateTimeAsc();
        StringBuilder sb = new StringBuilder();
        sb.append("【所有角色信息】\n");
        for (Character character : characters) {
            sb.append(character.getFullDescription()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 根据用户提示词匹配角色，获取在userPrompt中出现的角色描述信息
     * 匹配条件：角色名称、角色关系、角色代号
     * 特殊处理：当提示词包含"只出场异性角色"时，根据主角性别筛选异性角色
     * 
     * @param userPrompt 用户输入的创作要求
     * @return 匹配到的角色描述字符串
     */
    public String getMatchedCharactersDescription(String userPrompt) {
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            return "";
        }
        
        List<Character> allCharacters = characterRepository.findAllByOrderByIsMainDescCreateTimeAsc();
        List<Character> matchedCharacters = new java.util.ArrayList<>();
        
        // 检查是否需要进行异性角色筛选
        boolean onlyOppositeGender = userPrompt.contains("只出场异性角色");
        String protagonistGender = null;
        
        if (onlyOppositeGender) {
            // 找出主角的性别
            for (Character character : allCharacters) {
                if (character.getIsMain() != null && character.getIsMain()) {
                    protagonistGender = character.getGender();
                    break;
                }
            }
        }
        
        // 遍历所有角色，检查是否匹配
        for (Character character : allCharacters) {
            boolean matched = false;
            
            // 1. 检查角色名称是否在userPrompt中出现
            if (character.getName() != null && !character.getName().isEmpty()) {
                if (userPrompt.contains(character.getName())) {
                    matched = true;
                }
            }
            
            // 2. 检查角色关系是否在userPrompt中出现
            if (!matched && character.getRoleRelation() != null && !character.getRoleRelation().isEmpty()) {
                if (userPrompt.contains(character.getRoleRelation())) {
                    matched = true;
                }
            }
            
            // 3. 检查角色代号是否在userPrompt中出现
            if (!matched && character.getCodeName() != null && !character.getCodeName().isEmpty()) {
                if (userPrompt.contains(character.getCodeName())) {
                    matched = true;
                }
            }
            
            // 如果匹配到了角色
            if (matched) {
                // 如果需要异性筛选，检查性别
                if (onlyOppositeGender && protagonistGender != null) {
                    String charGender = character.getGender();
                    // 只有异性角色才添加（主角除外）
                    if (character.getIsMain() != null && character.getIsMain()) {
                        // 主角始终添加
                        matchedCharacters.add(character);
                    } else if (charGender != null && !charGender.equals(protagonistGender)) {
                        // 异性角色添加
                        matchedCharacters.add(character);
                    }
                } else {
                    matchedCharacters.add(character);
                }
            }
        }
        
        // 如果没有匹配到任何角色，返回空字符串
        if (matchedCharacters.isEmpty()) {
            return "";
        }
        
        // 构建匹配角色的描述信息
        StringBuilder sb = new StringBuilder();
        sb.append("【出场角色信息】\n");
        for (Character character : matchedCharacters) {
            sb.append(character.getFullDescription()).append("\n");
        }
        return sb.toString();
    }

    /**
     * 根据用户提示词匹配角色ID列表
     * 
     * @param userPrompt 用户输入的创作要求
     * @return 匹配到的角色ID列表
     */
    public List<Long> getMatchedCharacterIds(String userPrompt) {
        if (userPrompt == null || userPrompt.trim().isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        List<Character> allCharacters = characterRepository.findAllByOrderByIsMainDescCreateTimeAsc();
        List<Long> matchedIds = new java.util.ArrayList<>();
        
        for (Character character : allCharacters) {
            if (character.getName() != null && !character.getName().isEmpty()) {
                if (userPrompt.contains(character.getName())) {
                    matchedIds.add(character.getId());
                }
            }
        }
        
        return matchedIds;
    }

    /**
     * 搜索角色
     * @param keyword 关键词
     * @return 角色列表
     */
    public List<Character> searchCharacters(String keyword) {
        return characterRepository.findByNameContaining(keyword);
    }

    /**
     * 获取角色总数
     * @return 角色数量
     */
    public Long getCharacterCount() {
        return characterRepository.getCharacterCount();
    }

    /**
     * 根据章节ID获取角色
     * @param chapterId 章节ID
     * @return 角色列表
     */
    public List<Character> getCharactersByChapter(Long chapterId) {
        return characterRepository.findByChapterId(chapterId);
    }
}
