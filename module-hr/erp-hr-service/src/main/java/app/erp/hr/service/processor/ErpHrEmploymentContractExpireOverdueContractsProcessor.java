package app.erp.hr.service.processor;

import app.erp.hr.dao.entity.ErpHrEmploymentContract;
import app.erp.hr.service.ErpHrConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.lt;

/**
 * ErpHrEmploymentContract expireOverdueContracts per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含逾期合同批量到期编排（ACTIVE 且 endDate &lt; today → EXPIRED，逐条容错跳过失败项并告警）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpHrEmploymentContractExpireOverdueContractsProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpHrEmploymentContractExpireOverdueContractsProcessor.class);

    @Inject
    IDaoProvider daoProvider;

    public List<ErpHrEmploymentContract> expireOverdueContracts(IServiceContext context) {
        LocalDate now = CoreMetrics.today();
        QueryBean q = new QueryBean();
        q.addFilter(eq("status", ErpHrConstants.CONTRACT_STATUS_ACTIVE));
        q.addFilter(lt("endDate", now));
        List<ErpHrEmploymentContract> overdue = contractDao().findAllByQuery(q);
        List<ErpHrEmploymentContract> expired = new ArrayList<>();
        for (ErpHrEmploymentContract c : overdue) {
            try {
                c.setStatus(ErpHrConstants.CONTRACT_STATUS_EXPIRED);
                contractDao().updateEntity(c);
                expired.add(c);
            } catch (Exception ex) {
                LOG.warn("contract-expire-failed: id={}, reason={}", c.getId(), ex.getMessage());
            }
        }
        return expired;
    }

    private IEntityDao<ErpHrEmploymentContract> contractDao() {
        return daoProvider.daoFor(ErpHrEmploymentContract.class);
    }
}
