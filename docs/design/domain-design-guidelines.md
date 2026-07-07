# 域设计指南

> **本文定位**：业务语义、状态命名、跨域业务规则的设计层规范。**技术实现规则**（ErrorCode 落位、删除策略实现、BizModel/xbiz/task.xml 选型、迁移策略等）归 `docs/architecture/`，本文只保留概要 + 指针，避免职责过宽。具体：
> - ErrorCode 命名/编码规则 → 概要见 §七，技术落位归 architecture
> - 删除策略实现 → 概要见 §九，技术落位归 architecture
> - BizModel/xbiz 决策 → 概要见 §十八，权威规则见 `docs/architecture/service-layer-orchestration.md`

## 目的

定义 nop-app-erp 各业务域的设计原则、边界划分、跨域协作模式与数据一致性策略。持久化字段、字典和 ORM 模型以 `model/app-erp-<domain>.orm.xml` 为准；技术实现细节见 `docs/architecture/`。

---

## 一、设计原则

### 1.1 单一职责原则

每个域只负责自己核心的业务能力，不越界处理其他域的职责：

| 域 | 核心职责 | 不负责 |
|---|----------|--------|
| master-data | 主数据维护（物料/SKU/往来单位/仓库/科目） | 业务单据、余额计算 |
| inventory | 库存移动、流水、余额 | 物料主数据、存货凭证生成 |
| purchase | 采购订单、入库、发票、付款 | 库存写入、应付凭证生成 |
| sales | 销售订单、出库、发票、收款 | 库存扣减、应收凭证生成 |
| finance | 凭证生成、过账、成本核算、期末结账 | 业务单据、物料主数据 |
| assets | 资产卡片、折旧、资本化、处置 | 资产实物维护、折旧科目映射 |
| manufacturing | BOM、工单、工艺、报工 | 库存写入、成本结转凭证 |
| projects | 项目、任务、工时 | 成本归集凭证 |
| maintenance | 设备维护、计划、停机 | 资产折旧、备件库存出库 |
| quality | 质检、NCR、CAPA | 退货/返工执行 |

### 1.2 松耦合原则

- **跨域调用只通过接口**：使用 `I*Biz` 接口，不直接调用实现类
- **避免循环依赖**：按 DAG 顺序组织，与 `data-dependency-matrix.md §2.1` 对齐：
  - `master-data → inventory → purchase/sales → finance（L3 顶）`
  - `assets/projects/manufacturing/quality/maintenance（L2 扩展）`
- **事件驱动解耦**：非关键路径使用事件异步通知，避免同步阻塞

### 1.3 幂等性原则

所有对外暴露的接口必须保证幂等：
- 使用唯一业务单号作为幂等键
- 重复调用同一单号的相同操作应返回相同结果
- 已完成的操作再次调用视为成功（空操作）

### 1.4 事务边界原则

| 操作类型 | 事务策略 |
|----------|----------|
| 单一域操作 | 本地事务 |
| 跨域同步操作 | 调用方事务传播（REQUIRED） |
| 跨域异步操作 | 各自独立事务 + 最终一致性 |
| 财务过账 | 异步事件 + posted 标志兜底 |

---

## 二、域间边界与交互模式

### 2.1 边界划分准则

**共享主数据**：master-data 域提供，其他域只读引用
- 物料/SKU、往来单位、仓库/库位、科目表/科目、币种/汇率、计量单位

**业务单据**：归属各自业务域，不跨域共享
- 采购订单（purchase）、销售订单（sales）、工单（manufacturing）

**状态流转**：每个域维护自己的状态机，跨域状态通过事件同步

### 2.2 交互模式分类

| 模式 | 适用场景 | 优点 | 缺点 |
|------|----------|------|------|
| **同步调用** | 强一致性要求、阻塞式确认 | 实时性强、一致性高 | 耦合度高、性能瓶颈 |
| **事件驱动** | 弱一致性要求、非阻塞流程 | 解耦、可扩展、异步 | 最终一致性、需处理失败 |
| **数据订阅** | 报表、数据分析 | 批量高效 | 数据延迟 |

### 2.3 标准交互模式

#### 模式 A：同步调用（强一致性）

采购域调用库存域的 `I*Biz` 接口生成移动单，事务传播为 REQUIRED。

**适用**：采购入库、销售出库（必须同步确认库存变更）

#### 模式 B：事件驱动（最终一致性）

业务域审核通过后发布事件，财务域订阅并异步生成凭证（post-commit）。

**适用**：凭证生成、成本结转（不阻塞业务流程）

#### 模式 C：数据查询（只读）

任何域通过 `IErpMd*Biz` 接口查询主数据，只读无事务。

**适用**：查询主数据、验证数据有效性

---

## 三、跨域协作规则

### 3.1 主数据引用分级规则

> 主数据引用按场景分级，采用平台原生机制（详见 `data-dependency-matrix.md §5.5-5.6` 与 `../nop-entropy/docs-for-ai/02-core-guides/cross-module-entity-reference.md §7`）。读引用按场景分别用机制 B 或机制 D。

| 引用场景 | 采用机制 | 说明 |
|----------|----------|------|
| 读引用——列表显示名 | **机制 D**（纯外键 + 冗余显示名字段） | 零 join，主数据改名需同步刷新冗余字段 |
| 读引用——详情带出完整对象 | **机制 D**（`@BizLoader` + `requireBiz`） | 懒加载，列表场景慎用（N+1） |
| 读引用——高频多维筛选/报表/GraphQL 展开 | **机制 B**（`notGenCode="true"` + `<to-one>`） | EQL 可点导航、自动 LEFT JOIN；已在本工程 17 业务域落地（见数据矩阵 §5.6.2） |
| 写引用 | **必须经 `IErpMd*Biz` 接口**（`@BizMutation`） | 跨域写强制走接口封装业务规则 |
| 给主数据加业务字段 | **`app-erp-delta` 的 `ext:baseClass` Delta 扩展** | 禁止给 master-data 表生成新 `className` |

### 3.2 库存域协作规则

| 调用方 | 操作 | 接口 |
|--------|------|------|
| purchase | 采购入库 | `IErpInvStockMoveBiz.generateIncomingMove()` |
| sales | 销售出库 | `IErpInvStockMoveBiz.generateOutgoingMove()` |
| manufacturing | 领料/完工 | `IErpInvStockMoveBiz.generateProductionMove()` |
| maintenance | 备件出库 | `IErpInvStockMoveBiz.generateConsumptionMove()` |

### 3.3 财务域协作规则

所有业务域通过 `IErpFinAcctDocProvider` 接口注册凭证生成规则，财务域统一聚合：
- 采购域：处理 AP_INVOICE/PAYMENT
- 销售域：处理 AR_INVOICE/RECEIPT
- 库存域：处理存货估值
- 资产域：处理 DEPRECIATION/CAPITALIZATION

### 3.4 质检域协作规则

| 触发域 | 事件 | 质量域动作 |
|--------|------|------------|
| purchase | 采购入库审核 | 触发来料检验 |
| sales | 销售出库审核 | 触发出货检验 |
| manufacturing | 工单报工/完工 | 触发制程/完工检验 |

---

## 四、数据一致性策略

### 4.1 强一致性场景

**场景**：库存扣减与业务单据审核必须在同一事务

```
销售出库单审核
    ├─ 校验可用量（库存域）
    ├─ 生成出库移动单（库存域）
    ├─ 更新出库单状态为已审核（销售域）
    └─ 事务提交（任一失败则全部回滚）
```

### 4.2 最终一致性场景

**场景**：业务单据审核与凭证生成可异步

```
业务单据审核（成功）
    ├─ 更新单据状态
    ├─ 设置 posted=false
    ├─ 发布过账事件
    └─ 返回成功

异步过账（事件驱动）
    ├─ 消费过账事件
    ├─ 生成凭证
    ├─ 设置 posted=true
    └─ 失败则重试（兜底扫描）
```

### 4.3 兜底机制

**posted 标志 + 定时扫描**：已审核但未过账的单据由定时任务重新触发过账。

### 4.4 冲销与回滚

| 单据类型 | 冲销方式 |
|----------|----------|
| 业务单据 | 生成红字冲销单（数量取负） |
| 凭证 | 生成红字凭证（金额取负） |
| 库存移动 | 生成反向移动单（方向相反） |

---

## 五、并发控制

### 5.1 乐观锁

所有业务单据使用乐观锁版本号，适用于并发编辑和状态变更场景。

### 5.2 悲观锁

适用于库存扣减、付款核销等需防止并发冲突的场景。

### 5.3 分布式锁（可选）

适用于跨服务、跨节点的资源互斥场景。

---

## 六、权限与安全

### 6.1 角色职责分离

| 角色 | 职责 | 禁止 |
|------|------|------|
| 采购员 | 创建/提交采购单据 | 审核自己创建的单据 |
| 销售员 | 创建/提交销售单据 | 审核自己创建的单据 |
| 审核人 | 审核业务单据 | 创建业务单据（可配置） |
| 财务员 | 过账、结账 | 业务单据审核（可配置） |
| 管理员 | 系统配置、反结账 | 日常业务操作（可限制） |

### 6.2 数据权限

- **行级权限**：根据组织/部门/项目限制数据可见性
- **字段级权限**：敏感字段（如成本、利润）只对授权角色可见

### 6.3 危险操作审计

所有危险操作必须记录审计日志：

| 操作 | 审计内容 |
|------|----------|
| 反审核 | 操作人、时间、原状态、冲销凭证号 |
| 反结账 | 操作人、时间、期间、审批人 |
| 红字冲销 | 操作人、时间、原单据号、冲销原因 |

---

## 七、ErrorCode 错误码约定

> **职责边界**：本节给出业务语义层的 ErrorCode 命名/编码规则（业务域命名空间约定）。技术落位（`ErrorCode` 接口实现、异常类生成、i18n 资源文件、错误响应格式）归 `docs/architecture/`（技术落位文档待建，暂以本节为参照）。

所有业务异常必须扩展 `NopException` + `ErrorCode`，描述使用中文（平台 i18n 处理翻译）。

### 7.1 命名空间

ErrorCode 遵循 Nop 平台惯例 `nop.err.<module>.<name>`，应用层以 `erp.err` 替换 `nop.err`。格式：`erp.err.<domain-short>.<name>`（`domain-short` 见 `docs/architecture/domain-module-split-analysis.md` §2.0 `appName` 列）。

| 域 | 命名空间 | 示例 |
|---|---|---|
| purchase | `erp.err.pur.*` | `erp.err.pur.order-not-found` |
| sales | `erp.err.sal.*` | `erp.err.sal.insufficient-credit` |
| inventory | `erp.err.inv.*` | `erp.err.inv.negative-stock-blocked` |
| finance | `erp.err.fin.*` | `erp.err.fin.period-not-open` |
| assets | `erp.err.ast.*` | `erp.err.ast.asset-not-depreciatable` |
| manufacturing | `erp.err.mfg.*` | `erp.err.mfg.bom-not-found` |
| projects | `erp.err.prj.*` | `erp.err.prj.task-already-completed` |
| maintenance | `erp.err.mnt.*` | `erp.err.mnt.equipment-not-found` |
| quality | `erp.err.qa.*` | `erp.err.qa.ncr-already-resolved` |
| master-data | `erp.err.md.*` | `erp.err.md.sku-duplicate` |
| crm | `erp.err.crm.*` | `erp.err.crm.lead-not-found` |
| cs | `erp.err.cs.*` | `erp.err.cs.ticket.not-found` |
| hr | `erp.err.hr.*` | `erp.err.hr.social-insurance-base-not-found` |
| logistics | `erp.err.log.*` | `erp.err.log.gateway-not-registered` |
| b2b | `erp.err.b2b.*` | `erp.err.b2b.edi-format-not-registered` |
| contract | `erp.err.ct.*` | `erp.err.ct.illegal-status-transition` |
| drp | `erp.err.drp.*` | `erp.err.drp.plan.illegal-transition` |
| aps | `erp.err.aps.*` | `erp.err.aps.no-available-slot` |

### 7.2 编码规则

- 使用可读字符串标识（如 `erp.err.pur.order-not-found`），遵循 Nop 平台 kebab-case 惯例
- 不推荐数字编码（与 Nop 平台惯例不符）
- ErrorCode 定义存放在各域 `-service` 模块的 `service/` 包下（与 `ErpXxxConstants` 同级）
- 每个域的 ErrorCode 定义以 `interface` + `ErrorCode` 静态字段形式：`Erp<Domain>Errors`（如 `ErpPurErrors`）

### 7.3 使用规范

```java
// 正确：使用 ErrorCode + NopException
throw new NopException(ErpPurErrors.ERR_ORDER_NOT_FOUND).param("orderId", orderId);

// 错误：直接抛出 RuntimeException 或自定义异常
throw new RuntimeException("订单不存在"); // 禁止
```

---

## 八、审计与追溯

### 8.1 操作日志（复用平台）

所有业务操作的日志**复用 Nop Platform 内置审计日志（nop-sys）**，各域不自建审计日志模块。平台自动记录操作人、操作时间、操作类型、变更前/后数据。

### 8.2 数据血缘

通过回链表追溯数据来源：
- 凭证 → 业务单据 → 库存移动 → 物料/SKU
- 发票 → 入库单 → 采购订单 → 供应商

### 8.3 数据对账

定期对账任务：
- 库存余额 vs 流水汇总
- 应收余额 vs 发票汇总
- 应付余额 vs 发票汇总
- 总账 vs 明细凭证

### 8.4 危险操作审计

除平台内置审计外，以下危险操作需额外定义业务级审计事件（记录操作理由、审批人等业务语义）：

| 操作 | 额外审计内容 |
|------|------------|
| 反审核 | 操作人、时间、原状态、冲销凭证号、操作理由 |
| 反结账 | 操作人、时间、期间、审批人、操作理由 |
| 管理员强操作 | 操作人、时间、变更前后数据、操作理由 |

---

## 九、删除策略

> **职责边界**：本节给出业务语义层的删除策略（哪些单据可删、用什么档位）。技术实现（`delFlag`/`delVersion` 字段机制、平台 `@BizMutation` 逻辑删除、归档迁移）归 `docs/architecture/`（技术落位文档待建，暂以本节 + `data-archival-strategy.md` 为参照）。

### 9.1 三档删除策略

| 单据状态 | 删除策略 | 说明 |
|----------|----------|------|
| 草稿（DRAFT / UNSUBMITTED） | 允许物理删除 | 草稿单据无业务影响，可物理删除减少数据膨胀 |
| 已审核未过账（docStatus=APPROVED, posted=false） | 只允许作废（docStatus→CANCELLED） | 已有审计价值，不允许物理删除 |
| 已过账/已完成（posted=true） | 完全禁止删除 | 纠错只能走红冲/反向单，保证审计轨迹完整 |

### 9.2 实现规则

- 所有 BizModel 的 `deleteById` / `deleteByIdList` 方法需校验单据状态
- 草稿状态物理删除需记录操作日志（操作人、时间、删除数据摘要）
- 已审核单据的删除请求返回明确错误码（`erp.<domain>.delete-not-allowed`）

---

## 十、会计期间统一规则

### 10.1 跨域统一复用

所有域**统一使用 finance 域的会计期间**（`ErpFinAccountingPeriod`），各域不维护独立期间。

- `businessDate` 决定期间归属
- 期间结账后禁止修改该期间内的**所有业务单据**（跨域一致）
- finance 域暴露 `IErpFinAccountingPeriodBiz` 供其他域校验期间状态

### 10.2 期间状态机

```
NOT_OPENED → OPEN → CLOSING → CLOSED
```

| 状态 | 含义 |
|------|------|
| `NOT_OPENED` | 预建但未开启的期间（初始态） |
| `OPEN` | 当前可操作期间 |
| `CLOSING` | 正在结账中（禁止新单据） |
| `CLOSED` | 已结账（禁止任何修改） |

### 10.3 期间操作约束

- 同一时刻只能有一个 `OPEN` 期间（当前期间）
- 开启新期间前必须先结账当前期间
- 反结账只能从 `CLOSED` 回退到 `OPEN`，需管理员权限
- 跨域期间校验：任何业务操作前校验 `businessDate` 所属期间是否为 `OPEN`

---

## 十一、负库存容错机制

负库存是**临时容错机制**，允许先出后入的业务场景（如先发货后入库、跨期到货等）。

### 11.1 处理规则

- 出库按移动加权平均成本出库（即使余额为负）
- 补货入库后成本自然平滑
- 不触发独立的凭证生成（凭证在业务单据审核时生成，与库存余额正负无关）
- 不会自动调整负库存余额

### 11.2 配置策略

- 全局开关：`erp-inv.allow-negative-stock`（按仓库可配，默认 false）
- 负库存持续时间监控：超过配置阈值（如 7 天）自动告警
- 详见 `docs/design/inventory/README.md`

---

## 十二、外币重估规则

### 12.1 重估范围

仅对**货币性科目**进行期末重估——非货币性科目按历史汇率不动：

| 科目类型 | 是否重估 | 汇率基准 | 差异处理 |
|----------|----------|----------|----------|
| 货币性（现金/银行存款/应收/应付） | 是 | 期末最后一天汇率 | 差异进当期汇兑损益 |
| 非货币性（固定资产/存货/股本） | 否 | 历史汇率 | 不调整 |

### 12.2 重估流程

```
期末结账 → 重估货币性科目余额 → 按期末汇率重算本位币金额
    → 差异 = 重估后金额 - 重估前金额
    → 生成汇兑损益凭证（借/贷：汇兑损益科目）
```

> 符合《企业会计准则第19号——外币折算》要求。

---

## 十三、版本演进策略

### 13.1 向后兼容

- 新增字段非空时提供默认值
- 删除字段前标记为 deprecated（保留至少一个版本）
- API 变更使用版本号控制

### 13.2 数据迁移

- 使用 nop-db-migration 管理 schema 变更
- 迁移脚本按版本顺序执行
- 迁移前备份数据

---

## 十四、单据标准字段约定

所有业务单据头（采购/销售/库存/资产/项目/维护/质量/制造工单等）必须统一携带以下四组公共字段。这是业财一体、多组织、多币种、多账套并行核算的基线，也是跨域统计与兜底扫描的统一入口。

### 14.1 组织与时间维度

| 字段 | 含义 | 备注 |
| --- | --- | --- |
| `orgId` | 业务组织（引用 `ErpMdOrganization`） | 多公司/多部门维度；查询过滤与权限隔离的依据 |
| `businessDate` | 业务日期 | 区别于 `createdAt`（系统创建时间）；用于期间归集与报表口径 |
| `createdAt`/`updatedAt` | 审计时间 | 框架自动维护 |

### 14.1.1 单据编号作用域

所有单据编号（凭证号、订单号、移动单号等）**在 orgId 内唯一**：

- 同一公司内单据编号连续不重复
- 不同公司可重复使用相同编号
- 序列号按 orgId 分组维护
- 单据编号作为幂等键使用（§1.3 幂等性原则）

### 14.1.2 单据编号模板

单据编号支持可配置模板，格式：`{PREFIX}{DATE}{SEQ}`

| 占位符 | 含义 | 示例 |
|--------|------|------|
| `{PREFIX}` | 单据类型前缀 | PO（采购订单）、SI（销售发票） |
| `{DATE}` | 日期格式 | YYYYMM、YYYYMMDD |
| `{SEQ}` | 序列号（按 orgId 内唯一） | 4位数字 |

示例模板：
- 采购订单：`PO{YYYYMM}{SEQ4}` → PO202606-0001
- 销售发票：`SI{YYYYMMDD}{SEQ4}` → SI20260629-0001
- 凭证：`V{YYYYMM}{SEQ4}` → V202606-0001

> 模板在数据库配置（`ErpSysDocNumberRule`），按 orgId + billType 维护。断号/跳号在业务号中允许；凭证号过账后分配保证连续（Q83）。

### 14.2 业财过账标志

| 字段 | 含义 | 备注 |
| --- | --- | --- |
| `posted` | 业财过账标志（BOOLEAN） | 业务单据是否已生成会计凭证；`false` 表示待过账 |
| `postedAt` | 过账时间 | 凭证生成时刻 |
| `postedBy` | 过账人 | 触发过账的作业/用户 |

> 兜底扫描：定时任务扫描 `posted=false` 且满足过账条件（已审核 + 单据生效）的单据，触发 `IErpFinAcctDocProvider` 补过账，保证业财最终一致。详见 `docs/design/finance/posting.md`。

### 14.2.1 posted 物理锁定规则

`posted=true` 的单据**物理锁定为只读**，纠错只能走红冲/反向单：

- 不允许任何直接 UPDATE 单据行数据
- 不允许修改金额、数量、科目等关键字段
- 纠错路径：生成红字冲销单据（反向凭证/退货单）
- 管理员不可绕过此锁定（与 Q7 决议一致）

> 已过账数据影响财务报表，任何直接修改都会破坏审计轨迹。这是 ERP 数据完整性的核心要求。

### 14.3 多币种四件套（金额类单据头/行）

| 字段 | 含义 | 备注 |
| --- | --- | --- |
| `currencyId` | 交易币种（引用 `ErpMdCurrency`） | 源币种 |
| `exchangeRate` | 汇率 | 源币种 → 本位币 |
| `amountSource` | 源币种金额 | 原始业务金额 |
| `amountFunctional` | 本位币金额 | `amountSource × exchangeRate`，凭证记账依据 |

### 14.4 并发控制

| 字段 | 含义 | 备注 |
| --- | --- | --- |
| `version` | 乐观锁版本号 | 框架自动维护 |

### 14.5 多账套维度（财务与存货估值相关）

凭证头/行（`ErpFinVoucher`/`ErpFinVoucherLine`）与存货估值（`ErpInvCostLayer`/`ErpInvStockLedger`/`ErpFinGlBalance`）额外携带 `acctSchemaId`（引用 `ErpMdAcctSchema`），支撑多套账并行核算（财务账/管理账/税务账/合并账/预算账）。

### 14.6 设计决策与反模式

- **决策**：`orgId`/`posted`/`businessDate`/`currencyId` 等标准维度使用**物理列**而非扩展字段（EAV）。理由：这些维度是所有单据的通用查询过滤、索引、统计口径，物理列的查询性能与索引能力远优于 EAV；EAV 仅用于真正按需扩展的业务属性。
- **反模式**（禁止）：用扩展字段承载 `orgId`/`posted`；用 `ErpMdPartner` 的余额字段"魔法更新"代替 AR/AP open-item 明细账（已改为 `ErpFinArApItem` + `ErpFinReconciliation`）。

---

## 十五、借贷方向约定

中式复式记账的五大类别借贷方向约定：

| 会计要素 | 增加方 | 减少方 | 典型科目 |
|----------|--------|--------|----------|
| **资产** | 借方 | 贷方 | 现金、银行存款、应收账款、存货、固定资产 |
| **负债** | 贷方 | 借方 | 应付账款、预收账款、应交税费 |
| **所有者权益** | 贷方 | 借方 | 实收资本、盈余公积、未分配利润 |
| **收入** | 贷方 | 借方 | 主营业务收入、其他业务收入 |
| **费用/成本** | 借方 | 贷方 | 主营业务成本、管理费用、销售费用 |

**恒等式**：资产 = 负债 + 所有者权益 + (收入 - 费用)

> 凭证生成逻辑 `createFacts()` 实现时必须遵循以上约定。错误的借贷方向会导致凭证借贷不平衡，触发实时校验拦截。

---

## 十六、状态机命名与跨域映射规范

> 本节统一各域状态命名约定，避免跨域流程串联时出现语义混乱。

### 16.1 双轴状态分离

所有业务单据头使用**双轴状态**：

- **`docStatus`（业务生命周期）**：表达单据在业务流程中的位置，独立于审批。
- **`approveStatus`（审批状态）**：表达审批流结果，可与 docStatus 解耦组合。

加上业财一体标志 `posted`（boolean），构成**三轴状态**。三者独立、可组合（如 `docStatus=APPROVED, approveStatus=APPROVED, posted=true` 是稳定终态）。

### 16.2 各域 docStatus 取值约定

不同业务性质的单据使用不同的状态值集合，但**初始态都用 DRAFT**，**作废态都用 CANCELLED**：

| 域 | 单据类型 | docStatus 取值 | 说明 |
|---|---|---|---|
| purchase/sales | 订单/出入库/发票/收付款 | `DRAFT` / `CANCELLED`（docStatus）+ `UNSUBMITTED` / `SUBMITTED` / `APPROVED` / `REJECTED`（approveStatus） | 通用业务单据，docStatus 与 approveStatus 双轴独立（新建单据 docStatus=DRAFT, approveStatus=UNSUBMITTED） |
| inventory | 移动单/盘点单/拣货单 | `DRAFT` / `CONFIRMED` / `DONE` / `CANCELLED` | 作业类单据（无审批轴，作业确认即生效） |
| finance | 凭证 | `DRAFT` / `POSTED` / `CANCELLED` | 凭证特殊：无 SUBMITTED，DRAFT 直接过账到 POSTED |
| finance | 会计期间 | `NOT_OPENED` / `OPEN` / `CLOSING` / `CLOSED` | 时间窗口状态机（见 §十） |
| assets | 资产卡片 | `DRAFT` / `IN_SERVICE` / `IDLE` / `SCRAPPED` / `SOLD` | 资产生命周期 |
| manufacturing | 工单 | `DRAFT` / `SUBMITTED` / `NOT_STARTED` / `STOCK_PARTIAL` / `STOCK_RESERVED` / `IN_PROCESS` / `COMPLETED` / `STOPPED` / `CLOSED` / `CANCELLED` | 制造执行链（与 `manufacturing/state-machine.md` `erp-mfg/work-order-status` 10 态字典一致；质检门控经 config-gated 钩子，不加 INSPECTING 字典态，见 plan 2237-1） |
| projects | 项目/任务 | `DRAFT` / `OPEN` / `ON_HOLD` / `COMPLETED` / `CANCELLED` | 项目生命周期。`ON_HOLD` 为项目独有暂停态（可恢复），见 `projects/state-machine.md` |
| quality | 质检/NCR/CAPA | `DRAFT` / `IN_PROGRESS` / `COMPLETED` / `CANCELLED` | 质量流程 |
| maintenance | 工单/请求 | `DRAFT` / `SCHEDULED` / `IN_PROGRESS` / `COMPLETED` / `CANCELLED` | 维护执行 |
| crm | 线索 | `NEW` / `QUALIFIED` / `CONVERTED` / `LOST` / `CANCELLED` | 线索生命周期（与 `crm/state-machine.md` `erp-crm/lead-status` 一致；不含工单状态轴） |
| cs | 工单 | `NEW` / `ASSIGNED` / `IN_PROGRESS` / `RESOLVED` / `CLOSED` / `CANCELLED` | 客服工单流程（与 `customer-service/README.md` `erp-cs/ticket-status` 一致；SLA 从 NEW 计时到 RESOLVED） |
| hr | 请假/工时单 | `DRAFT` / `SUBMITTED` / `APPROVED` / `REJECTED` / `CANCELLED` | HR 审批类单据（与 `human-resource/README.md` 一致；招聘工单另用 `OPEN`/`SCREENING`/`INTERVIEW`/`OFFERED`/`HIRED`/`REJECTED`/`CLOSED`，员工在职状态 `employmentStatus` 用 `ACTIVE`/`PROBATION`/`RESIGNED`/`TERMINATED`/`RETIRED`，二者均非 docStatus） |
| logistics | 发运单 | `DRAFT` / `CONFIRMED` / `IN_TRANSIT` / `DELIVERED` / `CANCELLED` | 物流发运流程（与 `logistics/state-machine.md` 一致） |
| b2b | EDI 事务 | `TO_SEND` / `SENT` / `ACKNOWLEDGED` / `RECEIVED` / `ARCHIVED` / `TO_CANCEL` / `CANCELLED` | EDI 报文生命周期（与 `b2b/state-machine.md` 一致；出/入站共用 docStatus） |
| contract | 合同 | `DRAFT` / `NEGOTIATION` / `ACTIVE` / `SUSPENDED` / `EXPIRED` / `TERMINATED` | 合同全生命周期（与 `contract/state-machine.md` 一致；版本状态 `isCurrent`/`FINALIZED`/`SIGNED` 单独管理） |
| drp | DRP 计划 | `DRAFT` / `COMPUTED` / `APPROVED` | 分销需求计划（与 `drp/state-machine.md` 一致；明细行 `SUGGESTED`/`CONFIRMED` 是行级状态，非 docStatus） |
| aps | 工序订单 | `DRAFT` / `PLANNED` / `IN_PROGRESS` / `COMPLETED` / `CANCELLED` | 排产执行（与 `aps/state-machine.md` `erp-aps/operation-order-status` 一致） |

### 16.3 approveStatus 取值约定（仅带审批的单据）

| approveStatus | 业务含义 |
|---|---|---|
| `UNSUBMITTED` | 未提交审批（初始） |
| `SUBMITTED` | 已提交待审批（提交到工作流，等待审核人处理；审核人未处理时可撤回至 UNSUBMITTED） |
| `APPROVED` | 审批通过 |
| `REJECTED` | 审批拒绝（可修改后重新提交） |

> inventory 的作业单（移动单/盘点单/拣货单）通常不经过审批流，`approveStatus` 可省略或保持 `UNSUBMITTED`。

### 16.4 反审核目标态

反审核（已审核单据撤销审核）的目标态是 `REJECTED`（可重新提交），**不是 `UNSUBMITTED`**（初始态）。语义理由：

- 反审核的单据已发生过业务（如已生成下游单据、已过账），不应回退为"未提交"。
- `REJECTED` 保留"曾审核过"的历史语义，便于审计追溯。
- 从 `REJECTED` 可重新 `SUBMITTED` 进入审批，或显式作废到 `CANCELLED`。

### 16.5 跨域状态映射

业务域之间串联流程时，状态映射规则（本表是全部跨域映射的唯一权威源，各域 state-machine.md 的跨域触发以本表为准）：

| # | 上游域 → 下游域 | 触发条件 | 映射规则 | 设计文档 |
|---|----------------|---------|---------|---------|
| 1 | purchase.`Receive` APPROVED → inventory.`StockMove` | 入库单审核通过 | 上游 APPROVED 触发下游 DRAFT → CONFIRMED（同事务） | `purchase/state-machine.md` |
| 2 | sales.`Delivery` APPROVED → inventory.`StockMove` | 出库单审核通过 | 同上 | `sales/state-machine.md` |
| 3 | purchase.`Return` APPROVED → inventory.`StockMove` | 退货单审核通过 | 生成反向出库移动单（红字冲销原入库） | `purchase/returns.md` |
| 4 | sales.`Return` APPROVED → inventory.`StockMove` | 退货单审核通过 | 生成反向入库移动单（红字冲销原出库）+ 红字发票 + 退款 | `sales/README.md` |
| 5 | 业务单据 APPROVED → finance.`Voucher` | 单据审核通过 | 上游 APPROVED → posted=false → 异步生成凭证（凭证走自身 DRAFT → POSTED） | `finance/posting.md` |
| 6 | 业务单据 CANCELLED → finance.`Voucher` | 单据作废 | 按业财回链反查 → 生成红字凭证冲销 | `finance/posting.md` |
| 7 | manufacturing.`MaterialIssue` → inventory.`StockMove` | 领料单确认 | 生成出库移动单（扣减原材料库存） | `manufacturing/state-machine.md` |
| 8 | manufacturing.`FinishedInput` → inventory.`StockMove` | 完工入库确认 | 生成入库移动单（增加产成品库存） | `manufacturing/state-machine.md` |
| 9 | manufacturing.`CostClose` → finance.`Voucher` | 成本结转 | 生成成本结转凭证（MANUFACTURING_COST_CLOSE） | `manufacturing/bom-and-routing.md` |
| 10 | logistics.`Shipment` DELIVERED → finance.`Voucher` | 发运单确认送达 | 触发运费凭证（FREIGHT），按双路径：销售→FREIGHT 凭证 / 采购→Landed Cost | `logistics/state-machine.md` |
| 11 | crm.`Lead` CONVERTED → sales.`Quotation` | 商机转化 | 调用 IErpSalQuotationBiz 创建报价单（弱指针，核心零污染） | `crm/README.md` |
| 12 | quality.`Inspection` REJECTED → purchase.`Return` / sales.`Return` / manufacturing.`Rework` | 质检不合格 | 按业务类型触发退货/返工/报废 | `quality/README.md` |
| 13 | maintenance.`Downtime` → manufacturing.`Workcenter` | 设备停机 | 扣减工作中心可用时段（影响 CRP/APS 排产） | `maintenance/README.md` |
| 14 | contract.`InvoicePlan` → purchase/sales.`Invoice` | 开票计划到期 | 按合同线生成 AP/AR 发票草稿 | `contract/README.md` |
| 15 | drp.`Line` APPROVED → inventory.`TransferOrder` / purchase.`Order` | 补货单批准 | 仓间调拨→TransferOrder，采购→PurchaseOrder | `drp/README.md` |
| 16 | aps.`OperationOrder` PLANNED → manufacturing.`JobCard` | 排产完成 | 按 OperationOrder 排程创建执行层 JobCard | `aps/README.md` |
| 17 | b2b.`Asn` RECEIVED → purchase.`Receive` | ASN 入站 | ASN 通知采购域准备收货（ASN 不直接写库存） | `b2b/README.md` |
| 18 | hr.`Timesheet` APPROVED → projects.`CostCollection` | 工时审核通过 | 工时成本归集到项目 | `human-resource/README.md` |
| 19 | hr.`Salary` CONFIRMED → finance.`Voucher` | 薪酬确认 | 生成薪酬凭证（SALARY） | `human-resource/README.md` |

> **跨域状态映射不直接耦合字段值**，而是通过事件/接口触发下游单据的状态迁移。下游单据的状态机独立运转。
>
> 跨域通信遵循 `docs/architecture/cross-domain-constraints.md` 的消弧约束（`initiatorDomain` 字段、循环检测、单向传播）。

---

## 十七、可配置点清单

本节统一列出全部业务可配置项、默认值、可选值和作用域。

### 两套配置机制

| 机制 | 适用场景 | 实现方式 | 参考 |
|------|----------|----------|------|
| **实体表字段** | 实体特有的配置（如仓库的批次策略） | 在 ORM 实体表上增加配置字段 | Odoo（`stock.location.removal_strategy_id`） |
| **独立配置表** | 跨域/全局配置（如过账模式、信用额度级别） | 独立 `ErpSysConfig` 表，按 Name+OrgId 查找 | iDempiere（`AD_SysConfig`） |

### 配置优先级链

所有配置项遵循三级覆盖优先级（从高到低）：

| 优先级 | 配置维度 | 覆盖规则 | 示例 |
|--------|----------|----------|------|
| 1（最高） | 按实体（实体表字段） | 实体自身字段值 | 某仓库的批次策略=FIFO |
| 2 | 按组织（`ErpSysConfig.orgId`） | 该组织下的所有实体使用此配置 | 公司 A 的成本方法=FIFO |
| 3（最低） | 全局默认（`ErpSysConfig.orgId=NULL`） | 未配置时使用 | 全局批次策略=FIFO |

> 参考 iDempiere `AD_SysConfig` 的 `AD_Client_ID`/`AD_Org_ID` 级联回退：`SELECT Value FROM ErpSysConfig WHERE Name=? AND (orgId IS NULL OR orgId=?) ORDER BY orgId DESC NULLS LAST`。

### ErpSysConfig 表

> ORM 真相：`module-master-data/model/app-erp-master-data.orm.xml` → `ErpSysConfig` 实体（`erp_sys_config` 表）。

配置键查询逻辑：`SELECT configValue FROM erp_sys_config WHERE configKey=? AND (orgId IS NULL OR orgId=?) ORDER BY orgId DESC NULLS LAST`。

### 仓库批次策略字段

> ORM 真相：`module-master-data/model/app-erp-master-data.orm.xml` → `ErpMdWarehouse.batchSelectionStrategy` 字段，引用字典 `erp-md/batch-strategy`（FIFO/FEFO/MANUAL）。空值表示使用全局配置。

### 配置项清单

| 配置项 | 默认值 | 可选值 | 作用域 | 说明 |
|--------|--------|--------|--------|------|
| `erp-inv.allow-negative-stock` | false | true/false | 全局/按仓库 | 允许负库存 |
| `erp-inv.batch-selection-strategy` | FIFO | FIFO/FEFO/MANUAL | 全局/按物料 | 批次选择策略 |
| `erp-inv.in-transit-timeout-days` | 7 | 整数 | 全局 | 在途超时告警天数 |
| `erp-inv.negative-stock-alert-days` | 3 | 整数 | 全局 | 负库存告警阈值 |
| `erp-inv.scrap-operation-type` | SCRAP | 作业类型编码 | 全局/按物料分类 | 报废出库作业类型 |
| `erp-sal.credit-check-level` | SOFT_WARNING | SOFT_WARNING/SPECIAL_APPROVAL/HARD_BLOCK | 全局/按客户 | 信用额度检查级别 |
| `erp-fin.auto-post-on-close` | true | true/false | 全局 | 结账时自动过账 |
| `erp-fin.auto-depreciation` | true | true/false | 全局 | 结账时自动折旧 |
| `erp-fin.reconcile-precision` | 0.01 | 正数 | 全局 | 核销金额精度 |
| `erp-fin.allow-over-reconcile` | false | true/false | 全局 | 允许超额核销 |
| `erp-md.uom-conversion-strict` | true | true/false | 全局/按物料 | 单位换算严格模式 |
| `erp-md.price-tiers` | 全部启用 | 按物料类别配置 | 按物料类别 | 可用价格档位 |
| `erp-md.critical-attributes` | 见 §十四 | 可扩展 | 按主数据类型 | 关键属性列表 |
| `erp-qua.ncr-posting-mode` | AUTO_POST | AUTO_POST/MANUAL_POST | 全局/按NCR类型 | NCR过账模式 |
| `erp-mfg.bom-snapshot-strategy` | LOCK_AT_CREATION | LOCK_AT_CREATION/AUTO_UPGRADE | 全局/按物料 | BOM快照策略 |
| `erp-fin.closing-reminder-days` | 3 | 整数 | 全局 | 结账提醒提前天数 |

---

## 十八、BizModel 与 xbiz 决策规则

> **职责边界**：本节是业务域视角的概要。**权威规则**（task.xml 编排、I*Biz 分工、步骤实现方式选择、映射约定、Delta 定制）见 `docs/architecture/service-layer-orchestration.md`，本节不重复其细节。

> 依据 Nop Platform `Model → Delta → Java` 决策框架（`ai-defaults.md`）。

### 业务逻辑分层

| 层 | 技术载体 | 用途 | 修改代价 |
|---|---------|------|---------|
| 模型声明 | `orm.xml` 字段/字典/关系、`xmeta` 元数据 | 字段级约束、显示规则、自动计算 | 低（改模型→重新生成） |
| 声明式逻辑 | `xbiz` 文件（XPL 模板） | CRUD 钩子（beforeSave/afterQuery）、字段级 autoExpr、校验规则 | 低（改 xbiz→热更新） |
| 编排逻辑 | BizModel Java 类（`@BizMutation/@BizQuery`） | 跨域操作（库存写入/凭证生成）、复杂事务、外部系统调用 | 中（需编译） |
| 基础服务 | `I*Biz` 接口 + 实现类 | 跨模块调用、业务原子操作 | 中（接口+实现） |
| SPI 扩展 | `@Inject Map<Code, Provider>` | 插件化业务类型（过账 provider、承运商网关、EDI 格式） | 低（新增 bean） |

### 选型规则

| 场景 | 推荐方式 | 不推荐方式 |
|------|---------|-----------|
| 字段默认值/自动计算 | `xmeta` autoExpr / `xbiz` beforeSave | Java setter |
| 字段级校验（非空/范围/唯一） | `orm.xml` mandatory / unique / `xmeta` 校验 | Java validator |
| 实体 CRUD 标准操作 | CrudBizModel（codegen 生成） | 手写全部 BizModel |
| 单一域业务操作（审批/作废） | BizModel `@BizMutation` | xbiz（过于复杂） |
| 跨域写操作（审核→库存→凭证） | BizModel `@BizMutation` + `I*Biz` 调用 | 手写事务管理 |
| 报表数据聚合 | `EQL` + `@BizQuery` | 存储过程 |
| 参数校验/配置读取 | `nop-config` / `xbiz` | 硬编码 |
| 第三方系统对接（TMS/EDI） | SPI Provider（`@Inject Map`） | 硬编码 switch/case |

### 具体示例

```java
// ✅ 推荐：BizModel + @BizMutation 做跨域操作
@BizModel("ErpPurOrder")
public class ErpPurOrderBizModel extends CrudBizModel<ErpPurOrder> {
    @BizMutation
    public ErpPurOrder approve(@Name("id") String id) {
        ErpPurOrder order = dao().require(id);
        // 1. 校验
        // 2. 跨域写入库存
        invStockMoveBiz.generateIncomingMove(...);
        // 3. 更新状态
        order.setApproveStatus(APPROVED);
        // 4. 发布过账事件（自动）
        return order;
    }
}

// ✅ 推荐：xbiz 做字段级声明式逻辑
// ErpPurOrder.xbiz
// <beforeSave>
//   entity.setTotalAmount(line.amount.sum());
// </beforeSave>

// ⛔ 不推荐：在 Java 中手写简单的字段计算
```

## 十九、命名规范附录

### 19.1 通用命名规则

- 实体类前缀：`Erp<DomainShort>`（如 `ErpMdMaterial`、`ErpInvStockMove`、`ErpPurOrder`）
- 表前缀：`erp_<short>_*`（如 `erp_md_`、`erp_inv_`、`erp_pur_`）
- 字典命名空间：`erp-<short>/<dict-name>`（如 `erp-md/material-type`、`erp-fin/voucher-type`）
- 完整物理目录 ↔ 逻辑工程名 ↔ appName ↔ 表前缀映射见 `docs/architecture/domain-module-split-analysis.md §2.0`

### 19.2 命名异常登记

某些前缀因业务概念跨域性质，不归属任何单一业务域，登记如下：

| 前缀 | 来源概念 | 涉及实体 | 异常原因 |
|------|---------|---------|---------|
| `erp-ct` | Contract | `ErpCt*` / `erp_ct_*` | 源自英文 Contract，避免与 `cs`（Customer Service）和 `crm`（Customer Relationship）混淆；详见 §19.3 |
| `ErpSys*` / `erp_sys_*` | 跨域系统配置与通知派发 | `ErpSysConfig`、`ErpSysNotification`、`ErpSysNotificationRecipient`、`ErpSysNotificationTemplate`、`ErpSysNotificationLog`、`ErpSysNotificationRead`、`ErpSysDocNumberRule` | 这些实体的业务语义跨多个域（配置归 master-data 维护、通知派发归 notify 子系统、单据号规则归 master-data），统一用 `Sys` 前缀表达"系统级跨域"性质，不强制归属某一域。`module-notify` 的通知实体命名为 `ErpSysNotification*` 而非 `ErpNotifyNotification*` 是有意为之——避免在已稳定的 `ErpSys*` 命名族中引入第二套前缀造成分裂 |

### 19.3 `erp-ct` 前缀来源（Contract）

`erp-ct` 是 Contract 域的简称。选择 `ct` 而非 `contract` 或 `ctr`：
- `contract` 全拼过长，不符合项目其他域的 3 字母简称惯例（`pur/sal/inv/fin/ast/mfg/qa/mnt/crm/cs/hr/aps/log/drp`）
- `ctr` 与 `crm`/`cs` 视觉上易混淆
- `ct` 是 Contract 的常见缩写（法律/商务语境），且与相邻前缀区分度高

此裁定登记于 `docs/plans/2026-07-07-2200-1-multi-dim-audit-supplement.md`（L-8）。

## 二十、总结

本指南定义了 nop-app-erp 的核心设计原则与约束，各域设计时必须遵守：

1. **单一职责**：每个域只负责核心业务能力
2. **松耦合**：通过接口和事件解耦
3. **幂等性**：保证重复调用安全
4. **事务边界**：区分强一致性与最终一致性
5. **权限安全**：职责分离、审计日志
6. **审计追溯**：回链表保证数据可追溯
7. **标准字段**：所有业务单据头统一携带 orgId/businessDate/posted/多币种四件套
各域设计文档应引用本指南，确保设计一致性。技术实现细节（锁策略、缓存、索引、平台组件集成）见 `docs/architecture/`。