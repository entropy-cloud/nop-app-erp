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

import app.erp.fin.dao.entity.ErpFinConsolidationElimination;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  合并抵消候选: erp_fin_consolidation_elimination
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpFinConsolidationElimination extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 候选编码: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 核算组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 抵消类型: ELIMINATION_TYPE VARCHAR */
    public static final String PROP_NAME_eliminationType = "eliminationType";
    public static final int PROP_ID_eliminationType = 4;
    
    /* 会计期间: PERIOD_ID BIGINT */
    public static final String PROP_NAME_periodId = "periodId";
    public static final int PROP_ID_periodId = 5;
    
    /* 配对键: PAIR_KEY VARCHAR */
    public static final String PROP_NAME_pairKey = "pairKey";
    public static final int PROP_ID_pairKey = 6;
    
    /* 配对记录: MATCH_ID BIGINT */
    public static final String PROP_NAME_matchId = "matchId";
    public static final int PROP_ID_matchId = 7;
    
    /* 调出组织: FROM_ORG_ID BIGINT */
    public static final String PROP_NAME_fromOrgId = "fromOrgId";
    public static final int PROP_ID_fromOrgId = 8;
    
    /* 调入组织: TO_ORG_ID BIGINT */
    public static final String PROP_NAME_toOrgId = "toOrgId";
    public static final int PROP_ID_toOrgId = 9;
    
    /* 抵消金额: ELIMINATION_AMOUNT DECIMAL */
    public static final String PROP_NAME_eliminationAmount = "eliminationAmount";
    public static final int PROP_ID_eliminationAmount = 10;
    
    /* 草稿抵消凭证: DRAFT_VOUCHER_ID BIGINT */
    public static final String PROP_NAME_draftVoucherId = "draftVoucherId";
    public static final int PROP_ID_draftVoucherId = 11;
    
    /* 抵消状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 12;
    
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
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 19;
    

    private static int _PROP_ID_BOUND = 20;

    
    /* relation:  */
    public static final String PROP_NAME_period = "period";
    
    /* relation:  */
    public static final String PROP_NAME_match = "match";
    
    /* relation:  */
    public static final String PROP_NAME_draftVoucher = "draftVoucher";
    
    /* relation:  */
    public static final String PROP_NAME_fromOrg = "fromOrg";
    
    /* relation:  */
    public static final String PROP_NAME_toOrg = "toOrg";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[20];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_eliminationType] = PROP_NAME_eliminationType;
          PROP_NAME_TO_ID.put(PROP_NAME_eliminationType, PROP_ID_eliminationType);
      
          PROP_ID_TO_NAME[PROP_ID_periodId] = PROP_NAME_periodId;
          PROP_NAME_TO_ID.put(PROP_NAME_periodId, PROP_ID_periodId);
      
          PROP_ID_TO_NAME[PROP_ID_pairKey] = PROP_NAME_pairKey;
          PROP_NAME_TO_ID.put(PROP_NAME_pairKey, PROP_ID_pairKey);
      
          PROP_ID_TO_NAME[PROP_ID_matchId] = PROP_NAME_matchId;
          PROP_NAME_TO_ID.put(PROP_NAME_matchId, PROP_ID_matchId);
      
          PROP_ID_TO_NAME[PROP_ID_fromOrgId] = PROP_NAME_fromOrgId;
          PROP_NAME_TO_ID.put(PROP_NAME_fromOrgId, PROP_ID_fromOrgId);
      
          PROP_ID_TO_NAME[PROP_ID_toOrgId] = PROP_NAME_toOrgId;
          PROP_NAME_TO_ID.put(PROP_NAME_toOrgId, PROP_ID_toOrgId);
      
          PROP_ID_TO_NAME[PROP_ID_eliminationAmount] = PROP_NAME_eliminationAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_eliminationAmount, PROP_ID_eliminationAmount);
      
          PROP_ID_TO_NAME[PROP_ID_draftVoucherId] = PROP_NAME_draftVoucherId;
          PROP_NAME_TO_ID.put(PROP_NAME_draftVoucherId, PROP_ID_draftVoucherId);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
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
    
    /* 候选编码: CODE */
    private java.lang.String _code;
    
    /* 核算组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 抵消类型: ELIMINATION_TYPE */
    private java.lang.String _eliminationType;
    
    /* 会计期间: PERIOD_ID */
    private java.lang.Long _periodId;
    
    /* 配对键: PAIR_KEY */
    private java.lang.String _pairKey;
    
    /* 配对记录: MATCH_ID */
    private java.lang.Long _matchId;
    
    /* 调出组织: FROM_ORG_ID */
    private java.lang.Long _fromOrgId;
    
    /* 调入组织: TO_ORG_ID */
    private java.lang.Long _toOrgId;
    
    /* 抵消金额: ELIMINATION_AMOUNT */
    private java.math.BigDecimal _eliminationAmount;
    
    /* 草稿抵消凭证: DRAFT_VOUCHER_ID */
    private java.lang.Long _draftVoucherId;
    
    /* 抵消状态: STATUS */
    private java.lang.String _status;
    
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
    

    public _ErpFinConsolidationElimination(){
        // for debug
    }

    protected ErpFinConsolidationElimination newInstance(){
        ErpFinConsolidationElimination entity = new ErpFinConsolidationElimination();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpFinConsolidationElimination cloneInstance() {
        ErpFinConsolidationElimination entity = newInstance();
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
      return "app.erp.fin.dao.entity.ErpFinConsolidationElimination";
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
        
            case PROP_ID_eliminationType:
               return getEliminationType();
        
            case PROP_ID_periodId:
               return getPeriodId();
        
            case PROP_ID_pairKey:
               return getPairKey();
        
            case PROP_ID_matchId:
               return getMatchId();
        
            case PROP_ID_fromOrgId:
               return getFromOrgId();
        
            case PROP_ID_toOrgId:
               return getToOrgId();
        
            case PROP_ID_eliminationAmount:
               return getEliminationAmount();
        
            case PROP_ID_draftVoucherId:
               return getDraftVoucherId();
        
            case PROP_ID_status:
               return getStatus();
        
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
        
            case PROP_ID_orgId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_orgId));
               }
               setOrgId(typedValue);
               break;
            }
        
            case PROP_ID_eliminationType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_eliminationType));
               }
               setEliminationType(typedValue);
               break;
            }
        
            case PROP_ID_periodId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_periodId));
               }
               setPeriodId(typedValue);
               break;
            }
        
            case PROP_ID_pairKey:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_pairKey));
               }
               setPairKey(typedValue);
               break;
            }
        
            case PROP_ID_matchId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_matchId));
               }
               setMatchId(typedValue);
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
        
            case PROP_ID_eliminationAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_eliminationAmount));
               }
               setEliminationAmount(typedValue);
               break;
            }
        
            case PROP_ID_draftVoucherId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_draftVoucherId));
               }
               setDraftVoucherId(typedValue);
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
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_eliminationType:{
               onInitProp(propId);
               this._eliminationType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_periodId:{
               onInitProp(propId);
               this._periodId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_pairKey:{
               onInitProp(propId);
               this._pairKey = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_matchId:{
               onInitProp(propId);
               this._matchId = (java.lang.Long)value;
               
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
        
            case PROP_ID_eliminationAmount:{
               onInitProp(propId);
               this._eliminationAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_draftVoucherId:{
               onInitProp(propId);
               this._draftVoucherId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
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
     * 候选编码: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 候选编码: CODE
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
     * 抵消类型: ELIMINATION_TYPE
     */
    public final java.lang.String getEliminationType(){
         onPropGet(PROP_ID_eliminationType);
         return _eliminationType;
    }

    /**
     * 抵消类型: ELIMINATION_TYPE
     */
    public final void setEliminationType(java.lang.String value){
        if(onPropSet(PROP_ID_eliminationType,value)){
            this._eliminationType = value;
            internalClearRefs(PROP_ID_eliminationType);
            
        }
    }
    
    /**
     * 会计期间: PERIOD_ID
     */
    public final java.lang.Long getPeriodId(){
         onPropGet(PROP_ID_periodId);
         return _periodId;
    }

    /**
     * 会计期间: PERIOD_ID
     */
    public final void setPeriodId(java.lang.Long value){
        if(onPropSet(PROP_ID_periodId,value)){
            this._periodId = value;
            internalClearRefs(PROP_ID_periodId);
            
        }
    }
    
    /**
     * 配对键: PAIR_KEY
     */
    public final java.lang.String getPairKey(){
         onPropGet(PROP_ID_pairKey);
         return _pairKey;
    }

    /**
     * 配对键: PAIR_KEY
     */
    public final void setPairKey(java.lang.String value){
        if(onPropSet(PROP_ID_pairKey,value)){
            this._pairKey = value;
            internalClearRefs(PROP_ID_pairKey);
            
        }
    }
    
    /**
     * 配对记录: MATCH_ID
     */
    public final java.lang.Long getMatchId(){
         onPropGet(PROP_ID_matchId);
         return _matchId;
    }

    /**
     * 配对记录: MATCH_ID
     */
    public final void setMatchId(java.lang.Long value){
        if(onPropSet(PROP_ID_matchId,value)){
            this._matchId = value;
            internalClearRefs(PROP_ID_matchId);
            
        }
    }
    
    /**
     * 调出组织: FROM_ORG_ID
     */
    public final java.lang.Long getFromOrgId(){
         onPropGet(PROP_ID_fromOrgId);
         return _fromOrgId;
    }

    /**
     * 调出组织: FROM_ORG_ID
     */
    public final void setFromOrgId(java.lang.Long value){
        if(onPropSet(PROP_ID_fromOrgId,value)){
            this._fromOrgId = value;
            internalClearRefs(PROP_ID_fromOrgId);
            
        }
    }
    
    /**
     * 调入组织: TO_ORG_ID
     */
    public final java.lang.Long getToOrgId(){
         onPropGet(PROP_ID_toOrgId);
         return _toOrgId;
    }

    /**
     * 调入组织: TO_ORG_ID
     */
    public final void setToOrgId(java.lang.Long value){
        if(onPropSet(PROP_ID_toOrgId,value)){
            this._toOrgId = value;
            internalClearRefs(PROP_ID_toOrgId);
            
        }
    }
    
    /**
     * 抵消金额: ELIMINATION_AMOUNT
     */
    public final java.math.BigDecimal getEliminationAmount(){
         onPropGet(PROP_ID_eliminationAmount);
         return _eliminationAmount;
    }

    /**
     * 抵消金额: ELIMINATION_AMOUNT
     */
    public final void setEliminationAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_eliminationAmount,value)){
            this._eliminationAmount = value;
            internalClearRefs(PROP_ID_eliminationAmount);
            
        }
    }
    
    /**
     * 草稿抵消凭证: DRAFT_VOUCHER_ID
     */
    public final java.lang.Long getDraftVoucherId(){
         onPropGet(PROP_ID_draftVoucherId);
         return _draftVoucherId;
    }

    /**
     * 草稿抵消凭证: DRAFT_VOUCHER_ID
     */
    public final void setDraftVoucherId(java.lang.Long value){
        if(onPropSet(PROP_ID_draftVoucherId,value)){
            this._draftVoucherId = value;
            internalClearRefs(PROP_ID_draftVoucherId);
            
        }
    }
    
    /**
     * 抵消状态: STATUS
     */
    public final java.lang.String getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 抵消状态: STATUS
     */
    public final void setStatus(java.lang.String value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
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
    public final app.erp.fin.dao.entity.ErpFinAccountingPeriod getPeriod(){
       return (app.erp.fin.dao.entity.ErpFinAccountingPeriod)internalGetRefEntity(PROP_NAME_period);
    }

    public final void setPeriod(app.erp.fin.dao.entity.ErpFinAccountingPeriod refEntity){
   
           if(refEntity == null){
           
                   this.setPeriodId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_period, refEntity,()->{
           
                           this.setPeriodId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.fin.dao.entity.ErpFinIntercompanyMatch getMatch(){
       return (app.erp.fin.dao.entity.ErpFinIntercompanyMatch)internalGetRefEntity(PROP_NAME_match);
    }

    public final void setMatch(app.erp.fin.dao.entity.ErpFinIntercompanyMatch refEntity){
   
           if(refEntity == null){
           
                   this.setMatchId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_match, refEntity,()->{
           
                           this.setMatchId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.fin.dao.entity.ErpFinVoucher getDraftVoucher(){
       return (app.erp.fin.dao.entity.ErpFinVoucher)internalGetRefEntity(PROP_NAME_draftVoucher);
    }

    public final void setDraftVoucher(app.erp.fin.dao.entity.ErpFinVoucher refEntity){
   
           if(refEntity == null){
           
                   this.setDraftVoucherId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_draftVoucher, refEntity,()->{
           
                           this.setDraftVoucherId(refEntity.getId());
                       
           });
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
