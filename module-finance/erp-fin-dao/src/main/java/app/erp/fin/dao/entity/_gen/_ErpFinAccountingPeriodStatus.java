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

import app.erp.fin.dao.entity.ErpFinAccountingPeriodStatus;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  期间结账状态: erp_fin_accounting_period_status
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpFinAccountingPeriodStatus extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 期间ID: PERIOD_ID BIGINT */
    public static final String PROP_NAME_periodId = "periodId";
    public static final int PROP_ID_periodId = 2;
    
    /* 账套: ACCT_SCHEMA_ID BIGINT */
    public static final String PROP_NAME_acctSchemaId = "acctSchemaId";
    public static final int PROP_ID_acctSchemaId = 3;
    
    /* 凭证总数: TOTAL_VOUCHERS INTEGER */
    public static final String PROP_NAME_totalVouchers = "totalVouchers";
    public static final int PROP_ID_totalVouchers = 4;
    
    /* 已过账凭证数: POSTED_VOUCHERS INTEGER */
    public static final String PROP_NAME_postedVouchers = "postedVouchers";
    public static final int PROP_ID_postedVouchers = 5;
    
    /* 未过账凭证数: UNPOSTED_VOUCHERS INTEGER */
    public static final String PROP_NAME_unpostedVouchers = "unpostedVouchers";
    public static final int PROP_ID_unpostedVouchers = 6;
    
    /* 应收模块状态: AR_STATUS VARCHAR */
    public static final String PROP_NAME_arStatus = "arStatus";
    public static final int PROP_ID_arStatus = 7;
    
    /* 应付模块状态: AP_STATUS VARCHAR */
    public static final String PROP_NAME_apStatus = "apStatus";
    public static final int PROP_ID_apStatus = 8;
    
    /* 存货模块状态: INV_STATUS VARCHAR */
    public static final String PROP_NAME_invStatus = "invStatus";
    public static final int PROP_ID_invStatus = 9;
    
    /* 总账模块状态: GL_STATUS VARCHAR */
    public static final String PROP_NAME_glStatus = "glStatus";
    public static final int PROP_ID_glStatus = 10;
    
    /* 资产模块状态: ASSET_STATUS VARCHAR */
    public static final String PROP_NAME_assetStatus = "assetStatus";
    public static final int PROP_ID_assetStatus = 11;
    
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
    public static final String PROP_NAME_period = "period";
    
    /* relation:  */
    public static final String PROP_NAME_acctSchema = "acctSchema";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[18];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_periodId] = PROP_NAME_periodId;
          PROP_NAME_TO_ID.put(PROP_NAME_periodId, PROP_ID_periodId);
      
          PROP_ID_TO_NAME[PROP_ID_acctSchemaId] = PROP_NAME_acctSchemaId;
          PROP_NAME_TO_ID.put(PROP_NAME_acctSchemaId, PROP_ID_acctSchemaId);
      
          PROP_ID_TO_NAME[PROP_ID_totalVouchers] = PROP_NAME_totalVouchers;
          PROP_NAME_TO_ID.put(PROP_NAME_totalVouchers, PROP_ID_totalVouchers);
      
          PROP_ID_TO_NAME[PROP_ID_postedVouchers] = PROP_NAME_postedVouchers;
          PROP_NAME_TO_ID.put(PROP_NAME_postedVouchers, PROP_ID_postedVouchers);
      
          PROP_ID_TO_NAME[PROP_ID_unpostedVouchers] = PROP_NAME_unpostedVouchers;
          PROP_NAME_TO_ID.put(PROP_NAME_unpostedVouchers, PROP_ID_unpostedVouchers);
      
          PROP_ID_TO_NAME[PROP_ID_arStatus] = PROP_NAME_arStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_arStatus, PROP_ID_arStatus);
      
          PROP_ID_TO_NAME[PROP_ID_apStatus] = PROP_NAME_apStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_apStatus, PROP_ID_apStatus);
      
          PROP_ID_TO_NAME[PROP_ID_invStatus] = PROP_NAME_invStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_invStatus, PROP_ID_invStatus);
      
          PROP_ID_TO_NAME[PROP_ID_glStatus] = PROP_NAME_glStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_glStatus, PROP_ID_glStatus);
      
          PROP_ID_TO_NAME[PROP_ID_assetStatus] = PROP_NAME_assetStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_assetStatus, PROP_ID_assetStatus);
      
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
    
    /* 期间ID: PERIOD_ID */
    private java.lang.Long _periodId;
    
    /* 账套: ACCT_SCHEMA_ID */
    private java.lang.Long _acctSchemaId;
    
    /* 凭证总数: TOTAL_VOUCHERS */
    private java.lang.Integer _totalVouchers;
    
    /* 已过账凭证数: POSTED_VOUCHERS */
    private java.lang.Integer _postedVouchers;
    
    /* 未过账凭证数: UNPOSTED_VOUCHERS */
    private java.lang.Integer _unpostedVouchers;
    
    /* 应收模块状态: AR_STATUS */
    private java.lang.String _arStatus;
    
    /* 应付模块状态: AP_STATUS */
    private java.lang.String _apStatus;
    
    /* 存货模块状态: INV_STATUS */
    private java.lang.String _invStatus;
    
    /* 总账模块状态: GL_STATUS */
    private java.lang.String _glStatus;
    
    /* 资产模块状态: ASSET_STATUS */
    private java.lang.String _assetStatus;
    
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
    

    public _ErpFinAccountingPeriodStatus(){
        // for debug
    }

    protected ErpFinAccountingPeriodStatus newInstance(){
        ErpFinAccountingPeriodStatus entity = new ErpFinAccountingPeriodStatus();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpFinAccountingPeriodStatus cloneInstance() {
        ErpFinAccountingPeriodStatus entity = newInstance();
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
      return "app.erp.fin.dao.entity.ErpFinAccountingPeriodStatus";
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
        
            case PROP_ID_periodId:
               return getPeriodId();
        
            case PROP_ID_acctSchemaId:
               return getAcctSchemaId();
        
            case PROP_ID_totalVouchers:
               return getTotalVouchers();
        
            case PROP_ID_postedVouchers:
               return getPostedVouchers();
        
            case PROP_ID_unpostedVouchers:
               return getUnpostedVouchers();
        
            case PROP_ID_arStatus:
               return getArStatus();
        
            case PROP_ID_apStatus:
               return getApStatus();
        
            case PROP_ID_invStatus:
               return getInvStatus();
        
            case PROP_ID_glStatus:
               return getGlStatus();
        
            case PROP_ID_assetStatus:
               return getAssetStatus();
        
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
        
            case PROP_ID_periodId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_periodId));
               }
               setPeriodId(typedValue);
               break;
            }
        
            case PROP_ID_acctSchemaId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_acctSchemaId));
               }
               setAcctSchemaId(typedValue);
               break;
            }
        
            case PROP_ID_totalVouchers:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_totalVouchers));
               }
               setTotalVouchers(typedValue);
               break;
            }
        
            case PROP_ID_postedVouchers:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_postedVouchers));
               }
               setPostedVouchers(typedValue);
               break;
            }
        
            case PROP_ID_unpostedVouchers:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_unpostedVouchers));
               }
               setUnpostedVouchers(typedValue);
               break;
            }
        
            case PROP_ID_arStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_arStatus));
               }
               setArStatus(typedValue);
               break;
            }
        
            case PROP_ID_apStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_apStatus));
               }
               setApStatus(typedValue);
               break;
            }
        
            case PROP_ID_invStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_invStatus));
               }
               setInvStatus(typedValue);
               break;
            }
        
            case PROP_ID_glStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_glStatus));
               }
               setGlStatus(typedValue);
               break;
            }
        
            case PROP_ID_assetStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_assetStatus));
               }
               setAssetStatus(typedValue);
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
        
            case PROP_ID_periodId:{
               onInitProp(propId);
               this._periodId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_acctSchemaId:{
               onInitProp(propId);
               this._acctSchemaId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_totalVouchers:{
               onInitProp(propId);
               this._totalVouchers = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_postedVouchers:{
               onInitProp(propId);
               this._postedVouchers = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_unpostedVouchers:{
               onInitProp(propId);
               this._unpostedVouchers = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_arStatus:{
               onInitProp(propId);
               this._arStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_apStatus:{
               onInitProp(propId);
               this._apStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_invStatus:{
               onInitProp(propId);
               this._invStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_glStatus:{
               onInitProp(propId);
               this._glStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_assetStatus:{
               onInitProp(propId);
               this._assetStatus = (java.lang.String)value;
               
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
     * 期间ID: PERIOD_ID
     */
    public final java.lang.Long getPeriodId(){
         onPropGet(PROP_ID_periodId);
         return _periodId;
    }

    /**
     * 期间ID: PERIOD_ID
     */
    public final void setPeriodId(java.lang.Long value){
        if(onPropSet(PROP_ID_periodId,value)){
            this._periodId = value;
            internalClearRefs(PROP_ID_periodId);
            
        }
    }
    
    /**
     * 账套: ACCT_SCHEMA_ID
     */
    public final java.lang.Long getAcctSchemaId(){
         onPropGet(PROP_ID_acctSchemaId);
         return _acctSchemaId;
    }

    /**
     * 账套: ACCT_SCHEMA_ID
     */
    public final void setAcctSchemaId(java.lang.Long value){
        if(onPropSet(PROP_ID_acctSchemaId,value)){
            this._acctSchemaId = value;
            internalClearRefs(PROP_ID_acctSchemaId);
            
        }
    }
    
    /**
     * 凭证总数: TOTAL_VOUCHERS
     */
    public final java.lang.Integer getTotalVouchers(){
         onPropGet(PROP_ID_totalVouchers);
         return _totalVouchers;
    }

    /**
     * 凭证总数: TOTAL_VOUCHERS
     */
    public final void setTotalVouchers(java.lang.Integer value){
        if(onPropSet(PROP_ID_totalVouchers,value)){
            this._totalVouchers = value;
            internalClearRefs(PROP_ID_totalVouchers);
            
        }
    }
    
    /**
     * 已过账凭证数: POSTED_VOUCHERS
     */
    public final java.lang.Integer getPostedVouchers(){
         onPropGet(PROP_ID_postedVouchers);
         return _postedVouchers;
    }

    /**
     * 已过账凭证数: POSTED_VOUCHERS
     */
    public final void setPostedVouchers(java.lang.Integer value){
        if(onPropSet(PROP_ID_postedVouchers,value)){
            this._postedVouchers = value;
            internalClearRefs(PROP_ID_postedVouchers);
            
        }
    }
    
    /**
     * 未过账凭证数: UNPOSTED_VOUCHERS
     */
    public final java.lang.Integer getUnpostedVouchers(){
         onPropGet(PROP_ID_unpostedVouchers);
         return _unpostedVouchers;
    }

    /**
     * 未过账凭证数: UNPOSTED_VOUCHERS
     */
    public final void setUnpostedVouchers(java.lang.Integer value){
        if(onPropSet(PROP_ID_unpostedVouchers,value)){
            this._unpostedVouchers = value;
            internalClearRefs(PROP_ID_unpostedVouchers);
            
        }
    }
    
    /**
     * 应收模块状态: AR_STATUS
     */
    public final java.lang.String getArStatus(){
         onPropGet(PROP_ID_arStatus);
         return _arStatus;
    }

    /**
     * 应收模块状态: AR_STATUS
     */
    public final void setArStatus(java.lang.String value){
        if(onPropSet(PROP_ID_arStatus,value)){
            this._arStatus = value;
            internalClearRefs(PROP_ID_arStatus);
            
        }
    }
    
    /**
     * 应付模块状态: AP_STATUS
     */
    public final java.lang.String getApStatus(){
         onPropGet(PROP_ID_apStatus);
         return _apStatus;
    }

    /**
     * 应付模块状态: AP_STATUS
     */
    public final void setApStatus(java.lang.String value){
        if(onPropSet(PROP_ID_apStatus,value)){
            this._apStatus = value;
            internalClearRefs(PROP_ID_apStatus);
            
        }
    }
    
    /**
     * 存货模块状态: INV_STATUS
     */
    public final java.lang.String getInvStatus(){
         onPropGet(PROP_ID_invStatus);
         return _invStatus;
    }

    /**
     * 存货模块状态: INV_STATUS
     */
    public final void setInvStatus(java.lang.String value){
        if(onPropSet(PROP_ID_invStatus,value)){
            this._invStatus = value;
            internalClearRefs(PROP_ID_invStatus);
            
        }
    }
    
    /**
     * 总账模块状态: GL_STATUS
     */
    public final java.lang.String getGlStatus(){
         onPropGet(PROP_ID_glStatus);
         return _glStatus;
    }

    /**
     * 总账模块状态: GL_STATUS
     */
    public final void setGlStatus(java.lang.String value){
        if(onPropSet(PROP_ID_glStatus,value)){
            this._glStatus = value;
            internalClearRefs(PROP_ID_glStatus);
            
        }
    }
    
    /**
     * 资产模块状态: ASSET_STATUS
     */
    public final java.lang.String getAssetStatus(){
         onPropGet(PROP_ID_assetStatus);
         return _assetStatus;
    }

    /**
     * 资产模块状态: ASSET_STATUS
     */
    public final void setAssetStatus(java.lang.String value){
        if(onPropSet(PROP_ID_assetStatus,value)){
            this._assetStatus = value;
            internalClearRefs(PROP_ID_assetStatus);
            
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
    public final app.erp.md.dao.entity.ErpMdAcctSchema getAcctSchema(){
       return (app.erp.md.dao.entity.ErpMdAcctSchema)internalGetRefEntity(PROP_NAME_acctSchema);
    }

    public final void setAcctSchema(app.erp.md.dao.entity.ErpMdAcctSchema refEntity){
   
           if(refEntity == null){
           
                   this.setAcctSchemaId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_acctSchema, refEntity,()->{
           
                           this.setAcctSchemaId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
