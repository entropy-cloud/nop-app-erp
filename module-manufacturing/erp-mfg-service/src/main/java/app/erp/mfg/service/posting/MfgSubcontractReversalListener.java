package app.erp.mfg.service.posting;

import app.erp.fin.service.posting.IErpFinVoucherReversedListener;
import app.erp.fin.service.posting.VoucherPostedEvent;
import app.erp.fin.service.posting.VoucherReversedEvent;
import app.erp.fin.service.posting.IErpFinVoucherPostedListener;
import app.erp.mfg.dao.entity.ErpMfgSubcontractOrder;
import app.erp.mfg.service.ErpMfgConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoEntity;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpMfgSubcontractOrder）=同域实体批量聚合，批量读写，写路径经编排层 Facade 事务边界承接。
/**
 * 委外加工凭证红冲监听者（业财闭环方向二：财务侧红冲→业务单据回退，M5.2 覆盖 manufacturing 委外段）。
 *
 * <p>财务员直接红冲已过账凭证时，finance 引擎派发 {@link VoucherReversedEvent}，本监听者据此回退委外单状态
 * （设计 {@code posting.md §冲销机制方向二 §裁决4 回退目标态表}）。镜像 {@code PurReversalListener} 范式结构
 * （switch businessType → findByCode → posted==true 前置 → 回退），但<b>回退字段不同</b>：
 * PurReversalListener 回退 {@code approveStatus APPROVED→REJECTED}（采购单为审批轴驱动），
 * 本监听者回退 {@code docStatus→CANCELLED}（委外单为 docStatus 驱动，COMPLETED 时 approveStatus 已 APPROVED
 * 且 CANCELLED 为终态，无 approveStatus 回退）。
 *
 * <p>三段业务类型共用同一委外单，billHeadCode 为 {@code orderCode + "-SI"/"-SR"/"-SF"}，
 * 经去后缀反查委外单 code。幂等安全：{@code posted==false} 时 no-op，与域级 {@code reverseCompletion} 无双重处理。
 *
 * <p>监听者失败经 {@code ErpFinReversalListenerRegistry.dispatch} 的 try/catch 隔离，不阻断其他域监听者、
 * 不回滚已过账红字凭证。
 */
public class MfgSubcontractReversalListener implements IErpFinVoucherReversedListener, IErpFinVoucherPostedListener {

    static final String SUFFIX_ISSUE = "-SI";
    static final String SUFFIX_RECEIPT = "-SR";
    static final String SUFFIX_FEE = "-SF";

    @Inject
    IDaoProvider daoProvider;

    /**
     * F2.1（P1-CK-fin-001）：正向过账成功回写。mfg 域覆盖：SUBCONTRACT_FEE（strip -SF）→
     * SubcontractOrder（dispatcher 仅 fee 段 markPosted，issue/receipt 段无 posted 追踪不回写）；
     * MANUFACTURING_ISSUE（strip -MI）→ MaterialIssue；PRODUCTION_VARIANCE（strip -PV）→
     * WorkOrder 名下 CostVariance 行 posted=true（对齐 ProductionVarianceDispatcher.markPosted，
     * 该实体无 postedAt/postedBy 列）。
     */
    @Override
    public void onVoucherPosted(VoucherPostedEvent event, IServiceContext context) {
        String businessType = event.getBusinessType();
        if (businessType == null) {
            return;
        }
        java.sql.Timestamp now = io.nop.api.core.time.CoreMetrics.currentTimestamp();
        switch (businessType) {
            case "SUBCONTRACT_FEE":
                markPosted(findByCode(ErpMfgSubcontractOrder.class,
                        stripBillHeadSuffix(event.getBillHeadCode())), now);
                break;
            case "MANUFACTURING_ISSUE":
                markPosted(findByCode(app.erp.mfg.dao.entity.ErpMfgMaterialIssue.class,
                        stripSuffix(event.getBillHeadCode(), "-MI")), now);
                break;
            case "PRODUCTION_VARIANCE":
                markVariancePosted(stripSuffix(event.getBillHeadCode(), "-PV"));
                break;
            default:
                break;
        }
    }

    private static String stripSuffix(String code, String suffix) {
        return code != null && code.endsWith(suffix) ? code.substring(0, code.length() - suffix.length()) : code;
    }

    /** 生产差异回写：按工单 code 反查名下全部 CostVariance 行置 posted（镜像 dispatcher 的 markPosted(lines)）。 */
    private void markVariancePosted(String workOrderCode) {
        if (workOrderCode == null) {
            return;
        }
        app.erp.mfg.dao.entity.ErpMfgWorkOrder wo = findByCode(
                app.erp.mfg.dao.entity.ErpMfgWorkOrder.class, workOrderCode);
        if (wo == null) {
            return;
        }
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("workOrderId", wo.getId()));
        java.util.List<app.erp.mfg.dao.entity.ErpMfgCostVariance> lines =
                daoProvider.daoFor(app.erp.mfg.dao.entity.ErpMfgCostVariance.class).findAllByQuery(q);
        io.nop.dao.api.IEntityDao<app.erp.mfg.dao.entity.ErpMfgCostVariance> dao =
                daoProvider.daoFor(app.erp.mfg.dao.entity.ErpMfgCostVariance.class);
        for (app.erp.mfg.dao.entity.ErpMfgCostVariance line : lines) {
            if (!Boolean.TRUE.equals(line.getPosted())) {
                line.setPosted(true);
                dao.updateEntity(line);
            }
        }
    }

    private void markPosted(ErpMfgSubcontractOrder order, java.sql.Timestamp now) {
        if (order == null || Boolean.TRUE.equals(order.getPosted())) {
            return;
        }
        order.setPosted(true);
        order.setPostedAt(now);
        order.setPostedBy(currentUserId());
        daoProvider.daoFor(ErpMfgSubcontractOrder.class).updateEntity(order);
    }

    private void markPosted(app.erp.mfg.dao.entity.ErpMfgMaterialIssue issue, java.sql.Timestamp now) {
        if (issue == null || Boolean.TRUE.equals(issue.getPosted())) {
            return;
        }
        issue.setPosted(true);
        issue.setPostedAt(now);
        issue.setPostedBy(currentUserId());
        daoProvider.daoFor(app.erp.mfg.dao.entity.ErpMfgMaterialIssue.class).updateEntity(issue);
    }

    private static String currentUserId() {
        io.nop.api.core.auth.IUserContext ctx = io.nop.api.core.auth.IUserContext.get();
        return ctx != null ? ctx.getUserId() : null;
    }

    @Override
    public void onVoucherReversed(VoucherReversedEvent event, IServiceContext context) {
        String businessType = event.getBusinessType();
        if (businessType == null) {
            return;
        }
        switch (businessType) {
            case "SUBCONTRACT_ISSUE":
            case "SUBCONTRACT_RECEIPT":
            case "SUBCONTRACT_FEE":
                rollbackSubcontractOrder(event);
                break;
            default:
                // 非委外域业务类型——忽略（其他域监听者处理）
                break;
        }
    }

    protected void rollbackSubcontractOrder(VoucherReversedEvent event) {
        String orderCode = stripBillHeadSuffix(event.getBillHeadCode());
        if (orderCode == null) {
            return;
        }
        ErpMfgSubcontractOrder order = findByCode(ErpMfgSubcontractOrder.class, orderCode);
        if (order == null || !Boolean.TRUE.equals(order.getPosted())) {
            return;
        }
        order.setPosted(false);
        order.setPostedAt(null);
        order.setPostedBy(null);
        order.setDocStatus(ErpMfgConstants.SUBCONTRACT_STATUS_CANCELLED);
        daoProvider.daoFor(ErpMfgSubcontractOrder.class).updateEntity(order);
    }

    /**
     * 去除 billHeadCode 的委外段后缀（-SI/-SR/-SF），还原委外单 code。
     */
    protected String stripBillHeadSuffix(String billHeadCode) {
        if (billHeadCode == null) {
            return null;
        }
        if (billHeadCode.endsWith(SUFFIX_FEE)) {
            return billHeadCode.substring(0, billHeadCode.length() - SUFFIX_FEE.length());
        }
        if (billHeadCode.endsWith(SUFFIX_RECEIPT)) {
            return billHeadCode.substring(0, billHeadCode.length() - SUFFIX_RECEIPT.length());
        }
        if (billHeadCode.endsWith(SUFFIX_ISSUE)) {
            return billHeadCode.substring(0, billHeadCode.length() - SUFFIX_ISSUE.length());
        }
        return billHeadCode;
    }

    protected <T extends IDaoEntity> T findByCode(Class<T> entityClass, String code) {
        if (code == null) {
            return null;
        }
        IEntityDao<T> dao = daoProvider.daoFor(entityClass);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        List<T> list = dao.findAllByQuery(q);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }
}
