# flux 运行时 data-source/公式渲染回归——看板空渲染、时间线/日历错误框、模板公式 mount 期抛错

- 日期：2026-09-03
- 发现于：plan `2026-09-03-0938-1-dual-amis-office-viewer-peer-alignment` Phase 2 全量 visual 目录级验证（双 amis pageerror 消除后首次暴露）；同日 M2.1 计划 `2026-09-03-0400-1` Phase 2 批次 C 复现
- 状态：fixed（未提交，工作树；跨仓库保护区 = nop-chaos-flux/nop-chaos-next `auto + dual-agent-approval`；待 successor 双独立子 agent 批准后提交）
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
