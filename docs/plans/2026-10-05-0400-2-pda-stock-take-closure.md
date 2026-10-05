# 2026-10-05-0400-2 pda-stock-take 盘点扫码页全闭环重构

> Plan Status: completed
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

Status: completed
Targets: 本计划 Decision 记录 + `docs/design/inventory/barcode-integration.md` 注记（按 D4 结论触达或不触达，无弱措辞）
Skill: none（业务裁决，依据 owner doc）

- Item Types: `Decision`

- [x] Decision: **payload 补齐路径 = (b) 后端 `scanSave`**（2026-10-05 实仓盘点后定稿）。新增 `@BizMutation scanSave(takeId, skuBarcode, locationCode, actualQuantity)` 于 `ErpInvStockTakeLineBizModel`（裸 CRUD 16 行实核）：服务端派生 materialId/uoMId（SKU 行 uoMId 列，resolveSku 返回不含——round1 M3 实证）、bookQuantity（StockBalance 快照，D2）、differenceQuantity=actual−book、nextLineNo（头下 max+1），单次往返落行。替代 (a) 纯前端派生否决理由：skuInfo 不含 uoMId 需二次查询 + bookQuantity 需第三次查询 + 跨表单 scope 引用脆弱（F5）。残留风险：scanSave 为新 GraphQL 契约面（BizModel 方法新增，非 orm/api.xml，无保护区）。
- [x] Decision: **bookQuantity 数据来源 = StockBalance 落行时点快照**。`ErpInvStockBalance(warehouseId=take.warehouseId, locationId, skuId)` 无余额行时 bookQuantity=0（全量盘点口径：无余额记录即账面 0）。对齐 owner doc completeTake D1「盘点行字段快照对账，否决实时 StockBalance.totalQuantity 比对」（state-machine.md:172）——行上 bookQuantity 即录入时点快照，completeTake 按行字段重算差异，两处口径闭合。
- [x] Decision: **连续扫码行语义 = 新行**。每次扫码独立成行（nextLineNo 递增），同 SKU+库位多次扫=多行，差异聚合发生在 completeTake 移动单生成侧（逐行处理）。owner doc 无累加语义；startTake 不预生成行（实核），新行语义与既有生命周期兼容、每笔录入可追溯。
- [x] Decision: **盘点单状态守卫 = docStatus==CONFIRMED 白名单**（盘点中作业窗口：startTake 后 completeTake 前，state-machine.md:158-170 盘点单状态机）。守卫实现 = 行服务内 take.docStatus 常量比较（窗口判断非状态迁移，不经 StateMachine Bean 迁移 API——Bean 治理迁移，非查询窗口）；错误码 `ERR_INV_TAKE_NOT_IN_COUNTING`。
- [x] Decision: **盘点单定位形态 = 扫码为主 + select 兜底**。页面「扫盘点单码」走 `@query:ErpInvBarcode__resolveBarcode` 消费 STOCK_TAKE 分支（doc() 返回 {type:DOC, refType:STOCK_TAKE, refId, refCode}——实核 :223-230）回填 takeId 并展示单号；辅以 `ErpInvStockTake__findPage`（现成）按 docStatus=CONFIRMED 过滤的 select 兜底。

Exit Criteria:

- [x] 五个 Decision 全部落盘（选择/替代/残留风险），与 owner doc 无冲突（state-machine.md 引文核对一致；D6 执行期增补含理由）

### Phase 2 - 服务端落行实现（若 Phase 1 选 (b)）

Status: completed
Targets: `module-inventory/erp-inv-service/`（BizModel + 必要的 Errors/i18n 键）
Skill: development-wisdom-gate

- Item Types: `Add | Proof`
- Prereqs: Phase 1；若选 (a) 本阶段整体跳过并在计划记录（范围修订理由）。

- [x] Add: `scanSave(takeId?, takeCode?, skuBarcode, locationCode?, actualQuantity)` @BizMutation 落地（takeCode 可选入参为执行期增补 D6：flux select 动态选项源在本仓手写页无可用承载〔F15 实证〕，盘点单定位改为扫/输编号服务端按 code 解析——PDA 键盘楔作业惯例）：barcode-enabled 守卫 / docStatus==CONFIRMED 窗口守卫 / SKU example 直查 / 库位必填+仓库归属校验 / StockBalance 快照 bookQuantity / nextLineNo max+1 / 派生落行。DAO 访问走 CrudBizModel 内建 daoFor（内建访问器消解注入面；生成层 beans 已声明本 BizModel——「生成层未声明致注入失败」为 stale target 误诊，结束审计 MAJOR-2 订正，手写层显式声明保留以对齐 barcode 先例）。
- [x] Add: ErrorCode 5 码（not-in-counting / qty-invalid / sku-unknown / location-unknown / location-warehouse-mismatch，`erp.err.inv.*` 前缀 zh 源语言——既有 ErrorCode 无 EN yaml 先例，模式一致）。
- [x] Proof: JUnit `TestErpInvStockTakeLineScanSave` **8/8 绿**（happy path 派生字段/新行递增/StockBalance 快照/DRAFT 拒绝/未知 SKU/仓库不匹配/缺库位门控/takeCode 解析）；inv-service 全模块 274/0/0 零回归（274 含新增 8；初记 273 系报告口径偏差，审计方独立复跑 274 实证）。

Exit Criteria:

- [x] scanSave 落行行为有 JUnit 断言覆盖（TestErpInvStockTakeLineScanSave 8/8：派生字段/新行/快照/四守卫/takeCode）
- [x] 触域模块 `mvn test -pl module-inventory/erp-inv-service` 绿（273+8 全绿）

### Phase 3 - 页面重构

Status: completed
Targets: `pda-stock-take/main.page.yaml`
Skill: flux-rendering-conformance-audit-prompt

- Item Types: `Fix | Add`
- Prereqs: Phase 1（+Phase 2 若适用）；plan 1 范式（onClick ajax 接线 + confirmText/messages 兄弟键 + then 链）。

- [x] Fix: 按钮改 flux 原生接线（onClick ajax + `/r/` 直发 + confirmText 兄弟键 + messages + then setValues 重置）；2 处表单级 api 消解；跨表单 `${scanForm.*}` 引用消解（单表单域 + scanSave 单请求）。
- [x] Add: 盘点单定位（D5 修订：扫/输编号文本输入——select 动态选项源无承载见 D6）；消息双语；成功后表单重置（setValues 清空扫码字段）；placeholder i18nEn 块承载（**执行期发现：同节点双 i18nEn 键触发 duplicate-key 启动失败**——合并单块承载，CAT4 块配对语义不变）。
- [x] Proof: `npm run validate:flux` errors=325 零新增、pda 页 errors=0；cjk report pda 新增 0。

Exit Criteria:

- [x] 页面按闭环重写完成，validate:flux 该页 errors=0 且无 actionType/api/form-api 残留（grep+导出条目双证）
- [x] i18n 双语覆盖（含 placeholder，单块承载）——`check-hardcoded-cjk.mjs` report 模式该页新增 0

### Phase 4 - E2E 升级 + 文档对齐

Status: completed
Targets: `tests/e2e/mobile/pda-stock-take.mobile.spec.ts`、`docs/design/inventory/barcode-integration.md`
Skill: browser-use:web-gui-tester（调试期）；spec 按 runbook 规范

- Item Types: `Fix | Proof`

- [x] Fix: E2E 升级为真实点击驱动：GraphQL 种单链（wh/loc/uom/material/sku/take+startTake）→ 页面输 takeCode/条码/库位/数量 → 点击保存 → confirm 对话框 → waitForResponse `/r/ErpInvStockTakeLine__scanSave` → findPage 反查落行派生字段（materialId/uoMId/lineNo=1/difference=5）→ cleanup（删行/cancelTake/delete take/主数据逐删）。移动视口 375×812。
      **执行期排查记录**：select 动态选项源经 4 形态实测（expression source/inline source/inline+type:source/data-source+options 表达式）均无法稳定承载〔F15 登记〕→ D6 裁决改 takeCode 文本输入（服务端解析）。测试基建勘误 3 处：GraphQL 对象参数须 input() 变量（JSON 内联键引号非法）/findPage 为 query 非 mutation/GraphQLClient.findPage 便捷方法。诊断经 fluxDebug fixture（`dumpFluxDebug`）完成。
- [x] Proof: 新 spec 本地绿；mobile 分层全量 **6/6 绿**（approval 链零回归）。
- [x] Proof（文档）: `barcode-integration.md` §PDA 场景 5 状态注记更新（v2 闭环 + 测试三件套）；分析报告 F2/F3/F4/F5 状态回填 + F15 新发现登记。

Exit Criteria:

- [x] E2E 真实点击保存链路绿（mobile 3/3 + 分层全量 6/6，输出留证）
- [x] owner doc + 分析报告状态回填完成（barcode-integration.md v2 段 + F2-F5 回填/F15 登记）

## Draft Review Record

- Independent draft review iteration 1: needs revision (agent_d0a0c323) — 0 BLOCKER / 3 MAJOR / 5 MINOR：M1「OPEN 状态盘点单」不存在（实态 DRAFT/CONFIRMED=盘点中/DONE/CANCELLED，须增状态守卫 Decision）/ M2 owner doc 路径错 `docs/design/inventory/barcode-integration.md` ×4 / M3 skuInfo 不含 uoMId（D1(a) 前提失真）+ m1~m5（bookQuantity 来源升 Decision/cleanup 通道注记/88 行/弱措辞/env.confirm 联动提示）。
- Independent draft review iteration 2: accept (agent_e9902535) after <M1 废 OPEN、D4 状态守卫 Decision（StateMachine Bean 实核在位）；M2 路径四处修正实存；M3 skuInfo 实码核对吻合；m1/m2/m3/m4 全落实；无阻塞新问题（minor：cleanup 通道引文路径顺手补全为 tests/e2e/business-actions/_helper.ts>

## Closure Gates

- [x] 范围内行为完成（闭环：扫/输单号→扫字段→保存成功→反馈重置，浏览器真实点击断言 3/3）
- [x] 相关文档对齐（barcode-integration.md v2 注记 + 分析报告 F2-F5 回填/F15 登记 + D5/D6 裁决在计划）
- [x] 已运行验证（全量 `mvn clean install -DskipTests` BUILD SUCCESS + inv-service 274 全绿（含新增 8） + mobile 分层 6/6 + `npm run validate:flux` errors=325 零新增 + `check-hardcoded-cjk.mjs` report exit 0/pda 0 新增 + 合规检查器 exit 0）
- [x] 无范围内项目降级为 deferred/follow-up（D6 为执行期裁决增补非降级；F14 归盘点作业面批次见分析报告）
- [x] 独立草案审查已完成并记录（2 轮收敛）
- [x] 文本一致性已验证：状态、阶段、门控和日志一致（日志随闭包提交写入 docs/logs/2026/10-05.md）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（记录见 Closure）
- [x] 结束证据存在于文件中（surefire 报告 + playwright 输出 + 门禁日志）

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

Status Note: 独立结束审计记录见下；执行纪要：Phase 1 五 Decision 落盘（D6 执行期增补：盘点单定位改 takeCode 服务端解析——select 动态选项源 F15 实证无承载）；Phase 2 scanSave 落地（5 错误码 + JUnit 8/8 + 全模块 274/0/0）；Phase 3 页面重构（flux 原生接线 + takeCode 流 + 双语块单承载）；Phase 4 E2E 真实点击链（mobile 6/6）。验证全绿（构建 BUILD SUCCESS / flux 门禁 325 零新增 / cjk 0 新增 / 合规 0）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 round1 agent_28f4aac0（NEEDS REVISION：MAJOR-1 takeCode 路径 nextLineNo 误用请求参数致 lineNo 恒 1 / MAJOR-2「生成层未声明」不实记录 + 6 MINOR）→ 整改（take.getId() 修复 + takeCode 双扫强化断言 + 三处文本订正）→ round2 agent_ac9c59a1（**ACCEPT**）
- Evidence: round2 独立复验——surefire 8/8（强化断言在位）、inv-service 全模块 274/0/0、plan-gates --strict PASS、compliance exit 0、cjk report exit 0、flux errors=325 零新增/pda errors=0、build13 BUILD SUCCESS（`_tmp/build13-pda.log`）

Follow-up:

- <非阻塞跟进项>
