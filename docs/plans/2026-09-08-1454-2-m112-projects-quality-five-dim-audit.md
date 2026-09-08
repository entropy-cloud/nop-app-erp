---
status: active
mission: ai-check-r3
work-item: M1.12
group: "2026-09-08-1454"
verify: [test]
---

# 2026-09-08-1454-2 M1.12 projects + quality 五维符合性审计（U07 + U09 全格）

## Current Baseline

- 依赖已满足：M0.6 done（plan `2026-09-06-1451-3`）；MI.9 收官 done（plan `2026-09-07-1715-3`，独立 closure audit ACCEPT，2026-09-08；`docs/testing/known-good-baselines.md` 2026-09-08 `ai-check-r3 MI 终态行`：全 reactor `mvn test` 4006/0/0/1、CJK checker report mode CAT1..4 = 0/0/0/0、`--strict`/`--self-test` PASS、双 checker 零回归、白名单 27 文件四要素齐备）。M1.12 两单元 deps 均为 M0.6 + MI.9，已全部满足。
- 同 mission 先例：M1.1/M1.5/M1.8 三切片（plans `2026-09-08-1042-1/2/3`）已执行并独立闭包审计 ACCEPT，`ck-finance-posting-r3.md` / `ck-mfg-workorder-r3.md` / `ck-assets-lifecycle-r3.md` 已落盘——本计划沿用其通过的七阶段执行形态，扩展为双单元。
- 强制核对矩阵已冻结：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md`——本计划唯一格集合 = **U07 × 五维（全格）+ U09 × 五维（全格）= 10 格**（§4 映射表第 12 行）；判定标准/机械核查程式/跨轮查重程序以该冻结清单 §1/§2 为准，执行期只允许在查重裁决列追加证据，不改判定标准。
- U07 projects：owner doc `docs/design/projects/`（`README.md`/`state-machine.md`/`cost-collection.md`/`profitability.md`/`task-dag.md`/`use-cases.md`，实仓核验在盘）；物理面 `module-projects/erp-prj-{dao,service,web}` 的 `src/main`；实体族 = `ErpPrjProject*`/`ErpPrjTask*`（DAG）/`ErpPrjTimesheet*`/`ErpPrjCostCollection*`/`ErpPrjProjectSettlement*`/`ErpPrjBudget*`/`ErpPrjProjectPnl*`/`ErpPrjBilling*`/`ErpPrjMilestone*` + processor（`ErpPrjProjectSettlementProcessor`）；§3.3 焦点：B = 结算防重守卫（同类型）/工时回滚/成本聚合；F = 项目看板/盈利页；S = project/cost_collection/timesheet/budget/project_pnl seed + 金额自洽（pnl↔collection）；T = P1 工时结算清单行覆盖；I = prj 探针族（CAT-1 14/CAT-2 7，MI 后应零）。
- U09 quality：owner doc `docs/design/quality/`（`README.md`/`state-machine.md`/`spc.md`/`inspection-integration.md`/`recall.md`/`use-cases.md`，实仓核验在盘）；物理面 `module-quality/erp-qa-{dao,service,web}` 的 `src/main`；实体族 = `ErpQaInspection*`/`ErpQaNonConformance*`/`ErpQaAction*`/`ErpQaRecall*`/`ErpQaSpc*`/`ErpQaSamplingPlan*`/`ErpQaCalibration*` + 领域服务（`NcrLifecycleService`/`SpcCapabilityCalculator`/`SpcOutOfControlHandler`/`NcrReturnOrchestrator`/`NcrScrapAcctDocProvider`/`InspectionResultEvaluator`/`InspectionTemplateMatcher`/`RecallTargetLocator`）；§3.3 焦点：B = SPC 引擎双层门控/NCR 链/I*Biz 化消费点；F = 质检/NCR/SPC 页面；S = inspection/non_conformance/action + SPC 三表（1145-2 批 Strategy C）+ 引擎重算覆盖防护；T = P1 来料检验→NCR→退货清单行覆盖；I = qa 探针族（CAT-1 1/CAT-2 10，MI 后应零）。
- 共享代码唯一归属（冻结清单 §3.2）：common 抽象族行为缺陷归 U20（M1.15），本切片只审调用点；posting 引擎内部归 fin-1（prj 结转过账/qa 报废凭证的消费侧归本切片，涉引擎内部时标注「归属 fin-1」归并）；聚合横切面归 U21；notify 派发子系统本体归 U11。
- 跨轮查重源（§2）：r1 `docs/audits/check/ai-check-index.md`（`ck-projects.md` C5.1：0 P0 / 7 P1 / 7 P2 / 7 P3，done；`ck-quality.md` C5.2：0 P0 / 5 P1 / 9 P2 / 11 P3，done）+ r2 只读目录 `docs/audits/check/2026-08-28-2049-ai-check-r2/` + 索引 §Mission 基线快照（已裁决偏离不重复报告）。
- DIM-I 内嵌 MI 先行时序口径（冻结清单 §1.5）：prj/qa 探针族 MI 后已清零——DIM-I 维仅验证零回归与白名单合规，不再产生同类 finding；脚本红 = MI 回归升级裁决报 MI 通道；白名单四要素缺失 = 白名单登记缺陷 finding。prj 白名单 1 条（`ErpPrjDashboardBizModel.java`，C1）+ qa 白名单 1 条（`ErpQaDashboardBizModel.java`，C1）。
- 绿色基线：`docs/testing/known-good-baselines.md` 2026-09-08 MI 终态行（MV.1 对照面）；compliance checker 对照面 = M0.3 快照行（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42）；模块回归对照计数 prj 179 / qa 184（cjk-baseline §批注账 MI.6 批 2 行锚点；执行期以 known-good-baselines 最新行为权威，姊妹计划测试增量允许并披露）；`npm run validate:flux` step [1/3] 导出 0 error（999 页 / erp 855），整体 exit 1 余项 = 325 条既有 `variant=primary` stub 外部漂移（successor 在案，非本切片 finding）。
- 仓库现状（2026-09-08 起草时实核）：HEAD `f40b4bbae`，工作树零脏面。审计证据以实跑时点 HEAD + 脏面披露为准（MI.9 收官审计先例：脏树实跑 + 披露）。
- 剩余差距：U07/U09 各五格 verdict 未落盘；执行目录尚无 projects/quality 切片 `ck-*.md` 报告。

## Goals

- 按冻结清单对 U07 × 五维 + U09 × 五维共 10 格全跑（禁止抽样、禁止跳维），逐格落 verdict（`pass` / `finding` / `n-a` 带理由），产出两份报告 `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-projects-r3.md` + `ck-quality-r3.md`（各含五维覆盖矩阵 + finding 列表 + 统计 + 剩余风险声明，roadmap 横切关注点 13）。
- 每候选 finding 完成三态查重裁决（复用/归并/新立 `-r3` ID，历史 ID 永不覆写）；两单元 DIM-B 维各含 owner-doc 关键断言抽样 ≥2 doc × 2 断言（漂移则扩至全部该单元 owner doc）。
- 双索引同步：本轮 `ai-check-r3-index.md` 产物清单两行 + 跨轮 `docs/audits/check/ai-check-index.md`（§报告清单 M1.12 行 + §Finding 追踪新 ID 行）。
- 全程零生产代码改动（roadmap 规则 6）：只产 finding 与索引，收官 `git status` 机械核证。

## Non-Goals

- 不修复任何 finding（M1 只读审计；修复归 M2.x，强制先写失败测试）；不改任何生产代码/ORM/api.xml/配置/页面/seed 文件。
- 不覆盖 U12/U13/U18/U17/U19（M1.14）与 U10/U15/U16/U11/U01/U20（M1.15）等其他单元格；prj/qa 跨域消费点行为按 §3.2 归属标注，不在本切片重复立项。
- 不接管 r1/r2 工作项（横切关注点 5）；不重开既有裁决（lesson 09/10、compliance baseline、CJK 白名单、SPC/CRP 双层门控 seed 静态行裁决）；不做 roadmap 状态翻转（done 转换由结束审计机制处置）。
- DIM-I 不产生同类 CJK finding（§1.5 内嵌口径）。

## Phase 1 — 执行目录就位 + 切片红线与脏面披露

> 统一类型：Proof（3 项 Proof）。
> Skill: none
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/`（幂等复用，不新建目录）；盘点注记落本计划勾选注记
> Prereqs: 无

- [x] <Proof> `mkdir -p docs/audits/check/2026-09-06-1645-ai-check-r3/`（本轮唯一规范执行目录，幂等复用；roadmap 规则 8），确认 `ai-check-r3-index.md` 与 `m0-5-audit-checklists.md` 在位
      - Skill: none
      - 证据（2026-09-08 T0）：目录已存在（16 条目，幂等复用零新建）；`ai-check-r3-index.md` 与 `m0-5-audit-checklists.md` 均在位；索引头部登记路径 = `docs/audits/check/2026-09-06-1645-ai-check-r3/` 与实际一致
- [x] <Proof> 记录审计时点仓库状态：HEAD hash、`git status --porcelain` 脏面清单、同批姊妹计划（`2026-09-08-1454-1/3`）执行状态披露（MI.9 收官审计脏树实跑先例）；后续全部证据注记引用该时点
      - Skill: none
      - 证据：**T0 = HEAD `c8509908fcfbe6eb81ac94e9685efac8094b2c38`（2026-09-08 20:03，docs-only 提交——plan 基线 `f40b4bbae` 后唯一推进 = M1.10 hr 报告落盘提交，生产代码零变化）**；脏面 = 2 条 untracked 计划文件（本计划 `2026-09-08-1454-2` + 姊妹 `2026-09-08-1454-3`），无 tracked 修改；姊妹计划状态披露：`1454-1`（M1.10 hr）已执行并独立闭包审计 ACCEPT（提交 c8509908f）；`1454-3`（M1.13 pur+sal+inv）未执行（roadmap M1.13 = todo，untracked 草稿在盘）。后续全部证据注记引用 T0
- [x] <Proof> 切片红线基线：`bash docs/audits/nop-compliance-checker.sh`（对照 M0.3 快照行）+ `node tools/check-hardcoded-cjk.mjs`（report mode，对照 0/0/0/0 终态）实跑记录；漂移即登记（不就地裁决）
      - Skill: none
      - 证据（T0 实跑）：compliance checker **exit 0 零漂移**，全表与 M0.3 快照行逐值一致（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42，其余 0）；CJK checker report mode **CAT1..4 = 0/0/0/0**（3430 java + 886 yaml 扫描，per-domain 违规节空）= MI 终态行精确一致；无漂移登记项

Exit Criteria:

- [x] 执行目录就位且本轮索引头部登记路径一致；时点/脏面披露在案
- [x] 双 checker 红线数字在案且与 M0.3 快照行 / MI 终态行对账一致（或漂移已登记）

## Phase 2 — DIM-B 后端平台合规走查（U07 + U09，15 维度 + R1-R12 + 断言抽样）

> 统一类型：Proof-heavy（3 Proof + 2 Decision）。
> Skill: nop-platform-conformance-audit-prompt + code-quality-audit-prompt（roadmap M1.12 行指定，承 M1.1 行「同上」）
> Targets: `module-projects/erp-prj-dao|erp-prj-service/src/main/java` + `module-quality/erp-qa-dao|erp-qa-service/src/main/java`；走查 verdict 落勾选注记
> Prereqs: Phase 1 完成

- [x] <Proof> 冻结清单 §1.1 机械核查程式全套实跑（两单元范围）：compliance checker 逐规则对照 + 反模式 grep 族（`extends RuntimeException`/`@Inject private`/`System.currentTimeMillis`/`@Transactional`×`@BizMutation` 对照 R6 基线/`IDaoProvider|IOrmTemplate|@SqlLibMapper` 命中处注释理由）+ codegen 产物安全（`__XGEN_FORCE_OVERRIDE__`、`_gen` 脏面）+ 聚合完整性（按 §6 勘误 E1 路径）
      - Skill: nop-platform-conformance-audit-prompt
      - 证据（T0）：① checker 19 规则零漂移（= Phase 1 行，R6=2 基线站为 finance Facade REQUIRES_NEW×2）；② 反模式族 prj+qa 四目录全零（`extends RuntimeException`=0/`@Inject private`=0/`System.currentTimeMillis`=0）；`@Transactional` 命中 5 文件（prj Timesheet/Task BizModel + ProjectPostingExecutor、qa RecallProcessor + NcrPostingExecutor）经逐文件核验**全为 javadoc 说明性引注**（「不叠加 @Transactional」「事务钉 Facade REQUIRES_NEW」），真实注解共存=0；`IDaoProvider|IOrmTemplate` 命中 prj 21 类 / qa 18 类逐点核对：per-mutation Processor 同域 daoFor = 编排层标准范式（r1 全切面口径、M1.8 同批先例不立项）、Dashboard（prj L40/qa L44）+ prj Report（L48-58）+ NcrLifecycleService（L31-34）豁免注释在位；**无注释跨实体站点** = qa NcrPostingDispatcher:121-131/NcrReturnOrchestrator:131-141（ErpInvStockBalance）+ ErpQaReportBizModel:361（ErpMdMaterial，Report 缺 Dashboard 同型豁免注释）→ 新立 qa-027-r3；prj TimesheetPostingDispatcher.resolveSubjectCode:281 daoFor(ErpMdSubject) 判归 R2a/R2d 科目解析基线站族（M1.8 §2.4「posting dispatcher 科目解析 daoFor(ErpMdSubject) 基线站」同族先例不立项）；③ codegen 安全：`__XGEN_FORCE_OVERRIDE__` prj 12 + qa 34 = 46 处全为 dict.yaml codegen 只读校验点（src/main 与 target/classes 镜像各半），`git status` `_gen`/`_` 前缀产物 = 空；④ 聚合完整性（E1 勘误路径）`nop/main/auth/app.action-auth.xml` x:extends 列表含 `/erp/prj/auth/erp-prj.action-auth.xml`（L11）+ `/erp/qa/auth/erp-qa.action-auth.xml`（L13）
- [x] <Proof> U07 projects 15 维度逐维走查（程序式确定性走查落 verdict；焦点：⑧状态机——结算防重守卫（同类型）/工时回滚；②跨实体——成本聚合跨域读；⑨审批流/作业；⑮断言抽样 `state-machine.md` + `cost-collection.md`/`profitability.md` ≥2 doc × 2 断言）；blocker/major/minor 按 prompt 严重性指南分级
      - Skill: nop-platform-conformance-audit-prompt
      - 证据（T0，15/15 无跳维）：①pass（编排 Java + xbiz 脚本分工合规，无手改生成物）；②**finding**——17/17 BizModel 全走 AbstractErpCrudBizModel+I*Biz，跨域读取豁免面齐全（ExpenseCostAggregator:187-190 E3 注释、SettlementProcessor:310-312 E3 注释）；rollbackAssetIfNeeded 仍 daoFor(ErpAstAsset) 直写跨域（:223-233，且 :63 已注入 IErpAstAssetBiz 并 :213 用于建卡——同类内不一致加重）= r1 P2-CK-prj-014 仍 open 归并；**新发现 rollbackFromTimesheet 全程 `new ServiceContextImpl()` 丢弃调用方 context**（ProjectCostAggregator:128/139/142/145/149/152 五站点，对照 ExpenseCostAggregator:182-185 兜底范式）→ P3-CK-prj-023-r3；③pass（ErpPrjErrors 31 码集中零散落）；④pass；⑤pass（CoreMetrics/StringHelper 全覆盖、零第三方工具 import）；⑥pass（17/17 基类接入 + @Name+context 尾参规范）；⑦pass（orm.xml:912-976 notGenCode 5 实体 + :976 DAG 约束注记在位）；⑧pass（**结算防重同类型守卫在位**：CreateSettlementProcessor:32-41 → findActiveSettlementOfType:343 `eq("settlementType")` 仅 FINAL/CLOSE 判重 = P1-CK-prj-004 修复复核有效；**工时回滚闭环完整**：CancelProcessor:37-48 GL 红冲→posted 三件套清空→rollbackFromTimesheet 归集行删/头减回/actualCost 减回 = P1-CK-prj-001 修复复核有效；5 状态机 Bean 注册 app-service.beans.xml:13-22；**dict 死值新观察**——`erp-prj/timesheet-status` 字典零列绑定（status 实绑 wf/approve-status，:44-48 vs :230）+ `pnl-calc-status` PENDING 零 writer（常量 ：86 零消费）→ P3-CK-prj-024-r3）；⑨**finding**——job 三件套接线在位（pnl-calc.job.yaml→batch.xml→PnlCalcHelper:75-84 逐条 REQUIRES_NEW+失败隔离）+ doApprove 双轴联动一致；**Settlement/Timesheet/Project/Task 四实体 Java 审批-冲销-生命周期动作零 auth 声明**（保留层 xbiz 空 `<actions/>`、无 @Auth；同单元 Billing/Budget/CostCollection xbiz 脚本三动作 auth 在位；姊妹域同型 Processor 委托 approve 均声明 auth——ErpSalOrder.xbiz:16-24 实证）→ P3-CK-prj-022-r3；⑩pass；⑪pass（tenantId 零预置；orgId 缺失归并 r1 P2-RC-086 族）；⑫pass（指针：28 测试类+15 `_cases` 目录，深查归 DIM-T）；⑬⑭pass（机械项）；⑮**2 doc × 10 断言**（state-machine 5 + cost-collection 5）——9/10 一致，1 漂移（state-machine.md §适用对象三 :156「Billing 18 行 CRUD 桩零 writer」↔ ErpPrjBilling.xbiz 现 5 个审批 script mutation 写 approveStatus——doc 未随实现同步）→ P3-CK-prj-025-r3（1 漂移 < 2 未触发扩样）；blocker=0/major=0/minor=4（新发面全 P3）
- [x] <Decision> U07 候选 finding 逐条过 §2 三态裁决（查 r1 `ck-projects.md` §Finding 追踪 + r2 目录 + §Mission 基线快照）：复用/归并原 ID/新立 `P{n}-CK-prj-{NNN}-r3`；裁决证据落勾选注记
      - Skill: code-quality-audit-prompt
      - 裁决（r1 ck-projects 21 条逐一比对 + r2 目录无同型 + §Mission 基线快照 checker 命中均裁决偏离）：**复用 8**（prj-001..007 全 fixed——004 结算防重/001 工时回滚/005 posted 解耦回退/007 latest 快照四项 HEAD 复核修复在位有效 + prj-008 F1.3 基类 17/17 接入在位）；**归并 13**（prj-009 入参边界（submit 负 hours/settlementType NPE 未修）、prj-010 结算链告警缺失（Timesheet 链有 dispatchFailureAlert、Settlement 链仅 LOG——Dispatcher:46-55 复核原样）、prj-011 refreshPnl 幂等键、prj-012 CostCollection 头金额一次性、prj-013 结算 Provider 双金额、prj-014 rollbackAssetIfNeeded daoFor 跨域写（追加证据：:63 IErpAstAssetBiz 已注入未用于回滚）、prj-015 N+1、prj-016 归集头无 UK、prj-017 PnL 收入口径+posted 死守卫（PnlCalculator:79-82/:144-161 复核原样）、prj-018 窄期间成本归属、prj-019 延期预警消费 CANCELLED（Dashboard:146 复核原样）、prj-020 宽 catch 族、prj-021 死常量——均 open 追加 r3 复核证据）；**新立 4**（均 P3）：`P3-CK-prj-022-r3`（⑨四实体审批动作零 auth 声明，跨域范式不对称；子代理建议 P2 经复裁降 P3——r1 同族 b2b-011/drp-018 缺 FNPT 注册均 P3、主守卫=状态机+F1.3 基类）、`P3-CK-prj-023-r3`（②rollbackFromTimesheet 上下文丢弃）、`P3-CK-prj-024-r3`（⑧dict 治理：timesheet-status 死字典 + pnl-calc-status PENDING 死值）、`P3-CK-prj-025-r3`（⑮Billing 桩断言 owner-doc 漂移）；**dispatcher 科目解析 daoFor(ErpMdSubject) 判基线站族不立项**（M1.8 §2.4 同族先例）；历史 21 ID 零覆写
- [x] <Proof> U09 quality 15 维度逐维走查（程序式确定性走查落 verdict；焦点：⑧状态机——SPC 引擎双层门控（seed 静态行不被引擎重算覆盖）与 NCR 链迁移；②跨实体 I*Biz 化消费点——NCR→退货/召回跨域调用；⑮断言抽样 `state-machine.md` + `spc.md`/`inspection-integration.md` ≥2 doc × 2 断言）；blocker/major/minor 按 prompt 严重性指南分级
      - Skill: nop-platform-conformance-audit-prompt
      - 证据（T0，15/15 无跳维）：①pass（常量/配置集中，保留层改动零手改生成物）；②**finding**——16/16 BizModel 干净；I*Biz 化消费点：NCR→退货经 IErpPurReturnBiz/IErpSalReturnBiz ✓、NCR→召回经 IErpQaRecallBiz ✓、RecallTargetLocator 全 I*Biz（inv/sal 四接口）✓、报废凭证经 IErpFinVoucherBiz ✓；**但金额/仓库解析经 daoFor(ErpInvStockBalance) 跨域直查无豁免**（NcrPostingDispatcher:121-131 + NcrReturnOrchestrator:131-141，且裸 setLimit(1) 无排序——limit1 面归 qa-011）→ P2-CK-qa-027-r3（含 ErpQaReportBizModel:361 ErpMdMaterial 缺豁免注释站点）；**SPC 域 I*Biz 声明未用簇**（SpcCapabilityCalculator:3-4 import 未用+:44-47 javadoc 称 I*Biz 回写 vs :287-298 daoFor 直写；SpcOutOfControlHandler:51-53 死注入 vs :99/:116 daoFor；InspectionTemplateMatcher:30-73 IErpQaInspectionTemplateBiz 在位未用无豁免）→ P3-CK-qa-028-r3；③pass（ErpQaErrors 35 码集中；宽 catch 面=qa-021 归并）；④pass（@Inject setter 齐备、事务钉 Facade javadoc 引注、SPC 级联 afterCommit 模式 B）；⑤**finding**（=qa-019 归并扩证：Java 源 LocalDate.now 零命中、但 `erp-qa.spc-enabled` 死配置 javadoc 自称「双层门控第二层」零消费 + 双 cron 键零消费 + `recall-require-approval`/`ERR_RECALL_APPROVAL_REQUIRED` 零消费同族扩员——归并 P3-CK-qa-019）；⑥pass；⑦pass（notGenCode 5 实体全 md 域带机制 B 注记）；⑧**finding**——NCR 链 6 动作状态机 Bean 完整、dict↔writer 全对照 ncr-status 5 值全有 writer（OVERDUE/STALE 死值=doc 已登记一致）；**SPC 双层门控裁决复核**：job 层 `nop.job.erp-qa-spc-*.enabled|false` 默认关在位 = 冻结裁决（1145-2 §SPC 引擎重算覆盖风险 + seed-data.md L19）成立不重开；其死配置第二层缺陷归 qa-019；**新发现 severity 字典污染**（UpgradeToRecallProcessor:52-54 NCR severity=NORMAL 直写 recall.severityLevel，recall-severity 字典无 NORMAL 值，:52 注释谎称「码值对齐」）→ P2-CK-qa-026-r3；⑨**finding**——Recall xbiz 五动作 auth 在位 + qa-003 inject 修复复核有效 + 失败隔离达标；**NCR 财务 mutation 无显式 auth**（ErpQaNonConformance.xbiz 空 `<actions/>`，action-auth 无 FNPT 子节点，与 Recall 不对称）→ P3-CK-qa-029-r3；**spc-sampling.batch.xml collectSamples 无 per-item 容错**（poison chunk）→ P3-CK-qa-030-r3；qa-007 让步自审复核原样归并；⑩pass（Delta 覆盖机制声明在位零违规）；⑪pass（tenantId 零预置；qa-011 orgId 面归并）；⑫pass（指针：test-mock 三件套跨域 mock + spc.md:96 seed 配方）；⑬⑭pass；⑮**2 doc × 14 断言初抽**（state-machine 6 + spc 8）——**2 漂移**（spc.md:29 clCenterType「=10/20/30」数字编码注记 vs string 字典；spc.md:168 数据契约 range/stdDev vs Dashboard loadSpcSamples:307-330 未返回）→ **触发扩样至全部 owner doc**：4 doc × 33 断言（README 8/inspection-integration 8/recall 8/use-cases 9），追加漂移 10 处聚 3 簇（强制质检触发机制 3 doc 物料级 vs 单据类型级 config；字段/字典/状态命名簇 7 处——reference_* vs relatedBill*、erp-qa/approve-status vs wf/、IN_EXECUTION 幽灵态、use-cases NCR 漏第 5 态、spc.md 2 处、recall batchId；模板「物料类别级」2 doc 未实现）→ 新立 P3-CK-qa-031/032/033-r3（qa-031 疑似需求分歧只登记不裁决注记）；blocker=0/major=2/minor=6（新发面）
- [x] <Decision> U09 候选 finding 逐条过 §2 三态裁决（查 r1 `ck-quality.md` §Finding 追踪 + r2 目录 + §Mission 基线快照）：复用/归并原 ID/新立 `P{n}-CK-qa-{NNN}-r3`；裁决证据落勾选注记
      - Skill: code-quality-audit-prompt
      - 裁决（r1 ck-quality 25 条逐一比对 + r2 目录无同型 + §Mission 基线快照）：**复用 8**（qa-001..005 全 fixed——002 isInspectionCleared 取最新（Evaluator 复核）+ 003 batch inject（spc-capability.batch.xml FQCN 复核）+ 005 reverseApprove 双轴联动（RecallProcessor:166-176 复核）HEAD 有效 + qa-006 F1.3 基类 + qa-012/013 F1.1 引擎层收敛）；**归并 17**（qa-007 让步自审、008 classifyByCpk(null)、009 SPC N+1、010 系数表越界（已转静默回落残余）、011 裸 limit1（追加 :124/:138 双站点排序缺失证据）、014 空壳退货单（追加对照 ErpQaRecallGenerateReturnsProcessor:60-76 已带行证明能力在位）、015 RETURN 不受门控、016 escalateToRecall 死胡同（:97 注释自认）、017 enforceGate 并发、018 posted 三件套、**019 死配置族（扩员证据：spc-enabled「第二层」javadoc 失真 + recall-require-approval/ERR_RECALL_APPROVAL_REQUIRED 新站点）**、020 dashboard 全量、021 宽 catch（追加 SpcOutOfControlHandler:148-150 幂等预检站点）、022 报表排序、023 notifyCustomers、024 TemplateMatcher 退化、025 quantity 兜底——均 open 追加复核证据）；**SPC 静态行重算覆盖不重开**（Non-Goal 冻结裁决：1145-2 分析 §142「双层门控默认关 fresh-DB 不触发重算」+ seed-data.md L19 + M1.5 DIM-S 行同判例；其死配置面已归 qa-019）；**新立 8**（2 P2 + 6 P3）：`P2-CK-qa-026-r3`（severity NORMAL→recall severityLevel 字典污染 + 注释谎称对齐）、`P2-CK-qa-027-r3`（跨域 daoFor 三站点无豁免：NcrPostingDispatcher/NcrReturnOrchestrator/ErpQaReportBizModel）、`P3-CK-qa-028-r3`（SPC 域 I*Biz 声明未用簇 3 站点）、`P3-CK-qa-029-r3`（NCR 财务 mutation 零显式 auth 与 Recall 不对称）、`P3-CK-qa-030-r3`（spc-sampling batch 无 per-item 容错）、`P3-CK-qa-031-r3`（⑮强制质检触发机制 3 doc 漂移，疑似需求分歧只登记）、`P3-CK-qa-032-r3`（⑮字段/字典/状态/契约命名漂移簇 7 处）、`P3-CK-qa-033-r3`（⑮模板类别级设计层级未实现 2 doc，疑似需求分歧只登记）；历史 25 ID 零覆写

Exit Criteria:

- [x] U07×B 格 verdict 落盘（pass/finding/n-a 带理由），15 维度无跳维，断言抽样 ≥2 doc × 2 断言在案
      - verdict = **finding**（4 新立 P3：prj-022/023/024/025-r3；8 复用 + 13 归并）；15/15 无跳维；抽样 2 doc × 10 断言在案（9/10 一致）
- [x] U09×B 格 verdict 落盘（pass/finding/n-a 带理由），15 维度无跳维，断言抽样 ≥2 doc × 2 断言在案
      - verdict = **finding**（8 新立：2 P2 + 6 P3，qa-026..033-r3；8 复用 + 17 归并）；15/15 无跳维；初抽 2 doc × 14 断言 + 漂移触发扩样 6 doc × 47 断言在案（35/47 一致，12 漂移聚 4 簇）
- [x] 两单元机械程式输出数字在案且与基线对账一致；三态裁决完成（复用/归并/新立逐条在案）
      - checker 19 规则零漂移 + 反模式族全零 + codegen 46 站点零手改 + 聚合器双注册；prj 复用8/归并13/新立4、qa 复用8/归并17/新立8 逐条在案（上文两项 Decision 注记）

## Phase 3 — DIM-F 前端页面与 E2E 走查

> 统一类型：Proof（3 项 Proof）。
> Skill: none（判定锚点 = 冻结清单 §1.2 所列 architecture/design docs + e2e-runbook）
> Targets: `module-projects/erp-prj-web/src/main/resources/_vfs`（项目看板/盈利页）+ `module-quality/erp-qa-web/src/main/resources/_vfs`（质检/NCR/SPC 页面）；E2E 涉 prj/qa spec 时按 §1.2 ③
> Prereqs: Phase 1 完成

- [x] <Proof> 全局面静态门禁实跑（两单元共用全局数字）：`npm run validate:flux`（step [1/3] 导出 0 error / 999 页；整体 exit 1 余项 = 325 条既有 `variant=primary` stub 外部漂移，successor 在案）+ flux-only grep（`component="AMIS"` 保留层 expect 0 / ORM `ext:web-renderer="flux"` 缺失 expect 空）
      - Skill: none
      - 证据（T0 实跑）：step [1/3] `FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855`（= 基线精确一致）；step [3/4] `files=855 validated=855 errors=325`（325 条 ERR 100% 含 `variant`（非 variant ERR = 0）= 既有 dropdown-button variant 外部漂移同型族，整体 exit 1 余项即此，successor 在案非本切片 finding）；域命中对账：prj 20 条 + qa 19 条 ERR 全为同族成员（两域非 variant ERR = 0）；`component="AMIS"` prj/qa web 保留层 = 0；`ext:web-renderer="flux"` 两 ORM 无缺失
- [x] <Proof> U07 projects 页面走查：项目看板/盈利页对照 view-and-page-strategy（REST `/r/` 数据访问 / codegen vs 手写边界查 M0.4 矩阵 / 定制走 `x:extends` / i18n-en 承载约定）逐页落 verdict；涉 prj E2E spec 时核对 PageObject 模式 + `E2E_ENGINE` flux 缺省 + 禁 GraphQL 断言
      - Skill: none
      - 证据（T0，逐页清单落 ck-projects-r3.md §1 DIM-F 行）：prj 实测 17 实体页面族（含 ErpPrjActivityType，任务书面 16 系漏计 ActivityType）+ 8 手写页；89 yaml 全量源头链三步核查与 M0.4 注册表（B#87-94 + A#29-34）100% 一致零遗漏；33 保留层 view.xml 全部 `x:extends="_gen/…"` bounded-merge；手写页数据访问全 `@query:`/`@mutation:` REST（dashboard 5×`@query:ErpPrjDashboard__*`、project-pnl/settlement/report 同）；i18nEn 全覆盖（MI.8 批 1 修复态复核在位：dashboard 24/24、kanban 块式、pnl/settlement 15/15）；孪生双文件参数一致（dashboard endDate、kanban 拖拽 vs 行按钮 = Phase 0 记录的设计裁决，无 mfg-023 型漂移）；graphql 命中仅 `graphql:labelProp` XMeta 元数据键（非数据访问）；E2E：`E2E_ENGINE` 缺省 flux（engine.ts:8-13 return 'flux'）；prj spec 8 文件（crud/dashboards/reports/visual），qa-recall/qa-spc specs 违规选择器 = 0、GraphQL 断言 = 0；`prj-cost-collection.write.spec.ts` L112 `.cxd-InputTable` 命中经核证 = flux-skip 门控遗留 AMIS DOM 验证块（L101 `test.skip(getEngineType()==='flux')` 恒跳过）——同型遍布 4 crud write spec + 19 visual（ast-inventory.write 同型而 M1.8 判 0），系统性 skip 门控遗留非本切片回归，沿姊妹切片口径不立项
- [x] <Proof> U09 quality 页面走查：质检/NCR/SPC 页面（SPC chart/sample/capability 族）同上逐页落 verdict；涉 qa E2E spec 时同上核对；两单元查重：全局面 325 ERR 族与 prj/qa 域命中条数对账（既有外部漂移不立项）
      - Skill: none
      - 证据（T0）：qa 实测 16 实体页面族 + 8 手写页，源头链与 M0.4（B#99-106 + A#36-38）100% 一致；NCR 页保留层行按钮 `@mutation:__submitReview/__resolve/__cancel` + CAPA 抽屉、质检页批量/单张判合格 mutation、SPC chart/sample/capability 三页手写（chart 页 3×`@query` 含 Dashboard 聚合）；i18nEn 全覆盖（spc-chart tpl 失控/Normal 双语、capability tpl 双语、dashboard adaptor 脚本 EN 孪生）；ncr-disposal 存根页 = plan 2026-08-03-1232-4 Deferred 豁免在册；325 ERR 族域对账 = prj 20 + qa 19（其余 286 为他域同族成员），既有外部漂移零立项；**跨域归属标注**：view.xml 保留层 `<confirmText>`/`<messages><success>` 元素文本 CJK 无 i18n 承载（prj 26 + qa 22 处）——不在 CAT-4 门控面（仅 page/flux yaml）与 F15 门控面（label/title/displayName 属性）内，全域 18 域 520 处系统性既有（T0 前在），跨域横切归 U21/M1.16 + successor 触发条件「F15/CAT-4 门控扩展覆盖 view.xml 元素文本时」随报告登记，非本切片 finding

Exit Criteria:

- [x] U07×F 格 verdict 落盘；U09×F 格 verdict 落盘；全局面门禁数字在案对账一致
      - U07×F = **pass**（confirmText 债务归属标注在案）；U09×F = **pass**；门禁数字 0 error/999 页/855 页/325 ERR 与基线逐值一致
- [x] 两单元范围页面逐页走查完成，无跳页
      - prj 17 族 ×（main/picker/ref）+ 8 手写页 + qa 16 族 + 8 手写页全清单走查（逐页 verdict 落两报告 §1），抽样精读 6+2 页（prj dashboard/kanban 双文件/pnl + qa dashboard/NonConformance/spc-chart/ncr-disposal）

## Phase 4 — DIM-S seed 数据走查

> 统一类型：Proof（3 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/seed-data.md`）
> Targets: `app-erp-all/src/main/resources/_vfs/_init-data`（只读清点）+ prj seed（project/cost_collection/timesheet/budget/project_pnl）+ qa seed（inspection/non_conformance/action + SPC 三表）
> Prereqs: Phase 1 完成

- [x] <Proof> 引用完整性门禁实跑：`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` 全绿（363 实体 findAll + to-one 零悬空 + 零孤儿 CSV + scope-pinning 计数）
      - Skill: none
      - 证据（T0 实跑）：**4/0/0/0 全绿 BUILD SUCCESS**（12.98s，`mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity`）= M0.3/M1.5/M1.8 同门禁口径一致
- [x] <Proof> U07 seed 面核对：`git status --porcelain app-erp-all/src/main/resources/_vfs/_init-data/` expect 空 + 资产清点对账 + deploy `_seed_*.sql` 同步义务逐命中查登记处表 + 金额自洽（project_pnl ↔ cost_collection 聚合口径抽查，§3.3 U07 S 行）
      - Skill: none
      - 证据：`git status --porcelain _init-data/` = 空（零 seed 变更，双面快照重录义务未触发——只读审计正常态）；资产清点 **372 CSV + 1 SQL**（= 冻结清单 §1.3 ②「M1.5 批次后」登记值）；deploy `_seed_*.sql` 全仓仅 cs/notify 两族（6 文件）且均已在 seed-data.md 登记处 ✅ 已聚合（prj 无 deploy seed 零同步义务）；金额自洽：**`project_pnl.totalCost(30000.00)` ↔ Σ`cost_collection.totalAmount`(头 30000 = LABOR 20000 + MATERIAL 10000)**（seed-data.md:194 登记约束逐字成立）+ `project.actualCost=30000` 三方一致；附注：pnl 种子行（PRJ-PNL-2026-001 revenue 50000/cost 30000）= `TestErpC12PrjTimesheetSettlement.java:66` 登记的「零改写零消费」静态演示行，其 revenue/成本分类拆分为 07-08 批手作值（billings 全批 CANCELLED 系 M1.2b 零漂移裁决），重算将按现行引擎产出不同拆分——登记约束（totalCost↔collection）成立，超约束面为静态演示行货币性观察非 finding（剩余风险节披露）
- [x] <Proof> U09 seed 面核对：零 seed 变更 + inspection/non_conformance/action 表关联完整性抽查 + SPC 三表（1145-2 批 Strategy C）静态行不被引擎重算覆盖防护核对（域内 config 门控默认关）+ deploy `_seed_*.sql` 同步义务逐命中查登记处表
      - Skill: none
      - 证据：零 seed 变更（同上）；关联完整性抽查全命中——NCR-001/002 `inspectionId=2`→ErpQaInspection id=2 实存 + `sourceCode=INS-2026-002` 一致、NCR-003 `sourceType=SPC/sourceCode=SPC-CHART-001`→chart id=1 实存、action `ncrId=1`→NCR id=1 实存、recall-001 `sourceNcrId=1`→NCR 实存 + recall_target `recallId=1`/`batchNo=B20260701` 与 inspection-001 批号一致；SPC 三表静态行 = 1145-2 Strategy C 裁决行逐项在位（chart-1 parameterId=0 占位软引用 + CALC_STATUS=PENDING + cl/ucl/lcl 空、sample-1 isOutOfControl=true、capability-1 INADEQUATE；另 P/NP/C/U 四图 = plan 2026-07-19-0120-2 登记批 CALCULATED 静态值）；**重算覆盖防护 = 双 job 门控默认关在位**（`erp-qa-spc-sampling/capability.job.yaml` 均 `enabled: @cfg:nop.job.*.enabled|false`，fresh-DB 不触发重算）= 冻结裁决（1145-2 §SPC 引擎重算覆盖风险 + seed-data.md L19 + M1.5 DIM-S 行同判例）成立、Non-Goal 不重开；deploy seed 同步义务 = qa 无 deploy seed 零义务

Exit Criteria:

- [x] U07×S 格 verdict 落盘；U09×S 格 verdict 落盘；`TestErpSeedDataIntegrity` 全绿在案
      - U07×S = **pass**；U09×S = **pass**；门禁 4/0/0/0 全绿在案
- [x] seed 零变更 + 同步义务核对 + 金额自洽/重算防护结果在案
      - 零 seed 变更 + cs/notify 双登记处已聚合 + pnl↔collection 30000=30000 + SPC 双门控默认关逐项在案

## Phase 5 — DIM-T 单元测试走查

> 统一类型：Proof（6 项 Proof，两单元各 3）。
> Skill: none（判定锚点 = `docs/architecture/testing-strategy.md`）
> Targets: `module-projects/erp-prj-service` + `module-quality/erp-qa-service`（`<SVC>` 记法同冻结清单 §1.4）
> Prereqs: Phase 1 完成

- [x] <Proof> U07 本地回归：`mvn test -pl module-projects/erp-prj-service` 全绿零失败（数字落注记，对照 known-good-baselines + 姊妹计划增量披露）
      - Skill: none
      - 证据（T0 实跑）：**179/0/0/0 BUILD SUCCESS**——= `cjk-baseline.md` §批注账 MI.6 批 2 行锚点 `prj 179` 精确一致，零失败零回归；姊妹计划 1454-1（hr）/1454-3（pur+sal+inv）不触 prj 模块，无姊妹增量待披露
- [x] <Proof> U07 覆盖缺口对账：`grep -rc "@BizQuery\|@BizMutation"` 公开方法清单 × `ls <SVC>/_cases/...` 逐项对账（结算/工时/成本聚合族 BizModel）；P1 工时结算清单行覆盖核对；缺口按业务关键度定级
      - Skill: none
      - 证据：动作计数逐 BizModel——Project 7 / Task 9 / Timesheet 5 / Settlement 7 / CostCollection 2 / ProjectPnl 2（其余 11 实体 0 = 纯 CRUD 桩）；本域无 `_cases` 快照目录（直断言测试风格，同 hr-1 先例），对账改按测试类枚举：Project 族 ↔ Precheck+StateMachineMatrix+CrudSmoke+BaselineIoC/DeltaOverride；Task 族 ↔ TaskDependency+TaskStateMachineMatrix+TestTaskDependencyValidator+kanban E2E/visual（findBoardData 读侧由 E2E/visual 层承载）；Timesheet 族 ↔ TimesheetCost+MulticurrencyPosting+TimesheetAndSettlementStateMachines+PostingFailureAlert+FxRateResolution；Settlement 族 ↔ ProjectSettlement+SettlementRetention+SettlementStateMachines+AcctDocProviderAccountKey；CostCollection ↔ BudgetAndCollection；Pnl ↔ ProjectPnl+PnlCalcJob；Dashboard/Report ↔ Dashboard+GrossMargin+ReportRendering——**零业务关键缺口**；P1 清单行核对：testing-strategy §关键业务流快照测试清单（P0/P1/P2）**无 projects 行**（清单完备性观察记剩余风险，非缺口），工时→结算链覆盖实证 = `TestErpC12PrjTimesheetSettlement#testPrjTimesheetSettlementClosedLoop`（app-erp-all 集成闭环：submit/approve/cancel+PROJECT_COST_COLLECTION 凭证行+结算 CLOSE→posted→reverseSettlement 红冲+资产卡片回退）+ 上列单测族双层在位
- [x] <Proof> U07 快照纪律：`grep -rn "SnapshotTest.RECORDING" <SVC>/src/test/java` expect 0 + `delVersion`/decimal `*` 通配屏蔽合规抽查
      - Skill: none
      - 证据：`SnapshotTest.RECORDING` = 0 残留；`delVersion` 零命中（无屏蔽面）；`_cases` 无 `*` 通配屏蔽（无快照目录）；179 全绿 = CHECKING 态等价证明
- [x] <Proof> U09 本地回归：`mvn test -pl module-quality/erp-qa-service` 全绿零失败（数字落注记）
      - Skill: none
      - 证据（T0 实跑）：**184/0/0/0 BUILD SUCCESS**——= 批注账锚点 `qa 184` 精确一致，零失败零回归
- [x] <Proof> U09 覆盖缺口对账：同法对账（inspection/NCR/SPC 族 BizModel）；P1 来料检验→NCR→退货清单行覆盖核对；缺口按业务关键度定级
      - Skill: none
      - 证据：动作计数——Inspection 8 / NonConformance 7 / Recall 6 / Action 3 / SpcChart 4 / SpcCapability 1（其余 10 实体 0 = CRUD 桩）；测试类枚举对账：Inspection 族 ↔ InspectionStateMachine+ResultStateMachineMatrix+ApprovalStateMachineMatrix+InspectionTrigger+CriticalItemVeto+TemplateCrudSmoke+BusinessCancelLinkage；NCR 族 ↔ NcrCapaEndToEnd+NcrPosting+NonConformanceStateMachineMatrix；Recall 族 ↔ RecallE2E+RecallLocateNotifyReturn+RecallStateMachine+RecallStateMachineMatrix+RecallApprovalStateMachineMatrix；SPC 族 ↔ SpcSampling+SpcSamplingEvaluateBatch+SpcAttributesSampling+SpcAttributesControlLimit+SpcCapability+SpcOutOfControl+RuleEnginePure+Statistics+CapabilityFormulas+DashboardSpc(+Chart)；跨域 stub 测试 ↔ TestStubErpPurReturnBiz/SalReturnBiz/SalDeliveryBiz（I*Biz 契约面）；**零业务关键缺口**；P1 清单行「quality 来料检验→NCR→退货（4 实体，P1）」覆盖实证 = TestErpQaInspectionTrigger（来料/制程触发）→ TestErpQaNcrCapaEndToEnd/NcrPosting（REJECTED→NCR→RETURN 处置 + NcrReturnOrchestrator 经 TestStubErpPurReturnBiz 断言退货单创建）→ TestErpQaRecallLocateNotifyReturn + `TestErpC09QaNcrCapaScrap`（app-erp-all 集成）+ E2E qa-recall/qa-spc specs——清单行四层覆盖在位
- [x] <Proof> U09 快照纪律：RECORDING grep expect 0 + 屏蔽合规抽查
      - Skill: none
      - 证据：`SnapshotTest.RECORDING` = 0 残留；`delVersion` 命中 3 处均为软删断言/注释（TestErpQaBusinessCancelLinkage 审计可追溯断言，非屏蔽滥用）；`*` 通配零命中；184 全绿 = CHECKING 态等价证明

Exit Criteria:

- [x] U07×T 格 verdict 落盘；本地回归全绿数字在案
      - U07×T = **pass**（179/0/0/0；零关键缺口；testing-strategy 清单无 projects 行观察记报告）
- [x] U09×T 格 verdict 落盘；本地回归全绿数字在案
      - U09×T = **pass**（184/0/0/0；P1 清单行四层覆盖在案）
- [x] 两单元覆盖对账 + 关键业务流清单核对 + 快照纪律结果在案
      - 逐 BizModel 动作 × 测试类对账零关键缺口；prj 清单行缺位观察 + qa P1 行四层覆盖；RECORDING=0 + delVersion/通配合规双侧在案

## Phase 6 — DIM-I i18n 零回归与白名单合规（MI 先行时序口径）

> 统一类型：Proof（2 项 Proof）。
> Skill: none（判定锚点 = `docs/architecture/i18n-compliance.md` + `docs/audits/cjk-baseline.md`）
> Targets: 全域门控 + prj/qa 白名单条目核对
> Prereqs: Phase 1 完成

- [x] <Proof> 零回归门控：`node tools/check-hardcoded-cjk.mjs --strict`（exit 0，vs 冻结 SNAPSHOT 单向收紧）+ `--self-test` PASS；脚本红 = MI 回归，升级裁决报 MI 通道，不立 DIM-I finding
      - Skill: none
      - 证据（T0 实跑）：`--strict` **PASS exit 0**（0 new violations vs 冻结 SNAPSHOT，170 baseline files 单向收紧成立；actual 全部为 removed-from-tree 改善项）；`--self-test` **PASS**（self-test green）；report mode（Phase 1 行）CAT1..4 = 0/0/0/0——prj 探针族（CAT-1 14/CAT-2 7）与 qa 探针族（CAT-1 1/CAT-2 10）维持清零零回归，脚本零红无 MI 回归升级项
- [x] <Proof> 白名单合规核对：§WHITELIST prj 条目（`ErpPrjDashboardBizModel.java`）+ qa 条目（`ErpQaDashboardBizModel.java`）全数核对（各 1 条，不足 3 条时全数核对并记录条目数）四要素 + `grep -L "@Locale"` 全量 `*Errors.java` expect 空 + `git status --porcelain 'module-*/erp-*-meta/**' 'app-erp-all/src/main/resources/_vfs/i18n/**'` expect 空（§1.5 ④ 全式）；四要素缺失 = 白名单登记缺陷 finding
      - Skill: none
      - 证据：条目数记录 = prj 1 条（cjk-baseline.md:333）+ qa 1 条（:368），不足 3 全数核对；**prj 条目四要素齐备**（文件路径 ✓/理由 `@Description("中文") 专属文件 1 行同 E3 豁免 批 2/2 Phase 3` ✓/owner doc `i18n-compliance.md 判定准绳表 #5` ✓/裁决来源 `plan 2026-09-07-1715-1 Phase 3` ✓），实仓复核 `ErpPrjDashboardBizModel.java:63 @Description("项目看板 KPI…")` 登记准确；**qa 条目四要素齐备**（同构，批 2/2 Phase 4），实仓复核 `ErpQaDashboardBizModel.java:68 @Description("质检看板 KPI…")` 登记准确——零白名单登记缺陷；`grep -L @Locale` *Errors.java = 空（ErpPrjErrors/ErpQaErrors @Locale("zh-CN") 在位）；`erp-*-meta` + `_vfs/i18n` 手改核证 = 空

Exit Criteria:

- [x] U07×I 格 verdict 落盘；U09×I 格 verdict 落盘；`--strict`/`--self-test` PASS 在案
      - U07×I = **pass**；U09×I = **pass**（MI 先行时序口径：仅零回归 + 白名单合规验证，零同类 finding）；`--strict` PASS exit 0 + `--self-test` PASS 在案
- [x] 白名单四要素核对 + `@Locale` + meta 禁手改核对结果在案
      - 2 条白名单四要素全数核对零缺陷 + @Locale 全覆盖 + meta/i18n 零手改，逐项在案

## Phase 7 — 三态裁决汇总 + ck 报告落盘 + 双索引同步 + 零改动核证

> 统一类型：Add-heavy（3 Add + 2 Proof + 1 Decision）。
> Skill: code-quality-audit-prompt
> Targets: `docs/audits/check/2026-09-06-1645-ai-check-r3/ck-projects-r3.md` + `ck-quality-r3.md`（新增）+ `ai-check-r3-index.md` + `docs/audits/check/ai-check-index.md`
> Prereqs: Phase 2~6 完成（10 格 verdict 齐备）

- [x] <Decision> 两单元候选 finding 汇总复裁决：逐条确认三态与级别一致性与 ID 规范（`P{n}-CK-prj-{NNN}-r3` / `P{n}-CK-qa-{NNN}-r3`，历史 ID 永不覆写；升级级别按 code-history-deferred-triangulation-audit-prompt §3.1）
      - Skill: code-quality-audit-prompt
      - 证据：复裁决逐条过账——prj 新立 4 条 ID 规范核验（`P3-CK-prj-022-r3`..`P3-CK-prj-025-r3`，NNN 接续 r1 最大 021，无冲突）；qa 新立 8 条（`P2-CK-qa-026-r3`/`P2-CK-qa-027-r3` + `P3-CK-qa-028-r3`..`P3-CK-qa-033-r3`，NNN 接续 r1 最大 025）；级别一致性复裁：prj-022-r3 子代理建议 P2 经复裁降 P3（r1 b2b-011/drp-018 同族 P3 校准 + 主守卫在位，降档理由落报告）；qa-026-r3 定 P2（qa-004 主路径 P1 已修、升级路径窄面降档）；qa-027-r3 定 P2（跨域形态对齐 fin-007 P2，区别于 ast-028 同模块 P3 降档先例）；qa-031/033-r3 带「疑似需求分歧只登记不裁决」注记（r1 sal-016/mfg-009/hr-008 先例）；统计对账：prj 复用 8/归并 13/新立 4 = 21 历史 ID 全对账、qa 复用 8/归并 17/新立 8 = 25 历史 ID 全对账，零覆写零冲突
- [x] <Add> 落盘 `ck-projects-r3.md`：五维覆盖矩阵（5 格 verdict 全落）+ finding 列表（含归属标注与归并指针）+ 统计 + 剩余风险声明（横切关注点 13 四件套）
      - Skill: none
      - 证据：`docs/audits/check/2026-09-06-1645-ai-check-r3/ck-projects-r3.md` 落盘——§1 五维矩阵 5/5（B=finding/F=pass/S=pass/T=pass/I=pass 逐格机械证据）+ §2 三态裁决（复用 8/归并 13/新立 4 + §2.4 归属标注：fin-1/U20/U21/R2a 基线站/i18n 债务 successor 五项）+ §3 统计表（P1 0|7|0、P2 0|1|6、P3 4|0|7）+ §4 剩余风险四件套（已查/未深查/残留风险 5 项/successor 触发条件）
- [x] <Add> 落盘 `ck-quality-r3.md`：同上结构
      - Skill: none
      - 证据：`docs/audits/check/2026-09-06-1645-ai-check-r3/ck-quality-r3.md` 落盘——§1 五维矩阵 5/5（B=finding/F=pass/S=pass/T=pass/I=pass）+ §2 三态裁决（复用 8/归并 17/新立 8 + SPC 静态行裁决不重开声明 + §2.4 归属标注六项）+ §3 统计表（P1 0|5|0、P2 2|3|6、P3 6|0|11）+ §4 剩余风险四件套（含维度⑮扩样 6 doc × 47 断言 12 漂移聚 4 簇处置）
- [x] <Add> 双索引同步：本轮 `ai-check-r3-index.md` 产物清单追加两行 + 跨轮 `docs/audits/check/ai-check-index.md` §报告清单追加 M1.12 行 + §Finding 追踪新立 `-r3` ID 行（归并的追加证据至原 ID）
      - Skill: none
      - 证据：本轮索引 `ai-check-r3-index.md` 产物清单追加 `ck-projects-r3.md` + `ck-quality-r3.md` 两行（含矩阵/裁决/时点摘要）；跨轮索引 §报告清单追加两行（projects：0|0|0|4、quality：0|2|0|6，状态 done 附三态摘要）+ §Finding 追踪追加 12 行新立 ID（P3-CK-prj-022..025-r3 + P2-CK-qa-026/027-r3 + P3-CK-qa-028..033-r3，均 open + 修复方向）+ 底部追加 M1.12 prj+qa 复核注记（46 条历史 ID 逐一复核三态 + 归并追加证据指针 + SPC 裁决不重开声明），与两报告实际产物逐格一致
- [x] <Proof> 零生产代码改动核证：`git status --porcelain` 过滤 `module-*`/`app-erp-all` 生产路径 expect 空（roadmap 规则 6）
      - Skill: none
      - 证据（T0 后实跑）：过滤 `module-*`/`app-erp-all` = 空（exit 1 零命中）；全脏面 = 6 条 docs 面（2 修改索引 + 2 新报告 + 2 untracked 计划文件），零生产代码/ORM/配置/页面/seed 改动
- [x] <Proof> 收尾回归：`mvn test -pl module-projects/erp-prj-service` + `mvn test -pl module-quality/erp-qa-service` 复跑全绿（审计只读不变式复证；全仓验证归收官机制）
      - Skill: none
      - 证据（落盘后复跑）：prj **179/0/0/0** + qa **184/0/0/0** 双全绿——审计只读不变式复证成立

Exit Criteria:

- [x] `ck-projects-r3.md` + `ck-quality-r3.md` 落盘且两矩阵 10 格 verdict 完整（缺一格不算完）
      - 两报告落盘；10 格 verdict 全落（prj：B=finding/F=pass/S=pass/T=pass/I=pass；qa：B=finding/F=pass/S=pass/T=pass/I=pass）
- [x] 双索引行追加在案；零生产代码改动核证通过；prj/qa service 回归根绿
      - 本轮索引 2 行 + 跨轮索引 2 报告行 + 12 finding 行 + 复核注记在案；零生产改动核证通过；复跑 179 + 184 双全绿

## Draft Review Record

- dispatch review #review-2026-09-07-171530-mission-driver-2026-09-08-1454-2-m112-projects-quality-five-dim-audit-1-0ceb2992 to 2026-09-07-171530-mission-driver
- 2026-09-08：iteration 1，共识 accept #review-2026-09-07-171530-mission-driver-2026-09-08-1454-2-m112-projects-quality-five-dim-audit-1-0ceb2992（补 Closure Gates 只读收官定制门控（M1.8 同系列先例）；Phase 6 ④ 补 `app-erp-all/src/main/resources/_vfs/i18n/**` 路径对齐冻结清单 §1.5 全式；Phase 2 头部计数勘误 4 Proof→3 Proof；frontmatter `status: draft` → `active`。基线引用逐项实仓复核成立：冻结清单 §4 M1.12 行 / §3.3 U07+U09 焦点 / §1.5 内嵌口径 / r1 计数 C5.1=0|7|7|7、C5.2=0|5|9|11 / cjk-baseline 批注账 prj 179 + qa 184 / 白名单 prj 1 条 + qa 1 条（C1 @Description 专属，批注账 MI.6 批 2）/ known-good-baselines MI 终态行 4006/0/0/1 + M0.3 快照行 / HEAD `f40b4bbae`）

## Closure Gates

> 仅在所有阶段执行项与退出标准全部勾选 `[x]` 后关闭。本计划为只读审计（零生产代码改动，roadmap 规则 6），验证命令组即结果表面本身（各 Phase 所列红线），完整仓库 build/test 不适用——验证面 = Phase 1 双 checker 红线 + Phase 4 seed 门禁 + Phase 5 局部回归 + Phase 7 收尾复跑。ledger 格式计数域仅含 Phase 勾选项，本节门控以普通条目记录核对结果（MI.9 / M1.8 先例）。

- 范围内行为完成（U07 × 五维 + U09 × 五维共 10 格 verdict 全落盘且无跳维；`ck-projects-r3.md` + `ck-quality-r3.md`（五维覆盖矩阵 + finding 列表 + 统计 + 剩余风险声明）落盘）
  - ✅ 核对（2026-09-08）：两报告 §1 矩阵 10/10 格 verdict 落盘（prj B=finding/F=pass/S=pass/T=pass/I=pass；qa B=finding/F=pass/S=pass/T=pass/I=pass）；15 维 × 2 单元无跳维（各报告 DIM-B 行注明 15/15）
- 相关文档对齐（本轮索引 `ai-check-r3-index.md` 与跨轮 `ai-check-index.md` 追加行与两报告实际产物一致；roadmap 状态翻转不适用（Non-Goal））
  - ✅ 核对：本轮索引 +2 产物行、跨轮索引 §报告清单 +2 行 + §Finding 追踪 +12 行新立 ID + 底部 M1.12 复核注记，行内容与两报告 §2/§3 逐格一致；roadmap M1.12 行按 Non-Goal + 规则 3 + M1.5/M1.8/M1.10 先例仅追加「执行完成待收官翻转」注记不改 todo
- 已运行验证：`bash docs/audits/nop-compliance-checker.sh`（对照 M0.3 快照行）+ `node tools/check-hardcoded-cjk.mjs`（report mode CAT1..4=0 / `--strict` / `--self-test`）+ `mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` + `npm run validate:flux`（step [1/3] 0 error / 999 页）+ `mvn test -pl module-projects/erp-prj-service` + `mvn test -pl module-quality/erp-qa-service` 全绿——各 Phase 红线数字在案对账一致（若 compliance 漂移，按已知失败模式「Compliance 基线漂移」登记移交独立裁决，不就地裁决、不闭包）
  - ✅ 核对：九条 PASS 线落 `## Verification`（checker 零漂移无移交项；325 条 variant 漂移为批前在案外部事项非 compliance 面）
- 无范围内项目降级为 deferred/follow-up（§3.2 归属标注仅为 finding 归并指针，非本计划工作项降级）
  - ✅ 核对：10 格全跑无降级；§2.4 归属标注（fin-1/U20/U21/R2a 基线站/i18n 债务 successor）均为 finding 归并指针与边界登记
- 独立草案审查已完成并记录（见 Draft Review Record，iteration 1 accept）
  - ✅ 核对：Draft Review Record 在案（iteration 1 共识 accept，2026-09-08）
- 文本一致性已验证：frontmatter `status: active` 保持 = ledger 协议，完成态由全勾选 + `## Verification` pass 线 + `## Closure` 回执派生；10 格 verdict 与两报告统计逐格一致
  - ✅ 核对：frontmatter `status: active` 未改写（ledger 协议）；7 Phase 执行项 + 退出标准全 `[x]`；10 格 verdict 与两报告 §3 统计表逐格一致（prj 4/8/13、qa 8/8/17）
- 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
  - ✅ 核对：独立子代理闭包审计已执行（回执落 `## Closure`，task `ses_kxrbmvffspYecHTZTdAJog`）
- 结束证据存在于文件中（Phase 勾选注记含数字红线 + 两报告/双索引产物 + `## Closure` 审计回执 + 当日 `docs/logs/` 条目）
  - ✅ 核对：Phase 1-7 勾选注记含全部数字红线；两报告 + 双索引在盘；`docs/logs/2026/09-08.md` 当日条目在案

## Verification

- pass test 20260908-2142-closure-r1 exit=0

> 闭包 visit 记录（2026-09-08 21:42，独立 closer，fresh session）：`mvn test -pl module-projects/erp-prj-service` BUILD SUCCESS（exit 0，Tests run: 179, Failures: 0, Errors: 0, Skipped: 0 = 批注账锚点零回归）+ `mvn test -pl module-quality/erp-qa-service` BUILD SUCCESS（exit 0，184/0/0/0，`TestErpQaSpcOutOfControl` 毫秒边界 flake 本轮未触发）+ `node tools/check-hardcoded-cjk.mjs --strict` PASS exit 0 + `node tools/check-hardcoded-cjk.mjs --self-test` PASS——四项均本 visit 实跑复证。增量口径：闭包时点工作树改动全为 docs 审计产物面（非 docs 改动 = 0），按 mission 增量构建指引以 `-pl` 定面两 service 模块，不清 target 全量重编；完整仓库回归归 roadmap 收官机制（只读审计计划：验证命令组即结果表面本身，Closure Gates 定制门控，MV.1 对照面 = known-good-baselines 2026-09-08 MI 终态行）。以下为执行期各 Phase 红线的 PASS 记录（T0 = HEAD `c8509908`（2026-09-08）实跑，各 Phase 勾选注记含同源数字；闭包 visit 复跑覆盖 `test`/`--strict`/`--self-test` 收官态等价）：

- PASS `bash docs/audits/nop-compliance-checker.sh` — exit 0 零漂移，19 规则逐值 = M0.3 快照行（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42，其余 0）
- PASS `node tools/check-hardcoded-cjk.mjs`（report mode）— CAT1..4 = 0/0/0/0（3430 java + 886 yaml），= MI 终态行精确一致
- PASS `node tools/check-hardcoded-cjk.mjs --strict` — exit 0，0 new violations vs 冻结 SNAPSHOT（170 baseline files 单向收紧）
- PASS `node tools/check-hardcoded-cjk.mjs --self-test` — self-test green
- PASS `mvn test -pl app-erp-all -Dtest=TestErpSeedDataIntegrity` — 4/0/0/0 BUILD SUCCESS
- PASS `npm run validate:flux` — step [1/3] `FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855`；[3/4] files=855 validated=855 errors=325（100% variant 既有外部漂移，successor 在案；整体 exit 1 余项即此）
- PASS `mvn test -pl module-projects/erp-prj-service` — 179/0/0/0 BUILD SUCCESS（两轮：Phase 5 + Phase 7 复跑一致）
- PASS `mvn test -pl module-quality/erp-qa-service` — 184/0/0/0 BUILD SUCCESS（两轮一致）
- PASS `git status --porcelain` 过滤 `module-*`/`app-erp-all` 生产路径 — 空（零生产代码改动，roadmap 规则 6）

## Closure

Status Note: 全 7 Phase 执行项与退出标准全部勾选（ledger 格式，勾选即完成信号，frontmatter `status: active` 按 ledger 协议保持，完成态由本节回执 + `## Verification` pass 线派生）。U07 × 五维 + U09 × 五维共 10 格 verdict 全落盘（prj：B=finding（新立 4 全 P3）/F=pass/S=pass/T=pass/I=pass；qa：B=finding（新立 8：2 P2 + 6 P3）/F=pass/S=pass/T=pass/I=pass）；两报告 + 双索引落盘且逐格一致；零生产代码改动核证通过；prj 179 / qa 184 service 回归两轮全绿；SPC 双层门控 seed 静态行裁决复核成立不重开（Non-Goal 遵守）；roadmap M1.12 按 Non-Goal + 同批先例仅登记执行完成注记不翻转（done 翻转归结束审计机制）。

Closure Audit Evidence:

- Auditor / Agent: 独立子代理（fresh session，无执行者上下文；general subagent，闭包审计 7 项全量核证）
- Evidence: task `ses_f7ecf1160ffevrk1HobLboYUOG`（2026-09-08，T0 = HEAD `c8509908`）——**ACCEPT**：①计划完整性（7 Phase 全 `[x]` + frontmatter 协议保持 + Verification 9 PASS 线）②产物实存（两报告结构完整 + 统计逐格一致：prj 4/8/13 = 21 ID = r1 C5.1 `0|7|7|7`、qa 8/8/17 = 25 ID = C5.2 `0|5|9|11`）③双索引一致（+2 产物行 + M1.12 两行 `0|0|0|4`/`0|2|0|6` + 12 条 `-r3` ID 行 + 复核注记）④零生产改动（过滤 = 0 行，脏面 6 条 docs）⑤验证复跑（--strict PASS exit 0 + prj 179/0/0/0 + qa 184/0/0/0——qa 首跑 `TestErpQaSpcOutOfControl` 毫秒跨秒舍入 flake 单类复跑即绿，判时间边界环境 flake 非回归）⑥反 hollow 抽验 3/3 实证（qa-026 升级处理器注释谎称对齐属实 / prj-022 空 xbiz 属实 / SPC 双 job 默认关属实）+ 归并历史 ID 维持 open ⑦文本一致。回执义务 3 项已全部兑现：Closure 回执落账（本节）+ `docs/logs/2026/09-08.md` M1.12 条目 + roadmap M1.12 行执行完成注记；Minor-1 prj-023 行号引注（:128/139/142/145/149/152）已随回执修正于报告与计划注记；Minor-2 SPC 毫秒边界 flake 登记为测试基础设施观察。

- dispatch audit #audit-20260908-2142-m112-projects-quality-five-dim-audit-1-126e0494 to 2026-09-08-193051-mission-driver models={exec:zhipuai-coding-plan/glm-5.3-flash,aud:zhipuai-coding-plan/glm-5.3-flash}
- accepted #audit-20260908-2142-m112-projects-quality-five-dim-audit-1-126e0494：独立闭包审计 ACCEPT——44/44 勾选全绿（7 Phase 执行项 + 退出标准），五维矩阵 10/10 格 verdict 落盘（prj：B=finding 新立 4 全 P3 / F=pass / S=pass / T=pass / I=pass；qa：B=finding 新立 8（2 P2 + 6 P3 qa-026..033-r3）/ F=pass / S=pass / T=pass / I=pass）；三态裁决 prj 复用 8/归并 13/新立 4 + qa 复用 8/归并 17/新立 8，历史 21+25 ID 零覆写；两报告 + 双索引 + roadmap 注记 + 日志条目四方一致；零生产代码改动核证保持（脏面全为 docs 审计产物面）。闭包 visit（2026-09-08 21:42）实跑：`mvn test -pl module-projects/erp-prj-service` 179/0/0/0 + `mvn test -pl module-quality/erp-qa-service` 184/0/0/0 双全绿（exit 0）+ `check-hardcoded-cjk --strict` PASS exit 0 + `--self-test` PASS；plan-check `--strict` 结构绿，derivedCompleted 由本回执 + `pass test` 线成立（单模型降级如实声明：exec = aud = zhipuai-coding-plan/glm-5.3-flash）

Follow-up:

- 30 条归并 open finding（prj 13 + qa 17）+ 12 条新立 `-r3` finding 的修复归 M2.x（M2.6 hr+prj+qa 批，强制先写失败测试）——非本计划工作项，登记于两报告 §4 与跨轮索引。
- view.xml 元素文本 i18n 债务（全域 520 处）归 U21/M1.16 + successor 触发条件「F15/CAT-4 门控扩展覆盖 view.xml 元素文本时」。
- `validate:flux` 325 条 variant 外部漂移 successor 在案（nop-chaos-flux dist 基线裁决）；`TestErpQaSpcOutOfControl` 毫秒边界 flake 登记测试基础设施观察。
