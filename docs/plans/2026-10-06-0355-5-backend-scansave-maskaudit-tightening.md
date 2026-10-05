# 2026-10-06-0355-5 后端小额收紧（scanSave nextLineNo 热路径 + MaskAuditRecorder 分桶换代）

> Plan Status: active
> Last Reviewed: 2026-10-06
> Source: docs/analysis/2026-10-06-0352-perf-ux-round3-increment-analysis.md §3.2 F-R11 + §4 MaskAudit 注记
> Related: 2026-10-05-0400-2-pda-stock-take-closure.md（scanSave 交付）、2026-10-05-0400-4-mask-audit-dedup-boundary.md（traceId 进键裁决）
> Audit: required

## Current Baseline

- `ErpInvStockTakeLineBizModel.nextLineNo`（:152-162，调用点 :139）：`findAllByExample(仅 takeId)` 全列物化 + 内存 for 求 max+1——扫码热路径（`DEFAULT_BARCODE_ENABLED=true`）单盘点 O(N) 行/次、整盘点累计 O(N²)。`IDX_INV_STOCK_TAKE_LINE_TAKE_ID` 既有可承载点查。已知平台坑（A9 登记）：纯维度投影仍注入主键维度——即便如此仍单行返回，收益保持。
- `ErpInvStockTakeLine` 无 (takeId,lineNo) 唯一键（orm.xml StockTakeLine 段仅非唯一索引）——并发双扫可静默重复行号（round-2 F3 交付了服务端生成未补 UK）。
- `MaskAuditRecorder`（`MAX_DEDUP_ENTRIES=500` :55，键构造 :133-148）：traceId 为 DedupKey 第 5 分量 + 容量 clear-all——修复后键基数=单请求 distinct cell 数，>500 cell 的大列表授权读会请求中段整集清空 → 已记录 cell 同请求重记（E4.2「同一请求内仅记一次」在大请求内漂移；方向=过记保守）。ThreadLocal 键跨请求不命中已由 traceId 保证，但条目集合本身跨请求残留至容量清空。
- **owner doc 现状**：`docs/design/roles-and-permissions.md:105-112`（E4.2）明文记载去重键为 4 分量「userId × entityName × objId × fieldName」+「ThreadLocal LinkedHashSet 上限 500 防泄漏」——该段已落后于 0400-4 的 traceId 第 5 分量改动，且本批分桶换代将再度改变键形与容量语义：**E4.2 段落更新是本批计划级义务**（非「无需改」）。
- 测试基线：`TestMaskAuditRecorder` 现有 13 用例（含 `dedupDifferentUserSeparateAudits` :190-199 同 traceId 不同 user 分立断言）——分桶换代须保持 4 分量键语义方可全绿。
- 遗留 API：`clearThreadDedup()`/`threadDedupSize()`（:202-212，实仓零外部调用）编码旧载荷形态，换代须同步更新/删除。
- `ErpAstDisposalProcessor.java:121-122` 对负 `disposalAmount` 零校验（静默参与损益）——round-3 报告 F-R10 后端半，Decision 待 owner doc 核对。

## Goals

> **规则 4/14 合并裁决（迭代 1 审查要求显式化）**：本计划合并 scanSave（module-inventory）与 MaskAuditRecorder（module-common-service）两个组件——二者 owner doc 与验证路径不同，按规则 14 拆分判据亦可成立；合并理由=同为 round-3 批次 5 微额收紧、结束标准同构联合（触域单测绿+red-green/proof 证据+零 orm 保证）、Closure Gates 完全共享、体量极小（2 生产文件+测试+Decision），规则 4 的「多个独立结束标准」触发条件不成立。替代=拆为两 plan（碎片化成本>收益）；风险=单批审计面稍宽，由分 Phase 隔离补偿。

- `nextLineNo` 单查询化：`orderBy(lineNo desc) + limit 1` 点查（规避投影注入坑的确定性路径；若执行时实证 OQL max 投影单行可行亦可，Decision 记录）。
- `MaskAuditRecorder` 分桶换代：ThreadLocal 载荷改为 (lastTraceId, 去重集)，traceId 变更换代整集；**去重集键保持 4 分量 (userId, entityName, objId, fieldName) 不变**（traceId 升为桶身份）——保证既有 13 用例（含同 traceId 不同 user 分立）语义不变；null-traceId 跳过去重语义不变。
- JUnit：MaskAudit 新增 >500 cell 单请求「各记一次」用例（旧 clear-all 实现下红、新实现绿——red-green 成立）；nextLineNo 的证明策略归 Phase 1 Decision（SQL 语句计数断言 vs characterization 不变性测试+理由记录）。
- Decision ×3：StockTakeLine UK（倾向 watch-only 登记）；disposalAmount 后端符号守卫（先核对 ast owner doc）；**分桶后桶容量/线程驻留策略**（保留桶内 cap+触顶清桶[保守多记方向与现状一致] vs 接受桶驻留上界=上一请求 distinct cell 数并记录）。
- owner doc：更新 `docs/design/roles-and-permissions.md` E4.2 段落（键形+换代语义+容量策略与实现对齐）。

## Non-Goals

- 不做 orm.xml 变更（UK 若裁决立项须独立 plan + 双批准）。
- 不动 ErpInvBarcodeBizModel 六连点查链（既有裁决）。
- 不扩 GATE-04 宇宙（Deferred 登记）。

## Task Route

- Type: `implementation-only change`（行为保持优化 + 审计语义精确化）
- Owner Docs: docs/design/roles-and-permissions.md E4.2（「同一请求线程内仅记一次」）；docs/design/inventory/ 盘点相关段落；docs/design/assets/ 处置金额语义
- Skill Selection Basis: Skill: none（单方法优化 + 既有测试类扩展；orm 保护区不在本批）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。

## Execution Plan

### Phase 1 - scanSave nextLineNo

Status: planned
Targets: `module-inventory/erp-inv-service/.../ErpInvStockTakeLineBizModel.java` + 既有测试类
Skill: none

- Item Types: `Fix | Decision | Proof`

- [ ] Decision: 点查形态（orderBy-limit1 vs OQL max）+ 证明策略（SQL 语句计数断言[旧=findAllByExample 全量 N+1、新=1 条点查，red-green 成立] vs characterization 不变性测试+「行为保持型优化不适用 red-green」理由记录）——二选一落计划
- [ ] Fix: nextLineNo 单查询化
- [ ] Proof: 按 Decision 策略执行（计数断言 red-green 或 characterization 全绿+理由）+ 既有 lineNo 语义用例（TestErpInvStockTakeLineScanSave :134,:150-151,:197-200）保持绿 + 触域 `mvn test -pl module-inventory/erp-inv-service -am`

Exit Criteria:

- [ ] JUnit 绿 + red-green 证据**或** characterization 理由记录（按 Phase 1 Decision 二选一）在案

### Phase 2 - MaskAuditRecorder 分桶换代

Status: planned
Targets: `module-common-service/.../MaskAuditRecorder.java` + `TestMaskAuditRecorder`
Skill: none

- Item Types: `Fix | Proof`

- [ ] Fix: 分桶换代实现（4 分量键形不变；保持 null-traceId 保守多记、fail-closed、config-gate 语义零变化）；`clearThreadDedup()`/`threadDedupSize()` 同步更新/删除
- [ ] Decision: 桶容量/线程驻留策略（桶内 cap+触顶清桶 vs 接受驻留上界并记录）落计划
- [ ] Proof: 新增 >500 cell 单请求「各记一次」用例（红绿实证：旧实现 clear-all 下重记、新实现精确一次）+ 既有 13 用例全绿 + `mvn test -pl module-common-service -am` 绿

Exit Criteria:

- [ ] 新旧用例全绿 + red-green 证据在案

### Phase 3 - Decision 裁决与登记

Status: planned
Targets: 本计划 + docs/analysis/2026-10-06-0352 报告 Deferred 表（如 watch-only）
Skill: none

- Item Types: `Decision`

- [ ] Decision: StockTakeLine UK——核对盘点 owner doc 与并发场景登记，预期 watch-only（触发条件登记）
- [ ] Decision: disposalAmount 符号守卫——先实仓核对 docs/design/assets/ 处置金额语义；有据 → NopException 守卫 + JUnit；无据 → watch-only 登记（防真空立法）

Exit Criteria:

- [ ] 两项 Decision 落计划（含 owner doc 引文或「无据」证据）

## Closure Gates

- [ ] 范围内行为完成
- [ ] 相关文档对齐（`docs/design/roles-and-permissions.md` E4.2 段落随实现更新；audit 域测试证据在计划）
- [ ] 已运行验证（全项目 `mvn clean install -DskipTests` + 触域单测 + `npm run check:unbounded-queries` + 合规检查器 actual≤baseline）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证
- [ ] 结束审计由独立子代理（新会话）执行
- [ ] 结束证据存在于文件中

## Draft Review Record

- Independent draft review iteration 1: NEEDS REVISION (agent_3826a1be, 2026-10-06) — 撤回「E4.2 无需改」（owner doc :105-112 记载 4 分量键+500 cap，须随实现更新）；Phase 1 red-green 不可执行如写（行为保持）→ 二选一机制；分桶键形钉死 4 分量；桶容量/驻留新增 Decision；clearThreadDedup/threadDedupSize 纳入 Phase 2；规则 4/14 合并裁决记录；Closure Gates 补全量构建；:54→:55 笔误 → 全部采纳修订
- Independent draft review iteration 2: NEEDS REVISION→条件收敛 (agent_e418a158, 2026-10-06) — 残留 2 处机械缺陷（Draft Review Record 占位段重复、Phase 1 Exit Criteria 与 red-green 二选一脱节），已按审查者枚举逐字修复；审查者裁决「不动摇技术方案/基线/范围，修订后可执行」→ 达成 ACCEPT 条件

## Deferred But Adjudicated

### StockTakeLine (takeId,lineNo) UK（若 Phase 3 裁决 watch-only）

- Classification: `watch-only residual`
- Why Not Blocking Closure: orm 保护区 + 存量数据去重前置；单用户 PDA 并发面窄
- Reopen Trigger: 并发盘点场景立项；或 md 域 G1 类批量索引轮顺带
- Successor Required: `no`

## Closure

Status Note: (pending)

Closure Audit Evidence:

- Auditor / Agent: (pending)

Follow-up:

- (none)
