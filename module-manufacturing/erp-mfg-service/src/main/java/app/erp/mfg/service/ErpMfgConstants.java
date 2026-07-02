package app.erp.mfg.service;

/**
 * 制造域常量。字典码值权威：`erp-mfg-meta/.../dict/*.dict.yaml` + `module-manufacturing/model/*.orm.xml`。
 *
 * <p>权威：`docs/design/manufacturing/bom-and-routing.md`、`docs/plans/2026-07-02-1538-2-manufacturing-bom-routing-rollup.md`。
 */
public interface ErpMfgConstants {

    // BOM 类型（erp-mfg/bom-type）
    int BOM_TYPE_MANUFACTURED = 10;
    int BOM_TYPE_PHANTOM = 20;

    // 成本滚算状态（erp-mfg/cost-rollup-status）
    int COST_ROLLUP_STATUS_DRAFT = 10;
    int COST_ROLLUP_STATUS_CALCULATED = 20;
    int COST_ROLLUP_STATUS_FIRMED = 30;

    // 多级 BOM 展开深度上限 / 环兜底（`erp-mfg.bom-max-depth`）
    String CONFIG_BOM_MAX_DEPTH = "erp-mfg.bom-max-depth";
    int DEFAULT_BOM_MAX_DEPTH = 15;
}
