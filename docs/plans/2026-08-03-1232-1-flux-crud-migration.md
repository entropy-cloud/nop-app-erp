# 2026-08-03-1232-1-flux-crud-migration 全量 CRUD 页面 Flux 渲染迁移（基础设施）

> Plan Status: draft
> Last Reviewed: 2026-08-03
> Source: 用户决策（2026-08-03）——界面设计全面转向 nop-chaos-flux，后续不再考虑 AMIS 实现；`docs/design/flux-complex-pages.md`（Flux 复杂页面实现设计）
> Related: `2026-08-03-1232-2`（F13 重写）、`2026-08-03-1232-3`（F16 重写）、`2026-08-03-1232-4`（占位页）、`2026-08-03-1232-5`（文档范式）
> Audit: required

## Current Baseline

- **前端宿主**：`app-erp-all` 依赖 `nop-web-site`（nop-chaos-next 打包产物）；**菜单链路为 action-auth.xml `component="AMIS"` → SiteMapApi → route item.pageType → RouteRenderer 按 pageType 选渲染器**（全部 `*.action-auth.xml` 资源当前 `component="AMIS"`，无 `component="FLUX"` 先例——**菜单翻转是本计划的前置执行项**，且属 auth 邻近区 plan-first）；`RouteRenderer.tsx:89-95` 已实现 `pageType === 'flux'` → `FluxRouteEntry`（动态加载 `../n/init` ensureFluxRuntime + `../n/FluxRouteRenderer`）。注：nop-chaos-next 站点静态 demo 菜单 `data/menu-config.json` 的 pageType 与 ERP 真实链路（action-auth → SiteMapApi）不同，不作为 ERP 依据
- **flux 运行时已打包**：nop-web-site assets 含 `host-flux-runtime-qwpQwETd.js.gz`、`pkg-nop-chaos-flux-eL48GOg2.js.gz`、`pkg-nop-chaos-extension-host` 等
- **nop-entropy 渲染模式开关（关键机制）**：`WebConfigs.CFG_WEB_RENDER_MODE`（`nop.web.render-mode`，默认 `amis`）；`web.xlib` 顶部 `x:post-extends` 引入 `web/impl_flux_mode.xpl`——当 `nop.web.render-mode=flux` 时，**`GenPage/GenForm/GenGrid/GenInputTable/GenTable` 五个标签被 x:override=replace 动态替换为 flux-web.xlib 版本**。即现有 page.yaml 的 `x:gen-extends: <web:GenPage .../>` **零修改**即可输出 flux JSON
- **渲染标签库**：`flux-web.xlib`（1020 行，38 tags：GenPage/GenContainerModel/GenForm/GenGrid/GenAction/GenInputTable/GenTable 等）；`flux-control.xlib`（1182 行，edit-tree-parent/edit-decimal/edit-short/edit-byte 等 domain→控件映射）
- **容器分派**：`flux-web.xlib:GenContainerModel`（body 级）分派 crud/simple/tabs/wizard/group 五类（picker 落 otherwise 抛 `nop.err.web.unknown-page-type`）；**页面级 picker 有独立实现**——`flux-web/impl_GenPage.xpl` picker 分支 → `page_picker.xpl` 输出 `<picker valueField labelField><grid/></picker>`，352 个 picker.page.yaml 在 flux 模式正常产出 picker JSON；**真实缺口在 nop-chaos-flux 前端渲染器**（无页面级 picker 渲染器，仅表单字段级 type:'picker' 需 name/pickerDialog）
- **页面回退**：`PageModelLoaderFactory.java:37-46` flux 模式优先加载同名 `*.flux.yaml`；`WebPageHelper.isFluxMode/fixPage`（:106-153）flux 分支跳过 AMIS 特有处理
- **页面规模**：352 手写 view.xml（非 _gen；354 个标准 CRUD 页中 2 个 alias 页 pricing-rule/sales-price-list 共享 ErpSalPricingRule/ErpSalPriceList view.xml）+ 383 main.page.yaml（**其中 29 个为手写 AMIS 无 x:gen-extends**：10 域 dashboard + cs 绩效看板 + 2 wizard + 16 占位——这些归 P2/P3/P4，不在本计划 CRUD 范围）+ **354 标准 CRUD main.page.yaml**（x:gen-extends web:GenPage）+ 352 picker.page.yaml + 45 非标准 page.yaml + 38 ref-*.page.yaml（含 2 死文件：ref-asset/ref-employee；ErpMntEquipment/ref-equipment 有引用非死文件）
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

Status: planned
Targets: `module-master-data/erp-md-web/.../ErpMdMaterial/`、`app-erp-all`、菜单资源 `erp-md.action-auth.xml`
Skill: `nop-frontend-dev`

- Item Types: `Proof`（单实体端到端验证）+ `Decision`（picker 归属）
- Prereqs: 无

- [ ] 以 `nop.web.render-mode=flux` 启动 app-erp-all；**ErpMdMaterial 菜单资源 `component` 翻为 FLUX（auth 邻近区，plan-first）**，访问 main 页面确认 flux JSON 输出 + FluxRouteEntry 浏览器渲染
      - Skill: `nop-frontend-dev`
- [ ] 对比 AMIS 输出与 flux 输出的 JSON 差异清单（结构/字段/action 映射），记录差异分类（等价变换/缺失能力/待裁决）
      - Skill: `nop-frontend-dev`
- [ ] Decision: picker 页面归属裁决——按实测机制：后端 `page_picker.xpl` 已输出页面级 picker JSON；缺口在 nop-chaos-flux 前端渲染器（无页面级 picker 渲染器，仅表单字段级 type:'picker'）。方案 A 扩展 nop-chaos-flux 页面级 picker 渲染器（nop-chaos-flux 仓库变更）/ 方案 B 表单字段级 picker（pickerPage 引用）/ 方案 C 保持 AMIS 渲染 picker（双引擎最小面）。记录理由与替代方案、残留风险
      - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] ErpMdMaterial 在 flux 模式下渲染成功（菜单翻转 + 无 `nop.err.web.*` 异常），main 页面列表 + 弹窗编辑可用
- [ ] AMIS vs flux JSON 差异清单已落盘（计划内或引用的分析文件），差异分类明确
- [ ] picker 归属 Decision 已记录理由（含替代方案与残留风险），按实测机制（非「不抛错」误述）

### Phase 1 - 标准 CRUD 全量验证（354 页）

Status: planned
Targets: `module-*/erp-*-web/.../pages/**/main.page.yaml`（354 个 x:gen-extends web:GenPage 者）
Skill: `nop-frontend-dev`

- Item Types: `Proof`（全量渲染验证）+ `Fix`（缺口修复）
- Prereqs: Phase 0

- [ ] 全量扫描：flux 模式下 354 标准 CRUD main.page.yaml 逐页 `PageProvider__getPage` 输出合法 flux JSON（无 `nop.err.*`）；按域批量验证；29 个手写 AMIS main.page.yaml 列入豁免清单（输出 AMIS JSON 属预期，交付 P2-P4）
      - Skill: `nop-frontend-dev`
- [ ] **controlLib 缺口修复（nop-entropy 变更）**：ERP view.xml 显式 controlLib 指向 AMIS `/erp/xlib/control.xlib`；flux 模式下需 flux 变体（edit-amount 输出 flux input-number 而非 AMIS kilometer 属性）。修复定位：ERP control.xlib 增加 flux 输出分支 或 flux-web 加载逻辑处理（nop-entropy 变更须记入 `nop-entropy/ai-dev/logs/`）
      - Skill: `nop-frontend-dev`
- [ ] `GenGridCol` 属性 pick 扩展（format 等）——核实哪些属性确需 pick 层支持（kilometer/precision 经 control.xlib 输出，不在 pick 层）
      - Skill: `nop-frontend-dev`
- [ ] 24 个 onEvent 视图行为等价：**逐页清单（24 个文件实名）** + 每页联动语义（行内推算/跨控件联动）在 flux 输出的对应动作（NormalizeAction 转换后行为），缺失则补；含 ErpFinVoucher 等财务敏感页（保护区域注意：只改前端，不触后端）
      - Skill: `nop-frontend-dev`
- [ ] tabs 容器验证：15 layoutControl="tabs" + 3 pages 级 tabs（AstAsset/MntEquipment/ErpHrEmployee）的 flux 输出
      - Skill: `nop-frontend-dev`
- [ ] 38 个 ref-*.page.yaml 在 flux 下的 drawer 引用验证（drawer 打开/关闭/数据回填；2 死文件除外）
      - Skill: `nop-frontend-dev`

Exit Criteria:

- [ ] 354 标准 CRUD main.page.yaml 在 flux 模式 100% 输出合法 JSON（0 个 `nop.err.web.*`），分域验证记录完整
- [ ] 29 个手写 AMIS 页豁免清单确认（输出 AMIS JSON 属预期）
- [ ] onEvent 24 视图逐页清单 + 等价证据（DOM 断言或测试）存在；无法等价者已记录为 Decision 或移交 P2-P5（不静默丢失）
- [ ] gen-control/tabs/ref-drawer 的 flux 行为等价证据存在

### Phase 2 - 菜单翻转 + E2E 双引擎回归

Status: planned
Targets: `module-*/erp-*-web/.../auth/*.action-auth.xml`（菜单翻转）、`tests/e2e/`
Skill: `nop-testing`

- Item Types: `Proof`（回归）+ `Fix`（测试/适配修复）
- Prereqs: Phase 1；**跨计划依赖：菜单翻转范围受 P2-P4 完成状态约束（未重写页保持 AMIS）——与 1232-2/3/4 按顺序执行**

- [ ] **菜单按域翻转**：已通过 Phase 1 的域菜单资源 `component="AMIS"` → `component="FLUX"`（auth 邻近区，plan-first）；豁免清单（29 手写页 + 未重写页）菜单保持 AMIS
      - Skill: `nop-testing`
- [ ] `E2E_ENGINE=flux npx playwright test` 全量运行，与 `E2E_ENGINE=amis` 基线对比；失败项分类（flux 渲染缺陷 / adapter 缺口 / 基线自身失败 / 混合期豁免页失败）
      - Skill: `nop-testing`
- [ ] 修复 `FluxAdapter.ts` 缺口（若失败源于 adapter 选择器/等待策略）
      - Skill: `nop-testing`
- [ ] 修复 flux 渲染缺陷（若失败源于 flux-web 输出问题，回 Phase 1 修复链）
      - Skill: `nop-testing`
- [ ] 产出 flux 引擎下的已知良好基线记录（`docs/testing/` 或计划内）

Exit Criteria:

- [ ] 已翻转域在 `E2E_ENGINE=flux` 下全量结果：0 个 flux 特有失败（豁免清单页除外，逐项注明）
- [ ] FluxAdapter 或 flux-web 修复项全部落地并有证据
- [ ] 混合期豁免机制验证（豁免页 AMIS 渲染不受翻转影响）

### Phase 3 - 基线收口与切换决策

Status: planned
Targets: `app-erp-all`、`docs/backlog/frontend-ui-roadmap.md`
Skill: none

- Item Types: `Decision`（默认渲染模式）+ `Follow-up`
- Prereqs: Phase 2

- [ ] Decision: `nop.web.render-mode` 默认值——保持 `amis` 双栈共存 vs 切换 `flux` 全量（依据 Phase 0-2 证据）；若切 flux，菜单翻转范围与顺序（全部域 or 按 P2-P4 节奏分批）
      - Skill: none
- [ ] Follow-up: AMIS 运行时退役条件（何时可移除 `host-amis-adapter` 依赖，触发条件 = 全部 P1-P5 完成且 flux E2E 连续 N 天全绿）
      - Skill: none

Exit Criteria:

- [ ] 渲染模式默认值 Decision 已记录理由与替代方案
- [ ] AMIS 退役 Follow-up 触发条件已命名

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_03a16d593ffeD5tQ5Wcx5UG2aR) — 菜单翻转机制缺失(B1)、383→353 基数(B2)、picker 机制误述(B3)、controlLib 层定位(M1)、onEvent 标准(M2)、数字错误集(M4)、NormalizeAction 命名(M5)、menu-config 误导(M6)、97.6% 无源(M7)、死文件(M8)；已全部修订
- Independent draft review iteration 2: needs revision (ses_039fd5634ffeZEuSN6dTc7VAej) — 基数 353→354（实测）、30→29（cs 绩效重复计数）、352 view.xml 无源删除、Closure Gates/Deferred 残留 383/44、Phase 2 跨计划依赖声明；已全部修订
- Independent draft review iteration 3: needs revision (ses_039f038a2ffeKvOhw06DbbHEUi) — L125 豁免清单 30 残留（→29）、L17 view.xml 354→352（alias 页共享非 1:1）、死文件 3→2（ref-equipment 有引用）；已全部修订

## Closure Gates

- [ ] 范围内行为完成（354 CRUD 页 flux 等价 + picker 裁决 + E2E 回归通过）
- [ ] 相关文档对齐（`flux-complex-pages.md` §2 更新为实测机制；`frontend-ui-roadmap.md` 决策更新）
- [ ] 已运行验证（`E2E_ENGINE=flux npx playwright test` + `mvn test -pl app-erp-all` 相关模块）
- [ ] 无范围内项目降级为 deferred/follow-up（picker 归属必须在本计划内裁决，不得悬空）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

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

Status Note: <why the plan can close>

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
