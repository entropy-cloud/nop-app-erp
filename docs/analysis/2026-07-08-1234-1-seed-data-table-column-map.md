# 演示种子数据初始化（init-data）— 表清单、列映射与加载拓扑序

> Owner: `docs/plans/2026-07-08-1234-1-demo-seed-data-init.md` Phase 1 Exit Criterion 2
> 权威源: `module-master-data/model/app-erp-master-data.orm.xml`（逐表逐列核实）

## 1. DataInitInitializer 门控机制 + 非幂等结论（Phase 1 Exit Criterion 1）

**门控机制（经源码 + beans.xml + 经验性启动核实）**：
- `orm-defaults.beans.xml`（平台 `_dump`）注册 `DataInitInitializer` 为条件 bean：
  `<ioc:condition><if-property name="nop.orm.init-database-data"/></ioc:condition>` + `ioc:after="nopOrmSessionFactory,DataBaseSchemaInitializer"`。
- `nop.orm.init-database-data`（Boolean，默认 `false`）→ `false`/缺省时 bean **不实例化**（条件不满足）；`=true` 时每次启动 `@PostConstruct init()` 均执行。
- `init()` 经 `ormTemplate.runInSession()` → 按 ORM 拓扑序遍历实体 → 检查 `{location}/{tableName}.csv` → `dao.newEntity()` + 填列(`code` 映射) + `dao.saveEntity()`。

**非幂等结论（经验性确认，2026-07-08）**：
- 平台源码 `DataInitInitializer.loadCsvData()`(:83-107) 逐行 `dao.saveEntity()`，**无存在性检查、无 upsert、无 truncate、无幂等守卫**。
- 经验性重跑：fresh DB（删 `./db/erp.mv.db`）首次启动 → `erp_md_currency.csv` 2 行插入成功，GraphQL `ErpMdCurrency__findList` 返回 2 行（CNY/USD）。
- 不删 DB 再次启动（持久 H2 已有数据，`init-database-data=true` → 条件 bean 实例化 → `init()` 重跑）→ `org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException: Unique index or primary key violation: PK_ERP_MD_CURRENCY(ID)` for `CAST(1 AS BIGINT)` → 应用启动失败。
- **结论**：持久 H2 文件库（`./db/erp`）上重复启动若表已有 seed 行 → 主键冲突；fresh-DB 重置（删 `./db/erp.mv.db`）是 seed 启动的必需前置步骤。archived 平台计划 `129-orm-auto-init-database-data.md` Deferred 裁定幂等性为「optimization candidate / 刻意 out-of-scope」。

**框架自动填充字段（经 PK 冲突错误行核实，CSV 无须提供）**：
- `CREATED_BY`=`'sys'`、`CREATE_TIME`=`<启动时间戳>`、`UPDATED_BY`=`'sys'`、`UPDATE_TIME`=`<启动时间戳>`、`DEL_VERSION`=`0`、`VERSION`=`0`。
- 即所有实体公共审计/版本字段由 ORM 拦截器自动填充，CSV 仅提供业务列 + 显式 `ID`（跨表 FK 引用需要固定可读 ID）。

**平台 bug 修复（本计划执行中发现并修复，记录于 `nop-entropy/ai-dev/logs/`）**：
- `DataInitInitializer.init()` 原 `@PostConstruct` 直接调用 `ormTemplate.runInSession()`，但在 Quarkus 应用启动期，IoC 循环依赖（工厂 bean `OrmSessionFactoryBean` 的 interceptors 链 → `nopOrmTemplate`）导致 `ormTemplate.sessionFactory` 在此时刻为 `null` → `OrmSessionRegistry.instance().get(null)` → NPE（`ConcurrentHashMap.get(null)`）。
- archived plan 129 仅经单元测试验证（`TestDataInitInitializer` 手工 `new OrmTemplateImpl(sessionFactory)` 构造 + 预建 `IContext`），未覆盖真实 IoC 容器启动路径，故未暴露。
- **修复**：`DataInitInitializer` 新增 `ensureOrmTemplateSessionFactory()`，在 `init()` 入口检测 `ormTemplate.sessionFactory` 是否为 null，若 null 则用自身已注入的 `ormSessionFactory` 补齐（`nop-entropy/nop-persistence/nop-orm/src/main/java/io/nop/orm/initialize/DataInitInitializer.java`）。

## 2. 配置门控 Decision（Phase 1 Exit Criterion 3）

**选择：候选 A**（`-Dnop.orm.init-database-data=true` JVM 属性，镜像 0637-1 认证 `allow-create-default-user=true` 范式）。

**理由**：
- 生产 `application.yaml` **不改默认**（保持 `init-database-data` 缺省 false）。seed 经 JVM 属性 / `playwright.config.ts` webServer 命令行触发，生产安全。
- 与既有 E2E webServer 命令（`-Dnop.auth.service-public=true -Dnop.auth.login.allow-create-default-user=true`）范式一致，无新 profile 引入。

**替代方案分析**：
- 候选 B（`%demo` Quarkus profile + `init-database-data: true`）：需改 `application.yaml` 加 profile 段，且 profile 激活需额外 `-Dquarkus.profile=demo`。比候选 A 多一处配置面变更，无明显收益。**rejected**。
- 候选 C（生产默认 `true`）：**rejected**——非幂等，持久 H2 重复启动会主键冲突，生产不可默认开。

**残留风险**：
- 生产误开 seed（运维误传 `-Dnop.orm.init-database-data=true`）→ 首次启动插入 seed，二次启动主键冲突 → 应用启动失败（fail-fast，非静默数据污染，可接受）。
- E2E/演示启动前须删 `./db/erp.mv.db`（fresh-DB 重置）；webServer 生命周期可靠性经 Phase 1 + Phase 3 双重经验性确认。

## 3. seed 表清单 + 列映射 + 加载拓扑序（Phase 1 Exit Criterion 2）

CSV 列名 = 实体 column `code`（UPPER_SNAKE_CASE）。`ID` 列虽 `tagSet="seq-default"`（自动序列），但跨表 FK 引用需要固定 ID，故 CSV 显式提供 `ID`。框架字段（`DEL_VERSION`/`VERSION`/`CREATED_BY`/`CREATE_TIME`/`UPDATED_BY`/`UPDATE_TIME`）自动填充，CSV 不含。

### 加载拓扑序（DataInitInitializer 按 ORM `getEntityModelsInTopoOrder()` 自动排序，以下为 seed 设计依赖序）

| 序 | 表名 | 依赖（FK 上游） | seed 行数 | mandatory 业务列（code） | FK 列（引用上游 ID） |
|----|------|----------------|----------|------------------------|---------------------|
| 1 | erp_md_currency | — | 2 | CODE,NAME | — |
| 2 | erp_md_uom | — | 4 | CODE,NAME | — |
| 3 | erp_md_organization | currency | 2 | CODE,NAME,ORG_TYPE,STATUS | FUNCTIONAL_CURRENCY_ID→currency |
| 4 | erp_md_material_category | (self) | 3 | CODE,NAME | PARENT_ID→self |
| 5 | erp_md_tax_rate | — | 3 | CODE,NAME,TAX_TYPE,RATE,STATUS | — |
| 6 | erp_md_partner | — | 4 | CODE,NAME,PARTNER_TYPE,STATUS | — |
| 7 | erp_md_employee | organization,(partner) | 3 | CODE,NAME,STATUS | ORG_ID→organization |
| 8 | erp_md_warehouse | organization,employee | 2 | CODE,NAME,STATUS | ORG_ID→organization, MANAGER_ID→employee |
| 9 | erp_md_location | warehouse,(self) | 2 | WAREHOUSE_ID,CODE,NAME | WAREHOUSE_ID→warehouse |
| 10 | erp_md_material | category,uom,warehouse,tax_rate | 4 | CODE,NAME,MATERIAL_TYPE,UOM_ID,STATUS | CATEGORY_ID, UOM_ID, DEFAULT_WAREHOUSE_ID, DEFAULT_TAX_RATE_ID |
| 11 | erp_md_material_sku | material,uom,tax_rate | 4 | MATERIAL_ID,SKU_CODE,UOM_ID | MATERIAL_ID, UOM_ID, TAX_RATE_ID |
| 12 | erp_md_uom_conversion | uom,(material) | 2 | FROM_UOM_ID,TO_UOM_ID,CONVERSION_RATE | FROM_UOM_ID, TO_UOM_ID |
| 13 | erp_md_partner_address | partner | 4 | PARTNER_ID,ADDRESS_TYPE,ADDRESS | PARTNER_ID |
| 14 | erp_md_partner_contact | partner | 3 | PARTNER_ID,CONTACT_PERSON | PARTNER_ID |
| 15 | erp_md_bank_account | partner | 2 | PARTNER_ID,BANK_NAME,BANK_ACCOUNT | PARTNER_ID |
| 16 | erp_md_subject | (self),currency | 8 | CODE,NAME,SUBJECT_CLASS,DIRECTION,STATUS | PARENT_ID→self, CURRENCY_ID |
| 17 | erp_md_acct_schema | organization,currency | 1 | CODE,NAME,ORG_ID,NATURE,FUNCTIONAL_CURRENCY_ID,STATUS | ORG_ID, FUNCTIONAL_CURRENCY_ID |
| 18 | erp_md_acct_schema_coa | acct_schema | 1 | ACCT_SCHEMA_ID,NAME | ACCT_SCHEMA_ID |
| 19 | erp_md_settlement_method | — | 3 | CODE,NAME,SETTLEMENT_TYPE,STATUS | — |
| 20 | erp_md_cost_center | organization,employee,(self) | 2 | CODE,NAME,ORG_ID,STATUS | ORG_ID, MANAGER_ID, PARENT_ID |
| 21 | erp_md_exchange_rate | currency | 2 | FROM_CURRENCY_ID,TO_CURRENCY_ID,RATE,VALID_FROM | FROM_CURRENCY_ID, TO_CURRENCY_ID |

**seed 记录集范围（Phase 1 Decision item 4）**：21 表，共约 57 行连贯主数据（单组织/本位币最小集，覆盖看板/报表/CRUD 列表最小可见前提）。业务交易单据 Non-Goal。

**参照完整性防护**：
- 所有 FK 列引用上游已 seed 行的固定 ID（见每表 CSV 的 ID 列）。
- DataInitInitializer 按拓扑序插入，上游表先于下游表，FK 引用启动时已存在。
- 列名经 `code` 映射核实；不匹配抛 `NopException`（不静默跳过），启动期即暴露错误。
