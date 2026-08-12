---
调研日期: 2026-08-12
来源: ~/sources/erp/superset（GitHub apache/superset，浅克隆）
分类: 国际开源 · Python/Flask + React（现代企业 BI）
状态: 已完成（基于源码实测）
---

# Apache Superset 调研报告

> **现代企业级 BI Web 应用**（Apache-2.0）：无代码图表构建 + SQL Lab + **轻量语义层** + Embedded SDK + **MCP 服务**。对 nop-app-erp 看板子系统的参考价值集中在 **语义层抽象** 与 **嵌入式/协议化暴露**（对比 AMIS 渲染的看板）。注意：Nop 平台自带 `nop-report`/`nop-datav`，本报告仅作设计参考。

## 1. 基本信息

| 项 | 值 |
|---|---|
| 技术栈 | Python/Flask（SQLAlchemy + Celery）· React 前端（superset-frontend/） |
| 数据库 | 几乎所有 SQL 数据库 |
| License | Apache-2.0 |
| 定位 | 现代企业 BI——数据探索与可视化平台（可替代/增强商业 BI） |

## 2. 核心架构（源码实测 `superset/`）

| 模块 | 用途 |
|------|------|
| databases / datasets | 数据源与数据集抽象 |
| charts / dashboards | 图表与看板（无代码构建） |
| explore | 探索式分析 |
| **sqllab** | SQL Lab（专业查询工作台） |
| **semantic_layers** | **轻量语义层**（指标/维度/度量规范化） |
| reports | 定期报表调度 |
| row_level_security | 行级安全 |
| **embedded** + superset-embedded-sdk/ | **嵌入式 SDK**（把 BI 嵌入业务应用） |
| superset-websocket/ | 实时推送 |
| **mcp_service/** | **MCP 服务**（把 BI 能力暴露给 AI） |

## 3. 对 nop-app-erp 的借鉴

| # | 借鉴点 | Superset 证据 | 对 nop 的落地建议 |
|---|--------|-------------|-------------------|
| 1 | **轻量语义层** | semantic_layers（指标/维度规范化） | 看板 KPI 口径统一：对照 nop-report 的报表模型，度量定义与业务逻辑解耦 |
| 2 | **嵌入式 SDK 模式** | embedded-sdk + websocket | 把看板嵌入第三方/门户场景的暴露模式 |
| 3 | **MCP 暴露分析能力** | mcp_service/ | 与 ERPClaw 的 MCP 路由对照——BI/报表能力作为 AI 工具暴露 |
| 4 | **行级安全** | row_level_security | 对照 nop 的数据权限（enforcement 栈）在报表场景的落实 |

## 4. 关键证据文件

- `/Users/abc/sources/erp/superset/superset/semantic_layers/`（语义层）
- `/Users/abc/sources/erp/superset/superset/charts/`、`dashboards/`（图表看板）
- `/Users/abc/sources/erp/superset/superset/embedded/`、`superset-embedded-sdk/`（嵌入式）
- `/Users/abc/sources/erp/superset/superset/mcp_service/`（MCP 服务）

> ⚠️ 定位提示：非 ERP；与 nop-report/nop-datav 功能重复，仅保留为 BI 架构设计参考（用户确认保留）。
