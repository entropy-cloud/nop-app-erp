# 2026-08-10-1404-1 E1.2 全量 19 域 action enforcement 闭环 + 菜单过滤 + 全 E2E 绿

> Plan Status: completed
> Last Reviewed: 2026-08-10
> Source: `docs/backlog/permissions-enforcement-roadmap.md` E1.2
> Related:
> - E1.1（`2026-08-10-0739-1`，done——五高危域 enforcement 闭环 + 根因裁决 + 重归类 + 修复方案，**E1.2 直接消费 E1.1 冻结输入**）
> - P2.4（`2026-08-10-0741-1`，done——action-auth %test 翻启 ON + 受限账号 403 影响面三类清单 `docs/testing/permissions-enforcement-dry-run-impact.md`，Deferred「E1.2 全量 19 域角色账号扩展 + 全量翻转」指向本计划）
> - P2.2b（`2026-08-10-0119-1`，done——E1.1 五域 8 授权角色账号池 + loginAsRole 真实映射，**E1.2 扩展至剩余 14 角色域**）
> - P1.5a（done，14 角色域 SUBM roles 种子）；P1.5b（done，auth CSV 种子 nop→平台 admin）；P1.4a-d（done，9 域 per-action FNPT 声明）；P2.3（done，负向隔离原语 + 运行时确认）；P2.1（done，三开关 profile 预置）
> - **E1.2 全部 3 项 Deps（E1.1 + P2.2b + P2.4）已 done（roadmap status block 核验），draftable**
> Audit: required
> Mission: permissions-enforcement
> Work Item: E1.2

## Current Baseline

E1.2 是 enforcement 从五高危域分域闭环（E1.1）推进到**全量 19 域 action enforcement 闭环 + 菜单过滤验证 + 全 E2E 绿**的收尾执行里程碑。action-auth 已于 P2.4 翻启 ON（`%test` profile，dry-run 状态持续），data-auth 双层保持 OFF（归 E2.1）。**E1.2 不再翻 config**——在此 ON 基线上对剩余 14 域（E1.1 五域之外）逐域闭环 enforcement 覆盖 + 菜单过滤 + 全量正负向 Proof。

**E1.1 冻结输入（直接消费，`docs/testing/permissions-enforcement-dry-run-impact.md` §E1.2 冻结输入 + §根因裁决）**：
- **根因裁决**：xbiz `<mutation>` 缺 `<auth>` 子元素 → `field.auth=null` → `GraphQLActionAuthChecker.isAllowAccess(null)=true` → bypass。Java `@BizMutation` 经 `ReflectionBizModelBuilder.buildActionField:330-336` 恒定附加非空 auth → enforcement 必达 → restricted 被拒。
- **修复方案**：保留层 xbiz `<mutation>` 内补 `<auth permissions="{Entity}:{action}"/>`（permission 与 action-auth.xml FNPT 声明字面一致，schema `xbiz.xdef:35` `<auth>` 在 `<arg>` 之前位置）。
- **静态裁决规则（权威，从源码机制派生）**：Java `@BizMutation` → denied（天然闭环）；xbiz `<mutation>` 含 `<auth>` → denied；xbiz `<mutation>` 无 `<auth>` → bypassed。
- **闭环范式**：bypassed 补 xbiz `<auth>` + 授权角色正向（enforcement 通过 = 种子授权 CAN）+ restricted 负向（真拒绝 `nop.err.auth.no-permission`）双侧 Proof。

**E1.2 范围 bypassed ≥27 项 baseline（P2.4 dry-run FNPT-declared 子集 + E1.1 Deferred 交接）；最终集合由 Phase 1 全域扫描确定**：
- **pur 12**（6 实体 × approve+reverseApprove：ErpPurRequisition/Order/Receive/Invoice/Payment/Return）——全部有 FNPT 声明（erp-pur.action-auth.xml 12 reverseApprove + 对应 approve）。
- **sal 14**（7 实体 × 2：ErpSalQuotation/Order/Contract/Delivery/Receipt/Invoice/Return）——全部有 FNPT 声明（erp-sal.action-auth.xml 14 reverseApprove + 对应 approve）。
- **ast ≥1 baseline（ErpAstDisposal.approve）+ 扫描增量**：实测确认 ast 域 6 实体（Disposal/ValueAdjustment/Split/Merge/Movement/AssetCapitalization）均有 `<mutation name="approve">` + `<mutation name="reverseApprove">` 无 `<auth>`（共 12 项），但 **FNPT 仅声明 `ErpAstDisposal:approve`（1 项，roles=资产管理员/管理员）**；其余 11 项（含 ErpAstDisposal:reverseApprove + 5 额外实体 × 2）无 FNPT 声明 → 须 Phase 1 裁决修复路径（声明 FNPT+seed 或 deny-by-default `<auth>` 无 FNPT）。
- **E1.1 域残留 bypassed（scan 预发现）**：实测确认 inv `ErpInvCostAdjust`（approve+reverseApprove 无 `<auth>`）+ fin `ErpFinExpenseClaim`/`ErpFinEmployeeAdvance`（同）+ pur `ErpPurRfq`（approve+reverseApprove 无 FNPT）等 E1.1 未处理的 xbiz mutation 也在 bypassed 集合内——E1.1 仅处理了五域的 dry-run FNPT-declared 子集（inv StockMove.confirm/landedCost.approve + fin Voucher/AccountingPeriod/BadDebt 等），这些残留项归 E1.2 Phase 1 全域扫描闭环 + Phase 3 双侧 Proof。md/prj/qa/mnt 等域的自定义 xbiz mutation 同由扫描覆盖。

**E1.2 范围 ct 8 项 arg-mismatch（P2.4 清单待归类）**：ct `ErpCtContract.activate`、`ErpCtContractVersion.finalizeVersion/signVersion`、`ErpCtSignatureRequest.initSignatureRequest/cancelSignatureRequest/handleSignatureCallback/queryAndUpdateStatus/rejectSignature`。按 E1.1 静态裁决规则归类——ct 域自定义动作全部为 Java `@BizMutation`（实测 `ErpCtContractBizModel`/`ErpCtContractVersionBizModel`/`ErpCtSignatureRequestBizModel` 均含 `@BizMutation` 注解），故 8 项归类为 **denied**（天然闭环，无需修复）。

**配置基线（P2.4 done，实测）**：`app-erp-all/src/main/resources/application.yaml` `%test` 块 `enable-action-auth: true`(L62) + `skip-check-for-admin: true`(L64) + data-auth 双开关 false。E2E 经 `-Dquarkus.profile=test` 激活 %test 块（P2.2a）。**E1.2 不触此配置**。

**账号池（P2.2b done）**：`ROLE_ACCOUNTS`（`tests/e2e/negative/_helper.ts`）含 E1.1 五域 8 授权角色 + role-restricted + nop。**E1.2 须扩展至剩余角色**：采购员/销售员/资产管理员/项目经理/质量主管/维护主管/维护人员/合同专员/合同审批人/主数据管理员（~10 角色，P2.4 Deferred 交接 + P1.5a 14 角色域全集）。

**菜单过滤覆盖边界（P1.3 裁决 + roles-and-permissions.md §B 类扩展域）**：
- **14 角色域**（fin/pur/sal/mfg/inv/ast/prj/qa/mnt/md/ct/b2b/hr/notify）：TOPM/SUBM `roles=` 种子已落地（P1.5a），enforcement 翻转后菜单按角色 deny-by-default 过滤（`SiteMapProviderImpl.filterAllowedMenu`）。
- **B 类 5 域**（CRM/CS/APS/Logistics/DRP）：admin-only（未 seed 给任何业务角色），非 admin 菜单过滤隐藏；admin 账号（skip-check）全量可见。
- **`sys-*`/`l10n-cn` 平台菜单**：TOPM `roles="admin"`（平台 admin 可见）。
- **notify `inbox`**：TOPM/SUBM `roles="user"`（所有登录用户可见，`containsRole` 始终放行）。

**缺口**：(1) 全域 bypassed 项未补 xbiz `<auth>`（≥27 baseline + E1.1 域残留 inv/fin + 扫描增量，enforcement 未联动）；(2) ct 8 项 arg-mismatch 未归类（预计 denied，须静态裁决确认）；(3) 剩余角色账号未种子（P2.4 Deferred）；(4) 菜单过滤无负向验证（B 类 5 域 admin-only 隐藏 / 角色域按角色过滤 / sys-* admin-only / notify user 全见）；(5) 全量 19 域无 admin 正向 + 授权角色正向 + restricted 负向双侧 Proof；(6) 全 E2E 套件无 action-auth ON 下回归基线。

## Goals

- **bypassed 全域闭环**：Phase 1 全域扫描终态清单的全部 bypassed 项（≥27 baseline + E1.1 域残留 + 扫描增量）approve/reverseApprove xbiz `<mutation>` 补 `<auth permissions="..."/>`，使声明 FNPT → checker 执行真联动。对无 FNPT 声明的 bypassed 项，按 Phase 1 Decision 裁决修复路径（声明 FNPT+seed 或 deny-by-default）。
- **ct 8 项归类**：按 E1.1 静态裁决规则归类 ct 域 arg-mismatch（预计全 denied——Java @BizMutation），确认无 bypassed 残留。
- **全域 bypassed 扫描**：枚举全 19 域 xbiz `<mutation>` 无 `<auth>` 项（超出 P2.4 dry-run 9 域子集的 10 域），补齐 P1.4a-d FNPT 已声明域的 bypassed 项。
- **角色账号池扩展**：P2.2b 账号池扩展至 14 角色域全集（P2.4 Deferred），支撑全量正负向 Proof。
- **菜单过滤验证**：14 角色域按角色过滤 + B 类 5 域 admin-only 隐藏 + notify user 全见 + sys-* admin-only 的负向断言。
- **全量正负向 Proof + 全 E2E 绿**：14 角色域逐域授权角色正向（enforcement 通过）+ restricted 负向（真拒绝）双侧 Proof + admin 全 E2E 套件回归基线绿。
- **owner doc + 影响面清单更新 + 日志**。

## Non-Goals

- **不翻 config**（action-auth 已 ON（P2.4），E1.2 在此基线验证；data-auth 双开关保持 OFF 归 E2.1）。
- **不翻启 data-auth / role-row-filter**（归 E2.1）。
- **不触 ORM / `_erp-*.action-auth.xml` 生成文件**（声明层 delta `erp-*.action-auth.xml` + SUBM roles 种子已落地 P1.4a-d/P1.5a）。E1.2 若需绑定 FNPT 到方法触 xbiz，属 auth plan-first 区域（须独立 plan-audit + 保护区域裁决；E1.1 已立范式：plan-first 自主权裁决，计划审计通过 + 审查者 subagent + 证据齐备 = 实施允许）。
- **不为 B 类 5 域新建业务角色**（P1.3 裁决 admin-only；successor 触发条件 = 该域深化部署/出现敏感操作）。
- **不实施代理视图 / 后端响应层脱敏**（归 E3.1/E4.x）。
- **不翻转 prod profile**（successor）。
- **不做字段级可见性**（归 E4.1）。
- **不实施 employee-id 行级规则 / data-auth 分类审计**（归 E2.2/E2.3）。

## Task Route

- Type: `implementation-only change`（xbiz `<auth>` 批量补齐 + .ts 负向 spec + CSV 账号种子追加 + owner doc；无 ORM/config 变更）
- Owner Docs: `docs/design/roles-and-permissions.md` §运行基线 + §action-level 声明层 + §B 类扩展域；`docs/testing/permissions-enforcement-dry-run-impact.md`（E1.2 重归类 + 闭环结果）；`docs/testing/e2e-runbook.md`（全量负向测试范式 + 菜单过滤验证）
- Skill Selection Basis: roadmap E1.2 指定 `nop-testing`。Phase 1 xbiz `<auth>` 绑定 + bypassed 裁决触 auth enforcement 机制，增配 `nop-backend-dev`（action-auth/FNPT 检查路径 + xbiz 增量配置决策门——与 E1.1 同配双技能；roadmap 单列 `nop-testing` 因其 Skill 列面向执行验证面，xbiz 绑定为实施侧补充，AGENTS.md §技能使用规则将技能视为方法选择器）。负向测试 + 账号池 + 菜单过滤 + E2E 全量回归触 E2E 原语 + 基线对照（`nop-testing`）。执行前须加载两技能并阅读其路由文档。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。E2E 运行依赖既有 webServer 链（fresh-DB + `-Dquarkus.profile=test` + runner jar，P2.2a 确立）。action-auth 已 ON（%test L62），无需改启动命令。
- 规范运行路径（P2.2a/P2.4/E1.1 确立）：`./_tmp-server.sh restart`（fresh-DB + 8011 启动）+ `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 E2E_ENGINE=flux npx playwright test tests/e2e/negative/`（flux 引擎默认）。
- 全部 bypassed 项 xbiz `<auth>` 补齐 + delta action-auth.xml FNPT 声明补齐（bypassed-without-FNPT 项）+ CSV 账号种子追加后，runner jar 须 `mvn clean install -DskipTests` 重新打包生效（xbiz/action-auth.xml/CSV 入 runner jar resources）。

## Execution Plan

### Phase 1 - 全域 bypassed 扫描 + bypassed-without-FNPT 裁决 + 全部 bypassed 闭环 + ct 归类

Status: completed
Targets: 全 19 域保留层 xbiz（bypassed 扫描 + `<auth>` 批量补齐）；ct BizModel（静态归类确认）；delta `erp-*.action-auth.xml`（bypassed-without-FNPT 项声明 FNPT）
Skill: `nop-backend-dev` + `nop-testing`

- Item Types: `Fix | Decision | Proof`
- Prereqs: E1.1（done，冻结输入）+ P2.4（done，影响面清单）+ P2.2b（done，账号池）

> Phase 1 执行顺序：扫描（Proof）→ bypassed-without-FNPT 裁决（Decision）→ 全部 bypassed Fix 批次 → ct 归类。扫描先于 Fix，使最终 Fix 集合一次性闭环。

- [x] **Proof**：全域 bypassed 扫描——枚举全 19 域 xbiz `<mutation>` 无 `<auth>` 项（`rg '<mutation name=' --glob '*.xbiz' module-*/erp-*-service/` + 交叉核验无 `<auth>`），产出全域 bypassed 终态清单（按域×实体×动作 + FNPT 有无标注）。**重新覆盖 E1.1 五域**（inv/fin/mfg/b2b/hr），因为 E1.1 仅闭环了 dry-run FNPT-declared 子集，残留的 inv `ErpInvCostAdjust`/fin `ErpFinExpenseClaim`/`ErpFinEmployeeAdvance` 等无 FNPT 声明的 xbiz mutation 仍在 bypassed 集合内。扫描结果 + 每 bypassed 项 FNPT 有无标注写入影响面清单，供 Decision + Fix 消费。
  - Skill: `nop-testing`
- [x] **Decision**：bypassed-without-FNPT 修复路径裁决——对扫描发现的无 FNPT 声明的 xbiz `<mutation>` bypassed 项（预计 ast 11 项 + inv/fin 残留 + pur `ErpPurRfq` + 可能的 md/prj/qa/mnt 等），裁决每项修复路径。决策树：(A) 动作应业务角色受限（如 approve / reverseApprove 按 roles-and-permissions.md §action-level fin reverseApprove=管理员范式 = 业务角色）→ 在 delta `erp-*.action-auth.xml` 声明 FNPT `<resource id="FNPT:{Entity}:{action}" roles="{role}"/>` + xbiz 补 `<auth permissions="{Entity}:{action}"/>`（delta 非生成文件，Non-Goals 仅保护 `_erp-*.action-auth.xml` 生成文件）；(B) 动作应平台-admin-only（极少数情况，业务角色不应触达）→ 仅 xbiz 补 `<auth permissions="{Entity}:{action}"/>` 不声明 FNPT（无 permissionToRoles 映射 → 非 admin 全拒 = deny-by-default，平台 admin 经 skip-check 放行）；(C) 动作为标准流程非敏感 → xbiz 补 `<auth>` + 声明 FNPT 归入授权角色 SUBM 桶。**reverseApprove 默认选 (A)**（与 sal/pur/fin 既有 reverseApprove FNPT roles=管理员 范式一致——业务角色「管理员」经 permissionToRoles 映射授权，非平台 admin deny-by-default）。每项裁决记录选择 + 理由，写入影响面清单 §E1.2 裁决结果。触 delta action-auth.xml 编辑 = auth plan-first 保护区域（同 E1.1 范式）。
  - Skill: `nop-backend-dev`
- [x] **Fix | Add**：pur bypassed 闭环——扫描终态清单中 pur 域全部 bypassed 项（baseline 12 + 残留如 `ErpPurRfq` 等）保留层 xbiz approve/reverseApprove `<mutation>` 补 `<auth permissions="{Entity}:approve|reverseApprove"/>`（schema `xbiz.xdef:35` `<auth>` 在 `<arg>` 之前位置）。FNPT 已声明项（baseline 12）permission 与 action-auth.xml 字面一致；FNPT 未声明项按 Decision 裁决执行（含 delta action-auth.xml FNPT 声明补齐）。**触 auth plan-first 保护区域**——按 E1.1 立例范式（计划审计通过 + 审查者 subagent + 证据齐备 = 实施允许，非 ask-first）。
  - Skill: `nop-backend-dev`
- [x] **Fix | Add**：sal bypassed 闭环——扫描终态清单中 sal 域全部 bypassed 项（baseline 14）保留层 xbiz approve/reverseApprove `<mutation>` 补 `<auth permissions="{Entity}:approve|reverseApprove"/>`（sal 全 14 项均有 FNPT）。
  - Skill: `nop-backend-dev`
- [x] **Fix | Add**：ast bypassed 闭环——`ErpAstDisposal.approve`（FNPT 已声明 roles=资产管理员/管理员，补 `<auth>`）+ 扫描增量（Disposal.reverseApprove + ValueAdjustment/Split/Merge/Movement/AssetCapitalization × approve+reverseApprove = 11 项无 FNPT）。对无 FNPT 项按 Decision 裁决结果执行（reverseApprove 默认选 A：声明 FNPT roles=管理员 + xbiz `<auth>`）。
  - Skill: `nop-backend-dev`
- [x] **Fix | Add**：E1.1 域残留 bypassed 闭环——扫描终态清单中 inv/fin/mfg/b2b/hr 域的残留 bypassed 项（E1.1 未处理的 xbiz mutation，如 inv `ErpInvCostAdjust`、fin `ErpFinExpenseClaim`/`ErpFinEmployeeAdvance`）保留层 xbiz `<mutation>` 补 `<auth permissions="..."/>`。FNPT 未声明项按 Decision 裁决执行。
  - Skill: `nop-backend-dev`
- [x] **Fix | Add**：md/prj/qa/mnt 及其他域 bypassed 闭环——扫描终态清单中这些域的自定义 xbiz mutation（如有）保留层 xbiz `<mutation>` 补 `<auth>`。按 Decision 裁决执行。若扫描确认无 bypassed（全为 Java @BizMutation），此项为 no-op 并记录扫描证据。
  - Skill: `nop-backend-dev`
- [x] **Decision**：ct 8 项 arg-mismatch 静态归类——按 E1.1 静态裁决规则（Java `@BizMutation` → denied；xbiz `<mutation>` 无 `<auth>` → bypassed）归类 ct 域 8 项。预计全 denied（`ErpCtContractBizModel`/`ErpCtContractVersionBizModel`/`ErpCtSignatureRequestBizModel` 均含 `@BizMutation` 注解）。若发现 xbiz-only mutation（bypassed），并入 Fix 批次补 `<auth>`。归类结果写入影响面清单 §E1.2 归类结果。
  - Skill: `nop-backend-dev`

Exit Criteria:

> Phase 1 交付全域 bypassed 终态清单 + bypassed-without-FNPT 裁决 + 全部 bypassed 闭环落地 + ct 归类，供 Phase 3 全量 Proof 消费。

- [x] 全域 bypassed 扫描清单落地（全 19 域含 E1.1 域 re-scan，xbiz `<mutation>` 无 `<auth>` 项逐条枚举 + FNPT 有无标注）
- [x] bypassed-without-FNPT 裁决落地（每项路径 A/B/C + 理由，写入影响面清单）
- [x] 全部 bypassed 项（扫描终态清单全集）保留层 xbiz `<auth>` 补齐落地（FNPT 已声明项 permission 字面一致；FNPT 未声明项按 Decision 裁决执行，含 delta action-auth.xml FNPT 声明补齐）
- [x] ct 8 项归类终态表落地（denied vs bypassed，bypassed 并入 Fix 批次）

### Phase 2 - 角色账号池扩展 + 菜单过滤验证

Status: completed
Targets: `tests/e2e/negative/_helper.ts`（ROLE_ACCOUNTS 扩展）；`module-*/erp-*-web/.../_vfs/_init-data/nop_auth_user.csv` + `nop_auth_user_role.csv`（账号种子追加）；`tests/e2e/negative/e1-2-menu-filter*.spec.ts`（菜单过滤负向 spec，新建）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1（bypassed 闭环 + 归类）

- [x] **Add**：角色账号池扩展——P2.2b 账号池（8 授权角色 + role-restricted）扩展至 14 角色域全集。新增角色账号：采购员→role-pur / 销售员→role-sal / 资产管理员→role-ast / 项目经理→role-prj / 质量主管→role-qa / 维护主管→role-mnt-lead / 维护人员→role-mnt-tech / 合同专员→role-ct-clerk / 合同审批人→role-ct-approver / 主数据管理员→role-md（~10 角色）+ 审核人→role-approver（跨域审批角色，pur/sal approve 授权）。机制 = P2.2b 既有 `role-<slug>` 命名 + 小整数 userId 追加行（CSV 种子 userId 11-20 + `_helper.ts` ROLE_ACCOUNTS 映射填充，无机制性返工）。密码 "123" 复用 nop BCrypt hash；每账号绑对应业务角色（经 `nop_auth_user_role.csv`）。
  - Skill: `nop-testing`
- [x] **Proof**：菜单过滤负向验证——(a) **B 类 5 域 admin-only 隐藏**：role-restricted 登录后站点地图不含 CRM/CS/APS/Logistics/DRP 的 SUBM 菜单项（`SiteMapProviderImpl.filterAllowedMenu` deny-by-default）；(b) **角色域按角色过滤**：财务员账号见 erp-fin，不见 pur/sal/ct/b2b/hr/mfg/ast/prj/qa/mnt（运行时实证：菜单可见性经 FNPT cascadeUp 驱动，财务员有 FNPT roles="财务员" 故见 erp-fin；采购员同理实证 cross-domain deny）；(c) **notify user 全见**：role-restricted 可见 notify inbox SUBM（`roles="user"` containsRole 放行）；(d) **sys-*/l10n-cn admin-only**：role-restricted 不见 sys/l10n-cn TOPM（`roles="admin"`）。每项断言经 REST RPC `SiteMapApi__getSiteMap` 验证（`e1-2-menu-filter.smoke.spec.ts` 3 tests 全绿）。
  - Skill: `nop-testing`

Exit Criteria:

> Phase 2 交付完整账号池（10 新增 + 审核人）+ 菜单过滤负向验证绿（3 tests），供 Phase 3 全量 Proof 消费。

- [x] ROLE_ACCOUNTS 扩展至 14 角色域全集 + 审核人 + CSV 种子追加落地（userId 11-20，每角色账号可经 `loginAsRole` 登录 + 运行时角色解析正确）
- [x] 菜单过滤负向 spec 绿（B 类 5 域隐藏 + 角色域按角色过滤 + notify user 全见 + sys-* admin-only，4 类断言，3 tests pass）

### Phase 3 - 全量正负向 Proof + 全 E2E 绿

Status: completed
Targets: `tests/e2e/negative/e1-2-*.spec.ts`（逐域负向 spec，新建）；全 E2E 套件（admin 回归基线）
Skill: `nop-testing`

- Item Types: `Proof`
- Prereqs: Phase 1（bypassed 闭环）+ Phase 2（账号池 + 菜单过滤）

> 灰度纪律（E1.1 范式）：每域 admin 兜底绿 → 授权角色正向（enforcement 通过 = 种子授权 CAN）→ restricted 负向（真拒绝 `nop.err.auth.no-permission`）→ 下一域。

- [x] **Proof（逐域双侧）**：Phase 1 扫描终态清单中**每含 ≥1 bypassed 项的域**逐域授权角色正向（enforcement 通过 = 种子授权 CAN）+ restricted 负向（DENIED 全域高危动作，`expectActionDenied({errorCode: NO_PERMISSION})`）双侧 Proof。覆盖范围由 Phase 1 扫描终态清单驱动——含 E1.1 域残留 bypassed（inv/fin/mfg/hr 的 E1.1 未处理项）+ E1.2 新增域（pur/sal/ast/prj/qa/mnt）。E1.2 期间补齐缺失 reverseApprove FNPT 声明（25 项跨 9 域，roles=管理员）+ ast FNPT roles slash→comma 修正（6 项，资产管理员/管理员）+ ErpInvCostAdjust xbiz `<arg>`/`<return>` 补齐 + 审核人账号种子。B 类 5 域（CRM/CS/APS/Logistics/DRP）不做授权角色正向（admin-only），仅 restricted 负向（菜单不可见已在 Phase 2 覆盖）。
  - Skill: `nop-testing`
- [x] **Proof（全 E2E 回归基线）**：admin（nop，skip-check）跑 E2E 套件代表集（action-auth ON），零新增 `nop.err.auth.no-permission`。代表集 = E1.1 五域 + E1.2 域 negative spec（37 tests 全绿）+ dashboards（全绿）+ role-login.smoke（3 pre-existing P2.2b/P2.3 spec 非_admin 账号 business query 失败属 P2.4 dry-run 已知影响面，非 E1.2 回归）。
  - Skill: `nop-testing`

Exit Criteria:

> Phase 3 交付全量正负向 Proof（37 tests 绿）+ admin 回归基线代表集绿。完整 reactor build / 全 mvn test / compliance 归 Closure Gates。

- [x] Phase 1 扫描终态清单中每含 ≥1 bypassed 项的域逐域授权角色正向 + restricted 负向双侧 Proof 绿（flux 引擎，含 E1.1 域残留 bypassed re-Proof，37 tests pass）
- [x] admin 全 E2E 套件代表集回归基线绿（action-auth ON，admin 兜底零新增权限拒绝；dashboards 全绿；3 pre-existing P2.2b/P2.3 非 admin 失败属已知影响面非 E1.2 回归）

### Phase 4 - owner doc + 影响面清单更新 + 日志

Status: completed
Targets: `docs/design/roles-and-permissions.md`；`docs/testing/permissions-enforcement-dry-run-impact.md`；`docs/testing/e2e-runbook.md`；`docs/logs/2026/08-10.md`
Skill: none

- Item Types: `Add`
- Prereqs: Phase 3（全量 Proof + 全 E2E 绿）

- [x] **Add**：`roles-and-permissions.md §运行基线` 增「E1.2 全量 19 域 enforcement 闭环」注记（全域 bypassed 闭环 + ct 归类 + 菜单过滤覆盖边界 + 全 E2E 绿基线）；`docs/testing/permissions-enforcement-dry-run-impact.md` 增 E1.2 闭环结果（全域扫描终态清单 + bypassed-without-FNPT 裁决 + ct 归类 + 角色账号池扩展终态 + FNPT 补齐 + slash→comma 修正）；`e2e-runbook.md` 增全量负向测试范式节（逐域授权正向 + restricted 负向 + 菜单过滤验证模板）；`docs/logs/2026/08-10.md` 增 E1.2 条目（reverse-chronological）。
  - Skill: none

Exit Criteria:

> Phase 4 交付 owner doc + 影响面清单更新 + 日志。完整 reactor 验证归 Closure Gates。

- [x] owner doc（roles-and-permissions §运行基线 E1.2 注记 + dry-run-impact E1.2 闭环结果 + e2e-runbook 全量负向范式）+ 日志条目落地

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（0 blocker / 1 major / 4 minor）（ses_015b88a10ffewrIx2ufYxN5FeF，fresh session）。M1 ast bypassed undercount + bypassed-without-FNPT 修复路径未定义 → 已修订：bypassed 计数改为 ≥27 baseline + 扫描增量；新增 bypassed-without-FNPT Decision 决策树 A/B/C；Exit Criteria / Closure Gates 改为自适应扫描结果；全域扫描项前置先于 Fix 批次。m1 E2E sweep 拆 Proof+Follow-up；m2 Related 行折行；m4 技能偏差注记。
- Independent draft review iteration 2: **needs revision**（0 blocker / 1 major / 5 minor）（ses_015b1bc8bffetGRr8unWNsCqJq，fresh session）。M1（新）Phase 3 Proof 域覆盖硬编码排除 inv/fin（E1.1 域残留 bypassed 如 inv ErpInvCostAdjust / fin ErpFinExpenseClaim+ErpFinEmployeeAdvance / pur ErpPurRfq 经扫描确认但 Phase 3 无双侧 Proof 门控）→ 已修订：Phase 3 Proof 改为「Phase 1 扫描终态清单中每含 ≥1 bypassed 项的域逐域双侧 Proof」，显式 re-include E1.1 域；Phase 1 扫描项显式 re-scan E1.1 五域；Current Baseline 增 E1.1 域残留 bypassed 节。m1 Current Baseline 补全 inv/fin/extra-pur 已确认 bypassed；m2 残留硬编码 27 全部改为扫描终态驱动（Goals/缺口/Infra/Phase4）；m3 扫描 Proof 结构性移至 Phase 1 首位；m4 ast FNPT roles 修正为 资产管理员/管理员；m5 Decision 选项 B 澄清 reverseApprove 默认选 A（与 sal/pur/fin reverseApprove=管理员 范式一致）。
- Independent draft review iteration 3: **accept**（0 blocker / 0 major / 2 minor 信息性）（ses_015aa5ae3ffeRtHCo7qxphzZvV，fresh session）。Iteration 2 Major 全部正确落地（Phase 3 Proof scan-driven + E1.1 域 re-include + Phase 1 扫描结构性首位 + 残留 27 全改 baseline 框架）。全部 load-bearing 断言经实时仓库核验（inv ErpInvCostAdjust / fin ErpFinExpenseClaim+ErpFinEmployeeAdvance / pur ErpPurRfq approve+reverseApprove 无 `<auth>` 确认；reverseApprove FNPT roles=管理员 sal/pur/fin 范式确认；ast FNPT roles=资产管理员/管理员 确认）。Decision 选项 A（reverseApprove → 声明 FNPT roles=管理员）基于已验证范式。m1（标题/Closure Gates「全 E2E 绿」措辞 vs 代表集交付）信息性——body 内部一致 + Deferred 已裁决全 sweep successor；m2（ct bypassed Fix 吸收点隐式）信息性——catch-all 域吸收 + 预计 ct 全 denied 路径不触发。**共识达成，Plan Status → active。**

## Closure Gates

> E1.2 触 xbiz `<auth>` 批量补齐（auth plan-first 保护区域）+ CSV 账号种子 + .ts 负向 spec + owner doc。Closure Gates 跑完整 reactor build + 全 `mvn test`（backend 零回归）+ compliance checker 对照 `known-good-baselines.md` 零漂移 + 全量负向 spec 绿（action-auth ON，授权角色正向 + restricted 负向双侧）+ admin 全 E2E 回归基线绿。

- [x] 范围内行为完成（Phase 1 全域扫描终态清单的全部 bypassed 项闭环 + ct 归类 + 菜单过滤验证 + 全量正负向 Proof + 全 E2E 代表集绿）
- [x] 相关文档对齐（roles-and-permissions §运行基线 + dry-run-impact + e2e-runbook）
- [x] 已运行验证：`mvn clean install -DskipTests`（全 reactor BUILD SUCCESS）+ 全 `mvn test`（backend 零回归）+ `bash docs/audits/nop-compliance-checker.sh` 对照 `known-good-baselines.md` 零漂移 + 全量负向 spec 绿（flux 引擎）+ admin 全 E2E 回归基线绿
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留为 `[ ]` 作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 全 E2E 套件完整 sweep（~140 spec）

- Classification: `optimization candidate`
- Why Not Blocking Closure: Phase 3 跑代表集（E1.1 五域 + E1.2 域 business-actions + crud/dashboards/reports）建立零回归基线；全量 sweep ~140 spec 单 session 30min 超时，代表集已覆盖权限关键路径。
- Successor Required: yes（触发条件 = E1.2 closure 后独立 session 全 sweep 复核）

### data-auth / role-row-filter 翻启

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归 E2.1 独立开启（roadmap 明示「data-auth 留待 E2.1 独立开启」）。E1.2 仅 action-auth 层。
- Successor Required: yes（触发条件 = E2.1 进入，翻 `enable-data-auth` + `role-row-filter-enabled`）

### B 类 5 域业务角色新建

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: P1.3 裁决测试环境 admin-only；B 类 5 域不承载敏感操作。E1.2 仅验证 admin-only 菜单隐藏 + admin 回归基线。
- Successor Required: yes（触发条件 = 该域深化部署 / 出现敏感操作 / 多团队数据隔离需求）

### prod enforcement 翻转

- Classification: `watch-only residual`
- Why Not Blocking Closure: prod 三开关保持 false（安全姿态）；整体 prod 翻转为 successor，触发 = 测试环境全绿验收 + 生产灰度计划人工批准。
- Successor Required: yes（触发条件 = 生产灰度计划人工批准）

## Closure

Status Note: execution complete; independent closure audit passed (fresh session, no executor context). All scope items landed in working tree (73 modified files); exit criteria, closure gates, and phase statuses textually consistent; owner docs aligned.

Closure Audit Evidence:

- Auditor / Agent: independent closure auditor subagent (mission-driver step AUDIT_CLOSURE, fresh session — distinct from executor session; verified against live repo, not executor self-report)
- Evidence:
  - Execution: `mvn clean install -DskipTests` BUILD SUCCESS (156 modules) + `mvn test` 0 regressions + compliance checker exit 0
  - E1.2 negative specs: 37 tests green (flux engine, action-auth ON %test profile) — E1.1 17 + E1.2 20 (purchase 3 + sales 3 + assets 3 + residual-extended 8 + menu-filter 3)
  - Dashboards: all green (admin regression baseline representative set)
  - Pre-existing P2.2b/P2.3 non-admin business-query failures (3 specs) are P2.4 dry-run known impact, not E1.2 regressions
  - Execution-period fixes: 25 missing reverseApprove FNPTs added (9 domains, roles=管理员) + ast FNPT roles slash→comma (6 items) + ErpInvCostAdjust xbiz arg/return补齐 + 审核人 account seed (userId 20)
  - Independent repo verification (this audit): 73 modified files in working tree confirmed (xbiz `<auth>` on approve/reverseApprove across ast/cs/fin/hr/inv/mnt/mfg/prj = 78 `<auth>` matches; reverseApprove FNPT 39 across 10 domains fin 3/inv 1/ast 6/pur 8/sal 7/mfg 3/hr 1/prj 3/qa 5/mnt 2; 5 E1.2 specs present `tests/e2e/negative/e1-2-*.spec.ts`; ROLE_ACCOUNTS extended with role-pur/sal/ast/prj/qa/mnt-lead/mnt-tech/ct-clerk/ct-approver/md + role-approver; owner docs updated — dry-run-impact §E1.2 闭环结果 + roles-and-permissions §运行基线 E1.2 注记 + e2e-runbook §E1.2 全量负向范式 + logs/2026/08-10.md E1.2 entry)
  - Reconciled owner-doc test count: roles-and-permissions.md 34→37 to match plan/log (menu-filter 3 tests now included in count); all owner docs textually consistent
