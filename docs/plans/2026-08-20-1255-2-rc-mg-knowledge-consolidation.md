# 2026-08-20-1255-2-rc-mg-knowledge-consolidation MG 知识沉淀与守卫强化

> Plan Status: active
> Mission: requirement-compliance
> Work Item: MG G.1 + G.2 + G.3（知识沉淀与守卫强化）
> Last Reviewed: 2026-08-20
> Source: `docs/backlog/requirement-compliance-roadmap.md` MG 节（G.1/G.2/G.3 全 todo；deps `MV done`）
> Related: 前置计划 `2026-08-20-1255-1-rc-mv-full-verification-closure-audit.md`（MV completed 是本计划启动前提）；方法论主体 `docs/audits/requirement-compliance-methodology.md`；skill 路由壳 `docs/skills/requirement-compliance-audit-prompt.md`
> Audit: required

## Current Baseline

（2026-08-20 实仓核验）

- **lessons 现状**：`docs/lessons/` 已有 11 课 + README 索引（01 ORM 跨模块表前缀 / 02 cross-ref 重编号 / 03 doc-status 命名 / 04 BizModel 契约与测试 / 05 E2E 日志优先诊断 / 06 codegen 产品编辑覆盖 / 07 compliance 基线漂移 / 08 无独立审计闭包 / 09 过账吞异常悬挂 / 10 dict 死状态 / 11 index 状态未回填）。roadmap G.1 点名的两类模式——**文档化简化滥用**（documented simplification 掩盖需求-实现分歧）与**需求基线陈旧**（product-scope/use-cases 漂移未被审计发现）——均未覆盖。
- **mission 期高频模式候选**（提炼输入，须与既有 11 课去重）：config-gate「部署启用决策 vs 契约缺失」认定范式（A4.1.4 起跨 ≥10 切片复用）；xbiz XScript try/catch 不可行须下沉 Java Bean（M4.64 机制注记 + RC-R1.89 D2）；死常量/死列激活范式（RC-R1.89 `BILL_DATA_*_ER`）；dangling dict 值反模式（RC-R1.73 D3——与 lesson 10 部分重叠，须划界非重复）；「owner doc 表述过时未随实现更新」（RC-R1.89 纠正 payroll.md「hr 模块零 xbiz」过时表述）。
- **G.2 技能现状**：`docs/skills/requirement-compliance-audit-prompt.md` 已存在（M0.1 期建立的 methodology 路由薄壳），方法论主体 `docs/audits/requirement-compliance-methodology.md` §1-§10 覆盖 MA1-MA4 **审计阶段**方法。MR1 修复阶段方法回流状态分两档：**已进方法论**——双独立子 agent 批准规程（§5 保护区域暂停协议，已引用 2026-08-15 裁决）与 R1.0 展开器机制（§10，RC-R1.n 命名/done 判据/占位符禁令），但实践约定散落各 plan；**完全未回流**——A/B/C 批量裁决范式（2026-08-12，仅存 roadmap 头部裁决段）与 compliance baseline-raise per-site 证据范式（methodology/skill 零命中）。
- **G.3 上下文现状**：`docs/context/project-context.md` §已知失败模式（速查）现 4 条（基线漂移 / closure-pending 缺独立审计 / @Inject private / 过账吞异常）；§当前项目阶段仍写「当前重点：需求-实现符合性审计修复批次」——mission 收口后该表述过时。`README.md` 已知失败模式段为指针型（指向 project-context.md 权威，避免第二漂移真相源）。

## Goals

- G.1：新失败模式提炼落 `docs/lessons/`——至少覆盖 roadmap 点名两类（文档化简化滥用 / 需求基线陈旧），并扫描 mission 证据补充高频模式，与既有 11 课显式去重划界。
- G.2：需求符合性审计方法闭环——MR1 修复阶段方法回流裁决（A/B/C 批量裁决 / baseline-raise per-site 证据属完全未回流；双批准规程 / 展开器机制属已进方法论待实践约定补齐），回流至 methodology 主体与 skill 路由壳，保持 skill 薄壳定位（NG2 不搬主体）。
- G.3：`project-context.md` 已知失败模式速查增补 + 当前项目阶段段更新；`README.md` 指针一致性核验。
- roadmap G.1-G.3 → done（mission 路线图全部行闭合）。

## Non-Goals

- 不改产品代码 / 测试 / ORM / 契约（纯文档知识沉淀）。
- 不复跑任何审计或验证命令（消费 MV 计划产出的事实）。
- 不做 plans 批量归档（AGENTS.md §14 人工批准约束，季度审查另行触发）。
- 不裁决 mission 是否完成（engine 依审计轮次决定）。
- 不新建通用审计 prompt 技能（G.2 是更新既有 requirement-compliance 技能族，非新建）。

## Task Route

- Type: `app-layer design change`（文档知识沉淀——owner docs 与方法论更新）
- Owner Docs: `docs/lessons/README.md`（课目格式与索引）+ `docs/skills/README.md`（技能注册表）+ `docs/audits/requirement-compliance-methodology.md`（G.2 主体）+ `docs/context/project-context.md`（G.3）
- Skill Selection Basis: 文档提炼与写作对齐各 README 既有格式，不加载实现类技能；Skill: none。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。

## Execution Plan

### Phase 1 - G.1 失败模式提炼

Status: planned
Targets: `docs/lessons/*.md` + `docs/lessons/README.md`
Skill: none

- Item Types: `Add`
- Prereqs: 计划 2026-08-20-1255-1（MV）completed

- [ ] Add: 新 lesson 覆盖 roadmap 点名两类——①文档化简化滥用（documented simplification / Deferred 标注掩盖需求-实现分歧；案例引用 MA2 复查与 MR1 重开族，如 P1-MA2-083 承付恢复不对称、P1-RC-025 换货缺失）②需求基线陈旧（product-scope/use-cases 漂移；案例引用 0.2 修正与 UC 标题重复裁决）——每课含模式定义 / 触发场景 / 案例 file:line 或 plan id 引用 / 自检规避清单，对齐既有课目格式
      - Skill: none
- [ ] Add: 扫描 mission 证据（MA1-MA4 报告 + MR1 89 plans + `docs/logs/2026/08-*`）提炼补充高频模式（Current Baseline 列出的 5 个候选逐一裁决：入课 / 划界归并既有课 / 不入课说明理由）；与既有 11 课重叠面（lesson 07/08/10）显式划界
      - Skill: none
- [ ] Add: `docs/lessons/README.md` 索引更新
      - Skill: none

Exit Criteria:

- [ ] 至少 2 个新 lesson 落盘（点名两类必含）且索引一致；补充候选逐项裁决记录在计划执行注记

### Phase 2 - G.2 审计方法回流

Status: planned
Targets: `docs/audits/requirement-compliance-methodology.md` + `docs/skills/requirement-compliance-audit-prompt.md`
Skill: none

- Item Types: `Decision | Add`
- Prereqs: Phase 1 完成（提炼输入就绪）

- [ ] Decision: 回流范围裁决——MR1 阶段方法四项（A/B/C 批量裁决范式 / 双独立子 agent 批准操作规程 / baseline-raise per-site 证据范式 / R1.0 展开器→RC-R1.n 展开机制）逐项裁决去向：进 methodology 主体（新增节）/ 进 skill 路由壳使用场景 / 已覆盖仅补实践约定引用；裁决理由与替代方案记录（起点 = Current Baseline 两档现状：**双批准规程与展开器机制已进 §5/§10（待实践约定补齐）；A/B/C 批量裁决与 baseline-raise per-site 证据零回流**）
      - Skill: none
- [ ] Add: 按 D 裁决更新 methodology 与 skill（保持 skill 薄壳路由定位；skill 与 methodology 冲突时以 methodology 为准的声明段同步校验）
      - Skill: none

Exit Criteria:

- [ ] 四项方法在 methodology/skill 中可路由到（新增节或既有引用），skill 声明段一致性无漂移

### Phase 3 - G.3 上下文文件更新

Status: planned
Targets: `docs/context/project-context.md` + `README.md` + roadmap MG 行
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1-2 完成（速查条目从新 lesson 提炼）

- [ ] Add: `project-context.md` §已知失败模式（速查）增补——速查入选判据 = Phase 1 新课中「roadmap 点名两类之一」或「模式在 mission 证据中跨 ≥2 域复现」的条目（逐条裁决记录，精简内联摘要风格，详案指向 lesson 文件）；§当前项目阶段更新（requirement-compliance 批次完成、当前重点回归 AGENTS.md 既有表述：看板运行时视觉/浏览器回归 + 各域细化端到端验证）
      - Skill: none
- [ ] Add: `README.md` 已知失败模式指针一致性核验（断链 / 过时表述修正，不改指针型设计）；roadmap G.1/G.2/G.3 → done
      - Skill: none

Exit Criteria:

- [ ] project-context.md 两段更新落盘且与 AGENTS.md §当前项目阶段不矛盾；README 指针有效；roadmap MG 三行 done

## Draft Review Record

- Independent draft review iteration 1: needs revision (ses_fe2764d66ffeWfVKURgQlGK650) because G.2 基线伪事实——4 项方法中双批准规程（§5 已引 2026-08-15 裁决）与展开器机制（§10）实际已进方法论，仅 A/B/C 批量裁决与 baseline-raise per-site 证据零回流，两档现状未区分；另 2 项 MINOR（Deferred 注记错指 Phase 2、速查入选判据未定）
- Independent draft review iteration 2: needs revision (ses_fe2712375ffezwnk80MfBFanIm) after 两档基线/Goals/Phase 2 起点改写与 MINOR 两项修复落地，但 Phase 2 Decision 括注「后两项已进 §5/§10，前两项零回流」相对四项枚举顺序错位索引（正确映射 = 双批准+展开器在、A/B/C+per-site 零），与自身枚举矛盾；1 项 MINOR（iteration 1 记录未回填）
- Independent draft review iteration 3: accept (ses_fe26c9e96ffegNO0XyU941rV8S) after 括注错位改写为显式两档陈述（与 Current Baseline/Goals/实仓证据逐字一致：methodology §5:210 引 2026-08-15 双批准、§10:359 展开器、per-site/批量裁决零命中）+ Review Record 回填；全计划复检无新增问题。共识达成，Plan Status → active。

## Closure Gates

> 文档型计划（零代码/测试/契约变更）——删除 typecheck/build/lint/test 验证命令门控，以文档一致性检查替代：所有新增/修改文档与其索引、声明段、指针交叉一致。

- [ ] 范围内行为完成（G.1 新课 + G.2 回流 + G.3 更新）
- [ ] 相关文档对齐（lessons/README + skills/README + methodology 声明段 + project-context + roadmap MG 三行 done）
- [ ] 文档一致性检查通过（索引↔文件↔指针↔声明段零断链，`rg` 抽验新引用可达）
- [ ] 无范围内项目降级为 deferred/follow-up（补充候选不入课者须带理由裁决记录，非模糊降级）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中
- [ ] `docs/logs/2026/`（执行当日）日志条目

## Deferred But Adjudicated

### 补充模式候选中裁决不入课的条目

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 候选模式若频度/复用性不足（单例个案），入课反而稀释信噪比；Phase 1 执行注记逐项记录不入课理由
- Successor Required: no（触发条件：同类模式再次出现时重开提炼）

## Closure

Status Note: <pending>

Closure Audit Evidence:

- Auditor / Agent: <pending>
- Evidence: <pending>

Follow-up:

- <pending>
