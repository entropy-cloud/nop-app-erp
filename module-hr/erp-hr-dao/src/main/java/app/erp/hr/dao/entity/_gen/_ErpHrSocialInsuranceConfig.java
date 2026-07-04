package app.erp.hr.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.hr.dao.entity.ErpHrSocialInsuranceConfig;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  社保配置: erp_hr_social_insurance_config
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpHrSocialInsuranceConfig extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 城市: CITY_CODE VARCHAR */
    public static final String PROP_NAME_cityCode = "cityCode";
    public static final int PROP_ID_cityCode = 2;
    
    /* 险种: INSURANCE_TYPE VARCHAR */
    public static final String PROP_NAME_insuranceType = "insuranceType";
    public static final int PROP_ID_insuranceType = 3;
    
    /* 公司比例: COMPANY_RATE DECIMAL */
    public static final String PROP_NAME_companyRate = "companyRate";
    public static final int PROP_ID_companyRate = 4;
    
    /* 个人比例: EMPLOYEE_RATE DECIMAL */
    public static final String PROP_NAME_employeeRate = "employeeRate";
    public static final int PROP_ID_employeeRate = 5;
    
    /* 基数下限: BASE_LOWER_LIMIT DECIMAL */
    public static final String PROP_NAME_baseLowerLimit = "baseLowerLimit";
    public static final int PROP_ID_baseLowerLimit = 6;
    
    /* 基数上限: BASE_UPPER_LIMIT DECIMAL */
    public static final String PROP_NAME_baseUpperLimit = "baseUpperLimit";
    public static final int PROP_ID_baseUpperLimit = 7;
    
    /* 生效日期: EFFECTIVE_FROM DATE */
    public static final String PROP_NAME_effectiveFrom = "effectiveFrom";
    public static final int PROP_ID_effectiveFrom = 8;
    
    /* 失效日期: EFFECTIVE_TO DATE */
    public static final String PROP_NAME_effectiveTo = "effectiveTo";
    public static final int PROP_ID_effectiveTo = 9;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 10;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 11;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 12;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 13;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 14;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 15;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 16;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 17;
    

    private static int _PROP_ID_BOUND = 18;

    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[18];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_cityCode] = PROP_NAME_cityCode;
          PROP_NAME_TO_ID.put(PROP_NAME_cityCode, PROP_ID_cityCode);
      
          PROP_ID_TO_NAME[PROP_ID_insuranceType] = PROP_NAME_insuranceType;
          PROP_NAME_TO_ID.put(PROP_NAME_insuranceType, PROP_ID_insuranceType);
      
          PROP_ID_TO_NAME[PROP_ID_companyRate] = PROP_NAME_companyRate;
          PROP_NAME_TO_ID.put(PROP_NAME_companyRate, PROP_ID_companyRate);
      
          PROP_ID_TO_NAME[PROP_ID_employeeRate] = PROP_NAME_employeeRate;
          PROP_NAME_TO_ID.put(PROP_NAME_employeeRate, PROP_ID_employeeRate);
      
          PROP_ID_TO_NAME[PROP_ID_baseLowerLimit] = PROP_NAME_baseLowerLimit;
          PROP_NAME_TO_ID.put(PROP_NAME_baseLowerLimit, PROP_ID_baseLowerLimit);
      
          PROP_ID_TO_NAME[PROP_ID_baseUpperLimit] = PROP_NAME_baseUpperLimit;
          PROP_NAME_TO_ID.put(PROP_NAME_baseUpperLimit, PROP_ID_baseUpperLimit);
      
          PROP_ID_TO_NAME[PROP_ID_effectiveFrom] = PROP_NAME_effectiveFrom;
          PROP_NAME_TO_ID.put(PROP_NAME_effectiveFrom, PROP_ID_effectiveFrom);
      
          PROP_ID_TO_NAME[PROP_ID_effectiveTo] = PROP_NAME_effectiveTo;
          PROP_NAME_TO_ID.put(PROP_NAME_effectiveTo, PROP_ID_effectiveTo);
      
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
    
    /* 城市: CITY_CODE */
    private java.lang.String _cityCode;
    
    /* 险种: INSURANCE_TYPE */
    private java.lang.String _insuranceType;
    
    /* 公司比例: COMPANY_RATE */
    private java.math.BigDecimal _companyRate;
    
    /* 个人比例: EMPLOYEE_RATE */
    private java.math.BigDecimal _employeeRate;
    
    /* 基数下限: BASE_LOWER_LIMIT */
    private java.math.BigDecimal _baseLowerLimit;
    
    /* 基数上限: BASE_UPPER_LIMIT */
    private java.math.BigDecimal _baseUpperLimit;
    
    /* 生效日期: EFFECTIVE_FROM */
    private java.time.LocalDate _effectiveFrom;
    
    /* 失效日期: EFFECTIVE_TO */
    private java.time.LocalDate _effectiveTo;
    
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
    

    public _ErpHrSocialInsuranceConfig(){
        // for debug
    }

    protected ErpHrSocialInsuranceConfig newInstance(){
        ErpHrSocialInsuranceConfig entity = new ErpHrSocialInsuranceConfig();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpHrSocialInsuranceConfig cloneInstance() {
        ErpHrSocialInsuranceConfig entity = newInstance();
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
      return "app.erp.hr.dao.entity.ErpHrSocialInsuranceConfig";
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
        
            case PROP_ID_cityCode:
               return getCityCode();
        
            case PROP_ID_insuranceType:
               return getInsuranceType();
        
            case PROP_ID_companyRate:
               return getCompanyRate();
        
            case PROP_ID_employeeRate:
               return getEmployeeRate();
        
            case PROP_ID_baseLowerLimit:
               return getBaseLowerLimit();
        
            case PROP_ID_baseUpperLimit:
               return getBaseUpperLimit();
        
            case PROP_ID_effectiveFrom:
               return getEffectiveFrom();
        
            case PROP_ID_effectiveTo:
               return getEffectiveTo();
        
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
        
            case PROP_ID_cityCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_cityCode));
               }
               setCityCode(typedValue);
               break;
            }
        
            case PROP_ID_insuranceType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_insuranceType));
               }
               setInsuranceType(typedValue);
               break;
            }
        
            case PROP_ID_companyRate:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_companyRate));
               }
               setCompanyRate(typedValue);
               break;
            }
        
            case PROP_ID_employeeRate:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_employeeRate));
               }
               setEmployeeRate(typedValue);
               break;
            }
        
            case PROP_ID_baseLowerLimit:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_baseLowerLimit));
               }
               setBaseLowerLimit(typedValue);
               break;
            }
        
            case PROP_ID_baseUpperLimit:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_baseUpperLimit));
               }
               setBaseUpperLimit(typedValue);
               break;
            }
        
            case PROP_ID_effectiveFrom:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_effectiveFrom));
               }
               setEffectiveFrom(typedValue);
               break;
            }
        
            case PROP_ID_effectiveTo:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_effectiveTo));
               }
               setEffectiveTo(typedValue);
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
        
            case PROP_ID_cityCode:{
               onInitProp(propId);
               this._cityCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_insuranceType:{
               onInitProp(propId);
               this._insuranceType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_companyRate:{
               onInitProp(propId);
               this._companyRate = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_employeeRate:{
               onInitProp(propId);
               this._employeeRate = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_baseLowerLimit:{
               onInitProp(propId);
               this._baseLowerLimit = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_baseUpperLimit:{
               onInitProp(propId);
               this._baseUpperLimit = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_effectiveFrom:{
               onInitProp(propId);
               this._effectiveFrom = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_effectiveTo:{
               onInitProp(propId);
               this._effectiveTo = (java.time.LocalDate)value;
               
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
     * 城市: CITY_CODE
     */
    public final java.lang.String getCityCode(){
         onPropGet(PROP_ID_cityCode);
         return _cityCode;
    }

    /**
     * 城市: CITY_CODE
     */
    public final void setCityCode(java.lang.String value){
        if(onPropSet(PROP_ID_cityCode,value)){
            this._cityCode = value;
            internalClearRefs(PROP_ID_cityCode);
            
        }
    }
    
    /**
     * 险种: INSURANCE_TYPE
     */
    public final java.lang.String getInsuranceType(){
         onPropGet(PROP_ID_insuranceType);
         return _insuranceType;
    }

    /**
     * 险种: INSURANCE_TYPE
     */
    public final void setInsuranceType(java.lang.String value){
        if(onPropSet(PROP_ID_insuranceType,value)){
            this._insuranceType = value;
            internalClearRefs(PROP_ID_insuranceType);
            
        }
    }
    
    /**
     * 公司比例: COMPANY_RATE
     */
    public final java.math.BigDecimal getCompanyRate(){
         onPropGet(PROP_ID_companyRate);
         return _companyRate;
    }

    /**
     * 公司比例: COMPANY_RATE
     */
    public final void setCompanyRate(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_companyRate,value)){
            this._companyRate = value;
            internalClearRefs(PROP_ID_companyRate);
            
        }
    }
    
    /**
     * 个人比例: EMPLOYEE_RATE
     */
    public final java.math.BigDecimal getEmployeeRate(){
         onPropGet(PROP_ID_employeeRate);
         return _employeeRate;
    }

    /**
     * 个人比例: EMPLOYEE_RATE
     */
    public final void setEmployeeRate(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_employeeRate,value)){
            this._employeeRate = value;
            internalClearRefs(PROP_ID_employeeRate);
            
        }
    }
    
    /**
     * 基数下限: BASE_LOWER_LIMIT
     */
    public final java.math.BigDecimal getBaseLowerLimit(){
         onPropGet(PROP_ID_baseLowerLimit);
         return _baseLowerLimit;
    }

    /**
     * 基数下限: BASE_LOWER_LIMIT
     */
    public final void setBaseLowerLimit(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_baseLowerLimit,value)){
            this._baseLowerLimit = value;
            internalClearRefs(PROP_ID_baseLowerLimit);
            
        }
    }
    
    /**
     * 基数上限: BASE_UPPER_LIMIT
     */
    public final java.math.BigDecimal getBaseUpperLimit(){
         onPropGet(PROP_ID_baseUpperLimit);
         return _baseUpperLimit;
    }

    /**
     * 基数上限: BASE_UPPER_LIMIT
     */
    public final void setBaseUpperLimit(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_baseUpperLimit,value)){
            this._baseUpperLimit = value;
            internalClearRefs(PROP_ID_baseUpperLimit);
            
        }
    }
    
    /**
     * 生效日期: EFFECTIVE_FROM
     */
    public final java.time.LocalDate getEffectiveFrom(){
         onPropGet(PROP_ID_effectiveFrom);
         return _effectiveFrom;
    }

    /**
     * 生效日期: EFFECTIVE_FROM
     */
    public final void setEffectiveFrom(java.time.LocalDate value){
        if(onPropSet(PROP_ID_effectiveFrom,value)){
            this._effectiveFrom = value;
            internalClearRefs(PROP_ID_effectiveFrom);
            
        }
    }
    
    /**
     * 失效日期: EFFECTIVE_TO
     */
    public final java.time.LocalDate getEffectiveTo(){
         onPropGet(PROP_ID_effectiveTo);
         return _effectiveTo;
    }

    /**
     * 失效日期: EFFECTIVE_TO
     */
    public final void setEffectiveTo(java.time.LocalDate value){
        if(onPropSet(PROP_ID_effectiveTo,value)){
            this._effectiveTo = value;
            internalClearRefs(PROP_ID_effectiveTo);
            
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
