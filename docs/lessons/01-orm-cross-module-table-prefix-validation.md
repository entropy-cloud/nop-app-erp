# Lesson 01: 跨模块外部实体引用表名前缀验证

> **来源**：2026-07-07 多维审计补充整改（plan 2026-07-07-2200-1）
> **适用场景**：ORM 模型设计、跨模块外部实体引用（`notGenCode="true"`）
> **失败模式**：表名前缀双重拼接导致运行时 SQL 表名错误

## 问题

当业务域 A 通过 `notGenCode="true"` 外部实体引用业务域 B 的实体时，如果引用方的 ORM 模型中 `tableName` 已经包含了表前缀（如 `erp_md_material`），而平台的 codegen 又自动拼接引用方的 appName 前缀（如 `erp-ast`），会导致最终表名变成 `erp_ast_erp_md_material`（双重前缀）。

## 根因

1. 外部实体引用的 `refEntityName` 指向另一个模块的实体类名
2. `tableName` 属性的值应该是**完整的物理表名**（含前缀），而非逻辑名
3. codegen 不会为 `notGenCode="true"` 的外部实体重新拼接表名——它直接使用 `tableName` 的值

## 正确做法

- 外部实体引用的 `tableName` 必须与被引用域 orm.xml 中声明的表名**完全一致**
- 验证方法：`grep -r "tableName=" module-A/model/*.orm.xml` 中外部实体的表名，与 `module-B/model/*.orm.xml` 中的原始声明对比
- 外部实体的 `tableName` 不应包含引用方的域前缀

## 反例

```xml
<!-- 错误：引用 master-data 的 Material，但表名加了 assets 前缀 -->
<entity name="app.erp.md.dao.entity.ErpMdMaterial" tableName="erp_ast_erp_md_material"
        notGenCode="true" refEntityName="..."/>
```

## 正例

```xml
<!-- 正确：表名与 master-data 域中声明一致 -->
<entity name="app.erp.md.dao.entity.ErpMdMaterial" tableName="erp_md_material"
        notGenCode="true" refEntityName="..."/>
```

## 检查清单

- [ ] 每个 `notGenCode="true"` 外部实体的 `tableName` 与源域 orm.xml 完全匹配
- [ ] 外部实体的 `tableName` 不含引用方域前缀
- [ ] 跨模块引用变更后运行 `mvn clean install -DskipTests` 确认编译通过
- [ ] `xmllint --noout` 验证 XML well-formed
