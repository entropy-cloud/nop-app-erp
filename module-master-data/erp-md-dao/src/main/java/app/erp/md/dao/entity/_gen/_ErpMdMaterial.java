package app.erp.md.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.md.dao.entity.ErpMdMaterial;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  物料: erp_md_material
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpMdMaterial extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 物料编码: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 物料名称: NAME VARCHAR */
    public static final String PROP_NAME_name = "name";
    public static final int PROP_ID_name = 3;
    
    /* 物料类型: MATERIAL_TYPE VARCHAR */
    public static final String PROP_NAME_materialType = "materialType";
    public static final int PROP_ID_materialType = 4;
    
    /* 分类ID: CATEGORY_ID BIGINT */
    public static final String PROP_NAME_categoryId = "categoryId";
    public static final int PROP_ID_categoryId = 5;
    
    /* 主计量单位: UOM_ID BIGINT */
    public static final String PROP_NAME_uoMId = "uoMId";
    public static final int PROP_ID_uoMId = 6;
    
    /* 状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 7;
    
    /* 存货计价方法: COST_METHOD VARCHAR */
    public static final String PROP_NAME_costMethod = "costMethod";
    public static final int PROP_ID_costMethod = 8;
    
    /* 是否批次管理: IS_BATCH_MANAGED BOOLEAN */
    public static final String PROP_NAME_isBatchManaged = "isBatchManaged";
    public static final int PROP_ID_isBatchManaged = 9;
    
    /* 是否序列号管理: IS_SERIAL_MANAGED BOOLEAN */
    public static final String PROP_NAME_isSerialManaged = "isSerialManaged";
    public static final int PROP_ID_isSerialManaged = 10;
    
    /* 默认仓库: DEFAULT_WAREHOUSE_ID BIGINT */
    public static final String PROP_NAME_defaultWarehouseId = "defaultWarehouseId";
    public static final int PROP_ID_defaultWarehouseId = 11;
    
    /* 最低库存: MIN_STOCK DECIMAL */
    public static final String PROP_NAME_minStock = "minStock";
    public static final int PROP_ID_minStock = 12;
    
    /* 最高库存: MAX_STOCK DECIMAL */
    public static final String PROP_NAME_maxStock = "maxStock";
    public static final int PROP_ID_maxStock = 13;
    
    /* 安全库存: SAFETY_STOCK DECIMAL */
    public static final String PROP_NAME_safetyStock = "safetyStock";
    public static final int PROP_ID_safetyStock = 14;
    
    /* 采购提前期(天): LEAD_TIME_DAYS INTEGER */
    public static final String PROP_NAME_leadTimeDays = "leadTimeDays";
    public static final int PROP_ID_leadTimeDays = 15;
    
    /* 重量(kg): WEIGHT DECIMAL */
    public static final String PROP_NAME_weight = "weight";
    public static final int PROP_ID_weight = 16;
    
    /* 体积(m3): VOLUME DECIMAL */
    public static final String PROP_NAME_volume = "volume";
    public static final int PROP_ID_volume = 17;
    
    /* 默认税率: DEFAULT_TAX_RATE_ID BIGINT */
    public static final String PROP_NAME_defaultTaxRateId = "defaultTaxRateId";
    public static final int PROP_ID_defaultTaxRateId = 18;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 19;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 20;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 21;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 22;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 23;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 24;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 25;
    

    private static int _PROP_ID_BOUND = 26;

    
    /* relation:  */
    public static final String PROP_NAME_category = "category";
    
    /* relation:  */
    public static final String PROP_NAME_uoM = "uoM";
    
    /* relation:  */
    public static final String PROP_NAME_defaultWarehouse = "defaultWarehouse";
    
    /* relation:  */
    public static final String PROP_NAME_defaultTaxRate = "defaultTaxRate";
    
    /* relation:  */
    public static final String PROP_NAME_skus = "skus";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[26];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_name] = PROP_NAME_name;
          PROP_NAME_TO_ID.put(PROP_NAME_name, PROP_ID_name);
      
          PROP_ID_TO_NAME[PROP_ID_materialType] = PROP_NAME_materialType;
          PROP_NAME_TO_ID.put(PROP_NAME_materialType, PROP_ID_materialType);
      
          PROP_ID_TO_NAME[PROP_ID_categoryId] = PROP_NAME_categoryId;
          PROP_NAME_TO_ID.put(PROP_NAME_categoryId, PROP_ID_categoryId);
      
          PROP_ID_TO_NAME[PROP_ID_uoMId] = PROP_NAME_uoMId;
          PROP_NAME_TO_ID.put(PROP_NAME_uoMId, PROP_ID_uoMId);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_costMethod] = PROP_NAME_costMethod;
          PROP_NAME_TO_ID.put(PROP_NAME_costMethod, PROP_ID_costMethod);
      
          PROP_ID_TO_NAME[PROP_ID_isBatchManaged] = PROP_NAME_isBatchManaged;
          PROP_NAME_TO_ID.put(PROP_NAME_isBatchManaged, PROP_ID_isBatchManaged);
      
          PROP_ID_TO_NAME[PROP_ID_isSerialManaged] = PROP_NAME_isSerialManaged;
          PROP_NAME_TO_ID.put(PROP_NAME_isSerialManaged, PROP_ID_isSerialManaged);
      
          PROP_ID_TO_NAME[PROP_ID_defaultWarehouseId] = PROP_NAME_defaultWarehouseId;
          PROP_NAME_TO_ID.put(PROP_NAME_defaultWarehouseId, PROP_ID_defaultWarehouseId);
      
          PROP_ID_TO_NAME[PROP_ID_minStock] = PROP_NAME_minStock;
          PROP_NAME_TO_ID.put(PROP_NAME_minStock, PROP_ID_minStock);
      
          PROP_ID_TO_NAME[PROP_ID_maxStock] = PROP_NAME_maxStock;
          PROP_NAME_TO_ID.put(PROP_NAME_maxStock, PROP_ID_maxStock);
      
          PROP_ID_TO_NAME[PROP_ID_safetyStock] = PROP_NAME_safetyStock;
          PROP_NAME_TO_ID.put(PROP_NAME_safetyStock, PROP_ID_safetyStock);
      
          PROP_ID_TO_NAME[PROP_ID_leadTimeDays] = PROP_NAME_leadTimeDays;
          PROP_NAME_TO_ID.put(PROP_NAME_leadTimeDays, PROP_ID_leadTimeDays);
      
          PROP_ID_TO_NAME[PROP_ID_weight] = PROP_NAME_weight;
          PROP_NAME_TO_ID.put(PROP_NAME_weight, PROP_ID_weight);
      
          PROP_ID_TO_NAME[PROP_ID_volume] = PROP_NAME_volume;
          PROP_NAME_TO_ID.put(PROP_NAME_volume, PROP_ID_volume);
      
          PROP_ID_TO_NAME[PROP_ID_defaultTaxRateId] = PROP_NAME_defaultTaxRateId;
          PROP_NAME_TO_ID.put(PROP_NAME_defaultTaxRateId, PROP_ID_defaultTaxRateId);
      
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
      
          PROP_ID_TO_NAME[PROP_ID_remark] = PROP_NAME_remark;
          PROP_NAME_TO_ID.put(PROP_NAME_remark, PROP_ID_remark);
      
    }

    
    /* ID: ID */
    private java.lang.Long _id;
    
    /* 物料编码: CODE */
    private java.lang.String _code;
    
    /* 物料名称: NAME */
    private java.lang.String _name;
    
    /* 物料类型: MATERIAL_TYPE */
    private java.lang.String _materialType;
    
    /* 分类ID: CATEGORY_ID */
    private java.lang.Long _categoryId;
    
    /* 主计量单位: UOM_ID */
    private java.lang.Long _uoMId;
    
    /* 状态: STATUS */
    private java.lang.String _status;
    
    /* 存货计价方法: COST_METHOD */
    private java.lang.String _costMethod;
    
    /* 是否批次管理: IS_BATCH_MANAGED */
    private java.lang.Boolean _isBatchManaged;
    
    /* 是否序列号管理: IS_SERIAL_MANAGED */
    private java.lang.Boolean _isSerialManaged;
    
    /* 默认仓库: DEFAULT_WAREHOUSE_ID */
    private java.lang.Long _defaultWarehouseId;
    
    /* 最低库存: MIN_STOCK */
    private java.math.BigDecimal _minStock;
    
    /* 最高库存: MAX_STOCK */
    private java.math.BigDecimal _maxStock;
    
    /* 安全库存: SAFETY_STOCK */
    private java.math.BigDecimal _safetyStock;
    
    /* 采购提前期(天): LEAD_TIME_DAYS */
    private java.lang.Integer _leadTimeDays;
    
    /* 重量(kg): WEIGHT */
    private java.math.BigDecimal _weight;
    
    /* 体积(m3): VOLUME */
    private java.math.BigDecimal _volume;
    
    /* 默认税率: DEFAULT_TAX_RATE_ID */
    private java.lang.Long _defaultTaxRateId;
    
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
    
    /* 备注: REMARK */
    private java.lang.String _remark;
    

    public _ErpMdMaterial(){
        // for debug
    }

    protected ErpMdMaterial newInstance(){
        ErpMdMaterial entity = new ErpMdMaterial();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpMdMaterial cloneInstance() {
        ErpMdMaterial entity = newInstance();
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
      return "app.erp.md.dao.entity.ErpMdMaterial";
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
        
            case PROP_ID_name:
               return getName();
        
            case PROP_ID_materialType:
               return getMaterialType();
        
            case PROP_ID_categoryId:
               return getCategoryId();
        
            case PROP_ID_uoMId:
               return getUoMId();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_costMethod:
               return getCostMethod();
        
            case PROP_ID_isBatchManaged:
               return getIsBatchManaged();
        
            case PROP_ID_isSerialManaged:
               return getIsSerialManaged();
        
            case PROP_ID_defaultWarehouseId:
               return getDefaultWarehouseId();
        
            case PROP_ID_minStock:
               return getMinStock();
        
            case PROP_ID_maxStock:
               return getMaxStock();
        
            case PROP_ID_safetyStock:
               return getSafetyStock();
        
            case PROP_ID_leadTimeDays:
               return getLeadTimeDays();
        
            case PROP_ID_weight:
               return getWeight();
        
            case PROP_ID_volume:
               return getVolume();
        
            case PROP_ID_defaultTaxRateId:
               return getDefaultTaxRateId();
        
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
        
            case PROP_ID_remark:
               return getRemark();
        
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
        
            case PROP_ID_name:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_name));
               }
               setName(typedValue);
               break;
            }
        
            case PROP_ID_materialType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_materialType));
               }
               setMaterialType(typedValue);
               break;
            }
        
            case PROP_ID_categoryId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_categoryId));
               }
               setCategoryId(typedValue);
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
        
            case PROP_ID_status:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_status));
               }
               setStatus(typedValue);
               break;
            }
        
            case PROP_ID_costMethod:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_costMethod));
               }
               setCostMethod(typedValue);
               break;
            }
        
            case PROP_ID_isBatchManaged:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isBatchManaged));
               }
               setIsBatchManaged(typedValue);
               break;
            }
        
            case PROP_ID_isSerialManaged:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isSerialManaged));
               }
               setIsSerialManaged(typedValue);
               break;
            }
        
            case PROP_ID_defaultWarehouseId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_defaultWarehouseId));
               }
               setDefaultWarehouseId(typedValue);
               break;
            }
        
            case PROP_ID_minStock:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_minStock));
               }
               setMinStock(typedValue);
               break;
            }
        
            case PROP_ID_maxStock:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_maxStock));
               }
               setMaxStock(typedValue);
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
        
            case PROP_ID_leadTimeDays:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_leadTimeDays));
               }
               setLeadTimeDays(typedValue);
               break;
            }
        
            case PROP_ID_weight:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_weight));
               }
               setWeight(typedValue);
               break;
            }
        
            case PROP_ID_volume:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_volume));
               }
               setVolume(typedValue);
               break;
            }
        
            case PROP_ID_defaultTaxRateId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_defaultTaxRateId));
               }
               setDefaultTaxRateId(typedValue);
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
        
            case PROP_ID_remark:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_remark));
               }
               setRemark(typedValue);
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
        
            case PROP_ID_name:{
               onInitProp(propId);
               this._name = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_materialType:{
               onInitProp(propId);
               this._materialType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_categoryId:{
               onInitProp(propId);
               this._categoryId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_uoMId:{
               onInitProp(propId);
               this._uoMId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_costMethod:{
               onInitProp(propId);
               this._costMethod = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_isBatchManaged:{
               onInitProp(propId);
               this._isBatchManaged = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_isSerialManaged:{
               onInitProp(propId);
               this._isSerialManaged = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_defaultWarehouseId:{
               onInitProp(propId);
               this._defaultWarehouseId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_minStock:{
               onInitProp(propId);
               this._minStock = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_maxStock:{
               onInitProp(propId);
               this._maxStock = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_safetyStock:{
               onInitProp(propId);
               this._safetyStock = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_leadTimeDays:{
               onInitProp(propId);
               this._leadTimeDays = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_weight:{
               onInitProp(propId);
               this._weight = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_volume:{
               onInitProp(propId);
               this._volume = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_defaultTaxRateId:{
               onInitProp(propId);
               this._defaultTaxRateId = (java.lang.Long)value;
               
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
        
            case PROP_ID_remark:{
               onInitProp(propId);
               this._remark = (java.lang.String)value;
               
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
     * 物料编码: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 物料编码: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
        }
    }
    
    /**
     * 物料名称: NAME
     */
    public final java.lang.String getName(){
         onPropGet(PROP_ID_name);
         return _name;
    }

    /**
     * 物料名称: NAME
     */
    public final void setName(java.lang.String value){
        if(onPropSet(PROP_ID_name,value)){
            this._name = value;
            internalClearRefs(PROP_ID_name);
            
        }
    }
    
    /**
     * 物料类型: MATERIAL_TYPE
     */
    public final java.lang.String getMaterialType(){
         onPropGet(PROP_ID_materialType);
         return _materialType;
    }

    /**
     * 物料类型: MATERIAL_TYPE
     */
    public final void setMaterialType(java.lang.String value){
        if(onPropSet(PROP_ID_materialType,value)){
            this._materialType = value;
            internalClearRefs(PROP_ID_materialType);
            
        }
    }
    
    /**
     * 分类ID: CATEGORY_ID
     */
    public final java.lang.Long getCategoryId(){
         onPropGet(PROP_ID_categoryId);
         return _categoryId;
    }

    /**
     * 分类ID: CATEGORY_ID
     */
    public final void setCategoryId(java.lang.Long value){
        if(onPropSet(PROP_ID_categoryId,value)){
            this._categoryId = value;
            internalClearRefs(PROP_ID_categoryId);
            
        }
    }
    
    /**
     * 主计量单位: UOM_ID
     */
    public final java.lang.Long getUoMId(){
         onPropGet(PROP_ID_uoMId);
         return _uoMId;
    }

    /**
     * 主计量单位: UOM_ID
     */
    public final void setUoMId(java.lang.Long value){
        if(onPropSet(PROP_ID_uoMId,value)){
            this._uoMId = value;
            internalClearRefs(PROP_ID_uoMId);
            
        }
    }
    
    /**
     * 状态: STATUS
     */
    public final java.lang.String getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 状态: STATUS
     */
    public final void setStatus(java.lang.String value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
        }
    }
    
    /**
     * 存货计价方法: COST_METHOD
     */
    public final java.lang.String getCostMethod(){
         onPropGet(PROP_ID_costMethod);
         return _costMethod;
    }

    /**
     * 存货计价方法: COST_METHOD
     */
    public final void setCostMethod(java.lang.String value){
        if(onPropSet(PROP_ID_costMethod,value)){
            this._costMethod = value;
            internalClearRefs(PROP_ID_costMethod);
            
        }
    }
    
    /**
     * 是否批次管理: IS_BATCH_MANAGED
     */
    public final java.lang.Boolean getIsBatchManaged(){
         onPropGet(PROP_ID_isBatchManaged);
         return _isBatchManaged;
    }

    /**
     * 是否批次管理: IS_BATCH_MANAGED
     */
    public final void setIsBatchManaged(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isBatchManaged,value)){
            this._isBatchManaged = value;
            internalClearRefs(PROP_ID_isBatchManaged);
            
        }
    }
    
    /**
     * 是否序列号管理: IS_SERIAL_MANAGED
     */
    public final java.lang.Boolean getIsSerialManaged(){
         onPropGet(PROP_ID_isSerialManaged);
         return _isSerialManaged;
    }

    /**
     * 是否序列号管理: IS_SERIAL_MANAGED
     */
    public final void setIsSerialManaged(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isSerialManaged,value)){
            this._isSerialManaged = value;
            internalClearRefs(PROP_ID_isSerialManaged);
            
        }
    }
    
    /**
     * 默认仓库: DEFAULT_WAREHOUSE_ID
     */
    public final java.lang.Long getDefaultWarehouseId(){
         onPropGet(PROP_ID_defaultWarehouseId);
         return _defaultWarehouseId;
    }

    /**
     * 默认仓库: DEFAULT_WAREHOUSE_ID
     */
    public final void setDefaultWarehouseId(java.lang.Long value){
        if(onPropSet(PROP_ID_defaultWarehouseId,value)){
            this._defaultWarehouseId = value;
            internalClearRefs(PROP_ID_defaultWarehouseId);
            
        }
    }
    
    /**
     * 最低库存: MIN_STOCK
     */
    public final java.math.BigDecimal getMinStock(){
         onPropGet(PROP_ID_minStock);
         return _minStock;
    }

    /**
     * 最低库存: MIN_STOCK
     */
    public final void setMinStock(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_minStock,value)){
            this._minStock = value;
            internalClearRefs(PROP_ID_minStock);
            
        }
    }
    
    /**
     * 最高库存: MAX_STOCK
     */
    public final java.math.BigDecimal getMaxStock(){
         onPropGet(PROP_ID_maxStock);
         return _maxStock;
    }

    /**
     * 最高库存: MAX_STOCK
     */
    public final void setMaxStock(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_maxStock,value)){
            this._maxStock = value;
            internalClearRefs(PROP_ID_maxStock);
            
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
     * 采购提前期(天): LEAD_TIME_DAYS
     */
    public final java.lang.Integer getLeadTimeDays(){
         onPropGet(PROP_ID_leadTimeDays);
         return _leadTimeDays;
    }

    /**
     * 采购提前期(天): LEAD_TIME_DAYS
     */
    public final void setLeadTimeDays(java.lang.Integer value){
        if(onPropSet(PROP_ID_leadTimeDays,value)){
            this._leadTimeDays = value;
            internalClearRefs(PROP_ID_leadTimeDays);
            
        }
    }
    
    /**
     * 重量(kg): WEIGHT
     */
    public final java.math.BigDecimal getWeight(){
         onPropGet(PROP_ID_weight);
         return _weight;
    }

    /**
     * 重量(kg): WEIGHT
     */
    public final void setWeight(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_weight,value)){
            this._weight = value;
            internalClearRefs(PROP_ID_weight);
            
        }
    }
    
    /**
     * 体积(m3): VOLUME
     */
    public final java.math.BigDecimal getVolume(){
         onPropGet(PROP_ID_volume);
         return _volume;
    }

    /**
     * 体积(m3): VOLUME
     */
    public final void setVolume(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_volume,value)){
            this._volume = value;
            internalClearRefs(PROP_ID_volume);
            
        }
    }
    
    /**
     * 默认税率: DEFAULT_TAX_RATE_ID
     */
    public final java.lang.Long getDefaultTaxRateId(){
         onPropGet(PROP_ID_defaultTaxRateId);
         return _defaultTaxRateId;
    }

    /**
     * 默认税率: DEFAULT_TAX_RATE_ID
     */
    public final void setDefaultTaxRateId(java.lang.Long value){
        if(onPropSet(PROP_ID_defaultTaxRateId,value)){
            this._defaultTaxRateId = value;
            internalClearRefs(PROP_ID_defaultTaxRateId);
            
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
     * 
     */
    public final app.erp.md.dao.entity.ErpMdMaterialCategory getCategory(){
       return (app.erp.md.dao.entity.ErpMdMaterialCategory)internalGetRefEntity(PROP_NAME_category);
    }

    public final void setCategory(app.erp.md.dao.entity.ErpMdMaterialCategory refEntity){
   
           if(refEntity == null){
           
                   this.setCategoryId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_category, refEntity,()->{
           
                           this.setCategoryId(refEntity.getId());
                       
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
    public final app.erp.md.dao.entity.ErpMdWarehouse getDefaultWarehouse(){
       return (app.erp.md.dao.entity.ErpMdWarehouse)internalGetRefEntity(PROP_NAME_defaultWarehouse);
    }

    public final void setDefaultWarehouse(app.erp.md.dao.entity.ErpMdWarehouse refEntity){
   
           if(refEntity == null){
           
                   this.setDefaultWarehouseId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_defaultWarehouse, refEntity,()->{
           
                           this.setDefaultWarehouseId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdTaxRate getDefaultTaxRate(){
       return (app.erp.md.dao.entity.ErpMdTaxRate)internalGetRefEntity(PROP_NAME_defaultTaxRate);
    }

    public final void setDefaultTaxRate(app.erp.md.dao.entity.ErpMdTaxRate refEntity){
   
           if(refEntity == null){
           
                   this.setDefaultTaxRateId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_defaultTaxRate, refEntity,()->{
           
                           this.setDefaultTaxRateId(refEntity.getId());
                       
           });
           }
       
    }
       
    private final OrmEntitySet<app.erp.md.dao.entity.ErpMdMaterialSku> _skus = new OrmEntitySet<>(this, PROP_NAME_skus,
        null, null,app.erp.md.dao.entity.ErpMdMaterialSku.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.md.dao.entity.ErpMdMaterialSku> getSkus(){
       return _skus;
    }
       
}
// resume CPD analysis - CPD-ON
