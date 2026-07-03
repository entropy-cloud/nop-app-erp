package app.erp.crm.service.support;

import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.service.ErpCrmConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.ne;
import static io.nop.api.core.beans.FilterBeans.or;

/**
 * 线索查重：按 companyName/contactEmail/contactPhone 命中既有非终态（NEW/QUALIFIED）Lead。
 *
 * <p>对齐 {@code docs/design/crm/README.md §业务规则3}（参考 Axelor DuplicateObjectsCrmService）。
 * 配置 {@code erp-crm.auto-convert-duplicate-lead=false}（默认）时仅提示不阻断。
 */
public class LeadDuplicateChecker {

    @Inject
    IDaoProvider daoProvider;

    /**
     * 返回与指定 lead 候选键（companyName/contactEmail/contactPhone）命中的其他非终态线索。
     *
     * @param lead    待查重的线索（须已加载 companyName/contactEmail/contactPhone）
     * @param context 服务上下文
     * @return 候选重复线索列表（不含自身），无重复返回空列表
     */
    public List<ErpCrmLead> findDuplicates(ErpCrmLead lead, IServiceContext context) {
        if (lead == null) {
            return new ArrayList<>();
        }

        Set<String> keys = new HashSet<>();
        addIfPresent(keys, lead.getCompanyName());
        addIfPresent(keys, lead.getContactEmail());
        addIfPresent(keys, lead.getContactPhone());
        if (keys.isEmpty()) {
            return new ArrayList<>();
        }

        List<ErpCrmLead> nonTerminal = loadNonTerminalLeads();
        List<ErpCrmLead> duplicates = new ArrayList<>();
        for (ErpCrmLead candidate : nonTerminal) {
            if (Objects.equals(candidate.getId(), lead.getId())) {
                continue;
            }
            if (keyHits(candidate, keys)) {
                duplicates.add(candidate);
            }
        }
        return duplicates;
    }

    /**
     * 查重并按配置决定是否抛出提示异常。
     *
     * @throws NopException 当 {@code erp-crm.auto-convert-duplicate-lead=true}（非默认）且发现重复时抛
     *                      {@link app.erp.crm.service.ErpCrmErrors#ERR_DUPLICATE_LEAD_FOUND}（阻断保存）。
     *                      默认配置（false）下仅返回候选，调用方可记录日志，不阻断。
     */
    public List<ErpCrmLead> checkAndNotify(ErpCrmLead lead, IServiceContext context) {
        List<ErpCrmLead> duplicates = findDuplicates(lead, context);
        if (!duplicates.isEmpty()) {
            boolean autoMerge = io.nop.api.core.config.AppConfig.var(
                    ErpCrmConstants.CONFIG_AUTO_CONVERT_DUPLICATE_LEAD, Boolean.FALSE);
            if (autoMerge) {
                throw new NopException(app.erp.crm.service.ErpCrmErrors.ERR_DUPLICATE_LEAD_FOUND)
                        .param(app.erp.crm.service.ErpCrmErrors.ARG_DUPLICATE_COUNT, duplicates.size());
            }
        }
        return duplicates;
    }

    private boolean keyHits(ErpCrmLead candidate, Set<String> keys) {
        return keys.contains(norm(candidate.getCompanyName()))
                || keys.contains(norm(candidate.getContactEmail()))
                || keys.contains(norm(candidate.getContactPhone()));
    }

    private void addIfPresent(Set<String> keys, String value) {
        String n = norm(value);
        if (n != null) {
            keys.add(n);
        }
    }

    private String norm(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<ErpCrmLead> loadNonTerminalLeads() {
        IEntityDao<ErpCrmLead> dao = daoProvider.daoFor(ErpCrmLead.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("docStatus", Arrays.asList(
                ErpCrmConstants.DOC_STATUS_NEW, ErpCrmConstants.DOC_STATUS_QUALIFIED)));
        return new ArrayList<>(dao.findAllByQuery(q));
    }
}
