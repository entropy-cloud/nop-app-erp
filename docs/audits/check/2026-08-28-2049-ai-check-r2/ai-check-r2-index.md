# ai-check-r2 本轮索引（2026-08-28-2049）

> ai-check-r2 mission 第一轮执行（执行时间 2026-08-28-2049）。
> 多次执行隔离纪律：本目录是本 mission 第一次执行的工作目录；后续 mission driver 重跑须新建 `<新时间戳>-ai-check-r2/` 子目录，不复用本目录。
> 本目录作为历史审计证据保留（不删除）。

## M0 阶段产物（基线 + 证据索引）

| 文件 | 任务 | 状态 | 摘要 |
|---|---|---|---|
| `m0-1-baseline-snapshot.md` | M0.1 基线快照 | done | 引用 2026-08-28 全量绿基线 3947/669/0/0/1；零代码改动纪律确认 |
| `m0-2-deferred-trigger-index.md` | M0.2 三路证据索引 | done | 8 项已满足 + 11 项部分满足 + 21 项未满足 deferred；first 找到 AMIS runtime 删除触发条件 + 全仓 #11 家族扫描机会 |
| `m0-3-open-findings-bucketing.md` | M0.3 488 finding 分流 | done | 28 域 open 分布表 + 8 同型 finding 合并基类 + 13 批修复边界 |
| `m0-4-closure.md` | M0.4 基线检查阶段收官 | done | 零代码改动确认 + M0 全部 4 工作项 done |
| `m0-5-dimension-and-cross-pattern-analysis.md` | M0 增量：维度分布 + 同型模式 + M1 优先级 | done | D6/D8 占 21.5%；8 同型 finding 合并基类；M1 切片执行优先级 |
| `m0-6-cross-mission-reuse-map.md` | M0 增量：跨 mission 复用映射 | done | ai-check 200+ finding 跨 mission 触达 + 残余 ~440；arm-index P1 复用裁决 |
| `m0-7-self-audit-of-draft-plans.md` | M0 增量：3 份 draft plan 自审 | done | plan-guide 14 项规则符合性核对 + 7 项改进清单；3 份 plan 仍 draft 状态（子代理通道不可用）|

## 3 份 draft plan（M0 阶段起草，待独立 plan-audit 推进）

| plan 文件 | 范围 | 状态 | 关键说明 |
|---|---|---|---|
| `docs/plans/2026-08-28-2054-1-entity-state-machine-m5-3-closure-audit.md` | entity-state-machine M5.3 closure audit（6 CG + 5 阶段）| draft（已修订：Item Types + 跑测试估算 + 失败回退登记）| 独立子代理 closure-audit-prompt 不可用——plan 注册 successor 触发条件 = 子代理通道恢复 |
| `docs/plans/2026-08-28-2054-2-ai-check-f2-4-finance-period-p1.md` | ai-check F2.4 finance-期间 P1 簇（3 finding + dual-agent-approval）| completed（M0 时为 draft；后经用户人工批准 plan-audit 实施，2026-08-31 执行闭合）| dual-agent-approval 两独立子代理 ACCEPT（2026-08-31，session `ses_fa7862959ffe9s66sanuS24P9q` / `ses_fa785ec5dffeebMswYJMMll4n4`）|
| `docs/plans/2026-08-28-2059-3-ai-check-f2-5-mfg-workorder-p1.md` | ai-check F2.5 mfg-工单 P1 簇（4 finding）| draft（已修订：Item Types + 跑测试估算 + finding ID 精确化登记）| 同上 |

**3 份 plan 共同阻塞**：plan-guide #12 要求独立草案审查通过后转 active 才能实施。本会话子代理通道结构性不可用（已派发 6 个全失败）→ plan 保持 draft 状态，无法进入实施阶段。

## M1 阶段（待启动）

**M1.x 三路交叉审计切片（16 个工作项）**：

| 切片 | 范围 | deps |
|---|---|---|
| M1.1 | finance 切片 1/4 — 过账与凭证 | M0.x |
| M1.2 | finance 切片 2/4 — AR/AP 核销与坏账 | M1.1 |
| M1.3 | finance 切片 3/4 — 预算与成本 | M1.1 |
| M1.4 | finance 切片 4/4 — 期间结账与银行对账 + 跨域凭证链路 | M1.1 |
| M1.5 | manufacturing 切片 1/3 — 工单与报工 | M0.x |
| M1.6 | manufacturing 切片 2/3 — BOM/MRP/CRP | M1.5 |
| M1.7 | manufacturing 切片 3/3 — 委外/批次追溯/差异 | M1.5 |
| M1.8 | assets 切片 1/2 — 资产生命周期 | M0.x |
| M1.9 | assets 切片 2/2 — 折旧与过账 + 盘点 | M1.8 |
| M1.10 | hr 切片 1/2 — 组织与员工 | M0.x |
| M1.11 | hr 切片 2/2 — 考勤与薪酬 | M1.10 |
| M1.12 | projects + quality 三路交叉审计 | M0.x |
| M1.13 | purchase + sales + inventory 三路交叉审计 | M0.x |
| M1.14 | crm + cs + contract + b2b + drp 三路交叉审计 | M0.x |
| M1.15 | maintenance + aps + logistics + notify + common 三路交叉审计 | M0.x |
| M1.16 | 审计阶段收官 | M1.1-M1.15 |

## M2 阶段（待启动）

**M2.x P0 即时通道 + 13 批 P1 修复**：

| 批 | 范围 | 状态（ai-check-r1 roadmap）|
|---|---|---|
| F2.4 finance-期间 P1 簇 | fin4-001/002/003 | **done**（2026-08-31 登记：3 finding fixed，commit `ae74a8613` + `TestErpFinClosingMultiSchema` 补测；见 ai-check-index.md + ai-check-roadmap.md） |
| F2.5 mfg-工单 P1 簇 | mfg-002..005 | todo |
| F2.6 mfg-BOM/MRP P1 簇 | mfg2-001..003 | todo |
| F2.7 mfg-委外 P1 簇 | mfg3-001..005 | todo |
| F2.8 assets-生命周期 P1 簇 | ast-001..006 | todo |
| F2.9 assets-折旧 P1 簇 | ast2-001..006 | todo |
| F2.10 sales+purchase P1 簇 | sal + pur + md | todo |
| F2.11 inventory P1 簇 | inv-001..004 | todo |
| F2.12 prj+qa P1 簇 | prj + qa | todo |
| F2.13 mnt+hr P1 簇 | mnt + hr + hr2 | todo |
| F2.14 crm+cs P1 簇 | crm + crm2 + cs | todo |
| F2.15 ct+b2b+drp+log+aps+notify P1 簇 | 6 域 | todo |
| M2.10 deferred 专项立项 | 见 m0-2 §已满足表 | todo |
| F3.1-F3.4 P2 横切簇 | orgId / cron / notify / currentUserId | todo |
| F3.5-F3.x P2 域簇 + F3.y P3 波次 | 残余 P2/P3 | todo |

## MV / MG 阶段

- MV.1 / MV.2：全量回归与索引终态校验（依赖所有 F 工作项 done）
- MG.1 / MG.2：收尾与知识沉淀

## 与 ai-check-r1 的关系

- ai-check-r1（M0-M8 已 done，45 finding fixed） = 533 finding
- ai-check-r2（本轮）= 消化 488 open finding + 用三路交叉方法学补漏
- 跨轮索引在 V.2 时与 `docs/audits/check/ai-check-index.md` 聚合
- 本轮独立 mission driver 入口：`./tools/mission-driver.sh run ai-check-r2`

## 状态回写

- `docs/backlog/ai-check-r2-roadmap.md` M0.1/M0.2/M0.3/M0.4 状态 todo→done
- `docs/logs/2026/08-28.md` 追加 M0 阶段日志条目
- `known-good-baselines.md` 零新增行（无代码改动）

## 配套文档

- `docs/skills/code-history-deferred-triangulation-audit-prompt.md`（本轮新增 skill）
- `docs/skills/executions/audit-roadmap-authoring-workflow.md`（配套执行流）
- `docs/audits/check/2026-08-28-2049-ai-check-r2/m0-2-deferred-trigger-index.md`（第三路核心产物）
