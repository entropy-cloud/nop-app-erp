
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizAction;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import jakarta.annotation.Nullable;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.spi.IErpMdPartnerReferenceChecker;

import jakarta.inject.Inject;
import java.util.Collections;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ne;

@BizModel("ErpMdPartner")
public class ErpMdPartnerBizModel extends CrudBizModel<ErpMdPartner> implements IErpMdPartnerBiz {

    /**
     * 跨域引用计数 SPI（F7 §3）。master-data 不可反向依赖 purchase/sales/inventory，
     * 默认无实现返回空 Map（删除走原 __delete 路径）。
     */
    @Inject
    @Nullable
    protected IErpMdPartnerReferenceChecker partnerReferenceChecker;

    public ErpMdPartnerBizModel() {
        setEntityName(ErpMdPartner.class.getName());
    }

    @Override
    @BizAction
    public ErpMdPartner findById(@Name("id") Long id, IServiceContext context) {
        if (id == null) {
            return null;
        }
        // 经 get() 走数据权限 + Meta 管道（回归默认读取行为，对齐审计 D2 裁决）。
        return get(String.valueOf(id), true, context);
    }

    /**
     * F7 §3 编码唯一性前置校验。Add 模式 excludeId 传 null；Edit 模式传自身 id 排除自身。
     *
     * <p>实现说明：直接经 {@code dao().findAllByQuery(query)} 查询避免 CrudBizModel 管道的
     * objMeta filter 校验（默认仅允许 eq/in/date-between，ne 在 id 上不被允许）。
     */
    @Override
    @BizQuery
    public boolean isCodeUnique(@Name("code") String code,
                                @Optional @Name("excludeId") Long excludeId,
                                IServiceContext context) {
        if (code == null || code.isEmpty()) {
            return true;
        }
        QueryBean query = new QueryBean();
        query.addFilter(eq("code", code));
        if (excludeId != null) {
            query.addFilter(ne("id", excludeId));
        }
        return dao().findAllByQuery(query).isEmpty();
    }

    /**
     * F7 §3 删除引用预览。经 SPI 跨域解耦；默认无实现返回空 Map。
     */
    @Override
    @BizQuery
    public Map<String, Long> countReferences(@Name("id") Long id, IServiceContext context) {
        if (id == null || partnerReferenceChecker == null) {
            return Collections.emptyMap();
        }
        Map<String, Long> result = partnerReferenceChecker.countReferences(id);
        return result == null ? Collections.emptyMap() : result;
    }
}
