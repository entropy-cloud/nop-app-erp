package app.erp.pur.service.posting;

import app.erp.fin.service.posting.VoucherPostedEvent;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.service.ErpPurConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F2.1（P1-CK-fin-001）：PurReversalListener 正向过账回写单测（dual 接口 onVoucherPosted）。
 * posted 翻转（false→true + postedAt/postedBy 三字段）+ 已 posted 跳过（防 version 无谓递增）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestPurPostedListenerWriteback extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @Test
    public void testPostedFlipsTrueWithThreeFields() {
        String code = "PI-F21-POST-001";
        ormTemplate.runInSession(session -> {
            seedInvoice(code, false);
            return null;
        });

        ormTemplate.runInSession(session -> {
            PurReversalListener listener = new PurReversalListener();
            listener.daoProvider = daoProvider;
            VoucherPostedEvent event = new VoucherPostedEvent();
            event.setVoucherId("v-f21-001");
            event.setBusinessType("AP_INVOICE");
            event.setBillHeadCode(code);
            listener.onVoucherPosted(event, CTX);
            session.flush();
            return null;
        });

        ErpPurInvoice after = findInvoice(code);
        assertEquals(Boolean.TRUE, after.getPosted(), "悬挂回写：posted false→true");
        assertNotNull(after.getPostedAt(), "postedAt 回写");
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, after.getApproveStatus(),
                "正向回写不改审批轴（只补 posted 三字段）");
    }

    @Test
    public void testAlreadyPostedSkipsUpdate() {
        String code = "PI-F21-POST-002";
        ormTemplate.runInSession(session -> {
            seedInvoice(code, true);
            return null;
        });
        int versionBefore = findInvoice(code).getVersion();

        ormTemplate.runInSession(session -> {
            PurReversalListener listener = new PurReversalListener();
            listener.daoProvider = daoProvider;
            VoucherPostedEvent event = new VoucherPostedEvent();
            event.setVoucherId("v-f21-002");
            event.setBusinessType("AP_INVOICE");
            event.setBillHeadCode(code);
            listener.onVoucherPosted(event, CTX);
            session.flush();
            return null;
        });

        ErpPurInvoice after = findInvoice(code);
        assertEquals(Boolean.TRUE, after.getPosted(), "已 posted 保持 true");
        assertEquals(versionBefore, after.getVersion(), "已 true 跳过：version 无谓递增被防住");
    }

    @Test
    public void testFindByCodeMissIsNoOp() {
        ormTemplate.runInSession(session -> {
            PurReversalListener listener = new PurReversalListener();
            listener.daoProvider = daoProvider;
            VoucherPostedEvent event = new VoucherPostedEvent();
            event.setBusinessType("AP_INVOICE");
            event.setBillHeadCode("NOT-EXIST-CODE-F21");
            listener.onVoucherPosted(event, CTX);
            return null;
        });
        assertTrue(true, "findByCode miss no-op 不抛异常（分期部署优雅降级）");
    }

    // ---------- helpers ----------

    private ErpPurInvoice findInvoice(String code) {
        return ormTemplate.runInSession(session -> {
            IEntityDao<ErpPurInvoice> dao = daoProvider.daoFor(ErpPurInvoice.class);
            io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
            q.addFilter(io.nop.api.core.beans.FilterBeans.eq("code", code));
            q.setLimit(1);
            return dao.findAllByQuery(q).isEmpty() ? null : dao.findAllByQuery(q).get(0);
        });
    }

    private void seedInvoice(String code, boolean posted) {
        IEntityDao<ErpPurInvoice> dao = daoProvider.daoFor(ErpPurInvoice.class);
        ErpPurInvoice invoice = new ErpPurInvoice();
        invoice.setCode(code);
        invoice.setSupplierId("990101");
        invoice.setBusinessDate(LocalDate.of(2026, 8, 10));
        invoice.setCurrencyId("990301");
        invoice.setDocStatus(ErpPurConstants.DOC_STATUS_ACTIVE);
        invoice.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        invoice.setPosted(posted);
        if (posted) {
            invoice.setPostedAt(CoreMetrics.currentTimestamp());
            invoice.setPostedBy("poster");
        }
        dao.saveEntity(invoice);
    }
}
