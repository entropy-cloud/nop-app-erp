
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinBudgetLineBiz;
import app.erp.fin.dao.dto.BudgetVsActualRow;
import app.erp.fin.dao.entity.ErpFinBudgetLine;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.isNull;
import static io.nop.api.core.beans.FilterBeans.ne;
import static io.nop.api.core.beans.FilterBeans.or;

/**
 * 预算明细行 Biz（CrudBizModel）。标准 CRUD + {@link #getBudgetVsActual} 预算对比查询。
 *
 * <p>预算对比（budget.md §业务规则5）：按 {@code (subjectId, periodId, costCenterId)} 维度从 {@link ErpFinVoucherLine}
 * 关联凭证 {@code postingType} 三通道聚合——BUDGET 凭证行=预算数，COMMITMENT 凭证行=承付款，
 * 其余（NORMAL/NULL/RESERVATION）凭证行=实际数，余量=预算−实际−承付（对齐控制引擎 P1-MA2-084 三通道分离口径）。
 */
@BizModel("ErpFinBudgetLine")
public class ErpFinBudgetLineBizModel extends CrudBizModel<ErpFinBudgetLine> implements IErpFinBudgetLineBiz {

    /** 聚合通道：BUDGET 凭证 → 预算数。 */
    private static final String CHANNEL_BUDGET = "budget";
    /** 聚合通道：COMMITMENT 凭证 → 承付款。 */
    private static final String CHANNEL_COMMITMENT = "commitment";
    /** 聚合通道：其余（NORMAL/NULL/RESERVATION）凭证 → 实际数。 */
    private static final String CHANNEL_ACTUAL = "actual";

    public ErpFinBudgetLineBizModel() {
        setEntityName(ErpFinBudgetLine.class.getName());
    }

    @Override
    @BizQuery
    public List<BudgetVsActualRow> getBudgetVsActual(@Name("acctSchemaId") Long acctSchemaId,
                                                     @Name("periodId") Long periodId,
                                                     @Name("subjectId") Long subjectId,
                                                     IServiceContext context) {
        IDaoProvider dp = daoProvider();
        IEntityDao<ErpFinVoucher> voucherDao = dp.daoFor(ErpFinVoucher.class);
        QueryBean vq = new QueryBean();
        vq.addFilter(eq("docStatus", ErpFinConstants.VOUCHER_STATUS_POSTED));
        vq.addFilter(eq("isReversed", Boolean.FALSE));
        if (acctSchemaId != null) {
            vq.addFilter(eq("acctSchemaId", acctSchemaId));
        }
        if (periodId != null) {
            vq.addFilter(eq("periodId", periodId));
        }
        // 加载全部过账凭证（BUDGET/COMMITMENT/其余三通道均需纳入聚合；恒真式过滤保留以显式表达
        // "不过滤通道"语义——不承担通道分流，三通道分类在下方内存内 per-voucher 谓词处完成）
        vq.addFilter(or(eq("postingType", ErpFinConstants.POSTING_TYPE_BUDGET),
                or(isNull("postingType"), ne("postingType", ErpFinConstants.POSTING_TYPE_BUDGET))));
        List<ErpFinVoucher> vouchers = voucherDao.findAllByQuery(vq);
        if (vouchers.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, String> voucherChannel = new HashMap<>();
        for (ErpFinVoucher v : vouchers) {
            voucherChannel.put(v.getId(), channelOf(v.getPostingType()));
        }

        IEntityDao<ErpFinVoucherLine> lineDao = dp.daoFor(ErpFinVoucherLine.class);
        QueryBean lq = new QueryBean();
        lq.addFilter(in("voucherId", voucherChannel.keySet()));
        if (subjectId != null) {
            lq.addFilter(eq("subjectId", subjectId));
        }
        List<ErpFinVoucherLine> lines = lineDao.findAllByQuery(lq);

        Map<String, BudgetVsActualRow> agg = new HashMap<>();
        Map<Long, ErpMdSubject> subjectCache = new HashMap<>();
        for (ErpFinVoucherLine l : lines) {
            if (l.getSubjectId() == null) {
                continue;
            }
            String channel = voucherChannel.get(l.getVoucherId());
            if (channel == null) {
                continue;
            }
            ErpMdSubject subject = subjectCache.computeIfAbsent(l.getSubjectId(), this::loadSubject);
            String key = l.getSubjectId() + "|" + l.getCostCenterId() + "|" + l.getProjectId();
            BudgetVsActualRow row = agg.computeIfAbsent(key, k -> newRow(l, subject));
            BigDecimal amount = nz(l.getAmountFunctional());
            if (CHANNEL_BUDGET.equals(channel)) {
                row.setBudgetAmount(row.getBudgetAmount().add(amount));
            } else if (CHANNEL_COMMITMENT.equals(channel)) {
                row.setCommitmentAmount(row.getCommitmentAmount().add(amount));
            } else {
                row.setActualAmount(row.getActualAmount().add(amount));
            }
        }
        for (BudgetVsActualRow row : agg.values()) {
            row.setAvailableAmount(row.getBudgetAmount().subtract(row.getActualAmount()).subtract(row.getCommitmentAmount()));
        }
        return new ArrayList<>(agg.values());
    }

    /** per-voucher 通道分类：BUDGET → budget；COMMITMENT → commitment；其余（NORMAL/NULL/RESERVATION）→ actual。 */
    private static String channelOf(String postingType) {
        if (ErpFinConstants.POSTING_TYPE_BUDGET.equals(postingType)) {
            return CHANNEL_BUDGET;
        }
        if (ErpFinConstants.POSTING_TYPE_COMMITMENT.equals(postingType)) {
            return CHANNEL_COMMITMENT;
        }
        return CHANNEL_ACTUAL;
    }

    private BudgetVsActualRow newRow(ErpFinVoucherLine l, ErpMdSubject subject) {
        BudgetVsActualRow row = new BudgetVsActualRow();
        row.setSubjectId(l.getSubjectId());
        row.setSubjectCode(subject != null ? subject.getCode() : l.getSubjectCode());
        row.setSubjectName(subject != null ? subject.getName() : l.getSubjectName());
        row.setCostCenterId(l.getCostCenterId());
        row.setProjectId(l.getProjectId());
        return row;
    }

    private ErpMdSubject loadSubject(Long id) {
        return daoProvider().daoFor(ErpMdSubject.class).getEntityById(id);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    // subjectCode 为预算行已持久化冗余列；subjectName 派生读取 ErpMdSubject.name。

}
