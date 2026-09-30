# 2026-10-01-0313-1-usc02a-action-auth-prod-gray action-auth 生产灰度 + 菜单/动作完整性收口

> Plan Status: completed
> Last Reviewed: 2026-10-01
> Source: `docs/backlog/user-story-coverage-roadmap.md` USC-02a（US-PL-01 承接项；permissions-enforcement-roadmap prod 翻转 successor 唯一载体）
> Related: `docs/design/roles-and-permissions.md`（:171 P1.5a 注记 / :242 双层声明 / :207 运行基线）、`docs/testing/permissions-enforcement-dry-run-impact.md`
> Audit: required（auth/permissions 保护区：plan-first + owner doc + tests；菜单塌缩设计决策修订另须双独立子代理批准——owner doc :171 明示）

## Current Baseline

- **配置基线**：`app-erp-all/src/main/resources/application.yaml` `%prod` block `enable-action-auth: false`（:81）；`%test`=true（:66，P2.4 翻启）；`%dev`=false。E2E 全套件经 playwright `-Dquarkus.profile=test` 运行（action-auth ON 已被全域负向套件覆盖）。
- **动作层已闭环**：E1.1/E1.2（permissions-enforcement mission，2026-08-10）全域 19 域 approve/reverseApprove xbiz `<auth>` 补齐 + 25 reverseApprove FNPT 补声明，dry-run 61 项子集 bypassed 闭环；9 域 per-action FNPT `roles=` 种子在位（fin 28/pur 16/sal 14/mfg 25/qa 10/mnt 22 等）。
- **菜单层设计缺口（owner doc :171 登记，2026-09-05 运行时实证）**：per-entity SUBM 节点不挂 `roles=`、依赖 FNPT cascade-up；无本角色 FNPT 锚点的域自底向上塌缩（机理已实证并精化：per-entity SUBM 被 `applyAuthFilter` 直接置 DISABLED → 菜单组 SUBM 全子菜单 inactive → `SiteResourceBean.fixStatus()`（nop-entropy `SiteResourceBean.java:350-356`，仅 children 全 menu 且全 inactive 时触发）逐层上传播至 TOPM）。受影响面：管理员（业务）→erp-md、采购员→erp-pur、销售员→erp-sal、作业员/生产计划员→erp-mfg、质检员→erp-qa、维护人员→erp-mnt。
- **缺口回归信号**：`tests/e2e/negative/e1-2-menu-filter.smoke.spec.ts` (e) 断言（:133 采购员见 erp-md TOPM / :135 见 md-material SUBM）为故意红灯（plan 2026-08-25-1956-2 落地的缺口回归信号）；:171 注记称「md 全域 0 FNPT」已部分过时——M2.8（commit 818759a61）为 md 补 8 个 FNPT 种子（SupplierApproval 7 动作→采购员 + Currency refresh→管理员），(e):133 静态推导可能已转绿、:135 仍红，运行时态以执行期实测为准。
- **平台机制实证**（本计划基线盘点，只读核验）：`cascadeResourceToRoles` 仅 child→parent 上行（`SiteCacheDataBuilder.java:221-232`）；`permissionToRoles` 仅聚合 FNPT 资源自身映射（:169-177）——**per-entity SUBM 挂 `roles=` 不改变任何动作授权面**；`isMenu()`=TOPM/SUBM（FNPT 非 menu，per-entity 层不触发 fixStatus 塌缩条件）。
- **%prod 翻转联动缺口（本计划新增盘点发现）**：`%prod` 省略 `nop.auth.skip-check-for-admin` → 平台默认 false（DR-1e 安全姿态，`NopAuthConfigs.java:77`）→ `isSkipForAdmin` 恒 false（`SiteMapProviderImpl.java:241-246`）→ 平台 admin（nop 账号，绑平台 `admin` 角色）下**全部业务域 + erp-sys 自身子树**塌缩（erp-sys 6 菜单组 SUBM 及其下节点均无 roles=/FNPT 种子），仅剩 notify（roles="user"）——与 `app.action-auth.xml:41` erp-sys TOPM `roles="admin"` 的既定意图相悖（顶层种子被塌缩机理架空）。
- **登录种子**：`_init-data/nop_auth_user.csv`（userId 1-21：nop + role-* 业务角色账号，密码 "123"）+ `nop_auth_user_role.csv`（21 角色绑定）为部署期种子；`orm.init-database-data: true` 根配置无 %prod 覆盖 → %prod 探针可用业务角色账号。
- **剩余差距**：%prod 翻转未做；102 个 per-entity/页面型 SUBM 节点无种子（md 28/pur 12/sal 10/mfg 22/qa 17/mnt 13，含 ~25 个页面/看板/向导型节点）；erp-sys 子树无 admin 种子；owner doc :171/:242 未随新事实修订。

## Goals

- 菜单塌缩设计决策落地：per-entity/页面型 SUBM 节点按「镜像父菜单组 `roles=`」规则补静态种子（6 域 102 节点 + erp-sys 子树 admin 种子），恢复 owner doc 「菜单组层覆盖可见性」既定设计意图；FNPT 动作授权面零变更。
- `%prod` 灰度翻转：`enable-action-auth: true`（%prod），skip-check-for-admin 保持平台默认 false（DR-1e 不放松）；平台 admin 可见面 = erp-sys（修复后）+ notify，业务操作须业务角色账号（部署语义文档化）。
- owner doc 修订：`:171` 注记更正（机理精化 + M2.8 时效性修正 + 本决策裁决）+ `:242` 双层声明更新（per-entity 层种子在位）。
- 验证：e1-2-menu-filter (e) 红灯转绿 + negative 全套件零回归 + `%prod` profile 运行时探针（启动守卫 + 拒绝负向 + 菜单过滤 + 业务角色可见性 + 平台 admin RBAC 姿态，四组断言）+ compliance/xdsl 门禁零漂移。

## Non-Goals

- data-auth / role-row-filter 的 %prod 翻转（USC-02b 独立计划持有；本计划仅翻转 enable-action-auth）。
- 平台 `fixStatus`/`applyAuthFilter` 语义变更（涉 nop-entropy 外部仓库，Non-Goal）。
- FNPT 动作授权面扩大（per-entity 种子不触动作权限；query/mutation FNPT 保持 deny-by-default 现状——E1.x 边界）。
- xbiz `<mutation>` 无 `<auth>` 的 bypass 面（doc :248 已实证不因翻转消除，属平台 checker 语义，维持登记）。
- B 类 5 域（CRM/CS/APS/Logistics/DRP）不加种子的 P1.3 姿态不变（%prod 运行时可达语义变化见 D2 裁决）；15 域 data-auth inert stub 不动。

## Task Route

- Type: `implementation-only change`（配置 + 静态种子 + 文档；auth/permissions 保护区 plan-first）
- Owner Docs: `docs/design/roles-and-permissions.md`（修订 :171/:242）
- Skill Selection Basis: `Skill: none`——已对照方法相近项：`nop-backend-dev`（Java BizModel 编写技能，本计划零 Java 变更）、`nop-testing`（E2E 编写技能，本计划零新 spec——红灯转绿 + 既有套件回归）；变更面为 XML 配置种子 + YAML 一行 + 文档 + 验证脚本，无技能匹配；结论 none。

## Infrastructure And Config Prereqs

- `%prod` 探针需本地可运行 runner jar（`app-erp-all/target/app-erp-all-1.0-SNAPSHOT-runner.jar`，先 `mvn clean install -DskipTests` 保证新鲜——lessons/26 stale jar 防线：修正→重装→校验三步串联）。
- 探针用独立端口 + 独立 DB 文件：显式覆盖数据源 `-Dnop.datasource.jdbc-url=jdbc:h2:./db/erp-probe`（根配置 `jdbc-url: jdbc:h2:./db/erp` 无 profile 覆盖，系统属性优先）+ `-Dquarkus.http.port=8081` + 显式 `-Dquarkus.profile=prod`；种子初始化复用 `-Dnop.orm.init-database-data=true`；脚本化于 `tools/verify-prod-auth-gray.sh`（对齐 `validate-flux-fresh.sh` 范式：strict mode + 探针后 teardown 删 `erp-probe.mv.db`/`erp-probe.trace.db` + 失败非零退出）。

## Execution Plan

### Phase 1 — 设计裁决与双独立子代理批准（保护区流程）

Status: completed
Targets: `docs/design/roles-and-permissions.md`（:171/:242 修订）、本计划 Decision 记录
Skill: none

- Item Types: `Decision`

- [x] Decision: **D1 菜单塌缩修复选项**——(a) per-entity/页面型 SUBM 镜像父菜单组 `roles=` 静态种子（采纳）；否决 (b) 补 per-action FNPT 种子（扩大动作授权面，违反最小授权 + SoD——审批类 FNPT 给操作员角色将破坏 SoDGuard 语义）；否决 (c) 平台 fixStatus 语义裁决（nop-entropy 外部仓库，影响全平台，收益不匹配）。理由：菜单可见性 ≠ 动作授权（平台源码实证：permissionToRoles 仅聚合 FNPT 自身映射、级联仅上行），(a) 恢复 P1.5a「菜单组层覆盖可见性」既定意图且零授权面扩散；残留风险（**批准代理 B C3 升级为显式部署语义**）：%prod 全操作（含查询 findPage）经 GraphQLActionAuthChecker enforcement 检查 + query/mutation FNPT 零种子 → 全角色页面数据加载被拒，UX =「菜单壳」（菜单可见、数据 403）；此为 %test 既有姿态（P2.4 起）的 %prod 延伸，经探针第⑤组断言（采购员 ErpPurOrder__findPage → no-permission）固化为已验收部署语义，生产启用时按语义对待非故障。
      Skill: none
- [x] Decision: **D2 %prod 翻转联动**——erp-sys 子树补 `roles="admin"` 种子（恢复 app.action-auth.xml:41 既有意图）；skip-check-for-admin 保持省略（平台默认 false，DR-1e 姿态不放松）；平台 admin 可见面 = erp-sys + notify，业务域须经业务角色账号（部署语义写入 owner doc）。**B 类 5 域裁决（批准代理 B C1 三分法更正）**：crm/cs/drp = 零种子真 fail-closed，%prod 全员不可达（含平台 admin）；aps = 24 FNPT 生产计划员种子、log = 10 FNPT（库管员，销售员×8+管理员×2）级联部分可见——二者 TOPM 虽无 roles= 但对种子角色可达；%prod 下 TOPM 无种子对无种子角色不可达为 DR-1e fail-closed 接受姿态；successor 触发条件 = crm/cs/drp 生产启用或出现敏感操作时补角色种子升格 A 类 / aps、log 补 TOPM/菜单组种子完成域角色化（非「从不可达升格」）。**[accept-agent B C1 修订]**否决替代：prod 设 skip-check-for-admin=true（削弱 DR-1e）；nop 绑定业务角色（混淆双命名空间，P1.5b 已裁决分离）。
      Skill: none
- [x] Decision: **D4 灰度开/关回归口径**——roadmap「既有 E2E 在灰度开/关回归」解读：开 = %test ON 全套件（翻转不触 %test 块）+ %prod 探针；关 = %prod 翻转前基线，自 P2.4 起 E2E 载体无独立关态（%test 恒 ON），不另建关态回归（pre-P2.4 历史绿为证据）。
      Skill: none
- [x] Decision: **D3 每域种子角色清单**——镜像规则：per-entity/页面型 SUBM `roles=` = 其父菜单组 SUBM 现有 `roles=` 值逐字复制（不发明新授权；erp-sys 特例 = 全子树 `roles="admin"`）。各域父菜单组清单以执行期实读 delta 文件为准（md 13 角色 TOPM 系 / pur 采购员 / sal 销售员 / mfg、qa、mnt 按各自菜单组细分）。
      Skill: none
- [x] Proof: **双独立子代理批准**（owner doc :171 明示本设计决策修订须 auto + dual-agent-approval）——两个 fresh-session 子代理分别独立审查 D1/D2/D3/D4 + 本计划 + 平台机理证据链，各自给出 GRANT/DENY 及理由；任一 DENY 则修订重审。批准记录（agent 指针 + 结论）落盘本计划 Draft Review Record 之后的双批准小节。
      Skill: none

Exit Criteria:

- [x] D1/D2/D3/D4 决策落盘本计划（含 A 条件 C1/C2 + B 条件 C1-C5 全部并入）；双独立子代理批准记录落盘（A：GRANT 附 2 条件；B：条件 GRANT 附 4 必须+1 建议；零 DENY，条件落实后批准生效）；owner doc :171/:242 修订稿条款已固化于 Phase 2 执行项（随 Phase 2 提交）。

### Phase 2 — 种子落地 + %prod 翻转 + owner doc 修订

Status: completed
Targets: 6 域 `erp-<short>.action-auth.xml` delta + `app-erp-all/.../auth/app.action-auth.xml`（erp-sys 子树）+ `application.yaml` %prod 一行 + `docs/design/roles-and-permissions.md`
Skill: none

- Item Types: `Add | Fix`

- [x] Add: 6 域 delta 文件 102 个 per-entity/页面型 SUBM 节点补 `roles=`（镜像父菜单组值逐字复制；干跑计数 102 与计划精确一致，逐域 md 28/pur 12/sal 10/mfg 22/qa 17/mnt 13；基座零变更）。
      Skill: none
- [x] Add: `app.action-auth.xml` erp-sys 子树 26 个 SUBM（6 菜单组 + 20 叶节点）补 `roles="admin"`（+TOPM 共 27 admin 引用；可见 ≠ 可用注记入 application.yaml 部署语义注释）。
      Skill: none
- [x] Fix: `application.yaml` `%prod` block `enable-action-auth: false` → `true`（单行 + 部署语义注释块；enable-data-auth 保持 false 归 USC-02b）。
      Skill: none
- [x] Add: owner doc 修订（**批准代理 A C1/C2 + B C2/C5 条款并入**）——`:171` 注记追加裁决段：(a) D1-D4 结论 + 机理精化[fixStatus 触发条件=children 全 menu 全 inactive、per-entity 层由 applyAuthFilter 直接置 DISABLED]；(b) M2.8 时效性更正[md 8 FNPT + ct 3 FNPT 财务员种子致财务员→erp-ct 级联可见面补登]；(c) 塌缩面收敛清单按 B 实测精确化：全塌缩修复 4 面[采购员→pur/销售员→sal/作业员→mfg/质检员→qa] + 半塌缩精化[管理员→md 经 M2.8 级联 3 节点、生产计划员→mfg 6、维护人员→mnt 5（:171「mnt 无该业务角色 per-action 种子」过时——实有 维护人员×7 种子）、财务员→fin 19 节点约半数组塌缩]；(d) **三类残量登记**：[i] 6 域 base-only 49 个未声明 SUBM（pur 10/sal 9/mfg 17/qa 6/mnt 6/md 1，line/child 页面型，属性追加式修复结构性不可覆盖、维持不可见非回归）+ [ii] FNPT 富集八域域内半塌缩实测清单（inv 库管员 6/ast 资产管理员 9/prj 项目经理 7/hr HR 专员 17·薪酬审批人 4/ct 合同双角色 5/5/b2b 双角色 5/7 + A 路 146 节点口径[fin 38/inv 16/ast 19/hr 32/prj 16/ct 13/b2b 12]）+ [iii] 管理员（业务）→erp-md seed-content 残留（TOPM 13 角色既定不含管理员，另裁）；(e) B 类三分法 fail-closed 姿态与两类 successor 触发条件；(f) **「菜单壳」部署语义**（%prod 全操作 enforcement + query/mutation FNPT 零种子 = 全角色数据加载 403，已验收语义非故障）。`:242` 双层声明更新**作用域限定为「6 域 delta 声明节点 + erp-sys 子树种子在位」**（不得写全域性「per-entity 层静态种子在位」），并加 SUBM 不得携带 permissions= 不变量句（A 路风险 3）+ :167 l10n-cn 过时引用顺手订正。
      Skill: none

Exit Criteria:

- [x] `git diff --stat` = 9 文件（6 域 delta + app.action-auth.xml + application.yaml + owner doc）+ 计划级附加足迹（roadmap 状态回写 + C4 spec 显式扩界 [2 处断言 + 注释，测试真相对齐 M2.8 已裁决语义] + 探针脚本新文件）——Phase 2 范围内 9 文件全部为属性追加/单行翻转/文档段，无节点删除或结构重排；xmllint 7 文件全部通过。

### Phase 3 — 验证（%test 套件 + %prod 运行时探针）

Status: completed
Targets: `tools/verify-prod-auth-gray.sh`（新增）、E2E negative 套件
Skill: none

- Item Types: `Proof | Add`

- [x] Add: `tools/verify-prod-auth-gray.sh`——%prod profile 探针：新鲜 jar 重装后以 `-Dquarkus.profile=prod -Dquarkus.http.port=8081 -Dnop.datasource.jdbc-url=jdbc:h2:./db/erp-probe` 启动 → 启动守卫断言（enable-action-auth 实际生效值 = true 且来源为 %prod 块）→ 四组断言：① role-restricted（userId 10）GraphQL FNPT 动作（ErpFinBadDebt.writeOff）返回 no-permission；② role-restricted SiteMap：B 类 5 域隐藏（%prod fail-closed 姿态，D2 裁决）+ notify 可见；③ role-pur（采购员，userId 11）SiteMap：erp-pur + erp-md TOPM + md-material SUBM 可见、erp-fin 隐藏；④ nop（平台 admin）SiteMap：erp-sys 可见（D2 修复后，**可见 ≠ 可用**——sys 页面动作 %prod 下对所有人 deny，平台实体零权限点）+ erp-fin 隐藏（RBAC 姿态）；⑤ 「菜单壳」已验收语义断言：role-pur `ErpPurOrder__findPage` → `nop.err.auth.no-permission`（C3）。脚本退出码非零 = 失败；teardown 杀进程删 `erp-probe.mv.db`/`erp-probe.trace.db`。
      Skill: none
- [x] Proof: **C4 预检实测基线（修复前，种子落地前运行）**：menu-filter 2 failed / 2 passed——(b) 红于 ：81「财务员见 erp-ct」（M2.8 ct 财务员种子级联可见的**预存差异非本计划回归**，按 triage 显式扩界处置：spec 断言对齐 M2.8 已裁决语义 [erp-ct 移入可见断言 + hidden 清单移除]，注释引用 commit 818759a61 与本计划 C4）；(e) 红于 ：135 仅（:133 已转绿——两批准代理静态预判命中）。修复后 4/4 全绿。
      Skill: none
- [x] Proof: E2E negative 全套件（`npx playwright test tests/e2e/negative`）：**64 passed / 9 failed，9 失败经 stash+HEAD 复跑逐条复现一致 = 全部预存、零回归**（失败集：e2-1×2 / e2-2×2 / e2-3×3 / role-login×2——机制均为 %test 侧「菜单壳」业务角色页面数据加载 403，P2.4 翻启即存在的预存套件红，登记见下）；menu-filter (e) 红灯转绿 + (b) C4 对齐后 4/4 绿。抽样回归 business-actions 2 spec + crud 1 spec：fin-bad-debt PASS；pur-return approve path + sales CRUD smoke 2 失败经 stash+HEAD 复跑一致 = 预存（AMIS/flux 渲染超时预存环境问题族），零回归。
      Skill: none
- [x] Proof: 探针脚本实跑 **6 组断言全过**（guard + ① restricted FNPT 拒绝 + ② B 类 5 域隐藏+notify 可见 + ③ 采购员 erp-pur/erp-md/md-material 可见+erp-fin 隐藏 + ④ 平台 admin erp-sys 可见[可见≠可用]+erp-fin 隐藏 + ⑤ 采购员 findPage 拒绝「菜单壳」已验收语义）；执行期脚本缺陷两轮修复（H2 拒绝隐式相对路径 → `./db/` 前缀；nop 顶层 extensions 层级[extensions 为 errors 兄弟节点非内嵌] → 提取器双层级兜底）。`bash docs/audits/nop-compliance-checker.sh` **exit 0**；`npm run check:xdsl` **PASS 0 违规**。
      Skill: none
      Skill: none

Exit Criteria:

- [x] 三组 Proof 全绿并落盘摘要；红灯清零（menu-filter (e) 转绿 + (b) C4 对齐）且零新增失败（9+2 失败全部 HEAD 归因预存）。

## Draft Review Record

- Independent draft review iteration 1: needs revision（独立子代理 agent_92e303c3，0 Blocker + 3 Major + 5 Minor；抽查 7 项实仓核验全 PASS 含平台源码行号零漂移）——Major-1 Phase 2 退出标准文件数 8 vs 9 自相矛盾；Major-2 探针独立 DB 缺数据源覆盖键；Major-3 B 类 5 域 %prod 全员不可达盲区未显式裁决；Minor：backlog README 松弛措辞/灰度开关口径未明文/探针 profile 显式化/管理员→erp-md seed-content 残留豁免/文本瑕疵。已全部整改（新增 D4 Decision）。
- Independent draft review iteration 2（定点复审）: accept（同一独立子代理 agent_92e303c3）——8 项修复逐项核对全过，无新引入阻塞问题；3 条非阻塞注记（Non-Goals B 类措辞同步/Goals 断言计数对齐/D4 编号顺序）已随手落实前两条。

## 双独立子代理批准记录（保护区：auth/permissions 设计决策，owner doc :171 要求）

- 批准代理 A: **GRANT（附 2 项条件，fresh session，2026-10-01）**——D1/D2/D3/D4 全部批准（平台机理逐行独立复核成立 + 全仓 39 文件 0 个 TOPM/SUBM 带 permissions= 反例排查 + 六域 post-fix 模拟与 e1-2 断言预判吻合）。条件：**C1** = owner doc :242 修订限定作用域为「6 域 102 节点 + erp-sys 子树」，:171 塌缩面收敛清单须补登记 7 个对照面域（fin/inv/ast/hr/prj/ct/b2b）的**部分菜单组塌缩残留**（146 个 per-entity/页面型 SUBM：38/16/19/32/16/13/12，fail-closed 方向非安全面）+ successor 触发条件（同 B 类裁决家族），或扩面镜像（执行者二选一）；**C2** = 「平台 admin 可见面 = erp-sys + notify」须注记「可见 ≠ 可用」（平台实体零权限点，sys 页面动作 %prod 下对所有人 deny），探针断言 ④ 不得暗示可用性。可选：:167 l10n-cn 过时引用顺手订正。**执行者处置：选择登记残留+successor 最小路径**（两次审查批准范围 = 6 域 + erp-sys；7 域镜像机制与已干跑验证的变换脚本相同 [102 节点计数精确命中]，作具名 successor 承接——登记于 Deferred But Adjudicated 节）；C1/C2 并入 Phase 2 owner doc 修订稿与探针断言措辞。
- 批准代理 B: **条件 GRANT（4 必须 + 1 建议，fresh session 独立复核，与 A 路互不共享上下文，2026-10-01）**——D1/D2/D3/D4 方向与平台机理证据链独立复核全部成立（含 GraphQL enforcement 全链：GraphQLExecutor:72/155、ReflectionBizModelBuilder:363-367 自动派生 auth、SiteCacheData.isPermitted 只查 permissionToRoles）。**C1（必须）**：B 类 5 域「全员不可达」为事实错误——aps 24 FNPT（生产计划员）/log 10 FNPT（库管员，销售员×8+管理员×2）级联可见，真 fail-closed 仅 crm/cs/drp（0 FNPT 实测）；D2/owner doc 修订/探针②理由改三分法。**C2（必须）**：owner doc :242 收敛为「6 域 delta 声明节点种子在位」+ 登记三类残量（base-only 49 SUBM[10/9/17/6/6/1]、FNPT 富集八域域内半塌缩实测清单、:171 维护人员表述更正[有 7 个种子实态部分可见 5 节点]+财务员→erp-ct 级联可见面补登）。**C3（必须）**：D1 残留风险升级为显式部署语义条目——%prod 全操作（含查询）enforcement + query/mutation FNPT 零种子 → 全角色「菜单壳」；探针增补第⑤组断言（采购员 ErpPurOrder__findPage → no-permission）固化已验收语义。**C4（必须）**：Phase 3 前实测 e1-2 (b) 现状并预登记 triage 分支（若「财务员不见 erp-ct」红 = M2.8 ct 种子[runAccrual/postSettlement/triggerInvoice→财务员]级联可见的预存差异非本计划回归；spec 修改超出零新 spec 边界须显式扩界或另立 successor）。**C5（建议）**：塌缩面收敛清单按 B 实测精确化（全塌缩 4 面[采购员→pur/销售员→sal/作业员→mfg/质检员→qa]/半塌缩[管理员→md 3 节点、生产计划员→mfg 6、维护人员→mnt 5、财务员→fin 19]）。双批准合并结论：**两路条件 GRANT、零 DENY，按计划 :64 规则条件落实后批准生效，准予进入 Phase 2**。

## Closure Gates

- [x] 范围内行为完成（6 域 102 节点 + erp-sys 子树 26 节点种子落地 + %prod 翻转 + owner doc 四处修订 + 探针脚本 + C4 spec 对齐）
- [x] 相关文档对齐（owner doc :167 订正/:171 追加裁决段/:213 基线表/:242 作用域限定双层声明；roadmap USC-02a 行状态回写；backlog README 本批不改——USC 工作项状态仅 roadmap 持有，索引行随 roadmap 收官批统一同步）
- [x] 已运行验证（negative 全套件 64/73 绿 + 9 失败 HEAD 归因预存 + 抽样回归 3 spec [1 绿 2 预存 HEAD 一致] + %prod 探针 6 组断言全过 + compliance checker exit 0 + xdsl PASS；`mvn clean install -DskipTests` BUILD SUCCESS 探针前置重装）
- [x] 无范围内项目降级为 deferred/follow-up（7 域残留与 data-auth 均为批准条件授权的具名 successor，见 Deferred 节）
- [x] 独立草案审查已完成并记录（iteration 1 needs revision 3M+5m → 定点复审 accept）
- [x] 双独立子代理批准已完成并落盘（A：GRANT 附 2 条件；B：条件 GRANT 附 4 必须+1 建议；条件全部并入后批准生效）
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致（日志条目随提交落盘 docs/logs/2026/10-01.md）
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符（审计方独立复跑确认 2 审计项于其审计时保持 [ ] 未预勾）
- [x] 结束证据存在于文件中（本计划 Closure 节 + owner doc :173 裁决段 + docs/logs/2026/10-01.md）

## Deferred But Adjudicated

### 7 对照面域部分菜单组塌缩残留（批准代理 A 条件 C1 处置）

- Classification: `successor ownership`（批准代理 A 双批准条件 C1 授权的登记最小路径）
- Why Not Blocking Closure: fail-closed 方向残留（菜单组不可见而非越权暴露），非安全面；两次审查批准范围 = 6 域 102 节点 + erp-sys；镜像机制与已验证变换相同可机械承接。
- 残留面：fin 财务员 8/15 组死（38 节点）/inv 库管员 7/9（16）/ast 资产管理员 5/7（19）/hr HR 专员 7/12 + 薪酬审批人 11/12（32）/prj 项目经理 5/8（16）/ct 合同双角色 5/7（13）/b2b 对账员 3/5（12），合计 146 个 per-entity/页面型 SUBM。
- Successor Required: `yes`——触发条件：USC-02a 落地后任一对照面域菜单完整性投诉/验收需求，或下一权限批次开启时（镜像 erp-fin 等域菜单组 roles= 至 per-entity/页面型 SUBM，机制同 D3）。

### 15 域 data-auth inert stub 与 %prod data-auth 翻转

- Classification: `successor ownership`（USC-02b 载体，非本计划 deferred）
- Why Not Blocking Closure: roadmap 明文 data-auth prod 翻转由 USC-02b 独立持有，单次交付原子性（roadmap §6 USC-02b）。
- Successor Required: `yes`（USC-02b）

## Closure

Status Note: 独立结束审计 ACCEPT（0 Blocker / 0 Major / 3 Minor 已随收官整改：roadmap 注记订正随 done 翻转、日志落盘、重复 Skill 行排版修复）。交付物核验（102+26 种子纯属性追加零节点删除、FNPT 零变更、%prod 翻转、owner doc 四处修订、C4 spec 对齐）+ 独立复跑（探针 6 组断言 exit 0 / xdsl PASS / checker exit 0 / menu-filter 4/4 / role-login 预存归因自洽）+ 批准条件 A C1/C2 + B C1-C5 逐条落实——USC-02a 可标记 done。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理（fresh session，只读，2026-10-01）——ACCEPT（0 Blocker / 0 Major / 3 Minor）
- Evidence: ①交付物：7 XML xmllint 全过；6 域删除行/含 roles= 新增行精确配对 28/12/10/22/17/13=102、去属性后逐字一致（纯属性追加零节点删除、FNPT 行零变更）；app.action-auth.xml 26/26 配对、节点级核验 6 菜单组+20 叶全 SUBM roles="admin"、全文件 admin=27（TOPM :41 既存）；application.yaml %prod true/false+部署语义注释；owner doc :167 订正/:173 裁决段（三类残量+三分法+菜单壳+可见≠可用+permissions= 不变量）/:215 基线行/:244 作用域限定；spec (b) 对齐含 818759a61+C4 注释。②独立复跑全绿：verify-prod-auth-gray.sh 6 组断言 PASS exit 0（jar 与工作树 8 资源逐字节一致）、check:xdsl PASS 0 违规 exit 0、compliance-checker exit 0、menu-filter 4/4 绿、role-login 复跑 2 failed 与登记一致（ErpMdCurrency__findPage 403，HEAD %test 块一致+种子不触动作授权→预存归因自洽）。③批准条件 A C1/C2 + B C1-C5 逐条落实核对；2 审计门控保持未勾。④Minor 3 项随收官整改。

Follow-up:

- （无——缺口分流已尽：data-auth 归 USC-02b；平台语义类残留维持 owner doc 登记态。）
