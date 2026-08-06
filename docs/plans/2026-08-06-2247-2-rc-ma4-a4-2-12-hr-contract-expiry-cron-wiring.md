# 2026-08-06-2247-2 rc-ma4-a4-2-12-hr-contract-expiry-cron-wiring UC-HR-07 合同到期 cron 运行时调度接线确认（A1.12 §7-1）

> Plan Status: completed
> Last Reviewed: 2026-08-06
> Mission: requirement-compliance
> Work Item: A4.2.12（A1.12 §7-1：MA4 运行时行为验证 — UC-HR-07 合同到期提醒 cron 运行时调度接线，`ErpHrContractExpiryJob.execute()` 经 nop-job `.job.yaml` 反射调用 + 两层 config 门控[nop-job enabled 默认 false + in-job contract-expiry-cron 空值跳过]运行时是否实际活跃）
> Source: `docs/backlog/requirement-compliance-roadmap.md` Work Item A4.2.12；存疑点来源 `docs/audits/2026-08-02-2328-rc-ma1-a1-12-hr-f1-employee-organization.md` §7-1
> Related: `docs/plans/2026-08-07-0400-3-rc-ma4-a4-2-ext-domain-runtime-expander.md`（A4.2 展开器 done，本行即其展开的 A4.2.12 实体行）、`docs/audits/2026-08-02-2328-rc-ma1-a1-12-hr-f1-employee-organization.md`（A1.12 done，UC-HR-07 接受，§7-1 存疑点）、`docs/plans/2026-08-06-0847-1-rc-ma4-a4-1-4-budget-config-default-deployment-contract.md`（范式参照：config 默认关闭 vs「开箱即用」部署契约核对先例，A4.1.4 done）
> Audit: required

## Current Baseline

> 本计划是**运行时行为验证**（verification or audit work），结果表面 = 一份验证报告（落盘 `docs/audits/2026-08-06-2247-rc-ma4-a4-2-12-hr-contract-expiry-cron-wiring.md`）+ 必要时 arm-index finding 注记更新。**不改代码/ORM/api.xml/真相源**（只读评估：nop-job `.job.yaml` 接线 census + 两层 config 门控默认值 + 全 application.yaml 部署 override 普查 + BeanMethodJobInvoker 反射调用链）。范式对齐 A4.1.4（done — config 默认关闭 vs 部署契约核对先例）。

- **存疑点原文**（A1.12 报告 §7-1，`2026-08-02-2328-...-a1-12-...md` §7:322）：
  - 「**UC-HR-07 cron 运行时调度接线**：`ErpHrContractExpiryJob.execute()` 依赖 `scheduler.yaml` cronExpr 反射调用 + `erp-hr.contract-expiry-cron` config 非空。运行时 scheduler.yaml 是否实际接线 + cron config 是否非空需 MA4 运行时确认（本切片静态确认代码门控逻辑正确 + 单测覆盖 cron 空/非空两路径）。」（A1.12 §7-1 已使用 live repo 类名 `ErpHrContractExpiryJob`，无命名歧义。）

- **关联既有 finding**：无独立 P1/P2 finding（A1.12 §5 裁决 UC-HR-07 = **接受**：cron-gated Job + 单一可配置提醒窗口 + 续签/到期终止完整 + 跨域通知派发）。⑮不续签→RESIGNED 复用 P1-MA2-039 successor Deferred（resolved R1.15）。本验证确认 UC-HR-07 cron 接线的**运行时活跃性**——若两层 config 门控均默认关闭 → 合同到期自动化运行时非默认活跃，登记 config-gate 部署启用注意（不撤销 UC-HR-07 接受，因 cron-gated 机制本身存在）。

- **需求契约（L1 权威）**：`docs/design/human-resource/use-cases.md` UC-HR-07 合同到期提醒（断言⑲cron 调度 + ⑳30/60/90 提醒窗口 + ㉑扫描 ACTIVE 合同 + ㉒通知 HR + ㉓续签 + ㉔到期终止）。⑲要求"cron 调度"——L1 要求 cron-gated 机制存在（已满足），未要求开箱默认启用。

- **实现现状（L3，实测锚点，本计划起草时 live repo 核实）**：
  - **作业逻辑类**（已 live 核实）：`ErpHrContractExpiryJob.java:34`（`public void execute():54`）。
    - `:22-23` 注释「由 nop-job-local 的 scheduler.yaml 经 BeanMethodJobInvoker 反射调用 execute()。触发频率由 scheduler.yaml 的 cronExpr 决定（设计默认每日 01:00）」。
    - `:25` 注释「实际执行门控：erp-hr.contract-expiry-cron 配置为空时跳过（"不调度"语义）」。
    - `:55-57` `String cron = resolveCronConfig(); if (StringHelper.isEmpty(cron)) { LOG.info("erp-hr-contract-expiry-skipped: cron config empty (erp-hr.contract-expiry-cron)"); ... }`——**in-job 第二层门控：config 空值跳过**。
  - **config 常量**（已 live 核实）：`ErpHrConstants.java:250` `CONFIG_CONTRACT_EXPIRY_CRON = "erp-hr.contract-expiry-cron"`。
  - **bean 注册**（已 live 核实）：`app-service.beans.xml:44` `<bean id="erpHrContractExpiryJob" class="app.erp.hr.service.job.ErpHrContractExpiryJob"/>`。
  - **nop-job 调度接线**（已 live 核实，**关键**）：`app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-hr-contract-expiry.job.yaml`：
    - `enabled: "@cfg:nop.job.erp-hr-contract-expiry.enabled|false"`——**nop-job enabled 默认 FALSE（作业默认不触发）**。
    - `trigger.cronExpr: "@cfg:nop.job.erp-hr-contract-expiry.cron-expr|0 0 1 * * ?"`——默认每日 01:00。
    - `invoker.bean: erpHrContractExpiryJob` + `invoker.method: execute`——反射调用链接线完整。
  - **两层 config 门控叠加**（本验证核心发现）：①nop-job 层 `nop.job.erp-hr-contract-expiry.enabled` 默认 **false**（作业不触发）；②in-job 层 `erp-hr.contract-expiry-cron` 默认空（即便触发也跳过）。**两层均默认关闭 → 合同到期自动化运行时非默认活跃**（待 Phase 1 全 application.yaml 部署 override 普查确认是否有任何站点启用）。
  - **既有测试**（已 live 核实）：`TestErpHrContractExpiry`（A1.12 §3 确认 7 方法强断言，覆盖 cron 空/非空两路径 + scan/expire/renew）。

- **既有证据（复用输入）**：
  - A1.12 §3（UC-HR-07 实现证据：ErpHrContractExpiryJob cron-gated + scan/expire/renew 完整 + TestErpHrContractExpiry 7 方法强）
  - A1.12 §5（UC-HR-07 = 接受；cron-gated Job 接受 on ⑲）
  - A1.12 §7-1（静态存疑点）

- **剩余差距**：(a) nop-job `.job.yaml` 接线存在但 `enabled` 默认 false——全 application.yaml 部署 override 是否有站点设 `nop.job.erp-hr-contract-expiry.enabled=true` 未普查；(b) in-job `erp-hr.contract-expiry-cron` 是否有部署非空 override 未普查；(c) 两层 config key 命名不一致（nop-job 层 `nop.job.erp-hr-contract-expiry.*` vs in-job 层 `erp-hr.contract-expiry-cron`）是否致运维混淆未确认。本验证闭合 UC-HR-07 cron 接线运行时活跃性裁决。

- **保护区域**：只读评估（grep nop-job .job.yaml + 两层 config 默认值 + 全 application.yaml 部署 override 普查 + BeanMethodJobInvoker 反射调用链 + 命名对账），不触及 ORM/会计过账逻辑**修改**。属 roadmap 预授权类目（只读评估）。本验证**不实施修复**（若登记 config-gate 部署启用注意，属运维文档/owner doc 注记预授权；config 默认值调整属 application.yaml 部署配置预授权不触 ask-first）。

## Goals

- **nop-job 接线 census**（§7-1 核心）：核验 `erp-hr-contract-expiry.job.yaml`（`app-erp-all/.../nop/job/conf/`）的完整接线——`enabled`/`cronExpr`/`invoker.bean`/`invoker.method` 各字段 + BeanMethodJobInvoker 反射调用链是否完整可达 `ErpHrContractExpiryJob.execute()`。给出 file:line 证据。
- **两层 config 门控默认值 census**：核验两层 config 门控默认值——①nop-job 层 `nop.job.erp-hr-contract-expiry.enabled`（实测默认 **false**）；②in-job 层 `erp-hr.contract-expiry-cron`（实测默认空，`ErpHrContractExpiryJob.resolveCronConfig`）。确认两层叠加下合同到期自动化是否默认活跃。
- **全 application.yaml 部署 override 普查**：grep 全 20 生产 application.yaml + 部署运维文档，确认是否有任何站点设 `nop.job.erp-hr-contract-expiry.enabled=true` 或 `erp-hr.contract-expiry-cron` 非空 override。
- **config key 命名一致性核验**：核验两层 config key 命名（nop-job 层 `nop.job.erp-hr-contract-expiry.*` vs in-job 层 `erp-hr.contract-expiry-cron`）是否致运维混淆（运维可能只设一层而遗漏另一层）。
- **裁决**（方法论 §2 判据 + 三源对照）：①两层 config 门控均默认关闭 + 无部署 override → 合同到期自动化运行时非默认活跃，但 cron-gated 机制本身存在（L1 ⑲"cron 调度"要求机制存在已满足）→ **维持 UC-HR-07 接受** + **登记 config-gate 部署启用注意**（watch-only/P2，对齐 A4.1.4 budget config 范式：config 默认关闭是部署启用决策非契约缺失）；②有部署 override 启用 → 闭合，UC-HR-07 接受维持无额外注记；③接线断裂（.job.yaml 不存在或反射链断）→ 登记 finding（按 §2 判据分级）。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 A1.12 §5 UC-HR-07 接受裁决分层一致。
- 产出验证报告 + §8 过程纪律自检。

## Non-Goals

- **不修改 config 默认值/不启用作业**（属部署配置决策，本计划仅登记 config-gate 部署启用注意，不实施修复）。
- **不修改代码/ORM/api.xml/BizModel/Processor/真相源**（只读评估）。
- **不重新核实 UC-HR-07 全部验收标准**（A1.12 §5 已判接受；本验证只评 cron 接线运行时活跃性差异）。
- **不展开 A1.12 §7-2**（30/60/90 多档预警运行时配置，归 A4.2.13 独立工作项）。
- **不展开 A1.12 §7-3/§7-4/§7-5**（评估权重/handleContract 三态/未到岗回退，各自独立工作项 A4.2.14/A4.2.15/A4.2.16）。
- **不实际执行 cron 触发重现**（只读 .job.yaml 接线 census + config 默认值 + 部署 override 普查 + 反射调用链推理；真实 cron 触发重现属部署/运维范围，非本验证范围）。
- **不重审 P1-MA2-039**（UC-HR-07⑮ 不续签→RESIGNED successor Deferred，resolved R1.15，A1.12 §5 已复用，本验证不覆盖该控制点）。

## Task Route

- Type: `verification or audit work`（UC-HR-07 合同到期 cron 运行时调度接线确认 + UC-HR-07 接受裁决运行时活跃性补充）
- Owner Docs: `docs/audits/requirement-compliance-methodology.md`（§MA4 + §2 判据 + §4 Q1 真相源层级 + §7 衔接 + §8 自检 + §9 冻结 + §去重协议）+ `docs/backlog/requirement-compliance-roadmap.md`（A4.2.12 行）+ `docs/audits/2026-08-02-2328-rc-ma1-a1-12-hr-f1-employee-organization.md` §7-1 + §5 UC-HR-07 裁决（输入）+ `docs/design/human-resource/`（合同到期 owner doc）。
- Skill Selection Basis: `Skill: docs/skills/multi-dimensional-audit-prompt.md`（roadmap MA4 指定）。cron 接线活跃性评估需多维度归类（nop-job 接线 / 两层 config 门控默认值 / 部署 override 普查 / config key 命名一致性 / UC-HR-07 接受裁决协同 / A4.1.4 config-gate 范式对比 / MA4↔A5.6 边界）。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。只读评估（grep nop-job .job.yaml + 两层 config 默认值 + 全 application.yaml 部署 override 普查 + BeanMethodJobInvoker 反射调用链 + 命名对账）。§8 自检需跑 `bash docs/audits/nop-compliance-checker.sh`（纯 reporter；无生产代码变更故无回归风险）。

## Execution Plan

### Phase 1 - nop-job 接线 census + 两层 config 门控默认值 + 全 application.yaml 部署 override 普查

Status: completed
Targets: `docs/audits/2026-08-06-2247-rc-ma4-a4-2-12-hr-contract-expiry-cron-wiring.md`（验证报告）
Skill: `docs/skills/multi-dimensional-audit-prompt.md`

- Item Types: `Proof`（Phase 1 全 Proof）
- Prereqs: A4.2 done（展开器已追加 A4.2.12 行）；A1.12 done（§7-1 已落盘 + §5 UC-HR-07 接受裁决已登记）

- [x] `Proof` nop-job 接线 census：核验 `erp-hr-contract-expiry.job.yaml`（`app-erp-all/.../nop/job/conf/`）完整接线——`enabled`/`cronExpr`/`invoker.bean`/`invoker.method` 各字段 + BeanMethodJobInvoker 反射调用链是否完整可达 `ErpHrContractExpiryJob.execute()`。给出 file:line 证据。确认 nop-job 调度层接线存在（vs §7-1 字面"scheduler.yaml 是否实际接线"疑问）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 两层 config 门控默认值 census：核验两层 config 门控默认值——①nop-job 层 `nop.job.erp-hr-contract-expiry.enabled`（`@cfg:...|false` 实测默认 false）+ `nop.job.erp-hr-contract-expiry.cron-expr`（默认 `0 0 1 * * ?`）；②in-job 层 `erp-hr.contract-expiry-cron`（`ErpHrContractExpiryJob.resolveCronConfig` + `ErpHrConstants.CONFIG_CONTRACT_EXPIRY_CRON`，默认空）。确认两层叠加下合同到期自动化是否默认活跃。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` 全 application.yaml 部署 override 普查：grep 全 20 生产 application.yaml + 部署运维文档（README/seed/部署文档），确认是否有任何站点设 `nop.job.erp-hr-contract-expiry.enabled=true` 或 `erp-hr.contract-expiry-cron` 非空 override。给出站点级证据（哪站点设/未设）。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` config key 命名一致性核验：核验两层 config key 命名（nop-job 层 `nop.job.erp-hr-contract-expiry.*` vs in-job 层 `erp-hr.contract-expiry-cron`）是否致运维混淆（运维只设一层而遗漏另一层的风险）+ module-meta.yaml configKey 声明完整性。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`
- [x] `Proof` MA4↔A5.6 边界声明：本验证审「行为是否符合需求」（cron 接线运行时活跃性），与 A5.6 审「E2E 断言强度」边界按此执行。不重做 A5.6 E2E 断言强度审计。
      - Skill: `docs/skills/multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] nop-job 接线 census 有明确结论（接线完整 / 断裂），每条有证据（file:line）
- [x] 两层 config 门控默认值 + 全 application.yaml 部署 override 普查有明确结论（默认活跃 / 默认关闭 + 站点启用情况），每条有证据（file:line）

### Phase 2 - cron 接线活跃性裁决 + finding 衔接 + §8 自检

Status: completed
Targets: `docs/audits/2026-08-06-2247-rc-ma4-a4-2-12-hr-contract-expiry-cron-wiring.md`（定稿）；`docs/audits/arm-index.md`（UC-HR-07 注记或 config-gate finding 登记，若有）
Skill: none

- Item Types: `Add | Proof | Decision`
- Prereqs: Phase 1 nop-job 接线 census + 两层 config 门控默认值 + 部署 override 普查完成

- [x] `Decision` UC-HR-07 cron 接线运行时活跃性裁决（方法论 §2 判据 + 三源对照）：①两层 config 门控均默认关闭 + 无部署 override → 合同到期自动化运行时非默认活跃，但 cron-gated 机制本身存在（L1 ⑲"cron 调度"要求机制存在已满足）→ **维持 UC-HR-07 接受** + **登记 config-gate 部署启用注意**（watch-only/P2，对齐 A4.1.4 budget config 范式）；②有部署 override 启用 → 闭合，UC-HR-07 接受维持无额外注记；③接线断裂（.job.yaml 不存在或反射链断）→ 登记 finding（按 §2 判据分级，若致合同到期自动化完全缺失可能 P1②异常路径）。裁决须列明 §2 判据编号 + L1/L2/L3 三源 + 与 A1.12 §5 UC-HR-07 接受裁决分层一致。
      - Skill: none
- [x] `Add` finding/注记更新：若登记 config-gate 部署启用注意 → arm-index 新建 P2-RC watch-only finding 行（合同到期自动化 config-gate 默认关闭，部署启用决策）+ owner doc `recruitment.md`/部署文档补 config 启用注记（预授权文档更新）；若接线断裂 → arm-index 新建 finding 行 + 触发 MR1（归 R1.0 展开器）。
      - Skill: none
- [x] `Proof` §8 过程纪律自检：运行 `bash docs/audits/nop-compliance-checker.sh` 附 actual vs baseline 表（无生产代码变更，注明「无回归风险」）；closure-audit 独立性声明；与 arm-index 交叉去重声明（与 UC-HR-07 / A1.12 §5/§7 的复用关系 + config-gate 范式与 A4.1.4 对比 + MA4↔A5.6 边界）。不以 checker 退出码 0 作为门控依据。
      - Skill: none

Exit Criteria:

- [x] 验证报告定稿（nop-job 接线 census + 两层 config 门控 + 部署 override 普查 + 命名对账 + 活跃性裁决 + finding 衔接 + §8 自检齐全）
- [x] UC-HR-07 注记或 config-gate finding 登记 + 若归 MR1 已记录

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 ses_028702bc0ffev9kqjRfXIn4aQ4，fresh session，未起草本计划）— 9/10 checklist PASS，crux（两层 config 门控 `enabled|false` 默认 + in-job contract-expiry-cron 空）全 VERIFIED。**1 Blocker**：Current Baseline 的「命名说明」+ Phase 1 `Proof 命名对账（ErpHrn vs ErpHrContractExpiryJob）` 为虚构叙事——A1.12 §7-1（`:322`）已使用 live repo 类名 `ErpHrContractExpiryJob`，repo 全局 grep `ErpHrn` 仅命中计划文件本身，无命名对账需求（违反 Rule 1 从实时基线开始）。
- Independent draft review iteration 2: accept（独立子代理 ses_0286ceb6dffez0P1HmYBQw0Pit，fresh session，未起草/未参与前审）— Blocker 已修复：`命名说明` bullet 改写为「A1.12 §7-1 已使用 live repo 类名 ErpHrContractExpiryJob，无命名歧义」+ Phase 1 虚构 `Proof 命名对账` item 已删除（grep `ErpHrn` 计划文件零命中）。crux（两层 config 门控 + enabled|false 默认 + 反射调用链）经 live repo 复核 intact VERIFIED。4/4 验证任务全 VERIFIED。共识达成，转 active。

## Closure Gates

> 本计划为**只读 cron 接线活跃性评估**（无代码/ORM/api.xml/view.xml/真相源变更），故删除完整仓库 `typecheck`/`build`/`lint`/`test` 验证命令门控。验证 = nop-job 接线 census + 两层 config 门控默认值 + 部署 override 普查 + 命名对账 + 活跃性裁决 + finding 衔接 + §8 过程纪律自检 + 独立草案审查 + 文本一致性 + 独立结束审计。

- [x] 范围内行为完成：A4.2.12 验证报告 nop-job 接线 census + 两层 config 门控 + 部署 override 普查 + 命名对账 + 活跃性裁决齐全 + finding/注记更新
- [x] 相关文档对齐：报告与方法论 §MA4 + §2 判据 + §4 Q1 + §去重协议一致；与 A1.12 §7-1 + §5 UC-HR-07 接受裁决一致
- [x] 已运行验证：nop-job 接线 census + §8 checker actual vs baseline 实测记录（本计划无代码变更故不跑 build/test）
- [x] 无范围内项目降级为 deferred/follow-up（登记的 config-gate note/finding 是验证**输出**，非范围内项目降级；Deferred But Adjudicated 正确分类）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：Plan Status / Phase Status / Exit Criteria / Closure Gates / 日志条目都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项保留为未勾选状态作为人工门控占位符（本次结束审计由独立子代理于 fresh session 执行，未重用执行者上下文；见 ## Closure 的 Closure Audit Evidence）
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### config-gate 部署启用注意（合同到期自动化默认关闭）

- Classification: `watch-only residual`（config 默认关闭是部署启用决策，非契约缺失）
- Why Not Blocking Closure: 本计划是 cron 接线活跃性评估，结果表面 = 验证报告 + finding/注记登记。两层 config 门控默认关闭（nop-job enabled=false + in-job contract-expiry-cron 空）是部署启用决策——L1 UC-HR-07 ⑲要求"cron 调度"机制存在（已满足：.job.yaml 接线 + ErpHrContractExpiryJob 逻辑 + TestErpHrContractExpiry 强测），默认关闭非契约缺失。对齐 A4.1.4 budget config 范式（config 默认关闭 = 部署启用决策）。owner doc/部署文档补 config 启用注记属预授权文档更新。
- Successor Required: no（部署启用决策，非代码修复 successor）

## Closure

Status Note: 已执行完毕（2026-08-06）。两 Phase 全 [x] + Plan Status: completed + 全 8 Closure Gates [x]。裁决 = 维持 UC-HR-07 接受 + 登记 config-gate watch-only residual（两层 config 门控默认关闭是部署启用决策，对齐 A4.1.4 budget config 范式），0 新 finding / 不触发 MR0 / 不归 MR1。验证报告落盘 `docs/audits/2026-08-06-2247-rc-ma4-a4-2-12-hr-contract-expiry-cron-wiring.md`（Audit Status: closed）。部署运维文档修正 `docs/architecture/job-scheduling.md §3.15:229`（stale `erp-hr-contract-expiry-reminder` DESIGN → live `erp-hr-contract-expiry` DONE config-gated + 两层 config 启用注记）。Phase 2 item 2 条件裁决：§2 判据 = 接受（非 P2），故**不新建** arm-index P2-RC finding 行（对齐 A4.1.4 = 0 finding）；config-gate-default-off 事实已在 P1-MA2-086 arm-index 描述记录（§7 去重不重复登记），owner doc config 启用注记经 `job-scheduling.md` 修正落实。独立结束审计已由独立子代理（fresh session，未重用执行者上下文）执行并通过，证据见下方 Closure Audit Evidence。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（mission-driver CLOSURE_VERIFY 步骤；fresh session，未重用执行者上下文，未起草本计划，未参与草案审查 ses_028702bc0ffev9kqjRfXIn4aQ4 / ses_0286ceb6dffez0P1HmYBQw0Pit）
- Evidence: 验证报告 `docs/audits/2026-08-06-2247-rc-ma4-a4-2-12-hr-contract-expiry-cron-wiring.md`（§0 TL;DR + §2 census + §5 裁决 + §8 checker actual==baseline 全 16 规则精确匹配 + §9 结论）；`job-scheduling.md §3.15:229` 修正；checker actual==baseline（R1d=14/R2a=34/R2b=229/R2c=1382/R2d=34/R3=5 等）；TestErpHrContractExpiry surefire 7 tests / 0 failures（L4 既有证据复用，本验证零代码变更）；独立结束审计走查核验项：①live 复核 `app-erp-all/src/main/resources/_vfs/nop/job/conf/erp-hr-contract-expiry.job.yaml` 接线完整（jobName/enabled 默认 false/cronExpr 默认 0 0 1 * * ?/invoker.bean=erpHrContractExpiryJob/invoker.method=execute 6 字段齐全）；②live 复核 `module-hr/erp-hr-service/.../ErpHrContractExpiryJob.java` 存在；③live 复核 `docs/architecture/job-scheduling.md:229` stale DESIGN → DONE config-gated 修正已落盘；④live 复核验证报告 Audit Status: closed + §0 TL;DR 裁决 = 维持 UC-HR-07 接受 + 0 新 finding；⑤反中空检查 pass（.job.yaml 反射链 invoker.bean→app-service.beans.xml:44 bean 注册→ErpHrContractExpiryJob.execute() 完整可达，无 return null / 空体 / 吞异常）；⑥Deferred 诚实性 pass（config-gate watch-only residual 是验证输出非范围内降级，分类正确）；⑦文本一致性 pass（Plan Status completed / 两 Phase Status completed / Exit Criteria 全 [x] / 8 Closure Gates 全 [x] / Closure 证据非占位符）

Follow-up:

- 部署启用合同到期自动化时设 `nop.job.erp-hr-contract-expiry.enabled=true` + `erp-hr.contract-expiry-cron` 非空（运维 config 决策，非代码修复）
