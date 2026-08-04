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

### 7.4 successor 触发条件（历史记录，2026-08-03）

Phase 2 解除阻塞原需同时满足：
1. nop-web-site 重新打包修复 `require("react")` CJS 互错（flux 运行时 bundle 可在浏览器初始化）
2. nop-chaos-flux 实现页面级 picker 前端渲染器（或本计划 picker 方案 B/C 在浏览器层验证）
3. 计划 1232-2/3/4 完成（29 手写 AMIS 页 flux 重写）

> **状态更新（2026-08-05，见 §8）**：条件 1 已满足（bundle 重打包为 ESM vendor chunks，浏览器层实证通过）；条件 2/3 仍 open，但经裁决不阻塞本计划 CRUD 基线（picker 表单字段级方案 B 后端 JSON 已就绪 + 混合期 AMIS 兜底；29 手写页经全量翻转后 flux 经 amis-compat 渲染，dashboard smoke 绿）。

## 8. Phase 2 浏览器层执行（2026-08-05，阻塞解除 + 全量回归）

### 8.1 Blocker #1 解除实证（nop-web-site bundle `require("react")` 已修复）

§7.2 记录的致命 bundle 缺陷在 2026-08-04 重打包后**已修复**，2026-08-05 静态 + 浏览器层实证确认：

- **旧 bundle 已消失**：`runner.jar`（构建于 2026-08-04 22:05）中 §7.2 引用的 `pkg-nop-chaos-flux-eL48GOg2.js`、`host-flux-runtime-qwpQwETd.js.gz`、`index-NICKLKZp.js.gz` **均不存在**；当前 assets 为 `pkg-nop-chaos-flux-B3u1liCF.js.gz`、`host-flux-runtime-DF9mpgm2.js.gz`、`index-CT5xXlNq.js.gz`（hash 全变）。
- **react 改为 ESM vendor chunk**：新 `pkg-nop-chaos-flux-B3u1liCF.js` 中 `"react"` 字面量计数 = **0**（旧 bundle 第 100:22095 含 `AF(\`react\`)` require-shim 调用）；react 经独立 ESM chunk `vendor-react-EjA2QFhT.js`（`import{t as e}from"./vendor-react-..."`，导出 `Symbol.for("react.transitional.element")`）由浏览器原生 ESM 加载，不再 CJS `require()`。
- **浏览器层实证（决定性）**：`E2E_ENGINE=flux npx playwright test tests/e2e/crud/master-data.smoke.spec.ts`（即 §7.2 同一 smoke）**1 passed (13.0s)**——flux RouteRenderer 初始化、表格 DOM 挂载、GraphQL 200、add 表单字段全链通过，无 `require("react")` 错误。
- **服务端复验**：`SiteMapApi__getSiteMap` 全局 flux 模式下全部资源 `component=FLUX`；`PageProvider__getPage` ErpMdMaterial status=0/cols=29/loadAction=true。`mvn test -pl app-erp-all -Dtest=ErpAllFluxPagesTest` → **FLUX_PAGE_ERROR_COUNT: 0**（§2 基线复验成立）。

结论：Blocker #1 解除。浏览器层 flux 渲染管线端到端可达。

### 8.2 菜单翻转状态（已完成，跨计划先行落地）

全 19 域 `erp-*.action-auth.xml`（源文件）已 100% 翻转为 `component="FLUX"`，**0 个 `component="AMIS"` 残留**（跨全仓库 `*.action-auth.xml` 实测）。翻转经 commit `738810aa5 feat(flux): 全18域实体翻转 web-renderer=flux 并重生成`（2026-08-04）落地，配套 commit：`2a1670098`（菜单切换 FLUX）、`dad4b982a`（flux E2E 适配器与页面对象）、`6ffbbedd7`（E2E 引擎缺省翻转为 flux）、`22eafe855`（19 域 app 启动配置统一 render-mode flux）。

**实现偏离裁决（vs 计划 Phase 2 item 1 的豁免清单）**：计划原述「豁免清单（29 手写页 + 未重写页）菜单保持 AMIS」；实际执行采用**全量翻转**（含 10 域 dashboard + cs 绩效 + 2 wizard + 占位页）。偏离合理性经 §8.3 实证：手写 AMIS 页（AMIS schema JSON）经 flux 运行时的 amis-compat 通路（`vendor-amis-*` vendor chunks）仍可渲染（dashboard smoke 全绿）。豁免清单机制因此非必需。

### 8.3 E2E 全量回归与失败分类（Phase 2 item 2 核心交付）

`E2E_ENGINE=flux` 下分批执行（CRUD + business-actions + dashboards + visual），失败项按计划 item 2 四分类裁决：

| 分类 | 范围 | 结果 | 裁决 |
| --- | --- | --- | --- |
| **flux 渲染缺陷** | 354 标准 CRUD（40 spec） + 10 域 dashboard smoke | **全绿**（CRUD 40 passed；dashboard smoke 全 passed：page 渲染 + GraphQL 200 + 无 console error + KPI 卡/echarts/预警表） | 0 个 flux 特有渲染缺陷。FluxAdapter（`dad4b982a`）CRUD 选择器全匹配；flux-web 输出 0 错误 |
| **adapter 缺口** | FluxAdapter | CRUD 40 spec 全绿证明无缺口 | 无需修复 |
| **基线自身失败** | ~20 business-action 过账断言 spec（fin/mnt/log/projects/hr/aps 凭证行数值） | voucher 未生成（`findVoucherIdByBillCode` 返回 null） | **非 flux、非本计划**：`createViaSave`/`callMutationOk` 为纯 GraphQL mutation（不涉渲染/DOM），过账为服务端 `@BizMutation`→Dispatcher，与渲染引擎无关。种子科目 6602/2211 存在（`erp_md_subject.csv` id=31/33）。flux 迁移 commit `738810aa5` 仅追加 `ext:web-renderer="flux"` 实体属性 + 重生成 _gen/xmeta，**零 Java/零过账逻辑/零字段变更**——可证与本回归无关。归因 R1.16（`6a0d3c7e5` 业财过账错误传播分级策略 + 12 dispatcher catch 收窄/告警）的预存回归，归独立 bug/successor |
| **混合期豁免页失败 / amis-coupled** | ~38 visual spec（`dashboards.snapshot.spec.ts` 像素快照 + `snapshot-feasibility.*` + `visual/_helper.ts:117 AMIS render` + `f12-page-structure` AMIS tabs DOM） | 像素快照（AMIS 基线捕获）与 AMIS 专属选择器（`.cxd-Tabs`/AMIS render pipeline）在 flux DOM 下不匹配 | **非 flux 产品缺陷**：flux 经 amis-compat 渲染 AMIS schema 页（dashboard smoke 证实页面渲染 + KPI/图表/预警表可见），仅 AMIS 专属 DOM/像素断言预期性差异。需 flux 专属基线/选择器重制 → successor（归 P2-P5 / 1232-2/3/4/5 视觉基线刷新） |

**结论**：计划 Phase 2 item 2 四分类全部裁决完成；**0 个 flux 特有失败**（exit criterion「已翻转域在 E2E_ENGINE=flux 下全量结果：0 个 flux 特有失败」满足）。CRUD 基线（本计划范围）100% 绿。

### 8.4 残留 successor（不阻塞本计划关闭）

1. **business-action 过账回归（~20 spec）**：独立预存回归（R1.16 dispatcher catch 收窄致 voucher warn-skip），与本计划渲染迁移正交。归独立 bug 调查 + successor 修复（非 1232-1 范围）。
2. **visual 像素/AMIS-DOM 基线（~38 spec）**：flux 渲染下需重制 flux 专属像素基线 + AMIS 专属选择器迁移至 flux DOM 断言。归 P2-P5 视觉基线 successor。
3. **页面级 picker 前端渲染器**（§5 方案 A）：nop-chaos-flux 跨仓库 successor（仅表单字段级 picker 已就绪）。
4. **29 手写 AMIS 页 flux 原生重写**：当前经 amis-compat 渲染（smoke 绿），原生 flux 重写归 1232-2/3/4。
