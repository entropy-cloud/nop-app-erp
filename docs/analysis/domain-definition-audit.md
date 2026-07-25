# ORM Domain 定义审计

> Plan: `docs/plans/2026-07-24-2200-1-cross-domain-code-abstraction.md` Phase 5 Exit Gate
> 审计日期：2026-07-25

## 结论：domain 定义已完备，无需 ORM 补充

实时仓库复核显示项目 ORM 模型中金额/数量/单价/税率等关键字段已普遍使用 `domain` 属性：

| domain | 出现次数 | 用途 |
|---|---|---|
| `amount` | 311 | 金额字段（精度由 domain 声明） |
| `quantity` | 115 | 数量字段 |
| `unitPrice` | 33 | 单价字段 |
| `taxAmount` | 19 | 税额字段 |
| `exchangeRate` | 37 | 汇率字段 |
| `createdBy`/`createTime`/`updatedBy`/`updateTime`/`version`/`delVersion` | ~350 each | 审计字段（标准 domain） |

## Phase 5 决策

由于 domain 定义已完备：
- **不需要 ORM domain 补充**（deferred 项不适用）
- Phase 5 直接进入 control.xlib 创建 + gen-control 脚本清理

## gen-control 脚本现状

- 224 个 view.xml 文件包含 `<gen-control>` 块
- 主要模式：datetime 格式化（`YYYY-MM-DD HH:mm:ss`）、数字精度/千分位、状态徽章
- 大部分 gen-control 与 domain 映射功能重复，可通过 control.xlib 消除
