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

import app.erp.md.dao.entity.ErpMdMaterialSku;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  物料SKU: erp_md_material_sku
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpMdMaterialSku extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 物料ID: MATERIAL_ID BIGINT */
    public static final String PROP_NAME_materialId = "materialId";
    public static final int PROP_ID_materialId = 2;
    
    /* SKU编码: SKU_CODE VARCHAR */
    public static final String PROP_NAME_skuCode = "skuCode";
    public static final int PROP_ID_skuCode = 3;
    
    /* 条码: BARCODE VARCHAR */
    public static final String PROP_NAME_barcode = "barcode";
    public static final int PROP_ID_barcode = 4;
    
    /* 计量单位: UOM_ID BIGINT */
    public static final String PROP_NAME_uoMId = "uoMId";
    public static final int PROP_ID_uoMId = 5;
    
    /* 换算系数: CONVERSION_RATE DECIMAL */
    public static final String PROP_NAME_conversionRate = "conversionRate";
    public static final int PROP_ID_conversionRate = 6;
    
    /* 采购价(不含税): PURCHASE_PRICE DECIMAL */
    public static final String PROP_NAME_purchasePrice = "purchasePrice";
    public static final int PROP_ID_purchasePrice = 7;
    
    /* 销售价(不含税): SALE_PRICE DECIMAL */
    public static final String PROP_NAME_salePrice = "salePrice";
    public static final int PROP_ID_salePrice = 8;
    
    /* 批发价(不含税): WHOLESALE_PRICE DECIMAL */
    public static final String PROP_NAME_wholesalePrice = "wholesalePrice";
    public static final int PROP_ID_wholesalePrice = 9;
    
    /* 零售价(不含税): RETAIL_PRICE DECIMAL */
    public static final String PROP_NAME_retailPrice = "retailPrice";
    public static final int PROP_ID_retailPrice = 10;
    
    /* 默认税率: TAX_RATE_ID BIGINT */
    public static final String PROP_NAME_taxRateId = "taxRateId";
    public static final int PROP_ID_taxRateId = 11;
    
    /* 是否默认SKU: IS_DEFAULT BOOLEAN */
    public static final String PROP_NAME_isDefault = "isDefault";
    public static final int PROP_ID_isDefault = 12;
    
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
    public static final String PROP_NAME_material = "material";
    
    /* relation:  */
    public static final String PROP_NAME_uoM = "uoM";
    
    /* relation:  */
    public static final String PROP_NAME_taxRate = "taxRate";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[19];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_materialId] = PROP_NAME_materialId;
          PROP_NAME_TO_ID.put(PROP_NAME_materialId, PROP_ID_materialId);
      
          PROP_ID_TO_NAME[PROP_ID_skuCode] = PROP_NAME_skuCode;
          PROP_NAME_TO_ID.put(PROP_NAME_skuCode, PROP_ID_skuCode);
      
          PROP_ID_TO_NAME[PROP_ID_barcode] = PROP_NAME_barcode;
          PROP_NAME_TO_ID.put(PROP_NAME_barcode, PROP_ID_barcode);
      
          PROP_ID_TO_NAME[PROP_ID_uoMId] = PROP_NAME_uoMId;
          PROP_NAME_TO_ID.put(PROP_NAME_uoMId, PROP_ID_uoMId);
      
          PROP_ID_TO_NAME[PROP_ID_conversionRate] = PROP_NAME_conversionRate;
          PROP_NAME_TO_ID.put(PROP_NAME_conversionRate, PROP_ID_conversionRate);
      
          PROP_ID_TO_NAME[PROP_ID_purchasePrice] = PROP_NAME_purchasePrice;
          PROP_NAME_TO_ID.put(PROP_NAME_purchasePrice, PROP_ID_purchasePrice);
      
          PROP_ID_TO_NAME[PROP_ID_salePrice] = PROP_NAME_salePrice;
          PROP_NAME_TO_ID.put(PROP_NAME_salePrice, PROP_ID_salePrice);
      
          PROP_ID_TO_NAME[PROP_ID_wholesalePrice] = PROP_NAME_wholesalePrice;
          PROP_NAME_TO_ID.put(PROP_NAME_wholesalePrice, PROP_ID_wholesalePrice);
      
          PROP_ID_TO_NAME[PROP_ID_retailPrice] = PROP_NAME_retailPrice;
          PROP_NAME_TO_ID.put(PROP_NAME_retailPrice, PROP_ID_retailPrice);
      
          PROP_ID_TO_NAME[PROP_ID_taxRateId] = PROP_NAME_taxRateId;
          PROP_NAME_TO_ID.put(PROP_NAME_taxRateId, PROP_ID_taxRateId);
      
          PROP_ID_TO_NAME[PROP_ID_isDefault] = PROP_NAME_isDefault;
          PROP_NAME_TO_ID.put(PROP_NAME_isDefault, PROP_ID_isDefault);
      
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
    
    /* 物料ID: MATERIAL_ID */
    private java.lang.Long _materialId;
    
    /* SKU编码: SKU_CODE */
    private java.lang.String _skuCode;
    
    /* 条码: BARCODE */
    private java.lang.String _barcode;
    
    /* 计量单位: UOM_ID */
    private java.lang.Long _uoMId;
    
    /* 换算系数: CONVERSION_RATE */
    private java.math.BigDecimal _conversionRate;
    
    /* 采购价(不含税): PURCHASE_PRICE */
    private java.math.BigDecimal _purchasePrice;
    
    /* 销售价(不含税): SALE_PRICE */
    private java.math.BigDecimal _salePrice;
    
    /* 批发价(不含税): WHOLESALE_PRICE */
    private java.math.BigDecimal _wholesalePrice;
    
    /* 零售价(不含税): RETAIL_PRICE */
    private java.math.BigDecimal _retailPrice;
    
    /* 默认税率: TAX_RATE_ID */
    private java.lang.Long _taxRateId;
    
    /* 是否默认SKU: IS_DEFAULT */
    private java.lang.Boolean _isDefault;
    
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
    

    public _ErpMdMaterialSku(){
        // for debug
    }

    protected ErpMdMaterialSku newInstance(){
        ErpMdMaterialSku entity = new ErpMdMaterialSku();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpMdMaterialSku cloneInstance() {
        ErpMdMaterialSku entity = newInstance();
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
      return "app.erp.md.dao.entity.ErpMdMaterialSku";
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
        
            case PROP_ID_materialId:
               return getMaterialId();
        
            case PROP_ID_skuCode:
               return getSkuCode();
        
            case PROP_ID_barcode:
               return getBarcode();
        
            case PROP_ID_uoMId:
               return getUoMId();
        
            case PROP_ID_conversionRate:
               return getConversionRate();
        
            case PROP_ID_purchasePrice:
               return getPurchasePrice();
        
            case PROP_ID_salePrice:
               return getSalePrice();
        
            case PROP_ID_wholesalePrice:
               return getWholesalePrice();
        
            case PROP_ID_retailPrice:
               return getRetailPrice();
        
            case PROP_ID_taxRateId:
               return getTaxRateId();
        
            case PROP_ID_isDefault:
               return getIsDefault();
        
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
        
            case PROP_ID_materialId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_materialId));
               }
               setMaterialId(typedValue);
               break;
            }
        
            case PROP_ID_skuCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_skuCode));
               }
               setSkuCode(typedValue);
               break;
            }
        
            case PROP_ID_barcode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_barcode));
               }
               setBarcode(typedValue);
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
        
            case PROP_ID_conversionRate:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_conversionRate));
               }
               setConversionRate(typedValue);
               break;
            }
        
            case PROP_ID_purchasePrice:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_purchasePrice));
               }
               setPurchasePrice(typedValue);
               break;
            }
        
            case PROP_ID_salePrice:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_salePrice));
               }
               setSalePrice(typedValue);
               break;
            }
        
            case PROP_ID_wholesalePrice:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_wholesalePrice));
               }
               setWholesalePrice(typedValue);
               break;
            }
        
            case PROP_ID_retailPrice:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_retailPrice));
               }
               setRetailPrice(typedValue);
               break;
            }
        
            case PROP_ID_taxRateId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_taxRateId));
               }
               setTaxRateId(typedValue);
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
        
            case PROP_ID_materialId:{
               onInitProp(propId);
               this._materialId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_skuCode:{
               onInitProp(propId);
               this._skuCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_barcode:{
               onInitProp(propId);
               this._barcode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_uoMId:{
               onInitProp(propId);
               this._uoMId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_conversionRate:{
               onInitProp(propId);
               this._conversionRate = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_purchasePrice:{
               onInitProp(propId);
               this._purchasePrice = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_salePrice:{
               onInitProp(propId);
               this._salePrice = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_wholesalePrice:{
               onInitProp(propId);
               this._wholesalePrice = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_retailPrice:{
               onInitProp(propId);
               this._retailPrice = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_taxRateId:{
               onInitProp(propId);
               this._taxRateId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_isDefault:{
               onInitProp(propId);
               this._isDefault = (java.lang.Boolean)value;
               
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
     * 物料ID: MATERIAL_ID
     */
    public final java.lang.Long getMaterialId(){
         onPropGet(PROP_ID_materialId);
         return _materialId;
    }

    /**
     * 物料ID: MATERIAL_ID
     */
    public final void setMaterialId(java.lang.Long value){
        if(onPropSet(PROP_ID_materialId,value)){
            this._materialId = value;
            internalClearRefs(PROP_ID_materialId);
            
        }
    }
    
    /**
     * SKU编码: SKU_CODE
     */
    public final java.lang.String getSkuCode(){
         onPropGet(PROP_ID_skuCode);
         return _skuCode;
    }

    /**
     * SKU编码: SKU_CODE
     */
    public final void setSkuCode(java.lang.String value){
        if(onPropSet(PROP_ID_skuCode,value)){
            this._skuCode = value;
            internalClearRefs(PROP_ID_skuCode);
            
        }
    }
    
    /**
     * 条码: BARCODE
     */
    public final java.lang.String getBarcode(){
         onPropGet(PROP_ID_barcode);
         return _barcode;
    }

    /**
     * 条码: BARCODE
     */
    public final void setBarcode(java.lang.String value){
        if(onPropSet(PROP_ID_barcode,value)){
            this._barcode = value;
            internalClearRefs(PROP_ID_barcode);
            
        }
    }
    
    /**
     * 计量单位: UOM_ID
     */
    public final java.lang.Long getUoMId(){
         onPropGet(PROP_ID_uoMId);
         return _uoMId;
    }

    /**
     * 计量单位: UOM_ID
     */
    public final void setUoMId(java.lang.Long value){
        if(onPropSet(PROP_ID_uoMId,value)){
            this._uoMId = value;
            internalClearRefs(PROP_ID_uoMId);
            
        }
    }
    
    /**
     * 换算系数: CONVERSION_RATE
     */
    public final java.math.BigDecimal getConversionRate(){
         onPropGet(PROP_ID_conversionRate);
         return _conversionRate;
    }

    /**
     * 换算系数: CONVERSION_RATE
     */
    public final void setConversionRate(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_conversionRate,value)){
            this._conversionRate = value;
            internalClearRefs(PROP_ID_conversionRate);
            
        }
    }
    
    /**
     * 采购价(不含税): PURCHASE_PRICE
     */
    public final java.math.BigDecimal getPurchasePrice(){
         onPropGet(PROP_ID_purchasePrice);
         return _purchasePrice;
    }

    /**
     * 采购价(不含税): PURCHASE_PRICE
     */
    public final void setPurchasePrice(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_purchasePrice,value)){
            this._purchasePrice = value;
            internalClearRefs(PROP_ID_purchasePrice);
            
        }
    }
    
    /**
     * 销售价(不含税): SALE_PRICE
     */
    public final java.math.BigDecimal getSalePrice(){
         onPropGet(PROP_ID_salePrice);
         return _salePrice;
    }

    /**
     * 销售价(不含税): SALE_PRICE
     */
    public final void setSalePrice(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_salePrice,value)){
            this._salePrice = value;
            internalClearRefs(PROP_ID_salePrice);
            
        }
    }
    
    /**
     * 批发价(不含税): WHOLESALE_PRICE
     */
    public final java.math.BigDecimal getWholesalePrice(){
         onPropGet(PROP_ID_wholesalePrice);
         return _wholesalePrice;
    }

    /**
     * 批发价(不含税): WHOLESALE_PRICE
     */
    public final void setWholesalePrice(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_wholesalePrice,value)){
            this._wholesalePrice = value;
            internalClearRefs(PROP_ID_wholesalePrice);
            
        }
    }
    
    /**
     * 零售价(不含税): RETAIL_PRICE
     */
    public final java.math.BigDecimal getRetailPrice(){
         onPropGet(PROP_ID_retailPrice);
         return _retailPrice;
    }

    /**
     * 零售价(不含税): RETAIL_PRICE
     */
    public final void setRetailPrice(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_retailPrice,value)){
            this._retailPrice = value;
            internalClearRefs(PROP_ID_retailPrice);
            
        }
    }
    
    /**
     * 默认税率: TAX_RATE_ID
     */
    public final java.lang.Long getTaxRateId(){
         onPropGet(PROP_ID_taxRateId);
         return _taxRateId;
    }

    /**
     * 默认税率: TAX_RATE_ID
     */
    public final void setTaxRateId(java.lang.Long value){
        if(onPropSet(PROP_ID_taxRateId,value)){
            this._taxRateId = value;
            internalClearRefs(PROP_ID_taxRateId);
            
        }
    }
    
    /**
     * 是否默认SKU: IS_DEFAULT
     */
    public final java.lang.Boolean getIsDefault(){
         onPropGet(PROP_ID_isDefault);
         return _isDefault;
    }

    /**
     * 是否默认SKU: IS_DEFAULT
     */
    public final void setIsDefault(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isDefault,value)){
            this._isDefault = value;
            internalClearRefs(PROP_ID_isDefault);
            
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
    public final app.erp.md.dao.entity.ErpMdTaxRate getTaxRate(){
       return (app.erp.md.dao.entity.ErpMdTaxRate)internalGetRefEntity(PROP_NAME_taxRate);
    }

    public final void setTaxRate(app.erp.md.dao.entity.ErpMdTaxRate refEntity){
   
           if(refEntity == null){
           
                   this.setTaxRateId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_taxRate, refEntity,()->{
           
                           this.setTaxRateId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
