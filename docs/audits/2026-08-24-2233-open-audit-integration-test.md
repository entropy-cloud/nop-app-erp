# 开放式审计报告 — mission `integration-test`（重执行，当前 HEAD 复核）

> Audit Status: planned
> Audit Type: open-ended
> Mission: integration-test
> Remediation: P1 发现 OA-01/OA-02/OA-03 已路由至计划 `docs/plans/2026-08-25-0330-1-aggregate-notify-cs-seeds-into-app-init-data.md`（OA-01）、`docs/plans/2026-08-25-0330-2-fix-closeperiod-fx-flush-profit-loss.md`（OA-02）、`docs/plans/2026-08-25-0330-3-adjudicate-integration-test-defect-routing.md`（OA-03，含 C18/C20b 分诊与勘误→缺陷路由通道立法）——**三计划均已执行完毕（Plan Status: completed，2026-08-25；各自 Closure 段含独立结束审计证据与验证基线）**，修复产物回链 arm-index P1 汇总区 :728 行（OA-01/OA-02/OA-03 分项已修复注记 + bugs 登记路径）。P2 发现 OA-04/05/06/07 + P2-IT-01..04 均已入 `docs/backlog/integration-test-roadmap.md` § Follow-up Backlog（OA-07 已于 mission-driver 起草门步骤补登）。

- **审计对象**：`integration-test` mission 整件工作的**项目级完整上下文**——M0.1-M0.3 + B1-B10（22 用例）+ V.1/V.2 收口 + 窗口内同前缀关联计划（0900-1/1147-1/2115-1），对照 AGENTS.md、owner docs（integration-testing.md / seed-data.md / e2e-runbook.md / drp/README.md / period-close.md）、arm-index、已知失败模式 9-13 与实仓代码/配置/快照的交集。非单一计划核实（反窄化自检见文末）。
- **审计者**：独立开放审计子代理（fresh session，无执行者上下文；mission-driver 2026-08-24-223318 派发，2026-08-25 当前 HEAD 重执行）。
- **与首轮报告的关系**：本文件覆盖同路径首轮草稿（`Audit Status: planned`）。首轮 P1/P2 结论经本轮**全量独立复核确认**（证据链逐项在实仓重跑），并新增：① 当前 HEAD 活体验证（测试套件 + compliance checker，本轮独立执行）；② 三份 0330-* remediation 计划的路由状态与草案审查证据核验；③ 一项新 P2（OA-07）。
- **与姊妹 multi-audit 的关系**：不重复其四项 P2（P2-IT-01..04，本轮复核均仍未修复、backlog 在盘）；其活体验证口径本轮独立复现一致。
- **本轮活体验证（2026-08-25 04:2x，前置 `lsof` 8011/8080 无 live server）**：
  - `mvn test -pl app-erp-all`：**54/0/0/1，BUILD SUCCESS，墙钟 01:37**——与 V.1 基线逐项零漂移（独立复现，非转抄）。
  - `bash docs/audits/nop-compliance-checker.sh`：R1d=14/R2a=34/R2b=237/R2c=1505/R2d=38/R3=5/R6=2/R10=12/R12a/b/c=70/66/41——与 `compliance-baseline.md` §BASELINE 逐项相等零漂移（lesson 07 复核通过）。
- **实仓结构核验摘录**：`it/` = 22 用例类 + 试点 + 基类 + 6 冻结时钟扩展 = 30 文件，**24 测试类各恰 1 `@Test`**（逐类 grep 异常集为空）；全部 `@NopTestConfig(localDb = false)` + `@NopTestProperty(nop.orm.init-database-data=true)`（grep -L 零缺失）；`it/` 零裸时钟调用；`db/`、`_tmp/` gitignore 正确（`git check-ignore` 实证）；`_init-data/` = 94 CSV + `zz-sequence-advance.sql` 共 95 文件；C13 快照通配纪律抽查成立（response 层 `*` 仅 4 响应 × 5 时间戳字段 = closedAt/createTime/endDate/reverseCloseAt/updateTime，tables 层 1130/150 金额全字面值）；全仓 `_seed_*.sql` deploy SQL 仅 notify/cs 两族。

---

## 发现（按严重性排序）

> 本审计 **0 × P0 / 3 × P1 / 8 × P2**（3 P1 + 7 P2 为首轮发现的本轮复核确认，1 P2 为本轮新增）。P1 均非 mission 测试交付物缺陷（测试工程本身质量与验证纪律过硬），而是 mission 执行期**发现的生产行为缺口被「勘误」口径吸收**的形态——首轮已路由至三份 active 计划，本轮确认路由在位但**缺陷在计划执行前仍存在于生产代码/种子资产**。

### [P1] OA-01 聚合 app 部署种子缺失模块级业务种子（notify 模板 27 行 + cs 编号规则 1 行）——owner doc 根因误判 + 系统性聚合缺口（首轮发现，本轮全链复核确认，路由 0330-1 active 未执行）
- **一行理由**：module-notify/module-cs 的业务关键种子只存在于模块 `deploy/sql/` 三方言 SQL，从未聚合进聚合 app 唯一自动加载的种子源 `app-erp-all/src/main/resources/_vfs/_init-data/`（本轮实证 95 文件零命中），聚合 app fresh-DB 模式（E2E/演示/集成测试同源）下通知子系统整体静默失活——真实产品级种子契约漂移，且被设计文档错误归因为「未实现动作面」。
- **本轮复核证据链**：
  1. `module-notify/deploy/sql/{mysql,oracle,postgresql}/_seed_erp-notify.sql` 承载 `erp_sys_notification_template` 27 行（7101 `cs.sla-overdue` … 7134 `wf.hr-salary.cc` 审批族 … 7201-7209 `log.draft-escalation`/`cs.ticket-created`/`cs.ticket-assign-no-match`/`cs.knowledge-suggest-create`/`cs.survey-invitation`/`cs.fulfillment-*`/`mnt.equipment-*`）；`module-cs/deploy/sql/{三方言}/_seed_erp-cs.sql` 承载 `nop_sys_code_rule` `cs-ticket-code`（`TK{@year}{@month}{@csTicketMonthSeq:4}`，1 行）。全仓仅此两族 `_seed_*` deploy SQL（find 实证）。
  2. `_init-data/` 95 文件（94 CSV + `zz-sequence-advance.sql`）零 `erp_sys_notification_template`/`nop_sys_code_rule` 行（ls + grep 实证）；deploy SQL 无任何自动消费路径（app-erp-all/module-notify/module-cs 生产代码与配置全检索零命中）；`DataInitInitializer` 只装载 `_init-data`（seed-data.md:71 自述）。
  3. 根因误判实证：`docs/design/integration-testing.md:458` 勘误(4) 记 C16 工单链 notify「均模板缺失静默降级不落库……为**未实现动作面**」——但 module-cs 派发实现自 2026-08-18 起在位（本轮 grep：`ErpCsTicketBizModel`/`ErpCsEntitlementBizModel`/`ErpCsTicketScanOverdueTicketsProcessor`/`ErpCsTicketResolveProcessor`/`ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor`/`ErpCsEntitlementExpiryJob` 均注入 `IErpSysNotificationBiz`，`ErpCsConstants:159` 定义 `NOTIFY_EVENT_TICKET_CREATED = "cs.ticket-created"`），模板种子同步存在。**根因是「种子未聚合进 app 装配」而非「功能未实现」**——owner doc 记录了错误的生产真相，会误导后续修复方向。
  4. 无路由复核：arm-index 316 findings 无此控制点（本轮 grep：notify 命中均为 MA1/MA2 历史切片行，非种子聚合控制点）；`docs/bugs/` 最新 8 条无记录；seed-data.md §快照重录义务（:67-91 本轮确认在位）把种子资产口径固化为恰「94 CSV + 1 SQL」，反而把排除固化为契约。
- **影响面**：聚合 app（产品交付物）演示/E2E/fresh-DB 部署下全部模板驱动通知静默丢弃 + CS 工单 TK 编号规则缺失回退（C16 在 it 套件可过单证示 ticket save 对规则缺失有回退路径，非硬失败）；跨模块 deploy 种子 ↔ 聚合 app 种子的同步义务完全未定义。
- **路由状态**：`docs/plans/2026-08-25-0330-1-...md` active（草案审查 iteration 1 acceptable，task `ses_fca9b2d19ffepEINL5iVLkqTQd`，含四档映射/overdue 实证/断言污染修正等非阻塞注记采纳记录），覆盖聚合落地 + :458 根因修正 + seed-data.md 同步义务立法 + 双面快照重录义务。**执行前缺陷持续存在**。

### [P1] OA-02 closePeriod 同事务 FX 凭证未 flush → 损益结转凭证永久缺 FX 腿：两套件钉死**相反**业务语义，已被黄金路径回归套件按现行为看护（首轮发现，本轮代码级复核确认，路由 0330-2 active 未执行）
- **一行理由**：同一业务操作（期间损益结转是否包含汇兑损益）在 fin-service 单测与 app-erp-all 集成测各被一枚全绿测试断言为相反结果（含 FX vs 不含 FX），机制根因是 flush 时序缺陷，会计保护区域（损益结转正确性）缺陷被回归套件反向看护。
- **本轮复核证据链**：
  1. 机制代码级实证：`ErpFinAccountingPeriodProcessor.closeGlModule`（:164-173）序列 = `exchangeRevaluationService.revalue()` → **紧接** `profitLossClosingService.close()`（DB 直查聚合）；FX 凭证经 `CloseVoucherWriter.writeVoucher`（`voucherDao.saveEntity`/`lineDao.saveEntity`/`billRDao.saveEntity`，CloseVoucherWriter.java:104/128/136）直接 save **无 flush**；编排末尾 `facade.orm().flushSession()`（ErpFinAccountingPeriodClosePeriodProcessor.java:95）在聚合**之后**，来得太晚。折旧（AST 模块）产物可见性同型待复核（0330-2 已纳入 Goals）。
  2. 语义冲突实证：`module-finance/.../TestErpFinProfitLossClosing.java:109-143` `testProfitLossClosingIncludesFxGainLoss` 断言本年利润净额 = 收入 100 + 汇兑收益 50 = **150（含 FX）**；`app-erp-all/.../TestErpC13FinPeriodCloseReverse.java:118` `PL_TOTAL=1130` 断言 PERIOD_CLOSE **不含** FX 腿（含腿应为 1280 = 5001 收入 1130 + 6603 汇兑 150）。两测试对等价业务场景钉死不同结转总额。
  3. 持久性推演：反结账→重结账路径中红字凭证被 isReversed 过滤（正确）、新 FX 凭证同样同事务未 flush → **任何 closePeriod 路径下都结不出 FX 腿**（非仅首次）。
  4. 修复阻力实证：`integration-testing.md:409` 勘误(2) 以「行为差异以实仓为准」收档；C13 快照 tables 层 1130 金额全字面值（本轮抽查）——正确修复落地时 C13 及其快照必红。
  5. 无路由复核：arm-index fin period-close 相关行无 flush 时序控制点；`docs/bugs/` 无记录。
- **路由状态**：`docs/plans/2026-08-25-0330-2-...md` active（草案审查 iteration 1 acceptable，task `ses_fca9b087bffehkZGj0ohbdtTql`，机制断言 6 调用方清单逐核），覆盖聚合前 flush 边界 + C13 断言/快照翻转（1130→1280）+ bugs/arm-index 登记 + owner doc 显式裁决 + 会计保护区域双独立子代理批准。**执行前缺陷持续存在**。

### [P1] OA-03 ASN→收货链从不写 orgId → 账套解析 null → 过账零凭证 posted 永久悬挂（已知失败模式 #11 家族「缺 writer」新站点）：C19 以 DAO fixture 掩蔽后未路由（首轮发现，本轮复核确认，路由 0330-3 active 未执行）
- **一行理由**：`ErpB2bAsnCreateReceiveFromAsnProcessor` 全文件零 orgId 写入（本轮 grep 复核零命中），B9 勘误自证「orgId=null 时账套解析为 null → 过账零凭证 posted 悬挂 false」——lesson 09 / 已知失败模式 #11（业财过账悬挂）家族、R1.16 十二站点清扫未覆盖的「缺 writer」新控制点，测试用 fixture 补 orgId 后该生产悬挂对黄金路径套件**不可见**，零路由。
- **本轮复核证据链**：
  1. `module-b2b/erp-b2b-service/.../processor/ErpB2bAsnCreateReceiveFromAsnProcessor.java`：`rg orgId` 零命中（同链 PO 有 orgId 可透传，缺的是 writer）。
  2. `integration-testing.md:507` 勘误(2)：「ASN 创建链不落收货头 orgId（exchangeRate 由列 defaultValue=1 兜底；orgId=null 时账套解析为 null → 过账零凭证 posted 悬挂 false）——用例以 DAO fixture 补全 orgId」。
  3. 遮蔽实证：`TestErpC19B2bAsnAutoReceiveLandedCost.java:40/:57/:167` 三处注释如实记录该生产缺口，但**无 `workaround-for` 语义标注**（grep 零命中）——fixture 使 B2B 旗舰路径（ASN 自动收货）在生产 webhook/save 真实上下文下的过账悬挂对回归套件不可见，注释与遮蔽并存反而固化「以 fixture 为准」。
  4. 未裁决未知数：生产 GraphQL/webhook 上下文若平台自动回填 orgId 则可能仅部分上下文悬挂——该条件从未被裁决或文档化；「缺 writer + 测试遮蔽 + 无路由」本身构成必须路由的契约缺口。
  5. 无路由复核：arm-index 无 ASN orgId/账套解析悬挂 finding（本轮 grep：ASN/orgId 命中均为 A2.14 状态机 / A2.18 多账套历史切片行）；`docs/bugs/` 无记录；R1.16（P1-MA2-074 家族）清扫的是 catch 吞异常站点，非缺 writer 站点。
- **同型未路由项（供 0330-3 批量分诊，已在计划 Current Baseline 列举）**：C18 勘误(4)（integration-testing.md:492，本轮复核原文在位）——返利 `postSettlement` 产物为 DRAFT 负额 credit memo、**非已过账凭证、无红字反向分录**；C20b 勘误(3)（:541 区域）——仿真覆盖不写回参数表 → 提升后重算回落基线参数。均属执行期发现的实现-设计实质差距，仅勘误落盘未路由。
- **路由状态**：`docs/plans/2026-08-25-0330-3-...md` active（含两分支裁决设计 + C18/C20b 批量分诊 + 勘误↔缺陷路由通道三绑定义务立法 + arm-index 反向回填）。**执行前缺陷持续存在**。

### [P2] OA-07（本轮新增）integration-testing.md §6 C20a 层 1 断言行保留「−已分配」公式，与同文档勘误块**和 DRP owner doc 双重相反**
- **一行理由**：`docs/design/integration-testing.md:536` 层 1 断言仍写「净需求 = 毛需求 − 在途 − 在手 − **− 已分配**」，而 10 行之上的勘误块(:526) 已更正为 `+ allocatedQty`，且 DRP owner doc `docs/design/drp/README.md:104` 本来就写 `netRequirement = max(0, safetyStock + forecastDemand - currentStock + allocatedQty - onOrderQty)`——即设计时从 owner doc 派生该行时**抄错符号**，非实现漂移；owner doc 与实仓 DrpEngine 口径一致，无生产缺陷，纯设计文档内部矛盾。
- **与 P2-IT-01 的区别**：P2-IT-01 只覆盖 :191「C01-C22」标签；本行为公式级矛盾且矛盾方向指向 owner doc（会误导后续用例维护者以为实仓/owner doc 错了）。
- **处置建议**：并入 Follow-up Backlog（触发条件：下次触碰该文档时顺手修正——P2-IT-01 同批）。

### [P2] OA-04（复核确认，仍未修复，backlog 在盘）524 个 `input/tables` CSV 为机制 (c) 下的惰性载荷——编辑无生效的静默陷阱，case 目录内无就地提示
- 基类 `ErpIntegrationTestCase.configExecutionMode` 双模强制 `setTableInit(false)`（本轮源码通读复核）→ 平台 `AutoTestCaseDataBaseInitializer.loadInputData()` 永不执行，22 用例目录中 524 个 input CSV（本轮 count 复核）既不被装载也不参与比对，维护者编辑其改变前置态会得到静默 no-op；机制披露仅在 `integration-testing.md` §3.4，case 目录内（autotest.yaml/README）零就地标记。纯维护性陷阱与资产膨胀，无行为错误，不升级。

### [P2] OA-05（复核确认，仍未修复，backlog 在盘）6 个冻结时钟扩展为逐批复制的同构克隆 + 基类 `findBillLink(String)` 全表扫描
- `B9/B10/C07/C11C12/C13C14/C15C16C17FrozenClockExtension` 六类（本轮 count=6 复核）方法体逐行同构、REFERENCE_DATE 全等 2026-07-17，可参数化为单一扩展（`AbstractFrozenClockExtension` 基座已在 common-test）；基类 `ErpIntegrationTestCase.findBillLink(String)` 以 `findAllByQuery(new QueryBean())` 全表加载后内存过滤（本轮源码复核，对照 C13 本地覆写用带过滤查询）。测试代码整洁度 polish，无行为风险，不升级。

### [P2] OA-06（复核确认，仍未修复，backlog 在盘）roadmap「当前基线」节保留 mission 开工前缺口描述，与已收口状态自相矛盾
- `docs/backlog/integration-test-roadmap.md`「当前基线」节仍记「缺口：app-erp-all 模块 11 个测试全部为 auth/web/meta 基建类，**零业务集成测试**」——与头部收口注记（54/0/0/1、23 集成类，本轮均复核在位）字面矛盾；历史语境可保留但应标注时点。纯叙事层漂移，不升级。

### [P2] P2-IT-01（复核确认，仍未修复，backlog 在盘）设计文档 §6 用例编号标签「C01-C22」失实
- `integration-testing.md:191` 写「C01-C22」，全文无 C22 用例（本轮 grep 复核仅命中标签自身）；实集 = C01-C21（含 C20a/C20b）= 22 用例；计数语义（Σ=88、19/19 域 ≥2）无一处受影响。

### [P2] P2-IT-02（复核确认，仍未修复，backlog 在盘）e2e-runbook「集成测试」运行方式注释计数陈旧
- `docs/testing/e2e-runbook.md:1013` 注释「全量（含既有 12 基建类…）」；本轮实数非 it 测试类 = **14**（`TestModuleMetaReader`…`ErpFluxDebugInvPickerTest` 等，flux 会话计划落入）；权威计数显式指向 known-good-baselines V.1 行（本轮确认 V.1 行在盘且 54/0/0/1 与活跑一致），不构成双真相源，仅注释性漂移。

### [P2] P2-IT-03（复核确认，历史事实不可改写，backlog 在盘）mission 命名空间混装
- 用户直发兄弟计划（0900-1/1147-1/2115-1）以 `integration-test` 提交前缀落地，非 roadmap 工作项；各计划自身有独立 plan-audit/closure 与用户来源声明（非无声扩权），登记性/命名一致性问题，下一 mission 启动时立约。

### [P2] P2-IT-04（复核确认，watch-only，backlog 在盘）V.1「E2E 零回归」为构造性满足，Playwright 全量套件未在收口时点整体实跑
- V.1 以 27 提交逐提交 `git show --name-only` 证明零生产零 seed（构造性边界，经 V.1 独立结束审计 PASS）；窗口内他计划确有前端生产面变更，其 E2E 验证义务委托各计划闭包；登记性残余风险，触发条件在盘。

---

## 反窄化自检

本审计未局限于任一计划的内部声明核实：对象覆盖 roadmap 全工作项 + 窗口内提交的代码面 + 跨真相源（arm-index 316 findings / seed-data.md / deploy SQL / 模块生产代码 / DRP owner doc）交叉比对。P1 均为**跨工件、跨 mission 才可见**的形态（种子聚合缺口、双套件语义冲突、悬挂家族新站点）——非任一计划内部核数可得出。本轮新增的 OA-07 来自「集成测试设计文档 ↔ 域 owner doc」的直接交叉比对（前两轮审计均未触达 drp/README.md 公式原文）。已知失败模式加权：#9（compliance 漂移）本轮活跑逐项相等 ✓；#10（closure-pending）roadmap 全部计划闭包证据在案（V.1/V.2/B5 抽验 + 三 0330 计划为 active 非声称完成，无 closure-pending 形态）✓；#11（过账悬挂）→ OA-03 即其「缺 writer」新控制点 ✓；#12（dict 死状态）22 用例实走状态机含负路径 ✓；#13（arm-index 回填）本 mission 无 finding 所有权，N/A——但 OA-01/02/03 表明「**发现于集成测试、应回填 arm-index/backlog 的缺陷**」反向回填通道缺失，属 #13 的对偶形态，已由 0330-3 立法承接。

## 结论

**needs revision**（0 P0 / 3 P1 / 8 P2）。需要修订的对象不是 mission 的测试交付物（质量与验证纪律均过硬，本轮独立活体复现 54/0/0/1 @01:37 + compliance 零漂移），而是：

1. **三份 active remediation 计划的执行**（OA-01/02/03）——执行前生产缺陷持续存在且被黄金路径套件按现状看护（C13 `PL_TOTAL=1130`、C19 fixture 补 orgId、C16 自包含模板绕开模板族）；修复落地时须按计划内义务同步翻转断言与快照并履行双面快照重录义务（seed-data.md §快照重录义务）。
2. **`docs/design/integration-testing.md`** —— :458 C16 勘误根因误判修正（0330-1 已覆盖）+ :536 C20a 层 1 公式符号修正 + :191 编号标签（OA-07/P2-IT-01，下次触碰时顺手）。
3. **`docs/backlog/integration-test-roadmap.md` § Follow-up Backlog** —— 补登 OA-07 行（与既有 7 行同触发条件处置）。
4. **流程**——0330-3 的勘误↔缺陷路由三绑定义务立法落地后，「以当前实现为准」的收口才不再系统性把生产缺陷钉死为期望行为。

P2 项（OA-04/05/06/07 + P2-IT-01..04）入 follow-up backlog，不单独驱动修复计划。剩余值得警惕的未知数：Playwright 全量套件回收窗口（P2-IT-04）；`TestAuthSeedLoadingProof` 与 it 套件共享 `db/erp.mv.db` 的删除时序依赖 surefire 串行（forkCount=1/parallel=none，本轮 pom 口径未复验变化）与基类静态键控 `freshDbForClass`，若未来并行化须先解耦；工作树中双审计报告、三份 remediation 计划、roadmap Follow-up Backlog 节均为未提交变更——路由证据的 git 持久性依赖 mission-driver 步骤收尾提交，若本步骤提交仅含本审计文件需确认其余路由工件不遗失。

<AI_STEP_RESULT>issues</AI_STEP_RESULT>
