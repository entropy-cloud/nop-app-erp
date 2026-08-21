# 2026-08-21-1045-2-bigint-id-m13-common-service-orgid-string 主键/外键 string 化 M1.3：common-service 组织隔离 orgId String 语义适配

> Plan Status: active
> Mission: id-string-migration
> Work Item: M1.3
> Last Reviewed: 2026-08-21
> Source: `docs/backlog/id-string-migration-roadmap.md` M1.3
> Related: `docs/plans/2026-08-21-1045-1-bigint-id-m0-order-freeze-audit-proofs.md`（M0.1，前置）、`docs/plans/2026-08-21-1045-3-bigint-id-m11-master-data-migration.md`（M1.1，后继，依赖本计划先完成）
> Audit: required

## Current Baseline

- **三文件 Long 语义现状**（module-common-service/src/main/java/app/erp/common/org/）：
  - `ErpOrgContext.java`：`currentOrgId(IServiceContext)` 返回 `Long`（:30-36，经 `toLong` :55-74 做 Number/String→Long 宽容转换）；`setCurrentOrgId(IServiceContext, Long)`（:44-48）；`readFromProvider`（:50-53）。
  - `ErpOrgIsolationOrmInterceptor.java`：`stampOrgId` 取 `Long orgId`（:41）、`orgId.equals(current)` 比较后 `orm_propValueByName(PROP_ORG_ID, orgId)` 写值（:49-53）。**迁移后若不改**：String 列值 vs Long 比较，`Long.equals(String)` 恒 false → 隔离开启时每次 save 重复 stamp。
  - `ErpOrgIsolationQueryTransformer.java`：`Long orgId = ErpOrgContext.currentOrgId(context)` + `FilterBeans.eq(PROP_ORG_ID, orgId)`（:60-61）。**迁移后若不改**：eq 过滤值类型 Long vs String 属性 → 查询过滤失效或类型错。
  - 三处均走反射 API / Object 通道，**编译器不报错**，属路线图横切 §3 语义陷阱类别，路线图显式归属 M1.3。
- **orgId 列规模**：226 个真实 FK 列分布全部 18 业务域（工具 scan 口径；XML 出现 404 次含 178 处 `<index>` 索引成员引用）。master-data orm 含 6 个 orgId FK 列 → M1.3 必须先于 M1.1（路线图第 2 轮审查 MAJOR 裁定 + 依赖图 M1_3→M1_1 边）。
- **全仓外部调用点（M0.1 审计 ③ 复核口径）**：唯一编译级外部调用 = `module-finance/erp-fin-service/src/test/java/app/erp/common/org/TestErpOrgIsolation.java`（`ErpOrgContext.setCurrentOrgId(ctx, 2L)` :72 编译级；`ContextProvider.setContextAttr(CONTEXT_ATTR_CURRENT_ORG_ID, 2L)` :56,87,102 Object 通道非编译级）。main 代码无 ErpOrgContext 直接调用；业务侧 `eq("orgId", xxx.getOrgId())` **同域** getter→eq 点随各域迁移编译器驱动自洽，**跨域** getter→eq 点（如 ast 消费 inv 实体的 `ErpAstInventoryProcessor.java:114`、b2b :216）在两域迁移窗口期存在值类型错配且编译器不报错——归 M0.1 审计 ③ 清单 + 消费域 plan grep 门控，不归本计划。
- **common-service 模块形态**：无 orm 模型（非 `model/*.orm.xml` 保护区域）；现有测试仅 `TestMaskAuditRecorder` 1 个文件（与 org 无关）；组织隔离合冋试验 `TestErpOrgIsolation` 在 fin-service test（依赖真实 orm 会话，common-service 模块内无法承载）。
- **config-gated 默认关闭**：`ErpOrgIsolationConstants.CONFIG_ORG_ISOLATION_ENABLED` 默认 false → 单组织基线零行为影响（三文件 Javadoc 明示）。
- **剩余差距**：三文件仍是 Long 语义；orgId 列开始迁移（M1.1 起）前若不先行适配，隔离开启路径产生恒 false 比较 / 过滤值类型错。

## Goals

- 三文件 orgId 处理由 Long 语义改为 String 语义（转换、比较、stamp、eq 过滤值全链 String）。
- `ErpOrgContext` 转换函数保持过渡期宽容：接受 String / Number / null / 空白（未迁移写入方仍可能向 context attr 放 Long）。
- 为 `ErpOrgContext` 纯逻辑补单元测试（common-service 模块内可承载，无 orm 依赖）。
- 登记 `TestErpOrgIsolation`（fin-service）的已知中间态编译破坏，successor = M2.1 finance plan。
- 路线图 M1.3 → done + 日志。

## Non-Goals

- 不改任何 `model/*.orm.xml`（orgId 列迁移随各域 plan）。
- 不修复 `TestErpOrgIsolation`（fin-service test，随 M2.1 finance 迁移修复——其行为断言依赖 fin 实体 orgId 列类型，本计划阶段修编译也无法修运行时语义）。
- 不做任何域迁移、快照重录、E2E 修复。
- 不跑全量构建/全量测试（中间态设计使然，verify 限定 common-service 模块）。

## Task Route

- Type: `implementation-only change`
- Owner Docs: `docs/backlog/id-string-migration-roadmap.md` M1.3、`../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md` §主键设计方案 B
- Skill Selection Basis: 纯 Java 语义适配 + 单测编写；`nop-backend-dev` 覆盖 common-service 平台约定（@Inject 可见性、异常规范），`nop-testing` 覆盖非快照型单元测试基类选择。无 ORM 模型变更，不加载 orm 审计技能。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（org 隔离 config 默认关闭，无需环境变量/端口/外部服务）。

## Execution Plan

### Phase 1 - 三文件 Long→String 语义适配

Status: planned
Targets: `module-common-service/src/main/java/app/erp/common/org/{ErpOrgContext,ErpOrgIsolationOrmInterceptor,ErpOrgIsolationQueryTransformer}.java`
Skill: `nop-backend-dev`

- Item Types: `Fix | Decision`
- Prereqs: M0.1 done（审计 ③ 调用点清单复核无新增编译级调用方）

- [ ] Fix: `ErpOrgContext`——`currentOrgId` 返回 `String`；`setCurrentOrgId(IServiceContext, String)`；`toLong` 改 `toStringValue`（String 先做数字合法性校验，合法直接返回、非法返回 null；Number 经 `String.valueOf`；null/空白返回 null——**校验式契约**：与现状 `toLong` 的 no-op 语义对齐，非法输入统一静默返回 null）。Javadoc 同步（Long→String、过渡期宽容说明）。
  - Skill: `nop-backend-dev`
- [ ] Fix: `ErpOrgIsolationOrmInterceptor.stampOrgId`——局部变量 `Long orgId` → `String orgId`；`orgId.equals(current)` 语义保持（String vs String；列值经 `orm_propValueByName` 读取为 Object，须先做 String 归一比较再 stamp，比较与写入均 String）。
  - Skill: `nop-backend-dev`
- [ ] Fix: `ErpOrgIsolationQueryTransformer.transform`——`Long orgId` → `String orgId`，`FilterBeans.eq(PROP_ORG_ID, orgId)` 过滤值 String。
  - Skill: `nop-backend-dev`
- [ ] Decision: 转换函数过渡期宽容 vs 严格 String-only——选宽容（Number/String 均接受）：过渡期内未迁移写入方（如 `TestErpOrgIsolation` 的 `setContextAttr(..., 2L)`）向 context 放 Long 仍可读通；严格模式无收益且增加中间态破坏面。残留风险：宽容转换对非法字符串静默返回 null（与现状 no-op 语义一致，隔离开启时表现为跳过 stamp，可接受）。
  - Skill: none

Exit Criteria:

- [ ] 三文件零 `Long` orgId 语义残留（Phase 2 grep 门控证明）；common-service 编译通过（`mvn clean install -pl module-common-service -am -DskipTests`）

### Phase 2 - 单元测试 + 语义陷阱 grep 门控

Status: planned
Targets: `module-common-service/src/test/java/app/erp/common/org/TestErpOrgContext.java`（新增）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1

- [ ] Add: `TestErpOrgContext` 纯单元测试（无 orm 依赖，覆盖转换矩阵）：context attr 为 `String "2"` → `currentOrgId` 返回 `"2"`；为 `Long 2L` → 返回 `"2"`（过渡宽容）；为 `String "abc"` → 返回 null（非法输入 no-op，锁定校验式契约）；为 `null` / 空白串 → 返回 null；`setCurrentOrgId(String)` 写入后可读回；`isActive` 在隔离 config 关闭时恒 false（config-gated 默认关闭行为锁定）。
  - Skill: `nop-testing`
- [ ] Proof: 语义陷阱 grep 门控（路线图 M1.3 指定清单）——`grep -nE "orgId\.equals|FilterBeans\.eq\([^,]*PROP_ORG_ID" module-common-service/src/main` 逐条人工核对为 String 语义；`grep -nE "Long" module-common-service/src/main/java/app/erp/common/org/` 零命中（Javadoc 历史注记除外，逐条列出）。结果记录本计划文件。
  - Skill: none
- [ ] Proof: 域级 verify——`mvn clean install -pl module-common-service -am -DskipTests` 全绿 + `mvn test -pl module-common-service` 全绿（含新增测试）。
  - Skill: `nop-testing`

Exit Criteria:

- [ ] 新增单测全绿且覆盖转换矩阵全部形态；grep 门控零残留（例外逐条列出并核可）
- [ ] common-service 模块 build + test 全绿

### Phase 3 - 中间态破坏登记 + 状态流转

Status: planned
Targets: 本计划文件、`docs/backlog/id-string-migration-roadmap.md`、`docs/logs/2026/08-21.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 2

- [ ] Add: 登记 `TestErpOrgIsolation`（fin-service test）编译破坏为已知中间态产物（本计划 Deferred But Adjudicated + M0.1 审计工件调用点清单 successor 标注 M2.1）——`setCurrentOrgId(ctx, 2L)` 编译级破坏，M2.1 finance plan 修复为 String 形态并随 fin 实体 orgId 列迁移恢复运行时语义。
  - Skill: none
- [ ] Add: 路线图 M1.3 → `done`；日志条目（含验证状态）。
  - Skill: none

Exit Criteria:

- [ ] 中间态破坏在计划与审计工件双登记且 successor 明确；路线图状态与日志一致

## Draft Review Record

- Independent draft review iteration 1 (ses_fddc28f91ffe24EE6xkdZlnD63): `passes draft review` — 0 BLOCKER / 0 MAJOR / 3 MINOR（① 转换器契约「直接返回 vs 校验」内部不一致 + 测试矩阵缺 `"abc"` 用例；② 窗口期未迁移域隔离开启的 String 入 Long 属性退化未登记；③ 「随各域迁移自洽」表述遗漏跨域 getter→eq 点归属）。基线 file:line、唯一调用点、测试可行性、verify 命令、M2.1 successor 推理均验证通过。
- 修订（iteration 1 → 2）：三 MINOR 全部处理——转换器契约为校验式（非法→null）+ `"abc"` 测试用例；新增「窗口期隔离开启于未迁移域」watch-only 登记；基线改写区分同域/跨域 getter→eq 点归属（跨域归 M0.1 审计 ③ + 消费域 plan）。
- **裁定：iteration 1 verdict 为 passes + 三项处方 MINOR 已逐条落地，共识达成，Plan Status → active。**

## Closure Gates

> 本计划改 1 个共享模块的 3 个生产文件 + 新增 1 个测试类，不改 orm 模型/公共契约。完整仓库验证定制为：common-service 模块级 build + test（中间态全量构建失败属路线图设计使然，禁止以全量构建为 gate）。

- [ ] 范围内行为完成（三文件 String 语义 + 单测 + grep 门控零残留）
- [ ] 相关文档对齐（三文件 Javadoc、路线图 M1.3 状态、M0.1 审计工件 successor 标注、日志）
- [ ] 已运行验证：`mvn clean install -pl module-common-service -am -DskipTests` + `mvn test -pl module-common-service` 全绿；grep 门控记录在案
- [ ] 无范围内项目降级为 deferred/follow-up（TestErpOrgIsolation 修复非本计划范围，属路线图预先裁决的中间态设计，登记非降级）
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### TestErpOrgIsolation（fin-service test）编译破坏

- Classification: `watch-only residual`
- Why Not Blocking Closure: 该测试同时依赖 `ErpOrgContext` API 签名与 fin 实体 orgId 列类型（`seedArApItem(long id, long orgId)`、`assertEquals(2L, saved.getOrgId())`），只有 M2.1 finance 迁移时才能同时修复编译与运行时语义；本计划阶段任何单独修复都无法恢复其运行时行为。fin-service test 不在 M1.3/M1.1 的任何 verify 闭包内（-am 只向上游构建），中间态破坏面为零。
- Successor Required: `yes`（M2.1 finance plan：签名 String 化 + 断言随 fin 迁移重录）

### 窗口期隔离开启于未迁移域的运行时退化

- Classification: `watch-only residual`
- Why Not Blocking Closure: M1.3 落地后至各域迁移前，若在某未迁移域（orgId 列仍 Long）手动开启隔离开关：stamp 写 String 入 Long 属性、eq 过滤值 String vs Long 列——相比 M1.3 前的 Long/Long 一致性属运行时退化（编译器不报错）。实际风险≈零：config 默认关闭、全仓唯一开启方 TestErpOrgIsolation 本身已编译破坏、无生产部署开启。窗口随各域迁移自然关闭。
- Successor Required: `no`（触发条件：出现任何在未迁移域上开启 org 隔离的部署/测试需求时，先行该域迁移或临时关闭隔离）

## Closure

Status Note: pending

Closure Audit Evidence:

- Auditor / Agent: pending
- Evidence: pending

Follow-up:

- 无（已确认缺陷零；TestErpOrgIsolation 已按中间态设计登记 successor）
