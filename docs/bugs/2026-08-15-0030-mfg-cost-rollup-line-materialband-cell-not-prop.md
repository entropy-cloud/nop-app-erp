# ErpMfgCostRollupLine 页面 materialBand cell-not-prop：全 reactor mvn test 预存在失败

## 症状

- 全 reactor `mvn test` 时 `app-erp-all` FAILURE，2 个测试类失败：
  - `ErpAllWebPagesTest.testValidateAllPages`
  - `ErpAllFluxPagesTest.testAllPagesRenderInFluxMode`
- 错误：`FLUX_PAGE_ERR /erp/mfg/pages/ErpMfgCostRollupLine/main.page.yaml` + `picker.page.yaml`，`nop.err.xui.form.cell-not-prop`，`formId=view, cellId=materialBand`，「表单[view]的字段[materialBand]不是已定义的实体属性」@ `ErpMfgCostRollupLine.view.xml:34`。
- `materialBand` 字段在 `ErpMfgCostRollupLine` 实体上不存在（页面/实体字段不匹配）。

## 复现

- `mvn test -pl app-erp-all -Dtest=ErpAllFluxPagesTest -Dsurefire.failIfNoSpecifiedTests=false`（失败）
- 或全 reactor `mvn test`（app-erp-all 在 reactor 末尾暴露）

## 排除

- **本计划（RC-R1.25 crm Forecast territory tier，plan 2026-08-14-2304-1）无关**：`git stash` 回退全部未提交变更 → clean HEAD 复跑 `ErpAllFluxPagesTest` **同样失败**（FLUX_PAGE_ERROR_COUNT: 2，同一 materialBand 错误）。变更仅触 `module-crm/erp-crm-service`，零 mfg/页面文件触及。
- `ErpMfgCostRollupLine.view.xml` 最后修改 = `452e418d0`（2026-08-11，permissions-enforcement plan-2026-08-11-0915 E4.1 采购保密字段级可见性）——非本计划引入。
- 另一全 reactor 失败 `TestAuthSeedLoadingProof`（OrmTransactionListener NPE）已另立 bug 注记 `docs/bugs/2026-08-14-0930-authseed-loading-npe-ormtransactionlistener.md`（nop-entropy lazy-property 回归，外部仓库 ask-first）。

## 影响与处置

- 影响：全 reactor `mvn test` 无法全绿；模块级 `-pl` 验证不受影响（erp-crm-service 180 tests 全绿）。
- 处置：登记为预存在已知失败（非本计划回归）。修复归属 mfg 域页面/实体字段对齐（`materialBand` 移除或实体补字段），待独立计划裁决（触 mfg web view.xml + 可能的实体字段语义），不在 RC-R1.25 范围内。
