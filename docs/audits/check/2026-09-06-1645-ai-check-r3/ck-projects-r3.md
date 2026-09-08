# ck-projects-r3 — projects U07 五维符合性审计报告（ai-check-r3 M1.12）

> 工作项：M1.12（U07 × 五维全格 + U09 × 五维全格，冻结清单 §4 映射表第 12 行；本报告 = U07 projects 半格，U09 半格见 `ck-quality-r3.md`）。
> 执行日期：2026-09-08。审计时点 T0：HEAD `c8509908fcfbe6eb81ac94e9685efac8094b2c38`（2026-09-08 20:03；计划基线 `f40b4bbae` 后唯一推进 = M1.10 hr 报告落盘 docs-only 提交，生产代码零变化）；脏面 = 2 条 untracked 计划文件（本计划 `2026-09-08-1454-2` + 姊妹 `2026-09-08-1454-3`），tracked 零修改。
> 判定依据（冻结）：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md` §1/§2（判定标准/机械核查程式/跨轮查重）+ §3.3 U07 行 + §6 勘误 E1。执行期仅在报告与计划勾选注记追加证据，判定标准零改动。
> 范围：projects 全域（C 级，无切片细分）——项目/任务(DAG)/工时/成本归集/结算/预算/PnL/开票/里程碑全实体族 + processor（`ErpPrjProjectSettlementProcessor` 族）+ posting/cost/pnl/dashboard/report/job 子包（`module-projects/erp-prj-{dao,service,web}` src/main）；owner docs `docs/design/projects/`（state-machine/cost-collection/profitability/task-dag/use-cases/README）。Skills：`nop-platform-conformance-audit-prompt` + `code-quality-audit-prompt`（DIM-B）；其余维 Skill: none。
> 切片边界（§3.2）：posting 引擎内部归 fin-1（M1.1 已收官，本切片仅审消费侧调用点）；common 抽象族基类行为归 U20（M1.15），本切片只审调用点（17/17 AbstractErpCrudBizModel 接入合规）；聚合横切面归 U21；notify 消费点归 U11。
> 零生产代码改动（roadmap 规则 6）：本报告 + 双索引 + 计划勾选注记为唯一产物（收官 `git status` 机械核证见 §4）。

## 1. 五维覆盖矩阵（5/5 完整）

| 维度 | 判定面（U07 全格） | 机械核查结果 | verdict |
| --- | --- | --- | --- |
| **DIM-B 后端平台合规** | §1.1 全套机械程式 + 15 维度走查（重点②⑧⑨⑮）+ 维度⑮断言抽样 2 doc × 10 断言 | 反模式族全零（`extends RuntimeException`=0 / `@Inject private`=0 / `System.currentTimeMillis`=0；`@Transactional` 3 文件命中全为 javadoc「不叠加 @Transactional」说明引注，真实共存=0，R6=2 基线站为 finance Facade）；checker 19 规则 = M0.3 快照行零漂移；`__XGEN_FORCE_OVERRIDE__` 12 处全为 dict.yaml codegen 校验点零手改；聚合器（E1 路径）含 `/erp/prj/auth/erp-prj.action-auth.xml`（L11）；15/15 无跳维：①编排 Java + xbiz 脚本分工合规；②17/17 BizModel 走基类+I*Biz，跨域读取豁免面齐全（ExpenseCostAggregator E3 注释、SettlementProcessor:310-312 E3 注释、Dashboard L40/Report L48-58 豁免 javadoc），rollbackAssetIfNeeded daoFor(ErpAstAsset) 跨域写 = r1 P2-CK-prj-014 归并（追加 :63 I*Biz 已注入未用于回滚证据）；③ErpPrjErrors 31 码集中；④⑤⑥⑦⑩⑪⑬⑭ pass（notGenCode 5 实体 + :976 DAG 注记；tenantId 零预置；orgId 缺失归并 P2-RC-086 族）；⑧**结算防重同类型守卫在位**（CreateSettlementProcessor:32-41→findActiveSettlementOfType:343 `eq("settlementType")` 仅 FINAL/CLOSE，P1-CK-prj-004 修复复核有效）+ **工时回滚闭环完整**（CancelProcessor:37-48 GL 红冲→posted 三件套清空→归集行删/头减回/actualCost 减回，P1-CK-prj-001 修复复核有效）+ 5 状态机 Bean 注册在位；⑨job 三件套接线在位 + doApprove 双轴联动一致；⑮2 doc × 10 断言 9/10 一致（1 漂移：state-machine.md §适用对象三 Billing「零 writer CRUD 桩」断言过期）。**新发现 4 条全 P3**（见 §2.3） | **finding**（4 新立 P3；8 复用 + 13 归并，见 §2） |
| **DIM-F 前端页面与 E2E** | §1.2 全套 + 项目看板/盈利页 + 全部实体页面族 | `npm run validate:flux` step [1/3] `FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855`（= 基线精确一致）；[3/4] `files=855 validated=855 errors=325`——325 条 100% variant 同型既有外部漂移（prj 域 20 条同族成员，非 variant ERR=0），successor 在案非本切片 finding；`component="AMIS"` 保留层 0、ORM `ext:web-renderer="flux"` 无缺失；实测 17 实体页面族（任务书面 16 系漏计 ActivityType）+ 8 手写页，89 yaml 源头链与 M0.4 注册表（B#87-94/A#29-34）100% 一致；33 保留层 view.xml 全 `x:extends="_gen/…"` bounded-merge；数据访问全 `@query:`/`@mutation:` REST（dashboard 5×`@query:ErpPrjDashboard__*`）；i18nEn 全覆盖（MI.8 批 1 修复态复核在位）；dashboard/kanban 孪生双文件参数一致无 mfg-023 型漂移；graphql 命中仅 `graphql:labelProp` XMeta 元数据键；E2E：`E2E_ENGINE` 缺省 flux（engine.ts:8-13），prj specs GraphQL 断言 0，`.cxd-` 命中（prj-cost-collection.write L112）= flux-skip 门控遗留 AMIS DOM 验证块（L101 `test.skip`，同型遍布 4 crud write spec + 19 visual，M1.8 ast-inventory.write 同型判 0 先例），系统性 skip 门控遗留不立项 | **pass**（confirmText i18n 债务归属标注见 §2.4） |
| **DIM-S seed 数据** | §1.3 全套 + project/cost_collection/timesheet/budget/project_pnl seed + 金额自洽 | `TestErpSeedDataIntegrity` **4/0/0/0 全绿**（BUILD SUCCESS 12.98s）；`git status --porcelain _init-data/` 空（零 seed 变更，双面快照重录义务未触发）；资产清点 **372 CSV + 1 SQL**（= 冻结口径登记值）；deploy `_seed_*.sql` 全仓仅 cs/notify 两族且均已在 seed-data.md 登记处 ✅ 已聚合（prj 无 deploy seed 零同步义务）；金额自洽：**`project_pnl.totalCost`(30000) ↔ Σ`cost_collection.totalAmount`(头 30000 = LABOR 20000 + MATERIAL 10000)**（seed-data.md:194 登记约束逐字成立）+ `project.actualCost=30000` 三方一致；pnl 种子行 = `TestErpC12PrjTimesheetSettlement:66` 登记「零改写零消费」静态演示行（revenue/分类拆分为 07-08 批手作值，登记约束面成立，超约束货币性观察记 §4 残留风险） | **pass** |
| **DIM-T 单元测试** | §1.4 全套 + 工时/结算/成本聚合族对账 + P1 清单行核对 | `mvn test -pl module-projects/erp-prj-service` **179/0/0/0 全绿**（= cjk-baseline §批注账 MI.6 批 2 行锚点 `prj 179` 精确一致，零回归）；覆盖对账（本域无 `_cases` 快照目录，直断言风格同 hr-1 先例，按测试类枚举）：Project 7 动作 ↔ Precheck+StateMachineMatrix+CrudSmoke+BaselineIoC/DeltaOverride；Task 9 ↔ TaskDependency+TaskStateMachineMatrix+TestTaskDependencyValidator+kanban E2E/visual；Timesheet 5 ↔ TimesheetCost+MulticurrencyPosting+TimesheetAndSettlementStateMachines+PostingFailureAlert+FxRateResolution；Settlement 7 ↔ ProjectSettlement+SettlementRetention+SettlementStateMachines+AcctDocProviderAccountKey；CostCollection 2 ↔ BudgetAndCollection；Pnl 2 ↔ ProjectPnl+PnlCalcJob；Dashboard/Report ↔ Dashboard+GrossMargin+ReportRendering——**零业务关键缺口**；P1 工时结算链 = `TestErpC12PrjTimesheetSettlement#testPrjTimesheetSettlementClosedLoop`（集成闭环）+ 单测族双层在位（testing-strategy 清单无 projects 行，清单完备性观察记 §4）；`SnapshotTest.RECORDING` = 0；`delVersion`/`*` 通配零命中 | **pass** |
| **DIM-I i18n（MI 先行时序口径）** | §1.5 全套（仅零回归 + 白名单合规，不产同类 finding） | `--strict` **PASS exit 0**（0 new violations vs 冻结 SNAPSHOT，170 files 单向收紧）；`--self-test` **PASS**；prj 探针族（CAT-1 14/CAT-2 7）维持清零零回归；WHITELIST prj 条目 1 条（`ErpPrjDashboardBizModel.java`，cjk-baseline:333）四要素齐备（路径/理由 @Description 专属 E3 豁免批 2/2 Phase 3/owner doc 判定准绳表 #5/裁决来源 plan 2026-09-07-1715-1 Phase 3），实仓复核 `:63 @Description("项目看板 KPI…")` 登记准确零缺陷；`grep -L @Locale` *Errors.java = 空；`erp-*-meta` + `_vfs/i18n` 零手改 | **pass** |

## 2. Finding 三态裁决（§2 跨轮查重全维强制）

> 查重源：r1 `docs/audits/check/ai-check-index.md` §报告清单/§Finding 追踪（同域报告 `ck-projects.md` C5.1 全 21 条逐一比对）+ r2 只读目录 `2026-08-28-2049-ai-check-r2/`（无 prj 同型新独立登记）+ §Mission 基线快照（checker 各命中均为已裁决偏离）。**本轮新立 4 条**（`P3-CK-prj-022-r3`..`P3-CK-prj-025-r3`）；历史 21 ID 零覆写。

### 2.1 复用（同型已 fixed，HEAD 复核有效）— 8 条

| 原 ID | 修复在位证据（T0 = HEAD `c8509908`） |
| --- | --- |
| P1-CK-prj-001（工时 cancel 只红冲 GL 不回退归集） | `ProjectCostAggregator.rollbackFromTimesheet` 在位（L117-162：归集行删除 + 头 totalAmount 减回 + project.actualCost 减回 + 无行 no-op 幂等）；`ErpPrjTimesheetCancelProcessor:37-48` 三步接线；`TestErpPrjTimesheetCost` 回归行全绿 |
| P1-CK-prj-002（Billing.amountFunctional 零 writer） | `ErpPrjBillingBizModel` syncAmountFunctional 在位（defaultPrepareSave/Update） |
| P1-CK-prj-003（费用归集头级幂等键） | `ExpenseCostAggregator` 行级幂等键 `claim.code+lineNo` 在位（:171 I*Biz + :187-190 E3 注释）；`TestErpPrjExpenseAggregation` 断言在位 |
| P1-CK-prj-004（结算单无重复创建守卫） | `ErpPrjProjectSettlementCreateSettlementProcessor:32-41` FINAL/CLOSE 前置判重 → `findActiveSettlementOfType:338-347`（:343 `eq("settlementType", settlementType)` **同类型过滤确认**）；INTERIM 放行语义注记在位 |
| P1-CK-prj-005（CLOSE 结算失败资产卡滞留在役） | `ErpPrjProjectSettlementCancelProcessor.rollbackAssetIfNeeded` 已与 posted 解耦（移出 posted 分支） |
| P1-CK-prj-006（工时/结算 tryPost 孤儿凭证窗口） | F1.1/F1.2 收敛在位：工时链归集前移 tryPost 之前（ApproveProcessor:49-55 F1.2 注记）+ billHeadCode 稳定；`TestPrjPostingFaultInjection` 全绿 |
| P1-CK-prj-007（看板毛利率多快照全量求和） | `ErpPrjDashboardBizModel.getProjectGrossMargin` latestByProject（periodTo DESC, id DESC，非 CALCULATED 跳过）；`TestErpPrjDashboardGrossMargin` 全绿 |
| P2-CK-prj-008（裸 CrudBizModel 无状态守卫） | F1.3 统一基类接入在位：17/17 BizModel extends `AbstractErpCrudBizModel`（posted/APPROVED 拒通用 update/delete；调用点合规，基类行为缺陷归 U20/M1.15） |

### 2.2 归并（同型 open 追加证据至原 ID）— 13 条

| 原 ID | r3 复核证据（T0，原 ID 状态不动仍 open） |
| --- | --- |
| P2-CK-prj-009 | 入参边界缺失原样：Timesheet submit 负 hours 无校验 + createSettlement settlementType 字典值无校验/project 缺失 NPE 面未变 |
| P2-CK-prj-010 | 结算链过账失败无告警原样：`ProjectSettlementPostingDispatcher.tryPost:46-55` 失败仅 LOG，无 notificationBiz（对照工时链 `prj.timesheet-posting-failure` 在位——同域不对称仍在） |
| P2-CK-prj-011 | refreshPnl 幂等键 `to=today()` 逐日漂移原样（ProjectPnlCalculator 调用面未变） |
| P2-CK-prj-012 | CostCollection 头 amountSource/amountFunctional 建头一次性写入原样（seed 实证：头 30000/30000 与行累计一致为静态巧合，动态聚合只累加 totalAmount） |
| P2-CK-prj-013 | 结算 Provider 未按 RC-R1.64 双金额范式折算原样 |
| P2-CK-prj-014 | rollbackAssetIfNeeded 经 daoFor(ErpAstAsset) 跨域直写原样（SettlementProcessor:223-233）；**追加加重证据**：同类 :63 已注入 `IErpAstAssetBiz` 并 :213 用于建卡，回滚却绕行——同类内双范式不一致 |
| P3-CK-prj-015 | 聚合链 N+1 与全表加载原样（ExpenseCostAggregator 全表载入已审核报销单+逐单调行等） |
| P3-CK-prj-016 | 归集头创建与幂等去重无 UK/锁支撑原样 |
| P3-CK-prj-017 | PnL 收入口径漂移原样（sumRevenue 仅排 CANCELLED 无审批态过滤，PnlCalculator:154-161；posted 列零 writer 死守卫 :79-82） |
| P3-CK-prj-018 | PnL 窄期间成本按归集头 businessDate 归属原样（findCostHeads:198-206 头级日期过滤） |
| P3-CK-prj-019 | 延期预警消费 CANCELLED 原样（Dashboard:146 仅排 COMPLETED）；orgId 全域缺失复用 P2-RC-086 注记 |
| P3-CK-prj-020 | 宽 catch 静默 + 错误码误用 + 审计字段兜底 "system" 原样 |
| P3-CK-prj-021 | 死常量与死方法原样（结算借贷科目 billData 键零消费、parseAmount 零调用） |

### 2.3 新立 `-r3` — 4 条（全 P3）

**P3-CK-prj-022-r3**（DIM-B 维度⑨）

- **控制点**：`ErpPrjProjectSettlementBizModel`（approve/reject/reverseSettlement/cancel/returnRetention/createSettlement，:58-100）+ `ErpPrjTimesheetBizModel`（submit/approve/reject/cancel）+ `ErpPrjProjectBizModel`（startProject/holdProject/resumeProject/closeProject/cancelProject）+ `ErpPrjTaskBizModel` 四迁移——审批-冲销-生命周期动作**零 action 级 auth 声明**：保留层 xbiz 全为空 `<actions/>`（`ErpPrjProjectSettlement.xbiz` 实证）、Java 侧无 @Auth，repo 全域 grep `ErpPrj*:approve` 权限引用 = 0（仅菜单级 FNPT）。
- **对照**：同单元 Billing/Budget/CostCollection xbiz 脚本三动作 `<auth permissions="ErpPrjXxx:*">` 在位；姊妹域同型 Processor 委托 approve 均声明 auth（`ErpSalOrder.xbiz:16-24` `ErpSalOrder:approve` 实证）——跨域范式不对称。
- **问题/严重性**：高价值动作（结算过账审批/红冲/转固）当前仅登录态即可触达，权限模型纵深防御缺口。定 **P3**（子代理建议 P2 经复裁降档：r1 同族缺 FNPT/auth 注册 b2b-011/drp-018 均判 P3；主守卫 = 状态机 Bean + F1.3 状态锁基类在位；action-auth enforcement 上下文 = enableActionAuth 配置灰度未全开）。
- **建议修复方向**：4 实体保留层 xbiz 增 `<mutation><auth/>` 增量声明（零逻辑覆盖，镜像 ErpSalOrder 范式）；或 ADRAM 显式豁免裁决。

**P3-CK-prj-023-r3**（DIM-B 维度②）

- **控制点**：`ProjectCostAggregator.rollbackFromTimesheet:128/139/142/145/149/152`——I*Biz 回退链（lineBiz.findList/deleteEntity、collectionBiz.get/updateEntity、projectBiz.get/updateEntity）全程 `new ServiceContextImpl()`，丢弃调用方 `ErpPrjTimesheetCancelProcessor` 持有的真实 `IServiceContext`（用户身份/数据权限不进管道）。
- **对照**：同模块 `ExpenseCostAggregator:182-185` 有 `IServiceContext.getCtx()` 优先取真实上下文的兜底范式。
- **问题/严重性**：上下文传递断裂，权限/审计语义弱化（回退动作用匿名上下文执行）；P3（行为面只读+回退正确性不受影响）。
- **建议修复方向**：`rollbackFromTimesheet` 增加 context 参数并透传（镜像 ExpenseCostAggregator 兜底范式）。

**P3-CK-prj-024-r3**（DIM-B 维度⑧ dict 治理）

- **控制点**：① `erp-prj/timesheet-status` 字典（orm.xml:44-48 定义）**零列绑定**——`ErpPrjTimesheet.status` 实绑 `wf/approve-status`（:230），常量注释自认「已合并到 approve-status」（ErpPrjConstants）——残留死字典；② `erp-prj/pnl-calc-status` 的 PENDING 值**零 writer**（常量 ErpPrjConstants:86 无消费，refreshPnl 恒写 CALCULATED，ProjectPnlCalculator:122）。
- **问题/严重性**：死字典/死值治理债（lesson 10 同族静态面）；P3。
- **建议修复方向**：ORM（ask-first 保护区域）登记清理或复用决策；PENDING 保留为初始态语义则补注释登记。

**P3-CK-prj-025-r3**（DIM-B 维度⑮ owner-doc 漂移）

- **控制点**：`docs/design/projects/state-machine.md` §适用对象三 :156「ErpPrjBilling 18 行 CRUD 桩，零 writer，完整状态机属 successor」↔ 实仓 `ErpPrjBilling.xbiz` 现含 5 个审批 script mutation（submitForApproval/approve/reject/reverseApprove/withdrawApproval，写 approveStatus='APPROVED'/'REJECTED' + approvedBy/At）——Billing 已非零 writer 桩，doc 未随实现同步。
- **问题/严重性**：owner-doc 漂移（断言过期误导后续审计/开发）；P3。
- **建议修复方向**：§适用对象三 补实现注记（同 :19 风格），不修需求契约段。

### 2.4 归属标注（§3.2 共享代码边界 + 跨域横切）

- posting 引擎内部（ErpFinPostingProcessor/REQUIRES_NEW 语义/sweep）**归属 fin-1**（M1.1 已收官）；本切片仅核对 TimesheetPostingDispatcher/ProjectSettlementPostingDispatcher/ProjectPostingExecutor 消费侧调用序（tryPost/reverse 正确、质保金返还失败显式抛 ERR_RETENTION_RETURN_POSTING_FAILED）。
- `TimesheetPostingDispatcher.resolveSubjectCode:281` daoFor(ErpMdSubject) 跨域读——判归 **R2a/R2d 科目解析基线站族**（M1.8 §2.4「posting dispatcher 科目解析 daoFor(ErpMdSubject) 基线站」同族先例不立项；同文件币种/汇率解析均走 IBizObjectManager I*Biz）。
- common 抽象族（AbstractErpCrudBizModel 状态锁/AbstractProcessor helper）调用点已审合规；基类行为缺陷归 U20（M1.15）。
- 聚合横切面（action-auth 聚合器/seed 全量装载/flux 导出门禁本体）归 U21（M1.16）；本格仅核 prj 注册在位性。
- **跨域 i18n 债务（DIM-F 归属标注）**：保留层 view.xml `<confirmText>`/`<messages><success>` 元素文本 CJK 无 i18n 承载（prj 26 处：Project 10/Task 8/Timesheet 8）——不在 CAT-4 门控面（仅 page/flux yaml）与 F15 门控面（label/title/displayName 属性）内，全域 18 域 520 处系统性既有（T0 前在），归 U21/M1.16 + successor 触发条件「F15/CAT-4 门控扩展覆盖 view.xml 元素文本时」，非本切片 finding。

## 3. 统计

| 级别 | 本轮新立 | 复用（fixed 复核有效） | 归并（open 追加证据） |
| --- | --- | --- | --- |
| P0 | 0 | 0 | 0 |
| P1 | 0 | 7（prj-001..007） | 0 |
| P2 | 0 | 1（prj-008） | 6（prj-009..014） |
| P3 | 4（prj-022..025-r3，DIM-B ⑨②⑧⑮） | 0 | 7（prj-015..021） |
| **合计** | **4** | **8** | **13** |

五格 verdict：DIM-B **finding**（4 新立 P3，minor 级）/ DIM-F **pass** / DIM-S **pass** / DIM-T **pass** / DIM-I **pass**。历史 21 条 r1 ID 状态零覆写（8 fixed 复核有效 + 13 open 追加证据）。

## 4. 剩余风险声明（横切关注点 13 完整四件套）

- **已查**：erp-prj-service 32 个 main java 全读（processor 15 + entity 6 + cost 5 + posting 3 + pnl/dashboard/report/job + 状态机 5 Bean + Errors/Constants/Configs）+ orm.xml 字典/notGenCode/列绑定段 + 17 保留层 xbiz + beans/batch/job yaml + web 89 yaml 全量源头链核查（6+2 页精读）+ seed 6 CSV 逐行 + 机械程式全套实跑（checker 19 规则/反模式族/codegen 46 站点/聚合 E1/validate:flux/seed 门禁/prj 回归 179/strict+self-test）；owner docs 2 doc × 10 断言抽样；r1 21 条逐一比对裁决；r2 目录核对。
- **未深查（边界归属）**：posting 引擎本体（归 fin-1）；`AbstractErpCrudBizModel` 基类内部（归 U20/M1.15，本切片仅调用点）；`erp-prj-web` 全量渲染时行为（静态走查 + validate:flux 门禁，浏览器回归归看板运行时专项）；finance 侧 `IErpFinVoucherBiz.post/reverse` 契约内部（fin-1 收官面）；测试代码仅作覆盖对账消费。
- **残留风险（登记不裁决）**：① 13 条归并 open finding 修复归 M2.x（M2.6 hr+prj+qa 批），其中 P2-CK-prj-010（结算链告警缺失）与 P2-CK-prj-014（跨域 daoFor 直写，本次追加同类内双范式不一致加重证据）建议 M2.x 优先；② 4 条新立 P3 与 r1 同族修复时一并收口（022 auth 声明与 qa-029-r3 同型跨域对齐；023 上下文透传与全域 `new ServiceContextImpl()` 族同扫——扫描面归 U20/M1.15 裁决）；③ pnl 种子行（revenue 50000/分类拆分）为登记静态演示行，重算将产出不同拆分（billings 全批 CANCELLED 系 M1.2b 零漂移裁决）——演示数据货币性观察，登记约束面成立；④ testing-strategy §关键业务流清单无 projects 行（清单完备性观察，链路覆盖已由 C12 + 单测族实证）；⑤ `validate:flux` 325 条 variant 漂移为批前在案外部事项（successor：nop-chaos-flux dist 基线裁决）。
- **successor 触发条件**：M1.17 收官以覆盖矩阵完整性校验本报告 5/5 格；M2.x 修复以「先写失败测试」承接 §2.2/§2.3 open 项；view.xml 元素文本 i18n 债务在 F15/CAT-4 门控扩展时升格立项（触发即转 U21/M1.16 或独立 MI 批）。
