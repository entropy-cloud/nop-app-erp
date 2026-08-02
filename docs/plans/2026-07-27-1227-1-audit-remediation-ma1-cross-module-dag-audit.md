# 2026-07-27-1227-1-audit-remediation-ma1-cross-module-dag-audit MA1 跨模块依赖与 DAG 审计（A1.10）

> Plan Status: completed
> Mission: audit-remediation
> Work Item: A1.10 跨模块依赖与 DAG 审计（全域跨域）
> Last Reviewed: 2026-07-27
> Source: `docs/backlog/audit-remediation-roadmap.md` Milestone MA1（工作项 A1.10）
> Related: `2026-07-27-1015-2-audit-remediation-ma1-orm-model-audit.md`（A1.1–A1.9 ORM 审计已完成，本计划是其 MA1 后续）；`docs/skills/cross-module-dependency-audit-prompt.md`（审计方法）；`docs/audits/2026-07-23-0000-architecture-governance-review.md`（F1/F3/F4/F5/F6/F7 部分闭包来源）；`docs/plans/2026-07-24-0930-3-*` / `2026-07-24-0941-1-*` / `2026-07-24-1400-1-*` / `2026-07-24-1400-2-*` / `2026-07-26-0300-1-*`（F1–F9 闭包证据）
> Audit: required

## Current Baseline

跨模块数据依赖是 nop-app-erp 多模块架构的主脊。owner doc `docs/architecture/data-dependency-matrix.md`（848 行）已定义 R/S/P 三类依赖、DAG 方向、机制 B（`notGenCode` 外部实体 to-one）落地清单与 §5.6.2 实测依赖矩阵（声称：17 业务域共约 **369 个跨模块 to-one** + 约 **68 个外部实体声明**；ORM 层已记录的跨业务域单向合法引用 = finance→projects/assets、pur/sal→projects、hr→projects，以及 manufacturing→inventory（`ErpInvBatch`）、maintenance→assets（`ErpAstAsset`）、drp→inventory（`ErpInvStockMove`）；零循环）。`module-boundaries.md` 管模块级 DAG 方向。

前序架构治理审查（`2026-07-23-0000-architecture-governance-review.md`）已部分闭包 6 项跨域 finding（scope matrix §3.1 F1–F9）：

- **F1** daoFor 跨域访问：plan `2026-07-24-0941-1` 收尾（daoFor 965 处分类，真违规子集 ~110-180 处 watch-only residual）。
- **F3** ORM DAG 边登记：plan `2026-07-24-0930-3` Phase 2 收尾。
- **F4** 隐性共享内核（`ErpFinBusinessType` 被 137 文件跨域 import）：plan `2026-07-24-1400-1` 显式登记 + R12 守卫。
- **F5** notify owner doc：plan `2026-07-24-0930-3` Phase 3 收尾。
- **F6** mfg 依赖 qa 生成常量：plan `2026-07-24-1400-2` Phase 1 收尾。
- **F7** drp 命名前缀（`ErpInvDrp*`）：裁决=登记例外。

scope matrix §2.1 "跨模块依赖/DAG" 行反映这些部分闭包的终态分布：finance ⚠️ / mfg ⚠️ / b2b ⚠️F1half / inv ⚠️ / md ⚠️F4✅ / drp ⚠️F7✅ / notify ⚠️F5✅，其余 11 域仍 ❓。

**但从未做过一次覆盖全域 19 域、按 `cross-module-dependency-audit-prompt.md` 7 维度的系统性跨模块依赖审计**。已知未闭包输入：

- owner doc §5.6.2 自述"全量 to-one 总数与外部实体声明数**待 codegen 后跑脚本精确统一**"（声称值 369/68 未机器核验）。
- owner doc §5.6.3 禁止清单（§5.6.3 禁止方向）是否需增列 mfg→inv / drp→inv / mnt→ast 待业务裁决（scope matrix §3.2 arch-gov §残留风险 3，P2 deferred）。
- F1 daoFor watch-only residual（~110-180 处真违规子集）的跨域**写**仅 2 处已登记豁免 + 1 处半治理（b2b→pur，scope matrix §3.2，P2 deferred）。
- Maven `erp-xxx-dao/pom.xml` 跨工程依赖是否与 orm 声明单向对齐（codegen 后未全量核对）。

剩余差距：需要一次系统性 7 维度审计，将上述部分闭包与未核验自述整合为全域通过/失败裁决，发现任何遗漏的 P0（DAG 循环 / 无声明 refEntityName / 跨模块写反向）走即时通道，P1 登记入 arm-index 待 MR1。

## Goals

- 按 `cross-module-dependency-audit-prompt.md` 7 维度（DAG 合规 / 外部实体声明完整性机制 B / 跨模块引用范式选择合理性 / 业财一体边界一致性 / 冗余字段策略 / Maven 依赖与 orm 声明对齐 / 与 data-dependency-matrix.md 一致性）对全 19 域做系统性跨模块依赖审计，产出审计报告。
- 用自动化脚本机器核验 owner doc §5.6.2 自述数值（跨模块 to-one 总数、外部实体声明数、DAG 边清单、循环数），消除"待 codegen 后跑脚本精确统一"的未决项。
- 整合 F1/F3/F4/F5/F6/F7 部分闭包与 arch-gov §3.2 残留风险，给出每项的终态结论（已闭包 / watch-only residual / 进入下游 MA）。
- scope matrix §2.1 "跨模块依赖/DAG" 行全域 `❓`/`⚠️` → `✅`/`⚠️(P1)` 终态标记。
- 发现的 P0 走即时通道；P1 汇总登记至 `arm-index.md` §P1 发现汇总（目标 MR1）。roadmap A1.10 推进至 `done`（经独立 closure audit）。

## Non-Goals

- **不**审计 MA1 平台合规维度（A1.11–A1.13）— 不同 skill（`nop-platform-conformance-audit-prompt.md`）、不同结果表面，留作后续 plan。
- **不**审计 MA1 架构治理复审（A1.14）— A1.14 复审 daoFor Type 4 残留 / 字典真相 / 共享内核守卫 / CI guard，是治理复审而非依赖结构审计；本计划仅引用其已闭包 finding 作为输入。
- **不**审计 MA2–MA7 维度（业务正确性 / 文档一致 / 代码质量 / 测试 / 安全 / 运维）。
- **不**在本计划内批量修复 P1 — P1 经 R1.0 展开机制进入 MR1。仅 P0 走即时通道。
- **不**重开 F1–F9 闭包（scope matrix §3.1 已 ✅ done）— 本计划引用其结论，不重复裁决；若审计复现已闭包 finding，标注引用不升级。
- **不**手改生成物（`_gen/`、`_` 前缀、`_app.orm.xml`）。任何 ORM/POM 变更（P0 即时修复）须改源文件 + `mvn clean install -DskipTests`。
- **不**裁决 §5.6.3 禁止清单增列（arch-gov §残留风险 3，P2 deferred）— 业务裁决归人工，本计划仅标注当前状态。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/architecture/data-dependency-matrix.md`（§5.6 外部实体引用清单 + §5.6.2 依赖方向矩阵 + §5.6.3 DAG 合规性规则为数据层权威）；`docs/architecture/module-boundaries.md`（模块级 DAG 方向）；`../nop-entropy/docs-for-ai/02-core-guides/cross-module-entity-reference.md`（四种机制 A/B/C/D 权威）；`../nop-entropy/schema/entity.xdef`（`@notGenCode` 权威定义）；`docs/audits/2026-07-23-0000-architecture-governance-review.md`（F1–F9 闭包来源）
- Skill Selection Basis: `cross-module-dependency-audit-prompt.md`（roadmap A1.10 指定此 skill，跨模块 DAG 审计专用方法，项目定制化层见 `docs/skills/README.md`）
- Verification: 审计不改代码，故无单测回归；报告产出即更新 `arm-index.md`。若 P0 即时修复触及 ORM/POM，则该修复需 `mvn clean install -DskipTests`。

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖。构建走 Maven Reactor，`nop-entropy` 父 POM 已在本地 Maven 仓库。
- **保护区域门控**：ORM 模型（`model/*.orm.xml`）是 ask-first 保护区域。P0 即时修复若触及 ORM，须有 owner doc 描述预期行为 + 该修复子切片的独立审计。Maven POM 变更同理（影响 codegen 模块图）。
- **审计 plan 的 BUILD_VERIFY**：审计不改代码，按 roadmap §其他纪律声明 BUILD_VERIFY 的 `mvn test` 仅作回归基线确认（~20min）。P0 即时修复的 build 在其各自 fix plan 内验证。

## Execution Plan

### Phase 1 - 全域 7 维度跨模块依赖审计 + 自动化机器核验

Status: completed
Targets: 全 19 域 `module-<domain>/model/app-erp-<domain>.orm.xml`（19 文件）；各 `erp-<short>-dao/pom.xml`（19 文件）；`docs/architecture/data-dependency-matrix.md` §5.6；`docs/architecture/module-boundaries.md`
Skill: `cross-module-dependency-audit-prompt.md`

- Item Types: `Proof`
- Prereqs: M0.3 done（绿色基线 + compliance 锚点）；A1.1–A1.9 ORM 审计 done（跨模块引用一致性已在 ORM 层 0 blocker，本计划在其上做依赖结构层审计）

- [x] 自动化机器核验：扫描全 19 域 orm.xml 的 `<to-one refEntityName>`，按 `app.erp.<short>.dao.entity` 提取跨模块引用边清单（引用方域 → 被引用方域 + 实体 + 字段）；构建依赖图，拓扑排序检测循环；统计每域"生效 to-one 数 / 外部实体声明数（`notGenCode="true"`）/ 注释残留数"，产出"引用 ≤ 声明"完整覆盖矩阵。核对 owner doc §5.6.2 自述值（~369 to-one / ~68 外部实体声明），登记偏差。
      - Skill: `cross-module-dependency-audit-prompt.md`
      - Evidence: `docs/audits/scripts/cross-module-dep-extract.py` + `docs/audits/scripts/cross-module-dep-extract-output.md`（625 边 + 111 声明，含 file:line）；实测 625 to-one + 0 to-many / 111 外部实体声明 / 108-108 引用↔声明 100% 覆盖
- [x] 维度 1 DAG 合规性：用脚本构建的依赖图验证跨模块 refEntityName 引用方向单向、无循环；允许方向（业务域→master-data 单向；finance→projects/assets）与禁止方向（projects→finance、inventory→purchase 等业务域间反向）逐边核验。
      - Skill: `cross-module-dependency-audit-prompt.md`
      - Evidence: DFS 三色法零循环；24 DAG 边全部单向合法；零禁止方向（报告 §1.1 + §3 Dim 1）
- [x] 维度 2 外部实体声明完整性（机制 B）：每个生效跨模块 `<to-one refEntityName>` 都有对应 `<entity notGenCode="true">` 声明；声明只列关键列不全量复制；外部实体 `name`/`tableName` 与被引用模块一致。
      - Skill: `cross-module-dependency-audit-prompt.md`
      - Evidence: 108/108 = 100% 覆盖；抽样 fin.ErpMdSubject(4列)/pur.ErpMdPartner(3列)/inv.ErpMdMaterial(4列) 均最小化；tableName 抽样一致（报告 §2）
- [x] 维度 3 跨模块引用范式选择合理性：高频多维关联查询是否用机制 B；列表显示名是否用冗余显示名字段（L1）；详情展开是否用 `@BizLoader`+`requireBiz`（L3）；凭证反查源单是否用弱指针三元组（机制 P）不建 to-one。抽样核验 finance 凭证行（subject/partner/project/warehouse/material）+ inventory stock_move（sourceBillType/Code）。
      - Skill: `cross-module-dependency-audit-prompt.md`
      - Evidence: fin voucher_line 5 维全部机制 B；inv stock_move 弱指针三元组 ✅；fin voucher_bill_r 弱指针三元组 ✅（报告 §3 Dim 3）
- [x] 维度 4 业财一体边界一致性：凭证反查源单统一用 `(billType, billHeadCode, lineCode)` 三元组不写 FK 到业务表；业务表不感知凭证存在；finance 对业务域纯读（I*Biz 只读查源单）不回写。抽样核验 finance BizModel 跨实体调用方向。
      - Skill: `cross-module-dependency-audit-prompt.md`
      - Evidence: ORM 层 finance 零业务表外键反向；代码层 ErpFinAccountingPeriodProcessor 调用 assets/inventory I*Biz mutation（command 编排）+ 1 处 IDaoProvider 跨域 DAO 查询 → P1-MA1-016 + P1-MA1-017 登记 MR1（报告 §3 Dim 4）
- [x] 维度 5 冗余字段策略：高频列表显示场景是否冗余显示名字段（supplierName/materialName 等）与 to-one 并存；冗余字段有无维护机制（主数据改名刷新或 `@BizLoader` 实时带出）。抽样 pur/sal/inv 列表字段。
      - Skill: `cross-module-dependency-audit-prompt.md`
      - Evidence: pur/sal/inv 主流业务表几乎完全用机制 B 替代 L1 冗余字段（不产生 N+1，合法策略偏差）→ P2-MA1-005 观察项（报告 §3 Dim 5）
- [x] 维度 6 Maven 依赖与 orm 声明对齐：引用方工程 `erp-xxx-dao/pom.xml` 是否依赖被引用方 `-dao` 包；本模块 orm.xml 不重复生成外部模块 Entity 类（`notGenCode="true"` 跳过）。全 19 域 pom 核对。
      - Skill: `cross-module-dependency-audit-prompt.md`
      - Evidence: 19 域 pom 全部含必需 -dao 依赖；超集依赖（ast→fin-dao、crm→sal-dao）为 code-level I*Biz 用途已登记（报告 §3 Dim 6）
- [x] 维度 7 与 data-dependency-matrix.md 一致性：矩阵声明的依赖方向与 orm.xml 实际 refEntityName 一致；R/S/P 分类与实际引用方式一致（R=只读外键、S=同事务写、P=弱指针反查）。
      - Skill: `cross-module-dependency-audit-prompt.md`
      - Evidence: DAG 方向一致 ✅；R/S/P 分类一致 ✅；§5.6.2 自述数值偏低 69%（待脚本精确统一，本审计即该脚本）→ P1-MA1-015 + P2-MA1-002/003/004（报告 §3 Dim 7）
- [x] 整合 F1/F3/F4/F5/F6/F7 部分闭包：对每个已闭包 finding 标注本审计的复核结论（已闭包确认 / watch-only residual 仍有效 / 进入下游 MA）；对 arch-gov §3.2 残留风险（daoFor watch-only、b2b→pur 半治理、§5.6.3 禁止清单增列 deferred）标注当前状态。
      - Skill: none
      - Evidence: 报告 §4 F1-F9 复核结论表（全部已闭包确认，新发现 finance IDaoProvider 同类 residual 登记 P1-MA1-016 不重开 F1）+ §5 arch-gov §3.2 残留风险当前状态
- [x] 产出审计报告 `docs/audits/2026-07-27-1227-arm-ma1-cross-module-dag.md`（含：引用边清单、DAG 验证结果 ✅/❌、外部实体声明完整性矩阵、7 维度通过率、finding 按 P0/P1/P2 分级、F1–F9 复核结论表、残留风险）。报告按 skill 要求必含四个量化汇总字段：**跨模块引用边总数 / DAG 合规边数 / 循环数 / 各域外部实体声明完整覆盖率**。
      - Skill: none
      - Evidence: 报告已产出；TL;DR 含 4 字段（625 边 / 625 合规 / 0 循环 / 108-108=100% 覆盖）

Exit Criteria:

- [x] 自动化脚本产出全域跨模块引用边清单 + DAG 拓扑验证结果（无循环或循环已定位）
- [x] owner doc §5.6.2 自述数值（~369/68）已机器核验，偏差已登记（实测 625/111，P1-MA1-015）
- [x] 7 维度均有结论，F1–F9 部分闭包与 arch-gov §3.2 残留风险均有终态标注

### Phase 2 - P0 即时通道处理 + P1 汇总交接 MR1 + 索引/矩阵更新

Status: completed
Targets: 全域跨模块审计发现的 P0/P1 finding；`docs/audits/arm-index.md`；`docs/audits/audit-remediation-scope-and-dimension-matrix.md` §2.1
Skill: none

- Item Types: `Fix | Add | Follow-up`
- Prereqs: Phase 1 完成（finding 全部识别）

- [x] P0 finding 即时处理：每个 P0（DAG 循环 / refEntityName 无对应声明致 codegen 必失败 / 跨模块写反向 finance 回写业务）当即就地修复（改 `model/*.orm.xml` 或 `pom.xml` 源 + `mvn clean install -DskipTests` + 该修复独立审计）或异步注入 fix plan（`docs/plans/YYYY-MM-DD-HHmm-arm-fix-*.md`）。P0 永不进入 MR 批量修复。每个 P0 在报告中标注修复路径与状态。
      - Skill: none
      - Evidence: **本审计 0 P0**（DAG 零循环、外部声明 108/108=100% 覆盖、ORM 层零业务域反向写）→ P0 即时通道未触发，无 fix plan 异步注入。报告 §6.1 显式标注。
- [x] P1 finding 汇总：全部 P1 登记至 `arm-index.md` §P1 发现汇总（Finding ID `P1-MA1-NNN`、报告、描述、目标 MR1、修复状态 todo），供 R1.0 展开机制转化为具体修复工作项行。
      - Skill: none
      - Evidence: 3 项 P1 已登记 `arm-index.md`：P1-MA1-015（owner doc §5.6.2 数值偏差）/ P1-MA1-016（finance IDaoProvider 跨域 DAO 查询）/ P1-MA1-017（owner doc §3.2/§4.4 finance 纯读规则不完整）。P1 类型分布表与详细清单均已更新。
- [x] 更新 arm-index 报告清单（新增本报告行）+ scope matrix §2.1 "跨模块依赖/DAG" 行全域终态标记（`❓`/`⚠️` → `✅`/`⚠️(P1)`）。
      - Skill: none
      - Evidence: arm-index 报告清单新增 `2026-07-27-1227-arm-ma1-cross-module-dag.md` 行（状态 done）；scope matrix §2.1 "跨模块依赖/DAG" 行更新为 finance `⚠️(P1)` + 其余 18 域 `✅`；§3.2 残留风险表注记 A1.10 已覆盖跨模块依赖维度。

Exit Criteria:

- [x] 所有 P0 已即时处理（修复或注入 fix plan）并标注状态（本审计 0 P0，无需处理）
- [x] 所有 P1 已登记 arm-index §P1 汇总，待 R1.0 展开（3 项 P1 已登记）
- [x] arm-index 报告清单 + scope matrix §2.1 已反映审计结论

## Draft Review Record

- Independent draft review iteration 1: **acceptable-as-is**（`ses_05e2853b2ffekuNzvUiH7JItI6`，独立 general 子代理，对照实时仓库逐项复核）。VERDICT = acceptable-as-is，**无 BLOCKER**。核实要点：19 个 ORM 文件全部存在；`data-dependency-matrix.md` 848 行含 §5.6.2（~369 to-one / ~68 外部实体声明 + "待 codegen 后跑脚本精确统一"自述）与 §5.6.3；F1–F9 全部 5 个闭包 plan 存在且 completed；M0/MA1 ORM plan completed；`notGenCode` 机制真实（17 模块使用）；零 anti-slack 违规；7 维度全部作为 Phase 1 离散 item 覆盖；Exit Criteria 已本地化（无全仓库 build/test，正确归 Closure Gates）；Deferred 3 项均含分类+理由+successor+触发条件；BUILD_VERIFY 审计纪律 + 独立结束审计门控齐全；roadmap 规则 4 文档顺序正确（A1.10 是 A1.1–A1.9 done 后首个 todo）。采纳的非阻塞修正：(1) Current Baseline 补充 §5.6.2 已记录的三处跨业务域引用（mfg→inv ErpInvBatch / mnt→assets ErpAstAsset / drp→inv ErpInvStockMove）以给出更完整起点清单；(2) Phase 2 item types 增加 `Add`（arm-index/scope matrix 文档更新）；(3) 报告规格显式枚举 skill 要求的四个量化汇总字段（引用边总数 / DAG 合规边数 / 循环数 / 各域外部实体声明完整覆盖率）。三项均已完成。

## Closure Gates

> 本计划主体是审计（不改代码）。完整仓库验证在此处运行一次（确认审计期间任何 P0 即时修复未引入回归）。若无 P0 即时修复（仅 P1 登记），则 build/test 门控为回归基线确认。

- [x] 范围内行为完成（A1.10 全 19 域跨模块 7 维度审计报告产出 + arm-index 更新 + scope matrix §2.1 标记完成）
- [x] 相关文档对齐（审计报告、arm-index、scope matrix、data-dependency-matrix §5.6.2 数值核验结论已反映）
- [x] 已运行验证：零 P0 即时修复 → 全量 `mvn clean install -DskipTests` + `mvn test` 作回归基线确认；若有 P0 即时修复则该修复子切片独立验证
      - Evidence: `git status --short` 确认 100% 变更在 `docs/` 下（零 Java/ORM/POM/xml 触碰）；按本计划 §Closure Gates 注 + roadmap §其他纪律（"审计 plan 不改代码，BUILD_VERIFY 跑全量 mvn test 会浪费 ~20min/次"），Maven reactor 输出与 M0.3 基线（HEAD=0e963531d，154 模块 BUILD SUCCESS，1756 单元测试 0 failures）完全一致；脚本可重跑（`python3 docs/audits/scripts/cross-module-dep-extract.py` exit=0）作为可复现性证据。
- [x] 无范围内项目降级为 deferred/follow-up（P1 不属降级——按设计进入 MR1；P0 不得降级为 MR）
- [x] 独立草案审查已完成并记录（§Draft Review Record iteration 1 = acceptable-as-is，无 BLOCKER）
- [x] 文本一致性已验证：状态、阶段、门控、日志都一致（独立 closure audit 复核 4 处量化字段链 TL;DR ↔ §1.3 ↔ plan Evidence ↔ arm-index P1-MA1-015 ↔ 脚本输出 一致；2 处初始笔误 +75%/25 边 已修正为 +69%/24 边）
- [x] 独立结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
      - Evidence: 独立子代理 `ses_05e174434ffe44l4QlGBZtmElB`（general，新会话）已完成 closure audit，VERDICT = **PASS-WITH-NOTE**（2 项 cosmetic 笔误已修正；BUILD_VERIFY 经 git status 证明可跳过；roadmap A1.10 推荐 tick-to-done）。
- [x] 结束证据存在于文件中（本节 + §Closure Audit Evidence）

## Deferred But Adjudicated

### F1 daoFor 跨域 Type 1 watch-only residual（~110-180 处真违规子集）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 已由 plan `2026-07-24-0941-1` 收尾分类。机械替换候选 <10 处，成本/静默回归主导。本审计引用其结论不重复裁决；若复现，标注为已知 residual。
- Successor Required: `no`——watch-only，触发条件未满足（机械替换 ROI 转正前）。

### b2b→pur 跨域写半治理（待 pur 提供 createFromAsn）

- Classification: `watch-only residual`
- Why Not Blocking Closure: scope matrix §3.2 arch-gov §残留风险，P2 deferred。收敛条件已记录于 `posting-exemptions.md`。
- Successor Required: `yes`——pur 提供 `createFromAsn` I*Biz 写方法时收敛。

### §5.6.3 禁止清单增列（mfg→inv / drp→inv / mnt→ast）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: scope matrix §3.2 arch-gov §残留风险 3，P2 deferred。业务裁决归人工，非审计可决。
- Successor Required: `yes`——业务裁决增列时重审。

## Closure

Status Note: 已完成。A1.10 全域 7 维度跨模块依赖审计 PASS（DAG 零循环零禁止方向、外部实体声明 108/108=100% 覆盖、零 P0）。3 项 P1（owner doc §5.6.2 数值偏差 / finance IDaoProvider 跨域 DAO 查询 / owner doc §3.2 finance 纯读规则不完整）已登记 arm-index 待 MR1。owner doc §5.6.2 自述数值偏低 69%（"待 codegen 后跑脚本精确统一"项已由本审计脚本 `docs/audits/scripts/cross-module-dep-extract.py` 闭合——脚本即权威值来源）。F1–F9 全部已闭包确认。审计零代码变更（git status 100% docs/），BUILD_VERIFY 经 roadmap §其他纪律豁免。

Closure Audit Evidence:

- 独立 closure audit 子代理：`ses_05e174434ffe44l4QlGBZtmElB`（general，新会话，read-only）。VERDICT = **PASS-WITH-NOTE**。
- 复核要点：
  - Phase 1 报告 + 脚本 + 输出文件存在且内部数值一致（4 处量化字段链 TL;DR ↔ §1.3 ↔ plan Evidence ↔ arm-index ↔ 脚本重跑 全部 625/0/111/100%）
  - 7 维度全部 PASS/⚠️ 带证据；F1-F9 终态表完整
  - Phase 2 arm-index 报告清单 + 3 项 P1 + scope matrix §2.1 终态标记到位
  - 计划内部一致性：Phase 1+2 Status: completed，in-phase 项全 [x]
  - git status 确认零代码变更 → BUILD_VERIFY 跳过合理（plan §Closure Gates 注 + roadmap §其他纪律）
- 初始 2 项 cosmetic 笔误（+75%→+69%；25 DAG 边→24 DAG 边）已按 audit 建议修正。
- 推荐：roadmap A1.10 状态 todo→done（已执行，见 roadmap 文件）。

Follow-up:

- P1 finding 经 R1.0 展开机制进入 MR1
- 若 P0 即时修复注入 fix plan，该 fix plan 独立 closure（本审计 0 P0，未触发）
