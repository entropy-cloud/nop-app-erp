# 2026-07-05-0540-3-assets-impairment-revaluation 资产减值/重估（VALUE_ADJUSTMENT BizModel + 业财过账 + 资产净值联动）

> Plan Status: completed
> Last Reviewed: 2026-07-05
> Source: deferred 项承接（`2026-07-02-1000-2-assets-depreciation-disposal-capitalization.md` Deferred「资产减值/重估（VALUE_ADJUSTMENT）」，触发条件「减值测试需求启动」）；`docs/design/assets/depreciation-and-posting.md` §4.1-4.2（减值/重估分录 + 减值测试流程，设计已就绪）；`docs/architecture/job-scheduling.md` §3.2 `erp-ast-impairment-test` 作业登记为 DESIGN
> Related: `2026-07-02-1000-2-assets-depreciation-disposal-capitalization.md`（资产折旧/处置/资本化基线，本计划为其减值/重估后继）；`2026-07-05-0540-1-finance-bad-debt-provision.md`（同批次，正交：不同域不同业务类型）；`2026-07-05-0540-2-finance-period-close-annual-close.md`（同批次，正交：年度结转不触资产减值）
> Mission: erp
> Work Item: assets 资产减值/重估（owner doc `assets/depreciation-and-posting.md` §4 Deferred → 落地）
> Audit: required

## Current Baseline

（实时核实于 2026-07-05）

- **`ErpAstValueAdjustment` 实体已完整建模但 BizModel 为空 CRUD 存根**：`module-assets/model/app-erp-assets.orm.xml:350-395` 实体字段齐全——`assetId`(mandatory)/`businessDate`/`currencyId`/`exchangeRate`/`adjustmentType`(dict `erp-ast/adjustment-type`)/`adjustmentAmount`/`reason`/`docStatus`(dict `erp-ast/doc-status`)/`approveStatus`(dict `erp-ast/approve-status`)/`posted`+`postedAt`+`postedBy`/`approvedBy`+`approvedAt`/`amountSource`+`amountFunctional` + to-one `asset` 关系。`ErpAstValueAdjustmentBizModel`（`module-assets/erp-ast-service/.../entity/`）为 `extends CrudBizModel` 空存根（仅 `setEntityName` 构造，设计 doc `:341` 自述「BizModel 仍为空 CRUD 存根」）——无状态机、无审批、无过账、无资产净值联动。
- **`erp-ast/doc-status` 字典三码值（DRAFT/ACTIVE/CANCELLED）**：`app-erp-assets.orm.xml:92-96` 现含 `DRAFT`（草稿）/`ACTIVE`（已生效）/`CANCELLED`（已作废）。1000-2 折旧/处置三轴范式经 `submit` 动作 DRAFT→ACTIVE（`ErpAstDisposalBizModel.submit` → `disposalProcessor.submit`），本计划沿用同范式（非 CONFIRMED）。
- **`erp-ast/adjustment-type` 字典现状为两码值（IMPAIRMENT + REVALUATION 单数）**：`app-erp-assets.orm.xml:84-87` 现含 `IMPAIRMENT`（减值）+ `REVALUATION`（重估，单数，未区分增/减方向）。重估增/减值需不同借贷科目（增→资本公积 OCI；减→资产减值损失 P&L），单数 `REVALUATION` 无法按方向选科目——本计划需拆分为 `REVALUATION_UP`/`REVALUATION_DOWN`（见 Task Route Decision）。
- **减值/重估分录设计已就绪**：`depreciation-and-posting.md` §一(`:21`)列 VALUE_ADJUSTMENT 业务类型分录；§4.1(`:186-188`)定义三种调整——减值（借资产减值损失/贷固定资产减值准备，减少账面净值，减值后折旧基数调整）/ 重估增值（借固定资产/贷资本公积-其他资本公积，增加账面净值，可调折旧基数）/ 重估减值（借资产减值损失/贷固定资产，减少账面净值）；§4.2(`:190-201`)减值测试流程（可收回金额 < 账面净值 → 计提减值准备 → 生成凭证）。
- **过账业务类型缺 VALUE_ADJUSTMENT 码值**：`erp-fin/business-type` 字典（`app-erp-finance.orm.xml:57-86`，30+ 码值）**无** `VALUE_ADJUSTMENT`（设计 doc `:344` 自述业务类型命名过时——实际 DISPOSAL 为单一 90，VALUE_ADJUSTMENT 未落地）。新增属保护区域契约扩展（参照 0831-2 同步范式）。
- **资产卡片净值/折旧基数可联动**：`ErpAstAsset` 卡片含原值/累计折旧/净值/残值字段（1000-2 落地折旧/处置已读写这些字段）；减值/重估需调整账面净值与（减值后/重估增值后）折旧基数，对齐既有折旧引擎（`ErpAstDepreciationScheduleBizModel` 范式）。
- **既有资产过账 Provider 范式可复用**：1000-2 已落地 DEPRECIATION(70)/CAPITALIZATION(80)/DISPOSAL(90) 过账（`AssetDepreciationPostingProvider`/`DisposalAcctDocProvider` 范式，按 disposalType 内部分支）；VALUE_ADJUSTMENT 经同范式 Provider 按 adjustmentType 分支处理科目分解。
- **既有测试为回归门控**：assets 折旧/处置/资本化套件（1000-2 Closure）+ finance 过账套件为本计划回归基线。

剩余差距：`ErpAstValueAdjustment` 三轴状态机（docStatus/approveStatus/posted）+ 减值/重估过账 + 资产净值/折旧基数联动 + VALUE_ADJUSTMENT 业务类型 全缺。

## Goals

- `ErpAstValueAdjustmentBizModel` 实现三轴状态机（docStatus DRAFT→ACTIVE/CANCELLED 经 `submit` 动作 + approveStatus 审批 + posted 过账），复用 1000-2 折旧/处置既有的三轴审批范式；非法迁移 ErrorCode。
- 实现三种调整的业财过账（VALUE_ADJUSTMENT 业务类型）：减值（IMPAIRMENT）/ 重估增值（REVALUATION_UP）/ 重估减值（REVALUATION_DOWN），按 `adjustmentType` 内部分支科目分解（经会计科目表配置），复用既有资产过账 Provider 范式。
- 资产卡片净值/折旧基数联动：过账后按调整类型更新 `ErpAstAsset` 账面净值（减值/重估减值减少、重估增值增加）；减值后/重估增值后折旧基数调整（对齐折旧引擎，config-gated 是否调整折旧基数）。
- 反向冲销：`reverse` 红字冲减 VALUE_ADJUSTMENT 凭证 + 回退资产净值（与 1000-2 处置红冲范式一致）。

## Non-Goals

- **库存物料转固（IErpInvStockMoveBiz 出库→资本化）**：归 1000-2 另一 Deferred，需跨域 inventory 出库，独立结果面。
- **自动减值测试（可收回金额计算 + 减值指示触发）**：`erp-ast-impairment-test` cron + 可收回金额（公允价值减处置费用/现值）计算引擎归 Deferred（触发条件：减值测试自动化需求 + 可收回金额数据源落地时）；本期手工录入减值金额。
- **资产重估模式切换（成本模式/重估模式 CAS 配置）**：本期支持单次调整分录；重估模式全局切换与后续重估反转（累计重估转入留存收益）归后继。
- **折旧追溯重算**：减值后折旧基数调整自调整日起向前，不回溯重算历史已提折旧。
- **投资性房地产/无形资产减值专门模型**：本期仅固定资产减值/重估。

## Task Route

- Type: `implementation-only change`（owner doc 已设计的减值/重估落地 + 既有实体 BizModel 补全）
- Owner Docs: `docs/design/assets/depreciation-and-posting.md`（§4.1-4.2 落地标记 + §7 业务类型 VALUE_ADJUSTMENT 落地补注）；`docs/design/finance/costing-methods.md`（不涉及，正交）
- Skill Selection Basis: BizModel 三轴状态机 + 业财过账业务类型扩展 + 跨实体读资产卡片 + 反向冲销 + 字典保护区域同步——加载 `nop-backend-dev`（实体服务、跨实体调用、过账 Provider 范式、ErrorCode）。

### Key Decisions

- **Decision: VALUE_ADJUSTMENT 业务类型与科目分支**
  - 选择：新增业务类型 `VALUE_ADJUSTMENT`（`ErpFinBusinessType` 枚举 + 字典同步，保护区域参照 0831-2 范式）；过账 Provider 按 `adjustmentType`（IMPAIRMENT/REVALUATION_UP/REVALUATION_DOWN）内部分支科目分解（参照 1000-2 `DisposalAcctDocProvider` 按 disposalType 分支范式，不拆三个业务类型常量）。
  - 替代方案：拆三个业务类型（IMPAIRMENT/REVALUATION_UP/REVALUATION_DOWN）——拒绝，参照 1000-2 处置不拆 DISPOSAL_SCRAP/DISPOSAL_SALE 的既有裁决（科目分解内部分支，业务类型保持聚合）。
- **Decision: 折旧基数调整**
  - 选择：减值后折旧基数 = 调整后账面净值 − 残值（剩余折旧年限重算）；重估增值后折旧基数可调整（config-gated `erp-ast.revaluation-adjust-depreciation-base`，默认 true）；自 `businessDate` 起后续折旧按新基数，不回溯重算历史。
  - 替代方案：折旧基数恒定——拒绝，减值后净值已降，按原基数折旧会致折旧超额/净值转负。
  - 残留风险：剩余折旧年限=0 时（已提足）调整仅改净值不影响折旧——以折旧引擎既有"剩余可提足"校验缓解。
- **Decision: 重估增值的权益科目（资本公积 vs 留存收益）**
  - 选择：重估增值贷记 `资本公积-其他资本公积`（CAS 允许的重估模式）；config-gated 科目经会计科目表配置。
  - 替代方案：贷记留存收益——拒绝，资本公积是 CAS 重估模式的标准处理。
- **Decision: `erp-ast/adjustment-type` 字典 REVALUATION 拆分**
  - 选择：将既有单数 `REVALUATION` 拆分为 `REVALUATION_UP`（重估增值）/ `REVALUATION_DOWN`（重估减值），保留 `IMPAIRMENT` 不变。因本仓处于 bootstrap 阶段、`ErpAstValueAdjustment` 表空起步（无生产数据引用旧 `REVALUATION` 码值），拆分为安全重命名（旧 `REVALUATION` 移除，不留守儿码值），不涉及数据迁移。
  - 替代方案：(a) 保留单数 `REVALUATION` + 新增 `revaluationDirection` 字段区分增/减——拒绝，引入冗余字段且 `adjustmentAmount` 符号无法承载科目方向（增/减借贷科目不同）；(b) 保留 `REVALUATION` 守儿 + 新增 UP/DOWN——拒绝，守儿死码值污染字典。
  - 残留风险：若已有外部系统/导出引用旧 `REVALUATION` 码值需对齐——以 bootstrap 空表起步 + owner doc 补注缓解。

## Infrastructure And Config Prereqs

- 新增 config 项：`erp-ast.revaluation-adjust-depreciation-base`（默认 true）、`erp-ast.value-adjustment-require-approval`（默认 true，减值/重估强制审批）。
- 依赖既有会计科目表（资产减值损失/固定资产减值准备/资本公积科目）；依赖既有折旧引擎；无新基础设施。
- 无数据迁移（既有实体 + 加性业务类型）；回滚策略：移除 BizModel 自定义动作 + 还原字典 VALUE_ADJUSTMENT（实体保留无害，BizModel 回退空 CRUD）。

## Execution Plan

### Phase 1 - VALUE_ADJUSTMENT 业务类型 + 三轴状态机 + 审批

Status: completed
Targets: `module-finance/model/app-erp-finance.orm.xml`（`erp-fin/business-type` 加 VALUE_ADJUSTMENT）；`ErpFinBusinessType` 枚举；`ErpAstValueAdjustmentBizModel`（三轴状态机）；`ErpAstErrors`（新 ErrorCode）；`ErpAstConstants`
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: 无

- [x] `Decision`：在计划记录 VALUE_ADJUSTMENT 业务类型与科目分支 + 折旧基数调整 + 重估增值权益科目裁决（见 Task Route Key Decisions）
  - Skill: `nop-backend-dev`
- [x] `Add`：`erp-fin/business-type` 加性追加 `VALUE_ADJUSTMENT` + `ErpFinBusinessType` 枚举同步（保护区域契约同步，参照 0831-2 范式）；`erp-ast/adjustment-type` 字典将既有 `REVALUATION` 拆分为 `REVALUATION_UP`/`REVALUATION_DOWN`（保留 `IMPAIRMENT`，见 Task Route Decision，bootstrap 空表安全重命名）
  - Skill: `nop-backend-dev`
- [x] `Add`：`ErpAstValueAdjustmentBizModel` 三轴状态机——`submit`（DRAFT→ACTIVE）/`cancel`（DRAFT→CANCELLED）docStatus + `approve`/`reject` approveStatus + 强制审批 config-gated；非法迁移 ErrorCode（参照 1000-2 折旧/处置 `submit`→ACTIVE 三轴范式）；校验资产存在且非已处置
  - Skill: `nop-backend-dev`

Exit Criteria:

> 仅证明状态机可用 + 业务类型一致；过账与净值联动在 Phase 2 验证。

- [x] `VALUE_ADJUSTMENT` 在 ORM 字典与枚举一致；`adjustment-type` 字典三码值齐
- [x] 三轴状态机非法迁移抛 ErrorCode；submit→ACTIVE 前置审批通过（config-gated 强制）

### Phase 2 - VALUE_ADJUSTMENT 过账 + 资产净值/折旧基数联动

Status: completed
Targets: `AssetValueAdjustmentPostingProvider`（或扩展既有资产过账 Provider）；`ErpAstAsset` 净值/折旧基数更新；`ErpAstValueAdjustmentBizModel`（post 接线）
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1

- [x] `Add`：VALUE_ADJUSTMENT 过账 Provider——按 `adjustmentType` 分支科目分解：IMPAIRMENT（借资产减值损失/贷固定资产减值准备）/ REVALUATION_UP（借固定资产/贷资本公积-其他资本公积）/ REVALUATION_DOWN（借资产减值损失/贷固定资产）；经 `IErpFinVoucherBiz.post` 或既有资产过账通道生成凭证，业财回链关联调整单；置 `posted=true`
  - Skill: `nop-backend-dev`
- [x] `Add`：资产卡片净值/折旧基数联动——过账后更新 `ErpAstAsset` 账面净值（减值/重估减值减少、重估增值增加 = ±adjustmentAmount）；减值后/重估增值后折旧基数调整（剩余折旧年限重算，config-gated `revaluation-adjust-depreciation-base`）；对齐折旧引擎后续折旧读新基数
  - Skill: `nop-backend-dev`
- [x] `Add`：`reverse` 红字冲减——红冲 VALUE_ADJUSTMENT 凭证 + 回退资产净值/折旧基数（与 1000-2 处置红冲范式一致）；已红冲单据二次红冲抛 ErrorCode
  - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 减值过账生成 VALUE_ADJUSTMENT 凭证（借资产减值损失/贷减值准备）+ 资产净值减少 adjustmentAmount + 折旧基数下调
- [x] 重估增值/减值分录方向正确 + 净值增减；反向红冲回退净值与凭证

### Phase 3 - 测试 + owner doc

Status: completed
Targets: 行为测试（ast-service）；`docs/design/assets/depreciation-and-posting.md`；`docs/logs/2026/07-05.md`
Skill: `nop-backend-dev`

- Item Types: `Proof | Add`
- Prereqs: Phase 2

- [x] `Proof`：行为测试——三种调整分录（IMPAIRMENT/REVALUATION_UP/REVALUATION_DOWN）+ 净值联动 + 折旧基数调整（减值后后续折旧按新基数）+ 三轴状态机非法迁移 + 反向红冲回退 + 强制审批；策略 `JunitAutoTestCase` + GraphQL 快照，`mvn test -pl module-assets/erp-ast-service -am` + `mvn test -pl module-finance/erp-fin-service -am`
  - Skill: `nop-backend-dev`
- [x] `Add`：owner doc 对齐——`depreciation-and-posting.md` §4.1-4.2 标记落地 + §7 业务类型 VALUE_ADJUSTMENT 落地补注（移除"业务类型命名过时"漂移）；§配置点增 revaluation-adjust-depreciation-base/value-adjustment-require-approval
  - Skill: none

Exit Criteria:

- [x] 新增减值/重估行为测试全绿；1000-2 折旧/处置/资本化套件零回归；finance 过账套件零回归
- [x] owner doc 偏离补注收口（VALUE_ADJUSTMENT Deferred 标记落地）

## Draft Review Record

- Independent draft review iteration 1: needs-revision（ses_0d0e58dc6ffeMiAJ8I5JiEEpMa，general 独立子代理新会话）because 规则 1+9：Current Baseline 未盘点 `erp-ast/adjustment-type` 字典实际值，且 Goals/Phase2/ExitCriteria 预设 IMPAIRMENT/REVALUATION_UP/REVALUATION_DOWN 三码值，而实时 `app-erp-assets.orm.xml:84-87` 仅有 IMPAIRMENT + REVALUATION（单数）两码值；Phase 1「核实...缺则加性补」误将语义拆分描述为纯加性。要求：盘点字典实际值 + 为 REVALUATION→UP/DOWN 拆分补 Decision（含替代+残留风险）。
- 已修订（iter1→iter2）：Current Baseline 补 `erp-ast/adjustment-type` 两码值盘点（:84-87）；新增 Decision「REVALUATION 拆分」（拆分选择 + 两替代：保留单数+方向字段 / 保留守儿码值 + 残留风险：外部系统引用旧码）；Phase 1 改述拆分（bootstrap 空表安全重命名）。
- Independent draft review iteration 2: needs-revision（ses_0d0e24408ffeF477MSX0wQNk1Z，general 独立子代理新会话）— adjustment-type 拆分修复通过，但同类新事实错误：docStatus 预设 DRAFT→CONFIRMED + `confirm` 动作，而实时 `erp-ast/doc-status`（:92-96）仅 DRAFT/ACTIVE/CANCELLED（无 CONFIRMED）；1000-2 范式实为 `submit`→ACTIVE（`ErpAstDisposalBizModel.submit`→`disposalProcessor.submit`）。另 typo「1002-2」应为「1000-2」。
- 已修订（iter2→iter3）：Current Baseline 补 `erp-ast/doc-status` 三码值盘点（DRAFT/ACTIVE/CANCELLED :92-96）；Goals/Phase1/ExitCriteria 全链 CONFIRMED→ACTIVE、`confirm`→`submit`（沿用 1000-2 范式）；修正 1002-2→1000-2。
- Independent draft review iteration 3: accept（ses_0d0df102cffeS696G6Wzg90xy0，general 独立子代理新会话）— doc-status 修复通过（:92-96 三码值属实；submit→ACTIVE 全链一致；无残留 CONFIRMED/1002-2）；adjustment-type 拆分 Decision 完整；逐项码值声明全数核实无新事实错误；14 规则 + 反松弛全过；核算正确（减值→P&L+contra-asset、重估增→资本公积 OCI、重估减→P&L；折旧基数前瞻重算无追溯）。共识达成，转 active。

## Closure Gates

> 仅在所有项目和每阶段退出标准都勾选 `[x]` 后关闭。结束时运行一次完整仓库验证。

- [x] 范围内行为完成（三轴状态机 + 三种调整过账 + 净值/折旧基数联动 + 反向红冲）
- [x] 相关文档对齐（depreciation-and-posting.md / 当日日志）
- [x] 已运行验证：根 `mvn clean install -DskipTests` + `mvn test -pl module-assets/erp-ast-service -am` + `mvn test -pl module-finance/erp-fin-service -am`
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status=completed；Phase 1/2/3 Status=completed 且 Exit Criteria 全 `[x]`；Closure Gates 全 `[x]`；日志 `docs/logs/2026/07-05.md:366-397` 与计划一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位
- [x] 结束证据存在于文件中（见下文 `## Closure`）

## Deferred But Adjudicated

### 自动减值测试（可收回金额计算 + 减值指示触发 cron）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 可收回金额（公允价值减处置费用/现值）计算引擎 + 减值指示数据源属独立结果面；本期手工录入减值金额。对应 `job-scheduling.md` §3.2 `erp-ast-impairment-test`（DESIGN）。
- Successor Required: yes（触发条件：减值测试自动化需求 + 可收回金额数据源落地时）

### 库存物料转固（inventory 出库→资本化）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归 1000-2 另一 Deferred，需跨域 inventory 出库，独立结果面。
- Successor Required: yes（触发条件：库存转固业务需求落地时）

### 重估模式全局切换 + 累计重估转入留存收益

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期支持单次调整分录；CAS 重估模式全局切换与后续重估反转归后继。
- Successor Required: yes（触发条件：重估模式全局采用决策落地时）

### 投资性房地产/无形资产减值专门模型

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本期仅固定资产；投资性房地产/无形资产有专门减值规则。
- Successor Required: yes（触发条件：投资性房地产/无形资产模块深化时）

## Closure

Status Note: 三轴状态机 + 三种调整业财过账 + 资产净值/折旧基数联动 + 反向红冲均已落地并全绿验证；独立结束审计（新会话子代理，不复用执行者上下文）逐项核实实时仓库后接受关闭。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，ses 由 mission-driver closure-audit 流派发；非执行者）
- 实时仓库逐项核实（非信任 `[x]` 标记）：
  - `module-finance/model/app-erp-finance.orm.xml:94` + `erp-fin-dao/.../ErpFinBusinessType.java:51`（`VALUE_ADJUSTMENT(390)`）+ `business-type.dict.yaml:157` + i18n `_erp-fin.i18n.yaml:859` — 业务类型四点一致（ORM/枚举/dict/i18n）
  - `module-assets/model/app-erp-assets.orm.xml:85-87` — `adjustment-type` 字典三码值齐（IMPAIRMENT/REVALUATION_UP/REVALUATION_DOWN，旧单数 REVALUATION 已移除）
  - `ErpAstValueAdjustmentBizModel.java`（Facade，5 个 `@BizMutation`+`@SingleSession` 动作）→ `ErpAstValueAdjustmentProcessor.java`（三轴状态机 + 过账编排 + 净值联动 + 红冲，protected step 方法非空体）
  - `ValueAdjustmentAcctDocProvider.java:42-72` — 按 adjustmentType 三分支科目分解（IMPAIRMENT 借 6702/贷 1604；REVALUATION_UP 借 1601/贷 4002；REVALUATION_DOWN 借 6702/贷 1601），非空实现
  - `ValueAdjustmentPostingDispatcher.java` — 组装 PostingEvent 经 AssetPostingExecutor，失败吞异常保持 posted=false（对齐 disposal 范式）
  - `app-service.beans.xml:32-46` — Provider/Dispatcher/Processor/BizModel 全部注册（非"注册但不可达"）
  - `ErpAstErrors.java:105-141` — 9 个 `ERR_ADJUSTMENT_*` ErrorCode（含 already-reversed/approval-required/illegal-transition）
  - `TestErpAstValueAdjustment.java`（6 case，257 行）：三种调整分录 + 净值联动断言（9000/17000/10000）+ 非法迁移 `assertThrows` + 草稿作废 + 红冲净值回退 + `isAllVouchersReversed` 凭证红冲断言（非占位测试）
  - owner doc `docs/design/assets/depreciation-and-posting.md:341`（落地标记）+ `:344`（业务类型漂移补注收口）；日志 `docs/logs/2026/07-05.md:366-397`（含验证状态）
- Anti-Hollow 复核：Provider 三分支均有实际借/贷 fact 生成；Processor `applyAssetValueChange`/`rollbackAssetValue` 实际写 `ErpAstAsset.netBookValue`/`currentValue`；过账失败路径返回 null 保持 posted=false（非吞异常空体）；红冲经 `executor.reverse` 实际触发凭证红字冲销
- 验证状态（来自日志，执行者声明；审计核实命令与计划 Closure Gates 一致）：`mvn clean install -DskipTests` 全工程 BUILD SUCCESS；`mvn test -pl module-assets/erp-ast-service -am` 全绿（6 新增 + 22 既有零回归）；`mvn test -pl module-finance/erp-fin-service -am` 全绿（零回归）
- 文本一致性：Plan Status / 三 Phase Status / 各 Exit Criteria / Closure Gates / 日志条目全数一致，无 `draft`/`pending`/占位残留
- Deferred honesty：4 项 Deferred（自动减值测试 / 库存转固 / 重估模式全局切换 / 投资性房地产无形资产）均为计划内 Non-Goal 且命名后继触发条件，无已确认缺陷/契约漂移藏匿

Follow-up:

- 自动减值测试（见上方 Deferred）
- 库存物料转固（见上方 Deferred）
- 重估模式全局切换 + 累计重估转入留存收益（见上方 Deferred）
- 投资性房地产/无形资产减值专门模型（见上方 Deferred）
