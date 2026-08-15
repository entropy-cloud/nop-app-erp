# 2026-08-15-2119-1-rc-mr1-r1-44-fin-reverse-close-audit-trail RC-R1.44 — finance 反结账审计轨迹（MR1 第二批 A 类 ORM 批量授权 + 会计过账逻辑双独立子 agent 批准）

> Plan Status: completed
> Last Reviewed: 2026-08-16
> Mission: requirement-compliance
> Work Item: RC-R1.44（P1-RC-006 finance 反结账审计轨迹[操作人/原因]完全缺失）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.44 行 + `docs/audits/arm-index.md` P1-RC-006 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（**2026-08-12 批量裁决 A 类：ErpFinAccountingPeriod 加 reverseCloseReason/reversedBy/reverseCloseAt 3 列 或 新增 ErpFinReverseCloseLog 实体，ORM 部分批量授权**）
> Related: `docs/design/finance/use-cases.md`（L1 UC-FIN-07 RC-9，:140）；`docs/design/finance/period-close.md`（§反结账流程 :170-228）；`docs/audits/2026-08-06-1708-rc-ma4-a4-1-20-rc9-reverse-close-audit-trail-degraded-evidence.md`（A4.1.20 降级证据评估）；`docs/plans/2026-08-15-1838-2-finance-bank-recon-counterparty-dimension.md`（A 类 ORM 批量授权执行先例）
> Audit: required

## Current Baseline

- **finding P1-RC-006（arm-index 行，UC-FIN-07 RC-9）**：L1（`use-cases.md:140`）逐字「全程审计(记录反结账**操作人/原因**)」。L3 实仓（HEAD 核查）：
  - `IErpFinPeriodCloseBiz.reverseClose`（`module-finance/erp-fin-dao/.../IErpFinPeriodCloseBiz.java:44-45`）签名 `reverseClose(@Name("periodId") Long periodId, IServiceContext context)` **无 reason 参数**；
  - `ErpFinAccountingPeriodReverseCloseProcessor.reverseClose`（`module-finance/erp-fin-service/.../processor/ErpFinAccountingPeriodReverseCloseProcessor.java:30-71`）：`requirePeriod:31` → `assertCanReverseClose:33`（CLOSED_FINAL 守卫）→ `isReverseCloseApprovalRequired:38-41`（kill-switch，config `erp-fin.reverse-close-approval-required` 默认 true）→ `setStatus(OPEN):51` → `reverseCloseVoucher ×2-3:54-62`（PERIOD_CLOSE/FX 红冲 + 年末 PROFIT_TO_RETAINED_EARNINGS）→ `reopenModules:68` → `flushSession:69`。**无 reason 参数、无 `setReversedBy`/`setReverseCloseReason`/`setReverseCloseAt`、无 ReverseCloseLog 写入**；
  - ORM `ErpFinAccountingPeriod`（`app-erp-finance.orm.xml:655-694`）：19 列（id..status=11 / `closedBy`=12 :670 / `closedAt`=13 :671 / delVersion=14 / version=15 / createdBy=16 / createTime=17 / updatedBy=18 / updateTime=19），**无 `reversedBy`/`reverseCloseReason`/`reverseCloseAt` 列**；**propId 20/21/22 空闲**；tagSet=`gid,erp.finance`（无 `audit,audit-save`）；
  - 全仓 `rg "reversedBy\|reverseCloseReason\|reverseCloseAt\|ReverseCloseLog\|reverseCloseLog"`（排除 docs/）= **0 命中**。
- **A4.1.20 降级证据评估（2026-08-06，维持 P1 不降级）**：通用 `updatedBy`/`updateTime` 经平台 `OrmTimestampHelper.onUpdate` + `EntityPersisterImpl.queueUpdate` 自动填充**确被反结账覆盖**（操作人+时间部分可追）BUT 可靠性边界受限——①无 reason（合规硬伤）②updateTime 非专属时间戳 ③易被后续更新覆盖 ④无结构化操作日志 → L1「全程审计[操作人/原因]」reason 维度仍功能完全缺失 → **P1 维持不降级**；修复归 MR1（有降级证据 + 无活跃数据破坏 → 可排 P0 之后但须实现）。
- **预授权判据（2026-08-12 批量裁决 A 类）**：ORM 结构变更（`ErpFinAccountingPeriod` 加 3 可空列或新 ReverseCloseLog 实体）**批量授权**（对齐 Q3 纯加性类自动执行范围；**越界回落双独立子 agent 批准**——若执行中发现需改既有语义/加 UK/迁移 → 暂停人工）；**会计过账逻辑[反结账]部分（reverseClose 契约变更 + 审计落库）按 roadmap 预授权声明（`requirement-compliance-roadmap.md:13` 2026-08-15 用户裁决升级「ORM/会计核心/数据删除等保护区域允许 AI 自动修改，但必须经两个独立子 agent 分别检查批准」+ `:17` 会计过账逻辑变更类 + `:29` 2026-08-08 A2 裁决「越界项标准流程：独立 fix plan + plan-audit + 双独立子 agent 批准 checkbox」+ `docs/context/ai-autonomy-policy.md:79,81,83` 人工扩展授权登记）须 双独立子 agent 批准 + 独立 plan-audit**。roadmap RC-R1.44 行 `todo`，Deps（R1.0 done）已满足。
- **实仓参照（可复用模式）**：`ErpFinPostingException`（orm.xml:1615-1672）`resolutionNote`(15, VARCHAR 500)/`resolvedBy`(16, `stdDomain="userId"`)/`resolvedAt`(17, `tagSet="clock"`) 三列 + 写入范式 `ErpFinPostingExceptionIgnoreProcessor:40-42`（`setResolutionNote; setResolvedBy(currentUserId()); setResolvedAt(CoreMetrics.currentTimestamp())`）；`ErpFinBadDebt.reason`（orm.xml:1690, propId 12, VARCHAR 500）reason 列先例；结账对称写入 `ErpFinAccountingPeriodClosePeriodProcessor:93-94`（`setClosedAt(CoreMetrics.currentTimestamp()); setClosedBy(facade.currentUserId())`）；`ErpFinAccountingPeriodProcessor.currentUserId:709-716`（null-safe `IUserContext.get().getUserId()`）。
- **调用面（变更影响）**：BizModel Facade `ErpFinAccountingPeriodBizModel.reverseClose:69-72`（@BizMutation 委托 Processor）；xbiz `<actions/>` 空（全部 Java @BizMutation）；GraphQL mutation `ErpFinAccountingPeriod__reverseClose` 消费点——`period-close-wizard/main.page.yaml:408`（`mutation($pid:Long){...}`）+ `main.flux.yaml:153-164` + `ErpFinAccountingPeriod.view.xml:72` + action-auth `erp-fin.action-auth.xml:56-58`（FNPT:ErpFinAccountingPeriod:reverseClose）+ i18n + **E2E spec `tests/e2e/business-actions/fin-period-close-wizard.action.spec.ts:171-174`（GraphQL `callMutationOk('ErpFinAccountingPeriod','reverseClose',{periodId},'id status')` 直调无 reason 参数 + `expect(status).toBe('OPEN')` 断言）+ `tests/e2e/visual/fin-period-close-wizard.visual.spec.ts:79-117`（按钮/步骤渲染）**。**前端 wizard 无 reason 输入控件**。
- **测试基线**：6 个测试类调用 reverseClose（全部经 `periodBiz`/`periodCloseBiz` 接口 Bean 直接调用，非 GraphQL）：`TestErpFinReverseClose`（:36）/`TestErpFinModuleCloseOrder#testReverseCloseApprovalBlocked`（:85，kill-switch 默认 true 阻断断言）/`TestErpFinPeriodStateMachine`（:62/81/91）/`TestErpFinAnnualClose#testReverseCloseBlockedWhenNextYearExists`（:83）/`TestErpFinPeriodCloseEndToEnd#testFullChain`（:56）/`TestErpFinPeriodClosePerf`（:97）。测试 yaml 设 `reverse-close-approval-required: false`：`period-close-end-to-end-test.yaml:8` / `annual-close-test.yaml:10` / `period-close-state-machine-test.yaml:2` / `auxiliary-recon-gate-test.yaml:5`。E2E `tests/e2e/business-actions/fin-period-close-wizard.action.spec.ts`（playwright.config.ts webServer 设 `-Derp-fin.reverse-close-approval-required=false`）。**reason 参数 + 审计字段零测试覆盖**（A4.1.20 §3）。
- **涉及文件**：`module-finance/model/app-erp-finance.orm.xml`（ErpFinAccountingPeriod 加 3 列，propId 20/21/22）；`IErpFinPeriodCloseBiz.java`（reverseClose 签名增 reason）；`ErpFinAccountingPeriodReverseCloseProcessor.java`（审计落库）；`ErpFinAccountingPeriodBizModel.java`（Facade 传参）；`ErpFinErrors.java`（新 ErrorCode）；`period-close-wizard/main.page.yaml` + `main.flux.yaml` + `ErpFinAccountingPeriod.view.xml`（reason 输入控件，Scope 裁决见 Phase 1）；**E2E spec `fin-period-close-wizard.action.spec.ts` + `fin-period-close-wizard.visual.spec.ts`（reason 传参适配）**；测试 6 类 + 新增；owner doc `period-close.md` + arm-index/roadmap/`docs/logs/`（回填）。

## Goals

- **RC-9 反结账全程审计运行时成立（P1-RC-006 核心）**：反结账操作记录**操作人（reversedBy）+ 原因（reverseCloseReason）+ 时间（reverseCloseAt）** 专属审计轨迹，reason 参数经契约链（IErpFinPeriodCloseBiz → BizModel Facade → Processor）传递并落库——消除 A4.1.20 ①无 reason 合规硬伤 + ②非专属时间戳（新增专属列）+ ③易被覆盖（专属列不被后续无关更新覆盖）；④结构化操作日志维度按 Phase 1 裁决（tagSet audit 增强 or 既有 ErpFinAccountingPeriodStatus 轨迹补充，见 Decision D3）。
- **审计写入范式**：对齐 `ErpFinPostingException` resolutionNote/resolvedBy/resolvedAt 三列写入范式（`setReverseCloseReason(reason)` + `setReversedBy(facade.currentUserId())` + `setReverseCloseAt(CoreMetrics.currentTimestamp())`），写入点 = ReverseCloseProcessor 状态翻转段（setStatus(OPEN) 后、reopenModules 前）。
- **契约兼容**：`reverseClose(periodId, reason, context)`——reason 参数经 Phase 1 D1 裁决必填/可选语义（选项见下）；GraphQL 契约经 BizModel 参数注解自动暴露。
- **测试**：① reason 落库断言（reverseClose 传 reason → 行字段 reversedBy/reverseCloseReason/reverseCloseAt 断言，对齐 ErpFinPostingException 断言范式）；② reason 缺失行为断言（按 D1 语义：必填拒绝 or 可选 null 落库）；③ 既有 6 测试类调用点适配（签名变更）零回归；④ kill-switch 阻断路径不变（approval-required=true 仍拒绝，reason 无关）。
- **零回归**：既有 finance 测试全绿（`TestErpFinReverseClose`/`TestErpFinPeriodCloseEndToEnd`/`TestErpFinAnnualClose`/`TestErpFinPeriodClosePerf` 等）+ **E2E spec（action + visual）reason 适配后全绿** + 全仓构建 + compliance checker 零漂移（ORM 3 加列属 Q3 纯加性授权；零新增 daoFor/import 面——审计写入全经 ORM to-one getter/既有 helper）。
- **owner doc 收敛**：`period-close.md §反结账流程` 补审计轨迹实现注记（3 列 + reason 契约 + 写入范式 + 与既有 kill-switch 关系）；不修改需求契约段（use-cases L1 不动）。
- **回填**：arm-index P1-RC-006 → `done (RC-R1.44)` + roadmap 行 → `done` + `docs/logs/` 日志条目。

## Non-Goals

- **不实现完整反结账审批流**（P1-MA3-036/P1-MA2-020 successor——本行只补审计轨迹，kill-switch `reverse-close-approval-required` 行为不变）。
- **不改变反结账冲销逻辑本身**（reverseCloseVoucher 红冲 + reopenModules 行为零变化——本行只在其上附加审计写入）。
- **不新增 ErpFinReverseCloseLog 实体**（除非 Phase 1 D2 裁决推翻——3 可空列方案更轻、对称 closedBy/closedAt、A 类裁决两选项等价，倾向 3 列）。
- **不实现 NopSysChangeLog 平台变更日志接入**（tagSet audit 增强 = 结构性 tagSet 变更超出 A 类纯加性授权，须 ask-first——归 D3 裁决，倾向不纳入，见 Deferred）。
- **不实现前端完整反结账向导改造**（reason 输入控件最小接线，见 Phase 1 D4 裁决；不重构 wizard 流程）。
- **不改真相源契约段落**（use-cases L1 不动；period-close.md 契约段不动，仅补实现注记）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧修复：ORM 纯加性 3 列[A 类批量授权] + 会计过账逻辑[反结账]契约变更[双独立子 agent 批准]；Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/finance/use-cases.md`（L1 UC-FIN-07 RC-9）+ `docs/design/finance/period-close.md`（§反结账流程）
- Skill Selection Basis: 实现面 = ORM 模型变更（`nop-backend-dev`：orm.xml 模型优先 + 增量重生成）+ Processor/BizModel/ErrorCode 契约变更（`nop-backend-dev`：per-mutation Processor 模式 + 平台辅助工具 CoreMetrics/currentUserId）；前端 reason 输入（`nop-frontend-dev`：page.yaml/flux 最小接线，D4 裁决后落地）；测试（`nop-testing`：JunitBaseTestCase 直断言 + 签名适配回归 + E2E spec 适配）。

## Infrastructure And Config Prereqs

- 无新 config key/环境变量/外部服务（kill-switch config 既有，行为不变）。
- ORM 变更触发增量重生成：`mvn clean install -DskipTests`（gen-orm.xgen 增量链，对齐 AGENTS.md）。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-finance/erp-fin-service`。

## Execution Plan

### Phase 1 - 契约语义与载体裁决（Decision）

Status: completed
Targets: `IErpFinPeriodCloseBiz.java`；`ErpFinAccountingPeriodReverseCloseProcessor.java`；`app-erp-finance.orm.xml`（ErpFinAccountingPeriod）；`period-close.md`；wizard 页面
Skill: `nop-backend-dev`

- Item Types: `Decision | Proof`
- Prereqs: 无（既有基线）

- [x] `Decision` **D1 reason 参数语义**：**选项 A（选定）** = 必填（`@Name("reason")` 非空，缺失抛新 ErrorCode `ERR_REVERSE_CLOSE_REASON_REQUIRED`——L1「记录反结账操作人/原因」reason 是合规必备项，A4.1.20 硬伤即 reason 不可追，必填从源头保证可追）；**选项 B（否决）** = 可选（null 允许，缺失不落 reason——向后兼容但无法保证合规）。**理由**：Q4 强制实现 + A4.1.20 reason 不可追是 P1 维持的核心 hinge，必填是唯一能消除合规硬伤的语义；调用面仅 6 测试类 + wizard（均可适配）。**残留风险（显式）**：若选 B（否决项）：reason 可能缺失 → 审计不完整。选定 A 后残留：GraphQL 契约破坏性变更（旧客户端直调 reverseClose(periodId) 将收到 ERR_REVERSE_CLOSE_REASON_REQUIRED）——本项目消费面已全量适配（E2E spec + wizard + 测试），外部 API 消费方为 next-major 契约升级项。
      - Skill: `nop-backend-dev`
- [x] `Decision` **D2 审计存储载体**：**选项 A（选定）** = `ErpFinAccountingPeriod` 加 3 可空列 `reversedBy`（propId 20，`stdDomain="userId"`，VARCHAR 36）/`reverseCloseReason`（propId 21，VARCHAR 500，对齐 ErpFinBadDebt.reason precision）/`reverseCloseAt`（propId 22，`tagSet="clock"`，TIMESTAMP）——对称 closedBy/closedAt，单实体读审计免 join，A 类裁决两选项之一；**选项 B（否决）** = 新增 `ErpFinReverseCloseLog` 实体（独立审计轨迹实体，但需新实体 + 新 dao 面 + 查询接线，且历史反结账无行可查——本行从空实现起步，3 列更简单且经生成链自动暴露）。**理由**：3 列方案与结账对称（closedBy/closedAt 先例）、纯加性最小面、生成链自动同步（RC-R1.43 实证）。**残留风险（显式）**：同期间可多次反结账（`TestErpFinPeriodStateMachine.testForwardAndReverse` 实证 CLOSED_FINAL→OPEN→re-close 循环），第二次反结账将覆盖 `reversedBy/reverseCloseReason/reverseCloseAt` 仅保留末次记录——「全程审计」字面与单行承载的张力（A4.1.20 边界③「易被覆盖」的残留形态：专属列免于无关更新覆盖，但不免下次反结账覆盖）；缓解 = 专属列 + 期间状态轨迹（`ErpFinAccountingPeriodStatus.statusRecords`）可组合追溯；多级反结账审计历史（逐次保留）归 Deferred「结构化操作日志」条目的 successor 触发条件（审计/合规场景要求多级 change-trail 时立项）。
      - Skill: `nop-backend-dev`
- [x] `Decision` **D3 结构化操作日志维度**：**选项 A（选定）** = 不纳入本行（3 列专属审计已覆盖「操作人/原因/时间」L1 字面；tagSet `audit,audit-save` 增强触发 NopSysChangeLog 属结构性 tagSet 变更超出 A 类授权 → Deferred 登记）；**选项 B（否决）** = 经既有 `ErpFinAccountingPeriodStatus.statusRecords`（to-many，orm.xml:680）补反结账记录行（status=OPEN + 时间戳，但无 reason 承载列——需加列超出授权）。**理由**：A4.1.20 ④「无结构化操作日志」是可靠性边界非 L1 字面要求（L1 仅要求操作人/原因/时间），3 列即满足验收标准；结构化日志归 successor。**残留风险**：无结构化 change-trail，逐次反结账历史（含 reason 演进）不可查询，外部审计如需多级审计日志须触发 successor 立项。
      - Skill: `nop-backend-dev`
- [x] `Decision` **D4 前端接线范围**：**选项 A（选定）** = wizard 最小接线（`period-close-wizard/main.page.yaml` reverseClose 确认动作增 reason 输入控件 + mutation 传参 + `main.flux.yaml` 同步——原因必须可录入才能端到端达成「记录原因」）；**选项 B（否决）** = 纯后端（reason 仅 API 层承载，前端后续追加——反结账操作员无法录入原因，端到端不可达）。**理由**：L1 场景是操作员反结账，无 UI 录入则 reason 必填会阻塞 wizard 现有调用；D1 必填 + D4 前端接线须同步落地。**残留风险（显式）**：flux 页面结构核对（`main.flux.yaml` 为 flux 渲染目标，`main.page.yaml` 为 legacy 页面——两文件同步修改须经 E2E visual spec 渲染核对；`ErpFinAccountingPeriod.view.xml` row action 无独立 reason 输入窗（行级反结账按钮无 reason 控件），reason 必填后行级按钮将报错——Phase 4 处理：view.xml row action 增 reason 输入 dialog 最小接线）。
      - Skill: `nop-frontend-dev`
- [x] `Proof` **调用面全集确认**：reverseClose 全部生产调用方（BizModel Facade `ErpFinAccountingPeriodBizModel.reverseClose:69-72` + wizard `main.page.yaml:408` + `main.flux.yaml:153-170` + `ErpFinAccountingPeriod.view.xml:72` + action-auth `erp-fin.action-auth.xml:56-58`（FNPT 资源，权限名不变，零改动）+ **E2E spec `fin-period-close-wizard.action.spec.ts:171-174` 直调 + `fin-period-close-wizard.visual.spec.ts:117-131` 渲染/清理路径**）与 7 个测试调用点基线确认（6 类：`TestErpFinReverseClose:36`/`TestErpFinModuleCloseOrder:85`/`TestErpFinPeriodStateMachine:62,81,91`/`TestErpFinAnnualClose:83`/`TestErpFinPeriodCloseEndToEnd:56`/`TestErpFinPeriodClosePerf:97`；`TestErpFinAccountingPeriodStateMachineMatrix` 零调用点——实仓 grep 实证）；`ErpFinPostingException` 三列写入范式（orm.xml:1633-1635 resolutionNote/resolvedBy/resolvedAt）+ `ErpFinAccountingPeriodProcessor.currentUserId:709-716`（null-safe）+ `CoreMetrics.currentTimestamp()` helper 实证。**E2E visual spec:130 finally 清理路径亦直调 reverseClose 无 reason——一并纳入 Phase 4 适配清单（原计划遗漏点，draft review 后执行期发现，按调用面全集一致性处理）。**
      - Skill: `nop-testing`

Exit Criteria:

- [x] D1-D4 裁决记录落盘计划（选择 + 备选 + 理由 + 残留风险），调用面全集清单（生产 4 点 + 测试 6 类）产出
- [x] ORM 3 列 propId 分配（20/21/22 空闲实证——orm.xml:672-677 propId 14-19 为 delVersion/version/createdBy/createTime/updatedBy/updateTime，20/21/22 空闲确认）与生成链机制确认（RC-R1.43/R1.40 先例：`mvn clean install -DskipTests` 增量链）

### Phase 2 - ORM 3 列 + 增量重生成（A 类批量授权变更）

Status: completed
Targets: `module-finance/model/app-erp-finance.orm.xml`（ErpFinAccountingPeriod）
Skill: `nop-backend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1 D2 裁决（3 列方案选定）

- [x] `Add` `app-erp-finance.orm.xml` ErpFinAccountingPeriod 增 3 可空列（按 D2 裁决）：`reversedBy`（propId 20，`stdDomain="userId"`，VARCHAR 36，无 NOT NULL/默认/索引/UK）+ `reverseCloseReason`（propId 21，VARCHAR 500）+ `reverseCloseAt`（propId 22，`tagSet="clock"`，TIMESTAMP）——**均无 NOT NULL/无默认值/无索引/无 UK**（Q3 纯加性授权范围；⚠ propId 不可占用 14-19 审计列，错赋抛 `ERR_ORM_MODEL_DUPLICATE_PROP_ID`，对齐 RC-R1.43 先例显式声明 20/21/22）。
      - Skill: `nop-backend-dev`
- [x] `Proof` 增量重生成验证：`mvn clean install -DskipTests` 触发 gen-orm.xgen → 生成产物核对（Entity 3 getter/setter + xmeta 3 字段 + DDL 三方言同步，无结构性漂移）+ propId 分配核对。
      - Skill: `nop-testing`

Exit Criteria:

- [x] ORM 3 列落地（orm.xml/Entity/xmeta/DDL 四同步 grep 核对），`mvn clean install -DskipTests` 生成链通过，分域编译通过

### Phase 3 - 契约变更 + 审计落库（会计过账逻辑，双独立子 agent 批准门控）

Status: completed
Targets: `IErpFinPeriodCloseBiz.java`；`ErpFinAccountingPeriodReverseCloseProcessor.java`；`ErpFinAccountingPeriodBizModel.java`；`ErpFinErrors.java`
Skill: `nop-backend-dev`

- Item Types: `Fix | Add | Decision`
- Prereqs: Phase 2 完成

- [x] `Proof` **双独立子 agent 批准（会计过账逻辑[反结账]变更门控，硬门）**：两个独立子代理（fresh session，无执行者上下文）分别检查批准本 Phase 的 reverseClose 契约变更 + 审计落库实现（批准记录落盘本计划 Draft Review Record/Closure 段，对齐 2026-08-08 A2 裁决「批准人=两个独立子 agent 分别检查批准」+ roadmap 预授权声明「会计过账逻辑变更：须双独立子 agent 批准 + 独立 plan-audit」）。批准前置条件：变更不改变红冲/回开业务语义（纯附加审计写入 + 参数传递），Q4 收敛方向（向 owner doc 契约收敛）。
      - Skill: `nop-backend-dev`
- [x] `Fix` `IErpFinPeriodCloseBiz.reverseClose` 签名增 `@Name("reason") String reason`（按 D1 语义必填，javadoc 同步 RC-9 审计契约）；`ErpFinAccountingPeriodBizModel.reverseClose` Facade 传参。
      - Skill: `nop-backend-dev`
- [x] `Fix` `ErpFinAccountingPeriodReverseCloseProcessor.reverseClose` 增审计落库：reason 必填守卫（D1 选项 A 时缺失抛 `ERR_REVERSE_CLOSE_REASON_REQUIRED`）+ setStatus(OPEN) 后、reopenModules 前写 `setReverseCloseReason(reason)`/`setReversedBy(facade.currentUserId())`/`setReverseCloseAt(CoreMetrics.currentTimestamp())`（对齐 ErpFinPostingExceptionIgnoreProcessor 写入范式）；红冲/回开逻辑零变化。
      - Skill: `nop-backend-dev`
- [x] `Add` `ErpFinErrors` 新 ErrorCode（`ERR_REVERSE_CLOSE_REASON_REQUIRED`，描述中文；i18n 资源键在 Phase 4 与 wizard 输入控件一并落地——**i18n 落 Phase 4 具体项，非「按需」**；D1 选项 A 时）。
      - Skill: `nop-backend-dev`
- [x] `Fix` 6 个既有测试类 reverseClose 调用点适配（reason 参数传入，按 D1 语义必填场景补理由文本）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 双独立子 agent 批准记录落盘（批准人 2 个独立子代理 + 结论），reverseClose 契约链（IBiz→BizModel→Processor）reason 传参贯通 + 审计 3 字段落库（grep 证据）+ 既有 6 测试类适配编译通过
- [x] 红冲/回开逻辑零变更（git diff 仅审计写入 + 参数传递）

### Phase 4 - 前端 reason 输入 + E2E 适配 + 测试 + 文档回填

Status: completed
Targets: `period-close-wizard/main.page.yaml`；`main.flux.yaml`；`ErpFinAccountingPeriod.view.xml`；E2E spec `fin-period-close-wizard.action.spec.ts` + `fin-period-close-wizard.visual.spec.ts`；新增测试；`period-close.md`；arm-index/roadmap/`docs/logs/`
Skill: `nop-frontend-dev` + `nop-testing`

- Item Types: `Add | Proof | Fix`
- Prereqs: Phase 3 完成

- [x] `Add`（D4 选项 A）wizard 最小接线：`main.page.yaml` 反结账确认动作增 reason 输入控件（text 必填）→ mutation `ErpFinAccountingPeriod__reverseClose(periodId:$pid, reason:$reason)`；`main.flux.yaml` + `view.xml` 同步（对齐 flux 渲染模式，`E2E_ENGINE`/页面模型约定见 `docs/architecture/view-and-page-strategy.md`）；i18n 资源键（新 ErrorCode + reason 输入 label）一并落地。
      - Skill: `nop-frontend-dev`
- [x] `Fix` **E2E spec 适配（MAJOR-1 修复，reason 必填致既有直调必失败）**：`fin-period-close-wizard.action.spec.ts:171-174` 的 `callMutationOk('ErpFinAccountingPeriod','reverseClose',{periodId},'id status')` 增 `reason` 传参（D1 语义必填时须显式提供理由文本；若执行期 D1 裁决为可选则补 null 兼容断言——按最终 D1 裁决落地）+ `expect(reversed.status).toBe('OPEN')` 断言保持；`fin-period-close-wizard.visual.spec.ts:79-117` 渲染核对（reason 输入控件出现）。**E2E 运行按 runbook 强制 flux 渲染模式（`E2E_ENGINE` 缺省即 flux，见 `docs/testing/e2e-runbook.md`）**。
      - Skill: `nop-testing`
- [x] `Add` 新增 `TestErpFinReverseCloseAuditTrail`（或扩展既有类）：① reason 落库断言（reverseClose 传 reason → reversedBy=当前用户/reverseCloseReason=reason/reverseCloseAt 非空，对齐 ErpFinPostingException 断言范式）；② reason 缺失拒绝断言（D1 选项 A 时 ERR_REVERSE_CLOSE_REASON_REQUIRED + 状态保持 CLOSED_FINAL + 零审计字段写入）；③ 既有 6 测试类回归（签名适配后全绿）。
      - Skill: `nop-testing`
- [x] `Add` owner doc 注记：`period-close.md §反结账流程` 补审计轨迹实现注记（3 列 + reason 契约 + 写入范式 + 与 kill-switch 关系 + 结构化操作日志 successor 声明 + 多次反结账覆盖残留边界）；`use-cases.md` 不动。
      - Skill: `nop-backend-dev`
- [x] `Proof` 零回归验证：`mvn test -pl module-finance/erp-fin-service` 全绿 + **E2E spec 运行（`fin-period-close-wizard.action.spec.ts` + `visual.spec.ts`，flux 模式）** + `mvn clean install -DskipTests` 全量构建 + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline（零新增 daoFor/import 面）+ 回填（arm-index P1-RC-006 → done (RC-R1.44) + roadmap 行 done + `docs/logs/2026/08-15.md` 日志条目——执行期为 2026-08-16，按日志指南落 `docs/logs/2026/08-16.md`）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 新测试全绿（①-③）+ 既有 finance 测试零回归 + **E2E spec（action + visual）reason 适配后全绿** + wizard reason 接线落地（页面核对）
- [x] owner doc 注记 + 三处回填（arm-index/roadmap/log）+ compliance checker 零漂移

## Draft Review Record

- Independent draft review iteration 1: `needs revision` (`ses_ffa61d443ffelyA1SBq8i99oda`) — 1 MAJOR + 4 MINOR。MAJOR-1 已修正：**E2E GraphQL 消费点遗漏**——`tests/e2e/business-actions/fin-period-close-wizard.action.spec.ts:171-174` 经 `callMutationOk('ErpFinAccountingPeriod','reverseClose',{periodId},'id status')` 直调无 reason 参数，D1 必填落地后必失败——已纳入 Phase 1 调用面全集清单 + Phase 4 新增 E2E spec 适配项（reason 传参 + visual 渲染核对）+ Phase 4 exit criteria/Closure Gates 增 E2E spec 运行门（flux 模式）。4 MINOR 已修正：(1) 双独立子 agent 批准门控项补 Item Type 标注（`Proof`）；(2) D2 补显式残留风险（同期间多次反结账覆盖审计字段，仅保留末次记录——A4.1.20 边界③残留形态 + 缓解/归属 successor）；(3) i18n「按需」软措辞去除——i18n 落 Phase 4 具体项；(4) 预授权引用归属修正（roadmap 预授权声明 :13/:17/:29 + ai-autonomy-policy :79/:81/:83 人工扩展授权登记）。其余 baseline 声明实仓核实 PASS（契约链/ORM propId 空闲/6 测试类调用点/E2E wizard/门控框架/Q4 强制实现/Deferred 两项 adjudicated）。
- Independent draft review iteration 2: `acceptable` (`ses_ffa56c8aaffeLya6Md52Nxip6U`) — 5/5 修正逐项复核确认（E2E spec 文件路径/内容/runbook flux 模式实证；Proof 类型标注；D2 残留风险；i18n 落位；预授权引用四源）。2 项非阻塞观察已吸收：D2「见 Deferred」指针明确指向「结构化操作日志」条目 successor 触发条件；Phase 1 exit criteria 调用面摘要口径确认非遗漏。共识达成，计划可转 active。
- **Phase 3 双独立子 agent 批准（会计过账逻辑[反结账]变更门控，硬门）2026-08-16 执行**：批准人 = 两个独立子代理（fresh session，互不共享执行者上下文）。
  - **Agent #1（`ses_ff99a6ee4ffezeRyfTUCGcc4X5`）→ APPROVE**：按 plan-audit 审查计划与设计证据——①红冲/回开五行为零变更核验（assertCanReverseClose/kill-switch/次年门控/setStatus(OPEN)/reverseCloseVoucher×2-3+reverseDepreciation/reopenModules 全部保留原位，变更仅 reason 守卫 + setStatus(OPEN) 后 reopenModules 前 3 字段写入）；②Draft Review Record 两轮独立审查合规；③Q4 收敛方向（period-close.md §反结账步骤1/8 + use-cases L1 RC-9）；④D1/D2 与 roadmap RC-R1.44 行 + 2026-08-12 A 类批量裁决一致；⑤ORM 纯加性实证（propId 20/21/22 空闲、无 NOT NULL/默认/索引/UK、四产物同步）；⑥7 个测试调用点 + 2 E2E spec 全覆盖。0 MAJOR；4 MINOR（非阻塞）：reason 守卫位置建议 fail-fast（状态守卫前）、2 个 negative smoke spec 未列入调用面全集（e1-1-finance.smoke.spec.ts:63 / dry-run-impact.smoke.spec.ts:62，经核实 enforcement 先于业务逻辑 → 功能免疫，Phase 4 追记 traceability）、D1 必填为裁决解释（非 A 类字面，落在本门控范围内，可接受）、行号漂移（orm.xml 1636-1638/1693）。
  - **Agent #2（`ses_ff99a4b57ffeAo1NiyzVPy0fkk`）→ APPROVE**：独立复核同一证据——①授权链四源核实准确（roadmap:13/:29/:34/:436 + ai-autonomy-policy:79/:83）；②全仓 `rg reverseClose` 零 xbiz/task.xml/其他 BizModel 间接调用方，census 完备，3 个 E2E negative 站点经论证免疫（enforcement 前置 + 正向断言仅非 NO_PERMISSION）；③守卫/门控排序分析：reason 守卫置于 setStatus(OPEN) 前即满足测试②断言（状态保持 CLOSED_FINAL + 零审计写入），与 kill-switch 相对顺序无害；④同事务保证（@BizMutation 包裹 + flushSession:69）；⑤红冲字节级不变可执行（exit criteria「git diff 仅审计写入 + 参数传递」）；⑥ORM 加性纯度实仓复核（索引/UK 不含新列）；⑦compliance 零漂移成立（R2c 计数 daoFor 零新增 + R12 三命名导入不动 + 新 ErrorCode 落已导入的 ErpFinErrors）；⑧owner doc 对齐（period-close.md:183-186/:216-220）。Residual risks（非阻塞，均已在计划内裁决）：多次反结账覆盖审计列（D2 已裁决 + successor 触发条件）、reversedBy null-safe、census traceability 缺口（negative specs 追记）、守卫位置要求 setStatus 前。
  - **结论**：双 agent 均 APPROVE，批准记录落盘完成（本段），Phase 3 可实施。

## Closure Gates

- [x] 范围内行为完成（P1-RC-006 审计轨迹：reason 契约 + 3 列落库 + 前端接线 + 测试）
- [x] 相关文档对齐（period-close.md 注记 + arm-index P1-RC-006 → done (RC-R1.44) + roadmap 行 done）
- [x] 已运行验证（`mvn clean install -DskipTests` + `mvn test -pl module-finance/erp-fin-service` + **E2E spec（`fin-period-close-wizard.action.spec.ts` + `fin-period-close-wizard.visual.spec.ts`，flux 模式）** + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 结构化操作日志（NopSysChangeLog tagSet audit 增强）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: A4.1.20 ④「无结构化操作日志」是可靠性边界非 L1 字面要求（L1 仅要求操作人/原因/时间，3 列专属审计满足）；tagSet `audit,audit-save` 增强属结构性 tagSet 变更超出 A 类纯加性授权，须 ask-first
- Successor Required: `yes`（触发条件：审计/合规场景要求 NopSysChangeLog 结构化变更日志时，按 ORM ask-first 流程立项）

### 完整反结账审批流

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: P1-MA3-036/P1-MA2-020 既有 successor；本行只补审计轨迹，kill-switch 行为不变
- Successor Required: `no`（既有 successor 追踪已覆盖）

## Closure

Status Note: 计划于 2026-08-16 执行完成并关闭。四 Phase 全部 `[x]` + `Status: completed`；双独立子 agent 批准（Phase 3 硬门）落盘 Draft Review Record（`ses_ff99a6ee4ffezeRyfTUCGcc4X5` + `ses_ff99a4b57ffeAo1NiyzVPy0fkk` 均 APPROVE）。验证全绿：`mvn test -pl module-finance/erp-fin-service` **493/493**（490 基线 + 3 新增 `TestErpFinReverseCloseAuditTrail`）+ E2E 2 spec flux 全绿（action + visual，visual 首跑因 flux wizard `mountOnEnter` 需导航激活 step body 失败后修正，失败记录未隐藏）+ `mvn clean install -DskipTests` 全量 BUILD SUCCESS + compliance checker **actual == baseline 零漂移**（R2c=1399 / R12a=69 / R12b=66 / R12c=40 精确一致）。回填完成：arm-index P1-RC-006 → done (RC-R1.44) + roadmap RC-R1.44 → done ✅ + owner doc period-close.md §反结账流程注记 + `docs/logs/2026/08-16.md` 日志条目（执行期为 2026-08-16，按日志指南落当日文件而非计划书写的 08-15 占位）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（fresh session，`ses_ff979c31dffeDWAjWMjblY1Hgf`）
- Evidence: 独立结束审计 2026-08-16 执行——Gate 1 范围内行为完成 PASS（IBiz 契约 `IErpFinPeriodCloseBiz.java:53-55` + Processor 守卫 `:40-43` 状态守卫前 + 审计写入 `:68-70` setStatus(OPEN) 后 reopenModules 前 + 红冲/回开零变更 git diff 实证 + ORM 3 列 `orm.xml:672-674` 无 NOT NULL/默认/索引/UK + ErrorCode `ErpFinErrors.java:304-306` + 前端三文件接线 + 新测试 3 组）；Gate 2 文档对齐 PASS（period-close.md:230-237 + arm-index:136 + roadmap:436 + log 08-16.md）；Gate 3 验证证据 PASS（test-r1-44-fin2.log 493/0/0 BUILD SUCCESS + e2e-r1-44.log action 通过 + e2e-r1-44-vis.log visual 1 passed 55.5s + build-r1-44-full.log BUILD SUCCESS + checker-r1-44.log 16 规则全 ≤ baseline）；Gate 4 无降级 PASS（Deferred 恰 2 项 pre-adjudicated）；Gate 5 草案审查 PASS（2 轮 + 双 agent 批准记录）；Gate 6 文本一致性 FAIL→审计后由执行者修正（Plan Status active→completed + Closure Gates 8/8 ticked + Status Note/Evidence/Follow-up 填写）；Gate 7 独立结束审计 PASS（本审计即独立子代理，执行者未自我审计）；Gate 8 证据一致性 PASS。总体裁决：**PASS**（1 MAJOR 为文本闭合缺项，审计后已修正；1 MINOR i18n 资源键偏离——ErrorCode 消息无 i18n yaml 键为仓库全仓约定[零 `erp.err.*` i18n 键]，生成 xmeta i18n-en displayName 已覆盖 3 列英文 label，偏离已在日志注记——本计划 Closure 段补注：i18n yaml 手改被仓库保护规则拒绝，i18n 维度由生成层 + 内联约定满足）。

Follow-up:

- git 提交（35 文件实现 + 文档变更未提交，HEAD 仍为草案提交 `c90884665`）——等待人工/CI 常规提交流程，本计划不代提交。
- i18n 资源键偏离（MINOR-1）：如后续需要 `erp.err.fin.period-close.reverse-close-reason-required` 的显式 i18n yaml 条目，须人工解除仓库 `*.i18n.yaml` 编辑保护后补充（当前由 ErrorCode 内联中文 + 生成 xmeta i18n-en displayName 覆盖）。
