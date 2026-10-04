# 2026-10-05-0400-2 pda-stock-take 盘点扫码页全闭环重构

> Plan Status: active
> Last Reviewed: 2026-10-05
> Source: docs/analysis/2026-10-05-0353-perf-ux-round2-increment-analysis.md §3.1-F2/F3 + §3.2-F4/F5 + §3.3-F9/F10 + §4 批次 2
> Related: 2026-10-05-0400-1（按钮接线范式先落地）；USC-05 plan 2026-10-01-0930-1（本页初版交付）
> Audit: required

## Current Baseline

- `module-inventory/erp-inv-web/src/main/resources/_vfs/erp/inv/pages/pda-stock-take/main.page.yaml`（88 行，USC-05 交付）存在叠加缺陷：
  1. **按钮惰性**（分析报告 F1 同族）：2 个 AMIS 风格按钮 + 2 处表单级 api（7 页中表单级 api 仅存于此页），运行期无事件计划（plan 1 探针将定案并建立改写范式）。
  2. **保存链后端必败**（F2）：payload 仅 takeId/lineNo/skuId/locationId/actualQuantity，缺 ORM mandatory 列 `materialId`/`uoMId`/`bookQuantity`/`differenceQuantity`（`erp-inv-dao/.../orm/_app.orm.xml:1309-1327`）；`ErpInvStockTakeLineBizModel` 裸 CRUD 无补齐钩子；E2E 从未走通保存。
  3. **lineNo 硬编码 1**（F3）：两处 `lineNo: 1`；StockTakeLine 实体无 unique-keys；连续扫码语义未定义。
  4. **盘点单手输数字主键**（F4）：`takeIdInput` 裸 input-text；`resolveBarcode` STOCK_TAKE 分支（`ErpInvBarcodeBizModel.java:88`，解析结果含强制 locationCode）已实现但页面零消费。
  5. **解析结果零展示 + 跨表单引用脆弱**（F5）：`${scanForm.refId}`/`${scanForm.location.locationId}` 全库无先例；无 messages、无成功后重置、无已录行反馈。
  6. **placeholder×2 无 i18nEn**（F9 部分）+ form/button api 双份冗余（F10）。
- 状态机事实（草案审查 round1 实核）：盘点单 `docStatus` 复用字典 `erp-inv/move-status`；owner doc `docs/design/inventory/state-machine.md:158-170` 状态机 = DRAFT → 盘点中 → DONE/CANCELLED，**无 OPEN 值**，「盘点中」实际持久化为 `CONFIRMED`（已登记的 doc label drift）；守卫权威载体 = `ErpInvStockTakeStateMachine` Bean（`ErpInvStockTakeBizModel.startTake:47-52` 实证）。允许 PDA 落行的 docStatus 白名单是本计划核心业务裁决。
- 数据事实：`resolveSku` 返回 skuInfo 仅含 **skuCode/barcode/materialId，不含 uoMId**（`ErpInvBarcodeBizModel.java:128-133`）；`ErpInvStockTake__findPage` 已存在且被 stock-take-flow 页使用（select 兜底数据源现成）；`startTake` 纯状态翻转、不预生成行（D2「新行」倾向与既有生命周期兼容）。
- 设计契约：`docs/design/inventory/barcode-integration.md` §PDA 场景 5（扫 SKU+库位→输实盘数量）+ `pda-require-location-scan` 配置（STOCK_TAKE 分支 locationCode 必填）。
- 既有护栏：JUnit `TestErpInvBarcodeResolve` 7/7；既有 E2E `tests/e2e/mobile/pda-stock-take.mobile.spec.ts`（纯可达性/渲染断言）。E2E cleanup 通道可行（草案审查已验证）：StockTakeLine 裸 CRUD 经 `_helper.ts:224-239` `__delete` 逻辑删范式删行；盘点单 docStatus 可经 CRUD update 还原。
- plan 1 将建立惰性按钮→flux 原生接线的改写范式与运行时证据。

## Goals

- PDA 盘点扫码形成**真实可用闭环**：扫/输条码 → 解析（结果可见）→ 定位盘点单（扫码或选择，非手输 ID）→ 输实盘数量 → 保存成功（后端校验通过、行落库）→ 反馈与重置。
- 保存链服务端必败问题消除：materialId/uoMId/bookQuantity/differenceQuantity 由确定性来源补齐。
- 行号生成与连续扫码语义明确；跨表单脆弱引用消解；messages/reset/解析展示/i18nEn 补齐。
- E2E 升级为真实点击驱动的保存链路断言（修复「两层测试之间的按钮点击缝」）。

## Non-Goals

- 盘点审批/过账/差异处理流程（既有 StockTake 状态机不动）。
- orm.xml/api.xml 契约变更（若 Decision 选中后端新 mutation，仅为 BizModel 方法新增，不动 orm/api.xml；一旦执行中发现必须动 orm/api.xml，停下走保护区双批准）。
- PDA 打磨项（防重复提交节流、按压式大按钮、已录行清单列表化——watch-only，见 Deferred）。
- 6 个存量页改写（plan 1 范围）。

## Task Route

- Type: `bug investigation → implementation-only change`（叠加缺陷修复 + 1 个新增服务方法候选）
- Owner Docs: `docs/design/inventory/barcode-integration.md`（PDA 场景 5 契约）、`docs/design/inventory/state-machine.md`（盘点单状态机与 docStatus 权威）、`docs/architecture/i18n-compliance.md`（文案双语义务）
- Skill Selection Basis: `flux-rendering-conformance-audit-prompt`（接线与校验语法合规）；按 `../nop-entropy/docs-for-ai/03-runbooks/write-bizmodel-method.md`（若走后端 mutation）；`development-wisdom-gate`（自检）

## Infrastructure And Config Prereqs

- 本地 runner（E2E 真实点击用）；`barcode-enabled` 默认开启（生产 application.yaml 已有键）。
- flux 门禁 m2 新鲜度前置（改 yaml 后先 install 触域模块）。
- 无新增外部服务/端口依赖。

## Execution Plan

### Phase 1 - 保存链设计裁决（Decision-first）

Status: planned
Targets: 本计划 Decision 记录 + `docs/design/inventory/barcode-integration.md` 注记（按 D4 结论触达或不触达，无弱措辞）
Skill: none（业务裁决，依据 owner doc）

- Item Types: `Decision`

- [ ] Decision: **payload 补齐路径**。候选：(a) 纯前端派生——解析返回 skuInfo 含 materialId 可带出，但 **uoMId 不在返回内需二次查询**（SKU 行有 uoMId 列），bookQuantity 亦需按 sku+location 的库存查询接口（多次往返 + 跨表单引用仍脆弱）；(b) 后端新增 `@BizMutation scanSave`（或同名语义）于 ErpInvStockTakeLineBizModel——入参 takeOrderCode/takeId、skuBarcode 或 skuId、locationCode、actualQuantity；服务端派生 materialId/uoMId、查询账面数量、计算 differenceQuantity、分配 nextLineNo、校验盘点单状态守卫，单次往返落行。记录选择、替代方案与残留风险；倾向 (b)（PDA 瘦客户端/单往返/派生逻辑单一真相源/天然消解跨表单引用），以实仓接口盘点后定稿。
- [ ] Decision: **bookQuantity 数据来源**（(b) 定稿条件）：现存量快照（StockBalance 表按 sku+location）vs 其他口径——以 inventory 域 owner doc 的账面数量语义裁决（completeTake 用「盘点行字段快照对账」先例暗示落行时点快照）。
- [ ] Decision: **连续扫码行语义**（同 SKU+库位再次保存）：新行（每次盘点录入独立成行，过账时聚合）vs 累加既有行。以 `docs/design/inventory/` stock-take 行语义 owner doc 为准裁决并回写注记；默认倾向新行（经典盘点行为、可追溯每笔录入，startTake 不预生成行的既有生命周期兼容）。
- [ ] Decision: **盘点单状态守卫**：允许 PDA 落行的 docStatus 白名单（候选 DRAFT / CONFIRMED=盘点中）+ 守卫实现载体（`ErpInvStockTakeStateMachine` Bean 沿用，不旁路）；以 state-machine.md owner doc 裁决（预期「盘点中」=CONFIRMED 为作业窗口）。
- [ ] Decision: **盘点单定位形态**：消费 `resolveBarcode` STOCK_TAKE 分支（扫盘点单条码带回 takeId+强制 locationCode）为主，辅以 `ErpInvStockTake__findPage`（现成）按状态白名单过滤的 select 兜底。

Exit Criteria:

- [ ] 五个 Decision 全部落盘（选择/替代/残留风险），与 owner doc 无冲突（有冲突则先对齐 owner doc）

### Phase 2 - 服务端落行实现（若 Phase 1 选 (b)）

Status: planned
Targets: `module-inventory/erp-inv-service/`（BizModel + 必要的 Errors/i18n 键）
Skill: development-wisdom-gate

- Item Types: `Add | Proof`
- Prereqs: Phase 1；若选 (a) 本阶段整体跳过并在计划记录（范围修订理由）。

- [ ] Add: 新增 scanSave（名以 Phase 1 裁决为准）@BizMutation：参数校验（barcode-enabled 门控沿用、locationCode 按 `pda-require-location-scan` 必填）、SKU 解析（复用 resolveBarcode 内部路径或 findFirstByExample）、盘点单 docStatus 白名单守卫（D4 裁决，StateMachine Bean 载体）、nextLineNo 分配（头下 max(lineNo)+1，并发面如实登记——PDA 单人单单场景）、materialId/uoMId 派生、bookQuantity 取数（D2 裁决口径）、differenceQuantity=actual-book 落行。
- [ ] Add: ErrorCode（`erp.err.inv` 前缀）+ zh/en i18n 键；`@Inject` 非 private；业务异常 extends NopException（平台规则）。
- [ ] Proof: JUnit（module-inv-service）——正常落行断言（含派生字段值/lineNo 递增）、盘点单状态白名单外拒绝、未知条码具名拒绝、locationCode 缺失拒绝；既有 TestErpInvStockTakeCompleteDiffMove 等零回归。

Exit Criteria:

- [ ] scanSave 落行行为有 JUnit 断言覆盖（不止签名存在）
- [ ] 触域模块 `mvn test -pl module-inventory/erp-inv-service -am` 绿

### Phase 3 - 页面重构

Status: planned
Targets: `pda-stock-take/main.page.yaml`
Skill: flux-rendering-conformance-audit-prompt

- Item Types: `Fix | Add`
- Prereqs: Phase 1（+Phase 2 若适用）；plan 1 范式（onClick ajax 接线 + confirmText/messages 兄弟键 + then 链）。

- [ ] Fix: 按钮改 flux 原生接线（plan 1 范式）；2 处表单级 api 消解（调用由按钮/表单提交语义承载）；跨表单 `${scanForm.*}` 引用消解——改单表单域或 Phase 2 后端 mutation 单请求承载。
- [ ] Add: 解析成功结果展示区（SKU/物料/库位/盘点单只读回显）；messages.success/failed（i18n 双语）；保存成功后表单重置（连续扫码循环）；盘点单定位（D5 裁决形态）；placeholder×2 i18nEn object 块承载（`disposal-wizard/main.page.yaml:40-42` 范式）。
- [ ] Proof: `npm run validate:flux` 该页 error=0、unknown-property 仅余 i18nEn 类已定性误报族（对照改写前后 warning 清单留证）。

Exit Criteria:

- [ ] 页面按闭环重写完成，validate:flux 该页无 error 且无 actionType/api/form-api 残留
- [ ] i18n 双语覆盖（含 placeholder）——`check-hardcoded-cjk.mjs` report 模式该页新增 0

### Phase 4 - E2E 升级 + 文档对齐

Status: planned
Targets: `tests/e2e/mobile/pda-stock-take.mobile.spec.ts`、`docs/design/inventory/barcode-integration.md`
Skill: browser-use:web-gui-tester（调试期）；spec 按 runbook 规范

- Item Types: `Fix | Proof`

- [ ] Fix: E2E 升级为真实点击驱动：输入条码 → 点击解析 → 断言解析结果展示与 /r/ 请求发出 → 定位盘点单 → 输数量 → 点击保存 → 断言保存请求成功 + verifyState 查询落行值（派生字段/lineNo）→ cleanup（`tests/e2e/business-actions/_helper.ts:224-239` `__delete` 范式删行 + CRUD update 还原盘点单 docStatus——通道已验证可行；mobile 层跨目录复用该 helper）。移动视口 375×812 按 runbook 移动分层。
- [ ] Proof: 新 spec 本地绿；既有 mobile 分层零回归。
- [ ] Proof（文档）: `barcode-integration.md` §PDA 场景 5 状态注记更新（页面由「表单式作业面（后端 JUnit 承载语义）」升级为「浏览器层真实闭环断言」）；分析报告 §3.1-F2/F3/§3.2-F4/F5 状态回填。

Exit Criteria:

- [ ] E2E 真实点击保存链路绿（命令输出留证）
- [ ] owner doc + 分析报告状态回填完成

## Draft Review Record

- Independent draft review iteration 1: needs revision (agent_d0a0c323) — 0 BLOCKER / 3 MAJOR / 5 MINOR：M1「OPEN 状态盘点单」不存在（实态 DRAFT/CONFIRMED=盘点中/DONE/CANCELLED，须增状态守卫 Decision）/ M2 owner doc 路径错 `docs/design/inventory/barcode-integration.md` ×4 / M3 skuInfo 不含 uoMId（D1(a) 前提失真）+ m1~m5（bookQuantity 来源升 Decision/cleanup 通道注记/88 行/弱措辞/env.confirm 联动提示）。
- Independent draft review iteration 2: accept (agent_e9902535) after <M1 废 OPEN、D4 状态守卫 Decision（StateMachine Bean 实核在位）；M2 路径四处修正实存；M3 skuInfo 实码核对吻合；m1/m2/m3/m4 全落实；无阻塞新问题（minor：cleanup 通道引文路径顺手补全为 tests/e2e/business-actions/_helper.ts>

## Closure Gates

- [ ] 范围内行为完成（闭环：解析可见→定位→保存成功→反馈重置，浏览器真实点击断言）
- [ ] 相关文档对齐（barcode-integration.md + 分析报告回填 + Phase 1 Decision 触发的 owner doc 注记）
- [ ] 已运行验证（全量 `mvn clean install -DskipTests` + 触域 test + 新旧 E2E + `npm run validate:flux` 零新增 + `check-hardcoded-cjk.mjs` report 0 新增 + 合规检查器 exit 0）
- [ ] 无范围内项目降级为 deferred/follow-up（Phase 2 若跳过须有 Phase 1 显式裁决记录）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### PDA 打磨项（防重复提交节流/大按钮/已录行清单列表化）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 功能闭环为本计划结果表面；打磨项无功能正确性影响，待 PDA 上量实测反馈驱动。
- Successor Required: `no`（触发=PDA 上量实测反馈）

### 同 SKU+库位累加语义（若 Phase 1 D3 裁决新行）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: owner doc 现行语义承载；累加语义属产品迭代。
- Successor Required: `no`（触发=inventory owner doc 修订盘点行语义）

## Closure

Status Note: <closure 时填写>

Closure Audit Evidence:

- Auditor / Agent: <independent subagent>
- Evidence: <task id / log link>

Follow-up:

- <非阻塞跟进项>
