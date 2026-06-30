# 合同仓库与全文检索设计

## 目的

设计合同仓库功能，实现合同文档的集中存储、OCR 识别、全文检索、元数据标签和高级搜索。参考 SAP Contract Lifecycle Management Repository 和 SpringCM 的文档管理模式。

## 设计依据

> 参考 **SAP CLM**：文档存储库 + 元数据索引 + 合规保留策略。
>
> 参考 **SpringCM**（DocuSign CLM）：文件夹结构 + 全文搜索 + 自定义字段。
>
> 来源 `docs/analysis/erp-survey/2026-06-30-0000-axelor-open-suite.md` §合同管理。

## 架构概览

```
┌─────────────────────────────────────────────────────────────┐
│                  合同仓库 (Contract Repository)                │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  上传层: UI 上传 / API 上传 / 批量导入                        │
│                                                             │
│  ┌───────────┐    ┌───────────┐    ┌───────────────────┐    │
│  │ OCR 识别   │    │ 全文索引   │    │ 元数据提取          │    │
│  │ (Tesseract │    │ (Elastic  │    │ 合同类型/金额/日期   │    │
│  │  /云OCR)   │    │  Search) │    │ 自定义字段          │    │
│  └───────────┘    └───────────┘    └───────────────────┘    │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              搜索层                                   │   │
│  │  全文搜索 | 元数据筛选 | 日期范围 | 金额范围 | 标签     │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              合规层                                   │   │
│  │  保留策略 | 归档策略 | 销毁策略 | 审计追溯             │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## 合同文档存储

### ErpCtDocument（合同文档）

`ErpCtDocument` 扩展了标准的附件机制，增加 OCR 文本、全文检索字段和合规保留日期。

| 字段 | 含义 | 参考 |
|------|------|------|
| id/orgId | 标准 | |
| contractId | 关联合同（可选，独立文档可无合同） | → ErpCtContract |
| code | 文档编码（自动生成或手动输入） | 🟢 SpringCM Document ID |
| docName | 文档名称 | |
| docType | dict `erp-ct/doc-type`：CONTRACT_SCAN / AMENDMENT / ATTACHMENT / CERTIFICATE / OTHER | |
| attachmentId | 附件 ID（指向文件存储） | |
| fileSize | 文件大小（字节） | |
| fileHash | 文件 SHA-256 哈希（防篡改校验） | |
| mimeType | 文件 MIME 类型 | |
| ocrText | OCR 识别后的文本内容 | |
| ocrStatus | dict `erp-ct/ocr-status`：PENDING / PROCESSING / COMPLETED / FAILED | |
| fullTextSearch | 全文检索索引字段（ocrText + 元数据组合） | |
| metadataTags | JSON 自定义标签（`{"party":"供应商A","region":"华东"}`） | 🟢 SpringCM 自定义字段 |
| retentionDate | 保留截止日期（到达后自动归档） | 🟢 SAP CLM 保留策略 |
| archiveDate | 归档日期（归档后不可修改） | |
| purgeDate | 销毁日期（到达后系统删除，终态） | |
| isArchived | 是否已归档 | |
| versionNo | 文档版本（同一附件更新时递增） | |
| remark | 备注 | |
| 标准审计字段 | | |

### 文档类型字典 `erp-ct/doc-type`

| code | label | value |
|------|-------|-------|
| CONTRACT_SCAN | 合同扫描件 | 10 |
| AMENDMENT | 补充协议 | 20 |
| ATTACHMENT | 附件 | 30 |
| CERTIFICATE | 证明文件 | 40 |
| OTHER | 其他 | 90 |

### OCR 状态字典 `erp-ct/ocr-status`

| code | label | value |
|------|-------|-------|
| PENDING | 等待处理 | 10 |
| PROCESSING | 处理中 | 20 |
| COMPLETED | 已完成 | 30 |
| FAILED | 识别失败 | 40 |

## OCR 识别

### 支持场景

| 输入类型 | 说明 | 建议方案 |
|----------|------|----------|
| 扫描件 PDF | 图片型 PDF 需要 OCR 提取文字 | Tesseract OCR / 云 OCR |
| 图片（JPG/PNG） | 拍照的合同图片 | Tesseract OCR / 云 OCR |
| 电子 PDF | 已有文字层的 PDF 直接提取 | PDFBox / iText 提取 |
| OFD（中国） | 中国电子发票/公文格式 | OFD 解析库 |

### OCR 流程

```
合同文档上传
    │
    ├─► 判断文件类型
    │     ├─ 电子 PDF → 直接提取文字 → fullTextSearch = 文字内容
    │     ├─ 扫描件 PDF → 调用 OCR 引擎
    │     ├─ 图片 → 调用 OCR 引擎
    │     └─ OFD → 调用 OFD 解析器
    │
    ├─► OCR 进度
    │     PENDING → PROCESSING → COMPLETED
    │                           → FAILED (重试或人工)
    │
    ├─► OCR 完成后
    │     ├─ ocrText = 识别文字
    │     ├─ fullTextSearch = ocrText + metadataTags 组合
    │     └─ 更新 ocrStatus = COMPLETED
    │
    └─► OCR 失败时
          ocrStatus = FAILED
          记录错误原因
          人工可重新提交 OCR
```

## 全文检索

### 索引策略

| 策略 | 说明 | 技术选型 |
|------|------|----------|
| 数据库全文索引 | MySQL FULLTEXT / PostgreSQL tsvector | 内建，无需额外组件 |
| 搜索引擎 | Elasticsearch / OpenSearch | 需要额外部署，搜索能力强 |
| 混合 | 数据库主索引 + ES 辅助 | 灵活但维护成本高 |

### 搜索字段组合

`fullTextSearch` 字段包含以下内容的拼接：

```
fullTextSearch = concat(
    docName,
    ocrText,
    code,
    metadataTags 中的关键值
)
```

## 元数据标签

### 预设标签

| 标签类型 | 说明 | 示例值 |
|----------|------|--------|
| party | 合同方 | 供应商A / 客户B |
| contractType | 合同类型 | 采购 / 销售 |
| region | 区域 | 华东 / 华南 |
| department | 所属部门 | 采购部 / 销售部 |
| projectCode | 项目编号 | PRJ-2026-001 |

### 自定义标签

用户可动态添加键值对标签，存储在 `metadataTags` JSON 字段中：

```json
{
  "party": "供应商A",
  "region": "华东",
  "projectCode": "PRJ-2026-001",
  "customField1": "值1"
}
```

## 高级搜索

### 搜索过滤器

| 过滤器 | 类型 | 示例 |
|--------|------|------|
| 全文搜索 | 文本 | `keyword` → 匹配 fullTextSearch |
| 文档编码 | 精确 | `code = DOC-001` |
| 文档类型 | 多选 | `docType IN (CONTRACT_SCAN, AMENDMENT)` |
| 关联合同 | 精确 | `contractId = 123` |
| 上传日期范围 | 日期区间 | `createTime BETWEEN 2026-01-01 AND 2026-06-30` |
| 文件大小范围 | 数字区间 | `fileSize >= 1024000` |
| 元数据标签 | 键值对 | `metadataTags->party = "供应商A"` |
| OCR 状态 | 多选 | `ocrStatus = COMPLETED` |
| 是否归档 | 布尔 | `isArchived = false` |

### 搜索结果排序

| 排序方式 | 说明 |
|----------|------|
| 相关性（全文搜索时） | 按 TF-IDF 匹配度降序 |
| 上传日期 | 按 createTime 降序（默认） |
| 文件大小 | 按 fileSize |
| 文档名称 | 按 docName 字母序 |

## 文档保留策略

### 保留策略配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `erp-ct.doc-retention-years` | 保留年限（到达后归档） | 10 |
| `erp-ct.doc-archive-years` | 归档保留年限（到达后销毁） | 20 |
| `erp-ct.doc-auto-archive` | 是否自动归档 | true |
| `erp-ct.doc-auto-purge` | 是否自动销毁 | false（需人工确认） |

### 生命周期

```
上传
 │
 ▼
 活跃期（createTime ~ retentionDate）
    ├─ 可编辑、可搜索、可下载
    └─ retentionDate 到达 → 归档（isArchived=true）
 │
 ▼
 归档期（retentionDate ~ purgeDate）
    ├─ 只读、可搜索、不可删除
    └─ purgeDate 到达 → 系统删除（软删除或真实删除）
 │
 ▼
 销毁（purgeDate 到达）
    └─ 完全清除（记录保留在审计日志中）
```

### 合规规则

| 规则 | 处理 |
|------|------|
| 合同 ACTIVE 期间不允许归档 | 合同状态 ACTIVE 时文档强制保留在活跃期 |
| 合同终止/到期后启动保留计时 | 从合同 endDate 开始计算 retentionDate |
| 审计锁定期 | 有进行中的审计时，文档不允许销毁 |
| 法律保留（Legal Hold） | admin 手动设置 legalHold=true 阻止所有归档/销毁操作 |

## 证据强度

| 证据 | 强度 | 说明 |
|------|------|------|
| 合同文档存储库 | 🟢 | SAP CLM Document Repository |
| 全文搜索 | 🟢 | SpringCM / DocuSign CLM 全文索引 |
| OCR 扫描识别 | 🟢 | Tesseract OCR / ABBYY |
| 元数据与自定义字段 | 🟢 | SpringCM 自定义字段模板 |
| 保留策略 | 🟢 | SAP CLM 生命周期管理 |
| 高级搜索 | 🟢 | Elasticsearch / 数据库全文索引模式 |

## 参考

- `contract/README.md`（合同域概览）
- `contract/use-cases.md` §UC-CT-10 仓库搜索
- `contract/state-machine.md`（合同状态机）
- `model/app-erp-contract.orm.xml`（ORM 模型）
