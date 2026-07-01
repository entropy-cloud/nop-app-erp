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

import app.erp.drp.dao.entity.ErpInvDrpSafetyStockCalc;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  安全库存计算: erp_inv_drp_safety_stock_calc
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpInvDrpSafetyStockCalc extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 编号: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 物料: MATERIAL_ID BIGINT */
    public static final String PROP_NAME_materialId = "materialId";
    public static final int PROP_ID_materialId = 4;
    
    /* 仓库: WAREHOUSE_ID BIGINT */
    public static final String PROP_NAME_warehouseId = "warehouseId";
    public static final int PROP_ID_warehouseId = 5;
    
    /* 计算方法: METHOD INTEGER */
    public static final String PROP_NAME_method = "method";
    public static final int PROP_ID_method = 6;
    
    /* 服务水平: SERVICE_LEVEL INTEGER */
    public static final String PROP_NAME_serviceLevel = "serviceLevel";
    public static final int PROP_ID_serviceLevel = 7;
    
    /* 分析月数: HISTORY_MONTHS INTEGER */
    public static final String PROP_NAME_historyMonths = "historyMonths";
    public static final int PROP_ID_historyMonths = 8;
    
    /* 提前期(天): LEAD_TIME_DAYS INTEGER */
    public static final String PROP_NAME_leadTimeDays = "leadTimeDays";
    public static final int PROP_ID_leadTimeDays = 9;
    
    /* 建议安全库存: CALCULATED_SAFETY_STOCK DECIMAL */
    public static final String PROP_NAME_calculatedSafetyStock = "calculatedSafetyStock";
    public static final int PROP_ID_calculatedSafetyStock = 10;
    
    /* 建议再订货点: CALCULATED_ROP DECIMAL */
    public static final String PROP_NAME_calculatedRop = "calculatedRop";
    public static final int PROP_ID_calculatedRop = 11;
    
    /* 覆盖安全库存: OVERRIDE_SAFETY_STOCK DECIMAL */
    public static final String PROP_NAME_overrideSafetyStock = "overrideSafetyStock";
    public static final int PROP_ID_overrideSafetyStock = 12;
    
    /* 计算时间: LAST_CALCULATED_AT DATETIME */
    public static final String PROP_NAME_lastCalculatedAt = "lastCalculatedAt";
    public static final int PROP_ID_lastCalculatedAt = 13;
    
    /* 覆盖人: OVERWRITTEN_BY VARCHAR */
    public static final String PROP_NAME_overwrittenBy = "overwrittenBy";
    public static final int PROP_ID_overwrittenBy = 14;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 15;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 16;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 17;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 18;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 19;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 20;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 21;
    

    private static int _PROP_ID_BOUND = 22;

    
    /* relation:  */
    public static final String PROP_NAME_material = "material";
    
    /* relation:  */
    public static final String PROP_NAME_warehouse = "warehouse";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[22];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_materialId] = PROP_NAME_materialId;
          PROP_NAME_TO_ID.put(PROP_NAME_materialId, PROP_ID_materialId);
      
          PROP_ID_TO_NAME[PROP_ID_warehouseId] = PROP_NAME_warehouseId;
          PROP_NAME_TO_ID.put(PROP_NAME_warehouseId, PROP_ID_warehouseId);
      
          PROP_ID_TO_NAME[PROP_ID_method] = PROP_NAME_method;
          PROP_NAME_TO_ID.put(PROP_NAME_method, PROP_ID_method);
      
          PROP_ID_TO_NAME[PROP_ID_serviceLevel] = PROP_NAME_serviceLevel;
          PROP_NAME_TO_ID.put(PROP_NAME_serviceLevel, PROP_ID_serviceLevel);
      
          PROP_ID_TO_NAME[PROP_ID_historyMonths] = PROP_NAME_historyMonths;
          PROP_NAME_TO_ID.put(PROP_NAME_historyMonths, PROP_ID_historyMonths);
      
          PROP_ID_TO_NAME[PROP_ID_leadTimeDays] = PROP_NAME_leadTimeDays;
          PROP_NAME_TO_ID.put(PROP_NAME_leadTimeDays, PROP_ID_leadTimeDays);
      
          PROP_ID_TO_NAME[PROP_ID_calculatedSafetyStock] = PROP_NAME_calculatedSafetyStock;
          PROP_NAME_TO_ID.put(PROP_NAME_calculatedSafetyStock, PROP_ID_calculatedSafetyStock);
      
          PROP_ID_TO_NAME[PROP_ID_calculatedRop] = PROP_NAME_calculatedRop;
          PROP_NAME_TO_ID.put(PROP_NAME_calculatedRop, PROP_ID_calculatedRop);
      
          PROP_ID_TO_NAME[PROP_ID_overrideSafetyStock] = PROP_NAME_overrideSafetyStock;
          PROP_NAME_TO_ID.put(PROP_NAME_overrideSafetyStock, PROP_ID_overrideSafetyStock);
      
          PROP_ID_TO_NAME[PROP_ID_lastCalculatedAt] = PROP_NAME_lastCalculatedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_lastCalculatedAt, PROP_ID_lastCalculatedAt);
      
          PROP_ID_TO_NAME[PROP_ID_overwrittenBy] = PROP_NAME_overwrittenBy;
          PROP_NAME_TO_ID.put(PROP_NAME_overwrittenBy, PROP_ID_overwrittenBy);
      
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
    
    /* 编号: CODE */
    private java.lang.String _code;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 物料: MATERIAL_ID */
    private java.lang.Long _materialId;
    
    /* 仓库: WAREHOUSE_ID */
    private java.lang.Long _warehouseId;
    
    /* 计算方法: METHOD */
    private java.lang.Integer _method;
    
    /* 服务水平: SERVICE_LEVEL */
    private java.lang.Integer _serviceLevel;
    
    /* 分析月数: HISTORY_MONTHS */
    private java.lang.Integer _historyMonths;
    
    /* 提前期(天): LEAD_TIME_DAYS */
    private java.lang.Integer _leadTimeDays;
    
    /* 建议安全库存: CALCULATED_SAFETY_STOCK */
    private java.math.BigDecimal _calculatedSafetyStock;
    
    /* 建议再订货点: CALCULATED_ROP */
    private java.math.BigDecimal _calculatedRop;
    
    /* 覆盖安全库存: OVERRIDE_SAFETY_STOCK */
    private java.math.BigDecimal _overrideSafetyStock;
    
    /* 计算时间: LAST_CALCULATED_AT */
    private java.time.LocalDateTime _lastCalculatedAt;
    
    /* 覆盖人: OVERWRITTEN_BY */
    private java.lang.String _overwrittenBy;
    
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
    

    public _ErpInvDrpSafetyStockCalc(){
        // for debug
    }

    protected ErpInvDrpSafetyStockCalc newInstance(){
        ErpInvDrpSafetyStockCalc entity = new ErpInvDrpSafetyStockCalc();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpInvDrpSafetyStockCalc cloneInstance() {
        ErpInvDrpSafetyStockCalc entity = newInstance();
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
      return "app.erp.drp.dao.entity.ErpInvDrpSafetyStockCalc";
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
        
            case PROP_ID_materialId:
               return getMaterialId();
        
            case PROP_ID_warehouseId:
               return getWarehouseId();
        
            case PROP_ID_method:
               return getMethod();
        
            case PROP_ID_serviceLevel:
               return getServiceLevel();
        
            case PROP_ID_historyMonths:
               return getHistoryMonths();
        
            case PROP_ID_leadTimeDays:
               return getLeadTimeDays();
        
            case PROP_ID_calculatedSafetyStock:
               return getCalculatedSafetyStock();
        
            case PROP_ID_calculatedRop:
               return getCalculatedRop();
        
            case PROP_ID_overrideSafetyStock:
               return getOverrideSafetyStock();
        
            case PROP_ID_lastCalculatedAt:
               return getLastCalculatedAt();
        
            case PROP_ID_overwrittenBy:
               return getOverwrittenBy();
        
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
        
            case PROP_ID_method:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_method));
               }
               setMethod(typedValue);
               break;
            }
        
            case PROP_ID_serviceLevel:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_serviceLevel));
               }
               setServiceLevel(typedValue);
               break;
            }
        
            case PROP_ID_historyMonths:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_historyMonths));
               }
               setHistoryMonths(typedValue);
               break;
            }
        
            case PROP_ID_leadTimeDays:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_leadTimeDays));
               }
               setLeadTimeDays(typedValue);
               break;
            }
        
            case PROP_ID_calculatedSafetyStock:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_calculatedSafetyStock));
               }
               setCalculatedSafetyStock(typedValue);
               break;
            }
        
            case PROP_ID_calculatedRop:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_calculatedRop));
               }
               setCalculatedRop(typedValue);
               break;
            }
        
            case PROP_ID_overrideSafetyStock:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_overrideSafetyStock));
               }
               setOverrideSafetyStock(typedValue);
               break;
            }
        
            case PROP_ID_lastCalculatedAt:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_lastCalculatedAt));
               }
               setLastCalculatedAt(typedValue);
               break;
            }
        
            case PROP_ID_overwrittenBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_overwrittenBy));
               }
               setOverwrittenBy(typedValue);
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
        
            case PROP_ID_method:{
               onInitProp(propId);
               this._method = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_serviceLevel:{
               onInitProp(propId);
               this._serviceLevel = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_historyMonths:{
               onInitProp(propId);
               this._historyMonths = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_leadTimeDays:{
               onInitProp(propId);
               this._leadTimeDays = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_calculatedSafetyStock:{
               onInitProp(propId);
               this._calculatedSafetyStock = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_calculatedRop:{
               onInitProp(propId);
               this._calculatedRop = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_overrideSafetyStock:{
               onInitProp(propId);
               this._overrideSafetyStock = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_lastCalculatedAt:{
               onInitProp(propId);
               this._lastCalculatedAt = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_overwrittenBy:{
               onInitProp(propId);
               this._overwrittenBy = (java.lang.String)value;
               
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
     * 编号: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 编号: CODE
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
     * 仓库: WAREHOUSE_ID
     */
    public final java.lang.Long getWarehouseId(){
         onPropGet(PROP_ID_warehouseId);
         return _warehouseId;
    }

    /**
     * 仓库: WAREHOUSE_ID
     */
    public final void setWarehouseId(java.lang.Long value){
        if(onPropSet(PROP_ID_warehouseId,value)){
            this._warehouseId = value;
            internalClearRefs(PROP_ID_warehouseId);
            
        }
    }
    
    /**
     * 计算方法: METHOD
     */
    public final java.lang.Integer getMethod(){
         onPropGet(PROP_ID_method);
         return _method;
    }

    /**
     * 计算方法: METHOD
     */
    public final void setMethod(java.lang.Integer value){
        if(onPropSet(PROP_ID_method,value)){
            this._method = value;
            internalClearRefs(PROP_ID_method);
            
        }
    }
    
    /**
     * 服务水平: SERVICE_LEVEL
     */
    public final java.lang.Integer getServiceLevel(){
         onPropGet(PROP_ID_serviceLevel);
         return _serviceLevel;
    }

    /**
     * 服务水平: SERVICE_LEVEL
     */
    public final void setServiceLevel(java.lang.Integer value){
        if(onPropSet(PROP_ID_serviceLevel,value)){
            this._serviceLevel = value;
            internalClearRefs(PROP_ID_serviceLevel);
            
        }
    }
    
    /**
     * 分析月数: HISTORY_MONTHS
     */
    public final java.lang.Integer getHistoryMonths(){
         onPropGet(PROP_ID_historyMonths);
         return _historyMonths;
    }

    /**
     * 分析月数: HISTORY_MONTHS
     */
    public final void setHistoryMonths(java.lang.Integer value){
        if(onPropSet(PROP_ID_historyMonths,value)){
            this._historyMonths = value;
            internalClearRefs(PROP_ID_historyMonths);
            
        }
    }
    
    /**
     * 提前期(天): LEAD_TIME_DAYS
     */
    public final java.lang.Integer getLeadTimeDays(){
         onPropGet(PROP_ID_leadTimeDays);
         return _leadTimeDays;
    }

    /**
     * 提前期(天): LEAD_TIME_DAYS
     */
    public final void setLeadTimeDays(java.lang.Integer value){
        if(onPropSet(PROP_ID_leadTimeDays,value)){
            this._leadTimeDays = value;
            internalClearRefs(PROP_ID_leadTimeDays);
            
        }
    }
    
    /**
     * 建议安全库存: CALCULATED_SAFETY_STOCK
     */
    public final java.math.BigDecimal getCalculatedSafetyStock(){
         onPropGet(PROP_ID_calculatedSafetyStock);
         return _calculatedSafetyStock;
    }

    /**
     * 建议安全库存: CALCULATED_SAFETY_STOCK
     */
    public final void setCalculatedSafetyStock(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_calculatedSafetyStock,value)){
            this._calculatedSafetyStock = value;
            internalClearRefs(PROP_ID_calculatedSafetyStock);
            
        }
    }
    
    /**
     * 建议再订货点: CALCULATED_ROP
     */
    public final java.math.BigDecimal getCalculatedRop(){
         onPropGet(PROP_ID_calculatedRop);
         return _calculatedRop;
    }

    /**
     * 建议再订货点: CALCULATED_ROP
     */
    public final void setCalculatedRop(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_calculatedRop,value)){
            this._calculatedRop = value;
            internalClearRefs(PROP_ID_calculatedRop);
            
        }
    }
    
    /**
     * 覆盖安全库存: OVERRIDE_SAFETY_STOCK
     */
    public final java.math.BigDecimal getOverrideSafetyStock(){
         onPropGet(PROP_ID_overrideSafetyStock);
         return _overrideSafetyStock;
    }

    /**
     * 覆盖安全库存: OVERRIDE_SAFETY_STOCK
     */
    public final void setOverrideSafetyStock(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_overrideSafetyStock,value)){
            this._overrideSafetyStock = value;
            internalClearRefs(PROP_ID_overrideSafetyStock);
            
        }
    }
    
    /**
     * 计算时间: LAST_CALCULATED_AT
     */
    public final java.time.LocalDateTime getLastCalculatedAt(){
         onPropGet(PROP_ID_lastCalculatedAt);
         return _lastCalculatedAt;
    }

    /**
     * 计算时间: LAST_CALCULATED_AT
     */
    public final void setLastCalculatedAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_lastCalculatedAt,value)){
            this._lastCalculatedAt = value;
            internalClearRefs(PROP_ID_lastCalculatedAt);
            
        }
    }
    
    /**
     * 覆盖人: OVERWRITTEN_BY
     */
    public final java.lang.String getOverwrittenBy(){
         onPropGet(PROP_ID_overwrittenBy);
         return _overwrittenBy;
    }

    /**
     * 覆盖人: OVERWRITTEN_BY
     */
    public final void setOverwrittenBy(java.lang.String value){
        if(onPropSet(PROP_ID_overwrittenBy,value)){
            this._overwrittenBy = value;
            internalClearRefs(PROP_ID_overwrittenBy);
            
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
    public final app.erp.md.dao.entity.ErpMdMaterial getMaterial(){
       return (app.erp.md.dao.entity.ErpMdMaterial)internalGetRefEntity(PROP_NAME_material);
    }

    public final void setMaterial(app.erp.md.dao.entity.ErpMdMaterial refEntity){
   
           if(refEntity == null){
           
                   this.setMaterialId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_material, refEntity,()->{
           
                           this.setMaterialId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdWarehouse getWarehouse(){
       return (app.erp.md.dao.entity.ErpMdWarehouse)internalGetRefEntity(PROP_NAME_warehouse);
    }

    public final void setWarehouse(app.erp.md.dao.entity.ErpMdWarehouse refEntity){
   
           if(refEntity == null){
           
                   this.setWarehouseId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_warehouse, refEntity,()->{
           
                           this.setWarehouseId(refEntity.getId());
                       
           });
           }
       
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
       
}
// resume CPD analysis - CPD-ON
