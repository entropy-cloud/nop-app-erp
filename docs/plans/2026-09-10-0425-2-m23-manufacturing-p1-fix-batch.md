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

- [x] <Proof> 写失败测试：对 IN_PROCESS/STOCK_RESERVED/STOCK_PARTIAL/STOPPED/COMPLETED/CLOSED/CANCELLED 七种 docStatus（approveStatus=APPROVED 前置态——七者均审批轴不翻：STOCK_PARTIAL 系 IN_PROCESS 部分齐套开工在制态、CANCELLED 系 NOT_STARTED 取消可达[assertCanCancel 白名单含 NOT_STARTED]）逐组合断言 reverseApprove 被业务异常拒绝；NOT_STARTED 控制组断言放行且双轴回写行为与现状一致（DRAFT + 审计字段清空 + 可重提）。断言可观察行为（异常错误码 + docStatus 不变），非仅类型。执行确认红：现行为复活终态单据 → 测试失败，失败输出记入勾选注记（缺陷复现证据）。**〔执行注记 2026-09-10〕落点 = `TestErpMfgReservationLifecycle`（P1-CK-mfg-002 回归同族类）新增两法：`testReverseApproveRejectedForInProcessAndTerminalDocStatus`（七组合 × 断言错误码 + docStatus/approveStatus 双轴不变）+ `testReverseApproveNotStartedControlGroupUnchanged`（控制组：放行 + DRAFT 回写 + approveStatus=REJECTED + approvedBy/approvedAt 清空 + 可重提）；断言强度 = 可观察行为（`resp.getCode()` 错误码 + 实体重读状态断言），非仅类型。红确认：`mvn test -Dtest=...#两法` → `Tests run: 2, Failures: 1`——负路径法于首个组合 IN_PROCESS 失败：`ApiResponse[status=0,...]` expected `<erp.err.mfg.work-order.reverse-approve-doc-status-forbidden>` but was `<null>`——即 reverseApprove 对在制工单**放行成功**（status=0 = DRAFT 复活路径可达，缺陷直接复现）；控制组同轮 PASS（NOT_STARTED 现行为正确）。七组合共享单一守卫调用链（红轮首组合即截断），修复后全 7 组合逐组合断言绿（见 Phase 2 证据）。**
      - Skill: bug-diagnosis-prompt
- [x] <Proof> 复现定位记录：按 bug-diagnosis 四阶段将缺陷链（守卫缺失站点 ×3 + 引入点 `1166339ae`）与 finding 报告证据对账，确认无第三站点（工单外实体是否共享该 Processor 族——grep 实核注记；委外单归 mfg3-012 不入本批）。**〔执行注记 2026-09-10〕四阶段对账：①根因 = reverseApprove 全链无 docStatus 守卫（假设消解：非下游状态机/权限层问题——三站逐一实核）；②三站在位 HEAD 实核全一致——`validateTransitionForReverseApprove`（仅审批轴 `assertCanReverseApprove`，ApprovalStateMachine L70-75 仅 APPROVED 判定）× `doReverseApprove`（无条件回写 docStatus=DRAFT + 清审计字段）× `ErpMfgWorkOrder.xbiz` reverseApprove mutation（标准审批 xbiz 仅权限守卫通道，无业务守卫）；③无第三站点（grep 实核）：`ErpMfgWorkOrderProcessor` reverseApprove 家族唯一消费方 = `ErpMfgWorkOrderReverseApproveProcessor`（本实体），共享骨架 `AbstractReverseApproveProcessor` 已合规（commit `4dab22be3` → rejectedStatus()=REJECTED）非缺陷站点，mfg 域内 SubcontractOrder 为平行独立家族（`ErpMfgSubcontractOrderProcessor`，P2-CK-mfg3-012 同型控制点，本批仅登记不修——见 Phase 3 核验记录）；④引入点实核勘误：git `-S` 实证 doReverseApprove 无条件 DRAFT 回写由 F2.5 提交 `a7058c18f`（2026-08-28，mfg-002/003/004/005 P1 簇修复）引入，`1166339ae`（2026-09-08，姊妹计划 2026-09-07-2200-1 直抛领域码批次，同文件后续提交、`a7058c18f` 的后代）保留该行为面——plan/finding 报告所记「引入点 `1166339ae`（F2.5 批次）」系同链两提交混记，finding 报告原文「F2.5 批次 `1166339ae` 前后引入」的模糊表述经实测精确化，缺陷链实质（引入批次 = F2.5 P1-CK-mfg-002 修复无条件回写叠加零守卫链）与报告一致。**
      - Skill: bug-diagnosis-prompt

Exit Criteria:

- [x] 负路径失败测试在位且当前树红（七组合复现 + NOT_STARTED 控制组绿），失败输出注记在案 **〔2026-09-10 实证：修复前树红 `Failures: 1`（IN_PROCESS 组合 status=0 放行=复活缺陷复现，失败输出已注记）+ 控制组绿；修复后全 7 组合 + 控制组 2/2 绿〕**
- [x] 缺陷链与 finding 报告逐站对账一致，无未登记第三站点 **〔2026-09-10 实核：三站 HEAD 逐一对账一致；第三站点排查 = 工单外无共享消费方（grep 全仓），SubcontractOrder 平行家族即已登记的 P2-CK-mfg3-012 非未登记站点；引入点两提交混记勘误注记在案〕**

## Phase 2 — 守卫修复（Decision + Fix + Add）

> 统一类型：Decision | Fix | Add（1 Decision + 1 Fix + 1 Add）。
> Skill: bug-diagnosis-prompt
> Targets: `ErpMfgWorkOrderProcessor.java`（validateTransitionForReverseApprove / doReverseApprove）、`ErpMfgErrors`（新增反审核 docStatus 越界错误码）
> Prereqs: Phase 1 完成（失败测试在位）

- [x] <Decision> 守卫方案裁决：推荐 (a) `validateTransitionForReverseApprove` 增 docStatus 白名单校验 = 仅 `NOT_STARTED` 放行（依据：`doReverseApprove` 注释自述「未开工前提」+ P1-CK-mfg-002 修复意图即服务未开工反审核；SUBMITTED 与 approveStatus=APPROVED 正常不相交，白名单收窄到 NOT_STARTED 不损失合法路径）。替代方案：(b) 排除式黑名单（排除终态 + IN_PROCESS/STOPPED/STOCK_*）——被否，白名单语义更严且与「未开工前提」自述一致，黑名单对未预见新状态开放；(c) `doReverseApprove` 回写前校验 `docStatus ∈ {SUBMITTED, NOT_STARTED}`——被否，守卫应置于校验 step（与 validateTransitionFor* 既有分层模式一致），do 层保持纯执行。残余风险：未来若出现「已开工可反审核」产品需求，须走 owner doc（`docs/design/manufacturing/state-machine.md`）修订而非放宽白名单——在勾选注记显式登记。裁决与 owner doc 对账：核对 state-machine.md 反审核语义描述一致，发现漂移即先修 doc 注记再落码。**〔执行注记 2026-09-10〕采纳 (a) 白名单 = 仅 NOT_STARTED。owner doc 对账结论：`docs/design/manufacturing/state-machine.md` 工单节仅描述 docStatus 迁移轴（§迁移完整性）与审批角色（§角色与权限），**未声明 reverseApprove 允许任意 docStatus**——白名单守卫与 owner doc 零矛盾（无漂移，无需先修 doc；doReverseApprove 代码注释「未开工前提」即代码自身声明的适用前提，为白名单唯一语义锚）。残余风险显式登记：未来「已开工可反审核」产品需求须走 state-machine.md owner doc 修订 + 独立计划，不得在本守卫上放宽白名单。**
      - Skill: bug-diagnosis-prompt
- [x] <Fix> 接入守卫：`validateTransitionForReverseApprove` 在审批轴断言后增 docStatus 白名单校验，拒绝路径抛业务异常（错误码见下项）；`doReverseApprove` 不改（纯执行层保持）。行为不变式：NOT_STARTED 路径逐字节现状保持。**〔执行注记 2026-09-10〕守卫落 `validateTransitionForReverseApprove` 审批轴断言后：`docStatus != NOT_STARTED` → 抛 `ERR_REVERSE_APPROVE_DOC_STATUS_FORBIDDEN`（param：workOrderCode + currentStatus）；`doReverseApprove` 零改动（git diff 实证仅 validate 方法 +4 行守卫块）；NOT_STARTED 控制组测试与既有 `testReverseApproveThenResubmit`（P1-CK-mfg-002 回归）全绿 = 行为不变式保持。**
      - Skill: bug-diagnosis-prompt
- [x] <Add> 错误码：`ErpMfgErrors` 增反审核 docStatus 越界错误码（中文描述合规——ErrorCode.define 中文为源语言，经 i18n 后期补英文；`@Locale("zh-CN")` 接口注解已在位[MI.1]）；异常参数传码不传散文（沿 `e.param(ARG_WORK_ORDER_CODE, code)` 既有模式，状态参数传 docStatus 枚举名/码本身——MI.5a CAT-2 契约）。**〔执行注记 2026-09-10〕`ErpMfgErrors.ERR_REVERSE_APPROVE_DOC_STATUS_FORBIDDEN` = `erp.err.mfg.work-order.reverse-approve-doc-status-forbidden`（中文描述「工单[{workOrderCode}]当前单据状态[{currentStatus}]不允许反审核，仅未开工(NOT_STARTED)的工单可反审核」，params = ARG_WORK_ORDER_CODE + ARG_CURRENT_STATUS[docStatus 枚举名本身，CAT-2 传码契约]）；`@Locale("zh-CN")` 接口注解在位；测试断言经 `ErpMfgErrors.ERR_REVERSE_APPROVE_DOC_STATUS_FORBIDDEN.getErrorCode()` 传码（Phase 1 红轮用字面量占位，本步随常量落地换常量引用）。**
      - Skill: bug-diagnosis-prompt

Exit Criteria:

- [x] Phase 1 负路径测试全绿（七组合拒绝 + 控制组放行），`testReverseApproveThenResubmit` 等 P1-CK-mfg-002 既有回归保持绿 **〔2026-09-10 实证：`Tests run: 2, Failures: 0, Errors: 0`（负路径 7 组合逐组合断言绿 + 控制组绿）；域级回归 310/0/0/0 全绿含 `testRejectThenResubmit`/`testReverseApproveThenResubmit` P1-CK-mfg-002 回归（见 Phase 3 证据）〕**
- [x] 守卫落在校验 step 层、do 层零改动；错误码与传参符合 CAT-2 传码契约；Decision 三要素（选择/替代/残余风险）注记在案 **〔2026-09-10 实证：守卫在 `validateTransitionForReverseApprove`（校验 step），`doReverseApprove` git diff 零改动；currentStatus 传 docStatus 枚举名本身；Decision 选择 (a)/替代 (b)(c)/残余风险三要素注记在上〕**

## Phase 3 — 批级证明与收官门控

> 统一类型：Proof（3 项 Proof）。
> Skill: none
> Targets: 无新增代码；验证输出与 M2.9 消费证据记入勾选注记
> Prereqs: Phase 2 完成

- [x] <Proof> 域级回归：`mvn test -pl module-manufacturing/erp-mfg-service -am` 全绿零新增失败（基线 308/0/0/0，数字记入注记）**〔执行证据 2026-09-10：`Tests run: 310, Failures: 0, Errors: 0, Skipped: 0` BUILD SUCCESS——基线 308 + 本批新增 2（负路径 + 控制组）= 310，零回归〕**
      - Skill: none
- [x] <Proof> 收官门控：全 reactor `mvn test` 零新增失败（对照 2026-09-09 基线行 4006/0/0/1）+ `bash docs/audits/nop-compliance-checker.sh` exit 0 逐规则持平（R2b=242/R2c=1542/R12a=71；漂移即停走独立基线裁决）+ `node tools/check-hardcoded-cjk.mjs --strict` PASS + `git status --porcelain app-erp-all/src/main/resources/_vfs/_init-data/` 空（seed 零改动证明；seed 实际落点为 app-erp-all `_vfs/_init-data/`，路径须匹配 tracked 文件否则 porcelain 恒空不构成证明）+ `mvn clean install -DskipTests` BUILD SUCCESS **〔执行证据 2026-09-10：① 全 reactor `mvn test` exit 0 BUILD SUCCESS，surefire 逐类聚合 **3993/0/0/1**（= M1.17 闭包审计裁决的 surefire 口径真值 3991 + 本批新增 2；1 skipped 为基线预存；2026-09-09 基线行登记值 4006 与 surefire 聚合口径差 15 已由 M1.17 计数对账注记裁决为日志拼接重复计入，`## Verification` 计数注记同口径）；② compliance checker exit 0，R2b=242/R2c=1542/R12a=71 与基线逐值持平；③ CJK `--strict` exit 0 PASS（0 new violations vs frozen snapshot，CAT1..4 快照冻结面 0/0/209/1318 同 M2.0 复核口径）；④ seed porcelain 空 + `git ls-files` 同路径 373 个 tracked 文件（非空集，证明构成）；⑤ `mvn clean install -DskipTests` exit 0 BUILD SUCCESS（01:38 min）。本批变更面恰 3 文件：Processor + Errors + 测试类，`git status --porcelain` 全树复核无 ORM/api.xml/seed/页面触碰〕**
      - Skill: none
- [x] <Proof> M2.9 消费证据落盘（本计划勾选注记，不改索引）：P1-CK-mfg-022-r3 fixed 证据指针（测试类/方法 + 守卫站点）；P2-CK-mfg-010 状态继承注记（控制点消解 + 证据指针，回填归 M2.9）；mfg3-012 同控制点核验记录（委外 Processor 是否同型缺守卫——实核结论 + 若同型仅登记不修）；seed/ORM/api.xml 零触碰声明 **〔执行注记 2026-09-10 四件齐备，见下〕**
      - Skill: none

**M2.9 消费证据（本计划勾选注记，不改索引——回填归 M2.9）：**

1. **P1-CK-mfg-022-r3 fixed 证据指针**：测试 = `TestErpMfgReservationLifecycle#testReverseApproveRejectedForInProcessAndTerminalDocStatus`（七组合负路径：IN_PROCESS/STOCK_RESERVED/STOCK_PARTIAL/STOPPED/COMPLETED/CLOSED/CANCELLED × approveStatus=APPROVED → 断言 `erp.err.mfg.work-order.reverse-approve-doc-status-forbidden` 拒绝 + docStatus/approveStatus 双轴不变）+ `#testReverseApproveNotStartedControlGroupUnchanged`（NOT_STARTED 控制组：放行 + DRAFT 回写 + REJECTED + 审计字段清空 + 可重提）；守卫站点 = `ErpMfgWorkOrderProcessor#validateTransitionForReverseApprove`（审批轴断言后 docStatus 白名单仅 NOT_STARTED）+ `ErpMfgErrors.ERR_REVERSE_APPROVE_DOC_STATUS_FORBIDDEN`（新码）；`doReverseApprove` 零改动。先红后绿证据见 Phase 1/2 注记；域级 310/0/0/0 + 全 reactor 3993/0/0/1 零新增失败。
2. **P2-CK-mfg-010 状态继承注记**（r1 open 原 ID 状态不动，回填归 M2.9）：其控制点 = reverseApprove 无 docStatus 守卫（ck-mfg-workorder.md 记录）——本批白名单守卫落地即**消解该控制点**（终态/在制复活路径关闭，「assertCanSubmit 挡住」缓解失效的叠加面随之消除：DRAFT 复活不再可达）；fixed 证据指针同上条。状态继承（同型 finding 状态继承义务）归 M2.9 收官回填。
3. **mfg3-012 同控制点核验记录**（P2-CK-mfg3-012，r1 open，归 mfg-3 格——本批仅核验登记不修）：HEAD 实核 `ErpMfgSubcontractOrderProcessor#validateTransitionForReverseApprove`（L303-310 区域：仅审批轴 `assertCanReverseApprove`，无 docStatus 守卫）+ `#doReverseApprove`（L351-356 区域：翻 REJECTED + 清审计字段、**不动 docStatus**）——委外单同型缺守卫**实核仍在**（与 `ck-mfg-subcontract-r3.md` §2.2 记录一致；委外 8 态单轴 docStatus 语义，ISSUED/RECEIVED/COMPLETED 可翻 REJECTED 矛盾态但无 DRAFT 回写复活叠加，其「若修」须按委外单轴语义独立裁决 docStatus 白名单目标值，非工单 NOT_STARTED 的镜像搬运）。核验结论：同型控制点成立，升级证据无新增（无本批 P1 级叠加面），维持 P2 归 r1 通道/M2.x 后续批次。
4. **seed/ORM/api.xml 零触碰声明**：本批变更面 = `ErpMfgWorkOrderProcessor.java` + `ErpMfgErrors.java` + `TestErpMfgReservationLifecycle.java` 恰 3 文件；`git status --porcelain` 全树无 `module-*/model/`、`*.api.xml`、`_init-data/`、web 页面命中；seed 双面重录义务未触发。〔闭包审计 addendum 2026-09-10：执行批 3 文件之外，审计独立复跑新增第 4 文件 = 预存 flaky 快照 `3_report_completion_response.json5` 时间戳通配稳定化（见 `## Verification` 闭包审计行）——非 ORM/api.xml/seed/页面，零触碰声明语义不变〕

Exit Criteria:

- [x] mfg service 全绿 + 全 reactor 零新增失败 + 双 checker 持平基线 + seed 零改动证明在案 **〔2026-09-10 五证齐备：310/0/0/0 + 3993/0/0/1 + compliance exit 0（R2b=242/R2c=1542/R12a=71 持平）+ CJK strict PASS + seed porcelain 空（373 tracked 路径匹配）+ clean install BUILD SUCCESS，逐项数字见上〕**
- [x] M2.9 消费证据三件（fixed 指针 / 010 继承注记 / 012 核验记录）落盘于计划注记 **〔2026-09-10 四件齐备（三件 + seed/ORM/api.xml 零触碰声明），见上方 M2.9 消费证据节〕**

## Draft Review Record

- dispatch review #review-2026-09-09-210030-mission-driver-2026-09-10-0425-2-m23-manufacturing-p1-fix-batch-1-5f3dca61 to 2026-09-09-210030-mission-driver
- 2026-09-10：iteration 1，共识 approved #review-2026-09-09-210030-mission-driver-2026-09-10-0425-2-m23-manufacturing-p1-fix-batch-1-5f3dca61

## Verification

- 2026-09-10 执行会话先红后绿 **pass**：修复前 `mvn test -Dtest=TestErpMfgReservationLifecycle#testReverseApproveRejectedForInProcessAndTerminalDocStatus+testReverseApproveNotStartedControlGroupUnchanged` → `Tests run: 2, Failures: 1`（IN_PROCESS 组合 status=0 放行 = 复活缺陷复现，控制组绿）；修复后同命令 → `Tests run: 2, Failures: 0, Errors: 0`（七组合拒绝 + 控制组放行全绿）。
- 2026-09-10 域级回归 **pass**：`mvn test -pl module-manufacturing/erp-mfg-service -am` exit 0，**310/0/0/0**（基线 308 + 新增 2，零回归）。
- 2026-09-10 执行会话 `verify: [test]` 门控：全 reactor `mvn test` **pass**（exit 0 BUILD SUCCESS，surefire 逐类聚合 3993/0/0/1 零新增失败——M1.17 闭包审计裁决的 surefire 口径真值 3991 + 本批新增 2；1 skipped 为 2026-09-09 基线行预存，预存失败清单 = 无）。计数对账注记：基线行登记 4006 与 surefire 聚合口径差 15 系 m0-6 日志拼接重复计入（M1.17 `## Verification` 已裁决，基线行数值勘误归 owner 流程 successor），本计划沿用该裁决口径不重复裁决。
- 2026-09-10 双 checker **pass**：`bash docs/audits/nop-compliance-checker.sh` exit 0（R2b=242/R2c=1542/R12a=71 与基线逐值持平零漂移）；`node tools/check-hardcoded-cjk.mjs --strict` exit 0 PASS（0 new violations vs frozen snapshot）。
- 2026-09-10 seed 零改动 **pass**：`git status --porcelain app-erp-all/src/main/resources/_vfs/_init-data/` 空（`git ls-files` 同路径 373 tracked 文件非空集，证明构成）。
- 2026-09-10 构建 **pass**：`mvn clean install -DskipTests` exit 0 BUILD SUCCESS（01:38 min）。
- pass test 20260910-0620 exit=0
- 2026-09-10 闭包审计独立复跑 **pass**（auditor 新会话）：`mvn test -pl module-manufacturing/erp-mfg-service`（增量口径，仅受影响模块）exit 0 BUILD SUCCESS **310/0/0/0**；compliance checker exit 0（R2b=242/R2c=1542/R12a=71 持平）；CJK `--strict` exit 0 PASS；seed porcelain 空。复跑首 红 = `TestErpMfgWorkOrderEndToEnd#testEndToEndIssueReportCompletion` 预存 flaky（快照 `3_report_completion_response.json5` 跨实体时间戳 `@var:ErpInvStockMove@updateTime_5` 跨毫秒边界 1ms 偏差 → `not-equals-var-value`，与本批守卫无关——模式 = docs/bugs/2026-08-25-frozen-clock-millis §49 + docs/logs/2026/07-22.md 预存 flaky 判例）；按 2026-08-24 B4/B5 通配先例（78 处）外科通配 `"updateTime": "*"`（同 case 兄弟文件 `2_record_work_response.json5` 已是此写法）后复跑全绿——审计批新增变更面仅此 1 快照文件（业务断言零削弱）。

## Closure

- dispatch audit #audit-20260910-0702-2026-09-10-0425-2-m23-manufacturing-p1-fix-batch-1-7403ef22 to 2026-09-09-210030-mission-driver models={exec:glm-5.3-flash,aud:glm-5.3-flash}
- accepted #audit-20260910-0702-2026-09-10-0425-2-m23-manufacturing-p1-fix-batch-1-7403ef22：独立闭包审计通过——P1-CK-mfg-022-r3 修复落地成立（守卫在校验 step 仅 NOT_STARTED 放行 + CAT-2 传码新码，do 层零改动；先红后绿 + 七组合负路径/控制组测试经 GraphQL mutation 运行时在位）；审计新会话复跑全绿：`mvn test -pl module-manufacturing/erp-mfg-service` 310/0/0/0 exit 0（首跑红为预存 flaky 快照跨实体时间戳 1ms 边界偏差，按 08-24 B4/B5 通配先例外科通配后复绿，与本批守卫无关）+ compliance exit 0（R2b=242/R2c=1542/R12a=71 持平）+ CJK `--strict` PASS + seed porcelain 空；14/14 检查项全勾，Exit Criteria 与 live repo 逐项实核一致，语义审计（anti-hollow/deferred honesty/docs sync）无阻塞发现，frontmatter `status: active` 保持（ledger 协议，completion 由引擎派生）。
