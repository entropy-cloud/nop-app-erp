package app.erp.drp.service;

import app.erp.drp.dao.entity.ErpDrpParameter;
import app.erp.drp.dao.entity.ErpInvDrpSafetyStockCalc;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdWarehouse;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
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
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 5 测试：DRP 安全库存优化引擎。
 *
 * <p>覆盖 {@code docs/design/drp/safety-stock-optimization.md}：STATISTICAL/SIMPLE/DDMRP 三法计算、
 * Z 值映射、历史不足降级 SIMPLE、优先级解析（override > calculated > parameter）、人工回写门 confirmWriteback。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpDrpSafetyStock extends JunitAutoTestCase {

    static final Long ORG_ID = 5401L;
    static final Long UOM_ID = 5501L;
    static final Long WH_ID = 5101L;
    static final Long M_STAT = 5201L;  // STATISTICAL 路径
    static final Long M_SIMPLE = 5202L; // 历史不足降级 SIMPLE

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testStatisticalCalculation() {
        seedMaterial(M_STAT);
        seedWarehouse();
        seedParameter(M_STAT, bd("50"), 15);

        // 提供 3 个月历史出库（满足 historyMonths=3 的最小样本要求 ≥2）
        seedOutboundMove(M_STAT, "SM-1", LocalDate.of(2026, 4, 10), bd("1200"));
        seedOutboundMove(M_STAT, "SM-2", LocalDate.of(2026, 5, 10), bd("980"));
        seedOutboundMove(M_STAT, "SM-3", LocalDate.of(2026, 6, 10), bd("1500"));

        Long calcId = seedCalc(M_STAT, ErpDrpConstants.SS_METHOD_STATISTICAL,
                ErpDrpConstants.SERVICE_LEVEL_PCT95, 3, 15);

        calculateOk(calcId);

        ErpInvDrpSafetyStockCalc calc = daoProvider.daoFor(ErpInvDrpSafetyStockCalc.class).getEntityById(calcId);
        assertNotNull(calc.getCalculatedSafetyStock(), "STATISTICAL 应产出 calculatedSafetyStock");
        assertTrue(calc.getCalculatedSafetyStock().signum() > 0, "安全库存应 > 0");
        assertNotNull(calc.getCalculatedRop(), "应产出 calculatedRop");
        assertTrue(calc.getCalculatedRop().compareTo(calc.getCalculatedSafetyStock()) > 0,
                "ROP 应 > 安全库存（含提前期需求）");
        assertNotNull(calc.getLastCalculatedAt(), "应回写 lastCalculatedAt");
    }

    @Test
    public void testInsufficientHistoryDowngradesToSimple() {
        seedMaterial(M_SIMPLE);
        seedWarehouse();
        seedParameter(M_SIMPLE, bd("30"), 10);

        // 仅提供 1 个月历史（< 2 个月，STATISTICAL 不足 → 降级 SIMPLE）
        seedOutboundMove(M_SIMPLE, "SM-S1", LocalDate.of(2026, 6, 10), bd("300"));

        Long calcId = seedCalc(M_SIMPLE, ErpDrpConstants.SS_METHOD_STATISTICAL,
                ErpDrpConstants.SERVICE_LEVEL_PCT95, 3, 10);

        calculateOk(calcId); // 降级后应成功

        ErpInvDrpSafetyStockCalc calc = daoProvider.daoFor(ErpInvDrpSafetyStockCalc.class).getEntityById(calcId);
        assertEquals(ErpDrpConstants.SS_METHOD_SIMPLE, calc.getMethod(), "历史不足应降级 SIMPLE");
        assertNotNull(calc.getCalculatedSafetyStock());
        assertTrue(calc.getCalculatedSafetyStock().signum() >= 0);
    }

    @Test
    public void testSimpleCalculation() {
        seedMaterial(M_STAT);
        seedWarehouse();
        seedParameter(M_STAT, bd("40"), 10);

        seedOutboundMove(M_STAT, "SM-SIM-1", LocalDate.of(2026, 6, 10), bd("600"));

        Long calcId = seedCalc(M_STAT, ErpDrpConstants.SS_METHOD_SIMPLE,
                ErpDrpConstants.SERVICE_LEVEL_PCT95, 3, 10);
        calculateOk(calcId);

        ErpInvDrpSafetyStockCalc calc = daoProvider.daoFor(ErpInvDrpSafetyStockCalc.class).getEntityById(calcId);
        assertNotNull(calc.getCalculatedSafetyStock());
        // SIMPLE: SS = μ_d × leadTime × 0.5 = (600/30) × 10 × 0.5 = 20 × 10 × 0.5 = 100
        assertEquals(0, calc.getCalculatedSafetyStock().compareTo(bd("100.00")),
                "SIMPLE 法 SS = μ_d(20) × leadTime(10) × safetyFactor(0.5) = 100");
    }

    @Test
    public void testDdmrpCalculation() {
        seedMaterial(M_STAT);
        seedWarehouse();
        seedParameter(M_STAT, bd("40"), 10);

        seedOutboundMove(M_STAT, "SM-DD-1", LocalDate.of(2026, 6, 10), bd("900"));

        Long calcId = seedCalc(M_STAT, ErpDrpConstants.SS_METHOD_DDMRP,
                ErpDrpConstants.SERVICE_LEVEL_PCT95, 3, 10);
        calculateOk(calcId);

        ErpInvDrpSafetyStockCalc calc = daoProvider.daoFor(ErpInvDrpSafetyStockCalc.class).getEntityById(calcId);
        // DDMRP: SS = μ_d × (leadTime + variabilityDays + orderCycle) = (900/30) × (10+3+2) = 30 × 15 = 450
        assertEquals(0, calc.getCalculatedSafetyStock().compareTo(bd("450.00")),
                "DDMRP 法 SS = μ_d(30) × (10+3+2) = 450");
    }

    @Test
    public void testFindEffectiveSafetyStockPriority() {
        seedMaterial(M_STAT);
        seedWarehouse();
        // parameter.safetyStock = 50
        ErpDrpParameter param = seedParameter(M_STAT, bd("50"), 15);

        // 无 calc 记录 → 返回 parameter.safetyStock = 50
        BigDecimal level1 = findEffectiveSafetyStock(param.getId());
        assertEquals(0, level1.compareTo(bd("50")), "无 calc 时取 parameter.safetyStock");

        // 有 calc，calculatedSafetyStock=80，无 override → 取 calculated = 80
        Long calcId = seedCalc(M_STAT, ErpDrpConstants.SS_METHOD_SIMPLE,
                ErpDrpConstants.SERVICE_LEVEL_PCT95, 3, 15);
        setCalcResult(calcId, bd("80"), bd("100"));

        BigDecimal level2 = findEffectiveSafetyStock(param.getId());
        assertEquals(0, level2.compareTo(bd("80")), "calculated > parameter 时取 calculated");

        // 设置 override = 120 → 取 override = 120
        setOverride(calcId, bd("120"));
        BigDecimal level3 = findEffectiveSafetyStock(param.getId());
        assertEquals(0, level3.compareTo(bd("120")), "override > calculated 时取 override");
    }

    @Test
    public void testConfirmWriteback() {
        seedMaterial(M_STAT);
        seedWarehouse();
        ErpDrpParameter param = seedParameter(M_STAT, bd("50"), 15);

        Long calcId = seedCalc(M_STAT, ErpDrpConstants.SS_METHOD_SIMPLE,
                ErpDrpConstants.SERVICE_LEVEL_PCT95, 3, 15);
        setCalcResult(calcId, bd("80"), bd("100"));

        // 回写前 parameter.safetyStock = 50
        assertEquals(0, daoProvider.daoFor(ErpDrpParameter.class).getEntityById(param.getId()).getSafetyStock().compareTo(bd("50")));

        confirmWritebackOk(calcId);

        // 回写后 parameter.safetyStock = 80（calculated 值）
        assertEquals(0, daoProvider.daoFor(ErpDrpParameter.class).getEntityById(param.getId()).getSafetyStock().compareTo(bd("80")),
                "confirmWriteback 回写 calculatedSafetyStock 到 ErpDrpParameter");

        // 设置 override 后回写覆盖值
        setOverride(calcId, bd("120"));
        confirmWritebackOk(calcId);
        assertEquals(0, daoProvider.daoFor(ErpDrpParameter.class).getEntityById(param.getId()).getSafetyStock().compareTo(bd("120")),
                "override 非空时回写覆盖值");
    }

    // ---------- helpers ----------

    private void calculateOk(Long calcId) {
        Map<String, Object> args = new java.util.LinkedHashMap<>();
        args.put("calcId", calcId);
        ApiResponse<?> resp = rpc(mutation, "ErpInvDrpSafetyStockCalc__calculate", args);
        assertEquals(0, resp.getStatus(), "calculate 应成功: " + resp);
    }

    private BigDecimal findEffectiveSafetyStock(Long parameterId) {
        Map<String, Object> args = new java.util.LinkedHashMap<>();
        args.put("parameterId", parameterId);
        ApiResponse<?> resp = rpc(query, "ErpInvDrpSafetyStockCalc__findEffectiveSafetyStock", args);
        assertEquals(0, resp.getStatus(), "findEffectiveSafetyStock 应成功: " + resp);
        return (BigDecimal) resp.getData();
    }

    private void confirmWritebackOk(Long calcId) {
        Map<String, Object> args = new java.util.LinkedHashMap<>();
        args.put("calcId", calcId);
        ApiResponse<?> resp = rpc(mutation, "ErpInvDrpSafetyStockCalc__confirmWriteback", args);
        assertEquals(0, resp.getStatus(), "confirmWriteback 应成功: " + resp);
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private void setCalcResult(Long calcId, BigDecimal ss, BigDecimal rop) {
        ormTemplate.runInSession(() -> {
            ErpInvDrpSafetyStockCalc calc = daoProvider.daoFor(ErpInvDrpSafetyStockCalc.class).getEntityById(calcId);
            calc.setCalculatedSafetyStock(ss);
            calc.setCalculatedRop(rop);
            daoProvider.daoFor(ErpInvDrpSafetyStockCalc.class).updateEntity(calc);
        });
    }

    private void setOverride(Long calcId, BigDecimal override) {
        ormTemplate.runInSession(() -> {
            ErpInvDrpSafetyStockCalc calc = daoProvider.daoFor(ErpInvDrpSafetyStockCalc.class).getEntityById(calcId);
            calc.setOverrideSafetyStock(override);
            daoProvider.daoFor(ErpInvDrpSafetyStockCalc.class).updateEntity(calc);
        });
    }

    private Long seedCalc(Long materialId, String method, String serviceLevel, int historyMonths, int leadTimeDays) {
        Long id = 5700L + materialId + (long) method.hashCode() % 100;
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpInvDrpSafetyStockCalc> dao = daoProvider.daoFor(ErpInvDrpSafetyStockCalc.class);
            ErpInvDrpSafetyStockCalc c = new ErpInvDrpSafetyStockCalc();
            c.orm_propValueByName("id", id);
            c.setCode("SS-" + materialId + "-" + method);
            c.setOrgId(ORG_ID);
            c.setMaterialId(materialId);
            c.setWarehouseId(WH_ID);
            c.orm_propValueByName("method", method);
            c.orm_propValueByName("serviceLevel", serviceLevel);
            c.setHistoryMonths(historyMonths);
            c.setLeadTimeDays(leadTimeDays);
            dao.saveEntity(c);
        });
        return id;
    }

    private ErpDrpParameter seedParameter(Long materialId, BigDecimal safetyStock, int leadTimeDays) {
        Long id = 5800L + materialId;
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpDrpParameter> dao = daoProvider.daoFor(ErpDrpParameter.class);
            ErpDrpParameter p = new ErpDrpParameter();
            p.orm_propValueByName("id", id);
            p.setMaterialId(materialId);
            p.setWarehouseId(WH_ID);
            p.setSafetyStock(safetyStock);
            p.setReplenishmentLeadTime(leadTimeDays);
            p.setOrderMultiple(bd("1"));
            p.orm_propValueByName("replenishmentMethod", ErpDrpConstants.REPLENISHMENT_METHOD_MIN_MAX);
            p.setOrgId(ORG_ID);
            dao.saveEntity(p);
        });
        return daoProvider.daoFor(ErpDrpParameter.class).getEntityById(id);
    }

    private void seedOutboundMove(Long materialId, String code, LocalDate businessDate, BigDecimal qty) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpInvStockMove> mdao = daoProvider.daoFor(ErpInvStockMove.class);
            Long moveId = 5300L + (long) Math.abs(code.hashCode() % 500);
            ErpInvStockMove m = new ErpInvStockMove();
            m.orm_propValueByName("id", moveId);
            m.setCode(code);
            m.orm_propValueByName("moveType", ErpDrpConstants.MOVE_TYPE_OUTGOING);
            m.setOrgId(ORG_ID);
            m.setBusinessDate(businessDate);
            m.setSourceWarehouseId(WH_ID);
            m.setDocStatus("APPROVED");
            m.orm_propValueByName("approveStatus", "APPROVED");
            m.setPosted(Boolean.TRUE);
            mdao.saveEntity(m);

            IEntityDao<ErpInvStockMoveLine> ldao = daoProvider.daoFor(ErpInvStockMoveLine.class);
            ErpInvStockMoveLine line = new ErpInvStockMoveLine();
            line.orm_propValueByName("id", moveId + 50000);
            line.setMoveId(moveId);
            line.setLineNo(10);
            line.setMaterialId(materialId);
            line.orm_propValueByName("uoMId", UOM_ID);
            line.setQuantity(qty);
            ldao.saveEntity(line);
        });
    }

    private void seedMaterial(Long id) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            ErpMdMaterial m = new ErpMdMaterial();
            m.orm_propValueByName("id", id);
            m.setCode("MAT-" + id);
            m.setName("Material " + id);
            m.orm_propValueByName("materialType", "GOODS");
            m.setUoMId(UOM_ID);
            m.setStatus("ACTIVE");
            dao.saveEntity(m);
        });
    }

    private void seedWarehouse() {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdWarehouse> dao = daoProvider.daoFor(ErpMdWarehouse.class);
            ErpMdWarehouse w = new ErpMdWarehouse();
            w.orm_propValueByName("id", WH_ID);
            w.setCode("WH-" + WH_ID);
            w.setName("Warehouse " + WH_ID);
            w.setStatus("ACTIVE");
            dao.saveEntity(w);
        });
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
