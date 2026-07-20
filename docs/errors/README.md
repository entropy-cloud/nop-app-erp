# 错误码索引

> Plan `2026-07-20-2200-1` L-2 落地。本文件作为错误码管理的集中索引，不重复定义（真相源在各域 `Erp*Errors.java`）。

## 目的

集中索引 18 业务域 + 1 跨域通知派发子系统的 ErrorCode 定义位置、命名约定、注册表状态，避免：

- 新增错误码时跨域查找命名空间冲突
- 审计/重构时无法快速定位错误码源头
- 多域引用同一错误码语义时漂移

## 命名约定

> 权威源：`docs/design/domain-design-guidelines.md §7.1 命名空间`。本节为指针，不重复规则。

ErrorCode 遵循 Nop 平台惯例 `nop.err.<module>.<name>`，应用层以 `erp.err` 替换 `nop.err`：

```
erp.err.<domain-short>.<name>
```

`domain-short` 见 `docs/architecture/domain-module-split-analysis.md §2.0` 的 `appName` 列（如 `pur`/`sal`/`fin`/`mfg`/`ast`/`qa`/`mnt`/`inv`/`prj`/`md`/`crm`/`cs`/`hr`/`aps`/`log`/`b2b`/`ct`/`drp`/`notify`）。

## 各域 ErrorCode 真相源

| 域 | 文件 | 模块路径 | 备注 |
|----|------|----------|------|
| master-data | `ErpMdErrors.java` | `module-master-data/erp-md-service/.../md/service/` | 主数据：物料/伙伴/科目/仓库等 |
| inventory | `ErpInvErrors.java` | `module-inventory/erp-inv-service/.../inv/service/` | 库存：移动单/盘点/批次 |
| purchase | `ErpPurErrors.java` | `module-purchase/erp-pur-service/.../pur/service/` | 采购：订单/收货/发票/付款 |
| sales | `ErpSalErrors.java` | `module-sales/erp-sal-service/.../sal/service/` | 销售：订单/出库/发票/收款 |
| finance | `ErpFinErrors.java` | `module-finance/erp-fin-service/.../fin/service/` | 凭证/期间/预算/坏账 |
| finance-posting | `ErpFinPostingErrors.java` | `module-finance/erp-fin-service/.../fin/service/posting/` | 业财过账引擎专用 |
| assets | `ErpAstErrors.java` | `module-assets/erp-ast-service/.../ast/service/` | 资产：折旧/处置/CIP |
| manufacturing | `ErpMfgErrors.java` | `module-manufacturing/erp-mfg-service/.../mfg/service/` | 工单/BOM/工序 |
| projects | `ErpPrjErrors.java` | `module-projects/erp-prj-service/.../prj/service/` | 项目/任务/工时/成本 |
| quality | `ErpQaErrors.java` | `module-quality/erp-qa-service/.../qa/service/` | 质检/NCR/CAPA/SPC/召回 |
| maintenance | `ErpMntErrors.java` | `module-maintenance/erp-mnt-service/.../mnt/service/` | 设备/维护计划/访问 |
| notify | `ErpNotifyErrors.java` | `module-notify/erp-notify-service/.../notify/service/` | 跨域通知派发 |
| crm | `ErpCrmErrors.java` | `module-crm/erp-crm-service/.../crm/service/` | 线索/机会/客户/活动 |
| cs | `ErpCsErrors.java` | `module-cs/erp-cs-service/.../cs/service/` | 工单/SLA/CSAT/ entitlement |
| hr | `ErpHrErrors.java` | `module-hr/erp-hr-service/.../hr/service/` | 员工/合同/薪资/招聘 |
| aps | `ErpApsErrors.java` | `module-aps/erp-aps-service/.../aps/service/` | 排产/工序订单/ATP-CTP |
| logistics | `ErpLogErrors.java` | `module-logistics/erp-log-service/.../log/service/` | 发运/承运商/追踪 |
| b2b | `ErpB2bErrors.java` | `module-b2b/erp-b2b-service/.../b2b/service/` | EDI/ASN/Partner Profile |
| contract | `ErpCtErrors.java` | `module-contract/erp-ct-service/.../ct/service/` | 合同/版本/审批/返利 |
| drp | `ErpDrpErrors.java` | `module-drp/erp-drp-service/.../drp/service/` | 分销需求计划 |

## ErrorCode 实现模式

> 完整示例见 `../nop-entropy/docs-for-ai/05-examples/dto-and-errors.java`。

每个域的 `Erp*Errors` 是 `interface`，包含：

- `ARG_<NAME>` 常量（错误参数名）
- `ErrorCode.define("erp.err.<domain-short>.<name>", "中文描述", ARG_...)` 静态字段

业务代码使用：

```java
throw new NopException(ErpXxxErrors.ERR_YYY)
    .param(ErpXxxErrors.ARG_ZZZ, value);
```

## 注册表状态

> 当前**未引入全局 ErrorCode 注册表**（错误码生成 + i18n 资源文件自动同步等基础设施）。错误码定义直接在各域 `Erp*Errors.java` 中由人工维护。

**Follow-up**（触发条件：错误码总量 > 500 或跨域引用错误码场景 ≥ 3 处）：

- 引入错误码扫描脚本，检测：
  - 跨域引用其他域的 `Erp*Errors` 常量（应通过 SPI 解耦或挪到 `md` / `notify` 共享层）
  - 未使用的错误码定义（dead code）
  - 命名空间冲突（两域使用相同 `erp.err.<short>.<name>`）
- 自动生成 i18n 资源文件

## 与其他文档的关系

- `docs/design/domain-design-guidelines.md §7.1` — ErrorCode 命名空间规则（真相源）
- `docs/lessons/03-process-doc-status-naming.md` — 状态文本一致性经验
- `docs/architecture/system-baseline.md` — ErrorCode 技术落位（如建立后）
- 各域 `state-machine.md` — 状态机迁移拒绝路径引用的错误码
