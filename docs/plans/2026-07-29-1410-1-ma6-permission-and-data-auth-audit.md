# 2026-07-29-1410-1-ma6-permission-and-data-auth-audit MA6 权限注解与数据权限审计（A6.1 + A6.2 + A6.3）

> Plan Status: completed
> Last Reviewed: 2026-07-29
> Source: `docs/backlog/audit-remediation-roadmap.md` MA6（A6.1 / A6.2 / A6.3）
> Related: plan `2026-07-29-1410-2`（A6.4 保护区域纪律审计，同批 MA6）；A2.18 多公司隔离审计（P1-MA2-093 orgId 读路径隔离未落地，A6.3 交叉复核）；A3.6 API 契约审计（P1-MA3-046 全域敏感动作零运行时权限保护，与 A6.1/A6.2 协同裁决）
> Audit: required

## Current Baseline

- MA6（安全与权限层审计）4 工作项全部 `todo`。本计划覆盖前 3 项（A6.1 权限注解完整性 grep + A6.2 finance+mfg+pur+sal 权限深度抽样 + A6.3 数据权限运行验证）；A6.4 保护区域纪律审计由 plan `2026-07-29-1410-2` 覆盖。三者共享 owner doc `docs/design/roles-and-permissions.md`（按 authoring guide rule 14 合并为单 plan 的 3 个阶段）。
- **A6.1 权限注解现状**（实仓 grep 2026-07-29）：全域 ~825 个 `@BizMutation` + ~313 个 `@BizQuery`，**零** `@BizAuth` / `@Auth` / `permissionName` / 任何角色绑定注解。操作级拦截 `nop.auth.enable-action-auth=false`（平台默认，application.yaml 未显式覆盖）；`nop.auth.skip-check-for-admin=true`（管理员跳过）。权限资源点已就绪但默认关闭：`app-erp-all/.../app.action-auth.xml`（13KB）+ 38 个生成 `_erp-*.action-auth.xml`（TOPM/SUBM/FNPT 三层，FNPT 模式 `{EntityName}:{query,mutation}`）。
- **A6.2 角色与高危操作现状**：`roles-and-permissions.md` 定义 15 角色矩阵 + 高危操作表（反审核/作废/反结账/资产处置/工单关闭/负库存放行/红冲凭证等需管理员或额外审批）+ 职责分离（创建人≠审核人）。各域 `state-machine.md` 声明迁移执行角色。但运行时是否对高危操作做**程序级**守卫（除 FNPT 层外是否有 BizModel 内显式角色/状态校验）从未被系统性抽样。
- **A6.3 数据权限现状**（实仓 grep 2026-07-29）：19 模块 `erp-{module}.data-auth.xml` **全部** `<objs/>` 空规则；**零**自定义 `IDataAuthChecker` / `IQueryTransformer`；`IServiceContext`/`IContext` 均**无** `getOrgId()`。owner doc `roles-and-permissions.md §数据权限` 声明「业务员只能看自己创建的单据」「质检员只看分配给自己的任务」等行级过滤，但 `data-auth.xml` 全空 → 声明的能力**运行时未落地**。
- **A2.18 已确认**（交叉输入）：orgId 查询隔离全仓未落地（P1-MA2-093）；11 个 dashboard BizModel 经 IDaoProvider 直访绕过仅有的（空）认证管道；单组织种子 176 行全部 `orgId=2` 完全掩盖跨组织泄漏。**A6.3 须复核 A2.18 结论 + 评估角色侧行级过滤（部门/创建人可见）缺口**（A2.18 聚焦多公司 orgId 维度，A6.3 聚焦角色侧行级过滤，互补不重复）。
- **A3.6 已登记**（协同裁决输入）：P1-MA3-046「全域敏感动作零运行时权限保护」与本计划 A6.1/A6.2 同根因——A3.6 从 API 契约面 dim4 发现，A6.1/A6.2 从安全审计纵深核实，须交叉去重并合并为单一 MR3 修复项。
- 验证基线：`mvn clean install -DskipTests` 全绿（154 模块）；`mvn test` 全绿（~2890 测试，0 failures）。
- 剩余差距：权限注解完整性从未做过全域系统性 grep 审计；4 个 S/A 级域（finance/mfg/pur/sal）高危操作程序级守卫从未抽样；数据权限行级过滤声明-落地差距从未被独立审计（仅 A2.18 从多公司维度旁证）。

## Goals

- **A6.1**：grep 全域 19 模块权限注解完整性，核实 `@BizMutation`/`@BizQuery`/敏感动作是否携带任何角色/权限绑定 + FNPT 权限点与高危操作表的覆盖关系；与 P1-MA3-046 协同裁决（合并/分裂/升降级）。输出权限注解完整性审计报告。
- **A6.2**：对 finance+mfg+pur+sal 深度抽样——职责分离（创建人≠审核人）+ 高危操作（反审核/作废/反结账/处置/红冲）程序级守卫 + 状态机迁移执行角色与代码一致性。输出权限深度抽样审计报告。
- **A6.3**：数据权限运行验证——`data-auth.xml` 规则落地 + `IDataAuthChecker`/`IQueryTransformer` 缺位 + 角色侧行级过滤（部门/创建人/分配人可见）声明-落地差距 + 复核 A2.18 orgId 读路径隔离结论。输出数据权限审计报告。
- 注册 P0（即时通道）/ P1（目标 MR3）/ P2（watch-only）发现至 `docs/audits/arm-index.md`，与 MA1-MA5 + A2.18（P1-MA2-093 等）+ A3.6（P1-MA3-046）已登记 P1 交叉去重。
- 推进 roadmap A6.1-A6.3 状态（审计产出后转 `ready`，独立 closure audit 后转 `done`）。

## Non-Goals

- 不修复权限注解缺失 / 不实现 data-auth 规则 / 不为高危操作加程序级守卫（修复属 MR3 批量修复，由 R3.0 展开机制转化）。
- 不重新审计多公司 orgId **写路径**隔离（A2.18 已完成；A6.3 仅复核**读路径**+角色侧行级过滤，聚焦互补维度）。
- 不裁决 `enable-action-auth` 灰度启用策略（owner doc `roles-and-permissions.md §灰度启用步骤` 已定义，非审计缺陷）——Phase 1 仅评估"`false` 默认对残留风险的影响范围"（风险评估），**不**做启用/灰度决策。
- 不审计保护区域**过程纪律**（A6.4 由 plan `2026-07-29-1410-2` 覆盖；本计划聚焦权限注解/数据权限的技术正确性，A6.4 聚焦触及保护区域工作的证据三件套）。
- 不解决 xwf 浏览器层审批轴不可行裁决（plan `2026-07-09-2330-1` 已权威裁决 NOT FEASIBLE）。
- 不变更任何生产代码 / ORM / 契约（纯审计）。

## Task Route

- Type: `verification or audit work`
- Owner Docs: `docs/design/roles-and-permissions.md`（角色体系 + 高危操作 + 数据权限 + 角色→权限点映射 + 运行基线）+ 各域 `docs/design/<domain>/state-machine.md`（迁移执行角色，A6.2 用）+ `docs/architecture/multi-company.md`（A6.3 复核 A2.18 用）。
- Skill Selection Basis: roadmap 明确指定 A6.1 = `open-ended-audit-prompt.md`（grep 完整性需主动搜索未知缺口，非结构化清单）；A6.2/A6.3 = `multi-dimensional-audit-prompt.md`（权限深度抽样与数据权限需跨多维度挑战——需求正确性/owner-doc 对齐/架构边界/验证充分性/回归风险）。加载后读 `docs/skills/README.md §项目定制化层`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline.
- A6.1/A6.2 为静态 grep + 代码抽样（需可跑 `rg`/读源码）。A6.3 数据权限运行验证以**静态推理 + 既有测试证据**为主（`data-auth.xml` 全空已结构性证明行级过滤未落地；若需运行时佐证可读 dashboard BizModel 直访路径的既有单测断言）。审计只读，不改代码。

## Execution Plan

### Phase 1 - 权限注解完整性 grep 审计（A6.1）

Status: completed
Targets: 全域 19 模块 `module-*/erp-*-service/src/main/java/**` + `module-*/erp-*-dao/src/main/java/**`（~825 `@BizMutation` + ~313 `@BizQuery`）；38 个 `_erp-*.action-auth.xml` + `app.action-auth.xml`；报告 `docs/audits/2026-07-29-1410-arm-ma6-permission-annotation-completeness.md`
Skill: `open-ended-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0 锚点

- [x] 全域 grep 核实 `@BizMutation`/`@BizQuery` 是否携带任何角色/权限绑定（`@BizAuth`/`@Auth`/`permissionName`/`@RolesAllowed` 等），产出"注解形态矩阵"（裸注解 vs 带绑定）
- [x] 核实 FNPT 权限点（`{EntityName}:{query,mutation}`）对 owner doc 高危操作表的覆盖关系：表中所列高危动作（反审核/作废/反结账/处置/红冲等）是否有对应 FNPT 点 / 是否仅归入泛化 `mutation` 点 / 是否存在细粒度缺口
- [x] 评估 `enable-action-auth=false` 默认关闭对"已定义权限点但不生效"的影响范围（owner doc 声明为有意默认，评估是否为残留风险）
- [x] 与 P1-MA3-046（A3.6 全域敏感动作零运行时权限保护）协同裁决：同根因合并为单一 MR3 项 / 分裂为不同修复面 / 升降级
      - Skill: `open-ended-audit-prompt.md`
- [x] 产出权限注解完整性审计报告，分类 P0/P1/P2，更新 arm-index.md（与 A3.6 P1-MA3-046 去重）
  - Skill: `open-ended-audit-prompt.md`

Exit Criteria:

- [x] 注解形态矩阵产出（裸注解 vs 带绑定的全域计数）+ FNPT 覆盖高危操作表裁决
- [x] A6.1 P0/P1/P2 已登记 arm-index.md，且与 P1-MA3-046 去重关系明确（合并/分裂已裁决）

### Phase 2 - finance+mfg+pur+sal 权限深度抽样（A6.2）

Status: completed
Targets: finance / manufacturing / purchase / sales 4 个 S/A 级域 BizModel + 状态机文档；报告 `docs/audits/2026-07-29-1410-arm-ma6-permission-depth-sampling.md`
Skill: `multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: Phase 1（A6.1 注解形态矩阵为抽样入口）

- [x] 职责分离抽样：核实创建人≠审核人在 4 域是否程序级可强制（submit→approve 是否同 userId 拦截）还是仅靠角色配置隐含
- [x] 高危操作程序级守卫抽样：逐项核实 owner doc 高危操作表中 finance（反结账/红冲/批量折旧）+ mfg（工单关闭/强制部分齐套开工）+ pur/sal（反审核/作废）是否有 BizModel 内显式角色/状态守卫，还是仅靠状态机不可逆性"被动"防护
- [x] 状态机迁移执行角色一致性：抽样 4 域 `state-machine.md` 声明的迁移角色 vs 实际 BizModel 方法是否有任何角色断言（owner doc 声明"角色名同源"，核实代码层是否落地）
- [x] 评估"高危操作仅靠状态不可逆被动防护"的风险面（如反审核无独立守卫但 DRAFT 不可逆——是否构成可绕过路径）
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 产出权限深度抽样审计报告，分类 P0/P1/P2，更新 arm-index.md（去重）
  - Skill: `multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 4 域职责分离 + 高危操作守卫抽样矩阵产出（每项裁决：显式守卫 / 被动防护 / 无防护）
- [x] A6.2 P0/P1/P2 已登记 arm-index.md 且去重

### Phase 3 - 数据权限运行验证（A6.3）

Status: completed
Targets: 19 模块 `erp-{module}.data-auth.xml` + BizModel 查询路径 + dashboard BizModel；报告 `docs/audits/2026-07-29-1410-arm-ma6-data-permission-runtime.md`
Skill: `multi-dimensional-audit-prompt.md`

- Item Types: `Proof | Decision`
- Prereqs: M0 锚点（A6.3 审数据权限层，不依赖 Phase 1/2 的注解结论；可与 Phase 1/2 并行）

- [x] 核实 `data-auth.xml` 全空对 owner doc `§数据权限` 声明的运行时影响（"业务员只看自己创建的单据"等行级过滤是否完全不生效）
- [x] 核实零 `IDataAuthChecker`/`IQueryTransformer` 是否有替代机制（如 BizModel 内 QueryTransformer 手拼条件 / IContext 取 userId 过滤）兜底声明能力
- [x] 角色侧行级过滤声明-落地差距矩阵：owner doc 声明的 4 类可见规则（自己创建/部门可见/财务全见/分配给自己）逐项核实代码层落地
- [x] 复核 A2.18 orgId 读路径隔离结论（P1-MA2-093）+ 评估角色侧行级过滤（部门/创建人可见）缺口——聚焦互补维度，不重复 A2.18 的多公司 orgId 维度
- [x] 抽样 dashboard BizModel 直访路径是否绕过（空）数据权限管道（A2.18 已识别 11 个，A6.3 复核确认状态）
      - Skill: `multi-dimensional-audit-prompt.md`
- [x] 产出数据权限审计报告，分类 P0/P1/P2，更新 arm-index.md（与 A2.18 P1-MA2-093 去重）
  - Skill: `multi-dimensional-audit-prompt.md`

Exit Criteria:

- [x] 角色侧行级过滤声明-落地差距矩阵产出 + A2.18 复核结论
- [x] A6.3 P0/P1/P2 已登记 arm-index.md，且与 P1-MA2-093 去重关系明确

## Draft Review Record

- Independent draft review iteration 1: acceptable as-is (task `ses_0537f469bffeAVI7TuJ75QBc3z`) because 全部基线声明经实仓验证（`@BizMutation`~830/`@BizQuery`~320/零权限注解、19 data-auth.xml 全 `<objs/>`、零 IDataAuthChecker、A6.1/A6.2/A6.3 deps 与 roadmap 一致、P1-MA2-093 + P1-MA3-043 交叉引用存在）；A6.1+A6.2+A6.3 合并为单 plan 符合 rule 14（共享 owner doc roles-and-permissions.md）；item typing/skill/exit criteria/Closure Gates 全部合规；anti-slack 零命中。采纳 2 项非阻塞观察：①Non-Goals 补 A6.4 边界声明 + Phase 1 补"风险评估≠启用决策"澄清；②`~825`/`~313` 计数在 EXECUTE 起始重 grep 取权威值。

## Closure Gates

> 本 plan 为纯审计，不改代码。`mvn test` 仅作回归基线确认（见 roadmap 横切关注点 §审计 plan 的 BUILD_VERIFY）。

- [x] A6.1 + A6.2 + A6.3 三份审计报告产出
- [x] arm-index.md 已登记本批次全部 P0/P1/P2，且与 MA1-MA5 + A2.18（P1-MA2-093 等）+ A3.6（P1-MA3-046）既有 P1 交叉去重
- [x] roadmap A6.1-A6.3 状态推进至 `ready`（独立 closure audit 后转 `done`）
- [x] 已运行 `mvn clean install -DskipTests`（154 模块绿）+ `mvn test`（0 failures）作回归基线确认
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态 / 阶段 / 门控 / 日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### xwf 浏览器层审批轴权限验证

- Classification: `watch-only residual`
- Why Not Blocking Closure: plan `2026-07-09-2330-1` 已权威裁决 NOT FEASIBLE（sysUser(0) 兜底层阻断），4 实体审批浏览器层权限链不可达。属平台限制，非权限注解缺陷。
- Successor Required: `no`（触发条件：nop-entropy 修复 sysUser 兜底或提供浏览器层身份映射 API）

## Closure

Status Note: EXECUTE 完成（2026-07-29）。三阶段全 done——A6.1 grep 确认 1138 动作注解 100% 裸注解 + 706 FNPT 仅 query/mutation/inbox（高危操作坍缩进泛化点，FNPT 粒度细化合并入 P1-MA3-046，A6.1 零独立新 P1）；A6.2 产出 P1-MA6-001（4 域职责分离创建人≠审核人程序级零强制）；A6.3 产出 P1-MA6-002（角色侧行级过滤 4 类规则 0 落地，与 P1-MA2-093 orgId 维度互补不重复）。零 P0（owner doc 显式声明权限基线为有意默认 + 单组织种子无活跃数据破坏 + 平台 HTTP 认证默认开启）。MA6 累计 P1=2（目标 MR3）。回归基线：`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS + `mvn test` 0 failures。roadmap A6.1-A6.3 推进至 `ready`（待独立结束审计后转 `done`）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（新会话，非 EXECUTE 执行者，2026-07-29）
- Evidence:
  - **PASS — 三份报告存在且非 hollow**：`docs/audits/2026-07-29-1410-arm-ma6-{permission-annotation-completeness,permission-depth-sampling,data-permission-runtime}.md` 三文件实仓存在（11242/12154/12366 字节，非空骨架）。
  - **PASS — 实仓 grep 计数零漂移**：独立重跑 `rg -c '@BizMutation' module-*/erp-*-{service,dao}/src/main/java` = **825**（与报告 §2 声明一致）；`rg '@BizAuth|@Auth\b|permissionName|@RolesAllowed|@PreAuthorize|@Secured'` = **0**（确认 A6.1 1138 裸注解结论）。
  - **PASS — P1-MA6-001 代码引用精确**：`ErpPurOrderProcessor.java:335-337` `protected void doApprove(ErpPurOrder order, IServiceContext context)` 紧接 `order.setApprovedBy(currentUserId())`，无 `createdBy`/`getCreatedBy` 比对——4 域 SoD 程序级零强制结论成立（finder 非臆造）。
  - **PASS — P1-MA6-002 data-auth.xml 全空核实**：19 个 `erp-*.data-auth.xml`（`src/main/resources/_vfs/erp/<m>/auth/`）**全部** `<objs/>`（grep 计数=1/文件，仅含空标签），确认角色侧行级过滤 4 类规则 0 落地；app 级 `app.data-auth.xml` 仅 `GenFromModules` 聚合空模块。
  - **PASS — arm-index.md 登记完整**：`docs/audits/arm-index.md` §报告清单 +3 行（done）；§P1 详细清单 +2 行（P1-MA6-001/002，目标 MR3，todo）；§MA6 汇总段 + §交叉去重表（P1-MA3-046↔A6.1 FNPT 合并 / P1-MA6-002↔P1-MA2-093 互补不重复）均落地。
  - **PASS — roadmap 推进**：`docs/backlog/audit-remediation-roadmap.md` A6.1/A6.2/A6.3 `todo → ready`（A6.4 由 plan 2026-07-29-1410-2 覆盖，仍 todo）。
  - **PASS — 日志同步**：`docs/logs/2026/07-29.md` 已含本计划条目（任务/核心结论/finding/变更/后续五段齐全）。
  - **PASS — 五点一致性**：Plan Status=completed / 三 Phase Status=completed / 三 Phase Exit Criteria 全 [x] / Closure Gates 全 [x] / Closure 段填充——零状态漂移。
  - **PASS — Deferred honesty**：Deferred 项仅 xwf 浏览器层审批轴（plan 2026-07-09-2330-1 已权威裁决 NOT FEASIBLE，平台限制非权限注解缺陷，successor=no 带 nop-entropy 触发条件）——无范围内活跃缺陷隐藏为 deferred；Follow-up 项（MR3 修复）非降级缺陷。
- Verdict: **APPROVED** — 本 plan 为 audit-only（零生产代码变更），三阶段产出真实、可复核、与实仓一致；P1-MA6-001/002 为真实的声明-落地差距发现（非臆造），零 P0 裁决依据成立（owner doc 显式有意默认 + 单组织种子无活跃数据破坏 + 平台 HTTP 认证默认开启）。MA6 A6.1-A6.3 现可转 `done`。

Follow-up:

- 权限注解缺失 / data-auth 规则缺失 / 高危操作守卫缺失的修复不在此处；由 R3.0 展开机制将本批次 P1 转化为 MR3 具体修复工作项行。
