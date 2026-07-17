package app.erp.ct.service;

import app.erp.contract.dao.entity.ErpCtContractVersion;
import app.erp.contract.dao.entity.ErpCtInvoicePlan;
import app.erp.contract.dao.entity.ErpCtRebateAccrual;
import app.erp.contract.dao.entity.ErpCtRebateAgreement;
import app.erp.contract.dao.entity.ErpCtRebateSettlement;
import app.erp.contract.dao.entity.ErpCtVolumeDiscount;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdUoM;
import app.erp.pur.dao.entity.ErpPurInvoice;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 合同版本/InvoicePlan/批量折扣/返利 行为测试（plan 2026-07-04-1115-1 Phase 6）。
 *
 * <p>覆盖：合同状态机全路径 + 版本 isCurrent 翻转、InvoicePlan 双向触发 + 失败路径、
 * VolumeDiscount 区间命中/无重叠拒、返利 PROGRESSIVE 跨档补差、结算 POSTED 贷项 + 计提标记。
 *
 * <p>沿用 Phase 4 样板（-service 模块、JunitAutoTestCase、@NopTestConfig schema bootstrap）。
 * 跨域前置主数据由 createPrereqs() 自建；已过账发票经 DAO 直接构造（绕过审核管道）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCtContractRebate extends JunitAutoTestCase {

    @RegisterExtension
    static CtFrozenClockExtension frozenClock = new CtFrozenClockExtension();

    @Inject
    IGraphQLEngine graphQLEngine;

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IOrmTemplate ormTemplate;

    // ============ 合同状态机 + 版本管理 ============

    @Test
    public void testContractStateMachine() {
        long[] setup = setupActiveContract("PURCHASE", "INBOUND");
        long contractId = setup[0];

        // NEGOTIATION → ACTIVE（activate 已在 setup 完成）
        Map<?, ?> contract = (Map<?, ?>) executeRpc(GraphQLOperationType.query, "ErpCtContract__get",
                ApiRequest.build(Map.of("id", String.valueOf(contractId)))).getData();
        assertEquals("ACTIVE", contract.get("status"), "setup 后合同应为 ACTIVE");

        // ACTIVE → SUSPENDED
        executeRpc(GraphQLOperationType.mutation, "ErpCtContract__suspend",
                ApiRequest.build(Map.of("contractId", contractId)));
        contract = (Map<?, ?>) executeRpc(GraphQLOperationType.query, "ErpCtContract__get",
                ApiRequest.build(Map.of("id", String.valueOf(contractId)))).getData();
        assertEquals("SUSPENDED", contract.get("status"));

        // SUSPENDED → ACTIVE
        executeRpc(GraphQLOperationType.mutation, "ErpCtContract__resume",
                ApiRequest.build(Map.of("contractId", contractId)));
        contract = (Map<?, ?>) executeRpc(GraphQLOperationType.query, "ErpCtContract__get",
                ApiRequest.build(Map.of("id", String.valueOf(contractId)))).getData();
        assertEquals("ACTIVE", contract.get("status"));

        // ACTIVE → TERMINATED
        executeRpc(GraphQLOperationType.mutation, "ErpCtContract__terminate",
                ApiRequest.build(Map.of("contractId", contractId)));
        contract = (Map<?, ?>) executeRpc(GraphQLOperationType.query, "ErpCtContract__get",
                ApiRequest.build(Map.of("id", String.valueOf(contractId)))).getData();
        assertEquals("TERMINATED", contract.get("status"));

        // 非法迁移：TERMINATED → ACTIVE（activate）应失败
        ApiResponse<?> bad = executeRpc(GraphQLOperationType.mutation, "ErpCtContract__activate",
                ApiRequest.build(Map.of("contractId", contractId)));
        assertNotEquals(0, bad.getStatus(), "终态合同不可再 activate");
    }

    @Test
    public void testAmendVersionFlip() {
        long[] setup = setupActiveContract("PURCHASE", "INBOUND");
        long contractId = setup[0];

        // amend：ACTIVE → DRAFT，新建版本（versionNo=2，isCurrent=true），旧版本 isCurrent=false
        executeRpc(GraphQLOperationType.mutation, "ErpCtContract__amend",
                ApiRequest.build(Map.of("contractId", contractId)));

        Map<?, ?> contract = (Map<?, ?>) executeRpc(GraphQLOperationType.query, "ErpCtContract__get",
                ApiRequest.build(Map.of("id", String.valueOf(contractId)))).getData();
        assertEquals("DRAFT", contract.get("status"), "amend 后合同头回 DRAFT");

        // 校验版本翻转
        List<ErpCtContractVersion> versions = findVersions(contractId);
        assertEquals(2, versions.size(), "amend 后应有 2 个版本");
        int currentCount = 0;
        int maxVersionNo = 0;
        for (ErpCtContractVersion v : versions) {
            if (Boolean.TRUE.equals(v.getIsCurrent())) {
                currentCount++;
                assertEquals("DRAFT", v.getStatus(), "新版本应为 DRAFT");
            }
            if (v.getVersionNo() != null && v.getVersionNo() > maxVersionNo) {
                maxVersionNo = v.getVersionNo();
            }
        }
        assertEquals(1, currentCount, "恰有 1 个 current 版本");
        assertEquals(2, maxVersionNo, "versionNo 单调递增到 2");
    }

    // ============ VolumeDiscount ============

    @Test
    public void testVolumeDiscountResolve() {
        long[] setup = setupActiveContract("PURCHASE", "INBOUND");
        long contractLineId = setup[1];

        // 建区间带：[0,100) 0%，[100,500) 5%
        saveDiscountBand(contractLineId, new BigDecimal("0"), new BigDecimal("100"), new BigDecimal("0"));
        saveDiscountBand(contractLineId, new BigDecimal("100"), new BigDecimal("500"), new BigDecimal("5"));

        // qty=50 → 命中 [0,100) 0% → 折后价=原价
        Map<?, ?> r1 = (Map<?, ?>) executeRpc(GraphQLOperationType.query, "ErpCtVolumeDiscount__resolveDiscount",
                ApiRequest.build(Map.of("contractLineId", contractLineId,
                        "qty", new BigDecimal("50"), "unitPrice", new BigDecimal("100")))).getData();
        assertEquals(0, new BigDecimal("100").compareTo(new BigDecimal(r1.get("discountedUnitPrice").toString())),
                "0% 档应回退原价");

        // qty=200 → 命中 [100,500) 5% → 折后价=95
        Map<?, ?> r2 = (Map<?, ?>) executeRpc(GraphQLOperationType.query, "ErpCtVolumeDiscount__resolveDiscount",
                ApiRequest.build(Map.of("contractLineId", contractLineId,
                        "qty", new BigDecimal("200"), "unitPrice", new BigDecimal("100")))).getData();
        assertEquals(0, new BigDecimal("95").compareTo(new BigDecimal(r2.get("discountedUnitPrice").toString())),
                "5% 档折后价应为 95");
        assertTrue(Boolean.TRUE.equals(r2.get("bandMatched")), "应命中带");
    }

    @Test
    public void testVolumeDiscountOverlapRejected() {
        long[] setup = setupActiveContract("PURCHASE", "INBOUND");
        long contractLineId = setup[1];

        saveDiscountBand(contractLineId, new BigDecimal("0"), new BigDecimal("100"), new BigDecimal("0"));
        // 重叠带 [50,200) 应被拒
        ApiResponse<?> bad = saveDiscountBand(contractLineId, new BigDecimal("50"), new BigDecimal("200"),
                new BigDecimal("10"));
        assertNotEquals(0, bad.getStatus(), "重叠区间带保存应被拒");
    }

    // ============ InvoicePlan 触发 ============

    @Test
    public void testInvoicePlanTriggerInbound() {
        long[] setup = setupActiveContract("PURCHASE", "INBOUND");
        long contractLineId = setup[1];
        long partnerId = setup[2];
        long currencyId = setup[3];
        long materialId = setup[4];

        // 建开票计划
        long planId = saveInvoicePlan(contractLineId, new BigDecimal("1000"));

        // 触发 → 生成 AP 发票草稿
        ApiResponse<?> resp = executeRpc(GraphQLOperationType.mutation, "ErpCtInvoicePlan__triggerInvoice",
                ApiRequest.build(Map.of("planId", planId)));
        assertEquals(0, resp.getStatus(), "ACTIVE 合同触发应成功");

        ErpCtInvoicePlan plan = daoProvider.daoFor(ErpCtInvoicePlan.class).getEntityById(planId);
        assertTrue(Boolean.TRUE.equals(plan.getIsInvoiced()), "isInvoiced 应为 true");
        assertNotNull(plan.getInvoiceBillCode(), "invoiceBillCode 非空");

        // 验证 AP 发票已创建
        ErpPurInvoice invoice = daoProvider.daoFor(ErpPurInvoice.class)
                .findFirstByQuery(eqQuery("code", plan.getInvoiceBillCode()));
        assertNotNull(invoice, "AP 发票草稿应已生成");
    }

    @Test
    public void testInvoicePlanSuspendedRejected() {
        long[] setup = setupActiveContract("PURCHASE", "INBOUND");
        long contractLineId = setup[1];
        long contractId = setup[0];

        // SUSPENDED
        executeRpc(GraphQLOperationType.mutation, "ErpCtContract__suspend",
                ApiRequest.build(Map.of("contractId", contractId)));

        long planId = saveInvoicePlan(contractLineId, new BigDecimal("1000"));
        ApiResponse<?> bad = executeRpc(GraphQLOperationType.mutation, "ErpCtInvoicePlan__triggerInvoice",
                ApiRequest.build(Map.of("planId", planId)));
        assertNotEquals(0, bad.getStatus(), "SUSPENDED 合同触发应失败");
    }

    // ============ 返利计提 + 结算 ============

    @Test
    public void testRebateProgressiveAccrualAndSettlement() {
        long[] setup = setupActiveContract("PURCHASE", "INBOUND");
        long partnerId = setup[2];
        long currencyId = setup[3];
        long contractId = setup[0];

        // 建返利协议（ACTIVE，PROGRESSIVE）+ 阶梯：[0,1M) 0%，[1M,5M) 2%
        long agreementId = createRebateAgreement(partnerId, contractId, "PURCHASE", "PROGRESSIVE");
        createRebateTier(agreementId, new BigDecimal("0"), new BigDecimal("1000000"), new BigDecimal("0"));
        createRebateTier(agreementId, new BigDecimal("1000000"), null, new BigDecimal("2"));

        // 预置已过账 AP 发票（800K）→ 累计 800K，0% 档，返利 0
        createPostedApInvoice("REB-PI-1", partnerId, currencyId, new BigDecimal("800000"));
        executeRpc(GraphQLOperationType.mutation, "ErpCtRebateAgreement__runAccrual",
                ApiRequest.build(Map.of("agreementId", agreementId, "asOfDate", CoreMetrics.currentDate().toString())));
        ErpCtRebateAgreement ag = daoProvider.daoFor(ErpCtRebateAgreement.class).getEntityById(agreementId);
        assertEquals(0, new BigDecimal("800000").compareTo(ag.getTotalAccumulatedAmount()),
                "累计金额=800K");
        assertEquals(0, BigDecimal.ZERO.compareTo(ag.getEstimatedRebateAmount()),
                "0% 档预估返利=0");

        // 新增已过账 AP 发票（400K）→ 累计 1.2M，跨入 2% 档，追溯补差 → 返利 24000
        createPostedApInvoice("REB-PI-2", partnerId, currencyId, new BigDecimal("400000"));
        executeRpc(GraphQLOperationType.mutation, "ErpCtRebateAgreement__runAccrual",
                ApiRequest.build(Map.of("agreementId", agreementId, "asOfDate", CoreMetrics.currentDate().toString())));
        ag = daoProvider.daoFor(ErpCtRebateAgreement.class).getEntityById(agreementId);
        assertEquals(0, new BigDecimal("1200000").compareTo(ag.getTotalAccumulatedAmount()),
                "累计金额=1.2M");
        assertEquals(0, new BigDecimal("24000").compareTo(ag.getEstimatedRebateAmount()),
                "2% 档预估返利=24000（追溯补差）");

        // 验证计提明细总额一致
        List<ErpCtRebateAccrual> accruals = findAccruals(agreementId);
        BigDecimal accruedSum = BigDecimal.ZERO;
        for (ErpCtRebateAccrual a : accruals) {
            accruedSum = accruedSum.add(a.getAccruedRebate());
        }
        assertEquals(0, new BigDecimal("24000").compareTo(accruedSum), "计提明细总和=24000");

        // 结算：DRAFT → POSTED，生成贷项凭证（负额 AP 发票），标记计提已结算
        long settlementId = createSettlement(agreementId);
        ApiResponse<?> postResp = executeRpc(GraphQLOperationType.mutation, "ErpCtRebateSettlement__postSettlement",
                ApiRequest.build(Map.of("settlementId", settlementId)));
        assertEquals(0, postResp.getStatus(), "结算过账应成功");

        ErpCtRebateSettlement settlement = daoProvider.daoFor(ErpCtRebateSettlement.class)
                .getEntityById(settlementId);
        assertEquals("POSTED", settlement.getStatus(), "结算单状态=POSTED");
        assertEquals(0, new BigDecimal("24000").compareTo(settlement.getTotalRebateAmount()),
                "结算总额=24000");
        assertNotNull(settlement.getCreditMemoBillCode(), "贷项凭证号非空");
        assertEquals("AP_INVOICE", settlement.getCreditMemoBillType(), "PURCHASE 返利→AP 贷项");

        // 计提标记已结算
        for (ErpCtRebateAccrual a : findAccruals(agreementId)) {
            assertTrue(Boolean.TRUE.equals(a.getIsSettled()), "计提应标记 isSettled=true");
        }

        // 验证贷项（负额）AP 发票已生成
        ErpPurInvoice creditMemo = daoProvider.daoFor(ErpPurInvoice.class)
                .findFirstByQuery(eqQuery("code", settlement.getCreditMemoBillCode()));
        assertNotNull(creditMemo, "贷项 AP 发票应已生成");
        assertTrue(creditMemo.getTotalAmount().signum() < 0, "贷项发票应为负额");
    }

    @Test
    public void testSettlementIllegalTransition() {
        long[] setup = setupActiveContract("PURCHASE", "INBOUND");
        long agreementId = createRebateAgreement(setup[2], setup[0], "PURCHASE", "PERIOD_END");
        long settlementId = createSettlement(agreementId);

        // POSTED
        executeRpc(GraphQLOperationType.mutation, "ErpCtRebateSettlement__postSettlement",
                ApiRequest.build(Map.of("settlementId", settlementId)));

        // 再次 postSettlement（POSTED → POSTED）应失败
        ApiResponse<?> bad = executeRpc(GraphQLOperationType.mutation, "ErpCtRebateSettlement__postSettlement",
                ApiRequest.build(Map.of("settlementId", settlementId)));
        assertNotEquals(0, bad.getStatus(), "POSTED 状态再次过账应失败");
    }

    // ============ 前置数据构造 ============

    /**
     * @return [contractId, contractLineId, partnerId, currencyId, materialId]
     */
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
            // 建当前版本（FINALIZED）
            createVersion(contractId, 1, true, "FINALIZED");
            // NEGOTIATION → ACTIVE
            executeRpc(GraphQLOperationType.mutation, "ErpCtContract__activate",
                    ApiRequest.build(Map.of("contractId", contractId)));
            return null;
        });
        return ids;
    }

    private long createPartner() {
        ErpMdPartner p = daoProvider.daoFor(ErpMdPartner.class).newEntity();
        p.setCode("CT-REB-PARTNER-" + System.nanoTime());
        p.setName("返利测试伙伴");
        p.setPartnerType("CUSTOMER");
        p.setStatus("ACTIVE");
        daoProvider.daoFor(ErpMdPartner.class).saveEntity(p);
        return p.getId();
    }

    private long createCurrency() {
        ErpMdCurrency c = daoProvider.daoFor(ErpMdCurrency.class).newEntity();
        c.setCode("CNY-CT");
        c.setName("人民币");
        daoProvider.daoFor(ErpMdCurrency.class).saveEntity(c);
        return c.getId();
    }

    private long createUoM() {
        ErpMdUoM u = daoProvider.daoFor(ErpMdUoM.class).newEntity();
        u.setCode("PCS-CT");
        u.setName("个");
        daoProvider.daoFor(ErpMdUoM.class).saveEntity(u);
        return u.getId();
    }

    private long createMaterial(long uomId) {
        ErpMdMaterial m = daoProvider.daoFor(ErpMdMaterial.class).newEntity();
        m.setCode("MAT-CT-" + System.nanoTime());
        m.setName("测试物料");
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
        data.put("code", "CT-" + System.nanoTime());
        data.put("contractName", "返利测试合同");
        data.put("contractType", type);
        data.put("contractDirection", direction);
        data.put("partnerId", partnerId);
        data.put("currencyId", currencyId);
        data.put("startDate", "2026-01-01");
        data.put("endDate", "2027-12-31");
        data.put("status", "NEGOTIATION");
        Map<?, ?> r = (Map<?, ?>) executeRpc(GraphQLOperationType.mutation, "ErpCtContract__save",
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
        Map<?, ?> r = (Map<?, ?>) executeRpc(GraphQLOperationType.mutation, "ErpCtContractLine__save",
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
        executeRpc(GraphQLOperationType.mutation, "ErpCtContractVersion__save",
                ApiRequest.build(Map.of("data", data)));
    }

    private ApiResponse<?> saveDiscountBand(long contractLineId, BigDecimal from, BigDecimal to, BigDecimal percent) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("contractLineId", contractLineId);
        data.put("fromQty", from);
        if (to != null) {
            data.put("toQty", to);
        }
        data.put("discountPercent", percent);
        return executeRpc(GraphQLOperationType.mutation, "ErpCtVolumeDiscount__save",
                ApiRequest.build(Map.of("data", data)));
    }

    private long saveInvoicePlan(long contractLineId, BigDecimal amount) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("contractLineId", contractLineId);
        data.put("planDate", "2026-06-01");
        data.put("amount", amount);
        data.put("invoiceTerm", "MILESTONE");
        data.put("isInvoiced", false);
        Map<?, ?> r = (Map<?, ?>) executeRpc(GraphQLOperationType.mutation, "ErpCtInvoicePlan__save",
                ApiRequest.build(Map.of("data", data))).getData();
        return toLongId(r);
    }

    private long createRebateAgreement(long partnerId, long contractId, String rebateType, String method) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", "REB-AG-" + System.nanoTime());
        data.put("contractId", contractId);
        data.put("partnerId", partnerId);
        data.put("rebateType", rebateType);
        data.put("startDate", "2026-01-01");
        data.put("endDate", "2027-12-31");
        data.put("accrualMethod", method);
        data.put("status", "ACTIVE");
        Map<?, ?> r = (Map<?, ?>) executeRpc(GraphQLOperationType.mutation, "ErpCtRebateAgreement__save",
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

    private void createPostedApInvoice(String code, long supplierId, long currencyId, BigDecimal amount) {
        ErpPurInvoice invoice = daoProvider.daoFor(ErpPurInvoice.class).newEntity();
        invoice.setCode(code);
        invoice.setSupplierId(supplierId);
        invoice.setBusinessDate(LocalDate.of(2026, 6, 15));
        invoice.setCurrencyId(currencyId);
        invoice.setExchangeRate(BigDecimal.ONE);
        invoice.setTotalAmount(amount);
        invoice.setAmountSource(amount);
        invoice.setAmountFunctional(amount);
        invoice.setTotalAmountWithTax(amount);
        invoice.setDocStatus("ACTIVE");
        invoice.setApproveStatus("APPROVED");
        invoice.setPaidStatus("UNPAID");
        invoice.setPosted(true);
        daoProvider.daoFor(ErpPurInvoice.class).saveEntity(invoice);
    }

    private long createSettlement(long agreementId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("rebateAgreementId", agreementId);
        data.put("settlementDate", CoreMetrics.currentDate().toString());
        data.put("status", "DRAFT");
        Map<?, ?> r = (Map<?, ?>) executeRpc(GraphQLOperationType.mutation, "ErpCtRebateSettlement__save",
                ApiRequest.build(Map.of("data", data))).getData();
        return toLongId(r);
    }

    // ============ 查询辅助 ============

    @SuppressWarnings("unchecked")
    private List<ErpCtContractVersion> findVersions(long contractId) {
        return ormTemplate.runInSession(session -> {
            QueryBean q = new QueryBean();
            q.addFilter(eq("contractId", contractId));
            return daoProvider.daoFor(ErpCtContractVersion.class).findAllByQuery(q);
        });
    }

    @SuppressWarnings("unchecked")
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
