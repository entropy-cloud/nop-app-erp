package app.erp.drp.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.drp.dao.entity.ErpDrpLine;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  DRP明细: erp_drp_line
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpDrpLine extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 计划ID: PLAN_ID BIGINT */
    public static final String PROP_NAME_planId = "planId";
    public static final int PROP_ID_planId = 2;
    
    /* 行号: LINE_NO INTEGER */
    public static final String PROP_NAME_lineNo = "lineNo";
    public static final int PROP_ID_lineNo = 3;
    
    /* 物料: MATERIAL_ID BIGINT */
    public static final String PROP_NAME_materialId = "materialId";
    public static final int PROP_ID_materialId = 4;
    
    /* 目标仓库: WAREHOUSE_ID BIGINT */
    public static final String PROP_NAME_warehouseId = "warehouseId";
    public static final int PROP_ID_warehouseId = 5;
    
    /* 来源仓库: SOURCE_WAREHOUSE_ID BIGINT */
    public static final String PROP_NAME_sourceWarehouseId = "sourceWarehouseId";
    public static final int PROP_ID_sourceWarehouseId = 6;
    
    /* 补货类型: REPLENISHMENT_TYPE INTEGER */
    public static final String PROP_NAME_replenishmentType = "replenishmentType";
    public static final int PROP_ID_replenishmentType = 7;
    
    /* 当前库存: CURRENT_STOCK DECIMAL */
    public static final String PROP_NAME_currentStock = "currentStock";
    public static final int PROP_ID_currentStock = 8;
    
    /* 已分配量: ALLOCATED_QTY DECIMAL */
    public static final String PROP_NAME_allocatedQty = "allocatedQty";
    public static final int PROP_ID_allocatedQty = 9;
    
    /* 在单量: ON_ORDER_QTY DECIMAL */
    public static final String PROP_NAME_onOrderQty = "onOrderQty";
    public static final int PROP_ID_onOrderQty = 10;
    
    /* 预测需求量: FORECAST_DEMAND DECIMAL */
    public static final String PROP_NAME_forecastDemand = "forecastDemand";
    public static final int PROP_ID_forecastDemand = 11;
    
    /* 安全库存: SAFETY_STOCK DECIMAL */
    public static final String PROP_NAME_safetyStock = "safetyStock";
    public static final int PROP_ID_safetyStock = 12;
    
    /* 净需求: NET_REQUIREMENT DECIMAL */
    public static final String PROP_NAME_netRequirement = "netRequirement";
    public static final int PROP_ID_netRequirement = 13;
    
    /* 建议补货量: SUGGESTED_QTY DECIMAL */
    public static final String PROP_NAME_suggestedQty = "suggestedQty";
    public static final int PROP_ID_suggestedQty = 14;
    
    /* 批准补货量: APPROVED_QTY DECIMAL */
    public static final String PROP_NAME_approvedQty = "approvedQty";
    public static final int PROP_ID_approvedQty = 15;
    
    /* 生成单据类型: ORDER_BILL_TYPE VARCHAR */
    public static final String PROP_NAME_orderBillType = "orderBillType";
    public static final int PROP_ID_orderBillType = 16;
    
    /* 生成单据号: ORDER_BILL_CODE VARCHAR */
    public static final String PROP_NAME_orderBillCode = "orderBillCode";
    public static final int PROP_ID_orderBillCode = 17;
    
    /* 状态: STATUS INTEGER */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 18;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 19;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 20;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 21;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 22;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 23;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 24;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 25;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 26;
    

    private static int _PROP_ID_BOUND = 27;

    
    /* relation:  */
    public static final String PROP_NAME_plan = "plan";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[27];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_planId] = PROP_NAME_planId;
          PROP_NAME_TO_ID.put(PROP_NAME_planId, PROP_ID_planId);
      
          PROP_ID_TO_NAME[PROP_ID_lineNo] = PROP_NAME_lineNo;
          PROP_NAME_TO_ID.put(PROP_NAME_lineNo, PROP_ID_lineNo);
      
          PROP_ID_TO_NAME[PROP_ID_materialId] = PROP_NAME_materialId;
          PROP_NAME_TO_ID.put(PROP_NAME_materialId, PROP_ID_materialId);
      
          PROP_ID_TO_NAME[PROP_ID_warehouseId] = PROP_NAME_warehouseId;
          PROP_NAME_TO_ID.put(PROP_NAME_warehouseId, PROP_ID_warehouseId);
      
          PROP_ID_TO_NAME[PROP_ID_sourceWarehouseId] = PROP_NAME_sourceWarehouseId;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceWarehouseId, PROP_ID_sourceWarehouseId);
      
          PROP_ID_TO_NAME[PROP_ID_replenishmentType] = PROP_NAME_replenishmentType;
          PROP_NAME_TO_ID.put(PROP_NAME_replenishmentType, PROP_ID_replenishmentType);
      
          PROP_ID_TO_NAME[PROP_ID_currentStock] = PROP_NAME_currentStock;
          PROP_NAME_TO_ID.put(PROP_NAME_currentStock, PROP_ID_currentStock);
      
          PROP_ID_TO_NAME[PROP_ID_allocatedQty] = PROP_NAME_allocatedQty;
          PROP_NAME_TO_ID.put(PROP_NAME_allocatedQty, PROP_ID_allocatedQty);
      
          PROP_ID_TO_NAME[PROP_ID_onOrderQty] = PROP_NAME_onOrderQty;
          PROP_NAME_TO_ID.put(PROP_NAME_onOrderQty, PROP_ID_onOrderQty);
      
          PROP_ID_TO_NAME[PROP_ID_forecastDemand] = PROP_NAME_forecastDemand;
          PROP_NAME_TO_ID.put(PROP_NAME_forecastDemand, PROP_ID_forecastDemand);
      
          PROP_ID_TO_NAME[PROP_ID_safetyStock] = PROP_NAME_safetyStock;
          PROP_NAME_TO_ID.put(PROP_NAME_safetyStock, PROP_ID_safetyStock);
      
          PROP_ID_TO_NAME[PROP_ID_netRequirement] = PROP_NAME_netRequirement;
          PROP_NAME_TO_ID.put(PROP_NAME_netRequirement, PROP_ID_netRequirement);
      
          PROP_ID_TO_NAME[PROP_ID_suggestedQty] = PROP_NAME_suggestedQty;
          PROP_NAME_TO_ID.put(PROP_NAME_suggestedQty, PROP_ID_suggestedQty);
      
          PROP_ID_TO_NAME[PROP_ID_approvedQty] = PROP_NAME_approvedQty;
          PROP_NAME_TO_ID.put(PROP_NAME_approvedQty, PROP_ID_approvedQty);
      
          PROP_ID_TO_NAME[PROP_ID_orderBillType] = PROP_NAME_orderBillType;
          PROP_NAME_TO_ID.put(PROP_NAME_orderBillType, PROP_ID_orderBillType);
      
          PROP_ID_TO_NAME[PROP_ID_orderBillCode] = PROP_NAME_orderBillCode;
          PROP_NAME_TO_ID.put(PROP_NAME_orderBillCode, PROP_ID_orderBillCode);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
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
      
    }

    
    /* ID: ID */
    private java.lang.Long _id;
    
    /* 计划ID: PLAN_ID */
    private java.lang.Long _planId;
    
    /* 行号: LINE_NO */
    private java.lang.Integer _lineNo;
    
    /* 物料: MATERIAL_ID */
    private java.lang.Long _materialId;
    
    /* 目标仓库: WAREHOUSE_ID */
    private java.lang.Long _warehouseId;
    
    /* 来源仓库: SOURCE_WAREHOUSE_ID */
    private java.lang.Long _sourceWarehouseId;
    
    /* 补货类型: REPLENISHMENT_TYPE */
    private java.lang.Integer _replenishmentType;
    
    /* 当前库存: CURRENT_STOCK */
    private java.math.BigDecimal _currentStock;
    
    /* 已分配量: ALLOCATED_QTY */
    private java.math.BigDecimal _allocatedQty;
    
    /* 在单量: ON_ORDER_QTY */
    private java.math.BigDecimal _onOrderQty;
    
    /* 预测需求量: FORECAST_DEMAND */
    private java.math.BigDecimal _forecastDemand;
    
    /* 安全库存: SAFETY_STOCK */
    private java.math.BigDecimal _safetyStock;
    
    /* 净需求: NET_REQUIREMENT */
    private java.math.BigDecimal _netRequirement;
    
    /* 建议补货量: SUGGESTED_QTY */
    private java.math.BigDecimal _suggestedQty;
    
    /* 批准补货量: APPROVED_QTY */
    private java.math.BigDecimal _approvedQty;
    
    /* 生成单据类型: ORDER_BILL_TYPE */
    private java.lang.String _orderBillType;
    
    /* 生成单据号: ORDER_BILL_CODE */
    private java.lang.String _orderBillCode;
    
    /* 状态: STATUS */
    private java.lang.Integer _status;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
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
    

    public _ErpDrpLine(){
        // for debug
    }

    protected ErpDrpLine newInstance(){
        ErpDrpLine entity = new ErpDrpLine();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpDrpLine cloneInstance() {
        ErpDrpLine entity = newInstance();
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
      return "app.erp.drp.dao.entity.ErpDrpLine";
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
        
            case PROP_ID_planId:
               return getPlanId();
        
            case PROP_ID_lineNo:
               return getLineNo();
        
            case PROP_ID_materialId:
               return getMaterialId();
        
            case PROP_ID_warehouseId:
               return getWarehouseId();
        
            case PROP_ID_sourceWarehouseId:
               return getSourceWarehouseId();
        
            case PROP_ID_replenishmentType:
               return getReplenishmentType();
        
            case PROP_ID_currentStock:
               return getCurrentStock();
        
            case PROP_ID_allocatedQty:
               return getAllocatedQty();
        
            case PROP_ID_onOrderQty:
               return getOnOrderQty();
        
            case PROP_ID_forecastDemand:
               return getForecastDemand();
        
            case PROP_ID_safetyStock:
               return getSafetyStock();
        
            case PROP_ID_netRequirement:
               return getNetRequirement();
        
            case PROP_ID_suggestedQty:
               return getSuggestedQty();
        
            case PROP_ID_approvedQty:
               return getApprovedQty();
        
            case PROP_ID_orderBillType:
               return getOrderBillType();
        
            case PROP_ID_orderBillCode:
               return getOrderBillCode();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_orgId:
               return getOrgId();
        
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
        
            case PROP_ID_planId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_planId));
               }
               setPlanId(typedValue);
               break;
            }
        
            case PROP_ID_lineNo:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_lineNo));
               }
               setLineNo(typedValue);
               break;
            }
        
            case PROP_ID_materialId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_materialId));
               }
               setMaterialId(typedValue);
               break;
            }
        
            case PROP_ID_warehouseId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_warehouseId));
               }
               setWarehouseId(typedValue);
               break;
            }
        
            case PROP_ID_sourceWarehouseId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_sourceWarehouseId));
               }
               setSourceWarehouseId(typedValue);
               break;
            }
        
            case PROP_ID_replenishmentType:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_replenishmentType));
               }
               setReplenishmentType(typedValue);
               break;
            }
        
            case PROP_ID_currentStock:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_currentStock));
               }
               setCurrentStock(typedValue);
               break;
            }
        
            case PROP_ID_allocatedQty:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_allocatedQty));
               }
               setAllocatedQty(typedValue);
               break;
            }
        
            case PROP_ID_onOrderQty:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_onOrderQty));
               }
               setOnOrderQty(typedValue);
               break;
            }
        
            case PROP_ID_forecastDemand:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_forecastDemand));
               }
               setForecastDemand(typedValue);
               break;
            }
        
            case PROP_ID_safetyStock:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_safetyStock));
               }
               setSafetyStock(typedValue);
               break;
            }
        
            case PROP_ID_netRequirement:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_netRequirement));
               }
               setNetRequirement(typedValue);
               break;
            }
        
            case PROP_ID_suggestedQty:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_suggestedQty));
               }
               setSuggestedQty(typedValue);
               break;
            }
        
            case PROP_ID_approvedQty:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_approvedQty));
               }
               setApprovedQty(typedValue);
               break;
            }
        
            case PROP_ID_orderBillType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_orderBillType));
               }
               setOrderBillType(typedValue);
               break;
            }
        
            case PROP_ID_orderBillCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_orderBillCode));
               }
               setOrderBillCode(typedValue);
               break;
            }
        
            case PROP_ID_status:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_status));
               }
               setStatus(typedValue);
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
        
            case PROP_ID_planId:{
               onInitProp(propId);
               this._planId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_lineNo:{
               onInitProp(propId);
               this._lineNo = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_materialId:{
               onInitProp(propId);
               this._materialId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_warehouseId:{
               onInitProp(propId);
               this._warehouseId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_sourceWarehouseId:{
               onInitProp(propId);
               this._sourceWarehouseId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_replenishmentType:{
               onInitProp(propId);
               this._replenishmentType = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_currentStock:{
               onInitProp(propId);
               this._currentStock = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_allocatedQty:{
               onInitProp(propId);
               this._allocatedQty = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_onOrderQty:{
               onInitProp(propId);
               this._onOrderQty = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_forecastDemand:{
               onInitProp(propId);
               this._forecastDemand = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_safetyStock:{
               onInitProp(propId);
               this._safetyStock = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_netRequirement:{
               onInitProp(propId);
               this._netRequirement = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_suggestedQty:{
               onInitProp(propId);
               this._suggestedQty = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_approvedQty:{
               onInitProp(propId);
               this._approvedQty = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_orderBillType:{
               onInitProp(propId);
               this._orderBillType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_orderBillCode:{
               onInitProp(propId);
               this._orderBillCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
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
     * 计划ID: PLAN_ID
     */
    public final java.lang.Long getPlanId(){
         onPropGet(PROP_ID_planId);
         return _planId;
    }

    /**
     * 计划ID: PLAN_ID
     */
    public final void setPlanId(java.lang.Long value){
        if(onPropSet(PROP_ID_planId,value)){
            this._planId = value;
            internalClearRefs(PROP_ID_planId);
            
        }
    }
    
    /**
     * 行号: LINE_NO
     */
    public final java.lang.Integer getLineNo(){
         onPropGet(PROP_ID_lineNo);
         return _lineNo;
    }

    /**
     * 行号: LINE_NO
     */
    public final void setLineNo(java.lang.Integer value){
        if(onPropSet(PROP_ID_lineNo,value)){
            this._lineNo = value;
            internalClearRefs(PROP_ID_lineNo);
            
        }
    }
    
    /**
     * 物料: MATERIAL_ID
     */
    public final java.lang.Long getMaterialId(){
         onPropGet(PROP_ID_materialId);
         return _materialId;
    }

    /**
     * 物料: MATERIAL_ID
     */
    public final void setMaterialId(java.lang.Long value){
        if(onPropSet(PROP_ID_materialId,value)){
            this._materialId = value;
            internalClearRefs(PROP_ID_materialId);
            
        }
    }
    
    /**
     * 目标仓库: WAREHOUSE_ID
     */
    public final java.lang.Long getWarehouseId(){
         onPropGet(PROP_ID_warehouseId);
         return _warehouseId;
    }

    /**
     * 目标仓库: WAREHOUSE_ID
     */
    public final void setWarehouseId(java.lang.Long value){
        if(onPropSet(PROP_ID_warehouseId,value)){
            this._warehouseId = value;
            internalClearRefs(PROP_ID_warehouseId);
            
        }
    }
    
    /**
     * 来源仓库: SOURCE_WAREHOUSE_ID
     */
    public final java.lang.Long getSourceWarehouseId(){
         onPropGet(PROP_ID_sourceWarehouseId);
         return _sourceWarehouseId;
    }

    /**
     * 来源仓库: SOURCE_WAREHOUSE_ID
     */
    public final void setSourceWarehouseId(java.lang.Long value){
        if(onPropSet(PROP_ID_sourceWarehouseId,value)){
            this._sourceWarehouseId = value;
            internalClearRefs(PROP_ID_sourceWarehouseId);
            
        }
    }
    
    /**
     * 补货类型: REPLENISHMENT_TYPE
     */
    public final java.lang.Integer getReplenishmentType(){
         onPropGet(PROP_ID_replenishmentType);
         return _replenishmentType;
    }

    /**
     * 补货类型: REPLENISHMENT_TYPE
     */
    public final void setReplenishmentType(java.lang.Integer value){
        if(onPropSet(PROP_ID_replenishmentType,value)){
            this._replenishmentType = value;
            internalClearRefs(PROP_ID_replenishmentType);
            
        }
    }
    
    /**
     * 当前库存: CURRENT_STOCK
     */
    public final java.math.BigDecimal getCurrentStock(){
         onPropGet(PROP_ID_currentStock);
         return _currentStock;
    }

    /**
     * 当前库存: CURRENT_STOCK
     */
    public final void setCurrentStock(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_currentStock,value)){
            this._currentStock = value;
            internalClearRefs(PROP_ID_currentStock);
            
        }
    }
    
    /**
     * 已分配量: ALLOCATED_QTY
     */
    public final java.math.BigDecimal getAllocatedQty(){
         onPropGet(PROP_ID_allocatedQty);
         return _allocatedQty;
    }

    /**
     * 已分配量: ALLOCATED_QTY
     */
    public final void setAllocatedQty(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_allocatedQty,value)){
            this._allocatedQty = value;
            internalClearRefs(PROP_ID_allocatedQty);
            
        }
    }
    
    /**
     * 在单量: ON_ORDER_QTY
     */
    public final java.math.BigDecimal getOnOrderQty(){
         onPropGet(PROP_ID_onOrderQty);
         return _onOrderQty;
    }

    /**
     * 在单量: ON_ORDER_QTY
     */
    public final void setOnOrderQty(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_onOrderQty,value)){
            this._onOrderQty = value;
            internalClearRefs(PROP_ID_onOrderQty);
            
        }
    }
    
    /**
     * 预测需求量: FORECAST_DEMAND
     */
    public final java.math.BigDecimal getForecastDemand(){
         onPropGet(PROP_ID_forecastDemand);
         return _forecastDemand;
    }

    /**
     * 预测需求量: FORECAST_DEMAND
     */
    public final void setForecastDemand(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_forecastDemand,value)){
            this._forecastDemand = value;
            internalClearRefs(PROP_ID_forecastDemand);
            
        }
    }
    
    /**
     * 安全库存: SAFETY_STOCK
     */
    public final java.math.BigDecimal getSafetyStock(){
         onPropGet(PROP_ID_safetyStock);
         return _safetyStock;
    }

    /**
     * 安全库存: SAFETY_STOCK
     */
    public final void setSafetyStock(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_safetyStock,value)){
            this._safetyStock = value;
            internalClearRefs(PROP_ID_safetyStock);
            
        }
    }
    
    /**
     * 净需求: NET_REQUIREMENT
     */
    public final java.math.BigDecimal getNetRequirement(){
         onPropGet(PROP_ID_netRequirement);
         return _netRequirement;
    }

    /**
     * 净需求: NET_REQUIREMENT
     */
    public final void setNetRequirement(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_netRequirement,value)){
            this._netRequirement = value;
            internalClearRefs(PROP_ID_netRequirement);
            
        }
    }
    
    /**
     * 建议补货量: SUGGESTED_QTY
     */
    public final java.math.BigDecimal getSuggestedQty(){
         onPropGet(PROP_ID_suggestedQty);
         return _suggestedQty;
    }

    /**
     * 建议补货量: SUGGESTED_QTY
     */
    public final void setSuggestedQty(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_suggestedQty,value)){
            this._suggestedQty = value;
            internalClearRefs(PROP_ID_suggestedQty);
            
        }
    }
    
    /**
     * 批准补货量: APPROVED_QTY
     */
    public final java.math.BigDecimal getApprovedQty(){
         onPropGet(PROP_ID_approvedQty);
         return _approvedQty;
    }

    /**
     * 批准补货量: APPROVED_QTY
     */
    public final void setApprovedQty(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_approvedQty,value)){
            this._approvedQty = value;
            internalClearRefs(PROP_ID_approvedQty);
            
        }
    }
    
    /**
     * 生成单据类型: ORDER_BILL_TYPE
     */
    public final java.lang.String getOrderBillType(){
         onPropGet(PROP_ID_orderBillType);
         return _orderBillType;
    }

    /**
     * 生成单据类型: ORDER_BILL_TYPE
     */
    public final void setOrderBillType(java.lang.String value){
        if(onPropSet(PROP_ID_orderBillType,value)){
            this._orderBillType = value;
            internalClearRefs(PROP_ID_orderBillType);
            
        }
    }
    
    /**
     * 生成单据号: ORDER_BILL_CODE
     */
    public final java.lang.String getOrderBillCode(){
         onPropGet(PROP_ID_orderBillCode);
         return _orderBillCode;
    }

    /**
     * 生成单据号: ORDER_BILL_CODE
     */
    public final void setOrderBillCode(java.lang.String value){
        if(onPropSet(PROP_ID_orderBillCode,value)){
            this._orderBillCode = value;
            internalClearRefs(PROP_ID_orderBillCode);
            
        }
    }
    
    /**
     * 状态: STATUS
     */
    public final java.lang.Integer getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 状态: STATUS
     */
    public final void setStatus(java.lang.Integer value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
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
     * 
     */
    public final app.erp.drp.dao.entity.ErpDrpPlan getPlan(){
       return (app.erp.drp.dao.entity.ErpDrpPlan)internalGetRefEntity(PROP_NAME_plan);
    }

    public final void setPlan(app.erp.drp.dao.entity.ErpDrpPlan refEntity){
   
           if(refEntity == null){
           
                   this.setPlanId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_plan, refEntity,()->{
           
                           this.setPlanId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
