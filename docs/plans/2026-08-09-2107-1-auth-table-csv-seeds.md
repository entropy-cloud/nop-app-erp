# 2026-08-09-2107-1 auth 表 CSV 种子（角色记录 + nop 测试账号 + 平台 admin 绑定）

> Plan Status: completed
> Last Reviewed: 2026-08-09
> Source: `docs/backlog/permissions-enforcement-roadmap.md` P1.5b
> Related: P1.5a（`2026-08-09-1600-1`，done——冻结 roleId 词表 21 业务 + 平台 admin/nop-admin/user，P1.5b 须逐字复用为 `nop_auth_role.ROLE_ID`）；P2.1（`2026-08-09-0751-3`，done——dev/test profile `skip-check-for-admin: true` 预置就绪，使本计划 nop→admin 绑定在 action-auth 翻转后可经 `DefaultActionAuthChecker.isPermitted` 兜底放行）；P2.2a（管理员兜底先行，直接后继——依赖本计划 nop 账号 + admin 绑定）；P2.2b（角色化渐进，直接后继——逐域补角色账号，复用本计划落地的 21 业务角色记录）；roadmap 横切关注点 2（B2 修复项：nop 须显式绑平台 admin 角色非业务「管理员」）
> Audit: required
> Mission: permissions-enforcement
> Work Item: P1.5b

## Current Baseline

P1.5b 是 enforcement 从声明层推进到强制执行层的**账号基建**：把 P1.5a 冻结的 roleId 词表物化为 `nop_auth_role` 记录，并为 E2E 创建一个绑定平台 admin 角色的 nop 测试账号（固定小整数 userId + 平台 admin 角色），使后续 P2.2a（admin 兜底 E2E 基线）可经 `DefaultActionAuthChecker` 的 `skip-check-for-admin` 兜底放行。

**auth 种子现状（实测）**：`app-erp-all/src/main/resources/_vfs/_init-data/` 含 91 个 `erp_*` 业务 CSV + 1 SQL，**0 个 auth 表 CSV**（`ls | grep auth` 零命中）。nop-entropy 侧无 auth `_init-data/` 目录（`nop-auth-dao` 无 `_init-data/`）。**P1.5b 从零创建 auth 种子，无重复风险**（plan `2026-07-08-1234-1` demo seed 明确排除 auth：E2E 认证经 `addDefaultUser()` 自动创建 nop/123）。

**nop 账号现状（实测）**：E2E 唯一账号 `nop/123`（`tests/e2e/auth.ts:6-7`），由 `LoginServiceImpl.addDefaultUser()`（`nop-entropy/.../LoginServiceImpl.java:132-170`）在 `nop_auth_user` 表为空时创建——`user.setUserId(StringHelper.generateUUID())`（随机 UUID，非固定值）+ **无任何角色绑定**。playwright webServer 命令（`playwright.config.ts:18`）以 `-Dnop.auth.login.allow-create-default-user=true -Dnop.orm.init-database-data=true` + `rm -f db/erp.mv.db db/erp.trace.db`（每次 fresh-DB）启动。enforcement 翻转后此账号无授权会大面积 403。

**admin 兜底机制（实测，B2 修复依据）**：`DefaultActionAuthChecker.isPermitted`（`nop-entropy/.../DefaultActionAuthChecker.java:35-41`）：当 `CFG_AUTH_SKIP_CHECK_FOR_ADMIN.get()` 为 true（P2.1 已在 dev/test profile 预置）时，若 `userContext.isUserInRole("admin") || isUserInRole("nop-admin")` → return true（跳过权限检查）。角色集来源：`LoginServiceImpl.buildUserContext`（`LoginServiceImpl.java:292-336`，注意非 `getAuthUser`——后者仅按 loginType 取实体不收集角色）经 `user.getRoles()`（`nop_auth_user_role` → `nop_auth_role` join）收集 roleId → `context.setRoles(roleIds)`。**结论**：nop 须经 `nop_auth_user_role` 绑定 ROLE_ID="admin"，且须存在 `nop_auth_role` 记录 ROLE_ID="admin" 供 join 解析，`isUserInRole("admin")` 方返回 true，skip-check 兜底方生效。平台角色字面常量（`AuthCoreConstants.java:16-19`）：`ROLE_ADMIN="admin"` / `ROLE_USER="user"` / `ROLE_NOP_ADMIN="nop-admin"`。

**密码编码机制（实测，load-bearing）**：默认编码器 = `CompositePasswordEncoder`（bean `nopPasswordEncoder`，`auth-core-defaults.beans.xml:37-40`，SHA256→BCrypt 两段）：encode 时 `salt = generateSalt()`（UUID，`SHA256PasswordEncoder`）→ `encoded1 = sha256(password + salt)` → `final = BCrypt.hashpw(encoded1, BCrypt.gensalt())`（BCrypt 自带随机 salt，**非确定性**，salt 参数被忽略）。verify 时 `passwordMatches(storedSalt, inputPwd, storedPassword)` = `bcrypt.checkpw(sha256(inputPwd, storedSalt), storedPassword)`（`CompositePasswordEncoder.passwordMatches:53-55`）。**关键**：CSV 种子的 PASSWORD/SALT 列无法硬编码固定值——须经平台编码器一次性生成。`nop_auth_user` 无 per-row 算法列（全表单一 `nopPasswordEncoder` bean）。

**USER_ID 与 seq（实测）**：`nop_auth_user.USER_ID`（`nop-auth.orm.xml:38-39`）VARCHAR(50) PK `tagSet="seq"`（字符串 seq：值为 null/空时分配 UUID，显式非空值保留）。**证据**：demo seed `erp_md_employee.csv` 用显式 ID=1,2,3（`tagSet="seq-default"` 数值 seq）经 `DataInitInitializer.loadCsvData`（`nop-entropy/.../DataInitInitializer.java:83-107`，逐行 `dao.saveEntity`，无幂等守护）加载后保留——显式非空值在 seq 加载路径存活。字符串 seq 类比：显式非空 userId 保留（须 Proof 确认；`addDefaultUser` 的 UUID 来自代码 `generateUUID()` 非 seq 覆盖）。

**roleId 词表（P1.5a 冻结，须逐字复用）**：21 业务角色（采购员/销售员/库管员/财务员/资产管理员/项目经理/生产计划员/生产主管/作业员/质检员/质量主管/维护主管/维护人员/审核人/管理员 + HR 专员/薪酬审批人/合同专员/合同审批人/B2B 对账员/B2B 管理员）+ 3 平台角色（`admin`/`nop-admin`/`user`）。「业务角色名即 roleId」——`nop_auth_role.ROLE_ID` 列值 = 上述中文字面（UTF-8），与 P1.5a 已落地的 `roles=` 静态种子（`erp-*.action-auth.xml`）字面一致。

**enforcement 状态**：三开关仍 OFF（P2.1 预置 config 变量但保持 false）；本计划落地不改运行时拦截行为（账号/角色种子在 enforcement OFF 时不影响现有 nop/123 全通行为——CSV 种子使 `addDefaultUser` 跳过，但 CSV 显式 seed nop/123 + admin 绑定等效覆盖）。

**缺口**：(1) 无 auth 表 CSV 种子；(2) nop 账号随机 UUID userId + 无角色（B2 风险）；(3) 21 业务角色 + 平台角色无 DB 记录（P2.2b 角色账号无处绑定）；(4) 密码编码方案未验证（roadmap「先行验证」义务）。

## Goals

- **物化 roleId 词表为 `nop_auth_role` 记录**：24 条（21 业务角色中文字面 ROLE_ID + 平台 `admin`/`nop-admin`/`user`），作为 P1.5a 静态 `roles=` 种子的 DB 侧对应物 + P2.2b 角色账号绑定的前置。
- **创建 nop 测试账号（B2 修复 + E2.2 前置）**：`nop_auth_user` 显式小整数 userId（如 "1"）+ 平台默认编码器生成的 PASSWORD/SALT（密码 "123"，与 E2E fixture 一致）；`nop_auth_user_role` 绑定 nop → 平台 `admin` 角色（非业务「管理员」——双命名空间分离，消解 B2 风险）。
- **密码/salt 编码方案验证（roadmap「先行验证」义务）**：经平台 `CompositePasswordEncoder` 生成有效 PASSWORD/SALT 对，Proof 验证 `passwordMatches` 往返成立。
- **小整数 userId 存活 Proof**：验证显式非空 userId 经 `DataInitInitializer` 种子加载后保留（未被 seq 覆盖为 UUID）。

## Non-Goals

- **不做逐域角色账号**（归 P2.2b「角色化渐进」——按负向测试需要逐域补账号，复用本计划 21 业务角色记录）。
- **不做 admin 兜底 E2E 实测验证**（归 P2.2a——fixture 切 admin + skip-check 生效 + 全 E2E 绿；本计划仅交付账号/角色种子 + 单元级 Proof）。
- **不翻转 enforcement 开关**（归 P2.4/E1.x；本计划落地 enforcement 仍 OFF）。
- **不创建 `nop_auth_role_resource` CSV**（P1.5a 静态 `<resource roles="...">` 种子已在运行时并入 `resourceToRoles`/`permissionToRoles`（`SiteCacheDataBuilder`），DB 行冗余——roadmap §框架/平台复用「无需新增 role-resource 实体表」）。
- **不做 user→employee 关联 / employee-id 行级规则**（归 E2.2；本计划仅交付小整数 userId 约定，为 E2.2 等效方案铺路）。
- **不改 ORM**（USER_ID `seq` tag 保留；显式 userId 存活经 Proof 确认，不触 ask-first ORM 变更）。
- **不改 `playwright.config.ts` 的 `allow-create-default-user=true` flag**（CSV 种子使 `addDefaultUser` 跳过，该 flag 退化为无害 fallback；是否清理归 P2.2a）。
- **不创建 `nop_auth_site` / `nop_auth_resource` 种子**（由 codegen `_erp-*.action-auth.xml` + 平台 site 建模产出，非 P1.5b 结果表面）。

## Task Route

- Type: `implementation-only change`（CSV 种子数据 + Proof；roleId 词表由 P1.5a 冻结，密码机制由平台 bean 落地，本计划无新业务逻辑/契约变更）
- Owner Docs: `docs/design/roles-and-permissions.md`（§角色体系 roleId 词表 + §角色→权限点映射 §action-level 声明层 + §运行基线 B2 双命名空间）；`docs/design/app-overview.md` §种子范式（`_init-data/` CSV + DataInitInitializer，若需补 auth 种子注记）
- Skill Selection Basis: roadmap P1.5b 指定 `nop-testing`。本计划核心交付 = CSV 种子 + 编码方案 Proof（`@NopTestConfig` 注入 `IPasswordEncoder` 往返断言）+ 种子加载 Proof。`nop-testing` 路由 Proof 阶段的基类选择（`JunitBaseTestCase` + `@Inject`）与种子断言方法。无 BizModel 业务代码。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。enforcement 保持 OFF（P2.1 预置 config 变量），auth 种子落地无运行时拦截效果。
- 种子加载 Proof 需 E2E 式启动（fresh-DB + `init-database-data=true`），复用 `playwright.config.ts:18` 既有 webServer 命令模式（本地手动跑，非 CI 门控）。

## Execution Plan

### Phase 1 - 编码方案 / userId 策略 / nop 落位 / 角色集 / role_resource 范围 裁决与探查

Status: completed
Targets: 本计划内 Decision 记录
Skill: `nop-testing`（密码编码 Proof 探查）

- Item Types: `Decision | Proof`
- Prereqs: P1.5a（done）+ P2.1（done）

- [x] **Proof（探查）**：密码编码方案验证——`@NopTestConfig` 测试（`JunitBaseTestCase`）`@Inject IPasswordEncoder`（= `CompositePasswordEncoder` bean），对密码 "123"：`salt = passwordEncoder.generateSalt()`、`hash = passwordEncoder.encodePassword(salt, "123")`、断言 `passwordEncoder.passwordMatches(salt, "123", hash) == true`（往返）。产出可提交的 SALT + PASSWORD 对（Phase 3 写入 CSV）。确认两段编码（SHA256→BCrypt）往返成立。
  - Skill: `nop-testing`
  - **执行注记**：Proof 落地为 `TestAuthSeedEncodingProof#testPasswordEncoderRoundtripForSeedPassword`（纯逻辑测试，手动构造 `CompositePasswordEncoder` 装配与平台 `nopPasswordEncoder` bean 逐字一致，避免 app-erp-all 测试容器 ALL_LAZY 模式下 schema init 不先于 DB 访问 bean 的 pre-existing 环境故障）。产出的 SALT+PASSWORD 对：`SALT=26dce419976e4e7f95f7a9dcb82e5bc4` / `PASSWORD=$2a$10$74DaI9b3RwzmmA3xpb6ZN.WTzl2YjNf7cLVTTaW1TmaW1rGCdn892`（已固化到 nop_auth_user.csv）
- [x] **Proof（探查）**：小整数 userId 存活验证——经 E2E 式启动（fresh-DB + `init-database-data=true`）加载含显式 `USER_ID=1` 的 nop_auth_user CSV 后，查询 `nop_auth_user` 确认 userId 保留为 "1"（未被 `seq` 覆盖为 UUID）。证据基线：demo seed 显式 ID 存活 `seq-default`（`erp_md_employee.csv` ID=1,2,3）；字符串 seq 类比存活，本 Proof 确证。若**未**存活（显式值被覆盖）→ 升级为 ask-first（ORM seq tag 处置），暂停本计划触及 user CSV 行。
  - Skill: `nop-testing`
  - **执行注记**：Proof 落地为 `TestAuthSeedLoadingProof#testUserSeedSmallIntegerIdSurvivesAndActive`——`BaseTestCase` + 手动 `CoreInitialization.initialize()` + `init-database-data=true` 加载 `/_init-data/` 后查询 `nop_auth_user` 确认 `userId="1"` 保留 + `STATUS=1`（ACTIVE）。seq null-guard 三处代码站点佐证（`OrmEntityIdGenerator.genSeq` / `EntityPersisterImpl.checkColumnValueWhenSave` / `CrudBizModel` clone null-out）。**存活确认**，无需 ask-first 升级
- [x] **Decision**：密码编码方案。**采纳**：平台默认 `CompositePasswordEncoder`（SHA256→BCrypt）一次性生成 PASSWORD/SALT 对，提交至 CSV。考虑的替代方案：(a) 自定义种子初始化 bean 在加载时编码——**拒绝**：新增 Java 组件侵入性高于 CSV，roadmap 明确「CSV 种子」交付形态；(b) 改用纯 SHA256 简化方案——**拒绝**：平台无 per-row 算法列，默认 bean 为 composite，纯 SHA256 与 verify 路径不匹配。残留风险：BCrypt 非确定性使 hash 对肉眼不透明 → 经 `passwordMatches` 往返 Proof 消解。
  - Skill: none
- [x] **Decision**：userId 策略。**采纳**：显式小整数字符串 userId（nop = "1"）。考虑的替代方案：(a) 让 seq 分配 UUID——**拒绝**：违背 roadmap「测试账号须显式指定小整数 userId（支撑 E2.2 等效方案）」+ 破坏 E2.2 `user.id==employee.id` 相等比较；(b) ORM 移除 seq tag——**拒绝**：ask-first 保护区域，且显式值存活（Phase 1 Proof）后无必要。残留风险：无（Proof 确认后）。
  - Skill: none
- [x] **Decision**：nop 账号落位。**采纳**：nop **纳入** CSV 种子（固定 userId + admin 角色 + 编码密码），使 `addDefaultUser()` 因表非空跳过（CSV 完整 seed nop）。`playwright.config.ts` 的 `allow-create-default-user=true` 退化为无害 fallback（保留，不清理——归 P2.2a 裁决）。考虑的替代方案：(a) nop 不入 CSV，由 `addDefaultUser` 创建——**拒绝**：随机 UUID userId + 无角色 → B2 风险复发 + E2.2 前置失败；(b) 入 CSV 且同时关闭 `allow-create-default-user` flag——**部分采纳**：flag 保留为 fallback（harmless），不强制关闭。残留风险：vestigial flag 已文档化。
  - Skill: none
- [x] **Decision**：角色记录集。**采纳**：创建全部 24 条 `nop_auth_role`（21 业务中文字面 + `admin`/`nop-admin`/`user`）。考虑的替代方案：(a) 仅创建 admin——**拒绝**：P2.2b 逐域角色账号需业务角色记录已存在方可绑定，且 P1.5a 词表须完整物化；(b) 全部 24——采纳。残留风险：无。
  - Skill: none
- [x] **Decision**：`nop_auth_role_resource` 范围。**采纳**：**不创建**。P1.5a 静态 `<resource roles="...">` 种子（`erp-*.action-auth.xml`）在运行时经 `SiteCacheDataBuilder.build()` 并入 `resourceToRoles`/`permissionToRoles`，DB 行冗余。考虑的替代方案：(a) 物化 DB 行——**拒绝**：与静态种子重复，roadmap §框架/平台复用「无需新增 role-resource 实体表」；(b) 跳过——采纳。残留风险：无（静态种子为运行时权威）。
  - Skill: none

Exit Criteria:

> Phase 1 为 Decision + 探查 Proof，无代码/配置变更。两项 Proof 确认编码往返 + userId 存活；五项 Decision 落地于本计划内，可被 Phase 2/3 直接消费。

- [x] 密码编码往返 Proof 通过（`passwordMatches(salt,"123",hash)==true`），产出可提交 SALT+PASSWORD 对
- [x] 小整数 userId 存活 Proof 通过（fresh-DB 加载后 userId="1" 保留）；或确认未存活并按 ask-first 升级暂停
- [x] 五项 Decision（编码方案 / userId 策略 / nop 落位 / 角色集 / role_resource 范围）落地，含替代方案与残留风险

### Phase 2 - nop_auth_role 种子（24 条角色记录）

Status: completed
Targets: `app-erp-all/src/main/resources/_vfs/_init-data/nop_auth_role.csv`
Skill: `nop-testing`

- Item Types: `Add`
- Prereqs: Phase 1（角色集 Decision）

- [x] **Add**：创建 `nop_auth_role.csv`（CSV header = 实体 column code，与 `nop-auth.orm.xml` 一致：`ROLE_ID,ROLE_NAME,CHILD_ROLE_IDS,IS_PRIMARY,DEL_FLAG,VERSION,CREATED_BY,CREATE_TIME,UPDATED_BY,UPDATE_TIME,REMARK`）。CSV **省略**框架管理列（`CREATED_BY/CREATE_TIME/UPDATED_BY/UPDATE_TIME/VERSION` 等），由 `DataInitInitializer` 自动填充（与既有 91 业务 CSV 一致——这些 CSV 仅含业务列）。24 条记录，ROLE_ID 逐字 = P1.5a 词表：21 业务角色（采购员/销售员/库管员/财务员/资产管理员/项目经理/生产计划员/生产主管/作业员/质检员/质量主管/维护主管/维护人员/审核人/管理员 + HR 专员/薪酬审批人/合同专员/合同审批人/B2B 对账员/B2B 管理员）+ 平台 `admin`/`nop-admin`/`user`。ROLE_NAME 取可读显示名（业务角色名即显示名；平台角色 ROLE_NAME 描述性如「平台管理员」）。`DEL_FLAG=0`（active）、`IS_PRIMARY=0`。无 CHILD_ROLE_IDS（本期不建组合角色）。
  - Skill: `nop-testing`

Exit Criteria:

> Phase 2 交付 24 条角色记录 CSV。enforcement OFF，无运行时回归；退出仅证 well-formed + roleId 词表逐字一致。

- [x] `nop_auth_role.csv` 创建，24 条 ROLE_ID 与 P1.5a 词表（21 业务 + admin/nop-admin/user）逐字一致（机器可核对：CSV ROLE_ID 列集合 == 词表）
- [x] CSV well-formed（列名匹配 `nop-auth.orm.xml` column code；`xmllint` 不适用 CSV，用行/列计数 + header 校验）

### Phase 3 - nop 用户种子 + admin 绑定 + 编码密码

Status: completed
Targets: `app-erp-all/src/main/resources/_vfs/_init-data/nop_auth_user.csv`、`.../nop_auth_user_role.csv`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1（编码方案 + userId 策略 + nop 落位 Decision）+ Phase 2（admin 角色记录已存在供 FK join）

- [x] **Add**：创建 `nop_auth_user.csv`（header = `nop-auth.orm.xml` user column code：`USER_ID,USER_NAME,PASSWORD,SALT,NICK_NAME,DEPT_ID,OPEN_ID,REL_DEPT_ID,GENDER,AVATAR,EMAIL,EMAIL_VERIFIED,PHONE,PHONE_VERIFIED,BIRTHDAY,USER_TYPE,STATUS,ID_TYPE,ID_NBR,EXPIRE_AT,PWD_UPDATE_TIME,CHANGE_PWD_AT_LOGIN,REAL_NAME,MANAGER_ID,WORK_NO,POSITION_ID,TELEPHONE,CLIENT_ID,DEL_FLAG,VERSION,TENANT_ID,CREATED_BY,CREATE_TIME,UPDATED_BY,UPDATE_TIME,REMARK,EXT_FLAGS`；CSV 省略框架管理列）。nop 单行：`USER_ID=1`（显式小整数，Phase 1 Proof 确认存活）、`USER_NAME=nop`、`PASSWORD`+`SALT`=Phase 1 编码 Proof 产出的 "123" 编码对、`OPEN_ID=0`、`NICK_NAME=Nopper`、`GENDER=1`（USER_GENDER_DEFAULT=1，dict auth/gender 男=1）、`USER_TYPE=1`（USER_TYPE_DEFAULT=1，dict auth/user-type 普通用户=1）、`STATUS=1`（**USER_STATUS_ACTIVE=1**，dict auth/user-status 正常=1/停用=0/废弃=2——`isAllowLogin`（`LoginServiceImpl.java:281-283`）拒绝 `status != ACTIVE`，故 STATUS 必须为 1 非 0）、`DEL_FLAG=0`、`TENANT_ID=0`、其余可空。参照 `addDefaultUser()`（`LoginServiceImpl.java:140-156`）的字段取值，保证语义等价但 userId 固定 + 角色绑定。
  - Skill: `nop-testing`
- [x] **Add**：创建 `nop_auth_user_role.csv`（header：`USER_ID,ROLE_ID,VERSION,CREATED_BY,CREATE_TIME,UPDATED_BY,UPDATE_TIME,REMARK`）。nop → 平台 admin 绑定单行：`USER_ID=1,ROLE_ID=admin`（B2 修复——绑定平台 admin 角色非业务「管理员」，双命名空间分离）。
  - Skill: `nop-testing`
- [x] **Proof**：种子往返断言——`@NopTestConfig` 测试加载 nop_auth_user CSV 值后，`passwordEncoder.passwordMatches(salt, "123", password) == true` 确认 CSV 中的编码密码与登录 verify 路径一致。
  - Skill: `nop-testing`

Exit Criteria:

> Phase 3 交付 nop 用户 + admin 绑定 CSV + 编码密码往返 Proof。完整 E2E 登录实测归 P2.2a。

- [x] `nop_auth_user.csv` 单行 nop（userId=1 小整数，password/salt 为 Phase 1 编码产出）；`nop_auth_user_role.csv` 单行 USER_ID=1,ROLE_ID=admin
- [x] 编码密码往返 Proof 通过（`passwordMatches` 对 CSV 值成立）

### Phase 4 - 种子加载 Proof + owner doc 对齐 + 日志

Status: completed
Targets: `docs/design/roles-and-permissions.md`（实现注记）；`docs/logs/2026/08-09.md`
Skill: `nop-testing`

- Item Types: `Proof | Add`
- Prereqs: Phase 2 + Phase 3

- [x] **Proof**：种子加载端到端验证——经 E2E 式启动（fresh-DB `rm -f db/*.mv.db` + `init-database-data=true`，复用 `playwright.config.ts:18` 命令模式本地跑）后，查询确认：(1) `nop_auth_role` 24 行（21 业务 + 3 平台）；(2) `nop_auth_user` nop 行 userId="1"（小整数存活，Phase 1 Proof 复核）**且 `STATUS=1`（ACTIVE——`isAllowLogin` 拒绝非 ACTIVE，disabled 种子会使 P2.2a 登录失败）**；(3) `nop_auth_user_role` 绑定 (1, admin) 存在；(4) 角色集经 join 解析后 `isUserInRole("admin")` 成立（skip-check 兜底前置就绪）。记录为 Closure 证据。
  - Skill: `nop-testing`
- [x] **Add**：owner doc 实现注记——`roles-and-permissions.md` §角色体系增「P1.5b 已物化 24 条 nop_auth_role 记录 + nop 测试账号（userId=1，绑平台 admin 角色，B2 修复）」注记，并**核验既有语义分离注记（§角色体系「管理员」双命名空间注记，P1.3 已落地于 L41-45）与实际种子一致**（nop 绑平台 `admin` 角色非业务「管理员」——roadmap 横切关注点 2 要求 P1.5b 验证种子绑定一致）；§运行基线增「auth 种子就绪（角色 + nop/admin 绑定），enforcement 仍 OFF，翻转归 P2.4」。`docs/logs/2026/08-09.md` 增 P1.5b 条目（reverse-chronological）。
  - Skill: none

Exit Criteria:

> Phase 4 交付种子加载 Proof + owner doc 对齐。完整 repo build/test 归 Closure Gates。

- [x] 种子加载 Proof 四项（24 角色 / nop userId=1 且 STATUS=1 ACTIVE / admin 绑定 / isUserInRole）实测通过
- [x] owner doc 实现注记 + 日志条目落地

## Draft Review Record

- Independent draft review iteration 1: needs revision（**1 blocker** / 0 major / 3 minor）（ses_019592499ffe0Y7dxQrfoiR9NB）。**B1**（blocker，覆盖面/可执行性）Phase 3 `STATUS=0` 创建的是**停用**用户（dict auth/user-status 停用=0/正常=1，`isAllowLogin`（`LoginServiceImpl.java:281-283`）拒绝 `status != USER_STATUS_ACTIVE`），nop 无法登录，且本计划 Non-Goal 把登录实测推给 P2.2a，缺陷会静默漏过 P1.5b closure 在 P2.2a 才暴露。修正：`STATUS=1`（ACTIVE）+ Phase 4 Proof 增 `STATUS=1` 子检查（P1.5b closure 自捕获）。m1（可执行性）`GENDER=0/USER_TYPE=0` 与「语义等价 addDefaultUser」主张矛盾（`USER_GENDER_DEFAULT=1`/`USER_TYPE_DEFAULT=1`，dict auth/gender 男=1、auth/user-type 普通用户=1）→ 改 1/1。m2（可执行性）Current Baseline 误称 `getAuthUser`（仅取实体）收集角色，实为 `buildUserContext`（`LoginServiceImpl.java:292-336`）→ 订正方法名。m3（规范性）Phase 2「CSV 可省略或显式」框架管理列留二可选择 → 收敛为「CSV 省略，DataInitInitializer 自动填充」（与既有 91 业务 CSV 一致）。全部已修。live-repo 事实核验通过（0 auth CSV 起步 / 列名逐字匹配 / skip-check 逻辑 / B2 role 记录必需 / 密码非确定性 / roleId 词表匹配 P1.5a / deps 全 done / Proof 先于 Add 编排）。
- Independent draft review iteration 2: accept（0 blocker / 0 major / 1 minor，信息性）（ses_01953169effewFVoa5ByFZPCV5）。B1/m1/m2/m3 全部 resolved（STATUS=1 + Phase 4 STATUS 子检查 / GENDER=1 USER_TYPE=1 / buildUserContext 订正 / 框架列收敛为省略），逐项经 dict（auth/user-status 正常=1、auth/gender 男=1、auth/user-type 普通用户=1）+ `isAllowLogin`（`LoginServiceImpl.java:281-283`）+ `AuthApiConstants.java` 常量独立复核。回归扫荡清洁：anti-slack 零禁词、status 仍 draft 未越级、item-type 全标、lean exit、单结果表面、跨计划一致性机器可核、Draft Review Record iteration 1 诚实记录、P1.5b scope 对齐、Proof 先于 Add 编排含 ask-first 升级回退。唯一信息性 minor：Phase 4 doc-update 未显式点名 roadmap 横切关注点 2 要求的「语义分离注记核验」——已补（P1.3 既落地的 §角色体系双命名空间注记 L41-45 与实际种子绑定的核验）。共识达成，Plan Status → active。

## Closure Gates

> 本计划新增 3 个 auth CSV 种子文件（无 Java 变更、无 ORM 变更、无 enforcement 翻转）。三开关仍 OFF，auth 种子在 enforcement OFF 时不影响现有 nop/123 全通行为。Closure Gates 跑完整 build + compliance checker 对照零漂移 + 种子加载 Proof。

- [x] 范围内行为完成（24 角色 CSV + nop 用户 CSV + admin 绑定 CSV + 编码方案 Proof + userId 存活 Proof）
- [x] 相关文档对齐（`roles-and-permissions.md` 实现注记 + 日志）
- [x] 已运行验证：`mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh` 对照 `docs/testing/known-good-baselines.md` 零漂移（CSV 种子不预期触发 checker 反模式基线漂移）+ 种子加载 Proof（fresh-DB 加载四项核对）+ 编码密码往返 Proof
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留为未勾选状态作为人工门控占位符
- [x] 结束证据存在于文件中
- [x] **跨计划一致性登记（P1.5a 交接义务）**：本计划 `nop_auth_role.ROLE_ID` 列值集合 == P1.5a（`2026-08-09-1600-1` Phase 1 D2）冻结词表（21 业务角色 + admin/nop-admin/user）逐字。Closure 时核对两处可机器核对锚点：(1) 本计划 Related 行引用 P1.5a + Phase 2 项点名 roleId 来源；(2) `nop_auth_role.csv` ROLE_ID 列值集合 == P1.5a 词表

## Deferred But Adjudicated

### 逐域角色账号（21 业务角色对应的测试用户）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归 P2.2b「角色化渐进」——按负向测试需要逐域补账号，复用本计划落地的 21 业务角色记录（P2.2b 仅新增 user + user_role 行，角色记录已就绪）。
- Successor Required: yes（触发条件 = P2.2b 进入，按域补账号）

### admin 兜底 E2E 实测验证

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅交付账号/角色种子 + 单元级 Proof（编码往返 + 加载核对）；E2E 全套件 admin 基线绿归 P2.2a（E1 硬前置）。
- Successor Required: yes（触发条件 = P2.2a 进入）

### playwright allow-create-default-user flag 清理

- Classification: `watch-only residual`
- Why Not Blocking Closure: CSV 种子使 `addDefaultUser()` 因表非空跳过，该 flag 退化为无害 fallback（不创建重复 nop）。是否清理归 P2.2a 裁决。
- Successor Required: yes（触发条件 = P2.2a 进入时裁决 flag 去留）

## Closure

Status Note: executed（四 Phase 全落地；独立结束审计通过——见下方证据）

Closure Audit Evidence:

- **范围内交付物**：3 个 auth CSV 种子（`app-erp-all/src/main/resources/_vfs/_init-data/nop_auth_role.csv` 24 条 + `nop_auth_user.csv` 1 行 nop userId=1 + `nop_auth_user_role.csv` 1 行 (1,admin)）+ 2 个 Proof 测试类（`TestAuthSeedEncodingProof` 编码往返 2 tests + `TestAuthSeedLoadingProof` 种子加载 3 tests）。
- **编码方案 Proof**：`TestAuthSeedEncodingProof#testPasswordEncoderRoundtripForSeedPassword` 绿——`CompositePasswordEncoder`（SHA256→BCrypt，装配与平台 `nopPasswordEncoder` bean 逐字一致）对 "123" 生成 SALT+PASSWORD 对，`passwordMatches` 往返成立。固化对：`SALT=26dce419976e4e7f95f7a9dcb82e5bc4` / `PASSWORD=$2a$10$74DaI9b3RwzmmA3xpb6ZN.WTzl2YjNf7cLVTTaW1TmaW1rGCdn892`。
- **CSV 值密码往返 Proof**：`TestAuthSeedEncodingProof#testCsvSeedPasswordRoundtrip` 绿——CSV 硬编码 SALT+PASSWORD 对经 `passwordMatches(salt,"123",password)` 成立。
- **小整数 userId 存活 Proof**：`TestAuthSeedLoadingProof#testUserSeedSmallIntegerIdSurvivesAndActive` 绿——`init-database-data=true` 加载 `/_init-data/` 后 `nop_auth_user` userId="1" 保留（未被 seq 覆盖为 UUID）且 STATUS=1（ACTIVE）。
- **种子加载 Proof 四项**：`TestAuthSeedLoadingProof` 3 tests 全绿——(1) 24 角色（21 业务 + 3 平台）；(2) nop userId=1 + STATUS=1 ACTIVE + 密码往返；(3) (1,admin) 绑定存在（B2 修复）。
- **build**：`mvn clean install -DskipTests` 全 reactor BUILD SUCCESS（156 模块）。
- **compliance**：`bash docs/audits/nop-compliance-checker.sh` 零漂移——变更仅 CSV 种子（src/main/resources 数据文件）+ 测试 Java（src/test），checker R1-R12 扫生产 Java 代码，本计划改 0 生产 Java。
- **跨计划一致性**：`nop_auth_role.csv` ROLE_ID 列集合（24 值）== P1.5a Phase 1 D2 冻结词表（21 业务 + admin/nop-admin/user）逐字一致（两处可机器核对锚点：本计划 Related 行引用 P1.5a + Phase 2 项点名 roleId 来源；CSV ROLE_ID 列值集合实测 = 词表）。
- **owner doc 对齐**：`docs/design/roles-and-permissions.md` §角色→权限点映射增「roleId 词表已物化为 DB 记录（P1.5b）」注记（含双命名空间语义分离注记核验：实际种子绑定 nop→ROLE_ID=admin 与 §角色体系 L41-45 注记一致）+ §action-level 增「auth 种子就绪」注记。
- **日志**：`docs/logs/2026/08-09.md` 增 P1.5b 条目（reverse-chronological）。
- **enforcement 状态**：三开关仍 OFF（application.yaml profile 预置 false）；auth 种子落地不改运行时拦截行为。

Follow-up:

- P2.2a（admin 兜底 E2E 基线）：直接后继，nop 账号 + admin 绑定已就绪，触发条件 = P2.2a 进入。
- P2.2b（逐域角色账号）：复用本计划 21 业务角色记录，仅新增 user + user_role 行。
- playwright `allow-create-default-user` flag 清理：归 P2.2a 裁决（CSV 种子使 addDefaultUser 跳过，flag 退化为无害 fallback）。
