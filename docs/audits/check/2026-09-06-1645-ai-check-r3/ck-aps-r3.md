# ck-aps-r3 — aps U15 五维符合性审计报告（ai-check-r3 M1.15）

> 工作项：M1.15（六单元全格；本报告 = U15 aps 格，姊妹格见 `ck-maintenance-r3.md` / `ck-logistics-r3.md` / `ck-notify-r3.md` / `ck-master-data-r3.md` / `ck-common-r3.md`）。
> 执行日期：2026-09-09。审计时点 T0：HEAD `e331c55b2`（脏面 = 1 条 untracked 计划文件，tracked 零修改）——口径同 `ck-maintenance-r3.md` 头注。
> 判定依据（冻结）：`m0-5-audit-checklists.md` §1/§2 + §3.3 U15 行 + §6 勘误 E1。
> 范围：aps 全域——排产引擎（前向/后向/TOC 三模式 + 瓶颈探测 + 约束时间轴）/约束求解（constraint 族）/auto-dispatch（齐套/缺料 hold/手工派工）/替代路由（manual override + routing-selection-reason）/OperationOrder 13 边状态机 + 产能预留 + ATP/CTP 服务（`module-aps/erp-aps-{dao,service}` src/main）；owner docs `docs/design/aps/`（scheduling/constraint-based-planning/auto-dispatch/alternative-routing/state-machine/README/use-cases/seed-data）。Skills：`nop-platform-conformance-audit-prompt` + `code-quality-audit-prompt`（DIM-B）；其余维 Skill: none。
> 切片边界（§3.2）：posting 引擎归 fin-1（aps 无过账面）；common 抽象族行为归 U20（基类接入但 status 列惰性 = F1.3 注册边界，残余归 pur-003 族共性 + common-011-r3 save 通道盲区）；聚合横切面归 U21；notify 本体归 U11（aps 消费点 3 事件经 `IErpSysNotificationBiz` 合规记录）。
> 零生产代码改动：本报告 + 双索引 + 计划勾选注记为唯一产物。

## 1. 五维覆盖矩阵（5/5 完整）

| 维度 | 判定面（U15 全格） | 机械核查结果 | verdict |
| --- | --- | --- | --- |
| **DIM-B 后端平台合规** | §1.1 全套 + 15 维度走查（域焦点：前向/后向排产引擎/约束求解/dispatch；共性②⑦⑧⑨⑮）+ ⑮抽样 6 断言 | 反模式族全零（RuntimeException=0/@Inject private=0/System.currentTimeMillis=0/@Transactional 0/`LocalDate.now`·`LocalDateTime.now(` 0）；checker 19 规则零漂移；`__XGEN_FORCE_OVERRIDE__` 12 处全为 erp-aps-meta dict 校验点。②跨域 mfg/inv 只读经 IDaoProvider.daoFor **9 文件全部有豁免注释/裁决 javadoc**（D5 选项 A/matrix §9.4 永久豁免/零启动耦合范式逐一在位）+ notify 经 `IErpSysNotificationBiz` 唯一强注入边（DAG 无环）+ mfg SPI 弱收集空 list 兜底——**②维 pass**（对比 mnt/log 的缺注释站点，aps 为全仓最规范域）；⑦aps 无 notGenCode 面（被引用侧无）、Maven 边单向 pass；⑧operation-order-status 8 值逐值 writers 全活零死值 + 13 边矩阵与 owner doc §2 逐条对齐 + 终态无出边 + start/complete/cancel/revertToDraft/hold/unhold 六命名边守卫在位（引擎可行性直写边 = javadoc :37-40 有意边界裁定）——**⑧维 pass**；⑨job 双层门控（enabled 默认 false）+ cron 键漂移 = aps-010 open + **18 个自定义 mutation 零专属 FNPT = 新立 P3-CK-aps-015-r3**（同族 mnt-023-r3）；⑮6 断言 4 一致 2 漂移（均归并既有 aps-003/005/004，见 §2.5） | **finding**（4 新立：1 P2 + 3 P3；归并 11，见 §2） |
| **DIM-F 前端页面与 E2E** | §1.2 全套 + 排产甘特图（复杂手写页清单成员） | validate:flux step[1/3] 0 error/999/855；325 ERR 全 variant 族（aps 命中 4 条同族不立项）；AMIS 0 / ORM flux 缺失 0。页面面 7 实体 ×4 文件全量清点：7/7 view.xml 保留层继承（bounded-merge 定制 `ErpApsOperationOrder.view.xml:8`）；甘特页 `schedule-gantt.flux.yaml`（flux 权威）+ `page.yaml` 双载体 = 全仓 convention，双份零遮蔽缺陷抽查（对照 mfg-023-r3 孪生缺陷形态：参数绑定两份均正确）；数据访问 `@query:ErpApsOperationOrder__findGanttData` + `@mutation:__updateSchedule` REST /r/ 合规，i18nEn 齐全，死分支 0，auth 注册 FLUX 资源在位。**注记**：updateSchedule 裸 mutation 的前端接线点 = aps-004 既有 finding 消费侧同证（页面按已暴露 mutation 契约接线，页面本身零缺陷）；微瑕：`ErpApsCapacityReservation.view.xml` 保留层 0 处 i18n-en（生成层兜底，非违规）。E2E：4 个 aps spec PageObject 合规 + E2E_ENGINE 缺省 flux | **pass**（归并态注记） |
| **DIM-S seed 数据** | §1.3 全套 + aps 7 表 seed（M1.4b 批，`docs/design/aps/seed-data.md`） | `TestErpSeedDataIntegrity` **4/0/0/0 全绿**；porcelain 空；**372 CSV + 1 SQL** 冻结值；`erp_aps_*` **7 CSV** 精确在册（plan/schedule/operation_order/capacity_reservation/constraint/dispatch_log/dispatch_rule 族）；aps deploy `_seed_*.sql` = 0（登记处一致）；引擎重算覆盖防护：schedule-status 静态行无引擎覆盖门控面 N/A（aps 无 posted 凭证域） | **pass** |
| **DIM-T 单元测试** | §1.4 全套 + P2 OperationOrder 排产清单行 | `mvn test -pl module-aps/erp-aps-service` **82/0/0/0 全绿**（= 锚点 82 零增量，两轮同值）；17 测试类（含 4 状态机变体）+ 74 autotest 用例目录；RECORDING = 0；**P2 行在位**：`TestErpApsSchedulingEngine`（6）/`TestErpApsCapacityReservation`（3 含并发冲突）/`TestErpApsSchedulingToc`（6）/`TestErpApsAutoDispatch`（10）/`TestErpApsOperationOrderStateGuards`（11 边守卫逐边）/`TestErpApsAlternativeRouting`（8）；**覆盖缺口 = 新立 1 条**：`batchScheduleForward`/`updateSchedule`/`findGanttData`/`simulateSchedule` 4 个公开动作零测试引用（**P3-CK-aps-016-r3**，同型 mnt-020-r3/hr2-030-r3 族） | **finding**（1 新立 P3） |
| **DIM-I i18n（MI 先行时序口径）** | §1.5 全套 | `--strict` **PASS exit 0** + `--self-test` **PASS**；aps 探针族 CAT-1 = 0 维持清零（MI 先行口径）；WHITELIST aps 条目 **2**（`IErpApsOperationOrderBiz.java` + `ErpApsOperationOrderBizModel.java` 均 E3 @Description 豁免）四要素齐备实核（两文件在位 + 各 @Description 1 行实证 + plan 2026-09-07-1715-1 Phase 2 裁决）；`@Locale` 缺失 = 空；meta 零手改 | **pass** |

## 2. Finding 三态裁决（§2 跨轮查重全维强制）

> 查重源：r1 `ck-aps.md`（C8.1）全 11 条逐一比对 + r2 只读目录 + §Mission 基线快照。**本轮新立 5 条（1 P2 + 4 P3）**；历史 11 ID 零覆写（r1 族最大号 CK-aps-011，新立自 012 起）。

### 2.1 复用 — 0 条（11/11 open 无 fixed 同型）

### 2.2 归并（同型 open 追加证据至原 ID）— 11 条

| 原 ID | r3 复核证据（T0） |
| --- | --- |
| P1-CK-aps-001 | `ErpApsSchedulingProcessor.java:174-179` 仍仅 horizonStart 非空时 ge(earliestStartDateT) 无 isNull 分支；建单侧 `ErpApsWorkOrderToOperationProcessor.java:166-186` 不写该列，引擎三级兜底（Engine:767-779）被查询前置过滤旁路原样 |
| P1-CK-aps-002 | Processor:94/:130 `setFrozenPlanned(null)` + 时间轴仅 maintenance 约束（Engine:656-668）+ `acquireReservation` pre-check（:262-269）persist 即冲突整轮回滚原样 |
| P2-CK-aps-003 | `releaseReservationsByOrder` 生产调用仍仅 2 处（InsertRush:80/ManualOverride:58）；cancel:256-269/complete:241-252/updateSchedule:339-355/doDispatch:348-376 均不释放；SM Bean javadoc :48-49 自认 Deferred 原样 |
| P2-CK-aps-004 | `ErpApsOperationOrderBizModel.java:339-355` updateSchedule 仅 null-start 检查，零产能校验/零状态守卫/零预留同步/end 可空；前端 `schedule-gantt.flux.yaml:88-96` 已接通原样 |
| P2-CK-aps-005 | `ErpApsSchedulingEngine.java:688-698` sortByForward 首键 priority + :713-725 前置约束单向游标 + :741-747 recordChain 只推进不回溯原样 |
| P2-CK-aps-006 | 常量 :48-50 + 消费点 3 处在位；三库 `_seed_erp-notify.sql` grep `aps.` 零命中原样（notify-002 全仓族 mnt/log/aps 模板缺口并案跟踪） |
| P2-CK-aps-007 | `ErpApsWorkOrderScanJob.java:41` biz 直调无 runInSession vs 同模块 AutoDispatchJob:57 范式原样 |
| P2-CK-aps-008 | **补充裁定**：BizModel:49 `extends AbstractErpCrudBizModel` 在位但 aps 全域 ORM 零 posted/approveStatus 列（orm grep=0）→ ErpCrudStatusLock 守卫惰性（基类 javadoc :17 自述边界）；未 override defaultPrepareUpdate → status/plannedStartDateT/machineId 仍可 update_ 直改。**现症在位 + 通道盲区升级注记**（generic `save` 通道无守卫 = common-011-r3 本轮新立的 U20 格核心证据站点之一） |
| P3-CK-aps-009 | `ErpApsConfigs.java:10-22` 四键声明零消费（对照已消费键 CONFIG_TOC_BOTTLENECK_THRESHOLD:117/CONFIG_SCHEDULING_SOLVER:147）原样 |
| P3-CK-aps-010 | 两 job yaml `.cron-expr` vs bean `-cron` 键名漂移双在位原样 |
| P3-CK-aps-011 | A 项 `Engine.java:664` 无守卫在位——**现症收窄注记**：orm:185-186 constraint startTime/endTime 已 `mandatory="true"`（r1「非强制列」前提与现状不符，NPE 触发面被模型级强制收窄）；B 项 orgId 缺失（AutoDispatch:316-326/AtpCtp:116-147）+ isReservationActive 逐条 getEntityById N+1 + C 项 ATP 公式缺计划入库/安全库存两项（AtpCtp:110-114 vs scheduling.md:308）原样 |

### 2.3 新立 `-r3` — 5 条（1 P2 + 4 P3）

**P2-CK-aps-012-r3**（DIM-B ⑧ 状态机终态复活）
- **控制点**：`ErpApsSchedulingInsertRushOrderProcessor.java:101-103`——`if (!OP_STATUS_DRAFT.equals(rush.getStatus())) { rush.setStatus(DRAFT); }` 无 assertCan/终态检查。
- **问题**：FINISHED/CANCELLED 工序被 insertRushOrder 直接置 DRAFT → 引擎重排 → 回写 PLANNED，违反 state-machine.md:54-56「终态不可恢复」；IN_PROGRESS 本体仅当落在 loadPlannedInWindow（:54）窗口内才被 :57-63 拦截，plannedEndDateT 已过的在制工序同样逃逸。既有测试 `testInsertRushOrderRejectsInProgress:128-141` 只覆盖窗口内其他工序，未覆盖 rush 本体。
- **三态裁决**：新立（r1 对 insertRushOrder 仅「验证为正确」结论 L122，无本控制点）。P2（状态机终态语义击穿，触发条件为对终态/过期工序执行急单插入）。
- **修复方向**：M2.x——rush 本体加状态白名单（DRAFT/PLANNED/UNSCHEDULABLE）+ 过期窗口在制拒绝或显式 re-open 裁决；先写终态复活失败测试。

**P3-CK-aps-013-r3**（DIM-B ⑩/D2 批量容错）
- **控制点**：`ErpApsOperationOrderBizModel.java:124-131` batchScheduleForward 仅 `catch (NopException)`，javadoc :115 承诺「不阻塞其他行」。
- **问题**：引擎/持久层非 NopException（JdbcException/NPE 族）直接冒泡中止整批；且先行成功行的 managed 实体改写在整批回滚时与 `BatchOperationResult` 上报 success 背离（结果/事务一致性缺口，同型 pur-013/mfg-017 族）。
- **三态裁决**：新立。P3。
- **修复方向**：M2.x——catch 面收窄为 Exception + 失败行 evict/clear-session 或结果上报与事务边界对齐。

**P3-CK-aps-014-r3**（DIM-B ⑧ 约束装载）
- **控制点**：`ErpApsSchedulingProcessor.java:193-196` 约束 horizon 过滤要求双界非空；`ErpApsSchedulingEngine.buildTimelines:656-668` 对载入行无条件 addBusy。
- **问题**：仅设 horizonStart（或仅 End）的排产方案对维护约束**完全不过滤**，历史长期停机约束持续侵占未来可排产能（与 aps-002 假冲突叠加放大）。
- **三态裁决**：新立（r1 aps-011 仅登记约束空时间 NPE 维度）。P3。
- **修复方向**：M2.x——单界方案按已设界单侧过滤 + 载入行与 horizon 相关性裁剪。

**P3-CK-aps-015-r3**（DIM-B ⑨ FNPT 家族）
- **控制点**：手写层 `erp-aps.action-auth.xml` 零专属 FNPT（纯菜单资源）；18 个自定义 mutation（OperationOrder 16 + Schedule 2）+ IErpApsAtpCtpService 3 query 全靠生成层 7 实体 × {query,mutation} 粗粒度 FNPT 兜底。
- **三态裁决**：新立（同族 mnt-023-r3 本轮新立，跨域 9 先例全 P3 校准）。P3。
- **修复方向**：M2.x 按排产员/计划员角色补 FNPT 注册（state-machine.md:87 cancel-approve Deferred 边界对齐）。

**P3-CK-aps-016-r3**（DIM-T 覆盖缺口）
- **控制点**：`batchScheduleForward`/`updateSchedule`/`findGanttData`/`simulateSchedule` 4 个公开动作 src/test + `_cases` 零引用（grep 实证）；其中 findGanttData 为甘特页唯一数据源、updateSchedule 为唯一拖拽 mutation（aps-004 消费入口）。
- **三态裁决**：新立（同型 mnt-020-r3/hr2-030-r3/mfg2-027-r3 族）。P3。
- **修复方向**：M2.x 测试批补甘特数据/拖拽/批量/仿真四动作用例。

### 2.4 归属标注（§3.2 + 跨域横切）

- posting 引擎：aps 无过账面，不适用。
- common 抽象族：基类接入合规但全域无 posted/approveStatus 列 → 守卫惰性 = pur-003 族共性样本 + common-011-r3（save 通道盲区）关键佐证站点；IDaoProvider 9 文件豁免注释全齐（全仓规范性最优域，无站点归 common-014-r3 缺注释清单）。
- 聚合横切面归 U21：聚合器注册在位；FNPT 缺口 aps-015-r3 本格新立（与家族同批）。
- notify 消费点记录（归 U11 格）：3 事件（workorder-no-routing/operation-workcenter-missing/dispatch-material-shortage）经 I*Biz 合规；模板缺口归并 notify-002 全仓族。

### 2.5 维度⑮断言抽样记录（2 doc × 6 断言：一致 4 / 漂移 2）

state-machine.md 3 断言（`erp.err.aps.op-illegal-transition` 三入口拦截 ✓ / HOLD 双态三边 ✓ / §2「cancel 释放预留产能」→ **漂移**（= aps-003 Deferred 承诺矛盾，§2 表与 §4 裁决互相矛盾））；scheduling.md 3 断言（§2.4 earliestStartDateT 前置约束公式 → **漂移**（= aps-005 乱序倒挂）/ §8.3 dragUpdateOperation「需校验产能」→ **漂移**（= aps-004 零校验 + 方法名/签名漂移）/ §8.1 auto-dispatch runInSession + 开关 cron 逐字 ✓（cron 键漂移归 aps-010））。漂移 2 = 既有 r1 ID 同点，未新立未触发扩样。

## 3. 统计

| 级别 | 本轮新立 | 复用 | 归并 |
| --- | --- | --- | --- |
| P0 | 0 | 0 | 0 |
| P1 | 0 | 0 | 2（aps-001/002） |
| P2 | 1（aps-012-r3 终态复活） | 0 | 6（aps-003..008） |
| P3 | 4（aps-013/014/015/016-r3） | 0 | 3（aps-009/010/011） |
| **合计** | **5** | **0** | **11** |

五格 verdict：DIM-B **finding**（4 新立）/ DIM-F **pass**（归并态注记）/ DIM-S **pass** / DIM-T **finding**（1 新立）/ DIM-I **pass**。历史 11 条 r1 ID 零覆写。

## 4. 剩余风险声明（横切关注点 13 完整四件套）

- **已查**：erp-aps-{dao,service} 全部 Processor（7 族）+ 7 BizModel + SchedulingEngine/BottleneckDetector/AtpCtpServiceImpl/LoadSourceProvider + 13 边状态机 Bean + 2 job + ErpApsErrors/Constants/Configs + 机械程式全套实跑；owner docs 2 doc × 6 断言；r1 11 条全量逐条复核；r2 目录核对；aps seed 7 CSV 精确对账；甘特页双载体逐行抽查。
- **未深查（边界归属）**：ErpCrudStatusLock 基类内部与 save 通道（归 U20/common-011-r3）；mfg/inv 被读实体消费语义（归 M1.5-M1.7/M1.13 已闭合格）；浏览器端甘特渲染回归（归看板专项）；TOC 瓶颈算法数值正确性（引擎数学面仅结构审计）。
- **残留风险（登记不裁决）**：① 11 条归并 open 修复归 M2.x，P1 两条（展望期漏排/增量排产整轮回滚）建议最优先——排产可用性直接失效；② aps-012-r3 与 003/004 同为「预留生命周期 + 状态守卫」簇，建议 M2.x 同批设计（释放时机/守卫白名单/拖拽校验三合一）；③ 守卫惰性共性族（aps 8 值 status 列全样本）归 pur-003 族 + common-011-r3 统一裁决。
- **successor 触发条件**：M1.17 收官完整性校验本报告 5/5 格；auto-dispatch/workorder-scan job 投产（enabled=true）前 aps-007/010 必须修复；甘特拖拽投产前 aps-004/012-r3 必须修复。
