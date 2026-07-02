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

import app.erp.fin.dao.entity.ErpFinCreditFacility;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  银行授信额度: erp_fin_credit_facility
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpFinCreditFacility extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 单号: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 核算组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 授信银行账户: FUND_ACCOUNT_ID BIGINT */
    public static final String PROP_NAME_fundAccountId = "fundAccountId";
    public static final int PROP_ID_fundAccountId = 4;
    
    /* 授信类型: FACILITY_TYPE INTEGER */
    public static final String PROP_NAME_facilityType = "facilityType";
    public static final int PROP_ID_facilityType = 5;
    
    /* 授信总额: TOTAL_AMOUNT DECIMAL */
    public static final String PROP_NAME_totalAmount = "totalAmount";
    public static final int PROP_ID_totalAmount = 6;
    
    /* 已用额度: USED_AMOUNT DECIMAL */
    public static final String PROP_NAME_usedAmount = "usedAmount";
    public static final int PROP_ID_usedAmount = 7;
    
    /* 可用额度: AVAILABLE_AMOUNT DECIMAL */
    public static final String PROP_NAME_availableAmount = "availableAmount";
    public static final int PROP_ID_availableAmount = 8;
    
    /* 生效日: VALID_FROM DATE */
    public static final String PROP_NAME_validFrom = "validFrom";
    public static final int PROP_ID_validFrom = 9;
    
    /* 到期日: VALID_TO DATE */
    public static final String PROP_NAME_validTo = "validTo";
    public static final int PROP_ID_validTo = 10;
    
    /* 状态: STATUS INTEGER */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 11;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 12;
    
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
    public static final String PROP_NAME_fundAccount = "fundAccount";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[19];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_fundAccountId] = PROP_NAME_fundAccountId;
          PROP_NAME_TO_ID.put(PROP_NAME_fundAccountId, PROP_ID_fundAccountId);
      
          PROP_ID_TO_NAME[PROP_ID_facilityType] = PROP_NAME_facilityType;
          PROP_NAME_TO_ID.put(PROP_NAME_facilityType, PROP_ID_facilityType);
      
          PROP_ID_TO_NAME[PROP_ID_totalAmount] = PROP_NAME_totalAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_totalAmount, PROP_ID_totalAmount);
      
          PROP_ID_TO_NAME[PROP_ID_usedAmount] = PROP_NAME_usedAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_usedAmount, PROP_ID_usedAmount);
      
          PROP_ID_TO_NAME[PROP_ID_availableAmount] = PROP_NAME_availableAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_availableAmount, PROP_ID_availableAmount);
      
          PROP_ID_TO_NAME[PROP_ID_validFrom] = PROP_NAME_validFrom;
          PROP_NAME_TO_ID.put(PROP_NAME_validFrom, PROP_ID_validFrom);
      
          PROP_ID_TO_NAME[PROP_ID_validTo] = PROP_NAME_validTo;
          PROP_NAME_TO_ID.put(PROP_NAME_validTo, PROP_ID_validTo);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
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
    
    /* 单号: CODE */
    private java.lang.String _code;
    
    /* 核算组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 授信银行账户: FUND_ACCOUNT_ID */
    private java.lang.Long _fundAccountId;
    
    /* 授信类型: FACILITY_TYPE */
    private java.lang.Integer _facilityType;
    
    /* 授信总额: TOTAL_AMOUNT */
    private java.math.BigDecimal _totalAmount;
    
    /* 已用额度: USED_AMOUNT */
    private java.math.BigDecimal _usedAmount;
    
    /* 可用额度: AVAILABLE_AMOUNT */
    private java.math.BigDecimal _availableAmount;
    
    /* 生效日: VALID_FROM */
    private java.time.LocalDate _validFrom;
    
    /* 到期日: VALID_TO */
    private java.time.LocalDate _validTo;
    
    /* 状态: STATUS */
    private java.lang.Integer _status;
    
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
    

    public _ErpFinCreditFacility(){
        // for debug
    }

    protected ErpFinCreditFacility newInstance(){
        ErpFinCreditFacility entity = new ErpFinCreditFacility();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpFinCreditFacility cloneInstance() {
        ErpFinCreditFacility entity = newInstance();
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
      return "app.erp.fin.dao.entity.ErpFinCreditFacility";
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
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_fundAccountId:
               return getFundAccountId();
        
            case PROP_ID_facilityType:
               return getFacilityType();
        
            case PROP_ID_totalAmount:
               return getTotalAmount();
        
            case PROP_ID_usedAmount:
               return getUsedAmount();
        
            case PROP_ID_availableAmount:
               return getAvailableAmount();
        
            case PROP_ID_validFrom:
               return getValidFrom();
        
            case PROP_ID_validTo:
               return getValidTo();
        
            case PROP_ID_status:
               return getStatus();
        
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
        
            case PROP_ID_code:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_code));
               }
               setCode(typedValue);
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
        
            case PROP_ID_fundAccountId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_fundAccountId));
               }
               setFundAccountId(typedValue);
               break;
            }
        
            case PROP_ID_facilityType:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_facilityType));
               }
               setFacilityType(typedValue);
               break;
            }
        
            case PROP_ID_totalAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_totalAmount));
               }
               setTotalAmount(typedValue);
               break;
            }
        
            case PROP_ID_usedAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_usedAmount));
               }
               setUsedAmount(typedValue);
               break;
            }
        
            case PROP_ID_availableAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_availableAmount));
               }
               setAvailableAmount(typedValue);
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
        
            case PROP_ID_status:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_status));
               }
               setStatus(typedValue);
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
        
            case PROP_ID_code:{
               onInitProp(propId);
               this._code = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_fundAccountId:{
               onInitProp(propId);
               this._fundAccountId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_facilityType:{
               onInitProp(propId);
               this._facilityType = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_totalAmount:{
               onInitProp(propId);
               this._totalAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_usedAmount:{
               onInitProp(propId);
               this._usedAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_availableAmount:{
               onInitProp(propId);
               this._availableAmount = (java.math.BigDecimal)value;
               
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
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.Integer)value;
               
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
     * 单号: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 单号: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
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
     * 授信银行账户: FUND_ACCOUNT_ID
     */
    public final java.lang.Long getFundAccountId(){
         onPropGet(PROP_ID_fundAccountId);
         return _fundAccountId;
    }

    /**
     * 授信银行账户: FUND_ACCOUNT_ID
     */
    public final void setFundAccountId(java.lang.Long value){
        if(onPropSet(PROP_ID_fundAccountId,value)){
            this._fundAccountId = value;
            internalClearRefs(PROP_ID_fundAccountId);
            
        }
    }
    
    /**
     * 授信类型: FACILITY_TYPE
     */
    public final java.lang.Integer getFacilityType(){
         onPropGet(PROP_ID_facilityType);
         return _facilityType;
    }

    /**
     * 授信类型: FACILITY_TYPE
     */
    public final void setFacilityType(java.lang.Integer value){
        if(onPropSet(PROP_ID_facilityType,value)){
            this._facilityType = value;
            internalClearRefs(PROP_ID_facilityType);
            
        }
    }
    
    /**
     * 授信总额: TOTAL_AMOUNT
     */
    public final java.math.BigDecimal getTotalAmount(){
         onPropGet(PROP_ID_totalAmount);
         return _totalAmount;
    }

    /**
     * 授信总额: TOTAL_AMOUNT
     */
    public final void setTotalAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_totalAmount,value)){
            this._totalAmount = value;
            internalClearRefs(PROP_ID_totalAmount);
            
        }
    }
    
    /**
     * 已用额度: USED_AMOUNT
     */
    public final java.math.BigDecimal getUsedAmount(){
         onPropGet(PROP_ID_usedAmount);
         return _usedAmount;
    }

    /**
     * 已用额度: USED_AMOUNT
     */
    public final void setUsedAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_usedAmount,value)){
            this._usedAmount = value;
            internalClearRefs(PROP_ID_usedAmount);
            
        }
    }
    
    /**
     * 可用额度: AVAILABLE_AMOUNT
     */
    public final java.math.BigDecimal getAvailableAmount(){
         onPropGet(PROP_ID_availableAmount);
         return _availableAmount;
    }

    /**
     * 可用额度: AVAILABLE_AMOUNT
     */
    public final void setAvailableAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_availableAmount,value)){
            this._availableAmount = value;
            internalClearRefs(PROP_ID_availableAmount);
            
        }
    }
    
    /**
     * 生效日: VALID_FROM
     */
    public final java.time.LocalDate getValidFrom(){
         onPropGet(PROP_ID_validFrom);
         return _validFrom;
    }

    /**
     * 生效日: VALID_FROM
     */
    public final void setValidFrom(java.time.LocalDate value){
        if(onPropSet(PROP_ID_validFrom,value)){
            this._validFrom = value;
            internalClearRefs(PROP_ID_validFrom);
            
        }
    }
    
    /**
     * 到期日: VALID_TO
     */
    public final java.time.LocalDate getValidTo(){
         onPropGet(PROP_ID_validTo);
         return _validTo;
    }

    /**
     * 到期日: VALID_TO
     */
    public final void setValidTo(java.time.LocalDate value){
        if(onPropSet(PROP_ID_validTo,value)){
            this._validTo = value;
            internalClearRefs(PROP_ID_validTo);
            
        }
    }
    
    /**
     * 状态: STATUS
     */
    public final java.lang.Integer getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 状态: STATUS
     */
    public final void setStatus(java.lang.Integer value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
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
    public final app.erp.fin.dao.entity.ErpFinFundAccount getFundAccount(){
       return (app.erp.fin.dao.entity.ErpFinFundAccount)internalGetRefEntity(PROP_NAME_fundAccount);
    }

    public final void setFundAccount(app.erp.fin.dao.entity.ErpFinFundAccount refEntity){
   
           if(refEntity == null){
           
                   this.setFundAccountId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_fundAccount, refEntity,()->{
           
                           this.setFundAccountId(refEntity.getId());
                       
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
