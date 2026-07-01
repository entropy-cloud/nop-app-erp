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

import app.erp.inv.dao.entity.ErpInvStockTakeLine;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  盘点单行: erp_inv_stock_take_line
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpInvStockTakeLine extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 盘点单ID: TAKE_ID BIGINT */
    public static final String PROP_NAME_takeId = "takeId";
    public static final int PROP_ID_takeId = 2;
    
    /* 行号: LINE_NO INTEGER */
    public static final String PROP_NAME_lineNo = "lineNo";
    public static final int PROP_ID_lineNo = 3;
    
    /* 物料: MATERIAL_ID BIGINT */
    public static final String PROP_NAME_materialId = "materialId";
    public static final int PROP_ID_materialId = 4;
    
    /* SKU: SKU_ID BIGINT */
    public static final String PROP_NAME_skuId = "skuId";
    public static final int PROP_ID_skuId = 5;
    
    /* 计量单位: UO_M_ID BIGINT */
    public static final String PROP_NAME_uoMId = "uoMId";
    public static final int PROP_ID_uoMId = 6;
    
    /* 库位: LOCATION_ID BIGINT */
    public static final String PROP_NAME_locationId = "locationId";
    public static final int PROP_ID_locationId = 7;
    
    /* 批号: BATCH_NO VARCHAR */
    public static final String PROP_NAME_batchNo = "batchNo";
    public static final int PROP_ID_batchNo = 8;
    
    /* 账面数量: BOOK_QUANTITY DECIMAL */
    public static final String PROP_NAME_bookQuantity = "bookQuantity";
    public static final int PROP_ID_bookQuantity = 9;
    
    /* 实盘数量: ACTUAL_QUANTITY DECIMAL */
    public static final String PROP_NAME_actualQuantity = "actualQuantity";
    public static final int PROP_ID_actualQuantity = 10;
    
    /* 差异数量: DIFFERENCE_QUANTITY DECIMAL */
    public static final String PROP_NAME_differenceQuantity = "differenceQuantity";
    public static final int PROP_ID_differenceQuantity = 11;
    
    /* 单位成本: UNIT_COST DECIMAL */
    public static final String PROP_NAME_unitCost = "unitCost";
    public static final int PROP_ID_unitCost = 12;
    
    /* 差异金额: DIFFERENCE_AMOUNT DECIMAL */
    public static final String PROP_NAME_differenceAmount = "differenceAmount";
    public static final int PROP_ID_differenceAmount = 13;
    
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
    public static final String PROP_NAME_take = "take";
    
    /* relation:  */
    public static final String PROP_NAME_material = "material";
    
    /* relation:  */
    public static final String PROP_NAME_sku = "sku";
    
    /* relation:  */
    public static final String PROP_NAME_uoM = "uoM";
    
    /* relation:  */
    public static final String PROP_NAME_location = "location";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[21];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_takeId] = PROP_NAME_takeId;
          PROP_NAME_TO_ID.put(PROP_NAME_takeId, PROP_ID_takeId);
      
          PROP_ID_TO_NAME[PROP_ID_lineNo] = PROP_NAME_lineNo;
          PROP_NAME_TO_ID.put(PROP_NAME_lineNo, PROP_ID_lineNo);
      
          PROP_ID_TO_NAME[PROP_ID_materialId] = PROP_NAME_materialId;
          PROP_NAME_TO_ID.put(PROP_NAME_materialId, PROP_ID_materialId);
      
          PROP_ID_TO_NAME[PROP_ID_skuId] = PROP_NAME_skuId;
          PROP_NAME_TO_ID.put(PROP_NAME_skuId, PROP_ID_skuId);
      
          PROP_ID_TO_NAME[PROP_ID_uoMId] = PROP_NAME_uoMId;
          PROP_NAME_TO_ID.put(PROP_NAME_uoMId, PROP_ID_uoMId);
      
          PROP_ID_TO_NAME[PROP_ID_locationId] = PROP_NAME_locationId;
          PROP_NAME_TO_ID.put(PROP_NAME_locationId, PROP_ID_locationId);
      
          PROP_ID_TO_NAME[PROP_ID_batchNo] = PROP_NAME_batchNo;
          PROP_NAME_TO_ID.put(PROP_NAME_batchNo, PROP_ID_batchNo);
      
          PROP_ID_TO_NAME[PROP_ID_bookQuantity] = PROP_NAME_bookQuantity;
          PROP_NAME_TO_ID.put(PROP_NAME_bookQuantity, PROP_ID_bookQuantity);
      
          PROP_ID_TO_NAME[PROP_ID_actualQuantity] = PROP_NAME_actualQuantity;
          PROP_NAME_TO_ID.put(PROP_NAME_actualQuantity, PROP_ID_actualQuantity);
      
          PROP_ID_TO_NAME[PROP_ID_differenceQuantity] = PROP_NAME_differenceQuantity;
          PROP_NAME_TO_ID.put(PROP_NAME_differenceQuantity, PROP_ID_differenceQuantity);
      
          PROP_ID_TO_NAME[PROP_ID_unitCost] = PROP_NAME_unitCost;
          PROP_NAME_TO_ID.put(PROP_NAME_unitCost, PROP_ID_unitCost);
      
          PROP_ID_TO_NAME[PROP_ID_differenceAmount] = PROP_NAME_differenceAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_differenceAmount, PROP_ID_differenceAmount);
      
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
    
    /* 盘点单ID: TAKE_ID */
    private java.lang.Long _takeId;
    
    /* 行号: LINE_NO */
    private java.lang.Integer _lineNo;
    
    /* 物料: MATERIAL_ID */
    private java.lang.Long _materialId;
    
    /* SKU: SKU_ID */
    private java.lang.Long _skuId;
    
    /* 计量单位: UO_M_ID */
    private java.lang.Long _uoMId;
    
    /* 库位: LOCATION_ID */
    private java.lang.Long _locationId;
    
    /* 批号: BATCH_NO */
    private java.lang.String _batchNo;
    
    /* 账面数量: BOOK_QUANTITY */
    private java.math.BigDecimal _bookQuantity;
    
    /* 实盘数量: ACTUAL_QUANTITY */
    private java.math.BigDecimal _actualQuantity;
    
    /* 差异数量: DIFFERENCE_QUANTITY */
    private java.math.BigDecimal _differenceQuantity;
    
    /* 单位成本: UNIT_COST */
    private java.math.BigDecimal _unitCost;
    
    /* 差异金额: DIFFERENCE_AMOUNT */
    private java.math.BigDecimal _differenceAmount;
    
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
    

    public _ErpInvStockTakeLine(){
        // for debug
    }

    protected ErpInvStockTakeLine newInstance(){
        ErpInvStockTakeLine entity = new ErpInvStockTakeLine();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpInvStockTakeLine cloneInstance() {
        ErpInvStockTakeLine entity = newInstance();
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
      return "app.erp.inv.dao.entity.ErpInvStockTakeLine";
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
        
            case PROP_ID_takeId:
               return getTakeId();
        
            case PROP_ID_lineNo:
               return getLineNo();
        
            case PROP_ID_materialId:
               return getMaterialId();
        
            case PROP_ID_skuId:
               return getSkuId();
        
            case PROP_ID_uoMId:
               return getUoMId();
        
            case PROP_ID_locationId:
               return getLocationId();
        
            case PROP_ID_batchNo:
               return getBatchNo();
        
            case PROP_ID_bookQuantity:
               return getBookQuantity();
        
            case PROP_ID_actualQuantity:
               return getActualQuantity();
        
            case PROP_ID_differenceQuantity:
               return getDifferenceQuantity();
        
            case PROP_ID_unitCost:
               return getUnitCost();
        
            case PROP_ID_differenceAmount:
               return getDifferenceAmount();
        
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
        
            case PROP_ID_takeId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_takeId));
               }
               setTakeId(typedValue);
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
        
            case PROP_ID_skuId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_skuId));
               }
               setSkuId(typedValue);
               break;
            }
        
            case PROP_ID_uoMId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_uoMId));
               }
               setUoMId(typedValue);
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
        
            case PROP_ID_bookQuantity:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_bookQuantity));
               }
               setBookQuantity(typedValue);
               break;
            }
        
            case PROP_ID_actualQuantity:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_actualQuantity));
               }
               setActualQuantity(typedValue);
               break;
            }
        
            case PROP_ID_differenceQuantity:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_differenceQuantity));
               }
               setDifferenceQuantity(typedValue);
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
        
            case PROP_ID_differenceAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_differenceAmount));
               }
               setDifferenceAmount(typedValue);
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
        
            case PROP_ID_takeId:{
               onInitProp(propId);
               this._takeId = (java.lang.Long)value;
               
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
        
            case PROP_ID_skuId:{
               onInitProp(propId);
               this._skuId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_uoMId:{
               onInitProp(propId);
               this._uoMId = (java.lang.Long)value;
               
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
        
            case PROP_ID_bookQuantity:{
               onInitProp(propId);
               this._bookQuantity = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_actualQuantity:{
               onInitProp(propId);
               this._actualQuantity = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_differenceQuantity:{
               onInitProp(propId);
               this._differenceQuantity = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_unitCost:{
               onInitProp(propId);
               this._unitCost = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_differenceAmount:{
               onInitProp(propId);
               this._differenceAmount = (java.math.BigDecimal)value;
               
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
     * 盘点单ID: TAKE_ID
     */
    public final java.lang.Long getTakeId(){
         onPropGet(PROP_ID_takeId);
         return _takeId;
    }

    /**
     * 盘点单ID: TAKE_ID
     */
    public final void setTakeId(java.lang.Long value){
        if(onPropSet(PROP_ID_takeId,value)){
            this._takeId = value;
            internalClearRefs(PROP_ID_takeId);
            
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
     * 计量单位: UO_M_ID
     */
    public final java.lang.Long getUoMId(){
         onPropGet(PROP_ID_uoMId);
         return _uoMId;
    }

    /**
     * 计量单位: UO_M_ID
     */
    public final void setUoMId(java.lang.Long value){
        if(onPropSet(PROP_ID_uoMId,value)){
            this._uoMId = value;
            internalClearRefs(PROP_ID_uoMId);
            
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
     * 账面数量: BOOK_QUANTITY
     */
    public final java.math.BigDecimal getBookQuantity(){
         onPropGet(PROP_ID_bookQuantity);
         return _bookQuantity;
    }

    /**
     * 账面数量: BOOK_QUANTITY
     */
    public final void setBookQuantity(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_bookQuantity,value)){
            this._bookQuantity = value;
            internalClearRefs(PROP_ID_bookQuantity);
            
        }
    }
    
    /**
     * 实盘数量: ACTUAL_QUANTITY
     */
    public final java.math.BigDecimal getActualQuantity(){
         onPropGet(PROP_ID_actualQuantity);
         return _actualQuantity;
    }

    /**
     * 实盘数量: ACTUAL_QUANTITY
     */
    public final void setActualQuantity(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_actualQuantity,value)){
            this._actualQuantity = value;
            internalClearRefs(PROP_ID_actualQuantity);
            
        }
    }
    
    /**
     * 差异数量: DIFFERENCE_QUANTITY
     */
    public final java.math.BigDecimal getDifferenceQuantity(){
         onPropGet(PROP_ID_differenceQuantity);
         return _differenceQuantity;
    }

    /**
     * 差异数量: DIFFERENCE_QUANTITY
     */
    public final void setDifferenceQuantity(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_differenceQuantity,value)){
            this._differenceQuantity = value;
            internalClearRefs(PROP_ID_differenceQuantity);
            
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
     * 差异金额: DIFFERENCE_AMOUNT
     */
    public final java.math.BigDecimal getDifferenceAmount(){
         onPropGet(PROP_ID_differenceAmount);
         return _differenceAmount;
    }

    /**
     * 差异金额: DIFFERENCE_AMOUNT
     */
    public final void setDifferenceAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_differenceAmount,value)){
            this._differenceAmount = value;
            internalClearRefs(PROP_ID_differenceAmount);
            
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
    public final app.erp.inv.dao.entity.ErpInvStockTake getTake(){
       return (app.erp.inv.dao.entity.ErpInvStockTake)internalGetRefEntity(PROP_NAME_take);
    }

    public final void setTake(app.erp.inv.dao.entity.ErpInvStockTake refEntity){
   
           if(refEntity == null){
           
                   this.setTakeId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_take, refEntity,()->{
           
                           this.setTakeId(refEntity.getId());
                       
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
    public final app.erp.md.dao.entity.ErpMdUoM getUoM(){
       return (app.erp.md.dao.entity.ErpMdUoM)internalGetRefEntity(PROP_NAME_uoM);
    }

    public final void setUoM(app.erp.md.dao.entity.ErpMdUoM refEntity){
   
           if(refEntity == null){
           
                   this.setUoMId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_uoM, refEntity,()->{
           
                           this.setUoMId(refEntity.getId());
                       
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
       
}
// resume CPD analysis - CPD-ON
