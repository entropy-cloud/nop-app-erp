---
调研日期: 2026-06-22
来源: ~/sources/erp/Yu-FAMS-SpringBoot（GitHub watch-rain7/Yu-FAMS-SpringBoot，浅克隆）
分类: 国产开源 · SpringBoot
状态: 已完成（基于源码实测）
---

# Yu-FAMS-SpringBoot 调研报告

> 固定资产管理系统，SpringBoot + Vue 技术栈。**对 nop-app-erp 的固定资产域（assets）设计有直接参考价值，尤其是资产生命周期状态与折旧字段模型。**

## 1. 基本信息

| 项 | 值 |
|---|---|
| 技术栈 | Spring Boot 2.5.9 · Java 8 · MyBatis 2.2.1 · MySQL 8.0 · Vue |
| License | 开源 |
| 定位 | 中小企业固定资产全生命周期管理 |
| 源码规模 | 完整源码（Java + SQL + Vue） |

## 2. 核心业务实体

| 实体 | 文件 | 职责 |
|------|------|------|
| **Assets** | `entity/Assets.java:10` | 资产主记录：编码、名称、类别、型号、数量、取得日期、原值(money)、当前值(currentValue)、折旧方法(depreciate)、折旧率(depreciationRate)、部门、使用人、位置、状态 |
| **AssetsDetail** | `entity/AssetsDetail.java:8` | 资产变动审计追踪（操作前/后状态对比） |
| **AssetsTransfer** | `entity/AssetsTransfer.java:8` | 资产调拨（部门/使用人转移），含审批流程 |
| **AssetsRepair** | `entity/AssetsRepair.java:9` | 资产维修记录 |
| **AssetsReceive** | `entity/AssetsReceive.java:7` | 资产领用/归还 |
| **AssetsIn** | `entity/AssetsIn.java` | 资产入库 |
| **Inventory** | `entity/Inventory.java:10` | 资产盘点 |
| **Category** | `entity/Category.java` | 资产分类 |

## 3. 资产生命周期状态

| 实体 | 状态值 | 迁移 |
|------|--------|------|
| Assets.status | "正常" / "维修中" / "报废" | 正常→维修中→正常；正常→报废 |
| AssetsTransfer.status | "待审批" → "已审批" → "已调拨" / "已拒绝" | 审批流 |
| AssetsRepair.status | "处理中" → "已完成" | 维修完成 |
| Inventory.status | "待盘点" → "已盘点" → "已确认" | 盘点流程 |

## 4. 折旧模型

**字段设计**（`Assets.java:42-44`）：
- `depreciate`（String）：折旧方法名称
- `depreciationRate`（BigDecimal）：折旧率
- `money`（BigDecimal）：原值
- `currentValue`（BigDecimal）：当前账面价值

**局限**：折旧字段存在但**无自动折旧计算逻辑**——Service 层未实现按期计提折旧的算法。这是对 nop 的反面教训：字段设计不等于业务逻辑实现。

## 5. 对 nop-app-erp 的可借鉴设计点

| # | 借鉴点 | Yu-FAMS 证据 | 对 nop 的落地建议 |
|---|--------|-------------|-------------------|
| 1 | **资产+审计追踪双表** | Assets + AssetsDetail | nop 资产卡片变动时自动生成 Detail 记录（操作前/后快照） |
| 2 | **资产调拨独立实体** | AssetsTransfer | nop 调拨单独立表，记录来源/目的部门+使用人，含审批状态 |
| 3 | **资产领用/归还** | AssetsReceive | nop 资产可被领用（绑定使用人），归还时释放 |
| 4 | **资产盘点** | Inventory | nop 支持定期盘点，盘点差异自动调整 |
| 5 | **折旧字段模型** | depreciate + depreciationRate + money + currentValue | nop 资产卡片内建折旧方法、折旧率、原值、当前值字段 |

## 6. 不建议借鉴的点

- **无自动折旧计算**：nop 必须实现按期计提折旧的算法（直线法/双倍余额递减/工作量法）
- **无财务凭证集成**：nop 需要折旧→凭证的自动触发（通过 `IErpFinAcctDocProvider`）
- **状态过于简单**：nop 需要更完整的状态机（草稿→使用中→闲置→报废/出售）
- **无资产类别科目映射**：nop 需要资产类别绑定会计科目（固定资产/累计折旧/折旧费用）

## 7. 关键证据文件

- `/Users/abc/sources/erp/Yu-FAMS-SpringBoot/src/main/java/com/example/entity/Assets.java`（资产核心实体）
- `/Users/abc/sources/erp/Yu-FAMS-SpringBoot/src/main/java/com/example/entity/AssetsTransfer.java`（调拨实体）
- `/Users/abc/sources/erp/Yu-FAMS-SpringBoot/src/main/java/com/example/entity/AssetsRepair.java`（维修实体）
- `/Users/abc/sources/erp/Yu-FAMS-SpringBoot/src/main/resources/sql/xm-assets-数据库.sql`（数据库脚本）
