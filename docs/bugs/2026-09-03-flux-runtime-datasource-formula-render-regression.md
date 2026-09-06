# flux 运行时 data-source/公式渲染回归——看板空渲染、时间线/日历错误框、模板公式 mount 期抛错

- 日期：2026-09-03
- 发现于：plan `2026-09-03-0938-1-dual-amis-office-viewer-peer-alignment` Phase 2 全量 visual 目录级验证（双 amis pageerror 消除后首次暴露）；同日 M2.1 计划 `2026-09-03-0400-1` Phase 2 批次 C 复现
- 状态：fixed（未提交，工作树，owner 提交移交；跨仓库保护区 = nop-chaos-flux/nop-chaos-next `auto + dual-agent-approval`；修复计划 = docs/plans/2026-09-03-1815-1-flux-unpublished-scope-tolerance-and-scheduling-bundle.md，Phase 1 双批准 + Phase 2 F1-F4 落地 + Phase 3 链路发布与 ERP 实证完成，见下方「2026-09-06 回写」节）
- 影响：**flux data-source 驱动页面 + 未护栏公式消费节点**的渲染正确性与 E2E console 守卫；不影响 dashboards/reports 像素资产（2026-09-03 全量 visual 目录级运行中 dashboards/reports 全部通过）、不影响 18 域 CRUD 列表/表单（自含数据管线）

## 症状（2026-09-03 全量 visual 目录级运行实测，63 失败/225 中归本家族 12）

1. `/prj-task-kanban`、`/cs-ticket-kanban`、`/crm-opportunity-kanban`：看板组件**整体缺席**（`[data-slot="kanban"]` 0 个，20s waitForSelector 超时）——后端 `ErpPrjTask__findBoardData` 返回 200 且数据完整（4 列含卡片）。
2. `/crm-activity-timeline`、`/cs-action-log`、`/crm-activity-calendar`、`/hr-team-vacation-calendar`：页面 mount 期抛 console error `Expression evaluation failed for: ${(rawData.items ?? [])...}` / `${"共 " + (rawData.total ?? 0)...}`，错误框（retry UI）**持久锁存**，`暂无数据`；data-source ajax 200 后亦不恢复。
3. `f16-complex-pages` 等：`Template evaluation failed for: <div ...>${plannedQuantity ? ...}...</div>`——行作用域变量未定义时 tpl 模板求值抛错。

## 根因证据链（2026-09-03 运行时探针 + 包体逐区域 diff）

1. **后端无责**：`ErpCrmActivity__findPage` / `ErpPrjTask__findBoardData` 均 200，响应体完整（`{"data":{"total":2,"items":[...]},"status":0}`；board 数据 4 列）。runner 日志 `nop.graphql.end-rpc-request errorCode=null`。
2. **时序证明「发布从未发生」**：console error +3138ms，ajax 响应 +3145ms（晚 7ms）；+11.2s 手动点击错误框 retry **再次抛错** → `rawData` 在数据到达 8s 后仍不在 scope → ajax data-source 的**scope 发布环节失效**（非时序竞态、非守卫缺失单因）。
3. **对照实验锁定边界**：`/hr-org-chart` 页（同 `type: data-source` + `dependsOn` + ajax 结构）**正常工作**——其消费公式为未定义安全式 `${deptTree && deptTree.length > 0}`（短路，无未定义成员访问），树正常渲染 → data-source→scope 通路在该页可用；区别在消费公式是否对未发布变量做成员访问。
4. **公式求值器语义**：`flux-formula` evaluator 对非 optional 成员访问目标为 undefined 时**抛错**（`Cannot access member of null or undefined`；`onUndefinedVariable` 仅为旁路告警不拦截）→ mount 期（data-source 经 useEffect 注册，首次渲染必然先于发布）任何未护栏 `${x.y}` 公式必抛。
5. **公式型 data-source 级联死亡**：`createFormulaDataSourceController.start()` 于 microtask 求值公式 → throw → `onDependenciesChange?.(void 0)` **清空依赖订阅** → `rawData` 后续发布也不再触发重算 → `timelineItems` 永不发布（与探针 items=1 仅根节点一致）。
6. **包体归因**：served `pkg-nop-chaos-flux-BTOy7WaA.js` 含 `enclosing picker context`（新 tgz 特有字符串）→ 当前 runner 服务的是 2026-09-03 08:42 全链重建（`repack-flux-and-refresh.sh`）从 nop-chaos-flux HEAD `44ef5188f` 重打的 `libs/nop-chaos-flux-0.1.0.tgz`（未提交工作树变更）。
7. **逐区域 diff 未收敛到单点**：old tgz（HEAD `63899d6` 内 08-29 工件，M1.5 期绿色）vs new tgz 的 `//#region` 级 diff 共 90 处变更区域；`compileDataSource`/node-compiler/source-registry/evaluator/async-data controller/api-cache/request-runtime（仅符号重命名）/data-source-renderer/node-renderer/node-error-boundary **逐位相同**；变更集中于 picker v3.3/v3.4（`case "pick"`、PickerRuntimeContext 接线）、ApiResponse ok-removal（`status: 200`→`status: 0`，static-eval/defaults）、table optionRow/group 增强、page.tsx breadcrumb/extra/remark、按钮语义变体、validation url/integer/format。**静态 diff 无法唯一归因**——推荐 successor 首步做动态 bisect：以 old tgz 换入重建（`rebuild-flux-chain.sh --skip-flux` 前换回 `git show HEAD:libs/nop-chaos-flux-0.1.0.tgz`）验证载体，再在 flux 提交序列（08-29→`44ef5188f`，含 `9a21ad932` kanban 列头聚合、`18b70ec91` 按钮变体、`646d16ba4` BASE_SCHEMA_FIELDS、picker 双提交）内二分。
8. **看板缺席单列**：kanban 消费 `data: ${boardData}`（非成员访问，mount 期不抛），组件缺席指向看板渲染器本体或 boardData 发布——flux 08-31 `9a21ad932`（kanban 列头聚合）为嫌疑提交，successor 单独核实。

## 与已登记预存红灯的关系（全量 63 失败分账）

- **Family A（已登记预存，5）**：`ext-domains-child-table` ×5 = `docs/bugs/2026-09-02-ext-domains-child-table-amis-legacy-selector-flux-preexisting-red.md`（`.cxd-Crud` AMIS 遗留选择器），本批失败集与登记基线 IDENTICAL。
- **Family B（AMIS 遗留选择器/渲染同族未登记面，~34）**：f12 page-structure ×8、tree-entity-views ×8、status-tag ×8、sensitive-operation ×4、gl-mapping-rule ×2、field-format ×3（8 位小数格式未生效）、sensitive-masking ×1——同一 `.cxd-*`/AMIS 渲染契约家族，与 A 同根因（flip-orm-to-flux 全域翻转后 AMIS 时代 DOM 断言失配），**早于 M2.1 存在**、非 flux 运行时回归，归 flux 迁移/e2e 基建 owner 域（沿 A 的 Follow-up 归属）。
- **Family C（本 bug，12）**：f13 ×7 + crud-pages R41（/crm-activity-timeline）×1 + f16 ×4。
- **Family D（零散需个案，~12）**：_exploration 严格 feasibility ×7（`waitForResponse` 30s 超时，非像素漂移）、party-search-picker ×2（GraphQL `非法的字符`）、list-query-filter ×1、f16 其余—个案归因归 successor/owner 域。
- **M2.1 变更零贡献**：本计划改动面 = `crud-pages.snapshot.spec.ts`（新增）+ `CrudListPage.clickEdit`（仅新 spec 消费）+ R43 基线 + material-customs findPage 修复（使 2 用例由红转绿）——不触达上述任何失败面。

## 修复方向（successor；保护区流程）

1. **动态 bisect 定位提交**（首选首步）：old-tgz 换入重建复现实验（见 §7）确认载体后，在 flux 08-29→`44ef5188f` 提交区间二分重打 tgz + `rebuild-flux-chain.sh` + f13/crud-pages 探针。
2. 机制层候选修复面（以 bisect 结论为准）：渲染期公式求值对「未发布 data-source 名」的失败路径应可恢复（依赖订阅保留/延迟求值/`onDependenciesChange` 不因求值失败清空）；或公式型 data-source start 失败后保留依赖记录待 scope 变更重算；看板渲染器单独核实 `9a21ad932`。
3. 门禁补强（本仓可做）：f13/crud-pages 即守卫——任何 flux 重打 tgz + 全链重建的收口必须跑 `tests/e2e/visual/f13-non-standard-views.visual.spec.ts` + `crud-pages.snapshot.spec.ts`（fixtures console 守卫链路），不得只跑 dashboards/reports 子集（M1.5「50/50」口径漏洞）。
4. 修复落地区域 = nop-chaos-flux（源码）→ `repack-flux-and-refresh.sh` 重打 tgz → 全链重建，全程按保护区 `auto + dual-agent-approval`（跨仓库 plan + 双独立子 agent 批准）。

## 2026-09-06 回写（修复计划 Phase 3 归因勘误 + 实证）

**归因勘误（修正本 note §根因证据链 6-8 的 bisect 方向）**：

1. **修正 1——tgz 不是载体**：git-HEAD 内 libs tgz 与 flux master `44ef5188f` 现-build 产物逐字节相同（cmp 实证），且该 tgz（vintage ≈08-27~08-30）同样复现 timeline 抛错（vite dev A/B 实测）。§7 观察到的「新旧 90 区域 diff」实为 git-HEAD tgz 与 09-02 临时 sync 产物的对比；§7 的动态 bisect 建议作废。
2. **修正 2——前端组合不改变结果**：git worktree 复原历史组合（next@`61f5dc2`+flux@`3912109e9` / next@`61f5dc2`+flux@master / next@HEAD+flux@`08-04`）全部复现 timeline 2/2 node-error → 失败与「哪天引入」无关，系目标页对「data-source 未发布期」的容忍缺陷从未在真实验证中暴露（f13 E2E 当年绿证据不覆盖本缺陷面）。§8 kanban 缺席归因 `9a21ad932` 作废。
3. **修正 3——kanban/calendar/gantt 渲染器从未进 bundle（产品缺口非回归）**：`registerDefaultFluxRenderers` 只注册六包，`flux-renderers-scheduling` 从未在列 → ERP authored `type: kanban/calendar` 静默不渲染。已在修复计划 F4 中将 scheduling 入 bundle 默认注册面（含 styles.css @import）。

**修复内容**（plan 2026-09-03-1815-1 Phase 2，flux 工作树，未提交）：F1 `evaluateLeaf` 抛出路径依赖落盘（finally）+ F2 公式 data-source publish sentinel 容忍（pending + 依赖保留，不 reportPublishFailure）+ F3 `resolveNodeProps` sentinel 引用稳定回退（含 **meta 表达式对称 catch 增量登记**：when/visible/className 同经 `evaluateCompiledValue`、mount 期同样命中 sentinel → sentinel→`undefined` 回退 + 依赖保持，plan Non-Goal 预留条件触发后按约定增量落地，2026-09-06 结束审计补录）+ F4 scheduling 渲染器入 bundle（新增单测共 9 个）。语义沿 `evaluateControllerStopCondition` 既有 sentinel 先例，非新造。

**ERP 实证（2026-09-06，fresh-DB runner + 重打 tgz 全链）**：

- f13 全 spec **8/8 绿**（timeline ×2 + kanban ×3 + calendar ×2 + org-chart 对照 ×1；timeline 页零 console error 探针在案）。
- `crud-pages.snapshot.spec.ts` **69/69 绿**（R41 /crm-activity-timeline 转绿；零基线漂移）。
- f16 目录级 **11 passed / 1 failed / 2 skipped**，逐例归因：
  - Family C（本根因，tpl 求值抛错）目标面：mfg work-order 进度 tab 实测 dialog 打开、**零 console error**、无错误框锁存——抛错症状已消除。
  - `finance ErpFinVoucher edit drawer ... autoBalance buttons` failed：**非本根因**——autoBalance 按钮在 `751749e17`（08-31 flux 编译清理，plan 2026-08-30-2238-1）中被移除而 f16 spec 仍断言，spec/view 漂移，归 ERP e2e 基建 owner 域处置（修 spec 或恢复按钮）。
  - `mfg ErpMfgWorkOrder view drawer` + `quality ErpQaNonConformance view drawer` skipped：**非本根因**——种子行在案（WO-2026-001..004），skip 系 helper `openFirstRowDrawer` 的 `[data-slot="drawer-surface"]` 定位器与现行 dialog 型 view 弹层（`dialog-*` slots）失配，归 f16 spec 基建域。
  - 残留（successor）：work-order 工单进度仪表板 custom text cell（HTML-string tpl）内容渲染为空（无报错）——页面写法/控件语义归后续规范文档决策（本修复计划 Non-Goal，见其 Non-Goals「ERP 页面公式改写」条）。
