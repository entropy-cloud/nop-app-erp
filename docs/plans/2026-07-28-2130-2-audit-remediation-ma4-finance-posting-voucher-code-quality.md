# 2026-07-28-2130-2-audit-remediation-ma4-finance-posting-voucher-code-quality MA4 finance 代码质量审计 — 过账与凭证链路（A4.1a）

> Plan Status: completed
> Last Reviewed: 2026-07-28
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA4（工作项 A4.1a，S 级拆分 1/2）
> Related: `docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.4「代码质量（MA4）」行 + §1.3 finance 功能模块拆分「过账引擎与凭证链路」切片；`docs/audits/arm-index.md`（P1 索引）；`docs/skills/code-quality-audit-prompt.md`（审计方法）；`docs/design/finance/posting.md`（过账引擎 owner doc 锚点）；`docs/plans/2026-07-27-2211-2-audit-remediation-ma2-finance-posting-voucher-state-machine.md`（A2.5a 同切片业务正确性审计——状态机正确性，本审计聚焦**代码实现质量**，互补不重叠）；`docs/plans/2026-07-28-2130-3-audit-remediation-ma4-finance-budget-arap-cost-period-code-quality.md`（A4.1b 同域拆分 2/2——预算/AR-AP/成本/期间，不同功能模块，独立计划）
> Audit: required

## Current Baseline

finance 代码质量审计过账与凭证链路切片（代码与前端质量层 MA4 第一项，S 级拆分 1/2）。roadmap 工作项 A4.1a 声明审查"finance 代码质量审计 — 过账与凭证（S 级拆分 1/2）"，owner doc 标注 `docs/design/finance/posting.md`，skill `docs/skills/code-quality-audit-prompt.md`。

**关键基线事实（实时仓库核实）**：

- **finance 域是全域最高复杂度域**（scope matrix §1.1 快照 2026-07-27，用于驱动 S 级分级）：48 实体 / 137 mutation / 36 query / 36 Proc/Engine/Resolver / 24 状态机实体。S 级（mutation ≥ 70 + Java ≥ 250 + Proc ≥ 30 三项均满足，S 级判定稳定），按 scope matrix §1.3 功能模块拆分为 7 片，本审计覆盖「过账引擎与凭证链路」片。（注：scope matrix §1.1 自述"复杂度数据会随代码变化而漂移，MV 验证里程碑需重跑确认"；实时 Java 文件数已增至 436、view.xml 72，但分级结论 S 级不受影响。）
- **过账与凭证链路代码规模**（实时仓库核实）：`find module-finance -path "*service*" \( -name "*Posting*" -o -name "*Voucher*" -o -name "*Processor*" -o -name "*Provider*" \) -name "*.java" -not -path "*/target/*"` = 77 源文件。含核心组件：`ErpFinPostingProcessor`（过账编排器）/ `IErpFinVoucherBiz`（凭证 Facade，9 域 11 调用方）/ `IErpFinAcctDocProvider` SPI（可插拔科目文档 Provider，全域 ~20+ 实现）/ `VoucherFact`（凭证事实）/ `ErpFinVoucherBillR`（业财回链）/ `ErpFinDeferredPostingRetryHelper`（兜底重试）/ `CloseVoucherWriter`（直接持久化路径）。
- **owner doc `posting.md` 锚点**：三层模型（PostingEvent 契约 / 幂等保证 / businessType vs billType 分工 / 业务类型映射唯一权威源 / 凭证模板机制 / 过账引擎可插拔 Provider 机制 / 跨域自动聚合）。owner doc 是过账链路的行为基线。
- **MA2 已审计的已知 finding（代码质量审计输入，非重复审计）**：A2.5a 状态机审查（P1-MA2-031 DRAFT→CANCELLED 缺失 + 红字凭证终态 / P1-MA2-032 IGNORED 凭证悬挂缺告警闭环）；A2.17 并发审计（P0-MA2-018 voucher_bill_r 无 UK deferred + alreadyPosted TOCTOU / reverse() REQUIRES_NEW doc↔code 冲突交接 A3.3）；A2.18 多公司审计（P1-MA2-096 ErpFinGlBalance 无自然键 / P1-MA2-099 GL 映射 cache 跨组织泄漏）；A3.3 owner-doc drift（P1-MA3-030 reverse() REQUIRES_NEW / P1-MA3-031 CommitmentAcctDocProvider 矛盾 / P1-MA3-039 persistVoucher 多币种丢失 / P1-MA3-026 postingType 三源不一致）。
- **MA1 已审计的已知 finding**：P1-MA1-018 finance enum↔dict 漂移；P1-MA1-016 ErpFinAccountingPeriodProcessor 跨域 DAO 查询（reverseDepreciation）。

**审计张力**：MA2 审计了过账链路的**业务正确性**（状态机/并发/隔离/owner-doc drift），但**代码实现质量**（架构边界完整性 / 核心实现正确性 / 类型与契约质量 / 错误处理与操作安全 / 测试有效性 / 可维护性 / 自动化防护）是 MA4 的独立维度。MA2 已知 finding 是本审计的**输入**（复核运行时是否如 owner doc 声明 / 是否有未发现的代码缺陷），非重复审计。本审计聚焦 MA2 未覆盖的代码质量维度：如 PostingProcessor 编排的事务边界健壮性 / Provider SPI 实现的一致性 / 兜底重试的异常吞咽 / VoucherFact 类型安全 / 跨域 Facade 调用的错误传播 / 测试是否覆盖异常路径而非仅黄金路径。

剩余差距：需要一次过账与凭证链路的代码实现质量审计。发现的缺陷分类为：(a) **架构边界违规**（major——跨域写绕过 I*Biz / 生成物手编）；(b) **核心实现正确性**（major/blocker——事务边界缺失 / 异常吞咽致悬挂 / 幂等破缺）；(c) **错误处理与操作安全**（major——NopException 规范 / ErrorCode 完整性）；(d) **测试有效性**（major——异常路径未覆盖 / 测试断言强度不足）；(e) **可维护性风险**（P2——复杂度热点 / 重复模式）。blocker/major 登记为 P1（代码类，目标 MR2——MR2 deps = MA3+MA4 done，由 R2.0 展开机制读取 MA3+MA4 批次 P1；若属业务正确性则目标 MR1）。若发现活跃数据破坏路径，升级标注走 P0 即时通道。

## Goals

- 按 `code-quality-audit-prompt.md` 7 重点领域（架构边界 / 核心实现正确性 / 类型契约 / 错误处理 / 测试有效性 / 可维护性 / 自动化防护）对 finance 过账与凭证链路代码做系统性实现质量审计，产出审计报告。
- 审计覆盖过账链路核心组件：`ErpFinPostingProcessor` 编排 / `IErpFinVoucherBiz` Facade（post/reverse/persistVoucher）/ `IErpFinAcctDocProvider` SPI 及其 ~20+ 实现 / `VoucherFact` 类型 / `ErpFinVoucherBillR` 回链 / `ErpFinDeferredPostingRetryHelper` 兜底重试 / `CloseVoucherWriter` 直接持久化 / 凭证模板机制。
- 复核 MA2 已知 finding（P0-MA2-018 / P1-MA2-031/032 / P1-MA3-026/030/031/039 / P1-MA1-016/018）的运行时状态，标记是否有 MA2 未发现的代码层缺陷。
- scope matrix §2.4「代码质量（MA4）」行增 finance 过账片完成注记段（§2.4 无 per-domain 列，以注记段反映进度；finance 代码质量全片终态在 A4.1b 收口）。
- 发现的 blocker/major 登记为 P1 汇总至 `arm-index.md` §P1 发现汇总。roadmap A4.1a 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**做过账链路的业务正确性/状态机审计 — 归 A2.5a（已 done）。本审计聚焦**代码实现质量**（事务边界/异常处理/类型安全/测试有效性），MA2 已知 finding 作为输入复核而非重复审计。
- **不**做预算/AR-AP/成本/期间代码质量 — 归 A4.1b（同批起草，S 级拆分 2/2，不同功能模块）。
- **不**做 view.xml vs 后端契约 drift — 归 A4.6（MA4 view drift 批次）。本审计审后端代码实现质量，不审前端消费。
- **不**做 owner doc vs 代码 drift — 归 A3.3（已 done）。本审计的 owner doc drift 复核以 A3.3 已登记 finding 为输入。
- **不**做预算/AR-AP/成本/期间代码质量 — 归 A4.1b。本审计覆盖 `CloseVoucherWriter` 直接持久化路径实现质量；A4.1b 仅复核期间结账侧调用点的错误传播（`ErpFinArApItemGenerator` 实现质量归 A4.1b）。
- **不**做测试覆盖深度统计 — 归 A5.1（MA5 测试层）。本审计的"测试有效性"维度审**异常路径覆盖 + 断言强度**，非覆盖深度统计。
- **不**做权限注解完整性 — 归 A6.1/A6.2（MA6 安全层）。
- **不**在本计划内批量修复代码缺陷 — P1 经 R2.0 展开机制进入 MR2（文档+代码）/MR1（业务正确性类）。本审计只识别缺陷 + 分类。
- **不**手改生成物或 ORM 源模型。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/finance/posting.md`（过账引擎 owner doc 锚点——roadmap A4.1a 指定）；`module-finance/erp-fin-service/`（过账链路代码实现——审计对象）；`docs/design/finance/state-machine.md`（凭证状态机——A2.5a 已审，本审计复核运行时）；`docs/audits/2026-07-27-2211-arm-ma2-finance-posting-voucher-state-machine.md`（A2.5a 已知 finding——本审计输入）
- Skill Selection Basis: `code-quality-audit-prompt.md`（roadmap A4.1a 指定此 skill——7 重点领域 + 严重性指南 P0-P3 + 发现按严重性排序。项目定制化层见 `docs/skills/README.md`）。与 A4.1b 不同结果表面（过账/凭证链路 vs 预算/AR-AP/成本/期间），独立计划。与 A2.5a 不同维度（代码实现质量 vs 业务正确性状态机），互补不重叠。
- Verification: 审计不改代码/文档，故无单测回归；报告产出即更新 `arm-index.md`。代码缺陷修复在 MR2（文档+代码）/MR1（业务正确性）批量进行。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。本审计为代码静态审查 + 测试有效性抽样，不运行应用。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码/文档，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。代码静态审查无回归风险，build/test 门控为同型审计 plan 的标准 Closure 实践。

## Execution Plan

### Phase 1 - 过账与凭证链路代码实现质量系统性审计（7 重点领域）

Status: completed
Targets: `module-finance/erp-fin-service/` 过账链路代码（ErpFinPostingProcessor / IErpFinVoucherBiz 实现 / IErpFinAcctDocProvider SPI 实现 / VoucherFact / ErpFinVoucherBillR / ErpFinDeferredPostingRetryHelper / CloseVoucherWriter / 凭证模板）；owner doc `docs/design/finance/posting.md`
Skill: `code-quality-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线）；MA1 + MA2 done（已知 finding 作为输入）；A2.5a done（状态机基线）；A3.3 done（owner-doc drift 基线）。

- [x] 领域「架构和边界完整性」：核查过账链路代码的跨域访问合规性——Provider SPI 实现是否经 I*Biz 接口（非 daoFor 直访）/ 凭证写入是否经 Facade / 生成物是否手编。复核 P1-MA1-016（ErpFinAccountingPeriodProcessor 跨域 DAO）运行时状态。标记边界违规站点。
      - Skill: `code-quality-audit-prompt.md`
      - 结果：⚠️(P1) — 6 站点跨域 daoFor(ErpMd*) 绕 I*Biz（P1-MA4-003，同 P1-MA1-022 根因在 finance posting 投影）；P1-MA1-016 如登记；CloseVoucherWriter 绕 Facade 为文档化设计选择；生成物零手编 ✓。见报告 §2.1。
- [x] 领域「核心实现正确性」：核查 PostingProcessor 编排的事务边界健壮性（@BizMutation 自动事务 + REQUIRES_NEW 隔离）/ alreadyPosted 幂等检查的 TOCTOU（复核 P0-MA2-018 deferred 状态）/ 兜底重试 ErpFinDeferredPostingRetryHelper 的异常吞咽与悬挂（复核 P1-MA2-032）/ persistVoucher 多币种折算（复核 P1-MA3-039）/ CloseVoucherWriter 直接持久化路径是否绕过编排。标记事务/幂等/异常处理缺陷。
      - Skill: `code-quality-audit-prompt.md`
      - 结果：⚠️(P1) — 新发现 P1-MA4-001（重试耗尽 RETRYING 死状态无升级）；alreadyPosted TOCTOU 如 P0-MA2-018 登记；persistVoucher 多币种如 P1-MA3-039 登记（运行时确认 drift 活跃）；事务边界正确 ✓。见报告 §2.2。
- [x] 领域「类型和契约质量」：核查 VoucherFact 类型安全（单一 amount 字段 vs amountSource/amountFunctional 分离——复核 P1-MA2-002/009）/ Provider SPI 实现的参数返回契约一致性 / postingType 三源不一致（复核 P1-MA3-026）。标记类型不匹配/契约漂移。
      - Skill: `code-quality-audit-prompt.md`
      - 结果：⚠️(P1 维持) — VoucherFact 单 amount 如 P1-MA2-002/009 登记；postingType 三源如 P1-MA3-026 登记（局部常量重复→P2-MA4-001）；Provider SPI 契约一致 ✓。见报告 §2.3。
- [x] 领域「错误处理和操作安全」：核查过账链路异常是否全部扩展 NopException + ErrorCode（`erp.err.fin.*`）/ 异常是否携带足够上下文（billHeadCode/businessType）/ MANUAL_POST 与 AUTO_POST 路径的错误传播差异。标记裸异常/ErrorCode 缺失/错误信息不足。
      - Skill: `code-quality-audit-prompt.md`
      - 结果：✅(P3) — 全链路 NopException+ErrorCode + O-6 未预期异常归一化 + 上下文齐全 ✓；CloseVoucherWriter:82 inline ErrorCode.define 未集中（P3→P2-MA4-001）。见报告 §2.4。
- [x] 领域「测试有效性」：抽样 finance 64 测试中过账链路相关测试，核查**异常路径覆盖**（非仅黄金路径）+ 断言强度（是否仅断言 posted=true 还是校验凭证行数值/业财回链）。复核 P1-MA2-032（IGNORED 悬挂）测试覆盖。标记测试空洞。
      - Skill: `code-quality-audit-prompt.md`
      - 结果：⚠️(P1) — 新发现 P1-MA4-002（断言弱：仅 count lines + total，未校验行级 amountSource/amountFunctional/exchangeRate；异常/重试路径零覆盖：无多币种/IGNORED-悬挂/重试耗尽/红冲红字凭证负向）；多币种 bug P1-MA3-039 对测试不可见。见报告 §2.5。
- [x] 领域「可维护性和未来变更风险」：核查过账链路复杂度热点（PostingProcessor 行数/圈复杂度）/ Provider 实现的重复模式（~20+ 实现）/ 凭证模板配置的可维护性。标记 P2 可维护性风险。
      - Skill: `code-quality-audit-prompt.md`
      - 结果：⚠️(P2) — P2-MA4-001（4 项合并：no-op setAcctSchemaId catch:198 / 局部常量重复 / incrementRetryAndRethrow 命名误导 / inline ErrorCode）；PostingProcessor 944 行步骤化良好；凭证模板可维护性良好 ✓。见报告 §2.6。
- [x] 领域「自动化和防护覆盖」：核查过账链路是否有 compliance checker 规则守护（R8 Processor 无 xbiz / R2 daoFor 跨域）/ 是否有测试门控防止回归。标记防护缺口。
      - Skill: `code-quality-audit-prompt.md`
      - 结果：⚠️(P2) — P2-MA4-002（checker R2d 未覆盖 *Resolver/*Propagator/*Helper → finance 内部跨域 daoFor 无守卫；TOCTOU/重试耗尽无静态规则归测试门控）。见报告 §2.7。
- [x] 产出审计报告 `docs/audits/2026-07-28-2130-arm-ma4-finance-posting-voucher-code-quality.md`（含：7 领域逐项审查结果 / MA2 已知 finding 运行时复核 / P0-P3 finding 清单按严重性排序 / 每项含文件路径+行引用 / 裁决通过/失败 / 剩余风险）。
      - Skill: none
      - 结果：报告已产出（8 节：TL;DR / 范围矩阵 / 7 领域逐项 / P1 清单 3 项 / P2 清单 2 项 / 交叉去重 / MA1-MA3 运行时复核 12 项 / 剩余风险交接 / 裁决 FAIL）。

Exit Criteria:

> 审计报告是唯一可观察产物。完整仓库 `mvn test` 属 Closure Gates（见执行时规则 7）。

- [x] 7 重点领域逐项审查结果产出（每领域至少一句裁决，含"本领域无缺陷"）
- [x] MA2 已知 finding 运行时复核产出（每项标记"如 owner doc 声明"或"发现新代码层缺陷"）
- [x] P0-P3 finding 清单产出按严重性排序，每个含文件路径+行引用+严重性+缺陷描述+影响+目标 MR

### Phase 2 - finding 汇总交接 MR2/MR1 + 索引/矩阵更新

Status: completed
Targets: 过账链路代码质量 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.4「代码质量（MA4）」finance 列
Skill: none

- Item Types: `Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [x] finding 汇总：全部缺陷 blocker/major 登记为 P1 至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA4-NNN`，next = 001+、报告、领域、缺陷描述、目标 MR2[代码类]/MR1[业务正确性类]、修复状态 todo）。与 MA2/MA3 已登记 P1 经交叉去重无冲突。
      - Skill: none
      - 结果：3 项 P1 已登记 arm-index 新增 §A4.1a 段（P1-MA4-001 MR1[业财悬挂] / P1-MA4-002 MR2[测试质量] / P1-MA4-003 MR1[同 P1-MA1-022 一并，不重复计入 MR2]）+ 2 项 P2 watch-only（P2-MA4-001/002）。交叉去重：P1-MA4-003 同 P1-MA1-022 根因；P1-MA4-001 与 P1-MA2-032 相邻路径同型；P1-MA4-002 与 A5.1 互补。报告 §5 已记录去重裁决。
- [x] 分类裁决：代码实现质量 finding 目标 MR2（MR2 deps = MA3+MA4 done）；业务正确性类 finding 目标 MR1；活跃数据破坏走 P0 即时通道，在报告中明确标注。
      - Skill: none
      - 结果：P1-MA4-002（测试质量）→ MR2；P1-MA4-001（业财悬挂闭环）/ P1-MA4-003（架构边界同 P1-MA1-022）→ MR1；本审计零 P0（报告 §0 + §8 明确标注：无活跃数据破坏路径，TOCTOU 是 P0-MA2-018 已登记 deferred）。
- [x] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §2.4「代码质量（MA4）」行增 finance 过账片完成注记段（§2.4 无 per-domain 列，以注记段反映）。
      - Skill: none
      - 结果：arm-index 报告清单新增本报告行（done）+ 新增 §A4.1a findings 段（3 P1 + 2 P2）；scope matrix §2.4 表后新增 finance 过账片完成注记段（Verdict FAIL + 3 P1 摘要 + 终态在 A4.1b 收口）。

Exit Criteria:

- [x] 所有缺陷 blocker/major 已登记 arm-index §P1 汇总（代码类 MR2 / 业务正确性类 MR1），待展开
- [x] 与 MA2/MA3 已登记 P1 经交叉去重无重复登记
- [x] arm-index 报告清单 + scope matrix 注记段已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_056cc3627ffeuxa5gLxlvnuiAp`，独立 general 子代理，fresh-context，对照实时仓库逐项复核）。发现 2 项 BLOCKER（规则 1 基线诚实性）：(a) finance 总数（331 Java/64 测试/36 view.xml）误标"实时仓库核实"，实时为 436 Java/64 测试/72 view.xml——这些是 scope matrix §1.1 快照数；(b) find 命令未加 `-name "*.java" -not -path "*/target/*"` 过滤致不可复现 77。已验证正确项：全部引用 finding ID 真实存在于 arm-index ✓；规则 14 拆分正当（A2.5a/b/c 先例存在为 3 独立 plan）✓；MR 路由正确 ✓；scope 匹配 ✓；anti-slack ✓。
- Independent draft review iteration 2: **accept**（同源审查复核）——修订已落地：(1) finance 复杂度基数改为标注"scope matrix §1.1 快照 2026-07-27，用于驱动 S 级分级"+ 注记实时漂移（436 Java/72 view.xml，分级结论 S 级不受影响），删除"实时仓库核实"误标；(2) find 命令补 `-name "*.java" -not -path "*/target/*"` 过滤（77 源文件可复现）；(3) §2.4 退出标准修正（§2.4 无 per-domain 列，改注记段）；(4) Non-Goals 补 CloseVoucherWriter 所有权归属。Plan Status 转 active。

## Closure Gates

> 本计划主体是代码静态审查 + 测试有效性抽样（不改代码；产出为审计报告 + arm-index/scope-matrix 更新）。完整仓库验证在此处运行一次（同型审计 plan 的标准 Closure 实践）。代码缺陷修复在 MR2/MR1 批量进行；活跃数据破坏走 P0 即时通道。本审计只识别缺陷 + 分类。

- [x] 范围内行为完成（A4.1a 过账与凭证链路代码质量审计报告产出 + arm-index 更新 + scope matrix 标记完成）
- [x] 相关文档对齐（审计报告、arm-index、scope matrix 结论已反映）
- [x] 已运行验证：代码静态审查无代码变更，build/test 门控仅作回归基线确认（同型审计 plan 的相同 Closure 实践）
- [x] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR2/MR1）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证（状态、阶段、门控、日志都一致）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留空作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 预算/AR-AP/成本/期间代码质量（A4.1b）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计聚焦过账与凭证链路代码实现质量；finance 其余功能模块（预算/AR-AP/成本/期间）归 A4.1b（同批起草，S 级拆分 2/2）。若过账审计中发现跨模块代码质量问题，标注交接 A4.1b。
- Successor Required: `yes`——A4.1b 执行时复核。

### 业务正确性/状态机（A2.5a）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计审代码**实现质量**（事务/异常/类型/测试）；过账链路业务正确性/状态机归 A2.5a（已 done）。MA2 已知 finding 作为本审计输入复核。
- Successor Required: `no`——A2.5a 已 done。

### view.xml drift（A4.6）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 前端 view.xml 调用的 API/字段 vs 后端契约 drift 归 A4.6。本审计审后端代码实现质量。
- Successor Required: `yes`——A4.6 执行时复核 finance view。

### 测试覆盖深度统计（A5.1）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本审计"测试有效性"维度审异常路径覆盖 + 断言强度；覆盖深度统计归 A5.1。
- Successor Required: `yes`——A5.1 执行时复核 finance 测试深度。

## Closure

Status Note: A4.1a finance 过账与凭证链路代码实现质量审计已完成。两 Phase 全 done（报告产出 + arm-index/scope-matrix 更新）；独立 closure audit（新会话）CLOSURE_PASS（7/7 检查通过，含 2 项 finding 活体复核）；完整仓库 `mvn test` BUILD SUCCESS（基线绿，无代码变更）。审计结论 Verdict FAIL（零 P0 + 3 项新 P1[MR1×2/MR2×1] + 2 项 P2 watch-only）；MA1/MA2/MA3 已知 finding 运行时复核 12 项全部「如登记」无升级（P1-MA2-032 复核发现相邻路径新缺陷 P1-MA4-001）。发现的代码缺陷经分类进入 MR1（P1-MA4-001 业财悬挂 / P1-MA4-003 架构边界同 P1-MA1-022）/ MR2（P1-MA4-002 测试质量），不在本审计批量修复（按 plan Non-Goals）。

Closure Audit Evidence:

- Auditor / Agent: 独立 general 子代理 `ses_05697a3e1ffeXligA1VVkuKIW4`（fresh-context，对照实时仓库逐项复核）
- Verdict: **CLOSURE_PASS**（7/7 检查通过）
- Evidence:
  - 检查 1（报告完整性）：7 领域各含裁决（§1.2 L51-57 + §2.1-§2.7）；MA1/MA2/MA3 运行时复核 12 项齐全（§6 L228-239）；P0-P3 finding 清单按严重性排序（§0 TL;DR + §3 P1 三表 + §4 P2 表）✓
  - 检查 2（finding 活体复核）：P1-MA4-001 经 `ErpFinDeferredPostingRetryHelper.java:133-136` + `deferred-posting-sweep.batch.xml:14-15` 实仓确认 RETRYING 死状态真实；P1-MA4-003 经 grep `daoFor(ErpMd` 于 posting/ 命中 6 站点与报告一致 ✓
  - 检查 3（arm-index 登记）：报告行 done（L50）+ §A4.1a 段登记 P1-MA4-001/002/003 + 2 P2 + 无 ID 碰撞（MA4 独立命名空间）✓
  - 检查 4（scope matrix 注记）：§2.4 表后 A4.1a 完成注记段（Verdict FAIL + 3 P1 摘要 + A4.1b 收口）✓
  - 检查 5（plan 一致性）：Phase 1/2 均 `Status: completed` + 全部 `[x]`（含 exit criteria），无残留 `[ ]` ✓
  - 检查 6（去重诚实）：P1-MA4-003 同 P1-MA1-022 不重复计入 MR2 + P1-MA4-001 与 P1-MA2-032 相邻路径不重叠，报告 §5 + arm-index 双重记录 ✓
  - 检查 7（build 框架）：Infrastructure § + Closure Gates 引言均框定 build/test 为基线确认（无代码变更）✓
- 验证基线：完整仓库 `mvn test` → `[INFO] BUILD SUCCESS`（执行者运行，2026-07-28；代码静态审查无代码变更，build/test 仅作回归基线确认）。
