# AI网文创作系统

一个基于Spring Boot + H2数据库 + jQuery的个人AI网文创作系统，支持通过AI辅助生成超长篇网文小说。

## 功能特性

### 核心功能
- **章节管理**: 创建、编辑、删除章节，点击加载对应内容
- **角色管理**: 管理小说角色信息（姓名、外貌、性格、背景）
- **AI内容生成**: 调用OpenAI兼容接口生成正文、概括、汇总
- **提示词模板**: 自定义不同栏目的AI提示词模板
- **AI配置管理**: 支持多套AI配置，可调节温度、TopP等参数

### 页面布局
- **左上-标题栏**: 章节列表，支持点击切换、编辑、删除
- **左下-角色信息栏**: 角色列表，点击弹框编辑
- **中上-用户提示栏**: 输入创作要求发送给AI
- **中下-正文栏**: 展示AI生成的正文，带字数统计和重新生成
- **右上-章节概括栏**: 当前章节的AI生成概括
- **右下-章节汇总栏**: 多章节的汇总信息

## 技术栈

- **后端**: Java 11 + Spring Boot 2.7.18
- **数据库**: H2嵌入式数据库（数据持久化到文件）
- **前端**: HTML + CSS + JavaScript + jQuery
- **HTTP客户端**: OkHttp（调用AI接口）

## 快速开始

### 1. 环境要求
- JDK 11+
- Maven 3.6+

### 2. 运行项目
```bash
# 进入项目目录
cd mybookCrearer

# 编译运行
mvn spring-boot:run
```

### 3. 访问系统
打开浏览器访问: http://localhost:8080

### 4. 配置AI
1. 点击右上角「AI配置」按钮
2. 填写API地址、API密钥、模型名称
3. 支持OpenAI及兼容接口（如Azure、Claude等）

## AI配置说明

### 默认参数
| 参数 | 默认值                                        | 说明 |
|------|--------------------------------------------|------|
| API地址 | https://api.openai.com/v1/chat/completions | OpenAI接口地址 |
| 模型 | gpt-3.5-turbo                              | 使用的模型 |
| 温度 | 0.7                                        | 创造性程度(0-2) |
| Top P | 0.9                                        | 采样概率 |
| 最大Token | 4096                                       | 单次生成最大长度 |
| 超时 | 1200秒                                      | 请求超时时间 |

### 支持的变量（提示词模板）
- `{{userPrompt}}` - 用户输入的提示词
- `{{characters}}` - 角色信息
- `{{prevSummary}}` - 历史章节概括
- `{{title}}` - 当前章节标题
- `{{content}}` - 当前章节内容

## 项目结构
```
src/main/java/com/novel/
├── NovelCreatorApplication.java  # 启动类
├── controller/
│   └── NovelController.java      # 统一REST控制器
├── service/
│   ├── ChapterService.java       # 章节服务
│   ├── CharacterService.java     # 角色服务
│   ├── AIService.java            # AI调用服务
│   ├── PromptTemplateService.java # 模板服务
│   ├── AIConfigService.java      # 配置服务
│   └── SummaryCollectionService.java # 汇总服务
├── repository/                   # 数据访问层
├── entity/                       # 实体类
└── dto/                          # 数据传输对象

src/main/resources/
├── application.yml               # 配置文件
└── static/
    └── index.html                # 前端页面
```

## API接口

### 章节管理
- `GET /api/chapters` - 获取所有章节
- `POST /api/chapters` - 创建章节
- `PUT /api/chapters/{id}` - 更新章节
- `DELETE /api/chapters/{id}` - 删除章节

### AI生成
- `POST /api/generate` - 生成正文内容
- `POST /api/chapters/{id}/summary` - 生成章节概括
- `POST /api/collections/generate` - 生成章节汇总

### 角色管理
- `GET /api/characters` - 获取所有角色
- `POST /api/characters` - 创建角色
- `PUT /api/characters/{id}` - 更新角色

### 配置管理
- `GET /api/configs` - 获取所有配置
- `POST /api/configs` - 创建配置
- `POST /api/ai/test` - 测试AI连接

## 数据库

使用H2嵌入式数据库，数据文件保存在项目根目录的`data/`文件夹下。

访问H2控制台: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:file:./data/noveldb`
- 用户名: `sa`
- 密码: (空)

## 注意事项

1. **不使用Lombok**: 项目按要求未使用Lombok，所有Getter/Setter手动编写
2. **数据持久化**: 所有数据自动保存到H2数据库文件
3. **AI接口**: 需要有效的OpenAI API密钥或兼容接口
4. **超长文本**: 生成100万字需要多次调用AI，建议分章节逐步生成

## 许可证

MIT License
