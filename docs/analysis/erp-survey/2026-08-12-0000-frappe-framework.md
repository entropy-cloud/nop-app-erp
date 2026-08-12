---
调研日期: 2026-08-12
来源: ~/sources/erp/frappe（GitHub frappe/frappe，浅克隆）
分类: 国际开源 · Python（低代码框架）
状态: 已完成（基于源码实测）
---

# Frappe Framework 调研报告

> ERPNext 的底座框架（MIT License），"The best code is the one that is not written"。**元数据驱动（语义驱动）** 的全栈低代码 Web 框架：模型定义自动生成 REST API、后台管理、报表。与 Nop Platform 同为"模型驱动"路线，但路径不同（Python 元类 + 运行时 vs Java codegen + Delta）——**平台架构对照参考**。

## 1. 基本信息

| 项 | 值 |
|---|---|
| 技术栈 | Python 3 + MariaDB + JS（前端 desk） |
| License | MIT |
| 定位 | 元数据驱动的全栈低代码 Web 框架（为 ERPNext 而生） |
| 工具链 | `bench` 命令行工具 |

## 2. 核心架构（源码实测）

### 2.1 元数据驱动模型（DocType）

- 受 **Semantic Web** 启发：应用围绕元数据定义构建（`frappe/model/`），DocType 声明式定义字段/权限/流程
- 模型定义 → 自动生成：REST API（`frappe/api/`）、后台管理界面（desk）、无代码 Report Builder（`frappe/query_builder` 等）

### 2.2 内置领域能力（框架级目录）

- `frappe/{automation,contacts,core,custom,desk,email,geo,integrations,printing,workflow,website}`
- 角色权限（`frappe/permissions/`）、认证（`frappe/auth/`）、搜索（`frappe/search/`）、数据层（`frappe/data/`）、数据库（`frappe/database/`）

### 2.3 与 Nop Platform 的路径差异

| 维度 | Frappe | Nop Platform |
|------|--------|-------------|
| 模型真相源 | Python 元类 DocType（运行时元数据表） | XML（orm.xml，编译时唯一真相） |
| 代码生成 | 无（运行时解释 + 自动 API） | nop-cli gen 生成完整模块链 |
| 定制 | 覆盖 DocType/权限/脚本 | Delta 差量合并（x:extends） |
| 前端 | JS desk（生成式后台） | AMIS / 自定义视图 XML |
| 类型安全 | 动态 | 编译期 Java 类型 |

## 3. 对 nop-app-erp 的借鉴

| # | 借鉴点 | Frappe 证据 | 对 nop 的落地建议 |
|---|--------|------------|-------------------|
| 1 | **语义驱动建模哲学** | Semantic Web 启发的元数据模型 | 对照 nop 的模型驱动；两者哲学同源（模型即真相），实现差异（codegen vs runtime）正是 nop 的平台级优势 |
| 2 | **模型自动生成 REST API** | frappe/api 自动暴露 | Nop 的 GraphQL 自动暴露同理，无需借鉴实现 |
| 3 | **无代码 Report Builder** | 内置报表构建 | 对照 nop-report 的报表设计器体验 |
| 4 | **bench 工具链** | 一键开发/部署 | 对照 nop 的 build.sh / nop-cli |

## 4. 关键证据文件

- `/Users/abc/sources/erp/frappe/frappe/model/`（DocType 元数据模型）
- `/Users/abc/sources/erp/frappe/frappe/api/`（自动 REST API）
- `/Users/abc/sources/erp/frappe/frappe/workflow/`（工作流）
- `/Users/abc/sources/erp/frappe/frappe/permissions/`（权限）

> ⚠️ 定位提示：ERPNext 的功能调研已覆盖其业务面；本报告聚焦框架层架构对照。
