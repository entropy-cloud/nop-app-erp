
package app.erp.ct.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.contract.dao.entity.ErpCtContractVersion;

/**
 * 合同版本业务接口。除标准 CRUD 外，定义版本状态机契约
 * （对齐 {@code docs/design/contract/state-machine.md} §版本管理）：
 *
 * <ul>
 *   <li>{@link #finalizeVersion}：DRAFT → FINALIZED。</li>
 *   <li>{@link #signVersion}：FINALIZED → SIGNED，置 isCurrent=true 并原子翻转同级版本 isCurrent=false。</li>
 * </ul>
 *
 * <p>仅当前版本（isCurrent=true）可签署，违反抛
 * {@link io.nop.api.core.exceptions.NopException}。
 */
public interface IErpCtContractVersionBiz extends ICrudBiz<ErpCtContractVersion> {

    @BizMutation
    ErpCtContractVersion finalizeVersion(@Name("versionId") Long versionId, IServiceContext context);

    @BizMutation
    ErpCtContractVersion signVersion(@Name("versionId") Long versionId, IServiceContext context);
}
