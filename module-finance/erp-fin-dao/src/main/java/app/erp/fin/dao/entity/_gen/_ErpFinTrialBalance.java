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

import app.erp.fin.dao.entity.ErpFinTrialBalance;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  试算平衡表: erp_fin_trial_balance
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpFinTrialBalance extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 2;
    
    /* 账套: ACCT_SCHEMA_ID BIGINT */
    public static final String PROP_NAME_acctSchemaId = "acctSchemaId";
    public static final int PROP_ID_acctSchemaId = 3;
    
    /* 期间: PERIOD_ID BIGINT */
    public static final String PROP_NAME_periodId = "periodId";
    public static final int PROP_ID_periodId = 4;
    
    /* 科目: SUBJECT_ID BIGINT */
    public static final String PROP_NAME_subjectId = "subjectId";
    public static final int PROP_ID_subjectId = 5;
    
    /* 科目编码: SUBJECT_CODE VARCHAR */
    public static final String PROP_NAME_subjectCode = "subjectCode";
    public static final int PROP_ID_subjectCode = 6;
    
    /* 科目名称: SUBJECT_NAME VARCHAR */
    public static final String PROP_NAME_subjectName = "subjectName";
    public static final int PROP_ID_subjectName = 7;
    
    /* 期初借方: OPENING_DEBIT DECIMAL */
    public static final String PROP_NAME_openingDebit = "openingDebit";
    public static final int PROP_ID_openingDebit = 8;
    
    /* 期初贷方: OPENING_CREDIT DECIMAL */
    public static final String PROP_NAME_openingCredit = "openingCredit";
    public static final int PROP_ID_openingCredit = 9;
    
    /* 本期借方: PERIOD_DEBIT DECIMAL */
    public static final String PROP_NAME_periodDebit = "periodDebit";
    public static final int PROP_ID_periodDebit = 10;
    
    /* 本期贷方: PERIOD_CREDIT DECIMAL */
    public static final String PROP_NAME_periodCredit = "periodCredit";
    public static final int PROP_ID_periodCredit = 11;
    
    /* 期末借方: CLOSING_DEBIT DECIMAL */
    public static final String PROP_NAME_closingDebit = "closingDebit";
    public static final int PROP_ID_closingDebit = 12;
    
    /* 期末贷方: CLOSING_CREDIT DECIMAL */
    public static final String PROP_NAME_closingCredit = "closingCredit";
    public static final int PROP_ID_closingCredit = 13;
    
    /* 生成时间: GENERATED_AT TIMESTAMP */
    public static final String PROP_NAME_generatedAt = "generatedAt";
    public static final int PROP_ID_generatedAt = 14;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 15;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 16;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 17;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 18;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 19;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 20;
    

    private static int _PROP_ID_BOUND = 21;

    
    /* relation:  */
    public static final String PROP_NAME_acctSchema = "acctSchema";
    
    /* relation:  */
    public static final String PROP_NAME_period = "period";
    
    /* relation:  */
    public static final String PROP_NAME_subject = "subject";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[21];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_acctSchemaId] = PROP_NAME_acctSchemaId;
          PROP_NAME_TO_ID.put(PROP_NAME_acctSchemaId, PROP_ID_acctSchemaId);
      
          PROP_ID_TO_NAME[PROP_ID_periodId] = PROP_NAME_periodId;
          PROP_NAME_TO_ID.put(PROP_NAME_periodId, PROP_ID_periodId);
      
          PROP_ID_TO_NAME[PROP_ID_subjectId] = PROP_NAME_subjectId;
          PROP_NAME_TO_ID.put(PROP_NAME_subjectId, PROP_ID_subjectId);
      
          PROP_ID_TO_NAME[PROP_ID_subjectCode] = PROP_NAME_subjectCode;
          PROP_NAME_TO_ID.put(PROP_NAME_subjectCode, PROP_ID_subjectCode);
      
          PROP_ID_TO_NAME[PROP_ID_subjectName] = PROP_NAME_subjectName;
          PROP_NAME_TO_ID.put(PROP_NAME_subjectName, PROP_ID_subjectName);
      
          PROP_ID_TO_NAME[PROP_ID_openingDebit] = PROP_NAME_openingDebit;
          PROP_NAME_TO_ID.put(PROP_NAME_openingDebit, PROP_ID_openingDebit);
      
          PROP_ID_TO_NAME[PROP_ID_openingCredit] = PROP_NAME_openingCredit;
          PROP_NAME_TO_ID.put(PROP_NAME_openingCredit, PROP_ID_openingCredit);
      
          PROP_ID_TO_NAME[PROP_ID_periodDebit] = PROP_NAME_periodDebit;
          PROP_NAME_TO_ID.put(PROP_NAME_periodDebit, PROP_ID_periodDebit);
      
          PROP_ID_TO_NAME[PROP_ID_periodCredit] = PROP_NAME_periodCredit;
          PROP_NAME_TO_ID.put(PROP_NAME_periodCredit, PROP_ID_periodCredit);
      
          PROP_ID_TO_NAME[PROP_ID_closingDebit] = PROP_NAME_closingDebit;
          PROP_NAME_TO_ID.put(PROP_NAME_closingDebit, PROP_ID_closingDebit);
      
          PROP_ID_TO_NAME[PROP_ID_closingCredit] = PROP_NAME_closingCredit;
          PROP_NAME_TO_ID.put(PROP_NAME_closingCredit, PROP_ID_closingCredit);
      
          PROP_ID_TO_NAME[PROP_ID_generatedAt] = PROP_NAME_generatedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_generatedAt, PROP_ID_generatedAt);
      
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
    
    /* 组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 账套: ACCT_SCHEMA_ID */
    private java.lang.Long _acctSchemaId;
    
    /* 期间: PERIOD_ID */
    private java.lang.Long _periodId;
    
    /* 科目: SUBJECT_ID */
    private java.lang.Long _subjectId;
    
    /* 科目编码: SUBJECT_CODE */
    private java.lang.String _subjectCode;
    
    /* 科目名称: SUBJECT_NAME */
    private java.lang.String _subjectName;
    
    /* 期初借方: OPENING_DEBIT */
    private java.math.BigDecimal _openingDebit;
    
    /* 期初贷方: OPENING_CREDIT */
    private java.math.BigDecimal _openingCredit;
    
    /* 本期借方: PERIOD_DEBIT */
    private java.math.BigDecimal _periodDebit;
    
    /* 本期贷方: PERIOD_CREDIT */
    private java.math.BigDecimal _periodCredit;
    
    /* 期末借方: CLOSING_DEBIT */
    private java.math.BigDecimal _closingDebit;
    
    /* 期末贷方: CLOSING_CREDIT */
    private java.math.BigDecimal _closingCredit;
    
    /* 生成时间: GENERATED_AT */
    private java.sql.Timestamp _generatedAt;
    
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
    

    public _ErpFinTrialBalance(){
        // for debug
    }

    protected ErpFinTrialBalance newInstance(){
        ErpFinTrialBalance entity = new ErpFinTrialBalance();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpFinTrialBalance cloneInstance() {
        ErpFinTrialBalance entity = newInstance();
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
      return "app.erp.fin.dao.entity.ErpFinTrialBalance";
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
        
            case PROP_ID_acctSchemaId:
               return getAcctSchemaId();
        
            case PROP_ID_periodId:
               return getPeriodId();
        
            case PROP_ID_subjectId:
               return getSubjectId();
        
            case PROP_ID_subjectCode:
               return getSubjectCode();
        
            case PROP_ID_subjectName:
               return getSubjectName();
        
            case PROP_ID_openingDebit:
               return getOpeningDebit();
        
            case PROP_ID_openingCredit:
               return getOpeningCredit();
        
            case PROP_ID_periodDebit:
               return getPeriodDebit();
        
            case PROP_ID_periodCredit:
               return getPeriodCredit();
        
            case PROP_ID_closingDebit:
               return getClosingDebit();
        
            case PROP_ID_closingCredit:
               return getClosingCredit();
        
            case PROP_ID_generatedAt:
               return getGeneratedAt();
        
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
        
            case PROP_ID_acctSchemaId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_acctSchemaId));
               }
               setAcctSchemaId(typedValue);
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
        
            case PROP_ID_subjectId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_subjectId));
               }
               setSubjectId(typedValue);
               break;
            }
        
            case PROP_ID_subjectCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_subjectCode));
               }
               setSubjectCode(typedValue);
               break;
            }
        
            case PROP_ID_subjectName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_subjectName));
               }
               setSubjectName(typedValue);
               break;
            }
        
            case PROP_ID_openingDebit:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_openingDebit));
               }
               setOpeningDebit(typedValue);
               break;
            }
        
            case PROP_ID_openingCredit:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_openingCredit));
               }
               setOpeningCredit(typedValue);
               break;
            }
        
            case PROP_ID_periodDebit:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_periodDebit));
               }
               setPeriodDebit(typedValue);
               break;
            }
        
            case PROP_ID_periodCredit:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_periodCredit));
               }
               setPeriodCredit(typedValue);
               break;
            }
        
            case PROP_ID_closingDebit:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_closingDebit));
               }
               setClosingDebit(typedValue);
               break;
            }
        
            case PROP_ID_closingCredit:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_closingCredit));
               }
               setClosingCredit(typedValue);
               break;
            }
        
            case PROP_ID_generatedAt:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_generatedAt));
               }
               setGeneratedAt(typedValue);
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
        
            case PROP_ID_acctSchemaId:{
               onInitProp(propId);
               this._acctSchemaId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_periodId:{
               onInitProp(propId);
               this._periodId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_subjectId:{
               onInitProp(propId);
               this._subjectId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_subjectCode:{
               onInitProp(propId);
               this._subjectCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_subjectName:{
               onInitProp(propId);
               this._subjectName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_openingDebit:{
               onInitProp(propId);
               this._openingDebit = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_openingCredit:{
               onInitProp(propId);
               this._openingCredit = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_periodDebit:{
               onInitProp(propId);
               this._periodDebit = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_periodCredit:{
               onInitProp(propId);
               this._periodCredit = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_closingDebit:{
               onInitProp(propId);
               this._closingDebit = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_closingCredit:{
               onInitProp(propId);
               this._closingCredit = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_generatedAt:{
               onInitProp(propId);
               this._generatedAt = (java.sql.Timestamp)value;
               
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
     * 组织: ORG_ID
     */
    public final java.lang.Long getOrgId(){
         onPropGet(PROP_ID_orgId);
         return _orgId;
    }

    /**
     * 组织: ORG_ID
     */
    public final void setOrgId(java.lang.Long value){
        if(onPropSet(PROP_ID_orgId,value)){
            this._orgId = value;
            internalClearRefs(PROP_ID_orgId);
            
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
     * 期间: PERIOD_ID
     */
    public final java.lang.Long getPeriodId(){
         onPropGet(PROP_ID_periodId);
         return _periodId;
    }

    /**
     * 期间: PERIOD_ID
     */
    public final void setPeriodId(java.lang.Long value){
        if(onPropSet(PROP_ID_periodId,value)){
            this._periodId = value;
            internalClearRefs(PROP_ID_periodId);
            
        }
    }
    
    /**
     * 科目: SUBJECT_ID
     */
    public final java.lang.Long getSubjectId(){
         onPropGet(PROP_ID_subjectId);
         return _subjectId;
    }

    /**
     * 科目: SUBJECT_ID
     */
    public final void setSubjectId(java.lang.Long value){
        if(onPropSet(PROP_ID_subjectId,value)){
            this._subjectId = value;
            internalClearRefs(PROP_ID_subjectId);
            
        }
    }
    
    /**
     * 科目编码: SUBJECT_CODE
     */
    public final java.lang.String getSubjectCode(){
         onPropGet(PROP_ID_subjectCode);
         return _subjectCode;
    }

    /**
     * 科目编码: SUBJECT_CODE
     */
    public final void setSubjectCode(java.lang.String value){
        if(onPropSet(PROP_ID_subjectCode,value)){
            this._subjectCode = value;
            internalClearRefs(PROP_ID_subjectCode);
            
        }
    }
    
    /**
     * 科目名称: SUBJECT_NAME
     */
    public final java.lang.String getSubjectName(){
         onPropGet(PROP_ID_subjectName);
         return _subjectName;
    }

    /**
     * 科目名称: SUBJECT_NAME
     */
    public final void setSubjectName(java.lang.String value){
        if(onPropSet(PROP_ID_subjectName,value)){
            this._subjectName = value;
            internalClearRefs(PROP_ID_subjectName);
            
        }
    }
    
    /**
     * 期初借方: OPENING_DEBIT
     */
    public final java.math.BigDecimal getOpeningDebit(){
         onPropGet(PROP_ID_openingDebit);
         return _openingDebit;
    }

    /**
     * 期初借方: OPENING_DEBIT
     */
    public final void setOpeningDebit(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_openingDebit,value)){
            this._openingDebit = value;
            internalClearRefs(PROP_ID_openingDebit);
            
        }
    }
    
    /**
     * 期初贷方: OPENING_CREDIT
     */
    public final java.math.BigDecimal getOpeningCredit(){
         onPropGet(PROP_ID_openingCredit);
         return _openingCredit;
    }

    /**
     * 期初贷方: OPENING_CREDIT
     */
    public final void setOpeningCredit(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_openingCredit,value)){
            this._openingCredit = value;
            internalClearRefs(PROP_ID_openingCredit);
            
        }
    }
    
    /**
     * 本期借方: PERIOD_DEBIT
     */
    public final java.math.BigDecimal getPeriodDebit(){
         onPropGet(PROP_ID_periodDebit);
         return _periodDebit;
    }

    /**
     * 本期借方: PERIOD_DEBIT
     */
    public final void setPeriodDebit(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_periodDebit,value)){
            this._periodDebit = value;
            internalClearRefs(PROP_ID_periodDebit);
            
        }
    }
    
    /**
     * 本期贷方: PERIOD_CREDIT
     */
    public final java.math.BigDecimal getPeriodCredit(){
         onPropGet(PROP_ID_periodCredit);
         return _periodCredit;
    }

    /**
     * 本期贷方: PERIOD_CREDIT
     */
    public final void setPeriodCredit(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_periodCredit,value)){
            this._periodCredit = value;
            internalClearRefs(PROP_ID_periodCredit);
            
        }
    }
    
    /**
     * 期末借方: CLOSING_DEBIT
     */
    public final java.math.BigDecimal getClosingDebit(){
         onPropGet(PROP_ID_closingDebit);
         return _closingDebit;
    }

    /**
     * 期末借方: CLOSING_DEBIT
     */
    public final void setClosingDebit(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_closingDebit,value)){
            this._closingDebit = value;
            internalClearRefs(PROP_ID_closingDebit);
            
        }
    }
    
    /**
     * 期末贷方: CLOSING_CREDIT
     */
    public final java.math.BigDecimal getClosingCredit(){
         onPropGet(PROP_ID_closingCredit);
         return _closingCredit;
    }

    /**
     * 期末贷方: CLOSING_CREDIT
     */
    public final void setClosingCredit(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_closingCredit,value)){
            this._closingCredit = value;
            internalClearRefs(PROP_ID_closingCredit);
            
        }
    }
    
    /**
     * 生成时间: GENERATED_AT
     */
    public final java.sql.Timestamp getGeneratedAt(){
         onPropGet(PROP_ID_generatedAt);
         return _generatedAt;
    }

    /**
     * 生成时间: GENERATED_AT
     */
    public final void setGeneratedAt(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_generatedAt,value)){
            this._generatedAt = value;
            internalClearRefs(PROP_ID_generatedAt);
            
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
    public final app.erp.md.dao.entity.ErpMdSubject getSubject(){
       return (app.erp.md.dao.entity.ErpMdSubject)internalGetRefEntity(PROP_NAME_subject);
    }

    public final void setSubject(app.erp.md.dao.entity.ErpMdSubject refEntity){
   
           if(refEntity == null){
           
                   this.setSubjectId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_subject, refEntity,()->{
           
                           this.setSubjectId(refEntity.getId());
                       
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
