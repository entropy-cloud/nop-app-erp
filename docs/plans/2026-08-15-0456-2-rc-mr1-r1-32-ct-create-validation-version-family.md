# 2026-08-15-0456-2-rc-mr1-r1-32-ct-create-validation-version-family RC-R1.32 — contract 创建校验与版本族（MR1 第一批纯预授权）

> Plan Status: completed
> Last Reviewed: 2026-08-15
> Mission: requirement-compliance
> Work Item: RC-R1.32（P1-RC-072 UC-CT-01 A/B/D 创建校验 + P1-RC-073 UC-CT-02 A/C amend 行复制/驳回恢复，同域同修复方式族）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.32 行 + `docs/audits/arm-index.md` P1-RC-072/073 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（纯 BizModel/Processor 代码逻辑预授权，G6 contract 生命周期族）
> Related: `docs/design/contract/use-cases.md`（L1 UC-CT-01/02）；`docs/design/contract/state-machine.md`（§2 DRAFT→NEGOTIATION 漂移 + §适用对象二 版本轴）；`docs/audits/2026-08-08-0135-rc-ma4-a4-2-155-162-contract-runtime.md`（A4.2.155-157 运行时证据）；`docs/plans/2026-08-12-1118-1-ct-lifecycle-state-machine-bean.md`（状态机 Bean 先例）
> Audit: required

## Current Baseline

- **finding P1-RC-072（arm-index 行，UC-CT-01 A/B/D）**：L1（`use-cases.md:13/15/21`）逐字「系统校验 总金额 = ∑行金额；startDate < endDate / 提交审批 → NEGOTIATION（创建 v1 版本，isCurrent=true）/ 金额超预算 → 拦截提交，触发特批流程」。L3 实仓：
  - `ErpCtContractBizModel#defaultPrepareSave:66-72` 仅兜底 businessDate，**零金额/日期校验**；`ErpCtContractActivateProcessor#activate:33-49` 仅校验 type/direction + 版本 FINALIZED，无 totalAmount 校验。
  - grep `validateTotal|sumLineAmount|totalAmount.*equals|ERR_CT_AMOUNT` 跨 erp-ct-service/src/main **零命中**。
  - **无 `submit`/`negotiate` @BizMutation**（合同到 NEGOTIATION 经通用 CRUD save 直设 status——state-machine.md §2 登记「DRAFT→NEGOTIATION 命名动作 writer 零落地」implementation drift + successor，**本行即该 successor 落地**）；测试 `TestErpCtContractPosting#createVersion:225`/`TestErpCtContractTerminate#createVersion:183` 均手工 seed 版本证实 v1 非自动创建。
  - L4：activate 强断言 ACTIVE，未断言 totalAmount=∑行 + v1 自动创建。**§4 三判据**：不适用（state-machine.md 无创建校验/v1 Deferred 标注）。→ Q4=(a) 强制实现 P1。
- **finding P1-RC-073（arm-index 行，UC-CT-02 A/C）**：L1（`use-cases.md:33/43`）逐字「系统复制原合同所有行到变更单，允许增删改 / 变更单被驳回 → 原合同保持 ACTIVE 不变」。L3 实仓：`ErpCtContractAmendProcessor#amend:43-76` 遍历版本 + 旧 isCurrent=false + 新建版本头（contractId/versionNo/versionDate/isCurrent/status）+ 合同头 setStatus(DRAFT)——**零 ErpCtContractLine 复制**（`:64-70` 仅 newEntity 版本头）；`ErpCtContractLineBizModel` CRUD 桩无 copy 方法；grep `copyLine|cloneLine|复制行|rejectAmend|restoreActive` 跨 contract 零命中——amend 将合同头 setStatus(DRAFT)+旧版本 isCurrent=false 后**无 restore-to-ACTIVE 逻辑**，变更单驳回则合同卡 DRAFT + 无 current 版本。L4：E2E ct-contract-lifecycle amend 强断言 versionCount>0，未断言行复制 + 未断言驳回恢复。→ Q4=(a) 强制实现 P1。
- **A4.2.155/156/157 运行时证据**（`2026-08-08-0135-rc-ma4-a4-2-155-162-contract-runtime.md`，维持 P1）：
  - A4.2.155/156：`defaultPrepareSave` 零金额/日期校验 + `_ErpCtContract.xmeta` validator/expression 零命中 + `ErpCtContract.view.xml` totalAmount 零 validator + GraphQL save 实证不拒绝金额不一致合同（`TestErpCtContractPosting#createContract:192-208` 未设 totalAmount 仍 save 成功）；无 submit/negotiate mutation + save 路径零版本钩子。
  - A4.2.157：`ErpCtContractAmendProcessor#amend:35-66` 仅版本翻转零行复制 + `ErpCtContract.view.xml` rowActions 无 amend 按钮 + edit 表单 lines 子表 sub-grid-edit 纯手工编辑。
- **实仓（HEAD 核查）**：
  - `ErpCtContractBizModel`（219 行）：activate/suspend/resume/terminate/expire/amend 六 mutation + `validateTypeDirectionCombo:165-180` + `findCurrentVersion:182-187`/`findVersions:189-193` helper + `illegalTransition:195-208`。注入 `IErpCtContractVersionBiz`/`ErpCtContractActivateProcessor`/`ErpCtContractAmendProcessor`/`ErpCtContractStateMachine`。
  - `ErpCtContractStateMachine`（`module-contract/erp-ct-service/.../statemachine/`）：7 边（activate/suspend/resume/terminate×2/expire/amend），**无 assertCanSubmitForNegotiation / submit 边**（NEGOTIATION 漂移如实编码不伪造边——本行落地 submit 时须补 Bean 边：`assertCanSubmitForNegotiation(DRAFT→NEGOTIATION)` + `submitTargetStatus()`）。
  - `ErpCtContractAmendProcessor`（111 行）：`amend:43-76` = assertCanAmend(ACTIVE) → 版本遍历（旧 isCurrent=false 逐个 updateEntity）→ 新建版本头（max+1、isCurrent=true、VERSION_STATUS_DRAFT）→ 合同头 setStatus(DRAFT)。**零行复制、零驳回恢复**。
  - 版本轴 Bean：`ErpCtContractVersionStateMachine`（M3.18 落地，DRAFT→FINALIZED→SIGNED 2 边 + 终态 SIGNED）。
  - `ErpCtContractLine` ORM（`app-erp-contract.orm.xml:195-214`）：contractId/lineNo/materialId/description/quantity/unitPrice/amount/remark——行复制 = 同一 contractId 下逐行 newEntity 复制（同合同 amend 模型，变更单 = 同合同 DRAFT 态，行留在原 contractId 下）。
  - `ErpCtConfigs`：无 erp-ct 预算相关键（finance budget 属跨域 config-gated 特性——**本行预算特批裁决见 Decision D3**）。
  - 测试基线：erp-ct-service 测试类——TestErpCtContractPosting(4 @Test)/TestErpCtContractCrudSmoke(5)/TestErpCtContractTerminate(6)/TestErpCtESignature(19)/TestErpCtContractRebate(8)/TestErpCtRebateSettlementEnd(2)/TestErpCtResponseMasking(4) + statemachine 矩阵 5 类（9+7+7+3+3）≈ 77 tests 基线（执行时以实跑计数为准）。
- **预授权判据**（第一批纯预授权）：纯 BizModel/Processor 代码逻辑（跨字段校验 + submit mutation + 版本 Bean 边 + 行复制 + rejectAmend + 测试），**不触 ORM 结构/会计过账/删除**；roadmap RC-R1.32 行 `todo`，Deps（R1.0 done）已满足。
- **涉及文件**：`ErpCtContractBizModel.java`（defaultPrepareSave/Update 校验 + submit/rejectAmend mutation）；`ErpCtContractAmendProcessor.java`（copyLinesFromOriginal 接线）；`ErpCtContractStateMachine.java`（submit 边 + rejectAmend 恢复语义按 Bean 既有模式）；`ErpCtErrors.java`（新增错误码）；`IErpCtContractBiz.java`（契约）；`ErpCtConstants.java`（按需）；测试类（TestErpCtContractCreateValidate / 既有测试扩展）；`docs/design/contract/state-machine.md` + `docs/audits/arm-index.md` + `docs/backlog/requirement-compliance-roadmap.md` + `docs/logs/2026/08-15.md`（回填）。

## Goals

- **创建校验运行时成立（P1-RC-072 ①②）**：`defaultPrepareSave` 增跨字段校验——(a) `totalAmount == ∑(lines.amount)`（有行时；无行时 totalAmount 可空或校验必填，**Decision 项**）；(b) `startDate < endDate`（两者非空时；缺失容忍策略 **Decision 项**）。违反抛新增领域错误码（`ERR_CT_AMOUNT_MISMATCH` / `ERR_CT_DATE_RANGE_INVALID`，**错误码命名 Decision 项**）。
- **submit @BizMutation（P1-RC-072 ② + state-machine.md §2 successor）**：`ErpCtContractBizModel#submit`（DRAFT→NEGOTIATION）——状态守卫（Bean 新增 `assertCanSubmitForNegotiation`，**仅守卫 status==DRAFT，不因已有版本拒绝**——amend 生命周期下变更 DRAFT 已有版本（v1 false + v2 DRAFT true），submit 是其唯一前向出口，见 MAJOR-1 修正）+ 校验通过后 `setStatus(NEGOTIATION)` + **版本创建语义**：零版本时自动创建 v1（versionNo=1、isCurrent=true、VERSION_STATUS_DRAFT 或 FINALIZED——**Decision 项 D2**：v1 初始版本状态，契约 §适用对象二 初始态 = DRAFT，activate 前须 finalizeVersion）；**已有版本时保留既有 DRAFT 当前版本不动**（amend 场景：v2 已 isCurrent=true + DRAFT，submit 后维持，仅合同头 → NEGOTIATION）。**activate 前置约束（MAJOR-3 修正）**：amend→submit 路径下 activate 级联 signVersion 仅对 FINALIZED 生效（`ErpCtContractActivateProcessor:52-55`），v2 保持 DRAFT current 时 activate 会静默跳过签署——**activate 前须显式 `finalizeVersion(v2)`**（版本 Bean `assertCanFinalize` 仅收 DRAFT，可达性有保证）；activate 对非 FINALIZED current 版本的静默跳过行为按 D6 裁决（补齐守卫 vs watch-only 登记）。
- **amend 行复制（P1-RC-073 ①）**：`ErpCtContractAmendProcessor.amend` 增 `copyLinesFromOriginal`——复制原合同全部 `ErpCtContractLine` 到变更单（同 contractId 模型下行已保留，复制语义 = **新建版本时行级快照或行保留确认**，**Decision 项 D1：同合同模型下行无需复制（行留在 contractId 下），复制义务语义化为「行保留 + 版本切换后仍可编辑」确认或按 L1 字面逐行 newEntity 复制到变更 DRAFT 上下文**）。
- **rejectAmend @BizMutation（P1-RC-073 ②）**：变更单驳回 → 合同头 restore ACTIVE + **前任当前版本 restore isCurrent=true**（`ErpCtContractBizModel#rejectAmend`，守卫 status==DRAFT）——恢复目标规格化（**Decision 项 D5**）：取 SIGNED 最大 versionNo（无 SIGNED 回落 FINALIZED 最大者）——跨请求可行 + 防重复 amend/reject 周期遗留 DRAFT 行被误恢复 + 防 finalize-then-reject 恢复未签署 FINALIZED 为 current（MAJOR-2/MAJOR-B/iteration-4 修正）；遗留 DRAFT 版本行去留随 D5 一并裁决。
- **测试**：新增测试组——创建校验（金额匹配/不匹配/startDate≥endDate/缺失容忍）；submit（DRAFT→NEGOTIATION + v1 自动创建 + 非 DRAFT 拒绝 + **已有版本放行并保留既有 DRAFT current 版本不动**）；amend 行保留/复制语义；rejectAmend（DRAFT→ACTIVE + 前任当前版本 isCurrent 恢复 + 非 DRAFT 拒绝）；既有 activate 全链回归双路径（首提 submit→finalizeVersion→activate + amend→submit→finalizeVersion→activate）。
- **零回归**：erp-ct-service 既有测试全绿（~77 基线）+ 全仓构建 + compliance checker 零漂移。
- **回填**：arm-index P1-RC-072/073 → `done (RC-R1.32)` + roadmap 行 → `done` + owner doc 注记（state-machine.md §2 漂移注记更新为已实现）+ `docs/logs/` 日志条目。

## Non-Goals

- **不实现 P1-RC-072 ③ 预算校验 hook + 特批流程**（**Decision D4 裁决**）：roadmap RC-R1.32 行范围 = 「totalAmount=∑行 + startDate<endDate + submit 建 v1 版本 + amend 复制行 + rejectAmend 驳回恢复 ACTIVE」——预算特批未列；理由链：① roadmap 行为权威 R1.0 展开范围；② 展开器 G6 将 RC-R1.32 分类为「同域同修复方式（纯 BizModel 生命周期编排）」而预算是 contract→finance 跨域契约（P1-RC-078 先例归 ask-first 类）；③ 特批流程与 P1-RC-077 审批引擎（RC-R1.34 独立行）语义重叠。本行按 roadmap 范围裁剪并登记 Deferred But Adjudicated successor，触发条件 = 预算特批业务流立项 / finance budget 跨域契约行启动 / roadmap 行范围修订纳入③。
- **不实现 P1-RC-076/077**（terminate 法务门控/审批引擎——独立行 RC-R1.34）。
- **不实现 P1-RC-074/075**（计费族——独立行 RC-R1.33）。
- **不实现 P1-MA2-071**（到期自动化 job——独立行 RC-R1.35）。
- **不触 ORM 结构**（零列/零索引变更）。
- **不做前端 AMIS 接线**（submit/rejectAmend 按钮、行复制前端编排均不在本行；后端 mutation 提供能力面）。
- **不改真相源契约段落**（use-cases L1 不动）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权代码逻辑修复，Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/contract/use-cases.md`（L1 UC-CT-01/02）+ `docs/design/contract/state-machine.md`（§2 漂移注记 + §适用对象二 版本轴）+ `docs/audits/2026-08-08-0135-rc-ma4-a4-2-155-162-contract-runtime.md`（A4.2.155-157 运行时证据）
- Skill Selection Basis: 实现面 = CrudBizModel defaultPrepareSave 跨字段校验 + per-mutation Processor + 状态机 Bean 边扩展 + 跨实体 IBiz（`nop-backend-dev`）；测试（`nop-testing`：JunitAutoTestCase + GraphQL RPC + 快照）。无 view.xml/xbiz/ORM 变更。

## Infrastructure And Config Prereqs

- 无新 config key/环境变量/外部服务。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-contract/erp-ct-service`。

## Execution Plan

### Phase 1 - Explore 校验语义与版本/复制模型裁决（Decision）

Status: completed
Targets: `ErpCtContractBizModel.java`；`ErpCtContractAmendProcessor.java`；`ErpCtContractStateMachine.java`；`ErpCtContractVersionStateMachine.java`；`_ErpCtContract.xmeta`；`ErpCtErrors.java`
Skill: `nop-backend-dev`

- Item Types: `Decision | Proof`
- Prereqs: 无（既有基线）

- [x] `Decision` **创建校验语义裁决（D1）**：(a) totalAmount 匹配口径——`defaultPrepareSave` 时经 `IErpCtContractLineBiz`/dao 查行汇总 Σ amount 与 totalAmount 比对（scale 4 HALF_UP 容差？**决策记录**）；**行金额数据源**：prepareSave 时 in-memory to-many 实体图（同请求子表保存，flush 顺序风险）vs DAO 查询（跨请求已落库行可靠；同事务混合场景须 orm().flushSession() 防 stale——对齐 R1.8 totalHours 汇总先例）；无行时策略（totalAmount 空允许 / 有 totalAmount 无行拒绝？**决策记录**，建议无行时 totalAmount 可空——既有 `TestErpCtContractPosting#createContract:192-208` 合同先行 save 无行零改动通过，对齐 §2 前置条件「金额/条款/日期必填」`state-machine.md:42` 由 submit 守卫生效）；(b) startDate/endDate——两者非空时 `startDate.isBefore(endDate)` 拒绝，单侧缺失容忍（**决策记录**，对齐 XMeta mandatory 既有语义）。记录理由 + 备选。
      **决策记录（已执行）**：金额口径 = 有行时 totalAmount 必须非空且 == Σ行金额（双方 scale 4 HALF_UP 舍入后 `compareTo` 精确比较，容忍浮点舍入痕迹）；无行时 totalAmount 可空**亦可设**（头先行、行后加的 save 流合法；既有 `TestErpCtContractPosting#createContract:192-208` 未设 totalAmount 零改动通过）。行金额数据源 = defaultPrepareSave 用 in-memory to-many 实体图（`entity.getLines()`，仅同请求嵌套子表时非空——ORM prepareSave 阶段嵌套行已 attach 于实体集合）；submit 用 `IErpCtContractLineBiz.findList` DAO 查询（跨请求已落库行可靠，对齐 R1.8 totalHours 先例 `ErpHrTimesheetBizModel#sumHoursByTimesheet:121-125`）。日期 = 两者非空时 `startDate.isBefore(endDate)` 否则拒绝；单侧缺失容忍；双 null 容忍（对齐 XMeta mandatory 既有语义）。**更新路径不做金额/日期校验**（defaultPrepareUpdate 不接线——remark 等部分更新不触发全量重校验；submit 为权威业务门卫，§2 前置条件「金额/条款/日期必填」由 submit 守卫生效）。错误码命名 = `ERR_CT_AMOUNT_MISMATCH`（`erp.err.ct.amount-mismatch`）/ `ERR_CT_DATE_RANGE_INVALID`（`erp.err.ct.date-range-invalid`）（按计划建议名定案）。备选否决：prepareSave 用 DAO 查询（新实体零行必然空结果，无意义且引入 flush 时序依赖）；update 全量重校验（破坏部分更新流）。
      - Skill: `nop-backend-dev`
- [x] `Decision` **v1 版本初始状态裁决（D2）**：submit 自动创建 v1 时 versionNo=1 + isCurrent=true + status=VERSION_STATUS_DRAFT（对齐 §适用对象二 初始态 DRAFT + 版本 Bean `assertCanFinalize` 仅收 DRAFT `ErpCtContractVersionStateMachine:45-49` + activate 级联 signVersion 仅对 FINALIZED 生效 `ErpCtContractActivateProcessor:52-55`；§2 迁移表行 42「创建 v1 版本（DRAFT→FINALIZED）」读作「经 DRAFT→FINALIZED 演进」）vs status=FINALIZED（一步到位）。建议 DRAFT（对齐初始态契约 + 既有 activate 级联要求 FINALIZED 由 finalizeVersion 显式到达）。**决策记录理由 + 备选**。
      **决策记录（已执行）**：v1 初始状态 = `VERSION_STATUS_DRAFT`（versionNo=1 + isCurrent=true + DRAFT）。理由：对齐 §适用对象二 初始态 = DRAFT + 版本 Bean `assertCanFinalize` 仅收 DRAFT（`ErpCtContractVersionStateMachine:45-49`）+ activate 级联 signVersion 仅对 FINALIZED 生效（`ErpCtContractActivateProcessor:52-55`）——DRAFT→FINALIZED 经 `finalizeVersion` 显式到达，activate 前须 finalizeVersion 契约（Phase 3 测试③双路径覆盖）。备选 FINALIZED 一步到位否决（破坏初始态契约 + 跳过定稿确认）。
      - Skill: `nop-backend-dev`
- [x] `Decision` **amend 行复制语义裁决（D3）**：同合同 amend 模型（amend 翻转合同头至 DRAFT + 版本切换，行留 contractId 下）下，L1「复制原合同所有行到变更单」语义化确认——选项 A（裁决候选）：行保留即满足（版本切换不删行，变更 DRAFT 可编辑既有行，零复制代码——与现行 amend 行为一致，**仅补文档注记**）；选项 B：逐行 newEntity 复制（同 contractId 下复制产生重复行，**与同合同模型冲突**）。建议选项 A + owner doc 注记（变更单=同合同 DRAFT 态，行保留编辑）；记录理由 + 备选 + 与 L1 字面的对齐声明（若审查判定必须字面复制则转选项 B 需评估行重复语义）；**Phase 4 arm-index 落地摘要须显式记录 D3 解释（同合同模型 ⇒ 复制语义 = 行保留）防 compliance 漂移**。**D3 同时裁决 rejectAmend 恢复目标规格（D5）所需的前任当前版本捕获机制**（amend 时快照 vs 恢复时推导——见 D5）。
      **决策记录（已执行）**：选项 A（行保留即满足）——同合同 amend 模型（amend 翻转合同头至 DRAFT + 版本切换，行留 contractId 下），变更单 = 同合同 DRAFT 态，行保留可编辑即满足 L1「复制原合同所有行到变更单」语义（复制义务语义化为「行保留 + 版本切换后仍可编辑」确认）；零复制代码（amend Processor 零逻辑变更，仅注释补记）；选项 B 逐行 newEntity 复制在同 contractId 下产生重复行，与同合同模型冲突，否决。Phase 4 arm-index 落地摘要显式记录「同合同模型 ⇒ 复制语义 = 行保留」防 compliance 漂移。D3 同时裁决 D5 恢复目标捕获机制 = **恢复时推导**（选项 B，见 D5），否决 amend 时快照（跨请求不可得 + 无持久化载体）。
      - Skill: `nop-backend-dev`
- [x] `Decision` **rejectAmend 恢复目标裁决（D5）**：恢复目标 = **选项 B（裁决候选）：优先 status==SIGNED 中 versionNo 最大者；无 SIGNED 时回落 FINALIZED 最大者**——跨请求可行（amend 与 rejectAmend 是**独立 @BizMutation 调用/事务**，amend 内存快照在 rejectAmend 执行时已不可得；Bean 实例字段是共享单例并发/重启危害），对重复 amend/reject 周期遗留的 DRAFT 行免疫（status 过滤天然排除未定稿遗留行——MAJOR-2 修正的目标即由此达成）。**SIGNED 优先的理由（iteration-4 MINOR 修正）**：finalize-then-reject 路径可达（amend→finalizeVersion(v2)→rejectAmend，守卫仅查合同头 status==DRAFT），若含 FINALIZED 则会把未签署的 v2（FINALIZED）恢复为 current，产生 ACTIVE 合同 + 未签署 current 版本的不一致态；SIGNED 优先恒与 amend 前 current（ACTIVE 态下 activate 保证 current 为 SIGNED）重合，且同样对 DRAFT 免疫。选项 A（amend 时捕获前任当前版本快照）否决：需持久化载体（新列触 ORM ask-first 越界）或调用方持有 versionNo 回传（API 契约脆弱），非最小面。**遗留 DRAFT 版本行去留裁决**（保留孤儿行 vs 删除——**决策记录**，建议保留：版本历史可追溯，逻辑删除语义不破坏）。**决策记录理由 + 备选**。
      **决策记录（已执行）**：恢复目标 = **选项 B**：优先 `status==SIGNED` 中 versionNo 最大者；无 SIGNED 回落 FINALIZED 最大者。理由：跨请求可行（amend 与 rejectAmend 是**独立 @BizMutation 调用/事务**，amend 内存快照在 rejectAmend 执行时已不可得；Bean 实例字段共享单例并发/重启危害）；**SIGNED 优先**防 finalize-then-reject 恢复未签署 FINALIZED 为 current（ACTIVE 态下 activate 保证 current 为 SIGNED，SIGNED 优先恒与 amend 前 current 重合）；对重复 amend/reject 周期遗留 DRAFT 行免疫（status 过滤天然排除未定稿遗留行）。遗留 DRAFT 版本行：**保留**（版本历史可追溯，逻辑删除语义不破坏）。边界裁决（无 SIGNED/FINALIZED 候选时——零版本 ACTIVE 合同 amend 后驳回）：仅恢复合同头 ACTIVE，同时清空遗留 DRAFT 版本的 isCurrent（恢复「无 current 版本」前置不变量，防 ACTIVE+DRAFT-current 不一致态）。选项 A（amend 时快照）否决：需持久化载体（新列触 ORM ask-first 越界）或调用方持有 versionNo 回传（API 契约脆弱）。
      - Skill: `nop-backend-dev`
- [x] `Decision` **activate 对非 FINALIZED current 版本的守卫裁决（D6）**：amend→submit 路径下 v2 保持 DRAFT current 时 activate 级联 signVersion 静默跳过（`ErpCtContractActivateProcessor:52-55`）——选项 A（裁决候选）：补齐守卫（activate 要求 current 版本 FINALIZED 否则拒绝，显式错误码）——行为变更超出本行 roadmap 范围；选项 B：watch-only 登记（既有流经显式 finalizeVersion 可达，静默跳过仅影响跳步骤的调用方）——建议选项 B + owner doc 注记（本行落地 submit 后 activate 前置 finalizeVersion 契约，测试 ③ 覆盖 amend→submit→finalizeVersion→activate 全链）。**决策记录理由 + 备选**。
      **决策记录（已执行）**：选项 B watch-only 登记——activate 对非 FINALIZED current 版本静默跳过维持现状（补齐守卫属行为变更超出本行 roadmap 范围；既有流经显式 finalizeVersion 可达，静默跳过仅影响跳步骤的调用方）。落地：owner doc 注记（本行落地 submit 后 activate 前置 finalizeVersion 契约）+ Phase 3 测试③覆盖 amend→submit→finalizeVersion→activate 全链。选项 A（补齐守卫）否决：行为变更超出范围 + 与既有 activate 语义（`ErpCtContractActivateProcessor:52-55` 已签署放行）冲突面扩大。
      - Skill: `nop-backend-dev`
- [x] `Decision` **预算特批范围裁决（D4）**：roadmap RC-R1.32 行未列预算 hook（见 Non-Goals）——理由链：① roadmap 行为权威 R1.0 展开范围（`roadmap:424` 显式未列）；② 展开器 G6 行（`2026-08-07-1910:198`）将 RC-R1.32 分类为「同域同修复方式（纯 BizModel 生命周期编排）」——预算校验是 contract→finance 跨域契约，按 P1-RC-078 先例（「跨域契约须与 purchase/sales 订单行协调 ask-first」）属 ask-first 类，超出第一批纯预授权边界；③ 特批流程与 P1-RC-077 审批引擎（RC-R1.34 独立行）语义重叠。确认按 roadmap 范围裁剪，预算特批登记 Deferred But Adjudicated successor（触发条件 = 预算特批业务流立项 / finance budget 跨域契约行启动 / roadmap 行范围修订纳入③）；若草案审查认为 Q4=(a) 强制义务覆盖③，则转 Phase 2 纳入 config-gated 预算校验 hook（跨域 `IErpFinBudgetControlBiz` 调已有能力，`erp-fin.budget-commitment-enabled` 默认 false）——**本项为范围门，审查收敛后定案**。
      **决策记录（已执行）**：范围门定案 = **按 roadmap 范围裁剪**——预算特批登记 Deferred But Adjudicated successor（触发条件 = 预算特批业务流立项 / finance budget 跨域契约行启动 / roadmap 行范围修订纳入③）。理由链：① roadmap 行为权威 R1.0 展开范围（`roadmap:424` 显式未列预算 hook）；② 展开器 G6 行（`2026-08-07-1910:198`）将 RC-R1.32 分类为「同域同修复方式（纯 BizModel 生命周期编排）」——预算校验是 contract→finance 跨域契约，按 P1-RC-078 先例（「跨域契约须与 purchase/sales 订单行协调 ask-first」）属 ask-first 类，超出第一批纯预授权边界；③ 特批流程与 P1-RC-077 审批引擎（RC-R1.34 独立行）语义重叠。草案审查 5 iterations 收敛（0 BLOCKER）无 Q4=(a) 覆盖③主张 → 不转 Phase 2 config-gated hook。
      - Skill: `nop-backend-dev`
- [x] `Proof` **既有测试误伤面核查**：grep erp-ct 测试集全部 `ErpCtContract__save`/`__activate`/`__amend` 调用面——确认 defaultPrepareSave 增校验后哪些既有测试 seed 未设 totalAmount/行（如 `TestErpCtContractPosting#createContract:192-208` 未设 totalAmount——**识别调整点**：种子补 totalAmount 或校验豁免路径[无行时 totalAmount 可空]）；确认 rejectAmend 与既有 amend 测试零冲突。
      **核查结论（已执行）**：全部既有 erp-ct-service 测试 `ErpCtContract__save` 调用面核查——`TestErpCtContractPosting#createContract:192-208` / `TestErpCtContractTerminate#createContract:166-181` / `TestErpCtContractCrudSmoke` 5 处（:52/:72/:95/:122/:150）/ `TestErpCtContractRebate:380-394` / `TestErpCtRebateSettlementEnd:176-190` 全部：**未设 totalAmount + 无嵌套行**（行经独立 `ErpCtContractLine__save` 后建）+ startDate<endDate 合法 → **D1 语义下零调整**（无行时 totalAmount 空允许；日期合法）。`TestErpCtESignature:347-349` 经 raw DAO `new ErpCtContract()` 绕过 defaultPrepareSave 零影响。E2E `ct-contract-lifecycle`（totalAmount=10000 零行）/`ct-contract-version`（totalAmount=5000 零行）/`ct-invoice-plan-trigger`（totalAmount=10000 零行）seed 合同零行 → 零调整。rejectAmend 为新增 mutation，与既有 amend 测试（`TestErpCtContractRebate:114` amend 断言版本计数）零冲突。识别调整点清单 = **空**（零调整）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] D1-D6 决策记录落盘（含理由 + 备选）+ 误伤面核查结论（已识别调整点清单：既有测试种子需补 totalAmount 的具体位置）
- [x] 版本 Bean 边扩展方案确认（assertCanSubmitForNegotiation 命名 + submitTargetStatus + rejectAmend 边 + 与既有 7 边零冲突）

### Phase 2 - 创建校验 + submit + rejectAmend 落地（P1-RC-072/073 核心）

Status: completed
Targets: `ErpCtContractBizModel.java`；`ErpCtContractStateMachine.java`；`ErpCtErrors.java`；`IErpCtContractBiz.java`
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 1 完成

- [x] `Fix` `defaultPrepareSave` 增跨字段校验（D1 语义）：totalAmount=∑行金额比对 + startDate<endDate，违反抛新增错误码（`ERR_CT_AMOUNT_MISMATCH`/`ERR_CT_DATE_RANGE_INVALID` 或按 D1 命名裁决）。**行金额数据源按 D1 裁决**（prepareSave 时 in-memory to-many 实体图 vs DAO 查询——同事务保存子表存在 flush 顺序风险，**决策记录**）。**仅新建路径**（defaultPrepareUpdate 是否同样校验——**决策记录**，建议新建校验 + 更新按 D1 补充裁决）。
      **落地记录**：`ErpCtContractBizModel#validateCreateFields`（D1 语义）——`startDate<endDate` 校验（`validateDateRange`）+ 有行时 totalAmount 非空且 == Σ行金额（in-memory 实体图口径，`entity.getLines()`）；`validateContractFields`（submit 权威门卫，`IErpCtContractLineBiz.findList` DAO 口径）；更新路径不校验（D1 裁决）。
      - Skill: `nop-backend-dev`
- [x] `Add` `ErpCtContractStateMachine` 增 submit 边：`assertCanSubmitForNegotiation(status)`（DRAFT→NEGOTIATION）+ `submitTargetStatus()`——落地 §2 漂移 successor（Bean 由 7 边 → 8 边，transitions() 元数据同步）。
      **落地记录**：`assertCanSubmitForNegotiation`（仅 DRAFT）+ `submitTargetStatus()=NEGOTIATION` + `assertCanRejectAmend`（仅 DRAFT）+ `rejectAmendTargetStatus()=ACTIVE`——Bean 7 边 → 9 边（transitions() 元数据同步，matrix 测试 7→9 断言更新）；`ErpCtContractVersionStateMachine` 零变更（v1 创建 = 初始态写入非迁移）。
      - Skill: `nop-backend-dev`
- [x] `Add` `ErpCtContractBizModel#submit` @BizMutation（IErpCtContractBiz 契约 + 委托或 Processor——**决策记录**：镜像既有 activate 委托 ErpCtContractActivateProcessor（Processor 形态）或 suspend/resume 直写形态，**取一致性裁决**——注意既有 activate/amend 为 Processor、suspend/resume/terminate/expire 为 BizModel 直写，两类先例并存，裁决记录理由）：守卫 Bean assertCanSubmitForNegotiation → 校验（可复用 D1 校验）→ setStatus(NEGOTIATION) → 版本创建（零版本建 v1 / 已有版本保留 current——D2/MAJOR-1 语义）→ updateEntity + 返回合同。
      **落地记录**：直写形态（对齐 suspend/resume/terminate/expire 五先例，非 Processor——单步操作 + 条件副作用；activate/amend 为既有 Processor 先例并存，裁决取多数一致性）：守卫 Bean `assertCanSubmitForNegotiation` → `validateContractFields`（D1 复用 + DAO 行口径）→ `ensureVersionOnSubmit`（零版本建 v1[versionNo=1、isCurrent=true、DRAFT，D2]；已有版本零操作）→ setStatus(NEGOTIATION) → updateEntity。`IErpCtContractBiz` 契约同步（`@BizMutation submit`）。
      - Skill: `nop-backend-dev`
- [x] `Add` `ErpCtContractBizModel#rejectAmend` @BizMutation（契约 + 实现）：守卫 status==DRAFT（Bean 增 `assertCanRejectAmend` 边 DRAFT→ACTIVE）→ **恢复 D5 裁决目标（选项 B：优先 SIGNED 最大 versionNo，无 SIGNED 回落 FINALIZED 最大者）**——查询既有版本列表过滤 status + max versionNo 恢复 isCurrent=true（**不按「最近非当前版本」推导**——防重复 amend/reject 周期遗留 DRAFT 行被误恢复，MAJOR-2 修正；**不依赖 amend 内存快照**——跨请求不可得，MAJOR-B 修正；**SIGNED 优先防 finalize-then-reject 恢复未签署 FINALIZED 为 current**，iteration-4 修正）→ 合同头 setStatus(ACTIVE) → updateEntity。驳回后激活流（submit 不可再用——合同已回 ACTIVE，需重新 amend）。
      **落地记录**：直写形态：守卫 Bean `assertCanRejectAmend`（仅 DRAFT）→ `restoreCurrentVersion`（D5 选项 B：SIGNED 最大 versionNo 优先，无 SIGNED 回落 FINALIZED 最大者；恢复目标 isCurrent=true 其余 false 原子翻转；无候选时清空遗留 DRAFT 的 isCurrent 恢复「无 current 版本」不变量）→ setStatus(ACTIVE) → updateEntity。`IErpCtContractBiz` 契约同步。
      - Skill: `nop-backend-dev`
- [x] `Add` 错误码（D1/守卫码：`ERR_CT_AMOUNT_MISMATCH`/`ERR_CT_DATE_RANGE_INVALID` + 按需 `ERR_CT_VERSION_EXISTS` 等）+ 参数表注册。
      **落地记录**：`ErpCtErrors.ERR_CT_AMOUNT_MISMATCH`（erp.err.ct.amount-mismatch，参数 contractCode/totalAmount/sumLineAmount）+ `ERR_CT_DATE_RANGE_INVALID`（erp.err.ct.date-range-invalid，参数 contractCode/startDate/endDate），描述中文。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 创建校验接线且运行时拒绝金额/日期不一致合同（GraphQL save 实证拒绝）
- [x] submit 建 v1 自动创建（GraphQL 实调断言版本行落库）+ Bean 8 边元数据同步
- [x] rejectAmend 恢复 ACTIVE + 版本 isCurrent 恢复（实调断言）

### Phase 3 - amend 行语义确认 + 测试矩阵

Status: completed
Targets: `ErpCtContractAmendProcessor.java`（按 D3 裁决）；测试类（新增 `TestErpCtContractCreateValidate` 或扩展既有）
Skill: `nop-backend-dev` + `nop-testing`（混合：amend 行语义项属后端，测试矩阵项属测试）

- Item Types: `Fix | Add | Proof`
- Prereqs: Phase 2 完成

- [x] `Fix`/`Add` amend 行复制/保留（按 D3 裁决）：选项 A 下补注释/owner doc 注记 + 确保 amend 后行仍可编辑（既有行为验证）；选项 B 下逐行复制实现。
      **落地记录**：D3 选项 A（行保留即满足）——零复制代码；`ErpCtContractAmendProcessor.amend` 增 D3 语义注释（同合同 amend 模型 ⇒ 复制语义 = 行保留；逐行 newEntity 复制产生重复行与同合同模型冲突）；测试⑤ `testAmendKeepsLinesRetained` 断言 amend 后行保留 + 行可编辑。
      - Skill: `nop-backend-dev`
- [x] `Add` 测试组：① 创建校验（totalAmount 匹配放行/不匹配拒绝 + 日期倒置拒绝 + 缺失容忍）；② submit（DRAFT→NEGOTIATION + v1 自动创建断言 + 非 DRAFT 拒绝 + **已有版本放行并保留既有 DRAFT current 版本不动**[amend→submit 场景，MAJOR-1 语义]）；③ **全链回归双路径**：submit→finalizeVersion(v1)→activate（零版本首提路径）+ **amend→submit→finalizeVersion(v2)→activate**（amend 路径，MAJOR-3 修正——activate 前显式 finalizeVersion 契约）；④ rejectAmend（DRAFT→ACTIVE + **SIGNED 优先最大 versionNo 恢复 isCurrent 断言**[D5 选项 B 语义] + 非 DRAFT 拒绝 + **重复 amend→rejectAmend 周期二次恢复目标正确性**——遗留 DRAFT 行不被误恢复 + **finalize-then-reject 边界**：amend→finalizeVersion(v2)→rejectAmend → 恢复 SIGNED v1 而非 FINALIZED v2，iteration-4 断言）；⑤ amend 行保留/复制语义（D3 结果断言）；⑥ GraphQL RPC 冒烟 + 快照录制。
      **落地记录**：新增 `TestErpCtContractCreateValidate`（16 @Test 全绿）——① 创建校验 5（缺失容忍放行/嵌套行匹配放行/嵌套行不匹配拒绝/有行 totalAmount 缺失拒绝/日期倒置拒绝）；② submit 4（v1 自动创建+NEGOTIATION/非 DRAFT 拒绝/已有版本保留 DRAFT current 不动[MAJOR-1]/DAO 行口径金额不一致拒绝）；③ 全链双路径 2（submit→finalizeVersion(v1)→activate 首提 + amend→submit→finalizeVersion(v2)→activate[MAJOR-3]）；④ rejectAmend 4（SIGNED 最大恢复/非 DRAFT 拒绝/finalize-then-reject 边界[iteration-4]/重复周期二次恢复[MAJOR-2]）；⑤ amend 行保留 1；GraphQL RPC 实调全覆盖（无仅静态接线）。
      - Skill: `nop-testing`
- [x] `Proof` 既有 erp-ct-service 测试零回归：`mvn test -pl module-contract/erp-ct-service`（~77 基线 + 新增全绿——特别核验 TestErpCtContractPosting/TestErpCtContractTerminate 种子调整后行为）。
      **落地记录**：`mvn test -pl module-contract/erp-ct-service` 全模块 **98 tests 全绿**（82 既有基线 + 16 新增，0 failures / 0 errors）——TestErpCtContractPosting 4 / TestErpCtContractCrudSmoke 5 / TestErpCtContractTerminate 6 / TestErpCtESignature 19 / TestErpCtContractRebate 8 / TestErpCtRebateSettlementEnd 2 / TestErpCtResponseMasking 4 / statemachine 矩阵 5 类（9+7+7+3+3）零回归。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 新增测试组全绿 + erp-ct-service 全模块零回归（BUILD SUCCESS）
- [x] submit/rejectAmend/校验有运行时断言证据（GraphQL RPC 实调，非仅静态接线）

### Phase 4 - 文档回填 + arm-index/roadmap 状态

Status: completed
Targets: `docs/design/contract/state-machine.md`；`docs/audits/arm-index.md`；`docs/backlog/requirement-compliance-roadmap.md`；`docs/logs/2026/08-15.md`
Skill: none

- Item Types: `Add | Fix`
- Prereqs: Phase 1-3 完成

- [x] `Add` owner doc 注记：`state-machine.md §2` 漂移注记（DRAFT→NEGOTIATION successor）更新为已实现（submit 边 + v1 创建语义 + **amend 路径 activate 前置 finalizeVersion 契约[D6 裁决]**）+ §适用对象二 版本轴初始态注记 + amend 行保留语义（D3 裁决 + arm-index 落地摘要显式记录「同合同模型 ⇒ 复制语义 = 行保留」防 compliance 漂移）+ rejectAmend 恢复语义（D5 选项 B：SIGNED 优先最大 versionNo，无 SIGNED 回落 FINALIZED）+ D6 watch-only 登记（activate 对非 FINALIZED current 静默跳过）；不修改需求契约段（use-cases L1 不动）。
      - Skill: none
- [x] `Add` arm-index P1-RC-072/073 → `done (RC-R1.32)` + 修复落地摘要；roadmap RC-R1.32 → done ✅（含落地摘要）；`docs/logs/2026/08-15.md` 日志条目写入。
      - Skill: none

Exit Criteria:

- [x] arm-index/roadmap 状态回填 + owner doc 注记落盘 + 日志条目写入

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 ses_ffdece74dffeafRYeloImNFbm1）— 0 BLOCKER / 2 MAJOR / 4 MINOR。MAJOR-1 = Goals「submit 已有版本时拒绝」与 amend 生命周期死锁（amend 后合同 = DRAFT 且已有版本[v1 false + v2 DRAFT true]，submit 是变更单唯一前向出口）——Goals 修正为「仅守卫 status==DRAFT；零版本建 v1 / 已有版本保留既有 DRAFT 当前版本不动」；MAJOR-2 = rejectAmend「最近非当前版本」恢复目标在重复 amend/reject 周期后误选（二次 amend 遗留 DRAFT 行被恢复为 current 产生 ACTIVE+DRAFT-current 不一致态）——新增 Decision D5（amend 时捕获前任当前版本快照 vs 取 FINALIZED/SIGNED 最大 versionNo）+ 遗留版本行去留裁决；MINOR 全部修正（D2 证据句改写[种子直接写 FINALIZED 非经 finalizeVersion] + 引用 §2 迁移表行 42 / D4 理由链补 roadmap 权威 + G6 同域 + P1-RC-078 跨域 ask-first 先例 / Phase 3 skill 改混合声明 / D1 补行金额数据源与 §2 前置条件引用）。
- Independent draft review iteration 2: needs revision（独立子代理 ses_ffde5fc49ffej07avJm09HO5Jy）— 0 BLOCKER / 3 MAJOR / 2 MINOR。MAJOR-1 = 修订未传播：Goals/Phase 3 测试组 ② 仍写「已有版本拒绝」与修正语义矛盾——两处改「已有版本放行并保留 current 版本不动」；MAJOR-2 = Phase 2 rejectAmend 项仍写「版本遍历找最近非当前版本 setCurrent(true)」——改写为「恢复 D5 捕获的前任当前版本行」；MAJOR-3 = amend→submit 后 v2 DRAFT current 在 NEGOTIATION，activate 级联 signVersion 仅对 FINALIZED 生效（`ErpCtContractActivateProcessor:52-55`）致 activate 静默跳过签署——Goals 补「activate 前须显式 finalizeVersion(v2)」+ 新增 Decision D6（activate 对非 FINALIZED current 守卫补齐 vs watch-only 登记）+ Phase 3 测试 ③ 改双路径（首提 submit→finalizeVersion→activate + amend→submit→finalizeVersion→activate）；MINOR 全部修正（submit 形态决策前提纠错[activate/amend 为 Processor、suspend/resume/terminate/expire 为直写两类并存] / Phase 4 arm-index 落地摘要显式记录 D3 解释防 compliance 漂移）。
- Independent draft review iteration 3: needs revision（独立子代理 ses_ffde18fbeffe9lzCVaRSDUlszP）— 0 BLOCKER / 2 MAJOR / 0 MINOR。MAJOR-A = Goals 测试 bullet（:39）仍残留「已有版本拒绝」第三处矛盾——改「已有版本放行并保留既有 DRAFT current 版本不动」；MAJOR-B = D5 选项 A 快照载体缺口（amend 与 rejectAmend 是独立 @BizMutation 调用/事务，内存快照跨请求不可得；Bean 实例字段共享单例并发/重启危害；ORM 新列触 ask-first 越界）——D5 翻转推荐选项 B（恢复时取 status∈{FINALIZED,SIGNED} 中 versionNo 最大者，跨请求可行 + 对遗留 DRAFT 行免疫）+ Phase 2 rejectAmend 实现项与 Phase 3 测试 ④ 同步改选项 B 语义。
- Independent draft review iteration 4: needs revision（独立子代理 ses_ffdddc7ecffet33GXHTEbC4ht6）— 0 BLOCKER / 0 MAJOR / 1 MINOR。MINOR = D5 选项 B 过滤 {FINALIZED,SIGNED} 在 finalize-then-reject 可达路径（amend→finalizeVersion(v2)→rejectAmend，守卫仅查合同头 status==DRAFT）下会把未签署的 FINALIZED v2 恢复为 current，产生 ACTIVE 合同 + 未签署 current 版本不一致态，与 Goals「前任当前版本」语义偏离——D5 修正为 **SIGNED 优先最大 versionNo（无 SIGNED 回落 FINALIZED）**（ACTIVE 态下 activate 保证 current 为 SIGNED，SIGNED 优先恒与 amend 前 current 重合）+ Phase 2 实现项与 Phase 3 测试 ④ 同步（含 finalize-then-reject 边界断言）+ Phase 4 owner doc 注记同步。
- Independent draft review iteration 5: needs revision（独立子代理 ses_ffddaca4fffeeqjGPPz8gjb249）— 0 BLOCKER / 0 MAJOR / 1 MINOR。MINOR = Goals rejectAmend bullet（:38）仍呈 iteration-4 前形式「status∈{FINALIZED,SIGNED} 中 versionNo 最大者」（并集过滤变体）——与 D5/Phase 2/Phase 3 ④/Phase 4 的 SIGNED 优先语义不一致（文本一致性传播缺口，无行为风险——bullet 委托 D5 且全部执行项已 SIGNED 优先）——修正为「取 SIGNED 最大 versionNo（无 SIGNED 回落 FINALIZED 最大者）」。

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。**完整仓库验证在此处**：结束时运行一次全量验证。

- [x] 范围内行为完成——P1-RC-072/073 创建校验 + submit v1 + amend 行语义 + rejectAmend 恢复运行时成立（独立结束审计复核：`ErpCtContractBizModel` 六 helpers + 两 mutation 实调 + 16 @Test 运行时断言全覆盖）
- [x] 相关文档对齐——arm-index/roadmap/owner doc/日志回填（独立结束审计 grep 复核全部落盘）
- [x] 已运行验证（`mvn test -pl module-contract/erp-ct-service` 全绿 + `mvn clean install -DskipTests` 全量 + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline）——独立结束审计实测复核通过
- [x] 无范围内项目降级为 deferred/follow-up（范围内项全 landed；Deferred 三项均有裁决记录，无一为已确认缺陷/契约漂移）
- [x] 独立草案审查已完成并记录（5 iterations 全记录于 Draft Review Record）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致（独立结束审计逐项核对通过）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（本独立结束审计子代理逐项核验后勾选，见 Closure Audit Evidence）
- [x] 结束证据存在于文件中（Closure 节已落盘实测证据）

## Deferred But Adjudicated

（本行结束审计时按实际裁决登记。draft 期已识别候选，供结束审计前定稿：

### P1-RC-072 ③ 预算校验 hook + 特批流程

- Classification: `watch-only residual`
- Why Not Blocking Closure: roadmap RC-R1.32 行（权威 R1.0 展开范围）未列预算 hook；展开器 G6 分类「同域」而预算是 contract→finance 跨域契约（P1-RC-078 先例 ask-first）；特批流程与 RC-R1.34 审批引擎语义重叠。D4 范围门裁决后登记。
- Successor Required: `yes`（触发条件 = 预算特批业务流立项 / finance budget 跨域契约行启动 / roadmap 行范围修订纳入③）

### 前端 submit/rejectAmend AMIS 按钮接线

- Classification: `watch-only residual`
- Why Not Blocking Closure: 本行落地后端 mutation 能力面；前端按钮接线属 UI 增强非 L1 后端义务。
- Successor Required: `no`

### amend 遗留 DRAFT 版本行去留

- Classification: `optimization candidate`
- Why Not Blocking Closure: rejectAmend 后 amend 期间创建的 DRAFT 版本行保留为孤儿历史（逻辑删除语义不破坏）；是否清理归 D5 裁决，属数据卫生优化非契约义务。
- Successor Required: `no`）

## Closure

Status Note: 四 Phase 全执行完成（2026-08-15）——Phase 1 D1-D6 决策落盘 + 误伤面核查零调整；Phase 2 创建校验（D1 语义）+ submit/rejectAmend mutation + Bean 9 边落地；Phase 3 测试矩阵 16 组全绿 + 全模块 98 tests 零回归；Phase 4 文档回填（state-machine.md §2 漂移注记更新为已实现 + §适用对象二 版本族裁定注记 + arm-index P1-RC-072/073 → done (RC-R1.32) + roadmap RC-R1.32 → done ✅ + 日志）。验证：`mvn test -pl module-contract/erp-ct-service` 98/98 全绿 + `mvn clean install -DskipTests` 全量 BUILD SUCCESS + `mvn test` 全 reactor 仅 3 项已登记 known failures（2026-08-14 预存，app-erp-all，与 contract 无关）+ checker actual ≤ baseline 零漂移。8 项 Closure Gates 执行者保守未勾选（对齐先例 2026-07-01-1132-1 P2 / 同批 R1.31），逐项核验归独立结束审计子代理（新会话）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（mission-driver closure auditor，新会话无执行者上下文）
- Evidence: （1）**实仓代码复核**——`ErpCtContractBizModel.java`（402 行）：`defaultPrepareSave:74-84` 接线 `validateCreateFields`（D1 语义，in-memory 实体图口径）+ `submit:86-105`（守卫 `assertCanSubmitForNegotiation` → `validateContractFields` DAO 口径 → `ensureVersionOnSubmit` 零版本建 v1 → NEGOTIATION）+ `rejectAmend:107-123`（守卫 `assertCanRejectAmend` → `restoreCurrentVersion` D5 选项 B → ACTIVE），六 helpers 全部实装无空壳；`ErpCtContractStateMachine.java` 9 边（submit/rejectAmend 新 2 边 + `transitions()` 元数据同步）；`ErpCtErrors.java:50-54` 错误码注册；`IErpCtContractBiz` 契约 submit/rejectAmend 同步；`ErpCtContractAmendProcessor` D3 行保留注释（零复制代码）；矩阵测试 `TestErpCtContractStateMachineMatrix` 7→9 边断言翻转。（2）**测试实测**：`mvn test -pl module-contract/erp-ct-service` — **Tests run: 98, Failures: 0, Errors: 0**（16 新增 `TestErpCtContractCreateValidate` + 82 基线零回归），GraphQL RPC 实调断言全覆盖。（3）**全量构建实测**：`mvn clean install -DskipTests` 全 reactor **BUILD SUCCESS**。（4）**合规检查实测**：`bash docs/audits/nop-compliance-checker.sh` actual 全 16 规则 == `compliance-baseline.md`（R1d=14 R2a=34 R2b=230 R2c=1394 R2d=34 R3=5 R4=0 R5=0 R6=2 R7=0 R8=0 R10=9 R11=0 R12a=69 R12b=66 R12c=40）**零漂移**。（5）**文档复核**：`state-machine.md` §2 漂移注记更新为已实现（:44/:59-63）+ §适用对象二 版本族裁定注记（:208-212）+ 审查提示（:347）；arm-index P1-RC-072（:258）/P1-RC-073（:259）→ `done (RC-R1.32)` 含落地摘要（显式记录「同合同模型 ⇒ 复制语义 = 行保留」）；roadmap RC-R1.32（:424）→ `done ✅`；`docs/logs/2026/08-15.md` 日志条目（含 98 tests 验证状态）。（6）**五维一致**：Plan Status / 4 Phase Status / 各 Exit Criteria / 8 Closure Gates / Closure 证据 / 日志全部一致；Deferred 三项（预算特批 watch-only + 前端按钮 watch-only + 遗留 DRAFT 行 optimization）均有裁决与触发条件，无隐藏缺陷或契约漂移。

Follow-up:

- 预算特批流程（P1-RC-072 ③）：触发条件 = 预算特批业务流立项 / finance budget 跨域契约行启动 / roadmap 行范围修订纳入（见 Deferred But Adjudicated）。
- 前端 submit/rejectAmend AMIS 按钮接线：UI 增强非本行义务，Successor Required: no。
- amend 遗留 DRAFT 版本行清理：数据卫生优化，Successor Required: no。
