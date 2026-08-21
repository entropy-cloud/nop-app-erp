
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinIntercompanyMatchBiz;
import app.erp.fin.dao.dto.DualSideDiffReport;
import app.erp.fin.dao.dto.DualSideDiffReport.DualSideDiffRow;
import app.erp.fin.dao.entity.ErpFinIntercompanyMatch;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.processor.ErpFinIntercompanyMatchRunMatchingProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 公司间自动配对 BizModel（plan 2026-07-22-1000-1 A3，multi-company.md §公司间自动配对算法）。
 *
 * <p>{@code runMatching(periodId)} 委派 {@link ErpFinIntercompanyMatchRunMatchingProcessor}。
 * {@code checkDualSideConsistency} 为只读 @BizQuery（保留 BizModel）。
 *
 * <p>权威：{@code docs/architecture/multi-company.md §Decision C}。
 */
@BizModel("ErpFinIntercompanyMatch")
public class ErpFinIntercompanyMatchBizModel extends CrudBizModel<ErpFinIntercompanyMatch>
        implements IErpFinIntercompanyMatchBiz {

    @Inject
    ErpFinIntercompanyMatchRunMatchingProcessor runMatchingProcessor;

    public ErpFinIntercompanyMatchBizModel() {
        setEntityName(ErpFinIntercompanyMatch.class.getName());
    }

    @Override
    @BizMutation
    public int runMatching(@Name("periodId") String periodId, IServiceContext context) {
        return runMatchingProcessor.runMatching(periodId, context);
    }

    @Override
    @BizQuery
    public DualSideDiffReport checkDualSideConsistency(@Name("pairKey") String pairKey,
                                                @Name("periodId") String periodId,
                                                IServiceContext context) {
        DualSideDiffReport report = new DualSideDiffReport();
        report.setDirection("INTERCOMPANY");

        QueryBean q = new QueryBean();
        q.addFilter(eq("pairKey", pairKey));
        if (periodId != null) {
            q.addFilter(eq("periodId", periodId));
        }
        List<ErpFinIntercompanyMatch> matches = daoProvider().daoFor(ErpFinIntercompanyMatch.class).findAllByQuery(q);

        if (matches.isEmpty()) {
            report.setConsistent(true);
            return report;
        }

        boolean allConsistent = true;
        for (ErpFinIntercompanyMatch m : matches) {
            DualSideDiffRow row = new DualSideDiffRow();
            row.setPartnerId(m.getArOrgId());
            row.setFinanceSettled(m.getMatchedAmount());
            row.setDomainSettled(m.getMatchedAmount());
            row.setDiff(m.getDiffAmount());
            boolean consistent = ErpFinConstants.INTERCOMPANY_MATCH_MATCHED.equals(m.getStatus());
            row.setStatus(consistent ? "CONSISTENT" : "INCONSISTENT");
            report.getRows().add(row);
            if (!consistent) {
                allConsistent = false;
            }
        }
        report.setConsistent(allConsistent);
        return report;
    }

}
