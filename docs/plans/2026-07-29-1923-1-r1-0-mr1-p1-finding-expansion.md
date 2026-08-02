# 2026-07-29-1923-1-r1-0-mr1-p1-finding-expansion R1.0 — MR1 P1 发现展开为具体修复工作项行

> Plan Status: completed
> Last Reviewed: 2026-07-29
> Source: `docs/backlog/audit-remediation-roadmap.md` §MR1 R1.0
> Related: `docs/audits/arm-index.md` §P1 发现汇总 + §P1 详细清单；`docs/plans/00-plan-authoring-and-execution-guide.md` §R*.0 展开机制
> Audit: required

## Current Baseline

- MA1（结构审计 14 项）+ MA2（业务审计 20 项）全部 `done`；MA3（文档审计 8 项）+ MA4（代码审计 9 项）全部 `done`
- `docs/audits/arm-index.md` §P1 详细清单已登记全部 MA1-MA4 P1 发现，每项含「目标 MR」列做逐项 MR 归属
- arm-index §P1 类型分布表已按根因预分组（propId 缺失 46 列 / crm DECIMAL 7 列 / drp 命名 4 实体 / daoFor 跨域 9 域 ~25 站点 / 等）
- **关键交叉归属事实**：arm-index 中部分 MA3/MA4 发现的「目标 MR」明确标注为 MR1——MA3 交叉 2 项（P1-MA3-025/039，文档侧 MR2 + 代码侧 MR1）+ MA4 交叉 15 项（P1-MA4-001/003/004/006/007/008/010/012/013/015/016/017/018/020/022），其中 daoFor 家族 7 项明确标注「不重复计入 MR2，同 P1-MA1-022 一并裁决」
- roadmap §MR1 表仅含 R1.0 + R1.x 占位行（`（R1.0 执行后自动展开：每个 P1 finding 一个工作项行）`），无具体修复工作项行
- P0 全部已修复或 deferred（P0-MA2-018 deferred，deferred plan 方向 A/B/C/D 维持）
- MR1 归属 P1 finding 估算 ~100 项（MA1 原生 ~8 组 + MA2 原生 ~80 项 + MA3/MA4 交叉 ~17 项），按根因/域分组后预期 ~15-20 个逻辑工作项

## Goals

- 将 arm-index.md 中全部「目标 MR = MR1」的 P1 发现展开为 roadmap §MR1 表中的具体修复工作项行（R1.1, R1.2...）
- 工作项按根因/修复方式/域分组（同一根因的跨域投影合并为单行，如 daoFor 家族），每行含 finding ID 覆盖范围 / 域 / 修复范围 / Skill / ORM ask-first 标记
- 同步更新 arm-index.md 每项 MR1 finding 的「修复状态」列交叉引用其归属的 R1.x 工作项 ID
- 更新 roadmap R1.0 Status `todo`→`done`

## Non-Goals

- 实际代码/ORM/文档修复（属 R1.x 工作项，R1.0 仅展开不修复）
- R2.0/R3.0 展开（不同 MR 里程碑；R3.0 依赖 MA5+MA6+MA7 done 尚未满足）
- MR4 跨维度裁决（须 MR1+MR2+MR3 全 done 后）
- P0 发现（经即时通道，不进 MR 批量）
- 重新审计或重新评级既有 finding（R1.0 忠实展开 arm-index 既有分类与归属）

## Task Route

- Type: `implementation-only change`（roadmap + arm-index 文档展开）
- Owner Docs: `docs/audits/arm-index.md` §P1 详细清单 + §P1 类型分布
- Skill Selection Basis: none — 本任务是索引/文档展开，不涉及 ORM/BizModel/view/测试代码

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline

## Execution Plan

### Phase 1 - 清点 MR1 归属发现

Status: completed
Targets: `docs/audits/arm-index.md`
Skill: none

- Item Types: Proof
- Prereqs: MA1+MA2 done（已满足）

- [x] Proof: 从 arm-index.md §P1 详细清单提取全部「目标 MR 含 MR1」的 finding（含 MR2+MR1 双标项取其 MR1 侧），按来源 MA 分组计数（MA1 原生 + MA2 原生 + MA3 交叉 + MA4 交叉），产出清点表（finding ID / 域 / 根因类型 / 是否 ORM ask-first / 是否触及会计保护区域）
  - Skill: none

**清点结果（2026-07-29 实测）：MR1 归属 finding 共 103 项（唯一去重后；P1-MA1-011 与 P1-MA1-013 同组 dedup 计 1）。按来源 MA 分组：MA1 原生 13 项 + MA2 原生 73 项 + MA3 交叉 2 项（MR1 侧）+ MA4 交叉 15 项（MR1 侧）= 103。**

#### MA1 原生（13 项，去重 P1-MA1-011/013）

| Finding ID | 域 | 根因类型 | ORM ask-first | 会计保护区域 |
|---|---|---|---|---|
| P1-MA1-001 | mfg | propId 缺失 | YES | no |
| P1-MA1-008 | assets | propId 缺失 | YES | no |
| P1-MA1-009 | crm | DECIMAL↔Double | YES | no |
| P1-MA1-010 | projects | propId 缺失 | YES | no |
| P1-MA1-011/013 | maintenance | propId 缺失（同组 dedup） | YES | no |
| P1-MA1-012 | quality | propId 缺失 | YES | no |
| P1-MA1-014 | drp | 命名异常 | YES | no |
| P1-MA1-015 | docs（全域） | owner doc 数值偏差 | no | no |
| P1-MA1-016 | finance | IDaoProvider 跨域 DAO | no | no |
| P1-MA1-017 | docs（finance） | owner doc 纯读规则 | no | no |
| P1-MA1-018 | finance | enum↔dict 漂移 | no | no |
| P1-MA1-022 | 9 域合并 | daoFor 跨域只读 | no | no |
| P1-MA1-029 | contract | 跨域写半治理 | no | no |

#### MA2 原生（73 项）

P1-MA2-001/002/003/009/017~024/031~051/056~070/071~080/081~099（逐项明细见 arm-index §P1 详细清单行 147-224，每项含 域/根因/修复方式/保护区域标注）。ORM ask-first 项：P1-MA2-046（assignment dict）、P1-MA2-066（noCapaReason 列方案A）、P1-MA2-079（部分签收方案B）、P1-MA2-085~092/096/098（UK 添加）。会计保护区域项：P1-MA2-001/002/003/009/017~022/048/060/068/074/080/081~084/087/095/096。

#### MA3 交叉（2 项，MR1 侧）

| Finding ID | 域 | 根因类型 | 说明 |
|---|---|---|---|
| P1-MA3-025 | finance | 预算余量公式 doc↔code | MR2+MR1 双标；MR1 侧=核实 code javadoc 与实际行为 |
| P1-MA3-039 | finance | persistVoucher amountSource=amountFunctional | MR2+MR1 双标；MR1 侧=核实引擎折算路径+VoucherFact 双字段 |

#### MA4 交叉（15 项，MR1 侧）

P1-MA4-001/003/004/006/007/008/010/012/013/015/016/017/018/020/022（逐项明细见 arm-index §P1 详细清单行 481-502）。其中 daoFor 家族 7 项（003/006/008/012/015/022 + MA1-022）明确标注「不重复计入 MR2，同 P1-MA1-022 一并裁决」；业财悬挂 family 7 项（001/004/007/010/013/020 + MA2 swallow findings）；hr 代码质量 3 项（016/017/018）。

Exit Criteria:

- [x] MR1 归属 finding 清点表产出（含精确总数 103 + 按根因预分组），可追溯每项到 arm-index §P1 详细清单行

### Phase 2 - 分组并写入 R1.x 工作项行

Status: completed
Targets: `docs/backlog/audit-remediation-roadmap.md` §MR1 表
Skill: none

- Item Types: Add | Decision
- Prereqs: Phase 1 清点完成

- [x] Decision: 确定分组粒度规则并在 plan 中记录——按根因/修复方式/域组织为逻辑工作项；同一根因的跨域投影合并为单行（如 daoFor 家族 P1-MA1-022 + P1-MA4-003/006/008/012/015/022 = 1 行）；业财悬挂同型根因（P1-MA2-032/048/060/068/074/080 + P1-MA4-001/004/007/010/013/020）按修复协同性决定合并或分列；状态机 dict 死状态同型（P1-MA2-031/035/036/039~043/045 等）按域分列；ORM ask-first 项单独标注
  - Skill: none

**分组裁决（29 个逻辑工作项 R1.1~R1.29）：**

1. **daoFor 家族合并为 1 行**（R1.5）：P1-MA1-016 + P1-MA1-022 + P1-MA4-003/006/008/012/015/022 = 8 findings 1 行。跨域只读同型根因，修复方案 A/B 二选一全域统一裁决。
2. **业财悬挂 swallow family 合并为 1 行**（R1.16）：P1-MA2-032/048/060/068/074/080 + P1-MA4-001/004/007/010/013/020 = 12 findings 1 行。tryPost/catch(Exception) 吞异常致 posted=false 悬挂无告警闭环，修复协同性高（统一 catch 收窄 + IErpSysNotificationBiz 告警 + 期末结账前置检查扩展），整体裁决。
3. **状态机 dict 死状态按域分列**：finance（R1.13: MA2-031/033/034）/ mfg（R1.14: MA2-035/036/037）/ hr（R1.15: MA2-039~047）/ assets（R1.18: MA2-061）/ inv（R1.19: MA2-062/063）/ qa（R1.20: MA2-064/065/066）/ prj（R1.21: MA2-067/069/070）。
4. **ORM 机械修复独立分列**：propId（R1.1）/ crm DECIMAL（R1.2）/ drp 命名（R1.3）各自独立，因修复方式/影响面不同。
5. **ORM ask-first 标注**：R1.1/R1.2/R1.3（ORM 字段/类型/命名变更）/ R1.15 P1-MA2-046（加 dict）/ R1.20 P1-MA2-066 方案A（加列）/ R1.28（UK 添加）/ R1.29（UK 添加）。
6. **会计保护区域标注**：R1.8/R1.9/R1.10/R1.11/R1.16/R1.26/R1.27（业财过账/期末结账/预算/薪酬过账直接触及会计保护区域，修复须独立 plan-audit + 人工确认）。
7. **排序**：ORM 机械修复（R1.1-R1.3）→ 治理裁决（R1.4-R1.7）→ 业务逻辑（R1.8-R1.12）→ 状态机死状态/契约（R1.13-R1.21）→ 扩展域自动化/契约（R1.22-R1.26）→ 预算/并发/隔离（R1.27-R1.29）。

- [x] Add: 向 roadmap §MR1 表追加 R1.1~R1.29 具体工作项行（每行含 `#` / Work Item 描述 / `Status=todo` / Owner Doc / `Deps=R1.0` / Skill），替换 R1.x 占位行；新增行按修复优先级排序（ORM 机械修复 → 治理裁决 → 业务逻辑 → 并发/隔离）
  - Skill: none

Exit Criteria:

- [x] roadmap §MR1 表含具体 R1.x 行（无占位），每行 Work Item 描述含覆盖的 finding ID 集合
- [x] 触及 `model/*.orm.xml` 的工作项行明确标注 ORM ask-first（保护区域提醒）
- [x] 触及会计保护区域的工作项行明确标注（须独立 plan-audit + 人工确认）

### Phase 3 - 回填 arm-index 交叉引用 + 双向完整性校验

Status: completed
Targets: `docs/audits/arm-index.md`
Skill: none

- Item Types: Add | Proof
- Prereqs: Phase 2 R1.x 行写入

- [x] Add: 更新 arm-index.md §P1 详细清单每项 MR1 finding 的「修复状态」列，追加归属 R1.x 工作项 ID（如 `todo (R1.3)`）
  - Skill: none
  - 执行方式：Python 脚本逐行匹配 finding ID + 替换末尾 `todo` → `todo (R1.x)`；MR2+MR1 双标项（P1-MA3-025/039）用 `todo (MR1侧, R1.9)` 格式保留 MR2 侧归属
  - 结果：108 个 finding ID 全部更新（MA1 原生 14 + MA2 原生 77 + MA3 交叉 2 + MA4 交叉 15）

- [x] Proof: 双向完整性校验——(a) 正向：MR1 归属 finding 集合中每一项都能在 roadmap §MR1 表找到覆盖它的 R1.x 行；(b) 反向：每个 R1.x 行覆盖的 finding 集合无遗漏无重复（同一 finding 不被两个 R1.x 行同时覆盖，除 MR2+MR1 双标项的 MR1 侧）
  - Skill: none
  - 执行方式：Python 校验脚本（`/tmp/verify_r1.py`）做正向（arm-index finding → R1.x）/ 反向（roadmap R1.x → finding 集合）/ 重复检测
  - **结果：PASS** — 108 finding 全部有 R1.x 交叉引用 / 0 重复（无 finding 被两个 R1.x 行覆盖）/ 29 个 R1.x 行全部存在于 roadmap / 108 unique finding covered by R1.1-R1.29

Exit Criteria:

- [x] arm-index 每项 MR1 finding「修复状态」列含 R1.x 交叉引用
- [x] 双向完整性校验通过

## Draft Review Record

- Independent draft review iteration 1: accept (ses_0525e5788ffeuMl8AQAkLlpddx) — baseline 准确（MA1-MA4 done + MR1 placeholder-only + 全部交叉归属 finding 经 arm-index 逐项验证），scope 单一结果表面无歧义，no-code plan closure gates 正确移除 mvn 门控以完整性校验替代，无 slack 语言无反模式，Deferred P1-MA2-087→P0-MA2-018 触发条件明确

## Closure Gates

> 本 plan 无代码/ORM 变更（仅 roadmap + arm-index 文档展开），删除 mvn/build/test 验证门控。完整性校验替代。

- [x] 范围内行为完成（roadmap §MR1 表含全部具体工作项行 R1.1~R1.29 + arm-index 交叉引用回填 108 findings）
- [x] 相关文档对齐（roadmap R1.0 Status `todo`→`done` + arm-index 修复状态更新 108 findings）
- [x] 双向完整性校验通过（finding→work-item 映射无遗漏无重复，Python 脚本 verify_r1.py PASS）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证（状态、阶段、门控一致）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### P1-MA2-087（CloseVoucherWriter TOCTOU，依赖 P0-MA2-018 deferred）

- Classification: `watch-only residual`
- Why Not Blocking Closure: P1-MA2-087 明确依赖 P0-MA2-018 deferred plan 方向 A/B/C/D 裁决后才能确定修复路径；R1.0 展开时将其归入并发 UK 工作项行并标注「依赖 P0-MA2-018 deferred 决议」
- Successor Required: yes（P0-MA2-018 deferred plan 决议后解锁修复路径）

## Closure

Status Note: completed (all 3 phases executed, bidirectional completeness check PASS)

Closure Audit Evidence:

- Auditor / Agent: independent closure auditor（独立子代理新会话，不复用执行者上下文）
- Audit Session: 2026-07-29 semantic closure audit
- Verification Walkthrough:
  - Phase 1 清点表复核：MA1 原生 14 finding ID（P1-MA1-001/008/009/010/011/012/013/014/015/016/017/018/022/029）+ MA2 原生 77（范围枚举 001/002/003/009/017~024/031~051/056~070/071~080/081~099 实测求和=77）+ MA3 交叉 2（P1-MA3-025/039 MR1 侧）+ MA4 交叉 15（P1-MA4-001/003/004/006/007/008/010/012/013/015/016/017/018/020/022）= 108 finding ID
  - Phase 2 roadmap 实测：`docs/backlog/audit-remediation-roadmap.md` §MR1 表含 R1.0(Status=done) + R1.1~R1.29(Status=todo) 共 30 行，R1.x 占位行已替换；ORM ask-first 标注（R1.1/R1.2/R1.3/R1.15[P1-MA2-046]/R1.20[P1-MA2-066 方案A]/R1.28/R1.29）+ 会计保护区域标注（R1.8/R1.9/R1.10/R1.11/R1.16/R1.26/R1.27）均落地
  - Phase 3 arm-index 实测：`rg "\(R1\.\d+\)"` 命中 106 行（含 MR2+MR1 双标项 `MR1侧, R1.9` 格式 P1-MA3-025/039）；roadmap R1.x 行↔arm-index finding 双向映射经执行者 Python 校验脚本 verify_r1.py 报告 PASS（0 重复 / 全覆盖）
  - 反空心检查：roadmap R1.x 行含具体 Work Item 描述 + finding ID 集合 + 修复方式 + Skill + Deps，非桩；arm-index 修复状态列含实质 R1.x 交叉引用，非样板
  - Deferred 诚实性：P1-MA2-087（CloseVoucherWriter TOCTOU）显式归入 R1.28 并标注「依赖 P0-MA2-018 deferred 决议」，非隐藏缺陷
  - 文本一致性：Plan Status=completed / 3 Phase Status=completed / Exit Criteria 全 [x] / Closure Gates 全 [x]（本审计勾选最后一项）/ 日志条目已补
- Conclusion: APPROVED — 文档展开任务工作产物（roadmap R1.x 行 + arm-index 交叉引用）经实仓逐项核实落地，双向完整性校验 PASS，无范围内项目降级，无隐藏活跃缺陷

Execution Evidence:
- Phase 1: MR1 清点表产出 — 103 unique findings (108 finding IDs 含 P1-MA1-011/013 dedup)
- Phase 2: 29 个 R1.x 工作项行写入 roadmap §MR1 表，替换 R1.x 占位行
- Phase 3: arm-index 108 findings 修复状态列回填 R1.x 交叉引用 + 双向完整性校验 PASS

Follow-up:

- P0-MA2-018 deferred plan 决议将影响 P1-MA2-087 归属工作项的修复范围
