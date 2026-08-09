# 2026-08-09-1314-2 permission-mapping-convergence-adjudication

> Plan Status: active
> Last Reviewed: 2026-08-09
> Source: `docs/backlog/permissions-enforcement-roadmap.md` P1.3
> Related: mission `permissions-enforcement`；P1.1（弱前置：敏感动作子集输入）
> Audit: required
> Mission: permissions-enforcement
> Work Item: P1.3

## Current Baseline

`docs/design/roles-and-permissions.md` §角色→权限点映射已建立**粗粒度蓝图**（15 角色 × SUBM 域/菜单组 + FNPT 前缀引用），细粒度 15×674 FNPT 全矩阵明确 Deferred（触发条件 = RBAC 精细化到单据字段级）。

**已落地 per-action FNPT 声明 + 静态 role-resource 种子**（5 域，`<resource ... roles="...">` 属性承载）：

- finance（`module-finance/erp-fin-web/.../erp-fin.action-auth.xml`）：Voucher `:post`/`:reverse`→财务员、AccountingPeriod `:closePeriod`→财务员 / `:reverseClose`→管理员、BadDebt `:writeOff`→财务员 / `:reverseApprove`→管理员。
- b2b：EdiDoc `:markSent`→B2B 对账员 / `:cancel`→B2B 管理员、Asn `:handleInboundWebhook`→B2B 管理员。
- manufacturing：WorkOrder `:start`/`:close`/`:cancel`→生产主管。
- inventory：StockMove `:confirm`→库管员、LandedCost `:approve`→库管员。
- human-resource：LeaveRequest `:approve`→HR 专员、Salary `:approve`→薪酬审批人 / `:markPaid`→薪酬审批人。

19 域均有 delta `erp-*.action-auth.xml`（`x:extends` 生成基 `_erp-*.action-auth.xml`），但**仅上述 5 域声明了独立 per-action FNPT 点 + roles 种子**；其余域（pur/sal 审批集、mfg 委外、assets 处置、全 EDI 生命周期、contract 电子签、扩展域敏感子集）的 per-action 声明未补齐（归 P1.4a-d）。

**B 类扩展域**（CRM/CS/APS/Logistics/DRP，`roles-and-permissions.md` §第二批扩展域 B）：角色映射显式 deferred，SUBM/FNPT 资源已由 codegen 产出，但**未裁决测试环境 enforcement 行为**（新建业务角色 vs 测试环境 admin-only 沿用）。

**缺口**：收敛粒度未正式裁决（角色×SUBM + 敏感动作 per-action FNPT + 兜底策略）并回写 owner doc；B 类域 enforcement 行为未裁决；admin 双命名空间语义张力（横切关注点 2：`skip-check-for-admin` 只认平台角色 `admin`/`nop-admin`，业务角色名「管理员」不可互换充当兜底）未在 owner doc 补语义分离注记。

## Goals

- **裁决收敛粒度**：正式确认映射粒度 = 角色×SUBM Menu + 敏感动作 per-action FNPT + 兜底策略（管理员=平台 admin 兜底 + 业务角色显式种子），并明确排除 15×674 全矩阵（Non-Goal）。记录决策、替代方案、残留风险。
- **裁决 B 类扩展域（CRM/CS/APS/Logistics/DRP）测试环境 enforcement 行为**：在「为每域新建业务角色并补种子」与「测试环境沿用 admin-only（非 admin 受限）」之间二选一，给出理由与对 E1.2 全量翻转的影响。
- **回写 `roles-and-permissions.md`**：将收敛粒度裁决写入 §角色→权限点映射；将 B 类 enforcement 行为裁决写入 §第二批扩展域 B；在 §角色体系「管理员」行补**双命名空间语义分离注记**（平台 admin ≠ 业务角色「管理员」，横切关注点 2）。

## Non-Goals

- **不补齐 per-action FNPT 声明**（归 P1.4a-d，本计划仅裁决粒度策略，不动 delta action-auth.xml）。
- **不产 auth 表 CSV 种子**（角色记录/用户绑定/测试账号归 P1.5b）。
- **不做 15×674 全矩阵逐点映射**（Non-Goal，触发条件 = RBAC 单据字段级精细化）。
- **不做生产环境 enforcement 翻转决策**（生产为 successor，触发条件 = 测试环境全绿 + 生产灰度计划人工批准）。
- **不做行级 data-auth 规则裁决**（归 E2.x，独立于 action-auth 粒度）。

## Task Route

- Type: `requirement clarification`（权限粒度裁决 + owner doc 回写，产决策非代码）
- Owner Docs: `docs/design/roles-and-permissions.md` §角色→权限点映射、§第二批扩展域、§角色体系、§action-level 声明层
- Skill Selection Basis: `none` —— 纯权限粒度裁决与文档回写，不动 action-auth.xml/种子/代码；与 roadmap 表格 P1.3 Skill 列一致。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（只读现有 action-auth.xml 与 owner doc，不改运行时）。

## Execution Plan

### Phase 1 - 收敛粒度裁决

Status: planned
Targets: `docs/design/roles-and-permissions.md` §角色→权限点映射、§action-level 声明层
Skill: none

- Item Types: `Decision` / `Proof`
- Prereqs: P1.1（弱依赖：敏感动作子集由 P1.1 五面清单提供输入——但 P1.3 可先裁决粒度策略框架，敏感动作清单核对随 P1.4a-d 落地）

- [ ] **Decision**：裁决映射粒度 = 角色×SUBM + 敏感动作 per-action FNPT + 兜底策略。考虑的替代方案：(a) 全 15×674 逐点矩阵（拒绝：过大、易与生成文件漂移、Non-Goal）；(b) 仅 SUBM 粗粒度（拒绝：敏感动作坍缩进泛化 mutation 桶，无法表达 reverseClose/writeOff 等高危动作）；(c) 收敛粒度（采纳）。残留风险：per-action 子集边界依赖 P1.1 清单与后续 P1.4a-d 逐域补齐，本裁决不冻结具体动作清单。
  - Skill: none
- [ ] **Decision**：裁决兜底策略 = 平台 admin 角色（字面 `admin`/`nop-admin`）经 `skip-check-for-admin` 全放行 + 业务角色经 `<resource roles="...">` 静态种子显式授权；两套命名空间不可互换。考虑的替代方案：(a) 业务角色「管理员」兼任兜底（拒绝：`skip-check-for-admin` 不认业务角色名，会致兜底失效，B2 风险）；(b) 双命名空间分离（采纳）。残留风险：owner doc「管理员=平台 superuser」表述与分离主张的语义张力须由本注记消解。
  - Skill: none
- [ ] **Proof**：引用 5 域既有 per-action FNPT roles 种子（finance/b2b/mfg/inventory/hr action-auth.xml 行号）作为"收敛粒度已部分验证可行"的证据。（doc-only 计划：本 Proof 为既有种子证据引用，非测试命令，符合 doc-only closure-gate 规则。）
  - Skill: none

Exit Criteria:

- [ ] 收敛粒度与兜底策略两项 Decision 落地，含替代方案与残留风险记录，可被 P1.4a-d / P1.5a 直接消费。

### Phase 2 - B 类扩展域 enforcement 行为裁决

Status: planned
Targets: `docs/design/roles-and-permissions.md` §第二批扩展域 B
Skill: none

- Item Types: `Decision` / `Add`
- Prereqs: Phase 1

- [ ] **Decision**：裁决 CRM/CS/APS/Logistics/DRP 测试环境 enforcement 行为——在「新建业务角色 + 补 SUBM 种子」与「测试环境沿用 admin-only（非 admin 受限/不可见）」之间二选一。考虑的替代方案与残留风险记录（如选 admin-only：E1.2 全量翻转时这 5 域对非 admin 不可见，需在 P2.4 dry-run 影响面清单登记；如选新建角色：P1.5a 种子范围扩大）。
  - Skill: none
- [ ] **Add**：将 B 类裁决写入 `roles-and-permissions.md` §第二批扩展域 B（更新 deferred 边界为明确行为裁决，替换原"暂未定义独立 ERP 角色映射"的模糊表述）。
  - Skill: none

Exit Criteria:

- [ ] B 类域 enforcement 行为经裁决并落盘，E1.2 全量翻转的覆盖边界明确（5 域对非 admin 的可见性已定义）。

### Phase 3 - owner doc 回写与语义分离注记

Status: planned
Targets: `docs/design/roles-and-permissions.md` §角色体系、§角色→权限点映射
Skill: none

- Item Types: `Add`
- Prereqs: Phase 1 + Phase 2

- [ ] **Add**：§角色→权限点映射补收敛粒度裁决结论（替换/明确现有"粗粒度蓝图"表述，指向 P1.4a-d 为落地路径）。
  - Skill: none
- [ ] **Add**：§角色体系「管理员」行补**双命名空间语义分离注记**：平台内置角色 `admin`/`nop-admin`（`skip-check-for-admin` 认）≠ 业务角色「管理员」（action-auth roles 种子用），两套命名空间各自绑定不可互换（消解横切关注点 2 的语义张力）。**归属说明**：roadmap 横切 2 原将该注记归于 P1.5b/P2.2a 落地，因本计划裁决兜底策略（Phase 1 D2）即产生该注记的动机，提前在此落地更自洽；P1.5b/P2.2a 届时仅验证种子绑定一致。
  - Skill: none

Exit Criteria:

- [ ] owner doc 三处（映射粒度、B 类行为、管理员语义分离）回写完成，与 roadmap 措辞一致、无矛盾。

## Draft Review Record

- Independent draft review iteration 1: accept（0 blocker / 0 major / 3 minor）（ses_01b0b8cc7ffeAmV5HTXmlyqIgd）。minor：Phase 2 phase-level Item Types 漏 `Add`；admin 双命名空间注记从 P1.5b/P2.2a 提前未声明归属；Phase 1 Proof 类型在 doc-only 语境下非规范（信息性）。三 minor 均非阻塞，可顺带修。
- 合并修订（iteration 1 → v2，顺带吸收 minor）：Phase 2 Item Types 改 `Decision / Add`；Phase 3 admin 注记补归属说明（提前落地理由）；Phase 1 Proof 补 doc-only 证据引用注记。裁决内容与基线无变化。
- Independent draft review iteration 2: 不需要（iteration 1 即 accept，本轮仅吸收非阻塞 minor，无结构性变更）。

## Closure Gates

> 本计划为纯裁决/文档工作（无生产代码、无 action-auth.xml/ORM/种子改动——仅读现有 delta 文件作证据）。移除 build/test 验证命令门控，理由：无代码变更；验证对象为裁决内部一致性、与生成文件真相源不漂移、owner doc 措辞自洽。

- [ ] 范围内行为完成（收敛粒度 + 兜底策略 + B 类行为 + 三处 owner doc 回写）
- [ ] 相关文档对齐（`roles-and-permissions.md` 三节）
- [ ] 无代码变更 → 跳过 build/test 门控（已说明理由）；裁决与既有 5 域种子/生成文件 `_erp-*.action-auth.xml` 一致性已核验
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控、日志一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 15×674 FNPT 全矩阵逐点映射

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 收敛粒度已满足测试环境 enforcement 验证；全矩阵过大易漂移，roadmap 明确 Non-Goal。
- Successor Required: yes（触发条件 = RBAC 精细化到单据字段级需求出现）

### 生产环境 enforcement 翻转

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本裁决限定测试环境；生产翻转需独立灰度计划 + 人工批准。
- Successor Required: yes（触发条件 = 测试环境全绿验收 + 生产灰度计划人工批准）

## Closure

Status Note: <待完成后填写>

Closure Audit Evidence:

- Auditor / Agent: <独立审计者>
- Evidence: <task id / 链接>

Follow-up:

- <非阻塞跟进项；已确认缺陷不得出现于此>
