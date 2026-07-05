# 01 跨 7 模块 organization 表名拼写错误传播（erp_md_md_organization）

## 问题

- 7 个域源模型在引用 master-data 的 `ErpMdOrganization` 外部实体时，`tableName` 写成 `erp_md_md_organization`（双 `md_` 拼写错误），而 master-data 源模型（`module-master-data/.../ErpMdOrganization`）权威表名为 `erp_md_organization`。
- 运行时任何按 organization 外部实体建表/查询的路径会引用不存在的表 `erp_md_md_organization`，导致 SQL 错误或数据隔离失效。
- 影响范围：aps/b2b/contract/cs/drp/hr/logistics 7 域；代码生成将该错误表名同步传播到各域生成的 `_app.orm.xml`。
- 严重性：🔴 P0（运行时错误 + 财务/组织隔离风险）。

## 复现

- 前提：完成 `nop-cli gen` / `mvn clean install` 后的代码生成产物。
- 触发：在任一受影响域启动应用，执行任何触达 organization 关联的 GraphQL 查询（例如带 `org{...}` 选择字段的 `findPage`）。
- 最小观察：ORM 解析出的实体表名为 `erp_md_md_organization`，与建表脚本 `erp_md_organization` 不一致。

## 诊断方法

- 诊断难度：中等——错误本身是一处复制粘贴，但静默传播到 7 个模块的生成物，单模块审查难以察觉跨模块一致性。
- 调查路径：跨审查综合审计（`docs/audits/2026-07-05-1400-cross-review-synthesis.md`）通过跨模块比对发现 7 域声明与 master-data 权威表名不一致；主代理 grep + 精读实时仓库全部确认。
- 被拒绝的假设：曾怀疑仅是生成产物问题，但溯源确认根因在源模型（复制粘贴外部实体声明时多带了一层 `md_` 前缀）。
- 决定性证据：`grep -rn "erp_md_md_organization"` 在 7 个源模型及其 `_app.orm.xml` 命中；master-data 源 `ErpMdOrganization` 定义为 `tableName="erp_md_organization"`。

## 根本原因

- 复制粘贴传播：源模型编写外部实体声明（`refEntityName` + 本地 `tableName` 重声明）时，`tableName` 误写为 `erp_md_md_organization`，因模板复用在 7 域中重复出现。
- 代码生成器忠实继承源模型声明的 `tableName`，未与目标实体权威定义交叉校验，导致错误传播到所有生成物。

## 修复

- 设计意图：组织表名唯一权威来源是 master-data 的 `ErpMdOrganization` 实体定义；跨域引用应通过 `refEntityName` 继承表名，不应本地重声明 `tableName`。
- 已确认实时仓库源模型（`module-*/model/*.orm.xml`）与生成产物（`*_dao/.../orm/_app.orm.xml`）均为正确的 `erp_md_organization`——源模型已收敛为仅用 `refEntityName` 引用，生成器从 master-data 继承权威表名。
- 本计划执行时：删除了滞后的 gitignored `_dump/`（旧构建残留，仍含错误表名）；`mvn clean install -DskipTests` 全绿，重新生成的 `_app.orm.xml` 全部为 `erp_md_organization`。

## 测试

- `mvn clean install -DskipTests`（全量重生成 + 编译，146 reactor 模块全绿）。
- `mvn test -pl module-{drp,hr} -am`（drp/hr 含 organization 相关测试，验证关联路径无回归）。
- 手动验证：`grep -rn "erp_md_md_organization"` 在所有 `.orm.xml`/Java/资源文件（排除 `_tmp/` 日志与 `docs/` 描述性文本）= 0。
- 级别：集成（全量构建）+ 静态断言（grep 退出标准）。

## 受影响的工件

- `module-{aps,b2b,contract,cs,drp,hr,logistics}/model/app-erp-*.orm.xml` — 源模型 organization 外部实体引用（已收敛为 `refEntityName` 继承权威表名）。
- `module-{aps,b2b,contract,cs,drp,hr,logistics}/erp-*-dao/src/main/resources/_vfs/erp/*/orm/_app.orm.xml` — 生成产物，重生成后 `tableName="erp_md_organization"`。

## 未来重构注意事项

- 新增跨域引用 master-data 实体时，**只用 `refEntityName`**，不要本地重声明 `tableName`——避免再次引入拼写漂移。
- 代码生成器当前不校验本地重声明的 `tableName` 与目标实体定义是否一致；若未来增加生成期跨模块表名一致性校验，可在此类错误进入产物前拦截。
- 审查跨域外部实体声明时，应将所有域的 `tableName` 与 master-data 权威定义做集合比对，而非逐模块单独审查。

## 预防差距

- 缺少生成期"本地重声明 tableName 必须匹配 refEntityName 目标实体 tableName"的断言。
- 前序审计（首轮 + D1–D5）按单模块审查，未做跨模块 organization 表名集合一致性检查，导致传播到 7 域才被跨角度审查发现。
