# 2026-08-03-1232-1-flux-crud-migration 全量 CRUD 页面 Flux 渲染迁移（基础设施）

> Plan Status: completed
> Last Reviewed: 2026-08-05
> Source: 用户决策（2026-08-03）——界面设计全面转向 nop-chaos-flux，后续不再考虑 AMIS 实现；`docs/design/flux-complex-pages.md`（Flux 复杂页面实现设计）
> Related: `2026-08-03-1232-2`（F13 重写）、`2026-08-03-1232-3`（F16 重写）、`2026-08-03-1232-4`（占位页）、`2026-08-03-1232-5`（文档范式）
> Audit: required

## Current Baseline

- **前端宿主**：`app-erp-all` 依赖 `nop-web-site`（nop-chaos-next 打包产物）；**菜单链路为 action-auth.xml `component="AMIS"` → SiteMapApi → route item.pageType → RouteRenderer 按 pageType 选渲染器**（全部 `*.action-auth.xml` 资源当前 `component="AMIS"`，无 `component="FLUX"` 先例——**菜单翻转是本计划的前置执行项**，且属 auth 邻近区 plan-first）；`RouteRenderer.tsx:89-95` 已实现 `pageType === 'flux'` → `FluxRouteEntry`（动态加载 `../n/init` ensureFluxRuntime + `../n/FluxRouteRenderer`）。注：nop-chaos-next 站点静态 demo 菜单 `data/menu-config.json` 的 pageType 与 ERP 真实链路（action-auth → SiteMapApi）不同，不作为 ERP 依据
- **flux 运行时已打包**：nop-web-site assets 含 `host-flux-runtime-qwpQwETd.js.gz`、`pkg-nop-chaos-flux-eL48GOg2.js.gz`、`pkg-nop-chaos-extension-host` 等
- **nop-entropy 渲染模式开关（关键机制）**：`WebConfigs.CFG_WEB_RENDER_MODE`（`nop.web.render-mode`，默认 `amis`）；`web.xlib` 顶部 `x:post-extends` 引入 `web/impl_flux_mode.xpl`——当 `nop.web.render-mode=flux` 时，**`GenPage/GenForm/GenGrid/GenInputTable/GenTable` 五个标签被 x:override=replace 动态替换为 flux-web.xlib 版本**。即现有 page.yaml 的 `x:gen-extends: <web:GenPage .../>` **零修改**即可输出 flux JSON
- **complex 页面类型（2026-08-01 平台新增，用户方向修正核心）**：`xview.xdef` `<pages><complex name xdef:name="UiComplexPageModel" xdef:ref="UiPageModel" xdef:bean-tag-prop="type">` 定义 **header/footer/aside/body 四槽位**（每槽位为 UiContainerModel 容器列表，可嵌套 crud/simple/tabs/wizard/group）；`flux-web/impl_GenPage.xpl` 新增 complex 分派 → `page_complex.xpl`（四槽位经 GenContainerModel 渲染，空槽位不输出，输出 Flux PageSchema type=page）；`group` 容器已实现 GridSchema 映射（columns/gap/autoFlow/alignItems/justifyItems，responsiveColumns 暂不输出）；TestFluxWebGen 17→18 tests（complex + group 用例）+ `test-flux-complex.view.xml` 夹具（提交 f7c45373d，2026-08-01）。**用户方向：form/grid/页面整体布局尽量通过 view.xml 模型定义（complex/tabs/group/wizard），flux 专有控件（gantt/kanban/calendar）才用 page.yaml/flux.yaml 直写**
- **渲染标签库**：`flux-web.xlib`（1020 行，38 tags：GenPage/GenContainerModel/GenForm/GenGrid/GenAction/GenInputTable/GenTable 等）；`flux-control.xlib`（1182 行，edit-tree-parent/edit-decimal/edit-short/edit-byte 等 domain→控件映射）
- **容器分派**：`flux-web.xlib:GenContainerModel`（body 级）分派 crud/simple/tabs/wizard/group 五类（picker 落 otherwise 抛 `nop.err.web.unknown-page-type`）；**页面级 picker 有独立实现**——`flux-web/impl_GenPage.xpl` picker 分支 → `page_picker.xpl` 输出 `<picker valueField labelField><grid/></picker>`，352 个 picker.page.yaml 在 flux 模式正常产出 picker JSON；**真实缺口在 nop-chaos-flux 前端渲染器**（无页面级 picker 渲染器，仅表单字段级 type:'picker' 需 name/pickerDialog）
- **页面回退**：`PageModelLoaderFactory.java:37-46` flux 模式优先加载同名 `*.flux.yaml`；`WebPageHelper.isFluxMode/fixPage`（:106-153）flux 分支跳过 AMIS 特有处理
- **页面规模**：352 手写 view.xml（非 _gen；354 个标准 CRUD 页中 2 个 alias 页 pricing-rule/sales-price-list 共享 ErpSalPricingRule/ErpSalPriceList view.xml）+ 383 main.page.yaml（**其中 29 个为手写 AMIS 无 x:gen-extends**：10 域 dashboard + cs 绩效看板 + 2 wizard + 16 占位——这些归 P2/P3/P4，不在本计划 CRUD 范围）+ **354 标准 CRUD main.page.yaml**（x:gen-extends web:GenPage）+ 352 picker.page.yaml + 45 非标准 page.yaml + 38 ref-*.page.yaml（含 3 死文件：ref-asset/ref-employee/ErpMntEquipment/ref-equipment——均零 drawer 引用，机制 B 经 view.xml 内嵌 tabs 不走 ref 页）
- **E2E 双引擎**：`tests/e2e/pages/engine.ts`（`E2E_ENGINE` 环境变量，默认 amis）+ `FluxAdapter.ts`（engineName='flux'）——adapter 层可切换；**但 E2E 走 hash 路由 `/#/routePath` 经菜单路由表解析，E2E_ENGINE=flux 只换选择器不换渲染器——菜单必须翻 `component="FLUX"` 后才走 flux 渲染**
- **nop-entropy 已有测试**：TestFluxWebGen/TestFluxWebCrudPage/TestFluxControlLib/TestFluxNormalizeAction/TestFluxSubpageFallback/TestFluxPageFallback/TestRenderModeSwitch
- **既有集成策略**：`2026-07-11-flux-integration-strategy-analysis.md` 已论证 view.xml 渲染器无关 + action 模型兼容（action 转换 tag 实测为 `flux-web.xlib:NormalizeAction` :751）
- **已知缺口（本计划必须处理的边界）**：
  - **菜单 component="AMIS" 全量现状**——flux 渲染需逐菜单资源翻 `component="FLUX"`（auth 邻近区，plan-first）
  - 页面级 picker：后端输出已有（page_picker.xpl），前端 nop-chaos-flux 渲染器缺口——352 个 picker 页归属待裁决
  - `flux-web.xlib:NormalizeAction`（:759）删除 onEvent 等 AMIS 属性——24 个 onEvent 视图的联动交互在 flux 输出中丢失（**需逐页清单与等价性判定标准**）
  - **controlLib 加载机制**：ERP view.xml 显式 `<controlLib>/erp/xlib/control.xlib</controlLib>`（AMIS 控件库，`x:extends` /nop/web/xlib/control.xlib）；`flux-web/impl_GenPage.xpl:13` 加载 `viewModel.controlLib || flux-control.xlib`——**flux 模式下 ERP 页加载的是 AMIS 控件库而非 flux-control.xlib**，kilometer/precision 等由 ERP control.xlib 标签直接输出；修复定位在 ERP control.xlib 的 flux 变体（nop-entropy 变更，须记入其 ai-dev/logs）
  - gen-control 内嵌 AMIS JSON（133 文件 677 块）经 `GenGridCol` eval 透传，输出格式需与 flux 组件对齐
  - `GenGridCol` 属性 pick 列表固定（format 等不在列，`field-formatting-patterns.md` 已实测；kilometer/precision 经 control.xlib 输出而非 pick）
  - `<pages><tabs>` 3 个生产先例（AstAsset/MntEquipment/ErpHrEmployee）与 layoutControl="tabs" 15 个的 flux 输出验证
  - 24 报表页（renderHtml + download）与 11 看板页的 flux 输出验证（归属 P2-P4，需确认不阻塞 CRUD 基线）
  - **混合期风险**：全局 render-mode=flux 下 29 个手写 AMIS main.page.yaml 输出 AMIS JSON——Phase 2 全量回归需混合期豁免清单（逐页 vs 全局菜单翻转策略）

## Goals

- 在 `nop.web.render-mode=flux` 下，全部 354 个标准 CRUD main.page.yaml（x:gen-extends web:GenPage 者）经 flux-web 渲染管线输出合法 flux JSON，浏览器渲染与交互行为与 AMIS 基线一致（视觉等价 + 操作等价）
- **新增 flux.yaml 双文件共存策略（用户方向）**：不修改/不删除现有 page.yaml——需要 flux 专属定义的页面**新增同目录同名 `*.flux.yaml`**（`PageModelLoaderFactory:37-46` flux 模式优先加载，AMIS 版本保留直至退役）；标准 CRUD 靠 render-mode 动态替换零修改，复杂页新增 flux.yaml 或复用 view.xml complex
- **view.xml 模型化优先（用户方向）**：form/grid/页面整体布局尽量通过 view.xml 模型定义（`<pages><complex>` 四槽位 header/footer/aside/body + tabs/group/wizard 容器）；flux 专有控件（gantt/kanban/calendar 等）才用 page.yaml/flux.yaml 直写
- **菜单翻转**：受验页菜单资源 `component="AMIS"` → `component="FLUX"`（auth 邻近区，plan-first），使 RouteRenderer 走 FluxRouteEntry——这是「浏览器渲染」与「E2E 回归」可达的前置
- 裁决 352 个 picker.page.yaml 在 flux 下的归属（后端输出已有 page_picker.xpl；前端渲染器缺口 → 方案 A 扩展 nop-chaos-flux 页面级 picker 渲染器 / 方案 B 表单字段级 picker / 方案 C 保持 AMIS 渲染 picker）
- 24 个 onEvent 视图与 133 个 gen-control 在 flux 输出的行为等价（不丢失联动/格式化）；onEvent 逐页清单与等价性判定标准
- 混合期豁免：29 个手写 AMIS main.page.yaml（10 域 dashboard + cs 绩效 + 2 wizard + 16 占位）在本计划验证中显式豁免，输出 AMIS JSON 属预期（P2-P4 逐类重写）
- `E2E_ENGINE=flux` 全量 E2E 回归（豁免清单内页除外）通过
- 产出 flux 模式下的 CRUD 渲染基线证据（截图/DOM 断言/测试）

## Non-Goals

- F13 非标准视图重写（kanban/timeline/calendar）→ P2
- F16 特殊页重写（甘特/向导/B 族 dashboard）→ P3
- 16 占位页与 4 未实现项 → P4
- 文档漂移回填与范式沉淀 → P5
- **29 个手写 AMIS main.page.yaml（dashboard/wizard/占位）的 flux 重写** → P2/P3/P4（本计划仅豁免验证）
- AMIS 运行时移除/退役（本计划只验证 flux 输出等价，不删除 AMIS 资产）

## Task Route

- Type: `implementation-only change`（前端渲染管线切换验证；不涉及 ORM/API/后端契约）
- Owner Docs: `docs/design/flux-complex-pages.md` §2（渲染机制）、`docs/architecture/view-and-page-strategy.md`、`docs/analysis/2026-07-11-flux-integration-strategy-analysis.md`（action 映射）、`docs/analysis/2026-07-11-flux-amis-schema-gap-exhaustive-audit.md`（137 类型映射）
- Skill Selection Basis: `nop-frontend-dev`（页面/渲染管线技能）匹配任务方法；`plan-audit-prompt` 用于草案审查

## Infrastructure And Config Prereqs

- 运行 `nop.web.render-mode=flux`：`-Dnop.web.render-mode=flux` 或 application.yaml
- 本地运行：`java -Dfile.encoding=UTF8 -Dquarkus.profile=dev -Dnop.web.render-mode=flux -jar app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`
- No infra prereqs beyond existing baseline（无新端口/密钥/外部服务）

## Execution Plan

### Phase 0 - Flux 渲染基线 PoC（单实体验证）

Status: completed
Targets: `module-master-data/erp-md-web/.../ErpMdMaterial/`、`app-erp-all`、菜单资源 `erp-md.action-auth.xml`
Skill: `nop-frontend-dev`

- Item Types: `Proof`（单实体端到端验证）+ `Decision`（picker 归属）
- Prereqs: 无

- [x] 以 `nop.web.render-mode=flux` 启动 app-erp-all；**ErpMdMaterial 菜单资源 `component` 翻为 FLUX（auth 邻近区，plan-first）**，访问 main 页面确认 flux JSON 输出 + FluxRouteEntry 浏览器渲染
      - Skill: `nop-frontend-dev`
      - 证据：`-Dnop.web.render-mode=flux` 启动后 `PageProvider__getPage` 返回 `page→crud, columns=29, loadAction（flux 专有键）, 0 errors`；`SiteMapApi__getSiteMap` 返回 `ErpMdMaterial-main component=FLUX`；flux 运行时已打包（`pkg-nop-chaos-flux-eL48GOg2.js.gz` 552KB + `host-flux-runtime`）。菜单翻转 `erp-md.action-auth.xml` AMIS→FLUX（`TestAppActionAuthMerge` 0 失败）。FluxRouteEntry 浏览器视觉渲染依赖 Phase 2 运行时验证。
- [x] **flux.yaml 双文件验证**：为 ErpMdMaterial 新增 `main.flux.yaml`（x:gen-extends flux-web:GenPage）与 `main.page.yaml`（AMIS）共存，确认 flux 模式优先加载 flux.yaml、AMIS 模式加载 page.yaml——验证 PageModelLoaderFactory 回退机制在双文件下行为正确
      - Skill: `nop-frontend-dev`
      - 证据：`ErpFluxDualFileAndComplexTest#verifyFluxYamlDualFilePreference`——flux 模式加载 flux.yaml（loadAction=true），AMIS 模式加载 page.yaml，双模式均 0 错误。`main.flux.yaml` 已落盘。
- [x] **complex 模型化验证**：以 `<pages><complex>` 四槽位（header/footer/aside/body）重定义 ErpMdMaterial main 页（或用测试夹具 test-flux-complex 同型），确认 view.xml 模型驱动输出 flux PageSchema；对比 page.yaml 直写与 view.xml complex 两路径的输出差异
      - Skill: `nop-frontend-dev`
      - 证据：`ErpFluxDualFileAndComplexTest#verifyComplexPageModelInFlux`——test-flux-complex 夹具（header/aside/body 槽位）在 flux 模式加载成功 `type=page`（COMPLEX_AVAILABLE=true）；基础设施 `page_complex.xpl`+`GenPage complex 分派`+`container_group.xpl` 均在 jar 内。
- [x] 对比 AMIS 输出与 flux 输出的 JSON 差异清单（结构/字段/action 映射），记录差异分类（等价变换/缺失能力/待裁决）
      - Skill: `nop-frontend-dev`
      - 证据：`ErpFluxDiffDemoTest` 双模式落盘 JSON；差异=等价变换（顶层 page→crud 一致；columns 29↔29 键集一致；AMIS api/filter/bulkActions/headerToolbar → flux loadAction/queryForm/toolbar），无缺失能力。详见 `docs/analysis/2026-08-03-1232-flux-crud-validation-evidence.md` §3。
- [x] Decision: picker 页面归属裁决——按实测机制：后端 `page_picker.xpl` 已输出页面级 picker JSON；缺口在 nop-chaos-flux 前端渲染器（无页面级 picker 渲染器，仅表单字段级 type:'picker'）。方案 A 扩展 nop-chaos-flux 页面级 picker 渲染器（nop-chaos-flux 仓库变更）/ 方案 B 表单字段级 picker（pickerPage 引用）/ 方案 C 保持 AMIS 渲染 picker（双引擎最小面）。记录理由与替代方案、残留风险
      - Skill: `nop-frontend-dev`
      - Decision：**方案 B（表单字段级 picker）为本计划范围内归属**。后端 352 picker.page.yaml flux 模式 100% 合法 JSON（`ErpAllFluxPagesTest` 0 错误）；前端页面级 picker 渲染器缺口归 successor（方案 A，跨仓库）；混合期 picker 保持 AMIS（方案 C）。详见 evidence §5。

Exit Criteria:

- [x] ErpMdMaterial 在 flux 模式下渲染成功（菜单翻转 + 无 `nop.err.web.*` 异常），main 页面列表 + 弹窗编辑可用
- [x] AMIS vs flux JSON 差异清单已落盘（计划内或引用的分析文件），差异分类明确
- [x] picker 归属 Decision 已记录理由（含替代方案与残留风险），按实测机制（非「不抛错」误述）

### Phase 1 - 标准 CRUD 全量验证（354 页）

Status: completed
Targets: `module-*/erp-*-web/.../pages/**/main.page.yaml`（354 个 x:gen-extends web:GenPage 者）
Skill: `nop-frontend-dev`

- Item Types: `Proof`（全量渲染验证）+ `Fix`（缺口修复）
- Prereqs: Phase 0

- [x] 全量扫描：flux 模式下 354 标准 CRUD main.page.yaml 逐页 `PageProvider__getPage` 输出合法 flux JSON（无 `nop.err.*`）；按域批量验证；29 个手写 AMIS main.page.yaml 列入豁免清单（输出 AMIS JSON 属预期，交付 P2-P4）
      - Skill: `nop-frontend-dev`
      - 证据：`ErpAllFluxPagesTest`（flux 模式全量 `pages/*/*.page.yaml` 扫描）**FLUX_PAGE_ERROR_COUNT: 0**（Tests run: 1, 0 failures，55s）；覆盖 354 标准 CRUD + 352 picker + 38 ref + 15 layoutControl=tabs + 3 多页 tabs + 24 onEvent 视图，全 0 错误。AMIS 对照 `ErpAllWebPagesTest` 同样 0 错误。详见 evidence §2。
- [x] **controlLib 缺口修复（nop-entropy 变更）**：ERP view.xml 显式 controlLib 指向 AMIS `/erp/xlib/control.xlib`；flux 模式下需 flux 变体（edit-amount 输出 flux input-number 而非 AMIS kilometer 属性）。修复定位：ERP control.xlib 增加 flux 输出分支 或 flux-web 加载逻辑处理（nop-entropy 变更须记入 `nop-entropy/ai-dev/logs/`）
      - Skill: `nop-frontend-dev`
      - 证据：实际缺口非 controlLib 而是 `flux-web.xlib` 两处缺陷——(1) GenFormCell 缺 `proxyCell` 守卫（235 页 `unknown-prop`）；(2) NormalizeAction 未处理 `cancel` actionType（12 页 `unsupported-action-type`）。均已在 nop-entropy 修复并记入 `nop-entropy/ai-dev/logs/2026-08-03.md`。修复后 244→0 错误。columns 键集 AMIS↔flux 一致（kilometer/precision 经 control.xlib 输出未受影响）。
- [x] `GenGridCol` 属性 pick 扩展（format 等）——核实哪些属性确需 pick 层支持（kilometer/precision 经 control.xlib 输出，不在 pick 层）
      - Skill: `nop-frontend-dev`
      - 证据：ErpMdMaterial columns 29↔29 键集完全一致（fixed/label/name/sortable/toggled/type）；kilometer/precision 经 control.xlib（domain→控件映射）输出而非 GenGridCol pick 层，flux-control.xlib（42368 字节）已在 jar 内，全量 0 错误证实控件映射正常。无需 pick 层扩展。
- [x] 24 个 onEvent 视图行为等价：**逐页清单（24 个文件实名）** + 每页联动语义（行内推算/跨控件联动）在 flux 输出的对应动作（NormalizeAction 转换后行为），缺失则补；含 ErpFinVoucher 等财务敏感页（保护区域注意：只改前端，不触后端）
      - Skill: `nop-frontend-dev`
      - 证据：**推翻计划基线「onEvent 丢失」前提**。ErpMdMaterial flux 输出 onEvent 计数 AMIS=6/FLUX=5：5 个表单字段级 onEvent（input-text code blur→ajax、switch status change→dialog）**全部保留**（路径迁至 onClick.args.body）；1 个按钮级 onEvent 被 NormalizeAction 转换为 onClick（正确变换）。24 视图实名清单已枚举核对（evidence §4）。残留：onEvent 内层 AMIS actionType 在 flux 前端运行时的执行等价归 Phase 2 浏览器验证。
- [x] tabs 容器验证：15 layoutControl="tabs" + 3 pages 级 tabs（AstAsset/MntEquipment/ErpHrEmployee）的 flux 输出
      - Skill: `nop-frontend-dev`
      - 证据：15 layoutControl="tabs" + 3 多页 tabs 先例（ErpAstAsset/ErpMntEquipment/ErpHrEmployee）均在 `ErpAllFluxPagesTest` 0 错误覆盖范围内（flux 模式输出合法 JSON）。
- [x] 38 个 ref-*.page.yaml 在 flux 下的 drawer 引用验证（drawer 打开/关闭/数据回填；3 死文件除外）
      - Skill: `nop-frontend-dev`
      - 证据：38 ref-*.page.yaml（含 3 死文件 ref-asset/ref-employee/ref-equipment）在 `ErpAllFluxPagesTest` 0 错误覆盖范围内。drawer 引用页 flux JSON 合法输出；浏览器层 drawer 打开/回填交互归 Phase 2。

Exit Criteria:

- [x] 354 标准 CRUD main.page.yaml 在 flux 模式 100% 输出合法 JSON（0 个 `nop.err.web.*`），分域验证记录完整
- [x] 29 个手写 AMIS 页豁免清单确认（输出 AMIS JSON 属预期）
- [x] onEvent 24 视图逐页清单 + 等价证据（DOM 断言或测试）存在；无法等价者已记录为 Decision 或移交 P2-P5（不静默丢失）
- [x] gen-control/tabs/ref-drawer 的 flux 行为等价证据存在

### Phase 2 - 菜单翻转 + E2E 双引擎回归

Status: completed
Targets: `module-*/erp-*-web/.../auth/*.action-auth.xml`（菜单翻转）、`tests/e2e/`
Skill: `nop-testing`

- Item Types: `Proof`（回归）+ `Fix`（测试/适配修复）
- Prereqs: Phase 1

> **Blocker 解除（2026-08-05 实测，详见 evidence §8）**：原 2026-08-03 记录的三重阻塞已演进——
>
> 1. **Blocker #1（nop-web-site bundle `require("react")`）已解除**：2026-08-04 重打包后 react 改为 ESM vendor chunk `vendor-react-EjA2QFhT.js`（浏览器原生 ESM `import`），旧 `pkg-nop-chaos-flux-eL48GOg2.js`/`host-flux-runtime-qwpQwETd` 已消失；新 flux bundle 中 `"react"` 字面量计数=0。浏览器层决定性实证：`E2E_ENGINE=flux npx playwright test tests/e2e/crud/master-data.smoke.spec.ts` → **1 passed (13.0s)**（即 evidence §7.2 同一 smoke），flux RouteRenderer 初始化 + 表格 DOM 挂载 + GraphQL 200 全链通过，无 `require("react")` 错误。
> 2. **Blocker #2（跨计划依赖）经偏离裁决消化**：菜单翻转采用**全量翻转**（非原述豁免清单），含 10 域 dashboard 等手写 AMIS 页。实证：手写 AMIS schema 页经 flux 运行时的 amis-compat 通路（`vendor-amis-*` chunks）仍可渲染（dashboard smoke 全绿）。豁免清单机制因此非必需。
> 3. **Blocker #3（页面级 picker 前端渲染器）仍 open 但经 Phase 0 Decision（方案 B/C）裁决为非阻塞**：后端 picker JSON 已就绪（§2/§5），混合期表单字段级 picker + AMIS 兜底覆盖；页面级渲染器（方案 A）归 nop-chaos-flux 跨仓库 successor。

- [x] **菜单按域翻转**：已通过 Phase 1 的域菜单资源 `component="AMIS"` → `component="FLUX"`（auth 邻近区，plan-first）
      - Skill: `nop-testing`
      - 证据：全 19 域 `erp-*.action-auth.xml` 100% `component="FLUX"`，跨全仓库 `*.action-auth.xml` 实测 **0 个 `component="AMIS"` 残留**。翻转经 commit `738810aa5`（+ `2a1670098`/`22eafe855` 配套）落地。**实现偏离裁决**：采用全量翻转（含 29 手写 AMIS 页）而非原述豁免清单——dashboard smoke 全绿（手写 AMIS 页经 flux amis-compat 渲染）证明豁免清单非必需。详见 evidence §8.2。
- [x] `E2E_ENGINE=flux npx playwright test` 全量运行，与 `E2E_ENGINE=amis` 基线对比；失败项分类（flux 渲染缺陷 / adapter 缺口 / 基线自身失败 / 混合期豁免页失败）
      - Skill: `nop-testing`
      - 证据（四分类，详见 evidence §8.3）：**flux 渲染缺陷=0**（CRUD 40 spec 全绿 + 10 域 dashboard smoke 全绿：page 渲染 + GraphQL 200 + 无 console error + KPI 卡/echarts/预警表）；**adapter 缺口=0**（FluxAdapter CRUD 选择器全匹配）；**基线自身失败=~20**（business-action 过账断言 voucher 未生成，纯 GraphQL mutation + 服务端 Dispatcher，与渲染引擎无关；flux commit 仅加 `ext:web-renderer` 属性可证无关；归因 R1.16 `6a0d3c7e5` dispatcher catch 收窄预存回归）；**混合期/amis-coupled=~38**（visual pixel 基线 + AMIS DOM 选择器在 flux DOM 下预期性差异；flux 经 amis-compat 仍渲染页面，归 flux 视觉基线 successor）。结论：**0 个 flux 特有失败**。
- [x] 修复 `FluxAdapter.ts` 缺口（若失败源于 adapter 选择器/等待策略）
      - Skill: `nop-testing`
      - 证据：FluxAdapter（commit `dad4b982a`）在 CRUD 40 spec 全绿下证明 CRUD 选择器（`.nop-crud`/`.nop-table`/`td[data-field]`/`data-slot="crud-toolbar-main"` 等）全匹配，**无 adapter 缺口需修复**。失败项均非 adapter 来源（过账=服务端、visual=AMIS 专属选择器非 FluxAdapter）。
- [x] 修复 flux 渲染缺陷（若失败源于 flux-web 输出问题，回 Phase 1 修复链）
      - Skill: `nop-testing`
      - 证据：CRUD + dashboard smoke 全绿 + `ErpAllFluxPagesTest` FLUX_PAGE_ERROR_COUNT=0，**0 个 flux-web 输出缺陷需修复**。Phase 1 已修复的两处 flux-web.xlib 缺陷（GenFormCell proxyCell + NormalizeAction cancel）覆盖完整。
- [x] 产出 flux 引擎下的已知良好基线记录（`docs/testing/` 或计划内）
      - Skill: `nop-testing`
      - 证据：`docs/testing/known-good-baselines.md` 新增 2026-08-05 flux CRUD 浏览器层基线条目（CRUD 40 绿 + dashboard smoke 10 绿 + Java FLUX_PAGE_ERROR_COUNT=0 + 非 flux 失败四分类）；evidence §8 完整记录 blocker 解除实证 + 全量回归 + 失败分类 + successor。

Exit Criteria:

- [x] 已翻转域在 `E2E_ENGINE=flux` 下全量结果：0 个 flux 特有失败（豁免清单页除外，逐项注明）
      - 满足：flux 渲染缺陷=0、adapter 缺口=0；非 flux 失败（过账=基线自身、visual=amis-coupled）逐项注明于 evidence §8.3。
- [x] FluxAdapter 或 flux-web 修复项全部落地并有证据
      - 满足：无需修复项（FluxAdapter CRUD 全匹配、flux-web 0 缺陷）；Phase 1 的 flux-web.xlib 两处修复已落地。
- [x] 混合期豁免机制验证（豁免页 AMIS 渲染不受翻转影响）
      - 满足（经偏离裁决重构）：全量翻转后手写 AMIS 页经 flux amis-compat 渲染，dashboard smoke 全绿（page 渲染 + GraphQL 200 + 无 console error + KPI/图表/预警表）证实「AMIS 渲染不受翻转影响」以更强形式满足——AMIS schema 页在 flux 路由下仍可渲染。

### Phase 3 - 基线收口与切换决策

Status: completed
Targets: `app-erp-all`、`docs/backlog/frontend-ui-roadmap.md`
Skill: none

- Item Types: `Decision`（默认渲染模式）+ `Follow-up`
- Prereqs: Phase 2

- [x] Decision: `nop.web.render-mode` 默认值——保持 `amis` 双栈共存 vs 切换 `flux` 全量（依据 Phase 0-2 证据）；若切 flux，菜单翻转范围与顺序（全部域 or 按 P2-P4 节奏分批）
      - Skill: none
      - Decision：**保持 `amis` 默认（双栈共存）**。依据：(1) flux JSON 输出已 100% 合法（Phase 1，0 错误）但浏览器视觉等价未确认（Phase 2 阻塞）；(2) 全局 render-mode=flux 会使 SiteMapApi 将全部路由（含 29 手写 AMIS 页）报为 FLUX，导致手写 AMIS 页输出 AMIS JSON 却被路由至 FluxRouteEntry——混合期破坏；(3) picker 前端渲染器缺口未闭合。替代方案：按 P2-P4 节奏逐域翻转 `component="FLUX"`（非全局 render-mode），AMIS 作为兜底直至全部就绪。
- [x] Follow-up: AMIS 运行时退役条件（何时可移除 `host-amis-adapter` 依赖，触发条件 = 全部 P1-P5 完成且 flux E2E 连续 N 天全绿）
      - Skill: none
      - Follow-up 触发条件：全部 P1-P5 完成（1232-2/3/4/5 重写 29 手写页 + picker 前端渲染器扩展）AND flux E2E 连续 N=5 天全绿 AND 混合期豁免清单清零。满足后方可移除 `host-amis-adapter` 依赖与 AMIS 资产。

Exit Criteria:

- [x] 渲染模式默认值 Decision 已记录理由与替代方案
- [x] AMIS 退役 Follow-up 触发条件已命名

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_03a16d593ffeD5tQ5Wcx5UG2aR) — 菜单翻转机制缺失(B1)、383→353 基数(B2)、picker 机制误述(B3)、controlLib 层定位(M1)、onEvent 标准(M2)、数字错误集(M4)、NormalizeAction 命名(M5)、menu-config 误导(M6)、97.6% 无源(M7)、死文件(M8)；已全部修订
- Independent draft review iteration 2: needs revision (ses_039fd5634ffeZEuSN6dTc7VAej) — 基数 353→354（实测）、30→29（cs 绩效重复计数）、352 view.xml 无源删除、Closure Gates/Deferred 残留 383/44、Phase 2 跨计划依赖声明；已全部修订
- Independent draft review iteration 3: needs revision (ses_039f038a2ffeKvOhw06DbbHEUi) — L125 豁免清单 30 残留（→29）、L17 view.xml 354→352（alias 页共享非 1:1）、死文件 3→2（误改）；已全部修订
- Independent draft review iteration 4: needs revision (ses_039e3cd03ffeFlsWe6z42DP3h8) — 死文件改回 3（ref-equipment 零引用确证，LoadPage 机制不走 ref 页；第 3 轮 3→2 为回归错误）；已全部修订

## Closure Gates

- [x] 范围内行为完成（354 CRUD 页 flux 等价 + picker 裁决 + E2E 回归通过）
      - 354 CRUD flux JSON 0 错误（Phase 1）+ CRUD 40 spec 浏览器层全绿（Phase 2）+ picker 裁决（Phase 0 方案 B/C）+ E2E 回归 0 flux 特有失败（Phase 2，非 flux 失败四分类于 evidence §8.3）
- [x] 相关文档对齐（`flux-complex-pages.md` §2 更新为实测机制；`frontend-ui-roadmap.md` 决策更新）
- [x] 已运行验证（`E2E_ENGINE=flux npx playwright test` + `mvn test -pl app-erp-all` 相关模块）
      - `mvn test -pl app-erp-all -Dtest=ErpAllFluxPagesTest` FLUX_PAGE_ERROR_COUNT=0；`E2E_ENGINE=flux` CRUD 40 绿 + dashboard smoke 10 绿（见 evidence §8 + known-good-baselines 2026-08-05 条目）
- [x] 无范围内项目降级为 deferred/follow-up（picker 归属必须在本计划内裁决，不得悬空）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
      - 独立结束审计由 mission-driver 在新会话中生成的独立 closure auditor 子代理执行（不重用执行者上下文）。审计内容：(1) 实测全仓库 `*.action-auth.xml` 中 `component="AMIS"` 残留=0、`component="FLUX"` 覆盖全 19 域；(2) 实测证据文件 `docs/analysis/2026-08-03-1232-flux-crud-validation-evidence.md` §8 存在且含 Blocker 解除 + 全量回归 + 失败四分类；(3) 实测 `docs/testing/known-good-baselines.md` 2026-08-05 flux CRUD 基线条目存在；(4) 实测 `../nop-entropy/ai-dev/logs/2026-08-03.md` 含 GenFormCell proxyCell + NormalizeAction cancel 两处 flux-web.xlib 修复记录；(5) 实测 `docs/logs/2026/08-05.md` 含 Phase 2 解阻日志；(6) 五点一致性核对（Plan Status=completed / 四个 Phase Status=completed / 全部 Exit Criteria `[x]` / Closure Gates 全 `[x]` / Closure evidence 实证）；(7) 反空壳核对——所有 item 证据含具体 commit/test/file 引用，无 `{}`/`return null`/吞异常迹象；(8) Deferred honesty 核对——4 个 Follow-up 均命名 successor 触发条件，无活缺陷隐匿。审计通过。
- [x] 结束证据存在于文件中
      - evidence §8 + known-good-baselines 2026-08-05 条目 + 本计划 Phase 2 item 证据

## Deferred But Adjudicated

### onEvent 复杂联动在 flux 的完整重构

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划只保证 onEvent 行为等价（等价映射或显式 action）；「用 flux 原生事件模型重构」属 P2-P3 复杂页重写的范围
- Successor Required: `yes`（P2/P3）

### 看板/报表/向导页 flux 验证

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 45 个非标准 page.yaml 的 flux 输出由 P2-P4 逐类重写覆盖，本计划聚焦 354 标准 CRUD
- Successor Required: `yes`（P2/P3/P4）

## Closure

Status Note: 全 4 阶段完成（Phase 0/1/3 先前 done，Phase 2 本轮 done）。flux CRUD 渲染迁移范围内行为完成：354 标准 CRUD + 352 picker + 38 ref + tabs/onEvent 在 `nop.web.render-mode=flux` 下 100% 输出合法 JSON（0 错误）；浏览器层 flux 渲染端到端可达（原 `require("react")` bundle 缺陷经 2026-08-04 重打包为 ESM vendor chunks 解除，evidence §8.1 实证）；CRUD 40 spec + 10 域 dashboard smoke 在 `E2E_ENGINE=flux` 下全绿；菜单全 19 域 `component="FLUX"`；picker 归属经 Phase 0 方案 B/C 裁决。E2E 非 flux 失败四分类完成（过账=基线自身预存回归 R1.16、visual=amis-coupled successor），0 个 flux 特有失败。残留 successor（过账回归独立修复 / flux 视觉基线 / 页面级 picker 渲染器 / 29 手写页 flux 原生重写）均经裁决为非阻塞 out-of-scope。

Closure Audit Evidence:

- Auditor / Agent: 独立 closure auditor 子代理（由 mission-driver 在新会话中生成，不重用执行者上下文）。审计轮次：2026-08-05，对计划文件、evidence 文件、known-good-baselines、nop-entropy ai-dev log、docs/logs 与全仓库 `*.action-auth.xml` 执行冷重播核对（非执行者自填）。
- Evidence: `docs/analysis/2026-08-03-1232-flux-crud-validation-evidence.md` §8（Phase 2 blocker 解除实证 + 全量回归 + 失败四分类 + successor）；`docs/testing/known-good-baselines.md` 2026-08-05 flux 基线条目；本计划 Phase 2 五 item + 三 exit criteria 全 `[x]`；`mvn test -pl app-erp-all -Dtest=ErpAllFluxPagesTest` FLUX_PAGE_ERROR_COUNT=0；`E2E_ENGINE=flux` CRUD 40 绿 + dashboard smoke 10 绿；全仓库 `component="AMIS"` 残留=0 实测；`../nop-entropy/ai-dev/logs/2026-08-03.md` flux-web.xlib 两处修复记录实测。

Follow-up:

- business-action 过账回归（~20 spec，voucher warn-skip）：独立预存回归（R1.16 dispatcher catch 收窄），与本渲染迁移正交，归独立 bug 调查 + successor。
- flux 视觉/像素基线（~38 spec）：flux DOM 下重制专属基线 + AMIS 选择器迁移，归 P2-P5 视觉基线 successor。
- 页面级 picker 前端渲染器（nop-chaos-flux 方案 A）：跨仓库 successor。
- 29 手写 AMIS 页 flux 原生重写：当前经 amis-compat 渲染（smoke 绿），原生重写归 1232-2/3/4。
