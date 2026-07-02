# 最佳实践合规审计：当前代码实现 vs `docs/design` 与 `nop-entropy/docs-for-ai`

| 项 | 值 |
|----|----|
| 审计类型 | 开放式合规审计（代码实现 vs 平台最佳实践） |
| 审计范围 | 18 域 ORM 模型（279 实体）、1721 个 Java 生成文件 + 280 个自定义 BizModel、ErrorCode、跨模块引用、view.xml |
| 审计日期 | 2026-07-02 |
| 审计者 | AI 代理（基于实时仓库证据） |
| 证据方法 | 全仓库 `rg` 扫描 + 抽样精读 + `git diff` 生成物完整性检查 |
| 参照标准 | `nop-entropy/docs-for-ai/00-start-here/ai-defaults.md`、`02-core-guides/orm-model-design.md`、`02-core-guides/service-layer.md`、`docs/design/` 各域 owner doc |

## 1. 总体裁决

**整体良好。** 核心架构原则（Model→Delta→Java、CrudBizModel、I*Biz 跨实体、NopException/ErrorCode、CoreMetrics 时间线）贯彻彻底，无生成物手工篡改等严重违规。存在 **3 类系统性 ORM 偏差**（D1 字典 int / D3 业务动作字段建模不一致 / D4 amount·boolFlag）与 **2 类代码级问题**（D2 数据权限旁路【中等】、D5 IOrmTemplate【低】）。view.xml 页面定制尚未开展（与当前 roadmap 阶段一致，非违规）。

| 维度 | 严重违规 | 系统性偏差 | 代码级问题 | 待补 |
|------|---------|-----------|-----------|------|
| 数量 | 0 | 3 | 2 | 1（页面定制） |

## 2. 合规项（已验证）

### 2.1 平台核心原则

| # | 检查项 | 结论 | 证据 |
|---|--------|------|------|
| 1 | Model→Delta→Java 决策顺序 | ✅ 通过 | ORM 模型为唯一真相源；`git diff` 显示 **零生成物被手改**（`_gen/`、`_*.java`、`_app.orm.xml`、`_service.beans.xml`、`_dao.beans.xml` 均无未提交改动） |
| 2 | BizModel 基类 | ✅ 通过 | 自定义 BizModel 均为 `extends CrudBizModel<T> implements I*Biz`（例 `ErpPurOrderBizModel.java:47`） |
| 3 | 跨实体访问经 I*Biz 接口 | ✅ 通过 | 供应商启用校验经 `IErpMdPartnerBiz.findById(...)`（`ErpPurOrderBizModel.java:237`），未用 `daoProvider()`/`IDaoProvider`。接口继承 `ICrudBiz` |
| 4 | NopException + ErrorCode | ✅ 通过 | 10 个域有 `*Errors.java`，使用 `ErrorCode.define("erp.err.<domain>.<name>", 中文描述, ARG_*)`，按实体分作用域（order/receive/invoice/payment/return 各自独立，避免文案误导）。示例 `ErpPurErrors.java` |
| 5 | 时间统一 CoreMetrics | ✅ 通过 | 全仓库 **零** `System.currentTimeMillis()` / `System.nanoTime()` / `LocalDateTime.now()` / `new Date()` / `new Timestamp()`（非生成代码） |
| 6 | @BizMutation 不叠加 @Transactional | ✅ 通过 | 183 处 `@BizMutation`，仅 1 处 `@Transactional(REQUIRES_NEW)`（`ErpFinVoucherBizModel.java:42`，文档允许的跨域失败隔离例外） |
| 7 | @Inject 非 private | ✅ 通过 | 全仓库 **零** `@Inject private` |
| 8 | 不用 Spring @Value | ✅ 通过 | 全仓库 **零** `org.springframework...@Value`；配置走 `@InjectValue` |
| 9 | 返回 Entity 而非 DTO | ✅ 通过 | 状态机方法均返回实体（`ErpPurOrder` 等），字段可见性交 xmeta |
| 10 | 设计文档可追溯 | ✅ 通过 | BizModel Javadoc 引用 `docs/design/<domain>/state-machine.md` 章节，并区分"订单审核=纯状态推进"与"入库单审核=触发 generateMove" |

### 2.2 ORM 模型设计

| # | 检查项 | 结论 | 证据 |
|---|--------|------|------|
| 11 | `<to-many>` 显式 estRows | ✅ 通过 | 源模型 90 个 `<to-many>`，**全部 90** 带有 `ext:estRows`（0 遗漏）。`orm-model-design.md` 要求显式声明预估行数 |
| 12 | `<to-many>` 展开式 join | ✅ 通过 | **全部** 使用展开式 `<join><on leftProp="" rightProp=""/></join>`，**0 处** 简写 `<join on=>`（消除隐式字段名假定） |
| 13 | `<to-many>` 禁止跨模块 | ✅ 通过 | 90 个 `<to-many>` 的 `refEntityName` 与父实体 **同域包**（inv→inv、drp→drp、hr→hr），无跨模块 to-many 违规 |
| 14 | `<to-many>` tagSet 标准 | ✅ 通过 | 统一 `tagSet="pub,cascade-delete,insertable,updatable"` |
| 15 | 跨模块实体引用（机制 B） | ✅ 通过 | **135 处** `notGenCode="true"` 外部实体引用，均正确声明 `biz:moduleId="erp/md"`（inventory/maintenance/制造等引用主数据实体）。符合 `cross-module-entity-reference.md` 机制 B |
| 16 | 跨模块 `<to-one>` | ✅ 通过 | 使用全限定 `refEntityName="app.erp.md.dao.entity.ErpMdOrganization"`（`app-erp-purchase.orm.xml:126`），展开式 join |
| 17 | 逻辑删除 | ✅ 通过 | **279/279 实体** 用 `delVersion`（BIGINT，推荐方案），**0 实体** 用 `deleted` BOOLEAN；全部 `useLogicalDelete="true"` |
| 18 | 主键策略一致 | ✅ 通过 | 一致采用 BIGINT + `tagSet="seq-default"`（单机方案）；`ext:allowIdAsColName="true"` 正确设置 |
| 19 | userId 引用列 | ✅ 通过 | 15 处用户 FK 列正确用 `stdDomain="userId"` + `VARCHAR(36)`（如 `assignedToId`、`teamLeaderId`），区分于审计字段与员工引用 |

### 2.3 其他

| # | 检查项 | 结论 | 证据 |
|---|--------|------|------|
| 20 | 测试存在 | ✅ 部分通过 | 97 个测试文件已建（含 `TestErpInvStockMoveBizModel.java` 等）；随 BizModel 深化逐步补充中 |

## 3. 偏差与问题（按严重度）

> **说明**：D1–D4 为跨 **18 域完全一致** 的系统性偏差，表明是设计初期统一决策而非随机失误。一致性本身是优点，但仍偏离 `orm-model-design.md` 的推荐标准，应记录并由人工裁决是否回正。

### D1【中等】字典 `valueType="int"`（系统性）

> **整改状态（2026-07-02）**：**Deferred**（计划 `2026-07-02-0900-1` Phase 5）。裁决理由：int→string 使列 Java 类型 Integer→String，全量 `== CONSTANT` 比较退化为引用相等（静默回归风险），加 187 dict + 311 列 + 18 常量文件 + 396 引用 + 84 测试 + 187 快照的爆炸半径，对中等/P2 不划算。前向指导：新域字典一律 string。触发重评估：业财一体打通前 / 跨系统集成启动时。详见 `docs/architecture/system-baseline.md §字段与类型约定`。

- **现状**：34 个 orm 源文件（18 域）的字典**全部**用 `valueType="int"`，值为 `10/20/30/40`。
- **规范**：`orm-model-design.md:298-316` **强烈推荐** `valueType="string"`（语义编码如 `"APPROVED"`）。文档逐项论证 string 优于 int：AI 可读性、SQL 自解释、重构友好（插入新值无需重排序）、跨系统集成、多语言。
- **证据**：`app-erp-purchase.orm.xml:32` `<dict name="erp-pur/doc-status" valueType="int">`；`valueType="int"` 命中 34 文件，`valueType="string"` 仅 4 文件。
- **现状缓解**：代码用 `ErpPurConstants.APPROVE_STATUS_APPROVED`（=30）常量映射，部分抵消可读性损失；但 SQL/数据层面 `WHERE status = 30` 仍需查字典。
- **影响**：业财一体打通前的数据迁移成本较低；越往后修正成本越高。

### D2【中等】BizModel 中 `dao().getEntityById()` / `dao().findAllByQuery()`（代码级，约 48 处 / 15 个 BizModel）

> **整改状态（2026-07-02）**：**已整改**（计划 `2026-07-02-0900-1` Phase 2）。15 个 BizModel 的主实体 getEntityById 重载 → `requireEntity`/`get`、主实体 findAllByQuery → `findList`/`findCount`（走数据权限+Meta 管道）；同聚合子表 `loadLines`（15 处）按审计边界场景裁决保留并加注释（父实体已授权，子行无独立权限规则）。`rg 'dao().getEntityById|dao().findAllByQuery'`（排除 _gen/test）= 0；改动 5 域单测全绿。

- **现状**：sales/purchase/inventory 的 BizModel helper 方法广泛使用，例如：
  - `ErpPurOrderBizModel.java:205` `ErpPurOrder order = dao().getEntityById(orderId);`
  - `ErpSalReturnBizModel.java:359` `return new ArrayList<>(dao.findAllByQuery(q));`
- **规范**：`ai-defaults.md:71-73` 明确列为反模式 → 应使用 `requireEntity(id, action, context)` / `doFindList(query, this::invokeDefaultPrepareQuery, selection, context)`。
- **根因**：`requireEntity`/`doFindList` 走**数据权限 + Meta 过滤 + 逻辑删除**完整管道；`dao().getEntityById()` 与 `dao().findAllByQuery()` **静默绕过数据权限**。
- **现状缓解**：主入口方法（`submit`/`approve`/`reject`）**已正确**经 `requireOrder()`→`requireEntity()` 加载；问题集中在 helper（`loadLines` 子表加载、`existsActiveByRequisition` 存在性检查、变更后重载）。同实体/子表场景的实际数据权限风险较低，但仍是文档明示反模式。
- **建议**：存在性检查与重载改用 `requireEntity()`/`doFindList()`，或在 helper 内显式注释说明为何可绕过管道。

### D3【中等】业务动作字段 `approvedBy`/`postedBy`/`closedBy` 建模不一致（系统性，三模式）

> **整改状态（2026-07-02）**：**已整改**（计划 `2026-07-02-0900-1` Phase 4）。模式 A（10 域 58 列 approvedBy/postedBy/closedBy）补 `stdDomain="userId"`（保留列名，命名瑕疵登记残留）；模式 C（hr `ErpHrShiftSwapRequest.approvedById`）BIGINT→VARCHAR(36) + `stdDomain="userId"` + 删除到 `ErpHrEmployee` 的 to-one；cs 模式 B 基准不变。BizModel `setApprovedBy(currentUserId())` 零回归。详见 `docs/architecture/system-baseline.md §字段与类型约定`。

> **本条经复审修正**。初版曾建议统一到 `domain="createdBy"`（被动审计惯例），经讨论确认该方向错误：`approvedBy`/`postedBy`/`closedBy` 是**业务动作责任字段**，与 `createdBy`/`updatedBy`（被动审计）语义不同，不应套用同一约定。

**语义区分（建模前提）：**

| 字段类别 | 语义 | 主体 | 正确建模 |
|---------|------|------|---------|
| 被动审计（createdBy/updatedBy） | 框架自动填充，纯合规轨迹 | 登录用户 | `domain="createdBy"` VARCHAR(50) 登录名 |
| **业务动作责任**（approvedBy/postedBy/closedBy） | "谁执行了审核/过账/结账动作" | 登录**系统用户**（点按钮的人） | `stdDomain="userId"` VARCHAR(36) |
| 任务分派（pickerId/handlerId/salespersonId/managerId） | "哪个员工/岗位被指派负责此项工作" | 业务员工 | to-one → ErpMdEmployee（BIGINT） |

审核/过账/结账是**治理动作**，执行者是认证主体（用户），不是"员工记录"。`orm-model-design.md:588-599` 将 postedBy/approvedBy/closedBy 与 createdBy 一并归入 `domain="createdBy"` 惯例，该归类对**纯被动审计**成立，但对需查询/统计的业务动作字段过粗——本审计据此细化平台指引。

**现状：代码库存在 3 种不一致模式：**

- **模式 A（主流，~10 域）**：`approvedBy`/`postedBy`/`closedBy` 为裸 `VARCHAR(36)`，存用户 UUID，**无** `stdDomain`、**无** `-Id` 后缀、**无** to-one。
  - 命中域：inventory / sales / assets / projects / manufacturing / maintenance / quality / purchase / finance(postedBy,closedBy)
  - 证据：`app-erp-inventory.orm.xml:392` approvedBy；代码 `order.setApprovedBy(currentUserId())`（`ErpPurOrderBizModel.java:112` 等）
  - 问题：无 `stdDomain="userId"` → 平台**不**自动解析显示名；命名 `approvedBy` 存 UUID 是"名称列存 ID"的语义错配

- **模式 B（cs 域）**：`approvedById` VARCHAR(36) + `stdDomain="userId"` ✅
  - 证据：`app-erp-cs.orm.xml:628`。平台自动解析显示 + 可过滤，符合业务动作字段语义

- **模式 C（hr 域）**：`approvedById` BIGINT + `<to-one>` → `ErpHrEmployee` ⚠️
  - 证据：`app-erp-hr.orm.xml:685`（ErpHrShiftSwapRequest）+ `:698` to-one
  - 问题：审核动作的执行者是**用户**，非员工记录。引用员工需写入时做 user→employee 解析（多余且对系统账号/外部审计员/无档案管理员会失败）；耦合员工实体生命周期，污染历史审批事实；`stdDomain="userId"` 已免费提供展示解析
  - 缓解：该字段**尚未在任何 BizModel 赋值**（`setApprovedById(` 零命中，hr 无 approve 动作），现为纯模型声明，实现前修正成本为零
  - 关联隐患：项目存在**两个员工表**——`erp_hr_employee`（ErpHrEmployee）与 `erp_md_employee`（ErpMdEmployee，主数据规范，被 inventory/projects/quality/manufacturing/purchase 跨域引用）。hr 自建员工表是数据所有权问题，与 approvedBy 引用叠加更说明此处该用 `userId`

**收敛建议（三模式统一为 `stdDomain="userId"`）：**
- 模式 A → 补 `stdDomain="userId"` + 列名改 `approvedById`（去掉裸 `approvedBy`）
- 模式 C → 改 `stdDomain="userId"`；员工引用只留给真正的分派字段（如 requesterId/assignedEmployeeId）
- 被动审计字段（createdBy/updatedBy）保持 `domain="createdBy"` 不变

### D4【低】`amount` domain 精度与 `boolFlag` 类型（系统性）

> **整改状态（2026-07-02）**：**已整改**（计划 `2026-07-02-0900-1` Phase 3）。17 域 `amount` domain → `precision=18 scale=2`（继承列重生成为 18,2；显式 override 列保留 20,4 为精度超集，残留登记）；18 域 `boolFlag` domain → `BOOLEAN`/`boolean`（零列引用，前向对齐）。详见 `docs/architecture/system-baseline.md §字段与类型约定`。

- **现状**：
  - **18 域全部** `<domain name="amount" stdSqlType="DECIMAL" precision="20" scale="4"/>`
  - **18 域全部** `<domain name="boolFlag" stdSqlType="TINYINT"/>`
- **规范**：
  - `orm-model-design.md:409` 标准 `amount` = `precision=18 scale=2`（金额 2 位小数；数量 `quantity` 才是 4 位）。
  - `orm-model-design.md:486` 布尔标记应 `stdSqlType="BOOLEAN"`（`stdDataType="boolean"`），避免 TINYINT。
- **影响**：非错误（更宽精度仍可用），但 `scale=4` 用于金额会存冗余小数位；TINYINT 非 BOOLEAN 在跨库语义上有细微差异。

### D5【低】BizModel 注入 `IOrmTemplate` + 多余 flushSession（7 个 BizModel，8 处调用）

> **整改状态（2026-07-02）**：**已整改**（计划 `2026-07-02-0900-1` Phase 1）。7 个 BizModel 移除 `@Inject IOrmTemplate`（`rg '@Inject IOrmTemplate' --glob '*BizModel.java'` = 0）；8 处 flush 逐 call-site 裁决——6 处删除（ORM 层查询/应用层 ID 生成前置），2 处保留（跨域 REQUIRES_NEW 过账前刷盘，改 `orm().flushSession()`）。退货过账单测（#2/#6）全绿。

> **本条经复审修正**。初版定性为"边界场景、可探索基类替代"，经核实两处均为明确反模式：基类已提供等价方法，且多数 flush 调用本身不必要。

- **现状**：inventory/sales×3/purchase×3 共 7 个 BizModel 注入 `IOrmTemplate`，**全部仅用于 `ormTemplate.flushSession()`**（8 处调用）。例：`ErpPurOrderBizModel.java:50` `@Inject IOrmTemplate ormTemplate;`。

- **反模式一（注入多余）**：`CrudBizModel` 基类已提供 `public IOrmTemplate orm()`（`CrudBizModel.java:269`），BizModel 内直接 `orm().flushSession()` 即可，无需 `@Inject IOrmTemplate`。`ai-defaults.md:74` 将 BizModel 中 `IOrmTemplate` 列为底层写法。

- **反模式二（多数 flush 本身不必要）**：Nop 在**应用层生成主键**（`saveEntity` 时即在内存对象上赋 ID，无需 DB 往返），且 GraphQL 方法执行完毕**自动 flush**。手动 flush 仅在"后续直接执行 SQL/EQL（如 `@SqlLibMapper`）绕过 ORM session"时才需要（`docs-for-ai/05-examples/ibiz-and-bizmodel.java:240` 要点 8）。
  - **`createFromRequisition`（`ErpPurOrderBizModel.java:175-180`）**：注释"flush 使订单 ID 落地，再保存行"是**误解**——`dao().saveEntity(order)` 后 `order.getId()` 已在内存可用，子行 `saveEntity` 直接引用即可，flush 多余。
  - **`existsActiveByRequisition`（`:190-194`）**：flush 在 `dao().findAllByQuery()` 之前。`findAllByQuery` 是 ORM 层查询（非裸 SQL/mapper），不属于"必须先 flush"的情形；即便确需 flush，也应 `orm().flushSession()`。

- **建议**：移除 `@Inject IOrmTemplate` 字段；逐一复核 8 处 flush——**保留**条件为其后紧跟 `@SqlLibMapper`/原生 SQL，**或**跨域 `REQUIRES_NEW` 独立会话调用（如 `ErpSalReturnBizModel:133`/`ErpPurReturnBizModel:130` 在退货过账前，须刷盘避免独立会话丢失暂存 DONE/余额态）；经草案审查核实 8 处中 **6 处可删、2 处保留**（保留的改 `orm().flushSession()`）。
  > **复审补正**：初版建议仅列"mapper/原生 SQL"为保留条件，遗漏了 `REQUIRES_NEW` 独立会话这一合法 flush 场景，已据独立草案审查补正。

### D6【信息】view.xml 页面定制尚未开展（阶段预期，非违规）

- **现状**：`src/main/resources` 下 279 个自定义 view.xml，但 **157 个为 18 行、122 个为 19 行**（codegen 骨架）；**无** 30–5000 行区间的定制页面。
- **结论**：AMIS 页面定制尚未开始，全部为 `nop-cli gen` 生成的标准 CRUD 空壳。
- **对照**：`docs/backlog/README.md` roadmap 将页面定制列为后续里程碑，**符合当前阶段**，不计为偏差。

## 4. 修复优先级建议

| 优先级 | 项 | 建议动作 | 影响面 |
|--------|----|---------|--------|
| P1 | D2 数据权限旁路 | helper 方法改用 `requireEntity()`/`doFindList()` | 15 个 BizModel（含 master-data/finance），约 48 处 |
| P1 | D3 业务动作字段建模 | 三模式统一 `stdDomain="userId"`（模式 A 补标记+改名；模式 C 改 userId）；hr 双员工表另议 | ~10 域模型（模式 A）+ cs/hr + BizModel 写入逻辑 |
| P2 | D1 字典 int→string | 业财一体打通前人工裁决是否迁移（含数据） | 18 域 + 历史/测试数据 |
| P3 | D4 amount/boolFlag | amount scale→2；boolFlag→BOOLEAN | 18 域模型（重新生成） |
| P4 | D5 IOrmTemplate + 多余 flush | 移除 `@Inject IOrmTemplate`；删除非 mapper 前置的 flush；保留的改 `orm().flushSession()` | 7 个 BizModel，8 处 flush |
| — | D6 页面定制 | 按 roadmap 推进，非本次审计纠正项 | 后续阶段 |

## 5. 审计方法与局限

- **覆盖**：ORM 源模型全量扫描（`module-*/model/*.orm.xml`）、自定义 Java 全量扫描（排除 `_gen/` 与 `_` 前缀）、`git diff` 生成物完整性校验。
- **抽样精读**：`ErpPurOrderBizModel`、`IErpPurOrderBiz`、`ErpPurErrors`、`app-erp-purchase.orm.xml` 作为代表性样本深读。
- **未深入**：个别域 BizModel（crm/hr/mfg 等扩展域）的业务逻辑正确性仅做了反模式扫描，未逐行审计状态机可达性；如需可补充 `state-machine-business-review-prompt.md` 维度审计。
- **复审修正**：
  - **D3**：初版建议统一到 `domain="createdBy"`，经讨论确认 `approvedBy`/`postedBy`/`closedBy` 是业务动作责任字段（非被动审计），应统一 `stdDomain="userId"`；并据此修正 `nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md` 列域设计章节（消除 operatorId 内部矛盾、拆分业务动作字段与被动审计、操作人不建员工引用）。
  - **D5**：初版定性为"边界场景、可探索基类替代"，经核实 `CrudBizModel.orm()`（`CrudBizModel.java:269`）已提供 flushSession，注入 `IOrmTemplate` 多余；且 Nop 应用层生成主键 + GraphQL 自动 flush，多数 flush 调用本身不必要（仅 `@SqlLibMapper`/原生 SQL 前置才需要）。
- **裁决性质**：本审计为合规性（实现 vs 平台规范）审计，非计划结束审计；P1–P4 为建议，需人工确认后落地。

## 6. 相关文档

- `nop-entropy/docs-for-ai/00-start-here/ai-defaults.md` — 反模式表与自检清单
- `nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md` — 字典/domain/关系/审计字段规范
- `nop-entropy/docs-for-ai/02-core-guides/cross-module-entity-reference.md` — 跨模块引用四机制
- `docs/context/project-context.md` — 当前阶段（codegen 完成、待 BizModel 深化）
- `docs/backlog/README.md` — 路线图与里程碑
