# 文档驱动 AP 自动化管道设计（Document-Driven AP Automation）

## 定位

本文基于 Paperless-ngx（文档管理系统）调研，设计 nop-app-erp **文档驱动应付/应收自动化的摄取管道**：扫描件/邮件附件 → OCR → 分类 → 结构化单据 → 与既有三单匹配/过账链路衔接。**当前阶段只产出设计与分析，不进入编码状态**（2026-08-12 用户指示）。

## 来源与背景

- 参考报告：`docs/analysis/erp-survey/2026-08-12-0000-paperless-ngx.md`（消费管道 ingest→OCR→classify→archive + 邮件摄取 + AI 模块 + 审计日志）。
- 现状基线：
  - b2b 域 EDI 已落地（`b2b-integration.md` 报文/信封状态机；MFT 实体 `ErpB2bMftLog/ErpB2bMftCertificate` 在 module-b2b，结构化报文入口）；
  - AP 三单匹配（订单-入库-发票）已落地（`core-business-roadmap.md` M1）；
  - 通知链路已存在（`IErpSysNotificationBiz` + notify 域 3 实体，`notification-strategy.md`）；webhook 出站仅设计文档化、配置/日志表尚未实体化（`integration-pattern.md` 已注记）。
- 缺口：**非结构化文档入口**（纸质/PDF/邮件发票）完全缺失——供应商发票目前只能人工录入。

## 现状 vs Paperless-ngx 对照

| 维度 | nop 现状 | Paperless-ngx | 差距 |
|------|---------|---------------|------|
| 摄取入口 | b2b EDI（结构化） | 扫描件/邮件/上传多入口 | 非结构化摄取缺失 |
| OCR | 无 | OCR 解析器（含远程 OCR） | 需接入 OCR 引擎 |
| 分类 | 无（人工录入） | ML 自动分类打标（标签/对应方/单据类型） | 自动识别发票要素 |
| 管道化 | 无 | documents 消费管道 + 队列 | 管道编排缺失 |
| 审计 | 业务审计日志 | django-auditlog | 可复用既有审计 |

## 设计要点

### 1. 摄取管道（Consumption Pipeline）

```
文档入口(扫描上传/邮件/API)
    ↓
OCR 解析(发票要素提取: 供应商/金额/日期/税号/行项)
    ↓
自动分类(规则 + ML: 单据类型/对应方/匹配到采购单)
    ↓
草稿单据生成(ErpPurInvoice 草稿, approveStatus=UNSUBMITTED)
    ↓
三单匹配校验(既有链路) → 审核 → 过账(既有链路)
```

- **管道即队列**：摄取、解析、分类为异步步骤（对照 Paperless 队列 + nop-job/事件驱动）；「生成草稿单据」为人工确认门（分类置信度低时挂起人工）。
- **单据载体**：文档文件存 `nop-file` 模块（对照报告「与 nop-file 模块对照」），业务表只存文件引用 + 解析结果字段（不落全文）。

### 2. 分类引擎（规则优先 + ML 可选）

- **Phase 1 设计**：规则引擎（文件名/发件人/OCR 关键字段 → 单据类型/对应方），经 `nop-rule` 或既有规则模式承载；规则结果附置信度，低置信挂人工队列。
- **Phase 2（触发）**：ML 分类器作为可选实现（对照 paperless_ai），经 SPI 注入（对齐 `IErpFinAcctDocProvider` 注入范式）。
- **与三单匹配衔接**：解析出的供应商/金额/日期作为三单匹配的预填输入，不替代既有匹配校验逻辑。

### 3. 邮件摄取（对照 paperless_mail）

- 供应商发票邮件入口：邮件轮询 → 附件入管道 → 自动归档（对照 b2b MFT/EDI 之外的补充入口）。
- 设计：复用既有通知链路 + nop-job 定时轮询范式；**webhook 配置表实体化前不得引用 `ErpSysWebhookConfig`**（integration-pattern.md 已注记未落地）；邮件凭证（IMAP）配置归 `external-api-integration-pattern.md` 端点配置范式。

### 4. 审计与追溯

- 每个文档生命周期（摄取→解析→分类→入账）记录审计日志（文档处理轨迹 + 业务实体回链：`docs/design/voucher-back-link-patterns.md` 同型回链）。
- 文档-发票-凭证三方回链可追溯（对应 Paperless auditlog 的 ERP 形态）。

## 落地策略（分阶段）

| 阶段 | 内容 | 状态 |
|------|------|------|
| 设计 | 本文档（管道 + 分类 + 邮件 + 审计） | ✅ 已完成（本批次） |
| 前置调研 | OCR 引擎选型（Tesseract 等）、`nop-file` 能力核实 | todo（roadmap E3.5 前置） |
| 实现 | 全部推迟至调研结论后，随 E3 整体计划实施（plan-first）；ORM 变更（文档引用/解析字段）已获授权（`erp-enhancement-roadmap.md` §8.1） | **暂不编码** |

## 反模式自检表

| # | 反模式 | 正确做法 |
|---|--------|----------|
| AP-1 | OCR/分类结果直接过账（无人工门） | 生成草稿单据 + 三单匹配校验 + 审核流程 |
| AP-2 | 解析逻辑写死在 Pipe 内 | 规则引擎承载 + SPI 可注入 ML |
| AP-3 | 全文存储进业务表 | 文件存 `nop-file`，业务表存引用+解析字段 |
| AP-4 | 重复实现邮件/队列基建 | 复用 nop-job + 既有事件/通知链路 |
| AP-5 | 绕过既有三单匹配建新校验 | 解析结果只作预填，校验走既有链路 |

## 相关文档

- `docs/analysis/erp-survey/2026-08-12-0000-paperless-ngx.md` — 参考报告
- `docs/architecture/b2b-integration.md` — 既有 EDI/MFT 入口
- `docs/architecture/external-api-integration-pattern.md` — 端点/凭证配置范式
- `docs/design/finance/posting.md` — 过账链路（管道末端）
- `docs/backlog/erp-enhancement-roadmap.md` — 本主题 roadmap