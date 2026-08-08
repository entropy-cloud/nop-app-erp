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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RC-R1.12 发票侧承付冲销恢复集成测试（plan 2026-08-08-1603-2，budget.md §承付会计 §3 冲销恢复语义）。
 *
 * <p>验证 {@code ErpPurInvoiceReverseApproveProcessor.reverseApprove} + {@code ErpPurInvoiceCancelProcessor.cancel}
 * 在 AP 红冲 + 状态回退之后对关联 PO 重新生成 COMMITMENT 凭证（对称恢复正向 release-on-invoice-approve）：
 * <ul>
 *   <li>正向链闭合：PO approve → C1（COMMITMENT）；invoice approve → 释放（C1 isReversed=true）；
 *       invoice reverseApprove/cancel → 新 C2（isReversed=false，billCode=PO code）+ <b>每 PO 恰 1 张活跃凭证</b>。</li>
 *   <li>恢复前置守卫（防幽灵承付/双占用）：config 关闭 / 从未 APPROVED 取消 / PO CANCELLED / PO REJECTED /
 *       无关联 PO 时零新凭证。</li>
 *   <li>交互矩阵：双重 reverseApprove 幂等（第二次被迁移守卫拒绝）；Seq B 多发票逐张冲销 → 2-active
 *       显式裁决保守边界。</li>
 * </ul>
 *
 * <p>config 驱动：{@code budget-commitment-test.yaml}（{@code erp-fin.budget-commitment-enabled=true} +
 * 科目 1408）；config 关闭场景经 {@code AppConfig} 运行时翻转。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:budget-commitment-test.yaml")
public class TestErpPurInvoiceCommitmentRestore extends JunitAutoTestCase {

    static final Long ORG_ID = 9101L;
    static final Long SUPPLIER_ID = 9201L;
    static final Long WAREHOUSE_ID = 9301L;
    static final Long MATERIAL_ID = 9401L;
    static final Long UOM_ID = 9501L;
    static final Long CURRENCY_ID = 9601L;
    static final Long ACCT_SCHEMA_ID = 9701L;
    static final String COMMITMENT_BILL_TYPE = "PURCHASE_ORDER_COMMITMENT";
    static final String POSTING_TYPE_COMMITMENT = "COMMITMENT";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    private final AtomicLong idSeq = new AtomicLong(900000L);

    @Test
    public void testInvoiceReverseApproveRestoresCommitment() {
        seedPrereqs();
        long orderLineId = nextId();
        long receiveId = nextId();
        long receiveLineId = nextId();
        Long orderId = newOrder("PO-IRS-001", orderLineId);
        Long invoiceId = buildPoReceiveInvoice("PR-IRS-001", receiveId, receiveLineId, orderId, orderLineId,
                "PI-IRS-001", receiveLineId);

        // ① PO approve → COMMITMENT C1
        assertEquals(0, submitOrder(orderId).getStatus());
        assertEquals(0, approveOrder(orderId).getStatus());
        assertEquals(1, countActiveCommitments("PO-IRS-001"), "PO 审核后恰 1 张活跃承付凭证");

        // ② invoice approve → release（C1 红冲，生成红冲凭证）
        assertEquals(0, submitInvoice(invoiceId).getStatus());
        assertEquals(0, approveInvoice(invoiceId).getStatus());
        List<ErpFinVoucherBillR> links = commitmentLinks("PO-IRS-001");
        assertEquals(2, links.size(), "发票审核释放后 = 原凭证 + 红冲凭证");
        assertEquals(0, countActiveCommitments("PO-IRS-001"), "发票审核释放后无活跃承付凭证");

        // ③ invoice reverseApprove → 恢复 C2（isReversed=false，billCode=PO code）
        assertEquals(0, reverseApproveInvoice(invoiceId).getStatus());
        links = commitmentLinks("PO-IRS-001");
        assertEquals(3, links.size(), "冲销恢复后共 3 张凭证（C1 + 红冲 + C2 恢复）");
        ErpFinVoucher restored = activeVoucherOf(links);
        assertNotNull(restored, "冲销恢复后应存在 1 张活跃承付凭证（单活跃不变量）");
        assertEquals(POSTING_TYPE_COMMITMENT, restored.getPostingType(), "恢复凭证 postingType=COMMITMENT");
        assertFalse(Boolean.TRUE.equals(restored.getIsReversed()), "恢复凭证 isReversed=false");
        assertEquals(1, countActiveCommitments("PO-IRS-001"), "冲销恢复后恰 1 张活跃凭证");
    }

    @Test
    public void testInvoiceCancelRestoresCommitment() {
        seedPrereqs();
        long orderLineId = nextId();
        long receiveId = nextId();
        long receiveLineId = nextId();
        Long orderId = newOrder("PO-ICS-001", orderLineId);
        Long invoiceId = buildPoReceiveInvoice("PR-ICS-001", receiveId, receiveLineId, orderId, orderLineId,
                "PI-ICS-001", receiveLineId);

        assertEquals(0, submitOrder(orderId).getStatus());
        assertEquals(0, approveOrder(orderId).getStatus());
        assertEquals(1, countActiveCommitments("PO-ICS-001"));

        assertEquals(0, submitInvoice(invoiceId).getStatus());
        assertEquals(0, approveInvoice(invoiceId).getStatus());
        assertEquals(0, countActiveCommitments("PO-ICS-001"), "发票审核释放后无活跃凭证");

        // cancel 路径同型恢复
        assertEquals(0, cancelInvoice(invoiceId).getStatus());
        List<ErpFinVoucherBillR> links = commitmentLinks("PO-ICS-001");
        assertEquals(3, links.size(), "作废恢复后共 3 张凭证（C1 + 红冲 + C2 恢复）");
        ErpFinVoucher restored = activeVoucherOf(links);
        assertNotNull(restored, "作废恢复后应存在 1 张活跃承付凭证（单活跃不变量）");
        assertEquals(POSTING_TYPE_COMMITMENT, restored.getPostingType());
        assertFalse(Boolean.TRUE.equals(restored.getIsReversed()));
        assertEquals(1, countActiveCommitments("PO-ICS-001"), "作废恢复后恰 1 张活跃凭证");
    }

    @Test
    public void testConfigDisabledNoRestore() {
        seedPrereqs();
        long orderLineId = nextId();
        long receiveId = nextId();
        long receiveLineId = nextId();
        Long orderId = newOrder("PO-ICF-001", orderLineId);
        Long invoiceId = buildPoReceiveInvoice("PR-ICF-001", receiveId, receiveLineId, orderId, orderLineId,
                "PI-ICF-001", receiveLineId);

        assertEquals(0, submitOrder(orderId).getStatus());
        assertEquals(0, approveOrder(orderId).getStatus());
        assertEquals(1, countActiveCommitments("PO-ICF-001"), "config ON 时 PO 审核生成 C1");

        // 关总开关：invoice approve 不释放、reverseApprove 不恢复
        AppConfig.getConfigProvider().assignConfigValue(
                ErpFinConstants.CONFIG_BUDGET_COMMITMENT_ENABLED, Boolean.FALSE);
        try {
            assertEquals(0, submitInvoice(invoiceId).getStatus());
            assertEquals(0, approveInvoice(invoiceId).getStatus());
            assertEquals(0, reverseApproveInvoice(invoiceId).getStatus());
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpFinConstants.CONFIG_BUDGET_COMMITMENT_ENABLED, Boolean.TRUE);
        }
        List<ErpFinVoucherBillR> links = commitmentLinks("PO-ICF-001");
        assertEquals(1, links.size(), "config 关闭时 reverseApprove/cancel 零恢复凭证");
        assertEquals(1, countActiveCommitments("PO-ICF-001"), "C1 保持活跃（无新凭证生成）");
    }

    @Test
    public void testCancelNeverApprovedNoRestore() {
        seedPrereqs();
        long orderLineId = nextId();
        long receiveId = nextId();
        long receiveLineId = nextId();
        Long orderId = newOrder("PO-INV-001", orderLineId);
        buildPoReceiveInvoice("PR-INV-001", receiveId, receiveLineId, orderId, orderLineId,
                "PI-INV-001", receiveLineId);

        assertEquals(0, submitOrder(orderId).getStatus());
        assertEquals(0, approveOrder(orderId).getStatus());
        assertEquals(1, countActiveCommitments("PO-INV-001"), "C1 已生成");

        // 从未 APPROVED 的 invoice 直接作废 → wasApproved=false → 零恢复（防幽灵承付双占用）
        ErpPurInvoice invoice = daoProvider.daoFor(ErpPurInvoice.class)
                .getEntityById(findInvoiceByCode("PI-INV-001"));
        assertEquals(0, cancelInvoice(invoice.getId()).getStatus());
        assertEquals(1, commitmentLinks("PO-INV-001").size(), "从未 APPROVED 取消不恢复（零新凭证）");
        assertEquals(1, countActiveCommitments("PO-INV-001"), "C1 保持唯一活跃凭证");
    }

    @Test
    public void testPoCancelledNoRestore() {
        seedPrereqs();
        long orderLineId = nextId();
        long receiveId = nextId();
        long receiveLineId = nextId();
        Long orderId = newOrder("PO-IPC-001", orderLineId);
        Long invoiceId = buildPoReceiveInvoice("PR-IPC-001", receiveId, receiveLineId, orderId, orderLineId,
                "PI-IPC-001", receiveLineId);

        assertEquals(0, submitOrder(orderId).getStatus());
        assertEquals(0, approveOrder(orderId).getStatus());
        assertEquals(0, submitInvoice(invoiceId).getStatus());
        assertEquals(0, approveInvoice(invoiceId).getStatus());
        assertEquals(0, countActiveCommitments("PO-IPC-001"), "发票审核已释放承付");

        // PO 作废（终态守卫：docStatus=CANCELLED）→ invoice 冲销恢复跳过
        assertEquals(0, cancelOrder(orderId).getStatus());
        assertEquals(0, reverseApproveInvoice(invoiceId).getStatus());
        List<ErpFinVoucherBillR> links = commitmentLinks("PO-IPC-001");
        assertEquals(2, links.size(), "PO CANCELLED 后冲销恢复零新凭证（仅 C1 + 红冲，防永久泄漏）");
        assertEquals(0, countActiveCommitments("PO-IPC-001"), "无活跃凭证（无恢复）");
    }

    @Test
    public void testPoRejectedNoRestore() {
        // Seq A：PO approve→invoice approve→PO reverseApprove（PO=REJECTED）→invoice reverseApprove → 零恢复
        seedPrereqs();
        long orderLineId = nextId();
        long receiveId = nextId();
        long receiveLineId = nextId();
        Long orderId = newOrder("PO-IPR-001", orderLineId);
        Long invoiceId = buildPoReceiveInvoice("PR-IPR-001", receiveId, receiveLineId, orderId, orderLineId,
                "PI-IPR-001", receiveLineId);

        assertEquals(0, submitOrder(orderId).getStatus());
        assertEquals(0, approveOrder(orderId).getStatus());
        assertEquals(0, submitInvoice(invoiceId).getStatus());
        assertEquals(0, approveInvoice(invoiceId).getStatus());

        // PO 反审核 → approveStatus=REJECTED（非 APPROVED PO 无活跃承付义务）
        assertEquals(0, reverseApproveOrder(orderId).getStatus());
        assertEquals(0, reverseApproveInvoice(invoiceId).getStatus());
        List<ErpFinVoucherBillR> links = commitmentLinks("PO-IPR-001");
        assertEquals(2, links.size(), "PO 非 APPROVED 时冲销恢复零新凭证（仅 C1 + 红冲，防双占用）");
        assertEquals(0, countActiveCommitments("PO-IPR-001"), "无活跃凭证（无恢复）");
    }

    @Test
    public void testNoLinkedOrderNoRestore() {
        seedPrereqs();
        long orderLineId = nextId();
        long receiveId = nextId();
        long receiveLineId = nextId();
        Long orderId = newOrder("PO-INL-001", orderLineId);
        buildPoReceiveInvoice("PR-INL-001", receiveId, receiveLineId, orderId, orderLineId,
                null, null);

        assertEquals(0, submitOrder(orderId).getStatus());
        assertEquals(0, approveOrder(orderId).getStatus());
        assertEquals(1, countActiveCommitments("PO-INL-001"), "C1 已生成");

        // 发票行零 receiveLineId 回链 → approve 不释放、reverseApprove 不恢复
        Long invoiceId = newInvoice("PI-INL-001", null);
        ErpPurInvoice invoice = daoProvider.daoFor(ErpPurInvoice.class).getEntityById(invoiceId);
        assertEquals(0, submitInvoice(invoice.getId()).getStatus());
        assertEquals(0, approveInvoice(invoice.getId()).getStatus());
        assertEquals(0, reverseApproveInvoice(invoice.getId()).getStatus());
        assertEquals(1, commitmentLinks("PO-INL-001").size(), "无关联 PO 时零恢复凭证");
        assertEquals(1, countActiveCommitments("PO-INL-001"), "C1 保持唯一活跃凭证");
    }

    @Test
    public void testSeqBMultiInvoiceTwoActiveBoundary() {
        // Seq B：PO approve→inv#1 approve→inv#2 approve（释放均吞掉）→inv#1 reverseApprove→inv#2 reverseApprove
        // → 恰 2 张活跃凭证（全量恢复语义的已知保守边界，按比例语义归 successor）
        seedPrereqs();
        long orderLineId = nextId();
        long receiveId = nextId();
        long receiveLineId = nextId();
        Long orderId = newOrder("PO-ISB-001", orderLineId);
        buildPoReceiveInvoice("PR-ISB-001", receiveId, receiveLineId, orderId, orderLineId,
                "PI-ISB-001", receiveLineId);
        Long invoice2Id = newInvoice("PI-ISB-002", receiveLineId);

        assertEquals(0, submitOrder(orderId).getStatus());
        assertEquals(0, approveOrder(orderId).getStatus());
        assertEquals(1, countActiveCommitments("PO-ISB-001"));

        // 两张发票均审核：首张释放（C1 红冲），次张容错跳过（无活跃凭证可红冲）
        assertEquals(0, submitInvoice(findInvoiceByCode("PI-ISB-001")).getStatus());
        assertEquals(0, approveInvoice(findInvoiceByCode("PI-ISB-001")).getStatus());
        assertEquals(0, submitInvoice(invoice2Id).getStatus());
        assertEquals(0, approveInvoice(invoice2Id).getStatus());
        assertEquals(0, countActiveCommitments("PO-ISB-001"), "两发票审核后无活跃凭证");

        // 逐张冲销恢复 → 2 张活跃（显式裁决 2-active 边界）
        assertEquals(0, reverseApproveInvoice(findInvoiceByCode("PI-ISB-001")).getStatus());
        assertEquals(1, countActiveCommitments("PO-ISB-001"), "第一张冲销恢复后 1 张活跃");
        assertEquals(0, reverseApproveInvoice(invoice2Id).getStatus());
        assertEquals(2, countActiveCommitments("PO-ISB-001"), "第二张冲销恢复后 2 张活跃（Seq B 裁决边界）");
        assertEquals(4, commitmentLinks("PO-ISB-001").size(), "凭证总数 = C1 + 红冲 + C2 + C3");
    }

    @Test
    public void testDoubleReverseApproveNoSecondRestore() {
        // 幂等守卫：第二次 reverseApprove 被迁移守卫拒绝（REJECTED 非 APPROVED），不二次恢复
        seedPrereqs();
        long orderLineId = nextId();
        long receiveId = nextId();
        long receiveLineId = nextId();
        Long orderId = newOrder("PO-IDR-001", orderLineId);
        Long invoiceId = buildPoReceiveInvoice("PR-IDR-001", receiveId, receiveLineId, orderId, orderLineId,
                "PI-IDR-001", receiveLineId);

        assertEquals(0, submitOrder(orderId).getStatus());
        assertEquals(0, approveOrder(orderId).getStatus());
        assertEquals(0, submitInvoice(invoiceId).getStatus());
        assertEquals(0, approveInvoice(invoiceId).getStatus());

        assertEquals(0, reverseApproveInvoice(invoiceId).getStatus());
        assertEquals(1, countActiveCommitments("PO-IDR-001"), "首次冲销恢复后 1 张活跃");

        // 幂等守卫：第二次 reverseApprove 命中 isRejected 幂等返回（已 REJECTED 直接返回不二次处理）→ 不二次恢复
        ApiResponse<?> second = reverseApproveInvoice(invoiceId);
        assertEquals(0, second.getStatus(), "二次反审核应为幂等返回（isRejected 直接返回）");
        assertEquals(1, countActiveCommitments("PO-IDR-001"), "幂等二次反审核不产生新恢复凭证");
        assertEquals(3, commitmentLinks("PO-IDR-001").size(), "凭证总数 = C1 + 红冲 + C2（无第二次恢复）");
    }

    // ---------- chain builders ----------

    /**
     * @return invoiceId（invoiceCode 为 null 时不创建发票行 receiveLineId 回链——无关联 PO 场景）
     */
    private Long buildPoReceiveInvoice(String receiveCode, long receiveId, long receiveLineId, Long orderId,
                                       long orderLineId, String invoiceCode, Long invoiceReceiveLineId) {
        long newReceiveId = nextId();
        ormTemplate.runInSession(session -> {
            newReceive(receiveCode, newReceiveId, orderId);
            newReceiveLine(receiveLineId, newReceiveId, orderLineId, new BigDecimal("10"), new BigDecimal("5"));
            return null;
        });
        assertEquals(0, approveReceive(newReceiveId).getStatus(), "源入库单审核应成功");
        if (invoiceCode == null) {
            return null;
        }
        return newInvoice(invoiceCode, invoiceReceiveLineId);
    }

    // ---------- seed ----------

    private void seedPrereqs() {
        ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "OPEN");
            // 承付科目 1408 + PURCHASE_INPUT（1401/2202）+ AP_INVOICE（1403/2221/2202）
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

    /**
     * @param receiveLineId 为 null 时不设置发票行 receiveLineId 回链（无关联 PO 场景）
     */
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

    private Long findInvoiceByCode(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        List<ErpPurInvoice> list = daoProvider.daoFor(ErpPurInvoice.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0).getId();
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

    private ApiResponse<?> reverseApproveOrder(Long id) {
        return executeRpc(mutation, "ErpPurOrder__reverseApprove", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> cancelOrder(Long id) {
        return executeRpc(mutation, "ErpPurOrder__cancel", ApiRequest.build(Map.of("orderId", id)));
    }

    private ApiResponse<?> approveReceive(Long id) {
        return executeRpc(mutation, "ErpPurReceive__approve", ApiRequest.build(Map.of("id", String.valueOf(id))));
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

    private ApiResponse<?> cancelInvoice(Long id) {
        return executeRpc(mutation, "ErpPurInvoice__cancel", ApiRequest.build(Map.of("invoiceId", id)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private Long nextId() {
        return idSeq.incrementAndGet();
    }
}
