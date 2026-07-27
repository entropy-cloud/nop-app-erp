# MA1 架构治理复审报告（A1.14）

> 复审日期：2026-07-27 14:30
> 复审类型：架构治理残留核验 + 新漂移扫描（首审报告 `docs/audits/2026-07-23-0000-architecture-governance-review.md` 的复审）
> 复审方法：主代理读取首审报告 + M0.3 锚点 + 19 规则 compliance checker + 实仓 grep 复核（daoFor Type 1/4 / 字典 / 共享内核 / CI guard 四子维度）
> 复审范围：全 19 `module-*/` + `app-erp-all` + `.github/workflows/compliance.yml`
> 对照基线：`docs/audits/compliance-baseline.md §M0 锚点注记`（HEAD=0e963531d，2026-07-27 落锚）
> 当前 HEAD：`8f53ed612`（首审报告产出后 5 个 commits 内的密集审计-修复计划落地）
> 来源工作项：`docs/backlog/audit-remediation-roadmap.md` MA1 §A1.14
> 关联首审：`docs/audits/2026-07-23-0000-architecture-governance-review.md`（F1–F9 全闭包）
> 上游 MA1 审计：A1.10 跨模块 DAG（`2026-07-27-1227-arm-ma1-cross-module-dag.md`）+ A1.11/A1.12/A1.13 平台合规

---

## 执行摘要

A1.14 复审核验：(1) F1–F9 残留**未回退**；(2) 自首审（2026-07-23）以来 5 天内的密集审计-修复计划落地**未引入新的 P0 架构治理缺口**；(3) CI guard 基线相对 M0.3 锚点（HEAD=0e963531d）**无上漂**——全 19 规则 actual ≤ baseline，且 R2c 实际下降 -2（合规改善）。

复审发现 **0 个 P0** + **1 个新 P1**（`P1-MA1-029` ErpCtInvoicePlanBizModel 跨域写半治理，与首审 F1 同根因）。该 P1 已登记 `arm-index.md §P1 发现汇总`，进入 MR1。daoFor Type 4 残留 + governed path 平台解耦维持首审 deferred 状态（successor 已命名触发条件）。

**MA1 架构治理维度全域 19 列全部 ✅/⚠️，无 ❓**。本复审完成后 MA1 里程碑（A1.1–A1.14）全部 done。

---

## F1–F9 残留复审表

| Finding | 首审闭包结论 | 复审结论 | 复审证据 |
|---------|-------------|---------|---------|
| **F1** daoFor 跨域访问真违规子集 + 已登记豁免 | Type 1 chained/variable-split 两形态全域清零（37 处重构）；Type 4（~10-30 处跨域写/读）阻塞 successor 裁决分支 (b) 需平台 lazy/SPI 解耦；3 跨域写豁免登记（MrpReleaseService / ErpCtRebateSettlementBizModel / ErpB2bAsnBizModel） | **未回退**。Type 1 `findAllByQuery` watch-only residual 实测 **110 处**（首审估算 ~113，误差 ±3 在合理范围内，未恶化）；3 已登记豁免文件经实测仍存在且代码模式未变；R2b/R2c/R2d 实测 315/1226/28 均等于或低于 M0.3 基线 315/1228/28（R2c -2 合规改善）。**1 个新发现**：`ErpCtInvoicePlanBizModel`（contract→pur/sal 跨域写）有 javadoc bypass rationale 但**未**登记 `posting-exemptions.md`（首审 line 130/144 已识别该文件为 "bypass rationale 文件" 但闭包时仅补登了 ErpB2bAsnBizModel）→ 登记为 `P1-MA1-029` 进入 MR1 | `find module-* -name '*.java'` grep `daoFor(Erp.*\.findAllByQuery` = 110；`grep ErpB2bAsn\|ErpCtRebate\|MrpRelease posting-exemptions.md` 各命中；compliance checker actual ≤ baseline 全 19 规则 |
| **F2** 字典与状态枚举真相碎裂 | D1 全域推广完成（9 域 `Erp*DocStatus` + doc-status 6 域共享 dict 统一 + 8 per-domain approve-status.dict.yaml 移除）；cs `time-entry-approve-status` 永久裁决特化 | **未回退**。9 域 `Erp*DocStatus` dao 层接口全部存在（ast/cs/fin/inv/mnt/mfg/pur/qa/sal）；`doc-status.dict.yaml` 仅存于 `module-common-service/_vfs/dict/erp/`（6 域共享）；`find module-* -name 'approve-status.dict.yaml'` = 0（per-domain 全部移除）；cs `time-entry-approve-status` 仍为合法特化保留 | `find module-*/erp-*-dao -name 'Erp*DocStatus.java'` = 9；`find module-* -name 'doc-status.dict.yaml' -path '*/src/*'` = 1；`find module-* -name 'approve-status.dict.yaml'` = 0 |
| **F3** ORM 跨业务域 DAG 边登记不完整 | `data-dependency-matrix.md §5.6.2` 补 3 边（drp→inv ErpInvStockMove + mfg→inv ErpInvBatch + mnt→ast ErpAstAsset） | **未回退**。3 边仍在 §5.6.2（line 530/532/538），均标注"批准保留" | `grep -nE 'ErpInvStockMove\|ErpInvBatch\|ErpAstAsset' docs/architecture/data-dependency-matrix.md` 命中 3 边行 |
| **F4** 隐性共享内核 finance + master-data | 3 跨域语义类型（`ErpFinBusinessType` enum / `PostingEvent` DTO / `AcctSchemaResolver` dao 工具）裁决为显式共享内核（不迁移），R12 守卫追踪跨域 import 基线 69/66/38 | **未回退**。R12a/R12b/R12c 实测 69/66/38 **精确等于 M0.3 锚点基线**——零跨域 import 增长，共享内核未静默扩张 | compliance checker §R12 输出：R12a=69 / R12b=66 / R12c=38 |
| **F5** notify 子系统无 owner docs | `docs/design/notify/README.md` 创建 + `module-boundaries.md §Owner Docs` 增 notify 行 | **未回退**。`docs/design/notify/README.md` 存在（9215 字节）；`module-boundaries.md` 含 notify 行 | `ls docs/design/notify/README.md` + `grep notify docs/architecture/module-boundaries.md` 命中 |
| **F6** mfg 依赖 qa 生成常量 | `ErpQaInspectionType` 非生成接口在 `erp-qa-dao` 创建，`ErpMfgWorkOrderProcessor` 改引该接口 | **未回退**。`ErpQaInspectionType.java` 仍在 `module-quality/erp-qa-dao/.../constants/`；`grep _ErpQaDaoConstants module-manufacturing` 排除 _gen/target 后 = 0 | `find module-quality -name 'ErpQaInspectionType.java'` + `grep -rn _ErpQaDaoConstants module-manufacturing` |
| **F7** drp 实体命名前缀越界 | 4 实体保留 `ErpInvDrp*` 类名 + 登记命名例外（方案 b，零 ORM 风险） | **未回退**。`docs/design/drp/README.md §ErpInvDrp* 实体命名例外登记`（plan 2026-07-24-1400-2 §F7 裁决）仍存在，含 4 实体逐项登记（类名/className/表名/所属域/豁免理由/收敛触发条件） | `grep -nE 'ErpInvDrp.*命名例外' docs/design/drp/README.md` 命中 |
| **F8** Compliance checker 未集成 CI、无基线日志 | checker 接入 CI（`.github/workflows/compliance.yml`）+ 基线日志（`compliance-baseline.md` 19 行精确基线 + machine-readable YAML 块）+ 门控经 anti-fake-green 三例证明有效 | **未回退**。`.github/workflows/compliance.yml` 仍在 PR 检查路径（`on: pull_request: branches: [master]`）；`compliance` job 运行 checker + 解析 baseline YAML 块 + 单向收紧门控；`web-pages-validation` job 仍存在；compliance checker 全 19 规则 actual ≤ baseline 验证有效 | `.github/workflows/compliance.yml` 含 `compliance:` + `web-pages-validation:` 两个 jobs |
| **F9** 19 web 冒烟测试在模块级构建系统性跳过 | 19 `Erp*WebPagesTest` 改为 `@Tag("full-app")` + 模块级 surefire `excludedGroups=full-app` + CI `app-erp-all` 阶段强制运行页面校验 | **未回退**。19 web 测试均使用 `@Tag("full-app")`（38 grep 命中 = 19 注解 + 19 import）；`@Disabled` 全仓 web 模块 = 0；`web-pages-validation` job 仍运行 `ErpAllWebPagesTest` | `grep -rE '@Tag.*"full-app"' module-*/erp-*-web/` = 38；`grep -rE '@Disabled' module-*/erp-*-web/` = 0 |

---

## 新漂移扫描结论（自 2026-07-23 首审以来）

### 1. 跨域写（daoFor + saveEntity/updateEntity）

**精确扫描**（`daoFor(Erp<Other>.class).saveEntity/updateEntity` 同行链式调用）：

| 文件 | 源→目 | 行为 | 治理状态 |
|------|------|------|---------|
| `MrpReleaseService.java:137,152` | mfg→pur ErpPurOrder(Line) | 跨域写 PO 骨架草稿 | ✅ 已登记（posting-exemptions.md） |
| `ErpCtRebateSettlementBizModel.java:153,186` | contract→pur/sal ErpInvoiceLine | 跨域写负额发票（贷项凭证） | ✅ 已登记（posting-exemptions.md） |
| `ErpB2bAsnBizModel.java:226` | b2b→pur ErpPurReceive | 跨域写 ASN 接收单草稿（config-gated） | ✅ 已登记（posting-exemptions.md，首审 F1 闭包项 #2 补登） |
| `ErpCtInvoicePlanBizModel.java:159,196` | contract→pur/sal ErpInvoiceLine | 跨域写发票计划草稿 | ⚠️ **未登记**（javadoc bypass rationale 存在 line 41-45，但 `posting-exemptions.md` 未收录）→ **新 P1**（`P1-MA1-029`） |

**结论**：1 个新 P1 finding（`P1-MA1-029` ErpCtInvoicePlanBizModel 半治理），与首审 F1 同根因（首审已识别但未在闭包时补登豁免）。**0 个新 P0**。3 已登记豁免文件全部保持。

### 2. 其他治理漂移维度（5 启发式）

| 启发式 | 首审基线 | 当前 actual | 状态 |
|--------|---------|-------------|------|
| R4 extends RuntimeException | 0 | 0 | ✅ 无漂移 |
| R5 @Inject private | 0 | 0 | ✅ 无漂移 |
| R6 @Transactional in BizModel | 2 | 2（ErpFinVoucherBizModel post/reverse） | ✅ 无漂移 |
| R7 System.currentTimeMillis() | 0 | 0 | ✅ 无漂移 |
| LocalDate.now()（R7 同性质，watch-only） | 1（首审 P2-MA1-019：ErpFinVoucherTemplateBizModel:95） | 2（新增 ErpMdCurrencyBizModel:60，programmer-error 路径） | ✅ watch-only（非 GraphQL 面向，无 checker 规则） |

### 3. 重点扫描密集变更域（2026-07-24→27 落地的 5 commits 后）

finance GL mapping / intercompany / commitment / mfg MRP-DRP 仿真 / FX notes 五个密集变更域：

- **finance**：R2b 跨域 daoFor 用量 = 首审基线（315，未变）；R12 共享内核 import 零增长；新跨域写 = 0
- **mfg**：MRP/DRP 仿真域新增 daoFor 用量已在 plan `2026-07-25-1057-1/1057-2` 中经独立基线裁决吸收（基线上调至 1228 后实测 1226）；MrpReleaseService 跨域写豁免未变
- **inventory**：P0-MA1-021 已闭包（plan `2026-07-27-1430-1`）—— CostAdjustmentPostingDispatcher 不再直接写 `ErpFinVoucher`，改为注入 `IErpFinVoucherBiz`；R2c 实测下降 -2 部分源于此修复

---

## CI Guard 基线对照（M0.3 锚点 HEAD=0e963531d）

> 全 19 规则 actual ≤ baseline。compliance checker 经 `bash docs/audits/nop-compliance-checker.sh` 实测（2026-07-27 14:30，HEAD=8f53ed612）。

| 规则 | Baseline（M0.3） | Actual（HEAD=8f53ed612） | Delta | 状态 |
|------|------------------|---------------------------|-------|------|
| R1a/R1b/R1c | 0/0/0 | 0/0/0 | 0/0/0 | ✅ |
| R1d | 17 | 17 | 0 | ✅ |
| R2a | 37 | 37 | 0 | ✅ |
| R2b | 315 | 315 | 0 | ✅ |
| R2c | 1228 | 1226 | **-2** | ✅ improvement（P0-MA1-021 修复 + 部分代码清理） |
| R2d | 28 | 28 | 0 | ✅ |
| R3 | 5 | 5 | 0 | ✅ |
| R4 | 0 | 0 | 0 | ✅ |
| R5 | 0 | 0 | 0 | ✅ |
| R6 | 2 | 2 | 0 | ✅ |
| R7 | 0 | 0 | 0 | ✅ |
| R8 | 42 | 42 | 0 | ✅ |
| R10 | 6 | 6 | 0 | ✅ |
| R11 | 0 | 0 | 0 | ✅ |
| R12a（共享内核 ErpFinBusinessType） | 69 | 69 | 0 | ✅ 零扩张 |
| R12b（共享内核 PostingEvent） | 66 | 66 | 0 | ✅ 零扩张 |
| R12c（共享内核 AcctSchemaResolver） | 38 | 38 | 0 | ✅ 零扩张 |

**结论**：M0.3 锚点 → HEAD 期间，零规则上漂。R2c 合规改善 -2（基线维持 1228 不下调——为 successor 留 headroom，符合基线管理纪律"鼓励但不强制更新"）。

---

## Scope Matrix §2.1 架构治理行全域结论（12 ❓ → 补全）

> 12 ❓ 域经快速机械核查（daoFor/字典/共享内核/guard 四子维度）全部转为 ✅/⚠️(residual)。7 已 ⚠️ 域仅复核残留未回退。

| 域 | 状态 | 复核理由 |
|----|------|---------|
| finance | ⚠️(residual) | 首审 ⚠️F1residual 维持——daoFor Type 4 跨域写 successor（governed path 平台解耦）+ R12a 共享内核 69 跨域 import（已裁决登记）；本复审 R2b 用量未变 |
| mfg | ⚠️(residual→✅) | 首审 ⚠️F6✅ 升级为 ⚠️(residual)——F6 已闭包（ErpQaInspectionType 迁移完成），仅余 MrpReleaseService 跨域写豁免（已登记）+ Type 4 successor |
| hr | ✅ | 12 ❓ 域之一转 ✅——R2b=32/R2c 范围内、Type 4 写=0、共享内核 import=3（R12 范围内）、跨域只读 daoFor 9 文件（watch-only，无 P0/P1） |
| assets | ⚠️(P1) | 12 ❓ 域之一转 ⚠️(P1)——Type 4 写=0、共享内核 import=19（finance PostingEvent 派发消费，F4 已裁决登记）、跨域只读 daoFor 20 文件（与 P1-MA1-022 同根因，待 MR1 裁决方案 A/B） |
| pur | ⚠️(P1) | 12 ❓ 域之一转 ⚠️(P1)——Type 4 写=0、共享内核 import=6、跨域只读 daoFor 13 文件（P1-MA1-022） |
| sal | ⚠️(P1) | 12 ❓ 域之一转 ⚠️(P1)——Type 4 写=0、共享内核 import=6、跨域只读 daoFor 15 文件（P1-MA1-022） |
| qa | ⚠️(P1) | 12 ❓ 域之一转 ⚠️(P1)——Type 4 写=0、共享内核 import=3、跨域只读 daoFor 13 文件（NCR 跨域过账，P1-MA1-022） |
| crm | ✅ | 12 ❓ 域之一转 ✅——R2b=18/R2c 范围内、Type 4 写=0、共享内核 import=0（不消费 finance/master-data 共享类型）、跨域只读 daoFor 13 文件（dashboard 聚合，永久接受） |
| prj | ⚠️(P1) | 12 ❓ 域之一转 ⚠️(P1)——Type 4 写=0、共享内核 import=5、跨域只读 daoFor 11 文件（项目过账，P1-MA1-022） |
| cs | ✅ | 12 ❓ 域之一转 ✅——R2b=11、Type 4 写=0、共享内核 import=0、跨域只读 daoFor 7 文件（少量聚合） |
| ct | ⚠️(P1) | 12 ❓ 域之一转 ⚠️(P1)——**Type 4 写=4**（2 已登记 ErpCtRebateSettlementBizModel + 2 未登记 ErpCtInvoicePlanBizModel → P1-MA1-029）、共享内核 import=0、跨域只读 daoFor 4 文件 |
| b2b | ⚠️(residual→✅) | 首审 ⚠️F1half 升级为 ⚠️(residual)——F1 闭包项 #2 已完成（ErpB2bAsnBizModel 跨域写豁免已补登 posting-exemptions.md），残留仅为 successor 收敛条件（待 pur 提供 createFromAsn） |
| inv | ⚠️(P1) | 首审 ⚠️ 维持——**P0-MA1-021 已闭包**（plan 2026-07-27-1430-1）；R2c 改善 -2 部分源于此；跨域只读 daoFor 待 MR1（P1-MA1-022） |
| md | ⚠️(residual) | 首审 ⚠️F4✅ 维持为 ⚠️(residual)——DAG 根域 + 共享内核 AcctSchemaResolver 所在域（R12c=38）；F4 闭包完成 |
| mnt | ⚠️(P1) | 12 ❓ 域之一转 ⚠️(P1)——Type 4 写=0、共享内核 import=5、跨域只读 daoFor 6 文件（维修过账，P1-MA1-022） |
| drp | ⚠️(residual) | 首审 ⚠️F7✅ 维持为 ⚠️(residual)——F7 已闭包（4 ErpInvDrp* 命名例外登记）；命名 successor 待 drp 域重大 ORM 变更时顺带 |
| aps | ⚠️(P1) | 12 ❓ 域之一转 ⚠️(P1)——Type 4 写=0、共享内核 import=0、跨域只读 daoFor 3 文件（P1-MA1-022） |
| log | ✅ | 12 ❓ 域之一转 ✅——R2b=0、Type 4 写=0、共享内核 import=2、跨域只读 daoFor 1 文件（极简域） |
| notify | ✅ | 首审 ⚠️F5✅ 升级为 ✅——F5 已闭包（owner doc 创建完成），跨域通知派发子系统无治理漂移 |

**全域结论**：MA1 架构治理维度 19 列全部 ✅/⚠️（含 residual / P1 标注），**0 ❓ 列**。

---

## Findings 分级

### P0（即时通道）

**无 P0**。本复审期间未发现需要立即回滚或阻断发布的阻断性架构治理问题。

### P1（汇总交接 MR1）

| Finding ID | 描述 | 复审发现路径 | 目标 MR | 修复方式 |
|-----------|------|------------|--------|---------|
| `P1-MA1-029` | `ErpCtInvoicePlanBizModel`（contract→pur/sal）跨域写半治理——有 javadoc bypass rationale（line 41-45）但**未**登记 `posting-exemptions.md`。与首审 F1 闭包项 #2（ErpB2bAsnBizModel）同型。首审已识别（line 130/144）但闭包时仅补登 ErpB2bAsnBizModel | 实仓 grep `daoFor\(Erp[A-Za-z]+\.class\)\.saveEntity` 同行链式 | MR1 | 在 `posting-exemptions.md` 补登 `ErpCtInvoicePlanBizModel`（contract→pur/sal）豁免条目，含位置/触发场景/理由（避免服务依赖级联，与 ErpCtRebateSettlementBizModel 同源）/风险/补偿机制/收敛条件（待 pur/sal 提供 `createFromInvoicePlan` I\*Biz） |

> 已知残留（首审已裁决 deferred，**不重复登记**）：daoFor Type 4 跨域写/读残留（~10-30 处）+ governed path 平台解耦（F1 闭包项 #1 裁决分支 b）——维持 successor 状态（触发条件：nop-entropy 平台 lazy/SPI 解耦机制落地）。

### P2（watch-only）

| Finding ID | 描述 | 处置 |
|-----------|------|------|
| `P2-MA1-030` | `ErpMdCurrencyBizModel:60` 新增 `LocalDate.now()` 调用（programmer-error 路径，与 P2-MA1-019 同性质） | watch-only，MR1 顺手收敛或永久接受（与 P2-MA1-019 合并处理） |

---

## 残留风险

1. **daoFor Type 4 跨域写/读残留维持首审 deferred**——本复审实测 5 处同行链式跨域写（3 已登记豁免 + ErpCtRebateSettlementBizModel 2 行 + ErpCtInvoicePlanBizModel 2 行未登记），现状未恶化。successor 维持触发条件：nop-entropy 平台 lazy/SPI 解耦机制落地。
2. **compliance checker 注释排除校准的块注释残留风险**——R1d/R6/R10 三规则的注释行排除 grep 在块注释 `/* ... */` 跨行（无 `*` 续行前缀）场景下漏排除（首审残留风险，实测 0；successor 升级 AST）。
3. **R2c 基线 1228 vs actual 1226**——本复审未下调基线（保留 2 headroom 给 successor），符合"鼓励但不强制更新"纪律。后续 MR 修复若进一步降低 R2c，可在该 plan 内顺手下调基线至新精确值。
4. **A1.14 复审为快照**——本复审覆盖 2026-07-23 → 2026-07-27 窗口。后续若再引入密集跨域编排变更（如 MR1 修复或 MA2 业务审计），下一轮 MV 验证里程碑（V.2）将重跑 compliance checker 对照基线确认无漂移。

---

## 结论

nop-app-erp 的架构治理主脊（DAG 单向依赖 + `I*Biz` 写契约 + ORM 模型驱动生成纪律 + compliance checker CI guard）**经首审后 5 天密集变更依然健康**：

- F1–F9 残留**全部未回退**
- 新漂移扫描发现 **0 P0 + 1 P1**（ErpCtInvoicePlanBizModel 半治理，MR1 收敛）
- CI guard 19 规则 actual ≤ M0.3 基线锚点，R2c 实际下降 -2（合规改善）
- 共享内核 R12 三类型跨域 import 零增长（69/66/38 精确等于基线）
- scope matrix §2.1 架构治理行**全域 19 列补全，0 ❓**

**MA1 架构治理维度收尾，A1.14 推进至 done**。MA1 里程碑（A1.1–A1.14）全部 done。
