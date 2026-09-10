package app.erp.drp.service;

import app.erp.drp.dao.entity.ErpDrpLine;
import app.erp.drp.dao.entity.ErpDrpParameter;
import app.erp.drp.dao.entity.ErpDrpPlan;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.md.dao.entity.ErpMdCurrency;
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
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * P3-CK-drp-022-r3 replenishment-method 死值回归（plan 2026-09-10-1141-2 Phase 1）。
 *
 * <p>缺陷面：ErpDrpParameter.replenishmentMethod mandatory 三选一（MIN_MAX/PERIODIC/LOT_FOR_LOT），
 * 但引擎仅隐式 LOT_FOR_LOT——用户配置 MIN_MAX/PERIODIC 静默无效（min/max/reviewPeriod 零消费）。
 *
 * <p>修复裁决 = runDrp 接入补货方法语义（MIN_MAX 低于 min 补至 max；PERIODIC 定期审视 order-up-to），
 * 对齐 docs/design/drp/README.md §仓库补货参数 + ui-patterns.md 补货方法声明。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpDrpReplenishmentMethod extends JunitAutoTestCase {

    @RegisterExtension
    static DrpFrozenClockExtension frozenClock = new DrpFrozenClockExtension();

    static final String ORG_ID = "6401";
    static final String UOM_ID = "6501";
    static final String WH_TARGET = "6101";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testMinMaxTopsUpToMaxWhenBelowMin() {
        seedBase();
        // MIN_MAX：min=100/max=300，当前库存 20 < min，无需求 → 补至 max = 280（缺陷现状：静默 0）
        seedParameter("6201", "MIN_MAX", "0", "100", "300", null, null);
        seedBalance("6201", bd("20"));

        String planId = seedPlan("DRP-MM");
        runDrpOk(planId);

        ErpDrpLine line = findLine(linesOf(planId), "6201");
        assertNotNull(line, "MIN_MAX 补货行应存在");
        assertEquals(0, line.getSuggestedQty().compareTo(bd("280")),
                "MIN_MAX 低于 min 应补至 max（300-20=280），实际=" + line.getSuggestedQty());
    }

    @Test
    public void testPeriodicOrderUpToTargetOnReview() {
        seedBase();
        // PERIODIC：目标水位 max=500，当前 100，无需求 → 定期审视 order-up-to = 400（缺陷现状：静默 0）
        seedParameter("6202", "PERIODIC", "0", null, "500", null, null);
        seedBalance("6202", bd("100"));

        String planId = seedPlan("DRP-PER");
        runDrpOk(planId);

        ErpDrpLine line = findLine(linesOf(planId), "6202");
        assertNotNull(line, "PERIODIC 补货行应存在");
        assertEquals(0, line.getSuggestedQty().compareTo(bd("400")),
                "PERIODIC 定期审视应 order-up-to（500-100=400），实际=" + line.getSuggestedQty());
    }

    @Test
    public void testLotForLotStaysDemandDriven() {
        seedBase();
        // LOT_FOR_LOT 回归护栏：需求驱动净额不受水位逻辑影响
        seedParameter("6203", "LOT_FOR_LOT", "50", "100", "300", null, null);
        seedBalance("6203", bd("20"));

        String planId = seedPlan("DRP-LFL");
        runDrpOk(planId);

        ErpDrpLine line = findLine(linesOf(planId), "6203");
        assertNotNull(line);
        // 净额=safetyStock 50 + 0 需求 − 20 现存 = 30，orderMultiple=1 → 30；水位逻辑不应触发（补线语义仅 MIN_MAX/PERIODIC）
        assertEquals(0, line.getSuggestedQty().compareTo(bd("30")),
                "LOT_FOR_LOT 保持需求驱动，实际=" + line.getSuggestedQty());
    }

    @Test
    public void testMinMaxNullFloorStaysDemandDriven() {
        seedBase();
        // 既有 seed 形态回归护栏：MIN_MAX 但 min/max 未配置 → 保持需求驱动（零水位语义）
        seedParameter("6204", "MIN_MAX", "50", null, null, null, null);
        seedBalance("6204", bd("20"));

        String planId = seedPlan("DRP-MMNULL");
        runDrpOk(planId);

        ErpDrpLine line = findLine(linesOf(planId), "6204");
        assertNotNull(line);
        assertEquals(0, line.getSuggestedQty().compareTo(bd("30")),
                "MIN_MAX 无 min 配置时保持需求驱动净额（safetyStock 50 − 现存 20 = 30），实际=" + line.getSuggestedQty());
    }

    // ---------- helpers ----------

    private void runDrpOk(String planId) {
        ApiResponse<?> resp = rpc(GraphQLOperationType.mutation, "ErpDrpPlan__runDrp", Map.of("planId", planId));
        assertEquals(0, resp.getStatus(), "runDrp 应成功: " + resp);
    }

    private String seedPlan(String code) {
        String id = String.valueOf(7001L + Math.abs(code.hashCode() % 600));
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpDrpPlan> dao = daoProvider.daoFor(ErpDrpPlan.class);
            ErpDrpPlan plan = dao.newEntity();
            plan.orm_propValueByName("id", id);
            plan.setCode(code);
            plan.setPlanName("DRP-" + code);
            plan.setBusinessDate(LocalDate.of(2026, 7, 1));
            plan.setPeriodFrom(LocalDate.of(2026, 7, 1));
            plan.setPeriodTo(LocalDate.of(2026, 7, 31));
            plan.setStatus(ErpDrpConstants.DRP_PLAN_STATUS_DRAFT);
            plan.setOrgId(ORG_ID);
            dao.saveEntity(plan);
        });
        return id;
    }

    private void seedParameter(String materialId, String method, String safetyStock, String min, String max,
                               String sourceWarehouseId, String supplierId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpDrpParameter> dao = daoProvider.daoFor(ErpDrpParameter.class);
            ErpDrpParameter p = dao.newEntity();
            p.orm_propValueByName("id", String.valueOf(7900L + Long.parseLong(materialId)));
            p.setMaterialId(materialId);
            p.setWarehouseId(WH_TARGET);
            p.setSafetyStock(safetyStock == null ? null : bd(safetyStock));
            p.setOrderMultiple(bd("1"));
            p.setPreferredSourceWarehouseId(sourceWarehouseId);
            p.setPreferredSupplierId(supplierId);
            p.setReplenishmentLeadTime(7);
            p.orm_propValueByName("replenishmentMethod", method);
            if (min != null) {
                p.setMinStockLevel(bd(min));
            }
            if (max != null) {
                p.setMaxStockLevel(bd(max));
            }
            p.setOrgId(ORG_ID);
            dao.saveEntity(p);
        });
    }

    private void seedBalance(String materialId, BigDecimal available) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
            ErpInvStockBalance b = dao.newEntity();
            b.orm_propValueByName("id", String.valueOf(8500L + Long.parseLong(materialId)));
            b.setOrgId(ORG_ID);
            b.setMaterialId(materialId);
            b.setWarehouseId(WH_TARGET);
            b.setTotalQuantity(available);
            b.setAvailableQuantity(available);
            dao.saveEntity(b);
        });
    }

    private void seedBase() {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            for (String id : new String[]{"6201", "6202", "6203", "6204"}) {
                ErpMdMaterial m = dao.newEntity();
                m.orm_propValueByName("id", id);
                m.setCode("MAT-MM-" + id);
                m.setName("Material " + id);
                m.orm_propValueByName("materialType", "GOODS");
                m.setUoMId(UOM_ID);
                m.setStatus("ACTIVE");
                dao.saveEntity(m);
            }
            IEntityDao<ErpMdWarehouse> wdao = daoProvider.daoFor(ErpMdWarehouse.class);
            ErpMdWarehouse w = wdao.newEntity();
            w.orm_propValueByName("id", WH_TARGET);
            w.setCode("WH-MM");
            w.setName("Warehouse MM");
            w.setStatus("ACTIVE");
            wdao.saveEntity(w);
            IEntityDao<ErpMdCurrency> cdao = daoProvider.daoFor(ErpMdCurrency.class);
            ErpMdCurrency c = cdao.newEntity();
            c.orm_propValueByName("id", "6702");
            c.setCode("CNY-MM");
            c.setName("人民币-MM");
            c.orm_propValueByName("isActive", Boolean.TRUE);
            cdao.saveEntity(c);
        });
    }

    private ApiResponse<?> rpc(GraphQLOperationType op, String action, Map<String, Object> args) {
        io.nop.graphql.core.IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private List<ErpDrpLine> linesOf(String planId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("planId", planId));
        q.addOrderField("lineNo", false);
        return daoProvider.daoFor(ErpDrpLine.class).findAllByQuery(q);
    }

    private ErpDrpLine findLine(List<ErpDrpLine> lines, String materialId) {
        for (ErpDrpLine l : lines) {
            if (materialId.equals(l.getMaterialId())) {
                return l;
            }
        }
        return null;
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
