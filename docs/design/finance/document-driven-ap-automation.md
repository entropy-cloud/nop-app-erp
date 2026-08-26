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

## 前置调研结论（2026-08-26，E3 整体计划 Phase 1）

- **`nop-file` 能力核实（活仓核验）**：文件上传/存储/引用全链在应用内已可用——`nopFileStore` bean（`DaoResourceFileStore`，`nop-file-dao`，本地目录零配置默认）+ `NopFileRecord` 实体（`/f/upload`、`/f/download/{fileId}` REST 端点经 `nop-quarkus-file` 随 web starter 引入）；`erp-fin-service` 已直接依赖 `nop-biz-file-core`（`IFileStore` SPI）。业务表存 `fileId` 引用 + 解析字段即可（AP-3 成立），文件本体全部走 nop-file，不新建存储。
- **OCR 选型裁决**：
  - 候选盘点：① tess4j（JNI 封装 libtesseract，Apache-2.0）——需系统级二进制 + `chi_sim` 语言包，本机与 CI/E2E 环境均不可用（核实：`tesseract` 不存在）；② 外部云 OCR——**排除**（约束：不引入外部云服务与密钥）；③ 纯 Java 文本抽取——PDFBox（Apache-2.0）**已随平台 `nop-report-pdf` 在 `erp-fin-service` 依赖树内**（零新增第三方依赖），对数字 PDF/文本文件可直接抽取文本。
  - **选定**：`IErpFinOcrEngine` SPI + 默认实现 = **本地文本抽取引擎**（PDFBox 抽取数字 PDF/txt 文本 + 要素规则解析）；扫描件/图片在默认引擎下无文本 → 置信度低 → 挂人工队列（人工门不可绕过，AP-1 成立）。真 OCR 引擎（tess4j 适配器）为 SPI 可插拔项，触发条件 = 目标环境具备 tesseract 二进制与语言包。
  - 分层验证（对齐计划 Infrastructure 裁决）：JUnit 用 SPI 测试替身 + 真实数字 PDF/txt 文件验证默认引擎；E2E 用数字 PDF/txt 夹具走真实管道；扫描图片路径以低置信人工门 JUnit 验证。
- **异步载体核实**：既有 finance 作业全部为 nop-job → nop-batch 模式（`app-erp-all/_vfs/nop/job/conf/*.job.yaml` + 域内 `_vfs/nop/batch-task/fin/*.batch.xml`，默认关闭）；管道异步步骤沿用该模式，不新建队列基建（AP-4 成立）。
- **ORM 变更清单**（§8.1 E3.5 授权行，已随计划 dual-agent-approval）：新增 `ErpFinApDocument`（文档引用 + 解析结果 + 分类/置信度/草稿回链字段）、`ErpFinApDocumentLog`（处理轨迹日志）两实体 + 配套字典；`ErpPurInvoice` 零变更（草稿经既有 `IErpPurInvoiceBiz` 管道创建，三单匹配校验走既有 approve 链路，AP-5 成立）。

## 落地策略（分阶段）

| 阶段 | 内容 | 状态 |
|------|------|------|
| 设计 | 本文档（管道 + 分类 + 邮件 + 审计） | ✅ 已完成（本批次） |
| 前置调研 | OCR 引擎选型（Tesseract 等）、`nop-file` 能力核实 | ✅ 已完成（2026-08-26，结论见上节） |
| 实现 | 摄取管道（上传→解析→分类→草稿→预填）随 E3 整体计划实施（`2026-08-26-0735-2` Phase 6）；config-gate 默认关闭；ORM 变更按 §8.1 授权清单落地 | in progress |

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