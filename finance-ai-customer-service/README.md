# 财务共享智能 AI 客服系统

基于 AI-Nova 框架的企业财务共享中心智能客服系统，采用 Spring Boot + Vue 3 前后端分离架构，通过 SupervisorAgent 多智能体调度实现报销、发票、薪资、供应商、流程引导等财务场景的智能问答与业务办理。

## 系统架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                        前端 (Vue 3 + Element Plus)                   │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐  │
│  │ 智能对话  │ │ 运营看板  │ │ 对话日志  │ │ 知识管理  │ │ 审计日志  │  │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘  │
└────────────────────────────┬────────────────────────────────────────┘
                             │ SSE / REST
┌────────────────────────────▼────────────────────────────────────────┐
│                     API 层 (Spring WebFlux)                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐               │
│  │ChatController│  │AdminController│  │AuditController│              │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘               │
└─────────┼─────────────────┼─────────────────┼───────────────────────┘
          │                 │                 │
┌─────────▼─────────────────▼─────────────────▼───────────────────────┐
│                        服务层 (Service)                               │
│  ┌────────────────┐ ┌──────────────┐ ┌──────────────┐               │
│  │ConversationSvc │ │OperationSvc  │ │KnowledgeSvc  │  ...          │
│  └───────┬────────┘ └──────────────┘ └──────────────┘               │
└──────────┼──────────────────────────────────────────────────────────┘
           │
┌──────────▼──────────────────────────────────────────────────────────┐
│                   AI 智能体层 (AI-Nova Agent)                        │
│                                                                      │
│              ┌─────────────────────────┐                             │
│              │   SupervisorAgent       │                             │
│              │   (LLM 意图调度器)       │                             │
│              └────┬───┬───┬───┬───┬───┘                             │
│                   │   │   │   │   │                                  │
│         ┌─────┐ ┌─┴─┐ ┌┴──┐ ┌┴──┐ ┌┴───┐                          │
│         │报销 │ │发票│ │薪资│ │供应│ │流程 │   ← 5 个 ReactAgent     │
│         │Agent│ │Agent│ │Agent│ │商Agent│ │引导Agent│                │
│         └──┬──┘ └─┬──┘ └─┬──┘ └─┬──┘ └──┬──┘                      │
│            │      │      │      │       │                           │
│         ┌──▼──┐┌──▼──┐┌──▼──┐┌──▼───┐┌──▼──────┐                  │
│         │Tools││Tools││Tools││Tools ││Tools    │                   │
│         └─────┘└─────┘└─────┘└──────┘└─────────┘                   │
└──────────────────────────────────────────────────────────────────────┘
           │
┌──────────▼──────────────────────────────────────────────────────────┐
│                    基础设施层 (Infrastructure)                        │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐               │
│  │  MySQL   │ │FSS 平台  │ │ ERP 系统 │ │ 税务/HR  │               │
│  │ (MyBatis)│ │  Client  │ │  Client  │ │  Client  │               │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘               │
└──────────────────────────────────────────────────────────────────────┘
```

## 对话处理流程

```
用户发送消息
      │
      ▼
┌─────────────┐    ┌──────────────────┐
│ ChatController│──▶│ConversationService│
│ (SSE Stream) │    └────────┬─────────┘
└─────────────┘             │
                            ▼
                  ┌──────────────────┐
                  │ 创建/恢复会话     │
                  │ 持久化用户消息    │
                  └────────┬─────────┘
                           │
                           ▼
                  ┌──────────────────┐
                  │ SupervisorAgent  │
                  │ (意图识别+调度)   │
                  └────────┬─────────┘
                           │
              ┌────┬───┬───┼───┬────┐
              ▼    ▼   ▼   ▼   ▼    │
           报销  发票 薪资 供应商 流程  │
           Agent Agent Agent Agent Agent│
              │    │   │   │   │    │
              └────┴───┴───┼───┴────┘
                           │
                           ▼
                  ┌──────────────────┐
                  │ 调用外部系统工具   │
                  │ (FSS/ERP/税务/HR)│
                  └────────┬─────────┘
                           │
                           ▼
                  ┌──────────────────┐
                  │ 数据脱敏处理      │
                  │ (身份证/银行卡/薪资)│
                  └────────┬─────────┘
                           │
                           ▼
                  ┌──────────────────┐
                  │ SSE 流式返回      │
                  │ (打字机效果)      │
                  └────────┬─────────┘
                           │
                           ▼
                  ┌──────────────────┐
                  │ 记录审计日志      │
                  │ 更新运营指标      │
                  └──────────────────┘
                           │
                  (连续3轮未解决?)
                     是 ──▶ 转接人工客服
```

## 项目结构

```
finance-ai-customer-service/
├── pom.xml                        # 父 POM（统一版本管理）
├── finance-ai-common/             # 公共模块
│   ├── dto/                       #   ChatRequest, ChatStreamResponse, DashboardMetrics ...
│   ├── enums/                     #   SessionStatus, MessageRole, ChatActionEnum, KnowledgeStatus
│   └── constants/                 #   ErrorCode, SensitivePatterns
├── finance-ai-domain/             # 领域层
│   ├── entity/                    #   Session, ChatMessage, KnowledgeEntry, AuditLog ...
│   └── repository/                #   各实体仓储接口
├── finance-ai-infrastructure/     # 基础设施层
│   ├── mapper/                    #   MyBatis Mapper (XML + 接口)
│   ├── client/                    #   FSS/ERP/Tax/HR 外部系统客户端 (10s超时 + 3次重试)
│   ├── memory/                    #   ChatMemoryConfig (关系型DB记忆存储, 最大50轮)
│   └── db/migration/              #   Flyway 迁移脚本 (V1~V4)
├── finance-ai-agent/              # AI 智能体层
│   ├── AgentConfig.java           #   5 个 ReactAgent + SkillAdvisor 配置
│   ├── SupervisorAgentConfig.java #   SupervisorAgent 顶层调度器
│   ├── ChatModelConfig.java       #   AI 模型连接配置
│   └── tool/                      #   ExpenseQuery/Submit, InvoiceVerify, SalaryQuery,
│                                  #   SupplierQuery, BillingSubmit, LoanQuery
├── finance-ai-service/            # 服务层
│   ├── conversation/              #   ConversationService, HumanHandoffService
│   ├── operation/                 #   OperationService (热点统计, 运营指标, 告警)
│   ├── knowledge/                 #   KnowledgeService, KnowledgeCategoryService
│   ├── audit/                     #   AuditLogService, AuditLogSearchService
│   ├── advisor/                   #   DataMaskingService, DataMaskingAdvisor, HumanHandoffAdvisor
│   ├── satisfaction/              #   SatisfactionFeedbackService
│   ├── autoreply/                 #   AutoReplyRuleService
│   ├── security/                  #   DataPermissionService (行级数据权限)
│   └── tool/                      #   KnowledgeQueryTool (知识库RAG查询)
├── finance-ai-api/                # API 层 (Spring WebFlux)
│   ├── controller/                #   ChatController, AdminController, AuditController, HealthController
│   ├── security/                  #   SecurityConfig, DevSecurityConfig, UserPrincipal
│   └── resources/
│       └── application.yml        #   应用配置 (支持环境变量覆盖)
└── finance-ai-ui/                 # 前端 (Vue 3 + Vite + Element Plus)
    ├── src/
    │   ├── views/
    │   │   ├── chat/              #   ChatView (智能对话)
    │   │   ├── admin/             #   Dashboard, LogQuery, HotTopics, Knowledge
    │   │   ├── audit/             #   AuditLogs
    │   │   ├── login/             #   LoginView
    │   │   └── error/             #   Forbidden, NotFound
    │   ├── components/            #   ChatBubble, MessageInput, SessionList,
    │   │                          #   SatisfactionDialog, TypingEffect
    │   ├── api/                   #   chat.js, admin.js, audit.js, auth.js
    │   ├── store/                 #   Pinia (user, chat)
    │   ├── router/                #   角色路由守卫
    │   └── utils/                 #   auth.js (JWT解析), sse.js (SSE流处理)
    ├── package.json
    └── vite.config.js
```

## 环境要求

| 组件    | 版本要求      |
|---------|--------------|
| JDK     | 1.8+         |
| Maven   | 3.6+         |
| MySQL   | 5.7+ / 8.0+  |
| Node.js | 18+          |
| npm     | 9+           |

## 数据库初始化

JDBC 连接串已配置 `createDatabaseIfNotExist=true`，首次启动自动创建 `finance_ai` 数据库。Flyway 自动执行 4 个迁移脚本：

| 脚本 | 说明 |
|------|------|
| V1__init_schema.sql | 创建 6 张核心表：t_session, t_chat_message, t_knowledge_entry, t_audit_log, t_operation_metrics, t_satisfaction_feedback |
| V2__session_title_pinned.sql | 会话增加标题和置顶字段 |
| V3__knowledge_category.sql | 知识库分类表 |
| V4__add_response_time.sql | 运营指标增加响应时间字段 |

只需确保 MySQL 已启动且用户有建库权限即可，无需手动建库建表。

## 后端配置

配置文件：`finance-ai-api/src/main/resources/application.yml`，支持环境变量覆盖：

| 配置项 | 环境变量 | 默认值 | 说明 |
|--------|----------|--------|------|
| 数据库用户名 | `DB_USERNAME` | root | MySQL 用户名 |
| 数据库密码 | `DB_PASSWORD` | root | MySQL 密码 |
| SSO Issuer URI | `SSO_ISSUER_URI` | https://sso.company.com/... | 企业 SSO 认证地址 |
| SSO JWK Set URI | `SSO_JWK_SET_URI` | https://sso.company.com/... | JWT 公钥地址 |
| AI 模型地址 | `AI_MODEL_BASE_URL` | https://ai-nova.company.com/api | AI-Nova 模型 API |
| AI 模型 Key | `AI_MODEL_API_KEY` | your-api-key | 模型 API Key |
| AI 模型名称 | `AI_MODEL_NAME` | GLM-4.7-Flash | 使用的模型 |
| 财务共享平台 | `FSS_PLATFORM_URL` | https://fss.company.com/api | FSS 平台地址 |
| ERP 系统 | `ERP_URL` | https://erp.company.com/api | ERP 系统地址 |
| 税务接口 | `TAX_URL` | https://tax.company.com/api | 税务验真接口 |
| HR 系统 | `HR_URL` | https://hr.company.com/api | HR 系统地址 |

## 启动后端

```bash
cd finance-ai-customer-service

# 编译打包（跳过测试可加 -DskipTests）
mvn clean package

# 方式一：Maven 插件启动
cd finance-ai-api
mvn spring-boot:run

# 方式二：直接运行 JAR
java -jar finance-ai-api/target/finance-ai-api-1.0.0-SNAPSHOT.jar
```

通过环境变量配置：

```bash
# Linux / macOS
export DB_USERNAME=your_user DB_PASSWORD=your_password AI_MODEL_API_KEY=your_key
java -jar finance-ai-api/target/finance-ai-api-1.0.0-SNAPSHOT.jar

# Windows
set DB_USERNAME=your_user
set DB_PASSWORD=your_password
set AI_MODEL_API_KEY=your_key
java -jar finance-ai-api/target/finance-ai-api-1.0.0-SNAPSHOT.jar
```

后端启动后监听 `http://localhost:8080`。

## 启动前端

```bash
cd finance-ai-ui

npm install       # 安装依赖
npm run dev       # 启动开发服务器 → http://localhost:3000
```

API 请求自动代理到后端 `localhost:8080`。

生产构建：

```bash
npm run build     # 产物在 dist/，部署到 Nginx 或其他静态服务器
```

## 智能体与工具

### SupervisorAgent 调度架构

```
                    用户消息
                       │
                       ▼
              ┌─────────────────┐
              │ SupervisorAgent │  ← LLM 意图识别
              │ (finance-supervisor) │
              └────────┬────────┘
                       │ 根据意图路由
        ┌──────┬───────┼───────┬──────┐
        ▼      ▼       ▼       ▼      ▼
   ┌────────┐┌────────┐┌────────┐┌────────┐┌────────┐
   │expense ││invoice ││salary  ││supplier││guide   │
   │-agent  ││-agent  ││-agent  ││-agent  ││-agent  │
   └───┬────┘└───┬────┘└───┬────┘└───┬────┘└───┬────┘
       │         │         │         │         │
       ▼         ▼         ▼         ▼         ▼
  ExpenseQuery InvoiceVerify SalaryQuery SupplierQuery FormValidation
  ExpenseSubmit             LoanQuery                  材料补齐引导
  BillingSubmit                                        单据退回分析
  LoanQuery
```

| 智能体 | 职责 | 绑定工具 |
|--------|------|----------|
| expense-agent | 报销单查询/提交、借款单/付款申请 | ExpenseQueryTool, ExpenseSubmitTool, BillingSubmitTool, LoanQueryTool |
| invoice-agent | 发票验真、发票咨询 | InvoiceVerifyTool |
| salary-agent | 工资条/个税/社保公积金查询 | SalaryQueryTool |
| supplier-agent | 供应商信息核对与搜索 | SupplierQueryTool |
| guide-agent | 单据退回分析、材料补齐、表单验证 | FormValidationService |

此外，`KnowledgeQueryTool` 作为知识库 RAG 查询工具，在对话中按需调用企业知识库获取财务制度、报销标准、审批流程等信息。

### 技能系统 (Skill)

通过 `ClasspathSkillRegistry` 从 `classpath:skills/` 加载技能定义，配合 `SkillAdvisor` 实现渐进式知识加载——模型按需调用 `read_skill()` 获取完整知识内容，减少初始上下文大小。

## 用户角色与页面

| 角色 | 说明 | 可访问页面 |
|------|------|-----------|
| EMPLOYEE | 企业内部员工 | 智能客服对话 |
| OPERATOR | 财务共享中心管理人员 | 智能客服对话 + 运营管理后台 (看板/日志/热点/知识库) |
| AUDITOR | 审计人员 | 智能客服对话 + 审计日志 |

### 员工对话

1. 登录后进入对话页面，点击「+ 新对话」开始会话
2. 输入问题，AI 以 SSE 流式打字机效果逐字回复
3. 支持场景：报销查询/提交、发票验真、薪资/个税/社保查询、借款/付款查询、供应商核对、开票申请、财务制度咨询
4. 连续 3 轮未解决 → 自动转接人工客服
5. 结束会话时弹出满意度评价（1-5 分 + 文字反馈）

### 运营管理后台 (OPERATOR)

- 运营看板：今日服务量、自助解决率、人工转接率、平均响应时间、热点问题柱状图、满意度趋势折线图
- 对话日志：按时间/员工ID/意图分类筛选，展开查看完整对话
- 热点问题：按频次排序的 TOP 20 高频咨询
- 知识库管理：分类树 + 知识条目列表，支持新增/编辑/审核（草稿→待审核→已生效）
- 自动回复规则：配置关键词匹配的自动回复

### 审计日志 (AUDITOR)

按时间范围、员工 ID、会话 ID 检索完整对话审计日志。

## API 端点

### 对话 `/api/v1/chat`（需认证）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /stream | SSE 流式对话 |
| GET | /sessions | 获取会话列表 |
| GET | /sessions/{id} | 获取会话详情 |
| POST | /sessions/{id}/close | 关闭会话 |
| POST | /sessions/{id}/feedback | 提交满意度评价 |
| POST | /sessions/{id}/rename | 会话重命名 |
| POST | /sessions/{id}/pin | 会话置顶 |
| GET | /sessions/{id}/waiting | 等待人工坐席 SSE 推送 |

### 运营管理 `/api/v1/admin`（需 OPERATOR 角色）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /logs | 对话日志查询 |
| GET | /hot-topics | 热点问题统计 |
| GET | /metrics | 运营指标看板 |
| POST | /knowledge | 新增知识条目 |
| PUT | /knowledge/{id} | 编辑知识条目 |
| PUT | /knowledge/{id}/review | 审核知识条目 |
| DELETE | /knowledge/{id} | 删除知识条目 |
| GET | /knowledge/categories | 获取知识分类树 |
| GET | /auto-reply-rules | 获取自动回复规则 |
| POST | /auto-reply-rules | 配置自动回复规则 |

### 审计 `/api/v1/audit`（需 AUDITOR 角色）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /logs | 审计日志检索 |

### 健康检查

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /health | 应用健康状态 |

## 安全机制

- JWT Token 认证（集成企业 SSO / OAuth2 Resource Server）
- 角色权限控制：EMPLOYEE / OPERATOR / AUDITOR 三级角色
- 敏感信息自动脱敏：身份证号、银行卡号、工资金额在响应中自动掩码
- 完整审计日志：所有对话交互、工具调用、人工转接均记录
- 连续 5 次认证失败锁定账户 30 分钟
- 行级数据权限隔离：员工只能查看自己有权访问的数据
- 外部系统调用 10 秒超时熔断 + 最多 3 次重试

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3, Vite, Element Plus, Pinia, Vue Router, ECharts, Axios |
| API 层 | Spring WebFlux, SSE (Server-Sent Events) |
| 服务层 | Spring Boot 2.7.18 |
| AI 框架 | AI-Nova (ReactAgent, SupervisorAgent, SkillAdvisor, ChatClient) |
| 数据访问 | MyBatis, Flyway |
| 数据库 | MySQL 5.7+ |
| 安全 | Spring Security, OAuth2 Resource Server, JWT |
| 测试 | JUnit 5, jqwik (属性测试), Mockito, AssertJ |

## 运行测试

```bash
cd finance-ai-customer-service
mvn test
```
