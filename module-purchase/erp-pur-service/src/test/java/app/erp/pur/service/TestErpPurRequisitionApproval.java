package app.erp.pur.service;

import app.erp.pur.biz.IErpPurRequisitionBiz;
import app.erp.pur.dao.entity.ErpPurRequisition;
import app.erp.pur.dao.entity.ErpPurRequisitionLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.exceptions.NopException;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Phase 1 服务层集成测试：采购请购单三轴审批状态机。
 *
 * <p>直接调用 {@link IErpPurRequisitionBiz} 的 Java API（不走 GraphQL 快照），自建行明细后断言状态迁移。
 * 请购头无供应商，状态机不做供应商校验；请购 approve 仅状态推进（请购无自动下游触发，转化是显式独立动作）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurRequisitionApproval extends JunitAutoTestCase {
    private static final IServiceContext CTX = new ServiceContextImpl();


    static final Long ORG_ID = 1201L;
    static final Long REQUESTER_ID = 2201L;
    static final Long MATERIAL_ID = 4201L;
    static final Long UOM_ID = 5201L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpPurRequisitionBiz reqBiz;

    @Test
    public void testReqSubmitApproveRejectResubmit() {
        ErpPurRequisition req = newRequisition("PR-SUBMIT-001");
        ormTemplate.runInSession(() -> saveRequisitionWithLine(req));

        ErpPurRequisition submitted = reqBiz.submit(req.getId(), CTX);
        assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, submitted.getApproveStatus(),
                "提交 → SUBMITTED");

        ErpPurRequisition approved = reqBiz.approve(req.getId(), CTX);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus(),
                "审核通过 → APPROVED");
    }

    @Test
    public void testReqRejectAndResubmit() {
        ErpPurRequisition req = newRequisition("PR-REJ-001");
        ormTemplate.runInSession(() -> saveRequisitionWithLine(req));

        reqBiz.submit(req.getId(), CTX);
        ErpPurRequisition rejected = reqBiz.reject(req.getId(), CTX);
        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, rejected.getApproveStatus(),
                "驳回 → REJECTED");

        ErpPurRequisition resubmitted = reqBiz.submit(req.getId(), CTX);
        assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, resubmitted.getApproveStatus(),
                "REJECTED 重新提交 → SUBMITTED");
    }

    @Test
    public void testReqIllegalTransitionRejected() {
        ErpPurRequisition req = newRequisition("PR-ILL-001");
        ormTemplate.runInSession(() -> saveRequisitionWithLine(req));

        reqBiz.submit(req.getId(), CTX);
        ErpPurRequisition approved = reqBiz.approve(req.getId(), CTX);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus());

        // APPROVED → 再次 submit 非法
        assertThrows(NopException.class, () -> reqBiz.submit(req.getId(), CTX),
                "APPROVED 不可再提交，应抛 NopException");
        // APPROVED → withdrawSubmit 非法
        assertThrows(NopException.class, () -> reqBiz.withdrawSubmit(req.getId(), CTX),
                "APPROVED 不可撤回提交，应抛 NopException");

        // 反审核 APPROVED → REJECTED，目标态非 UNSUBMITTED（state-machine §3/§11.4）
        ErpPurRequisition reversed = reqBiz.reverseApprove(req.getId(), CTX);
        assertEquals(ErpPurConstants.APPROVE_STATUS_REJECTED, reversed.getApproveStatus(),
                "反审核目标态 = REJECTED 非 UNSUBMITTED");
    }

    @Test
    public void testReqCancelFromDraft() {
        ErpPurRequisition req = newRequisition("PR-CANCEL-001");
        ormTemplate.runInSession(() -> saveRequisitionWithLine(req));

        ErpPurRequisition cancelled = reqBiz.cancel(req.getId(), CTX);
        assertEquals(ErpPurConstants.DOC_STATUS_CANCELLED, cancelled.getDocStatus(),
                "草稿 → 作废 docStatus=CANCELLED");

        // 已作废不可再提交
        assertThrows(NopException.class, () -> reqBiz.submit(req.getId(), CTX),
                "已作废请购单不可提交，应抛 NopException");
    }

    // ---------- helpers ----------

    private ErpPurRequisition newRequisition(String code) {
        ErpPurRequisition req = new ErpPurRequisition();
        req.setCode(code);
        req.setOrgId(ORG_ID);
        req.setRequesterId(REQUESTER_ID);
        req.setBusinessDate(LocalDate.of(2026, 7, 1));
        req.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        req.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        return req;
    }

    private void saveRequisitionWithLine(ErpPurRequisition req) {
        daoProvider.daoFor(ErpPurRequisition.class).saveEntity(req);
        IEntityDao<ErpPurRequisitionLine> lineDao = daoProvider.daoFor(ErpPurRequisitionLine.class);
        ErpPurRequisitionLine line = new ErpPurRequisitionLine();
        line.setRequisitionId(req.getId());
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(new BigDecimal("10"));
        lineDao.saveEntity(line);
    }
}
