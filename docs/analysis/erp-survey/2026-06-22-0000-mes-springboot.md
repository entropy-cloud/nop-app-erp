---
调研日期: 2026-06-22
来源: ~/sources/erp/MES-SpringBoot（GitHub wangziyang6/MES-SpringBoot，浅克隆）
分类: 国产开源 · SpringBoot
状态: 已完成（基于源码实测）
---

# MES-SpringBoot 调研报告

> 轻量级 MES（制造执行系统），SpringBoot + MyBatis-Plus。**对 nop-app-erp 的制造域（manufacturing）BOM 与工艺路线设计有参考价值，但无质量/设备模块。**

## 1. 基本信息

| 项 | 值 |
|---|---|
| 技术栈 | Spring Boot 2.1.7 · Java 8 · MyBatis-Plus 3.1.2 · MySQL · Redis · Shiro |
| License | 开源 |
| 定位 | 轻量 MES，聚焦工艺路线（BOM + 工序 + 工艺流程） |
| 源码规模 | 单模块 SpringBoot 应用 |

## 2. 模块结构

| 包 | 职责 |
|---|------|
| `technology/` | BOM、工艺流程、工序 |
| `basedata/` | 物料主数据 |
| `system/` | 用户、角色、菜单、部门、字典 |
| `digitization/` | 生产计划（仅 stub） |

## 3. 核心业务实体

| 实体 | 文件 | 职责 |
|------|------|------|
| **SpBom** | `technology/entity/SpBom.java:16` | BOM 头：bomCode、materielCode、state、versionNumber、factory |
| **SpBomItem** | `technology/entity/SpBomItem.java:17` | BOM 行：bomHeadId、materielItemCode、itemNum、itemUnit、operTyper |
| **SpFlow** | `technology/entity/SpFlow.java:15` | 工艺流程：flow、flowDesc、process |
| **SpOper** | `technology/entity/SpOper.java:15` | 工序：oper、operDesc |
| **SpFlowOperRelation** | `technology/entity/SpFlowOperRelation.java:15` | 工艺-工序关联：flowId、perOper、oper、nextOper、sortNum、operType |
| **SpMaterile** | `basedata/entity/SpMaterile.java:14` | 物料主数据：materiel、matType、unit、size、model、flowId |

## 4. BOM 与工艺路线模型

**BOM 结构**：SpBom（头）+ SpBomItem（行），每行关联一个工序类型（operTyper）。

**工艺流程**：SpFlow（流程）→ SpFlowOperRelation（流程-工序关联）→ SpOper（工序），形成工序序列。

**物料与工艺绑定**：SpMaterile.flowId 关联到 SpFlow，表示该物料的生产工艺路线。

## 5. 局限性

- **无质量检验模块**：无 quality inspection 相关实体
- **无设备管理模块**：无 equipment/device 实体
- **无工单执行**：无 WorkOrder/JobCard 实体，只有 BOM 和工艺定义
- **无成本核算**：无成本相关字段或逻辑
- **命名不规范**：`SpMaterile`（应为 Material）拼写错误

## 6. 对 nop-app-erp 的可借鉴设计点

| # | 借鉴点 | MES-SpringBoot 证据 | 对 nop 的落地建议 |
|---|--------|---------------------|-------------------|
| 1 | **BOM 头+行结构** | SpBom + SpBomItem | nop BOM 头记录产出物料，BOM 行记录子件清单 |
| 2 | **工艺流程与工序分离** | SpFlow + SpOper + SpFlowOperRelation | nop 工艺路线独立表，工序序列通过关联表定义 |
| 3 | **物料绑定工艺路线** | SpMaterile.flowId | nop 物料主数据可绑定默认工艺路线 |
| 4 | **BOM 行绑定工序** | SpBomItem.operTyper | nop BOM 行可标注"在哪道工序消耗" |

## 7. 不建议借鉴的点

- **无工单执行**：nop 需要完整的工单（WorkOrder）+ 作业卡（JobCard）模型
- **无齐套校验**：nop 需要工单领料前校验子件可用量
- **无联副产品**：nop BOM 需支持联产品/副产品定义
- **无成本核算**：nop 需要工单成本归集与结转

## 8. 关键证据文件

- `/Users/abc/sources/erp/MES-SpringBoot/mes/src/main/java/com/wangziyang/mes/technology/entity/SpBom.java`（BOM 头）
- `/Users/abc/sources/erp/MES-SpringBoot/mes/src/main/java/com/wangziyang/mes/technology/entity/SpBomItem.java`（BOM 行）
- `/Users/abc/sources/erp/MES-SpringBoot/mes/src/main/java/com/wangziyang/mes/technology/entity/SpFlow.java`（工艺流程）
- `/Users/abc/sources/erp/MES-SpringBoot/mes/src/main/java/com/wangziyang/mes/technology/entity/SpOper.java`（工序）
