package app.erp.pur.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.dao.entity.ErpPurInvoiceLine;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.dao.entity.ErpPurReceiveLine;
import app.erp.pur.dao.entity.ErpPurReturn;
import app.erp.pur.dao.entity.ErpPurReturnLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * RC-R1.12 退货侧承付冲销恢复集成测试（plan 2026-08-08-1603-2，budget.md §承付会计 §3 冲销恢复语义，退货侧扩展）。
 *
 * <p>验证 {@code ErpPurReturnReverseApproveProcessor.reverseApprove} + {@code ErpPurReturnCancelProcessor.cancel}
 * 对源 PO 重新生成 COMMITMENT 凭证（对称恢复正向 release-on-return）：
 * <ul>
 *   <li>正向链闭合：PO approve → C1；return approve（双开关 ON）→ release（C1 isReversed=true）；
 *       return reverseApprove/cancel → 新 C2 + <b>恰 1 张活跃凭证</b>。</li>
 *   <li>恢复前置守卫（防幽灵承付/双占用）：子开关 OFF（总开关 ON）/ 总开关 OFF / 从未 APPROVED 取消 /
 *       无关联 PO 时零新凭证。</li>
 *   <li>跨路径交互：return approve（releaseIfPresent 全额释放）后 invoice approve（容错跳过）→
 *       invoice reverseApprove 恢复 → 恰 1 张活跃凭证。</li>
 * </ul>
 *
 * <p>config 驱动：{@code return-commitment-test.yaml}（总开关 + 子开关 ON + 科目 1408）；开关关闭场景经
 * {@code AppConfig} 运行时翻转。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:return-commitment-test.yaml")
public class TestErpPurReturnCommitmentRestore extends JunitAutoTestCase {

    static final Long ORG_ID = 11101L;
    static final Long SUPPLIER_ID = 11201L;
    static final Long WAREHOUSE_ID = 11301L;
    static final Long MATERIAL_ID = 11401L;
    static final Long UOM_ID = 11501L;
    static final Long CURRENCY_ID = 11601L;
    static final Long ACCT_SCHEMA_ID = 11701L;
    static final String COMMITMENT_BILL_TYPE = "PURCHASE_ORDER_COMMITMENT";
    static final String POSTING_TYPE_COMMITMENT = "COMMITMENT";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    private final AtomicLong idSeq = new AtomicLong(1100000L);

    @Test
    public void testReturnReverseApproveRestoresCommitment() {
        seedPrereqs();
        long orderLineId = nextId();
        long receiveId = nextId();
        long receiveLineId = nextId();
        Long orderId = newOrder("PO-RTR-001", orderLineId);
        Long receiveId2 = seedApprovedReceive("PR-RTR-001", receiveId, receiveLineId, orderId, orderLineId);

        assertEquals(0, submitOrder(orderId).getStatus());
        assertEquals(0, approveOrder(orderId).getStatus());
        assertEquals(1, countActiveCommitments("PO-RTR-001"), "PO 审核后恰 1 张活跃承付凭证");

        // return approve（双开关 ON）→ release-on-return（C1 红冲，生成红冲凭证）
        Long returnId = newReturn("RT-RTR-001", receiveId2, receiveLineId);
        assertEquals(0, approveReturn(returnId).getStatus(), "退货审核应成功");
        List<ErpFinVoucherBillR> links = commitmentLinks("PO-RTR-001");
        assertEquals(2, links.size(), "退货审核释放后 = 原凭证 + 红冲凭证");
        assertEquals(0, countActiveCommitments("PO-RTR-001"), "退货审核释放后无活跃凭证");

        // return reverseApprove → 恢复 C2
        assertEquals(0, reverseApproveReturn(returnId).getStatus(), "退货反审核应成功");
        links = commitmentLinks("PO-RTR-001");
        assertEquals(3, links.size(), "冲销恢复后共 3 张凭证（C1 + 红冲 + C2 恢复）");
        ErpFinVoucher restored = activeVoucherOf(links);
        assertNotNull(restored, "冲销恢复后应存在 1 张活跃承付凭证（单活跃不变量）");
        assertEquals(POSTING_TYPE_COMMITMENT, restored.getPostingType(), "恢复凭证 postingType=COMMITMENT");
        assertFalse(Boolean.TRUE.equals(restored.getIsReversed()), "恢复凭证 isReversed=false");
        assertEquals(1, countActiveCommitments("PO-RTR-001"), "冲销恢复后恰 1 张活跃凭证");
    }

    @Test
    public void testReturnCancelRestoresCommitment() {
        seedPrereqs();
        long orderLineId = nextId();
        long receiveId = nextId();
        long receiveLineId = nextId();
        Long orderId = newOrder("PO-RTC-001", orderLineId);
        Long receiveId2 = seedApprovedReceive("PR-RTC-001", receiveId, receiveLineId, orderId, orderLineId);

        assertEquals(0, submitOrder(orderId).getStatus());
        assertEquals(0, approveOrder(orderId).getStatus());
        assertEquals(1, countActiveCommitments("PO-RTC-001"));

        Long returnId = newReturn("RT-RTC-001", receiveId2, receiveLineId);
        assertEquals(0, approveReturn(returnId).getStatus());
        assertEquals(0, countActiveCommitments("PO-RTC-001"), "退货审核释放后无活跃凭证");

        // return cancel 路径同型恢复
        assertEquals(0, cancelReturn(returnId).getStatus(), "退货作废应成功");
        List<ErpFinVoucherBillR> links = commitmentLinks("PO-RTC-001");
        assertEquals(3, links.size(), "作废恢复后共 3 张凭证（C1 + 红冲 + C2 恢复）");
        ErpFinVoucher restored = activeVoucherOf(links);
        assertNotNull(restored, "作废恢复后应存在 1 张活跃承付凭证（单活跃不变量）");
        assertEquals(POSTING_TYPE_COMMITMENT, restored.getPostingType());
        assertFalse(Boolean.TRUE.equals(restored.getIsReversed()));
        assertEquals(1, countActiveCommitments("PO-RTC-001"), "作废恢复后恰 1 张活跃凭证");
    }

    @Test
    public void testSubSwitchOffNoRestore() {
        // 子开关 OFF（总开关 ON）：正向从未 release → 冲销恢复必须同开关门控，否则幽灵承付双占用
        seedPrereqs();
        long orderLineId = nextId();
        long receiveId = nextId();
        long receiveLineId = nextId();
        Long orderId = newOrder("PO-RTS-001", orderLineId);
        Long receiveId2 = seedApprovedReceive("PR-RTS-001", receiveId, receiveLineId, orderId, orderLineId);

        assertEquals(0, submitOrder(orderId).getStatus());
        assertEquals(0, approveOrder(orderId).getStatus());
        assertEquals(1, countActiveCommitments("PO-RTS-001"));

        AppConfig.getConfigProvider().assignConfigValue(
                ErpFinConstants.CONFIG_BUDGET_COMMITMENT_RELEASE_ON_RETURN, Boolean.FALSE);
        try {
            Long returnId = newReturn("RT-RTS-001", receiveId2, receiveLineId);
            assertEquals(0, approveReturn(returnId).getStatus());
            assertEquals(1, countActiveCommitments("PO-RTS-001"), "子开关 OFF 时退货审核不释放");
            assertEquals(0, reverseApproveReturn(returnId).getStatus());
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpFinConstants.CONFIG_BUDGET_COMMITMENT_RELEASE_ON_RETURN, Boolean.TRUE);
        }
        assertEquals(1, commitmentLinks("PO-RTS-001").size(), "子开关 OFF 时冲销恢复零新凭证（防幽灵承付）");
        assertEquals(1, countActiveCommitments("PO-RTS-001"), "C1 保持唯一活跃凭证");
    }

    @Test
    public void testTotalSwitchOffNoRestore() {
        // 总开关 OFF：全链零承付凭证
        AppConfig.getConfigProvider().assignConfigValue(
                ErpFinConstants.CONFIG_BUDGET_COMMITMENT_ENABLED, Boolean.FALSE);
        try {
            seedPrereqs();
            long orderLineId = nextId();
            long receiveId = nextId();
            long receiveLineId = nextId();
            Long orderId = newOrder("PO-RTO-001", orderLineId);
            Long receiveId2 = seedApprovedReceive("PR-RTO-001", receiveId, receiveLineId, orderId, orderLineId);

            assertEquals(0, submitOrder(orderId).getStatus());
            assertEquals(0, approveOrder(orderId).getStatus());
            assertEquals(0, countActiveCommitments("PO-RTO-001"), "总开关 OFF 时 PO 审核零承付凭证");

            Long returnId = newReturn("RT-RTO-001", receiveId2, receiveLineId);
            assertEquals(0, approveReturn(returnId).getStatus());
            assertEquals(0, reverseApproveReturn(returnId).getStatus());
            assertEquals(0, commitmentLinks("PO-RTO-001").size(), "总开关 OFF 时冲销恢复零凭证");
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpFinConstants.CONFIG_BUDGET_COMMITMENT_ENABLED, Boolean.TRUE);
        }
    }

    @Test
    public void testCancelNeverApprovedNoRestore() {
        // 从未 APPROVED 的 return cancel → wasApproved=false → 零恢复（防幽灵承付双占用）
        seedPrereqs();
        long orderLineId = nextId();
        long receiveId = nextId();
        long receiveLineId = nextId();
        Long orderId = newOrder("PO-RTN-001", orderLineId);
        Long receiveId2 = seedApprovedReceive("PR-RTN-001", receiveId, receiveLineId, orderId, orderLineId);

        assertEquals(0, submitOrder(orderId).getStatus());
        assertEquals(0, approveOrder(orderId).getStatus());
        assertEquals(1, countActiveCommitments("PO-RTN-001"));

        Long returnId = newReturn("RT-RTN-001", receiveId2, receiveLineId);
        assertEquals(0, cancelReturn(returnId).getStatus());
        assertEquals(1, commitmentLinks("PO-RTN-001").size(), "从未 APPROVED 取消不恢复（零新凭证）");
        assertEquals(1, countActiveCommitments("PO-RTN-001"), "C1 保持唯一活跃凭证");
    }

    @Test
    public void testNoLinkedPoNoRestore() {
        // return.receive.orderId = null → resolvePurchaseOrderCode 无法解析 → 零恢复
        seedPrereqs();
        long orderLineId = nextId();
        long receiveId = nextId();
        long receiveLineId = nextId();
        Long orderId = newOrder("PO-RNL-001", orderLineId);
        seedApprovedReceive("PR-RNL-001", receiveId, receiveLineId, orderId, orderLineId);
        assertEquals(0, submitOrder(orderId).getStatus());
        assertEquals(0, approveOrder(orderId).getStatus());
        assertEquals(1, countActiveCommitments("PO-RNL-001"), "C1 已生成");

        // 无订单关联的已审核入库单（orderId=null，approveStatus 直置 APPROVED）→ 退货 approve/reverseApprove 均不动作
        long orphanReceiveId = nextId();
        long orphanReceiveLineId = nextId();
        ormTemplate.runInSession(session -> {
            newReceive("PR-RNL-ORPHAN", orphanReceiveId, null);
            ErpPurReceive r = daoProvider.daoFor(ErpPurReceive.class).getEntityById(orphanReceiveId);
            r.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
            daoProvider.daoFor(ErpPurReceive.class).saveOrUpdateEntity(r);
            newReceiveLine(orphanReceiveLineId, orphanReceiveId, null, new BigDecimal("10"), new BigDecimal("5"));
            return null;
        });
        Long orphanReturnId = newReturn("RT-RNL-ORPHAN", orphanReceiveId, orphanReceiveLineId);
        assertEquals(0, approveReturn(orphanReturnId).getStatus());
        assertEquals(0, reverseApproveReturn(orphanReturnId).getStatus());
        assertEquals(1, commitmentLinks("PO-RNL-001").size(), "无关联 PO 的退货冲销零新凭证");
        assertEquals(1, countActiveCommitments("PO-RNL-001"), "C1 保持唯一活跃凭证");
    }

    @Test
    public void testInvoiceReverseAfterReturnReleaseSingleActive() {
        // 跨路径交互：return approve（releaseIfPresent 已全额释放）→ invoice approve（容错跳过）→
        // invoice reverseApprove 恢复 → 恰 1 张活跃凭证（单活跃不变量）
        seedPrereqs();
        long orderLineId = nextId();
        long receiveId = nextId();
        long receiveLineId = nextId();
        Long orderId = newOrder("PO-RTI-001", orderLineId);
        Long receiveId2 = seedApprovedReceive("PR-RTI-001", receiveId, receiveLineId, orderId, orderLineId);

        assertEquals(0, submitOrder(orderId).getStatus());
        assertEquals(0, approveOrder(orderId).getStatus());
        assertEquals(1, countActiveCommitments("PO-RTI-001"));

        Long returnId = newReturn("RT-RTI-001", receiveId2, receiveLineId);
        assertEquals(0, approveReturn(returnId).getStatus(), "退货审核应成功（releaseIfPresent 全额释放）");
        assertEquals(0, countActiveCommitments("PO-RTI-001"), "退货审核后无活跃凭证");

        // 发票审核（释放容错跳过——无活跃凭证可红冲）+ 冲销恢复
        Long invoiceId = newInvoice("PI-RTI-001", receiveLineId);
        assertEquals(0, submitInvoice(invoiceId).getStatus());
        assertEquals(0, approveInvoice(invoiceId).getStatus());
        assertEquals(0, reverseApproveInvoice(invoiceId).getStatus());

        List<ErpFinVoucherBillR> links = commitmentLinks("PO-RTI-001");
        assertEquals(3, links.size(), "凭证总数 = C1 + 红冲 + C2（恢复）");
        ErpFinVoucher restored = activeVoucherOf(links);
        assertNotNull(restored, "恢复后应恰 1 张活跃凭证（单活跃不变量）");
        assertEquals(POSTING_TYPE_COMMITMENT, restored.getPostingType());
        assertFalse(Boolean.TRUE.equals(restored.getIsReversed()));
        assertEquals(1, countActiveCommitments("PO-RTI-001"), "恢复后恰 1 张活跃凭证");
    }

    // ---------- seed ----------

    private void seedPrereqs() {
        ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "OPEN");
            // 承付科目 1408 + PURCHASE_INPUT（1401/2202）+ PURCHASE_RETURN（2202/1401）+ AP_INVOICE（1403/2221/2202）
            seedSubject("1408", "承付占用科目", "EXPENSE", "DEBIT");
            seedSubject("1401", "库存商品", "ASSET", "DEBIT");
            seedSubject("2202", "应付账款-暂估", "LIABILITY", "CREDIT");
            seedSubject("1403", "在途物资", "ASSET", "DEBIT");
            seedSubject("2221", "应交税费-进项税额", "LIABILITY", "CREDIT");
            seedAcctSchema();
            seedActiveSupplier();
            return null;
        });
    }

    private void seedAcctSchema() {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = new ErpMdAcctSchema();
        schema.setId(ACCT_SCHEMA_ID);
        schema.setCode("AS-" + ORG_ID);
        schema.setName("账套" + ORG_ID);
        schema.setOrgId(ORG_ID);
        schema.setNature("FINANCIAL");
        schema.setFunctionalCurrencyId(CURRENCY_ID);
        schema.setStatus("ACTIVE");
        dao.saveEntity(schema);
    }

    private void seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end, String status) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(code);
        period.setName(code);
        period.setOrgId(ORG_ID);
        period.setYear(year);
        period.setMonth(month);
        period.setStartDate(start);
        period.setEndDate(end);
        period.setStatus(status);
        dao.saveEntity(period);
    }

    private void seedSubject(String code, String name, String subjectClass, String direction) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.setSubjectClass(subjectClass);
        subject.setDirection(direction);
        subject.setStatus("ACTIVE");
        dao.saveEntity(subject);
    }

    private void seedActiveSupplier() {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(SUPPLIER_ID);
        partner.setCode("SUP-" + SUPPLIER_ID);
        partner.setName("供应商" + SUPPLIER_ID);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus(ErpPurConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(partner);
    }

    // ---------- entity builders ----------

    private Long newOrder(String code, long orderLineId) {
        return ormTemplate.runInSession(session -> {
            IEntityDao<ErpPurOrder> dao = daoProvider.daoFor(ErpPurOrder.class);
            ErpPurOrder order = new ErpPurOrder();
            order.setCode(code);
            order.setOrgId(ORG_ID);
            order.setSupplierId(SUPPLIER_ID);
            order.setWarehouseId(WAREHOUSE_ID);
            order.setBusinessDate(LocalDate.of(2026, 7, 1));
            order.setCurrencyId(CURRENCY_ID);
            order.setExchangeRate(new BigDecimal("1"));
            order.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
            order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
            order.setReceiveStatus(ErpPurConstants.RECEIVE_STATUS_UNRECEIVED);
            order.setPosted(false);
            order.setAmountSource(new BigDecimal("50"));
            order.setAmountFunctional(new BigDecimal("50"));
            order.setTotalAmount(new BigDecimal("50"));
            order.setTotalTaxAmount(BigDecimal.ZERO);
            order.setTotalAmountWithTax(new BigDecimal("50"));
            dao.saveEntity(order);

            IEntityDao<ErpPurOrderLine> lineDao = daoProvider.daoFor(ErpPurOrderLine.class);
            ErpPurOrderLine line = new ErpPurOrderLine();
            line.setId(orderLineId);
            line.setOrderId(order.getId());
            line.setLineNo(1);
            line.setMaterialId(MATERIAL_ID);
            line.setUoMId(UOM_ID);
            line.setQuantity(new BigDecimal("10"));
            line.setUnitPrice(new BigDecimal("5"));
            line.setAmount(new BigDecimal("50"));
            lineDao.saveEntity(line);
            return order.getId();
        });
    }

    /**
     * 创建已审核入库单（approveStatus=SUBMITTED → ErpPurReceive__approve）。
     *
     * @return receiveId
     */
    private Long seedApprovedReceive(String code, long receiveId, long receiveLineId, Long orderId, long orderLineId) {
        ormTemplate.runInSession(session -> {
            newReceive(code, receiveId, orderId);
            newReceiveLine(receiveLineId, receiveId, orderLineId, new BigDecimal("10"), new BigDecimal("5"));
            return null;
        });
        assertEquals(0, approveReceive(receiveId).getStatus(), "源入库单审核应成功");
        return receiveId;
    }

    private void newReceive(String code, Long receiveId, Long orderId) {
        IEntityDao<ErpPurReceive> dao = daoProvider.daoFor(ErpPurReceive.class);
        ErpPurReceive receive = new ErpPurReceive();
        receive.setId(receiveId);
        receive.setCode(code);
        receive.setOrgId(ORG_ID);
        receive.setOrderId(orderId);
        receive.setSupplierId(SUPPLIER_ID);
        receive.setWarehouseId(WAREHOUSE_ID);
        receive.setBusinessDate(LocalDate.of(2026, 7, 1));
        receive.setCurrencyId(CURRENCY_ID);
        receive.setExchangeRate(new BigDecimal("1"));
        receive.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        receive.setApproveStatus(ErpPurConstants.APPROVE_STATUS_SUBMITTED);
        receive.setReceiveStatus(ErpPurConstants.RECEIVE_STATUS_UNRECEIVED);
        receive.setPosted(false);
        dao.saveEntity(receive);
    }

    private void newReceiveLine(Long lineId, Long receiveId, Long orderLineId, BigDecimal qty, BigDecimal unitPrice) {
        IEntityDao<ErpPurReceiveLine> dao = daoProvider.daoFor(ErpPurReceiveLine.class);
        ErpPurReceiveLine line = new ErpPurReceiveLine();
        line.setId(lineId);
        line.setReceiveId(receiveId);
        line.setLineNo(1);
        line.setOrderLineId(orderLineId);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitPrice(unitPrice);
        dao.saveEntity(line);
    }

    private Long newReturn(String code, Long receiveId, Long receiveLineId) {
        return ormTemplate.runInSession(session -> {
            Long returnId = nextId();
            IEntityDao<ErpPurReturn> dao = daoProvider.daoFor(ErpPurReturn.class);
            ErpPurReturn returnOrder = new ErpPurReturn();
            returnOrder.setId(returnId);
            returnOrder.setCode(code);
            returnOrder.setOrgId(ORG_ID);
            returnOrder.setReceiveId(receiveId);
            returnOrder.setSupplierId(SUPPLIER_ID);
            returnOrder.setWarehouseId(WAREHOUSE_ID);
            returnOrder.setBusinessDate(LocalDate.of(2026, 7, 2));
            returnOrder.setCurrencyId(CURRENCY_ID);
            returnOrder.setExchangeRate(new BigDecimal("1"));
            returnOrder.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
            returnOrder.setApproveStatus(ErpPurConstants.APPROVE_STATUS_SUBMITTED);
            returnOrder.setTotalAmount(new BigDecimal("15"));
            returnOrder.setPosted(false);
            dao.saveEntity(returnOrder);

            IEntityDao<ErpPurReturnLine> lineDao = daoProvider.daoFor(ErpPurReturnLine.class);
            ErpPurReturnLine line = new ErpPurReturnLine();
            line.setId(nextId());
            line.setReturnId(returnId);
            line.setLineNo(1);
            line.setReceiveLineId(receiveLineId);
            line.setMaterialId(MATERIAL_ID);
            line.setUoMId(UOM_ID);
            line.setQuantity(new BigDecimal("3"));
            line.setUnitPrice(new BigDecimal("5"));
            line.setAmount(new BigDecimal("15"));
            line.setReason("质量不合格");
            lineDao.saveEntity(line);
            return returnId;
        });
    }

    private Long newInvoice(String code, Long receiveLineId) {
        return ormTemplate.runInSession(session -> {
            IEntityDao<ErpPurInvoice> dao = daoProvider.daoFor(ErpPurInvoice.class);
            ErpPurInvoice invoice = new ErpPurInvoice();
            invoice.setCode(code);
            invoice.setOrgId(ORG_ID);
            invoice.setSupplierId(SUPPLIER_ID);
            invoice.setBusinessDate(LocalDate.of(2026, 7, 1));
            invoice.setCurrencyId(CURRENCY_ID);
            invoice.setExchangeRate(new BigDecimal("1"));
            invoice.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
            invoice.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
            invoice.setPaidStatus(ErpPurConstants.PAID_STATUS_UNPAID);
            invoice.setPaidAmount(BigDecimal.ZERO);
            invoice.setTotalAmount(new BigDecimal("50"));
            invoice.setTotalTaxAmount(new BigDecimal("6.5"));
            invoice.setTotalAmountWithTax(new BigDecimal("56.5"));
            invoice.setPosted(false);
            dao.saveEntity(invoice);

            IEntityDao<ErpPurInvoiceLine> lineDao = daoProvider.daoFor(ErpPurInvoiceLine.class);
            ErpPurInvoiceLine line = new ErpPurInvoiceLine();
            line.setInvoiceId(invoice.getId());
            line.setReceiveLineId(receiveLineId);
            line.setLineNo(1);
            line.setMaterialId(MATERIAL_ID);
            line.setUoMId(UOM_ID);
            line.setQuantity(new BigDecimal("10"));
            line.setUnitPrice(new BigDecimal("5"));
            line.setTaxRate(new BigDecimal("13"));
            lineDao.saveEntity(line);
            return invoice.getId();
        });
    }

    // ---------- assertion helpers ----------

    private List<ErpFinVoucherBillR> commitmentLinks(String poCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", poCode));
        q.addFilter(eq("billType", COMMITMENT_BILL_TYPE));
        return daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(q);
    }

    private long countActiveCommitments(String poCode) {
        long n = 0;
        for (ErpFinVoucherBillR link : commitmentLinks(poCode)) {
            if (!Boolean.TRUE.equals(voucherOf(link).getIsReversed())) {
                n++;
            }
        }
        return n;
    }

    private ErpFinVoucher activeVoucherOf(List<ErpFinVoucherBillR> links) {
        for (ErpFinVoucherBillR link : links) {
            ErpFinVoucher v = voucherOf(link);
            if (!Boolean.TRUE.equals(v.getIsReversed())) {
                return v;
            }
        }
        return null;
    }

    private ErpFinVoucher voucherOf(ErpFinVoucherBillR link) {
        return daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
    }

    // ---------- rpc ----------

    private ApiResponse<?> submitOrder(Long id) {
        return executeRpc(mutation, "ErpPurOrder__submitForApproval", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> approveOrder(Long id) {
        return executeRpc(mutation, "ErpPurOrder__approve", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> approveReceive(Long id) {
        return executeRpc(mutation, "ErpPurReceive__approve", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> approveReturn(Long id) {
        return executeRpc(mutation, "ErpPurReturn__approve", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> reverseApproveReturn(Long id) {
        return executeRpc(mutation, "ErpPurReturn__reverseApprove", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> cancelReturn(Long id) {
        return executeRpc(mutation, "ErpPurReturn__cancel", ApiRequest.build(Map.of("returnId", id)));
    }

    private ApiResponse<?> submitInvoice(Long id) {
        return executeRpc(mutation, "ErpPurInvoice__submitForApproval", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> approveInvoice(Long id) {
        return executeRpc(mutation, "ErpPurInvoice__approve", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> reverseApproveInvoice(Long id) {
        return executeRpc(mutation, "ErpPurInvoice__reverseApprove", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private Long nextId() {
        return idSeq.incrementAndGet();
    }
}
