package app.erp.b2b.service.codemapping;

import app.erp.b2b.biz.IErpB2bCodeMappingBiz;
import app.erp.b2b.dao.entity.ErpB2bCodeMapping;
import app.erp.b2b.service.ErpB2bConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import io.nop.dao.api.IDaoProvider;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 代码映射解析器。出站方向 internal→external / 入站方向 external→internal，
 * 按 partnerId + mappingType + code 查 {@link ErpB2bCodeMapping}。
 *
 * <p><b>未找到处理</b>：返回原值 + WARN 日志（不阻断业务流转，{@code blockingLevel=WARN} 语义）。
 * 对应 {@code edi-formats.md §6.2}。
 *
 * <p>使用 {@link IDaoProvider} 直接查询（跨实体只读，不经 I*Biz 权限管道——代码映射为系统级查表，
 * 无需用户上下文过滤）。参 logistics {@code GatewayDispatcher.findCarrierByCode} 的 IDaoProvider 范式。
 */
public class CodeMappingResolver {

    private static final Logger LOG = LoggerFactory.getLogger(CodeMappingResolver.class);

    @Inject
    IDaoProvider daoProvider;

    /**
     * 出站解析：内部代码 → 外部代码。
     *
     * @param partnerId    伙伴 ID
     * @param mappingType  映射类型（MATERIAL / PARTNER / UOM）
     * @param internalCode 内部代码
     * @return 外部代码（未找到返回原值 + WARN）
     */
    public String resolveOutbound(Long partnerId, String mappingType, String internalCode) {
        if (internalCode == null) {
            return null;
        }
        ErpB2bCodeMapping mapping = findMapping(partnerId, mappingType, internalCode, true);
        if (mapping != null) {
            return mapping.getExternalCode();
        }
        LOG.warn("出站代码映射未找到：partnerId={} type={} internalCode={}（保留原值）", partnerId, mappingType, internalCode);
        return internalCode;
    }

    /**
     * 入站解析：外部代码 → 内部代码。
     *
     * @param partnerId    伙伴 ID
     * @param mappingType  映射类型（MATERIAL / PARTNER / UOM）
     * @param externalCode 外部代码
     * @return 内部代码（未找到返回原值 + WARN）
     */
    public String resolveInbound(Long partnerId, String mappingType, String externalCode) {
        if (externalCode == null) {
            return null;
        }
        ErpB2bCodeMapping mapping = findMapping(partnerId, mappingType, externalCode, false);
        if (mapping != null) {
            return mapping.getInternalCode();
        }
        LOG.warn("入站代码映射未找到：partnerId={} type={} externalCode={}（保留原值）", partnerId, mappingType, externalCode);
        return externalCode;
    }

    private ErpB2bCodeMapping findMapping(Long partnerId, String mappingType, String code, boolean byInternal) {
        IEntityDao<ErpB2bCodeMapping> dao = daoProvider.daoFor(ErpB2bCodeMapping.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("mappingType", mappingType));
        q.addFilter(eq(byInternal ? "internalCode" : "externalCode", code));
        if (partnerId != null) {
            q.addFilter(eq("partnerId", partnerId));
        }
        return dao.findFirstByQuery(q);
    }
}
