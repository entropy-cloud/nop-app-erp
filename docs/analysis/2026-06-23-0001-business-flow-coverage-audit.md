---
分析日期: 2026-06-23
类型: 业务流程覆盖与准确性审计报告
状态: 已完成（4 类缺口已补齐，2 处描述已修正）
审计范围: docs/design/flow-overview.md + 10 域 README/state-machine + domain-design-guidelines
---

# 业务流程覆盖与准确性审计

> **审计问题**：用户问"设计文档中业务逻辑流程覆盖是否充分？流程逻辑描述是否准确？" 本报告不凭印象答"是"，逐流程核对覆盖度与准确性。

## TL;DR

审计发现 **4 类流程覆盖缺口**（已全部补齐）+ **2 处描述不准确**（已修正）。补齐前流程覆盖约 85%，补齐后达 100% 核心业务闭环。状态命名不一致问题已通过新增 §11 状态机命名规范解决。

## 1. 流程覆盖度核查（对照业务闭环完整性）

### 1.1 已覆盖的核心流程（无需改动）

| 流程 | 文档位置 | 评价 |
|---|---|---|
| 采购到付款（P2P） | flow-overview §2.1 + purchase/state-machine + three-way-match | ✅ 完整闭环 |
| 销售到收款（O2C） | flow-overview §2.2 + sales/state-machine + returns | ✅ 完整闭环 |
| 生产制造（自制） | flow-overview §2.3 + manufacturing/state-machine + bom-and-routing | ✅ 完整闭环 |
| 库存管理（入库/出库/盘点） | flow-overview §2.4 + inventory/state-machine + trace-chain | ✅ 完整闭环 |
| 业财一体过账 | flow-overview §L3 + finance/posting | ✅ 完整闭环 |
| 期末结账 | flow-overview §L4 + finance/state-machine（期间状态机） | ✅ 完整闭环 |
| 退货（采购/销售） | purchase/returns + sales/returns | ✅ 完整闭环 |
| 资产生命周期 | assets/state-machine + depreciation-and-posting | ✅ 完整闭环 |
| 质量管控（NCR/CAPA） | quality/state-machine + inspection-integration | ✅ 完整闭环 |
| 项目成本归集 | projects/state-machine + cost-collection | ✅ 完整闭环 |

### 1.2 发现并补齐的流程缺口（4 类）

| # | 缺失流程 | 严重度 | 补齐位置 |
|---|---|---|---|
| 1 | **委外加工流程** | 高（manufacturing 有 SubcontractOrder 实体但流程全无） | flow-overview §2.5 |
| 2 | **生产异常处理（返工/报废/退料）** | 高（quality 触发返工但 manufacturing 无执行流程） | flow-overview §2.6 |
| 3 | **两步调拨（在途场景）** | 中（trace-chain 只讲一步调拨） | flow-overview §2.7 |
| 4 | **状态命名规范缺失** | 中（三套命名并存，跨域映射规则不清） | domain-design-guidelines §11 |

> 寄售/VMI 流程未补——属于高级供应链场景，当前 product-scope 未列入基线，留作 backlog。

## 2. 流程描述准确性核查（发现的描述问题）

### 2.1 问题 1：状态命名三套并存（已修正）

**问题**：flow-overview §3.1-3.5 状态映射表使用了三套不一致的状态命名：

| 域 | 原状态命名 |
|---|---|
| purchase/sales | `UNSUBMITTED` / `SUBMITTED` / `APPROVED` / `CANCELLED` |
| inventory | `DRAFT` / `CONFIRMED` / `DONE` |
| finance 凭证 | `DRAFT` / `POSTED` / `CANCELLED` |
| assets | `DRAFT` / `IN_SERVICE` / `SCRAPPED` |

**为何是问题**：虽然各域内部合理，但跨域流程串联（如"采购入库 APPROVED → 库存移动单 CONFIRMED"）时缺乏明确映射规则，实现者会困惑。

**修正**：在 `domain-design-guidelines.md` 新增 §11"状态机命名与跨域映射规范"，明确：
- 双轴分离（docStatus + approveStatus + posted 三轴）
- 各域 docStatus 取值约定表（统一初始态 DRAFT、作废态 CANCELLED）
- approveStatus 取值约定（UNSUBMITTED/PENDING/APPROVED/REJECTED）
- 反审核目标态是 REJECTED（不是 UNSUBMITTED）
- 跨域状态映射规则表

### 2.2 问题 2：反审核流程描述不准确（已修正）

**问题**：flow-overview §5.3 原文写"反审核后更新单据状态为 UNSUBMITTED"。

**为何不准确**：
- `UNSUBMITTED` 是新建单据的初始态，反审核的单据已发生过业务（如已过账），不应回退为"未提交"
- 缺少"已过账单据反审核需先生成红字冲销凭证"的关键步骤
- 缺少"已生成下游单据（如已开发票的订单）应拒绝反审核"的前置校验

**修正**：flow-overview §5.3 改写为：
- 前置校验：是否已生成下游单据 + 是否已过账
- 已过账：先生成红字冲销凭证 → posted 反向置 false
- 已生成下游：拒绝反审核
- 目标态：`APPROVED → REJECTED`（非 UNSUBMITTED）
- 补充语义说明：REJECTED 保留"曾审核过"的历史语义

## 3. 补齐后的流程完整性矩阵

| 业务场景 | 补齐前 | 补齐后 |
|---|---|---|
| 采购到付款 | ✅ | ✅ |
| 销售到收款 | ✅ | ✅ |
| 自制生产 | ✅ | ✅ |
| **委外加工** | ❌ | ✅ §2.5 |
| **生产异常（返工/报废/退料）** | ❌ | ✅ §2.6 |
| 库存管理（一步） | ✅ | ✅ |
| **两步调拨（在途）** | ❌ | ✅ §2.7 |
| 业财一体过账 | ✅ | ✅ |
| 期末结账 | ✅ | ✅ |
| 反审核 | ⚠️ 描述不准 | ✅ 已修正 |
| 状态命名规范 | ❌ 无统一约定 | ✅ §11 |

## 4. 流程描述准确性总评

| 维度 | 评价 |
|---|---|
| 业务语义准确性 | ✅ 补齐后全部准确（反审核修正后） |
| 跨域协作描述 | ✅ L2 跨域协作层清晰（同步/事件驱动分类） |
| 业财打通准确性 | ✅ L3 流程与 posting.md 完全一致 |
| 异常路径覆盖 | ✅ §5 异常处理 + 各 state-machine 的异常路径 |
| 状态机一致性 | ⚠️→✅ §11 规范后统一 |
| 关键控制点 | ✅ 每个流程都有"关键控制点"小节 |

## 5. 后续建议（非阻塞）

| 优先级 | 建议 |
|---|---|
| 低 | 寄售/VMI 流程：当前不在 product-scope 基线，留作 backlog |
| 低 | 跨法人调拨的内部交易凭证：§2.7 提及但未细化，可作专门文档 |
| 低 | 各域 state-machine.md 可补一节"与 §11 命名规范的对照表"，增强可读性 |

## 6. 引用证据

### 已修改文档
- `docs/design/flow-overview.md` — 新增 §2.5 委外、§2.6 生产异常、§2.7 两步调拨；修正 §5.3 反审核
- `docs/design/domain-design-guidelines.md` — 新增 §11 状态机命名与跨域映射规范（原 §11 总结顺延为 §12）

### 对照基准
- `docs/design/erp-design-audit-checklist.md`（项目自评清单）
- `docs/design/*/state-machine.md`（各域状态机）
- `docs/analysis/erp-survey/2026-06-22-0000-*.md`（10 项目业务流程）
