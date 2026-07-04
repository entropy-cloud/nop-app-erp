
package app.erp.ct.biz;

import app.erp.contract.dao.entity.ErpCtSignatureRequest;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.time.LocalDate;
import java.util.List;

/**
 * 电子签章请求业务接口（plan 2026-07-04-2200-2，design e-signature.md）。
 *
 * <p>除标准 CRUD 外，定义签章生命周期契约：
 * <ul>
 *   <li>{@link #initSignatureRequest}：FINALIZED 版本→建 SignatureRequest(PENDING_SIGNATURE) + 调
 *       {@code IErpCtSignatureProvider.initSignature} 回填 providerRequestId。</li>
 *   <li>{@link #handleSignatureCallback}：webhook 回调入口（@BizMutation，HMAC + 幂等 + 按 event 推进状态机）。</li>
 *   <li>{@link #queryAndUpdateStatus}：主动轮询 Provider.queryStatus 推进状态机（与 callback 共用迁移逻辑）。</li>
 *   <li>{@link #cancelSignatureRequest} / {@link #rejectSignature}：终态迁移。</li>
 *   <li>{@link #findExpiringRequests}：到期查询（cron 注册归 Non-Goal）。</li>
 * </ul>
 *
 * <p>状态机权威定义见 {@code docs/design/contract/e-signature.md §ErpCtSignatureRequest 状态机}。
 */
public interface IErpCtSignatureRequestBiz extends ICrudBiz<ErpCtSignatureRequest> {

    @BizMutation
    ErpCtSignatureRequest initSignatureRequest(@Name("contractVersionId") Long contractVersionId,
                                               @Name("signers") String signersJson,
                                               @Name("providerCode") String providerCode,
                                               IServiceContext context);

    @BizMutation
    ErpCtSignatureRequest handleSignatureCallback(@Name("providerCode") String providerCode,
                                                  @Name("signature") String signature,
                                                  @Name("eventId") String eventId,
                                                  @Name("payload") String payload,
                                                  IServiceContext context);

    @BizMutation
    ErpCtSignatureRequest queryAndUpdateStatus(@Name("requestId") Long requestId,
                                               IServiceContext context);

    @BizMutation
    ErpCtSignatureRequest cancelSignatureRequest(@Name("requestId") Long requestId,
                                                 IServiceContext context);

    @BizMutation
    ErpCtSignatureRequest rejectSignature(@Name("requestId") Long requestId,
                                          @Name("reason") String reason,
                                          IServiceContext context);

    @BizQuery
    List<ErpCtSignatureRequest> findExpiringRequests(@Name("asOfDate") LocalDate asOfDate,
                                                     IServiceContext context);
}
