# ck-assets-lifecycle-r3 — assets「资产生命周期」ast-1 五维符合性审计报告（ai-check-r3 M1.8）

> 工作项：M1.8（U06 × 五维 × ast-1，冻结清单 §4 映射表第 8 行 / §3.3 U06 行）。
> 执行日期：2026-09-08。审计时点 T0：HEAD `69851bb2dcd72d9183bf66d57e35a8ee3377f211`（基线快照 `40512c567` 已被 docs-only 提交推进——姊妹 M1.5 计划落盘提交 `69851bb2` 即 T0，计划「实跑时 HEAD + 脏面披露」纪律覆盖）；脏面 = **空**（`git status --porcelain` 零输出；姊妹 plan `2026-09-07-2200-1` StateMachine 直抛领域码批次已随更早提交在 T0 代码态内，全仓 3991/0/0/1 full-green 随其落账）。
> 判定依据（冻结）：`docs/audits/check/2026-09-06-1645-ai-check-r3/m0-5-audit-checklists.md` §1/§2（判定标准/机械核查程式/跨轮查重）+ §3.3 U06 行 + §6 勘误 E1。执行期仅在本报告与计划勾选注记追加证据，判定标准零改动。
> 范围：资产建档/变动/盘点——资产卡片生命周期状态机、Cap/CIP 资本化、Split/Merge、ValueAdjustment、Disposal、Inventory 盘点单生命周期与变动处置（`module-assets/erp-ast-{dao,service,web}` src/main）；owner docs `state-machine.md`（424 行）+ `cip.md` + `split-merge.md` + `use-cases.md`。Skills：`nop-platform-conformance-audit-prompt` + `code-quality-audit-prompt`（DIM-B）；其余维 Skill: none。
> 切片边界：折旧批量/资产过账/盘点→凭证闭环归 ast-2（M1.9，`depreciation-and-posting.md`）；posting 引擎内部归 fin-1（M1.1 已收官）；common 抽象族基类行为归 U20（M1.15），本切片只审调用点。折旧/过账面命中均按归属标注归并，不立项。
> 零生产代码改动（roadmap 规则 6）：本报告 + 双索引 + 计划勾选注记为唯一产物（收官 `git status` 机械核证见 §4）。

## 1. 五维覆盖矩阵（5/5 完整）

| 维度 | 判定面（ast-1 增量） | 机械核查结果 | verdict |
| --- | --- | --- | --- |
| **DIM-B 后端平台合规** | §1.1 全套机械程式 + 15 维度走查（重点②③⑧⑨）+ 维度⑮断言抽样 | 反模式族全零（`extends RuntimeException`=0 / `@Inject private`=0 / `System.currentTimeMillis`=0）；`@Transactional` 全域仅 AssetPostingExecutor javadoc 引注（Facade REQUIRES_NEW 事务边界描述），真实共存=0；checker 19 规则逐项 = M0.3 快照行零漂移（R1d=14/R2a=34/R2b=242/R2c=1542/R2d=38/R3=5/R6=2/R10=14/R12a=71/R12b=66/R12c=42）；`__XGEN_FORCE_OVERRIDE__` 35 处全为 codegen dict.yaml 只读校验点零手改（`git status _gen/` 空）；聚合器（E1 勘误路径）含 `/erp/ast/auth/erp-ast.action-auth.xml` + `x:extends` ×2；跨实体走 I*Biz（`IErpAstAssetCapitalizationBiz`/`IErpAstDisposalBiz`/`IErpAstDepreciationScheduleBiz`/`IErpMntEquipmentBiz` @Nullable 裁剪部署/`IErpFinVoucherBiz` Facade），`IOrmTemplate` 仅 flushSession/runInSession；维度⑧ 状态机 Bean 化（Asset 11 命名边直抛领域码 + Inventory 6 动作 + Movement 退化轴死状态裁定 + 双轴 Approval/Document 族）+ dict 无未登记死状态；维度⑨ xbiz `<auth permissions>` 在位（approve/reverseApprove/mutation）+ 审批轴 5 动作 Bean 守卫；维度⑮ 3 doc × 7 断言（state-machine 3 + cip 2 + split-merge 2）——6/6 一致 + 1 处漂移（cip.md「预计净残值按类别比例计算」↔ `setResidualValue(ZERO)` + Category 无 residualRate = 既有 open P3-CK-ast-017，未触发 ≥2 扩样）；15/15 维度无跳维（⑫指针 DIM-T）。**新发现 1 条**：`ErpAstAssetBizModel.getAssetAuditTrail` 经 `daoProvider().daoFor(ErpAstAssetActionLog.class)` 直查他实体（`IErpAstAssetActionLogBiz` 在位未用、无豁免注释，L100）——新立 P3-CK-ast-028-r3（见 §2.3） | **finding**（1 新立 P3；8 复用 + 19 归并，见 §2） |
| **DIM-F 前端页面与 E2E** | §1.2 全套 + ast-1 面 = 资产台账/变动/盘点页 | `npm run validate:flux` step [1/3] `FLUX_PAGE_ERROR_COUNT: 0 pageCount=999 erpPages=855`；[3/4] `files=855 validated=855 errors=325`——325 条逐条同型（`variant="primary"`×dropdown-button 枚举漂移，非变体过滤零逃逸），ast 域 15 条同族成员，successor 在案（roadmap MI.8 done 行），非本切片 finding，整体 exit 1 余项即此既有外部漂移；`component="AMIS"` 保留层 0、ORM `ext:web-renderer="flux"` 缺失 0；资产台账 `ErpAstAsset.view.xml` 保留层 `x:extends="_gen/…"` + bounded-merge + 自定义列全量 `i18n-en:label`，`main.page.yaml` = GenPage 源头链（M0.4 row 35 ref 页禁改义务在位）；盘点页 `@query:ErpAstInventory__findPage`（REST `/r/` 约定）；dashboard 孪生双文件 `.flux.yaml`/`.page.yaml` 参数名一致（periodId，**无 mfg-023 型孪生漂移**）；4 手写页 i18nEn 承载在位（13/17/16/2 处，MI.8 修复态）；页面 graphql 命中 0；E2E：ast spec（ast-inventory-count/ast-value-adjustment）违规选择器 = 0，GraphQL 命中为 runbook L122 豁免 + L229 登记的 API 驱动型数据层通道（业务动作 spec 经 GraphQL 直驱 @BizMutation），`E2E_ENGINE` 缺省 flux（engine.ts L8-13） | **pass** |
| **DIM-S seed 数据** | §1.3 全套 + asset/depreciation_schedule 金额自洽（2210-1 批口径） | `TestErpSeedDataIntegrity` **4/0/0/0 全绿**（BUILD SUCCESS）；`git status --porcelain _init-data/` 空（零 seed 变更，双面快照重录义务未触发）；资产清点 **372 CSV + 1 SQL**（= 冻结口径「M1.5 批次后」登记值）；deploy `_seed_*.sql` 全仓仅 cs/notify 两族且均已在 seed-data.md §97 登记处 ✅ 已聚合（无第三态；ast 无 deploy seed 零同步义务）；自洽抽查：asset id=2（AST-2026-002 数控机床 original=120000/accum=6000/NBV=114000）↔ 最新 schedule（period 2026-07 EXECUTED accum=6000/NBV=114000）双字段全等（seed-data.md L193 约束）；id=1/3（accum=0 无 schedule 行）内部自洽 | **pass** |
| **DIM-T 单元测试** | §1.4 全套 + 建档/变动/盘点关键业务流行 | `mvn test -pl module-assets/erp-ast-service` **339/0/0/0 全绿**（锚点 = `cjk-baseline.md` §批注账 MI.6 批1 行 `erp-ast 337` 全绿——零失败零回归；339 = 337 + 姊妹 StateMachine 直抛码计划矩阵测试增量，计划基线已预告）；覆盖对账：Asset BizModel 3 动作（suspend/resume/getAssetAuditTrail）↔ IdleStateMachine + ExtFieldsAndAuditTrail；Cap 5 审批动作（xbiz 委托）↔ TestErpAstCapitalization；Cip 7（2 query + 5 D-mutation）↔ CipTransfer；Split/Merge 各 6（cancel + 5 xbiz）↔ SplitMerge（含 P1-001/002/006 修复回归行 testProportionalSplitConservesAccumDepAndResidual/testMergeConservesResidualAndRemainingLife + reverse-not-supported 负路径）；VA 6 ↔ ValueAdjustment（含 005 上限拒绝 + F1.2 配对用例）；Disposal 5 ↔ Disposal×3 族（含 EquipmentLinkage 双零回链）；Inventory 8 动作 ↔ Inventory（含 P1-003 回归行 testReconcileRejectsMissingActualQuantity）+ E2E ast-inventory-count；Movement 审批轴 ↔ MovementReverseApprove；折旧→过账链行（Depreciation/CatchUp/PostingReverse 族在位全绿）**归 ast-2 切片逐行核对**（本切片边界）；`SnapshotTest.RECORDING` = 0 残留；`delVersion` 命中仅 javadoc 注解、`*` 通配零滥用（339 全绿 = CHECKING 态等价证明） | **pass** |
| **DIM-I i18n（MI 先行时序口径）** | §1.5 全套（仅零回归 + 白名单合规，不产同类 finding） | `--strict` **PASS exit 0**（0 new violations vs 冻结 SNAPSHOT，170 baseline files 单向收紧成立）；`--self-test` **PASS**；ast 探针族（CAT-1 39 / CAT-2 29±）维持清零零回归；WHITELIST ast 条目抽样 4（≥3，批 1/2 assets 段全抽）：4/4 四要素齐备（文件路径/理由/owner doc 指针/裁决来源 plan 2026-09-07-0902-1），实仓复核 `IErpAstAssetBiz`@Description / Dashboard@Description L56 / AssetBizModel@Description L93（混合文件 5 行已改英文）/ SuspendResume `IDLE_SINCE_PREFIX="闲置自 "`（C2 契约 + TestErpAstIdleStateMachine 断言在位）登记准确零缺陷；`grep -L @Locale` *Errors.java = 空（ErpAstErrors @Locale("zh-CN") 在位）；`erp-*-meta` + `_vfs/i18n` 零手改 | **pass** |

## 2. Finding 三态裁决（§2 跨轮查重全维强制）

> 查重源：r1 `docs/audits/check/ai-check-index.md` §报告清单/§Finding 追踪（同域报告 `ck-assets-lifecycle.md` C4.4 全 27 条逐一比对）+ r2 只读目录 `2026-08-28-2049-ai-check-r2/`（F2.8/F2.9 簇指针，无新独立 finding）+ §Mission 基线快照（checker 各命中均为已裁决偏离）。**本轮新立 1 条**（`P3-CK-ast-028-r3`）；历史 ID 零覆写。姊妹 plan `2026-09-07-2200-1` 已改写 ast 错误码通道（SM 非法边直抛领域码 + 调用点同码补参），fixed 复核一律以 T0=HEAD `69851bb2` 行为为准（下列行号均为 T0 实测）。

### 2.1 复用（同型已 fixed，HEAD 复核有效）— 8 条

| 原 ID | 修复在位证据（T0 = HEAD `69851bb2`） |
| --- | --- |
| P1-CK-ast-001（Split PROPORTIONAL 累计折旧二次派生覆盖） | `computeAllocation` 二次派生循环已用 `if (fixedMode)` 包裹（ErpAstSplitProcessor L312-323，注释显式引用本 ID）——PROPORTIONAL 保留第一遍 + `applyRemainderForAccumDep` 补差（L288-311）；回归测试 `TestErpAstSplitMerge#testProportionalSplitConservesAccumDepAndResidual` 在位全绿 |
| P1-CK-ast-002（Split 目标卡残值全额复制） | `allocatedResidual` 新增（L334-349：PROPORTIONAL 行比例 / FIXED_AMOUNT 金额比例）+ `createTargetAssets` L394 接线（注释引用本 ID）；同回归测试断言 Σ 目标残值=源残值 |
| P1-CK-ast-003（盘点漏录按实盘 0 判盘亏） | `calculateVariance` 前置完整性校验（ErpAstInventoryProcessor L165-172：`actualQuantity==null` 行抛 `ERR_AST_INVENTORY_ACTUAL_QUANTITY_MISSING`，注释引用本 ID）；`TestErpAstInventory#testReconcileRejectsMissingActualQuantity` 在位全绿 |
| P1-CK-ast-004（VA NBV 联动悬挂窗口永不执行） | F1.2 修复在位：`applyAssetValueChange` 无条件前移 tryPost 之前（executeApprove L92 + doAutoApprove L335，注释显式引用）+ `executeReverseApprove` 无条件 `rollbackAssetValue` 配对（L118-120）——凭证成败不再决定 NBV 联动，sweep 重试场景业务面已就位 |
| P1-CK-ast-005（VA 减值无上限 → 负净值） | `validateForApproval` 上限校验（L205-221：非 REVALUATION_UP 时 `amount ≤ NBV−残值`，超限抛 `ERR_ADJUSTMENT_AMOUNT_EXCEEDS_NBV`）+ `applyAssetValueChange` 下调分支 `max(残值,0)` 下限（L256，注释引用本 ID）；`TestErpAstValueAdjustment` 超限拒绝用例在位 |
| P1-CK-ast-006（Split/Merge 新卡折旧口径三重错位） | Merge 侧：目标卡残值 = Σ 源残值（L96→L388）+ `resolveUsefulLifeMonths` 按剩余期间加权（L290-309，注释引用本 ID + owner doc 字面）+ `resolveElapsedMonths`（L336-356）+ 计划基数 = 剩余净值 − 残值（L416）+ 起点 = 继承点次月（L423）；Split 侧：计划基数减已提（L441）+ 起点 = 继承点次月（L450）；`TestErpAstSplitMerge#testMergeConservesResidualAndRemainingLife` 在位全绿 |
| P2-CK-ast-007（通用 CRUD 无状态守卫 + delete 传导 reverseApprove 断链） | F1.3 统一基类接入在位：ast 全实体 BizModel extends `AbstractErpCrudBizModel`（基类 `defaultPrepareUpdate/Delete` 状态锁守卫——posted=true 或 APPROVED 拒绝，config 默认 ON，无列实体惰性，module-common-service L12-49 实证；调用点合规，基类行为缺陷归 U20/M1.15）——非 DRAFT 资产 delete/update 入口被封，r1 的「delete→reverseApprove 静默跳过」传导链入口已闭合（Cap `executeReverseApprove` 的 `if (asset != null)` 防御分支仍在，现为纵深防御非唯一屏障） |
| P2-CK-ast-015（REQUIRES_NEW 凭证先于主事务末步提交） | F1.2 修复在位：VA 链 applyAssetValueChange 前移 + reverse 配对（见 004 行）；Disposal 双凭证尾部连续——catchUp 数据段先行供数 gainLoss（L109）+ `#CATCHUP` 凭证段后置与 DISPOSAL 凭证相邻提交（L153-159）+ 计划行回填推至两凭证全提交后最终尾部（L161-163，注释引用 F1.2 跨连接 update-entity-not-found 实证）+ `TestErpAstDisposalEquipmentLinkage` 双零回链断言；Split/Merge/Inventory 链结构残余（tryPost 后 reload/updateEntity）按索引 F1.2 终态「四链 (c) 收敛」族裁决闭合，本复核无异态 |

### 2.2 归并（同型 open 追加证据至原 ID）— 19 条

| 原 ID | r3 复核证据（T0，原 ID 状态不动仍 open） |
| --- | --- |
| P2-CK-ast-008 | Cap `executeReverseApprove` 时序原样：`postingDispatcher.reverse(cap)`（L126，REQUIRES_NEW 红冲）仍先于 `assertCanReverseCapitalize`（L130）；Disposal 同构（L185 reverse 先于 L190 assert）。新增缓解注记：ast2-004 修复引入的「已执行折旧拒绝」守卫（L121-125）位于 reverse **之前**，部分收窄触发面（有折旧执行场景提前抛错），但「资产被处置后 reverseApprove 资本化单」场景（count=0）仍红冲先行——核心缺陷未闭合 |
| P2-CK-ast-009 | Dashboard 五查询零 orgId filter 原样（loadInServiceAssets L178-183 / sumPeriodDepreciation L185-195 / sumCipBalance L197-206 / loadExecutedSchedulesInRange L208-213 / loadAssetIdsWithExecutedDepreciationInPeriod L215-225） |
| P2-CK-ast-010 | 双侧原样：`postProcess` 不扣减 accumulatedCost（ErpAstCipProcessor L176-198 仅翻 CostItem flag + CIP 状态）+ `sumCipBalance` 按 `isCompleted=false` 过滤 Σ 全额（L197-206）——部分转固双计窗口在 |
| P2-CK-ast-011 | `ErpAstCipReverseTransferProcessor` L34-41 原样：`findCostItemsByCapitalization` 仅 `eq("capitalizationId")` 无 cipId/sourceCode 归属校验（facade L267-271），`size()` 比较为唯一部分红冲防线 |
| P2-CK-ast-012 | `ErpAstInventoryReverseProcessor.reverse` 原样：红冲凭证 + posted 三件套清空 + status 回 RECONCILING（L31-52），零资产侧回滚（盘盈新卡存续、盘亏 SCRAPPED 无逆动作——AssetStateMachine 11 边无 shortage 逆边实证）；正向 `handleShortageTriggerDisposal` 终态短路 L287-290 同 r1 |
| P2-CK-ast-013 | `validateSources` null continue 原样（ErpAstMergeProcessor L219-221）+ `executeApprove` L93-96 四连 `sum()` 对 null 源 NPE 面原样（静态 sum L529-535 方法引用解引用） |
| P2-CK-ast-014 | `validateSources` 无 sourceAssetId 去重原样（L207-243）——对照 Split `validateLines` codes 去重守卫（SplitProcessor L227-237）镜像不对称仍在 |
| P2-CK-ast-016 | `applyAssetValueChange` 折旧基数死代码原样（ErpAstValueAdjustmentProcessor L261-267：`newDepreciableBase` 局部变量算后无消费 + `shouldAdjustDepreciationBase` L303-311 纯谓词空转）——注意 005 修复的 floor 逻辑（L256）与死代码块并存但互不替代（该块本意是折旧基数联动，非 NBV 下限） |
| P3-CK-ast-017 | Cap 建卡 `setResidualValue(BigDecimal.ZERO)` 原样（L241）+ 盘盈建卡同型（InventoryProcessor L249）+ ORM 实证 `ErpAstAssetCategory` 无 residualRate 列（grep 零命中）——即维度⑮抽中的 cip.md 漂移断言本体 |
| P3-CK-ast-018 | `plannedAmount` 非直线法 return ZERO 原样（Cap L295-305，0 计划行照落）+ Split/Merge 非直线法直接 return 不生成（L419-421/L411-413）——三处行为不一致仍在 |
| P3-CK-ast-019 | Disposal `validateForApproval` 仅 assetId/disposalType 非空校验原样（L266-271），负 disposalAmount 无符号守卫（对照 CIP `validateAmountPositive` L229-235 同域不对称仍在） |
| P3-CK-ast-020 | `catchUpDepreciationToDisposalPeriod` 原样：disposalPeriod 解析有 try/catch（L323-328）而 `YearMonth.parse(lastExecuted)` 裸奔（L334）双分支不对称仍在 |
| P3-CK-ast-021 | 四 dispatcher（Split L53-60/Merge/VA L43-51/Inventory L43-50）catch 内仅 LOG.warn/error 原样，`dispatchFailureAlert` 仍仅 Cap（L66/L72-88）与 Disposal（L61/L67-83）两链在位——2/6 覆盖不对称未变 |
| P3-CK-ast-022 | `currentUserId` 宽 catch 返 null 无日志七站点原样（Split L537-544 / Merge L516-523 / VA L387-394 / Cap L384-391 / Disposal L407-414 / Inventory L415-422 + Dashboard 族外）——全域族（md-008 等）修复时同族收口 |
| P3-CK-ast-023 | 三处置入善后缺失原样：Split `disposeSourceAsset`（L470-474）/ Merge `disposeSourceAssets`（L445-452）DISPOSED+NBV=0 无 cancelPendingSchedules；盘亏 SCRAPPED 既不 cancel 也不清 NBV（InventoryProcessor L294-300）——对照 Disposal `cancelPendingSchedules`（L351-360）不对称仍在 |
| P3-CK-ast-024 | Dashboard 双缺陷原样：`loadExecutedSchedulesInRange` 签名收 from/to 查询无日期过滤全表载入（L208-213）+ `findDepreciationMissingAlert` 无 acquisitionDate 当月豁免（L143-162，资本化计划次月起 = 当期必然假预警） |
| P3-CK-ast-025 | suspend remark 追加式标记原样（SuspendResumeProcessor L48-49 `remark + "；" + idleMark`）+ resume 不清理（L57-71）——多轮循环 remark 无界增长在；对照：`IDLE_SINCE_PREFIX` 已按白名单 C2 契约登记（DIM-I 行），本条仅remark 堆叠维度 |
| P3-CK-ast-026 | Cap `findAssetByCode` 无 orgId 无 limit 原样（L326-331 `list.get(0)`）+ Cip 同族 `setLimit(1)` 亦无 orgId（L273-282）——UK (code, orgId) 跨组织命中不确定面未变 |
| P3-CK-ast-027 | VA `executeReverseApprove` 无资产状态守卫原样（L116-131 `rollbackAssetValue` 直呼，无 isTerminal/状态校验）——对照 approve 侧 `validateAssetAdjustable`（L224-238）仅在正向，终态卡片 NBV 回写污染面未变 |

### 2.3 新立 `-r3` — 1 条

**P3-CK-ast-028-r3**（DIM-B 维度②；同型注记：`P2-CK-fin-007`（fin 格，跨域子型「BizModel daoFor 直查他实体无豁免注释」同族——跨单元按 r1 惯例（ast-007/ast-009 同型另立先例）在本切片自立 ID，不跨格归并）

- **控制点**：`ErpAstAssetBizModel.getAssetAuditTrail`（L94-125）——跨实体（ErpAstAssetActionLog）读取经 `daoProvider().daoFor(ErpAstAssetActionLog.class)`（L100）+ 注入 `IOrmTemplate` runInSession（L73/L95），**未注入 `IErpAstAssetActionLogBiz`**（接口在位：`IErpAstAssetActionLogBiz` + 裸 `ErpAstAssetActionLogBizModel extends CrudBizModel` 实证），亦无「I*Biz 无法满足」的豁免注释。对照同模块标准范式：Cap `countExecutedDepreciation` 经 `scheduleBiz.findList`（I*Biz，注释显式声明「对齐跨实体访问纪律，不新增 daoFor 站点」）；Dashboard/Report 的 daoProvider 注入有 javadoc 豁免理由。冻结清单 §1.1 程式②「命中处须有注释理由」直接命中。
- **证据**：本切片 IDaoProvider/IOrmTemplate 机械扫描（§1 DIM-B 行）全部命中点逐一核对——per-mutation Processor 的 `daoProvider.daoFor(本模块实体)` 为编排层标准范式（r1 全切面同口径未立项）、`orm()` helper 仅 flushSession（对齐 M1.5 mfg-1 裁决口径）、Dashboard/Report 有豁免注释；唯一无理由跨实体 daoFor 站点 = AssetBizModel L100。r1 ast 27 条无此形态、r2 无同型独立登记。
- **问题**：维度②「BizModel 中跨实体访问通过 @Inject I*Biz」违反点；行为面只读、修复廉价（注入 `IErpAstAssetActionLogBiz.findList` 替换），即时风险低 → P3（对齐 fin-007 P2 中的跨域越权子型降档：本站点同模块、纯读、I*Biz 现成）。
- **建议修复方向**：注入 `IErpAstAssetActionLogBiz`，`getAssetAuditTrail` 改经 `findList(q, null, context)`（镜像 Cap `countExecutedDepreciation` 范式）；或补豁免注释说明 IOrmTemplate runInSession 必要理由（若确有 session 语义依赖）。

### 2.4 归属标注（§3.2 共享代码边界）

- posting dispatcher/Provider 族与 `AssetPostingExecutor` = **资产过账面，归 ast-2（M1.9）**——本格仅按 §3.2 消费侧原则核对调用点（approve 链 tryPost/reverse 调用序，即 P2-CK-ast-008/015 控制点）；dispatcher 内部（buildEvent 凭证行组装/科目解析 daoFor(ErpMdSubject) R2a 基线站）与折旧引擎（Depreciation*Processor/Calculator/batch.xml）不在本格立项。
- posting 引擎内部（ErpFinPostingProcessor/REQUIRES_NEW 语义/sweep）**归属 fin-1**（M1.1 已收官，P1-CK-fin-003/005 承接链维持）。
- common 抽象族（`AbstractErpCrudBizModel` 状态锁、`AbstractReverseApproveProcessor.illegalStatusException` 骨架通道——Split/Merge ReverseApprove 覆写直抛领域码为姊妹 plan 2026-09-07-2200-1 form-3 有意契约修正）调用点已审合规；基类行为缺陷归 U20（M1.15）。
- 聚合横切面（action-auth 聚合器/seed 全量装载/flux 导出门禁本体）归 U21（M1.16）；本格仅核 ast 注册在位性。
- Movement 退化轴 ACTIVE 死状态 + Split/Merge DISPOSED 内部重组终态均为 owner doc 显式裁定登记（state-machine.md §适用对象二/§适用对象四 §3，layer-2 四方对照），维持既有裁决不重开。

## 3. 统计

| 级别 | 本轮新立 | 复用（fixed 复核有效） | 归并（open 追加证据） |
| --- | --- | --- | --- |
| P0 | 0 | 0 | 0 |
| P1 | 0 | 6（ast-001..006） | 0 |
| P2 | 0 | 2（ast-007/015） | 8（ast-008..014、016） |
| P3 | 1（ast-028-r3，DIM-B②） | 0 | 11（ast-017..027） |
| **合计** | **1** | **8** | **19** |

五格 verdict：DIM-B **finding**（P3-CK-ast-028-r3，minor）/ DIM-F **pass** / DIM-S **pass** / DIM-T **pass** / DIM-I **pass**。历史 27 条 r1 ID 状态零覆写（8 fixed 复核有效 + 19 open 追加证据）。

## 4. 剩余风险声明（横切关注点 13 完整四件套）

- **已查**：ast-1 九链核心 Facade 全读（Split 570/Merge 546/Inventory 438/VA 409/Cap 406/Disposal 429/Cip 355 行 + SuspendResume/ReverseApprove per-mutation 全读）+ Asset/Inventory/Movement 状态机 Bean 全读 + Dashboard/AssetBizModel 全读 + 6 dispatcher tryPost/alert 调用点核对 + 机械程式全套实跑（checker 19 规则 / 反模式族 / codegen 安全 35 站点 / 聚合完整性 E1 路径 / validate:flux / seed 门禁 / ast 回归 339 / strict+self-test）；owner docs 3 份 × 7 断言抽样；r1 27 条逐一比对裁决；r2 簇指针核对。
- **未深查（边界归属）**：折旧引擎本体 5 Processor + DepreciationPostingDispatcher/Provider + depreciation.batch.xml（归 ast-2/M1.9——本切片仅核对 Split/Merge/Disposal 与其交点的修复回归行）；posting dispatcher 内部凭证行组装与 `daoFor(ErpMdSubject)` R2a 基线站（归 ast-2 + fin-1）；Maintenance 族 9 类（UC-AST-10，r1 同口径不在 ast-1/ast-2 切片清单，其 dispatcher 未深读）；`erp-ast-web` 全量 view.yaml 契约 drift（仅 ast-1 面页面走查，全局面归 U21/M1.16）；ErpAstReportBizModel 渲染细则（报表子系统既有验收，仅结构扫描）；测试代码仅作覆盖对账消费。
- **残留风险（登记不裁决）**：① 19 条归并 open finding 的修复归 M2.x（assets P1 修复批及其后），其中 P2-CK-ast-008（红冲先行于守卫，ast2-004 守卫仅部分收窄）与 P2-CK-ast-012（盘点 reverse 资产轴不可逆不对称）在处置冲销/盘点重处理活跃场景影响面最大，建议 M2.x 优先级排序参考；② P3-CK-ast-028-r3 与 P2-CK-fin-007 同族（BizModel daoFor 直查他实体），两站点修复时同族一并收口并考虑全仓同型扫描（跨单元扫描面归 U20/M1.15 裁决）；③ `validate:flux` 325 条 variant 漂移与 compliance 机器基线块差距均为批前在案外部事项（successor：`ai-check-r3-compliance-baseline-raise` / nop-chaos-flux dist 基线裁决），非本切片范围。
- **successor 触发条件**：M1.17 收官以覆盖矩阵完整性校验本报告 5/5 格；M2.x 修复以「先写失败测试」承接 §2.2/§2.3 open 项；ast-2 切片（M1.9）承接折旧→过账链行覆盖核对与本格标注的过账面归并指针。
