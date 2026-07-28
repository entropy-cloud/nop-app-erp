# 2026-07-29-0024-3-audit-remediation-ma4-assets-depreciation-processor-code-quality MA4 assets 折旧引擎与 Processor 链路专属审计（A4.3）

> Plan Status: completed
> Last Reviewed: 2026-07-29
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA4（工作项 A4.3，assets 折旧引擎与 Processor 链路专属审计——scope matrix §1.2 assets 特殊处理独立工作项）
> Related: `docs/audits/audit-remediation-scope-and-dimension-matrix.md` §1.2 assets 特殊处理 + §2.4「**assets Processor 链路专属（MA4 新增）**」行 + §2.5「assets Processor 链路」触发依据；`docs/audits/arm-index.md`（P1 索引）；`docs/skills/code-quality-audit-prompt.md`（审计方法）；`docs/design/assets/depreciation-and-posting.md` + `state-machine.md` + `split-merge.md` + `cip.md` + `inventory.md` + `maintenance.md`（owner doc 锚点）；`docs/plans/2026-07-28-0400-2-audit-remediation-ma2-assets-state-machine.md`（A2.10 同域业务正确性审计——状态机正确性，本审计聚焦**代码实现质量**，互补不重叠）；`docs/plans/2026-07-28-2130-2-audit-remediation-ma4-finance-posting-voucher-code-quality.md`（A4.1a finance 过账片——MA4 已落地的首例范式参照）
> Audit: required

## Current Baseline

assets 折旧引擎与 Processor 链路代码质量专属审计（代码与前端质量层 MA4 第五项，assets 域专用工作项）。roadmap 工作项 A4.3 声明审查"assets 折旧引擎与 Processor 链路专属审计（48 Processor，全域最高密度）"，owner doc 标注 `docs/design/assets/`，skill `docs/skills/code-quality-audit-prompt.md`。

**关键基线事实（实时仓库核实）**：

- **assets 域 Processor 密度全域最高**（scope matrix §1.1 快照 2026-07-27）：24 实体 / 61 mutation / 48 Proc/Engine/Resolver / 18 状态机实体 / 14 测试 / 23 跨域 daoFor。Proc=48 ≥ 30 满足 S 级判定，但 scope matrix §1.2 assets 特殊处理裁决：assets Processor 集中在折旧引擎/处置/价值调整/资本化一个子系统（非分散在多个独立子系统），保持 A 级，但新增 A4.3 专属审计确保 48 Processor 得到聚焦审查。**折旧正确性直接影响财务报表，是高风区域。**
- **assets 折旧引擎与 Processor 链路代码规模**（实时仓库核实）：`find module-assets -path "*service*" \( -name "*Depreciation*" -o -name "*Disposal*" -o -name "*ValueAdjust*" -o -name "*Capitaliz*" -o -name "*Processor*" -o -name "*Posting*" -o -name "*PostingDispatcher*" -o -name "*AcctDocProvider*" \) -name "*.java" -not -path "*/target/*"` = 70 文件（含 ~8 测试）。核心组件：
  - **折旧引擎**：`DepreciationCalculator`（折旧计算——直线/工作量等多种方法）/ `ErpAstDepreciationScheduleProcessor`（折旧计划编排 + executeBatchDepreciation/executeDepreciation/reverseDepreciation 期末批量 + 月度 cron）
  - **Processor 链路（审批轴 × 业务事件）**：Capitalization 6 Processor（建卡 IN_SERVICE）/ Disposal 6 Processor（处置 SCRAPPED/SOLD）/ ValueAdjustment 7 Processor（减值/重估）/ Split 7 Processor（拆分）/ Merge 7 Processor（合并）/ Inventory 2 Processor（盘点）/ Maintenance 2 Processor（维修资本化/费用化）/ Cip 1 Processor（在建工程转固）
  - **过账链路**：`AssetPostingExecutor`（过账编排）+ 9 PostingDispatcher × AcctDocProvider 对（Depreciation/Disposal/ValueAdjustment/Capitalization/AssetInventory/AssetMerge/AssetSplit/MaintenanceCapitalization/MaintenanceExpense）——每对构造 IErpFinAcctDocProvider 科目文档 + 经 IErpFinVoucherBiz Facade 过账
  - **业务实体 BizModel**：`ErpAstDepreciationScheduleBizModel` / `ErpAstDisposalBizModel` / `ErpAstValueAdjustmentBizModel` / `ErpAstAssetCapitalizationBizModel`
- **owner docs**：`depreciation-and-posting.md`（折旧引擎 + 过账链路 + §七 错误处理）/ `state-machine.md`（资产卡片 + 折旧计划 + 7 业务单据状态机）/ `split-merge.md`（拆分/合并）/ `cip.md`（在建工程）/ `inventory.md`（盘点）/ `maintenance.md`（维修资本化/费用化）。
- **MA2 已审计的已知 finding（代码质量审计输入，非重复审计）**：A2.10 assets 状态机审查（P1-MA2-058 Movement reverseApprove→SUBMITTED INLINE 违反 owner doc §2 强制 REJECTED / P1-MA2-059 Movement 全 5 INLINE 缺 isCancelled 守卫 / P1-MA2-060 Capitalization/Disposal tryPost 吞异常悬挂 + reverseApprove 仅 posted=true 时回滚资产状态致资产侧状态悬挂[DeferredPostingSweepJob 兜底] / P1-MA2-061 ErpAstAsset IDLE 死状态 + 折旧引擎只查 IN_SERVICE）；A2.17 并发审计（**P1-MA2-089 executeDepreciation 缺 status==PENDING 守卫致并发首次折旧执行重复 schedule 行 + 双计累计折旧** + 透明乐观锁降级——10 个 assets 状态机实体全部声明 versionProp）；A2.18 多公司审计（assets 侧折旧/CoA cache 方向性 SAFE，但相关隔离缺口读路径交接）。
- **MA1 已审计的已知 finding**：P1-MA1-008（ErpAstDepreciationSchedule/Movement/Revaluation/Split/Merge/Disposal/Capitalization/Transfer 共 29 列 propId 缺失）；P1-MA1-022（assets 跨域只读 daoFor——`ErpAstDepreciationScheduleProcessor:290` ErpFinAccountingPeriod + 9 个 posting dispatcher ErpMdSubject）；P2-MA1-023/024（DISPOSED/CANCELLED owner doc drift——writer 存在非死状态）。
- **MA3 已审计的已知 finding（owner-doc drift，复核输入）**：P1-MA3-033（auto-depreciation 配置键名漂移——`erp-fin.auto-depreciation` vs code `erp-fin.auto-depreciation-on-close`，直接影响 assets 折旧自动触发）。

**审计张力**：MA2 审计了 assets 链路的**业务正确性**（状态机/并发），并已发现高严重度 finding（P1-MA2-060 过账悬挂 + reverseApprove 不对称 / P1-MA2-089 并发重复折旧），但**代码实现质量**（架构边界完整性 / 核心实现正确性 / 类型与契约质量 / 错误处理与操作安全 / 测试有效性 / 可维护性 / 自动化防护）是 MA4 的独立维度。MA2 已知 finding 是本审计的**输入**（复核运行时是否如 owner doc 声明 / 是否有未发现的代码缺陷）。本审计聚焦 MA2 未覆盖的代码质量维度：如 DepreciationCalculator 多种折旧方法的算术正确性与精度（折旧正确性直接影响财务报表）/ executeBatchDepreciation 编排的事务边界与部分失败处理 / 9 PostingDispatcher × AcctDocProvider 对的过账异常吞咽与悬挂一致性（复核 P1-MA2-060 tryPost 同型根因是否在全部 9 对一致）/ reverseApprove 红冲闭环对称性（复核 P1-MA2-060 资产侧状态悬挂）/ 并发首次折旧重复（复核 P1-MA2-089）/ Processor 审批轴 6×7 的对称性与重复模式 / 跨域 Facade 调用（IErpFinVoucherBiz / IErpFinAccountingPeriodBiz）错误传播 / auto-depreciation config 触发链路（复核 P1-MA3-033）/ 测试是否覆盖异常路径（过账悬挂/并发重复折旧/部分失败/红冲不对称）。

剩余差距：需要一次 assets 折旧引擎与 Processor 链路的代码实现质量专属审计。发现的缺陷分类为：(a) **架构边界违规**（major——跨域写绕过 I*Biz / 生成物手编）；(b) **核心实现正确性**（major/blocker——折旧算术错误 / 事务边界缺失 / 异常吞咽致悬挂 / 幂等破缺 / 并发重复）；(c) **错误处理与操作安全**（major——NopException 规范 / ErrorCode 完整性）；(d) **测试有效性**（major——异常路径未覆盖 / 测试断言强度不足）；(e) **可维护性风险**（P2——48 Processor 重复模式 / 复杂度热点）。blocker/major 登记为 P1（代码类目标 MR2——MR2 deps = MA3+MA4 done；若属业务正确性则目标 MR1）。若发现活跃数据破坏路径（折旧算术错误/并发双计累计折旧直接影响财务报表），升级标注走 P0 即时通道。

## Goals

- 按 `code-quality-audit-prompt.md` 7 重点领域（架构边界 / 核心实现正确性 / 类型契约 / 错误处理 / 测试有效性 / 可维护性 / 自动化防护）对 assets 折旧引擎与 Processor 链路代码做系统性实现质量专属审计，产出审计报告。
- 审计覆盖核心组件：DepreciationCalculator + ErpAstDepreciationScheduleProcessor（折旧引擎 + 期末批量 + cron）/ 8 类业务 Processor 审批轴（Capitalization/Disposal/ValueAdjustment/Split/Merge/Inventory/Maintenance/Cip）/ AssetPostingExecutor + 9 PostingDispatcher × AcctDocProvider 对（过账链路）/ 业务实体 BizModel。
- 复核 MA1/MA2/MA3 已知 finding（P1-MA1-008/022 / P1-MA2-058/059/060/061/089 / P1-MA3-033 / P2-MA1-023/024）的运行时状态，标记是否有 MA2 未发现的代码层缺陷。重点复核折旧算术正确性与并发重复折旧（直接影响财务报表）。
- scope matrix §2.4「assets Processor 链路专属（MA4 新增）」行 + §2.5「assets Processor 链路」行推进至完成（assets 域 MA4 代码质量终态在此收口）。
- 发现的 blocker/major 登记为 P1 汇总至 `arm-index.md` §P1 发现汇总。roadmap A4.3 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**做 assets 状态机/业务正确性审计 — 归 A2.10（已 done）。本审计聚焦**代码实现质量**（折旧算术/事务边界/异常处理/类型安全/测试有效性），MA2 已知 finding 作为输入复核而非重复审计。
- **不**做 view.xml vs 后端契约 drift — 归 A4.6（MA4 view drift 批次）。
- **不**做 owner doc vs 代码 drift — 归 A3.3/A3.4（已 done）。本审计的 owner doc drift 复核以 A3.4 已登记 finding 为输入。
- **不**做 finance 侧过账引擎实现质量（DepreciationAcctDocProvider 等经 IErpFinVoucherBiz Facade，其 Facade 实现质量归 A4.1a）——本审计复核 assets 侧 9 PostingDispatcher 调用点的错误传播与悬挂。
- **不**做测试覆盖深度统计 — 归 A5.4（MA5 测试层，assets 14 测试 / 61 mutation 比 0.23）。
- **不**做权限注解完整性 — 归 A6.1/A6.2（MA6 安全层）。
- **不**做 assets 的 view.xml/前端代码 — assets 前端归 A4.8 view drift 批次。
- **不**在本计划内批量修复代码缺陷 — P1 经 R2.0/R1.0 展开机制进入 MR2/MR1。本审计只识别缺陷 + 分类。
- **不**手改生成物或 ORM 源模型。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/assets/depreciation-and-posting.md`（折旧引擎 owner doc 锚点——roadmap A4.3 指定）+ `state-machine.md` + `split-merge.md` + `cip.md` + `inventory.md` + `maintenance.md`；`module-assets/erp-ast-service/`（折旧引擎 + Processor 链路代码实现——审计对象）；`docs/audits/2026-07-28-0400-arm-ma2-assets-state-machine.md`（A2.10 已知 finding——本审计输入）；`docs/audits/2026-07-28-1249-arm-ma2-concurrency-optimistic-lock.md`（A2.17 P1-MA2-089——本审计输入）
- Skill Selection Basis: `code-quality-audit-prompt.md`（roadmap A4.3 指定此 skill——7 重点领域 + 严重性指南 P0-P3 + 发现按严重性排序。项目定制化层见 `docs/skills/README.md`）。与 A2.10 不同维度（代码实现质量 vs 业务正确性状态机），互补不重叠。与 finance/mfg MA4 计划不同结果表面（assets 折旧/Processor 链路），独立计划。
- Verification: 审计不改代码/文档，故无单测回归；报告产出即更新 `arm-index.md`。代码缺陷修复在 MR2/MR1 批量进行。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。本审计为代码静态审查 + 测试有效性抽样，不运行应用。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码/文档，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。代码静态审查无回归风险，build/test 门控为同型审计 plan 的标准 Closure 实践。

## Execution Plan

### Phase 1 - 折旧引擎与 Processor 链路代码实现质量系统性专属审计（7 重点领域）

Status: completed
Targets: `module-assets/erp-ast-service/` 折旧引擎 + Processor 链路代码（DepreciationCalculator + ErpAstDepreciationScheduleProcessor / 8 类业务 Processor 审批轴 Capitalization/Disposal/ValueAdjustment/Split/Merge/Inventory/Maintenance/Cip / AssetPostingExecutor + 9 PostingDispatcher × AcctDocProvider 对 / 业务实体 BizModel ErpAstDepreciationScheduleBizModel/ErpAstDisposalBizModel/ErpAstValueAdjustmentBizModel/ErpAstAssetCapitalizationBizModel）；owner docs `docs/design/assets/{depreciation-and-posting,state-machine,split-merge,cip,inventory,maintenance}.md`
Skill: `code-quality-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA1 + MA2 + MA3 done（已知 finding 作为输入）；A2.10 done（状态机基线）；A2.17 done（并发基线 P1-MA2-089）；A3.4 done（owner-doc drift 基线）；A4.1a done（MA4 过账 Facade 范式参照）。

- [x] 领域「架构和边界完整性」：核查折旧引擎/Processor 链路代码的跨域访问合规性——9 PostingDispatcher 是否经 IErpFinVoucherBiz Facade 过账（非 daoFor 直写凭证）/ 折旧引擎读 ErpFinAccountingPeriod 是否经 Facade（复核 P1-MA1-022 ErpAstDepreciationScheduleProcessor:290 跨域 DAO）/ Processor 读 ErpMdSubject 科目是否合规 / reverseDepreciation 被 finance 跨域调用点（finance 侧归 A4.1b，本审计复核 assets 侧接待实现质量）。标记边界违规站点。
      - Skill: `code-quality-audit-prompt.md`
- [x] 领域「核心实现正确性」：核查 DepreciationCalculator 多种折旧方法的算术正确性与精度（直线/工作量等——折旧正确性直接影响财务报表）/ executeBatchDepreciation 编排的事务边界与部分失败处理（一批资产中单个失败是否整批回滚还是部分提交）/ 9 PostingDispatcher × AcctDocProvider 对的过账异常吞咽与悬挂一致性（复核 P1-MA2-060 tryPost 同型根因是否在全部 9 对一致——检查返回值/posted/告警）/ reverseApprove 红冲闭环对称性（复核 P1-MA2-060 资产侧状态悬挂——posted=false 窗口期是否回滚资产）/ 并发首次折旧重复（复核 P1-MA2-089 executeDepreciation 缺 PENDING 守卫）/ auto-depreciation cron 触发链路（复核 P1-MA3-033 config 键名）。标记算术错误/事务/幂等/异常悬挂/并发缺陷。
      - Skill: `code-quality-audit-prompt.md`
- [x] 领域「类型和契约质量」：核查 8 类业务 Processor 审批轴的参数返回契约一致性 / 9 AcctDocProvider 科目文档构造的 BigDecimal 类型安全（折旧额/净值/残值）/ 折旧计划 schedule 行的金额精度。标记类型不匹配/契约漂移。
      - Skill: `code-quality-audit-prompt.md`
- [x] 领域「错误处理和操作安全」：核查折旧引擎/Processor 链路异常是否全部扩展 NopException + ErrorCode（`erp.err.ast.*`）/ 折旧算术溢出/过账失败/并发冲突/资产状态非法迁移的错误传播 / 批量折旧部分失败的告警闭环。标记裸异常/ErrorCode 缺失/错误信息不足。
      - Skill: `code-quality-audit-prompt.md`
- [x] 领域「测试有效性」：抽样 assets 14 测试中折旧/Processor 相关测试（TestErpAstDepreciation / TestErpAstDisposal / TestErpAstDisposalWorkflowApproval / TestErpAstValueAdjustment / TestErpAstCapitalization / TestErpAstPostingReverse / TestErpAstAcctDocProviderAccountKey），核查**异常路径覆盖**（过账悬挂/并发重复折旧/批量部分失败/红冲不对称/折旧算术边界[残值/期末]/折旧计划已执行重复执行）+ 断言强度（是否仅断言 posted=true 还是校验折旧额数值/累计折旧/净值/凭证行）。标记测试空洞——assets 测试/mutation 比 0.23 偏低，异常路径覆盖是重点。
      - Skill: `code-quality-audit-prompt.md`
- [x] 领域「可维护性和未来变更风险」：核查 48 Processor 的重复模式（8 类业务 × 审批轴 5-7 动作的对称性）/ DepreciationCalculator 折旧方法策略的可扩展性 / 9 AcctDocProvider 对的对称性。标记 P2 可维护性风险——48 Processor 全域最高密度，重复模式提取候选。
      - Skill: `code-quality-audit-prompt.md`
- [x] 领域「自动化和防护覆盖」：核查折旧引擎/Processor 链路是否有 compliance checker 规则守护（R8 Processor 无 xbiz / R2 daoFor 跨域）/ 是否有测试门控防止回归（折旧算术/过账/红冲）。标记防护缺口——折旧正确性直接影响财务报表，防护优先级高。
      - Skill: `code-quality-audit-prompt.md`
- [x] 产出审计报告 `docs/audits/2026-07-29-0024-arm-ma4-assets-depreciation-processor-code-quality.md`（含：7 领域逐项审查结果 / MA1/MA2/MA3 已知 finding 运行时复核 / P0-P3 finding 清单按严重性排序 / 每项含文件路径+行引用 / 裁决通过/失败 / 剩余风险）。
      - Skill: none

Exit Criteria:

> 审计报告是唯一可观察产物。完整仓库 `mvn test` 属 Closure Gates（见执行时规则 7）。

- [x] 7 重点领域逐项审查结果产出（每领域至少一句裁决，含"本领域无缺陷"）
- [x] MA1/MA2/MA3 已知 finding 运行时复核产出（每项标记"如 owner doc 声明"或"发现新代码层缺陷"）
- [x] P0-P3 finding 清单产出按严重性排序，每个含文件路径+行引用+严重性+缺陷描述+影响+目标 MR

### Phase 2 - finding 汇总交接 MR2/MR1 + 索引/矩阵更新

Status: completed
Targets: 折旧引擎/Processor 链路代码质量 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.4「assets Processor 链路专属」行 + §2.5「assets Processor 链路」行
Skill: none

- Item Types: `Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [x] finding 汇总：全部缺陷 blocker/major 登记为 P1 至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA4-NNN`，起始编号 = A4.1a/A4.1b/A4.2a/A4.2b 已分配最大 P1-MA4-N + 1，避免命名空间碰撞；报告、领域、缺陷描述、目标 MR2[代码类]/MR1[业务正确性类]、修复状态 todo）。与 MA2/MA3/A4.1a/A4.1b/A4.2a/A4.2b 已登记 P1 经交叉去重无冲突。
      - Skill: none
- [x] 分类裁决：代码实现质量 finding 目标 MR2；业务正确性类 finding 目标 MR1；活跃数据破坏走 P0 即时通道（折旧算术错误/并发双计累计折旧直接影响财务报表，升级评估优先），在报告中明确标注。
      - Skill: none
- [x] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §2.4「assets Processor 链路专属（MA4 新增）」行 + §2.5「assets Processor 链路」行推进至完成（assets 域 MA4 代码质量终态收口）。
      - Skill: none

Exit Criteria:

- [x] 所有缺陷 blocker/major 已登记 arm-index §P1 汇总（代码类 MR2 / 业务正确性类 MR1），待展开
- [x] 与 MA2/MA3/A4.1a/A4.1b/A4.2a/A4.2b 已登记 P1 经交叉去重无重复登记
- [x] arm-index 报告清单 + scope matrix 已反映审计结论（assets Processor 链路终态）

## Draft Review Record

- Independent draft review iteration 1: **accept**（`ses_0567157e5ffehitUbfELmNO82C`，独立 general 子代理，fresh-context，对照实时仓库逐项复核）。VERDICT: accept，无 BLOCKER。LIVE-REPO 复核：find 计数 70（=基线声称）可复现；processor/ 39 文件 + posting/ 19 文件（9 PostingDispatcher + 9 AcctDocProvider + 1 AssetPostingExecutor）；9 对 dispatcher↔provider 1:1 配对验证（Depreciation/Disposal/ValueAdjustment/Capitalization/AssetInventory/AssetMerge/AssetSplit/MaintenanceCapitalization/MaintenanceExpense）；核心组件全部存在（DepreciationCalculator/ErpAstDepreciationScheduleProcessor/AssetPostingExecutor/ErpAstAssetCapitalizationBizModel/ErpAstDisposalBizModel/ErpAstValueAdjustmentBizModel PASS）；"48 Proc/Engine/Resolver" 正确引自 scope matrix §1.1（非误标 48 Processor 文件）；finding ID 真实（P1-MA1-008/022 + P1-MA2-058/059/060/061/089 + P1-MA3-033 均 PASS，assets 归属正确）；owner docs 存在（6 个全 PASS）；scope matrix §2.4「assets Processor 链路专属」行 + §2.5「assets Processor 链路」行均存在。逐项裁决：规则 1 基线诚实（9 对配对 + 48 Proc 引用准确）PASS / 规则 2 边界清晰（排除 A2.10 状态机 + finance A4.1a Facade）PASS / 规则 4/5/7 一个结果表面 + 类型标注 PASS / 规则 8/9 skill 正确 PASS / 高风险强调（折旧算术 + 并发双计 P1-MA2-089 直接影响财务报表 + P0 升级评估）PASS / anti-slack PASS / P1-MA4 命名空间起始 = A4.1a/A4.1b/A4.2a/A4.2b max(006)+1 = 007 collision-safe PASS / 命名 N=3 PASS / Plan Status draft PASS。Plan Status 转 active。

## Closure Gates

> 本计划主体是代码静态审查 + 测试有效性抽样（不改代码；产出为审计报告 + arm-index/scope-matrix 更新）。完整仓库验证在此处运行一次（同型审计 plan 的标准 Closure 实践）。代码缺陷修复在 MR2/MR1 批量进行；活跃数据破坏走 P0 即时通道。本审计只识别缺陷 + 分类。

- [x] 范围内行为完成（A4.3 折旧引擎与 Processor 链路代码质量专属审计报告产出 + arm-index 更新 + scope matrix 标记完成）
- [x] 相关文档对齐（审计报告、arm-index、scope matrix 结论已反映）
- [x] 已运行验证：代码静态审查无代码变更，build/test 门控仅作回归基线确认（同型审计 plan 的相同 Closure 实践）
- [x] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR2/MR1）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留空作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### assets view.xml drift（A4.6/A4.8）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 前端 view.xml 调用的 API/字段 vs 后端契约 drift 归 A4.6/A4.8。本审计审后端代码实现质量。
- Successor Required: `yes`——A4.6/A4.8 执行时复核 assets view。

### 测试覆盖深度统计（A5.4）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计"测试有效性"维度审异常路径覆盖 + 断言强度；覆盖深度统计归 A5.4（assets 14 测试 / 61 mutation 比 0.23）。
- Successor Required: `yes`——A5.4 执行时复核 assets 测试深度。

### 业务正确性/状态机（A2.10）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计审代码**实现质量**（折旧算术/事务/异常/类型/测试）；assets 状态机业务正确性归 A2.10（已 done）。MA2 已知 finding（P1-MA2-058/059/060/061/089）作为本审计输入复核。
- Successor Required: `no`——A2.10 已 done。

## Closure

Status Note: A4.3 assets 折旧引擎与 Processor 链路代码质量专属审计完成。审计报告 `docs/audits/2026-07-29-0024-arm-ma4-assets-depreciation-processor-code-quality.md` 产出（7 重点领域逐项裁决 + MA1/MA2/MA3 已知 finding 运行时复核 9 项「如登记」无升级 + P0-P3 finding 清单）。**Verdict: ⚠️(P1)——零 P0**（无活跃数据破坏；折旧算术经残值约束双重兜底 + 三方法主路径数值断言测试覆盖；并发双计 P1-MA2-089 已登记 deferred 待 MR1）。**3 项新 P1**（P1-MA4-013 折旧 dispatcher posted=false 业财悬挂无自动重试/告警——MR1；P1-MA4-014 折旧/Processor 链路测试有效性不足——MR2；P1-MA4-015 跨域 daoFor 绕 I\*Biz 同 P1-MA1-022 投影——MR1 不重复计入 MR2）+ **2 项新 P2** watch-only（P2-MA4-006 可维护性热点 / P2-MA4-007 自动化防护缺口）。全部 P1 登记至 `arm-index.md` §P1 汇总（与 MA2/MA3/A4.1a/A4.1b/A4.2a/A4.2b 已登记 P1 交叉去重无重复登记）。arm-index 报告清单 + scope matrix §2.4/§2.5「assets Processor 链路」行推进至 ⚠️(P1) done。roadmap A4.3 推进至 done。build/test 回归基线确认：`mvn clean install -DskipTests` BUILD SUCCESS + `mvn test -pl module-assets/erp-ast-service -am` 90/90 全绿（代码静态审查无代码变更，门控仅作回归基线确认）。**assets 域 MA4 代码质量终态在此收口：3 P1 + 2 P2，零 P0。** 独立结束审计（fresh-context 通用子代理）VERDICT: 所有 7 项实质交付物 PASS——唯一 blocker 类（Closure Gates 8 项勾选 + 本 Status Note/Evidence 填写）已修复。

Closure Audit Evidence:

- Auditor / Agent: independent general subagent（fresh-context closure audit，新会话，2026-07-29）
- Evidence: Fresh-context walkthrough against live repo. (1) Report `docs/audits/2026-07-29-0024-arm-ma4-assets-depreciation-processor-code-quality.md` complete — 7 code-quality domains each with verdict, §3 MA1/MA2/MA3 known-findings runtime re-review (9 items all 「如登记」), §4 P0-P3 list sorted with file:line refs, §5 ⚠️(P1) verdict + zero P0 + remaining risks. (2) Spot-check P1-MA4-013 real — `DepreciationPostingDispatcher.tryPost:43-57` confirmed swallow-on-Exception→return null; no `DeferredPostingSweepJob` covers `ErpAstDepreciationSchedule` (grep *DeferredPosting*.java=0). (3) P1-MA4-014 real — `TestErpAstDepreciation` residualValue all `BigDecimal.ZERO` (6 sites); `TestErpAstPostingReverse` zero mock/concurrency/posted=false-window trigger. (4) P1-MA4-015 real — `ErpAstDepreciationScheduleProcessor.findPeriod:289-296` uses `daoFor(ErpFinAccountingPeriod)`. (5) Numbering collision-safe: P1-MA4-013/014/015 follow max prior 012; P2-MA4-006/007 follow max prior 005; cross-dedup vs P1-MA2-060 (different path) + P1-MA1-022 (root projection) explicit. (6) arm-index.md report row done + A4.3 summary + three P1 rows with correct columns. (7) scope matrix §2.5 "assets Processor 链路 ⚠️(P1) done". (8) roadmap A4.3 = done. Plan deliverables all PASS. Fix applied: 8 Closure Gates ticked + Status Note + this evidence line (was the only blocker category).

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
