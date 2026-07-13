
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;

import app.erp.crm.biz.IErpCrmConfigRuleBiz;
import app.erp.crm.dao.entity.ErpCrmConfigRule;
import app.erp.crm.service.ErpCrmConfigs;
import app.erp.crm.service.ErpCrmErrors;

import static io.nop.api.core.beans.FilterBeans.eq;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 配置规则 BizModel（plan 2026-07-07-1430-2 §Phase 1）。
 *
 * <p>维护钩子：单配置器规则数不超 {@code erp-crm.cpq.max-rules-per-configurator} 上限。
 */
@BizModel("ErpCrmConfigRule")
public class ErpCrmConfigRuleBizModel extends CrudBizModel<ErpCrmConfigRule> implements IErpCrmConfigRuleBiz {
    public ErpCrmConfigRuleBizModel() {
        setEntityName(ErpCrmConfigRule.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpCrmConfigRule> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        enforceRuleLimit(entityData.getEntity());
    }

    @Override
    protected void defaultPrepareUpdate(EntityData<ErpCrmConfigRule> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        enforceRuleLimit(entityData.getEntity());
    }

    protected void enforceRuleLimit(ErpCrmConfigRule rule) {
        if (rule == null || rule.getConfiguratorId() == null) {
            return;
        }
        int max = ErpCrmConfigs.cpqMaxRulesPerConfigurator();
        if (max <= 0) {
            return;
        }
        IEntityDao<ErpCrmConfigRule> dao = dao();
        QueryBean q = new QueryBean();
        q.addFilter(eq("configuratorId", rule.getConfiguratorId()));
        long count = dao.countByQuery(q);
        // 新建：count 即为同配置器既有规则数；更新：count 包含自身，不超限即放行
        if (rule.getId() == null && count >= max) {
            throw new NopException(ErpCrmErrors.ERR_CPQ_RULE_LIMIT_EXCEEDED)
                    .param(ErpCrmErrors.ARG_CONFIGURATOR_ID, rule.getConfiguratorId())
                    .param(ErpCrmErrors.ARG_RULE_COUNT, count + 1)
                    .param(ErpCrmErrors.ARG_MAX_RULES, max);
        }
        if (rule.getId() != null && count > max) {
            throw new NopException(ErpCrmErrors.ERR_CPQ_RULE_LIMIT_EXCEEDED)
                    .param(ErpCrmErrors.ARG_CONFIGURATOR_ID, rule.getConfiguratorId())
                    .param(ErpCrmErrors.ARG_RULE_COUNT, count)
                    .param(ErpCrmErrors.ARG_MAX_RULES, max);
        }
    }

    
    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name/*Code 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCrmConfigRule.class)
    public List<String> configuratorName(@ContextSource List<ErpCrmConfigRule> rows) {
        orm().batchLoadProps(rows, Collections.singleton("configurator"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmConfigRule row : rows) {
            result.add(row.orm_attached() && row.getConfigurator() != null ? row.getConfigurator().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCrmConfigRule.class)
    public List<String> orgName(@ContextSource List<ErpCrmConfigRule> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmConfigRule row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

}
