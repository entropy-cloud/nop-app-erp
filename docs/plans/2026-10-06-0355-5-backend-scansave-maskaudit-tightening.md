# 2026-10-06-0355-5 后端小额收紧（scanSave nextLineNo 热路径 + MaskAuditRecorder 分桶换代）

> Plan Status: completed
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

Status: completed
Targets: `module-inventory/erp-inv-service/.../ErpInvStockTakeLineBizModel.java` + 既有测试类
Skill: none

- Item Types: `Fix | Decision | Proof`

- [x] Decision: 点查形态=**QueryBean orderBy(lineNo desc) + setLimit(1) 点查**（确定性路径，规避 A9 登记的纯维度投影主键注入坑；仓内先例 ErpApsWorkOrderToOperationProcessor 同款）。证明策略=**characterization 不变性测试 + 理由记录**（TestErpInvStockTakeLineScanSave 8/8 绿：lineNo 语义不变）——「行为保持型优化不适用 red-green」；语句计数断言需 DAO 监听设施（仓内无先例）投入>收益；查询形态单点性由代码审阅保证（findAllByExample 全量物化已消除）。
  - Skill: none
- [x] Fix: nextLineNo 单查询化（imports 对齐仓内 QueryBean/FilterBeans/IEntityDao 先例路径）
  - Skill: none
- [x] Proof: characterization 8/8 绿 + 触域 `mvn test -pl module-inventory/erp-inv-service` 绿
  - Skill: none

Exit Criteria:

- [x] JUnit 绿 + characterization 理由记录（按 Phase 1 Decision 二选一）在案

### Phase 2 - MaskAuditRecorder 分桶换代

Status: completed
Targets: `module-common-service/.../MaskAuditRecorder.java` + `TestMaskAuditRecorder`
Skill: none

- Item Types: `Fix | Decision | Proof`

- [x] Fix: 分桶换代实现（ThreadLocal 载荷 `(当前 traceId, LinkedHashSet<4 分量 DedupKey>)`，traceId 变更整桶换代；null-traceId 保守多记/fail-closed/config-gate 语义零变化）；`clearThreadDedup()`/`threadDedupSize()` 适配桶载荷
  - Skill: none
- [x] Decision: 桶容量/线程驻留=**接受驻留上界并记录**（无桶内 cap——E4.2「同一请求内仅记一次」精确成立；驻留上界=单请求 distinct cell 数×短串，随下次请求换代释放，严格优于旧实现的跨请求驻留至 500 触顶；替代[保留 cap+触顶清桶]会在大请求内重引入中段重记漂移，弃）
  - Skill: none
- [x] Proof: 新增 `over500DistinctCellsInSingleRequestEachRecordedOnce` **red-green 实证**（旧实现[HEAD 版临时还原] 1 failure=第 501 cell 起 clear-all 重记红；新实现 0 failure）+ 既有 13 用例全绿（14/14）+ 触域 `mvn test -pl module-common-service` 绿
  - Skill: none

Exit Criteria:

- [x] 新旧用例全绿 + red-green 证据在案（旧实现红/新实现绿，双侧实跑）

### Phase 3 - Decision 裁决与登记

Status: completed
Targets: 本计划 + round-3 报告 Deferred 表
Skill: none

- Item Types: `Decision`

- [x] Decision: StockTakeLine (takeId,lineNo) UK=**watch-only 登记**（单用户 PDA 时序并发面窄；UK 须存量数据去重前置 + orm 保护区双批准；round-3 报告 §6 Deferred 既有登记维持）
  - Skill: none
- [x] Decision: disposalAmount 后端符号守卫=**watch-only 登记（无据）**——实仓核对 docs/design/assets/ 仅 ui-patterns.md:164 字段列举、无金额语义约束；按防真空立法原则不改后端；前端 min:0 已随批次 4 落地（钳制常规路径）
  - Skill: none

Exit Criteria:

- [x] 两项 Decision 落计划（含 owner doc 核对证据）

## Draft Review Record

- Independent draft review iteration 1: NEEDS REVISION (agent_3826a1be, 2026-10-06) — 撤回「E4.2 无需改」（owner doc :105-112 记载 4 分量键+500 cap，须随实现更新）；Phase 1 red-green 不可执行如写（行为保持）→ 二选一机制；分桶键形钉死 4 分量；桶容量/驻留新增 Decision；clearThreadDedup/threadDedupSize 纳入 Phase 2；规则 4/14 合并裁决记录；Closure Gates 补全量构建；:54→:55 笔误 → 全部采纳修订
- Independent draft review iteration 2: NEEDS REVISION→条件收敛 (agent_e418a158, 2026-10-06) — 残留 2 处机械缺陷（Draft Review Record 占位段重复、Phase 1 Exit Criteria 与 red-green 二选一脱节），已按审查者枚举逐字修复；审查者裁决「不动摇技术方案/基线/范围，修订后可执行」→ 达成 ACCEPT 条件

## Closure Gates

> 完整仓库验证在此处一次性执行（本批触 module-common-service/module-inventory/erp-inv-service 两模块 + 文档）。

- [x] 范围内行为完成（nextLineNo 单查询化 + MaskAuditRecorder 分桶换代 + 三项 Decision 成文）
- [x] 相关文档对齐（E4.2 粒度段随实现更新——外科手术级单行 diff；UK/disposalAmount watch-only 登记于 round-3 报告 §6）
- [x] 已运行验证（全项目 `mvn clean install -DskipTests` exit 0 + 触域单测 TestErpInvStockTakeLineScanSave 8/8、TestMaskAuditRecorder 14/14 + red-green 双侧实跑 + `npm run check:unbounded-queries` 71 站点 PASS 零新增 + 合规检查器 exit 0；本批零 flux/view 文件触及故 validate:flux 不适用——git status 可证）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录（迭代 1 agent_3826a1be + 迭代 2 agent_e418a158 条件收敛）
- [x] 文本一致性已验证（状态/阶段/门控/日志一致——本节恢复后核验）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（agent_242ab4da 2026-10-06：ACCEPT 附 3 项回填条件[本节恢复/日志/指针]，已全部实施；a-g 七项实证 + red-green 双侧独立复现 1 failure→14/14 + 工作树哈希级还原核对）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### StockTakeLine (takeId,lineNo) UK（若 Phase 3 裁决 watch-only）

- Classification: `watch-only residual`
- Why Not Blocking Closure: orm 保护区 + 存量数据去重前置；单用户 PDA 并发面窄
- Reopen Trigger: 并发盘点场景立项；或 md 域 G1 类批量索引轮顺带
- Successor Required: `no`

## Closure

Status Note: scanSave nextLineNo 单查询化（characterization 8/8 绿）+ MaskAuditRecorder 分桶换代（red-green 双侧实证、14/14 绿、E4.2 owner doc 同步）+ 三项 Decision 成文（UK/disposalAmount watch-only、桶驻留策略）；规则 4/14 合并裁决在案。

Closure Audit Evidence:

- Auditor / Agent: (pending)

Follow-up:

- (none)
