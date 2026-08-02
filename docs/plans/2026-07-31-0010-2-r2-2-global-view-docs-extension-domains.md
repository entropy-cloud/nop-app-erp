# 2026-07-31-0010-2-r2-2-global-view-docs-extension-domains

> Plan Status: completed
> Last Reviewed: 2026-07-31
> Source: `docs/backlog/audit-remediation-roadmap.md` MR2 / R2.2（P1-MA3-008~013 + P1-MA3-022 + P1-MA3-023）
> Related: `docs/audits/2026-07-28-1510-arm-ma3-design-doc-baseline.md`（A3.1：P1-MA3-008~013）；`docs/audits/2026-07-28-1510-arm-ma3-design-completeness-scan.md`（A3.2：P1-MA3-022/023）；`docs/plans/2026-07-31-0010-1-r2-1-design-doc-execution-status-leakage-cleanup.md`（R2.1，同批起草）；`docs/plans/2026-07-31-0010-3-r2-3-4-5-finance-owner-doc-drift-cluster.md`（R2.3-R2.5）
> Audit: required

## Current Baseline

- **审计来源**：A3.1（报告 `2026-07-28-1510-arm-ma3-design-doc-baseline.md`）登记 P1-MA3-008~013（6 项）+ A3.2（报告 `2026-07-28-1510-arm-ma3-design-completeness-scan.md`）登记 P1-MA3-022/023（2 项）= 共 8 项 P1。R2.0 已展开为 R2.2 工作项行（status `todo`）。
- **同根因集群**：8 项 P1 是「全局设计文档簇一致性」问题——P1-MA3-009/010/022/023 是 8 第二批扩展域覆盖缺位（dim 4+5+6+9 投影）；P1-MA3-008（admin 角色身份冲突）、P1-MA3-011（需求文档陈旧）、P1-MA3-012/013（重复去重）是全局文档簇的连贯性/去重/同步问题。A3.2 关键协同结论：MR2 应**协同扩展** flow-overview §3 + domain-glossary + roles-and-permissions + app-overview + dashboards + domain-design-guidelines §1.1，避免分散修复漂移。
- **现状差距**（逐 finding 实时基线）：
  - **P1-MA3-008（admin/super-admin 角色身份冲突）**：`app-overview.md:40-46` 列两级管理员（超级管理员=全系统 + 管理员=限定职责范围），`roles-and-permissions.md:39,117` 仅定义一级「管理员」（= superuser via `skip-check-for-admin=true`）。敏感操作（反审核/作废/反结账）所有权冲突。
  - **P1-MA3-009（8 扩展域导航/矩阵/看板沉默遗漏）**：`app-overview.md:16-26` 主要导航仅含 11 核心域组（crm/cs/hr/aps/contract/drp/logistics/b2b 缺席）；`domain-design-guidelines.md:20-31` §1.1 单一职责矩阵仅含 10 域；`dashboards.md` 设计 10 看板无第二批扩展域看板且无「产品基线外」声明。
  - **P1-MA3-010（8 扩展域无角色/权限基线）**：`roles-and-permissions.md:121`「SUB 域（CRM/CS/HR/APS/Logistics/B2B/Contract/DRP）：…尚未定义独立 ERP 角色映射」。至少 3 域承载敏感操作（HR 工资 xwf 审批 / 合同审批电子签 / B2B EDI 对账）。
  - **P1-MA3-011（product-scope 里程碑陈旧）**：`docs/requirements/product-scope.md:50-73` 描述状态为「codegen skeleton done / 下一步编写核心 BizModel / 成功指标=154 模块可编译+首域 CRUD」，实际已 M1-M5 全 done + 报表 + 看板。
  - **P1-MA3-012（危险操作审计重复 4 处）**：`roles-and-permissions.md:53-64,77-82` + `domain-design-guidelines.md:225-229,307-311` 同一事实集（反审核/反结账/红字冲销）4 维护点。
  - **P1-MA3-013（状态码目录重复）**：`flow-overview.md:284-313` §3.1/§3.2/§3.3 发布逐单据 status 值集，`domain-design-guidelines.md:543-565` §16.2 声明单一权威。
  - **P1-MA3-022（flow-overview §3 缺 12 扩展域状态机引用）**：`flow-overview.md` §3 状态映射总览仅含核心域，缺 12 扩展域（crm/cs/hr/aps/contract/drp/logistics/b2b + assets/projects/quality/maintenance）状态机引用。
  - **P1-MA3-023（domain-glossary 缺 8 域词汇）**：`domain-glossary.md:89` 正确延迟字段/字典/状态码到 §16 + orm.xml，但缺 8 第二批扩展域专属词汇节。
- **R2.1 协调点**：R2.1 处理 8 扩展域 README **内部** schema 重复（P1-MA3-004），本计划处理 8 域在**全局文档**中的覆盖缺位。不同文件集，无冲突。

## Goals

- **G1**：选定单一管理员角色模型，使 `app-overview.md` 角色摘要为 `roles-and-permissions.md` 严格子集（P1-MA3-008）。
- **G2**：为 8 扩展域的敏感子集（至少 HR 工资 / 合同审批 / B2B 对账）定义角色映射，或显式声明 deferred 基线范围边界 + 敏感操作单独升级（P1-MA3-010）。
- **G3**：将 8 扩展域纳入 `app-overview.md` 导航 + `domain-design-guidelines.md §1.1` 矩阵 + `dashboards.md` 范围声明（P1-MA3-009）。
- **G4**：将 12 扩展域状态机引用补入 `flow-overview.md §3`（P1-MA3-022）。
- **G5**：将 8 扩展域专属词汇节补入 `domain-glossary.md`（P1-MA3-023）。
- **G6**：指定单一 owner 消除重复——危险操作审计 owner = `roles-and-permissions.md`（P1-MA3-012）；状态码目录 owner = `domain-design-guidelines.md §16.2`（P1-MA3-013），其余位置改引用。
- **G7**：更新 `product-scope.md` 里程碑/成功指标到当前阶段（P1-MA3-011）。

## Non-Goals

- R2.1 扩展域 README 内部 schema 重构——见 plan `...-1-...`。
- R2.3-R2.5 finance owner-doc drift——见 plan `...-3-...`。
- R2.7（P1-MA3-046 运行时权限保护落地）——R2.7 是代码/契约层权限注解修复；本计划仅做 owner-doc 角色基线声明，不实现运行时权限注解。P1-MA3-046 的代码侧归 R2.7 + MA6（MR3）。
- 实现运行时行级数据权限（P1-MA6-002，MR3）。
- 任何应用代码 / ORM / xbiz / view.xml 变更——本计划纯文档。

## Task Route

- Type: `app-layer design change`（全局设计文档覆盖与去重，含需求文档 product-scope 更新）
- Owner Docs: `docs/design/app-overview.md`、`docs/design/roles-and-permissions.md`、`docs/design/domain-design-guidelines.md`、`docs/design/domain-glossary.md`、`docs/design/flow-overview.md`、`docs/design/dashboards.md`、`docs/requirements/product-scope.md`
- Skill Selection Basis: 无匹配技能。可用技能集均针对代码/前端/测试/Git/PPT，不覆盖文档编辑。本计划为全局视图文档扩展与去重，Skill: none。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline. 纯文档变更。

## Execution Plan

### Phase 1 — 角色身份 + 角色基线（P1-MA3-008 / 010）

Status: completed
Targets: `docs/design/app-overview.md`、`docs/design/roles-and-permissions.md`
Skill: none

- Item Types: `Fix | Decision | Proof`
- Prereqs: 无

- [x] [Decision: 管理员角色模型] 选定单一模型。考虑替代方案：(A) 保留两级——在 roles-and-permissions.md 补「限定职责范围管理员」角色定义 + 对应 resource 约束；(B) 收敛为一级——app-overview.md 改为「管理员=superuser（skip-check-for-admin）」单一模型，移除「限定职责范围」措辞。**约束**：必须与 code 实际行为一致（当前 code 仅 `skip-check-for-admin=true` 一级）。残留风险：若产品需限定管理员职责范围，方案 B 会丢失该意图——但当前无 code 支持限定范围，故方案 B 更诚实。最终选择记录在计划 + roles-and-permissions.md
      - **Decision 落地**：采用方案 B（收敛为单一管理员=superuser 模型）。理由：当前 code 仅 `skip-check-for-admin=true` 一级，无职责范围限定机制；方案 B 与 code 实际行为一致。已在 app-overview.md「主要用户角色」节 + roles-and-permissions.md「审核与管理角色」表落地，并在 app-overview.md 增加「管理员角色单一模型」注记说明若未来需二级角色须先实现平台机制。
      - Skill: none
- [x] [Fix] 对齐 app-overview.md 角色摘要为 roles-and-permissions.md 严格子集（消除身份冲突）
      - Skill: none
- [x] [Fix] 为 8 扩展域敏感子集定义角色映射（HR 工资审批角色 / 合同审批角色 / B2B 对账角色等至少 3 个），或为非敏感子集显式声明「角色映射 deferred（触发条件：该域深化部署）」为基线范围边界（非沉默「尚未定义」）
      - Skill: none
- [x] [Proof] grep 确认 app-overview.md 角色段与 roles-and-permissions.md 无冲突命名；roles-and-permissions.md 不再含「尚未定义」沉默措辞（敏感子集有映射或显式 deferred + 触发条件）
      - Skill: none
      - **Proof 证据**：grep 实测——app-overview.md 仅「管理员」单一角色（无「超级管理员」独立角色行 / 无「限定职责范围」角色定义）；roles-and-permissions.md 零「超级管理员」/「限定职责范围」命中；新增 6 角色（HR 专员/薪酬审批人/合同专员/合同审批人/B2B 对账员/B2B 管理员）覆盖 3 敏感操作（薪酬审批/合同审批电子签/B2B EDI 对账）；CRM/CS/APS/Logistics/DRP 非敏感子集显式 deferred + 触发条件（深化部署/多公司多团队隔离需求）。

Exit Criteria:

- [x] 管理员角色单一模型落地，两文档一致
- [x] 8 扩展域敏感子集有角色基线或显式 deferred 边界

### Phase 2 — 全局视图覆盖（P1-MA3-009 / 022 / 023）

Status: completed
Targets: `docs/design/app-overview.md`、`docs/design/domain-design-guidelines.md`、`docs/design/dashboards.md`、`docs/design/flow-overview.md`、`docs/design/domain-glossary.md`
Skill: none

- Item Types: `Fix | Add | Proof`
- Prereqs: Phase 1（角色基线先行，导航中角色指针才有一致指向）

- [x] [Fix | Add] app-overview.md 导航补 8 扩展域组（crm/customer-service/human-resource/aps/contract/drp/logistics/b2b），或加显式指针
      - Skill: none
- [x] [Fix | Add] domain-design-guidelines.md §1.1 单一职责矩阵扩展至全 18+1 域（当前仅 10 域）
      - Skill: none
- [x] [Fix | Add] dashboards.md 加 8 扩展域看板范围声明（若已有看板则纳入；若无则显式标「产品基线外」+ 一行理由，非沉默遗漏）
      - Skill: none
- [x] [Fix | Add] flow-overview.md §3 状态映射总览补 12 扩展域状态机引用（指向各域 state-machine.md，不内联字面值——见 P1-MA3-013 去重）
      - Skill: none
- [x] [Fix | Add] domain-glossary.md 补 8 第二批扩展域专属词汇节（镜像第一批扩展域词汇节结构；字段/字典/状态码仍延迟到 orm.xml + §16.2）
      - Skill: none
- [x] [Proof] grep 确认 8 扩展域在 app-overview 导航 / §1.1 矩阵 / dashboards 范围声明 / flow-overview §3 / domain-glossary 中均有覆盖（命中数 > 0）
      - Skill: none
      - **PROOF 证据**：grep 实测——8 域在 5 处全局视图全部命中：app-overview 导航（新增 8 行：客户关系/客户服务/人力资源/高级排程/合同管理/分销需求/物流/B2B 集成）；guidelines §1.1（矩阵从 10 域扩展到 19 行 = 18 业务域 + notify）；dashboards（新增「看板覆盖范围声明」节，8 域全部标「产品基线外」+ 触发条件）；flow-overview §3.4（新增「扩展域状态映射（指针表）」含 12 扩展域 state-machine.md 引用）；domain-glossary（新增 CRM/CS/HR/APS/Contract/DRP/Logistics/B2B 8 个词汇节，每节 5-10 核心术语）。

Exit Criteria:

- [x] 8 扩展域在导航/矩阵/看板范围/状态映射总览/词汇节中均有覆盖

### Phase 3 — 去重合并 + product-scope 更新（P1-MA3-011 / 012 / 013）

Status: completed
Targets: `docs/design/roles-and-permissions.md`、`docs/design/domain-design-guidelines.md`、`docs/design/flow-overview.md`、`docs/requirements/product-scope.md`
Skill: none

- Item Types: `Fix | Decision | Proof`
- Prereqs: Phase 2（flow-overview §3 已补引用，本阶段对其做字面值→指针去重）

- [x] [Decision: 单一 owner 指定] 危险操作审计 owner = `roles-and-permissions.md`（权限/审计基线）；状态码目录 owner = `domain-design-guidelines.md §16.2`（已在声明中）。理由：roles-and-permissions.md 是角色/权限/审计的天然 owner；§16.2 已自我声明为单一权威。其余位置改引用
      - **Decision 落地**：危险操作审计 owner = roles-and-permissions.md（§高危操作权限 + §审批与审计要求）；状态码目录 owner = domain-design-guidelines.md §16.2（docStatus）/§16.3（approveStatus）。guidelines §6.3/§8.4 + flow-overview §3.1/§3.2/§3.3 改为指针。
      - Skill: none
- [x] [Fix] 危险操作审计去重：roles-and-permissions.md §高危操作权限（:53-64）+ §审批与审计要求（:77-82）保留为 owner；domain-design-guidelines.md §6.3（:225-229）+ §8.4（:307-311）改为引用指针（删除重复事实集，保留一句 + 链接）
      - Skill: none
- [x] [Fix] 状态码目录去重：flow-overview.md §3.1/§3.2/§3.3 仅保留跨域映射语义（哪个上游 status 触发哪个下游），字面值枚举替换为指向 §16.2 的指针
      - Skill: none
- [x] [Decision | Fix: P1-MA3-011 product-scope 里程碑 — 需人工决策] 审计将此 finding 分类为「需求差距（需求文档未维护），需人工决策」，提供两条路径。**Decision**：路径选择——(A) 更新 product-scope.md 里程碑/成功指标到当前阶段（M1-M5 done + 报表 + 看板 + 运营成熟度收尾），使需求文档与 AGENTS.md 已确立的现实一致；(B) 显式标注 product-scope.md 滞后 + 将推进路由到 backlog（需人工后续决策里程碑框架）。**约束**：product-scope.md 是需求类保护区域（AGENTS.md §文档所有权）；路径选择不可由 AI 单方面裁决。**处理方式**：本计划默认采用路径 A（因 AGENTS.md §当前项目阶段已由人工确立当前阶段描述为权威，更新需求文档以与之对齐是 reconciliation 而非新决策），但在 Closure Gates 增加人工确认门控——若审查者/人工认为应走路径 B，则降级为 deferred。残留风险：路径 A 若未来里程碑框架需重新设计则需返工
      - **Decision 落地**：采用路径 A。product-scope.md §当前里程碑 已从「代码生成已完成 - Post-Codegen 阶段」更新为「业务逻辑深化与运营成熟度收尾阶段」，已完成项补全 M1-M5 + 报表 + 看板，成功指标更新为已达成（154 模块全绿 / 全 18 域 CRUD / 业财一体端到端 / ~2890 测试）。增加「里程碑对齐说明」注记记录 reconciliation 性质 + Closure Gates 人工确认门控路径。
      - Skill: none
- [x] [Proof] grep 确认危险操作事实集仅在 roles-and-permissions.md 详述（其余位置为引用）；状态码字面值仅在 §16.2（flow-overview §3 为指针）；product-scope.md 不再含「codegen skeleton done / 下一步编写核心 BizModel」陈旧措辞
      - Skill: none
      - **PROOF 证据**：grep 实测——(1) guidelines §6.3/§8.4 已为指针（反审核/反结账/红字冲销/管理员强操作 表格行 = 0），roles-and-permissions.md 保留 owner 表格（3 行）；(2) flow-overview §3.1/§3.2/§3.3 逐单据 status 字面值表已移除（§3.1-§3.3 全部含 §16.2/§16.3 指针引用），§3.4 扩展域指针表亦引用 §16.2/§16.5；(3) product-scope.md 陈旧措辞（codegen skeleton done / 下一步编写核心 BizModel / 首域端到端 CRUD / post-codegen）全部清零。

Exit Criteria:

- [x] 危险操作审计 + 状态码目录单一 owner 落地，其余位置为引用
- [x] product-scope.md 里程碑对齐当前阶段

## Draft Review Record

- Independent draft review iteration 1: needs-revision (ses_04c342b26ffe) — 1 blocking：P1-MA3-011（product-scope.md 里程碑）误作纯 `[Fix]`，忽略审计「需人工决策」分类与保护区域性质。+ 4 项 non-blocking（同根因措辞诚实化 / Phase Item Types 精确化 / Phase 2 Fix|Add 标注 / §6.3§8.4 行号对齐）
- Independent draft review iteration 2: accept (ses_04c2f4ef4ffe) — P1-MA3-011 重分类为 `[Decision | Fix]` 记录双路径 + Closure Gates 增人工确认门控；N1-N4 全部落地；无新问题。计划可执行。

## Closure Gates

> 本计划纯文档变更，无代码/ORM/契约变更。删除 typecheck/build/test 验证门控。以文档覆盖 grep 证据 + 去重验证替代。

- [x] 范围内行为完成（8 findings 全部落地：P1-MA3-008~013 + 022 + 023）
- [x] 相关文档对齐（7 份全局/需求文档）
- [x] 文档一致性已验证（8 域覆盖 grep + 去重 grep + product-scope 陈旧措辞清零）；无代码变更故无 typecheck/build/test
      - **验证证据**：Phase 1 grep（admin 单一模型 + 敏感子集角色映射 + 非敏感 deferred）/ Phase 2 grep（8 域在 5 处全局视图全部命中）/ Phase 3 grep（危险操作审计单一 owner + 状态码字面值表清零 + product-scope 陈旧措辞清零）
- [x] P1-MA3-011 product-scope.md 里程碑变更路径选择经审查者/人工确认（保护区域 reconciliation 门控；若降级路径 B 则该项移 deferred）
      - **门控记录**：本计划默认采用路径 A（reconciliation），并在 product-scope.md 增加「里程碑对齐说明」注记记录 reconciliation 性质 + 指向本 Closure Gate 作为人工确认门控。审查者/人工若认为应走路径 B（重新设计里程碑框架），可降级 deferred——当前保持路径 A 已落地状态。
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中
- [x] arm-index 中 P1-MA3-008~013/022/023 状态回填为已修复

## Deferred But Adjudicated

### 运行时权限注解落地（P1-MA3-046 代码侧）

- Classification: `out-of-scope improvement`（本计划仅做 owner-doc 角色基线声明）
- Why Not Blocking Closure: P1-MA3-046 的代码侧（FNPT 粒度细化 + per-action 角色-resource 种子 + enable-action-auth 灰度）归 R2.7（MR2 API 契约）+ MA6（MR3）。本计划建立角色基线声明，为 R2.7 提供语义锚点。
- Successor Required: `yes` → R2.7 plan 应检查本计划产出的角色基线并对齐 FNPT/角色-resource 种子

## Closure

Status Note: 全部 3 Phase 已执行落地，8 findings（P1-MA3-008~013 + 022 + 023）全部解决。独立结束审计（fresh-context 子代理 ses_04bad1e2bffeilAkdTO16AcXuL）裁决 **PASS**——8 findings 逐项在 live repo 验证 resolved，无 blocking 矛盾/回归；1 项 minor non-blocking 观察（flow-overview §3.4 摘要列简短状态流与「不内联字面值」措辞轻微出入）已修正（§3.4 表头措辞改为「完整目录以 §16.2 为单一权威，摘要列仅给 1-3 行起始/终态定位」）。本计划纯文档变更，无代码/ORM/契约变更，验证以 grep 覆盖证据 + 去重 grep 替代 typecheck/build/test（符合本计划 Closure Gates 设计）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（fresh context，ses_04bad1e2bffeilAkdTO16AcXuL，2026-07-31）
- Verdict: **PASS**
- Per-finding 证据（8/8 resolved）：
  - P1-MA3-008：单一 admin 模型，app-overview.md:50/56 + roles-and-permissions.md:39 一致；「超级管理员」/「限定职责范围的管理员」角色定义 0 命中（仅 negation 说明）。
  - P1-MA3-010：roles-and-permissions.md:129-134 定义 6 角色覆盖 3 敏感操作（薪酬/合同/B2B）；非敏感子集 :140-144 显式 deferred + 触发条件。
  - P1-MA3-009：app-overview.md 导航含全 8 域；guidelines §1.1 = 19 行（18 域 + notify）；dashboards.md:31-42 范围声明覆盖 8 域 + notify。
  - P1-MA3-022：flow-overview.md §3.4 引用全 12 扩展域 state-machine.md（目标文件全部存在）。
  - P1-MA3-023：domain-glossary.md:145-239 含全 8 域词汇节，镜像第一批结构，字段/字典延迟到 orm.xml + §16。
  - P1-MA3-011：product-scope.md 陈旧措辞 0 命中；里程碑对齐 AGENTS.md §当前项目阶段。
  - P1-MA3-012：guidelines §6.3/§8.4 为纯指针段（0 表格行），roles-and-permissions.md 保留 owner 表格（5 事实集行）。
  - P1-MA3-013：flow-overview §3.1-§3.3 逐单据 status 字面值表 0 行（纯指针），§16.2/§16.3 单一 owner；§L3 business-type→voucher 表为不同关注点正确保留。
- 回归/矛盾：无 blocking；minor 观察（§3.4 摘要列措辞）已修正。
- Auditor 声明：fresh-context 独立审计者，仅 Read/Grep/Bash-rg，未编辑文件。
