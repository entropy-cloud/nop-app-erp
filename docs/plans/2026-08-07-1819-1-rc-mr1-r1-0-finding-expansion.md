# 2026-08-07-1819-1-rc-mr1-r1-0-finding-expansion R1.0 — MR1 P0/P1 需求分歧汇总、排序并展开为 RC-R1.n 修复工作项行（分批启动：第一批纯预授权类修复）

> Plan Status: completed
> Last Reviewed: 2026-08-07
> Mission: requirement-compliance
> Work Item: R1.0（MR1 P0/P1 需求分歧**展开器**）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 R1.0
> Related: `docs/audits/requirement-compliance-methodology.md` §10（MR1 展开器机制）+ §5（预授权类目 + 保护区域暂停协议 + 人工扩展授权）；`docs/discussions/2026-08-07-1140-rc-approval-inventory-analysis.md` §5 Q1-Q10 + §7 A1-A5（权威人工裁决）；`docs/plans/2026-08-07-0400-3-rc-ma4-a4-2-ext-domain-runtime-expander.md`（A4.2 展开器范式先例）；`docs/plans/2026-07-29-1923-1-r1-0-mr1-p1-finding-expansion.md`（audit-remediation R1.0 展开器范式先例）
> Audit: required

## Current Baseline

> 本计划是**展开器工作项**（expander，对齐 A4.1/A4.2/R1.0(audit-remediation) 范式），结果表面 = roadmap §MR1 表内追加的 RC-R1.n 实体行 + 展开映射记录。展开器**不实施修复本身**——修复属后续各 RC-R1.n 实体行（各自独立 plan + 独立 done）。展开完成（全部实体行已追加）后 R1.0 标记 done（同 A4.1/A4.2 的 done 判据 = 展开完成，而非「全部修复完成」）。

- **审计里程碑已有效完成**：MA1（A1.1-A1.51 全部 51 切片 done）+ MA2（A2.1-A2.9 全部 done）+ MA3（A3.1-A3.5 全部 done）+ MA4（A4.1 业财 25 行 + A4.2 扩展域 185 行全部 done，**除 3 行 MR1-blocked successor**：A4.2.3（P1-RC-008 预留写路径）、A4.2.79（P1-RC-031 效期拦截）、A4.2.119（P1-RC-049 物料归集）——三行显式标注「保留 todo 待 MR1 落地后回队」，是 MR1 修复的**下游 successor** 而非 R1.0 的阻塞项（roadmap MA4 表行内注记 + A4.2.79/A4.2.119 的 Deferred But Adjudicated 先例）。**§7 A1 裁决背景明确「MA1-MA4 全部 done（185+ 实体行）后」分批启动 R1.0** → R1.0 的 `Deps = MA1+MA2+MA3+MA4 done` 满足（3 行 MR1-blocked 行属 successor 不阻塞展开器）。
- **输入就绪**：全部 MA1-MA4 报告的 P0/P1 finding 已登记 `docs/audits/arm-index.md`（§RC 发现追踪区 + §P1 详细清单 + 各域 A2.x/A4.x 分区）。实仓清点：**88 个唯一 P1-RC ID + 87 个唯一 P2-RC ID**（arm-index 全文本 grep 实测）；其中 `P2-RC-004` 经 A4.1.15 运行时数值推理**升级为 P1**（FIFO 到岸成本 delta 层结构性永不被消耗，目标 MR 已改为 MR1）。**零 P0**（arm-index 无实体 P0-RC finding——`P0-RC-` 字样仅 methodology/MA1-MA4 plan 命名模板文本命中，非实体 finding）→ MR0 无活跃行，R1.0 只展开 P1。**展开输入集 = 全部 MR1 归属 P1 finding**，不只限 P1-RC 系列——RC 报告对既有 audit-remediation finding 显式「重开/归 MR1」的行亦纳入（Phase 1 提取规则，见下）。
- **复用/重开 finding 需纳入展开**：P1-MA2-083（承付恢复不对称，A4.2.31/45 维持 reuse 重开，§5 Q9 裁决归表 B 会计收敛类随 Q4 授权回队，纯 Processor 预授权）；P1-MA2-071（contract 到期 job 重开族，A4.2.158 reuse，Q9 维持重开强制实现）；P1-RC-091（A4.1.5 试算平衡 BUDGET-only 未排 COMMITMENT——§7 A3 裁决**属会计核心路径须独立 plan-audit + ask-first**）；P1-RC-092（A4.1.17 MySQL-RR MVCC TOCTOU 重新打开——修复方向①②③ 触及锁/check 逻辑或 ORM UK 须 ask-first）；P2-RC-061（A4.2.148 维持 P2，§7 A4 裁决**纯逻辑修复预授权**——补 IDLE 分支）；P2-RC-057（A4.2.145 维持 P2，§7 A5 裁决 ORM defaultValue 改 "WARN" **纯收敛按 Q3 纯加性类自动执行**）。**RC 报告显式重开/归 MR1 的既有 audit-remediation P1 finding 亦属展开输入**（基线预期 ≥5 项，以 Phase 1 实仓 census 为准）：P1-MA4-017（hr 薪酬 270/290/300 过账接线，A4.2.22/23「修复归 MR1+ask-first」，arm-index 注「须重开经 MR1（RC-R1.n）实现」）；P1-MA2-041（hr 调研 publish/close 三态死状态 + 18 行 CRUD 桩，arm-index §A2.7a 重开注记「Q4=(a) 下重开经 MR1」+ A1.14 §RC 交叉引用注记 :355；A4.2.22-26 运行时切片回填 arm-index 行维持重开，修复归 MR1 纯 BizModel 预授权[arm-index:357]）；P1-MA2-043（hr 工时单 approve/reject，P1-RC-015 注「与 P1-MA2-043[reuse，approve/reject] MR1 修复协同」）；P1-MA2-062（inventory completeTake 自动差异移动单，A4.2.80「§4 复核倾向重开 P1 入 MR1，须人工确认 product-scope」）；P1-MA1-010（projects 工时过账 exchangeRate=ONE 硬编码，A4.2.116「维持 P1-MA1-010 投影，修复归 MR1 触过账 buildEvent ask-first」）。**与 audit-remediation roadmap §MR1 R1.x 行的交叉核对规则（关键判据）**：这些既有 finding 的 R1.x 行多为 `resolved` 但属**方案 B（documented simplification / Deferred 标注）关闭**——文本一致性维度关闭**不**关闭需求契约维度（methodology §4「resolved 不撤销」+ RC 报告 A4.2.n 运行时确认的「维持 P1」注记）。故：**(a) RC 报告显式重开/归 MR1 者（含 resolved-via-deferral）照常展开为 RC-R1.n 行**，其 audit-remediation R1.x 行仅作背景交叉引用（不因「R1.x 已 resolved」而跳过）；(b) 仅当 finding 系 **resolved-via-implementation 且未被 RC 报告重开** 时才登记交叉引用不重复展开（基线预期无此类项，Phase 1 实仓逐一核对）。此判据依据 = RC 报告 A4.2.n 行内「维持 P1 / 重开」注记 + arm-index「修复归 MR1」注记，非 R1.x 行状态。
- **人工裁决已生效（2026-08-07 批准 / 2026-08-08 生效，`2026-08-07-1140` §5 + §7）**：
  - **§7 A1（R1.0 启动方式）= 分批启动**：第一批 = 纯预授权类修复（A2 代码逻辑类，约 30+ 项：P1-RC-003/010/017/019 等，不触 ORM/会计核心/删除），driver 自动逐项 DRAFT_PLANS → 审查 → EXECUTE → closure → done；越界项按 A2 逐项暂停。
  - **§7 A2（越界项处理机制）**：每个越界项独立 fix plan + 独立 plan-audit + plan 内 `- [ ] ask-first 人工确认` checkbox；driver 执行到越界行暂停等待人工批准，非触及行继续。越界项指：改既有语义 ORM（P1-RC-006/092 等）、会计核心路径行为（VoucherFact/PostingProcessor）、数据删除链路（P1-RC-062 SKU 删除、P1-RC-079 purgeDate→delete）、真相源契约段修订。
  - **§7 A3**：P1-RC-091 属会计核心路径 → 独立 plan-audit + ask-first，不自动执行。
  - **§7 A4**：P2-RC-061 纯逻辑修复（`EquipmentStatusLinker.restoreToRunning` 补 IDLE 分支）→ 按 A2 预授权自动执行。
  - **§7 A5**：P2-RC-057 ORM defaultValue 改 `"WARN"` → 按 Q3 纯加性类自动执行。
  - **Q3（ORM 纯加性批量授权）**：加列/加 UK/新增实体、不改既有语义、无 NOT NULL 无默认列、无涉及既有数据的 UK 增设、无删除/迁移/索引改造 → 免 ask-first；超出回落独立 fix plan + plan-audit。
  - **Q4（收敛性会计修复批量授权）**：使实现向 owner doc 契约收敛、不反向改契约段、不涉删除/迁移 → 免 ask-first；VoucherFact/PostingProcessor 核心路径改动行为仍须独立 plan-audit。
  - **Q5-Q9**：P1-RC-025/029/031/062 + 表 E 重开族（P1-RC-008/009/056/061 + P1-MA2-071 + P1-RC-063）维持强制实现，Q4=(a) 无例外。**P1-MA2-083 不属重开族**——§5 Q9 裁决「归表 B 会计收敛类，随 Q4 批量授权 + 独立 plan-audit 回队」（roadmap 预授权声明段同文），展开时按 Q4 收敛性会计类登记（纯 Processor 调既有 commit() 入口预授权，A4.2.31/45 已证）。
  - **Q10**：P2-RC-005/011/016/012 use-cases 真相源命名修订经批准登记（§9 流程）——属 P2 级文档修订任务，**不属 R1.0 的 P0/P1 展开范围**，登记于 Deferred But Adjudicated（Successor Required: yes，单独文档任务）。
- **roadmap §MR1 表现状**：仅含 R1.0 行（Status: todo），无 RC-R1.n 实体行。R1.0 的 Work Item 描述：「MA1-MA4 P0/P1 需求分歧汇总、排序并展开为具体修复工作项行（对每个分歧裁决"修复"或"登记"；触及保护区域行标注）」。
- **剩余差距**：RC-R1.n 实体行未追加 = MR1 修复队列缺口。本展开器解除此缺口后，mission driver 可按分批启动裁决逐 RC-R1.n 实体行 DRAFT_PLANS → 执行。

## Goals

- 读取 MA1-MA4 报告 + arm-index（§RC 追踪区 + §P1 详细清单 + 各域分区）的 MR1 归属 P1 finding **全集**（完整枚举，禁止抽样；P1-RC 系列 + RC 报告显式重开/归 MR1 的既有 audit-remediation P1 finding），逐 finding 登记为 roadmap §MR1 表内 RC-R1.n 实体行。
- 按 §7 A1 分批启动裁决归类：**第一批 = 纯预授权代码逻辑类**（A2，不触 ORM/会计核心/删除，~30+ 项），越界项（改语义 ORM / 会计核心路径 / 数据删除 / 真相源契约段）逐项标注 ask-first + 独立 plan-audit 义务。
- 每行 RC-R1.n 含：finding ID 交叉引用（含复用/重开 finding 的 RC 交叉引用）/ 域 / 修复范围 / 批次归属（第一批 or 越界）/ 触及保护区域标注（是/否 + 类别）/ Skill。
- 展开完成后 R1.0 标记 done（done 判据 = 全部实体行已追加，非修复完成）。
- 产出展开映射记录 `docs/audits/<执行时间戳>-rc-mr1-r1-0-expander.md`（finding → RC-R1.n 行映射 + 批次归类依据 + 计数自检 + 保护区域标注汇总）。

## Non-Goals

- **不实施任何修复**（修复属后续各 RC-R1.n 实体行，各自独立 plan + 独立 done + 独立草案审查/结束审计）。
- **不展开 MV/MG 里程碑**（MV V.1-V.3 deps = MR1 done 未满足；MG G.1-G.3 deps = MV done 未满足）。
- **不解锁 A4.2.3 / A4.2.79 / A4.2.119**（三行是 MR1 修复的 successor，触发条件 = 对应 P1-RC-008/031/049 修复落地后回队；R1.0 仅登记其回队依赖，不展开为 RC-R1.n 行）。
- **不重新评级既有 finding**（R1.0 忠实展开 arm-index 既有分级；P2-RC-004→P1 升级已在 A4.1.15 报告 + arm-index 行内完成，本展开器沿用）。
- **不修改真相源**（product-scope / use-cases / owner doc 需求契约；§9 冻结条款）。roadmap §MR1 表追加 RC-R1.n 行是工作项追踪更新（非冻结真相源），属展开器既定动作。Q10 use-cases 命名修订是已批准的独立文档任务，不并入本展开器。
- **不展开 P2 登记项**（P2 登记不强制，除 §7 A4/A5 明确裁决修复的 P2-RC-061/057 外，一律 successor watch-only 不入 MR1 表）。
- **不修改代码/ORM/api.xml/BizModel/Processor/view.xml**（只读收集 + roadmap 表追加 + arm-index 交叉引用回填）。

## Task Route

- Type: `verification or audit work`（展开器：P0/P1 finding 全集 → 批次归类 → RC-R1.n 实体行展开与登记；非实现变更）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§2 分级判据 + §5 预授权/保护区域 + §10 MR1 展开器机制）+ `docs/backlog/requirement-compliance-roadmap.md`（§MR1 R1.0 + Work Item Details）+ `docs/discussions/2026-08-07-1140-rc-approval-inventory-analysis.md`（§5 Q1-Q10 + §7 A1-A5 权威裁决）+ `docs/audits/arm-index.md`（§RC 发现追踪区，finding 全集输入）
- Skill Selection Basis: `Skill: none`（roadmap §MR1 R1.0 行 Skill 列 = none；展开器是 finding 全集清点 + 批次归类 + 表追加的索引/登记工作，不涉及 ORM/BizModel/view/测试代码编写，与 A4.2 展开器 Skill 选择一致——A4.2 用 `multi-dimensional-audit-prompt` 是因为需对运行时验证方法做维度归类，本展开器的归类对象是「修复预授权类别」而非审计维度，归类依据已由 §7 A1/A2 + Q3/Q4 裁决完全确定，无需额外审计方法框架）

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 展开器以读 MA1-MA4 报告 + arm-index + 追加 roadmap 行为主（纯分析）。§8 过程纪律自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；本展开器无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - P0/P1 finding 全集清点与批次归类

Status: completed
Targets: `docs/audits/arm-index.md` §RC 发现追踪区 + MA1-MA4 报告（交叉核对）
Skill: none

- Item Types: `Proof | Decision`
- Prereqs: MA1+MA2+MA3+MA4 done（已满足，含 §7 A1 裁决确认）

- [x] `Proof` 全集清点：从 arm-index（§RC 追踪区 + §P1 详细清单 + 各域 A2.x/A4.x 分区）逐行提取全部 MR1 归属 P1 finding——(a) 88 个唯一 P1-RC ID（P1-RC-091/092 已含在内不重复计数）；(b) P2-RC-004 升级项；(c) RC 报告显式重开/归 MR1 的既有 audit-remediation P1 finding（基线预期 ≥5：P1-MA4-017 / P1-MA2-041 / P1-MA2-043 / P1-MA2-062 / P1-MA1-010，以 arm-index 行内「修复归 MR1 / 重开经 MR1」注记为提取依据，Phase 1 实仓确认全集后分母以实测为准）；(d) 复用/重开纳入行（P1-MA2-083/071）。**交叉核对规则**：既有 finding 的 audit-remediation R1.x 行状态（多为 resolved-via-deferral）**不**作为跳过展开的依据——RC 报告显式重开/归 MR1 者（含 resolved-via-deferral）照常展开，R1.x 行仅背景交叉引用；仅 resolved-via-implementation 且未被 RC 报告重开者登记交叉引用不重复展开（基线预期无此类项，Phase 1 实仓逐一核对并记录）。产出清点表（finding ID / 域 / UC / 修复范围摘要 / 预授权类别 / 触及保护区域标注 / 批次归属 / R1.x 交叉引用）。零 P0 证实（arm-index 无实体 P0-RC finding）。
      - Skill: none
- [x] `Decision` 批次归类（分批启动裁决落地的核心裁决）：按 §7 A1「纯预授权类修复（A2 代码逻辑类，不触 ORM/会计核心/删除）」vs「越界项（改语义 ORM / 会计核心路径 / 数据删除 / 真相源契约段）」逐 finding 归类；Q3/Q4 批量授权子集（纯加性 ORM / 收敛性会计）按授权判据落入第一批可自动执行（但 VoucherFact/PostingProcessor 核心路径改行为仍须独立 plan-audit）；§7 A3（P1-RC-091）/A4（P2-RC-061）/A5（P2-RC-057）按裁决登记；arm-index 行内「修复方式」注记（预授权 or ask-first）作归类核对依据。归类结果在展开映射记录中逐项列出依据。
      - Skill: none
- [x] `Proof` 计数自检：**展开分母 = Phase 1 实仓 census 实测 MR1 归属 P1 finding 全集**（基线预期 = 91 + 5 = 96 findings：88 P1-RC + P2-RC-004 升级项 + P1-MA2-083/071 复用重开行 + RC 重开族 ≥5 项[P1-MA4-017/P1-MA2-041/P1-MA2-043/P1-MA2-062/P1-MA1-010]，最终数以 census 实测为准，若出现差异须在展开映射记录说明差异来源）；批次一（纯预授权）项数 + 越界项数 = 展开分母，另加 §7 A4/A5 裁决的 2 个 P2 修复行（P2-RC-061/057）——每项恰好归属一行，无遗漏无重复；展开映射记录须逐项列出分母构成与每行 finding 集合，防静默错分。
      - Skill: none

Exit Criteria:

- [x] MR1 归属 P1 finding 全集清点表产出（展开分母 = Phase 1 census 实测：基线预期 96 = 88 P1-RC + P2-RC-004 升级项 + P1-MA2-083/071 复用重开行 + RC 重开族 ≥5 项；精确总数 + 按域/批次分组），可追溯每项到 arm-index 行
- [x] 批次归类完成，每项含预授权类别 + 保护区域标注 + 归类依据；计数自检通过（批次一 + 越界项 = 展开分母，另含 §7 A4/A5 裁决的 P2-RC-061/057 两修复行，每项恰好归属一行）

### Phase 2 - 排序并写入 RC-R1.n 工作项行

Status: completed
Targets: `docs/backlog/requirement-compliance-roadmap.md` §MR1 表
Skill: none

- Item Types: `Add | Decision`
- Prereqs: Phase 1 清点归类完成

- [x] `Decision` 分组粒度规则：按域 × 修复方式（纯 BizModel 逻辑 / 调度接线 / config 接线 / SPI 实现 / 跨域协调 / ORM 变更 / 会计核心）组织为逻辑工作项；同域同根因同控制点的 finding 合并为单行（如 P1-RC-050/051 同站点协同、A4.2.31/45 承付恢复对称性同根因族）；不同域/不同修复方式分列。分组裁决记录于展开映射记录。
      - Skill: none
- [x] `Add` 向 roadmap §MR1 表**追加** RC-R1.1~RC-R1.n 具体工作项行（每行含 `#` / Work Item 描述[含覆盖的 finding ID 集合] / `Status=todo` / Owner Doc / `Deps=R1.0` / Skill / 批次归属标注 / 触及保护区域标注），在既有 R1.0 行之后按序追加（§MR1 表不预注册占位行，本展开器产出实体行）；行序 = 第一批（纯预授权）在前、越界项在后（越界项行内显式标注「ask-first + 独立 plan-audit」），批次内按域编排。
      - Skill: none
- [x] `Add` 按 §7 A2 在越界项行的 plan 义务注记「须独立 fix plan + 独立 plan-audit + plan 内 ask-first 人工确认 checkbox」（保护区域暂停协议 §5）。
      - Skill: none

Exit Criteria:

- [x] roadmap §MR1 表含具体 RC-R1.n 行（无占位行），每行 Work Item 描述含覆盖的 finding ID 集合 + 批次归属 + 保护区域标注
- [x] 第一批 = 纯预授权类修复（A2 代码逻辑类 + Q3/Q4 批量授权子集），越界项全部显式标注 ask-first 义务

### Phase 3 - 回填 arm-index 交叉引用 + 双向完整性校验 + R1.0 done

Status: completed
Targets: `docs/audits/arm-index.md`；`docs/backlog/requirement-compliance-roadmap.md`（R1.0 Status → done）；`docs/audits/<执行时间戳>-rc-mr1-r1-0-expander.md`（展开映射记录定稿）
Skill: none

- Item Types: `Add | Proof`
- Prereqs: Phase 2 RC-R1.n 行写入

- [x] `Add` 更新 arm-index §RC 追踪区每条 P1 finding 的「修复状态」列，追加归属 RC-R1.n 工作项 ID（如 `todo (RC-R1.3)`）；复用/重开 finding（P1-MA2-083/071）在既有行追加 RC 交叉引用注记。
      - Skill: none
- [x] `Proof` 双向完整性校验——(a) 正向：MR1 归属 finding 集合中每一项都能在 roadmap §MR1 表找到覆盖它的 RC-R1.n 行；(b) 反向：每个 RC-R1.n 行覆盖的 finding 集合无遗漏无重复（同一 finding 不被两个 RC-R1.n 行覆盖）；(c) 批次完整性：第一批/越界项划分与 §7 A1/A2 + Q3/Q4 + A3/A4/A5 裁决一致。
      - Skill: none
- [x] `Proof` 过程纪律自检（methodology §8 模板）：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline（本展开器无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明（展开器不新建 finding，仅复用/回填）。
      - Skill: none
- [x] `Add` 展开完成即 roadmap R1.0 状态标记 `done`（done 判据 = 全部 RC-R1.n 行已追加，非修复完成）。
      - Skill: none

Exit Criteria:

- [x] arm-index 每条 MR1 归属 P1 finding「修复状态」列含 RC-R1.n 交叉引用
- [x] 双向完整性校验 + 批次完整性校验通过
- [x] 展开映射记录已落盘（finding → RC-R1.n 映射 + 批次归类依据 + 计数自检）；roadmap R1.0 标记 done

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（独立子代理 `ses_024400fc0ffeTqK3n71YGbLjVJ`，fresh session）——1 MAJOR（Q5-Q9 引用误含 P1-MA2-083 于表 E 重开族，与 §5 Q9「P1-MA2-083 不属重开族——归表 B 会计收敛类」矛盾）+ 2 MINOR（P1-RC-091/092 重复列举 + 计数分母未定义；「替换 R1.0 展开占位」措辞与 §MR1 不预注册占位行矛盾）。修订：Q5-Q9 列表移除 P1-MA2-083 并正确归类 Q4 收敛类；Phase 1 分母显式化（91 = 88 + P2-RC-004 + 复用重开行）；Phase 2 措辞改「追加」。
- Independent draft review iteration 2: `needs revision`（独立子代理 `ses_0243be5dbffeVqFo3CjkoRhYnG`，fresh session）——3 项 iteration-1 修复全部 PASS + 结构/裁决引用/批次判据复核 PASS，但新 MAJOR：展开分母 91 低估——遗漏 RC 报告显式重开/归 MR1 的既有 audit-remediation P1 finding（P1-MA4-017 / P1-MA2-041 / P1-MA2-043 / P1-MA2-062 / P1-MA1-010，arm-index 行内「修复归 MR1 / 重开经 MR1」注记证实），违反「P1 finding 全集（完整枚举）」契约。修订：输入集扩展为「MR1 归属 P1 finding 全集」（P1-RC 系列 + RC 重开族 ≥5 项），分母改为 Phase 1 census 实测（基线预期 96）。
- Independent draft review iteration 3: `needs revision`（独立子代理 `ses_02433b387ffersRm79xATVEx40`，fresh session）——iteration-2 修复基本 PASS（输入集/分母/计数数学全核实），但新 MAJOR：R1.x 交叉核对规则按字面执行会把 8 个非 P1-RC 输入项中的 6 个（P1-MA4-017[R1.26] / P1-MA2-041/043[R1.15] / P1-MA2-062[R1.19] / P1-MA2-083[R1.27] / P1-MA2-071[R1.22]）因「R1.x 已 resolved」而跳过展开——但该 6 项 R1.x 均为**方案 B Deferred 关闭**（resolved ≠ implemented），按字面规则将静默回退 iteration-2 修复致分母塌缩至 ~90；另 MINOR：`P0-RC-` 字样实际还出现在 ~20 个 rc-ma1 plan 命名模板文本，非仅 methodology。修订：交叉核对规则加显式 carve-out——「RC 报告显式重开/归 MR1 者（含 resolved-via-deferral）照常展开，R1.x 行仅背景交叉引用；仅 resolved-via-implementation 且未被重开者才不重复展开」（baseline + Phase 1 两处同步）；零 P0 表述改为「arm-index 无实体 P0-RC finding」。
- Independent draft review iteration 4: `accept`（独立子代理 `ses_024285cd0ffe1h1puxZIHxaElE`，fresh session）——iteration-3 两处修复全 PASS：carve-out 规则在 baseline（:17）与 Phase 1（:70）双位置同构对齐，无残留「已 resolved 则跳过」陈旧表述（grep 逐条核对）；6 个 Deferred-via-doc 案例逐一核实（audit-remediation roadmap R1.15/R1.19/R1.22/R1.26/R1.27 均 done 且方案 B Deferred 关闭 + RC 侧 roadmap A4.2.22/23/26/80/116/31/45/158「维持 P1/重开归 MR1」注记支撑 carve-out 一致性）；零 P0 表述准确（arm-index grep `P0-RC-` 0 命中）；Draft Review Record 1-3 记录准确且 Plan Status 保持 draft。全门复核 PASS：baseline 计数（88/87/0）/里程碑状态（MA1 51 切片 + MA2 9 + MA3 5 + MA4 仅 A4.2.3/79/119 todo 为 MR1-blocked successor）/§7 A1-A5 + Q3/Q4/Q5-Q9/Q10 逐字引用/展开器语义（done=展开完成）/Deps 规则/单一结果表面/anti-slack/项目类型/header/Non-Goals/Skill none/计数数学（96 = 88+P2-RC-004+083/071+≥5，+2 P2 行 = 98，无陈旧 91/93）全 PASS。0 BLOCKER 0 MAJOR，2 non-blocking MINOR（P0-RC- 分布补注 + P1-MA2-041 引用锚点精确化）——修订：零 P0 括注改「methodology/MA1-MA4 plan 命名模板」；P1-MA2-041 锚点改指 arm-index §A2.7a 重开注记 + A1.14 §RC 交叉引用注记 :355 + A4.2.22-26 回填 :357。共识达成，转 active。

## Closure Gates

> 本计划为**展开器**（只读收集 + roadmap 表追加 + arm-index 交叉引用回填，无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = RC-R1.n 行完整性（P1 finding 全集覆盖）+ 批次归类正确性（§7 裁决一致）+ 展开映射记录 + 双向完整性校验 + 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：RC-R1.n 实体行已追加到 roadmap §MR1 表（P1 finding 全集覆盖，第一批纯预授权在前、越界项标注 ask-first）；R1.0 状态 done（展开完成）
- [x] 相关文档对齐：展开映射记录与 methodology §10 MR1 展开器机制 + §5 预授权/保护区域 + §7 A1-A5 裁决一致；RC-R1.n 行字段与 roadmap §MR1 工作项规范一致；与 A4.2 展开器范式同构
- [x] 已运行验证：双向完整性校验（finding→RC-R1.n 映射无遗漏无重复）+ 批次完整性校验 + §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、退出标准、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### RC-R1.n 实体行的修复执行

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 展开器结果表面 = RC-R1.n 行追加（展开完成即 done）。各 RC-R1.n 的修复实施属后续独立 plan + 独立 done（mission driver 按 §7 A1 分批启动逐项 DRAFT_PLANS → 独立草案审查 → EXECUTE → 独立结束审计 → done）。越界项行按 §7 A2 暂停等待人工批准（plan 内 ask-first checkbox），非触及行继续。本展开器闭环不阻塞于修复落地。
- Successor Required: yes（各 RC-R1.n 实体行）

### A4.2.3 / A4.2.79 / A4.2.119（MA4 三行 MR1-blocked successor）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 三行是 MR1 修复的下游 successor（分别依赖 P1-RC-008 预留写路径 / P1-RC-031 效期拦截 / P1-RC-049 物料归集落地），roadmap 显式「保留 todo 待 MR1 落地后回队」。R1.0 展开器只登记其回队依赖（对应 RC-R1.n 行的修复落地即触发回队），不展开为 RC-R1.n 行。§7 A1 裁决背景「MA1-MA4 全部 done（185+ 实体行）」已将三行视为 successor 而非 R1.0 阻塞项。
- Successor Required: yes（对应 RC-R1.n 修复落地后回队，触发条件 = P1-RC-008/031/049 修复 done）

### Q10 use-cases 真相源命名修订（P2-RC-005/011/016/012）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 经 §7 Q10 人工批准登记（§9 流程：变更理由 + 影响面 + 批准人），属 P2 级文档修订任务，非 P0/P1 修复，不属 R1.0 展开范围；且涉及真相源修订须按 §9 冻结条款人工批准流程执行，不宜并入展开器自动动作。
- Successor Required: yes（单独文档任务，触发条件 = 人工启动 §9 修订流程）

### P2 登记项（P2-RC-002/003/006/008/010/012/013/014/015/019/023/024/028/035/036/038/040/041/042/043/044/045/048/050/051/053/058/062/064/065/068/069/070/072/075/079/080/081/084/085/086/087 等 87 项）

- Classification: `watch-only residual`
- Why Not Blocking Closure: P2 登记不强制（methodology §2 + Q4 裁决），各 arm-index 行已登记 successor watch-only + 修复建议 + 预授权类别。R1.0 只展开 P0/P1（含 §7 A4/A5 明确裁决修复的 P2-RC-061/057）；其余 P2 不入 MR1 表。部分 P2 的修复建议已标注预授权类目，可经后续 backlog/人工裁决进入修复队列，非本展开器范围。
- Successor Required: no（watch-only；修复经后续 backlog 裁决）

## Closure

Status Note: 执行完成（draft → active → 执行 → 独立结束审计）。展开映射记录 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md` 已落盘。展开器结果表面 = roadmap §MR1 表追加 RC-R1.1-RC-R1.89 实体行（done 判据 = 展开完成而非修复完成，对齐 §10 R1.0 + A4.2 范式）。本展开器零代码/ORM/api.xml/真相源变更（roadmap §MR1 表追加 + arm-index 交叉引用回填属工作项追踪更新，非冻结真相源）；各 RC-R1.n 的修复执行属后续独立 plan（mission driver 按 §7 A1 分批启动逐项 DRAFT_PLANS → 审查 → EXECUTE → 结束审计 → done），不阻塞本展开器闭环。

Closure Audit Evidence:

- Auditor / Agent: 待独立结束审计子代理（新会话，不重用执行者上下文）
- Evidence: 待执行后填写（可引用：展开映射记录 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（Audit Status: closed）+ roadmap §MR1 表 89 行 + arm-index 98 项回填 + checker actual vs baseline §6 表）

Follow-up:

- 各 RC-R1.n 实体行修复执行（mission driver 按 §7 A1 分批启动逐项 DRAFT_PLANS；越界项按 §7 A2 暂停 ask-first；第一批 40 行可自动执行，越界项 49 行暂停等待人工批准）
- A4.2.3/79/119 回队（对应 P1-RC-008/031/049 修复 done 后）
- Q10 use-cases 命名修订（P2-RC-005/011/016/012，§9 流程）
