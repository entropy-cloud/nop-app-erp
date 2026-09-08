# ck-mfg-workorder-r3 — manufacturing「工单与报工」mfg-1 五维符合性审计报告（ai-check-r3 M1.5）

> 工作项：M1.5（U08 × 五维 × mfg-1，冻结清单 §4 映射表第 5 行 / §3.3 U08 行；完工入库移动为跨域消费点——消费侧行为归本格，posting 引擎内部归属 fin-1，§3.2）。
> 执行日期：2026-09-08。审计时点 T0：HEAD `dd39e6cce`（plan-2026-09-08-1042-1 M1.1 fin-1 落盘提交）；脏面 = **空**（`git status --porcelain` 零输出，本计划文件已随 HEAD 入库；姊妹 plan `2026-09-07-2200-1` 已于 `67308e144` ACCEPT 收官，其 mfg StateMachine 直抛领域码批次 `1166339ae` 已在 T0 代码态内）。
> 判定依据（冻结）：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md` §1/§2（判定标准/机械核查程式/跨轮查重）+ §3.3 U08 行 + §6 勘误 E1。执行期仅在本报告与计划勾选注记追加证据，判定标准零改动。
> 范围：工单双轴状态机（approveStatus×docStatus）/作业卡/领料红冲三件套回退/预留/完工入库幂等键（`module-manufacturing/erp-mfg-{dao,service,web}` src/main）；owner docs `state-machine.md`（290 行）+ `use-cases.md` + `material-reservation.md`。Skills：`nop-platform-conformance-audit-prompt` + `code-quality-audit-prompt`（DIM-B）；其余维 Skill: none。
> 零生产代码改动（roadmap 规则 6）：本报告 + 双索引 + 计划勾选注记为唯一产物（收官 `git status` 机械核证见 §4）。

## 1. 五维覆盖矩阵（5/5 完整）

| 维度 | 判定面（mfg-1 增量） | 机械核查结果 | verdict |
| --- | --- | --- | --- |
| **DIM-B 后端平台合规** | §1.1 全套机械程式 + 15 维度走查（重点②③⑧⑨）+ processor-extension-pattern 对齐 + 维度⑮断言抽样 | 反模式族全零（`extends RuntimeException`=0 / `@Inject private`=0 / `System.currentTimeMillis`=0 / `@Transactional∩@BizMutation` 真实共存=0，7 文件命中均为 javadoc「本类不带 @Transactional」约定注记）；checker 19 规则逐项 = M0.3 快照行零漂移（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42）；`__XGEN_FORCE_OVERRIDE__` 20 处全为 codegen dict.yaml 只读校验点零手改；聚合器（E1 勘误路径）含 `erp/mfg/auth/erp-mfg.action-auth.xml` + 保留层 action-auth 在位；I*Biz 跨实体（inv/qa/notify/md 契约）+ `IOrmTemplate` 仅 flushSession；维度⑮ 3 doc × 9 断言 9/9 一致零漂移未触发扩样；15/15 维度无跳维（⑫指针 DIM-T） | **finding**（新立 1 P1 + 1 P3……见 §2；P1-CK-mfg-022-r3 在本维 ⑧⑨ 控制点） |
| **DIM-F 前端页面与 E2E** | §1.2 全套 + mfg-1 面 = 工单看板/BOM 树看板/工单·作业卡·领料实体页 | `npm run validate:flux` `files=855 validated=855 errors=325`——325 条逐条同型（`variant:"primary"`×dropdown-button 枚举漂移）横跨 18 域（mfg 34 条同族成员），successor 在案（roadmap MI.8 done 行），非本切片 finding；`component="AMIS"` 保留层 0、ORM `ext:web-renderer="flux"` 缺失 0；工单看板 `.flux.yaml`（运行时面）REST `@query:` + dashboards pattern + i18nEn 全合规，孪生 `.page.yaml`（回退遮蔽文件）CRP 日期参数错配 → 新立 P3-CK-mfg-023-r3；BOM 树看板 REST+i18nEn 合规（消费 ErpMfgBom 属 mfg-2 面邻接记录）；工单/作业卡/领料实体页 = M0.4 GenPage stub（源头 view.xml `i18n-en` 18 处承载）；E2E：mfg spec 9 个违规选择器=0、`E2E_ENGINE` 缺省 flux（engine.ts L8-9）、GraphQL 命中为 runbook L229 登记 API 驱动型数据层通道（L122 禁令不适用） | **finding**（仅 P3-CK-mfg-023-r3，minor） |
| **DIM-S seed 数据** | §1.3 全套 + work_order/workcenter/crp_load 自洽 + 双层门控默认关 | `TestErpSeedDataIntegrity` **4/0/0/0 全绿**（BUILD SUCCESS）；资产清点 **372 CSV + 1 SQL**（= 冻结口径「M1.5 批次后」登记值）；`git status --porcelain _init-data/` 空（零 seed 变更，快照重录义务未触发）；deploy `_seed_*.sql` 全仓仅 cs/notify 两族且均已在 seed-data.md §97 登记处 ✅ 已聚合（无第三态；mfg 无 deploy seed 零同步义务）；自洽抽查：`erp_mfg_crp_load.csv` workOrderId=1→WO-2026-001 实存 + workcenterId=1→WC-001 实存（= seed-data.md 实证结论行）、work_order 4 行三态覆盖（0930-1 口径）、workcenter 配置链在位（0628-1 口径）；SPC/CRP 双层门控默认关（L19/L21）静态行不被重算覆盖 | **pass** |
| **DIM-T 单元测试** | §1.4 全套 + P1 工单→领料→报工→完工入库→成本结转清单行 | `mvn test -pl module-manufacturing/erp-mfg-service` **308/0/0/0 全绿**（= 姊妹批次 `1166339ae` 参照值，零回归）；覆盖对账：WorkOrder BizModel 10 动作（审批轴 5 + checkAvailability/cancel/start/stop/resume/close/reportCompletion）↔ WorkOrderStateMachine/EndToEnd/ReservationLifecycle（含 P1-CK-mfg-002 回归行）/CancelInspectionLinkage 逐动作落点；JobCard 7 动作 ↔ JobCardStateMachineMatrix + EndToEnd（holdJob/resumeJob/cancelJob grep 命中）；MaterialIssue confirm/reverseConfirm ↔ MaterialIssue/Reversal 专项；行族/TimeLog 裸 CRUD 零自定义动作零义务；P1 关键流逐行全覆盖（审批/预留/领料/红冲/报工/完工入库/成本结转 7 行零缺口，另有 4 orchestration + 2 business-actions E2E）；`SnapshotTest.RECORDING`=0 残留；`*` 通配仅 createTime/updateTime 框架屏蔽、`delVersion` 均为录制值——合规；reverseApprove 终态组合测试缺位已并入 P1-CK-mfg-022-r3 证据不重复立项 | **pass**（附 022-r3 测试缺位注记） |
| **DIM-I i18n（MI 先行时序口径）** | §1.5 全套（仅零回归 + 白名单合规，不产同类 finding） | `--strict` **PASS exit 0**（0 new violations vs 冻结 SNAPSHOT；170 baseline files，totals CAT1..4=0/0/209/1318 为基线记录值，现树实测 0/0/0/0 与 report mode 一致）；`--self-test` **PASS**（5 断言全绿）；mfg 探针族（CAT-1 31/CAT-2 9）维持清零零回归；WHITELIST 27 文件（= MI.9 收官值）mfg 文件条目 1 条 + 按消费链扩样共抽 4 条（mfg Dashboard/inv/pur Dashboard 同簇 + aps `IErpApsOperationOrderBiz` mfg 建卡 SPI）——4/4 四要素齐备零登记缺陷；`grep -L @Locale` *Errors.java = 空；`erp-*-meta` + `_vfs/i18n` 零手改 | **pass** |

## 2. Finding 三态裁决（§2 跨轮查重全维强制）

> 查重源：r1 `docs/audits/check/ai-check-index.md` §报告清单/§Finding 追踪（同域报告 `ck-mfg-workorder.md` C4.1 全 21 条逐一比对）+ r2 只读目录 `2026-08-28-2049-ai-check-r2/` + §Mission 基线快照（checker 各命中均为已裁决偏离）。**本轮新立 2 条**（`-r3` 后缀）；历史 ID 零覆写。

### 2.1 复用（同型已 fixed，HEAD 复核有效）— 5 条

| 原 ID | 修复在位证据（T0 = HEAD `dd39e6cce`） |
| --- | --- |
| P0-CK-mfg-001（完工入库幂等键吞增量报工） | `completionMoveBillCode`（ErpMfgWorkOrderProcessor L431-451）首张保持 `wo.code` + 后续增量 `-C<累计完工量>` 后缀（累计量严格单调保证唯一）+ VARCHAR(50) 超长显式报错；`generateCompletionMove` L418 接线在位 |
| P1-CK-mfg-002（驳回/反审核后无法重提） | `doReject`/`doReverseApprove` 双轴联动回写 `docStatus=DRAFT`（L290-307，注释显式引用本 ID）+ 回归测试 `TestErpMfgReservationLifecycle#testRejectThenResubmit/#testReverseApproveThenResubmit` 在位；**其与 P2-CK-mfg-010 的叠加副作用 = 本轮 P1-CK-mfg-022-r3**（见 2.3） |
| P1-CK-mfg-003（领料红冲不回退 mfg 累计字段） | 镜像回退三件套（ErpMfgMaterialIssueReverseConfirmProcessor L52-59 + Abstract L168-193）：`rollbackWorkOrderLineActualQty` + `rollbackMaterialCostToWorkOrder`（REVERSAL 流水 \|totalCost\| 冲减）+ `unconsumeReservations` 在位 |
| P1-CK-mfg-004（完工入库静默缺失） | destWarehouseId/uomId 缺失不再静默 return——`LOG.error`（含 P1-CK-mfg-004 标记，facade L391-408）G3 温和方案在位（不阻断完工，与修复登记口径一致） |
| P1-CK-mfg-005（红冲失败吞异常推进终态） | 步骤 1/2 失败即抛 `NopException` 中止回滚整个 @BizMutation（ReverseConfirmProcessor L41-50 零 try/catch 吞咽 + javadoc L27-29 修复声明），状态保持 DONE+posted=true 可重试 |

### 2.2 归并（同型 open 追加证据至原 ID）— 16 条

| 原 ID | r3 复核证据（T0，原 ID 状态不动仍 open） |
| --- | --- |
| P2-CK-mfg-006 | WorkOrder/BomSnapshot 等实体 BizModel 仍裸 `CrudBizModel`（`defaultPrepare*` 计数=0） |
| P2-CK-mfg-007 | `KitAvailabilityChecker` 零 `orgId` filter（grep 零命中）+ Dashboard `orgId` 计数=0 原样 |
| P2-CK-mfg-008 | `getConsumption`/`.consumption` 运行时消费 grep 仍零命中 |
| P2-CK-mfg-009 | `assertCanCancel` 白名单仍 `{DRAFT, SUBMITTED, NOT_STARTED}`（DocumentStateMachine L133-142） |
| P2-CK-mfg-010 | 控制点仍在且**被 022-r3 实质加重**（见 2.3 归并注记）——本 ID 状态不动，升级后果由 022-r3 承载 |
| P2-CK-mfg-011 | F1.2 部分修复在位（reportCompletion L66-81 updateEntity+releaseRemainingReservations 前移至 generateCompletionMove 之前，注释显式引用）；残余归 F2.5 successor（与本计划基线口径一致） |
| P3-CK-mfg-012 | reportCompletion 负数仍静默置 ZERO（L34-36） |
| P3-CK-mfg-013 | `CONFIG_JOBCARD_AUTO_GENERATE_CRON` 死常量仍在（ErpMfgConstants L112） |
| P3-CK-mfg-014 | Dashboard 无界加载原样（computeOnTimeRate/sumCompletedQtyInRange/loadCompletedInRange/findDelayedWorkOrderAlert） |
| P3-CK-mfg-015 | `findWorkOrderIdsWithJobCards` 仍 `q.setLimit(workOrderIds.size())`（L287） |
| P3-CK-mfg-016 | `currentUserId` 宽 catch 返 null 无日志（facade L826-836）原样 |
| P3-CK-mfg-017 | `generatePendingJobCards` 单事务逐单吞异常原样 |
| P3-CK-mfg-018 | `isInspectionGated` `bomId==null` 恒 false（facade L468）原样 |
| P3-CK-mfg-019 | AUTO_UPGRADE 读侧仍从不读 `wo.getBomId()`（explodeRequirements AUTO_UPGRADE 分支）原样 |
| P3-CK-mfg-020 | `applyLaborCostToWorkOrder` 仍无工单终态守卫（ErpMfgJobCardProcessor L58-70，仅 null/signum 短路） |
| P3-CK-mfg-021 | 汇率仍 `BigDecimal.ONE`（ManufacturingIssuePostingDispatcher L125 + ProductionVarianceDispatcher L164）+ 贷方科目 `"1401"` 硬编码（L142）原样 |

### 2.3 新立 `-r3` — 2 条

**P1-CK-mfg-022-r3**（D3/D8；归并注记：P2-CK-mfg-010 同型控制点升级，原 ID 状态不动）

- **控制点**：`ErpMfgWorkOrderProcessor#doReverseApprove`（L299-307：P1-CK-mfg-002 修复后**无条件回写 `docStatus=DRAFT`**）× `#validateTransitionForReverseApprove`（L247-254：仅审批轴 `assertCanReverseApprove`，来源态判定仅 APPROVED，ErpMfgWorkOrderApprovalStateMachine L70-75）× xbiz `reverseApprove` mutation（仅 `ErpMfgWorkOrder:reverseApprove` 权限守卫）——reverseApprove 全链**无任何 docStatus 守卫**。
- **证据**：工单 approve 后 approveStatus 恒 APPROVED（IN_PROCESS/STOCK_RESERVED/STOPPED/COMPLETED/CLOSED 全程不翻审批轴）→ 对生产中/已完工/已关闭工单调 `ErpMfgWorkOrder__reverseApprove`：审批轴守卫通过 → `approveStatus=REJECTED` + 审计字段清空 + **`docStatus` 被强制回写 DRAFT 并持久化**。原 P2-CK-mfg-010 记录的缓解「终态复活被 `assertCanSubmit`（docStatus 仅 DRAFT）挡住」已失效——修复前终态单据只是双轴矛盾态，修复后终态单据被 reverseApprove 本身直接复活为 DRAFT 可编辑可重提态。回归测试 `testReverseApproveThenResubmit` 仅覆盖 approve→reverseApprove（NOT_STARTED）路径，终态组合零覆盖。r1 无此形态（该叠加系 P1-CK-mfg-002 修复 F2.5 批次 `1166339ae` 前后引入的新行为面）；r2 无同型。
- **问题**：COMPLETED/CLOSED 工单的 completedQuantity/成本/库存移动/凭证已按终态结转，被翻回 DRAFT 后可重提重审重开工——重复生产、重复入库、历史计价漂移；P1 判据「用户可见的正确性错误 + 强回归风险」成立（升级依据：原 P2 的「终态+REJECTED 矛盾态」可逆性弱，现「终态→DRAFT」是真实状态破坏）。
- **建议修复方向**：`validateTransitionForReverseApprove` 增加 docStatus 白名单（`NOT_STARTED`；或至少排除终态 {COMPLETED, CLOSED, CANCELLED} 与 IN_PROCESS/STOPPED/STOCK_*），或 `doReverseApprove` 回写 DRAFT 前校验 `docStatus ∈ {SUBMITTED, NOT_STARTED}`；补终态/在制组合负路径测试；修复时同步核 P2-CK-mfg3-012（委外同型，归 mfg-3 格）。

**P3-CK-mfg-023-r3**（DIM-F；新立，r1 明示未审 web 面、r2 无同型）

- **控制点**：`erp-mfg-web/.../pages/dashboard/main.page.yaml` L190-192（CRP 负荷图 data-source 参数 `dateFrom: "${filterForm?.dateFrom || null}"` / `dateTo: "${filterForm?.dateTo || null}"`）对照同文件 filterForm 表单字段（仅 `startDate`/`endDate`，L17-34）——参数名错配，日期区间过滤在该文件恒 null。
- **证据**：运行时面为孪生文件 `main.flux.yaml`（view-and-page-strategy L58「`*.flux.yaml` 双文件回退优先于 `*.page.yaml`」，其 L170-171 已正确绑定 `startDate`/`endDate`），故**当前零运行时影响**；但 `.page.yaml` 为 M0.4 登记手写页（row 76，MI.8 维护面），后续维护者按其结构改日期过滤将落空，且两文件已实际漂移。
- **问题**：遮蔽文件潜伏缺陷 + 孪生手写双文件无一致性校验通道。P3 定级（无即时行为影响）。
- **建议修复方向**：对齐 `main.page.yaml` 参数名为 `startDate`/`endDate`（随 M2.x 前端批顺带修）；或在 view-and-page-strategy 登记孪生文件「flux.yaml 为唯一权威、page.yaml 冻结」的维护裁决，防持续漂移。

### 2.4 归属标注（§3.2 共享代码边界）

- 完工入库移动消费侧（`generateCompletionMove` 幂等键/acctSchema 解析/移动单行构造）= **本格 mfg-1**；inventory `generateMove`/`findExisting` 幂等去重机制与 GL 过账引擎内部（REQUIRES_NEW、凭证平衡）**归属 fin-1** 归并，本格不重复立项（P0-CK-mfg-001 修复即消费侧唯一键细化范式，与引擎零耦合）。
- common 抽象族（`AbstractReverseApproveProcessor.illegalStatusException` 骨架通道、SoDGuard）调用点已审合规（ReverseApproveProcessor 直抛领域码覆写为有意契约修正，plan 2026-09-07-2200-1 form-3 登记）；基类行为缺陷归 U20（M1.15）。
- 聚合横切面（action-auth 聚合器/seed 全量装载/flux 导出门禁本体）归 U21（M1.16）；本格仅核 mfg 注册在位性（§1 矩阵 DIM-B/DIM-F 行）。
- JobCard TRANSFERRED 死状态 / MRP CANCELLED / forecast CONSUMED 等 Deferred 裁决（P1-MA2-035 族 + owner doc Deferred 标注）维持既有裁决不重开。

## 3. 统计

| 级别 | 本轮新立 | 复用（fixed 复核有效） | 归并（open 追加证据） |
| --- | --- | --- | --- |
| P0 | 0 | 1（mfg-001） | 0 |
| P1 | 1（mfg-022-r3） | 4（mfg-002..005） | 0 |
| P2 | 0 | 0 | 6（mfg-006..011） |
| P3 | 1（mfg-023-r3，DIM-F） | 0 | 10（mfg-012..021） |
| **合计** | **2** | **5** | **16** |

五格 verdict：DIM-B **finding**（P1-CK-mfg-022-r3）/ DIM-F **finding**（P3-CK-mfg-023-r3，minor）/ DIM-S **pass** / DIM-T **pass** / DIM-I **pass**。历史 21 条 r1 ID 状态零覆写（5 fixed 复核有效 + 16 open 追加证据）。

## 4. 剩余风险声明（横切关注点 13 完整四件套）

- **已查**：mfg-1 工单 Facade（837 行全读）+ ReportCompletion/ReverseConfirm/ReverseApprove per-mutation Processor 全读 + 双轴状态机 4 Bean 全读（Document/Approval 直抛领域码态）+ AbstractErpMfgMaterialIssueProcessor 关键区 + JobCard/调度建卡/看板/过账 dispatcher 抽查 + 机械程式全套实跑（checker 19 规则 / 反模式族 / codegen 安全 / 聚合完整性 / validate:flux / seed 门禁 / mfg 回归 / strict+self-test）；owner docs 3 份 × 9 断言抽样（state-machine 4 + material-reservation 3 + use-cases 2）；r1 21 条逐一比对裁决。
- **未深查（边界归属）**：mfg-2 面（BOM 展开/MRP/CRP/仿真引擎本体——BomExpander/CostRollupService/MrpEngine/SimulationMrpEngine，归 M1.6）；mfg-3 面（委外链/批次基因本体/差异计算器——Subcontract 族/BatchGenealogyWriter/ProductionVarianceCalculator，归 M1.7；P2-CK-mfg3-012 委外 reverseApprove 同型已登记其格）；`erp-mfg-web` 全量 view.xml 契约 drift（仅 mfg-1 面页面走查，全局面归 U21/M1.16）；posting 引擎内部（归 fin-1，M1.1 已收官）；测试代码仅作覆盖对账消费。
- **残留风险（登记不裁决）**：① 16 条归并 open finding 的修复归 M2.x（manufacturing P1 修复批及其后），其中 P1-CK-mfg-022-r3（终态复活）与 P2-CK-mfg-006（裸 CRUD 旁路，快照族 LOCK_AT_CREATION 语义）组合可放大（DRAFT 复活后经通用 update 改历史行），建议 M2.x 优先级排序参考；② P3-CK-mfg-023-r3 的孪生文件漂移面不限于 mfg（全仓 dashboard 手写双文件同构），是否升全局面裁决归 U21/M1.16；③ `validate:flux` 325 条 variant 漂移与 compliance 机器基线块差距均为批前在案外部事项（successor：`ai-check-r3-compliance-baseline-raise` / nop-chaos-flux dist 基线裁决），非本切片范围。
- **successor 触发条件**：M1.17 收官以覆盖矩阵完整性校验本报告 5/5 格；M2.x 修复以「先写失败测试」承接 §2.2/§2.3 open 项（022-r3 首补终态负路径测试）。
