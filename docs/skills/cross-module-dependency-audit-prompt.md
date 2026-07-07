# 跨模块数据依赖审计提示


> **项目定制化层（nop-app-erp）**：使用本提示前必须先读 `docs/skills/README.md §项目定制化层（nop-app-erp）`，将本仓库的保护区域（`module-<domain>/model/*.orm.xml` ask-first、会计/财务/数据删除）、验证命令（`mvn clean install -DskipTests`）、命名约定（`Erp<Domain>` 实体前缀、`erp-<short>/<dict>` 字典、`erp.err.<short>` ErrorCode 前缀）和已知失败模式注入上下文。本提示的通用默认值在本仓库不充分。


在审计多模块应用的跨工程数据依赖合理性、DAG 合规性、外部实体引用一致性时使用此提示。

在跨模块 ORM 关联建立后（机制 B notGenCode 落地）、模块拆分调整后、或 codegen 前最终核对时使用。不要将其用作单模块内部审计、需求综合或计划审计的替代品。

```text
您是高级架构师。以下是项目所有业务域的 `<domain>/model/*.orm.xml` 与 `docs/architecture/data-dependency-matrix.md`。审计跨模块数据依赖的合理性、DAG 合规性、外部实体引用完整性。

首先阅读这些文件：
- `AGENTS.md`
- `docs/architecture/module-boundaries.md`（模块依赖方向 + 跨工程实体关系规则）
- `docs/architecture/data-dependency-matrix.md`（R/S/P 三类依赖矩阵 + §5.6 外部实体引用清单）
- `../nop-entropy/docs-for-ai/02-core-guides/cross-module-entity-reference.md`（四种机制 A/B/C/D）
- `../nop-entropy/schema/entity.xdef`（@notGenCode 权威定义）
- 所有 `<domain>/model/*.orm.xml`

审计维度：

1. DAG 合规性
   - 跨模块 refEntityName 引用方向单向，无循环。
   - 允许方向：业务域 → master-data（单向）；finance → projects/assets（finance 是 DAG 顶）。
   - 禁止方向：业务域之间的反向引用（如 projects → finance、inventory → purchase）。
   - 用脚本构建依赖图，拓扑排序检测循环。

2. 外部实体声明完整性（机制 B）
   - 每个生效的跨模块 `<to-one refEntityName="app.erp.X.dao.entity.Y">` 都有对应 `<entity notGenCode="true">` 声明。
   - 声明的列只列关键列，不全量复制（运行时由被引用模块的 Entity 类提供完整列）。
   - 外部实体声明的 `name`（实体全限定名）与 `tableName` 与被引用模块一致。

3. 跨模块引用范式选择合理性
   - 高频多维关联查询（如凭证行按 subject/partner/project/warehouse/material 筛选）→ 应用机制 B。
   - 列表显示名 → 用冗余显示名字段（L1）。
   - 详情展开 → 用 @BizLoader + requireBiz（L3）。
   - 报表复杂查询 → 用 EQL 子查询（IN）。
   - 业务表之间反查源单（凭证反查业务单）→ 用弱指针字符串三元组（机制 P），不建 to-one。

4. 业财一体边界一致性
   - 凭证反查源单统一用 `(billType, billHeadCode, lineCode)` 字符串三元组，不写 FK 到业务表。
   - 业务表不感知凭证存在（单向：业务→财务）。
   - finance 对业务域是纯读（I*Biz 只读查源单），不回写业务表。

5. 冗余字段策略
   - 高频列表显示场景应冗余显示名字段（如 supplierName、materialName），与 to-one 并存。
   - 冗余字段需有维护机制（主数据改名时刷新，或 @BizLoader 实时带出）。

6. Maven 依赖与 orm 声明对齐
   - 引用方工程的 `erp-xxx-dao/pom.xml` 应依赖被引用方的 `-dao` 包（codegen 后核对）。
   - 本模块的 orm.xml 不重复生成外部模块的 Entity 类（靠 notGenCode="true" 跳过）。

7. 与 data-dependency-matrix.md 一致性
   - 矩阵中声明的依赖方向与 orm.xml 实际 refEntityName 一致。
   - 矩阵的 R/S/P 分类与实际引用方式一致（R=只读外键、S=同事务写、P=弱指针反查）。

自动化核查脚本建议：
- 扫描所有 orm.xml 的 refEntityName，按 `app.erp.(md|inv|pur|sal|fin|ast|prj|mfg|qa|mnt).dao.entity` 提取跨模块引用边。
- 对照 ALLOWED 字典验证方向合法性。
- 拓扑排序检测循环。
- 统计每域"生效 to-one 数 / 注释残留数 / 外部实体声明数"，验证"引用 ≤ 声明"完整覆盖。

严重性指南：
- `blocker`：DAG 循环、refEntityName 无对应声明（codegen 必失败）、跨模块写反向（finance 回写业务）。
- `major`：高频关联查询场景未用机制 B（性能差）、缺关键外部实体声明、弱指针与 to-one 范式混用。
- `minor`：冗余声明、命名小问题。

按严重性返回发现，附：引用边清单、DAG 验证结果（✅/❌）、外部实体声明完整性矩阵。最后给：
- 裁决：通过/失败
- 跨模块引用边总数、DAG 合规边数、循环数
- 各域外部实体声明完整覆盖率
- 残留风险（如 Maven pom 未对齐——codegen 阶段处理）
```
