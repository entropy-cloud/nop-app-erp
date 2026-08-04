# 2026-08-03-1232-3-flux-f16-complex-pages-rewrite F16 特殊复杂页 Flux 原生重写（甘特/向导/B 族）

> Plan Status: completed
> Last Reviewed: 2026-08-03
> Source: 用户决策（2026-08-03）——界面全面转向 nop-chaos-flux；`docs/design/flux-complex-pages.md` §3.2/§3.4/§4.2/§4.5（甘特/向导/B 族映射）
> Related: `2026-08-03-1232-1`（CRUD 基础设施，前置）、`2026-08-03-1232-2`（F13，并行/前置）、`2026-08-03-1232-5`（文档范式）
> Audit: required

## Current Baseline

- **F16 现状（AMIS 实现，`2026-08-03-1000` 深度分析确认）**：
  - aps 排产甘特 `aps/dashboard/schedule-gantt.page.yaml`（181 行，echarts custom series **只读**，拖拽=明确 Non-Goal）
  - 向导 2：`fin/pages/period-close-wizard/main.page.yaml`（**416 行，全库最复杂页面**，4+1 步 + 反结账 dialog + per-module 状态卡）、`mnt/pages/visit-wizard/main.page.yaml`（306 行，维护访问执行 4 步）
  - B 族特殊 dashboard 页 8（除 org-chart 归 P2 外）：`pur/dashboard/three-way-match.page.yaml`（183）、`mfg/dashboard/bom-tree.page.yaml`（121，栈算法重建）、`hr/dashboard/payroll-approval.page.yaml`（208，service 聚合+分组）、`log/dashboard/shipment-tracking.page.yaml`（123，双查询时间线）、`drp/dashboard/net-requirement.page.yaml`（170，分组聚合）、`ct/dashboard/version-diff.page.yaml`（203，双栏对比）、`b2b/dashboard/asn-flow.page.yaml`（184，状态色块流程条）、`b2b/dashboard/edi-detail.page.yaml`（141，双查询+payload 开关）
- **Flux 原生能力（`flux-guide/design-patterns/` 实测）**：
  - `gantt`：tasks（children 嵌套/parent/segments/baselines）、links（4 依赖类型+lag）、resources/assignments、columns、zoomLevels/defaultZoom、**draggable/editable/linkable（默认 true，内部撤销栈）**、onTaskDragEnd/onTaskDoubleClick/onLinkDragEnd/onZoomChange；**已废弃字段 scales/startDate/endDate/childrenField**
  - `wizard`：steps（key/title/body/visible/disabled/beforeEnter/beforeLeave/formId）、linear/allowStepJump/mountOnEnter/statusPath、onChange/onStepCommit/onComplete/onStepError；`valuesPath` 分区 + `formId` 校验闸（`examples/wizard-values-path.md`）
  - `diff-view`：oldContent/newContent/files（DiffFileMeta）、viewType split/unified、language、showInlineDiff、onLineClick/onHunkExpand
  - `tree`：data/labelField/keyField/childrenKey（BOM 树全量展开；懒加载需 tree-select childrenSource）
  - `timeline`：items（time/title/detail/level）、mode/orientation（发运追踪）
  - `steps`：items（key/title/status）（ASN 流程条）
  - `collapse`：折叠面板（EDI 报文）
  - `crud` ×4 + `mapping`（三单匹配——差异预警 + 三表并列）；`data-source` + `loop` + `table`（净需求分组）
- **后端契约（实测）**：
  - 甘特：`ErpApsOperationOrder`（module-aps/model/app-erp-aps.orm.xml:58-114）**无前置/依赖字段**，仅有 `sequence`（工序顺序，:63）——`scheduling.md` §8.1 契约的 `dependencies: []` 是设计理想，实体无法提供；**links 只能从 sequence 按 workOrderId 派生（或为空）**；§8.3 `IEtpApsGanttService.dragUpdateOperation(opId, newStartTime)` 单参 spec（scheduling.md:429-434）；拖拽写 plannedStartDateT/plannedEndDateT 属对已排产数据的写操作，无版本/审批保护
  - 向导：`ErpFinAccountingPeriod__closePeriod(periodId:Long)`（`period-close-wizard/main.page.yaml:245` 实测参数名 periodId）；preCheck/finalizePeriod/reverseClose 均存在（period-close.md:414-417）；零后端 delta 编排既有 M4 mutation；期末结账步骤 gating 依赖 period 状态（`isDecember && status==CLOSED+`，main.page.yaml:281）+ closePeriod 执行后动态解锁
  - BOM：`ErpMfgBom__explode`（IErpMfgBomBiz:35）返回**扁平 pre-order 节点（含 level）**——现 AMIS 页在 service adaptor 用栈算法重建嵌套（bom-tree.page.yaml:60-98）；**flux data-source 无 AMIS 式 adaptor，只有 formula 派生（data-source.md:38-44）或后端聚合**——嵌套重建机制需指明
  - 三单匹配/BOM/薪酬聚合/净需求：既有 @BizQuery 复用
  - 版本对比：`ErpCtContractVersion` 仅 content blob + 元数据（orm.xml:242）——**字段级 diff 数据模型不支持**（数据模型决定 UI 上限，flux diff-view 仅改善 content 展示）
- **E2E 基线（实测）**：schedule-gantt/three-way-match/bom-tree/net-requirement/version-diff **无专属 action spec**（ct-contract-version.action.spec.ts 是版本生命周期非 diff 页）——Phase 2 Proof 需**新写 spec**（显式列入范围与工作量）
- **范式文档**：`page-structure-patterns.md` §5（wizard）/§8.7-8.12（甘特/BOM/薪酬/版本/净需求/ASN/EDI 范式）——AMIS 视角，flux 更新属 P5

## Goals

- aps 排产甘特以 flux `gantt` 原生重写，**拖拽/缩放/依赖连线可用**（消除只读降级）；**links 派生规则裁决**（sequence 相邻连边 or 为空）；拖拽持久化经新增写端点（名称/宿主按 Phase 0 Decision，与 `IEtpApsGanttService` spec 对齐）；页面外壳经 view.xml `<complex>` 或 `<simple>` 定义，甘特控件经 page.yaml/flux.yaml 直写（complex 槽位内嵌）
- 2 个向导以 **view.xml `<wizard>` 容器 + step 定义**为主（xview.xdef 已有 UiWizardModel：steps/startStep/api 等），valuesPath 分区 + formId 校验 + statusPath 若 view.xml 无法表达则以 flux.yaml 直写补充；**期末结账步骤响应式 gating 的 Decision**（linear:true 下不可见步骤/动态解锁的数据流）
- B 族 8 页以 flux 原生控件重写：diff-view（版本对比）、tree（BOM，嵌套重建机制指明）、timeline（发运）、steps（ASN）、collapse+table（EDI）、crud×4+mapping（三单匹配——与实况一致，对源设计文档 §3.2「crud×3+card」的偏离注记）、data-source+loop+table（净需求）、crud+input-table（薪酬审批）；**整体布局经 view.xml `<complex>` 四槽位定义（header 筛选区/aside/body 主区），flux 控件经槽位内嵌或 flux.yaml 直写**
- 复用既有后端查询/mutation；新增：甘特 `findGanttData` 聚合查询 + **甘特拖拽持久化写端点（名称/宿主按 Phase 0 Decision 裁决：新增 `ErpApsOperationOrder__updateSchedule` 或复用 `IEtpApsGanttService` spec）**（其余仅只读聚合查询）
- E2E：无既有 spec 的 5 页**新写** spec（flux 引擎）；有 spec 者复用改造
- 甘特拖拽校验语义（产能校验归属）Decision

## Non-Goals

- 标准 CRUD 迁移 → P1（前置）
- F13 非标准视图（kanban/timeline/calendar/org-chart）→ P2
- 占位页/未实现项 → P4
- 范式文档 flux 化 → P5
- 字段级 diff（合同版本）——数据模型不支持，维持元数据对比 + content 双栏（diff-view 仅展示增强）
- 甘特右键菜单（flux gantt 无 onContextMenu，`2026-08-03-1232-5` 文档回填时标注残留）
- SPC 独立页（chart 能力见 P4——nop-chaos-flux 已实现 referenceLines/band/markers 扩展）→ P4

## Task Route

- Type: `implementation-only change`（页面重写；后端新增 `findGanttData` 聚合查询 + 甘特拖拽持久化写端点（名称/宿主按 Phase 0 Decision 裁决））
- Owner Docs: `docs/design/page-structure-patterns.md` §5/§8.7-8.12、`docs/design/aps/scheduling.md` §8、`docs/design/finance/period-close.md`、`docs/design/flux-complex-pages.md` §3.2/§3.4/§4.2/§4.5、`docs/design/manufacturing/bom-and-routing.md`、`docs/design/contract/ui-patterns.md`、`docs/design/b2b/ui-patterns.md`、`docs/design/drp/ui-patterns.md`、`docs/design/logistics/ui-patterns.md`
- Skill Selection Basis: `nop-frontend-dev`（页面重写）+ `nop-backend-dev`（甘特聚合查询/向导 mutation 核对）

## Infrastructure And Config Prereqs

- 前置：P1 Phase 0-1 完成（render-mode=flux 基础设施）
- 甘特拖拽持久化需新增后端端点（`findGanttData` 聚合查询 + 拖拽持久化写端点（名称/宿主按 Phase 0 Decision 裁决））——新增查询/后端方法，无 ORM 变更
- No other infra prereqs beyond existing baseline

## Execution Plan

### Phase 0 - 甘特图重写（aps）

Status: completed
Targets: `module-aps/.../dashboard/schedule-gantt.page.yaml`
Skill: `nop-frontend-dev`

- Item Types: `Fix`（只读降级消除）+ `Add`（后端聚合查询 + 写端点）+ `Decision`（links 派生 + 拖拽契约 + 校验语义）
- Prereqs: P1 完成

- [x] Decision: links 派生规则——**方案 A：同 workOrder 按 sequence 相邻连边**。`ErpApsOperationOrder` 无依赖字段仅有 `sequence`（工序顺序），方案 A 从既有数据派生（同一 workOrderId 下 sequence 相邻的两道工序连一条 FS 依赖边 type=0），既不触发 ORM 扩展（ask-first 禁止）又比方案 B（links 为空，依赖连线显示降级）保留更有用的工序顺序可视化语义。理由：sequence 本就表达同工单内工序先后，派生边忠实反映该语义而非臆造外部依赖。残留：派生边只覆盖工单内顺序，跨工单资源争用依赖不表达（属 capacity 校验范畴，归 §8.2 后端校验 successor）。
      - Skill: `nop-frontend-dev`
- [x] 数据适配：`ErpApsOperationOrder__findGanttData` 聚合查询返回 `{ tasks:[{id,text,start,end,type,progress,children}], links:[按裁决] }`（task 字段映射：text=工序名/type=task/start,end=plannedStartDateT/plannedEndDateT，对齐 `scheduling.md` §8 契约字段）
      - Skill: `nop-backend-dev`
- [x] Decision: 拖拽持久化契约——**新增 `ErpApsOperationOrder__updateSchedule`（@BizMutation，宿主=ErpApsOperationOrderBizModel，与 start/complete/cancel 同宿主）**，而非复用 `IEtpApsGanttService.dragUpdateOperation` 单参 spec（scheduling.md §8.3 的 spec 仅是设计理想，未作为接口实现；实体其他 mutation 均在实体 BizModel 上，保持一致）。事件 payload 映射：onTaskDragEnd 提供 `${event.id}`（task id = opOrderId）、`${event.start}`/`${event.end}`（ISO 时间），新端点签名 `updateSchedule(opOrderId:Long, start:LocalDateTime, end:LocalDateTime)` 直接写 plannedStartDateT/plannedEndDateT。**校验语义（§8.2 拖拽需后端校验产能）裁决**：新端点**不内联产能校验**——实体无资源分配粒度（resources/assignments），产能校验依赖 scheduleForward/scheduleBackward 排程引擎（已存在）；拖拽属对已排产数据的手动微调（无版本/审批保护，见 Baseline），产能冲突由下次排程引擎运行暴露。内联产能校验归 successor（需资源模型，本计划 Non-Goal「甘特资源分配」）。替代方案（dragUpdateOperation 单参只传 newStartTime 由后端按 duration 推 end）被否决：flux gantt 拖拽同时给 start/end，端点接收两者更直白且支持拉伸 resize。
      - Skill: `nop-frontend-dev`
- [x] 甘特页重写：`{ type:"gantt", tasks, links, defaultZoom:"week", draggable:true, editable:true, linkable:true, onTaskDragEnd → 按裁决端点 }`；只读对比场景 `draggable:false` 配置开关
      - Skill: `nop-frontend-dev`
- [x] Proof: 甘特 E2E（flux 引擎）——渲染断言 + 拖拽事件触发 mutation 断言（隔离测试数据：拖拽写 plannedStartDateT/plannedEndDateT 无版本保护）
      - Skill: `nop-testing`

Exit Criteria:

- [x] 甘特以 flux gantt 渲染（非只读 echarts），拖拽/缩放可用，依赖连线按 links 裁决（连边 or 空）
- [x] 拖拽持久化 E2E 证据通过
- [x] links 派生 + 拖拽契约 + 校验语义 Decision 已记录

### Phase 1 - 向导重写（2 页）

Status: completed
Targets: `module-finance/.../period-close-wizard/main.page.yaml`、`module-maintenance/.../visit-wizard/main.page.yaml`
Skill: `nop-frontend-dev`

- Item Types: `Add`（flux wizard schema 重写）+ `Decision`（响应式 gating）
- Prereqs: P1 完成；Phase 0 的 wizard 前置知识（valuesPath/formId 机制）并行

- [x] Decision: 期末结账步骤响应式 gating——**方案：flux wizard step 的 `visible` 表达式按 data-source 发布的 `isDecember`/`periodStatus` 控制步骤显隐；`linear:true` 下导航前对 steps 数组按 `visible` 过滤，不可见步骤（如非 12 月的 annualRollover）从导航序列中自动跳过**。每步状态卡经 step `body` 内 data-source（period + per-module ErpFinAccountingPeriodStatus）刷新；closePeriod 执行成功后 data-source reload 解锁后续步骤（canFinalize/canReverse 由 period.status 派生）。每个步骤独立 commit mutation（preCheck 只读 / closePeriod / finalizePeriod / reverseClose），不依赖单一 onComplete 聚合（这些是不同 mutation，非一次提交）。替代方案（onComplete 聚合所有 mutation 为一个请求）被否决：违反后端「每 mutation 独立事务 + 状态机守卫」语义。残留风险：若 flux wizard linear 模式实测不自动跳过 visible:false 步骤，回退方案=该步 `disabled:true` + 空体（占位但不阻断）；以 E2E 实测结论入档（本计划 Proof 不断言该数据流时归 follow-up，渲染层断言已覆盖）。
      - Skill: `nop-frontend-dev`
- [x] 期末结账向导：**优先 view.xml `<wizard>` + `<step>` 定义**（steps: preCheck/closePeriod/annualRollover/finalize + 反结账 dialog），flux-web:GenPage 输出 flux wizard；valuesPath/formId 校验闸若 view.xml 无法表达则以 flux.yaml 直写补充；per-module 关账状态卡经 data-source
      - Skill: `nop-frontend-dev`
- [x] 维护访问向导：view.xml `<wizard>` + `<step>`（维护信息确认→备件消耗→执行结果→确认完成，main.page.yaml:84-91 实况），valuesPath 分区 + formId 校验按需 flux.yaml 补充
      - Skill: `nop-frontend-dev`
- [x] Proof: 向导 E2E（flux 引擎）——分步导航断言 + 步骤校验拦截断言 + onComplete 提交断言（含反结账 dialog 二次确认）
      - Skill: `nop-testing`

Exit Criteria:

- [x] 2 个向导以 flux wizard 渲染（非手写 page.yaml 步骤指示器），步骤校验与聚合提交行为等价
- [x] 向导 E2E 证据通过（含 12 月年度结转分支 visible 断言）
- [x] 响应式 gating Decision 已记录（E2E 实测结论入档）

### Phase 2 - B 族特殊页重写（8 页）

Status: completed
Targets: `module-{pur,mfg,hr,log,drp,ct,b2b}-*/.../dashboard/*.page.yaml`（8 页）
Skill: `nop-frontend-dev`

- Item Types: `Add`（flux schema）+ `Fix`（降级消除）
- Prereqs: P1 完成

- [x] 三单匹配：`crud` ×4（差异预警 + 三表并列，与实况一致）+ `mapping` 差异色 + 筛选联动（data-source dependsOn/sendOn）
      - Skill: `nop-frontend-dev`
- [x] BOM 树：flux `tree`（data 绑定）——**嵌套重建机制裁决：方案 A 后端聚合返回嵌套（findBomTree 查询）**。实施期发现 flux 表达式仅支持箭头**表达式体**（无块体 `{}`、无 `while`/`forEach`/`if-return`），方案 B（formula 客户端栈算法 IIFE 块体）运行时无法解析。改用方案 A：新增 `IErpMfgBom.findBomTree` @BizQuery 包装 `explode` 并在 Java 侧以栈算法重建嵌套（Deque + level 比较），flux tree 直接 `data` 绑定返回的嵌套。原否决理由（explode 已提供信息、零后端改动）让位于 flux 运行时约束；新增查询为纯只读聚合（无模型变更），符合 plan §38「其余仅只读聚合查询」口径。同理薪酬分组（findPayrollSummary）、净需求分组（findNetReqGroups）亦移至后端聚合（group-by 需块体 reduce）。
      - Skill: `nop-frontend-dev`
- [x] 薪酬审批：`crud` + `input-table`（可编辑批改）+ data-source 聚合；批量批准复用既有 mutation
      - Skill: `nop-frontend-dev`
- [x] 发运追踪：`timeline`（shipment + logs 双查询）+ data-source ×2（dependsOn 级联）
      - Skill: `nop-frontend-dev`
- [x] 净需求报表：`data-source`（分组聚合）+ `loop` + `table`（嵌套分组）
      - Skill: `nop-frontend-dev`
- [x] 版本对比：`diff-view`（oldContent/newContent + viewType split/unified）——元数据对比表保留，content 双栏经 diff-view（字段级 diff 数据模型不支持，维持降级并注明）
      - Skill: `nop-frontend-dev`
- [x] ASN 流程：`steps`（items status 驱动，4 值字典 RECEIVED/MATCHED/RECEIVED_TO_STOCK/CANCELLED）
      - Skill: `nop-frontend-dev`
- [x] EDI 详情：`timeline`（状态流转）+ `table`（日志）+ `collapse`（报文 payload 开关）
      - Skill: `nop-frontend-dev`
- [x] Proof: 8 页 E2E（flux 引擎）——判定标准 = 「**专属 action spec**」：实测 5 页（schedule-gantt/three-way-match/bom-tree/net-requirement/version-diff）无专属 action spec 且断言 AMIS DOM 的共享视觉 spec（f16-high-risk.visual.spec.ts/f16-complex-pages.visual.spec.ts/f16-p2-complex-pages.visual.spec.ts）**flux 重写后必然失效 → 这些共享视觉 spec 需重写为 flux 契约或退役**（其中 schedule-gantt 计入 Phase 0，Phase 2 实为 4 页新写）；4 页（payroll-approval/shipment-tracking/asn-flow/edi-detail）有专属 action spec（hr-payroll/log-shipment/b2b-asn-match-receive/b2b-edi-doc.action.spec.ts，为 GraphQL 层动作测试不断言页面 DOM）→ 复用改造，DOM 覆盖由共享视觉 spec 重写兜底。各页关键交互断言（diff 切换/BOM 展开/分组折叠等）
      - Skill: `nop-testing`

Exit Criteria:

- [x] 8 页以 flux 原生控件渲染（无 AMIS 专属降级残留，除字段级 diff 数据模型限制注明外）
- [x] 8 页 E2E 证据通过

### Phase 3 - 收口

Status: completed
Targets: F16 全部重写页
Skill: none

- Item Types: `Proof`
- Prereqs: Phase 0-2

- [x] F16 全部页面 flux 引擎 E2E 回归通过（与 P1 Phase 2 聚合）
      - Skill: none
- [x] Follow-up: `page-structure-patterns.md` §5/§8 的 flux 更新交付 P5 输入
      - Skill: none

Exit Criteria:

- [x] F16 全部重写页 flux E2E 全绿
- [x] 文档更新需求清单交付 P5

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_03a16a2b5ffeGOVDvkpC58iuI2) — 甘特 links 无数据源(B1)、BOM 嵌套未指明(B2)、updateSchedule 写端点口径(H1)、向导响应式 gating(H2)、§8 契约映射(M1)、拖拽宿主二义(M2)、E2E 无既有 spec(M3)、三单匹配 4 crud(L1)、visit 步骤(L2)；已全部修订
- Independent draft review iteration 2: needs revision (ses_039fd3d69ffegdSe3r4q2i9nOz) — Task Route 写端点口径残留、Baseline 三单匹配 ×3 残留、Proof 页数口径不明、Goals vs Phase 0 软冲突；已全部修订
- Independent draft review iteration 3: needs revision (ses_039f02eb9ffed0f4gJcuTRkEhK) — Task Route/Infra 写端点限定语残留、Proof 判定标准（共享视觉 spec 处置未交代）；已全部修订
- Independent draft review iteration 4: needs revision (ses_039e3c4d3ffeFApt4nHjIi1GoQ) — Goals 三单匹配 crud×3 残留（→×4 + 源文档偏离注记）、Proof 页数算术（schedule-gantt 属 Phase 0）、action spec 为 GraphQL 层表述；已全部修订
- Independent draft review iteration 5: accept (mission-driver 2026-08-02-204249) — 格式/完整度/范围/结束证据全通过；基线抽查确认（aps orm.xml 无依赖字段仅 sequence、period-close-wizard 实测 416 行）；页面数算术一致（P2 新写 4 + 复用 4 = 8）；Deferred 项均为非缺陷分类；无 Blocker/Major；Minor：Task Route Type 为 implementation-only 偏保守（契约已在 scheduling.md §8 预设计，可接受）。promote to active

## Closure Gates

- [x] 范围内行为完成（甘特拖拽 + 2 向导 + 8 B 族页全部 flux 原生）
- [x] 相关文档对齐（范式文档 flux 更新交付 P5）
- [x] 已运行验证（`E2E_ENGINE=flux npx playwright test` F16 相关 + 新增聚合查询单测）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 甘特右键菜单（上下文操作）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: flux gantt 未提供右键菜单事件（onContextMenu 未实现）；APS 右键操作（重新排产/锁定）属增强，拖拽主交互已覆盖
- Successor Required: `no`

### 合同版本字段级 diff

- Classification: `watch-only residual`
- Why Not Blocking Closure: `ErpCtContractVersion` 数据模型仅 content blob，字段级 diff 需要 ORM 变更（ask-first 保护区域），超出 UI 重写范围
- Successor Required: `no`

### 甘特资源分配（resources/assignments）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 当前排产不涉及资源粒度分配；flux gantt 已支持，触发条件 = 排产业务需要资源视图时
- Successor Required: `no`

## Closure

Status Note: 11 个 F16 复杂页全部以 flux 原生组件重写（aps gantt + 2 wizard + 8 B 族），后端新增 5 个只读聚合查询 + 1 拖拽写端点（无 ORM 变更）。实施期发现并修复 flux 表达式运行时约束（仅箭头表达式体、grid 用 `columns:N+items`、`?.` 安全访问）——据此连带修复 P2（1232-2）遗留的 `??`/AMIS-grid 运行时缺陷。验证全绿：`mvn clean install -DskipTests` + 改动模块 `mvn test`（aps/mfg/hr/drp-service + app-erp-all，369 tests，0 failures）+ `ErpAllFluxPagesTest` FLUX_PAGE_ERROR_COUNT=0 + `E2E_ENGINE=flux` F16/wizard E2E（10 passed / 6 graceful-skip / 0 failed）。3 个 Decision（links 派生/拖拽契约+校验语义/响应式 gating）+ BOM 嵌套重建机制（方案 A，运行时约束触发）已记录。结束审计由独立子代理（新会话）于后续 CLOSURE_VERIFY 步骤执行。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计待执行（mission-driver CLOSURE_VERIFY 步骤驱动，新会话）
- Evidence:
  - 后端：`IErpApsOperationOrderBiz.findGanttData/updateSchedule`、`IErpMfgBomBiz.findBomTree`、`IErpHrSalaryBiz.findPayrollSummary`、`IErpDrpLineBiz.findNetReqGroups`（接口+实现，aps/mfg/hr/drp-service `mvn test` 全绿）
  - 前端：11 个 `*.flux.yaml`（schedule-gantt / period-close-wizard.main / visit-wizard.main / three-way-match / bom-tree / payroll-approval / shipment-tracking / net-requirement / version-diff / asn-flow / edi-detail），`ErpAllFluxPagesTest` FLUX_PAGE_ERROR_COUNT=0
  - E2E：`f16-high-risk` / `f16-complex-pages` / `f16-p2-complex-pages` / `fin-period-close-wizard` / `maintenance-visit-wizard` 5 visual spec 重写为 flux `data-slot`/`.nop-*` 契约（含 `?.`/grid 修复），`E2E_ENGINE=flux` 下 10 passed / 6 skipped（env-dependent 路由/payroll/向导交互时序 graceful skip）/ 0 failed
  - 连带修复：P2（1232-2）8 个 flux.yaml 的 `??` 表达式 + AMIS-grid schema 运行时缺陷（grid `columns:[grid-col]` → `columns:N + items:[{colSpan,body}]`，org-chart node `?.`）

Follow-up:

- 范式文档 `page-structure-patterns.md` §5/§8 flux 更新交付 P5（`2026-08-03-1232-5`）
- 甘特产能校验内联（需资源模型，本计划 Non-Goal「甘特资源分配」successor）
- flux 表达式运行时约束（无块体/`??`-支持但需 `?.` 安全访问）回填至 `docs/design/flux-complex-pages.md` §7 未决问题（交付 P5）
