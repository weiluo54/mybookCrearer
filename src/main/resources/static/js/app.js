/**
 * AI网文创作系统 - 前端脚本
 */

// localStorage 缓存键名常量
const CACHE_KEYS = {
    BOOK_SELECTIONS: 'novel_book_selections'
};

// 全局变量
let currentChapterId = null;
let currentBookId = null;
let chapters = [];
let characters = [];
let collections = [];
let templates = [];
let configs = [];
let books = [];
let streamAbortController = null;

// 页面加载完成后初始化
$(document).ready(function() {
    loadBooks(); // 先加载书籍
    loadAllData();
    $('#contentText').on('input', updateWordCount);
    
    // 添加下拉选择器变化监听
    setupDropdownListeners();
    
    // 添加键盘快捷键
    $(document).on('keydown', function(e) {
        // Ctrl+S 保存
        if (e.ctrlKey && e.key === 's') {
            e.preventDefault();
            saveContent();
        }
        // Escape 关闭弹窗
        if (e.key === 'Escape') {
            $('.modal.show').removeClass('show');
        }
    });
});

// 设置下拉选择器变化监听
function setupDropdownListeners() {
    // 主界面的模板和配置选择器
    $('#templateSelect').on('change', function() {
        saveDropdownSelection('templateSelect', $(this).val());
    });
    
    $('#configSelect').on('change', function() {
        saveDropdownSelection('configSelect', $(this).val());
    });
    
    // 概括生成弹窗的选择器
    $('#summaryTemplateSelect').on('change', function() {
        saveDropdownSelection('summaryTemplateSelect', $(this).val());
    });
    
    $('#summaryConfigSelect').on('change', function() {
        saveDropdownSelection('summaryConfigSelect', $(this).val());
    });
    
    // 汇总生成弹窗的选择器
    $('#collectionTemplateSelect').on('change', function() {
        saveDropdownSelection('collectionTemplateSelect', $(this).val());
    });
    
    $('#collectionConfigSelect').on('change', function() {
        saveDropdownSelection('collectionConfigSelect', $(this).val());
    });
    
    // 角色生成弹窗的选择器
    $('#characterTemplateSelect').on('change', function() {
        saveDropdownSelection('characterTemplateSelect', $(this).val());
    });
    
    $('#characterConfigSelect').on('change', function() {
        saveDropdownSelection('characterConfigSelect', $(this).val());
    });
}

// 保存下拉选择器的选择到localStorage
function saveDropdownSelection(selectId, value) {
    if (!currentBookId) return;
    
    try {
        let bookSelections = JSON.parse(localStorage.getItem(CACHE_KEYS.BOOK_SELECTIONS) || '{}');
        
        if (!bookSelections[currentBookId]) {
            bookSelections[currentBookId] = {};
        }
        
        bookSelections[currentBookId][selectId] = value;
        localStorage.setItem(CACHE_KEYS.BOOK_SELECTIONS, JSON.stringify(bookSelections));
        
        console.log(`已保存书籍 ${currentBookId} 的 ${selectId} 选择: ${value}`);
    } catch (error) {
        console.error('保存下拉选择失败:', error);
    }
}

// 从localStorage加载下拉选择器的选择
function loadDropdownSelection(selectId) {
    if (!currentBookId) return null;
    
    try {
        const bookSelections = JSON.parse(localStorage.getItem(CACHE_KEYS.BOOK_SELECTIONS) || '{}');
        return bookSelections[currentBookId] && bookSelections[currentBookId][selectId] || null;
    } catch (error) {
        console.error('加载下拉选择失败:', error);
        return null;
    }
}

// 应用缓存的下拉选择器选择
function applyCachedSelections() {
    if (!currentBookId) return;
    
    const selectors = [
        'templateSelect',
        'configSelect',
        'summaryTemplateSelect', 
        'summaryConfigSelect',
        'collectionTemplateSelect',
        'collectionConfigSelect',
        'characterTemplateSelect',
        'characterConfigSelect'
    ];
    
    selectors.forEach(selectId => {
        const cachedValue = loadDropdownSelection(selectId);
        if (cachedValue) {
            const $select = $('#' + selectId);
            if ($select.length && $select.find(`option[value="${cachedValue}"]`).length) {
                $select.val(cachedValue);
                console.log(`已恢复书籍 ${currentBookId} 的 ${selectId} 选择: ${cachedValue}`);
            }
        }
    });
}

// 清除指定书籍的缓存选择
function clearBookSelections(bookId) {
    try {
        let bookSelections = JSON.parse(localStorage.getItem(CACHE_KEYS.BOOK_SELECTIONS) || '{}');
        if (bookSelections[bookId]) {
            delete bookSelections[bookId];
            localStorage.setItem(CACHE_KEYS.BOOK_SELECTIONS, JSON.stringify(bookSelections));
            console.log(`已清除书籍 ${bookId} 的缓存选择`);
        }
    } catch (error) {
        console.error('清除书籍选择缓存失败:', error);
    }
}

// 加载所有数据
function loadAllData() {
    loadStats();
    loadChapters();
    loadCharacters();
    loadCollections();
    loadTemplates();
    loadConfigs();
    // loadWorldBooks 由 loadBooks 或 switchBook 触发，避免重复调用
}

// 加载统计数据
function loadStats() {
    $.get('/api/stats', function(res) {
        if (res.code === 200) {
            $('#statChapters').text(res.data.chapterCount);
            $('#statWords').text(res.data.totalWordCount);
            $('#statCharacters').text(res.data.characterCount);
        }
    });
}

// 加载章节列表
function loadChapters() {
    $.get('/api/chapters', function(res) {
        if (res.code === 200) {
            chapters = res.data;
            renderChapterList();
        }
    });
}

// 渲染章节列表
function renderChapterList() {
    let html = '';
    chapters.forEach(ch => {
        const active = ch.id === currentChapterId ? 'active' : '';
        html += `
            <div class="chapter-item ${active}" onclick="selectChapter(${ch.id})">
                <span class="title">第${ch.chapterOrder}章 ${ch.title}</span>
                <div class="actions">
                    <button onclick="event.stopPropagation();editChapterTitle(${ch.id},'${ch.title}')" title="编辑标题">✏️</button>
                    <button onclick="event.stopPropagation();deleteChapter(${ch.id})" title="删除章节">🗑️</button>
                </div>
            </div>
        `;
    });
    $('#chapterList').html(html || '<div style="padding:20px;color:var(--text-muted);text-align:center;">暂无章节，点击上方按钮创建</div>');
}

// 选择章节
function selectChapter(id) {
    currentChapterId = id;
    const chapter = chapters.find(c => c.id === id);
    if (chapter) {
        $('#currentTitle').text(`第${chapter.chapterOrder}章 ${chapter.title}`);
        $('#contentText').val(chapter.content || '');
        $('#summaryText').val(chapter.summary || '');
        $('#userPrompt').val(chapter.userPrompt || '');
        updateWordCount();
    }
    renderChapterList();
}

// 创建新章节
function createNewChapter() {
    if (!currentBookId) {
        showToast('请先选择或创建一本书籍', 'error');
        return;
    }
    const title = prompt('请输入章节标题:', '新章节');
    if (title) {
        $.ajax({
            url: '/api/chapters',
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ title: title, bookId: currentBookId }),
            success: function(res) {
                if (res.code === 200) {
                    showToast('章节创建成功', 'success');
                    loadChapters();
                    loadStats();
                    setTimeout(() => selectChapter(res.data.id), 300);
                } else {
                    showToast(res.message, 'error');
                }
            }
        });
    }
}

// 编辑章节标题
function editChapterTitle(id, oldTitle) {
    const newTitle = prompt('修改章节标题:', oldTitle);
    if (newTitle && newTitle !== oldTitle) {
        $.ajax({
            url: '/api/chapters/' + id,
            type: 'PUT',
            contentType: 'application/json',
            data: JSON.stringify({ title: newTitle }),
            success: function(res) {
                if (res.code === 200) {
                    showToast('标题修改成功', 'success');
                    loadChapters();
                    if (currentChapterId === id) {
                        $('#currentTitle').text(`第${res.data.chapterOrder}章 ${newTitle}`);
                    }
                }
            }
        });
    }
}

// 删除章节
function deleteChapter(id) {
    if (confirm('确定要删除这个章节吗？此操作不可恢复。')) {
        $.ajax({
            url: '/api/chapters/' + id,
            type: 'DELETE',
            success: function(res) {
                if (res.code === 200) {
                    showToast('章节删除成功', 'success');
                    if (currentChapterId === id) {
                        currentChapterId = null;
                        $('#currentTitle').text('请选择或创建章节');
                        $('#contentText').val('');
                        $('#summaryText').val('');
                        $('#userPrompt').val('');
                    }
                    loadChapters();
                    loadStats();
                }
            }
        });
    }
}

// 加载角色列表
function loadCharacters() {
    $.get('/api/characters', function(res) {
        if (res.code === 200) {
            characters = res.data;
            renderCharacterList();
        }
    });
}

// 渲染角色列表
function renderCharacterList() {
    let html = '';
    characters.forEach(ch => {
        const mainClass = ch.isMain ? 'main' : '';
        const initial = ch.name.charAt(0);
        html += `
            <div class="character-item ${mainClass}">
                <div class="char-info" onclick="editCharacter(${ch.id})">
                    <div class="avatar">${initial}</div>
                    <span class="char-name">${ch.name}</span>
                </div>
                <div class="char-actions">
                    <button onclick="event.stopPropagation();editCharacter(${ch.id})" title="编辑">✏️</button>
                    <button onclick="event.stopPropagation();deleteCharacter(${ch.id})" title="删除">🗑️</button>
                </div>
            </div>
        `;
    });
    $('#characterList').html(html || '<div style="padding:20px;color:var(--text-muted);text-align:center;">暂无角色</div>');
}

// 删除角色
function deleteCharacter(id) {
    const char = characters.find(c => c.id === id);
    const charName = char ? char.name : '该角色';
    if (confirm(`确定要删除角色"${charName}"吗？`)) {
        $.ajax({
            url: '/api/characters/' + id,
            type: 'DELETE',
            success: function(res) {
                if (res.code === 200) {
                    showToast('角色删除成功', 'success');
                    loadCharacters();
                    loadStats();
                } else {
                    showToast(res.message || '删除失败', 'error');
                }
            },
            error: function() {
                showToast('删除失败，请重试', 'error');
            }
        });
    }
}

// 打开角色弹窗
function openCharacterModal(id) {
    $('#characterId').val('');
    $('#charName').val('');
    $('#charRole').val('');
    $('#charGender').val('');
    $('#charRoleRelation').val('');
    $('#charCodeName').val('');
    $('#charInfo').val('');
    $('#charIsMain').prop('checked', false);
    $('#characterModalTitle').text('新建角色');
    showModal('characterModal');
}

// 编辑角色
function editCharacter(id) {
    const char = characters.find(c => c.id === id);
    if (char) {
        $('#characterId').val(char.id);
        $('#charName').val(char.name);
        $('#charRole').val(char.role || '');
        $('#charGender').val(char.gender || '');
        $('#charRoleRelation').val(char.roleRelation || '');
        $('#charCodeName').val(char.codeName || '');
        
        // 直接使用存储的info字段（用户输入的原始内容）
        $('#charInfo').val(char.info || '');
        $('#charIsMain').prop('checked', char.isMain);
        $('#characterModalTitle').text('编辑角色');
        showModal('characterModal');
    }
}

// 保存角色
function saveCharacter() {
    const id = $('#characterId').val();
    
    // 新建角色时需要检查是否选择了书籍
    if (!id && !currentBookId) {
        showToast('请先选择或创建一本书籍', 'error');
        return;
    }
    
    const charName = $('#charName').val();
    const charInfo = $('#charInfo').val();
    
    if (!charName) {
        showToast('请输入角色名称', 'error');
        return;
    }
    
    const data = {
        name: charName,
        info: charInfo,  // 将所有信息存放在info字段
        isMain: $('#charIsMain').prop('checked'),
        role: $('#charRole').val() || null,
        gender: $('#charGender').val() || null,
        roleRelation: $('#charRoleRelation').val() || null,
        codeName: $('#charCodeName').val() || null
    };
    
    // 新建角色时添加 bookId
    if (!id) {
        data.bookId = currentBookId;
    }
    
    const url = id ? '/api/characters/' + id : '/api/characters';
    const method = id ? 'PUT' : 'POST';
    
    $.ajax({
        url: url,
        type: method,
        contentType: 'application/json',
        data: JSON.stringify(data),
        success: function(res) {
            if (res.code === 200) {
                showToast('角色保存成功', 'success');
                closeModal('characterModal');
                loadCharacters();
                loadStats();
            } else {
                showToast(res.message, 'error');
            }
        }
    });
}

// 生成内容
function generateContent() {
    const userPrompt = $('#userPrompt').val();
    if (!userPrompt) {
        showToast('请输入创作要求', 'error');
        return;
    }
    
    // 如果没有选择章节，需要创建新章节，检查是否有书籍
    if (!currentChapterId && !currentBookId) {
        showToast('请先选择或创建一本书籍', 'error');
        return;
    }
    
    const isStreamMode = $('#streamMode').prop('checked');
    
    // 获取前一个章节的内容（如果创建新章节）
    let previousChapterContent = '';
    if (!currentChapterId && chapters.length > 0) {
        // 获取最后一个（最新）章节的内容
        const lastChapter = chapters[chapters.length - 1];
        if (lastChapter && lastChapter.content) {
            previousChapterContent = lastChapter.content;
        }
    }
    
    const request = {
        userPrompt: userPrompt,
        chapterId: currentChapterId,
        createNewChapter: !currentChapterId,
        bookId: currentBookId,  // 添加 bookId
        templateId: $('#templateSelect').val() || null,
        configId: $('#configSelect').val() || null,
        includeHistory: $('#includeHistory').prop('checked'),
        historyCount: 5,
        generateType: 'CONTENT',
        stream: isStreamMode,
        previousChapterContent: previousChapterContent  // 添加前一章节内容
    };
    
    if (isStreamMode) {
        generateContentStream(request);
    } else {
        generateContentNormal(request);
    }
}

// 非流式生成内容
function generateContentNormal(request) {
    showLoading();
    
    $.ajax({
        url: '/api/generate',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(request),
        success: function(res) {
            hideLoading();
            if (res.code === 200) {
                showToast('内容生成成功！', 'success');
                handleGenerateResult(res.data);
            } else {
                showToast(res.message, 'error');
            }
        },
        error: function(xhr) {
            hideLoading();
            showToast('生成失败，请检查AI配置', 'error');
        }
    });
}

// 流式生成内容
function generateContentStream(request) {
    streamAbortController = new AbortController();
    
    $('#generateBtn').prop('disabled', true);
    $('#stopStreamBtn').show();
    $('#contentText').val('');
    showToast('正在生成中...', 'success');
    
    let fullContent = '';
    let buffer = '';
    
    fetch('/api/generate/stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request),
        signal: streamAbortController.signal
    }).then(response => {
        if (!response.ok) {
            throw new Error('请求失败: ' + response.status);
        }
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        
        function read() {
            reader.read().then(({done, value}) => {
                if (done) {
                    onStreamComplete(fullContent);
                    return;
                }
                
                const chunk = decoder.decode(value, {stream: true});
                buffer += chunk;
                
                const lines = buffer.split('\n');
                buffer = lines.pop() || '';
                
                lines.forEach(line => {
                    line = line.trim();
                    if (!line) return;
                    
                    if (line.startsWith('data:')) {
                        const data = line.substring(5).trim();
                        
                        if (data === '[DONE]') return;
                        if (data === '[ERROR]' || data.startsWith('[ERROR]')) {
                            showToast('生成出错: ' + data, 'error');
                            return;
                        }
                        
                        try {
                            const json = JSON.parse(data);
                            if (json.choices && json.choices[0]) {
                                const choice = json.choices[0];
                                if (choice.delta && choice.delta.content) {
                                    fullContent += choice.delta.content;
                                    $('#contentText').val(fullContent);
                                    updateWordCount();
                                    const textarea = document.getElementById('contentText');
                                    textarea.scrollTop = textarea.scrollHeight;
                                } else if (choice.message && choice.message.content) {
                                    fullContent = choice.message.content;
                                    $('#contentText').val(fullContent);
                                    updateWordCount();
                                }
                            }
                        } catch(e) {
                            if (data && data !== '[DONE]') {
                                fullContent += data;
                                $('#contentText').val(fullContent);
                                updateWordCount();
                            }
                        }
                    }
                });
                
                read();
            }).catch(error => {
                if (error.name === 'AbortError') {
                    showToast('已停止生成', 'success');
                    onStreamComplete(fullContent);
                } else {
                    onStreamError(error);
                }
            });
        }
        
        read();
    }).catch(error => {
        if (error.name === 'AbortError') {
            showToast('已停止生成', 'success');
            onStreamComplete(fullContent);
        } else {
            onStreamError(error);
        }
    });
}

function onStreamComplete(fullContent) {
    $('#generateBtn').prop('disabled', false);
    $('#stopStreamBtn').hide();
    streamAbortController = null;
    
    if (fullContent) {
        showToast('内容生成完成！', 'success');
        
        // 调用后端解析角色信息标记（【@ 内容 @】格式）
        parseCharacterInfoFromContent(fullContent);
    }
    
    updateWordCount();
    loadChapters();
    loadStats();
    
    if (currentChapterId && fullContent) {
        saveContentOnly();
    }
}

// 解析内容中的角色信息标记
function parseCharacterInfoFromContent(content) {
    if (!content) return;
    
    $.ajax({
        url: '/api/parse-character-info',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify({ content: content }),
        success: function(res) {
            if (res.code === 200) {
                console.log('角色信息解析完成');
                // 刷新角色列表
                loadCharacters();
            }
        },
        error: function(err) {
            console.error('解析角色信息失败:', err);
        }
    });
}

function onStreamError(error) {
    $('#generateBtn').prop('disabled', false);
    $('#stopStreamBtn').hide();
    streamAbortController = null;
    showToast('生成失败: ' + error.message, 'error');
    console.error('流式生成错误:', error);
}

function stopStreamGeneration() {
    if (streamAbortController) {
        streamAbortController.abort();
        showToast('正在停止生成...', 'success');
    }
}

function handleGenerateResult(data) {
    if (data.content) {
        $('#contentText').val(data.content);
    }
    updateWordCount();
    loadChapters();
    loadStats();
    
    if (currentChapterId) {
        saveContentOnly();
    }
}

function regenerateContent() {
    if (!currentChapterId) {
        showToast('请先选择一个章节', 'error');
        return;
    }
    if (confirm('确定要重新生成这章的内容吗？当前内容将被覆盖。')) {
        generateContent();
    }
}

function saveContent() {
    if (!currentChapterId) {
        showToast('请先选择一个章节', 'error');
        return;
    }
    
    $.ajax({
        url: '/api/chapters/' + currentChapterId,
        type: 'PUT',
        contentType: 'application/json',
        data: JSON.stringify({
            content: $('#contentText').val(),
            summary: $('#summaryText').val(),
            userPrompt: $('#userPrompt').val()
        }),
        success: function(res) {
            if (res.code === 200) {
                showToast('保存成功', 'success');
                loadChapters();
                loadStats();
            }
        }
    });
}

function saveContentOnly() {
    if (!currentChapterId) return;
    
    $.ajax({
        url: '/api/chapters/' + currentChapterId,
        type: 'PUT',
        contentType: 'application/json',
        data: JSON.stringify({
            content: $('#contentText').val(),
            userPrompt: $('#userPrompt').val()
        }),
        success: function(res) {
            if (res.code === 200) {
                const chapter = chapters.find(c => c.id === currentChapterId);
                if (chapter) {
                    chapter.content = $('#contentText').val();
                }
                loadChapters();
                loadStats();
            }
        }
    });
}

function saveSummary() {
    if (!currentChapterId) {
        showToast('请先选择一个章节', 'error');
        return;
    }
    
    const summary = $('#summaryText').val();
    
    $.ajax({
        url: '/api/chapters/' + currentChapterId,
        type: 'PUT',
        contentType: 'application/json',
        data: JSON.stringify({ summary: summary }),
        success: function(res) {
            if (res.code === 200) {
                showToast('章节概括保存成功', 'success');
                const chapter = chapters.find(c => c.id === currentChapterId);
                if (chapter) {
                    chapter.summary = summary;
                }
            } else {
                showToast(res.message, 'error');
            }
        },
        error: function() {
            showToast('保存失败', 'error');
        }
    });
}

// 加载章节汇总
function loadCollections() {
    $.get('/api/collections', function(res) {
        if (res.code === 200) {
            collections = res.data;
            renderCollectionList();
        }
    });
}

function renderCollectionList() {
    let html = '';
    collections.forEach(c => {
        const preview = c.content ? c.content.substring(0, 50) + '...' : '';
        html += `
            <div class="collection-item" onclick="showCollectionDetail(${c.id})">
                <div class="coll-title">${c.title}</div>
                <div class="coll-preview">${preview}</div>
            </div>
        `;
    });
    $('#collectionList').html(html || '<div style="padding:20px;color:var(--text-muted);text-align:center;">暂无汇总</div>');
}

function showCollectionDetail(id) {
    const coll = collections.find(c => c.id === id);
    if (coll) {
        $('#collectionDetailId').val(coll.id);
        $('#collectionDetailTitle').text('编辑汇总');
        $('#collectionDetailTitleInput').val(coll.title);
        $('#collectionDetailContent').val(coll.content);
        showModal('collectionDetailModal');
    }
}

function saveCollection() {
    const id = $('#collectionDetailId').val();
    if (!id) {
        showToast('汇总ID不存在', 'error');
        return;
    }
    
    const data = {
        title: $('#collectionDetailTitleInput').val(),
        content: $('#collectionDetailContent').val()
    };
    
    if (!data.title) {
        showToast('请输入汇总标题', 'error');
        return;
    }
    
    $.ajax({
        url: '/api/collections/' + id,
        type: 'PUT',
        contentType: 'application/json',
        data: JSON.stringify(data),
        success: function(res) {
            if (res.code === 200) {
                showToast('汇总保存成功', 'success');
                closeModal('collectionDetailModal');
                loadCollections();
            } else {
                showToast(res.message, 'error');
            }
        },
        error: function() {
            showToast('保存失败', 'error');
        }
    });
}

function deleteCollection() {
    const id = $('#collectionDetailId').val();
    if (!id) {
        showToast('汇总ID不存在', 'error');
        return;
    }
    
    if (confirm('确定要删除这个汇总吗？')) {
        $.ajax({
            url: '/api/collections/' + id,
            type: 'DELETE',
            success: function(res) {
                if (res.code === 200) {
                    showToast('汇总删除成功', 'success');
                    closeModal('collectionDetailModal');
                    loadCollections();
                } else {
                    showToast(res.message, 'error');
                }
            },
            error: function() {
                showToast('删除失败', 'error');
            }
        });
    }
}

// 加载模板
function loadTemplates() {
    $.get('/api/templates', function(res) {
        if (res.code === 200) {
            templates = res.data;
            let html = '<option value="">选择提示词模板</option>';
            templates.filter(t => t.templateType === 'CONTENT').forEach(t => {
                html += `<option value="${t.id}">${t.name}</option>`;
            });
            $('#templateSelect').html(html);
        }
    });
}

// 加载配置
function loadConfigs() {
    $.get('/api/configs', function(res) {
        if (res.code === 200) {
            configs = res.data;
            let html = '<option value="">选择AI配置</option>';
            configs.forEach(c => {
                const def = c.isDefault ? ' (默认)' : '';
                html += `<option value="${c.id}">${c.name}${def}</option>`;
            });
            $('#configSelect').html(html);
        }
    });
}

// 配置管理
function openConfigModal() {
    showConfigList();
    renderConfigList();
    showModal('configModal');
}

function showConfigList() {
    $('#configListSection').show();
    $('#configFormSection').hide();
    renderConfigList();
}

function showConfigForm(id) {
    $('#configListSection').hide();
    $('#configFormSection').show();
    
    if (id) {
        const config = configs.find(c => c.id === id);
        if (config) {
            $('#configFormTitle').text('编辑配置');
            $('#configId').val(config.id);
            $('#configName').val(config.name);
            $('#configApiUrl').val(config.apiUrl);
            $('#configApiKey').val(config.apiKey || '');
            $('#configModel').val(config.model);
            $('#configMaxTokens').val(config.maxTokens);
            $('#configTemperature').val(config.temperature);
            $('#configTopP').val(config.topP);
            $('#configIsDefault').prop('checked', config.isDefault);
        }
    } else {
        $('#configFormTitle').text('新建配置');
        $('#configId').val('');
        $('#configName').val('');
        $('#configApiUrl').val('https://api.openai.com/v1/chat/completions');
        $('#configApiKey').val('');
        $('#configModel').val('gpt-3.5-turbo');
        $('#configMaxTokens').val('4096');
        $('#configTemperature').val('0.7');
        $('#configTopP').val('0.9');
        $('#configIsDefault').prop('checked', false);
    }
}

function renderConfigList() {
    if (configs.length === 0) {
        $('#configListContainer').html(`
            <div class="empty-list">
                <p>暂无AI配置</p>
                <button class="btn-primary" onclick="showConfigForm()" style="padding:8px 16px;">+ 创建第一个配置</button>
            </div>
        `);
        return;
    }
    
    let html = '';
    configs.forEach(c => {
        const defaultBadge = c.isDefault ? '<span class="item-badge">默认</span>' : '';
        html += `
            <div class="config-item">
                <div class="item-header">
                    <span class="item-title">${c.name}</span>
                    ${defaultBadge}
                </div>
                <div class="item-info">
                    <span>🤖 ${c.model || 'gpt-3.5-turbo'}</span>
                    <span>🌡️ ${c.temperature || 0.7}</span>
                    <span>📊 ${c.maxTokens || 4096} tokens</span>
                </div>
                <div class="item-actions">
                    <button class="btn-edit" onclick="showConfigForm(${c.id})">✏️ 编辑</button>
                    ${!c.isDefault ? `<button class="btn-default" onclick="setDefaultConfig(${c.id})">⭐ 设为默认</button>` : ''}
                    <button class="btn-delete" onclick="deleteConfig(${c.id})">🗑️ 删除</button>
                </div>
            </div>
        `;
    });
    $('#configListContainer').html(html);
}

function setDefaultConfig(id) {
    $.ajax({
        url: '/api/configs/' + id,
        type: 'PUT',
        contentType: 'application/json',
        data: JSON.stringify({ isDefault: true }),
        success: function(res) {
            if (res.code === 200) {
                showToast('已设为默认配置', 'success');
                loadConfigs();
                setTimeout(renderConfigList, 300);
            } else {
                showToast(res.message, 'error');
            }
        }
    });
}

function deleteConfig(id) {
    if (confirm('确定要删除这个配置吗？')) {
        $.ajax({
            url: '/api/configs/' + id,
            type: 'DELETE',
            success: function(res) {
                if (res.code === 200) {
                    showToast('配置删除成功', 'success');
                    loadConfigs();
                    setTimeout(renderConfigList, 300);
                } else {
                    showToast(res.message, 'error');
                }
            }
        });
    }
}

function saveConfig() {
    const id = $('#configId').val();
    const data = {
        name: $('#configName').val(),
        apiUrl: $('#configApiUrl').val(),
        apiKey: $('#configApiKey').val(),
        model: $('#configModel').val(),
        maxTokens: parseInt($('#configMaxTokens').val()),
        temperature: parseFloat($('#configTemperature').val()),
        topP: parseFloat($('#configTopP').val()),
        isDefault: $('#configIsDefault').prop('checked'),
        isActive: true
    };
    
    if (!data.name || !data.apiUrl || !data.apiKey) {
        showToast('请填写必要信息', 'error');
        return;
    }
    
    const url = id ? '/api/configs/' + id : '/api/configs';
    const method = id ? 'PUT' : 'POST';
    
    $.ajax({
        url: url,
        type: method,
        contentType: 'application/json',
        data: JSON.stringify(data),
        success: function(res) {
            if (res.code === 200) {
                showToast('配置保存成功', 'success');
                closeModal('configModal');
                loadConfigs();
            } else {
                showToast(res.message, 'error');
            }
        }
    });
}

function testAIConnection() {
    const apiUrl = $('#configApiUrl').val();
    const apiKey = $('#configApiKey').val();
    if (!apiUrl || !apiKey) {
        showToast('请先填写API地址和密钥', 'error');
        return;
    }
    showToast('正在测试连接...', 'success');
    $.post('/api/ai/test', function(res) {
        showToast(res.data || res.message, res.code === 200 ? 'success' : 'error');
    });
}

// 模板管理
function openTemplateModal() {
    showTemplateList();
    renderTemplateList();
    showModal('templateModal');
}

function showTemplateList() {
    $('#templateListSection').show();
    $('#templateFormSection').hide();
    renderTemplateList();
}

function showTemplateForm(id) {
    $('#templateListSection').hide();
    $('#templateFormSection').show();
    
    if (id) {
        const template = templates.find(t => t.id === id);
        if (template) {
            $('#templateFormTitle').text('编辑模板');
            $('#templateId').val(template.id);
            $('#templateName').val(template.name);
            $('#templateType').val(template.templateType);
            $('#templateSystem').val(template.systemPrompt || '');
            $('#templatePrompt').val(template.promptTemplate || '');
            $('#templateIsDefault').prop('checked', template.isDefault);
        }
    } else {
        $('#templateFormTitle').text('新建模板');
        $('#templateId').val('');
        $('#templateName').val('');
        $('#templateType').val('CONTENT');
        $('#templateSystem').val('');
        $('#templatePrompt').val('');
        $('#templateIsDefault').prop('checked', false);
    }
}

function renderTemplateList() {
    if (templates.length === 0) {
        $('#templateListContainer').html(`
            <div class="empty-list">
                <p>暂无提示词模板</p>
                <button class="btn-primary" onclick="showTemplateForm()" style="padding:8px 16px;">+ 创建第一个模板</button>
            </div>
        `);
        return;
    }
    
    const typeNames = {
        'CONTENT': '正文生成',
        'SUMMARY': '章节概括',
        'COLLECTION': '章节汇总',
        'CHARACTER': '角色生成'
    };
    
    let html = '';
    templates.forEach(t => {
        const defaultBadge = t.isDefault ? '<span class="item-badge">默认</span>' : '';
        const typeBadge = `<span class="item-badge" style="background:var(--accent-primary);">${typeNames[t.templateType] || t.templateType}</span>`;
        const preview = t.promptTemplate ? t.promptTemplate.substring(0, 80) + '...' : '无内容';
        html += `
            <div class="template-item">
                <div class="item-header">
                    <span class="item-title">${t.name}</span>
                    <div>${typeBadge} ${defaultBadge}</div>
                </div>
                <div class="item-info">
                    <span style="display:block;color:var(--text-muted);font-size:12px;margin-top:5px;">${preview}</span>
                </div>
                <div class="item-actions">
                    <button class="btn-edit" onclick="showTemplateForm(${t.id})">✏️ 编辑</button>
                    ${!t.isDefault ? `<button class="btn-default" onclick="setDefaultTemplate(${t.id}, '${t.templateType}')">⭐ 设为默认</button>` : ''}
                    <button class="btn-delete" onclick="deleteTemplate(${t.id})">🗑️ 删除</button>
                </div>
            </div>
        `;
    });
    $('#templateListContainer').html(html);
}

function setDefaultTemplate(id, templateType) {
    $.ajax({
        url: '/api/templates/' + id,
        type: 'PUT',
        contentType: 'application/json',
        data: JSON.stringify({ isDefault: true, templateType: templateType }),
        success: function(res) {
            if (res.code === 200) {
                showToast('已设为默认模板', 'success');
                loadTemplates();
                setTimeout(renderTemplateList, 300);
            } else {
                showToast(res.message, 'error');
            }
        }
    });
}

function deleteTemplate(id) {
    if (confirm('确定要删除这个模板吗？')) {
        $.ajax({
            url: '/api/templates/' + id,
            type: 'DELETE',
            success: function(res) {
                if (res.code === 200) {
                    showToast('模板删除成功', 'success');
                    loadTemplates();
                    setTimeout(renderTemplateList, 300);
                } else {
                    showToast(res.message, 'error');
                }
            }
        });
    }
}

function saveTemplate() {
    const id = $('#templateId').val();
    const data = {
        name: $('#templateName').val(),
        templateType: $('#templateType').val(),
        systemPrompt: $('#templateSystem').val(),
        promptTemplate: $('#templatePrompt').val(),
        isDefault: $('#templateIsDefault').prop('checked')
    };
    
    if (!data.name || !data.promptTemplate) {
        showToast('请填写必要信息', 'error');
        return;
    }
    
    const url = id ? '/api/templates/' + id : '/api/templates';
    const method = id ? 'PUT' : 'POST';
    
    $.ajax({
        url: url,
        type: method,
        contentType: 'application/json',
        data: JSON.stringify(data),
        success: function(res) {
            if (res.code === 200) {
                showToast('模板保存成功', 'success');
                closeModal('templateModal');
                loadTemplates();
            } else {
                showToast(res.message, 'error');
            }
        }
    });
}

// 章节概括AI生成
function openSummaryGenerateModal() {
    if (!currentChapterId) {
        showToast('请先选择一个章节', 'error');
        return;
    }
    populateSummarySelects();
    showModal('summaryGenerateModal');
}

function populateSummarySelects() {
    let templateHtml = '<option value="">使用默认模板</option>';
    templates.filter(t => t.templateType === 'SUMMARY').forEach(t => {
        const def = t.isDefault ? ' (默认)' : '';
        templateHtml += `<option value="${t.id}">${t.name}${def}</option>`;
    });
    $('#summaryTemplateSelect').html(templateHtml);
    
    let configHtml = '<option value="">使用默认配置</option>';
    configs.forEach(c => {
        const def = c.isDefault ? ' (默认)' : '';
        configHtml += `<option value="${c.id}">${c.name}${def}</option>`;
    });
    $('#summaryConfigSelect').html(configHtml);
}

function doGenerateSummary() {
    if (!currentChapterId) {
        showToast('请先选择一个章节', 'error');
        return;
    }
    
    const templateId = $('#summaryTemplateSelect').val();
    const configId = $('#summaryConfigSelect').val();
    const isStreamMode = $('#summaryStreamMode').prop('checked');
    
    if (isStreamMode) {
        generateSummaryStream(currentChapterId, templateId, configId);
    } else {
        generateSummaryNormal(currentChapterId, templateId, configId);
    }
}

function generateSummaryNormal(chapterId, templateId, configId) {
    showLoading();
    
    let url = '/api/chapters/' + chapterId + '/summary';
    const params = [];
    if (templateId) params.push('templateId=' + templateId);
    if (configId) params.push('configId=' + configId);
    if (currentBookId) params.push('bookId=' + currentBookId);
    if (params.length > 0) url += '?' + params.join('&');
    
    $.ajax({
        url: url,
        type: 'POST',
        success: function(res) {
            hideLoading();
            if (res.code === 200) {
                $('#summaryText').val(res.data);
                $('#summaryGenerateOutput').val(res.data);
                showToast('章节概括生成成功', 'success');
                saveSummary();
            } else {
                showToast(res.message, 'error');
                $('#summaryGenerateOutput').val('生成失败: ' + res.message);
            }
        },
        error: function() {
            hideLoading();
            showToast('生成失败', 'error');
            $('#summaryGenerateOutput').val('生成失败，请检查AI配置');
        }
    });
}

function generateSummaryStream(chapterId, templateId, configId) {
    streamAbortController = new AbortController();
    
    $('#summaryGenerateBtn').prop('disabled', true);
    $('#summaryStopStreamBtn').show();
    $('#summaryGenerateOutput').val('');
    showToast('正在生成章节概括...', 'success');
    
    let fullContent = '';
    let buffer = '';
    
    let url = '/api/chapters/' + chapterId + '/summary/stream';
    const params = [];
    if (templateId) params.push('templateId=' + templateId);
    if (configId) params.push('configId=' + configId);
    if (params.length > 0) url += '?' + params.join('&');
    
    fetch(url, {
        method: 'POST',
        signal: streamAbortController.signal
    }).then(response => {
        if (!response.ok) {
            throw new Error('请求失败: ' + response.status);
        }
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        
        function read() {
            reader.read().then(({done, value}) => {
                if (done) {
                    onSummaryStreamComplete(fullContent);
                    return;
                }
                
                const chunk = decoder.decode(value, {stream: true});
                buffer += chunk;
                
                const lines = buffer.split('\n');
                buffer = lines.pop() || '';
                
                lines.forEach(line => {
                    line = line.trim();
                    if (!line) return;
                    
                    if (line.startsWith('data:')) {
                        const data = line.substring(5).trim();
                        
                        if (data === '[DONE]') return;
                        if (data === '[ERROR]' || data.startsWith('[ERROR]')) {
                            showToast('生成出错: ' + data, 'error');
                            return;
                        }
                        
                        try {
                            const json = JSON.parse(data);
                            if (json.choices && json.choices[0]) {
                                const choice = json.choices[0];
                                if (choice.delta && choice.delta.content) {
                                    fullContent += choice.delta.content;
                                    $('#summaryGenerateOutput').val(fullContent);
                                    const textarea = document.getElementById('summaryGenerateOutput');
                                    textarea.scrollTop = textarea.scrollHeight;
                                } else if (choice.message && choice.message.content) {
                                    fullContent = choice.message.content;
                                    $('#summaryGenerateOutput').val(fullContent);
                                }
                            }
                        } catch(e) {
                            if (data && data !== '[DONE]') {
                                fullContent += data;
                                $('#summaryGenerateOutput').val(fullContent);
                            }
                        }
                    }
                });
                
                read();
            }).catch(error => {
                if (error.name === 'AbortError') {
                    showToast('已停止生成', 'success');
                    onSummaryStreamComplete(fullContent);
                } else {
                    onSummaryStreamError(error);
                }
            });
        }
        
        read();
    }).catch(error => {
        if (error.name === 'AbortError') {
            showToast('已停止生成', 'success');
            onSummaryStreamComplete(fullContent);
        } else {
            onSummaryStreamError(error);
        }
    });
}

function onSummaryStreamComplete(fullContent) {
    $('#summaryGenerateBtn').prop('disabled', false);
    $('#summaryStopStreamBtn').hide();
    streamAbortController = null;
    
    if (fullContent) {
        showToast('章节概括生成完成！', 'success');
        $('#summaryText').val(fullContent);
        saveSummary();
    }
}

function onSummaryStreamError(error) {
    $('#summaryGenerateBtn').prop('disabled', false);
    $('#summaryStopStreamBtn').hide();
    streamAbortController = null;
    showToast('生成失败: ' + error.message, 'error');
    console.error('流式生成错误:', error);
}

function stopSummaryStream() {
    if (streamAbortController) {
        streamAbortController.abort();
        showToast('正在停止生成...', 'success');
    }
}

// 章节汇总AI生成
function openCollectionGenerateModal() {
    populateCollectionSelects();
    renderCollectionChapterList();
    showModal('collectionGenerateModal');
}

function renderCollectionChapterList() {
    if (chapters.length === 0) {
        $('#collectionChapterList').html('<div style="color:var(--text-muted);text-align:center;padding:20px;">暂无章节</div>');
        updateSelectedChapterCount();
        return;
    }
    
    let html = '';
    chapters.forEach(ch => {
        html += `
            <label style="display:flex;align-items:center;padding:8px;margin-bottom:5px;background:white;border-radius:4px;cursor:pointer;border:1px solid var(--border-color);">
                <input type="checkbox" class="collection-chapter-checkbox" value="${ch.id}" data-order="${ch.chapterOrder}" style="margin-right:10px;">
                <span>第${ch.chapterOrder}章 ${ch.title}</span>
            </label>
        `;
    });
    $('#collectionChapterList').html(html);
    
    $('.collection-chapter-checkbox').on('change', updateSelectedChapterCount);
    updateSelectedChapterCount();
}

function selectAllChaptersForCollection() {
    $('.collection-chapter-checkbox').prop('checked', true);
    updateSelectedChapterCount();
}

function deselectAllChaptersForCollection() {
    $('.collection-chapter-checkbox').prop('checked', false);
    updateSelectedChapterCount();
}

function updateSelectedChapterCount() {
    const count = $('.collection-chapter-checkbox:checked').length;
    $('#selectedChapterCount').text('已选择 ' + count + ' 章');
}

function getSelectedChapterIds() {
    const selectedIds = [];
    $('.collection-chapter-checkbox:checked').each(function() {
        selectedIds.push({
            id: parseInt($(this).val()),
            order: parseInt($(this).data('order'))
        });
    });
    selectedIds.sort((a, b) => a.order - b.order);
    return selectedIds.map(item => item.id);
}

function populateCollectionSelects() {
    let templateHtml = '<option value="">使用默认模板</option>';
    templates.filter(t => t.templateType === 'COLLECTION').forEach(t => {
        const def = t.isDefault ? ' (默认)' : '';
        templateHtml += `<option value="${t.id}">${t.name}${def}</option>`;
    });
    $('#collectionTemplateSelect').html(templateHtml);
    
    let configHtml = '<option value="">使用默认配置</option>';
    configs.forEach(c => {
        const def = c.isDefault ? ' (默认)' : '';
        configHtml += `<option value="${c.id}">${c.name}${def}</option>`;
    });
    $('#collectionConfigSelect').html(configHtml);
}

function doGenerateCollection() {
    const templateId = $('#collectionTemplateSelect').val();
    const configId = $('#collectionConfigSelect').val();
    const chapterIds = getSelectedChapterIds();
    const isStreamMode = $('#collectionStreamMode').prop('checked');
    
    if (chapterIds.length === 0) {
        showToast('请至少选择一个章节', 'error');
        return;
    }
    
    if (isStreamMode) {
        generateCollectionStream(templateId, configId, chapterIds);
    } else {
        generateCollectionNormal(templateId, configId, chapterIds);
    }
}

function generateCollectionNormal(templateId, configId, chapterIds) {
    showLoading();
    
    let url = '/api/collections/generate';
    const params = [];
    if (templateId) params.push('templateId=' + templateId);
    if (configId) params.push('configId=' + configId);
    params.push('chapterIds=' + chapterIds.join(','));
    if (params.length > 0) url += '?' + params.join('&');
    
    $.ajax({
        url: url,
        type: 'POST',
        success: function(res) {
            hideLoading();
            if (res.code === 200) {
                $('#collectionGenerateOutput').val(res.data.content);
                showToast('章节汇总生成成功', 'success');
                loadCollections();
            } else {
                showToast(res.message, 'error');
                $('#collectionGenerateOutput').val('生成失败: ' + res.message);
            }
        },
        error: function() {
            hideLoading();
            showToast('生成失败', 'error');
            $('#collectionGenerateOutput').val('生成失败，请检查AI配置');
        }
    });
}

function generateCollectionStream(templateId, configId, chapterIds) {
    streamAbortController = new AbortController();
    
    $('#collectionGenerateBtn').prop('disabled', true);
    $('#collectionStopStreamBtn').show();
    $('#collectionGenerateOutput').val('');
    showToast('正在生成章节汇总...', 'success');
    
    let fullContent = '';
    let buffer = '';
    
    let url = '/api/collections/generate/stream';
    const params = [];
    if (templateId) params.push('templateId=' + templateId);
    if (configId) params.push('configId=' + configId);
    params.push('chapterIds=' + chapterIds.join(','));
    if (params.length > 0) url += '?' + params.join('&');
    
    fetch(url, {
        method: 'POST',
        signal: streamAbortController.signal
    }).then(response => {
        if (!response.ok) {
            throw new Error('请求失败: ' + response.status);
        }
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        
        function read() {
            reader.read().then(({done, value}) => {
                if (done) {
                    onCollectionStreamComplete(fullContent);
                    return;
                }
                
                const chunk = decoder.decode(value, {stream: true});
                buffer += chunk;
                
                const lines = buffer.split('\n');
                buffer = lines.pop() || '';
                
                lines.forEach(line => {
                    line = line.trim();
                    if (!line) return;
                    
                    if (line.startsWith('data:')) {
                        const data = line.substring(5).trim();
                        
                        if (data === '[DONE]') return;
                        if (data === '[ERROR]' || data.startsWith('[ERROR]')) {
                            showToast('生成出错: ' + data, 'error');
                            return;
                        }
                        
                        try {
                            const json = JSON.parse(data);
                            if (json.choices && json.choices[0]) {
                                const choice = json.choices[0];
                                if (choice.delta && choice.delta.content) {
                                    fullContent += choice.delta.content;
                                    $('#collectionGenerateOutput').val(fullContent);
                                    const textarea = document.getElementById('collectionGenerateOutput');
                                    textarea.scrollTop = textarea.scrollHeight;
                                } else if (choice.message && choice.message.content) {
                                    fullContent = choice.message.content;
                                    $('#collectionGenerateOutput').val(fullContent);
                                }
                            }
                        } catch(e) {
                            if (data && data !== '[DONE]') {
                                fullContent += data;
                                $('#collectionGenerateOutput').val(fullContent);
                            }
                        }
                    }
                });
                
                read();
            }).catch(error => {
                if (error.name === 'AbortError') {
                    showToast('已停止生成', 'success');
                    onCollectionStreamComplete(fullContent);
                } else {
                    onCollectionStreamError(error);
                }
            });
        }
        
        read();
    }).catch(error => {
        if (error.name === 'AbortError') {
            showToast('已停止生成', 'success');
            onCollectionStreamComplete(fullContent);
        } else {
            onCollectionStreamError(error);
        }
    });
}

function onCollectionStreamComplete(fullContent) {
    $('#collectionGenerateBtn').prop('disabled', false);
    $('#collectionStopStreamBtn').hide();
    streamAbortController = null;
    
    if (fullContent) {
        showToast('章节汇总生成完成！', 'success');
        loadCollections();
    }
}

function onCollectionStreamError(error) {
    $('#collectionGenerateBtn').prop('disabled', false);
    $('#collectionStopStreamBtn').hide();
    streamAbortController = null;
    showToast('生成失败: ' + error.message, 'error');
    console.error('流式生成错误:', error);
}

function stopCollectionStream() {
    if (streamAbortController) {
        streamAbortController.abort();
        showToast('正在停止生成...', 'success');
    }
}

// 角色信息AI生成
function openCharacterGenerateModal(characterId) {
    $('#generateCharacterId').val(characterId || '');
    $('#characterGeneratePrompt').val('');
    
    if (characterId) {
        const char = characters.find(c => c.id === characterId);
        if (char) {
            $('#characterGenerateTitle').text('🤖 AI完善角色：' + char.name);
        }
    } else {
        $('#characterGenerateTitle').text('🤖 AI生成新角色');
    }
    
    populateCharacterSelects();
    showModal('characterGenerateModal');
}

function populateCharacterSelects() {
    let templateHtml = '<option value="">使用默认模板</option>';
    templates.filter(t => t.templateType === 'CHARACTER').forEach(t => {
        const def = t.isDefault ? ' (默认)' : '';
        templateHtml += `<option value="${t.id}">${t.name}${def}</option>`;
    });
    $('#characterTemplateSelect').html(templateHtml);
    
    let configHtml = '<option value="">使用默认配置</option>';
    configs.forEach(c => {
        const def = c.isDefault ? ' (默认)' : '';
        configHtml += `<option value="${c.id}">${c.name}${def}</option>`;
    });
    $('#characterConfigSelect').html(configHtml);
}

function doGenerateCharacter() {
    const characterId = $('#generateCharacterId').val();
    const userPrompt = $('#characterGeneratePrompt').val();
    const templateId = $('#characterTemplateSelect').val();
    const configId = $('#characterConfigSelect').val();
    const isStreamMode = $('#characterStreamMode').prop('checked');
    
    if (!userPrompt) {
        showToast('请输入角色描述/需求', 'error');
        return;
    }
    
    if (isStreamMode) {
        generateCharacterStream(characterId, userPrompt, templateId, configId);
    } else {
        generateCharacterNormal(characterId, userPrompt, templateId, configId);
    }
}

function generateCharacterNormal(characterId, userPrompt, templateId, configId) {
    showLoading();
    
    let url = characterId ? '/api/characters/' + characterId + '/generate' : '/api/characters/generate';
    const params = [];
    params.push('userPrompt=' + encodeURIComponent(userPrompt));
    if (templateId) params.push('templateId=' + templateId);
    if (configId) params.push('configId=' + configId);
    if (params.length > 0) url += '?' + params.join('&');
    
    $.ajax({
        url: url,
        type: 'POST',
        success: function(res) {
            hideLoading();
            if (res.code === 200) {
                $('#characterGenerateOutput').val(res.data);
                showToast('角色信息生成成功', 'success');
                showCharacterGenerateResult(res.data, characterId);
            } else {
                showToast(res.message, 'error');
                $('#characterGenerateOutput').val('生成失败: ' + res.message);
            }
        },
        error: function() {
            hideLoading();
            showToast('生成失败', 'error');
            $('#characterGenerateOutput').val('生成失败，请检查AI配置');
        }
    });
}

function generateCharacterStream(characterId, userPrompt, templateId, configId) {
    streamAbortController = new AbortController();
    
    $('#characterGenerateBtn').prop('disabled', true);
    $('#characterStopStreamBtn').show();
    $('#characterGenerateOutput').val('');
    showToast('正在生成角色信息...', 'success');
    
    let fullContent = '';
    let buffer = '';
    
    let url = '/api/characters/generate/stream';
    const params = [];
    params.push('userPrompt=' + encodeURIComponent(userPrompt));
    if (templateId) params.push('templateId=' + templateId);
    if (configId) params.push('configId=' + configId);
    if (params.length > 0) url += '?' + params.join('&');
    
    fetch(url, {
        method: 'POST',
        signal: streamAbortController.signal
    }).then(response => {
        if (!response.ok) {
            throw new Error('请求失败: ' + response.status);
        }
        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        
        function read() {
            reader.read().then(({done, value}) => {
                if (done) {
                    onCharacterStreamComplete(fullContent, characterId);
                    return;
                }
                
                const chunk = decoder.decode(value, {stream: true});
                buffer += chunk;
                
                const lines = buffer.split('\n');
                buffer = lines.pop() || '';
                
                lines.forEach(line => {
                    line = line.trim();
                    if (!line) return;
                    
                    if (line.startsWith('data:')) {
                        const data = line.substring(5).trim();
                        
                        if (data === '[DONE]') return;
                        if (data === '[ERROR]' || data.startsWith('[ERROR]')) {
                            showToast('生成出错: ' + data, 'error');
                            return;
                        }
                        
                        try {
                            const json = JSON.parse(data);
                            if (json.choices && json.choices[0]) {
                                const choice = json.choices[0];
                                if (choice.delta && choice.delta.content) {
                                    fullContent += choice.delta.content;
                                    $('#characterGenerateOutput').val(fullContent);
                                    const textarea = document.getElementById('characterGenerateOutput');
                                    textarea.scrollTop = textarea.scrollHeight;
                                } else if (choice.message && choice.message.content) {
                                    fullContent = choice.message.content;
                                    $('#characterGenerateOutput').val(fullContent);
                                }
                            }
                        } catch(e) {
                            if (data && data !== '[DONE]') {
                                fullContent += data;
                                $('#characterGenerateOutput').val(fullContent);
                            }
                        }
                    }
                });
                
                read();
            }).catch(error => {
                if (error.name === 'AbortError') {
                    showToast('已停止生成', 'success');
                    onCharacterStreamComplete(fullContent, characterId);
                } else {
                    onCharacterStreamError(error);
                }
            });
        }
        
        read();
    }).catch(error => {
        if (error.name === 'AbortError') {
            showToast('已停止生成', 'success');
            onCharacterStreamComplete(fullContent, characterId);
        } else {
            onCharacterStreamError(error);
        }
    });
}

function onCharacterStreamComplete(fullContent, characterId) {
    $('#characterGenerateBtn').prop('disabled', false);
    $('#characterStopStreamBtn').hide();
    streamAbortController = null;
    
    if (fullContent) {
        showToast('角色信息生成完成！', 'success');
        showCharacterGenerateResult(fullContent, characterId);
    }
}

function onCharacterStreamError(error) {
    $('#characterGenerateBtn').prop('disabled', false);
    $('#characterStopStreamBtn').hide();
    streamAbortController = null;
    showToast('生成失败: ' + error.message, 'error');
    console.error('流式生成错误:', error);
}

function stopCharacterStream() {
    if (streamAbortController) {
        streamAbortController.abort();
        showToast('正在停止生成...', 'success');
    }
}

function showCharacterGenerateResult(result, characterId) {
    try {
        const charData = typeof result === 'string' ? JSON.parse(result) : result;
        
        if (characterId) {
            $('#characterId').val(characterId);
            $('#characterModalTitle').text('编辑角色（AI生成）');
        } else {
            $('#characterId').val('');
            $('#characterModalTitle').text('新建角色（AI生成）');
        }
        
        if (charData.name) $('#charName').val(charData.name);
        
        // 将AI生成的角色信息整合到charInfo框中
        let charInfoText = '';
        if (charData.role) charInfoText += '【角色定位】' + charData.role + '\n';
        if (charData.status) charInfoText += '【角色状态】' + charData.status + '\n';
        if (charData.ability) charInfoText += '【角色能力】' + charData.ability + '\n';
        if (charData.appearance) charInfoText += '【外貌描述】' + charData.appearance + '\n';
        if (charData.personality) charInfoText += '【性格特点】' + charData.personality + '\n';
        if (charData.background) charInfoText += '【背景故事】' + charData.background + '\n';
        
        $('#charInfo').val(charInfoText);
        if (charData.isMain !== undefined) $('#charIsMain').prop('checked', charData.isMain);
        
        showModal('characterModal');
    } catch (e) {
        console.log(e,'AI生成结果：\n\n' + result);
    }
}

// 更新字数统计（只统计汉字）
function updateWordCount() {
    const content = $('#contentText').val() || '';
    const chineseChars = content.match(/[\u4e00-\u9fa5]/g);
    const count = chineseChars ? chineseChars.length : 0;
    $('#wordCount').text(count);
}

// 弹窗操作
function showModal(id) {
    $('#' + id).addClass('show');
}

function closeModal(id) {
    $('#' + id).removeClass('show');
}

// 加载动画
function showLoading() {
    $('#loading').addClass('show');
    $('#generateBtn').prop('disabled', true);
}

function hideLoading() {
    $('#loading').removeClass('show');
    $('#generateBtn').prop('disabled', false);
}

// 提示消息
function showToast(message, type) {
    const toast = $(`<div class="toast ${type}">${message}</div>`);
    $('body').append(toast);
    setTimeout(() => toast.remove(), 3000);
}

// 变量帮助弹框功能
const variableConfigs = {
    template: {
        title: '📋 模板可用变量',
        variables: [
            { name: '{{userPrompt}}', desc: '用户输入的创作要求' },
            { name: '{{characters}}', desc: '角色信息列表' },
            { name: '{{prevSummary}}', desc: '前几章的概括内容' },
            { name: '{{title}}', desc: '当前章节标题' },
            { name: '{{content}}', desc: '当前章节正文内容' },
            { name: '{{chapterOrder}}', desc: '当前章节序号' },
            { name: '{{summaries}}', desc: '所有章节概括' },
            { name: '{{chapterCount}}', desc: '章节总数' },
            { name: '{{totalWords}}', desc: '总字数统计' }
        ]
    },
    summary: {
        title: '📋 章节概括可用变量',
        variables: [
            { name: '{{title}}', desc: '章节标题' },
            { name: '{{content}}', desc: '章节正文内容' },
            { name: '{{chapterOrder}}', desc: '章节序号' },
            { name: '{{prevSummary}}', desc: '前几章概括' },
            { name: '{{characters}}', desc: '角色信息' }
        ]
    },
    collection: {
        title: '📋 章节汇总可用变量',
        variables: [
            { name: '{{selectedContents}}', desc: '选中章节的正文内容（按顺序拼接）' },
            { name: '{{selectedSummaries}}', desc: '选中章节的概括（按顺序拼接）' },
            { name: '{{summaries}}', desc: '所有章节概括' },
            { name: '{{characters}}', desc: '角色信息' },
            { name: '{{chapterCount}}', desc: '章节总数' },
            { name: '{{totalWords}}', desc: '总字数统计' }
        ]
    },
    character: {
        title: '📋 角色生成可用变量',
        variables: [
            { name: '{{userPrompt}}', desc: '用户输入的角色需求' },
            { name: '{{characterName}}', desc: '角色名称（编辑时可用）' },
            { name: '{{characterInfo}}', desc: '现有角色信息（编辑时可用）' },
            { name: '{{existingCharacters}}', desc: '已有角色列表' },
            { name: '{{recentSummary}}', desc: '最近章节概括' }
        ]
    }
};

function showVariableHelp(type) {
    const config = variableConfigs[type];
    if (!config) {
        showToast('未知的变量类型', 'error');
        return;
    }
    
    $('#variableHelpTitle').text(config.title);
    
    let html = '';
    config.variables.forEach(v => {
        html += `
            <div class="variable-item">
                <div class="variable-info">
                    <div class="variable-name">${v.name}</div>
                    <div class="variable-desc">${v.desc}</div>
                </div>
                <button class="variable-copy-btn" onclick="copyVariable('${v.name}', this)">复制</button>
            </div>
        `;
    });
    $('#variableListContainer').html(html);
    
    showModal('variableHelpModal');
}

function copyVariable(variable, btn) {
    if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(variable).then(() => {
            showCopySuccess(btn);
        }).catch(err => {
            fallbackCopy(variable, btn);
        });
    } else {
        fallbackCopy(variable, btn);
    }
}

function fallbackCopy(text, btn) {
    const textarea = document.createElement('textarea');
    textarea.value = text;
    textarea.style.position = 'fixed';
    textarea.style.left = '-9999px';
    document.body.appendChild(textarea);
    textarea.select();
    try {
        document.execCommand('copy');
        showCopySuccess(btn);
    } catch (err) {
        showToast('复制失败，请手动复制', 'error');
    }
    document.body.removeChild(textarea);
}

function showCopySuccess(btn) {
    const $btn = $(btn);
    const originalText = $btn.text();
    $btn.text('已复制!').addClass('copied');
    setTimeout(() => {
        $btn.text(originalText).removeClass('copied');
    }, 1500);
    showToast('变量已复制到剪贴板', 'success');
}

// ==================== 书籍管理功能 ====================

// 加载书籍列表
function loadBooks() {
    $.get('/api/books', function(res) {
        if (res.code === 200) {
            books = res.data;
            renderBookSelect();
            // 检查是否有激活的书籍
            const activeBook = books.find(b => b.isActive);
            if (activeBook) {
                currentBookId = activeBook.id;
                $('#bookSelect').val(activeBook.id);
                // 书籍加载完成后再加载世界书
                loadWorldBooks();
            }
        }
    });
}

// 渲染书籍下拉选择器
function renderBookSelect() {
    let html = '<option value="">选择书籍</option>';
    books.forEach(b => {
        const active = b.isActive ? ' (当前)' : '';
        html += `<option value="${b.id}"${b.isActive ? ' selected' : ''}>${b.name}${active}</option>`;
    });
    $('#bookSelect').html(html);
}

// 切换书籍
function switchBook(bookId) {
    if (!bookId) return;
    
    $.ajax({
        url: '/api/books/' + bookId + '/switch',
        type: 'POST',
        success: function(res) {
            if (res.code === 200) {
                currentBookId = bookId;
                showToast(res.message, 'success');
                loadBooks();
                loadAllData(); // 重新加载所有数据
                
                // 等待数据加载完成后恢复缓存的下拉选择
                setTimeout(function() {
                    applyCachedSelections();
                }, 500);
            } else {
                showToast(res.message, 'error');
            }
        },
        error: function() {
            showToast('切换书籍失败', 'error');
        }
    });
}

// 打开书籍管理弹窗
function openBookModal() {
    showBookList();
    renderBookList();
    showModal('bookModal');
}

// 显示书籍列表
function showBookList() {
    $('#bookListSection').show();
    $('#bookFormSection').hide();
    renderBookList();
}

// 显示书籍表单
function showBookForm(id) {
    $('#bookListSection').hide();
    $('#bookFormSection').show();
    
    if (id) {
        const book = books.find(b => b.id === id);
        if (book) {
            $('#bookFormTitle').text('编辑书籍');
            $('#bookId').val(book.id);
            $('#bookName').val(book.name);
            $('#bookAuthor').val(book.author || '');
            $('#bookGenre').val(book.genre || '');
            $('#bookDescription').val(book.description || '');
        }
    } else {
        $('#bookFormTitle').text('新建书籍');
        $('#bookId').val('');
        $('#bookName').val('');
        $('#bookAuthor').val('');
        $('#bookGenre').val('');
        $('#bookDescription').val('');
    }
}

// 渲染书籍列表
function renderBookList() {
    if (books.length === 0) {
        $('#bookListContainer').html(`
            <div class="empty-list">
                <p>暂无书籍</p>
                <button class="btn-primary" onclick="showBookForm()" style="padding:8px 16px;">+ 创建第一本书</button>
            </div>
        `);
        return;
    }
    
    let html = '';
    books.forEach(b => {
        const activeBadge = b.isActive ? '<span class="item-badge">当前</span>' : '';
        const genre = b.genre ? `<span>📚 ${b.genre}</span>` : '';
        const author = b.author ? `<span>✍️ ${b.author}</span>` : '';
        html += `
            <div class="config-item" style="${b.isActive ? 'border-color:var(--accent-primary);' : ''}">
                <div class="item-header">
                    <span class="item-title">${b.name}</span>
                    ${activeBadge}
                </div>
                <div class="item-info">
                    ${author}
                    ${genre}
                </div>
                <div class="item-actions">
                    <button class="btn-edit" onclick="showBookForm(${b.id})">✏️ 编辑</button>
                    ${!b.isActive ? `<button class="btn-default" onclick="switchBook(${b.id});closeModal('bookModal');">📖 切换</button>` : ''}
                    <button class="btn-delete" onclick="deleteBook(${b.id})">🗑️ 删除</button>
                </div>
            </div>
        `;
    });
    $('#bookListContainer').html(html);
}

// 保存书籍
function saveBook() {
    const id = $('#bookId').val();
    const data = {
        name: $('#bookName').val(),
        author: $('#bookAuthor').val(),
        genre: $('#bookGenre').val(),
        description: $('#bookDescription').val()
    };
    
    if (!data.name) {
        showToast('请输入书籍名称', 'error');
        return;
    }
    
    const url = id ? '/api/books/' + id : '/api/books';
    const method = id ? 'PUT' : 'POST';
    
    $.ajax({
        url: url,
        type: method,
        contentType: 'application/json',
        data: JSON.stringify(data),
        success: function(res) {
            if (res.code === 200) {
                showToast('书籍保存成功', 'success');
                showBookList();
                loadBooks();
            } else {
                showToast(res.message, 'error');
            }
        },
        error: function() {
            showToast('保存失败', 'error');
        }
    });
}

// 删除书籍
function deleteBook(id) {
    const book = books.find(b => b.id === id);
    if (book && book.isActive) {
        showToast('不能删除当前激活的书籍', 'error');
        return;
    }

    if (confirm('确定要删除这本书吗？相关的章节、角色等数据也会被删除！')) {
        $.ajax({
            url: '/api/books/' + id,
            type: 'DELETE',
            success: function(res) {
                if (res.code === 200) {
                    showToast('书籍删除成功', 'success');
                    loadBooks();
                    renderBookList();
                } else {
                    showToast(res.message, 'error');
                }
            },
            error: function() {
                showToast('删除失败', 'error');
            }
        });
    }
}

// ==================== 世界书功能 ====================

let worldBooks = [];

// 加载世界书列表
function loadWorldBooks() {
    if (!currentBookId) {
        $('#worldBookList').html('<div class="empty-hint">请先选择书籍</div>');
        $('#worldBookListContainer').html('<div class="empty-list"><p>请先选择书籍</p></div>');
        return;
    }
    $.get('/api/worldbooks?book=' + currentBookId, function(res) {
        if (res.code === 200) {
            worldBooks = res.data || [];
            renderWorldBookList();
            renderWorldBookListModal();
        }
    });
}

// 渲染左侧栏世界书列表（简略版）
function renderWorldBookList() {
    if (worldBooks.length === 0) {
        $('#worldBookList').html('<div class="empty-hint">暂无世界书条目</div>');
        return;
    }
    let html = '';
    worldBooks.forEach(wb => {
        const activeClass = wb.isActive ? '' : 'inactive';
        const activeIcon = wb.isActive ? '✅' : '⏸️';
        html += `
            <div class="character-item ${activeClass}" onclick="openWorldBookListModal()">
                <span class="char-name">${activeIcon} ${wb.title}</span>
                <span class="char-role" style="font-size:11px;color:var(--text-muted);">${(wb.content || '').substring(0, 30)}...</span>
            </div>
        `;
    });
    $('#worldBookList').html(html);
}

// 渲染世界书管理弹框列表（详细版）
function renderWorldBookListModal() {
    if (worldBooks.length === 0) {
        $('#worldBookListContainer').html(`
            <div class="empty-list">
                <p>暂无世界书条目</p>
                <button class="btn-primary" onclick="openWorldBookModal()" style="padding:8px 16px;">+ 创建第一个条目</button>
            </div>
        `);
        return;
    }
    
    let html = '';
    worldBooks.forEach(wb => {
        const activeBadge = wb.isActive ? '<span class="item-badge" style="background:var(--accent-success);">启用</span>' : '<span class="item-badge" style="background:var(--text-muted);">禁用</span>';
        const preview = wb.content ? wb.content.substring(0, 80) + '...' : '无内容';
        html += `
            <div class="config-item worldbook-item" style="${!wb.isActive ? 'opacity:0.6;' : ''}">
                <div class="item-header">
                    <span class="item-title">🏷️ ${wb.title}</span>
                    ${activeBadge}
                </div>
                <div class="item-info">
                    <span style="display:block;color:var(--text-muted);font-size:12px;margin-top:5px;line-height:1.5;">${preview}</span>
                </div>
                <div class="item-actions">
                    <button class="btn-edit" onclick="editWorldBookFromList(${wb.id})">✏️ 编辑</button>
                    <button class="btn-toggle" onclick="toggleWorldBookActive(${wb.id}, ${!wb.isActive})">${wb.isActive ? '⏸️ 禁用' : '✅ 启用'}</button>
                    <button class="btn-delete" onclick="deleteWorldBookFromList(${wb.id})">🗑️ 删除</button>
                </div>
            </div>
        `;
    });
    $('#worldBookListContainer').html(html);
}

// 打开世界书管理弹框
function openWorldBookListModal() {
    if (!currentBookId) {
        showToast('请先选择书籍', 'error');
        return;
    }
    renderWorldBookListModal();
    showModal('worldBookListModal');
}

// 打开世界书编辑弹窗（新建）
function openWorldBookModal() {
    if (!currentBookId) {
        showToast('请先选择书籍', 'error');
        return;
    }
    $('#worldBookModalTitle').text('新建世界书条目');
    $('#worldBookId').val('');
    $('#worldBookTitle').val('');
    $('#worldBookContent').val('');
    $('#worldBookSortOrder').val(0);
    $('#worldBookIsActive').prop('checked', true);
    $('#deleteWorldBookBtn').hide();
    showModal('worldBookModal');
}

// 从列表中编辑世界书
function editWorldBookFromList(id) {
    const wb = worldBooks.find(w => w.id === id);
    if (!wb) return;
    
    $('#worldBookModalTitle').text('编辑世界书条目');
    $('#worldBookId').val(wb.id);
    $('#worldBookTitle').val(wb.title);
    $('#worldBookContent').val(wb.content);
    $('#worldBookSortOrder').val(wb.sortOrder || 0);
    $('#worldBookIsActive').prop('checked', wb.isActive !== false);
    $('#deleteWorldBookBtn').show();
    showModal('worldBookModal');
}

// 编辑世界书（旧方法保留兼容）
function editWorldBook(id) {
    editWorldBookFromList(id);
}

// 关闭世界书编辑弹框并返回列表
function closeWorldBookEditModal() {
    closeModal('worldBookModal');
}

// 切换世界书启用状态
function toggleWorldBookActive(id, newStatus) {
    $.ajax({
        url: '/api/worldbooks/' + id,
        type: 'PUT',
        contentType: 'application/json',
        data: JSON.stringify({ isActive: newStatus }),
        success: function(res) {
            if (res.code === 200) {
                showToast(newStatus ? '已启用' : '已禁用', 'success');
                loadWorldBooks();
            } else {
                showToast(res.message, 'error');
            }
        },
        error: function() {
            showToast('操作失败', 'error');
        }
    });
}

// 保存世界书
function saveWorldBook() {
    const id = $('#worldBookId').val();
    const data = {
        title: $('#worldBookTitle').val(),
        content: $('#worldBookContent').val(),
        sortOrder: parseInt($('#worldBookSortOrder').val()) || 0,
        isActive: $('#worldBookIsActive').prop('checked'),
        bookId: currentBookId
    };
    
    if (!data.title) {
        showToast('请输入触发词/标题', 'error');
        return;
    }
    if (!data.content) {
        showToast('请输入世界书内容', 'error');
        return;
    }
    
    const url = id ? '/api/worldbooks/' + id : '/api/worldbooks';
    const method = id ? 'PUT' : 'POST';
    
    $.ajax({
        url: url,
        type: method,
        contentType: 'application/json',
        data: JSON.stringify(data),
        success: function(res) {
            if (res.code === 200) {
                showToast('世界书保存成功', 'success');
                closeModal('worldBookModal');
                loadWorldBooks();
            } else {
                showToast(res.message, 'error');
            }
        },
        error: function() {
            showToast('保存失败', 'error');
        }
    });
}

// 从列表中删除世界书
function deleteWorldBookFromList(id) {
    if (confirm('确定要删除这个世界书条目吗？')) {
        $.ajax({
            url: '/api/worldbooks/' + id,
            type: 'DELETE',
            success: function(res) {
                if (res.code === 200) {
                    showToast('世界书删除成功', 'success');
                    loadWorldBooks();
                } else {
                    showToast(res.message, 'error');
                }
            },
            error: function() {
                showToast('删除失败', 'error');
            }
        });
    }
}

// 删除世界书（从编辑弹框）
function deleteWorldBook() {
    const id = $('#worldBookId').val();
    if (!id) return;
    
    if (confirm('确定要删除这个世界书条目吗？')) {
        $.ajax({
            url: '/api/worldbooks/' + id,
            type: 'DELETE',
            success: function(res) {
                if (res.code === 200) {
                    showToast('世界书删除成功', 'success');
                    closeModal('worldBookModal');
                    loadWorldBooks();
                } else {
                    showToast(res.message, 'error');
                }
            },
            error: function() {
                showToast('删除失败', 'error');
            }
        });
    }
}
