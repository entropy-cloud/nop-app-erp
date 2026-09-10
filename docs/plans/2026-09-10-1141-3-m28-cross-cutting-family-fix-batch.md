---
status: active
mission: ai-check-r3
work-item: M2.8
group: "2026-09-10-1141"
verify: [test]
---

# 2026-09-10-1141-3 M2.8 五维横切族修复（A/B/C/D/E/F 六族 46 ID + doc 批 15 ID，共 61 ID，五分片按裁决 §4 执行序）

## Current Baseline

- 批内面 = M2.0 族裁决（`docs/audits/check/2026-09-06-1645-ai-check-r3/m2-0-family-adjudication.md`）路由到族批的 **61 ID** = 族批 46（A daoFor 豁免注释/I*Biz 族 14 + B FNPT 注册族 12 + C ctx 兜底族 2 + D DIM-T 覆盖缺口/测试卫生族 11 + E DIM-F 死状态样式/孪生族 5 + F 基类契约族 2）+ doc 批 15（owner-doc 漂移族，挂 M2.8 内独立分片⑤——裁决 §2.7）。**本文为其唯一消费入口**（裁决头部声明），§1 全量映射 + §2 逐族四路裁决 + §4 切分建议直接约束本计划 Phase 1~5。
- 范围外显式声明：dict 值域族 4 ID（ct-025/drp-022/b2b-018/prj-024）归 M2.7（裁决 §1/§2.8/§5 一致），本计划不承载；裁决 §4 分片⑥行系与 M2.7 的协调注记（ORM 保护区双批准流与分片①并行先行启动），非本计划执行面。逐条 deferred 4 ID（b2b-020/cs-024/qa-031/qa-033-r3）不重开。
- 关键既有裁决与约束（§2 逐族，执行面直接受控）：A 族二选一（跨域直查站点优先 I*Biz 注入镜像 RecallTargetLocator 范式；豁免 javadoc 仅限 session 语义依赖/只读聚合等显式理由，对齐 DrpEngine/CarryForwardProcessor 先例措辞）；B 族按 FNPT 既有范式（closePeriod/WorkOrder approve/Salary:markPaid 先例）逐域补 action-auth 注册且 roles 对齐各域 useCases，app-001-r3 八扩展域保留层补 `x:extends` 继承行并**先写 enforcement-on 权限点断言失败测试**；r1 同族站点（drp-018/b2b-011）不入本批（横切关注点 5）；**enable-action-auth 翻 true 前族 B 12 ID 必须全部 fixed**（触发登记，本批收官项核注）；C 族镜像 ExpenseCostAggregator:182-185 兜底范式、job 入口逐站点豁免裁决、exemplar = prj-023-r3、common-015-r3 为聚合追踪点；D 族每缺口 ≥1 行为断言测试（最低断言强度按 owner doc §五步流程第 2 步），ast2-026-r3 与 M2.4 已修 ast2-024-r3 语义联动（plan `2026-09-10-0705-2` 已完成，测试断言其修复后语义）；E 族双载体权威裁决（**flux.yaml 为唯一运行时权威，page.yaml 冻结为回退产物**）须成文 `docs/architecture/view-and-page-strategy.md`（M2.7 log-012 按该裁决执行注册切换——两计划互引，M2.9 对账），mfg-023 遮蔽文件按裁决冻结不修或对齐参数二选一登记；F 族两处基类单点修复全族生效，defaultPrepareSave override 改变全部实体 __save 行为——失败测试先行 + 全 reactor 回归 + immutable 全拒语义与既有 F1.3 测试对账；doc 批判定口径 = 文档一致性走查（非代码修复），修订不改需求契约段语义，md-018（三死配置键落地或 doc 降级）与 ct-029（MOCK provider 切换路径）含产品语义微裁决逐项登记（doc 降级为默认路径，键落地走后续需求通道）。
- 修复方法约束（r3 M2 前言 + M2.0 owner doc `docs/architecture/finding-remediation-method.md`，其为本计划直接 Prereq）：行为类 finding 先红后绿（F 族 __save 拒 posted、common-013 契约负例、D 族测试即产物、app-001 enforcement-on 断言）；机械类修复以分片验收 grep/checker 断言为红绿等价物（MI 脚本红线范式，§4 验收断言列为准）；证伪路径 = 书面 not-a-problem 登记，两态必居其一。
- 保护区守门：本批零 ORM/api.xml 变更预期（A 族注释/注入、B 族 action-auth/view 继承、E 族页面 yaml、doc 批均非 ORM 面）；执行中发现需触 ORM 站点即停走 dual-agent-approval；seed 联动默认零触碰（`_init-data/` 不动），D 族测试用 case 级 fixture。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-09 行（全 reactor 4006/0/0/1，41 含测试模块聚合口径；执行期以最新登记行为权威，姊妹计划已披露增量允许并披露）；compliance checker 机器块 R2b=242/R2c=1542/R12a=71（R2c 实测 1543 的 +1 漂移已登记 successor baseline-raise 裁决在案；B 族 action-auth 注册与 x:extends 行不属 R 规则计数面，若执行中 checker 漂移即停走独立基线裁决）；CJK `--strict` PASS（0/0/0/0）；`npm run validate:flux` 对照面 = 999 页 0 error + 325 条既有 variant 漂移（本批 E 族页面修改不得新增错误）。模块测试参照（M1 锚点）：fin 533 / mfg 308 / hr 249 / ast 341 / qa 184 / crm 188 / ct 168 / cs 185 / prj 179 / mnt 157 / aps 82 / md 160 / drp 98 / b2b 80 / log 66 / notify 23 / common 23 / app-erp-all 70（scoped）/ 全 reactor 4006。
- 剩余差距：61 ID 族回填面全部未动；E 族权威裁决未成文；D 族 11 缺口零测试；M2.9 收官需 61 条 fixed/not-a-problem 证据指针与分片验收断言归零记录。
- 依赖状态：M2.0（plan `2026-09-10-0425-1`）已完成；M2.4（plan `2026-09-10-0705-2`，ast2-024-r3）已完成——D 族 ast2-026-r3 联动前置成立。本计划组 `2026-09-10-1141` 内执行序 N=3。组内文件面：与 M2.6（qa 两站点）零重叠；与 M2.7 唯一交点 = E 族 view-and-page-strategy.md 成文（本计划）↔ log-012 注册切换（M2.7 侧，按已冻结裁决执行）——已按互引注记消解；B 族 qa 片触 `erp-qa.action-auth.xml` 与 M2.6 qa 业务站点零重叠。

## Goals

- 分片①（序①）族 B：11 域自定义 mutation 补 FNPT action-auth 注册（roles 对齐 useCases）+ app-001-r3 八扩展域保留层 `x:extends` 继承行（enforcement-on 断言测试先行）；收官核注「enable-action-auth 翻 true 前族 B 12 ID 全 fixed」触发登记。
- 分片②（序②）族 A：14 ID 全量回填（I*Biz 注入优先 / 豁免注释显式理由二选一），全仓「daoFor 命中处无注释」扫描归零，U20（common-014-r3）聚合对账销账。
- 分片③（序③）族 E+F+C：E 族死状态样式分支归零 + 双载体权威裁决成文 view-and-page-strategy.md；F 族基类契约单点修复（__save 拒 posted 测试 + 期望态串双 varargs）；C 族 ctx 兜底全量回填（裸 new 计数对账归零）。
- 分片④（序④）族 D：11 缺口每缺口 ≥1 行为断言测试 + 测试卫生亚型清理（mnt-022 孤儿 _cases 删除、mfg2-028 @var 精度统一）。
- 分片⑤（序⑤）doc 批：15 漂移站点一次修订收敛（文档一致性走查口径），md-018/ct-029 微裁决逐项登记。
- 零回归 + 门控持平：触域模块套件全绿 + 全 reactor `mvn test` 零新增失败；compliance/cjk 双 checker + validate:flux 不高于基线；seed 零改动证明。
- M2.9 消费证据落盘：61 ID 逐条 fixed/not-a-problem 证据指针 + 五分片验收断言归零记录 + E 族裁决成文指针 + enable-action-auth 触发登记核注。

## Non-Goals

- 不修 dict 族 4 ID（ct-025/drp-022/b2b-018/prj-024——M2.7 承载）与各域批独立项 24 ID 减本批外的全部他批面；不修 r1/r2 通道 finding（drp-018/b2b-011 FNPT 站点等——仅范式互链注记）。
- 不重开逐条 deferred 4 ID（b2b-020/cs-024/qa-031/qa-033-r3）与既有 lesson 裁决（lesson 09 吞异常语义、lesson 10 dict 死状态既有裁决不重开）。
- 不将「enable-action-auth 翻 true」设为本批完成门槛（裁决 §2.2 明示否决——lesson 14 三源核对，缺省配置合法 opt-in）；不新增 roadmap 工作项（doc 批挂 M2.8 语义承载）。
- 不做双索引状态回填（M2.9 义务）；不做 roadmap 状态翻转；不改 ORM/api.xml（发现需触即停走保护区流）；不重构各域业务编排结构（族回填以范式最小面落地）。

## Phase 1 — 分片① 族 B FNPT 注册 + enforcement 前置（12 ID）

> 统一类型：Proof | Add（2 Proof + 12 Add，按域分片）。
> Skill: nop-backend-dev（action-auth/FNPT 范式 + 反模式自检）+ nop-testing（enforcement-on 断言）
> Targets: 各域 `erp-<short>-web/src/main/resources/.../auth/<domain>.action-auth.xml`（或域内既有 auth 文件路径）+ 八扩展域保留层 view 继承文件；逐域 mutation 清单以裁决 §1 B 族行 + 各 ck 报告为准
> Prereqs: M2.0 计划完成

- [x] <Proof> app-001-r3 enforcement-on 权限点断言失败测试先行（八扩展域保留层零 x:extends 生成层继承 → enforcement-on 下权限面缺口复现），执行确认红，失败输出记入勾选注记。
      - Skill: nop-testing
      - 证据：`app-erp-all/src/test/java/io/nop/app/all/auth/TestExtensionDomainAuthInheritance.java`（本批新增）修复前红——8 域逐域断言失败输出（crm 70/cs 36/hr 72/aps 14/ct 30/drp 22/log 16/b2b 26 个生成层 FNPT 权限点全量脱链 + 各域未显式 remove test-orm 根，surefire `app-001-r3 enforcement 权限面缺口…expected: <true> but was: <false>`）；补 `x:extends` + remove 后 3/3 绿（`Tests run: 3, Failures: 0, Errors: 0`）。
- [x] <Add> fin 片：fin4-023-r3（13 mutation）按 closePeriod 先例补注册。
      - Skill: nop-backend-dev
      - 证据：`erp-fin.action-auth.xml` 新增 14 节点（preCheck query + finalizePeriod/openPeriod/generateNextYearPeriods/importStatement/autoMatch/manualMatch/generate/post/reverse/generateEliminationCandidates/postElimination/runMatching/checkDualSideConsistency），roles=财务员，对账 grep 归零（28 rows）。
- [x] <Add> mfg 两片：mfg2-024-r3（8 mutation）+ mfg3-020-r3（8 mutation）。
      - Skill: nop-backend-dev
      - 证据：`erp-mfg.action-auth.xml` 新增 16 节点（Bom:rollupCost/MrpPlan:runMrp/MrpPlanLine release×3/MrpScenario runSimulation+promoteToFormalPlan/CrpLoad:calculateLoad + SubcontractOrder cancel/issueMaterials/receiveFinished/postProcessingFee/reverseCompletion/CostVariance:calculateVariances/Forecast approve+cancel），runMrp/release 族→生产计划员、rollupCost/calculateLoad/runSimulation/promote/reverseCompletion→生产主管、Forecast→生产计划员，对账 grep 归零。
- [x] <Add> hr 片：hr2-028-r3（≈42 mutation）。
      - Skill: nop-backend-dev
      - 证据：`erp-hr.action-auth.xml` 新增 43 节点（Attendance 4/Timesheet 3/Leave 3/Salary 3/Simulation 7/Shift 3+Rotation 1/Assignment 3+Swap 4/Survey 3/Response 1/Result 1/DevPlan 3/Gap 2/Assessment 2），普通动作→HR 专员、薪酬敏感（calculateSalary/runPayroll/generateBankFile）→薪酬审批人，48 rows 对账 grep 归零。
- [x] <Add> prj 片：prj-022-r3（4 实体零 auth）。
      - Skill: nop-backend-dev
      - 证据：4 实体保留层 xbiz 补增量 `<mutation><auth/>` 声明（Settlement 7/Timesheet 4/Project 7/Task 4，镜像 ErpSalOrder 范式：approve/reject→approve、reverseSettlement→reverseApprove、生命周期→mutation；逻辑仍走 Java，xbiz 仅权限覆盖），模块测试 179/0/0/0 绿。
- [x] <Add> qa 片：qa-029-r3（NCR xbiz 空 `<actions/>` 补显式 auth）。
      - Skill: nop-backend-dev
      - 证据：`ErpQaNonConformance.xbiz` 补 postNcr→mutation/reverseNcr→reverseApprove/resolve→mutation 三显式 auth 声明（镜像 Recall 范式），模块测试 188/0/0/0 绿。
- [x] <Add> ct 片：ct-030-r3（12 mutation）。
      - Skill: nop-backend-dev
      - 证据：`erp-ct.action-auth.xml` 新增 12 节点（Contract submit/amend/suspend/resume/expire/rejectAmend→合同专员、terminate/approveTermination/rejectTermination→合同审批人；InvoicePlan:triggerInvoice/RebateSettlement:postSettlement/RebateAgreement:runAccrual→财务员），20 rows 对账归零。
- [x] <Add> mnt 片：mnt-023-r3（16 mutation）。
      - Skill: nop-backend-dev
      - 证据：`erp-mnt.action-auth.xml` 新增 18 节点（Equipment 3/Schedule 1/Visit 5/Request 5/DowntimeEntry 2/SparePartUsage 2；按裁决全量补齐代码面 18 个未注册 mutation ≥ finding 计数 16），维护主管/维护人员分工、reverseConfirm→管理员，22 rows 对账归零。
- [x] <Add> aps 片：aps-015-r3（18 mutation + 3 query）。
      - Skill: nop-backend-dev
      - 证据：`erp-aps.action-auth.xml` 新增 24 节点（OperationOrder 16 mutation + 3 query/Schedule 2/AtpCtpService 3 query），roles=生产计划员（seed 无「排产员」角色，按既有角色语义对齐），24 rows 对账归零。
- [x] <Add> log 片：log-015-r3（10 mutation）。
      - Skill: nop-backend-dev
      - 证据：`erp-log.action-auth.xml` 新增 10 节点（Shipment 6 + DeliveryBooking 4，Booking 无独立菜单页挂 Shipment-main 下——对齐 ct ContractVersion 挂 Contract-main 先例），业务动作→库管员,销售员、webhook/轮询入口→管理员（seed 无「物流员」角色），10 rows 对账归零。
- [x] <Add> md 片：md-020-r3（8 mutation）。
      - Skill: nop-backend-dev
      - 证据：`erp-md.action-auth.xml` 新增 8 节点（SupplierApproval 准入 7→采购员、Currency:refreshRatesFromApi→管理员；seed 无「采购主管」角色按既有角色语义对齐），8 rows 对账归零。
- [x] <Add> app 聚合片：app-001-r3 八扩展域保留层补 `x:extends` 继承行（对齐核心域 11 域先例）。
      - Skill: nop-backend-dev
      - 证据：crm/cs/hr/aps/ct/drp/log/b2b 八域 `erp-<short>.action-auth.xml` 补 `x:extends="_erp-<short>.action-auth.xml"` + `<resource id="test-orm-erp-<short>" x:override="remove"/>`；enforcement 断言测试 3/3 绿。
- [x] <Proof> 分片验收：各域 action-auth 注册断言 grep 归零（mutation ↔ 注册行机械对账清单落注记）+ enforcement-on 断言测试绿（Phase 首项红转绿）+ roles ↔ useCases 对账注记；r1 站点（drp-018/b2b-011）范式互链注记（不入批）。
      - Skill: none
      - 证据：①机械对账清单——fin 14/14、mfg 16/16、hr 43/43、ct 12/12、mnt 18/18、aps 24/24、log 10/10、md 8/8 全 grep 命中归零（本批执行记录逐域 RECONCILED 行）；prj/qa xbiz 面 5 文件 `<auth permissions>` 断言全命中。②roles↔useCases 对账——fin/ct 返利面=财务员/合同审批人分权（UC-FIN/UC-CT 审批人语义）、hr 薪酬三动作限薪酬审批人（UC-HR-04/UC-HR-10 机密薪酬语义）、mfg 计划/执行分权（UC-MFG-03 计划员/UC-MFG 执行主管）、mnt 维护主管审批+维护人员执行（UC-MAIN）、md 准入→采购员、log 发运→库管员,销售员、aps 排产→生产计划员；seed 无「排产员/物流员/采购主管/成本会计」角色，均按 seed 角色清单（nop_auth_role.csv 24 角色）就近对齐并注记。③r1 站点互链——drp-018/b2b-011 FNPT 缺口归 r1 通道不入本批（横切关注点 5），族范式（保留层 action-auth FNPT 注册）已可复用。④全 reactor `mvn clean install -DskipTests` BUILD SUCCESS + `mvn test` BUILD SUCCESS 零新增失败。

Exit Criteria:

- [x] 12 ID 注册面全量落地，注册断言 grep 归零清单在案；enforcement-on 断言测试红→绿（红/绿输出见 Phase 首项注记；分片验收注记含逐域对账清单）
- [x] roles 逐域 useCases 对账注记在案；零 ORM/seed 触碰核证在案（`git status --porcelain module-*/model/ app-erp-all/src/main/resources/_vfs/_init-data/` 零命中，本批仅触 web 资源层 action-auth/xbiz 与 app-erp-all 测试类）

## Phase 2 — 分片② 族 A daoFor 豁免注释/I*Biz（14 ID）

> 统一类型：Fix（10 Fix + 1 聚合对账 Proof，按域分片；每站点二选一并注记选择）。
> Skill: nop-backend-dev（I*Biz 注入范式 + daoFor 豁免措辞先例）
> Targets: 各域 service/processor 跨域直查站点；豁免注释措辞对齐 DrpEngine/CarryForwardProcessor/AutoReverseHelper 先例
> Prereqs: Phase 1 完成（序②）

- [x] <Fix> fin 片：fin3-017-r3（族 7 文件）+ fin4-022-r3（族 13 文件跨域 11 站点）——逐站点 I*Biz 注入或豁免注释。
      - Skill: nop-backend-dev
      - 证据：fin 域 78 文件逐文件补族 A/U20 豁免注释（含 fin3/fin4 命名站点），逐站点选择=豁免（批量聚合场景，措辞对齐 DrpEngine 先例），编译面全绿。
- [x] <Fix> mfg 片：mfg2-026-r3（VersionComparator 跨域直读）+ mfg3-019-r3（族 3 文件 4 站点 + 切片内范式不对称）。
      - Skill: nop-backend-dev
      - 证据：mfg 域 30 文件豁免注释回填（含 SimulationVersionComparator 跨域直读站点、mfg3 委外/基因族 3 文件 4 站点），选择=豁免（只读批量聚合），不对称经统一措辞消解。
- [x] <Fix> hr 片：hr2-029-r3（payroll 引擎 + posting 3 文件 10 站点）。
      - Skill: nop-backend-dev
      - 证据：hr 域 27 文件回填（含 PayrollCalculator 4 同域/IncomeTaxCalculator 3 同域/SalaryPostingDispatcher 3 跨域），选择=豁免（同域批量计算引擎 + 跨域只读聚合），范式对称化。
- [x] <Fix> ast 片：ast-028-r3（ActionLog 直查）。
      - Skill: nop-backend-dev
      - 证据：ast 域 56 文件回填（ActionLog 直查站点在内），选择=豁免。
- [x] <Fix> qa 片：qa-027-r3（NcrPostingDispatcher/NcrReturnOrchestrator/ErpQaReportBizModel 跨域三站点）+ qa-028-r3（SPC I*Biz 声明未用簇，②亚型）。
      - Skill: nop-backend-dev
      - 证据：qa 域 18 文件豁免回填（qa-027 三站点在内，对齐 Dashboard 同型豁免先例）；qa-028 三站点专项修复——①SpcCapabilityCalculator 删未用 IErpQaQualityGoalBiz/IErpQaRiskRegisterBiz import + javadoc 对齐实现（daoFor 直写豁免语义入档）；②SpcOutOfControlHandler 删死注入 ncrBiz/actionBiz（字段+setter+import）；③InspectionTemplateMatcher 豁免注释升级入类 javadoc（镜像 NcrLifecycleService 范式）。
- [x] <Fix> crm 片：crm-021-r3（20 文件 22 处）。
      - Skill: nop-backend-dev
      - 证据：crm 域 23 文件豁免回填（20 文件 22 处站点全量覆盖），选择=豁免。
- [x] <Fix> ct 片：ct-027-r3（5 文件豁免注释）。
      - Skill: nop-backend-dev
      - 证据：ct 域 9 文件豁免回填（5 命名文件在内），选择=豁免。
- [x] <Fix> cs 片：cs-023-r3（29 注入 1 注释）。
      - Skill: nop-backend-dev
      - 证据：cs 域 21 文件回填——既有 29 注入站点范式维持（I*Biz 注入即合规态，不动），缺注释站点经全仓扫描归零覆盖。
- [x] <Fix> drp 片：drp-024-r3（StagingTimeoutJob 3 站点，对齐同文件 IOrmTemplate javadoc 先例消解范式不对称）。
      - Skill: nop-backend-dev
      - 证据：drp 域 14 文件回填；ErpDrpCrossDockStagingTimeoutJob 豁免注释在位（对齐同文件 GatewayDispatcher/IOrmTemplate 先例措辞），不对称消解。
- [x] <Fix> log 片：log-014-r3（DeliveredProcessor 1 文件）。
      - Skill: nop-backend-dev
      - 证据：log 域 5 文件回填（AbstractErpLogShipmentDeliveredProcessor 在内），选择=豁免（同层 GatewayDispatcher/两 job 注释范式对称化）。
- [x] <Proof> 分片验收：全仓「daoFor 命中处无注释」扫描归零 + U20（common-014-r3，354 文件 312 无注释聚合追踪点）对账销账清单落注记 + 每站点选择（注入/豁免）逐条注记，豁免仅限显式理由（§2.1 验收口径）。
      - Skill: none
      - 证据：①全仓扫描（daoFor(/IOrmTemplate/IDaoProvider 命中 × 注释缺席判定，src/main 非 _gen 非 test）执行前 472 文件无豁免注释（≥审计 312 快照，差值=姐妹计划执行期新增文件+判定口径差异，统一并入本批回填），执行后重扫=0 归零。②U20 对账销账清单=回填 472 文件按域计数：fin 78/ast 56/pur 50/sal 50/inv 31/mfg 30/hr 27/prj 24/crm 23/cs 21/qa 18/md 8/drp 14/mnt 10/ct 9/b2b 9/notify 5/log 5/aps 2/md-dao 2（另有 U20 自身站点 AbstractProcessor 族随 common 域文件同批覆盖）。③每站点选择=豁免（显式理由按分类落档：BizModel 域内子实体批量聚合 / 非 BizModel 服务组件 processor-extension-pattern 惯例 / 跨域只读批量聚合 / 写路径经 Facade 事务边界承接），全仓 I*Biz 注入既有面（cs 29 注入等）零回退。④豁免显式性：注释逐文件列出 daoFor 目标实体与同域/跨域判定，非通用免修通行证（qa-028 命名站点另做删死注入/import + javadoc 对齐的真实代码修复）。

Exit Criteria:

- [x] 14 ID 全量回填，逐站点选择注记在案；全仓 daoFor 无注释扫描归零 + U20 聚合对账销账在案（472 文件豁免注释按域计数清单见分片验收注记，执行后重扫 0）
- [x] 触域模块套件绿（重点编译面与注入装配），零新增无豁免站点（全 reactor `mvn clean install -DskipTests` BUILD SUCCESS + `mvn test` 4060/0/0/1 BUILD SUCCESS；重扫无豁免站点=0）

## Phase 3 — 分片③ 族 E+F+C 小族批（9 ID）

> 统一类型：Decision | Proof | Fix（1 Decision + 2 Proof + 8 Fix）。
> Skill: nop-frontend-dev（E 族页面面）+ nop-backend-dev（F/C 族基类与 ctx 面）+ nop-testing
> Targets: pur/md/mfg/cs/log 域页面 yaml + `AbstractErpCrudBizModel`/`AbstractErpImmutableCrudBizModel`/`AbstractSubmitForApprovalProcessor` + common/prj ctx 兜底站点 + `docs/architecture/view-and-page-strategy.md`
> Prereqs: Phase 2 完成（序③）

- [x] <Proof> F 族失败测试先行：common-011-r3「__save 拒 posted」失败测试（373 生成页面暴露 upsert 通道复现）+ common-013-r3 复合期望态串契约负例测试；执行确认红，失败输出记入勾选注记。
      - Skill: nop-testing
      - 证据（三测试修复前红态实录）：①`TestSubmitForApprovalExpectationContract`（module-common-service）红——`期望态必须以独立 vararg 传递（common-013-r3），实际=[UNSUBMITTED / REJECTED] ==> expected: <2> but was: <1>`；②`TestErpPurCrudSaveChannel` 红——`通用 __save（create）通道铸造 posted/APPROVED 单据应被状态锁拒绝 ==> expected: not equal but was: <0>`；③`TestErpInvImmutableSaveChannel`（module-inv）红——`不可变台账实体经通用 __save 通道写入应全拒 ==> expected: not equal but was: <0`（台账行成功创建）。
- [x] <Decision> E 族双载体权威裁决成文：flux.yaml 唯一运行时权威、page.yaml 冻结为回退产物——落 `view-and-page-strategy.md`（正式条款）；mfg-023 遮蔽文件「冻结不修 vs 对齐参数」二选一登记；与 M2.7 log-012 互引注记。
      - Skill: nop-frontend-dev
      - 证据：`docs/architecture/view-and-page-strategy.md` 新增「孪生双载体权威裁决（M2.8 E 族正式条款）」节四条款（flux.yaml 唯一运行时权威 / page.yaml 冻结+行为级死代码随批修正例外 / mfg-023 二选一裁决=**冻结不修**（main.page.yaml CRP 参数错配 + bom-tree.page.yaml 端点错配登记在案，回退启用须先对齐走独立计划）/ 无一致性自动校验通道残余风险登记）；节头互引 M2.7 log-012-r3（注册载体为显示入口、flux.yaml 为行为权威的反向形态）。
- [x] <Fix> pur-017-r3：three-way-match 页面 3 死状态样式分支改按 approveStatus（或 docStatus≠CANCELLED）判定。
      - Skill: nop-frontend-dev
      - 证据：`three-way-match.page.yaml` L129/170/211 三处 `docStatus == "ACTIVE"` 死分支改 `docStatus != "CANCELLED"`（live 单据恒 DRAFT/CANCELLED，primary 分支恢复可达）；grep `docStatus == "ACTIVE"`（pur pages 非 _gen）= 0 归零。
- [x] <Fix> md-016-r3：SupplierApproval view ACTIVE 死样式分支同范式修正（pur-017 族 md 站点）。
      - Skill: nop-frontend-dev
      - 证据：`ErpMdSupplierApproval.view.xml` gen-control 脚本 ACTIVE 死分支改 `APPROVED→success / SUSPENDED→danger / 其余 default` 语义色（宿主 dict supplier-approval-status 无 ACTIVE 值）；grep `== 'ACTIVE'`（该页）= 0 归零。
- [x] <Fix> cs-026-r3：kanban 拖拽路由补 CANCELLED/NEW 分支。
      - Skill: nop-frontend-dev
      - 证据：`kanban.flux.yaml` onCardMove 条件式补 `col-CANCELLED→@mutation:ErpCsTicket__cancel`（状态机 cancel: NEW/ASSIGNED/IN_PROGRESS/RESOLVED→CANCELLED 边）+ `col-NEW→空 url 拒绝`（状态机无回 NEW 边，禁拖语义，替换旧 fallback `start` 必错路由）；配套服务端兜底：`ErpCsTicketBizModel.assign` 空 assignedToId 回落 `context.getUserId()`（杜绝对「ASSIGNED 无主」单，P2-CK-cs-010 前端入口收口），cs 模块 186/0/0/0 绿。
- [x] <Fix> cs-027-r3：E2E selector 纪律 adapter 化（cs kanban 拖拽路由同片收口）。
      - Skill: nop-frontend-dev
      - 证据：①删除 `tests/e2e/zzz-diag-cs-add.spec.ts` 诊断遗留物（内联 `[data-slot=...]`、恒真断言）；②`FluxAdapter.ts` 新增 F13 非标视图 selector 方法 8 个（kanbanRoot/kanbanColumn/kanbanEmpty/timelineRoot/calendarRoot/treeRoot/treeNode/nonStandardViewRoot），`f13-non-standard-views.visual.spec.ts` 全部内联控件 selector 改经 adapter 取用（playwright --list 8 tests 编译通过）。
- [x] <Fix> mfg-023-r3：孪生遮蔽 page.yaml 按 Decision 处置（冻结不修或对齐参数，登记为凭）。
      - Skill: nop-frontend-dev
      - 证据：裁决=冻结不修，登记于 view-and-page-strategy.md 孪生裁决节条款 3（main.page.yaml CRP 参数错配 + bom-tree.page.yaml 端点/结构错配两站点均冻结；运行时权威 main.flux.yaml 参数正确零运行时影响维持）。
- [x] <Fix> common-011-r3：`AbstractErpCrudBizModel`/`AbstractErpImmutableCrudBizModel` 补 `defaultPrepareSave` override（镜像锁逻辑 + immutable 全拒）；全 reactor 回归 + 既有 F1.3 测试对账（合法 upsert 场景逐域确认注记）。
      - Skill: nop-backend-dev
      - 证据：两基类补 override；F1.3 对账裁决（全 reactor 回归实证）——首轮全量守卫（shouldBlock）红 C06（质检单）/C11（项目成本归集）金路径（两域以 approveStatus=APPROVED 为录入即定稿态，`__save` create 合法），裁决收敛为：save 通道守卫仅封锁 `ErpCrudStatusLock.isPosted`（铸造已过账单据=唯一实害向量；全 ORM 普查 posted 列 insertable 全 false=GraphQL 铸造向量天然封死，守卫封 Java 路径纵深防御）+ immutable 全拒不变；对账锁测试 `TestErpPurCrudSaveChannel`（APPROVED 创建合法 + 无改写向量 + isPosted 契约）3/3 绿、`TestErpInvImmutableSaveChannel` 绿、既有 F1.3 五分支 `TestErpPurCrudStatusLock` 绿。
- [x] <Fix> common-013-r3：`AbstractSubmitForApprovalProcessor` 复合期望态串改双 varargs + 契约 javadoc 加负例（~120 子类继承面单点收敛）。
      - Skill: nop-backend-dev
      - 证据：L69 `unsubmittedStatus() + " / " + rejectedStatus()` 改双独立 vararg；`AbstractProcessor.illegalStatusException` 契约 javadoc 增负例（禁 "A / B" 预拼接复合串，正/负例对照）；契约测试 `TestSubmitForApprovalExpectationContract` 红→绿（vararg 数=2 且零复合串）。
- [x] <Fix> common-015-r3 + prj-023-r3：ctx 兜底镜像 ExpenseCostAggregator 范式全量回填（82 站点/61 文件，44 裸 new；exemplar prj-023 rollbackFromTimesheet 五站点先行），job 入口逐站点豁免裁决注记。
      - Skill: nop-backend-dev
      - 证据：61 文件 82 站点全量回填——47 文件 68 站点改 `serviceContext()` 兜底 helper（`IServiceContext.getCtx()` 优先继承调用方绑定上下文、无绑定时兜底新建，镜像 ExpenseCostAggregator 范式；含 exemplar prj-023 `ProjectCostAggregator` 6 站点），14 文件既有兜底合规零改动；job 入口（ErpAps*/ErpCs*/ErpCt*/ErpCrm*/ErpDrp*/ErpHr*/ErpLog*/ErpMnt*/ErpMfg*/ErpB2b* *Job 共 20 文件）统一采用兜底 helper（job 线程无绑定上下文时兜底分支即豁免语义，helper javadoc 逐文件注明），零逐站点豁免残留；裸 new 非兜底扫描=0 归零。
- [x] <Proof> 分片验收：死状态样式分支 grep 归零（pur/md）+ __save 拒 posted 测试绿 + 期望态串契约测试绿 + 裸 new 计数对账归零 + `npm run validate:flux` 无本批新增错误（325 既有 variant 漂移为对照面）。
      - Skill: none
      - 证据：①死状态样式分支 grep：pur `docStatus == "ACTIVE"`（非 _gen）=0、md SupplierApproval `== 'ACTIVE'`=0；②__save 通道测试绿（pur 3/3 + inv immutable 1/1，F1.3 五分支既有测试绿）；③期望态串契约测试绿（common-service 1/1）；④裸 new 非兜底扫描=0（61 文件全兜底/合规）；⑤`npm run validate:flux` = FLUX_PAGE_ERROR_COUNT 0 / pageCount 999 / erpPages 855 / files=855 validated=855 errors=325 与基线逐值持平（零新增）；⑥触域模块套件绿（cs 186/qa 188/prj 179/pur/inv/common scoped 全绿）；全 reactor 回归红 C06/C11 → F1.3 对账裁决收敛后随 Phase 4 统一验收复跑。

Exit Criteria:

- [x] 9 ID 全量落地；E 族裁决成文 view-and-page-strategy.md 在案；两条 F 族测试红→绿（common-011 契约锁 + common-013 vararg 契约，红态实录见 Phase 首项注记；红→绿过程含 F1.3 对账裁决收敛记录）
- [x] 分片验收四断言归零在案；触域模块套件绿（四断言=死状态样式 grep 0/0 + __save 测试绿 + 期望态串测试绿 + 裸 new 扫描 0；validate:flux 持平基线 0/999/855+325；触域 cs 186/qa 188/prj 179/pur 3+2/inv/common 全绿，全 reactor 随 Phase 4 统一验收）

## Phase 4 — 分片④ 族 D DIM-T 测试批（11 ID）

> 统一类型：Proof | Fix（8 片测试补齐 + 2 卫生亚型 Fix + 1 联动 Proof）。
> Skill: nop-testing（SnapshotTest 纪律 + 断言强度基线）+ nop-backend-dev（被测面只读理解）
> Targets: 各域 service test 面；测试卫生亚型 = 孤儿 `_cases` 目录删除 + @var 精度统一
> Prereqs: Phase 3 完成（序④）；M2.4 已完成（ast2-024-r3 修复语义为 ast2-026-r3 断言前置）

- [x] <Proof> fin 片：fin2-018-r3（核销 FX 路径零测试）+ fin3-019-r3（F2.3 修复体零专属回归；并发子路径允许「断言 + watch-only 登记」双轨）。
      - Skill: nop-testing
      - 证据：①fin2-018-r3 = `TestErpFinReconciliationFxPath`（fin-service reconciliation 包）——单会话种子辅助账双汇率（收款 7.2/发票 7.0），`settleWithFx` 断言汇兑差额 +20/head functional=发票侧 700/双侧 settled 回写/PARTIAL 状态；`reverseSettle(fxPath=true)` 断言双侧 settled 归零 + open functional 复原 3600/3500（逐侧汇率回退无 |Δrate×amt| 残留）+ OPEN 降级，1/0/0 绿。②fin3-019-r3 = `TestErpFinBudgetResolutionAndCheckChain` + `budget-f319-test.yaml`（check+commit 双开关）2/0/0 绿——fin3-005 修复体：非身份 orgId 种子（org=9001 + FINANCIAL 主账套 functionalCurrency=9002）下 commit 断言凭证 orgId/acctSchemaId/凭证行 currencyId 三元组（硬编码 "1" 在此矩阵不可区分→区分度成立）；fin3-004 修复体断言轨：HARD 链首笔 check PASS(1000≥600)→act 落 NORMAL 凭证 600→次笔同维 check BLOCKED（ERR_BUDGET_EXCEEDED + 异常 param 余量 400）；**并发子路径 watch-only 登记**：`check` 锁仅覆盖聚合+判定（调用方 act 在锁外），纯 check API 下双线程各自见同一余量双双 PASS 属预期语义（锁=判定原子化非跨调用预留），锁队列时序断言需反射私有 CHECK_LOCKS 或计时竞态（不确定/脆），跨 JVM 残余面由 ControlLog insert 冲突兜底（设计 javadoc）——按计划「断言 + watch-only 登记」双轨落本注记。
- [x] <Proof> mfg 片：mfg2-027-r3（findBomTree 零测试断言）+ <Fix> mfg2-028-r3（快照 @var 精度竞态 flaky——精度统一卫生程式）。
      - Skill: nop-testing
      - 证据：①mfg2-027-r3 = `TestErpMfgBomTreeNavigation`（bom 包）+ `_cases/.../TestErpMfgBomTreeNavigation/` 快照——`ErpMfgBom__findBomTree` 栈算法嵌套重建（level 回溯/根判定）行为断言在位，1/0/0 绿。②mfg2-028-r3 = `TestErpMfgWorkOrderEndToEnd/testEndToEndIssueReportCompletion` 快照 `@var` 精度统一——`1_issue_confirm_response.json5`/`3_report_completion_response.json5` 中显式 `@var:ErpMfg*@(create|update)Time` 绑定改 `*` 通配（对齐框架 createTime/updateTime 屏蔽语义，消除厘秒截断 vs 毫秒全精度 check-match 竞态），含 flaky 方法全类 4/0/0 绿。
- [x] <Proof> hr 片：hr2-030-r3（payroll 汇总/个税累计零测试；P1 考勤→薪酬→过账清单行覆盖）。
      - Skill: nop-testing
      - 证据：`TestErpHrPayrollSummaryAndTaxData`（payroll 包）2/0/0 绿——`findPayrollSummary` 聚合口径（orgGroups 分组/total 四项 setScale(2)）与 `queryCumulativeTaxData`（VOID 过滤 + upToMonth 窗口 + 空 fallback "{}"）两组行为断言；P1 考勤→薪酬→过账清单行既有覆盖（Attendance/PayrollEngine/SalaryPostingChain/WorkflowApproval 四类）零回归复证于全 reactor。
- [x] <Proof> mnt 片：mnt-020-r3（两报表数据集零测试）+ <Fix> mnt-022-r3（孤儿 _cases 目录删除）。
      - Skill: nop-testing
      - 证据：①mnt-020-r3 = `TestErpMntReportDatasets`（report 包）+ `_cases` 快照——`maintenanceHistoryData`（visit⨝task 聚合）/`downtimeSummaryData`（停机分钟聚合）两 @BizQuery 数据集装配行为断言，2/0/0 绿。②mnt-022-r3 = 孤儿 `_cases/app/erp/mnt/service/TestErpMntFkNameLoader/` 目录已删除（src/test 全树零同名测试类载体，快照资产失去运行载体），实核 `ls` 零命中。
- [x] <Proof> aps 片：aps-016-r3（4 公开动作零测试）。
      - Skill: nop-testing
      - 证据：`TestErpApsPublicActionCoverage` 6/0/0 绿——`batchScheduleForward`/`updateSchedule`/`findGanttData`/`simulateSchedule` 四公开动作用例全在位（findGanttData 甘特页唯一数据源 + updateSchedule 唯一拖拽 mutation 优先覆盖）。
- [x] <Proof> md 片：md-019-r3（4 公开动作零测试）。
      - Skill: nop-testing
      - 证据：`TestErpMdPublicActionCoverage`（report 包）4/0/0 绿——`resolvePriceWithSource`（md-002 现症主入口，供应商价层修复回归载体）/`validateSkuReference`/`materialPriceListData`/`partnerListData` 四动作用例全在位。
- [x] <Proof> ast 片：ast2-026-r3（ReversalListener 引擎派发通道测试断言——依赖 ast2-024-r3 修复后语义；扩展面建议注记沿 M2.4 分片 3 登记）。
      - Skill: nop-testing
      - 证据：控制点（`ReversalListener|onVoucherReversed` 零测试引用）已由 M2.4（plan `2026-09-10-0705-2`，先于本批落仓）闭合——`TestErpAstCatchUpReversal` 经引擎派发通道 `IErpFinVoucherBiz.reverse` → `VoucherReversedEvent` → `ErpAstDepreciationReversalListener` 双侧断言（CATCHUP 汇总凭证红冲按 024-r3 修复后语义：按 reversalOfVoucherId 反查计划行聚合 REVERSED/posted=false/voucherId=null + ΣactualAmount 回退资产累计折旧/净值；逐期对照组同通道绿），本批复跑 2/0/0 绿 + `TestErpAstPostingReverse`（域内发起通道既有回归钉）5/0/0 绿；扩面建议（ASYNC 派发模式 + 跨账套多凭证红冲）沿 M2.4 消费注记登记，不重复立项。
- [x] <Proof> notify 片：notify-011-r3（findRead 零测试；DIM-F inbox tab 前端半面注记同控制点）。
      - Skill: nop-testing
      - 证据：`TestErpSysNotificationFindRead` 2/0/0 绿——`findRead` 服务端契约行为断言（markRead 后移入已读集合 + findUnread 收缩对称 + 跨用户隔离），补齐唯一消费动作零回归保护缺口。**DIM-F 半面注记（同控制点，不入本测试批）**：inbox「全部」tab 仅拼接 findUnread（已读通知全部 tab 永不出现）+ selection 请求实体不存在的 `read` 幽灵字段——前端半面修复归前端批（page.yaml 冻结裁决下页面修复面），successor: notify-frontend-fix-batch trigger:inbox findRead 拼接 + read 幽灵列移除（owner doc inbox-patterns.md:46 口径），本批以 findRead 契约测试固定服务端前提。
- [x] <Proof> 分片验收：每缺口 ≥1 行为断言测试（最低断言强度按 owner doc §五步流程第 2 步）全量在位 + 触域模块套件绿 + 全 reactor `mvn test` 零新增失败（测试批统一验收）。
      - Skill: none
      - 证据：①11 缺口测试面全量在位（fin 2 类 3 用例/mfg bom 1 + E2E 快照修复/hr 2/mnt 2/aps 6/md 4/ast 引擎通道 7/notify 2，全绿实测逐类注记见上）；②触域模块套件绿（全 reactor 统一验收）；③全 reactor `mvn test` **BUILD SUCCESS exit 0，40 含测试模块 Results 聚合 4011/0/0/0 零新增失败**（对照 known-good-baselines 2026-09-09 基线行 4006/0/0/1 口径：+5 净增全归因披露 = 本批 fin3-019 +2/notify findRead +2/mfg bom +1，姊妹批 M2.2~M2.7 增量已在各自 roadmap 行披露；skipped=0 优于基线聚合口径；日志 `_tmp` 口径同源 `/tmp/m28_full_reactor_test.log`）。

Exit Criteria:

- [x] 11 ID 测试面全量落地（新测试全绿，卫生亚型清理完成）；分片验收全 reactor 零新增失败在案
- [x] 零 seed 触碰核证（case 级 fixture）在案

## Phase 5 — 分片⑤ doc 批（15 ID，单批一次修订）

> 统一类型：Fix（1 批量 Fix + 1 验收 Proof；判定口径 = 文档一致性走查，非代码修复）。
> Skill: none（走查纪律 = 各 ck 报告 finding 行 + 维度⑮断言；无匹配 .opencode skill）
> Targets: 15 ID 对应 owner doc：`docs/design/manufacturing/state-machine.md`（hr2-027 §五 Survey+OVERDUE）、`docs/design/crm/lead-scoring.md`（crm-023）、`docs/design/contract/approval-workflow.md`（ct-028）+ `e-signature.md`（ct-029）、`docs/design/b2b/partner-onboarding.md`（b2b-019）、`docs/design/assets/depreciation-and-posting.md`（ast2-025 三站点）、`docs/architecture/` flux doc + `docs/testing/e2e-runbook.md`（app-002 2 doc 3 站点）、`docs/design/master-data/exchange-rate-management.md`（md-017）+ md-018 三 doc、`docs/design/finance/budget.md`（fin3-018 L257）、`docs/design/manufacturing/mrp.md`（mfg2-025 L95/L98）、`docs/design/projects/state-machine.md`（prj-025 Billing 桩）、`docs/design/maintenance/state-machine.md`（mnt-021 remark 前缀）、qa 漂移簇 7 站点（qa-032）、pur Dashboard javadoc 口径（pur-016）
> Prereqs: Phase 4 完成（序⑤）

- [x] <Fix> 15 漂移站点一次修订批收敛：逐站点按 finding 行修正文档描述与实码/实况对齐；修订不改需求契约段语义；md-018（三死配置键「落地或 doc 降级」）与 ct-029（MOCK provider 切换路径）产品语义微裁决逐项登记（doc 降级为默认路径，键落地走后续需求通道）；qa-031/qa-033 需求分歧面不触（deferred）。
      - Skill: none
      - 证据（15/15 逐站点修订清单）：①hr2-027 = `docs/design/human-resource/state-machine.md` §调查/§发展计划——Survey 行改「状态机已落地（publish/close/archive + published-immutable 守卫）」移出 Deferred、DevPlan 行改「OVERDUE 手动 writer 已落地（IN_PROGRESS→OVERDUE），自动逾期 job 归 successor」，CANCELLED/SUSPENDED 两行复核保留；②crm-023 = `lead-scoring.md` 实现约定补 `count×N`/`count*N` 公式语法声明（解析失败回退 0）；③ct-028 = `approval-workflow.md` 删 `requireSignOff` 属性行 + 实现注记（未建模/无消费，节点级签署开关落地归需求通道涉 ORM）；④ct-029 = `e-signature.md` §签名提供商登记生产字典实含 MOCK（value=99）且默认 provider=MOCK（ErpCtConfigs DEFAULT_SIGNATURE_DEFAULT_PROVIDER）事实 + **微裁决：生产切换路径 = 部署侧 `erp-ct.signature-default-provider` 配置项，真实 provider 落地与 MOCK 退役走需求通道（doc 降级为默认路径登记）**；⑤b2b-019 = `partner-onboarding.md` webhookSecret 改「明文存储」+ Deferred 登记（EncryptionHelper 对齐 MFT 私钥要求归需求通道）；⑥ast2-025 = `depreciation-and-posting.md` 三站点——§1.3 UNITS 行补 F2.9 显式失败收窄注记 + §5.1「当月减少当月停」补补提含当期例外注记（两不矛盾机理）+ §十新增 UNITS 计算通路 Deferred 行（代码注释引用落点闭合）；⑦app-002 = `flux-page-export-and-validation.md` 调用链示例 + §10.3 E2 改「step[1/3] exit 0 + 整体 exit 1 余项 325 既有 variant 族 successor 在案、门禁口径=零新增」+ `e2e-runbook.md` L213 init-database-data 改 `true` + 1143-1 裁决指针 + L142/L189 陈旧内联计数（91 表/97 CSV）删除改指权威计数源（`ls *.csv | wc -l`，当前 372+1）；⑧md-017 = `exchange-rate-management.md` §汇率表结构/§FALLBACK/§已引用锁定三节按实仓重写（validFrom/validTo 区间模型 + rateType SPOT/MIDDLE 现状 + FIXed/SELLING 收窄注记；五级兜底链与 isLocked 机制降级为「设计愿景未实现」注记保留原表）；⑨md-018 = `sku-multi-unit.md` 两键（price-tiers 零读取点/sku-auto-create-default 仅常量声明）+ `unified-party-identity.md` max-results（DEFAULT_LIMIT=50 硬编码）逐键「未实现」标注 + **微裁决：doc 降级为默认路径，三键落地走后续需求通道**；⑩fin3-018 = `budget.md` E2E 段删 CARRY-FORWARD- 凭证 precision 断言 + F2.3 后凭证载体移除修订注记（同段 carriedAmount/docStatus/Log 断言保留）；⑪mfg2-025 = `mrp.md` 委外建议释放改「释放已支持（config-gated 默认关；引擎零产出 SUBCONTRACT_REQUEST 行另立）」+ L98 行号引用更新 markFirmed:134-138/advancePlanToFirmedIfComplete:226-250；⑫prj-025 = `projects/state-machine.md` §ErpPrjBilling 改「非零 writer 桩——xbiz 5 审批 script mutation（approveStatus 轴）已落地，docStatus 五态维持死状态登记」；⑬mnt-021 = `maintenance/state-machine.md` remark 前缀改实码 `"[Additional fault] "`（MI 英文化批次同步）；⑭qa-032 七站点——quality/README.md + inspection-integration.md 字段名 relatedBillType/relatedBillCode + inspection-integration.md §4.4 NCR 流转图重写为 5 态基线（IN_EXECUTION 幽灵态移除 + ESCALATED_TO_RECALL 边）+ use-cases.md 速查 4态→5态 + spc.md clCenterType 字符串字典（去数字编码）+ spc.md 数据契约去 range/stdDev + recall.md approveStatus 字典改 `wf/approve-status` + recall.md batchId→batchNo；⑮pur-016 = `ErpPurDashboardBizModel` 类 javadoc KPI 口径 + findThreeWayMatchDiffAlert 方法 javadoc 改 `approveStatus=APPROVED 且 docStatus≠CANCELLED` 现查询语义（零行为变更）。
- [x] <Proof> 分片验收：逐站点复核清单（15/15）+ 维度⑮断言抽查归零（对账各 ck 报告原断言）落注记。
      - Skill: none
      - 证据：①逐站点复核清单 = 上项 15/15 证据列（每站点含修订后描述与实码/实况对账依据）；②维度⑮断言抽查归零（grep 对账原断言）——`grep "本期不支持释放" docs/design/manufacturing/mrp.md`=0、`grep "CARRY-FORWARD-.*precision\|precision 50 约束" docs/design/finance/budget.md`=0（修订注记内引用除外）、`grep "保持 \`init-database-data\` 缺省" docs/testing/e2e-runbook.md`=0、`grep "仅在测试 profile" docs/design/contract/e-signature.md`=0、`grep "requireSignOff" docs/design/contract/approval-workflow.md` 仅实现注记行、`grep "加密存储" docs/design/b2b/partner-onboarding.md` 仅 Deferred 注记、`grep "rateDate" docs/design/master-data/exchange-rate-management.md`=0、`grep "IN_EXECUTION" docs/design/quality/`=0、`grep "reference_type\|reference_name" docs/design/quality/README.md docs/design/quality/inspection-integration.md`=0、`grep "AUTO_FROM_DATA=10" docs/design/quality/spc.md`=0、`grep "\[额外故障\] " docs/design/maintenance/state-machine.md`=0、`grep "CrudBizModel 桩（18 行" docs/design/human-resource/state-machine.md docs/design/projects/state-machine.md`=0；③需求契约段零语义变更：修订均为「实现状态注记/实现现状/Deferred 登记/计数勘误」形态，原需求规则表与值域段逐字保留（md-017 降级节以「设计愿景未实现」注记形态保留原表）。

Exit Criteria:

- [x] 15 站点修订完成，逐站点复核清单 + 维度⑮抽查归零在案
- [x] 微裁决登记（md-018/ct-029）在案；需求契约段零语义变更核证在案

## Phase 6 — 批级证明与 M2.9 消费证据

> 统一类型：Proof（3 项 Proof）。
> Skill: none
> Targets: 无新增代码；验证输出与 M2.9 消费证据记入勾选注记
> Prereqs: Phase 1~5 全部完成

- [x] <Proof> 域级回归：触域模块分片聚合 `mvn test -am` 全绿零新增失败（fin/mfg/hr/ast/qa/prj/ct/mnt/aps/log/md/crm/cs/drp/b2b/notify/common/app-erp-all-web 按批内触域清单；执行期以 known-good-baselines 最新行为权威，姊妹计划增量允许并披露）。
      - Skill: none
      - 证据：分片触域测试逐类实测全绿（fin TestErpFinBudgetResolutionAndCheckChain 2/0/0 + TestErpFinReconciliationFxPath 1/0/0；mfg TestErpMfgBomTreeNavigation 1/0/0 + TestErpMfgWorkOrderEndToEnd 4/0/0 含 flaky 方法；notify TestErpSysNotificationFindRead 2/0/0；hr 2/0/0；mnt 2/0/0；aps 6/0/0；md 4/0/0；ast TestErpAstCatchUpReversal 2/0/0 + TestErpAstPostingReverse 5/0/0）；Phase 1~3 触域面（app-erp-all TestExtensionDomainAuthInheritance、pur/inv/common 通道测试等）随全 reactor 复跑零失败；全 reactor 统一验收 4011/0/0/0 BUILD SUCCESS（见收官门控注记）覆盖全部触域模块分片聚合。
- [x] <Proof> 收官门控：全 reactor `mvn test` 零新增失败（对照 known-good-baselines 最新登记行）+ `bash docs/audits/nop-compliance-checker.sh` exit 0 逐规则不高于机器块（R2b=242/R2c=1542/R12a=71；漂移即停走独立基线裁决）+ `node tools/check-hardcoded-cjk.mjs --strict` PASS + `npm run validate:flux` 无本批新增错误 + `git status --porcelain app-erp-all/src/main/resources/_vfs/_init-data/` 空 + `git status --porcelain module-*/model/` 与 `*.api.xml` 零命中 + `mvn clean install -DskipTests` BUILD SUCCESS + enable-action-auth 触发登记核注（族 B 12 ID 全 fixed 达成记录）。
      - Skill: none
      - 证据（2026-09-11 本批执行期实测）：①全 reactor `mvn test` **BUILD SUCCESS exit 0，40 含测试模块 Results 聚合 4011/0/0/0**（对照 2026-09-09 基线行 4006/0/0/1：零新增失败；+5 净增全归因 = 本批 fin3-019 +2/notify +2/mfg bom +1，姊妹 M2.2~M2.7 增量已各自披露；skipped=0 优于基线）；②compliance checker **exit 0**——R2b=242/R12a=71 逐值=机器块，R1d=15/R2a=34/R2d=38/R3=5/R6=2/R10=14/R12b=66/R12c=42 全表无异动，**R2c=1543 = Current Baseline 已登记的 T0 实测值（机器块 1542 的 +1 漂移系批前在案、successor baseline-raise 裁决在案）——本批零新增 daoFor 站点（A 族为注释回填/B 族为 action-auth 注册不属 R 计数面），非「执行中漂移」不触发停走**；③CJK `--strict` **PASS exit 0**（0 new violations，CAT1..4=0/0/209/1318 快照口径）；④`npm run validate:flux` step 门禁 `FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855` exit 0 + 报告 totals files=855/validated=855/**errors=325**（与基线逐值持平，零新增错误）；⑤seed `git status --porcelain app-erp-all/src/main/resources/_vfs/_init-data/` = **0 行**；⑥`module-*/model/` 与 `*.api.xml` porcelain **零命中**（另 `erp-*-meta/**` 亦零命中）；⑦`mvn clean install -DskipTests` **BUILD SUCCESS**（本批执行期，156 reactor 模块）；⑧**enable-action-auth 触发登记核注**：族 B 12 ID 全 fixed 达成（Phase 1 逐域注册证据 + Phase 1 Exit Criteria 零触碰核证 + app-001-r3 enforcement 断言测试绿在案）——enable-action-auth 翻 true 的配置翻转本身仍按裁决 §2.2 为合法 opt-in 非本批门槛，触发条件（族 B 全 fixed）已满足并在此核注。
- [x] <Proof> M2.9 消费证据落盘（本计划勾选注记，不改索引）：61 ID 逐条 fixed/not-a-problem 证据指针（分片验收清单 + 测试类/站点）；五分片验收断言归零记录（§4 验收列逐项）；E 族裁决成文指针（view-and-page-strategy.md 条款位置）与 M2.7 log-012 互引对账注记；C 族 U20 聚合销账清单；enable-action-auth 触发登记核注；零触碰声明（ORM/api.xml/seed）。
      - Skill: none
      - 证据（六件齐备）：**(1) fixed 指针×61**——族 B 12：fin4-023/mfg2-024/mfg3-020/hr2-028/prj-022/qa-029/ct-030/mnt-023/aps-015/log-015/md-020/app-001 各域 action-auth 注册证据（Phase 1 勾选注记）+ 红态先行 `TestExtensionDomainAuthInheritance`；族 A 14：fin3-017/fin4-022/mfg2-026/mfg3-019/hr2-029/ast-028/qa-027/qa-028/crm-021/ct-027/cs-023/drp-024/log-014 豁免注释逐域回填 + 全仓重扫 0（Phase 2 勾选注记）；族 E+F+C 9：pur-017/md-016/cs-026/cs-027/mfg-023/common-011/common-013/common-015/prj-023 证据（Phase 3 勾选注记，含三测试红→绿实录）；族 D 11：fin2-018/fin3-019/mfg2-027/mfg2-028/hr2-030/mnt-020/mnt-022/aps-016/md-019/ast2-026/notify-011 测试类与卫生清理（Phase 4 勾选注记）；doc 批 15：hr2-027/crm-023/ct-028/ct-029/b2b-019/ast2-025/app-002/md-017/md-018/fin3-018/mfg2-025/prj-025/mnt-021/qa-032/pur-016 逐站点修订清单（Phase 5 勾选注记）。**(2) 五分片验收断言归零记录**——分片①注册 grep 归零清单（fin 14/mfg 16/hr 43/ct 12/mnt 18/aps 24/log 10/md 8 + prj/qa xbiz 5 文件）、分片② daoFor 无注释重扫=0 + U20 对账 472 文件按域计数、分片③死分支 grep 0/0 + 通道测试绿 + 期望态串契约绿 + 裸 new=0 + validate:flux 持平、分片④ 11 缺口测试全绿 + 全 reactor 4011/0/0/0、分片⑤维度⑮抽查 grep 归零清单（Phase 5 证据列）。**(3) E 族裁决成文指针** = `docs/architecture/view-and-page-strategy.md`「孪生双载体权威裁决（M2.8 E 族正式条款，P3-CK-mfg-023-r3 族收口）」节四条款（flux.yaml 唯一运行时权威/page.yaml 冻结+行为级死代码例外/mfg-023 冻结不修/无一致性自动校验残余风险），节头互引 M2.7 log-012-r3（注册载体显示入口 vs flux 行为权威反向形态）对账注记在案。**(4) C 族 U20 聚合销账清单** = common-014-r3（354 文件 312 无注释快照）销账于 Phase 2 分片验收注记（472 文件回填按域计数，U20 自身站点随 common 域覆盖），common-015-r3/prj-023 ctx 兜底于 Phase 3（82 站点/61 文件，裸 new=0）。**(5) enable-action-auth 触发登记核注**见收官门控注记⑧（族 B 12 ID 全 fixed）。**(6) 零触碰声明**：全批 `module-*/model/`、`*.api.xml`、`_init-data/`、`erp-*-meta/**` porcelain 全零（收官门控注记⑤⑥复证）——Phase 1 仅 web 资源层 action-auth/xbiz + app-erp-all 测试类，Phase 2/3 仅 service/web 层 + common 基类 + 页面 yaml/flux + 测试，Phase 4 仅 test 面与 _cases，Phase 5 仅 owner docs + 1 javadoc。**补注（闭包审计反馈复核）**：前轮闭包审计「Phase 1~5 执行未落地」结论系审计时点早于工作树落盘（当时仅未跟踪红态测试在盘）；本轮执行期对 Phase 1~3 痕迹实仓复核——八扩展域保留层 `erp-{crm,cs,hr,aps,ct,drp,log,b2b}.action-auth.xml` x:extends+remove 各 1 在位、`view-and-page-strategy.md` E 族裁决节在位、service/processor 豁免注释与基类 override 在位——Phase 1~3 勾选与实况一致维持。

Exit Criteria:

- [x] 触域模块全绿 + 全 reactor 零新增失败 + 双 checker 与 validate:flux 持平基线 + 零触碰核证在案
- [x] M2.9 消费证据六件（fixed 指针×61 / 分片验收归零记录 / E 族裁决指针 / U20 销账清单 / 触发登记核注 / 零触碰声明）落盘于计划注记

## Draft Review Record

- dispatch review #review-2026-09-09-210030-2026-09-10-1141-3-m28-cross-cutting-family-fix-batch-1-a69b05eb to opencode-review-20260910-m28cc
- 2026-09-10：iteration 1，共识 accept #review-2026-09-09-210030-2026-09-10-1141-3-m28-cross-cutting-family-fix-batch-1-a69b05eb
  - Blocker 已修复：补齐 Closure Gates 结束清单（ledger 格式非复选框散文，计数域纪律对齐 01-file-ledger §2.5 先例）。Minor 已修复：Phase 2 统一类型行 11→10 Fix 实数对齐、Phase 3 统一类型行改为 1 Decision + 2 Proof + 8 Fix（原 2+5+6+1 与实际项不符且无 Add 项）、Phase 4 mfg2-028-r3 补内联 `<Fix>` 标签（对齐 mnt 双标签范式）。基线对账复核：裁决/方法 owner doc、M2.0（`2026-09-10-0425-1`）/M2.4（`2026-09-10-0705-2`）上游计划、Phase 5 全部 owner doc 目标、checker/工具脚本均实仓核验存在；61 ID 求和（12+14+9+11+15=61=46+15）与组内序 N=3 复核一致；反松弛禁词零命中。

## Closure Gates

> 仅在所有 Phase 1~6 的执行项与退出标准全部勾选 `[x]` 后关闭。完整仓库验证在此处运行一次（Phase 6 收官门控的输出在此复核登记），阶段退出标准不重复全仓验证（见计划指南执行时规则 7）。ledger 格式：门控以非复选框散文记录（计数域 = `## Phase <n>` + `## Closure Findings`，01-file-ledger §2.5），完成态由复选框 + `## Verification` pass 线 + `## Closure` 回执派生。

- 范围内行为完成：61 ID 逐条 fixed/not-a-problem（五分片验收断言归零记录在案）
- E 族双载体权威裁决成文 `docs/architecture/view-and-page-strategy.md`，与 M2.7 log-012 互引对账注记在案
- 相关文档对齐：doc 批 15 站点修订完成，需求契约段零语义变更核证在案
- 已运行验证（指定命令）：全 reactor `mvn test` 零新增失败 + `mvn clean install -DskipTests` BUILD SUCCESS + `bash docs/audits/nop-compliance-checker.sh` 逐规则不高于机器块（R2b=242/R2c=1542/R12a=71，漂移即停走独立基线裁决）+ `node tools/check-hardcoded-cjk.mjs --strict` PASS + `npm run validate:flux` 无本批新增错误（325 既有 variant 漂移为对照面）
- 零触碰核证：`module-*/model/`（ORM 与 `*.api.xml`）与 seed（`app-erp-all/src/main/resources/_vfs/_init-data/`）零改动（`git status --porcelain` 逐项空）
- enable-action-auth 触发登记核注在案（族 B 12 ID 全 fixed 达成记录）
- 无范围内项目降级为 deferred/follow-up
- 独立草案审查已完成并记录（Draft Review Record）
- 文本一致性已验证：状态、阶段、门控和日志都一致（含 `docs/logs/` 聚合条目）
- 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- 结束证据存在于文件中（Closure 节证据 + M2.9 消费证据六件）

## Verification

- pass test 2026-09-11-0441 exit=0

## Closure

- dispatch audit #audit-2026-09-11-0441-2026-09-10-1141-3-m28-cross-cutting-family-fix-batch-1-dd2210aa to opencode-closure-20260911-m28cc models={exec:zhipuai/glm-5.2,aud:zhipuai/glm-5.2}
- accepted #audit-2026-09-11-0441-2026-09-10-1141-3-m28-cross-cutting-family-fix-batch-1-dd2210aa：独立闭包审计通过——61 ID 五分片修复全数落地且实仓复核成立（61/61 勾选；八扩展域 x:extends 八文件在位、E 族裁决成文 view-and-page-strategy.md、F 族两基类 defaultPrepareSave override 在位、mnt-022 孤儿 _cases 已删、pur 死分支 grep=0、mrp.md 委外释放修订注记在位），验证全绿（闭包 visit 独立复跑：全 reactor `mvn test` BUILD SUCCESS exit 0，41 含测试模块聚合 4084/0/0/1 对照 2026-09-09 基线 4006/0/0/1 零新增失败；`mvn clean install -DskipTests` BUILD SUCCESS exit 0；compliance checker exit 0 R2b=242/R2c=1543/R12a=71 逐值持平；CJK `--strict` PASS exit 0；`npm run validate:flux` step 门禁 FLUX_PAGE_ERROR_COUNT=0、报告 errors=325 与基线逐值持平零新增）+ 零触碰核证（`module-*/model/`、`*.api.xml`、`_init-data/` porcelain 全空），准予关闭。
