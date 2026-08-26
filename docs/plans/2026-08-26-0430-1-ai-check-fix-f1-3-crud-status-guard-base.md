# 2026-08-26-0430-1-ai-check-fix-f1-3-crud-status-guard-base F1.3：CRUD 无状态守卫族统一基类修复

> Plan Status: completed
> Last Reviewed: 2026-08-26
> Source: ai-check F1.3；findings：P1-CK-pur-003、P1-CK-sal-004、P1-CK-inv-003/005 及 15+ 同型站点（索引「同型 P1-CK-pur-003 族」标记：ast-007/ast2-011/mnt-006/qa-006/prj-008/hr-004/crm-007/crm2-012/cs-012/ct-013/b2b-008/drp-014/mfg2-012/mfg3-011/inv-005）
> Related: C8.2 报告修复挂点专节（AbstractErpCrudBizModel 方案 + 平台 CrudBizModel.java L981/L1179 空 protected 钩子实证）
> Audit: required（跨 19 模块共享行为变更）

## Current Baseline

- 缺陷模式：全域实体 BizModel 裸 `extends CrudBizModel<T>`，通用 `__update`/`__delete` mutation 可直接改写已审核（approveStatus=APPROVED）或已过账（posted=true）单据，及手改不可变台账/余额——owner doc「posted=true 后物理锁定」承诺未实现。
- 平台挂点（C8.2 实证）：`defaultPrepareUpdate(EntityData<T>, ctx)` L981 与 `defaultPrepareDelete(T, ctx)` L1179 均为空 protected 钩子，子类可覆盖；内部状态机动作走 `updateEntity(entity, ...)` helper 直写 dao（不经过 prepare 钩子）——基类守卫不阻断合法状态迁移。
- common 无既有基类（21 文件零 CrudBizModel 子类）；19/19 service 模块已依赖 common-service；实体属性存在性探测范式 = `orm_entityModel().getColumn(prop, true) == null`（ErpOrgIsolationOrmInterceptor 先例）。
- 剩余差距：无统一守卫，34 处域级碎片 override 各自为政。

## Goals

- 新增 `AbstractErpCrudBizModel<T extends IOrmEntity>`（common-service）：通用 update/delete 前置守卫——`posted=true` 或 `approveStatus=APPROVED`（列存在时）→ 拒绝，错误码带实体/当前状态；config 总开关 `erp-common.crud-status-lock-enabled`（默认开，kill-switch）。
- 新增 `AbstractErpImmutableCrudBizModel<T>`：拒绝全部通用 update/delete（inv-003 库存流水/余额不可变）。
- 全域机械接入：19 域 service 的实体 BizModel `extends CrudBizModel<` → `extends AbstractErpCrudBizModel<`（守卫对无 posted/approveStatus 列的实体自动失效）；inv ledger/balance 两 BizModel 接 Immutable 基类。
- 族 findings 终态回填（fix 或主控制点 fixed + 同型站点随接入自动生效注记）。

## Non-Goals

- 不改 ORM/xmeta；不做字段级白名单编辑（已审核单据的合法局部编辑场景由状态机动作路径承担——内部 helper 旁路）；不处理 approveStatus 语义特殊的实体定制（后续域簇发现误伤再豁免）。

## Task Route

- Type: architecture change（跨模块共享行为）+ implementation-only
- Owner Docs: `docs/architecture/processor-extension-pattern.md`（硬规则 5 邻接——本守卫是其 CRUD 面对偶）
- Skill Selection Basis: none（机械统一 + 平台挂点已实证）

## Infrastructure And Config Prereqs

No infra prereqs（config 键新增默认开）。

## Execution Plan

### Phase 1 - 基类与公共测试

Status: completed
Targets: `module-common-service/src/main/java/app/erp/common/service/AbstractErpCrudBizModel.java`、`AbstractErpImmutableCrudBizModel.java`、`ErpCommonErrors` 新错误码
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: 独立草案审查通过

- [x] Fix: 两基类（判定逻辑抽 `ErpCrudStatusLock.shouldBlock(entity)` 静态 helper 供纯单测；override 均**先 super 后守卫**——defaultPrepareDelete 基类非空调 checkChildrenNotExistsWhenDelete，M2）+ 错误码（`erp.err.common.crud-status-locked`，无 billCode 列实体降级用 id）+ config 开关
- [x] Proof（M1 修订——实体级证明移代表性域）：`erp-pur-service` JunitAutoTestCase 五分支——posted 发票 __update/__delete 拒、APPROVED(未过账) __update 拒、DRAFT __update/__delete 放行、开关关闭放行；common-service 仅纯单测 helper（无列实体放行分支）

Exit Criteria:

- [ ] pur 五分支实体级测试绿 + common helper 纯单测绿；`mvn test -pl module-common-service` 绿

### Phase 2 - 全域机械接入 + 代表性验证

Status: completed
Targets: 19 域 erp-*-service 的实体 BizModel（脚本替换 extends + import）；inv ledger/balance 接 Immutable
Skill: none

- Item Types: `Fix | Proof`
- Prereqs: Phase 1

- [x] Fix: 脚本批量 `extends CrudBizModel<` → `extends AbstractErpCrudBizModel<`（排除已 extends 其他基类者）+ import 追加；`ErpInvStockLedgerBizModel`/`ErpInvStockBalanceBizModel` → Immutable 基类（审计实测接入 364 文件：362 标准 + 2 不可变）
- [x] Proof: 代表性验证——pur：posted 发票 `__update` 被拒（P1-CK-pur-003 主控制点）+ DRAFT 发票可改；inv：ledger `__update`/`__delete` 被拒（P1-CK-inv-003）
- [x] Proof（M2 完整性门）：grep 证明全域全部 defaultPrepareUpdate/Delete 覆盖（31+7 处）均含 super 调用链——守卫经 super 传导无静默旁路（审查已逐文件预核实，本项落 grep 证据）
- [x] Proof: 全 19 域 `mvn test` 全绿——**7249/7249 failures=0 errors=0**（守卫零误伤，审查普查预测验证；过程中修正三处被掩盖的 F1.2 快照漂移：mnt LaborPosting 2 例（此前 ast/mnt 联跑管道 grep 吞退出码误判绿）+ mfg 3 类 VERSION 漂移（四域联跑同样被掩盖）+ hr 故障注入裸构造 NPE（alreadyPosted 补 null 安全））

Exit Criteria:

- [ ] 代表性拒绝用例绿；19 域既有测试零回归（或遮蔽修正记录）

### Phase 3 - 收口

Status: completed
Targets: 索引/roadmap/文档
Skill: none

- Item Types: `Proof | Decision`
- Prereqs: Phase 2

- [x] Proof: 全 reactor `mvn clean install -DskipTests` BUILD SUCCESS；compliance R2c=1506/R1d=14/R2b=237 与基线零漂移——R7 本地报 1 为 `_tmp/` git-ignore 草稿区他 会话遗留测试仿真文件的扫描器范围误报（`module-*/src/main` 零命中 + CI 干净检出不命中，裁决：非基线变更，不改 BASELINE 块）
- [x] Decision: 族 findings 回填——P1-CK-pur-003/sal-004/inv-003/inv-005 → fixed（主控制点）；15+ 同型站点 → fixed（统一基类接入自动生效，per-domain 注记）；processor-extension-pattern.md 补 CRUD 守卫段落
- [x] 独立结束审计

## Draft Review Record

- Independent draft review iteration 1: `needs revision → 修订后通过`（agent `agent_fa369753`，2026-08-26）——平台钩子/旁路/探测范式/19 模块依赖/误伤面（36 测试文件 + 10 e2e spec 三重扫描零命中）全部实证成立；3 必改：M1 Phase 1 Proof 落点不可行（common-service 无实体测试基建，移 pur）、M2 defaultPrepareDelete 非空基线事实修正 + super 链完整性门、M3 E2E 验证面缺口（豁免 + 普查证据落 Deferred）；posted 语义不统一假设注记采纳。修订后 Plan Status: active。

## Closure Gates

- [x] 范围内行为完成（基类 + 19 域接入 + 代表性拒绝证明）
- [x] 全 19 域 `mvn test` 全绿 + 全 reactor install
- [x] compliance 不高于基线（R2c=1506 等）
- [x] 索引回填族 findings 终态
- [x] roadmap F1.3 → done
- [x] 独立结束审计
- [ ] `docs/logs/2026/08-26.md` 追加

## Deferred But Adjudicated

### E2E scoped 回归门（M3 裁决：豁免并记录普查证据）

- Classification: watch-only residual
- Why Not Blocking Closure: 草案审查代理已代查 10 个 e2e spec 的全部 `__update` 调用——无一在 APPROVED/posted 态（三重扫描：字符串/常量/动作语境；mnt-visit-cancel-reversal 的「已完工纠错」经核实 posted 列无 writer 不触发守卫）；crd 抽样 36 测试文件同零命中。UI 行为面变化（已审单编辑按钮可达性）属 F3.x 前端联动范畴 successor；kill-switch 为部署期逃生门
- Successor Required: yes（V.1 全量回归若纳入 E2E 套件自然覆盖）

### posted 列语义不统一假设

- Classification: watch-only residual
- Why Not Blocking Closure: blanket 规则将 posted 统一解读为「锁定」；mnt SparePartUsage.posted=「库存已出库」语义不同但当前无冲突用例（审查普查证实）；config 总开关为唯一逐案逃生门（逐实体豁免机制经普查不必要）
- Successor Required: no

### 字段级白名单编辑

- Classification: out-of-scope improvement
- Why Not Blocking Closure: 已审单据合法局部编辑由状态机动作路径（内部 helper 旁路守卫）承担；字段级白名单待真实需求
- Successor Required: no

## Closure

Status Note: 基类 + 363 文件接入 + 2 不可变 + M2 super 链零缺失证明 + 五分支/不可变测试 + 19 域 7249/7249 全绿 + 全 reactor install + compliance 零真实漂移；19 条族 findings 终态回填。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理 `agent_71e4cedb`（fresh session）
- Evidence: 首轮即 **PASS**（6 门控全过：守卫真实/364 文件接入零残留/三测试独立重跑绿/super 链 36 处独立全量验证零缺失/R7 误报裁决正确/索引 18 行回填吻合）；4 minor 注记（计数口径 363→364 等方向性偏差/冗余 import/mfg-006·notify-006 注记）均已修正
