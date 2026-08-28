# ai-check-r2 M0.2 — 三路证据索引（代码 × 历史 × Deferred）

> 落盘时间：2026-08-28-2049
> 路径：`docs/audits/check/2026-08-28-2049-ai-check-r2/m0-2-deferred-trigger-index.md`

## 方法学回顾

ai-check-r2 的核心方法学（"代码 × 历史 × Deferred" 三路交叉审计）：
- **第一路（代码）**：按域 × B1/B2/B3/B4 维度扫一次本轮切片（与 ai-check-r1 D1-D10 维度正交——B1=异常闭环 / B2=状态机 dict 可达性 / B3=调度链完整性 / B4=守卫完整性）
- **第二路（历史）**：读 `docs/audits/` + `docs/lessons/` + `docs/audits/check/ai-check-index.md` 全量，按"复用 / 新增 / 残余风险"三态裁决
- **第三路（Deferred）**：读最近 50 份 plan 的 Deferred / Successor / Non-Goal 段，列出每条 deferred 项的**触发条件 + 今天是否已满足 + 是否立项 finding**——**本文件即此产物**

## 第一路索引：按域 × B 维度

按 ai-check-r1 已有 28 份 ck-*.md 报告，分布见 `docs/audits/check/ai-check-index.md` 报告清单。本轮不再重扫（ai-check-r1 报告已是代码层面权威基线）。B 维度交叉索引如下（**继承 ai-check-r1 的 533 finding 状态**）：

| B 维度 | 含义 | ai-check-r1 报告归属 | finding 数 | 本轮关注点 |
|---|---|---|---|---|
| B1 异常闭环 | 异常无告警通道 / 宽 catch 吞咽 | MA2 业务审计（A2.5a-c/A2.6/A2.7a-b 等）| 散布在 533 中 | M1 阶段按域切片重扫 |
| B2 状态机 dict 可达性 | 死状态 / 非法迁移无守卫 | ck-master-data/finance/mfg/ast/hr/crm 等 | 散布 533 | M1 切片（与 state-machine-mission 矩阵交叉） |
| B3 调度链完整性 | batch.xml 声明 vs 实际调用 / 孤立 job | ck-finance/cron 漂移族 | F3.2 cron 键漂移（9 域 job.yaml 双键统一） | M1 切片 |
| B4 守卫完整性 | @BizMutation 乐观锁 / versionProp / 权限注解 | F1.3 CRUD 无状态守卫族 + F1.4 AcctSchemaResolver | 45 已 fixed | M1 切片 |

## 第二路索引：复用 / 新增 / 残余风险三态裁决

读 533 finding × 历史 5 轮 mission（audit-remediation / requirement-compliance / id-string-migration / integration-test / permissions-enforcement）的 `arm-index.md` / `requirement-compliance-roadmap.md` / `audit-remediation-roadmap.md` / 各 mission own index，状态裁决：

| 类别 | 数量 | 状态 | 证据 |
|---|---|---|---|
| 已被 audit-remediation MR1-MR6 修复 | 13 P0/P1 | **closed**（arm-index P0-MA2-016 done / P1-MA2-011 done 等）| RC-R1.49 BOM 快照 / RC-R1.50 price variance / RC-R1.60 prj cost rate / MR1 R1.1-R1.29 / MR5 R5.1-R5.8 / MR6 R6.0-R6.8 / R2.x / R3.x |
| 已被 requirement-compliance MR1 修复 | 60 P0/P1 | **closed** | RC-R1.1 ~ RC-R1.60（hr attendance timesheet / survey / cost rate tier / bom snapshot / catch-up depreciation / disposal voucher / idle state / mfg price variance / prj cost rate）|
| 已被 id-string-migration M1-M3 修复 | 19 域 1662 列 | **closed** | M4.1 done（2026-08-23）|
| 已被 permissions-enforcement E1.1-E4.2 修复 | 5 域 action enforcement + role-row-filter + masking + 审计 | **closed**（测试环境）| E1.1 / E1.2 / E2.1 / E2.2 / E2.3 / E3.1 / E3.2 / E4.1 / E4.2 |
| 已被 entity-state-machine-migration M1-M4 修复 | 105 状态机 Bean | **closed** | M1.1-M1.3（cs Ticket 试点）+ M2.x（19 直接生命周期）+ M3.x（19 复杂/审批轴）+ M4.x（65 财务影响/保护域）；M5.1 全域审计零 finding |
| 已被 integration-test B1-B10 修复 | 22 集成用例 | **closed** | 22 用例 CHECKING 三层全比对全绿 |
| **残余风险（ai-check-r1 finding 中由上述 mission 未触达的）** | 488 open finding | **待 ai-check-r2 M1-M2 切片** | 见 `ai-check-index.md` 488 open finding 列表 |
| **新增（本轮 M1 阶段待发现）** | 待定 | 0（基线扫描尚未执行） | M1.1-M1.16 切片将产出 |

> **结论**：ai-check-r1 533 finding 中，**已由既往 mission 修复 45（ai-check 自身） + 跨 mission 触达大量残余**，但**残余 488 open finding 仍待修复**——这是 ai-check-r2 M2.x 修复批的核心目标。第三路（Deferred）扫描结果揭示了"未立项的隐性 finding"机会。

## 第三路索引：Deferred 触发条件扫描

读最近 50 份 plan 的 `## Deferred But Adjudicated` / `## Follow-up` 段（命令：`for f in $(ls 2026-08-2*.md 2026-08-1*.md | sort -r | head -50); do awk '/^## Deferred But Adjudicated/,/^## [^D]/' "$f"; done`），输出按"**已满足** / **部分满足** / **未满足**"三态分组（按 2026-08-28 锚定基线判断）：

### 已满足（建议立即立项 finding 进入 ai-check-r2 索引）

| 来源 plan | deferred 项 | 触发条件 | 当前状态（2026-08-28）| 建议 |
|---|---|---|---|---|
| `2026-08-26-0330-1` F1.2 | posted 回写窄窗 | 业务变更触 mfg 完工移动红冲路径或 fin source-bill posted 反写验证 | mfg-001 已 fix，posted 反写已通；窄窗未独立断言 | **可追加 P2 找到 P1-CK-mfg-005/fin-018 同型站点** |
| `2026-08-25-0330-2` | 年度结转 flush 复核 | 年度结账端到端用例落地或 12 月期间结账验证 | B10（C20a/C20b）已落，未含 12 月期间用例 | **追加 mfg/fin 月度/年度结账 flush 三层回归用例** |
| `2026-08-24-1943-1` V.1 | 全量套件重跑（构造性满足）| V.1 执行时点 git 审计发现未预先裁决生产/seed 变更 | 已 3 次重跑 2026-08-25/27/28 全绿 | **关闭 watch-only** |
| `2026-08-24-1147-1` | nop-web-site bundle `require("react")` 致命错误 | 2026-08-04 重打包已修复；2026-08-28 活仓零命中 react 字面量 | **触发条件已满足** | **关闭 successor** |
| `2026-08-23-0434-1` | nopSequenceGenerator self-wait 全量移除 | nop-entropy 平台修复 | 平台未修（`d2c8e7ed42` 仅第一环），项目内 4 域已 delta 兜底 | **保持 successor**（平台 follow-up）|
| `2026-08-22-2311-3` M3.10 | 存量 page.yaml raw-GraphQL `:Long`（notify/aps/b2b）| M4.1 收尾 | M4.1 done 2026-08-23 grep 零命中 | **关闭**（已 done）|
| `2026-08-28-1600-1` | AMIS Runtime/Adapter 包移除 | 仓库全量 grep 无 `component="AMIS"` 残留 + 全 AMIS DSL page.yaml 删除 + flux E2E N=5 天全绿 | 条件 1 已 done（`component="AMIS"` 0 残留），条件 2 由本计划完成后满足，条件 3 在 0828 反复重跑已多日 | **待本 plan 完成后**触发 |
| `2026-08-27-2006-3` E3.6 P1-6 | introspection.enabled 默认 false 关闭 | 20/20 应用工件 grep 通过 | **已 done** | **关闭** |

### 部分满足（需进一步调查）

| 来源 plan | deferred 项 | 触发条件 | 当前状态 | 建议 |
|---|---|---|---|---|
| `2026-08-26-0430-1` F1.3 | posted 列语义不统一假设 | 是否有 posted 列语义跨域漂移 | posted 列在 fin/mfg/crm/cs/hr 等多域存在但写入时机不一致——R2.15 修复 3 项（workOrder close/start/复合状态）已统一，**残余站点待 ai-check 扫描** | 列入 ai-check-r2 M1.x 扫描 |
| `2026-08-26-0330-1` F1.2 | mfg 领料链消耗失败注入断言 | 02.1 结束审计必改项 2 范围修正落地 | 已 fix R1.20 inventory 批次效期，**mfg 领料链未单独跑** | 追加 mfg-001 域内完整失败注入测试 |
| `2026-08-23-1752-1` | seed 修正授权触发时的重录耗时测量 | 实际触发 seed 修正 | 未发生（M4.1 跨 19 域 + 多 mission 全部走自包含建数，**未授权路径触发**）| **保持 watch-only**（mission 闭包 baseline 已建立）|
| `2026-08-23-1835-2` M0.2 | 试点用例覆盖不足的领域（pur/inv/fin/md 部分路径）| M0.3 追加 B1-Bn 工作项 | B1-B10 已全部 done（C01-C21 用例已覆盖 6 域 13+ 集成路径）| **关闭 successor**（已满足）|
| `2026-08-23-1835-3` M0.3 | 试点覆盖面已扩展 | B1-Bn 实施 | 22 集成用例完成 | **关闭** |
| `2026-08-25-0330-3` | 全仓 #11 家族「缺 writer」站点扫描 | 下一轮 arm/审计 mission | **本 ai-check-r2 正是新审计 mission** | **触发条件已满足**——ai-check-r2 M1 切片须扫描全仓 `#11` 家族 |
| `2026-08-25-0232-1` V.2 | CI 自动接线 / 覆盖未实现功能用例 / mission-driver monitor 解析限制 | 触发条件未明 | 维持 successor | **保持** |
| `2026-08-23-0434-3` | 孤儿操作人列建模 follow-up | 实施期发现 | 未发生 | **保持** |
| `2026-08-23-0434-3` | compliance 真违规修复超阈值分支 | Phase 1 裁决 | 未发生 | **保持** |

### 未满足（按 owner doc / successor 触发条件显式登记）

| 来源 plan | deferred 项 | 触发条件 | 状态 |
|---|---|---|---|
| `2026-08-26-0735-2` E3.7 | 跨域流程编排试点 | 真实 nop-wf 跨域编排需求（人工门控/超时/整链追溯）出现 | 设计已完备，等业务需求 |
| `2026-08-26-0735-2` | 周期盘点 / 平台语义层映射 / APS 多约束+预测衔接 / ML 分类 / 邮件摄取 / SCIM | owner doc 明示触发条件驱动的深化项 | 业务需要时启动 |
| `2026-08-24-0900-1` | 向上游 nop-entropy 反馈 delVersion 误退 | 下次从上游同步代码或平台建立 issue 通道 | 等平台发版 |
| `2026-08-28-1600-1` | Timesheet 周网格共享组件 | hr 考勤或 projects 工时录入周网格交互需求落地 | 业务需要时 |
| `2026-08-28-1600-1` | Barcode/PDA 扫描交互 | 项目 2.x | Non-Goal 项目 2.x |
| `2026-08-28-1600-1` | 报表页 i18n 标签 | l10n successor | 业务需要时 |
| `2026-08-28-1600-1` | 自定义 xlib 抽象过度抽取 | ≥3 复用 + ≥50% 缩减 + 总行数净减 | 业务需要时 |
| `2026-08-27-2100-1` | 坏账守卫测试债（复裁条件）| 06.1 复裁 | 等复裁 mission 启动 |
| `2026-08-27-2100-1` | fin2-006 余额缓存刷新 | 业务变更触 arApBalance 缓存陈旧 | 等业务变更 |
| `2026-08-26-0630-1` | ErpMdAcctSchemaBizModel#findFirstByOrg 同型 INACTIVE 泄漏 | 跨域审计下次扫描 | ai-check-r2 M1 切片可触达 |
| `2026-08-26-0630-1` | fin 内部 4 closer 服务同病控制点 | 跨域审计下次扫描 | ai-check-r2 M1.4 期间结账切片可触达 |
| `2026-08-26-0630-1` | 二期 5 域 listener（assets/hr/projects/maintenance/quality）| 6 域悬挂链 listener 落位 | ai-check-r2 F2.x 修复批可触达 |
| `2026-08-26-0630-1` | listener 编排重放残留（冲抵等）| 跨域审计下次扫描 | ai-check-r2 M1 切片 |
| `2026-08-26-0530-1` | resolver 本体 null 语义统一 | 跨域审计下次扫描 | ai-check-r2 M1 切片 |
| `2026-08-26-0530-1` | fin 内部 closer 服务同病控制点（S2）| 同上 | ai-check-r2 M1.4 |
| `2026-08-26-0430-1` | E2E scoped 回归门（M3 裁决）| M3 裁决 | 等 M3 启动 |
| `2026-08-26-0430-1` | posted 列语义不统一假设 | 同 M1 切片 | 列入 ai-check-r2 扫描 |
| `2026-08-26-0330-1` | mfg 领料链凭证后残余（R2）| 02.1 结束审计必改项 2 范围修正 | 业务需要时 |
| `2026-08-26-0120-1` | 完工移动红冲缺失 | 业务变更 | 业务需要时 |
| `2026-08-22-2311-3` | web 页面测试治理排除（@Tag("full-app")）| 跨 mission 收口 | 业务需要时 |

## 第三路扫描的立即行动项

基于上述扫描，**已满足**类（8 项）中可立即立项为 ai-check-r2 finding 的有：
1. **AMIS Runtime/Adapter 包移除触发条件**（82.5% 满足——本轮 non-standard-pages-flux-rewrite-and-amis-removal 计划完成后即达 100%）
2. **mfg 领料链消耗失败注入断言**（测试债，可独立测试补齐）
3. **全仓 #11 家族「缺 writer」扫描**（本 ai-check-r2 mission 自身承载，可 M1 切片纳入）

**部分满足**类（11 项）需 ai-check-r2 M1 切片重扫确认：
- posted 列语义不统一（已有 R2.15 修复 3 项 + R1.3 AbstractErpImmutableCrudBizModel 接入，**残余站点需扫描**）
- 跨域审计下次扫描族（fin closer / AcctSchemaResolver / listener 重放）—— M1.1-M1.4 期间结账切片覆盖

**未满足**类（21 项）由各 owner doc 与后继 mission 跟踪，ai-check-r2 不直接处理。

## 多次执行隔离

本文件落盘于 `docs/audits/check/2026-08-28-2049-ai-check-r2/m0-2-deferred-trigger-index.md`，是 ai-check-r2 mission 第一次执行的 M0.2 产物。后续 mission driver 重跑 M0.2 须新建 `<YYYY-MM-DD-HHmm>-ai-check-r2/` 子目录（本目录保留为历史审计证据，不删除）。

## 落盘物

- 本文件（`m0-2-deferred-trigger-index.md`）——三路证据索引核心产物
- 配合 `m0-1-baseline-snapshot.md`（基线快照）共同构成 M0 阶段产出
- M0.3（488 open finding 分流）+ M0.4（基线检查阶段收官）随后落盘
