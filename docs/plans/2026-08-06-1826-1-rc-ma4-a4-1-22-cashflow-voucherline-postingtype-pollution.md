# 2026-08-06-1826-1 rc-ma4-a4-1-22-cashflow-voucherline-postingtype-pollution 现金流量表读 VoucherLine 不过滤 postingType 的 BUDGET/COMMITMENT 影子凭证污染运行时确认

> Plan Status: completed
> Last Reviewed: 2026-08-06
> Mission: requirement-compliance
> Work Item: A4.1.22（MA4 运行时行为验证 — A1.7 §7 存疑点 SP-1：UC-FIN-16 现金流量表 `buildCashFlowDataset` 读 `ErpFinVoucherLine` 不过滤 voucher.`postingType`，BUDGET/COMMITMENT 影子凭证若含现金科目[1001/1002/1012/1031]行是否污染现金流量表）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.1.22；存疑点来源 `docs/audits/2026-08-02-2115-rc-ma1-a1-7-finance-f7-reports-dashboards-multischema.md` §7 SP-1
> Related: `docs/plans/2026-08-07-0300-3-rc-ma4-a4-1-finance-runtime-expander.md`（A4.1 展开器 done）、`docs/plans/2026-08-06-0847-2-rc-ma4-a4-1-5-commitment-trial-balance-exposure.md`（A1.2 caveat ③ 交叉项 — 承付试算平衡暴露，已 done）、`docs/audits/2026-08-02-2115-rc-ma1-a1-7-finance-f7-reports-dashboards-multischema.md`（A1.7 报告 §2.2 现金流量数据集查询链 + §5.2 caveat ③ 收口 + §7 SP-1）、`docs/design/finance/use-cases.md:318`（UC-FIN-16 L1）
> Audit: required

## Current Baseline

> 本计划是**运行时行为验证**（verification or audit work），结果表面 = 一份 A4.1.22 验证报告（落盘 `docs/audits/2026-08-06-1826-rc-ma4-a4-1-22-cashflow-voucherline-postingtype-pollution.md`）+ 必要时 arm-index finding 注记更新。**不改代码/ORM/api.xml/真相源**（只读评估：读 `buildCashFlowDataset:299-323` + `loadPostedVoucherLines:424-439` + `isCashSubjectCode:549-553` + BUDGET/COMMITMENT 投递 Provider 的 createFacts 科目分布 + seed 凭证普查 + `app-erp-finance.orm.xml:1740-1742` 范式注记）。范式对齐 A4.1.21（done — period-close 运行时行为评估同型工作项）+ A4.1.5（done — 承付试算平衡暴露同 caveat ③ 交叉项）。

- **存疑点原文**（A1.7 报告 §7 SP-1，`2026-08-02-2115-...-a1-7-...md` §7）：「cash flow 读 VoucherLine 不过滤 postingType，BUDGET/COMMITMENT 影子凭证是否含现金科目（1001/1002/1012/1031）行 → 现金流量表是否被影子凭证污染」。静态状态：BUDGET/COMMITMENT 实操不触现金科目（预算/承付追踪费用/AP 科目），低风险；但代码无显式守卫。MA4 运行时确认方式：seed 含 BUDGET postingType + 现金科目行的凭证，跑 `buildCashFlowDataset` 断言是否计入。

- **关联既有 finding**：
  - **caveat ③**（A1.2 §7 + A1.7 §5.2 caveat ③ 收口）：A1.2 budget 切片登记 caveat ③「COMMITMENT 影子凭证 header 借贷不经 assertBalanced；交 A1.7 复核是否破坏试算平衡/三表恒等式」。A1.7 §2.2/§5.2 收口结论：**BS/IS 安全**（`orm.xml:1740-1742` 注记「过账引擎本就不维护 ErpFinGlBalance，故预算不引入 GlBalance 结构变更」+ BUDGET/COMMITMENT 不入 GlBalance）；**cash flow 读 VoucherLine 不过滤 postingType** — 理论上可含 BUDGET/COMMITMENT 现金行，但实操不触现金科目 → 低风险登记 §7 SP-1 供 MA4 运行时确认。A4.1.5（done — `2026-08-06-0847-2-...-a4-1-5-commitment-trial-balance-exposure.md`）已确认承付凭证 header 借贷不平时试算平衡暴露面，本验证补 cash flow 维度差异。

- **需求契约（L1 权威）**：`docs/design/finance/use-cases.md:318` UC-FIN-16 财务三大报表。验收标准⑧「现金流量表按经营/投资/筹资分类」+ caveat ③ 交叉（BUDGET/COMMITMENT 过滤）。L1 未显式要求 cash flow 排除影子凭证，但「现金流量表反映真实现金收支」语义隐含 postingType=ACTUAL（实际凭证）口径。

- **实现现状（L3，实测锚点，本计划起草时 live repo 核实，全在 module-finance/erp-fin-service）**：
  - 现金流量表数据集（`ErpFinReportBizModel.java:299-323`）：`buildCashFlowDataset(periodId)` → `loadPostedVoucherLines(periodId):302` 取已过账凭证行 → 逐行 `if (!isCashSubjectCode(l.getSubjectCode())) continue;:305`（仅按科目代码前缀过滤现金科目）→ `r.put("section", "OPERATING");:314`（硬编码 OPERATING 分类，关联 P1-RC-007）→ 计入 rows。**关键缺口**：`loadPostedVoucherLines` 不按 voucher.postingType 过滤。
  - 已过账凭证行加载（`loadPostedVoucherLines:424-439`）：`vq.addFilter(eq("docStatus", VOUCHER_STATUS_POSTED));:427` + `eq("periodId", periodId);:429` + `applyOrgAndSchemaScope(vq, periodId);:430`（orgId + 主账套 scope，R1.29 fix）→ 取 voucherIds → `lq.addFilter(in("voucherId", voucherIds));:437` 加载 VoucherLine。**全程无 postingType 过滤** —— BUDGET/COMMITMENT postingType 的已过账凭证的行同样被加载。
  - 现金科目判定（`isCashSubjectCode:549-553`）：`code.startsWith("1001")||"1002"||"1012"||"1031"`（库存现金/银行存款/其他货币资金/存出保证金）。若 BUDGET/COMMITMENT 凭证行命中这些前缀 → 计入现金流量表。
  - **postingType 字段归属**（`app-erp-finance.orm.xml`）：`postingType` 列在 **ErpFinVoucher 头**（`:418`，凭证级，非行级）；VoucherLine 实体**无 postingType 字段**（grep `ErpFinVoucherLine.java` 零命中）。故过滤须在 voucher 头侧（`loadPostedVoucherLines` 的 vq 查询），非行侧。
  - **BUDGET/COMMITMENT 影子凭证范式**（`app-erp-finance.orm.xml:1740-1742` 注记）：「预算作为 postingType=BUDGET 的『影子凭证』与实际凭证并行入账，复用凭证引擎。预算余额/实际余额/可用余额统一从 ErpFinVoucherLine 按关联凭证 postingType 聚合（派生，不落库）」—— 证实影子凭证确实过 POSTED 状态且经同一 VoucherLine 表，与 ACTUAL 凭证行同表混存。

- **既有证据（复用输入）**：
  - A1.7 §2.2（`:121`）：BS/IS 数据源 GlBalance 无 postingType 过滤但 BUDGET/COMMITMENT 不入 GlBalance 故安全；cash flow 读 VoucherLine 不过滤 postingType 但实操不触现金科目（静态推断）。
  - A4.1.5（done — `2026-08-06-0847-2-...-a4-1-5-commitment-trial-balance-exposure.md`）：承付凭证 header 借贷不平时试算平衡暴露面已确认（BS/IS 安全 + cash flow 低风险登记 SP-1）。
  - A1.2 caveat ③ 收口（A1.7 §5.2）：试算平衡/三表恒等式不被破坏（BS/IS 维度）。

- **剩余差距**：cash flow 读 VoucherLine 不过滤 voucher.postingType 的**运行时实际污染面**未确认 —— ①BUDGET/COMMITMENT 影子凭证的 createFacts 投递的科目分布是否包含 1001/1002/1012/1031 现金科目（静态推断「不触」未运行时证实）；②seed/生产凭证普查是否存在 postingType∈{BUDGET,COMMITMENT} 且 VoucherLine.subjectCode 命中现金前缀的凭证；③若存在，`buildCashFlowDataset` 是否将其计入（污染现金流量表）。本验证闭合 caveat ③ 在 cash flow 维度的运行时污染裁决。

- **保护区域**：只读评估（读 buildCashFlowDataset + loadPostedVoucherLines + isCashSubjectCode + BUDGET/COMMITMENT Provider createFacts 科目分布 + seed 凭证普查 + orm.xml 范式注记），不触及 ORM/会计过账逻辑**修改**。属 roadmap 预授权类目（只读评估）。本验证**不实施修复**（若发现污染，登记 finding 归 MR1；修复触及 buildCashFlowDataset/loadPostedVoucherLines 加 postingType 过滤属 BizModel 代码逻辑[预授权自动执行]，不触 §5 ask-first — 非会计过账核心路径，仅报表读侧过滤）。

## Goals

- `buildCashFlowDataset` + `loadPostedVoucherLines` 读取链核验：给出 `ErpFinReportBizModel.java:299-323`（buildCashFlowDataset → loadPostedVoucherLines → isCashSubjectCode 现金科目过滤 + :314 硬编码 OPERATING）+ `loadPostedVoucherLines:424-439`（docStatus=POSTED + periodId + orgAndSchemaScope，**无 postingType 过滤**）+ `isCashSubjectCode:549-553`（1001/1002/1012/1031 前缀）证据（file:line）。证实 cash flow 读路径不区分 ACTUAL/BUDGET/COMMITMENT 凭证。
- BUDGET/COMMITMENT 影子凭证科目分布核验（本存疑点核心）：核验 BUDGET/COMMITMENT postingType 影子凭证的 createFacts 投递科目是否包含现金科目（1001/1002/1012/1031）—— 普查 budget/commitment Provider 的 createFacts 实现 + creditSubject/debitSubject 配置 + orm.xml 范式注记 `:1740-1742`。确认静态推断「BUDGET/COMMITMENT 追踪费用/AP 科目，不触现金科目」在运行时是否成立。
- seed/生产凭证普查：grep 是否存在 postingType∈{BUDGET,COMMITMENT} 且 VoucherLine.subjectCode 命中 1001/1002/1012/1031 前缀的凭证（seed 数据 + 测试夹具）。若存在 → 运行 `buildCashFlowDataset` 断言是否计入（污染）；若不存在 → 确认实操无污染（低风险成立）。
- postingType 字段归属确认：核验 `postingType` 在 ErpFinVoucher 头（orm.xml:418）非 VoucherLine 行（实体无该字段）—— 确认过滤须在 voucher 头侧 vq 查询，非行侧 lq 查询，为修复方向（若登记 finding）提供准确控制点。
- 对齐 caveat ③（A1.2 + A1.7 §5.2）+ §2 判据给出 cash flow 维度运行时裁决：①若 BUDGET/COMMITMENT 影子凭证实操不触现金科目（createFacts 科目分布无现金前缀 + seed 无污染凭证）→ 维持 caveat ③ 接受[cash flow 低风险]，登记 P2 watch-only（代码无显式守卫，未来定制可能引入现金科目影子凭证）或维持接受无新 finding；②若存在 BUDGET/COMMITMENT 现金科目凭证且 buildCashFlowDataset 计入 → 登记 P1（现金流量表污染，归 MR1，§2 P1① 主路径数值正确性）。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 caveat ③[BS/IS 安全]分层一致。
- 产出验证报告 + §8 过程纪律自检。

## Non-Goals

- **不修复 cash flow postingType 过滤**（若发现污染，登记 finding 归 MR1；修复 = buildCashFlowDataset/loadPostedVoucherLines 加 postingType=ACTUAL 过滤属 BizModel 代码逻辑，预授权自动执行，不触 §5 ask-first）。
- **不修改代码/ORM/api.xml/BizModel/真相源**（只读评估）。
- **不重新核实 UC-FIN-16 全部验收标准**（A1.7 §5 已判 ⑧ P1[P1-RC-007 现金流量分类缺失] + ⑨ caveat ③ 收口[BS/IS 安全] + CLOSED 门控 P2[P2-RC-008]；本验证只评 caveat ③ cash flow 维度的运行时污染差异）。
- **不裁决 P1-RC-007**（现金流量经营/投资/筹资三分类 + 间接法缺失，:314 硬编码 OPERATING — 本验证仅记录该 finding 关联，不重新裁决）。
- **不展开 A1.7 §7 SP-2/SP-3/SP-4**（多账套渲染 / CLOSED 门控 / 看板行级权限，独立工作项 A4.1.23/A4.1.24/A4.1.25）。
- **不实际执行多组织/多账套运行时重现**（只读 createFacts 科目分布 + seed 凭证普查 + loadPostedVoucherLines 读取链推理；真实影子凭证注入重现属 MR1 修复验证范围，非本验证范围）。

## Task Route

- Type: `verification or audit work`（现金流量表 postingType 过滤缺失的 BUDGET/COMMITMENT 影子凭证污染运行时确认 + caveat ③ cash flow 维度裁决）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §4 Q1 真相源层级 + §7 衔接 + §8 自检 + §9 冻结 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.1.22 行）+ `docs/audits/2026-08-02-2115-rc-ma1-a1-7-finance-f7-reports-dashboards-multischema.md` §7 SP-1 + §2.2 cash flow 数据集查询链 + §5.2 caveat ③ 收口（输入）+ `docs/design/finance/use-cases.md:318`（UC-FIN-16 L1）。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。cash flow postingType 污染评估需多维度归类（读取链 / isCashSubjectCode 现金科目分布 / BUDGET-COMMITMENT Provider createFacts 科目分布 / postingType 字段归属[voucher 头 vs voucherLine 行] / seed 凭证普查 / caveat ③ cash flow 维度裁决 / MA4↔A5.6 边界）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。只读评估（读 buildCashFlowDataset + loadPostedVoucherLines + isCashSubjectCode + BUDGET/COMMITMENT Provider createFacts 科目分布 + seed 凭证普查 + orm.xml 范式注记）。§8 自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - cash flow postingType 污染面评估

Status: completed
Targets: `docs/audits/2026-08-06-1826-rc-ma4-a4-1-22-cashflow-voucherline-postingtype-pollution.md`（验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: A4.1 done（展开器已追加 A4.1.22 行）；A1.7 done（§7 SP-1 已落盘 + §2.2 cash flow 数据集查询链 + §5.2 caveat ③ 收口）

- [x] `Proof` 读取链核验：给出 `ErpFinReportBizModel.java:299-323`（buildCashFlowDataset → `loadPostedVoucherLines:302` → `isCashSubjectCode:305` 现金科目过滤 + `:314` 硬编码 OPERATING）+ `loadPostedVoucherLines:424-439`（`docStatus=POSTED:427` + `periodId:429` + `applyOrgAndSchemaScope:430`，**无 postingType 过滤**）+ `isCashSubjectCode:549-553`（1001/1002/1012/1031 前缀）证据（file:line）。证实 cash flow 读路径不区分 ACTUAL/BUDGET/COMMITMENT 凭证。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` BUDGET/COMMITMENT 影子凭证科目分布核验（本存疑点核心）：核验 BUDGET/COMMITMENT postingType 影子凭证的 createFacts 投递科目是否包含现金科目（1001/1002/1012/1031）—— 普查 budget/commitment Provider 的 createFacts 实现 + creditSubject/debitSubject 配置 + orm.xml 范式注记 `:1740-1742`（影子凭证经同一 VoucherLine 表与 ACTUAL 混存）。确认静态推断「BUDGET/COMMITMENT 追踪费用/AP 科目，不触现金科目」在运行时是否成立。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` postingType 字段归属确认：给出 `postingType` 在 ErpFinVoucher 头（`app-erp-finance.orm.xml:418`）非 VoucherLine 行（`ErpFinVoucherLine.java` grep 零命中）证据。确认过滤须在 voucher 头侧 vq 查询（`loadPostedVoucherLines:426-431`），非行侧 lq 查询（`:436-438`），为修复方向（若登记 finding）提供准确控制点。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` seed/生产凭证普查：grep 是否存在 `postingType`∈{BUDGET,COMMITMENT} 且 VoucherLine.subjectCode 命中 1001/1002/1012/1031 前缀的凭证（seed 数据 + 测试夹具 + createFacts 投递的科目分布）。若存在 → 评估 `buildCashFlowDataset` 是否计入（污染）；若不存在 → 确认实操无污染（低风险成立）。产出凭证普查清单 + 科目分布矩阵。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` MA4↔A5.6 边界声明：本验证审「行为是否符合需求」（cash flow 是否被影子凭证污染），与 A5.6 审「E2E 断言强度」边界按此执行。不重做 A5.6 E2E 断言强度审计。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Decision` caveat ③ cash flow 维度运行时裁决（方法论 §2 判据 + 三源对照）：①若 BUDGET/COMMITMENT 影子凭证实操不触现金科目（createFacts 科目分布无现金前缀 + seed 无污染凭证）→ 维持 caveat ③ 接受[cash flow 低风险]，登记 **P2 watch-only**（代码无显式守卫，未来定制可能引入现金科目影子凭证）或维持接受无新 finding；②若存在 BUDGET/COMMITMENT 现金科目凭证且 buildCashFlowDataset 计入 → 登记 **P1**（现金流量表污染，归 MR1，§2 P1① 主路径数值正确性）。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 caveat ③[BS/IS 安全]分层一致（cash flow 维度是 caveat ③ 之外的读路径差异，不撤销 BS/IS 安全结论）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 读取链 + 影子凭证科目分布 + postingType 归属 + seed 凭证普查证据落盘（全集，无遗漏），每条有证据（file:line）
- [x] caveat ③ cash flow 维度运行时裁决有明确结论（维持接受/P2 watch-only 或 P1 MR1），与 A1.7 §5.2 caveat ③[BS/IS 安全]分层一致

### Phase 2 - finding 衔接 + §8 自检 + 报告定稿

Status: completed
Targets: `docs/audits/2026-08-06-1826-rc-ma4-a4-1-22-cashflow-voucherline-postingtype-pollution.md`（定稿）；`docs/audits/arm-index.md`（新 finding 或注记，若有）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 1 cash flow postingType 污染面评估 + 运行时裁决完成

- [x] `Add` finding/注记更新：若 P2 watch-only → 新建 finding（P2-RC-xxx，cash flow postingType 过滤缺失 watch-only，与 caveat ③ 不同维度[读路径过滤 vs 试算平衡]）；若 P1 → 新建 finding（P1-RC-xxx，归 MR1）；若维持接受无新 finding（实操不触现金科目）→ 在 arm-index caveat ③ 相关行追加 cash flow 维度注记。禁止未经比对新建重复 finding（grep arm-index 同域同控制点后裁决，确认与 caveat ③/P1-RC-007 不同控制点）。
      - Skill: none
- [x] `Proof` §8 过程纪律自检：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明（与 A1.7 §2.2/§5.2 caveat ③ / A4.1.5 承付试算平衡暴露 的复用关系 + MA4↔A5.6 边界）。不以 checker 退出码 0 作为门控依据。
      - Skill: none

Exit Criteria:

- [x] 验证报告定稿（读取链 + 影子凭证科目分布 + postingType 归属 + seed 凭证普查 + 运行时裁决 + finding 衔接 + §8 自检齐全）
- [x] 新 finding 或注记已登记入 arm-index（若有变更）或有明确「维持接受无变更」记录并有 grep 依据

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is（独立子代理 ses_0295ffe2affeMHop7AIjkVFrq4，新会话不重用执行者上下文）— 全 12 checklist 项 PASS，零信任核对 live code（buildCashFlowDataset:299-323 / loadPostedVoucherLines:424-439 无 postingType 过滤 / isCashSubjectCode:549-553[1001/1002/1012/1031] / postingType 在 ErpFinVoucher 头 orm.xml:418 非 VoucherLine[entity grep 零命中，orm.xml:1620 属 ErpFinPostingException] / orm.xml:1740-1742 影子凭证范式注记 / A1.7 §7 SP-1:286 逐字 / arm-index caveat ③ + P1-RC-091 自承「同根因家族不同控制点」确认）零漂移；单一结果表面；anti-slack 零命中；item typing 合规（Proof/Decision/Add 无 Fix）；Deps 门控满足（A4.1 expander done + A1.7 done）；保护区域纪律（只读 + 修复归 MR1 BizModel 读侧过滤预授权非 ask-first）；方法学对齐（§2/§4/§7/§8 + Decision 双分支开放）；dedup 可辩护（caveat ③[P1-RC-091] + P1-RC-007 不同控制点）。2 non-blocking Minors（M1 Decision 分支①「P2 watch-only 或维持接受」未预指判别式——执行时按证据裁决；M2 Deferred successor「可选」措辞属范围外非 slack）。promote to active。

## Closure Gates

> 本计划为**只读 cash flow postingType 污染面评估**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 读取链 + 影子凭证科目分布 + postingType 归属 + seed 凭证普查 + 运行时裁决 + finding 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A4.1.22 验证报告读取链 + 影子凭证科目分布 + postingType 归属 + seed 凭证普查 + 运行时裁决齐全 + finding/注记更新（若有）
- [x] 相关文档对齐：报告与方法论 §MA4 + §2 判据 + §4 Q1 + §去重协议一致；与 A1.7 §7 SP-1 + §2.2 + §5.2 caveat ③ 一致
- [x] 已运行验证：读取链 + 影子凭证科目分布 + §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up（若登记 finding 是验证**输出**，非范围内项目降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项保留为未勾选状态作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### cash flow postingType 过滤修复（若 A4.1.22 登记 finding 后修复归口）

- Classification: `out-of-scope improvement`（本验证是污染面评估，修复归 MR1）
- Why Not Blocking Closure: 本计划是污染面评估，结果表面 = 验证报告 + finding/注记登记。修复（若有）归 MR1（R1.0→RC-R1.n），修复 = buildCashFlowDataset/loadPostedVoucherLines 加 postingType=ACTUAL 过滤属 BizModel 代码逻辑[报表读侧过滤]，预授权自动执行，**不触 §5 ask-first**（非会计过账核心路径）。本验证闭环不阻塞于修复落地（finding 是验证**输出**，非范围内项目降级）。
- Successor Required: yes（MR1 R1.0 展开器读取本报告 finding[若有] → RC-R1.n 修复，按报告裁决方向：①实操不触现金科目→owner doc 标注边界 + 可选 buildCashFlowDataset 加 ACTUAL 过滤守卫；②存在污染→loadPostedVoucherLines vq 查询加 postingType filter）

## Closure

Status Note: 执行者已完成全计划（Phase 1 cash flow postingType 污染面评估 + Phase 2 finding 衔接 + §8 自检 + 报告定稿）。**只读评估，零生产代码变更**（无 ORM/会计过账/真相源修改），故 build/test/lint 门控按 plan §Closure Gates 删除（验证 = 读取链 + 影子凭证科目分布 + postingType 归属 + seed 凭证普查 + 运行时裁决 + finding 衔接 + §8 checker + 独立结束审计）。核心裁决：存疑点 A1.7 §7 SP-1「现金流量表是否被 BUDGET/COMMITMENT 影子凭证污染」答案 = **否（当前运行时）**——`loadPostedVoucherLines:424-439` 确实不过滤 postingType，但 BUDGET/COMMITMENT 影子凭证科目分布不含现金前缀（6601/6001/1408）+ seed/生产普查零污染凭证 + COMMITMENT config 默认关闭 → 维持 caveat ③ 接受[cash flow 低风险成立]，新建 **P2-RC-085 watch-only**（代码无显式守卫，未来定制潜在污染风险，归 MR1 successor）。两 Phase `Status: completed`、所有 Phase 项与 Exit Criteria 已勾选、Plan Status `completed`、roadmap A4.1.22 行翻转 `todo → done ✅`、arm-index 增 P2-RC-085 行 + RC 交叉引用注记。本计划无 `> Source Audits:` 行（roadmap 源生计划），关闭 source audits 步骤跳过。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计由独立子代理（新会话 `ses_029501e79ffeZxLgZAvZeC3MU4`，fresh session，未执行本计划）执行。**Verdict: pass**（10/10 checks PASS，无 blocker）。关键独立零信任核对：(1) 读取链 `ErpFinReportBizModel.java:299-323/424-439/549-553`（loadPostedVoucherLines 无 postingType 过滤 + isCashSubjectCode 1001/1002/1012/1031 + :314 硬编码 OPERATING）；(2) postingType 归属 orm.xml:418[ErpFinVoucher 头]/:1620[ErpFinPostingException] + ErpFinVoucherLine.java 零命中 + orm.xml:1740-1742 范式注记；(3) BudgetVoucherGenerator 取 ErpFinBudgetLine.subjectId 无科目大类守卫；(4) CommitmentVoucherGenerator subject 来自 config[1408] + isCommitmentEnabled 默认 FALSE；(5) seed 普查 BUDGET 凭证 ID=12 行=6601+6001 非 1001，1001 全属 NORMAL 凭证 + budget_line 全 6601/6001 + COMMITMENT 凭证用 1408；(6) 裁决逻辑 §2 P2① 正确 + 与 P1-RC-091[试算平衡]/P1-RC-007[分类]不同控制点去重 §7.4；(7) arm-index P2-RC-085 行 7 列对齐 + 编号无碰撞（P2-RC-084 之后）+ 交叉引用注记位置正确；(8) 计划内部一致性 Plan Status completed / 双 Phase completed / 全 `[x]`；(9) §8 checker 记录 + 无代码变更/无回归声明 + 独立性声明 + 交叉去重声明齐全；(10) 真相源冻结——git status 仅 docs/audits/+docs/backlog/+docs/plans/ 变更，未触 module-*/docs/design//docs/requirements/。结束审计门控（plan Closure Gates）由本独立裁决满足。

Follow-up:

- MR1 修复 cash flow postingType 过滤（若登记 finding）：BizModel 代码逻辑预授权自动执行，不触 ask-first
