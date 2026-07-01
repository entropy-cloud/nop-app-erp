# 跨域集成约束

## 目的

定义 nop-app-erp 跨域集成的约束机制，包括消弧事件、跨域实体引用（平台原生机制）与跨域事务约束。

## 消弧事件

跨域通信使用事件驱动解耦，但需防止事件循环（消弧）：

### 消弧规则

| 规则 | 说明 |
|------|------|
| 事件发起方标记 | 事件携带 `initiatorDomain` 字段 |
| 循环检测 | 消费方检查 `initiatorDomain` 是否是自己，是则跳过 |
| 单向传播 | 事件只允许单向传播（A→B→C，不允许 A→B→A） |

### 事件注册

```
ErpSysEventConfig
    ├─ eventId
    ├─ eventTopic
    ├─ producerDomain（发起域）
    ├─ consumerDomains（消费域列表）
    └─消弧规则（dedup-window, max-retry）
```

## 跨域实体引用（平台原生机制）

> 完整机制（四种范式 + 平台实测证据）见 `../nop-entropy/docs-for-ai/02-core-guides/cross-module-entity-reference.md`，本工程落地清单见 `data-dependency-matrix.md §5.5-5.6`。

### 读引用

| 场景 | 机制 | 说明 |
|------|------|------|
| 列表显示关联对象名 | **机制 D**（纯外键 + 冗余显示名） | 零 join，主数据改名需同步刷新冗余字段 |
| 详情带出完整对象 | **机制 D**（`@BizLoader` + `requireBiz`） | 懒加载，列表场景慎用（N+1） |
| 高频多维筛选/报表/GraphQL 展开 | **机制 B**（`notGenCode="true"` + `<to-one>`） | EQL 可点导航、自动 LEFT JOIN，已在本工程 17 业务域落地（见 `data-dependency-matrix.md §5.6.2`） |

### 写引用

| 场景 | 机制 | 说明 |
|------|------|------|
| 跨域写入（如业财过账、库存写入） | **`I*Biz` 接口**（`@BizMutation`） | 跨域写必须经接口封装业务规则，禁止直接 ORM 跨域写 |

### 禁止

- **禁止**给外部（master-data 等）表生成新 `className`——加字段走 `app-erp-delta` 的 `ext:baseClass` Delta 扩展（见 `data-dependency-matrix.md §5.6.6`）。
- **禁止**业务域之间的反向或循环 ORM 引用——单向 DAG，违例见 `data-dependency-matrix.md §5.6.3`。

## 跨域事务约束

| 场景 | 约束 |
|------|------|
| 库存+业务单据 | **同事务（SYNC，强制）**——物理库存正确性硬约束（第①层） |
| 业务单据+凭证 | **默认 SYNC 同事务**；按 `(billType, acctSchemaId)` 可切 ASYNC post-commit（第②层可配），`posted` 兜底恒定生效（见 `posting.md §总体架构`） |
| 主数据引用 | 只读（无事务） |
