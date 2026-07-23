# 2026-07-24-0605-3-daofor-type1-orm-navigation-refactor-batch1 daoFor Type 1（ORM 导航可替代）重构第一批 + ORM 关系缺口裁决（F1 successor）

> Plan Status: active
> Mission: erp
> Work Item: daoFor Type 1 真违规子集重构第一批 + ORM 关系缺口分类
> Last Reviewed: 2026-07-24
> Source: `docs/plans/2026-07-24-0930-3-governed-path-cost-eval-arch-doc-alignment.md` §Follow-up「Type 1（~100-150 ORM 导航可替代）daoFor 按域分批重构计划（Phase 1 裁决=可安全重构，无前置阻塞）」+ `docs/analysis/governed-path-cost-evaluation.md` §3.2/§4
> Related: `docs/audits/2026-07-23-0000-architecture-governance-review.md` §F1（HIGH）、`docs/plans/2026-07-16-2134-1-ddd-entity-methods-daofor-convergence.md`（daoFor 6 类分类，Type 1 定义来源）、`docs/plans/2026-07-24-0930-1-compliance-guard-activation-ci-baseline.md`（checker R2b=319/R2c=1108 基线门控）
> Audit: required

## Current Baseline

governed-path 成本评估（`docs/analysis/governed-path-cost-evaluation.md`）已裁决：daoFor 真违规子集分两类——**Type 1（~100-150 处 ORM 导航可替代）可立即安全重构**（不引入跨域 service 耦合，不破坏单模块测试）；Type 4（~10-30 跨域写/读）受阻塞待平台解耦。`2026-07-24-0930-3` Follow-up 明示 Type 1「无前置阻塞，开按域分批重构计划」。

实时仓库核实（2026-07-24）：

- checker 基线：R2b（BizModel 跨域 daoFor）= **319**，R2c（全生产代码 daoFor）= **1108**，R2a（BizModel daoFor ErpMd*）= 37，R2d（Processor daoFor ErpMd*）= 34。
- daoFor 按域分布（service 层 src/main/java，`_gen` 排除）：finance **232** / manufacturing **150** / inventory 98 / assets 90 / projects 63 / drp 56 / purchase 52 / sales 49 / hr 46 / quality 45 / crm 42 / b2b 36 / maintenance 29 / contract 25 / master-data 22 / logistics 16 / cs 13 / aps 11 / notify 10。
- Type 1 子集**尚未精确枚举**——`2026-07-16-2134-1` 给出 6 类估算（Type 1 ~100-150）；该计划 Phase 4 已落地 6 处样本，故剩余 Type 1 估算为 ~94-144（在分类自身 ±15pp 容差内，Phase 1 枚举时自校正）。判定 Type 1 的条件：该 daoFor 可用既有 ORM 关系 getter（`entity.getLines()` / `entity.getRelation()`）替代，且该关系在同一 DAO classpath 内（无需跨域 service jar）。
- **关键残留风险**（成本评估 §3.4 残留风险 2）：Type 1 重构需逐处确认 ORM 关系确实存在；**部分 daoFor 是因关系未建模**，个别可能需补 ORM `<to-one>`（属 ORM 保护区域，需 owner doc 授权）→ 这些不在本计划范围，须 Explore 阶段分类为 ORM-gap successor。
- 既有豁免登记（`posting-exemptions.md`）：MrpReleaseService（mfg→pur 写）、ErpCtRebateSettlementBizModel（contract→pur/sal 写）、ErpB2bAsnBizModel（b2b→pur 写）——均为 Type 4 写豁免，不在 Type 1 重构范围。

剩余差距：Type 1 子集无逐处清单 → 无法机械重构；governed-path 合规（R2b/R2c 基线）尚未下降。

## Goals

1. **枚举并分类 Type 1 子集**：逐处判定每个候选 daoFor 是否可安全重构为 ORM 导航（关系存在）vs 需补 ORM 关系（ORM-gap）vs 已属其他类（Type 2/3/5/6 合法或 Type 4 阻塞）。
2. **第一批重构 ORM-gap=0 的安全子集**：在 R2b 计数最高的 1-2 域（finance/manufacturing 代表）将 ORM 关系已存在的 Type 1 daoFor 改为 ORM 导航 getter，下降 checker R2b/R2c 基线。
3. **建立重构范式 + checker 基线门控更新流程**：为后续按域分批重构 successor 提供可复用模式。

## Non-Goals

- **不改 ORM 模型 / 不补 ORM `<to-one>` `<to-many>` 关系**（保护区域，ask-first）——ORM-gap 子集分类为 Deferred successor（触发：ORM 关系授权）。
- **不重构 Type 4（~10-30 跨域写/读）**——阻塞中，待 nop-entropy 平台 lazy/SPI 解耦或保留登记豁免（成本评估 §3.2）。
- **不重构全 19 域**——仅第一批（最高计数域），建立范式；其余域为显式 successor。
- **不重构已登记豁免（MrpReleaseService / ErpCtRebateSettlementBizModel / ErpB2bAsnBizModel）**。
- **不重构 Type 2（同域子实体）/ Type 3（Processor 架构约束）/ Type 5（看板只读聚合）/ Type 6（历史残留）**——本计划仅 Type 1。
- **不改 biz 方法签名 / API 契约 / xbiz / 页面**——内部访问方式重构（daoFor → 关系 getter），行为不变。

## Task Route

- Type: `architecture change`（governed-path 合规结构改进，结果面 = 跨域数据访问方式收敛 + checker 基线下降）
- Owner Docs: `docs/analysis/governed-path-cost-evaluation.md`（Type 1 重构前置条件裁决）、`docs/architecture/cross-domain-constraints.md`（跨域访问写引用契约）、`docs/architecture/data-dependency-matrix.md §5.3`（禁止 IDaoProvider/IOrmTemplate 直接跨域查表）、`docs/plans/2026-07-16-2134-1-ddd-entity-methods-daofor-convergence.md`（6 类分类）
- Skill Selection Basis: `nop-backend-dev`（匹配「跨实体调用 / ORM 关系导航 / daoFor 收敛 / 产品化可定制性自检」工作方法，D1 + 0930-2 同型范式均经该技能路由）；`nop-debugging`（若重构后单模块测试异常需根因定位 ORM 关系延迟加载/级联问题）。
- Bundling 裁决（rule 14）：Type 1 枚举（Phase 1）+ 第一批重构（Phase 2）+ 基线更新（Phase 3）共享同一结果面（governed-path Type 1 合规收敛），为单一计划多阶段；不与 F2d 字面量（不同结果面/契约）或闭包 #12（验证工作）合并。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（纯 Java 重构 + ORM 关系 getter，无端口/密钥/外部服务/数据迁移）。

## Execution Plan

### Phase 1 — Type 1 子集逐处枚举 + 三态分类（Explore-heavy）

Status: planned
Targets: checker R2b 输出（319 BizModel 跨域 daoFor）+ finance/manufacturing 等高计数域 service src/main/java
Skill: `nop-backend-dev`

- Item Types: `Decision | Proof`
- Prereqs: 无（成本评估裁决已完成）

- [ ] `Proof`：产出 Type 1 候选清单——从 checker R2b 输出提取 BizModel 跨域 daoFor 站点（file:line + daoFor 目标实体 + 所在方法 + 读/写语义）。聚焦 finance/manufacturing/inventory/assets/projects（前 5 高计数域，占 ~75% daoFor）。
  - Skill: `nop-backend-dev`
- [ ] `Decision`：逐处三态分类——(a) **safe**：ORM `<to-one>`/`<to-many>` 关系已建模且在同 DAO classpath，daoFor→关系 getter 可直接替代；(b) **ORM-gap**：业务上应有关系但 ORM 未建模，需补 `<to-one>`（ORM 保护区域）→ 移出范围转 Deferred；(c) **not-Type-1**：属 Type 2/3/4/5/6（同域子实体 / Processor 约束 / 跨域写阻塞 / 只读聚合 / 历史残留）→ 排除。记录每条判定 + ORM 关系 file:line 证据（a 类）或缺口理由（b 类）+ 残留风险（边界判定：关系存在但跨域 service 才能安全导航的归 Type 4）。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] Type 1 候选清单三态分类完成（safe / ORM-gap / not-Type-1），每条带 file:line + 判定依据
- [ ] safe 子集（第一批重构框）与 ORM-gap 子集（Deferred 框）边界明确

### Phase 2 — 第一批 safe 子集重构（finance/manufacturing 代表域）

Status: planned
Targets: Phase 1 safe 子集（finance/manufacturing 域为主，ORM-gap=0 的站点）
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Item Types Note: Phase 2 is Fix-heavy (daoFor→ORM navigation)
- Prereqs: Phase 1 完成（safe 子集已落）

- [ ] `Fix`：逐处将 safe 子集 `daoProvider().daoFor(ErpXxx).getEntityById(id)` / `findAllByQuery(...)` 改为 ORM 关系 getter（`entity.getXxx()` / `entity.getXxxList()`）或同域已注入 I*Biz 的关系导航；移除冗余 daoFor/import。每域重构后 `mvn test -pl <module>/<service>` 验证单模块测试仍启动成功（成本评估 §4 前置条件 3：任何 daoFor→关系重构必须验证目标域单模块测试启动）。
  - Skill: `nop-backend-dev`
- [ ] `Proof`：抽样验证重构后行为不变——状态机判断/读聚合结果与重构前等价（关系 getter 返回同一实体/集合），经该域既有测试覆盖。
  - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] safe 子集（第一批框）全部重构为 ORM 导航，daoFor 命中数下降
- [ ] finance/manufacturing（及 safe 子集触及域；inv/assets/projects 等枚举出的 safe 站点若未在本批重构则显式归入「剩余域」successor）`mvn test` 全绿（单模块测试启动成功 + 行为不变）

### Phase 3 — checker 基线下降 + ORM-gap successor 登记 + 文档对齐

Status: planned
Targets: `docs/audits/compliance-baseline.md`（R2b/R2c 下降）、治理审查 F1、governed-path-cost-evaluation.md
Skill: none

- Item Types: `Proof | Add`
- Prereqs: Phase 2 完成

- [ ] `Proof`：`mvn clean install -DskipTests`（154 模块 BUILD SUCCESS）+ 复跑 `bash docs/audits/nop-compliance-checker.sh` 记录 R2b/R2c 新基线（应较 319/1108 下降，下降量 = 第一批 safe 子集站点数）；更新 `docs/audits/compliance-baseline.md` 基线 + 增量注记（本计划重构导致下降，非回归）。
  - Skill: none
- [ ] `Add`：ORM-gap 子集登记为 Deferred successor（逐条 file:line + 触发条件「ORM `<to-one>` 关系授权时」）于本计划 §Deferred But Adjudicated；governed-path-cost-evaluation.md 补第一批落地证据 + 剩余 Type 1 站点估算；治理审查 §F1 successor 更新。
  - Skill: none

Exit Criteria:

- [ ] 全仓 BUILD SUCCESS + checker R2b/R2c 基线下降并记录
- [ ] ORM-gap 子集登记为 successor（带触发条件）；剩余 Type 1 successor 边界明确

## Draft Review Record

- Independent draft review iteration 1: `accept` (`ses_06ef9207cffe69pnRYZLW0QD6u`，独立 general 子代理，新会话冷重播无起草者上下文，2026-07-24) — 全部 load-bearing 事实主张经实时仓库逐项核实**精确匹配**（R2b=319/R2c=1108 基线；finance 232/mfg 150/notify 10 daoFor 计数；成本评估 Type 1 可安全重构/Type 4 阻塞裁决；6 类分类；3 豁免登记；Source follow-up）。CRITICAL 核实通过：重构目标为手写 BizModel/Processor，`_gen` 排除，不触 ORM 保护区域。Phase 1 Explore 对未知 Type 1 子集的诚实范围化 + 第一批「最高计数域」单结果面经判定 defensible。R1-R14 + anti-slack 全 PASS，0 BLOCKER / 0 MAJOR。3 non-blocking MINOR（剩余 Type 1 计数 ~94-144——已在基线注记；inv/assets/projects safe 站点归 successor——已在退出标准注记；Phase 2 退出标准措辞——已收紧）。草案审查收敛 → `Plan Status: active`。

## Closure Gates

> 本计划触及服务层 Java（daoFor→ORM 导航重构），无 ORM/契约/ext:dict/biz 方法签名变更。完整仓库验证：`mvn clean install -DskipTests`（154 模块）+ 受影响域 `mvn test`（单模块启动验证）+ checker 复跑（R2b/R2c 基线下降记录）。

- [ ] 范围内行为完成（第一批 safe 子集重构 + ORM-gap 分类）
- [ ] 相关文档对齐（compliance-baseline + governed-path-cost-evaluation + 治理审查 F1）
- [ ] 已运行验证：`mvn clean install -DskipTests` + 受影响域 `mvn test`（单模块启动成功）+ checker 复跑（R2b/R2c 下降记录，非回归）
- [ ] 无范围内项目降级为 deferred/follow-up（ORM-gap 是 Phase 1 明示的 ask-first 保护区域排除裁决非范围缩减；Type 4/其余域为成本评估既定 successor）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### Type 1 剩余域重构（safe 子集第二/三批）

- Classification: `optimization candidate`
- Why Not Blocking Closure: 第一批建立范式后，剩余 14 域 safe 子集为同型按域分批重构，本计划仅覆盖最高计数 1-2 域。
- Successor Required: `yes`（触发条件：第一批范式验证通过后，按域计数高低推进）

### Type 1 ORM-gap 子集（需补 ORM `<to-one>`）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 需 ORM 关系建模（保护区域，ask-first）；Phase 1 分类后逐条登记 file:line。
- Successor Required: `yes`（触发条件：ORM `<to-one>` 关系授权 + owner doc 明示关系语义）

### Type 4 跨域写/读 daoFor（~10-30 处）

- Classification: `watch-only residual`
- Why Not Blocking Closure: 阻塞中——需 nop-entropy 平台 lazy/SPI 解耦或保留登记豁免（成本评估 §3.2）。
- Successor Required: `yes`（触发条件：nop-entropy 提供 lazy/SPI 解耦 或 业务方要求封堵 daoFor 直访）

## Closure

Status Note: pending

Closure Audit Evidence:

- Auditor / Agent: pending（独立子代理新会话）

Follow-up:

- Type 1 剩余域 safe 子集分批重构（见上触发条件）
- Type 1 ORM-gap 子集（见上触发条件）
- Type 4（见上触发条件）
