
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinGlMappingRuleBiz;
import app.erp.fin.dao.api.IErpFinGlMappingResolver;
import app.erp.fin.dao.entity.ErpFinGlMappingRule;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import jakarta.inject.Inject;

import java.util.List;


/**
 * GL 映射规则 BizModel（plan 2026-07-21-0827-1 A1）。
 *
 * <p>标准 CRUD 走 {@link CrudBizModel}；本类覆写 {@code defaultPrepareSave/Update/Delete} 钩子注册
 * post-commit 缓存失效回调（保证只在事务成功提交后失效，避免失败回滚后缓存与库不一致）。
 * 另提供 {@link #findApplicableRules}（运维调试 query）+ {@link #refreshCache}（list grid 工具栏按钮）。
 *
 * <p>权威：{@code docs/design/finance/gl-mapping-rules.md §4.2 主动失效机制}。
 */
@BizModel("ErpFinGlMappingRule")
public class ErpFinGlMappingRuleBizModel extends CrudBizModel<ErpFinGlMappingRule> implements IErpFinGlMappingRuleBiz {

    @Inject
    IErpFinGlMappingResolver glMappingResolver;

    public ErpFinGlMappingRuleBizModel() {
        setEntityName(ErpFinGlMappingRule.class.getName());
    }

    /**
     * save 钩子：注册 post-commit 缓存失效回调。
     */
    @Override
    protected void defaultPrepareSave(EntityData<ErpFinGlMappingRule> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        txn().afterCommit(null, () -> glMappingResolver.invalidateCache());
    }

    /**
     * update 钩子：注册 post-commit 缓存失效回调。
     */
    @Override
    protected void defaultPrepareUpdate(EntityData<ErpFinGlMappingRule> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        txn().afterCommit(null, () -> glMappingResolver.invalidateCache());
    }

    /**
     * delete 钩子：注册 post-commit 缓存失效回调。
     */
    @Override
    protected void defaultPrepareDelete(ErpFinGlMappingRule entity, IServiceContext context) {
        super.defaultPrepareDelete(entity, context);
        txn().afterCommit(null, () -> glMappingResolver.invalidateCache());
    }

    /**
     * 调试查询：返回某 (businessType, accountKey) 的所有候选规则（按 priority DESC 排序）。
     * 运维诊断"为什么解析到这个科目"用。
     */
    @Override
    @BizQuery
    public List<ErpFinGlMappingRule> findApplicableRules(@Name("businessType") String businessType,
                                                          @Name("accountKey") String accountKey,
                                                          IServiceContext context) {
        QueryBean query = new QueryBean();
        query.addFilter(io.nop.api.core.beans.FilterBeans.eq("businessType", businessType));
        query.addFilter(io.nop.api.core.beans.FilterBeans.eq("accountKey", accountKey));
        query.addOrderField("priority", true); // DESC
        return findList(query, null, context);
    }

    /**
     * 缓存手动刷新（list grid 工具栏按钮触发）。多节点部署下仅刷新本节点缓存。
     */
    @Override
    @BizMutation
    public void refreshCache(IServiceContext context) {
        glMappingResolver.invalidateCache();
    }
}
