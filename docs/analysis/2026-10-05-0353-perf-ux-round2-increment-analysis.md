# 性能与 UI/UX 深度优化第二轮（增量轮）分析报告

> 日期：2026-10-05
> 性质：实施导向的深度分析第二轮。第一轮（`docs/analysis/2026-09-27-perf-ux-deep-optimization-analysis.md`，2026-09-28 收官）修复批次 1-5+A9 补遗全部完成并提交。本轮在该基线上做**增量+全量复核**：三路独立只读扫描通道（后端性能 / UI-UX 现状核验 / 前端渲染与 flux 合规）+ 执行者对关键 finding 的实仓源码级复核。**本报告只分析不动代码；修复按 §4 批次 plan 逐批执行。**
> 增量范围：`18bc67af2..HEAD`（34 提交，fixture-bundle 测试基建 7 plan + 用户故事覆盖 USC-03~08），生产代码增量 19 文件。
> 量化基线：手写复杂页 7 页命中惰性按钮接线（16 ajax 按钮 + 6 dialog 按钮 + 12 onEvent 刷新链 + 2 表单级 api 均在 pda 页）；上轮全部存量修复抽样 8 项在位零回归；机械门禁三绿（无界查询台账 71 站点 PASS / 合规检查器 exit 0 / flux 门禁 freshness --check-only PASS）。
> 关联：`2026-09-27-perf-ux-deep-optimization-analysis.md`（第一轮基线与 §6 Deferred 台账）、`2026-09-14-complex-page-ux-review.md`（UX 首轮基线）、`docs/design/inventory/barcode-integration.md`（USC-05 设计契约）、`docs/design/roles-and-permissions.md`（USC-03/USC-02a owner doc）。

---

## 1. 结论先行

| 面向 | 核心结论 |
|------|---------|
| 存量面 | **零回归**。第一轮全部修复抽样核验在位（P1×6 字面参数死链 0 命中、FinBudgetScenario confirmText、l10n-cn 死链移除、状态字段只读、drawer 尺寸、空态、看板联动）；危险动作确认覆盖 100%（44/44，较上轮 93.5% 提升——2 处无 confirmText 的 danger 均为 preview-then-confirm dialog 范式）；看板 SQL 聚合范式零回退；全部 §6 Deferred 触发条件核对为未触发、维持登记 |
| 增量面（后端） | 无上轮裁决体系外的新性能反模式。2 项小额残留：`ErpMdMaterialSku.barcode` 无 DB 索引但已被 USC-05 **默认开启**的扫码热路径直查（第一轮 md 域 Deferred G1 的触发条件实质强化）；`MaskAuditRecorder` 去重 ThreadLocal 跨请求不清零 → %prod 字段读审计**漏报**（USC-03 审计翻转激活的正确性缺陷） |
| 增量面（UI/UX + 渲染） | **本轮核心发现：7 个手写复杂页的 AMIS 风格按钮（`actionType: ajax` + `api`）在 flux 运行期惰性**——flux button renderer 仅声明 `onClick` 一个事件字段，`actionType`/`api` 无人消费、唯一转换点（`page_simple.xpl`）只服务 view.xml 生成路径，导出产物原样携带，E2E 驱动范式（`callMutationOk` 直发 GraphQL）从不点击按钮，故从未暴露。叠加 PDA 盘点页三重缺陷（按钮惰性 + 保存 payload 缺 4 个 mandatory 字段 + lineNo 硬编码），以及 mfg 菜单角色错配（动作授权 UI 不可达） |

**本轮修复批次**（§4）：批次 1 = 6 个存量手写页按钮接线修复（含运行时探针正反对照定案）；批次 2 = pda-stock-take 全闭环重构；批次 3 = mfg 菜单角色可达性；批次 4 = MaskAuditRecorder 去重边界修复；批次 5 = barcode 索引（orm.xml 保护区双批准）。

---

## 2. 核心发现：手写页 AMIS 风格按钮运行期惰性（F1）

### 2.1 现象与家族清单

7 个手写复杂页（`*.page.yaml` 整页直写路径，无 `x:gen-extends`）存在 AMIS 风格按钮接线：

| # | 页面 | 域 | `actionType: ajax` 按钮 | 表单级 `api` | 页面职能 |
|---|------|----|------------------------|-------------|---------|
| 1 | `pda-stock-take/main.page.yaml` | inv | 2 | 2 | PDA 盘点扫码（USC-05 新增） |
| 2 | `stock-take-flow/main.page.yaml` | inv | 3 | 0 | 盘点作业流 |
| 3 | `disposal-wizard/main.page.yaml` | ast | 1 | 0 | 资产处置三步向导 |
| 4 | `period-close-wizard/main.page.yaml` | fin | 3 | 0 | 期末结账向导 |
| 5 | `visit-wizard/main.page.yaml` | mnt | 4 | 0 | 巡检向导 |
| 6 | `lead-conversion/main.page.yaml` | crm | 1 | 0 | 线索转化 |
| 7 | `ErpSysNotification/inbox.page.yaml` | notify | 2 | 0 | 通知收件箱（标记已读等） |

合计 **16 个惰性按钮 + 2 处表单级 api（均在 pda 页，归批次 2）**。同页存在 flux 原生写法对照（如 disposal-wizard 第 2 步「下一步」按钮用 `onClick: {action: refreshSource, targetId}`，导出产物中该按钮保留 onClick 而提交按钮仅剩 `actionType`+`api`）。另有同族惰性接线：6 个 `actionType: dialog` 按钮（period-close-wizard 3 + inbox 3，迁移表 `dialog → {action:'openDialog', args}`）与 12 处 `onEvent: after-success` 刷新链（迁移方向 = ActionSchema `then` 链），随批次 1 一并裁决处置。

### 2.2 证据链（六环闭合，静态+产物级）

1. **渲染器契约**：`nop-chaos-flux/packages/flux-renderers-basic/src/basic-renderer-definitions.ts` button 定义 `fields` 仅 `{ key: 'onClick', kind: 'event' }` 一个事件字段；`actionType`/`api` 未声明 → 编译期分类 unknown → 丢弃（`flux-compiler/src/schema-compiler/node-compiler.ts:171` 仅 `rule.kind === 'event'` 进 `rawEventPlans`）。
2. **渲染器实现**：`flux-renderers-basic/src/button.tsx:227` `handleClick` 仅执行 `props.events.onClick?.(event)`；无 eventPlan 时点击无任何效果。
3. **唯一转换点不覆盖手写页**：AMIS `api` → flux `submitAction`/ajax args 的转换仅存在于 `nop-entropy/nop-frontend-support/nop-web/.../flux-web/page_simple.xpl:15-28`（view.xml GenPage 简单页/dialog 路径）；手写复杂页不经此路径。flux-compiler 自身 action 编译器（`action-compiler.ts`）无 `ajax`/`api` 字样的按钮级映射。
4. **导出产物实证**：`app-erp-all/target/flux-pages/erp/ast/pages/disposal-wizard/main.page.json` 中提交按钮节点仅含 `['actionType','api','label','type']`（无 onClick）；pda 页导出同型。导出链不改写。
5. **迁移指南明文**：`nop-chaos-flux/flux-guide/09-amis-migration.md §actionType → action 转换`：「`ajax`（或有 `api`）→ `{action: 'ajax', args: {url, method, data, ...}}`」——即 AMIS 写法**必须转换**后才能在 flux 使用，`actionType`/`api` 属「AMIS action 中被删除的属性」。flux 核心确有 ajax action 词汇（`flux-core/src/constants.ts:40` + `types/actions.ts:178`），仅在 onClick ActionSchema 内使用。
6. **测试为何未暴露**：本仓 business-actions E2E 的驱动范式是 `callMutationOk`（浏览器 context 直发 GraphQL），从不点击 UI 按钮（全库仅 `voucher-back-link` 点击过 view/关闭类按钮）；pda 页 E2E 为纯可达性断言。惰性因此零暴露。

**注意（诚实口径）**：验证器的 `unknown-property` warning 不必然等于运行期失效——`i18nEn` 同样被标 unknown-property 但实际工作（导出/宿主链消费）。故本发现定性为「静态+产物级实锤、源码级无任何消费路径」，**批次 1 仍保留浏览器运行时探针做正反对照终证**（正对照=flux 原生 onClick 按钮发请求；负对照=`actionType:ajax` 按钮不发请求；修复后正对照复验）。

### 2.3 为何此前未被发现（机制教训）

- flux 门禁 18494 warning 中 unknown-property 6533 条曾整体裁决为「生成层 prop 表缺口，app 仓不可达 Deferred」——该裁决对 GenPage 生成页正确（`i18nEn`/生成器遗留属性），但**手写页的 `actionType`/`api` 是 app 仓可修的切片**，被批量裁决掩盖。
- E2E 分层设计里「业务断言走 GraphQL、UI 断言走可达性」的两层之间**没有按钮点击层**，跨层缝未被任何门禁覆盖。

---

## 3. 全部新发现清单（按严重级）

### 3.1 P1（确认缺陷）

| # | 发现 | 位置 | 证据 | 修复方向 |
|---|------|------|------|---------|
| F1 | 7 页 AMIS 风格按钮运行期惰性（见 §2） | §2.1 清单 | §2.2 六环证据链 | 按 `flux-guide/09-amis-migration.md` 转换表逐按钮改写：`onClick: {action:'ajax', confirmText?…, messages?…, args:{url, method, data}}`（**confirmText/messages 是 args 的兄弟键**——`flux-core types/actions.ts` AjaxActionSchema 形状 + 运行时 `runtime-action-helpers.ts` 只读 `action.source?.confirmText/messages`，放 args 内被静默忽略）；`onEvent: after-success` 刷新链迁 ActionSchema `then` 链；`actionType: dialog` 迁 `{action:'openDialog', args}`；批次 1 探针定案 + 6 存量页机械改写，批次 2 pda 页随全闭环重构改写 |
| F2 | PDA 盘点行保存链后端必败：payload 缺 `materialId`/`uoMId`/`bookQuantity`/`differenceQuantity` 4 个 mandatory 列，后端裸 CRUD 无补齐钩子 | `pda-stock-take/main.page.yaml:59-64,83-88` × `module-inventory/erp-inv-dao/.../orm/_app.orm.xml:1309-1327`（mandatory="true"） | 页面仅传 takeId/lineNo/skuId/locationId/actualQuantity；`ErpInvStockTakeLineBizModel` 裸 CRUD；E2E 未走通保存 | 批次 2：扫码解析返回的 `skuInfo` 含 materialId（**不含 uoMId**——`resolveSku` 返回 skuCode/barcode/materialId，草案审查实证）；账面/差异数量由后端落行钩子或前端二次查询补齐（Decision 项）；或改调专门「扫码落行」mutation |
| F3 | `lineNo: 1` 硬编码 ×2 + 行实体无唯一键：连续扫码要么静默重复行号、无幂等语义 | `pda-stock-take/main.page.yaml:61,85`；`_app.orm.xml` StockTakeLine 无 `<unique-keys>` | PDA 盘点核心循环=连续扫码落行 | 批次 2：行号生成（后端 nextLineNo 或前端 scope 计数），并裁决行幂等语义（同 SKU+库位累加 vs 新行） |

### 3.2 P2（一致性问题/正确性问题）

| # | 发现 | 位置 | 修复方向 |
|---|------|------|---------|
| F4 | 盘点单定位退化为手输数字主键 ID；`resolveBarcode` 的 STOCK_TAKE 分支（解析结果含强制 locationCode）已实现但页面零消费 | `pda-stock-take/main.page.yaml:66-70`；`ErpInvBarcodeBizModel.java:87-90` | 批次 2：盘点单 picker（`ErpInvStockTake__findPage` 现成数据源，状态白名单以盘点单状态机实际 docStatus 值裁决——字典 `erp-inv/move-status`，无 OPEN 值）或扫盘点单条码消费解析分支 |
| F5 | 解析结果零展示（用户无「扫到什么」确认）；`${scanForm.refId}`/`${scanForm.location.locationId}` 跨表单 scope 引用全库无先例（运行时脆弱点）；无 messages.success/failed、保存后表单不重置、无已录行清单 | `pda-stock-take/main.page.yaml:16-88` | 批次 2：解析成功回填只读展示区 + messages + reset；跨表单引用改单表单域或显式数据流 |
| F6 | mfg 菜单 roles 字面错配（USC-02a D3 镜像规则机械复制父组 roles 所致）：`ErpMfgMrpPlan-main`/`ErpMfgForecast-main` roles="生产主管"，而子 FNPT（runMrp/approve/releasePurchaseRequest/releaseWorkRequest/releaseSubcontractRequest/cancel 等 6 个规划员 FNPT + 3 个管理员 reverseApprove FNPT）roles 不同。**假阳性风险注记（草案审查发现）**：平台 `SiteCacheDataBuilder.cascadeResourceToRoles`（nop-auth-service `:221-231`）对 `authCascadeUp`（默认 true）资源把子 roles 递归 UNION 进祖先 → 子 FNPT roles 可能已级联使页面对规划员可见；本仓 E2E `tests/e2e/negative/e1-2-menu-filter.smoke.spec.ts` M2.8 已实证同机制（ct FNPT cascade-up 使财务员可见 erp-ct）。**该 finding 是否构成真实「UI 路死授权」必须先经运行时 SiteMap 探针定案**（以生产计划员角色取 `/r/SiteMapApi__getSiteMap` 断言两页面实际可见性），纯静态字面比对不构成修复依据（auth 保护区禁无缺陷依据的授权面变更） | `module-manufacturing/erp-mfg-web/.../erp-mfg.action-auth.xml:100-139,:182-189` | 批次 3 改为探针先行：可见 → 撤销 finding 并修正本报告（静态比对方法教训登记）；不可见 → 按实证重写机制模型后修复。若成立，镜像规则勘误（「子 FNPT roles ⊆ 页面菜单 roles」约束）经 owner doc `roles-and-permissions.md` 裁决登记 |
| F7 | `ErpMdMaterialSku.barcode` 列无 DB 索引（实体索引仅 MATERIAL_ID/UOM_ID/TAX_RATE_ID），USC-05 将其接入**默认开启**（`DEFAULT_BARCODE_ENABLED=true`）的扫码热路径第一跳（`findFirstByExample` 仅 barcode 过滤）；md 域 `findSkuByBarcode` 同列共用。第一轮 md 域 Deferred G1（DB 唯一索引）触发条件实质强化 | `module-master-data/model/app-erp-master-data.orm.xml:381`（barcode 列）+ 同实体 `<indexes>` 段；`ErpInvBarcodeBizModel.java:118-120`；`ErpInvConstants.java:170-176` | 批次 5：orm.xml 增 unique-key/index + 重生成 + 三方言 deploy SQL 派生同步（F1/F2 先例 plan 2026-09-28-0852-2）；**orm.xml 保护区双独立子 agent 批准** |
| F8 | `MaskAuditRecorder` 去重 ThreadLocal（`tlDedup`）跨请求不清零：`clearThreadDedup()` 全仓零调用方；仅 500 键满时整体 clear。USC-03 %prod 字段读审计翻转后生产首次激活该路径 → 池化工作线程跨请求残留去重键 → 同用户同线程再读同字段**审计漏报**（内存有界 ≤500 键/线程，非性能风险）。owner doc E4.2（roles-and-permissions.md:112）已明文「同一请求线程内仅记一次」——实码违反之（code-vs-doc 漂移） | `module-common-service/.../MaskAuditRecorder.java:54,128-130,187` | 批次 4：键载体以实仓设施盘点为前提裁决（`IContext.getTraceId()`——ContextHttpServerFilter 每请求设置/UUID 兜底；或自管 IAttributeSet UUID；或请求边界清理[平台 filter 挂点属外部保护区须升级]），**`IServiceContext` 无 requestId、getExecutionId 恒 null 不可用**（草案审查实证）；JUnit 护栏须驱动生产代码路径 |

### 3.3 P3（体验优化）

| # | 发现 | 位置 | 修复方向 |
|---|------|------|---------|
| F9 | pda 页 2 处 placeholder 无 `i18nEn` 承载（object 块形态范式现成 `disposal-wizard/main.page.yaml:40-42`）；`check-hardcoded-cjk.mjs --strict` 对冻结基线存在新增站点（pda CAT4、fin dashboard CAT4 批次 5 空态文案、`ErpInvBarcodeBizModel.java` Java 侧）须逐站点分类（合法 baseline-raise / 真违规 Fix）并刷新 `cjk-baseline.md` | `pda-stock-take/main.page.yaml:33,39` 等 | 批次 2（pda 页内）+ 批次 4 或 5（baseline 裁决刷新，lessons-07 义务） |
| F10 | pda 页 form api 与 button api 双份同载荷冗余（配置面）；`required` 属性与 flux 校验语法（`validations`）的对应性须随批次 2 改写统一核对 | `pda-stock-take/main.page.yaml` | 批次 2：改写时消解（按钮承载调用 + 表单域 `validations` flux 语法） |
| F11 | **text 节点内嵌 HTML 按字面渲染**（探针截图实证）：PDA 页顶部提示显示原文 `<div class="alert alert-info">…`——flux `text` 渲染 textContent 不解释 HTML（AMIS html 卡片残留族）。disposal-wizard/period-close 等页的 `<strong>` 提示文本同族 | `pda-stock-take/main.page.yaml:13` 等 | pda 页随批次 2 重构消除；其余页登记 Deferred（触发=逐域 UX 迭代）；gotchas G-002 附带登记 |
| F12 | **页面级 `i18nEn` 块无运行时消费者**（探针实证：localStorage 切 en-US 重载后页面标题/label/placeholder/按钮全部保持中文；壳层 chrome（登录页/菜单）双语正常——XML 层 `i18n-en:` 机制工作，page.yaml 层 `i18nEn` 块全链（nop-entropy/nop-chaos*/壳层）零消费者，导出 JSON 原样保留）。当前仅作 CJK checker CAT4 的资源登记载体 | 全部手写 page.yaml 的 `i18nEn` 块 | Deferred（触发=壳层 i18n 渲染迭代或国际化需求立项；修复属 nop-chaos-next 壳层外部保护区）；CAT4 义务照旧保留 i18nEn 块 |
| F13 | **壳层对未注册 `@mutation:` 回退 `{data:...}` 信封，破坏标量参数自定义 mutation**（批次 1 运行时复验实锤：重接线后 startTake 请求到达但报 `unknown-operation-arg: 没有定义参数[data]`——`nop-chaos-next/apps/main/src/services/nopRpcResolver.ts:274-277` 仅适配 `__save` 类 data 参数操作） | 壳层外部仓（不可改）；本仓侧修复 = 标量参数自定义 mutation 用 `/r/Entity__method` 直发 URL（resolver `/r/` 分支原样直达） | 已随批次 1 落地（9 处标量 mutation 改 `/r/` + disposal `__save` registry 平铺；disposal `__save` 去掉作者预包裹改 registry 平铺形态）；规则登记 gotchas G-002 |
| F14 | **stock-take-flow 页数据面与实体错位（预先存在缺陷，非本轮引入）**：`stockTakeData` 查询 selection 含 PageBean 层不存在的 `id` + 列字段 `name`/`status`/`rangeDepartmentId`/`responsibleById` 4/7 不在 ErpInvStockTake 实体（真字段 id/code/orgId/businessDate/takeType/warehouseId/docStatus/approveStatus/posted/remark…）→ 服务端 `undefined-field` 报错、表格恒空；状态文案 `IN_PROGRESS` 与实体持久化值 `CONFIRMED`（盘点中，已登记 label drift）错位 | `stock-take-flow/main.page.yaml:26,33-49` × `module-inventory/model/app-erp-inventory.orm.xml` | 归盘点作业面批次（与 pda 页同域同族，随批次 2 或复扫轮处理）；批次 1 仅修按钮接线（已完成） |

### 3.4 复核后不修（存量面复核结论 + 排除项）

- 上轮全部修复在位（§1 存量面行）；GATE-04 无界查询台账 PASS；flux 门禁 325 error 全为已裁决 `variant="primary"` 基线族零新增；`/graphql` REST 约定零真实回归（仅注释命中）；全库零轮询；内联样式均为既有已裁决族（b2b edi-detail/ct version-diff dashboard tpl + gen-control CANCELLED 删除线），pda 页贡献 0。
- 后端 Deferred 全表（A4/A5、G2/G3、B6、C1、I2、flux warning、看板取数合并、列裁剪、联动范式）触发条件逐条核对未触发、维持登记（agent 核对表存档于本轮扫描记录）。
- `resolveBarcode` 六连点查 fallback 链：单据三类均走 `UK_xxx_CODE_ORG(code,orgId)` 最左前缀索引、PDA 单用户扫码低频，反射为 inv→sal DAG 违规禁依赖下的既定裁决设计——不立项。
- 条码审计写放大：每命中 mutation +1 条 NopAuthOpLog INSERT（异步批吸收、低频人工动作）——可接受；oplog 留存三重缺口与 %test 连接悬挂为在册平台层 successor，非本轮。

---

## 4. 修复批次规划

> 每批 = 一个独立 plan（独立草案审查 → 执行 → 验证 → 独立结束审计 → 独立 git 提交）。命名遵循 `00-plan-authoring-and-execution-guide.md`。

| 批次 | Plan 主题 | 覆盖发现 | 改动面 | 保护区/审批 |
|------|----------|---------|--------|------------|
| 1 | flux 手写页按钮接线修复（探针定案 + 6 存量页改写） | F1（除 pda 页按钮）+ G-登记（flux-integration-gotchas 增补 G-NNN：手写页 AMIS actionType/api/onEvent/dialog 惰性陷阱） | 6 个 page.yaml（14 ajax 按钮 + 12 onEvent 刷新链 + 6 dialog 按钮）+ 浏览器探针 + gotchas 登记 | 无（本仓页面文件非保护区）；flux 源码零改动 |
| 2 | pda-stock-take 全闭环重构 | F1（pda 按钮）+ F2 + F3 + F4 + F5 + F9（pda 部分）+ F10 | 1 个 page.yaml + 可能 1 个后端 BizModel 小改（Decision：前端派生 vs 后端落行钩子）+ E2E 升级为真实点击保存链路 | 无（inv 页面 + 既有 mutation 复用优先）；若动 orm/契约则升级审批 |
| 3 | mfg 菜单角色可达性——运行时探针定案 | F6（假阳性风险注记见 §3.2） | 探针（SiteMap 角色视角）→ 可见则撤销+报告修正，不可见则修复 | **auth/permissions 保护区 plan-first**：无缺陷依据不扩授权面；owner doc `roles-and-permissions.md` + 级联机制证据 |
| 4 | MaskAuditRecorder 去重请求边界修复 | F8 + F9（cjk baseline 裁决刷新顺带） | `MaskAuditRecorder` + 接线点 + JUnit | owner doc `roles-and-permissions.md` E4.2 证据 + 测试（审计域 plan-first 慣例） |
| 5 | ErpMdMaterialSku.barcode 索引 | F7 | 1 个 orm.xml +1 非唯一索引（唯一键归 md 域 Deferred G1）+ 重生成 + 三方言 deploy SQL | **orm.xml 保护区：双独立子 agent 批准**（F1/F2 先例流程） |

**执行顺序**：1 → 2 → 3 → 4 → 5。批次 1 先以最小探针+机械改写验证范式并产出运行时定案；批次 2 复用其范式做 pda 闭环；3/4 为小面独立项；5 需双批准+重生成放最后。

## 5. Deferred But Adjudicated（本轮新增/更新）

| 项 | 分类 | 不做原因 | 重开触发条件 |
|----|------|---------|-------------|
| flux 验证器 prop 表补齐（`i18nEn`/`required` 等误报 unknown-property 的收敛） | out-of-scope improvement | 校验器 prop 表属 nop-chaos-flux 仓（外部保护区）；误报不掩盖真问题（actionType/api 已由源码级定案） | flux 仓 prop 表迭代时 |
| 7 页改写后的 unknown-property warning 归零门禁化（把手写页 warning 纳入冻结基线） | optimization candidate | 待批次 1/2 落地后基线才有意义 | 批次 1/2 完成后的复扫轮 |
| `required` vs `validations` 全库统一核对（手写页表单校验语法面） | optimization candidate | 与批次 2 同面但跨页；先在批次 2 建立范式 | 批次 2 完成后按同范式逐页推广，或 flux 校验语法指南增补时 |
| GenPage 生成页 unknown-property warning 6533 条 | 维持第一轮 Deferred | 生成层 prop 表缺口，app 仓不可达（不变） | nop-entropy 模板迭代时 |
| pda 防重复提交节流/按压式大按钮等 PDA 打磨项 | watch-only residual | 批次 2 先交付功能闭环 | PDA 上量实测反馈 |
| mfg 之外域的 roles 镜像规则全量复查 | watch-only residual | 本轮仅 mfg 实锤错配（USC-02a D3 镜像在全库的其余子 FNPT 抽查未见同型，批次 3 复核留证） | USC-02a 类 roles 批量规则再迭代时全量重扫 |

## 6. 验证与基线义务（适用全部批次）

- 全项目构建：`mvn clean install -DskipTests`（批次 5 须触发重生成）
- 单测：`mvn test -pl <module> -am`（触域模块）；批次 2 含 E2E 真实点击断言
- flux 门禁：`npm run validate:flux` exit 0（对照 325 error 基线零新增；m2 新鲜度前置）
- 无界查询台账：`npm run check:unbounded-queries`（触后端批次）
- 合规检查器：`bash docs/audits/nop-compliance-checker.sh`（lesson 07 义务，actual > baseline 逐站点分类）
- CJK checker --strict：批次 4 完成基线裁决刷新后应回绿
- 每批完成更新 `docs/logs/2026/10-05.md`；每批独立 git 提交
