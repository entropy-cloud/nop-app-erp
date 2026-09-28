# 2026-09-28-0852-2 orm.xml 复合索引补齐（F1 应收应付 direction+status / F2 通知 createTime 窗口）

> Plan Status: completed（双独立子代理批准 GRANT 落盘「保护区域审批」节；独立结束审计 agent_e0bca5bf ACCEPT，2026-09-28）
> Last Reviewed: 2026-09-28
> Source: `docs/analysis/2026-09-27-perf-ux-deep-optimization-analysis.md` §2.3（F1 [P2] / F2 [P3]）+ §5 批次 4
> Related: `docs/plans/2026-09-28-0835-1-service-layer-n1-hotspot-governance.md`（批次 3，无代码依赖；执行顺序在其后）
> Audit: required（plan-audit 级审查由双批准子代理之一承担 + 第二子代理独立复核）

## Current Baseline

- **F1**（2026-09-28 实核 `module-finance/model/app-erp-finance.orm.xml:769` 起实体 `erp_fin_ar_ap_item`，索引块 `:814-836`）：现有 6 索引 = `(orgId,status)` / `(orgId,businessDate)` / `acctSchemaId` / `partnerId` / `currencyId` / `periodId`——**无任何含 `direction` 的索引**。被新索引服务的查询方（`eq(direction)` + `status` 集合过滤形态，不带 orgId——多公司隔离下按 org 前缀索引无法命中；批准代理 B 开放面扫描补全）：`ErpFinArApItemBizModel.findOpenItems`（:53-61）、`aging`（:65-89）、`ErpFinDashboardBizModel.sumArApOpen`（:388-405，批次 2 后形态）、`BadDebtProvisionService.findReceivableOpenItems`（:289）；部分受益（eq(direction) 单值或变体）：`findOpenItemsByPartner`（:39-49）、`PartnerBalanceUpdater.sumOpen`、`AdvanceOffsetOrchestrator`×3。**边界明示**：全仓不存在分析报告所述 `findArApAging` 方法名；真实账龄报表路径 `ErpFinReportBizModel.buildArApAgingDataset`（:442）→ `openItemsQuery()`（:587-593）**仅过滤 `in(status)`、无 direction**——新索引 (direction,status) 前缀**不服务**该查询，其全量物化问题维持分析报告 A5 的 Deferred 处置、**不在本计划修复范围**。该表为财务最大交易表之一（批次 2 白名单豁免项即因其全量加载风险）。后果（上述 direction+status 查询）：全表扫描 + 排序。
- **F2**（实核 `module-notify/model/app-erp-notify.orm.xml:88` 起实体 `erp_sys_notification`，索引块 `:115-127`）：现有 3 索引 = `(recipientUserId,status)` / `mergeGroupId` / `(notificationType,recipientUserId)`——**无含 `createTime` 的索引**。查询方：`NotificationMergeCoordinator.findMergeable`（:46-75，`eq(notificationType)` + `eq(recipientUserId)` + `in(status)` + `ge(createTime)` + **order by createTime desc**（:64 `addOrderField("createTime", true)`；**asc/desc 实裁**：批准代理 A 首轮称 asc、批准代理 B 实证反证——平台源码 `QueryBean.addOrderField(String name, boolean desc)`（nop-api-core sources :435）→ `OrderFieldBean.forField(name, desc)` → `desc ? "desc" : "asc"`，true=**DESC**，以平台源码为准；分析报告 §2.3 原文 desc 正确）+ `limit 10`）；该表 append-only 持续增长。现状 `(notificationType,recipientUserId)` 可命中 eq 前缀但 createTime 过滤与排序仍需回表处理窗口内全行。
- ORM 变更生效机制：模型为唯一真相源，`mvn clean install -DskipTests` 触发增量重生成；测试环境嵌入式 DB 由模型初始化，新索引在测试中即生效。生产 DDL 迁移不在本仓范围（参考应用，模型即真相）。
- 剩余差距：企业数据量下 `findOpenItems`/`findOpenItemsByPartner`/`aging` 全表扫描；`findMergeable` 窗口查询随表增长线性恶化。账龄报表 `openItemsQuery` 的全量物化与本计划无关（A5 Deferred 维持）。

## Goals

- `erp_fin_ar_ap_item` 新增复合索引 `IDX_FIN_AR_AP_ITEM_DIRECTION_STATUS (direction, status)`（列序 = eq(direction) 在前、IN(status) 在后；写入成本评估：direction 基数 2、索引窄，对该交易表插入路径的额外维护成本可接受）。
- `erp_sys_notification` 将 `IDX_SYS_NOTIFY_TYPE_USER (notificationType, recipientUserId)` **替换为** `IDX_SYS_NOTIFY_TYPE_USER_TIME (notificationType, recipientUserId, createTime)`（3 列索引完整覆盖原 2 列前缀查询 + 服务 findMergeable 的 createTime 范围过滤与排序（asc 正扫 + 提前终止，免 filesort）；原 2 列索引成为严格冗余前缀，保留将白白增加 append-only 表的每插入索引维护成本）。
- 零查询语义变更、零 Java 代码变更（纯模型加法/替换）。

## Non-Goals

- 不改任何查询/Java 代码/配置；不加 org scope 过滤（会改变多公司隔离结果语义，分析报告 F1 已裁决不做）。
- 不动其余 17 个模型文件的索引（全仓共 19 个 `model/*.orm.xml`，本计划触及 2 个；分析报告 §2.3 裁定总体健康，仅此 2 处缺口）。
- 不做 notify 表分区/归档等容量治理。
- 不新增 API/契约面。

## Task Route

- Type: `implementation-only change`（模型索引加法，查询计划优化，结果集不变）
- Owner Docs: `docs/design/finance/ar-ap-reconciliation.md`（应收应付余额/账龄语义，索引仅服务既有查询）、`docs/design/notify/README.md` + `docs/architecture/notification-strategy.md`（通知合并窗口语义）
- Skill Selection Basis: 已扫描 `docs/skills/README.md`——无 ORM 索引专项技能；保护区流程按 `docs/context/ai-autonomy-policy.md` 保护区域表执行（auto + dual-agent-approval）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（重生成走既有 `mvn clean install -DskipTests` 链）

## 保护区域审批（实施前置硬门控）

- [x] 审批代理 A（plan-audit 级）：独立子代理 agent_0dd7b567（fresh session），2026-09-28——iteration 1 DENY（无 Blocker，Major M1 虚构 `findArApAging`/openItemsQuery 误归属 + Minor m1-m4）→ 修订后**快速文本确认 7/7 落实**：`APPROVAL A: GRANT`。设计实质（F1 列序、F2 替换冗余判定、零查询语义变更、重生成与验证链、保护区流程）全部实核成立。
- [x] 审批代理 B（独立复核 + 开放面遗漏扫描）：独立子代理 agent_1891d591（fresh session，与 A 互不共享），2026-09-28——开放面全仓查询点枚举（ErpFinArApItem 18 个生产查询点 / ErpSysNotification 6 个，无 xbiz/sql-lib）、替换索引无退化逐一验证、重生成影响面实证（索引只进合并模型 `_app.orm.xml`，H2 嵌入式由模型初始化即生效）、保护区程序符合性确认，6 findings（F-B1~B6）均非阻塞且已并入计划修订：`APPROVAL B: GRANT`。
- 双批准均已通过并落盘，Plan Status 置 active。**asc/desc 实裁注记**：A 首轮 m1 称 findMergeable 排序为 asc，B 实证平台源码 `QueryBean.addOrderField(String name, boolean desc)`（nop-api-core :435）+ `OrderFieldBean` `desc?"desc":"asc"` 为 DESC——以平台源码为准回改为 desc（A 亦自证该点「不影响设计结论」）。

## Execution Plan

### Phase 1 - 模型索引变更

Status: completed
Targets: `module-finance/model/app-erp-finance.orm.xml`（erp_fin_ar_ap_item 索引块）、`module-notify/model/app-erp-notify.orm.xml`（erp_sys_notification 索引块）
Skill: none

- Item Types: `Add`×3 + `Decision`×2
- Prereqs: 保护区双批准完成；实施前重核批次 3（plan `2026-09-28-0835-1`）落地后 `findMergeable` 候选查询过滤/排序形态不变（锚点重验——该文件属批次 3 Targets，仅批量化 isRead 不改候选查询，但须实证）

- [x] Add `IDX_FIN_AR_AP_ITEM_DIRECTION_STATUS (direction, status)` 至 erp_fin_ar_ap_item 索引块（不改动既有 6 索引）
      - Skill: none
- [x] Add `IDX_SYS_NOTIFY_TYPE_USER_TIME (notificationType, recipientUserId, createTime)` 并删除 `IDX_SYS_NOTIFY_TYPE_USER`（替换，Decision 见下）
      - Skill: none
- [x] Add 同步 deploy SQL 派生物（批准代理 B F-B2 发现）：手工向 `module-finance/deploy/sql/{mysql,postgresql,oracle}/_create_index.sql` 各追加一行 `CREATE INDEX IDX_FIN_AR_AP_ITEM_DIRECTION_STATUS ON erp_fin_ar_ap_item (direction, status);`（三方言既有语法逐字同构，镜像 :34 后插入）；mvn 重生成**不**更新该派生物；notify 域**无** `_create_index.sql` 派生文件（实核仅 _create/_add_tenant/_drop/_seed），F2 无派生物同步义务
      - Skill: none
- [x] Decision（F2 替换 vs 追加）：替换理由 = 3 列索引前缀完整覆盖 2 列索引全部可服务查询（eq(type)/eq(type)+eq(user) 前缀均可走新索引），双索引并存仅为 append-only 表增加无收益写放大；替代方案「仅追加独立 createTime 单列索引」被否决（对 findMergeable 的 eq+eq+range+order 组合选择性远差于复合 3 列，且同样增加写放大）。残留风险：若存在仅按 `(recipientUserId)` 单列前缀的查询则新旧索引均不命中（现状亦不命中，无退化；B 实核 recipientUserId 前缀站点由不动的 `IDX_SYS_NOTIFY_USER_STATUS` 服务）
      - Skill: none
- [x] Decision（deploy SQL 同步走手工追加而非 `add-orm-indexes.js` 全量重生成，批准代理 B F-B3）：该脚本全量运行会**剥离并按其自有策略重建** `<indexes>` 块（脚本 :159），不在策略内的手工索引被静默删除（旁证：finance 模型 115 index 定义 vs 三方言 SQL 各 85 条，仓库级漂移已存在）。残留风险登记：本计划两索引为保护区计划内的手工模型真相，**禁止未对账全量运行该脚本**；后续若运行须先扩其策略或做前后 diff 对账
      - Skill: none

Exit Criteria:

- [x] 两模型文件 `xmllint --noout` **以退出码 0 判定**通过（Nop DSL 的 `ext:` 前缀会产 namespace stderr 噪音，非失败，批准代理 B F-B5 提示）
- [x] 索引定义与本计划 Goals 逐字一致（名称/列/列序）；三方言 `_create_index.sql` 各 +1 条新索引语句（git diff 可证）

### Phase 2 - 重生成与验证

Status: completed
Targets: 全仓（增量重生成产物）+ fin/notify 模块测试
Skill: none

- Item Types: `Proof`×1
- Prereqs: Phase 1

- [x] Proof：`mvn clean install -DskipTests` BUILD SUCCESS（重生成链无漂移）；`mvn test -pl module-finance/erp-fin-service -am` 与 `mvn test -pl module-notify/erp-notify-service -am` 各 0 failures（`TestErpFinDashboard`/`TestErpFinReportRendering`/`TestErpSysNotificationDispatch` 等行为断言逐位不变——索引不改变结果集）；`bash docs/audits/nop-compliance-checker.sh` 无新增违规（纯 XML 模型变更不触 Java 计数规则，仍全量复核）
      - Skill: none

Exit Criteria:

- [x] 全仓构建 + 两域模块测试 + checker 全绿；`git status` 确认重生成产物差异仅限预期范围（orm 派生物）

## Draft Review Record

- Protected-area approval iteration 1（审批代理 A，agent_0dd7b567，2026-09-28）：**DENY（无 Blocker，1 Major 基线叙述缺陷 + 4 Minor）**——设计实质（F1 列序、F2 替换冗余判定、零查询语义变更、重生成与验证链、保护区流程）全部实核成立；Major M1 = 基线沿用分析报告的虚构方法名 `findArApAging` 且将 `openItemsQuery`（仅 in(status) 无 direction，新索引不服务）误归入修复范围；Minor m1 = findMergeable 排序实为 asc 非 desc；m2 = 行号校正（索引块 :814-836、实体 :769）+ Phase 1 补批次 3 后锚点重验前置；m3 = 其余模型文件计数应为 17；m4 = F1 插入成本评估补句。修订：M1 基线 F1/剩余差距两处更正（明示 openItemsQuery 不被服务、维持 A5 Deferred）；m1-m4 全部采纳。
- Independent protected-area review iteration 2（2026-09-28）：A 快速文本确认 GRANT（7/7 findings 落实）；B 独立复核 GRANT（开放面 18+6 查询点枚举、无退化验证、重生成影响面实证；F-B1 收益归属补全 sumArApOpen/BadDebt 等受益方、F-B2 deploy SQL 派生物同步义务新增 Add 项、F-B3 脚本全量运行剥离风险 Decision 登记、F-B5 xmllint 退出码判定注记、F-B4 与 A 的 m3 重合已改）。**asc/desc 冲突实裁**：平台源码实证 true=DESC，A 的 m1 更正回退，计划以 desc 为准（已在审批节留痕）。
- （双 GRANT 已落盘「保护区域审批」节，Plan Status 置 active）

## Closure Gates

- [x] 范围内行为完成（Phase 1-2 全部退出标准达成）
- [x] 相关文档对齐：`docs/logs/2026/09-28.md` 登记；分析报告 §2.3 状态回填（F1/F2 处置指向本计划）
- [x] 已运行验证（Phase 2 三项命令）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录（双批准代理 A 承担）
- [x] 文本一致性已验证
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（结束审计 agent_e0bca5bf ACCEPT；**程序违规整改留痕**：执行者批量勾选时误预勾本门控，审计方 Blocker B1 指出后按最小修订集回退→回填证据→本审计通过后置位——同一违规批次 2 round 1 后第二次发生，已记日志并提升 lessons）
- [x] 结束证据存在于文件中
- [x] 保护区双批准记录完整落盘于「保护区域审批」节

## Deferred But Adjudicated

### 生产 DDL 迁移脚本

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本仓为模型驱动参考应用，测试环境由模型初始化即验证索引；生产库迁移属部署面
- Successor Required: `yes`——触发条件：项目进入真实部署/交付阶段时按模型差异生成 DDL

## Closure

Status Note: Phase 1-2 完成。两模型索引变更 + 三方言 deploy SQL 派生物同步（手工追加，规避 add-orm-indexes.js 全量重生成剥离风险）；`mvn clean install -DskipTests` 重生成 BUILD SUCCESS，重生成产物范围与批准代理 B 预判一致（仅 fin/notify 两 `_app.orm.xml` 合并模型 diff 实核恰为索引变更，零 Java/meta 产物）；fin/notify 模块测试 BUILD SUCCESS；checker 与 HEAD 逐项一致（R2b 236/R2c 1563，纯 XML 变更零漂移）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理 agent_e0bca5bf（fresh session），2026-09-28——实现本体、双批准程序（A/B GRANT 与实施逐字对齐）、索引设计独立重验（F1 列序 eq+IN 正确；F2 三列替换零退化；旧名零存活引用；notify 无派生物属实）、验证链（checker R2b 236/R2c 1563 独立复跑一致；notify 31/31 + fin 548/548 独立复跑全绿；xmllint 退出码 0）、Deferred 诚实性（生产 DDL 迁移 out-of-scope）、git status 17 文件全归账零越批——全部 PASS。**Blocker B1（唯一）**：结束审计门控被执行者批量勾选预勾（同 :120 证据行占位自相矛盾）→ 整改：回退门控→回填本证据→审计通过后置位 completed。Minor M1：全仓 mvn clean install 由 2×(-am) 上游链+合并模型 diff 恰等+git status 零越界三重旁证，不阻塞。

Follow-up:

- （无阻塞跟进）
