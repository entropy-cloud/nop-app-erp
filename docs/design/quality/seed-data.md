# quality 域种子数据

> **资产类型**：部署期种子资产（`app-erp-all/src/main/resources/_vfs/_init-data/`），非测试资产（边界裁决见 `docs/architecture/seed-data.md` 头部）。
> **落地批次**：M1.2b（plan `docs/plans/2026-09-01-1245-3-m12b-mnt-qa-prj-seed-expansion.md`，2026-09-01）。此前 6 张 quality 表 seed（inspection / non_conformance / action，plan `2026-07-09-0930-2`；spc_chart / spc_sample / spc_capability + NCR SPC 追加行，plan `2026-07-09-1145-2`）见 `docs/architecture/seed-data.md` 历史批次段。
> **业务语义 owner docs**：`docs/design/quality/` 各域文档（spc.md / recall.md / integration-testing.md §C16 勘误等）；本文件只登记种子数据面，不重复业务语义。

## 种子数据范围（M1.2b 批次 10 表）

quality 域 16 规格实体中，6 表已由历史批次 seed；本批补齐其余 **10 规格表（28 行）**，达成 quality 域全量 seed 覆盖（16 / 16）。

### 10 规格表 CSV

| 实体 | CSV 文件 | 行数 | 用例指示 | 必填 FK 闭环 |
|---|---|---|---|---|
| ErpQaCalibration（量具校准） | `erp_qa_calibration.csv` | 3 | P×2+N-TERM(CANCELLED) | —（无必填 FK）；CALIBRATED_BY 可选→ErpMdEmployee〔跨域:md·已seed 21〕；**TARGET_VALUE/TOLERANCE 留空**（见「执行期登记」） |
| ErpQaInspectionLine（检验单行） | `erp_qa_inspection_line.csv` | 3 | P | INSPECTION_ID→ErpQaInspection〔已seed 1/2/3〕；RESULT ∈ `erp-qa/inspection-result`；行语义与头表结论一致（1/3 ACCEPTED、2 REJECTED） |
| ErpQaInspectionTemplate（检验模板） | `erp_qa_inspection_template.csv` | 2 | P+N-DIS(IS_ACTIVE=0) | —（无必填 FK）；MATERIAL_ID 可选→ErpMdMaterial〔已seed 3/1〕 |
| ErpQaInspectionTemplateLine（模板参数行） | `erp_qa_inspection_template_line.csv` | 3 | P（2+1 按头） | TEMPLATE_ID→ErpQaInspectionTemplate〔本批〕 |
| ErpQaQualityGoal（质量目标） | `erp_qa_quality_goal.csv` | 3 | P×2+N-TERM(CANCELLED) | —（无必填 FK）；RESPONSIBLE_PERSON_ID 可选→ErpMdEmployee〔已seed 21〕 |
| ErpQaRecall（召回单） | `erp_qa_recall.csv` | 3 | P×2+N-TERM(CANCELLED) | —（必填 FK 无）；SOURCE_NCR_ID 可选→ErpQaNonConformance〔已seed 1〕、MATERIAL_ID 可选→〔已seed 3〕 |
| ErpQaRecallTarget（召回对象） | `erp_qa_recall_target.csv` | 3 | P（2+1 按头） | RECALL_ID→ErpQaRecall〔本批〕；PARTNER_ID 可选→ErpMdPartner〔跨域:md·已seed 1/2/3〕；RETURN_STATUS ∈ `erp-qa/recall-target-return-status`（PENDING/NOTIFIED/RETURNED 全谱） |
| ErpQaReview（质量评审） | `erp_qa_review.csv` | 3 | P×2+N-TERM(CANCELLED) | —（无必填 FK）；RELATED_BILL_CODE 软引用既有检验单/召回单 |
| ErpQaRiskRegister（风险登记） | `erp_qa_risk_register.csv` | 3 | P×2+N-TERM(CLOSED) | —（无必填 FK）；OWNER_ID 为 VARCHAR 弱引用（员工 21 语义） |
| ErpQaSamplingPlan（抽样计划） | `erp_qa_sampling_plan.csv` | 2 | P+N-DIS(IS_ACTIVE=false) | —（无必填 FK） |

## FK 闭环图

```
[跨域:md·已seed] erp_md_employee(21 质检员甲)、erp_md_material(1 产品甲/3 原料X)、erp_md_partner(1 华东科技/2 华南贸易/3 北方钢铁)
   ──CALIBRATED_BY/RESPONSIBLE_PERSON_ID/MATERIAL_ID/PARTNER_ID──▶ 校准/目标/召回/召回对象
[已seed·qa] erp_qa_inspection(1/2/3)、erp_qa_non_conformance(1 NCR-2026-001)
   ──INSPECTION_ID/SOURCE_NCR_ID──▶ inspection_line / recall
[本批] erp_qa_inspection_template(1..2) ──TEMPLATE_ID──▶ inspection_template_line
[本批] erp_qa_recall(1..3) ──RECALL_ID──▶ recall_target
```

全部必填 FK 落在〔已seed〕∪〔本批〕集合内，零悬空、白名单零增量（`TestErpSeedDataIntegrity` 全绿背书）。

## 干扰面零漂移设计（本批核验结论）

- **qa 看板/C09 零消费**：`ErpQaDashboardBizModel` 读取面 = inspection（头表）+ action + spc_* + non_conformance（grep 实证）——本批 10 表零进入看板 KPI/SPC 三计数器；`TestErpC09QaNcrCapaScrap` 全链自包含建数（自有 inspection/action/NCR id），种子 inspection_line 行挂 seed 检验单 1/2/3，与 C09 自建 id 零交集。
- **召回处理器链惰性**：`ErpQaRecall*Processor` 仅由召回动作显式触发（E2E `quality-recall-generate-returns.action.spec` 自建召回单），种子行不被扫描。
- 快照机制 `_chgType` 增量记录，显式种子 id < 100000 不入既有快照、不耗 default 序列。

## 与既有 6 表 seed 的衔接（语义一致性约束）

- **检验行与头表结论一致**：inspection_line 3 行分别挂检验单 1（ACCEPTED）/ 2（REJECTED，对应 NCR-2026-001/002 源）/ 3（ACCEPTED），行结果与头表 RESULT 逐行对齐。
- **召回链闭环**：recall id=1（BATCH_NCR_UPGRADE）SOURCE_NCR_ID=NCR-2026-001（头表 2026-07-04 开单，召回 07-07 升级，时序自洽）；recall_target 对象按 partner 1/2 分销 + partner 3 供应商排查，RETURN_STATUS 三态全谱演示。
- **维度值复用既有值域**：组织 2；员工 21（质检员甲）；物料 1/3；往来单位 1/2/3；全部静态日期落在 2026-07 参考期（冻结时钟纪律）。

## 用例指示编码与 negative 行语义

- **P（最小正例行）**：全部 10 表均有。
- **N-TERM（终态行）**：calibration id=3 / quality_goal id=3 / recall id=3 / review id=3 均 `CANCELLED`（取消终态）；risk_register id=3 `CLOSED`（风险关闭终态）。
- **N-DIS（禁用行）**：inspection_template id=2 `IS_ACTIVE=0`；sampling_plan id=2 `IS_ACTIVE=false`（启用前置守卫负路径）。

## 执行期登记（pre-existing ORM quirk，CSV-only 规避）

`ErpQaCalibration.targetValue`/`tolerance` 在运行时 ORM（`_app.orm.xml`，源 `module-quality/model/app-erp-quality.orm.xml` 列声明 `domain="measuredValue"` 覆写）为 `stdDataType=decimal + stdSqlType=VARCHAR`——该实体既往零 seed 零读路径从未暴露；seed 装载后 findAll 物化经 `orm_internalSet` 硬转换对非空值抛 ClassCastException（plan 2026-09-01-1245-3 Phase 1 门禁首跑拦截实证）。**CSV-only 规避**：两列均 nullable，种子 3 行留空。模型修正（`stdSqlType="DECIMAL"` + 重生成 + 种子补值）登记 plan Deferred But Adjudicated 归 successor。28 表全量扫描证实该例外仅此 2 列。

## 约定对齐

- 列头 = DB 列名大写下划线（逐表以 ORM `code=` 为准）；省略审计列；ISO 日期（时间戳列本批未涉及）；小写布尔；ID < 100000；字典码 ∈ `erp-qa/*` + `erp/doc-status` + `wf/approve-status` 字典（逐值核对）。
- 新增/修改本域 seed 时同步履行 `docs/architecture/seed-data.md`「快照重录义务」与 `TestErpSeedDataIntegrity` 门禁（基线常量随批更新协议 190→218）。
