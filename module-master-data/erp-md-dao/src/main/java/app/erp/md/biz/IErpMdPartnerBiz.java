
package app.erp.md.biz;

import io.nop.api.core.annotations.biz.BizAction;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.md.dao.entity.ErpMdPartner;

import java.util.Map;

public interface IErpMdPartnerBiz extends ICrudBiz<ErpMdPartner> {

    /**
     * 按主键加载往来单位，不存在返回 null。供业务域做供应商/客户启用校验等只读跨域访问
     * （经 I*Biz 管道，对齐 data-dependency-matrix §3 / service-layer 跨实体访问规则）。
     */
    @BizAction
    ErpMdPartner findById(@Name("id") Long id, IServiceContext context);

    /**
     * F7 §3 编码唯一性前置校验（async validator on blur 调用入口）。
     *
     * @param code       待校验编码
     * @param excludeId  edit 模式排除自身 ID（add 模式传 null）
     * @return true 表示编码可用（无冲突）；false 表示已被其他记录占用
     */
    @BizQuery
    boolean isCodeUnique(@Name("code") String code,
                         @Optional @Name("excludeId") Long excludeId,
                         IServiceContext context);

    /**
     * F7 §3 删除引用预览（row-delete-button 点击时调用入口）。
     *
     * <p>跨域引用经 SPI（{@code IErpMdPartnerReferenceChecker}）解耦：默认无实现返回空 Map（删除走原 __delete 路径）。
     *
     * @param id 往来单位 ID
     * @return key=引用域名，value=引用行数。无引用或无 SPI 实现返回空 Map
     */
    @BizQuery
    Map<String, Long> countReferences(@Name("id") Long id, IServiceContext context);
}
