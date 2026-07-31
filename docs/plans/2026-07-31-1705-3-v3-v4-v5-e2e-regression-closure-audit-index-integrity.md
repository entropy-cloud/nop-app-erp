# 2026-07-31-1705-3-v3-v4-v5-e2e-regression-closure-audit-index-integrity 行为回归 + 独立闭包审计 + 索引完整性

> Plan Status: completed
> Last Reviewed: 2026-07-31
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MV 工作项 V.3 + V.4 + V.5（todo，依赖 V.1/V.2 绿基线，由 plan `-1705-2` 提供）
> Related: plan `2026-07-31-1705-2-...`（前置：V.1 全量绿 + V.2 compliance 零漂移）；plan `2026-07-31-1705-1-...`（R4.1 裁决）；本计划是 MV 里程碑的收尾结果表面
> Audit: required

## Current Baseline

- **roadmap 状态**：V.3/V.4/V.5 均 todo，依赖 V.1+V.2 done（`-1705-2`）。本计划是 MV 里程碑的最后结果表面（行为回归 + 闭包 + 可追溯）。
- **V.3 E2E 已知基线**（`docs/testing/known-good-baselines.md`，最近全量 2026-07-25）：
  - 全套件（非 visual）：`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test --workers=1 tests/e2e/{business-actions,crud,dashboards,examples,orchestration,pages,reports}` → **490 passed / 1 failed / 3 skipped，~1.0h**。
  - 1 failed = `master-data.write.amis`（AMIS form-button selectOption↔switch test-infra Non-Goal，非产品缺陷，`crud/_helper.ts:174`，自 2026-07-12 悬挂，known-good-baselines §Known Failures (Accepted) 已登记）。
  - visual/ 像素快照套件**不在本基线**（归 plan 2026-07-17-2010-2），V.3 Non-Goal。
  - 自 2026-07-25 基线后 MR1/MR2/MR3/MR5 修复未做 E2E 回归确认；A5.6 报告裁决 258 spec 断言强度矩阵（强 66.3%/中 8.5%/弱 21.3%/工具 2.7%）。
- **V.4 closure audit 范围**：roadmap V.4 = 「独立子代理 closure audit（全部 P0 + 关键 P1 修复）」。R3.5（plan `-1439-1`）已完成 Round 3 closure audit（14 份 closure-pending 计划，全 PASS）。V.4 是**审计-修复 mission 级**闭包审计——对全部 P0（即时通道）+ 关键 P1（MR1/MR2/MR3/MR5 主修复）做最终独立复核。skill = `docs/skills/closure-audit-prompt.md`。
- **V.5 索引完整性**：roadmap V.5 = 「审计报告索引完整性校验（所有 P0/P1 可追溯到修复或 deferred）」。arm-index §P0 发现追踪 + §P1 详细清单 + §跨维度发现 需与实际修复/deferred 状态双向一致。R3.0/R3.5 已做部分回填；V.5 是最终全量可追溯复核。
- **剩余差距**：MR 修复后 E2E 未回归确认；mission 级 closure audit 未做；arm-index 全量可追溯未做最终复核。

## Goals

- **V.3**：跑 E2E 抽样回归（覆盖 MR1/MR2/MR3/MR5 实际触及的业务链路域），确认 MR 修复未引入行为回归；`master-data.write.amis` 维持已知 Non-Goal 1-failed 基线。
- **V.4**：由独立子代理（新会话，不重用执行者上下文）对全部 P0 + 关键 P1 修复跑 mission 级 closure audit，产出闭包裁决（PASS / 需补强）。
- **V.5**：全量复核 arm-index 中所有 P0/P1 发现可追溯到「已修复」或「显式 deferred + successor」，无裸 todo。
- 三项全过后，MV 里程碑可整体收口，解除 MG（G.1–G.4 知识沉淀）的阻塞。

## Non-Goals

- 不做 V.1（构建/测试）和 V.2（compliance）——归 `-1705-2`。
- 不做 visual/ 像素快照套件回归（非本基线，归独立 plan）。
- 不做 MG 知识沉淀（G.1–G.4，依赖 MV 全 done，下一轮起草）。
- 不重写既有测试（A5.6/R3.2 已裁决 E2E 增强 successor；V.3 只回归不改写）。
- V.4 不替代各 MR 计划自有的 plan-level closure audit（R3.5 Round 3 已做）；V.4 是 mission 级聚合复核。

## Task Route

- Type: `verification or audit work`（V.3 回归 + V.4/V.5 审计复核）
- Owner Docs: `docs/testing/known-good-baselines.md`（V.3）；`docs/audits/arm-index.md`（V.4/V.5）；`docs/skills/closure-audit-prompt.md`（V.4 方法）
- Skill Selection Basis: V.3 无技能（机械回归）；V.4 显式 `closure-audit-prompt.md`（roadmap V.4 指派，独立子代理闭包审计方法）；V.5 无技能（索引双向一致性机械复核）。

## Infrastructure And Config Prereqs

- V.3 需本地运行应用：`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`（fresh-DB 种子 + 端口 8011，见 known-good-baselines 基线条目「_tmp-server.sh restart」机制 + playwright.config.ts webServer JVM args 同步）。需 V.1 的 runner jar 构建产物。
- V.4/V.5 无额外 infra（只读复核 + 审计）。

## Execution Plan

### Phase 1 - V.3 E2E 抽样回归

Status: completed
Targets: `tests/e2e/{business-actions,orchestration,dashboards,reports,crud}/`
Skill: none

- Item Types: `Proof`
- Prereqs: V.1 绿构建产物（runner jar）+ V.2 done

- [x] **Decision: 抽样范围**。基于 MR1/MR2/MR3/MR5 实际触及域（finance/purchase/sales/inventory/manufacturing/assets/hr/quality 等）选定代表性抽样 spec 集——优先 business-actions + orchestration 链路 spec（断言强度强/中档）+ 各域 CRUD smoke + dashboards/reports value spec。记录抽样清单与理由（覆盖 MR 触及域，非全量 490）。
  - Skill: none
  - **抽样清单（125 spec，覆盖 MR 触及域，非全量 490）**：orchestration 7（p2p/o2c chain + reverse + reverseApprove + mfg-chain，强断言覆盖 P2P/O2C 过账链 + 红冲回写）+ business-actions 95（finance 13 凭证/期间/FX/坏账/预算/对账/合并抵消；inventory 4 成本/移动/冲销；mfg 3 工单/差异/领料红冲；quality 8 含 P0-MA2-017 守卫 NCR resolve/scrap/reverse/return；assets 3 折旧/价值调整/CIP；hr 3 薪酬/仿真/考勤；sales/purchase 2 退货；M5 4 notify-inbox/mnt 过账红冲）+ CRUD list-value 9（finance/inventory/mfg/master-data/purchase/sales/quality/assets/hr）+ dashboards value 7 + reports value 6。理由：覆盖全部 MR1/2/3/5 触及域 + 强/中断言档，非全量。
- [x] fresh-DB 启动（`_tmp-server.sh restart` + 与 playwright.config.ts webServer 同步的 4 键 JVM args：ap-subject-code=2202 / exchange-gain-loss-subject-code=6603 / current-year-profit-subject-code=4103 / auto-depreciation-on-close=false）后跑抽样 spec：`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test --workers=1 <抽样 spec 集>`
  - fresh-DB 启动成功（server ready after 6s）；抽样分 4 批跑完。
- [x] **Proof**: 抽样 passed/failed 记录；`master-data.write.amis` 若在抽样内则维持已知 Non-Goal 1-failed（不视为回归）；抽样内其余 0 回归。若发现新失败，登记为 P0/P1 按即时通道或 MR 纪律处置，不静默降级。
  - **结果：125 passed / 0 failed**（master-data.write.amis 已知 Non-Goal 不在抽样内）。
  - **执行期发现并处置 4 项新失败**（quality-ncr-scrap-posting / quality-ncr-reverse-voucher-line / quality-ncr-return-disposition ×2）：根因为**测试层契约对齐滞后**——MR1 P0-MA2-017 修复增强了 NCR `resolve` 门控（无 CAPA 措施时须显式提供 `noCapaReason`），JUnit 层 `TestErpQaNcrCapaEndToEnd` 已更新，但 4 个 E2E browser-layer spec 仍按旧契约（无 `noCapaReason`）调用 `resolve`。**非产品缺陷**（产品行为正确且有意，由 JUnit 背书）。**处置**：最小测试层契约对齐修复——4 spec 的 `resolve` 调用补 `noCapaReason` 参数（零生产代码/契约/ORM 变更），重跑 4 passed。`mvn clean install -DskipTests` BUILD SUCCESS（154 模块）。

Exit Criteria:

> 本阶段交付 E2E 抽样无回归证据（已知 Non-Goal 除外）。

- [x] 抽样 spec 集 0 回归（`master-data.write.amis` 已知 Non-Goal 1-failed 除外），抽样覆盖 MR 触及域的理由已记录

### Phase 2 - V.4 独立子代理 mission 级 closure audit

Status: completed
Targets: `docs/audits/arm-index.md` §P0 + §P1 详细清单；各 MR 修复计划
Skill: `docs/skills/closure-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: Phase 1（V.3 无回归）+ V.1/V.2 done

- [x] **独立子代理 closure audit**（**必须新会话、不重用执行者上下文**，遵循 `00-plan-authoring-and-execution-guide.md` 结束时规则 6 + 12）：对「全部 P0（即时通道修复）+ 关键 P1（MR1 R1.1–R1.29 / MR2 R2.1–R2.15 / MR3 R3.1–R3.7 / MR5 R5.1–R5.8 主修复）」抽样复核——每项验证 (a) 修复实际落地于实时仓库 (b) 测试/证据存在 (c) owner-doc 对齐 (d) 无 hollow closure（接口存在≠行为完整）。产出 mission 级闭包裁决。
  - Skill: `docs/skills/closure-audit-prompt.md`
  - 独立子代理 task id = `ses_048159890ffekeMzdnaXzHeRO0`（fresh session, cold context, read-only, 检查 ~40 实时文件）。
- [x] **Proof**: closure audit 报告（独立子代理 task id + 抽样清单 + PASS/需补强裁决）。若「需补强」，登记具体项并按其严重度处置（P0 即时通道 / P1 successor plan），不静默降级。
  - **裁决：passes closure audit**。
  - **P0（6 项全量）**：P0-MA1-021 CLOSED（CostAdjustmentPostingDispatcher 经 IErpFinVoucherBiz.post/reverse，无直接 voucherDao 写）；P0-MA2-016 CLOSED（ProfitLossClosingService:91-93 仅排除 PERIOD_CLOSE，EXCHANGE_GAIN_LOSS 不再排除）；P0-MA2-017 CLOSED（passInspection/failInspection 守卫+posted+NCR 触发；resolve noCapaReason 门控；TestErpQaNcrCapaEndToEnd 背书）；P0-MA2-018 DEFERRED-OK（显式 adjudication：红冲同键/多账套/软删除三重冲突，plan status=deferred + AGENTS rule 12 + 方向 A/B/C/D 待人工裁决）；P0-MA2-019 CLOSED（ErpApsCapacityReservation 实体 + UK_APS_CAPACITY_RESERVATION_SLOT + hasOverlappingReservation 预检）；P0-MA2-020 CLOSED（UK_INV_STOCK_BALANCE_NATURAL 含 orgId，A2.18 复核维持）。**5 CLOSED + 1 DEFERRED-OK，零 HOLLOW**。
  - **P1（9 项抽样跨 MR1/MR2/MR3/MR5）**：MR1 R1.9 多币种（VoucherFact amountSource/amountFunctional + 2 测试）/R1.16 过账悬挂告警（IErpSysNotificationBiz 10+ dispatcher + 4 测试）/R1.29 多公司 orgId（ErpOrgIsolationQueryTransformer）；MR2 R2.15 view.xml drift（ErpMfgWorkOrder.view.xml visibleOn 移除 STARTED）；MR3 R3.3 SoD（SoDGuard + AbstractApproveProcessor + ERR_PUR_APPROVER_IS_CREATOR）/R3.4 data-auth（ErpRoleDataAuthChecker config-gated + 6 业务员规则 + 质检员 ownerId 规则）/R3.6 billR 索引（IDX_FIN_VOUCHER_BILL_R_BILL_CODE_BIZ_TYPE）/R3.7 i18n CI（compliance.yml i18n job）；MR5 R5.1 S-mutation sinking（ErpPurOrderApproveProcessor 非 hollow：validateBusinessRules+afterStateChange+commitment/intercompany hooks+sodErrorCode）。**9 CLOSED 全非 hollow**。
  - **残留风险（非阻塞）**：P0-MA2-018 是唯一 open P0（DEFERRED-OK，TOCTOU 由 alreadyPosted 预检缓解非 DB-enforced，successor 待人工裁决）；R3.4 data-auth config-gated OFF（结构性落地，基线运行时默认关，gray-release 有意，非遗漏）。

Exit Criteria:

> 本阶段由独立子代理交付 closure audit 裁决。执行者不得自我审计、不得勾选本阶段自己的 gate（由独立审计结果驱动）。

- [x] 独立子代理（新会话）closure audit 完成，产出 PASS 或带显式补强项的裁决；证据（task id）记录于本计划 Closure 段

### Phase 3 - V.5 arm-index 全量可追溯复核

Status: completed
Targets: `docs/audits/arm-index.md`
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 2 closure audit

- [x] **P0 全量追溯**：arm-index §P0 发现追踪 每条 P0 → 修复 plan + 修复状态（done/closed），无裸「即时通道未闭合」。
  - 6 P0 全量：P0-MA1-021 done / P0-MA2-016 done / P0-MA2-017 fixed / P0-MA2-018 deferred（显式 adjudication + successor 方向 A/B/C/D）/ P0-MA2-019 fixed / P0-MA2-020 done。**5 closed + 1 显式 deferred+adjudicated，零裸 todo**。
- [x] **P1 全量追溯**：arm-index §P1 详细清单 每条 P1 → 归属 MR 工作项 + 该工作项 Status=done，或显式 deferred + successor。R3.0/R3.5 已部分回填，本阶段做最终全量双向一致性复核。
  - **发现并修复不一致**：roadmap 确认 MR1 R1.1–R1.29（29 项全 done）+ MR2 R2.1–R2.15（15 项全 done）+ MR3/MR5 全 done，但 arm-index §P1 详细清单仍有 **102 条陈旧 `todo (R*.x)` 标签**（75 MR1 [R1.12–R1.29，含 67 P1-MA2 + 8 P1-MA4] + 27 MR2 [R2.6/R2.8–R2.15]）未回填——R3.0/R3.5 部分回填后残留。
  - **回填处置**：102 条状态单元逐条回填为 `✅ resolved (R*.x done, roadmap 2026-07-31 确认 done)`（仅状态列末尾，不动描述）。回填后全表 191 P1 行：resolved/done/fixed 179 + 其余 deferred+successor/watch-only P2，**零裸 todo**。
- [x] **跨维度追溯**：arm-index §跨维度发现 3 条 → R4.1（`-1705-1`）裁决为 adjudicated，状态一致。
  - 3 跨维度发现全量：发现 1（P1-MA3-046↔P1-MA2-093/094↔P1-MA6-001/002 四维度交叉）= **adjudicated (R4.1)**（R2.7+R1.29+R3.3+R3.4 协同闭合）/ 发现 2（P1-MA6-002↔P1-MA2-093）= **adjudicated (R4.1)**（互补不重复）/ 发现 3（P1-MA3-048↔P1-MA2-054 子例）= **closed (MR5 R5.8)**。3/3 adjudicated/closed，状态一致。
- [x] **Proof**: 生成「P0/P1 → 修复/deferred」追溯矩阵快照；记录任何残留裸 todo 并处置（不得留裸 todo 闭合 MV）。
  - 追溯矩阵快照：P0 6/6 可追溯（5 closed + 1 deferred+adjudicated）；P1 191/191 可追溯（resolved/done/fixed 179 + deferred+successor / watch-only）；跨维度 3/3 adjudicated/closed。**残留裸 todo = 0**（102 陈旧标签已回填）。

Exit Criteria:

> 本阶段交付 arm-index 全量可追溯证据。

- [x] arm-index 全部 P0/P1/跨维度发现可追溯到「已修复」或「显式 deferred + successor」，零裸 todo

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is (independent subagent, fresh session, cold context) because the plan accurately reflects live-repo facts (E2E baseline 490/1/3 on 2026-07-25 with `master-data.write.amis` as accepted Non-Goal + 4 JVM args correct; V.3/V.4/V.5 all `todo` with V.1→V.3→V.4→V.5 chain intact; MR1–MR3+MR5 done; correctly excludes V.1/V.2 owned by `-1705-2`), correctly enforces independent closure-audit discipline for Phase 2/V.4 (explicit new-session / no-self-audit / no-placeholder language at lines 78/84/86 + Closure Gates line 122), correctly scopes V.4 as mission-level aggregation not replacement of R3.5 per-MR audits (line 34), and is free of anti-slack violations (no banned words; both deferred items carry explicit successor triggers). Non-blocking observations: (a) bundling 3 distinct verification surfaces is borderline under Rule 4 but defensible as the roadmap-designated MV milestone收尾 (sequential dependency, all read-only, one composite result surface); (b) Phase 1 Prereqs add V.2 beyond roadmap's V.3 deps=V.1 (reasonable since V.1/V.2 co-delivered in `-1705-2`); (c) Current Baseline omits transitive prereq R4.1/`-1705-1` status (chain is sound via V.1); (d) "自 2026-07-12 悬挂" dating for master-data.write.amis is imprecise (baseline first records it 2026-07-25; factual state is correct).

## Closure Gates

> 完整仓库验证在此处。本计划 V.3 触及 E2E 运行；V.4/V.5 是只读审计复核（无代码变更）。

- [x] 范围内行为完成（V.3 抽样无回归 + V.4 独立 closure audit PASS/补强项已处置 + V.5 零裸 todo）
- [x] 相关文档对齐（arm-index 闭合回填 + known-good-baselines V.3 基线条目）
- [x] 已运行验证：V.3 抽样 `npx playwright test` 0 回归（Non-Goal 除外）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### visual/ 像素快照套件回归

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: visual/ 像素快照套件不在 V.3 基线（归 plan 2026-07-17-2010-2 独立范围）。V.3 覆盖业务行为回归；视觉回归是独立结果表面。
- Successor Required: `yes`（独立 plan，触发条件=视觉回归里程碑启动）

### E2E 测试增强（A5.6/R3.2 successor）

- Classification: `optimization candidate`
- Why Not Blocking Closure: R3.2 已裁决 E2E 仅冒烟盲区 successor（6 已 seed 仅冒烟报表补 value spec），A5.6 报告登记断言强度增强 successor。V.3 只回归不改写。
- Successor Required: `yes`（触发条件=6 报表 value spec 补全 / 断言强度增强计划启动）

## Closure

Status Note: MV 里程碑整体收口。V.3/V.4/V.5 全 done，MG（G.1–G.4 知识沉淀）阻塞解除。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 closure audit（task `ses_048159890ffekeMzdnaXzHeRO0`，fresh session / cold context / read-only / ~40 实时文件检查）+ 执行者验证（V.3 E2E 抽样 125 spec + V.5 arm-index 全量回填复核）
- Evidence:
  - V.3：`_tmp-server.sh restart`（fresh-DB seed 6s ready）+ 4 批抽样 `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test --workers=1` → **125 passed / 0 failed**；4 项 quality NCR E2E 契约对齐修复（noCapaReason 参数，零生产代码）+ 重跑 4 passed；`mvn clean install -DskipTests` BUILD SUCCESS（156 模块）。
  - V.4：独立子代理裁决 `passes closure audit`——6 P0 全量（5 CLOSED + 1 DEFERRED-OK）+ 9 P1 抽样跨 MR1/2/3/5 全 CLOSED 非 hollow。
  - V.5：arm-index 102 陈旧 `todo (R*.x)` 标签回填（75 R1 + 27 R2），全表 191 P1 + 6 P0 + 3 跨维度发现零裸 todo。
  - roadmap MV V.3/V.4/V.5 状态 todo→done + v24 changelog 回填。

Follow-up:

- P0-MA2-018 维持 deferred（显式 adjudication，successor 方向 A/B/C/D 待人工裁决，TOCTOU 由 alreadyPosted 预检缓解非 DB-enforced）——归 MG/未来轮次。
- R3.4 data-auth config-gated OFF（结构性落地，运行时默认关，gray-release 有意）——enforcement 翻转 successor。
- MG（G.1–G.4 知识沉淀）下一轮起草。
