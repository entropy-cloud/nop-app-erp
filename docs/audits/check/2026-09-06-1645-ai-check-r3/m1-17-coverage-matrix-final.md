# M1.17 五维 × 21 核对单元覆盖矩阵终态核账（105 基础格全景）

> 收官核账产物（plan `2026-09-10-0251-1-m117-audit-phase-closure.md` Phase 2；roadmap M1.17）。
> 核账时点：HEAD `5c97f878c`（2026-09-10），`git status --porcelain` 脏面 = 1 untracked 计划文件（本计划）披露，零生产路径脏面。
> 方法：逐份机械抽取 28 份 `ck-*-r3.md` §1 五维覆盖矩阵行（`| **DIM-x ...` 表行，末列 verdict 首词 `pass`/`finding`/`n-a`），按 `m0-5-audit-checklists.md` §4 冻结映射表聚合为 21 单元 × 5 维基础格；抽取脚本全程可重放，聚合规则 = S 级多切片基础格任一切片 `finding` 则该基础格 `finding`（全 `pass` 方为 `pass`；`n-a` 仅 U20 DIM-F/S 两格显式带理由）。
> 判定源冻结：`m0-5-audit-checklists.md` §3.3 全矩阵格册 + §4 唯一格集合映射（140 切片子格 → 105 基础格）。

## 1. 28 份报告五维 verdict 机械抽取结果（切片子格层，140 格）

| 报告 | 工作项 | 单元(切片) | B | F | S | T | I | 新立 finding（分级） |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| ck-finance-posting-r3.md | M1.1 | U05(fin-1) | finding | pass | pass | pass | pass | 0 |
| ck-finance-arap-r3.md | M1.2 | U05(fin-2) | finding | pass | pass | finding | pass | 1（1 P2） |
| ck-finance-budget-costing-r3.md | M1.3 | U05(fin-3) | finding | pass | pass | finding | pass | 3（3 P3） |
| ck-finance-period-misc-r3.md | M1.4 | U05(fin-4) | finding | pass | pass | pass | pass | 3（3 P3） |
| ck-mfg-workorder-r3.md | M1.5 | U08(mfg-1) | finding | finding | pass | pass | pass | 2（1 P1 + 1 P3） |
| ck-mfg-bom-mrp-r3.md | M1.6 | U08(mfg-2) | finding | finding | pass | finding | pass | 5（5 P3） |
| ck-mfg-subcontract-r3.md | M1.7 | U08(mfg-3) | finding | finding | pass | pass | pass | 2（2 P3） |
| ck-assets-lifecycle-r3.md | M1.8 | U06(ast-1) | finding | pass | pass | pass | pass | 1（1 P3） |
| ck-assets-depreciation-r3.md | M1.9 | U06(ast-2) | finding | pass | pass | finding | pass | 3（2 P2 + 1 P3） |
| ck-hr-org-r3.md | M1.10 | U14(hr-1) | finding | pass | pass | pass | pass | 0 |
| ck-hr-attendance-payroll-r3.md | M1.11 | U14(hr-2) | finding | pass | pass | finding | pass | 4（4 P3） |
| ck-projects-r3.md | M1.12 | U07 | finding | pass | pass | pass | pass | 4（4 P3） |
| ck-quality-r3.md | M1.12 | U09 | finding | pass | pass | pass | pass | 8（2 P2 + 6 P3） |
| ck-purchase-r3.md | M1.13 | U03 | finding | finding | pass | pass | pass | 3（1 P2 + 2 P3） |
| ck-sales-r3.md | M1.13 | U04 | finding | pass | pass | pass | pass | 0 |
| ck-inventory-r3.md | M1.13 | U02 | finding | pass | pass | pass | pass | 1（1 P2） |
| ck-crm-r3.md | M1.14 | U12 | finding | finding | pass | pass | pass | 5（1 P2 + 4 P3） |
| ck-cs-r3.md | M1.14 | U13 | finding | finding | pass | pass | pass | 5（5 P3） |
| ck-contract-r3.md | M1.14 | U18 | finding | finding | pass | pass | pass | 6（2 P1 + 1 P2 + 3 P3） |
| ck-b2b-r3.md | M1.14 | U17 | finding | pass | pass | pass | pass | 4（1 P2 + 3 P3） |
| ck-drp-r3.md | M1.14 | U19 | finding | pass | pass | pass | pass | 4（1 P2 + 3 P3） |
| ck-maintenance-r3.md | M1.15 | U10 | finding | finding | pass | finding | pass | 4（4 P3） |
| ck-aps-r3.md | M1.15 | U15 | finding | pass | pass | finding | pass | 5（1 P2 + 4 P3） |
| ck-logistics-r3.md | M1.15 | U16 | finding | finding | pass | pass | pass | 4（1 P2 + 3 P3） |
| ck-notify-r3.md | M1.15 | U11 | finding | finding | pass | pass | pass | 3（2 P2 + 1 P3） |
| ck-master-data-r3.md | M1.15 | U01 | finding | finding | pass | finding | pass | 6（1 P2 + 5 P3） |
| ck-common-r3.md | M1.15 | U20 | finding | **n-a** | **n-a** | pass | pass | 5（1 P2 + 4 P3） |
| ck-app-erp-all-r3.md | M1.16 | U21 | finding | finding | pass | pass | pass | 2（2 P3） |

切片子格合计 = 28 × 5 = **140**，与 §4 冻结对账式 140（75 C 级 + 55 S 级 + 10 U20/U21）逐格归属一致；**缺格 = 0**。

## 2. 21 单元 × 5 维基础格全景矩阵（105 格）

> 每格登记：报告文件 → verdict（finding 计数引用 §1 末列/§4 汇总）。S 级 4 单元（U05/U06/U08/U14）基础格 = 切片聚合（任一切片 finding → 基础格 finding）。

| 单元 | DIM-B | DIM-F | DIM-S | DIM-T | DIM-I |
| --- | --- | --- | --- | --- | --- |
| U01 master-data | finding（ck-master-data-r3） | finding | pass | finding | pass |
| U02 inventory | finding（ck-inventory-r3） | pass | pass | pass | pass |
| U03 purchase | finding（ck-purchase-r3） | finding | pass | pass | pass |
| U04 sales | finding（ck-sales-r3） | pass | pass | pass | pass |
| U05 finance | finding（4 切片全 finding） | pass（4 切片全 pass） | pass（4 切片全 pass） | finding（fin-2/fin-3） | pass（4 切片全 pass） |
| U06 assets | finding（ast-1/ast-2） | pass（2 切片全 pass） | pass（2 切片全 pass） | finding（ast-2） | pass（2 切片全 pass） |
| U07 projects | finding（ck-projects-r3） | pass | pass | pass | pass |
| U08 manufacturing | finding（3 切片全 finding） | finding（3 切片全 finding） | pass（3 切片全 pass） | finding（mfg-2） | pass（3 切片全 pass） |
| U09 quality | finding（ck-quality-r3） | pass | pass | pass | pass |
| U10 maintenance | finding（ck-maintenance-r3） | finding | pass | finding | pass |
| U11 notify | finding（ck-notify-r3） | finding | pass | pass | pass |
| U12 crm | finding（ck-crm-r3） | finding | pass | pass | pass |
| U13 cs | finding（ck-cs-r3） | finding | pass | pass | pass |
| U14 hr | finding（hr-1/hr-2） | pass（2 切片全 pass） | pass（2 切片全 pass） | finding（hr-2） | pass（2 切片全 pass） |
| U15 aps | finding（ck-aps-r3） | pass | pass | finding | pass |
| U16 logistics | finding（ck-logistics-r3） | finding | pass | pass | pass |
| U17 b2b | finding（ck-b2b-r3） | pass | pass | pass | pass |
| U18 contract | finding（ck-contract-r3） | finding | pass | pass | pass |
| U19 drp | finding（ck-drp-r3） | pass | pass | pass | pass |
| U20 common-service | finding（ck-common-r3） | **n-a**（判定面归 U21，见 §3） | **n-a**（判定面归 U21，见 §3） | pass（ck-common-r3） | pass（ck-common-r3） |
| U21 app-erp-all | finding（ck-app-erp-all-r3） | finding（ck-app-erp-all-r3） | pass（ck-app-erp-all-r3） | pass（ck-app-erp-all-r3） | pass（ck-app-erp-all-r3） |

### 2.1 机械完整性断言

- 105 基础格 = 21 单元 × 5 维全落盘：**零缺失**（140 切片子格全部归属到唯一基础格，无重叠无空隙，§4 映射逐行核对）。
- 基础格 verdict 分布：B = 21 finding；F = 10 finding + 10 pass + 1 n-a；S = 20 pass + 1 n-a；T = 7 finding + 14 pass；I = 21 pass。合计 **finding 38 / pass 65 / n-a 2 = 105**（对账一致）。
- 切片子格 verdict 分布：B = 28 finding；F = 12 finding + 15 pass + 1 n-a；S = 27 pass + 1 n-a；T = 8 finding + 20 pass；I = 28 pass。合计 **finding 48 / pass 90 / n-a 2 = 140**（对账一致）。
- DIM-I 28/28 切片子格全 pass（MI 先行清剿后零回归），DIM-S 除 U20 显式 n-a 外全 pass——两维无新 finding，与 MI.9 收官终态一致。

## 3. U20 特例交叉核对（n-a 归属转移，非漏审）

- `ck-common-r3.md` DIM-F verdict 原文：`**n-a**（common 层无独立页面，判定面归 U21）`；DIM-S verdict 原文：`**n-a**（common 层无独立 seed，判定面归 U21）`。
- `ck-app-erp-all-r3.md`（U21）实际覆盖面交叉确认：DIM-F = 菜单全局面（`component=FLUX` 全量清点 + 菜单→页面映射）+ `npm run validate:flux` 导出门禁全链（0 error/999 页/855 erp 页 + 325 ERR variant 族对账）+ 464 URL 全量核 461 达 3 dead 归并 common-002——**含 common 层页面/菜单判定面**；DIM-S = `_init-data` 全量 seed 口径（`TestErpSeedDataIntegrity` 4/0/0/0 门禁宿主 + 372 CSV + 1 SQL 资产清点 + 零孤儿）——**含 common 层 seed 判定面**。
- 结论：U20 两格 n-a 为**归属转移**（`m0-5-audit-checklists.md` §3.2 共享代码唯一归属裁决「聚合横切面归 U21」的执行结果），与 U21 实覆盖交叉一致，非漏审；105 格完整性断言在 n-a 显式带理由前提下成立。

## 4. 汇总统计（全 mission M1 阶段新立 `-r3` finding）

- 分级计数：**P0 = 0 / P1 = 3 / P2 = 17 / P3 = 73，合计 93**。
- 与权威对账源三方一致：①28 份报告各自 §统计 新立数逐报告相加 = 93；②跨轮索引 `docs/audits/check/ai-check-index.md` §Finding 追踪 r3 行逐 ID 计数 = 93 行（P1 3 / P2 17 / P3 73）、distinct ID 零重复；③本轮索引 `ai-check-r3-index.md` 各报告行内联新立计数逐行一致。
- P1 = 3：P1-CK-mfg-022-r3（reverseApprove 终态复活）、P1-CK-ct-025-r3（sign-status/provider 字典 value-code 双轨）、P1-CK-ct-026-r3（返利协议 ACTIVE 死状态前置悖论）。
- P2 = 17：fin2-018 / ast2-024 / ast2-026 / qa-026 / qa-027 / pur-015 / inv-012 / crm-020 / ct-027 / b2b-017 / drp-021 / aps-012 / log-012 / notify-009 / notify-010 / md-015 / common-011（均 `-r3` 后缀，全称见跨轮索引 §Finding 追踪对应行）。
- P3 = 73：见跨轮索引 §Finding 追踪（含双 handoff 全仓族聚合裁决 common-014/015-r3 两槽位）。
- 历史通道统计（三态裁决口径）：各切片复用（已修 HEAD 复核有效）与归并（现症复核）合计覆盖 r1/r2 历史面，历史 ID 零覆写零冲突（跨轮索引 §Finding 追踪 r1/r2 行保留完整，per-ID `-r3` 后缀新立无撞号）。

## 5. 核账中登记的既有漂移（不就地裁决，显式登记）

1. **跨轮索引 §报告清单 8 行 P1/P2 计数列列序误植**：`ck-quality-r3`（列 P1=2 应 P2=2）/ `ck-purchase-r3`（P1=1 应 P2=1）/ `ck-inventory-r3`（P1=1 应 P2=1）/ `ck-aps-r3`（P1=1 应 P2=1）/ `ck-logistics-r3`（P1=1 应 P2=1）/ `ck-notify-r3`（P1=2 应 P2=2）/ `ck-master-data-r3`（P1=1 应 P2=1）/ `ck-common-r3`（P1=1 应 P2=1）——8 行合计 10 个 P2 计数被写入 P1 列（列合计 P1=13/P2=7 vs 权威 3/17，差值 10 双向吻合）。**裁决**：§Finding 追踪 per-ID 行为权威记录（与 28 报告 + roadmap 三方逐值一致），8 行同 row 内联描述的分级文字均正确，属汇总派生列誊写漂移（同型先例：mfg-workorder 行 2026-09-08 勘误注记）；跨轮索引在本计划为只读核对面 → successor: M2.9 trigger:双索引状态回填时修正 8 行 P1/P2 计数列。
2. **validate:flux 325 条 variant 族既有漂移**：已在 M1.16 等切片命中面对账（100% variant=primary dropdown-button stub 族，非本轮 finding），既有 successor 在案（r1 同族通道），本计划不重开。
3. compliance checker 机器块差值（R2b/R2c/R12a 基线 raise）已由 plan `2026-09-09-2100-1` 闭合（Phase 1 复跑逐值一致，无新漂移）。

## 6. 剩余风险声明（M2.x 修复批输入面）

- 93 个 open `-r3` finding（P1 3 / P2 17 / P3 73）为 M2.1~M2.8 修复批的直接输入面：P0 即时通道空集；P1 3 条按域归 M2.3（mfg-022）/ M2.7（ct-025/026）；P2/P3 按 M2.2~M2.7 域批 + M2.8 横切族（daoFor 豁免注释族 common-014、ServiceContextImpl 裸 new 族 common-015、FNPT 零注册族、零测试断言族）承接；强制「先写失败测试 → 修复 → 绿」方法论（M2.0）。
- P3-CK-app-001-r3（八扩展域 action-auth 保留层零 x:extends 继承）为 enforcement 前置阻塞面：启用强制 FNPT 门控前须先修，归 M2.8/M2.0 裁决排序。（勘误 2026-09-10：本节初稿误植前缀 `P1-CK-app-001-r3`，权威面——§4 P1 清单 / 跨轮索引 §报告清单 / `ck-app-erp-all-r3.md` / roadmap M1.16 行——均为 P3，随独立收官审计 Minor 1 更正；零计数/零裁决影响，M2.9 双索引回填时同批复核）
- owner-doc 漂移簇（fin3-018 / hr2-027 / mnt-021 / md-017 / app-002 / qa-031/032 等⑮维族）归 M2.x doc 批修订或标 Deferred 裁决。
- 跨轮索引 §报告清单 8 行计数列修正归 M2.9 双索引状态回填（§5.1 登记）。
