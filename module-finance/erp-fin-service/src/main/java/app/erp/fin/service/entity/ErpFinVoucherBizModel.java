
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.AuditType;
import io.nop.api.core.annotations.biz.BizAudit;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.txn.TransactionPropagation;
import io.nop.api.core.annotations.txn.Transactional;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;
import io.nop.core.context.IServiceContext;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.dto.VoucherReversePreview;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.posting.ErpFinPostingProcessor;
import app.erp.fin.service.statemachine.ErpFinVoucherDocumentStateMachine;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

// 族 A/U20 豁免登记：本类为BizModel；daoFor 目标（ErpFinVoucherBillR、ErpFinVoucherLine）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * 凭证聚合根 Biz（过账记录主实体）。CRUD 之外承载业财过账的两个动作入口（{@code post}/{@code reverse}），
 * 为过账引擎 Facade（{@code processor-extension-pattern.md} 两层结构）。Facade 只负责入口/事务/参数透传，
 * 编排委托 {@link ErpFinPostingProcessor}。
 *
 * <p>事务入口钉在 {@link BizMutation}：{@link #post} 叠加 {@link Transactional}(REQUIRES_NEW) 承接跨域失败隔离
 * （过账失败回滚独立事务，不污染源单据主事务；语义承接自原 {@code InvPostingExecutor}）——这是
 * {@code processor-extension-pattern.md} 硬规则 1 的显式独立事务边界声明，故此处特意叠加 @Transactional。
 * ORM Session 由编排层 {@link ErpFinPostingProcessor} 的 {@code @SingleSession} 承接（@SingleSession 原位于
 * 重构前的过账入口方法、现迁移至编排方法），使 Session 作用域精确覆盖 ORM 工作、在编排方法返回时刷新——
 * 这样跨域调用方（{@code InvPostingDispatcher}）的 try/catch 能稳定捕获过账异常（事务/Session 边界不自洽问题见 plan 闭合记录）。
 *
 * <p>O-7：{@link #reverse} 对齐 {@link #post} 叠加 {@link Transactional}(REQUIRES_NEW)，使红冲凭证的写操作
 * 同样以独立事务承接，避免红冲异常污染调用方主事务（与过账一致的事务边界语义）。
 *
 * <p>O-17：{@link #post} 叠加 {@link BizAudit}(AUDIT_SUCCESS)，过账操作经平台审计日志机制记录操作人/时间/事件键，
 * 满足会计凭证过账的可追溯性要求。
 *
 * <p>{@link #postVoucher}/{@link #reverseVoucher}：UI 按钮入口，作用于已存在的凭证实体，分别执行
 * DRAFT→POSTED 状态切换与红冲标记（不与跨域业财过账入口 {@link #post} 混淆）。
 */
@BizModel("ErpFinVoucher")
public class ErpFinVoucherBizModel extends AbstractErpCrudBizModel<ErpFinVoucher> implements IErpFinVoucherBiz {
    public ErpFinVoucherBizModel() {
        setEntityName(ErpFinVoucher.class.getName());
    }

    @Inject
    ErpFinPostingProcessor postingProcessor;

    @Inject
    ErpFinVoucherDocumentStateMachine documentStateMachine;

    @Override
    // nop-check: allow @Transactional(REQUIRES_NEW) — 过账独立事务边界，见 processor-extension-pattern.md 硬规则 1
    @BizMutation
    @BizAudit(auditType = AuditType.AUDIT_SUCCESS)
    @Transactional(propagation = TransactionPropagation.REQUIRES_NEW)
    public String post(@Name("event") PostingEvent event, IServiceContext context) {
        return postingProcessor.process(event, context);
    }

    @Override
    // nop-check: allow @Transactional(REQUIRES_NEW) — 红冲独立事务边界，与 post 一致
    @BizMutation
    @Transactional(propagation = TransactionPropagation.REQUIRES_NEW)
    public String reverse(@Name("billHeadCode") String billHeadCode,
                        @Name("businessType") ErpFinBusinessType businessType,
                        IServiceContext context) {
        return postingProcessor.reverseProcess(billHeadCode, businessType, context);
    }

    @Override
    @BizMutation
    public ErpFinVoucher postVoucher(@Name("voucherId") String voucherId, IServiceContext context) {
        ErpFinVoucher voucher = requireEntity(String.valueOf(voucherId), null, context);
        assertPeriodNotLocked(voucher);
        try {
            documentStateMachine.assertCanPost(voucher.getDocStatus());
        } catch (NopException e) {
            // Bean 直抛领域码 ERR_FIN_VOUCHER_ILLEGAL_TRANSITION（plan 2026-09-07-2200-1），本处同码补参 voucherId。
            throw e.param(ErpFinErrors.ARG_VOUCHER_ID, voucherId);
        }
        // F1.2→F2.1（P1-CK-fin-002）：借贷平衡校验——DRAFT→POSTED 迁移边守卫（state-machine.md L40）
        assertBalancedFromLines(voucher);
        voucher.setDocStatus(documentStateMachine.postVoucherTargetStatus());
        voucher.setPostedBy(context.getUserContext() != null ? context.getUserContext().getUserId() : null);
        voucher.setPostedAt(CoreMetrics.currentTimestamp());
        updateEntity(voucher, null, context);
        return voucher;
    }

    @Override
    @BizMutation
    public ErpFinVoucher reverseVoucher(@Name("voucherId") String voucherId, IServiceContext context) {
        ErpFinVoucher voucher = requireEntity(String.valueOf(voucherId), null, context);
        assertPeriodNotLocked(voucher);
        // isReversed 标志操作的前置分类（POSTED），非 docStatus 迁移边（契约 §3/§对象一；POSTED 保留）。
        if (!documentStateMachine.isPosted(voucher.getDocStatus())) {
            throw new NopException(ErpFinErrors.ERR_FIN_VOUCHER_ILLEGAL_TRANSITION)
                    .param(ErpFinErrors.ARG_VOUCHER_ID, voucherId)
                    .param(ErpFinErrors.ARG_CURRENT_STATUS, voucher.getDocStatus());
        }
        // P2-CK-fin-015：业务凭证（有 BillR 业财回链）与红字凭证（REVERSAL 亦带回链）拒绝单边标记——
        // 标记后 GL 排除式汇总移除该笔，但源单 posted=true 原样 + 辅助账开放，且后续源单反审核走
        // reverse() 找不到可冲销凭证被 ERR_REVERSE_SOURCE_NOT_FOUND 永久阻断（posting.md 冲销机制
        // 「双向闭环缺一不可」）。业务凭证走源单反审核；红字凭证拒绝防 GL 汇总翻符号；
        // 无回链手工凭证保留标记式（state-machine.md L41 单边简化裁决范围）。
        if (!loadBillLinks(voucherId).isEmpty()) {
            throw new NopException(ErpFinErrors.ERR_REVERSE_VOUCHER_BILL_LINKED)
                    .param(ErpFinErrors.ARG_VOUCHER_ID, voucherId);
        }
        voucher.setIsReversed(true);
        updateEntity(voucher, null, context);
        return voucher;
    }

    /**
     * F7 §3 凭证红字冲销预览。只读，不执行实际冲销。镜像 {@link #reverseVoucher} 的前置校验
     * （须 POSTED 且未红冲），返回结构化预览供前端 dialog 展示后再确认执行。
     */
    @Override
    @BizQuery
    public VoucherReversePreview previewReverseVoucher(@Name("voucherId") String voucherId, IServiceContext context) {
        ErpFinVoucher voucher = requireEntity(String.valueOf(voucherId), null, context);
        // 只读预览守卫，委托 Bean isPosted 分类 helper（一致性，非迁移边）。
        if (!documentStateMachine.isPosted(voucher.getDocStatus())) {
            throw new NopException(ErpFinErrors.ERR_FIN_VOUCHER_ILLEGAL_TRANSITION)
                    .param(ErpFinErrors.ARG_VOUCHER_ID, voucherId)
                    .param(ErpFinErrors.ARG_CURRENT_STATUS, voucher.getDocStatus());
        }

        VoucherReversePreview preview = new VoucherReversePreview();
        preview.setVoucherId(voucher.getId());
        preview.setVoucherCode(voucher.getCode());
        preview.setVoucherType(voucher.getVoucherType());
        preview.setVoucherDate(voucher.getVoucherDate());
        preview.setTotalDebit(nz(voucher.getTotalDebit()));
        preview.setTotalCredit(nz(voucher.getTotalCredit()));
        preview.setReversedDebit(nz(voucher.getTotalDebit()).negate());
        preview.setReversedCredit(nz(voucher.getTotalCredit()).negate());
        preview.setLineCount(countLines(voucherId));
        preview.setWillSetReversed(true);
        for (ErpFinVoucherBillR link : loadBillLinks(voucherId)) {
            VoucherReversePreview.BillLinkInfo info = new VoucherReversePreview.BillLinkInfo();
            info.setBillType(link.getBillType());
            info.setBillCode(link.getBillCode());
            info.setBusinessType(link.getBusinessType());
            preview.getBillLinks().add(info);
        }
        return preview;
    }

    // 同聚合子表只读加载（对齐 ErpFinReconciliationBizModel.loadLines 的 D2 边界场景，
    // 显式查询避免依赖 to-many 懒加载的会话存活）。
    private int countLines(String voucherId) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider().daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return dao.findAllByQuery(q).size();
    }

    private List<ErpFinVoucherBillR> loadBillLinks(String voucherId) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider().daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return dao.findAllByQuery(q);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    /**
     * 期间状态守卫（P1-MA2-021）：凭证所属期间已 CLOSED/CLOSED_FINAL 时禁止过账/红冲。
     * owner doc state-machine.md §期间控制「CLOSED/CLOSED_FINAL 可修改凭证=否」。
     */
    private void assertPeriodNotLocked(ErpFinVoucher voucher) {
        String periodId = voucher.getPeriodId();
        if (periodId == null) {
            return;
        }
        IEntityDao<app.erp.fin.dao.entity.ErpFinAccountingPeriod> dao =
                daoProvider().daoFor(app.erp.fin.dao.entity.ErpFinAccountingPeriod.class);
        app.erp.fin.dao.entity.ErpFinAccountingPeriod period = dao.getEntityById(periodId);
        if (period == null) {
            return;
        }
        String status = period.getStatus();
        if (ErpFinConstants.PERIOD_STATUS_CLOSED.equals(status)
                || ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL.equals(status)) {
            throw new NopException(ErpFinErrors.ERR_FIN_VOUCHER_PERIOD_LOCKED)
                    .param(ErpFinErrors.ARG_VOUCHER_ID, voucher.getId())
                    .param(ErpFinErrors.ARG_PERIOD_STATUS, status);
        }
    }

    /**
     * F2.1（P1-CK-fin-002）：过账边借贷平衡断言——聚合凭证行 Σdebit vs Σcredit（null→ZERO，
     * compareTo 容忍 scale 差异；负数红字风格天然对称通过）。通过后重算头合计并随本次
     * updateEntity 持久化（修复手工 CRUD 凭证头合计 stale/null 漂移）。零行凭证 Σ0==Σ0 通过
     * （期末零凭证先例 CloseVoucherWriter L80 单独拒绝零合计，此处不重复裁决）。
     */
    protected void assertBalancedFromLines(ErpFinVoucher voucher) {
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("voucherId", voucher.getId()));
        java.util.List<ErpFinVoucherLine> lines = daoProvider().daoFor(ErpFinVoucherLine.class).findAllByQuery(q);
        java.math.BigDecimal debit = java.math.BigDecimal.ZERO;
        java.math.BigDecimal credit = java.math.BigDecimal.ZERO;
        for (ErpFinVoucherLine line : lines) {
            debit = debit.add(line.getDebitAmount() != null ? line.getDebitAmount() : java.math.BigDecimal.ZERO);
            credit = credit.add(line.getCreditAmount() != null ? line.getCreditAmount() : java.math.BigDecimal.ZERO);
        }
        if (debit.compareTo(credit) != 0) {
            throw new NopException(app.erp.fin.service.posting.ErpFinPostingErrors.ERR_UNBALANCED)
                    .param("totalDebit", debit)
                    .param("totalCredit", credit)
                    .param(ErpFinErrors.ARG_VOUCHER_ID, voucher.getId());
        }
        voucher.setTotalDebit(debit);
        voucher.setTotalCredit(credit);
    }
}