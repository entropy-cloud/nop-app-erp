package app.erp.fin.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.fin.dao.entity.ErpFinIntercompanyTransferPrice;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  跨法人转移定价规则: erp_fin_intercompany_transfer_price
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpFinIntercompanyTransferPrice extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 规则编码: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 规则名称: NAME VARCHAR */
    public static final String PROP_NAME_name = "name";
    public static final int PROP_ID_name = 3;
    
    /* 核算组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 4;
    
    /* 调出组织(空=通配): FROM_ORG_ID BIGINT */
    public static final String PROP_NAME_fromOrgId = "fromOrgId";
    public static final int PROP_ID_fromOrgId = 5;
    
    /* 调入组织(空=通配): TO_ORG_ID BIGINT */
    public static final String PROP_NAME_toOrgId = "toOrgId";
    public static final int PROP_ID_toOrgId = 6;
    
    /* 物料(空=通配): MATERIAL_ID BIGINT */
    public static final String PROP_NAME_materialId = "materialId";
    public static final int PROP_ID_materialId = 7;
    
    /* 物料分类(空=通配): MATERIAL_CATEGORY_ID BIGINT */
    public static final String PROP_NAME_materialCategoryId = "materialCategoryId";
    public static final int PROP_ID_materialCategoryId = 8;
    
    /* 定价方法: PRICING_METHOD VARCHAR */
    public static final String PROP_NAME_pricingMethod = "pricingMethod";
    public static final int PROP_ID_pricingMethod = 9;
    
    /* 加成率(COST_PLUS): MARKUP_RATE DECIMAL */
    public static final String PROP_NAME_markupRate = "markupRate";
    public static final int PROP_ID_markupRate = 10;
    
    /* 固定单价(NEGOTIATED/MARKET兜底): FIXED_PRICE DECIMAL */
    public static final String PROP_NAME_fixedPrice = "fixedPrice";
    public static final int PROP_ID_fixedPrice = 11;
    
    /* 市场价来源说明(MARKET): MARKET_REF_SOURCE VARCHAR */
    public static final String PROP_NAME_marketRefSource = "marketRefSource";
    public static final int PROP_ID_marketRefSource = 12;
    
    /* 生效日期: VALID_FROM DATE */
    public static final String PROP_NAME_validFrom = "validFrom";
    public static final int PROP_ID_validFrom = 13;
    
    /* 失效日期: VALID_TO DATE */
    public static final String PROP_NAME_validTo = "validTo";
    public static final int PROP_ID_validTo = 14;
    
    /* 是否启用: IS_ACTIVE BOOLEAN */
    public static final String PROP_NAME_isActive = "isActive";
    public static final int PROP_ID_isActive = 15;
    
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
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 22;
    

    private static int _PROP_ID_BOUND = 23;

    
    /* relation:  */
    public static final String PROP_NAME_fromOrg = "fromOrg";
    
    /* relation:  */
    public static final String PROP_NAME_toOrg = "toOrg";
    
    /* relation:  */
    public static final String PROP_NAME_material = "material";
    
    /* relation:  */
    public static final String PROP_NAME_materialCategory = "materialCategory";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[23];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_name] = PROP_NAME_name;
          PROP_NAME_TO_ID.put(PROP_NAME_name, PROP_ID_name);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_fromOrgId] = PROP_NAME_fromOrgId;
          PROP_NAME_TO_ID.put(PROP_NAME_fromOrgId, PROP_ID_fromOrgId);
      
          PROP_ID_TO_NAME[PROP_ID_toOrgId] = PROP_NAME_toOrgId;
          PROP_NAME_TO_ID.put(PROP_NAME_toOrgId, PROP_ID_toOrgId);
      
          PROP_ID_TO_NAME[PROP_ID_materialId] = PROP_NAME_materialId;
          PROP_NAME_TO_ID.put(PROP_NAME_materialId, PROP_ID_materialId);
      
          PROP_ID_TO_NAME[PROP_ID_materialCategoryId] = PROP_NAME_materialCategoryId;
          PROP_NAME_TO_ID.put(PROP_NAME_materialCategoryId, PROP_ID_materialCategoryId);
      
          PROP_ID_TO_NAME[PROP_ID_pricingMethod] = PROP_NAME_pricingMethod;
          PROP_NAME_TO_ID.put(PROP_NAME_pricingMethod, PROP_ID_pricingMethod);
      
          PROP_ID_TO_NAME[PROP_ID_markupRate] = PROP_NAME_markupRate;
          PROP_NAME_TO_ID.put(PROP_NAME_markupRate, PROP_ID_markupRate);
      
          PROP_ID_TO_NAME[PROP_ID_fixedPrice] = PROP_NAME_fixedPrice;
          PROP_NAME_TO_ID.put(PROP_NAME_fixedPrice, PROP_ID_fixedPrice);
      
          PROP_ID_TO_NAME[PROP_ID_marketRefSource] = PROP_NAME_marketRefSource;
          PROP_NAME_TO_ID.put(PROP_NAME_marketRefSource, PROP_ID_marketRefSource);
      
          PROP_ID_TO_NAME[PROP_ID_validFrom] = PROP_NAME_validFrom;
          PROP_NAME_TO_ID.put(PROP_NAME_validFrom, PROP_ID_validFrom);
      
          PROP_ID_TO_NAME[PROP_ID_validTo] = PROP_NAME_validTo;
          PROP_NAME_TO_ID.put(PROP_NAME_validTo, PROP_ID_validTo);
      
          PROP_ID_TO_NAME[PROP_ID_isActive] = PROP_NAME_isActive;
          PROP_NAME_TO_ID.put(PROP_NAME_isActive, PROP_ID_isActive);
      
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
    
    /* 规则编码: CODE */
    private java.lang.String _code;
    
    /* 规则名称: NAME */
    private java.lang.String _name;
    
    /* 核算组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 调出组织(空=通配): FROM_ORG_ID */
    private java.lang.Long _fromOrgId;
    
    /* 调入组织(空=通配): TO_ORG_ID */
    private java.lang.Long _toOrgId;
    
    /* 物料(空=通配): MATERIAL_ID */
    private java.lang.Long _materialId;
    
    /* 物料分类(空=通配): MATERIAL_CATEGORY_ID */
    private java.lang.Long _materialCategoryId;
    
    /* 定价方法: PRICING_METHOD */
    private java.lang.String _pricingMethod;
    
    /* 加成率(COST_PLUS): MARKUP_RATE */
    private java.math.BigDecimal _markupRate;
    
    /* 固定单价(NEGOTIATED/MARKET兜底): FIXED_PRICE */
    private java.math.BigDecimal _fixedPrice;
    
    /* 市场价来源说明(MARKET): MARKET_REF_SOURCE */
    private java.lang.String _marketRefSource;
    
    /* 生效日期: VALID_FROM */
    private java.time.LocalDate _validFrom;
    
    /* 失效日期: VALID_TO */
    private java.time.LocalDate _validTo;
    
    /* 是否启用: IS_ACTIVE */
    private java.lang.Boolean _isActive;
    
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
    

    public _ErpFinIntercompanyTransferPrice(){
        // for debug
    }

    protected ErpFinIntercompanyTransferPrice newInstance(){
        ErpFinIntercompanyTransferPrice entity = new ErpFinIntercompanyTransferPrice();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpFinIntercompanyTransferPrice cloneInstance() {
        ErpFinIntercompanyTransferPrice entity = newInstance();
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
      return "app.erp.fin.dao.entity.ErpFinIntercompanyTransferPrice";
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
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_fromOrgId:
               return getFromOrgId();
        
            case PROP_ID_toOrgId:
               return getToOrgId();
        
            case PROP_ID_materialId:
               return getMaterialId();
        
            case PROP_ID_materialCategoryId:
               return getMaterialCategoryId();
        
            case PROP_ID_pricingMethod:
               return getPricingMethod();
        
            case PROP_ID_markupRate:
               return getMarkupRate();
        
            case PROP_ID_fixedPrice:
               return getFixedPrice();
        
            case PROP_ID_marketRefSource:
               return getMarketRefSource();
        
            case PROP_ID_validFrom:
               return getValidFrom();
        
            case PROP_ID_validTo:
               return getValidTo();
        
            case PROP_ID_isActive:
               return getIsActive();
        
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
        
            case PROP_ID_orgId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_orgId));
               }
               setOrgId(typedValue);
               break;
            }
        
            case PROP_ID_fromOrgId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_fromOrgId));
               }
               setFromOrgId(typedValue);
               break;
            }
        
            case PROP_ID_toOrgId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_toOrgId));
               }
               setToOrgId(typedValue);
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
        
            case PROP_ID_materialCategoryId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_materialCategoryId));
               }
               setMaterialCategoryId(typedValue);
               break;
            }
        
            case PROP_ID_pricingMethod:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_pricingMethod));
               }
               setPricingMethod(typedValue);
               break;
            }
        
            case PROP_ID_markupRate:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_markupRate));
               }
               setMarkupRate(typedValue);
               break;
            }
        
            case PROP_ID_fixedPrice:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_fixedPrice));
               }
               setFixedPrice(typedValue);
               break;
            }
        
            case PROP_ID_marketRefSource:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_marketRefSource));
               }
               setMarketRefSource(typedValue);
               break;
            }
        
            case PROP_ID_validFrom:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_validFrom));
               }
               setValidFrom(typedValue);
               break;
            }
        
            case PROP_ID_validTo:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_validTo));
               }
               setValidTo(typedValue);
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
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_fromOrgId:{
               onInitProp(propId);
               this._fromOrgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_toOrgId:{
               onInitProp(propId);
               this._toOrgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_materialId:{
               onInitProp(propId);
               this._materialId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_materialCategoryId:{
               onInitProp(propId);
               this._materialCategoryId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_pricingMethod:{
               onInitProp(propId);
               this._pricingMethod = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_markupRate:{
               onInitProp(propId);
               this._markupRate = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_fixedPrice:{
               onInitProp(propId);
               this._fixedPrice = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_marketRefSource:{
               onInitProp(propId);
               this._marketRefSource = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_validFrom:{
               onInitProp(propId);
               this._validFrom = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_validTo:{
               onInitProp(propId);
               this._validTo = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_isActive:{
               onInitProp(propId);
               this._isActive = (java.lang.Boolean)value;
               
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
     * 规则编码: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 规则编码: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
        }
    }
    
    /**
     * 规则名称: NAME
     */
    public final java.lang.String getName(){
         onPropGet(PROP_ID_name);
         return _name;
    }

    /**
     * 规则名称: NAME
     */
    public final void setName(java.lang.String value){
        if(onPropSet(PROP_ID_name,value)){
            this._name = value;
            internalClearRefs(PROP_ID_name);
            
        }
    }
    
    /**
     * 核算组织: ORG_ID
     */
    public final java.lang.Long getOrgId(){
         onPropGet(PROP_ID_orgId);
         return _orgId;
    }

    /**
     * 核算组织: ORG_ID
     */
    public final void setOrgId(java.lang.Long value){
        if(onPropSet(PROP_ID_orgId,value)){
            this._orgId = value;
            internalClearRefs(PROP_ID_orgId);
            
        }
    }
    
    /**
     * 调出组织(空=通配): FROM_ORG_ID
     */
    public final java.lang.Long getFromOrgId(){
         onPropGet(PROP_ID_fromOrgId);
         return _fromOrgId;
    }

    /**
     * 调出组织(空=通配): FROM_ORG_ID
     */
    public final void setFromOrgId(java.lang.Long value){
        if(onPropSet(PROP_ID_fromOrgId,value)){
            this._fromOrgId = value;
            internalClearRefs(PROP_ID_fromOrgId);
            
        }
    }
    
    /**
     * 调入组织(空=通配): TO_ORG_ID
     */
    public final java.lang.Long getToOrgId(){
         onPropGet(PROP_ID_toOrgId);
         return _toOrgId;
    }

    /**
     * 调入组织(空=通配): TO_ORG_ID
     */
    public final void setToOrgId(java.lang.Long value){
        if(onPropSet(PROP_ID_toOrgId,value)){
            this._toOrgId = value;
            internalClearRefs(PROP_ID_toOrgId);
            
        }
    }
    
    /**
     * 物料(空=通配): MATERIAL_ID
     */
    public final java.lang.Long getMaterialId(){
         onPropGet(PROP_ID_materialId);
         return _materialId;
    }

    /**
     * 物料(空=通配): MATERIAL_ID
     */
    public final void setMaterialId(java.lang.Long value){
        if(onPropSet(PROP_ID_materialId,value)){
            this._materialId = value;
            internalClearRefs(PROP_ID_materialId);
            
        }
    }
    
    /**
     * 物料分类(空=通配): MATERIAL_CATEGORY_ID
     */
    public final java.lang.Long getMaterialCategoryId(){
         onPropGet(PROP_ID_materialCategoryId);
         return _materialCategoryId;
    }

    /**
     * 物料分类(空=通配): MATERIAL_CATEGORY_ID
     */
    public final void setMaterialCategoryId(java.lang.Long value){
        if(onPropSet(PROP_ID_materialCategoryId,value)){
            this._materialCategoryId = value;
            internalClearRefs(PROP_ID_materialCategoryId);
            
        }
    }
    
    /**
     * 定价方法: PRICING_METHOD
     */
    public final java.lang.String getPricingMethod(){
         onPropGet(PROP_ID_pricingMethod);
         return _pricingMethod;
    }

    /**
     * 定价方法: PRICING_METHOD
     */
    public final void setPricingMethod(java.lang.String value){
        if(onPropSet(PROP_ID_pricingMethod,value)){
            this._pricingMethod = value;
            internalClearRefs(PROP_ID_pricingMethod);
            
        }
    }
    
    /**
     * 加成率(COST_PLUS): MARKUP_RATE
     */
    public final java.math.BigDecimal getMarkupRate(){
         onPropGet(PROP_ID_markupRate);
         return _markupRate;
    }

    /**
     * 加成率(COST_PLUS): MARKUP_RATE
     */
    public final void setMarkupRate(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_markupRate,value)){
            this._markupRate = value;
            internalClearRefs(PROP_ID_markupRate);
            
        }
    }
    
    /**
     * 固定单价(NEGOTIATED/MARKET兜底): FIXED_PRICE
     */
    public final java.math.BigDecimal getFixedPrice(){
         onPropGet(PROP_ID_fixedPrice);
         return _fixedPrice;
    }

    /**
     * 固定单价(NEGOTIATED/MARKET兜底): FIXED_PRICE
     */
    public final void setFixedPrice(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_fixedPrice,value)){
            this._fixedPrice = value;
            internalClearRefs(PROP_ID_fixedPrice);
            
        }
    }
    
    /**
     * 市场价来源说明(MARKET): MARKET_REF_SOURCE
     */
    public final java.lang.String getMarketRefSource(){
         onPropGet(PROP_ID_marketRefSource);
         return _marketRefSource;
    }

    /**
     * 市场价来源说明(MARKET): MARKET_REF_SOURCE
     */
    public final void setMarketRefSource(java.lang.String value){
        if(onPropSet(PROP_ID_marketRefSource,value)){
            this._marketRefSource = value;
            internalClearRefs(PROP_ID_marketRefSource);
            
        }
    }
    
    /**
     * 生效日期: VALID_FROM
     */
    public final java.time.LocalDate getValidFrom(){
         onPropGet(PROP_ID_validFrom);
         return _validFrom;
    }

    /**
     * 生效日期: VALID_FROM
     */
    public final void setValidFrom(java.time.LocalDate value){
        if(onPropSet(PROP_ID_validFrom,value)){
            this._validFrom = value;
            internalClearRefs(PROP_ID_validFrom);
            
        }
    }
    
    /**
     * 失效日期: VALID_TO
     */
    public final java.time.LocalDate getValidTo(){
         onPropGet(PROP_ID_validTo);
         return _validTo;
    }

    /**
     * 失效日期: VALID_TO
     */
    public final void setValidTo(java.time.LocalDate value){
        if(onPropSet(PROP_ID_validTo,value)){
            this._validTo = value;
            internalClearRefs(PROP_ID_validTo);
            
        }
    }
    
    /**
     * 是否启用: IS_ACTIVE
     */
    public final java.lang.Boolean getIsActive(){
         onPropGet(PROP_ID_isActive);
         return _isActive;
    }

    /**
     * 是否启用: IS_ACTIVE
     */
    public final void setIsActive(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isActive,value)){
            this._isActive = value;
            internalClearRefs(PROP_ID_isActive);
            
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
    public final app.erp.md.dao.entity.ErpMdOrganization getFromOrg(){
       return (app.erp.md.dao.entity.ErpMdOrganization)internalGetRefEntity(PROP_NAME_fromOrg);
    }

    public final void setFromOrg(app.erp.md.dao.entity.ErpMdOrganization refEntity){
   
           if(refEntity == null){
           
                   this.setFromOrgId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_fromOrg, refEntity,()->{
           
                           this.setFromOrgId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdOrganization getToOrg(){
       return (app.erp.md.dao.entity.ErpMdOrganization)internalGetRefEntity(PROP_NAME_toOrg);
    }

    public final void setToOrg(app.erp.md.dao.entity.ErpMdOrganization refEntity){
   
           if(refEntity == null){
           
                   this.setToOrgId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_toOrg, refEntity,()->{
           
                           this.setToOrgId(refEntity.getId());
                       
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
    public final app.erp.md.dao.entity.ErpMdMaterialCategory getMaterialCategory(){
       return (app.erp.md.dao.entity.ErpMdMaterialCategory)internalGetRefEntity(PROP_NAME_materialCategory);
    }

    public final void setMaterialCategory(app.erp.md.dao.entity.ErpMdMaterialCategory refEntity){
   
           if(refEntity == null){
           
                   this.setMaterialCategoryId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_materialCategory, refEntity,()->{
           
                           this.setMaterialCategoryId(refEntity.getId());
                       
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
