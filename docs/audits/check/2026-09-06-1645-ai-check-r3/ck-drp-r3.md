# ck-drp-r3 — drp U19 五维符合性审计报告（ai-check-r3 M1.14）

> 工作项：M1.14（U12 + U13 + U18 + U17 + U19 各 × 五维全格，冻结清单 §4 映射表第 14 行；本报告 = U19 drp 格，其余四域格分别见 `ck-crm-r3.md` / `ck-cs-r3.md` / `ck-contract-r3.md` / `ck-b2b-r3.md`）。
> 执行日期：2026-09-09。审计时点 T0：HEAD `5eb2c4dbe9faa68a685b5a90728cc2493b477b76`（2026-09-09；计划基线 `2c1c1ef25` 后唯一推进 = 同批姊妹审计产物提交，生产代码零变化）；脏面 = 2 条 untracked 计划文件（本计划 + 姊妹 `0547-3`），tracked 零修改。
> 判定依据（冻结）：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md` §1/§2 + §3.3 U19 行 + §6 勘误 E1。执行期仅在报告与计划勾选注记追加证据，判定标准零改动。
> 范围：drp 全域（C 级，无切片细分）——净需求引擎（DrpEngine/DrpDemandAggregator/DrpReleaseService + Plan/Line 状态机 Bean 5/4 边）/补货（ErpDrpParameter + Plan/Line Processor 7 族 + release 链）/越库（ErpInvDrpCrossDock + CrossDockProcessor 六边矩阵 + CrossDockStagingTimeoutJob + inv_drp_* 跨域表）/安全库存（ErpInvDrpSafetyStockCalc/SafetyStockEngine 三级链 + confirmWriteback）/交期（ErpInvDrpLeadTimeRecord/LeadTimeProcessor 评分族）/仿真（Scenario/ScenarioVersion/ScenarioParam/SimulationDrpEngine/ParamResolver/VersionComparator）/月台与供应商评分（DockAppointment/SupplierScore）（`module-drp/erp-drp-{dao,service,web}` src/main）；owner docs `docs/design/drp/`（cross-dock/lead-time-tracking/safety-stock-optimization + state-machine/README/use-cases）。Skills：`nop-platform-conformance-audit-prompt` + `code-quality-audit-prompt`（DIM-B）；其余维 Skill: none。
> 切片边界（§3.2）：posting 引擎内部归 fin-1（drp 无过账面）；common 抽象族行为归 U20（11/11 BizModel 基类接入调用点合规；ErpCrudStatusLock 对 status 列惰性 = F1.3 注册边界，残余归 pur-003 族共性裁决）；聚合横切面归 U21（erp-drp-app action-auth x:extends 保留层注册在位；FNPT 缺口 drp-018 归并）；notify 消费点归 U11（drp-service 零 notify 消费点，grep 实证无消费点可记）。
> 零生产代码改动（roadmap 规则 6）：本报告 + 双索引 + 计划勾选注记为唯一产物（收官 `git status` 机械核证见 §4）。

## 1. 五维覆盖矩阵（5/5 完整）

| 维度 | 判定面（U19 全格） | 机械核查结果 | verdict |
| --- | --- | --- | --- |
| **DIM-B 后端平台合规** | §1.1 全套机械程式 + 15 维度走查（域焦点：净需求引擎/补货单生成/越库/安全库存；共性②⑦⑧⑨⑮）+ 维度⑮断言抽样 3 doc × 多断言交叉点 | 反模式族全零（RuntimeException=0/@Inject private=0/System.currentTimeMillis=0/@Transactional=0，job 的 LocalDateTime.ofInstant 非 now() 不违约定）；checker 19 规则 = M0.3 快照行零漂移；`__XGEN_FORCE_OVERRIDE__` 12 处全为 erp-drp-meta/erp-inv dict.yaml 校验点零手改（drp 字典跨 erp-drp/erp-inv 双命名空间为模型声明事实非缺陷）；聚合器含 `/erp/drp/auth/erp-drp.action-auth.xml`。IDaoProvider/IOrmTemplate 17 文件逐文件核验：DrpEngine/DemandAggregator/ReleaseService/SafetyStockEngine 4 核心豁免注释齐备（「非 BizModel 服务助手对齐 MrpEngine 范式」「I*Biz 以订单头为粒度不便行级聚合」「偏离计划 Task Route 登记」「跨域只读 inv 明示」），LeadTimeProcessor 跨域读经 I*Biz + matrix §2.4 登记注记，唯 StagingTimeoutJob 3 处跨域只读 daoFor（:180/:186/:198）无注释 = P3-CK-drp-024-r3。15/15 无跳维：①引擎 Java 化有 MRP-范式豁免 pass；②跨域写经 `IErpInvStockMoveBiz.generateMove`（Job:165）+ notGenCode 6 外部实体生成产物**零表名双重拼接**（`_app.orm.xml` grep `erp_md_erp|erp_inv_erp` = 0，lesson 01 pass）；③④⑤机械全零 pass；⑥11/11 AbstractErpCrudBizModel pass；⑦机制 B 6 实体 + Maven 边 pur→drp-dao 已登记（cross-dock.md D1 裁决）pass；⑧**finding**——Plan/Line 状态轴 SM Bean 声明式治理合规；dict 死值：plan/line/simulation 三 dict 全值有 writer ✓，**drp-xdock-status PENDING 无生产 writer**（链入口断链 = **新立 P2-CK-drp-021-r3**）、**drp-replenishment-method MIN_MAX/PERIODIC 死值**（mandatory 参数列三选一但引擎仅隐式 LOT_FOR_LOT，decideReplenishmentType/roundToMultiple 零消费）= **新立 P3-CK-drp-022-r3**、simulation-param-type LEAD_TIME 死值 = drp-019 归并；⑨job 双层门控同键 + invoker 接线完整 pass；⑩matchingStrategy 列 A 类授权双批准注记 pass；⑪无 tenantId 预置 pass；⑫12 测试类含 Engine/SafetyStock/Simulation/CrossDock/SM 族 pass；⑬codegen 安全 pass；⑭erp-drp-app 聚合器保留层注册 pass；⑮3 doc × 多断言漂移 5（2 新立 + 3 归并，8 一致） | **finding**（4 新立：1 P2 + 3 P3；复用 2 + 归并 18，见 §2） |
| **DIM-F 前端页面与 E2E** | §1.2 全套 + DRP 计划页面 | `npm run validate:flux`：FLUX_PAGE_ERROR_COUNT 0 / 999 页（erp 855）；整体 exit 1 = 325 ERR 全 variant 族既有外部漂移（drp 命中 7 条同族不立项）；`component="AMIS"` 0、ORM `ext:web-renderer="flux"` 无缺失。页面面 24 page.yaml + 1 flux.yaml 全量清点（M0.4 重放零漂移）：11 实体 main+picker codegen stub 族 22 页全 PASS（保留层继承、`git status module-drp` 零脏面）；手写页仅 dashboard/net-requirement 孪生（`@query:ErpDrpLine__findNetReqGroups` 自定义 @BizQuery + `ErpDrpPlan__get` REST /r/，i18nEn 18/18）；`graphql:` 3 处全 labelProp；docStatus 死状态样式分支 0 命中。**事实记录**：无独立仿真页（runSimulation 经 mutation，E2E `drp-simulation.action.spec.ts` GraphQL 层覆盖）——非违规。E2E：E2E_ENGINE 缺省 flux、drp 5 business-actions spec selector 纪律 0 命中、页面级 GraphQL 断言 0。r1 drp 族无 DIM-F finding；drp-018 FNPT 缺口原站点归并 open 不重开（DIM-F 复核同证：手写 action-auth FNPT 仍 = 0 vs 生成基线 22） | **pass**（归并态注记） |
| **DIM-S seed 数据** | §1.3 全套 + drp 11 表 seed（M1.5 批含 inv_drp_* 跨域表） | `TestErpSeedDataIntegrity` **4/0/0/0 全绿**；`_init-data` porcelain 空（零 seed 变更）；资产清点 **372 CSV + 1 SQL** = 冻结登记值；drp deploy `_seed_*.sql` 命中 = 0（登记处一致）；erp_drp_* 6 + erp_inv_drp_* 5 = **11 CSV** 精确在册（跨域表 inv_drp_* 命名空间承载合规）；越库跨域表 FK 链抽查：XDK-SEED-2026-001（COMPLETED，PURCHASE_RECEIPT/PR-SEED-REF-001→SALES_DELIVERY/SD-SEED-REF-001，MATERIAL_ID=1，PRE_ALLOCATED，MATCHED/LOADED 时间序自洽）+ #002 CANCELLED 终态自洽；safety_stock_calc/lead_time_record 行与 material/warehouse 引用完整；drp 非过账域 posted 列零命中 N/A 带理由 | **pass** |
| **DIM-T 单元测试** | §1.4 全套 + P2 DRP 清单行覆盖 | `mvn test -pl module-drp/erp-drp-service` **98/0/0/0 全绿 BUILD SUCCESS**（= 锚点 98 零增量）；覆盖对账：注解动作 24 / BizModel 11 vs `_cases` 资产根 12（Engine/CrossDock/LeadTimeStats/InventoryIntegration/ScheduleRelease/PlanCrudSmoke/ForecastSource/FkNameLoader/SM Delta+IoC 族）；**P2 行在位**：`TestErpDrpEngine`（runDrp 净需求链）+ PlanStateMachine 族 ✓；`SnapshotTest.RECORDING` = 0 | **pass** |
| **DIM-I i18n（MI 先行时序口径）** | §1.5 全套（仅零回归 + 白名单合规，不产同类 finding） | `--strict` **PASS exit 0** + `--self-test` **PASS**；drp 探针族（CAT-1 2）维持清零零回归；WHITELIST drp 条目 **0**（记录条目数 0，与计划基线一致——MI.6 批 2 drp 0 无批内面）；`grep -L @Locale` = 空；meta/i18n 零手改 | **pass** |

## 2. Finding 三态裁决（§2 跨轮查重全维强制）

> 查重源：r1 `ai-check-index.md` §Finding 追踪（同域报告 `ck-drp.md` C7.4 全 20 条逐一比对）+ r2 只读目录（无 drp 同型新独立登记）+ §Mission 基线快照。**本轮新立 4 条**（1 P2 + 3 P3）；历史 20 ID 零覆写（实仓核对 2026-09-09：r1 族最大号 CK-drp-020，新立自 021 起）。

### 2.1 复用（同型已 fixed，HEAD 复核有效）— 2 条

| 原 ID | 修复在位证据（T0） |
| --- | --- |
| P2-CK-drp-012（nextVersionNo 升序撞 UK，同型 P1-CK-mfg2-003 族） | `SimulationDrpEngine:257-266` 已改 `addOrderField("versionNo", true)` DESC + 修复注释「P2-CK-drp-012 修复（同型 P1-CK-mfg2-003）」在位；`TestErpDrpSimulation#testThirdVersionRunSucceeds` 红→绿回归在 98 套件绿内（mfg2-003 平台签名复核 `true`=DESC 佐证同族） |
| P2-CK-drp-014（CRUD update 无已审守卫，同型 P1-CK-pur-003 族） | F1.3 统一基类接入在位：11/11 BizModel extends `AbstractErpCrudBizModel`。残余注记：drp 全部实体用 `status` 列（非 posted/approveStatus），守卫对本域惰性 = F1.3 修复证据自登记的设计边界（「激活由列存在性决定」，ErpCrudStatusLock.java:26-41 实测）——ORDERED 复活/overrideSafetyStock 越权直写残余面归 P1-CK-pur-003 族共性裁决（U20 格），本格不重复立项 |

### 2.2 归并（同型 open 追加证据至原 ID）— 18 条

> 18 条 open r1 ID 于 T0 全量逐条现症复核在位（域走查逐条 file:line 证据），原 ID 状态不动仍 open。

| 原 ID | r3 复核证据（T0） |
| --- | --- |
| P1-CK-drp-001 | DrpEngine:85-89 公式未变（currentStock 取 available + allocatedQty 加回 reserved 双计）+ Aggregator:176-199 available/reserved 同读原样 |
| P1-CK-drp-002 | Aggregator:215-216 仍仅 ne(docStatus,CANCELLED)，DONE 完成调拨不过滤原样 |
| P1-CK-drp-003 | Aggregator:236-242 头查询仍无 warehouseId/orgId；行级 `quantity−receivedQuantity` 扣减为新缓解证据（部分收货面），主缺陷在位——**现症收窄注记** |
| P2-CK-drp-004 | DrpEngine:148 lineNo=10 起 + :165-173 clearSuggestedLines 仅删 SUGGESTED（state-machine.md §2 L38「清除计算结果明细行」断言漂移同点）+ :157 totalReplenishmentQty=null 原样 |
| P2-CK-drp-005 | ErpDrpLineBizModel:66-77 approveLine 无 approvedQty 回填（对照 approvePlan Processor:58-60 有回填不对称）原样 |
| P2-CK-drp-006 | DrpEngine:84 `nz(param.getSafetyStock())` 直读 + findEffectiveSafetyStock 生产零消费（safety-stock-optimization.md §业务规则 1 L194 断言漂移同点）原样 |
| P2-CK-drp-007 | Aggregator:83-95 逐参数 5 查询 N+1 原样 |
| P2-CK-drp-008 | Aggregator:176-260 四族查询无 orgId 原样 |
| P2-CK-drp-009 | SafetyStockEngine:303-316 monthlyDemands 无零需求月日历填充（safety-stock-optimization.md §数据清洗 L119 断言漂移同点）原样 |
| P2-CK-drp-010 | SimulationDrpEngine:100+ 仍落真实 ErpDrpPlan(COMPUTED)+SUGGESTED 行原样 |
| P2-CK-drp-011 | ErpDrpSimulationParamResolver:84 invalidateCache 生产调用 0（grep 实证）原样 |
| P2-CK-drp-013 | orm:221-234 Parameter 仅普通索引 + SS Calc UK 仅 (code,orgId) orm:269-271 原样 |
| P3-CK-drp-015 | Processor:33-34 死读 auto-writeback + ErpDrpConfigs:19-20 horizon 默认 30 vs README:120 声明 90（README §配置点断言漂移同点）原样 |
| P3-CK-drp-016 | SafetyStockEngine:342-351 零提前期兜底 0 + LEAD_TIME_INVALID 零使用原样 |
| P3-CK-drp-017 | approvePlan Processor:55-62 零行放行 + cancelLine:44-46 无 advance 调用原样 |
| P3-CK-drp-018 | 手写 erp-drp.action-auth.xml FNPT = 0 vs 生成基线 22（DIM-F 复核同证）原样 |
| P3-CK-drp-019 | runSimulation 仅消费 SAFETY_STOCK/REPLENISHMENT_QTY（SimulationDrpEngine:109-110），LEAD_TIME 变体零消费原样 |
| P3-CK-drp-020 | requireScenario/requireVersion（SimulationDrpEngine:272-287）/requireVersion（Comparator:88-93）/requireCalc（SS Engine:378-389）not-found 误码族原样 |

### 2.3 新立 `-r3` — 4 条（1 P2 + 3 P3）

**P2-CK-drp-021-r3**（DIM-B ⑧/B2+B3 链入口断链）
- **控制点**：全仓生产代码零 `ErpInvDrpCrossDock` PENDING writer（grep `setStatus.*XDOCK_STATUS_PENDING` 仅守卫/查询；`ErpInvDrpCrossDockProcessor.java:482` dao 无 newEntity 创建点；DrpReleaseService Non-Goal 自认越库释放独立面；b2b grep 零命中排除外域写入）。
- **问题**：`cross-dock.md` §预分配流程 L96 声明「DRP 释放时生成 ErpInvDrpCrossDock（PENDING）」未实现——方式 2 ASN 与方式 3 均无自动创建路径 → `markReceivedFromPurchase`（按 PUR_ORDER+sourceBillCode 匹配 PENDING）与 StagingTimeoutJob 的输入链**入口断链**：除非用户手工经 CRUD 创建带精确 sourceBillCode 的 PENDING 记录，收货驱动越库链永远空转（xdock-enabled=false 默认下无感知）。维度⑮ cross-dock.md §预分配流程断言漂移同点。
- **三态裁决**：新立（r1 drp 族无越库链入口 finding——020 条集中于引擎口径与释放面）。P2（特性级链路断链，默认门控下无症状；启用即失效）。
- **修复方向**：三选一——DrpReleaseService 释放时按需创建 PENDING（落地 owner doc 方式 1）；或 purchase 收货侧 create-if-absent；或 owner doc 实现注记裁决「PENDING 仅经 CRUD 手工创建」并登记链路前置条件。修复阶段先写「释放→收货→越库匹配」失败测试。

**P3-CK-drp-022-r3**（DIM-B ⑧/D4，drp-019 同族）
- **控制点**：`ErpDrpConstants.java:28-30` 常量齐 + orm:201 `replenishmentMethod` mandatory + orm:202-204 min/max/reviewPeriod 列齐 + DrpEngine `decideReplenishmentType`/`roundToMultiple` 零消费（grep 全 service 零业务引用）。
- **问题**：drp-replenishment-method dict MIN_MAX/PERIODIC 死值——参数必填列三选一，但引擎仅隐式 LOT_FOR_LOT 语义；用户配置 MIN_MAX（min/max 水位）或 PERIODIC（审视周期）**静默无效**。同族 drp-019（simulation LEAD_TIME 死值）但位于主参数实体且 mandatory，用户误导面更大。
- **三态裁决**：新立（r1 无 replenishment-method 轴）。P3。
- **修复方向**：runDrp 接入 MIN_MAX（net = maxStock−currentStock）与 PERIODIC（reviewPeriod 取整）语义；或 dict 缩减为 LOT_FOR_LOT + owner doc 登记 Deferred。

**P3-CK-drp-023-r3**（DIM-B ⑮/D5 ORM 真相源漂移）
- **控制点**：`orm:373` `<to-one name="dock" refEntityName="...ErpInvDrpCrossDock">`（dockId 关系错挂越库实体本体）+ `orm:361` status 列无 ext:dict——vs `cross-dock.md` §月台预约 L139「dockId → ErpMdWarehouseLocation where type=DOCK」+「status dict：AVAILABLE/BOOKED/ARRIVED/COMPLETED/CANCELLED」。
- **问题**：月台预约为 Non-Goal 未实现，运行时影响零；但 ORM 是持久化真相源——dock 关系错挂实体在启用时产出错误关联，status 列缺 5 值 dict 声明。维度⑮断言漂移同点。
- **三态裁决**：新立。P3（Non-Goal 面的模型真相源缺陷，行为零影响）。
- **修复方向**：ORM 修正 dock refEntityName + 补 dict（保护区 A 类授权双批准）；或与 owner doc 对齐登记 Non-Goal 裁决注记。

**P3-CK-drp-024-r3**（DIM-B ②）
- **控制点**：`ErpDrpCrossDockStagingTimeoutJob.java:180/186/198`——3 处跨域只读 daoFor（ErpMdLocation/ErpInvStockMove/ErpMdMaterial）无豁免注释（同文件 IOrmTemplate session/reload 用法有 javadoc 理由，对照 SafetyStockEngine/DrpEngine 先例范式不对称）。
- **三态裁决**：新立（r1 无此族；事实理由存在——md 实体无 purpose-built I*Biz 读接口，仅缺注释声明）。P3。
- **修复方向**：类/方法 javadoc 补一行豁免理由（对齐 DrpEngine 先例形态）。

### 2.4 归属标注（§3.2 共享代码边界 + 跨域横切）

- posting 引擎：drp 无过账消费点，不适用。
- common 抽象族调用点合规（11/11 基类接入）；基类行为（status 列惰性边界）归 U20——drp 全实体 status 列惰性为跨域共性族（crm docStatus 族同型）最强样本，已注入 pur-003 族残余注记。
- 聚合横切面归 U21：erp-drp-app 聚合器保留层注册在位；drp-018 FNPT 缺口维持原 ID（跨域家族 ct 站点 = P3-CK-ct-030-r3 本轮新立）。
- notify：drp-service 零消费点（grep 实证），无记录项。
- 跨切片注记：023-r3 ORM 真相源漂移涉保护区（dock 关系修正走双批准）；021-r3 修复裁决若走 b2b 收货侧联动则牵 b2b 格（M1.14 本轮已闭合，跨切片注记归 M2.x 统一批次）。

### 2.5 维度⑮断言抽样记录（3 doc 主抽 + README/lead-time 交叉点，漂移 5 / 一致 8）

state-machine.md 5 断言（APPROVED→DRAFT 清行语义 → **漂移** = drp-004 / EXECUTED 唯一终态 ✓ / APPROVED→EXECUTED 前置 ✓ / §6 审批角色计划主管 → **漂移**（角色模型无代码载体，关联 drp-018 FNPT 面）/ 乐观锁并发 ✓）；safety-stock-optimization.md 5 断言（Z 值表 ✓ / 三级优先链 → **漂移** = drp-006 / 零需求月保留 → **漂移** = drp-009 / 人工确认回写 ✓（config 死读归 drp-015）/ 联合变分公式+样本门槛 ✓）；cross-dock.md 4 断言（超时自动转正常入库 ✓ / 六边矩阵含 PENDING→MATCHED 直连边 ✓ / §预分配 PENDING 生成 → **漂移** = **021-r3 新立** / §月台预约 dockId+status dict → **漂移** = **023-r3 新立**）；交叉点 lead-time-tracking.md 2 断言（权重 40/30/20/10 + 等级阈值 ✓ / 联合变异公式 ✓）+ README 2 断言（净需求公式逐字 ✓（currentStock 语义问题归 drp-001）/ horizon 默认 90 → **漂移** = drp-015）。漂移 ≥2 扩样条款已履行。

## 3. 统计

| 级别 | 本轮新立 | 复用（fixed 复核有效） | 归并（open 追加证据） |
| --- | --- | --- | --- |
| P0 | 0 | 0 | 0 |
| P1 | 0 | 0 | 3（drp-001/002/003） |
| P2 | 1（drp-021-r3 越库链入口断链） | 2（drp-012 + drp-014） | 9（drp-004/005/006/007/008/009/010/011/013） |
| P3 | 3（drp-022/023/024-r3） | 0 | 6（drp-015/016/017/018/019/020） |
| **合计** | **4** | **2** | **18** |

> 计数精确对账：r1 全 20 条 = 复用 2（012/014）+ 归并 18（P1×3 + P2×9 + P3×6）✓（P2 open 实列 = 004/005/006/007/008/009/010/011/013 共 9 条；P3 open 实列 = 015/016/017/018/019/020 共 6 条）；新立 4 = P2×1 + P3×3。

五格 verdict：DIM-B **finding**（4 新立）/ DIM-F **pass**（归并态注记）/ DIM-S **pass** / DIM-T **pass** / DIM-I **pass**。历史 20 条 r1 ID 状态零覆写（2 fixed 复核有效 + 18 open 追加证据）。

## 4. 剩余风险声明（横切关注点 13 完整四件套）

- **已查**：erp-drp-{dao,service} 全部 processor（7 族 + CrossDock/LeadTime 双实体 Processor）+ 11 BizModel + 2 状态机 Bean（5/4 边）+ DrpEngine/DemandAggregator/ReleaseService/SafetyStockEngine/SimulationDrpEngine 5 引擎 + ParamResolver/VersionComparator + StagingTimeoutJob + ErpDrpErrors/Constants/Configs + 机械程式全套实跑（checker/反模式/codegen/聚合 E1/validate:flux/seed 门禁/drp 回归 98/strict+self-test/notGenCode 表名双重拼接专项）；owner docs 3 doc 主抽 + 2 交叉点 × 18 检查点；r1 20 条全量逐条复核；r2 目录核对；drp seed 11 CSV 越库跨域表 FK 链抽查；执行者对 021-r3 零 writer 断言全域 grep 复核。
- **未深查（边界归属）**：`AbstractErpCrudBizModel`/ErpCrudStatusLock 基类内部（归 U20，status 列惰性残余已注入共性族注记）；`erp-drp-web` 渲染时行为（静态 + 门禁，浏览器回归归看板专项）；inv 侧被写实体（generateMove 消费语义归 inv 格 M1.13 已闭合）；月台预约/供应商评分 Non-Goal 面（仅 023-r3 模型真相源缺陷登记）。
- **残留风险（登记不裁决）**：① 18 条归并 open 修复归 M2.x，P1 三条（可用量双计/在途不过滤 DONE/跨仓在途污染）建议最优先——净需求口径系统性失真直接决定补货量正确性；② P2-CK-drp-021-r3 与 xdock-enabled 门控联动——默认关无症状，启用即空转，建议与 b2b 收货侧/owner doc 裁决三选一后同批落地；③ status 列守卫惰性共性族（drp 11 实体全样本 + crm docStatus 族）建议 U20 格统一裁决 ErpCrudStatusLock 列扩展机制；④ 022-r3 replenishment-method 死值与 drp-019 simulation LEAD_TIME 死值同批收口（dict 收窄 or 引擎接入二选一）。
- **successor 触发条件**：M1.17 收官以覆盖矩阵完整性校验本报告 5/5 格；M2.x 修复以「先写失败测试」承接 open 项；越库启用（xdock-enabled=true）投产前 021-r3 必须修复或裁决。
