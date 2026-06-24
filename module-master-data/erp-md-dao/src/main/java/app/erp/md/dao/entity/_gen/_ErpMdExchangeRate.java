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

import app.erp.md.dao.entity.ErpMdExchangeRate;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  汇率: erp_md_exchange_rate
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpMdExchangeRate extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 源币种: FROM_CURRENCY_ID BIGINT */
    public static final String PROP_NAME_fromCurrencyId = "fromCurrencyId";
    public static final int PROP_ID_fromCurrencyId = 2;
    
    /* 目标币种: TO_CURRENCY_ID BIGINT */
    public static final String PROP_NAME_toCurrencyId = "toCurrencyId";
    public static final int PROP_ID_toCurrencyId = 3;
    
    /* 汇率类型: RATE_TYPE VARCHAR */
    public static final String PROP_NAME_rateType = "rateType";
    public static final int PROP_ID_rateType = 4;
    
    /* 汇率: RATE DECIMAL */
    public static final String PROP_NAME_rate = "rate";
    public static final int PROP_ID_rate = 5;
    
    /* 生效日期: VALID_FROM DATE */
    public static final String PROP_NAME_validFrom = "validFrom";
    public static final int PROP_ID_validFrom = 6;
    
    /* 失效日期: VALID_TO DATE */
    public static final String PROP_NAME_validTo = "validTo";
    public static final int PROP_ID_validTo = 7;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 8;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 9;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 10;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 11;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 12;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 13;
    

    private static int _PROP_ID_BOUND = 14;

    
    /* relation:  */
    public static final String PROP_NAME_fromCurrency = "fromCurrency";
    
    /* relation:  */
    public static final String PROP_NAME_toCurrency = "toCurrency";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[14];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_fromCurrencyId] = PROP_NAME_fromCurrencyId;
          PROP_NAME_TO_ID.put(PROP_NAME_fromCurrencyId, PROP_ID_fromCurrencyId);
      
          PROP_ID_TO_NAME[PROP_ID_toCurrencyId] = PROP_NAME_toCurrencyId;
          PROP_NAME_TO_ID.put(PROP_NAME_toCurrencyId, PROP_ID_toCurrencyId);
      
          PROP_ID_TO_NAME[PROP_ID_rateType] = PROP_NAME_rateType;
          PROP_NAME_TO_ID.put(PROP_NAME_rateType, PROP_ID_rateType);
      
          PROP_ID_TO_NAME[PROP_ID_rate] = PROP_NAME_rate;
          PROP_NAME_TO_ID.put(PROP_NAME_rate, PROP_ID_rate);
      
          PROP_ID_TO_NAME[PROP_ID_validFrom] = PROP_NAME_validFrom;
          PROP_NAME_TO_ID.put(PROP_NAME_validFrom, PROP_ID_validFrom);
      
          PROP_ID_TO_NAME[PROP_ID_validTo] = PROP_NAME_validTo;
          PROP_NAME_TO_ID.put(PROP_NAME_validTo, PROP_ID_validTo);
      
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
    
    /* 源币种: FROM_CURRENCY_ID */
    private java.lang.Long _fromCurrencyId;
    
    /* 目标币种: TO_CURRENCY_ID */
    private java.lang.Long _toCurrencyId;
    
    /* 汇率类型: RATE_TYPE */
    private java.lang.String _rateType;
    
    /* 汇率: RATE */
    private java.math.BigDecimal _rate;
    
    /* 生效日期: VALID_FROM */
    private java.time.LocalDate _validFrom;
    
    /* 失效日期: VALID_TO */
    private java.time.LocalDate _validTo;
    
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
    

    public _ErpMdExchangeRate(){
        // for debug
    }

    protected ErpMdExchangeRate newInstance(){
        ErpMdExchangeRate entity = new ErpMdExchangeRate();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpMdExchangeRate cloneInstance() {
        ErpMdExchangeRate entity = newInstance();
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
      return "app.erp.md.dao.entity.ErpMdExchangeRate";
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
        
            case PROP_ID_fromCurrencyId:
               return getFromCurrencyId();
        
            case PROP_ID_toCurrencyId:
               return getToCurrencyId();
        
            case PROP_ID_rateType:
               return getRateType();
        
            case PROP_ID_rate:
               return getRate();
        
            case PROP_ID_validFrom:
               return getValidFrom();
        
            case PROP_ID_validTo:
               return getValidTo();
        
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
        
            case PROP_ID_fromCurrencyId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_fromCurrencyId));
               }
               setFromCurrencyId(typedValue);
               break;
            }
        
            case PROP_ID_toCurrencyId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_toCurrencyId));
               }
               setToCurrencyId(typedValue);
               break;
            }
        
            case PROP_ID_rateType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_rateType));
               }
               setRateType(typedValue);
               break;
            }
        
            case PROP_ID_rate:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_rate));
               }
               setRate(typedValue);
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
        
            case PROP_ID_fromCurrencyId:{
               onInitProp(propId);
               this._fromCurrencyId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_toCurrencyId:{
               onInitProp(propId);
               this._toCurrencyId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_rateType:{
               onInitProp(propId);
               this._rateType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_rate:{
               onInitProp(propId);
               this._rate = (java.math.BigDecimal)value;
               
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
     * 源币种: FROM_CURRENCY_ID
     */
    public final java.lang.Long getFromCurrencyId(){
         onPropGet(PROP_ID_fromCurrencyId);
         return _fromCurrencyId;
    }

    /**
     * 源币种: FROM_CURRENCY_ID
     */
    public final void setFromCurrencyId(java.lang.Long value){
        if(onPropSet(PROP_ID_fromCurrencyId,value)){
            this._fromCurrencyId = value;
            internalClearRefs(PROP_ID_fromCurrencyId);
            
        }
    }
    
    /**
     * 目标币种: TO_CURRENCY_ID
     */
    public final java.lang.Long getToCurrencyId(){
         onPropGet(PROP_ID_toCurrencyId);
         return _toCurrencyId;
    }

    /**
     * 目标币种: TO_CURRENCY_ID
     */
    public final void setToCurrencyId(java.lang.Long value){
        if(onPropSet(PROP_ID_toCurrencyId,value)){
            this._toCurrencyId = value;
            internalClearRefs(PROP_ID_toCurrencyId);
            
        }
    }
    
    /**
     * 汇率类型: RATE_TYPE
     */
    public final java.lang.String getRateType(){
         onPropGet(PROP_ID_rateType);
         return _rateType;
    }

    /**
     * 汇率类型: RATE_TYPE
     */
    public final void setRateType(java.lang.String value){
        if(onPropSet(PROP_ID_rateType,value)){
            this._rateType = value;
            internalClearRefs(PROP_ID_rateType);
            
        }
    }
    
    /**
     * 汇率: RATE
     */
    public final java.math.BigDecimal getRate(){
         onPropGet(PROP_ID_rate);
         return _rate;
    }

    /**
     * 汇率: RATE
     */
    public final void setRate(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_rate,value)){
            this._rate = value;
            internalClearRefs(PROP_ID_rate);
            
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
    public final app.erp.md.dao.entity.ErpMdCurrency getFromCurrency(){
       return (app.erp.md.dao.entity.ErpMdCurrency)internalGetRefEntity(PROP_NAME_fromCurrency);
    }

    public final void setFromCurrency(app.erp.md.dao.entity.ErpMdCurrency refEntity){
   
           if(refEntity == null){
           
                   this.setFromCurrencyId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_fromCurrency, refEntity,()->{
           
                           this.setFromCurrencyId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdCurrency getToCurrency(){
       return (app.erp.md.dao.entity.ErpMdCurrency)internalGetRefEntity(PROP_NAME_toCurrency);
    }

    public final void setToCurrency(app.erp.md.dao.entity.ErpMdCurrency refEntity){
   
           if(refEntity == null){
           
                   this.setToCurrencyId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_toCurrency, refEntity,()->{
           
                           this.setToCurrencyId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
