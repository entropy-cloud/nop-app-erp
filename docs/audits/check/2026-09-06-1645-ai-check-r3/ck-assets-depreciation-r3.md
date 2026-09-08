# ck-assets-depreciation-r3 — assets「折旧与过账 + 盘点切片」ast-2 五维符合性审计报告（ai-check-r3 M1.9）

> 工作项：M1.9（U06 × 五维 × ast-2，冻结清单 §4 映射表第 9 行 / §3.3 U06 行）。
> 执行日期：2026-09-09。审计时点 T0：HEAD `a23975f2158145396af58bcc2ca734ea0f7cf1cc`（2026-09-09 04:58 `plan-2026-09-09-0232-2 M1.7 mfg-3 五维审计落盘 + 闭包审计回执 ACCEPT`——姊妹 M1.6/M1.7 计划落盘提交已推进基线快照，计划「实跑时 HEAD + 脏面披露」纪律覆盖）；脏面 = 仅 `?? docs/plans/2026-09-09-0232-3-m19-ast-depreciation-posting-five-dim-audit.md`（本计划自身，草案产物；同批无其他在制会话脏面）。
> 判定依据（冻结）：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md` §1/§2（判定标准/机械核查程式/跨轮查重）+ §3.3 U06 行 + §6 勘误 E1。执行期仅在本报告与计划勾选注记追加证据，判定标准零改动。
> 范围：折旧批量链（nop-job 路径 + catchUp 补提 + 折旧计划行生成/重算 + 资本化维修基数）/ 资产过账链（AcctDocProvider/PostingDispatcher 族 + 处置/盘点凭证联动 + F2.9 新增 `ErpAstDepreciationReversalListener` 合规面）/ 盘点→凭证闭环（盘盈盘亏差异→凭证，不含盘点单生命周期本体）——`module-assets/erp-ast-{dao,service}` src/main 折旧/过账/盘点→凭证族文件 + `depreciation.batch.xml` + `erp-ast-depreciation.job.yaml`；owner docs `depreciation-and-posting.md`（352 行，实仓实测）+ `use-cases.md`（244 行）。Skills：`nop-platform-conformance-audit-prompt` + `code-quality-audit-prompt`（DIM-B）；其余维 Skill: none。
> 切片边界：资产卡片生命周期状态机/资本化/拆分合并/价值调整/处置生命周期/盘点单生命周期与变动处置归 ast-1（M1.8 已审，本切片发现生命周期面缺陷按归属标注归并不立项）；posting 引擎内部（ErpFinPostingProcessor 幂等/sweep/resolveSubjects）归 fin-1（M1.1 已收官）；common 抽象族基类行为归 U20（M1.15），本切片只审调用点合规；聚合横切面归 U21（M1.16）。
> 零生产代码改动（roadmap 规则 6）：本报告 + 双索引 + 计划勾选注记为唯一产物（收官 `git status` 机械核证见本报告尾部与计划 Phase 7 注记）。

## 1. 五维覆盖矩阵（5/5 完整）

| 维度 | 判定面（ast-2 增量） | 机械核查结果 | verdict |
| --- | --- | --- | --- |
| **DIM-B 后端平台合规** | §1.1 全套机械程式 + 15 维度走查（重点⑥ nop-job 折旧批量/catchUp 补提、折旧方法族 UNITS、资本化维修基数、过账 Provider/Dispatcher 注册与 REQUIRES_NEW 面、盘点→凭证闭环）+ 维度⑮断言抽样 | 反模式族全零（`extends RuntimeException`=0 / `@Inject private`=0 / `System.currentTimeMillis`=0 / `@Transactional`∩`@BizMutation`=0——唯一命中 AssetPostingExecutor 为 javadoc 引注，真实注解=0）；checker 19 规则逐项 = M0.3 快照行零漂移（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42；R12a=71 即含 F2.9 ReversalListener 已知第 71 站）；`IDaoProvider/IOrmTemplate` 命中点逐一核对=既有裁决范式（per-mutation Processor daoFor 本模块实体 + `orm()` helper 仅 flushSession + Dashboard/Report javadoc 豁免 + `daoFor(ErpMdSubject)`/`daoFor(ErpFinAccountingPeriod)` = P1-MA4-015 已裁决），无新增无理由站点；`__XGEN_FORCE_OVERRIDE__` 35 处全为 codegen dict.yaml 只读校验点零手改（产物路径 git 空）；聚合器（E1 勘误路径）`x:extends` 含 `/erp/ast/auth/erp-ast.action-auth.xml`；F2.9 修复族 HEAD 复核 8 条全有效（001 UNITS 显式失败 ExecuteDepreciation L67-72/CatchUp L89-93、002 基数去 `.add(increment)` Recalc L58-64/88-89、003 当月增加下月提守卫 L46-61、004 逆资本化守卫 L122+cancelSchedules PENDING-only L337-339、005 ReversalListener bean 注册 L245-246+域内红冲 REVERSED 标记防双应用、006 VA originalValue 同步 L253/289+处置净值读 BILL_DATA_NET_BOOK_VALUE（1604 腿 Deferred 维持裁决）、011 F1.3 基类接入、021 (c) 收敛）；ReversalListener 合规面：`@Inject` 包级可见 + 跨实体经 I*Biz + javadoc 纪律声明零新增 daoFor；维度⑧ schedule dict 4 值全有 writer 无新增死状态；维度⑨ nop-batch 三件套功能在位（job yaml→nopBatchTaskRunner→batch.xml chunk batchSize=50，ast2-015 键名漂移 open 归并）；维度⑮ 2 doc × 9 断言——7 一致 + 2 漂移（§1.3 UNITS 行未随 F2.9 收窄 + §5.1「当月减少当月停」vs 补提含当期 r1 注记未修订）触发扩样、两 doc 全文走查。**新发现 2 条**：P2-CK-ast2-024-r3（CATCHUP 汇总凭证引擎侧红冲闭环缺口）+ P3-CK-ast2-025-r3（owner doc 未同步簇）——见 §2.3 | **finding**（2 新立：1 P2 + 1 P3；8 复用 + 15 归并 + 跨切片扩员 2，见 §2） |
| **DIM-F 前端页面与 E2E** | §1.2 全套 + ast-2 面 = 折旧计划表/折旧报表/盘点→凭证/手写页 | `npm run validate:flux` step[1/3] `FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855`；终态 `files=855 validated=855 errors=325`——325 条逐条同型（`variant="primary"`×dropdown-button 既有枚举漂移），ast 域无新增族成员，successor（roadmap MI.8 done 行）在案非本切片 finding，整体 exit 1 余项即此既有外部漂移（与 M1.8 轮同数字对账一致）；`component="AMIS"` 保留层 0、ORM `ext:web-renderer="flux"` 缺失 0；`ErpAstDepreciationSchedule.view.xml` 保留层 `x:extends="_gen/…"`+bounded-merge+自定义列全量 `i18n-en:label`；report 两页 `@query:ErpAstReport__renderHtml`+i18nEn 7/6；盘点页 `@query:ErpAstInventory__findPage`；disposal-wizard `@query/@mutation`+i18nEn 16；asset-repair=注册 Deferred stub（plan 2026-08-03-1232-4）；dashboard 孪生双文件 periodId 参数一致（无 mfg-023 型漂移）；M0.4 源头链 rows 1/3/4/7/8/9 判定与实仓一致；ast web graphql 命中 0；E2E：ast spec 族 6 件 = 业务动作 API 驱动型（runbook 登记通道），违规选择器 0，`E2E_ENGINE` 缺省 flux（engine.ts L9-14） | **pass** |
| **DIM-S seed 数据** | §1.3 全套 + asset/depreciation_schedule 金额自洽（2210-1 批口径） | `TestErpSeedDataIntegrity` **4/0/0/0 全绿**（BUILD SUCCESS，= M1.8 轮同计数）；`git status --porcelain _init-data/` 空（零 seed 变更，双面快照重录义务未触发）；资产清点 **372 CSV + 1 SQL**（= 冻结口径登记值）；deploy `_seed_*.sql` 全仓仅 cs/notify 两族且均已在 seed-data.md 登记处 ✅ 已聚合（ast 无 deploy seed 零同步义务）；自洽抽查：asset id=2（AST-2026-002 original=120000/accum=6000/NBV=114000）↔ 最新 schedule（2026-07 EXECUTED actual=2000/accum=6000/NBV=114000/posted=false 无凭证回链义务）双字段全等；id=1/3（accum=0 无 schedule 行）内部自洽 | **pass** |
| **DIM-T 单元测试** | §1.4 全套 + 折旧计提→计划行→过账凭证→红冲/补提（P1 清单族）逐行 | `mvn test -pl module-assets/erp-ast-service` **339/0/0/0 全绿**（= M1.8 执行后实测参照精确一致零回归）；覆盖对账：DepreciationSchedule 5 mutation ↔ TestErpAstDepreciation（含 F2.9 回归行 testUnitsMethodRejectedNotSilentZero/testPeriodBeforeAcquisitionMonthRejected）+ TestErpAstCatchUpDepreciation + TestDepreciationCalculator + ScheduleStateMachineMatrix + `_cases/job/TestErpAstDepreciationJob`；过账族 ↔ TestErpAstAcctDocProviderAccountKey + TestDepreciationPostingFailureAlert + TestAstPostingFaultInjection + TestErpAstPostingReverse（域侧红冲 5 测试）+ TestErpAstCapitalization（含 testReverseApproveRejectedAfterDepreciationExecuted）+ TestErpAstMaintenance（含 F2.9 计划总额断言）；盘点→凭证 ↔ TestErpAstInventory + InventoryStateMachineMatrix；关键业务流行计提→计划行→过账凭证→红冲→补提五行单测全绿；**覆盖缺口 1 处新立**（P2-CK-ast2-026-r3：`ErpAstDepreciationReversalListener` 引擎派发通道全仓零测试引用——见 §2.3）；`SnapshotTest.RECORDING` = 0 残留；`delVersion` 命中仅 javadoc、`*` 通配零滥用（339 全绿 = CHECKING 态等价证明） | **finding**（1 新立 P2，覆盖缺口） |
| **DIM-I i18n（MI 先行时序口径）** | §1.5 全套（仅零回归 + 白名单合规，不产同类 finding） | `--strict` **PASS exit 0**（0 new violations vs 冻结 SNAPSHOT，170 baseline files 单向收紧成立，CAT1..4=0/0/209/1318）；`--self-test` **PASS**；ast 探针族（CAT-1 39/CAT-2 29±）维持清零零回归；WHITELIST ast 条目（MI.6 批 1/2 Java 面 4 文件）全抽 4（≥3）：4/4 四要素齐备且 HEAD 实态一致（`IErpAstAssetBiz` @Description L38 / Dashboard @Description L56 / AssetBizModel @Description L93+audit 已英文化 / SuspendResume `IDLE_SINCE_PREFIX="闲置自 "` L35 + TestErpAstIdleStateMachine L199 契约断言）；`grep -L @Locale` 全量 *Errors.java = 空；`erp-*-meta`+`_vfs/i18n` 零手改 | **pass** |

## 2. Finding 三态裁决（§2 跨轮查重全维强制）

> 查重源：r1 `docs/audits/check/ck-assets-depreciation.md`（ast2 23 条逐一比对：索引终态 8 fixed / 15 open）+ 姊妹 r3 `ck-assets-lifecycle-r3.md`（ast 28 条，跨切片扩员指针）+ r2 只读目录 `2026-08-28-2049-ai-check-r2/`（F2.9 簇指针，无新独立登记）+ §Mission 基线快照（checker 各命中均为已裁决偏离）。**本轮新立 3 条**（自 `024` 起：ast2-024/025/026-r3）；历史 ID 零覆写。以下行号均为 T0=HEAD `a23975f21` 实测。

### 2.1 复用（同型已 fixed，HEAD 复核有效）— 8 条

| 原 ID | 修复在位证据（T0 = HEAD `a23975f21`，F2.9/F1.3/(c)收敛 族） |
| --- | --- |
| P1-CK-ast2-001（UNITS 折旧恒 0 静默失效） | 两调用点显式失败在位：ExecuteDepreciation L67-72 / CatchUp L89-93 抛 `ERR_DEPRECIATION_UNITS_NOT_CONFIGURED`（注释引用本 ID + owner doc §十 Deferred 声明）；回归测试 `TestErpAstDepreciation#testUnitsMethodRejectedNotSilentZero` 在位全绿。注记：owner doc §十 实文无 UNITS Deferred 行（代码注释引用失配）→ 归并进新立 P3-CK-ast2-025-r3 |
| P1-CK-ast2-002（资本化维修基数双计） | `recalculateForCapitalizationMaintenance` 基数 L58-64 去掉 `.add(increment)`（注释引用本 ID，按当前卡片状态重算）+ 计划行 NBV L88-89 同步；`TestErpAstMaintenance#testCapitalizePathWithDepreciationRecalc` 增断言在位全绿 |
| P1-CK-ast2-003（当月增加下月提守卫缺失） | ExecuteDepreciation L46-61 前置守卫 `!periodYm.isAfter(acquisitionYm)` → `ERR_DEPRECIATION_PERIOD_BEFORE_ACQUISITION`（注释引用本 ID）；`TestErpAstDepreciation#testPeriodBeforeAcquisitionMonthRejected` 在位全绿；owner doc §5.1 折旧调度规则行断言 ⑮ 抽样一致 |
| P1-CK-ast2-004（逆资本化红冲回退闭环断裂） | Cap `executeReverseApprove` L122 「存在已执行折旧拒绝」守卫（`ERR_CAPITALIZATION_HAS_EXECUTED_DEPRECIATION`）+ `cancelSchedules` L337-339 PENDING-only 过滤（注释引用本 ID，对齐 assertCanCancel）；`TestErpAstCapitalization#testReverseApproveRejectedAfterDepreciationExecuted` 在位全绿 |
| P1-CK-ast2-005（无红冲反写监听 + sweep 异步红冲死锁） | F2.9 修复体在位：`ErpAstDepreciationReversalListener`（implements `IErpFinVoucherReversedListener`，bean 注册 app-service.beans.xml L245-246，镜像 MfgSubcontractReversalListener 范式）+ 域内 `reverseDepreciation` L50-62 REVERSED 标记跳过自身回退防双应用 + ExecuteDepreciation L85-94 同防。合规面：I*Biz 跨实体读零新增 daoFor + `@Inject` 包级可见。**残留缺口**：监听器静默跳过 `#CATCHUP` + 逐期红冲键失配 → 新立 P2-CK-ast2-024-r3（见 §2.3）；监听器零测试引用 → 新立 P2-CK-ast2-026-r3 |
| P1-CK-ast2-006（VA 三方失同步/处置净值口径） | 不变量部分在位：VA `applyAssetValueChange` L253 REVALUATION_UP 同步上调 originalValue + `rollbackAssetValue` L289 对称下调；`DisposalAcctDocProvider` L75-82 处置净值读 `BILL_DATA_NET_BOOK_VALUE`（含 VA 联动）按 raw key 存在性回退 original−accum（注释引用本 ID）；1604 减值准备处置结转腿 Deferred 至独立计划（保护区域）维持原裁决不重开 |
| P2-CK-ast2-011（全实体裸 CrudBizModel 无状态守卫） | F1.3 统一基类接入在位：`ErpAstDepreciationScheduleBizModel`/`ErpAstAssetBizModel` 均 extends `AbstractErpCrudBizModel`（L30/L54 实证）——通用 CRUD update/delete 守卫生效（基类行为归 U20/M1.15，调用点合规）；residual 注记：executeDepreciation 域内 mutation 路径 REVERSED 行重绑（L88-90 status==EXECUTED 前置跳过回退块后重算重绑 EXECUTED）为数值自洽的自愈语义（elapsed 不计 REVERSED 行），r1 契约元数据失配子项随 F1.3 裁决闭合不重开 |
| P3-CK-ast2-021（折旧凭证 REQUIRES_NEW 先于主事务末步提交） | (c) 收敛裁决维持：billHeadCode=`资产码#期间` 稳定（Dispatcher L216-218）+ F1.1 引擎幂等返回既有 id → 重试 posted 收敛（引擎层传导，ast 无该链代码变更；本复核 `tryPost` L141→reload/update L142-148 结构无异态，随索引 fixed 终态维持） |

### 2.2 归并（同型 open 追加证据至原 ID）— 15 条 + 跨切片扩员 2 条

| 原 ID | r3 复核证据（T0，原 ID 状态不动仍 open） |
| --- | --- |
| P2-CK-ast2-007（elapsed 口径） | 原样：ExecuteDepreciation L96 `countExecuted(assetId) - (wasExecuted?1:0)` + CatchUp L98 同型 + facade `countExecuted` L126-131 无 period 比较——DECLINING 补提/重执行错位面未变 |
| P2-CK-ast2-008（批量单事务逐资产吞异常） | 原样：Batch L47-53 catch 仅 `LOG.warn`（英文消息）无告警派发、无逐资产独立事务；期末结账双入口并发现实未变（UK 守卫 L132-139 自证预期并发） |
| P2-CK-ast2-009（维修过账失败重试封死无告警） | 原样：MaintenancePostProcessor L82 无条件 `setStatus(postTargetStatus())` + L84 voucherId 门控三元组；CAPITALIZE 资产变更先于 tryPost 顺序未变；Expense dispatcher 无 dispatchFailureAlert 未变 |
| P2-CK-ast2-010（减值/重估折旧基数死代码） | 原样：`applyAssetValueChange` 死代码块（`newDepreciableBase` 局部算后无消费）+ `shouldAdjustDepreciationBase` config 门控空转（本切片维度①在案注记）；注意与 006 修复的 floor/NBV 下限逻辑并存互不替代（M1.8 ast-016 同款注记） |
| P2-CK-ast2-012（批量折旧 N+1） | 原样：每资产 ≥4 查询 + `countExecuted` `findAllByQuery().size()` 全实体加载（facade L126-131）+ 批量期全资产 findAllByQuery（Batch L43）；对照 `findLastExecutedPeriod` L56-64 `setLimit(1)` 正确示范仍在 |
| P2-CK-ast2-013（维修独立贷方科目死分支） | 原样：Provider L62 `inventorySubject` 读取后零引用 + Dispatcher L84-87 四科目硬编码（6602/2502/1002/1403）不读类别配置；成本类型拆分数据面未建 |
| P2-CK-ast2-014（资产 CRUD 无折旧配置校验） | 原样：`ERR_DEPRECIATION_USEFUL_LIFE_INVALID` 全 service 零消费（grep 实证）；Calculator L33-36 静默兜底（残值≥原值→0、年限≤0→1）未变；资本化路径校验与 CRUD 直建路径不对称未变（F1.3 基类守卫为状态锁面，非数值校验面） |
| P3-CK-ast2-015（死配置常量+cron 键漂移+owner doc 类名不存在） | 原样：`ErpAstConstants` L20/22/24 三常量零消费 + 全仓 `ErpAstDepreciationJob` 零命中 + 实际键 `nop.job.erp-ast-depreciation.enabled|false`（默认 false 双层门控）+ cron 默认非空 `0 0 2 1 * ?`；**扩员**：owner doc §5.1 L218 定时作业登记行（声称 ErpAstDepreciationJob 三件套+键默认空）与 §十 L349「nop-job 定时自动折旧 Deferred（触发条件 nop-job 接线）」互斥且双双与 job yaml 实态漂移——owner doc 双站点归并本 ID |
| P3-CK-ast2-016（YearMonth.now() 直读时钟） | **部分已修**：Java 侧 Recalc L68-72 已改 `CoreMetrics.today()`（注释引 bug 2026-09-01-0058 冻结时钟联动）——索引 open 态 = 回填缺口（随 fin4-013 先例注记，修复有效）；XML 侧 `depreciation.batch.xml` L14 `${java.time.YearMonth.now().toString()}` 仍在（R7 checker 盲区变体未消）——原 ID 维持 open，追加「Java 站点已修/XML 站点残留」证据 |
| P3-CK-ast2-017（折旧多币种面零填充） | 原样：Dispatcher `buildEvent` L197 / `buildCatchUpEvent` L151 `setExchangeRate(BigDecimal.ONE)`；schedule 四列（currencyId/exchangeRate/amountSource/amountFunctional）写入代码零处；对照 MaintenanceExpense 链透传范式（InventoryPostingDispatcher L73 亦正确透传）同域不对称未变 |
| P3-CK-ast2-018（非直线法 plannedAmount 恒 0） | 原样：Cap `plannedAmount` L295 SL-only 分支非 SL 返 0 + Recalc L65 固定直线均摊不读 method |
| P3-CK-ast2-019（残值配置面缺失） | 原样：Cap L241 `setResidualValue(ZERO)` 硬编码 + Cap 实体/Category 无残值输入列（M1.8 P3-CK-ast-017 同款并案在案） |
| P3-CK-ast2-020（currentValue 语义漂移+depreciationRate 死列） | 原样：8 writer 多语义（Cap L240=原值/VA L259/295=净值/Maintenance L96/L114=±增量/Inventory L248=评估值/Split L391=行成本/Merge L386=合计原值）+ 折旧三链零维护 + `getDepreciationRate` 零消费 |
| P3-CK-ast2-022（补提凭证失败重跑静默 no-op） | 原样：CatchUp L148-150 `created.isEmpty()` 早退不再补发凭证 + `postCatchUpVoucherOnly` L185-187 total=0 返 null 行留 EXECUTED+posted=false；关联新立 ast2-024-r3（GL 侧红冲后的同域漂移面）注记 |
| P3-CK-ast2-023（报表/看板全表加载无 orgId） | 原样：Report L293/309/329/343 `findAllByQuery` 无 orgId 无分页；dashboard 同型（M1.8 P2-CK-ast-009 并案在案） |
| （跨切片）P3-CK-ast-022 | 扩员：折旧 facade `ErpAstDepreciationScheduleProcessor.currentUserId` L139-146 宽 catch 返 null 无日志——`currentUserId` 全域族新增站点（M1.8 六站点清单外；与 Split/Merge/VA/Cap/Disposal/Inventory 同型收口时一并处理） |
| （跨切片）P3-CK-ast-026 | 扩员：`ErpAstDepreciationReversalListener.findAssetByCode` L99-105 无 orgId 过滤（`setLimit(1)` 在位）——UK (code, orgId) 跨组织命中不确定面同族新站点（billHeadCode 解析侧） |

### 2.3 新立 `-r3` — 3 条

**P2-CK-ast2-024-r3**（DIM-B 维度⑧/⑥；F2.9 闭环残留，新控制点）

- **控制点**：`ErpAstDepreciationReversalListener.rollbackDepreciationSchedule` L65-68——`billHeadCode.endsWith("#CATCHUP")` 静默 return（javadoc 声明「汇总凭证无单一计划行可回退」）；对照 GL 实态：CATCHUP 汇总凭证 billHeadCode = `资产码#当期#CATCHUP`（Dispatcher L169-171），businessType 仍为 DEPRECIATION（L146）→ 引擎红冲派发 `VoucherReversedEvent` 到达本监听者后被后缀检查跳过。
- **证据**：时序推演（全部控制点 T0 实读）——财务员对已过账 CATCHUP 汇总凭证执行红冲（合法运营操作，凭证为普通 GL 凭证）→ GL 红字凭证生效 → 事件派发被 listener L65-68 跳过 → 多期计划行滞 `posted=true/voucherId=CATCHUP 凭证`、资产累计折旧/净值不回退、无告警；自愈通道双断：①域内重跑 catchUpDepreciation——全部 EXECUTED 幂等跳过（CatchUp L105-107）返回空 no-op（即 r1 ast2-022 同面）；②逐期 `reverseDepreciation(assetId, 漏提期)`——计划行 posted=true → `postingDispatcher.reverse(资产码#漏提期)`（ReverseDepreciation L48-49）→ GL 无该 billHeadCode 凭证（实存键为 资产码#当期#CATCHUP）→ 引擎 `ERR_REVERSE_SOURCE_NOT_FOUND` 硬失败。GL↔资产静默漂移且无可达自愈路径。
- **问题**：F2.9（ast2-005）闭环修复对汇总凭证形态不闭合——监听跳过 + 红冲键失配使 CATCHUP 凭证的引擎侧红冲成为「ast2-005 死锁形态」在汇总凭证子集的残留面。
- **定级 P2 理由**：真实运营操作触发、GL↔资产用户可见漂移无告警（ast2-005 同缺陷族曾定 P1）；限于 CATCHUP 凭果子集（单资产单次补提一张），触发面窄于 005 原发面故不升 P1。
- **建议修复方向**：listener 识别 `#CATCHUP` 后缀改走聚合回退——按 `voucherId=事件凭证` 反查全部计划行（`backfillCatchUpSchedules` 的逆操作）逐行置 REVERSED/posted=false 并回退资产累计；或 catchUp 落行时以 `#CATCHUP#漏提期` 粒度登记可逆锚点。同步补 026-r3 的监听器回归测试。

**P3-CK-ast2-025-r3**（DIM-B 维度⑮漂移簇；owner doc 漂移）

- **控制点**：`docs/design/assets/depreciation-and-posting.md` 三站点：①§1.3 L67 工作量法行仍声明公式+「需维护累计工作量」，未随 F2.9 UNITS 显式失败收窄（代码 ExecuteDepreciation L68/CatchUp L89 注释与索引修复注记均声称「owner doc §十 登记 Deferred」，§十 实文 L345-352 无 UNITS 行——引用失配）；②§5.1 L225「折旧终止月：处置/报废的当月停止计提折旧（当月减少当月停）」vs UC-AST-05 L94+代码 `catchUpDepreciationToDisposalPeriod` 补提至出售期含当期——r1 已注记「归 C4.4 裁决后修订 owner doc」，ast-1 r1/M1.8 收官后文本仍未修订；③§7.2 L293 G4 注未提及 `ErpAstDepreciationReversalListener` 引擎侧红冲回退通道（不完整型，自愈路径描述已部分过时）。
- **证据**：两 doc 全文走查（维度⑮扩样后全量）；三站点逐一对照代码（行号见上）；§5.1/§十 互斥站点归并 P3-CK-ast2-015 不重复立项。
- **问题**：owner doc 未随 F2.9 修复与 r1 裁决同步——文档消费方按 §1.3 会误判 UNITS 可用、按 §5.1 L225 与 UC-AST-05 得到矛盾处置口径、按 §7.2 漏判引擎侧自愈通道。
- **定级 P3 理由**：纯文档漂移零运行时影响；UNITS 实际行为（显式失败）安全侧。
- **建议修复方向**：§1.3 补 UNITS 显式失败行 + §十 补 Deferred 登记（消除代码注释引用失配）；§5.1 L225 按 r1 裁决修订为「处置当月补提含当期」；§7.2 补 ReversalListener 通道说明。

**P2-CK-ast2-026-r3**（DIM-T 覆盖缺口）

- **控制点**：`ErpAstDepreciationReversalListener`（F2.9 修复体）全仓零测试引用——ast service 测试 grep `ReversalListener|onVoucherReversed` 零命中；orchestration E2E 反向冲销 spec 仅 p2p/o2c/mfg 三域（`o2c-reverse`/`p2p-reverse`/`mfg-subcontract-chain`），无 assets 引擎侧红冲→域回退场景。
- **证据**：对照 F2.9 同批修复 001~004 均带命名红→绿回归测试（索引修复注记逐一在案），005 的修复注记未点名测试；域内发起红冲已由 `TestErpAstPostingReverse.testDepreciationReverseRollsBackAssetCard` 覆盖，缺口限「GL 侧红冲 → 引擎派发 → 监听回退」跨域通道——恰为 ast2-005 原发缺陷路径。
- **问题**：testing-strategy 覆盖要求表「每跨域场景 ≥1 测试」缺口 + P1 级修复体无回归钉（后续重构/引擎演进可静默破坏闭环）。
- **定级 P2 理由**：冻结清单「缺口 = finding（P2 起，按缺口业务关键度定级）」；行为属 P1 级红冲闭环家族故接近 P1 上限。
- **建议修复方向**：ast service 增引擎派发通道集成测试（voucherBiz.reverse → 断言计划行 REVERSED/posted=false + 资产累计回退 + CATCHUP 分支行为按 024-r3 修复后语义断言）；随 M2.x assets P1 修复批执行。

### 2.4 归属标注（§3.2 共享代码边界）

- posting 引擎内部（`ErpFinPostingProcessor` 幂等 null 语义/`ErpFinDeferredPostingRetryHelper` sweep/`resolveSubjects`/`ErpFinReversalListenerRegistry` dispatch 隔离）**归属 fin-1**（M1.1 已收官，P1-CK-fin-003/005 承接链维持）；本切片仅审消费侧（6 dispatcher tryPost null 门控/tryPostCatchUp/reverse 调用序）。
- common 抽象族（`AbstractErpCrudBizModel` 状态锁基类行为、`UniqueConstraintHelper`）调用点已审合规；基类行为缺陷归 U20（M1.15）。
- 聚合横切面（action-auth 聚合器/seed 全量装载/flux 导出门禁本体）归 U21（M1.16）；本格仅核 ast 注册在位性。
- 资产卡片/盘点单生命周期状态机与变动处置面归 ast-1（M1.8 已审）：本切片盘点面仅审「差异→凭证」联动（AssetInventoryPostingDispatcher/Provider + InventoryPost 过账编排）；生命周期缺陷命中即标注归并不立项（本切片未发现需归并的生命周期面新缺陷）。
- MAINTENANCE_ISSUE(492)/MAINTENANCE_LABOR(493) 业务类型与维护域联动面归 mnt 切片（M1.15/U10），本切片不深查。

## 3. 统计

| 级别 | 本轮新立 | 复用（fixed 复核有效） | 归并（open 追加证据） |
| --- | --- | --- | --- |
| P0 | 0 | 0 | 0 |
| P1 | 0 | 6（ast2-001..006） | 0 |
| P2 | 2（ast2-024-r3 DIM-B、ast2-026-r3 DIM-T） | 1（ast2-011） | 7（ast2-007/008/009/010/012/013/014） |
| P3 | 1（ast2-025-r3 DIM-B⑮） | 1（ast2-021） | 8（ast2-015/016/017/018/019/020/022/023）+ 跨切片扩员 2（P3-CK-ast-022/026） |
| **合计** | **3** | **8** | **15 + 2 跨切片** |

五格 verdict：DIM-B **finding**（P2-CK-ast2-024-r3 + P3-CK-ast2-025-r3）/ DIM-F **pass** / DIM-S **pass** / DIM-T **finding**（P2-CK-ast2-026-r3）/ DIM-I **pass**。历史 23 条 r1 ast2 ID + 28 条 r1 ast ID 状态零覆写（8 fixed 复核有效 + 15 open 追加证据 + 2 跨切片扩员；ast2-016 索引 open = Java 站点已修的回填缺口注记，随 fin4-013 先例）。

## 4. 剩余风险声明（横切关注点 13 完整四件套）

- **已查**：ast-2 切片核心链全读（ExecuteDepreciation 152 行/ExecuteBatch 56 行/CatchUp 233 行/ReverseDepreciation 70 行/Recalc 98 行/DepreciationCalculator 80 行/facade 151 行/ReversalListener 119 行/DepreciationPostingDispatcher 239 行/AssetPostingExecutor 43 行/AssetInventoryPostingDispatcher 117 行/AssetInventoryAcctDocProvider 106 行/ScheduleBizModel 90 行/InventoryPostProcessor 60 行）+ 过账族其余成员与 open-finding 控制点逐一 grep+片段实读（Disposal/VA/Maintenance×2/Capitalization/Split/Merge dispatcher+provider 族、Cap/VA/Maintenance/Inventory/Disposal Processor、ErpAstConstants/Errors）+ 机械程式全套实跑（checker 19 规则/反模式族/codegen 安全 35 站点/聚合完整性 E1 路径/validate:flux 双数字/seed 门禁/ast 回归 339/strict+self-test）+ `depreciation.batch.xml`/`erp-ast-depreciation.job.yaml` 接线核对 + owner docs 2 份 × 9 断言抽样（扩样后全文走查）+ r1 ast2 23 条逐一比对裁决 + M1.8 ast 28 条跨切片指针核对 + r2 簇指针核对 + E2E ast spec 族纪律核对 + 盘点→凭证闭环时序推演。
- **未深查（边界归属）**：posting 引擎内部（幂等/sweep/科目回填——归属 fin-1）；Maintenance 资本化 Provider 科目分解逐行（与 013 同范式推断，r1 同口径，修复阶段顺带核）；`erp-ast-web` 全量 view.yaml 契约 drift（全局面归 U21/M1.16）；报表 xpt 模板渲染细则（报表子系统既有验收，仅 REST 接线核对）；ErpAstAssetAuditRecorder 审计面细节（M1.8 已审 ExtFieldsAndAuditTrail 链）；orchestration E2E spec 内部断言质量（仅归属性核对）。
- **残留风险（登记不裁决）**：① 15 条归并 open finding 的修复归 M2.x（assets P1 修复批及其后），其中 P2-CK-ast2-008（双入口并发 session 连锁/孤儿凭证群）与 P2-CK-ast2-009（维修过账死锁无告警）在期末结账活跃场景影响面最大，建议 M2.x 优先级排序参考；② ast2-024-r3（CATCHUP 红冲缺口）与 ast2-026-r3（监听器零测试）应同批修复同批补测；③ ast2-025-r3 的 owner doc 修订与 ast2-015 扩员站点（§5.1/§十 互斥）建议同一次 doc 修订批收敛；④ validate:flux 325 条 variant 漂移与 compliance 机器基线块差距均为批前在案外部事项（successor：`ai-check-r3-compliance-baseline-raise` / nop-chaos-flux dist 基线裁决），非本切片范围。
- **successor 触发条件**：M1.17 收官以覆盖矩阵完整性校验本报告 5/5 格；M2.x 修复以「先写失败测试」承接 §2.2/§2.3 open 项（024/025/026-r3 在列）；U06 五格至此全部被 M1.8（ast-1）+ 本切片（ast-2）覆盖完毕。

---

## 收官机械核证（Phase 7 Proof，T0 后零生产代码改动）

- `git status --porcelain` 过滤生产路径（`module-*`/`app-erp-all` 排除 docs）expect 空——实跑：唯一脏面 = 本计划文件 + 本报告 + 双索引（均 docs 路径），生产路径零改动 ✅
- `mvn test -pl module-assets/erp-ast-service` 复跑全绿——339/0/0/0 ✅（Phase 7 收尾回归，见计划注记）
