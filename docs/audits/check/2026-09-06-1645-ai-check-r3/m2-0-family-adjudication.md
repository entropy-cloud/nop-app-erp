# M2.0 同型 finding 基类化裁决（r3 轮 93 条全量族路由四分）

> 轮内产物（plan `2026-09-10-0425-1-m20-fix-method-baseline-family-adjudication.md` Phase 2 交付）；**M2.8 执行者唯一消费入口**（沿 m0-4 矩阵先例）。
> 枚举源：`docs/audits/check/ai-check-index.md` §Finding 追踪执行时点实值（2026-09-10，`rg '^.*P[0-3]-CK-[a-z0-9]+-[0-9]+-r3 \|'` 机械提取 93 行；P0=0 / P1=3 / P2=17 / P3=73，与 plan Current Baseline 及 M1.17 收官核账一致）。逐 ID 复核索引行现值后归类，未凭记忆补 ID（lesson 13）。
> 修复方法论与族回填范式权威：`docs/architecture/finding-remediation-method.md`（M2.0 owner doc，本文引用其范式节不复制）。
> 边界：本文只做路由裁决，不修复任何 finding（M2.1~M2.8 义务）、不改两份索引任何行状态（M2.9 义务）、不预写 M2.8 计划。

## §1 族清单冻结（93 → 8 类去向全量映射）

| # | Finding ID | 级别 | 族 | 去向 |
| --- | --- | --- | --- | --- |
| 1 | P3-CK-common-014-r3 | P3 | A daoFor 豁免注释族（聚合追踪点 U20，354 文件 312 无注释） | M2.8 族批·分片 2 |
| 2 | P3-CK-fin3-017-r3 | P3 | A（fin-3 族 7 文件，移交槽位） | M2.8 族批·分片 2（fin 片） |
| 3 | P3-CK-fin4-022-r3 | P3 | A（fin-4 族 13 文件，移交槽位） | M2.8 族批·分片 2（fin 片） |
| 4 | P3-CK-ast-028-r3 | P3 | A（ast ActionLog 直查，移交槽位） | M2.8 族批·分片 2（ast 片） |
| 5 | P2-CK-qa-027-r3 | P2 | A（qa 跨域三站点，移交槽位） | M2.8 族批·分片 2（qa 片） |
| 6 | P3-CK-qa-028-r3 | P3 | A（qa SPC I*Biz 声明未用簇，②亚型） | M2.8 族批·分片 2（qa 片） |
| 7 | P3-CK-mfg2-026-r3 | P3 | A（mfg-2 VersionComparator 直读） | M2.8 族批·分片 2（mfg 片） |
| 8 | P3-CK-mfg3-019-r3 | P3 | A（mfg-3 族 3 文件 4 站点） | M2.8 族批·分片 2（mfg 片） |
| 9 | P3-CK-hr2-029-r3 | P3 | A（hr payroll 引擎 3 文件 10 站点） | M2.8 族批·分片 2（hr 片） |
| 10 | P3-CK-crm-021-r3 | P3 | A（crm 20 文件 22 处） | M2.8 族批·分片 2（crm 片） |
| 11 | P2-CK-ct-027-r3 | P2 | A（ct 5 文件豁免注释） | M2.8 族批·分片 2（ct 片） |
| 12 | P3-CK-cs-023-r3 | P3 | A（cs 29 注入 1 注释） | M2.8 族批·分片 2（cs 片） |
| 13 | P3-CK-drp-024-r3 | P3 | A（drp StagingTimeoutJob 3 站点） | M2.8 族批·分片 2（drp 片） |
| 14 | P3-CK-log-014-r3 | P3 | A（log DeliveredProcessor 1 文件） | M2.8 族批·分片 2（log 片） |
| 15 | P3-CK-fin4-023-r3 | P3 | B FNPT 注册族（fin 13 mutation） | M2.8 族批·分片 1（fin 片） |
| 16 | P3-CK-mfg2-024-r3 | P3 | B（mfg-2 8 mutation） | M2.8 族批·分片 1（mfg 片） |
| 17 | P3-CK-mfg3-020-r3 | P3 | B（mfg-3 8 mutation） | M2.8 族批·分片 1（mfg 片） |
| 18 | P3-CK-hr2-028-r3 | P3 | B（hr ≈42 mutation） | M2.8 族批·分片 1（hr 片） |
| 19 | P3-CK-prj-022-r3 | P3 | B（prj 4 实体零 auth） | M2.8 族批·分片 1（prj 片） |
| 20 | P3-CK-qa-029-r3 | P3 | B（qa NCR xbiz 空 actions） | M2.8 族批·分片 1（qa 片） |
| 21 | P3-CK-ct-030-r3 | P3 | B（ct 12 mutation） | M2.8 族批·分片 1（ct 片） |
| 22 | P3-CK-mnt-023-r3 | P3 | B（mnt 16 mutation） | M2.8 族批·分片 1（mnt 片） |
| 23 | P3-CK-aps-015-r3 | P3 | B（aps 18 mutation + 3 query） | M2.8 族批·分片 1（aps 片） |
| 24 | P3-CK-log-015-r3 | P3 | B（log 10 mutation） | M2.8 族批·分片 1（log 片） |
| 25 | P3-CK-md-020-r3 | P3 | B（md 8 mutation） | M2.8 族批·分片 1（md 片） |
| 26 | P3-CK-app-001-r3 | P3 | B 亚型 enforcement 前置阻塞面（八扩展域保留层零 x:extends） | M2.8 族批·分片 1（app 聚合片） |
| 27 | P3-CK-common-015-r3 | P3 | C ctx 兜底族（聚合追踪点，82 站点/61 文件，44 裸 new） | M2.8 族批·分片 4 |
| 28 | P3-CK-prj-023-r3 | P3 | C（exemplar 移交槽位，ExpenseCostAggregator 镜像范式） | M2.8 族批·分片 4 |
| 29 | P2-CK-fin2-018-r3 | P2 | D DIM-T 覆盖缺口族（核销 FX 路径零测试） | M2.8 族批·分片 3 |
| 30 | P3-CK-fin3-019-r3 | P3 | D（F2.3 修复体零专属回归） | M2.8 族批·分片 3 |
| 31 | P3-CK-mfg2-027-r3 | P3 | D（findBomTree 零测试） | M2.8 族批·分片 3 |
| 32 | P3-CK-mfg2-028-r3 | P3 | D（快照 @var 精度 flaky，测试卫生亚型） | M2.8 族批·分片 3 |
| 33 | P3-CK-hr2-030-r3 | P3 | D（payroll 汇总/个税累计零测试） | M2.8 族批·分片 3 |
| 34 | P3-CK-mnt-020-r3 | P3 | D（两报表数据集零测试） | M2.8 族批·分片 3 |
| 35 | P3-CK-mnt-022-r3 | P3 | D（孤儿 _cases 目录，测试卫生亚型） | M2.8 族批·分片 3 |
| 36 | P3-CK-aps-016-r3 | P3 | D（4 公开动作零测试） | M2.8 族批·分片 3 |
| 37 | P3-CK-md-019-r3 | P3 | D（4 公开动作零测试） | M2.8 族批·分片 3 |
| 38 | P2-CK-ast2-026-r3 | P2 | D（ReversalListener 引擎通道零测试） | M2.8 族批·分片 3（与 ast2-024-r3 修复同批联动） |
| 39 | P3-CK-notify-011-r3 | P3 | D（findRead 零测试；DIM-F+DIM-T 双面同控制点，含 inbox tab 前端半面） | M2.8 族批·分片 3 |
| 40 | P3-CK-pur-017-r3 | P3 | E DIM-F 死状态样式/孪生族（three-way-match 3 分支） | M2.8 族批·分片 4 |
| 41 | P3-CK-md-016-r3 | P3 | E（SupplierApproval view ACTIVE 死样式，pur-017 族 md 站点） | M2.8 族批·分片 4 |
| 42 | P3-CK-mfg-023-r3 | P3 | E（孪生遮蔽 page.yaml 参数错配） | M2.8 族批·分片 4 |
| 43 | P3-CK-cs-026-r3 | P3 | E（kanban 拖拽路由 CANCELLED/NEW 缺分支） | M2.8 族批·分片 4 |
| 44 | P3-CK-cs-027-r3 | P3 | E（E2E selector 纪律偏离，测试卫生亚型） | M2.8 族批·分片 4 |
| 45 | P2-CK-common-011-r3 | P2 | F 基类契约族（save 通道 defaultPrepareSave 零覆盖，F1.3 最后盲区） | M2.8 族批·分片 4 |
| 46 | P3-CK-common-013-r3 | P3 | F（SubmitForApproval 复合期望态串，~120 子类单点修复） | M2.8 族批·分片 4 |
| 47 | P3-CK-hr2-027-r3 | P3 | DOC owner-doc 漂移族（state-machine.md §五） | doc 批（M2.8 内独立分片 5） |
| 48 | P3-CK-crm-023-r3 | P3 | DOC（lead-scoring.md 声明落后） | doc 批·分片 5 |
| 49 | P3-CK-ct-028-r3 | P3 | DOC（approval-workflow.md requireSignOff） | doc 批·分片 5 |
| 50 | P3-CK-ct-029-r3 | P3 | DOC（e-signature.md MOCK 生产 dict） | doc 批·分片 5 |
| 51 | P3-CK-b2b-019-r3 | P3 | DOC（partner-onboarding.md webhookSecret） | doc 批·分片 5 |
| 52 | P3-CK-ast2-025-r3 | P3 | DOC（depreciation-and-posting.md 三站点） | doc 批·分片 5 |
| 53 | P3-CK-app-002-r3 | P3 | DOC（flux doc + e2e-runbook 2 doc 3 站点） | doc 批·分片 5 |
| 54 | P3-CK-md-017-r3 | P3 | DOC（exchange-rate-management.md 结构性漂移） | doc 批·分片 5 |
| 55 | P3-CK-md-018-r3 | P3 | DOC（3 doc 死配置键承诺；三键落地或 doc 降级裁决随批） | doc 批·分片 5 |
| 56 | P3-CK-fin3-018-r3 | P3 | DOC（budget.md L257 stale） | doc 批·分片 5 |
| 57 | P3-CK-mfg2-025-r3 | P3 | DOC（mrp.md L95/L98） | doc 批·分片 5 |
| 58 | P3-CK-prj-025-r3 | P3 | DOC（state-machine.md Billing 桩断言过期） | doc 批·分片 5 |
| 59 | P3-CK-mnt-021-r3 | P3 | DOC（state-machine.md remark 前缀字面量） | doc 批·分片 5 |
| 60 | P3-CK-qa-032-r3 | P3 | DOC（qa 漂移簇 7 站点） | doc 批·分片 5 |
| 61 | P3-CK-pur-016-r3 | P3 | DOC（Dashboard javadoc 口径措辞——Java 注释面零行为，随 doc 批同收口） | doc 批·分片 5 |
| 62 | P1-CK-ct-025-r3 | P1 | DICT dict 值域族（sign-status/sign-provider 双轨） | M2.7（ct 站点；ORM 保护区路由） |
| 63 | P3-CK-drp-022-r3 | P3 | DICT（replenishment-method 死值） | M2.7（drp 站点，与 ct-025 同批范式对齐） |
| 64 | P3-CK-b2b-018-r3 | P3 | DICT（mft-status 三值死） | M2.7（b2b 站点，同批） |
| 65 | P3-CK-prj-024-r3 | P3 | DICT（timesheet-status 死字典 + PENDING 死值；ORM 触及面走保护区） | M2.7（prj 站点，同批） |
| 66 | P1-CK-mfg-022-r3 | P1 | 域批（roadmap M2.3 行） | M2.3 |
| 67 | P1-CK-ct-026-r3 | P1 | 域批（roadmap M2.7 行；返利 ACTIVE 前置悖论） | M2.7 |
| 68 | P2-CK-aps-012-r3 | P2 | 域批独立项 | M2.7（aps） |
| 69 | P2-CK-ast2-024-r3 | P2 | 域批独立项 | M2.4（assets；与分片 3 ast2-026-r3 修复同批联动） |
| 70 | P2-CK-b2b-017-r3 | P2 | 域批独立项（跨域写裸 DAO 三选一裁决） | M2.7（b2b） |
| 71 | P2-CK-crm-020-r3 | P2 | 域批独立项（qualify 双前置） | M2.7（crm） |
| 72 | P2-CK-drp-021-r3 | P2 | 域批独立项（越库链入口三选一裁决） | M2.7（drp） |
| 73 | P2-CK-inv-012-r3 | P2 | 域批独立项（序列号 writer 缺失；与 P3-CK-inv-017 死字典联动） | M2.5（inv） |
| 74 | P2-CK-log-012-r3 | P2 | 域批独立项（追踪页串页；消费分片 4 孪生权威裁决） | M2.7（log） |
| 75 | P2-CK-md-015-r3 | P2 | 域批独立项（ORM defaultValue；保护区路由） | M2.7（md） |
| 76 | P2-CK-notify-009-r3 | P2 | 域批独立项（错误码接线；随 notify-004/005/003 修复批次） | M2.7（notify） |
| 77 | P2-CK-notify-010-r3 | P2 | 域批独立项（markAllRead 越权；与 notify-004 同批） | M2.7（notify） |
| 78 | P2-CK-pur-015-r3 | P2 | 域批独立项（核销 docStatus 守卫；与 r1 sal-010/pur-005 统一批次设计） | M2.5（pur） |
| 79 | P2-CK-qa-026-r3 | P2 | 域批独立项（severity 字典污染） | M2.6（qa） |
| 80 | P3-CK-common-012-r3 | P3 | 域批独立项（OrgIsolation fail-open；隔离开关投产前必须修——触发条件登记） | M2.7（common） |
| 81 | P3-CK-fin4-024-r3 | P3 | 域批独立项（intercompany 币种硬编码；config 投产升 P1 触发） | M2.2（fin） |
| 82 | P3-CK-aps-013-r3 | P3 | 域批独立项（batchScheduleForward 容错/一致性） | M2.7（aps） |
| 83 | P3-CK-aps-014-r3 | P3 | 域批独立项（约束 horizon 单界） | M2.7（aps） |
| 84 | P3-CK-crm-022-r3 | P3 | 域批独立项（ForecastPeriod OPEN 初始态不设防） | M2.7（crm） |
| 85 | P3-CK-crm-024-r3 | P3 | 域批独立项（kanban 前缀硬编码 + ¥ 无 i18nEn） | M2.7（crm） |
| 86 | P3-CK-cs-025-r3 | P3 | 域批独立项（TicketAssignResolver 降级零日志） | M2.7（cs） |
| 87 | P3-CK-drp-023-r3 | P3 | 域批独立项（DockAppointment.dock refEntityName 错挂；ORM 保护区路由） | M2.7（drp） |
| 88 | P3-CK-log-013-r3 | P3 | 域批独立项（book 在途预约白名单） | M2.7（log） |
| 89 | P3-CK-qa-030-r3 | P3 | 域批独立项（collectSamples per-item 容错） | M2.6（qa） |
| 90 | P3-CK-b2b-020-r3 | P3 | 逐条 deferred（防御分支裸 IAE 不可达护栏；索引明示不入修复队列优先级） | deferred（M2.9/MV.2 计数） |
| 91 | P3-CK-cs-024-r3 | P3 | 逐条 deferred（assertCan 裸 IAE 不可达护栏，同型 b2b-020） | deferred（M2.9/MV.2 计数） |
| 92 | P3-CK-qa-031-r3 | P3 | 逐条 deferred（疑似需求分歧只登记——需求裁决后改代码或 3 doc 同步） | deferred（M2.9/MV.2 计数） |
| 93 | P3-CK-qa-033-r3 | P3 | 逐条 deferred（疑似需求分歧只登记——检验模板类别级层级） | deferred（M2.9/MV.2 计数） |

**对账**：8 类去向计数 = 族批 46（A14 + B12 + C2 + D11 + E5 + F2）+ doc 批 15 + dict 族 4 + 域批 24（P1×2 + P2×12 + P3×10）+ deferred 4 = **93**（P1 3 / P2 17 / P3 73）。

## §2 逐族四路裁决（选择 / 替代方案 / 残余风险）

### §2.1 族 A — daoFor/IOrmTemplate 豁免注释与 I*Biz 注入族（14 ID）→ M2.8 族批

- **选择**：族批。修复范式 = 二选一逐站点落地——(a) 注入 `I*Biz` 改经接口读（首选，镜像 RecallTargetLocator/Cap countExecutedDepreciation 范式）；(b) 补一行豁免 javadoc（对齐 DrpEngine/CarryForwardProcessor/AutoReverseHelper 措辞先例）。按域分片认领（fin/ast/qa/mfg/hr/crm/ct/cs/drp/log 各 1 片），U20（common-014-r3）为聚合追踪点：移交槽位 finding（fin3-017/fin4-022/ast-028/qa-027）维持各自 ID 范围不动，由聚合点统一销账。
- **替代方案**：逐条留域批——被否，族为系统性「登记纪律缺失」而非 14 个独立访问路径问题（common-014-r3 裁决原文），域批逐条修复会产生 14 次重复范式决策与无对账的分片漂移。
- **残余风险**：豁免注释化可能被滥用为「免修通行证」——缓解：分片 2 验收口径含「跨域直查站点优先 I*Biz 注入，注释豁免仅限 session 语义依赖/只读聚合等显式理由」；每片 PR 注记逐站点选择。

### §2.2 族 B — FNPT 注册族 + enforcement 前置阻塞面（12 ID）→ M2.8 族批

- **选择**：族批。范式 = 保留层 `<domain>.action-auth.xml` 按 FNPT 既有范式（closePeriod/WorkOrder approve/Salary:markPaid 先例）逐域补自定义 mutation 注册，roles 对齐各域 useCases；11 个注册片 + app-001-r3 聚合片（八扩展域保留层补 `x:extends` 继承行，对齐核心域 11 域先例，先写 enforcement-on 权限点断言失败测试）。r1 同族站点（drp-018/b2b-011）**不入本批**（横切关注点 5，归 r1 通道），仅范式互链。
- **替代方案**：(a) 逐条域批——被否，同 §2.1；(b) 将「enable-action-auth 翻 true」设为族批完成门槛（config 投产前置）——**否决**（lesson 14 三源核对适用：enable-action-auth 缺省 false + seed 零 FNPT 授权行 + 无任何「默认开启」部署契约声明 → 缺省配置合法 opt-in；不得因 enforcement 前景把族修复硬契约化）。触发条件显式登记：**enable-action-auth 翻 true 前，族 B 12 ID 必须全部 fixed**（否则 enforcement 下全量业务动作 deny = 功能死锁面）。
- **残余风险**：roles 粒度裁决（哪些 mutation 给哪个角色）涉及权限语义，M2.8 计划起草时须逐域引 useCases 对账；app-001-r3 的 x:extends 继承会使生成层 22~109 资源/域入聚合链，enforcement-on 下权限面扩大——缓解：先写 enforcement-on 断言测试再合入。

### §2.3 族 C — ServiceContextImpl ctx 兜底族（2 ID）→ M2.8 族批

- **选择**：族批。范式 = 镜像 ExpenseCostAggregator:182-185 getCtx() 兜底（context 参数透传 + 兜底构造），job 入口逐站点豁免裁决（无调用方 context 可透传的调度入口登记豁免理由）。exemplar = prj-023-r3（rollbackFromTimesheet 五站点），common-015-r3（82 站点/61 文件，44 裸 new）为聚合追踪点。
- **替代方案**：只修 exemplar 不回填全族——被否，44 文件身份/数据权限丢弃是横切安全面，逐域留批会产生同型漂移；全量立即回填 82 站点——降级为分片内按域分批，避免单批规模失控。
- **残余风险**：job 入口豁免判定存在灰区（调度链中间层是否算入口）；缓解：豁免逐站点注记 + U20 聚合对账复核。

### §2.4 族 D — DIM-T 覆盖缺口/测试卫生族（11 ID）→ M2.8 族批

- **选择**：族批（测试批）。范式 = 每缺口 ≥1 行为断言测试（最低断言强度按 owner doc §五步流程第 2 步）；测试卫生亚型（mnt-022 孤儿 _cases、mfg2-028 @var flaky、cs-027 selector 纪律→实际归分片 4 E 亚型）按各自卫生程式（删孤儿目录/`*` 通配屏蔽或精度统一/selector adapter 化）。ast2-026-r3 与域批 ast2-024-r3 修复同批联动（测试断言依赖 024 修复后语义）。
- **替代方案**：逐条域批补测试——被否，11 个缺口同属「覆盖要求 §1.4 每公开方法/跨域场景 ≥1 测试」的系统性执行缺口，分散修复无统一覆盖对账。
- **残余风险**：并发/时序类断言（fin3-019 latch、mfg2-028 精度）自身可能引入新 flaky——缓解：优先快照与确定性断言，并发子路径允许「断言 + watch-only 登记」双轨（fin3-019 索引注记原文）。

### §2.5 族 E — DIM-F 死状态样式/孪生遮蔽/E2E 纪律族（5 ID）→ M2.8 族批

- **选择**：族批。范式两件：①死状态样式分支统一改按 `approveStatus`（或 `docStatus≠CANCELLED`）判定（pur-017 三分支 + md-016 一分支）；②孪生双载体权威裁决落 `view-and-page-strategy.md`：**flux.yaml 为唯一运行时权威，page.yaml 冻结为回退产物**——遮蔽文件缺陷（mfg-023）按裁决冻结不修或对齐参数（M2.8 执行者二选一并登记）；cs-026 补拖拽分支 + cs-027 selector adapter 化同片收口。
- **替代方案**：留域批（pur/ast 页面各域自修）——被否，孪生遮蔽与死状态样式是同构面（mfg-023/cs-027/log-012/md-016 跨 4 域同型），无统一权威裁决则每域重复裁决孪生归属。
- **残余风险**：「flux.yaml 唯一权威」裁决影响全部孪生页维护契约，须在 view-and-page-strategy.md 正式成文（M2.8 分片 4 义务之一），否则仅修页不立裁决会复发。
- **联动注记**：域批 P2-CK-log-012-r3（注册版串页 + 修复版死产物）消费本族孪生裁决（反向形态：page.yaml 为注册载体）。

### §2.6 族 F — 基类契约收敛族（2 ID）→ M2.8 族批

- **选择**：族批。两处均为抽象基类单点修复、全族生效：common-011-r3 = `AbstractErpCrudBizModel`/`AbstractErpImmutableCrudBizModel` 补 `defaultPrepareSave` override（镜像锁逻辑 + immutable 全拒）+「__save 拒 posted」失败测试；common-013-r3 = `AbstractSubmitForApprovalProcessor` 复合期望态串改双 varargs + 契约 javadoc 加负例（~120 子类继承面单点收敛）。
- **替代方案**：逐子类/逐调用点修复——被否，正是 F1.3 基类范式要消除的形态；deferred——被否，common-011-r3 为 P2 且是 F1.3 家族最后盲区（373 生成页面暴露 upsert 通道）。
- **残余风险**：defaultPrepareSave override 会改变全部实体的 __save 行为（ formerly 可 upsert 的合法场景需逐域确认）——缓解：失败测试先行的同时跑全 reactor 回归；immutable 全拒语义与既有 F1.3 测试对账。

### §2.7 doc 批 — owner-doc 漂移族（15 ID）→ 归属裁决：M2.8 内独立 doc 分片（分片 5）

- **选择**：**独立 doc 批次、挂 M2.8 计划内立项**（不新增 roadmap 工作项）：15 个漂移站点一次 doc 修订批收敛（hr2-027/crm-023/ct-028/ct-029/b2b-019/ast2-025/app-002/md-017/md-018/fin3-018/mfg2-025/prj-025/mnt-021/qa-032/pur-016），判定口径 = 文档一致性走查（非代码修复），修订不改需求契约段语义（qa-031/qa-033 需求分歧面除外——已 deferred）。
- **替代方案**：(a) 归入 M2.8 代码族分片混跑——被否，判定/验收口径不同（走查 vs checker/grep 断言归零），混跑会稀释验收信号；(b) 完全独立于 M2.8 的新工作项——被否，roadmap 不发明工作项，M2.8「五维横切族修复」语义足以承载。
- **残余风险**：md-018（三死配置键「落地或降级」）与 ct-029（MOCK provider 切换路径）含产品语义微裁决——缓解：doc 批内逐项登记裁决（doc 降级为默认路径，键落地走后续需求通道）。

### §2.8 dict 值域族（4 ID）→ 单独裁决：P1 执行归 M2.7，跨轮边界移交 r1 通道

- **选择**：ct-025-r3（P1，writer 改 value 形态或 dict 收敛单属性 + 历史数据迁移；ORM 触及面走保护区 dual-agent-approval）归 M2.7 ct 站点；drp-022-r3/b2b-018-r3/prj-024-r3 三个 r3 P3 站点与 ct-025 同批范式对齐（dict 死值/双轨统一收口）。**跨轮边界**：r1 b2b-013「统一批次」注记移交 r1 通道——r3 只修 r3 站点，范式一致性由 M2.9 同型状态继承统一核注，不反向扩 r3 范围到 r1 open 面。
- **替代方案**：全 dict 面（含 r1 b2b-013）统一一批——被否，违反横切关注点 5（r1/r2 工作项不入 r3 裁决范围）；dict 族整体 deferred——被否，ct-025-r3 为 P1 不可 deferred。
- **残余风险**：ct-025-r3 历史数据迁移涉及存量行形态转换，若走 writer 改 value 形态须迁移脚本与快照重录联动评估。

### §2.9 留域批（P1 三条 + 同链/同域独立项，24 ID）

- **选择**：P1 三条按 roadmap 行落位——mfg-022-r3 → M2.3；ct-025-r3/ct-026-r3 → M2.7（ct-025 属 dict 族见 §2.8）。12 个 P2 + 10 个 P3 独立项路由到其域批通道（M2.2/M2.4/M2.5/M2.6/M2.7），**域批计划起草时按 P1 → P2 → P3 序消化**（域批 charter 为 P1 修复批，独立项为同域附带项——本裁决将其显式挂靠，M2.9/MV.2 终态要求全部 finding 达 fixed/not-a-problem/deferred，故必须承载）。
- **替代方案**：P2/P3 独立项全部 M2.8 或 deferred——被否：单域功能性修复（守卫/writer/白名单）不具族形态，塞入族批破坏「一处修复全族回填」判定；全部 deferred 违背 P2 级产品价值（核销守卫/越权纠错/状态机守卫均有实害）。
- **残余风险**：M2.7 通道承载量大（P1×2 + dict 族 3 + P2×7 + P3×7 ≈ 19 ID）——缓解：M2.7 计划起草时按域再分片（aps/b2b/crm/cs/drp/log/notify/md/common 各自成片），允许拆多 plan 执行（同一 roadmap 行多次执行先例 = MI.2/MI.3/MI.4）。

### §2.10 逐条 deferred（4 ID）

| ID | 理由 | 触发条件（重开事件） |
| --- | --- | --- |
| P3-CK-b2b-020-r3 | assertCan 防御分支裸 IAE——私有 switch 全覆盖不可达护栏，无运行时可达路径；索引明示「不入修复队列优先级」 | 同型站点 ≥3 时立防御编程约定族批（NopException/AssertionError 显式化） |
| P3-CK-cs-024-r3 | 同上同型（内部不可达护栏三 case 闭合） | 同上 |
| P3-CK-qa-031-r3 | 疑似需求分歧（物料级 vs 单据类型级强制质检两机制并存），审计不裁决 | 产品侧需求裁决后改代码或 3 doc 同步（归 M2.9/MV.2 deferred 计数 + 需求通道） |
| P3-CK-qa-033-r3 | 疑似需求分歧（检验模板类别级层级未实现），审计不裁决 | 产品侧裁决：补类别维度（ORM 保护区）或 doc 降两级描述 |

## §3 跨轮边界与状态继承注记

> r3 修复批只产证据注记；状态回填归 M2.9（owner doc §索引回填协议 r3 centralized 口径）。r1/r2 未消化 open finding 不入本裁决范围（横切关注点 5），仅登记「由其所属轮次通道处理」。

| 继承关系 | 注记 |
| --- | --- |
| P2-CK-mfg-010（r1 open）← P1-CK-mfg-022-r3 | 同控制点升级源（§3.1 归并 + 后果升级新立）。M2.3 修复 mfg-022-r3（reverseApprove docStatus 白名单）即消解其控制点——r3 修复批落证据注记，r1 行状态不动，回填归 M2.9 同型状态继承 |
| P2-CK-mfg3-012（r1 open，委外同型 reverseApprove 无守卫） | M2.3 修复范式（docStatus 白名单）同型覆盖其站点；M2.3 执行时一并核注证据，回填归 M2.9 |
| P2-CK-pur-005 / P1-…-sal-010（r1 open，核销 cancel/settle 侧）← P2-CK-pur-015-r3 | 「核销 docStatus 守卫」统一批次设计跨轮协同（索引原文）；M2.5 执行 pur-015-r3 时落统一设计证据，r1 两行回填归 M2.9；r1 通道若先修则 M2.5 复用其范式 |
| r1 b2b-013「统一批次」← dict 值域族 | 见 §2.8：r3 只修 r3 站点，统一批次注记移交 r1 通道 |
| FNPT 族 r1 站点（drp-018/b2b-011） | 由 r1 通道处理；族 B 范式互链不扩范围 |
| r1/r2 其余 open finding（含 mnt-001/002/003 已修在位「回填缺口」注记型） | 全部由其所属轮次通道处理，不入本裁决；M2.9 回填时按「索引 open=回填缺口」先例（fin4-013/mnt-001 先例）逐行核注 |

## §4 M2.8 执行切分建议（供 M2.8 计划起草直接消费；不预写计划）

**分片与执行序**（每片验收口径 = 该族 checker/grep 断言归零 + 域测试零回归 + 双 checker 不高于基线，对齐 owner doc §族回填范式）：

| 分片 | 内容（族 → ID 数） | 按域分片 | 验收断言 | 序 |
| --- | --- | --- | --- | --- |
| 1 | B FNPT 注册 + enforcement（12） | fin/mfg(×2)/hr/prj/qa/ct/mnt/aps/log/md/app 11+1 片 | 各域 action-auth 注册断言 grep 归零；enforcement-on 断言测试绿（app 片） | ①（机械面大，先解锁 enforcement 判断） |
| 2 | A daoFor 豁免注释/I*Biz（14） | fin/mfg/hr/ast/qa/crm/ct/cs/drp/log 10 片 | 全仓「daoFor 命中处无注释」扫描归零（U20 聚合对账 + 每站点选择注记） | ② |
| 3 | E+F+C 小族批（DIM-F 5 + 基类契约 2 + ctx 兜底 2） | 按域 6~8 片 + common 2 片 | 死状态样式分支 grep 归零；__save 拒 posted 测试绿；期望态串契约测试绿；裸 new 计数对账归零 | ③ |
| 4 | D DIM-T 测试批（11） | fin/mfg/hr/mnt/aps/md/ast/notify 8 片 | 每缺口 ≥1 测试 + 全 reactor `mvn test` 零新增失败 | ④（测试批最后统一验收全 reactor） |
| 5 | doc 批（15） | 单批一次修订 | 逐站点复核清单 + 维度⑮断言抽查归零 | ⑤ |
| 6 | dict 值域批（4） | ct/drp/b2b/prj 4 片 | 双轨/死值 grep 断言归零；ORM 触及面双批准记录落盘 | ⑥（**保护区 dual-agent-approval 批准流与分片 1 并行先行启动**，避免阻塞收尾） |

**并行域批通道**（非 M2.8）：M2.3（mfg-022-r3）、M2.4（ast2-024-r3）、M2.5（pur-015-r3 + inv-012-r3）、M2.6（qa-026-r3 + qa-030-r3）、M2.7（§2.9 全表 19 ID）——域批与族批可交错执行，无共享文件面冲突（族批触 base class/action-auth/页面样式，域批触域内业务守卫）。

**收敛判据**：93 ID 全部到达 fixed / not-a-problem / deferred(4)；M2.9 双索引回填 + 同型状态继承核注（§3 表全消化）。

## §5 逐 ID 复核与零冲突声明

- 枚举程序：`rg '^.*P[0-3]-CK-[a-z0-9]+-[0-9]+-r3 \|' docs/audits/check/ai-check-index.md`（2026-09-10 执行时点实值）= 93 行；逐行读取摘要与「修复归」列后归类，ID 清单与本表逐一对应，无凭记忆补录。
- P 级分布对账：P1=3（ct-025/ct-026/mfg-022）/ P2=17 / P3=73，与 plan Current Baseline、M1.17 收官核账（m1-17-coverage-matrix-final.md 及跨轮索引 §报告清单）一致。
- P1 去向一致性：mfg-022-r3 → M2.3、ct-025-r3/ct-026-r3 → M2.7，与 `docs/backlog/ai-check-r3-roadmap.md` M2.3/M2.7 行域范围一致（ct 属 M2.7「其余域」清单）。
- 本裁决不改两份索引任何行、不翻转 roadmap 任何状态（Non-Goal 边界内）。
