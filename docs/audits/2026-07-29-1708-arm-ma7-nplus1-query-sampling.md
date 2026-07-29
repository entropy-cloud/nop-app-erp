# A7.3 N+1 查询抽样审计报告

> 审计 ID：A7.3
> 里程碑：MA7（运维与性能层审计）
> 维度：N+1 查询抽样（S 级域列表查询路径：循环内逐条加载关联实体 + 关联加载策略 + 延迟加载集合触发）
> 域范围：S 级 4 域（finance / manufacturing / hr / assets）`module-*/erp-*-service/src/main/java` BizModel/Processor/Aggregator/Engine/Dashboard
> Owner Doc：各域 `module-*/erp-*-service/src/main/java` BizModel 查询模式
> Skill：`docs/skills/open-ended-audit-prompt.md`（开放式审计：主动搜索未知 N+1 站点 + 关联加载策略 + 延迟集合触发）
> 审计日期：2026-07-29
> 关联 plan：`docs/plans/2026-07-29-1708-1-ma7-error-code-index-nplus1-audit.md` Phase 3
> Source Audits: `docs/audits/2026-07-29-1708-arm-ma7-nplus1-query-sampling.md`
> Audit Status: closed

## Verdict: PASS（⚠ 2 项 P2 watch-only，零 P0 + 零 P1）

S 级 4 域列表查询路径**未发现无界 N+1**——不存在「列表结果循环内逐条 `getEntityById` 加载关联实体且 N 无上界」的经典 N+1 反模式。代码库采用**结构性 N+1 规避策略**：关联数据经显式独立查询加载（`loadLines(voucherId)` / `findBillLinks(...)` 范式）而非 ORM 延迟集合逐行触发，且零 `@BatchSize`/`DataLoader`/`fetch="eager"` 基础设施依赖。仅 2 项 P2 watch-only：finance 过账红冲路径的**有界 N+1**（N=1-3）+ finance dashboard/budget/年结的**载入后内存聚合**反模式（相关但非经典 N+1）。

## 1. N+1 站点矩阵（S 级 4 域逐域标注）

**审计方法**：(1) 全域 `findPage/findList/findAll` 调用站点枚举；(2) `getEntityById(` 在循环内的「N+1 吸烟枪」检测；(3) `findAllByQuery` 后循环 sum/count 的「载入后内存聚合」反模式检测；(4) `@OneToMany` 延迟集合在序列化/转换时触发风险评估；(5) `@BatchSize`/`DataLoader`/`fetch="eager"` 缓解基础设施存在性核实。

### 1.1 列表查询调用站点基线

| 域 | `findPage/findList/findAll` 站点 | 用途 |
|----|--------------------------------|------|
| finance | 2 | `AutoReconciliationEngine:329`（核销候选项批量加载）+ `DualSideConsistencyChecker:112`（双面一致性检查 AR/AP 项批量加载）——**单次批量加载，无循环内逐条加载** |
| manufacturing | 1 | `ErpMfgMaterialIssueBizModel:202`（领料关联库存台账批量加载）——**单次批量加载** |
| hr | 7 | `ErpHrShiftRotationPatternBizModel:184` / `ErpHrGapAnalysisBizModel:148,164` / `ErpHrShiftBizModel:232` / `ErpHrDevelopmentPlanBizModel:138` / `ErpHrEmployeeAssessmentBizModel:148` / `ErpHrSalarySimulationBizModel:569`——**全部为单次批量加载（findList 单调用），后续循环为内存计算非 DB 逐条加载** |
| assets | 0 | 列表查询经 `CrudBizModel` 继承默认分页，无自定义 findList |
| **合计** | **10** | — |

**关键结论**：10 个 `findList` 站点**全部为单次批量加载**——加载列表后在内存做计算/过滤/聚合，**不存在「列表每行触发一次 DB 查询」的经典 N+1**。

### 1.2 `getEntityById` 在循环内（N+1 吸烟枪检测）

| 站点 | 模式 | N 的上界 | 裁决 |
|------|------|---------|------|
| `ErpFinPostingProcessor.findPostedVoucher:852-862`（finance） | `for (link : links) { voucherDao.getEntityById(link.getVoucherId()); }` | **有界 N=1-3**（links = 同 (billCode, businessType) 的 billR 行数；正常 1 行，红冲 2 行，多账套 N 行按账套数） | P2-MA7-005（有界 N+1，低影响；见 §2） |
| `ErpFinPostingProcessor.findAllPostedVouchers:875-890`（finance） | 同上（收集全部已过账未冲销凭证） | 有界 N=1-3 | P2-MA7-005（同上） |
| `ErpFinPostingProcessor.markOriginalVoucherReversed:913-925`（finance） | 同上（补标原凭证 isReversed） | 有界 N=1-3 | P2-MA7-005（同上） |
| `AdvanceOffsetOrchestrator:184`（finance） | `for (link : links) { ... }` | 有界（advance offset 链） | P2-MA7-005（同族） |
| `BadDebtProvisionService:171`（finance） | `for (link : links) { ... }` | 有界（坏账关联凭证） | P2-MA7-005（同族） |
| `ErpAstCipProcessor:202,311`（assets） | `for (item : costItems) { ... }` | 有界（CIP 成本项，按资本化范围） | ✅ 内存计算循环（非 getEntityById DB 调用） |
| manufacturing / hr | — | — | **零 `getEntityById` 在循环内**（hr 7 站点 + mfg 1 站点全部清洁） |

### 1.3 载入后内存聚合反模式（N+1 相邻反模式）

**非经典 N+1**（无逐行 DB 查询），但是「全表/全量加载后内存 sum/count」的性能反模式——应改用 SQL `SUM()/COUNT()` 聚合。

| 站点 | 模式 | 数据量 | 裁决 |
|------|------|--------|------|
| `ErpFinDashboardBizModel:221-227`（finance） | `findAllByQuery` 加载全部开放 AR/AP 项 → `for (it : items) sum.add(openAmountFunctional)` | 单组织种子小；生产规模下开放 AR/AP 项可达万级 | P2-MA7-006（应改 SQL `SUM(openAmountFunctional)`） |
| `ErpFinDashboardBizModel:206-212`（finance） | `findAllByQuery` 加载全部资金账户 → 内存求和 | 账户数小 | ✅ 可接受（配置级数据量） |
| `ErpFinBudgetControlBiz:132`（finance） | `lineDao.findAllByQuery(lq)` 加载凭证行 → 内存聚合预算消耗 | 凭证行高-volume | P2-MA7-006（与 P2-MA4-003 同族） |
| `AnnualCloseService:156`（finance） | `glDao.findAllByQuery(clearQ)` 加载 GL 余额 → 内存聚合 | GL 余额按科目×期间，中量 | P2-MA7-006（年结低频，可接受） |
| `ErpFinReportBizModel:325`（finance） | AR/AP 项内存聚合 | 报表低频 | P2-MA7-006（与 P2-MA4-003 同族） |

### 1.4 延迟加载集合触发 N+1 风险

**ORM `@OneToMany`/`to-many` 声明数**：finance 8 / mfg 14 / hr 7 / assets 5 = 34 处 `to-many` 关系。

**风险评估**：S 级 4 域列表查询路径**不序列化/不遍历** `@OneToMany` 延迟集合。子集合（如 `voucher.lines` / `order.lines` / `workOrder.jobCards`）经**显式独立查询**加载（`loadLines(voucherId)` / `findBillLinks(...)` / `findItems(...)` 范式），而非经 ORM 关系 getter 在父列表序列化时逐行触发。Nop 平台 GraphQL 响应按 `XMeta` 字段投影，列表查询默认不展开 `to-many` 字段（需显式 `fields` 请求）。故延迟集合触发 N+1 风险**低**。

**潜在风险点**：若未来 GraphQL 查询经 `fields` 显式请求列表的 `to-many` 字段（如 `{items {lines {materialId}}}`）且无 DataLoader/BatchSize 缓解，会触发 N+1。当前零 DataLoader 基础设施（见 §1.5）——属未来 successor。

### 1.5 N+1 缓解基础设施存在性

| 缓解机制 | 全域使用 | 裁决 |
|---------|---------|------|
| `@BatchSize(size=N)` | **0**（S 级 4 域零使用） | 不依赖 |
| `fetch="eager"` / `FetchType.EAGER` | **0** | 不依赖 |
| `DataLoader` / `@Fetch` | **0** | 不依赖 |
| GridBean 批量装配 | 有限（`ErpFinReportBizModel` 内存聚合，非 batch-load 关联） | — |

**裁决**：代码库**不依赖 ORM 级 N+1 缓解基础设施**，而是通过**结构性规避**——关联数据经显式查询加载（应用层控制加载策略）而非 ORM 延迟加载自动触发。这是 Nop 平台 + 本项目的**有意架构选择**：BizModel 显式编排查询，避免 ORM 魔法带来的隐式 N+1。当前 S 级 4 域列表查询路径因此**结构性免疫**经典 N+1。代价是后续若引入 GraphQL 嵌套字段展开（`{items {relations {...}}}`），需补 DataLoader 层。

## 2. N+1 站点详查

### P2-MA7-005 finance 过账红冲路径有界 N+1（5 站点同族）

**站点**：
- `ErpFinPostingProcessor.findPostedVoucher:852-862`
- `ErpFinPostingProcessor.findAllPostedVouchers:875-890`
- `ErpFinPostingProcessor.markOriginalVoucherReversed:913-925`
- `AdvanceOffsetOrchestrator:184`
- `BadDebtProvisionService:171`

**模式**：
```java
List<ErpFinVoucherBillR> links = findBillLinks(billHeadCode, businessType, context);
for (ErpFinVoucherBillR link : links) {
    ErpFinVoucher voucher = voucherDao.getEntityById(link.getVoucherId());  // 逐条加载
    ...
}
```

**N 的上界**：`links` = 同 `(billCode, businessType)` 的 billR 行数。
- 正常过账：1 行（单凭证）。
- 红冲：2 行（原凭证 + 红字凭证）。
- 多账套：N 行（每账套 1-2 行），N = 账套数（默认单账套 N=1）。

**影响评估**：**有界且小**（N 典型 1-3）。每次过账红冲路径产生 1-3 次额外 `getEntityById` 查询。非无界 N+1（不会随业务规模增长到百/千次）。单组织种子 + 单账套下 N=1（无可见影响）；多账套大规模下 N 略增但仍受账套数硬约束。

**可修复性**：批量加载替代可行——收集 `links.stream().map(VoucherBillR::getVoucherId).collect(toList())` 后单次 `findAllByIds(voucherIds)` + 内存过滤。改造工作量小（5 站点同质），但收益有限（N=1-3 时节省 0-2 次查询）。

**裁决**：**P2 watch-only**（有界 N+1，低影响）。归 MR3 性能优化批次，与 P1-MA7-001（billR 索引）协同修复时可顺手批量加载化。

### P2-MA7-006 finance dashboard/budget/年结载入后内存聚合（5 站点）

**模式**：`findAllByQuery` 全量加载实体列表 → Java 循环 `sum/count` 聚合，而非 SQL `SUM()/COUNT()` 聚合。

**站点**：`ErpFinDashboardBizModel:221-227`（AR/AP 开放项求和）+ `ErpFinBudgetControlBiz:132`（凭证行预算消耗聚合）+ `AnnualCloseService:156`（GL 余额聚合）+ `ErpFinReportBizModel:325`（AR/AP 报表聚合）+ `ErpFinBudgetScenarioProcessor:386`（凭证行聚合）。

**影响**：生产规模下（万级凭证行 / 千级 AR-AP 开放项），全量载入内存消耗 heap + 网络传输冗余。应改 SQL `SELECT SUM(open_amount_functional) FROM ... WHERE ...` 单次聚合。单组织种子下数据量小无明显症状。

**与 P2-MA4-003 的关系**：P2-MA4-003（A4.1b finance 代码质量）已登记「FX 重估全表扫描 erp_fin_voucher_line + 预算/坏账余额全局 voucherId 内存载入 + closePeriod no-op + precheck load-then-filter + 重复 subject 解析」5 项可维护性/性能热点合并。**P2-MA7-006 是其在 N+1 抽样审计维度的投影**（dashboard + budget + 年结载入后聚合），与 P2-MA4-003 **同族不重复登记**——归并 MR2/MR3 一并裁决（SQL 聚合化改造）。

**裁决**：**P2 watch-only**（载入后聚合反模式，非经典 N+1 但性能相邻）。归并 P2-MA4-003 修复范围。

## 3. Finding 汇总

### P0（即时通道）

无。

### P1（目标 MR3）

无。S 级 4 域列表查询路径零无界 N+1。

### P2（watch-only）

| Finding ID | 描述 | 处置 |
|-----------|------|------|
| `P2-MA7-005` | **finance 过账红冲路径有界 N+1（5 站点同族）**：`ErpFinPostingProcessor.findPostedVoucher:852/findAllPostedVouchers:875/markOriginalVoucherReversed:913` + `AdvanceOffsetOrchestrator:184` + `BadDebtProvisionService:171` 经 `for (link : links) voucherDao.getEntityById(link.getVoucherId())` 逐条加载凭证。N 有界（1-3，按红冲/多账套计数），非无界 N+1。批量加载替代可行（`findAllByIds`）。 | watch-only，MR3 性能优化——与 P1-MA7-001（billR 索引）协同修复时顺手批量加载化（5 站点收集 voucherIds → 单次 `findAllByIds`） |
| `P2-MA7-006` | **finance dashboard/budget/年结载入后内存聚合（5 站点）**：`ErpFinDashboardBizModel:221-227`（AR/AP 求和）+ `ErpFinBudgetControlBiz:132`（凭证行聚合）+ `AnnualCloseService:156`（GL 余额聚合）+ `ErpFinReportBizModel:325`（报表聚合）+ `ErpFinBudgetScenarioProcessor:386`。全量载入内存后 Java 循环 sum/count，应改 SQL `SUM()/COUNT()` 聚合。**与 P2-MA4-003 同族**（MA4 A4.1b 已登记 5 项可维护性/性能热点合并含 FX 全表扫描/预算 voucherId 内存载入）。 | watch-only，归并 P2-MA4-003 MR2/MR3 修复范围——SQL 聚合化改造 |

## 4. 与 MA1-MA6 已登记 finding 交叉去重

| 本审计观察 | 已登记 finding | 关系 |
|-----------|---------------|------|
| finance 载入后内存聚合（5 站点） | P2-MA4-003（A4.1b 可维护性/性能热点合并 5 项含 FX 全表扫描/预算 voucherId 载入/closePeriod no-op/precheck load-then-filter/重复 subject 解析） | **同族投影**：P2-MA4-003 是 MA4 代码质量维度；P2-MA7-006 是 MA7 N+1 抽样维度。同一批站点不同审计轴投影。**归并不重复登记**——P2-MA7-006 标注「归并 P2-MA4-003 修复范围」 |
| finance 过账红冲有界 N+1 | P0-MA2-018（billR UK deferred）+ P1-MA7-001（billR 索引） | **互补**：P0-MA2-018/P1-MA7-001 是索引维度（加速 findBillLinks 查询）；P2-MA7-005 是 N+1 维度（links 循环内 getEntityById）。不同维度，修复路径独立但协同 |
| hr 域零 N+1 | A4.4（hr 代码质量 S 级） | A4.4 已确认 hr 计算引擎同域只读 + 跨域 Facade 零 daoFor 写；A7.3 补充确认 hr 列表查询路径零 N+1 |
| 零 @BatchSize/DataLoader 基础设施 | A3.8（可定制性验证）+ A1.11-A1.13（平台合规） | A3.8 已登记「BizLoader owner doc 声明 3 业务示例全部以其他机制实现」；A7.3 确认 N+1 缓解基础设施同型零落地（结构性规避替代） |

## 5. Exit Criteria 核实

- [x] S 级 4 域 N+1 站点矩阵产出（§1.1 列表查询基线 10 站点全部单次批量 + §1.2 getEntityById 循环检测 5 有界站点 [P2-MA7-005] + §1.3 载入后聚合 5 站点 [P2-MA7-006] + §1.4 延迟集合风险低 + §1.5 零缓解基础设施结构性规避）
- [x] A7.3 P0/P1/P2 已登记 arm-index.md（零 P0 + 零 P1 + 2 P2）且去重（§4 与 P2-MA4-003 同族归并 + P0-MA2-018/P1-MA7-001 互补）
