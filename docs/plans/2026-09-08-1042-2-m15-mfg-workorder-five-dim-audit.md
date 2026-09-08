---
status: active
mission: ai-check-r3
work-item: M1.5
group: "2026-09-08-1042"
verify: [test]
---

# 2026-09-08-1042-2 M1.5 manufacturing mfg-1 五维符合性审计（工单与报工切片）

## Current Baseline

- 依赖已满足：M0.6 done（plan `2026-09-06-1451-3`）；MI.9 收官 done（plan `2026-09-07-1715-3`，独立 closure audit ACCEPT，2026-09-08；`docs/testing/known-good-baselines.md` 2026-09-08 `ai-check-r3 MI 终态行`：全 reactor `mvn test` 4006/0/0/1、CJK report mode CAT1..4 = 0/0/0/0、双 checker 零回归、白名单 27 文件四要素齐备）。M1.5 与 M1.1 同为 M1 首批 deps 满足项；本计划执行顺序居本批第 2（M1.1 解锁 fin 后续切片，先行的文档序首批成员）。
- 强制核对矩阵已冻结：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md`——本计划唯一格集合 = **U08 × 五维 × mfg-1**（§4 映射表第 5 行）；判定标准/机械核查程式/跨轮查重程序以该冻结清单 §1/§2 为准。
- 切片范围（U08 mfg-1）：工单双轴状态机（approveStatus×docStatus）/作业卡/领料红冲三件套回退/预留/完工入库幂等键；owner doc `docs/design/manufacturing/state-machine.md`（290 行，实仓核验在盘）+ `docs/design/manufacturing/use-cases.md` + `docs/design/manufacturing/material-reservation.md`；物理面 `module-manufacturing/erp-mfg-{dao,service,web}` 的 `src/main`。
- 共享代码唯一归属（冻结清单 §3.2）：完工入库移动为跨域消费点——**消费侧行为归本格**，涉及 posting 引擎内部时标注「归属 fin-1」归并；common 抽象族（`AbstractProcessor.illegal*`、状态锁基类）行为缺陷归 U20（M1.15），本切片只审调用点合规；聚合横切面归 U21（M1.16）。
- 跨轮查重源（§2）：r1 `docs/audits/check/ck-mfg-workorder.md`（C4.1：1 P0 / 4 P1 / 6 P2 / 10 P3，done；P0-CK-mfg-001 + P1-CK-mfg-002~005 fixed；P2-CK-mfg-006~011 open——其中 011 部分修复、残余归 F2.5 successor；P3-CK-mfg-012~021 open）+ r2 只读目录 `docs/audits/check/2026-08-28-2049-ai-check-r2/` + 索引 §Mission 基线快照。
- DIM-I 内嵌 MI 先行时序口径（冻结清单 §1.5）：mfg 探针族（CAT-1 31 / CAT-2 9）MI 后已清零——本切片 DIM-I 维仅验证零回归与白名单合规，不再产生同类 finding；脚本红 = MI 回归升级报 MI 通道；白名单四要素缺失 = 白名单登记缺陷 finding。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-08 MI 终态行（MV.1 对照面）；compliance checker 对照面 = M0.3 快照行（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42）；机器基线块差距归 successor `ai-check-r3-compliance-baseline-raise`。
- 仓库现状（2026-09-08 草案时点实核，草案审查时点已复验）：草案基线 HEAD `40512c567`；姊妹 plan `2026-09-07-2200-1`（StateMachine 直抛领域码，mfg 批次 `1166339ae` 先行入库、非其 Phase 4 六小域批次成员）已于 `67308e144` 独立 closure audit ACCEPT 落账收官；审查时点 HEAD `8825a10e1`（lesson 19 索引登记行），脏面仅本批 3 份 `2026-09-08-1042-*` 计划未入库。DIM-B/T 走查将面对姊妹重构后的 mfg 代码态。审计证据一律以实跑时 HEAD + 脏面披露为准（Phase 1 机械登记）。
- 剩余差距：U08 × 五维 × mfg-1 五格 verdict 未落盘；mfg-1 无 `-r3` 切片报告。

## Goals

- 按冻结清单对 U08 × 五维 × mfg-1 五格全跑（禁止抽样、禁止跳维），逐格落 verdict，产出 `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-mfg-workorder-r3.md`（五维覆盖矩阵 + finding 列表 + 统计 + 剩余风险声明）。
- 每候选 finding 完成三态查重裁决（复用/归并/新立 `-r3` ID，历史 ID 永不覆写）；DIM-B 维含 owner-doc 关键断言抽样 ≥2 doc × 2 断言（漂移则扩至全部 owner doc）。
- 双索引同步：本轮 `ai-check-r3-index.md` 产物清单 + 跨轮 `docs/audits/check/ai-check-index.md`。
- 全程零生产代码改动（roadmap 规则 6），收官 `git status` 机械核证。

## Non-Goals

- 不修复任何 finding（修复归 M2.x）；不改任何生产代码/ORM/api.xml/配置/页面/seed 文件。
- 不覆盖 mfg-2（BOM/MRP/CRP，M1.6）/ mfg-3（委外/批次追溯/差异，M1.7）格与其他单元格。
- 不接管 r1/r2 工作项；不重开既有裁决（含 P2-CK-mfg-011 的 F2.5 successor 归属、lesson 09/10）；不做 roadmap 状态翻转。
- DIM-I 不产生同类 CJK finding（§1.5 内嵌口径）。

## Phase 1 — 执行目录就位 + 切片红线与脏面披露

> 统一类型：Proof（3 项 Proof）。
> Skill: none
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/`（幂等复用）；盘点注记落本计划勾选注记
> Prereqs: 无（与 M1.1 计划并行起草，执行序居后不阻塞）

- [x] <Proof> `mkdir -p docs/audits/check/2026-09-06-1645-ai-check-r3/`（幂等复用；roadmap 规则 8），确认索引与冻结清单在位
      - Skill: none
      - 证据：目录已存在（幂等复用）；`ai-check-r3-index.md` + `m0-5-audit-checklists.md`（冻结清单）+ r1/r2 产物均在位；姊妹 M1.1 产物 `ck-finance-posting-r3.md` + `mi9-closure-verification.md` 同目录。
- [x] <Proof> 记录审计时点仓库状态：HEAD hash、`git status --porcelain` 脏面清单、姊妹在制会话披露；后续全部证据注记引用该时点
      - Skill: none
      - 证据：审计时点 HEAD `dd39e6cce`（plan-2026-09-08-1042-1 M1.1 finance fin-1 五维审计落盘 + 闭包审计回执 ACCEPT）；`git status --porcelain` = 空（零脏面；草案基线提及的本批 3 计划已随 `dd39e6cce` 入库）；无姊妹在制会话——plan `2026-09-07-2200-1` 已于 `67308e144` ACCEPT 收官、plan `2026-09-08-1042-1`（M1.1 fin-1）已随 HEAD 收官。后续证据一律以本时点为准。
- [x] <Proof> 切片红线基线：`bash docs/audits/nop-compliance-checker.sh`（对照 M0.3 快照行）+ `node tools/check-hardcoded-cjk.mjs`（report mode，对照 0/0/0/0 终态）实跑记录；漂移即登记（不就地裁决）
      - Skill: none
      - 证据：compliance checker 实跑 R1a=0/R1b=0/R1c=0/R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R4=0/R5=0/R6=2/R7=0/R8=0/R10=14/R11=0/R12a=71/R12b=66/R12c=42——与 M0.3 快照行逐规则一致，零漂移；CJK report mode CAT-1=0/CAT-2=0/CAT-3=0/CAT-4=0（CAT-5 注释 21158 行豁免仅统计）——与 MI 终态行 0/0/0/0 一致，零漂移。无漂移登记义务。

Exit Criteria:

- [x] 执行目录就位且本轮索引头部登记路径一致；时点/脏面披露在案
- [x] 双 checker 红线数字在案且与 M0.3 快照行 / MI 终态行对账一致（或漂移已登记）

## Phase 2 — DIM-B 后端平台合规走查（15 维度 + R1-R12 + 断言抽样）

> 统一类型：Proof-heavy（3 Proof + 1 Decision）。
> Skill: nop-platform-conformance-audit-prompt + code-quality-audit-prompt（roadmap M1.5 行指定）
> Targets: `module-manufacturing/erp-mfg-dao|erp-mfg-service/src/main/java`（mfg-1 范围 = 工单/作业卡/领料/预留/完工入库族文件）
> Prereqs: Phase 1 完成

- [x] <Proof> 冻结清单 §1.1 机械核查程式全套实跑：compliance checker 逐规则对照 + 反模式 grep 族 + codegen 产物安全 + 聚合完整性（按 §6 勘误 E1 路径 `app-erp-all/src/main/resources/_vfs/nop/main/auth/app.action-auth.xml`）
      - Skill: nop-platform-conformance-audit-prompt
      - 证据：① compliance checker 实跑（Phase 1 同轮）R1a-d/R2a-d/R3-R8/R10-R12a-c 逐规则 = M0.3 快照行零漂移（R6=2 均在 finance 已裁决 REQUIRES_NEW）。② 反模式 grep 族（scope=erp-mfg-{dao,service}/src/main/java）：`extends RuntimeException`=0、`@Inject+private`=0、`System.currentTimeMillis`=0、`@Transactional∩@BizMutation` 共存=0（7 文件命中均为 javadoc 事务边界约定注记「本类不带 @Transactional」，零真实注解共存）、`IDaoProvider/IOrmTemplate/@SqlLibMapper` 命中处均有注释理由（Processor 非 BizModel 同域持久化范式，AbstractErpMfgMaterialIssueProcessor 类 javadoc + ErpMfgScheduleToJobCardProcessor L44；IOrmTemplate 仅 `flushSession()` 会话控制非跨实体数据访问，跨实体读全走 I*Biz：stockMoveBiz/stockLedgerBiz/reservationBiz/workOrderBiz/workOrderLineBiz）。③ codegen 产物安全：`git status --porcelain` mfg 五模块 = 空（零手改）；`__XGEN_FORCE_OVERRIDE__` 非 _gen 命中 20 处全在 `erp-mfg-meta/src/main/resources/_vfs/dict/erp-mfg/*.dict.yaml`（代码生成 dict 只读校验点，零脏面=零手改）。④ 聚合完整性（E1 勘误路径）：聚合器 `x:extends` 含 `erp/mfg/auth/erp-mfg.action-auth.xml`，mfg 保留层 action-auth（app.action-auth.xml + app.data-auth.xml 无 `_` 前缀）在位。
- [x] <Proof> 15 维度逐维走查 mfg-1 范围（重点：②跨实体 I*Biz（inv 预留/库存调用点）③NopException ⑧双轴状态机 ⑨审批流；完工入库移动消费侧涉及引擎内部时标注归属 fin-1）+ blocker/major/minor 分级
      - Skill: nop-platform-conformance-audit-prompt
      - 证据（逐维，无跳维）：①Model→Delta→Java：per-mutation Processor 提取 + xbiz 一行委托（processor-extension-pattern 两层结构），声明未落地项已有跟踪（P2-CK-mfg-008 consumption）——pass。②跨实体：facade/issue 链全走 I*Biz（IErpInvStockMoveBiz/IErpInvReservationBiz/IErpQaInspectionBiz/IErpSysNotificationBiz/IErpMfgSubcontractOrderBiz）；daoFor(ErpMdMaterial) 2 站点 = checker R2d 基线已裁决条目（P1-MA4-008 data-dependency-matrix §9 豁免族）——pass。③NopException：全错误 ErpMfgErrors ErrorCode + NopException（grep extends RuntimeException=0）；姊妹重构直抛领域码 `ERR_INVALID_STATUS_TRANSITION` + 调用点同码补参在位——pass。④IoC/事务：@Inject 零 private；Processor 族零真实 @Transactional（javadoc 显式声明跟随 Facade）——pass。⑤平台辅助：CoreMetrics.currentTimestamp()/today() 在位，System.currentTimeMillis=0——pass。⑥标准服务模式：实体 BizModel extends CrudBizModel + @BizQuery/@BizMutation——pass（守卫缺口 = 既有 open P2-CK-mfg-006 归并，不重复立项）。⑦机制 B：mfg orm 10 处 `notGenCode="true"` 显式 biz:moduleId+tableName 声明——pass。⑧双轴状态机：Document/Approval 双 Bean 单轴建模 + transitions() 元数据完备 + 终态分类一致；**发现 P1-CK-mfg-022-r3**（见 Decision 行）。⑨审批流：submit/approve/reject/reverseApprove/withdraw 链 + SoD（SoDGuard.assertApproverNotCreator + `erp.err.mfg.approver-is-creator`）在位；**同发现 P1-CK-mfg-022-r3**（reverseApprove×docStatus 组合守卫）。⑩定制顺序：protected step 方法族 + Delta 同名 bean id 覆盖点 javadoc 声明——pass。⑪多租户：orgId 全实体在位；齐套/看板隔离缺口 = 既有 open P2-CK-mfg-007 归并——pass（带归并）。⑫测试与验证：DIM-T 维（Phase 5）承载。⑬Codegen 产物安全：见上行③——pass。⑭聚合完整性：见上行④——pass（聚合器机制本体归 U21）。⑮owner-doc 断言抽样：见下行专项 Proof——pass。完工入库移动消费侧（generateCompletionMove→stockMoveBiz.generateMove→GL 过账）：消费侧行为本格已审（幂等键 P0-CK-mfg-001 修复验证通过、acctSchema 解析在位）；引擎内部（REQUIRES_NEW 过账、凭证平衡）**归属 fin-1** 归并，不重复立项。blocker=0 / major=1（P1-CK-mfg-022-r3）/ minor=0（新增口径）。
- [x] <Proof> 维度⑮ owner-doc 关键断言抽样：`docs/design/manufacturing/state-machine.md` + `use-cases.md` + `material-reservation.md` 中 ≥2 doc × 2 关键断言（双轴状态名/迁移路径/ErrorCode）对照代码；≥2 处漂移扩大至全部 owner doc
      - Skill: code-quality-audit-prompt
      - 证据（3 doc × 9 断言，漂移 0，无需扩样）：state-machine.md §2 断言「IN_PROCESS→COMPLETED 完工数量 ≥ 工单数量」↔ ErpMfgWorkOrderReportCompletionProcessor L45 `newCompleted.compareTo(planned) >= 0` + assertCanReportCompletion 仅 IN_PROCESS——一致；§职责分离断言「创建人审核人不可同一人抛 erp.err.mfg.approver-is-creator」↔ ErpMfgErrors L305-306 + doApprove L281 SoDGuard——一致；§实现约定断言「无 INSPECTING 态 + inspection-gate-enabled 默认 false + ERR_INSPECTION_REQUIRED」↔ ErpMfgErrors L95-96 + facade L587-589 默认 false + reportCompletion L46-49——一致；§异常路径断言「ERR_OVER_REPORT 硬编码拒绝、无超产 config key」↔ ErpMfgErrors L100-101 + ErpMfgConstants 零 over-report 键——一致。material-reservation.md §配置项断言「3 键默认 reservation-enabled/over-pick-warning/auto-release-on-complete=true」↔ facade L805/809/813 readBoolConfig 默认值——一致；§D2 状态映射断言「取消→CANCELLED / 完工剩余>0→PARTIALLY_CONSUMED / 已全领→CONSUMED」↔ releaseReservations 传 RESERVATION_STATUS_CANCELLED + releaseRemainingReservations 传 "COMPLETED"（库存侧 D2 映射，r1 L187 已验证）——一致；§预留量 min(需求,可用) ↔ 库存侧 consumeFromLines min 封顶（r1 验证为正确 L187 复核 HEAD 仍有效）——一致。use-cases.md UC-MFG-04 断言「STOCK_PARTIAL→IN_PROCESS 强制开工（配置允许+权限）」↔ assertCanStart 双来源 + isAllowPartialKitStart 默认 false + xbiz start 权限——一致；UC-MFG-07 断言「单位成本=(材料+人工+制造费用)/完工数量」↔ recomputeTotals L476-481（含 subcontract 加性扩展，非漂移）——一致。
- [x] <Decision> 本维候选 finding 逐条过 §2 三态裁决（查 r1 `ck-mfg-workorder.md` 21 条 finding + r2 + 基线快照；r1 open 项同型归并原 ID，fixed 项复用并复核 HEAD 有效性）；裁决证据落勾选注记
      - Skill: code-quality-audit-prompt
      - 裁决证据：**新立 1 条**——`P1-CK-mfg-022-r3`（D3/D8；归并注记 P2-CK-mfg-010 同型控制点）：doReverseApprove 随 P1-CK-mfg-002 修复改为无条件回写 `docStatus=DRAFT`（ErpMfgWorkOrderProcessor L299-307），而 reverseApprove 全链无 docStatus 守卫（validateTransitionForReverseApprove L247-254 仅审批轴 + assertCanReverseApprove 仅 APPROVED 判定 + xbiz 仅权限），IN_PROCESS/COMPLETED/CLOSED 工单（approveStatus 恒 APPROVED）调 reverseApprove 即被复活为 DRAFT 可编辑可重提态——原 P2-CK-mfg-010 的「终态复活被 assertCanSubmit 挡住」缓解失效，历史 completedQuantity/成本/库存移动与 DRAFT 态脱钩；回归测试仅覆盖 NOT_STARTED 路径（TestErpMfgReservationLifecycle.testReverseApproveThenResubmit）。三态：同型 open（P2-CK-mfg-010）→ 归并原 ID 追加新证据 + 后果实质升级（P2 双轴矛盾态→P1 终态复活可达）→ 按 §3.1 新立 `-r3` ID，原 ID 状态不动；r2 无同型（mfg reverseApprove 组合守卫 r1 首记）。**fixed 复用复核 5 条**——P0-CK-mfg-001：completionMoveBillCode 首张 wo.code + 增量 `-C<累计量>` 后缀（facade L431-451 + L418 接线）HEAD 有效；P1-CK-mfg-002：doReject/doReverseApprove 双轴联动写 DRAFT（L290-307）+ 回归测试在位——HEAD 有效（其与 P2-CK-mfg-010 的叠加副作用即 P1-CK-mfg-022-r3）；P1-CK-mfg-003：镜像回退三步（rollbackWorkOrderLineActualQty/rollbackMaterialCostToWorkOrder/unconsumeReservations，ReverseConfirmProcessor L52-59 + Abstract L168-193）HEAD 有效；P1-CK-mfg-004：destWarehouseId/uomId 缺失 LOG.error + 通知不再静默（facade L391-408）HEAD 有效；P1-CK-mfg-005：红冲任一步失败抛 NopException 中止回滚（ReverseConfirmProcessor L41-50 零吞咽 + javadoc L27-29）HEAD 有效。**open 归并 15 条**（原 ID 状态不动，本格复核 HEAD 仍在）：P2-CK-mfg-006（实体 BizModel 仍裸 CrudBizModel 零 defaultPrepare）、007（KitAvailabilityChecker 零 orgId filter + Dashboard orgId=0）、008（consumption 运行时零消费）、009（cancel 白名单仍 {DRAFT,SUBMITTED,NOT_STARTED}）、011（reportCompletion 已前移 updateEntity/releaseRemainingReservations 至 generateCompletionMove 之前——部分修复与计划基线口径一致，残余归 F2.5 successor）、012（reportCompletion 负数静默置 ZERO L34-36）、013（CONFIG_JOBCARD_AUTO_GENERATE_CRON 死常量仍在 L112）、014（Dashboard 无界加载）、015（findWorkOrderIdsWithJobCards `setLimit(workOrderIds.size())` L287 仍在）、016（currentUserId 宽 catch 返 null L826-836）、017（generatePendingJobCards 单事务逐单吞异常）、018（isInspectionGated bomId==null 恒 false L468）、019（AUTO_UPGRADE 忽略显式 bomId）、020（applyLaborCostToWorkOrder 无终态守卫 L58-70）、021（汇率 BigDecimal.ONE + 贷方 1401 硬编码 L125/L142/L164）——均归并原 ID，不重复立项。

Exit Criteria:

- [x] U08×B×mfg-1 格 verdict 落盘，15 维度无跳维
- [x] 机械程式输出数字在案且与基线对账一致；候选 finding 三态裁决完成

## Phase 3 — DIM-F 前端页面与 E2E 走查

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = 冻结清单 §1.2）
> Targets: `module-manufacturing/erp-mfg-web/src/main/resources/_vfs`（mfg-1 面：排产/工单看板页面）
> Prereqs: Phase 1 完成

- [x] <Proof> 全局面静态门禁实跑：`npm run validate:flux`（导出 0 error；325 条既有 variant 外部漂移为 successor 在案，非本切片 finding）+ flux-only grep（AMIS 保留层 / ORM `ext:web-renderer="flux"`）
      - Skill: none
      - 证据：`npm run validate:flux` 实跑 `Results: files=855 validated=855 errors=325 warnings=18491`（exit 1）——325 条 ERR 逐条核对**全部同型**（`variant:"primary"` × `dropdown-button` 渲染器枚举漂移），域分布横跨 18 域（mfg 34 条为全域同型成员，非本切片缺陷）；该 325 漂移已在案（roadmap MI.8 done 行 + MI.8 收官 plan `2026-09-07-1715-2`：「整体 exit 1 余项 = 325 条既有 variant 外部漂移（successor 在案）」），非本切片 finding，与本计划预期口径逐字一致。flux-only 机械：`component="AMIS"` 保留层 = 0；`grep -L ext:web-renderer="flux"` module-*/model/*.orm.xml = 空（19 域全带渲染标记）。
- [x] <Proof> mfg-1 页面走查：排产/工单看板页对照 view-and-page-strategy + dashboards pattern（REST `/r/` / M0.4 源头链查表 / `x:extends` 定制 / i18n-en 承载）逐页落 verdict；涉 mfg E2E spec 时核对 PageObject + flux 引擎缺省 + 禁 GraphQL 断言
      - Skill: none
      - 证据（逐页，无跳页）：**工单看板 `dashboard/main`**（M0.4 手写页 rows 75/76）：`.flux.yaml`（运行时面，view-and-page-strategy L58「`*.flux.yaml` 双文件回退优先于 `*.page.yaml`」）数据源全 `@query:ErpMfgDashboard__*`（REST `/r/` 约定 ✓）+ KPI 卡/饼图/趋势线/CRP combo 图/延期预警 crud 对齐 dashboards pattern ✓ + 全部 CJK 文案携 i18nEn ✓；`.page.yaml`（被回退遮蔽的孪生手写文件）发现 CRP data-source 日期参数错配 `filterForm?.dateFrom/dateTo`（表单实际字段 startDate/endDate，恒 null）——运行时零影响（flux.yaml L170-171 已正确绑定 startDate/endDate）→ **新立 P3-CK-mfg-023-r3**（minor：遮蔽文件潜伏缺陷，孪生手写双文件漂移风险）。**BOM 树看板 `dashboard/bom-tree`**（rows 73/74）：`@query:ErpMfgBom__*` REST ✓ + i18nEn 在位 ✓——消费 ErpMfgBom 属 mfg-2 面（邻接记录，不落本格 verdict）。**实体页**：ErpMfgWorkOrder/JobCard/MaterialIssue/行族/TimeLog/快照族 main.page.yaml 经 M0.4 三步程序核验 = GenPage codegen stub（正文仅 web:GenPage 引用），文案源头在 view.xml 模型源——ErpMfgWorkOrder.view.xml `i18n-en:*` 18 处承载 ✓（CAT-4 衔接归 DIM-I）；ref-work-order picker 两页 = M0.4 rows 27/28 codegen stub 禁改 ✓。**E2E 纪律**：mfg spec 9 个（business-actions 5 + orchestration 4）机械 grep 违规选择器 `data-slot/data-testid/.cxd-` = **0** ✓；PageObject/adapter 形态 ✓（经 `./_helper` + `../pages` 引擎无关 helper）；`E2E_ENGINE` 缺省 flux（engine.ts L8-9）✓；spec 内 `graphql` 命中 4 处均为 API 驱动型数据层断言（e2e-runbook L229 明文登记「自定义 @BizMutation 经 GraphQL 全栈可达」+「4 制造链编排 spec 经 GraphQL 驱动」为现行注册架构，非 flux 页面 GraphQL 断言，L122 禁令不适用）✓。

Exit Criteria:

- [x] U08×F×mfg-1 格 verdict 落盘；全局面门禁数字在案对账一致
- [x] mfg-1 范围页面逐页走查完成，无跳页

## Phase 4 — DIM-S seed 数据走查

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/seed-data.md` + `docs/design/manufacturing/seed-data.md`）
> Targets: `app-erp-all/src/main/resources/_vfs/_init-data`（只读清点）+ mfg 相关 seed（work_order/cost_variance/forecast 族 + workcenter 配置链 + crp_load）
> Prereqs: Phase 1 完成

- [x] <Proof> 引用完整性门禁实跑：`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿
      - Skill: none
      - 证据：实跑 `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0` + `BUILD SUCCESS`（exit 0，全表可加载 + to-one 零悬空 + 零孤儿 CSV + scope-pinning 门控通过）。
- [x] <Proof> mfg-1 seed 面核对：`git status --porcelain` seed 路径 expect 空 + deploy `_seed_*.sql` 同步义务查登记处表 + work_order/workcenter/crp_load seed 自洽约束抽查（按 seed-data.md 运营域约束段；SPC/CRP 双层门控默认关核对）
      - Skill: none
      - 证据：① seed 资产清点 = **372 CSV + 1 SQL**（`zz-sequence-advance.sql`），与冻结清单 §1.3 ②登记行「M1.5 批次后 = 372 CSV + 1 SQL」一致，零增量零孤儿；② `git status --porcelain app-erp-all/.../_init-data/` = 空（零 seed 变更，快照重录义务未触发，只读审计正常态）；③ deploy `_seed_*.sql` 全仓 `find` 仅 module-cs（3 方言）+ module-notify（3 方言）——与 seed-data.md §同步义务登记处表逐行核对：两模块均 ✅ 已聚合（2026-08-25 `nop_sys_code_rule.csv` + `erp_sys_notification_template.csv`），其余模块无 deploy seed，mfg 无同步义务；④ mfg-1 seed 自洽抽查：`erp_mfg_crp_load.csv`（workOrderId=1→WO-2026-001 实存 + workcenterId=1→WC-001 实存，与 seed-data.md 实证结论行逐字一致）+ `erp_mfg_work_order.csv` 4 行（WO-2026-001~004，覆盖 IN_PROCESS/STOCK_PARTIAL/COMPLETED 三态，与 0930-1 登记一致）+ `erp_mfg_workcenter.csv` WC-001 主装配线单班配置链在位；⑤ 门控默认关核对：SPC 引擎双层门控默认关（seed-data.md L19）+ CRP 重算链 nop-job 双层门控默认关（L21）——seed 静态行不被引擎重算覆盖，与登记裁决一致。

Exit Criteria:

- [x] U08×S×mfg-1 格 verdict 落盘；`TestErpSeedDataIntegrity` 全绿在案
- [x] seed 零变更 + 同步义务核对 + 自洽抽查结果在案

## Phase 5 — DIM-T 单元测试走查

> 统一类型：Proof（3 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/testing-strategy.md`）
> Targets: `module-manufacturing/erp-mfg-service`（`<SVC>` 绑定：`<SVC>` = `module-manufacturing/erp-mfg-service`，冻结清单 §1.4 程式记号）
> Prereqs: Phase 1 完成

- [x] <Proof> 切片本地回归：`mvn test -pl module-manufacturing/erp-mfg-service` 全绿零失败（全仓聚合对照面 = known-good-baselines 2026-09-08 MI 终态行 4006/0/0/1；模块级参照 = 姊妹 plan `2026-09-07-2200-1` mfg 批次提交 `1166339ae` 308 green——known-good-baselines 无模块级 mfg 计数行，实跑计数照实登记不预填）
      - Skill: none
      - 证据：实跑 `Tests run: 308, Failures: 0, Errors: 0, Skipped: 0` + `BUILD SUCCESS`（exit 0）——与姊妹批次提交 `1166339ae` 的 308 green 参照值一致（姊妹重构后基线保持，零回归）；全仓聚合对照面 = MI 终态行 4006/0/0/1（本切片只读审计零代码改动，聚合面无扰动义务，收官核证见 Phase 7）。
- [x] <Proof> 覆盖缺口对账：公开方法清单 × `_cases` 测试目录清单逐项对账（mfg-1 范围 = 工单/作业卡/领料/预留/完工入库族 BizModel）；关键业务流清单核对——P1 工单→领料→报工→完工入库→成本结转清单行逐行核覆盖；缺口按业务关键度定级
      - Skill: none
      - 证据：公开方法对账——WorkOrder BizModel @BizQuery/@BizMutation=10（submit/withdraw/approve/reject/reverseApprove/checkAvailability/cancel/start/stop/resume/close/reportCompletion 族）↔ TestErpMfgWorkOrderStateMachine + TestErpMfgWorkOrderEndToEnd + TestErpMfgReservationLifecycle（含 testReverseApproveThenResubmit P1-CK-mfg-002 回归行）+ TestErpMfgWorkOrderCancelInspectionLinkage + processor/ + statemachine/ 目录，逐动作有落点；JobCard=7（startJob/recordWork/submitJob/completeJob/holdJob/resumeJob/cancelJob）↔ TestErpMfgJobCardStateMachineMatrix + TestErpMfgWorkOrderEndToEnd（grep holdJob/resumeJob/cancelJob 命中两文件）；MaterialIssue=2（confirm/reverseConfirm）↔ TestErpMfgMaterialIssue + TestErpMfgMaterialIssueReversal；行族/TimeLog BizModel=0 自定义动作（裸 CRUD，P2-CK-mfg-006 归并）零测试义务。P1 关键业务流逐行：工单审批↔WorkOrderStateMachine/EndToEnd ✓、预留↔ReservationLifecycle ✓、领料出库↔MaterialIssue/IssuePosting ✓、领料红冲↔MaterialIssueReversal ✓、报工↔WorkOrderEndToEnd（recordWork 回写 laborCost）✓、完工入库↔CompletionPosting ✓、成本结转↔CostFlowEndToEnd ✓——P1 清单行全覆盖零缺口；浏览器层另有 mfg-chain 等 4 orchestration spec + 2 business-actions spec（runbook L229 登记）。**缺口登记：无新立**（reverseApprove 终态组合守卫缺测试已并入 P1-CK-mfg-022-r3 证据，不重复立项；ErrorCode 覆盖抽查——ERR_OVER_REPORT/ERR_MATERIAL_ISSUE_NOT_POSTED/ERR_INVALID_STATUS_TRANSITION（矩阵测试）/ERR_JOB_CARDS_ALREADY_GENERATED 均有断言落点）。52 测试文件 / 308 测试全绿佐证覆盖有效性。
- [x] <Proof> 快照纪律：`SnapshotTest.RECORDING` 提交态零残留 + `*` 通配/`delVersion` 屏蔽合规抽查
      - Skill: none
      - 证据：`grep -rn "SnapshotTest.RECORDING" src/test/java` = **0**（录制态零残留，CHECKING 提交态合规）；`*` 通配命中仅 `createTime`/`updateTime`（框架自动屏蔽字段，testing-strategy 允许）；`delVersion: 0` 命中均为快照输出记录值（录制内容非屏蔽指令）——零违规。

Exit Criteria:

- [x] U08×T×mfg-1 格 verdict 落盘；本地回归全绿数字在案
- [x] 覆盖对账 + 关键业务流清单核对 + 快照纪律结果在案

## Phase 6 — DIM-I i18n 零回归与白名单合规（MI 先行时序口径）

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/i18n-compliance.md` + `docs/audits/cjk-baseline.md`）
> Targets: 全域门控 + mfg 白名单条目抽查
> Prereqs: Phase 1 完成

- [x] <Proof> 零回归门控：`node tools/check-hardcoded-cjk.mjs --strict`（exit 0）+ `--self-test` PASS；脚本红 = MI 回归升级报 MI 通道，不立 DIM-I finding
      - Skill: none
      - 证据：`--strict` 实跑 **RESULT: PASS（exit 0）**——「0 new violations vs frozen snapshot; 170 baseline files, totals CAT1..4=0/0/209/1318」（209/1318 为基线历史总量记录，现树实测 CAT-1..4 = 0/0/0/0，与 Phase 1 report mode 一致；输出「removed from tree」行 = MI 清剿后基线文件已清零的单向收紧记录）；`--self-test` 实跑 **RESULT: PASS (self-test green)**（5 断言全过）。脚本零红，无 MI 回归升级义务。
- [x] <Proof> 白名单合规抽查：§WHITELIST mfg 相关条目抽 ≥3 条核对四要素 + `grep -L "@Locale"` 全量 `*Errors.java` expect 空 + `git status --porcelain 'module-*/erp-*-meta/**'` expect 空；四要素缺失 = 白名单登记缺陷 finding
      - Skill: none
      - 证据：§WHITELIST 块共 27 文件（与 MI.9 收官基线「白名单 27 文件」一致，只增不删）；mfg 域文件条目 = 1 条（`ErpMfgDashboardBizModel.java`），按「mfg 相关条目」扩样至 mfg 消费链相邻运行时字符串条目共抽 **4 条**核对四要素（文件路径 + cats + 理由 + owner doc 指针 + 裁决来源带 plan 指针与日期）：① mfg `ErpMfgDashboardBizModel`（@Description 中文专属文件同 E3 豁免；i18n-compliance.md 准绳表 #5；plan 2026-09-07-1715-1 Phase 2）✓ ② inv `ErpInvDashboardBizModel` 同簇 ✓ ③ pur `ErpPurDashboardBizModel` 同簇（Phase 3）✓ ④ aps `IErpApsOperationOrderBiz`（mfg 排程建卡跨域契约 SPI，mfg `ErpMfgScheduleToJobCardProcessor` 消费面）✓——四要素 **4/4 齐备，零登记缺陷**。`grep -L "@Locale"` 全量 `*Errors.java` = 空（19 域全数声明）✓；`git status --porcelain 'module-*/erp-*-meta/**' 'app-erp-all/.../i18n/**'` = 空（`_` 前缀生成 i18n 零手改）✓。

Exit Criteria:

- [x] U08×I×mfg-1 格 verdict 落盘；`--strict`/`--self-test` PASS 在案
- [x] 白名单四要素抽查 ≥3 条 + `@Locale` + meta 禁手改核对结果在案

## Phase 7 — 三态裁决汇总 + ck 报告落盘 + 双索引同步 + 零改动核证

> 统一类型：Add-heavy（2 Add + 2 Proof + 1 Decision）。
> Skill: code-quality-audit-prompt
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-mfg-workorder-r3.md`（新增）+ `ai-check-r3-index.md` + `docs/audits/check/ai-check-index.md`
> Prereqs: Phase 2~6 完成（五格 verdict 齐备）

- [x] <Decision> 全切片候选 finding 汇总复裁决：三态一致性、级别（P0~P3）、ID 规范（`P{n}-CK-mfg-{NNN}-r3`）、归属标注（fin-1/U20 归并指针）复核
      - Skill: code-quality-audit-prompt
      - 复裁决：新立 2 条（`P1-CK-mfg-022-r3` DIM-B ⑧⑨——reverseApprove 全链无 docStatus 守卫 × P1-CK-mfg-002 修复无条件回写 DRAFT 叠加，终态工单复活可达，归并 P2-CK-mfg-010 同型控制点 + 后果实质升级，原 ID 状态不动；`P3-CK-mfg-023-r3` DIM-F——dashboard/main.page.yaml 遮蔽孪生文件 CRP 日期参数错配，运行时零影响）——ID 规范符合 `P{n}-CK-mfg-{NNN}-r3`（022/023 接续 r1 序列 001~021），级别判据对照 §1.1 严重性指南/§2 三态表逐条复核一致；复用 5（r1 fixed 全数 HEAD 复核有效）+ 归并 16（r1 open 全数现场复核仍在，追加 T0 证据，历史 ID 零覆写）；归属标注：posting 引擎内部→fin-1、common 基类→U20、聚合横切→U21、mfg-2/mfg-3 邻界面→M1.6/M1.7，全部落 ck 报告 §2.4。
- [x] <Add> 落盘 `ck-mfg-workorder-r3.md`：五维覆盖矩阵 + finding 列表 + 统计 + 剩余风险声明
      - Skill: none
      - 证据：`docs/audits/check/2026-09-06-1645-ai-check-r3/ck-mfg-workorder-r3.md` 落盘——五维覆盖矩阵 5/5（B=finding / F=finding(minor) / S=pass / T=pass / I=pass）+ 三态裁决三节（复用 5 / 归并 16 / 新立 2）+ 归属标注节 + 统计表 + 剩余风险四件套（已查/未深查边界/残留风险登记/successor 触发条件）；审计时点 HEAD `dd39e6cce` + 零脏面披露在报告头部。
- [x] <Add> 双索引同步：本轮索引产物清单追加 + 跨轮索引 §报告清单 M1.5 行 + §Finding 追踪新立 ID 行
      - Skill: none
      - 证据：本轮 `ai-check-r3-index.md` 产物清单追加 `ck-mfg-workorder-r3.md` 行（M1.5，覆盖矩阵/裁决统计/新立 ID/时点）；跨轮 `ai-check-index.md` §报告清单追加 M1.5 行（P0/P1/P2/P3 = 1/1/0/0 新立 + 裁决摘要）+ §Finding 追踪追加 `P1-CK-mfg-022-r3`/`P3-CK-mfg-023-r3` 两行（级别/维度/归并指针/摘要/arm-index 裁决/open 状态/修复方向），插接于 r1 mfg-021 行后保持域分组；历史 ID 零覆写。
- [x] <Proof> 零生产代码改动核证：`git status --porcelain` 过滤生产路径 expect 空
      - Skill: none
      - 证据：`git status --porcelain | grep -E '^.. (module-.*\.(java|xml|yaml|csv|sql)|app-erp-all/src)'` = 零命中（exit 1）；全部脏面 = 恰 4 个审计产物（本轮索引 + 跨轮索引 + 本计划勾选 + ck-mfg-workorder-r3.md 新增）——roadmap 规则 6 零生产代码改动核证通过。
- [x] <Proof> 收尾回归：`mvn test -pl module-manufacturing/erp-mfg-service` 复跑全绿
      - Skill: none
      - 证据：复跑 `Tests run: 308, Failures: 0, Errors: 0, Skipped: 0` + `BUILD SUCCESS`（exit 0）——与 Phase 5 首跑计数一致，本切片收官全绿。

Exit Criteria:

- [x] `ck-mfg-workorder-r3.md` 落盘且五维矩阵 5 格 verdict 完整
- [x] 双索引行追加在案；零生产代码改动核证通过；mfg service 回归根绿

## Draft Review Record

- dispatch review #review-2026-09-07-171530-mission-driver-2026-09-08-1042-2-m15-mfg-workorder-five-dim-audit-1-83e0756c to 2026-09-07-171530-mission-driver
- 2026-09-08：iteration 1，共识 accept #review-2026-09-07-171530-mission-driver-2026-09-08-1042-2-m15-mfg-workorder-five-dim-audit-1-83e0756c（补 Closure Gates——按姊妹 M1.1 同批先例为本只读审计计划定制门控；基线「仓库现状」快照刷新至审查时点实核——HEAD `40512c567`→`8825a10e1`、姊妹 plan `2026-09-07-2200-1` 已 ACCEPT 收官、脏面=本批 3 计划；Phase 5 回归锚重指——known-good-baselines 无模块级 mfg 计数行，308 参照改挂 git `1166339ae` + MI 终态行聚合对照面；Phase 5 Targets `<SVC>` 补绑定注记；冻结清单 §1.5/§3.2/§4/§6-E1、MI 终态行数字、r1 统计 21 finding、CAT 31/9、state-machine.md 290 行、三项技能名、`TestErpSeedDataIntegrity`/`validate:flux`/auth 勘误路径逐一实仓复验在盘；frontmatter `status: draft` → `active`）

## Closure Gates

> 仅在所有项目和每个阶段的退出标准都勾选 `[x]` 后关闭。本计划为只读审计（零生产代码改动），验证命令组即结果表面本身（Phase 1~7 所列），完整仓库验证不在此重复（MV.1 对照面 = `known-good-baselines.md` 2026-09-08 MI 终态行；roadmap 收官机制承载全仓回归与 done 翻转）。ledger 格式计数域仅含 Phase 勾选项，本节门控以普通条目记录核对结果。

- 范围内行为完成（Phase 1~7 全部执行项与退出标准 `[x]`；五格 verdict 完整——`ck-mfg-workorder-r3.md` 覆盖矩阵 5/5，缺一格不算完）
- 相关文档对齐（双索引同步在案——本轮 `ai-check-r3-index.md` 产物清单行 + 跨轮 `ai-check-index.md` §报告清单 M1.5 行与 §Finding 追踪新 ID 行；历史 ID 零覆写；零生产代码改动 = owner doc 零对齐义务）
- 已运行验证：Phase 1 双 checker 红线（compliance checker 对照 M0.3 快照行 + CJK report mode 对照 0/0/0/0）+ Phase 4 `TestErpSeedDataIntegrity` 全绿 + Phase 5/7 `mvn test -pl module-manufacturing/erp-mfg-service` 两次全绿 + Phase 6 `--strict` PASS + `--self-test` PASS + Phase 7 零改动核证（`git status --porcelain` 过滤 `module-*`/`app-erp-all` 生产路径 expect 空）——各命令数字落 Phase 勾选注记；任一红线漂移 = 登记裁决后再闭包，不就地吞掉
- 无范围内项目降级为 deferred/follow-up（finding 的「归属 fin-1/U20」归并标注与修复归 M2.x 是审计产出分流，非范围降级）
- 独立草案审查已完成并记录（见 Draft Review Record，iteration 1）
- 文本一致性已验证：状态、阶段、门控和日志都一致（ledger 格式：frontmatter `status: active` 保持，完成态由全部勾选 + `## Verification` pass 线 + `## Closure` 回执派生；当日日志条目 `docs/logs/2026/09-08.md` 落盘）
- 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- 结束证据存在于文件中（Phase 勾选注记 + `ck-mfg-workorder-r3.md` + 双索引行 + `## Verification` pass 线 + `## Closure` 审计回执）

## Verification

- pass test 20260908-1320-closure-r1 exit=0

> 闭包 visit 记录：全仓 `mvn test` BUILD SUCCESS（exit 0，Finished at 2026-09-08T13:20:18+08:00，模块级汇总 3936/0/0 + 1 pre-existing `@Disabled` skip）；同日首轮/二轮全仓跑在 ct（`TestErpCtContractExpiryJob`）与 log（`TestErpLogTrackingPollJob`）快照上命中已登记的墙钟毫秒竞态模式（`docs/bugs/2026-08-25-frozen-clock-millis-testclock-displacement.md`——`@var` 合并 + 1ms 秒界劈裂 `not-equals-var-value`，隔离复跑即绿，非 mfg 范围、零生产代码改动），第三轮全仓跑全绿；`mvn clean install -DskipTests` 156/156 BUILD SUCCESS（02:04）。

## Closure

- dispatch audit #audit-20260908-1320-m15-mfg-workorder-1-73648d68 to 2026-09-07-171530-mission-driver models={exec:zhipuai-coding-plan/glm-5.3-flash,aud:zhipuai-coding-plan/glm-5.3-flash}
- accepted #audit-20260908-1320-m15-mfg-workorder-1-73648d68：独立闭包审计 ACCEPT——35/35 勾选全绿，五维矩阵 5/5 verdict 落盘（B=finding / F=finding(minor) / S/T/I=pass；三态裁决复用 5 / 归并 16 / 新立 2——P1-CK-mfg-022-r3 + P3-CK-mfg-023-r3，历史 ID 零覆写，实仓抽查 P1 证据与 `ErpMfgWorkOrderProcessor` 活码一致）；闭包 visit 实跑全仓 `mvn test` BUILD SUCCESS exit 0（3936/0/0 + 1 pre-existing skip；首轮/二轮 ct/log 快照命中 docs/bugs/2026-08-25 已登记墙钟毫秒竞态模式、隔离复跑即绿、第三轮全绿）+ `mvn clean install -DskipTests` 156/156 BUILD SUCCESS + `git status` 生产路径零触碰核证（只读审计零改动红线保持）；plan-check `--strict` derivedCompleted 成立
