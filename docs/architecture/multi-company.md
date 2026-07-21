# 多公司架构

## 目的

定义 nop-app-erp 的多公司（多法人）支持机制，包括组织隔离、内部交易与合并抵消。

> **EXPAND 说明（plan 2026-07-22-1000-1 A3）**：本文档从原 52 行概念描述扩展为含可执行语义的 owner doc。
> EXPAND subsumes roadmap 标注的 `intercompany-consolidation.md` deliverable（未单独建文件，语义全部并入本文件）。
> 原概念段（组织模型 / 数据隔离 / Transfer Pricing 概念表 / 合并抵消概念 / 配置继承）保留，其后追加可执行语义段。

## 组织模型

```
集团（ErpMdOrganization, orgType=GROUP，顶层 parentId=null）
    ├─► 公司A（ErpMdOrganization, orgType=COMPANY）  ← 法人根
    │      ├─► 部门1（orgType=DEPARTMENT）
    │      └─► 仓库归属公司A（ErpMdWarehouse.orgId = 公司A.id）
    └─► 公司B（ErpMdOrganization, orgType=COMPANY）  ← 法人根
           ├─► 部门3
           └─► 仓库归属公司B
```

> **事实修正（plan A3 Phase 0 核实）**：原文档图示「集团（ErpMdCorporation）」未实体化 —— 仓库 grep 零命中 `ErpMdCorporation`。
> 集团为顶层 `ErpMdOrganization`（orgType=GROUP）；法人根为 orgType=COMPANY 的 `ErpMdOrganization`。
> 法人根判定 = 沿 `parentId` 自引用链向上走，首个 orgType=COMPANY 的节点即该组织所属法人。

## 数据隔离

- 所有业务单据按 `orgId` 隔离查询
- 单据编号在 orgId 内唯一（见 `domain-design-guidelines.md` §14.1.1）
- 库存按仓库隔离，仓库归属组织
- 凭证按 `acctSchemaId`（账套）隔离

## Transfer Pricing（内部转移定价）— 概念

跨法人调拨触发内部交易凭证：

| 调拨类型 | 会计处理 |
|----------|----------|
| 同法人调拨 | 仅库存移动，无凭证 |
| 跨法人调拨 | 视同买卖，生成内部销售/采购凭证 |

## 合并抵消 — 概念

期末合并报表时需抵消内部交易：

- 内部应收 vs 内部应付抵消
- 内部收入 vs 内部成本抵消
- 内部存货利润抵消

## 配置继承

子公司的配置（科目表、成本核算方法、折旧方法）默认继承集团配置，可按公司覆盖：

| 配置项 | 继承规则 |
|--------|----------|
| 会计科目表 | 按账套独立 |
| 成本核算方法 | 按账套独立 |
| 折旧方法 | 按资产类别独立 |
| 税率 | 按公司独立 |

---

## 跨公司交易生命周期状态机（A3 EXPAND）

跨法人内部交易凭证的生命周期与抵消候选识别解耦：

```
[跨法人调拨 confirm]
        │
        ▼ (config-gated: erp-fin.intercompany-posting-enabled)
[转移定价解析] ──cost-plus/market/negotiated──► [配对凭证生成]
        │                                         │
        │   fromOrg 侧：内部销售凭证(AR)            │   toOrg 侧：内部采购凭证(AP)
        ▼                                         ▼
[ErpFinVoucher POSTED] ◄──bill_r 回链──► [ErpFinVoucher POSTED]
        │
        ▼ (期末，config-gated: erp-fin.consolidation-elimination-enabled)
[runMatching(periodId)] ──配对键扫描──► [ErpFinIntercompanyMatch MATCHED/DIFF]
        │
        ▼
[generateEliminationCandidates(periodId)] ──3 类抵消扫描──► [ErpFinConsolidationElimination CANDIDATE]
        │
        ▼ (人工审核后)
[postElimination(candidateId)] ──生成草稿抵消分录──► [ErpFinVoucher(DRAFT) + Elimination DRAFT_VOUCHER]
```

状态语义：
- **同法人调拨**：config-gated 不影响 —— 始终仅库存移动，无凭证（既有行为完全不变）
- **跨法人调拨**：config-gated 默认 false（保护既有基线）；启用后经转移定价规则生成配对凭证
- **配对/抵消**：均为期末批处理，config-gated 默认 false，不改变日常过账路径

## 转移定价规则模型（A3 EXPAND）

### Decision A — 转移定价模型范围

**裁决**：
1. **三策略全量落地**（cost-plus / market / negotiated），非仅 cost-plus 试点。理由：三策略实现复杂度差异小（均是一个解析方法 + 字段集），全量落地避免 successor 反复改 ORM。
2. **触发点 = 库存调拨 `ErpInvTransferOrder.confirm` 后置**（经 finance SPI `IErpFinIntercompanyTransferBiz`）。不新建独立内部交易单据 —— 复用既有调拨单作为唯一触发源，避免双源真相。
3. **替代方案（已否决）**：独立内部交易单据 `ErpFinIntercompanyTransaction`。否决理由：与调拨单双源易不一致；转移定价是调拨的会计派生，不是独立业务单据。

### 实体：`ErpFinIntercompanyTransferPrice`

转移定价规则表（权威源 `module-finance/model/app-erp-finance.orm.xml`）：

| 字段 | 类型 | 语义 |
|------|------|------|
| fromOrgId / toOrgId | BIGINT | 调出/调入组织（空=通配） |
| materialId / materialCategoryId | BIGINT | 物料/物料分类维度（空=通配） |
| pricingMethod | dict `erp-fin/transfer-pricing-method` | COST_PLUS / MARKET / NEGOTIATED |
| markupRate | DECIMAL | COST_PLUS 加成率（如 0.10 = 加成 10%） |
| fixedPrice | DECIMAL | NEGOTIATED 固定单价 |
| marketRefSource | VARCHAR | MARKET 取价来源说明（如「上月采购均价」） |
| validFrom / validTo | DATE | 有效期（复用 C3 `IDateRange` MUTEX 语义） |

**解析算法**（`IErpFinTransferPriceResolver`）：
1. 按 (fromOrgId, toOrgId, materialId) 精确查 → 命中返回
2. 回落到 (fromOrgId, toOrgId, materialCategoryId) → 命中返回
3. 回落到全通配 default 规则 → 命中返回
4. 全空 → 抛 `ERP_FIN_TRANSFER_PRICE_NOT_FOUND`（config-gated strict 时）或返回 null（默认，保留调拨不生成凭证）
5. 命中后按 pricingMethod 计算单价：COST_PLUS = 成本 × (1+markupRate)；MARKET = 取价（当前实现按 fixedPrice 兜底，真实市场价接入归 successor）；NEGOTIATED = fixedPrice

**缓存**：进程内 `ConcurrentHashMap` + eager load + CRUD 主动失效（对齐 A1 `ErpFinGlMappingResolver`）。

### Decision B — 跨公司凭证生成路径

**裁决**：经独立 `IntercompanyVoucherGenerator`（与 A2 `CommitmentVoucherGenerator` 同型）生成配对凭证，**不走 `ErpFinAcctDocRegistry` Provider 路由**。

理由：
1. 跨法人配对凭证是「一次调拨 → 两条凭证（双法人双账套）」，与单 Provider 单凭证模型不匹配。
2. `INTERCOMPANY_SALE` / `INTERCOMPANY_PURCHASE` 不进入 `ErpFinBusinessType` 枚举（与 BUDGET/COMMITMENT 同裁决：影子/派生凭证不走 Provider 路由）。
3. `IntercompanyAcctDocProvider` 类存在仅文档化科目解析约定（与 `CommitmentAcctDocProvider` 同型，`getSupportedBusinessTypes()` 返回空集）。

**与 GL Mapping intercompany 维度的协同**：配对凭证的 `VoucherFact.accountKey` 设置 `INTERCOMPANY_AR` / `INTERCOMPANY_AP` / `INTERCOMPANY_REVENUE` / `INTERCOMPANY_COST`，经 A1 `ErpFinGlMappingResolver` 解析目标科目（解除 A1 Deferred「intercompany 维度规则」）。

## 公司间自动配对算法（A3 EXPAND）

### Decision C — 配对识别键与一致性校验

**配对键** = `(pairKey, periodId)`，其中 `pairKey = min(fromOrgId,toOrgId) + ":" + max(fromOrgId,toOrgId) + ":" + materialId`。

**`runMatching(periodId)` 算法**：
1. 扫描本期 `ErpFinVoucher`（billType=INTERCOMPANY_SALE/PURCHASE，未红冲）按 pairKey 分组
2. 每个 pairKey 下：AR 侧金额合计 vs AP 侧金额合计
3. 差额 ≤ precision → 写 `ErpFinIntercompanyMatch`（status=MATCHED，matchedAmount=较小侧）
4. 差额 > precision → 写 `ErpFinIntercompanyMatch`（status=DIFF，diffAmount=差额）

**`checkDualSideConsistency(pairKey, periodId)`**：返回 `DualSideDiffReport`（复用 `DualSideDiffReport.DualSideDiffRow` 结构范式，对齐 plan `2026-07-12-0204-2` `checkDualSideConsistency`）。

## 合并抵消范围（A3 EXPAND）

### Decision D — 合并抵消落地范围

**裁决**：本期仅落**抵消候选集识别 + 抵消分录草稿生成**，**不含实时合并报表渲染**（归 successor）。

**本期落地抵消 3 类**：
| 类型 | eliminationType | 抵消逻辑（候选识别） |
|------|----------------|---------------------|
| 内部 AR vs AP | AR_AP | 扫描 `ErpFinIntercompanyMatch` MATCHED 记录，抵消金额 = matchedAmount |
| 内部收入 vs 成本 | REVENUE_COST | 扫描配对凭证 INTERCOMPANY_REVENUE vs INTERCOMPANY_COST 行 |
| 内部存货利润 | INVENTORY_PROFIT | **试点**：仅当 config `erp-fin.elimination-inventory-profit-enabled=true` 时识别（默认 false，依赖未实现利润计算复杂度） |

**successor 触发条件**：实时合并报表渲染（合并资产负债表/利润表）→ 业务客户合并报表需求 + nop-report successor plan。

### `ErpFinConsolidationElimination` 状态机

```
CANDIDATE ──postElimination──► DRAFT_VOUCHER ──(人工过账)──► POSTED
```

- **CANDIDATE**：`generateEliminationCandidates` 识别的待抵消候选
- **DRAFT_VOUCHER**：`postElimination` 生成的草稿抵消分录凭证（`ErpFinVoucher` docStatus=DRAFT）
- **POSTED**：人工审核过账后（既有凭证过账路径，本计划不重复实现）

## 与 Posting + GL Mapping 关系（A3 EXPAND）

| 机制 | 职责 | 与 A3 关系 |
|------|------|-----------|
| `ErpFinPostingProcessor` | 单据过账编排 | A3 不改其核心；配对凭证经独立 Generator 写入 |
| `ErpFinAcctDocRegistry` | Provider 路由 | A3 不接入路由（INTERCOMPANY_* 不进枚举） |
| A1 `ErpFinGlMappingResolver` | 多维科目解析 | A3 新增 intercompany 维度（fromOrgId/toOrgId）+ 4 accountKey |
| A2 `CommitmentVoucherGenerator` | 影子凭证生成 | A3 `IntercompanyVoucherGenerator` 同型范式 |

## 反模式自检表

| # | 反模式 | 正确做法 |
|---|--------|---------|
| AP-1 | 将 INTERCOMPANY_SALE/PURCHASE 加入 `ErpFinBusinessType` 枚举并经 Provider 路由 | 保持影子凭证独立 Generator（与 BUDGET/COMMITMENT 同裁决） |
| AP-2 | 跨法人调拨不 config-gate 直接生成凭证 | 必须 config-gate（默认 false），保护既有基线零回归 |
| AP-3 | 合并抵消自动过账（不经人工审核） | 仅生成 DRAFT_VOUCHER，人工审核后过账 |
| AP-4 | 实时合并报表渲染混入抵消候选识别 | 候选识别与报表渲染解耦；渲染归 successor |
| AP-5 | 同法人调拨生成凭证 | 同法人始终仅库存移动（config-gate 不影响同法人路径） |
| AP-6 | 转移定价规则不设有效期（validFrom/validTo） | 复用 C3 `IDateRange` MUTEX 语义，规则按日期互斥 |
| AP-7 | 在 inventory 域硬编码跨法人判定逻辑 | 跨法人判定 + 定价解析 + 凭证生成全部在 finance 域（SPI 调用方仅传 fromWarehouseId/toWarehouseId） |

## 落地证据（plan 2026-07-22-1000-1）

见 `docs/backlog/deepening-roadmap.md §8.9 A3 落地证据`。
