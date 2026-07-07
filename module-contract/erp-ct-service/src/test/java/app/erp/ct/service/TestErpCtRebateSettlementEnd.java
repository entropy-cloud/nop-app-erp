package app.erp.ct.service;

import app.erp.contract.dao.entity.ErpCtRebateAccrual;
import app.erp.contract.dao.entity.ErpCtRebateAgreement;
import app.erp.contract.dao.entity.ErpCtRebateSettlement;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdUoM;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.*;

@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCtRebateSettlementEnd extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @Test
    public void testSalesRebateSettlementGeneratesArCreditMemo() {
        long[] setup = setupSalesRebateAndAccrue();
        long settlementId = setup[1];

        ApiResponse<?> resp = executeRpc(mutation, "ErpCtRebateSettlement__postSettlement",
                ApiRequest.build(Map.of("settlementId", settlementId)));
        assertEquals(0, resp.getStatus());

        ErpCtRebateSettlement settlement = daoProvider.daoFor(ErpCtRebateSettlement.class)
                .getEntityById(settlementId);
        assertEquals("POSTED", settlement.getStatus());
        assertNotNull(settlement.getCreditMemoBillCode());
        assertEquals("AR_INVOICE", settlement.getCreditMemoBillType());
        assertTrue(settlement.getTotalRebateAmount().signum() > 0);

        ErpSalInvoice creditMemo = daoProvider.daoFor(ErpSalInvoice.class)
                .findFirstByQuery(eqQuery("code", settlement.getCreditMemoBillCode()));
        assertNotNull(creditMemo);
        assertTrue(creditMemo.getTotalAmount().signum() < 0);
    }

    @Test
    public void testSettlementMarksAccrualsSettled() {
        long[] setup = setupSalesRebateAndAccrue();
        long agreementId = setup[0];
        long settlementId = setup[1];

        ApiResponse<?> resp = executeRpc(mutation, "ErpCtRebateSettlement__postSettlement",
                ApiRequest.build(Map.of("settlementId", settlementId)));
        assertEquals(0, resp.getStatus());

        List<ErpCtRebateAccrual> accruals = findAccruals(agreementId);
        assertFalse(accruals.isEmpty());
        for (ErpCtRebateAccrual a : accruals) {
            assertTrue(Boolean.TRUE.equals(a.getIsSettled()));
        }
    }

    private long[] setupSalesRebateAndAccrue() {
        long[] setup = setupActiveContract("SALES", "OUTBOUND");
        long partnerId = setup[2];
        long currencyId = setup[3];
        long contractId = setup[0];

        long agreementId = createRebateAgreement(partnerId, contractId, "SALES", "PROGRESSIVE");
        createRebateTier(agreementId, new BigDecimal("0"), new BigDecimal("1000000"), new BigDecimal("0"));
        createRebateTier(agreementId, new BigDecimal("1000000"), null, new BigDecimal("2"));

        createPostedArInvoice("SREB-SI-1", partnerId, currencyId, new BigDecimal("1200000"));
        executeRpc(mutation, "ErpCtRebateAgreement__runAccrual",
                ApiRequest.build(Map.of("agreementId", agreementId, "asOfDate", LocalDate.now().toString())));

        long settlementId = createSettlement(agreementId);
        return new long[]{agreementId, settlementId};
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

            long contractId = createContract(partnerId, currencyId, contractType, direction);
            ids[0] = contractId;
            ids[1] = createContractLine(contractId, materialId);
            createVersion(contractId, 1, true, "FINALIZED");
            executeRpc(mutation, "ErpCtContract__activate",
                    ApiRequest.build(Map.of("contractId", contractId)));
            return null;
        });
        return ids;
    }

    private long createPartner() {
        ErpMdPartner p = daoProvider.daoFor(ErpMdPartner.class).newEntity();
        p.setCode("CT-SREB-PARTNER-" + System.nanoTime());
        p.setName("销售返利测试伙伴");
        p.setPartnerType("CUSTOMER");
        p.setStatus("ACTIVE");
        daoProvider.daoFor(ErpMdPartner.class).saveEntity(p);
        return p.getId();
    }

    private long createCurrency() {
        ErpMdCurrency c = daoProvider.daoFor(ErpMdCurrency.class).newEntity();
        c.setCode("CNY-SREB");
        c.setName("人民币");
        daoProvider.daoFor(ErpMdCurrency.class).saveEntity(c);
        return c.getId();
    }

    private long createUoM() {
        ErpMdUoM u = daoProvider.daoFor(ErpMdUoM.class).newEntity();
        u.setCode("PCS-SREB");
        u.setName("个");
        daoProvider.daoFor(ErpMdUoM.class).saveEntity(u);
        return u.getId();
    }

    private long createMaterial(long uomId) {
        ErpMdMaterial m = daoProvider.daoFor(ErpMdMaterial.class).newEntity();
        m.setCode("MAT-SREB-" + System.nanoTime());
        m.setName("销售返利测试物料");
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
        data.put("code", "CT-SREB-" + System.nanoTime());
        data.put("contractName", "销售返利测试合同");
        data.put("contractType", type);
        data.put("contractDirection", direction);
        data.put("partnerId", partnerId);
        data.put("currencyId", currencyId);
        data.put("startDate", "2026-01-01");
        data.put("endDate", "2027-12-31");
        data.put("status", "NEGOTIATION");
        Map<?, ?> r = (Map<?, ?>) executeRpc(mutation, "ErpCtContract__save",
                ApiRequest.build(Map.of("data", data))).getData();
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
        Map<?, ?> r = (Map<?, ?>) executeRpc(mutation, "ErpCtContractLine__save",
                ApiRequest.build(Map.of("data", data))).getData();
        return toLongId(r);
    }

    private void createVersion(long contractId, int versionNo, boolean isCurrent, String status) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("contractId", contractId);
        data.put("versionNo", versionNo);
        data.put("versionDate", "2026-01-01");
        data.put("isCurrent", isCurrent);
        data.put("status", status);
        executeRpc(mutation, "ErpCtContractVersion__save",
                ApiRequest.build(Map.of("data", data)));
    }

    private long createRebateAgreement(long partnerId, long contractId, String rebateType, String method) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", "SREB-AG-" + System.nanoTime());
        data.put("contractId", contractId);
        data.put("partnerId", partnerId);
        data.put("rebateType", rebateType);
        data.put("startDate", "2026-01-01");
        data.put("endDate", "2027-12-31");
        data.put("accrualMethod", method);
        data.put("status", "ACTIVE");
        Map<?, ?> r = (Map<?, ?>) executeRpc(mutation, "ErpCtRebateAgreement__save",
                ApiRequest.build(Map.of("data", data))).getData();
        return toLongId(r);
    }

    private void createRebateTier(long agreementId, BigDecimal from, BigDecimal to, BigDecimal percent) {
        app.erp.contract.dao.entity.ErpCtRebateTier tier =
                daoProvider.daoFor(app.erp.contract.dao.entity.ErpCtRebateTier.class).newEntity();
        tier.setRebateAgreementId(agreementId);
        tier.setFromAmount(from);
        tier.setToAmount(to);
        tier.setRebatePercent(percent);
        daoProvider.daoFor(app.erp.contract.dao.entity.ErpCtRebateTier.class).saveEntity(tier);
    }

    private void createPostedArInvoice(String code, long customerId, long currencyId, BigDecimal amount) {
        ErpSalInvoice invoice = daoProvider.daoFor(ErpSalInvoice.class).newEntity();
        invoice.setCode(code);
        invoice.setCustomerId(customerId);
        invoice.setBusinessDate(LocalDate.of(2026, 6, 15));
        invoice.setCurrencyId(currencyId);
        invoice.setExchangeRate(BigDecimal.ONE);
        invoice.setTotalAmount(amount);
        invoice.setAmountSource(amount);
        invoice.setAmountFunctional(amount);
        invoice.setTotalAmountWithTax(amount);
        invoice.setDocStatus("ACTIVE");
        invoice.setApproveStatus("APPROVED");
        invoice.setReceivedStatus("UNRECEIVED");
        invoice.setPosted(true);
        daoProvider.daoFor(ErpSalInvoice.class).saveEntity(invoice);
    }

    private long createSettlement(long agreementId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("rebateAgreementId", agreementId);
        data.put("settlementDate", LocalDate.now().toString());
        data.put("status", "DRAFT");
        Map<?, ?> r = (Map<?, ?>) executeRpc(mutation, "ErpCtRebateSettlement__save",
                ApiRequest.build(Map.of("data", data))).getData();
        return toLongId(r);
    }

    private List<ErpCtRebateAccrual> findAccruals(long agreementId) {
        return ormTemplate.runInSession(session -> {
            QueryBean q = new QueryBean();
            q.addFilter(eq("rebateAgreementId", agreementId));
            return daoProvider.daoFor(ErpCtRebateAccrual.class).findAllByQuery(q);
        });
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
