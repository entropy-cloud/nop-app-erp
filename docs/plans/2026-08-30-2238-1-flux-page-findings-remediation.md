# 2026-08-30-2238-1-flux-page-findings-remediation Flux 页面验证发现集修复（ERP 源 + 上游 3 项）

> Plan Status: completed
> Last Reviewed: 2026-08-30
> Source: plan 2026-08-30-1126-1（Flux 页面导出+编译验证工具链）首跑发现集；用户指令「修复 nop-app-erp 中发现的问题」
> Related: `docs/architecture/flux-page-export-and-validation.md`（工具链契约）、plan 2026-08-30-1126-1（Follow-up：发现项修复切片）
> Audit: required

## Current Baseline

- 工具链已落地（plan 2026-08-30-1126-1 completed）：`npm run validate:flux` 导出 999 页并编译校验，当前语料报告 `_tmp/flux-page-validation-report.json`（999 文件 / **1104 error**；erp/* 892、平台 nop/* 212）。
- **运行时事实（Approver B 编译探针 + 渲染链源码证实，修正「当前能跑」的预设）**：浏览器运行时编译为 `continueOnError`——违反契约的 prop/节点被**剥离**而非报错。因此发现集里的多数错误对应**今天浏览器里已坏的功能**：17+ 看板 KPI 卡片区渲染为空（grid columns 数组被剥离）、附件按钮点击失败（`{type:'api'}` 未知 action）、notify 点击弹错误 toast、asn-flow 行点击选中从不激活、tpl 列模板被忽略显示原始值。本计划是修复真实缺陷，不是「能跑的页面改写」。

### 发现集精确分账（双批准人逐条重算一致）

**erp/* 892 = ERP 源可修 124 + 上游所致 768**：

- 上游 768 = dependsOn 462（全部来自 nop-entropy `flux-web.xlib:127` 生成的 crud loadAction 无 dependsOn；ERP 手写 .flux.yaml 的 reaction 均已正确声明）+ variant 288（flux `data-schema-validation.ts:96` 编译期自建查询表单发射非法 `variant:'primary'`，导出 JSON 中不存在该值）+ view-file blob 18（nop-entropy `flux-control.xlib:605-619/662` 的 view-file/view-file-list 控件发射 amis 风格 `{type:'api',responseType:'blob'}`）。
- ERP 源可修 124：F1 tpl 列 45 / F2 number 列 6 / F3 static 列 4 / F4 wrapper 4 / F5 panel 4 / F6 grid 布局 22（**20 个文件**）/ F7 表达式 12 / F8 notify 5 / F9a then:[] 14 / F10 region 6（payroll 2 + party-search 4，两处空 region 各被重复报告为 invalid-region-node + unhandled-compilation-error）/ F11 hidden 布尔 1 / F12 rowClick 1。

### 逐类溯源与已验证配方

| # | 类别 | 数量 | 源头（实测） | 修法（编译探针验证） |
| --- | --- | --- | --- | --- |
| F1 | 列 `type:'tpl'` | 45 | ct `ErpCtContract.view.xml` gen-control c:script 等 | **plain 列**（去 type，0 err 0 warn；运行时列上下文本就不按 type 分发模板，tpl 今天就是死属性——已知视觉降级：`label-*` Bootstrap 类无样式，登记非回归）；确需模板渲染用 `cell:{type:'text',text}` region |
| F2 | 列 `type:'number'`+`kilometer` | 6 | ast `ErpAstInventoryLine.view.xml`、inv `ErpInvStockMoveLine.view.xml` gen-control JS | 去 `type`/`kilometer` 保留 align/precision（plain 列；千分位在 flux 运行时从未存在，零退化） |
| F3 | 列 `type:'static'` | 4 | fin `ErpFinVoucherLine.view.xml` gen-control JS | 去 type 用 plain 列 |
| F4 | `wrapper` | 4 | ast disposal-wizard、crm lead-conversion `main.page.yaml`（无 flux 孪生）；cs `ErpCsTicket.view.xml:138/202` | → `container`（className/body；visibleOn→visible） |
| F5 | `panel` | 4 | **mnt visit-wizard `main.flux.yaml`:82/104/113/154**（孪生回退：flux 模式加载 flux.yaml 而非 page.yaml） | → `container` |
| F6 | 布局 `grid`+`columns:[card...]` | 22 / 20 文件 | 17 看板 + asset-stocktake/ErpCsQualityDashboard/cost-center/project-pnl/project-settlement 等 page.yaml | → `grid{columns:N,items:[{body:[card]}]}`（N=卡片数；items body region 编译验证）——**激活当前空渲染的 KPI 区** |
| F7 | 表达式 | 12 | b2b asn-flow/edi-detail（sendOn 包 `${}`——schema.ts 明示 raw expression；edi-detail formula 箭头函数+语句体）、ct version-diff×2、drp net-requirement、fin bills-by-voucher（regex 字面量）、log shipment-tracking、pur three-way-match×3 | sendOn 去 `${}`；formula 重写为三元链/map/indexOf/slice |
| F8 | `action:'notify'` | 5 | aps/crm×2/cs/prj 5 个 `.flux.yaml` | → `action:'showToast'`（args={level?,message}，BUILT_IN_ACTION_REGISTRY 无 notify；当前点击是错误 toast） |
| F9a | onClick `{then:[]}` | 14 | view.xml `<action link="..." >` **缺 `actionType="link"`**（平台 `flux-web.xlib:848` 按 actionType 分支映射 navigate，缺失则 `_.delete` 清空 link 后剩空壳） | 补 `actionType="link"`（平台即映射 `{action:'navigate',args:{url}}`） |
| F10 | region 条目非法 + `primaryField` | 6 | hr payroll-approval（actions 区放 action 对象）、md party-search picker（footerToolbar 空、primaryField、Action entries 非对象） | region 条目按钮化 `{type:'button',label,onClick}`；`primaryField`→`rowKey` |
| F11 | `hidden:"true"` 字符串 | 1 | hr `ErpHrEmployee.view.xml:268` gen-control XML attr 直通 | gen-control 改 c:script 输出 `{type:'input-number',hidden:true}` |
| F12 | `rowClick`+`$slot` | 1 | b2b asn-flow `.flux.yaml:106-109` | → `onRowClick:{action:'setValue',args:{path:'selectedAsnId',value:'${record.id}'}}`（运行时 rowScope 展开 record；当前详情面板从不激活） |

### 上游 3 项（保护区域，dual-agent-approval）

- **U1（nop-chaos-flux）**：`packages/flux-renderers-data/src/data-schema-validation.ts:96` `variant:'primary'`→`'default'`。自产自检矛盾（button 枚举无 primary；运行时本就剥离回落 default，`button.tsx:65` cva defaultVariants=default，视觉零变化）；无测试断言该发射值。A/B 均已批准。
- **U2'（nop-entropy，替代被 Approver A 否决的「flux 校验器降级」方案）**：根因是 nop-entropy 生成器不合规（crud loadAction 无 dependsOn），flux 仓 normative（`dependency-tracking.md §3.3.1` dependsOn required + 2026-07-07 plan 行 102 裁定 error）**不动**。修法：**两个发射点**同发惰性哨兵 `dependsOn: ['__crud_load__']`——`flux-web/grid_crud.xpl:22`（主/picker crud 页，406 个）与 `flux-web.xlib:127` GenGridImpl（view 弹窗/页签内嵌 grid，56 个，链路 impl_flux_mode.xpl:36-43 → impl_GenGrid.xpl:24；Approver A iteration 2 必改：单修 grid_crud 只能 892→180）。哨兵为 flux 仓纯命令式 reaction 的 dummy-root 惯例（其 crud 测试用 `dependsOn:['__crud_test__']`）；哨兵根永不写入 → reaction 不自动触发，命令式链（ready 初始 fire/分页/查询提交）不变——B' 验证现状 deps=undefined 属「任意 scope 写入即可 force reload」的过度触发缺陷态，U2' 收敛为仅命令式（消除缺陷、回归 normative 意图）。**不要顺手改** `page_simple.xpl:61`/`container_simple.xpl:34` 的 loadAction（挂 page/container 渲染器，非 reaction 字段，今天 0 error）。master + fix-ai-check 两树同源落地并 install（fix-ai-check 为 ERP 平台基线）。

- **U3b（nop-entropy）**：`flux-control.xlib` view-file（605-619）与 view-file-list（~655-670）onClick `{type:'api',url,method:'get',responseType:'blob'}` → `{action:'ajax',args:{url,method:'get',responseType:'blob'}}`。flux runtime 完整支持 blob 下载（`flux-runtime/src/async-data/blob-download.ts` content-disposition 文件名解析 + `request-runtime.ts:323/347` responseType 透传）；当前这些附件按钮在浏览器点击即失败。两树同源落地。

- 取舍记录（U2）：曾评估 (a) flux 校验器 error→warning（Approver B 批准，但与 flux normative 文档及 2026-07-07 已完成计划的显式裁定冲突，Approver A 否决）；(b) 修订 flux normative 基线（该仓 successor plan 纪律，体量不成比例）；**(c) 生成器哨兵 dependsOn（选定）**——尊重上游契约、零运行时变化、改动点最小。
- 环境：nop-chaos-flux 当前 `pnpm build` 因预存 TS 错误（flux-renderers-content markdown.tsx）失败——U1 文件所在包可 `pnpm --filter` 单独重建；nop-entropy 两树 + m2 纪律同上一计划（fix-ai-check 树为 ERP 基线，master 为规范集成位）。

## Goals

- ERP 源 124 error 全修 + 上游 3 项落地 → **erp/* 编译验证 error == 0 且退出码 0**。
- 激活的运行时功能不回退：附件下载、看板 KPI 区、notify 提示、asn-flow 行选中、link 跳转。
- 视觉基线按 runbook 重录（漂移仅来自预期变更）。

## Non-Goals

- 平台页（nop/* 212）修复——大头 dependsOn 70 随 U2' 自动消解、variant 残余（4 处手写字面量）与 static 90 等归 nop-entropy 上游跟进（Deferred 登记）。
- warning 清零（41341，退出码契约只看 error）。
- flux-renderers-content 预存 TS 错误、example.json 松弛示例（该仓日志已登记）。

## Task Route

- Type: `implementation-only change`（页面 schema + 平台生成器缺陷修复，无模型/契约变更）
- Owner Docs: `docs/architecture/flux-page-export-and-validation.md`、`docs/architecture/view-and-page-strategy.md`、`docs/testing/e2e-runbook.md`（视觉重录 + m2 新鲜度）
- Skill Selection Basis: 页面/view.xml/xlib 编辑无匹配业务技能（配方全部经编译探针预验证）；审计用 `plan-audit-prompt.md` / `closure-audit-prompt.md`。

## Infrastructure And Config Prereqs

- 上游先行（Phase 1）使后续进度指标单调：U1+U2'+U3b 落地后 erp error 立即 892→124。
- 模块资源变更后 `mvn -pl <module>-web install -DskipTests` 再导出（m2 新鲜度）；同批模块可 `mvn -pl a,b,c install` 合并，每批至少一个 validator 检查点。
- 视觉重录需 Playwright 环境。

## Execution Plan

### Phase 1 - 上游 3 项（U1 / U2' / U3b）

Status: completed
Targets: `../nop-chaos-flux/packages/flux-renderers-data/src/data-schema-validation.ts`；`../nop-entropy（master + fix-ai-check 两树）/nop-frontend-support/nop-web/src/main/resources/_vfs/nop/web/xlib/flux-web/grid_crud.xpl`、`.../xlib/flux-control.xlib`
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: 双独立子代理批准（含 U2' 替代方案的重新确认，记录落盘 Draft Review Record）

- [x] Fix：U1 variant primary→default + flux 侧相关测试核查（无断言该发射值，负例测试不受影响）
- [x] Fix：U2' grid_crud.xpl loadAction 发射 `dependsOn:['__crud_load__']`（注释引用 dummy-root 惯例与根因）
- [x] Fix：U3b flux-control.xlib view-file / view-file-list onClick → ajax+blob
- [x] Proof：U1 后 `pnpm --filter @nop-chaos/flux-renderers-data build` + 该包测试绿 + 金标集复跑 0 error；U2'/U3b 后 fix-ai-check 树 `mvn -pl nop-frontend-support/nop-web install` → ERP 导出复跑：dependsOn/view-file 类 erp error 归零（预期 892→124）
- [x] Add：flux worktree 提交本计划文件集（validate-pages.mjs/shared.mjs/validate.mjs/package.json/docs/logs/08-30.md + U1 + 测试；**避开他人未提交的 docs/analysis 与 design-patterns 文件**）；nop-entropy 两树日志

Exit Criteria:

- [x] erp error 892→124（仅剩 ERP 源 12 类）；上游三包/模块测试绿；上游仓提交完成

### Phase 2 - ERP view.xml 修复（F1/F2/F3/F4-cs/F9a/F11）

Status: completed
Targets: ct/drp/fin/hr/inv/log/mfg/mnt/cs 各模块 `*.view.xml`
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: Phase 1（否则进度指标混杂上游噪声）

- [x] Fix：F1 tpl 列（plain 列为主，模板语义强的用 cell region）
  - **执行中范围扩展（Decision，2026-08-30）**：tpl gen-control 位点经全仓 grep 为同构模式共约 130 处（原计划按报错文件计 45 处）；其中报错的 45 处在弹窗内嵌 table 列（列 type 经 renderer 校验），其余在主 crud 列与 form 字段（不触发编译错误但运行时模板死渲染——列上下文不按 type 分发）。选择**全仓同构清剿**（同一机械变换 `tpl→cell:{type:'text',text}` / `type:'tpl',tpl:→cell text`）：理由 (a) 同构变换审阅成本低；(b) 主 crud 列模板今日显示原始值（脱敏/状态徽章失效），cell region 激活真实渲染属运行时修复；(c) 避免同类问题二次立项。风险登记：~86 源文件大面积变更、list 列视觉从原始值变为模板渲染（正向变化，无 list 页像素基线），以编译验证 + Phase 4 抽样目检兜底。form 字段位点的 tpl（不报错、运行时未知类型被剥离）同变换为 cell text——form 字段 cell 为未知属性（warning 级），字段本身 custom/notSubmit 不参与提交，行为不劣于现状（今日不可见）。
- [x] Fix：F2/F3 number/static 列 plain 化
- [x] Fix：F4 cs ticket `<wrapper>`→container
- [x] Fix：F9a 14 处 `<action link=...>` 补 `actionType="link"`
- [x] Fix：F11 hr hidden 布尔（c:script）
- [x] Proof：分批 install（每批 ≥1 validator 检查点），本阶段类别 error 归零

Exit Criteria:

- [x] F1/F2/F3/F4-cs/F9a/F11 覆盖错误归零（validator perFile 核验）

### Phase 3 - ERP 手写 page.yaml/.flux.yaml 修复（F4-yaml/F5/F6/F7/F8/F10/F12）

Status: completed
Targets: 对应 `main.page.yaml` / `.flux.yaml` / picker 页（F5 在 **main.flux.yaml**）
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: Phase 2（同模块合并 install）

- [x] Fix：F4/F5 wrapper/panel→container（含 visit-wizard flux.yaml 4 处）
- [x] Fix：F6 20 文件 grid 布局 → grid{columns:N,items}（逐页确认卡片数与响应式语义）
- [x] Fix：F7 12 处表达式（sendOn 去 `${}`；arrow/regex formula 重写）
- [x] Fix：F8 notify→showToast ×5
- [x] Fix：F10 payroll actions 按钮化 + party-search rowKey/footerToolbar/actions
- [x] Fix：F12 onRowClick setValue
- [x] Proof：分批 install + validator 检查点

Exit Criteria:

- [x] erp/* error == 0（validator 对导出 erp 子树直验）

### Phase 4 - 端到端验证、视觉回归与收口

Status: completed（2026-08-31：validate:flux erp exit 0 / 全量构建绿 / checker 零漂移 / 视觉 10+10 绿 / 行为冒烟完成；三仓日志见 docs/logs/2026/08-30.md 与 nop-entropy ai-dev/logs/2026/08-30.md）
Targets: wrapper 脚本、`_tmp/flux-page-validation-report.json`、e2e visual baselines、三仓日志
Skill: none

- Item Types: `Proof | Add | Follow-up`
- Prereqs: Phase 1-3

- **执行中范围扩展（Decision，2026-08-30/31，用户指令触发）**：视觉回归揭示两簇新根因，均在「修复=激活真实缺陷」义务范围内：
  - **V1 看板过滤失效簇（P1）**：看板 + payroll 的页面级 data-source 模板读 `${filterForm?.x}` 恒 null——flux named form **仅经 `valuesPath` 才向父作用域发布值**（`form-runtime.ts setupExternalPublication`），未配置时过滤参数从未发出（探针证实请求体 `periodId:null`）。修复=filterForm 补 `valuesPath: filterForm`（**8 看板** fin/sal/pur/inv/ast/mnt/mfg/qa + payroll 同源重写；**prj 无过滤表单**——审计 MF-2 更正：原「9 域」口径把 prj 误计，其 `${filterForm?.projectId}` 为死引用恒 null 等价无过滤（value-spec 亦无过滤参数），已删除死参数行）。KPI 断言由此翻绿（finance 1130/sales 1000/purchase 850/inventory 10450/assets 135000/mfg 180/quality 0.67 全命中 value-spec 权威值）。
  - **V5 结束审计 MF-2/MF-3 追加固化（2026-08-31）**：首轮结束审计（agent_1caacaf2，NEEDS_REVISION）发现 cs `ErpCsQualityDashboard` 与全仓 report/wizard/picker 页存在同型活体缺陷（named form 无 valuesPath + 跨作用域 `${formName?.x}` 读）。按 F1「全仓同构清剿避免二次立项」先例处置：**53 文件批量补 valuesPath**（report 集群 balance-sheet/cash-flow/income-statement 等、wizard disposal/lead-conversion/visit-wizard/period-close/stock-take、kanban/timeline、picker partyFilter、page.yaml 孪生）。实证（探针，跑后删除）：balance-sheet 渲染报表请求体 periodId 1→2 真实送达（修复前恒 null）；cs 看板 KPI 请求体为 filterForm 投影形状。回归 dashboards.visual 10/10 + fin-balance-sheet.smoke 绿；validate:flux erp 855 文件 0 error 保持。commit 2eef3c348。
  - **V2 文档缺口（用户直接指令，人工批准）**：`@query:`/`@mutation:` 前缀的 CRUD 参数整形（nopRpcResolver operationRegistry：尾缀命中注册操作只提交特定命名参数、filter_* 转 TreeBean、未注册 @query 透传、未注册 @mutation 兜底 `{data}`）docs-for-ai 未覆盖——已补 `docs-for-ai/02-core-guides/flux-rendering.md`（两树同步提交 1584f4a2cf / 4c275d80c2），含 /p/ 行、valuesPath 跨作用域规则与页面作者规则。
  - **V3 行为激活范式修正**：asn-flow 行点击 `onRowClick+setValue` 不可用（行事件 action 上下文=行作用域，`scope.update` 本地写行内遮蔽；crud selection 组合态固定写 `$_crud.<id>.*` 且 reaction 不订阅该命名空间）——重写为操作列按钮 + `openDialog`（行绑定 `${id}` 进 surface 作用域，flux-guide crud-with-dialog 范式）。voucher/recon 反冲预览模板 flux 化（HTML 标签非法→纯文本多节点 + `previewData?.x` 可选访问 + `| default:` 管道→`??`）。
  - **V4 测试侧腐化修复**：`_helper.ts` REST `/r/` 等待（8/29 迁移遗留）、KPI 取值选择器 `h3`、图表断言 `svg.recharts-surface`、`pickFluxDate` 日历驱动（day-15 探测显示月份防上月格振荡）、过滤重载改为 fill 自动重载等待（模板依赖跟踪防抖重发，click refreshSource 被去重）；spec 补 6 域权威 seed 窗口参数；reverse-preview.action.spec 数据构造修复（postVoucher 从行重算合计→补平衡分录行）。

- [x] Add：`scripts/validate-flux-pages.sh` 增加 ERP 范围校验步骤（validator 直验 `$EXPORT_DIR/erp` 子树，零上游改动）——ERP 侧退出码必须 0；全量（含平台页）保留原语义
- [x] Proof：`npm run validate:flux`——erp 子树 exit 0 / error 0；全量报告留存（平台残余登记 Deferred）
- [x] Proof：ERP 全量 `mvn clean install -DskipTests` 绿（FULL_BUILD_EXIT=0，2026-08-31）+ `bash docs/audits/nop-compliance-checker.sh` 零漂移（exit 0，命中项均为既有形态）
- [x] Proof：视觉回归——`dashboards.snapshot.spec.ts` **全量 10 test**：复跑确认漂移仅来自 KPI 区激活（35% 像素=卡片网格从空渲染激活，diff 审视 + CDN 截图目检确认 6 卡横排正常），`--update-snapshots` 重录后 10/10 绿；`dashboards.visual` KPI 断言 **10/10 翻绿**（V1 修复后）；party-search picker（rowKey 修复后 f16 系列绿）；行为激活冒烟完成：voucher 红冲 dialog 内容渲染无模板错误、asn 流程 dialog detail+steps+明细行全渲染、cs kbSuggestion 条件空态正常（种子无匹配属业务语义）、recon 反冲模板与 voucher 同款（种子无 POSTED 数据，按钮存在性由 visibleOn 守卫，模式由 voucher 证明）、reverse-preview.action.spec + f16-p2 复跑 8/8 绿
- [x] Add：三仓日志（ERP docs/logs/2026/08-30.md 两段 + nop-entropy ai-dev/logs/2026/08-30.md 追加 V2 文档变更段）+ 发现分账终态补记（本节下方「终态对账」）
- [x] Follow-up：平台页残余（variant 4 处手写字面量、static 90 等）已登记 Deferred But Adjudicated → successor 触发条件落盘

**终态对账（2026-08-31，复审核准口径）**：erp/* 892 error → **0**（erp 子树 validator exit 0，999 页导出 0 失败）；执行中范围扩展 V1-V5 另修 **8 看板 + payroll 过滤失效（V1，valuesPath）**、asn-flow 交互范式、voucher/recon 预览模板、6 处测试侧腐化，及 **53 文件 valuesPath 同型清剿（V5，含 cs 绩效看板与 report/wizard/picker 集群；复审独立重扫 0 残留）**。视觉终态：dashboards.snapshot 10/10（KPI 激活基线重录）+ dashboards.visual 10/10（KPI token 全命中 value-spec 权威值）。

Exit Criteria:

- [x] erp 子树 exit 0；全量构建绿；checker 零漂移；视觉基线重录或漂移裁决落盘；行为激活验证完成

## Draft Review Record

- Independent draft review iteration 1（2026-08-30，双批准人 fresh session 并行）：
  - **Approver A**（agent_6caee492-068b-4114-af76-fc742a1c83f9）：NEEDS_REVISION——U1 批准 / **U2（校验器降级）否决**（flux normative `dependency-tracking.md §3.3.1` + 2026-07-07 plan 行 102 显式裁定 + Deferred successor 纪律；根因在 nop-entropy 生成器 `flux-web.xlib:127`；ERP 手写 reaction 均合规）/ M1 F9 溯源错误（18 blob 是平台 view-file 控件发射、14 是 `{then:[]}` link 缺 actionType，原「`{type:'api'}` 在 ERP 源」0 命中）/ M2 分账失真（142/750、F10=6、F6=20 文件、U1 全局 297 vs erp 288）/ M3 F5 目标应为 main.flux.yaml（孪生回退）/ m1-m3（视觉范围 10 test、F1 视觉降级注记、Deferred static=90 与 nop variant 9 不随 U1 消解）。
  - **Approver B**（agent_05ca1af9-d620-430e-b975-9ee22ec7ab5c）：APPROVE 附必改——运行时事实链（continueOnError 剥离：17 看板 KPI 今天空渲染、F8 错误 toast、F12 死功能、F1/F2 死属性——修复=激活真实缺陷）/ F1 配方必改为 plain 列或 cell region（typed 列模板不渲染且增 warning）/ Phase 4 必改 erp 子树直验 exit 0 / 顺序倒置（上游先行）/ 视觉义务明示（10 基线全量重录 + KPI 断言翻绿 + party-search/visit-wizard + 行为页冒烟）/ flux worktree 提交义务 / 分账 142/750 精确闭合。
  - **修订落盘（执行者）**：U2 → U2'（nop-entropy 生成器哨兵 dependsOn，Approver A 预认可方向 (a)）；F9 拆 18（→U3b 上游）+14（actionType="link"）；F5 目标改 main.flux.yaml；F1 配方改 plain/cell；分账 124+768；F6=20 文件；Phase 顺序倒置；Phase 4 erp 子树直验 + 视觉全量 10 test + 行为冒烟 + worktree 提交；Deferred 修正。**U2' 与 U3b 为新增 nop-entropy 变更，须双批准人就修订后的上游集重新确认**（iteration 2）。
- Independent draft review iteration 2（2026-08-30，上游变更集 U1+U2'+U3b 重新确认）：
  - **Approver A**（agent_6caee492-068b-4114-af76-fc742a1c83f9，续审）：**APPROVE**——U2' 设计正确（哨兵零新增诊断、运行时零变化、尊重 normative），附 1 必改（U2' 目标须含 grid_crud.xpl:22 + flux-web.xlib:127 GenGridImpl 两个发射点，406+56=462，已并入计划文本）+ 1 勘误（Deferred variant 残余=4 处手写字面量 + 5 处 enrichment 随 U1 消解，已修正）+ 防过度修复注记（page_simple/container_simple 的 loadAction 不改，已写入）。声明「落盘后无需再过 iteration 3」。
  - **Approver B'**（agent_af055b56-5f29-4df7-b2f7-f7f0cc2556ab，fresh session 接替不可达的 B）：**APPROVE 无必改**——独立验证 U2'（哨兵根 parsePath 合法、全生态零写入方、命令式链不受影响、两树基线 IDENTICAL）、U3b（ajax fieldRules + runtime blob 链路端到端）、U1（枚举/回落证据）；9 项必改并入核查全过；哨兵外溢评估：全部 nop 应用生成页合法化、碰撞概率可忽略、性能更优。附 4 项非阻塞建议（运行时口径按「消除过度触发缺陷」记录、GenGridImpl 残留风险已随双发射点修复消除、view-file-list 外层 dialog 结构 successor 登记、picker 页 5 处同源自动修复）。
  - 双批准成立（A iteration 2 + B'），Plan Status 转 active。

## Closure Gates

- [x] 范围内行为完成（F1-F12 + U1/U2'/U3b）
- [x] 相关文档对齐（设计文档终态数字、三仓日志、runbook 无需变更）
- [x] 已运行验证：erp 子树 validator exit 0 + 全量构建 + checker + 视觉重录 + 行为激活冒烟
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查（含保护区域双批准 iteration 2 对 U2'/U3b 的确认）已完成并记录
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理（新会话）执行
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 平台页（nop/*）残余修复

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划范围=「nop-app-erp 中发现的问题」；U2' 使 dependsOn 70 自动消解、U1 消解 nop 侧 enrichment variant 5 处，残余（variant **4 处手写字面量**（DemoPage×2 含 success、assign-auth、change-self-pass）、static 90、tpl 等）属 nop-entropy 页面资产；GenGridImpl 同族未来 form 嵌 grid 控件复现风险已随 U2' 双发射点修复消除
- Successor Required: yes（触发：本计划合入后复跑全量验证，平台残余>0 时 nop-entropy 侧立项；另 view-file-list 外层 `{type:'dialog'}` 结构性修复一并评估）

### warning 清零（41341）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 工具退出码契约只看 error
- Successor Required: no

## Closure

Status Note: **completed（2026-08-31 复审 APPROVE 闭合，无需第三轮审计）**。

MF 处置记录（2026-08-31）：

- MF-1：master 树 U2'/U3b 三文件补提交 `7e324192fc`（2d1c47f0ac message 与文件清单不符的事实错误已在补提交说明中注明；fix-ai-check 树 7e074c2482 原本完整，两树 diff 空）。
- MF-2：prj 死引用删除 + V1 口径更正（8 看板 + payroll）。
- MF-3：cs 看板修复 + 53 文件同型清剿（V5 段）+ 双实证。
- MF-4：Phase 1-4 执行项与退出标准、Closure Gates 补勾（本节随复审结论更新）。

Closure Audit Evidence:

- Auditor / Agent: 首轮 agent_1caacaf2-2e40-45b0-a1be-7775d64b4a90（NEEDS_REVISION，MF-1..MF-4）；复审同 agent（2026-08-31）——**APPROVE**：MF-1..4 全部 CLOSED（master 7e324192fc 三文件核验 + `git log -S __crud_load__` 命中；prj 死引用删除 + 口径勘误留痕；同型缺陷独立重扫 0 残留 + 导出语料 valuesPath 实证；全文 `- [ ]` 计数 0），无第三轮义务
- Evidence: 两轮审计报告全文见会话记录；MF 处置提交 7e324192fc（entropy master）/ 2eef3c348（ERP）；验证产物 /tmp/vis-kpi6.log、/tmp/vis-snap2.log、/tmp/beh6.log、/tmp/reg1.log、/tmp/reg2.log、/tmp/validate-final2.log、/tmp/full-build.log、/tmp/checker.log
- 复审 CONCERN 登记（不阻塞）：(1) flux-web.xlib 两树存在非本计划的 G-001 块漂移（master 1063f7f4b9 未移植 fix-ai-check），归 G-001 owner 计划跟进；(2) V5 行为实证探针未留存（以导出语料 + reg2/validate-final2 回归代偿），后续关键行为实证宜可重放 spec 固化

Follow-up:

- 平台页残余修复（见 Deferred）
