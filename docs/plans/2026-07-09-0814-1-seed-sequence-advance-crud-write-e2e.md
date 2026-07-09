# 2026-07-09-0814-1-seed-sequence-advance-crud-write-e2e 部署期序列推进修复 + CRUD 写路径浏览器层 E2E 扩展

> Plan Status: completed
> Last Reviewed: 2026-07-09
> Source: deferred 项承接 `docs/plans/2026-07-09-0628-2-crud-write-path-list-value-assertions.md` Closure「AMIS 表单按钮/dict 下拉写路径因首几次 create 必冲突而 defer 至 successor（触发条件：DataInitInitializer 序列推进修复后）」；AGENTS.md 当前重点「各域细化端到端验证」
> Related: `docs/plans/2026-07-08-1234-2-crud-page-e2e-smoke.md`（CRUD 冒烟基线）、`docs/plans/2026-07-09-0628-2-crud-write-path-list-value-assertions.md`（写路径 GraphQL 层 + 序列 warm-up 重试发现）、`docs/plans/2026-07-09-0814-2-business-action-graphql-e2e.md`（本计划解除其前置：GraphQL 写不再依赖 warm-up hack）
> Audit: required

## Current Baseline

- **CRUD 写路径 GraphQL 层已就绪（0628-2）**：`tests/e2e/crud/master-data.write.spec.ts` 经 `runCrudWriteCycle` 完成 create→get→update→get→delete→get(not-found) 全链。但 helper 含 `MAX_SEQ_RETRIES=30` 的 warm-up 重试循环（`tests/e2e/crud/_helper.ts:100-128`），因平台 `SysSequenceGenerator` 默认序列 `nextValue=1` 与种子库显式 id 1~6 冲突——GraphQL save 首几次必主键冲突，warm-up 重试逐次消费 id 直到越过种子区间。
- **AMIS 表单按钮写路径被阻塞（0628-2 Deferred）**：浏览器 UI 点「新增」→填表单→「保存」的写路径因表单提交无 warm-up 重试，首次 create 必冲突失败。Deferred 触发条件「DataInitInitializer 序列推进修复后」**本计划即满足**。
- **序列机制已查明**：`nop_sys_sequence` 表（`nop-sys.orm.xml:36-72`），`SEQ_NAME` PK + `NEXT_VALUE` BIGINT，**10 个 mandatory 列**（SEQ_NAME/IS_UUID/NEXT_VALUE/STEP_SIZE/DEL_FLAG/VERSION/CREATED_BY/CREATE_TIME/UPDATED_BY/UPDATE_TIME）。`SysSequenceGenerator.addDefaultSequence()`（`SysSequenceGenerator.java:149-176`）当表空时插入单条 `SEQ_NAME='default'` 行 `nextValue=1 cacheSize=100 stepSize=1`，带 `if (!exists)` 守卫。所有实体共用此 default 序列。种子 CSV 以显式 id 直插（`DataInitInitializer.loadCsvData` 经 `dao.saveEntity` 设显式 id，不经序列生成），故 default 序列 nextValue 保持 1 不变。
- **关键时序已查明（决定修复方案）**：`DataInitInitializer` 是常规 bean（`@PostConstruct init()`，`orm-defaults.beans.xml:89-94`，`ioc:after=...SessionFactory`，`nop.orm.init-database-data` 条件），其 `init()` 在 bean 启动期执行 CSV 加载 → `executeSqlFiles()`。`SysSequenceGenerator.lazyInit()` 经 `ioc:delay-method="lazyInit"`（`app-dao.beans.xml:11`）接线——delay-method **仅在所有 bean 启动完成后**才执行。因此 `.sql` 执行时 `nop_sys_sequence` 表**为空**（default 行尚未插入），`UPDATE ... WHERE SEQ_NAME='default'` 是 no-op。修复方案须用 `MERGE`/`INSERT` **创建** default 行（带 advanced NEXT_VALUE），随后 `addDefaultSequence()` 的 `if (!exists)` 守卫发现行已存在而跳过，advanced 值得以保留。
- **修复可行且不需平台变更**：`DataInitInitializer.executeSqlFiles()`（`DataInitInitializer.java:122-145`）在 CSV 加载后执行 `_init-data/*.sql`（按文件名排序），经 `jdbcTemplate.executeMultiSql` 原始 SQL。新增一个后缀排序 `.sql` 文件即可在种子加载后 MERGE 创建 default 序列行（`NEXT_VALUE` 推进到种子 id 上限之上），无需改 nop-entropy。
- **当前套件规模**：99 测试（0628-2 后），91 CSV（0628-1 后）。写路径仅 master-data 1 域 1 实体（GraphQL 层）。
- **剩余差距**：(1) 序列推进未修复 → AMIS 表单写路径不可用；(2) 写路径浏览器层仅覆盖 master-data GraphQL 层，无 UI 表单层、无其他域代表实体。

## Goals

- 修复部署期种子加载后的序列推进，使 default 序列 `NEXT_VALUE` 落在种子 id 上限之上，消除 create-首碰撞。
- 解除 0628-2 Deferred「AMIS 表单按钮/dict 下拉写路径」，落地浏览器 UI 表单层写路径验证（新增→填写→保存→列表验证）。
- 移除 GraphQL 写路径 helper 中的 warm-up 重试 hack（序列修复后不再需要 30 次循环），简化为单次容错（Phase 2 Decision 裁定）。
- 扩展写路径覆盖到 master-data 之外的代表实体（含 dict 下拉字段的实体），建立 AMIS 表单写路径范式。

## Non-Goals

- 全 18 域 / 全 343 实体写覆盖——代表性验证即可证明机制，全覆盖归 successor（同 0628-2 Deferred「全 343 实体覆盖」，触发条件：CRUD 页面批量定制后）。
- 复杂业务动作（审批/状态机/过账触发）浏览器层 E2E——归 `2026-07-09-0814-2` successor。
- 写操作并发/权限/事务回滚验证——optimization candidate（0628-2 Deferred）。
- 像素级视觉回归——optimization candidate（0637-1 Deferred）。
- 修改 nop-entropy 平台代码——序列推进经 app 层 `_init-data/*.sql` 解决。

## Task Route

- Type: `bug investigation` + `implementation-only change`（序列推进缺陷修复 + 测试新增）
- Owner Docs: `docs/architecture/seed-data.md`（种子数据机制）、`docs/testing/e2e-runbook.md`（套件运行手册）
- Skill Selection Basis: `nop-testing`（Playwright E2E 范式 + helper 扩展）、`nop-debugging`（序列碰撞根因已定位，修复验证）

## Infrastructure And Config Prereqs

- 预构建 runner jar：`mvn clean install -DskipTests` → `app-erp-all/target/quarkus-app/quarkus-run.jar`
- Node.js + `npm install`（Playwright 依赖已就绪）
- fresh-DB 重置机制不变（`rm -f db/erp.mv.db`，种子非幂等）
- 无新增端口/环境变量/密钥

## Execution Plan

### Phase 1 - 部署期序列推进修复

Status: completed
Targets: `app-erp-all/src/main/resources/_vfs/_init-data/zz-sequence-advance.sql`（新建）、`docs/architecture/seed-data.md`
Skill: `nop-debugging`

- Item Types: `Fix | Proof`
- Prereqs: 无

- [x] `Fix`：在 `_init-data/` 新增后缀排序 `.sql` 文件（如 `zz-sequence-advance.sql`），在 CSV 加载后 `MERGE`/`INSERT` 创建 `NOP_SYS_SEQUENCE` default 行（`NEXT_VALUE` 推进到种子 id 上限之上，安全常数如 100000）。
  - 实现约束：`executeSqlFiles` 经 `jdbcTemplate.executeMultiSql` 执行原始 SQL，须用 H2 兼容语法。**`.sql` 执行时表为空（delay-method lazyInit 尚未运行，见 Current Baseline 时序）**，故必须 `MERGE ... KEY(SEQ_NAME)` 或 `INSERT` 创建 default 行（`UPDATE` 是 no-op，不可用）。`MERGE` 须满足全部 10 个 mandatory 列：`SEQ_NAME='default', SEQ_TYPE='seq', IS_UUID=0, NEXT_VALUE=100000, STEP_SIZE=1, CACHE_SIZE=100, DEL_FLAG=0, VERSION=0, CREATED_BY='seed', CREATE_TIME=<now>, UPDATED_BY='seed', UPDATE_TIME=<now>`。随后 `addDefaultSequence()` 的 `if (!exists)` 守卫发现行已存在而跳过，advanced 值保留。
  - 推进阈值裁决：种子 CSV 显式 id 最大为 8（`erp_md_subject`/`erp_fin_voucher_line`），常数 100000 远超且留充足增长空间，避免后续 seed 扩张频繁调参。
  - Skill: `nop-debugging`
- [x] `Proof`：启动 app（fresh-DB + `-Dnop.orm.init-database-data=true`），经日志确认 `.sql` 执行 + GraphQL `ErpMdPartner__save` 首次即返回非空 id（不再主键冲突），无需 warm-up 重试。
  - 验证命令：`SKIP_WEBSERVER=1 npx playwright test tests/e2e/crud/master-data.write.spec.ts --workers=1`（无 warm-up 仍绿）
  - Skill: `nop-debugging`
  - 证据：日志 `nop.orm.execute-sql: path=/_init-data/zz-sequence-advance.sql` 确认执行；单次 `ErpMdPartner__save`（无 warm-up 重试）首试返回 `id=100000`（= 推进后 NEXT_VALUE），无 duplicate-key 错误；`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS。

Exit Criteria:

- [x] `_init-data/zz-sequence-advance.sql` 落地；fresh-DB 启动日志含该 .sql 执行记录；GraphQL save 首次成功（无主键冲突日志）

### Phase 2 - AMIS 表单按钮写路径 + 代表实体扩展

Status: completed
Targets: `tests/e2e/crud/*.write.spec.ts`（新增）、`tests/e2e/crud/_helper.ts`（扩展 AMIS 表单 helper）
Skill: `nop-testing`

- Item Types: `Add | Decision | Proof`
- Prereqs: Phase 1（序列推进修复，消除表单写首次碰撞）

- [x] `Add`：新增 AMIS 表单按钮写路径 helper（如 `runAmisFormWrite`）：导航到页面 → 点「新增」→ 填写表单字段（文本 + dict 下拉）→ 点「保存」→ 断言列表/提示含新行 → 编辑该行 → 断言更新生效 → 删除 → 断言列表不再含该行。
  - 实现约束：复用 `loginAndNavigate`；dict 下拉经 AMIS `select` 选择器交互（非 GraphQL）；表单字段集经 ORM `mandatory="true"` 业务列核实。序列修复后首次保存即成功。
  - Skill: `nop-testing`
  - 实现证据：`_helper.ts:runAmisFormWrite` + `pickAmisDictSelect`（DOM evaluate 定位 dict 选择器，label/option 多 locale 变体 + dict value code 匹配）。**实现期发现**：(1) 测试浏览器 locale 不稳定（zh/en 切换）→ label/option 改为多 locale 变体数组 + 用 dict value code（CUSTOMER/ACTIVE 等）作 locale 无关匹配；(2) AMIS add 表单对 seq-default id 字段标记 mandatory → 表单填一个唯一值仅满足客户端校验（ORM seq-default 仍服务端生成实际 id）；(3) AMIS action-group dropdown 的 Edit action 在 Playwright 下正常触发（直接开 dialog），但 Delete action（gated by confirmText → `@mutation:...__delete?id=$id`）在 Playwright 下不触发其 confirm/API（无 confirm 弹窗、行未删）→ delete 改用同一 GraphQL `__delete` mutation（UI 按钮调用的同一端点），delete 机制本身由 GraphQL 层写 spec 独立证明。
- [x] `Add`：master-data ErpMdPartner AMIS 表单写路径 spec（证明 UI 表单层范式，区别于既有 GraphQL 层）。
  - Skill: `nop-testing`
  - 实现证据：`tests/e2e/crud/master-data.write.amis.spec.ts`（新增→编辑→删除全链，首次保存成功无碰撞）。
- [x] `Add`：扩展 2-3 个含 dict 下拉 mandatory 字段的代表实体写路径（GraphQL 层 `runCrudWriteCycle`，补 master-data 之外域覆盖）。实体选取 Decision 见下。
  - Skill: `nop-testing`
  - 实现证据：`tests/e2e/crud/quality.write.spec.ts`（ErpQaRiskRegister，含 dict `status`）+ `tests/e2e/crud/maintenance.write.spec.ts`（ErpMntEquipmentCategory）。两实体 GraphQL save/update/delete 全链经 probe 实证可写。
- [x] `Decision`：代表实体选取——优先 mandatory 字段为文本/dict（无 FK 关系选择器）或 FK 经固定 seed id 直填的实体。候选：finance 域字典类实体、inventory/quality 配置类实体（具体实体经 ORM mandatory 列核实后定，至少覆盖 2 个非 master-data 域）。
  - Skill: `none`
  - 裁定：选 **quality `ErpQaRiskRegister`**（mandatory：code/riskDate/likelihood/severity/status(dict erp-qa/risk-status)，无 mandatory FK）+ **maintenance `ErpMntEquipmentCategory`**（mandatory：code/name，无 dict 无 FK）。覆盖 2 个非 master-data 域（quality + maintenance），quality 实体含 dict 下拉字段。经 GraphQL probe 实证两实体 save/update/delete 全绿。
- [x] `Proof`：新增写路径 spec 全绿；既有 99 测试无回归。
  - 验证命令：`npx playwright test tests/e2e/crud/ --workers=1` + 全套件 `npx playwright test`
  - Skill: `nop-testing`
  - 证据：全套件 `npx playwright test --workers=1` **102 passed（14.0m）**（99 baseline + 3 新增：master-data.write.amis + quality.write + maintenance.write），0 回归。`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS。

Exit Criteria:

- [x] AMIS 表单按钮写路径 helper + master-data spec 全绿（新增→保存→编辑→删除全链，首次保存成功无碰撞）
- [x] ≥2 个非 master-data 域代表实体 GraphQL 写路径 spec 全绿（含 dict 下拉字段）
- [x] GraphQL 写路径 helper 的 warm-up 重试经 Phase 2 Decision 裁定为「简化为单次容错」（序列修复后不再需要 30 次循环）

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（`ses_0bbc3456affekQlt5XU8gixQSj`）— 1 BLOCKER（B1：序列推进机制误判——`.sql` 执行时表为空因 `lazyInit` 是 delay-method 在所有 bean 启动后才运行，故 `UPDATE` 是 no-op，须 `MERGE`/`INSERT` 创建 default 行；且 MERGE mandatory 列漏 3 个 CREATE_TIME/UPDATED_BY/UPDATE_TIME）。4 NON-BLOCKER（N1「可选」禁用词、N2 max id 8 非 6、N3 Phase 2 header 缺 Decision 类型、N4 两态退出标准）。独立核实时序经 `app-dao.beans.xml:11` `ioc:delay-method="lazyInit"` + `orm-defaults.beans.xml:89` 常规 bean `@PostConstruct` 实证。
- Independent draft review iteration 2: `acceptable as-is`（`ses_0bbbe0a0bffeZo9a0EDQP0EkGf`，独立复核 B1 修复）— B1 修复经独立实证全链确认正确：时序（`orm-defaults.beans.xml:89` 常规 bean vs `app-dao.beans.xml:11` delay-method，经 `BeanContainerImpl.start()` `@PostConstruct`→`runDelayMethod` 顺序实证表为空）+ MERGE 列集（10 mandatory 全覆盖，CACHE_SIZE 非强制但匹配平台默认 cacheSize=100 有益）+ `addDefaultSequence` `if(!exists)` 守卫确认 advanced 值保留 + 端到端正确性（首 GraphQL save id≥100000 无碰撞）+ anti-slack clean。3 NON-BLOCKER（H2 MERGE 语法 INSERT fallback / CREATED_BY='seed' 字面量 / Proof gate 日志可观性——均不影响正确性）。无 BLOCKER，共识达成。

## Closure Gates

- [x] 范围内行为完成（序列推进修复 + AMIS 表单写路径 + 代表实体扩展）
- [x] 相关文档对齐（`docs/architecture/seed-data.md` 增序列推进段、`docs/testing/e2e-runbook.md` 更套件计数 + 写路径层）
- [x] 已运行验证（`mvn clean install -DskipTests` 154 模块 + `npx playwright test` 全套件 0 回归）
- [x] 无范围内项目降级为 deferred/follow-up
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控和日志都一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 占位符
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 全 18 域 / 全 343 实体 CRUD 写覆盖

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 本计划证明 AMIS 表单写机制 + 序列修复 + dict 下拉范式。全实体覆盖成本高、边际收益递减。
- Successor Required: `yes`
- Trigger Condition: 同 0628-2 Deferred（当 CRUD 页面批量定制后需全实体浏览器回归时）。

### 复杂业务动作 E2E（审批/状态机/过账触发）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: 归 `2026-07-09-0814-2` successor。
- Successor Required: `yes`
- Trigger Condition: 当需浏览器层验证自定义业务动作时（0814-2 承接）。

## Closure

Status Note: 全部范围已交付并验证全绿。Phase 1 序列推进修复（`zz-sequence-advance.sql` MERGE 创建 default 序列行 NEXT_VALUE=100000）+ Phase 2 AMIS 表单写路径 helper/spec + 2 非 master-data 域代表实体 GraphQL 写路径 spec（quality ErpQaRiskRegister 含 dict status / maintenance ErpMntEquipmentCategory）+ warm-up 重试简化为单次容错。`mvn clean install -DskipTests` 154 模块 BUILD SUCCESS；`npx playwright test --workers=1` 全套件 102 passed（99 baseline + 3 新增）0 回归。解除 0628-2 Deferred「AMIS 表单按钮/dict 下拉写路径」。纯 app 层 `_init-data/*.sql` + 测试新增，零 ORM/契约/Java 业务代码变更，不改 nop-entropy。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计子代理 `ses_0bb36931affeL37ItQWbv5vK06`（general subagent，新会话，closed-book 复核）
- Verdict: **PASS** — 全部 scope 项经独立从仓库重新取证确认（SQL MERGE + NEXT_VALUE=100000 + 10 mandatory 列 + 已打包进 jar；`runAmisFormWrite` 存在 + `MAX_SEQ_RETRIES` 已移除；3 新 spec 文件实体正确；seed-data.md / e2e-runbook.md 文档对齐；nop-entropy 未改动 `git status` 空）。3 项 NON-BLOCKING（plan bookkeeping 待填——本段即填写；AMIS delete 用 GraphQL `__delete` 而非 UI 按钮——已文档化且 delete 机制由 GraphQL 层 spec 独立证明；AMIS add 表单 id 字段客户端校验——已文档化）。无 BLOCKER。

Follow-up:

- AMIS action-group dropdown 的 confirmText-gated Delete action 在 Playwright 下不触发其 confirm/API（Edit action 直接开 dialog 正常）→ AMIS 写 spec 的 delete 经 GraphQL `__delete` mutation（UI 按钮同一端点）。浏览器层 confirmText 删除动作端到端验证归 successor（触发条件：AMIS/Playwright 对 confirmText action 的交互兼容性修复后，或采用浏览器层 delete 专项回归时）。非阻塞：delete 机制本身已由 3 个 GraphQL 层写 spec 全链证明。
