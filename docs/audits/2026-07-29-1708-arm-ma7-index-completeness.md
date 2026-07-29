# A7.2 索引完整性审计报告

> 审计 ID：A7.2
> 里程碑：MA7（运维与性能层审计）
> 维度：索引完整性（ORM index/unique-key 声明 vs 实际查询模式覆盖矩阵 + 缺索引热点 + 冗余索引 + 复合索引合理性）
> 域范围：S+A 级 10 域（finance / manufacturing / hr / assets [S 级] + purchase / sales / inventory / quality / crm / projects [A 级]）`module-*/model/app-erp-*.orm.xml` + 对应 `erp-*-service` BizModel/Processor 查询模式
> Owner Doc：各域 `module-*/model/app-erp-*.orm.xml`（索引权威源）
> Skill：`docs/skills/open-ended-audit-prompt.md`（开放式审计：主动搜索未知缺索引热点 + 冗余 + 复合升级候选）
> 审计日期：2026-07-29
> 关联 plan：`docs/plans/2026-07-29-1708-1-ma7-error-code-index-nplus1-audit.md` Phase 2
> Source Audits: `docs/audits/2026-07-29-1708-arm-ma7-index-completeness.md`
> Audit Status: closed

## Verdict: ⚠️(P1)（零 P0；1 项 P1 + 2 项 P2 watch-only）

索引覆盖整体扎实（947 索引声明，Top-10 高频查询列 100% 覆盖），但发现 **1 项高影响缺索引热点**：`ErpFinVoucherBillR` 缺 `(billCode, businessType)` 索引，导致每次过账 `alreadyPosted` 幂等预检 + `findBillLinks` 反查对该表全表扫描。该表是业财过账链路的核心回链表，过账高频写路径。P0-MA2-018 的字面 UK 修复因红冲/多账套/软删除三重冲突 deferred，但**非唯一索引**无此冲突，可独立落地。

## 1. 索引声明基线矩阵（S+A 10 域）

**审计方法**：逐域 `rg '<index ' / '<unique-key '` 精确计数 + 索引列频次聚合 + 与查询列频次交叉。

| 域 | `<index>` | `<unique-key>` | 合计 | 级别 |
|----|-----------|----------------|------|------|
| finance | 111 | 18 | 129 | S |
| manufacturing | 113 | 14 | 127 | S |
| hr | 98 | 16 | 114 | S |
| assets | 77 | 11 | 88 | S |
| purchase | 86 | 8 | 94 | A |
| sales | 75 | 9 | 84 | A |
| inventory | 100 | 11 | 111 | A |
| quality | 42 | 11 | 53 | A |
| crm | 66 | 17 | 83 | A |
| projects | 54 | 10 | 64 | A |
| **合计** | **822** | **125** | **947** | — |

**Unique-key 模式**：100% 经 `<unique-key name="UK_*" columns="..."/>` 声明（非 `<index unique="true">`）。事务单据普遍 `UK_*_CODE_ORG (code, orgId)`（多公司隔离自然键，~70 处，A2.18 已确认正确）。零 `<index unique="true">`。

## 2. 查询列频次 × 索引覆盖交叉矩阵

**审计方法**：S+A 10 域 service 层 `rg 'eq\("[a-zA-Z]+'` 提取查询列频次 × ORM `<index><column name=X>` 覆盖频次交叉。

### 2.1 高频查询列——100% 已索引（PASS）

| 查询列 | 查询站点数 | 索引覆盖数 | 裁决 |
|--------|-----------|-----------|------|
| `status` | 35 | 44 | ✅ 充分覆盖 |
| `employeeId` | 31 | 18 | ✅ 覆盖（多域分布） |
| `periodId` | 30 | 13 | ✅ 覆盖 |
| `code` | 29 | unique-key `UK_*_CODE_ORG` | ✅ 唯一约束覆盖 |
| `materialId` | 28 | 46 | ✅ 充分覆盖 |
| `docStatus` | 26 | 47 | ✅ 充分覆盖 |
| `orgId` | 18 | 142 + unique-key | ✅ 充分覆盖（含多公司隔离） |
| `projectId` | 14 | 17 | ✅ 覆盖 |
| `batchNo` | 13 | inventory 域索引 | ✅ 覆盖 |
| `warehouseId` | 12 | 26 | ✅ 覆盖 |

### 2.2 中低频查询列——索引缺口（P1/P2）

| 查询列 | 查询站点数 | 索引覆盖数 | 裁决 |
|--------|-----------|-----------|------|
| **`billCode`**（ErpFinVoucherBillR） | 高频（每次过账） | **0**（billR 表仅 voucherId 索引） | **🔴 P1-MA7-001**（见 §3） |
| **`businessType`**（ErpFinVoucherBillR + ErpFinVoucher） | 11 | 1（非 billR 表） | **🔴 P1-MA7-001**（与 billCode 复合） |
| `isReversed` | 8（finance 凭证聚合） | 0 | P2-MA7-004（与 periodId 复合升级候选） |
| `approveStatus` | 10 | 2 | P2-MA7-004（常与 docStatus 复合，docStatus 已索引） |
| `posted` | 4（dashboard + period 集成） | 0 | P2-MA7-004（常与 orgId 复合） |
| `sourceBillType`/`sourceBillCode` | 5 | 1 each | P2 watch-only（回链查询低频） |
| `relatedBillType`/`relatedBillCode` | 6 | 0 | P2 watch-only（概念弱指针） |
| `year`/`month`（ErpFinAccountingPeriod） | 17/11 | 0 | ✅ 可接受（期间表小数据量，~12 行/年/组织） |
| `direction`/`costMethod` | 11/6 | 0 | ✅ 可接受（配置表小数据量） |

## 3. 缺索引热点详查

### P1-MA7-001 `ErpFinVoucherBillR` 缺 `(billCode, businessType)` 索引

**表**：`erp_fin_voucher_bill_r`（业财回链表，过账链路核心）

**现状索引**（`module-finance/model/app-erp-finance.orm.xml:643-647`）：
```xml
<indexes>
    <index name="IDX_FIN_VOUCHER_BILL_R_VOUCHER_ID" unique="false">
        <column name="voucherId"/>
    </index>
</indexes>
```
仅 `(voucherId)` 单列索引——服务于「按凭证反查回链」方向。

**热点查询路径**（每次过账必经）：
- `ErpFinPostingProcessor.findBillLinks:884-888`：
  ```java
  QueryBean q = new QueryBean();
  q.addFilter(and(eq("billCode", billHeadCode), eq("businessType", businessType.name())));
  ```
- 调用方：`alreadyPosted:473`（过账幂等预检，**每次过账必调**）+ `reverseVoucher:850,868,911`（红冲反查）+ `BankReconAdjustmentVoucherBuilder:112`（银行调节反查）。

**影响**：`erp_fin_voucher_bill_r` 无 `(billCode, businessType)` 索引 → `alreadyPosted` 每次过账对该表**全表扫描**。随凭证累积，过账延迟线性增长。单组织种子下数据量小（无明显症状），生产规模（万级凭证 × 多业务类型）成为过账链路热点。

**与 P0-MA2-018 的关系**：P0-MA2-018 提议加 `UK_FIN_VOUCHER_BILL_R_BILL` unique 约束 on `(billCode, businessType)`，但经独立 plan-audit 裁定**不可实施**（红冲同键 2 行 / 多账套同键 N 行 / 软删除重插三重冲突，见 arm-index P0-MA2-018 修复状态 `deferred`）。**但非唯一索引无此冲突**——红冲产生 2 行同 `(billCode, businessType)` + 多账套产生 N 行同键，非唯一索引正常容纳，仅加速查询不强制唯一。故 P1-MA7-001 的非唯一索引修复路径与 P0-MA2-018 deferred UK **不冲突，可独立落地**。

**修复方式**：MR3 裁决——`app-erp-finance.orm.xml` ErpFinVoucherBillR `<indexes>` 增 `<index name="IDX_FIN_VOUCHER_BILL_R_BILL_CODE_BIZ_TYPE" unique="false"><column name="billCode"/><column name="businessType"/></index>`（codegen 增量再生）。触及 ORM ask-first 保护区域，修复须独立 plan-audit + 人工确认。

## 4. 冗余索引评估

### P2-MA7-003 防御性 FK 索引策略（~120 索引覆盖零查询列）

**审计方法**：索引列频次（§2）× 查询列频次交叉，识别「索引覆盖高但查询命中零」的列。

| 索引列 | 索引数 | 查询站点数 | 裁决 |
|--------|--------|-----------|------|
| `currencyId` | 51 | 0 | 防御性 FK 索引（UI 显示 join 用，非过滤） |
| `uoMId` | 26 | 0 | 同上 |
| `locationId` | 7 | 0 | 同上 |
| `ownerId` | 9 | 0 | 同上 |
| `departmentId` | 9 | 0 | 同上 |
| `costCenterId` | 5 | 0 | 同上 |
| `categoryId` | 5 | 0 | 同上 |
| `competencyId` | 5 | 0 | 同上 |
| `workcenterId` | 8 | 0 | 同上 |
| **合计** | **~125** | **0** | — |

**裁决**：**防御性 FK 索引策略，可接受**。索引 FK 列是 DB 最佳实践（加速 JOIN + 未来查询就绪 + codegen 自动化）。代价是写时索引维护 + 存储，对种子规模可忽略；生产规模下可按 `EXPLAIN` 实测后选择性删除真正零 JOIN + 零查询的索引。不强制 MR3 修复——登记 P2 watch-only，生产性能压测触发时按实测裁决。

## 5. 复合索引升级候选

### P2-MA7-004 复合索引升级候选（3 处）

| 候选 | 表 | 现状 | 升级方案 | 触发条件 |
|------|-----|------|---------|---------|
| `(orgId, posted)` | 销售域 ErpSalDelivery/Invoice/Receipt + 采购域 ErpPurReceive/Invoice/Payment 等事务单据 | orgId 单列已索引；`posted` 零索引 | 增 `(orgId, posted)` 复合索引 | dashboard 未过账查询（`eq("posted", TRUE)` 站点 2-3 处/域）生产规模下成热点时 |
| `(periodId, isReversed)` | `erp_fin_voucher_line` | periodId 已索引；isReversed 零索引 | 增 `(periodId, isReversed)` 复合 | 期间结账/预算控制/FX 重估凭证聚合（8 站点 `eq("isReversed", FALSE)` + periodId）生产规模下成热点时 |
| `(billCode, businessType)` | `erp_fin_voucher_bill_r` | 零索引 | **已升级为 P1-MA7-001**（高频写路径，独立落地） | 当前即需 |

**裁决**：前 2 处为 P2 watch-only——当前单列索引（orgId / periodId）已提供首列过滤，复合索引仅优化「首列过滤后对二级列再过滤」的回表行数。种子规模下无显著差异；生产规模 + 性能压测显示热点时再落地。

## 6. Finding 汇总

### P0（即时通道）

无。索引缺口不致活跃数据破坏（仅性能影响）；P1-MA7-001 是性能热点非正确性缺陷。

### P1（目标 MR3）

| Finding ID | 描述 | 目标 MR | 修复状态 |
|-----------|------|--------|---------|
| `P1-MA7-001` | **`ErpFinVoucherBillR` 缺 `(billCode, businessType)` 索引**——`erp_fin_voucher_bill_r` 仅 `(voucherId)` 单列索引，但 `ErpFinPostingProcessor.findBillLinks:884-888` + `alreadyPosted:473`（每次过账幂等预检）+ `reverseVoucher:850,868,911`（红冲反查）+ `BankReconAdjustmentVoucherBuilder:112` 均按 `(billCode, businessType)` 查询 → 全表扫描。随凭证累积过账延迟线性增长。与 P0-MA2-018 deferred UK（红冲/多账套/软删除冲突）**不冲突**——非唯一索引正常容纳多行同键，仅加速查询。 | MR3 | todo |

### P2（watch-only）

| Finding ID | 描述 | 处置 |
|-----------|------|------|
| `P2-MA7-003` | **防御性 FK 索引策略（~125 索引覆盖零查询列）**：currencyId(51)/uoMId(26)/locationId(7)/ownerId(9)/departmentId(9)/costCenterId(5)/categoryId(5)/competencyId(5)/workcenterId(8) 等 FK 列被索引但零 `eq()/in()` 查询命中。是 DB 最佳实践（JOIN 加速 + 未来就绪 + codegen 自动），代价为写时维护 + 存储。 | watch-only，生产性能压测触发时按 `EXPLAIN` 实测选择性删除真正零 JOIN + 零查询索引 |
| `P2-MA7-004` | **复合索引升级候选 2 处**：(a) `(orgId, posted)` 事务单据表优化 dashboard 未过账查询；(b) `(periodId, isReversed)` 凭证行表优化期间结账聚合。当前单列索引已提供首列过滤，复合仅优化回表行数。 | watch-only，生产规模 + 性能压测显示热点时落地 |

## 7. 与 MA1-MA6 已登记 finding 交叉去重

| 本审计观察 | 已登记 finding | 关系 |
|-----------|---------------|------|
| ErpFinVoucherBillR 缺 (billCode, businessType) 索引 | P0-MA2-018（billR UK deferred） | **互补不重复**：P0-MA2-018 是 UK 唯一约束（防重复凭证，deferred 因冲突）；P1-MA7-001 是非唯一索引（加速查询，无冲突可独立落地）。同一表同一列对，不同约束类型，不同修复路径 |
| 防御性 FK 索引冗余 | n/a | 新观察，P2 watch-only |
| 复合索引升级候选 | n/a | 新观察，P2 watch-only |
| ErpFinAccountingPeriod year/month 无索引 | n/a | 可接受（小表 ~12 行/年/组织） |

## 8. Exit Criteria 核实

- [x] S+A 级 10 域索引覆盖矩阵产出（§1 声明基线 + §2 查询列×覆盖交叉矩阵 + §3 缺索引热点 [P1-MA7-001] + §4 冗余索引 [P2-MA7-003] + §5 复合升级候选 [P2-MA7-004]）
- [x] A7.2 P0/P1/P2 已登记 arm-index.md（零 P0 + 1 P1 + 2 P2）且去重（§7 与 P0-MA2-018 互补不重复）
