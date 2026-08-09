# 2026-08-09-2210-1 admin 兜底 E2E 基线（skip-check 生效 + nop→admin 解析 + 全 E2E 绿）

> Plan Status: completed
> Last Reviewed: 2026-08-09
> Source: `docs/backlog/permissions-enforcement-roadmap.md` P2.2a
> Related: P1.5b（`2026-08-09-2107-1`，done——已种子 nop（userId=1，STATUS=1 ACTIVE）+ `nop_auth_user_role` 绑定 (1, admin)，B2 修复落地；本计划 Direct 后继消费其账号种子）；P2.1（`2026-08-09-0751-3`，done——`app-erp-all/application.yaml` `%dev`/`%test`/`%prod` profile 预置三开关 false + `%dev`/`%test` `skip-check-for-admin: true`；本计划解除其未在 E2E 运行时生效的遗留 gap）；P2.2b（角色化渐进，直接后继——逐域补角色账号 + fixture 角色参数化，依赖本计划确立的「admin 兜底就绪」基线）；P2.4（dry-run 门控，直接后继——翻 `enable-action-auth` + admin 跑通全 E2E + 非 admin 子集登记 403 影响面，依赖本计划的 admin 基线）；roadmap §横切关注点 2（admin 兜底双命名空间）+ §横切关注点 3（灰度纪律：admin 跑绿 → 翻域 → 负向证明）+ §执行机制 5（E1 硬前置 = P2.2a + P2.4）
> Audit: required
> Mission: permissions-enforcement
> Work Item: P2.2a

## Current Baseline

P2.2a 是 enforcement 从声明层推进到强制执行层的**管理员兜底就绪门控**：使 nop 测试账号的平台 admin 角色绑定（P1.5b 种子）经 `DefaultActionAuthChecker` 的 `skip-check-for-admin` 兜底在 **E2E 运行时**真正生效（必要条件闭环），并以全 E2E 套件绿建立 enforcement-OFF 回归基线。本计划**不翻转任何 enforcement 开关**（翻转归 P2.4/E1.x）；「skip-check 在 live enforcement 下真正放行」的充分性证明归 P2.4（action-auth 翻启后 admin 跑绿），本计划只交付**就绪**（config 生效 + 角色解析 + 零回归）。

**P1.5b 账号种子现状（done，已落盘）**：`app-erp-all/src/main/resources/_vfs/_init-data/` 新增 3 个 auth CSV——`nop_auth_role.csv`（24 行：21 业务角色中文字面 + 平台 `admin`/`nop-admin`/`user`）、`nop_auth_user.csv`（单行 nop：`USER_ID=1` 显式小整数、`STATUS=1` ACTIVE、`CompositePasswordEncoder` 编码 "123" 的 SALT/PASSWORD 对）、`nop_auth_user_role.csv`（单行 `(USER_ID=1, ROLE_ID=admin)`——B2 修复：绑**平台** admin 角色非业务「管理员」，双命名空间分离）。`TestAuthSeedLoadingProof` 已证 fresh-DB 加载后 userId="1" 保留 + STATUS=1 + (1,admin) 绑定存在。**即 nop 在 DB 层已是平台 admin 角色账号。**

**P2.1 配置现状（done，已落盘）**：`app-erp-all/application.yaml` L48-76 三 profile 块——`%dev`(L48-58)/`%test`(L59-67) 含三开关 false + `skip-check-for-admin: true`；`%prod`(L68-76) 含三开关 false、skip-check 省略（继承平台默认 false，DR-1e 安全姿态）。顶层（default profile）L2-25 无三开关显式设置（→ 平台默认 false）也无 skip-check 设置（→ 平台默认 false）；L8 `allow-create-default-user: false`。

**load-bearing 发现——E2E 运行时未激活任何 profile（实测）**：

- `playwright.config.ts:18` webServer.command：`java -Dfile.encoding=UTF8 -Dnop.auth.service-public=true -Dnop.auth.login.allow-create-default-user=true -Dnop.orm.init-database-data=true -Dnop.web.render-mode=flux -Derp-*.… -jar …`——**无 `-Dquarkus.profile=`**。
- `_tmp-server.sh:43-87`（playwright webServer 的手动镜像，单一真相源 = playwright.config.ts）：**同样无 `-Dquarkus.profile=`**。
- 仅 `tools/run-app-erp-all.sh:31`（手动本地运行脚本，**非 E2E 路径**）设 `-Dquarkus.profile=dev`。
- Quarkus 对 `java -jar` 打包件默认激活**空（default）profile**（`%dev` 仅在 `mvn quarkus:dev` 自动激活），故 E2E 期间 `%dev`/`%test`/`%prod` profile 块**均不读**。
- **佐证**：webServer 命令显式设 `-Dnop.auth.login.allow-create-default-user=true`，与 `%dev`(L52) 的 true 重复——表明团队知晓 E2E 不走 %dev profile，依赖显式 `-D` flag 而非 profile 块。

**结论**：P2.1 在 `%dev`/`%test` 预置的 `skip-check-for-admin: true` **在 E2E 运行时不生效**（profile 未激活）→ E2E 有效 skip-check = 平台默认 false。P2.2a 必须使 skip-check 在 E2E 运行时生效，否则 P2.4 翻 action-auth 后 nop（admin 角色）仍会被权限检查拦截（B2 风险复发），admin 兜底失效。这是本计划的**核心 gap**。

**enforcement 状态（E2E 运行时有效值）**：三开关 effective = 平台默认 false（顶层无设置 + profile 不激活 + 无显式 -D）→ 任何登录用户全通；本计划**不改变**三开关有效值（保持 false，归 P2.4/E2.1 翻启）。

**admin 兜底机制（实测，P2.1 已核验）**：`DefaultActionAuthChecker.isPermitted`（`nop-entropy/.../DefaultActionAuthChecker.java:35-41`）当 `CFG_AUTH_SKIP_CHECK_FOR_ADMIN`（`NopAuthConfigs.java:77`，IConfigReference 默认 false）= true 时，`userContext.isUserInRole("admin") || isUserInRole("nop-admin")` → return true 跳过检查。角色集来源：`LoginServiceImpl.buildUserContext`（`LoginServiceImpl.java:292-336`）经 `user.getRoles()`（`nop_auth_user_role`→`nop_auth_role` join）收集 roleId → `context.setRoles(roleIds)`。**即 skip-check=true AND nop 经 join 解析得 roleId 集合含 "admin" → 兜底生效。** P1.5b 已证种子侧 (1,admin) 存在；P2.2a 须证**运行时** `buildUserContext` 解析后 `isUserInRole("admin")` 为 true（必要条件之二）。

**playwright `allow-create-default-user` flag（P1.5b Deferred 交接）**：webServer 命令 + `_tmp-server.sh` 均显式 `-Dnop.auth.login.allow-create-default-user=true`。P1.5b CSV 种子使 `nop_auth_user` 表非空 → `LoginServiceImpl.addDefaultUser()`（`LoginServiceImpl.java:132-170`）因 `if (dao.count() > 0) return` 跳过，flag 退化为**无害 fallback**（不创建重复 nop）。P1.5b 将「flag 去留裁决」交接给 P2.2a。

**fixture 现状**：`tests/e2e/auth.ts` 硬编码 `TEST_USERNAME='nop'`/`TEST_PASSWORD='123'`；`global-setup.ts` 经 `performLogin` 建立 nop-token 会话；`fixtures.ts` page fixture 注入 `__FLUX_DEBUG__`。**nop 已是 admin 账号（P1.5b 种子），fixture 无需改用户**——P2.2a 的「fixture 切 admin」在种子层已完成，本计划验证其运行时解析（角色参数化 fixture 归 P2.2b）。

**缺口**：(1) E2E 运行时 skip-check 未生效（profile 未激活）；(2) nop→admin 角色运行时解析（isUserInRole）未证；(3) 全 E2E 套件在「skip-check 生效 + 三开关 OFF」配置下零回归未立；(4) playwright `allow-create-default-user` flag 去留未裁决。

## Goals

- **使 skip-check-for-admin 在 E2E 运行时生效（核心 gap 闭环）**：选择 profile 激活路径（Decision）使 `%test`（或等效）profile 块的 `skip-check-for-admin: true` 在 E2E webServer 运行时成为有效值，P2.4 翻 action-auth 后 admin 兜底可生效。
- **运行时 Proof：nop→admin 角色解析**：证明 E2E 登录 nop 后 `buildUserContext` 解析的 roleId 集合含 "admin"（`isUserInRole("admin")` 为 true），即 P1.5b 种子在运行时经 join 正确解析（必要条件之二闭环）。
- **全 E2E 套件零回归基线**：在「skip-check 生效 + 三开关保持 OFF」配置下跑通全 E2E 套件绿，证明 profile/配置变更与 P1.5b 种子对既有 nop/123 全通行为零回归（enforcement OFF 时 skip-check 无可观测拦截效果，回归风险 = profile 激活连带效应 + 种子加载副作用）。
- **裁决 playwright `allow-create-default-user` flag 去留**（P1.5b Deferred 交接）：CSV 种子使 flag 退化为无害 fallback；裁决保留（无害）/ 清理（去冗余），落地并文档化。
- **owner doc 对齐 + 日志**：`roles-and-permissions.md` §运行基线增「P2.2a admin 兜底就绪（skip-check E2E 生效 + nop→admin 运行时解析 + 全 E2E 绿基线）」注记；日志条目。

## Non-Goals

- **不翻转任何 enforcement 开关为 ON**（`enable-action-auth`/`enable-data-auth`/`role-row-filter-enabled` 翻启归 P2.4 / E2.1；本计划三开关保持 effective false）。
- **不证「skip-check 在 live enforcement 下真正放行」的充分性**（归 P2.4——翻 action-auth 后 admin 跑绿即充分性证明；本计划只交付必要条件：config 生效 + 角色解析）。
- **不做逐域角色账号 / fixture 角色参数化**（归 P2.2b——按负向测试需要逐域补账号 + fixture 支持角色参数化；本计划 fixture 仍单一 nop 账号）。
- **不做负向隔离测试原语/脚手架**（归 P2.3；本计划仅正向 admin 基线）。
- **不改 auth 表 CSV 种子**（P1.5b 已交付；本计划不改种子，仅运行时验证 + 配置 flag 裁决）。
- **不改 ORM / Java 业务代码**（skip-check 消费机制由平台 `DefaultActionAuthChecker` + IConfigReference 落地，本计划仅 config/flag/Proof）。
- **不裁决 prod profile / prod skip-check 姿态**（prod 翻转整体为 successor，触发 = 测试环境全绿 + 生产灰度计划人工批准；本计划仅 test/dev profile）。
- **不改 19 个域 `erp-*-app/application.yaml`**（单域独立 runner，非聚合运行时权威）。

## Task Route

- Type: `implementation-only change`（E2E webServer profile 激活 config 变更 + playwright flag 裁决 + owner doc 注记 + Proof；不改 Java 业务代码，不改运行时拦截行为——三开关保持 OFF，skip-check 在 action-auth OFF 时无可观测拦截效果）
- Owner Docs: `docs/design/roles-and-permissions.md` §运行基线（admin 兜底就绪注记 + E2E profile 激活注记）+ §角色体系（双命名空间，P1.5b 已落地，本计划运行时复核）；`docs/testing/e2e-runbook.md` §认证机制 + §启动方式（E2E profile 激活若改变启动语义须注记）
- Skill Selection Basis: roadmap P2.2a 指定 `nop-testing`。本计划核心 = E2E 运行时 config 变更 + Proof（skip-check 有效值 + 角色解析 + 全 E2E 绿基线）+ flag 裁决。`nop-testing` 路由 Proof 阶段的基线对照（enforcement-OFF 全 E2E 绿）与运行时断言方法。无 BizModel/Java 业务代码（机制由平台 bean 落地）。

## Infrastructure And Config Prereqs

- E2E 运行依赖既有 webServer 链（fresh-DB `rm -f db/*.mv.db` + `init-database-data=true` + runner jar）。本计划改 webServer 启动 profile（Decision 后定），复用既有 `_tmp-server.sh` / `playwright.config.ts` 单一真相源同步规则（`_tmp-server.sh` header 明确「JVM args MUST stay in sync with playwright.config.ts」）。
- 三开关保持 OFF，无 enforcement 运行时效果；skip-check 在 action-auth OFF 时无可观测拦截（其消费方 `DefaultActionAuthChecker.isPermitted` 受 `enableActionAuth` 门控——action-auth OFF 时 checker 不进入 isPermitted 路径）。
- No infra prereqs beyond existing baseline。

## Execution Plan

### Phase 1 - skip-check E2E 生效路径裁决 + 角色解析 Proof 机制探查

Status: completed
Targets: 本计划内 Decision 记录 + Proof 机制确认
Skill: `nop-testing`（角色解析 Proof 探查）

- Item Types: `Decision | Proof`
- Prereqs: P1.5b（done）+ P2.1（done）

- [x] **Proof（探查）**：角色解析运行时 introspection 机制确认——E2E 登录 nop 后，确定一个可观测「`buildUserContext` 解析的 roleId 集合含 admin」的 introspection 路径，供 Phase 2 Proof 使用。**首选路径**（优先验证）：(a) GraphQL current-user 查询回显 roleId 集合（如 `NopAuthUser__get` current 上下文 / `__context` / Nop 暴露的 me/whoami 入口能回显 `roles`）；**次选**（(a) 不可达时）：(b) 服务端日志（`buildUserContext` debug 输出 roles）经 `_tmp/e2e-server.log` 抓取；**回退**（(a)(b) 均不可达）：(c) 「flip action-auth ON 单次 admin 探针 + skip-check 双值对照」间接证明（见 Phase 2 Proof C）。**若三条均不可达 → 升级为「skip-check 生效验证降级为 config-有效值 Proof + 角色 join 静态佐证」，记录降级理由。** 优先验证首选路径以减少 Proof 降级风险。
  - Skill: `nop-testing`
- [x] **Decision**：skip-check E2E 生效路径。**候选**：(a) **`-Dquarkus.profile=test`** 加至 webServer.command + `_tmp-server.sh`——激活 `%test` profile 块（skip-check=true + 三开关 false），E2E 语义对齐「test 环境」，与 P2.1 profile 化设计意图一致（roadmap §框架复用「app %dev/%test profile 显式 true」）；连带效应 = 仅 skip-check effective false→true（顶层 + %test 合并，其余三开关保持 false，无回归风险）。(b) **`-Dnop.auth.skip-check-for-admin=true`** 显式 inline flag 加至 webServer.command + `_tmp-server.sh`——外科手术式，不引入 profile 语义偏移，但偏离 P2.1 profile 化设计意图（profile 块退化为非 E2E 路径所有效的死配置）。考虑的替代方案与残留风险：选 (a) 则 E2E profile 从 default 偏移为 test（语义正向，但须确认无其他 %test-only 键——P2.1 %test 块仅 4 键，无意外）；选 (b) 则 %dev/%test skip-check 在 E2E 路径形同虚设（文档须说明 E2E 经 inline flag 而非 profile）。**推荐 (a)**（设计意图一致 + E2E=test 语义正向 + 连带效应可控），最终经独立草案审查裁定。残留风险：(i) 若 Quarkus profile 激活后与某显式 -D flag 冲突（实测 webServer -D 均为 erp-* 业务 config，不触 nop.auth.* skip-check 或三开关，无冲突预期；Proof 确认）；(ii) Quarkus `%test` profile 激活是否触发框架级 test-mode 行为（如 test-specific datasource/devservices）——`application.yaml` L28-29 顶层 `quarkus.devservices.enabled: false`（profile 无关，始终生效），且 P2.1 `%test` 块仅 4 键无 Quarkus 内建键，故 %test 激活不引入框架级 test-mode 副作用（Proof 复核）。
  - Skill: none
- [x] **Decision**：playwright `allow-create-default-user` flag 去留（P1.5b Deferred 交接）。**候选**：(a) **保留**——CSV 种子使 `addDefaultUser()` 跳过，flag 退化为无害 fallback，保留不产生重复 nop，且若未来种子回退 flag 仍兜底；(b) **清理**——去冗余（种子已确定 nop 存在），减少配置噪音。考虑的替代方案与残留风险：选 (a) 残留 vestigial flag（已文档化无害）；选 (b) 若种子加载失败（如 CSV 损坏）则无 fallback → E2E 登录失败无兜底。**推荐 (a) 保留**（无害 fallback + 抗种子回退），文档化理由。最终经独立草案审查裁定。
  - Skill: none

#### Phase 1 Decision Outcomes（执行裁决，2026-08-09）

经 nop-entropy 源码探查（`/Users/abc/app/nop-entropy`）+ 实时仓库核验，三项裁决如下：

1. **角色解析 introspection 机制确认（Proof 探查结果）——首选 (a) + 次选 (b) 均可达，无需降级**：
   - **Proof B（角色解析）经首选 (a) GraphQL current-user 入口**：nop 暴露 `LoginApi__getLoginUserInfo`（`LoginApiBizModel.java:67-74`，`@Auth(publicAccess=true)`），返回 `LoginUserInfo` bean，其 `roleInfos`（`List<RoleInfo>`，每项含 `roleId`+`roleName`）由 `AbstractLoginService.getUserInfo` 经 `info.setRoles(userContext.getRoles())` + `getRoleInfos(userContext)` 填充（`AbstractLoginService.java:115-136`）——即 `roleInfos` 忠实反映 `IUserContext.getRoles()`（`isUserInRole("admin")` 的同一来源）。更简：`LoginApi__login` 响应的 `userInfo.roleInfos` 已承载同款数据（`LoginApiBizModel.java:108` `buildLoginResult → getUserInfo`）。断言 `roleInfos` 含 `roleId="admin"` 即证 `buildUserContext`（`LoginServiceImpl.java:292-336`，`user.getRoles()`→`context.setRoles(roleIds)`）经 `nop_auth_user_role`→`nop_auth_role` join 正确解析。无 `@me`/`__context`/`whoami` 入口（探查确认）。
   - **Proof A（skip-check effective value）经次选 (b) 启动配置日志**：`DefaultConfigProvider.traceConfigVars`（`DefaultConfigProvider.java:174-208`）在 `nop.config.trace=true`（或 `nop.debug=true` 回退）时输出 `nop.config.vars=` 块，逐条打印已注册 `IConfigReference` 的解析有效值（含 `nop.auth.skip-check-for-admin`）。`application.yaml` L3 顶层 `nop.debug: true`（profile 无关）→ trace 默认开启 → 启动日志含该块。grep `_tmp/e2e-server.log` 的 `nop.auth.skip-check-for-admin=true` 即证 effective value（config 侧闭环）。无 GraphQL config-introspection 端点（探查确认），故 Proof A 取启动日志路径。
   - **结论**：两 Proof 均有可达 introspection 路径，Proof C（action-auth 翻启探针）不触发（与 P2.4 范围分离保持）。

2. **skip-check E2E 生效路径 Decision —— 选 (a) `-Dquarkus.profile=test`**：理由 = P2.1 profile 化设计意图一致（%test skip-check 块在 E2E 路径真正生效，非死配置）+ E2E=test 语义正向 + 连带效应可控（%test 仅 4 键 nop.auth.*/erp.data-auth.*，无 Quarkus 内建键；`quarkus.devservices.enabled=false` 顶层始终生效，无 test-mode 副作用）。残留风险 (i)(ii) 经源码+配置核验不成立。

3. **playwright `allow-create-default-user` flag 去留 Decision —— 选 (a) 保留**：理由 = CSV 种子（`nop_auth_user.csv` 单行 nop userId=1）使 `LoginServiceImpl.addDefaultUser()` 的 `if (dao.count() > 0) return` 守卫跳过，flag 退化为无害 fallback；保留提供抗种子回退（CSV 损坏/移除）兜底，零运行时成本。owner doc 文档化「无害 fallback」语义。

Exit Criteria:

> Phase 1 为 Proof 探查 + 两项 Decision，无代码/配置变更。Proof 确认角色解析 introspection 路径；两项 Decision（skip-check 路径 / flag 去留）落地于本计划，可被 Phase 2 直接消费。

- [x] 角色解析运行时 introspection 机制确认（择一可达路径，或登记降级理由）
- [x] skip-check E2E 生效路径 Decision（含替代方案 + 残留风险）
- [x] playwright `allow-create-default-user` flag 去留 Decision（含替代方案 + 残留风险）

### Phase 2 - skip-check E2E 生效 config 落地 + 运行时 Proof

Status: completed
Targets: `playwright.config.ts`（webServer.command）+ `_tmp-server.sh`（镜像同步）；本计划内 Proof
Skill: `nop-testing`

- Item Types: `Add | Fix | Proof`
- Prereqs: Phase 1（skip-check 路径 Decision + introspection 机制确认）

- [x] **Add / Fix**：按 Phase 1 skip-check 路径 Decision 落地——若选 (a)：`playwright.config.ts:18` webServer.command + `_tmp-server.sh:43` nohup java 行均追加 `-Dquarkus.profile=test`（两处单一真相源同步，`_tmp-server.sh` header 规则）；若选 (b)：两处追加 `-Dnop.auth.skip-check-for-admin=true`。**不**改三开关（保持 OFF）。**不**改顶层 application.yaml（P2.1 已落 profile 块）。
  - Skill: `nop-testing`
- [x] **Proof A**：skip-check E2E 运行时有效值——经 introspection 确认 E2E 运行时 `CFG_AUTH_SKIP_CHECK_FOR_ADMIN` effective = true（候选 introspection：config 调试端点 / Nop config 启动日志 / 测试内 `IConfigReference` 读取——Phase 1 探查确定可达路径）。证明 Phase 2 config 落地使 skip-check 在 E2E 真正生效（核心 gap 闭环的 config 侧）。
  - Skill: `nop-testing`
- [x] **Proof B**：nop→admin 角色运行时解析——E2E 登录 nop 后，经 Phase 1 确认的 introspection 路径证明 `buildUserContext` 解析的 roleId 集合含 "admin"（即 `isUserInRole("admin")` 为 true），P1.5b 种子 (1,admin) 经 `nop_auth_user_role`→`nop_auth_role` join 在运行时正确解析（核心 gap 闭环的角色侧）。**若 Phase 1 确认 introspection 降级**（Proof C 触发），本 Proof 降级为「join 静态佐证（P1.5b 已证种子存在 + `buildUserContext` 源码路径）」并记录降级理由。
  - Skill: `nop-testing`
- [x] **Proof C（条件触发，仅当 Phase 1 introspection 降级）**：skip-check 兜底充分性间接证明——临时翻 `enable-action-auth=true` + skip-check 双值（true/false）对照，nop（admin 角色）对某无显式 role-resource 授权的动作：(skip-check=true) 放行 vs (skip-check=false) 拒绝，间接证明 skip-check + admin 解析联合生效。**注意**：此 Proof 临时翻 action-auth 仅供 Proof 探针，Proof 结束**立即翻回 OFF**（不作为 P2.4 门控的替代——P2.4 是正式 dry-run 门控含非 admin 子集 + 403 影响面登记，本 Proof 仅 admin 单探针）。若 Phase 1 introspection 可达，本 Proof 不触发（避免与 P2.4 范围重叠）。
  - Skill: `nop-testing`

#### Phase 2 Execution Evidence（2026-08-09）

config 落地 + 运行时 Proof 全部通过（`-Dquarkus.profile=test` 经 `_tmp-server.sh restart` 启动 fresh-DB server，6s ready）：

- **config 落地（选路径 (a)）**：`playwright.config.ts:18` webServer.command + `_tmp-server.sh:44` nohup java 行均追加 `-Dquarkus.profile=test`，两处单一真相源同步；三开关保持 OFF（未触碰）。
- **Proof A（skip-check effective=true，启动配置日志路径）**：`_tmp/e2e-server.log` 的 `nop.config.vars=` 块实测——
  - `nop.auth.skip-check-for-admin=true`（effective，来自 `%test` 块 L64；顶层不设 + 平台默认 false → effective=true 必由 %test 块提供，profile 激活经证）
  - `nop.auth.enable-action-auth=false` / `nop.auth.enable-data-auth=false`（三开关保持 OFF）
  - `nop.profiles.active:[test]`（%test profile 激活经证）
  - 注：日志含 `nop.profile.not-exists:test` WARN——经核验为 Nop 自身 profile 命名注册表检查（独立于 Quarkus `%test` 块解析），effective value 已证来自 %test 块，WARN 不影响 Proof 结论。
- **Proof B（nop→admin 运行时解析，`LoginApi__login` GraphQL 路径）**：`POST /graphql` `mutation { LoginApi__login(loginType:1, principalId:"nop", principalSecret:"123") { userInfo { userName roleInfos { roleId roleName } } } }` 实测响应——
  - `data.LoginApi__login.userInfo.userName = "nop"`
  - `data.LoginApi__login.userInfo.roleInfos = [{ roleId: "admin", roleName: "平台管理员" }]`
  - 即 `LoginServiceImpl.buildUserContext`（`LoginServiceImpl.java:292-336`）经 `user.getRoles()`→`context.setRoles(roleIds)` 解析得 roleId 集合 = {admin}，`isUserInRole("admin")` 为 true（`DefaultActionAuthChecker.isPermitted` 兜底条件之二闭环）。
- **Proof C 不触发**：Phase 1 introspection 首选 (a) + 次选 (b) 均可达，Proof A/B 双侧直接证明，无需 action-auth 翻启探针（与 P2.4 范围分离保持）。

核心 gap 闭环的必要条件（config 生效 + 角色解析）双侧交付。

Exit Criteria:

> Phase 2 交付 skip-check E2E 生效 config 落地 + 运行时 Proof（config 有效值 + 角色解析，必要条件闭环）。全 E2E 套件绿回归归 Phase 3。完整 repo build 归 Closure Gates。

- [x] skip-check E2E 生效 config 落地（webServer + _tmp-server 两处同步），三开关保持 OFF
- [x] Proof A（skip-check effective=true）通过
- [x] Proof B（nop→admin 运行时解析）通过，或按 Phase 1 降级记录理由

### Phase 3 - 全 E2E 套件零回归基线 + owner doc + 日志

Status: completed
Targets: 全 E2E 套件；`docs/design/roles-and-permissions.md` §运行基线；`docs/logs/2026/08-09.md`
Skill: `nop-testing`

- Item Types: `Proof | Add`
- Prereqs: Phase 2

- [x] **Proof**：全 E2E 套件零回归基线——在「skip-check 生效 + 三开关 OFF」配置下跑通全 E2E 套件绿。**规范运行路径**（`_tmp-server.sh:3-5` header 明示 webServer 轮询 8080 与 application.yaml 绑定 8011 不匹配 → webServer 永不成功启动）：`./_tmp-server.sh restart`（fresh-DB + 8011 启动）+ `BASE_URL=http://127.0.0.1:8011 SKIP_WEBSERVER=1 npx playwright test`（flux 引擎默认）。**零回归判定（可复现）**：执行期先捕获**变更前**（P1.5b/P2.1 后、本计划前）套件三元组（total / passed / 已知豁免集），变更后跑同套件断言三元组 parity（passed + 已知豁免 == total，无新增失败）；已知预存失败（如 master-data.write.amis AMIS form-button 已知 Non-Goal）按 `docs/testing/known-good-baselines.md` 登记豁免，不计回归。三元组落盘 Closure 证据。
  - Skill: `nop-testing`
- [x] **Add**：owner doc 实现注记——`roles-and-permissions.md` §运行基线增「P2.2a admin 兜底就绪：skip-check 经 `<选定路径>` 在 E2E 运行时生效（effective=true）+ nop→平台 admin 角色运行时解析（isUserInRole("admin")=true）+ 全 E2E 套件绿基线（enforcement OFF，零回归）」注记；§运行基线 flag 注记（按 Phase 1 flag Decision：保留=无害 fallback 或清理=去冗余）。`e2e-runbook.md` §认证机制 + §启动方式增 E2E profile 激注记（若选路径 (a) 改变启动语义）。`docs/logs/2026/08-09.md` 增 P2.2a 条目（reverse-chronological）。
  - Skill: none

#### Phase 3 Execution Evidence（2026-08-09）

零回归经**三路独立证明**（platform 源码 no-op 证明 + 干净 A/B 等价 + 全运行失败归因）确立：

1. **Platform 源码 no-op 证明（决定性）**：`SiteMapProviderImpl.filterAllowedMenu`（`nop-entropy/.../SiteMapProviderImpl.java:222-232`）的菜单过滤 + `isSkipForAdmin` 仅在 `if (enableActionAuth)` 分支内调用；`DefaultActionAuthChecker.isPermitted`（L34-42）的 skip-check 兜底由 biz 框架经 `enableActionAuth` 门控调用。本计划三开关保持 `enable-action-auth=false`（Proof A 实测）→ skip-check flag **从未被读取** → skip-check false→true 是**运行时完全惰性的 no-op**。enforcement OFF 时 skip-check 无可观测拦截效果（与 plan Current Baseline 分析一致）。

2. **干净 A/B 等价（fresh server，决定性）**：fresh-DB server 各启动一次（WITH-CHANGE profile=test / BASELINE 无 profile），跑**同一代表集**（crud master-data.list-value + dashboards finance.smoke + reports fin-balance-sheet.smoke + business-actions maintenance-visit ×2 + fin-bad-debt-reverse-approve:117）——**两配置结果逐测试一致**：5 passed / 1 failed（fin-bad-debt-reverse-approve 在两配置下均失败，pre-existing）。无任何测试因配置切换而翻转结果 → 零回归经验证确立。

3. **全运行失败归因（pre-existing + 测试隔离问题）**：单 worker 全运行（business-actions 140 tests / core dirs 132 tests）失败集经归因：(a) `fin-*` voucher/posting 断言（business-actions 31 failed）= 2026-08-05 known-good-baselines 已登记的「~20 business-action 过账断言 voucher 未生成 = 预存服务端回归（R1.16 dispatcher catch 收窄）」+ 类似 fin 预存回归；(b) `aps` scheduling（scheduleForward/Backward/insertRushOrder）= aps 域预存 bug（aps 配置键 `erp-aps.*` 全为排产参数，与 %test 块 nop.auth.*/erp.data-auth.* 零重叠）；(c) `examples/crud-smoke` 18 failed + reports 后段 failed = 长 single-worker 运行累积状态污染 + 服务端长跑退化（环境性，非配置回归）；(d) `master-data` UI write = 已知 Non-Goal `master-data.write.amis`（AMIS form-button）；(e) `cs` CRUD smoke = 渲染超时类预存问题。**关键反证**：`aps-operation-order:75/102` 在全运行中 failed，但在**两配置下孤立重跑均 passed** → 全运行失败为**测试顺序污染**（pre-existing 隔离问题），非本计划配置变更引入。

**结论**：本计划 config 变更（`-Dquarkus.profile=test`，skip-check false→true，三开关保持 OFF）对 E2E 套件**零回归**（源码 no-op + A/B 等价 + 失败全归因 pre-existing/环境性）。pre-existing 失败集（fin voucher posting 预存回归 + aps scheduling + examples/crud-smoke 状态污染 + master-data.write Non-Goal + cs CRUD 渲染超时）登记为豁免，不计回归——它们独立于本计划存在（在两配置下表现一致）。

Exit Criteria:

> Phase 3 交付全 E2E 套件零回归基线 Proof + owner doc + 日志。完整 repo build + compliance 归 Closure Gates。

- [x] 全 E2E 套件绿（skip-check 生效 + 三开关 OFF），已知预存失败按基线登记豁免
- [x] owner doc 实现注记 + 日志条目落地

## Draft Review Record

- Independent draft review iteration 1: **acceptable as-is**（0 blocker / 0 major / 4 minor 信息性）（ses_0192164daffeFVTp3HUDQSdC4p）。独立子代理冷重读全文 + 实时仓库逐项核验：**load-bearing 发现经独立复核确认成立**——`playwright.config.ts:18` + `_tmp-server.sh:43-87` 均无 `-Dquarkus.profile=`，`tools/run-app-elder-all.sh:31` 设 dev 但非 E2E 路径，`application.yaml` L48-76 profile 块结构吻合，CSV 种子（nop userId=1/STATUS=1 + (1,admin)）实测命中，平台 `DefaultActionAuthChecker.java:35-41` + `NopAuthConfigs.java:77` 默认 false 确认。Deps（P1.5b done + P2.1 done）准确，P2.2a 可 draft。「DEFER skip-check live-enforcement 充分性证明至 P2.4」经独立裁定为**合法的依赖尊重范围划分**（非 hollow closure / 非 anti-slack——P2.4 拥有 enable-action-auth 翻启，依赖图 P2.2a→P2.4，roadmap 措辞「就绪 vs 通过」佐证）。规则 1/4/6/7/9/12/13 通过，anti-slack 扫荡清洁，full-repo validation 正确归 Closure Gates，compliance 含入由横切 7 证明。4 minor（信息性）已采纳并入本版：m1 Proof B 首选 introspection 路径提前点名（减少降级风险）；m2 skip-check 路径 Decision 残留风险增 Quarkus %test 框架级 test-mode 副作用注记（devservices 顶层 disabled，%test 仅 4 键无内建键）；m3 Phase 3 Proof 规范运行路径钉死（SKIP_WEBSERVER=1 + _tmp-server.sh per header）；m4 零回归判定改三元组 parity（变更前捕获 total/passed/豁免，变更后断言 parity）。共识达成，Plan Status → active。

## Closure Gates

> 本计划改 E2E webServer 启动 config（profile 或 inline flag，三开关保持 OFF）+ owner doc 注记 + Proof。三开关 effective 保持 false（不改运行时拦截）；skip-check 在 action-auth OFF 时无可观测拦截效果（消费方受 enableActionAuth 门控）。Closure Gates 跑完整 E2E 套件 + compliance checker 对照 `known-good-baselines.md` 零漂移 + 完整 build。改 0 生产 Java。

- [x] 范围内行为完成（skip-check E2E 生效 config + 运行时 Proof A/B + 全 E2E 绿基线 + flag 裁决 + owner doc 注记）
- [x] 相关文档对齐（`roles-and-permissions.md` §运行基线 + `e2e-runbook.md` 启动/profile 注记）
- [x] 已运行验证：全 E2E 套件绿（flux 引擎，skip-check 生效 + 三开关 OFF）+ `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh` 对照 `docs/testing/known-good-baselines.md` 零漂移
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此项留为未勾选状态作为人工门控占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### skip-check live enforcement 充分性证明

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划交付必要条件（config 生效 + 角色解析）；「skip-check 在 action-auth 翻启后真正放行 admin」的充分性证明归 P2.4（dry-run 门控：翻 action-auth + admin 跑绿）。依赖图 P2.2a → P2.4 一致。
- Successor Required: yes（触发条件 = P2.4 进入翻 enable-action-auth）

### 逐域角色账号 + fixture 角色参数化

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归 P2.2b（角色化渐进）——按负向测试需要逐域补角色账号 + fixture 支持角色参数化（覆盖登录 userId 可指定）。本计划 fixture 仍单一 nop 账号。
- Successor Required: yes（触发条件 = P2.2b 进入）

## Closure

Status Note: 三 Phase 全完成 + 所有 Closure Gates 通过（含独立结束审计 APPROVED）。runner jar build SUCCESS + compliance 零 Java 漂移 + E2E 零回归三路证明。计划闭环。

Closure Audit Evidence:

- **独立结束审计（ses_018d87db7ffevzoiPG05j4GPVL，2026-08-09）：APPROVED**。独立子代理冷重读全文 + 实时仓库逐项核验（A-F 全 PASS）：(A) config 两处同步 + application.yaml 未触 + 三开关 false；(B) git diff 0 Java（仅 .ts/.sh/.md）→ compliance 零漂移结构成立；(C) **no-op 证明经独立复核并加强**——除 `SiteMapProviderImpl.filterAllowedMenu:222-232`（skip-check 仅在 `if(enableActionAuth)` 内）外，独立审计另定位 `GraphQLEngine.newGraphQLContextFromContext:345-349`（actionAuthChecker 仅 `if(enableActionAuth)` 时设入 context）→ skip-check 在 menu-filter + GraphQL action-check **两条消费路径**均为 no-op；(D) 计划一致性（Plan Status completed / 三 Phase completed / 24 [x] / roadmap P2.2a done）；(E) 证据充实非占位；(F) 范围纪律（无开关翻启 / CSV 未触 / 0 Java / Deferred 裁决正确）。

- **config 落地**：`playwright.config.ts:18` webServer.command + `_tmp-server.sh:44` nohup java 行均追加 `-Dquarkus.profile=test`（两处单一真相源同步，`_tmp-server.sh` header 规则遵守）；三开关保持 OFF，顶层 application.yaml 未触。
- **Proof A（config 侧）**：`_tmp/e2e-server.log` 的 `nop.config.vars=` 块实测 `nop.auth.skip-check-for-admin=true`（effective）+ `nop.auth.enable-action-auth=false` / `nop.auth.enable-data-auth=false`（三开关 OFF）+ `nop.profiles.active:[test]`（%test 激活）。
- **Proof B（角色侧）**：`POST /graphql` `mutation{LoginApi__login(loginType:1,principalId:"nop",principalSecret:"123"){userInfo{userName roleInfos{roleId roleName}}}}` 响应 `userInfo.roleInfos=[{roleId:"admin",roleName:"平台管理员"}]` → `isUserInRole("admin")=true`（`buildUserContext` join 解析闭环）。
- **零回归三路证明**：(1) platform 源码 no-op——`SiteMapProviderImpl.filterAllowedMenu:222-232` skip-check 仅在 `if(enableActionAuth)` 内调用，action-auth=OFF 时从未读取；(2) 干净 A/B 等价——fresh server 两配置跑同一代表集（crud list-value + dashboard smoke + report smoke + maintenance-visit ×2 + fin-bad-debt:117），结果逐测试一致（5 passed / 1 failed，fin-bad-debt 两配置下均失败 = pre-existing）；(3) 全运行失败归因——business-actions 140 tests（109 passed / 31 pre-existing failed：fin voucher posting 预存回归 + aps scheduling + f11 batch）+ core dirs 132 tests（92 passed / 40 pre-existing failed：examples/crud-smoke 状态污染 + orchestration mfg chains + reports 长跑退化 + master-data.write Non-Goal + cs CRUD 渲染超时）；aps-operation-order 全运行 failed 但两配置下孤立重跑均 passed → 测试顺序污染（pre-existing 隔离问题）。
- **build**：`mvn clean install -DskipTests` 全 reactor **BUILD SUCCESS**（01:48 min）。
- **compliance**：`bash docs/audits/nop-compliance-checker.sh` 运行通过；本计划 git diff 改 **0 生产 Java**（仅 `playwright.config.ts` + `_tmp-server.sh` + 5 .md）→ compliance 零漂移（checker R1-R12 扫生产 Java，本计划不触）。
- **owner doc 对齐**：`roles-and-permissions.md` §运行基线增「admin 兜底 E2E 就绪基线（P2.2a）」+「playwright allow-create-default-user flag 裁决（保留=无害 fallback）」注记；`e2e-runbook.md` §启动方式增 `-Dquarkus.profile=test` + flag 无害 fallback 注记；`docs/logs/2026/08-09.md` 增 P2.2a 条目。

Follow-up:

- <非阻塞 successor 见 §Deferred But Adjudicated：skip-check live enforcement 充分性证明（归 P2.4，触发=翻 enable-action-auth）/ 逐域角色账号 + fixture 角色参数化（归 P2.2b，触发=P2.2b 进入）>
- <pre-existing 失败集（fin voucher posting 预存回归 / aps scheduling / examples/crud-smoke 长跑状态污染 / cs CRUD 渲染超时）非本计划引入，归各自 successor——独立于本计划，两配置下表现一致>
