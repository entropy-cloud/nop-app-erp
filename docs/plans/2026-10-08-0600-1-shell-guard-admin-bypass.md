# Plan: 壳层路由守卫补平台 admin 旁路（跨仓 nop-chaos-next）

- **Status**: active
- **Date**: 2026-10-08
- **Type**: 跨仓库代码修改（nop-chaos-next，保护区：外部仓库代码 = auto + dual-agent-approval）
- **Owner doc**: `docs/design/roles-and-permissions.md` §角色体系 L39/L166（管理员 = 平台 superuser，dev/test 拥有全系统访问，经 `nop.auth.skip-check-for-admin` 兜底）；平台服务端 `SiteMapProviderImpl.isSkipForAdmin`（roles 含 `admin`/`nop-admin` → 跳过全部菜单/动作过滤）

## 1. 背景与证据

2026-10-08 浏览器实测（start-app.sh dev profile，fresh-DB，nop/123 登录）：

- 登录成功、角色解析正确（`auth:v2` user.roles = `["admin","user"]`，nop-chaos-next `055781a` roleInfos 回退修复已随 nop-entropy 22:41 前端重建生效）。
- 看板/凭证页正常渲染（fin-dashboard-main KPI 非空、inv-dashboard-main、ErpFinVoucher-main）。
- **业务角色 scoped 的 CRUD 页面对 admin 统一 403**：`ErpMdPartner-main`/`ErpMdMaterial-main`/`ErpMdCurrency-main` 等渲染「403 无权限访问——当前角色集合不允许访问此路由」。
- E2E 复现：`E2E_ENGINE=flux SKIP_WEBSERVER=1 npx playwright test tests/e2e/crud/master-data.smoke.spec.ts` → 1 failed（页内 403，`.nop-crud` 永不可见）。

根因链（三层事实）：

1. 域级手写 action-auth 菜单种子按 owner doc P1.5a 挂 13 业务角色（`roles="采购员,…,维护人员"`），**平台 admin 不在种子内**——这是设计（§L42「平台 admin 也不依赖 roles 种子（它走兜底）」）。
2. 服务端 admin 兜底：`SiteMapProviderImpl.filterAllowedMenu` → `isSkipForAdmin(roleIds)` 为 true 时跳过 `applyAuthFilter`，站点地图全量返回（含角色元数据）。
3. **壳层客户端守卫无 admin 概念**：`packages/core/src/hooks/usePermissionGuard.ts` 与 `packages/shared/src/utils/menu.ts filterMenusByRoles` 均为字面交集 → admin（roles=[admin,user]）对 13 业务角色路由求交为空 → 403/菜单剔除。owner doc 承诺的「admin dev/test 全系统访问」在浏览器层断裂。

无在途修复：nop-chaos-next HEAD `59e63c8` 守卫源码与已服务资产（host-app-state-*.js.gz）均无 admin 旁路；nop-entropy 工作树 clean。

## 2. 变更面（nop-chaos-next，最小 diff）

1. `packages/core/src/hooks/usePermissionGuard.ts`：`userRoles` 含 `admin` 或 `nop-admin` 时恒返回 true（镜像服务端 `isSkipForAdmin` 字面角色名）。
2. `packages/shared/src/utils/menu.ts` `filterMenusByRoles`：同一旁路（admin 用户菜单全量保留，语义=服务端对 admin 跳过过滤）。
3. 两个文件的既有测试文件各追加 admin 旁路用例（正向：admin 放行 role-scoped 路由/菜单；负向回归：非 admin 用户字面交集行为逐字不变）。

不改动：服务端（nop-entropy Java 零变更）、菜单/角色种子（owner doc 双命名空间注记禁止给 admin 补种子）、mock 形态（`{value}`/mock auth 用例保持）。

## 3. 验证与生效链

1. nop-chaos-next：`packages/core`、`packages/shared` 单测绿（vitest）。
2. `apps/main` 生产构建：`env -u VITE_ENABLE_MOCK pnpm build`（含 tsc --noEmit 门禁）。
3. `bash scripts/sync-site.sh`（guard 拒绝 mock dist；产物含 nop-shared import map）。
4. nop-entropy：`mvn install -pl nop-frontend-support/nop-web-site`（更新 .m2）。
5. nop-app-erp：`mvn clean install -DskipTests -pl app-erp-all` + `./scripts/start-app.sh start`。
6. 浏览器复验：nop/123 登录 → `ErpMdPartner-main`/`ErpMdMaterial-main` 列表渲染（.nop-crud 可见）+ fin-dashboard-main/ErpFinVoucher-main 零回归。
7. E2E：crud/master-data.smoke.spec.ts 红→绿；negative/e1-2-menu-filter.smoke.spec.ts 全绿（非 admin 角色过滤语义零回归）。
8. 旁证：已构建资产 grep 含 admin 旁路字面量。

## 4. 保护区审批（auto + dual-agent-approval）

- [x] 审批代理 A（fresh session，plan-audit 视角）：**VERDICT: GRANT**（agent_483f12e8-5c33-4f89-8ec1-09f20d9cbb6e，2026-10-08）——条件 C-1..C-4 并入：C-1 客户端旁路无条件 vs 服务端配置门控（%prod=false）的不对称性须显式登记；C-2 字面精确匹配 `admin`/`nop-admin` + 负向用例（业务角色「管理员」中文字面不得触发旁路）；C-3 登记 nop-chaos-next 既有脏文件基线（`docs/logs/flux-sync/2026/10-05.md`、`libs/nop-chaos-flux-0.1.0.tgz`、`pnpm-lock.yaml`——与本计划无关，Closure 对照时排除）；C-4 E2E 证据写明确切调用形态。
- [x] 审批代理 B（fresh session，独立开放面复核）：**VERDICT: GRANT**（agent_34e6aa89-37a1-4e5e-ad0b-3f96c64cbaf6，2026-10-08）——条件 M-1..M-5 并入：M-1 旁路在两函数体内实现（禁 `filterMenusByRoles` 早退裸数组、禁调用点特判；第三处 `amis-core transform.ts:49 xui:roles` 门经核实对本 app 惰性）；M-2 字面量单源化于 packages/shared 常量（镜像 `NopAuthConstants.ROLE_ADMIN/ROLE_NOP_ADMIN`），core import 之；**`usePermissionGuard.test.ts` 顶部手抄 checkPermission 镜像必须同步更新否则静默假绿**；M-3 登记 sync-site 会改写 nop-entropy git 跟踪的 web-site 资产（哈希文件名增删）为预期变更面且须提交；M-4 配置门控分歧落盘 owner doc §运行基线注记（随 Closure）；M-5 验证链写明具体命令，e1-2 仅覆盖服务端 REST（客户端非 admin 回归由单测负向用例承重）。
- 非阻塞建议（登记）：S-1 nop-app-erp E2E 无 UI 层 ForbiddenPage 负向断言（successor 候选）；S-2 服务端 `containsRole` 的 `user` 角色恒放行特例客户端未镜像（既有分歧、无害，随 M-4 注记）。

两批准均 GRANT，依 2026-08-15 保护区域裁决（auto + dual-agent-approval）实施。

## 5. Execution Record

- [x] 实施两文件 + 测试：
  - `packages/shared/src/utils/menu.ts`：新增 `PLATFORM_ADMIN_ROLES` + `hasPlatformAdminRole`（shared 单源，注释含 C-1 门控不对称性）；`filterMenusByRoles` 函数体内 `adminBypass` 并入 `selfAllowed`（M-1 无早退裸数组，结构语义保持）。
  - `packages/core/src/hooks/usePermissionGuard.ts`：逻辑抽出为纯函数 `evaluatePermission`（消灭既有测试的手抄镜像，M-2），hook 经 `useMemo` 消费；`hasPlatformAdminRole` 从 `@nop-chaos/shared` 导入。
  - `packages/shared/src/index.ts`：补两符号导出。
  - 测试：`usePermissionGuard.test.ts` 7/7（含平台 admin 放行 + 业务「管理员」中文字面不触发旁路 + 非 admin 字面交集逐字不变）；`menu.test.ts` 41/41（新增 platform admin bypass describe 6 用例；1 个既有用例的用户角色 `admin`→`manager` 适配旁路语义并注记原因——该用例原以 admin 作普通字面角色）。
- [x] 验证链全绿留证（C-4 命令形态如实登记）：
  1. `npx vitest run src/hooks/usePermissionGuard.test.ts`（packages/core）= **7 passed**；`npx vitest run src/utils/menu.test.ts`（packages/shared）= **41 passed**。
  2. `apps/main`：`env -u VITE_ENABLE_MOCK pnpm build` = EXIT 0（tsc --noEmit 门禁 + vite 生产构建 + import map 注入，layer violations none）。
  3. `bash scripts/sync-site.sh` = 通过 mock-dist guard，资产落 `nop-entropy/nop-frontend-support/nop-web-site/.../META-INF/resources/`。
  4. nop-entropy：`mvn install -pl nop-frontend-support/nop-web-site -DskipTests` = EXIT 0（.m2 06:29）。
  5. nop-app-erp：`mvn clean install -DskipTests -pl app-erp-all` = EXIT 0（runner 内 `pkg-shared-*.js.gz` 解压 grep 实证含 `nop-admin` 字面）+ `./scripts/start-app.sh start` = 13s ready（dev profile，fresh-DB）。
  6. 浏览器复验（dev profile live server，nop/123）：`auth:v2` user.roles=`["admin","user"]`；**8/8 核心页面渲染零 403/404**：ErpMdMaterial-main（CRUD 表格完整）/ ErpMdPartner-main / fin-dashboard-main（KPI 非空）/ inv-dashboard-main（KPI 非空）/ ErpFinVoucher-main / ErpPurOrder-main / ErpSalOrder-main / ast-dashboard-main。IAB 合成点击在该按钮上存在输入管道怪癖（多次点击被吞；页内程序化 click 触发正常；E2E Playwright-Chrome 同 bundle 登录正常——非本计划变更引入，登记为测试基建观察项）。
  7. E2E（playwright webServer 自启 %test 服务器，**非 SKIP_WEBSERVER**）：`E2E_ENGINE=flux npx playwright test tests/e2e/crud/master-data.smoke.spec.ts tests/e2e/negative/e1-2-menu-filter.smoke.spec.ts` = **6 passed（57.0s）**——crud master-data 修复前红（403 致 `.nop-crud` 不可见）→ 绿；e1-2 五断言全绿（非 admin 角色过滤语义零回归；其只覆盖服务端 REST，客户端非 admin 回归由单测负向用例承重，M-5）。注：SKIP_WEBSERVER 对 dev-profile live server 跑 e1-2 会 5 失败（%dev `enable-action-auth=false` 全量菜单语义 ≠ %test 过滤语义）——为测试载体错配非回归，登记避免误读。
  8. 旁证：runner 资产含 `nop-admin`（见第 5 步）；sync 后 nop-entropy 资产 grep `nop-admin` 命中 `pkg-shared-kp33Gdod.js.gz`。
- [x] C-3 基线登记：nop-chaos-next 工作树存在**与本计划无关的既有脏文件**：`docs/logs/flux-sync/2026/10-05.md`、`libs/nop-chaos-flux-0.1.0.tgz`、`pnpm-lock.yaml`（他会话在途产物）——Closure 对照变更面时排除。
- [x] M-3 变更面登记：本计划落地后 nop-entropy 工作树 diff = **38 个 `nop-frontend-support/nop-web-site/src/main/resources/META-INF/resources/` 下 git 跟踪资产**（sync-site 哈希文件名增删，预期面）——Java/源码零变更；该生成资产 diff 须随本计划提交进 nop-entropy 方可复现（**待提交，见 Closure Gates**）。nop-chaos-next 变更面 = 计划声明的 5 文件（2 源 + 2 测试 + 1 index 导出）。

## Closure Gates

- [x] 验证链 1-8 全绿（见 Execution Record）
- [x] 独立结束审计（fresh session）**ACCEPT**——2026-10-08，变更面/批准条件/单测数（7/41 独立复现 + 两包全量 45/45 与 180/180）/双批准/残量登记逐项核实通过；0 Blocker / 0 Major / 2 Minor（并发会话写入的日志+基线文件须并入提交面；shared/dist 落后 src 无功能影响）。审计指针：本会话 agent_fdcd68ec-6b46-42d6-ae0b-a07fe36f9ace
- [ ] 三个仓库变更面提交：nop-chaos-next（5 文件，排除 C-3 既有脏文件）、nop-entropy（38 资产 + 本计划若登记于 nop-app-erp 则跨仓指针）、nop-app-erp（快照修复 + 本计划 + 日志 + 基线 + owner doc 注记）——提交动作待用户确认后执行（工作树为多会话共享面，执行者不擅自 commit）
- [ ] 残量登记（非阻塞）：S-1 UI 层 ForbiddenPage 负向 E2E 断言缺位；S-2 服务端 `containsRole` 的 `user` 特例客户端未镜像（无害）；IAB 登录按钮合成点击吞没观察项
