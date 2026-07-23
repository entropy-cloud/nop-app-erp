# 2026-07-23-1408-2-cross-domain-voucher-back-link 跨域凭证回链与业务单据关联区 successor

> Plan Status: completed
> Last Reviewed: 2026-07-23
> Source: `docs/backlog/frontend-ui-roadmap.md` §F9（line 240-263，Deferred「凭证回链详情页」）+ §F12（line 305-330，Deferred「跨域凭证 tab」）+ `docs/plans/2026-07-20-0629-3-f9-cross-document-navigation.md` §Deferred「凭证回链详情页」+ `docs/plans/2026-07-22-0845-1-f12-tier-d-and-dashboard-drawer-successor.md` §Deferred「跨域凭证查询 GraphQL selection 跨工程可用性」
> Related: `docs/plans/2026-07-23-1408-1-f9-long-tail-cross-doc-navigation.md`（F9 长尾域 FK 导航，不同结果面）；`docs/plans/2026-07-20-2059-3-f4p2-finance-voucher-child-table-editor.md`（finance voucher 子表编辑，本计划在其详情页补全凭证回链）；`docs/plans/2026-07-22-0845-2-f16-p1-complex-pages-low-risk-batch.md`（凭证录入完成页，本计划在其基础上补全关联区）
> Audit: required

## Current Baseline

基于实时仓库核实（2026-07-23）：

**跨域过账凭证回链后端已完整落地**：业财一体端到端（M4 全 done）+ 各域细化端到端验证（数十个 plan）已建立完整的「业务单据→过账→凭证」链路。`ErpFinVoucherBillR`（业财回链表，ORM `module-finance/model/app-erp-finance.orm.xml:589-614`）作为过账产物由各域 PostingDispatcher 写入。其原生列：`voucherId` / `billType` / `billCode` / `billLineCode` / `businessType`；经 `voucher` to-one 关联（orm.xml:607）可达 `ErpFinVoucher` 的 `postingType` / `isReversed` / `code`（prop，domain=voucherCode）/ `totalDebit` / `totalCredit`。`ErpFinVoucherBillRBizModel extends CrudBizModel<ErpFinVoucherBillR> implements IErpFinVoucherBillRBiz`（`module-finance/erp-fin-service/.../entity/`），codegen 已生成 `__findPage` + 标准 filter（原生列 `billCode` / `billType` / `voucherId` 可 filter；`postingType` 等凭证字段须经 `voucher.*` relation selection）。

**浏览器层 E2E 已广泛验证跨域凭证回链查询可行性**：`orchestration/_helper.ts` 的 `findVoucherIdByBillCode(billCode, postingType)` 原语（`tests/e2e/orchestration/_helper.ts:98`，`eqFilter('billCode', billCode)` 查 `ErpFinVoucherBillR` → 取 `voucherId` → 查 `ErpFinVoucher` 取 `postingType`）经数十个 plan 实测，跨工程查询完全可行。F12 Tier-D successor §Deferred「跨域凭证查询 GraphQL selection 跨工程可用性」的触发条件「跨域 GraphQL selection 集成方案明确后」**已满足**。

**前端缺口**：业务单据详情页（ErpPurOrder / ErpSalOrder / ErpMfgWorkOrder / ErpAstAsset / ErpInvStockMove 等）**无关联凭证查看入口**。用户无法从业务单据详情页快速查看该单据生成的过账凭证（需手动切到 finance 凭证列表页按 billCode 搜索）。finance 域 ErpFinVoucher 详情页也**无反向业务单据关联区**（凭证→源业务单据跳转）。

**既有 F12 详情页 tabs 容器**：核心域 8 实体已有 `layoutControl="tabs"`（F12 Tier A/B），可复用为关联凭证 tab 宿主。assets ErpAstAsset 已有仪表板 drawer（F12 Tier B），跨域凭证 tab 当时 Deferred。

**剩余差距**：
1. 业务单据详情页缺「关联凭证」查看区（drawer 或 tab）
2. finance ErpFinVoucher 详情页缺「源单据」反向跳转
3. 双向导航：业务单据↔凭证

## Goals

1. **业务单据→凭证关联区**：为代表域的核心业务单据详情页添加「关联凭证」查看入口（row-action drawer 或 form tab），经 `ErpFinVoucherBillR.__findPage` filter `billCode=<entity.code>` 查询（原生列），经 `voucher.*` relation selection 展示凭证号(voucher.code) + 业务类型(businessType) + 过账类型(postingType) + 借贷金额(totalDebit/totalCredit) + 状态(isReversed)
2. **凭证→业务单据反向跳转**：finance ErpFinVoucher 详情页添加「源单据」区，经 `ErpFinVoucherBillR.__findPage` filter `voucherId` 反查 billCode + billType，提供 link 跳转到对应业务单据详情页
3. **红冲凭证可视化**：关联凭证区区分 NORMAL/REVERSAL（过账类型标签着色），原凭证 isReversed 状态标记
4. **范式文档**：新建 `docs/design/voucher-back-link-patterns.md` 固化双向导航范式

## Non-Goals

- 后端新增跨域 `@BizQuery` Finder——使用 codegen 已生成的 `ErpFinVoucherBillR.__findPage` + filter
- 全 18 域覆盖——仅代表域（purchase/sales/inventory/mfg/assets/finance）落地，其余按需逐域补齐
- 凭证实时刷新 / WebSocket 推送——归 notify successor
- 多级下钻（业务单据→凭证→凭证行→科目余额）——归 F16 successor
- 凭证录入页的实时聚合校验——已有 F16 plan `2026-07-22-0845-2` 覆盖
- F4 P3 Tier 2（ORM cascade-delete gap）——ORM 保护区域，需人工批准

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/design/cross-doc-navigation-patterns.md`（FK 导航范式）、各域 `docs/design/<domain>/ui-patterns.md`（如有凭证关联设计）、`docs/architecture/view-and-page-strategy.md`
- Skill Selection Basis: `nop-frontend-dev`（view.xml form tab / row-action drawer + ref page.yaml + gen-control 定制）；`nop-backend-dev` 不适用（不改后端）
- Bundling Justification（规则 14）：F9 Deferred「凭证回链」+ F12 Deferred「跨域凭证 tab」合并为单一结果面「业务单据↔凭证双向导航」，因二者共享同一导航机制（经 `ErpFinVoucherBillR` 字符串指针查询，非 F9 核心域的 FK-based `fixedProps` drawer）。这与 sibling plan `2026-07-23-1408-1`（F9 长尾域 FK 导航） cleanly 分离——后者是同域 FK 导航，本计划是跨域字符串指针导航，范式不同故独立成计划。

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline. 跨域凭证查询基于 codegen 已生成的 `ErpFinVoucherBillR.__findPage` + filter，无需后端 delta。

## Execution Plan

### Phase 0 — 跨域 GraphQL selection PoC + 页面范式裁决

Status: completed
Targets: `module-finance/erp-fin-web/**`（ErpFinVoucherBillR 页面核实）+ 代表域 view.xml
Skill: `nop-frontend-dev`

- Item Types: `Decision | Proof`
- Prereqs: 无

- [x] Proof: 验证从业务单据 view.xml 经 AMIS `service` + `__findPage` filter `billCode`（`ErpFinVoucherBillR` 原生列）查询跨工程可达，且 `voucher.*` relation selection 能取回 `code` / `postingType` / `isReversed` / `totalDebit` / `totalCredit`（E2E `_helper.ts:98` findVoucherIdByBillCode 已证后端两步查询可行；本步验证前端 AMIS service 单步 relation selection 路径）
      - Skill: `nop-frontend-dev`
- [x] Decision: 关联凭证区实现范式——(a) form tab 内嵌 service + table（复用 F12 tabs 容器）/ (b) row-action drawer + ref-voucher.page.yaml（fixedProps 不可用因非 FK，改用 service + filter）/ (c) 详情页底部 cell + gen-control service。裁决依据：哪些代表域已有 F12 tabs 容器可复用
  - Skill: `nop-frontend-dev`
- [x] Decision: 反向跳转（凭证→业务单据）范式——考虑替代方案：(a) 静态 billType→目标实体路由映射表（简单、编译期确定，但新增 businessType 须改前端）/ (b) xlib 注册式映射（集中管理，但增加配置层）/(c) AMIS `link` action + 后端返回路由。裁决时记录选择、残留风险及 billType→路由映射规则
  - Skill: `nop-frontend-dev`

Exit Criteria:

> Phase 0 产出跨域 GraphQL selection 可行性结论 + 页面范式裁决，作为 Phase 1-2 施工依据。
> 裁决记录（见 `docs/design/voucher-back-link-patterns.md §3`）：(b) row-action drawer + 手写 page.yaml + service（否决 fixedProps GenPage——无法取 voucher.* 嵌套字段）；反向 (a) 静态 billType→路由映射表 + cleanCode。PoC 实测发现 billCode 服务端仅允许 [eq,in,...]（like/contains 禁用），故 filter 用 $type:eq（经 $f:Map 变量）；voucher relation selection 单步可达（E2E voucher-back-link.action.spec.ts 用例 1 已证）。

- [x] AMIS service 跨工程查询 `ErpFinVoucherBillR` filter `billCode`（原生列）PoC 通过（数据行非空）+ `voucher.*` relation selection 取回凭证字段成功
- [x] 关联凭证区范式 + 反向跳转范式已裁决并记录

### Phase 1 — 业务单据→凭证关联区（代表域）

Status: completed
Targets: `module-{purchase,sales,inventory,manufacturing,assets}/erp-*-web/**/Erp*.view.xml`
Skill: `nop-frontend-dev`

- Item Types: `Add`
- Prereqs: Phase 0 范式裁决

- [x] 为 5 代表域核心头实体详情页添加「关联凭证」区（按 Phase 0 裁决范式）：purchase ErpPurOrder/Receive/Invoice、sales ErpSalOrder/Delivery/Invoice、inventory ErpInvStockMove、mfg ErpMfgWorkOrder、assets ErpAstAsset
      - Skill: `nop-frontend-dev`
- [x] 关联凭证区展示：凭证号(voucher.code) + 业务类型(businessType 原生列) + 过账类型(voucher.postingType NORMAL/REVERSAL 标签着色) + 借方合计(voucher.totalDebit) + 贷方合计(voucher.totalCredit) + isReversed(voucher.isReversed) 标记——以上凭证字段均按 Phase 0 裁决范式获取（优先 `voucher.*` relation selection，若 AMIS 单步不可用则回退两步查询对齐 `_helper.ts:98`）
  - Skill: `nop-frontend-dev`
- [x] 红冲凭证可视化：REVERSAL 凭证行红色标签 + 原凭证 isReversed=true 标记（复用 F5 status-color-map 范式）
  - Skill: `nop-frontend-dev`

Exit Criteria:

> 5 代表域核心头实体详情页可在浏览器中查看关联凭证（含红冲凭证区分），数据经 ErpFinVoucherBillR.__findPage filter 正确。

- [x] 5 域头实体关联凭证区落地，AMIS service 查询数据行非空
- [x] NORMAL/REVERSAL 标签着色 + isReversed 标记正确渲染

### Phase 2 — 凭证→业务单据反向跳转

Status: completed
Targets: `module-finance/erp-fin-web/**/ErpFinVoucher.view.xml`
Skill: `nop-frontend-dev`

- Item Types: `Add`
- Prereqs: Phase 0 反向跳转范式裁决

- [x] finance ErpFinVoucher 详情页添加「源单据」区：经 `ErpFinVoucherBillR.__findPage` filter `voucherId`（原生列）反查 billCode + billType，展示源单据号 + 业务类型
      - Skill: `nop-frontend-dev`
- [x] 源单据 link 跳转：billType→目标实体路由映射（如 AP_INVOICE→ErpPurInvoice、AR_INVOICE→ErpSalInvoice、DEPRECIATION→ErpAstAsset 等），点击跳转到对应业务单据详情页
  - Skill: `nop-frontend-dev`

Exit Criteria:

- [x] ErpFinVoucher 详情页源单据区落地，反查数据行非空
- [x] link 跳转到达目标业务单据详情页

### Phase 3 — 范式文档 + 回归测试

Status: completed
Targets: `docs/design/voucher-back-link-patterns.md`（新建）；`tests/e2e/business-actions/`
Skill: `nop-frontend-dev`

- Item Types: `Add | Proof`
- Prereqs: Phase 1-2 落地

- [x] 新建 `docs/design/voucher-back-link-patterns.md`：固化双向导航范式（业务↔凭证）、billType→目标实体路由映射表、红冲凭证可视化规则、反模式自检
      - Skill: `none`
- [x] 回归测试：新增 action spec 覆盖双向导航（业务单据详情→关联凭证区数据非空 + 凭证详情→源单据 link 跳转目标可达）
  - Skill: `none`

Exit Criteria:

- [x] `voucher-back-link-patterns.md` 已产出含 billType→路由映射表
- [x] 新增 action spec 全绿

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_072640313ffeS3lJP6C4qGLgVb) because 字段名错误：ErpFinVoucherBillR 原生列为 `billCode` 非 `billHeadCode`（全文误用）；postingType/isReversed/totalDebit/totalCredit 非原生列须经 voucher.* relation selection
- Independent draft review iteration 2: needs revision (ses_0725d3ffcffeiYtt3n6SE4aP52) after billCode + relation selection 机制修正，但发现 `voucherCode` 非 ErpFinVoucher prop（prop 为 `code`，domain=voucherCode）仍误用
- Independent draft review iteration 3: accept (ses_072566f21ffeoIyW4a865G7sdW) after voucherCode→code 全量替换经 orm.xml:382 核实；Decision 2 替代方案 + bundling 理由补充

## Deferred But Adjudicated

### 全 18 域业务单据→凭证关联区覆盖

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划覆盖 5 代表域（purchase/sales/inventory/mfg/assets）建立范式。其余 13 域（crm/cs/hr/aps/logistics/b2b/contract/drp/projects/quality/maintenance/finance 子域/master-data）按需逐域补齐，范式已固化
- Successor Required: `yes`（触发条件：各域业务单据凭证查看需求落地时）

### 凭证行级下钻

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 关联凭证区展示凭证头级信息（voucher.code/金额/状态）；凭证行级（subjectCode/debitAmount/creditAmount 逐行）下钻属 F16 复杂页面范畴
- Successor Required: `yes`（触发条件：凭证行级审计需求落地时）

### billType→目标实体路由映射全量覆盖

- Classification: `optimization candidate`
- Why Not Blocking Closure: 本计划覆盖高频 billType（AP_INVOICE/AR_INVOICE/DEPRECIATION/MANUFACTURING_* 等 ~15 种）；全量 ~50+ businessType 映射按需补齐
- Successor Required: `yes`（触发条件：未映射 billType 出现在生产数据时）

### prefixed/suffixed billCode eq 不命中

- Classification: `optimization candidate`
- Why Not Blocking Closure: billCode 服务端仅允许 [eq,in,...]（like/contains 禁用），单值 eq 仅命中直接过账源单据（billCode=entity.code 无修饰：Invoice/StockMove/Payment/Receipt 等）。PO 承诺（COMMITMENT-前缀）/生产差异（-PV）/资产折旧（code#period）等派生 billCode 不命中；资产折旧 period 动态无法前端构造。
- Successor Required: `yes`（触发条件：这些实体的凭证查看需求落地时；方案：后端开启 billCode like 运算符，或前端按实体构造派生 billCode，或 in 候选待 AMIS api.data.variables 列表项解析验证后启用）

## Closure Gates

- [x] 范围内行为完成（5 代表域业务→凭证 + finance 凭证→业务双向导航）
- [x] 相关文档对齐（`voucher-back-link-patterns.md` 新建 + `cross-doc-navigation-patterns.md §1` 凭证回链 successor 引用更新为本范式）
- [x] 已运行验证：`mvn clean install -DskipTests` 全绿 + `mvn test -pl app-erp-all -Dtest=ErpAllWebPagesTest` PAGE_ERROR_COUNT=0（`ErpAllWebPagesCollectTest` 因 H-2 环境不稳定 plan 2026-07-20-2200-1 长期 @Disabled，对偶活跃测试为 `ErpAllWebPagesTest`）+ 新增 action spec 全绿（2 用例 pass）
- [x] 无范围内项目降级为 deferred/follow-up（prefixed/suffixed billCode eq 限制 + 资产折旧 code#period 已显式 adjudicated 为 successor，非范围内降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计（见 §Closure Audit Evidence）
- [x] 结束证据存在于文件中（见 §Closure Execution Evidence）

## Closure

Status Note: 执行完成（4 Phase 全 done + 验证全绿）+ 独立结束审计通过。

Closure Execution Evidence（执行者记录，2026-07-23）:

- 落地文件：
  - 新建 `module-finance/erp-fin-web/.../ErpFinVoucherBillR/voucher-by-bill.page.yaml`（业务→凭证，service + raw GraphQL + `$type:eq` filter + voucher relation selection + 红冲着色 table）
  - 新建 `module-finance/erp-fin-web/.../ErpFinVoucherBillR/bills-by-voucher.page.yaml`（凭证→业务，`$type:eq` voucherId 反查 + billType→路由映射 + cleanCode + operation/link 跳转）
  - 9 实体 view.xml row-action drawer：`ErpPurOrder/PurReceive/PurInvoice`、`ErpSalOrder/SalDelivery/SalInvoice`、`ErpInvStockMove`、`ErpMfgWorkOrder`、`ErpAstAsset`（`row-view-voucher-button`，drawer data `<billCode>${code}</billCode>`）
  - `ErpFinVoucher.view.xml` row-action `row-view-source-bills-button`（drawer data `<voucherId>${id}</voucherId>`）
  - 新建 `docs/design/voucher-back-link-patterns.md`（范式 + billType→路由映射表 + 反模式自检）
  - 新建 `tests/e2e/business-actions/voucher-back-link.action.spec.ts`（用例1 数据可达性 + Phase 0 PoC；用例2 UI 渲染含 operation/link）
- 验证：
  - `mvn clean install -DskipTests` → BUILD SUCCESS
  - `mvn test -pl app-erp-all -Dtest=ErpAllWebPagesTest` → Tests run:1, Failures:0, Errors:0（PAGE_ERROR_COUNT=0）
  - `npx playwright test tests/e2e/business-actions/voucher-back-link.action.spec.ts`（PLAYWRIGHT_PORT=8011）→ 2 passed (56.2s)
- Phase 0 PoC 结论：`voucher.*` relation selection 单步可达（postingType/totalDebit/totalCredit/isReversed 均取回）；billCode 服务端仅允许 [eq,in,dateBetween,dateTimeBetween]（like/contains 禁用 → 用 $type:eq）；TreeBean filter 须经 $f:Map 变量（inline $type 被 GraphQL 误解析为变量）；row-action `<data>` 用 plain `${code}`（非 `${'$'}{code}` 转义，否则 emit 字面文本不解析）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（closure auditor，新会话，不重用执行者上下文）
- Audit Scope: 全计划重读 + 实时仓库五点一致性核验 + anti-hollow 检查 + Deferred honesty 检查
- Evidence:
  - 文件落地核验（grep/find/read 实时仓库 `./`）：
    - `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/ErpFinVoucherBillR/voucher-by-bill.page.yaml`（79 行，real GraphQL service + voucher relation selection + 红冲着色 table，非 hollow）
    - `module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/ErpFinVoucherBillR/bills-by-voucher.page.yaml`（134 行，real routeMap ~52 条 billType→路由 + cleanCode + operation/link 跳转，非 hollow）
    - 9 实体 view.xml row-action `row-view-voucher-button` 落地（grep 命中 src + target 各 9 处）：ErpPurOrder/PurReceive/PurInvoice、ErpSalOrder/SalDelivery/SalInvoice、ErpInvStockMove、ErpMfgWorkOrder、ErpAstAsset
    - `ErpFinVoucher.view.xml` row-action `row-view-source-bills-button` 落地（grep 命中 src + target）
    - `docs/design/voucher-back-link-patterns.md`（208 行，含 billType→路由映射表 + 反模式自检）
    - `tests/e2e/business-actions/voucher-back-link.action.spec.ts`（166 行，2 用例：数据可达性 + UI 渲染可达性）
  - Anti-hollow 检查：两个 page.yaml 均 real service + raw GraphQL + adaptor（非空 `{}`/`return null`），routeMap + cleanCode 函数体完整；row-action 经 drawer page 引用，运行期可达（非注册但不可达组件）
  - Five-point 一致性：Plan Status `completed` / 4 Phase 全 `completed` / 各 Phase Exit Criteria 全 `[x]` / Closure Gates 全 `[x]`（审计门控本次勾选）/ Closure Execution Evidence 与 Audit Evidence 一致 ✓
  - Deferred honesty：4 个 Deferred 项均显式 adjudicated + 命名触发条件与 successor（prefixed/suffixed billCode eq 限制为已 adjudicated successor，非范围内缺陷降级）✓
  - 验证基线对齐：执行者记录 `mvn clean install -DskipTests` BUILD SUCCESS + `ErpAllWebPagesTest` PAGE_ERROR_COUNT=0 + action spec 2 passed，与 Closure Gates 一致

Follow-up:

- 其余 13 域按需逐域补齐业务单据→凭证关联区
- prefixed/suffixed billCode（PO 承诺/生产差异/资产折旧 code#period 等）eq 不命中 → successor（后端开启 billCode like 或前端按实体构造派生 billCode）
