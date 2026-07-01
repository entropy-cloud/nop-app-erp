package app.erp.fin.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;

import java.util.List;
import java.util.Set;

/**
 * 凭证生成 Provider（业财过账可插拔扩展点）。各业务域可实现本接口并注册为 Bean，
 * 由 {@link ErpFinAcctDocRegistry} 按 {@link ErpFinBusinessType} 路由调用。
 *
 * <p>默认实现（{@link #isFallback()} 返回 {@code true}）仅兜底未被任何域 Provider 接管的业务类型；
 * 当某业务类型既有域 Provider 又有默认 Provider 时，域 Provider 优先。
 */
public interface IErpFinAcctDocProvider {

    /**
     * 本 Provider 支持的业务类型集合。
     */
    Set<ErpFinBusinessType> getSupportedBusinessTypes();

    /**
     * 按事件与上下文生成凭证分录（内部 DTO），不直接写持久化实体。
     */
    List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx);

    /**
     * 是否为默认/兜底 Provider。默认 {@code false}；默认 Provider 返回 {@code true}。
     * 注册中心对同一业务类型：域 Provider（非默认）优先，默认 Provider 仅填充空缺 key。
     */
    default boolean isFallback() {
        return false;
    }
}
