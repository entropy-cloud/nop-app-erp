package app.erp.drp.service;

import app.erp.drp.dao.entity.ErpDrpLine;
import app.erp.drp.dao.entity.ErpDrpParameter;
import app.erp.drp.dao.entity.ErpDrpPlan;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.mfg.dao.entity.ErpMfgForecast;
import app.erp.mfg.dao.entity.ErpMfgForecastLine;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdWarehouse;
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
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * DRP forecastDemand 接入预测来源测试（plan 2026-07-05-0427-1 Phase 3 Exit Criteria）。
 *
 * <p>覆盖：
 * <ul>
 *   <li>DRP 运行含 approved 预测（按目标仓库匹配）时，ErpDrpLine.forecastDemand 被填充且参与净需求公式</li>
 *   <li>仓库过滤：预测行 warehouseId ≠ 目标仓库不进入消费；warehouseId 为 null（产品级）不进入 DRP 消费</li>
 *   <li>config-gated erp-drp.forecast-consume-enabled=false 时 forecastDemand=0</li>
 *   <li>无预测时 forecastDemand=0（与基线一致，无回归）</li>
 * </ul>
 */
@io.nop.api.core.annotations.autotest.NopTestConfig(localDb = true,
        initDatabaseSchema = io.nop.api.core.annotations.core.OptionalBoolean.TRUE,
        enableActionAuth = io.nop.api.core.annotations.core.OptionalBoolean.FALSE)
public class TestErpDrpForecastSource extends JunitAutoTestCase {

    static final Long ORG_ID = 6401L;
    static final Long UOM_ID = 6501L;
    static final Long CURRENCY_ID = 6701L;
    static final Long SUPPLIER_ID = 6801L;
    static final Long WH_TARGET = 6101L;
    static final Long WH_OTHER = 6103L; // 非目标仓（仓库过滤测试）

    static final Long M = 6201L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testForecastDemandFilling() {
        seedMaterial();
        seedWarehouse();
        // safetyStock=0，stock=0 → net = 0 + forecastDemand(20) - 0 + 0 - 0 = 20
        seedParameter(M, bd("0"), bd("1"), null, SUPPLIER_ID);
        // APPROVED 仓级预测 M × WH_TARGET × 20（区间覆盖 plan）
        seedForecast("FCST-DRP-OK", "APPROVED", M, WH_TARGET,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10), bd("20"));

        Long planId = seedPlan("DRP-FCST-OK");
        runDrpOk(planId);

        ErpDrpLine line = findLine(planId, M);
        assertNotNull(line, "DRP 行应存在");
        assertEquals(0, line.getForecastDemand().compareTo(bd("20")),
                "forecastDemand 应被预测聚合填充为 20");
        assertEquals(0, line.getNetRequirement().compareTo(bd("20")),
                "净需求 = safetyStock(0) + forecastDemand(20) - currentStock(0) + allocatedQty(0) - onOrderQty(0) = 20");
    }

    @Test
    public void testWarehouseFiltering() {
        seedMaterial();
        seedWarehouse();
        // M 在 WH_TARGET safetyStock=0
        seedParameter(M, bd("0"), bd("1"), null, SUPPLIER_ID);

        // 仓库过滤 1：预测行 warehouseId = WH_OTHER（非目标仓）→ 不应被消费
        seedForecast("FCST-WH-OTHER", "APPROVED", M, WH_OTHER,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10), bd("999"));
        // 仓库过滤 2：产品级预测（warehouseId=null）→ 不应进入 DRP 仓级消费
        seedForecastProductLevel("FCST-PROD-LEVEL", "APPROVED", M,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10), bd("888"));

        Long planId = seedPlan("DRP-FILTER");
        runDrpOk(planId);

        ErpDrpLine line = findLine(planId, M);
        assertNotNull(line);
        assertEquals(0, line.getForecastDemand().compareTo(BigDecimal.ZERO),
                "其他仓预测 + 产品级预测均不应进入 DRP 消费");
    }

    @Test
    public void testForecastDisabledNoRegression() {
        seedMaterial();
        seedWarehouse();
        seedParameter(M, bd("0"), bd("1"), null, SUPPLIER_ID);
        seedForecast("FCST-OFF", "APPROVED", M, WH_TARGET,
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 10), bd("20"));

        setConfig(ErpDrpConfigs.CONFIG_DRP_FORECAST_CONSUME_ENABLED, "false");
        try {
            Long planId = seedPlan("DRP-OFF");
            runDrpOk(planId);
            ErpDrpLine line = findLine(planId, M);
            assertNotNull(line);
            assertEquals(0, line.getForecastDemand().compareTo(BigDecimal.ZERO),
                    "config-gated 关闭时 forecastDemand=0");
        } finally {
            setConfig(ErpDrpConfigs.CONFIG_DRP_FORECAST_CONSUME_ENABLED, "true");
        }
    }

    @Test
    public void testNoForecastZeroBaseline() {
        seedMaterial();
        seedWarehouse();
        seedParameter(M, bd("0"), bd("1"), null, SUPPLIER_ID);

        Long planId = seedPlan("DRP-NOFCST");
        runDrpOk(planId);
        ErpDrpLine line = findLine(planId, M);
        assertNotNull(line);
        assertEquals(0, line.getForecastDemand().compareTo(BigDecimal.ZERO),
                "无预测时 forecastDemand=0（基线一致）");
    }

    // ---------- helpers ----------

    private void runDrpOk(Long planId) {
        ApiResponse<?> resp = rpc(mutation, "ErpDrpPlan__runDrp", Map.of("planId", planId));
        assertEquals(0, resp.getStatus(), "runDrp 应成功: " + resp);
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpDrpLine findLine(Long planId, Long materialId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("planId", planId));
        q.addFilter(eq("materialId", materialId));
        List<ErpDrpLine> list = daoProvider.daoFor(ErpDrpLine.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private Long seedPlan(String code) {
        Long id = 6001L + (long) Math.abs(code.hashCode() % 600);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpDrpPlan> dao = daoProvider.daoFor(ErpDrpPlan.class);
            ErpDrpPlan plan = new ErpDrpPlan();
            plan.orm_propValueByName("id", id);
            plan.setCode(code);
            plan.setPlanName("DRP-" + code);
            plan.setPeriodFrom(LocalDate.of(2026, 7, 1));
            plan.setPeriodTo(LocalDate.of(2026, 7, 31));
            plan.setStatus(ErpDrpConstants.DRP_PLAN_STATUS_DRAFT);
            plan.setOrgId(ORG_ID);
            dao.saveEntity(plan);
        });
        return id;
    }

    private void seedParameter(Long materialId, BigDecimal safetyStock, BigDecimal orderMultiple,
                               Long sourceWarehouseId, Long supplierId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpDrpParameter> dao = daoProvider.daoFor(ErpDrpParameter.class);
            ErpDrpParameter p = new ErpDrpParameter();
            p.orm_propValueByName("id", 6900L + materialId);
            p.setMaterialId(materialId);
            p.setWarehouseId(WH_TARGET);
            p.setSafetyStock(safetyStock);
            p.setOrderMultiple(orderMultiple);
            p.setPreferredSourceWarehouseId(sourceWarehouseId);
            p.setPreferredSupplierId(supplierId);
            p.setReplenishmentLeadTime(7);
            p.orm_propValueByName("replenishmentMethod", ErpDrpConstants.REPLENISHMENT_METHOD_MIN_MAX);
            p.setOrgId(ORG_ID);
            dao.saveEntity(p);
        });
    }

    private void seedMaterial() {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            ErpMdMaterial m = new ErpMdMaterial();
            m.orm_propValueByName("id", M);
            m.setCode("MAT-" + M);
            m.setName("Material " + M);
            m.orm_propValueByName("materialType", "GOODS");
            m.setUoMId(UOM_ID);
            m.setStatus("ACTIVE");
            dao.saveEntity(m);
        });
    }

    private void seedWarehouse() {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdWarehouse> dao = daoProvider.daoFor(ErpMdWarehouse.class);
            for (Long wid : new Long[]{WH_TARGET, WH_OTHER}) {
                ErpMdWarehouse w = new ErpMdWarehouse();
                w.orm_propValueByName("id", wid);
                w.setCode("WH-" + wid);
                w.setName("Warehouse " + wid);
                w.setStatus("ACTIVE");
                dao.saveEntity(w);
            }
            IEntityDao<ErpMdCurrency> cdao = daoProvider.daoFor(ErpMdCurrency.class);
            ErpMdCurrency c = new ErpMdCurrency();
            c.orm_propValueByName("id", CURRENCY_ID);
            c.setCode("CNY");
            c.setName("人民币");
            c.orm_propValueByName("isActive", Boolean.TRUE);
            cdao.saveEntity(c);
        });
    }

    private Long seedForecast(String code, String status, Long materialId, Long warehouseId,
                              LocalDate pStart, LocalDate pEnd, BigDecimal qty) {
        Long headId = 9300L + (long) Math.abs(code.hashCode() % 400);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgForecast> headDao = daoProvider.daoFor(ErpMfgForecast.class);
            ErpMfgForecast head = new ErpMfgForecast();
            head.orm_propValueByName("id", headId);
            head.setCode(code);
            head.setOrgId(ORG_ID);
            head.setPlanName("预测-" + code);
            head.setPeriodFrom(pStart);
            head.setPeriodTo(pEnd);
            head.setStatus(status);
            headDao.saveEntity(head);

            IEntityDao<ErpMfgForecastLine> lineDao = daoProvider.daoFor(ErpMfgForecastLine.class);
            ErpMfgForecastLine line = new ErpMfgForecastLine();
            line.orm_propValueByName("id", headId * 1000 + 10);
            line.setForecastId(headId);
            line.setLineNo(10);
            line.setMaterialId(materialId);
            line.setWarehouseId(warehouseId);
            line.setUoMId(UOM_ID);
            line.setPeriodStart(pStart);
            line.setPeriodEnd(pEnd);
            line.setForecastQty(qty);
            lineDao.saveEntity(line);
        });
        return headId;
    }

    /** 产品级预测（warehouseId=null），不应进入 DRP 仓级消费。 */
    private Long seedForecastProductLevel(String code, String status, Long materialId,
                                          LocalDate pStart, LocalDate pEnd, BigDecimal qty) {
        Long headId = 9350L + (long) Math.abs(code.hashCode() % 400);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgForecast> headDao = daoProvider.daoFor(ErpMfgForecast.class);
            ErpMfgForecast head = new ErpMfgForecast();
            head.orm_propValueByName("id", headId);
            head.setCode(code);
            head.setOrgId(ORG_ID);
            head.setPlanName("预测-" + code);
            head.setPeriodFrom(pStart);
            head.setPeriodTo(pEnd);
            head.setStatus(status);
            headDao.saveEntity(head);

            IEntityDao<ErpMfgForecastLine> lineDao = daoProvider.daoFor(ErpMfgForecastLine.class);
            ErpMfgForecastLine line = new ErpMfgForecastLine();
            line.orm_propValueByName("id", headId * 1000 + 10);
            line.setForecastId(headId);
            line.setLineNo(10);
            line.setMaterialId(materialId);
            // warehouseId 留空（产品级）
            line.setUoMId(UOM_ID);
            line.setPeriodStart(pStart);
            line.setPeriodEnd(pEnd);
            line.setForecastQty(qty);
            lineDao.saveEntity(line);
        });
        return headId;
    }

    private void setConfig(String key, String value) {
        io.nop.api.core.config.AppConfig.getConfigProvider().assignConfigValue(key, value);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
