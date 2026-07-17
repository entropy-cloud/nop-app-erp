package app.erp.ct.service;

import app.erp.contract.dao.entity.ErpCtInvoicePlan;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdUoM;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.sal.dao.entity.ErpSalInvoice;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.*;

@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCtContractPosting extends JunitAutoTestCase {

    @RegisterExtension
    static CtFrozenClockExtension frozenClock = new CtFrozenClockExtension();

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @Test
    public void testTriggerInvoiceGeneratesApInvoice() {
        long[] setup = setupActiveContract("PURCHASE", "INBOUND");
        long contractLineId = setup[1];

        long planId = saveInvoicePlan(contractLineId, new BigDecimal("1000"));

        ApiResponse<?> resp = executeRpc(mutation, "ErpCtInvoicePlan__triggerInvoice",
                ApiRequest.build(Map.of("planId", planId)));
        assertEquals(0, resp.getStatus());

        ErpCtInvoicePlan plan = daoProvider.daoFor(ErpCtInvoicePlan.class).getEntityById(planId);
        assertTrue(Boolean.TRUE.equals(plan.getIsInvoiced()));
        assertNotNull(plan.getInvoiceBillCode());

        ErpPurInvoice invoice = daoProvider.daoFor(ErpPurInvoice.class)
                .findFirstByQuery(eqQuery("code", plan.getInvoiceBillCode()));
        assertNotNull(invoice);
        assertEquals("DRAFT", invoice.getDocStatus());
        assertFalse(invoice.getPosted());
    }

    @Test
    public void testTriggerInvoiceGeneratesArInvoice() {
        long[] setup = setupActiveContract("SALES", "OUTBOUND");
        long contractLineId = setup[1];

        long planId = saveInvoicePlan(contractLineId, new BigDecimal("2000"));

        ApiResponse<?> resp = executeRpc(mutation, "ErpCtInvoicePlan__triggerInvoice",
                ApiRequest.build(Map.of("planId", planId)));
        assertEquals(0, resp.getStatus());

        ErpCtInvoicePlan plan = daoProvider.daoFor(ErpCtInvoicePlan.class).getEntityById(planId);
        assertTrue(Boolean.TRUE.equals(plan.getIsInvoiced()));
        assertNotNull(plan.getInvoiceBillCode());

        ErpSalInvoice invoice = daoProvider.daoFor(ErpSalInvoice.class)
                .findFirstByQuery(eqQuery("code", plan.getInvoiceBillCode()));
        assertNotNull(invoice);
        assertEquals("DRAFT", invoice.getDocStatus());
        assertFalse(invoice.getPosted());
    }

    @Test
    public void testTriggerInvoiceRejectedForSuspendedContract() {
        long[] setup = setupActiveContract("PURCHASE", "INBOUND");
        long contractLineId = setup[1];
        long contractId = setup[0];

        executeRpc(mutation, "ErpCtContract__suspend",
                ApiRequest.build(Map.of("contractId", contractId)));

        long planId = saveInvoicePlan(contractLineId, new BigDecimal("1000"));
        ApiResponse<?> bad = executeRpc(mutation, "ErpCtInvoicePlan__triggerInvoice",
                ApiRequest.build(Map.of("planId", planId)));
        assertNotEquals(0, bad.getStatus());
    }

    private long[] setupActiveContract(String contractType, String direction) {
        long[] ids = new long[5];
        ormTemplate.runInSession(session -> {
            long partnerId = createPartner();
            long currencyId = createCurrency();
            long uomId = createUoM();
            long materialId = createMaterial(uomId);
            ids[2] = partnerId;
            ids[3] = currencyId;
            ids[4] = materialId;
            return null;
        });
        long partnerId = ids[2];
        long currencyId = ids[3];
        long materialId = ids[4];

        long contractId = createContract(partnerId, currencyId, contractType, direction);
        ids[0] = contractId;
        ids[1] = createContractLine(contractId, materialId);
        createVersion(contractId, 1, true, "FINALIZED");
        executeRpc(mutation, "ErpCtContract__activate",
                ApiRequest.build(Map.of("contractId", contractId)));
        return ids;
    }

    private long createPartner() {
        ErpMdPartner p = daoProvider.daoFor(ErpMdPartner.class).newEntity();
        p.setCode("CT-POST-PARTNER-" + System.nanoTime());
        p.setName("开票测试伙伴");
        p.setPartnerType("CUSTOMER");
        p.setStatus("ACTIVE");
        daoProvider.daoFor(ErpMdPartner.class).saveEntity(p);
        return p.getId();
    }

    private long createCurrency() {
        ErpMdCurrency c = daoProvider.daoFor(ErpMdCurrency.class).newEntity();
        c.setCode("CNY-CP");
        c.setName("人民币");
        daoProvider.daoFor(ErpMdCurrency.class).saveEntity(c);
        return c.getId();
    }

    private long createUoM() {
        ErpMdUoM u = daoProvider.daoFor(ErpMdUoM.class).newEntity();
        u.setCode("PCS-CT-POST");
        u.setName("个");
        daoProvider.daoFor(ErpMdUoM.class).saveEntity(u);
        return u.getId();
    }

    private long createMaterial(long uomId) {
        ErpMdMaterial m = daoProvider.daoFor(ErpMdMaterial.class).newEntity();
        m.setCode("MAT-CT-POST-" + System.nanoTime());
        m.setName("开票测试物料");
        m.setMaterialType("GOODS");
        m.setUoMId(uomId);
        m.setStatus("ACTIVE");
        daoProvider.daoFor(ErpMdMaterial.class).saveEntity(m);
        return m.getId();
    }

    private long toLongId(Map<?, ?> r) {
        Object id = r.get("id");
        if (id instanceof Number) {
            return ((Number) id).longValue();
        }
        return Long.parseLong(String.valueOf(id));
    }

    private long createContract(long partnerId, long currencyId, String type, String direction) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", "CT-POST-" + System.nanoTime());
        data.put("contractName", "开票测试合同");
        data.put("contractType", type);
        data.put("contractDirection", direction);
        data.put("partnerId", partnerId);
        data.put("currencyId", currencyId);
        data.put("startDate", "2026-01-01");
        data.put("endDate", "2027-12-31");
        data.put("status", "NEGOTIATION");
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtContract__save",
                ApiRequest.build(Map.of("data", data)));
        assertEquals(0, resp.getStatus(), "ErpCtContract__save 应成功: " + resp);
        Map<?, ?> r = (Map<?, ?>) resp.getData();
        return toLongId(r);
    }

    private long createContractLine(long contractId, long materialId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("lineNo", 1);
        data.put("contractId", contractId);
        data.put("materialId", materialId);
        data.put("quantity", new BigDecimal("100"));
        data.put("unitPrice", new BigDecimal("10"));
        data.put("amount", new BigDecimal("1000"));
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtContractLine__save",
                ApiRequest.build(Map.of("data", data)));
        assertEquals(0, resp.getStatus(), "ErpCtContractLine__save 应成功: " + resp);
        Map<?, ?> r = (Map<?, ?>) resp.getData();
        return toLongId(r);
    }

    private void createVersion(long contractId, int versionNo, boolean isCurrent, String status) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("contractId", contractId);
        data.put("versionNo", versionNo);
        data.put("versionDate", "2026-01-01");
        data.put("isCurrent", isCurrent);
        data.put("status", status);
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtContractVersion__save",
                ApiRequest.build(Map.of("data", data)));
        assertEquals(0, resp.getStatus(), "ErpCtContractVersion__save 应成功: " + resp);
    }

    private long saveInvoicePlan(long contractLineId, BigDecimal amount) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("contractLineId", contractLineId);
        data.put("planDate", "2026-06-01");
        data.put("amount", amount);
        data.put("invoiceTerm", "MILESTONE");
        data.put("isInvoiced", false);
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtInvoicePlan__save",
                ApiRequest.build(Map.of("data", data)));
        assertEquals(0, resp.getStatus(), "ErpCtInvoicePlan__save 应成功: " + resp);
        Map<?, ?> r = (Map<?, ?>) resp.getData();
        return toLongId(r);
    }

    private QueryBean eqQuery(String field, Object value) {
        QueryBean q = new QueryBean();
        q.addFilter(eq(field, value));
        return q;
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
