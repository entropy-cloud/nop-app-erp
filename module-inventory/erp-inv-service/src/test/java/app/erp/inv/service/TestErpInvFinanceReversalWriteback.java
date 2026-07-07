package app.erp.inv.service;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.service.ErpFinConstants;
import app.erp.inv.dao.entity.ErpInvOwnershipTransfer;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 业财闭环方向二集成测试（计划 {@code 2026-07-04-1452-2} Phase 3）——库存域。
 *
 * <p>验证财务侧直接红冲已过账凭证时，{@code InvReversalListener} 监听 {@link IErpFinVoucherBiz#reverse}
 * 派发的 {@code VoucherReversedEvent}，回退所有权转移单状态（posted=false；库存单据无 approveStatus 轴，
 * 仅 posted 翻转，对齐设计 {@code posting.md §冲销机制方向二 §实现策略 裁决4}）。
 *
 * <p>对标采购/销售域测试，但断言仅 posted 翻转（库存单据无 approveStatus）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvFinanceReversalWriteback extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();

    static final Long ORG_ID = 1303L;
    static final Long PARTNER_ID = 2301L;
    static final Long WAREHOUSE_ID = 3301L;
    static final Long LOCATION_ID = 4301L;
    static final Long MATERIAL_ID = 5301L;
    static final Long ACCT_SCHEMA_ID = 7303L;
    static final Long CURRENCY_ID = 6301L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinVoucherBiz voucherBiz;

    @Test
    public void testFinanceReverseRollsBackOwnershipTransferPosted() {
        seedPeriodAndAcctSchema();

        ErpInvOwnershipTransfer transfer = new ErpInvOwnershipTransfer();
        transfer.setCode("OT-FIN-REV-001");
        transfer.setOrgId(ORG_ID);
        transfer.setPartnerId(PARTNER_ID);
        transfer.setWarehouseId(WAREHOUSE_ID);
        transfer.setSourceLocId(LOCATION_ID);
        transfer.setDestLocId(LOCATION_ID);
        transfer.setFromOwnershipType(ErpInvConstants.OWNERSHIP_TYPE_VMI_SUPPLIER);
        transfer.setToOwnershipType(ErpInvConstants.OWNERSHIP_TYPE_OWNED);
        transfer.setTransferType(ErpInvConstants.TRANSFER_TYPE_VMI_CONSUME);
        transfer.setBusinessDate(LocalDate.of(2026, 7, 1));
        transfer.setCurrencyId(CURRENCY_ID);
        transfer.setDocStatus(ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CONFIRMED);
        transfer.setPosted(true);
        transfer.setPostedAt(CoreMetrics.currentDateTime());
        transfer.setPostedBy("test-user");
        ormTemplate.runInSession(() -> daoProvider.daoFor(ErpInvOwnershipTransfer.class).saveEntity(transfer));

        Long originalVoucherId = seedPostedVoucherFor(transfer.getCode(),
                ErpFinBusinessType.OWNERSHIP_TRANSFER, new BigDecimal("1000"));

        assertTrue(Boolean.TRUE.equals(reload(transfer).getPosted()),
                "前置：所有权转移单已过账 posted=true");

        Long redVoucherId = voucherBiz.reverse(transfer.getCode(), ErpFinBusinessType.OWNERSHIP_TRANSFER, CTX);

        assertNotNull(redVoucherId);
        assertNotEquals(originalVoucherId, redVoucherId);

        ErpInvOwnershipTransfer reloaded = reload(transfer);
        assertFalse(Boolean.TRUE.equals(reloaded.getPosted()),
                "方向二：财务红冲后所有权转移单 posted 应被监听者回退为 false");
        assertEquals(null, reloaded.getPostedAt(), "postedAt 应清空");
        assertEquals(null, reloaded.getPostedBy(), "postedBy 应清空");
        // docStatus 不回退（库存物理冲销独立于凭证红冲，单据终态保留审计轨迹）
        assertEquals(ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CONFIRMED, reloaded.getDocStatus(),
                "库存单据无 approveStatus 轴，财务红冲仅回退 posted 标志，docStatus 保留");
    }

    // ---------- helpers ----------

    private ErpInvOwnershipTransfer reload(ErpInvOwnershipTransfer transfer) {
        return daoProvider.daoFor(ErpInvOwnershipTransfer.class).getEntityById(transfer.getId());
    }

    private Long seedPostedVoucherFor(String billCode, ErpFinBusinessType businessType, BigDecimal total) {
        IEntityDao<ErpFinVoucher> vDao = daoProvider.daoFor(ErpFinVoucher.class);
        IEntityDao<ErpFinVoucherBillR> billRDao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        IEntityDao<ErpFinAccountingPeriod> periodDao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        QueryBean pq = new QueryBean();
        pq.addFilter(eq("code", "2026-07"));
        pq.setLimit(1);
        ErpFinAccountingPeriod period = periodDao.findAllByQuery(pq).get(0);
        return ormTemplate.runInSession(session -> {
            ErpFinVoucher voucher = new ErpFinVoucher();
            voucher.setCode("PST-SEED-" + billCode);
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
            billR.setBillCode(billCode);
            billR.setBusinessType(businessType.name());
            billRDao.saveEntity(billR);
            return voucher.getId();
        });
    }

    private void seedPeriodAndAcctSchema() {
        ormTemplate.runInSession(() -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "OPEN");
            seedAcctSchema(ACCT_SCHEMA_ID, ORG_ID);
        });
    }

    private void seedAcctSchema(Long id, Long orgId) {
        IEntityDao<app.erp.md.dao.entity.ErpMdAcctSchema> dao = daoProvider.daoFor(
                app.erp.md.dao.entity.ErpMdAcctSchema.class);
        app.erp.md.dao.entity.ErpMdAcctSchema schema = new app.erp.md.dao.entity.ErpMdAcctSchema();
        schema.setId(id);
        schema.setCode("AS-" + id);
        schema.setName("账套" + id);
        schema.setOrgId(orgId);
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
}
