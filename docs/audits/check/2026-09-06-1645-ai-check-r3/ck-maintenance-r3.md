# ck-maintenance-r3 — maintenance U10 五维符合性审计报告（ai-check-r3 M1.15）

> 工作项：M1.15（U10 + U15 + U16 + U11 + U01 + U20 各 × 五维全格，冻结清单 §4 映射表第 15 行；本报告 = U10 maintenance 格，其余五格分别见 `ck-aps-r3.md` / `ck-logistics-r3.md` / `ck-notify-r3.md` / `ck-master-data-r3.md` / `ck-common-r3.md`）。
> 执行日期：2026-09-09。审计时点 T0：HEAD `e331c55b2fc42196cdd008ca2bb45232c9046843`（计划基线 `2c1c1ef25` 后两笔推进均为姊妹审计产物提交，生产代码零变化）；脏面 = 1 条 untracked 计划文件（本计划自身），tracked 零修改。
> 判定依据（冻结）：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md` §1/§2 + §3.3 U10 行 + §6 勘误 E1。执行期仅在报告与计划勾选注记追加证据，判定标准零改动。
> 范围：maintenance 全域（C 级）——设备（equipment 状态机 + status log + 校准）/维保计划（schedule→due visit 生成）/报修（request→visit 编排 + 额外故障）/访问（visit 生命周期 + 任务模板）/停机（downtime entry + 运行时链）/备件消耗（usage + 出库过账）/OEE 与看板报表（`module-maintenance/erp-mnt-{dao,service}` src/main）；owner docs `docs/design/maintenance/`（state-machine/equipment-integration/README/use-cases/seed-data）。Skills：`nop-platform-conformance-audit-prompt` + `code-quality-audit-prompt`（DIM-B）；其余维 Skill: none。
> 切片边界（§3.2）：posting 引擎内部归 fin-1（mnt 两 posting dispatcher/provider 为消费侧，本格只审调用点）；common 抽象族行为归 U20（15/15 BizModel 基类接入调用点合规）；聚合横切面归 U21（E1 勘误路径聚合器含 `/erp/mnt/auth/erp-mnt.action-auth.xml`）；notify 派发子系统本体归 U11（mnt 消费点 3 处记录：downtime 7208/7209 + posting 失败告警）。
> 零生产代码改动（roadmap 规则 6）：本报告 + 双索引 + 计划勾选注记为唯一产物（收官 `git status` 机械核证见 §4）。

## 1. 五维覆盖矩阵（5/5 完整）

| 维度 | 判定面（U10 全格） | 机械核查结果 | verdict |
| --- | --- | --- | --- |
| **DIM-B 后端平台合规** | §1.1 全套机械程式 + 15 维度走查（域焦点：设备状态机/维保计划/预警查询；共性②⑧⑨⑮）+ 维度⑮断言抽样 6 条 | 反模式族全零（RuntimeException=0/@Inject private=0/System.currentTimeMillis=0/@Transactional=0/LocalDate.now=0）；checker 19 规则 = M0.3 快照行零漂移 exit 0；`__XGEN_FORCE_OVERRIDE__` 12 处全为 erp-mnt-meta dict.yaml 校验点；聚合器 E1 路径含 mnt 注册。②跨域写经 `IErpInvStockMoveBiz`/`IErpFinVoucherBiz`/`IErpSysNotificationBiz` 全合规，跨域只读 daoFor（inv/fin/mfg/qa 11 站点）5 文件有 E3 豁免注释、9 文件无专项注记（两 dispatcher javadoc 以 billHeadCode 判重语义间接覆盖，Report/Processor 基类为域内聚合）——汇总归 U20 全仓族（common-014-r3）；⑧12 dict 逐一 writers 核对：visit-status 5 值/request-status 6 值/status-log-source 3 值全活（DISPOSAL 第 4 写值 = owner doc `equipment-integration.md:45` 显式 successor 裁决非遗漏），equipment-status 5 值可达但 MANUAL changeStatus 无值域校验（mnt-014 open 现症在位 `ErpMntEquipmentBizModel.java:31-41`）；visit-result/calibration-result/schedule-type/priority LOW·HIGH·URGENT 等零命名写点值归 mnt-006 CRUD 旁路维度不计死值；⑨手写层 4 个专属 FNPT（Calibration/Request × approve/reverseApprove）vs 18 个自定义 mutation → **16 个零专属注册 = 新立 P3-CK-mnt-023-r3**（跨域家族 drp-018/b2b-011/prj-022-r3/qa-029-r3/hr2-028-r3/fin4-023-r3/mfg2-024-r3/mfg3-020-r3/ct-030-r3）；job cron 键漂移 = mnt-012 open 在位；⑮6 断言 5 一致 1 漂移（= **新立 P3-CK-mnt-021-r3**） | **finding**（2 新立 P3：021/023；复用 5 + 归并 14，见 §2） |
| **DIM-F 前端页面与 E2E** | §1.2 全套 + 设备台账/维保看板页面族 | `npm run validate:flux` step[1/3]：FLUX_PAGE_ERROR_COUNT 0 / 999 页（erp 855）；整体 325 ERR 全 variant 族既有外部漂移（mnt 命中 26 条同族不立项，非 variant ERR = 0）；`component="AMIS"` 0、ORM `ext:web-renderer="flux"` 无缺失。页面面 15 实体目录全量清点：15/15 view.xml 保留层 `x:extends` 继承、`git status module-maintenance` 零脏面；手写页 6 份（dashboard/visit-wizard 双载体 + report 两报表）全走 `/r/` REST（`@query:ErpMntDashboard__*` ×5 / `@query:ErpMntVisit__findPage` / `/p/ErpMntReport__download` blob），i18nEn 全量在位，docStatus 死状态样式分支 0。E2E：8 个 mnt business-actions spec PageObject 合规 + `E2E_ENGINE` 缺省 flux 实证（engine.ts `return 'flux'`）+ 页面级 GraphQL 断言 0；**归并态**：`tests/e2e/visual/_exploration/complex-pages.snapshot.spec.ts`（:149 `/mnt-visit-wizard` 页 + 内联 `data-slot` selector）与 `tests/e2e/visual/business-actions.snapshot.spec.ts`（ErpMntVisit cancel-dialog/status-tags 行 + 内联 `.cxd-`/`data-slot`）= P3-CK-cs-027-r3「visual 层内联 selector」家族 **2 新站点**（M1.14 cs 格同族，0 新立）。dashboard/visit-wizard `main.page.yaml`+`main.flux.yaml` 双载体为全仓 convention（flux 权威），遮蔽文件本格抽查零缺陷 | **finding**（归并态 0 新立：cs-027-r3 家族 mnt 2 站点） |
| **DIM-S seed 数据** | §1.3 全套 + equipment/schedule/request/downtime/visit 族 seed（0930-2 批 + M1.2b 批全量扩展） | `TestErpSeedDataIntegrity` **4/0/0/0 全绿**（363 实体 findAll + to-one 零悬空 + 零孤儿 CSV + scope-pinning）；`_init-data` porcelain 空（零 seed 变更）；资产清点 **372 CSV + 1 SQL** = 冻结登记值；`erp_mnt_*` **15 CSV** 在册（设备/计划/访问/停机/备件/校准族）；mnt deploy `_seed_*.sql` 命中 = 0（登记处「其余模块全仓无其他」一致）；mnt 双面快照重录义务：本切片零 seed 变更无触发；posted 静态行面 N/A（mnt 凭证经 provider 运行时生成，seed 无静态 posted 行） | **pass** |
| **DIM-T 单元测试** | §1.4 全套 + 维护链清单行覆盖 | `mvn test -pl module-maintenance/erp-mnt-service` **157/0/0/0 全绿**（= 锚点 157 零增量，Phase 5/7 两轮同值）；src/test 27 测试类 + `_cases` 22 用例根；`SnapshotTest.RECORDING` = 0；维护链覆盖在位：`TestErpMntDowntimeAndE2E.testPlannedVisitFullFlowWithSparePartIssue`（计划生成→visit→complete→备件出库）+ `TestErpMntLaborPosting` + `TestErpMntDueVisitIdempotency`；**覆盖缺口 = 新立 2 条**：`ErpMntReportBizModel.maintenanceHistoryData:199`/`downtimeSummaryData:208` 两 @BizQuery 零测试引用（**P3-CK-mnt-020-r3**）+ `_cases/.../TestErpMntFkNameLoader/` 孤儿快照目录零测试类载体（**P3-CK-mnt-022-r3**） | **finding**（2 新立 P3：020/022） |
| **DIM-I i18n（MI 先行时序口径）** | §1.5 全套（仅零回归 + 白名单合规，不产同类 finding） | `--strict` **PASS exit 0**（0 新增违规 vs 冻结快照 170 files）+ `--self-test` **PASS**；report mode CAT-1..4 = **0/0/0/0**（mnt 探针族 CAT-1 10/CAT-2 7 维持清零零回归）；WHITELIST mnt 条目 **1**（`ErpMntDashboardBizModel.java` E3 @Description 豁免）四要素齐备实核（文件在位 + @Description 1 行实证 + owner doc 指针 + plan 2026-09-07-1715-1 Phase 4 裁决）；`grep -L @Locale` 全量 *Errors.java = 空；meta/聚合 i18n porcelain 空（`_` 前缀生成 yaml 零手改） | **pass** |

## 2. Finding 三态裁决（§2 跨轮查重全维强制）

> 查重源：r1 `ai-check-index.md` §Finding 追踪（同域报告 `ck-maintenance.md` C5.3 全 19 条逐一比对）+ r2 只读目录（无 mnt 同型新独立登记）+ §Mission 基线快照。**本轮新立 4 条（全 P3）**；历史 19 ID 零覆写（实仓核对 2026-09-09：r1 族最大号 CK-mnt-019，新立自 020 起）。

### 2.1 复用（同型已 fixed，HEAD 复核有效）— 5 条

| 原 ID | 修复在位证据（T0） |
| --- | --- |
| P1-CK-mnt-001（validateNotConfirmed 空守卫，CANCELLED 可复活） | `AbstractErpMntSparePartUsageProcessor.java:72-76` 已接线 `documentStateMachine.assertCanConfirm(usage.getDocStatus())`（仅 DRAFT 合法）+ `validateCanReverse` :92-96 接线 `assertCanReverseConfirm`。**注记**：索引行仍 open = 回填缺口（同 fin4-013 先例，原 ID 状态不动）；残留关联面：状态机 Bean javadoc `ErpMntSparePartUsageDocumentStateMachine.java:27`「silent-guard gap 暂不强制」已过期 → 归并 P3-CK-common-008 陈旧 javadoc 家族新站点（U20 格收口） |
| P1-CK-mnt-002（reverseConfirm 吞异常推进终态） | `ErpMntSparePartUsageReverseConfirmProcessor.java:34-52` GL 红冲与反向移动单失败均 rethrow（"kept ACTIVE retryable"），不再吞异常推进 CANCELLED；javadoc :16-21 自证修复 |
| P1-CK-mnt-003（restoreToRunning 无条件覆盖设备态） | `EquipmentStatusLinker.java:133-138` 前置守卫「设备当前 ∈ {UNDER_MAINTENANCE, DOWN} 才恢复否则 no-op」，javadoc :122-127 声明覆盖两条失真路径 |
| P2-CK-mnt-006（CRUD 无状态守卫，同型 F1.3 族） | 15/15 实体 BizModel `extends AbstractErpCrudBizModel`（Dashboard/Report 两个服务型 BizObject 除外，合理）；基类 `ErpCrudStatusLock.shouldBlock` 拒 posted=true/APPROVED 在位（基类行为归 U20 格——save 通道盲区见 common-011-r3 新立） |
| P2-CK-mnt-007（visit complete REQUIRES_NEW 凭证先于主事务末步） | `ErpMntVisitCompleteProcessor.java:44-49` restoreToRunning + completeLinkedRequest 已前移至 doComplete（REQUIRES_NEW postLabor :67-68）之前；:69-70 幂等误 warn 降 info |

### 2.2 归并（同型 open 追加证据至原 ID）— 14 条

> 14 条 open r1 ID 于 T0 全量逐条现症复核在位（file:line 证据），原 ID 状态不动仍 open。

| 原 ID | r3 复核证据（T0） |
| --- | --- |
| P2-CK-mnt-004 | `ErpMntDashboardBizModel.java:329-330` 仍 ge/le(businessDate)；`ScheduleDueGenerator.java:186-191` 与 `ErpMntRequestAcceptProcessor.java:43-52` 均不写 businessDate，`_ErpMntVisit.xmeta:88-91` 无 defaultValue 原样 |
| P2-CK-mnt-005 | `ScheduleDueGenerator.java:94` 循环无 per-schedule try/catch（:101 注释自证）+ `ErpMntDueVisitJob.java:46-47` 仅 LOG.error 无告警重试原样 |
| P2-CK-mnt-008 | Dashboard/Report 全文 orgId 零命中；生成器 data map 无 orgId；job `new ServiceContextImpl()` 无用户上下文（`ErpMntDueVisitJob.java:41`，全域族归 common-015-r3）原样 |
| P2-CK-mnt-009 | `ErpMntSparePartUsageConfirmProcessor.java:21-41` 无 warehouseId/正数守卫 + `SparePartIssueService.java:39,48` 原样透传 + 时长负值守卫缺失（`ErpMntVisitCompleteProcessor.java:57-59`）原样 |
| P3-CK-mnt-010 | `ErpMntDashboardBizModel.java:127-128` 逐设备 computeOee，`OeeCalculator.java` 每设备 7-8 查询链原样 |
| P3-CK-mnt-011 | notify 写点仅 3 文件，全 src/main 无 DRAFT 访问 TODO 提醒生成；`equipment-integration.md:231` 断言仍无载体原样 |
| P3-CK-mnt-012 | `ErpMntDueVisitJob.java:36-40` 仅判空 + :55-56 配置值不进调度器 + job yaml cronExpr=nop.job 键原样；附带漂移 `use-cases.md:30`「LocalDate.now() 基准」vs `CoreMetrics.today()` 同点注记 |
| P3-CK-mnt-013 | 四边界全在位：重叠停机双计（`OeeCalculator.java:252-258`）/quality 无钳制（:145）/报表右边界含次日零点（`ErpMntReportBizModel.java:356`）/开放窗口含 startTime null 草稿行（`ErpMntDowntimeEntryBizModel.java:60`）原样 |
| P3-CK-mnt-014 | `ErpMntEquipmentBizModel.java:31-41` changeStatus 直写 status 无字典校验并同步状态日志原样 |
| P3-CK-mnt-015 | `ErpMntVisitReportAdditionalFaultProcessor.java:65` description 原样 null + :79-88 toLongUserId 宽 catch 无日志原样 |
| P3-CK-mnt-016 | `MaintenanceIssuePostingDispatcher.java:180` + `MaintenanceLaborPostingDispatcher.java:205` 汇率硬编码 ONE 原样 |
| P3-CK-mnt-017 | `ErpMntDashboardBizModel.java:358` setLimit(5000) 截断误报 + Report 无界加载原样 |
| P3-CK-mnt-018 | `ErpMntVisitCancelProcessor` 对 ACTIVE SparePartUsage 零动作 + `ErpMntRequestAcceptProcessor.java:44` 一对一硬绑定原样 |
| P3-CK-mnt-019 | `app-erp-maintenance.orm.xml:311` UK (code,orgId) + 生成器无 orgId → NULL 不判重原样 |

### 2.3 新立 `-r3` — 4 条（全 P3）

**P3-CK-mnt-020-r3**（DIM-T 覆盖缺口）
- **控制点**：`ErpMntReportBizModel.java:199`（maintenanceHistoryData）/`:208`（downtimeSummaryData）——两 @BizQuery 在 src/test + `_cases` 零引用（grep 实证 0 命中）。
- **问题**：两张种子报表的数据集装配逻辑（visit⨝task 聚合、停机分钟聚合）零回归保护；r1 mnt-017 仅登记其无界加载性能维度，覆盖缺失未登记。
- **三态裁决**：新立（同型先例 mfg2-027-r3/hr2-030-r3/fin2-018-r3 覆盖缺口族）。P3。
- **修复方向**：M2.x 测试批补两报表数据集快照用例（renderHtml/download 已有 TestErpMntReportRendering 可扩展）。

**P3-CK-mnt-021-r3**（DIM-B ⑮ owner-doc 漂移）
- **控制点**：`docs/design/maintenance/state-machine.md:56` 断言额外故障 remark 追加 `"[额外故障] " + description` vs `ErpMntVisitReportAdditionalFaultProcessor.java:46` 实码前缀 `"[Additional fault] "`。
- **问题**：用户可见 remark 内容与 owner doc 不符（疑为 MI i18n 英文化批次改码未同步文档）。
- **三态裁决**：新立（r1 mnt 族无此断言站点）。P3。
- **修复方向**：doc 维护批同步前缀字面量（或经配置承载）。

**P3-CK-mnt-022-r3**（DIM-T 测试卫生）
- **控制点**：`module-maintenance/erp-mnt-service/_cases/app/erp/mnt/service/TestErpMntFkNameLoader/` 目录在位，src/test 全树零同名测试类（全仓 grep 实证）。
- **问题**：快照资产失去运行载体，疑似测试类删除后 `_cases` 未清理——孤儿用例目录。
- **三态裁决**：新立。P3。
- **修复方向**：M2.x 测试卫生批删除孤儿目录或恢复载体测试类。

**P3-CK-mnt-023-r3**（DIM-B ⑨ FNPT 家族）
- **控制点**：手写层 `erp-mnt.action-auth.xml` 仅 4 个专属 FNPT（ErpMntCalibration/ErpMntRequest × approve/reverseApprove）；自定义 @BizMutation 共 18 个 → 16 个（Visit 5 中除注册外/Request 3/Schedule 1/DowntimeEntry 2/Equipment 3/SparePartUsage 2）零专属注册，仅靠生成层实体级泛化 mutation 权限兜底。
- **问题**：deny-by-default 下业务动作无角色门，与同域已注册 4 项形成不对称（同族 hr2-028-r3 仅 5/42 在册形态）。
- **三态裁决**：新立（r1 mnt 族无 FNPT 条目；跨域家族 drp-018/b2b-011/prj-022-r3/qa-029-r3/hr2-028-r3/fin4-023-r3/mfg2-024-r3/mfg3-020-r3/ct-030-r3 全 P3 校准）。P3。
- **修复方向**：M2.x 按域角色（维护主管/管理员，对齐 use-cases）补 FNPT 注册，与家族跨域同批对齐。

### 2.4 归属标注（§3.2 共享代码边界 + 跨域横切）

- posting 引擎内部归 fin-1：mnt 两 dispatcher/provider 为消费侧，post/reverse 调用点合规（I*Biz）。
- common 抽象族行为归 U20：15/15 基类接入；跨域只读 daoFor 9 文件无专项注记 → 汇总归 common-014-r3 全仓族裁决；过期 javadoc 1 站点归并 common-008；job `new ServiceContextImpl()` 站点归 common-015-r3 全仓族。
- 聚合横切面归 U21：E1 路径聚合器含 mnt 注册；cs-027-r3 visual selector 家族 mnt 2 站点归并（E2E 资产属跨域共享面）。
- notify 消费点记录（归 U11 格）：downtime 7208/7209 双事件 + posting 失败告警共 3 处，经 `IErpSysNotificationBiz` 合规。

### 2.5 维度⑮断言抽样记录（1 doc 主抽 + 1 doc 交叉，6 断言：一致 5 / 漂移 1）

state-machine.md 4 断言（visit CANCELLED 终态语义 ✓ 修复后一致 / DRAFT→SCHEDULED→IN_PROGRESS→COMPLETED + 三源 cancel 六边矩阵 ✓ / §56 remark 前缀 → **漂移 = 021-r3 新立** / MAINTENANCE_ISSUE(492)/MAINTENANCE_LABOR(493) + Dr 6602 config 键 ✓）；equipment-integration.md 2 断言（COMPLETED 恢复 RUNNING/IDLE 按前态 ✓ / notify 7208 + `erp-mnt.downtime-notify-enabled` 门控降级 ✓）。漂移 1 < 2 未触发扩样条款。

## 3. 统计

| 级别 | 本轮新立 | 复用（fixed 复核有效） | 归并（open 追加证据） |
| --- | --- | --- | --- |
| P0 | 0 | 0 | 0 |
| P1 | 0 | 3（mnt-001/002/003，索引 open=回填缺口注记） | 0 |
| P2 | 0 | 2（mnt-006/007） | 4（mnt-004/005/008/009） |
| P3 | 4（mnt-020/021/022/023-r3） | 0 | 10（mnt-010..019） |
| **合计** | **4** | **5** | **14** |

> 计数精确对账：r1 全 19 条 = 复用 5（P1×3 + P2×2）+ 归并 14（P2×4 + P3×10）✓；新立 4 全 P3。

五格 verdict：DIM-B **finding**（2 新立 P3）/ DIM-F **finding**（归并态 0 新立）/ DIM-S **pass** / DIM-T **finding**（2 新立 P3）/ DIM-I **pass**。历史 19 条 r1 ID 状态零覆写（5 fixed 复核有效 + 14 open 追加证据）。

## 4. 剩余风险声明（横切关注点 13 完整四件套）

- **已查**：erp-mnt-{dao,service} 全部 Processor 族 + 15 BizModel + 4 状态机 Bean（visit/request/usage/document + 设备 linker）+ ScheduleDueGenerator/OeeCalculator/EquipmentRuntimeCalculator/EquipmentStatusLinker/SparePartIssueService + ErpMntErrors/Constants/Configs + 机械程式全套实跑（checker 零漂移/反模式/codegen/聚合 E1/validate:flux/seed 门禁/mnt 回归 157×2/strict+self-test）；owner docs 2 doc × 6 断言；r1 19 条全量逐条复核；r2 目录核对；mnt seed 15 CSV 对账；visual spec 全量 selector 扫描。
- **未深查（边界归属）**：`AbstractErpCrudBizModel`/ErpCrudStatusLock 基类内部（归 U20，save 通道盲区 common-011-r3 已新立）；`erp-mnt-web` 渲染时行为（静态 + 门禁，浏览器回归归看板专项）；fin/inv 被写实体消费语义（归 fin-1/inv 格）；workflow 审批流（mnt 无 wf 面登记）。
- **残留风险（登记不裁决）**：① 14 条归并 open 修复归 M2.x，P2 四条（businessDate KPI 恒空/job 无隔离/orgId 无过滤/消耗入参边界）建议最优先；② mnt-023-r3 FNPT 家族与 aps/log/md 同批跨域对齐收口；③ 021-r3 文档漂移与 MI 英文化批次的同步机制缺口（同族 pur-016-r3/hr2-027-r3）建议 doc 维护批统一清偿。
- **successor 触发条件**：M1.17 收官以覆盖矩阵完整性校验本报告 5/5 格；M2.x 修复以「先写失败测试」承接 open 项；due-visit job 投产（enabled=true）前 mnt-005 隔离告警必须修复。
