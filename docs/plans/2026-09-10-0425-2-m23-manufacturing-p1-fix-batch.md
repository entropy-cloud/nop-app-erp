---
status: active
mission: ai-check-r3
work-item: M2.3
group: "2026-09-10-0425"
verify: [test]
---

# 2026-09-10-0425-2 M2.3 manufacturing P1 修复批（P1-CK-mfg-022-r3 reverseApprove docStatus 守卫）

## Current Baseline

- 批内面 = 恰 1 条 r3 新立 P1：`P1-CK-mfg-022-r3`（open，`docs/audits/check/2026-09-06-1645-ai-check-r3/ck-mfg-workorder-r3.md` §2.3；索引行在案）。manufacturing 域 r3 新立 P1 面仅此一条（M1.5 新立 2 = 本 P1 + P3-CK-mfg-023-r3；M1.6/M1.7 新立全 P3），无同批可合并的其他 P1。
- 缺陷链（执行者 2026-09-10 HEAD 实核三站在位）：`module-manufacturing/erp-mfg-service/src/main/java/app/erp/mfg/service/processor/ErpMfgWorkOrderProcessor.java` 中 `#validateTransitionForReverseApprove`（L247-254，仅审批轴 `assertCanReverseApprove`，来源态判定仅 APPROVED——`ErpMfgWorkOrderApprovalStateMachine` L70-75）× `#doReverseApprove`（L299-307，P1-CK-mfg-002 修复后**无条件回写 `docStatus=DRAFT`** + 清空审批审计字段）× xbiz `reverseApprove` mutation（仅 `ErpMfgWorkOrder:reverseApprove` 权限守卫）——全链**无任何 docStatus 守卫**。工单 approve 后 approveStatus 恒 APPROVED（IN_PROCESS/STOCK_RESERVED/STOPPED/COMPLETED/CLOSED 全程不翻审批轴）→ 对在制/终态工单调 reverseApprove 即被复活为 DRAFT 可编辑可重提态；原 P2-CK-mfg-010 的「终态复活被 assertCanSubmit 挡住」缓解失效；历史 completedQuantity/成本/库存移动/凭证与 DRAFT 态脱钩（重复生产、重复入库、历史计价漂移风险）。
- 意图锚点：`doReverseApprove` 内注释自述「反审核（**未开工前提**）回写 docStatus=DRAFT」——代码自身声明的适用前提即 docStatus=NOT_STARTED；P1-CK-mfg-002 修复（commit `1166339ae`，F2.5 批次）为该叠加行为面的引入点（此前终态单据仅是双轴矛盾态，修复后终态可被直接复活）。
- 测试现状：回归测试 `TestErpMfgReservationLifecycle#testReverseApproveThenResubmit` 仅覆盖 approve→reverseApprove（NOT_STARTED）路径；终态/在制组合零覆盖（M1.5 DIM-T 附 022-r3 测试缺位注记，未重复立项）。mfg service 基线 308/0/0/0（M1.5/M1.6/M1.7 三切片参照值一致）。
- 归并与继承注记：本 finding 系 `P2-CK-mfg-010`（r1，open）同型控制点升级新立（原 ID 状态不动）——本批修复即消解该控制点，状态继承证据落本计划、回填归 M2.9（「同型 finding 状态继承」义务）；`P2-CK-mfg3-012`（r1，open，委外单同型，归 mfg-3 格）finding 报告注明「修复时同步核」——本批只做同控制点核验与证据登记，不修（P2 非 P1 批范围，r1 通道所有）。
- 修复方法约束（r3 M2 前言 + M2.0 owner doc `docs/architecture/finding-remediation-method.md`，其为本计划直接 Prereq）：先写失败测试 → 修复 → 测试绿 + 既有测试零回归；证伪 = 书面 not-a-problem；错误参数传码不传散文（MI.5a 已登记 CAT-2 契约：状态码/枚举名本身）；本批无 ORM/api.xml/seed/页面变更（不触发保护区双批准与 seed 双面重录）。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-09 行（全 reactor 4006/0/0/1 零新增失败；compliance R2b=242/R2c=1542/R12a=71）；CJK `--strict` PASS（CAT1..4 = 0/0/0/0）。
- 剩余差距：reverseApprove 终态/在制组合守卫缺失 + 负路径测试缺位；M2.9 收官需本批 fixed 证据指针。
- 依赖状态：M2.0（plan `2026-09-10-0425-1`，组内 N=1）为本计划 Prereq；本计划组内执行序 N=2。

## Goals

- `P1-CK-mfg-022-r3` 修复落地：reverseApprove 链获得 docStatus 守卫（裁决选项见 Phase 2 Decision），在制/终态工单调 reverseApprove 被显式拒绝（业务错误码 + 传码参数），NOT_STARTED 合法路径行为不变（P1-CK-mfg-002 回归路径保持绿）。
- 先写失败测试：终态/在制组合负路径测试（IN_PROCESS/STOCK_RESERVED/STOCK_PARTIAL/STOPPED/COMPLETED/CLOSED/CANCELLED → 拒绝；NOT_STARTED → 放行控制组）先红后绿，填补 DIM-T 缺位注记。
- 零回归 + 门控持平：mfg service 套件全绿 + 全 reactor `mvn test` 零新增失败；compliance/cjk 双 checker 不高于基线；seed 零改动证明。
- M2.9 消费证据落盘：fixed 证据指针（测试 + 行为注记）+ P2-CK-mfg-010 状态继承注记 + mfg3-012 同控制点核验记录。

## Non-Goals

- 不修 `P2-CK-mfg3-012`（委外同型 P2，r1 通道所有）——仅同控制点核验 + 证据登记；若核验发现升级证据，登记注记归其所属通道，不在本批扩scope。
- 不修 manufacturing 域其他 r3 finding（mfg2-024..028-r3、mfg3-019/020-r3、mfg-023-r3 等 P2/P3——去向以 M2.0 族裁决为准）。
- 不改 ORM/api.xml/seed/页面文件；不做双索引状态回填（M2.9 义务）；不做 roadmap 状态翻转。
- 不重构审批轴/文档轴状态机结构（守卫以最小面接入，不借机改状态机 Bean 语义——既有裁决不重开）。

## Phase 1 — 失败测试先行（终态/在制负路径）

> 统一类型：Proof（2 项 Proof）。
> Skill: bug-diagnosis-prompt（roadmap M2.3 行指定；先读 `docs/skills/bug-diagnosis-prompt.md` 四阶段定位纪律再动手）
> Targets: `module-manufacturing/erp-mfg-service/src/test/java/`（既有工单状态机/生命周期测试类扩展，落点执行时按现有测试布局选定，优先 `TestErpMfgWorkOrderApprovalStateMachineMatrix` / `TestErpMfgWorkOrderDocumentStateMachineMatrix`（statemachine 包）/ `TestErpMfgReservationLifecycle` 同族）
> Prereqs: M2.0 计划完成（修复方法 owner doc 在位）

- [ ] <Proof> 写失败测试：对 IN_PROCESS/STOCK_RESERVED/STOCK_PARTIAL/STOPPED/COMPLETED/CLOSED/CANCELLED 七种 docStatus（approveStatus=APPROVED 前置态——七者均审批轴不翻：STOCK_PARTIAL 系 IN_PROCESS 部分齐套开工在制态、CANCELLED 系 NOT_STARTED 取消可达[assertCanCancel 白名单含 NOT_STARTED]）逐组合断言 reverseApprove 被业务异常拒绝；NOT_STARTED 控制组断言放行且双轴回写行为与现状一致（DRAFT + 审计字段清空 + 可重提）。断言可观察行为（异常错误码 + docStatus 不变），非仅类型。执行确认红：现行为复活终态单据 → 测试失败，失败输出记入勾选注记（缺陷复现证据）。
      - Skill: bug-diagnosis-prompt
- [ ] <Proof> 复现定位记录：按 bug-diagnosis 四阶段将缺陷链（守卫缺失站点 ×3 + 引入点 `1166339ae`）与 finding 报告证据对账，确认无第三站点（工单外实体是否共享该 Processor 族——grep 实核注记；委外单归 mfg3-012 不入本批）。
      - Skill: bug-diagnosis-prompt

Exit Criteria:

- [ ] 负路径失败测试在位且当前树红（七组合复现 + NOT_STARTED 控制组绿），失败输出注记在案
- [ ] 缺陷链与 finding 报告逐站对账一致，无未登记第三站点

## Phase 2 — 守卫修复（Decision + Fix + Add）

> 统一类型：Decision | Fix | Add（1 Decision + 1 Fix + 1 Add）。
> Skill: bug-diagnosis-prompt
> Targets: `ErpMfgWorkOrderProcessor.java`（validateTransitionForReverseApprove / doReverseApprove）、`ErpMfgErrors`（新增反审核 docStatus 越界错误码）
> Prereqs: Phase 1 完成（失败测试在位）

- [ ] <Decision> 守卫方案裁决：推荐 (a) `validateTransitionForReverseApprove` 增 docStatus 白名单校验 = 仅 `NOT_STARTED` 放行（依据：`doReverseApprove` 注释自述「未开工前提」+ P1-CK-mfg-002 修复意图即服务未开工反审核；SUBMITTED 与 approveStatus=APPROVED 正常不相交，白名单收窄到 NOT_STARTED 不损失合法路径）。替代方案：(b) 排除式黑名单（排除终态 + IN_PROCESS/STOPPED/STOCK_*）——被否，白名单语义更严且与「未开工前提」自述一致，黑名单对未预见新状态开放；(c) `doReverseApprove` 回写前校验 `docStatus ∈ {SUBMITTED, NOT_STARTED}`——被否，守卫应置于校验 step（与 validateTransitionFor* 既有分层模式一致），do 层保持纯执行。残余风险：未来若出现「已开工可反审核」产品需求，须走 owner doc（`docs/design/manufacturing/state-machine.md`）修订而非放宽白名单——在勾选注记显式登记。裁决与 owner doc 对账：核对 state-machine.md 反审核语义描述一致，发现漂移即先修 doc 注记再落码。
      - Skill: bug-diagnosis-prompt
- [ ] <Fix> 接入守卫：`validateTransitionForReverseApprove` 在审批轴断言后增 docStatus 白名单校验，拒绝路径抛业务异常（错误码见下项）；`doReverseApprove` 不改（纯执行层保持）。行为不变式：NOT_STARTED 路径逐字节现状保持。
      - Skill: bug-diagnosis-prompt
- [ ] <Add> 错误码：`ErpMfgErrors` 增反审核 docStatus 越界错误码（中文描述合规——ErrorCode.define 中文为源语言，经 i18n 后期补英文；`@Locale("zh-CN")` 接口注解已在位[MI.1]）；异常参数传码不传散文（沿 `e.param(ARG_WORK_ORDER_CODE, code)` 既有模式，状态参数传 docStatus 枚举名/码本身——MI.5a CAT-2 契约）。
      - Skill: bug-diagnosis-prompt

Exit Criteria:

- [ ] Phase 1 负路径测试全绿（七组合拒绝 + 控制组放行），`testReverseApproveThenResubmit` 等 P1-CK-mfg-002 既有回归保持绿
- [ ] 守卫落在校验 step 层、do 层零改动；错误码与传参符合 CAT-2 传码契约；Decision 三要素（选择/替代/残余风险）注记在案

## Phase 3 — 批级证明与收官门控

> 统一类型：Proof（3 项 Proof）。
> Skill: none
> Targets: 无新增代码；验证输出与 M2.9 消费证据记入勾选注记
> Prereqs: Phase 2 完成

- [ ] <Proof> 域级回归：`mvn test -pl module-manufacturing/erp-mfg-service -am` 全绿零新增失败（基线 308/0/0/0，数字记入注记）
      - Skill: none
- [ ] <Proof> 收官门控：全 reactor `mvn test` 零新增失败（对照 2026-09-09 基线行 4006/0/0/1）+ `bash docs/audits/nop-compliance-checker.sh` exit 0 逐规则持平（R2b=242/R2c=1542/R12a=71；漂移即停走独立基线裁决）+ `node tools/check-hardcoded-cjk.mjs --strict` PASS + `git status --porcelain app-erp-all/src/main/resources/_vfs/_init-data/` 空（seed 零改动证明；seed 实际落点为 app-erp-all `_vfs/_init-data/`，路径须匹配 tracked 文件否则 porcelain 恒空不构成证明）+ `mvn clean install -DskipTests` BUILD SUCCESS
      - Skill: none
- [ ] <Proof> M2.9 消费证据落盘（本计划勾选注记，不改索引）：P1-CK-mfg-022-r3 fixed 证据指针（测试类/方法 + 守卫站点）；P2-CK-mfg-010 状态继承注记（控制点消解 + 证据指针，回填归 M2.9）；mfg3-012 同控制点核验记录（委外 Processor 是否同型缺守卫——实核结论 + 若同型仅登记不修）；seed/ORM/api.xml 零触碰声明
      - Skill: none

Exit Criteria:

- [ ] mfg service 全绿 + 全 reactor 零新增失败 + 双 checker 持平基线 + seed 零改动证明在案
- [ ] M2.9 消费证据三件（fixed 指针 / 010 继承注记 / 012 核验记录）落盘于计划注记

## Draft Review Record

- dispatch review #review-2026-09-09-210030-mission-driver-2026-09-10-0425-2-m23-manufacturing-p1-fix-batch-1-5f3dca61 to 2026-09-09-210030-mission-driver
- 2026-09-10：iteration 1，共识 approved #review-2026-09-09-210030-mission-driver-2026-09-10-0425-2-m23-manufacturing-p1-fix-batch-1-5f3dca61

## Verification

## Closure
