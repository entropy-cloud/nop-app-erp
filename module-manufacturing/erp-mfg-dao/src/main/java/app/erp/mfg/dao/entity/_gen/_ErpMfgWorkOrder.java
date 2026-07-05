package app.erp.mfg.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.mfg.dao.entity.ErpMfgWorkOrder;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  工单: erp_mfg_work_order
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpMfgWorkOrder extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 工单号: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* BOM: BOM_ID BIGINT */
    public static final String PROP_NAME_bomId = "bomId";
    public static final int PROP_ID_bomId = 4;
    
    /* 工艺路线: ROUTING_ID BIGINT */
    public static final String PROP_NAME_routingId = "routingId";
    public static final int PROP_ID_routingId = 5;
    
    /* 生产版本: PRODUCTION_VERSION_ID BIGINT */
    public static final String PROP_NAME_productionVersionId = "productionVersionId";
    public static final int PROP_ID_productionVersionId = 6;
    
    /* 来源 MRP 计划: SOURCE_MRP_PLAN_ID BIGINT */
    public static final String PROP_NAME_sourceMrpPlanId = "sourceMrpPlanId";
    public static final int PROP_ID_sourceMrpPlanId = 7;
    
    /* 来源单据类型: SOURCE_ORDER_TYPE VARCHAR */
    public static final String PROP_NAME_sourceOrderType = "sourceOrderType";
    public static final int PROP_ID_sourceOrderType = 8;
    
    /* 来源单据号: SOURCE_ORDER_CODE VARCHAR */
    public static final String PROP_NAME_sourceOrderCode = "sourceOrderCode";
    public static final int PROP_ID_sourceOrderCode = 9;
    
    /* 产品(主产出): PRODUCT_ID BIGINT */
    public static final String PROP_NAME_productId = "productId";
    public static final int PROP_ID_productId = 10;
    
    /* 计划数量: PLANNED_QUANTITY DECIMAL */
    public static final String PROP_NAME_plannedQuantity = "plannedQuantity";
    public static final int PROP_ID_plannedQuantity = 11;
    
    /* 完工数量: COMPLETED_QUANTITY DECIMAL */
    public static final String PROP_NAME_completedQuantity = "completedQuantity";
    public static final int PROP_ID_completedQuantity = 12;
    
    /* 报废数量: SCRAPPED_QUANTITY DECIMAL */
    public static final String PROP_NAME_scrappedQuantity = "scrappedQuantity";
    public static final int PROP_ID_scrappedQuantity = 13;
    
    /* 工单日期: BUSINESS_DATE DATE */
    public static final String PROP_NAME_businessDate = "businessDate";
    public static final int PROP_ID_businessDate = 14;
    
    /* 计划开工日期: PLANNED_START_DATE DATE */
    public static final String PROP_NAME_plannedStartDate = "plannedStartDate";
    public static final int PROP_ID_plannedStartDate = 15;
    
    /* 计划完工日期: PLANNED_END_DATE DATE */
    public static final String PROP_NAME_plannedEndDate = "plannedEndDate";
    public static final int PROP_ID_plannedEndDate = 16;
    
    /* 实际开工日期: ACTUAL_START_DATE DATE */
    public static final String PROP_NAME_actualStartDate = "actualStartDate";
    public static final int PROP_ID_actualStartDate = 17;
    
    /* 实际完工日期: ACTUAL_END_DATE DATE */
    public static final String PROP_NAME_actualEndDate = "actualEndDate";
    public static final int PROP_ID_actualEndDate = 18;
    
    /* 币种: CURRENCY_ID BIGINT */
    public static final String PROP_NAME_currencyId = "currencyId";
    public static final int PROP_ID_currencyId = 19;
    
    /* 材料成本: MATERIAL_COST DECIMAL */
    public static final String PROP_NAME_materialCost = "materialCost";
    public static final int PROP_ID_materialCost = 20;
    
    /* 人工成本: LABOR_COST DECIMAL */
    public static final String PROP_NAME_laborCost = "laborCost";
    public static final int PROP_ID_laborCost = 21;
    
    /* 制造费用: OVERHEAD_COST DECIMAL */
    public static final String PROP_NAME_overheadCost = "overheadCost";
    public static final int PROP_ID_overheadCost = 22;
    
    /* 委外成本: SUBCONTRACT_COST DECIMAL */
    public static final String PROP_NAME_subcontractCost = "subcontractCost";
    public static final int PROP_ID_subcontractCost = 23;
    
    /* 总成本: TOTAL_COST DECIMAL */
    public static final String PROP_NAME_totalCost = "totalCost";
    public static final int PROP_ID_totalCost = 24;
    
    /* 单位成本: UNIT_COST DECIMAL */
    public static final String PROP_NAME_unitCost = "unitCost";
    public static final int PROP_ID_unitCost = 25;
    
    /* 单据状态: DOC_STATUS VARCHAR */
    public static final String PROP_NAME_docStatus = "docStatus";
    public static final int PROP_ID_docStatus = 26;
    
    /* 审核状态: APPROVE_STATUS VARCHAR */
    public static final String PROP_NAME_approveStatus = "approveStatus";
    public static final int PROP_ID_approveStatus = 27;
    
    /* 优先级: PRIORITY VARCHAR */
    public static final String PROP_NAME_priority = "priority";
    public static final int PROP_ID_priority = 28;
    
    /* 已过账: POSTED BOOLEAN */
    public static final String PROP_NAME_posted = "posted";
    public static final int PROP_ID_posted = 29;
    
    /* 过账时间: POSTED_AT DATETIME */
    public static final String PROP_NAME_postedAt = "postedAt";
    public static final int PROP_ID_postedAt = 30;
    
    /* 过账人: POSTED_BY VARCHAR */
    public static final String PROP_NAME_postedBy = "postedBy";
    public static final int PROP_ID_postedBy = 31;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 32;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 33;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 34;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 35;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 36;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 37;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 38;
    
    /* APS排程来源(弱参照): SOURCE_SCHEDULE_ID BIGINT */
    public static final String PROP_NAME_sourceScheduleId = "sourceScheduleId";
    public static final int PROP_ID_sourceScheduleId = 39;
    
    /* 汇率: EXCHANGE_RATE DECIMAL */
    public static final String PROP_NAME_exchangeRate = "exchangeRate";
    public static final int PROP_ID_exchangeRate = 40;
    
    /* 源币种金额: AMOUNT_SOURCE DECIMAL */
    public static final String PROP_NAME_amountSource = "amountSource";
    public static final int PROP_ID_amountSource = 41;
    
    /* 本位币金额: AMOUNT_FUNCTIONAL DECIMAL */
    public static final String PROP_NAME_amountFunctional = "amountFunctional";
    public static final int PROP_ID_amountFunctional = 42;
    

    private static int _PROP_ID_BOUND = 43;

    
    /* relation:  */
    public static final String PROP_NAME_bom = "bom";
    
    /* relation:  */
    public static final String PROP_NAME_routing = "routing";
    
    /* relation:  */
    public static final String PROP_NAME_productionVersion = "productionVersion";
    
    /* relation:  */
    public static final String PROP_NAME_product = "product";
    
    /* relation:  */
    public static final String PROP_NAME_currency = "currency";
    
    /* relation:  */
    public static final String PROP_NAME_lines = "lines";
    
    /* relation:  */
    public static final String PROP_NAME_jobCards = "jobCards";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    
    /* relation:  */
    public static final String PROP_NAME_sourceMrpPlan = "sourceMrpPlan";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[43];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_bomId] = PROP_NAME_bomId;
          PROP_NAME_TO_ID.put(PROP_NAME_bomId, PROP_ID_bomId);
      
          PROP_ID_TO_NAME[PROP_ID_routingId] = PROP_NAME_routingId;
          PROP_NAME_TO_ID.put(PROP_NAME_routingId, PROP_ID_routingId);
      
          PROP_ID_TO_NAME[PROP_ID_productionVersionId] = PROP_NAME_productionVersionId;
          PROP_NAME_TO_ID.put(PROP_NAME_productionVersionId, PROP_ID_productionVersionId);
      
          PROP_ID_TO_NAME[PROP_ID_sourceMrpPlanId] = PROP_NAME_sourceMrpPlanId;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceMrpPlanId, PROP_ID_sourceMrpPlanId);
      
          PROP_ID_TO_NAME[PROP_ID_sourceOrderType] = PROP_NAME_sourceOrderType;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceOrderType, PROP_ID_sourceOrderType);
      
          PROP_ID_TO_NAME[PROP_ID_sourceOrderCode] = PROP_NAME_sourceOrderCode;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceOrderCode, PROP_ID_sourceOrderCode);
      
          PROP_ID_TO_NAME[PROP_ID_productId] = PROP_NAME_productId;
          PROP_NAME_TO_ID.put(PROP_NAME_productId, PROP_ID_productId);
      
          PROP_ID_TO_NAME[PROP_ID_plannedQuantity] = PROP_NAME_plannedQuantity;
          PROP_NAME_TO_ID.put(PROP_NAME_plannedQuantity, PROP_ID_plannedQuantity);
      
          PROP_ID_TO_NAME[PROP_ID_completedQuantity] = PROP_NAME_completedQuantity;
          PROP_NAME_TO_ID.put(PROP_NAME_completedQuantity, PROP_ID_completedQuantity);
      
          PROP_ID_TO_NAME[PROP_ID_scrappedQuantity] = PROP_NAME_scrappedQuantity;
          PROP_NAME_TO_ID.put(PROP_NAME_scrappedQuantity, PROP_ID_scrappedQuantity);
      
          PROP_ID_TO_NAME[PROP_ID_businessDate] = PROP_NAME_businessDate;
          PROP_NAME_TO_ID.put(PROP_NAME_businessDate, PROP_ID_businessDate);
      
          PROP_ID_TO_NAME[PROP_ID_plannedStartDate] = PROP_NAME_plannedStartDate;
          PROP_NAME_TO_ID.put(PROP_NAME_plannedStartDate, PROP_ID_plannedStartDate);
      
          PROP_ID_TO_NAME[PROP_ID_plannedEndDate] = PROP_NAME_plannedEndDate;
          PROP_NAME_TO_ID.put(PROP_NAME_plannedEndDate, PROP_ID_plannedEndDate);
      
          PROP_ID_TO_NAME[PROP_ID_actualStartDate] = PROP_NAME_actualStartDate;
          PROP_NAME_TO_ID.put(PROP_NAME_actualStartDate, PROP_ID_actualStartDate);
      
          PROP_ID_TO_NAME[PROP_ID_actualEndDate] = PROP_NAME_actualEndDate;
          PROP_NAME_TO_ID.put(PROP_NAME_actualEndDate, PROP_ID_actualEndDate);
      
          PROP_ID_TO_NAME[PROP_ID_currencyId] = PROP_NAME_currencyId;
          PROP_NAME_TO_ID.put(PROP_NAME_currencyId, PROP_ID_currencyId);
      
          PROP_ID_TO_NAME[PROP_ID_materialCost] = PROP_NAME_materialCost;
          PROP_NAME_TO_ID.put(PROP_NAME_materialCost, PROP_ID_materialCost);
      
          PROP_ID_TO_NAME[PROP_ID_laborCost] = PROP_NAME_laborCost;
          PROP_NAME_TO_ID.put(PROP_NAME_laborCost, PROP_ID_laborCost);
      
          PROP_ID_TO_NAME[PROP_ID_overheadCost] = PROP_NAME_overheadCost;
          PROP_NAME_TO_ID.put(PROP_NAME_overheadCost, PROP_ID_overheadCost);
      
          PROP_ID_TO_NAME[PROP_ID_subcontractCost] = PROP_NAME_subcontractCost;
          PROP_NAME_TO_ID.put(PROP_NAME_subcontractCost, PROP_ID_subcontractCost);
      
          PROP_ID_TO_NAME[PROP_ID_totalCost] = PROP_NAME_totalCost;
          PROP_NAME_TO_ID.put(PROP_NAME_totalCost, PROP_ID_totalCost);
      
          PROP_ID_TO_NAME[PROP_ID_unitCost] = PROP_NAME_unitCost;
          PROP_NAME_TO_ID.put(PROP_NAME_unitCost, PROP_ID_unitCost);
      
          PROP_ID_TO_NAME[PROP_ID_docStatus] = PROP_NAME_docStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_docStatus, PROP_ID_docStatus);
      
          PROP_ID_TO_NAME[PROP_ID_approveStatus] = PROP_NAME_approveStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_approveStatus, PROP_ID_approveStatus);
      
          PROP_ID_TO_NAME[PROP_ID_priority] = PROP_NAME_priority;
          PROP_NAME_TO_ID.put(PROP_NAME_priority, PROP_ID_priority);
      
          PROP_ID_TO_NAME[PROP_ID_posted] = PROP_NAME_posted;
          PROP_NAME_TO_ID.put(PROP_NAME_posted, PROP_ID_posted);
      
          PROP_ID_TO_NAME[PROP_ID_postedAt] = PROP_NAME_postedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_postedAt, PROP_ID_postedAt);
      
          PROP_ID_TO_NAME[PROP_ID_postedBy] = PROP_NAME_postedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_postedBy, PROP_ID_postedBy);
      
          PROP_ID_TO_NAME[PROP_ID_remark] = PROP_NAME_remark;
          PROP_NAME_TO_ID.put(PROP_NAME_remark, PROP_ID_remark);
      
          PROP_ID_TO_NAME[PROP_ID_delVersion] = PROP_NAME_delVersion;
          PROP_NAME_TO_ID.put(PROP_NAME_delVersion, PROP_ID_delVersion);
      
          PROP_ID_TO_NAME[PROP_ID_version] = PROP_NAME_version;
          PROP_NAME_TO_ID.put(PROP_NAME_version, PROP_ID_version);
      
          PROP_ID_TO_NAME[PROP_ID_createdBy] = PROP_NAME_createdBy;
          PROP_NAME_TO_ID.put(PROP_NAME_createdBy, PROP_ID_createdBy);
      
          PROP_ID_TO_NAME[PROP_ID_createTime] = PROP_NAME_createTime;
          PROP_NAME_TO_ID.put(PROP_NAME_createTime, PROP_ID_createTime);
      
          PROP_ID_TO_NAME[PROP_ID_updatedBy] = PROP_NAME_updatedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_updatedBy, PROP_ID_updatedBy);
      
          PROP_ID_TO_NAME[PROP_ID_updateTime] = PROP_NAME_updateTime;
          PROP_NAME_TO_ID.put(PROP_NAME_updateTime, PROP_ID_updateTime);
      
          PROP_ID_TO_NAME[PROP_ID_sourceScheduleId] = PROP_NAME_sourceScheduleId;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceScheduleId, PROP_ID_sourceScheduleId);
      
          PROP_ID_TO_NAME[PROP_ID_exchangeRate] = PROP_NAME_exchangeRate;
          PROP_NAME_TO_ID.put(PROP_NAME_exchangeRate, PROP_ID_exchangeRate);
      
          PROP_ID_TO_NAME[PROP_ID_amountSource] = PROP_NAME_amountSource;
          PROP_NAME_TO_ID.put(PROP_NAME_amountSource, PROP_ID_amountSource);
      
          PROP_ID_TO_NAME[PROP_ID_amountFunctional] = PROP_NAME_amountFunctional;
          PROP_NAME_TO_ID.put(PROP_NAME_amountFunctional, PROP_ID_amountFunctional);
      
    }

    
    /* ID: ID */
    private java.lang.Long _id;
    
    /* 工单号: CODE */
    private java.lang.String _code;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* BOM: BOM_ID */
    private java.lang.Long _bomId;
    
    /* 工艺路线: ROUTING_ID */
    private java.lang.Long _routingId;
    
    /* 生产版本: PRODUCTION_VERSION_ID */
    private java.lang.Long _productionVersionId;
    
    /* 来源 MRP 计划: SOURCE_MRP_PLAN_ID */
    private java.lang.Long _sourceMrpPlanId;
    
    /* 来源单据类型: SOURCE_ORDER_TYPE */
    private java.lang.String _sourceOrderType;
    
    /* 来源单据号: SOURCE_ORDER_CODE */
    private java.lang.String _sourceOrderCode;
    
    /* 产品(主产出): PRODUCT_ID */
    private java.lang.Long _productId;
    
    /* 计划数量: PLANNED_QUANTITY */
    private java.math.BigDecimal _plannedQuantity;
    
    /* 完工数量: COMPLETED_QUANTITY */
    private java.math.BigDecimal _completedQuantity;
    
    /* 报废数量: SCRAPPED_QUANTITY */
    private java.math.BigDecimal _scrappedQuantity;
    
    /* 工单日期: BUSINESS_DATE */
    private java.time.LocalDate _businessDate;
    
    /* 计划开工日期: PLANNED_START_DATE */
    private java.time.LocalDate _plannedStartDate;
    
    /* 计划完工日期: PLANNED_END_DATE */
    private java.time.LocalDate _plannedEndDate;
    
    /* 实际开工日期: ACTUAL_START_DATE */
    private java.time.LocalDate _actualStartDate;
    
    /* 实际完工日期: ACTUAL_END_DATE */
    private java.time.LocalDate _actualEndDate;
    
    /* 币种: CURRENCY_ID */
    private java.lang.Long _currencyId;
    
    /* 材料成本: MATERIAL_COST */
    private java.math.BigDecimal _materialCost;
    
    /* 人工成本: LABOR_COST */
    private java.math.BigDecimal _laborCost;
    
    /* 制造费用: OVERHEAD_COST */
    private java.math.BigDecimal _overheadCost;
    
    /* 委外成本: SUBCONTRACT_COST */
    private java.math.BigDecimal _subcontractCost;
    
    /* 总成本: TOTAL_COST */
    private java.math.BigDecimal _totalCost;
    
    /* 单位成本: UNIT_COST */
    private java.math.BigDecimal _unitCost;
    
    /* 单据状态: DOC_STATUS */
    private java.lang.String _docStatus;
    
    /* 审核状态: APPROVE_STATUS */
    private java.lang.String _approveStatus;
    
    /* 优先级: PRIORITY */
    private java.lang.String _priority;
    
    /* 已过账: POSTED */
    private java.lang.Boolean _posted;
    
    /* 过账时间: POSTED_AT */
    private java.time.LocalDateTime _postedAt;
    
    /* 过账人: POSTED_BY */
    private java.lang.String _postedBy;
    
    /* 备注: REMARK */
    private java.lang.String _remark;
    
    /* 逻辑删除版本: DEL_VERSION */
    private java.lang.Long _delVersion;
    
    /* 数据版本: VERSION */
    private java.lang.Integer _version;
    
    /* 创建人: CREATED_BY */
    private java.lang.String _createdBy;
    
    /* 创建时间: CREATE_TIME */
    private java.sql.Timestamp _createTime;
    
    /* 修改人: UPDATED_BY */
    private java.lang.String _updatedBy;
    
    /* 修改时间: UPDATE_TIME */
    private java.sql.Timestamp _updateTime;
    
    /* APS排程来源(弱参照): SOURCE_SCHEDULE_ID */
    private java.lang.Long _sourceScheduleId;
    
    /* 汇率: EXCHANGE_RATE */
    private java.lang.String _exchangeRate;
    
    /* 源币种金额: AMOUNT_SOURCE */
    private java.lang.String _amountSource;
    
    /* 本位币金额: AMOUNT_FUNCTIONAL */
    private java.lang.String _amountFunctional;
    

    public _ErpMfgWorkOrder(){
        // for debug
    }

    protected ErpMfgWorkOrder newInstance(){
        ErpMfgWorkOrder entity = new ErpMfgWorkOrder();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpMfgWorkOrder cloneInstance() {
        ErpMfgWorkOrder entity = newInstance();
        orm_forEachInitedProp((value, propId) -> {
            entity.orm_propValue(propId,value);
        });
        return entity;
    }

    @Override
    public String orm_entityName() {
      // 如果存在实体模型对象，则以模型对象上的设置为准
      IEntityModel entityModel = orm_entityModel();
      if(entityModel != null)
          return entityModel.getName();
      return "app.erp.mfg.dao.entity.ErpMfgWorkOrder";
    }

    @Override
    public int orm_propIdBound(){
      IEntityModel entityModel = orm_entityModel();
      if(entityModel != null)
          return entityModel.getPropIdBound();
      return _PROP_ID_BOUND;
    }

    @Override
    public Object orm_id() {
    
        return buildSimpleId(PROP_ID_id);
     
    }

    @Override
    public boolean orm_isPrimary(int propId) {
        
            return propId == PROP_ID_id;
          
    }

    @Override
    public String orm_propName(int propId) {
        if(propId >= PROP_ID_TO_NAME.length)
            return super.orm_propName(propId);
        String propName = PROP_ID_TO_NAME[propId];
        if(propName == null)
           return super.orm_propName(propId);
        return propName;
    }

    @Override
    public int orm_propId(String propName) {
        Integer propId = PROP_NAME_TO_ID.get(propName);
        if(propId == null)
            return super.orm_propId(propName);
        return propId;
    }

    @Override
    public Object orm_propValue(int propId) {
        switch(propId){
        
            case PROP_ID_id:
               return getId();
        
            case PROP_ID_code:
               return getCode();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_bomId:
               return getBomId();
        
            case PROP_ID_routingId:
               return getRoutingId();
        
            case PROP_ID_productionVersionId:
               return getProductionVersionId();
        
            case PROP_ID_sourceMrpPlanId:
               return getSourceMrpPlanId();
        
            case PROP_ID_sourceOrderType:
               return getSourceOrderType();
        
            case PROP_ID_sourceOrderCode:
               return getSourceOrderCode();
        
            case PROP_ID_productId:
               return getProductId();
        
            case PROP_ID_plannedQuantity:
               return getPlannedQuantity();
        
            case PROP_ID_completedQuantity:
               return getCompletedQuantity();
        
            case PROP_ID_scrappedQuantity:
               return getScrappedQuantity();
        
            case PROP_ID_businessDate:
               return getBusinessDate();
        
            case PROP_ID_plannedStartDate:
               return getPlannedStartDate();
        
            case PROP_ID_plannedEndDate:
               return getPlannedEndDate();
        
            case PROP_ID_actualStartDate:
               return getActualStartDate();
        
            case PROP_ID_actualEndDate:
               return getActualEndDate();
        
            case PROP_ID_currencyId:
               return getCurrencyId();
        
            case PROP_ID_materialCost:
               return getMaterialCost();
        
            case PROP_ID_laborCost:
               return getLaborCost();
        
            case PROP_ID_overheadCost:
               return getOverheadCost();
        
            case PROP_ID_subcontractCost:
               return getSubcontractCost();
        
            case PROP_ID_totalCost:
               return getTotalCost();
        
            case PROP_ID_unitCost:
               return getUnitCost();
        
            case PROP_ID_docStatus:
               return getDocStatus();
        
            case PROP_ID_approveStatus:
               return getApproveStatus();
        
            case PROP_ID_priority:
               return getPriority();
        
            case PROP_ID_posted:
               return getPosted();
        
            case PROP_ID_postedAt:
               return getPostedAt();
        
            case PROP_ID_postedBy:
               return getPostedBy();
        
            case PROP_ID_remark:
               return getRemark();
        
            case PROP_ID_delVersion:
               return getDelVersion();
        
            case PROP_ID_version:
               return getVersion();
        
            case PROP_ID_createdBy:
               return getCreatedBy();
        
            case PROP_ID_createTime:
               return getCreateTime();
        
            case PROP_ID_updatedBy:
               return getUpdatedBy();
        
            case PROP_ID_updateTime:
               return getUpdateTime();
        
            case PROP_ID_sourceScheduleId:
               return getSourceScheduleId();
        
            case PROP_ID_exchangeRate:
               return getExchangeRate();
        
            case PROP_ID_amountSource:
               return getAmountSource();
        
            case PROP_ID_amountFunctional:
               return getAmountFunctional();
        
           default:
              return super.orm_propValue(propId);
        }
    }

    

    @Override
    public void orm_propValue(int propId, Object value){
        switch(propId){
        
            case PROP_ID_id:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_id));
               }
               setId(typedValue);
               break;
            }
        
            case PROP_ID_code:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_code));
               }
               setCode(typedValue);
               break;
            }
        
            case PROP_ID_orgId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_orgId));
               }
               setOrgId(typedValue);
               break;
            }
        
            case PROP_ID_bomId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_bomId));
               }
               setBomId(typedValue);
               break;
            }
        
            case PROP_ID_routingId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_routingId));
               }
               setRoutingId(typedValue);
               break;
            }
        
            case PROP_ID_productionVersionId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_productionVersionId));
               }
               setProductionVersionId(typedValue);
               break;
            }
        
            case PROP_ID_sourceMrpPlanId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_sourceMrpPlanId));
               }
               setSourceMrpPlanId(typedValue);
               break;
            }
        
            case PROP_ID_sourceOrderType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_sourceOrderType));
               }
               setSourceOrderType(typedValue);
               break;
            }
        
            case PROP_ID_sourceOrderCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_sourceOrderCode));
               }
               setSourceOrderCode(typedValue);
               break;
            }
        
            case PROP_ID_productId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_productId));
               }
               setProductId(typedValue);
               break;
            }
        
            case PROP_ID_plannedQuantity:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_plannedQuantity));
               }
               setPlannedQuantity(typedValue);
               break;
            }
        
            case PROP_ID_completedQuantity:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_completedQuantity));
               }
               setCompletedQuantity(typedValue);
               break;
            }
        
            case PROP_ID_scrappedQuantity:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_scrappedQuantity));
               }
               setScrappedQuantity(typedValue);
               break;
            }
        
            case PROP_ID_businessDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_businessDate));
               }
               setBusinessDate(typedValue);
               break;
            }
        
            case PROP_ID_plannedStartDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_plannedStartDate));
               }
               setPlannedStartDate(typedValue);
               break;
            }
        
            case PROP_ID_plannedEndDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_plannedEndDate));
               }
               setPlannedEndDate(typedValue);
               break;
            }
        
            case PROP_ID_actualStartDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_actualStartDate));
               }
               setActualStartDate(typedValue);
               break;
            }
        
            case PROP_ID_actualEndDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_actualEndDate));
               }
               setActualEndDate(typedValue);
               break;
            }
        
            case PROP_ID_currencyId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_currencyId));
               }
               setCurrencyId(typedValue);
               break;
            }
        
            case PROP_ID_materialCost:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_materialCost));
               }
               setMaterialCost(typedValue);
               break;
            }
        
            case PROP_ID_laborCost:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_laborCost));
               }
               setLaborCost(typedValue);
               break;
            }
        
            case PROP_ID_overheadCost:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_overheadCost));
               }
               setOverheadCost(typedValue);
               break;
            }
        
            case PROP_ID_subcontractCost:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_subcontractCost));
               }
               setSubcontractCost(typedValue);
               break;
            }
        
            case PROP_ID_totalCost:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_totalCost));
               }
               setTotalCost(typedValue);
               break;
            }
        
            case PROP_ID_unitCost:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_unitCost));
               }
               setUnitCost(typedValue);
               break;
            }
        
            case PROP_ID_docStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_docStatus));
               }
               setDocStatus(typedValue);
               break;
            }
        
            case PROP_ID_approveStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_approveStatus));
               }
               setApproveStatus(typedValue);
               break;
            }
        
            case PROP_ID_priority:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_priority));
               }
               setPriority(typedValue);
               break;
            }
        
            case PROP_ID_posted:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_posted));
               }
               setPosted(typedValue);
               break;
            }
        
            case PROP_ID_postedAt:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_postedAt));
               }
               setPostedAt(typedValue);
               break;
            }
        
            case PROP_ID_postedBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_postedBy));
               }
               setPostedBy(typedValue);
               break;
            }
        
            case PROP_ID_remark:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_remark));
               }
               setRemark(typedValue);
               break;
            }
        
            case PROP_ID_delVersion:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_delVersion));
               }
               setDelVersion(typedValue);
               break;
            }
        
            case PROP_ID_version:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_version));
               }
               setVersion(typedValue);
               break;
            }
        
            case PROP_ID_createdBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_createdBy));
               }
               setCreatedBy(typedValue);
               break;
            }
        
            case PROP_ID_createTime:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_createTime));
               }
               setCreateTime(typedValue);
               break;
            }
        
            case PROP_ID_updatedBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_updatedBy));
               }
               setUpdatedBy(typedValue);
               break;
            }
        
            case PROP_ID_updateTime:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_updateTime));
               }
               setUpdateTime(typedValue);
               break;
            }
        
            case PROP_ID_sourceScheduleId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_sourceScheduleId));
               }
               setSourceScheduleId(typedValue);
               break;
            }
        
            case PROP_ID_exchangeRate:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_exchangeRate));
               }
               setExchangeRate(typedValue);
               break;
            }
        
            case PROP_ID_amountSource:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_amountSource));
               }
               setAmountSource(typedValue);
               break;
            }
        
            case PROP_ID_amountFunctional:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_amountFunctional));
               }
               setAmountFunctional(typedValue);
               break;
            }
        
           default:
              super.orm_propValue(propId,value);
        }
    }

    @Override
    public void orm_internalSet(int propId, Object value) {
        switch(propId){
        
            case PROP_ID_id:{
               onInitProp(propId);
               this._id = (java.lang.Long)value;
               orm_id(); // 如果是设置主键字段，则触发watcher
               break;
            }
        
            case PROP_ID_code:{
               onInitProp(propId);
               this._code = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_bomId:{
               onInitProp(propId);
               this._bomId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_routingId:{
               onInitProp(propId);
               this._routingId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_productionVersionId:{
               onInitProp(propId);
               this._productionVersionId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_sourceMrpPlanId:{
               onInitProp(propId);
               this._sourceMrpPlanId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_sourceOrderType:{
               onInitProp(propId);
               this._sourceOrderType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_sourceOrderCode:{
               onInitProp(propId);
               this._sourceOrderCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_productId:{
               onInitProp(propId);
               this._productId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_plannedQuantity:{
               onInitProp(propId);
               this._plannedQuantity = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_completedQuantity:{
               onInitProp(propId);
               this._completedQuantity = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_scrappedQuantity:{
               onInitProp(propId);
               this._scrappedQuantity = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_businessDate:{
               onInitProp(propId);
               this._businessDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_plannedStartDate:{
               onInitProp(propId);
               this._plannedStartDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_plannedEndDate:{
               onInitProp(propId);
               this._plannedEndDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_actualStartDate:{
               onInitProp(propId);
               this._actualStartDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_actualEndDate:{
               onInitProp(propId);
               this._actualEndDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_currencyId:{
               onInitProp(propId);
               this._currencyId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_materialCost:{
               onInitProp(propId);
               this._materialCost = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_laborCost:{
               onInitProp(propId);
               this._laborCost = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_overheadCost:{
               onInitProp(propId);
               this._overheadCost = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_subcontractCost:{
               onInitProp(propId);
               this._subcontractCost = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_totalCost:{
               onInitProp(propId);
               this._totalCost = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_unitCost:{
               onInitProp(propId);
               this._unitCost = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_docStatus:{
               onInitProp(propId);
               this._docStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_approveStatus:{
               onInitProp(propId);
               this._approveStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_priority:{
               onInitProp(propId);
               this._priority = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_posted:{
               onInitProp(propId);
               this._posted = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_postedAt:{
               onInitProp(propId);
               this._postedAt = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_postedBy:{
               onInitProp(propId);
               this._postedBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_remark:{
               onInitProp(propId);
               this._remark = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_delVersion:{
               onInitProp(propId);
               this._delVersion = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_version:{
               onInitProp(propId);
               this._version = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_createdBy:{
               onInitProp(propId);
               this._createdBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_createTime:{
               onInitProp(propId);
               this._createTime = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_updatedBy:{
               onInitProp(propId);
               this._updatedBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_updateTime:{
               onInitProp(propId);
               this._updateTime = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_sourceScheduleId:{
               onInitProp(propId);
               this._sourceScheduleId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_exchangeRate:{
               onInitProp(propId);
               this._exchangeRate = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_amountSource:{
               onInitProp(propId);
               this._amountSource = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_amountFunctional:{
               onInitProp(propId);
               this._amountFunctional = (java.lang.String)value;
               
               break;
            }
        
           default:
              super.orm_internalSet(propId,value);
        }
    }

    
    /**
     * ID: ID
     */
    public final java.lang.Long getId(){
         onPropGet(PROP_ID_id);
         return _id;
    }

    /**
     * ID: ID
     */
    public final void setId(java.lang.Long value){
        if(onPropSet(PROP_ID_id,value)){
            this._id = value;
            internalClearRefs(PROP_ID_id);
            orm_id();
        }
    }
    
    /**
     * 工单号: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 工单号: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
        }
    }
    
    /**
     * 业务组织: ORG_ID
     */
    public final java.lang.Long getOrgId(){
         onPropGet(PROP_ID_orgId);
         return _orgId;
    }

    /**
     * 业务组织: ORG_ID
     */
    public final void setOrgId(java.lang.Long value){
        if(onPropSet(PROP_ID_orgId,value)){
            this._orgId = value;
            internalClearRefs(PROP_ID_orgId);
            
        }
    }
    
    /**
     * BOM: BOM_ID
     */
    public final java.lang.Long getBomId(){
         onPropGet(PROP_ID_bomId);
         return _bomId;
    }

    /**
     * BOM: BOM_ID
     */
    public final void setBomId(java.lang.Long value){
        if(onPropSet(PROP_ID_bomId,value)){
            this._bomId = value;
            internalClearRefs(PROP_ID_bomId);
            
        }
    }
    
    /**
     * 工艺路线: ROUTING_ID
     */
    public final java.lang.Long getRoutingId(){
         onPropGet(PROP_ID_routingId);
         return _routingId;
    }

    /**
     * 工艺路线: ROUTING_ID
     */
    public final void setRoutingId(java.lang.Long value){
        if(onPropSet(PROP_ID_routingId,value)){
            this._routingId = value;
            internalClearRefs(PROP_ID_routingId);
            
        }
    }
    
    /**
     * 生产版本: PRODUCTION_VERSION_ID
     */
    public final java.lang.Long getProductionVersionId(){
         onPropGet(PROP_ID_productionVersionId);
         return _productionVersionId;
    }

    /**
     * 生产版本: PRODUCTION_VERSION_ID
     */
    public final void setProductionVersionId(java.lang.Long value){
        if(onPropSet(PROP_ID_productionVersionId,value)){
            this._productionVersionId = value;
            internalClearRefs(PROP_ID_productionVersionId);
            
        }
    }
    
    /**
     * 来源 MRP 计划: SOURCE_MRP_PLAN_ID
     */
    public final java.lang.Long getSourceMrpPlanId(){
         onPropGet(PROP_ID_sourceMrpPlanId);
         return _sourceMrpPlanId;
    }

    /**
     * 来源 MRP 计划: SOURCE_MRP_PLAN_ID
     */
    public final void setSourceMrpPlanId(java.lang.Long value){
        if(onPropSet(PROP_ID_sourceMrpPlanId,value)){
            this._sourceMrpPlanId = value;
            internalClearRefs(PROP_ID_sourceMrpPlanId);
            
        }
    }
    
    /**
     * 来源单据类型: SOURCE_ORDER_TYPE
     */
    public final java.lang.String getSourceOrderType(){
         onPropGet(PROP_ID_sourceOrderType);
         return _sourceOrderType;
    }

    /**
     * 来源单据类型: SOURCE_ORDER_TYPE
     */
    public final void setSourceOrderType(java.lang.String value){
        if(onPropSet(PROP_ID_sourceOrderType,value)){
            this._sourceOrderType = value;
            internalClearRefs(PROP_ID_sourceOrderType);
            
        }
    }
    
    /**
     * 来源单据号: SOURCE_ORDER_CODE
     */
    public final java.lang.String getSourceOrderCode(){
         onPropGet(PROP_ID_sourceOrderCode);
         return _sourceOrderCode;
    }

    /**
     * 来源单据号: SOURCE_ORDER_CODE
     */
    public final void setSourceOrderCode(java.lang.String value){
        if(onPropSet(PROP_ID_sourceOrderCode,value)){
            this._sourceOrderCode = value;
            internalClearRefs(PROP_ID_sourceOrderCode);
            
        }
    }
    
    /**
     * 产品(主产出): PRODUCT_ID
     */
    public final java.lang.Long getProductId(){
         onPropGet(PROP_ID_productId);
         return _productId;
    }

    /**
     * 产品(主产出): PRODUCT_ID
     */
    public final void setProductId(java.lang.Long value){
        if(onPropSet(PROP_ID_productId,value)){
            this._productId = value;
            internalClearRefs(PROP_ID_productId);
            
        }
    }
    
    /**
     * 计划数量: PLANNED_QUANTITY
     */
    public final java.math.BigDecimal getPlannedQuantity(){
         onPropGet(PROP_ID_plannedQuantity);
         return _plannedQuantity;
    }

    /**
     * 计划数量: PLANNED_QUANTITY
     */
    public final void setPlannedQuantity(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_plannedQuantity,value)){
            this._plannedQuantity = value;
            internalClearRefs(PROP_ID_plannedQuantity);
            
        }
    }
    
    /**
     * 完工数量: COMPLETED_QUANTITY
     */
    public final java.math.BigDecimal getCompletedQuantity(){
         onPropGet(PROP_ID_completedQuantity);
         return _completedQuantity;
    }

    /**
     * 完工数量: COMPLETED_QUANTITY
     */
    public final void setCompletedQuantity(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_completedQuantity,value)){
            this._completedQuantity = value;
            internalClearRefs(PROP_ID_completedQuantity);
            
        }
    }
    
    /**
     * 报废数量: SCRAPPED_QUANTITY
     */
    public final java.math.BigDecimal getScrappedQuantity(){
         onPropGet(PROP_ID_scrappedQuantity);
         return _scrappedQuantity;
    }

    /**
     * 报废数量: SCRAPPED_QUANTITY
     */
    public final void setScrappedQuantity(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_scrappedQuantity,value)){
            this._scrappedQuantity = value;
            internalClearRefs(PROP_ID_scrappedQuantity);
            
        }
    }
    
    /**
     * 工单日期: BUSINESS_DATE
     */
    public final java.time.LocalDate getBusinessDate(){
         onPropGet(PROP_ID_businessDate);
         return _businessDate;
    }

    /**
     * 工单日期: BUSINESS_DATE
     */
    public final void setBusinessDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_businessDate,value)){
            this._businessDate = value;
            internalClearRefs(PROP_ID_businessDate);
            
        }
    }
    
    /**
     * 计划开工日期: PLANNED_START_DATE
     */
    public final java.time.LocalDate getPlannedStartDate(){
         onPropGet(PROP_ID_plannedStartDate);
         return _plannedStartDate;
    }

    /**
     * 计划开工日期: PLANNED_START_DATE
     */
    public final void setPlannedStartDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_plannedStartDate,value)){
            this._plannedStartDate = value;
            internalClearRefs(PROP_ID_plannedStartDate);
            
        }
    }
    
    /**
     * 计划完工日期: PLANNED_END_DATE
     */
    public final java.time.LocalDate getPlannedEndDate(){
         onPropGet(PROP_ID_plannedEndDate);
         return _plannedEndDate;
    }

    /**
     * 计划完工日期: PLANNED_END_DATE
     */
    public final void setPlannedEndDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_plannedEndDate,value)){
            this._plannedEndDate = value;
            internalClearRefs(PROP_ID_plannedEndDate);
            
        }
    }
    
    /**
     * 实际开工日期: ACTUAL_START_DATE
     */
    public final java.time.LocalDate getActualStartDate(){
         onPropGet(PROP_ID_actualStartDate);
         return _actualStartDate;
    }

    /**
     * 实际开工日期: ACTUAL_START_DATE
     */
    public final void setActualStartDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_actualStartDate,value)){
            this._actualStartDate = value;
            internalClearRefs(PROP_ID_actualStartDate);
            
        }
    }
    
    /**
     * 实际完工日期: ACTUAL_END_DATE
     */
    public final java.time.LocalDate getActualEndDate(){
         onPropGet(PROP_ID_actualEndDate);
         return _actualEndDate;
    }

    /**
     * 实际完工日期: ACTUAL_END_DATE
     */
    public final void setActualEndDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_actualEndDate,value)){
            this._actualEndDate = value;
            internalClearRefs(PROP_ID_actualEndDate);
            
        }
    }
    
    /**
     * 币种: CURRENCY_ID
     */
    public final java.lang.Long getCurrencyId(){
         onPropGet(PROP_ID_currencyId);
         return _currencyId;
    }

    /**
     * 币种: CURRENCY_ID
     */
    public final void setCurrencyId(java.lang.Long value){
        if(onPropSet(PROP_ID_currencyId,value)){
            this._currencyId = value;
            internalClearRefs(PROP_ID_currencyId);
            
        }
    }
    
    /**
     * 材料成本: MATERIAL_COST
     */
    public final java.math.BigDecimal getMaterialCost(){
         onPropGet(PROP_ID_materialCost);
         return _materialCost;
    }

    /**
     * 材料成本: MATERIAL_COST
     */
    public final void setMaterialCost(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_materialCost,value)){
            this._materialCost = value;
            internalClearRefs(PROP_ID_materialCost);
            
        }
    }
    
    /**
     * 人工成本: LABOR_COST
     */
    public final java.math.BigDecimal getLaborCost(){
         onPropGet(PROP_ID_laborCost);
         return _laborCost;
    }

    /**
     * 人工成本: LABOR_COST
     */
    public final void setLaborCost(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_laborCost,value)){
            this._laborCost = value;
            internalClearRefs(PROP_ID_laborCost);
            
        }
    }
    
    /**
     * 制造费用: OVERHEAD_COST
     */
    public final java.math.BigDecimal getOverheadCost(){
         onPropGet(PROP_ID_overheadCost);
         return _overheadCost;
    }

    /**
     * 制造费用: OVERHEAD_COST
     */
    public final void setOverheadCost(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_overheadCost,value)){
            this._overheadCost = value;
            internalClearRefs(PROP_ID_overheadCost);
            
        }
    }
    
    /**
     * 委外成本: SUBCONTRACT_COST
     */
    public final java.math.BigDecimal getSubcontractCost(){
         onPropGet(PROP_ID_subcontractCost);
         return _subcontractCost;
    }

    /**
     * 委外成本: SUBCONTRACT_COST
     */
    public final void setSubcontractCost(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_subcontractCost,value)){
            this._subcontractCost = value;
            internalClearRefs(PROP_ID_subcontractCost);
            
        }
    }
    
    /**
     * 总成本: TOTAL_COST
     */
    public final java.math.BigDecimal getTotalCost(){
         onPropGet(PROP_ID_totalCost);
         return _totalCost;
    }

    /**
     * 总成本: TOTAL_COST
     */
    public final void setTotalCost(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_totalCost,value)){
            this._totalCost = value;
            internalClearRefs(PROP_ID_totalCost);
            
        }
    }
    
    /**
     * 单位成本: UNIT_COST
     */
    public final java.math.BigDecimal getUnitCost(){
         onPropGet(PROP_ID_unitCost);
         return _unitCost;
    }

    /**
     * 单位成本: UNIT_COST
     */
    public final void setUnitCost(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_unitCost,value)){
            this._unitCost = value;
            internalClearRefs(PROP_ID_unitCost);
            
        }
    }
    
    /**
     * 单据状态: DOC_STATUS
     */
    public final java.lang.String getDocStatus(){
         onPropGet(PROP_ID_docStatus);
         return _docStatus;
    }

    /**
     * 单据状态: DOC_STATUS
     */
    public final void setDocStatus(java.lang.String value){
        if(onPropSet(PROP_ID_docStatus,value)){
            this._docStatus = value;
            internalClearRefs(PROP_ID_docStatus);
            
        }
    }
    
    /**
     * 审核状态: APPROVE_STATUS
     */
    public final java.lang.String getApproveStatus(){
         onPropGet(PROP_ID_approveStatus);
         return _approveStatus;
    }

    /**
     * 审核状态: APPROVE_STATUS
     */
    public final void setApproveStatus(java.lang.String value){
        if(onPropSet(PROP_ID_approveStatus,value)){
            this._approveStatus = value;
            internalClearRefs(PROP_ID_approveStatus);
            
        }
    }
    
    /**
     * 优先级: PRIORITY
     */
    public final java.lang.String getPriority(){
         onPropGet(PROP_ID_priority);
         return _priority;
    }

    /**
     * 优先级: PRIORITY
     */
    public final void setPriority(java.lang.String value){
        if(onPropSet(PROP_ID_priority,value)){
            this._priority = value;
            internalClearRefs(PROP_ID_priority);
            
        }
    }
    
    /**
     * 已过账: POSTED
     */
    public final java.lang.Boolean getPosted(){
         onPropGet(PROP_ID_posted);
         return _posted;
    }

    /**
     * 已过账: POSTED
     */
    public final void setPosted(java.lang.Boolean value){
        if(onPropSet(PROP_ID_posted,value)){
            this._posted = value;
            internalClearRefs(PROP_ID_posted);
            
        }
    }
    
    /**
     * 过账时间: POSTED_AT
     */
    public final java.time.LocalDateTime getPostedAt(){
         onPropGet(PROP_ID_postedAt);
         return _postedAt;
    }

    /**
     * 过账时间: POSTED_AT
     */
    public final void setPostedAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_postedAt,value)){
            this._postedAt = value;
            internalClearRefs(PROP_ID_postedAt);
            
        }
    }
    
    /**
     * 过账人: POSTED_BY
     */
    public final java.lang.String getPostedBy(){
         onPropGet(PROP_ID_postedBy);
         return _postedBy;
    }

    /**
     * 过账人: POSTED_BY
     */
    public final void setPostedBy(java.lang.String value){
        if(onPropSet(PROP_ID_postedBy,value)){
            this._postedBy = value;
            internalClearRefs(PROP_ID_postedBy);
            
        }
    }
    
    /**
     * 备注: REMARK
     */
    public final java.lang.String getRemark(){
         onPropGet(PROP_ID_remark);
         return _remark;
    }

    /**
     * 备注: REMARK
     */
    public final void setRemark(java.lang.String value){
        if(onPropSet(PROP_ID_remark,value)){
            this._remark = value;
            internalClearRefs(PROP_ID_remark);
            
        }
    }
    
    /**
     * 逻辑删除版本: DEL_VERSION
     */
    public final java.lang.Long getDelVersion(){
         onPropGet(PROP_ID_delVersion);
         return _delVersion;
    }

    /**
     * 逻辑删除版本: DEL_VERSION
     */
    public final void setDelVersion(java.lang.Long value){
        if(onPropSet(PROP_ID_delVersion,value)){
            this._delVersion = value;
            internalClearRefs(PROP_ID_delVersion);
            
        }
    }
    
    /**
     * 数据版本: VERSION
     */
    public final java.lang.Integer getVersion(){
         onPropGet(PROP_ID_version);
         return _version;
    }

    /**
     * 数据版本: VERSION
     */
    public final void setVersion(java.lang.Integer value){
        if(onPropSet(PROP_ID_version,value)){
            this._version = value;
            internalClearRefs(PROP_ID_version);
            
        }
    }
    
    /**
     * 创建人: CREATED_BY
     */
    public final java.lang.String getCreatedBy(){
         onPropGet(PROP_ID_createdBy);
         return _createdBy;
    }

    /**
     * 创建人: CREATED_BY
     */
    public final void setCreatedBy(java.lang.String value){
        if(onPropSet(PROP_ID_createdBy,value)){
            this._createdBy = value;
            internalClearRefs(PROP_ID_createdBy);
            
        }
    }
    
    /**
     * 创建时间: CREATE_TIME
     */
    public final java.sql.Timestamp getCreateTime(){
         onPropGet(PROP_ID_createTime);
         return _createTime;
    }

    /**
     * 创建时间: CREATE_TIME
     */
    public final void setCreateTime(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_createTime,value)){
            this._createTime = value;
            internalClearRefs(PROP_ID_createTime);
            
        }
    }
    
    /**
     * 修改人: UPDATED_BY
     */
    public final java.lang.String getUpdatedBy(){
         onPropGet(PROP_ID_updatedBy);
         return _updatedBy;
    }

    /**
     * 修改人: UPDATED_BY
     */
    public final void setUpdatedBy(java.lang.String value){
        if(onPropSet(PROP_ID_updatedBy,value)){
            this._updatedBy = value;
            internalClearRefs(PROP_ID_updatedBy);
            
        }
    }
    
    /**
     * 修改时间: UPDATE_TIME
     */
    public final java.sql.Timestamp getUpdateTime(){
         onPropGet(PROP_ID_updateTime);
         return _updateTime;
    }

    /**
     * 修改时间: UPDATE_TIME
     */
    public final void setUpdateTime(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_updateTime,value)){
            this._updateTime = value;
            internalClearRefs(PROP_ID_updateTime);
            
        }
    }
    
    /**
     * APS排程来源(弱参照): SOURCE_SCHEDULE_ID
     */
    public final java.lang.Long getSourceScheduleId(){
         onPropGet(PROP_ID_sourceScheduleId);
         return _sourceScheduleId;
    }

    /**
     * APS排程来源(弱参照): SOURCE_SCHEDULE_ID
     */
    public final void setSourceScheduleId(java.lang.Long value){
        if(onPropSet(PROP_ID_sourceScheduleId,value)){
            this._sourceScheduleId = value;
            internalClearRefs(PROP_ID_sourceScheduleId);
            
        }
    }
    
    /**
     * 汇率: EXCHANGE_RATE
     */
    public final java.lang.String getExchangeRate(){
         onPropGet(PROP_ID_exchangeRate);
         return _exchangeRate;
    }

    /**
     * 汇率: EXCHANGE_RATE
     */
    public final void setExchangeRate(java.lang.String value){
        if(onPropSet(PROP_ID_exchangeRate,value)){
            this._exchangeRate = value;
            internalClearRefs(PROP_ID_exchangeRate);
            
        }
    }
    
    /**
     * 源币种金额: AMOUNT_SOURCE
     */
    public final java.lang.String getAmountSource(){
         onPropGet(PROP_ID_amountSource);
         return _amountSource;
    }

    /**
     * 源币种金额: AMOUNT_SOURCE
     */
    public final void setAmountSource(java.lang.String value){
        if(onPropSet(PROP_ID_amountSource,value)){
            this._amountSource = value;
            internalClearRefs(PROP_ID_amountSource);
            
        }
    }
    
    /**
     * 本位币金额: AMOUNT_FUNCTIONAL
     */
    public final java.lang.String getAmountFunctional(){
         onPropGet(PROP_ID_amountFunctional);
         return _amountFunctional;
    }

    /**
     * 本位币金额: AMOUNT_FUNCTIONAL
     */
    public final void setAmountFunctional(java.lang.String value){
        if(onPropSet(PROP_ID_amountFunctional,value)){
            this._amountFunctional = value;
            internalClearRefs(PROP_ID_amountFunctional);
            
        }
    }
    
    /**
     * 
     */
    public final app.erp.mfg.dao.entity.ErpMfgBom getBom(){
       return (app.erp.mfg.dao.entity.ErpMfgBom)internalGetRefEntity(PROP_NAME_bom);
    }

    public final void setBom(app.erp.mfg.dao.entity.ErpMfgBom refEntity){
   
           if(refEntity == null){
           
                   this.setBomId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_bom, refEntity,()->{
           
                           this.setBomId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.mfg.dao.entity.ErpMfgRouting getRouting(){
       return (app.erp.mfg.dao.entity.ErpMfgRouting)internalGetRefEntity(PROP_NAME_routing);
    }

    public final void setRouting(app.erp.mfg.dao.entity.ErpMfgRouting refEntity){
   
           if(refEntity == null){
           
                   this.setRoutingId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_routing, refEntity,()->{
           
                           this.setRoutingId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.mfg.dao.entity.ErpMfgProductionVersion getProductionVersion(){
       return (app.erp.mfg.dao.entity.ErpMfgProductionVersion)internalGetRefEntity(PROP_NAME_productionVersion);
    }

    public final void setProductionVersion(app.erp.mfg.dao.entity.ErpMfgProductionVersion refEntity){
   
           if(refEntity == null){
           
                   this.setProductionVersionId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_productionVersion, refEntity,()->{
           
                           this.setProductionVersionId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdMaterial getProduct(){
       return (app.erp.md.dao.entity.ErpMdMaterial)internalGetRefEntity(PROP_NAME_product);
    }

    public final void setProduct(app.erp.md.dao.entity.ErpMdMaterial refEntity){
   
           if(refEntity == null){
           
                   this.setProductId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_product, refEntity,()->{
           
                           this.setProductId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdCurrency getCurrency(){
       return (app.erp.md.dao.entity.ErpMdCurrency)internalGetRefEntity(PROP_NAME_currency);
    }

    public final void setCurrency(app.erp.md.dao.entity.ErpMdCurrency refEntity){
   
           if(refEntity == null){
           
                   this.setCurrencyId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_currency, refEntity,()->{
           
                           this.setCurrencyId(refEntity.getId());
                       
           });
           }
       
    }
       
    private final OrmEntitySet<app.erp.mfg.dao.entity.ErpMfgWorkOrderLine> _lines = new OrmEntitySet<>(this, PROP_NAME_lines,
        null, null,app.erp.mfg.dao.entity.ErpMfgWorkOrderLine.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.mfg.dao.entity.ErpMfgWorkOrderLine> getLines(){
       return _lines;
    }
       
    private final OrmEntitySet<app.erp.mfg.dao.entity.ErpMfgJobCard> _jobCards = new OrmEntitySet<>(this, PROP_NAME_jobCards,
        null, null,app.erp.mfg.dao.entity.ErpMfgJobCard.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.mfg.dao.entity.ErpMfgJobCard> getJobCards(){
       return _jobCards;
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdOrganization getOrg(){
       return (app.erp.md.dao.entity.ErpMdOrganization)internalGetRefEntity(PROP_NAME_org);
    }

    public final void setOrg(app.erp.md.dao.entity.ErpMdOrganization refEntity){
   
           if(refEntity == null){
           
                   this.setOrgId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_org, refEntity,()->{
           
                           this.setOrgId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.mfg.dao.entity.ErpMfgMrpPlan getSourceMrpPlan(){
       return (app.erp.mfg.dao.entity.ErpMfgMrpPlan)internalGetRefEntity(PROP_NAME_sourceMrpPlan);
    }

    public final void setSourceMrpPlan(app.erp.mfg.dao.entity.ErpMfgMrpPlan refEntity){
   
           if(refEntity == null){
           
                   this.setSourceMrpPlanId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_sourceMrpPlan, refEntity,()->{
           
                           this.setSourceMrpPlanId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
