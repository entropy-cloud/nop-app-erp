package app.erp.pur.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.pur.dao.entity.ErpPurSupplierScorecard;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  供应商评分卡: erp_pur_supplier_scorecard
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpPurSupplierScorecard extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 供应商: PARTNER_ID BIGINT */
    public static final String PROP_NAME_partnerId = "partnerId";
    public static final int PROP_ID_partnerId = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 评分周期起: PERIOD_FROM DATE */
    public static final String PROP_NAME_periodFrom = "periodFrom";
    public static final int PROP_ID_periodFrom = 4;
    
    /* 评分周期止: PERIOD_TO DATE */
    public static final String PROP_NAME_periodTo = "periodTo";
    public static final int PROP_ID_periodTo = 5;
    
    /* 总分(派生): TOTAL_SCORE DECIMAL */
    public static final String PROP_NAME_totalScore = "totalScore";
    public static final int PROP_ID_totalScore = 6;
    
    /* 评级: STANDING VARCHAR */
    public static final String PROP_NAME_standing = "standing";
    public static final int PROP_ID_standing = 7;
    
    /* warn阈值: WARN_THRESHOLD DECIMAL */
    public static final String PROP_NAME_warnThreshold = "warnThreshold";
    public static final int PROP_ID_warnThreshold = 8;
    
    /* hold阈值: HOLD_THRESHOLD DECIMAL */
    public static final String PROP_NAME_holdThreshold = "holdThreshold";
    public static final int PROP_ID_holdThreshold = 9;
    
    /* prevent阈值: PREVENT_THRESHOLD DECIMAL */
    public static final String PROP_NAME_preventThreshold = "preventThreshold";
    public static final int PROP_ID_preventThreshold = 10;
    
    /* 周期状态: STATUS VARCHAR */
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
    public static final String PROP_NAME_supplier = "supplier";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    
    /* relation:  */
    public static final String PROP_NAME_criterias = "criterias";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[19];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_partnerId] = PROP_NAME_partnerId;
          PROP_NAME_TO_ID.put(PROP_NAME_partnerId, PROP_ID_partnerId);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_periodFrom] = PROP_NAME_periodFrom;
          PROP_NAME_TO_ID.put(PROP_NAME_periodFrom, PROP_ID_periodFrom);
      
          PROP_ID_TO_NAME[PROP_ID_periodTo] = PROP_NAME_periodTo;
          PROP_NAME_TO_ID.put(PROP_NAME_periodTo, PROP_ID_periodTo);
      
          PROP_ID_TO_NAME[PROP_ID_totalScore] = PROP_NAME_totalScore;
          PROP_NAME_TO_ID.put(PROP_NAME_totalScore, PROP_ID_totalScore);
      
          PROP_ID_TO_NAME[PROP_ID_standing] = PROP_NAME_standing;
          PROP_NAME_TO_ID.put(PROP_NAME_standing, PROP_ID_standing);
      
          PROP_ID_TO_NAME[PROP_ID_warnThreshold] = PROP_NAME_warnThreshold;
          PROP_NAME_TO_ID.put(PROP_NAME_warnThreshold, PROP_ID_warnThreshold);
      
          PROP_ID_TO_NAME[PROP_ID_holdThreshold] = PROP_NAME_holdThreshold;
          PROP_NAME_TO_ID.put(PROP_NAME_holdThreshold, PROP_ID_holdThreshold);
      
          PROP_ID_TO_NAME[PROP_ID_preventThreshold] = PROP_NAME_preventThreshold;
          PROP_NAME_TO_ID.put(PROP_NAME_preventThreshold, PROP_ID_preventThreshold);
      
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
    
    /* 供应商: PARTNER_ID */
    private java.lang.Long _partnerId;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 评分周期起: PERIOD_FROM */
    private java.time.LocalDate _periodFrom;
    
    /* 评分周期止: PERIOD_TO */
    private java.time.LocalDate _periodTo;
    
    /* 总分(派生): TOTAL_SCORE */
    private java.math.BigDecimal _totalScore;
    
    /* 评级: STANDING */
    private java.lang.String _standing;
    
    /* warn阈值: WARN_THRESHOLD */
    private java.math.BigDecimal _warnThreshold;
    
    /* hold阈值: HOLD_THRESHOLD */
    private java.math.BigDecimal _holdThreshold;
    
    /* prevent阈值: PREVENT_THRESHOLD */
    private java.math.BigDecimal _preventThreshold;
    
    /* 周期状态: STATUS */
    private java.lang.String _status;
    
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
    

    public _ErpPurSupplierScorecard(){
        // for debug
    }

    protected ErpPurSupplierScorecard newInstance(){
        ErpPurSupplierScorecard entity = new ErpPurSupplierScorecard();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpPurSupplierScorecard cloneInstance() {
        ErpPurSupplierScorecard entity = newInstance();
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
      return "app.erp.pur.dao.entity.ErpPurSupplierScorecard";
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
        
            case PROP_ID_partnerId:
               return getPartnerId();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_periodFrom:
               return getPeriodFrom();
        
            case PROP_ID_periodTo:
               return getPeriodTo();
        
            case PROP_ID_totalScore:
               return getTotalScore();
        
            case PROP_ID_standing:
               return getStanding();
        
            case PROP_ID_warnThreshold:
               return getWarnThreshold();
        
            case PROP_ID_holdThreshold:
               return getHoldThreshold();
        
            case PROP_ID_preventThreshold:
               return getPreventThreshold();
        
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
        
            case PROP_ID_partnerId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_partnerId));
               }
               setPartnerId(typedValue);
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
        
            case PROP_ID_periodFrom:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_periodFrom));
               }
               setPeriodFrom(typedValue);
               break;
            }
        
            case PROP_ID_periodTo:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_periodTo));
               }
               setPeriodTo(typedValue);
               break;
            }
        
            case PROP_ID_totalScore:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_totalScore));
               }
               setTotalScore(typedValue);
               break;
            }
        
            case PROP_ID_standing:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_standing));
               }
               setStanding(typedValue);
               break;
            }
        
            case PROP_ID_warnThreshold:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_warnThreshold));
               }
               setWarnThreshold(typedValue);
               break;
            }
        
            case PROP_ID_holdThreshold:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_holdThreshold));
               }
               setHoldThreshold(typedValue);
               break;
            }
        
            case PROP_ID_preventThreshold:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_preventThreshold));
               }
               setPreventThreshold(typedValue);
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
        
            case PROP_ID_partnerId:{
               onInitProp(propId);
               this._partnerId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_periodFrom:{
               onInitProp(propId);
               this._periodFrom = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_periodTo:{
               onInitProp(propId);
               this._periodTo = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_totalScore:{
               onInitProp(propId);
               this._totalScore = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_standing:{
               onInitProp(propId);
               this._standing = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_warnThreshold:{
               onInitProp(propId);
               this._warnThreshold = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_holdThreshold:{
               onInitProp(propId);
               this._holdThreshold = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_preventThreshold:{
               onInitProp(propId);
               this._preventThreshold = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
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
     * 供应商: PARTNER_ID
     */
    public final java.lang.Long getPartnerId(){
         onPropGet(PROP_ID_partnerId);
         return _partnerId;
    }

    /**
     * 供应商: PARTNER_ID
     */
    public final void setPartnerId(java.lang.Long value){
        if(onPropSet(PROP_ID_partnerId,value)){
            this._partnerId = value;
            internalClearRefs(PROP_ID_partnerId);
            
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
     * 评分周期起: PERIOD_FROM
     */
    public final java.time.LocalDate getPeriodFrom(){
         onPropGet(PROP_ID_periodFrom);
         return _periodFrom;
    }

    /**
     * 评分周期起: PERIOD_FROM
     */
    public final void setPeriodFrom(java.time.LocalDate value){
        if(onPropSet(PROP_ID_periodFrom,value)){
            this._periodFrom = value;
            internalClearRefs(PROP_ID_periodFrom);
            
        }
    }
    
    /**
     * 评分周期止: PERIOD_TO
     */
    public final java.time.LocalDate getPeriodTo(){
         onPropGet(PROP_ID_periodTo);
         return _periodTo;
    }

    /**
     * 评分周期止: PERIOD_TO
     */
    public final void setPeriodTo(java.time.LocalDate value){
        if(onPropSet(PROP_ID_periodTo,value)){
            this._periodTo = value;
            internalClearRefs(PROP_ID_periodTo);
            
        }
    }
    
    /**
     * 总分(派生): TOTAL_SCORE
     */
    public final java.math.BigDecimal getTotalScore(){
         onPropGet(PROP_ID_totalScore);
         return _totalScore;
    }

    /**
     * 总分(派生): TOTAL_SCORE
     */
    public final void setTotalScore(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_totalScore,value)){
            this._totalScore = value;
            internalClearRefs(PROP_ID_totalScore);
            
        }
    }
    
    /**
     * 评级: STANDING
     */
    public final java.lang.String getStanding(){
         onPropGet(PROP_ID_standing);
         return _standing;
    }

    /**
     * 评级: STANDING
     */
    public final void setStanding(java.lang.String value){
        if(onPropSet(PROP_ID_standing,value)){
            this._standing = value;
            internalClearRefs(PROP_ID_standing);
            
        }
    }
    
    /**
     * warn阈值: WARN_THRESHOLD
     */
    public final java.math.BigDecimal getWarnThreshold(){
         onPropGet(PROP_ID_warnThreshold);
         return _warnThreshold;
    }

    /**
     * warn阈值: WARN_THRESHOLD
     */
    public final void setWarnThreshold(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_warnThreshold,value)){
            this._warnThreshold = value;
            internalClearRefs(PROP_ID_warnThreshold);
            
        }
    }
    
    /**
     * hold阈值: HOLD_THRESHOLD
     */
    public final java.math.BigDecimal getHoldThreshold(){
         onPropGet(PROP_ID_holdThreshold);
         return _holdThreshold;
    }

    /**
     * hold阈值: HOLD_THRESHOLD
     */
    public final void setHoldThreshold(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_holdThreshold,value)){
            this._holdThreshold = value;
            internalClearRefs(PROP_ID_holdThreshold);
            
        }
    }
    
    /**
     * prevent阈值: PREVENT_THRESHOLD
     */
    public final java.math.BigDecimal getPreventThreshold(){
         onPropGet(PROP_ID_preventThreshold);
         return _preventThreshold;
    }

    /**
     * prevent阈值: PREVENT_THRESHOLD
     */
    public final void setPreventThreshold(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_preventThreshold,value)){
            this._preventThreshold = value;
            internalClearRefs(PROP_ID_preventThreshold);
            
        }
    }
    
    /**
     * 周期状态: STATUS
     */
    public final java.lang.String getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 周期状态: STATUS
     */
    public final void setStatus(java.lang.String value){
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
    public final app.erp.md.dao.entity.ErpMdPartner getSupplier(){
       return (app.erp.md.dao.entity.ErpMdPartner)internalGetRefEntity(PROP_NAME_supplier);
    }

    public final void setSupplier(app.erp.md.dao.entity.ErpMdPartner refEntity){
   
           if(refEntity == null){
           
                   this.setPartnerId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_supplier, refEntity,()->{
           
                           this.setPartnerId(refEntity.getId());
                       
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
       
    private final OrmEntitySet<app.erp.pur.dao.entity.ErpPurSupplierScorecardCriteria> _criterias = new OrmEntitySet<>(this, PROP_NAME_criterias,
        null, null,app.erp.pur.dao.entity.ErpPurSupplierScorecardCriteria.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.pur.dao.entity.ErpPurSupplierScorecardCriteria> getCriterias(){
       return _criterias;
    }
       
}
// resume CPD analysis - CPD-ON
