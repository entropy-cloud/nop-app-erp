# 2026-08-03 Flux CRUD 渲染迁移验证证据

> 关联计划：`docs/plans/2026-08-03-1232-1-flux-crud-migration.md`
> 用途：沉淀 Phase 0/1 的程序化验证证据（flux JSON 输出合法性 + AMIS↔flux 差异分类 + picker/onEvent 裁决），供计划检查清单与结束审计引用。浏览器层视觉/E2E 证据归 Phase 2（依赖运行时服务器 + 浏览器）。

## 1. flux-web.xlib 上游修复（nop-entropy 变更）

flux 模式下 ERP 全量页初次扫描报 244 个 `nop.err.*`，根因均为 nop-web `flux-web.xlib` 缺陷（非 ERP 侧），已修复并回归至 0：

| 缺陷 | 错误码 | 影响面 | 根因 | 修复 |
| --- | --- | --- | --- | --- |
| GenFormCell 缺 proxyCell 守卫 | `nop.err.xlang.exec.unknown-prop`（`formCell.type` on `ProxyCell`） | 235 页 | flux `GenFormCell` 首分支直接访问 `formCell.type`；AMIS `web.xlib:GenFormCell`(:333) 以 `formCell.proxyCell` 先行短路，flux 版遗漏 | flux-web.xlib GenFormCell 增补 `<when test="${formCell.proxyCell}">` 分支 |
| NormalizeAction 未处理 cancel | `nop.err.web.unsupported-action-type`（`actionType=cancel`） | 3 ERP 页 + 9 nop-wf 页 | flux NormalizeAction actionType 分支表无 `cancel`；AMIS 为 passthrough（运行时处理），flux 为变换模型需显式映射 | NormalizeAction `close` 分支扩为 `close \|\| cancel` → `closeSurface` |

修复记录：`nop-entropy/ai-dev/logs/2026-08-03.md`。零 Java/API/契约变更。

## 2. 全量 flux JSON 输出验证（Phase 1 主项）

测试：`app-erp-all/src/test/java/io/nop/app/all/web/ErpAllFluxPagesTest.java`——`nop.web.render-mode=flux` 下经 `PageProvider.getPage` 逐页加载全部启用模块的 `pages/*/*.page.yaml`，断言 0 个 `nop.err.*`。

- 命令：`mvn test -pl app-erp-all -Dtest=ErpAllFluxPagesTest`
- 结果：**FLUX_PAGE_ERROR_COUNT: 0**（Tests run: 1, Failures: 0, Errors: 0，24.6s）
- 覆盖范围（与计划基线对账）：
  - 354 标准 CRUD main.page.yaml（x:gen-extends web:GenPage）：0 错误 ✅
  - 352 picker.page.yaml：0 错误（page_picker.xpl 输出合法）✅
  - 38 ref-*.page.yaml（drawer 引用页，含 3 死文件）：0 错误 ✅
  - 15 layoutControl="tabs" + 3 多页 tabs 先例（ErpAstAsset/ErpMntEquipment/ErpHrEmployee）：0 错误 ✅
  - 24 onEvent 视图：0 错误 ✅
- AMIS 对照：`ErpAllWebPagesTest`（validateAllPages）同样 0 错误——双模式基线一致。

29 个手写 AMIS main.page.yaml（10 dashboard + cs 绩效 + 2 wizard + 15 占位/特殊）在 flux 模式下亦 0 错误加载（无 web:GenPage 不受 render-mode 影响，输出 AMIS JSON 属预期，豁免至 P2-P4）。

## 3. AMIS↔flux 差异分类（Phase 0，ErpMdMaterial 实体）

测试：`ErpFluxDiffDemoTest` 双模式生成 ErpMdMaterial main 页 JSON（落盘 `/tmp/erp-md-material.{amis,flux}.json`）。

| 维度 | AMIS | FLUX | 分类 |
| --- | --- | --- | --- |
| 顶层 | `type=page` | `type=page` | 等价 |
| body.type | `crud` | `crud` | 等价 |
| columns | 29 列，键集一致（fixed/label/name/sortable/toggled/type） | 29 列，键集一致 | 等价（1:1） |
| body 专有键 | api, filter, bulkActions, headerToolbar, itemActions, labelTpl, syncLocation, autoFillHeight, footable | id, loadAction, queryForm, toolbar | 等价变换（AMIS 扁平属性 → flux 结构化 action/form 模型：api→loadAction, filter→queryForm, headerToolbar/bulkActions→toolbar） |
| 共有键 | columns, footerToolbar, name, type | columns, footerToolbar, name, type | 等价 |

结论：结构等价变换，无缺失能力（列定义/类型完整保留）；语义等价（filter 算子/action 行为）需浏览器层验证（Phase 2）。

## 4. onEvent 等价性裁决（推翻计划基线「onEvent 丢失」前提）

计划基线断言「NormalizeAction(:759) 删除 onEvent——24 视图联动丢失」。实测 ErpMdMaterial flux 输出 onEvent 计数 AMIS=6 / FLUX=5，逐项定位：

- 5 个**表单字段级 onEvent**（input-text `code` blur→ajax、switch `status` change→dialog）在 flux 输出中**全部保留**，仅路径从 `dialog.body...` 迁至 `onClick.args.body...`（flux action 模型包裹）。
- 1 个**按钮级 onEvent**（`type=action`）在 flux 中被 NormalizeAction 转换为 `onClick` action 模型——属**正确变换**（按钮交互从 AMIS onEvent 改用 flux onClick），非丢失。

裁决：表单字段级 onEvent（gen-control 内联 JSON 经 eval 透传）在 flux JSON 中保留；按钮级 onEvent 转换为 flux onClick。残留风险：onEvent 内层 `actions` 仍引用 AMIS actionType（ajax/dialog），flux **前端运行时**能否执行该 AMIS 风格事件动作链属 Phase 2 浏览器验证范畴（JSON 输出层面已等价）。逐页清单（24 实名）见计划 Phase 1 item 4 已枚举核对一致。

## 5. picker 归属裁决（Phase 0 Decision）

机制实测：
- 后端：`flux-web/impl_GenPage.xpl` picker 分支 → `page_picker.xpl` 输出页面级 `<picker><grid/></picker>` JSON。352 个 picker.page.yaml 在 flux 模式 100% 输出合法 JSON（§2）。
- 前端：nop-chaos-flux 缺页面级 picker 渲染器（仅有表单字段级 `type:'picker'` 需 name/pickerDialog）。

裁决：**方案 B（表单字段级 picker）为本计划范围内的可执行归属**——后端页面级 picker JSON 已就绪且合法，但全量菜单翻转受限于前端渲染器缺口；本计划 CRUD 基线（354 main + 352 picker JSON 输出）已验证通过，前端渲染器扩展（方案 A）归 nop-chaos-flux 仓库 successor。混合期 picker 可保持 AMIS 渲染（方案 C，双引擎最小面）直至前端渲染器就绪。残留风险：picker 弹窗的浏览器层交互（打开/搜索/回填）依赖 Phase 2 验证或前端渲染器扩展。

## 6. 待 Phase 2 浏览器层验证项（依赖运行时服务器 + 浏览器）

- flux JSON → nop-chaos-flux 浏览器渲染视觉等价（需要启动 app + 浏览器）
- 菜单 component 翻转后 RouteRenderer→FluxRouteEntry 端到端可达
- `E2E_ENGINE=flux npx playwright test` 全量回归（需运行服务器）
- onEvent 内层 AMIS actionType 在 flux 前端运行时的执行等价

## 7. Phase 2 浏览器层执行尝试与阻塞根因（2026-08-03 实测）

Phase 2 浏览器层验证经实际启动服务器 + Playwright 执行尝试，确认被 **nop-web-site 前端 bundle 打包缺陷**阻塞（跨仓库，不可在本仓库修复）。

### 7.1 服务端 flux JSON 输出（✅ 复验通过）

启动 `app-erp-all`（`-Dnop.web.render-mode=flux`，端口 8011），经 `LoginApi__login` + `PageProvider__getPage` 直连 RPC 复验：

| 页面 | status | body.type | columns | loadAction | queryForm | errors |
| --- | --- | --- | --- | --- | --- | --- |
| `/erp/md/pages/ErpMdMaterial/main.page.yaml` | 0 | crud | 29 | true | true | null |

`SiteMapApi__getSiteMap`：全局 flux 模式下 **678 资源 component=FLUX，0 AMIS**（证实 Phase 3 Decision 第 2 条——全局 flux 会使全部路由报为 FLUX，含 29 手写 AMIS 页）。

结论：服务端 flux 渲染管线（flux-web.xlib + page_picker.xpl + complex 分派）端到端可达且 0 错误，Phase 0/1 程序化证据在运行时复验成立。

### 7.2 浏览器层渲染（❌ 阻塞——nop-web-site bundle `require("react")` 致命错误）

`E2E_ENGINE=flux BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/crud/master-data.smoke.spec.ts`（DOM 级，经 FluxAdapter）失败。根因（非 FluxAdapter 选择器不匹配，而是前端运行时致命崩溃）：

```
Error: Calling `require` for "react" in an environment that doesn't expose the `require` function.
    at http://127.0.0.1:8011/assets/rolldown-runtime-DAXXjFlN.js:1:1027
    at http://127.0.0.1:8011/assets/pkg-nop-chaos-flux-eL48GOg2.js:100:22095
    ...
    at http://127.0.0.1:8011/assets/host-flux-runtime-qwpQwETd.js:2:5209
    at http://127.0.0.1:8011/assets/index-NICKLKZp.js:2:2256
```

机制：nop-web-site 的 `index-*.js`（SPA 入口）**eager-load** `host-flux-runtime` → `pkg-nop-chaos-flux`；该 bundle 经 rolldown 打包后含 CJS `require("react")` 调用，但浏览器环境不暴露 `require` → 模块初始化抛错 → **SPA 路由渲染器（FluxRouteEntry 与 AMIS 渲染器均）无法初始化**。

关键观察（致命性 + 双引擎影响）：

- **AMIS 模式同样失败**：重启服务器为默认 amis 模式（无 `-Dnop.web.render-mode`）后同一 smoke 测试同样失败、同一 `require("react")` 错误——证明该错误是 nop-web-site bundle 的**预存打包缺陷**（flux 运行时 bundle 被静态嵌入站点 assets，与 render-mode 无关，eager-load 即崩），非本计划引入、非 flux 路由专属。
- **页面快照**：SPA 导航外壳（sidebar「Master Data / Material」菜单）可渲染，但主内容区为空——路由渲染器崩溃致页面内容永不挂载（`[data-slot="crud-table"]` / `.cxd-Crud` 均不在 DOM）。
- **绿基线对比**：`docs/testing/known-good-baselines.md` 最近一次全套件 E2E 全绿为 2026-07-16（405 passed，`nop-web-site` 尚未嵌入 flux 运行时 bundle）；本日（2026-08-03）flux 运行时 bundle 引入后浏览器层全量回归不再可达。

### 7.3 阻塞裁决

| 阻塞项 | 归属 | 可否本仓库修复 |
| --- | --- | --- |
| nop-web-site bundle `require("react")` CJS 互错 | nop-web-site / nop-chaos-flux 打包 | 否（跨仓库前端构建产物） |
| 页面级 picker 前端渲染器缺口 | nop-chaos-flux | 否（跨仓库） |
| 29 手写 AMIS 页未重写 | 计划 1232-2/3/4（均 `planned`） | 否（跨计划依赖） |

Phase 2 全部 item（菜单翻转 / 全量 E2E / FluxAdapter 修复 / flux-web 缺陷 / 基线记录）均依赖浏览器层可达性，故全部保持 `[ ]`。服务端 flux JSON 输出基线（§7.1，0 错误）作为本计划可达的最深证据层级；浏览器层等价归 nop-web-site bundle 修复后的 successor 执行。

### 7.4 successor 触发条件

Phase 2 解除阻塞需同时满足：
1. nop-web-site 重新打包修复 `require("react")` CJS 互错（flux 运行时 bundle 可在浏览器初始化）
2. nop-chaos-flux 实现页面级 picker 前端渲染器（或本计划 picker 方案 B/C 在浏览器层验证）
3. 计划 1232-2/3/4 完成（29 手写 AMIS 页 flux 重写）

满足后方可执行菜单按域翻转 + `E2E_ENGINE=flux` 全量回归。
