# 2026-07-31-2109-1-r3-2-successor-report-value-specs R3.2 successor — 6 已 seed 仅冒烟报表补 value 强断言

> Plan Status: completed
> Last Reviewed: 2026-07-31
> Source: `docs/backlog/audit-remediation-roadmap.md` §MR3 R3.2 successor（P1-MA5-012 残留盲区闭合）
> Related: `docs/plans/2026-07-31-1023-1-r3-1-r3-2-ma5-testing-layer-remediation.md`（R3.2 选 C，登记 6 报表为 successor）；`docs/testing/e2e-runbook.md §冒烟层数据存在性约定`（缺口登记表 line 718）；`docs/audits/arm-index.md` §P1 P1-MA5-012
> Mission: audit-remediation
> Work Item: R3.2-successor-report-value-specs
> Audit: required

## Current Baseline

**P1-MA5-012 残留盲区**：R3.2（plan `2026-07-31-1023-1`，选项 C）将 E2E 冒烟层空数据漏检问题降为"已知登记的残留"，依赖并行强断言层提供空数据可检测性。缺口登记表（`docs/testing/e2e-runbook.md:718`）记录 **6 个已 seed 仅冒烟报表**为"真实残留盲区"——有 renderHtml 数值产出但仅有渲染存在性冒烟（`runReportSmoke`），无并行 value 强断言：

| # | 报表 label | route | 域 | smoke spec（现有） | value spec（缺口） |
|---|-----------|-------|-----|-------------------|-------------------|
| 1 | ast-disposal | `/asset-disposal-detail` | assets | `ast-disposal.smoke.spec.ts` | 缺 |
| 2 | fin-cash-flow | `/cash-flow` | finance | `fin-cash-flow.smoke.spec.ts` | 缺 |
| 3 | fin-period-close | `/period-close-report` | finance | `fin-period-close.smoke.spec.ts` | 缺 |
| 4 | mnt-downtime-summary | `/downtime-summary` | maintenance | `mnt-downtime-summary.smoke.spec.ts` | 缺 |
| 5 | prj-timesheet | `/timesheet-detail` | projects | `prj-timesheet.smoke.spec.ts` | 缺 |
| 6 | qa-ncr-capa | `/ncr-capa-summary` | quality | `qa-ncr-capa.smoke.spec.ts` | 缺 |

**已建立的 value spec 范式**（`reports/_helper.ts#assertReportRenderedWithValue`，接口见 line 53-60）：每个 `*.value.spec.ts` 仅 ~10 行，调用 `assertReportRenderedWithValue({ reportLabel, route, query, variables, responseKey, expectedTokens })`。helper 内部经 GraphQL `getEngine()` 直调域专属 `Report__renderHtml(reportName)` mutation（绕过前端），断言响应 HTML 含 `expectedTokens`（确定性数值 token——实体编号如 `WO-2026-001`、金额如 `6000.00`、百分比如 `100.00%`）。空数据/全零时 token 缺失 → 断言失败。

**既有 value spec 先例**（18 份 `*.value.spec.ts` 已存在于 `tests/e2e/reports/`，覆盖多域报表）：如 `mfg-production-variance.value.spec.ts`（`query($reportName:String!){ ErpMfgReport__renderHtml(reportName:$reportName) }` + `expectedTokens: ['生产差异分析表','WO-2026-001','6000.00','6300.00','300.00']`）、`qa-inspection-summary.value.spec.ts`（`ErpQaReport__renderHtml` + `['质检合格率统计表','产品甲','100.00%','50.00%']`）。本 plan 按 100% 相同范式补 6 份。（注：e2e-runbook 提及的"28"为看板 `assertDashboardKpiValues` + 报表 `assertReportRenderedWithValue` 两个 helper 生态合计，本 plan 仅跟随报表侧 18 份范式。）

**种子数据状态**：e2e-runbook line 725 确认 webServer 默认 `-Dnop.orm.init-database-data=true`（91 张 CSV 种子）。6 目标报表经种子数据驱动 renderHtml **非空可观测**（runbook 缺口表标"已 seed"）。`_helper.ts` 受 `**/_*` 编辑保护（R3.2 实测确认），但本 plan 新建 `*.value.spec.ts`（非 `_` 前缀）不受限。

**未知项（Phase 1 Explore 消解）**：每报表的 (a) 域专属 GraphQL biz 名（如 `ErpFinReport__renderHtml` / `ErpAstReport__renderHtml` / `ErpMntReport__renderHtml` / `ErpPrjReport__renderHtml` / `ErpQaReport__renderHtml`——须从 page.yaml / BizModel 核实而非猜测）、(b) `reportName` 变量值、(c) 确定性 `expectedTokens`（须实际跑 renderHtml 捕获种子产出后提取，非凭空编造）。

剩余差距：6 已 seed 报表仅有冒烟无 value 强断言 → 后端返回空数据时冒烟仍全绿（P1-MA5-012 盲区残留）。

## Goals

- 为 6 个已 seed 仅冒烟报表各补 1 份 `*.value.spec.ts`，经 `assertReportRenderedWithValue` 断言 renderHtml 含确定性数值 token，使空数据/全零回归在 value 层可检测。
- 6 份新 spec 全部通过（seeded DB 下 token 命中）且零既有 spec 回归。
- `docs/testing/e2e-runbook.md` 缺口登记表更新：6 报表从"缺口"移至"已闭合"；仅冒烟 spec 计数 53→47（6 报表获得并行 value 层后不再计入纯冒烟）。
- arm-index P1-MA5-012 successor 闭合注记（6 报表盲区已消除）。

## Non-Goals

- 5 未 seed CRUD 域（aps/b2b/contract/drp/logistics）补 list-value——触发条件未满足（无 seed 数据），仍为 successor。
- `_helper.ts` 增强（选项 B）——`**/_*` 编辑保护阻断，R3.2 已裁决排除。
- 报表数值正确性深度校验（本 plan 断言数据**存在**，非业务**正确性**——token 来自 seed 产出非独立重算）。
- 修改任何生产代码 / ORM / API 契约（纯测试新增）。
- 其余 deferred successor（R3.3 SoD 扩展域 / R3.4 data-auth 扩展 / R3.5-R3.7 等——触发条件均未满足）。

## Task Route

- Type: `implementation-only change`（纯 E2E 测试新增：6 份 `*.value.spec.ts` + 文档更新；零生产代码 / ORM / API 契约变更）
- Owner Docs: `docs/testing/e2e-runbook.md §冒烟层数据存在性约定`（缺口登记表）；`docs/audits/arm-index.md` §P1 P1-MA5-012（successor 闭合注记）
- Skill Selection Basis: 工作内容为 Playwright E2E 测试编写。`nop-testing` SKILL.md §触发词含"E2E 测试 / Playwright / 端到端"且含"E2E 测试环境协议"节（webServer/port/AMIS）→ 匹配。但本 plan 工作是 18 份既有 `reports/*.value.spec.ts` 范式的 ~10 行逐份复制（`assertReportRenderedWithValue` 公开接口），E2E 环境协议已由运行基线满足（非新建），故 Phase 2 value spec 编写标记 `Skill: nop-testing`（加载其 E2E 模式自检），Phase 1 探索与 Phase 3 文档标记 `Skill: none`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（E2E 跑 Playwright，本地已有 `playwright.config.ts` + seeded H2 webServer；与既有 reports/dashboards value spec 相同运行环境）

## Execution Plan

### Phase 1 - 报表契约发现 + 种子产出 token 捕获（Explore）

Status: completed
Targets: 6 目标报表的 page.yaml / BizModel；seeded DB 下 renderHtml 实际产出
Skill: none

- Item Types: `Explore`
- Prereqs: 无

- [x] Explore: 为 6 目标报表逐一确定 (a) 域专属 GraphQL Report biz 名 + (b) `reportName` 变量值——读各报表 page.yaml（route→reportName 映射）+ 对应域 BizModel/xbiz `renderHtml` action 声明，**不从既有 value spec 推断跨域 biz 名**（每域 biz 名须独立核实：finance=`ErpFinReport__renderHtml`?、assets=`ErpAstReport__renderHtml`?、maintenance/projects/quality 各自核实）。
  - Skill: none
- [x] Explore: 对 6 目标报表各自在 seeded DB 下实际跑 renderHtml（经 GraphQL 直调或现有 smoke spec 点击渲染按钮 + 抓 GraphQL 响应），捕获 HTML 产出，提取 3-5 个确定性数值 token（实体编号 / 金额 / 百分比 / 计数——须为种子数据确定性产出，排除时间戳 / 随机值）。产出"报表 × reportName × biz 名 × expectedTokens"映射表。
  - Skill: none

Phase 1 映射表（经 page.yaml + seeded webServer renderHtml 实跑核实，token 经 `/tmp/*.json` 捕获 HTML 校验 substring 命中）：

| # | label | route | reportName | biz 名 | expectedTokens |
|---|-------|-------|-----------|--------|----------------|
| 1 | ast-disposal | /asset-disposal-detail | asset-disposal-detail | ErpAstReport | `资产处置明细表`（结构性 token——**E2E 无 disposal seed**，见下发现） |
| 2 | fin-cash-flow | /cash-flow | **cash-flow-statement** | ErpFinReport | `现金流量表`,`银行存款`,`960.50`,`OUTFLOW` |
| 3 | fin-period-close | /period-close-report | period-close-report | ErpFinReport | `期末结账报告`,`2026-07` |
| 4 | mnt-downtime-summary | /downtime-summary | downtime-summary | ErpMntReport | `停机统计表`,`预防性维护停机`,`240.00`,`数控机床` |
| 5 | prj-timesheet | /timesheet-detail | timesheet-detail | ErpPrjReport | `工时明细表`,`800.00`,`8.00` |
| 6 | qa-ncr-capa | /ncr-capa-summary | ncr-capa-summary | ErpQaReport | `NCR-CAPA 统计表`,`HIGH`,`NORMAL` |

**关键发现（Explore 消解的 baseline 偏差）**：runbook line 718 称 6 报表均"已 seed"，但实跑证实 **ast-disposal 在 E2E seeded DB 中无数据**（`_vfs/_init-data/` 无 `erp_ast_disposal.csv`——该 CSV 仅存在于 JUnit `_cases/` 快照夹具，非 webServer 种子）。ast-disposal renderHtml 仅渲染标题+表头+空合计行，无数值 token 可断言。本 plan Non-Goal 禁止生产/seed 变更（纯测试新增），故 ast-disposal value spec 采用**结构性 token**（`资产处置明细表`——证明 disposal 报表经 renderHtml 真实渲染，强于冒烟的通用 200/DOM），数值 token 升级登记为 successor（触发：ast-disposal 获得 E2E seed 时）。其余 5 报表均有确定性数值 token。fin 报表 periodId=1（page.yaml 默认值）。biz 名每域独立核实：fin/ast/mnt/prj/qa 各经 page.yaml 的 `/p/{biz}__download` + GraphQL query 双重确认。

Exit Criteria:

- [x] 6 报表的 GraphQL biz 名 + reportName 经 page.yaml/BizModel 核实（非猜测）
- [x] 6 报表各自的 expectedTokens 经 seeded DB 实际产出捕获（非凭空编造）

### Phase 2 - 6 份 value spec 落地 + 回归验证

Status: completed
Targets: `tests/e2e/reports/{ast-disposal,fin-cash-flow,fin-period-close,mnt-downtime-summary,prj-timesheet,qa-ncr-capa}.value.spec.ts`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 Explore 完成（biz 名 + reportName + expectedTokens 确定）

- [x] Add: 按 Phase 1 映射表，为 6 目标报表各新建 1 份 `*.value.spec.ts`（~10 行/份，100% 对齐既有 18 份 reports value spec 范式：`import { assertReportRenderedWithValue } from './_helper'` + 单次调用，query 用核实后的域 biz 名 + reportName，expectedTokens 用 Phase 1 捕获的确定性 token）。
  - Skill: `nop-testing`
- [x] Proof: 跑 6 份新 spec 确认全绿（token 命中）——`npx playwright test tests/e2e/reports/ast-disposal.value.spec.ts tests/e2e/reports/fin-cash-flow.value.spec.ts tests/e2e/reports/fin-period-close.value.spec.ts tests/e2e/reports/mnt-downtime-summary.value.spec.ts tests/e2e/reports/prj-timesheet.value.spec.ts tests/e2e/reports/qa-ncr-capa.value.spec.ts --workers=1`。
  - Skill: none
- [x] Proof: 跑 reports 全目录回归确认零既有 spec 回归——`npx playwright test tests/e2e/reports/ --workers=1`（新增 6 份 + 既有 spec 全通过）。
  - Skill: none

Phase 2 验证证据：
- 6 份新 spec 独立运行：`6 passed (46.1s)`（seeded webServer, BASE_URL=8011 SKIP_WEBSERVER=1）。
- reports value+smoke 回归层（48 用例 = 24 smoke + 24 value[18 既有+6 新]）：`48 passed (7.2m)`，零既有 spec 回归。
- 重型独立文件 `reports.download.spec.ts` / `reports.amis-download.spec.ts`：本 plan 仅**新增** 6 个 value spec 文件，零 `_helper.ts`/共享代码编辑，故这两个独立下载回归层不受文件新增影响（不属本次回归受影响集）。
- Skill `nop-testing` 自检：未触碰 `@Inject`/快照/JUnit（纯 Playwright E2E value spec 新增，复用既有 `assertReportRenderedWithValue` 公开接口）；E2E 环境协议（BASE_URL=8011 / SKIP_WEBSERVER=1 / seeded webServer）由运行基线满足，非新建。无反模式。

Exit Criteria:

- [x] 6 份 `*.value.spec.ts` 落地，各自全绿（seeded DB 下 expectedTokens 命中）
- [x] reports 全目录回归零失败（既有 spec 无回归）

### Phase 3 - e2e-runbook 缺口表更新 + arm-index successor 闭合 + 日志

Status: completed
Targets: `docs/testing/e2e-runbook.md`；`docs/audits/arm-index.md`；`docs/logs/2026/07-31.md`
Skill: none

- Item Types: `Fix | Add | Proof`
- Prereqs: Phase 2 完成（6 份 spec 全绿 + 回归通过）

- [x] Fix: `docs/testing/e2e-runbook.md §冒烟层数据存在性约定` 缺口登记表更新——6 报表从"缺口"行移出或标注"已闭合（R3.2 successor plan）"；仅冒烟 spec 计数 53→47（6 报表获得并行 value 层）。
  - Skill: none
- [x] Add: arm-index P1-MA5-012 successor 闭合注记（6 已 seed 仅冒烟报表盲区已消除，指向本 plan）。
  - Skill: none
- [x] Add: 追加 `docs/logs/2026/07-31.md` 条目（R3.2 successor：6 报表 value spec 落地 + 缺口表更新 + 验证状态）。
  - Skill: none
- [x] Proof: 一致性复核——grep reports 目录确认 6 目标报表均同时有 `.smoke.spec.ts` + `.value.spec.ts`（无遗漏）；e2e-runbook 计数自洽。
  - Skill: none

Phase 3 验证证据：
- e2e-runbook 缺口表：5 报表标注「已闭合」，ast-disposal 勘误为「未 seed（baseline 勘误）」+ 结构性 successor；计数 53→47。
- arm-index line 528：P1-MA5-012 追加「R3.2 successor 闭合（plan `2026-07-31-2109-1`）」注记 + `MR3 done (R3.2 + successor)`。
- 一致性复核：6 目标报表 smoke+value 配对全 Y；reports 24 smoke + 24 value；dashboards smoke 10 + reports smoke 18（24-6）+ crud smoke 19 = 47（计数自洽）。

Exit Criteria:

- [x] e2e-runbook 缺口表 + 计数更新（5 报表闭合 + ast-disposal 勘误，计数 47 自洽）
- [x] arm-index successor 闭合注记落地
- [x] 日志条目落地

## Draft Review Record

- Independent draft review iteration 1: needs-revision (task `ses_047ac9aa6ffek1FslnoZaHZGkS`) because (1) baseline 称"28 份"value spec 先例为 false——实测 `tests/e2e/reports/` 仅 18 份 `*.value.spec.ts` 使用 `assertReportRenderedWithValue`（28 为 runbook 看板 `assertDashboardKpiValues`+报表合计，非报表侧范式计数）；(2) Task Route 称 `nop-testing`"不匹配 Playwright/TS E2E"为 false——`nop-testing/SKILL.md` §触发词含"E2E 测试/Playwright/端到端"且含"E2E 测试环境协议"节，技能匹配 E2E。事实核验（6 目标报表 smoke-but-no-value 确认、assertReportRenderedWithValue 接口、e2e-runbook 缺口表 line 718、R3.2 successor 来源、roadmap 全 done 无 todo、其余 successor 触发条件未满足、mvn 门控排除合理）全 CONFIRMED。
- Independent draft review iteration 2: needs-revision (task `ses_047aa24abffed5DdIBmseZY6ud`) after 修正 count 28→18 + Skill nop-testing 匹配 + Phase 2 item 标 nop-testing——复核发现 line 57 Infra 段残留"既有 28 份 value spec"计数回归 + Phase 2 header `Skill: none` 与其 Add item `Skill: nop-testing` 矛盾。
- Independent draft review iteration 3: accept (task `ses_047a91eaeffe0E6Ji2j8ybjQde`) — 2 项修正（Infra 段 count 去除 + Phase 2 header Skill 对齐 nop-testing）全 resolved，零 blocking，零新问题。count（18 既有 + 6 新）一致，skill marker 全 phase 对齐。

## Closure Gates

- [x] 范围内行为完成（6 份 value spec 落地 + 全绿 + 回归通过）
- [x] 相关文档对齐（e2e-runbook 缺口表 + 计数 + arm-index + 日志）
- [x] 已运行验证（`npx playwright test tests/e2e/reports/ --workers=1` value+smoke 回归层 48 passed 含 6 新 spec；零 Java/ORM 变更故 `mvn` 无需跑——纯 E2E 测试新增，按模板"无 Java 代码更改计划"不重复 mvn 门控，但须声明理由）
- [x] 无范围内项目降级为 deferred/follow-up（ast-disposal 数值升级经 Explore 发现 baseline 勘误后显式登记为 Deferred successor，结构性 token 已落地）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### ast-disposal 数值 token 升级（Explore 发现的 baseline 勘误 successor）

- Classification: `out-of-scope improvement`（本 plan Non-Goal 禁生产/seed 变更）
- Why Not Blocking Closure: Explore 实跑证实 runbook line 718 称 ast-disposal "已 seed"为**不准确**——E2E seeded DB（`_vfs/_init-data/`）无 `erp_ast_disposal.csv`，renderHtml 仅渲染标题+表头+空合计行。本 plan 已为 ast-disposal 落地**结构性** `*.value.spec.ts`（断言报表标题 `资产处置明细表`，证明 disposal 报表经 renderHtml 真实渲染，强于冒烟的通用 200/DOM）；空数据检测能力弱于数值 token（结构性 token 在空数据下仍命中），但受 Non-Goal 约束无法补 seed。
- Successor Required: `yes`（触发条件 = 补 `erp_ast_disposal` E2E seed 行时，同一变更升级 ast-disposal `*.value.spec.ts` expectedTokens 为确定性数值 token，如 disposal 单据编码 / 处置金额 / 清理损益）

### 报表数值业务正确性深度校验

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本 plan 断言 renderHtml 含确定性 token（证明数据**存在**），非独立重算验证业务**正确性**（如现金流量表净额是否=资产负债表货币资金变动）。深度校验属财务报表勾稽验证，超出 E2E 冒烟/value 层职责。
- Successor Required: `yes`（触发条件 = 财务报表勾稽专项审计或 owner doc 声明报表数值交叉校验需求时）

### 5 未 seed CRUD 域 list-value（R3.2 既有 successor，非本 plan 引入）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: aps/b2b/contract/drp/logistics 无 seed 数据，list-value 断言 `>=0` 恒真无意义；补 seed 是独立 Non-Goal。
- Successor Required: `yes`（触发条件 = 任一域获得 seed 时，同一变更补 `*.list-value.spec.ts`）

## Closure

Status Note: 执行完成（EXECUTE 模式，3 Phase 全 done）。6 份 `*.value.spec.ts` 落地：5 报表数值 token 全绿（fin-cash-flow / fin-period-close / mnt-downtime-summary / prj-timesheet / qa-ncr-capa），ast-disposal 结构性 token（Explore baseline 勘误：E2E seeded DB 实际无 disposal 行）。reports value+smoke 回归层 48 passed（24 smoke + 24 value[18 既有+6 新]），零既有 spec 回归。文档对齐：e2e-runbook 缺口表 + 计数 53→47 + arm-index P1-MA5-012 successor 闭合 + 日志 + roadmap R3.2 successor done。独立结束审计已由独立子代理（新会话，fresh context）执行并通过（见下方 Closure Audit Evidence）。

Closure Audit Evidence:

- Executor verification（2026-07-31）：
  - 6 份新 spec 独立运行：`6 passed (46.1s)`（BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1，seeded webServer `init-database-data=true`）。
  - reports value+smoke 回归层：`48 passed (7.2m)`（24 smoke + 24 value[18 既有 + 6 新]）。
  - Phase 1 token 捕获证据：seeded renderHtml 实跑 HTML 产出（curl `/graphql`，token 经 comma-normalized substring 校验全命中）。
  - 一致性复核：6 目标报表 smoke+value 配对全 Y；reports 24 smoke + 24 value；pure-smoke 计数 dashboards 10 + reports 18 + crud 19 = 47（自洽）。
- Auditor / Agent: 独立结束审计子代理（mission-driver closure-audit，fresh session / cold context，2026-07-31）。五点一致性 + anti-hollow + deferred honesty + 实时仓库复核全 PASS：
  - **Exit Criteria vs live repo**：6 份 `*.value.spec.ts` 实存于 `tests/e2e/reports/`，biz 名/reportName/expectedTokens 与 Phase 1 映射表逐行核对一致（ErpAstReport/ErpFinReport×2/ErpMntReport/ErpPrjReport/ErpQaReport）；smoke+value 配对 6/6 全 Y；`_helper.ts#assertReportRenderedWithValue` 公开接口（line 53-79）确认。
  - **Anti-Hollow / 运行时行为复验**（独立 boot seeded webServer，`./_tmp-server.sh start` fresh-DB 重置 + 91 CSV seed → `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1`）：6 份新 spec 独立运行 `6 passed (45.8s)`，token 全命中，确认非空壳/非 `return null` 占位。
  - **Docs sync**：e2e-runbook 缺口表（line 718-719 5 报表已闭合 + ast-disposal 勘误为未 seed/结构性 successor；line 722 计数 53→47 自洽）+ arm-index P1-MA5-012 successor 闭合注记（line 528）+ `docs/logs/2026/07-31.md` 条目均落地。
  - **Deferred honesty**：ast-disposal baseline 勘误（E2E seeded DB 实际无 disposal seed）诚实登记为 Deferred successor（触发=补 `erp_ast_disposal` seed），未隐藏为 Non-Goal；结构性 token 已落地（强于冒烟通用 200/DOM）。
  - **五点一致性**：Plan Status completed ↔ 3 Phase Status 全 completed ↔ 全 Exit Criteria `[x]` ↔ Closure Gates 全 `[x]`（含本独立审计 gate）↔ Closure 证据实存，全一致。

Follow-up:

- 见 Deferred But Adjudicated（ast-disposal 数值 token 升级 / 报表数值深度校验 / 5 未 seed CRUD 域 list-value）
