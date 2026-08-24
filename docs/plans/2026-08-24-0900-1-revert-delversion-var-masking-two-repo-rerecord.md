# 2026-08-24-0900-1-revert-delversion-var-masking-two-repo-rerecord 回退 nop-entropy delVersion 快照变量屏蔽 + 双仓快照修复

> Plan Status: draft
> Last Reviewed: 2026-08-24
> Source: 用户裁决（2026-08-24 会话）：nop-entropy commit `b7d7010a19` 为 `AutoTestHelper.isVarCol` 增加 deleteVersionProp 列自动变量化规则，经技术核实为错误决策（delVersion 是确定性值），要求回退并修复两仓全部受影响测试
> Related: `docs/plans/2026-07-09-2330-1-xwf-approval-browser-e2e-feasibility.md`（无关，仅时间相邻）；本仓当日 xwf 修改重录期间发现的 sales `erp_md_subject.csv` 变量下标漂移即本缺陷的直接产物
> Audit: required（跨仓保护区域：双独立子 agent 批准）

## Current Baseline

### 缺陷本体（nop-entropy）

- `nop-autotest/nop-autotest-core/src/main/java/io/nop/autotest/core/execute/AutoTestHelper.java:32-34`（`isVarCol` 方法内）存在以下判断：

```java
if (propId == entityModel.getDeleteVersionPropId())
    return true;
```

- 该判断由 commit `b7d7010a19`（2026-07-15，作者 canonical，message「chore: 同步tcc重构、WebPagesTest生成模板及其他未提交变更」）引入，**+3 行无任何理由说明**：commit message 未提及、`CHANGELOG.md` 无记录、`ai-dev/logs/` 前后日志零提及、无配套测试。
- `isVarCol` 的消费方：`EntityRow`（运行时行捕获）与 `TagVarCollector`（录制保存/校验比对时的变量替换）。命中列在快照 CSV 中被替换为 `@var:{ShortName}@{colName}` 形式变量。

### 为什么是错误决策（技术论证）

- **delVersion 不非确定**：`nop-persistence/nop-orm/src/main/java/io/nop/orm/id/OrmEntityIdGenerator.java:57` 在插入时将 deleteVersionProp 硬编码初始化为 `0`，仅逻辑删除时递增。与 seq 主键（消费全局序列、每次运行值不同）、createTime/updateTime（墙钟）、clock 标记列（可变时钟）本质不同——后三类屏蔽是必要的，delVersion 屏蔽没有非确定性要消除。
- **代价一（丢断言能力）**：屏蔽后快照永久失去对「delVersion==0」及逻辑删除后精确递增值的校验，任何错误递增都不会被快照比对捕获。
- **代价二（变量别名噪声）**：`AutoTestVars.VarsMap.addVar` 按 putIfAbsent 分配名字下标，捕获顺序变化即产生 `delVersion` ↔ `delVersion_1` 的伪 diff。实证：本仓 2026-08-24 xwf 修改重录时 `module-sales/.../TestErpSalReceiptWorkflowApproval` 的 `erp_md_subject.csv` 两行科目一致从 `@var:ErpMdSubject@delVersion` 漂移到 `@var:ErpMdSubject@delVersion_1`，数据语义零变化但污染审查。

### 受影响面（实测盘点）

| 仓库 | 影响面 |
|------|--------|
| nop-entropy 自身 | **0 个受影响测试**——全仓 `_cases` 快照 CSV 零处引用 `@var:*@delVersion`；无针对 `isVarCol` 的单元测试 |
| nop-app-erp | **125 个测试类 / 约 2,061 个测试方法 case 目录 / 19,039 个快照 CSV** 引用 `@var:*@delVersion`，分布 20 个模块（finance 2952 / assets 2706 / purchase 2311 / manufacturing 1999 / sales 1945 / inventory 1289 / hr 820 / drp 681 / contract 747 / projects 608 / quality 606 / crm 419 / maintenance 389 / logistics 322 / aps 185 / cs 209 / master-data 223 / b2b 60 / notify 59 / app-erp-all 509，按文件数） |

### 重录机制（已核实）

- 平台提供全局开关 `AutoTestConfigs.CFG_AUTOTEST_FORCE_SAVE_OUTPUT`（`nop.autotest.force-save-output`）：JVM 参数 `-Dnop.autotest.force-save-output=true` 可令所有 `JunitAutoTestCase` 保存输出而非校验，**无需逐类修改注解**。
- 回退后旧行为恢复：delVersion 列不再进入 varCols，新录制 CSV 中该列为字面值（活行为 `0`）。
- 回退后旧快照必然失配（期望 `@var:...` 字符串 vs 实际字面值），因此全量重录不可回避。

## Goals

- nop-entropy：删除 `isVarCol` 中 deleteVersionPropId 判断（精确回退 `b7d7010a19` 的 +3 行），平台自身测试全绿。
- nop-app-erp：以全局 force-save-output 开关批量重录全部受影响快照；逐模块 diff 审计证明变更**仅限** delVersion 单元格转换（`@var:*@delVersion*` → 字面值），无其他语义漂移混入。
- 双仓验证全绿并在各自日志留痕；平台行为变化登记至本仓文档，消除未来调试认知差。

## Non-Goals

- 不修改其余 var 规则（seq/var/clock/createTime/updateTime 四类屏蔽保持不动）。
- 不使用 `CaseDataMigration` 迁移工具做存量快照文本变换（无法保证逻辑删除场景下递增值正确性，重录才是真值来源）。
- 不修改任何应用测试类的 Java 注解或断言逻辑（重录走全局开关）。
- 不触及 E2E/Playwright 层（该层不消费 JUnit 快照机制）。
- 不在本计划内向平台上游提 PR/issue（登记为 Deferred successor）。

## Task Route

- Type: `bug investigation + implementation-only change`
- Owner Docs: `../nop-entropy/ai-dev/logs/`（平台侧变更日志义务）；本仓 `docs/testing/e2e-runbook.md`（快照机制说明段，如含 JUnit 快照描述则同步）、`docs/testing/known-good-baselines.md`（验证基线更新）
- Skill Selection Basis: `nop-testing`（RECORDING/CHECKING/force-save-output 机制与快照目录结构，Phase 2/3 核心）；`nop-backend-dev` 不匹配（无 BizModel/业务代码变更）；Phase 1 平台回退本身为单点精确删除，Skill: none 足够

## Infrastructure And Config Prereqs

- nop-entropy 本地构建安装：`cd ../nop-entropy && mvn clean install -DskipTests` 后，nop-app-erp 才能消费回退后的 `nop-autotest-core` 构件（Maven 本地仓库依赖顺序）。
- 重录 JVM 参数：`mvn test -Dnop.autotest.force-save-output=true`（surefire argLine 传递由平台测试框架自动读取 AppConfig 系统属性，无需额外配置）。
- 回滚策略：两仓变更均为 git 追踪；任一阶段失败可 `git checkout -- <paths>` 恢复（nop-entropy 恢复单文件，本仓按模块恢复 `_cases`），平台构件回滚通过重新 install 旧代码实现。

## Execution Plan

### Phase 1 — 保护区域批准 + nop-entropy 回退与自证

Status: planned
Targets: `../nop-entropy/nop-autotest/nop-autotest-core/src/main/java/io/nop/autotest/core/execute/AutoTestHelper.java`、`../nop-entropy/ai-dev/logs/2026-08-24.md`
Skill: none（单点精确删除，无技能路由匹配项）

- Item Types: `Fix | Proof`
- Prereqs: 双独立子 agent 批准（下方 checkbox，保护区域硬门控）

- [ ] **双独立子 agent 批准**（外部仓库代码保护区域，auto + dual-agent-approval）：两个 fresh-session 子代理分别独立审查本计划 §Current Baseline 技术论证、回退 diff（精确 -3 行）、爆炸半径评估与回滚策略，双双通过后方可实施；批准记录（agent 指针 + 结论）落盘本节
      - Skill: none
- [ ] `Fix`：删除 `AutoTestHelper.isVarCol` 中 `getDeleteVersionPropId` 判断块（3 行），文件其余内容零改动
      - Skill: none
- [ ] `Proof`：nop-entropy 自身回归——`cd ../nop-entropy && mvn clean install -DskipTests` BUILD SUCCESS + `mvn test -pl nop-autotest/nop-autotest-core` 全绿 + 受影响面声明复核（`rg -rl "@var:[A-Za-z]+@delVersion" --glob '*.csv'` 仍应为 0，证明平台自身无快照需重录）
      - Skill: none
- [ ] 在 `../nop-entropy/ai-dev/logs/2026-08-24.md` 记录回退（日期/commit 反向引用 b7d7010a19/理由/影响面）
      - Skill: none

Exit Criteria:

- [ ] 双批准记录存在于本计划 Phase 1 内，两个子代理结论均为通过
- [ ] nop-entropy 构建 + autotest 模块测试全绿；`git diff` 仅含目标文件 -3 行
- [ ] 平台侧日志条目落盘

### Phase 2 — 先导模块重录 + diff 审计协议定标

Status: planned
Targets: `module-master-data/`（受影响文件数最小域之一，223 CSV）+ `module-sales/`（已知 `_1` 漂移实证域，用于闭环验证）
Skill: `nop-testing`

- Item Types: `Proof | Decision`
- Prereqs: Phase 1 完成（本地仓库已装回退后构件）

- [ ] `Decision`：diff 审计协议定标——每模块重录后执行 `git diff -- '<module>/**/_cases/**'`，逐 hunk 核对变更**当且仅当**为以下两类之一：① delVersion 列单元格 `@var:X@delVersion(_N)?` → 字面值（预期 `0`）；② 该列从变量引用变为空值/保持原字面（若实体未启用逻辑删除）。任何其他单元格变更（状态列/金额列/其他变量名下标漂移）→ 单类隔离调查（`git checkout` 还原该类后用逐类 `forceSaveOutput` 注解路线单独处理），不得静默接受。协议与本 Decision 结论记入本节
      - Skill: `nop-testing`
- [ ] `Proof`：先导重录 master-data —— `mvn test -pl module-master-data -Dnop.autotest.force-save-output=true`，随后 CHECKING 复跑（同命令去开关）全绿 + diff 审计通过
      - Skill: `nop-testing`
- [ ] `Proof`：sales 域重录 + 专项确认 `erp_md_subject.csv` 的 `delVersion_1` 别名噪声消失（变量下标漂移类别根除的实证）
      - Skill: `nop-testing`

Exit Criteria:

- [ ] diff 审计协议已成文（本计划 §Phase 2 Decision 记录）
- [ ] master-data + sales 两域 CHECKING 全绿且 diff 审计零意外变更

### Phase 3 — 全量批量重录（18 模块）

Status: planned
Targets: 其余 18 个受影响模块（按依赖序：core 域先行 finance/inventory/purchase/sales→manufacturing/assets/projects/maintenance/quality→hr/crm/cs/aps/logistics/b2b/contract/drp/notify→app-erp-all 最后）
Skill: `nop-testing`

- Item Types: `Fix | Proof`
- Prereqs: Phase 2 协议定标完成

- [ ] `Fix`：按批重录——每批 3-5 模块执行 `mvn test -pl <modules> -Dnop.autotest.force-save-output=true`，每批完成后立即执行 diff 审计（Phase 2 协议），通过后方进入下一批；意外变更按协议单类隔离
      - Skill: `nop-testing`
- [ ] `Proof`：app-erp-all 收尾批重录 + 审计（该聚合模块含跨域集成用例 509 文件，置末位避免上游域种子语义中途变化）
      - Skill: `nop-testing`
- [ ] `Proof`：全仓受影响面清零复核——`rg -rl "@var:[A-Za-z]+@delVersion" module-* app-erp-all --glob '*.csv' | wc -l` 结果为 0
      - Skill: `nop-testing`

Exit Criteria:

- [ ] 20 模块全部重录完成且逐批 diff 审计通过
- [ ] delVersion 变量引用清零 grep 门控通过

### Phase 4 — 全量验证、文档对齐与收尾

Status: planned
Targets: 双仓全量验证、`docs/testing/e2e-runbook.md`、`docs/testing/known-good-baselines.md`、`docs/logs/2026/08-24.md`
Skill: `nop-testing`

- Item Types: `Proof | Add`
- Prereqs: Phase 3 完成

- [ ] `Add`：`e2e-runbook.md` 快照相关段落补注「delVersion 列自 2026-08-24 起记录字面值而非变量（平台回退 b7d7010a19），逻辑删除递增值恢复快照强校验」——消除未来录制/比对认知差
      - Skill: none
- [ ] `Proof`：本仓全 reactor `mvn clean install -DskipTests` BUILD SUCCESS + `mvn test` 全量（对照 `known-good-baselines.md` 既有预存失败口径，新增失败为零）
      - Skill: `nop-testing`
- [ ] `Add`：`known-good-baselines.md` 登记本次全绿基线（含重录说明与日期）；`docs/logs/2026/08-24.md` 聚合条目
      - Skill: none

Exit Criteria:

- [ ] 双仓全量验证绿；基线与双仓日志落盘
- [ ] runbook 注记合入

## Draft Review Record

- Independent draft review iteration 1: （待独立草案审查——本计划触及跨仓保护区域，草案审查通过与双独立子 agent 批准均为实施前置）

## Closure Gates

- [ ] 范围内行为完成（平台回退 + 20 模块重录 + 清零 grep 门控）
- [ ] 相关文档对齐（runbook 注记 + known-good-baselines + 双仓日志）
- [ ] 已运行验证：nop-entropy `mvn clean install -DskipTests` + autotest 模块测试；本仓全 reactor install + `mvn test` 全量
- [ ] 无范围内项目降级为 deferred/follow-up
- [ ] 独立草案审查已完成并记录（含双独立子 agent 保护区域批准）
- [ ] 文本一致性已验证：状态、阶段、门控和日志都一致
- [ ] 结束审计由独立子代理（新会话）执行；执行者未自我审计且未将此留为 `[ ]` 作为人工门控占位符
- [ ] 结束证据存在于文件中

## Deferred But Adjudicated

### 向平台上游反馈（PR/issue 至 canonical_entropy）

- Classification: `out-of-scope improvement`（跨仓外部协作，不受本仓控制）
- Why Not Blocking Closure: 本地回退已达修复目的；上游同步冲突风险由后续 sync 时人工解决
- Successor Required: `yes`——触发条件：下次从上游 nop-entropy 同步代码时，或平台仓库建立 issue 通道时，提交回退说明（引本计划为依据），防止上游再次引入同型变更

## Closure

Status Note: （收尾时填写）

Closure Audit Evidence:

- Auditor / Agent: （待独立结束审计）
- Evidence: （待填）
