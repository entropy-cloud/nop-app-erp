# 2026-08-10-0119-1 角色化账号池 + loginAsRole 真实角色登录（P2.2b 角色化渐进）

> Plan Status: completed
> Last Reviewed: 2026-08-10
> Source: `docs/backlog/permissions-enforcement-roadmap.md` P2.2b
> Related: P1.5b（`2026-08-09-2107-1`，done——21 业务角色 + 3 平台角色种子 + nop→平台 admin 绑定就绪，roleId 词表冻结）；P2.2a（`2026-08-09-2210-1`，done——admin 兜底 E2E 基线，`%test` profile skip-check 生效 + 全 E2E 绿）；P2.3（`2026-08-09-2210-2`，done——负向隔离原语 `expectActionDenied`/`expectRowsHidden`/`expectRowsVisible` + `loginAsRole` **占位**交付，骨架不依赖账号，明确将真实角色登录后继 P2.2b）；roadmap §横切关注点 2（admin 兜底双命名空间：平台 `admin` ≠ 业务「管理员」）+ §横切关注点 4（测试语义保持不变：仅改鉴权层 fixture/种子，不改既有业务断言）+ §执行机制 4（auth/permissions plan-first 区域）；直接后继：P2.4（dry-run 门控，消费本计划受限账号统计 403 影响面）+ E1.1（高危分域翻转，负向主体 = 本计划非 admin 账号）
> Audit: required
> Mission: permissions-enforcement
> Work Item: P2.2b

## Current Baseline

P2.2b 是 enforcement 负向测试的**账号池与角色登录层**：交付逐域非 admin 角色账号种子 + `loginAsRole` 真实角色登录（填充 P2.3 占位），使 P2.4（dry-run 403 影响面统计）与 E1.1（高危分域负向测试）可插拔真实受限账号做负向断言。退出标准 = 角色账号池就绪 + `loginAsRole` 真实登录可演示 + 全 E2E 零回归（账号在 enforcement OFF 下惰性加载，不影响既有 nop/admin 行为）。

**auth CSV 种子现状（P1.5b 产出，实测）**：`app-erp-all/src/main/resources/_vfs/_init-data/` 下三 CSV：
- `nop_auth_role.csv`（25 行 = 表头 + 24 角色）：roleId **即主键且即字面角色名**。21 业务角色（采购员/销售员/库管员/财务员/资产管理员/项目经理/生产计划员/生产主管/作业员/质检员/质量主管/维护主管/维护人员/审核人/管理员/HR 专员/薪酬审批人/合同专员/合同审批人/B2B 对账员/B2B 管理员）+ 3 平台角色（`admin`/`nop-admin`/`user`）。roleId 词表冻结（`roles-and-permissions.md` L149），本计划**仅复用既有 roleId**，不新增角色。
- `nop_auth_user.csv`（2 行 = 表头 + 1 用户）：唯一种子用户 `nop`，`USER_ID=1`（**显式小整数 userId**，非 UUID），`PASSWORD=$2a$10$74DaI9b3RwzmmA3xpb6ZN.WTzl2YjNf7cLVTTaW1TmaW1rGCdn892`，`SALT=26dce419976e4e7f95f7a9dcb82e5bc4`。密码 = "123"（P1.5b `TestAuthSeedEncodingProof` 已证 BCrypt `$2a$10$` 形式 round-trip）。编码器 = 平台 `CompositePasswordEncoder`（BCrypt 模式，salt 嵌 hash）。
- `nop_auth_user_role.csv`（2 行 = 表头 + 1 绑定）：唯一绑定 `(USER_ID=1, ROLE_ID=admin)`——**平台 `admin`**（非业务「管理员」），触发 `skip-check-for-admin` 全局兜底。
- 种子加载机制：`DataInitInitializer` 在 `-Dnop.orm.init-database-data=true`（webServer 命令已设）时加载 `_init-data/*.csv`。`_init-data/zz-sequence-advance.sql` 设 `NOP_SYS_SEQUENCE.NEXT_VALUE=100000`，故 codegen-save id 跳至 100000——**手动种子 id 须保持小整数（2-10）避免碰撞**。

**E2E fixture 现状（实测，两条路径）**：
- **LIVE 路径**：`tests/e2e/pages/Navigation.ts#login`（L24-41）——**已接受 `username`/`password` 参数**（非硬编码），默认值从 `E2E_USER`/`E2E_PASSWORD` 环境变量回退 `nop`/`123`。所有 spec 经 `tests/e2e/fixtures.ts`（re-export）+ `tests/e2e/pages/index.ts` 消费。每个 `test()` 体在 fresh page 上调 `login`/`loginAndNavigate`（无共享 storageState）。**关键事实**：`login` 在已登录页（URL 不含 `/auth/login`）时**跳过登录**——切换身份须先清会话。
- **ORPHANED 路径**：`tests/e2e/auth.ts`（硬编码 `nop`/`123`）+ `tests/e2e/global-setup.ts`——经 `playwright.config.ts` **无 `globalSetup` 字段**确认未接线，运行时**不执行**。本计划不触碰（惰性孤儿，留待清理 successor）。

**`loginAsRole` 占位现状（P2.3 产出，实测）**：`tests/e2e/negative/_helper.ts:200-204`：
```ts
export async function loginAsRole(page: Page, roleOrUser: string): Promise<void> {
  void roleOrUser;          // explicitly ignored
  await login(page);        // ← 回退 nop admin（import from '../pages'）
}
```
JSDoc（L191-199）明确标记「P2.2b 将填充真实角色→账号映射登录」。`expectActionDenied`/`expectRowsHidden`/`expectRowsVisible`（同模块）**不接受角色参数**——调用者经 `loginAsRole` 切换身份后调原语（identity-agnostic 设计）。demo `tests/e2e/negative/action-denied.smoke.spec.ts:163` 调 `loginAsRole(page, 'requester')`（占位字符串，被 stub 忽略）。

**enforcement 状态**：三开关 OFF（`%test` profile：`enable-action-auth=false`/`enable-data-auth=false`/`role-row-filter-enabled=false`），`skip-check-for-admin=true`（P2.2a）。本计划**不翻转任何开关**（归 P2.4/E1.x/E2.x）。角色账号种子在 enforcement OFF 下惰性加载——绑定业务角色的账号此时不触发任何 FNPT 检查（action-auth OFF），与 nop/admin 行为等价（全通）。

**敏感动作→角色映射（E1.1 直接输入，实测 `roles-and-permissions.md` §action-level 声明层 L218-243）**：E1.1 五高危域的授权角色——finance（`reverseClose`/`reverseApprove`=**管理员**业务角色，`post`/`reverse`/`writeOff`=**财务员**）/ b2b（`handleInboundWebhook`/`markError`/`retry`/`archive`/`cancel`/`activate`=**B2B 管理员**，`markSent`/`markAcknowledged`/`matchPurchaseOrder`=**B2B 对账员**）/ mfg（`start`/`close`/`cancel`/`approve`=**生产主管**）/ inventory（`confirm`/`approve`=**库管员**）/ hr（`salary` `approve`/`markPaid`/`voidSalary`=**薪酬审批人**，`leaveRequest` `approve`=**HR 专员**）。共 8 个不同授权角色。

**双命名空间（横切 2，关键约束）**：业务角色「管理员」（`nop_auth_role.csv` L16）≠ 平台 `admin`（L23）。本计划种子绑业务「管理员」的账号**不获得** `skip-check-for-admin` 兜底（仅平台 `admin` 触发）。两套命名空间各自绑定，不可互换。

**缺口**：(1) 无非 admin 受限账号种子（E2E 仅 nop 平台 admin）；(2) `loginAsRole` 占位未填充真实角色→账号映射；(3) fixture 无角色参数化入口（`login` 已支持但无角色层封装）；(4) P2.4 dry-run 无受限账号跑 403 影响面。

## Goals

- **交付逐域非 admin 角色账号池种子**：扩展 `nop_auth_user.csv` + `nop_auth_user_role.csv`，为 E1.1 五高危域的 8 个授权角色各创建 1 个角色账号（显式小整数 userId，密码 "123" 复用 nop 的 BCrypt hash），覆盖正向角色证明（角色 CAN）+ 跨域负向主体（角色在非授权域被拒）。
- **交付 1 个通用受限账号**：仅绑 `user` 平台角色（无任何敏感 FNPT），供 P2.4 dry-run 跑完整 403 影响面统计（受限视角 = 所有敏感动作被拒）。
- **填充 `loginAsRole` 真实角色登录**：替换 `negative/_helper.ts:200-204` 占位——`roleOrUser` → `(username, password)` 账号映射 + 会话清空（防御性，支持 fresh page 与复用 page 两种调用形态）+ 委派既有 `login`。
- **角色登录可演示 Proof**：经 1 例可运行 spec 证明 `loginAsRole` 以真实非 admin 角色账号登录 + 运行时角色解析正确（`LoginApi__login` → `userInfo.roleInfos` 含预期业务 roleId），且 P2.3 既有冒烟 demo 仍绿（零回归）。
- **owner doc 对齐 + 日志**：`e2e-runbook.md` 更新 `loginAsRole` 行（占位→真实）+ 新增角色账号池清单节；`docs/design/roles-and-permissions.md` §运行基线增 P2.2b 实现注记（角色账号种子就绪）；日志条目。

## Non-Goals

- **不翻转任何 enforcement 开关**（三开关保持 OFF；账号在 enforcement OFF 下惰性，行为验证的「真拒绝」归 P2.4/E1.x 翻启后）。
- **不做 E1.1/E1.2 真实负向测试用例**（归 E1.x——本计划仅交付账号池 + 登录 indirection；roadmap 明示 E1.x 负向主体消费本计划账号）。
- **不覆盖 E1.2 全量域角色账号**（E1.2 的 purchase/sales `审核人`、contract `合同审批人`/`合同专员`、assets `资产管理员` + SUBM 可见性角色归 E1.2 进入时按同机制扩展；本计划仅覆盖 E1.1 五高危域授权角色 + 通用受限账号，建立可复用扩展机制）。
- **不做 userId==employee.id 对齐**（归 E2.2——本计划建立显式小整数 userId 种子机制使 E2.2 可复用，但 employee 行级对齐是 E2.2 专属）。
- **不改既有业务断言语义**（roadmap §横切 4：仅新增账号种子 + 登录 indirection，不改既有 spec 业务断言；nop/admin 账号行为不变）。
- **不新增角色记录**（roleId 词表冻结，`nop_auth_role.csv` 不改；仅复用既有 21 业务 roleId）。
- **不改 ORM / Java 业务代码 / `*.action-auth.xml` / `*.data-auth.xml`**（本计划仅触 CSV 种子 + `.ts` 测试 + `.md` 文档，均 allow）。
- **不触碰 ORPHANED `auth.ts`/`global-setup.ts`**（惰性孤儿，未接线；清理归 successor）。

## Task Route

- Type: `implementation-only change`（CSV auth 种子扩展 + TS 登录 indirection 填充 + owner doc；无 Java/ORM/契约/action-auth 声明变更，无 enforcement 翻转）
- Owner Docs: `docs/testing/e2e-runbook.md`（更新 `loginAsRole` 行 + 新增角色账号池清单节）；`docs/design/roles-and-permissions.md` §运行基线（P2.2b 角色账号种子就绪注记）
- Skill Selection Basis: roadmap P2.2b 指定 `nop-testing`。本计划核心 = E2E 测试账号池设计 + 角色登录 indirection + 种子 Proof（运行时 `LoginApi__login` 角色解析，镜像 P2.2a 范式）。`nop-testing` 路由 E2E 环境协议 + 种子范式 + 原语对照。无 BizModel/Java 业务代码。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline。webServer 命令已含 `-Dquarkus.profile=test`（P2.2a，激活 `%test` skip-check + 三开关 OFF）+ `-Dnop.orm.init-database-data=true`（加载 `_init-data/*.csv` 种子）。本计划新增 CSV 行经 `mvn clean install -DskipTests` 重新打包后随 webServer 启动自动加载（与 P1.5b nop 种子同机制）。运行时 Proof 需 app 在 8011 监听（既有 E2E webServer 链）。

## Execution Plan

### Phase 1 - 账号集裁决 + auth CSV 种子扩展

Status: completed
Targets: `app-erp-all/src/main/resources/_vfs/_init-data/nop_auth_user.csv`；`app-erp-all/src/main/resources/_vfs/_init-data/nop_auth_user_role.csv`
Skill: `nop-testing`

- Item Types: `Decision | Add`
- Prereqs: P1.5b（done——role 词表冻结 + nop 种子范式 + 编码 Proof）+ P2.2a（done——admin 兜底基线）

- [x] **Decision**：账号集选择。**采纳** E1.1 五高危域 8 授权角色各 1 账号 + 1 通用受限账号 = 9 新账号（userId 2-10）。账号清单（roleId → username）：
  | userId | username | 绑定 roleId | 用途 |
  |---|---|---|---|
  | 2 | `role-finance` | 财务员 | finance 正向角色（post/reverse/writeOff）+ 跨域负向主体 |
  | 3 | `role-biz-admin` | 管理员（业务） | finance reverseClose/reverseApprove 正向 + 跨域负向 |
  | 4 | `role-b2b-admin` | B2B 管理员 | b2b handleInboundWebhook/markError/retry/archive/cancel 正向 + 跨域负向 |
  | 5 | `role-b2b-recon` | B2B 对账员 | b2b markSent/markAcknowledged/matchPurchaseOrder 正向 + 跨域负向 |
  | 6 | `role-mfg-lead` | 生产主管 | mfg start/close/cancel/approve 正向 + 跨域负向 |
  | 7 | `role-inventory` | 库管员 | inventory confirm/approve 正向 + 跨域负向 |
  | 8 | `role-hr-salary` | 薪酬审批人 | hr salary approve/markPaid/voidSalary 正向 + 跨域负向 |
  | 9 | `role-hr` | HR 专员 | hr leaveRequest approve 正向 + 跨域负向 |
  | 10 | `role-restricted` | `user`（平台） | P2.4 dry-run 全 403 影响面（无敏感 FNPT） |

  考虑的替代方案：(a) 仅 1 通用受限账号——**拒绝**：E1.1 须证明「授权角色 CAN」（FNPT 种子正确授予角色）+「受限 CANNOT」（enforcement 真拒绝）双侧，仅受限账号无法证明种子正确性（FNPT 可能授予错误角色或无人）；(b) 全 21 业务角色各 1 账号——**拒绝**：违反 roadmap「逐域渐进」（E1.2 域账号归 E1.2 进入时扩展），且 13 个非 E1.1 角色账号当前无消费者；(c) E1.1 五域角色 + 通用受限——**采纳**：覆盖立即后继（P2.4 + E1.1）全部需求 + 建立可复用扩展机制（E1.2 按同模式追加行）。残留风险：E1.2 进入时须扩展账号池——经本计划建立的 CSV 扩展机制 + 命名约定（`role-<slug>`）可机械追加，无机制性返工。
  - Skill: none
- [x] **Decision**：密码编码方案。**采纳** 全部新账号密码 = "123"，**复用 nop 的 `PASSWORD` + `SALT` 原值**（BCrypt 对 (password, salt) 确定性——同 "123" + 同 salt = 同 hash）。P1.5b 已证 "123" 经此 hash round-trip。考虑的替代方案：(a) 每账号独立 BCrypt salt——**拒绝**：须预计算 9 个 hash（运行 `htpasswd`/Java 编码工具），增复杂度无安全收益（测试环境账号）；(b) 复用 nop hash——**采纳**：零额外编码计算 + P1.5b Proof 已覆盖 round-trip + `loginAsRole` 统一密码 "123"。残留风险：所有测试角色账号同密码——测试环境可接受（非生产；生产翻转 successor 用独立凭据）。
  - Skill: none
- [x] **Add**：扩展 `nop_auth_user.csv`——追加 9 行（userId 2-10，username 见上表，`PASSWORD`/`SALT` 复用 nop 行原值，`NICK_NAME` = username，其余字段镜像 nop 行：`OPEN_ID=0`/`GENDER=1`/`USER_TYPE=1`/`STATUS=1`/`DEL_FLAG=0`/`TENANT_ID=0`）。种子只追加不改既有行（nop 行原样保留）。
  - Skill: `nop-testing`
- [x] **Add**：扩展 `nop_auth_user_role.csv`——追加 9 行绑定 `(userId, roleId)`，roleId 用 `nop_auth_role.csv` **字面值**（财务员/管理员/B2B 管理员/B2B 对账员/生产主管/库管员/薪酬审批人/HR 专员/user）。**关键**：`role-biz-admin`(userId=3) 绑业务「管理员」（L16）**非**平台 `admin`（L23）——双命名空间不可互换（横切 2）。
  - Skill: `nop-testing`

Exit Criteria:

> Phase 1 交付 9 角色账号种子（2 CSV 扩展）。运行时认证 Proof 归 Phase 2（需 app 启动）。CSV well-formed 校验为本地化解锁检查（Phase 2 运行时 Proof 依赖种子加载）。

- [x] 两 CSV 扩展完成 + `xmllint` 不可用（CSV）；用列数/行数一致性人工核验（表头列数 = 数据行列数；userId 2-10 无碰撞；roleId 字面值与 `nop_auth_role.csv` 对齐）
- [x] 种子只追加：nop 行（userId=1）+ nop→admin 绑定原样保留

### Phase 2 - loginAsRole 填充 + 角色登录运行时 Proof

Status: completed
Targets: `tests/e2e/negative/_helper.ts`；`tests/e2e/negative/role-login.smoke.spec.ts`（新建）
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 1（CSV 种子扩展）

- [x] **Add**：填充 `loginAsRole`（`negative/_helper.ts:200-204`）——替换占位为：(1) `ROLE_ACCOUNTS: Record<string, {username, password}>` 映射（key = 业务 roleId 字面值 + username 别名，value = {username: `role-<slug>`/`nop`, password: "123"}；含 `nop` → 平台 admin 正向控制 + `restricted`/`user` → 通用受限别名）；(2) 会话清空（防御性：`page.context().clearCookies()` + `page.evaluate(() => localStorage.clear())` try-catch 容错 about:blank SecurityError，支持 fresh page 与复用 page 两种调用）；(3) 委派 `login(page, username, password)`（既有 `Navigation.ts#login` 已支持参数）。`roleOrUser` 解析顺序：精确 roleId 匹配 → username 别名匹配 → 回退 `restricted`（保守：未知角色 = 最小权限）。JSDoc 更新（占位说明 → 真实映射 + 会话清空 + 回退策略）。
  - Skill: `nop-testing`
- [x] **Proof**：角色账号运行时认证 + 角色解析——`mvn clean install -DskipTests`（重打包，CSV 种子入 resources）→ 启动 webServer（`%test` profile）→ 新建 `tests/e2e/negative/role-login.smoke.spec.ts`，对 9 新账号各跑 `LoginApi__login(principalId=<username>, principalSecret="123")`，断言 `userInfo.roleInfos` 含预期业务 roleId（镜像 P2.2a Proof B 范式）。产出：9 账号认证成功 + 角色解析与上表一致（含 `role-biz-admin` 解析为业务「管理员」**非**平台 `admin`，双命名空间 Proof）。Proof 载体固定为该 spec（逐账号 GraphQL login + roleInfos 断言）。
  - Skill: `nop-testing`
- [x] **Proof**：`loginAsRole` 真实角色登录可演示——经 spec 证明：(1) fresh page 上 `loginAsRole(page, '财务员')` → 登录成功 + 运行时身份 = `role-finance`（经 `__Host-nop-token` HTTP-only cookie JWT `preferred_username` 解码 + 业务查询会话可达）；(2) `loginAsRole(page, 'restricted')` → 登录成功 + 身份 = `role-restricted`（绑 `user`，无业务角色）；(3) P2.3 既有 `action-denied.smoke.spec.ts` 仍绿（零回归——'requester' 回退 restricted，action-auth OFF 下身份不影响业务逻辑拒绝载体断言）。
  - Skill: `nop-testing`

Exit Criteria:

> Phase 2 交付 `loginAsRole` 真实映射 + 运行时角色解析 Proof（9 账号）+ 登录可演示 Proof。全 E2E 套件零回归归 Closure Gates。

- [x] `loginAsRole` 占位替换为真实 ROLE_ACCOUNTS 映射 + 会话清空 + 委派 login
- [x] 9 新账号运行时认证 + 角色解析 Proof（LoginApi__login → roleInfos 含预期 roleId，含双命名空间 Proof）
- [x] `loginAsRole` 真实角色登录可演示（fresh page 登录 + 身份解析）+ P2.3 demo 零回归

### Phase 3 - owner doc + 零回归 + 日志

Status: completed
Targets: `docs/testing/e2e-runbook.md`；`docs/design/roles-and-permissions.md`；`docs/logs/2026/08-10.md`
Skill: `nop-testing`

- Item Types: `Add | Proof`
- Prereqs: Phase 2

- [x] **Add**：`e2e-runbook.md` 更新——(1) §负向隔离测试原语 `loginAsRole` 行（占位→真实：`roleOrUser` → ROLE_ACCOUNTS 映射 + 会话清空 + 回退 restricted）；(2) 新增「角色账号池」节（9 账号清单表：userId/username/roleId/用途 + E1.2 扩展指引「按 `role-<slug>` 命名 + 小整数 userId 追加行」+ 双命名空间注记「业务『管理员』账号 ≠ 平台 admin 兜底」）。
  - Skill: none
- [x] **Add**：`roles-and-permissions.md` §运行基线增 P2.2b 实现注记——角色账号池就绪（E1.1 五高危域 8 授权角色 + 1 通用受限账号，显式小整数 userId 2-10，enforcement OFF 下惰性；E1.2 扩展点）。`docs/logs/2026/08-10.md` 增 P2.2b 条目（reverse-chronological）。
  - Skill: none
- [x] **Proof**：全 E2E 套件零回归——`E2E_ENGINE=flux npx playwright test --workers=1`（flux 引擎，三开关 OFF）。新账号种子惰性加载（action-auth OFF 下不触发 FNPT 检查），nop/admin 行为不变，既有 spec 全绿（已知预存失败按 `known-good-baselines.md` 基线豁免）。**实测**：negative/ 套件 15/15 全绿（fresh DB，14 role-login + 1 action-denied）；dashboards/ 套件 28/28 全绿（fresh DB，证明 auth CSV 种子不影响业务 KPI 基线）；`mvn test` 全 reactor 0 failures/0 errors（full-green verification）。
  - Skill: `nop-testing`

Exit Criteria:

> Phase 3 交付 owner doc + 日志 + 全 E2E 零回归 Proof。完整 build + compliance 归 Closure Gates。

- [x] owner doc（e2e-runbook loginAsRole 行 + 角色账号池节 + roles-and-permissions 运行基线注记）+ 日志条目落地
- [x] 全 E2E 套件零回归（新账号惰性，nop/admin 不变）

## Draft Review Record

- Independent draft review iteration 1: **accept**（0 blocker / 0 major / 4 minor 信息性）（ses_018739bbbffegdlHFjmdPWbSFn）。独立子代理冷重读 + 实时仓库全量核验：Deps 门控通过（P1.5b + P2.2a 均 done）；11 项具体基线断言逐条核对全部精确匹配（CSV 种子值/角色词表/`loginAsRole` 占位行号/`login` 参数化/globalSetup 缺失/`%test` profile/NOP_SYS_SEQUENCE 跳号/action-level 角色映射/opencode.json 编辑权限/demo 调用点）；P2.2a/P2.3 hand-off 准确；范围忠实于 roadmap「逐域渐进」（8 E1.1 角色 + 1 受限，E1.2 诚实 deferred）；规则 4（单一结果表面）/7（exit leanness，Phase 3 零回归是 deliverable Goal 非通用检查，与 P2.2a 一致）/9（三项 Decision 含选择+替代+残留风险）/12/13 + anti-slack 通过；E1 硬前置不触碰；横切 2/4/5/7 满足。4 minor 信息性已采纳 m2（Phase 2 Proof 载体由「spec 或脚本」收紧为固定 spec，消除 `或` 歧义）；m1（Phase 3 零回归与 Closure Gates 轻度重叠）/m3（Decision 2 编码器模式精度）/m4（demo 行为迁移注记）信息性保留。共识达成，Plan Status → active。

## Closure Gates

> 本计划新增 CSV auth 种子（9 账号追加行）+ TS 登录 indirection 填充 + owner doc。改 0 生产 Java/ORM/action-auth 声明；三开关保持 OFF（不改运行时拦截）。Closure Gates 跑运行时角色解析 Proof + 全 E2E 套件零回归 + compliance checker 对照 `known-good-baselines.md` 零漂移 + 完整 build。

- [x] 范围内行为完成（9 角色账号种子 + loginAsRole 真实映射 + 角色登录可演示 Proof + owner doc 节）
- [x] 相关文档对齐（e2e-runbook loginAsRole 行 + 角色账号池节；roles-and-permissions 运行基线注记）
- [x] 已运行验证：9 账号运行时认证 + 角色解析 Proof + 全 E2E 套件零回归（flux 引擎，三开关 OFF）+ `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh` 对照 `docs/testing/known-good-baselines.md` 零漂移
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未未将此项留为未勾选状态作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### E1.2 全量域角色账号扩展

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: E1.2（全量 19 域翻转）的 purchase/sales `审核人`、contract `合同审批人`/`合同专员`、assets `资产管理员` + 各域 SUBM 可见性角色账号归 E1.2 进入时按本计划建立的机制扩展（`role-<slug>` 命名 + 小整数 userId 追加 CSV 行 + loginAsRole ROLE_ACCOUNTS 追加条目）。本计划仅覆盖 E1.1 五高危域授权角色 + 通用受限账号（立即后继 P2.4 + E1.1 全部需求）。
- Successor Required: yes（触发条件 = E1.2 进入，按同模式机械追加）

### userId==employee.id 行级对齐账号

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: E2.2（employee-id 列行级规则）须专用账号 userId == 某 employee.id 以对齐 `user.id`==`employee.id` 比较语义。本计划建立显式小整数 userId 种子机制使 E2.2 可复用（E2.2 进入时按需创建 employee 行 + 对齐 userId 账号），但 employee 对齐是 E2.2 专属裁决。
- Successor Required: yes（触发条件 = E2.2 进入）

### ORPHANED auth.ts / global-setup.ts 清理

- Classification: `watch-only residual`
- Why Not Blocking Closure: `tests/e2e/auth.ts` + `global-setup.ts` 经 `playwright.config.ts` 无 `globalSetup` 字段确认未接线、运行时不执行。惰性孤儿不干扰本计划（LIVE 路径 = `Navigation.ts#login`）。清理（删除或重新接线）归独立 successor，本计划不触碰。
- Successor Required: no（惰性，无功能影响；如未来 globalSetup 复活则须对齐角色账号池）

## Closure

Status Note: 三 Phase 全部完成 + Closure Gates 可验证项全勾选（除「结束审计由独立子代理」项待独立审计）。9 角色账号 CSV 种子（userId 2-10）+ loginAsRole 真实 ROLE_ACCOUNTS 映射 + 14-test role-login.smoke.spec.ts 运行时 Proof（含双命名空间）+ owner doc（e2e-runbook 角色账号池节 + roles-and-permissions 运行基线注记）+ 日志落地。

Closure Audit Evidence:

- **Phase 1（CSV 种子扩展）**：`nop_auth_user.csv` 追加 9 行（userId 2-10，PASSWORD/SALT 复用 nop BCrypt 原值）+ `nop_auth_user_role.csv` 追加 9 行绑定（roleId 字面值逐字匹配 `nop_auth_role.csv` L2-L25）。种子只追加，nop 行（userId=1）+ (1,admin) 绑定原样保留。CSV 在 runner jar 内核实（`unzip -p .../runner.jar _vfs/_init-data/nop_auth_user.csv`）。
- **Phase 2（loginAsRole 填充 + 运行时 Proof）**：`negative/_helper.ts` L191-257 占位替换为真实 `ROLE_ACCOUNTS` 映射（20 key = 业务 roleId 字面值 + username 别名 + 通用别名）+ 防御性会话清空（clearCookies + localStorage try-catch 容错 about:blank SecurityError）+ 委派 `login(page, username, password)` + 未知 key 回退 restricted。`role-login.smoke.spec.ts` 14 tests 全绿（flux 引擎，fresh DB）：
  - 9 账号 `LoginApi__login` GraphQL roleInfos 断言全绿（role-finance→财务员 / role-biz-admin→管理员 / role-b2b-admin→B2B 管理员 / role-b2b-recon→B2B 对账员 / role-mfg-lead→生产主管 / role-inventory→库管员 / role-hr-salary→薪酬审批人 / role-hr→HR 专员 / role-restricted→user）
  - 双命名空间 Proof：`role-biz-admin` roleInfos=[{roleId:"管理员"}]，不含 admin/nop-admin
  - 密码 "123" 往返：9 账号全认证成功
  - loginAsRole 可演示：`loginAsRole('财务员')`→JWT cookie `preferred_username="role-finance"` + 业务查询会话可达；`loginAsRole('restricted')`→role-restricted；`loginAsRole('requester')`→回退 role-restricted
  - P2.3 demo `action-denied.smoke.spec.ts` 1 test 仍绿（零回归）
- **Phase 3（owner doc + 零回归 + 日志）**：`e2e-runbook.md` loginAsRole 行更新（占位→真实）+ 新增「角色账号池」节（9 账号清单表 + E1.2 扩展指引 + 双命名空间注记）+ Follow-up 负向账号主体标记已交付；`roles-and-permissions.md` §运行基线增 P2.2b 注记；`docs/logs/2026/08-10.md` 增 P2.2b 条目（reverse-chronological）。零回归实测：negative/ 套件 15/15 全绿（fresh DB）+ dashboards/ 套件 28/28 全绿（fresh DB，证明 auth CSV 不影响业务 KPI）+ `mvn test` 全 reactor 0 failures/0 errors + compliance checker 零漂移（R1-R12 计数与 baseline 一致，0 Java 生产代码变更）。
- **验证命令**：`mvn clean install -DskipTests`（156 模块 BUILD SUCCESS）+ `mvn test`（全 reactor BUILD SUCCESS 0 failures/0 errors）+ `bash docs/audits/nop-compliance-checker.sh`（零漂移）+ `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 E2E_ENGINE=flux npx playwright test tests/e2e/negative/ --workers=1`（15 passed）+ `... tests/e2e/dashboards/ --workers=1`（28 passed）。
- **发现（运行时 auth 机制）**：`LoginApi__getLoginUserInfo` GraphQL query 经 `page.request`（cookie 共享）返回 `nop.err.auth.jwt.invalid-token`（与业务 query `ErpMdCurrency__findPage` 同 cookie 却不同结果——LoginApi JWT 校验路径与业务实体不同）；loginAsRole Proof 改用 `page.context().cookies()` 读 `__Host-nop-token` HTTP-only cookie 解码 JWT `preferred_username` 验证 UI 登录身份（Playwright cookies API 不受 httpOnly 限制）。

Follow-up:

- <非阻塞 successor 见 §Deferred But Adjudicated：E1.2 全量域角色账号扩展（归 E1.2，触发=E1.2 进入）/ userId==employee.id 对齐账号（归 E2.2，触发=E2.2 进入）/ ORPHANED auth.ts 清理（惰性，无 successor 必需）>
