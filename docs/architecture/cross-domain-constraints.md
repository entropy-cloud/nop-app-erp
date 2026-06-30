# 跨域集成约束

## 目的

定义 nop-app-erp 跨域集成的约束机制，包括消弧事件和 @RefLink 引用。

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

## @RefLink 引用

跨域数据引用使用 `@RefLink` 注解，禁止 ORM 层跨工程 `refEntityName`：

```java
@RefLink(target = "ErpMdMaterial", joinBy = "materialId")
private ErpMdMaterial material;
```

### 引用规则

| 规则 | 说明 |
|------|------|
| 只读引用 | @RefLink 只用于查询，不用于写入 |
| 接口优先 | 写入操作通过 `I*Biz` 接口 |
| 无循环引用 | A 引用 B，B 不可引用 A |
| 缓存友好 | 引用数据通过主数据缓存访问 |

## 跨域事务约束

| 场景 | 约束 |
|------|------|
| 库存+业务单据 | 同事务（REQUIRED） |
| 业务单据+凭证 | 异步事件（最终一致） |
| 主数据引用 | 只读（无事务） |
