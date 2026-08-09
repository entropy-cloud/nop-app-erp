# 2026-08-09-0751-3 enforcement-config-profile

> Plan Status: active
> Last Reviewed: 2026-08-09
> Source: `docs/backlog/permissions-enforcement-roadmap.md` P2.1
> Related: mission `permissions-enforcement`；P1.6（xwf 语义裁决，已 done——P2.1 唯一硬前置）；P2.2a（管理员兜底先行，其 skip-check-for-admin 依赖由本计划 Fix 项解除；P2.2a 仍阻塞于 P1.5b，本计划不解除该阻塞）；P2.4（dry-run 门控，将翻 enable-action-auth，依赖本计划预置 config 变量）；E2.1（data-auth 开启，将翻 enable-data-auth + role-row-filter，依赖本计划预置 config 变量）；**roadmap §横切关注点 2 L84 亦载有 skip-check「默认启用」漂移**（companion 修正——backlog 文档同型事实错误，本计划 Phase 3 一并修正，因 P2.1 拥有 skip-check 裁决）
> Audit: required
> Mission: permissions-enforcement
> Work Item: P2.1

## Current Baseline

P2.1 是 enforcement 从声明层推进到强制执行层的**配置基建**：把三开关预置为 dev/test profile 的 config 变量（默认 OFF），使后续 P2.4（action-auth）/E2.1（data-auth + row-filter）能按翻启节奏翻启而无需改代码（灰度粒度 = config 变量）。

**聚合 app 配置现状**（实测 `app-erp-all/src/main/resources/application.yaml`，52 行——enforcement 权威配置；19 个域 `erp-*--app/application.yaml` 为单域独立 runner，非聚合运行时权威，本计划不触及）：

- L4-12：`nop.auth.*` 仅有 `jwt.enc-key`、`login.allow-create-default-user: false`、`site-map.*`。
- **L48-52：唯一的 `"%dev":` profile 块**——仅覆盖 `nop.auth.login.allow-create-default-user: true`。
- **无 `nop.auth.enable-action-auth` / `enable-data-auth` / `skip-check-for-admin` 设置**（grep 全仓零命中源配置）。
- **无 `"%test":` / `"%prod":` profile 块**；**无 `application-{profile}.yaml` 文件**（glob 零命中）——仓库 profile 惯例 = 单文件内联 `"%<profile>":` 块（Nop/Quarkus profile-key 语法）。

**三开关实测**（platform 源 + app 源核验）：

| 开关 | 命名空间 | 定义源 | 平台默认 | app 是否覆盖 |
|------|----------|--------|----------|--------------|
| `nop.auth.enable-action-auth` | platform | `nop-entropy/.../ApiConfigs.java:87-88` | **false** | 否 |
| `nop.auth.enable-data-auth` | platform | `nop-entropy/.../ApiConfigs.java:91-92` | **false** | 否 |
| `erp.data-auth.role-row-filter-enabled` | **app** | key 名 `module-common-service/.../auth/ErpRoleDataAuthConstants.java:22`；默认值 `ErpRoleDataAuthChecker.java:39-42` `AppConfig.var(..., Boolean.FALSE)` | **false** | 否 |

- **action-auth** 消费：`GraphQLEngine.enableActionAuth`（`nop-entropy/.../biz-defaults.beans.xml:13` `@cfg:nop.auth.enable-action-auth|false`）+ `SiteMapProviderImpl.enableActionAuth`（菜单过滤门控）。
- **data-auth 双层门控**：`GraphQLEngine.enableDataAuth`（平台第一层，默认 false）+ `ErpRoleDataAuthChecker.isEnabled()`（app 第二层读 `erp.data-auth.role-row-filter-enabled`，默认 false）；两层皆 OFF → `getFilter()` 返回 null → 单组织基线零回归。翻转须**同时**开启两者（E2.1 范围）。`ErpRoleDataAuthChecker` 注册为 bean `nopDataAuthChecker`（`module-common-service/.../app-service.beans.xml:24-25` 覆盖平台 `DefaultDataAuthChecker`）。

**确认的 owner-doc 漂移（Fix 项）——`skip-check-for-admin` 默认值**：

- 平台 `nop-entropy/.../NopAuthConfigs.java:77` `CFG_AUTH_SKIP_CHECK_FOR_ADMIN` IConfigReference 默认 = **false**（DR-1e）。
- 平台文档 `nop-entropy/docs-for-ai/02-core-guides/auth-and-permissions.md:232,239`：「默认 `false`（管理员同样接受权限检查）」「该默认值由 `IConfigReference` 单一来源决定」。
- 消费方 `DefaultActionAuthChecker.java:36` 直接读 IConfigReference（平台 H-2 安全修复后已移除 bean 层 `@InjectValue|true` fallback——H-2 = 平台历史安全加固项，统一 admin 权限检查默认值由 IConfigReference 单一来源决定，杜绝 bean 覆盖绕过）。
- **app 全仓源配置零覆盖**（grep `skip-check-for-admin` 仅命中 `_dump/` 生成 merge 件，非源）→ **有效运行时默认 = false**。
- **但** owner doc `docs/design/roles-and-permissions.md:186` §运行基线表记 `skip-check-for-admin` = `true（默认）`，且 roadmap §横切关注点 2 / P2.2a（admin 兜底先行，**E1 硬前置**）依赖 admin 兜底有效。

**结论**：owner doc 与 roadmap 对 skip-check 默认值的表述与平台实际默认（false）不一致——这是已确认的 owner-doc 漂移。enforcement 翻转后（action-auth=true），若 skip-check 仍为 false，**平台 admin/nop-admin 角色不跳过权限检查**，P2.2a「nop 绑平台 admin → 全 E2E 绿」会大面积 403，admin 兜底失效（B2 风险复发）。

**enforcement 状态**：三开关有效默认全 false → 任何登录用户全通；本计划预置 config 变量默认 OFF（与有效默认一致，不改运行时行为），并显式设 skip-check=true 于 dev/test（解除 P2.2a 阻塞——见 Phase 1 Decision）。

**翻启节奏**（roadmap P2.1 + Details + 横切确认）：`enable-action-auth` 随 P2.4 翻启；`enable-data-auth` + `role-row-filter-enabled` 随 E2.1 同时翻启；prod 保持 OFF（successor）。

**缺口**：(1) 三开关未预置为 dev/test profile config 变量——后续 P2.4/E2.1 翻启须改代码而非调 config；(2) 无 `%test` profile 块（E2E/dev profile 测试无独立 config 面）；(3) skip-check-for-admin owner-doc 漂移未修，P2.2a admin 兜底阻塞未解除。

## Goals

- **预置三开关为 dev/test profile config 变量（默认 OFF）**：在 `app-erp-all/application.yaml` 的 `%dev`/`%test` profile 块显式声明 `nop.auth.enable-action-auth: false`、`nop.auth.enable-data-auth: false`、`erp.data-auth.role-row-filter-enabled: false`，使后续 P2.4/E2.1 按翻启节奏翻启而无需改代码（灰度粒度 = config 变量）。
- **建立 `%test` profile 块**：补齐 E2E/dev-profile 测试的独立 config 面（遵循内联 `"%profile":` 块惯例，不引入 `application-{profile}.yaml` 新模式）。
- **解除 P2.2a admin 兜底阻塞（Fix）**：在 `%dev`/`%test` profile 显式设 `nop.auth.skip-check-for-admin: true`，使平台 admin/nop-admin 角色在 action-auth 翻转后跳过权限检查（admin 兜底有效），不依赖与平台默认（false）的歧义；并修正 owner doc §运行基线 skip-check 漂移。
- **prod 保持 OFF**：`%prod` profile 显式三开关 false（与平台默认一致，确定性表达），skip-check 留平台默认 false（DR-1e 安全姿态，prod admin 兜底由 successor 裁决）。
- **owner doc 对齐**：全量修正 `skip-check-for-admin` 默认值漂移（`roles-and-permissions.md` L39/L143/L186 + `app-overview.md` L56 + roadmap L84，均断言「默认 true」（两种措辞：`默认启用` / `` `true`（默认） ``），而平台实际默认 false）+ §运行基线增 profile 化预置注记 + 翻启节奏落位。

## Non-Goals

- **不翻转任何 enforcement 开关为 ON**（翻启归 P2.4 / E2.1；本计划仅预置 config 变量默认 OFF + skip-check dev/test fix）。
- **不产 auth 表 CSV 种子 / 角色账号**（归 P1.5b；本计划仅 config 变量）。
- **不改 `ErpRoleDataAuthChecker` / `DefaultDataAuthChecker` 代码**（双层门控机制已就绪，本计划仅翻 config 变量）。
- **不改 19 个域 `erp-*-app/application.yaml`**（单域独立 runner，非聚合运行时权威）。
- **不做 admin 兜底实测验证**（归 P2.2a；本计划仅设 config 使其可生效）。
- **不做负向隔离测试**（归 P2.3 + E 段）。
- **不裁决 prod enforcement 翻转 / prod skip-check=true**（prod 翻转 + prod admin 兜底姿态归 successor，触发条件 = 测试环境全绿验收 + 生产灰度计划人工批准）。
- **不引入 `application-{profile}.yaml` 文件新模式**（遵循既有内联 `"%profile":` 块惯例）。

## Task Route

- Type: `implementation-only change`（application.yaml profile 块 config 变量预置 + owner doc 漂移修正；不改 Java 代码，不改运行时行为——三开关有效默认本就 false，skip-check dev/test 显式 true 在 action-auth OFF 时无运行时效果）
- Owner Docs: `docs/design/roles-and-permissions.md` §运行基线 + §数据权限
- Skill Selection Basis: `nop-testing` —— enforcement 配置 profile 化属测试与验证基建（与 roadmap 表格 P2.1 Skill 列一致）；本计划核心交付是 config 变量预置 + profile 块建立 + Proof 校验（YAML 解析 + grep 漂移清零）+ owner doc 修正，不写业务代码（机制已由 ErpRoleDataAuthChecker + 平台 bean 落地）。`nop-testing` 用于 Proof 阶段的配置校验与基线对照方法；Java/业务代码为零。

## Infrastructure And Config Prereqs

- No infra prereqs beyond existing baseline（仅 application.yaml profile 块 config 变量预置 + owner doc，三开关保持有效 OFF，不改运行时）。

## Execution Plan

### Phase 1 - skip-check-for-admin 漂移裁决 + profile 策略 Decision

Status: planned
Targets: `docs/design/roles-and-permissions.md` §运行基线（裁决记录落位）
Skill: none

- Item Types: `Decision`
- Prereqs: P1.6（done）

- [ ] **Decision**：裁决 `skip-check-for-admin` 漂移处理 + profile 策略。
  - **skip-check 漂移**：平台 IConfigReference 默认 false（DR-1e，`NopAuthConfigs.java:77` + 平台文档 L232/239 + `DefaultActionAuthChecker.java:36` 直读），app 源配置零覆盖 → 有效默认 false。owner doc §运行基线 L186 记「true（默认）」为**已确认漂移**。考虑的替代方案：(a) dev/test profile 显式设 `skip-check-for-admin=true`（解除 P2.2a admin 兜底阻塞，确定性表达，不依赖平台默认歧义）+ 修正 owner doc 为「平台默认 false，app dev/test 显式 true」——**采纳**；(b) 依赖平台默认（不设）——**拒绝**：有效默认 false 会使 P2.2a admin 兜底失效（B2 风险），与 roadmap §横切 2 / P2.2a 设计相悖；(c) 全 profile（含 prod）设 true——**拒绝**：prod admin 兜底姿态属 successor 裁决范围（Non-Goal），prod 留平台默认 false（DR-1e 安全姿态）。残留风险：prod 翻转 successor 须显式裁决 prod skip-check 姿态。
  - **profile 策略**：遵循既有内联 `"%<profile>":` 块惯例（`application.yaml:48-52` 唯一 `%dev` 块为范式），建立 `%test` + `%prod` 块；**不**引入 `application-{profile}.yaml` 文件新模式（无先例）。考虑的替代方案：(a) 内联块——**采纳**（遵循惯例）；(b) 独立 profile 文件——**拒绝**（引入新模式，无先例，增维护面）。
  - Skill: none

Exit Criteria:

- [ ] skip-check 漂移裁决（dev/test 显式 true + owner doc 修正）+ profile 策略裁决（内联块）落地于 plan，无阻塞歧义。

### Phase 2 - 预置三开关 config 变量 + skip-check fix + 建立 profile 块

Status: planned
Targets: `app-erp-all/src/main/resources/application.yaml`
Skill: `nop-testing`

- Item Types: `Add` / `Fix` / `Proof`
- Prereqs: Phase 1（裁决落地）

- [ ] **Add**：在 `app-erp-all/application.yaml` 建立三开关 + skip-check 的 profile 预置（内联 `"%profile":` 块），默认全 OFF。目标结构（`erp.data-auth.*` 与 `nop.auth.*` 为不同命名空间，Nop/Quarkus config 扁平化为 dotted key，跨命名空间并列正确解析）：
  ```yaml
  "%dev":
    nop:
      auth:
        login:
          allow-create-default-user: true
        enable-action-auth: false
        enable-data-auth: false
        skip-check-for-admin: true
    erp:
      data-auth:
        role-row-filter-enabled: false
  "%test":
    nop:
      auth:
        enable-action-auth: false
        enable-data-auth: false
        skip-check-for-admin: true
    erp:
      data-auth:
        role-row-filter-enabled: false
  "%prod":
    nop:
      auth:
        enable-action-auth: false
        enable-data-auth: false
    # skip-check-for-admin 省略 → 继承平台默认 false（DR-1e 安全姿态）
    erp:
      data-auth:
        role-row-filter-enabled: false
  ```
  - 扩展既有 `"%dev":` 块（L48-52）增三开关 false + skip-check=true（保留既有 allow-create-default-user=true）；新建 `"%test":` 块（同 dev 四变量，skip-check=true 使 E2E admin 兜底可生效）；新建 `"%prod":` 块（三开关 false 确定性表达；skip-check 省略→平台默认 false）。
  - Skill: `nop-testing`
- [ ] **Fix**：`skip-check-for-admin` owner-doc 漂移修正（解除 P2.2a skip-check 依赖阻塞——P2.2a 仍阻塞于 P1.5b）——本项是 Phase 2 config 落地的文档侧伴随（owner doc 全文修正归 Phase 3，本项确认 config 侧 fix 已使 admin 兜底在 dev/test 可生效）。
  - Skill: none
- [ ] **Proof**：YAML well-formed 校验 `app-erp-all/application.yaml`（用 Python `yaml.safe_load` 或 Nop config 启动解析；`xmllint` 不适用 yaml）+ profile 块结构自检（`%dev`/`%test` 含三开关 false + skip-check=true；`%prod` 含三开关 false 且无 skip-check）+ 三开关有效值仍为 false（预置不改运行时，action-auth OFF 时 skip-check 无运行时效果）。
  - Skill: none

Exit Criteria:

- [ ] `%dev`/`%test`/`%prod` 三 profile 块落地，三开关预置默认 OFF，skip-check dev/test 显式 true、prod 继承平台默认 false；YAML well-formed + profile 结构自检通过。

### Phase 3 - owner doc §运行基线修正 + 日志

Status: planned
Targets: `docs/design/roles-and-permissions.md` §运行基线 + §数据权限
Skill: none

- Item Types: `Fix` / `Add`
- Prereqs: Phase 2

- [ ] **Fix**：修正 `skip-check-for-admin` 默认值 owner-doc 漂移——**全量枚举并修正所有断言「平台默认 true」的位置**（Rule 13：已确认漂移不得遗漏）。实测命中位置（两种漂移措辞：`默认启用` 与 `true（默认）`）：`docs/design/roles-and-permissions.md` L39（§角色体系「管理员」行，措辞「默认启用」）、L143（§角色→权限点映射「管理员」行，措辞「默认启用」）、L186（§运行基线表，措辞「`true`（默认）」）；`docs/design/app-overview.md` L56（§角色体系「管理员」行，措辞「默认启用」）；`docs/backlog/permissions-enforcement-roadmap.md` L84（§横切关注点 2，措辞「默认启用」）。逐处由「默认启用 / `true`（默认）」改为「平台 IConfigReference 默认 `false`（DR-1e，`NopAuthConfigs.java:77` 单一来源）；app `%dev`/`%test` profile 显式 `true`（admin 兜底可生效，见 plan 2026-08-09-0751-3 / P2.1）；`%prod` 继承平台默认 `false`（安全姿态，prod 翻转 successor 裁决）」。roadmap L84 仅修正事实陈述，不改工作项状态/范围。
  - Skill: none
- [ ] **Add**：§运行基线增「profile 化预置」注记——三开关已在 `app-erp-all/application.yaml` `%dev`/`%test`/`%prod` profile 预置为 config 变量（默认 OFF，灰度粒度 = config 变量）；翻启节奏：`enable-action-auth` 随 P2.4、`enable-data-auth` + `role-row-filter-enabled` 随 E2.1 同时；prod 保持 OFF（successor）。§数据权限双层门控注记补「两开关 profile 预置就绪」。
  - Skill: none
- [ ] **Proof**：(1) **权威 gate——逐行 spot-check**：显式核验 5 个枚举行的**当前值列/语义**（roles-and-permissions.md L39/L143/L186 + app-overview.md L56 + roadmap L84）均已由「默认 true」（两种措辞）改为「平台默认 false + app dev/test 显式 true + prod 继承 false」，**无一仍断言默认 true**（直击语义，不依赖 token 顺序——校正文本合法含「true」与「默认」两 token，grep 无法稳定区分，故以 spot-check 为权威）。(2) **辅助扫荡——grep**：`grep -rnE "skip-check-for-admin.*默认启用" docs/`（排除 plans/audits/logs/discussions/analysis/lessons/retrospectives 历史目录）应零命中，用于发现 5 枚举行之外的任何**新**「默认启用」漂移点（L186 措辞异，由 spot-check 覆盖，不在 grep 责任内）。§运行基线表与 application.yaml 真相源一致。
  - Skill: none

Exit Criteria:

- [ ] owner doc skip-check 漂移全量修正（**权威 gate**：5 枚举行逐行 spot-check 均不再断言默认 true；**辅助**：`grep -rnE "skip-check-for-admin.*默认启用" docs/` 排除历史目录零命中）+ §运行基线 profile 化预置 + 翻启节奏注记落地，与 application.yaml 真相源一致。

## Draft Review Record

- Independent draft review iteration 1: needs revision（0 blocker / **1 major** / 5 minor）（ses_01a3e3d53ffeIsmtMMtEQnpl3j）。**M1** skip-check-for-admin owner-doc 漂移存在于 `roles-and-permissions.md` **L39/L143/L186 三处**（非仅 L186），Phase 3 仅修 L186 会留内部不一致（Rule 13：已确认漂移须全量 Fix）；m1 Skill nop-testing 与实际 config/doc 工作弱匹配；m2 Phase 2 未示具体 YAML 嵌套（erp.data-auth 跨命名空间新键）；m3 「H-2 修复」缺上下文；m4 Related 「解除其阻塞」对 P2.2a 误导（仍阻塞 P1.5b）；m5 roadmap L84 同型漂移未提。skip-check 漂移为 load-bearing 发现且**实测确认成立**（平台 NopAuthConfigs.java:77 默认 false + DefaultActionAuthChecker.java:36 直读 + app 源零覆盖 + owner doc L186 记 true）；基线全部实测准确；Deps（P1.6 done）确认；cadence 尊重（零开关翻 ON）；「无运行时行为变更」可辩护（skip-check 消费方受 enableActionAuth 门控）；protected-area 范围（test-env 批准内）通过。
- 合并修订（iteration 1 → v2）：**M1** Phase 3 Fix 全量枚举修正漂移（roles-and-permissions.md L39/L143/L186 + app-overview.md L56 + roadmap L84）+ grep 零残留 Proof；m2 Phase 2 增具体 YAML 嵌套片段（含 erp.data-auth 跨命名空间）；m3 H-2 加上下文（平台安全加固，统一默认值单一来源）；m4 Related 改「解除 skip-check 依赖阻塞（P2.2a 仍阻塞 P1.5b）」；m5 roadmap L84 列入 Phase 3 Fix + Related companion；m1 Skill 基础补 nop-testing 用于 Proof 校验/基线对照、Java 零变更。
- Independent draft review iteration 2: needs revision（0 blocker / **1 major** / 2 minor）（ses_01a362849ffe1uAMULCfFyjA6R）。**MAJOR-1** Phase 3/Proof/Closure Gate 的 grep 模式 `skip-check-for-admin.*默认启用` 不匹配 L186（L186 措辞为 `` `true`（默认） ``，非「默认启用」），致零残留 gate 无法检测未修的 L186（假「全清」）；M1 枚举 + Fix 实质完整（5 处全列），仅验证机制有漏洞。minor-1 YAML 缩进 2 vs 既有 3 空格（结构有效，信息性）；minor-2 `role-row-filter-enabled` 默认值引证应在 `ErpRoleDataAuthChecker.java:39-42`（L22 仅 key 名常量）。skip-check 漂移 load-bearing 发现再次实测确认成立；YAML 结构正确（nop.auth.* 为 login: 兄弟、erp: 跨命名空间顶层块、%prod 省略 skip-check 有理）；cadence/protected-area/Rule 9/13 通过；roadmap L84 companion 修正「仅事实陈述」非 scope creep。
- 合并修订（iteration 2 → v3）：**MAJOR-1** grep 模式改为 `skip-check-for-admin.*true.*默认`（实测覆盖「true 默认启用」与「`true`（默认）」两种漂移措辞，5 处全命中；校正后文本「默认 false … 显式 true」true 在 默认 之后不误命中）+ Proof/Closure Gate/Exit Criteria/Goals 统一新模式 + 增「逐行 spot-check 5 枚举行」作为权威 gate；minor-1 YAML 缩进注（执行时对齐既有 3 空格）；minor-2 默认值引证改 `ErpRoleDataAuthChecker.java:39-42`。
- Independent draft review iteration 3: needs revision（0 blocker / **1 major** / 0 minor）（ses_01a2f3e58ffeR7jA0Bbczw47J9）。iter-2 MAJOR-1（grep 漏 L186）**已解决**——新模式 `true.*默认` 实测命中含 L186 的全部 5 处 + spot-check 兜底。**新 MAJOR**（反向缺陷）：L159 校正文本自身含「显式 true … 继承平台默认 false」，token 序为 默认…true…默认，故 `true.*默认` 仍命中校正后文本 → grep 零命中 gate 不可满足，L163 理由（「true 在 默认 之后不误命中」）事实错误。Rule 9/13 + plan-first + protected-area + YAML 结构均仍通过；无其他回归。
- 合并修订（iteration 3 → v4）：grep 降级为**辅助扫荡**（`默认启用` 模式，仅用于发现 5 枚举行之外的新漂移点；L186 措辞异不在 grep 责任内）；**权威 gate 改为 5 枚举行逐行 spot-check**（直击「不再断言默认 true」语义，不依赖 token 顺序，规避校正文本 token 重叠）。Proof / Exit Criteria / Closure Gates 三处统一。
- Independent draft review iteration 4: accept（0 blocker / 0 major / 0 minor）（ses_01a2bfb9dffeqR4dopeL4IRKdi）。iter-3 新 MAJOR（grep 零命中 gate 不可满足 + 理由事实错误）**已解决**——5 枚举行逐行 spot-check 升为**权威 gate**（语义直击「不再断言默认 true」，不依赖 token 顺序，规避校正文本 token 重叠），grep 降为**辅助扫荡**（`默认启用` 模式，仅捕获 5 枚举行之外的新漂移点，L186 措辞异由 spot-check 覆盖），错误理由已移除；Proof/Exit Criteria/Closure Gates 三处一致。advisory grep 实测当前命中 4 处「默认启用」预校正基线、正确排除 L186，功能如设计。Rule 9/13 + anti-slack + plan-first + protected-area（test-env 范围、cadence 零开关翻 ON、prod→successor）全通过。
- Plan Status → active（四轮独立审查共识，0 blocker / 0 major）。

## Closure Gates

> 本计划改 `app-erp-all/application.yaml` profile 块（三开关预置默认 OFF，不改有效运行时值）+ owner doc 漂移修正。三开关有效默认本就 false，预置不改运行时；skip-check dev/test 显式 true 在 action-auth OFF 时无运行时效果（P2.2a 翻 action-auth 后才生效）。Closure Gates 运行 YAML 解析校验 + compliance checker 对照 `known-good-baselines.md` 零漂移（横切关注点 7）+ 完整 build。改 0 Java。

- [ ] 范围内行为完成（三 profile 块 + 三开关预置默认 OFF + skip-check dev/test fix + owner doc 漂移修正 + 翻启节奏注记）
- [ ] 相关文档对齐（`roles-and-permissions.md` §运行基线 + §数据权限）
- [ ] 已运行验证：application.yaml YAML 解析校验（`yaml.safe_load`/Nop config 启动）+ skip-check 漂移修正（权威 gate：5 枚举行逐行 spot-check 均不再断言默认 true；辅助 grep `默认启用` 排除历史目录零命中）+ `bash docs/audits/nop-compliance-checker.sh` 对照 `docs/testing/known-good-baselines.md` 零漂移 + `mvn clean install -DskipTests`
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控、日志一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### prod skip-check-for-admin 姿态裁决

- Classification: `watch-only residual`
- Why Not Blocking Closure: prod 翻转整体为 successor（Non-Goal，触发 = 测试环境全绿 + 生产灰度计划人工批准）；本计划 `%prod` 留平台默认 false（DR-1e 安全姿态）。prod 是否需 admin 兜底（skip-check=true）由 prod 翻转 successor 裁决。
- Successor Required: yes（触发条件 = 生产灰度计划人工批准）

### admin 兜底实测验证

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划仅设 config 使 admin 兜底**可**生效；实测验证（nop 绑平台 admin → 翻 action-auth → 全 E2E 绿）归 P2.2a（E1 硬前置）。
- Successor Required: yes（触发条件 = P2.2a 进入）

### application-{profile}.yaml 独立文件模式

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划遵循既有内联 `"%profile":` 块惯例（Phase 1 Decision）；独立 profile 文件新模式无先例，不引入。
- Successor Required: no（仅当项目未来确立多文件 profile 惯例时再评估）

## Closure

Status Note: <关闭时填写>

Closure Audit Evidence:

- Auditor / Agent: <独立结束审计子代理（新会话），执行者未自我审计>
- Evidence: <关闭时填写>

Follow-up:

- <仅非阻塞跟进项；已确认缺陷不得出现于此>
