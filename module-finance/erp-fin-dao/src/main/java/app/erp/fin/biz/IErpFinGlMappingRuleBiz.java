package app.erp.fin.biz;

import app.erp.fin.dao.entity.ErpFinGlMappingRule;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.util.List;


public interface IErpFinGlMappingRuleBiz extends ICrudBiz<ErpFinGlMappingRule> {

    /**
     * 调试查询：返回某 (businessType, accountKey) 的所有候选规则（按 priority DESC + 维度具体度 DESC 排序）。
     * 运维诊断"为什么解析到这个科目"用（plan 2026-07-21-0827-1 A1 Phase 2）。
     */
    @BizQuery
    List<ErpFinGlMappingRule> findApplicableRules(@Name("businessType") String businessType,
                                                   @Name("accountKey") String accountKey,
                                                   IServiceContext context);

    /**
     * 缓存手动刷新（list grid 工具栏按钮触发）。多节点部署下仅刷新本节点缓存。
     */
    @BizMutation
    void refreshCache(IServiceContext context);
}
