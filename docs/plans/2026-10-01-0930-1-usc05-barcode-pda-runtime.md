# 2026-10-01-0930-1-usc05-barcode-pda-runtime 条码/PDA 作业面运行时收口

> Plan Status: completed
> Last Reviewed: 2026-10-01
> Source: `docs/backlog/user-story-coverage-roadmap.md` USC-05（US-IV-05、US-PL-08 承接项；商贸/制造档 Must）
> Related: `docs/design/inventory/barcode-integration.md`（设计契约）、`docs/architecture/view-and-page-strategy.md`（PDA 扫码手写页预留位）、`docs/architecture/customization-capabilities.md`（档位组装）、`docs/plans/2026-10-01-0830-1`（USC-04 移动视口范式与 D5(b) residual）
> Audit: required（inventory 域实现：新增 Java BizModel + 页面 + 测试；无 ORM 模型变更、无保护区触碰——标准计划审计流程；ORM 零变更故不触发双批准，但解析入口属新业务 mutation 按实现类计划审计）

## Current Baseline

- **设计契约（barcode-integration.md 实读）**：4 配置键（`erp-inv.barcode-enabled` 默认 true 总开关 / `barcode-sku-format` 默认 EAN13 / `barcode-auto-print-on-receive` 默认 true / `pda-require-location-scan` 默认 true）；PDA 场景 1-7（收/上架/拣/发/盘/领料/质检）触发链终点**全部是既有 mutation**（generateMove/审批翻转/StockTakeLine 保存/状态 DONE）；BarcodeService 设计形态 = 「码→类型识别→分发」解析路由层（SKU/批次/位置三解析框），非业务 mutation。6 类条码中：SKU 码解析已实现（`ErpMdMaterialSkuBizModel.findSkuByBarcode` @BizQuery）；单据码=单号本身（扫入值即查询键）；库位/批次/序列号/托盘**无 barcode 列**（只能按 code 约定）；标签打印无子系统可挂；条码生成（ErpSysDocNumberRule）纯设计零实现。
- **实现现状**：4 个 `erp-inv.barcode-*`/`pda-*` 配置键全仓零命中（仅文档）；module-inventory 零扫码页面（web 层 grep 零命中）；`view-and-page-strategy.md` 复杂页清单预留「PDA 扫码」手写页位。实现缺口 = 解析入口 + 配置接线 + 作业面页面 + 测试，全部 0→1。
- **代表场景既有测试范式**：收货链（p2p-chain：PO/Receive approve→StockMove DONE）、发货链（o2c-chain：Delivery approve→OUTGOING）、盘点（ErpInvStockTake startTake/completeTake + StockTakeLine 保存）、领料（mfg-chain MaterialIssue.confirm）。盘点扫码页现成模板 = `stock-take-flow/main.page.yaml`（手写 flux 流程页驱动 Facade mutation 链、零后端 delta 先例）。
- **USC-04 遗产约束**：FluxAdapter 窄屏行定位失配（D5(b) residual）——PDA 作业面页面须用**表单式 UI**（input+button，非 crud 网格）规避；移动视口范式（setViewportSize）可复用。
- **剩余差距**：全部 0→1（见 Goals）；设计在册但本期不实现面需显式 residual 登记（批次/序列号/托盘解析、标签打印、上架库位推荐、FIFO/FEFO 校验、离线队列、拣货落点缺失[ErpInvPickingOrderBiz 纯 CRUD 无 mutation，场景 3 无落点]）。

## Goals

- 后端条码解析入口：`ErpInvBarcodeBizModel`（module-inventory/erp-inv-service，`@BizQuery resolveBarcode(code, locationCode?)`）——SKU 码→委托 findSkuByBarcode（SKU-first 优先）；单据码→按单号解析（Receive/MaterialIssue/StockTake 经 I*Biz 注入直查，Delivery 经 IBizObjectManager 按名解析[D1 裁决]）；库位→ErpMdLocation code 查询；返回结构化 {type, refType, refId, refCode, skuInfo?}。`erp-inv.barcode-enabled` 门控（OFF 抛 ERR_BARCODE_DISABLED）；`pda-require-location-scan`=仅 refType=STOCK_TAKE 分支要求 locationCode（D1 权威裁决）。
- 4 配置键接线进 `ErpInvConstants`：`barcode-enabled` 门控消费；`barcode-sku-format` 校验声明（EAN13 时对纯数字 13 位校验）；`barcode-auto-print-on-receive` **接线为显式 no-op + WARN 日志**（无打印子系统，B 路诚实性裁决预留）；`pda-require-location-scan` 门控 resolveBarcode 的 locationCode 必填校验。
- PDA 作业面页面（盘点场景代表）：`pda-stock-take/main.page.yaml`（表单式：扫码输入 → resolveBarcode → 盘点单/行定位 → 实盘数量输入 → StockTakeLine 保存）——沿 stock-take-flow 零后端 delta 范式；flux 响应式（表单式规避 D5(b) 网格 residual）。
- 代表场景测试：收货/发货/盘点三场景经 resolveBarcode 入口的 API 层触发断言（JUnit）+ PDA 页面浏览器层冒烟（含 375px 移动视口）。
- 档位说明登记：customization-capabilities.md 或 owner doc 档位段（商贸/制造档 Must 组装面；纯财务轻部署不组装 inventory 模块则本面随模块不装配）。

## Non-Goals

- 批次/序列号/托盘条码解析（无 barcode 数据列，需 ORM 加列——ORM 保护区另立）；库位复合码（仓库+区域+排+列+层，同前）。
- 标签打印子系统（auto-print-on-receive 键接线为显式 no-op + WARN 日志，residual 登记）。
- 上架库位推荐（ABC/周转率算法）、FIFO/FEFO 扫码校验、PDA 离线队列（设计反模式明示缓做）。
- 拣货场景落点（ErpInvPickingOrderBiz 纯 CRUD 无 mutation——场景 3 需先补拣货 mutation，另立）。
- 质检采样扫码（场景 7，qa 域联动另立）。
- ORM 模型变更（barcode 列新增等——保护区，不在本批）。

## Task Route

- Type: `implementation-only change`（inventory 域应用层：1 新 BizModel + 1 常量组 + 1 新页面 + 测试；ERR 码/i18n 伴生）
- Owner Docs: `docs/design/inventory/barcode-integration.md`（实现状态段更新：已实现/残量分界）、`docs/design/inventory/README.md` 或 seed-data 同域段（如适用）
- Skill Selection Basis: Phase 1（后端 BizModel + 页面）加载 `nop-backend-dev`（BizModel 编写场景）+ `nop-testing`（测试场景）；roadmap Skill 列 nop-testing 全coverage；Phase 2 `Skill: none`（文档登记）。

## Infrastructure And Config Prereqs

- 无外部基建新增；复用既有 JUnit（inv-service 测试套件）+ playwright（页面冒烟）+ 全量构建链。

## Execution Plan

### Phase 1 — 后端解析入口 + 配置接线 + 测试

Status: completed
Targets: `module-inventory/erp-inv-service/.../ErpInvBarcodeBizModel.java`（BizObj 名 `ErpInvBarcode`，沿 ErpInvReport 虚拟 BizObject 范式；**须在 `erp-inv-service/.../beans/app-service.beans.xml` 显式 `<bean>` 声明**[无 classpath 自动扫描，先例 ErpInvReportBizModel/ErpInvDashboardBizModel]）、`ErpInvConstants.java`、ERR 码/i18n、`module-inventory/erp-inv-service/src/test/.../TestErpInvBarcodeResolve.java`（SKU/pur/inv-take/mfg + 门控）+ `app-erp-all` 集成测试 Delivery 断言（B1 裁决）
Skill: nop-backend-dev + nop-testing

- Item Types: `Add | Decision`

- [x] Decision: **D1 解析路由设计与键消费矩阵（草案审查 B1/M1/m4 修订后）**——resolveBarcode(code, locationCode?) 解析顺序：SKU（委托 `IErpMdMaterialSkuBiz.findSkuByBarcode`，inv-dao→md-dao 传递依赖在案，inv-service 已有 `IErpMdMaterialBiz` 注入先例）→ 单据号四类：Receive/MaterialIssue/StockTake 经各自 `I*Biz` 注入直查（pur/mfg 依赖在案），**Delivery 经 `IBizObjectManager` 按名解析**（`getBizObject("ErpSalDelivery")`——DAG 合规先例 `ErpInvCostingBizModel` 跨模块解析范式；**B1 裁决：否决 pom 加 sal-dao**[inv→sal 违反 sales→inventory 单向 DAG 与 module-meta businessDependencies 声明]、否决收窄三类[四类单据为设计契约面]）→ 库位 code（ErpMdLocation 精确查询）→ 未命中抛 ERR_BARCODE_UNRESOLVED。**码型歧义优先级**：SKU-first（13 位纯数字先走 SKU 解析，未命中再走单据号——EAN13 与单号空间理论可撞，SKU 高频先试的 PDA 体验取舍）。**键消费矩阵**：barcode-enabled=总门控（OFF 抛 ERR_BARCODE_DISABLED）；sku-format=EAN13 数字 13 位校验（格式声明非解析前置）；pda-require-location-scan=true 时 **refType=STOCK_TAKE 分支要求 locationCode**（设计场景 5「扫 SKU+库位」对齐，M1 (a) 裁决：页面补库位输入步）；auto-print-on-receive=收货流读取时 WARN 日志 no-op（启动时不刷，residual 显式化）。**%prod 语义**：resolveBarcode 无 FNPT 种子 = %test admin 可用、%prod 业务角色 403（USC-02a D1 菜单壳已验收语义，随权限矩阵 successor）。**替代方案登记**：pom 加 sal-dao（否决，DAG 违规）；收窄三类（否决，设计契约面收缩无据）。**实现机制回写（执行期勘误）**：单据号解析实现为 IDaoProvider.daoFor(...).findFirstByExample(example)（pur/mfg/inv-take 编译依赖在案走类型化直查；Delivery Class.forName 反射直查——IBizObjectManager 按名代理需编译期接口，单域 classpath 缺 sal 时不可行）；SKU 分支由 findSkuByBarcode 委托改 barcode 列 example 直查（@BizQuery 直调需 IServiceContext，Direct Java 调用上下文为 null 会 NPE——执行期发现）。I*Biz-first 规则偏离原因：md 域 findSkuByBarcode 上下文依赖 + 跨域代理 classpath 限制，已在 ErpInvBarcodeBizModel 代码注释同步记录。残留风险：Delivery 按名解析在 inv-service 单域测试 classpath 无 sal BizObject——该断言移 app-erp-all 集成测试（sal 在聚合 classpath），容错先例 `ErpFinAccountingPeriodProcessor:208`。
      Skill: nop-backend-dev
- [x] Add: `ErpInvBarcodeBizModel.resolveBarcode` @BizQuery + 4 常量 + ERR_BARCODE_DISABLED/UNRESOLVED/LOCATION_REQUIRED + i18n（zh/en）。beans 显式声明（BizObj ErpInvBarcode）。
      Skill: nop-backend-dev
- [x] Add: JUnit `TestErpInvBarcodeResolve`（7 tests 全绿）——三代表场景：①收货向：种 PO+Receive（或复用既有测试基建）→ resolveBarcode(单号) → refType=PUR_RECEIVE 命中；②盘点向：StockTake 单号命中 + SKU 码委托解析（materialId/uoMId 种子链 + 13 位 EAN13 条码，沿 TestErpInvImmutableSaveChannel runInSession 先例）+ barcode-enabled=false 门控拒绝 + pda-require-location-scan 门控（location-required 实测）——**种单走 DAO 直种轻路径**；③领料向：MaterialIssue 单号命中（mfg-dao 直种）；④LOCATION 主解析（locationCode 参数路径）；⑤unresolved 拒绝。执行期发现：findSkuByBarcode @BizQuery 直调需 IServiceContext（null NPE）→ 改 barcode 列 example 等价查询（唯一性由 erp-md.sku-barcode-unique 门控）；Delivery 反射单域容错（classpath 无 sal-dao 跳过）。
      Skill: nop-testing

Exit Criteria:

- [x] inv-service 测试全绿（261/0/0 含新增 7，既有零回归）；resolveBarcode 经 GraphQL 可达（JUnit 经 IGraphQLEngine/直调断言，执行期发现直调 findSkuByBarcode 需 IServiceContext 改 barcode 列等价查询）。

### Phase 2 — PDA 作业面页面（盘点扫码）+ 浏览器层验证

Status: completed
Targets: `module-inventory/erp-inv-web/.../pages/pda-stock-take/main.page.yaml`、菜单注册、E2E
Skill: nop-frontend-dev

- Item Types: `Add`

- [x] Add: pda-stock-take 页面（表单式：barcode 输入与**库位 code 输入并列同表单**[非时序步——pda-require-location-scan=true 时 resolve 的 STOCK_TAKE 分支校验需要 locationCode 先在]→ resolveBarcode 展示解析结果 → StockTake 定位 → 实盘数量输入 → StockTakeLine 保存按钮）+ 菜单注册（erp-inv.action-auth.xml SUBM orderNo=402，域 delta 自动流入聚合壳无需动 app 层）+ i18n 内联（i18n-coverage-checker 门 exit 0）。
      Skill: nop-frontend-dev
- [x] Add: E2E `tests/e2e/mobile/pda-stock-take.mobile.spec.ts`——双视口（375px/1280px）表单冒烟 2/2 绿（页面可达+input 渲染+PDA 文案断言；语义断言由 JUnit 承载）。
      Skill: nop-testing

Exit Criteria:

- [x] 页面经 flux 编译（validate:flux PASS 0 error）+ E2E 双视口绿（2/2）；菜单在站点地图可见（SUBM 注册 + 域 delta 流入聚合壳）。

### Phase 3 — 档位登记 + 全量回归

Status: completed
Targets: barcode-integration.md、customization-capabilities.md、e2e-runbook
Skill: none

- Item Types: `Add | Proof`

- [x] Add: barcode-integration.md 实现状态段（已实现面/残量面分界+统一触发条件+对账表回指）。
      Skill: none
- [x] Add: customization-capabilities.md 档位组装说明登记（US-IV-05/US-PL-08 随 inventory 模块；纯财务轻部署不装配）。
      Skill: none
- [x] Proof: 全量回归——`mvn clean install -DskipTests` BUILD SUCCESS + inv-service **261/0/0** 全绿（45 类，含新 TestErpInvBarcodeResolve 7/7）+ mobile 分层 **5/5**（3 原有 + 2 PDA 双视口）+ checker exit 0 + xdsl PASS + i18n-coverage exit 0。
      Skill: none
      Skill: none

Exit Criteria:

- [x] 三组 Proof 全绿；文档对齐完成（实现状态段/档位登记/runbook[E2E 段 USC-04 已建，本批补 PDA 页条目于 mobile 分层]）。

## Draft Review Record

- Independent draft review iteration 3: accept（授权回填——iteration 2 复审明示「修毕无需再起实质复审轮，可回填 iteration 3 为 accept、转 active」，单行文本修订已落实）
- Independent draft review iteration 2（定点清扫）: accept（同一独立子代理 agent_9f519997）——0 Blocker/0 Major/3 Minor/2 Cosmetic 全部单行文本级（n2 per-item Skill 残留/n3 Targets mfg 承诺悬空→补 MaterialIssue 断言/n4 Goals 与 D1 同步/n1 Skill 重复行/n5 页面时序表述/审查记录措辞更正），已全部落实；复审明示「修毕无需再起实质复审轮，可回填 iteration 3 为 accept、转 active」。
- Independent draft review iteration 1: needs revision（独立子代理 agent_9f519997，1 Blocker + 2 Major + 6 Minor；基线断言实仓核验为真）——B1 Delivery 跨域 pom 违反 sales→inventory 单向 DAG（修：IBizObjectManager 按名解析先例 + Delivery 断言移 app-erp-all + 否决 pom 加依赖/收窄三类的替代方案登记）；M1 pda-require-location-scan 键语义与页面流程自相矛盾（修：(a) 页面补库位输入步对齐设计场景 5）；M2 残量册不闭环（修：场景/码型×交付面映射表七场景+六码型逐项对账）；m1 beans 显式声明+BizObj 具名/m2 Phase 2 Skill 改 nop-frontend-dev/m3 DAO 直种轻路径+sal classpath 容错/m4 D1 替代方案段+SKU-first 歧义优先级/m5 i18n-coverage-checker 门/m6 %prod 菜单壳语义承继。B1/M1/M2 全部整改落盘；m2/m3 为半落地（per-item Skill 行残留旧值/①收货向旧含糊措辞残留），本轮定点清扫订正。

## Closure Gates

- [x] 范围内行为完成（解析入口 BizModel + 4 键接线 + PDA 盘点页 + 菜单 + JUnit 7/7 + E2E 2/2 + 文档×3）
- [x] 相关文档对齐（barcode-integration.md 实现状态段 + customization-capabilities 档位登记 + roadmap USC-05 行回写）
- [x] 已运行验证（inv-service 261 全绿含新增 7 + mobile 分层 5/5 + checker/xdsl/i18n 三门禁 + plan-gates + 全量构建）
- [x] 无范围内项目降级为 deferred/follow-up（对账表逐项具名 successor）
- [x] 独立草案审查已完成并记录（3 轮：1B[DAG]+2M+6m → 定点清扫 0B/3m/2c → accept）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致（日志条目随提交落盘 docs/logs/2026/10-01.md）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（审计方独立复跑确认 2 审计项于其审计时保持 [ ] 未预勾）
- [x] 结束证据存在于文件中（本计划 Closure 节 + barcode-integration.md 实现状态段 + customization-capabilities.md 档位登记 + docs/logs/2026/10-01.md）

## Deferred But Adjudicated

### 设计场景/码型 × 交付面对账（M2 逐项映射；统一触发条件=仓储客户 PDA 部署点名对应场景/码型）

| 场景/码型 | 本批交付面 | 残量（具名） | 触发条件 |
|---|---|---|---|
| 场景 1 收货 | resolveBarcode 单号解析（Receive 命中）+ 既有 approve 链 | PDA 收货作业页（本期仅盘点页） | PDA 部署点名收货 |
| 场景 2 上架 | 库位 code 解析（Location 查询） | 上架库位推荐（ABC/周转率）+ PDA 上架页 | PDA 部署点名上架 |
| 场景 3 拣货 | —（**PickingOrderBiz 纯 CRUD 无 mutation 落点缺失**） | 拣货 mutation + PDA 拣货页 | 先补拣货 mutation 立项 |
| 场景 4 发货 | resolveBarcode 单号解析（Delivery 按名解析）+ 既有 approve 链 | PDA 发货作业页 | PDA 部署点名发货 |
| 场景 5 盘点 | **全链**：resolveBarcode + pda-stock-take 页 + StockTakeLine 保存 + locationCode 门控 | —（本期完整交付） | — |
| 场景 6 领料 | resolveBarcode 单号解析（MaterialIssue 命中）+ 既有 confirm 链 | PDA 领料作业页 | PDA 部署点名领料 |
| 场景 7 质检采样 | —（qa 域联动缺失） | 质检扫码联动 | qa 域 PDA 需求 |
| SKU 码 | **findSkuByBarcode 委托解析**（既有）+ sku-format 校验 | — | — |
| 批次/序列号/托盘码 | —（无 barcode 数据列=ORM 保护区） | ORM 加列 + 解析 | ORM 加列立项 |
| 库位复合码 | code 约定解析（v1，设计反模式条款支持） | 复合码配置载体 | 仓库复杂布局需求 |
| 条码生成（ErpSysDocNumberRule） | —（纯设计零实现） | 标签生成/打印子系统（含 auto-print-on-receive 激活） | 打印需求立项 |

- Classification: `successor ownership`
- Why Not Blocking Closure: roadmap 档位语义（商贸/制造档 Must、纯财务轻部署不组装）+ 逐项触发条件驱动；场景 5 全链为本批 Must 交付代表。
- Successor Required: `yes`（逐行触发条件见表）。

## Closure

Status Note: 独立结束审计四轮收敛通过（round1 NEEDS REVISION 3M+2m+2c → round2 1B[runbook 被 roadmap 全文覆盖——执行期脚本变量误用事故]+m 残留 → round3 仅余日志行 → round4 行级确认 RESOLVED）。行为面/授权面独立复跑全绿（barcode 7/7 + Delivery 1/1 + mobile 5/5×2 + 四门禁 exit 0 + runbook vs HEAD +6/−1 + roadmap 266 零命中）。USC-05 可标记 done。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（fresh session，只读，2026-10-01；四轮收敛）
- Evidence: round1 NEEDS REVISION（3M[Delivery 断言缺失/计数块失真/runbook 未更新]+2m+2c）→ 三轮整改 → round4 行级确认 RESOLVED。独立复跑全绿：TestErpInvBarcodeResolve 7/7 + TestErpBarcodeDeliveryResolve 1/1（四类单据解析面 4/4 运行时覆盖）+ mobile 5/5（两轮独立复跑）+ plan-gates --strict/compliance/xdsl/i18n-coverage 四门禁 exit 0 + runbook vs HEAD +6/−1（覆盖灾难彻底回退）+ roadmap 266 零命中。round2 引入的 runbook 覆盖事故（执行期 python 脚本变量误用 s3=s.replace 将 roadmap 全文写入 runbook）经 git checkout 恢复+定点编辑重做，round3/round4 双轮核验原内容在位。执行期事故链已如实登记 docs/logs/2026/10-01.md。

Follow-up:

- （无——残项均具名 successor。）
