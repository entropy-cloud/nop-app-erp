package app.erp.sal.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.sal.dao.entity.ErpSalPricingRule;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  销售促销规则: erp_sal_pricing_rule
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpSalPricingRule extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 规则名称: RULE_NAME VARCHAR */
    public static final String PROP_NAME_ruleName = "ruleName";
    public static final int PROP_ID_ruleName = 2;
    
    /* 规则编码: RULE_CODE VARCHAR */
    public static final String PROP_NAME_ruleCode = "ruleCode";
    public static final int PROP_ID_ruleCode = 3;
    
    /* 规则类型: RULE_TYPE VARCHAR */
    public static final String PROP_NAME_ruleType = "ruleType";
    public static final int PROP_ID_ruleType = 4;
    
    /* 目标: TARGET_TYPE VARCHAR */
    public static final String PROP_NAME_targetType = "targetType";
    public static final int PROP_ID_targetType = 5;
    
    /* 物料: MATERIAL_ID BIGINT */
    public static final String PROP_NAME_materialId = "materialId";
    public static final int PROP_ID_materialId = 6;
    
    /* 物料分类: MATERIAL_CATEGORY_ID BIGINT */
    public static final String PROP_NAME_materialCategoryId = "materialCategoryId";
    public static final int PROP_ID_materialCategoryId = 7;
    
    /* 客户组: CUSTOMER_GROUP_CODE VARCHAR */
    public static final String PROP_NAME_customerGroupCode = "customerGroupCode";
    public static final int PROP_ID_customerGroupCode = 8;
    
    /* 指定客户: PARTNER_ID BIGINT */
    public static final String PROP_NAME_partnerId = "partnerId";
    public static final int PROP_ID_partnerId = 9;
    
    /* 满减门槛: MIN_ORDER_AMOUNT DECIMAL */
    public static final String PROP_NAME_minOrderAmount = "minOrderAmount";
    public static final int PROP_ID_minOrderAmount = 10;
    
    /* 折扣率(%): DISCOUNT_PERCENT DECIMAL */
    public static final String PROP_NAME_discountPercent = "discountPercent";
    public static final int PROP_ID_discountPercent = 11;
    
    /* 折扣金额: DISCOUNT_AMOUNT DECIMAL */
    public static final String PROP_NAME_discountAmount = "discountAmount";
    public static final int PROP_ID_discountAmount = 12;
    
    /* 赠品物料: GIFT_MATERIAL_ID BIGINT */
    public static final String PROP_NAME_giftMaterialId = "giftMaterialId";
    public static final int PROP_ID_giftMaterialId = 13;
    
    /* 赠品SKU: GIFT_SKU_ID BIGINT */
    public static final String PROP_NAME_giftSkuId = "giftSkuId";
    public static final int PROP_ID_giftSkuId = 14;
    
    /* 赠品数量: GIFT_QUANTITY DECIMAL */
    public static final String PROP_NAME_giftQuantity = "giftQuantity";
    public static final int PROP_ID_giftQuantity = 15;
    
    /* 覆盖单价: PRICE_OVERRIDE DECIMAL */
    public static final String PROP_NAME_priceOverride = "priceOverride";
    public static final int PROP_ID_priceOverride = 16;
    
    /* 币种: CURRENCY_ID BIGINT */
    public static final String PROP_NAME_currencyId = "currencyId";
    public static final int PROP_ID_currencyId = 17;
    
    /* 优先级: PRIORITY INTEGER */
    public static final String PROP_NAME_priority = "priority";
    public static final int PROP_ID_priority = 18;
    
    /* 可叠加: STACKABLE BOOLEAN */
    public static final String PROP_NAME_stackable = "stackable";
    public static final int PROP_ID_stackable = 19;
    
    /* 生效时间: VALID_FROM TIMESTAMP */
    public static final String PROP_NAME_validFrom = "validFrom";
    public static final int PROP_ID_validFrom = 20;
    
    /* 失效时间: VALID_TO TIMESTAMP */
    public static final String PROP_NAME_validTo = "validTo";
    public static final int PROP_ID_validTo = 21;
    
    /* 启用: IS_ACTIVE BOOLEAN */
    public static final String PROP_NAME_isActive = "isActive";
    public static final int PROP_ID_isActive = 22;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 23;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 24;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 25;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 26;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 27;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 28;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 29;
    

    private static int _PROP_ID_BOUND = 30;

    
    /* relation:  */
    public static final String PROP_NAME_material = "material";
    
    /* relation:  */
    public static final String PROP_NAME_giftMaterial = "giftMaterial";
    
    /* relation:  */
    public static final String PROP_NAME_giftSku = "giftSku";
    
    /* relation:  */
    public static final String PROP_NAME_currency = "currency";
    
    /* relation:  */
    public static final String PROP_NAME_partner = "partner";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[30];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_ruleName] = PROP_NAME_ruleName;
          PROP_NAME_TO_ID.put(PROP_NAME_ruleName, PROP_ID_ruleName);
      
          PROP_ID_TO_NAME[PROP_ID_ruleCode] = PROP_NAME_ruleCode;
          PROP_NAME_TO_ID.put(PROP_NAME_ruleCode, PROP_ID_ruleCode);
      
          PROP_ID_TO_NAME[PROP_ID_ruleType] = PROP_NAME_ruleType;
          PROP_NAME_TO_ID.put(PROP_NAME_ruleType, PROP_ID_ruleType);
      
          PROP_ID_TO_NAME[PROP_ID_targetType] = PROP_NAME_targetType;
          PROP_NAME_TO_ID.put(PROP_NAME_targetType, PROP_ID_targetType);
      
          PROP_ID_TO_NAME[PROP_ID_materialId] = PROP_NAME_materialId;
          PROP_NAME_TO_ID.put(PROP_NAME_materialId, PROP_ID_materialId);
      
          PROP_ID_TO_NAME[PROP_ID_materialCategoryId] = PROP_NAME_materialCategoryId;
          PROP_NAME_TO_ID.put(PROP_NAME_materialCategoryId, PROP_ID_materialCategoryId);
      
          PROP_ID_TO_NAME[PROP_ID_customerGroupCode] = PROP_NAME_customerGroupCode;
          PROP_NAME_TO_ID.put(PROP_NAME_customerGroupCode, PROP_ID_customerGroupCode);
      
          PROP_ID_TO_NAME[PROP_ID_partnerId] = PROP_NAME_partnerId;
          PROP_NAME_TO_ID.put(PROP_NAME_partnerId, PROP_ID_partnerId);
      
          PROP_ID_TO_NAME[PROP_ID_minOrderAmount] = PROP_NAME_minOrderAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_minOrderAmount, PROP_ID_minOrderAmount);
      
          PROP_ID_TO_NAME[PROP_ID_discountPercent] = PROP_NAME_discountPercent;
          PROP_NAME_TO_ID.put(PROP_NAME_discountPercent, PROP_ID_discountPercent);
      
          PROP_ID_TO_NAME[PROP_ID_discountAmount] = PROP_NAME_discountAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_discountAmount, PROP_ID_discountAmount);
      
          PROP_ID_TO_NAME[PROP_ID_giftMaterialId] = PROP_NAME_giftMaterialId;
          PROP_NAME_TO_ID.put(PROP_NAME_giftMaterialId, PROP_ID_giftMaterialId);
      
          PROP_ID_TO_NAME[PROP_ID_giftSkuId] = PROP_NAME_giftSkuId;
          PROP_NAME_TO_ID.put(PROP_NAME_giftSkuId, PROP_ID_giftSkuId);
      
          PROP_ID_TO_NAME[PROP_ID_giftQuantity] = PROP_NAME_giftQuantity;
          PROP_NAME_TO_ID.put(PROP_NAME_giftQuantity, PROP_ID_giftQuantity);
      
          PROP_ID_TO_NAME[PROP_ID_priceOverride] = PROP_NAME_priceOverride;
          PROP_NAME_TO_ID.put(PROP_NAME_priceOverride, PROP_ID_priceOverride);
      
          PROP_ID_TO_NAME[PROP_ID_currencyId] = PROP_NAME_currencyId;
          PROP_NAME_TO_ID.put(PROP_NAME_currencyId, PROP_ID_currencyId);
      
          PROP_ID_TO_NAME[PROP_ID_priority] = PROP_NAME_priority;
          PROP_NAME_TO_ID.put(PROP_NAME_priority, PROP_ID_priority);
      
          PROP_ID_TO_NAME[PROP_ID_stackable] = PROP_NAME_stackable;
          PROP_NAME_TO_ID.put(PROP_NAME_stackable, PROP_ID_stackable);
      
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
    
    /* 规则名称: RULE_NAME */
    private java.lang.String _ruleName;
    
    /* 规则编码: RULE_CODE */
    private java.lang.String _ruleCode;
    
    /* 规则类型: RULE_TYPE */
    private java.lang.String _ruleType;
    
    /* 目标: TARGET_TYPE */
    private java.lang.String _targetType;
    
    /* 物料: MATERIAL_ID */
    private java.lang.Long _materialId;
    
    /* 物料分类: MATERIAL_CATEGORY_ID */
    private java.lang.Long _materialCategoryId;
    
    /* 客户组: CUSTOMER_GROUP_CODE */
    private java.lang.String _customerGroupCode;
    
    /* 指定客户: PARTNER_ID */
    private java.lang.Long _partnerId;
    
    /* 满减门槛: MIN_ORDER_AMOUNT */
    private java.math.BigDecimal _minOrderAmount;
    
    /* 折扣率(%): DISCOUNT_PERCENT */
    private java.math.BigDecimal _discountPercent;
    
    /* 折扣金额: DISCOUNT_AMOUNT */
    private java.math.BigDecimal _discountAmount;
    
    /* 赠品物料: GIFT_MATERIAL_ID */
    private java.lang.Long _giftMaterialId;
    
    /* 赠品SKU: GIFT_SKU_ID */
    private java.lang.Long _giftSkuId;
    
    /* 赠品数量: GIFT_QUANTITY */
    private java.math.BigDecimal _giftQuantity;
    
    /* 覆盖单价: PRICE_OVERRIDE */
    private java.math.BigDecimal _priceOverride;
    
    /* 币种: CURRENCY_ID */
    private java.lang.Long _currencyId;
    
    /* 优先级: PRIORITY */
    private java.lang.Integer _priority;
    
    /* 可叠加: STACKABLE */
    private java.lang.Boolean _stackable;
    
    /* 生效时间: VALID_FROM */
    private java.sql.Timestamp _validFrom;
    
    /* 失效时间: VALID_TO */
    private java.sql.Timestamp _validTo;
    
    /* 启用: IS_ACTIVE */
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
    

    public _ErpSalPricingRule(){
        // for debug
    }

    protected ErpSalPricingRule newInstance(){
        ErpSalPricingRule entity = new ErpSalPricingRule();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpSalPricingRule cloneInstance() {
        ErpSalPricingRule entity = newInstance();
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
      return "app.erp.sal.dao.entity.ErpSalPricingRule";
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
        
            case PROP_ID_ruleName:
               return getRuleName();
        
            case PROP_ID_ruleCode:
               return getRuleCode();
        
            case PROP_ID_ruleType:
               return getRuleType();
        
            case PROP_ID_targetType:
               return getTargetType();
        
            case PROP_ID_materialId:
               return getMaterialId();
        
            case PROP_ID_materialCategoryId:
               return getMaterialCategoryId();
        
            case PROP_ID_customerGroupCode:
               return getCustomerGroupCode();
        
            case PROP_ID_partnerId:
               return getPartnerId();
        
            case PROP_ID_minOrderAmount:
               return getMinOrderAmount();
        
            case PROP_ID_discountPercent:
               return getDiscountPercent();
        
            case PROP_ID_discountAmount:
               return getDiscountAmount();
        
            case PROP_ID_giftMaterialId:
               return getGiftMaterialId();
        
            case PROP_ID_giftSkuId:
               return getGiftSkuId();
        
            case PROP_ID_giftQuantity:
               return getGiftQuantity();
        
            case PROP_ID_priceOverride:
               return getPriceOverride();
        
            case PROP_ID_currencyId:
               return getCurrencyId();
        
            case PROP_ID_priority:
               return getPriority();
        
            case PROP_ID_stackable:
               return getStackable();
        
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
        
            case PROP_ID_ruleName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_ruleName));
               }
               setRuleName(typedValue);
               break;
            }
        
            case PROP_ID_ruleCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_ruleCode));
               }
               setRuleCode(typedValue);
               break;
            }
        
            case PROP_ID_ruleType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_ruleType));
               }
               setRuleType(typedValue);
               break;
            }
        
            case PROP_ID_targetType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_targetType));
               }
               setTargetType(typedValue);
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
        
            case PROP_ID_customerGroupCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_customerGroupCode));
               }
               setCustomerGroupCode(typedValue);
               break;
            }
        
            case PROP_ID_partnerId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_partnerId));
               }
               setPartnerId(typedValue);
               break;
            }
        
            case PROP_ID_minOrderAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_minOrderAmount));
               }
               setMinOrderAmount(typedValue);
               break;
            }
        
            case PROP_ID_discountPercent:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_discountPercent));
               }
               setDiscountPercent(typedValue);
               break;
            }
        
            case PROP_ID_discountAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_discountAmount));
               }
               setDiscountAmount(typedValue);
               break;
            }
        
            case PROP_ID_giftMaterialId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_giftMaterialId));
               }
               setGiftMaterialId(typedValue);
               break;
            }
        
            case PROP_ID_giftSkuId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_giftSkuId));
               }
               setGiftSkuId(typedValue);
               break;
            }
        
            case PROP_ID_giftQuantity:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_giftQuantity));
               }
               setGiftQuantity(typedValue);
               break;
            }
        
            case PROP_ID_priceOverride:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_priceOverride));
               }
               setPriceOverride(typedValue);
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
        
            case PROP_ID_priority:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_priority));
               }
               setPriority(typedValue);
               break;
            }
        
            case PROP_ID_stackable:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_stackable));
               }
               setStackable(typedValue);
               break;
            }
        
            case PROP_ID_validFrom:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_validFrom));
               }
               setValidFrom(typedValue);
               break;
            }
        
            case PROP_ID_validTo:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
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
        
            case PROP_ID_ruleName:{
               onInitProp(propId);
               this._ruleName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_ruleCode:{
               onInitProp(propId);
               this._ruleCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_ruleType:{
               onInitProp(propId);
               this._ruleType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_targetType:{
               onInitProp(propId);
               this._targetType = (java.lang.String)value;
               
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
        
            case PROP_ID_customerGroupCode:{
               onInitProp(propId);
               this._customerGroupCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_partnerId:{
               onInitProp(propId);
               this._partnerId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_minOrderAmount:{
               onInitProp(propId);
               this._minOrderAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_discountPercent:{
               onInitProp(propId);
               this._discountPercent = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_discountAmount:{
               onInitProp(propId);
               this._discountAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_giftMaterialId:{
               onInitProp(propId);
               this._giftMaterialId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_giftSkuId:{
               onInitProp(propId);
               this._giftSkuId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_giftQuantity:{
               onInitProp(propId);
               this._giftQuantity = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_priceOverride:{
               onInitProp(propId);
               this._priceOverride = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_currencyId:{
               onInitProp(propId);
               this._currencyId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_priority:{
               onInitProp(propId);
               this._priority = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_stackable:{
               onInitProp(propId);
               this._stackable = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_validFrom:{
               onInitProp(propId);
               this._validFrom = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_validTo:{
               onInitProp(propId);
               this._validTo = (java.sql.Timestamp)value;
               
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
     * 规则名称: RULE_NAME
     */
    public final java.lang.String getRuleName(){
         onPropGet(PROP_ID_ruleName);
         return _ruleName;
    }

    /**
     * 规则名称: RULE_NAME
     */
    public final void setRuleName(java.lang.String value){
        if(onPropSet(PROP_ID_ruleName,value)){
            this._ruleName = value;
            internalClearRefs(PROP_ID_ruleName);
            
        }
    }
    
    /**
     * 规则编码: RULE_CODE
     */
    public final java.lang.String getRuleCode(){
         onPropGet(PROP_ID_ruleCode);
         return _ruleCode;
    }

    /**
     * 规则编码: RULE_CODE
     */
    public final void setRuleCode(java.lang.String value){
        if(onPropSet(PROP_ID_ruleCode,value)){
            this._ruleCode = value;
            internalClearRefs(PROP_ID_ruleCode);
            
        }
    }
    
    /**
     * 规则类型: RULE_TYPE
     */
    public final java.lang.String getRuleType(){
         onPropGet(PROP_ID_ruleType);
         return _ruleType;
    }

    /**
     * 规则类型: RULE_TYPE
     */
    public final void setRuleType(java.lang.String value){
        if(onPropSet(PROP_ID_ruleType,value)){
            this._ruleType = value;
            internalClearRefs(PROP_ID_ruleType);
            
        }
    }
    
    /**
     * 目标: TARGET_TYPE
     */
    public final java.lang.String getTargetType(){
         onPropGet(PROP_ID_targetType);
         return _targetType;
    }

    /**
     * 目标: TARGET_TYPE
     */
    public final void setTargetType(java.lang.String value){
        if(onPropSet(PROP_ID_targetType,value)){
            this._targetType = value;
            internalClearRefs(PROP_ID_targetType);
            
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
     * 物料分类: MATERIAL_CATEGORY_ID
     */
    public final java.lang.Long getMaterialCategoryId(){
         onPropGet(PROP_ID_materialCategoryId);
         return _materialCategoryId;
    }

    /**
     * 物料分类: MATERIAL_CATEGORY_ID
     */
    public final void setMaterialCategoryId(java.lang.Long value){
        if(onPropSet(PROP_ID_materialCategoryId,value)){
            this._materialCategoryId = value;
            internalClearRefs(PROP_ID_materialCategoryId);
            
        }
    }
    
    /**
     * 客户组: CUSTOMER_GROUP_CODE
     */
    public final java.lang.String getCustomerGroupCode(){
         onPropGet(PROP_ID_customerGroupCode);
         return _customerGroupCode;
    }

    /**
     * 客户组: CUSTOMER_GROUP_CODE
     */
    public final void setCustomerGroupCode(java.lang.String value){
        if(onPropSet(PROP_ID_customerGroupCode,value)){
            this._customerGroupCode = value;
            internalClearRefs(PROP_ID_customerGroupCode);
            
        }
    }
    
    /**
     * 指定客户: PARTNER_ID
     */
    public final java.lang.Long getPartnerId(){
         onPropGet(PROP_ID_partnerId);
         return _partnerId;
    }

    /**
     * 指定客户: PARTNER_ID
     */
    public final void setPartnerId(java.lang.Long value){
        if(onPropSet(PROP_ID_partnerId,value)){
            this._partnerId = value;
            internalClearRefs(PROP_ID_partnerId);
            
        }
    }
    
    /**
     * 满减门槛: MIN_ORDER_AMOUNT
     */
    public final java.math.BigDecimal getMinOrderAmount(){
         onPropGet(PROP_ID_minOrderAmount);
         return _minOrderAmount;
    }

    /**
     * 满减门槛: MIN_ORDER_AMOUNT
     */
    public final void setMinOrderAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_minOrderAmount,value)){
            this._minOrderAmount = value;
            internalClearRefs(PROP_ID_minOrderAmount);
            
        }
    }
    
    /**
     * 折扣率(%): DISCOUNT_PERCENT
     */
    public final java.math.BigDecimal getDiscountPercent(){
         onPropGet(PROP_ID_discountPercent);
         return _discountPercent;
    }

    /**
     * 折扣率(%): DISCOUNT_PERCENT
     */
    public final void setDiscountPercent(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_discountPercent,value)){
            this._discountPercent = value;
            internalClearRefs(PROP_ID_discountPercent);
            
        }
    }
    
    /**
     * 折扣金额: DISCOUNT_AMOUNT
     */
    public final java.math.BigDecimal getDiscountAmount(){
         onPropGet(PROP_ID_discountAmount);
         return _discountAmount;
    }

    /**
     * 折扣金额: DISCOUNT_AMOUNT
     */
    public final void setDiscountAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_discountAmount,value)){
            this._discountAmount = value;
            internalClearRefs(PROP_ID_discountAmount);
            
        }
    }
    
    /**
     * 赠品物料: GIFT_MATERIAL_ID
     */
    public final java.lang.Long getGiftMaterialId(){
         onPropGet(PROP_ID_giftMaterialId);
         return _giftMaterialId;
    }

    /**
     * 赠品物料: GIFT_MATERIAL_ID
     */
    public final void setGiftMaterialId(java.lang.Long value){
        if(onPropSet(PROP_ID_giftMaterialId,value)){
            this._giftMaterialId = value;
            internalClearRefs(PROP_ID_giftMaterialId);
            
        }
    }
    
    /**
     * 赠品SKU: GIFT_SKU_ID
     */
    public final java.lang.Long getGiftSkuId(){
         onPropGet(PROP_ID_giftSkuId);
         return _giftSkuId;
    }

    /**
     * 赠品SKU: GIFT_SKU_ID
     */
    public final void setGiftSkuId(java.lang.Long value){
        if(onPropSet(PROP_ID_giftSkuId,value)){
            this._giftSkuId = value;
            internalClearRefs(PROP_ID_giftSkuId);
            
        }
    }
    
    /**
     * 赠品数量: GIFT_QUANTITY
     */
    public final java.math.BigDecimal getGiftQuantity(){
         onPropGet(PROP_ID_giftQuantity);
         return _giftQuantity;
    }

    /**
     * 赠品数量: GIFT_QUANTITY
     */
    public final void setGiftQuantity(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_giftQuantity,value)){
            this._giftQuantity = value;
            internalClearRefs(PROP_ID_giftQuantity);
            
        }
    }
    
    /**
     * 覆盖单价: PRICE_OVERRIDE
     */
    public final java.math.BigDecimal getPriceOverride(){
         onPropGet(PROP_ID_priceOverride);
         return _priceOverride;
    }

    /**
     * 覆盖单价: PRICE_OVERRIDE
     */
    public final void setPriceOverride(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_priceOverride,value)){
            this._priceOverride = value;
            internalClearRefs(PROP_ID_priceOverride);
            
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
     * 优先级: PRIORITY
     */
    public final java.lang.Integer getPriority(){
         onPropGet(PROP_ID_priority);
         return _priority;
    }

    /**
     * 优先级: PRIORITY
     */
    public final void setPriority(java.lang.Integer value){
        if(onPropSet(PROP_ID_priority,value)){
            this._priority = value;
            internalClearRefs(PROP_ID_priority);
            
        }
    }
    
    /**
     * 可叠加: STACKABLE
     */
    public final java.lang.Boolean getStackable(){
         onPropGet(PROP_ID_stackable);
         return _stackable;
    }

    /**
     * 可叠加: STACKABLE
     */
    public final void setStackable(java.lang.Boolean value){
        if(onPropSet(PROP_ID_stackable,value)){
            this._stackable = value;
            internalClearRefs(PROP_ID_stackable);
            
        }
    }
    
    /**
     * 生效时间: VALID_FROM
     */
    public final java.sql.Timestamp getValidFrom(){
         onPropGet(PROP_ID_validFrom);
         return _validFrom;
    }

    /**
     * 生效时间: VALID_FROM
     */
    public final void setValidFrom(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_validFrom,value)){
            this._validFrom = value;
            internalClearRefs(PROP_ID_validFrom);
            
        }
    }
    
    /**
     * 失效时间: VALID_TO
     */
    public final java.sql.Timestamp getValidTo(){
         onPropGet(PROP_ID_validTo);
         return _validTo;
    }

    /**
     * 失效时间: VALID_TO
     */
    public final void setValidTo(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_validTo,value)){
            this._validTo = value;
            internalClearRefs(PROP_ID_validTo);
            
        }
    }
    
    /**
     * 启用: IS_ACTIVE
     */
    public final java.lang.Boolean getIsActive(){
         onPropGet(PROP_ID_isActive);
         return _isActive;
    }

    /**
     * 启用: IS_ACTIVE
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
    public final app.erp.md.dao.entity.ErpMdMaterial getGiftMaterial(){
       return (app.erp.md.dao.entity.ErpMdMaterial)internalGetRefEntity(PROP_NAME_giftMaterial);
    }

    public final void setGiftMaterial(app.erp.md.dao.entity.ErpMdMaterial refEntity){
   
           if(refEntity == null){
           
                   this.setGiftMaterialId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_giftMaterial, refEntity,()->{
           
                           this.setGiftMaterialId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdMaterialSku getGiftSku(){
       return (app.erp.md.dao.entity.ErpMdMaterialSku)internalGetRefEntity(PROP_NAME_giftSku);
    }

    public final void setGiftSku(app.erp.md.dao.entity.ErpMdMaterialSku refEntity){
   
           if(refEntity == null){
           
                   this.setGiftSkuId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_giftSku, refEntity,()->{
           
                           this.setGiftSkuId(refEntity.getId());
                       
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
    public final app.erp.md.dao.entity.ErpMdPartner getPartner(){
       return (app.erp.md.dao.entity.ErpMdPartner)internalGetRefEntity(PROP_NAME_partner);
    }

    public final void setPartner(app.erp.md.dao.entity.ErpMdPartner refEntity){
   
           if(refEntity == null){
           
                   this.setPartnerId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_partner, refEntity,()->{
           
                           this.setPartnerId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
