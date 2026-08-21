# M0.1 冻结序与跨域 id 调用点审计（id-string-migration mission）

> 审计日期：2026-08-21
> 计划：`docs/plans/2026-08-21-1045-1-bigint-id-m0-order-freeze-audit-proofs.md`
> 路线图：`docs/backlog/id-string-migration-roadmap.md`（M0.1）
> 配套附录（机器生成清单，file:line 证据）：`docs/audits/2026-08-21-1045-id-migration-m0-cross-domain-coupling-appendix.md`
> 复现工具：`tools/freeze-id-migration-order.mjs`、`tools/verify-id-fix-copy-diff.mjs`、`tools/scan-cross-domain-id-coupling.mjs`、`tools/check-bigint-id-types.mjs`

## 1. 权威基线（08-21 实况，登记为 mission 基线）

| 口径 | 值 | 来源 |
| --- | --- | --- |
| BIGINT PK / BIGINT FK / 需改列 | **477 / 1185 / 1662** | `check-bigint-id-types.mjs dry-run`（19 文件、xmllint 19/19、重扫残留 0、幂等 yes、告警 0、BLOCKER 0） |
| 非 BIGINT 主/外键（不改） | 42 | 同上 |
| 未分类 BIGINT（不改） | 380 | 同上 |
| 逐域 stdDataType-only 变更行（合计=1662） | aps 26 / assets 110 / b2b 40 / contract 47 / crm 126 / cs 68 / drp 55 / **finance 214** / hr 138 / inventory 136 / logistics 32 / maintenance 64 / manufacturing 171 / **master-data 68** / notify 7 / projects 76 / purchase 118 / quality 58 / sales 108 | `verify-id-fix-copy-diff.mjs`（19/19 门控通过，零非 std 差异行） |
| `.getId()`（service main） | 1176 处 / 352 文件（08-16 口径 1026） | 附录 E |
| `Long xxxId` 声明 | 2586 行 / 2896 occurrence | 附录 E |
| E2E `Number(` / `Number(lnk.voucherId)` / `eqFilter('id'` | **874（105 文件）/ 11 / 36** | 附录 F |

08-16 路线图旧口径（463/1142/1605/40/368）已废止——增量来自 requirement-compliance mission 08-18→08-20 落地的 R1.70-R1.88。

## 2. 副本刷新（旧副本过期风险的处置）

对实况源重跑全量 dry-run，重生成 19/19 域副本；逐域 diff 实况源 vs 新副本**全部仅含 stdDataType 差异行**（零非 std 行，合计 1662 行 = 列数）；旧副本 14/19 漂移问题随刷新消除。**回写不得依赖静态副本**（RC mission 持续演进，副本会继续腐化）——见 §7 Decision A 的新鲜度门控机制。

## 3. Maven `-am` test-scope 行为实测（Proof）

- 环境：Apache Maven 3.9.12（848fbb4bf2d427b72bdb2471c22fced7ebd9a7a1）。
- 采样模块：`:app-erp-sales-service`（含 test-scope 上游 qa-service、inv-service、ct-service、notify-service）。
- 两变体（install 语义 `-DskipTests` / test 语义 `-Dmaven.test.skip=true`，均以 validate 采样——**reactor 选择先于 lifecycle 计算，与 goal/phase 无关**）reactor **完全一致 = 49 模块**（38 非 agg + 11 聚合器）。
- 结论：**test 闭包全量入 reactor，且中间模块的 test 边同样被遍历**（fin-codegen 经 common-test→fin-dao 的 test 边进入 sal 的 reactor）。
- 模型校验：冻结脚本 service 锚点闭包与两次实测精确吻合——sal 38+11=49；master-data 12+4=16（service 锚点）/ 15+4=19（api+app 锚点），与 M1.1 计划实测 16/19 完全一致。

## 4. 模块级 DAG 与冻结序

### 4.1 图与锚点

- 解析全部 `module-*` 各层 pom（codegen/api/dao/meta/service/web/app + 聚合器）+ app-erp-all，剥离 dependencyManagement/build/profiles（profiles 已核无 app-erp 依赖）；pom 自身 artifactId 取剥离 `<parent>` 后的首个（否则取到父聚合器）。
- 边 scope 分离：compile 类（compile/runtime/provided）与 test 类分开建模；跨域边共 **289 条**（test 145）。
- **锚点**（对齐 M1.1 计划实测的域级构建命令）：可行性判据取 **service 锚点闭包**（`-pl <域>/erp-<short>-service -am`，域级 build+test 最小单元）；全域命令（`-pl api,app -am`）单独报告 web/app 延后耦合。
- **惰性层**（判据允许出现在闭包中的未迁移模块，逐层实测依据）：`codegen`（main 零手写 Java，仅 postcompile 脚本 + test 运行器 + orm 模型副本）、`meta`（零手写 Java）、`api`（全生成件，`__XGEN_FORCE_OVERRIDE__`）、`web`（main 为 AMIS 数据文件；手写 Java 仅 src/test 本域页面测试）、`dao`（**有手写代码，逐域审计见 §5**）、`app`（本域打包聚合）。阻塞层 = `service`（手写 BizModel/Processor，下游测试以 String id 集成调用其 bean）。基建域（无 orm）：common-service（orgId 语义由 M1.3 在首个 orm 域之前适配）、common-test（实测 5 个 Java 文件零 id/Long 耦合）、app-erp-all。
- 判据（服务锚点 + test 闭包口径，保守正确）：**域 D 可行 ⟺ closure(D) ∩ 未迁移域 ⊆ 惰性层模块 ∪ {自身}**。

### 4.2 DAG 抽样核对（Proof）

抽样 12 条边逐条 grep pom 核实，**12/12 零偏差**（必查边全覆盖）：

| # | 边 | scope | pom 证据 |
| --- | --- | --- | --- |
| 1 | sal-service → qa-service | test | module-sales/erp-sal-service/pom.xml:107-110 |
| 2 | ast-service → fin-service | compile | module-assets/erp-ast-service/pom.xml:53-56 |
| 3 | prj-service → ast-service | compile | module-projects/erp-prj-service/pom.xml:53-56 |
| 4 | prj-service → fin-service | compile | module-projects/erp-prj-service/pom.xml:46-49 |
| 5 | fin-service → inv-dao | compile | module-finance/erp-fin-service/pom.xml:53 |
| 6 | ast-dao → fin-dao | compile | module-assets/erp-ast-dao/pom.xml:41-44 |
| 7 | mfg-service → qa-service | test | module-manufacturing/erp-mfg-service/pom.xml:105-108 |
| 8 | qa-service → sal-meta | test | module-quality/erp-qa-service/pom.xml:60-63 |
| 9 | fin-service → pur-dao | compile | module-finance/erp-fin-service/pom.xml:56 |
| 10 | crm-service → sal-service | test | module-crm/erp-crm-service/pom.xml:122-125 |
| 11 | hr-service → fin-service | compile | module-hr/erp-hr-service/pom.xml:32-35 |
| 12 | drp-service → pur-service | test | module-drp/erp-drp-service/pom.xml:93-96 |

（fin-dao→ast-dao 反向核实为**不存在**——域级 ast↔fin 交叉的真实形态是 ast-dao→fin-dao + fin-service→ast-dao + fin-app→ast-web→ast-service。）

### 4.3 冻结总序（19 域，脚本两次运行输出一致）

`master-data → notify → aps → b2b → contract → finance → assets → cs → hr → inventory → maintenance → projects → quality → manufacturing → purchase → sales → crm → drp → logistics`

每步可行性构成（service 闭包 ∩ 未迁移 = 仅惰性模块）与精确前置（= 闭包内外域 **service** 模块）：

| 位次 | 域 | service 闭包模块数 | 精确前置（外域 service） | 全域构建（api+app 锚点）延后耦合 |
| --- | --- | --- | --- | --- |
| 1 | master-data | 12 | —（根域） | 无 |
| 2 | notify | 12 | —（根域） | 无 |
| 3 | aps | 20 | notify | 无 |
| 4 | b2b | 20 | notify | 无 |
| 5 | contract | 22 | md, notify | 无 |
| 6 | finance | 26 | md, notify | **ast-service、prj-service**（fin-app→ast-web→ast-service、fin-app→prj-web→prj-service）——fin web/app 重生成延后至位次 12 后 |
| 7 | assets | 30 | fin, md, notify | 无 |
| 8 | cs | 35 | fin, md, notify | 无 |
| 9 | hr | 30 | fin, md, notify | 无 |
| 10 | inventory | 30 | fin, md, notify | 无 |
| 11 | maintenance | 36 | fin, inv, md, notify | 无 |
| 12 | projects | 32 | ast, fin, md, notify | 无 |
| 13 | quality | 35 | fin, inv, md, notify | 无 |
| 14 | manufacturing | 41 | fin, inv, mnt, md, notify, qa | 无 |
| 15 | purchase | 47 | ast, ct, fin, inv, md, notify, prj, qa | 无 |
| 16 | sales | 38 | ct, fin, inv, md, notify, qa | 无 |
| 17 | crm | 42 | ct, fin, inv, md, notify, qa, sal | 无 |
| 18 | drp | 50 | ast, ct, fin, inv, md, notify, prj, pur, qa, sal | 无 |
| 19 | logistics | 42 | ct, fin, inv, md, notify, qa, sal | 无 |

闭包内未迁移惰性模块构成（每步清单见脚本输出 `node tools/freeze-id-migration-order.mjs`；均为外域 dao+codegen，md/notify 根域闭包即含 fin/prj/notify 的 dao+codegen——经 common-test optional 依赖拉入，M1.1 计划实测同证）。

## 5. 惰性 dao 逐域结论（19/19 证实，零证伪）

判定口径（判据的编译耦合解释）：dao 手写代码（非 `_gen`）不引用外域 Java 类型于 id 语境（`getId()` 赋 Long / `setXxxId(Long)` / 外域实体 id 的 Long 参数传递）。

| 域 | 跨域手写 import | 结论 |
| --- | --- | --- |
| crm | 3 文件 5 行（IErpCrmLeadBiz:4-5、IErpCrmConversionBiz:4-5、IErpCrmProductConfiguratorBiz:11 → md ErpMdPartner / sal ErpSalQuotation） | **证实**——全部类型级（javadoc/返回类型），无 id 用法 |
| pur | 1 行（IErpPurPaymentBiz:9 → md SettlementAllocation DTO） | **证实**——DTO 引用（invoiceId 为 pur 自身语义，见附录 C watch 项） |
| sal | 3 行（IErpSalReceiptBiz:9 DTO；ErpSalPriceList:4 + ErpSalPriceListLine:4 → md IDateRange） | **证实**——DTO/日期接口，无实体 id 用法 |
| 其余 16 域 | 0 | **证实** |

补充：mfg-dao IErpMfgMrpPlanLineBiz.java:14 有 javadoc `{@link app.erp.pur.dao.entity.ErpPurOrder}`（注释级，非编译依赖）；dao 模块 `daoFor(` 仅 md-dao 2 处本域实体。**无证伪域，冻结序无需调整**（Phase 2 Decision 裁定）。

**语义耦合登记（非证伪、修复预告）**：dao 层本域签名声明 Long 但语义指向他域实体的 FK 参数/字段 **82 处/11 域**（mnt assetId、qa materialId/supplierId/warehouseId、mfg materialId/supplierId/currencyId/sourceWarehouseId、inv periodId、fin GlMappingDimensions DTO 组、aps materialId/routingId、drp/crm/cs/pur/sal 等；清单逐行见附录 C）——编译自洽（惰性成立），**本域迁移时编译器不报错，须按语义陷阱 grep 门控显式翻转**：清单见附录 C。service 层耦合清单（205 文件/289 边，`daoFor(Erp*)`/I*Biz 注入/外域实体引用 + id-as-Long 证据行）见附录 A，各域 plan Phase 2 输入。

## 6. orgId 语义调用点（M1.3 / M2.1 / 各域 plan 归属）

全量清单见附录 D：`ErpOrgContext` 外部调用**全仓唯一** = TestErpOrgIsolation.java:72（fin-service test；**M1.3 落地（2026-08-21）后 `setCurrentOrgId(ctx, 2L)` 编译破坏已发生，为已知中间态产物，successor = M2.1 finance plan**：签名 String 化 + 断言随 fin 实体 orgId 列迁移重录，见 plan `2026-08-21-1045-2` Deferred But Adjudicated）；`setContextAttr(CONTEXT_ATTR_CURRENT_ORG_ID,...)` 写入 3 处（同类 :56/:87/:102，Object 通道非编译级，M1.3 过渡期宽容转换可读通）；`eq("orgId",...)` 业务调用 34 行/13 模块（值全部来自实体 getter 或其派生——随域迁移自洽；inv 测试 1 处 `ORG_ID` 常量登记 inv plan Phase 3）。M1 内部序核验：**md orm 含 6 个 orgId FK 列**（552/872/972/1135/1185/1238 行；1215 行为 `<index>` 成员引用非列定义）→ **M1.3 先于 M1.1 维持**。

## 7. Decisions

**Decision A（回写机制）**：裁定 **(a) 时点 dry-run + 新鲜度门控**。各域 plan 落源三步：① `node tools/check-bigint-id-types.mjs dry-run`（全量幂等刷新，结构性绑定回写时点实况源）；② `node tools/verify-id-fix-copy-diff.mjs module-<domain>`（新增门控脚本：diff 实况源 vs 时点副本零非 stdDataType 行才放行）；③ 单文件落源 + `git diff` 逐行审核。**apply 模式被否**——沙箱实测：apply 后实况源 checksum 不变（E1：根本不写源文件，其「已回写文件」输出误导）；副本由实况源重推导刷新（E2：漂移列 newCol 出现于新副本，非粘贴旧 `_tmp`）；且 apply 跳过 XML 校验/残留重扫、不清理 outRoot 陈旧副本（E3：代码走读 check-bigint-id-types.mjs apply 分支仅写 `_tmp` 路径）。任何路径禁止盲 cp 静态副本。M1.1 计划 Phase 1 已自带同等门控并引用本裁定。

**Decision B（seq-string Proof 载体）**：裁定 **(a) module-common-test 测试专用 orm**，spike 成功。脚手架 3 个实质新文件（`_vfs/erp/tst/orm/app.orm.xml` DynamicOrmEntity 免实体类 proof 模型 + `app/erp/common/test/seq-proof-test.yaml` 测试配置（禁用 erp-fin/erp-notify 模块——optional 依赖的 orm 进 VFS 但外部实体类不在本模块 test classpath）+ `TestSeqStringIdProof.java`）+ 1 个零字节 `_module` VFS 模块标记。spike 判据核对：① 未触发（**零 pom 改动**，nop-autotest-junit/junit/H2 均既有）；③ 未触发（单会话跑通）；② 实质文件 = 3 ≤ 3 未触发（若计零字节 `_module` 标记则 4 > 3——两种计数均如实登记，采信实质内容口径：标记是 `ModuleManager.findAll("*/*/_module")` 的目录哨兵，与 .gitkeep 同性质；判据原意拦截脚手架膨胀/pom 污染，本载体 3 文件全 test-only 零生产影响）。**无需登记 Proof 语义变化**（未降级 (b)；M1.1 Phase 3 硬退出标准仍保留为双保险）。

**Decision C（惰性 dao 证伪处置）**：无证伪域，不适用路线图规则 6，冻结序维持。

## 8. Proofs 结论

1. **seq-string 行为**（载体 (a)，`mvn test -pl module-common-test` **4/4 绿**）：
   - `testSeqGeneratedIdIsNonEmptyString`：无显式 id 保存 → `orm_id()` **instanceof String 且非空** + 跨 session 回读一致；
   - `testExplicitNumericLongIdCoercedToString`（`orm_propValue(1, 5L)`）→ 存活且 String "5" + 回读一致；
   - `testExplicitNumericStringIdSurvives`（`prop_set("id","7")`）→ 存活且 String "7" + 回读一致；
   - `testFkShapedBigIntStringColumnRoundTrip`：BIGINT+string FK 形态列 `refId` 值 String 往返。
   平台方案 B（`orm-model-design.md` §主键设计）三断言在**迁移前**获得仓内实证。踩坑记录（M1.1 复用）：orm 根需 `<entities>` 包裹；ClassPathResource 须 `classpath:` 前缀；module name 为 `/`→`-` 形态；同 session 查询不见未 flush 插入——回读断言用独立 `runInSession`。
2. **E2E 影响面清单**：附录 F（`Number(` 874/105 文件、`Number(lnk.voucherId)` 11 精确不变、`eqFilter('id'` 36、简单形态 id 族 234 vs 数值族 523 + 复合形态 117 逐个判定、目录分布 business-actions 764、`String(id)` 正向兼容 27 处、复核命令）。

## 9. 遗留与移交

- finance web/app 重生成延后（M2.1 注记 + M4.1 核验）；
- `eq("orgId")` 值类型随域自洽（watch-only，各域 plan Phase 2/4 门控）；
- E2E 修复统一 M4.1（附录 F）；
- SettlementAllocation.invoiceId DTO 字段（md 托管、pur/sal 语义）——pur/sal plan 登记；
- 路线图 08-16 旧计数口径已废止并刷新（含共识行笔误修正）。
