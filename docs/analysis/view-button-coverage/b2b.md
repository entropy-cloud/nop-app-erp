# B2B 视图按钮需求覆盖分析

## 分析范围

B2B 域共 13 个实体，涵盖 EDI 消息处理、合作伙伴管理、ASN 管理、MFT 通道管理四大功能域。分类如下：

| 分类 | 实体数 | 实体名单 |
|------|--------|----------|
| **CRUD+Custom** | 2 | ErpB2bEdiDoc, ErpB2bCodeMapping |
| **CRUD** | 11 | ErpB2bPartnerProfile, ErpB2bEdiFormat, ErpB2bMftConfig, ErpB2bEdiLog, ErpB2bAsn, ErpB2bAsnLine, ErpB2bPartnerCredential, ErpB2bMftLog, ErpB2bMftCertificate, ErpB2bCertificationChecklist, ErpB2bTestExchange |

## 期望按钮推导依据

1. **CRUD 基线**（METHODOLOGY §1.1）：`add-button`、`batch-delete-button`（toolbar）；`row-view-button`、`row-update-button`、`row-delete-button`（row）。适用于所有实体。

2. **EDI 事务专用按钮**（`ui-patterns.md §EDI 事务`）：
   - 列表页工具栏：`[新建]`、`[重试]`、`[导出]` — `retry-button`（toolbar）为核心 EDI 操作
   - 列表页操作组：`[查看]`、`[操作▾]` — 下拉含 `retry` / `cancel` / `archive`
   - 详情页工具栏：`[重试]`、`[取消]`、`[归档]`、`[下载报文]`
   - 状态机（`state-machine.md`）定义 EdiDoc 的 ERROR→TO_SEND（retry）、SENT→TO_CANCEL（cancel）、RECEIVED→ARCHIVED（archive）三条手动操作

3. **合作伙伴状态迁移按钮**（`partner-onboarding.md`）：PartnerProfile 具有 REGISTERED→TESTING→CERTIFIED→PRODUCTION 状态生命周期，所有迁移均为 B2B 管理员手动操作。预期 row 级别状态迁移按钮。

4. **代码映射专用按钮**（`ui-patterns.md §代码映射`）：工具栏 `[新建]`、`[批量导入]`、`[导出]` — `batch-import-button`、`export-button`。

5. **ASN**（`asn-processing.md`）：状态机 RECEIVED→MATCHED→RECEIVED_TO_STOCK→CANCELLED。但 ASN 处理以系统自动执行为主（auto-match 后触发采购域入库），无明确 view 级别按钮描述，仅 `cancel` 操作可能需手动触发。

6. **配置/日志实体**（EdiFormat, MftConfig, EdiLog, MftLog, MftCertificate, Credential, 子表）：仅 CRUD 基线。

## 逐实体分析

### ErpB2bEdiDoc — CRUD+Custom

- **期望按钮**：add-button, batch-delete-button / row-view-button, row-update-button, row-delete-button + toolbar retry-button, toolbar export-button + row retry-button, row cancel-button, row archive-button
- **实际按钮**：add-button, batch-delete-button / row-view-button, row-update-button, row-delete-button
- **差距**：
  - `retry-button`（toolbar + row）: missing (blocker) — 核心 EDI 操作，ui-patterns 在列表页工具栏和操作组均有明确展示，state-machine.md 定义 ERROR→TO_SEND 重试路径，use-cases.md UC-B2B-006 确认重试为业务必选操作
  - `cancel-button`（row）: missing (blocker) — 状态机定义 SENT→TO_CANCEL 和 ERROR→CANCELLED 两条手动取消路径
  - `archive-button`（row）: missing (major) — 状态机定义 RECEIVED→ARCHIVED，是 EDI 事务完结的标准操作
  - `export-button`（toolbar）: missing (info) — ui-patterns 列表页工具栏展示此按钮
- **判定**：blocker（核心 EDI 操作 retry/cancel 无 UI 触发入口）

### ErpB2bPartnerProfile — CRUD

- **期望按钮**：CRUD + row test-button（REGISTERED→TESTING）, row certify-button（TESTING→CERTIFIED）, row go-live-button（CERTIFIED→PRODUCTION）, row suspend-button（PRODUCTION→SUSPENDED）, row restore-button（SUSPENDED→PRODUCTION）, row terminate-button（任意→TERMINATED）
- **实际按钮**：CRUD
- **差距**：
  - 所有 6 个状态迁移按钮均 missing（blocker — partner-onboarding.md 明确定义 B2B 管理员需手动执行每一状态跃迁，无 UI 按钮则整个合作方上线流程无法在界面端操作）
- **判定**：blocker（合作伙伴全生命周期状态迁移无 UI 触发入口）

### ErpB2bCodeMapping — CRUD+Custom

- **期望按钮**：CRUD + toolbar batch-import-button, toolbar export-button
- **实际按钮**：CRUD
- **差距**：
  - `batch-import-button`: missing (minor) — ui-patterns 工具栏明确展示
  - `export-button`: missing (info) — ui-patterns 工具栏明确展示
- **判定**：minor

### ErpB2bEdiFormat — CRUD

- **期望按钮**：CRUD
- **实际按钮**：CRUD
- **差距**：无
- **判定**：clean

### ErpB2bEdiLog — CRUD

- **期望按钮**：CRUD（标准日志列表）
- **实际按钮**：CRUD
- **差距**：无
- **判定**：clean

### ErpB2bMftConfig — CRUD

- **期望按钮**：CRUD（通道配置，标准配置类页面）
- **实际按钮**：CRUD
- **差距**：无
- **判定**：clean

### ErpB2bMftLog — CRUD

- **期望按钮**：CRUD（传输日志，标准日志列表）
- **实际按钮**：CRUD
- **差距**：无
- **判定**：clean

### ErpB2bMftCertificate — CRUD

- **期望按钮**：CRUD（证书管理，标准配置类页面）
- **实际按钮**：CRUD
- **差距**：无
- **判定**：clean

### ErpB2bAsn — CRUD

- **期望按钮**：CRUD（系统自动驱动为主，暂无非终态→作废手动按钮）
- **实际按钮**：CRUD
- **差距**：无（按文档严格准则）。**观察**：asn-processing.md 定义 RECEIVED→CANCELLED 路径，但 ASN 生命周期以系统自动执行为主，当前未要求独立 `cancel-button`。若后续补充手动取消需求，需追加 row-cancel-button。
- **判定**：clean（info: 模型-视图语义不匹配）

### ErpB2bAsnLine — CRUD

- **期望按钮**：CRUD（子表）
- **实际按钮**：CRUD
- **差距**：无
- **判定**：clean

### ErpB2bPartnerCredential — CRUD

- **期望按钮**：CRUD（合作方凭证，子表）
- **实际按钮**：CRUD
- **差距**：无
- **判定**：clean

### ErpB2bCertificationChecklist — CRUD

- **期望按钮**：CRUD（认证检查清单，子表）
- **实际按钮**：CRUD
- **差距**：无
- **判定**：clean

### ErpB2bTestExchange — CRUD

- **期望按钮**：CRUD（测试交换记录，子表）
- **实际按钮**：CRUD
- **差距**：无
- **判定**：clean

## 汇总

| 实体 | 预期分类 | 实际分类 | 差距严重程度 | 关键缺失 |
|------|----------|----------|--------------|----------|
| ErpB2bEdiDoc | CRUD+Custom | CRUD | **blocker** | retry-button（toolbar+row）、cancel-button（row） |
| ErpB2bPartnerProfile | CRUD+Custom | CRUD | **blocker** | test/certify/go-live/suspend/restore/terminate 共 6 个状态迁移按钮 |
| ErpB2bCodeMapping | CRUD+Custom | CRUD | minor | batch-import-button、export-button |
| ErpB2bEdiFormat | CRUD | CRUD | clean | — |
| ErpB2bEdiLog | CRUD | CRUD | clean | — |
| ErpB2bMftConfig | CRUD | CRUD | clean | — |
| ErpB2bMftLog | CRUD | CRUD | clean | — |
| ErpB2bMftCertificate | CRUD | CRUD | clean | — |
| ErpB2bAsn | CRUD | CRUD | clean | — |
| ErpB2bAsnLine | CRUD | CRUD | clean | — |
| ErpB2bPartnerCredential | CRUD | CRUD | clean | — |
| ErpB2bCertificationChecklist | CRUD | CRUD | clean | — |
| ErpB2bTestExchange | CRUD | CRUD | clean | — |

## 未覆盖实体观察

### Agreement

任务清单中列出的 "Agreement" 实体在 B2B 域 ORM 模型（`module-b2b/model/app-erp-b2b.orm.xml`）和所有 13 个 view.xml 中均不存在。设计文档（ui-patterns.md、state-machine.md、use-cases.md、partner-onboarding.md、asn-processing.md）也未定义 "Agreement" 实体。此为**模型缺口** — 若 Agreement 是未来需求，需先定义 ORM 模型、设计状态机、补充完整 CRUD+Custom 页面。
