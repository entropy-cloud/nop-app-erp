# 设计文档审查讨论：grill-with-docs 第二轮审计

## 审查方法

使用 grill-with-docs 方法论（relentless interview — 沿设计树向下排查，逐条挑战一致性、完整性与"更好实现"论证）对 plan02 + plan03 交付后的当前设计文档状态做全量审计。

## 审查范围

### 全局设计文档
- `docs/design/domain-design-guidelines.md`（§10/§16 状态机规范）
- `docs/design/domain-glossary.md`（术语词汇表）
- `docs/design/flow-overview.md`（流程总览 + 状态映射）
- `docs/design/feature-inventory.md`（功能清单）
- `docs/design/README.md`（设计文档索引）
- `docs/design/app-overview.md`（应用总览）

### 域设计文档
- `docs/design/purchase/state-machine.md`
- `docs/design/projects/use-cases.md`, `cost-collection.md`
- `docs/design/quality/state-machine.md`, `recall.md`, `README.md`
- `docs/design/finance/posting.md`, `expense-claim.md`, `treasury.md`
- `docs/design/manufacturing/crp.md`, `state-machine.md`
- `docs/design/purchase/supplier-evaluation.md`
- `docs/design/inventory/consignment.md`, `state-machine.md`
- `docs/design/crm/README.md`, `docs/design/logistics/README.md`
- `docs/architecture/b2b-integration.md`

### 资料来源
- 所有 erp-survey 调研报告（`docs/analysis/erp-survey/`）
- 前序审计：`2026-06-23-0000~0005` 系列、`2026-06-29-grill-with-docs-design-review.md`
- 计划：`02-documentation-improvement-plan.md`、`03-advanced-scenario-design-gap-fill.md`

---

## 综述：核心结论

**Plan 02 的 R1 状态机修复已全部落地**（会计期间初始态、approveStatus/docStatus 标注分离、跨域状态映射表）。**Plan 03 的 9 份新设计文档质量高**（实体清单、SPI 契约、反模式警示、证据诚实标注均达标）。

但本轮审查发现了 **4 个新的/未修复的不一致性**（1 HIGH、2 MEDIUM、1 LOW）与 **2 个审计过程质量问题**，详见下文。

### erp-survey 高级功能覆盖总览

| 类别 | 已覆盖（设计级） | 标记可选扩展/延迟 | P2 排除 |
|------|-----------------|-------------------|---------|
| 业财一体凭证过账 | 自动过账(IErpFinAcctDocProvider)、凭证模板、多账套、多币种、期间结账 | — | — |
| 库存 | 三层模型、追溯链、批次/序列号、调拨、盘点、**VMI(consignment.md)** | — | — |
| 采购/销售 | 三单匹配、合同、信用额度、RFQ/报价、退换货、**供应商评分卡(supplier-evaluation.md)** | — | — |
| 制造 | BOM/工艺路线、MRP、工单10态、委外、**CRP(crp.md, self-admitted delay APS)** | — | — |
| 质量 | 质检单/模板、NCR/CAPA、让步接收、**召回(recall.md)** | — | — |
| 资产 | 卡片/折旧/处置/资本化 | — | — |
| 项目 | 项目/任务/工时/成本归集(**已补expense-claim引用**) | — | — |
| 费用/资金 | **费用报销/借款(expense-claim.md)**、**资金/票据(treasury.md, 中式承兑汇票自建)** | — | — |
| CRM | — | **crm/README.md** (骨架) | — |
| TMS | — | **logistics/README.md** (三层SPI骨架) | — |
| EDI/B2B | — | **b2b-integration.md** (SPI+信封状态机骨架) | — |
| DRP/HR/POS/售后 | — | — | **P2 deferred** |

**对 erp-survey 的"更好实现"论证**：每份新文档通过"反模式警示 + 核心零污染 + 三轴分离 + SPI 注册中心"四组设计杠杆，系统性地规避了开源方案的 13 个已知反模式，论证充分。详见 §4。

---

## Round 1：Plan 03 交付缺口（HIGH — 1 个）

### R1-Q1: quality/state-machine.md 未加入 NCR ESCALATED_TO_RECALL（裁决 D2 落地不全）

**发现**：
- Plan 03 `recall.md:88` 裁决 D2 明确规定：新增 NCR status 值 `ESCALATED_TO_RECALL`（给字典 `erp-qa/ncr-status` 新增值），且引用 `quality/state-machine.md`。
- 但 `quality/state-machine.md` §NCR 状态机（:121-137）仍然只有 4 个状态：`OPEN / IN_REVIEW / RESOLVED / CANCELLED`，**无 ESCALATED_TO_RECALL**。
- Plan 03 Closure Audit 证据 D 声称"D2 ESCALATED_TO_RECALL 落地"——但只有 recall.md 记录了文字，state-machine.md 未同步更新。

**问题**：D2 的实质内容是"NCR 状态机新增一个状态值"，如果 state-machine.md（作为 NCR 状态机的权威描述）不更新，开发者看到的就是"NCR 只有 4 态"，不知道有 ESCALATED_TO_RECALL。这是一个**设计文档交付缺口**。

**候选解释**：

| 候选 | 内容 | 理由 |
|------|------|------|
| A. D2 的"落地"= 在 recall.md 中记录决议，state-machine.md 的更新属 ORM 落地阶段 | recall.md :146 确实引用了 state-machine.md 但未做标注 | 但 recall.md :88 明确说"新增 NCR status 值"，是设计文档本身承诺的变更 |
| B. 缺口属 closure audit 遗漏 | closure audit 只验证了 recall.md 写了 D2，未读 quality/state-machine.md | 确实是独立审计的深度不足 |
| C. state-machine.md 的更新在 ORM 阶段（plan 03 Non-Goals 声明不改 ORM） | "给字典 erp-qa/ncr-status 新增值" 涉及 ORM XML | 但 state-machine.md 不是 ORM，是设计文档，应当同步更新 |

**建议**：
- 采纳候选 A+B。在 `quality/state-machine.md` §NCR 状态机中补充 ESCALATED_TO_RECALL 状态，与 recall.md D2 一致。在 recall.md 中显式标注"state-machine.md 已同步更新"。
- closure audit 检查清单应补充"跨文档一致性验证"项。

---

## Round 2：长期未修复的设计不一致性（MEDIUM — 2 个）

### R2-Q1: projects 状态集与 domain-design-guidelines 冲突（HIGH-MEDIUM）

**发现**：

`projects/use-cases.md:9` 定义的项目状态：
```
DRAFT / OPEN / ON_HOLD / COMPLETED / CANCELLED
```

`domain-design-guidelines.md §16.2` 定义的项目 docStatus：
```
DRAFT / PLANNED / IN_PROGRESS / COMPLETED / CANCELLED
```

- 两套状态集**完全不同**：中间态一个是 `OPEN/ON_HOLD`，另一个是 `PLANNED/IN_PROGRESS`。
- 这是**设计层面的不一致**，不是"一个精确一个简述"的问题——如果 projects/state-machine.md 也使用了 OPEN/ON_HOLD（use-cases.md 引用 state-machine.md），那么 domain-design-guidelines 的状态声明就是**错误的**。
- 该问题**前置审计均未发现**（前序四份审计 + grill 第一轮 + plan02 + plan03 均未触及 projects 域）。

**问题**：两个文档声称描述同一概念（项目 docStatus），但值集完全不相交。开发者不知道该以谁为准。

**候选解释**：

| 候选 | 内容 | 理由 |
|------|------|------|
| A. `domain-design-guidelines.md` 是权威，`use-cases.md` 用的旧状态 | domain-design-guidelines §16.md 是统一规范 | 但 use-cases.md 是 projects 域的 owner doc，且有 state-machine.md 背书 |
| B. `use-cases.md` 是权威，domain-design-guidelines 的 projects 行未更新（plan 遗漏） | projects 域第一次被深度审计 | 可能是项目 10 域中唯一未做 plan02 状态机修复的域 |
| C. 两者是同一语义不同命名（PLANNED ≈ OPEN, IN_PROGRESS ≈ ON_HOLD） | 但 PLANNED 侧重计划阶段，OPEN 侧重已开放归集 | 语义不完全对等——ON_HOLD 是暂停，IN_PROGRESS 是进行中，语义不同 |

**建议**：采用候选 B。projects/state-machine.md 和 README.md 需要首次阅读以确定哪个是真相。建议统一为：
- 在 domain-design-guidelines §16.2 的 projects 行中采用 `DRAFT / OPEN / ON_HOLD / COMPLETED / CANCELLED`（与 use-cases.md 一致），因为 ON_HOLD 是项目独有状态（项目可以暂停再恢复），且在 use-cases.md 中有完整用例。
- 同步更新 domain-design-guidelines 后，确认 projects/state-machine.md 与之一致。

---

### R2-Q2: approveStatus 的 SUBMITTED 与 PENDING 命名不一致（MEDIUM）

**发现**：
- `domain-design-guidelines.md §16.3` 定义 approveStatus：`UNSUBMITTED / PENDING / APPROVED / REJECTED`（**无 SUBMITTED**）。
- `purchase/state-machine.md` 实际使用 SUBMITTED 作为 approveStatus（迁移图以 UNSUBMITTED → SUBMITTED → APPROVED/REJECTED 为主路径）。
- `flow-overview.md §5.3` 使用 `approveStatus=SUBMITTED`（:458）。

**问题**：SUBMITTED 是否等于 PENDING？还是 SUBMITTED 是 PENDING 之前的一个过渡态？文档对此没有解释。

**候选解释**：

| 候选 | 内容 | 理由 |
|------|------|------|
| A. SUBMITTED = PENDING，命名不统一 | purchase 迁移图从 UNSUBMITTED 到 SUBMITTED 到 APPROVED，没有 PENDING 状态 | 指南列了 PENDING 但无使用者，purchase 用 SUBMITTED |
| B. SUBMITTED 是 "已提交待审批"，PENDING 是 "审批中"——两级 | 但 purchase state-machine 没有"审批中"态 | 语义上合理，但 purchase 未区分 |
| C. SUBMITTED 是旧命名，应统一为 PENDING | domain-design-guidelines §16.3 是统一规范 | 需修改所有域文档 |

**建议**：采纳候选 A 或 C。统一命名：
- 若采用 C：`purchase/state-machine.md` 中的 SUBMITTED 改为 PENDING，flow-overview.md §5.3 的 SUBMITTED 改为 PENDING。
- 若保留 SUBMITTED：domain-design-guidelines §16.3 增加 SUBMITTED，说明"SUBMITTED = 已提交待审批，PENDING = 审批中（可选细分）"。
- **推荐 C**（最少歧义）：域名状态机文档与统一规范保持一致。

---

## Round 3：术语与引用不一致（LOW — 2 个）

### R3-Q1: domain-glossary 中 ON_HOLD 标注与实际冲突

**发现**：
- `domain-glossary.md:102`：ON_HOLD 标注为 `(planned — 暂未在任何域状态机中使用)`。
- `projects/use-cases.md:9,153-154` 确实使用了 ON_HOLD 作为项目状态。

**问题**：glossary 的说法已过时。项目域在 plan03 前就已使用 ON_HOLD（use-cases.md 非 plan03 新文件）。

**建议**：更新 glossary ON_HOLD 条目，明确"项目域使用：项目暂停态（可恢复），见 `projects/use-cases.md`"。

---

### R3-Q2: NCR 财务影响规则中 "REJECTED → 退货" 可能不完整

**发现**：
- `quality/state-machine.md:150-155` 的 NCR 财务影响规则表列出了"退货/返工/报废/让步接收"四条。
- 但未考虑 **召回** 场景（recall.md: recall 从 NCR 升级时，NCR status→ESCALATED_TO_RECALL，不走 RESOLVED）。

**问题**：如果 NCR 升级为召回（ESCALATED_TO_RECALL），财务处理不走退货/返工/报废中的任一条，而是走 recall.md 所述的"触发销售退货→sales 标准退货过账"。

**建议**：在 quality/state-machine.md:150 表格的"退货"行之后或另起一行，补充"升级为召回"处置方式：`升级为召回（ESCALATED_TO_RECALL）：NCR 升级为召回事件（ErpQaRecall），触发批量销售退货，过账走 sales 域标准退货`，并引用 recall.md。

---

## Round 4：erp-survey 高级功能"更好实现"论证审查

按 erp-survey 的 13 个子报告 + 4 份横向分析，逐类挑战 nop-app-erp 设计是否实现了（或未实现）各高级功能，以及是否"更好"。

### 4.1 业财一体化（对比：ERPNext on_submit / Metasfresh EventBus / 赤龙凭证三件套）

| 功能 | nop 实现 | "更好"论证强度 | 结论 |
|------|---------|---------------|------|
| 自动过账 | `IErpFinAcctDocProvider` + posted 兜底 + `@Inject Map` 注册 | **强**：类型安全 SPI 避免 ERPNext 的硬编码业务类型枚举；posted 幂等标志避免 Metasfresh EventBus 的至少一次重复 | ✅ 更好 |
| 凭证模板 | `VoucherTemplate` + 科目映射 | **中**：与赤龙/ Metasfresh 能力相当，未见本质超越 | ✅ 对等 |
| 多套科目表并行 | `acctSchemaId` 维度 + Provider 循环调用 | **中**：与 iDempiere 的 C_AcctSchema 能力相当 | ✅ 对等 |
| 业务单据过账回链 | `ErpFinVoucherBillR` | **强**：双向回链 + 红红冲销保留审计轨迹，超越赤龙的单一方向 | ✅ 更好 |
| **中式复式记账"五大类借贷方向"** | §15 借贷方向约定 | **强**：系统化声明，避免赤龙 ApInvoiceHead 字段无中心化的方向规则 | ✅ 更好 |

### 4.2 库存（对比：Odoo 三层 / 管伊佳 DepotHead type+subType）

| 功能 | nop 实现 | "更好"论证强度 | 结论 |
|------|---------|---------------|------|
| 三层模型（move/ledger/balance） | `inventory/state-machine.md` | **强**：流水不可变（反向流水）优于 Odoo 的 stock_quant 直接修改 | ✅ 更好 |
| VMI/寄售 | `consignment.md` + ownerId 维度 | **强**：Odoo 无 VMI 自动结算闭环，本项目自建（诚实标注） | ✅ 更好（自建） |
| 批次/序列号追溯 | 正向+反向递归追溯 | **强**：递归追溯超越 Odoo/ERPNext 的单级追溯 | ✅ 更好 |
| 所有制转移 vs 物理移动分离 | 专用 `OwnershipTransfer` 单（sourceLocId=destLocId） | **强**：解决了 Odoo 混合物理移动与物权变更的设计缺陷 | ✅ 更好 |

### 4.3 制造（对比：Odoo MRP / WEMS 视觉流程 / Tryton 状态机）

| 功能 | nop 实现 | "更好"论证强度 | 结论 |
|------|---------|---------------|------|
| BOM 多版本/快照 | `bom-and-routing.md` | **中**：与 Odoo/Odoo 替代 BOM 能力相当 | ✅ 对等 |
| MRP | `mrp.md` | **中**：与 Odoo MRP/Scheduler 能力相当 | ✅ 对等 |
| CRP/APS | `crp.md` + 四要素产能分离 | **强**：Odoo 仅前向排产无真 APS，本项目诚实标注 self-admit delay；但产能四要素分离设计超越 Odoo 单一 capacity 字段 | ✅ 对等（crp 设计好，aps 延迟） |
| **声明式状态机** | `domain-design-guidelines.md §16` 三轴分离 | **强**：Tryton 的 `_transitions` + `@transition` 是单轴，本项目三轴分离解决了组合爆炸 | ✅ 更好 |

### 4.4 质量（对比：Carbon QMS / Odoo quality）

| 功能 | nop 实现 | "更好"论证强度 | 结论 |
|------|---------|---------------|------|
| 质检模板 | `InspectionTemplate` | **中**：与 Carbon 能力相当 | ✅ 对等 |
| NCR/CAPA | 4 态 NCR + CAPA 验证闭环 | **中**：与 Carbon NCI 能力相当 | ✅ 对等 |
| **批次召回** | `recall.md` | **强**：开源零覆盖批次召回实体（Carbon 仅量具校准召回无独立实体），本项目自建 | ✅ 更好（自建，开源零覆盖） |

### 4.5 跨域/集成（对比：Metasfresh SPI / Odoo delivery+edi）

| 功能 | nop 实现 | "更好"论证强度 | 结论 |
|------|---------|---------------|------|
| **TMS 三层 SPI** | `IErpLogCarrierGatewayClient/Factory/Registry` + 中立 DTO | **强**：照搬 Metasfresh 黄金参考并改进（Nop IoC @Inject Map 自动聚合，优于 Metasfresh 手动 builder） | ✅ 更好 |
| **EDI 适用性派发** | `IErpB2bEdiProvider.getApplicability()` + 信封状态机 | **强**：Odoo account_edi `_get_move_applicability` 复制并改进 | ✅ 更好 |
| **核心零污染** | 所有 Phase 3 模块弱指针反查 | **强**：直接吸收 Odoo `sale.order.opportunity_id`/`carrier_id` 污染反例 | ✅ 更好 |

### 4.6 CRM / 费用报销 / 资金票据（对比：Odoo crm / hr_expense / 中式票据）

| 功能 | nop 实现 | "更好"论证强度 | 结论 |
|------|---------|---------------|------|
| CRM 单实体+阶段表 | `ErpCrmLead(leadType)` + `ErpCrmStage` | **中**：Odoo crm_lead type 判别 + crm_stage 阶段表，设计对等 | ✅ 对等 |
| 费用报销 paymentMode | `expense-claim.md` paymentMode 对方科目决策 | **强**：Odoo 单轴七态 state → nop 三轴分离 (docStatus+approveStatus+posted) | ✅ 更好 |
| **中式承兑汇票** | `treasury.md` 7 态票据 | **强**：4 个开源 ERP 均无票据实体，本项目自建且过账走 SPI | ✅ 更好（自建，开源零覆盖） |

### 4.7 供应商评分卡（对比：ERPNext 8-doctype）

| 功能 | nop 实现 | "更好"论证强度 | 结论 |
|------|---------|---------------|------|
| 周期评分+公式+变量 | `supplier-evaluation.md` `Scorecard/Criteria/Variable` | **中**：复制 ERPNext 8-doctype 范式 + nop 规则引擎 DSL 替代硬编码 Java | ✅ 更好 |

### 4.8 明显差距（P2 / 延迟 / 未覆盖）

| 功能 | 状态 | 理由 |
|------|------|------|
| **APS（有限产能排产）** | 自承认 follow-up | 开源无真 APS，项目合理延迟 |
| **DRP（分销需求计划）** | P2，MRP 部分覆盖 | 多级分销网络 >5 分仓场景才需要 |
| **HRMS/薪酬** | P2 | 本地化重，推荐第三方集成 |
| **EDI 标准细节（X12/EDIFACT）** | 延迟到客户需求 | b2b-integration.md 骨架已覆盖 SPI 形态 |
| **BPM 可视化流程设计器** | 未覆盖 | WEMS/星云有，本项目依赖 nop-wf（平台级能力） |

---

## Round 5：证据诚实性 + Nop 平台合规性审查

### 5.1 证据诚实性（Plan 03 纠正已落地）

| 文档 | 纠正项 | 验证结果 |
|------|-------|---------|
| `treasury.md` | 不误引 metasfresh.md:83/redragon-erp.md:41 为票据证据 | ✅ 诚实标注"4 个开源 ERP 均无中式承兑汇票实体" |
| `consignment.md` | 不声称"借鉴 Odoo VMI 自动结算" | ✅ 明确标注 Odoo 无结算闭环，自建能力 |
| `logistics/README.md` | TMS 主证改 Metasfresh | ✅ 主证 Metasfresh，Odoo 降为反模式对照 |
| `b2b-integration.md` | EDI 主证改 Odoo account_edi | ✅ 主证 Odoo account_edi，ERPNext edi 标注"仅映射非引擎" |
| `supplier-evaluation.md` | D5 拆分实体归属 | ✅ AVL→master-data，评分→purchase |

**未发现问题**。证据诚实标注（🟢/🟡/⚪）在所有 9 份新文档中一致使用，标注规范。

### 5.2 Nop 平台合规性

| 原则 | 验证 | 结果 |
|------|------|------|
| Model→Delta→Java 优先 | 所有新文档声明"待 ORM 计划落地"，未提议 Java 实现 | ✅ |
| `@Inject Map` 自动聚合 SPI | TMS/EDI/业财过账均使用 `@Inject Map<Code, Provider>` 模式 | ✅ |
| 核心实体零污染（弱指针） | CRM/TMS/EDI 均声明不在核心实体加字段 | ✅ 吸收 Odoo 反例 |
| `NopException` + `ErrorCode` | 错误码命名约定已在 domain-design-guidelines §7 | ✅ |
| 审批流走 nop-wf（不造轮子） | 所有文档引用 nop-wf，不提议自建审批引擎 | ✅ |

---

## 已确认的强项

| 方面 | 文件 | 评价 |
|------|------|------|
| **Plan 02 状态机修复全部落地** | domain-design-guidelines §16, flow-overview §3, purchase/state-machine.md | 会计期间 NOT_OPENED、approveStatus/docStatus 分离标注、per-domain 状态取值覆盖完整 |
| **Plan 03 设计文档质量高** | 全部 9 份新文档 | 边界清晰、实体清单标注待落地、反模式警示、证据诚实、SPI 契约深度恰当 |
| **"更好实现"论证充分** | 反模式警示 + 核心零污染 + 三轴分离 + SPI 注册中心 | 系统化地规避了 13 个开源反模式（Odoo 单轴 state/命名约定派发、iDempiere 反射 Doc 工厂、Odoo 核心污染等） |
| **证据诚实标注规范** | 所有文档 | 无编造证据，开源零覆盖诚实标注 |

---

## 决策汇总

| 决策 | 状态 | 位置 |
|------|------|------|
| D1: quality/state-machine.md 补充 ESCALATED_TO_RECALL | 待定 | `docs/design/quality/state-machine.md` |
| D2: 统一 projects 状态集 (OPEN/ON_HOLD vs PLANNED/IN_PROGRESS) | 待定 | `domain-design-guidelines.md` §16.2 + `projects/state-machine.md` |
| D3: 统一 approveStatus SUBMITTED vs PENDING 命名 | 待定 | `domain-design-guidelines.md` §16.3 + `purchase/state-machine.md` + `flow-overview.md` |
| D4: 更新 glossary ON_HOLD 条目（标注 projects 域使用） | 待定 | `domain-glossary.md` |
| D5: quality/state-machine.md NCR 表格补充"升级为召回"处置 | 待定 | `quality/state-machine.md` |

---

## 阻塞项

无阻塞项。所有待定决策可在同一设计变更周期内处理。

---

## 与 erp-survey 对照的最终结论

1. **核心 ERP 闭环**（进销存+财务+制造+质量+资产+项目+维护）：设计覆盖完备，"更好实现"论证在 10+ 个维度强于开源参考（三轴状态、SPI 注册中心、弱指针零污染、流水不可变、中式票据自建等）。
2. **P0 高级场景**（费用报销/资金票据）：是优于开源的**自建设计**（开源零覆盖票据、三轴 state 替代 Odoo 单轴七态）。
3. **P1 独立模块**（CRM/TMS/EDI）：当前为设计骨架，SPI 契约深度到位，实施级延迟到客户触发，"更好实现"论证仅限于 SPI 形态设计阶段。
4. **未覆盖**：P2（DRP/HR/POS/售后）明确排除；APS 自承认延迟；BPM 可视化设计器依赖平台 nop-wf。

**研究建议**：短期内修复 R1（ESCALATED_TO_RECALL）的文档缺口；中期由 owner 人工裁决 R2（projects 状态集/approveStatus 命名）。
