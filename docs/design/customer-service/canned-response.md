# 客服工单域 — 预设应答模板（Canned Response）

## 目的

为客服人员提供预设应答模板，减少重复输入、统一服务口径、提高响应效率。支持按类别分组、变量占位符替换和宏自动匹配。

> **实现状态**（plan `2026-07-11-1234-2`）：§一~§三 已实现。CannedResponseRenderer 纯函数式工具 + ErpCsCannedResponseBizModel（renderTemplate/suggestForTicket/applyCannedResponse）+ 3 ErrorCode + 3 配置键。§四（管理功能 Excel 导入/导出 + §五 分类树校验）归 Deferred successor。

---

## 一、模型设计

### 1.1 ErpCsCannedResponse（预设应答）

| 字段 | 含义 | 备注 |
|------|------|------|
| id/code/orgId | 标准 | |
| title | 模板标题（如"密码重置指引"） | 客服选单时可见 |
| content | 模板正文（支持变量占位符） | large=true |
| categoryId | 应答分类（→ErpCsCannedCategory） | |
| variableDefs | 变量定义列表（JSON） | see §1.3 |
| macroTicketTypeId | 自动匹配的工单类型（可选） | 创建工单时自动推荐 |
| macroPriority | 自动匹配的优先级（可选） | LOW/NORMAL/HIGH/URGENT |
| sequence | 排序 | |
| isActive | 是否启用 | |
| usageCount | 使用次数 | |
| 标准审计字段 | | |

### 1.2 ErpCsCannedCategory（应答分类）

| 字段 | 含义 |
|------|------|
| id/code/name/orgId | 标准 |
| parentId | 父分类（自引用树形） |
| icon | 分类图标 |
| sequence | 排序 |

**预设分类**：

| 分类 | 子分类 | 典型场景 |
|------|--------|----------|
| billing | invoice-request | 发票开具/寄送指引 |
| billing | payment-issue | 付款失败/扣款争议 |
| billing | refund | 退款流程说明 |
| billing | price-discrepancy | 价格差异解释 |
| technical | login-issue | 登录/账户锁定 |
| technical | system-error | 系统报错处理方案 |
| technical | api-integration | API 集成指导 |
| technical | performance | 系统性能问题排查 |
| account | password-reset | 密码重置步骤 |
| account | profile-update | 信息修改指引 |
| account | permission | 权限申请流程 |
| account | subscription | 订阅变更/取消 |

### 1.3 variableDefs 变量定义

```json
{
  "variables": [
    {"key": "{customer_name}", "label": "客户名称", "required": true},
    {"key": "{ticket_id}",     "label": "工单编号", "required": true},
    {"key": "{agent_name}",    "label": "客服姓名", "required": true},
    {"key": "{product_name}",  "label": "产品名称", "required": false},
    {"key": "{order_number}",  "label": "订单号",   "required": false},
    {"key": "{deadline}",      "label": "预计解决时间", "required": false}
  ]
}
```

系统内置变量在渲染时自动填充：

| 变量 | 来源 |
|------|------|
| `{customer_name}` | 工单.customerId → ErpMdPartner.name |
| `{ticket_id}` | 工单.code |
| `{agent_name}` | 当前用户.displayName |
| `{today}` | 当前日期 |
| `{now}` | 当前日期时间 |

### 1.4 模板示例

**密码重置指引**（category=account/password-reset）：

```
您好 {customer_name}，

关于您提交的密码重置请求（工单号：{ticket_id}），请按以下步骤操作：

1. 访问登录页面，点击"忘记密码"
2. 输入您注册的邮箱 {email}
3. 查收重置邮件，点击链接设置新密码
4. 使用新密码登录系统

如仍无法登录，请联系客服 {agent_name}。

ERP 客服团队
```

---

## 二、宏自动匹配

### 2.1 匹配规则

```
工单类型/优先级变更
        │
        ├─► 查询 ErpCsCannedResponse
        │       macroTicketTypeId = 工单.ticketTypeId（匹配）
        │       AND macroPriority = 工单.priority（匹配或空）
        │       AND isActive = true
        │
        ├─► 按排序升序取前 3 条
        │
        └─► 在工单详情页侧栏展示"推荐应答"
```

### 2.2 匹配优先级

| 优先级 | 匹配条件 | 示例 |
|--------|----------|------|
| 精确匹配 | ticketType + priority | "故障-紧急"→紧急故障流程 |
| 类型匹配 | ticketType（priority 为空） | "故障"→通用故障流程 |
| 全局兜底 | ticketType 和 priority 均为空 | 通用开头/结束语 |

---

## 三、使用流程

### 3.1 客服选择应答

```
客服回复工单
        │
        ├─► 点击"插入预设应答"
        │
        ├─► 展开分类树（billing / technical / account）
        │
        ├─► 选择模板 → 预览渲染后的内容
        │
        ├─► 点击"插入"→ content 填入回复编辑框
        │
        ├─► 客服可手动修改后发送
        │
        └─► usageCount +1
```

### 3.2 变量填充

```
模板渲染逻辑
        │
        ├─► 系统变量自动填充（customer_name, ticket_id, agent_name, today）
        │
        ├─► 自定义变量弹窗要求客服输入（如 {email}）
        │
        ├─► 缺失的必填变量 → 禁止发送，高亮未填项
        │
        └─► 渲染结果存入工单操作日志（actionType=NOTE, content=渲染后正文）
```

---

## 四、管理功能

### 4.1 模板管理

| 功能 | 说明 |
|------|------|
| 模板 CRUD | 按分类管理（isActive 可控制启用/停用） |
| 分类管理 | 树形分类维护（最大深度 3 级） |
| 变量管理 | 可新增/编辑自定义变量（系统变量不可删除） |
| 导入/导出 | Excel 批量导入模板（title, content, categoryId, variableDefs） |
| 使用统计 | usageCount 排序，识别高频/零使用模板 |

### 4.2 权限

| 角色 | 权限 |
|------|------|
| 客服代理 | 查看分类、使用模板、预览渲染结果 |
| 客服主管 | 模板 CRUD、分类维护、使用统计查看 |
| 客服管理员 | 变量管理、导入导出、全部管理 |

---

## 五、配置点

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `erp-cs.canned-response-enabled` | true | 是否启用预设应答 |
| `erp-cs.canned-response-macro-count` | 3 | 宏自动匹配展示条数 |
| `erp-cs.canned-response-category-max-depth` | 3 | 分类最大深度 |

---

## 六、证据与参考

| 证据 | 强度 | 说明 |
|------|------|------|
| Odoo helpdesk canned responses | 🟢 | `helpdesk.ticket.canned_response` 模型 + category 分组 |
| Zendesk macros | 🟢 | 条件触发 + 变量占位符 + 多语言 |
| ServiceNow knowledge templates | 🟡 | 模板分类 + 变量映射 |

## 七、跨域协作

| 对端 | 协作方式 |
|------|---------|
| 工单（ErpCsTicket） | 插入应答后写入工单操作日志 |
| 知识库（ErpCsKnowledgeBase） | 应答与知识库条目独立，互不覆盖 |
