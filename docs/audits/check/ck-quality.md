# ck-quality — quality 域实现代码检查报告

> 工作项：C5.2。执行日期：2026-08-25。执行者：域检查子代理（ZCode 会话）。
> 范围：`module-quality/erp-qa-service/src/main/java` 全部 72 个手写生产文件（entity BizModel 21 + spc 引擎 7 + posting 4 + processor 27 + statemachine 5 + dashboard 1 + report 1 + 根常量 3）+ `_vfs/nop/batch-task/qa/{spc-sampling,spc-capability}.batch.xml` + `_vfs/erp/qa/beans/app-service.beans.xml`、`_service.beans.xml`（接线核对）+ `app-erp-all/.../nop/job/conf/erp-qa-spc-{sampling,capability}.job.yaml`（D4）。跨域/平台核实：`InspectionTrigger`（business→quality 门控 Facade）、`module-quality/model/app-erp-quality.orm.xml`（versionProp/UK/useLogicalDelete/dict valueType）、`ErpOrgIsolationConstants`（写路径 preSave stamp 语义）、nop-entropy `OrmEntity#orm_propValueByName`（→`BeanTool.setProperty`）、`CascadeFlusher`（cascade-delete 机制）、RC-R1.26 plan（`inject` 简单名解析不成立的运行时实证）、`TestErpQaInspectionStateMachine`/`TestErpQaSpcSampling`（测试覆盖面与 seed 配方核对）。
> 方法：Skill: `code-quality-audit-prompt` + `behavioral-failure-mode-scan-prompt`（B1/B2/B3.1/B3.2/B4）。核心链逐文件深读（检验单 7 Processor + NCR/CAPA 生命周期 + posting 4 类 + SPC 7 引擎类 + Recall 12 Processor/BizModel + dashboard/report 全读；CRUD 桩 10 类抽查确认）+ 平台源码/plan 证据实证 + arm-index 复用裁决。
> 特别核查（mission 指定）：**P1-RC-042 修复在位验证 = 通过**（见「验证为正确」首条）；`P0-CK-mfg-001` 同型核查 = **非同型**（qa `createForBusinessBill` 无 findByRelatedBill 幂等短路、每次新建，不存在固定幂等键吞增量；qa 侧对应缺陷是并发双建，登记 P3-CK-qa-017）；`P1-CK-pur-003` CRUD 无守卫族 = 同型登记（P2-CK-qa-006）。

## 发现清单

> 状态初始 `open`；修复阶段逐项测试验证后回填 `ai-check-index.md`。

### P1-CK-qa-001（D7/D6）SPC 计量型采样幂等键只覆盖每子组首点——第二次起调度将已采样点重混成「幻影子组」，样本表数据污染 → 控制限漂移 → 假失控 → 假 NCR/CAPA

- **控制点**：`app/erp/qa/service/spc/SpcSamplingService.java#collectSamples`（L156 候选键：`String key = inspection.getCode() + "#" + safeLineNo(line) + "#" + value.toPlainString();`——每个点用**自己的 inspection code**）对照 `#buildSampledKeys`（L413-434：存量键从 `s.getSourceCode()#s.getSourceLineCode()` + **该子组全部 measuredValues** 重建——而 `setSourceCode/setSourceLineCode` 只记**子组首点**的 inspection code/lineNo（L201-203））。候选点 2..N 的键（`INS2#1#v2`）与存量重建键（`INS1#1#v2`，inspection code 是首点的）**永不匹配**。
- **证据**：推演 10 点（2 个满子组，subgroupSize=5）场景：第 1 次 collect → 2 个 sample（sourceCode=INS1 / INS6）；第 2 次运行候选键仅 INS1#1#v1、INS6#1#v6 命中（各子组首点），其余 8 点全部进入 pendingPoints → `offset+5<=8` → **创建第 3 个幻影子组**（混装 INS2-5+INS7 的旧点）；第 3 次起每轮再新增约 1 个幻影组直至所有候选点都当过某子组首点。存量测试 `TestErpQaSpcSampling#collectSamplesIsIdempotent`（L90-99）**碰巧通过**：seed 恰 5 点成 1 子组，二次运行 4 个未命中 pending < subgroupSize=5，凑不满新子组返回 0——是算术巧合而非幂等成立；20 子组用例（L116-117）只跑一次从未复跑。
- **问题**：SPC 采样调度（`erp-qa-spc-sampling` job 启用后每小时跑）从第二轮起持续把历史已采样点重复装进新子组：同一点被计入多个子组 → grandMean/averageRange 失真 → 控制限漂移 → 规则评估假失控 → `SpcOutOfControlHandler` 自动建假 NCR(sourceType=SPC)+CAPA（RC-R1.26 已把 evaluate 接进调度链，本缺陷的爆炸半径随之扩大）。属数据污染型正确性缺陷，job 默认 disabled（与 P1-RC-042 同一严重性校准）→ P1。
- **建议修复方向**：幂等键改按「点」对称重建——存量侧需为 measuredValues 中每个值记其来源（当前 schema 只存首点三元组，无法反推每点来源）；最小可行修复 = 存量键改用 `(chartId, 全体 inspection code 集合)` 子组级比对，或候选侧记录已入组 inspectionId 集合跳过整组；修复时补「≥2 子组二次采样」回归测试。
- **arm-index 裁决**：新增（arm-index grep「幂等键/SpcSample/sampledKeys」零命中；P1-RC-042 是漏调 evaluateRules 的接线断点，本条是采样幂等键本体缺陷，不同控制点）。

### P1-CK-qa-002（D3/D8）isInspectionCleared 全量 AND 语义使 REJECTED 永久阻塞——owner doc 设计的「复检新建质检单、以复检结果为准」路径无法解锁强制质检门

- **控制点**：`app/erp/qa/service/entity/ErpQaInspectionBizModel.java#isInspectionCleared`（L85-96：遍历该 bill **全部**质检单，`Objects.equals(result, REJECTED)` → `return false`——任一历史 REJECTED 即永久 BLOCKED）对照 owner doc `state-machine.md §3`「终态不可直接恢复；若需复检，**新建质检单**（经 `createForBusinessBill` 关联原单与业务单据）」+ `§4 异常路径`「复检结果与原检冲突 | **以复检结果为准**，原检记录保留（审计）」。
- **证据**：场景：强制质检 bill（`erp-qua.mandatory-inspection-bill-types` 含该类型）首检 REJECTED（如设备误判）→ 质检员按 owner doc §3 新建复检单 → 复检 ACCEPTED → `InspectionTrigger.enforceGate`（`erp-qa-dao/.../biz/InspectionTrigger.java` L42-49）→ `isInspectionCleared` 遍历 [REJECTED, ACCEPTED] → 命中 REJECTED → BLOCKED——**复检合格后业务单据仍永远放不了行**（passInspection 对 REJECTED 原单被 `result==PENDING` 守卫拒绝，无任何解锁路径；唯 cancelForBusinessBill 软删只针对 PENDING）。测试盲区：`TestErpQaInspectionStateMachine#testReinspectionViaNewIndependentInspection`（L194-209）注释声称「同一业务单据」，但 `seedInspection` helper（L308 `setRelatedBillCode("BILL-" + code.replace("INS-", ""))`）使原单/复检单落在**不同 billCode** 上，混合 [REJECTED, ACCEPTED] 同 bill 的 cleared 判定零覆盖。
- **问题**：`InspectionTrigger.enforceGate` 被 mfg/pur/sal 调用（config-gated 默认空=不强制）；启用后误判拒收的业务单据（复检合格场景）永久卡死，工单完工/入库流转循环断裂于设计的补救路径上。默认配置关闭（P1-RC-042 同校准）→ P1。
- **建议修复方向**：`isInspectionCleared` 语义改为「最新质检单（按 id/创建序）为 ACCEPTED/CONDITIONAL 即放行」（对齐 §4「以复检结果为准」），或 REJECTED 计数仅在无后续质检单时阻塞；修复时补同 bill 混合结果回归测试（复用该场景修测试 seed）。
- **arm-index 裁决**：新增（A2.12/P1-MA2-064/P1-RC-041 均未覆盖「复检后解锁」维度；grep「isInspectionCleared 复检/REJECTED 永久阻塞」零命中）。

### P1-CK-qa-003（D4）spc-capability.batch.xml 简单名 inject 运行时必失败——启用能力分析 job 即每 chunk 抛 ERR_IOC_UNKNOWN_BEAN_FOR_NAME，自动调度链断裂（P1-RC-042 同族，RC-R1.26 已 watch-only 登记未修）

- **控制点**：`module-quality/erp-qa-service/src/main/resources/_vfs/nop/batch-task/qa/spc-capability.batch.xml` L21 `const biz = inject('IErpQaSpcCapabilityBiz');` 对照 bean 实际注册 id `_service.beans.xml` L96-101（`app.erp.qa.service.entity.ErpQaSpcCapabilityBizModel` 与 `biz_ErpQaSpcCapability`——**均非** `IErpQaSpcCapabilityBiz`）+ RC-R1.26 plan 运行时实证（`docs/plans/2026-08-14-2304-2-rc-mr1-r1-26-qa-spc-evaluate-wiring.md` L71 Proof 项：`BeanContainerImpl.getBean(name)` 仅按 bean id 精确匹配，同型简单名 `inject('spcSamplingService')` 实测抛 `ERR_IOC_UNKNOWN_BEAN_FOR_NAME`）+ job 接线 `app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-qa-spc-capability.job.yaml`（`nop.job.erp-qa-spc-capability.enabled` 默认 false，cron 每日 02:00）。
- **证据**：RC-R1.26 修复只改了 `spc-sampling.batch.xml`（三段 inject 全改 FQCN）；同 plan L171/L175 明确 watch-only 登记「`spc-capability.batch.xml` 同型简单名注入 latent 缺陷（job 默认 disabled 未暴露，非本行范围）」——arm-index **未立案**（仅在 plan/日志中注明）。一旦部署侧启用该 job：每个 chunk 的 processor 执行到 `inject('IErpQaSpcCapabilityBiz')` 即抛异常 → 全部能力分析 chunk 失败 → UC-QA-10 周期 Cpk 计算生产调度路径不可达（行为等价于 P1-RC-042 修复前状态，另一 batch 文件）。
- **问题**：调度链完整性（D4/B3.1）——声明与注册不匹配的接线断点，且 owner doc `docs/architecture/job-scheduling.md` L208 仍标注该 job「（待实现）DESIGN」（文档漂移：实现已存在但断裂）。
- **建议修复方向**：`inject('IErpQaSpcCapabilityBiz')` 改为注入已注册 bean id（`biz_ErpQaSpcCapability` 或 FQCN BizModel id，对齐 spc-sampling 修复范式）；同步 job-scheduling.md L208 行状态。
- **arm-index 裁决**：新增（P1-RC-042 的同族姊妹站点——RC-R1.26 plan 内 watch-only 注记不构成 arm-index 立案，本 mission 按 mission 指令「核查其他 batch.xml 同型漏调」独立登记）。

### P1-CK-qa-004（D6/D1）autoCreateNcrFromInspection 用 Integer 20 写 String 字典列 severity——主路径自动 NCR 的 severity 恒为非法字典值 "20"，UI 映射失效并污染召回升级与报表聚合

- **控制点**：`app/erp/qa/service/entity/NcrLifecycleService.java#autoCreateNcrFromInspection`（L63-64 注释「severity 字段 mandatory：默认 NORMAL(20)」+ `ncr.orm_propValueByName("severity", 20);`）对照 ORM `app-erp-quality.orm.xml` L364（`severity` VARCHAR(20)、`ext:dict="erp-qa/severity"`）+ dict 定义（orm L58-63：valueType=string，值 **LOW/NORMAL/HIGH/CRITICAL** 字符串，**无 10/20/30/40 数数码**）+ 生成类 `_ErpQaNonConformance.getSeverity()` 返回 `java.lang.String` + 平台 `OrmEntity#orm_propValueByName` → `BeanTool.instance().setProperty`（nop-entropy `io/nop/orm/support/OrmEntity.java:444-446`，Integer→String 属性经转换落库为 "20"，不抛错）。同文件正确示范：`SpcOutOfControlHandler` L40-44 用 `SEVERITY_NORMAL = "NORMAL"` 等字符串常量。
- **证据**：REJECTED→自动 NCR 是 NCR 的主产生路径（`ErpQaInspectionRecordResultProcessor` L78-80 与 `ErpQaInspectionFailInspectionProcessor` L33 两处触发）。落库 severity="20"：① NCR 列表/详情 severity 字典映射失败（前端显示裸 "20"）；② `ErpQaNonConformanceUpgradeToRecallProcessor#upgradeToRecall` L50-52 `String severity = ncr.getSeverity()` 原样继承 → `recall.severityLevel="20"`（erp-qa/recall-severity 同为字符串字典，`ErpQaRecallRegisterProcessor.applyRecallFields` L66-67 无字典校验直接落库）——升级链全线污染；③ `ErpQaReportBizModel#buildNcrCapaSummaryDataset` 按 severity 聚合出现 "20" 桶。测试盲区：`TestErpQaInspectionStateMachine` L168-177 断言 NCR status/sourceType 但**零 severity 断言**（全绿基线因此未暴露）；RC-R1.26 批测快照中 SEVERITY=HIGH 来自 SPC 路径（字符串常量，合法），与本案无关。
- **问题**：字典值类型语义错误（作者按数值码 10/20/30/40 心智模型写死 20，实际 dict 是字符串枚举）——主路径每张自动 NCR 均携带非法 severity，用户可见正确性错误 → P1。
- **建议修复方向**：改为 `ncr.setSeverity(ErpQaConstants.SEVERITY_NORMAL)`（新增 NCR severity 字符串常量，对齐 SpcOutOfControlHandler）；upgradeToRecall 的码值对齐注释（L50「=10/20/30/40 码值对齐」）一并修正；补 severity 断言回归测试。
- **arm-index 裁决**：新增（arm-index grep「severity 20/dict 值类型」零命中；P2-RC-044 是 SPC 路径 severity 映射维度，不同控制点）。

### P1-CK-qa-005（D3/D5）Recall reverseApprove 双轴不联动——审批撤销后 status 仍 APPROVED：重提死锁 + 已撤审召回仍可定位目标/生成退货，绕过强制审批门

- **控制点**：`app/erp/qa/service/processor/ErpQaRecallProcessor.java#doReverseApprove`（L166-171：只 `setApproveStatus(REJECTED)` + 清 approvedBy/At，**不回写 status**——对照正向 `#doApprove` L150-156 双轴联动写 `approveStatus=APPROVED + status OPEN→APPROVED`）+ `#validateBusinessRulesForSubmit`（L124-126：`requireRecallStatus(recall, RECALL_STATUS_OPEN)`）+ `ErpQaRecallStateMachine#assertCanLocateTargets`（仅查 `status==APPROVED`，不查 approveStatus）。
- **证据**：① 死锁：reverseApprove 后 status=APPROVED + approveStatus=REJECTED → 重提链 `assertCanSubmit(REJECTED)` 通过但 `validateBusinessRulesForSubmit` 要求 status=OPEN → 抛非法迁移——审批轴允许的「驳回后重新提交」在组合层死锁（P1-CK-mfg-002 同构）；② 审批门绕过：status 仍 APPROVED → `locateTargets`（守卫只看 status）照常执行 → IN_PROGRESS → notifyCustomers/generateReturns 全链放行——**审批已被撤销的召回事件继续执行目标定位与批量销售退货**，违背 `recall.md 业务规则 4`「所有召回 APPROVED 才能执行（高风险，防误召回造成商誉损失）」。且 reverseApprove 无 status 前置守卫（IN_PROGRESS 中途仍可撤审，同 P2-CK-mfg-010 方向）。
- **问题**：召回是 owner doc 明示的高风险强制审批流（CRITICAL 级），撤销审批不收回执行资格属审批语义反转缺陷。触发面需显式 reverseApprove 动作（罕见）+ job 不涉及 → P1（不升 P0：无数据损坏、需显式人工触发）。
- **建议修复方向**：`doReverseApprove` 镜像回写 `status=OPEN`（与 doApprove 双轴联动对称）；`assertCanLocateTargets`/`validateTransitionForReverseApprove` 补 approveStatus=APPROVED 组合校验（或 status 白名单排除已执行中状态）。
- **arm-index 裁决**：新增（带同族注记）——P1-CK-mfg-002（双轴互锁死锁）+ P2-CK-mfg-010（reverseApprove 无单据状态守卫）同族，qa 召回站点新增「审批门可绕过」独立危害面（arm-index grep「reverseApprove recall/召回 撤审」零命中）。

### P2-CK-qa-006（D5/D3，同型 P1-CK-pur-003 族）通用 CRUD update/delete 无状态守卫——质检单/行/NCR/Action/Recall/SPC 全实体裸 CrudBizModel，终态与已过账数据可直改

- **控制点**：`app/erp/qa/service/entity/` 下 `ErpQaInspectionLineBizModel`/`ErpQaNonConformanceBizModel`/`ErpQaActionBizModel`/`ErpQaRecallTargetBizModel`/`ErpQaSamplingPlanBizModel`/`ErpQaCalibrationBizModel`/`ErpQaQualityGoalBizModel`/`ErpQaReviewBizModel`/`ErpQaRiskRegisterBizModel`/`ErpQaSpcSampleBizModel` 等 15-22 行裸 `CrudBizModel<T>`（零 `defaultPrepareSave/Update/Delete` 覆写）；`ErpQaInspectionBizModel`/`ErpQaNonConformanceBizModel`/`ErpQaRecallBizModel` 虽有命名动作链，但通用 `save_`/`update_`/`delete_` 仍对任何状态开放。
- **证据**：REJECTED 终态质检单可经 `ErpQaInspection__update_` 直改 `result=ACCEPTED`（绕过 recordResult 的 PENDING 守卫 + 关键项否决 + posted + NCR 触发——把 P0-MA2-017 修掉的 silent flip 从 CRUD 侧开了回来）；`ErpQaInspectionLine__update_` 直改 measuredValue/result 绕过 `InspectionResultEvaluator`；已过账 NCR（posted=true）可直改 quantity（重过账金额失真）；`ErpQaSpcSample__update_` 直改 mean/isOutOfControl。
- **问题**：同 P1-CK-pur-003/P1-CK-sal-004/P1-CK-inv-005/P2-CK-mfg-006 全域同型——命名动作链的状态守卫可被通用 CRUD 旁路；qa 侧额外威胁强制质检门控基线（终态 result 可改写）。按 mission 约定登记为同型站点。
- **建议修复方向**：`defaultPrepareUpdate/Delete` 守卫终态/已过账（result 轴 isTerminal + posted=true 拒改）；InspectionLine/SpcSample 建议行级只随主单动作写。
- **arm-index 裁决**：同型登记（P1-CK-pur-003 族，qa 站点新增计数）。

### P2-CK-qa-007（D5）recordResult 让步路径同事务自动「自审通过」——记录结果者即让步审批人，owner doc「质量主管审核」简化语义未落地

- **控制点**：`app/erp/qa/service/processor/ErpQaInspectionRecordResultProcessor.java#recordResult`（L70-74：`concession && aggregated==CONDITIONAL` 时 `inspection.setApproveStatus(approvalStateMachine.concessionApproveTargetStatus())`（=APPROVED）+ `inspection.setApprovedBy(context.getUserId())` + `setApprovedAt(...)`——**同一 mutation 内、同一操作者**，录结果与让步审批一步完成）对照 owner doc `state-machine.md §实现约定`「让步接收审批流（简化）：本期以 approveStatus=APPROVED（**质量主管审核**）简化」+ `inspection-integration.md §三`（让步流程：质检员提建议 → 质量主管审批 → CONDITIONAL）。
- **证据**：`ErpQaInspectionBizModel` 全部 mutation 中无独立让步审批动作（pass/fail/recordResult/batchPass/cancel/create/findByRelatedBill/isInspectionCleared——无 approve 入口）；调用 `recordResult(allowConcession=true)` 的任何用户（质检员）即自动成为 approvedBy。无 SoD 守卫（对照 mfg `SoDGuard.assertApproverNotCreator` 范式）。关键项否决（RC-R1.58）挡住关键项让步，但**非关键项让步完全自审**。
- **问题**：让步接收是 owner doc §6 列明的危险操作（降级使用质量风险、需主管审批）——审批形同虚设（audit 字段 approvedBy=记录者本人）。降级依据：本期为声明的「简化」范畴，缺陷在于简化掉了「审核人」这一最低语义。
- **建议修复方向**：拆出独立 `approveConcession` mutation（质量主管角色 + SoD：approvedBy ≠ result 记录者）；或最低限度 recordResult 置 approveStatus=SUBMITTED、审批人后续单独动作补 APPROVED。
- **arm-index 裁决**：新增（A2.12/P1-RC-040 未覆盖让步审批角色维度；grep「让步 自审/allowConcession 审批人」零命中）。

### P2-CK-qa-008（D6/D10）classifyByCpk(null) 返回 INADEQUATE——规格限缺失/σ=0 致 Cpk 不可算时误判最差级，触发 QualityGoal 清值 + 每日 RiskRegister 噪音

- **控制点**：`app/erp/qa/service/spc/SpcCapabilityCalculator.java#classifyByCpk`（L265-267：`if (cpk == null) return INADEQUATE;`）+ `#calculateCapability`（L151-156：`chart.getSpecMax()/getSpecMin()` 任一为 null → `computeCp/computeCpk` 返回 null；`averageRange` 为 null → withinStdDev null → cpk null）+ L183-193（`!isAttributes && INADEQUATE` → `writeBackQualityGoal` + `registerRisk`）+ `#writeBackQualityGoal` L292（`goal.setCurrentValue(cpk == null ? null : ...)`——cpk null 时**把目标当前值清成 null**）。
- **证据**：SPC 控制图 `specMin/specMax` 非强制（orm L771-772 无 mandatory）——无规格限的计量型 chart：每日能力分析 → cpk=null → level=INADEQUATE → ① QualityGoal.currentValue 被置 null（如该 goal 有历史值则被清除）；② 每日新建 `RISK-SPC-{chart}-{yyyyMMdd}` 风险行（UK 含日期 → 每天一行噪音）。owner doc/spc.md 无「不可算 = 最差级」语义（UC-QA-10 分档仅定义在有 Cpk 值时）。计数型分支 capabilityLevel=null 跳过触发（L183 有 isAttributes 判断）——计量型 null 却走触发，不对称。
- **问题**：null（不可计算）与最差（能力不足）语义混同，副作用（goal 清值 + 风险登记）在不可算场景误触发。与 P2-RC-045（名称约定回写 + 硬编码值 + 30 天窗口）同控制点不同维度。
- **建议修复方向**：`classifyByCpk(null)` 返回 null（不可分级），触发条件补 `capabilityLevel != null`；writeBackQualityGoal 对 null cpk no-op。
- **arm-index 裁决**：新增（带复用注记 P2-RC-045 同控制点不同缺陷维度，P2-RC-045 watch-only todo 不含 null 分级）。

### P2-CK-qa-009（D9）SPC 采样每小时全量加载历史 + 逐行 getEntityById N+1——findSamples 全样本 JSON 重解析 + resolveInspection 循环单查 + findApprovedInspectionLines 无时间窗全表扫

- **控制点**：`app/erp/qa/service/spc/SpcSamplingService.java#collectSamples`（L152 `resolveInspection(line.getInspectionId())` 在候选循环内逐行 `getEntityById`——N+1；L142 `findSamples(chartId)` 每次**全量**加载该 chart 全部历史样本；L413-434 `buildSampledKeys` 对每个存量样本 JSON 解析 measuredValues）+ `#findApprovedInspectionLines`（L366-404：`eq("parameterId", parameterId)` 无时间窗/无上限——参数行随检验量线性增长，每运行全表载入）。
- **证据**：调度 `erp-qa-spc-sampling` 每小时对每张 active chart 执行：全参数行扫描 + 全历史样本实体加载 + 全样本 JSON 反序列化 + N 候选 × 1 主单查询。运行 1 年的 chart（如 8760 样本 × n=5 值）每次调度解析 4.4 万个 JSON 值。`SpcControlLimitCalculator`/`SpcRuleEngine`/`SpcCapabilityCalculator.findSamplesInRange` 同型全量加载（recalculate/evaluate 每小时全样本）。
- **问题**：D9 性能缺陷（P1-MA2-086 已登记 job 并发幂等维度，本条是查询维度）——SPC 调度开启后成本随历史线性上升，小时级 cron 不可持续。
- **建议修复方向**：幂等增量窗口（仅扫 `createTime > max(sampleTime)` 之后行）；findSamples 只取 max(subgroupNo) 与增量；resolveInspection 批量 `in("id",...)` 预载。
- **arm-index 裁决**：新增（P3-CK-mfg-014 同型家族但量级不同——qa 是每小时调度 × 全历史，独立登记 P2）。

### P2-CK-qa-010（D6/D5）SPC 系数表只覆盖 n=2..10 且越界静默回落 n=10；subgroupSize=1 被 `subgroupSize < 2` 拒绝——subgroupSize>10 控制限偏窄、X_MR 单值图不可采样

- **控制点**：`app/erp/qa/service/spc/SpcControlLimitCalculator.java`（L48-79 D2/D3/D4 表仅 n=2..10；L235-242 `lookupD2` 越界「回落 n=10（保守）」**无日志**）+ `SpcSamplingService#collectSamples`（L122-126 `subgroupSize < 2` 抛 `ERR_QA_SPC_SUBGROUP_SIZE_INVALID`）对照 ORM `subgroupSize` INTEGER 无上界校验（L773 默认 5）+ chartType 字典含 X_MR（spc.md L22「X_BAR_R/X_BAR_S/X_MR/P/NP/C/U」）。
- **证据**：① `subgroupSize=20` 的 chart：d2 取 3.078（真值 ≈3.735）→ sigmaHat 低估 ≈17% → 控制限偏窄 → 假失控概率上升（规则 1 误报）——静默错误常数；② X_MR（单值-移动极差图）语义要求 n=1，采样入口 `subgroupSize < 2` 直接拒绝 → X_MR 类型 chart 一开调度每 chunk 抛错（batch processor 无 per-item try/catch，chunk 失败）。
- **问题**：D6 判定边界（系数表覆盖域未校验）+ D5 入参边界（图类型与子组大小约束冲突）。
- **建议修复方向**：subgroupSize 保存/调度入口校验 2..10（超界抛业务错误而非静默回落）；X_MR 明确支持路径（n=1 采样 + 移动极差控制限）或 owner doc 显式降级 Non-Goal 并在入口拒绝 chartType=X_MR 的采样。
- **arm-index 裁决**：新增（A2.12 SPC 组件审查与 A1.33 均未覆盖系数表边界维度）。

### P2-CK-qa-011（D8，同型 P2-CK-fin2-007/P2-CK-mfg-007 orgId 隔离族）NCR 报废计价与退货编排按 materialId 裸 limit 1 取任意库存余额行——多仓/多组织下金额与仓/币别任意性

- **控制点**：`app/erp/qa/service/posting/NcrPostingDispatcher.java#resolveStockBalance`（L117-131：`eq("materialId", materialId)` + `setLimit(1)` **无 orgId/warehouseId 过滤、无排序**——同物料多余额行时取任意一行）+ `NcrReturnOrchestrator#findStockBalance`（L131-141 同型）+ `#dispatchScrap`（L68-75：scrapAmount = quantity × 该任意行 avgCost；currencyId/warehouseId/orgId 均取自该行）。
- **证据**：inventory `ErpInvStockBalance` UK 为 (orgId, materialId, skuId, warehouseId, locationId, batchNo, ownerId)（A2.18）——同物料跨仓/跨组织必然多行。NCR（来源业务单据自带 orgId/warehouseId 语境）报废凭证金额按无序 limit 1 的行计价：不同执行时刻可能取到不同仓的均价 → 同一 NCR 重放金额漂移；退货单 warehouseId/currencyId 同样任意。
- **问题**：跨组织隔离维度（P2-CK-mfg-007 同族）+ 确定性缺陷（无 ORDER BY 的 limit 1）。对照正确示范：mfg 过账链按 `wo.getOrgId()` + `resolveAcctSchemaId(orgId)` 解析。
- **建议修复方向**：入参与 NCR 关联上下文（inspection.warehouseId / 来源单 orgId）过滤余额查询；多行命中时按来源仓精确匹配优先 + 兜底明确排序 + LOG.warn。
- **arm-index 裁决**：新增（orgId 族新站点 + 任意行确定性维度；P2-RC-086 是 dashboard 读路径维度，不同控制点）。

### P2-CK-qa-012（D7/D2，同型 P2-CK-inv-012/P1-CK-pur-002/P2-CK-mfg-011 族）NCR resolve 的 SCRAP 凭证 REQUIRES_NEW 先于主事务后续步骤提交——中途失败留孤儿凭证

- **控制点**：`app/erp/qa/service/processor/ErpQaNonConformanceResolveProcessor.java#resolve`（L49 `ncrDao().updateEntity(ncr)` 主事务内 → L52 `dispatchFinancialImpact` → `dispatchScrap` → `executor.postEvent`（`NcrPostingExecutor` L15-16 javadoc 自证「事务边界由 Facade `IErpFinVoucherBiz.post()` 的 `@Transactional(REQUIRES_NEW)` 承接」）→ L70 `ncrDao().updateEntity(ncr)`（posted 三件套写回，**仍在主事务**——此后乐观锁冲突/RETURN 编排异常均回滚主事务但不回滚已提交凭证）。
- **证据**：主事务回滚时序：NCR 状态回 IN_REVIEW、posted=false，但 NCR_SCRAP 凭证已入 GL——孤儿凭证无对应 RESOLVED NCR；用户重试 resolve → dispatchScrap 再次过账（除非 finance post 幂等命中——见 P2-CK-qa-013 的 null 语义冲突使幂等命中时 posted 永不置位）。与 mfg 完工链/inv doComplete 完全同构（全域第 N 站点）。
- **问题**：同型登记（P2-CK-inv-012 族 qa 站点）。RETURN 编排（`purReturnBiz.save` 同事务）无此问题——仅 SCRAP 过账受影响。
- **建议修复方向**：与 inv-012/mfg-011 联合裁决（过账移 afterCommit 或接受范式 + 对账告警）。
- **arm-index 裁决**：同型登记（P2-CK-inv-012 族，qa 站点新增计数）。

### P2-CK-qa-013（D8，同型 P1-CK-fin-003 族）dispatchScrap 以 `voucherId != null` 判成功置 posted——post() 幂等命中返回 null 时 posted 永不置位，postNcr 重试静默无效 + reverseNcr 守卫封死

- **控制点**：`app/erp/qa/service/posting/NcrPostingDispatcher.java#dispatchScrap`（L76-82 `String voucherId = executor.postEvent(event); if (voucherId != null) { ncr.setPosted(TRUE) ... }`）+ 调用方 `ErpQaNonConformancePostNcrProcessor#postNcr`（L28-30：`posted=true` 才拒重复；posted=false 则放行重派）+ `ErpQaNonConformanceReverseNcrProcessor#reverseNcr`（L28-30：`!posted` 抛 `ERR_NCR_NOT_POSTED`）。
- **证据**：P1-CK-fin-003 已实证 finance `post()` 幂等命中（同 businessType+billHeadCode 凭证已存在）返回 null——qa dispatcher 把 null 当失败不置 posted：resolve 事务在 dispatchScrap 之后回滚（见 P2-CK-qa-012）→ 凭证已提交 + posted=false → 人工 `postNcr` 重试 → dispatchScrap 幂等命中返回 null → 依旧不置 posted → postNcr「成功」返回但 posted 永远 false、reverseNcr 永久 `ERR_NCR_NOT_POSTED` 不可冲销。
- **问题**：P1-CK-fin-003 的 qa 站点（全域 dispatcher +1）。
- **建议修复方向**：随 fin-003 三态化（幂等命中显式返回「已存在」语义）联动修复。
- **arm-index 裁决**：同型登记（P1-CK-fin-003 族，qa 站点）。

### P2-CK-qa-014（D8）NcrReturnOrchestrator 创建的采购/销售退货单为无行空壳——仅头字段 + remark，无物料/数量行，NCR.returnCode 登记的是一张「无物可退」的草稿

- **控制点**：`app/erp/qa/service/posting/NcrReturnOrchestrator.java#createPurchaseReturn`（L90-99：data 仅 code/supplierId/warehouseId/currencyId/businessDate/docStatus/approveStatus/remark——**无 `lines` 键**、无 materialId/quantity）+ `#createSalesReturn`（L109-118 同型）对照同仓正确示范 `ErpQaRecallGenerateReturnsProcessor#createSalesReturnFor`（L60-76：带 lines[{materialId, uoMId, quantity, reason}]）。
- **证据**：NCR（来料不合格，quantity/materialId 明确）resolve disposition=RETURN → 生成 DRAFT 退货单零行——下游审批/过账无行可处理；采购员须从 remark「NCR退货:{code}」反查 NCR 手工补行。owner doc `state-machine.md §NCR 财务影响规则`「退货 → 生成红字入库凭证（冲销原入库暂估）」的金额载体缺失（退货单自带红字过账按行计量——无行即零凭证）。
- **问题**：跨域编排产出不完整单据（有头无行），闭环退化为「占位提醒」。降 P2 依据：单据 DRAFT/UNSUBMITTED 无财务副作用、人工可补。
- **建议修复方向**：对齐 recall generateReturns 范式带 lines（NCR.materialId + NCR.quantity + 单位从余额/物料解析）；或 owner doc 显式裁决「仅占位草稿」并补 UI 提示。
- **arm-index 裁决**：新增（arm-index grep「退货单 无行/空壳」零命中）。

### P3-CK-qa-015（D5）resolve 的 RETURN 编排不受 ncr-posting-mode 门控——MANUAL_POST 模式下仍自动创建退货草稿（且 postNcr 拒绝 RETURN，无人工替代入口）

- **控制点**：`app/erp/qa/service/processor/ErpQaNonConformanceResolveProcessor.java#dispatchFinancialImpact`（L67-75：`isScrap` 分支有 `ErpQaConfigs.isNcrAutoPosting()` 门控，`isReturn` 分支**无任何模式判断**直接 `orchestrateReturn`）+ `ErpQaNonConformancePostNcrProcessor#postNcr`（L31-36：非 SCRAP 抛 `ERR_NCR_DISPOSITION_NOT_POSTABLE`——MANUAL_POST 下 RETURN 无人工触发入口）。
- **证据**：owner doc `state-machine.md §实现约定`「`resolve` 按 `erp-qua.ncr-posting-mode`（AUTO_POST/MANUAL_POST）config-gated 分派，`postNcr`/`reverseNcr` 提供人工入口」——实现仅 SCRAP 遵守；MANUAL_POST 部署下 resolve 仍自动编排退货（意外草稿创建）。影响轻（DRAFT 无财务副作用）→ P3。
- **建议修复方向**：RETURN 分支补 `isNcrAutoPosting()` 门控（人工入口同步在 postNcr 支持 RETURN 或明确 owner doc 语义）。
- **arm-index 裁决**：新增。

### P3-CK-qa-016（D3）escalateToRecall 终态死胡同——翻 ESCALATED_TO_RECALL 但不建召回事件，此后 upgradeToRecall 被 IN_REVIEW 守卫永久拒绝

- **控制点**：`app/erp/qa/service/entity/ErpQaNonConformanceBizModel.java#escalateToRecall`（L88-100：仅 `setStatus(ESCALATED_TO_RECALL)`，L96 注释自认「仅状态迁移占位；不建召回实体。真正建召回用 upgradeToRecall」）+ `ErpQaNonConformanceStateMachine#assertCanUpgradeToRecall`（仅 IN_REVIEW 合法）。
- **证据**：两个入口并存于同一 BizModel：用户误调 `escalateToRecall` → NCR 终态 ESCALATED_TO_RECALL 且**无任何 Recall 实体**（recall.md「NCR status=ESCALATED_TO_RECALL 升级触发召回登记」的登记侧缺失）→ 再调 `upgradeToRecall` 抛非法迁移（status 已非 IN_REVIEW）——召回事件不可再创建（只能对该 NCR 永久放弃召回）。该方法是 owner doc §实现约定「召回 Non-Goal 仅状态迁移」时代的遗留，召回落地（2.11 已实现）后成为死胡同入口。
- **问题**：API 面残留的旧占位入口与正道互斥。P3（有 upgradeToRecall 正道、需误用触发）。
- **建议修复方向**：删除 `escalateToRecall`（对齐 reInspect 删除先例 P0-MA2-017 方案 A）或改为委托 upgradeToRecall 的别名。
- **arm-index 裁决**：新增。

### P3-CK-qa-017（D7）enforceGate check-then-create 无并发防护——同单并发流转双建 PENDING 质检单（P0-CK-mfg-001 同型核查结论：非同型，qa 为重复创建）

- **控制点**：`module-quality/erp-qa-dao/src/main/java/app/erp/qa/biz/InspectionTrigger.java#enforceGate`（L42-47 `findByRelatedBill` 为空 → `createForBusinessBill`——无锁、`erp_qa_inspection` 无 (relatedBillType, relatedBillCode, result) UK（ORM 仅 UK_QA_INSPECTION_CODE_ORG））。
- **证据**：同 bill 两次并发流转（如双端同时 confirm）：两事务均见 existing 空 → 均建 PENDING 单 → `isInspectionCleared` 要求全部非 PENDING → 质检员须对两张重复单都录结果。对照 mfg P0-CK-mfg-001（固定幂等键**吞增量**）：qa 是**重复创建**（无静默丢失），危害轻 → P3。
- **建议修复方向**：(relatedBillType, relatedBillCode, result=PENDING) 层面查重后建（或 DB 部分索引/显式锁）；同型双建的 `generateCode` 毫秒级碰撞（`"INS-"+billType+"-"+currentTimeMillis()`，UK_QA_INSPECTION_CODE_ORG 下同型同毫秒双建直接 DB 报错）一并收敛（序号/序列化编码）。
- **arm-index 裁决**：新增（带 P0-CK-mfg-001 非同型注记）。

### P3-CK-qa-018（D5/D8）recordResult 的 posted 三件套只写 posted 不写 postedAt/postedBy——与 passInspection/failInspection 的 markPosted 路径不一致

- **控制点**：`app/erp/qa/service/processor/ErpQaInspectionRecordResultProcessor.java#recordResult`（L69 `inspection.setPosted(Boolean.TRUE);` 裸写）对照基类 `AbstractErpQaInspectionProcessor#markPosted`（L70-74 posted+postedAt+postedBy 三件套）与 `ErpQaInspectionPassInspectionProcessor` L26 / `ErpQaInspectionFailInspectionProcessor` L31（均走 markPosted）。
- **证据**：recordResult 判定终态的质检单 postedAt/postedBy 恒 null——审计链（谁在何时过账该检验结论）缺半。owner doc §silent flip 守卫注记要求「设 posted=true」未细化三件套，但 NCR 侧 posted 三件套（state-machine.md §实现约定）是全域范式。
- **问题**：审计字段不一致（P3 级数据质量）。**建议**：改用 `markPosted(inspection, context)`。
- **arm-index 裁决**：新增。

### P3-CK-qa-019（D4/D1）死配置常量 + batch.xml 内 `java.time.LocalDate.now()`——`erp-qa-spc-sampling-cron`/`spc-capability-cron`/`spc.enabled` 零消费；实际门控为 nop.job.* 双层键

- **控制点**：`app/erp/qa/service/ErpQaConfigs.java`（L107-121 `getSpcSamplingCron`/`getSpcCapabilityCron`/`isSpcEnabled`——grep 全 service 零消费）对照实际接线 `erp-qa-spc-sampling.job.yaml`（`nop.job.erp-qa-spc-sampling.enabled|false` + `cron-expr|0 0 * * * ?`）+ `spc-capability.batch.xml` L22 `const periodTo = java.time.LocalDate.now();`（生产路径直接取系统时钟，绕过 `CoreMetrics` 可控时间基线——checker 只扫 Java 文件故未计入 R7 基线）。
- **问题**：同型 P3-CK-mfg-013/P3-CK-inv-021 家族（死常量 + 键漂移）+ batch XML 时间源不可控。30 天窗口硬编码已归 P2-RC-045（复用不重复）。
- **建议修复方向**：删死常量；batch XML 改 `inject` 平台时钟 bean 或经 biz 参数传入 periodTo。
- **arm-index 裁决**：新增（同型家族；30 天窗口维度复用 P2-RC-045）。

### P3-CK-qa-020（D9，同型 P3-CK-mfg-014 家族）dashboard/report 全实体载入内存聚合——期间全量检验单/NCR + 全量非完成 CAPA 内存过滤 + distinct chart 全载

- **控制点**：`app/erp/qa/service/dashboard/ErpQaDashboardBizModel.java`（`loadInspectionsInRange` L352-358 无界 findAllByQuery；`findCapaOverdueAlert` L174-192 `ne("status", COMPLETED)` 全载后内存 `due.isBefore(cutoff)` 过滤——可下推 `lt("dueDate", cutoff)`；`countOutOfControlCharts`/`countInadequateCapabilityCharts` L285-294/L331-340 全载实体内存 distinct——可 GROUP BY）+ `report/ErpQaReportBizModel`（`loadInspections`/`loadNcrs` L325-338 同型无界）。
- **问题**：数据量增长后看板/报表刷新线性变贵（对照 `findDefectTopN` 已用 DB GROUP BY 正确示范）。纯性能。orgId 缺失维度归 P2-RC-086（复用不重复登记）。
- **建议修复方向**：条件/聚合下推（QueryFieldBean count+distinct、lt 过滤）。
- **arm-index 裁决**：同型登记（P3-CK-mfg-014 家族，qa 站点）。

### P3-CK-qa-021（D2/D10，同型全域族）currentUserId/resolveUserId 宽 catch 返回 null 无日志 + findExistingSpcNcr 静默吞幂等预检失败

- **控制点**：`ErpQaRecallProcessor#currentUserId`（L197-206 `catch (Exception e) { return null; }` 无日志——影响 recall 审批审计字段）+ `NcrPostingDispatcher#resolveUserId`（L141-150 同型）+ `SpcOutOfControlHandler#findExistingSpcNcr`（L140-151 `catch (Exception e) { return null; }`——幂等预检失败被吞后必然走到 saveEntity，依赖 code UK 兜底重复）。
- **问题**：同 P3-CK-md-008/pur-010/sal-024/inv-018/fin2-012/mfg-016 全域同型——上下文/查询异常静默吞咽无可观测信号。
- **建议修复方向**：catch 补 LOG.warn；同型修复统一裁决。
- **arm-index 裁决**：同型登记（全域族，qa 站点）。

### P3-CK-qa-022（D6）NCR-CAPA 报表严重度排序误用 RECALL 字典常量（MEDIUM ≠ NORMAL）——NORMAL 行落入无序尾区；resolvedNcrCount 把 CANCELLED 计入（与 javadoc 不符）

- **控制点**：`app/erp/qa/service/report/ErpQaReportBizModel.java#buildNcrCapaSummaryDataset`（L294-298 `orderedSeverities = [RECALL_SEVERITY_CRITICAL, RECALL_SEVERITY_HIGH, RECALL_SEVERITY_MEDIUM, RECALL_SEVERITY_LOW]`——recall 字典含 MEDIUM 无 NORMAL，而 NCR `erp-qa/severity` 值为 NORMAL；L287-290 `RESOLVED || CANCELLED` 计入 resolvedNcrCount，javadoc L267 声明「status=RESOLVED」）。
- **证据**：NCR severity=NORMAL（最常见值）不在 orderedSeverities → 第一循环 `agg.get("NORMAL")=null` 跳过 → 第二循环追加在 CRITICAL/HIGH/MEDIUM/LOW 之后（MEDIUM 行恒空）——排序错乱 + 空行风险（MEDIUM 键 agg 中不存在故无空行，仅排序语义错）。
- **问题**：常量复用错字典 + 计数口径与文档不符。P3（报表展示层）。与 P1-CK-qa-004 叠加时 "20" 桶也落尾区。
- **建议修复方向**：定义 NCR severity 常量集（LOW/NORMAL/HIGH/CRITICAL）排序；resolved 口径与 javadoc 对齐（拆 cancelledCount 或改文档）。
- **arm-index 裁决**：新增。

### P3-CK-qa-023（D8）notifyCustomers 仅簿记不派发——「客户通知」无任何系统通知通道（notify 子系统未接入），notifyCustomer=true 纯声明

- **控制点**：`app/erp/qa/service/processor/ErpQaRecallNotifyCustomersProcessor.java#notifyCustomers`（L18-30：逐 target 写 notifiedAt/notifiedBy/returnStatus=NOTIFIED + recall.notifyCustomer=true——**无 `IErpSysNotificationBiz`/消息派发调用**；grep 全 quality service 通知派发零命中）。
- **证据**：owner doc recall.md 业务规则 3「客户通知是必备动作（Carbon "customer is immediately notified"）」——本实现把「通知」落地为「标记已通知」，无通道、无触达。M5 通知派发子系统（notify 域）已存在未被消费。severityLevel≥MEDIUM 的关闭门控因此校验的是一个纯自声明标志（调用即 true）。
- **问题**：合规动作退化为自证按钮。P3（owner doc 未显式要求系统通道；线下通知语义可辩）。
- **建议修复方向**：接 notify 子系统按 target.partnerId 派发召回通知后再置 NOTIFIED；或 owner doc 显式裁决「簿记 + 线下」。
- **arm-index 裁决**：新增。

### P3-CK-qa-024（D10/D5，带复用注记 P2-RC-040）InspectionTemplateMatcher 对 materialId=null 的调用退化为「任意 active 模板 limit 1（最旧 id）」——无物料业务单据复制到无关模板行

- **控制点**：`app/erp/qa/service/entity/InspectionTemplateMatcher.java#findActiveByMaterialAndType`（L53-59 `materialId != null` 才加过滤——两条件均可空；均为 null 时查询仅剩 `isActive=1` + `addOrderField("id", false)`（ASC 取最旧）+ `setLimit(1)`）+ 入口 `ErpQaInspectionCreateForBusinessBillProcessor#createForBusinessBill`（materialId 参数无 null 校验，`IErpQaInspectionBiz.createForBusinessBill` 契约允许 null）。
- **证据**：`createForBusinessBill(billType, billCode, null, type, ...)` → 匹配到系统里任意一张 active 模板 → 其模板行（规格/关键项）被复制进该质检单——检验项与被检对象无关，关键项否决基线（RC-R1.58）随之建立在无关规格上。P2-RC-040 已登记「类别级模板解析缺失」（两级 → 全局兜底主路径 OK），本条是其未覆盖的 null 入参退化维度。
- **建议修复方向**：materialId=null 时跳过模板匹配（返回 null → 无行人工补录）或入口前置校验。
- **arm-index 裁决**：新增（带 P2-RC-040 复用注记——同文件不同缺陷维度）。

### P3-CK-qa-025（D6/D10）NCR quantity 兜底 ONE——lotQuantity/sampleQuantity 均空时拒收数量记 1；SPC 失控 NCR quantity 恒 1

- **控制点**：`NcrLifecycleService#resolveRejectQuantity`（L70-78 `return BigDecimal.ONE;` 兜底）+ `SpcOutOfControlHandler#createNcrAndAction` L106（`ncr.setQuantity(BigDecimal.ONE)` 硬编码）。
- **证据**：quantity 是后续 SCRAP 过账金额的乘数（`dispatchScrap` L64-70 scrapAmount = quantity × avgCost）——兜底 1 使报废凭证金额在真实批量未知时按 1 单位计（有 warn 无的静默默认）；SPC NCR 的 quantity=1 与 chart 语境（失控子组涉及 n 个观测）无关。
- **问题**：数量兜底任意性（P3：触发需两数量字段均空/SPC 路径，金额偏差有 avgCost 语境限制）。**建议**：缺数量时 NCR 侧留 null 并在 dispatchScrap 前置校验（现有 `ERR_NCR_NO_QUANTITY` 已能拦截 null）或显式 config 默认。
- **arm-index 裁决**：新增。

## 验证为正确（显式排除，防误报）

- **P1-RC-042 修复在位（mission 指定核查项）**：`spc-sampling.batch.xml` L21-26 processor 段三段链完整 `collectSamples → recalculate → evaluate`，inject 全部 FQCN bean id（`app.erp.qa.service.spc.SpcSamplingService/SpcControlLimitCalculator/SpcRuleEngine`，与 `app-service.beans.xml` L57-64 注册一致）；批任务级测试 `TestErpQaSpcSamplingEvaluateBatch` 4 组在案（失控标记 + NCR/CAPA afterCommit 落库 + config 关闭仅标记 + 幂等）——**修复在位确认**（姊妹文件 spc-capability 的同类断点另立 P1-CK-qa-003）。
- **P0-MA2-017 修复在位**：`recordResult/passInspection/failInspection` 均经 `ErpQaInspectionResultStateMachine.assertCan*`（result==PENDING/null 单一源态）；pass/fail 走 `markPosted` 三件套；`failInspection` 触发 `autoCreateNcrFromInspection`（与 recordResult REJECTED 分支对齐）；`reInspect` 已删除（BizModel 无该方法，测试断言 unknown-operation）。
- **P1-MA2-066（noCapaReason 门控）在位**：`NcrLifecycleService#actionsGatePassed` L102-116——无 CAPA 时须 `StringHelper.isNotBlank(noCapaReason)`，有 CAPA 须全 COMPLETED + verificationPerson/verificationDate；`resolve` L40 先过门控再迁移。
- **P1-RC-040（关键项否决）在位**：`InspectionResultEvaluator.aggregate` L94-95 `isCritical==1` 且行 REJECTED → 直接 REJECTED（跳过 allowConcession）；复制链 `copyTemplateLinesToInspection` L67 `line.setIsCritical(spec.getIsCritical())` 完整。
- **P1-MA2-064/RC-R1.59（作废联动取消）在位**：`ErpQaInspectionCancelForBusinessBillProcessor`——config-gated（默认 true）、仅 result=PENDING 软删（useLogicalDelete）、终态不动、无匹配零副作用；行级清理经 ORM `lines` 关系 `cascade-delete` tagSet（平台 `CascadeFlusher#cascadeCollection` clear 机制承接，未逐行运行时验证——见剩余风险）。
- **D1 机械扫描全零**：qa service/dao `@Inject private`=0、`System.currentTimeMillis/LocalDateTime.now/new Date()`（Java 文件）=0、字典字符串 `==` 比较=0（仅 null 判空）、手编生成产物=0。`AttributesControlLimitFormulas` 两处 `IllegalArgumentException`（L126/L129）为防御性死分支（调用侧列表恒同长）——R4 基线 0 的边界外瑕疵，随 P2-CK-qa-010 修复收敛为 NopException。
- **平台 API `QueryBean.addOrderField(name, desc)` 使用正确**（沿用 C1.2 校准结论）：`loadLines`/`loadTemplateLines` lineNo ASC、`findSamplesOrdered`/`loadSpcSamples` subgroupNo ASC、`findLatestSpcChart` id **DESC**=取最新（语义正确）、`findByRelatedBill` id ASC（门控只看存在性，顺序无关）。
- **SPC 判异规则边界正确**：规则 1 严格越界（`compareTo(ucl) > 0`，等于限值不违规——WE 惯例）；规则 2/3/4 窗口（9 同侧/6 单调/14 交替）与 javadoc/spc.md 一致；oneSigma/twoSigma 计算为未用死代码（规则 5-8 未实现的预留，纯无害）。
- **Cp/Cpk/Pp/Ppk/Cpm 公式与阈值逐字对齐 spc.md**：Cp=(USL−LSL)/6σ̂、Cpk=min((USL−X̄̄),(X̄̄−LSL))/3σ̂、Cpm 含偏度修正；`classifyByCpk` 1.0/1.33/1.67 分档与 UC-QA-10 断言一致（null 分档缺陷另立 P2-CK-qa-008）；能力分析日期窗 `[periodFrom, periodTo+1)` 含闭边界正确。
- **计数型公式对齐 spc.md 注记**：P/NP/C/U 四公式 + 负下限钳 0 + 计数型能力保守降级（cp/cpk 全 null 不触发回写，L183 isAttributes 判断正确）；`AttributesControlLimitFormulas` 除零守卫（sumInspected.signum()==0 返回 null 三元组）在位。
- **σ̂=R̄/d2 与 ≥20 子组门槛在位**：recalculate `< SPC_MIN_SUBGROUPS_FOR_CONTROL_LIMIT` 返回 false 不重写控制限（PENDING 保持）——owner doc §关键流程 2 一致。
- **NCR 级联原子性在位**：recordResult/failInspection 的 NCR 创建与检验单终态同一 @BizMutation 事务（NCR 创建失败回滚 REJECTED，无悬挂 NCR）；SPC 路径 afterCommit + 双重幂等预检 + NCR code 确定性（UK 兜底并发）。
- **Recall 审批正向链双轴联动正确**：`doApprove`/`doReject` 均双轴同写（approveStatus + status）+ approvedBy/At（SoD 维度未要求）；close 门控（notifyCustomer + 无 PENDING target，config-gated）对齐 recall.md 规则 3。
- **RecallTargetLocator 定位算法对齐 recall.md**：trace-chain 开关校验（ERR_TRACE_CHAIN_DISABLED）→ batchTrace 批次入口 → OUTGOING+SALES_DELIVERY+DONE 过滤 → 按行累计本批次数；serialNo 单件 Non-Goal 有显式声明。
- **generateReturns 幂等在位**：确定性 RMA code（`RMA-{recall}-{targetId}`）+ RETURNED 跳过 + 行级数据完整（对照 NcrReturnOrchestrator 的无行版本已立 P2-CK-qa-014）。
- **batchPassInspection 行级失败无脏写**：`passInspection` 的守卫异常先于任何 setter/updateEntity（对照 P3-CK-pur-013 batchApprove 的部分写问题——qa 此站点失败点在写前，best-effort 语义安全；不登记）。
- **dict 死状态（P1-MA2-065）裁决在位**：QualityGoal/Review/Calibration 全态、risk-status MITIGATED/CLOSED、action-status OVERDUE、spc-calc-status STALE 均经 R1.20 resolved 为「预留语义入口（零 writer）」，owner doc §CRUD 桩实体状态机（Deferred）表逐项在案——不重复登记；dashboard 消费的状态（OPEN/IN_REVIEW/ACCEPTED/REJECTED/INADEQUATE）均有活跃 writer。
- **报表路径防注入在位**：`resolveReportPath` 经 `StringHelper.isValidVPath` + 前缀钉死 `/nop/main/report/qa/`；renderType 白名单；download 临时资源 5 分钟定时清理 + 失败即删。

## arm-index 复用 or 新增裁决（汇总）

- 关键符号 `sampledKeys 幂等`/`isInspectionCleared 复检`/`IErpQaSpcCapabilityBiz inject`/`severity 20`/`reverseApprove recall`/`让步 自审`/`classifyByCpk null`/`resolveStockBalance limit 1`/`退货空壳`/`escalateToRecall`/`quantity ONE` 在 `docs/audits/arm-index.md` **零命中 → 新增**。
- **复用（不重复登记，报告中注记）**：
  - CAPA 1:1 + actionType 硬编码 + severity LOW 死分支 → **P2-RC-044**（watch-only todo，live 仍如此：`SpcOutOfControlHandler` L119/L127-138）——不登记。
  - QualityGoal 名称约定回写 + RiskRegister 硬编码值 + 30 天窗口 → **P2-RC-045**（watch-only todo）——不登记（null 分级误触发为新增 P2-CK-qa-008）。
  - samplingFrequency 死字段 → **P2-RC-046**——不登记。
  - dispositionType 替代 defectType → **P2-RC-047**——不登记。
  - dashboard 直访 orgId/行级权限 → **P2-RC-086**（全 19 域 successor；`ErpQaDashboardBizModel` 在列）——不登记（读路径；NCR 计价写路径 orgId 缺失另立 P2-CK-qa-011）。
  - 类别级模板解析缺失 → **P2-RC-040**——null 入参退化维度为新增 P3-CK-qa-024（带注记）。
  - dict 死状态 → **P1-MA2-065**（resolved R1.20 Deferred 预留）——不登记。
  - CRUD 无守卫 → **P1-CK-pur-003 族**——P2-CK-qa-006 同型登记。
  - REQUIRES_NEW 凭证先提交 → **P2-CK-inv-012 族**——P2-CK-qa-012 同型登记。
  - post() 幂等 null 语义 → **P1-CK-fin-003 族**——P2-CK-qa-013 同型登记。
  - 宽 catch 无日志（currentUserId）→ **P3-CK-md-008 全域族**——P3-CK-qa-021 同型登记。
  - dashboard 内存聚合 → **P3-CK-mfg-014 家族**——P3-CK-qa-020 同型登记。
  - 死 cron 常量/键漂移 → **P3-CK-mfg-013/P3-CK-inv-021 家族**——P3-CK-qa-019 同型登记。
  - 双轴 reverseApprove 死锁 → **P1-CK-mfg-002/P2-CK-mfg-010 同族**——P1-CK-qa-005 新站点（召回审批门绕过为独立危害面，登记为新增带同族注记）。
  - **P0-CK-mfg-001 同型核查（mission 指定）**：qa `createForBusinessBill` 不存在 findByRelatedBill 幂等短路（每次新建、不返回已有单）——**非同型**；其并发双建缺陷以 P3-CK-qa-017 独立登记。

## 统计

| 级别 | 数量 | 编号 |
| --- | --- | --- |
| P0 | 0 | — |
| P1 | 5 | P1-CK-qa-001..005 |
| P2 | 9 | P2-CK-qa-006..014 |
| P3 | 11 | P3-CK-qa-015..025 |

按主维度：D6×5（004/008/010/022/025）、D5×5（006/007/015/018/024）、D8×4（011/013/014/023）、D3×3（002/005/016）、D7×3（001/012/017）、D4×2（003/019）、D9×2（009/020）、D2×1（021）。

同型登记 6 项（006 pur-003 族 / 012 inv-012 族 / 013 fin-003 族 / 020 mfg-014 族 / 021 md-008 族 / 019 mfg-013 族）；带同族注记新增 3 项（005 mfg-002/010 同族、024 P2-RC-040 注记、017 mfg-001 非同型注记）。

## 剩余风险（查了什么/没查什么）

- **已查**：erp-qa-service 72 文件中 58 个逐行深读（检验单链 7 Processor + BizModel + Evaluator + 模板匹配器、NCR/CAPA 全链 + posting 4 类、SPC 7 引擎类 + 3 Processor、Recall 12 文件、dashboard/report、状态机 5 Bean、Configs/Constants 抽查）；CRUD 桩 10 类（15-22 行）抽查确认裸桩；ORM 核对（全实体 versionProp/useLogicalDelete、UK 集、severity/isActive/verificationPerson 列类型、dict 全部值域）；平台/跨域实证 8 处（`OrmEntity#orm_propValueByName`→BeanTool、`CascadeFlusher` cascade、`ErpOrgIsolationConstants` 写路径 preSave、RC-R1.26 plan 的 inject 运行时实证、job yaml 双层门控、`_service.beans.xml` bean id 注册形态、mfg batch 对照、测试 seed 配方核对）；arm-index qa 相关 17 个 finding 全量裁决。
- **未深查**：`erp-qa-web` AMIS view.xml 契约 drift（归 C8.2）；`erp-qa-api` beans 骨架（纯数据类）；xmeta 层前端守卫（如 severity 字典控件对 "20" 的展示行为——P1-CK-qa-004 触发面以后端落库为准）；`InspectionLine.parameterId` 仅手工建行可非空的链路（`copyTemplateLinesToInspection` 留空——SPC chart.parameterId 匹配只覆盖手工行，为 baseline 注记的既有简化，未定性）；`IErpFinVoucherBiz.post/reverse` 的幂等/期间锁行为细节（引用 P1-CK-fin-003/ck-finance-posting.md 结论未重验）；测试代码正确性（38 个测试文件仅用于行为语义交叉验证）。
- **最不确定、建议主 agent 复核**：
  1. **P1-CK-qa-001**（SPC 幂等键幻影子组）——推演确凿（存量键重建只用首点 code × 全部值），但建议主 agent 用一次「10 点 seed + 连跑两次 collectSamples」集成测试实证样本数 2→3；若平台 `getEntityById` 会话缓存或 JSON 解析行为存在我未预见的对称性，结论需修正。
  2. **P1-CK-qa-004**（severity="20"）——「不抛错、落库 "20"」由绿色基线（NCR 创建断言通过）反推，`BeanTool.setProperty` 的 Integer→String 精确转换路径未逐字节运行时验证；建议断言 `ncr.getSeverity()` 实值。
  3. **P1-CK-qa-002**（复检死锁）——若产品语义是「拒收单必须经退货+重新交付新单流转」（复检仅在异 bill 上发生），则触发面收窄为 P2；但 owner doc §3/§4 的同单复检语义与实现矛盾确凿，建议与 owner doc 裁决方向联动定性。
  4. **P1-CK-qa-003**（spc-capability inject 断裂）——运行时失败证据引用 RC-R1.26 plan 的同型实测（`inject('spcSamplingService')` 抛 ERR_IOC_UNKNOWN_BEAN_FOR_NAME）——该 batch 文件本身默认 disabled 从未在生产执行过，属「启用即断」的确定性推断而非本 mission 运行时复现。
