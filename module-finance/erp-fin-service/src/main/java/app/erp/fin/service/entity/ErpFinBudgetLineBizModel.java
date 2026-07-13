
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinBudgetLineBiz;
import app.erp.fin.dao.dto.BudgetVsActualRow;
import app.erp.fin.dao.entity.ErpFinBudgetLine;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
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
 * 关联凭证 {@code postingType} 聚合，BUDGET 凭证行=预算数，NORMAL 凭证行=实际数，余量=预算−实际。
 */
@BizModel("ErpFinBudgetLine")
public class ErpFinBudgetLineBizModel extends CrudBizModel<ErpFinBudgetLine> implements IErpFinBudgetLineBiz {

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
        // 仅取预算(BUDGET) + 实际(NORMAL/NULL) 两类凭证
        vq.addFilter(or(eq("postingType", ErpFinConstants.POSTING_TYPE_BUDGET),
                or(isNull("postingType"), ne("postingType", ErpFinConstants.POSTING_TYPE_BUDGET))));
        List<ErpFinVoucher> vouchers = voucherDao.findAllByQuery(vq);
        if (vouchers.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, Boolean> voucherBudgetFlag = new HashMap<>();
        for (ErpFinVoucher v : vouchers) {
            voucherBudgetFlag.put(v.getId(),
                    ErpFinConstants.POSTING_TYPE_BUDGET.equals(v.getPostingType()));
        }

        IEntityDao<ErpFinVoucherLine> lineDao = dp.daoFor(ErpFinVoucherLine.class);
        QueryBean lq = new QueryBean();
        lq.addFilter(in("voucherId", voucherBudgetFlag.keySet()));
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
            Boolean isBudget = voucherBudgetFlag.get(l.getVoucherId());
            if (isBudget == null) {
                continue;
            }
            ErpMdSubject subject = subjectCache.computeIfAbsent(l.getSubjectId(), this::loadSubject);
            String key = l.getSubjectId() + "|" + l.getCostCenterId() + "|" + l.getProjectId();
            BudgetVsActualRow row = agg.computeIfAbsent(key, k -> newRow(l, subject));
            BigDecimal amount = nz(l.getAmountFunctional());
            if (isBudget) {
                row.setBudgetAmount(row.getBudgetAmount().add(amount));
            } else {
                row.setActualAmount(row.getActualAmount().add(amount));
            }
        }
        for (BudgetVsActualRow row : agg.values()) {
            row.setAvailableAmount(row.getBudgetAmount().subtract(row.getActualAmount()));
        }
        return new ArrayList<>(agg.values());
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

    // ---------- 高价值外键名称解析（机制 D）----------
    // subjectCode 为预算行已持久化冗余列；subjectName 派生读取 ErpMdSubject.name。

    @BizLoader(forType = ErpFinBudgetLine.class)
    public List<String> scenarioCode(@ContextSource List<ErpFinBudgetLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("scenario"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpFinBudgetLine line : lines) {
            result.add(line.getScenario() != null ? line.getScenario().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinBudgetLine.class)
    public List<String> orgName(@ContextSource List<ErpFinBudgetLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("org"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpFinBudgetLine line : lines) {
            result.add(line.getOrg() != null ? line.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinBudgetLine.class)
    public List<String> acctSchemaCode(@ContextSource List<ErpFinBudgetLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("acctSchema"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpFinBudgetLine line : lines) {
            result.add(line.getAcctSchema() != null ? line.getAcctSchema().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinBudgetLine.class)
    public List<String> periodCode(@ContextSource List<ErpFinBudgetLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("period"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpFinBudgetLine line : lines) {
            result.add(line.getPeriod() != null ? line.getPeriod().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinBudgetLine.class)
    public List<String> subjectName(@ContextSource List<ErpFinBudgetLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("subject"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpFinBudgetLine line : lines) {
            result.add(line.getSubject() != null ? line.getSubject().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinBudgetLine.class)
    public List<String> costCenterName(@ContextSource List<ErpFinBudgetLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("costCenter"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpFinBudgetLine line : lines) {
            result.add(line.getCostCenter() != null ? line.getCostCenter().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinBudgetLine.class)
    public List<String> departmentName(@ContextSource List<ErpFinBudgetLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("department"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpFinBudgetLine line : lines) {
            result.add(line.getDepartment() != null ? line.getDepartment().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinBudgetLine.class)
    public List<String> projectName(@ContextSource List<ErpFinBudgetLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("project"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpFinBudgetLine line : lines) {
            result.add(line.getProject() != null ? line.getProject().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinBudgetLine.class)
    public List<String> partnerName(@ContextSource List<ErpFinBudgetLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("partner"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpFinBudgetLine line : lines) {
            result.add(line.getPartner() != null ? line.getPartner().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinBudgetLine.class)
    public List<String> warehouseName(@ContextSource List<ErpFinBudgetLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("warehouse"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpFinBudgetLine line : lines) {
            result.add(line.getWarehouse() != null ? line.getWarehouse().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinBudgetLine.class)
    public List<String> materialName(@ContextSource List<ErpFinBudgetLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("material"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpFinBudgetLine line : lines) {
            result.add(line.getMaterial() != null ? line.getMaterial().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinBudgetLine.class)
    public List<String> currencyName(@ContextSource List<ErpFinBudgetLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpFinBudgetLine line : lines) {
            result.add(line.getCurrency() != null ? line.getCurrency().getName() : null);
        }
        return result;
    }
}
