
package app.erp.cs.biz;

import app.erp.cs.dao.entity.ErpCsTicket;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.cs.dao.entity.ErpCsServiceCatalogItem;

public interface IErpCsServiceCatalogItemBiz extends ICrudBiz<ErpCsServiceCatalogItem>{

    /**
     * 目录项驱动建单（service-catalog.md §2.1/§2.2）。按 catalogItem 的 ticketTypeId/slaPolicyId
     * 自动填充建 {@link ErpCsTicket}，写 catalogItemId，requestFormConfig JSON 字段映射到工单扩展属性，
     * 联动权益匹配（同客户）。返回新建的工单。
     *
     * @param catalogItemId 目录项 ID
     * @param formData      表单数据（subject/description/productId/orderNumber/urgency 等）
     */
    @BizMutation
    ErpCsTicket createFromCatalog(@Name("catalogItemId") Long catalogItemId,
                                  @Optional @Name("formData") java.util.Map<String, Object> formData,
                                  IServiceContext context);
}
