package app.erp.mfg.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgBomOperation;
import app.erp.mfg.dao.entity.ErpMfgCostRollup;
import app.erp.mfg.dao.entity.ErpMfgCostRollupLine;
import app.erp.mfg.dao.entity.ErpMfgCostVariance;
import app.erp.mfg.dao.entity.ErpMfgJobCardTimeLog;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkcenter;
import app.erp.mfg.service.costing.ProductionVarianceCalculator;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 4 行为测试：生产成本差异分析引擎（plan 2026-07-05-1838-2）。
 *
 * <p>覆盖：材料用量差异、人工效率/费率差异、制造费用差异、产量差异（完工≠计划）、完工触发 config-gated、
 * 手动 calculateVariances 幂等、差异过账凭证生成 + posted 标志、无标准成本抛 {@code ERR_VARIANCE_NO_STANDARD_COST}。
 *
 * <p>权威：{@code docs/design/manufacturing/variance-analysis.md}。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgProductionVariance extends JunitAutoTestCase {

    static final Long ORG_ID = 1401L;
    static final Long UOM_ID = 5401L;
    static final Long CURRENCY_ID = 6401L;
    static final Long WC1 = 6201L;
    static final String PERIOD_CODE = "2026-07";
    static final String VOUCHER_STATUS_POSTED = "POSTED";

    static final String SUBJECT_MATERIAL_VARIANCE = "1410";
    static final String SUBJECT_WIP_MATERIAL = "1411";
    static final String SUBJECT_LABOR_VARIANCE = "1412";
    static final String SUBJECT_WIP_LABOR = "1413";
    static final String SUBJECT_OVERHEAD_VARIANCE = "1414";
    static final String SUBJECT_WIP_OVERHEAD = "1415";

    static final Long P = 1201L;
    static final Long ACCT_SCHEMA_ID = 7401L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    ProductionVarianceCalculator productionVarianceCalculator;

    @Test
    public void testMaterialLaborOverheadVarianceHappyPath() {
        seedProduct(P);
        seedWorkcenter(WC1, bd("20"));
        Long bomId = seedBom(9201L, P);
        seedBomOperation(4201L, bomId, WC1, bd("60"));
        seedFirmedRollup(P, bd("10"), bd("10"), bd("5"), bd("25"));
        seedPeriodAndSubjects();

        ErpMfgWorkOrder wo = seedCompletedWorkOrder(8201L, "WO-PV-HAPPY", bomId, P,
                bd("2"), bd("2"), bd("25"), bd("35"), bd("8"));
        seedTimeLog(5601L, 8201L, bd("150"));

        productionVarianceCalculator.calculateVariances(8201L);

        List<ErpMfgCostVariance> lines = productionVarianceCalculator.findByWorkOrder(8201L);
        assertEquals(5, lines.size(), "5 类差异行（材料/效率/费率/制造/产量）");

        ErpMfgCostVariance material = lineByType(lines, ErpMfgConstants.VARIANCE_TYPE_MATERIAL_USAGE);
        assertEquals(ErpMfgConstants.COST_ELEMENT_MATERIAL, material.getCostElement());
        assertEquals(0, bd("20").compareTo(material.getStandardAmount()), "标准材料 = 10×2 = 20");
        assertEquals(0, bd("25").compareTo(material.getActualAmount()), "实际材料 = 25");
        assertEquals(0, bd("5").compareTo(material.getVarianceAmount()), "用量差异 = 25-20 = +5（unfavorable）");

        ErpMfgCostVariance eff = lineByType(lines, ErpMfgConstants.VARIANCE_TYPE_LABOR_EFFICIENCY);
        // 标准工时 60×2=120min；实际 150min；stdHourlyRate=20（WC1）
        // actualAtStdRate = 150/60×20 = 50；stdLaborTotal = 10×2 = 20
        assertEquals(0, bd("20").compareTo(eff.getStandardAmount()), "效率差异标准 = 10×2 = 20");
        assertEquals(0, bd("50").compareTo(eff.getActualAmount()), "效率差异实际 = 150/60×20 = 50");
        assertEquals(0, bd("30").compareTo(eff.getVarianceAmount()), "效率差异 = 50-20 = +30");

        ErpMfgCostVariance rate = lineByType(lines, ErpMfgConstants.VARIANCE_TYPE_LABOR_RATE);
        // 费率残差：actLabor(35) − actualAtStdRate(50) = -15
        assertEquals(0, bd("50").compareTo(rate.getStandardAmount()), "费率差异标准 = actualAtStdRate 50");
        assertEquals(0, bd("35").compareTo(rate.getActualAmount()), "费率差异实际 = wo.laborCost 35");
        assertEquals(0, bd("-15").compareTo(rate.getVarianceAmount()), "费率差异 = 35-50 = -15（favorable）");

        ErpMfgCostVariance oh = lineByType(lines, ErpMfgConstants.VARIANCE_TYPE_OVERHEAD);
        assertEquals(0, bd("10").compareTo(oh.getStandardAmount()), "标准制造费用 = 5×2 = 10");
        assertEquals(0, bd("8").compareTo(oh.getActualAmount()), "实际制造费用 = 8");
        assertEquals(0, bd("-2").compareTo(oh.getVarianceAmount()), "制造费用差异 = 8-10 = -2");

        ErpMfgCostVariance vol = lineByType(lines, ErpMfgConstants.VARIANCE_TYPE_VOLUME);
        // 完工=计划=2 → 产量差异为 0
        assertEquals(0, bd("50").compareTo(vol.getStandardAmount()), "产量标准 = 2×25 = 50");
        assertEquals(0, bd("50").compareTo(vol.getActualAmount()), "产量实际 = 2×25 = 50");
        assertEquals(0, BigDecimal.ZERO.compareTo(vol.getVarianceAmount()), "产量差异 = 0（完工=计划）");
    }

    @Test
    public void testVolumeVarianceWhenCompletedDiffersFromPlanned() {
        seedProduct(P);
        seedWorkcenter(WC1, bd("20"));
        Long bomId = seedBom(9202L, P);
        seedBomOperation(4202L, bomId, WC1, bd("60"));
        seedFirmedRollup(P, bd("10"), bd("10"), bd("5"), bd("25"));
        seedPeriodAndSubjects();

        // planned=3, completed=2 → COMPLETED 手工置位（模拟手动 calculateVariances 入口）
        ErpMfgWorkOrder wo = seedCompletedWorkOrder(8202L, "WO-PV-VOL", bomId, P,
                bd("3"), bd("2"), bd("20"), bd("20"), bd("10"));
        seedTimeLog(5602L, 8202L, bd("120"));

        productionVarianceCalculator.calculateVariances(8202L);

        List<ErpMfgCostVariance> lines = productionVarianceCalculator.findByWorkOrder(8202L);
        ErpMfgCostVariance vol = lineByType(lines, ErpMfgConstants.VARIANCE_TYPE_VOLUME);
        // 产量差异 = (completed - planned) × stdUnit = (2-3)×25 = -25
        assertEquals(0, bd("75").compareTo(vol.getStandardAmount()), "产量标准 = planned(3)×25 = 75");
        assertEquals(0, bd("50").compareTo(vol.getActualAmount()), "产量实际 = completed(2)×25 = 50");
        assertEquals(0, bd("-25").compareTo(vol.getVarianceAmount()), "产量差异 = 50-75 = -25");
    }

    @Test
    public void testNoStandardCostThrows() {
        seedProduct(P);
        Long bomId = seedBom(9203L, P);
        seedCompletedWorkOrder(8203L, "WO-PV-NOSTD", bomId, P,
                bd("1"), bd("1"), bd("10"), BigDecimal.ZERO, BigDecimal.ZERO);
        // 无 FIRMED rollup

        NopException ex = assertThrows(NopException.class,
                () -> productionVarianceCalculator.calculateVariances(8203L));
        assertEquals(ErpMfgErrors.ERR_VARIANCE_NO_STANDARD_COST.getErrorCode(), ex.getCode(),
                "无 FIRMED 标准成本抛 ERR_VARIANCE_NO_STANDARD_COST");
    }

    @Test
    public void testManualCalculateVariancesIdempotent() {
        seedProduct(P);
        seedWorkcenter(WC1, bd("20"));
        Long bomId = seedBom(9204L, P);
        seedBomOperation(4204L, bomId, WC1, bd("60"));
        seedFirmedRollup(P, bd("10"), bd("10"), bd("5"), bd("25"));
        seedPeriodAndSubjects();
        seedCompletedWorkOrder(8204L, "WO-PV-IDEM", bomId, P,
                bd("2"), bd("2"), bd("22"), bd("30"), bd("6"));
        seedTimeLog(5604L, 8204L, bd("140"));

        // 第一次手动计算（经 GraphQL BizModel 入口）
        ApiResponse<?> r1 = executeRpc(mutation, "ErpMfgCostVariance__calculateVariances",
                ApiRequest.build(Map.of("workOrderId", 8204L)));
        assertEquals(0, r1.getStatus(), "第一次 calculateVariances 应成功: " + r1);
        int count1 = productionVarianceCalculator.findByWorkOrder(8204L).size();

        // 第二次重算 → 旧行删除再重算，行数不变
        ApiResponse<?> r2 = executeRpc(mutation, "ErpMfgCostVariance__calculateVariances",
                ApiRequest.build(Map.of("workOrderId", 8204L)));
        assertEquals(0, r2.getStatus(), "第二次 calculateVariances（幂等）应成功: " + r2);
        int count2 = productionVarianceCalculator.findByWorkOrder(8204L).size();
        assertEquals(count1, count2, "重算幂等：行数不变（先删旧再重算）");
    }

    @Test
    public void testPostingVoucherGeneratedAndPostedFlagSet() {
        seedProduct(P);
        seedWorkcenter(WC1, bd("20"));
        Long bomId = seedBom(9205L, P);
        seedBomOperation(4205L, bomId, WC1, bd("60"));
        seedFirmedRollup(P, bd("10"), bd("10"), bd("5"), bd("25"));
        seedPeriodAndSubjects();

        seedCompletedWorkOrder(8205L, "WO-PV-POST", bomId, P,
                bd("2"), bd("2"), bd("25"), bd("35"), bd("8"));
        seedTimeLog(5605L, 8205L, bd("150"));

        // 经 GraphQL BizModel 入口（calculateVariances 内部调 dispatcher 过账）
        // GraphQL @BizMutation 事务在独立 session 中执行并提交，过账凭证经 REQUIRES_NEW 也独立提交
        ApiResponse<?> resp = executeRpc(mutation, "ErpMfgCostVariance__calculateVariances",
                ApiRequest.build(Map.of("workOrderId", 8205L)));
        assertEquals(0, resp.getStatus(), "calculateVariances（含过账）应成功: " + resp);

        ErpFinVoucher voucher = findVoucherByBillCode("WO-PV-POST-PV");
        assertNotNull(voucher, "生产差异凭证应生成（billHeadCode = woCode-PV）");
        assertEquals(VOUCHER_STATUS_POSTED, voucher.getDocStatus(), "凭证已过账");

        // 净差异：材料 +5（DEBIT）/ 人工 +15（DEBIT）/ 制造费用 -2（CREDIT）
        ErpFinVoucherLine matVarLine = findVoucherLine(voucher.getId(), SUBJECT_MATERIAL_VARIANCE);
        assertNotNull(matVarLine, "材料差异科目行存在");
        assertEquals("DEBIT", matVarLine.getDcDirection(), "材料差异 unfavorable → 借方");
        assertEquals(0, bd("5").compareTo(matVarLine.getDebitAmount()), "材料差异借方金额 = 5");

        ErpFinVoucherLine laborVarLine = findVoucherLine(voucher.getId(), SUBJECT_LABOR_VARIANCE);
        assertNotNull(laborVarLine, "人工差异科目行存在");
        assertEquals("DEBIT", laborVarLine.getDcDirection(), "人工差异 unfavorable → 借方");
        assertEquals(0, bd("15").compareTo(laborVarLine.getDebitAmount()), "人工差异净额借方 = 30-15 = 15");

        ErpFinVoucherLine ohVarLine = findVoucherLine(voucher.getId(), SUBJECT_OVERHEAD_VARIANCE);
        assertNotNull(ohVarLine, "制造费用差异科目行存在");
        assertEquals("CREDIT", ohVarLine.getDcDirection(), "制造费用差异 favorable → 贷方");
        assertEquals(0, bd("2").compareTo(ohVarLine.getCreditAmount()), "制造费用差异贷方金额 = 2");

        // posted 标志回写
        List<ErpMfgCostVariance> lines = productionVarianceCalculator.findByWorkOrder(8205L);
        assertTrue(lines.stream().allMatch(l -> Boolean.TRUE.equals(l.getPosted())),
                "全部差异行 posted=true");
    }

    @Test
    public void testCompletionTriggerAutoCalcConfigGated() {
        seedProduct(P);
        seedWorkcenter(WC1, bd("20"));
        Long bomId = seedBom(9206L, P);
        seedBomOperation(4206L, bomId, WC1, bd("60"));
        seedFirmedRollup(P, bd("10"), bd("10"), bd("5"), bd("25"));
        seedPeriodAndSubjects();

        // config 关（默认）→ 完工不自动算差异
        setVarianceAutoCalc(false);
        try {
            Long woIdOff = seedInProcessWorkOrder(8206L, "WO-PV-OFF", bomId, P,
                    bd("2"), bd("25"), bd("35"), bd("8"));
            seedTimeLog(5606L, woIdOff, bd("150"));
            Map<String, Object> req = new LinkedHashMap<>();
            req.put("workOrderId", woIdOff);
            req.put("completedQty", bd("2"));
            ApiResponse<?> resp = executeRpc(mutation, "ErpMfgWorkOrder__reportCompletion", ApiRequest.build(req));
            assertEquals(0, resp.getStatus(), "完工应成功: " + resp);
            assertTrue(productionVarianceCalculator.findByWorkOrder(woIdOff).isEmpty(),
                    "config 关 → 完工不触发差异计算");
        } finally {
            setVarianceAutoCalc(false);
        }

        // config 开 → 完工自动算差异
        Long woIdOn = seedInProcessWorkOrder(8207L, "WO-PV-ON", bomId, P,
                bd("2"), bd("25"), bd("35"), bd("8"));
        seedTimeLog(5607L, woIdOn, bd("150"));
        setVarianceAutoCalc(true);
        try {
            Map<String, Object> req = new LinkedHashMap<>();
            req.put("workOrderId", woIdOn);
            req.put("completedQty", bd("2"));
            ApiResponse<?> resp = executeRpc(mutation, "ErpMfgWorkOrder__reportCompletion", ApiRequest.build(req));
            assertEquals(0, resp.getStatus(), "完工应成功: " + resp);

            ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woIdOn);
            assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED, wo.getDocStatus(), "完工达量 → COMPLETED");

            List<ErpMfgCostVariance> lines = productionVarianceCalculator.findByWorkOrder(woIdOn);
            assertFalse(lines.isEmpty(), "config 开 → 完工自动触发差异计算");
            assertEquals(5, lines.size(), "5 类差异行");
        } finally {
            setVarianceAutoCalc(false);
        }
    }

    @Test
    public void testManualCalculateRejectsNonCompleted() {
        seedProduct(P);
        Long bomId = seedBom(9207L, P);
        seedFirmedRollup(P, bd("10"), bd("10"), bd("5"), bd("25"));
        // IN_PROCESS 工单（未完工）
        seedInProcessWorkOrder(8208L, "WO-PV-NOCOMP", bomId, P,
                bd("2"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        ApiResponse<?> resp = executeRpc(mutation, "ErpMfgCostVariance__calculateVariances",
                ApiRequest.build(Map.of("workOrderId", 8208L)));
        assertEquals(ErpMfgErrors.ERR_VARIANCE_WORKORDER_NOT_COMPLETED.getErrorCode(), resp.getCode(),
                "非 COMPLETED 工单手动计算拒绝 ERR_VARIANCE_WORKORDER_NOT_COMPLETED");
    }

    // ---------- query helpers ----------

    private ErpMfgCostVariance lineByType(List<ErpMfgCostVariance> lines, String type) {
        return lines.stream().filter(l -> type.equals(l.getVarianceType())).findFirst()
                .orElseThrow(() -> new AssertionError("no variance line of type " + type));
    }

    private ErpFinVoucher findVoucherByBillCode(String billCode) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", billCode));
        List<ErpFinVoucherBillR> links = dao.findAllByQuery(q);
        if (links.isEmpty()) {
            return null;
        }
        return daoProvider.daoFor(ErpFinVoucher.class).getEntityById(links.get(0).getVoucherId());
    }

    private ErpFinVoucherLine findVoucherLine(Long voucherId, String subjectCode) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        q.addFilter(eq("subjectCode", subjectCode));
        List<ErpFinVoucherLine> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private void setVarianceAutoCalc(boolean value) {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpMfgConstants.CONFIG_VARIANCE_AUTO_CALC_ENABLED, String.valueOf(value));
    }

    // ---------- seed helpers ----------

    private void seedProduct(Long id) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            ErpMdMaterial m = new ErpMdMaterial();
            m.orm_propValueByName("id", id);
            m.setCode("MAT-" + id);
            m.setName("Product " + id);
            m.orm_propValueByName("materialType", "GOODS");
            m.setUoMId(UOM_ID);
            m.setStatus("ACTIVE");
            dao.saveEntity(m);
        });
    }

    private void seedWorkcenter(Long id, BigDecimal hourlyRate) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkcenter> dao = daoProvider.daoFor(ErpMfgWorkcenter.class);
            ErpMfgWorkcenter wc = new ErpMfgWorkcenter();
            wc.orm_propValueByName("id", id);
            wc.setCode("WC-" + id);
            wc.setName("Workcenter " + id);
            wc.setHourlyRate(hourlyRate);
            dao.saveEntity(wc);
        });
    }

    private Long seedBom(Long id, Long productId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgBom> dao = daoProvider.daoFor(ErpMfgBom.class);
            ErpMfgBom bom = new ErpMfgBom();
            bom.orm_propValueByName("id", id);
            bom.setCode("BOM-" + id);
            bom.setProductId(productId);
            bom.setBomType(ErpMfgConstants.BOM_TYPE_MANUFACTURED);
            bom.setIsDefault(Boolean.TRUE);
            bom.setIsActive(Boolean.TRUE);
            bom.setQty(bd("1"));
            dao.saveEntity(bom);
        });
        return id;
    }

    private void seedBomOperation(Long id, Long bomId, Long workcenterId, BigDecimal standardTime) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgBomOperation> dao = daoProvider.daoFor(ErpMfgBomOperation.class);
            ErpMfgBomOperation op = new ErpMfgBomOperation();
            op.orm_propValueByName("id", id);
            op.setBomId(bomId);
            op.setLineNo(10);
            op.setOperationId(9000L);
            op.setWorkcenterId(workcenterId);
            op.setStandardTime(standardTime);
            dao.saveEntity(op);
        });
    }

    private void seedFirmedRollup(Long productId, BigDecimal materialCost, BigDecimal laborCost,
                                  BigDecimal overheadCost, BigDecimal unitCost) {
        ormTemplate.runInSession(() -> {
            Long headerId = productId * 10000 + 1;
            IEntityDao<ErpMfgCostRollup> headerDao = daoProvider.daoFor(ErpMfgCostRollup.class);
            ErpMfgCostRollup header = new ErpMfgCostRollup();
            header.orm_propValueByName("id", headerId);
            header.setCode("ROLLUP-" + productId);
            header.setOrgId(ORG_ID);
            header.setBusinessDate(LocalDate.of(2026, 6, 1));
            header.orm_propValueByName("status", ErpMfgConstants.COST_ROLLUP_STATUS_FIRMED);
            headerDao.saveEntity(header);

            IEntityDao<ErpMfgCostRollupLine> lineDao = daoProvider.daoFor(ErpMfgCostRollupLine.class);
            ErpMfgCostRollupLine line = new ErpMfgCostRollupLine();
            line.orm_propValueByName("id", productId * 10000 + 2);
            line.setCostRollupId(headerId);
            line.setLineNo(10);
            line.setMaterialId(productId);
            line.setUoMId(UOM_ID);
            line.setMaterialCost(materialCost);
            line.setLaborCost(laborCost);
            line.setOverheadCost(overheadCost);
            line.setUnitCost(unitCost);
            line.setTotalCost(unitCost);
            line.setCurrencyId(CURRENCY_ID);
            lineDao.saveEntity(line);
        });
    }

    private ErpMfgWorkOrder seedCompletedWorkOrder(Long id, String code, Long bomId, Long productId,
                                                   BigDecimal planned, BigDecimal completed,
                                                   BigDecimal materialCost, BigDecimal laborCost,
                                                   BigDecimal overheadCost) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
            wo.orm_propValueByName("id", id);
            wo.setCode(code);
            wo.setProductId(productId);
            wo.setBomId(bomId);
            wo.setOrgId(ORG_ID);
            wo.setCurrencyId(CURRENCY_ID);
            wo.setPlannedQuantity(planned);
            wo.setCompletedQuantity(completed);
            wo.setMaterialCost(materialCost);
            wo.setLaborCost(laborCost);
            wo.setOverheadCost(overheadCost);
            wo.setBusinessDate(LocalDate.of(2026, 7, 1));
            wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED);
            dao.saveEntity(wo);
        });
        return daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(id);
    }

    private Long seedInProcessWorkOrder(Long id, String code, Long bomId, Long productId,
                                        BigDecimal planned, BigDecimal materialCost,
                                        BigDecimal laborCost, BigDecimal overheadCost) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
            wo.orm_propValueByName("id", id);
            wo.setCode(code);
            wo.setProductId(productId);
            wo.setBomId(bomId);
            wo.setOrgId(ORG_ID);
            wo.setCurrencyId(CURRENCY_ID);
            wo.setPlannedQuantity(planned);
            wo.setCompletedQuantity(BigDecimal.ZERO);
            wo.setMaterialCost(materialCost);
            wo.setLaborCost(laborCost);
            wo.setOverheadCost(overheadCost);
            wo.setBusinessDate(LocalDate.of(2026, 7, 1));
            wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS);
            dao.saveEntity(wo);
        });
        return id;
    }

    private void seedTimeLog(Long id, Long workOrderId, BigDecimal durationMins) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgJobCardTimeLog> dao = daoProvider.daoFor(ErpMfgJobCardTimeLog.class);
            ErpMfgJobCardTimeLog log = new ErpMfgJobCardTimeLog();
            log.orm_propValueByName("id", id);
            log.setJobCardId(9001L);
            log.setWorkOrderId(workOrderId);
            log.setOperatorId("OP-001");
            log.setWorkDate(LocalDate.of(2026, 7, 1));
            log.setDurationMins(durationMins);
            dao.saveEntity(log);
        });
    }

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdAcctSchema> asDao = daoProvider.daoFor(ErpMdAcctSchema.class);
            ErpMdAcctSchema acctSchema = new ErpMdAcctSchema();
            acctSchema.orm_propValueByName("id", ACCT_SCHEMA_ID);
            acctSchema.setCode("ACCT-" + ORG_ID);
            acctSchema.setName("账套 " + ORG_ID);
            acctSchema.setOrgId(ORG_ID);
            acctSchema.orm_propValueByName("nature", "FINANCIAL");
            acctSchema.setFunctionalCurrencyId(CURRENCY_ID);
            acctSchema.orm_propValueByName("status", "ACTIVE");
            asDao.saveEntity(acctSchema);

            IEntityDao<ErpFinAccountingPeriod> pdao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
            ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
            period.setCode(PERIOD_CODE);
            period.setName(PERIOD_CODE);
            period.setOrgId(ORG_ID);
            period.orm_propValueByName("year", 2026);
            period.orm_propValueByName("month", 7);
            period.setStartDate(LocalDate.of(2026, 7, 1));
            period.setEndDate(LocalDate.of(2026, 7, 31));
            period.orm_propValueByName("status", "OPEN");
            pdao.saveEntity(period);

            seedSubject(SUBJECT_MATERIAL_VARIANCE, "制造差异-材料", "ASSET", "DEBIT");
            seedSubject(SUBJECT_WIP_MATERIAL, "在制品-材料", "ASSET", "DEBIT");
            seedSubject(SUBJECT_LABOR_VARIANCE, "制造差异-人工", "ASSET", "DEBIT");
            seedSubject(SUBJECT_WIP_LABOR, "在制品-人工", "ASSET", "DEBIT");
            seedSubject(SUBJECT_OVERHEAD_VARIANCE, "制造差异-制造费用", "ASSET", "DEBIT");
            seedSubject(SUBJECT_WIP_OVERHEAD, "在制品-制造费用", "ASSET", "DEBIT");
        });
    }

    private void seedSubject(String code, String name, String subjectClass, String direction) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.orm_propValueByName("subjectClass", subjectClass);
        subject.orm_propValueByName("direction", direction);
        subject.orm_propValueByName("status", "ACTIVE");
        dao.saveEntity(subject);
    }

    // ---------- misc helpers ----------

    private ApiResponse<?> executeRpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action,
                                      ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
