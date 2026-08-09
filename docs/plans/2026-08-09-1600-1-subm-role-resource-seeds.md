# 2026-08-09-1600-1 静态 role-resource 种子补全（SUBM 菜单组层）

> Plan Status: completed
> Last Reviewed: 2026-08-09
> Source: `docs/backlog/permissions-enforcement-roadmap.md` P1.5a
> Related: P1.4a（`2026-08-09-1400-2`，done）、P1.4b（`2026-08-09-1400-3`，done）、P1.4c（`2026-08-09-0751-1`，done）、P1.4d（`2026-08-09-0751-2`，done）、P1.6（`2026-08-09-1400-1`，done）、P2.1（`2026-08-09-0751-3`，done）；三份 P1.4 计划 Deferred「SUBM 菜单组层 roles 映射」均以本计划为 successor（触发条件 = P1.5a 进入，现已满足）；P1.5b（auth 表 CSV 种子，直接后继，roleId 词表须与本计划一致）
> Audit: required
> Mission: permissions-enforcement
> Work Item: P1.5a

## Current Baseline

**声明层已全部落地（P1.4a-d / P1.6 done）**：9 域 delta `erp-*.action-auth.xml` 已为最高危敏感动作声明独立 per-action FNPT 点，并用 `<resource ... roles="...">` 属性承载静态 role-resource 种子（finance/b2b/mfg/inventory/hr/assets/purchase/sales/contract，详见 `roles-and-permissions.md` §action-level「既有种子证据」表）。

**SUBM 菜单组层 roles 种子完全缺失（本计划缺口）**：实测全部 9 域 delta `erp-*.action-auth.xml`（`module-*/erp-*-web/src/main/resources/_vfs/erp/*/auth/erp-*.action-auth.xml`）的 TOPM/SUBM 资源**无任何 `roles=` 属性**——`grep 'resourceType="SUBM"' ... | grep roles=` 零命中；app 聚合层 `app.action-auth.xml` 的 `erp-sys`/`erp-l10n-cn` TOPM/SUBM 同样无 `roles=`。当前 `roles=` 仅存在于 per-action FNPT 资源（P1.4a-d 添加）。

**菜单过滤 deny-by-default 机制（源码确证）**：
- `SiteCacheDataBuilder.build()` 将每资源 `roles=` 属性并入 `resourceToRoles[resId]`（L149-151）；`authCascadeUp` 资源的角色**仅向上级联**至父资源（`cascadeResourceToRoles` L221-232，child→parent），**不向下传播**。
- `SiteMapProviderImpl.containsRole`（L252-266）按 `resourceToRoles` 判定菜单可见性：资源无 `roles=` 且无 DB role-resource 映射 → 非 admin 用户**菜单被隐藏**（enforcement ON 后）。
- 平台文档（`auth-and-permissions.md` L174-176）明确：**`roles` 属性匹配 roleId，不是角色显示名**——「若 seed 数据/权限配置中的 roleId 字面与角色名不同，以 roleId 为准……否则守卫形同虚设」。

**角色词表现状**：P1.4a-d 既有 FNPT 种子用**中文业务角色名**作 `roles=` 值（财务员/管理员/审核人/B2B 对账员/生产主管/库管员/薪酬审批人/合同审批人/合同专员/HR 专员/资产管理员）。这些值在 P1.5b 创建 `nop_auth_role` 记录时必须作为 **roleId**（非 roleName）落库才能生效；roleId 词表尚未正式冻结（本计划冻结）。

**收敛粒度裁决（P1.3，已 done）**：角色×SUBM Menu（菜单组层）+ 敏感动作 per-action FNPT + 兜底策略。per-action FNPT 部分（敏感动作）由 P1.4a-d 完成；**角色×SUBM 部分（菜单组可见性）= 本计划**。

**蓝图已就绪**：`roles-and-permissions.md` §角色→权限点映射 表（L127-143）定义 15 核心角色 → 可访问 SUBM 域/菜单组的完整蓝图；§第二批扩展域 A 定义 HR/Contract/B2B 敏感角色；§第二批扩展域 B 裁决 CRM/CS/APS/Logistics/DRP 为 **admin-only**（不新建业务角色，不加 SUBM 种子）。

**enforcement 仍 OFF**：`enable-action-auth=false`（P2.1 已预置 config 变量但保持 OFF）；本计划落地不改运行时行为（`roles=` 在 enforcement OFF 时不生效），仅"已就绪可授权"。

## Goals

- 为 14 个域的 TOPM/SUBM 菜单组资源补齐 `roles=` 静态种子（9 核心业务域 + master-data 共享只读 + hr/ct/b2b 敏感扩展 + notify 全用户），外加 `app.action-auth.xml` 聚合层 erp-sys/erp-l10n-cn（平台 admin），使 enforcement 翻转后菜单按角色过滤、授权角色可见、非授权角色隐藏。
- 冻结规范 roleId 词表（15 核心 + 6 敏感扩展 = 21 业务角色 + 平台 admin/nop-admin/user），作为 P1.4 既有种子 / 本计划新增种子 / P1.5b 角色记录三方的唯一一致性基准。
- 按域集群核验权限点 ID 与生成文件 `_erp-*.action-auth.xml` 一致（无新增/漂移权限点，仅 `roles=` 属性增量）。

## Non-Goals

- **不产 auth 表 CSV 种子 / 角色记录 / 用户账号**（归 P1.5b；本计划仅 `roles=` 静态属性种子，等价 `nop_auth_role_resource` 静态种子）。
- **不做 per-entity query/mutation FNPT 的 action 级授权种子**（收敛粒度 = 角色×SUBM + 敏感动作 per-action FNPT；常规 query/mutation 的 action 级 enforcement 归 E1.x；本计划仅菜单组可见性层）。
- **不改生成文件** `_erp-*.action-auth.xml`（真相源，AGENTS.md 规则 7）；种子只在 delta 非生成文件 `erp-*.action-auth.xml` 与聚合层 `app.action-auth.xml`。
- **不为 B 类扩展域（CRM/CS/APS/Logistics/DRP）新建业务角色或加 SUBM 种子**（P1.3 裁决 admin-only；非 admin 受限/不可见为既定行为）。
- **不翻转 enforcement 开关**（归 P2.4/E2.1；本计划落地后 enforcement 仍 OFF）。
- **不改 `roles-and-permissions.md` §角色→权限点映射 蓝图本身**（P1.3 已冻结；本计划仅消费蓝图 + 落地实现注记）。
- **不做数据权限行级规则**（归 E2.x，独立于 action-auth 菜单层）。

## Task Route

- Type: `implementation-only change`（蓝图已由 P1.3 冻结；本计划按蓝图落地 `roles=` 属性增量 + 冻结 roleId 词表 Decision）
- Owner Docs: `docs/design/roles-and-permissions.md`（§角色→权限点映射 蓝图 + §action-level 声明层 + §第二批扩展域 A/B + §运行基线）；`docs/design/app-overview.md` §菜单权威源与定制约定（三层文件链）
- Skill Selection Basis: roadmap P1.5a 指定 `nop-backend-dev`。本计划触及 action-auth.xml delta 定制 + 认证权限机制（菜单过滤/roleId 匹配/deny-by-default），匹配 `nop-backend-dev` 路由的 `02-core-guides/auth-and-permissions.md`。机制 Decision 阶段（Phase 1）读该文档确证；delta 编辑阶段（Phase 2/3）参照既有 P1.4a-d 范式。纯 mechanism/词汇冻结项标 `Skill: none`。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。enforcement 保持 OFF（P2.1 已预置 config 变量），`roles=` 落地无运行时效果。
- 调试期非门控辅助：`nop.auth.site-map.cache-timeout` 调小（如 `1s`）+ `nop.debug=true` 观 `_dump/.../site/{locale}-menu.yaml` 合并结果（非门控调试辅助，机制已由源码确证，不强制）。

## Execution Plan

### Phase 1 - 机制确认与 roleId 词表冻结

Status: completed
Targets: `docs/design/roles-and-permissions.md`（实现注记）；本计划内 Decision 记录
Skill: `nop-backend-dev`（读 `auth-and-permissions.md` 确证机制）

- Item Types: `Decision`
- Prereqs: P1.4a-d + P1.6 done（已满足）

- [x] **Decision**：确认 SUBM 菜单组层 `roles=` 的菜单过滤机制与结果表面边界。确证点（已读 `auth-and-permissions.md` L174-176 + 实测 `SiteMapProviderImpl.java:243-279` 源码确认）：(1) 菜单可见性 deny-by-default（`applyAuthFilter` L248-258 → `containsRole` L266-268：`authRoles` null/empty 直接 return false → DISABLED，无 `roles=` 资源对非 admin 隐藏）；(2) `authCascadeUp` 仅 child→parent 上级联（父 `roles=` 不向下覆盖子 FNPT 权限授权）；(3) 本计划结果表面 = 菜单组可见性层（per-entity query/mutation action 级授权不在此范围，归 E1.x，由 P2.4 dry-run 影响面清单登记）。考虑的替代方案：(a) 仅 SUBM 菜单组层 `roles=`（采纳，与 P1.3 收敛粒度「角色×SUBM」一致）；(b) 同时为 per-entity query/mutation FNPT 补 `roles=`（拒绝：超出收敛粒度，常规 CRUD action 授权归 E1.x，且 per-entity FNPT 为生成文件不可直接改——需 delta 逐实体覆盖，规模与 P1.3 Non-Goal「15×674 逐点矩阵」冲突）。残留风险：菜单可见但 action 未授权的 UX 张力（菜单可见 / 操作被拒）由 E1.x action 级 enforcement 翻转时收敛，不阻塞本计划。
  - Skill: `nop-backend-dev`
- [x] **Decision**：冻结规范 roleId 词表（21 业务角色 + 3 平台角色）。 roleId 集合 = `roles-and-permissions.md` §角色体系 + §第二批扩展域 A 的全部角色名，作为 `roles=` 值 / P1.5b `nop_auth_role.roleId` / 平台 `containsRole` 判定的唯一字面基准。**约束记录**：平台文档明确 `roles` 匹配 roleId 非显示名（`auth-and-permissions.md` L174-176）；本 app 采用「业务角色名即 roleId」策略（与 P1.4a-d 既有 FNPT 种子字面一致，避免返工）。21 业务角色：采购员/销售员/库管员/财务员/资产管理员/项目经理/生产计划员/生产主管/作业员/质检员/质量主管/维护主管/维护人员/审核人/管理员（核心 15）+ HR 专员/薪酬审批人/合同专员/合同审批人/B2B 对账员/B2B 管理员（敏感扩展 6）。平台角色 3 个：`admin`/`nop-admin`（skip-check-for-admin 命名空间，与业务「管理员」分属两套，不可互换——见横切关注点 2）+ `user`（平台内置「普通用户」，`containsRole`（实测 `SiteMapProviderImpl.java:270-272`：`authRoles.contains(AuthCoreConstants.ROLE_USER)` → return true）对资源 roles 含 `user` 时**始终放行**，用于表达「所有登录用户可见」语义）。考虑的替代方案：(a) 业务角色名即 roleId（采纳，与既有种子一致，P1.5b 直接复用）；(b) 改用英文/技术 roleId（拒绝：须同步返工 P1.4a-d 既有 9 域 FNPT 种子，违背最小变更）。残留风险：P1.5b 创建角色记录时 roleId 字面必须与此词表逐字一致（已在 P1.5b Deps 注记 + 本计划 Closure Gates 登记）。
  - Skill: none

Exit Criteria:

> Phase 1 为 doc-only Decision，无代码/配置变更。两 Decision 落地于本计划内（Phase 1 章节），可被 Phase 2/3 直接消费。

- [x] 机制边界与 roleId 词表两项 Decision 落地，含替代方案与残留风险记录
- [x] roleId 词表 21 业务角色 + 3 平台角色（admin/nop-admin/user）逐字明确，与 P1.4a-d 既有 FNPT 种子字面一致（spot-check 9 域 delta `roles=` 值均属词表内）

### Phase 2 - 核心业务域 SUBM roles 种子

Status: completed
Targets: `module-purchase/erp-pur-web/.../erp-pur.action-auth.xml`、`module-sales/.../erp-sal.action-auth.xml`、`module-inventory/.../erp-inv.action-auth.xml`、`module-finance/.../erp-fin.action-auth.xml`、`module-assets/.../erp-ast.action-auth.xml`、`module-projects/.../erp-prj.action-auth.xml`、`module-manufacturing/.../erp-mfg.action-auth.xml`、`module-quality/.../erp-qa.action-auth.xml`、`module-maintenance/.../erp-mnt.action-auth.xml`、`module-master-data/.../erp-md.action-auth.xml`
Skill: `nop-backend-dev`

- Item Types: `Add`
- Prereqs: Phase 1（机制边界 + roleId 词表冻结）

- [x] **Add**：按 `roles-and-permissions.md` §角色→权限点映射 蓝图 L127-143，为 9 核心业务域 delta `erp-*.action-auth.xml` 的 TOPM/SUBM 菜单组资源补 `roles=` 属性。落位策略：域级 TOPM（如 `erp-pur`）挂该域主角色；菜单组 SUBM（如 `erp-pur-sourcing`/`erp-pur-order`）继承域级角色或按蓝图细分；per-entity SUBM（如 `ErpPurOrder-main`）不单独挂 `roles=`（菜单组层覆盖可见性即可）。逐域 × 蓝图表行核验角色集合，**不新增/不改权限点 ID**（仅 `roles=` 属性增量）。按域集群分批：pur→sal→inv→fin→ast→prj→mfg→qa→mnt，每域独立提交 + 本地 `xmllint --noout` well-formed 校验。
  - Skill: `nop-backend-dev`
- [x] **Add**：master-data 共享只读 `roles=` 策略。`erp-md` 域被多角色只读访问（采购员/销售员/库管员/财务员/资产管理员/项目经理/生产计划员/生产主管/作业员/质检员/质量主管/维护主管/维护人员——按蓝图 L129-141 各行「erp-md 只读」枚举）。`erp-md` TOPM 及其菜单组 SUBM（md-material/md-partner/md-org 等）挂聚合角色集合（多角色 CSV）。只读 vs 全权的 action 级区分不在此层（归 E1.x FNPT）；本层仅保证可见性。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 2 交付 10 域（9 业务 + master-data）SUBM 菜单组 `roles=` 种子增量。enforcement OFF，无运行时回归风险；退出仅证 well-formed + 权限点 ID 零漂移。

- [x] 9 核心业务域 + master-data 的 TOPM/SUBM 菜单组资源均挂 `roles=`，角色集合与蓝图 L127-143 对应行逐字一致
- [x] 10 域 delta `erp-*.action-auth.xml` `xmllint --noout` well-formed 通过；无权限点 ID 新增/漂移（`<permissions>` 段未触碰）

### Phase 3 - 敏感扩展域 + 系统/B 类边界

Status: completed
Targets: `module-hr/.../erp-hr.action-auth.xml`、`module-contract/.../erp-ct.action-auth.xml`、`module-b2b/.../erp-b2b.action-auth.xml`、`app-erp-all/.../app.action-auth.xml`（erp-sys / erp-l10n-cn / 聚合层）；B 类域（crm/cs/aps/log/drp）边界确认
Skill: `nop-backend-dev`

- Item Types: `Add | Decision`
- Prereqs: Phase 2（核心域范式确立）

- [x] **Add**：敏感扩展域 SUBM `roles=` 种子——hr（HR 专员/薪酬审批人）、contract（合同专员/合同审批人）、b2b（B2B 对账员/B2B 管理员），按蓝图 §第二批扩展域 A（L153-160）。逐域核验既有 per-action FNPT 种子角色（P1.4c/d）与新增 SUBM 种子角色集合一致（同域角色在 SUBM 与 FNPT 层不矛盾）。
  - Skill: `nop-backend-dev`
- [x] **Decision**：系统菜单 `erp-sys`（sys-user/sys-resource/sys-config/sys-workflow/sys-report/sys-monitor）+ `erp-l10n-cn` 的 `roles=` 策略。蓝图 L143「管理员 → 全部域 TOPM + SUBM + sys-* + erp-l10n-cn」。考虑的替代方案：(a) sys-*/erp-l10n-cn 挂平台角色 `admin`（采纳——系统管理为平台 superuser 职责，与横切关注点 2 双命名空间分离一致：sys-* 经 `skip-check-for-admin` 兜底，业务「管理员」不经此路径）；(b) 挂业务角色「管理员」（拒绝：业务「管理员」不经 skip-check 全放行，sys-* 可见性应绑定平台 admin roleId）。残留风险：测试环境 nop 账号须绑定平台 admin 角色（P2.2a 硬前置，已在本计划 Deps 链下游）。在 `app.action-auth.xml` 聚合层为 erp-sys/erp-l10n-cn TOPM 挂 `roles="admin"`。
  - Skill: none
- [x] **Add**：聚合层 `app.action-auth.xml` 为 erp-sys/erp-l10n-cn TOPM 补 `roles="admin"`（系统管理 + 中国本地化为平台 admin 可见）。
  - Skill: `nop-backend-dev`
- [x] **Decision**：notify 跨域子系统菜单可见性策略。`erp-notify.action-auth.xml:6-8` 明示 `notify-inbox` TOPM 设计为「所有登录用户可见（持 ErpSysNotification:query 权限）」的用户收件箱。考虑的替代方案与裁定：(a) `notify-inbox` TOPM/SUBM 挂平台内置角色 `user`（`containsRole` L271-272 对资源 roles 含 `user` 始终放行）——**采纳**：原生表达「所有登录用户可见」，无需枚举业务角色，新角色加入不破坏；(b) 枚举全部 21 业务角色（拒绝：脆弱，新增角色须同步改种子，且 admin TOPM 与 inbox TOPM 分离设计已被 notify 文件注释 L7 显式区分）；(c) 归 admin-only（拒绝：违背 notify 收件箱面向最终用户的设计）。残留风险：notify 的 action 级（`ErpSysNotification:query/mutation` FNPT）enforcement 须同样绑定 `user` 才能对所有登录用户放行——action 级归 E1.x（本计划仅菜单可见性层 `user` 种子）；过渡期 enforcement OFF 由 admin 兜底覆盖。
  - Skill: none
- [x] **Add**：notify 域 `erp-notify.action-auth.xml` 为 `notify-inbox` TOPM + `ErpSysNotification-inbox` SUBM 补 `roles="user"`（所有登录用户可见，匹配其收件箱设计）。
  - Skill: `nop-backend-dev`
- [x] **Decision**：B 类扩展域（CRM/CS/APS/Logistics/DRP）边界确认。P1.3 裁决测试环境 admin-only，**不加业务角色 SUBM 种子**（非 admin 受限/不可见为既定行为，P2.4 dry-run 影响面清单须登记此覆盖边界）。考虑的替代方案：(a) B 类 admin-only 不加种子（采纳，P1.3 已裁决）；(b) 为 B 类臆造角色（拒绝：投机性工作，违背 owner doc 稳定设计原则）。残留风险：B 类域深化部署出现敏感操作时须升格 A 类补角色（successor，P1.3 已注）。实测 5 域 delta `erp-*.action-auth.xml` TOPM/SUBM `roles=` 命中数均为 0（边界已确认）。
  - Skill: none

Exit Criteria:

- [x] hr/ct/b2b 三敏感扩展域 SUBM `roles=` 种子落地，角色集合与蓝图 §第二批扩展域 A + 既有 FNPT 种子一致
- [x] erp-sys/erp-l10n-cn TOPM 挂 `roles="admin"`；notify-inbox TOPM/SUBM 挂 `roles="user"`；B 类 5 域确认不加种子（边界声明记录于本计划）
- [x] 涉及 delta/聚合层文件 `xmllint --noout` well-formed 通过

### Phase 4 - 聚合验证与 owner doc 对齐

Status: completed
Targets: `app-erp-all/.../app.action-auth.xml`（聚合合并核验）；`docs/design/roles-and-permissions.md`（实现注记）；`docs/logs/2026/08-09.md`
Skill: `nop-backend-dev`

- Item Types: `Proof | Add`
- Prereqs: Phase 2 + Phase 3

- [x] **Proof**：聚合合并正确性核验。`app.action-auth.xml` 经 `x:extends` 合并 19 域 + sys 模块后，TOPM/SUBM `roles=` 属性在合并结果中正确保留（delta `roles=` 不被生成基覆盖）。核验方式：(1) `xmllint --noout app.action-auth.xml` well-formed；(2) 全域 `grep 'resourceType="\(TOPM\|SUBM\)"' ... | grep 'roles='` 统计已挂载菜单组数与蓝图域数对账（14 角色域 + sys/l10n-cn）；(3) 第三项为非硬门控佐证：`nop.debug=true` 启动后检视 `_dump/nop-app/nop/main/site/zh-CN-menu.yaml` 合并树（机制已由源码确证，此项不作阻塞门）。
  - Skill: `nop-backend-dev`
- [x] **Add**：owner doc 实现注记更新——`roles-and-permissions.md` §角色→权限点映射 增「SUBM 菜单组层 roles 种子已落地（P1.5a）」实现注记 + §action-level 补「菜单可见性层（TOPM/SUBM roles）与敏感动作层（per-action FNPT roles）双层已就绪」； roleId 词表冻结注记（指向本计划 Phase 1 D2）。`docs/logs/2026/08-09.md` 增 P1.5a 条目。
  - Skill: none

Exit Criteria:

> Phase 4 交付聚合合并核验证据 + owner doc 对齐。完整 repo build/test 归 Closure Gates。

- [x] 聚合合并核验三方式（well-formed / grep 对账 / dump 佐证）通过，delta `roles=` 在合并结果中保留
- [x] owner doc 实现注记 + 日志条目落地

## Draft Review Record

- Independent draft review iteration 1: needs revision（0 blocker / 1 major / 3 minor）（ses_019e3e73bfferr4wrmL4KGjPEv）。M1 Phase 3 notify Decision 未对 notify-inbox「所有登录用户可见」设计给出表达方式（缺失替代方案 + 表示策略）；m1 域计数「14 角色域」与 Phase 2(10)+Phase 3(3) 不自洽；m2 反 slack 禁词「可选」出现两处；m3 P1.5b 一致性义务未点名可机器核对锚点。全部已修：notify 改 `roles="user"`（平台内置，`containsRole` L271-272 始终放行）+ 替代方案 + 残留风险；roleId 词表增平台 `user`（21 业务 + 3 平台）；notify 列为第 14 域（9 核心+md+hr/ct/b2b+notify）；「可选」改「非门控辅助/佐证」；P1.5b 义务点名两处可核对锚点（Related 行 + auth CSV `roleId` 列）。
- Independent draft review iteration 2: accept（0 blocker / 0 major / 1 minor）（ses_019df9443ffe43nBCfNY4ypdcs）。M1/m1/m3 完全 resolved，m2 实质 resolved；规则 11 文本一致性 / 规则 7 lean exit / 规则 14 单结果表面 / scope 纪律（per-entity FNPT 正确 deferred 至 E1.x）/ 保护区域（仅 delta + 聚合层 + docs）均通过。唯一残留 minor：Phase 4 exit「可选 dump」字面禁词——已修正为「dump 佐证」（本行同步更新）。共识达成，Plan Status → active。

## Closure Gates

> enforcement 保持 OFF，本计划为 `roles=` 属性增量 + doc-only Decision，无业务行为/契约运行时变更。完整 repo 验证在此处跑一次。

- [x] 范围内行为完成（14 角色域 + sys/l10n SUBM `roles=` 种子落地；B 类边界声明）
- [x] 相关文档对齐（`roles-and-permissions.md` 实现注记 + 日志）
- [x] 已运行验证：`mvn clean install -DskipTests`（全 156 reactor 模块绿）+ `xmllint --noout` 全涉及 action-auth.xml + `bash docs/audits/nop-compliance-checker.sh` 对比 `docs/testing/known-good-baselines.md` 零漂移（enforcement 配置/`roles=` 增量不预期触发 checker 反模式基线漂移）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留为未勾选状态作为人工门控占位符
- [x] 结束证据存在于文件中
- [x] **跨计划一致性登记**：本计划冻结的 roleId 词表（21 业务角色 + 平台 admin/nop-admin/user）须在 P1.5b（auth CSV 种子）创建 `nop_auth_role` 记录时逐字复用为 `roleId` 列值。Closure 时在该义务落地两处可机器核对锚点：(1) P1.5b 计划文件的 `Related:` 行引用本计划 + Deps 注记 roleId 词表来源；(2) P1.5b 的 auth CSV 种子 `nop_auth_role` 记录 `roleId` 列值集合 = 本计划 Phase 1 D2 词表（逐字）

## Deferred But Adjudicated

### per-entity query/mutation FNPT 的 action 级授权种子

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 收敛粒度 = 角色×SUBM + 敏感动作 per-action FNPT（P1.3 已冻结）；常规 query/mutation 的 action 级 enforcement 归 E1.x（P2.4 dry-run / E1.1 高危翻转时按影响面清单处理）。本计划仅菜单组可见性层。
- Successor Required: yes（触发条件 = E1.x action 级 enforcement 翻转，常规 query/mutation 授权按域铺开）

### B 类扩展域（CRM/CS/APS/Logistics/DRP）业务角色化

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: P1.3 裁决测试环境 admin-only（非 admin 受限/不可见为既定行为）。
- Successor Required: yes（触发条件 = 该域深化部署 / 出现敏感操作 / 多团队数据隔离需求——届时新建业务角色并补 SUBM 种子，P1.5a 范围扩大）

## Closure

Status Note: 全部 4 Phase + 退出标准执行完成；14 角色域 + sys/l10n-cn/notify 的 `roles=` 静态种子落地（15 文件，仅 `roles=` 属性增量，0 Java 变更）；`mvn clean install -DskipTests` 全 156 模块 BUILD SUCCESS；compliance checker 零漂移（0 Java 变更）；xmllint 全涉及文件 well-formed；owner doc 实现注记 + 日志条目落地；roadmap P1.5a todo→done。结束审计已由独立子代理执行并通过（见 Closure Audit Evidence）。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（mission-driver AUDIT_CLOSURE 步骤，新会话，不复用执行者上下文）
- Evidence: 独立审计 walkthrough（2026-08-09），逐项核验实时仓库：
  - 14 角色域 TOPM/SUBM `roles=` 挂载计数实测：pur=6 / sal=9 / inv=9 / fin=14 / ast=7 / prj=8 / mfg=9 / qa=9 / mnt=7 / md=11 / hr=12 / ct=7 / b2b=5 / notify=1（全部 >0，菜单组层种子已落地）
  - B 类 5 域（crm/cs/aps/log/drp）TOPM/SUBM `roles=` 命中数均为 0（admin-only 边界守住，未臆造角色）
  - 聚合层 `app-erp-all/.../app.action-auth.xml`：`erp-l10n-cn` TOPM（L40）`roles="admin"`、`erp-sys` TOPM（L69）`roles="admin"`（实测确认）
  - notify `erp-notify.action-auth.xml`：`notify-inbox` TOPM（L10）与 `ErpSysNotification-inbox` SUBM（L12）均 `roles="user"`（所有登录用户可见语义落地）
  - roleId 词表一致性：14 域全部 `roles=` 值逐字属 Phase 1 D2 冻结词表（21 业务角色 + 平台 admin/user），0 漂移
  - 15 涉及文件 `xmllint --noout` well-formed 独立复跑：0 failures
  - owner doc `docs/design/roles-and-permissions.md` 实现注记实测存在（L147「SUBM 菜单组层 roles 种子已落地」+ L149「roleId 词表冻结」+ L208「菜单可见性层与敏感动作层双层已就绪」）
  - 日志 `docs/logs/2026/08-09.md` P1.5a 条目实测存在（L3 起）
  - 反松弛自检：Plan Status=completed ↔ 4 Phase Status 全 completed ↔ 全部退出标准 `[x]` ↔ Closure Gates 全 `[x]` ↔ Closure 非占位符（五点一致）；Deferred But Adjudicated 两项均为 out-of-scope improvement 且 Successor Required=yes（非范围内缺陷降级）；无禁词
- 执行者自查证据（保留供溯源，非结束审计本身）：`mvn clean install -DskipTests` BUILD SUCCESS（156 模块，2:03）；`git diff` 仅 `roles=` 增量（0 `<permissions>` 变更）；compliance checker R1-R12 = pre-change baseline（0 Java 变更）

Follow-up:

- per-entity query/mutation FNPT action 级授权（归 E1.x，触发 = action 级 enforcement 翻转）
- B 类域业务角色化（successor，触发 = 域深化部署 / 敏感操作出现）
- roleId 词表跨计划一致性（P1.5b 创建角色记录时逐字复用——非缺陷 follow-up，是跨计划交接义务；词表 = 21 业务角色 + 平台 admin/nop-admin/user）
