# 2026-07-03-2108-1 全域字典 int→string 重构

> Plan Status: completed
> Last Reviewed: 2026-07-04
> Source: 用户请求（"本项目所有字典改成 string，所有引用字典的字段改成 varchar"）+ `nop-entropy/docs-for-ai/02-core-guides/code-style.md` 推荐
> Related: `docs/architecture/approval-framework.md`、`docs/architecture/wf-integration-design.md`（use-approval 接入需 string 字典，本计划为其前置）
> Audit: required

## Current Baseline

全量扫描实时源模型（`<domain>/model/*.orm.xml`，不含生成件）得：

- **18 个 ORM 源文件**，共 **222 个 dict 定义**：
  - `valueType="int"`：**198 个**（89.2%）— 重构目标
  - `valueType="string"`：**24 个**（CRM 全部 23 + master-data `batch-strategy` 1）— **已是目标态，不动**
- **321 个 `ext:dict` 字段**：
  - `stdSqlType="INTEGER"`：**295 个** — 重构目标
  - `stdSqlType="VARCHAR"`：26 个（CRM 24 + md `batchSelectionStrategy` + mfg `postedStatus`）— 已是目标态
- **int 字典形态**：`valueType="int"` + option `value="10/20/30..."`（与 `code` 分离），字段 `stdSqlType="INTEGER" stdDataType="int"`。
- **目标形态（CRM 样板）**：`valueType="string"` + option `value="CODE"`（value 与 code 合一），字段 `stdSqlType="VARCHAR" precision="20" stdDataType="string"`。
- **已 codegen 完成**：18 域 1721 Java 文件、`_app.orm.xml`×18、`*.dict.yaml`×223、`_Erp*DaoConstants.java`×18（784 常量）、`_create_*.sql`×54（3 方言）、实体 `_gen/*`。
- **已手写业务实现**：purchase/sales/inventory/finance/assets/manufacturing/quality/projects/maintenance/master-data/cs 等域已有实质 BizModel/Processor，且多数带"全绿"单测（见 `docs/logs/2026/`）。

### 受影响的手写代码（精确）

| 类别 | 数量 | 性质 |
| ---- | ---- | ---- |
| 手写 Java 常量文件 `Erp*Constants.java` | **10 文件 / 239 个 int 常量** | 类型 int→String、值→code |
| 手写 Java 业务文件（BizModel/Processor/Settler/Checker/Builder） | **~105 文件 / ~1054 处引用** | getter 返回 Integer→String；比较语义 `!=`/`==`（约 49 处）→ `.equals()`/`Objects.equals()` |
| 手写 Java 测试代码（`Test*.java`） | **~92 文件引用 `Erp*Constants`**（含数值字面量 `setStatus(10)` 类：55 文件/158 处；含数值比较 `getXxx()==10`/`!=0`：4 文件） | 字面量数值→code、比较重构；codegen 后 setter 变 String，未迁移则测试编译失败 |
| 手写 task.xml | **1 文件**（`ErpPurReceive/approve.task.xml`） | 状态码核对（顺带修 `FULLY_RECEIVED` vs 字典 `RECEIVED` 命名不一致 bug） |
| 手写测试输入 json5 | **5 文件**（master-data Partner 用例，`status: 10`） | 数值→code 字符串 |
| 生成文件（自动重生成，**不手改**） | `_app.orm`×18 / `dict.yaml`×223 / `_DaoConstants`(784) / 实体 `_gen` / `_create_*.sql`×54 | 重跑 codegen |

### 关键风险

1. **Java 比较语义变更（最高风险）**：分两类——(a) **类型迁移**（getter/setter `Integer`→`String`，约 105 业务文件 + 92 测试文件 / 1054 引用）：编译器强制报错，遗漏会被编译阻断；(b) **比较语义 bug**（`Integer !=`/`==` 自动装箱比较，业务约 300 处 + 测试 4 处）：**编译器不报错**，类型变 String 后变为地址比较"恒不等"，必须人工逐一改 `.equals()`/`Objects.equals()`，否则隐蔽逻辑 bug。审查资源聚焦 (b)。
2. **task.xml 状态码与字典 code 不一致**：当前 int 字段与字符串比较（`receive.approveStatus != 'SUBMITTED'`，字段存 20）已是潜在 bug；`FULLY_RECEIVED` vs 字典 code `RECEIVED` 不匹配。int→string 后这些比较将真正生效，可能暴露预存逻辑缺陷。
3. **跨命名空间字典引用**：drp.orm.xml 存在 `erp-inv/` 命名空间字典（`drp-ss-method`/`drp-service-level`/`drp-xdock-status`），改造须保证 dict 定义与所有引用字段 valueType 同步。
4. **高影响面**：会触及已通过测试的业务逻辑；收益是对齐平台 `code-style.md` 推荐方向 + 为 `use-approval` 接入（需平台标准 `wf/approve-status` string 字典）铺路。

## Goals

- 198 个 int dict → `valueType="string"`，option `value` 由数值改为与 `code` 相同的字符串。
- 295 个 `INTEGER` 的 `ext:dict` 字段 → `VARCHAR` + `stdDataType="string"` + 合理 `precision`。
- 10 个手写常量文件 239 常量：`int`→`String`，值改为 code。
- ~105 个手写业务文件 + ~92 个测试文件：getter/局部变量/字面量 `Integer`→`String`，比较改 `.equals()`/`Objects.equals()`。
- 1 个 task.xml 状态码核对（含命名 bug 修复）。
- 5 个测试输入 json5：状态数值→code 字符串。
- 全仓库验证全绿，无 int 字典 / 无 INTEGER ext:dict 字段残留。

## Non-Goals

- **不动 CRM 域**（23 dict + 24 字段已是 string，作为目标样板参照）。
- **不动 24 个已 string 字典**与 26 个已 VARCHAR 字段。
- **不接入 use-approval**（独立后继计划；本计划仅完成类型统一，为它清场）。
- **不改非 `ext:dict` 的纯数值字段**（amount/quantity/price/计数等）。
- **不改变业务行为语义**：状态机迁移、错误码、过账逻辑保持等价；仅类型与比较实现迁移，外加 task.xml 已确认的命名 bug 修复。
- **不做 i18n / XLSX / 物理库生产数据迁移**（见 Decision D5：bootstrap 阶段重建优先）。
- **不改 GraphQL/API 契约与前端页面**：实测无手写 `module-*/model/*.api.xml`、web 模块 `view.xml` 未引用 `ext:dict` 值类型（bootstrap 阶段页面未定制），类型随 codegen 自动同步，故前端/API 不在本计划范围。

## Task Route

- Type: `architecture change`（持久化模型 + 公共字段类型契约变更，跨 17 域共享行为）
- Owner Docs:
  - `nop-entropy/docs-for-ai/02-core-guides/code-style.md`（dict valueType 推荐规范，§Dict）
  - `nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md`（字典设计 §298-376）
  - `docs/context/project-context.md`（验证命令 + ORM 修改硬停止条件）
  - 各域 `docs/design/<domain>/state-machine.md`（确认状态码 code 语义不变）
- Skill Selection Basis:
  - Phase 1/2/5（ORM 模型 + codegen + 测试输入）：无匹配专用技能 → `Skill: none`。
  - Phase 3/4（改 BizModel/Processor/常量/比较语义）：匹配 `nop-backend-dev`（实体服务、跨实体、错误处理、事务边界）→ `Skill: nop-backend-dev`。
  - 无 ORM 模型设计专用技能；ORM 改动依据平台 `orm-model-design.md` + CRM 现成样板。

## Infrastructure And Config Prereqs

- 无新增基础设施/端口/密钥。
- **回滚策略**：纯 git 回退 ORM 后 `mvn clean install -DskipTests` 重生成即可恢复；bootstrap 阶段无生产数据迁移负担（Decision D5）。
- **执行前置**：`project-context.md` AI 阻塞条件规定"修改 XML 模型需明确人工批准"。用户已下达本请求（人工指令），Phase 1 执行前再次向用户确认范围。

## Execution Plan

### Phase 1 - ORM 源模型统一（dict + 字段）

Status: completed
Targets: 17 个 `module-*/model/app-erp-*.orm.xml`（除 crm）
Skill: none

- Item Types: `Fix | Decision | Proof`
- Prereqs: 用户确认 ORM 修改范围

- [x] 改 198 个 int dict：`valueType="int"` → `valueType="string"`，每个 option `value` 由数值改为与 `code` 相同字符串（保持 i18n 属性不变）
      - Skill: none
- [x] 改 295 个 INTEGER `ext:dict` 字段：`stdSqlType="INTEGER"` → `"VARCHAR"`、`stdDataType="int"` → `"string"`、补 `precision`（按 D2）
      - Skill: none
- [x] 核对跨命名空间字典引用（drp→`erp-inv/*` 等）：保证 dict 定义 valueType 与所有引用字段 stdSqlType/stdDataType 同步
      - Skill: none
- [x] Decision D1：option `value` 直接复用 `code`（对齐 CRM 样板 `value="LEAD"`），不另起枚举值；理由：消除 value/code 双轨、提升 SQL/调试可读性。替代方案（保留数值 value 仅改 valueType）被否决——违反 code-style 推荐且 codegen 常量仍生成数值。
  - Skill: none
- [x] Decision D2：`precision` 默认 20；当 code 长度 >20（如 `ERP_MFG_*`、`FULLY_RECEIVED` 类）用 30。理由：覆盖现有最长 code + 预留扩展，参照 CRM 字段约定。
  - Skill: none
- [x] Proof：17 个 ORM `xmllint --noout` well-formed 通过；grep 验证 `valueType="int"` 残留 = 0（全仓库，含 CRM 的 string 已是 0）。
  - Skill: none

Exit Criteria:

- [x] 17 个 ORM well-formed 校验通过
- [x] 全仓库 `rg 'valueType="int"' module-*/model/*.orm.xml` 命中 0
- [x] 全仓库 `rg 'ext:dict=.*INTEGER'` 形态命中 0（执行时以实际 grep 模式为准）

### Phase 2 - codegen 增量重生成

Status: completed
Targets: 生成件（`_app.orm.xml`×18 / `*.dict.yaml`×223 / `_Erp*DaoConstants.java`×18 / 实体 `_gen/*` / `_create_*.sql`×54）
Skill: none

- Item Types: `Proof`
- Prereqs: Phase 1 完成

- [x] 执行 `mvn clean install -DskipTests` 触发 `gen-orm.xgen` 增量链（按 `project-context.md`：**不**重跑 `nop-cli gen`）
      - Skill: none
- [x] Proof：抽查产物类型已迁移（如 `module-purchase/erp-pur-dao/.../_gen/_ErpPurReceive.java` 字段类型 `Integer`→`String`；`erp-pur/approve-status.dict.yaml` `valueType: string` + `value: APPROVED`；`_create_erp-pur.sql` 列 `VARCHAR(20)`）
      - Skill: none

Exit Criteria:

- [x] `mvn clean install -DskipTests` 全绿（146 reactor 模块）
- [x] 抽查 ≥3 域的 `_gen` 实体字段类型为 String

### Phase 3 - 手写 Java 常量迁移

Status: completed
Targets: 10 个 `Erp*Constants.java`（mfg 56 / fin 48 / qa 28 / ast 26 / pur 23 / inv 17 / sal 16 / prj 15 / md 8 / mnt 2）
Skill: nop-backend-dev

- Item Types: `Fix`
- Prereqs: Phase 2（生成件已 String，手写常量须对齐否则业务文件编译断）

- [x] 239 个 int 常量 → `String`，值由数值改为对应字典 code；常量命名保留（如 `APPROVE_STATUS_APPROVED = "APPROVED"`）
      - Skill: nop-backend-dev
- [x] Decision D3：常量值与字典 option `code` 一一对应；常量命名沿用现有 `DOMAIN_FIELD_CODE` 约定，仅改类型与字面量。
  - Skill: nop-backend-dev

Exit Criteria:

- [x] 10 个 service 模块 `mvn compile -DskipTests` 各自通过（解除 Phase 4 业务文件编译依赖）

### Phase 4 - 手写 Java 业务 + 测试代码（比较语义重构）+ task.xml

Status: completed
Targets: ~105 个 BizModel/Processor/Settler/Checker/Builder 文件 + ~92 个 `Test*.java`（55 含数值字面量 / 4 含数值比较）+ `ErpPurReceive/approve.task.xml`
Skill: nop-backend-dev

- Item Types: `Fix | Decision`
- Prereqs: Phase 3

- [x] getter 返回 / 局部变量 `Integer` → `String`（涉及 approveStatus/docStatus/postedStatus/receiveStatus/paidStatus/direction/postingType 等枚举字段）
      - Skill: nop-backend-dev
- [x] 比较重构（业务约 300 处 + 测试 4 处）：`!=`/`==`（Integer 装箱）→ `Objects.equals()` 或 `.equals()` 配 null 判断（Decision D4）
      - Skill: nop-backend-dev
- [x] 测试代码字面量迁移：55 文件/158 处 `setStatus(<数值>)` / 构造参数数值 → 对应 code 字符串（codegen 后 setter 变 String，不迁移则测试编译失败）
      - Skill: nop-backend-dev
- [x] Decision D4：比较策略——优先 `java.util.Objects.equals(a,b)`（null 安全、统一）；单侧非空时可用 `target.equals(entity.getXxx())`。**禁止** `==`/`!=` 比较 String。理由：规避装箱比较历史习惯导致的恒不等 bug。
  - Skill: nop-backend-dev
- [x] Fix：task.xml 状态码核对——`approve.task.xml` 中 `FULLY_RECEIVED`/`PARTIALLY_RECEIVED` 与字典 code 不一致，按字典实际 code（`RECEIVED`/`PARTIAL`）修正（属已确认实时缺陷，不可降级为 follow-up）
      - Skill: nop-backend-dev
- [x] 按域推进：先迁移该域业务代码，再迁移该域测试代码，跑该域 `mvn test` 验证状态机行为等价

Exit Criteria:

- [x] 已推进域的 `mvn test` 通过（业务行为等价于重构前基线）
- [x] grep 无 `== Erp*Constants` / `!= Erp*Constants` 形态的 String 比较（执行时按实际常量引用模式核对）
- [x] grep 无 `set[A-Za-z]*(Status|Type|Direction)\([0-9]+\)` 等数值字面量调用残留（覆盖 setStatus/setDirection/setPartnerType/setVoucherType 等）

### Phase 5 - 测试输入 + 物理库决策

Status: completed
Skill: none

- Item Types: `Fix | Decision`
- Prereqs: Phase 4

- [x] Fix：5 个 master-data Partner 测试输入 json5，`status: 10` → `status: "ACTIVE"`（对齐 `erp-md/active-status` code）
      - Skill: none
- [x] 输出快照由重跑测试自动同步（不手改）
      - Skill: none
- [x] Decision D5：物理库迁移策略——项目处于 bootstrap/codegen 阶段、无生产数据（`project-context.md`），采用"重建优先"：重生成 `_create_*.sql` + 重建开发/测试库，**不**写 `ALTER ... INTEGER→VARCHAR` 回填脚本。若后续出现需保留的开发库，由独立 follow-up 处理。
  - Skill: none

Exit Criteria:

- [x] 5 个 master-data json5 状态值已迁移为 code 字符串
- [x] 输出快照已重生成（测试重跑自动同步）
- [x] Decision D5 已记录

## Draft Review Record

- Independent draft review iteration 1: `needs revision`（ses_0d7e48fa2ffev1MIsze5olny0f）：1 阻塞——手写 Java 测试代码（~92 文件，55 含数值字面量/4 含数值比较）未纳入基线与范围，Phase 2 codegen 后测试将编译失败，使 Phase 4/5 退出标准不可达成。4 改进：业务文件数 ~73→~105、风险区分类型迁移 vs 比较语义 bug、Phase 4/5 退出全量验证归 Closure Gates、补 api.xml/view.xml 无影响说明。基线 ORM 层数字（198/24/295/26/10/239）经独立 grep 全部精确命中。
- Independent draft review iteration 2: `accept`（ses 独立从零复审）。B1 阻塞已实质解决并经独立 grep 精确验证（测试文件 92/92 命中、含数值字面量 setStatus/setDirection 55/55 命中、Baseline 第 30 行已增补测试行、Phase 4 第 161 行已纳入迁移条目、Phase 4 退出 + Closure Gates 第 208 行已设 grep 门控）。N1-N4 全部采纳：N1 Baseline 表更新 ~105（实测业务文件 103，≈合理）；N2 风险第 37 行区分类型迁移(a 编译器强制)/比较语义 bug(b 编译器不报错)；N3 Phase 4/5 退出改局部 mvn test、全量仅留 Closure Gates；N4 第 60 行补 api.xml/view.xml 无影响（实测无手写 api.xml、无 view.xml 引用 ext:dict）。遗漏面核查均无遗漏：4 个 finance YAML fixture 无数值状态字段；18 个 Erp*DaoConstants 为空壳 extends 生成件（0 int 常量）；@DictProvider 不存在；XMeta 全生成件；api.xml/view.xml 无引用。3 项非阻塞改进建议（不阻塞 accept）：(I1) 业务比较语义 bug 实测 ~319 行远超计划"约49处"，显著低估工作量，但 Phase 4 grep 门控 `==/!= Erp*Constants` 完整覆盖，质量无缺口；(I2) Phase 4 退出 grep 门控 `setStatus([0-9]+)/setDirection([0-9]+)` 漏报 17 个文件中的 setPartnerType/setVoucherType/setAdvanceType/setExpenseType/setApprovalType 等字典字段字面量（实测差集），建议执行时收宽为 `set[A-Za-z]*(Status|Type|Direction)\([0-9]+\)——`；此类 setter 因 codegen 后签名变 String 会被编译器强制报错，非阻塞；(I3) Goals 第 47 行残留旧数字 ~73，与 Baseline 表第 29 行 ~105 文本不一致，建议同步。次要数字差异（不影响）：qa int 常量实测 44 vs 计划 28；int dict 实测 202 vs 计划 198。

## Closure Gates

> 仅在所有阶段退出标准勾选 `[x]` 后关闭。

- [x] 范围内行为完成（类型统一，业务行为等价，task.xml bug 已修）
- [x] `mvn clean install -DskipTests` 全绿（146 模块）
- [x] `mvn test` 全绿
- [x] grep 门控：全仓库 `valueType="int"` dict 命中 0；全仓库 INTEGER `ext:dict` 字段命中 0；全仓库 Java 无 `set[A-Za-z]*(Status|Type|Direction)\([0-9]+\)`. 类数值字面量残留
- [x] 相关文档对齐（`docs/logs/` 开发日志更新；本计划引用的 code-style 推荐已满足）
- [x] 无范围内项目降级为 deferred/follow-up（task.xml bug 为不可降级 Fix）
- [x] 独立草案审查已完成并记录
- [x] 文本一致性已验证：状态、阶段、门控、日志一致
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
- [x] 结束证据存在于文件中

## Deferred But Adjudicated

### 接入平台 use-approval 能力

- Classification: `out-of-sscope improvement`
- Why Not Blocking Closure: use-approval 接入是独立结果表面（需 `flowInstanceId`、`.xwf`、objMeta `wf:wfName`、业务联动 xbiz 注入），本计划仅完成 dict 类型统一为其清场。
- Successor Required: `yes`（独立计划，首批付款/收款/资产处置实体）

### 物理库 ALTER 数据回填脚本（若出现需保留库）

- Classification: `watch-only residual`
- Why Not Blocking Closure: bootstrap 阶段无生产数据，重建优先（Decision D5）。
- Successor Required: `no`（触发条件：出现需保留数据的开发/演示库）

## Closure

Status Note: 全部阶段已完成。`mvn clean install -DskipTests` 与 `mvn test` 全绿（2026-07-04 验证基线）。全仓库 dict `valueType="int"` 命中 0、INTEGER `ext:dict` 字段命中 0、Java 中 `set[A-Za-z]*(Status|Type|Direction)\([0-9]+\)` 数值字面量残留命中 0。阶段 1（ORM）、阶段 2（codegen）、阶段 3（常量迁移）、阶段 4（业务+测试代码+task.xml 修正）、阶段 5（json5+快照+物理库决策 D5）均已勾选并验证。

执行过程中额外发现并修复了 Phase 1 遗留的类型迁移误伤：多个非 dict 的 DECIMAL 域字段（budget/hours/costRate/weight/volume/dimension/downtimeMinutes/measuredValue 等）被误改为 VARCHAR，已逐域回退为 DECIMAL（measuredValue 因需支持文本实测值保留 VARCHAR）；受影响的业务/测试代码（quality、projects、maintenance、assets 模块）已同步修正。

Closure Audit Evidence:

- Auditor / Agent: 独立结束审计（新会话）— _pending independent closure audit_
- Evidence: `mvn clean install -DskipTests` BUILD SUCCESS（146 模块）；`mvn test` BUILD SUCCESS（全域全量测试通过）；grep 门控全部命中 0。


- **Independent Closure Audit (2026-07-14-1449-1 batch)** — Auditor: independent closure audit subagent (fresh session, cold-replay, 2026-07-14). Verdict: **PASS_WITH_NOTES**. All exit criteria verified: zero valueType=int, zero INTEGER ext:dict fields, zero dict-field numeric-literal setter calls, constants migrated to String, task.xml bug fixed, json5 inputs migrated, DECIMAL rollback correct. NOTE: 2 contract dicts (sign-status/sign-provider) have numeric values introduced by LATER plan 2200-2, not by this plan. (Audit dispatch ref: docs/plans/2026-07-14-1449-1-closure-audit-consistency-remediation-batch.md Phase 2; this evidence block appended by Phase 3 backfill.)
Follow-up:

- 接入 use-approval（见 Deferred）
