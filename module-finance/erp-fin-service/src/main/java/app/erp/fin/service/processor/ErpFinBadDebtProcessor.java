package app.erp.fin.service.processor;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinBadDebt;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.close.CloseVoucherWriter;
import app.erp.fin.service.close.CloseVoucherWriter.Line;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.dao.IOrmEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 坏账核销/收回审批状态机编排 Processor（{@code processor-extension-pattern.md} 两层结构：Facade + Processor）。
 * Facade {@code ErpFinBadDebtBizModel} 仅负责入口/事务/委托，编排委托本类。
 *
 * <p>承载 writeOff/recovery 两类坏账事件（{@code bad-debt.md §步骤3 核销 / §步骤4a 恢复}）：
 * <ul>
 *   <li>{@link #writeOff(Long, String, IServiceContext)} 创建 WRITE_OFF 坏账单（金额 = 源 AR 辅助账项 openAmount）</li>
 *   <li>{@link #recover(Long, String, IServiceContext)} 创建 RECOVERY 坏账单（恢复已核销项）</li>
 *   <li>{@link #approve(Long, IServiceContext)} 审批通过后执行：变异 ArApItem（status/openAmount）+ 生成凭证</li>
 * </ul>
 *
 * <p>审批门控：{@code erp-fin.bad-debt-write-off-require-approval}（默认 true）时核销/恢复强制审批；
 * 关闭时创建即自动审批执行。计提/释放（批量估计）不经本类，归 {@code BadDebtProvisionService}。
 *
 * <p>事务边界：跟随 Facade {@code @BizMutation}+{@code @SingleSession} 事务，本类不带 {@code @Transactional}。
 */
public class ErpFinBadDebtProcessor {

    @Inject
    IDaoProvider daoProvider;

    // ===================== 创建坏账单 =====================

    public ErpFinBadDebt writeOff(Long arApItemId, String reason, IServiceContext context) {
        ErpFinArApItem item = requireOpenArApItem(arApItemId);
        ErpFinBadDebt debt = newBadDebt(ErpFinConstants.BAD_DEBT_TYPE_WRITE_OFF, item, item.getOpenAmountFunctional(), reason);
        if (!isWriteOffApprovalRequired()) {
            // 无需审批：先执行生效（变异 ArApItem + 凭证），再一次性保存（避免新建态 update 报错）。
            executeWriteOff(debt, item, context);
            debt.setApprovalStatus(ErpFinConstants.APPROVE_STATUS_APPROVED);
        }
        badDebtDao().saveEntity(debt);
        return debt;
    }

    public ErpFinBadDebt recover(Long arApItemId, String reason, IServiceContext context) {
        ErpFinArApItem item = requireWrittenOffArApItem(arApItemId);
        ErpFinBadDebt debt = newBadDebt(ErpFinConstants.BAD_DEBT_TYPE_RECOVERY, item, debtAmountOf(item), reason);
        if (!isWriteOffApprovalRequired()) {
            executeRecovery(debt, item, context);
            debt.setApprovalStatus(ErpFinConstants.APPROVE_STATUS_APPROVED);
        }
        badDebtDao().saveEntity(debt);
        return debt;
    }

    // ===================== 审批状态机 =====================

    public ErpFinBadDebt submit(Long badDebtId, IServiceContext context) {
        ErpFinBadDebt debt = requireBadDebt(badDebtId);
        validateTransitionForSubmit(debt);
        doSubmit(debt);
        return debt;
    }

    public ErpFinBadDebt approve(Long badDebtId, IServiceContext context) {
        ErpFinBadDebt debt = requireBadDebt(badDebtId);
        if (isAlreadyApproved(debt)) {
            return debt;
        }
        validateTransitionForApprove(debt);
        return approveInternal(debt, loadArApItem(debt.getSourceArApItemId()), context);
    }

    public ErpFinBadDebt reject(Long badDebtId, IServiceContext context) {
        ErpFinBadDebt debt = requireBadDebt(badDebtId);
        validateTransitionForReject(debt);
        doReject(debt);
        return debt;
    }

    // ===================== step：执行（审批通过后生效） =====================

    protected ErpFinBadDebt approveInternal(ErpFinBadDebt debt, ErpFinArApItem item, IServiceContext context) {
        if (Objects.equals(debt.getDocType(), ErpFinConstants.BAD_DEBT_TYPE_WRITE_OFF)) {
            executeWriteOff(debt, item, context);
        } else {
            executeRecovery(debt, item, context);
        }
        debt.setApprovalStatus(ErpFinConstants.APPROVE_STATUS_APPROVED);
        badDebtDao().updateEntity(debt);
        return debt;
    }

    /**
     * 执行坏账核销（§步骤3）：ArApItem status→WRITTEN_OFF + openAmount→0 + BAD_DEBT_WRITE_OFF 凭证（借Allowance/贷AR，不进 P&L）。
     * protected：下游可覆盖凭证生成或辅助账变异逻辑。
     */
    protected void executeWriteOff(ErpFinBadDebt debt, ErpFinArApItem item, IServiceContext context) {
        BigDecimal amount = debt.getAmount();
        validateAmount(amount, item);
        item.setSettledAmountFunctional(nz(item.getSettledAmountFunctional()).add(amount));
        item.setSettledAmountSource(nz(item.getSettledAmountSource()).add(amount));
        item.setOpenAmountFunctional(nz(item.getOpenAmountFunctional()).subtract(amount));
        item.setOpenAmountSource(nz(item.getOpenAmountSource()).subtract(amount));
        item.setStatus(ErpFinConstants.AR_AP_STATUS_WRITTEN_OFF);
        arApItemDao().updateEntity(item);

        ErpMdSubject allowance = requireSubject(ErpFinConstants.CONFIG_BAD_DEBT_ALLOWANCE_SUBJECT_CODE);
        ErpMdSubject ar = requireSubject(ErpFinConstants.CONFIG_AR_SUBJECT_CODE);
        List<Line> lines = Arrays.asList(
                new Line(allowance.getId(), allowance.getCode(), allowance.getName(),
                        ErpFinConstants.DC_DEBIT, amount, item.getPartnerId()),
                new Line(ar.getId(), ar.getCode(), ar.getName(),
                        ErpFinConstants.DC_CREDIT, amount, item.getPartnerId()));
        Long voucherId = writeBadDebtVoucher(debt, item, ErpFinBusinessType.BAD_DEBT_WRITE_OFF, "坏账核销", lines);
        debt.setVoucherId(voucherId);
    }

    /**
     * 执行坏账收回恢复（§步骤4a）：ArApItem 回退正常态（WRITTEN_OFF→OPEN）+ 恢复 openAmount + BAD_DEBT_RECOVERY 凭证（借AR/贷Allowance）。
     * protected：下游可覆盖。
     */
    protected void executeRecovery(ErpFinBadDebt debt, ErpFinArApItem item, IServiceContext context) {
        BigDecimal amount = debt.getAmount();
        item.setSettledAmountFunctional(nz(item.getSettledAmountFunctional()).subtract(amount));
        item.setSettledAmountSource(nz(item.getSettledAmountSource()).subtract(amount));
        item.setOpenAmountFunctional(nz(item.getOpenAmountFunctional()).add(amount));
        item.setOpenAmountSource(nz(item.getOpenAmountSource()).add(amount));
        item.setStatus(ErpFinConstants.AR_AP_STATUS_OPEN);
        arApItemDao().updateEntity(item);

        ErpMdSubject allowance = requireSubject(ErpFinConstants.CONFIG_BAD_DEBT_ALLOWANCE_SUBJECT_CODE);
        ErpMdSubject ar = requireSubject(ErpFinConstants.CONFIG_AR_SUBJECT_CODE);
        List<Line> lines = Arrays.asList(
                new Line(ar.getId(), ar.getCode(), ar.getName(),
                        ErpFinConstants.DC_DEBIT, amount, item.getPartnerId()),
                new Line(allowance.getId(), allowance.getCode(), allowance.getName(),
                        ErpFinConstants.DC_CREDIT, amount, item.getPartnerId()));
        Long voucherId = writeBadDebtVoucher(debt, item, ErpFinBusinessType.BAD_DEBT_RECOVERY, "坏账收回恢复", lines);
        debt.setVoucherId(voucherId);
    }

    protected Long writeBadDebtVoucher(ErpFinBadDebt debt, ErpFinArApItem item, ErpFinBusinessType businessType,
                                       String memo, List<Line> lines) {
        return CloseVoucherWriter.writeVoucher(daoProvider, "BD",
                debt.getCode(), businessType.name(), businessType.name(),
                debt.getOrgId(), debt.getAcctSchemaId(), debt.getPeriodId(), debt.getCurrencyId(),
                nz(debt.getExchangeRate()), debt.getBusinessDate(), lines, memo);
    }

    // ===================== step：迁移校验 =====================

    protected void validateTransitionForSubmit(ErpFinBadDebt debt) {
        String status = currentApprovalStatus(debt);
        if (!Objects.equals(status, ErpFinConstants.APPROVE_STATUS_UNSUBMITTED)) {
            throw illegalTransition(debt, status, "UNSUBMITTED");
        }
    }

    protected void validateTransitionForApprove(ErpFinBadDebt debt) {
        String status = currentApprovalStatus(debt);
        if (!Objects.equals(status, ErpFinConstants.APPROVE_STATUS_SUBMITTED)
                && !Objects.equals(status, ErpFinConstants.APPROVE_STATUS_UNSUBMITTED)) {
            throw illegalTransition(debt, status, "SUBMITTED 或 UNSUBMITTED");
        }
    }

    protected void validateTransitionForReject(ErpFinBadDebt debt) {
        String status = currentApprovalStatus(debt);
        if (!Objects.equals(status, ErpFinConstants.APPROVE_STATUS_SUBMITTED)
                && !Objects.equals(status, ErpFinConstants.APPROVE_STATUS_UNSUBMITTED)) {
            throw illegalTransition(debt, status, "SUBMITTED 或 UNSUBMITTED");
        }
    }

    // ===================== step：状态推进 =====================

    protected void doSubmit(ErpFinBadDebt debt) {
        debt.setApprovalStatus(ErpFinConstants.APPROVE_STATUS_SUBMITTED);
        badDebtDao().updateEntity(debt);
    }

    protected void doReject(ErpFinBadDebt debt) {
        debt.setApprovalStatus(ErpFinConstants.APPROVE_STATUS_REJECTED);
        badDebtDao().updateEntity(debt);
    }

    // ===================== 校验/查询辅助 =====================

    protected ErpFinArApItem requireOpenArApItem(Long arApItemId) {
        ErpFinArApItem item = arApItemDao().getEntityById(arApItemId);
        if (item == null) {
            throw new NopException(ErpFinErrors.ERR_AR_AP_ITEM_NOT_FOUND).param(ErpFinErrors.ARG_ID, arApItemId);
        }
        String status = item.getStatus();
        if (!Objects.equals(status, ErpFinConstants.AR_AP_STATUS_OPEN)
                && !Objects.equals(status, ErpFinConstants.AR_AP_STATUS_PARTIAL)) {
            throw new NopException(ErpFinErrors.ERR_BAD_DEBT_AR_AP_ITEM_NOT_OPEN)
                    .param(ErpFinErrors.ARG_AR_AP_ITEM_ID, arApItemId);
        }
        if (item.getOpenAmountFunctional() == null || item.getOpenAmountFunctional().signum() <= 0) {
            throw new NopException(ErpFinErrors.ERR_BAD_DEBT_AR_AP_ITEM_NOT_OPEN)
                    .param(ErpFinErrors.ARG_AR_AP_ITEM_ID, arApItemId);
        }
        return item;
    }

    protected ErpFinArApItem requireWrittenOffArApItem(Long arApItemId) {
        ErpFinArApItem item = arApItemDao().getEntityById(arApItemId);
        if (item == null) {
            throw new NopException(ErpFinErrors.ERR_AR_AP_ITEM_NOT_FOUND).param(ErpFinErrors.ARG_ID, arApItemId);
        }
        if (!Objects.equals(item.getStatus(), ErpFinConstants.AR_AP_STATUS_WRITTEN_OFF)) {
            throw new NopException(ErpFinErrors.ERR_BAD_DEBT_AR_AP_ITEM_NOT_WRITTEN_OFF)
                    .param(ErpFinErrors.ARG_AR_AP_ITEM_ID, arApItemId);
        }
        return item;
    }

    protected void validateAmount(BigDecimal amount, ErpFinArApItem item) {
        if (amount == null || amount.signum() <= 0) {
            throw new NopException(ErpFinErrors.ERR_BAD_DEBT_AMOUNT_INVALID);
        }
        if (amount.compareTo(nz(item.getOpenAmountFunctional())) > 0) {
            throw new NopException(ErpFinErrors.ERR_BAD_DEBT_AMOUNT_OVER_OPEN)
                    .param(ErpFinErrors.ARG_WRITE_OFF_AMOUNT, amount)
                    .param(ErpFinErrors.ARG_OPEN_AMOUNT, item.getOpenAmountFunctional());
        }
    }

    protected ErpFinBadDebt requireBadDebt(Long badDebtId) {
        ErpFinBadDebt debt = badDebtDao().getEntityById(badDebtId);
        if (debt == null) {
            throw new NopException(ErpFinErrors.ERR_BAD_DEBT_NOT_FOUND).param(ErpFinErrors.ARG_BAD_DEBT_ID, badDebtId);
        }
        return debt;
    }

    protected ErpFinArApItem loadArApItem(Long arApItemId) {
        return arApItemDao().getEntityById(arApItemId);
    }

    protected ErpFinBadDebt newBadDebt(String docType, ErpFinArApItem item, BigDecimal amount, String reason) {
        ErpFinBadDebt debt = badDebtDao().newEntity();
        debt.setCode("BD-" + StringHelper.generateUUID().substring(0, 12));
        debt.setOrgId(item.getOrgId());
        debt.setAcctSchemaId(item.getAcctSchemaId());
        debt.setDocType(docType);
        debt.setPartnerId(item.getPartnerId());
        debt.setSourceArApItemId(item.getId());
        debt.setAmount(amount);
        debt.setCurrencyId(item.getCurrencyId());
        debt.setExchangeRate(nz(item.getExchangeRate()));
        debt.setBusinessDate(CoreMetrics.today());
        debt.setReason(reason);
        debt.setApprovalStatus(ErpFinConstants.APPROVE_STATUS_UNSUBMITTED);
        debt.setPeriodId(item.getPeriodId());
        return debt;
    }

    /** 恢复金额默认取核销时坏账单记录的金额（取最近一张该 item 的 WRITE_OFF 坏账单金额）。 */
    protected BigDecimal debtAmountOf(ErpFinArApItem item) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("sourceArApItemId", item.getId()));
        q.addFilter(eq("docType", ErpFinConstants.BAD_DEBT_TYPE_WRITE_OFF));
        q.addFilter(eq("approvalStatus", ErpFinConstants.APPROVE_STATUS_APPROVED));
        q.addOrderField("id", true);
        q.setLimit(1);
        List<ErpFinBadDebt> list = badDebtDao().findAllByQuery(q);
        if (!list.isEmpty() && list.get(0).getAmount() != null) {
            return list.get(0).getAmount();
        }
        return nz(item.getOpenAmountFunctional());
    }

    protected ErpMdSubject requireSubject(String configKey) {
        String code = AppConfig.var(configKey, null);
        if (code == null || code.isEmpty()) {
            throw new NopException(ErpFinErrors.ERR_CLOSE_SUBJECT_NOT_CONFIGURED)
                    .param(ErpFinErrors.ARG_CONFIG_KEY, configKey);
        }
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        List<ErpMdSubject> list = dao.findAllByQuery(q);
        if (list.isEmpty()) {
            throw new NopException(ErpFinErrors.ERR_CLOSE_SUBJECT_NOT_CONFIGURED)
                    .param(ErpFinErrors.ARG_CONFIG_KEY, configKey);
        }
        return list.get(0);
    }

    protected boolean isAlreadyApproved(ErpFinBadDebt debt) {
        String status = debt.getApprovalStatus();
        return status != null && Objects.equals(status, ErpFinConstants.APPROVE_STATUS_APPROVED);
    }

    protected String currentApprovalStatus(ErpFinBadDebt debt) {
        String status = debt.getApprovalStatus();
        return status != null ? status : ErpFinConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    protected boolean isWriteOffApprovalRequired() {
        Boolean flag = AppConfig.var(ErpFinConstants.CONFIG_BAD_DEBT_WRITE_OFF_REQUIRE_APPROVAL, Boolean.TRUE);
        return flag == null || flag;
    }

    protected NopException illegalTransition(ErpFinBadDebt debt, String current, String expected) {
        return new NopException(ErpFinErrors.ERR_BAD_DEBT_ILLEGAL_APPROVAL_TRANSITION)
                .param(ErpFinErrors.ARG_BAD_DEBT_CODE, debt.getCode())
                .param(ErpFinErrors.ARG_CURRENT_STATUS, current)
                .param(ErpFinErrors.ARG_EXPECTED_STATUS, expected);
    }

    // ---------- misc helpers ----------

    protected IEntityDao<ErpFinBadDebt> badDebtDao() {
        return daoProvider.daoFor(ErpFinBadDebt.class);
    }

    protected IEntityDao<ErpFinArApItem> arApItemDao() {
        return daoProvider.daoFor(ErpFinArApItem.class);
    }

    protected IOrmTemplate orm() {
        return ((IOrmEntityDao<?>) badDebtDao()).getOrmTemplate();
    }

    protected String currentUserId() {
        try {
            IUserContext ctx = IUserContext.get();
            return ctx == null ? null : ctx.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    protected static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
