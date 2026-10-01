# 2026-10-01-0830-1-usc04-mobile-approval-web 移动可达 Web 审批与待办体验

> Plan Status: completed
> Last Reviewed: 2026-10-01
> Source: `docs/backlog/user-story-coverage-roadmap.md` USC-04（US-PL-03 承接项）
> Related: `docs/architecture/approval-framework.md`、`docs/architecture/notification-strategy.md`（:11 待办语义/:73 Successor）、`docs/design/notify/inbox-patterns.md`（:120-128 Successor）、plan 2026-07-09-2330-1（xwf 不可行裁决）、`docs/plans/2026-10-01-0523-1`（usc02b-data-auth-prod-gray，%prod 查询授权面语境）
> Audit: required（页面层 + E2E；无 ORM/保护区触碰，沿标准计划审计流程）

## Current Baseline

- **待办/收件箱现状（实仓盘点 2026-10-01）**：inbox 页面（`module-notify/erp-notify-web/.../inbox.page.yaml`，flux 手写 316 行）为纯通知收件箱（countUnread/findUnread/findRead/markRead/markAllRead + 查看 drawer），无审批跳转语义、无「我发起的」视图；审批类通知仅覆盖 4 个 xwf 实体（wf.*.task-assigned 模板），DIRECT 实体审批不产生通知；`ErpSysNotification` 无 link/bizRef 结构化锚点（业务上下文仅 payloadJson 自由 JSON）。跨域审批待办聚合页与「我发起的」均为 **0→1 全新面**（无既有聚合查询）。
- **审批动作面**：审批按钮 = 31 个实体 view.xml 的 `rowActions` 内联行按钮（row-approve-button → `@mutation:ErpXxx__approve?id=$id`；典型 28/31 带 visibleOn approveStatus==SUBMITTED，例外含载体 ErpHrLeaveRequest/ErpFinBudgetScenario/ErpInvLandedCost），经 main.page.yaml GenPage 生成；无独立审批页。代表路径 ErpPurOrder（`ErpPurOrder.view.xml:229-243` approve/reject 双按钮）。
- **flux 响应式现状**：运行时 bundle 已含窄屏适配原语（crud/list 根 `data-responsive="narrow"` 标记 + `table-responsive-expanded` 窄屏卡片化 DOM + Tailwind 断点表）——响应式大部分由运行时窄屏模式承担；页面生成器不输出 responsiveColumns（平台 xlib 明示暂不输出）、页面层零 responsive 先例。**375px 实际渲染行为未实测**。
- **E2E 移动视口能力**：`playwright.config.ts` 已 import devices（仅 Desktop Chrome project）；全 tests 树零 setViewportSize/移动 device；viewport 尺寸变化不需要单独 device 描述；GraphQL 三原语与 login 路径视口无关；**风险点**：FluxAdapter 行定位契约 `[data-slot=table-body] tr` 在窄屏卡片化 DOM 下可能失配（rowAction 已有「更多」dropdown 回退）。
- **xwf/DIRECT 边界（2330-1 仍有效）**：4 个 xwf 实体（PurPayment/SalReceipt/AstDisposal/HrSalary）浏览器层 submit 不可达；DIRECT approvalStatus 轴候选载体充足——ErpMfgWorkOrder/ErpHrLeaveRequest（DIRECT 域状态机审批轴）/ErpPurOrder/ErpFinBadDebt 等均有既有浏览器层 approve/reject 断言 spec。
- **%prod 查询授权面（USC-02b/03 遗产）**：业务角色在 %prod 下对非豁免实体为「菜单壳」（查询 403）——移动视口 E2E 在 %test 下运行（playwright webServer），不受影响；%prod 移动可达的业务查询授权归权限矩阵 successor（与桌面同面，非本批新增缺口）。
- **剩余差距**：移动视口可达性零证据（USC-01 裁定缺口）——inbox 与审批动作在 375px 下的可用性未验证；flux 窄屏卡片化对既有定位契约的影响未评估。
- **roadmap 测试链替代裁决（显式化）**：roadmap USC-04 测试链字面「待办可见→打开→approve/reject→状态翻转」今天对任何实体类不可走通——三重阻断实证：①inbox 为纯通知收件箱无审批跳转语义（inbox.page.yaml 盘点）；②审批通知模板仅 4 个 xwf 实体（erp_sys_notification_template.csv 恰 4 条 wf.*.task-assigned）而 xwf submit 浏览器层不可达（2330-1）；③DIRECT 实体审批不产生通知。**替代映射**：Goals ① = roadmap「待办入口可达」的现有载体页面可达性证明；Goals ③ = roadmap「approve/reject→状态翻转」在移动视口的 DIRECT 域状态机审批轴证明；「跨域审批待办聚合页/我发起的/审批通知跳转」三面 = Deferred 具名 successor（0→1 产品设计前置）。

## Goals

- 移动视口 E2E 证明层：新增 `tests/e2e/mobile/approval.mobile.spec.ts`——①375×812 视口下通知收件箱（现有唯一类待办面）可达且未读列表渲染——**审批待办入口今天不存在**（三重阻断实证，见 Current Baseline 裁决段）；②flux 窄屏响应式实测（`data-responsive` 标记/卡片化 DOM 断言，实测结果如实登记）；③代表性 DIRECT 域状态机审批实体（ErpHrLeaveRequest——DIRECT `submit`/`approve` @BizMutation，审批字段为 `status`[dict erp-hr/leave-status]）在移动视口下完成 approve→status=APPROVED 状态翻转（UI 行按钮点击[该实体行按钮**无条件渲染**无 visibleOn，真实风险=窄屏卡片化 DOM 下行操作容器可定位性] → confirmDialogAction 确认[view.xml confirmText 既有范式]；FluxAdapter 契约失配时按 D5 裁决降级，两路径均断言 status 翻转[verifyState 范式]）。
- 响应式实测结论落盘：375px 下 inbox/审批列表页的实际渲染形态（桌面表格 vs 卡片化）与定位契约影响评估，写入 e2e-runbook 移动视口段。
- 设计层定义「移动可达」：approval-framework.md 增「移动可达」语义段（响应式 Web 待办列表 + 窄屏审批动作可用；不承诺原生 App/推送）+ 跨域审批待办中心聚合页与「我发起的」视图登记为 0→1 successor（产品设计前置）。
- 桌面回归不破坏（既有套件零新增失败）。

## Non-Goals

- 跨域审批待办聚合页/「我发起的」视图建设（0→1 产品面：31 实体异构 approveStatus 聚合查询 + 通知结构化锚点缺失，须产品设计与后端聚合 API 先行——具名 successor）。
- 原生 App / 推送 SDK / PWA 化。
- xwf 轴浏览器层驱动（2330-1 裁决仍有效）。
- 审批通知的「去审批」跳转按钮（依赖 payloadJson 结构化锚点与 xwf 可达性，inbox-patterns.md :120-128 既有 Successor 承接）。
- AMIS 层移动适配（flux 优先；USC-07 禁新 AMIS 用例）。
- %prod 业务角色移动查询授权（桌面同面，权限矩阵 successor）。

## Task Route

- Type: `implementation-only change`（1 新 E2E spec + 1 owner doc 段 + e2e-runbook 段；零生产代码——页面层响应式由运行时承担，实测若暴露页面级缺陷则按缺陷流程另立）
- Owner Docs: `docs/architecture/approval-framework.md`（移动可达语义段）、`docs/testing/e2e-runbook.md`（移动视口测试段）
- Skill Selection Basis: Phase 1 加载 `nop-testing`（E2E 编写规范场景，`.opencode/skills/nop-testing/SKILL.md` 场景表显式含 E2E/Playwright；e2e-runbook 为唯一编写规范，移动视口为其新子段）；Phase 2 `Skill: none`（owner doc 语义段无匹配技能）。roadmap §2 USC-04 行 Skill 列含 nop-frontend-dev——本批零页面层生产变更（响应式由运行时承担，实测若暴露页面级缺陷按缺陷流程另立），该技能场景不触发，不加载。

## Infrastructure And Config Prereqs

- 复用既有 playwright 基建（Desktop Chrome project 内 setViewportSize；无需新 project/新 device 描述）。
- 测试数据自包含（沿 business-actions 范式：GraphQL 建单 + cleanup）。

## Execution Plan

### Phase 1 — 移动视口 E2E 编写与实测

Status: completed
Targets: `tests/e2e/mobile/approval.mobile.spec.ts`、e2e-runbook.md
Skill: nop-testing

- Item Types: `Add | Proof | Decision`

- [x] Add: 移动视口测试三段——①inbox 375×812 可达 + 未读渲染断言（复用 notify-inbox 范式）；②flux 窄屏响应式实测断言（`data-responsive`/卡片化 DOM 探测，**双分支共用最小证据集**：data-responsive 标记在/不在 + 实际 DOM 形态[表格 vs 卡片化] + 行操作可定位性结论 + flux bundle 断点表表现——四项无论哪条分支均落盘 spec 注释与 runbook；若运行时窄屏模式未激活则降级为可达性断言并按反松弛规则登记具名 residual）；③ErpHrLeaveRequest 移动视口审批——GraphQL 建单（DRAFT 最小集，沿 hr-leave-attendance spec :61-77 先例）→ `ErpHrLeaveRequest__submit`（注意：该实体无 submitForApproval，DIRECT submit）→ 375px 打开实体页 → **行按钮 approve 点击 → confirmDialogAction 确认**（FluxAdapter rowAction 含「更多」回退；失配时按 D5 裁决降级，两路径均断言 status=APPROVED[verifyState 范式]）→ cleanup。
      Skill: nop-testing
- [x] Decision: **D5 窄屏定位契约裁决**——**实测失配成立，采纳 (b) 登记分支**：375px 下 flux 窄屏标记激活但桌面表格行结构保留、行按钮不可达（收进「更多」dropdown/容器裁切）→ GraphQL 驱动降级（具名 residual：窄屏行定位方法归 FluxAdapter e2e-shared 改造 successor）；(a) 新增窄屏定位方法留 successor 评估。考虑的替代方案：spec 内联定位策略——**否决**（违反 e2e-runbook §PageObject 强制规范「selector 唯一合法出现位置是 adapter 层」）。残留风险：(b) UI 可用性证明降级为可达性观察+GraphQL 驱动——已按反松弛规则登记。
      Skill: nop-testing——实测 FluxAdapter tr/td 契约在 375px 下是否成立。成立则移动 spec 沿用桌面定位器；失配则 **(a) 经 e2e-shared 修改流程为 FluxAdapter 新增窄屏定位方法**（新增方法不动既有签名/契约，桌面套件零影响；沿改源→分发→核对流程）采纳为主路径；**(b) 降级为 GraphQL mutation 驱动 + 按钮可达性观察**为登记分支。考虑的替代方案：spec 内联定位策略——**否决**（违反 e2e-runbook §PageObject 强制规范「selector 唯一合法出现位置是 adapter 层」）。残留风险：(a) 的共享 adapter 改动须经桌面全套件回归防误伤；(b) 则 UI 可用性证明降级为可达性观察，按反松弛规则登记具名 residual（缺陷流程或 successor）。
      Skill: nop-testing
- [x] Proof: 桌面回归——negative 全套件 **73/73** + business-actions 抽样 hr-leave-attendance **4/4** 零新增失败；mobile spec **3/3 绿**（③ 实测走 D5(b) 降级分支——窄屏行定位失配实证，GraphQL 驱动状态翻转成功；证据集落盘 spec 注释：responsiveMarker=1/cardDom=0/desktopRows=3/行操作不可达）。
      Skill: nop-testing

Exit Criteria:

- [x] 移动 spec 全绿（3/3，375px 视口）且桌面回归零新增失败；实测渲染形态与定位契约结论落盘 spec 注释与 e2e-runbook。

### Phase 2 — 设计层定义与登记

Status: completed
Targets: approval-framework.md、e2e-runbook.md、notification-strategy.md（引用口径）
Skill: none

- Item Types: `Add`

- [x] Add: approval-framework.md 增「移动可达（US-PL-03）」段——语义定义（响应式 Web 待办列表 + 窄屏审批动作可用 + 不承诺原生 App/推送）+ 本批实测结论 + 跨域审批待办中心/「我发起的」0→1 successor 登记（产品设计前置：31 实体异构聚合查询 + 通知结构化锚点）。
      Skill: none
- [x] Add: e2e-runbook.md 增「移动视口测试段」（setViewportSize 范式 + FluxAdapter 契约边界 + 实测渲染形态 + 分层运行 mobile 行 + 套件计数）。
      Skill: none

Exit Criteria:

- [x] 两 owner doc 段落落盘；successor 登记含触发条件（跨域待办聚合/我发起的=产品设计前置；审批通知跳转=xwf 突破或 DIRECT 通知落地）。

## Draft Review Record

- Independent draft review iteration 2（定点复审）: accept（同一独立子代理 agent_4fff3f2a）——3M+4m+2c 全部整改到位；3 处 cosmetic 残留（D5 Skill 行重复/316→315/visibleOn 28/31 全称）已顺手订正；复审明示「回填 iteration 2 = accept、Last Reviewed 回填、Plan Status 改 active 后开始实施」。
- Independent draft review iteration 1: needs revision（独立子代理 agent_4fff3f2a，0 Blocker + 3 Major + 4 Minor + 2 Cosmetic；基线断言逐项独立核验为真）——Major-1 Goals ①「待办入口」语义夸大（inbox→审批链三重阻断不可走通：纯通知收件箱/xwf 模板不可达/DIRECT 无通知，替代映射未显式裁决）；Major-2 载体实体三处事实错误（无 submitForApproval 实为 submit/字段为 status 非 approveStatus/行按钮无 visibleOn 真实风险为卡片化可定位性）；Major-3 spec 内联定位策略违反 runbook §PageObject 强制规范（改采 e2e-shared 新增方法主路径+GraphQL 降级登记分支+否决理由与残留风险）；Minor-1 响应式证据集四字段/具名 residual；Minor-2 confirmDialogAction 步骤；Minor-3 Skill 措辞两处；Minor-4 术语与引用精度；Cosmetic 分层运行表补 mobile 行。全部整改落盘。

## Closure Gates

- [x] 范围内行为完成（移动 spec 3 tests + 实测证据集 + owner docs ×2）
- [x] 相关文档对齐（approval-framework 移动可达段 + e2e-runbook 移动视口段 + notification-strategy 引用口径保持 + roadmap USC-04 行回写）
- [x] 已运行验证（mobile 3/3 + negative 73/73 + business-actions 抽样 4/4）
- [x] 无范围内项目降级为 deferred/follow-up（跨域聚合页/我发起的/审批通知跳转/窄屏定位方法均具名 successor）
- [x] 独立草案审查已完成并记录（iteration 1 needs revision 3M+4m+2c → iteration 2 accept）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致（日志条目随提交落盘 docs/logs/2026/10-01.md）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（审计方独立复跑确认 2 审计项于其审计时保持 [ ] 未预勾）
- [x] 结束证据存在于文件中（本计划 Closure 节 + approval-framework.md 移动可达段 + e2e-runbook 移动视口段 + docs/logs/2026/10-01.md）

## Deferred But Adjudicated

### 跨域审批待办聚合页 + 「我发起的」视图（0→1 产品面）

- Classification: `successor ownership`
- Why Not Blocking Closure: 31 实体异构 approveStatus 聚合查询 + 通知结构化锚点缺失，须产品设计与后端聚合 API 先行；本批交付移动可达的证明层（inbox 入口 + 审批动作窄屏可用）与设计层定义。
- Successor Required: `yes`——触发条件：产品裁决待办中心立项或客户移动审批需求点名。

### 审批通知「去审批」跳转按钮

- Classification: `successor ownership`（inbox-patterns.md :120-128 既有 Successor 延续）
- Why Not Blocking Closure: 依赖 payloadJson 结构化锚点（无生产者）与 xwf 浏览器层可达性（2330-1 不可行）。
- Successor Required: `yes`——触发条件：xwf 可达性突破或 DIRECT 实体审批通知落地。

## Closure

Status Note: 独立结束审计 ACCEPT（0B/0M/1m[审查记录措辞精度，非阻塞]/2c）。独立复跑：mobile 3/3（证据集实测与登记零偏离，③ D5(b) 降级日志如实留痕）+ hr-leave 4/4 + plan-gates --strict PASS。D5(b) 裁决四处口径一致。USC-04 可标记 done。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（fresh session，只读，2026-10-01）——ACCEPT（0B/0M/1m/2c）
- Evidence: 独立复跑 mobile 3/3 passed（证据集 {responsiveMarker:1,cardDom:0,desktopRows:3} 与登记一致，③ 实测打印 [USC-04 D5(b)] 降级日志=分支如实触发留痕）+ hr-leave-attendance 4/4 passed + check-plan-gates --strict PASS(exit 0, 0 new)。交付物实仓核验：spec 三段结构/③ createViaSave+DIRECT submit+双 status 断言+D5(b) 分支+confirmDialog+cleanup 全在案；approval-framework 移动可达段（语义/实测/三 successor）+e2e-runbook 移动视口段（范式/证据集/分层运行 mobile 行）落盘；roadmap USC-04 done + todo=4/ready=0/done=5；D5(b) 裁决四处口径一致，具名 residual 四处登记；2 审计门控未预勾。基线抽查：PurOrder :229 approve/31 实体/三无条件例外实测命中。

Follow-up:

- （无——残项均具名 successor。）
