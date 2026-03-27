# 财务共享智能 AI 客服系统 — 启动与使用说明

## 一、环境要求

| 组件　　| 版本要求　　|
| ---------| -------------|
| JDK　　 | 1.8+　　　　|
| Maven　 | 3.6+　　　　|
| MySQL　 | 5.7+ / 8.0+ |
| Node.js | 18+　　　　 |
| npm　　 | 9+　　　　　|

## 二、项目结构

```
finance-ai-customer-service/
├── pom.xml                      # 父 POM（统一版本管理）
├── finance-ai-common/           # 公共模块：DTO、枚举、常量
├── finance-ai-domain/           # 领域层：实体、仓储接口
├── finance-ai-infrastructure/   # 基础设施层：MyBatis Mapper、外部系统客户端
├── finance-ai-agent/            # AI 能力层：Agent、Tool、Skill
├── finance-ai-service/          # 服务层：业务编排、会话管理
├── finance-ai-api/              # API 层：WebFlux 控制器、安全配置、启动类
└── finance-ai-ui/               # 前端：Vue 3 + Element Plus
```

## 三、数据库初始化

JDBC 连接串已配置 `createDatabaseIfNotExist=true`，应用首次启动时会自动创建 `finance_ai` 数据库（如果不存在）。Flyway 随后自动执行 `V1__init_schema.sql` 建表脚本，创建 6 张核心表：`t_session`、`t_chat_message`、`t_knowledge_entry`、`t_audit_log`、`t_operation_metrics`、`t_satisfaction_feedback`。

只需确保 MySQL 服务已启动且配置的用户有建库权限即可，无需手动建库建表。

如需手动初始化，也可执行：

```sql
CREATE DATABASE finance_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```

```bash
mysql -u root -p finance_ai < finance-ai-infrastructure/src/main/resources/db/migration/V1__init_schema.sql
```

## 四、后端配置

配置文件位于 `finance-ai-api/src/main/resources/application.yml`，支持环境变量覆盖：

| 配置项 | 环境变量 | 默认值 | 说明 |
|--------|---------|--------|------|
| 数据库用户名 | `DB_USERNAME` | root | MySQL 用户名 |
| 数据库密码 | `DB_PASSWORD` | root | MySQL 密码 |
| SSO Issuer URI | `SSO_ISSUER_URI` | https://sso.company.com/... | 企业 SSO 认证地址 |
| SSO JWK Set URI | `SSO_JWK_SET_URI` | https://sso.company.com/... | JWT 公钥地址 |
| AI 模型地址 | `AI_MODEL_BASE_URL` | https://ai-nova.company.com/api | AI-Nova 模型 API |
| AI 模型 Key | `AI_MODEL_API_KEY` | your-api-key | 模型 API Key |
| AI 模型名称 | `AI_MODEL_NAME` | deepseek-chat | 使用的模型 |
| 财务共享平台 | `FSS_PLATFORM_URL` | https://fss.company.com/api | FSS 平台地址 |
| ERP 系统 | `ERP_URL` | https://erp.company.com/api | ERP 系统地址 |
| 税务接口 | `TAX_URL` | https://tax.company.com/api | 税务验真接口 |
| HR 系统 | `HR_URL` | https://hr.company.com/api | HR 系统地址 |

## 五、启动后端

```bash
# 1. 进入项目根目录
cd finance-ai-customer-service

# 2. 编译打包（跳过测试可加 -DskipTests）
mvn clean package

# 3. 启动应用（方式一：Maven 插件）
cd finance-ai-api
mvn spring-boot:run

# 3. 启动应用（方式二：直接运行 JAR）
java -jar finance-ai-api/target/finance-ai-api-1.0.0-SNAPSHOT.jar
```

通过环境变量配置数据库等参数：

```bash
# Linux / macOS
export DB_USERNAME=your_user
export DB_PASSWORD=your_password
export AI_MODEL_API_KEY=your_key
java -jar finance-ai-api/target/finance-ai-api-1.0.0-SNAPSHOT.jar

# Windows
set DB_USERNAME=your_user
set DB_PASSWORD=your_password
set AI_MODEL_API_KEY=your_key
java -jar finance-ai-api/target/finance-ai-api-1.0.0-SNAPSHOT.jar
```

后端启动后监听 `http://localhost:8080`。

## 六、启动前端

```bash
# 1. 进入前端目录
cd finance-ai-ui

# 2. 安装依赖
npm install

# 3. 启动开发服务器
npm run dev
```

前端启动后访问 `http://localhost:3000`，API 请求会自动代理到后端 `localhost:8080`。

生产环境构建：

```bash
npm run build
# 产物在 dist/ 目录，部署到 Nginx 或其他静态服务器
```

## 七、运行测试

```bash
# 运行所有后端测试（265 个测试，含单元测试和属性测试）
cd finance-ai-customer-service
mvn test
```

## 八、系统使用指南

### 8.1 用户角色

系统有三种角色，登录后根据角色展示不同功能：

| 角色 | 说明 | 可访问页面 |
|------|------|-----------|
| EMPLOYEE（员工） | 企业内部员工 | 智能客服对话 |
| OPERATOR（运营人员） | 财务共享中心管理人员 | 智能客服对话 + 运营管理后台 |
| AUDITOR（审计人员） | 审计人员 | 智能客服对话 + 审计日志 |

### 8.2 员工对话（所有角色可用）

1. 登录后默认进入对话页面
2. 点击「+ 新对话」开始新会话
3. 在输入框输入问题，按 Enter 或点击「发送」
4. AI 会以打字机效果逐字回复
5. 支持的业务场景：
   - 报销单查询/提交
   - 发票验真
   - 薪资/个税/社保查询
   - 借款单/付款申请查询
   - 供应商信息核对
   - 开票申请
   - 财务制度/报销标准/审批流程/税务政策咨询
6. 连续 3 轮未解决问题时自动转接人工客服
7. 点击「结束会话」关闭对话，弹出满意度评价（1-5 分 + 文字反馈）

### 8.3 运营管理后台（OPERATOR 角色）

通过左侧导航菜单访问：

- **运营看板**：查看今日服务量、自助解决率、人工转接率、平均响应时间，以及热点问题柱状图和满意度趋势折线图
- **对话日志**：按时间范围、员工 ID、意图分类筛选对话记录，点击行展开查看完整对话
- **热点问题**：查看按频次排序的 TOP 20 高频咨询问题
- **知识库管理**：左侧分类树 + 右侧知识条目列表，支持新增/编辑/审核操作，状态标签区分草稿/待审核/已生效

### 8.4 审计日志（AUDITOR 角色）

- 按时间范围、员工 ID、会话 ID 检索完整的对话审计日志

## 九、API 端点一览

### 对话端点（/api/v1/chat，需认证）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /stream | SSE 流式对话 |
| GET | /sessions | 获取会话列表 |
| GET | /sessions/{id} | 获取会话详情 |
| POST | /sessions/{id}/close | 关闭会话 |
| POST | /sessions/{id}/feedback | 提交满意度评价 |
| GET | /sessions/{id}/waiting | 等待人工坐席 SSE 推送 |

### 运营管理端点（/api/v1/admin，需 OPERATOR 角色）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /logs | 对话日志查询 |
| GET | /hot-topics | 热点问题统计 |
| GET | /metrics | 运营指标看板 |
| POST | /knowledge | 新增知识条目 |
| PUT | /knowledge/{id}/review | 审核知识条目 |
| GET | /auto-reply-rules | 获取自动回复规则 |
| POST | /auto-reply-rules | 配置自动回复规则 |

### 审计端点（/api/v1/audit，需 AUDITOR 角色）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /logs | 审计日志检索 |

## 十、安全机制

- 所有 API 通过 JWT Token 认证（集成企业 SSO）
- 敏感信息（身份证号、银行卡号、工资金额）在响应中自动脱敏
- 所有对话交互记录完整审计日志
- 连续 5 次认证失败锁定账户 30 分钟
- 数据权限隔离：员工只能查看自己有权访问的数据
- 外部系统调用 10 秒超时熔断 + 最多 3 次重试
