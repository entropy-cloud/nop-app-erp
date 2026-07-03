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

import app.erp.mfg.dao.entity.ErpMfgBom;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  BOM: erp_mfg_bom
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpMfgBom extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* BOM编码: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 产品: PRODUCT_ID BIGINT */
    public static final String PROP_NAME_productId = "productId";
    public static final int PROP_ID_productId = 3;
    
    /* BOM类型: BOM_TYPE VARCHAR */
    public static final String PROP_NAME_bomType = "bomType";
    public static final int PROP_ID_bomType = 4;
    
    /* 消耗控制: CONSUMPTION VARCHAR */
    public static final String PROP_NAME_consumption = "consumption";
    public static final int PROP_ID_consumption = 5;
    
    /* 是否有效: IS_ACTIVE BOOLEAN */
    public static final String PROP_NAME_isActive = "isActive";
    public static final int PROP_ID_isActive = 6;
    
    /* 是否默认: IS_DEFAULT BOOLEAN */
    public static final String PROP_NAME_isDefault = "isDefault";
    public static final int PROP_ID_isDefault = 7;
    
    /* 展开多层BOM: USE_MULTI_LEVEL_BOM BOOLEAN */
    public static final String PROP_NAME_useMultiLevelBom = "useMultiLevelBom";
    public static final int PROP_ID_useMultiLevelBom = 8;
    
    /* 需要质检: INSPECTION_REQUIRED BOOLEAN */
    public static final String PROP_NAME_inspectionRequired = "inspectionRequired";
    public static final int PROP_ID_inspectionRequired = 9;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 10;
    
    /* 版本号: VERSION_LABEL VARCHAR */
    public static final String PROP_NAME_versionLabel = "versionLabel";
    public static final int PROP_ID_versionLabel = 11;
    
    /* BOM数量: QTY DECIMAL */
    public static final String PROP_NAME_qty = "qty";
    public static final int PROP_ID_qty = 12;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 13;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 14;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 15;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 16;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 17;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 18;
    

    private static int _PROP_ID_BOUND = 19;

    
    /* relation:  */
    public static final String PROP_NAME_product = "product";
    
    /* relation:  */
    public static final String PROP_NAME_lines = "lines";
    
    /* relation:  */
    public static final String PROP_NAME_operations = "operations";
    
    /* relation:  */
    public static final String PROP_NAME_byproducts = "byproducts";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[19];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_productId] = PROP_NAME_productId;
          PROP_NAME_TO_ID.put(PROP_NAME_productId, PROP_ID_productId);
      
          PROP_ID_TO_NAME[PROP_ID_bomType] = PROP_NAME_bomType;
          PROP_NAME_TO_ID.put(PROP_NAME_bomType, PROP_ID_bomType);
      
          PROP_ID_TO_NAME[PROP_ID_consumption] = PROP_NAME_consumption;
          PROP_NAME_TO_ID.put(PROP_NAME_consumption, PROP_ID_consumption);
      
          PROP_ID_TO_NAME[PROP_ID_isActive] = PROP_NAME_isActive;
          PROP_NAME_TO_ID.put(PROP_NAME_isActive, PROP_ID_isActive);
      
          PROP_ID_TO_NAME[PROP_ID_isDefault] = PROP_NAME_isDefault;
          PROP_NAME_TO_ID.put(PROP_NAME_isDefault, PROP_ID_isDefault);
      
          PROP_ID_TO_NAME[PROP_ID_useMultiLevelBom] = PROP_NAME_useMultiLevelBom;
          PROP_NAME_TO_ID.put(PROP_NAME_useMultiLevelBom, PROP_ID_useMultiLevelBom);
      
          PROP_ID_TO_NAME[PROP_ID_inspectionRequired] = PROP_NAME_inspectionRequired;
          PROP_NAME_TO_ID.put(PROP_NAME_inspectionRequired, PROP_ID_inspectionRequired);
      
          PROP_ID_TO_NAME[PROP_ID_remark] = PROP_NAME_remark;
          PROP_NAME_TO_ID.put(PROP_NAME_remark, PROP_ID_remark);
      
          PROP_ID_TO_NAME[PROP_ID_versionLabel] = PROP_NAME_versionLabel;
          PROP_NAME_TO_ID.put(PROP_NAME_versionLabel, PROP_ID_versionLabel);
      
          PROP_ID_TO_NAME[PROP_ID_qty] = PROP_NAME_qty;
          PROP_NAME_TO_ID.put(PROP_NAME_qty, PROP_ID_qty);
      
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
    
    /* BOM编码: CODE */
    private java.lang.String _code;
    
    /* 产品: PRODUCT_ID */
    private java.lang.Long _productId;
    
    /* BOM类型: BOM_TYPE */
    private java.lang.String _bomType;
    
    /* 消耗控制: CONSUMPTION */
    private java.lang.String _consumption;
    
    /* 是否有效: IS_ACTIVE */
    private java.lang.Boolean _isActive;
    
    /* 是否默认: IS_DEFAULT */
    private java.lang.Boolean _isDefault;
    
    /* 展开多层BOM: USE_MULTI_LEVEL_BOM */
    private java.lang.Boolean _useMultiLevelBom;
    
    /* 需要质检: INSPECTION_REQUIRED */
    private java.lang.Boolean _inspectionRequired;
    
    /* 备注: REMARK */
    private java.lang.String _remark;
    
    /* 版本号: VERSION_LABEL */
    private java.lang.String _versionLabel;
    
    /* BOM数量: QTY */
    private java.math.BigDecimal _qty;
    
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
    

    public _ErpMfgBom(){
        // for debug
    }

    protected ErpMfgBom newInstance(){
        ErpMfgBom entity = new ErpMfgBom();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpMfgBom cloneInstance() {
        ErpMfgBom entity = newInstance();
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
      return "app.erp.mfg.dao.entity.ErpMfgBom";
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
        
            case PROP_ID_productId:
               return getProductId();
        
            case PROP_ID_bomType:
               return getBomType();
        
            case PROP_ID_consumption:
               return getConsumption();
        
            case PROP_ID_isActive:
               return getIsActive();
        
            case PROP_ID_isDefault:
               return getIsDefault();
        
            case PROP_ID_useMultiLevelBom:
               return getUseMultiLevelBom();
        
            case PROP_ID_inspectionRequired:
               return getInspectionRequired();
        
            case PROP_ID_remark:
               return getRemark();
        
            case PROP_ID_versionLabel:
               return getVersionLabel();
        
            case PROP_ID_qty:
               return getQty();
        
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
        
            case PROP_ID_productId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_productId));
               }
               setProductId(typedValue);
               break;
            }
        
            case PROP_ID_bomType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_bomType));
               }
               setBomType(typedValue);
               break;
            }
        
            case PROP_ID_consumption:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_consumption));
               }
               setConsumption(typedValue);
               break;
            }
        
            case PROP_ID_isActive:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isActive));
               }
               setIsActive(typedValue);
               break;
            }
        
            case PROP_ID_isDefault:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isDefault));
               }
               setIsDefault(typedValue);
               break;
            }
        
            case PROP_ID_useMultiLevelBom:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_useMultiLevelBom));
               }
               setUseMultiLevelBom(typedValue);
               break;
            }
        
            case PROP_ID_inspectionRequired:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_inspectionRequired));
               }
               setInspectionRequired(typedValue);
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
        
            case PROP_ID_versionLabel:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_versionLabel));
               }
               setVersionLabel(typedValue);
               break;
            }
        
            case PROP_ID_qty:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_qty));
               }
               setQty(typedValue);
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
        
            case PROP_ID_productId:{
               onInitProp(propId);
               this._productId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_bomType:{
               onInitProp(propId);
               this._bomType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_consumption:{
               onInitProp(propId);
               this._consumption = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_isActive:{
               onInitProp(propId);
               this._isActive = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_isDefault:{
               onInitProp(propId);
               this._isDefault = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_useMultiLevelBom:{
               onInitProp(propId);
               this._useMultiLevelBom = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_inspectionRequired:{
               onInitProp(propId);
               this._inspectionRequired = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_remark:{
               onInitProp(propId);
               this._remark = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_versionLabel:{
               onInitProp(propId);
               this._versionLabel = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_qty:{
               onInitProp(propId);
               this._qty = (java.math.BigDecimal)value;
               
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
     * BOM编码: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * BOM编码: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
        }
    }
    
    /**
     * 产品: PRODUCT_ID
     */
    public final java.lang.Long getProductId(){
         onPropGet(PROP_ID_productId);
         return _productId;
    }

    /**
     * 产品: PRODUCT_ID
     */
    public final void setProductId(java.lang.Long value){
        if(onPropSet(PROP_ID_productId,value)){
            this._productId = value;
            internalClearRefs(PROP_ID_productId);
            
        }
    }
    
    /**
     * BOM类型: BOM_TYPE
     */
    public final java.lang.String getBomType(){
         onPropGet(PROP_ID_bomType);
         return _bomType;
    }

    /**
     * BOM类型: BOM_TYPE
     */
    public final void setBomType(java.lang.String value){
        if(onPropSet(PROP_ID_bomType,value)){
            this._bomType = value;
            internalClearRefs(PROP_ID_bomType);
            
        }
    }
    
    /**
     * 消耗控制: CONSUMPTION
     */
    public final java.lang.String getConsumption(){
         onPropGet(PROP_ID_consumption);
         return _consumption;
    }

    /**
     * 消耗控制: CONSUMPTION
     */
    public final void setConsumption(java.lang.String value){
        if(onPropSet(PROP_ID_consumption,value)){
            this._consumption = value;
            internalClearRefs(PROP_ID_consumption);
            
        }
    }
    
    /**
     * 是否有效: IS_ACTIVE
     */
    public final java.lang.Boolean getIsActive(){
         onPropGet(PROP_ID_isActive);
         return _isActive;
    }

    /**
     * 是否有效: IS_ACTIVE
     */
    public final void setIsActive(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isActive,value)){
            this._isActive = value;
            internalClearRefs(PROP_ID_isActive);
            
        }
    }
    
    /**
     * 是否默认: IS_DEFAULT
     */
    public final java.lang.Boolean getIsDefault(){
         onPropGet(PROP_ID_isDefault);
         return _isDefault;
    }

    /**
     * 是否默认: IS_DEFAULT
     */
    public final void setIsDefault(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isDefault,value)){
            this._isDefault = value;
            internalClearRefs(PROP_ID_isDefault);
            
        }
    }
    
    /**
     * 展开多层BOM: USE_MULTI_LEVEL_BOM
     */
    public final java.lang.Boolean getUseMultiLevelBom(){
         onPropGet(PROP_ID_useMultiLevelBom);
         return _useMultiLevelBom;
    }

    /**
     * 展开多层BOM: USE_MULTI_LEVEL_BOM
     */
    public final void setUseMultiLevelBom(java.lang.Boolean value){
        if(onPropSet(PROP_ID_useMultiLevelBom,value)){
            this._useMultiLevelBom = value;
            internalClearRefs(PROP_ID_useMultiLevelBom);
            
        }
    }
    
    /**
     * 需要质检: INSPECTION_REQUIRED
     */
    public final java.lang.Boolean getInspectionRequired(){
         onPropGet(PROP_ID_inspectionRequired);
         return _inspectionRequired;
    }

    /**
     * 需要质检: INSPECTION_REQUIRED
     */
    public final void setInspectionRequired(java.lang.Boolean value){
        if(onPropSet(PROP_ID_inspectionRequired,value)){
            this._inspectionRequired = value;
            internalClearRefs(PROP_ID_inspectionRequired);
            
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
     * 版本号: VERSION_LABEL
     */
    public final java.lang.String getVersionLabel(){
         onPropGet(PROP_ID_versionLabel);
         return _versionLabel;
    }

    /**
     * 版本号: VERSION_LABEL
     */
    public final void setVersionLabel(java.lang.String value){
        if(onPropSet(PROP_ID_versionLabel,value)){
            this._versionLabel = value;
            internalClearRefs(PROP_ID_versionLabel);
            
        }
    }
    
    /**
     * BOM数量: QTY
     */
    public final java.math.BigDecimal getQty(){
         onPropGet(PROP_ID_qty);
         return _qty;
    }

    /**
     * BOM数量: QTY
     */
    public final void setQty(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_qty,value)){
            this._qty = value;
            internalClearRefs(PROP_ID_qty);
            
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
       
    private final OrmEntitySet<app.erp.mfg.dao.entity.ErpMfgBomLine> _lines = new OrmEntitySet<>(this, PROP_NAME_lines,
        null, null,app.erp.mfg.dao.entity.ErpMfgBomLine.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.mfg.dao.entity.ErpMfgBomLine> getLines(){
       return _lines;
    }
       
    private final OrmEntitySet<app.erp.mfg.dao.entity.ErpMfgBomOperation> _operations = new OrmEntitySet<>(this, PROP_NAME_operations,
        null, null,app.erp.mfg.dao.entity.ErpMfgBomOperation.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.mfg.dao.entity.ErpMfgBomOperation> getOperations(){
       return _operations;
    }
       
    private final OrmEntitySet<app.erp.mfg.dao.entity.ErpMfgBomByproduct> _byproducts = new OrmEntitySet<>(this, PROP_NAME_byproducts,
        null, null,app.erp.mfg.dao.entity.ErpMfgBomByproduct.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.mfg.dao.entity.ErpMfgBomByproduct> getByproducts(){
       return _byproducts;
    }
       
}
// resume CPD analysis - CPD-ON
