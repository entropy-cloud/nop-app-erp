# 2026-08-06-2247-3 rc-ma4-a4-2-13-hr-contract-expiry-multi-tier-alert UC-HR-07 30/60/90 多档预警运行时配置确认（A1.12 §7-2）

> Plan Status: active
> Last Reviewed: 2026-08-06
> Mission: requirement-compliance
> Work Item: A4.2.13（A1.12 §7-2：MA4 运行时行为验证 — UC-HR-07 合同到期 30/60/90 多档预警运行时配置，实现采用单一可配置阈值[默认 30 天] + warningDays 参数可覆盖，运行时是否有多档调度配置[如三个 Job 实例分别传 30/60/90]）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.2.13；存疑点来源 `docs/audits/2026-08-02-2328-rc-ma1-a1-12-hr-f1-employee-organization.md` §7-2
> Related: `docs/plans/2026-08-07-0400-3-rc-ma4-a4-2-ext-domain-runtime-expander.md`（A4.2 展开器 done，本行即其展开的 A4.2.13 实体行）、`docs/audits/2026-08-02-2328-rc-ma1-a1-12-hr-f1-employee-organization.md`（A1.12 done，UC-HR-07 接受，§7-2 存疑点）、`docs/plans/2026-08-06-2247-2-rc-ma4-a4-2-12-hr-contract-expiry-cron-wiring.md`（同源 UC-HR-07 cron 接线验证[N=2]，不同控制点：cron 调度接线 vs 多档预警配置；本计划评 §7-2 多档预警维度）
> Audit: required

## Current Baseline

> 本计划是**运行时行为验证**（verification or audit work），结果表面 = 一份验证报告（落盘 `docs/audits/2026-08-06-2247-rc-ma4-a4-2-13-hr-contract-expiry-multi-tier-alert.md`）+ 必要时 arm-index finding 注记更新。**不改代码/ORM/api.xml/真相源**（只读评估：scanExpiringContracts 单一阈值 + warningDays 参数覆盖机制 census + ErpHrContractExpiryJob 调用是否传多档 + 全 application.yaml/部署多档调度配置普查）。范式对齐 A4.2.12（同源 UC-HR-07，不同控制点）。

- **存疑点原文**（A1.12 报告 §7-2，`2026-08-02-2328-...-a1-12-...md` §7:323）：
  - 「**UC-HR-07 30/60/90 多档预警运行时配置**：L1 "30/60/90 天" 多档预警概念，实现采用单一可配置阈值。运行时是否有多档调度配置（如三个 Job 实例分别传 warningDays=30/60/90）需 MA4 确认（静态确认单一阈值 config 驱动 + 参数可覆盖）。」

- **关联既有 finding**：无独立 P1/P2 finding（A1.12 §5 裁决 UC-HR-07 = **接受**：⑳30/60/90 提醒窗口——`scanExpiringContracts` 经 `ErpHrConfigs.contractExpiryWarningDays()` 默认 30 天 + `warningDays` 参数可覆盖，L1 "30/60/90 天" 是提醒窗口概念，实现采用单一可配置阈值，HR 可调 config 或传参实现多档扫描——实现可表达 L1 语义，单一阈值是配置简化非契约缺失）。本验证确认 UC-HR-07 多档预警的**运行时配置现状**——是否有多档调度配置（3 Job 实例）+ 自动化 Job 是否使用单一默认窗口。

- **需求契约（L1 权威）**：`docs/design/human-resource/use-cases.md` UC-HR-07 合同到期提醒 ⑳「30/60/90 提醒窗口」。L1 "30/60/90 天" 是多档预警**概念**（提前 30/60/90 天各派发一次提醒），实现可采用单一可配置阈值 + 多次调度表达，L1 未强制要求"三个独立 Job 实例"。

- **实现现状（L3，实测锚点，本计划起草时 live repo 核实）**：
  - **config 驱动单一阈值**（已 live 核实）：`ErpHrConfigs.contractExpiryWarningDays():171-173` 读取 `erp-hr.contract-expiry-warning-days`（`ErpHrConstants.CONFIG_CONTRACT_EXPIRY_WARNING_DAYS:248`）+ `DEFAULT_CONTRACT_EXPIRY_WARNING_DAYS`（默认 30）。
  - **参数覆盖机制**（已 live 核实）：`ErpHrEmploymentContractBizModel.scanExpiringContracts(@Optional @Name("warningDays") Integer warningDays, ...):65` → `:67` `int window = warningDays != null ? warningDays : ErpHrConfigs.contractExpiryWarningDays();`——**warningDays 参数非空时覆盖 config 默认，为空时用 config 默认（30）**。
  - **自动化 Job 调用**（已 live 核实，**关键**）：`ErpHrContractExpiryJob.execute():72` `contractBiz.scanExpiringContracts(null, ctx)`——**传 null warningDays → 用 config 默认 30 天单窗口**，无多档（无 30/60/90 三次调用 + 无三个 Job 实例）。`:28` 注释「到期预警：调 scanExpiringContracts（窗口默认 30 天）」。
  - **多档调度配置**（待 Phase 1 普查）：nop-job `.job.yaml` 仅一个 `erp-hr-contract-expiry` 作业（A4.2.12 已 census），无 30/60/90 三个独立作业实例。warningDays 参数可经 GraphQL 手工调用 scanExpiringContracts(60/90) 表达多档，但自动化 Job 不传多档。
  - **既有测试**（已 live 核实）：`TestErpHrContractExpiry`（A1.12 §3 确认覆盖 scan 空/非空两路径；多档参数覆盖路径覆盖度待 Phase 1 复核）。

- **既有证据（复用输入）**：
  - A1.12 §3（UC-HR-07 实现证据：scanExpiringContracts warningDays 参数覆盖 + contractExpiryWarningDays config 驱动）
  - A1.12 §5（UC-HR-07 = 接受；⑳30/60/90 提醒窗口接受——单一阈值 config 简化非契约缺失，L1 多档概念可表达）
  - A1.12 §7-2（静态存疑点）

- **剩余差距**：(a) 自动化 Job（`ErpHrContractExpiryJob.execute:72`）传 null warningDays → 单一默认 30 天窗口，是否有多档调度配置（3 Job 实例分别传 30/60/90）未做运行时普查；(b) 全 application.yaml/部署是否有 `erp-hr.contract-expiry-warning-days` 非 30 override 未普查；(c) 多档预警（30/60/90 各派发一次）在单一阈值 + 单次调度下是否满足 L1"30/60/90 提醒窗口"语义（提前 30 天提醒一次 vs 30/60/90 各提醒一次）需运行时裁决。本验证闭合 UC-HR-07 多档预警运行时配置裁决。

- **保护区域**：只读评估（grep scanExpiringContracts + warningDays 参数覆盖 + ErpHrContractExpiryJob 调用 + 全 application.yaml/部署多档调度配置普查），不触及 ORM/会计过账逻辑**修改**。属 roadmap 预授权类目（只读评估）。本验证**不实施修复**（若登记多档调度 successor，属 Job 多实例/多次调用配置或 owner doc 注记预授权）。

## Goals

- **scanExpiringContracts 单一阈值 + 参数覆盖机制 census**（§7-2 核心）：核验 `ErpHrEmploymentContractBizModel.scanExpiringContracts:65-67` 单一阈值 + warningDays 参数覆盖机制（参数非空覆盖 config 默认，为空用 config 默认 30）+ `ErpHrConfigs.contractExpiryWarningDays:171-173` config 驱动。给出 file:line 证据。
- **自动化 Job 多档调用核验**（§7-2 核心）：核验 `ErpHrContractExpiryJob.execute:72` 调用 `scanExpiringContracts(null, ctx)` 是否传多档（30/60/90 三次调用）还是单一默认窗口（null → 30 天单次）。确认自动化 Job 是否实现多档预警。
- **多档调度配置普查**：grep nop-job `.job.yaml` + 全 application.yaml/部署文档，确认是否有 30/60/90 三个独立 Job 实例或多次调度配置 + `erp-hr.contract-expiry-warning-days` 非 30 override。
- **L1 多档语义满足度裁决**：核验单一阈值 + 单次调度（提前 30 天提醒一次）是否满足 L1 ⑳"30/60/90 提醒窗口"语义（30/60/90 各提前提醒一次）——若仅提前 30 天提醒一次（不提前 60/90 天提醒），是否为配置简化（L1 多档概念可表达）vs 契约缺口。
- **裁决**（方法论 §2 判据 + 三源对照）：①自动化 Job 单一默认窗口（30 天单次）+ 无多档调度配置 + L1 多档概念可经 config/手工调用表达 → **维持 UC-HR-07 接受** + **登记多档调度 successor watch-only**（30/60/90 各提醒一次需多档调度配置，属增强 successor）；②有多档调度配置（3 Job 实例）→ 闭合，UC-HR-07 接受维持；③单一阈值致 60/90 天预警完全缺失且不可表达 → 登记 finding（按 §2 判据分级）。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 A1.12 §5 UC-HR-07 接受裁决分层一致。
- 产出验证报告 + §8 过程纪律自检。

## Non-Goals

- **不实现多档调度**（30/60/90 三个 Job 实例或多次调用，属增强 successor，本计划仅登记不实施修复）。
- **不修改代码/ORM/api.xml/BizModel/Processor/真相源**（只读评估）。
- **不重新核实 UC-HR-07 全部验收标准**（A1.12 §5 已判接受；本验证只评多档预警运行时配置差异）。
- **不展开 A1.12 §7-1**（cron 调度接线，归 A4.2.12 独立工作项；本计划仅引用其 cron 接线 census 结论[Job 默认 disabled]作为多档调度前置）。
- **不展开 A1.12 §7-3/§7-4/§7-5**（评估权重/handleContract 三态/未到岗回退，各自独立工作项 A4.2.14/A4.2.15/A4.2.16）。
- **不重审 P1-MA2-039**（UC-HR-07⑮ 不续签→RESIGNED successor Deferred，resolved R1.15，A1.12 §5 已复用，本验证不覆盖该控制点）。
- **不实际触发多档预警重现**（只读 scanExpiringContracts + warningDays 参数覆盖 + Job 调用 + 多档调度配置普查；真实多档预警派发重现属部署/运维范围，非本验证范围）。

## Task Route

- Type: `verification or audit work`（UC-HR-07 30/60/90 多档预警运行时配置确认 + UC-HR-07 接受裁决多档维度补充）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §4 Q1 真相源层级 + §7 衔接 + §8 自检 + §9 冻结 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.2.13 行）+ `docs/audits/2026-08-02-2328-rc-ma1-a1-12-hr-f1-employee-organization.md` §7-2 + §5 UC-HR-07 裁决（输入）+ `docs/design/human-resource/`（合同到期 owner doc）。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。多档预警配置评估需多维度归类（单一阈值 + 参数覆盖机制 / 自动化 Job 多档调用 / 多档调度配置普查 / L1 多档语义满足度 / UC-HR-07 接受裁决协同 / A4.2.12 cron 接线前置 / MA4↔A5.6 边界）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。只读评估（grep scanExpiringContracts + warningDays 参数覆盖 + ErpHrContractExpiryJob 调用 + 全 application.yaml/部署多档调度配置普查）。§8 自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - 单一阈值 + 参数覆盖机制 census + 自动化 Job 多档调用 + 多档调度配置普查

Status: planned
Targets: `docs/audits/2026-08-06-2247-rc-ma4-a4-2-13-hr-contract-expiry-multi-tier-alert.md`（验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof`（Phase 1 全 Proof）
- Prereqs: A4.2 done（展开器已追加 A4.2.13 行）；A1.12 done（§7-2 已落盘 + §5 UC-HR-07 接受裁决已登记）

- [ ] `Proof` 单一阈值 + 参数覆盖机制 census：核验 `ErpHrEmploymentContractBizModel.scanExpiringContracts:65-67` 单一阈值 + warningDays 参数覆盖机制（参数非空覆盖 config 默认，为空用 config 默认 30）+ `ErpHrConfigs.contractExpiryWarningDays:171-173` config 驱动 + `ErpHrConstants.CONFIG_CONTRACT_EXPIRY_WARNING_DAYS:248` + DEFAULT_CONTRACT_EXPIRY_WARNING_DAYS 默认值。给出 file:line 证据。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 自动化 Job 多档调用核验：核验 `ErpHrContractExpiryJob.execute:72` 调用 `scanExpiringContracts(null, ctx)` 是否传多档（30/60/90 三次调用）还是单一默认窗口（null → 30 天单次）。确认自动化 Job 是否实现多档预警（实测：传 null → 单一默认窗口）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` 多档调度配置普查：grep nop-job `.job.yaml`（确认是否仅一个 `erp-hr-contract-expiry` 作业 vs 30/60/90 三个实例）+ 全 application.yaml/部署文档（`erp-hr.contract-expiry-warning-days` 非 30 override + 是否有多次调度配置）。给出站点级证据。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` L1 多档语义满足度分析：核验单一阈值 + 单次调度（提前 30 天提醒一次）是否满足 L1 ⑳"30/60/90 提醒窗口"语义——30/60/90 各提前提醒一次 vs 仅提前 30 天提醒一次的差异分析 + warningDays 参数经 GraphQL 手工调用 scanExpiringContracts(60/90) 表达多档的可达性。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` A4.2.12 cron 接线前置声明：引用 A4.2.12（N=2）cron 接线 census 结论（Job 默认 disabled）作为多档调度前置——Job 默认 disabled 下多档调度更无从谈起，但多档配置维度独立于 cron 接线维度（即便 Job 启用，仍需评估是否多档），两计划不同控制点不重复。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [ ] `Proof` MA4↔A5.6 边界声明：本验证审「行为是否符合需求」（多档预警运行时配置），与 A5.6 审「E2E 断言强度」边界按此执行。不重做 A5.6 E2E 断言强度审计。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [ ] 单一阈值 + 参数覆盖机制 census 有明确结论，每条有证据（file:line）
- [ ] 自动化 Job 多档调用 + 多档调度配置普查有明确结论（多档 / 单一默认），每条有证据（file:line）

### Phase 2 - 多档预警配置裁决 + finding 衔接 + §8 自检

Status: planned
Targets: `docs/audits/2026-08-06-2247-rc-ma4-a4-2-13-hr-contract-expiry-multi-tier-alert.md`（定稿）；`docs/audits/arm-index.md`（UC-HR-07 注记或多档 successor finding 登记，若有）
Skill: none

- Item Types: `Add | Proof | Decision`
- Prereqs: Phase 1 单一阈值 + 参数覆盖机制 census + 自动化 Job 多档调用 + 多档调度配置普查完成

- [ ] `Decision` UC-HR-07 多档预警运行时配置裁决（方法论 §2 判据 + 三源对照）：①自动化 Job 单一默认窗口（30 天单次）+ 无多档调度配置 + L1 多档概念可经 config/手工调用表达 → **维持 UC-HR-07 接受** + **登记多档调度 successor watch-only**（30/60/90 各提醒一次需多档调度配置，属增强 successor，§2 P2① 次要验收标准 + 主路径[单一阈值 config 驱动]OK）；②有多档调度配置（3 Job 实例）→ 闭合，UC-HR-07 接受维持；③单一阈值致 60/90 天预警完全缺失且不可表达 → 登记 finding（按 §2 判据分级）。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 A1.12 §5 UC-HR-07 接受裁决分层一致。
      - Skill: none
- [ ] `Add` finding/注记更新：若登记多档调度 successor → arm-index 新建 P2-RC watch-only finding 行（多档预警调度 successor）+ owner doc `recruitment.md` 补多档调度注记（预授权文档更新）；若 60/90 缺失不可表达 → arm-index 新建 finding 行 + 触发 MR1（归 R1.0 展开器）。
      - Skill: none
- [ ] `Proof` §8 过程纪律自检：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明（与 UC-HR-07 / A1.12 §5/§7 的复用关系 + A4.2.12 cron 接线前置 + 多档调度 successor + MA4↔A5.6 边界）。不以 checker 退出码 0 作为门控依据。
      - Skill: none

Exit Criteria:

- [ ] 验证报告定稿（单一阈值 + 参数覆盖机制 census + 自动化 Job 多档调用 + 多档调度配置普查 + L1 多档语义满足度 + 多档配置裁决 + finding 衔接 + §8 自检齐全）
- [ ] UC-HR-07 注记或多档 successor finding 登记 + 若归 MR1 已记录

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is（独立子代理 ses_028700098ffeWRWYAL8KGMhl1，fresh session，未起草本计划）— 全 10 checklist 项 PASS（A 格式完整 / B Deps 满足[A4.2 展开器 done + A1.12 done] / C 规则14 单行非合并正确 + 与 A4.2.12 真正不同控制点[A4.2.12=cron 接线/默认活跃性 vs A4.2.13=多档阈值]互补不重复 / D 单一结果表面 / E baseline 零信任核验全 VERIFIED[contractExpiryWarningDays:171-175 + CONFIG_CONTRACT_EXPIRY_WARNING_DAYS:248 + scanExpiringContracts:65/67 参数覆盖 + ErpHrContractExpiryJob 调 scanExpiringContracts(null) 单一默认窗口 crux TRUE + 单一 job 实例无 30/60/90]零 FALSIFIED / F 反松弛 / G item typing / H Skill / I 保护区域[只读] / J 无矛盾[裁决 3 分支与 A1.12 §5 UC-HR-07 接受一致]）。零 Blocker。Non-blocking 已记录：①scanExpiringContracts(null) 调用点 :72 位于 runExpiryWarnings（经 execute:62 调用，A1.12 §3:152 已记两步结构，行归属 nit 非引用失效）。共识达成，转 active。

## Closure Gates

> 本计划为**只读多档预警配置评估**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = 单一阈值 + 参数覆盖机制 census + 自动化 Job 多档调用 + 多档调度配置普查 + L1 多档语义满足度 + 多档配置裁决 + finding 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [ ] 范围内行为完成：A4.2.13 验证报告单一阈值 + 参数覆盖机制 census + 自动化 Job 多档调用 + 多档调度配置普查 + 多档配置裁决齐全 + finding/注记更新
- [ ] 相关文档对齐：报告与方法论 §MA4 + §2 判据 + §4 Q1 + §去重协议一致；与 A1.12 §7-2 + §5 UC-HR-07 接受裁决一致
- [ ] 已运行验证：单一阈值 census + §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [ ] 无范围内项目降级为 deferred/follow-up（登记的多档 successor/finding 是验证**输出**，非范围内项目降级；Deferred But Adjudicated 正确分类）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：Plan Status / Phase Status / Exit Criteria / Closure Gates / 日志条目都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项保留为未勾选状态作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 多档调度配置（30/60/90 各提醒一次）

- Classification: `optimization candidate`（多档预警调度属增强 successor，单一阈值主路径 OK）
- Why Not Blocking Closure: 本计划是多档预警配置评估，结果表面 = 验证报告 + finding/注记登记。L1 ⑳"30/60/90 提醒窗口"是多档预警概念，实现采用单一可配置阈值（默认 30）+ warningDays 参数可覆盖（可经 GraphQL 手工调用表达多档）——单一阈值是配置简化非契约缺失（A1.12 §5 已裁决）。自动化 Job 单一默认窗口（30 天单次）+ 无多档调度配置属增强 successor（需 3 Job 实例或多次调度配置），主路径[单一阈值 config 驱动 + 参数覆盖]OK。owner doc 补多档调度注记属预授权文档更新。
- Successor Required: yes（多档预警调度需求实现时，配置 30/60/90 多次调度或 3 Job 实例；属部署/配置 successor，非 ORM/会计保护区域）

## Closure

Status Note: <关闭时填写>

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- 多档预警调度需求实现时配置 30/60/90 多次调度（部署/配置 successor，非代码修复）
