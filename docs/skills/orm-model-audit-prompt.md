# ORM 模型审计提示


> **项目定制化层（nop-app-erp）**：使用本提示前必须先读 `docs/skills/README.md §项目定制化层（nop-app-erp）`，将本仓库的保护区域（`module-<domain>/model/*.orm.xml` ask-first、会计/财务/数据删除）、验证命令（`mvn clean install -DskipTests`）、命名约定（`Erp<Domain>` 实体前缀、`erp-<short>/<dict>` 字典、`erp.err.<short>` ErrorCode 前缀）和已知失败模式注入上下文。本提示的通用默认值在本仓库不充分。


在将 `<domain>/model/*.orm.xml` 作为持久化模型真相源进行规范与完整性审计时使用此提示。

在 ORM 模型首次建立后、跨模块引用变更后、字段批量补齐后或 codegen 前最终核对时使用。不要将其用作需求综合、设计文档审计或计划审计的替代品。

```text
您是高级数据建模师和 Nop Platform 专家。以下是项目所有 `<domain>/model/*.orm.xml` 文件。按维度审计其规范合规性与业务字段完整性，对照 `docs/design/domain-design-guidelines.md` 的"单据标准字段约定"与 `docs/design/erp-design-audit-checklist.md`。

首先阅读这些文件：
- `AGENTS.md`
- `docs/design/domain-design-guidelines.md`（"单据标准字段约定" + "状态机命名与跨域映射规范"章节）
- `docs/design/erp-design-audit-checklist.md`
- `../nop-entropy/docs-for-ai/02-core-guides/orm-model-design.md`（平台 ORM 规范）
- `../nop-entropy/schema/entity.xdef`（属性权威定义）
- 平台参考实现：`../nop-entropy/nop-auth/model/nop-auth.orm.xml`、`~/app/nop-app-mall-wt/.../model/app-mall.orm.xml`
- 所有 `<domain>/model/*.orm.xml`

审计维度：

1. 类型规范
   - 每列显式 `code`（UPPER_SNAKE_CASE）+ `propId`（从 1 连续）+ `stdSqlType`（仅 StdSqlType 枚举值）+ `stdDataType`。
   - StdSqlType 与 stdDataType 配套：BOOLEAN↔boolean、INTEGER↔int、BIGINT↔long、DECIMAL↔string（nop 用 BigDecimal 序列化为 string）、VARCHAR↔string、DATE↔date、TIMESTAMP↔timestamp。
   - 残留废弃用法：`stdSqlType="INT"`（应 INTEGER）、`dictName=`（应 ext:dict=）。

2. 长度与精度
   - VARCHAR 字段带 `precision`；金额类 DECIMAL 用 `precision="20" scale="4"`（本位币）/`scale="8"`（汇率）。
   - 主键 BIGINT 不带 precision。
   - 文本类长字段用 CLOB 而非超长 VARCHAR。

3. 字典设计
   - `<dict name="域简称/kebab-name">` 格式（如 `erp-pur/doc-status`）。
   - option `code` UPPER_SNAKE、`value` 10/20/30 递增、`label` 中文。
   - 跨域不重复定义同名字典；ACTIVE/INACTIVE 类通用状态应复用 `erp-md/active-status`。
   - 每个字段引用的 `ext:dict=` 都有对应 `<dict>` 定义。

4. 标准字段完整性（每个实体）
   - 必有：`id`（BIGINT primary + stdDataType=long + tagSet="seq-default"）、`delVersion`、`version`、`createdBy`、`createTime`、`updatedBy`、`updateTime`、`remark`。
   - 实体配 `useLogicalDelete="true" deleteFlagProp="delVersion"`。
   - 审计四件套用 domain 复用，不重复定义列。

5. 业务字段完整性（对照 §10 单据标准字段约定）
   - 所有业务单据头：`orgId`（业务组织）、`businessDate`（业务日期）、`posted`/`postedAt`/`postedBy`（业财过账三件套）。
   - 所有金额类单据头/行：`currencyId` + `exchangeRate` + `amountSource` + `amountFunctional`（多币种四件套）。
   - 单据头：`docStatus` + `approveStatus`（双轴分离）。
   - 价税分离：`taxRateId` + `taxAmount` + `amount` + `amountWithTax`。

6. 关系设计
   - 本模块关系用 `<to-one>` + `<join><on leftProp="..." rightProp="id"/></join>` + `tagSet="pub"`。
   - 跨模块引用：要么机制 B（`<entity notGenCode="true">` 外部实体引用 + to-one），要么机制 D（纯外键列 + I*Biz）。不允许"无声明的跨模块 refEntityName"。
   - 头-行关系用 `tagSet="pub,cascade-delete,insertable,updatable"`。

7. 跨模块引用一致性（机制 B 落地）
   - 每个 `<to-one refEntityName="app.erp.Xxx.dao.entity.Yyy">` 的 Yyy 在本模块 orm.xml 有对应 `<entity notGenCode="true">` 声明（防 codegen 找不到 refEntityName）。
   - 外部实体声明的列只列本模块会用到的关键列（不全量复制）。
   - DAG 合规：跨模块引用单向，无循环（见 cross-module-dependency-audit-prompt）。

8. 命名一致性
   - 实体 className 与 orm 文件包名一致（`app.erp.<域简称>.dao.entity.Erp<域大写><Entity>`）。
   - tableName 与域前缀一致（`erp_<域简称>_<snake_entity>`）。
   - 列 name camelCase、code UPPER_SNAKE_CASE。

9. 需求覆盖（对照 product-scope）
   - 每个核心业务能力都有对应实体（用关键词→表名模糊匹配核对）。
   - 实体数量与 product-scope 声明一致。

严重性指南：
- `blocker`：类型非法、关系断裂（无声明）、标准字段缺失影响业财一体、DAG 循环。
- `major`：业务字段缺失（如金额类单据缺多币种四件套）、字典规范违规、跨域命名冲突。
- `minor`：冗余声明、列顺序优化、注释缺失。

自动化核查建议：使用 Python 脚本扫描所有 orm.xml，按维度统计问题清单（参考 `docs/logs/2026/06-22.md` 2026-06-23 条目的核查脚本结构）。手动抽样核实脚本的"误报"与"漏报"。

按严重性返回发现，附：受影响文件与行、问题、修复建议（具体字段补齐 / 类型修正 / 关系补声明）。最后给：
- 裁决：通过/失败
- 各维度通过率
- 修复后的字段补齐统计（如"补 N 个多币种四件套、M 个 posted 三件套"）
- 残留风险
```
