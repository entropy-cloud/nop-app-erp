package app.erp.md.spi;

import java.util.Map;

/**
 * 往来单位业务单据引用计数 SPI（F7 §3 主数据删除引用预览入口）。
 *
 * <p>master-data 是基础域，不得反向依赖 purchase/sales/inventory（依赖环约束，见
 * {@code docs/architecture/domain-module-split-analysis.md}）。往来单位被业务单据引用的计数经本 SPI 解耦：
 * master-data 仅声明端口，下游域（purchase/sales/inventory 等）各自实现并注册
 * （{@link jakarta.inject.Inject}{@code (required=false)} 注入）。
 *
 * <p>默认无实现时返回空 Map（删除直接走原 __delete 路径，UX 与未实现前一致）。
 * 下游接线归 Deferred（见计划 Follow-up）。
 */
public interface IErpMdPartnerReferenceChecker {

    /**
     * 统计指定往来单位被各业务单据引用的行数。
     *
     * @param partnerId 往来单位 ID
     * @return key=引用域名（如 {@code purchaseOrder}/{@code salesOrder}/{@code stockMove} 等），
     *         value=引用行数。无引用或无实现返回空 Map。
     */
    Map<String, Long> countReferences(Long partnerId);
}
