# 2026-09-28-0835-1 服务层 N+1/热点治理批（事务内外发重排部分裁决 + 5 站点等价批量化）

> Plan Status: completed
> Last Reviewed: 2026-09-28
> Source: `docs/analysis/2026-09-27-perf-ux-deep-optimization-analysis.md` §5 批次 3（G1/E2/E3/B3/B4/B5/J1/J3）；G1 事务外重排经实时基线复核后部分改判 Deferred（见 Decision-1）
> Related: `docs/plans/2026-09-27-0325-2-dashboard-report-query-perf.md`（批次 2，同源分析报告；无代码依赖）
> Audit: required

## Current Baseline

- 本计划全部站点于 2026-09-28 实时工作树逐一定位核实（行号为当前工作树实核值）。**在途冲突姿态（草案审查 iteration 1 更正）**：E2 两文件（`ErpFinDashboardBizModel.java`/`ErpFinReportBizModel.java`）在批次 2（plan `2026-09-27-0325-2`）在途未提交改动清单内——本计划实施时点必晚于批次 2 提交（提交后其形态即 HEAD），**实施前须按提交后形态重新实核 E2 行号锚点**；其余站点文件均无在途冲突。
- **G1**（`module-master-data/erp-md-service/src/main/java/app/erp/md/service/processor/ErpMdCurrencyRefreshRatesFromApiProcessor.java`）：refreshRatesFromApi 编排 = 加载全部币种（:44-57）→ `exchangeRateApiClientFactory.fetchRates`（:60，HTTP 位）→ 逐汇率 `findExistingRate`（:77-78，每汇率 1 次 eq×3 查询）+ 逐行 save/update（:89-93）。**实时事实**：`ErpMdExchangeRateApiClientFactory.instantiateClient` 当前仅落地 `"mock"` provider（真实 provider 抛 ERR_EXCHANGE_RATE_API_UNAVAILABLE 归 successor），且 config `erp-md.exchange-rate-api-enabled` 默认关——「HTTP 阻塞事务」当前为结构性风险而非现实风险。调用链：`ErpMdCurrencyBizModel.refreshRatesFromApi`（@BizMutation，事务边界）→ processor 全程。owner doc 已有方向裁决：`docs/architecture/external-api-integration-pattern.md` §6.3「API client 不可在事务内阻塞」+ AP5 反模式表（afterCommit 异步 / cron 轮询）。
- **E2**（`module-finance/erp-fin-service/.../dashboard/ErpFinDashboardBizModel.java` + `report/ErpFinReportBizModel.java`；两文件属批次 2 在途改动，锚点=批次 2 提交后形态，实施前重核）：getDashboardKpi（:62-81）请求内 **3 处** `AcctSchemaResolver.resolvePrimarySchemaId` 调用点 = sumArApOpen AR/AP 两腿（各经 `applyOrgAndSchemaScope`，:421-431）+ **损益腿 `aggPnlActivityBySubject`（:246）**；`sumBankBalance`（:370-384）**无** org/schema scope 过滤、不参与（草案审查 iteration 1 更正：三腿归因原表述有误）。3 次 resolve 的 orgId 同源于 `resolvePeriodOrgId(periodId)`——同一期间实体亦被重复加载 3 次；Trend loader（:195-210、:295-310）各 resolve 1 次；fin report :631 每 report 方法 resolve 1 次。全仓 AcctSchemaResolver 调用点 56（审查实测量化，≥分析报告「约 30+」；assets/fin 过账 Dispatcher 各 1 次/过账）。
- **E3**（`module-notify/erp-notify-service/.../dispatch/NotificationDispatcher.java`）：`prepare` 接收人循环（:103-107）内逐人 `parseChannelSet(template.getChannelSet())`（:105）；`mergeOrPersist` 内逐人 `AppConfig.var(CONFIG_NOTIFY_MERGE_ENABLED)`（:144-145）；`dispatchExternalChannels` 逐通知逐通道 `AppConfig.var`（:179、:188，EMAIL/SMS 各 1 次/通知）。
- **B5**（`module-notify/.../dispatch/NotificationMergeCoordinator.java`）：`findMergeable`（:46-75）候选查询（limit 10）后逐候选 `isRead`（:69-73，每候选 1 次查询）——每接收人最多 11 次查询。
- **B3**（`module-sales/erp-sal-service/.../processor/ErpSalReturnProcessor.java`）：`updateUndeliveredQuantity`（:499-526）两处 N+1——退货行循环逐行 `deliveryLineDao.getEntityById`（:508）；orderLineId 循环逐行 `orderLineDao.getEntityById`（:519）。:518-525 循环逐行 updateEntity（updateEntity 在 :523）保持不动（C1 Deferred 范围）。
- **B4**（`module-b2b/erp-b2b-service/.../processor/ErpB2bAsnCreateReceiveFromAsnProcessor.java`）：`fillReceiveLinesFromAsn` AsnLine 循环逐行 `materialDao.getEntityById`（:143）。PO 行反查已批量化（findPoLines 一次性），material 是唯一残留逐行站点。
- **J1**：全仓 src/main `Pattern.compile` 非静态站点**零命中**（分析报告所列 `NotificationRecipientResolver.java:63` 实为 `private static final java.util.regex.Pattern` 全限定名书写致 grep 误报，已实核排除）。
- **J3**：`String.format("%02x", b)` 字节转 hex 循环 3 处——`ErpB2bAsnHandleInboundWebhookProcessor.java:226`（HMAC 签名比对）、`ErpLogShipmentHandleTrackingWebhookProcessor.java:80`（同型）、`MockTransportAdapter.java:78`（mock SPI）。sal dashboard 月桶 `String.format("%02d")`（:124、:130，12 次/请求）量级可忽略。
- 数值/行为护栏已就位：md `TestErpMdCurrencyRefreshRatesDate` + `TestErpMdExchangeRateApiClient`；fin `TestErpFinDashboard` + `TestErpFinReportRendering`；notify `TestErpSysNotificationDispatch`（merge 路径）等 8 个 notify 测试类；sal 10+ `TestErpSalReturn*`（含 deliveredQuantity 回填断言）；b2b `TestErpB2bAsnInbound`/`TestErpB2bAsnInventoryIntegration`。
- 剩余差距：单据行级循环读放大（行数量级小但逐行往返）；notify 广播场景逐接收人查询/配置解析放大；fin 看板每请求 3 次重复 resolve。真实 HTTP provider 落地后事务内阻塞风险将现实化（本计划只登记触发条件，不实施重排）。

## Goals

- B3/B4/B5/G1(批量部分) 四处循环内逐行读全部改为一次性 `in(...)` 批量预载 + 内存 Map（纯等价变换：过滤条件、空值跳过、异常抛出语义逐字平移）。
- E3 notify 派发路径循环内重复解析（channelSet parse / AppConfig.var）全部提升到循环外，单次解析传参。
- E2 fin dashboard KPI 请求内 3 次重复 schema resolve + 3 次重复期间加载合并为每请求单次（文件内私有方法签名调整，跨请求缓存不做）。
- J3 两处生产 hex 循环改查表等价实现（或实证存在的平台 hex 工具）。
- 全部等价变换以既有 JUnit 行为断言为护栏，触及模块测试 0 failures。

## Non-Goals

- 不改任何 API 契约（api.xml/xbiz/IBiz 签名不变；`findMergeable` 公有签名不变）。
- 不实施 G1 事务外重排/异步化（Decision-1 改判 Deferred；不改 @BizMutation 事务边界，不新增 cron/异步队列）。
- 不实施真实汇率 provider（exchangerate-host/fixed-fetch）——既有 successor 义务不变。
- 不做 notify 外发通道批量化/异步化（I2 Deferred）、不做跨接收人候选合并查询（Decision-3 否决）、不做循环内 save/updateEntity 批量化（C1 Deferred）。
- 不动 fin 过账写路径与 30+ 过账 Dispatcher 的 AcctSchemaResolver 单次调用（Decision-2 豁免登记）。
- 不动 AcctSchemaResolver 静态工具类本身的实现与全仓调用点。
- 不做 MockTransportAdapter hex 循环（mock SPI，非生产路径，豁免登记）。

## Task Route

- Type: `implementation-only change`（读路径等价变换 + 循环外提升，用户可见行为不变）
- Owner Docs: `docs/design/master-data/exchange-rate-management.md`（汇率刷新语义）、`docs/design/notify/`（README + inbox-patterns，通知派发语义）+ `docs/architecture/notification-strategy.md`、`docs/architecture/external-api-integration-pattern.md` §6.3（G1 改判依据）
- Skill Selection Basis: 已扫描 `docs/skills/README.md`——无计划起草/服务层重构匹配技能（审计类技能 plan-audit/closure-audit 留待审查与结束阶段使用）；实施后以 `code-quality-audit-prompt.md` 自检

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（JUnit embedded 基建已存在；不触 config 键值——AppConfig.var 仅提升调用位置，读取语义不变）

## Execution Plan

### Phase 1 - master-data 汇率刷新批量预载（G1 批量部分）

Status: completed
Targets: `module-master-data/erp-md-service/src/main/java/app/erp/md/service/processor/ErpMdCurrencyRefreshRatesFromApiProcessor.java`
Skill: none

- Item Types: `Fix`×1 + `Decision`×1
- Prereqs: none

- [x] Fix `findExistingRate` 逐汇率查询改单次批量：以 `eq("fromCurrencyId", baseId) + eq("validFrom", validFrom) + in("toCurrencyId", targetCurrencyIds)` 一次查询建 `toCurrencyId → ErpMdExchangeRate` Map，upsert 循环改 Map 查找（命中即 update、未命中即 new，原 `list.get(0)` 语义 = Map 同键单值；币种数 ≪500 不分块，留痕理由：单 provider 全量币种量级）。**fetch-then-upsert 顺序与逐行 save/update 保持不变**（后者属 C1 Deferred）。等价性边界留痕（草案审查 M3）：Map 同键单值与原 `list.get(0)` 的等价以 `(fromCurrencyId, toCurrencyId, validFrom)` 幂等键唯一性（:62 设计假设）为界——若存在历史重复行，新旧实现同为非确定取一，等价性不降级
      - Skill: none
- [x] Decision-1（G1 事务外重排改判 Deferred）：实时基线证实 factory 仅 mock provider 且 config 默认关，HTTP 阻塞事务为结构性潜在风险；事务外重排须改 @BizMutation 边界或引入 cron/异步（行为/部署面变更，owner doc §6.3/AP5 方向），当前实施零现实收益且扩测试面。理由与替代方案（ suspension 不可用于 BizModel / afterCommit 适用外部写不适用预取 / cron=部署面新增）记入计划本节下方；Successor 触发条件 = 真实 provider 立项（exchangerate-host/fixed-fetch 任一落地时，按 §6.3/AP5 同步落事务外取数）
      - Skill: none

Exit Criteria:

- [x] `TestErpMdCurrencyRefreshRatesDate` + `TestErpMdExchangeRateApiClient` 全绿（0 failures），refresh 行为断言（幂等 upsert、速率实体字段）不变
- [x] upsert 循环内 DB 往返从「每汇率 1 次查询」降为「整批 1 次查询」（代码结构可证）

### Phase 2 - finance 看板/报表请求级 resolve 合并（E2）

Status: completed
Targets: `module-finance/erp-fin-service/src/main/java/app/erp/fin/service/dashboard/ErpFinDashboardBizModel.java`、`module-finance/erp-fin-service/src/main/java/app/erp/fin/service/report/ErpFinReportBizModel.java`
Skill: none

- Item Types: `Fix`×1 + `Decision`×1
- Prereqs: 批次 2（plan `2026-09-27-0325-2`）已提交、E2 行号锚点已按提交后形态重核（定点复审建议补记）

- [x] Fix getDashboardKpi 请求内合并：单次 `resolvePeriodOrgId(periodId)` + 单次 `AcctSchemaResolver.resolvePrimarySchemaId` 结果传递给 **sumArApOpen（AR/AP 两腿，经 applyOrgAndSchemaScope）与损益腿 aggPnlActivityBySubject 共 3 处调用点**；`sumBankBalance` 无 scope 过滤、不参与合并，**不得**为其新增 scope（= 行为变更，违反数值逐位不变）；私有方法签名内部调整，orgId null-skip 与 schemaId null-skip 过滤语义逐字保留（batch 2 已在 `applyOrgAndSchemaScope` 登记该语义）；若 Trend/Report 存在同请求重复 resolve 同型站点则同法合并，单次站点登记豁免
      - Skill: none
- [x] Decision-2（E2 范围裁决）：请求级 memo 采用「BizModel 入口单次 resolve 传参」而非 ThreadLocal/静态缓存——ThreadLocal 有请求生命周期清理风险、跨请求缓存有失效复杂度（分析报告原文即排除）；30+ 过账 Dispatcher 每过账 1 次 resolve = 最小放大，豁免登记不改
      - Skill: none

Exit Criteria:

- [x] `TestErpFinDashboard` + `TestErpFinReportRendering` 全绿数值逐位不变
- [x] getDashboardKpi 单请求内 resolvePrimarySchemaId 调用从 3 处（sumArApOpen×2 + 损益腿 aggPnlActivityBySubject）降为 1 处（代码结构可证）

### Phase 3 - notify 派发循环外提升 + 合并协调器批量 isRead（E3+B5）

Status: completed
Targets: `module-notify/erp-notify-service/src/main/java/app/erp/notify/service/dispatch/NotificationDispatcher.java`、`module-notify/erp-notify-service/src/main/java/app/erp/notify/service/dispatch/NotificationMergeCoordinator.java`、`module-notify/erp-notify-service/src/test/java/app/erp/notify/service/TestErpSysNotificationDispatch.java`
Skill: none

- Item Types: `Fix`×2 + `Decision`×1 + `Proof`×1
- Prereqs: none

- [x] Fix E3：`prepare` 循环外 `parseChannelSet` 一次 + `AppConfig.var(CONFIG_NOTIFY_MERGE_ENABLED)` 一次，传参入 `mergeOrPersist`（私有签名调整）；`dispatchExternal` 入口统一读 EMAIL/SMS 两 config 各一次传参入 `dispatchExternalChannels`（config 值请求内静态，读取位置移动零语义差）
      - Skill: none
- [x] Fix B5：`findMergeable` 内逐候选 `isRead` 改单次 `in("notificationId", candidateIds) + eq("userId", recipientUserId)` 批量查已读集 → 内存按候选 createTime desc 序取首个未读（公有签名不变；候选 ≤10 不分块，留痕）
      - Skill: none
- [x] Decision-3（跨接收人候选批查询否决）：单查询 `in(recipientUserId, allUsers)` 跨接收人取候选无法保持「每用户 top-10 by createTime desc」窗口语义（全局 limit 会破坏 per-user 截断；不设 limit 则窗口内全量加载无上界）——维持每用户候选查询 + 仅 isRead 批量化（每接收人 ≤11 次查询 → ≤2 次）
      - Skill: none
- [x] Proof：`TestErpSysNotificationDispatch` 全绿；若既有断言未覆盖「窗口内候选全部已读 → 不合并、新建」分支，则先补该断言再改（test-first 钉死 B5 选择语义）
      - Skill: none

Exit Criteria:

- [x] notify 触及模块 `mvn test -pl module-notify/erp-notify-service -am` 0 failures
- [x] merge 命中/未合并/全部已读三分支行为断言在位

### Phase 4 - sales 退货回填 + b2b ASN 收货行批量预载（B3+B4）

Status: completed
Targets: `module-sales/erp-sal-service/src/main/java/app/erp/sal/service/processor/ErpSalReturnProcessor.java`、`module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/processor/ErpB2bAsnCreateReceiveFromAsnProcessor.java`
Skill: none

- Item Types: `Fix`×2
- Prereqs: none

- [x] Fix B3 `updateUndeliveredQuantity`：退货行循环前按 `in("id", deliveryLineIds)` 批量取 deliveryLine 建 Map（null deliveryLineId 跳过语义保留）；orderLineId 循环前按 `in("id", orderLineIds)` 批量取 orderLine 建 Map（null orderLineId 跳过语义保留）；逐行 updateEntity 循环原样保留（C1 范围）
      - Skill: none
- [x] Fix B4 `fillReceiveLinesFromAsn`：循环前 distinct materialIds `in("id", ids)` 批量取 material 建 Map；循环内改 Map 查找，**两类异常语义逐字保留**——materialId 为 null 抛错（ARG_MATERIAL_ID=null）与 map 未命中抛错（ARG_MATERIAL_ID=materialId）参数不变
      - Skill: none
- 分块纪律留痕：B3/B4 均为单据行级集合（单退货行数/单 ASN 行数/行引用物料数），量级 ≪500，不做分块（同批次 2 inv toTraceRows 先例）

Exit Criteria:

- [x] sal 模块 `mvn test -pl module-sales/erp-sal-service -am` 0 failures（deliveredQuantity 回填断言在位）
- [x] b2b 模块 `mvn test -pl module-b2b/erp-b2b-service -am` 0 failures（含物料缺失抛错场景断言如既有覆盖；未覆盖则补）

### Phase 5 - webhook 签名 hex 循环等价替换（J3）

Status: completed
Targets: `module-b2b/erp-b2b-service/src/main/java/app/erp/b2b/service/processor/ErpB2bAsnHandleInboundWebhookProcessor.java`、`module-logistics/erp-log-service/src/main/java/app/erp/log/service/processor/ErpLogShipmentHandleTrackingWebhookProcessor.java`
Skill: none

- Item Types: `Fix`×2 + `Decision`×1
- Prereqs: none

- [x] Decision-4（J1/J3 终态登记）：J1 实例字段 Pattern 全仓零站点（分析报告所列 NotificationRecipientResolver:63 为全限定名静态常量误报，登记更正）；J3 修复范围 = 两处生产 webhook hex 循环，MockTransportAdapter:78（mock SPI）与 sal dashboard 月桶 `%02d`（12 次/请求）豁免登记
      - Skill: none
- [x] Fix 两处 `String.format("%02x", b)` 循环改 16 字符查表 append（等价输出：小写 hex、逐字节两位）；实现时先查证消费 jar 中 `io.nop.commons` 是否已有 hex 工具（有则用平台工具，无则查表实现并在代码注记说明）——**实现采用平台工具路径**：实证 `io.nop.commons.util.StringHelper.bytesToHex(byte[])`（nop-commons sources :734-743，小写输出）与原 `%02x` 循环逐位等价，两文件均改为 `StringHelper.bytesToHex(raw)`
      - Skill: none

Exit Criteria:

- [x] 两文件所在模块测试 0 failures（hex 输出被签名比对断言间接守护；如模块无直接断言则补一条 hex 等值断言）——实核：b2b `testWebhookValidSignatureCreatesAsn`/`testWebhookInvalidSignatureRejected` 与 log `TestErpLogShipmentPostingEnd`/`TestErpLogFreightPosting` 均以 hmacSha256 全链路往返强守护（hex 变化即拒收失败），满足间接守护条件，无需另补断言
- [x] b2b `mvn test -pl module-b2b/erp-b2b-service -am` 与 log 模块 `mvn test -pl module-logistics/erp-log-service -am` 全绿

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 agent_8fe34147，2026-09-28）——**Major F1**：基线首条「触及文件均不在批次 2 在途清单内」不实（fin dashboard/report 两文件属批次 2 Targets 且工作树含其改动，E2 锚点依赖在途形态）；**Major F2**：E2 三腿归因错误（sumBankBalance 两个版本均无 applyOrgAndSchemaScope 调用，第 3 处 resolve 实为损益腿 aggPnlActivityBySubject :246；Fix 文本按字面执行无法达成退出标准且有给 sumBankBalance 新增 scope 的行为变更风险）；Minor M1（applyOrgAndSchemaScope 实为 :421-431）/M2（B3 方法区间与 updateEntity 行号漂移）/M3（G1 Map 等价性须以幂等键假设为界留痕）。其余全部维度实核通过（G1/E3/B5/B3/B4/J1/J3 站点、Decision-1~4、owner docs、护栏测试、保护区声明、验证命令）。修订：基线首条改述在途冲突姿态（E2 锚点=批次 2 提交后形态+实施前重核）；E2 基线/Fix/退出标准改为「sumArApOpen×2 + 损益腿共 3 处合并，sumBankBalance 不参与且禁新增 scope」；M1/M2 行号更正；M3 等价性边界留痕。
- Independent draft review iteration 2: accept（同一审查代理定点复审，2026-09-28）——F1/F2/M1/M2/M3 全部忠实消解，无新引入不一致；唯一化妆品级建议（Phase 2 Prereqs 行补批次 2 时序前置）已采纳。**计划可进入实施**（Plan Status 置 active；实施时点晚于批次 2 提交）。

## Closure Gates

- [x] 范围内行为完成（Phase 1-5 全部退出标准达成）
- [x] 相关文档对齐：`docs/logs/2026/09-28.md` 新建登记；分析报告 §2 G1 行追加批次 3 处置回填（含 Decision-1 改判指向）；owner docs 若含派发/汇率实现机制注记则同步（行为语义不变则仅核对）
- [x] 已运行验证：触及模块（md/fin/notify/sal/b2b/log）`mvn test -pl <module> -am` 各 0 failures + `mvn clean install -DskipTests` 全仓 BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh`（漂移逐站点分类，新增 daoFor/import 须对照基线裁决）
- [x] 无范围内项目降级为 deferred/follow-up（G1 重排为计划起草时基于实时基线的改判并留痕 Decision-1，非执行期降级）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### G1 事务外重排 / 异步化（HTTP 出事务）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 当前仅 mock provider 且 config 默认关，HTTP 阻塞为结构性潜在风险；重排须改 @BizMutation 事务边界语义或新增 cron/异步（行为/部署面），owner doc `external-api-integration-pattern.md` §6.3/AP5 已载方向，实施时机应与真实 provider 立项绑定
- Successor Required: `yes`——触发条件：exchangerate-host/fixed-fetch 等真实 provider 实现立项时，按 §6.3/AP5 同步落地「事务外取数 + 事务内落库」（afterCommit 不适用预取；候选形态=cron 轮询拉取落库或 mutation 前置查询+apply 两段），并补事务边界测试

### notify 外发通道批量化 / 异步队列（I2，沿袭分析报告 §6）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 涉及 sender SPI 扩展/异步队列选型（nop-message 总线）
- Successor Required: `yes`——触发条件：广播量级实测（>500 人）或 nop-message 总线接入时

### 循环内 save/updateEntity 批量化（C1 子集：本批触及循环的写侧）

- Classification: `optimization candidate`
- Why Not Blocking Closure: Nop session flush 可 JDBC batch，放大系数未证实；分析报告裁决先 spike 平台批量 API
- Successor Required: `yes`——触发条件：平台批量 API spike 结论落地，或 HR payroll/notify 广播实测超标

## Closure

Status Note: Phase 1-5 全部完成。5 站点循环读放大消除（G1 汇率 upsert 前 in(toCurrencyId) 单次批载、B3 退货回填两处 getEntityById 批量化、B4 ASN 物料批量化、B5 isRead ≤11→≤2 次/接收人）、E3 notify 派发循环内重复解析全部提升（channelSet 每接收人 1 次→每派发 1 次、merge/EMAIL/SMS config 同法）、E2 fin KPI 请求 scope 解析 3 处→1 处（OrgSchemaScope 单次解析传递，sumBankBalance 无 scope 不参与、禁新增 scope 的约束恪守）、J3 两处 webhook hex 循环改平台 StringHelper.bytesToHex（小写逐位等价）。B5 test-first 新增守护测试 testMergeSkipsReadNotifications（候选全部已读→不合并新建）。全部等价变换以既有 JUnit 断言为护栏：触及六模块联合 mvn test 全绿（md/fin 首轮按序通过，notify/sal/b2b/log 恢复轮通过，b2b 82 tests 0 failures）+ `mvn clean install -DskipTests` 全仓 BUILD SUCCESS + checker R2b 236/R2c 1563 与 HEAD 逐项一致（本批零新增 daoFor，零漂移）。执行期纠错一处：NotificationMergeCoordinator 缺 java.util.Set 导入致联合测试首轮编译失败，修复后全绿（护栏有效）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理 agent_36981693（fresh session），2026-09-28——**FINAL VERDICT: ACCEPT**（无 Blocker/无 Major）。六门控全 PASS：文本一致性（执行者未预勾结束审计门控）；逐文件 diff 等价性（9 Java 文件实核——E2 sumBankBalance 零改动未新增过滤、B5 候选查询未动逐位等价、B4 异常参数逐字保留、J3 经 .m2 字节码实证 Base16.encode(bytes,false) 小写等价）；新增测试质量（testMergeSkipsReadNotifications 纯守护测试，7/0/0 实测）；验证执行（notify -am 独立复跑 + 其余 5 模块独立复跑 md 166/fin 548/sal 331/b2b 82/log 67 全 0 failures + checker R2b 236/R2c 1563 独立复跑与声称一致 + daoFor 净零）；Deferred 诚实性（mock-only + config 默认关实核）；无执行期降级。Minor 4 条：日志文件计数笔误（10→9 已更正）、提交显式限定文件集、notify `_cases` 快照随批提交、compliance-baseline.md 基线块记账同步归既有 successor（预存）。

Follow-up:

- （无阻塞跟进）
