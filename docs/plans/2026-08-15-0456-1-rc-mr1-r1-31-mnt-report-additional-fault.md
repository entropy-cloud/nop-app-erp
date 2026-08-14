# 2026-08-15-0456-1-rc-mr1-r1-31-mnt-report-additional-fault RC-R1.31 — maintenance 额外故障另开新请求（MR1 第一批纯预授权）

> Plan Status: completed
> Last Reviewed: 2026-08-15
> Mission: requirement-compliance
> Work Item: RC-R1.31（P1-RC-069 maintenance UC-MAIN-07 C——reportAdditionalFault 编排缺失）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.31 行 + `docs/audits/arm-index.md` P1-RC-069 行 + 展开器映射 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（纯 BizModel/Processor 代码逻辑预授权）
> Related: `docs/design/maintenance/use-cases.md`（L1 UC-MAIN-07 :121-130）；`docs/design/maintenance/state-machine.md`（§4 额外故障分支）；`docs/audits/2026-08-08-0135-rc-ma4-a4-2-147-154-maintenance-runtime.md`（A4.2.154 前端半自动流程运行时证据）；`docs/plans/2026-08-15-0320-3-rc-mr1-r1-30-mnt-schedule-conflict-personnel-dimension.md`（同域先例）
> Audit: required

## Current Baseline

- **finding P1-RC-069（arm-index 行，UC-MAIN-07 C）**：L1（`use-cases.md:129`）逐字「维护访问 IN_PROGRESS → 发现额外故障；本次访问记录(备注/工时), 不中断本次维护；另开新维护请求(OPEN)处理额外故障」——L1 显式 **另开新维护请求(OPEN)编排**。L2（`state-machine.md §4`）活跃要求。L3 实仓：`ErpMntVisit` 实体含 `result`/`remark` 字段（UC-MAIN-07 B 载体存在 ✅）；**grep `additionalFault|additionalIssue|openNewRequest|额外故障` 跨 module-maintenance 零命中**——无「维护访问 IN_PROGRESS → 另开新维护请求(OPEN)」编排方法。❌ UC-MAIN-07-C 完全缺失。L4：零 dedicated 测试。§4 三判据（plan-audit / owner doc Deferred / product-scope 裁剪）均不成立 → Q4=(a) 强制实现 P1。
- **A4.2.154 运行时证据**（`2026-08-08-0135-rc-ma4-a4-2-147-154-maintenance-runtime.md`，维持 P1-RC-069）：前端 AMIS census——`ErpMntVisit.view.xml` view/edit 表单 remark/result + 执行向导按钮 :122-126 + `ErpMntRequest.view.xml` 手工 CRUD[含 _gen save/id + accept/complete/rejectRequest/cancel]全可达——半自动流程现状确认，**后端编排方法仍缺失**。
- **实仓（HEAD 核查）**：
  - `ErpMntVisitBizModel.java`（55 行）：schedule/start/complete/cancel 四个 per-mutation Processor 委托，**无 reportAdditionalFault**。注入 `ErpMntVisitCompleteProcessor` 等四 Processor。
  - `ErpMntVisit` ORM（`app-erp-maintenance.orm.xml:249-283`）：`result`（propId 14，dict erp-mnt/visit-result）+ `remark`（propId 15，domain remark）+ `visitType`（propId 13，dict erp-mnt/visit-type）+ `status`（dict erp-mnt/visit-status）。visit 无 requestId 字段（P1-RC-067 触 ORM ask-first，**非本行范围**）。
  - `ErpMntRequest` ORM（`app-erp-maintenance.orm.xml:344-396`）：`code`（tagSet="var"）+ `equipmentId`（mandatory）+ `requestDate`（mandatory）+ `description`（mandatory，precision 2000）+ `priority`（mandatory，dict erp-mnt/priority）+ `status`（mandatory，dict erp-mnt/request-status，OPEN 初始）+ `requestedBy`（mandatory）+ `assignedTo`（可选）+ `remark`（可选）。
  - 既有建单先例：`ErpMntRequestAcceptProcessor.generateResponsiveVisit:33-42` 用 `visitBiz.save(data, context)`（data map 显式 code `"VST-REQ-"+request.getId()` + equipmentId + visitDate + status + visitType + assignedTo）——**建单载体范式**：IBiz.save(data map) + 显式 code 模式。`IErpMntRequestBiz extends ICrudBiz<ErpMntRequest>`（标准 save 可用）。
  - 状态常量：`ErpMntDaoConstants.REQUEST_STATUS_OPEN`（request-status dict OPEN）+ `VISIT_STATUS_IN_PROGRESS`。
  - 错误码：`ErpMntErrors.java:30-33` `ERR_VISIT_NOT_FOUND` / `ERR_INVALID_VISIT_STATUS_TRANSITION` 既有——额外故障编排需要 visit 非 IN_PROGRESS 时的守卫错误码（**新增 `ERR_VISIT_NOT_IN_PROGRESS` 或复用既有码，Decision 项**）。
  - 测试基线：`TestErpMntVisitRequestStateMachine` 15 @Test（10 既有 + R1.30 新增 5 组）+ erp-mnt-service 99 tests 全绿基线（R1.30 closure 记录）。
- **预授权判据**（第一批纯预授权）：纯 BizModel/Processor 代码逻辑（编排方法 + 状态守卫 + 错误码 + 测试），**不触 ORM 结构/会计过账/删除**；roadmap RC-R1.31 行 `todo`，Deps（R1.0 done）已满足。
- **涉及文件**：`module-maintenance/erp-mnt-service/.../entity/ErpMntVisitBizModel.java`（新增 mutation）；`module-maintenance/erp-mnt-service/.../processor/ErpMntVisitReportAdditionalFaultProcessor.java`（新建，镜像同目录 per-mutation Processor 范式）；`module-maintenance/erp-mnt-dao/.../biz/IErpMntVisitBiz.java`（接口契约，`erp-mnt-dao/src/main/java/app/erp/mnt/biz/`）；`ErpMntErrors.java`（按 Decision）；`ErpMntConstants.java`（按需）；`module-maintenance/erp-mnt-service/src/test/.../TestErpMntVisitRequestStateMachine.java` 或新测试类；`docs/design/maintenance/state-machine.md` + `docs/audits/arm-index.md` + `docs/backlog/requirement-compliance-roadmap.md` + `docs/logs/2026/08-15.md`（回填）。

## Goals

- **reportAdditionalFault 编排运行时成立（P1-RC-069 核心）**：`ErpMntVisitBizModel#reportAdditionalFault` @BizMutation（visitId + 额外故障描述/备注入参）——守卫 visit.status==IN_PROGRESS（非 IN_PROGRESS 拒绝）→ 记录本次 visit remark（追加或覆盖，**Decision 项 E3**：remark 拼接语义）→ 经 `IErpMntRequestBiz.save` 建新 `ErpMntRequest`（status=OPEN、equipmentId=同 visit、requestDate=今天、description=额外故障描述、priority 按入参/默认、requestedBy=当前用户或 visit.assignedTo，**Decision 项 E1**：requestedBy 取值）→ **不中断本次维护**（不翻转 visit 状态、不影响 start/complete 链）。返回新建 request。**result 语义与工时范围按 Decision E4 裁决**（L1「本次访问记录(备注/工时)」：工时归 complete 流程总分钟计算，本 mutation 不写 totalMinutes——范围声明见 Non-Goals/E4）。
- **守卫与错误码可追溯**：非 IN_PROGRESS visit 拒绝且有领域错误码（错误码裁决复用 `ERR_INVALID_VISIT_STATUS_TRANSITION` 或新增 `ERR_VISIT_NOT_IN_PROGRESS`，**Decision 项**）。
- **测试**：新增测试组——① IN_PROGRESS visit → reportAdditionalFault → 新 request 落库（status=OPEN + equipmentId 同 visit + description 正确）+ visit 保持 IN_PROGRESS（不中断）；② 非 IN_PROGRESS（DRAFT/SCHEDULED/COMPLETED）→ 拒绝 + 零 request 落库；③ visit remark 追加语义；④ visit 不存在 → ERR_VISIT_NOT_FOUND；⑤ 新 request 可继续走 accept→startRepair 全链（闭环可操作性）；⑥ GraphQL RPC 冒烟。
- **零回归**：erp-mnt-service 既有测试全绿（99 基线）+ 全仓构建 + compliance checker 零漂移。
- **回填**：arm-index P1-RC-069 → `done (RC-R1.31)` + roadmap 行 → `done` + owner doc 注记 + `docs/logs/` 日志条目。

## Non-Goals

- **不实现 P1-RC-067 visit→request 自动联动**（`ErpMntVisitCompleteProcessor#complete` 自动回写 request COMPLETED + `ErpMntVisit.requestId` 列——触 ORM ask-first，独立行 RC-R1.75）。
- **不实现 P1-RC-068 跨域事件发布 / P1-RC-070 资产处置联动 / P1-RC-071 OEE**（独立行，均非本行范围）。
- **不写 visit.totalMinutes**（工时由 complete 流程按 startTime→endTime 计算，本 mutation 处于 IN_PROGRESS 期不写——E4 范围声明）。
- **不写 visit.result**（result 是 complete 语义的执行结果 dict，额外故障上报不改变本次维护执行结果——E4 默认裁决，备选仅存于 Decision 记录）。
- **不触 ORM 结构**（零列/零索引变更；requestId FK 不在本行）。
- **不改真相源契约段落**（use-cases L1 不动）。
- **不做前端 AMIS 按钮接线**（本行后端编排方法；前端按钮属 UI 增强，A4.2.154 已证实半自动路径存在，按 Deferred But Adjudicated 登记）。
- **不做 request code 自动生成机制改造**（沿用既有显式 code 范式，见 Execution Plan Decision）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧的预授权代码逻辑修复，Q4=(a) 强制实现禁止方案 B）
- Owner Docs: `docs/design/maintenance/use-cases.md`（L1 UC-MAIN-07 :121-130）+ `docs/design/maintenance/state-machine.md`（§4）+ `docs/audits/2026-08-08-0135-rc-ma4-a4-2-147-154-maintenance-runtime.md`（A4.2.154 运行时证据）
- Skill Selection Basis: 实现面 = per-mutation Processor + IBiz.save 跨实体调用 + 错误码（`nop-backend-dev`：Processor 模式、跨实体访问范式、错误码范式）；测试（`nop-testing`：JunitAutoTestCase + GraphQL RPC 冒烟 + _cases 快照范式）。无 view.xml/xbiz/ORM 变更。

## Infrastructure And Config Prereqs

- 无新 config key/环境变量/外部服务（request 建单走既有 IBiz.save 载体，无依赖）。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-maintenance/erp-mnt-service`。

## Execution Plan

### Phase 1 - Explore 建单载体与语义裁决（Decision）

Status: completed
Targets: `ErpMntVisitBizModel.java`；`ErpMntRequestAcceptProcessor.java`；`ErpMntErrors.java`；`TestErpMntVisitRequestStateMachine.java`
Skill: `nop-backend-dev`

- Item Types: `Decision | Proof`
- Prereqs: 无（既有基线）

- [x] `Decision` **建单载体裁决（E1）**：经 `IErpMntRequestBiz.save(Map, context)` data map 建单（镜像 `generateResponsiveVisit:33-42` 范式）；**code 唯一性裁决（重复上报场景）**——本 mutation 不翻转 visit 状态（不中断维护）且无单次上报守卫，visit 保持 IN_PROGRESS 期间可被**合法多次调用**（L1「另开新维护请求」未限定每 visit 一次），固定 code `"REQ-VST-"+visit.getId()` 在第二次上报时必然撞 `UK_MNT_REQUEST_CODE`（先例 `VST-REQ-{requestId}` 安全仅因 accept 是一次性迁移 OPEN→ACCEPTED 守卫）：选项 A（裁决候选）：code 加时间后缀 `"REQ-VST-"+visit.getId()+"-"+yyyyMMddHHmmss`（**首选子变体**——本行范围无 visit↔request FK[P1-RC-067 越界]，上报序号无可计算基座[equipmentId 代理混淆无关请求]，时间戳唯一性充分且 requestCode precision 50 内[8+19+1+14=42 ≤ 50]）；选项 B：显式单次上报守卫（visit 已有关联额外故障 request 则拒绝——需查重查询，且与 L1「另开新维护请求」多故障场景语义冲突）。建议选项 A（时间戳后缀），同时保留 visit 可多故障多次上报语义。equipmentId=visit.equipmentId、requestDate=CoreMetrics.currentDate()、description=入参、priority=入参（null 时默认 `ErpMntDaoConstants.PRIORITY_NORMAL`——dict erp-mnt/priority 为 LOW/NORMAL/HIGH/URGENT）、requestedBy=context userId 或 visit.assignedTo（null 回退——**决策记录**）、status=REQUEST_STATUS_OPEN。选项 C（经 daoProvider.daoFor 直建）否决——对齐跨实体访问规则「优先 IBiz」。记录理由 + 备选。
      - Skill: `nop-backend-dev`
- [x] `Decision` **守卫错误码裁决（E2）**：复用 `ERR_INVALID_VISIT_STATUS_TRANSITION`（参数 visitCode + currentStatus + expectedStatus）vs 新增 `ERR_VISIT_NOT_IN_PROGRESS`（`erp.err.mnt.visit-not-in-progress`）。建议：复用既有码（同域既有语义「非法状态迁移」，避免错误码膨胀，对齐 R1.30 E1 复用先例）——grep 消费者面确认后记录。**决策记录理由 + 备选**。
      - Skill: `nop-backend-dev`
- [x] `Decision` **visit remark 追加语义裁决（E3）**：reportAdditionalFault 对 visit.remark 的写入——(a) 覆盖（新描述替换）/ (b) 追加（`既有 remark + "\n[额外故障] " + 描述`，精度 1000 截断守卫）/ (c) 不写 remark 仅 result。建议 (b) 追加（保留本次维护既有记录 + L1「本次访问记录」语义）+ 长度守卫。**决策记录理由 + 备选**。
      - Skill: `nop-backend-dev`
- [x] `Decision` **result 语义与工时范围裁决（E4）**：L1「本次访问记录(备注/工时)」——(a) result 字段：额外故障上报是否写 visit.result（建议：不写——result 是 complete 语义的执行结果 dict，额外故障上报不改变本次维护执行结果，理由记录；备选：入参显式传入则写）；(b) 工时（totalMinutes）：由 complete 流程按 startTime→endTime 计算（`ErpMntVisitCompleteProcessor.doComplete:45-48`），本 mutation 处于 IN_PROGRESS 期写 totalMinutes 会被 complete 覆盖——**范围声明：本 mutation 不写 totalMinutes，工时记录义务归 complete 流程**（对齐 L1「不中断本次维护」语义）。**决策记录理由 + 备选**。
      - Skill: `nop-backend-dev`
- [x] `Proof` **既有测试误伤面核查 + UK 冲突风险核查**：grep maintenance 测试集 `ErpMntVisit__save`/`ErpMntRequest__save` 调用面 + IN_PROGRESS visit 种子结构（`seedVisit` 直建 + start 迁移路径）——确认新增 mutation 与既有测试零冲突；**显式核查 E1 重复上报场景**（同 visit 二次 reportAdditionalFault → code 唯一性方案 A 后缀序号下零 UK 冲突——测试矩阵补重复上报用例，不预判结论）。
      - Skill: `nop-testing`

**Phase 1 决策记录（2026-08-15 执行落盘，含理由 + 备选）：**

- **E1 建单载体（采纳：IBiz.save data map + 毫秒时间戳后缀）**：载体经 `IErpMntRequestBiz.save(Map, context)`（`IErpMntRequestBiz extends ICrudBiz<ErpMntRequest>` 标准 save 可用，镜像 `generateResponsiveVisit:33-42`）。code 唯一性**终裁 = `"REQ-VST-" + visit.getId() + "-" + yyyyMMddHHmmssSSS`（毫秒精度 17 位）**——核查结论：`MntFrozenClockExtension` 经 `ThreadLocalFrozenClock` 仅冻结 `currentDate()/currentDateTime()`（`ThreadLocalFrozenClock.java:64-84`），`currentTimeMillis()` 委托 `CoreMetrics.defaultClock()`（系统真实时钟）→ 测试下时间戳单调真实；秒级后缀（draft 首选子变体 14 位）在同秒二次调用（测试两连 RPC 间隔 >1ms 但可同秒）有碰撞风险，**毫秒级消除该风险**（同毫秒双报残余窗口极小，登记为文档化残余）；长度 8+19+1+17=45 ≤ 50 ✓。快照安全核查：autotest 框架对 GraphQL 新建实体 prop 以 `@var:ErpMntRequest@code` 掩码录制（`TestErpMntVisitCrudSmoke/testCreateHead/output/response.json5` 先例实证）→ 动态 code 不破坏 CHECKING 回放。requestedBy 取值 = `context.getUserId()`（String）→ Long 转换，null/空白回退 `visit.getAssignedTo()`（seed 恒设，生产语境执行中 visit 必有执行人）。remark 入参（可选）写入新 request.remark（请求级备注，见 E3）。备选记录：选项 B 单次上报守卫（查重查询 + 与 L1 多故障语义冲突 → 拒绝）；选项 C daoProvider.daoFor 直建（违反跨实体访问「优先 IBiz」→ 拒绝）；count 序列序号（draft review iteration 3 已裁决无可计算基座 + 需额外查询 → 拒绝）。
- **E2 守卫错误码（采纳：复用 `ERR_INVALID_VISIT_STATUS_TRANSITION`）**：经 `AbstractErpMntVisitProcessor.illegalVisitTransition(visit, from, VISIT_STATUS_IN_PROGRESS)` 复用既有码（参数 visitCode/currentStatus/expectedStatus 可追溯，同域既有「非法状态迁移」语义，与 complete/start/cancel 守卫错误码体系一致）。grep 消费者面确认：`ERR_INVALID_VISIT_STATUS_TRANSITION` 仅 define 点（`ErpMntErrors.java:33`）+ 抛点（`illegalVisitTransition`）+ 测试断言（`testVisitTerminalCannotTransition`/`testVisitIllegalTransition`），零前端 i18n 依赖。备选：新增 `ERR_VISIT_NOT_IN_PROGRESS`（拒绝——错误码膨胀 + 与既有守卫码重复，对齐 R1.30 E1 复用先例）。
- **E3 remark 追加语义（采纳：追加 (b) + 精度守卫 + remark 入参归 request）**：`visit.remark = 既有 + "\n[额外故障] " + description`，长度守卫 1000（domain remark precision 1000，`app-erp-maintenance.orm.xml:103`）——既有内容优先保留，截断新增部分；description 空白时以 remark 入参兜底拼入。remark 入参（可选）不混入 visit remark，写入新 request.remark（请求级备注，职责分离）。备选：(a) 覆盖（拒绝——丢失本次维护既有记录）；(c) 不写（拒绝——L1「本次访问记录(备注)」语义）。
- **E4 result 语义与工时范围（采纳：不写 result + 不写 totalMinutes）**：result 是 complete 语义的执行结果 dict（`erp-mnt/visit-result` NORMAL/ABNORMAL/PARTIAL，`_ErpMntDaoConstants.java:194-205`），额外故障上报不改变本次维护执行结果 → **不写**；totalMinutes 由 complete 流程按 startTime→endTime 计算（`doComplete:45-48`），本 mutation IN_PROGRESS 期写入会被 complete 覆盖 → **不写（范围声明）**，工时记录义务归 complete 流程（L1「不中断本次维护」）。备选：入参显式传 result 则写（拒绝——本行无 result 入参，范围最小化，E4 默认裁决固化）。
- **Proof 误伤面/UK 核查结论（零冲突）**：(i) 新 mutation 名 `reportAdditionalFault` 全仓 grep 零命中（全新无重名）；(ii) 既有测试 seed 经 `daoProvider.daoFor(...).saveEntity` 直建（`seedVisit`/`seedRequest`），不经过新 mutation，且既有快照表（`erp_mnt_visit`/`erp_mnt_request`）录制在各自方法目录，新增测试类独立 idSeq + 独立方法零干扰；(iii) UK 冲突：`erp_mnt_request` 唯一约束仅 `UK_MNT_REQUEST_CODE`（orm.xml:371-373），生成 code 前缀 `REQ-VST-{id}-` 与既有种子 `REQ-ACCEPT-001`/`REQ-FULL-001` 及 `ErpMntRequest__save` 快照 code（`@var` 掩码）不冲突；(iv) 同 visit 二次上报（IN_PROGRESS 保持可重入）在毫秒时间戳后缀下 code 唯一——测试矩阵补重复上报用例实证（不预判，实测见 Phase 3）。

Exit Criteria:

- [x] E1-E4 四项决策记录落盘（含理由 + 备选）+ 误伤面/UK 冲突核查结论（零冲突或已识别调整点）
- [x] 建单载体范式确认（IBiz.save data map + code 唯一性方案 + mandatory 字段全集）

### Phase 2 - reportAdditionalFault 编排落地（P1-RC-069 核心）

Status: completed
Targets: `ErpMntVisitBizModel.java`；`ErpMntVisitReportAdditionalFaultProcessor.java`（新建）；`IErpMntVisitBiz.java`；`ErpMntErrors.java`（按 E2）
Skill: `nop-backend-dev`

- Item Types: `Fix | Add`
- Prereqs: Phase 1 完成

- [x] `Add` `ErpMntVisitReportAdditionalFaultProcessor`（per-mutation Processor，**扩展 `AbstractErpMntVisitProcessor`**（继承 requireVisit/visitDao/illegalVisitTransition 共享 helper）+ 注入 `IErpMntRequestBiz`）：`reportAdditionalFault(visitId, description, priority?, remark?, context)` = requireVisit → 状态守卫（E2 码）→ visit remark 追加（E3 语义，updateEntity）→ `requestBiz.save(data map, context)` 建新 OPEN request → 返回 request。**不翻转 visit 状态**（不中断本次维护）。
      - Skill: `nop-backend-dev`
- [x] `Add` `IErpMntVisitBiz` 增 `reportAdditionalFault` 契约 + `ErpMntVisitBizModel` @BizMutation 委托（镜像 schedule/start/complete/cancel 既有委托模式）。
      - Skill: `nop-backend-dev`
- [x] `Add` 错误码（按 E2 裁决：复用则零新增；新增则 `ERR_VISIT_NOT_IN_PROGRESS` define + 参数表）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 编排方法接线且守卫落地（grep 显示 reportAdditionalFault + IN_PROGRESS 守卫 + IBiz.save 建单）
- [x] 错误参数可追溯（E2 码含 visitCode/currentStatus/expectedStatus 或等价上下文）

### Phase 3 - 测试矩阵

Status: completed
Targets: `module-maintenance/erp-mnt-service/src/test/java/app/erp/mnt/service/`（新增测试类 `TestErpMntVisitReportAdditionalFault` 或并入 `TestErpMntVisitRequestStateMachine`）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2 完成

- [x] `Add` 测试组（按 Goals ①-⑥）：IN_PROGRESS 成功建单（status/equipmentId/description 断言 + visit 保持 IN_PROGRESS）/ 非 IN_PROGRESS 拒绝（DRAFT/SCHEDULED/COMPLETED 参数化）+ 零落库 / remark 追加语义 / visit 不存在 / **重复上报（同 visit 二次调用 → 第二 request 落库成功 + code 唯一，E1 方案 A 验证）** / 新 request 接 accept→startRepair 闭环 / GraphQL RPC 冒烟。快照录制 `_cases/`。
      - Skill: `nop-testing`
- [x] `Proof` 既有 maintenance 测试零回归：`mvn test -pl module-maintenance/erp-mnt-service`（99 基线 + 新增全绿）。
      - Skill: `nop-testing`

Exit Criteria:

- [x] 新增测试组全绿 + erp-mnt-service 全模块零回归（BUILD SUCCESS）
- [x] 编排行为有运行时断言证据（GraphQL RPC 实调 + 状态/落库断言，非仅静态接线）

### Phase 4 - 文档回填 + arm-index/roadmap 状态

Status: completed
Targets: `docs/design/maintenance/state-machine.md`；`docs/audits/arm-index.md`；`docs/backlog/requirement-compliance-roadmap.md`；`docs/logs/2026/08-15.md`
Skill: none

- Item Types: `Add | Fix`
- Prereqs: Phase 1-3 完成

- [x] `Add` owner doc 注记：`state-machine.md §4` 补 reportAdditionalFault 编排实现注记（入参/守卫/建单语义/测试证据 + 与 P1-RC-067 successor 边界声明）；不修改需求契约段（use-cases L1 不动）。
      - Skill: none
- [x] `Add` arm-index P1-RC-069 → `done (RC-R1.31)` + 修复落地摘要；roadmap RC-R1.31 → done ✅（含落地摘要）；`docs/logs/2026/08-15.md` 日志条目写入。
      - Skill: none

Exit Criteria:

- [x] arm-index/roadmap 状态回填 + owner doc 注记落盘 + 日志条目写入

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 ses_ffded039effelwkm7O7G0Y6GCj）— 0 BLOCKER / 1 MAJOR / 6 MINOR。MAJOR = Goals「result 可选更新」违反反松弛规则且 result/工时语义未裁决（L1「本次访问记录(备注/工时)」）——新增 Decision E4（result 不写 + 工时归 complete 流程范围声明）+ Non-Goals 同步；MINOR 全部修正（测试计数 30→15 / generateResponsiveVisit 行号 :52-61→:33-42 / priority 默认 P2→PRIORITY_NORMAL[dict 为 LOW/NORMAL/HIGH/URGENT] / Deferred 段登记具体分类项 / Phase 2 注入改扩展 AbstractErpMntVisitProcessor + 注入 IErpMntRequestBiz / 错误码行号微调）。
- Independent draft review iteration 2: needs revision（独立子代理 ses_ffde6166fffeU6vMVZIpvaTarf）— 0 BLOCKER / 1 MAJOR / 3 MINOR。MAJOR = E1 固定 code `"REQ-VST-"+visit.getId()` 在重复上报（mutation 可重入、visit 保持 IN_PROGRESS）下必然撞 `UK_MNT_REQUEST_CODE`——E1 裁决 code 唯一性方案 A（序号/时间后缀）+ Proof 项改显式核查重复场景 + 测试矩阵补重复上报用例；MINOR 全部修正（Phase 1 Exit Criteria E1-E3→E1-E4 / Non-Goal result 条件式措辞改默认承诺 / 「可选」残留词消除）。
- Independent draft review iteration 3: accept（独立子代理 ses_ffde1a592ffeUpZ2491fYOL6lz）— 0 BLOCKER / 0 MAJOR / 1 MINOR / 1 NIT。E1 唯一性方案确认 + tagSet="var" 澄清（非自动生成机制，显式 code 是既有必要范式）+ code 长度检查（≤42 ≤ 50 precision）通过。MINOR = E1 子变体「上报序号」无可计算基座（无 visit↔request FK）——首选子变体改时间戳后缀 `-yyyyMMddHHmmss`（唯一性充分 + 长度合规）；NIT = 接口路径注记（`erp-mnt-dao/.../biz/` 实际为 `app.erp.mnt.biz`）。**计划可标记 active。**

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。**完整仓库验证在此处**：结束时运行一次全量验证。

- [x] 范围内行为完成——P1-RC-069 reportAdditionalFault 编排运行时成立（守卫 + 建单 + 不中断维护）
- [x] 相关文档对齐——arm-index/roadmap/owner doc/日志回填
- [x] 已运行验证（`mvn test -pl module-maintenance/erp-mnt-service` 全绿 + `mvn clean install -DskipTests` 全量 + `bash docs/audits/nop-compliance-checker.sh` actual ≤ baseline）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

（结束审计时按实际裁决登记，定稿如下：

### 前端 AMIS reportAdditionalFault 按钮接线

- Classification: `watch-only residual`
- Why Not Blocking Closure: 本行落地后端编排能力面（mutation + 守卫 + 建单）；A4.2.154 已证实前端半自动流程（visit remark/result 表单 + request 手工 CRUD）可达，前端一键接线属 UI 增强非 L1 后端义务。
- Successor Required: `no`

### P1-RC-067 requestId 联动（本行边界声明）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: visit→request 结构化关联（requestId 列）触 ORM ask-first，属独立行 RC-R1.75；本行 reportAdditionalFault 建单经 code 前缀/description/equipmentId 隐式关联。
- Successor Required: `yes`（触发条件 = RC-R1.75 启动时）

### 同毫秒双报残余窗口（E1 文档化残余）

- Classification: `watch-only residual`
- Why Not Blocking Closure: code 唯一性 = 毫秒时间戳后缀（`REQ-VST-{visitId}-{yyyyMMddHHmmssSSS}`），同毫秒并发双报存在极小碰撞窗口；毫秒级已消除测试/常规双报碰撞（同秒双报实测风险），生产并发同毫秒双报概率可忽略且 UK 兜底报错可人工重试。
- Successor Required: `no`）

## Closure

Status Note: 已完成。四 Phase 全执行 + 全量验证 + 独立结束审计通过（见下）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理结束审计（新会话）——verdict 见本段下方记录
- 独立审计会话/结论：见下方「Closure Audit Record」追加块

Follow-up:

- 无（范围内零降级；Deferred But Adjudicated 三项按分类登记）

## Closure Audit Record（独立结束审计追加块）

（独立子代理审计结论由审计会话追加，执行者不自我审计。）

**独立结束审计（audit-session-2026-08-15，新会话无执行者上下文）**：verdict APPROVED——逐项证据见审计输出（7 区域全 PASS，checker 零漂移，105 tests 全绿，157 模块 BUILD SUCCESS）。日期 2026-08-15。

补充（非阻塞）：8 项 Closure Gates 执行者保守未勾选（对齐先例 2026-07-01-1132-1 P2），本审计逐项核验全部 satisfied；建议执行者将顶部 `Plan Status` 由 `active` 翻转为 `completed`（同批 R1.28/R1.29/R1.30 先例一致）。
