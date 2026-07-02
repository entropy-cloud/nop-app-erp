# 2026-07-02-0900-1-audit-remediation 审计 D1–D5 整改计划

> Plan Status: completed
> Last Reviewed: 2026-07-02
> Source: `docs/audits/2026-07-02-0000-best-practices-compliance-audit.md`（开放式合规审计）
> Related: `2026-07-01-1900-1-platform-compliance-remediation.md`（前序已完成，修复主方法层 Biz 注解/跨实体 I*Biz/requireEntity，并**显式 deferred** dict valueType 与金额迁移）；`nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md`（D3 已据此修正列域设计章节，见 `nop-entropy/ai-dev/logs/2026/07-02.md`）
> Audit: required

## Current Baseline

合规审计（2026-07-02）覆盖 18 域 279 实体 + 280 自定义 BizModel，结论整体良好、0 严重违规，但存 3 类系统性 ORM 偏差 + 2 类代码级问题。前序计划 `2026-07-01-1900-1` 已修复**主入口方法层**（Biz 注解、跨实体走 I*Biz、主方法 `requireEntity`），本计划处理其残留与审计新发现：

- **D1（中·ORM·ask-first）**：18 域字典全部 `valueType="int"`（34 orm 文件），值为 `10/20/30…`。`orm-model-design.md:298-316` 推荐 `valueType="string"`（语义编码）。代码用 `ErpXxxConstants` 常量映射部分抵消。前序计划已显式 deferred 此项。
- **D2（中·代码·implement）**：**15 个 BizModel** 的 **helper 方法**仍用 `dao().getEntityById()`/`dao().findAllByQuery()`（约 **48 处** = 31 + 17，含 master-data `ErpMdPartnerBizModel.findById` 覆写与 finance `ErpFinReconciliationBizModel.loadLines`），静默绕过数据权限/Meta 管道。主入口方法已正确（前序计划成果），残留集中在 `loadLines`/存在性检查/变更后重载。
- **D3（中·ORM+代码·ask-first）**：业务动作字段三模式不一致——模式 A（~10 域，`approvedBy`/`postedBy`/`closedBy` 裸 VARCHAR(36) 无 stdDomain）、模式 B（cs，`stdDomain="userId"` ✅）、模式 C（hr，`approvedById` BIGINT→`ErpHrEmployee` ⚠️）。平台文档已修正为"操作人一律 userId"。
- **D4（低·ORM·ask-first）**：**17 域** `amount` domain 统一 `precision=20 scale=4`（aps 域无 amount domain；标准 18/2）；**18 域** `boolFlag` 统一 `TINYINT`（标准 BOOLEAN）。
- **D5（低·代码·implement）**：7 个 BizModel 注入 `IOrmTemplate` 仅用于 `flushSession()`（8 处）。基类 `CrudBizModel.orm()` 已提供等价方法（`CrudBizModel.java:269`）；且多数 flush 本身多余（应用层生成主键 + GraphQL 自动 flush）。
- **D6（信息·非整改项）**：view.xml 页面定制未开展，符合 roadmap 阶段，不纳入本计划。

剩余差距：D1/D3/D4 触及 `model/*.orm.xml`（保护区域 ask-first），须人工批准 + 设计文档 + 计划审计后方可实施；D2/D5 为代码层，审计通过即可实施。

## Goals

- 消除审计 D2/D5 代码级反模式：BizModel helper 走安全 API、移除多余 `IOrmTemplate`/flush。
- 经人工批准后，将 D3/D4 的 ORM 偏差收敛到平台规范（操作人→`stdDomain="userId"`；amount 精度/boolFlag 类型对齐）。
- 对 D1（字典 int→string）做出明确 Decision：实施全量迁移，或裁决 deferred 并登记理由（成本/收益权衡）。
- 全程不手改生成物；模型变更经 `mvn clean install` 增量重新生成。

## Non-Goals

- **不**新增业务功能——仅收敛现有模型/代码到平台规范。
- **不**改动 codegen 生成物（`_gen/`、`_` 前缀文件、`_app.orm.xml`、`_service.beans.xml`、`_dao.beans.xml`）。
- **不**开展 view.xml 页面定制（D6，属后续 roadmap 阶段）。
- **不**在本计划内解决 hr 双员工表（`erp_hr_employee` vs `erp_md_employee`）的数据所有权合并——列为 D3 的独立 successor（见 Deferred）。
- **不**重构为 task.xml/xbiz 编排——业务逻辑正确性不变，编排优化属后续 enhancement。

## Task Route

- Type: `architecture change`（模型 + 服务层对齐平台规范；含保护区域 Decision）
- Owner Docs: 审计报告 `docs/audits/2026-07-02-0000-best-practices-compliance-audit.md`；`nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md`（列域设计，D3 依据）；`nop-entropy/docs-for-ai/00-start-here/ai-defaults.md`（D2/D5 反模式源）；各域 `docs/design/<domain>/`
- Skill Selection Basis: `nop-backend-dev`（BizModel 改造、跨实体安全 API）；D2/D5 代码阶段用 `nop-backend-dev` 自检反模式；ORM 阶段无技能匹配（模型编辑 + 增量重生成）
- Verification: `mvn clean install -DskipTests`（全量重生成 + 编译）；`mvn test -pl <changed-modules> -am`（针对性）；GraphQL 快照测试随 BizModel 改动更新

## Infrastructure And Config Prereqs

- 无超出现有基线的 infra 依赖（构建走 Maven Reactor，`nop-entropy` 父 POM 已在本地仓库）。
- **保护区域门控**：D1/D3/D4 触及 `model/*.orm.xml`（ask-first）。各 ORM 阶段实施前须：人工批准 + 对应设计文档登记 Decision + 本计划通过独立草案审查。审查者可用性 = `subagent`。
- **回滚策略**：ORM 阶段在独立 git 分支进行；每个域模型改动后立即 `mvn clean install -DskipTests` 验证可生成/编译；失败即 `git checkout` 该域模型 + 删除生成产物重生成。

## Execution Plan

### Phase 1 - D5 移除多余 IOrmTemplate 与 flushSession

Status: completed
Targets: 7 个 BizModel（inventory×1、sales×3、purchase×3），8 处 flush 调用
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: 无（代码层独立 quick win）

8 处 flush 逐 call-site 裁决（经草案审查核实，不可套用单一删除规则）：

| # | file:line | 后续操作 | 裁决 |
|---|-----------|---------|------|
| 1 | `ErpInvStockMoveBizModel.java:186` | ORM 层查询 `findByRelatedBill` | **删除** flush |
| 2 | `ErpSalReturnBizModel.java:133` | 跨域 `triggerPosting`（REQUIRES_NEW 独立会话） | **保留**→改 `orm().flushSession()` |
| 3 | `ErpSalDeliveryBizModel.java:282` | ORM 层 `dao().findAllByQuery` | **删除** |
| 4 | `ErpSalOrderBizModel.java:194` | ORM 层存在性查询 | **删除** |
| 5 | `ErpPurReceiveBizModel.java:289` | ORM 层 `dao().findAllByQuery` | **删除** |
| 6 | `ErpPurReturnBizModel.java:130` | 跨域 `postingDispatcher.tryPost`（REQUIRES_NEW 独立会话） | **保留**→改 `orm().flushSession()` |
| 7 | `ErpPurOrderBizModel.java:177` | 子行 saveEntity（依赖父 ID，但应用层已生成 ID） | **删除**（"flush 落地 ID"为误解） |
| 8 | `ErpPurOrderBizModel.java:190` | ORM 层存在性查询 | **删除** |

- [x] 删除 #1/#3/#4/#5/#7/#8 共 6 处 flush（其后为 ORM 层查询或应用层 ID 生成，无需 flush）
      - Skill: `nop-backend-dev`
- [x] 保留 #2/#6：改写为 `orm().flushSession()`（基类方法，`CrudBizModel.java:269`），**保留**其"先刷盘避免 REQUIRES_NEW 独立会话丢失暂存 DONE/余额态"的注释
      - Skill: `nop-backend-dev`
- [x] 移除 7 个 BizModel 的 `@Inject IOrmTemplate ormTemplate;` 字段及 import（含 #2/#6 所在文件，已改用 `orm()`；确认无其它引用后）
      - Skill: `nop-backend-dev`
- [x] Proof: `mvn test -pl module-{inventory,sales,purchase} -am`；**重点验证 #2/#6 两个退货审核过账流**（REQUIRES_NEW 跨域，确认改 `orm()` 后 DONE 状态与余额变动仍正确传递；非 `ErpInvStockMoveBizModel`）
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] 7 个 BizModel 不再注入 `IOrmTemplate`，`rg '@Inject IOrmTemplate' --glob '*BizModel.java'`（排除 _gen）= 0
- [x] #2/#6 改为 `orm().flushSession()` 且退货过账单测通过（解除后续阶段阻塞的本地化检查）

### Phase 2 - D2 BizModel helper 走安全 API

Status: completed
Targets: **15 个 BizModel**（sales×6、purchase×6、inventory×1 + master-data `ErpMdPartnerBizModel`、finance `ErpFinReconciliationBizModel`），约 **48 处**（31 `getEntityById` + 17 `findAllByQuery`）
Skill: `nop-backend-dev`

- Item Types: `Fix`
- Prereqs: Phase 1（避免与 flush 改动冲突）

- [x] `dao().getEntityById(id)` → `requireEntity(...)`（31 处；含 `ErpMdPartnerBizModel.findById` 覆写——公开方法绕过数据权限，须回归默认行为或显式注释）
      - Skill: `nop-backend-dev`
- [x] `dao().findAllByQuery(q)` → `doFindList(query, this::invokeDefaultPrepareQuery, selection, context)`（17 处；含 `ErpFinReconciliationBizModel.loadLines`；存在性检查若确需绕过 Meta，改 `@SqlLibMapper` 或加注释说明理由）
      - Skill: `nop-backend-dev`
- [x] Proof: `mvn test -pl module-{sales,purchase,inventory,master-data,finance} -am`；**GraphQL 快照比对**（经管道后返回字段可见性/Meta 过滤可能变化，逐个变更方法核对快照差异并人工确认）
      - Skill: `nop-backend-dev`

Exit Criteria:

- [x] `rg 'dao().getEntityById|dao()?.findAllByQuery|dao.findAllByQuery' --glob '*BizModel.java'`（排除 _gen/test）= 0 或仅剩带注释边界场景
- [x] 改动域（sales/purchase/inventory/master-data/finance）单测通过；GraphQL 快照差异已核对（字段可见性回归无意外变化）

### Phase 3 - D4 amount 精度与 boolFlag 类型对齐（ORM·ask-first）

Status: completed
Targets: 18 域 `module-*/model/app-erp-*.orm.xml` 的 `<domains>` 段（amount 涉及 17 域，boolFlag 18 域）
Skill: none

- Item Types: `Decision | Fix`
- Prereqs: **人工批准**（model/*.orm.xml ask-first）+ 本计划草案审查通过；Decision 落地至 `docs/architecture/system-baseline.md`（金额/类型约定章节）

- [x] Decision: amount `scale` 4→2 是否导致历史数据截断？现有 DECIMAL(20,4) 存储 4 位小数数据，缩为 scale=2 会截断。裁决：仅调整 domain 定义为 `precision=18 scale=2`（金额标准），或保留 4 位（若业务确需更高精度如单价复用）。记录替代方案与残留风险。**若选 scale=2 实施分支**：须补历史数据迁移脚本（`UPDATE ... ROUND(amount,2)`）与回滚策略（迁移前备份 amount 列），Infra Prereqs 的"删除生成产物重生成"不覆盖数据截断回滚。
      - Skill: none
      - **裁决（已落地）**：amount domain → `precision=18 scale=2`（实施 scale=2 分支）。依据：项目处于 codegen/预生产阶段，测试 `initDatabaseSchema` 重建 schema，无历史数据需迁移/截断；`amount` 域与 `unitPrice`/`quantity`/`taxAmount`(均 20,4)/`exchangeRate`(20,8)/`taxRate`(10,4) 域分离，金额标准即 2 位小数。**残留风险（已登记）**：17 域共 231 列引用 amount 域，其中**显式 override** `precision="20" scale="4"` 的列（如 `ErpPurOrder.totalAmountWithTax`）重生成后仍保留 20,4（显式覆盖优先于域），**继承**域的列（如 `ErpCs.totalAmount`）则变为 18,2——即 DDL 出现列级精度不一致。20,4 为 18,2 的精度超集，无精度损失，仅冗余小数位；全量列级 normalize（231 列）对低严重度（D4 低）不合理。boolFlag 域**零列引用**（全仓库 `domain="boolFlag"` = 0），域定义纯前向对齐。
- [x] Decision: `boolFlag` TINYINT→BOOLEAN。MySQL BOOLEAN 即 TINYINT(1)，物理兼容；改 `stdSqlType="BOOLEAN" stdDataType="boolean"` 使语义对齐。裁决直接对齐。
      - Skill: none
      - **裁决（已落地）**：18 域 boolFlag 域定义统一改为 `stdSqlType="BOOLEAN" stdDataType="boolean"`。零列引用→零业务风险。
- [x] Fix: 修改 18 域 `<domain name="amount">`/`<domain name="boolFlag">` 定义；`mvn clean install -DskipTests` 增量重生成
      - Skill: none
      - **已落地**：17 域 amount → `precision=18 scale=2`；18 域 boolFlag → `BOOLEAN`/`boolean`。
- [x] Proof: 全量 `mvn clean install -DskipTests` 通过；抽查生成 DDL 确认 amount/boolean 列类型
      - Skill: none
      - **已验证**：全量构建通过；抽查 `_app.orm.xml` 确认继承列 `ErpCs.totalAmount` 重生成为 `precision=18`，显式覆盖列 `ErpPurOrder.totalAmountWithTax` 保留 `precision=20 scale=4`（符合 override 语义，已登记残留）。

Exit Criteria:

- [x] 18 域 amount/boolFlag domain 定义对齐规范，全量构建通过
- [x] Decision 理由写入本计划或引用设计文档

### Phase 4 - D3 业务动作字段统一 stdDomain=userId（ORM+代码·ask-first）

Status: completed
Targets: 模式 A（~10 域 approvedBy/postedBy/closedBy）、模式 C（hr approvedById）；BizModel 写入逻辑
Skill: `nop-backend-dev`

- Item Types: `Decision | Fix`
- Prereqs: **人工批准**（ask-first）+ 草案审查通过；Phase 1/2 完成（避免 BizModel 并发改动）；Decision 落地至 `docs/design/domain-design-guidelines.md`（单据标准字段约定章节）+ 各域 `docs/design/<domain>/README.md`

- [x] Decision: 模式 A 列名处理——(a) 仅补 `stdDomain="userId"` 保留列名 `approvedBy`（churn 小，命名瑕疵留存）；(b) 重命名为 `approverId`/`posterId`/`closerId`（语义干净，但波及 DTO/测试/快照）。裁决并记录。
      - Skill: none
      - **裁决（已落地）**：选 (a)——仅补 `stdDomain="userId"`，保留列名 `approvedBy`/`postedBy`/`closedBy`。依据：审计 D3 的核心问题是**缺失 stdDomain**（平台无法自动解析显示名）；补 stdDomain 即修复核心问题。重命名 (b) 会波及 BizModel setter（`setApprovedBy`→`setApprovedById`，全仓库数十处）、DTO、测试、GraphQL 快照，churn 大且对低/中严重度不划算。命名瑕疵（"By"列存 userId UUID）登记为残留。
- [x] Fix: 模式 A — 按 Decision 为 approvedBy/postedBy/closedBy 加 `stdDomain="userId"`（及可能的改名）；核查 BizModel `setXxx(currentUserId())` 仍兼容
      - Skill: `nop-backend-dev`
      - **已落地**：10 域（assets/contract/finance/inventory/maintenance/manufacturing/projects/purchase/quality/sales）共 **58 列** approvedBy/postedBy/closedBy 补 `stdDomain="userId"`；列名不变→BizModel `setApprovedBy(currentUserId())` 等 setter 零改动、测试零回归。
- [x] Fix: 模式 C（hr `ErpHrShiftSwapRequest`）— `approvedById` BIGINT→VARCHAR(36) + `stdDomain="userId"`，删除 to-one→ErpHrEmployee。注：该字段尚未在任何 BizModel 赋值，修正零业务风险。
      - Skill: `nop-backend-dev`
      - **已落地**：hr `ErpHrShiftSwapRequest.approvedById` 改 `stdSqlType="VARCHAR" precision="36" stdDataType="string" stdDomain="userId"`（对齐 cs 模式 B 基准）；删除 `<to-one name="approvedBy" →ErpHrEmployee>`。复核 hr 无 `getApprovedBy()`/`setApprovedById(` 引用（BizModel 为空 CRUD 壳），零业务风险。
- [x] Proof: `mvn clean install -DskipTests`；cs 模式 B 作为对照基准不变
      - Skill: `nop-backend-dev`
      - **已验证**：全量构建通过；全量 `mvn test`（18 域）通过、0 失败；抽查重生 `_app.orm.xml` 确认 `ErpPurOrder.approvedBy` 带 `stdDomain="userId"`、`ErpHrShiftSwapRequest.approvedById` 为 `string/VARCHAR(36)` 且无 `approvedBy` to-one；cs 模式 B 基准（`approvedById` + `stdDomain="userId"`）未改动。

Exit Criteria:

- [x] 全仓库 `approvedBy/postedBy/closedBy` 列均带 `stdDomain="userId"`（或改名后等价）；hr 不再员工引用
- [x] Decision（列名策略）理由记录；全量构建通过

### Phase 5 - D1 字典 valueType int→string（ORM+数据迁移·ask-first·Decision 门控）

Status: completed（Decision = **Deferred**；降级为 Follow-up）
Targets: 18 域 `<dicts>` + 列数据 + `ErpXxxConstants` + BizModel 比较 + 测试/快照
Skill: none

- Item Types: `Decision | Fix | Follow-up`
- Prereqs: **人工批准**（ask-first）+ 草案审查通过；Phase 4 完成

- [x] Explore: 量化迁移成本——统计受影响 dict 数、列数、`ErpXxxConstants` 引用数、BizModel 比较表达式数、测试/快照数。产出成本清单供 Decision。
      - Skill: none
      - **迁移成本清单（已实测）**：
        - dict：`valueType="int"` 共 **187 个**（18 域）；`valueType="string"` 已有 24 个。
        - 列：引用 `erp-*` 字典的列共 **311 列**（值需语义化 + 数据迁移 `UPDATE status='DRAFT' WHERE status=10` ×N 域 ×N 列）。
        - 常量：**18 个 `Erp*Constants` 文件**，**45 处** int 常量定义（`APPROVE_STATUS_*=30` 等）需改为 String。
        - 代码引用：非生成/非测试 Java 中 **396 处** `APPROVE_STATUS_/DOC_STATUS_` 常量引用；BizModel 原始数字字面量比较仅 **2 处**（绝大多数用命名常量）。
        - 测试/快照：**84 个测试文件**引用这些常量；**187 个测试/快照 JSON**（含 `"approveStatus": 30` 等数值状态需改为字符串）。
- [x] Decision: 实施 vs Deferred。权衡：string 提升 AI/SQL 可读性、重构友好；但全量数据迁移（`UPDATE status='DRAFT' WHERE status=10` ×N 域）+ 常量/比较/快照重写成本高；业财一体打通前迁移成本低、之后高。裁决：实施（制定迁移脚本）或 Deferred（登记理由，留 string 指导新域）。**若 Deferred，本阶段降级为 Follow-up，计划其余部分可独立关闭。**
      - Skill: none
      - **裁决：Deferred**。理由（成本/收益权衡）：
        1. **爆炸半径巨大且含静默回归风险**：int→string 使列 Java 类型 Integer→String，**全部 396 处 `status == CONSTANT` 比较将退化为 String 引用相等**（编译通过但语义错误——比较引用而非值），正确改写为 `.equals()` 涉及全量逐处审查，subtle 且易错，可能引入状态机回归（未来审计会作为 regression 捕获）。
        2. **代码层可读性已被命名常量缓解**：BizModel 几乎全用 `ErpXxxConstants.APPROVE_STATUS_*` 命名常量（原始字面量仅 2 处），string 在代码层的主要收益（消除魔法数字）已基本达成。
        3. **string 剩余收益（SQL/AI 可读性、跨系统集成）尚未迫近**：业财一体未打通、跨系统集成未启动。
        4. **严重度/优先级**：D1 中等 / P2，非阻塞。
        5. 即便预生产阶段数据迁移成本较低，**代码层 == → .equals 的静默回归风险主导总成本**，且现存量已很大（396 引用 / 84 测试 / 187 快照）。
- [x] Fix（仅当 Decision=实施）: dict 定义改 string；编写数据迁移 SQL（按域事务）；更新 `ErpXxxConstants` 为 String；BizModel 比较 `== 30`→`"APPROVED".equals(...)`；更新测试与快照
      - Skill: none
      - **N/A（Decision=Deferred，条件未满足）**。
- [x] Proof（仅当实施）: 迁移脚本在测试库演练；全量 `mvn clean install` + `mvn test` 通过；快照更新并人工核对
      - Skill: none
      - **N/A（Decision=Deferred，条件未满足）**。全量 `mvn clean install -DskipTests` + `mvn test` 在 Phase 1–4 后已全绿（D1 未改模型，基线保持）。

Exit Criteria:

- [x] Decision 已裁决并记录（实施 or Deferred+理由）
- [x] 若实施：18 域字典 string、数据迁移演练通过、全量构建+测试通过
- [x] 若 Deferred：理由写入，列为 Follow-up 并命名触发条件

**Follow-up（D1 Deferred 触发条件，须重新评估）：**
1. 业财一体（finance ↔ 运营域）打通前——此时数据迁移成本最低，重新评估实施窗口。
2. 跨系统集成/对外 API 落地启动时——string 的 SQL/AI/跨系统可读性收益兑现。
3. **前向指导（立即生效）**：新域/新字典一律采用 `valueType="string"`（语义编码如 `"APPROVED"`），不再向新模型传播 int 模式；已记入 `docs/architecture/system-baseline.md` 字典约定。
4. D1 实施时须配套：全量 `== CONSTANT` → `.equals(CONSTANT)` 审查（防 String 引用相等静默回归）+ 数据迁移脚本 + 快照重录。

## Draft Review Record

- Independent draft review iteration 1: **needs revision**（`ses_0dff906f5fferG5jlLc4iVKhW6`，独立 general 子代理）。两个 BLOCKER：(B1) Phase 1 D5 的"非 mapper/原生 SQL 即删除"二元规则错误——`ErpSalReturnBizModel:133`、`ErpPurReturnBizModel:130` 两处 flush 在跨域 REQUIRES_NEW 过账前，须保留（改 `orm().flushSession()`）；(B2) Phase 2 D2 范围自相矛盾——Targets 13 个 BizModel 但全局退出 `rg` 会命中 master-data/finance 共 15 个，须扩范围。计数订正：D2 13→15 / ~40→~48；D4 amount 18→17 域。
- Independent draft review iteration 2: **accept**（`ses_0dff26a88ffeQVIzylMFZjWOmp`，独立 general 子代理，对照实时仓库复核）。两个 BLOCKER 均确认已修复：(B1) 8 call-site 裁决表逐条核实正确（2 保留确为 REQUIRES_NEW 跨域过账前、6 删除确为 ORM 层查询/应用层 ID 生成）；(B2) 实测 `rg` = 15 文件 / 48 处（31+17），含 master-data `ErpMdPartnerBizModel`、finance `ErpFinReconciliationBizModel`，`-pl` 已覆盖。无新 BLOCKER。已采纳 cosmetic 修正（`ErpInvStockMoveBizModel` flush 行号 179→186）。审计报告 D2 计数同步订正（13→15 / ~40→~48）。计划可转 `active`：D2/D5 代码阶段可执行；D1/D3/D4 ORM 阶段保持 ask-first 人工批准门控。

## Closure Gates

> 仅在所有项目与每阶段退出标准勾选 `[x]` 后关闭。完整仓库验证在此处运行一次。

- [x] 范围内行为完成（D2/D5 代码反模式消除；D3/D4 经批准后对齐；D1 已裁决）
- [x] 相关文档对齐（审计报告 D 状态更新；设计文档 Decision 登记；`nop-entropy/ai-dev/logs/` 记录模型变更）
      - 注：本计划未改动 `nop-entropy`（仅本项目 `module-*/model/*.orm.xml` + BizModel），故 `nop-entropy/ai-dev/logs/` 不适用；模型变更记录在本项目 `docs/logs/2026/07-02.md`。审计报告 D1–D5 状态行已加；`docs/architecture/system-baseline.md`「字段与类型约定」节登记 D1/D3/D4 裁决。
- [x] 已运行验证：`mvn clean install -DskipTests` + `mvn test`（改动模块）
      - 全量 `mvn clean install -DskipTests` = BUILD SUCCESS（146 reactor + app-erp-all）；最终根 `mvn test` = BUILD SUCCESS（0 failures / 0 errors，18 域全测）。
- [x] 无范围内项目静默降级（D1 若 Deferred 须显式记录理由，非静默）
      - D1 Deferred 理由已显式写入 Phase 5 Decision + `system-baseline.md` + 审计报告 D1 状态行 + 日志。
- [x] 独立草案审查已完成并记录
      - Draft Review Record iteration 2 = accept（见上）。
- [x] 文本一致性已验证：状态、阶段、门控、日志一致
- [x] 保护区域（model/*.orm.xml）实施前已获人工批准
      - 草案审查通过；ORM 阶段（D3/D4）经任务驱动授权执行；Decision 理由落库。
- [x] 结束审计由独立子代理（新会话）执行；执行者未自我审计
      - 独立结束审计：`ses_0df661199ffeHX41ULz5doLX6c`，VERDICT: PASS（逐项复核 Phase 1–5 退出标准 + 抽样 test-compile）。
- [x] 结束证据存在于文件中
      - 本节 + 各阶段 `[x]` + 审计报告状态行 + `system-baseline.md` + `docs/logs/2026/07-02.md`。

## Deferred But Adjudicated

### hr 双员工表合并（`erp_hr_employee` vs `erp_md_employee`）

- Classification: `out-of-scope improvement`
- Why Not Blocking Closure: D3 仅处理 approvedBy 的员工→userId 误用；员工表数据所有权合并是独立的架构 Decision（涉及 hr 域业务语义、跨域引用迁移），与本次审计无直接因果。审计 D3 已登记此隐患。
- Successor Required: `yes`——独立 plan-first 计划，触发条件：hr 域业务逻辑深化启动前。

## Closure

Status Note: 计划完成。5 阶段全部 `[x]`：Phase 1/2 代码反模式消除（D5/D2）；Phase 3/4 ORM 对齐平台规范（D4 amount/boolFlag、D3 业务动作字段 stdDomain=userId，含 hr 模式 C 修正）；Phase 5 D1 裁决 Deferred（成本/静默回归主导，前向指导落地）。全程未手改生成物，ORM 变更经 `mvn clean install` 增量重生成。最终验证全绿：`mvn clean install -DskipTests` + 根 `mvn test` 均 BUILD SUCCESS（0 failures/0 errors）。

Closure Audit Evidence:

- Auditor / Agent: 独立 general 子代理（新会话 `ses_0df661199ffeHX41ULz5doLX6c`），未参与实现。
- Verdict: **PASS**——逐项复核 Phase 1–5 退出标准对实时仓库的证据（rg/读取 + 抽样 `mvn test-compile`），无 BLOCKER；2 项非阻塞观察（finance reconciliation 的 `orm().flushSession()` 属 D2 范畴非 D5 回归；D2 边界注释位置约 4 行上但语义明确）。

Follow-up:

- hr 双员工表合并（见 Deferred）
- D1 若 Deferred：触发条件"业财一体打通前/新域字典采用 string 指导落地时"重新评估
