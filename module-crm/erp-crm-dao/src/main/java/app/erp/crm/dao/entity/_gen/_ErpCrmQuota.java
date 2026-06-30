package app.erp.crm.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.crm.dao.entity.ErpCrmQuota;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  销售配额: erp_crm_quota
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpCrmQuota extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 2;
    
    /* 区域: TERRITORY_ID BIGINT */
    public static final String PROP_NAME_territoryId = "territoryId";
    public static final int PROP_ID_territoryId = 3;
    
    /* 团队: TEAM_ID BIGINT */
    public static final String PROP_NAME_teamId = "teamId";
    public static final int PROP_ID_teamId = 4;
    
    /* 销售员: OWNER_ID BIGINT */
    public static final String PROP_NAME_ownerId = "ownerId";
    public static final int PROP_ID_ownerId = 5;
    
    /* 配额期间类型: PERIOD_TYPE VARCHAR */
    public static final String PROP_NAME_periodType = "periodType";
    public static final int PROP_ID_periodType = 6;
    
    /* 财年: FISCAL_YEAR INTEGER */
    public static final String PROP_NAME_fiscalYear = "fiscalYear";
    public static final int PROP_ID_fiscalYear = 7;
    
    /* 期间标签: PERIOD_LABEL VARCHAR */
    public static final String PROP_NAME_periodLabel = "periodLabel";
    public static final int PROP_ID_periodLabel = 8;
    
    /* 配额金额: QUOTA_AMOUNT DECIMAL */
    public static final String PROP_NAME_quotaAmount = "quotaAmount";
    public static final int PROP_ID_quotaAmount = 9;
    
    /* 币种: CURRENCY_ID BIGINT */
    public static final String PROP_NAME_currencyId = "currencyId";
    public static final int PROP_ID_currencyId = 10;
    
    /* 是否已定稿: IS_FINALIZED BOOLEAN */
    public static final String PROP_NAME_isFinalized = "isFinalized";
    public static final int PROP_ID_isFinalized = 11;
    
    /* 备注: NOTES VARCHAR */
    public static final String PROP_NAME_notes = "notes";
    public static final int PROP_ID_notes = 12;
    
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
    public static final String PROP_NAME_territory = "territory";
    
    /* relation:  */
    public static final String PROP_NAME_team = "team";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[19];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_territoryId] = PROP_NAME_territoryId;
          PROP_NAME_TO_ID.put(PROP_NAME_territoryId, PROP_ID_territoryId);
      
          PROP_ID_TO_NAME[PROP_ID_teamId] = PROP_NAME_teamId;
          PROP_NAME_TO_ID.put(PROP_NAME_teamId, PROP_ID_teamId);
      
          PROP_ID_TO_NAME[PROP_ID_ownerId] = PROP_NAME_ownerId;
          PROP_NAME_TO_ID.put(PROP_NAME_ownerId, PROP_ID_ownerId);
      
          PROP_ID_TO_NAME[PROP_ID_periodType] = PROP_NAME_periodType;
          PROP_NAME_TO_ID.put(PROP_NAME_periodType, PROP_ID_periodType);
      
          PROP_ID_TO_NAME[PROP_ID_fiscalYear] = PROP_NAME_fiscalYear;
          PROP_NAME_TO_ID.put(PROP_NAME_fiscalYear, PROP_ID_fiscalYear);
      
          PROP_ID_TO_NAME[PROP_ID_periodLabel] = PROP_NAME_periodLabel;
          PROP_NAME_TO_ID.put(PROP_NAME_periodLabel, PROP_ID_periodLabel);
      
          PROP_ID_TO_NAME[PROP_ID_quotaAmount] = PROP_NAME_quotaAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_quotaAmount, PROP_ID_quotaAmount);
      
          PROP_ID_TO_NAME[PROP_ID_currencyId] = PROP_NAME_currencyId;
          PROP_NAME_TO_ID.put(PROP_NAME_currencyId, PROP_ID_currencyId);
      
          PROP_ID_TO_NAME[PROP_ID_isFinalized] = PROP_NAME_isFinalized;
          PROP_NAME_TO_ID.put(PROP_NAME_isFinalized, PROP_ID_isFinalized);
      
          PROP_ID_TO_NAME[PROP_ID_notes] = PROP_NAME_notes;
          PROP_NAME_TO_ID.put(PROP_NAME_notes, PROP_ID_notes);
      
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
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 区域: TERRITORY_ID */
    private java.lang.Long _territoryId;
    
    /* 团队: TEAM_ID */
    private java.lang.Long _teamId;
    
    /* 销售员: OWNER_ID */
    private java.lang.Long _ownerId;
    
    /* 配额期间类型: PERIOD_TYPE */
    private java.lang.String _periodType;
    
    /* 财年: FISCAL_YEAR */
    private java.lang.Integer _fiscalYear;
    
    /* 期间标签: PERIOD_LABEL */
    private java.lang.String _periodLabel;
    
    /* 配额金额: QUOTA_AMOUNT */
    private java.lang.String _quotaAmount;
    
    /* 币种: CURRENCY_ID */
    private java.lang.Long _currencyId;
    
    /* 是否已定稿: IS_FINALIZED */
    private java.lang.Boolean _isFinalized;
    
    /* 备注: NOTES */
    private java.lang.String _notes;
    
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
    

    public _ErpCrmQuota(){
        // for debug
    }

    protected ErpCrmQuota newInstance(){
        ErpCrmQuota entity = new ErpCrmQuota();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpCrmQuota cloneInstance() {
        ErpCrmQuota entity = newInstance();
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
      return "app.erp.crm.dao.entity.ErpCrmQuota";
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
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_territoryId:
               return getTerritoryId();
        
            case PROP_ID_teamId:
               return getTeamId();
        
            case PROP_ID_ownerId:
               return getOwnerId();
        
            case PROP_ID_periodType:
               return getPeriodType();
        
            case PROP_ID_fiscalYear:
               return getFiscalYear();
        
            case PROP_ID_periodLabel:
               return getPeriodLabel();
        
            case PROP_ID_quotaAmount:
               return getQuotaAmount();
        
            case PROP_ID_currencyId:
               return getCurrencyId();
        
            case PROP_ID_isFinalized:
               return getIsFinalized();
        
            case PROP_ID_notes:
               return getNotes();
        
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
        
            case PROP_ID_orgId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_orgId));
               }
               setOrgId(typedValue);
               break;
            }
        
            case PROP_ID_territoryId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_territoryId));
               }
               setTerritoryId(typedValue);
               break;
            }
        
            case PROP_ID_teamId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_teamId));
               }
               setTeamId(typedValue);
               break;
            }
        
            case PROP_ID_ownerId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_ownerId));
               }
               setOwnerId(typedValue);
               break;
            }
        
            case PROP_ID_periodType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_periodType));
               }
               setPeriodType(typedValue);
               break;
            }
        
            case PROP_ID_fiscalYear:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_fiscalYear));
               }
               setFiscalYear(typedValue);
               break;
            }
        
            case PROP_ID_periodLabel:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_periodLabel));
               }
               setPeriodLabel(typedValue);
               break;
            }
        
            case PROP_ID_quotaAmount:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_quotaAmount));
               }
               setQuotaAmount(typedValue);
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
        
            case PROP_ID_isFinalized:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isFinalized));
               }
               setIsFinalized(typedValue);
               break;
            }
        
            case PROP_ID_notes:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_notes));
               }
               setNotes(typedValue);
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
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_territoryId:{
               onInitProp(propId);
               this._territoryId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_teamId:{
               onInitProp(propId);
               this._teamId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_ownerId:{
               onInitProp(propId);
               this._ownerId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_periodType:{
               onInitProp(propId);
               this._periodType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_fiscalYear:{
               onInitProp(propId);
               this._fiscalYear = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_periodLabel:{
               onInitProp(propId);
               this._periodLabel = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_quotaAmount:{
               onInitProp(propId);
               this._quotaAmount = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_currencyId:{
               onInitProp(propId);
               this._currencyId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_isFinalized:{
               onInitProp(propId);
               this._isFinalized = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_notes:{
               onInitProp(propId);
               this._notes = (java.lang.String)value;
               
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
     * 区域: TERRITORY_ID
     */
    public final java.lang.Long getTerritoryId(){
         onPropGet(PROP_ID_territoryId);
         return _territoryId;
    }

    /**
     * 区域: TERRITORY_ID
     */
    public final void setTerritoryId(java.lang.Long value){
        if(onPropSet(PROP_ID_territoryId,value)){
            this._territoryId = value;
            internalClearRefs(PROP_ID_territoryId);
            
        }
    }
    
    /**
     * 团队: TEAM_ID
     */
    public final java.lang.Long getTeamId(){
         onPropGet(PROP_ID_teamId);
         return _teamId;
    }

    /**
     * 团队: TEAM_ID
     */
    public final void setTeamId(java.lang.Long value){
        if(onPropSet(PROP_ID_teamId,value)){
            this._teamId = value;
            internalClearRefs(PROP_ID_teamId);
            
        }
    }
    
    /**
     * 销售员: OWNER_ID
     */
    public final java.lang.Long getOwnerId(){
         onPropGet(PROP_ID_ownerId);
         return _ownerId;
    }

    /**
     * 销售员: OWNER_ID
     */
    public final void setOwnerId(java.lang.Long value){
        if(onPropSet(PROP_ID_ownerId,value)){
            this._ownerId = value;
            internalClearRefs(PROP_ID_ownerId);
            
        }
    }
    
    /**
     * 配额期间类型: PERIOD_TYPE
     */
    public final java.lang.String getPeriodType(){
         onPropGet(PROP_ID_periodType);
         return _periodType;
    }

    /**
     * 配额期间类型: PERIOD_TYPE
     */
    public final void setPeriodType(java.lang.String value){
        if(onPropSet(PROP_ID_periodType,value)){
            this._periodType = value;
            internalClearRefs(PROP_ID_periodType);
            
        }
    }
    
    /**
     * 财年: FISCAL_YEAR
     */
    public final java.lang.Integer getFiscalYear(){
         onPropGet(PROP_ID_fiscalYear);
         return _fiscalYear;
    }

    /**
     * 财年: FISCAL_YEAR
     */
    public final void setFiscalYear(java.lang.Integer value){
        if(onPropSet(PROP_ID_fiscalYear,value)){
            this._fiscalYear = value;
            internalClearRefs(PROP_ID_fiscalYear);
            
        }
    }
    
    /**
     * 期间标签: PERIOD_LABEL
     */
    public final java.lang.String getPeriodLabel(){
         onPropGet(PROP_ID_periodLabel);
         return _periodLabel;
    }

    /**
     * 期间标签: PERIOD_LABEL
     */
    public final void setPeriodLabel(java.lang.String value){
        if(onPropSet(PROP_ID_periodLabel,value)){
            this._periodLabel = value;
            internalClearRefs(PROP_ID_periodLabel);
            
        }
    }
    
    /**
     * 配额金额: QUOTA_AMOUNT
     */
    public final java.lang.String getQuotaAmount(){
         onPropGet(PROP_ID_quotaAmount);
         return _quotaAmount;
    }

    /**
     * 配额金额: QUOTA_AMOUNT
     */
    public final void setQuotaAmount(java.lang.String value){
        if(onPropSet(PROP_ID_quotaAmount,value)){
            this._quotaAmount = value;
            internalClearRefs(PROP_ID_quotaAmount);
            
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
     * 是否已定稿: IS_FINALIZED
     */
    public final java.lang.Boolean getIsFinalized(){
         onPropGet(PROP_ID_isFinalized);
         return _isFinalized;
    }

    /**
     * 是否已定稿: IS_FINALIZED
     */
    public final void setIsFinalized(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isFinalized,value)){
            this._isFinalized = value;
            internalClearRefs(PROP_ID_isFinalized);
            
        }
    }
    
    /**
     * 备注: NOTES
     */
    public final java.lang.String getNotes(){
         onPropGet(PROP_ID_notes);
         return _notes;
    }

    /**
     * 备注: NOTES
     */
    public final void setNotes(java.lang.String value){
        if(onPropSet(PROP_ID_notes,value)){
            this._notes = value;
            internalClearRefs(PROP_ID_notes);
            
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
    public final app.erp.crm.dao.entity.ErpCrmTerritory getTerritory(){
       return (app.erp.crm.dao.entity.ErpCrmTerritory)internalGetRefEntity(PROP_NAME_territory);
    }

    public final void setTerritory(app.erp.crm.dao.entity.ErpCrmTerritory refEntity){
   
           if(refEntity == null){
           
                   this.setTerritoryId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_territory, refEntity,()->{
           
                           this.setTerritoryId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.crm.dao.entity.ErpCrmTeam getTeam(){
       return (app.erp.crm.dao.entity.ErpCrmTeam)internalGetRefEntity(PROP_NAME_team);
    }

    public final void setTeam(app.erp.crm.dao.entity.ErpCrmTeam refEntity){
   
           if(refEntity == null){
           
                   this.setTeamId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_team, refEntity,()->{
           
                           this.setTeamId(refEntity.getId());
                       
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
