# 2026-07-16-0012-1-view-grid-col-not-prop-remediation view.xml grid 非法列(col-not-prop)全量修复

> Plan Status: **completed**（经独立子代理新会话冷重播审计 PASS（2026-07-16））
> Last Reviewed: 2026-07-16
> Mission: erp
> Work Item: 2256-2 FK 显示名整改回归——view.xml grid 列引用不存在实体属性导致页面渲染崩溃
> Source: plan `2026-07-15-2246-1-fk-display-name-e2e-regression.md` Phase 3 根因结论。2246-1 Non-Goal 明确「生产 view.xml 缺陷开 successor」。
> Related: `2026-07-14-2256-2`（整改本体，引入部分回归）、`2026-07-15-2246-1`（E2E 回归，根因发现方，全套件全绿依赖本计划）、`docs/lessons/05-nop-e2e-failure-log-first-diagnosis.md`
> Audit: required

## Current Baseline

经实时仓库核实（HEAD 2026-07-16）+ 干净 server 实证 + 静态检查器 `tools/check-view-cols.mjs` 全量枚举：

### 根因（已由 2246-1 Phase 3 收口）

- 现象：13+ 域 CRUD smoke 稳定失败，报 `locator.waitFor Timeout 15000ms`（等 `.cxd-Crud`），跨 ≥10 份计划登记为「预存渲染超时环境问题」。
- 真因：view.xml grid 列引用不存在的实体属性 → `nop.err.xui.grid.col-not-prop` → `PageProvider__getPage`→`loadComponentModel` 抛错 → 页面不渲染 grid → 超时。**非时序问题，是生产缺陷。**
- 来源混合：① 2256-2 把 `xxxName` 回退为 `xxxId`，但部分 `xxxId` 非有效属性（被删的 @BizLoader 显示属性 / 误改的真实列）；② 更早遗留的非法列（如 `employeeDisplayName`，其 xmeta prop 已不存在但 view.xml 未更新）。

### 规模（`tools/check-view-cols.mjs` 全量枚举，仅 src/main/resources 源文件）

- **147 个非法列，跨 13 域**：hr 34 / crm 20 / finance 19 / assets 19 / mfg 16 / mnt 12 / qa 10 / projects 5 / cs 5 / b2b 4 / md 1 / logistics 1 / aps 1。
- 高频非法列（src 源文件计数，不含 target 镜像）：`employeeDisplayName`(16)、`leadCode`(6)、`assetCode`(6)、`workOrderNo`(5)、`uomId`(5)、`periodCode`(5)、`equipmentCode`(5)、`acctSchemaCode`(5)、`voucherCode`(4) 等。
- 完整清单可由 `node tools/check-view-cols.mjs` 实时再生（ authoritative enumerator + verifier）。

### 修复非均匀（实证，禁止机械批量改）

- **子类 A（简单回退）**：原列名是仍存在的真实列。例 `ErpCrmStage.stageName`(propId 4 真实列) ← 2256-2 误改为 `stageId`。修：`stageId`→`stageName`。
- **子类 B（关系名替换）**：原 `xxxName` 是被 2256-2 删的 @BizLoader 显示属性，实体有对应 to-one 关系。修：列名改为关系名，由 view-relation 解析 `{relation}.{dispCol}`。需逐实体核对关系名。
- **子类 C（FK 名不匹配）**：列名与实体实际 FK prop 名不符。例 `ErpCsTicketType` 列 `slaPolicyId`，但该实体 FK 实为 `defaultSlaPolicyId`/关系 `defaultSlaPolicy`（`slaPolicyId` 是 ErpCsTicket 的）。需逐实体从 ORM/xmeta 核对正确 prop。
- 判定依据：`git diff 253fcdeb8~1 253fcdeb8 -- <view.xml>` 看原列名 + `tools/check-view-cols.mjs` 核有效性 + 实体 xmeta/ORM 核关系与 FK 名。

### 剩余差距

147 个非法列阻断 ~13 smoke + 大量页面渲染；2246-1 全套件全绿依赖本计划完成。

## Goals

- 修复全部 147 个 view.xml 非法 grid 列，使每列引用实体有效属性（真实列 / 关系名路径）。
- `mvn test -pl app-erp-all -Dtest=ErpAllWebPagesTest`（`validateAllPages()`）BUILD SUCCESS / 0 errors（ground-truth 全量页面闸门转绿）。
- `node tools/check-view-cols.mjs` 报 0 非法列（静态复核）。
- 修复后重跑 2246-1 CRUD smoke，确认此前失败的 13 域转绿。

## Non-Goals

- **不改 ORM/契约/种子**——仅改 view.xml grid `<col id>`（及必要时的 label）。若某列揭示真实 ORM 缺失（如某 FK 列本应存在却未声明），开 sub-successor（ORM ask-first），不在本计划即时改 ORM。
- **不改 xmeta 源**（生成产物）——若 xmeta 与 ORM 不一致致 prop 缺失，记为发现并开 sub-successor；本计划仅在 view.xml 侧用已存在的有效属性。
- **不重写 2256-2 的显示名机制**——沿用 tagSet="disp" + view-relation；本计划只纠正被误改/遗留的列引用。
- **不承接 2246-1 全套件基线落盘**——本计划交付"页面可渲染 + smoke 转绿"；全套件 Playwright 全绿 + known-good-baselines 仍归 2246-1 Phase 3 收尾（依赖本计划完成）。

## Task Route

- Type: `仅实现变更`（view.xml grid 列引用纠正；不改 ORM/API/认证/契约；跨 13 域 view.xml，~147 处）。
- Owner Docs: `nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md`（tagSet=disp / 显示名解析 / view-relation）、`docs/lessons/05-nop-e2e-failure-log-first-diagnosis.md`（诊断法）。
- Skill Selection Basis: view.xml 列纠正匹配 `nop-frontend-dev`（XView/grid/列/bounded-merge/view-relation）。逐域修复前重新加载该技能。修复后用 `nop-debugging` 的 condition-based 思路（validateAllPages 作 ground-truth 门控，非任意超时）。
- Protected Areas: view.xml 在 `erp-*-web` 生产层；仅改 `<col id>` + 可选 label，不改 view 结构/按钮/表单/动作。ORM/xmeta 为 ask-first，不在本计划改。

## Infrastructure And Config Prereqs

- 无新增 infra。复用 `mvn clean install -DskipTests` 代码生成 + 资源打包链（view.xml 改动需 rebuild 方能传播到 target/jar）。
- 复用 E2E server（`BASE_URL=http://127.0.0.1:8011`，见 2246-1）做 smoke 复验。

## Execution Plan

### Phase 1 - 逐域修复 view.xml 非法列（按域分批，每批 rebuild + validateAllPages 门控）

Status: completed
Targets: 13 域的 `erp-*-web/src/main/resources/_vfs/.../pages/*/*.view.xml`
Skill: `nop-frontend-dev`

- Item Types: `Fix | Proof`
- Prereqs: 本计划草案通过独立审计

- 执行循环（每域）：
  1. `node tools/check-view-cols.mjs module-<domain>` 列出该域非法列。
  2. 逐列判定子类（A/B/C）：`git diff 253fcdeb8~1 253fcdeb8 -- <view.xml>` 取原列名；核 xmeta/ORM 取正确 prop（真实列名 / 关系名 / 正确 FK 名）。
  3. 改 `<col id="...">` 为正确值（必要时保 label）。
  4. `mvn clean install -DskipTests -pl module-<domain>/erp-<short>-web -am`（或全量）rebuild。
  5. `mvn test -pl app-erp-all -Dtest=ErpAllWebPagesTest` 验证（失败即抛，定位下一个）。
- [x] `Fix`：hr(34) / crm(20) / finance(19) / assets(19) / mfg(16) / mnt(12) / qa(10) / projects(5) / cs(5) / b2b(4) / md(1) / logistics(1) / aps(1) 逐域修复。
  - Skill: `nop-frontend-dev`
  - Execution Note: 全部 147 src 非法列已修。分派 4 个独立子代理按域簇并行修（hr / crm+cs / finance+assets / mfg+mnt+qa+prj+b2b）+ 主代理修 3 小域（aps/md/logistics）。子类分布：A（真实列回退，如 operationId→operationName、stageId→stageName、uomId→uoMId 大写 M）+ B（删显示属性→关系名，如 employeeDisplayName→employee、各 xxxCode→关系）+ C（FK 名不匹配，如 slaPolicyId→defaultSlaPolicy、currencyId→salaryCurrency）。每域修复后 `node tools/check-view-cols.mjs module-<domain>` 自验 src=0。全量 checker 复核 src 非法列=0。
- [x] `Proof`：`node tools/check-view-cols.mjs` 报 0 非法列（静态全量复核）。
  - Skill: none
  - Execution Note: 全量 src 非法列 = 0 confirmed。

### 修复中发现的附加缺陷（非 2256-2 回归，一并修以达成 validateAllPages 全绿）

- `ErpB2bPartnerProfile.view.xml`：重复 `<col id="partnerId">`（line 11/12，预存重复，2256-2 前即为双 partnerName）→ 移除冗余一条（保留带 label 的）。
- `ErpMfgSubcontractOrder.view.xml`：发料/收货参数 dialog（issueMaterials/receiveFinished，无 objMeta 绑定）的 3 个参数 cell（sourceWarehouseId/receivedQty/destWarehouseId）缺 `custom="true"` → 报 cell-not-prop。按 ErpHrRecruitment 的 scheduleInterview 范式补 `custom="true"`。此为预存缺陷（非 2256-2），此前 validateAllPages 从未启用故未暴露。
- 为非失败式一次性枚举全部页面错误，新增诊断测试 `app-erp-all/.../ErpAllWebPagesCollectTest.java`（validateAllPages 的 try/catch 全量收集版，报告所有 col/cell-not-prop + 重复，非 fail-fast）。保留为可复用页面回归诊断。

Exit Criteria:

- [x] `tools/check-view-cols.mjs` 报 0 非法列
- [x] `ErpAllWebPagesTest`（validateAllPages）0 errors（exit 0 confirmed）

### Phase 2 - smoke 回归 + 收尾

Status: completed
Targets: `tests/e2e/crud/*.smoke.spec.ts`；`docs/testing/known-good-baselines.md`（交还 2246-1）
Skill: none

- [x] `Proof`：干净 server（自起，curl 确认 200）跑 13 域 CRUD smoke，确认转绿（参见 lesson 05 六步法）。
  - Execution Note: 自起新鲜 server（PID 93289，runner jar 含全部修复，curl HTTP 200），`BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test tests/e2e/crud/ --grep "CRUD list/form smoke"` → **18 passed (2.3m)**。此前失败的 13 域全绿。
- [x] `Add`：日志 `docs/logs/2026/07-16.md` 增条目（修复域/列数/validateAllPages + smoke 结果）。

Exit Criteria:

- [x] 13 域 CRUD smoke 全绿（实为全 18 域 smoke 全绿）
- [x] 日志落地

## Draft Review Record

- Independent draft review iteration 1: `accept`（独立冷重播草案审计子代理，新会话无执行者/起草者上下文，2026-07-16）— 0 Blocker / 0 Major / 1 Minor。全部 load-bearing 事实主张经实时仓库逐项核实**零伪**：规模 147 src 非法列（294 含 target 镜像）+ 13 域分布（hr34/crm20/finance19/assets19/mfg16/mnt12/qa10/projects5/cs5/b2b4/md1/log1/aps1）与 `node tools/check-view-cols.mjs` 输出**逐数字吻合**；根因两例抽查 confirmed（ErpCrmStage.stageName=真实列 propId4/ErpCsTicketType FK=defaultSlaPolicyId+关系 defaultSlaPolicy，非 slaPolicyId）+ 第三例 employeeDisplayName 确认 subclass B（实体有 employee to-one 关系）；修复非均匀 A/B/C 三子类真实存在；工具存在且 `node --check` 通过；Non-Goal 仅改 view.xml `<col id>` 符合 ORM ask-first 保护区域；Closure Gate ErpAllWebPagesTest.validateAllPages 经 PageProvider.java:87 核实为遍历全 pages 的 getPage→loadComponentModel ground-truth 闸门。1 Minor（m1「高频非法列」频率表误按 src+target 294 口径计数，与「仅 src」总数 147 自相矛盾）已当场修订为 src 口径。模板合规 pass（rule 1/2/4/7/8/14 + anti-slack）。草案可接受执行 → `Plan Status: active`。

## Closure Gates

- [x] `tools/check-view-cols.mjs` 报 0 非法列
- [x] `ErpAllWebPagesTest` BUILD SUCCESS / 0 errors（exit 0）
- [x] 13 域 CRUD smoke 全绿（全 18 域 smoke 18 passed）
- [x] `mvn clean install -DskipTests` 154 模块 BUILD SUCCESS（全量 build exit 0 + b2b/mfg-web per-module install exit 0 + app-erp-all rebuild exit 0；view.xml 仅资源变更，无 Java）
- [x] 相关文档对齐（日志）
- [x] 无范围内项目降级为 deferred（ORM/xmeta 缺失须开显式 sub-successor）—— b2b 重复 + mfg 参数 cell 已一并修，无 sub-successor 触发。【审计者注：原文件此处遗留为 `[ ]`，但自身注释已述"无 sub-successor 触发"；独立审计核实 validateAllPages 0 errors + 0 非法列 + 抽查 5 例目标属性均语义正确，条件性 successor 触发条件（不可用既有属性纠正的 ORM/xmeta 缺失）**未满足**，无任何项被静默降级为 deferred，故据证据勾 `[x]`】
- [x] 独立草案审查已完成并记录
- [x] 结束审计由独立子代理（新会话）执行——已完成（独立 general 子代理冷重播 PASS，2026-07-16）

## Deferred But Adjudicated

### ORM/xmeta 真实缺失

- Classification: `watch-only residual`（修复过程中若发现某非法列揭示 ORM 真缺 FK 列声明或 xmeta 与 ORM 不同步）
- Why Not Blocking Closure: 本计划仅在 view.xml 侧用已存在有效属性绕过；真实 ORM 缺失开 sub-successor（ask-first）。
- Successor Required: `yes`（触发条件：修复中发现 xmeta/ORM 不一致且无法用既有属性纠正时）

## Closure

Status Note: 全部 147 src 非法列已修（4 子代理并行 + 主代理 3 小域）+ 2 附加预存缺陷（b2b 重复 partnerId、mfg 参数 dialog 3 cell 缺 custom）一并修。`tools/check-view-cols.mjs` src=0；`ErpAllWebPagesTest`（validateAllPages）exit 0 / 0 errors；干净 server 全 18 CRUD smoke 18 passed。Plan Status 仍 active，待独立子代理结束审计后方可 completed。→ 【2026-07-16 追加指针】独立子代理冷重播结束审计 PASS，证据见下 Closure Audit Evidence；Plan Status 已置 completed，Closure Gate 最后一项已勾。

Closure Audit Evidence:

- Auditor / Agent: 独立 general 子代理（新会话），冷重播无执行者/起草者上下文，2026-07-16。
- Verdict: **passes closure audit (PASS)**。
- 实际执行命令与逐项结论（对照实时仓库，不采信执行者自述）：
  1. 静态全量复核（决定性）：`node tools/check-view-cols.mjs` → `Checked 676 views, 6116 cols. 0 invalid col(s)`。扫描覆盖 `module-*/erp-*-web/.../_vfs/.../pages/*/*.view.xml`（排除 `_gen/`），含 src 源文件全集 → **src 非法列 = 0**。✓
  2. 运行时 ground-truth（决定性）：`mvn test -pl app-erp-all -Dtest=ErpAllWebPagesTest -Dmaven.compiler.fork=true -q` → exit 0（MVN_EXIT=0）；surefire `TEST-io.nop.app.all.web.ErpAllWebPagesTest.xml`：`tests="1" errors="0" failures="0" skipped="0"`（validateAllPages 全量页面闸门 0 errors）。✓
  3. Anti-Hollow 抽查（5 个，跨 A/B/C 子类，全部核实为真实有效属性/关系）：
     - ① ErpApsOperationOrder（子类 A）`operationName`：view.xml:11 `<col id="operationName">` + xmeta `name="operationName"` 存在 ✓
     - ② ErpCrmStage（子类 A）`stageName`：view.xml:10 `<col id="stageName">` + xmeta `name="stageName"` 存在 ✓
     - ③ mfg（子类 C，大写 M）`uoMId`：9+ 个真实 view 有 `<col id="uoMId">` + 9 个 mfg 实体 xmeta 含 `name="uoMId"` ✓
     - ④ ErpCsTicketType（子类 C）`defaultSlaPolicy`：view.xml:11 `<col id="defaultSlaPolicy">` + xmeta:76 `name="defaultSlaPolicy" ... ext:kind="to-one"`（to-one 关系，列有效）✓
     - ⑤ hr（子类 B）`employee`：16 个 hr view 有 `<col id="employee">` + 对应 xmeta `name="employee" ... ext:kind="to-one"` ✓
  4. 附加缺陷修复核实：① `ErpB2bPartnerProfile.view.xml` `<col id="partnerId">` 计数 = 1（重复已消除）；② `ErpMfgSubcontractOrder.view.xml` 3 个参数 cell（sourceWarehouseId/receivedQty/destWarehouseId）均已带 `custom="true"`。✓
  5. 规则 11 五点一致性：Plan Status（active 待审计，本审计 PASS 后改 completed）/ Phase 1-2（completed）/ 各 Exit Criteria（[x]）/ Closure Gates（除结束审计外 [x]，本审计勾最后一项）/ Closure Audit Evidence（原占位，本审计回填）/ Status Note 互相吻合；Draft Review Record 与各 Phase Execution Note 原文未篡改。✓
  6. Deferred 诚实性：Deferred 项为 watch-only residual（Successor Required: yes，条件触发）。validateAllPages 0 errors + 0 非法列 + 抽查 5 例目标属性均语义正确（非为通过闸门而指向任意现存属性），无证据表明发现不可纠正的 ORM/xmeta 缺失被静默吞掉；条件性 successor 触发条件**未满足**。✓
- 残留风险（非阻塞）：① 全量 `mvn clean install -DskipTests` 154 模块未在本次审计重跑（耗时）；但 validateAllPages 通过即证明 app-erp-all 装配正确且全页面可渲染，静态 checker 证明 src 干净，二者已等价覆盖 view.xml 资源正确性。② Closure Gate 中 18 域 CRUD smoke 全绿（18 passed）为执行者记录，本次审计未重跑 Playwright（依赖运行 server）；已以决定性的 validateAllPages（0 errors）+ 静态 checker（0 非法列）作运行时 ground-truth 替代证明，二者正是 smoke 失败的根因门控，转绿即等价。

Follow-up:

- <仅非阻塞跟进；确认的 ORM/xmeta 缺陷须以显式 sub-successor 承接>
