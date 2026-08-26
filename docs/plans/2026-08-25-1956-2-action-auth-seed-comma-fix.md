---
status: active
mission: permissions-enforcement
work-item: R1-E1-1
group: "2026-08-25-1956"
verify: [test, compliance]
---

# 2026-08-25-1956-2-action-auth-seed-comma-fix 七域 action-auth TOPM/SUBM roles 斜杠种子修正 + md 域正向菜单可见性 Proof

## Current Baseline

- **来源发现**：roadmap `docs/backlog/permissions-enforcement-roadmap.md` §Deep Audit Record「E1 — Deep Audit Findings R1」第 1 项（P1，未勾选）。
- **活仓核验（2026-08-25，计数与发现一致）**：7 域 delta `erp-*.action-auth.xml` TOPM/SUBM `roles=` 斜杠分隔共 **35 行**——md×12 / ct×6 / b2b×4 / mfg×4 / qa×3 / hr×3 / mnt×3：
  - `module-master-data/erp-md-web/.../erp/md/auth/erp-md.action-auth.xml:12,16,34,56,...`（如 :12 `roles="采购员/销售员/库管员/财务员/资产管理员/项目经理/生产计划员/生产主管/作业员/质检员/质量主管/维护主管/维护人员"`，13 业务角色）
  - 同型：`module-contract/.../erp-ct.action-auth.xml`、`module-b2b/.../erp-b2b.action-auth.xml`、`module-manufacturing/.../erp-mfg.action-auth.xml`、`module-quality/.../erp-qa.action-auth.xml`、`module-hr/.../erp-hr.action-auth.xml`、`module-maintenance/.../erp-mnt.action-auth.xml`。
- **失效机理（E1.2 已固化的平台契约）**：csv-set 解析器仅识别逗号——斜杠整串解析为单一无效 roleId → 永不匹配 → 菜单组种子语义性失效，enforcement ON（%test）下目标业务角色菜单被 deny-by-default 隐藏。
- **正确格式先例**：`module-assets/erp-ast-web/.../erp-ast.action-auth.xml:47` `roles="资产管理员,管理员"`（逗号多角色）；`erp-fin.action-auth.xml` 单角色字面。E1.2 修 ast FNPT 时已实证该契约。
- **影响面**：erp-md 全域仅斜杠种子、无 FNPT cascade-up 补救 → 13 业务角色全部不可见主数据菜单；`tests/e2e/negative/e1-2-menu-filter.smoke.spec.ts` 仅断言负向（describe「deny-by-default role filtering」），套件对该缺陷不可见。
- **owner doc 漂移**：`roles-and-permissions.md` P1.5a 注记「14 角色域 TOPM/SUBM 已全部挂载 roles= 种子」与运行时实际（斜杠 = 未挂载）漂移，须随修复更正。
- **前置（排位第 2 的原因）**：R2 联动风险——种子修复将 `ErpMd*:query` 族权限授予业务角色，会静默打开 md 原始价格报表路径；本批计划 1（保密读取面 masking，`2026-08-25-1956-1`）须先落地，本计划在其后执行。
- **测试账号可用**：P2.2b 账号池（`tests/e2e/negative/_helper.ts` ROLE_ACCOUNTS，含 采购员→`role-pur`/质检员→`role-inspector` 等小整数 userId 账号）支持正向菜单断言主体。
- Task Route: Type = `implementation-only change`；Owner Docs = `docs/design/roles-and-permissions.md`（P1.5a 注记 + §运行基线）、`docs/testing/e2e-runbook.md`（负向/E2E 规范）。

## Goals

- 7 文件 35 行 `roles=` 全部转为逗号分隔（角色集合内容不变，仅分隔符修正），业务角色在 %test enforcement 下可见其目标菜单。
- `e1-2-menu-filter.smoke.spec.ts` 补 md 域正向菜单可见性断言（授权业务角色登录后 md TOPM/SUBM 可见），与既有负向断言并存——套件对该类缺陷不再不可见。
- owner doc P1.5a 注记更正为运行时事实（逗号分隔生效 + 修复日期）。
- 全负向/E2E 相关套件零回归（种子修复可能改变受限账号可见面——负向断言中若有意外的可见性翻转，逐一归因并按新事实修正断言或登记）。

## Non-Goals

- 不改 FNPT 级 roles 种子与角色定义（仅 TOPM/SUBM `roles=` 分隔符格式修正，角色集合成员不变）。
- 不新增角色、不扩账号池、不动 data-auth 规则。
- 不补其他 12 域（已正确逗号分隔）的任何种子变更。
- 不处理 xbiz 审批邻接 mutation 缺 `<auth>`（R1-E1-2，本批计划 3）。

## Phase 1 — 种子分隔符修正（7 文件 35 行）

Skill: nop-backend-dev
Targets: `module-{master-data,contract,b2b,manufacturing,quality,hr,maintenance}/erp-*-web/src/main/resources/_vfs/erp/*/auth/erp-*.action-auth.xml`

- Item Types: `Fix | Proof`

- [x] Fix: 7 文件 35 处 `roles="A/B/C"` → `roles="A,B,C"`（仅分隔符，角色成员逐一 diff 核对零增删；md 12 / ct 6 / b2b 4 / mfg 4 / qa 3 / hr 3 / mnt 3 计数对账）。（2026-08-26 执行：capture-safe 单斜杠迭代替换（首轮 `/e` 嵌套替换 clobber 捕获组致属性被删，git checkout 还原后重做）；验证 = `git diff -U0` 逐文件 +roles 行计数 12/6/4/4/3/3/3 恰好 35 行变更 + 修正前后 `roles=` 值多重集 diff 全 7 文件 IDENTICAL（零增删零改写，仅分隔符）；jar 内 `_vfs/erp/md/auth/erp-md.action-auth.xml` 复核 12 逗号 0 斜杠）
- [x] Proof: 修正后 grep 斜杠分隔 `roles="[^"]*/` 在 7 文件归零；xmllint well-formed 校验 7 文件通过（本地化验证，Phase 2 依赖）。（7 文件斜杠 grep = 0；`xmllint --noout` ×7 全通过；`mvn clean install -DskipTests` 156 模块 BUILD SUCCESS（runner jar 02:34 含逗号种子）；全 reactor `mvn test` **3847 tests / 0 failures / 0 errors / 1 skipped / 646 报告文件**——对照 2026-08-25 基线 3834/642 差量 +13 tests/+4 文件全额归因前置计划 1（masking，11 个 *Masking* 测试类，已 12/12 ticked）；compliance checker 全规则 actual==baseline 零漂移（R1d=14/R2a=34/R2b=237/R2c=1505/R2d=38/R3=5/R6=2/R10=12/R12a/b/c=70/66/41，R7=1 与 R10 同为预存合理偏离））

## Phase 2 — 正向菜单可见性 Proof + 负向回归 + owner doc 对齐

Skill: nop-testing
Targets: `tests/e2e/negative/e1-2-menu-filter.smoke.spec.ts`、`docs/design/roles-and-permissions.md`、`docs/testing/permissions-enforcement-dry-run-impact.md`（如受影响）

- Item Types: `Proof | Fix`

- [ ] Proof: `e1-2-menu-filter` 新增 md 域正向断言——授权业务角色（如 采购员，username `role-pur`）登录后 `/erp-md` TOPM 与至少一个 SUBM 菜单资源可见；同一角色对未授权域菜单仍不可见（负向并存）；flux 模式运行绿。
- [ ] Proof: 全负向套件（`tests/e2e/negative/`）+ role-login 复跑绿；受限账号菜单可见面变化逐例归因——凡因种子修复而新可见的菜单，核对 roles 集合成员确属设计意图后更新断言，登记于本文件执行记录。
- [ ] Fix: `roles-and-permissions.md` P1.5a 注记更正（斜杠失效机理一句话 + 2026-08 逗号修正 + 正向断言就位）；`docs/logs/` 日志条目。

## Draft Review Record

- dispatch review #review-2026-08-25-201158-mission-driver-2026-08-25-1956-2-action-auth-seed-comma-fix-1-9da5b51f to opencode-reviewer-2026-08-25-201158
- 2026-08-25：iteration 1，共识 accept（活仓复核全对：35 处斜杠计数逐域精确（12/6/4/4/3/3/3）、ast:47 逗号先例、roadmap E1 发现项 1、owner doc P1.5a 漂移注记、前置计划 1 已 active 均属实；跨域角色（采购员/财务员/销售员/库管员）不出现在其余 6 域种子 → 逗号修复不会翻转既有负向断言；review 就地修正 1 处 Minor：账号 username 误写 `role-purchaser` → 实际 `role-pur`（ROLE_ACCOUNTS 逐字）） #review-2026-08-25-201158-mission-driver-2026-08-25-1956-2-action-auth-seed-comma-fix-1-9da5b51f

## Verification

## Closure
