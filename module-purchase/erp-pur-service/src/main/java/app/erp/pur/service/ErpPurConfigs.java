package app.erp.pur.service;

/**
 * 采购域配置项常量。权威定义见 {@code ErpPurConstants}（status/standing）与 owner docs（配置点）。
 *
 * <p>当前配置项经 {@code AppConfig.var(...)} 读取，缺失走默认，无需 .env。
 */
public interface ErpPurConfigs {

    // 评分卡：standing=RED 时是否禁止供应商参与新询价/报价（false=hold 需审批放行）
    // 详见 docs/design/purchase/supplier-evaluation.md §配置点
}
