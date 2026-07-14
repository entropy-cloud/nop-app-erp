package app.erp.mfg.service;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.service.ErpFinConstants;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.mfg.dao.entity.ErpMfgSubcontractOrder;
import app.erp.mfg.dao.entity.ErpMfgSubcontractOrderLine;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 委外红冲测试（plan 2026-07-14-1825-1 §Phase 3）。
 *
 * <p>覆盖：
 * <ul>
 *   <li>(a) 正路径：COMPLETED 委外单 reverseCompletion → posted=false + docStatus=CANCELLED + 红字凭证存在 + 原凭证 isReversed。</li>
 *   <li>(b) 非法状态守卫：RECEIVED（未 COMPLETED）调用 → ERR_SUBCONTRACT_CANNOT_REVERSE。</li>
 *   <li>(c) 幂等/守卫：已 CANCELLED + posted=false 再调用 → 守卫拒绝 ERR_SUBCONTRACT_CANNOT_REVERSE。</li>
 *   <li>(d) ReversalListener 路径：直接红冲 SUBCONTRACT_FEE 凭证 → 委外单 posted 自动回退 false + docStatus→CANCELLED。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgSubcontractReverse extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    static final Long ORG_ID = 1601L;
    static final Long WAREHOUSE_ID = 3601L;
    static final Long UOM_ID = 5601L;
    static final Long CURRENCY_ID = 6601L;
    static final Long ACCT_SCHEMA_ID = 7601L;
    static final Long SUPPLIER_ID = 4601L;
    static final Long P = 2401L;
    static final Long M1 = 2402L;
    static final String MOVE_TYPE_INCOMING = "INCOMING";
    static final String SUBJECT_RAW = "1401";
    static final String SUBJECT_FINISHED = "1405";
    static final String SUBJECT_SUBCONTRACT = "1408";
    static final String SUBJECT_AP = "2202";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IErpFinVoucherBiz voucherBiz;

    /**
     * (a) 正路径：全链生命周期到 COMPLETED + posted=true → reverseCompletion → 状态回退 + 红字凭证存在。
     */
    @Test
    public void testReverseCompletionRollsBackPostedAndStatus() {
        seedPeriodAndSubjects();
        seedMaterial(M1, "MOVING_AVERAGE");
        seedMaterial(P, null);
        generateIncoming(M1, "PR-SC-RV-MA", bd("10"), bd("5"));

        Long orderId = seedSubcontractOrder("SUB-RV", bd("50"));
        seedSubcontractLine(9801L, orderId, M1, bd("2"));

        setConfig(ErpMfgConstants.CONFIG_SUBCONTRACT_POSTING_ENABLED, "true");
        try {
            rpcOk(mutation, "ErpMfgSubcontractOrder__submitForApproval", Map.of("id", String.valueOf(orderId)));
            rpcOk(mutation, "ErpMfgSubcontractOrder__approve", Map.of("id", String.valueOf(orderId)));

            Map<String, Object> issueReq = new LinkedHashMap<>();
            issueReq.put("subcontractOrderId", orderId);
            issueReq.put("sourceWarehouseId", WAREHOUSE_ID);
            rpcOk(mutation, "ErpMfgSubcontractOrder__issueMaterials", issueReq);

            Map<String, Object> recvReq = new LinkedHashMap<>();
            recvReq.put("subcontractOrderId", orderId);
            recvReq.put("receivedQty", bd("1"));
            recvReq.put("destWarehouseId", WAREHOUSE_ID);
            rpcOk(mutation, "ErpMfgSubcontractOrder__receiveFinished", recvReq);

            rpcOk(mutation, "ErpMfgSubcontractOrder__postProcessingFee", Map.of("subcontractOrderId", orderId));
            assertEquals(ErpMfgConstants.SUBCONTRACT_STATUS_COMPLETED, statusOf(orderId));
            assertTrue(Boolean.TRUE.equals(reload(orderId).getPosted()), "前置：加工费过账后 posted=true");

            ErpFinVoucher originalFee = findVoucher("SUB-RV-SF", ErpFinBusinessType.SUBCONTRACT_FEE);
            assertNotNull(originalFee, "前置：应存在 SUBCONTRACT_FEE 凭证");

            rpcOk(mutation, "ErpMfgSubcontractOrder__reverseCompletion", Map.of("subcontractOrderId", orderId));

            ErpMfgSubcontractOrder reversed = reload(orderId);
            assertEquals(ErpMfgConstants.SUBCONTRACT_STATUS_CANCELLED, reversed.getDocStatus(),
                    "红冲后 docStatus→CANCELLED");
            assertFalse(Boolean.TRUE.equals(reversed.getPosted()), "红冲后 posted=false");
            assertEquals(null, reversed.getPostedAt(), "红冲后 postedAt 清空");

            ErpFinVoucher originalAfter = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(originalFee.getId());
            assertTrue(Boolean.TRUE.equals(originalAfter.getIsReversed()),
                    "原 SUBCONTRACT_FEE 凭证应被标记 isReversed=true");

            ErpFinVoucher redVoucher = findReversalVoucher("SUB-RV-SF", ErpFinBusinessType.SUBCONTRACT_FEE);
            assertNotNull(redVoucher, "应存在 SUBCONTRACT_FEE 红字冲销凭证");
            assertEquals(ErpFinConstants.POSTING_TYPE_REVERSAL, redVoucher.getPostingType(),
                    "红字凭证 postingType=REVERSAL");
        } finally {
            setConfig(ErpMfgConstants.CONFIG_SUBCONTRACT_POSTING_ENABLED, "false");
        }
    }

    /**
     * (b) 非法状态守卫：RECEIVED（未 COMPLETED）调用红冲 → ERR_SUBCONTRACT_CANNOT_REVERSE。
     */
    @Test
    public void testReverseCompletionRejectsNonCompleted() {
        seedPeriodAndSubjects();
        seedMaterial(M1, "MOVING_AVERAGE");
        seedMaterial(P, null);
        generateIncoming(M1, "PR-SC-ILL-MA", bd("10"), bd("5"));

        Long orderId = seedSubcontractOrder("SUB-ILL-RV", bd("50"));
        seedSubcontractLine(9811L, orderId, M1, bd("2"));

        setConfig(ErpMfgConstants.CONFIG_SUBCONTRACT_POSTING_ENABLED, "true");
        try {
            rpcOk(mutation, "ErpMfgSubcontractOrder__submitForApproval", Map.of("id", String.valueOf(orderId)));
            rpcOk(mutation, "ErpMfgSubcontractOrder__approve", Map.of("id", String.valueOf(orderId)));
            Map<String, Object> issueReq = new LinkedHashMap<>();
            issueReq.put("subcontractOrderId", orderId);
            issueReq.put("sourceWarehouseId", WAREHOUSE_ID);
            rpcOk(mutation, "ErpMfgSubcontractOrder__issueMaterials", issueReq);
            Map<String, Object> recvReq = new LinkedHashMap<>();
            recvReq.put("subcontractOrderId", orderId);
            recvReq.put("receivedQty", bd("1"));
            recvReq.put("destWarehouseId", WAREHOUSE_ID);
            rpcOk(mutation, "ErpMfgSubcontractOrder__receiveFinished", recvReq);
            assertEquals(ErpMfgConstants.SUBCONTRACT_STATUS_RECEIVED, statusOf(orderId));

            ApiResponse<?> resp = rpc(mutation, "ErpMfgSubcontractOrder__reverseCompletion",
                    Map.of("subcontractOrderId", orderId));
            assertEquals(ErpMfgErrors.ERR_SUBCONTRACT_CANNOT_REVERSE.getErrorCode(), resp.getCode(),
                    "RECEIVED 状态红冲应被守卫拒绝");
        } finally {
            setConfig(ErpMfgConstants.CONFIG_SUBCONTRACT_POSTING_ENABLED, "false");
        }
    }

    /**
     * (c) 幂等/守卫：已 CANCELLED + posted=false 再调用 → 守卫拒绝 ERR_SUBCONTRACT_CANNOT_REVERSE。
     */
    @Test
    public void testReverseCompletionIdempotentGuard() {
        seedPeriodAndSubjects();

        Long orderId = seedSubcontractOrder("SUB-IDEMP", bd("50"));
        ormTemplate.runInSession(() -> {
            ErpMfgSubcontractOrder order = daoProvider.daoFor(ErpMfgSubcontractOrder.class).getEntityById(orderId);
            order.setDocStatus(ErpMfgConstants.SUBCONTRACT_STATUS_CANCELLED);
            order.setPosted(false);
            order.setPostedAt(null);
            daoProvider.daoFor(ErpMfgSubcontractOrder.class).updateEntity(order);
        });

        ApiResponse<?> resp = rpc(mutation, "ErpMfgSubcontractOrder__reverseCompletion",
                Map.of("subcontractOrderId", orderId));
        assertEquals(ErpMfgErrors.ERR_SUBCONTRACT_CANNOT_REVERSE.getErrorCode(), resp.getCode(),
                "已 CANCELLED + posted=false 再红冲应被守卫拒绝（幂等）");
    }

    /**
     * (d) ReversalListener 路径：直接红冲 SUBCONTRACT_FEE 凭证 → 委外单 posted 自动回退 false + docStatus→CANCELLED。
     */
    @Test
    public void testFinanceReverseVoucherRollsBackSubcontractOrder() {
        seedPeriodAndSubjects();

        Long orderId = seedSubcontractOrder("SUB-LISTEN", bd("50"));
        String feeBillHeadCode = "SUB-LISTEN-SF";
        ormTemplate.runInSession(() -> {
            ErpMfgSubcontractOrder order = daoProvider.daoFor(ErpMfgSubcontractOrder.class).getEntityById(orderId);
            order.setDocStatus(ErpMfgConstants.SUBCONTRACT_STATUS_COMPLETED);
            order.setPosted(true);
            order.setPostedAt(CoreMetrics.currentDateTime());
            daoProvider.daoFor(ErpMfgSubcontractOrder.class).updateEntity(order);
        });

        seedPostedVoucherFor(feeBillHeadCode, ErpFinBusinessType.SUBCONTRACT_FEE, bd("50"));

        assertTrue(Boolean.TRUE.equals(reload(orderId).getPosted()), "前置：委外单 posted=true");

        Long redVoucherId = voucherBiz.reverse(feeBillHeadCode, ErpFinBusinessType.SUBCONTRACT_FEE, CTX);
        assertNotNull(redVoucherId, "财务侧红冲应生成红字凭证");

        ErpMfgSubcontractOrder rolled = reload(orderId);
        assertFalse(Boolean.TRUE.equals(rolled.getPosted()),
                "方向二：财务红冲后委外单 posted 应被监听者回退为 false");
        assertEquals(ErpMfgConstants.SUBCONTRACT_STATUS_CANCELLED, rolled.getDocStatus(),
                "方向二：财务红冲后委外单 docStatus→CANCELLED");
    }

    // ---------- helpers ----------

    private String statusOf(Long orderId) {
        return reload(orderId).getDocStatus();
    }

    private ErpMfgSubcontractOrder reload(Long orderId) {
        return daoProvider.daoFor(ErpMfgSubcontractOrder.class).getEntityById(orderId);
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
            period.setCode("2026-07-SC");
            period.setName("2026-07-SC");
            period.setOrgId(ORG_ID);
            period.orm_propValueByName("year", 2026);
            period.orm_propValueByName("month", 7);
            period.setStartDate(LocalDate.of(2026, 7, 1));
            period.setEndDate(LocalDate.of(2026, 7, 31));
            period.orm_propValueByName("status", "OPEN");
            pdao.saveEntity(period);

            seedSubject(SUBJECT_RAW, "原材料", "ASSET", "DEBIT");
            seedSubject(SUBJECT_FINISHED, "库存商品", "ASSET", "DEBIT");
            seedSubject(SUBJECT_SUBCONTRACT, "委外物资", "ASSET", "DEBIT");
            seedSubject(SUBJECT_AP, "应付账款", "LIABILITY", "CREDIT");
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

    private void seedMaterial(Long id, String costMethod) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            ErpMdMaterial m = new ErpMdMaterial();
            m.orm_propValueByName("id", id);
            m.setCode("MAT-" + id);
            m.setName("Material " + id);
            m.orm_propValueByName("materialType", "GOODS");
            m.setUoMId(UOM_ID);
            m.setStatus("ACTIVE");
            m.setCostMethod(costMethod);
            dao.saveEntity(m);
        });
    }

    private void generateIncoming(Long materialId, String billCode, BigDecimal qty, BigDecimal unitCost) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("moveType", MOVE_TYPE_INCOMING);
        req.put("orgId", ORG_ID);
        req.put("businessDate", "2026-07-01");
        req.put("acctSchemaId", ACCT_SCHEMA_ID);
        req.put("currencyId", CURRENCY_ID);
        req.put("destWarehouseId", WAREHOUSE_ID);
        req.put("relatedBillType", "PUR_RECEIPT");
        req.put("relatedBillCode", billCode);
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("materialId", materialId);
        line.put("uoMId", UOM_ID);
        line.put("quantity", qty);
        line.put("unitCost", unitCost);
        line.put("currencyId", CURRENCY_ID);
        req.put("lines", Collections.singletonList(line));
        rpcOk(mutation, "ErpInvStockMove__generateMove", Map.of("request", req));
    }

    private Long seedSubcontractOrder(String code, BigDecimal processingFee) {
        Long id = 8700L + (long) Math.abs(code.hashCode() % 500);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgSubcontractOrder> dao = daoProvider.daoFor(ErpMfgSubcontractOrder.class);
            ErpMfgSubcontractOrder order = new ErpMfgSubcontractOrder();
            order.orm_propValueByName("id", id);
            order.setCode(code);
            order.setOrgId(ORG_ID);
            order.setSupplierId(SUPPLIER_ID);
            order.setProductId(P);
            order.setBusinessDate(LocalDate.of(2026, 7, 1));
            order.setCurrencyId(CURRENCY_ID);
            order.setExchangeRate(BigDecimal.ONE);
            order.setProcessingFee(processingFee);
            order.setTotalAmount(processingFee);
            order.setDocStatus(ErpMfgConstants.SUBCONTRACT_STATUS_DRAFT);
            order.setApproveStatus(ErpMfgConstants.APPROVE_STATUS_UNSUBMITTED);
            order.orm_propValueByName("postedStatus", "DRAFT");
            dao.saveEntity(order);
        });
        return id;
    }

    private void seedSubcontractLine(Long id, Long orderId, Long materialId, BigDecimal qty) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgSubcontractOrderLine> dao = daoProvider.daoFor(ErpMfgSubcontractOrderLine.class);
            ErpMfgSubcontractOrderLine line = new ErpMfgSubcontractOrderLine();
            line.orm_propValueByName("id", id);
            line.setSubcontractOrderId(orderId);
            line.setLineNo(10);
            line.setMaterialId(materialId);
            line.setUoMId(UOM_ID);
            line.setQuantity(qty);
            dao.saveEntity(line);
        });
    }

    private ErpFinVoucher findVoucher(String billHeadCode, ErpFinBusinessType type) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billHeadCode), eq("businessType", type.name())));
        List<ErpFinVoucherBillR> links = dao.findAllByQuery(q);
        if (links.isEmpty()) {
            return null;
        }
        return daoProvider.daoFor(ErpFinVoucher.class).getEntityById(links.get(0).getVoucherId());
    }

    private ErpFinVoucher findReversalVoucher(String billHeadCode, ErpFinBusinessType type) {
        IEntityDao<ErpFinVoucher> dao = daoProvider.daoFor(ErpFinVoucher.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("postingType", ErpFinConstants.POSTING_TYPE_REVERSAL));
        List<ErpFinVoucher> list = dao.findAllByQuery(q);
        for (ErpFinVoucher v : list) {
            ErpFinVoucherBillR link = findBillR(v.getId(), billHeadCode, type);
            if (link != null) {
                return v;
            }
        }
        return null;
    }

    private ErpFinVoucherBillR findBillR(Long voucherId, String billHeadCode, ErpFinBusinessType type) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("voucherId", voucherId),
                and(eq("billCode", billHeadCode), eq("businessType", type.name()))));
        List<ErpFinVoucherBillR> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    /** 直接构造已过账凭证 + 业财回链（绕过过账引擎，模拟"已存在过账结果"的最小前置态）。 */
    private Long seedPostedVoucherFor(String billHeadCode, ErpFinBusinessType businessType, BigDecimal total) {
        IEntityDao<ErpFinVoucher> vDao = daoProvider.daoFor(ErpFinVoucher.class);
        IEntityDao<ErpFinVoucherBillR> billRDao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        IEntityDao<ErpFinAccountingPeriod> periodDao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        QueryBean pq = new QueryBean();
        pq.addFilter(eq("code", "2026-07-SC"));
        pq.setLimit(1);
        ErpFinAccountingPeriod period = periodDao.findAllByQuery(pq).get(0);
        return ormTemplate.runInSession(session -> {
            ErpFinVoucher voucher = new ErpFinVoucher();
            voucher.setCode("PST-SEED-" + billHeadCode);
            voucher.setVoucherType("TRANSFER");
            voucher.setPostingType(ErpFinConstants.POSTING_TYPE_NORMAL);
            voucher.setVoucherDate(LocalDate.of(2026, 7, 1));
            voucher.setOrgId(ORG_ID);
            voucher.setAcctSchemaId(ACCT_SCHEMA_ID);
            voucher.setPeriodId(period.getId());
            voucher.setTotalDebit(total);
            voucher.setTotalCredit(total);
            voucher.setIsReversed(false);
            voucher.setDocStatus(ErpFinConstants.VOUCHER_STATUS_POSTED);
            voucher.setPostedAt(CoreMetrics.currentDateTime());
            vDao.saveEntity(voucher);

            ErpFinVoucherBillR billR = new ErpFinVoucherBillR();
            billR.setVoucherId(voucher.getId());
            billR.setBillType(businessType.name());
            billR.setBillCode(billHeadCode);
            billR.setBusinessType(businessType.name());
            billRDao.saveEntity(billR);
            return voucher.getId();
        });
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private void rpcOk(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        ApiResponse<?> resp = rpc(op, action, args);
        assertEquals(0, resp.getStatus(), action + " 应成功: " + resp);
    }

    private void setConfig(String key, String value) {
        io.nop.api.core.config.AppConfig.getConfigProvider().assignConfigValue(key, value);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
