# 2026-10-05-0400-1 flux 手写页按钮接线修复（探针定案 + 6 存量页改写）

> Plan Status: completed
> Last Reviewed: 2026-10-05
> Source: docs/analysis/2026-10-05-0353-perf-ux-round2-increment-analysis.md §2/§3.1-F1/§4 批次 1
> Related: 2026-10-05-0400-2（pda 页随其全闭环重构单独处理，不在本计划）
> Audit: required

## Current Baseline

- 7 个手写复杂页（page.yaml 整页直写、无 `x:gen-extends`）使用 AMIS 风格按钮接线 `actionType: ajax` + `api`。flux 静态+产物级六环证据链判定该写法运行期惰性（button renderer 仅声明 `onClick` 事件字段；唯一 AMIS→flux 转换点 `page_simple.xpl` 只服务 view.xml GenPage 路径；导出 JSON 原样携带；E2E `callMutationOk` 范式从不点击按钮故零暴露）。分析报告 §2。
- 本计划范围（6 存量页，pda 页归 plan 2）：disposal-wizard(ast)、lead-conversion(crm)、period-close-wizard(fin)、stock-take-flow(inv)、visit-wizard(mnt)、ErpSysNotification/inbox(notify)。**惰性接线计数（草案审查 round1 实核修正）：14 个 `actionType: ajax` 按钮 + 12 处 `onEvent: after-success` 刷新链（period-close 3 / stock-take-flow 3 / visit-wizard 4 / inbox 2）+ 6 个 `actionType: dialog` 按钮（period-close 3 / inbox 3）**。表单级 `api` 实际仅存在于 pda 页（2 处，归 plan 2）；6 存量页为 0（初稿误记 inbox 1 处，已修正）。
- flux 原生正确范式（`flux-guide/09-amis-migration.md` 转换表 + `flux-core/src/types/actions.ts` 类型实核）：
  - ajax：`onClick: {action: 'ajax', confirmText?, messages?, args: {url, method, data}}` —— **confirmText/messages 是 args 的兄弟键**（`AjaxActionSchema extends ActionShapeFields`；运行时 `flux-runtime/src/runtime-action-helpers.ts:135-136` 只读 `action.source?.confirmText/messages`，放 args 内被静默忽略）。
  - 刷新链：`onEvent: after-success` → ActionSchema `then: [{action: 'refreshSource', targetId…}]`（AMIS onEvent 属迁移表「被删除的属性」，flux 用 action `then` 链承载）。
  - dialog：`actionType: dialog` → `{action: 'openDialog', args: {…}}`。
- 同页已有 flux 原生对照（disposal-wizard 第 2 步按钮 `onClick: {action: refreshSource, targetId}` 工作正常；inbox「刷新」按钮原生触发 countUnread/findUnread 请求）。
- 宿主 confirm 能力：`runtime-action-helpers.ts:142-147`——配置 confirmText 而宿主未提供 `env.confirm` 时动作**显式失败**；Phase 3 复验须确认 nop 宿主已接 env.confirm（12 处 confirmText 生效前提）。
- 既有门禁基线：`npm run validate:flux` error=325（已裁决 `variant="primary"` 族，零新增即 pass；owner doc `flux-page-export-and-validation.md:273` 写「当前 exit 1」与实跑 rc=0 不一致，本计划闭包时以实跑输出为准并顺带修正 owner doc 该行）；freshness 门禁 `tools/validate-flux-fresh.sh --check-only` 当前 PASS。
- `docs/architecture/flux-integration-gotchas.md` 现有 G-NNN 陷阱登记，无本条。

## Goals

- 6 个存量手写页的全部惰性接线改为 flux 原生 Action Algebra：14 个 ajax 按钮（url/method/data/confirmText/messages 语义逐按钮保持）、12 处 after-success 刷新链（then 链等值迁移）、6 个 dialog 按钮（openDialog 迁移）。
- 浏览器运行时探针完成正反对照定案：修复前 `actionType:ajax` 按钮点击不发请求（负对照）+ flux 原生按钮发请求（正对照）；修复后被改按钮发请求（复验）。
- 陷阱登记：`docs/architecture/flux-integration-gotchas.md` 新增 G-NNN（手写页 AMIS actionType/onEvent/dialog 惰性陷阱 + 转换范式），防止后续手写页复发。

## Non-Goals

- pda-stock-take 页（归 plan 2 全闭环重构，避免同文件双计划触碰）。
- flux 渲染器/编译器源码任何改动（nop-chaos-flux 外部保护区不动）。
- 验证器 prop 表缺口收敛（i18nEn 误报等，外部仓，Deferred 见分析报告 §5）。
- 按钮既有业务语义/文案/视觉调整（仅改接线，不做 UX 重设计）。
- GenPage 生成页的 unknown-property warning（生成层 Deferred 维持）。

## Task Route

- Type: `bug investigation → implementation-only change`（已确认缺陷修复）
- Owner Docs: `docs/analysis/2026-10-05-0353-perf-ux-round2-increment-analysis.md`（本轮分析）、`docs/architecture/flux-page-export-and-validation.md`（门禁契约）、`docs/architecture/flux-integration-gotchas.md`（陷阱登记）、`docs/testing/e2e-runbook.md`（渲染模式与验证层级）
- Skill Selection Basis: `flux-rendering-conformance-audit-prompt`（修复遵循 F1.2/F6.4 判定与迁移指南转换表）；`development-wisdom-gate`（实施自检）

## Infrastructure And Config Prereqs

- 运行时探针需要本地 runner：`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`（构建产物已在 target；若缺失先 `mvn clean install -DskipTests`）。E2E live server 与 app-erp-all 集成测试互斥纪律见 e2e-runbook。
- flux 门禁前置：改 page.yaml 后须先 `mvn -pl <touched-module> install -DskipTests` 刷新 m2 再跑 `npm run validate:flux`（F2.2 m2 新鲜度）。
- 浏览器驱动使用 browser-use 技能（正/负对照探针）。

## Execution Plan

### Phase 1 - 运行时探针定案（Proof-first）

Status: completed
Targets: 无文件改动（探针证据落 `_tmp/`）
Skill: browser-use:control-browser

- Item Types: `Proof | Decision`

- [x] Proof: 启动本地 runner，browser-use 落**安全对照组合**（草案审查 round1 钉死）：负对照 = pda 页「解析条码」按钮点击断言无 `/r/` 请求；正对照按候选页 403 门控实况改为等效双证（详见执行记录）。证据（截图+网络记录）落 `_tmp/2026-10-05-0400-1-probe/`。
      **执行记录（2026-10-05）**：负对照成立——输入经 DOM 断言填入（6901234567890/WH01-L01）、原生 DOM click 派发后 3s，`performance` 资源缓冲零新增且**服务端日志零请求**（`nop.graphql.end-rpc-request` 记录全部到达请求；同会话 LoginApi×3/SiteMap/PageProvider/getLoginUserInfo 在案，`ErpInvBarcode__resolveBarcode` 全日志零命中）。正对照等效双证：同一原生 click 机制驱动菜单成功（组展开/路由切换实证）+ 同会话传输层正常（服务端日志在案），排除「环境不能派发/不能传输」两备择假设。截图+verdict 落 `_tmp/2026-10-05-0400-1-probe/`。
- [x] Decision: **证实 → 按 Phase 2 继续**。附带新发现 F11（text 内嵌 HTML 字面渲染）、F12（页面级 i18nEn 无运行时消费者）登记分析报告 §3.3，不入本计划范围。

Exit Criteria:

- [x] 探针证据落盘 `_tmp/2026-10-05-0400-1-probe/`（含负/正对照网络断言记录）
- [x] Decision 记录：探针结论 + 计划继续/终止的显式裁决写入本计划

### Phase 2 - 6 页机械改写

Status: completed
Targets: §Current Baseline 所列 6 个 page.yaml
Skill: flux-rendering-conformance-audit-prompt（转换表核对）

- Item Types: `Fix`（14 ajax 按钮 + 12 刷新链 + 6 dialog 按钮，同族接线修复）
- Prereqs: Phase 1 证实

- [x] Fix: 逐 ajax 按钮改写为 `onClick: {action:'ajax', confirmText?, args:{url, method, data}}`（机械改写总量 23 按钮 = 本批 14 + 孪生进入向导钮 2 + 孪生 start/complete/cancel 3 + 孪生镜像 4；14 口径指 round-2 审查冻结的 page.yaml 清单）；url/method/data 逐字段等值迁移；confirmText 迁为 args 的**兄弟键**；i18nEn 块随 confirmText 下沉同层（CAT4 checker 同层兄弟配对语义，执行期 Decision D1：初版把 i18nEn.confirmText 留在按钮层会脱离配对层，改为随 confirmText 迁移）。原按钮无 messages 者不新增（严格同构）。
- [x] Fix: 12 处 `onEvent: after-success` 刷新链等值迁移为 `then` 数组（12 块展开为 19 个 refreshSource 步骤 + 孪生镜像 7 处 target→then = 26 步骤总量，targetId 逐处保持；markAllRead/markRead 的 4 级链按 then 数组顺序保持）。
- [x] Fix: 6 个 `actionType: dialog` 按钮迁移为 `onClick: {action:'openDialog'|'openDrawer', args:{…}}`（6/6；inbox 3 处原 `dialog:{type:drawer}` 按 openDrawer 迁移，AMIS `type:drawer` 键删除——action kind 决定 surface 类型；period-close 3 处 openDialog，args 即 surface 本体经 `action-adapter.ts:259-301` 实核）。
- [x] Decision: **执行期范围增补（记录理由）**：6 页残留 19 处 `visibleOn`/`disabledOn`（visibleOn 14 + disabledOn 5）经导出产物实证同族惰性（disabledOn 原样穿透导出、非 renderer 声明字段）——按迁移表 `visibleOn→visible`/`disabledOn→disabled` 一并迁移。理由：同一结果表面（惰性接线归零）内的同族机械修复，扩展后 grep 门控覆盖 `visibleOn|disabledOn`。CAT4 影响零（条件 prop 无中文标量）。
- [x] 每页改写后局部校验：6 触域模块 `mvn install -DskipTests` 刷新 m2 + `npm run validate:flux` errors=325 零新增（6 页 errors=0）+ 6 页源文件 grep `actionType|onEvent:|visibleOn|disabledOn` 零命中（唯一命中为注释文字）。

Exit Criteria:

- [x] 6 页 14 按钮 + 12 刷新链 + 6 dialog 按钮全部改写（+范围增补 19 处条件 prop：visibleOn 14 + disabledOn 5），grep `actionType|onEvent:|visibleOn|disabledOn` 在 6 页零命中（pda 页除外——归 plan 2）
- [x] flux 门禁对新接线编译验证：errors=0、unknown-property 为 i18nEn 及 select level 等既有 prop 表缺口误报族（`_tmp/flux-page-validation-report.json` 当轮留证；「触碰页计数持平」的初期归因已被 B-1 整改记录取代——真因系孪生旧内容被门禁验证，见 Phase 3 B-1 note）

### Phase 3 - 运行时复验 + 陷阱登记 + 门禁

Status: completed
Targets: `docs/architecture/flux-integration-gotchas.md`、tests/e2e/
Skill: development-wisdom-gate

- Item Types: `Proof | Add`
- Prereqs: Phase 2

- [x] Proof: browser-use 复验（stock-take-flow 页 startTake 全链，2 轮迭代）：第 1 轮发现 F13（壳层对未注册 `@mutation:` 回退 `{data:...}` 信封致服务端 `unknown-operation-arg`）→ 执行期 Decision D2：标量参数自定义 mutation 改 `/r/` 直发 URL（resolver `/r/` 分支原样直达，9 处）+ disposal `__save` 去 author 预包裹改 registry 平铺形态（规则登记 G-002）；重建 runner 后第 2 轮全链通过：点击 → confirm 对话框弹出（**env.confirm 宿主能力实测已接**）→ `/r/ErpInvStockTake__startTake` 请求发出 → **docStatus DRAFT→CONFIRMED** → cleanup（cancelTake+delete）。
      **范围注记**：其余 5 页按钮运行时点击复验受 403 角色门控限制未逐页执行——同机制同转换表机械迁移 + 本页全链实证 + 全 6 页 flux 门禁编译级 0 error 承载；逐页点击覆盖随各域业务 E2E 排期（Deferred 登记）。附带新发现 F14（stock-take-flow 数据面与实体错位，预先存在）登记分析报告 §3.3，不入本计划范围。证据落 `_tmp/2026-10-05-0400-1-probe/`（runner2/runner3 日志 + verdict）。
- [x] Add: Playwright 冒烟 spec `tests/e2e/business-actions/flux-rewired-buttons.smoke.spec.ts`（stock-take-flow startTake：click→confirm→waitForResponse `/r/`→verifyState CONFIRMED→cleanup），**1/1 绿**（7.9s，本地 runner）。
- [x] Add: `flux-integration-gotchas.md` G-002 定稿（惰性机制 + 转换范式 + URL 形态规则 + env.confirm 实测 + 探针证据指针）。
- [x] Proof: `npm run validate:flux` errors=325 零新增（314 个生成页全部为已裁决 `variant="primary"` 基线族；门禁固有 exit 1 语义与 owner doc :273 表述一致——先验「exit 0」系管道 tail 退出码误读，owner doc 无需修正）；freshness 门禁 `--check-only` PASS。
      **结束审计 B-1 整改（2026-10-05，审计 agent_21adb14b）**：审计实证 period-close-wizard 与 visit-wizard 的**被服务内容是同名 `main.flux.yaml` 孪生**（render-mode=flux 优先加载，`PageModelLoaderFactory.java:37-46`；孪生为 08-03 批次 flux 原生 wizard 重写版，page.yaml 为非服务 legacy）——本计划对这两页 page.yaml 的改写运行期不可达，且门禁此前验证的是孪生旧内容（「warning 计数持平」的真实原因，非既有族增量）。整改：按同一转换规则迁移两份孪生（period-close 4 按钮：进入向导 @query + 3 标量 mutation `/r/` 直发 + then refreshSource；visit 5 按钮：进入向导 + start/complete/cancel `/r/` + save registry 平铺 + confirmText i18nEn 随层下沉）；page.yaml 改写保留（双文件一致）。门禁机械证明达成：两孪生导出条目惰性族 unknown-props 清零（fin 33→18 / mnt 40→22，清除 15+18=33；残余为 select level 等既有 prop 表缺口误报族）+ errors=325 零新增。G-002 补「孪生遮蔽陷阱」节（M-2 整改）+「被服务内容」口径修正。孪生运行时点击探针：服务内容身份实证（重载后向导 UI = flux.yaml 结构 + 改写 `disabled:${!periodId}` 生效——按钮初始禁用、选期间后启用）；进入向导按钮的完整点击链因期间选择器在 fresh dev 库下选项为空（种子数据面，与接线无关）未走通——同模式接线（同 action 词汇/同 resolver 路径/同 confirm 机制）已由 stock-take-flow startTake 第三页全链实证承载，孪生专属点击覆盖随 finance/mnt 域业务 E2E 排期（Deferred 已登记）。

Exit Criteria:

- [x] 运行时复验证据落盘（stock-take-flow startTake 全链实证 + confirm 弹窗在案；其余 5 页 403 门控限权注记 + Deferred 登记，见 Phase 3 执行记录）
- [x] Playwright 冒烟 spec 绿（1/1，7.9s，留证本节）
- [x] G-002 登记完成（owner doc 退出码行经实核无需修正——先前「不一致」系管道退出码误读）
- [x] validate:flux 零新增 error + freshness PASS

## Draft Review Record

- Independent draft review iteration 1: needs revision (agent_d0a0c323) — 0 BLOCKER / 4 MAJOR / 4 MINOR：M1 inbox 表单级 api 不存在（虚空 Fix 项+计数失真）/ M2 confirmText-messages 应为 args 兄弟键（照初稿执行静默丢失确认框）/ M3 12 处 onEvent 刷新链未纳入改写语义 / M4 6 个 dialog 按钮同族惰性零裁决 + m1~m4（探针安全页选择/复验覆盖/owner doc 退出码矛盾/弱措辞）。
- Independent draft review iteration 2: accept (agent_e9902535) after <M1 计数实核吻合（14 ajax/12 onEvent/6 dialog/6 页 form-api=0）；M2 兄弟键范式与 AjaxActionSchema/runtime 读取路径一致 + env.confirm 复验入 Phase 3；M3 三处齐备；M4+m1/m2/m4 全落实；无新问题>

## Closure Gates

- [x] 范围内行为完成（6 页 8 个被服务/镜像文件〔4 页 page.yaml 单文件 + 2 页 page.yaml+flux.yaml 双文件〕；机械改写总量 23 ajax 按钮 + 26 then 步骤 + 6 dialog + 8 openDrawer/openDialog + 19 条件 prop；代表页运行时全链实证）
- [x] 相关文档对齐（分析报告 F11-F14 登记 + gotchas G-002 含孪生遮蔽陷阱 + URL 形态规则）
- [x] 已运行验证（`mvn clean install -DskipTests` 全量构建 BUILD SUCCESS〔4 轮：初版/F13 修正/孪生修正/R-1 cancel then 链后终版，`full-build4.log` 留证〕+ `npm run validate:flux` errors=325 零新增 + freshness PASS + 孪生 unknown-props 清空机械证明 + Playwright 冒烟 1/1 绿 + compliance checker exit 0 + cjk report exit 0 + plan-gates --strict PASS + 无界查询台账 PASS）
- [x] 无范围内项目降级为 deferred/follow-up（3 项 Deferred 均带触发条件正式登记）
- [x] 独立草案审查已完成并记录（2 轮收敛）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致（日志条目随闭包提交写入 docs/logs/2026/10-05.md）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（round 1 NEEDS REVISION〔B-1 孪生遮蔽〕已整改；round 2 记录见 Closure）
- [x] 结束证据存在于文件中（`_tmp/2026-10-05-0400-1-probe/` + 门禁日志 + spec）

## Deferred But Adjudicated

### 其余 5 页被改按钮的逐页运行时点击覆盖

- Classification: `optimization candidate`
- Why Not Blocking Closure: 代表页（stock-take-flow）全链实证 + 两孪生页被服务内容与编译级证明承载；逐页点击受 403 角色门控与测试数据前置（如期间选择器）限制，属各域业务 E2E 排期面。
- Successor Required: `no`（触发=各域业务 E2E 迭代排期）

### mnt/fin legacy page.yaml 的 URL 形态差异

- Classification: `watch-only residual`
- Why Not Blocking Closure: 非服务 legacy 无运行期影响；flux.yaml（被服务）已按 G-002 URL 规则迁移，legacy page.yaml 的部分标量 mutation 仍为 `@mutation:` 旧形态——legacy 复活（flux.yaml 删除）时须先按 G-002 迁移。
- Successor Required: `no`（触发=legacy page.yaml 归档治理或 flux.yaml 移除决策）

### twin page.yaml 非服务 legacy 的归档治理

- Classification: `watch-only residual`
- Why Not Blocking Closure: period-close/visit 两页的 page.yaml 为非服务 legacy（flux.yaml 为被服务内容），本轮已同步改写保持双文件一致；legacy 归档属页面资产治理。
- Successor Required: `no`（触发=页面资产治理专项）

### 手写页 unknown-property warning 归零门禁化

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划修复接线后，6 页的 actionType/api/onEvent warning 自然消失，但 i18nEn 等误报仍在；把手写页 warning 纳入冻结基线需先完成 plan 2（pda 页）后统一做基线裁决。
- Successor Required: `yes`（复扫轮：手写页 warning 冻结基线批次）

## Closure

Status Note: 独立结束审计三轮收敛（round1 NEEDS REVISION〔1 BLOCKER：flux.yaml 孪生遮蔽使 2/6 页改写运行期不可达且门禁验证孪生旧内容——审计方独立复验全部门禁与运行时证据后定位；整改=同规则迁移两孪生 + G-002 孪生遮蔽陷阱节 + 计数订正〕→ round2 NEEDS REVISION〔1 MAJOR：mnt 孪生 cancel 按钮丢失 then 刷新链 + 5 MINOR 文本簇——整改=R-1 then 链补回 + mnt 模块 install + 门禁复跑〔errors=325/visit warnings=22〕+ R-2 九项文本订正〕→ round3 delta 复审 ACCEPT〔R-1 机械+产物双证、R-2 八/九落盘、全门禁绿、build4 BUILD SUCCESS；附 closure 编辑义务 MINOR-1/INFO-1/INFO-2 三处文档收尾——已随本闭包完成〕）。范围：6 页 8 个被服务/镜像文件（机械总量 23 按钮/26 then 步骤/6 dialog/19 条件 prop）+ gotchas G-002 + 冒烟 spec；新发现 F11-F14 登记分析报告；批次 2-5 归后续 plan。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 round1 agent_21adb14b（NEEDS REVISION）/ round2 agent_cb632860（NEEDS REVISION）/ round3 agent_c5917237（ACCEPT）
- Evidence: `_tmp/2026-10-05-0400-1-probe/`（probe-verdict + 截图 + runner2/3/4 日志 + full-build4.log + gate-final*.log）；`_tmp/flux-page-validation-report.json`（errors=325 零新增/两孪生惰性族清零）；Playwright `flux-rewired-buttons.smoke.spec.ts` 1/1 绿（审计方独立复跑 7.7s 吻合）

Follow-up:

- 批次 2-5（pda 闭环/mfg 角色探针/MaskAuditRecorder 去重/barcode 索引）按 plans 2026-10-05-0400-2..5 顺序执行
