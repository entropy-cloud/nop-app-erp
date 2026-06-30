# 打印模板

## 目的

定义 nop-app-erp 的单据打印模板机制，支持按单据类型配置打印格式。

## 模板架构

```
ErpSysDocumentTemplate（打印模板配置）
    ├─ templateId
    ├─ billType（单据类型：PO/SI/PAYMENT 等）
    ├─ templateName
    ├─ templateType（LIST/DETAIL/FORM）
    ├─ reportEngine（nop-report）
    └─ templateContent（XML 模板定义）
```

## 模板类型

| 类型 | 用途 | 示例 |
|------|------|------|
| LIST | 列表打印 | 采购订单列表、库存台账 |
| DETAIL | 单据明细打印 | 采购订单打印、发票打印 |
| FORM | 表单打印 | 标签、条码 |

## 实现

- 使用 Nop Platform `nop-report` 组件
- 模板在数据库维护（`ErpSysDocumentTemplate`）
- 按租户+账套配置
- 支持模板版本管理

## 打印触发

- 手动打印：用户在 UI 点击打印按钮
- 自动打印：审核通过后自动打印（可配置）
- 批量打印：选择多条单据批量打印
