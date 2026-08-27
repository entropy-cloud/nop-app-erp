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
| 实现 | 摄取管道（上传→解析→分类→草稿→预填）随 E3 整体计划实施（`2026-08-26-0735-2` Phase 6）；config-gate 默认关闭；ORM 变更按 §8.1 授权清单落地 | ✅ done（E3.5，2026-08-27：`ErpFinApDocument/ErpFinApDocumentLog`（dual-agent 批准清单 #4/#5）+ `ErpFinApDocumentPipelineProcessor` 端到端编排（上传→`IErpFinOcrEngine` 默认 PDFBox 文本抽取→`IErpFinApDocClassifier` 规则分类→UNSUBMITTED 草稿→三单匹配预填，低置信挂 MANUAL_REVIEW 人工门）；异步载体 nop-batch/nop-job 默认关闭；`erp-fin.ap-doc-pipeline-enabled` config-gate；E3.6 护栏 `IRateLimiter` 接线上传入口；IT 6 用例 + E2E 2 用例） |
| P2 加固 | 入口契约（白名单/文件名精度/base64 NopException）+ PARSE 专属错误码 + 重复上传守卫 + 分类确定性 + OCR 可观测/页数上界（P2-1/3/6/7/8/9，裁决见下节） | ✅ done（2026-08-28，plan `2026-08-28-0219-1` Phase 1） |

## P2 加固裁决登记（2026-08-28，plan `2026-08-28-0219-1`）

### P2-1 重复上传幂等：应用层轻量守卫（方案 A）

- **裁决**：upload 入口落地应用层守卫（零 ORM）——按 `fileName` 查非终态（RECEIVED/PARSED/CLASSIFIED/DRAFTED）文档，`fileLength` 相同者逐候选读文件本体比对内容摘要，命中即拒绝（`ERR_AP_DOC_DUPLICATE_UPLOAD`，携带既有文档 docId 与状态）。终态（FAILED/MANUAL_REVIEW/ARCHIVED）不拦截，允许修复/复核后重传。
- **边界说明（最终权威去重 = 三单匹配）**：本守卫是摄取入口的防重闸门，**不是**幂等的权威层——同一发票经不同文件名/重命名/多渠道（EDI、邮件附件）进入时，最终去重责任仍在既有三单匹配校验（AP-5：解析结果只作预填，校验走既有 approve 链路）。
- **并发残留风险**：两个同内容并发上传可能同时通过守卫（无 DB 唯一约束，§8.1 授权已消费、新增唯一索引未授权），产生两份 RECEIVED 文档；后果有界——两份各自走管道，草稿均为 UNSUBMITTED，重复入账由三单匹配与人工审核拦截。ORM 唯一约束（如内容哈希列 + 条件唯一索引）留待下次 §8.1 授权窗口评估。
- **被拒替代方案**：方案 B（去重责任完全后移三单匹配 + 零代码）——被拒，因摄取入口不设闸门会让批量误操作（同一文件循环上传）直接放大为 N 份草稿，排障噪声高。

### P2-3 dict `ARCHIVED` 死状态：保留 + writer 归属显式化（方案 A）

- **裁决**：保留 `erp-fin/ap-doc-status` 的 `ARCHIVED` 值与 `process()` 守卫对它的消费语义（DRAFTED/ARCHIVED 不可重复处理），**不**在本批移除。
- **writer 归属**：`ARCHIVED` 的唯一合法 writer = 邮件摄取落地时对已完结文档的自动归档（§3 邮件摄取，E3.5 起 Non-Goal 暂缓）。触发条件 = 邮件摄取进入实现计划时，归档动作随 ingestion writer 一并落地；在此之前 `ARCHIVED` 无写入方，属「dict 值先于 writer 落地」的已知状态（lesson-10 家族：消费侧守卫先行，避免 writer 落地时补状态机）。
- **被拒替代方案**：方案 B（移除 dict 值 + 简化守卫，邮件摄取落地时回加）——被拒，因 dict/守卫回加需要跨批次协调 schema 与状态机漂移，且当前消费侧语义（终态不可重复处理）本身正确。

### P2-6 伙伴匹配内存扫描保留理由

名称双向包含匹配（`伙伴名 ⊇ 供应商名` 或 `供应商名 ⊇ 伙伴名`）无法下推为单一 ORM 查询过滤而不丢失反向包含命中（如下推 `name like %供应商名%` 会漏「伙伴名是供应商名子串」的匹配）；候选集有 `erp-fin.ap-doc-partner-match-limit`（默认 2000）上界 + 按 id 升序确定性排序，内存扫描代价有界且首匹配稳定。超限截断 WARN 可观测（未加载部分不可见，未匹配者照旧落人工门）。

### P2-8 上传入口白名单（定稿枚举）

扩展名：`pdf/txt/csv/json/xml/png/jpg/jpeg`；MIME：`application/pdf`、`text/*`、`application/json`、`application/xml`、`image/png`、`image/jpeg`（含 contains 语义的等价变体）。pdf/txt/csv/json/xml 可被默认引擎抽取文本；png/jpg/jpeg 过白名单后由默认引擎返回 null 落人工门（合法扫描件分支，人工门不可绕过）。`fileName` 超 200 字符（列精度）拒绝；非法 base64 以 `ERR_AP_DOC_INVALID_BASE64`（NopException 范式）拒绝。

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