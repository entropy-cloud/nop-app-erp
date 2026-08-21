package app.erp.fin.service.bankrecon;

import app.erp.fin.biz.IErpFinBankReconciliationBiz;
import app.erp.fin.dao.entity.ErpFinBankReconciliation;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.annotations.txn.TransactionPropagation;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.dao.txn.ITransactionTemplate;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.lt;

/**
 * 银行对账未达调整凭证自动红冲帮助类（plan 2026-08-07-1932-3 Phase 1，P1-RC-005 调度接线）。
 *
 * <p>供 nop-batch {@code bank-recon-auto-reverse.batch.xml} 的 processor 按记录调用：
 * 扫描「下月初」已过账（docStatus=POSTED）且未红冲的银行调节表（reconciliationDate 早于当月第一天），
 * 逐条经既有 {@link BankReconciliationBuilder#reverse} 入口红冲（BANK_RECON_ADJ 红字凭证 + docStatus→CANCELLED）。
 *
 * <p>双层门控：job 层 {@code nop.job.erp-fin-bank-recon-adj-reverse.enabled}（默认 false，部署 opt-in，
 * 见 job.yaml @cfg 引用）；本类消费业务 config {@code erp-fin.bank-recon-auto-reverse-next-month}
 * （默认 true，机制开关——false 时跳过并记录 INFO 日志，消除 A4.1.14 config 孤儿化）。
 *
 * <p>单条失败隔离：每条独立 REQUIRES_NEW 事务，失败（如启用期末结账部署下红冲撞 CLOSED 期间，
 * {@code ErpFinPostingProcessor.reverseProcess} 按原凭证日期解析期间 + resolveOpenPeriod CLOSED 拒红冲）
 * 记录 WARN 日志并返回 false，不阻断批次继续处理其余候选（CLOSED 碰撞从静默变为显式可观测）。
 */
public class ErpFinBankReconAutoReverseHelper {

    static final Logger LOG = LoggerFactory.getLogger(ErpFinBankReconAutoReverseHelper.class);

    @Inject
    IDaoProvider daoProvider;
    @Inject
    ITransactionTemplate transactionTemplate;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    BankReconciliationBuilder bankReconciliationBuilder;

    /**
     * 业务 config 机制开关：{@code erp-fin.bank-recon-auto-reverse-next-month} 默认 true。
     */
    public boolean isAutoReverseEnabled() {
        return AppConfig.var(ErpFinConstants.CONFIG_BANK_RECON_AUTO_REVERSE_NEXT_MONTH, true);
    }

    /**
     * 扫描全部跨期候选并逐条红冲（batch processor / 手动触发统一入口）。
     *
     * @return 红冲成功条数
     */
    public int reverseAll(IServiceContext ctx) {
        if (!isAutoReverseEnabled()) {
            LOG.info("erp-fin-bank-recon-auto-reverse-skipped-by-config: configKey={}",
                    ErpFinConstants.CONFIG_BANK_RECON_AUTO_REVERSE_NEXT_MONTH);
            return 0;
        }
        List<ErpFinBankReconciliation> candidates = findCandidates();
        int reversed = 0;
        for (ErpFinBankReconciliation recon : candidates) {
            if (reverseOne(recon.getId(), ctx)) {
                reversed++;
            }
        }
        return reversed;
    }

    /**
     * 红冲单条调节表。config=false 时跳过（INFO 日志，机制开关）。
     * 单条独立 REQUIRES_NEW 事务；失败记录 WARN 日志并返回 false（单条失败隔离，批次不中断）。
     *
     * @return true=红冲成功（或 config 关闭跳过）；false=红冲失败（候选保持 POSTED 下次重试）
     */
    public boolean reverseOne(String reconciliationId, IServiceContext ctx) {
        if (!isAutoReverseEnabled()) {
            LOG.info("erp-fin-bank-recon-auto-reverse-skipped-by-config: reconciliationId={}, configKey={}",
                    reconciliationId, ErpFinConstants.CONFIG_BANK_RECON_AUTO_REVERSE_NEXT_MONTH);
            return true;
        }
        try {
            return transactionTemplate.runInTransaction(null, TransactionPropagation.REQUIRES_NEW, txn ->
                    ormTemplate.runInSession(session -> {
                        ErpFinBankReconciliation recon = bankReconciliationBuilder.reverse(reconciliationId, ctx);
                        session.flush();
                        return true;
                    }));
        } catch (Exception e) {
            LOG.warn("erp-fin-bank-recon-auto-reverse-failed: reconciliationId={}, reason={}",
                    reconciliationId, e.getMessage());
            return false;
        }
    }

    /**
     * 候选 = docStatus=POSTED 且 reconciliationDate 早于当月第一天（跨期未达收敛；
     * 「< 当前月」而非「仅上月」——job 可能因停机错过首日运行，首次运行时收敛全部跨期未达）。
     *
     * <p>经 {@code IDaoProvider.daoFor} 直查（batch helper 非 BizModel，无法经 I*Biz 注入扫描查询——
     * 对齐 {@code ErpFinDeferredPostingRetryHelper} 既有 batch helper 范式，checker R2c 基线含同型站点）。
     */
    protected List<ErpFinBankReconciliation> findCandidates() {
        IEntityDao<ErpFinBankReconciliation> dao = daoProvider.daoFor(ErpFinBankReconciliation.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("docStatus", ErpFinConstants.VOUCHER_STATUS_POSTED));
        q.addFilter(lt("reconciliationDate", LocalDate.now().withDayOfMonth(1)));
        return dao.findAllByQuery(q);
    }
}
