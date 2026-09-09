# Finding 修复方法基线（ai-check 修复批操作规程）

## 定位

定义 ai-check mission 修复阶段（r1 MF / r2 M2 / r3 M2 及后续轮次同形工作项）的**统一修复操作规程**：每 finding 的强制流程、证伪路径、保护区路由、seed 联动义务、双 checker 门控、索引回填协议与同型 finding 族回填范式。

修复方法横跨 CRUD 守卫 / dict 值域 / seed / 前端 / 单测五维，非 processor 单一面，故独立成文；`processor-extension-pattern.md` 保持 Processor 模式专属职责，两者互引不重叠。

## 消费方

- `docs/backlog/ai-check-r2-roadmap.md` M2.0（同形工作项，直接引用本文，不重立口径）
- `docs/backlog/ai-check-r3-roadmap.md` M2.1~M2.8（域批 / P0 通道 / 族批执行者，唯一权威操作规程）
- `docs/backlog/ai-check-r3-roadmap.md` MV.1（全量回归门控引用双 checker 节）
- 后续轮次（r4+）修复阶段 M2.x 同形项

## 来源声明（不自造新规）

本文全部口径显式标注来源；与各 owner doc 冲突时以对应 owner doc 为准：

1. **r1 F0.2 修复方法基线**（`docs/backlog/ai-check-roadmap.md` §MF 引用块，经 F1.x/F2.x 20+ 修复批实证）——五步流程骨架、保护区路由、checker 不高于快照、每批 plan + 独立结束审计。
2. **r3 M2 前言**（`docs/backlog/ai-check-r3-roadmap.md` §Milestone M2）——加严口径：先写失败测试 / 证伪书面化 / seed 联动双面重录。
3. **r2 M2.0 加严口径**（`docs/backlog/ai-check-r2-roadmap.md` M2.0 行 + 修复阶段规则注记）——「必须先写失败测试再修复」固化。
4. 保护区规则唯一权威：`docs/context/ai-autonomy-policy.md` §保护区域。
5. seed 义务唯一权威：`docs/architecture/seed-data.md` §快照重录义务。
6. checker 基线唯一权威：`docs/audits/compliance-baseline.md` §BASELINE 机器块 + `docs/audits/cjk-baseline.md`。

## 五步强制流程（每 finding）

> 来源：r1 F0.2 步骤 1/3/4 + r2 M2.0 / r3 M2 前言把「先写失败测试」从可选固化为强制第 2 步。

1. **读报告 finding**：控制点（file:line）、证据、建议修复方向；跨轮查重（同型已 fixed 复用范式、同型 open 归并原 ID，按各轮冻结清单 §跨轮查重列程序）。
2. **先写失败测试**：复现缺陷。**最低断言强度 = 缺陷的可观察行为断言**——状态翻转 / 守卫拒绝 / 数值守恒 / 错误码抛出等行为结局，**不得**仅断言类型、签名或方法存在性（此类断言对实现与旧实现不可区分，失去回归价值）。测试落位按 `docs/architecture/testing-strategy.md`（域 `erp-*-service` 测试或 `app-erp-test-data` / 集成用例；快照纪律与异步过账时序模型同见该文）。
3. **复现**：失败测试红，确认测试确实捕获缺陷。
4. **修复**：最小完整修复；不借机改无关行为（对齐 lesson 09：不得借英文化批改吞异常行为之类的顺手改）。
5. **测试绿 + 既有测试零回归**：域模块测试全绿 + 全 reactor `mvn test` 零新增失败（对照 `docs/testing/known-good-baselines.md` 最新行登记的预存失败清单）。

## 证伪路径（not-a-problem）

> 来源：r1 F0.2 步骤 3 证伪分支 + r2/r3「书面 not-a-problem 说明（理由 + 证据）」加严。

验证后判定非缺陷时，**不接受口头/会话内裁决**，必须书面落盘：

- **格式**：finding ID + not-a-problem 结论 + 理由 + 证据（HEAD 时点 file:line 实仓复核，禁引用陈旧快照断言——lesson 13）+ 轮次/计划指针。
- **落位**：轮内执行目录索引/计划勾选注记（r3 口径集中回填归收官项，见下文「索引回填协议」）。
- **i18n 豁免特例**：CAT-2/3/4 类面走白名单显式登记（文件 + 理由 + owner doc 指针四要素齐备，按 `docs/architecture/i18n-compliance.md` 判定准绳），不接受无登记豁免。

## 保护区路由（修复批执行前置门）

> 来源：r1 F0.2 保护区路由段；现值对齐 `docs/context/ai-autonomy-policy.md` §保护区域表（该表为唯一权威，本文不复制其完整条款，仅给路由判定）。

| 修复触及面 | 路由 | 必需证据 |
| --- | --- | --- |
| `model/*.orm.xml` / `model/*.api.xml` | auto + dual-agent-approval | design doc + plan audit + 两个独立子 agent 批准（批准记录落盘计划） |
| 会计/财务过账（posting） | plan-first | owner doc + tests |
| auth/permissions | plan-first | owner doc + tests |
| 数据删除 | auto + dual-agent-approval | owner doc + tests + 双子 agent 批准 |
| 部署/外部集成 | plan-first | owner doc + tests |
| nop-chaos-flux / nop-chaos-next / nop-entropy 外部仓库 | auto + dual-agent-approval（仅新增测试/复现用例除外） | 跨仓库 plan + 双子 agent 批准 |

**批次执行形态**（r1 F0.2 口径）：每个修复批执行前建 plan（`docs/plans/{ts}-<mission>-fix-{id}.md` 命名沿各轮 roadmap 约定），批次完成经独立结束审计；执行者不得自我审计。

## seed 联动义务

> 来源：r3 M2 前言「seed 联动」节；义务细则唯一权威 = `docs/architecture/seed-data.md` §快照重录义务。

修复若触及部署期 seed 资产（`_init-data` CSV/SQL），**触发双面重录**：面 1 受影响域 `_cases` 快照 + 面 2 `app-erp-all` 集成用例快照；同步履行提交说明登记重录范围 + E2E 数值断言联动评估 + `TestErpSeedDataIntegrity` 门禁全绿。

## 双 checker 门控（修复后不高于基线）

> 来源：r1 F0.2「修复后 compliance checker 不得高于 C0.2 快照」+ r3 轮 CJK 门控（M0.2/M0.3 交付）。

1. **compliance checker**：`bash docs/audits/nop-compliance-checker.sh` exit 0，逐规则 actual ≤ `docs/audits/compliance-baseline.md` §BASELINE 机器块。合法新增（文档化范式的镜像站点等）走**独立 baseline-raise 裁决计划**（per-site 证据 + 显式更新 BASELINE 块），**不得顺手放宽**。
2. **CJK checker**：`node tools/check-hardcoded-cjk.mjs --strict` PASS exit 0（0 新增违规，CAT1..4 持平冻结快照；豁免走白名单显式登记）。
3. **漂移即停**：出现 actual > baseline 时按 `docs/context/project-context.md`「Compliance 基线漂移」失败模式开独立基线裁决，不在修复批内消化。

## 索引回填协议

> 来源：r1 F0.2 步骤 4（逐条回填）+ r3 轮集中化裁决（r3 roadmap M2.9「双索引状态回填」义务）。

- **finding 状态机**（`docs/audits/check/ai-check-index.md` §Finding 追踪）：`open` → `verifying`（测试验证中）→ `fixed`（附测试证据与提交指针）/ `not-a-problem`（附书面说明）/ `deferred`（显式例外，登记触发条件）。
- **r3 轮口径（centralized）**：修复批只产「fixed + 测试与提交指针 / not-a-problem + 说明」**证据注记**（落轮内执行目录计划/报告）；跨轮索引与本轮索引的行状态回填**集中归 M2.9 收官项**，修复批不改索引行。
- **r1 F0.2 替代模式（逐条回填）**：修复完成即回填 `ai-check-index` 状态（fixed + 测试与提交指针 / not-a-problem + 说明）。差异：逐条回填实时性高但要求每批执行者直接操作跨轮真相源，集中回填将该义务收敛到单一收官项以消除并发覆写；轮次采用哪种口径由该轮 roadmap 声明。

## 族回填范式（同型 finding 基类化）

> 来源：r1 F1.3/F1.2 先例 + r3 M1.x 双 handoff 族聚合裁决（common-014-r3 / common-015-r3）。

**判定**：同一控制点形态跨 ≥2 域/站点复现（族），且存在单一修复点（基类 / 范式约定 / 注册面）时，走族批「一处修复 + 全族回填」；仅单域孤立站点或修复点分散无共性时留域批逐站点。

**先例范式**：

| 先例 | 形态 | 修复范式 |
| --- | --- | --- |
| r1 F1.3 `AbstractErpCrudBizModel` | CRUD 状态锁族，363 文件全域接入 | 基类 + 全域接入，守卫激活由列存在性决定 |
| r1 F1.2 REQUIRES_NEW 守卫前置 | 孤儿凭证族，9+ 站点 | 守卫前置/副作用后置单一范式点，逐站点镜像 |
| r3 common-014-r3 | daoFor 无豁免注释全仓族（354 文件） | 逐文件补一行豁免 javadoc 或注入 I*Biz，**按域分片认领** + 聚合追踪点（U20）计数对账 |
| r3 common-015-r3 | `new ServiceContextImpl()` 裸 new 族（82 站点/61 文件） | 单一范式镜像（ExpenseCostAggregator ctx 兜底）+ job 入口逐站点豁免裁决 |

**分片与验收**：族批按域分片认领；每片验收口径 = 该族 checker/grep 断言归零 + 域测试零回归 + 双 checker 不高于基线；聚合追踪点 ID 承担全族计数对账（移交槽位 finding 维持各自 ID 范围不动，由聚合点统一销账）。
