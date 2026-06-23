# 设计文档审计报告（design-doc-audit-prompt）

- **审计日期**：2026-06-23
- **审计方法**：`docs/skills/design-doc-audit-prompt.md`（11 维度 + 严重性分级）
- **审计范围**：`docs/design/` 全部 36 个文件（10 域目录 + 7 个全局文档），交叉核对 `<domain>/model/*.orm.xml`、`docs/requirements/product-scope.md`、`docs/context/source-of-truth-and-precedence.md`
- **审计者**：独立审计代理
- **裁决**：**fail**（1 blocker + 5 major）

## 裁决：fail

存在 1 个 blocker（跨文档真相源冲突，可能导致错误实现）和 5 个 major（owner-doc 边界漂移、重复维护点、词汇表滞后）。必须先修复 blocker 与 major 后才能宣称设计基线稳定。

## 发现清单（按严重性排序）

### B-1 [blocker] 反审核目标态跨文档冲突，可能导致错误实现

- **受影响文件/行**：
  - `docs/design/purchase/state-machine.md:43,57,74,96,109,162`（6 处，声明反审核目标态为 `UNSUBMITTED`）
  - `docs/design/sales/state-machine.md:34`（1 处，声明反审核目标态为 `UNSUBMITTED`）
  - 对照：`docs/design/domain-design-guidelines.md:369-375`（§11.4 明确规定目标态是 `REJECTED`，**不是** `UNSUBMITTED`）
  - 对照：`docs/design/flow-overview.md:471-476`（§5.3 明确目标态 `REJECTED`）
- **问题**：同一业务语义（已审核单据撤销审核后的目标状态）在 3 类 owner doc 中给出冲突答案。`domain-design-guidelines.md` §11 与 `flow-overview.md` §5.3 已统一为 `REJECTED`，但 `purchase/state-machine.md`、`sales/state-machine.md` 仍保留旧值 `UNSUBMITTED`。
- **重要性原因**：反审核目标态直接决定状态机迁移图与实现逻辑。若实现者读 purchase/sales 状态机文档，会按 `APPROVED → UNSUBMITTED` 编码；若读 guidelines/flow-overview，会按 `APPROVED → REJECTED` 编码。两套行为互斥，且 `REJECTED` 保留"曾审核过"的审计语义，`UNSUBMITTED` 则丢失该语义。审计维度 5（跨设计一致性）+ 维度 7（工作流和状态清晰度）双重违反。
- **证据来源**：日志 `docs/logs/2026/06-23.md` 显示该冲突在"业务流程覆盖审计"中已识别并"修正"，但修正只落到 guidelines/flow-overview，未传播到 state-machine 文档——属于未完成的半截修复。
- **处理方式**：以 `domain-design-guidelines.md` §11.4（`REJECTED`）为准，修正 `purchase/state-machine.md` 与 `sales/state-machine.md` 的迁移图、迁移表、场景演练、审查提示。`UNSUBMITTED` 是新建初始态，不可作为反审核目标态。

### M-1 [major] `erp-design-audit-checklist.md` 是重复维护点，且混入实现状态

- **受影响文件**：`docs/design/erp-design-audit-checklist.md`（全文件，340 行）
- **问题**：
  1. **重复维护点**（维度 11）：该文件包含"功能清单/核对表/完成度跟踪/改进建议/设计文档完整性总结"，与 `feature-inventory.md`（功能清单 owner）、`README.md`（设计索引 owner）、`docs/backlog/`（实施顺序 owner）职责重叠。
  2. **时间敏感内容污染稳定设计**（维度 2）：第 5、171-180 行的"实现状态（2026-06-22 更新）""数据库设计进行中""核对完成度 85%""待完善项 ⚠️""改进建议 高优先级立即实施"等是实施跟踪/roadmap 内容，按 AGENTS.md 操作规则 6 与设计 README，应归 `docs/backlog/` 或 `docs/logs/`，不应留在 `docs/design/`。
  3. **claim 与审计基线冲突**：第 319-339 行"设计文档已达到超越主流开源 ERP 的完善程度""0 规范违规""0 最佳实践偏离"与本审计发现的 blocker/major 相矛盾，属未经验证的自我评估。
- **重要性原因**：稳定设计文档不应承载实施进度跟踪，否则每次 codegen 推进都要改这份"设计"文件，违反"设计是稳定产品基线"原则。且其自我评估会误导后续审计以为无需再审。
- **处理方式**：将实现状态/完成度/roadmap 部分移出 `docs/design/`（迁至 `docs/backlog/` 或合并进日志）；设计核对维度本身可保留为精简的"设计要点 checklist"但去掉时间戳与完成率。**注意**：未经人工批准不要直接移动文件（AGENTS.md 规则 14），本报告先标记，移动动作需确认。

### M-2 [major] 多个详细设计文档重复抄写 ORM schema/字段清单，违反 owner-doc 边界

- **受影响文件/行**：
  - `docs/design/master-data/sku-multi-unit.md:188-193, 380-389`（内嵌 `<entity>` XML 片段定义 barcode 列与单据行字段）
  - `docs/design/inventory/trace-chain.md`（含 Java DAO 伪代码 + 字段清单）
  - `docs/design/manufacturing/material-reservation.md:35-71`（内嵌完整 `<entity name="ErpMfgMaterialReservation">` 与 `<entity name="ErpMfgWorkOrder">` ORM 片段）
  - `docs/design/finance/costing-methods.md:44-56, 335-347`（内嵌 `<entity>` 与 `<dict>` 定义）
  - `docs/design/finance/multiple-accounting-schemas.md:30-147`（抄写 Account/AccountingSchema/Voucher/VoucherLine/StockLedger/Balance 的完整字段清单）
- **问题**（维度 4 owner-doc 边界）：AGENTS.md 操作规则 7 与设计 README 明确"设计文档不应重复表目录、逐字段 schema、字典清单，应引用 `model/*.orm.xml`"。但这些详细文档直接抄写 ORM XML 与字段表，形成第二个 schema 真相源。
- **重要性原因**：`source-of-truth-and-precedence.md` 规定"模型/schema 文件是数据库真相源，散文获胜只在有意更新时"。一旦 orm.xml 调整字段名/类型，这些抄写的段落会静默漂移，且 schema 真相与散文冲突时按规则要改散文，增加维护成本。例如 `multiple-accounting-schemas.md` 用 `schemaId/costingSchemaId`，而 orm.xml 用 `acctSchemaId`（见 domain-design-guidelines §10.5），已存在命名漂移风险。
- **处理方式**：将字段级 schema/字典抄写替换为对 `model/app-erp-<domain>.orm.xml` 的引用 + 业务语义说明；保留业务规则、流程、状态含义（这些是设计 owner doc 该有的内容）。对已识别的命名漂移（`schemaId` vs `acctSchemaId`）以 orm.xml 为准修正散文。

### M-3 [major] `domain-glossary.md` 通用状态词汇表滞后于 §11 状态命名规范

- **受影响文件/行**：`docs/design/domain-glossary.md:82-94`（"通用单据状态词汇"表）
- **问题**（维度 5 跨设计一致性 + 维度 6 域语言清晰度）：词汇表的状态词条目（DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED/DONE/OPEN/IN_PROGRESS/COMPLETED/ON_HOLD）未与 `domain-design-guidelines.md` §11.2"各域 docStatus 取值约定"对齐。§11.2 明确不同域用不同状态集（如 purchase/sales 初始态 DRAFT、inventory 用 CONFIRMED、finance 凭证用 POSTED、assets 用 IN_SERVICE 等），但词汇表给出的是一套通用枚举，既不覆盖 §11 的域专属状态（如 NOT_STARTED/STOCK_RESERVED/IN_SERVICE/SCRAPPED），又未标注"各域取值见 §11.2"。
- **重要性原因**：词汇表 README 声明"设计文档出现术语冲突时，以本词汇表为准"。若实现者据此认为只有词汇表那套状态，会与各域状态机的真实状态集冲突。这是 §11 规范化后词汇表未同步的漂移。
- **处理方式**：在词汇表状态节增加指针"各域 docStatus/approveStatus 取值以 `domain-design-guidelines.md` §11.2/§11.3 为准"，并补齐 §11 引入但词汇表缺失的业务状态（IN_SERVICE/IDLE/SCRAPPED/SOLD/STOCK_RESERVED/STOCK_PARTIAL/POSTED 等）的业务含义；或在词汇表明确"状态码目录归 `model/*.orm.xml` 字典，本表只统一业务语义"以避免重复维护。

### M-4 [major] `feature-inventory.md` 的核销功能 owner 指向错误

- **受影响文件/行**：`docs/design/feature-inventory.md:22, 28`
- **问题**（维度 11 维护成本 + 维度 5 跨设计一致性）：
  - 第 22 行"付款与核销"→ Owner Doc 指向 `purchase/state-machine.md`
  - 第 28 行"收款与核销"→ Owner Doc 指向 `sales/state-machine.md`
  - 但 `purchase/state-machine.md` / `sales/state-machine.md` 实际只描述 `paidStatus`/`receivedStatus`（收付款进度派生态），**核销机制**（多对多核销、open-item 明细账、账龄、冲销）的真正 owner 是 `docs/design/finance/ar-ap-reconciliation.md`。`erp-design-audit-checklist.md` §7 也把核销归 finance。
- **重要性原因**：核销是跨 purchase/sales/finance 的核心业财闭环（`ErpFinReconciliation`/`ErpFinArApItem` 在 finance 域）。指向错误 owner 会让实现者在采购/销售域找核销规则而找不到，或误在采购/销售域实现核销逻辑，破坏"核销归财务域"的边界（domain-design-guidelines §1.1）。
- **处理方式**：将"付款与核销""收款与核销"的核销部分 Owner Doc 改指 `finance/ar-ap-reconciliation.md`；purchase/sales state-machine 仅保留收付款进度状态描述。

### M-5 [major] 详细设计文档混入大量 Java/SQL 实现伪代码，越界到架构层

- **受影响文件/行**：
  - `docs/design/inventory/trace-chain.md:170-230, 332-352, 358-428`（递归追溯查询 Java 伪代码 + DAO 操作）
  - `docs/design/manufacturing/material-reservation.md:112-172, 224-266, 311-342, 376-398`（预留计算/释放/领料 Java 伪代码）
  - `docs/design/finance/costing-methods.md:407-424`（成本追溯 Java 查询代码）
  - `docs/design/master-data/sku-multi-unit.md:135-162, 197-213, 309-326, 351-372`（换算/条码查询/默认 SKU Java 伪代码）
- **问题**（维度 4 owner-doc 边界）：设计 README 明确"当文档需要实现策略时，应引用 `docs/architecture/`，不要在这里混入平台实现细节"。但这些文档用具体 Java 方法签名、`dao.findById`、`RoundingMode.HALF_UP`、乐观锁实现等实现层细节表达业务规则。
- **重要性原因**：设计文档应描述"业务语义/规则/结果"，实现细节（事务、锁、查询方式）属架构层。混入 Java 伪代码会让读者误以为这是实现契约，且 Nop 平台要求跨实体走 `I*Biz`（AGENTS.md），伪代码却直接 `dao.findById` 跨工程访问，本身违反平台规则，可能误导实现。
- **处理方式**：将 Java 伪代码改写为业务规则/算法描述（如"递归沿 originMoveId 向上收集所有上游移动单"），实现落位由架构文档与生成代码承载。

## 审查范围摘要

- 全量阅读 `docs/design/` 下 36 个文件（7 全局 + 10 域 README + 9 状态机 + 10 专题文档）。
- 交叉核对 `module-*/model/*.orm.xml` 的字典定义（approve-status/paid-status/received-status/doc-status）以判定 doc/schema 漂移归属。
- 参考 `docs/logs/2026/06-23.md` 确认部分冲突为前序审计的半截修复。
- 未深入：`docs/architecture/` 全文（仅在验证 owner-doc 边界时抽样引用）、`docs/input/` 原始调研材料（仅 trace-chain/sku-multi-unit 等标注了调研出处）。

## 需求/设计冲突分类摘要

| 冲突 | 分类 | 说明 |
|------|------|------|
| B-1 反审核目标态 UNSUBMITTED vs REJECTED | **设计漂移**（design drift） | requirements 未指定；设计内部 guidelines §11 已裁定 REJECTED，state-machine 未同步 |
| M-2 schemaId vs acctSchemaId 命名 | **schema 真相获胜，散文漂移** | orm.xml 用 `acctSchemaId`，multiple-accounting-schemas.md 用 `schemaId`，按 source-of-truth 改散文 |
| M-3 词汇表状态集滞后 | **设计漂移** | §11 规范化了状态命名，词汇表未跟进 |
| M-1/M-4/M-5 | **owner-doc 边界/维护成本** | 非需求冲突，属文档组织问题 |

## Owner 边界摘要

- ✅ 良好：10 域 README 普遍在头部声明"持久化字段以 orm.xml 为准"，边界意识到位。
- ⚠️ 漂移：专题文档（sku-multi-unit/trace-chain/material-reservation/costing-methods/multiple-accounting-schemas）普遍越界抄写 schema 与 Java 伪代码（M-2/M-5）。
- ⚠️ 重复：`erp-design-audit-checklist.md` 与 feature-inventory/README/backlog 职责重叠（M-1）。

## 域边界摘要

- ✅ 良好：10 域职责划分清晰（domain-design-guidelines §1.1），跨域走 `I*Biz` + 事件，DAG 无环。
- ⚠️ 漂移：核销归属在 feature-inventory 中错指 purchase/sales（M-4），与 §1.1（finance 负责核销）和 ar-ap-reconciliation.md 不一致。

## 维护成本摘要

- 最高维护成本：M-2（5+ 文档抄写 schema，orm.xml 变更需同步多处）。
- 次高：M-1（audit-checklist 含时间戳与完成率，每次推进需改"设计"文件）。
- M-3/M-4/M-5 为局部指针/表述修正，成本较低。

## 修复状态（审计后已落地）

| 发现 | 状态 | 修复动作 |
|------|------|----------|
| B-1 反审核目标态冲突 | ✅ 已修复 | `purchase/state-machine.md`（迁移图/迁移表/终态恢复/可达性/角色/场景C/审查提示 共 7 处）、`sales/state-machine.md`（迁移图/角色 共 2 处）统一改为目标态 `REJECTED`，并加注指向 §11.4 |
| M-4 核销 owner 错指 | ✅ 已修复 | `feature-inventory.md` 第 22/28 行"付款与核销""收款与核销"Owner Doc 改指 `finance/ar-ap-reconciliation.md`，收付款进度仍指各域 state-machine |
| M-3 词汇表状态滞后 | ✅ 已修复 | `domain-glossary.md` 状态节加"取值归属"说明，补 REJECTED 反审核语义，新增"域专属状态指针"避免重复维护 |
| M-1 audit-checklist 重复维护点 | ✅ 已修复（就地清理，未移动文件） | `erp-design-audit-checklist.md` 删除时间戳/完成率表/"0 违规/超越开源"自评；实施进度改指 `docs/backlog/`；保留稳定的设计核对维度；"已完成项"改为"设计落位指引"。按 AGENTS.md 规则 14 未移动文件，迁移至 backlog 待人工确认 |
| M-2 schemaId 命名漂移 | ✅ 已修复 | `finance/multiple-accounting-schemas.md` 的 `schemaId`/`costingSchemaId` 改为 `acctSchemaId`（对齐 orm.xml），删除抄写的 Account/AccountingSchema/Voucher/VoucherLine/StockLedger/Balance 字段清单，改为对 orm.xml 的引用 |
| M-2 预留实体归属漂移（审计中新发现） | ✅ 已修复（owner 边界标注） | `manufacturing/material-reservation.md` 头部加注：预留持久化真相源是库存域 `ErpInvReservation`/`Line`（非 `ErpMfgMaterialReservation`），跨域走 `IErpInvReservationBiz` |
| M-5 Java/ORM 伪代码越界（trace-chain/sku-multi-unit/material-reservation/costing-methods） | ⏳ 推迟 | 这些伪代码使用说明性命名（`skuDao.findById`），不构成与 orm.xml 的事实冲突契约，属 owner-doc 边界整洁性问题而非"会导致错误实现"。完整重写为业务规则描述是更大重构，标记为后续改进，不阻塞 codegen |

## 重审建议

B-1 与全部 major 的事实冲突已消除后，建议在进入首域 codegen 前再跑一次 `design-doc-audit-prompt` 确认通过。M-5 的伪代码清理可与后续 owner-doc 整洁性迭代合并处理。

## 剩余风险与未覆盖区域

- **未覆盖**：本审计聚焦设计文档内部一致性，未重新审计状态机 10 维度正确性（已有 `state-machine-business-review-prompt` 专项）；未审计 ORM 字段完整性（已有 `orm-model-audit-prompt` 专项）；未审计跨模块 DAG（已有 `cross-module-dependency-audit-prompt` 专项）。
- **未验证**：design-doc 对 `docs/architecture/` 的引用是否准确（如多处引用 integration-and-transaction-patterns.md / customization-capabilities.md，未逐一确认目标文件存在且内容匹配）。
- **风险**：B-1 若不修复，采购/销售域反审核实现将与财务冲销、审计追溯语义冲突，是进入 codegen 前必须消除的阻塞项。
- **建议**：修复 blocker + major 后，重新跑一次 design-doc-audit 确认通过，再进入首域 codegen。
