# 主数据统一 Party 身份查询（Unified Party Identity）

> Owner Doc for `deepening-roadmap.md` §Milestone C §C1（plan `2026-07-21-0827-2-master-data-unified-party-identity-query.md`）。
> Plan Status: active。本文件在 Plan Phase 0 落地，§6 试点记录在 Plan Phase 3 补齐。

## 1. 目的与范围

主数据域现存三类"业务方"实体，分散在各自的 CRUD 页面与 `IErpMd*Biz` 接口中，跨实体检索需用户先判断目标类型再进入对应模块。本文件定义一个统一的 Party 查询抽象：单一关键字入口 + 跨实体投影 DTO + 跨域查询接口 + 联合 picker。

**C1 范围（本 owner doc）**：

- 三实体联合检索：`ErpMdPartner`（往来单位）/ `ErpMdEmployee`（职员）/ `ErpMdOrganization`（组织）。
- 统一 DTO：`PartyRef`（含 partyType + partyId + 公共字段 + extension 容器）。
- 跨域查询接口：`IErpPartyBiz`（首例"非实体 + 跨域 `@Inject`"组合，§4 详述约定）。
- 联合 picker：`party-search/main.picker.page.yaml`（全仓库首例手写 picker.page.yaml）。
- F7 删除引用预览 SPI 模式扩展：跨 Party 类型引用扫描。

**显式边界**：

| 主题 | 归属 | 说明 |
|------|------|------|
| C2 跨境贸易字段扩展 | 独立 successor plan | 依赖 C1 Party 抽象基础但 ORM 改动（`ErpMdMaterial` 加字段 + `ErpMdMaterialCustoms` 新实体）归 C2 |
| `ErpMdUserAccount` 接入统一 Party | Deferred | 属 nop-auth 平台域，账户语义 ≠ 业务方 |
| 既有业务单据 FK 改造（`PurOrder.partnerId` 等） | Deferred | 改为通用 `partyId + partyType` 需改 ORM + 业务逻辑 + 全测试，高风险大范围；C1 仅提供查询层 |
| 物化视图 / 反向索引表 | Deferred successor | 3 实体 EstRows < 11K，union SQL 性能可接受 |
| Party 合并 / 去重（Golden Record / MDM） | Deferred | 同一自然人在多 Party 类型重复的合并语义属 MDM 范畴 |
| 全文索引 | Deferred successor | 默认 `LIKE '%keyword%'` 足够 |

## 2. Party 抽象边界

### 2.1 三实体字段对齐表（Explore (b) 结论）

| PartyRef 字段 | ErpMdPartner | ErpMdEmployee | ErpMdOrganization | 备注 |
|---------------|--------------|---------------|-------------------|------|
| `partyType` | PARTNER | EMPLOYEE | ORGANIZATION | 由 `ErpPartyType` enum 携带 |
| `partyId` | id | id | id | Long |
| `code` | code | code | code | 全员公共 |
| `name` | name | name | name | 全员公共 |
| `phone` | phone | phone | — | **Organization 无此列，投影为 null** |
| `email` | email | email | — | **Organization 无此列，投影为 null** |
| `status` | status | status | status | 全员公共，字典 `erp-md/active-status` |
| `displayName` | code + " - " + name | 同左 | 同左 | 拼接字符串 |
| `extension` (Map) | `partnerType` | `position`, `orgId`, `partnerId` | `orgType`, `parentId`, `functionalCurrencyId` | 实体特定字段，容器兼容未来扩展 |

**关键字检索范围**（Phase 0 Decision）：`code`/`name`/`phone`/`email` 4 字段 OR 模糊匹配（`LIKE '%keyword%'`）；Organization 仅匹配 `code`/`name`（无 phone/email 列）。keyword < 2 字符返回空（避免全表扫描）；SQL 注入防护经平台标准 `QueryBean` + 参数化查询。

### 2.2 Non-Goal 实体清单

| 实体 | 不纳入原因 |
|------|-----------|
| `ErpMdUserAccount` | 属 nop-auth 平台域（登录账户语义），非 master-data 业务方 |
| `ErpMdPartnerContact` | 联系人是 Partner 的从属实体，非独立业务方 |
| `ErpMdPartnerAddress` | 地址是 Partner 的从属实体 |
| `ErpMdWarehouse` / `ErpMdLocation` | 库存地点语义，非业务方 |
| `ErpMdCostCenter` | 成本归集维度，非业务方 |

## 3. 查询策略（Explore (a) 结论）

### 3.1 三方案权衡表

| 方案 | 性能（EstRows < 11K） | 代码复杂度 | 维护成本 | 平台范式一致性 | 结论 |
|------|---------------------|-----------|---------|--------------|------|
| (a) `IOrmTemplate` + 3 次查询 + Java merge | 3 次顺序查询 < 30ms 合计 | 低（每实体 1 次 findPage + 字段投影循环） | 低 | **高** — 与 `ErpMdDashboardBizModel` 既有范式同构 | **选定** |
| (b) `@SqlLibMapper` + native union SQL | 单次查询 < 15ms | 中（异构字段投影 union + NULL 填充） | 中（SQL 与 ORM 分离） | 中 — 平台支持但 dashboard 未使用 | 备选 |
| (c) 物化视图 / 反向索引表 | 单次查询 < 5ms | 高（同步开销 + schema 改动） | 高 | 低 — 平台无原生支持 | 拒绝（Deferred successor） |

### 3.2 决策

**选定方案 (a) `IOrmTemplate` + 3 查询 + Java merge**（Phase 0 Decision 裁决依据）：

- 3 实体字段异构（Organization 无 phone/email），union SQL 投影复杂；
- 3 次查询 + Java merge 可读性高 + 易于扩展新 Party 实体；
- EstRows 合计 < 11K（Partner < 10K / Employee < 1K / Organization < 100），性能可接受；
- 与 `ErpMdDashboardBizModel`（`@Inject IDaoProvider + IOrmTemplate`，line 42-45）既有范式同构。

**降级路径**：若生产性能监控 P95 > 500ms 或 Partner EstRows > 100K，转方案 (b) `@SqlLibMapper` + native union SQL；再触发则升级方案 (c) 物化视图（§7 触发条件）。

## 4. IErpPartyBiz 接口契约 + 非实体 BizModel 接口暴露约定

### 4.1 接口签名

```java
public interface IErpPartyBiz {
    List<PartyRef> findParties(String keyword, Set<ErpPartyType> partyTypes, int limit);
    PartyRef getParty(ErpPartyType partyType, Long partyId);
    Map<String, Long> findReferences(ErpPartyType partyType, Long partyId);
}
```

- `findParties`：关键字跨实体检索；`partyTypes=null` 查所有 3 类；`limit` 默认经配置项控制（`erp-md.party-search.max-results=50`）。
- `getParty`：单点查询；不存在抛 `ERP_MD_PARTY_NOT_FOUND`。
- `findReferences`：跨实体引用计数预览，**返回类型与既有 `IErpMdPartnerReferenceChecker.countReferences` 一致** `Map<String, Long>`（Path A 严格同构；非 rich DTO）。

### 4.2 非实体 BizModel 接口暴露约定（Phase 0 Decision 5）

本仓库有两种非实体 BizModel 范式：

| 范式 | 例子 | 接口？ | 跨域 `@Inject`？ | 说明 |
|------|------|-------|----------------|------|
| 非实体 + UI 入口 | `ErpMdDashboardBizModel` | **无** | 否 | 仅前端 page.yaml 调用 GraphQL，无 Java 跨工程消费者 |
| **非实体 + 跨域消费者** | **`ErpPartyBizModel`** | **有**（`IErpPartyBiz`） | **是** | purchase/sales/finance 可能 `@Inject IErpPartyBiz` |

**裁决（约定）**：当且仅当非实体 BizModel 有跨工程 `@Inject` 消费者时，才暴露 `IErp*Biz` 接口；纯 UI 入口（如 Dashboard）保持无接口。理由：接口是契约层，无消费者时不引入契约负担。

### 4.3 SPI 端口扩展（Explore (d) 结论 + Decision 4）

既有 SPI：`IErpMdPartnerReferenceChecker`（master-data 声明端口，purchase/sales/inventory/hr 等下游域注册实现，`@Inject(required=false)` 单实例 nullable）。

**3 方案权衡**：

| 方案 | 单一职责 | 工作量 | 既有契约冲击 | 结论 |
|------|---------|--------|------------|------|
| (a) 新增 2 独立 SPI：`IErpMdEmployeeReferenceChecker` + `IErpMdOrganizationReferenceChecker` | **清晰** | 模式化重复 | 无（既有 Partner SPI 不变） | **选定** |
| (b) 抽象统一 `IErpPartyReferenceChecker`（按 partyType dispatch） | 模糊 | 需改既有 Partner SPI | 高（破坏既有契约） | 拒绝 |
| (c) 复用 `IErpMdPartnerReferenceChecker` 语义扩展 | 模糊 | 低 | 高（破坏单一职责） | 拒绝 |

**选定方案 (a) Path A 严格同构**：

- 2 新 SPI 端口签名：`Map<String, Long> countReferences(Long partyId)` —— **无 IServiceContext 参数，返回 `Map<String, Long>`（key=引用域名 + value=引用计数），无 listReferences 方法**，与既有 Partner SPI 完全一致；
- `ErpPartyBizModel` 注入方式：`@Inject(required=false) IErpMdEmployeeReferenceChecker employeeReferenceChecker;`（单实例 nullable，非 `List<>`）；
- 下游域实现注册归 Deferred successor（仅试点 Employee/Organization 引用扫描机制；具体下游域实现按业务需求驱动落地）。

## 5. 联合 picker 范式（首例手写 picker.page.yaml）

### 5.1 模式说明

| 触发条件 | 实现路径 |
|---------|---------|
| BizModel 非实体 + 需要 AMIS picker 交互 | **手写 `main.picker.page.yaml`**（与 `dashboard/main.page.yaml` 既有手写非实体 page.yaml 同构） |

**为什么手写**：codegen `picker.page.yaml` 模板（`<web:GenPage view="XxxView.view.xml" page="picker"/>`）要求绑定实体 view.xml；Party 非实体无对应 view.xml，必须手写。

### 5.2 结构

`module-master-data/erp-md-web/src/main/resources/_vfs/erp/md/pages/party-search/main.picker.page.yaml`：

- AMIS `crud` + `picker` mode；
- asideFilter / filterForm：关键字 input-text + partyType 多选 tag（PARTNER/EMPLOYEE/ORGANIZATION，默认全选）；
- pick-list grid：`partyType` / `code` / `name` / `phone` / `email` / `status` 6 列（Organization 行 `phone`/`email` 显示空）；
- pick-query GraphQL：`@query:ErpParty__findParties(keyword=$keyword, partyTypes=$partyTypes, limit=50)`；
- onSelect 回填：onEvent.setValue 触发父表单字段填充（partyType + partyId + displayName + 可选 phone/email）。

### 5.3 注册路径

picker 不需独立菜单项，经父页面引用 `/erp/md/pages/party-search/main.picker.page.yaml` 即可。

## 6. 试点场景实施记录

### 6.1 F7 findReferences 扩展实施细节

试点选型 = 候选 1（F7 `countReferences` 扩展为 `IErpPartyBiz.findReferences`）。Phase 2 落地：

| 组件 | 路径 | 状态 |
|------|------|------|
| `IErpPartyBiz` 接口（含 `findReferences` 签名 `Map<String, Long>`） | `module-master-data/erp-md-dao/src/main/java/app/erp/md/biz/IErpPartyBiz.java` | **NEW** |
| `ErpPartyBizModel.findReferences` 实现（按 `partyType` dispatch） | `module-master-data/erp-md-service/src/main/java/app/erp/md/service/party/ErpPartyBizModel.java` | **NEW** |
| `IErpMdEmployeeReferenceChecker` SPI 端口（与既有 Partner SPI 严格同构） | `module-master-data/erp-md-dao/src/main/java/app/erp/md/spi/IErpMdEmployeeReferenceChecker.java` | **NEW** |
| `IErpMdOrganizationReferenceChecker` SPI 端口 | `module-master-data/erp-md-dao/src/main/java/app/erp/md/spi/IErpMdOrganizationReferenceChecker.java` | **NEW** |
| 既有 `IErpMdPartnerReferenceChecker` 复用 | （不变） | 既有 |
| F7 兼容路径 `ErpMdPartnerBizModel.countReferences` | （不变，view.xml 仍调 `ErpMdPartner__countReferences`） | 既有 |

### 6.2 数据流图

```
跨域消费者（如 purchase 想做"删除 Partner 前引用预览"）
    │
    │ @Inject IErpPartyBiz  （跨工程经 erp-md-dao 模块）
    ▼
IErpPartyBiz.findReferences(ErpPartyType.PARTNER, partnerId)
    │
    ▼
ErpPartyBizModel（service 模块，非实体 BizModel）
    │
    ▼
按 partyType dispatch：
    ├─ PARTNER     → partnerReferenceChecker.countReferences(partyId)
    │              （SPI 端口 = master-data 声明，下游域 purchase/sales/inventory 实现 + 注册）
    │              ↓
    │              下游域实现（如 ErpMdPartnerReferenceCheckerImpl）
    │              → 各自查询业务单据（PurOrder.partnerId / SalOrder.partnerId / ...）
    │              → 返回 Map<引用域名, 计数>
    │
    ├─ EMPLOYEE    → employeeReferenceChecker.countReferences(employeeId)
    │              （SPI 端口 = 本计划 NEW，下游域实现归 Deferred）
    │              ↓ 无实现时返回空 Map（@Nullable 注入 + null 检查）
    │
    └─ ORGANIZATION → organizationReferenceChecker.countReferences(organizationId)
                   （SPI 端口 = 本计划 NEW，下游域实现归 Deferred）
                   ↓ 无实现时返回空 Map
```

### 6.3 跨实体引用扫描清单

| Party 类型 | SPI 端口 | 下游域实现状态 | 备注 |
|-----------|---------|--------------|------|
| PARTNER | `IErpMdPartnerReferenceChecker`（既有，F7 plan 2026-07-20-1020-2） | 既有 + Phase 2 复用 | F7 已落地，本计划复用 |
| EMPLOYEE | `IErpMdEmployeeReferenceChecker`（NEW） | **Deferred** | 触发：删除 Employee 前引用预览运维场景 / A2/A3 启动 |
| ORGANIZATION | `IErpMdOrganizationReferenceChecker`（NEW） | **Deferred** | 触发：同上 + 注意 orgId 不纳入扫描（§Phase 2 Decision） |

### 6.4 性能数据（基线）

| 查询类型 | 实现方式 | 测试集数据量 | 单次响应时间 |
|---------|---------|------------|------------|
| `findParties`（关键字跨 3 实体） | IOrmTemplate 3 查询 + Java merge | 测试集（每实体 1-10 行） | < 30ms（含 ORM session + 投影） |
| `getParty`（单点） | `daoProvider.daoFor().getEntityById()` | 测试集 | < 5ms |
| `findReferences`（Partner 路径） | 单 SPI 调用 + HashMap | 测试集 | < 5ms |

生产基线触发条件：见 §7.2 物化视图 successor。

### 6.5 测试基线

| 测试类型 | 测试文件 | 场景数 | 状态 |
|---------|---------|-------|------|
| 单元测试 | `module-master-data/erp-md-service/src/test/java/app/erp/md/service/party/TestErpPartyBiz.java` | 8（含空数据集） | 全绿 |
| Visual smoke | `tests/e2e/visual/party-search-picker.visual.spec.ts` | 3（findParties + getParty + findReferences wiring） | 全绿（需运行时 + app-erp-all server） |
| md-service 全量回归 | `mvn test -pl module-master-data/erp-md-service` | 60 | 全绿 |
| 工作空间全量构建 | `mvn clean install -DskipTests` | 154 模块 | BUILD SUCCESS |

## 7. 性能与扩展

### 7.1 EstRows 基线

| 实体 | EstRows | 关键字查询 P95（基线目标） |
|------|---------|--------------------------|
| `ErpMdPartner` | < 10,000 | < 50ms |
| `ErpMdEmployee` | < 1,000 | < 10ms |
| `ErpMdOrganization` | < 100 | < 5ms |
| **合计** | < 11,100 | < 65ms（3 查询顺序） |

### 7.2 物化视图 successor 触发条件

- Partner EstRows > 100,000（业务增长 ~10×）；
- 关键字查询 P95 > 500ms；
- 任一触发 → 升级方案 (b) `@SqlLibMapper` + union SQL；
- (b) 仍不满足 → 升级方案 (c) 物化视图 / 反向索引表（独立 successor plan）。

## 8. 反模式自检表

| 反模式 | 应该这样写 |
|--------|----------|
| 在 master-data BizModel 中 `@Inject IPurOrderBiz` 反向依赖业务域做引用扫描 | **声明 SPI 端口（`IErpMdEmployeeReferenceChecker`）+ 下游域注册实现** —— master-data 是基础域，禁止反向依赖业务域 |
| 非实体 BizModel 无跨域消费者也暴露 `IErp*Biz` 接口 | 纯 UI 入口（如 Dashboard）保持无接口（§4.2 约定） |
| 用 `List<IErpMd*ReferenceChecker>` 注入多个下游域引用计数器 | **`@Inject(required=false)` 单实例 nullable**（与既有 `IErpMdPartnerReferenceChecker` 同构 Path A） |
| `IErpPartyBiz.findReferences` 返回 rich DTO `List<PartyReference>` | **返回 `Map<String, Long>`**（与既有 SPI 一致；rich DTO 是 contract upgrade，归 Path B successor） |
| 在 union SQL 里硬编码 NULL 投影处理 Organization 无 phone/email | **Java 内 merge** —— 各实体 findPage 后逐行投影到 `PartyRef`，缺失字段默认 null |
| keyword < 2 字符仍执行 LIKE 全表扫描 | **keyword < 2 返回空 List**（避免全表扫描） |
| 在 master-data 直接 `import` 业务域实体做引用扫描 | **改 SPI 端口** —— `IErpMdEmployeeReferenceChecker` / `IErpMdOrganizationReferenceChecker` |
| 手写 picker.page.yaml 复制 codegen 模板的 `<web:GenPage>` 结构 | **手写 AMIS `crud + picker` + GraphQL `@query`** —— 与 `dashboard/main.page.yaml` 既有手写非实体 page.yaml 同构 |
