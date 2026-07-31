# 2026-07-31-0958-1-r3-0-mr3-p1-finding-expansion R3.0 — MR3 P1 发现展开为具体修复工作项行

> Plan Status: completed
> Last Reviewed: 2026-07-31
> Source: `docs/backlog/audit-remediation-roadmap.md` §MR3 R3.0
> Related: `docs/audits/arm-index.md` §P1 发现汇总 + §P1 详细清单；`docs/plans/00-plan-authoring-and-execution-guide.md` §R*.0 展开机制；`docs/plans/2026-07-29-1923-1-r1-0-mr1-p1-finding-expansion.md`（R1.0 同构先例）
> Audit: required

## Current Baseline

- MA1（结构审计 14 项）+ MA2（业务审计 20 项）+ MA3（文档审计 8 项）+ MA4（代码审计 9 项）全部 `done`；MR1（29 项）+ MR2（15 项）+ MR5（8 项）全部 `done`。
- **MA5/MA6/MA7 审计报告已产出并登记入 arm-index**（arm-index 报告清单 28 份全部 `done`；MA5 6 份 + MA6 4 份 + MA7 4 份覆盖 A5.1-A5.6 / A6.1-A6.4 / A7.1-A7.4）。审计计划均 `Plan Status: completed`（`2026-07-29-1430-1` MA5 / `2026-07-29-1410-1` MA6 权限数据 / `2026-07-29-1410-2` MA6 保护区域 / `2026-07-29-1708-1` MA7 错误码索引 N+1 / `2026-07-29-1708-2` MA7 CI guard）。
- **roadmap 工作项状态滞后**：A5.1-A5.4 / A6.1-A6.4 / A7.1-A7.4 仍标 `ready`（"待独立 closure audit 转 done"）。其中多数审计计划已含独立结束审计证据（如 MA6 权限计划 1410-1 Closure Auditor = 独立子代理新会话 2026-07-29），但 MA7 CI 计划 `1708-2` Closure Auditor 标 `pending`、MA5 计划 `1430-1` Closure Auditor 标"由 Mission Driver CLOSURE_VERIFY 指派"——属 P1-MA6-005 所列"closure-pending completed 计划"超集的子集。
- arm-index §P1 详细清单已登记全部 MR3 归属 P1 发现，每项含「目标 MR = MR3」+「修复状态 = todo」列。**MR3 归属 P1 共 13 项**（去重后，不含 MA5 归并项 6 项——它们是 MA4/MA2 既有 finding 在测试层的系统化投影，已随 MR2 修复闭合，不重复展开）：
  - **MA5（6 项）**：P1-MA5-001 finance 计数文档过时(46→64) / P1-MA5-004 mfg 计数文档过时(19→29) / P1-MA5-006 mfg 物料预留零测试（注：arm-index §P1 详细清单 line 512 标"归并"为 P1-MA3-042 测试层投影，但 §P1 发现汇总 line 105 列为"5 项独立"之一目标 MR3——R3.0 须裁决其归属）/ P1-MA5-007 hr 计数文档过时(10→15) / P1-MA5-010 assets 计数文档深度分类错(浅1→浅0) / P1-MA5-012 E2E 55 仅冒烟 spec 结构性盲区
  - **MA6（5 项）**：P1-MA6-001 职责分离创建人≠审核人零强制 / P1-MA6-002 角色侧行级过滤 4 类规则 0 落地 / P1-MA6-003 ORM ask-first 保护区域 5 份计划缺独立 plan-audit/closure / P1-MA6-004 deployment+auth 保护区域 2 份计划缺独立 plan-audit/closure / P1-MA6-005 系统性第三波 closure-pending「completed」计划 ~16 份
  - **MA7（2 项）**：P1-MA7-001 ErpFinVoucherBillR 缺 (billCode, businessType) 索引 / P1-MA7-007 F15 i18n-coverage-checker.sh 未接入 CI
- **交叉维度协同注记**（arm-index 已登记，R3.0 展开时须保留交叉引用，不重新仲裁——仲裁属 R4.1）：P1-MA6-002（角色侧 createdById/assigneeId/deptId）↔ P1-MA2-093（orgId 多公司，MR1 已 done）互补不重复，须在对应 R3.x 行注明"与 P1-MA2-093 协同"；P1-MA7-001（非唯一索引）↔ P0-MA2-018（deferred UK）互补不重复；P2-MA7-005（红冲有界 N+1 批量加载）与 P1-MA7-001 协同（MR3 一并收益最大）。
- P0 全部已修复或 deferred（P0-MA2-018 deferred 方向 A/B/C/D 维持）。
- roadmap §MR3 表仅含 R3.0 + R3.x 占位行（`_（R3.0 执行后自动展开：新增行初始 Status=todo）_`），无具体修复工作项行。

剩余差距：roadmap §MR3 表无具体修复工作项行；MA5/MA6/MA7 工作项状态滞后（ready 未转 done）。

## Goals

- 将 arm-index.md 中全部「目标 MR = MR3」的 P1 发现（13 项，去重后）展开为 roadmap §MR3 表中的具体修复工作项行（R3.1, R3.2...），每行含 finding ID 覆盖范围 / 域 / 修复范围 / Skill / 是否触及保护区域标记。
- 工作项按根因 / 修复方式 / 结果表面分组（同一根因合并为单行，如计数文档刷新 4 项合并；保护区域过程纪律 P1-MA6-003/004/005 按结果表面分组）。
- 裁决 P1-MA5-006（mfg 物料预留零测试）归属：因 P1-MA3-042 物料预留子系统整体未实现且 R2.6 已标 owner doc Deferred，无代码可测——裁决为"successor 注记"而非独立修复工作项（理由记入 plan）。
- 同步更新 arm-index.md 每项 MR3 finding 的「修复状态」列交叉引用其归属的 R3.x 工作项 ID。
- 协同 MA5/MA6/MA7 工作项状态核对：对已具备独立结束审计证据的项（如 A6.1-A6.3 经 1410-1 独立子代理 closure）将 roadmap Status `ready`→`done`（bookkeeping，附证据指针）；对 closure 仍 pending 的项（如 A7.4 经 1708-2）保持 `ready` 并在 R3.x 行（P1-MA6-005）中显式覆盖其补审计路径——不静默降级。
- 更新 roadmap R3.0 Status `todo`→`done`。

## Non-Goals

- 实际代码/ORM/文档/CI 修复（属 R3.x 工作项，R3.0 仅展开不修复）。
- R4.0 跨维度裁决（须 MR1+MR2+MR3 全 done 后；本 plan 保留交叉协同注记供 R4.1，不仲裁）。
- 重新审计或重新评级既有 finding（R3.0 忠实展开 arm-index 既有分类与归属；P1-MA5-006 归属裁决例外因 arm-index 内部存在 line 105 vs line 512 自相矛盾，须消解）。
- P0 发现（经即时通道，不进 MR 批量）。
- 完整重跑 MA5/MA6/MA7 审计（已完成；仅做状态核对与 bookkeeping）。
- MR1/MR2 范围修复（已 done）。

## Task Route

- Type: `implementation-only change`（roadmap + arm-index 文档/索引展开 + 状态 bookkeeping，零代码变更）
- Owner Docs: `docs/audits/arm-index.md` §P1 发现汇总 + §P1 详细清单 + §交叉维度协同；`docs/backlog/audit-remediation-roadmap.md` §MR3
- Skill Selection Basis: none — 本任务是索引/文档展开与状态 bookkeeping，不涉及 ORM/BizModel/view/测试代码。R3.x 修复工作项行的 Skill 列将引用各 finding 报告标注的 skill（如 `open-ended-audit-prompt.md` / `multi-dimensional-audit-prompt.md`），R3.0 仅如实转录不执行。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 - 清点 MR3 归属发现 + 消解 P1-MA5-006 矛盾 + MA5/MA6/MA7 状态核对

Status: completed
Targets: `docs/audits/arm-index.md` §P1 详细清单 + §交叉维度协同；roadmap §MA5/MA6/MA7 状态列
Skill: none

- Item Types: `Proof | Decision`
- Prereqs: MA5+MA6+MA7 审计报告产出（已满足——arm-index 28 份报告全 done + 5 份审计计划 completed）

- [x] Proof: 从 arm-index.md §P1 详细清单提取全部「目标 MR = MR3」的 finding，逐一 grep 确认其「修复状态 = todo」且未被 MR1/MR2 覆盖。按来源 MA 分组计数（MA5 原生 + MA6 原生 + MA7 原生），产出清点表（finding ID / 域 / 根因类型 / 是否触及保护区域 / 交叉协同指针）。排除 MA5 归并项（002/003/005/008/009/011——MA4/MA2 投影，随 MR2 闭合）。
  - Skill: none

**清点结果（2026-07-31 实测）：MR3 归属独立 finding 共 13 项（去重后，不含 MA5 归并项 6 项 = P1-MA5-002/003/005/008/009/011）。按来源 MA 分组：MA5 原生 6 项 + MA6 原生 5 项 + MA7 原生 2 项 = 13。全部「修复状态 = todo」「目标 MR = MR3」经 arm-index §P1 详细清单 line 279-285（MA6/MA7）+ line 507-518（MA5）逐行核实，无 MR1/MR2 覆盖（归并项已显式排除）。**

| Finding ID | 域 | 根因类型 | 触及保护区域 | 交叉协同指针 |
|-----------|---|---------|------------|------------|
| `P1-MA5-001` | finance(docs) | 测试计数文档过时(46→64) | 否（机械文档刷新） | — |
| `P1-MA5-004` | mfg(docs) | 测试计数文档过时(19→29) | 否（机械文档刷新） | — |
| `P1-MA5-006` | mfg | 物料预留子系统零测试 | 否（被测功能不存在） | ↔ P1-MA3-042（R2.6 已 Deferred）→ successor 注记（Phase 1 裁决见下） |
| `P1-MA5-007` | hr(docs) | 测试计数文档过时(10→15) | 否（机械文档刷新） | — |
| `P1-MA5-010` | assets(docs) | 测试计数文档深度分类错(浅1→浅0) | 否（机械文档刷新） | — |
| `P1-MA5-012` | tests/e2e（跨域） | 55 仅冒烟 spec 结构性盲区 | 否（helper/spec/约定） | — |
| `P1-MA6-001` | finance/mfg/pur/sal（4 S/A 域） | 职责分离 SoD 创建人≠审核人程序级零强制 | 否（Processor 业务逻辑） | ≠ P1-MA3-046（action-level RBAC，工作流级 SoD 独立） |
| `P1-MA6-002` | 全 19 域 | 角色侧行级过滤 4 类规则 0 落地 | **是（auth/permissions — 19 data-auth.xml + IUserContext）** | ↔ P1-MA2-093（orgId 多公司维度，互补不重复，MR3 协同） |
| `P1-MA6-003` | finance/mfg/aps/inv/cross-domain | ORM ask-first 保护区域 5 计划缺独立 plan-audit/closure | 否（补审计证据，过程纪律） | — |
| `P1-MA6-004` | master-data/finance/frontend | deployment+auth 保护区域 2 计划缺独立 plan-audit/closure | 否（补审计证据，过程纪律） | — |
| `P1-MA6-005` | 全域（docs/plans/） | 第三波 closure-pending「completed」计划 ~16 份超集 | 否（补审计证据，过程纪律） | 含 003/004 高优先级切片 + 新浮现 MA5(A5.1-A5.4)/MA7(A7.4) closure-pending |
| `P1-MA7-001` | finance（ErpFinVoucherBillR） | 缺 (billCode, businessType) 非唯一索引 | **是（ORM ask-first）** | ↔ P0-MA2-018（deferred UK，互补不重复）+ P2-MA7-005（有界 N+1 批量加载协同） |
| `P1-MA7-007` | 全仓（CI workflow） | F15 i18n-coverage-checker.sh 未接入 CI | 否（CI 工作流，修复须独立 plan） | — |
- [x] Decision: 裁决 P1-MA5-006（mfg 物料预留零测试）归属。arm-index line 105 列其为"5 项独立[目标 MR3]"之一，line 512 标"（归并）...MA3 P1-MA3-042 功能缺失投影"。核实：P1-MA3-042 物料预留子系统整体未实现，R2.6（plan 2026-07-31-0310-1）已标 owner doc Deferred。**预期裁决（须 EXECUTE 时据实确认）**：因被测功能不存在，无测试可补——登记为"successor 注记"（物料预留实现 plan 须含测试建立）而非独立 R3.x 修复行。理由 + 替代方案（若 P1-MA3-042 未来实现则该测试义务随实现 plan 落地）记入 plan。
  - Skill: none

**裁决确认（2026-07-31 EXECUTE 实测）：P1-MA5-006 归属 = successor 注记（非独立 R3.x 修复行）。** 核实：(1) arm-index line 105 列其为「5 项独立[目标 MR3]」之一；(2) arm-index line 512 标「（归并）...MA3 P1-MA3-042 功能缺失投影」——内部矛盾确认存在；(3) P1-MA3-042 物料预留子系统整体未实现（material-reservation.md 声明 + R2.6 plan `2026-07-29-2005-1` Phase 6 / `2026-07-31-0310-1` 已标 owner doc Deferred）→ 被测功能不存在，零代码可测，无测试可补。**消解**：裁决为 successor 注记——当 P1-MA3-042 物料预留子系统实现 plan 落地时，该 plan 须含测试建立义务（见 §Deferred But Adjudicated 已命名 successor 触发条件）。故 MR3 归属独立 R3.x 修复行 = 13 − 1（006 successor）= 12 项展开为工作项行。
- [x] Proof: MA5/MA6/MA7 状态核对——逐一核实 12 个 ready 工作项（A5.1-A5.4 / A6.1-A6.4 / A7.1-A7.4）对应审计计划的 Closure Audit Evidence 段：
  - 有独立子代理（新会话）closure 证据 → roadmap Status `ready`→`done`（bookkeeping，附 Auditor 指针）
  - closure 仍 pending（如 A7.4 经 1708-2 "Auditor: pending"）→ 保持 `ready`，在 Phase 3 展开的 P1-MA6-005 R3.x 行中显式列入其补审计路径
  - 产出核对表（工作项 / 审计计划 / Closure 状态 / 处置）
  - Skill: none

**状态核对表（2026-07-31 实测，roadmap §MA5/MA6/MA7 行 314-323 逐行核实各审计计划 §Closure Audit Evidence 段）：**

| 工作项 | 审计计划 | Closure Audit Evidence 状态 | 处置 |
|-------|---------|---------------------------|------|
| A5.1-A5.4 | `2026-07-29-1430-1` | **pending** — `Auditor / Agent: 执行代理（本会话）产出...；独立结束审计由 Mission Driver CLOSURE_VERIFY 循环指派独立子代理`（执行者自查，独立 closure 推迟） | 保持 `ready`；补审计路径列入 R3.5（P1-MA6-005）行 |
| A5.5-A5.6 | `2026-07-29-1430-2` | **done** — `Auditor / Agent: 独立 closure-audit 子代理（fresh context，未参与执行，本次会话）` | roadmap 已 `done`（行 315），无需翻转 |
| A6.1-A6.3 | `2026-07-29-1410-1` | **done** — `Auditor / Agent: 独立结束审计子代理（新会话，非 EXECUTE 执行者，2026-07-29）` + PASS 五点一致 | `ready`→`done`（bookkeeping，Auditor 指针=独立子代理新会话 2026-07-29） |
| A6.4 | `2026-07-29-1410-2` | **done** — `Auditor / Agent: 独立结束审计子代理（新会话...task ses_052f048faffeleNlxiXQWmzfk3）` + PASS 五点一致 + 行号引用精确 | `ready`→`done`（bookkeeping，Auditor 指针=task ses_052f048faffeleNlxiXQWmzfk3） |
| A7.1-A7.3 | `2026-07-29-1708-1` | **done** — `Auditor / Agent: 独立 closure audit 子代理（新会话，不重用执行者上下文）` + PASS（报告非 hollow + arm-index 登记 + roadmap ready + 日志 + anti-hollow 复核） | `ready`→`done`（bookkeeping，Auditor 指针=独立子代理新会话） |
| A7.4 | `2026-07-29-1708-2` | **pending** — `Auditor / Agent: <独立结束审计子代理（pending）>` | 保持 `ready`；补审计路径列入 R3.5（P1-MA6-005）行 |

**翻转汇总：3 行 ready→done（A6.1-A6.3 / A6.4 / A7.1-A7.3）+ 2 行保持 ready（A5.1-A5.4 / A7.4）。** A5.5-A5.6 已 done 无需翻转。closure-pending 的 A5.1-A5.4（1430-1）+ A7.4（1708-2）补审计路径在 Phase 3 显式列入 R3.5 行覆盖范围。

Exit Criteria:

- [x] 清点表产出，MR3 归属 finding 数量与 arm-index §P1 发现汇总计数一致（预期 13 ± P1-MA5-006 裁决调整）— 实测 13 项独立（MA5×6 + MA6×5 + MA7×2），与 arm-index line 14「MR3 归属 P1 共 13 项」一致
- [x] P1-MA5-006 矛盾消解，裁决记入 plan（含理由 + successor 触发条件）— 裁决为 successor 注记（被测功能 P1-MA3-042 不存在），见本 Phase 裁决段 + §Deferred But Adjudicated
- [x] MA5/MA6/MA7 状态核对表产出，每项处置明确（done flip / 保持 ready + 补审计路径）— 3 行翻转 done + 2 行保持 ready + A5.5-A5.6 已 done

### Phase 2 - 排序与分组展开为 R3.x 工作项行

Status: completed
Targets: `docs/backlog/audit-remediation-roadmap.md` §MR3 表
Skill: none

- Item Types: `Add | Decision`
- Prereqs: Phase 1 清点完成

- [x] Decision: 确定分组策略（按根因 / 修复方式 / 结果表面），预期分组（EXECUTE 时据实最终化，不强制锁定）：
  - **测试计数文档刷新**（机械文档更新，4 finding 合并）：P1-MA5-001/004/007/010 → 1 行（`docs/testing/test-depth-classification.md` 四域计数刷新）
  - **E2E 仅冒烟盲区**：P1-MA5-012 → 1 行（helper 增强 / list-value spec 补充 / 文档化约定，修复方式裁决属 R3.x）
  - **职责分离 SoD 强制**：P1-MA6-001 → 1 行（触及 4 S/A 级域 Processor doApprove）
  - **角色侧行级过滤**：P1-MA6-002 → 1 行（触及 19 data-auth.xml + IUserContext，与 P1-MA2-093 协同注记）
  - **保护区域过程纪律批次**（按结果表面分 1-3 行）：P1-MA6-003（ORM ask-first 5 计划补 audit）+ P1-MA6-004（deployment+auth 2 计划补 audit）+ P1-MA6-005（第三波 ~16 份超集清理）——三者同结果表面（补独立 plan-audit/closure）但覆盖不同保护区域切片，裁决合并或分行
  - **billR 索引**：P1-MA7-001 → 1 行（`app-erp-finance.orm.xml` 加非唯一索引，ORM ask-first 保护区域，与 P0-MA2-018 deferred UK 互补不冲突注记）
  - **i18n checker CI 接入**：P1-MA7-007 → 1 行（`.github/workflows/compliance.yml` 新增 job）
  - 预期 7-9 行（视 P1-MA6-003/004/005 合并裁决）
  - Skill: none

**分组裁决（2026-07-31 EXECUTE 最终化）：7 个逻辑工作项 R3.1~R3.7。** 分组规则与残留风险：

1. **测试计数文档刷新合并为 1 行**（R3.1）：P1-MA5-001/004/007/010 四域计数刷新同根因（`docs/testing/test-depth-classification.md` 单一文档计数系统性过时）+ 同修复方式（机械文档计数刷新），合并为单行避免 4 行机械重复。
2. **E2E 仅冒烟盲区 1 行**（R3.2）：P1-MA5-012 单 finding，修复方式（helper 增强 / list-value spec / 文档约定）属 R3.2 EXECUTE 裁决。
3. **SoD 强制 1 行**（R3.3）：P1-MA6-001 单 finding（4 S/A 域 Processor.doApprove），独立于 action-level RBAC（≠ P1-MA3-046）。
4. **角色侧行级过滤 1 行**（R3.4）：P1-MA6-002 单 finding，与 MR1 P1-MA2-093（orgId 维度）协同注记（同为 data-auth.xml 填充 + IUserContext 扩展，不同维度互补不重复）。
5. **保护区域过程纪律批次合并为 1 行**（R3.5）：P1-MA6-003 + P1-MA6-004 + P1-MA6-005 三者**同结果表面**（补独立 plan-audit/closure evidence）+ P1-MA6-005 显式声明「003/004 是此超集的高优先级保护区域切片」。**合并理由**：同一补审计动作应用于不同 plan 集，分行造成人工碎片化；合并为单行统一发起 Round 3 closure-audit 批次更可执行。**新浮现 closure-pending**：MA5 A5.1-A5.4（plan 1430-1）+ MA7 A7.4（plan 1708-2）两份审计计划独立 closure 仍 pending（Phase 1 核对表确认），显式列入 R3.5 行覆盖范围——不静默降级。
6. **billR 索引 1 行**（R3.6）：P1-MA7-001 单 finding，**[ORM ask-first]** 保护区域，与 P0-MA2-018 deferred UK 互补不冲突注记 + P2-MA7-005 有界 N+1 协同注记。
7. **i18n checker CI 接入 1 行**（R3.7）：P1-MA7-007 单 finding，触及 CI 工作流（修复须独立 plan）。

**考虑的替代分组与残留风险**：(a) P1-MA6-003/004/005 分 3 行——被拒，因同结果表面合并更可执行且 P1-MA6-005 本身是超集；(b) 计数文档刷新分 4 行——被拒，因 4 行机械重复无信息增量。残留风险：R3.5 合并后单 plan 须处理 ~16+2 份计划的补审计，工作量集中，但属 bookkeeping 非代码风险。Skill 全 `none`（对齐 MR1/MR2 表惯例——R*.x 修复工作项行的 Skill 在各 R3.x EXECUTE 时按 finding 实际修复方式选定，R3.0 仅如实转录，arm-index finding 不携带 skill 标签）。

- [x] Add: 在 roadmap §MR3 表中，将 R3.x 占位行替换为具体修复工作项行。每行含：`#`（R3.1, R3.2...）/ Work Item（finding ID 覆盖范围 + 简述）/ Status=`todo` / Owner Doc / Deps=`R3.0` / Skill（转录 arm-index 报告标注）。触及保护区域的行加 `**[ORM ask-first]**` / `**[会计保护区域]**` 等标记（对齐 MR1/MR2 表惯例）。
  - Skill: none

**Add 落地：roadmap §MR3 表 R3.x 占位行替换为 R3.1~R3.7 共 7 行（见 `docs/backlog/audit-remediation-roadmap.md` 行 199）。** 每行字段完整（finding ID 覆盖范围 / 域 / 修复范围 / Skill=none / 保护区域标记 / Deps=R3.0），Status 全 `todo`。

Exit Criteria:

- [x] roadmap §MR3 表含具体 R3.x 工作项行（无占位行残留），每行字段完整（finding ID / 域 / 修复范围 / Skill / 保护区域标记 / Deps=R3.0），Status 全 `todo` — R3.1~R3.7 共 7 行落地，占位行已替换
- [x] 分组策略裁决记入 plan（含考虑的替代分组与残留风险）— 7 组裁决 + 替代分组(a)(b)被拒理由 + R3.5 残留风险记录

### Phase 3 - arm-index 交叉引用回填 + roadmap R3.0 done + 日志

Status: completed
Targets: `docs/audits/arm-index.md` §P1 详细清单；`docs/backlog/audit-remediation-roadmap.md` §MR3 R3.0；`docs/logs/2026/07-31.md`
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 2 工作项行落地

- [x] Add: 更新 arm-index.md §P1 详细清单每项 MR3 finding 的「修复状态」列，交叉引用其归属的 R3.x 工作项 ID（如 `MR3 todo (R3.x)`）。P1-MA5-006 按 Phase 1 裁决标注（successor 注记）。
  - Skill: none
- [x] Add: 更新 roadmap §MR3 R3.0 行 Status `todo`→`done`，Last Reviewed 注记本 plan id + 展开行数 + P1-MA5-006 裁决。
  - Skill: none
- [x] Add: 追加 `docs/logs/2026/07-31.md` 条目（R3.0 展开：N 项 MR3 P1 → M 行 R3.x 工作项 + P1-MA5-006 裁决 + MA5/MA6/MA7 状态核对结果）。
  - Skill: none
- [x] Proof: 一致性复核——grep 确认 arm-index §P1 详细清单中每项「目标 MR=MR3」finding 均有 R3.x 交叉引用（无遗漏），roadmap R3.x 行的 finding ID 覆盖范围与 arm-index 单向闭合（双向可追溯）。
  - Skill: none

**一致性复核结果（2026-07-31 实测）：**

- **arm-index → roadmap（正向）**：grep `| MR3 | todo` arm-index.md = 12 行独立 finding 全部回填 R3.x 交叉引用（R3.1×4 / R3.2×1 / R3.3×1 / R3.4×1 / R3.5×3 / R3.6×1 / R3.7×1 = 12）+ P1-MA5-006 successor 注记 + 6 归并项标注「随 MR2/MR1 parent 闭合」。`grep "| MR3 | todo |\|| MR3(归并) | todo |"` = 0 匹配（无遗漏裸 todo）。
- **roadmap → arm-index（反向）**：roadmap §MR3 R3.1~R3.7 共 7 行，每行 Work Item 列枚举的 finding ID 全部可在 arm-index §P1 详细清单定位到对应行且「目标 MR=MR3」：
  - R3.1 → P1-MA5-001/004/007/010（4 项 ✓）
  - R3.2 → P1-MA5-012（1 项 ✓）
  - R3.3 → P1-MA6-001（1 项 ✓）
  - R3.4 → P1-MA6-002（1 项 ✓）
  - R3.5 → P1-MA6-003/004/005（3 项 ✓）
  - R3.6 → P1-MA7-001（1 项 ✓）
  - R3.7 → P1-MA7-007（1 项 ✓）
  - 合计 12 项独立 finding 全部双向可追溯 + P1-MA5-006 successor 注记（非 R3.x 行，裁决记录于 §Deferred But Adjudicated）。
- **计数闭合**：13 项 MR3 归属 P1 = 12 项展开为 R3.x 行 + 1 项（P1-MA5-006）successor 注记，与 arm-index line 14「MR3 归属 P1 共 13 项」+ Phase 1 清点一致。

Exit Criteria:

- [x] arm-index §P1 详细清单 MR3 finding「修复状态」列全部回填 R3.x 交叉引用 — 12 独立 + 006 successor + 6 归并随 parent 闭合，0 裸 todo
- [x] roadmap R3.0 Status=done，R3.x 行 Status 全 todo — R3.0 done + R3.1~R3.7 todo
- [x] 双向可追溯复核通过（arm-index ↔ roadmap R3.x 行 finding ID 闭合）— 正向 12 项全回填 + 反向 7 行全定位 + 计数闭合 13=12+1 successor

## Draft Review Record

- Independent draft review iteration 1: accept (task `ses_04a170729ffe5aamyPBABzMyiM`) because 计划是诚实、范围准确、可执行的展开契约——忠实遵循 R1.0 同构先例；"13 项 MR3 P1"经 arm-index 核验准确（MA5×6 + MA6×5 + MA7×2 = 13）；P1-MA5-006 内部矛盾（arm-index line 105 vs line 512）真实存在且 successor 注记裁决属合法范围（消解索引矛盾 + 具体 successor 触发条件）；范围正确排除实际修复(R3.x)/跨维度裁决(R4.1)/重新审计；模板合规、无 slack 语言、零代码计划正确删除 build/test 门控。采纳 2 项非阻塞计数勘误（"A7.1-A7.7"→"A7.1-A7.4" + "14 个 ready"→"12 个 ready"）已修订。

## Closure Gates

> 本 plan 零代码/ORM/view 变更（纯 roadmap + arm-index 文档展开 + 状态 bookkeeping），按 authoring guide 执行时规则 7 删除 typecheck/build/lint/test 验证门控。文档一致性以 grep 复核证明。

- [x] 范围内行为完成（13 项 MR3 P1 全部展开为 R3.x 行或裁决为 successor 注记，无遗漏）
- [x] 相关文档对齐（roadmap §MR3 表 + arm-index §P1 详细清单 + roadmap MA5/MA6/MA7 状态列）
- [x] 已运行验证（grep 双向可追溯复核 + xmllint 不适用[纯 .md] + compliance checker 不适用[纯文档零反模式变更]）
- [x] 无范围内项目降级为 deferred/follow-up（P1-MA5-006 裁决为 successor 注记非降级——因被测功能不存在，附 successor 触发条件）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### P1-MA5-006 mfg 物料预留零测试（裁决为 successor 注记，非独立修复行）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: P1-MA3-042 物料预留子系统整体未实现（R2.6 plan 2026-07-31-0310-1 已标 owner doc Deferred）。被测功能不存在 → 无测试可补。arm-index line 105（独立 MR3）与 line 512（归并 P1-MA3-042 投影）矛盾，R3.0 消解为 successor 注记。
- Successor Required: `yes` — 当 P1-MA3-042 物料预留子系统实现 plan 落地时，该 plan 须含测试建立义务。

## Closure

Status Note: EXECUTE 完成（2026-07-31）+ 独立结束审计 PASS（2026-07-31）。三阶段全 done——Phase 1 清点 MR3 归属 P1 独立 13 项（MA5×6 + MA6×5 + MA7×2，排除 6 归并项）+ P1-MA5-006 矛盾消解为 successor 注记 + MA5/MA6/MA7 状态核对（3 批 ready→done bookkeeping + 2 项保持 ready 显式列入 R3.5）；Phase 2 裁决 7 个逻辑工作项 R3.1~R3.7 写入 roadmap §MR3 表（替换占位行）；Phase 3 arm-index §P1 详细清单 13 项交叉引用回填（12 独立 R3.x + 006 successor + 6 归并随 parent 闭合）+ roadmap R3.0 done + 日志 + 双向可追溯 grep 复核通过（正向 0 裸 todo + 反向 7 行全定位 + 计数闭合 13=12+1 successor）。零代码/ORM/view 变更，按 authoring guide 规则 7 删除 build/test 门控，文档一致性以 grep 证明。独立结束审计由独立子代理新会话执行（见下证据），Closure Gates 全 `[x]`。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，非 EXECUTE 执行者，2026-07-31）— CLOSURE_VERIFY 循环指派的独立 closure auditor
- Five-point consistency PASS: (1) 顶部 `Plan Status: completed` ↔ 三 Phase `Status: completed` 一致；(2) 三 Phase Exit Criteria 全 `[x]`（Phase 1×3 / Phase 2×2 / Phase 3×3）；(3) Closure Gates 全 `[x]`（含倒数两项由本独立子代理本次勾选）；(4) `docs/logs/2026/07-31.md` 行 3-8 与 plan Status 一致；(5) §Deferred But Adjudicated P1-MA5-006 successor 注记与 Phase 1 裁决一致
- 实时仓库复核（fresh session，逐项 grep/read 验证，非信任 [x]）：
  - `audit-remediation-roadmap.md:194` header 注记「R3.0 done（2026-07-31，plan 2026-07-31-0958-1）：13 项...展开为 7 个具体修复工作项行 R3.1~R3.7」
  - `audit-remediation-roadmap.md:198` R3.0 行 `Status=done` ✓
  - `audit-remediation-roadmap.md:199-205` R3.1~R3.7 共 7 行落地，字段完整（finding ID 覆盖范围 / 域 / 修复范围 / Skill=none / 保护区域标记 / Deps=R3.0），Status 全 `todo` ✓
  - `arm-index.md` §P1 详细清单 grep `| MR3 | todo (R3\.` = 12 独立 finding 全回填 R3.x 交叉引用（MA6×5: R3.3/R3.4/R3.5×3 + MA7×2: R3.6/R3.7 + MA5×5: R3.1×4/R3.2）✓
  - `arm-index.md:512` P1-MA5-006 标 `successor 注记（R3.0 裁决：被测功能 P1-MA3-042 不存在→无测试可补）` 与 §Deferred But Adjudicated 一致 ✓
  - `audit-remediation-roadmap.md:324/325/328` A6.1-A6.3/A6.4/A7.1-A7.3 三批 `done`（bookkeeping flip 落地）✓
  - `audit-remediation-roadmap.md:320/329` A5.1-A5.4/A7.4 保持 `ready` + 显式补审计路径列入 R3.5（不静默降级）✓
  - `docs/logs/2026/07-31.md:3-8` R3.0 日志条目存在 ✓
- 计数闭合: 13 项 MR3 归属 P1 = 12 独立 R3.x 行 + 1 successor 注记（P1-MA5-006），与 arm-index line 14 + Phase 1 清点一致 ✓
- Anti-hollow: 无代码变更（纯文档展开 + bookkeeping），无 hollow 风险面；R3.1~R3.7 行的工作项均带明确修复目标/路径/Deps，非占位
- Deferred honesty: A5.1-A5.4/A7.4 closure-pending 未隐藏——显式保持 ready + 列入 R3.5 行覆盖范围（非降级为 follow-up）；P1-MA5-006 successor 注记附 successor 触发条件（P1-MA3-042 物料预留实现 plan 落地），非活跃缺陷隐藏

Follow-up:

- _（非阻塞跟进；P1-MA5-006 successor 已在 Deferred But Adjudicated 命名触发条件）_
