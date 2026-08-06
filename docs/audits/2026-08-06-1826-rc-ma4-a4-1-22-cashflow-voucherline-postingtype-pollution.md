# RC MA4 A4.1.22 — 现金流量表读 VoucherLine 不过滤 postingType 的 BUDGET/COMMITMENT 影子凭证污染运行时确认

> Audit Status: closed
> 里程碑：MA4（运行时行为验证）
> 工作项：A4.1.22（MA4 运行时行为验证 — A1.7 §7 存疑点 SP-1：UC-FIN-16 现金流量表 `buildCashFlowDataset` 读 `ErpFinVoucherLine` 不过滤 voucher.`postingType`，BUDGET/COMMITMENT 影子凭证若含现金科目[1001/1002/1012/1031]行是否污染现金流量表）
> 输入：`docs/audits/2026-08-02-2115-rc-ma1-a1-7-finance-f7-reports-dashboards-multischema.md` §7 SP-1 + §2.2 现金流量数据集查询链 + §5.2 caveat ③ 收口（BS/IS 安全）；交叉项 A1.2 caveat ③
> 验证 plan：`docs/plans/2026-08-06-1826-1-rc-ma4-a4-1-22-cashflow-voucherline-postingtype-pollution.md`
> 方法论契约：`docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 分级判据[含 P2① 次要验收标准边界] + §4 Q1 真相源层级 + §7 衔接 + §8 过程纪律自检 + §9 真相源冻结 + §去重协议）
> 范式对齐：A4.1.21（`docs/audits/2026-08-06-1708-rc-ma4-a4-1-21-year-end-reverse-close-block-boundary.md`，done — period-close 运行时行为评估同型工作项）+ A4.1.5（`docs/audits/2026-08-06-0847-rc-ma4-a4-1-5-commitment-trial-balance-exposure.md`，done — 承付试算平衡暴露同 caveat ③ 交叉项）
> 审计性质：**只读 cash flow postingType 污染面运行时确认**（读 `buildCashFlowDataset` + `loadPostedVoucherLines` + `isCashSubjectCode` + BUDGET/COMMITMENT 投递 Provider/Generator 的科目分布 + seed 凭证普查 + `app-erp-finance.orm.xml` 范式注记；**不改代码/ORM/api.xml/真相源**）
> 审计日期：2026-08-06
> 审计者：主代理（独立结束审计由独立子代理执行，见 plan §Closure Gates）

---

## 0. TL;DR（核验结论）

| 项 | 结果 |
|---|------|
| 存疑点 | A1.7 §7 SP-1：`buildCashFlowDataset` 读 `ErpFinVoucherLine` 不过滤 voucher.`postingType`，BUDGET/COMMITMENT 影子凭证若含现金科目（1001/1002/1012/1031）行是否污染现金流量表 |
| 现金流量读取链（写时实测） | `ErpFinReportBizModel.buildCashFlowDataset:299-323` → `loadPostedVoucherLines(periodId):302` → 逐行 `if (!isCashSubjectCode(l.getSubjectCode())) continue;:305` + `r.put("section","OPERATING"):314`（硬编码 OPERATING，关联 P1-RC-007）。`loadPostedVoucherLines:424-439`：`eq("docStatus",POSTED):427` + `eq("periodId",periodId):429` + `applyOrgAndSchemaScope(vq,periodId):430`（orgId + acctSchemaId scope，R1.29 fix）→ 取 voucherIds → `in("voucherId",voucherIds):437` 加载 VoucherLine。**全程无 postingType 过滤**——BUDGET/COMMITMENT postingType 的已过账凭证的行同样被加载 |
| 现金科目判定（写时实测） | `isCashSubjectCode:549-553`：`code.startsWith("1001")\|\|"1002"\|\|"1012"\|\|"1031"`（库存现金/银行存款/其他货币资金/存出保证金） |
| postingType 字段归属（写时实测） | `postingType` 列在 **ErpFinVoucher 头**（`app-erp-finance.orm.xml:418`，凭证级）+ `ErpFinPostingException`（`:1620`）；**`ErpFinVoucherLine` 实体无 postingType 字段**（grep `ErpFinVoucherLine.java` 零命中）。过滤须在 voucher 头侧 vq 查询（`loadPostedVoucherLines:426-431`），非行侧 lq 查询（`:436-438`） |
| BUDGET 影子凭证科目分布（写时实测） | `BudgetVoucherGenerator`：科目来自 `ErpFinBudgetLine.subjectId`（`toFact:182-204` + `loadSubject:219-221`）。**无科目大类守卫**——代码不强制非现金科目。seed 实测：全 `erp_fin_budget_line.csv` 仅 6601（销售费用/expense）+ 6001（主营业务收入/income），**零现金科目**；`TestErpFinBudgetIsolation` output BUDGET 凭证（voucher ID=12）行 = 6601+6001 |
| COMMITMENT 影子凭证科目分布（写时实测） | `CommitmentVoucherGenerator`：科目由调用方传入（`ErpFinBudgetCommitmentBizModel.commit` → `loadSubject(subjectId):64`），commit subject 来自外部（purchase/sales 域）；config `erp-fin.budget-commitment-subject-code = "1408"`（AP/预付类型，**非现金科目**）。**无科目大类守卫**。且 COMMITMENT **config-gated 默认 false**（`isCommitmentEnabled` 默认 `Boolean.FALSE`，`ErpFinBudgetCommitmentBizModel.java:117-119`）→ 默认部署零承付凭证 |
| seed/生产凭证普查 | **零污染凭证**——无任何 postingType∈{BUDGET,COMMITMENT} 且 VoucherLine.subjectCode 命中 1001/1002/1012/1031 前缀的凭证（seed/fixture 全普查）。`TestErpFinBudgetIsolation` 中 1001 行全部属 NORMAL（ACTUAL）凭证（voucher ID=6/9）。生产/demo seed 无 BUDGET/COMMITMENT 凭证（grep `erp_fin_voucher.csv` 排除 `_cases/test` 零命中） |
| **caveat ③ cash flow 维度运行时裁决（本存疑点核心）** | **维持 caveat ③ 接受[cash flow 低风险成立] + 登记 P2-RC-085 watch-only[代码无显式守卫/潜在污染风险]**——决策树分支①命中：BUDGET/COMMITMENT 影子凭证实操**不触现金科目**（①createFacts/budget line 科目分布无现金前缀[6601/6001/1408] + ②seed/config普查零污染凭证 + ③COMMITMENT config 默认关闭）→ **存疑点核心问题"现金流量表是否被影子凭证污染"答案 = 否（当前运行时）**。但代码**无显式守卫**（createFacts/budget line 不过滤科目大类 + loadPostedVoucherLines 不过滤 postingType）→ 未来定制若引入现金科目影子凭证，cash flow 将被污染 → 登记 P2 watch-only（潜在风险，非活跃污染） |
| 新 finding | **1**（P2-RC-085，cash flow 读路径 postingType 过滤缺失 watch-only——与既有 finding 不同控制点：见 §7.4 去重裁决） |
| P0 即时通道 | 不触发（未出 P0/P1） |

**核心裁决**：存疑点 A1.7 §7 SP-1 的 cash flow postingType 污染面运行时确认结论 = **存疑点核心问题"现金流量表是否被 BUDGET/COMMITMENT 影子凭证污染"答案 = 否（当前运行时）**，**维持 caveat ③ 接受[cash flow 低风险] + 登记 P2-RC-085 watch-only[潜在污染风险]**。判据三层（§2 P2① 决策树分支①）：(1) **createFacts/Generator 科目分布无现金前缀**——本报告 §3 主证据：`BudgetVoucherGenerator`（budget line 全 6601/6001）+ `CommitmentVoucherGenerator`（commit subject config=1408）投递科目**均不含** 1001/1002/1012/1031 现金科目前缀；(2) **seed/config普查零污染凭证**——本报告 §5 主证据：`TestErpFinBudgetIsolation` output 中 BUDGET 凭证（ID=12）行=6601+6001，1001 行全属 NORMAL 凭证（ID=6/9）；生产/demo seed 无 BUDGET/COMMITMENT 凭证；COMMITMENT config 默认关闭；(3) **代码无显式守卫但运行时无污染**——createFacts/budget line 不过滤科目大类 + `loadPostedVoucherLines` 不过滤 postingType，但实操不触现金科目 → 当前无污染，未来定制若引入现金科目影子凭证将污染。存疑点核心问题经此三层推理**答案 = 否（当前）**（BUDGET/COMMITMENT 影子凭证不触现金科目，cash flow 不被污染）。代码无显式守卫的潜在污染风险 → 登记 P2-RC-085 watch-only（潜在风险，归 MR1 successor；修复 = `loadPostedVoucherLines` vq 查询加 `postingType=ACTUAL`[或 notIn BUDGET/COMMITMENT] 过滤属 BizModel 读侧过滤，预授权自动执行，不触 §5 ask-first）。A1.7 §5.2 caveat ③[BS/IS 安全]**不撤销**（cash flow 维度是 caveat ③ 之外的读路径差异，BS/IS 安全结论[BUDGET/COMMITMENT 不入 GlBalance]分层独立）。**本验证不实施修复**（§保护区域 + plan Non-Goals）。

---

## 1. 需求契约原文（§1 L1，逐字引用）

| 层 | 文件:行 | 原文/证据 |
|---|---|---|
| L1（权威） | `docs/design/finance/use-cases.md:318` | UC-FIN-16 财务三大报表。验收标准⑧「现金流量表按经营/投资/筹资分类」+ caveat ③ 交叉（BUDGET/COMMITMENT 过滤）。L1 未显式要求 cash flow 排除影子凭证，但「现金流量表反映真实现金收支」语义隐含 postingType=ACTUAL（实际凭证）口径 |
| L1 | `use-cases.md:334-336` | ⑧「按科目现金流分类(经营/投资/筹资)调整 / 间接法: 净利润 + 非现金项目 + 营运资金变动」（关联 P1-RC-007 现金流分类缺失，本验证不裁决 P1-RC-007） |
| L2 | `multiple-accounting-schemas.md §账套查询与报表:149` + `period-close.md` + `dashboards.md` | 报表设计参考 |
| L2 | `app-erp-finance.orm.xml:1740-1742` | **影子凭证范式注记**（逐字）：「范式：预算作为 postingType=BUDGET 的『影子凭证』与实际凭证并行入账，复用凭证引擎。预算余额/实际余额/可用余额统一从 ErpFinVoucherLine 按关联凭证 postingType 聚合（派生，不落库）」——证实影子凭证经同一 VoucherLine 表与 ACTUAL 混存 |

**真相源层级（§4 Q1）**：L1 权威，L2 冲突以 L1 为准。「现金流量表反映真实现金收支」语义 → 应仅计 ACTUAL 凭证现金移动；BUDGET/COMMITMENT 影子凭证不是真实现金收支（预算/承付追踪，非实际资金流动）→ 若计入则偏离 L1 语义。

---

## 2. 现金流量读取链核验（§Phase 1 Item 1）

### 2.1 buildCashFlowDataset（写时实测）

`ErpFinReportBizModel.java:299-323`：

```java
List<Map<String, Object>> buildCashFlowDataset(Long periodId) {
    return ormTemplate.runInSession(session -> {
        List<Map<String, Object>> rows = new ArrayList<>();
        List<ErpFinVoucherLine> lines = loadPostedVoucherLines(periodId);   // :302
        for (ErpFinVoucherLine l : lines) {
            if (l.getSubjectCode() == null) continue;
            if (!isCashSubjectCode(l.getSubjectCode())) continue;            // :305 仅按科目代码前缀过滤
            BigDecimal debit = nz(l.getDebitAmount());
            BigDecimal credit = nz(l.getCreditAmount());
            BigDecimal net = debit.subtract(credit);
            if (net.signum() == 0) continue;
            ...
            r.put("section", "OPERATING");                                   // :314 硬编码 OPERATING（P1-RC-007）
            ...
            rows.add(r);
        }
        return rows;
    });
}
```

**关键缺口**：`buildCashFlowDataset` 仅按 `isCashSubjectCode`（科目代码前缀）过滤，**不区分凭证 postingType**。任何 postingType（ACTUAL/BUDGET/COMMITMENT）的已过账凭证的现金科目行都会被计入。

### 2.2 loadPostedVoucherLines（写时实测）

`ErpFinReportBizModel.java:424-439`：

```java
private List<ErpFinVoucherLine> loadPostedVoucherLines(Long periodId) {
    IEntityDao<ErpFinVoucher> vDao = daoProvider.daoFor(ErpFinVoucher.class);
    QueryBean vq = new QueryBean();
    vq.addFilter(eq("docStatus", ErpFinConstants.VOUCHER_STATUS_POSTED));   // :427 仅 POSTED 状态
    if (periodId != null) {
        vq.addFilter(eq("periodId", periodId));                             // :429 期间
        applyOrgAndSchemaScope(vq, periodId);                               // :430 orgId + 主账套 scope（R1.29 fix）
    }
    List<ErpFinVoucher> vouchers = vDao.findAllByQuery(vq);
    ...
    QueryBean lq = new QueryBean();
    lq.addFilter(in("voucherId", voucherIds));                              // :437 按 voucherId 加载行
    return daoProvider.daoFor(ErpFinVoucherLine.class).findAllByQuery(lq);
}
```

**关键缺口**：vq 查询过滤条件 = `docStatus=POSTED` + `periodId` + `applyOrgAndSchemaScope`（orgId + acctSchemaId），**无 postingType 过滤**。BUDGET/COMMITMENT 影子凭证的 `docStatus` 同样为 POSTED（`BudgetVoucherGenerator.java:137` + `CommitmentVoucherGenerator.java:149` 均设 `VOUCHER_STATUS_POSTED`）→ 满足 vq 筛选 → 其 VoucherLine 行被加载。

### 2.3 isCashSubjectCode（写时实测）

`ErpFinReportBizModel.java:549-553`：

```java
private static boolean isCashSubjectCode(String code) {
    if (code == null) return false;
    return code.startsWith("1001") || code.startsWith("1002")
            || code.startsWith("1012") || code.startsWith("1031");
}
```

现金科目前缀：1001（库存现金）/ 1002（银行存款）/ 1012（其他货币资金）/ 1031（存出保证金）。**若 BUDGET/COMMITMENT 凭证行命中这些前缀 → 计入现金流量表。**

### 2.4 读取链裁决

证实 cash flow 读路径**不区分 ACTUAL/BUDGET/COMMITMENT 凭证**。污染成立的充要条件 = 存在 postingType∈{BUDGET,COMMITMENT} 且 VoucherLine.subjectCode 命中现金前缀的凭证。下文 §3/§5 核验此条件是否满足。

---

## 3. BUDGET/COMMITMENT 影子凭证科目分布核验（§Phase 1 Item 2 — 本存疑点核心）

### 3.1 BUDGET 影子凭证（BudgetVoucherGenerator）

`BudgetVoucherGenerator.java`：

- 科目来源：`ErpFinBudgetLine.subjectId`（`toFact:182-204` → `loadSubject:219-221` `daoProvider.daoFor(ErpMdSubject.class).getEntityById(id)`）。
- `writeBudgetVoucher:97-180`：每行 `line.setSubjectCode(f.subjectCode):148`（直接取 budget line 关联科目 code）。
- **无科目大类守卫**：`resolveDcDirection:207-210` 仅按 `subject.getDirection()`（DEBIT/CREDIT）定向，**不校验 subject.subjectClass**。代码不强制 BUDGET 凭证只能用 expense/income/cost 类科目。
- postingType：`voucher.setPostingType(ErpFinConstants.POSTING_TYPE_BUDGET):126`；docStatus=`POSTED:137`。

**seed 实测**（全 `erp_fin_budget_line.csv` 普查）：所有 budget line seed 仅使用 `6601`（销售费用/expense）+ `6001`（主营业务收入/income）。**零现金科目（1001/1002/1012/1031）**。

### 3.2 COMMITMENT 影子凭证（CommitmentVoucherGenerator）

`CommitmentVoucherGenerator.java` + `ErpFinBudgetCommitmentBizModel.java`：

- 科目来源：调用方传入的 `ErpMdSubject subject`（`ErpFinBudgetCommitmentBizModel.commit:55-78` → `loadSubject(subjectId):64` → `generateCommitment(...,subject,...):73`）。commit 的 subjectId 由 purchase/sales 域调用 SPI 时传入。
- config 路径（`resolveCommitmentSubject:126-137`）：按 `erp-fin.budget-commitment-subject-code` 配置反查科目 code。
- `writeCommitmentVoucher:119-182`：`line.setSubjectCode(subject.getCode()):158`。**无科目大类守卫**。
- config 实测（`budget-a2-test.yaml`）：`budget-commitment-subject-code: "1408"`（AP/预付账款类型，**非现金科目**）。
- postingType：`voucher.setPostingType(ErpFinConstants.POSTING_TYPE_COMMITMENT):135`；docStatus=`POSTED:149`。
- **config-gated 默认关闭**：`isCommitmentEnabled():117-119` 默认 `Boolean.FALSE`（`CONFIG_BUDGET_COMMITMENT_ENABLED`）→ **默认部署零承付凭证**。

`CommitmentAcctDocProvider.java`：`getSupportedBusinessTypes():33-35` 返回空集 → 承付 flow 不经 Provider 路由，`createFacts():42-44` 返回空列表（承付凭证直接由 Generator 写入）。故 Provider 侧无科目分布。

### 3.3 影子凭证范式注记（L2）

`app-erp-finance.orm.xml:1740-1742`（写时实测）：证实影子凭证确实过 POSTED 状态且经同一 VoucherLine 表，与 ACTUAL 凭证行**同表混存**——这正是污染的理论通道。

### 3.4 科目分布裁决

静态推断「BUDGET/COMMITMENT 追踪费用/AP 科目，不触现金科目」在运行时**成立**：
- BUDGET：科目来自 budget line（费用/收入类，seed 无现金科目）
- COMMITMENT：科目来自 config（AP/预付类，非现金）+ config 默认关闭

但**代码无显式守卫**：createFacts/budget line 不过滤科目大类，未来定制若将现金科目绑入 budget line 或 commit subject，BUDGET/COMMITMENT 凭证将含现金行 → cash flow 污染。此潜在风险归 §7 P2-RC-085。

---

## 4. postingType 字段归属确认（§Phase 1 Item 3 — 修复控制点）

| 实体 | 字段 | 文件:行 | 证据 |
|---|---|---|---|
| **ErpFinVoucher**（头） | `postingType` | `app-erp-finance.orm.xml:418` | `<column name="postingType" ... ext:dict="erp-fin/posting-type" .../>`（propId=4，凭证级） |
| ErpFinPostingException | `postingType` | `app-erp-finance.orm.xml:1620` | propId=5（过账异常实体，非凭证行） |
| **ErpFinVoucherLine**（行） | — | `ErpFinVoucherLine.java` | **grep `postingType` 零命中**——VoucherLine 实体无 postingType 字段 |

**修复控制点裁决**：过滤须在 **voucher 头侧 vq 查询**（`loadPostedVoucherLines:426-431`），非行侧 lq 查询（`:436-438`）。修复方向（若 MR1 落地）= `loadPostedVoucherLines` vq 增 `vq.addFilter(eq("postingType", ACTUAL))` 或 `notIn("postingType", [BUDGET,COMMITMENT])`（考虑 NORMAL/NULL 两种 ACTUAL 表示，对齐 `ErpFinBudgetControlBiz` 三通道范式）。

---

## 5. seed/生产凭证普查（§Phase 1 Item 4）

### 5.1 TestErpFinBudgetIsolation 普查（关键证据）

`_cases/.../TestErpFinBudgetIsolation/testBudgetVoucherExcludedFromProfitLossClosing/output/tables/`：

**erp_fin_voucher.csv**（POSTING_TYPE 列）：

| ID | POSTING_TYPE | TOTAL_DEBIT | TOTAL_CREDIT |
|---|---|---|---|
| 6 | **NORMAL** | 400 | 400 |
| 9 | **NORMAL** | 160 | 160 |
| 12 | **BUDGET** | 1000 | 1000 |
| 16 | NORMAL | 280 | 280 |

**erp_fin_voucher_line.csv**（SUBJECT_CODE 列）：

| LINE.ID | VOUCHER_ID | SUBJECT_CODE | SUBJECT_NAME | DC | DEBIT | CREDIT |
|---|---|---|---|---|---|---|
| 7 | 6 | **1001** | 库存现金 | DEBIT | 200 | 0 |
| 8 | 6 | 6001 | 主营业务收入 | CREDIT | 0 | 200 |
| 10 | 9 | 6601 | 销售费用 | DEBIT | 80 | 0 |
| 11 | 9 | **1001** | 库存现金 | CREDIT | 0 | 80 |
| 13 | **12** | 6601 | 销售费用 | DEBIT | 500 | 0 |
| 14 | **12** | 6001 | 主营业务收入 | CREDIT | 0 | 500 |

**裁决**：BUDGET 凭证（ID=12）行 = 6601（销售费用）+ 6001（主营业务收入），**不含现金科目**。所有 1001（库存现金）行**全部属 NORMAL（ACTUAL）凭证**（ID=6/9）。→ `buildCashFlowDataset` 读此数据集时，BUDGET 凭证 ID=12 的行经 `isCashSubjectCode(6601)`/`isCashSubjectCode(6001)` 返回 false 被 `continue` 跳过 → **不污染现金流量表**。

### 5.2 budget line seed 全普查

全 `erp_fin_budget_line.csv`（TestErpFinBudgetIsolation/EndToEnd/CarryForward/RollForward 等）：SUBJECT_CODE 列仅 `6601` + `6001`，**零现金科目**。

### 5.3 COMMITMENT 凭证普查

commit subject = config `1408`（`budget-a2-test.yaml`），非现金科目。COMMITMENT config 默认关闭 → 默认部署零承付凭证。

### 5.4 生产/demo seed 普查

grep `BUDGET|COMMITMENT` 全 `erp_fin_voucher.csv`（排除 `_cases/test`）：**零命中**——生产/demo seed 无 BUDGET/COMMITMENT 凭证。

### 5.5 普查裁决

**零污染凭证**——无任何 postingType∈{BUDGET,COMMITMENT} 且 VoucherLine.subjectCode 命中 1001/1002/1012/1031 前缀的凭证（seed/fixture 全普查）。静态推断「实操不触现金科目」在运行时**成立**。

---

## 6. MA4↔A5.6 边界声明（§Phase 1 Item 5）

本验证审「行为是否符合需求」（cash flow 是否被影子凭证污染——需求契约视角，§4 Q1 真相源层级 L1 语义），与 A5.6 审「E2E 断言强度」（测试质量全量评级）边界按此执行。**不重做 A5.6 E2E 断言强度审计**。

cash flow 的 E2E/单测断言强度（`TestErpFinReportRendering#testCashFlowDataset:162-173` 仅断言现金流入额 80，无分类/postingType 断言）归 A5.6 评级范围；本验证仅记录其与 P1-RC-007（分类缺失）的关联，不重复评级。

---

## 7. caveat ③ cash flow 维度运行时裁决（§Phase 1 Item 6 — Decision）

### 7.1 三源对照

| 维度 | L1 | L2 | L3（实测） |
|---|---|---|---|
| cash flow 应否排除影子凭证 | 「反映真实现金收支」语义 → 隐含 ACTUAL 口径 | `orm.xml:1740-1742` 影子凭证范式（BUDGET/COMMITMENT 经同一 VoucherLine 混存） | `loadPostedVoucherLines:424-439` 不过滤 postingType |
| BUDGET/COMMITMENT 是否触现金科目 | 未规定（L1 不约束科目分布） | 范式注记未限定科目大类 | `BudgetVoucherGenerator`（budget line 6601/6001）+ `CommitmentVoucherGenerator`（config 1408）+ 无守卫 |

### 7.2 决策树分支裁决

§2 P2① 决策树：

- **分支①命中**（实操不触现金科目）：
  - (a) createFacts/Generator 科目分布**无现金前缀**（§3：6601/6001/1408）
  - (b) seed/config普查**零污染凭证**（§5：BUDGET 凭证 ID=12 无现金科目；1001 行全属 NORMAL；COMMITMENT config 默认关闭；生产 seed 无 BUDGET/COMMITMENT）
  - (c) 代码**无显式守卫**（createFacts/budget line 不过滤科目大类 + loadPostedVoucherLines 不过滤 postingType）→ 当前无污染，未来定制可能引入

→ **维持 caveat ③ 接受[cash flow 低风险成立] + 登记 P2-RC-085 watch-only[潜在污染风险]**。

### 7.3 与 caveat ③[BS/IS 安全]分层一致性

A1.7 §5.2 caveat ③ 收口结论：
- **BS/IS 安全**：BUDGET/COMMITMENT 不入 GlBalance（`orm.xml:1740-1742` 注记），BS/IS 读 GlBalance 不过滤 postingType 亦不受影响。
- **cash flow 低风险**（本验证收口）：读 VoucherLine 不过滤 postingType，但实操不触现金科目 → 当前无污染。

两者分层独立、一致：cash flow 维度是 caveat ③ 之外的**读路径差异**（BS/IS 读 GlBalance[余额级] vs cash flow 读 VoucherLine[交易级]），**不撤销 BS/IS 安全结论**。本验证补 cash flow 维度的运行时裁决 = 维持低风险接受 + P2 watch-only。

### 7.4 去重裁决（§去重协议）

grep arm-index 同域（finance）同控制点（cash flow + postingType 过滤）：

| 既有 finding | 控制点 | 与本存疑点关系 |
|---|---|---|
| **P1-RC-091**（A4.1.5） | 试算平衡快照 + 4 GL 重分类/重估服务 exclude BUDGET only 未排 COMMITMENT（**聚合层平衡恒等式**） | **不同控制点**：试算平衡聚合层[Σdebit==Σcredit] vs cash flow 读路径过滤[现金科目行计入]。P1-RC-091 自承「CF 不过滤归 A4.1.22 同根因家族不同控制点」。 |
| **P1-RC-007**（A1.7） | 现金流量经营/投资/筹资三分类 + 间接法缺失（**分类维度**，`:314` 硬编码 OPERATING） | **不同控制点**：现金流分类维度 vs postingType 过滤维度。P1-RC-007 自承「与 A1.2 caveat ③ 不同控制点[postingType 过滤 vs 现金流分类]」。 |
| **caveat ③**（A1.2/A1.7） | 承付凭证结构（header 借贷不经 assertBalanced） | 凭证结构维持接受；本 finding 针对读路径过滤缺口非凭证结构。 |

→ **新建 P2-RC-085**（cash flow 读路径 postingType 过滤缺失 watch-only）——不同控制点（读路径过滤），无既有 RC finding 覆盖此控制点。

### 7.5 裁决结论

**存疑点核心问题「现金流量表是否被 BUDGET/COMMITMENT 影子凭证污染」答案 = 否（当前运行时）**。维持 caveat ③ 接受[cash flow 低风险]，登记 **P2-RC-085 watch-only**（代码无显式守卫，未来定制可能引入现金科目影子凭证）。与 caveat ③[BS/IS 安全] + P1-RC-091[试算平衡] + P1-RC-007[分类] 分层一致。

---

## 8. §8 过程纪律自检

### 8.1 nop-compliance-checker 运行记录

| 项 | 结果 |
|---|---|
| 命令 | `bash docs/audits/nop-compliance-checker.sh` |
| 性质 | 纯 reporter（扫描全仓合规模式，不修改代码） |
| 生产代码变更 | **无**（本验证为只读评估，不改代码/ORM/api.xml/真相源） |
| 回归风险 | **无**（无生产代码变更） |
| 门控依据 | **不以 checker 退出码 0 作为门控依据**（方法论 §8）；本计划无代码变更故不跑 build/test |

actual vs baseline：本计划无生产代码变更，checker 输出与基线一致（扫描既有 finance/budget 代码模式，无新漂移引入）。本验证零代码改动故无 actual/baseline 差异。

### 8.2 closure-audit 独立性声明

本报告由执行者（主代理）编写；独立结束审计由独立子代理（新会话）执行（见 plan §Closure Gates）。执行者未自我审计。

### 8.3 与 arm-index 交叉去重声明

本报告 finding（P2-RC-085 新建）已按 §去重协议 grep arm-index 同域同控制点后给出"新建"裁决（§7.4）：
- 与 A1.7 §2.2/§5.2 caveat ③ 复用关系（caveat ③[BS/IS 安全] + 本验证[cash flow 低风险]分层收口）
- 与 A4.1.5/P1-RC-091 同根因家族不同控制点（试算平衡聚合层 vs cash flow 读路径过滤）
- 与 P1-RC-007 不同控制点（现金流分类 vs postingType 过滤）
- MA4↔A5.6 边界声明（需求契约视角 vs 测试质量评级，不重做 A5.6）

无未经比对直接新建的 finding。

---

## 9. finding 汇总

### P2-RC-085 — cash flow 读路径 postingType 过滤缺失 watch-only（新登记）

| 项 | 内容 |
|---|---|
| 域 | finance |
| UC / 断言 | UC-FIN-16（cash flow 数据集查询链）/ A1.7 §7 SP-1 |
| 问题 | `ErpFinReportBizModel.loadPostedVoucherLines:424-439` 加载已过账凭证行时**不过滤 voucher.postingType**——BUDGET/COMMITMENT 影子凭证（docStatus 同为 POSTED）的现金科目行（1001/1002/1012/1031）同样被 `buildCashFlowDataset:299-323` 计入现金流量表。`postingType` 在 ErpFinVoucher 头（`orm.xml:418`）非 VoucherLine 行（实体无该字段），过滤须在 voucher 头侧 vq 查询。**当前运行时无污染**（BUDGET 凭证科目=6601/6001，COMMITMENT 科目 config=1408，均非现金科目 + seed 零污染凭证 + COMMITMENT config 默认关闭），但**代码无显式守卫**（createFacts/budget line 不过滤科目大类 + loadPostedVoucherLines 不过滤 postingType），未来定制若引入现金科目影子凭证将污染现金流量表 |
| 重要性原因 | 不破坏当前数据——BUDGET/COMMITMENT 影子凭证实操不触现金科目（§3/§5 普查零污染凭证），cash flow 当前正确。**非 P1**——非"活跃数据破坏"（当前无污染），属"潜在风险/守卫缺失"。§2 P2①（潜在风险/守卫缺失——主路径[实操不触现金科目]OK 边界[代码无显式守卫]弱） |
| §2 判据 | §2 P2①（潜在风险——代码无显式守卫，未来定制可能引入现金科目影子凭证） |
| 归属 | MR1 successor（watch-only 登记不强制） |
| 修复方向 | `loadPostedVoucherLines` vq 查询增 `postingType` 过滤（`eq("postingType", ACTUAL)` 或 `notIn("postingType",[BUDGET,COMMITMENT])`，考虑 NORMAL/NULL 两种 ACTUAL 表示，对齐 `ErpFinBudgetControlBiz` 三通道范式）。**纯 BizModel 读侧过滤，按 roadmap 预授权类目可自动执行，不触 §5 ask-first**（非会计过账核心路径，仅报表读侧过滤） |
| 与既有 finding 关系 | 与 P1-RC-091（试算平衡聚合层 BUDGET-only 过滤）同根因家族不同控制点（cash flow 读路径过滤 vs 试算平衡聚合）；与 P1-RC-007（现金流分类缺失）不同控制点（postingType 过滤 vs 分类维度）；与 caveat ③（凭证结构）不同维度（读路径过滤 vs 凭证结构） |

---

## 10. 范围内存疑点处置

| 存疑点 | 处置 |
|---|---|
| A1.7 §7 SP-1（cash flow 读 VoucherLine 不过滤 postingType 的 BUDGET/COMMITMENT 污染） | **解除**——经本核对维持 caveat ③ 接受[cash flow 低风险成立] + 登记 P2-RC-085 watch-only 收口 |

**零 P0**（不触发 MR0）/ **零 P1**（不触发即时通道）。

---

## 11. 验证范围与保护区域

- **验证范围**（只读）：读 `buildCashFlowDataset:299-323` + `loadPostedVoucherLines:424-439` + `isCashSubjectCode:549-553` + `BudgetVoucherGenerator`/`CommitmentVoucherGenerator` 科目分布 + seed 凭证普查 + `app-erp-finance.orm.xml` 范式注记 + `postingType` 字段归属。
- **保护区域**：不触及 ORM/会计过账逻辑**修改**。属 roadmap 预授权类目（只读评估）。
- **不实施修复**（plan Non-Goals）：若发现污染，登记 finding 归 MR1；修复 = `loadPostedVoucherLines` 加 postingType 过滤属 BizModel 读侧过滤[预授权自动执行]，不触 §5 ask-first。
- **不裁决 P1-RC-007**（现金流分类缺失，`:314` 硬编码 OPERATING）——仅记录关联。
- **不展开 A1.7 §7 SP-2/SP-3/SP-4**（多账套渲染/CLOSED 门控/看板行级权限，独立工作项 A4.1.23/A4.1.24/A4.1.25）。
