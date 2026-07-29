# 2026-07-29-1923-2-r2-0-mr2-p1-finding-expansion R2.0 — MR2 P1 发现展开为具体修复工作项行

> Plan Status: completed
> Last Reviewed: 2026-07-29
> Source: `docs/backlog/audit-remediation-roadmap.md` §MR2 R2.0
> Related: `docs/audits/arm-index.md` §P1 发现汇总 + §P1 详细清单；`docs/plans/00-plan-authoring-and-execution-guide.md` §R*.0 展开机制
> Audit: required

## Current Baseline

- MA3（文档-实现一致性审计 8 项）+ MA4（代码质量审计 9 项）全部 `done`
- `docs/audits/arm-index.md` §P1 详细清单已登记 MA3 P1=52 项（P1-MA3-001~061）+ MA4 P1=25 项（P1-MA4-001~025），每项含「目标 MR」列
- **MR2 归属精确边界**：arm-index 中 MA3/MA4 发现的「目标 MR」分布——
  - MA3 纯 MR2：P1-MA3-001~024/026~038/040~061（50 项；P1-MA3-025/039 为 MR2+MR1 双标，文档侧归 MR2 代码侧归 MR1）
  - MA4 纯 MR2：P1-MA4-002/005/009/011/014/019/021（测试有效性 7 项）+ P1-MA4-023/024/025（view.xml drift 3 项）= 10 项
  - MA4 明确「不重复计入 MR2」的交叉项：P1-MA4-003/006/008/012/015/022（daoFor 家族，MR1 一并裁决）+ P1-MA4-001/004/007/010/013/016/017/018/020（业财悬挂/算术，MR1）= 15 项排除
- roadmap §MR2 表仅含 R2.0 + R2.x 占位行，无具体修复工作项行
- MR2 归属 P1 finding 合计 ~60 项（MA3 50 + MA4 10），按根因/域分组后预期 ~12-15 个逻辑工作项

## Goals

- 将 arm-index.md 中全部「目标 MR = MR2」的 P1 发现展开为 roadmap §MR2 表中的具体修复工作项行（R2.1, R2.2...）
- 工作项按根因/修复方式/域分组，每行含 finding ID 覆盖范围 / 域 / 修复范围 / Skill
- 同步更新 arm-index.md 每项 MR2 finding 的「修复状态」列交叉引用其归属的 R2.x 工作项 ID
- 更新 roadmap R2.0 Status `todo`→`done`

## Non-Goals

- 实际文档/代码/view.xml 修复（属 R2.x 工作项，R2.0 仅展开不修复）
- R1.0/R3.0 展开（不同 MR 里程碑）
- MR4 跨维度裁决（须 MR1+MR2+MR3 全 done 后）
- MA4 交叉归属 MR1 的 15 项（P1-MA4-001/003/004/006/007/008/010/012/013/015/016/017/018/020/022）——由 R1.0 展开
- P1-MA3-025/039 的 MR1 侧（代码核实/折算路径）——由 R1.0 展开；本 plan 仅展开其 MR2 侧（文档更新）
- 重新审计或重新评级既有 finding

## Task Route

- Type: `implementation-only change`（roadmap + arm-index 文档展开）
- Owner Docs: `docs/audits/arm-index.md` §P1 详细清单
- Skill Selection Basis: none — 本任务是索引/文档展开，不涉及 ORM/BizModel/view/测试代码

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 - 清点 MR2 归属发现

Status: completed
Targets: `docs/audits/arm-index.md`
Skill: none

- Item Types: Proof
- Prereqs: MA3+MA4 done（已满足）

- [x] Proof: 从 arm-index.md §P1 详细清单提取全部「目标 MR = MR2」的 finding（排除明确标注「不重复计入 MR2」的 15 项 MA4 交叉项；P1-MA3-025/039 取其 MR2 文档侧），按维度分组计数（文档质量/owner-doc drift/测试有效性/view.xml drift），产出清点表（finding ID / 域 / 根因类型 / 修复方式文档/代码）
  - Skill: none

#### Phase 1 清点表（MR2 归属 P1 finding，精确 62 项）

| 维度 | 来源审计 | Finding ID 集合 | 数量 | 域 | 修复方式 |
|------|---------|----------------|------|-----|---------|
| 文档质量-设计基线 | MA3-A3.1 | P1-MA3-001~007 | 7 | 全域/finance/master-data/8扩展域/logistics/contract+cs/dashboards | 文档 |
| 文档质量-全局视图 | MA3-A3.1+A3.2 | P1-MA3-008~013, 022, 023 | 8 | app-overview/roles/guidelines/flow-overview/domain-glossary/product-scope | 文档 |
| owner-doc drift-finance 状态码/字段语义 | MA3-A3.3 | P1-MA3-024, 026~029 | 5 | finance | 文档 |
| owner-doc drift-finance 过账/事务/承付/多币种语义 | MA3-A3.3 | P1-MA3-025(MR2侧), 030~032, 039(MR2侧) | 5 | finance | 文档 |
| owner-doc drift-finance 配置键/门控 | MA3-A3.3 | P1-MA3-033~038 | 6 | finance | 文档 |
| owner-doc drift-mfg | MA3-A3.4 | P1-MA3-040~045 | 6 | manufacturing | 文档 |
| API 契约 | MA3-A3.6 | P1-MA3-046~049 | 4 | 全 19 域 | 文档（含权限注解裁决） |
| 索引路由 | MA3-A3.7 | P1-MA3-050~052, 054~056 | 6 | docs/index+articles+bugs+logs+errors+ppts | 文档 |
| 定制能力 | MA3-A3.8 | P1-MA3-057~061 | 5 | docs/architecture/customization-capabilities | 文档 |
| 测试有效性-finance | MA4-A4.1a/b | P1-MA4-002, 005 | 2 | finance | 代码（测试） |
| 测试有效性-mfg | MA4-A4.2a/b | P1-MA4-009, 011 | 2 | manufacturing | 代码（测试） |
| 测试有效性-assets | MA4-A4.3 | P1-MA4-014 | 1 | assets | 代码（测试） |
| 测试有效性-hr | MA4-A4.4 | P1-MA4-019 | 1 | hr | 代码（测试） |
| 测试有效性-pur+sal+inv | MA4-A4.5 | P1-MA4-021 | 1 | pur+sal+inv | 代码（测试） |
| view.xml drift | MA4-A4.6/7/8 | P1-MA4-023, 024, 025 | 3 | mfg/pur/hr | 代码（view.xml） |
| **合计** | | | **62** | | |

**排除校验**（明确标注「不重复计入 MR2」的 15 项 MA4 交叉项，目标 MR=MR1）：P1-MA4-001/003/004/006/007/008/010/012/013/015（业财悬挂+daoFor 家族，归 R1.5/R1.16）+ P1-MA4-016/017/018（hr 业财算术/过账，归 R1.26）+ P1-MA4-020（inv reverse 悬挂，归 R1.16）+ P1-MA4-022（pur+sal+inv daoFor，归 R1.5）= 15 项，均未出现在 MR2 表中。

Exit Criteria:

- [x] MR2 归属 finding 清点表产出（含精确总数 62 + 按维度预分组），可追溯每项到 arm-index §P1 详细清单行

### Phase 2 - 分组并写入 R2.x 工作项行

Status: completed
Targets: `docs/backlog/audit-remediation-roadmap.md` §MR2 表
Skill: none

- Item Types: Add | Decision
- Prereqs: Phase 1 清点完成

- [x] Decision: 确定分组粒度规则并在 plan 中记录——按根因/修复方式/域组织；设计文档执行状态泄漏（P1-MA3-001~007）按文档集群合并；finance owner-doc drift 配置键/状态码/语义（P1-MA3-024~038）按修复协同性合并或分列；mfg owner-doc drift（P1-MA3-040~045）单列；API 契约/权限/索引路由/定制能力各单列；测试有效性（P1-MA4-002/005/009/011/014/019/021）按域分列；view.xml drift（P1-MA4-023/024/025）单列
  - Skill: none

  **分组裁决（产出 15 个 R2.x 工作项，文档质量 → owner-doc drift → API 契约 → 索引 → 定制 → 测试有效性 → view.xml drift 顺序）**：

  | R2.x | 覆盖 finding | 粒度理由 |
  |------|-------------|---------|
  | R2.1 | P1-MA3-001~007 | A3.1 设计文档基线"实现状态泄漏/架构混合"集群（7），同根因（dim 2/3/5）协同 |
  | R2.2 | P1-MA3-008~013, 022, 023 | A3.1+A3.2 全局视图缺位 + 重复真相源（8），8 第二批扩展域四联投影协同（dim 4/5/6/9） |
  | R2.3 | P1-MA3-024, 026~029 | finance 状态机/字典/字段语义（5），doc↔doc↔code blocker 类 |
  | R2.4 | P1-MA3-025(MR2), 030~032, 039(MR2) | finance 过账/事务/承付/多币种语义（5），含 MR1 双标项文档侧 |
  | R2.5 | P1-MA3-033~038 | finance 配置键/门控（6），config 表 drift 同型 |
  | R2.6 | P1-MA3-040~045 | mfg owner-doc drift（6），单域单列 |
  | R2.7 | P1-MA3-046~049 | API 契约（4），含权限保护（与 A6.1/A6.2 协同）+ 命名约定 + 影子契约 + RPC 面 |
  | R2.8 | P1-MA3-050~052, 054~056 | 索引路由（6），docs 索引结构同型 |
  | R2.9 | P1-MA3-057~061 | 定制能力（5），customization-capabilities.md 单 owner doc |
  | R2.10 | P1-MA4-002, 005 | finance 测试有效性（2），同域 |
  | R2.11 | P1-MA4-009, 011 | mfg 测试有效性（2），同域 |
  | R2.12 | P1-MA4-014 | assets 测试有效性（1），单域 |
  | R2.13 | P1-MA4-019 | hr 测试有效性（1），单域 |
  | R2.14 | P1-MA4-021 | pur+sal+inv 测试有效性（1），A 级合并同型 |
  | R2.15 | P1-MA4-023, 024, 025 | view.xml drift（3），代码变更须独立 plan-audit |

- [x] Add: 向 roadmap §MR2 表追加 R2.1~R2.15 具体工作项行（每行含 `#` / Work Item 描述 / `Status=todo` / Owner Doc / `Deps=R2.0` / Skill），替换 R2.x 占位行；新增行按维度排序（文档质量 → owner-doc drift → API 契约 → 索引/定制 → 测试有效性 → view.xml drift）
  - Skill: none

Exit Criteria:

- [x] roadmap §MR2 表含具体 R2.1~R2.15 行（无占位），每行 Work Item 描述含覆盖的 finding ID 集合
- [x] view.xml drift 工作项行（R2.15）明确标注属代码变更（view.xml 修复 = code change，须独立 plan-audit）+ 测试有效性行（R2.10~R2.14）标注 `[代码]`

### Phase 3 - 回填 arm-index 交叉引用 + 双向完整性校验

Status: completed
Targets: `docs/audits/arm-index.md`
Skill: none

- Item Types: Add | Proof
- Prereqs: Phase 2 R2.x 行写入

- [x] Add: 更新 arm-index.md §P1 详细清单每项 MR2 finding 的「修复状态」列，追加归属 R2.x 工作项 ID（如 `todo (R2.3)`）
  - Skill: none

  **执行证据**：经脚本精确更新 62 行（60 非 dual 行尾 `| MR2 | todo (R2.x) |` + 2 dual 行 P1-MA3-025/039 更新为 `MR2+MR1 todo (MR1侧 R1.9 / MR2侧 R2.4)`）。更新后 grep 确认零残留 `| MR2 | todo |` 裸态。

- [x] Proof: 双向完整性校验——(a) 正向：MR2 归属 finding 集合中每一项都能在 roadmap §MR2 表找到覆盖它的 R2.x 行；(b) 反向：每个 R2.x 行覆盖的 finding 集合无遗漏无重复；(c) 排除校验：明确标注「不重复计入 MR2」的 15 项 MA4 交叉项确实未出现在 MR2 表中
  - Skill: none

  **校验结果（全部通过）**：
  - (a) 正向：62 项 MR2 finding 全部含 R2.x 交叉引用（0 残留裸 `| MR2 | todo |` + 2 dual 已含 MR2侧 R2.4）
  - (b) 反向：R2.1=7 / R2.2=8 / R2.3=5 / R2.4=5（3 normal+2 dual）/ R2.5=6 / R2.6=6 / R2.7=4 / R2.8=6 / R2.9=5 / R2.10=2 / R2.11=2 / R2.12=1 / R2.13=1 / R2.14=1 / R2.15=3 = **62 项**，与 Phase 1 清点精确一致，无遗漏无重复
  - (c) 排除：15 项 MR1 MA4 交叉项（P1-MA4-001/003/004/006/007/008/010/012/013/015/016/017/018/020/022）R2.x 引用计数全部 = 0，确实未出现在 MR2 表中

Exit Criteria:

- [x] arm-index 每项 MR2 finding「修复状态」列含 R2.x 交叉引用
- [x] 双向完整性校验通过（含交叉排除校验）

## Draft Review Record

- Independent draft review iteration 1: accept (ses_0525e3ac6ffenFjtk31AoCBgvn) — baseline 准确（MA3 52 P1 + MA4 25 P1 全部在 arm-index §P1 详细清单登记含目标 MR 列），MR2 scope 边界精确正确（15 项排除 MA4 交叉项逐项验证 目标 MR=MR1 + 6 项 daoFor 明确「不重复计入 MR2」标注确认 + P1-MA3-025/039 双标 MR2+MR1 确认），无 slack 语言无反模式

## Closure Gates

> 本 plan 无代码/ORM 变更（仅 roadmap + arm-index 文档展开），删除 mvn/build/test 验证门控。完整性校验替代。

- [x] 范围内行为完成（roadmap §MR2 表含全部具体工作项行 R2.1~R2.15 + arm-index 交叉引用回填 62 项）
- [x] 相关文档对齐（roadmap R2.0 Status `todo`→`done` + arm-index 修复状态更新）
- [x] 双向完整性校验通过（含交叉排除校验——正向 62 项全覆盖 / 反向 15 行无遗漏无重复合计 62 / 排除 15 项 MR1 交叉项零 R2.x 引用）
- [x] 无范围内项目降级为 deferred/follow-up（P1-MA3-025/039 的 MR1 侧明确移出范围归 R1.0，非本 plan deferred）
- [x] 独立草案审查已完成并记录（Draft Review Record iteration 1 accept）
- [x] 文本一致性已验证（状态、阶段、门控一致——3 Phases 全 completed + Plan Status completed + R2.0 done）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计（closure audit 见下）
- [x] 结束证据存在于文件中（见 Closure Audit Evidence）

## Deferred But Adjudicated

（无——R2.0 无 deferred 项；P1-MA3-025/039 的 MR1 侧明确移出范围归 R1.0）

## Closure

Status Note: completed (closure audit PASS by independent subagent ses_05243ab01ffevnzvgq5OCP3rxu, 2026-07-29)

Closure Audit Evidence:

- Auditor / Agent: 独立子代理 ses_05243ab01ffevnzvgq5OCP3rxu（新会话，非执行者）
- Verdict: **CLOSURE AUDIT: PASS**（全部 4 项检查通过——计划完成 / roadmap §MR2 表 R2.1~R2.15 + R2.0 done + 占位符移除 / arm-index 62 项交叉引用双向一致[正向 0 裸态 + 反向 15 行合计 62 + 排除 15 项零引用 + dual 2 项正确] / 覆盖完整性 MA3 52 + MA4 10 = 62 无遗漏无重复）

Follow-up:

- 无非阻塞跟进项（R2.1~R2.15 工作项 Status=todo，待 DRAFT_PLANS 后续起草修复 plan；P1-MA3-025/039 MR1 侧归 R1.9 已由 R1.0 展开）
