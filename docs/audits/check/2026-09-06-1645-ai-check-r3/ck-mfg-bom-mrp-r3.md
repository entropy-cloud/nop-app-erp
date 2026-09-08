# ck-mfg-bom-mrp-r3 — manufacturing「BOM/MRP/CRP」mfg-2 五维符合性审计报告（ai-check-r3 M1.6）

> 工作项：M1.6（U08 × 五维 × mfg-2，冻结清单 §4 映射表第 6 行 / §3.3 U08 行；BOM 展开消费交点 = `KitAvailabilityChecker.explodeRequirements` 参数消费面，齐套/预留/工单面归 mfg-1；委外释放/批次基因/差异公式归 mfg-3；posting 引擎内部归属 fin-1，§3.2）。
> 执行日期：2026-09-09。审计时点 T0：HEAD `7ad2e426b`（姊妹 M1.4 计划落盘提交）；脏面 = **本批 3 份 `2026-09-09-0232-*` 计划文件（untracked，草案产物）**——本计划（-1）+ M1.7 mfg-3（-2）+ M1.9 ast-2（-3），姊妹在制会话为同批并行起草，披露不阻塞；全部证据引用该时点。
> 判定依据（冻结）：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md` §1/§2（判定标准/机械核查程式/跨轮查重）+ §3.3 U08 行 + §6 勘误 E1。执行期仅在本报告与计划勾选注记追加证据，判定标准零改动。
> 范围：BOM 结构与展开（ErpMfgBom 族 + 展开消费交点）/工艺路由/MRP 引擎（MrpEngine + SimulationMrpEngine 净额归集/低阶码）/MRP 计划单生成/CRP 负荷链（crp_load）（`module-manufacturing/erp-mfg-{dao,service,web}` src/main）；owner docs `mrp.md`（120 行）+ `crp.md`（135 行）+ `bom-and-routing.md`（187 行）+ `simulation-engine.md`（323 行）+ `use-cases.md`。Skills：`nop-platform-conformance-audit-prompt` + `code-quality-audit-prompt`（DIM-B）；其余维 Skill: none。
> 零生产代码改动（roadmap 规则 6）：本报告 + 双索引 + 计划勾选注记为唯一产物（收官 `git status` 机械核证见 §4）。

## 1. 五维覆盖矩阵（5/5 完整）

| 维度 | 判定面（mfg-2 增量） | 机械核查结果 | verdict |
| --- | --- | --- | --- |
| **DIM-B 后端平台合规** | §1.1 全套机械程式 + 15 维度走查（重点：MRP 净额归集/低阶码——F2.6 修复 HEAD 复核、仿真版本语义、CRP 负荷链口径、BOM 展开消费交点）+ 维度⑮断言抽样 4 doc × 7 断言 | 反模式族 mfg dao+service 全零（`extends RuntimeException`=0 / `@Inject private`=0 / `System.currentTimeMillis`=0；`@Transactional∩@BizMutation` 7 文件命中均为 mfg-1/mfg-3/posting 族 javadoc 注记，mfg-2 范围=0）；checker 19 规则逐项 = M0.3 快照行零漂移（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42）；`IDaoProvider/IOrmTemplate` mfg-2 命中：核心服务助手 7 文件豁免注释齐备（BomExpander L44-45/MrpEngine L53-54/DemandAggregator L47-48/CostRollupService L61-69 E3.2 架构豁免/CrpLoadCalculator L67-68/MrpReleaseService L45-54 O-4/KitChecker L35），弱覆盖站点→新立 P3-CK-mfg2-026-r3；IOrmTemplate 仅 flushSession（CostRollupBizModel/MrpReleaseService flushReleaseOrThrow 既有范式）；`__XGEN_FORCE_OVERRIDE__` 20 处全为 erp-mfg-meta codegen dict.yaml 只读校验点零手改；聚合完整性（E1 勘误路径 `nop/main/auth/app.action-auth.xml` 多行 x:extends）含 `mfg/auth/erp-mfg.action-auth.xml` + 保留层文件在位；F2.6 三修复 HEAD 复核全部有效（001 skipAvailable+不消耗 / 002 availableConsumed map 双引擎 / 003 nextVersionNo DESC——`QueryBean.addOrderField(name, boolean desc)` 平台签名实核 `true`=DESC，mfg+drp 孪生修复在位）；P2-CK-mfg2-012 F1.3 修复复核 = 19 个 mfg-2 实体 BizModel 全部接入 `AbstractErpCrudBizModel` 守卫基类；⑧ MrpPlanStateMachine 3 边声明式 + CANCELLED 预留死状态（owner doc Deferred）+ null 初始态 run 守卫文档化；⑥⑨ R6.2 per-mutation Processor 全套（RunMrp/RunSimulation/Promote/RollupCost/CalculateLoad/3×Release）+ config-gate + NopException 领域码；**⑨⑭ FNPT 注册面缺口**（8 个自定义 mutation 零注册）→ 新立 P3-CK-mfg2-024-r3；⑮ 4 doc × 7 断言：mrp.md L92 低层码机制 = F2.6 修正声明与实现一致、crp.md L79/L128-129 APS 双源 SPI + L100-101 cron 门控语义一致、simulation-engine.md L96 4 维公式 = P2-CK-mfg2-006 既有裁决、bom-and-routing.md L64 use_multi_level_bom = P2-CK-mfg2-009 既有裁决；漂移 1 处（mrp.md L95 委外释放 stale 声明）→ 新立 P3-CK-mfg2-025-r3，<2 未触发全量扩样（实际已遍及 4 doc）；涉 posting 引擎内部（MfgPostingExecutor @Transactional 等）标注归属 fin-1 未走查内部 | **finding**（新立 3 P3 + 归并 19 + 复用 4，见 §2） |
| **DIM-F 前端页面与 E2E** | §1.2 全套 + mfg-2 面 = BOM 树看板 bom-tree（M1.5 邻接承接）/MRP 计划·需求·预测/仿真场景·版本·参数/BOM·路由·工作中心·生产版本/成本卷算实体页 + crp-load-report 报表页 | `npm run validate:flux` 双数字对账：`FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855` + `files=855 validated=855 errors=325`——325 条逐条同型（`variant:"primary"`×dropdown-button 枚举漂移），mfg 域命中 32 文件 34 条（= M1.5 记录的「mfg 34 条同族成员」精确一致，bom-tree/MrpScenario 族页面不在 ERR 内），successor 在案（roadmap MI.8 done 行）非本切片 finding；`component="AMIS"` 保留层 0、ORM `ext:web-renderer="flux"` 缺失 0；bom-tree 运行时面 `bom-tree.flux.yaml`（flux 原生 tree + `@query:ErpMfgBom__findBomTree` REST + i18nEn 全承载）合规——孪生遮蔽文件 `bom-tree.page.yaml` 漂移（`ErpMfgBom__explode` 扁平端点 + `childrenKey: children` 嵌套键错配）→ **归并 P3-CK-mfg-023-r3 追加站点证据**（运行时零影响，flux.yaml 权威）；crp-load-report.page.yaml `@query:ErpMfgReport__renderHtml` + `/p/ErpMfgReport__download` 参数三处一致 + i18nEn 合规；MRP/仿真/路由/BOM/CostRollup 实体页 = M0.4 GenPage stub（源头 view.xml `i18n-en` 承载，源头链查表 row 61-62/73-79）；view.xml `graphql` 命中 = `graphql:labelProp` 平台 meta 键非 API 调用；E2E：mfg spec 5 个（含 mfg-2 面 `mfg-mrp-simulation.action.spec.ts`）违规选择器=0，`E2E_ENGINE` 缺省 flux（engine.ts L8-9 `return 'flux'`），spec 内 GraphQL 命中为 runbook L229 业务动作套件登记的 API 驱动型数据层通道（L122 flux 页面 GraphQL 断言禁令不适用） | **finding**（归并态 0 新立，见 §2.4） |
| **DIM-S seed 数据** | §1.3 全套 + bom/mrp_plan/crp_load/routing 自洽 + workcenter 配置链 + 双层门控默认关 | `TestErpSeedDataIntegrity` **4/0/0/0 全绿**（BUILD SUCCESS，全表 findAll + to-one 零悬空 + 零孤儿 CSV + scope-pinning 门控）；资产清点 **372 CSV + 1 SQL**（= 冻结口径「M1.5 批次后」登记值）；`git status --porcelain _init-data/` 空（零 seed 变更，快照重录义务未触发）；deploy `_seed_*.sql` 全仓仅 cs/notify 两族（= seed-data.md §97 登记处已聚合，mfg 无 deploy seed 零同步义务、无第三态）；mfg-2 seed 自洽抽查：`erp_mfg_bom.csv` isDefault=false 干扰面裁决（plan 2026-09-01-1245-2）+ PHANTOM isActive=false N-DIS 负例 + bom_line→material/uom/operation FK 全实存（机器门禁背书）、`erp_mfg_mrp_plan.csv` DRAFT/COMPLETED/FIRMED 三态 + `-PROMOTED-2` 后缀与 promoteToFormalPlan 代码约定一致、`erp_mfg_crp_load.csv` workcenterId=1→WC-001 实存 + workOrderId=1→WO-2026-001 实存（= M1.5 同行种子）、routing/workcenter/capacity 配置链在位（0628-1 口径，efficiencyFactor=1）；双层门控默认关：crp-run-cron 空=跳过 + simulation-enabled false + subcontract-release false + overhead-allocation false（`application.yaml` 零 erp-mfg 覆盖，代码默认值治理）——seed 静态行不被引擎重算覆盖 | **pass** |
| **DIM-T 单元测试** | §1.4 全套 + MRP 计算→计划单生成→CRP 负荷链清单行 | `mvn test -pl module-manufacturing/erp-mfg-service` 三轮实跑：第一轮 **308/0/0/0 全绿**（= M1.5 执行参照值，零回归；全仓聚合对照面 = known-good-baselines 2026-09-08 MI 终态行 4006/0/0/1）；第二轮 1 Error——`TestErpMfgWorkOrderEndToEnd#testEndToEndIssueReportCompletion` 快照 `@var` 精度竞态（`ErpInvStockMove@updateTime_5` 变量值 `.66` 厘秒截断 vs 响应实际值 `.661` 毫秒全精度 check-match-fail，时间边界随机暴露）；隔离复跑 4/0/0/0 绿证实 flaky（非生产代码回归——本切片零生产代码改动）；第三轮全量复跑 **308/0/0/0 全绿**；flaky 本体 → 新立 P3-CK-mfg2-028-r3（mfg-1 E2E 测试资产归属注记）；覆盖对账（mfg-2 自定义动作 14 个）：runMrp↔TestErpMfgMrpEngine 8 测试 + MrpEndToEnd、explode/findDefaultBom↔TestErpMfgBomExplosion、rollupCost↔TestErpMfgCostRollup 9 测试（含 GraphQL wiring + E3.2 豁免不变量 + ReadPathMasking）、runSimulation/promoteToFormalPlan/compareVersions↔TestErpMfgMrpSimulation 9 测试（config-gate/覆盖回退序/第三版运行/结构化 diff）、calculateLoad/getLoadReport↔TestErpMfgCrpLoad 7 + CrpLoadSource 3（双源/回退/清快照/超载门控）、release×3↔MrpEndToEnd#testMrpRunAndReleaseEndToEnd + 并发同行释放 + 缺 supplier 拒绝 + TestErpMfgSubcontracting；关键业务流 MRP 计算→计划单生成→CRP 负荷链逐段覆盖在位（链路断点本体 = P2-CK-mfg2-005 open 承载，不重复立项）；**findBomTree 零测试断言**（栈算法重建嵌套未测）→ 新立 P3-CK-mfg2-027-r3；`SnapshotTest.RECORDING`=0 残留；mfg-2 测试文件 `*` 通配/delVersion 零命中（框架自动屏蔽 createTime/updateTime 合规） | **finding**（2 新立 P3，见 §2.3） |
| **DIM-I i18n（MI 先行时序口径）** | §1.5 全套（仅零回归 + 白名单合规，不产同类 finding） | `--strict` **PASS exit 0**（0 new violations vs 冻结 SNAPSHOT；170 baseline files，totals CAT1..4=0/0/209/1318 为基线记录值，现树实测 0/0/0/0 与 report mode 一致）；`--self-test` **PASS**；mfg 探针族（CAT-1 31/CAT-2 9）维持清零零回归（CrpLoadCalculator/CrpRunJob/MrpReleaseService 新近触点 LOG 全英文实核）；WHITELIST mfg 文件条目 1 条（ErpMfgDashboardBizModel）+ 按消费链扩样共抽 4 条（mfg Dashboard/inv/pur Dashboard 同簇 + aps `IErpApsOperationOrderBiz` mfg 建卡 SPI）——4/4 四要素齐备（路径/cats/理由/owner doc + 裁决来源 plan 2026-09-07-1715-1）零登记缺陷；`grep -L @Locale` *Errors.java = 空（ErpMfgErrors 实核 1 处声明）；`erp-*-meta` + `_vfs/i18n` 零手改 | **pass** |

## 2. Finding 三态裁决（§2 跨轮查重全维强制）

> 查重源：r1 `docs/audits/check/ck-mfg-bom-mrp.md`（23 条：0 P0 / 3 P1 / 9 P2 / 11 P3；跨轮索引 4 fixed / 19 open）逐条比对 + r2 只读目录 `2026-08-28-2049-ai-check-r2/`（mfg FNPT 族零命中）+ `ai-check-index.md` §Mission 基线快照（checker 各命中均为已裁决偏离）。**本轮新立 5 条**（`P3-CK-mfg2-{024..028}-r3`，自 024 起接续）；历史 ID 零覆写。

### 2.1 复用（同型已 fixed，HEAD 复核有效）— 4 条

| 原 ID | 修复在位证据（T0 = HEAD `7ad2e426b`） |
| --- | --- |
| P1-CK-mfg2-001（SAFETY_STOCK 可用量双扣） | `MrpEngine` L97-107/L127-140 + `SimulationMrpEngine` L150-162/L326-339：`skipAvailable` 分支 SAFETY_STOCK 净缺口直作净需求且不消耗 available；注释显式引用本 ID；回归测试 `testSafetyStockNetNotDoubleDeducted`（safety=100/avail=99→net=1）在套件 308 绿内 |
| P1-CK-mfg2-002（无低阶码净额归集低估） | 双引擎 `availableConsumed` map（MrpEngine L99/L132-136、Sim L152/L331-338）：run 内 per-material 已消耗累计、共享子件仅扣一次；`testSharedComponentAvailableConsumedOnce`（A+B 共享 C avail=50→Σplanned=150）在位；mrp.md L92 低层码声明已按 F2.6 修正一致 |
| P1-CK-mfg2-003（nextVersionNo ASC 撞 UK） | `SimulationMrpEngine.nextVersionNo` L505-516 `addOrderField("versionNo", true)` + setLimit(1) 取最大 +1——**平台签名实核**：`QueryBean.addOrderField(String name, boolean desc)`（nop-api-core 2.0.0-SNAPSHOT sources L435）`true`=DESC，注释与实现一致；`testThirdVersionRunSucceeds`（v1→v2→v3）在套件绿内；drp 孪生修复（P2-CK-drp-012）同型同参在位 |
| P2-CK-mfg2-012（20 个裸 CrudBizModel 无守卫） | F1.3 统一基类接入自动生效：19 个 mfg-2 实体 BizModel（Bom 族 4/Mrp 族 6/CostRollup 族 2/CrpLoad/Routing 族 2/ProductionVersion/Workcenter 族 3）全部 `extends AbstractErpCrudBizModel`（grep 实核清单见计划勾选注记），状态锁守卫经基类激活 |

### 2.2 归并（同型 open 追加证据至原 ID）— 19 条

| 原 ID | r3 复核证据（T0，原 ID 状态不动仍 open） |
| --- | --- |
| P2-CK-mfg2-004 | DemandAggregator 预测行仍忽略 warehouseId（L43/L156-157/L197 产品级契约声明原样）；mrp/simulation 双引擎零过滤原样——契约分歧（MRP 过滤 vs 契约改声明）仍待 owner doc 裁决 |
| P2-CK-mfg2-005 | `MrpReleaseService.releaseToWorkOrder` L171-190 仍不写 routingId/plannedEndDate；`CrpLoadCalculator.findWorkOrdersInWindow` L364-370 `ge("plannedEndDate",…)` 仍排除 NULL 完工日工单 + `distributeByWorkOrder` L258-261 仍要求 routingId 非空——链路断点三站点原样 |
| P2-CK-mfg2-006 | `SimulationVersionComparator.lookupStandardCost` L169-176 仍硬编码 ZERO（ successor 注释在位：「successor 接入采购价或标准成本时填充」） |
| P2-CK-mfg2-007 | `indexLines` L142-163 对 session 托管实体直接写值仍原样（L157-158 setNetRequirement/setPlannedQuantity） |
| P2-CK-mfg2-008 | cost-rollup-status FIRMED 仍零命名 writer（全 service grep 仅读侧 `ProductionVarianceCalculator` L351 + `ErpMfgCostRollupBizModel` STATUS_FIRMED 常量读 + masking 查询；DRAFT/CANCELLED 死值原样） |
| P2-CK-mfg2-009 | `ErpMfgBom.useMultiLevelBom` 列仍零消费（引擎双处 `explode(bom.getId(), planned, false)` 硬编码 + KitChecker `explodeRequirements` 硬编码 true） |
| P2-CK-mfg2-010 | `BomExpander.divide` L227-232 + `CostRollupService.divide` L358-363 除数为零仍静默返 ZERO（qty=0 BOM 全链静默归零原样） |
| P2-CK-mfg2-011 | `topDemandsByMaterial` 同物料多需求日仍取最晚 requirementDate（MrpEngine L263-265 + Sim L478-480 `isAfter` 才覆盖） |
| P3-CK-mfg2-013 | N+1/全表原样：物料全表扫描（DemandAggregator L126 + Sim applySafetyStockOverride L285）、销售订单全载+逐单调行 L94-99、CRP `workcenterCodes` L479 全表内存过滤、`findLatestFirmedStandardCost` FIRMED 头全载+逐头查行（CostRollupBizModel L52-70）；Calendar 扫描带类 D 裁决 limit 5000（L426-432，既有裁决不重开） |
| P3-CK-mfg2-014 | mrp-order-type 字典 PLANNED_ORDER 死值 + SUBCONTRACT_REQUEST 引擎零产出仍原样（双引擎 L145-147/L344-346 仅产 WORK_ORDER_REQUEST/PURCHASE_REQUEST） |
| P3-CK-mfg2-015 | `ErpMfgCrpRunJob.execute` L50-55 仍 catch(Exception)→LOG.error 无告警、直调 Biz 无事务包裹（清区间+重写非原子）原样 |
| P3-CK-mfg2-016 | CRP 产能三近似原样：`efficiencyByWorkcenter` L458-461 仍取 per-WC 最大效率、`availableHours` L487-502 重叠班次仍双计、`shiftHours` L518-533 解析异常仍静默 0 产能 |
| P3-CK-mfg2-017 | `releasePurchaseRequest`/`releaseSubcontractRequest` 仍仅守卫 supplierId（L73-76/L108-111），currencyId null 仍直达必填列原始 DB 错误 |
| P3-CK-mfg2-018 | `overheadAllocationRate` L247-258 解析失败仍静默 0；`aggregateSubcontractCost` L265-288 委外口径仍 = 整单加工费合计 / 本物料行数量合计（分子分母错配原样） |
| P3-CK-mfg2-019 | orgId 条件过滤原样（plan.orgId=null 时需求/库存/CRP 全程无组织维度，跨组织合并边界未裁决） |
| P3-CK-mfg2-020 | 错误码语义漂移原样（requireScenario 不存在→ERR_MFG_SIMULATION_NO_BASELINE_PLAN L519-521、requireVersion 不存在→ERR_..._ALREADY_PROMOTED L531-537）；**新站点追加**：`requireComparable` L138 `ARG_EXPECTED_STATUS` 参数承载 versionId（诊断参数语义错位，同族同型证据） |
| P3-CK-mfg2-021 | MRP 销售订单需求源仍仅排除 CANCELLED（DemandAggregator L90），DRAFT/未审批单仍入毛需求 |
| P3-CK-mfg2-022 | code 后缀拼接仍无长度守卫（Sim L123 `-SIM-V{n}`、L213-215 `-PROMOTED-{n}`）；e2e spec 头注释以「baseline ≤ 32 char」人工约束规避（mfg-mrp-simulation.action.spec.ts L40-42），系统性守卫仍缺 |
| P3-CK-mfg2-023 | `CostRollupService.createHead` L105-115 仍不写 orgId、code=`ROLLUP-{date}-{bomId}` 同 BOM 同日重复卷算仍无去重/废止 |

### 2.3 新立 `-r3` — 5 条

**P3-CK-mfg2-024-r3**（DIM-B 维度⑨/⑭ action-auth 注册面；同族 P3-CK-fin4-023-r3 / P3-CK-prj-022-r3，跨单元新站点 → 新立）

- **控制点**：`module-manufacturing/erp-mfg-web/.../erp/mfg/auth/erp-mfg.action-auth.xml`（203 行全读）——FNPT 资源仅登记 mfg-1 面（ErpMfgWorkOrder start/approve/reverseApprove/close/cancel + ErpMfgMaterialIssue approve/reverseApprove）与 mfg-3 面（ErpMfgSubcontractOrder approve/reverseApprove）。
- **证据**：mfg-2 全部 8 个自定义 mutation 零 FNPT/权限树注册：`ErpMfgBom:rollupCost`、`ErpMfgMrpPlan:runMrp`、`ErpMfgMrpPlanLine:releasePurchaseRequest/releaseWorkRequest/releaseSubcontractRequest`、`ErpMfgMrpScenario:runSimulation/promoteToFormalPlan`、`ErpMfgCrpLoad:calculateLoad`（BizModel @BizMutation 实核 + auth 文件 grep 零命中）。菜单树 mfg-mrp/mfg-mrp-simulation/mfg-cost 分组仅挂实体 main 页面，未挂动作节点。
- **问题**：动作未经 FNPT 注册无法按角色细粒度授权（生产计划员/生产主管对 runMrp/release/runSimulation 的职责分离不可配置），与 fin-4/prj 已登记同族缺口一致。P3 定级（与 fin4-023-r3 同级一致性）。
- **建议修复方向**：M2.x 前端/权限批为 8 动作补 FNPT 注册（runMrp/release 族→生产计划员、rollupCost/calculateLoad→生产主管或系统角色、runSimulation/promote→生产主管）；或按 fin-4 先例登记全局 FNPT 补录 successor。

**P3-CK-mfg2-025-r3**（DIM-B 维度⑮ owner-doc 漂移；单处漂移 <2 未触发全量扩样，实际已遍及 4 doc）

- **控制点**：`docs/design/manufacturing/mrp.md` L95 实现约定条目「**委外建议释放**：orderType=SUBCONTRACT_REQUEST 字典存在但委外流程独立面，本期不支持释放。」
- **证据**：`MrpReleaseService.releaseSubcontractRequest`（L103-118）+ `ErpMfgMrpPlanLineReleaseSubcontractRequestProcessor` + `IErpMfgMrpPlanLineBiz.releaseSubcontractRequest` @BizMutation 已落地（plan 2026-07-13-0455-1 §Phase 4，config-gated `erp-mfg.subcontract-release-enabled` 默认 false）——「本期不支持释放」与实仓 API 面不符（引擎零产出 SUBCONTRACT_REQUEST 行是另一回事 = P3-CK-mfg2-014，释放动作对手工行可达）。附带同文件 L98 行号引用 stale（`markFirmed:129-133`→实际 L134-138；`advancePlanToFirmedIfComplete:218-236`→实际 L226-250；机制语义一致，仅行号漂移）。
- **问题**：owner doc stale 断言误导后续维护者（误判释放面不存在）。P3 定级（对齐 pur-016-r3/fin3-18-r3 文档漂移先例）。
- **建议修复方向**：mrp.md L95 改写为「释放已支持（config-gated 默认关；引擎零产出该类型行，见 014）」并同步 L98 行号引用。

**P3-CK-mfg2-026-r3**（DIM-B 维度② 直 DAO 豁免注释缺失；同族 P3-CK-fin3-017-r3 / P3-CK-fin4-022-r3）

- **控制点**：`SimulationVersionComparator` L169-176 `lookupStandardCost` 经 IDaoProvider 跨域直读 `ErpMdMaterial` 无豁免注释（该文件唯一跨域站点）。
- **证据**：mfg-2 核心服务助手 7 文件均有显式范式/豁免注释（BomExpander/MrpEngine/DemandAggregator/CostRollupService E3.2 架构豁免/CrpLoadCalculator/MrpReleaseService O-4/KitAvailabilityChecker）；弱覆盖：`SimulationMrpEngine`（跨域读经「fork 对齐 MrpEngine」间接引用，无本文件直接注释）、`SimulationVersionComparator`（零注释）、`ErpMfgSimulationParamResolver`（同域）、`ErpMfgMrpPlanRunMrpProcessor`/3×Release Processor（同域薄委派，Processor 层范式注记在家族基类）。
- **问题**：跨域直读站点缺本地豁免理由，后续维护者无法就地判别合规性。P3 定级（对齐 fin3/fin4 同族）。
- **建议修复方向**：M2.x 批量补 2 行注释（VersionComparator + SimulationMrpEngine 类头），或登记「mfg 服务助手族豁免注释以家族头注记承载」裁决。

**P3-CK-mfg2-027-r3**（DIM-T 覆盖缺口）

- **控制点**：`ErpMfgBomBizModel.findBomTree`（@BizQuery，L71-109 栈算法重建嵌套树）零测试断言（src/test + _cases 全 grep 零命中）。
- **证据**：testing-strategy 覆盖要求表「BizModel 每公开方法 ≥1 测试」；mfg-2 其余 13 个自定义动作均有落点测试（§1 DIM-T 行对账），仅 findBomTree 缺位；其核心依赖 `BomExpander.explode` 已由 TestErpMfgBomExplosion 覆盖，未覆盖的是 pre-order DFS→嵌套重建的栈算法（level 回溯/根判定）。
- **问题**：BOM 树看板（dashboard/bom-tree 页）后端聚合逻辑无回归保护。P3 定级（只读可视化助手、核心展开已覆盖，业务关键度低）。
- **建议修复方向**：M2.x 补 1 个快照/断言测试（3 层嵌套 + phantom 合并 + 多根边界）。

**P3-CK-mfg2-028-r3**（DIM-T 快照纪律——测试资产时序 flaky；站点物理在 mfg-1 E2E 测试资产 `TestErpMfgWorkOrderEndToEnd`，M1.5 已闭包，由本切片 DIM-T 回归门禁第二轮随机暴露并登记承接，不阻塞本切片第三轮全绿）

- **控制点**：`TestErpMfgWorkOrderEndToEnd#testEndToEndIssueReportCompletion` 的 `3_report_completion_response.json5` 快照——`@var` 变量 `ErpInvStockMove@updateTime_5`（厘秒精度 `.66`）被后续输出以毫秒全精度（`.661`）断言，`nop.err.match.not-equals-var-value` 间歇失败。
- **证据**：本切片三轮实跑——第一轮 308 全绿（03:23）；第二轮 1 Error（03:36，`data.updateTime` 值 `2026-09-09 03:36:55.661` vs 变量 `2026-09-09 03:36:55.66`）；隔离复跑该类 4/0/0/0 绿；第三轮全量 308/0/0/0 绿。当 updateTime 毫秒第三位非 0 时变量（厘秒截断）与输出（全精度）必然失配——时间边界随机 flaky，CI 噪音源。框架 CREATE_TIME/UPDATE_TIME 自动屏蔽不覆盖显式 `@var` 绑定面。
- **问题**：测试资产 volatile 字段屏蔽缺口（显式 @var 绑定绕过框架自动屏蔽且精度不一致）。P3 定级（无生产行为影响，间歇 CI 红）。
- **建议修复方向**：M2.x 测试批：快照屏蔽或录制值改 `*` 通配（对齐 createTime/updateTime 框架屏蔽语义），或统一两侧输出精度。

### 2.4 归并站点追加（跨切片原 ID）— 1 条

- **P3-CK-mfg-023-r3**（mfg-1 格 DIM-F finding，open）：新站点 `dashboard/bom-tree.page.yaml` 孪生遮蔽文件漂移——`.flux.yaml`（运行时权威）调 `@query:ErpMfgBom__findBomTree` 嵌套树端点，`.page.yaml`（遮蔽）调 `ErpMfgBom__explode` 扁平端点且 `childrenKey: children` 与扁平节点结构错配（树控件层级失真）。运行时零影响（flux.yaml 优先），与原 ID「孪生手写双文件无一致性校验通道」同型同族；原 ID 证据扩员（main.page.yaml 日期参数错配 + bom-tree.page.yaml 端点/结构错配），修复方向同原 ID（M2.x 前端批对齐或登记孪生冻结裁决）。本格 verdict 按计划承接 M1.5 邻接记录落此。

### 2.5 归属标注（§3.2 共享代码边界）

- BOM 展开消费交点（`KitAvailabilityChecker.explodeRequirements` 参数消费面：快照感知/AUTO_UPGRADE/硬编码多级）= 本格 mfg-2 审计；齐套判定/预留/工单面本体归 mfg-1（M1.5 已审），不重复立项。
- 委外释放下游（`releaseToSubcontractOrder` 生成的委外单审批/领料/批次面）、`ProductionVarianceCalculator`/CostVariance 族/`CostBandClassifier` 归 mfg-3（M1.7 同批）；`releaseSubcontractRequest` 消费入口的 currencyId/supplierId 守卫缺口已在本格 P3-CK-mfg2-017 归并证据内。
- posting 引擎内部（`MfgPostingExecutor` @Transactional、dispatcher 族内部）**归属 fin-1**（M1.1 已收官），本格仅核调用点合规（R6=2 基线条目均为已裁决偏离）。
- common 抽象族（`AbstractErpCrudBizModel` 守卫基类行为、`AbstractProcessor.illegal*`）调用点已审合规（012 复用即基类接入证据）；基类行为缺陷归 U20（M1.15）。
- 聚合横切面（action-auth 聚合器本体/seed 全量装载/flux 导出门禁本体）归 U21（M1.16）；本格仅核 mfg 注册在位性 + FNPT 域内注册面（024-r3 为域内 action-auth 内容缺陷，非聚合机制缺陷）。

## 3. 统计

| 级别 | 本轮新立 | 复用（fixed 复核有效） | 归并（open 追加证据） |
| --- | --- | --- | --- |
| P0 | 0 | 0 | 0 |
| P1 | 0 | 3（mfg2-001..003） | 0 |
| P2 | 0 | 1（mfg2-012） | 8（mfg2-004..011） |
| P3 | 5（mfg2-024..028-r3） | 0 | 11（mfg2-013..023）+ 跨切片站点追加 1（mfg-023-r3） |
| **合计** | **5** | **4** | **19 + 1** |

五格 verdict：DIM-B **finding**（3 新立 P3：024/025/026）/ DIM-F **finding**（归并态 0 新立：mfg-023-r3 站点追加）/ DIM-S **pass** / DIM-T **finding**（2 新立 P3：027/028）/ DIM-I **pass**。历史 23 条 r1 mfg2 ID 状态零覆写（4 fixed 复核有效 + 19 open 追加证据，open 现状与跨轮索引 4 fixed/19 open 完全对账）。

## 4. 剩余风险声明（横切关注点 13 完整四件套）

- **已查**：mfg-2 引擎族全读（MrpEngine 304 行/DemandAggregator 299/SimulationMrpEngine 554/SimulationVersionComparator 181/ParamResolver 85/BomExpander 237/CostRollupService 387/CrpLoadCalculator 602/CrpCapacityProvider 41/CrpRunJob 79/KitAvailabilityChecker 消费交点段）+ 6 per-mutation Processor + MrpPlanStateMachine + 6 BizModel 动作面 + auth 文件 203 行全读 + bom-tree 孪生/crp 报表页全读 + 机械程式全套实跑（checker 19 规则 / 反模式族 / codegen 安全 / 聚合完整性 / validate:flux 双数字 / seed 门禁 + 自洽抽查 / mfg 回归 308 两轮 / strict+self-test）；owner docs 4 份 × 7 断言抽样（mrp 3 + crp 2 + bom-and-routing 1 + simulation-engine 1）；r1 23 条逐一比对裁决 + r2 目录 FNPT 族查重。
- **未深查（边界归属）**：委外链/批次基因/差异公式本体（归 mfg-3，M1.7 同批；releaseSubcontractOrder 持久化面仅审入口守卫）；posting 引擎内部（归 fin-1 已收官）；工单/领料/预留面（归 mfg-1 已审）；`erp-mfg-web` 全量 view.xml 契约 drift（仅 mfg-2 面页面走查，全局面归 U21/M1.16）；forecast 状态机/工时/作业卡域内面不在 mfg-2 焦点；测试代码仅作覆盖对账消费。
- **残留风险（登记不裁决）**：① 19 条归并 open finding 修复归 M2.x（manufacturing P1 修复批及其后）；其中 P2-CK-mfg2-005（MRP→CRP 链断）与 P2-CK-mfg2-012 已修守卫组合后，建议 M2.x 优先排 005（释放工单补 routingId/plannedEndDate 即可让 CRP 链生效，改动面小收益大）；② P3-CK-mfg2-024-r3 FNPT 缺口为跨域同族第三站点（fin4/prj/mfg2），建议 U21/M1.16 全局面盘点时升格为横切批次修复；③ P3-CK-mfg2-025-r3 揭示 mrp.md 维护滞后于 2026-07-13 落地批，mrp.md 其余行号引用存在同类漂移风险（本轮仅登记不强扩）；④ P3-CK-mfg2-028-r3 快照 `@var` 厘秒/毫秒精度竞态为间歇 CI 红噪音源（本切片三轮 1 暴露 2 全绿），修复前同族 E2E 复跑策略应含「红即隔离复跑裁决」；⑤ validate:flux 325 variant 漂移与 compliance 机器基线块差距均为批前在案外部事项（successor：`ai-check-r3-compliance-baseline-raise` / nop-chaos-flux dist 基线裁决），非本切片范围。
- **successor 触发条件**：M1.17 收官以覆盖矩阵完整性校验本报告 5/5 格；M2.x 修复以「先写失败测试」承接 §2.2/§2.3 open 项（024-r3 首补 FNPT 注册清单、027-r3 首补 findBomTree 测试）；mrp.md L95/L98 修正随 M2.x 文档批。
