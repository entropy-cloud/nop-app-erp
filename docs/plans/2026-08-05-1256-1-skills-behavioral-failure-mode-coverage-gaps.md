# 2026-08-05-1256-1 技能库行为失败模式覆盖缺口补强

> Plan Status: completed
> Last Reviewed: 2026-08-05
> Source: 用户提问「如何避免再次出现审计发现的反复失败；docs/skills 是否需要新增审计提示词」+ 两个独立子代理综合分析（ses_02fbffddeffeZHsPh5sXxrbok1 失败模式报告 / ses_02fbfea17ffe0q6csewssCEdXZ 技能覆盖报告）
> Related: `docs/skills/README.md`、`docs/audits/requirement-compliance-methodology.md`、`docs/lessons/07-11-*.md`
> Audit: required

## Current Baseline

经两份独立子代理报告交叉印证，`docs/skills/` 现有 24 个技能（另含 README.md）对 4 类「行为失败模式」**全无显式捕获信号**，且这些模式在 lessons 07-11 + batch-scheduling-audit 中有真实跨域复发证据：

- **B1 业财过账/异步任务吞异常悬挂**：`catch(Exception){log.warn}` 吞咽 → 业务标志位 `posted=false` 永久悬挂、无告警闭环。跨 12 站点同型（P1-MA2-032/048/060/068/074/080 + P1-MA4-001/004/007/010/013/020），经 R1.16 修但缺系统性回归保护。最相关的 `nop-platform-conformance-audit-prompt.md` 仅查「异常是否 extends NopException」，不查 catch 宽度/标志位终态/告警通道。
- **B2 dict 死状态**：dict 声明 N 状态值但生产代码仅部分有 `setStatus` writer，未写的永不出现。跨 8+ 域同型（lesson 10 表格列 finance/mfg/hr/inv/qa/prj/contract/aps/logistics），经 R1.13/14/15/19/20/21/22/25 修。`state-machine-business-review-prompt.md` 查迁移图完整性但不查 dict 值的 writer 可达性；`orm-model-audit-prompt.md` 仅查 dict 命名规范。
- **B3 批次/调度链断裂**：调度链节点缺失败接力/重试拓扑/状态闭合。专题报告 `docs/audits/2026-07-04-0000-batch-scheduling-audit.md`（13KB）+ P1-RC-042（spc-sampling.batch.xml 漏调 evaluateRules，生产 cron 永不触达规则评估）。`state-machine-business-review-prompt.md` 是单状态机审查，`cross-module-dependency-audit-prompt.md` 是模块级 DAG，均不涉 job 级调度链。
- **B4 守卫完整性散点**：状态机非法迁移守卫 + 并发乐观锁守卫 + `@BizMutation`/`@BizQuery` 权限注解守卫 + 输入边界守卫被切碎散落在 3 个技能，无统一清单切片。

同时存在维护性缺口：
- **孤儿技能**：`docs/skills/configuration-audit-prompt.md` 存在但 `docs/skills/README.md:26-52` 注册表与 `:54-78` 入门列表均未登记。
- **方法论未注册**：`docs/audits/requirement-compliance-methodology.md`（446 行，含 §4 三判据、§去重协议、五级追踪矩阵）已被 ~20 份 RC 审计报告 de facto 当方法使用（如 `docs/audits/2026-08-02-2328-rc-ma1-a1-12-hr-*.md`、`docs/audits/2026-08-03-0900-3-rc-ma1-a1-23-assets-*.md` 等），但 `docs/skills/` 全目录 grep `requirement-compliance-methodology` 零命中、README 未注册——audit-remediation MR2+ 无法经 skills 路由复用 §4 三判据核验。
- **路由断点**：`docs/audits/00-audit-execution-guide.md`（审计执行章程）未被 README 任何位置引用。
- **信号未路由**：`docs/skills/README.md:123-132 §已知失败模式` 仅列 8 项 ORM/Java 微模式，lessons 07-11 的 5 个系统性行为失败模式未进加权清单，导致 `multi-dimensional-audit-prompt.md:32` 与 `open-ended-audit-prompt.md:31` 的「已知失败模式作为维度内搜索加权」缺这些信号。

## Goals

- **G1** 新增 1 个合并技能 `behavioral-failure-mode-scan-prompt.md`，捕获 B1+B2+B3+B4 四类代码层行为失败模式（grep 程式 + 决策树 + 反模式自检表）。
- **G2** 注册孤儿技能 `configuration-audit-prompt.md` 进 README。
- **G3** 薄壳注册 `requirement-compliance-audit-prompt.md`（指向 methodology，不搬全文）。
- **G4** 强化 3 个现有技能（state-machine-business-review / multi-dimensional-audit / design-doc-audit）补部分覆盖缺口。
- **G5** 扩展 README §已知失败模式 8→13 项 + 加「关联文档」指针修路由断点。
- **G6** 反向验证：新/改技能不与现有技能冗余（按 README:188-197 反模式表 + 子代理冗余评估）。

## Non-Goals

- **NG1** 不为「Deferred 无批准 / hollow closure / owner doc 失实」新增技能——这三类已被 closure-audit / plan-audit / closure-pending-detection / multi-dim 强覆盖，问题是执行纪律（closure gate 未强制触发）非技能缺口。
- **NG2** 不把 `requirement-compliance-methodology.md` 全文搬进 skills（用薄壳方案 A 指向）。
- **NG3** 不合并 multi-dim 与 open-ended（冗余评估确认是设计性互补）。
- **NG4** 不改任何生产代码/ORM/契约——纯 docs/skills 方法论层。
- **NG5** 不解决「Compliance 基线漂移 / arm-index 状态不回填」——这些是 closure gate 执行纪律问题，归 mission-driver 钩子强化，非本计划范围。
- **NG6** 不扩展 checker R2d 扫描范围（daoFor ErpFin/ErpInv/ErpPur）——属工具增强非提示词，归 successor。

## Task Route

- Type: `verification or audit work`（方法论/审计技能补强）
- Owner Docs: `docs/skills/README.md`、`docs/audits/requirement-compliance-methodology.md`、`docs/audits/00-audit-execution-guide.md`、`docs/lessons/07-11-*.md`、`docs/audits/2026-07-04-0000-batch-scheduling-audit.md`
- Skill Selection Basis: 本计划本身是技能库补强；起草用 `document-audit-prompt` 思路（文档一致性），草案审查用 `plan-audit-prompt`，结束审计用 `closure-audit-prompt`。不涉及平台/ORM 技能。

## Infrastructure And Config Prereqs

No infra prereqs beyond existing baseline. 纯文档计划，无构建/测试/数据库/外部服务依赖。

## Execution Plan

### Phase 1 - 新增 behavioral-failure-mode-scan-prompt.md

Status: completed
Targets: `docs/skills/behavioral-failure-mode-scan-prompt.md`（新建）
Skill: `document-audit-prompt`（思路参考）

- Item Types: `Add | Decision`
- Prereqs: 无

- [x] Add: 创建技能文件，含 3 节 + 自检反模式清单
      - §1 业财过账/异步任务异常闭环扫描（来自 lesson 09）
        - grep 目标域 `catch\s*\(Exception` 在 PostingDispatcher/Processor/Job 类中的所有出现
        - 对每个 catch 块核查 4 信号：是否 rethrow 或落 FAILED；业务标志位（posted/docStatus）是否进终态；是否经 `IErpSysNotificationBiz` 进告警通道；调度链 preCheck 是否扫 IGNORED（非仅 PENDING）
        - 反模式自检表（搬 lesson 09:60-66）
        - Skill: `document-audit-prompt`
- [x] Add: §2 状态机 dict 可达性扫描（来自 lesson 10）
        - 枚举目标域 `<dict>` 声明的每个状态值，grep `setStatus(*_X)` writer
        - 无 writer 的值强制三选一裁决（删/实现/Deferred），引用 owner doc 迁移图核对
        - 决策树（搬 lesson 10:43-59）
        - Skill: `document-audit-prompt`
- [x] Add: §3 调度链 + 守卫完整性扫描（来自 batch-scheduling-audit + MA6 守卫维度）
        - **§3.1 调度链扫描**（审计对象 = job 级调度拓扑，参考 `docs/audits/2026-07-04-0000-batch-scheduling-audit.md` 方法）：调度链中 job A 失败时 job B/C 的接力策略；调度依赖图是否有环/孤立节点；失败重试是否对称（同步/异步兜底）；调度链节点的状态闭合（成功/失败/放弃三态显式）。**与 §3.2 审计对象不同**（调度拓扑 vs 代码层守卫），分立子节避免膨胀（README:188-197 反模式），G6 反向冗余验证重点复核此边界。
        - **§3.2 守卫完整性扫描**（审计对象 = 代码层守卫清单）：状态机非法迁移守卫 + 并发乐观锁守卫 + `@BizMutation`/`@BizQuery` 权限注解守卫 + 输入边界守卫——作为单切片单域的统一清单。
        - Skill: `document-audit-prompt`
- [x] Add: 文件头元数据（使用场景 / 不使用场景 / 必需输入 / 预期输出，对齐 README 注册表四列格式）
        - 使用场景：单域/单切片代码层行为失败模式扫描，closure 前由独立子代理运行
        - 不使用场景：设计层状态机图审查（用 state-machine-business-review）/ 平台规则合规（用 nop-platform-conformance）/ 单计划闭合（用 closure-audit）
        - Skill: `document-audit-prompt`
- [x] Decision: 与现有技能边界声明
        - 不替代 state-machine-business-review（设计层状态机图 vs 代码层 catch/dict/setStatus 扫描）
        - 不替代 closure-audit（单计划闭合 vs 单域行为扫描，可作 closure-audit 强制前置或独立维度）
        - 不替代 nop-platform-conformance（平台规则 vs 业务行为闭环）
        - 不替代 multi-dimensional-audit（多维整件工作挑战 vs 单域代码层扫描；behavioral-failure-mode-scan 可作 multi-dim 维度内的输入证据，但不替代其维度框架——两者抽象层不同）
        - 理由：避免单 skill 膨胀（README:188-197 反模式），4 类共享同一审计对象（代码层行为闭环）/同一生命周期（closure 前）/同一证据来源（lessons 09/10 + batch-scheduling-audit），合并优于分立
        - Skill: none

Exit Criteria:

> 文档计划；本阶段交付可观察结果为技能文件存在且结构完整。

- [x] `docs/skills/behavioral-failure-mode-scan-prompt.md` 存在，含 §1/§2/§3 三节 + 文件头元数据 + 与现有技能边界声明
- [x] §1/§2 各含 grep 程式；§3.1 含调度链拓扑分析程式（接力/有环/重试对称/状态闭合）+ §3.2 含守卫清单核查；各节含决策树/反模式自检表（即可被独立子代理照做执行）

### Phase 2 - 注册孤儿技能与薄壳注册方法论

Status: completed
Targets: `docs/skills/README.md`（编辑）、`docs/skills/requirement-compliance-audit-prompt.md`（新建薄壳）
Skill: `document-audit-prompt`

- Item Types: `Add | Fix`
- Prereqs: Phase 1 完成（同批写入 README 注册表）

- [x] Fix: `docs/skills/README.md:26-52` 注册表补登 `configuration-audit-prompt.md` 一行（修孤儿）
      - 使用场景/不使用场景/必需输入/预期输出 按 configuration-audit-prompt.md 文件头实际填写
      - Skill: `document-audit-prompt`
- [x] Add: 新建 `docs/skills/requirement-compliance-audit-prompt.md` 薄壳（< 50 行）
      - 主体指向 `docs/audits/requirement-compliance-methodology.md`（不搬全文）
      - 含使用场景（需求→实现五级追踪矩阵 + §4 三判据核验）/ 不使用场景（audit-remediation doc↔code 一致性，用 design-doc-audit）/ 必需输入（L1 use-cases + L2 owner docs + L3 code + L4 tests）/ 预期输出（9 段报告 + arm-index 衔接）
      - 声明：本技能是 methodology 的可路由入口，方法论主体与真相源冻结条款以 methodology 为准
      - Skill: `document-audit-prompt`
- [x] Add: `docs/skills/README.md:26-52` 注册表补登 `requirement-compliance-audit-prompt.md` 一行
      - Skill: `document-audit-prompt`
- [x] Add: `docs/skills/README.md:54-78` 入门列表补登两项（configuration-audit-prompt + requirement-compliance-audit-prompt）
      - Skill: `document-audit-prompt`

Exit Criteria:

- [x] `docs/skills/configuration-audit-prompt.md` 在 README 注册表与入门列表均出现
- [x] `docs/skills/requirement-compliance-audit-prompt.md` 薄壳存在且主体指向 methodology，README 注册表与入门列表均出现

### Phase 3 - 强化 3 个现有技能补部分覆盖缺口

Status: completed
Targets: `docs/skills/state-machine-business-review-prompt.md`、`docs/skills/multi-dimensional-audit-prompt.md`、`docs/skills/design-doc-audit-prompt.md`
Skill: `document-audit-prompt`

- Item Types: `Add`
- Prereqs: 无（独立于 Phase 1/2）

- [x] Add: `state-machine-business-review-prompt.md` 增「dict 可达性 grep 步骤」
      - 在现有「转换完整性/终端状态」检查后追加：枚举 dict 每个状态值 grep `setStatus` writer，无 writer 的强制三选一裁决（删/实现/Deferred）+ owner doc 迁移图核对
      - 交叉引用 behavioral-failure-mode-scan §2（避免重复，本处是设计层提示，详细 grep 程式在 behavioral-failure-mode-scan）
      - Skill: `document-audit-prompt`
- [x] Add: `multi-dimensional-audit-prompt.md` 按 `:30`「添加项目特定维度」机制（与既有示例「ORM 完整性 / 代码生成纪律」并列），**新增项目特定维度**「view.xml gen-control 契约（badge 调色板 vs dict 真值）」
      - **澄清**：该技能现有 7 维度均为通用维度（维度 6 实为「路由和技能选择正确性」），A4.6/A4.7/A4.8 报告中的「view.xml drift 7 维度」是审计时临时适配的项目维度，从未落盘到技能文件——本次是**新增项目维度**而非向不存在的「维度 6」追加
      - 检查 delta view 的 gen-control `<c:script>` 内 `== 'ACTIVE'`/successVals/dangerVals 是否对齐本域 dict 真值（非跨域残留如 DELIVERED/PAID/SETTLED 或共享 erp/doc-status 的 ACTIVE 误用到域专属状态列）
      - 引用 P2-MA4-014/015/016/017/019/020（注：P2-MA4-018 kilometer 拼写属属性拼写错误非 gen-control 契约漂移，故不纳入）
      - Skill: `document-audit-prompt`
- [x] Add: `design-doc-audit-prompt.md` 增「state-machine.md 章节覆盖矩阵」维度
      - 检查目标域所有「状态承载实体」（含子状态机如 CAPA/拣货单/排班分配/银行文件/InvoicePlan）是否在 state-machine.md 有独立章节，散落在其他 owner doc/plan 的子状态机登记为缺口
      - 引用 P2-MA2-037/043/045/047/052/053/056/059/062/063/065/070/071 跨 13 域同型证据
      - Skill: `document-audit-prompt`

Exit Criteria:

- [x] 3 个技能文件各自新增段落存在且引用证据编号
- [x] 新增段落不重复 behavioral-failure-mode-scan 的详细 grep 程式（本处是设计层/审计维度提示，详细程式交叉引用）

### Phase 4 - 扩展 README §已知失败模式 + 关联文档指针

Status: completed
Targets: `docs/skills/README.md`
Skill: `document-audit-prompt`

- Item Types: `Add`
- Prereqs: Phase 1-3 完成（已知失败模式清单引用 behavioral-failure-mode-scan）

- [x] Add: `docs/skills/README.md §已知失败模式`（当前 :123-132 共 8 项）扩到 13 项，补 lessons 07-11 的 5 个系统性行为失败模式（每项 1 行 + lesson 指针）
      - lesson 07 Compliance 基线漂移（计划新增 daoFor/import 后 closure 未重跑 checker）
      - lesson 08 closure-pending（声称 completed 缺独立 closure audit）
      - lesson 09 业财过账吞异常悬挂（catch 吞咽 + posted 悬挂）
      - lesson 10 dict 死状态（dict 值无 writer 不可达）
      - lesson 11 arm-index 状态不回填（roadmap done 但 arm-index 仍 todo）
      - Skill: `document-audit-prompt`
- [x] Add: `multi-dimensional-audit-prompt.md:32` 与 `open-ended-audit-prompt.md:31` 的「搜索加权」段，把「已知失败模式」从泛指改为强制引用 README §已知失败模式扩展后清单（13 项）+ lessons 索引
      - Skill: `document-audit-prompt`
- [x] Add: `docs/skills/README.md` 新增「关联文档」节，指向 `docs/audits/00-audit-execution-guide.md`（修路由断点）+ `docs/audits/requirement-compliance-methodology.md`（methodology 主体）
      - Skill: `document-audit-prompt`

Exit Criteria:

- [x] README §已知失败模式含 13 项（8 原 + 5 新），每项有 lesson 指针
- [x] multi-dim / open-ended 的搜索加权段显式引用 README §已知失败模式清单
- [x] README 含「关联文档」节指向 00-audit-execution-guide.md 与 methodology

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（ses_02fb73998ffeDCr3goRSQMCKQc）——2 项阻塞：(B-1) Phase 3 援引「multi-dimensional-audit-prompt.md 维度 6（gen-control 内联脚本契约）」不存在（实际维度 6 是「路由和技能选择正确性」，7 维度均通用；view.xml drift 维度是审计时临时适配从未落盘）；(B-2) Current Baseline「audit-remediation-roadmap-authoring-prompt.md:6 自引用为先例」是捏造引用（该行实为保护区域授权，全 skills 目录 grep methodology 零命中）。改进建议采纳：技能数 25→24（含 README）、§3 拆 §3.1 调度链/§3.2 守卫子节明边界、P2-MA4-018 不纳入注记。
- Independent draft review iteration 2: `accept`（ses_02fb303a0ffeUmGJc4byLxa27f）——两项阻塞经实时仓库逐项核实真已修正（B-1 维度锚点 / B-2 假引用），未引入新阻塞；技能数 24 / §3.1-§3.2 边界 / multi-dim:30 引用 / lessons 07-11 / P1-RC-042 全部核实为真；反松弛词干净、Closure Gates 完整。采纳 2 项非阻塞改进：(N-1) Decision 补 multi-dim 边界声明；(N-2) Exit Criteria §3 措辞从「grep 程式」改为「调度链拓扑分析程式 + 守卫清单核查」。计划进入 active。

## Closure Gates

> 文档计划（仅 docs/skills/ 与 docs/plans/ 变更，零生产代码/契约/ORM）。删除 build/test/lint 验证门控。验证 = 文档审查（结构完整性 + 交叉引用有效性 + 无冗余/冲突）。

- [x] 范围内行为完成（G1-G6 全部交付）
- [x] 相关文档对齐（README 注册表/入门列表/§已知失败模式/关联文档节一致）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录（iteration 1 起，反复至 consensus）
- [x] 文本一致性已验证：Plan Status / 阶段 Status / Exit Criteria / Closure Gates 一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中
- [x] 反向验证：新/改技能不与现有技能冗余（按 README:188-197 反模式表 + G6）

## Deferred But Adjudicated

### checker R2d 扫描范围扩展（daoFor ErpFin/ErpInv/ErpPur）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 属工具增强（checker 脚本）非提示词；P2-MA4-011 已登记 successor；本计划范围是技能库覆盖缺口，工具增强归独立任务
- Successor Required: yes（触发条件：daoFor 跨域防护需自动化时）

### mission-driver closure gate 强制钩子（基线漂移/closure-pending/arm-index 回填的执行纪律）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 这些是执行纪律问题非技能缺口（NG5）；技能已存在（closure-pending-detection / compliance-baseline-drift-adjudication），强化 mission-driver 钩子属工具层
- Successor Required: yes（触发条件：mission-driver 下一轮重构时）

## Closure

Status Note: 执行完毕（4 Phase 全部交付，Plan Status: completed）。G1-G6 + 8 项自洽 Closure Gates 全部勾选（含本次独立结束审计回填的 2 项）。文档审查已自验：2 新技能文件存在（behavioral-failure-mode-scan-prompt.md 231 行 / requirement-compliance-audit-prompt.md 45 行薄壳），README 注册表（26 行技能 = 26 个 .md 文件，无孤儿）+ 入门列表 + §已知失败模式（13 项）+ 关联文档节一致，3 强化技能各含证据编号引用，multi-dim/open-ended 搜索加权段显式引用 13 项清单。G6 反向冗余验证：behavioral-failure-mode-scan 与 4 邻接技能的边界声明齐全（state-machine-business-review / closure-audit / nop-platform-conformance / multi-dimensional-audit），requirement-compliance-audit 明确为 methodology 薄壳非全文搬运，3 强化技能新增段落交叉引用 behavioral-failure-mode-scan §2 而非重复 grep 程式。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（fresh session，不重用执行者上下文），由 mission-driver `2026-08-04-224309-mission-driver` 调度执行 closure-audit。
- Audit Method: 对照 plan guide + 实时仓库逐项语义核验（非盲信 `[x]` 标记）。
- Phase 1 落地核实：`docs/skills/behavioral-failure-mode-scan-prompt.md` 存在（18724 字节），§1（B1 业财过账吞异常悬挂，grep 程式 + 4 信号 + 决策树 + 反模式自检表 + 证据 P1-MA2-032/048/060/068/074/080 + P1-MA4-001/004/007/010/013/020）/ §2（B2 dict 死状态，证据 lesson 10 表格列）/ §3.1（调度链拓扑，引用 P1-RC-042 + batch-scheduling-audit）/ §3.2（守卫散点，引用 MA6 三报告）四节齐全 + 文件头四列元数据 + 与 4 邻接技能边界声明。无 hollow（grep 无 TODO/FIXME/return null/占位）。
- Phase 2 落地核实：`docs/skills/requirement-compliance-audit-prompt.md` 薄壳（45 行 < 50 目标）主体指向 methodology、零 §1-§10 内容重复、声明冲突时以 methodology 为准；`docs/skills/README.md:53` configuration-audit-prompt 注册（修孤儿）+ `:54` behavioral-failure-mode-scan + `:55` requirement-compliance-audit 注册；`:82-84` 入门列表三行补登。
- Phase 3 落地核实：`state-machine-business-review-prompt.md:71-74` 增「代码层 dict 可达性核查」交叉引用 behavioral-failure-mode-scan §2（不重复 grep 程式）；`multi-dimensional-audit-prompt.md:30/32-45` 新增项目特定维度「view.xml gen-control 契约（badge 调色板 vs dict 真值）」+ 证据 P2-MA4-014/015/016/017/019/020 + P2-MA4-018 排除注记；`design-doc-audit-prompt.md:101-105` 增「state-machine.md 章节覆盖矩阵」+ 跨 13 域证据 P2-MA2-037/043/045/047/052/053/056/059/062/063/065/070/071。
- Phase 4 落地核实：`docs/skills/README.md:148-154` §已知失败模式扩到 13 项（9-13 为 lessons 07-11 系统性行为失败模式，每项含 lesson 指针）；`:86-92` 新增「关联文档」节指向 `00-audit-execution-guide.md` + `requirement-compliance-methodology.md` + lessons（修路由断点）；`multi-dimensional-audit-prompt.md:47` + `open-ended-audit-prompt.md:31` 搜索加权段显式引用 README §已知失败模式 13 项清单 + lessons 索引。
- Anti-Hollow 核实：新技能已被 README 注册表 + 入门列表 + 入门 §已知失败模式 + 3 强化技能交叉引用路由，运行时可被独立子代理照做执行（grep 程式 + 决策树 + 反模式自检表可复制）；无空函数体 / return null / 吞咽异常 / 注册但不可达组件。
- Five-point consistency：Plan Status: completed / 4 Phase Status 均 completed / 4 Phase Exit Criteria 全 `[x]` / 8 Closure Gates 全 `[x]`（本次回填最后 2 项）/ Closure 证据非占位——全部一致。
- Deferred honesty：2 项 Deferred（checker R2d 扫描范围扩展 / mission-driver closure gate 强制钩子）均Classification=`out-of-scope improvement`、属工具层非提示词、Successor Required=yes 含触发条件；NG1/NG5/NG6 已显式排除——无范围内缺陷伪装为 deferred。
- Docs sync：`docs/logs/2026/08-05.md:3-12` 含本计划日志条目（Phase 1-4 交付摘要 + 验证状态）；本计划纯 docs/skills 方法论层（NG4 零生产代码/契约/ORM），无需更新 `docs/architecture/`。
- 参考文档存在性核实：`docs/audits/requirement-compliance-methodology.md`（32524 字节）/ `docs/audits/00-audit-execution-guide.md`（3567 字节）/ lessons 07/08/09/10/11 / `docs/audits/2026-07-04-0000-batch-scheduling-audit.md` 全部存在，无悬挂引用。
- Closure 裁决：`approved`（所有 Phase 落地、无 hollow、五点一致、deferred 诚实、docs 已同步）。

Follow-up:

- checker R2d 扫描范围扩展（见 Deferred But Adjudicated）
- mission-driver closure gate 强制钩子（见 Deferred But Adjudicated）
