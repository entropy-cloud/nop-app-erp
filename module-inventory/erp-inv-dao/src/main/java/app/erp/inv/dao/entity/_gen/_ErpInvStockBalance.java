package app.erp.inv.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.inv.dao.entity.ErpInvStockBalance;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  库存余额: erp_inv_stock_balance
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpInvStockBalance extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 2;
    
    /* 物料: MATERIAL_ID BIGINT */
    public static final String PROP_NAME_materialId = "materialId";
    public static final int PROP_ID_materialId = 3;
    
    /* SKU: SKU_ID BIGINT */
    public static final String PROP_NAME_skuId = "skuId";
    public static final int PROP_ID_skuId = 4;
    
    /* 仓库: WAREHOUSE_ID BIGINT */
    public static final String PROP_NAME_warehouseId = "warehouseId";
    public static final int PROP_ID_warehouseId = 5;
    
    /* 库位: LOCATION_ID BIGINT */
    public static final String PROP_NAME_locationId = "locationId";
    public static final int PROP_ID_locationId = 6;
    
    /* 批号: BATCH_NO VARCHAR */
    public static final String PROP_NAME_batchNo = "batchNo";
    public static final int PROP_ID_batchNo = 7;
    
    /* 总数量: TOTAL_QUANTITY DECIMAL */
    public static final String PROP_NAME_totalQuantity = "totalQuantity";
    public static final int PROP_ID_totalQuantity = 8;
    
    /* 预留数量: RESERVED_QUANTITY DECIMAL */
    public static final String PROP_NAME_reservedQuantity = "reservedQuantity";
    public static final int PROP_ID_reservedQuantity = 9;
    
    /* 冻结数量: LOCKED_QUANTITY DECIMAL */
    public static final String PROP_NAME_lockedQuantity = "lockedQuantity";
    public static final int PROP_ID_lockedQuantity = 10;
    
    /* 可用数量: AVAILABLE_QUANTITY DECIMAL */
    public static final String PROP_NAME_availableQuantity = "availableQuantity";
    public static final int PROP_ID_availableQuantity = 11;
    
    /* 计价方法: COST_METHOD INTEGER */
    public static final String PROP_NAME_costMethod = "costMethod";
    public static final int PROP_ID_costMethod = 12;
    
    /* 平均成本: AVG_COST DECIMAL */
    public static final String PROP_NAME_avgCost = "avgCost";
    public static final int PROP_ID_avgCost = 13;
    
    /* 总成本: TOTAL_COST DECIMAL */
    public static final String PROP_NAME_totalCost = "totalCost";
    public static final int PROP_ID_totalCost = 14;
    
    /* 币种: CURRENCY_ID BIGINT */
    public static final String PROP_NAME_currencyId = "currencyId";
    public static final int PROP_ID_currencyId = 15;
    
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
    public static final String PROP_NAME_sku = "sku";
    
    /* relation:  */
    public static final String PROP_NAME_warehouse = "warehouse";
    
    /* relation:  */
    public static final String PROP_NAME_currency = "currency";
    
    /* relation:  */
    public static final String PROP_NAME_location = "location";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[22];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_materialId] = PROP_NAME_materialId;
          PROP_NAME_TO_ID.put(PROP_NAME_materialId, PROP_ID_materialId);
      
          PROP_ID_TO_NAME[PROP_ID_skuId] = PROP_NAME_skuId;
          PROP_NAME_TO_ID.put(PROP_NAME_skuId, PROP_ID_skuId);
      
          PROP_ID_TO_NAME[PROP_ID_warehouseId] = PROP_NAME_warehouseId;
          PROP_NAME_TO_ID.put(PROP_NAME_warehouseId, PROP_ID_warehouseId);
      
          PROP_ID_TO_NAME[PROP_ID_locationId] = PROP_NAME_locationId;
          PROP_NAME_TO_ID.put(PROP_NAME_locationId, PROP_ID_locationId);
      
          PROP_ID_TO_NAME[PROP_ID_batchNo] = PROP_NAME_batchNo;
          PROP_NAME_TO_ID.put(PROP_NAME_batchNo, PROP_ID_batchNo);
      
          PROP_ID_TO_NAME[PROP_ID_totalQuantity] = PROP_NAME_totalQuantity;
          PROP_NAME_TO_ID.put(PROP_NAME_totalQuantity, PROP_ID_totalQuantity);
      
          PROP_ID_TO_NAME[PROP_ID_reservedQuantity] = PROP_NAME_reservedQuantity;
          PROP_NAME_TO_ID.put(PROP_NAME_reservedQuantity, PROP_ID_reservedQuantity);
      
          PROP_ID_TO_NAME[PROP_ID_lockedQuantity] = PROP_NAME_lockedQuantity;
          PROP_NAME_TO_ID.put(PROP_NAME_lockedQuantity, PROP_ID_lockedQuantity);
      
          PROP_ID_TO_NAME[PROP_ID_availableQuantity] = PROP_NAME_availableQuantity;
          PROP_NAME_TO_ID.put(PROP_NAME_availableQuantity, PROP_ID_availableQuantity);
      
          PROP_ID_TO_NAME[PROP_ID_costMethod] = PROP_NAME_costMethod;
          PROP_NAME_TO_ID.put(PROP_NAME_costMethod, PROP_ID_costMethod);
      
          PROP_ID_TO_NAME[PROP_ID_avgCost] = PROP_NAME_avgCost;
          PROP_NAME_TO_ID.put(PROP_NAME_avgCost, PROP_ID_avgCost);
      
          PROP_ID_TO_NAME[PROP_ID_totalCost] = PROP_NAME_totalCost;
          PROP_NAME_TO_ID.put(PROP_NAME_totalCost, PROP_ID_totalCost);
      
          PROP_ID_TO_NAME[PROP_ID_currencyId] = PROP_NAME_currencyId;
          PROP_NAME_TO_ID.put(PROP_NAME_currencyId, PROP_ID_currencyId);
      
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
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 物料: MATERIAL_ID */
    private java.lang.Long _materialId;
    
    /* SKU: SKU_ID */
    private java.lang.Long _skuId;
    
    /* 仓库: WAREHOUSE_ID */
    private java.lang.Long _warehouseId;
    
    /* 库位: LOCATION_ID */
    private java.lang.Long _locationId;
    
    /* 批号: BATCH_NO */
    private java.lang.String _batchNo;
    
    /* 总数量: TOTAL_QUANTITY */
    private java.lang.String _totalQuantity;
    
    /* 预留数量: RESERVED_QUANTITY */
    private java.lang.String _reservedQuantity;
    
    /* 冻结数量: LOCKED_QUANTITY */
    private java.lang.String _lockedQuantity;
    
    /* 可用数量: AVAILABLE_QUANTITY */
    private java.lang.String _availableQuantity;
    
    /* 计价方法: COST_METHOD */
    private java.lang.Integer _costMethod;
    
    /* 平均成本: AVG_COST */
    private java.lang.String _avgCost;
    
    /* 总成本: TOTAL_COST */
    private java.lang.String _totalCost;
    
    /* 币种: CURRENCY_ID */
    private java.lang.Long _currencyId;
    
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
    

    public _ErpInvStockBalance(){
        // for debug
    }

    protected ErpInvStockBalance newInstance(){
        ErpInvStockBalance entity = new ErpInvStockBalance();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpInvStockBalance cloneInstance() {
        ErpInvStockBalance entity = newInstance();
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
      return "app.erp.inv.dao.entity.ErpInvStockBalance";
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
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_materialId:
               return getMaterialId();
        
            case PROP_ID_skuId:
               return getSkuId();
        
            case PROP_ID_warehouseId:
               return getWarehouseId();
        
            case PROP_ID_locationId:
               return getLocationId();
        
            case PROP_ID_batchNo:
               return getBatchNo();
        
            case PROP_ID_totalQuantity:
               return getTotalQuantity();
        
            case PROP_ID_reservedQuantity:
               return getReservedQuantity();
        
            case PROP_ID_lockedQuantity:
               return getLockedQuantity();
        
            case PROP_ID_availableQuantity:
               return getAvailableQuantity();
        
            case PROP_ID_costMethod:
               return getCostMethod();
        
            case PROP_ID_avgCost:
               return getAvgCost();
        
            case PROP_ID_totalCost:
               return getTotalCost();
        
            case PROP_ID_currencyId:
               return getCurrencyId();
        
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
        
            case PROP_ID_skuId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_skuId));
               }
               setSkuId(typedValue);
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
        
            case PROP_ID_locationId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_locationId));
               }
               setLocationId(typedValue);
               break;
            }
        
            case PROP_ID_batchNo:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_batchNo));
               }
               setBatchNo(typedValue);
               break;
            }
        
            case PROP_ID_totalQuantity:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_totalQuantity));
               }
               setTotalQuantity(typedValue);
               break;
            }
        
            case PROP_ID_reservedQuantity:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_reservedQuantity));
               }
               setReservedQuantity(typedValue);
               break;
            }
        
            case PROP_ID_lockedQuantity:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_lockedQuantity));
               }
               setLockedQuantity(typedValue);
               break;
            }
        
            case PROP_ID_availableQuantity:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_availableQuantity));
               }
               setAvailableQuantity(typedValue);
               break;
            }
        
            case PROP_ID_costMethod:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_costMethod));
               }
               setCostMethod(typedValue);
               break;
            }
        
            case PROP_ID_avgCost:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_avgCost));
               }
               setAvgCost(typedValue);
               break;
            }
        
            case PROP_ID_totalCost:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_totalCost));
               }
               setTotalCost(typedValue);
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
        
            case PROP_ID_skuId:{
               onInitProp(propId);
               this._skuId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_warehouseId:{
               onInitProp(propId);
               this._warehouseId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_locationId:{
               onInitProp(propId);
               this._locationId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_batchNo:{
               onInitProp(propId);
               this._batchNo = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_totalQuantity:{
               onInitProp(propId);
               this._totalQuantity = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_reservedQuantity:{
               onInitProp(propId);
               this._reservedQuantity = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_lockedQuantity:{
               onInitProp(propId);
               this._lockedQuantity = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_availableQuantity:{
               onInitProp(propId);
               this._availableQuantity = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_costMethod:{
               onInitProp(propId);
               this._costMethod = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_avgCost:{
               onInitProp(propId);
               this._avgCost = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_totalCost:{
               onInitProp(propId);
               this._totalCost = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_currencyId:{
               onInitProp(propId);
               this._currencyId = (java.lang.Long)value;
               
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
     * SKU: SKU_ID
     */
    public final java.lang.Long getSkuId(){
         onPropGet(PROP_ID_skuId);
         return _skuId;
    }

    /**
     * SKU: SKU_ID
     */
    public final void setSkuId(java.lang.Long value){
        if(onPropSet(PROP_ID_skuId,value)){
            this._skuId = value;
            internalClearRefs(PROP_ID_skuId);
            
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
     * 库位: LOCATION_ID
     */
    public final java.lang.Long getLocationId(){
         onPropGet(PROP_ID_locationId);
         return _locationId;
    }

    /**
     * 库位: LOCATION_ID
     */
    public final void setLocationId(java.lang.Long value){
        if(onPropSet(PROP_ID_locationId,value)){
            this._locationId = value;
            internalClearRefs(PROP_ID_locationId);
            
        }
    }
    
    /**
     * 批号: BATCH_NO
     */
    public final java.lang.String getBatchNo(){
         onPropGet(PROP_ID_batchNo);
         return _batchNo;
    }

    /**
     * 批号: BATCH_NO
     */
    public final void setBatchNo(java.lang.String value){
        if(onPropSet(PROP_ID_batchNo,value)){
            this._batchNo = value;
            internalClearRefs(PROP_ID_batchNo);
            
        }
    }
    
    /**
     * 总数量: TOTAL_QUANTITY
     */
    public final java.lang.String getTotalQuantity(){
         onPropGet(PROP_ID_totalQuantity);
         return _totalQuantity;
    }

    /**
     * 总数量: TOTAL_QUANTITY
     */
    public final void setTotalQuantity(java.lang.String value){
        if(onPropSet(PROP_ID_totalQuantity,value)){
            this._totalQuantity = value;
            internalClearRefs(PROP_ID_totalQuantity);
            
        }
    }
    
    /**
     * 预留数量: RESERVED_QUANTITY
     */
    public final java.lang.String getReservedQuantity(){
         onPropGet(PROP_ID_reservedQuantity);
         return _reservedQuantity;
    }

    /**
     * 预留数量: RESERVED_QUANTITY
     */
    public final void setReservedQuantity(java.lang.String value){
        if(onPropSet(PROP_ID_reservedQuantity,value)){
            this._reservedQuantity = value;
            internalClearRefs(PROP_ID_reservedQuantity);
            
        }
    }
    
    /**
     * 冻结数量: LOCKED_QUANTITY
     */
    public final java.lang.String getLockedQuantity(){
         onPropGet(PROP_ID_lockedQuantity);
         return _lockedQuantity;
    }

    /**
     * 冻结数量: LOCKED_QUANTITY
     */
    public final void setLockedQuantity(java.lang.String value){
        if(onPropSet(PROP_ID_lockedQuantity,value)){
            this._lockedQuantity = value;
            internalClearRefs(PROP_ID_lockedQuantity);
            
        }
    }
    
    /**
     * 可用数量: AVAILABLE_QUANTITY
     */
    public final java.lang.String getAvailableQuantity(){
         onPropGet(PROP_ID_availableQuantity);
         return _availableQuantity;
    }

    /**
     * 可用数量: AVAILABLE_QUANTITY
     */
    public final void setAvailableQuantity(java.lang.String value){
        if(onPropSet(PROP_ID_availableQuantity,value)){
            this._availableQuantity = value;
            internalClearRefs(PROP_ID_availableQuantity);
            
        }
    }
    
    /**
     * 计价方法: COST_METHOD
     */
    public final java.lang.Integer getCostMethod(){
         onPropGet(PROP_ID_costMethod);
         return _costMethod;
    }

    /**
     * 计价方法: COST_METHOD
     */
    public final void setCostMethod(java.lang.Integer value){
        if(onPropSet(PROP_ID_costMethod,value)){
            this._costMethod = value;
            internalClearRefs(PROP_ID_costMethod);
            
        }
    }
    
    /**
     * 平均成本: AVG_COST
     */
    public final java.lang.String getAvgCost(){
         onPropGet(PROP_ID_avgCost);
         return _avgCost;
    }

    /**
     * 平均成本: AVG_COST
     */
    public final void setAvgCost(java.lang.String value){
        if(onPropSet(PROP_ID_avgCost,value)){
            this._avgCost = value;
            internalClearRefs(PROP_ID_avgCost);
            
        }
    }
    
    /**
     * 总成本: TOTAL_COST
     */
    public final java.lang.String getTotalCost(){
         onPropGet(PROP_ID_totalCost);
         return _totalCost;
    }

    /**
     * 总成本: TOTAL_COST
     */
    public final void setTotalCost(java.lang.String value){
        if(onPropSet(PROP_ID_totalCost,value)){
            this._totalCost = value;
            internalClearRefs(PROP_ID_totalCost);
            
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
    public final app.erp.md.dao.entity.ErpMdMaterialSku getSku(){
       return (app.erp.md.dao.entity.ErpMdMaterialSku)internalGetRefEntity(PROP_NAME_sku);
    }

    public final void setSku(app.erp.md.dao.entity.ErpMdMaterialSku refEntity){
   
           if(refEntity == null){
           
                   this.setSkuId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_sku, refEntity,()->{
           
                           this.setSkuId(refEntity.getId());
                       
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
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdLocation getLocation(){
       return (app.erp.md.dao.entity.ErpMdLocation)internalGetRefEntity(PROP_NAME_location);
    }

    public final void setLocation(app.erp.md.dao.entity.ErpMdLocation refEntity){
   
           if(refEntity == null){
           
                   this.setLocationId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_location, refEntity,()->{
           
                           this.setLocationId(refEntity.getId());
                       
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
