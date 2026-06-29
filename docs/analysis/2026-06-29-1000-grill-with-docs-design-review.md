# 设计文档审查讨论：grill-with-docs

## 审查方法

使用 `grill-with-docs` 技能（`C:\can\ai\skills\skills\engineering\grill-with-docs\SKILL.md`）对设计文档进行审查。

该方法的核心是 relentless interview — 沿着设计树向下走，逐个解决决策之间的依赖关系。在本审查中，我将这个方法应用于**文档审查**而非实时面试，系统地检查设计文档的内部一致性、完整性和语义清晰度。

## 审查范围

阅读了以下源文件：

### 全局设计文档
- `docs/design/README.md` — 设计文档索引与结构原则
- `docs/design/app-overview.md` — 应用基线
- `docs/design/domain-design-guidelines.md` — 域设计原则与标准字段约定
- `docs/design/flow-overview.md` — 全局业务流程编排
- `docs/design/domain-glossary.md` — 核心术语统一词汇表
- `docs/design/erp-design-audit-checklist.md` — 设计核对维度
- `docs/design/feature-inventory.md` — 功能清单
- `docs/design/roles-and-permissions.md` — 角色与权限模型
- `docs/skills/README.md` — 审查方法参考

### 域设计文档（代表性深度阅读）
- `docs/design/purchase/README.md` + `state-machine.md` + `three-way-match.md`
- `docs/design/inventory/README.md` + `state-machine.md` + `cross-domain.md`
- `docs/design/finance/README.md` + `posting.md`
- `docs/design/manufacturing/README.md` + `state-machine.md`
- `docs/design/quality/README.md`

---

## Round 1: 状态机命名约定不一致

### R1-Q1: `flow-overview.md` §3 状态表混用了 docStatus 和 approveStatus

**发现**：

- `flow-overview.md` §3.1 "采购域状态映射" 用 `UNSUBMITTED / SUBMITTED / APPROVED / CANCELLED` 表达"单据状态"。
- 但 `domain-design-guidelines.md` §11.1 明确规定使用**双轴状态分离**：`docStatus`（业务生命周期）+ `approveStatus`（审批状态）。
- `UNSUBMITTED` 按 §11.3 是 `approveStatus` 的取值，不是 `docStatus`。
- `CANCELLED` 按 §11.2 是各域通用 `docStatus` 作废态。

**问题**：§3 的表格到底在表达什么？是 `docStatus`？是 `approveStatus`？还是混淆了两者？

**候选解释**：

| 候选 | 内容 | 理由 |
|------|------|------|
| A. 表格表达的是 `approveStatus`（审批轴） | 大多数取值（UNSUBMITTED/SUBMITTED/APPROVED/REJECTED）是 approveStatus 的标准值 | 但 CANCELLED 是 docStatus 的作废态，不在 approveStatus 中 |
| B. 表格混用——表达"用户可见的综合状态" | 对用户同时展示审批状态和业务状态的聚合结果 | 但会模糊三轴分离的清晰性 |
| C. 表格是纯文字简述，不严谨但可接受 | owner doc 不是正式规范，`domain-design-guidelines.md` §11 才是权威 | 但作为流程总览应准确反映设计意图 |

**建议**：采纳候选 A，但需要澄清：表格标题改为"审批状态映射（approveStatus）"，并在脚注说明"作废是 docStatus 的终态，独立于审批轴"。同时补充 `docStatus` 的单独映射表。

---

### R1-Q2: `domain-design-guidelines.md` §11.2 会计期间初始态为 `CLOSED`

**发现**：

```
| finance | 会计期间 | `CLOSED` / `OPEN` / `CLOSING` / `CLOSED_FINAL`
```

`CLOSED` 作为起始状态，语义矛盾——一个会计期间在创建时应该是"未开启"或"开启中"，而不是"已结账"。

**候选解释**：

| 候选 | 内容 | 理由 |
|------|------|------|
| A. 是 typo，初始态应为 `NOT_OPENED` 或 `OPEN` | 符合直觉——新创建的期间还未开启 | CLOSED 作为初始态会违反"已结账期间不允许新增/修改凭证"的约束 |
| B. `CLOSED` 代表"初始未开启态"——非终态的 CLOSED | 初始态 CLOSED 是一种特殊标记 | 但与 CLOSED_FINAL（终态结账）重名会引起混淆 |
| C. 排序不代表状态机迁移顺序 | 表中只是枚举取值集合，不表示起始态 | 但 "CLOSED→OPEN" 的迁移不符合直觉——谁会在结账后才开启？ |

**建议**：初始态改为 `OPEN`（默认开启的期间），新增 `NOT_OPENED`（预建但未开启的期间）。CLOSED 只作为终态使用。这样状态机变为 `NOT_OPENED → OPEN → CLOSING → CLOSED`。

---

### R1-Q3: purchase 的 `docStatus` 取值被 domain-design-guidelines 写成 `DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED`，但 flow-overview 用 `UNSUBMITTED`

**发现**：

- `domain-design-guidelines.md` §11.2 说 purchase/sales 的 docStatus 取值是 `DRAFT / SUBMITTED / APPROVED / REJECTED / CANCELLED`。
- 但 `purchase/state-machine.md` 实际使用的初始态是 `UNSUBMITTED`，其审核轴迁移图从 UNSUBMITTED 开始。
- `flow-overview.md` §3.1 也用了 `UNSUBMITTED`。
- 而在 §11.3 中，`UNSUBMITTED` 被定义为 approveStatus 的初始态。

**问题**：purchase 单据的"未提交"到底是 `docStatus=DRAFT`（业务轴）还是 `approveStatus=UNSUBMITTED`（审批轴），还是说 UNSUBMITTED 同时作为两者的共同初始态？

**候选解释**：

| 候选 | 内容 | 理由 |
|------|------|------|
| A. `UNSUBMITTED` 是 approveStatus 的初始值，各域文档应统一用 `approveStatus=UNSUBMITTED, docStatus=DRAFT` 描述新建单据 | 严格遵循三轴分离 | purchase/state-machine.md 的迁移图实际上是在讲 approveStatus 的迁移，这一点应明确 |
| B. 新建单据时同时设 `docStatus=DRAFT` 和 `approveStatus=UNSUBMITTED`，但对外仅展示"草稿" | 合并了双轴的值 | 用户看到的"草稿"其实是两轴组合结果 |

**建议**：采纳候选 A。purchase/state-machine.md §2 的迁移图标题应补充"（approveStatus 轴）"，新建单据初始态描述为 `docStatus=DRAFT, approveStatus=UNSUBMITTED`。domain-design-guidelines.md §11.2 的 purchase docStatus 取值改为 `DRAFT / — / — / — / CANCELLED`（只有 DRAFT 是初始，APPROVED/REJECTED 是 approveStatus 的事，docStatus 在审核过程中不变）。

---

## Round 2: 域设计文档内容完整性

### R2-Q1: posting.md 定义了详细过账机制，但未定义事件消息契约

**发现**：

`finance/posting.md` 说："业务单据审核通过 → 发布过账事件 → 财务域异步生成凭证"

但未定义 `PostingEvent` 的消息体结构：

- 事件包含哪些字段？
- 是同步还是异步（明确说 post-commit）？
- 事件可靠性机制（at-least-once / exactly-once）？
- 死信队列处理？

**建议**：在 `finance/posting.md` 或 `docs/architecture/` 中补充事件契约定义：

```java
class PostingEvent {
    String billType;        // PURCHASE_INPUT / SALES_OUTPUT / AP_INVOICE / etc.
    String billHeadCode;    // 业务单据号
    String tenantId;        // 租户
    String acctSchemaId;    // 会计科目表（多账套并行需要）
    // ... 其他必要字段
}
```

---

### R2-Q2: 跨域事件（过账事件、库存事件）的消息契约没有统一文档

**发现**：

- `purchase/state-machine.md` 说"审核通过 → 财务域生成应付凭证"（异步事件）。
- `inventory/cross-domain.md` 说"移动单 DONE 后发布事件"。
- 但没有任何文档定义这些事件的标准消息结构。

**建议**：在 `docs/architecture/integration-and-transaction-patterns.md` 中补充跨域事件契约，或在 `domain-design-guidelines.md` 中新增"跨域事件契约"小节。

---

### R2-Q3: 制造域工单状态机有 10 态，但 flow-overview §3 未映射

**发现**：

- `domain-design-guidelines.md` §11.2 定义工单 docStatus 有 10 个取值：`DRAFT / SUBMITTED / APPROVED / RELEASED / IN_PROGRESS / COMPLETED / INSPECTING / REJECTED / CANCELLED / CLOSED`。
- `flow-overview.md` §3 只映射了 purchase/sales/inventory/finance/assets，没有 manufacturing 的状态映射表。

**建议**：在 `flow-overview.md` §3 新增 manufacturing 的状态映射表。

---

### R2-Q4: master-data 域文档完整性

**发现**：

- master-data 域没有 state-machine.md，这是按设计说明的（启停二态）。
- 但 master-data 也没有单独的 `ui-patterns.md`，而 domain-design-guidelines 的表里说每个域都有 ui-patterns.md。

**对齐检查**：
- `docs/design/README.md` 的结构选择原则说："主数据是启停二态（非工作流状态机），不单独建状态机文档，规则内嵌 README。"——这是合理的。
- 但 README.md 的"目录内文档组织约定"提到"`ui-patterns.md`（页面设计要点）：每个域一份"。

**问题**：master-data 到底有没有 ui-patterns.md？我在读的目录中 `master-data/` 下有 `ui-patterns.md`，所以是有的。但这是在 `docs/design/README.md` 的"全局设计文档"和"业务域设计文档"两个表格之外的域。它需要被完整审计。

**建议**：阅读并确认 `master-data/ui-patterns.md` 的质量。

---

## Round 3: 业财一体化设计深度

### R3-Q1: posted 标志的"已审核"前置条件不一致

**发现**：

- `flow-overview.md` §4.1 说"业务单据审核通过 → 设置 posted=false → 发布过账事件"。
- 但同一文档 §5.1 中兜底扫描的条件是"posted=false 且已审核超过 5 分钟"。
- 审核通过的同时设置 posted=false，兜底扫描再以"已审核 + posted=false"为条件——这意味着刚审核通过的单据可能被兜底扫描和事件触发同时处理。

**问题**：这是消息去重问题。如果事件处理和兜底扫描同时触发同一单据的过账，如何保证不重复过账？

**建议**：
- 过账过程应是幂等的：同一单据多次触发应检查是否已过账（posted=true），已过账则跳过。
- 在 posting.md 中明确去重机制（如：过账前先检查 posted 标志，已 true 则返回成功不重复生成凭证）。

---

### R3-Q2: posting.md 的 Provider 注册方式使用了 `@Inject List<IErpFinAcctDocProvider>`，但未考虑多账套并行的 `acctSchemaId` 路由

**发现**：

- `posting.md` 的 Provider 注册按 `businessType` 路由（一个 Provider 负责一种 businessType）。
- 但 `posting.md` 又说"同一业务单据在多套科目表下各生成一组凭证"。

**问题**：Provider 的 `createFacts(billData, acctSchema)` 需要循环调用多次，还是 Provider 内部一次性返回多套凭证的分录行？

**建议**：在 posting.md 中明确：
- Provider 的 `createFacts()` 语义：一次调用只处理一套科目表，还是处理全部？
- 建议：`createFacts(billData, acctSchemaId)` 一次调用处理一套科目表，财务域遍历当前激活的科目表列表多次调用。

---

## Round 4: 术语一致性

### R4-Q1: domain-glossary 定义了 ON_HOLD 作为跨域通用状态，但实际状态机中很少出现

**发现**：

- `domain-glossary.md` 第 96 行定义 `ON_HOLD` 为"暂停"，说明为"暂时停止执行（如项目/作业卡/资产闲置的语义近似态）"。
- 但 `domain-design-guidelines.md` §11 的各域状态取值中没有 `ON_HOLD`。
- 单独的 domain state-machine 中：purchase/sales 没有 ON_HOLD，inventory 没有 ON_HOLD，manufacturing 在 §11.2 中有 10 个状态但也无 ON_HOLD，manufacturing/material-reservation.md 中有 UNRESERVED/RESERVED/PICKED/RELEASED 也无 ON_HOLD。

**问题**：`ON_HOLD` 是否存在？如果存在，属于哪些域的哪些单据？

**候选解释**：
| 候选 | 内容 |
|------|------|
| A. 这是一个预留定义——未来可能需要暂停能力，但当前所有状态机都不需要 | 建议从 glossary 移除或标记为 "planned" |
| B. 在某些域中存在但未在 §11.2 列出 | 需补充到对应域 |

**建议**：采纳候选 A。若当前没有域使用 ON_HOLD，从 domain-glossary.md 移除该条目，避免产生"已定义但未使用"的混淆状态。未来需要时再添加。

---

### R4-Q2: "反审核目标态"在多个地方重复声明，增加了维护成本

**发现**：

`domain-design-guidelines.md` §11.4 声明了反审核目标态是 REJECTED（不是 UNSUBMITTED）。这一规则在以下地方重复出现：

- `domain-design-guidelines.md` §11.4
- `purchase/state-machine.md` 各处（至少 3 处）
- `flow-overview.md` §5.3
- `domain-glossary.md` REJECTED 条目

**问题**：大量重复增加了维护成本。§11.4 修改时需同步 4+ 个文件。

**建议**：精简为：
- `domain-design-guidelines.md` §11.4 — 唯一真相源，完整描述规则与理由。
- 其他文档仅引用 §11.4（如"见 domain-design-guidelines.md §11.4"），不再重复展开文本。
- 可考虑新增 `docs/references/reverse-audit-rule.md` 作为集中解释，但以本项目体量，引用 §11.4 足矣。

---

## Round 5: 跨域协作与边界

### R5-Q1: quality 域与 manufacturing/manufacturing 的协作仅用事件，但质检触发对 produce 流程是强约束

**发现**：

- `domain-design-guidelines.md` §3.4 说"工单报工/完工 → 触发制程/完工检验"。
- 但 quality 域的质检判定结果（ACCEPTED/CONDITIONAL/REJECTED）直接影响 manufacturing 的工单状态（COMPLETED → INSPECTING → [...] ）。

**问题**：这是强约束还是弱约束？工单完工后如果质检不合格，工单状态是否需要回退或保持 INSPECTING？

**建议**：在 `quality/inspection-integration.md`（或 quality/README.md）中对质检与制造的联动规则做显式约束描述，包括状态回退场景。

---

### R5-Q2: project 和 maintenance 的成本归集机制设计深度

**发现**：

- `projects/cost-collection.md` 存在。
- `projects/profitability.md` 存在。
- `maintenance/equipment-integration.md` 存在。

但这些文档在本次审查中未深度阅读。

**建议**：在未来审查中深度检查扩展域（assets/projects/maintenance）的设计文档与核心域的联动一致性。

---

## 已确认的强项

审查过程中也发现了一些设计良好的方面，这些不应在审计中被忽略：

| 方面 | 文件 | 评价 |
|------|------|------|
| **三轴状态分离** | `domain-design-guidelines.md` §11 | 清晰定义了 docStatus、approveStatus、posted 三轴独立演化，解决了单状态机的组合爆炸问题 |
| **可插拔过账引擎** | `finance/posting.md` | Provider 模式（IErpFinAcctDocProvider + Registry）使得新增业务类型零改动财务核心 |
| **FactsValidator 扩展点** | `finance/posting.md` | 参考 Metasfresh 的 IFactsValidator，提供了 GL Distribution 和合规校验的扩展能力 |
| **三层库存模型的不可变流水** | `inventory/README.md` | 流水不可变（冲销走反向）是保证审计追溯正确的核心设计 |
| **标准字段约定** | `domain-design-guidelines.md` §10 | orgId/businessDate/posted/多币种四件套统一，避免了各域各自为政的字段膨胀 |
| **文档结构因域而异** | `docs/design/README.md` | 不强制统一文档模板，按域复杂度决定文档深度——这是一个好的设计 |

---

## 决策汇总

| 决策 | 状态 | 位置 |
|------|------|------|
| flow-overview §3 表格应明确为 approveStatus 映射 + 补充 docStatus 映射 | 待定 | `docs/design/flow-overview.md` |
| 会计期间初始态改为 NOT_OPENED/OPEN | 待定 | `domain-design-guidelines.md` §11.2 |
| purchase state-machine 迁移图标题应明确为 approveStatus 轴 | 待定 | `purchase/state-machine.md` |
| 补充跨域事件消息契约 | 待定 | `finance/posting.md` 或 `docs/architecture/` |
| 制造域状态映射补充到 flow-overview §3 | 待定 | `flow-overview.md` |
| ON_HOLD 从 glossary 移除或标记为 planned | 待定 | `domain-glossary.md` |
| "反审核目标态"规则集中到 §11.4，其他文档只引用 | 待定 | 多文件 |
| posting.md 中明确多账套下的 Provider 调用语义 | 待定 | `finance/posting.md` |
| posting.md 中补充过账幂等/去重机制 | 待定 | `finance/posting.md` |

---

## 阻塞项

当前没有阻塞项。所有待定决策可在同一设计变更周期内处理。

---

## 后续步骤

1. 人工确认以上待定决策的采纳方向。
2. 确认后，对 `flow-overview.md`、`domain-design-guidelines.md`、`domain-glossary.md` 等文件执行编辑。
3. 编辑后触发 CI 验证（`mvn clean install -DskipTests` 确保构建不坏）。
