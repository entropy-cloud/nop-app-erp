# 2026-08-16-2043-3-rc-mr1-r1-60-prj-cost-rate-tier RC-R1.60 — projects 工时成本率三级降级（P1-RC-048：用户级 > 角色级 > 活动类型级费率载体落地）

> Plan Status: active
> Last Reviewed: 2026-08-16
> Mission: requirement-compliance
> Work Item: RC-R1.60（P1-RC-048，UC-PRJ-02 AC-③ 成本率三级优先级）
> Source: `docs/backlog/requirement-compliance-roadmap.md` §MR1 RC-R1.60 行 + `docs/audits/arm-index.md` P1-RC-048 行（:219）+ 展开器 `docs/audits/2026-08-07-1910-rc-mr1-r1-0-expander.md`（RC-R1.60 = ORM 结构变更：成本率费率载体）
> Related: `docs/design/projects/use-cases.md`（L1 UC-PRJ-02 :38）；`docs/design/projects/cost-collection.md`（§2.2 成本率配置 + §七 关键业务规则 3）；`docs/audits/2026-08-05-2200-2-rc-ma1-a1-34-projects-f1-startup-cost-collection.md`（A1.34 §5/§6 P1-RC-048 :209-213）；`docs/audits/2026-08-07-2359-rc-ma4-a4-2-113-123-projects-f1-f2-f3-runtime.md`（A4.2.116 协同）；`docs/plans/2026-08-16-1634-1-rc-mr1-r1-57-crm-team-member-allocation.md`（A 类 ORM 双独立子 agent 批准先例）
> Audit: required

## Current Baseline

- **finding P1-RC-048（arm-index:219，UC-PRJ-02 AC-③）**：L1（`use-cases.md:38`）逐字「成本率解析(优先级: 用户费率 > 角色费率 > 活动类型费率)」——要求**三级**优先级。L3 实仓（HEAD 核查）：`CostRateResolver.resolve:40-67`（`module-projects/erp-prj-service/.../cost/CostRateResolver.java`）实为「单填(timesheet.costRate) > 活动类型(activityType.costRate) > 全局 config(erp-prj.default-labor-cost-rate)」**三级但层级错位**——**用户级/角色级独立费率载体未实现**（`ErpPrjProjectUser.role` 为纯文本 VARCHAR(50) 无费率列，propId 4；无 ErpPrjRole 实体——全仓 grep 零命中）。
- **owner doc 立场（A1.34 §5/§6 复核）**：`cost-collection.md §2.2:58-61` 显式 Non-Goal 标注「用户级/角色级独立费率载体本期不存在…待多级费率配置需求落地时新增用户费率实体（successor）」；git log 全为 AI commits 无人工批准痕迹 → §4 三判据不满足 → **Q4=(a) 强制实现禁止方案 B**（roadmap 头 2026-08-12 A 类块 :33-47 裁决；对齐 A1.24 UC-AST-03 / A1.8 UC-MFG-05 先例）。
- **product-scope 确认义务（P1-RC-048 修复注记）**：「先须人工确认 product-scope 是否裁剪成本率层级」——**2026-08-12 批量裁决 A 类清单显式收录 RC-R1.60（ErpPrjProjectUser 加 costRate 列 + 新增 ErpPrjRole 实体 或费率实体）**，即用户级（项目成员费率）+ 角色级（角色费率）双载体均确认需要 ORM 结构变更并**批量批准**；对齐 RC-R1.56/58/59 范式（product-scope 复核未裁剪 + Q4=(a) 裁决声明闭合，审计不复开）。
- **ORM 结构现状（HEAD 核查）**：`ErpPrjProjectUser`（`module-projects/model/app-erp-projects.orm.xml:342-371`）列集 {id/projectId/userId/role VARCHAR50/startDate/endDate + 审计列}，propId 1-12（含 delVersion 7/version 8/createdBy 9/createTime 10/updatedBy 11/updateTime 12）；to-one `project` + `user`（ErpMdEmployee）；索引 IDX_PRJ_PROJECT_USER_PROJECT_ID/USER_ID；**无 costRate 列**（next free propId = 13）。`ErpPrjActivityType.costRate`（:318，propId 4）+ `ErpPrjTimesheet.costRate`（:228，propId 11）已存在。
- **A 类裁决边界（roadmap 头 2026-08-12 批量裁决 A 类块 :33-47，RC-R1.60 行 :39；Q3 纯加性授权 :24）**：「纯加性 ORM 变更批量授权（加列/加 UK/新增实体，不改既有语义、无 NOT NULL 无默认列、无既有数据 UK 增设、无删除/迁移/索引改造）」——RC-R1.60 = ErpPrjProjectUser 加 costRate 列（可空无默认）+ 新增 ErpPrjRole 实体（或费率实体），均在 Q3 纯加性范围内；**越界回落双独立子 agent 批准**（roadmap 行旧「须…双独立子 agent 批准 checkbox」字样保留执行期义务，对齐 RC-R1.57/58 先例）。
- **resolve 语义设计**：L1 三级 = 用户费率 > 角色费率 > 活动类型费率；现实现「单填 > 活动类型 > 全局」——**单填（timesheet.costRate）与三级层级的关系须 Phase 1 裁决**（候选：A. 单填保持最高（显式录入优先）；B. 单填移除、用户级最高——L1 字面无单填，A1.34 §9 已登记 businessType drift 冻结条款不适用费率；倾向 A 保持既有行为兼容）。
- **测试基线**：`TestErpPrjTimesheetCost` 7 组（含 testCostRateFromTimesheetOverridesActivityType / testCostRateFallsBackToActivityType / testCostRateThrowsWhenNoRateAvailable / testSubmitRejectsNonOpenProject 等）——**用户级/角色级费率零测试**。erp-prj-service **138 tests 全绿**（R1.27 基线）。
- **compliance 基线**：R2c=1422 / R2b=235 / R2d=35；新增 ErpPrjRole 实体经 ORM 生成 dao 层（R2b/c 计数面：`CostRateResolver` 若加 daoFor(ErpPrjRole.class) 同域站点 vs 经 to-one/IBiz 注入——结束前复跑 checker 分类登记）。

## Goals

- **UC-PRJ-02 AC-③ 三级降级运行时成立（P1-RC-048 核心）**：`CostRateResolver.resolve` 实现 L1 三级优先级「用户费率 > 角色费率 > 活动类型费率」（+ 单填/全局默认按 Phase 1 裁决归位），用户级/角色级独立费率载体落地。
- **ORM 纯加性变更（A 类批量授权 + 双独立子 agent 批准）**：`ErpPrjProjectUser` 加 `costRate` 列（可空、无默认、无索引、无 UK，propId 13）+ 新增 `ErpPrjRole` 实体（或费率实体，Phase 1 载体裁决——对齐 A 类清单「ErpPrjRole 实体 或费率实体」双选项；列集含 costRate + code/name + 审计列 + UK 设计）——纯加性，不改既有语义。
- **resolve 接线**：`CostRateResolver.resolve` 增用户级（`ErpPrjProjectUser` 按 projectId+userId 查 costRate）+ 角色级（`ErpPrjProjectUser.role` 文本 → `ErpPrjRole.code` 匹配 costRate）tier——**tier 插入位置按 Phase 1 裁决**（L1 字面三级 + 既有单填/全局兼容）；三处皆无维持抛 `ERR_COST_RATE_NOT_AVAILABLE`（既有错误码）。
- **测试**：新增①用户级费率命中（覆盖活动类型）；②角色级费率命中（覆盖活动类型，低于用户级）；③用户级缺失回落角色级；④三级皆缺回落全局 config；⑤单填覆盖全链（按裁决）；⑥用户级/角色级费率为 null 跳过；⑦新增实体 CRUD 冒烟；既有 138 tests 零回归。
- **零回归**：erp-prj-service 全量测试全绿 + 全量 `mvn test` + `mvn clean install -DskipTests`（ORM 变更触发增量重生成）+ compliance checker 零漂移或基线登记。
- **owner doc 收敛**：`cost-collection.md §2.2` Non-Goal→已实现（三级载体 + 解析链注记 + Phase 1 裁决）+ §七 规则 3 注记；arm-index P1-RC-048 → done (RC-R1.60) + roadmap 行 done + logs 条目。

## Non-Goals

- **不做既有语义修改**（不改 ErpPrjProjectUser.role 列语义——纯文本角色名保留，角色费率经 ErpPrjRole.code 匹配；不改 activityType/timesheet costRate 列）。
- **不实现角色管理 UI/角色 CRUD 页面**（ErpPrjRole 实体经标准 CRUD 生成，业务 UI 非本行范围）。
- **不实现多币种费率/费率历史版本化**（超出 L1 断言面，successor 登记）。
- **不改真相源契约段落**（use-cases L1 不动；cost-collection.md 契约段不动，仅补实现注记 + Non-Goal 移除）。

## Task Route

- Type: `implementation-only change`（P1 需求分歧修复：ORM 纯加性结构变更 + BizModel 解析逻辑；2026-08-12 A 类批量授权 + Q4=(a) 强制实现；越界回落双独立子 agent 批准）
- Owner Docs: `docs/design/projects/use-cases.md`（L1 UC-PRJ-02）+ `docs/design/projects/cost-collection.md`（§2.2/§七）
- Skill Selection Basis: ORM 模型变更 + 增量重生成 + BizModel 解析链（`nop-backend-dev`：模型优先 + 生成链 + 错误码范式）；测试（`nop-testing`：JunitBaseTestCase 直断言 + GraphQL 集成范式）。

## Infrastructure And Config Prereqs

- ORM 变更触发增量重生成：`mvn clean install -DskipTests`（gen-orm.xgen 增量链，对齐 AGENTS.md「ORM 模型变更后用 mvn clean install -DskipTests 触发增量重新生成，不要重跑 nop-cli gen」）。
- 无新 config key（全局默认费率沿用 `erp-prj.default-labor-cost-rate`）。
- 分域验证前置：`mvn install -DskipTests` 后 `mvn test -pl module-projects/erp-prj-service`。

## Execution Plan

### Phase 1 - 载体形态 + tier 归位 + product-scope 确认闭环裁决（Decision）

Status: planned
Targets: `module-projects/model/app-erp-projects.orm.xml`（只读）、`module-projects/erp-prj-service/.../cost/CostRateResolver.java`（只读）、`docs/design/projects/cost-collection.md`
Skill: `nop-backend-dev`
Item Types: `Decision`

- Prereqs: 无

- [ ] Decision：角色费率载体形态（A 类清单「ErpPrjRole 实体 或费率实体」双选项）——候选：A. 新增 `ErpPrjRole` 独立实体（code/name/costRate + 审计列 + UK(code)；`ErpPrjProjectUser.role` 文本按 code 匹配）；B. 用户/角色费率合一实体（如 ErpPrjCostRate 带 scope 判别列）——记录选择、备选、UK/索引设计、与既有 role 文本列的匹配语义（精确匹配 vs 模糊）。
      - Skill: `nop-backend-dev`
- [ ] Decision：tier 归位——单填(timesheet.costRate)与 L1 三级的层级关系（候选：A. 单填最高[既有行为兼容]；B. 单填移除[严格 L1]；C. 单填仅作录入覆盖用户级）——记录选择与对既有测试（testCostRateFromTimesheetOverridesActivityType）的影响。
      - Skill: `nop-backend-dev`
- [ ] Decision：用户级费率查询载体——`ErpPrjProjectUser` 按 (projectId, userId) 查 costRate（userId = ErpMdEmployee id，timesheet.userId 对齐）——记录 null/行缺失跳过语义。
      - Skill: `nop-backend-dev`
- [ ] Decision：`CostRateResolver` 数据访问形态——候选：A. 经既有 `daoProvider.daoFor(ErpPrjRole.class)` 同域站点（R2c 计数 +1 基线登记）；B. 经 IBiz 注入（IBiz-first，零 daoFor 面，对齐 AGENTS.md 平台规则）——记录选择与 checker 影响（对齐 R1.57 D4 先例：引擎注入形态显式裁决）。
      - Skill: `nop-backend-dev`
- [ ] Decision：product-scope 确认闭环——复核 `docs/requirements/product-scope.md` projects 域未裁剪成本率层级 → Q4=(a) 强制实现 + 2026-08-12 A 类裁决声明登记（对齐 RC-R1.56/58/59 范式，审计不复开）。
      - Skill: `none`
- [ ] Add：**orm.xml 变更草案落盘本计划**——按载体形态裁决产出 `ErpPrjProjectUser` 加 costRate 列 + 新增 ErpPrjRole 实体的完整实体段草案文本（列集/propId/UK/to-one/索引，对齐 RC-R1.57 先例 `2026-08-16-1634-1:95-129` 在计划内先出草案）+ `xmllint --noout` well-formed 验证（作为双独立子 agent 的批准对象，**live orm.xml 不先改**）。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 四裁决落地（计划内记录选择/备选/残留风险）；载体形态与 UK 设计可执行
- [ ] product-scope 确认闭环登记（复核未裁剪 + Q4=(a) 声明）
- [ ] orm.xml 变更草案文本落盘本计划 + xmllint --noout 通过（双 agent 批准对象就绪）

### Phase 2 - 双独立子 agent 批准 → live ORM 变更 → 增量重生成（Decision | Add）

Status: planned
Targets: `module-projects/model/app-erp-projects.orm.xml`、`module-projects/erp-prj-service/.../cost/CostRateResolver.java`
Skill: `nop-backend-dev`
Item Types: `Decision | Add`

- Prereqs: Phase 1

- [ ] Decision：**双独立子 agent 批准 checkbox**——**批准先于任何 live orm.xml 写入**：两个独立子代理（fresh session，不重用起草者上下文）对 Phase 1 草案分别检查批准（对齐 RC-R1.57/58 先例 + `ai-autonomy-policy.md:79`：两批准均通过后才可实施——纯加性最小性/propId 顺延/无 NOT NULL 无默认/无 UK 增设/无删除迁移/生成产物一致性/无跨域契约影响）；批准记录落盘本计划；**任一拒绝 → 修改不得实施，按拒绝意见修订草案后重新批准**。
      - Skill: `none`
- [ ] Add：live orm.xml 纯加性变更——按已批准草案写入 `ErpPrjProjectUser` 加 costRate 列（propId 13，可空无默认无索引无 UK）+ 新增 ErpPrjRole 实体（列集 + UK + to-one 引用 + 索引）。
      - Skill: `nop-backend-dev`
- [ ] Add：`mvn clean install -DskipTests` 增量重生成 BUILD SUCCESS——生成产物核对（_gen Entity/XMeta/i18n/DDL 三方言/api bean 仅新增字段或实体）。
      - Skill: `none`
- [ ] Add：`CostRateResolver.resolve` 增用户级/角色级 tier（按 Phase 1 裁决归位）——数据访问形态按 Phase 1 Decision（IBiz-first 或 daoFor 同域站点 [R2c 计数登记]）；javadoc 更新三级语义；单填/全局默认分支保持。
      - Skill: `nop-backend-dev`

Exit Criteria:

- [ ] 双独立子 agent 批准先行完成（记录落盘）→ live orm.xml 写入仅发生在批准后 → 重生成产物核对通过
- [ ] resolve 三级（+裁决归位）解析链落地，既有单填/全局行为零回归

### Phase 3 - 测试 + 文档 + 回填（Add | Proof）

Status: planned
Targets: `module-projects/erp-prj-service/src/test/`、`docs/design/projects/cost-collection.md`、`docs/audits/arm-index.md`、`docs/backlog/requirement-compliance-roadmap.md`、`docs/logs/2026/08-16.md`
Skill: `nop-testing`
Item Types: `Add | Proof`

- Prereqs: Phase 2

- [ ] Proof：新增 `TestErpPrjTimesheetCost` 扩展或新测试类——①用户级费率命中（覆盖活动类型）；②角色级费率命中（低于用户级）；③用户级缺失回落角色级；④三级皆缺回落全局 config；⑤单填覆盖（按裁决）；⑥用户级/角色级 null 跳过；⑦ErpPrjRole CRUD 冒烟（GraphQL save/find）；⑧ERR_COST_RATE_NOT_AVAILABLE 全缺维持。
      - Skill: `nop-testing`
- [ ] Proof：`mvn test -pl module-projects/erp-prj-service` 全绿（既有 138 零回归——testCostRateFromTimesheetOverridesActivityType 等按 Phase 1 裁决可能需调整断言）+ 全量 `mvn test` + `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh`（零漂移或 baseline-raise 登记 per-site 证据）。
      - Skill: `nop-testing`
- [ ] Add：owner doc 回填——`cost-collection.md §2.2` Non-Goal→已实现（三级载体 + 解析链 + Phase 1 裁决注记）+ §七 规则 3 注记；arm-index P1-RC-048 → done (RC-R1.60)（修复记录 + 历史保留）+ roadmap RC-R1.60 → done ✅ + 本日志条目。
      - Skill: `none`

Exit Criteria:

- [ ] 新测试全绿 + 既有测试零回归（或按裁决显式调整并记录）+ 全量验证命令通过（成功模式）；任一失败模式须修复或登记后才勾选
- [ ] owner doc/arm-index/roadmap 回填完成

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（`ses_ff560797dffeslky1uycYJQZ0O`，独立 general 子代理，新会话冷重播无起草者上下文）— 1 Blocker / 0 Major / 3 Minor。**B1（已修正）**：Phase 2 顺序违反 `ai-autonomy-policy.md:79`「两批准均通过后才可实施」+ 自引的 RC-R1.57 先例——live orm.xml 写入须**后于**双独立子 agent 批准，且 Phase 1 须先产出**在计划内的 orm.xml 变更草案文本 + xmllint 验证**作为批准对象（对齐 R1.57 `2026-08-16-1634-1:95-129/:150` + R1.58 范式）——已重构：Phase 1 增「orm.xml 变更草案落盘本计划 + xmllint」项，Phase 2 批准 checkbox 移至首项并明示「批准先于任何 live orm.xml 写入」，live 写入/重生成/resolver 后置；Minor：① roadmap 头行号 :33-35→:33-47（:39 行 + Q3 :24）（已修正）；② resolver 数据访问形态未裁决——已补 Phase 1 Decision（IBiz-first vs daoFor 同域站点 + R2c 影响）；③ N=3 执行顺序确认——R1.61/62 各自解锁依赖（A4.2.119/G9 族），R1.60 独立无依赖，排序合规。12/12 实仓抽查全 PASS（resolve 三级现状/ProjectUser propId 1-12 next-free 13/无 ErpPrjRole/cost-collection §2.2 Non-Goal/use-cases:38/7 测试/A 类清单 :39/arm-index P1-RC-048 :219/文件名格式/反松弛/单结果表面）。
- Independent draft review iteration 2: `acceptable as-is`（`ses_ff5579e63ffeKGHWITGLh8vkd1`，独立 general 子代理，新会话冷重播无起草者上下文）— 0 Blocker / 0 Major / 2 新 Minor（非阻塞引用精度 nit，已就地修正）：B1 全部修正核实 FIXED（Phase 1 在计划内草案 + xmllint :71 + Phase 2 首项批准 checkbox「批准先于任何 live orm.xml 写入」:89 + live 写入后置 :91/:100 镜像 R1.57:150）+ 3 项 iteration-1 Minor 全 FIXED；新 Minor m4（Q4=(a) 引用 :14 行号 :33-35→:33-47 已修正）+ m5（ai-autonomy-policy 补 :79 行号已修正）。格式/范围/反松弛/结束证据全 PASS。草案审查收敛 → `Plan Status: draft → active`。

## Closure Gates

- [ ] 范围内行为完成（三级费率降级运行时成立 + ORM 载体落地）
- [ ] 相关文档对齐（cost-collection.md + arm-index + roadmap）
- [ ] 已运行验证（`mvn test -pl module-projects/erp-prj-service` + 全量 `mvn test` + `mvn clean install -DskipTests` + `bash docs/audits/nop-compliance-checker.sh`）
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 角色管理 UI / 角色 CRUD 业务页面

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: ErpPrjRole 实体经标准 CRUD 生成（dao/meta/web 全链），业务 UI 打磨超出 L1 断言面
- Successor Required: `no`

### 费率历史版本化 / 生效日期窗口

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: L1 仅要求三级优先级解析，无历史版本/生效窗口断言；startDate/endDate 在 ProjectUser 已存在可承载成员窗口
- Successor Required: `yes`（触发条件：运营要求费率变更留痕/按历史期间回溯成本时，按 ORM ask-first 流程立项）

### 多币种费率

- Classification: `watch-only residual`
- Why Not Blocking Closure: 与 P2-RC-050/P1-MA1-010 多币种投影同域协同；费率载体先行落地，多币种折算归既有多币种 successor 族
- Successor Required: `yes`（触发条件：多币种费率需求落地时，与 P1-MA1-010 族协同立项）

## Closure

Status Note: <why the plan can close>

Closure Audit Evidence:

- Auditor / Agent: <independent auditor or independent subagent>
- Evidence: <task id / log link / walkthrough record>

Follow-up:

- <仅非阻塞跟进项目；已确认的缺陷不得出现在此处>
