# Flux 集成已知设计陷阱（Gotchas Catalog）

> 维护目的：记录 nop-app-erp 与 nop-chaos-flux 集成过程中发现的"看起来是 bug 实则是设计怪味"的坑，避免后续开发者重复踩坑。
>
> 修订规则：每条目含「症状 / 根因 / 现行 workaround / 推荐修复（待上游）」四段；推荐修复需要跨仓库 plan + 双 agent 批准，不可直接修改。

## G-001 `CrudQueryFormConfig.mode` 不控制 label 位置（✅ 已修复，2026-08-25）

> **状态**：上游修复已落地（nop-chaos-flux `ce5e53145`），workaround 已移除（nop-entropy `836a4c4cf5`），`mode` 单字段即可控制 label 位置。下文保留原始诊断记录供追溯。

### 症状
开发者在 `<queryForm mode="horizontal">` 设置后，发现表单 label 仍渲染在控件上方（`label-top`），不是同行右对齐（`label-left`），以为是 bug。

### 根因（已定位）
- `nop-chaos-flux/.../crud-schema.ts` 的 `CrudQueryFormConfig.mode` 字段类型是 `'manual' | 'auto'`，**语义是 autoGenerateQueryFilter 的生成模式，与 label 位置无关**。
- `nop-chaos-flux/.../data-schema-validation.ts:23` 才是真正决定渲染表单 mode 的地方：
  ```js
  mode: queryForm.layout === 'horizontal' ? 'horizontal' : 'normal',
  ```
  它读的是 `queryForm.layout` 不是 `queryForm.mode`。
- 两个 `mode` 字段同名但语义不同——这是设计怪味，不是 bug。

### 现行 workaround（已于 2026-08-25 移除）

> 上游修复落地后，workaround 不再需要。`grid_crud.xpl` 现在只设 `mode`（校验器直读 `queryForm.mode`，`layout` 回退仍兼容）。历史 workaround 形态（2026-08-24 ~ 2026-08-25）：

```xml
<queryForm xpl:if="filterForm" id="${crudName}-query-form"
           layout="${filterForm.layoutMode || 'horizontal'}"     <!-- 校验器读这个 -->
           mode="${filterForm.layoutMode || 'horizontal'}"        <!-- 下游消费用，传到渲染 form -->
           labelWidth="..."
           wrapClassName="m-b-nm"
           actionsClassName="...">
```
两者必须同时设置，缺一不生效。（仅历史参考，勿再用。）

### 推荐修复（已落地，nop-chaos-flux 上游）
**最小改动方案**：
1. `CrudQueryFormConfig.mode` 的 docstring 明确写「此 mode 控制 autoGenerateQueryFilter 自动生成，与 label 位置无关；label 位置请使用 layout 字段」。
2. 渲染层同时读取 `queryForm.layout` 和 `queryForm.mode`，让 `mode` 在 queryForm 语境下也接受 `'horizontal'|'normal'|'inline'`（与渲染 form.mode 同语义）。
3. 修订 schema 文档。

**推进路径**：跨仓库改动 → 须按 AGENTS.md「跨仓库原则」开 plan → 双独立子 agent 批准 → nop-chaos-flux 维护者评审合并。短期不阻塞 nop-app-erp。

### 关联代码
- 触发器：`/nop-entropy/nop-frontend-support/nop-web/.../flux-web/grid_crud.xpl`
- 上游校验：`/nop-chaos-flux/packages/flux-renderers-data/src/data-schema-validation.ts:23`
- 上游 schema：`/nop-chaos-flux/packages/flux-renderers-data/src/crud-schema.ts:10-31`

### 修复落地状态
- 2026-08-24：在 `grid_crud.xpl` 落 workaround（同时设 `layout + mode`）+ 本 gotcha 入档。
- 2026-08-25：上游修复落地（nop-chaos-flux `ce5e53145`：`CrudQueryFormConfig.mode` 类型扩展为并集 + `data-schema-validation.ts` 新增 `resolveFormMode()`，`mode` 优先、`layout` 回退、legacy `'manual'|'auto'` 不受影响；回归测试 `crud-query-form-mode-resolution.test.ts` 11 case 全绿，包级 882/882 全绿）。同批 `grid_crud.xpl` 移除 workaround（nop-entropy `836a4c4cf5`，只设 `mode`）。经 `rebuild-flux-chain.sh` 全链路发布后浏览器层视觉验证：CRUD 列表 queryForm label 同行左置（`01-crud-voucher`/`02-crud-purchase` PNG 确认）；`mvn test -pl app-erp-all` 54/0/0/1 与 B10 基线零漂移。执行计划：`docs/plans/2026-08-24-2115-1-fix-flux-crud-queryform-mode-semantics.md`。

---

## G-002 手写复杂页 AMIS 风格接线（actionType/api/onEvent/dialog）在 flux 运行期惰性（2026-10-05）

### 症状
手写 `*.page.yaml`（整页直写、无 `x:gen-extends` 的 complex 路径页）中按 AMIS 习惯写的按钮不工作：点击 `actionType: ajax` + `api` 按钮无任何网络请求、无报错、无反馈；`actionType: dialog` 按钮点击不弹窗；`onEvent: after-success` 刷新链不执行；`visibleOn`/`disabledOn` 条件显隐/禁用永不生效。导出 JSON 中这些属性原样保留（`actionType`/`api`/`onEvent`/`visibleOn`/`disabledOn`），flux 门禁报 unknown-property warning，但页面渲染正常——静默惰性，极难察觉。

### 根因
flux button renderer 仅声明 `onClick` 一个事件字段（`nop-chaos-flux/packages/flux-renderers-basic/src/basic-renderer-definitions.ts` button `fields`），AMIS 风格属性不是声明字段 → 编译期分类 unknown 丢弃，不生成 eventPlan（`flux-compiler/src/schema-compiler/node-compiler.ts:171` 仅 `kind === 'event'` 进 `rawEventPlans`）→ `button.tsx:227` 的 `handleClick` 执行 `props.events.onClick?.()` 为空。AMIS→flux 的唯一转换点 `page_simple.xpl`（nop-web）只服务 view.xml GenPage 路径，手写复杂页不经它。迁移指南 `flux-guide/09-amis-migration.md` 的转换表（ajax→`{action:'ajax', confirmText, args}`、dialog→openDialog、onEvent→then 链、visibleOn→visible、disabledOn→disabled）须由**页面作者或生成器**执行。

### 现行 workaround
手写页一律用 flux 原生 Action Algebra（本仓 6 个手写复杂页〔8 个页面文件，含 2 页 flux.yaml 孪生〕的**被服务内容**已于 2026-10-05 全量迁移，plan `2026-10-05-0400-1`；其中 period-close-wizard 与 visit-wizard 的被服务文件是同名 `main.flux.yaml` 孪生——见下方「孪生遮蔽」——两页的 page.yaml 为非服务 legacy，已同步改写保持一致）：

```yaml
- type: button
  label: "提交"
  onClick:
    action: ajax
    confirmText: "确认？"            # args 的兄弟键（运行时读 action.source.confirmText，放 args 内被静默忽略）
    args:
      url: '/r/Entity__action'       # 标量参数自定义 mutation：走 /r/ 直发（见下「URL 形态规则」）
      method: post
      data: {...}
    then:                            # 原 onEvent.after-success 刷新链
      - action: refreshSource
        targetId: someData
# actionType: dialog/dialog:{type: drawer} → onClick:{action:'openDialog'|'openDrawer', args:{title,size,body,...}}
# visibleOn/disabledOn → visible/disabled（表达式字符串）
```

**URL 形态规则（壳层 nopRpcResolver 语义，plan 2026-10-05-0400-1 运行时实锤）**：
- `@mutation:Xxx__save|update|...`（operationRegistry 注册后缀）：壳层经 argDataMap 发 `{data: <args.data>}`——args.data 写实体字段平铺即可。
- 标量参数自定义 mutation（如 `startTake(takeId)`）：**必须写 `/r/Xxx__method` 直发**——`@mutation:` 未注册回退会包一层 `{data: ...}`，服务端报 `unknown-operation-arg: 没有定义参数[data]`（`nop-chaos-next/apps/main/src/services/nopRpcResolver.ts:274-277`）。
- `@query:` 未注册操作不包裹，args.data 原样直达。
- **事件处理器路径同律**（plan 2026-10-06-0355-2）：kanban `onCardMove` 等事件处理器的 ajax args 携带 `@mutation:` 标量参数 URL 同样必败（G-002 陷阱不限于按钮 onClick）——三看板拖拽改状态链曾整族失效，已按 `/r/` 直发修复。附带纪律：mutation `data` 载荷内不得混入 `i18nEn` 杂键（会作为参数发出；中文 payload 值非 UI 文案，走 cjk-baseline.md WHITELIST 文件级豁免通道）。

注意：迁移后 confirm 依赖宿主 `env.confirm` 能力（`flux-runtime/src/runtime-action-helpers.ts:142-147`——配置 confirmText 而宿主未提供 env.confirm 时动作显式失败）。本仓宿主已接 env.confirm（2026-10-05 stock-take-flow 确认框实测弹出）。

### hybrid 半迁移形态（plan 2026-10-06-0355-1 实证，勿再引入）
`actionType: ajax` + `action: ajax` + `args:` + `target:` **平铺按钮级**（改了 AMIS 键名、未加 onClick 包裹）——四个键全部 unknown-property、按钮惰性，是「以为已迁移」的高危错觉形态（门禁 warning 与纯 AMIS 形态同族，看板/日历/时间线/甘特的刷新按钮曾整批中招）。刷新类按钮的正确转换是 `onClick: {action: refreshSource, targetId: <同名数据源>}`（数据源 args 才是权威参数面，勿在按钮级复制查询参数）；一次性请求才用 `onClick:{action:'ajax', args}`。

### download 动作转换行（09-amis-migration.md 缺行的仓内补充）
AMIS `actionType: download` + `api:{url,method,responseType,data}` → flux 内置 `download` 动作 Form 1（`flux-core/src/constants.ts:54`；`packages/flux-runtime/src/runtime-action-helpers.ts:337-388`，Form 1 分发 :373-388，responseType 强制 blob）：

```yaml
- type: button
  label: "下载 XLSX"
  onClick:
    action: download
    args:
      api:                  # 与页面 data-source 的 api 对象同构直迁
        url: /p/Entity__download
        method: post
        responseType: blob
        data: {...}
```

本仓 25 个报表页 ×2=50 个下载按钮已按此形态迁移（2026-10-06，运行时证明=`tests/e2e/reports/reports.amis-download.spec.ts` 真实点击 ×10）。

### 推荐修复（待 nop-chaos-flux）
验证器 prop 表把 `i18nEn` 等宿主层消费的属性纳入 known 列表，同时对手写路径上出现 `actionType`/`api`/`onEvent` 升级为 **error 级诊断**（当前 warning 淹没在生成页 i18nEn 误报中）。外部仓改动走保护区双批准。

### 关联代码
- `nop-chaos-flux/packages/flux-renderers-basic/src/button.tsx:227`（handleClick 仅执行 events.onClick）
- `nop-chaos-flux/packages/flux-compiler/src/schema-compiler/node-compiler.ts:171`（event 字段分类）
- `nop-entropy/nop-frontend-support/nop-web/.../flux-web/page_simple.xpl:15-28`（唯一 AMIS 转换点，仅 GenPage 路径）
- 本仓迁移范围：disposal-wizard / lead-conversion / period-close-wizard / stock-take-flow / visit-wizard / ErpSysNotification-inbox（pda-stock-take 随 plan `2026-10-05-0400-2` 重构迁移）
- 运行时证据：`_tmp/2026-10-05-0400-1-probe/`（服务端日志零请求实锤 + 迁移后复验）

### /p/ 二进制端点的 blob 透传规则（plan 2026-10-06-0508-1 修复）
flux download 动作 Form 1 契约 = 宿主 fetcher 返回 `{status:0, data:Blob, headers:{content-disposition}}`（`flux-runtime/src/runtime-action-helpers.ts:373-388`）。壳层 `nopRpcRequest` 的**非 @query:/@mutation:/r/ 透传分支曾丢弃 responseType**（nop-chaos-next http.ts，nop-chaos-next `34fb37d` 修复）：`/p/{Entity}__download`（报表二进制唯一端点——RPC 层会 JSON 序列化 WebContentBean，不可用 @query:/@mutation:/r/ 替代）响应被按 JSON 解析，Blob 管线永不触发 → 下载静默失败。修复后语义：2xx 门禁前置 + JSON-in-blob 错误恢复（错误 envelope 不作成功 Blob 返回）+ 2xx Blob 原样返回。注意：normalizeBlobData 自动下载语义仅属 @query:/@mutation: 分支；download 动作 api.url 必须写 `/p/...` 直连形态。附带效应：barcode-input WASM（经 fetcher 拉 blob）由坏转好。

### select 动态选项源形态规则（plan 2026-10-05-0400-6 spike 实证）

select 渲染器**只读 `props.options`**（`input.tsx:520` 唯一 allowSource 键；`input-choice-renderers.tsx:109-113`），动态选项的正确形态：

```yaml
- type: select
  name: periodId
  options:
    type: source                 # 必须（isSourceSchema 判定，缺 type 不发请求）
    action: ajax
    args:
      url: '@query:Entity__findList'   # 数组根（findPage 的 PageBean 非数组被 sanitize 丢弃）
      method: post
      selection: value:id label:code   # GraphQL 别名产出 sanitize 必需的 {label,value} 键形
```

- 表达式形态亦可：`options: '${(x?.items ?? []).map((i) => ({ label: i.name, value: i.id }))}'`（配合独立 data-source；**YAML 裸标量含 `: ` 必须单引号包裹**——裸写触发 mapping-values 解析失败）。
- `source:` 键为死 prop（嵌套扫描会发请求但值写回 props.source 无消费）；valueField/labelField 在 select 上不存在（属 input-tree 族），是 unknown-prop 惰性。
- **YAML 裸标量含 `: ` 解析破坏**（plan 0400-6 执行期发现）：`options: ${...{ label: a.code...}}` 未引号触发 `mapping values are not allowed here` 启动期页面加载失败——表达式含 `: ` 一律单引号包裹。

### 孪生遮蔽陷阱（本计划直接踩坑点）
`render-mode=flux` 下 `PageModelLoaderFactory` 对同名 `*.page.yaml`/`*.flux.yaml` **优先加载 flux.yaml**（`flux-page-export-and-validation.md:15`，本仓 31 个 flux.yaml）。改写 page.yaml 前必须先确认目标页是否存在 flux.yaml 孪生——存在则孪生才是被服务内容，page.yaml 改写对运行期无效，且 flux 门禁验证的也是孪生导出物（陈旧孪生会让「warning 计数持平」被误读为改动无效或既有族）。判别：`ls <page-dir>/`；门禁机械证明 = 修复后该页 unknown-property warning 中 `actionType/action/args` 族消失。

### 修复落地状态
- 2026-10-05：6 页（pda 随 plan `2026-10-05-0400-2`）被服务内容全量迁移（机械总量 23 ajax 按钮〔14 page.yaml + 2 孪生进入向导 + 3 孪生 start/complete/cancel + 4 孪生镜像〕、26 then 步骤〔19+7〕、6 dialog、19 visibleOn/disabledOn；period-close/visit 双文件一致），`npm run validate:flux` errors=325 零新增。pda 页归 plan `2026-10-05-0400-2`。
- 2026-10-06（plan `2026-10-06-0355-1`）：孪生页/报表页残留清零——17 个 hybrid 按钮（16 flux.yaml）→ `onClick:{action:'refreshSource',targetId}`、50 个 download 按钮（25 report 页）→ `onClick:{action:'download',args:{api}}`、level→variant 活面 21 处、select 死 labelField/valueField 富标签化 3 处（`type: tree` 上 labelField 为 graph 渲染器合法 prop，org-chart/bom-tree 两处豁免）；触及文件 wiring 族 warning 归零，errors=325 持平。余 28 条 wiring warning 属 view.xml delta/生成链页面对话框按钮（新登记独立 lane，修复前须运行时可达性证据）。

---

## G-003 refreshSource 跨 fragment scope 解析曾失败（exact-bucket 无父链回退）（2026-10-06，✅ 已修复）

### 症状
按钮（form 内）执行 `onClick/then: {action: refreshSource, targetId: <页面级数据源>}` 报 notify error `Source not found: <name>`，数据源零重发；E2E 探针实证（kanban 刷新按钮点击后 findBoardData 零请求，fluxDebug notify error 在案）。波及所有「子 scope 动作刷新页面级 source」站点——flux-guide 01/05 官方示例（form onSubmitSuccess 刷新页面级 pagedUsers）的规范形态，以及本仓第二轮 then:refreshSource 刷新链中跨 fragment scope 的站点（静默失效）。

### 根因
`source-registry.ts` 的 `refreshDataSource` 对传入 scope 仅查**该 scope 自身注册桶**（exact-bucket，无父链遍历；全局 nameIndex 仅在无 scope 调用时可达），而 action-adapter 总传按钮的 ctx.scope。对照：refreshNearest 有父链遍历，具名 refreshSource 反而无；guide data-source.md 曾把该限制作为已知约束记载（plan 513 L4.10）。

### 修复（nop-chaos-flux `b18117e97`，plan 2026-10-06-0555-1 双批准）
refreshDataSource 改为 scope 链最近优先遍历（同名就近遮蔽保持）+ 链上未命中回退全局 nameIndex；**isolate 边界截断遍历并抑制兜底**（与数据读取语义一致，隔离上下文不得刷新不可见链路上的 source）；guide 刷新姿势节改写为新语义。`findFirstInScope` 仍为本桶语义（refreshNearest 自行遍历），注释已显式分叉。

### 关联代码
- `nop-chaos-flux/packages/flux-runtime/src/async-data/source-registry.ts`（refreshDataSource / findFirstInScope）
- `nop-chaos-flux/packages/flux-runtime/src/action-adapter.ts:511-527`（refreshSource case，传 ctx.scope）
- app 仓验收：`tests/e2e/business-actions/amis-residual-refresh.action.spec.ts`（点击重发探针）

---

## 模板：新增 gotcha 条目

```markdown
## G-NNN <简短标题>

### 症状
<开发者会看到的现象>

### 根因
<具体文件 + 行号 + 解释>

### 现行 workaround
<代码片段>

### 推荐修复（待 <上游仓库>）
<最小改动 + 推进路径>

### 关联代码
<关键路径列表>

### 修复落地状态
<日期 + 落地点>
```