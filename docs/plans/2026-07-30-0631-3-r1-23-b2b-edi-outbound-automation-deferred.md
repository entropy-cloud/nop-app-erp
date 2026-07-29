# 2026-07-30-0631-3-r1-23-b2b-edi-outbound-automation-deferred b2b EDI 出站自动化 Deferred（MFT transport successor）

> Plan Status: completed
> Last Reviewed: 2026-07-30
> Source: audit-remediation-roadmap R1.23（P1-MA2-073，源自 A2.14 b2b 状态机审查）
> Related: `docs/audits/2026-07-28-1020-arm-ma2-ext-domains-state-machine.md`、`docs/audits/arm-index.md §P1-MA2-073`；plan `2026-07-30-0512-1-r1-18-assets-idle-state-machine-deferred.md`（Deferred 标注先例）、plan `2026-07-30-0631-2-r1-22-contract-negotiation-terminate-expiry-job-deferred.md`（同批 missing-automation Deferred 范式 P1-MA2-071）；`docs/design/b2b/managed-file-transfer.md`（MFT transport Non-Goal）；`docs/audits/arm-index.md §P1-MA2-033`（missing-automation P1 分级平行参照——该 finding 在 R1.13 中已实现，非 Deferred 先例）
> Audit: required

## Current Baseline

单项 finding 经实仓逐项确认：b2b EDI 出站自动化（自动发送 + ACK-timeout→ERROR + 自动重试 + ERROR>24h 升级）全部缺失，**不破坏已实现主路径**（EDI 8 态 markSent/markAcknowledged/retry/cancel/archive 全手工可达 + ASN 4 态 RECEIVED→MATCHED→RECEIVED_TO_STOCK 完整 + Webhook HMAC 验签 + eventId 去重 + UNIQUE 防重）。

**P1-MA2-073（b2b EDI 出站自动化全部缺失）— 确认：**
- `createOutbound:66-101` 留 TO_SEND；`TransportManager`（wired `app-service.beans.xml:49-50`）**生产代码零调用**（仅 test `TestErpB2bMftTransport.java:54`）——出站发送委托从未接线（bean comment `app-service.beans.xml:47-48` 称「被 EdiDocBizModel 出站发送委托调用」但代码不匹配：ErpB2bEdiDocBizModel 不注入 TransportManager）。
- `markSent/markAcknowledged/retry` 全手工；无 ACK-timeout→ERROR（无 `erp-b2b.ack-timeout-seconds` config + 无 Job 扫描 SENT 超时）；`retry:150-166` 仅手工 + retryCount++ 无自动触发 + 无指数退避；无 ERROR>24h 升级 Job。
- 全域无 `ErpB2b*Job.java` + 无 nop-job 注册 + 无 `*.job.xml`（grep 全 `module-b2b` 零匹配）。
- owner doc `state-machine.md §L-8（L63）`「自动重试最多 3 次（指数退避），耗尽后保留 ERROR 等待人工介入」+ `§6（L84）`「SENT→ERROR（ACK 超时 `erp-b2b.ack-timeout-seconds` 默认 24h 触发）」+ `§8（L126）`「ERROR 超过 24 小时未处理升级通知」+ §9 场景 C「系统每 30 分钟自动重试」。
- **关键缓解**：(1) 整个 b2b 子系统 **config-gated OFF 默认**（`erp-b2b.enabled` default false，`ErpB2bConfigs.java:9,27`）→ 默认 config 零生产暴露；(2) MFT transport 是 **Mock-only Deferred SPI**（`MockTransportAdapter` 唯一 impl，真实 AS2/SFTP/FTPS = `managed-file-transfer.md` Non-Goal）→ 出站自动化属 Deferred transport 集成范畴，非生产路径回归；(3) 所有状态迁移方法手工可达（状态机不破坏，仅未自动化）；(4) LOG.warn/error 提供运维可见性。
- **伴随 P2**：P2-MA2-068（state-machine.md 自动化承诺 vs README/MFT transport Deferred 文档内部不一致）+ P2-MA2-069（TO_CANCEL dict 死状态）——与本 finding 同根因，本计划一并 owner doc 标注对齐。
- **审计引用校正注记**：源审计 §4 称「§9 场景 C 系统每 30 分钟自动重试」——实仓 state-machine.md §9 场景 C 描述手工重试（B2B 管理员点击"重试"），真正的自动化承诺在 §6（自动重试策略）+ §4 + §L-8 callout。owner doc 标注时以 §6/§4/§L-8 为自动化承诺权威落点。

**保护区域：** 不触及会计/数据删除保护区域。本计划为纯 owner-doc 行为契约对齐（owner doc Deferred 标注），无代码/无 ORM 变更。

## Goals

- 消除 b2b 域 owner doc 与代码间出站自动化悬空：state-machine.md §L-8/§6/§8/§9 场景 C 明确「出站自动化 Deferred——MFT transport 真实对接（AS2/SFTP/FTPS）上线时实现」，bean comment 修正，伴随 P2（文档不一致 + TO_CANCEL dict 死状态）一并标注。
- owner doc 与代码（手工可达 + config-gated OFF + Mock transport）一致，无「文档承诺自动化但代码未实现」的悬空。

## Non-Goals

- 不实现 EDI 出站自动化 Job（P1-MA2-073）——裁决 Deferred（owner doc 正式化）。理由：(1) 整个 b2b 子系统 config-gated OFF 默认（`erp-b2b.enabled` default false）→ 默认 config 零生产暴露；(2) MFT transport 是 Mock-only Deferred SPI（真实 AS2/SFTP/FTPS = `managed-file-transfer.md` Non-Goal）→ 出站自动化属 Deferred transport 集成范畴；(3) 所有状态迁移方法手工可达（状态机不破坏）；(4) 与 R1.13 P1-MA2-033 + R1.22 P1-MA2-071 missing-automation Deferred 同型先例一致。successor：MFT transport 真实对接上线时实现 `ErpB2bEdiOutboundJob`。
- 不修正 `app-service.beans.xml:47-48` bean comment（生产 beans 配置属 app 层打包，非本域 owner doc 范畴——owner doc state-machine.md 标注 bean comment vs 代码不一致即可，successor 收敛）。
- 不实现 TO_CANCEL 两步取消（P2-MA2-069）——裁决 watch-only/Deferred（单步 SENT→CANCELLED 是功能等价简化）；dict TO_CANCEL 项保留为预留。

## Task Route

- Type: `app-layer design change`（owner doc 行为契约对齐，纯文档）
- Owner Docs: `docs/design/b2b/state-machine.md`
- Skill Selection Basis: 纯 owner doc Deferred 标注，无代码/ORM → `Skill: none`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline.

## Execution Plan

### Phase 1 - finding 裁决（Decision）

Status: completed
Targets: 本计划（裁决记录）
Skill: `none`

- Item Types: `Decision`
- Prereqs: none

- [x] **Decision**：P1-MA2-073 处置方案裁决（同型 missing-automation Deferred 范式，对齐 R1.13/R1.22 先例）。
      - b2b EDI 出站自动化缺失：**Deferred（与 arm-index 推荐方向一致）**。**arm-index 推荐对齐声明**：arm-index §P1-MA2-073 方案A（推荐）即「owner doc state-machine.md §L-8/§6/§8 标注 Deferred——MFT transport 真实对接上线时实现」——本计划采纳该推荐方向（不偏离）。理由：(1) 整个 b2b 子系统 config-gated OFF 默认（`erp-b2b.enabled` default false）→ 默认 config 零生产暴露；(2) MFT transport 是 Mock-only Deferred SPI（真实 AS2/SFTP/FTPS = `managed-file-transfer.md` Non-Goal）→ 出站自动化属 Deferred transport 集成范畴，非生产路径回归；(3) 所有状态迁移方法手工可达（状态机不破坏，仅未自动化）；(4) LOG.warn/error 提供运维可见性；(5) 与 R1.18/R1.20 + 同批 R1.22 P1-MA2-071（contract EXPIRED Job missing）missing-automation Deferred 范式一致（P1-MA2-033 在 R1.13 中已实现，仅作 missing-automation P1 分类平行参照）。残留风险：b2b-enabled=true（即 `erp-b2b.enabled`=true）时出站 EDI（TO_SEND/SENT/ERROR）生产环境无自动化推进（全依赖运营手工）→ successor 命名触发条件。伴随 P2（P2-MA2-068 文档不一致 + P2-MA2-069 TO_CANCEL dict 死状态）一并 owner doc 标注对齐。
      - Skill: `none`

Exit Criteria:

- [x] Phase 1 Decision 记录选择 + 理由 + arm-index 推荐对齐声明 + 残留风险 + successor 触发条件，Phase 2 严格遵循。

### Phase 2 - b2b EDI 出站自动化 + 伴随 P2 owner doc Deferred 标注（P1-MA2-073 + P2-MA2-068/069）

Status: completed
Targets: `docs/design/b2b/state-machine.md`
Skill: `none`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] state-machine.md §L-8（L63）+ §6（L84）+ §8（L126）+ §9 场景 C 标注「出站自动化（auto-send/ACK-timeout→ERROR/auto-retry 指数退避/ERROR>24h 升级）**Deferred**——MFT transport 真实对接（AS2/SFTP/FTPS）上线时实现 `ErpB2bEdiOutboundJob`（cron-gated，调用 TransportManager.send 推进 TO_SEND→SENT + ACK-timeout 扫描 SENT→ERROR + 自动重试 + ERROR>24h 升级经 `IErpSysNotificationBiz`），config-gated `erp-b2b.enabled` + transport-enabled」；每处命名 successor 触发条件；交叉链接 `managed-file-transfer.md` Non-Goal。
      - Skill: `none`
- [x] state-machine.md §实现偏离补注补「TransportManager bean（`app-service.beans.xml`）wired 但生产代码零调用（ErpB2bEdiDocBizModel 不注入 TransportManager，出站发送委托未接线）—— Deferred transport 集成范畴；当前 markSent/markAcknowledged/retry 全手工」；标注 bean comment vs 代码不一致（P2-MA2-068 文档内部不一致收敛）。
      - Skill: `none`
- [x] state-machine.md §2 ASCII 图 TO_CANCEL 中间态标注「**Deferred**——取消经单步 SENT→CANCELLED（功能等价简化），TO_CANCEL 两步取消（SENT→TO_CANCEL→CANCELLED）留 successor」；dict TO_CANCEL 项保留为预留语义入口（P2-MA2-069 收敛）。
      - Skill: `none`
- [x] state-machine.md §残留风险补注「`erp-b2b.enabled`=true 时出站 EDI 文档（TO_SEND/SENT/ERROR）生产环境无自动化推进——全依赖运营手工 markSent/markAcknowledged/retry；ERROR 状态文档可静默悬挂（仅 LOG.warn/error 可见性，无升级通知）；successor `ErpB2bEdiOutboundJob` 落地后闭合异步处理闭环」。
      - Skill: `none`

Exit Criteria:

- [x] state-machine.md 明确 073 + 伴随 P2 Deferred，owner doc 与代码（手工可达 + TransportManager uncalled + config-gated OFF + Mock transport）一致；successor 触发事件已命名；`erp-b2b.enabled`=true 残留风险已标注。

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is (ses_04ffc45d3ffeV9IpQw8cZ03GwW, fresh session) because 全部基线声明经实仓验证 TRUE（createOutbound:93 留 TO_SEND / markSent·markAcknowledged·retry 全手工 / TransportManager wired-but-uncalled[仅 TestErpB2bMftTransport:54 调用] / ErpB2bEdiDocBizModel 不注入 TransportManager / 零 ErpB2b*Job + 零 *.job.xml / ErpB2bConfigs 无 ack-timeout 键 / MockTransportAdapter 唯一 impl + managed-file-transfer.md Non-Goal / config-gated OFF 默认）；073 Deferred = arm-index 方案A（推荐）方向（无假偏离声明）；纯文档计划 mvn 门控按执行时规则 7 例外正确删除 + grep 验证；规则 13 满足（073 是 config-gated OFF + Mock transport Deferred 的 P1，Deferred 是审计自身推荐方向）；P2-068/069 折叠同根因同 doc 合规（规则 14）。修订：采纳非阻塞注记——(1) 配置键名 `erp-b2b.b2b-enabled`→`erp-b2b.enabled`（实仓 ErpB2bConfigs.java:9 权威键名）；(2) §9 场景 C 实为手工重试，自动化承诺权威落点为 §6/§4/§L-8（补审计引用校正注记）；(3) R1.13 P1-MA2-033 先例引用精度修正（已实现 openPeriod，仅作 P1 分级平行参照）。

## Closure Gates

> 本计划无代码/ORM 变更（纯 owner doc Deferred 标注），故删除 `mvn` 构建验证门控（见执行时规则 7 例外）。验证聚焦 owner doc 与代码一致性。

- [x] 范围内文档对齐完成（P1-MA2-073 + P2-MA2-068/069 裁决落地为 owner doc Deferred 标注）
- [x] 相关文档对齐（b2b/state-machine.md）
- [x] 已运行验证（grep 确认 TransportManager 生产代码零调用基线不变 + 无 ErpB2b*Job 基线不变 + owner doc Deferred 标注落地；compliance checker 本计划零新增命中）
- [x] 无范围内项目降级为 deferred/follow-up（Deferred 是处置裁决 + 已命名 successor，非范围内缺陷隐瞒）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### EDI 出站自动化 Job（P1-MA2-073 successor）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 整个 b2b 子系统 config-gated OFF 默认（`erp-b2b.enabled` default false）→ 默认 config 零生产暴露；MFT transport 是 Mock-only Deferred SPI（真实 AS2/SFTP/FTPS = `managed-file-transfer.md` Non-Goal）；所有状态迁移方法手工可达（状态机不破坏，仅未自动化）；LOG.warn/error 提供运维可见性。
- Successor Required: `yes`（MFT transport 真实对接（AS2/SFTP/FTPS）上线时实现 `ErpB2bEdiOutboundJob` cron-gated 调用 TransportManager.send 推进 TO_SEND→SENT + ACK-timeout 扫描 SENT→ERROR + 自动重试 3 次指数退避 + ERROR>24h 升级经 `IErpSysNotificationBiz` + `erp-b2b.ack-timeout-seconds` config）

### TO_CANCEL 两步取消（P2-MA2-069 successor）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 单步 SENT→CANCELLED 是功能等价简化（非死状态）；TO_CANCEL dict 项保留为预留语义入口。
- Successor Required: `yes`（业务要求两步取消确认（SENT→TO_CANCEL→CANCELLED）时实现 TO_CANCEL 迁移 + 确认动作）

## Closure

Status Note: 已完成。P1-MA2-073（b2b EDI 出站自动化缺失）+ 伴随 P2-MA2-068（文档内部不一致）+ P2-MA2-069（TO_CANCEL dict 死状态）裁决落地为 `docs/design/b2b/state-machine.md` 的 owner-doc Deferred 标注（顶部整体 banner + §L-8/§4/§6/§8/§9-C 各控制点注 + §2 TO_CANCEL 注 + 实现偏离补注 + 残留风险）。纯文档计划，零代码/ORM 变更；基线逐项 grep 确认不变（TransportManager 生产代码零调用 + 无 ErpB2b*Job + 无 ack-timeout config 键 + config-gated OFF 默认）。successor `ErpB2bEdiOutboundJob` 已在 banner + Deferred-But-Adjudicated 双重命名触发条件与 scope。独立会话结束审计 PASS（无缺陷）。

Closure Audit Evidence:

- 独立会话结束审计（非执行者自审）。逐项实仓复核通过：`grep -rn TransportManager module-b2b/erp-b2b-service/src/main` 仅命中 beans.xml:49-50 bean 定义 + TransportManager.java 类定义 + MockTransportAdapter.java 注释——**零生产调用**；ErpB2bEdiDocBizModel.java 唯一 @Inject 字段为 ErpB2bEdiRegistry（line 49-50），**不注入 TransportManager**，createOutbound:93 留 TO_SEND。
- 基线不变确认：`grep -rln "class ErpB2b.*Job" module-b2b` 零匹配；`grep -rn ack-timeout module-b2b` 零匹配（无 ack-timeout config 键）；无 `*.job.xml`、无 nop-job/@Scheduled/IJob 注册；`ErpB2bConfigs.java:27 DEFAULT_B2B_ENABLED = false`（config-gated OFF 默认）；`git status --short` 仅 docs/design/b2b/state-machine.md + 本计划文件——**零生产代码变更**。
- owner-doc 标注落地确认：state-machine.md 顶部整体 Deferred banner（citing P1-MA2-073+P2-068+P2-069）+ §L-8/§4/§6 四行/§8/§9-C 各带 Deferred 注 + 实现偏离补注（TransportManager wired-but-uncalled + bean comment vs 代码不一致 P2-068）+ §2 TO_CANCEL Deferred 注（单步 SENT→CANCELLED 功能等价、dict 保留预留 P2-069）+ 残留风险（erp-b2b.enabled=true 运营依赖）全部就位。
- 裁决对齐确认：arm-index §P1-MA2-073 推荐方案 A（owner-doc Deferred 标注），本计划采纳该方向；理由逐项证真——MockTransportAdapter 为唯一 transport impl + managed-file-transfer.md:8 Non-Goal（真实 AS2/SFTP/FTPS = follow-up）+ 手工状态迁移全可达 + LOG.warn/error 可见性 + R1.18/R1.22 同型 missing-automation Deferred 先例存在 + IErpSysNotificationBiz 存在于 module-notify。
- successor 命名确认：ErpB2bEdiOutboundJob 触发条件（MFT 真实对接 AS2/SFTP/FTPS 上线）+ 完整 scope（TransportManager.send 推进 TO_SEND→SENT + ACK-timeout 扫描 SENT→ERROR + 自动重试指数退避 + ERROR>24h 升级经 IErpSysNotificationBiz + erp-b2b.ack-timeout-seconds config）已在 banner 与 Deferred-But-Adjudicated 双重落点。
- Phase 1/Phase 2 均为 Status: completed 且全部 checklist [x]；纯文档计划，无代码/ORM/bean-comment 编辑，规则 7 mvn 门控例外成立。

Follow-up:

- 非阻塞；successor 已在 Deferred But Adjudicated 命名触发条件（MFT transport 真实对接上线时）。
