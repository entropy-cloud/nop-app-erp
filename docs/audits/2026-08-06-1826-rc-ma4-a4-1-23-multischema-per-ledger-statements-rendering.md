# RC MA4 A4.1.23 多账套「每账套独立三表」运行时渲染符合性验证报告

> 里程碑：MA4（RC） | 工作项：A4.1.23 | 域：finance
> 来源：`docs/backlog/requirement-compliance-roadmap.md` Work Item A4.1.23（A1.7 §7 SP-2 展开）
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§2 判据 / §4 Q1 真相源层级 / §7 arm-index 衔接 / §8 过程纪律 / §9 真相源冻结 / §去重协议 MA4↔A5.6）
> Skill：`docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）
> 审计类型：**只读运行时行为验证**（读报表方法签名 + 主账套 scope 解析 + AcctSchemaResolver + 多账套 config + L1/L2 措辞核对；不改代码/ORM/api.xml/真相源）
> 范式对齐：A4.1.21（period-close 运行时行为评估同型工作项 done）/ A4.1.22（cash flow postingType 运行时行为评估 done）
> 来源 Plan：`docs/plans/2026-08-06-1826-2-rc-ma4-a4-1-23-multischema-per-ledger-statements-rendering.md`

---

## 存疑点原文（A1.7 §7 SP-2）

> 来源：`docs/audits/2026-08-02-2115-rc-ma1-a1-7-finance-f7-reports-dashboards-multischema.md` §7 SP-2（`:287`）

**SP-2 原文**：「多账套部署（`multi-schema-enabled=true`）下"每账套独立三表"运行时渲染 — 当前读路径取主账套 FINANCIAL，非按账套切换渲染」。

- **静态状态**：GlBalance 物理按 acctSchemaId 隔离已落地（写路径，UC-FIN-05 ② 接受）；读路径取主账套是合理简化（UC-FIN-05 ② 接受）；L1 UC-FIN-16 涉及机制提"每账套独立三表"但未列为验收标准。
- **MA4 运行时确认方式**：多账套部署下按 acctSchemaId 参数切换报表渲染（当前 `balanceSheetData(periodId)` 无 acctSchemaId 参数）。

---

## 1. 需求契约原文（L1 逐字引用，§1 L1 格式）

### UC-FIN-16 财务三大报表（`docs/design/finance/use-cases.md:318`）

**场景**：基于科目余额生成资产负债表/利润表/现金流量表。

**可验证断言**（逐字引用 `use-cases.md:322-340`）：
```
// 数据来源
三表基于 ErpFinGlBalance(按 subject×period×维度 聚合的余额)

// 资产负债表(恒等式)
资产合计 == 负债 + 所有者权益

// 利润表
收入 - 成本 - 费用 = 净利润
净利润结转至资产负债表"未分配利润"

// 现金流量表
按科目现金流分类(经营/投资/筹资)调整
间接法: 净利润 + 非现金项目 + 营运资金变动

// 期间控制
报表基于已 CLOSED 期间的 GlBalance(未结账期间数据不完整)
```

**涉及机制**（逐字引用 `use-cases.md:342`）：`ErpFinGlBalance、period-close.md、多账套(每账套独立三表)`

> **关键观察（本存疑点核心）**：短语「多账套(每账套独立三表)」**仅出现于涉及机制 `:342`**，**未出现于**可验证断言 `:322-340` 的任何一条（数据来源 / BS 恒等式 / IS 公式 / CF 分类+间接法 / 期间控制 均未提 per-ledger 切换渲染）。

---

## 2. 实现证据（L3 代码路径，写时实测行号）

> HEAD 复核锚点。`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/report/ErpFinReportBizModel.java`。

### 2.1 报表读入口签名核验 — 无 acctSchemaId 参数（Phase 1 Proof 1）

| 读入口 | 签名（file:line，写时实测） | 调用 | 证据 |
|--------|----------------------------|------|------|
| 资产负债表 | `balanceSheetData(@Name("periodId") Long periodId, IServiceContext context):238` `@BizQuery` | → `buildBalanceSheetDataset(periodId):239` | **仅含 periodId，无 acctSchemaId 参数** |
| 利润表 | `incomeStatementData(@Name("periodId") Long periodId, IServiceContext context):244` `@BizQuery` | → `buildIncomeStatementDataset(periodId):245` | **仅含 periodId，无 acctSchemaId 参数** |
| 现金流量表 | `cashFlowStatementData(@Name("periodId") Long periodId, IServiceContext context):250` `@BizQuery` | → `buildCashFlowDataset(periodId):251` | **仅含 periodId，无 acctSchemaId 参数** |

**数据集构建方法签名（:269-304）**：
- `buildBalanceSheetDataset(Long periodId):269` → `loadGlBalances(periodId):272`
- `buildIncomeStatementDataset(Long periodId):284` → `loadGlBalances(periodId):287`
- `buildCashFlowDataset(Long periodId):299` → `loadPostedVoucherLines(periodId):302`

**结论**：三个 `@BizQuery` 读入口 + 三个 `buildXxxDataset(periodId)` 构建 方法**全部仅含 periodId**，**无 acctSchemaId 参数** → 读入口无 per-ledger 切换参数。证实 SP-2「当前 `balanceSheetData(periodId)` 无 acctSchemaId 参数」。

> 注：同文件 `buildBudgetVsActualDataset(Long acctSchemaId, Long periodId, Long subjectId):230` **含** acctSchemaId 参数——但该方法属预算 vs 实际报表（非 UC-FIN-16 三表），且其 acctSchemaId 经前端 AMIS `DS_VAR` 传入（`:197`），非三表读路径。三表读入口恒取主账套，不证伪本结论。

### 2.2 主账套 scope 解析核验 — 恒取主账套（Phase 1 Proof 2）

**scope 注入点注释（`:472-474`，逐字）**：
```java
// ===================== 多账套/多组织读路径隔离 scope（P1-MA2-095）=====================
// 多账套部署下报表按 periodId 聚合会双计；按期间所属组织 + 主账套补 filter 使单账套不双计。
// scope 不可解析时（period.orgId 为空等）跳过 filter，保护单组织基线零回归。
```

**scope 解析链（`:476-499`，写时实测）**：
- `resolvePeriodOrgId(Long periodId):476-482`：`daoProvider.daoFor(ErpFinAccountingPeriod.class).getEntityById(periodId).getOrgId()` → 返回期间所属组织 orgId（periodId 为空返回 null）。
- `resolveOrgSchemaId(Long orgId):484-486`：`orgId != null ? AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId) : null` → **委托 `AcctSchemaResolver.resolvePrimarySchemaId`**。
- `applyOrgAndSchemaScope(QueryBean q, Long periodId):488-499`（适用于 GlBalance/Voucher）：
  ```
  Long orgId = resolvePeriodOrgId(periodId);   // :490
  if (orgId == null) return;                    // :491-493  scope 不可解析跳过 filter
  q.addFilter(eq("orgId", orgId));              // :494
  Long schemaId = resolveOrgSchemaId(orgId);    // :495  → 仅取主账套
  if (schemaId != null) q.addFilter(eq("acctSchemaId", schemaId));  // :496-498
  ```

**AcctSchemaResolver.resolvePrimarySchemaId（`module-master-data/erp-md-dao/.../AcctSchemaResolver.java:28-40`）**：
```java
public static Long resolvePrimarySchemaId(IDaoProvider daoProvider, Long orgId) {
    ...
    return schemas.stream()
            .min(Comparator.comparingInt(AcctSchemaResolver::schemaPriority))  // :37
            .map(ErpMdAcctSchema::getId)
            .orElse(null);
}
// nature 优先级（:50-59）：FINANCIAL(0) → MANAGEMENT(1) → TAX(2) → CONSOLIDATION(3) → BUDGET(4) → 其他(99)
```
**取 nature 优先级最低（FINANCIAL=0）的 ACTIVE 账套 = 主账套**，恒定选取 FINANCIAL，**不按调用方意图切换**。

**loadGlBalances scope 应用点（`:386-413`）**：
- `loadGlBalances(periodId)` 在 `:397`（periodId 缺省取最近期间分支）+ `:403`（指定 periodId 分支）均调用 `applyOrgAndSchemaScope(q, periodId)`。
- `loadPostedVoucherLines(periodId):424` 在 `:430` 调用 `applyOrgAndSchemaScope(vq, periodId)`。

**结论**：多账套部署下报表渲染恒取**主账套 FINANCIAL** 三表，**无按 acctSchemaId 切换渲染次账套（MANAGEMENT/TAX 等）三表的入口**。次账套三表数据**物理存在**（GlBalance 按 acctSchemaId 隔离已落地，写路径），但**读路径无 per-ledger 选择入口**。证实 SP-2「读路径取主账套 FINANCIAL，非按账套切换渲染」。

---

## 3. 测试证据（L4，复用 A1.7 §3）

> 本验证为符合性裁决（读签名 + scope 解析 + L1/L2 措辞核对），不新增测试。既有隔离测试作为输入证据。

**`TestErpFinMultiSchemaReportIsolation#testBalanceSheetScopedToPrimarySchema:47-73`**（强断言，A1.7 §3 复用）：
- 同期间同组织同科目两账套（FINANCIAL 余额 1000 + MANAGEMENT 余额 3000），报表 `assertEquals(0, totalAsset.compareTo(new BigDecimal("1000")))` — **仅主账套 1000，不双计为 4000**。
- 证实读路径 scope 取主账套 FINANCIAL 行为稳定可重现（R1.29 fix 证据，P1-MA2-095 resolved）。

> 该测试断言的是「取主账套不双计」（双计维度），**未断言**「按账套切换渲染」（per-ledger 切换维度）——本验证补的就是后者维度差异。

---

## 4. 运行时行为证据（L5）

| 行为 | 证据来源 | 复用/差异 |
|------|---------|----------|
| 多账套写路径 stamp acctSchemaId + GlBalance 物理隔离 | A1.7 §2.1（`SchemaPropagator:44-81` + `ErpFinGlBalance.acctSchemaId` mandatory `orm.xml:910`） | **复用** UC-FIN-05 ② 接受（写路径隔离落地） |
| 读路径取主账套不双计 | A1.7 §2.4（`applyOrgAndSchemaScope` R1.29 fix）+ `TestErpFinMultiSchemaReportIsolation` 强断言 | **复用** P1-MA2-095 resolved R1.29 |
| 读入口无 acctSchemaId 参数（per-ledger 切换缺失） | 本报告 §2.1（`:238/244/250` 三 `@BizQuery` 签名） | **本验证补**（per-ledger 切换维度，非双计） |
| 多账套部署恒取主账套三表 | 本报告 §2.2（`resolvePrimarySchemaId` FINANCIAL=0 + `applyOrgAndSchemaScope:489-499`） | **本验证补**（恒取主账套，无切换入口） |

---

## 5. 符合性裁决（§2 判据 + §4 Q1 真相源层级 + 三源对照）

### 5.1 L1 涉及机制 vs 可验证断言判据力核验（Phase 1 Proof 3，本存疑点核心）

按方法论 §4 Q1 真相源层级裁决：

| 层级 | 真相源 | 角色 | 权威性 |
|------|--------|------|--------|
| 2（功能契约） | `use-cases.md` UC-FIN-16 **可验证断言** `:322-340`（5 条） | 功能验收契约 | **权威（符合性判据）** |
| 3（设计参考） | `use-cases.md` UC-FIN-16 **涉及机制** `:342`「多账套(每账套独立三表)」 | 设计语境 | **非真相源（非约束力）** |

**裁决推理**：
1. 短语「多账套(每账套独立三表)」**仅**位于 L1 涉及机制 `:342`（设计语境），**未**位于可验证断言 `:322-340`（符合性判据）。
2. §4 Q1 明确：涉及机制是**设计语境（非约束力）**，可验证断言才是**符合性判据**。5 条可验证断言（数据来源 / BS 恒等式 / IS 公式 / CF 分类+间接法 / 期间控制）**无一条**要求「读入口暴露 per-ledger 切换渲染」。
3. 涉及机制「每账套独立三表」的字面事实**成立**——GlBalance **物理按 acctSchemaId 隔离**（写路径 stamp + `ErpFinGlBalance.acctSchemaId` mandatory `orm.xml:910`），每账套余额行独立存在，次账套三表数据**物理可查**（直查 GlBalance by acctSchemaId 可得）。即「数据物理隔离可查」维度**已满足**。
4. L1 可验证断言未将「读入口暴露 per-ledger 切换」升为验收义务 → 读入口取主账套是**合理简化**，不违反任何可验证断言。

### 5.2 L2 doc↔code drift 评估（Phase 1 Proof 4）

**L2 实际描述（`docs/design/finance/multiple-accounting-schemas.md §账套查询与报表:149-169`，逐字核对）**：
- §多账套查询 `:151-169`：描述的是**凭证查询按账套过滤**（财务账 / 管理账 / 全部账套 `:156-160`）+ 按币种过滤 + 按时间过滤。
- §账套对账 `:171-189`：科目余额对账 / 凭证数量对账 / 业务单据关联对账 / 异常报告。
- **关键事实**：短语「每账套独立三表」**不存在于 L2**（`multiple-accounting-schemas.md` 全文 grep 零命中该短语）；L2 §账套查询与报表 描述的 per-ledger 过滤能力针对**凭证查询**（非报表三表读入口）。

**drift 裁决**：
- L2 约束的是**凭证查询按账套过滤**能力（`凭证查询 ├─ 按账套过滤`），与**报表三表读入口**是不同控制点。
- 报表三表读入口（`balanceSheetData/incomeStatementData/cashFlowStatementData(periodId)`）取主账套，**不与 L2 凭证查询过滤描述冲突**（L2 未对报表三表读入口下 per-ledger 切换义务）。
- **结论**：报表读入口与 L2 **无 doc↔code drift**（L2 仅约束凭证查询过滤，非报表读入口）。
- 即便解读为 L2 隐含「泛化 per-ledger 查询能力」含报表侧，按 §4 Q1：L1（更高权威层级 2）的可验证断言**未**将 per-ledger 切换升为验收义务，L2（层级 3 设计参考）的隐含解读**不构成 P1**（L1 未升级即不约束）。

### 5.3 既有 MA3 successor 裁决交叉引用（Phase 1 Proof 5）

**arm-index `P1-MA2-095` 行（`docs/audits/arm-index.md:552`）RC MA3 复查注记（2026-08-07）**：
> §对账差异 #5 核心核实项——finding resolved R1.29 via implementation[读路径 period.orgId + 主账套 scope 过滤已修复不双计，A1.7 RC §4 HEAD 复核确认]，successor[`multi-schema-enabled=true` 部署下「每账套独立三表」运行时渲染]触发条件未满足→维持 backlog successor（`2026-08-07-0430-rc-ma3-a3-5-...md` §2.6）；不回队 MR1；**与 A4.1.23 交叉引用**[不同控制点：MA3 successor 触发条件 vs MA4 运行时渲染行为]

**MA3 A3.5 §2.6 #6 裁决（`docs/audits/2026-08-07-0430-rc-ma3-a3-5-ext-cross-domain-successor-review.md:30,47`）**：
> #6 多账套 acctSchemaId 读路径隔离：`P1-MA2-095` finding resolved R1.29 via implementation（读路径 period.orgId + 主账套 scope 过滤，A1.7 RC §4 HEAD 复核确认已修复不双计）；其 successor（`multi-schema-enabled=true` 部署下「每账套独立三表」运行时渲染）**触发条件未满足** → **维持 backlog successor**。与 A4.1.23 交叉引用（不同控制点：MA3 successor 触发条件[部署启用] vs MA4 运行时渲染行为[每账套独立三表渲染]）。

**本验证 successor 裁决与既有 MA3 裁决对齐**：
- MA3 A3.5 裁决「successor 触发条件未满足→维持 backlog」（部署侧触发条件 = 多组织/多账套部署是否启用）。
- 本 MA4 裁决「行为符合需求契约」（L1 可验证断言未要求 per-ledger 切换 → 维持接受）。
- 两者各自裁决、交叉引用不冲突（MA3 = successor 触发条件是否回队 MR1；MA4 = 运行时行为是否符合 L1）。
- **本验证维持接受/无升级，与 MA3「successor 维持 backlog」一致**——无触发条件变化，无裁决冲突。

### 5.4 MA4↔A5.6 边界声明（Phase 1 Proof 6）

按方法论 §去重协议 MA4↔A5.6 边界：
- **MA4（本验证）**：审「行为是否符合需求」（多账套 per-ledger 三表渲染是否符合 L1/L2）。本验证裁决：符合（L1 可验证断言未要求 per-ledger 切换 + 数据物理隔离已落地）。
- **A5.6（audit-remediation）**：审「E2E 断言强度」（测试质量视角）。
- 判据不同，按此边界执行，**本验证不重做 A5.6 E2E 断言强度审计**。

### 5.5 per-ledger 渲染维度运行时裁决（Phase 1 Decision）

**裁决：维持 UC-FIN-05 ② 接受 + UC-FIN-16 涉及机制满足，无新 finding（Decision 分支 ①）**

**判据编号 + 三源对照**：

| 维度 | L1（权威） | L2（设计参考） | L3（实测锚点） | 裁决 |
|------|-----------|---------------|---------------|------|
| 可验证断言是否要求 per-ledger 切换 | `:322-340` 5 条断言**无一条**提 per-ledger 切换 | — | — | **L1 未下验收义务** |
| 涉及机制措辞 | `:342`「多账套(每账套独立三表)」= 设计语境非约束力 | 短语**不存在**于 L2（L2 §:149-169 描述凭证查询过滤+对账） | — | **L1 设计语境，L2 无该短语** |
| 数据物理隔离 | 可验证断言 ②（UC-FIN-05）「GlBalance 按 acctSchemaId 隔离」 | `multiple-accounting-schemas.md §数据隔离:243` | `ErpFinGlBalance.acctSchemaId` mandatory `orm.xml:910` + 写路径 stamp | **已落地（物理可查）** |
| 读入口 per-ledger 切换 | 无验收义务 | L2 仅约束凭证查询过滤（非报表读入口） | `:238/244/250` 三 `@BizQuery` 仅 periodId + `applyOrgAndSchemaScope` 取主账套 | **合理简化（无切换入口）** |

**分层一致性**（与既有接受/修复分层）：
- **UC-FIN-05 ② 接受**（写路径隔离）：GlBalance 按 acctSchemaId 物理隔离已落地（stamp + mandatory）→ 数据「每账套独立」物理成立。**本验证不撤销**。
- **P1-MA2-095 resolved R1.29**（读路径双计）：读路径 period.orgId + 主账套 scope 过滤已修复不双计（`TestErpFinMultiSchemaReportIsolation` 强断言 1000 not 4000）→ 双计维度已闭环。**本验证不撤销**（per-ledger 渲染是双计修复之外的「切换选择」维度，非双计）。
- **MA3 A3.5 successor**（per-ledger 渲染触发条件）：successor 触发条件未满足→维持 backlog。**本验证一致**（维持接受无升级，无触发条件变化）。

**裁决结论**：
1. L1（§4 更高权威）可验证断言 `:322-340` **未**要求「读入口暴露 per-ledger 切换」——读入口取主账套不违反任何可验证断言。
2. L1 涉及机制「每账套独立三表」= 设计语境（非约束力）；其字面事实**成立**（GlBalance 物理按 acctSchemaId 隔离，每账套余额行独立，次账套数据物理可查）。
3. L2（层级 3 设计参考）`§账套查询与报表:149-169` 描述的是**凭证查询按账套过滤**（非报表三表读入口），报表读入口与 L2 **无 drift**；即便隐含泛化解读，L1 未升级即不约束（§4 Q1）。
4. → **维持 UC-FIN-05 ② 接受 + UC-FIN-16 涉及机制满足，无新 finding**。per-ledger 三表切换读入口缺失属**合理简化 successor**（GlBalance 物理隔离已落地，与 MA3 A3.5 successor 裁决一致，不构成 P0/P1/P2）。

> **未走 Decision 分支 ②（P2 watch-only）的理由**：分支 ② 要求「L1/L2 解读为要求读入口暴露 per-ledger 切换而实现仅主账套」。经 §4 Q1 核验，L1 可验证断言未下此义务（涉及机制非约束力）+ L2 仅约束凭证查询（非报表读入口）→ 分支 ② 前提不成立，不登记 P2。doc↔code drift 经 §5.2 核验为「无 drift」（L2 未对报表读入口下义务），无须 owner doc 标注。

---

## 6. 与 arm-index 衔接（§7 复用 or 新增 裁决）

> 方法论 §7：产出 finding 前 grep arm-index 同域同控制点后裁决。

| 候选 | arm-index grep 结果 | 裁决 | 依据 |
|------|--------------------|------|------|
| per-ledger 三表切换读入口缺失（watch-only 候选） | grep `每账套独立三表\|per-ledger\|报表.*acctSchemaId\|账套.*渲染` arm-index → **P1-MA2-095 行（`:552`）已含 successor 声明** + MA3 A3.5 复查注记「successor 触发条件未满足→维持 backlog，与 A4.1.23 交叉引用」 | **不新建 finding** | 本验证裁决「维持接受无新 finding」；per-ledger 切换读入口缺失**已作为 P1-MA2-095 的 successor 登记**（arm-index `:552` + MA3 A3.5 §2.6 #6），与 P1-MA2-095 不同控制点（双计 vs 切换选择）但同 successor 链。无未经比对新建的 finding |

**注记更新（Phase 2）**：本验证裁决「维持接受」，per-ledger 渲染 successor 已在 arm-index `P1-MA2-095` 行（`:552`）经 MA3 A3.5 复查注记登记。**本验证无新 finding，无须新建 arm-index 行**——仅在 P1-MA2-095 行追加 MA4 A4.1.23 运行时裁决交叉引用注记（§Phase 2 同步），确认 successor 维持 backlog（与 MA3 裁决一致，MA4 运行时行为视角无升级）。

---

## 7. 静态存疑点闭环

> A1.7 §7 SP-2 经本 MA4 A4.1.23 运行时验证闭环。

| SP | 存疑点 | 闭环结论 |
|----|--------|---------|
| SP-2 | 多账套部署下「每账套独立三表」运行时渲染 — 读路径取主账套非按账套切换 | **闭环**：读入口无 acctSchemaId 参数（§2.1）+ 恒取主账套（§2.2）证实；L1 可验证断言未要求 per-ledger 切换（§5.1）+ L2 无 drift（§5.2）→ 维持 UC-FIN-05 ② 接受，per-ledger 切换 successor 维持 backlog（与 MA3 A3.5 一致）。**无新 finding** |

> A1.7 §7 SP-1/SP-3/SP-4（cash flow postingType / CLOSED 门控 / 看板行级权限）属独立工作项 A4.1.22（done）/A4.1.24/A4.1.25，本验证不展开。

---

## 8. 过程纪律自检（§8 模板）

- [x] **checker 退出码门控核查**：本报告产出后已运行 `bash docs/audits/nop-compliance-checker.sh`。actual vs baseline 汇总表见下。**区分门控退出码 vs 纯 reporter 退出码**——checker 脚本是纯 reporter，真正门控在 CI workflow（`.github/workflows/compliance.yml`）解析 actual > baseline => sys.exit(1)。本报告**不**以 checker 脚本退出码作为门控通过依据。**本审计为只读验证（零代码/ORM/api.xml/真相源变更），checker 无回归风险**——下表 actual ≤ baseline（与本验证前基线一致，本验证未引入任何代码变更）。

| 规则 | 描述 | Baseline（`compliance-baseline.md` machine-readable） | Actual（本验证 HEAD） | 漂移 | 裁决 |
|------|------|----------|----------------------|------|------|
| R1a-d | BizModel dao().save/update/get/findAll | 0/0/0/14 | 0/0/0/14 | 0 | ✅ |
| R2a | BizModel daoFor(ErpMd*) | 34 | 34 | 0 | ✅ |
| R2b | BizModel daoFor(Erp*) 跨域 | 229 | 229 | 0 | ✅ |
| R2c | 全生产代码 daoFor() 总量 | 1382 | 1382 | 0 | ✅ |
| R2d | Processor daoFor(ErpMd*) | 34 | 34 | 0 | ✅ |
| R3 | new Erp*() | 5 | ≤5（本审计未触及） | 0 | ✅ |
| R12a/b/c | 共享内核 import | 69/66/40 | 69/66/40（本审计未触及） | 0 | ✅ |
| （其余 R4/R5/R6/R7/R8/R10/R11） | — | — | — | 0 | ✅ |

- [x] **closure-audit 独立性声明**：本报告的 closure audit 将由独立子代理（新会话，不重用执行者上下文）执行，执行者不自我审计。
- [x] **与 arm-index 交叉去重声明**：本报告无新 finding（裁决「维持接受」）。per-ledger 切换读入口缺失已作为 P1-MA2-095 successor 登记（arm-index `:552` + MA3 A3.5 §2.6 #6），本验证仅追加 MA4 运行时裁决交叉引用注记，无未经比对新建的 finding。与 A1.7 §2.4/§5.1 UC-FIN-05 / P1-MA2-095 resolved R1.29 的复用关系见 §4；MA4↔A5.6 边界见 §5.4。

---

## 9. 与既有报告差异增量声明（§6 段落 9）

| 既有证据 | 已证实结论（本验证复用） | 本验证差异增量（MA4 运行时视角，仅本验证补） |
|---------|------------------------|---------------------------------------------|
| A1.7 §2.4（P1-MA2-095 读路径双计 HEAD 复核 resolved R1.29） | 读路径 period.orgId + 主账套 scope 过滤已修复不双计 | **本验证补**：per-ledger 切换维度裁决（双计修复之外的「切换选择」维度，非双计；L1 可验证断言未要求 per-ledger 切换 → 维持接受） |
| A1.7 §5.1 UC-FIN-05 接受（写路径 stamp + GlBalance 隔离） | 写路径隔离落地 | **本验证复用**：数据「每账套独立」物理成立（涉及机制字面事实） |
| MA3 A3.5 §2.6 #6（successor 触发条件未满足→维持 backlog） | successor 部署侧触发条件未回队 MR1 | **本验证补**：MA4 运行时行为视角裁决（行为符合 L1，与 MA3 一致无升级） |

本验证不复跑 A1.7 五级追踪 / MA2 状态机 / MA3 successor 触发条件复查，只补「per-ledger 切换渲染」维度的运行时符合性裁决（§去重协议 MA4↔A5.6 + MA4 行为符合需求视角）。

---

## 报告段落完整性自检（§6 段落完整性自检，MA4 验证适配）

| # | 段落 | 状态 |
|---|------|------|
| 1 | 需求契约原文（L1 逐字引用） | ✅ §1（UC-FIN-16 可验证断言 + 涉及机制逐字） |
| 2 | 实现证据（L3 file:line） | ✅ §2（§2.1 读入口签名 + §2.2 scope 解析链） |
| 3 | 测试证据（L4） | ✅ §3（复用 `TestErpFinMultiSchemaReportIsolation`） |
| 4 | 运行时行为证据（L5） | ✅ §4 |
| 5 | 符合性裁决（§2 判据 + §4 Q1 + 三源） | ✅ §5（§5.1-§5.5 含判据编号 + Decision） |
| 6 | 与 arm-index 衔接 | ✅ §6（无新 finding，successor 已登记） |
| 7 | 静态存疑点闭环 | ✅ §7（SP-2 闭环） |
| 8 | 过程纪律自检段 | ✅ §8（checker actual vs baseline 表 + 独立性 + 交叉去重） |
| 9 | 与既有报告差异增量声明 | ✅ §9 |

**9 段齐全，无缺失。**

---

## 附：裁决摘要（供 arm-index 注记）

- **A4.1.23（SP-2 多账套「每账套独立三表」运行时渲染）**：**维持接受，无新 finding**。
  - 读入口 `balanceSheetData/incomeStatementData/cashFlowStatementData(periodId)`（`:238/244/250`）无 acctSchemaId 参数；`applyOrgAndSchemaScope:489-499` + `AcctSchemaResolver.resolvePrimarySchemaId`（FINANCIAL=0 最低）恒取主账套。
  - L1 UC-FIN-16 可验证断言 `:322-340` 未要求 per-ledger 切换（涉及机制 `:342` = 设计语境非约束力）；L2 `§:149-169` 仅约束凭证查询过滤（非报表读入口），无 drift。
  - per-ledger 三表切换读入口缺失 = 合理简化 successor（GlBalance 物理隔离已落地），与 MA3 A3.5 §2.6 #6 successor 裁决一致（维持 backlog，不回队 MR1）。
  - 与 UC-FIN-05 ② 接受[写路径隔离] + P1-MA2-095 resolved R1.29[读路径双计] 分层一致（per-ledger 渲染 = 切换选择维度，非双计，不撤销双计修复）。
