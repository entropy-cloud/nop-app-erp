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

import app.erp.fin.dao.entity.ErpFinBankReconciliation;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  银行对账(余额调节表): erp_fin_bank_reconciliation
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpFinBankReconciliation extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 单号: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 资金账户: FUND_ACCOUNT_ID BIGINT */
    public static final String PROP_NAME_fundAccountId = "fundAccountId";
    public static final int PROP_ID_fundAccountId = 4;
    
    /* 银行对账单: STATEMENT_ID BIGINT */
    public static final String PROP_NAME_statementId = "statementId";
    public static final int PROP_ID_statementId = 5;
    
    /* 对账日期: RECONCILIATION_DATE DATE */
    public static final String PROP_NAME_reconciliationDate = "reconciliationDate";
    public static final int PROP_ID_reconciliationDate = 6;
    
    /* 账面余额: BOOK_BALANCE DECIMAL */
    public static final String PROP_NAME_bookBalance = "bookBalance";
    public static final int PROP_ID_bookBalance = 7;
    
    /* 对账单余额: STATEMENT_BALANCE DECIMAL */
    public static final String PROP_NAME_statementBalance = "statementBalance";
    public static final int PROP_ID_statementBalance = 8;
    
    /* 未达差异: UNRECONCILED_DIFF DECIMAL */
    public static final String PROP_NAME_unreconciledDiff = "unreconciledDiff";
    public static final int PROP_ID_unreconciledDiff = 9;
    
    /* 是否平衡: IS_BALANCED BOOLEAN */
    public static final String PROP_NAME_isBalanced = "isBalanced";
    public static final int PROP_ID_isBalanced = 10;
    
    /* 对账完成时间: RECONCILED_AT DATETIME */
    public static final String PROP_NAME_reconciledAt = "reconciledAt";
    public static final int PROP_ID_reconciledAt = 11;
    
    /* 对账人: RECONCILED_BY BIGINT */
    public static final String PROP_NAME_reconciledBy = "reconciledBy";
    public static final int PROP_ID_reconciledBy = 12;
    
    /* 状态: DOC_STATUS VARCHAR */
    public static final String PROP_NAME_docStatus = "docStatus";
    public static final int PROP_ID_docStatus = 13;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 14;
    
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
    public static final String PROP_NAME_fundAccount = "fundAccount";
    
    /* relation:  */
    public static final String PROP_NAME_statement = "statement";
    
    /* relation:  */
    public static final String PROP_NAME_adjustmentLines = "adjustmentLines";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[21];
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
      
          PROP_ID_TO_NAME[PROP_ID_statementId] = PROP_NAME_statementId;
          PROP_NAME_TO_ID.put(PROP_NAME_statementId, PROP_ID_statementId);
      
          PROP_ID_TO_NAME[PROP_ID_reconciliationDate] = PROP_NAME_reconciliationDate;
          PROP_NAME_TO_ID.put(PROP_NAME_reconciliationDate, PROP_ID_reconciliationDate);
      
          PROP_ID_TO_NAME[PROP_ID_bookBalance] = PROP_NAME_bookBalance;
          PROP_NAME_TO_ID.put(PROP_NAME_bookBalance, PROP_ID_bookBalance);
      
          PROP_ID_TO_NAME[PROP_ID_statementBalance] = PROP_NAME_statementBalance;
          PROP_NAME_TO_ID.put(PROP_NAME_statementBalance, PROP_ID_statementBalance);
      
          PROP_ID_TO_NAME[PROP_ID_unreconciledDiff] = PROP_NAME_unreconciledDiff;
          PROP_NAME_TO_ID.put(PROP_NAME_unreconciledDiff, PROP_ID_unreconciledDiff);
      
          PROP_ID_TO_NAME[PROP_ID_isBalanced] = PROP_NAME_isBalanced;
          PROP_NAME_TO_ID.put(PROP_NAME_isBalanced, PROP_ID_isBalanced);
      
          PROP_ID_TO_NAME[PROP_ID_reconciledAt] = PROP_NAME_reconciledAt;
          PROP_NAME_TO_ID.put(PROP_NAME_reconciledAt, PROP_ID_reconciledAt);
      
          PROP_ID_TO_NAME[PROP_ID_reconciledBy] = PROP_NAME_reconciledBy;
          PROP_NAME_TO_ID.put(PROP_NAME_reconciledBy, PROP_ID_reconciledBy);
      
          PROP_ID_TO_NAME[PROP_ID_docStatus] = PROP_NAME_docStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_docStatus, PROP_ID_docStatus);
      
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
    
    /* 组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 资金账户: FUND_ACCOUNT_ID */
    private java.lang.Long _fundAccountId;
    
    /* 银行对账单: STATEMENT_ID */
    private java.lang.Long _statementId;
    
    /* 对账日期: RECONCILIATION_DATE */
    private java.time.LocalDate _reconciliationDate;
    
    /* 账面余额: BOOK_BALANCE */
    private java.math.BigDecimal _bookBalance;
    
    /* 对账单余额: STATEMENT_BALANCE */
    private java.math.BigDecimal _statementBalance;
    
    /* 未达差异: UNRECONCILED_DIFF */
    private java.math.BigDecimal _unreconciledDiff;
    
    /* 是否平衡: IS_BALANCED */
    private java.lang.Boolean _isBalanced;
    
    /* 对账完成时间: RECONCILED_AT */
    private java.time.LocalDateTime _reconciledAt;
    
    /* 对账人: RECONCILED_BY */
    private java.lang.Long _reconciledBy;
    
    /* 状态: DOC_STATUS */
    private java.lang.String _docStatus;
    
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
    

    public _ErpFinBankReconciliation(){
        // for debug
    }

    protected ErpFinBankReconciliation newInstance(){
        ErpFinBankReconciliation entity = new ErpFinBankReconciliation();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpFinBankReconciliation cloneInstance() {
        ErpFinBankReconciliation entity = newInstance();
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
      return "app.erp.fin.dao.entity.ErpFinBankReconciliation";
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
        
            case PROP_ID_statementId:
               return getStatementId();
        
            case PROP_ID_reconciliationDate:
               return getReconciliationDate();
        
            case PROP_ID_bookBalance:
               return getBookBalance();
        
            case PROP_ID_statementBalance:
               return getStatementBalance();
        
            case PROP_ID_unreconciledDiff:
               return getUnreconciledDiff();
        
            case PROP_ID_isBalanced:
               return getIsBalanced();
        
            case PROP_ID_reconciledAt:
               return getReconciledAt();
        
            case PROP_ID_reconciledBy:
               return getReconciledBy();
        
            case PROP_ID_docStatus:
               return getDocStatus();
        
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
        
            case PROP_ID_statementId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_statementId));
               }
               setStatementId(typedValue);
               break;
            }
        
            case PROP_ID_reconciliationDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_reconciliationDate));
               }
               setReconciliationDate(typedValue);
               break;
            }
        
            case PROP_ID_bookBalance:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_bookBalance));
               }
               setBookBalance(typedValue);
               break;
            }
        
            case PROP_ID_statementBalance:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_statementBalance));
               }
               setStatementBalance(typedValue);
               break;
            }
        
            case PROP_ID_unreconciledDiff:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_unreconciledDiff));
               }
               setUnreconciledDiff(typedValue);
               break;
            }
        
            case PROP_ID_isBalanced:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isBalanced));
               }
               setIsBalanced(typedValue);
               break;
            }
        
            case PROP_ID_reconciledAt:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_reconciledAt));
               }
               setReconciledAt(typedValue);
               break;
            }
        
            case PROP_ID_reconciledBy:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_reconciledBy));
               }
               setReconciledBy(typedValue);
               break;
            }
        
            case PROP_ID_docStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_docStatus));
               }
               setDocStatus(typedValue);
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
        
            case PROP_ID_statementId:{
               onInitProp(propId);
               this._statementId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_reconciliationDate:{
               onInitProp(propId);
               this._reconciliationDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_bookBalance:{
               onInitProp(propId);
               this._bookBalance = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_statementBalance:{
               onInitProp(propId);
               this._statementBalance = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_unreconciledDiff:{
               onInitProp(propId);
               this._unreconciledDiff = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_isBalanced:{
               onInitProp(propId);
               this._isBalanced = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_reconciledAt:{
               onInitProp(propId);
               this._reconciledAt = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_reconciledBy:{
               onInitProp(propId);
               this._reconciledBy = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_docStatus:{
               onInitProp(propId);
               this._docStatus = (java.lang.String)value;
               
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
     * 资金账户: FUND_ACCOUNT_ID
     */
    public final java.lang.Long getFundAccountId(){
         onPropGet(PROP_ID_fundAccountId);
         return _fundAccountId;
    }

    /**
     * 资金账户: FUND_ACCOUNT_ID
     */
    public final void setFundAccountId(java.lang.Long value){
        if(onPropSet(PROP_ID_fundAccountId,value)){
            this._fundAccountId = value;
            internalClearRefs(PROP_ID_fundAccountId);
            
        }
    }
    
    /**
     * 银行对账单: STATEMENT_ID
     */
    public final java.lang.Long getStatementId(){
         onPropGet(PROP_ID_statementId);
         return _statementId;
    }

    /**
     * 银行对账单: STATEMENT_ID
     */
    public final void setStatementId(java.lang.Long value){
        if(onPropSet(PROP_ID_statementId,value)){
            this._statementId = value;
            internalClearRefs(PROP_ID_statementId);
            
        }
    }
    
    /**
     * 对账日期: RECONCILIATION_DATE
     */
    public final java.time.LocalDate getReconciliationDate(){
         onPropGet(PROP_ID_reconciliationDate);
         return _reconciliationDate;
    }

    /**
     * 对账日期: RECONCILIATION_DATE
     */
    public final void setReconciliationDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_reconciliationDate,value)){
            this._reconciliationDate = value;
            internalClearRefs(PROP_ID_reconciliationDate);
            
        }
    }
    
    /**
     * 账面余额: BOOK_BALANCE
     */
    public final java.math.BigDecimal getBookBalance(){
         onPropGet(PROP_ID_bookBalance);
         return _bookBalance;
    }

    /**
     * 账面余额: BOOK_BALANCE
     */
    public final void setBookBalance(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_bookBalance,value)){
            this._bookBalance = value;
            internalClearRefs(PROP_ID_bookBalance);
            
        }
    }
    
    /**
     * 对账单余额: STATEMENT_BALANCE
     */
    public final java.math.BigDecimal getStatementBalance(){
         onPropGet(PROP_ID_statementBalance);
         return _statementBalance;
    }

    /**
     * 对账单余额: STATEMENT_BALANCE
     */
    public final void setStatementBalance(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_statementBalance,value)){
            this._statementBalance = value;
            internalClearRefs(PROP_ID_statementBalance);
            
        }
    }
    
    /**
     * 未达差异: UNRECONCILED_DIFF
     */
    public final java.math.BigDecimal getUnreconciledDiff(){
         onPropGet(PROP_ID_unreconciledDiff);
         return _unreconciledDiff;
    }

    /**
     * 未达差异: UNRECONCILED_DIFF
     */
    public final void setUnreconciledDiff(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_unreconciledDiff,value)){
            this._unreconciledDiff = value;
            internalClearRefs(PROP_ID_unreconciledDiff);
            
        }
    }
    
    /**
     * 是否平衡: IS_BALANCED
     */
    public final java.lang.Boolean getIsBalanced(){
         onPropGet(PROP_ID_isBalanced);
         return _isBalanced;
    }

    /**
     * 是否平衡: IS_BALANCED
     */
    public final void setIsBalanced(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isBalanced,value)){
            this._isBalanced = value;
            internalClearRefs(PROP_ID_isBalanced);
            
        }
    }
    
    /**
     * 对账完成时间: RECONCILED_AT
     */
    public final java.time.LocalDateTime getReconciledAt(){
         onPropGet(PROP_ID_reconciledAt);
         return _reconciledAt;
    }

    /**
     * 对账完成时间: RECONCILED_AT
     */
    public final void setReconciledAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_reconciledAt,value)){
            this._reconciledAt = value;
            internalClearRefs(PROP_ID_reconciledAt);
            
        }
    }
    
    /**
     * 对账人: RECONCILED_BY
     */
    public final java.lang.Long getReconciledBy(){
         onPropGet(PROP_ID_reconciledBy);
         return _reconciledBy;
    }

    /**
     * 对账人: RECONCILED_BY
     */
    public final void setReconciledBy(java.lang.Long value){
        if(onPropSet(PROP_ID_reconciledBy,value)){
            this._reconciledBy = value;
            internalClearRefs(PROP_ID_reconciledBy);
            
        }
    }
    
    /**
     * 状态: DOC_STATUS
     */
    public final java.lang.String getDocStatus(){
         onPropGet(PROP_ID_docStatus);
         return _docStatus;
    }

    /**
     * 状态: DOC_STATUS
     */
    public final void setDocStatus(java.lang.String value){
        if(onPropSet(PROP_ID_docStatus,value)){
            this._docStatus = value;
            internalClearRefs(PROP_ID_docStatus);
            
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
    public final app.erp.fin.dao.entity.ErpFinBankStatement getStatement(){
       return (app.erp.fin.dao.entity.ErpFinBankStatement)internalGetRefEntity(PROP_NAME_statement);
    }

    public final void setStatement(app.erp.fin.dao.entity.ErpFinBankStatement refEntity){
   
           if(refEntity == null){
           
                   this.setStatementId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_statement, refEntity,()->{
           
                           this.setStatementId(refEntity.getId());
                       
           });
           }
       
    }
       
    private final OrmEntitySet<app.erp.fin.dao.entity.ErpFinBankReconciliationLine> _adjustmentLines = new OrmEntitySet<>(this, PROP_NAME_adjustmentLines,
        null, null,app.erp.fin.dao.entity.ErpFinBankReconciliationLine.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.fin.dao.entity.ErpFinBankReconciliationLine> getAdjustmentLines(){
       return _adjustmentLines;
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
