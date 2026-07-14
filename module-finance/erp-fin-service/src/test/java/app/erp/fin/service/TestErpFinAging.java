package app.erp.fin.service;

import app.erp.fin.biz.IErpFinArApItemBiz;
import app.erp.fin.dao.dto.ArApAgingRow;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.md.dao.entity.ErpMdPartner;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.config.AppConfig;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 账龄查询（{@link IErpFinArApItemBiz#aging}）集成测试（Phase 3）。验证按基准日（invoice_date/due_date）
 * 配置切换与账龄区间分桶（0-30/31-60/61-90/91-180/180+）正确。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinAging extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();
    private static final long PARTNER = 90L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinArApItemBiz arApItemBiz;

    @Test
    public void testAgingBucketsByInvoiceDate() {
        LocalDate asOf = LocalDate.of(2026, 7, 1);
        // 强制 invoice_date 基准（覆盖配置默认值）
        AppConfig.getConfigProvider().assignConfigValue(ErpFinConstants.CONFIG_AP_AGING_BASE,
                ErpFinConstants.AGING_BASE_INVOICE_DATE);

        ormTemplate.runInSession(() -> {
            seedPartner(PARTNER);
            // 相对 asOf(7-1) 的账龄：10天/40天/75天/120天/200天
            newItem(PARTNER, "AP-AG-1", asOf.minusDays(10), null, "100");
            newItem(PARTNER, "AP-AG-2", asOf.minusDays(40), null, "200");
            newItem(PARTNER, "AP-AG-3", asOf.minusDays(75), null, "300");
            newItem(PARTNER, "AP-AG-4", asOf.minusDays(120), null, "400");
            newItem(PARTNER, "AP-AG-5", asOf.minusDays(200), null, "500");
        });

        List<ArApAgingRow> rows = ormTemplate.runInSession(session -> arApItemBiz.aging(ErpFinConstants.DIRECTION_PAYABLE, asOf, CTX));
        assertEquals(1, rows.size(), "单 partner 一行");
        ArApAgingRow row = rows.get(0);
        assertEquals(0, row.getBucket030().compareTo(new BigDecimal("100")), "0-30 桶");
        assertEquals(0, row.getBucket3160().compareTo(new BigDecimal("200")), "31-60 桶");
        assertEquals(0, row.getBucket6190().compareTo(new BigDecimal("300")), "61-90 桶");
        assertEquals(0, row.getBucket91180().compareTo(new BigDecimal("400")), "91-180 桶");
        assertEquals(0, row.getBucket180Plus().compareTo(new BigDecimal("500")), "180+ 桶");
        assertEquals(0, row.getTotalOpen().compareTo(new BigDecimal("1500")), "未核销合计");
    }

    @Test
    public void testAgingBaseSwitchToDueDate() {
        LocalDate asOf = LocalDate.of(2026, 7, 1);
        // businessDate 100 天前，dueDate 10 天前 → due_date 基准应落入 0-30 桶
        AppConfig.getConfigProvider().assignConfigValue(ErpFinConstants.CONFIG_AP_AGING_BASE,
                ErpFinConstants.AGING_BASE_DUE_DATE);

        ormTemplate.runInSession(() -> {
            seedPartner(PARTNER);
            newItem(PARTNER, "AP-AG-DUE", asOf.minusDays(100), asOf.minusDays(10), "150");
        });

        List<ArApAgingRow> rows = ormTemplate.runInSession(session -> arApItemBiz.aging(ErpFinConstants.DIRECTION_PAYABLE, asOf, CTX));
        ArApAgingRow row = rows.get(0);
        assertEquals(0, row.getBucket030().compareTo(new BigDecimal("150")),
                "due_date 基准：dueDate 早 10 天 → 0-30 桶");
        assertEquals(0, row.getBucket91180().compareTo(BigDecimal.ZERO), "不应落入 91-180 桶");
    }

    @Test
    public void testDueDateNullFallsBackToInvoiceDate() {
        LocalDate asOf = LocalDate.of(2026, 7, 1);
        AppConfig.getConfigProvider().assignConfigValue(ErpFinConstants.CONFIG_AP_AGING_BASE,
                ErpFinConstants.AGING_BASE_DUE_DATE);

        ormTemplate.runInSession(() -> {
            seedPartner(PARTNER);
            // due_date 基准但 dueDate 为 null → 回退 businessDate（早 100 天 → 91-180 桶）
            newItem(PARTNER, "AP-AG-FB", asOf.minusDays(100), null, "250");
        });

        List<ArApAgingRow> rows = ormTemplate.runInSession(session -> arApItemBiz.aging(ErpFinConstants.DIRECTION_PAYABLE, asOf, CTX));
        ArApAgingRow row = rows.get(0);
        assertTrue(row.getBucket91180().compareTo(new BigDecimal("250")) == 0,
                "dueDate 为 null 时回退 invoice_date 分桶");
    }

    // ---------- helpers ----------

    private void seedPartner(long partnerId) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        if (dao.getEntityById(partnerId) != null) {
            return;
        }
        ErpMdPartner p = new ErpMdPartner();
        p.orm_propValue(1, partnerId);
        p.setCode("P-" + partnerId);
        p.setName("Partner " + partnerId);
        p.setPartnerType("CUSTOMER");
        p.setStatus("ACTIVE");
        p.setReceivableBalance(BigDecimal.ZERO);
        p.setPayableBalance(BigDecimal.ZERO);
        dao.saveEntity(p);
    }

    private ErpFinArApItem newItem(long partnerId, String sourceBillCode, LocalDate businessDate,
                                   LocalDate dueDate, String amount) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        ErpFinArApItem item = dao.newEntity();
        item.setCode("ARI-" + sourceBillCode);
        item.setOrgId(1L);
        item.setAcctSchemaId(1L);
        item.setDirection(ErpFinConstants.DIRECTION_PAYABLE);
        item.setPartnerId(partnerId);
        item.setSourceBillType("AP_INVOICE");
        item.setSourceBillCode(sourceBillCode);
        item.setBusinessDate(businessDate);
        item.setDueDate(dueDate);
        item.setCurrencyId(1L);
        item.setExchangeRate(BigDecimal.ONE);
        BigDecimal amt = new BigDecimal(amount);
        item.setAmountSource(amt);
        item.setAmountFunctional(amt);
        item.setSettledAmountSource(BigDecimal.ZERO);
        item.setSettledAmountFunctional(BigDecimal.ZERO);
        item.setOpenAmountSource(amt);
        item.setOpenAmountFunctional(amt);
        item.setStatus(ErpFinConstants.AR_AP_STATUS_OPEN);
        dao.saveEntity(item);
        return item;
    }
}
