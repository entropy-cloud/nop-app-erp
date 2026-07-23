package app.erp.qa.dao.constants;

/**
 * 质量域检验类型常量（dao 层）。承载跨域消费者（如 manufacturing 完工检验触发）所需的稳定码值，
 * 避免跨域直接引用生成产物 {@code _ErpQaDaoConstants}。
 *
 * <p>权威值来自 {@code module-quality/model/app-erp-quality.orm.xml} 关联字典 {@code erp-qa/inspection-type}。
 * 本接口为 dao 层非生成引用接口（D1 先例 {@code ErpQaDocStatus} 同型产物）；
 * service 层的 {@code app.erp.qa.service.ErpQaConstants} 在自身接口内重复声明了相同码值（service 层语义集合更广，
 * 包含 NCR/SPC/配置项等 dao 层不应承载的常量，故未 {@code extends} 本接口，保持各层语义内聚）。
 *
 * <p>裁决（plan 2026-07-24-1400-2 §Phase 1 Decision，选 B 新建专用接口）：
 * 检验类型（inspection-type）与单据状态（approve-status / doc-status）属不同语义轴，
 * 混入 {@code ErpQaDocStatus} 会破坏其语义内聚。新建专用接口使 qa 域 dao 层常量接口 +1，
 * 与 D1 模式（每语义轴一接口）一致，可接受。
 */
public interface ErpQaInspectionType {

    // 检验类型（erp-qa/inspection-type）
    String INSPECTION_TYPE_INCOMING = "INCOMING";
    String INSPECTION_TYPE_IN_PROCESS = "IN_PROCESS";
    String INSPECTION_TYPE_FINAL = "FINAL";
    String INSPECTION_TYPE_OUTGOING = "OUTGOING";
}
