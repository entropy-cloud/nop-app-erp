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

import app.erp.drp.dao.entity.ErpDrpParameter;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  仓库补货参数: erp_drp_parameter
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpDrpParameter extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 仓库: WAREHOUSE_ID BIGINT */
    public static final String PROP_NAME_warehouseId = "warehouseId";
    public static final int PROP_ID_warehouseId = 2;
    
    /* 物料: MATERIAL_ID BIGINT */
    public static final String PROP_NAME_materialId = "materialId";
    public static final int PROP_ID_materialId = 3;
    
    /* 安全库存: SAFETY_STOCK DECIMAL */
    public static final String PROP_NAME_safetyStock = "safetyStock";
    public static final int PROP_ID_safetyStock = 4;
    
    /* 补货提前期(天): REPLENISHMENT_LEAD_TIME INTEGER */
    public static final String PROP_NAME_replenishmentLeadTime = "replenishmentLeadTime";
    public static final int PROP_ID_replenishmentLeadTime = 5;
    
    /* 订货倍数: ORDER_MULTIPLE DECIMAL */
    public static final String PROP_NAME_orderMultiple = "orderMultiple";
    public static final int PROP_ID_orderMultiple = 6;
    
    /* 首选调出仓库: PREFERRED_SOURCE_WAREHOUSE_ID BIGINT */
    public static final String PROP_NAME_preferredSourceWarehouseId = "preferredSourceWarehouseId";
    public static final int PROP_ID_preferredSourceWarehouseId = 7;
    
    /* 首选供应商: PREFERRED_SUPPLIER_ID BIGINT */
    public static final String PROP_NAME_preferredSupplierId = "preferredSupplierId";
    public static final int PROP_ID_preferredSupplierId = 8;
    
    /* 补货方法: REPLENISHMENT_METHOD INTEGER */
    public static final String PROP_NAME_replenishmentMethod = "replenishmentMethod";
    public static final int PROP_ID_replenishmentMethod = 9;
    
    /* 最低库存: MIN_STOCK_LEVEL DECIMAL */
    public static final String PROP_NAME_minStockLevel = "minStockLevel";
    public static final int PROP_ID_minStockLevel = 10;
    
    /* 最高库存: MAX_STOCK_LEVEL DECIMAL */
    public static final String PROP_NAME_maxStockLevel = "maxStockLevel";
    public static final int PROP_ID_maxStockLevel = 11;
    
    /* 审视周期(天): REVIEW_PERIOD_DAYS INTEGER */
    public static final String PROP_NAME_reviewPeriodDays = "reviewPeriodDays";
    public static final int PROP_ID_reviewPeriodDays = 12;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 13;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 14;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 15;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 16;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 17;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 18;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 19;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 20;
    

    private static int _PROP_ID_BOUND = 21;

    
    /* relation:  */
    public static final String PROP_NAME_warehouse = "warehouse";
    
    /* relation:  */
    public static final String PROP_NAME_material = "material";
    
    /* relation:  */
    public static final String PROP_NAME_preferredSourceWarehouse = "preferredSourceWarehouse";
    
    /* relation:  */
    public static final String PROP_NAME_preferredSupplier = "preferredSupplier";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[21];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_warehouseId] = PROP_NAME_warehouseId;
          PROP_NAME_TO_ID.put(PROP_NAME_warehouseId, PROP_ID_warehouseId);
      
          PROP_ID_TO_NAME[PROP_ID_materialId] = PROP_NAME_materialId;
          PROP_NAME_TO_ID.put(PROP_NAME_materialId, PROP_ID_materialId);
      
          PROP_ID_TO_NAME[PROP_ID_safetyStock] = PROP_NAME_safetyStock;
          PROP_NAME_TO_ID.put(PROP_NAME_safetyStock, PROP_ID_safetyStock);
      
          PROP_ID_TO_NAME[PROP_ID_replenishmentLeadTime] = PROP_NAME_replenishmentLeadTime;
          PROP_NAME_TO_ID.put(PROP_NAME_replenishmentLeadTime, PROP_ID_replenishmentLeadTime);
      
          PROP_ID_TO_NAME[PROP_ID_orderMultiple] = PROP_NAME_orderMultiple;
          PROP_NAME_TO_ID.put(PROP_NAME_orderMultiple, PROP_ID_orderMultiple);
      
          PROP_ID_TO_NAME[PROP_ID_preferredSourceWarehouseId] = PROP_NAME_preferredSourceWarehouseId;
          PROP_NAME_TO_ID.put(PROP_NAME_preferredSourceWarehouseId, PROP_ID_preferredSourceWarehouseId);
      
          PROP_ID_TO_NAME[PROP_ID_preferredSupplierId] = PROP_NAME_preferredSupplierId;
          PROP_NAME_TO_ID.put(PROP_NAME_preferredSupplierId, PROP_ID_preferredSupplierId);
      
          PROP_ID_TO_NAME[PROP_ID_replenishmentMethod] = PROP_NAME_replenishmentMethod;
          PROP_NAME_TO_ID.put(PROP_NAME_replenishmentMethod, PROP_ID_replenishmentMethod);
      
          PROP_ID_TO_NAME[PROP_ID_minStockLevel] = PROP_NAME_minStockLevel;
          PROP_NAME_TO_ID.put(PROP_NAME_minStockLevel, PROP_ID_minStockLevel);
      
          PROP_ID_TO_NAME[PROP_ID_maxStockLevel] = PROP_NAME_maxStockLevel;
          PROP_NAME_TO_ID.put(PROP_NAME_maxStockLevel, PROP_ID_maxStockLevel);
      
          PROP_ID_TO_NAME[PROP_ID_reviewPeriodDays] = PROP_NAME_reviewPeriodDays;
          PROP_NAME_TO_ID.put(PROP_NAME_reviewPeriodDays, PROP_ID_reviewPeriodDays);
      
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
    
    /* 仓库: WAREHOUSE_ID */
    private java.lang.Long _warehouseId;
    
    /* 物料: MATERIAL_ID */
    private java.lang.Long _materialId;
    
    /* 安全库存: SAFETY_STOCK */
    private java.math.BigDecimal _safetyStock;
    
    /* 补货提前期(天): REPLENISHMENT_LEAD_TIME */
    private java.lang.Integer _replenishmentLeadTime;
    
    /* 订货倍数: ORDER_MULTIPLE */
    private java.math.BigDecimal _orderMultiple;
    
    /* 首选调出仓库: PREFERRED_SOURCE_WAREHOUSE_ID */
    private java.lang.Long _preferredSourceWarehouseId;
    
    /* 首选供应商: PREFERRED_SUPPLIER_ID */
    private java.lang.Long _preferredSupplierId;
    
    /* 补货方法: REPLENISHMENT_METHOD */
    private java.lang.Integer _replenishmentMethod;
    
    /* 最低库存: MIN_STOCK_LEVEL */
    private java.math.BigDecimal _minStockLevel;
    
    /* 最高库存: MAX_STOCK_LEVEL */
    private java.math.BigDecimal _maxStockLevel;
    
    /* 审视周期(天): REVIEW_PERIOD_DAYS */
    private java.lang.Integer _reviewPeriodDays;
    
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
    

    public _ErpDrpParameter(){
        // for debug
    }

    protected ErpDrpParameter newInstance(){
        ErpDrpParameter entity = new ErpDrpParameter();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpDrpParameter cloneInstance() {
        ErpDrpParameter entity = newInstance();
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
      return "app.erp.drp.dao.entity.ErpDrpParameter";
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
        
            case PROP_ID_warehouseId:
               return getWarehouseId();
        
            case PROP_ID_materialId:
               return getMaterialId();
        
            case PROP_ID_safetyStock:
               return getSafetyStock();
        
            case PROP_ID_replenishmentLeadTime:
               return getReplenishmentLeadTime();
        
            case PROP_ID_orderMultiple:
               return getOrderMultiple();
        
            case PROP_ID_preferredSourceWarehouseId:
               return getPreferredSourceWarehouseId();
        
            case PROP_ID_preferredSupplierId:
               return getPreferredSupplierId();
        
            case PROP_ID_replenishmentMethod:
               return getReplenishmentMethod();
        
            case PROP_ID_minStockLevel:
               return getMinStockLevel();
        
            case PROP_ID_maxStockLevel:
               return getMaxStockLevel();
        
            case PROP_ID_reviewPeriodDays:
               return getReviewPeriodDays();
        
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
        
            case PROP_ID_warehouseId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_warehouseId));
               }
               setWarehouseId(typedValue);
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
        
            case PROP_ID_safetyStock:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_safetyStock));
               }
               setSafetyStock(typedValue);
               break;
            }
        
            case PROP_ID_replenishmentLeadTime:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_replenishmentLeadTime));
               }
               setReplenishmentLeadTime(typedValue);
               break;
            }
        
            case PROP_ID_orderMultiple:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_orderMultiple));
               }
               setOrderMultiple(typedValue);
               break;
            }
        
            case PROP_ID_preferredSourceWarehouseId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_preferredSourceWarehouseId));
               }
               setPreferredSourceWarehouseId(typedValue);
               break;
            }
        
            case PROP_ID_preferredSupplierId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_preferredSupplierId));
               }
               setPreferredSupplierId(typedValue);
               break;
            }
        
            case PROP_ID_replenishmentMethod:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_replenishmentMethod));
               }
               setReplenishmentMethod(typedValue);
               break;
            }
        
            case PROP_ID_minStockLevel:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_minStockLevel));
               }
               setMinStockLevel(typedValue);
               break;
            }
        
            case PROP_ID_maxStockLevel:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_maxStockLevel));
               }
               setMaxStockLevel(typedValue);
               break;
            }
        
            case PROP_ID_reviewPeriodDays:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_reviewPeriodDays));
               }
               setReviewPeriodDays(typedValue);
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
        
            case PROP_ID_warehouseId:{
               onInitProp(propId);
               this._warehouseId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_materialId:{
               onInitProp(propId);
               this._materialId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_safetyStock:{
               onInitProp(propId);
               this._safetyStock = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_replenishmentLeadTime:{
               onInitProp(propId);
               this._replenishmentLeadTime = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_orderMultiple:{
               onInitProp(propId);
               this._orderMultiple = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_preferredSourceWarehouseId:{
               onInitProp(propId);
               this._preferredSourceWarehouseId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_preferredSupplierId:{
               onInitProp(propId);
               this._preferredSupplierId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_replenishmentMethod:{
               onInitProp(propId);
               this._replenishmentMethod = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_minStockLevel:{
               onInitProp(propId);
               this._minStockLevel = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_maxStockLevel:{
               onInitProp(propId);
               this._maxStockLevel = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_reviewPeriodDays:{
               onInitProp(propId);
               this._reviewPeriodDays = (java.lang.Integer)value;
               
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
     * 补货提前期(天): REPLENISHMENT_LEAD_TIME
     */
    public final java.lang.Integer getReplenishmentLeadTime(){
         onPropGet(PROP_ID_replenishmentLeadTime);
         return _replenishmentLeadTime;
    }

    /**
     * 补货提前期(天): REPLENISHMENT_LEAD_TIME
     */
    public final void setReplenishmentLeadTime(java.lang.Integer value){
        if(onPropSet(PROP_ID_replenishmentLeadTime,value)){
            this._replenishmentLeadTime = value;
            internalClearRefs(PROP_ID_replenishmentLeadTime);
            
        }
    }
    
    /**
     * 订货倍数: ORDER_MULTIPLE
     */
    public final java.math.BigDecimal getOrderMultiple(){
         onPropGet(PROP_ID_orderMultiple);
         return _orderMultiple;
    }

    /**
     * 订货倍数: ORDER_MULTIPLE
     */
    public final void setOrderMultiple(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_orderMultiple,value)){
            this._orderMultiple = value;
            internalClearRefs(PROP_ID_orderMultiple);
            
        }
    }
    
    /**
     * 首选调出仓库: PREFERRED_SOURCE_WAREHOUSE_ID
     */
    public final java.lang.Long getPreferredSourceWarehouseId(){
         onPropGet(PROP_ID_preferredSourceWarehouseId);
         return _preferredSourceWarehouseId;
    }

    /**
     * 首选调出仓库: PREFERRED_SOURCE_WAREHOUSE_ID
     */
    public final void setPreferredSourceWarehouseId(java.lang.Long value){
        if(onPropSet(PROP_ID_preferredSourceWarehouseId,value)){
            this._preferredSourceWarehouseId = value;
            internalClearRefs(PROP_ID_preferredSourceWarehouseId);
            
        }
    }
    
    /**
     * 首选供应商: PREFERRED_SUPPLIER_ID
     */
    public final java.lang.Long getPreferredSupplierId(){
         onPropGet(PROP_ID_preferredSupplierId);
         return _preferredSupplierId;
    }

    /**
     * 首选供应商: PREFERRED_SUPPLIER_ID
     */
    public final void setPreferredSupplierId(java.lang.Long value){
        if(onPropSet(PROP_ID_preferredSupplierId,value)){
            this._preferredSupplierId = value;
            internalClearRefs(PROP_ID_preferredSupplierId);
            
        }
    }
    
    /**
     * 补货方法: REPLENISHMENT_METHOD
     */
    public final java.lang.Integer getReplenishmentMethod(){
         onPropGet(PROP_ID_replenishmentMethod);
         return _replenishmentMethod;
    }

    /**
     * 补货方法: REPLENISHMENT_METHOD
     */
    public final void setReplenishmentMethod(java.lang.Integer value){
        if(onPropSet(PROP_ID_replenishmentMethod,value)){
            this._replenishmentMethod = value;
            internalClearRefs(PROP_ID_replenishmentMethod);
            
        }
    }
    
    /**
     * 最低库存: MIN_STOCK_LEVEL
     */
    public final java.math.BigDecimal getMinStockLevel(){
         onPropGet(PROP_ID_minStockLevel);
         return _minStockLevel;
    }

    /**
     * 最低库存: MIN_STOCK_LEVEL
     */
    public final void setMinStockLevel(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_minStockLevel,value)){
            this._minStockLevel = value;
            internalClearRefs(PROP_ID_minStockLevel);
            
        }
    }
    
    /**
     * 最高库存: MAX_STOCK_LEVEL
     */
    public final java.math.BigDecimal getMaxStockLevel(){
         onPropGet(PROP_ID_maxStockLevel);
         return _maxStockLevel;
    }

    /**
     * 最高库存: MAX_STOCK_LEVEL
     */
    public final void setMaxStockLevel(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_maxStockLevel,value)){
            this._maxStockLevel = value;
            internalClearRefs(PROP_ID_maxStockLevel);
            
        }
    }
    
    /**
     * 审视周期(天): REVIEW_PERIOD_DAYS
     */
    public final java.lang.Integer getReviewPeriodDays(){
         onPropGet(PROP_ID_reviewPeriodDays);
         return _reviewPeriodDays;
    }

    /**
     * 审视周期(天): REVIEW_PERIOD_DAYS
     */
    public final void setReviewPeriodDays(java.lang.Integer value){
        if(onPropSet(PROP_ID_reviewPeriodDays,value)){
            this._reviewPeriodDays = value;
            internalClearRefs(PROP_ID_reviewPeriodDays);
            
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
    public final app.erp.md.dao.entity.ErpMdWarehouse getPreferredSourceWarehouse(){
       return (app.erp.md.dao.entity.ErpMdWarehouse)internalGetRefEntity(PROP_NAME_preferredSourceWarehouse);
    }

    public final void setPreferredSourceWarehouse(app.erp.md.dao.entity.ErpMdWarehouse refEntity){
   
           if(refEntity == null){
           
                   this.setPreferredSourceWarehouseId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_preferredSourceWarehouse, refEntity,()->{
           
                           this.setPreferredSourceWarehouseId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdPartner getPreferredSupplier(){
       return (app.erp.md.dao.entity.ErpMdPartner)internalGetRefEntity(PROP_NAME_preferredSupplier);
    }

    public final void setPreferredSupplier(app.erp.md.dao.entity.ErpMdPartner refEntity){
   
           if(refEntity == null){
           
                   this.setPreferredSupplierId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_preferredSupplier, refEntity,()->{
           
                           this.setPreferredSupplierId(refEntity.getId());
                       
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
