# 科目映射规则（GL Mapping Rules）— A1

> Owner Doc for `deepening-roadmap.md §A1`。

本文件是 `ErpFinGlMappingRule`（科目映射规则表）+ `IErpFinGlMappingResolver`（解析引擎）的应用层业务设计
**稳定基线**，描述业务语义、优先级链算法、缓存策略、Provider 集成契约与反模式自检。字段集真相源在
`module-finance/model/app-erp-finance.orm.xml`；本文件不复刻字段定义，只解释字段语义与组合规则。

---

## §1 目的与范围

### 1.1 解决的问题

同一 `businessType` 的同一 `accountKey` 在不同业务维度下需要指向不同科目。例如 `AP_INVOICE` 的
`PURCHASE` 键：

- 默认账套 + 默认物料类别 → `1403 在途物资`
- 默认账套 + 原材料类别 → `1403 原材料`（更细颗粒）
- 税务账套 + 默认物料类别 → `5001 库存商品`（税务口径不同于财务口径）

Provider 既有路径直接硬编码最终 `subjectCode` 常量（如 purchase 域 PURCHASE 键回落 `"1403"`），无法在不动 Java 代码的前提下按维度外部化覆盖。本规则表作为 **Provider 之上的可选多维覆盖层**，使运维能直接配置规则而非 redeploy。

### 1.2 与 `ErpMdSubjectMapping` 边界（关键裁决）

| 维度 | `ErpMdSubjectMapping`（既有） | `ErpFinGlMappingRule`（A1 新增） |
|------|------------------------------|--------------------------------|
| **职责定位** | post-resolution 跨账套转换 | pre-resolution 多维业务规则 |
| **输入** | 已解析的 `sourceSubjectId` + 目标 `acctSchemaId` | `businessType` + `accountKey` + 业务维度 |
| **输出** | `targetSubjectId`（同一业务科目在不同账套的编码映射） | `targetSubjectCode`（建议的科目编码字符串） |
| **处理问题** | "同一业务科目在不同账套下用不同编码"（如管理账 `1403` → 税务账 `5001`） | "同一 businessType+accountKey 在不同业务维度下指向不同科目" |
| **执行时机** | 科目解析之后（facts 已 resolveSubjects） | 科目解析开头（既有 code→subject 查找之前） |
| **真相源** | `module-master-data/model/app-erp-master-data.orm.xml` `ErpMdSubjectMapping` | `module-finance/model/app-erp-finance.orm.xml` `ErpFinGlMappingRule` |

**三层职责不重叠**：Provider 生成 `VoucherFact{subjectCode, accountKey}` →（A1）规则匹配覆盖 `subjectCode`
→（既有）`resolveSubjects` 按 code 查 `ErpMdSubject` →（既有）`translateFactsForSchema` 经
`ErpMdSubjectMapping` 跨账套翻译 subjectId。

### 1.3 与 GL Distribution 边界

GL Distribution（科目分摊）按部门/项目**拆分一条分录为多条**，由 `posting.md §FactsValidator` 的
`IErpFinFactsValidator` 链实现，不在 A1 范围。A1 只解决 `accountKey → subjectCode` 单条解析。

### 1.4 与 A2/A3 边界

A2（预算多年度）+ A3（多公司深度）在 A1 规则表之上扩展：

- A2 加 `budget-scenario` 维度（规则按预算方案区分）+ 承付凭证类型。
- A3 加 `intercompany` 维度（规则按公司间交易类型区分）+ 合并规则。

A1 仅落地基础规则表 + 多维（partner/material/warehouse/department/project/acctSchema）+ 解析引擎。

---

## §2 实体字段表

真相源：`module-finance/model/app-erp-finance.orm.xml` `ErpFinGlMappingRule` 实体。本节解释字段语义。

> 注（2026-08-22，plan `docs/plans/2026-08-22-0002-1-bigint-id-m21-finance-migration.md`）：id 类字段（id/orgId/acctSchemaId/partnerGroupId/materialCategoryId/warehouseId/departmentId）**Java 层已 String 化**（id-string 迁移 M2.1）；下表「类型」列为 DB 列类型（BIGINT 保持不变，DDL 零变化）。`projectId` 为登记册延后列（prj 未迁移，Java 层暂保持 Long，M2.7 同批翻转）。

| 字段 | 类型 | 必填 | 语义 |
|------|------|------|------|
| `id` | BIGINT | ✓ | 主键 |
| `orgId` | BIGINT | ✓ | 核算组织（多组织隔离） |
| `businessType` | VARCHAR(30) | ✓ | 业务类型（字典 `erp-fin/business-type`，如 `AP_INVOICE`） |
| `accountKey` | VARCHAR(50) | ✓ | 科目占位符键（字典 `erp-fin/account-key`，如 `PURCHASE`/`INPUT_VAT`/`ACCOUNTS_PAYABLE`） |
| `acctSchemaId` | BIGINT | – | 账套 ID；**NULL = 通配**（适用所有账套） |
| `partnerGroupId` | BIGINT | – | 往来单位组 ID；NULL = 通配。**注：当前 master-data 无 `ErpMdPartnerGroup` 实体**，此字段为预留扩展点（业务方可自维护组 ID 列表） |
| `materialCategoryId` | BIGINT | – | 物料分类 ID（FK → `ErpMdMaterialCategory`）；NULL = 通配 |
| `warehouseId` | BIGINT | – | 仓库 ID（FK → `ErpMdWarehouse`）；NULL = 通配 |
| `departmentId` | BIGINT | – | 部门 ID（FK → `ErpMdOrganization`）；NULL = 通配 |
| `projectId` | BIGINT | – | 项目 ID（FK → `ErpPrjProject`）；NULL = 通配 |
| `targetSubjectCode` | VARCHAR(50) | ✓ | 命中后建议的目标科目编码字符串（**不**做 FK 到 `ErpMdSubject`，避免多账套下 FK 不一致） |
| `priority` | INTEGER | ✓ | 优先级数值；**0 = 最低（default 规则）**；数值越大优先级越高（≥100 为精确规则惯例） |
| `isActive` | BOOLEAN | ✓ | 是否启用；默认 `true` |
| 标准审计字段 | – | – | `delVersion`/`version`/`createdBy`/`createTime`/`updatedBy`/`updateTime` |

### 2.1 `targetSubjectCode` 而非 `targetSubjectId` 的设计理由

裁决理由（3 项）：

1. **与 Provider 既有输出同构**：Provider 既直接生成 `VoucherFact{subjectCode="1403"}`（字符串），
   resolver 输出 String 直接覆盖 `fact.subjectCode`，零类型转换。
2. **复用既有 `resolveSubjects` 逻辑**：覆盖 `subjectCode` 后由既有 `code → ErpMdSubject.findByCode`
   完成转换；不需要 resolver 自己持有 `ErpMdSubject` 引用。
3. **避免多账套 FK 不一致**：若用 `targetSubjectId`，同一规则在不同账套下需指向不同 `subjectId`，
   FK 约束在多账套场景（账套 A 的科目表 ≠ 账套 B 的科目表）会断裂。String `subjectCode` 由
   `ErpMdSubjectMapping`（§1.2）在跨账套阶段处理，职责清晰。

### 2.2 维度 NULL 语义（通配）

NULL 维度 = **通配任意值**。例如规则 `(AP_INVOICE, PURCHASE, acctSchemaId=NULL, partnerGroupId=NULL,
materialCategoryId=NULL, warehouseId=NULL, ...)` 匹配任意账套 + 任意 partner + 任意物料类别 ... 的
`AP_INVOICE+PURCHASE` 输入。该规则典型 `priority=0`（default 兜底）。

精确规则用具体非 NULL 维度 + `priority≥100`，命中后覆盖 default 规则。

### 2.3 `partnerGroupId` 字段的预留语义

当前 master-data 域 `ErpMdPartner` 仅有 `customerGroup`（VARCHAR 字符串标签）字段，**没有 `ErpMdPartnerGroup`
实体**。本字段为**预留扩展点**：

- 业务方可在 `ErpMdPartnerGroup` 实体化后（如 C2 跨境贸易扩展或 A3 多公司深度时引入），直接复用此字段。
- 当前 resolver 不会自动从 `partnerId` 推导 `partnerGroupId`（无对应实体 lookup 路径）；调用方需在
  `GlMappingDimensions` 中显式传入 `partnerGroupId`（如业务上下文已有）。
- resolver 内部对 `partnerGroupId` 仅做相等匹配，不尝试扩展查询。

---

## §3 优先级链算法

> **orgId 维度激活**：本节「按 `(orgId, businessType, accountKey)` 索引」自 A1 起即为 owner doc 设计真相源，但 resolver 实现自 A1 起漂移——orgId 解析硬编码返回 null、cache key 丢弃 `rule.orgId`。**此 doc/code drift 已收口**：代码现与设计一致，经 config-gate `erp-fin.gl-mapping.org-dimension-enabled`（默认 `false`）激活：关闭态（默认）= org-agnostic（orgId 不参与 cache key 与匹配，向后兼容现状）；开启态 = org 精确匹配（cache 按 `(orgId, businessType, accountKey)` 分桶 + exact 校验 + `specificity` 含 orgId 计数）。orgId 维度仅做 exact 匹配（非通配），因 `orgId` 在 ORM 层 `mandatory="true"`。

### 3.1 算法

解析按以下步骤（实现细节见 `docs/architecture/processor-extension-pattern.md`）：

1. **候选集**：按 `(orgId, businessType, accountKey)` 索引取出候选规则（启动期 eager load 全表）。候选为空 → 返回 `null`（保留 Provider fallback）。
2. **维度过滤**：逐条规则，跳过 `isActive=false`；对每个非 NULL 维度（acctSchemaId / partnerGroupId / materialCategoryId / warehouseId / departmentId / projectId）做相等匹配，不匹配则跳过。
3. **排序**：命中规则按 `(priority DESC, 维度具体度 DESC)` 排序。维度具体度 = 非 NULL 维度字段数（5 业务维度）+（acctSchemaId 非 NULL ? 1 : 0）。
4. **胜出**：取排序后首条的 `targetSubjectCode`。

### 3.2 priority 语义澄清

- `priority = 0` 表示**最低**（default 规则，全 NULL 维度兜底）。
- `priority` 数值越大优先级越高（≥100 为精确规则惯例，但非强制）。
- 同一 priority 内按维度具体度（非 NULL 维度数）DESC 排序；最高具体度者胜出。
- 这与 seed 中 "全 NULL 维度 priority=0" 语义一致：default 规则在 priority 维度输给任何精确规则。

### 3.3 决策表示例

输入：`businessType=AP_INVOICE, accountKey=PURCHASE, dimensions={materialCategoryId=42}, acctSchemaId=7`

| 规则 | acctSchemaId | materialCategoryId | 其他维度 | priority | 命中？ | 备注 |
|------|--------------|--------------------|----------|----------|--------|------|
| R1 | NULL | NULL | 全 NULL | 0 | ✓ | default 兜底 |
| R2 | 7 | NULL | 全 NULL | 100 | ✓ | 账套精确，物料通配 |
| R3 | NULL | 42 | 全 NULL | 100 | ✓ | 账套通配，物料精确 |
| R4 | 7 | 42 | 全 NULL | 100 | ✓ | 账套+物料双精确 |
| R5 | 8 | NULL | 全 NULL | 200 | ✗ | acctSchema 不匹配 |

排序（priority DESC, 具体度 DESC）：

1. R4 priority=100, 具体度=3（acctSchemaId+materialCategoryId+1 通用计数待定 → 按 §3.1 算法 acctSchemaId 单独 +1，5 维度占 1 → 共 2 维非 NULL；具体度 = 2）
2. R2 priority=100, 具体度=1（acctSchemaId）
3. R3 priority=100, 具体度=1（materialCategoryId）
4. R1 priority=0, 具体度=0

**胜出 = R4**（priority 相同时按具体度 DESC 打破并列）。

### 3.4 维度数据来源（resolver 内部扩展）

`GlMappingDimensions` 输入字段：`partnerId` / `materialId` / `warehouseId` / `departmentId` / `projectId`
（5 个业务原始 ID）。resolver 内部按需扩展：

| 输入字段 | 扩展目标 | 扩展路径 |
|----------|----------|----------|
| `materialId` | `materialCategoryId` | `IErpMdMaterialBiz.get(materialId).categoryId` |
| `partnerId` | `partnerGroupId` | **不扩展**（`ErpMdPartnerGroup` 不存在）；调用方需显式传 `partnerGroupId` |
| `warehouseId` | 直接使用 | warehouseId 即规则维度 |
| `departmentId` | 直接使用 | departmentId 即规则维度 |
| `projectId` | 直接使用 | projectId 即规则维度 |

扩展为 best-effort：materialId 找不到或 material 无 categoryId → `materialCategoryId=NULL`（参与通配）。

---

## §4 缓存策略 + 失效机制

> **orgId 分桶对齐**：cache 数据结构与 `:191` 规定一致——关闭态统一 `_` 桶（向后兼容），
> 开启态按 `(orgId, businessType, accountKey)` 分桶。`loadFromDb`（cache 禁用降级路径）同样按 orgId 精确过滤，
> 避免绕过分桶。config-gate 见 §3 orgId 维度激活注记。

### 4.1 进程内缓存设计

- **数据结构**：按 `(orgId, businessType, accountKey)` 索引的并发映射（实现细节见 `docs/architecture/processor-extension-pattern.md`）。
- **加载方式**：启动期 eager load 全表 + 按索引分桶；EstRows < 1000，启动开销可忽略。
- **TTL 配置项**：`erp-fin.gl-mapping.cache-ttl-seconds`（默认 `3600`，0 表示永久）。当前实现：进程内缓存 + 主动失效，TTL 仅作降级 fallback。
- **降级配置项**：`erp-fin.gl-mapping.cache-enabled`（默认 `true`）；`false` 时 `resolve` 每次查 DB。

### 4.2 主动失效机制

- **触发点**：规则 CRUD 标准方法（`save_`/`update_`/`delete_`/`deleteById_`）末尾触发缓存失效。
- **失效方式**：**全量 reload**（简单可靠；EstRows < 1000，全量 reload 开销 < 50ms）。
- **手动刷新入口**：`@BizMutation refreshCache()` — operator UI 工具栏按钮触发（多节点限制见 §6）。

### 4.3 多节点部署限制（Deferred）

当前进程内缓存仅覆盖单节点。多节点部署下：

- 规则变更经 TTL（默认 3600s）最终一致。
- 强一致需经 `NopSysEvent` 广播失效（Deferred — 触发条件：生产多节点 + 变更延迟投诉）。
- Operator UI 刷新按钮 tooltip 提示此限制。

### 4.4 ChangeLog 覆盖

实体 `tagSet` 含 `audit,audit-save`，复用平台 `NopSysChangeLog` 自动记录字段级变更审计（与
`ErpFinVoucherTemplate` 同范式）。运维改规则后可追溯。

---

## §5 Provider opt-in 集成契约

### 5.1 契约要点

A1 是 **Provider 之上的可选覆盖层**，向后兼容。Provider **不强制**设置 `accountKey`：

- Provider 不设置 `VoucherFact.accountKey`（即 `accountKey == null`）→ resolver 跳过该 fact，行为与
  A1 引入前完全一致。
- Provider 设置 `accountKey` → resolver 在 `resolveSubjects` 开头查询规则表：
  - 命中规则 → 用规则的 `targetSubjectCode` 覆盖 `fact.subjectCode`，其后既有 `code → ErpMdSubject`
    查找流程不变。
  - 未命中（规则表无对应 `businessType+accountKey` 任何配置）→ 默认不抛异常，记录 INFO 日志
    （`gl-mapping rule miss: ...`），保留 Provider 既有 `subjectCode`（向后兼容关键）。
  - 严格模式（`erp-fin.gl-mapping.strict-mode=true`，默认 `false`）→ 未命中抛
    `ERR_GL_MAPPING_NOT_FOUND`，生产稳定后启用。

### 5.2 既有 `VoucherFact.accountKey` 字段消费者清单（A1 落地前已存在）

`VoucherFact.accountKey` 字段自初代过账引擎落地即存在，A1 是**首个消费者**（不改字段定义）。既有生产者：

| 生产者 | 路径 | accountKey 字面量 | A1 是否提供默认规则 |
|--------|------|-------------------|---------------------|
| 银行对账调整 Provider | 生产 Provider | `BANK_RECV` / `ADJ_BANK_RECV` / `ADJ_BANK_PAID` / `BANK_PAID` | 否（运维可后续按需添加） |
| 默认模板 Provider | 默认模板 Provider | 复制 `ErpFinVoucherTemplateLine.accountKey`（运维自定义） | 否（模板路径独立解析，A1 不替换） |
| 跨账套传播 | 跨账套翻译阶段 | （信息性复制，不再次触发 resolver） | – |

**A1 跨账套传播语义**（关键）：resolver **仅在科目解析阶段运行一次**（pre-translation）；跨账套翻译阶段复制的 `accountKey` 在 translated fact 上**仅为信息性**（保留用于审计），**不再次触发 resolver**（避免双重解析）。如未来需在 target schema 下用不同规则重解析，需 successor plan 扩展（默认 Non-Goal）。

### 5.3 试点接入清单

| 域 | businessType | accountKey | 状态 |
|----------|--------------|------------|------|
| purchase | `AP_INVOICE` | `PURCHASE`（1403 在途物资） | 已接入 |
| purchase | `AP_INVOICE` | `INPUT_VAT`（2221 进项税） | 已接入 |
| purchase | `AP_INVOICE` | `ACCOUNTS_PAYABLE`（2202 应付账款） | 已接入 |
| sales | 各 businessType | 各键 | ⏳ Deferred（试点稳定 ≥ 2 周后批量接入） |
| inventory | 各 businessType | 各键 | ⏳ Deferred |
| assets/hr/maintenance | 各 businessType | 各键 | ⏳ Deferred |

### 5.4 接入步骤模板（5 步）

新 Provider 接入 A1 resolver 的标准流程：

1. **字典加 accountKey**：在 `module-finance/model/app-erp-finance.orm.xml` 的 `erp-fin/account-key` 字典
   中新增键（业务语义命名，避免与既有 in-wild 字面量冲突）；codegen 触发 `account-key.dict.yaml` 同步。
2. **VoucherFact 设置**：在 Provider 的 `createFacts` 中对应 fact 调 `fact.setAccountKey("XXX")`；
   既有 `subjectCode` 常量保留作为 fallback（规则表无匹配时仍走既有）。
3. **种子规则**：在测试中（或运维 UI）配置 default 规则（priority=0、全 NULL 维度、
   `targetSubjectCode=既有 SUBJECT_*`）—— 保持接入后行为不变。
4. **测试**：扩展 Provider 既有集成测试，验证（a）行为不变（无规则时走 fallback）+（b）覆盖生效
   （priority=100 + 精确维度 → 新 subjectCode）。
5. **文档**：在本文件 §5.3 试点清单更新状态。

### 5.5 试点边界（Non-Goals 重申）

- 不批量改造其他 Provider（仅 purchase 域 AP_INVOICE × 3 键试点）。
- 不替换默认模板 Provider 内部解析（模板路径独立；统一为 resolver 调用是 Deferred）。
- 不解决 `amountKey` 占位符替换（仍由 Provider 内部完成）。
- 不实现 GL Distribution（拆分一条分录为多条）。

---

## §6 Operator UI 交互

### 6.1 CRUD 页面

- **路径**：`module-finance/erp-fin-web/src/main/resources/_vfs/erp/fin/pages/ErpFinGlMappingRule/`
- **入口**：finance 菜单 → 总账（`fin-gl`）→ 科目映射规则
- **list grid 列**：`businessType` / `accountKey` / `acctSchemaId` / `partnerGroupId` / `materialCategoryId` /
  `warehouseId` / `departmentId` / `projectId` / `targetSubjectCode` / `priority` / `isActive`
- **默认排序**：`(businessType, accountKey, priority DESC)` — 运维直观查看优先级链
- **form 分组**：
  - `baseInfo[基本信息]`：`businessType` / `accountKey` / `acctSchemaId` / `priority` / `isActive`
  - `dimensions[维度条件]`：`partnerGroupId` / `materialCategoryId` / `warehouseId` / `departmentId` / `projectId`
  - `target[目标科目]`：`targetSubjectCode`（picker 选 `ErpMdSubject` 后回填 code，**不**存 subjectId 避免多账套 FK 不一致）
  - `audit[审计信息]`：`createdBy` / `createTime` / `updatedBy` / `updateTime`

### 6.2 控件

- `businessType` / `accountKey` 下拉经字典
- `targetSubjectCode` 引用 `ErpMdSubject` 既有 picker（选 subject 后回填 code）
- `isActive` Switch 控件

### 6.3 缓存手动刷新按钮

- **位置**：list grid toolbar
- **触发**：`@BizMutation refreshCache()` → 触发缓存失效
- **tooltip**："仅刷新本节点缓存；多节点生效请等待 TTL（默认 3600s）或重启"
- **多节点限制**：见 §4.3（Deferred 强一致广播）

### 6.4 strict-mode 切换说明

- 配置项 `erp-fin.gl-mapping.strict-mode`（默认 `false`）
- 切换方式：修改应用配置（重启生效；运行时切换属 Deferred）
- `true` 时空匹配抛 `ERR_GL_MAPPING_NOT_FOUND`；`false` 时空匹配保留 Provider fallback + INFO 日志
- 生产稳定期建议启用（捕获漏配规则）

---

## §7 反模式自检表

| 不要这样用 | 应该这样 |
|------------|----------|
| 用 A1 做 post-resolution 跨账套转换（输入已解析 subjectId） | 用 `ErpMdSubjectMapping`（post-resolution，§1.2 边界） |
| 用 A1 拆分一条分录为多条（按部门分摊金额） | 用 `IErpFinFactsValidator`（GL Distribution，§1.3 边界） |
| 用 A1 替换 `amountKey` 占位符解析 | 由 Provider 内部完成；A1 仅解决 `accountKey → subjectCode` |
| 在 resolver 中持有 `ErpMdSubject` 引用做 code→id 转换 | resolver 仅返回 `targetSubjectCode` 字符串；既有 `resolveSubjects` 完成转换（§2.1） |
| 让 resolver 在 `translateFactsForSchema` 后再次运行（双重解析） | resolver 仅运行一次 pre-translation（§5.2）；translated fact 的 `accountKey` 仅信息性 |
| 强制要求所有 Provider 必须设置 `accountKey` | A1 是 opt-in 覆盖层；不设置时行为完全不变（§5.1） |
| 默认 strict-mode=true（生产前未稳定） | 默认 false（试点期保留 fallback）；稳定后切换 |
| 在 PartnerGroup 实体不存在的当前阶段尝试自动 partnerId → partnerGroupId | partnerGroupId 仅相等匹配；由调用方显式传入（§2.3） |
| Provider 设置 accountKey 时丢弃既有 SUBJECT_* 常量 | 保留作为 fallback；规则表无匹配时仍走既有（§5.1） |
| 多节点部署下假设刷新按钮即时生效全集群 | tooltip 提示单节点限制；多节点需等待 TTL（§4.3） |

---

## intercompany 维度接入（A3）

> A3 承接 [`multi-company.md §与 Posting+GL Mapping 关系`](../architecture/multi-company.md) 既定义的 intercompany 维度接入，解除本文件 §A3 Deferred「intercompany 维度规则」。

### 维度扩展

`GlMappingDimensions` DTO 增 2 维：`fromOrgId` / `toOrgId`（跨法人交易双方组织）。resolver 的 `expandDimensions` 透传该 2 维（不自动扩展，因组织已是粒度）。

### 新增 accountKey（4 键）

`erp-fin/account-key` 字典增 4 INTERCOMPANY_* 键（由 intercompany 配对凭证生成器设置）：

| accountKey | 用途 | 默认回落编码 |
|------------|------|-------------|
| `INTERCOMPANY_AR` | 内部应收（AR 侧借方） | 1131 |
| `INTERCOMPANY_REVENUE` | 内部销售收入（AR 侧贷方） | 5001 |
| `INTERCOMPANY_COST` | 内部采购成本（AP 侧借方） | 1401 |
| `INTERCOMPANY_AP` | 内部应付（AP 侧贷方） | 2202 |

规则表无匹配时回落默认编码（保留向后兼容，不抛异常）。

### 消费路径

intercompany 配对凭证生成时直接调 `IErpFinGlMappingResolver.resolveSubjectCode(billType, accountKey, dims, acctSchemaId)`，不经科目解析钩子（因 INTERCOMPANY_* 不走 Provider 路由）。

---

## §8 Provider 批量接入清单

> A1 后续：将 GL Mapping Resolver 覆盖从 1 试点 Provider 扩展至全部 28 routing Provider。
> 接入策略裁决（Phase 1 Decision）：**同一业务含义优先复用现有通用键**（INVENTORY/AP/AR/COGS/REVENUE/BANK_DEPOSIT/
> FIXED_ASSET/ACCUMULATED_DEPRECIATION/DEPRECIATION_EXPENSE/SALARY_PAYABLE/INPUT_VAT/OUTPUT_TAX 等）；
> **域独有的科目语义新增键**（制造 WIP/差异、委外物资、产成品、各类营业外收支、票据、员工往来、利息、汇兑等）。
> 通用键跨域共享（同 accountKey 在不同 businessType 下可指向不同 targetSubjectCode，规则按 (businessType, accountKey) 索引）。

### 8.1 accountKey 字典完整表

通用键（既有，跨域复用）：`PURCHASE` `INPUT_VAT` `ACCOUNTS_PAYABLE` `INVENTORY` `AP` `AR` `OUTPUT_TAX` `BANK_DEPOSIT`
`CASH` `FIXED_ASSET` `ACCUMULATED_DEPRECIATION` `DEPRECIATION_EXPENSE` `SALARY_PAYABLE` `SALARY_EXPENSE` `TAX_PAYABLE`
`REVENUE` `COGS` `RETAINED_EARNINGS` + BankRecon/Template 既有键 + INTERCOMPANY_*（A3）。

新增域专用键（23）：

| accountKey | 中文标签 | 典型 fallback 编码 | 接入域（场景） |
|------------|----------|--------------------|---------------|
| `COST_VARIANCE` | 成本差异 | 6603 | inventory（成本调整） |
| `PURCHASE_PRICE_VARIANCE` | 材料成本差异(PPV) | 1404 | inventory（采购价差） |
| `MANUFACTURING_WIP` | 在制品(WIP) | 1411 | inventory（完工入库）/ mfg（发料、差异 WIP 侧） |
| `MANUFACTURING_VARIANCE` | 制造差异 | 1410~1416 | mfg（差异侧） |
| `SUBCONTRACT_MATERIAL` | 委外物资 | 1408 | mfg（委外发料/费用/收货） |
| `FINISHED_GOODS` | 产成品 | 1405 | mfg（委外收货） |
| `CIP` | 在建工程 | 1603 | assets（资本化）/ projects（CLOSE 转固） |
| `IMPAIRMENT_LOSS` | 资产减值损失 | 6702 | assets（减值） |
| `IMPAIRMENT_PROVISION` | 固定资产减值准备 | 1604 | assets（减值） |
| `CAPITAL_RESERVE` | 资本公积 | 4002 | assets（重估增值） |
| `NON_OPERATING_INCOME` | 营业外收入 | 6301 | assets（盘盈） |
| `NON_OPERATING_EXPENSE` | 营业外支出 | 6711 | assets（清理损益/盘亏）/ quality（报废） |
| `PROJECT_COST` | 项目成本 | 5101 | projects（结算） |
| `PROFIT_LOSS` | 本年利润 | 4103 | projects（结算） |
| `MAINTENANCE_EXPENSE` | 维修费用 | 6602 | maintenance / assets（维修费用/发料/人工） |
| `MAINTENANCE_CLEARING` | 维修中转清算 | 2502 | maintenance / assets（维修费用/资本化） |
| `NOTES_RECEIVABLE` | 应收票据 | 1121 | finance（票据） |
| `NOTES_PAYABLE` | 应付票据 | 2203 | finance（票据） |
| `EMPLOYEE_ADVANCE_RECEIVABLE` | 其他应收款-员工预支 | 1221 | finance（员工借款） |
| `EMPLOYEE_PAYABLE` | 其他应付款-员工 | 2241 | finance（员工借款抵扣 / 报销） |
| `FINANCIAL_EXPENSE` | 财务费用-利息支出 | 6603 | finance（授信利息 / 票据贴现息） |
| `EXCHANGE_GAIN_LOSS` | 汇兑损益 | 6051 | finance（票据贴现） |
| `ADMIN_EXPENSE` | 管理费用 | 6602 | finance（报销） |

> 编码冲突说明：`6602` 在折旧(DEPRECIATION_EXPENSE)/维修(MAINTENANCE_EXPENSE)/报销(ADMIN_EXPENSE)三场景含义不同，
> `6603` 在成本差异(COST_VARIANCE)/利息(FINANCIAL_EXPENSE)两场景含义不同。accountKey 按语义拆分键以保留
> 规则覆盖粒度；规则表无匹配时各自回落至 Provider 既有 fallback 编码（行为不变）。

### 8.2 业务类型 × accountKey 接入清单（覆盖全部 28 routing Provider）

下表覆盖全部 28 routing Provider（dormant 的 Commitment/Intercompany 不在范围）。`通用` = 复用既有键。

| 域 | 业务类型 | fact.accountKey（按 Dr→Cr 顺序） |
|----|----------|----------------------------------|
| purchase | AP_INVOICE | PURCHASE / INPUT_VAT / ACCOUNTS_PAYABLE |
| purchase | PAYMENT | ACCOUNTS_PAYABLE / BANK_DEPOSIT |
| purchase | PURCHASE_RETURN | ACCOUNTS_PAYABLE / INVENTORY |
| sales | AR_INVOICE | AR / REVENUE / OUTPUT_TAX |
| sales | RECEIPT | BANK_DEPOSIT / AR |
| sales | SALES_RETURN | INVENTORY / COGS |
| inventory | PURCHASE_INPUT | INVENTORY / ACCOUNTS_PAYABLE |
| inventory | MANUFACTURING_RECEIPT | INVENTORY / MANUFACTURING_WIP |
| inventory | SALES_OUTPUT | COGS / INVENTORY |
| inventory | COST_ADJUSTMENT | INVENTORY↔COST_VARIANCE（方向相关） |
| inventory | PURCHASE_PRICE_VARIANCE | PURCHASE_PRICE_VARIANCE↔ACCOUNTS_PAYABLE |
| inventory | LANDED_COST | INVENTORY(借) / ACCOUNTS_PAYABLE(贷) |
| mfg | MANUFACTURING_ISSUE | MANUFACTURING_WIP(汇总借) / INVENTORY(贷按物料) |
| mfg | SUBCONTRACT_ISSUE | SUBCONTRACT_MATERIAL(汇总借) / INVENTORY(贷按物料) |
| mfg | SUBCONTRACT_FEE | SUBCONTRACT_MATERIAL / ACCOUNTS_PAYABLE |
| mfg | SUBCONTRACT_RECEIPT | FINISHED_GOODS(借按物料) / SUBCONTRACT_MATERIAL(汇总贷) |
| mfg | PRODUCTION_VARIANCE | MANUFACTURING_VARIANCE↔MANUFACTURING_WIP（4要素方向相关） |
| assets | DEPRECIATION | DEPRECIATION_EXPENSE / ACCUMULATED_DEPRECIATION |
| assets | DISPOSAL | ACCUMULATED_DEPRECIATION + BANK_DEPOSIT + NON_OPERATING_EXPENSE + FIXED_ASSET |
| assets | ASSET_INVENTORY_ADJUSTMENT | FIXED_ASSET↔NON_OPERATING_INCOME(盘盈) / NON_OPERATING_EXPENSE↔FIXED_ASSET(盘亏) |
| assets | VALUE_ADJUSTMENT | IMPAIRMENT_LOSS↔IMPAIRMENT_PROVISION / FIXED_ASSET↔CAPITAL_RESERVE / IMPAIRMENT_LOSS↔FIXED_ASSET |
| assets | CAPITALIZATION | FIXED_ASSET / CIP 或 BANK_DEPOSIT（按 sourceType） |
| assets | ASSET_SPLIT | FIXED_ASSET(借/贷) |
| assets | MAINTENANCE_CAPITALIZATION | FIXED_ASSET / MAINTENANCE_CLEARING 或 BANK_DEPOSIT |
| assets | ASSET_MERGE | FIXED_ASSET(借/贷) |
| assets | MAINTENANCE_EXPENSE | MAINTENANCE_EXPENSE / MAINTENANCE_CLEARING 或 BANK_DEPOSIT |
| projects | PROJECT_SETTLEMENT | FIXED_ASSET/CIP(CLOSE转固) 或 PROJECT_COST+PROFIT_LOSS+REVENUE(结转) |
| quality | NCR_SCRAP | NON_OPERATING_EXPENSE / INVENTORY |
| maintenance | MAINTENANCE_LABOR | MAINTENANCE_EXPENSE / SALARY_PAYABLE |
| maintenance | MAINTENANCE_ISSUE | MAINTENANCE_EXPENSE(汇总借) / INVENTORY(贷按物料) |
| finance | EMPLOYEE_ADVANCE | EMPLOYEE_ADVANCE_RECEIVABLE / BANK_DEPOSIT |
| finance | EMPLOYEE_ADVANCE_SETTLE | BANK_DEPOSIT/EMPLOYEE_ADVANCE_RECEIVABLE(CASH) 或 EMPLOYEE_PAYABLE/EMPLOYEE_ADVANCE_RECEIVABLE(OFFSET) |
| finance | EXPENSE_CLAIM | ADMIN_EXPENSE / INPUT_VAT / EMPLOYEE_PAYABLE 或 BANK_DEPOSIT |
| finance | NOTES_PAYABLE_ISSUED | ACCOUNTS_PAYABLE / NOTES_PAYABLE |
| finance | NOTES_PAYABLE_HONORED | NOTES_PAYABLE / BANK_DEPOSIT |
| finance | CREDIT_FACILITY_INTEREST | FINANCIAL_EXPENSE / BANK_DEPOSIT |
| finance | NOTES_RECEIVABLE_RECEIVED | NOTES_RECEIVABLE / AR |
| finance | NOTES_RECEIVABLE_DISCOUNTED | BANK_DEPOSIT / FINANCIAL_EXPENSE / EXCHANGE_GAIN_LOSS / NOTES_RECEIVABLE |
| finance | NOTES_RECEIVABLE_ENDORSED | ACCOUNTS_PAYABLE / NOTES_RECEIVABLE |
| finance | NOTES_RECEIVABLE_COLLECTION | BANK_DEPOSIT / NOTES_RECEIVABLE |

### 8.3 接入步骤（对齐 §5.4 五步模板）

每个 Provider 接入遵循 §5.4：(1) 字典加 accountKey（本次一次性补全 23 键）→ (2) createFacts 的 fact 构造设 accountKey，
既有 SUBJECT_* 保留作 fallback → (3) 种子 default 规则（运维按需；测试中按需注入 priority=0 全 NULL 维度规则）→
(4) 测试覆盖命中覆盖 + 未命中 fallback 双路径 → (5) 本清单 §8.2 更新状态。

### 8.4 反模式自检（扩展 §7）

| 不要这样用 | 应该这样 |
|------------|----------|
| 为复用通用键而丢弃域专用语义（如把委外物资当 INVENTORY） | 委外物资/产成品/WIP 用域专用键，保留 fallback 编码差异可被规则覆盖 |
| 同一 voucher 内把借/贷配对的两行设为相同 accountKey | 配对行（如差异↔WIP）用不同键，否则规则会把两行覆盖为同科目破坏复式 |
| 对方向相关的 fact（INCREASE/DECREASE）按方向拆键 | 同键 + 既有 SUBJECT_* 方向逻辑保留；方向不进 accountKey（DC 由 fact.dcDirection 承载） |

---



## §9 路由验证

GL Mapping 路由（规则命中 → 科目覆盖）经 JUnit 单层验证 + 浏览器层全栈路径覆盖。验证的**行为契约**：规则命中时对应 accountKey 的凭证行 `subjectCode` 被覆盖、未命中键保留 Provider 默认科目；orgId 维度开启态下 cache 按 `(orgId, businessType, accountKey)` 分桶，非匹配 org 保留默认（§3 orgId 维度激活注记）。测试用例与断言范式见 `tests/e2e/business-actions/fin-gl-mapping-routing.action.spec.ts` 与 resolver JUnit 测试。

---



- Posting 设计：`docs/design/finance/posting.md` §科目映射 / §凭证模板机制 / §过账引擎 / §失败处理策略
- 多账套：`docs/design/finance/multiple-accounting-schemas.md` §科目映射规则
- Processor 范式：`docs/architecture/processor-extension-pattern.md`
- 路线图：`docs/backlog/deepening-roadmap.md` §A1
