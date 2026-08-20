# 开放式审计报告 — mission `requirement-compliance`

> Audit Status: planned
> Audit Type: open-ended
> Mission: requirement-compliance

- 审计对象：mission `requirement-compliance` 的**完整上下文**（M0 → MA1-MA4 → MR0/MR1[RC-R1.1-R1.89] → MV → MG 全链 + roadmap/arm-index/methodology/lessons 证据面 + 实仓代码/守卫/测试基线 + 同批次结束审计产物[两份审计报告、两份 active 修复计划、roadmap Follow-up Backlog]），不是单一计划或单一工件。
- 审计人：独立子代理（fresh session，非 mission 执行者，未参与任何 MR1/MV/MG/审计批次工作）。
- Skill：`docs/skills/open-ended-audit-prompt.md`，前置注入 `docs/skills/README.md §项目定制化层`（保护区域 / 验证命令 / 命名约定 / 13 项已知失败模式）+ `docs/audits/requirement-compliance-methodology.md`（mission 审计契约）。
- 证据基线：HEAD = `ac92d3091`（2026-08-20）；工作树 = roadmap Follow-up Backlog 追加（未提交）+ 4 份 untracked docs（multi/open 两份审计 + `2026-08-20-2052-1/2` 两份 active 修复计划），零代码变更——本审计对同路径 `planned` 草稿**逐条独立复核后定稿**（fresh session，非草稿作者），全部发现经实仓重验，未照抄草稿证据。定稿时点复核命令全数重跑：checker 19 规则零漂移、arm-index 87 条 P1-RC 恰 2 条无 done（015/092）、`ErpInvLandedCostProcessor:393-394 lock / :397-412 非锁 check` + ORM `:1353` 仅 `UK(code,orgId)` + 全部 application.yaml 零 isolation 配置、ApproveProcessor `:42/:66/:68/:69` 时序、cs pom `:61` crm-dao vs `docs/architecture/` 0 命中、`LocalDateTime/LocalDate.now()` 生产 sweep 恰 2 站点（b2b `:220` / fin `:117`，同文件 `:128` 已用 CoreMetrics）、recon helper `:99-103` WARN-only catch、aps `UNSCHEDULABLE` writer ×4、hr `:55 tryPostAccrual`、`docs/logs/2026/08-20.md` 无本批次条目（剩余未知数③仍实）——与草稿主张逐项一致，零新增发现。

## 0. 核验方法与证据基线（本审计独立复跑/重读）

| 核验项 | 命令/路径 | 结果 |
|---|---|---|
| Compliance checker 复跑（lesson 07） | `bash docs/audits/nop-compliance-checker.sh` | 全 19 规则 **actual == baseline 零漂移**（R1d=14 / R2a=34 / R2b=237 / R2c=1507 / R2d=38 / R3=5 / R6=2 / R10=12 / R12a/b/c=70/66/41，与 `compliance-baseline.md` 机器可读块及 `known-good-baselines.md` 2026-08-20 MV 行逐行一致；本审计不以退出码作门控依据） |
| arm-index P1 闭合状态（lesson 11） | `grep '^| \`P1-RC-'` 逐行提取 | **87 条 P1-RC 行，恰 2 条无 done 标记（P1-RC-015 / P1-RC-092，见 F1/F3）**，其余 85 条含 done 标记（格式两种：修复状态列 `done (RC-R1.x…)` + 行内 `【修复状态：done…】`）；`P0-RC-*` 0 条 |
| 实仓代码对抗性 grep（AGENTS.md 平台规则 + 已知失败模式 3-8） | `@Inject private` / `extends RuntimeException` / 字典值 `==` 比较 / `dao().updateEntity` / `System.currentTimeMillis()` | 全零命中（R4/R5/R7 checker 同证；新代码 aps/hr/cs 域 `getStatus() ==` 命中均为 null 检查非字典比较） |
| 时间源辅助类漂移（已知失败模式 #4 全变体） | `LocalDateTime.now()` / `LocalDate.now()` / `Instant.now()` 全生产代码 | **2 处命中（F4）**——checker R7 模式仅 `System\.currentTimeMillis`（checker 脚本 :254），对变体结构性盲 |
| B1 业财吞异常（lesson 09） | Posting dispatcher catch 块 + RC-R1.89 新增 hr 计提链 | 引擎层（`recordPostFailure` → `ErpFinPostingException` 工作台 + sweep 重试 + MANUAL 升级）+ dispatcher 层 G3 告警双重收口；`ErpHrSalaryPostApprovalProcessor:55` `postingDispatcher.tryPostAccrual` 接线实存（RC-R1.89 声明核验为真）；非引擎 reverse 路径 1 处观测缺口（F5） |
| dict 死状态（lesson 10，最近新增抽查） | aps `operation-order-status` 新三态 | `UNSCHEDULABLE` 活跃 writer ×4（`ErpApsSchedulingEngine:108/139/187/221`）+ HOLD/ON_HOLD 经 unhold mutation（`ErpApsOperationOrderBizModel:191`、StateMachine:131）——无死状态复发 |
| closure-pending（lesson 08，最近计划抽查） | RC-R1.78/79/80/86-89 + MV + MG 计划 | 独立草案审查（含 BLOCKER→修订迭代）+ 独立结束审计 PASS 证据 + session id 落盘齐备；MV V.3 曾检出的 round-2 缺口已补跑（lesson 8 范式实际运转） |
| P1-RC-092 实仓复核 | `ErpInvLandedCostProcessor` + `ErpInvLandedCostApproveProcessor` + `app-erp-inventory.orm.xml` + 生产 application.yaml + git log | 三修复选项**零落地**（见 F1 证据链，本审计逐项重验） |
| 同批次审计产物核验 | multi-audit 报告 + 两份 2052 计划 + roadmap Follow-up Backlog | 计划草案审查记录（含 2052-2 的 1 MAJOR→修订两轮）齐备；Follow-up Backlog 5 行与两份审计发现一一对应且误报撤回已同步 |

## 1. 发现（按严重性排序）

### F1 — [P1] P1-RC-092 被 RC-R1.47「已实现确认」虚假关闭：MySQL-RR TOCTOU 缺陷未修而 roadmap 标 done，且 arm-index 状态未回填（lesson 11 复发）

**一句话理由**：P1 级会计正确性风险（MySQL-RR 下到岸成本并发重复分摊）被一个仅重述 finding 已知前提的核实关闭——未实现、也未驳斥任一修复选项，mission「全路线图闭合」声明在该行上实质不成立，属 Q4=(a)（P1 必须实现、禁方案 B 无例外）契约违约。

**证据链**（本审计独立重验，行号为写时实测）：

1. **finding 本体**（`docs/audits/arm-index.md:298` P1-RC-092）：MySQL InnoDB REPEATABLE_READ（MySQL 默认隔离级别）下，`validateNotAlreadyAllocated` 对 `erp_inv_landed_cost` 的**非锁 SELECT** 读陈旧 MVCC 快照；`SELECT FOR UPDATE` 锁的是 `erp_pur_receive` 行（不同表），不能刷新读视图 → TOCTOU 重新打开 → 并发重复分摊（StockBalance 双计成本调整）。finding 给出三个修复方向：①部署侧强制 READ_COMMITTED ②sibling 行锁定读 ③`(receiveId, approveStatus)` UK 兜底，标注「MR1 + ask-first（锁/数据安全类）」。
2. **Blocker 修正先例**（`docs/plans/2026-08-06-1517-2-...md:81`，本审计重读）：该计划独立结束审计已把「锁+check 原子化成立」的原结论**推翻并修正**为「跨方言不一致（MySQL-RR 退化）」——即「锁 + check 已足够」论断在 2026-08-06 已被本 mission 自己的审计链证伪，RC-R1.47 的闭包理由与该在盘证据直接矛盾。
3. **闭包声明**（`docs/backlog/requirement-compliance-roadmap.md:439` RC-R1.47 + 头部 `:52` C 类摘要行，2026-08-12）：「pessimistic lock `ErpInvLandedCostProcessor:388-407` + app 级唯一性检查 `validateNotAlreadyAllocated:392-407` 已落地，UK 非必需（approveStatus 是状态字段非自然键）」→ done ✅。该核实重新确认的恰是 finding **已承认存在**的两个机制（锁 + check），未触及「check 非锁读陈旧快照」这一核心主张；「UK 非必需」仅回应选项③可行性，①②未处置，无 §4 式裁决记录。
4. **实仓复核（本审计）**：
   - `ErpInvLandedCostProcessor.java` `lockReceiveForAllocation`（`:391-395` 附近）= `ormTemplate.lock(receive)`（锁 receive 行，原形态）；`validateNotAlreadyAllocated`（`:397-412`）= `dao.findAllByQuery` 非锁读（原形态）——选项②未落地；
   - `app-erp-inventory.orm.xml:1353` 仅 `UK_INV_LANDED_COST_CODE_ORG(code,orgId)`，无 `(receiveId, approveStatus)` UK——选项③未落地；
   - 全部生产 `application.yaml` 零 `transactionIsolation` / `READ-COMMITTED`——选项①未落地；
   - `git log` 该文件最后实质修改 = 2026-08-13 状态机计划（`e5d122b38`，M4.34），闭包裁决（2026-08-12）前后均无修复 commit。
5. **状态未回填**：arm-index P1-RC-092 行「修复状态」列仍为 `todo（修复方向 MR1 评估…）`，仅末列追注「【R1.0 展开归属】RC-R1.47」——违反 methodology §11.3 四点回写与 lesson 11。MV V.3 任务级结束审计「89/89 五点一致」未捕获（Tier 1 会计核心机械提取 9 行未含 R1.47，Tier 2 37% 抽样未命中）。
6. **交叉引用**：同批次多维审计 `docs/audits/2026-08-17-2125-multi-audit-requirement-compliance.md` F1 独立得出同结论。本审计按 §7 规则裁决为**复用 P1-RC-092**（同根因同控制点），不新建 finding ID。
7. **修复状态（本审计时点）**：修复计划 `docs/plans/2026-08-20-2052-1-inv-landed-cost-mysql-rr-toctou-remediation.md` 已转 active（Phase 1 三选项裁决 + §5 锁/数据安全类双独立子 agent 批准门控 + Phase 2 守卫实现 + Phase 3 四点回写捆绑 P1-RC-015 规范化；草案独立审查 `acceptable as-is` 已记录；Current Baseline 行号经本审计抽验准确——ApproveProcessor `:42/:66/:68` 与实仓一致）——但全部 Phase 仍 planned、零执行。**发现 open，义务 = 执行并闭合该计划**。

**影响面**：H2（当前全绿验证基线）/ PG / MySQL-RC 部署行为正确；**MySQL 默认 RR 部署存在重复分摊窗口**（到岸成本重复计入 → 存货成本错报，会计正确性类）。无当前基线上的活跃数据破坏（沿袭原 P1 非 P0 四项理由：窄触发 / 下游 version 守护 / 已登记并发敏感点 / 期末对账可发现），故 P1 非 P0。

**修复义务（MUST fix）**：①执行既有 fix plan `2026-08-20-2052-1`（三选项择一或组合；锁/check 逻辑属 §5 数据安全保护类，计划内双批准门控须真实履行）；②roadmap `:439` + 头部 `:52` C 类措辞改写为真实裁决；③arm-index P1-RC-092 修复状态回填。若裁决「不修」，唯一合法出口 = 需求本身不合理经人工批准改 product-scope（Q4=(a)），不得以 C 类「已实现」重述关闭。

### F2 — [P1] data-dependency-matrix §2.4 缺失 cs-service → crm-dao Java 层边（RC-R1.65 落地未登记，架构契约漂移；复用 multi 审计 F2）

**一句话理由**：MR1 新增的真实 compile 级跨域依赖（cs→crm）未登记入模块 DAG 权威文档，而本 mission 全部其余 8 条同类边均已登记——架构 owner doc 与实仓 pom 出现实质性契约漂移。

**证据链**（本审计独立重验）：

1. `module-cs/erp-cs-service/pom.xml:61` `app-erp-crm-dao` compile scope（RC-R1.65，2026-08-18 落地，工单创建自动分配候选池，pom 注释自述 cs→crm 单向 R、DAG 无环）。
2. `docs/architecture/data-dependency-matrix.md` + `docs/architecture/module-boundaries.md` grep `crm-dao` = **双双 0 命中**（本审计复跑）。
3. 同 mission 全部同类先例均登记（cs→qa、pur→drp、pur→ct、sal→ct、assets→mnt、mfg↔mnt、mnt→qa、aps→notify、log→sal、drp 四边）——RC-R1.65 是唯一漏登的 Java 层新边；roadmap `:457` RC-R1.65 done 行无 matrix 登记短语（对比 R1.68/76/77/78/81/85/86 行内显式「matrix §2.4 登记」）。
4. 修复计划 `docs/plans/2026-08-20-2052-2-arch-matrix-cs-crm-edge-registration.md` 已转 active（两轮独立草案审查，1 MAJOR 已修——「@Nullable 注入容错」纠正为实仓的普通 `@Inject` + try/catch 失败隔离，本审计抽验 `TicketAssignResolver.java:34-38` 与修订后行文一致），Phase 1 仍 planned。**发现 open，义务 = 执行该计划**。

**影响面**：无行为影响、DAG 仍无环；但 §2.4 是跨模块审计与仓库拆分决策的权威输入，缺失行使该依赖对下游架构工作不可见。一行登记修复，属「真实契约漂移、应修非阻断」，P1。

### F3 — [P2] arm-index P1-RC-015 修复状态标记非规范：「修复落地（RC-R1.8…）」追加式回填，缺「done」状态词

**一句话理由**：修复内容完整（roadmap RC-R1.8 done + plan + 8 组测试证据齐备），仅回填格式与 85 条兄弟行不一致，机器/人工按状态词提取会误读为未闭合——cosmetic 级索引漂移。

**证据**（本审计重验）：`docs/audits/arm-index.md:154` 行尾「…零回归全绿 **【R1.0 展开归属】RC-R1.8**」，无 done 标记（本审计全量计数确认其为 87 条 P1-RC 中仅有的 2 条无 done 行之一）；修复内容以「；修复落地（RC-R1.8，plan …）」形式内嵌于原 todo 段落。交叉引用：multi 审计 F5 同结论。处置：已捆绑入 plan `2026-08-20-2052-1` Phase 3（不单独立项），执行 F1 时一并规范化。

### F4 — [P2] 生产代码时间源漂移 ×2（P2-MA1-019 家族复发）+ checker R7 守卫盲区

**一句话理由**：已知失败模式 #4 明文禁止生产代码用 `LocalDateTime.now()`/`LocalDate.now()`（须 CoreMetrics 系列以支持时间可控测试），MR1 期间新增 2 处违规站点，且 R7 守卫正则仅覆盖 `System.currentTimeMillis()` 一个变体，结构性无法拦截复发——低影响（均为 job 边界墙钟读）但属守卫可修复的真实漏洞。

**证据**（本审计独立 grep，恰 2 处生产命中）：

1. `module-b2b/erp-b2b-service/.../job/ErpB2bOnboardingMonitorJob.java:220` — `countEdiDocs` 查询上界 `LocalDateTime.now()`；**同文件 `:128` 已用 `CoreMetrics.currentDate()`**（同一 job 内时间源混用，反差自证为漂移非约定）。RC-R1.36 批次引入。
2. `module-finance/erp-fin-service/.../bankrecon/ErpFinBankReconAutoReverseHelper.java:117` — 候选扫描下界 `LocalDate.now().withDayOfMonth(1)`。RC-R1.2 批次引入。
3. 家族先例：`P2-MA1-019`（arm-index）曾登记同型站点（现仓已修复）——本 2 处为同族**新控制点复发**，建议并入 P2-MA1-019 家族追加注记而非新建 RC 编号（裁决留 remediation）。
4. 守卫盲区：`docs/audits/nop-compliance-checker.sh:254` R7 模式 = `System\.currentTimeMillis` 单一变体；R7 实测恒 0 造成「时间源已收敛」假象。

**处置建议**（已登记 roadmap Follow-up Backlog）：①两站点改 `CoreMetrics` 派生（纯代码预授权）；②独立小计划扩 R7 模式集（`LocalDate\.now\(\)|LocalDateTime\.now\(\)|Instant\.now\(\)`）并按基线裁决流程登记新基线值（预期 0 起步）。

### F5 — [P2] `ErpFinBankReconAutoReverseHelper.reverseOne` 持久失败通道仅 WARN 日志，无告警派发/异常工作台升级（B1 邻位观测缺口，watch-only）

**一句话理由**：单条红冲失败（如启用期末结账部署下撞 CLOSED 期间——其自身 javadoc 引用的场景）逐日重试逐日 WARN，无 `ErpSysNotification` 派发亦无 `ErpFinPostingException` 工作台条目，偏离 R1.16 G3「posted 悬挂必须可感知」的既定告警范式——但候选保持 POSTED 状态可见 + 每次运行自动重试自愈 + 行为经 RC-R1.2 审计计划显式裁决，故 watch-only。

**证据**（本审计重读 `:99-103`）：`catch (Exception e) → LOG.warn + return false`；对照 R1.16 范式（`SalaryPostingDispatcher.dispatchFailureAlert`、`DepreciationPostingDispatcher.dispatchFailureAlert` 同型）。该 helper 走 `BankReconciliationBuilder.reverse`（非 PostingEvent 引擎路径），不经 `recordPostFailure` 引擎层记录——恰是 B1 扫描程式的盲缝（引擎层收口不覆盖非引擎 reverse 路径）。处置：后续批次给 catch 分支补 `IErpSysNotificationBiz` 派发（镜像 A4.2.9 `BatchGenealogyWriter` 同型范式），已登记 Follow-up Backlog，不强制立项。

## 2. 正面确认（对抗性搜索后仍成立的项）

- **Lesson 07（基线漂移）**：本审计复跑 checker 19 规则零漂移，与 `compliance-baseline.md` 机器可读块及 `known-good-baselines.md` 2026-08-20 行逐行一致。
- **Lesson 08（closure-pending）**：最近批次计划（RC-R1.78/79/80/86-89、MV、MG）草案审查含真实 BLOCKER→修订迭代 + 独立结束审计 PASS + session id 落盘，无自我审计；MV V.3 对 round-2 缺口的补跑证明该门控实际生效。
- **Lesson 09（B1 吞异常）**：PostingEvent 路径失败可观测性在引擎层 + dispatcher 层双重收口；RC-R1.89 hr 计提链接线（`ErpHrSalaryPostApprovalProcessor:55`）+ 去重守卫 + per-stage 告警 + 幂等 posted 收敛实存，实现质量高于范式基线。
- **Lesson 10（dict 死状态）**：最近新增 dict（aps 工序三态等）抽查全部有活跃 writer，无复发。
- **平台约定**：`@Inject private`、`extends RuntimeException`、字典值 `==` 比较、`dao().updateEntity` 越权、`System.currentTimeMillis()` 全零命中。
- **MV/MG 证据有效性**：全量验证跑于 `957888ffc`，其后 2 commit 仅 docs（`git diff --name-only` 复核），3789/0/0/1 与 checker 零漂移证据对当前 HEAD 仍有效；MG 产物（lessons 12-15 + methodology §11 + project-context 失败模式 4→7）实存。
- **同批次审计产物质量**：两份 2052 修复计划的 Current Baseline 行号/机制描述经本审计抽验准确（ApproveProcessor `:42/:66/:68`、`TicketAssignResolver` 普通 `@Inject`、`TestErpInvLandedCostReceiveMutex` 实存）；multi 审计对草稿误报（docStatus 死映射）的主动撤回 + Follow-up Backlog 同步修正，是独立复核纪律的正面样例。
- **保护区域纪律**：RC-R1.89 / R1.44 / R1.49 / R1.81 / R1.87 / R1.79-D4 双 Reviewer APPROVE（session id 在盘）；A/B/C 批量裁决可追溯 2026-08-12 用户裁决原文。

## 3. 反窄化自检

本审计未局限于单一计划/工件的内部声明核验：覆盖 roadmap↔arm-index↔plan↔代码↔守卫工具五方一致性（F1/F3）、架构权威文档与实仓 pom 的契约差（F2）、守卫工具与其声称覆盖的规则面之差（F4）、已收口失败模式在新代码路径上的盲缝（F5）、以及全仓约定级 grep 与最近新增代码（aps/hr/cs/b2b）的失败模式抽查。搜索加权按 13 项已知失败模式分配（lesson 07/08/09/10/11 各有专项核验），但未让既有清单收窄维度——F4（守卫正则盲区）与 F5（非引擎 reverse 路径）均为清单外新控制点；对「C 类/已实现确认」型裁决另做了第二例抽查（RC-R1.55 FIFO delta 层，multi 审计经 A4.1.15/16 复核有运行时证据支撑，本审计未重验，登记为剩余未知数）。已知局限：未重跑 mvn 全量测试（复用 MV V.1 fresh 证据 + 其后零代码 commit 的有效性推定）；dict 死状态为抽查非全量（全量归 `behavioral-failure-mode-scan` 专项）。

## 4. 与 arm-index / 同批次审计交叉去重声明

- F1 = **复用** P1-RC-092（同根因同控制点），交叉引用 multi 审计 F1，不新建编号。
- F2 = 交叉引用 multi 审计 F2（本审计独立重验 pom/matrix/计划后确认），架构矩阵登记义务归属该发现，不新建 RC 编号。
- F3 = **复用** P1-RC-015 行的格式缺陷，交叉引用 multi 审计 F5，不新建。
- F4 = 与 P2-MA1-019 **同根因不同站点**（复发实例 + 守卫盲区维度），建议家族追加注记；是否升独立 RC 编号留 remediation 裁决。
- F5 = 新控制点（非引擎 reverse 路径的失败可观测性），grep arm-index 银行对账红冲/告警派生同控制点零命中；随 Follow-up Backlog watch-only 登记处置。
- 事实性勘误（不影响任何发现定级）：multi 审计 F3 所列 successVals 17 值中第 9 位实为 `RETRIED`（报告写作 `RETRIEVED`）；计数 17 与「仅 CLOSED 命中、RESOLVED 未入」结论不变。

## 5. 过程纪律自检

- [x] checker 门控核查：本报告产出后运行 `bash docs/audits/nop-compliance-checker.sh`，actual == baseline 零漂移（§0 表）；本审计未修改生产代码，无回归风险；不以脚本退出码作门控依据（纯 reporter，真门控在 CI workflow）。
- [x] closure-audit 独立性声明：本审计由独立子代理 fresh session 执行，未参与 mission 任何执行环节；对同路径 `planned` 草稿逐条独立复核后定稿（非照抄）。
- [x] 与 arm-index 交叉去重声明：全部 finding 已按 §7 规则 grep 比对并给出「复用 or 新增」裁决（§4），无未经比对直接新建的 finding。
- [x] 真相源冻结：本审计未修改 product-scope / use-cases / owner doc 契约段 / arm-index / roadmap（纯只读核验 + 本报告落盘定稿）。

## 6. 结论

**needs revision** —— 无 P0；2 项 P1（F1：P1-RC-092 虚假闭包 + 状态回填缺失；F2：架构矩阵 cs→crm 边漂移）须经修订收口后方可接受 mission「全路线图闭合」声明。需修订对象与路径：

1. **F1**：`docs/backlog/requirement-compliance-roadmap.md:439` + `:52`（闭包理由失真措辞）、`docs/audits/arm-index.md:298`（状态回填）、`module-inventory/.../ErpInvLandedCostProcessor.java`（MySQL-RR TOCTOU 实际处置，§5 数据安全类双批准门控）——执行计划 `docs/plans/2026-08-20-2052-1`（Phase 3 捆绑 arm-index P1-RC-015 规范化）。
2. **F2**：`docs/architecture/data-dependency-matrix.md §2.4` cs→crm 边登记——执行计划 `docs/plans/2026-08-20-2052-2`。

P2 项（F3/F4/F5）登记不强制，已归 roadmap `## Follow-up Backlog`（F3 另捆绑入 plan 2052-1 Phase 3）。

**剩余未知数（非阻塞警惕项）**：①「C 类已实现确认」裁决共 2 项，本审计深验 R1.47（F1）、抽查 R1.55 未重验——建议后续里程碑级闭合审计对「C 类/已实现确认」行做全量而非抽样；②R7 守卫盲区（F4）意味着时间源约定当前无机器门控，修复前复发只能靠人工审查拦截；③本结束审计批次（两份审计 + 两份计划 + Follow-up Backlog）的 `docs/logs/2026/08-20.md` 条目与 git 提交尚未落盘（批次进行中的预期中间态）——批次收口时须一并补齐，且 roadmap Follow-up Backlog 前言「均已转 planned/归档处置」措辞应随两份审计定稿（`open`）同步修正；④dict 死状态全量扫描与 `TestErpMdSkuServices` 偶发并行干扰（08-18 日志登记，复现二次再立档）仍开放。

<AI_STEP_RESULT>issues</AI_STEP_RESULT>
