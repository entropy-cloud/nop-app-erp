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

import app.erp.fin.dao.entity.ErpFinVoucher;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  会计凭证: erp_fin_voucher
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpFinVoucher extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 凭证号: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 凭证字: VOUCHER_TYPE INTEGER */
    public static final String PROP_NAME_voucherType = "voucherType";
    public static final int PROP_ID_voucherType = 3;
    
    /* 凭证日期: VOUCHER_DATE DATE */
    public static final String PROP_NAME_voucherDate = "voucherDate";
    public static final int PROP_ID_voucherDate = 4;
    
    /* 凭证编号: VOUCHER_NO VARCHAR */
    public static final String PROP_NAME_voucherNo = "voucherNo";
    public static final int PROP_ID_voucherNo = 5;
    
    /* 核算组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 6;
    
    /* 账套: ACCT_SCHEMA_ID BIGINT */
    public static final String PROP_NAME_acctSchemaId = "acctSchemaId";
    public static final int PROP_ID_acctSchemaId = 7;
    
    /* 会计期间: PERIOD_ID BIGINT */
    public static final String PROP_NAME_periodId = "periodId";
    public static final int PROP_ID_periodId = 8;
    
    /* 借方合计(本位币): TOTAL_DEBIT DECIMAL */
    public static final String PROP_NAME_totalDebit = "totalDebit";
    public static final int PROP_ID_totalDebit = 9;
    
    /* 贷方合计(本位币): TOTAL_CREDIT DECIMAL */
    public static final String PROP_NAME_totalCredit = "totalCredit";
    public static final int PROP_ID_totalCredit = 10;
    
    /* 是否红字冲销凭证: IS_REVERSED BOOLEAN */
    public static final String PROP_NAME_isReversed = "isReversed";
    public static final int PROP_ID_isReversed = 11;
    
    /* 原冲销凭证ID: REVERSAL_OF_VOUCHER_ID BIGINT */
    public static final String PROP_NAME_reversalOfVoucherId = "reversalOfVoucherId";
    public static final int PROP_ID_reversalOfVoucherId = 12;
    
    /* 凭证状态: DOC_STATUS INTEGER */
    public static final String PROP_NAME_docStatus = "docStatus";
    public static final int PROP_ID_docStatus = 13;
    
    /* 过账人: POSTED_BY BIGINT */
    public static final String PROP_NAME_postedBy = "postedBy";
    public static final int PROP_ID_postedBy = 14;
    
    /* 过账时间: POSTED_AT DATETIME */
    public static final String PROP_NAME_postedAt = "postedAt";
    public static final int PROP_ID_postedAt = 15;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 16;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 17;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 18;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 19;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 20;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 21;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 22;
    

    private static int _PROP_ID_BOUND = 23;

    
    /* relation:  */
    public static final String PROP_NAME_period = "period";
    
    /* relation:  */
    public static final String PROP_NAME_acctSchema = "acctSchema";
    
    /* relation:  */
    public static final String PROP_NAME_lines = "lines";
    
    /* relation:  */
    public static final String PROP_NAME_billLinks = "billLinks";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[23];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_voucherType] = PROP_NAME_voucherType;
          PROP_NAME_TO_ID.put(PROP_NAME_voucherType, PROP_ID_voucherType);
      
          PROP_ID_TO_NAME[PROP_ID_voucherDate] = PROP_NAME_voucherDate;
          PROP_NAME_TO_ID.put(PROP_NAME_voucherDate, PROP_ID_voucherDate);
      
          PROP_ID_TO_NAME[PROP_ID_voucherNo] = PROP_NAME_voucherNo;
          PROP_NAME_TO_ID.put(PROP_NAME_voucherNo, PROP_ID_voucherNo);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_acctSchemaId] = PROP_NAME_acctSchemaId;
          PROP_NAME_TO_ID.put(PROP_NAME_acctSchemaId, PROP_ID_acctSchemaId);
      
          PROP_ID_TO_NAME[PROP_ID_periodId] = PROP_NAME_periodId;
          PROP_NAME_TO_ID.put(PROP_NAME_periodId, PROP_ID_periodId);
      
          PROP_ID_TO_NAME[PROP_ID_totalDebit] = PROP_NAME_totalDebit;
          PROP_NAME_TO_ID.put(PROP_NAME_totalDebit, PROP_ID_totalDebit);
      
          PROP_ID_TO_NAME[PROP_ID_totalCredit] = PROP_NAME_totalCredit;
          PROP_NAME_TO_ID.put(PROP_NAME_totalCredit, PROP_ID_totalCredit);
      
          PROP_ID_TO_NAME[PROP_ID_isReversed] = PROP_NAME_isReversed;
          PROP_NAME_TO_ID.put(PROP_NAME_isReversed, PROP_ID_isReversed);
      
          PROP_ID_TO_NAME[PROP_ID_reversalOfVoucherId] = PROP_NAME_reversalOfVoucherId;
          PROP_NAME_TO_ID.put(PROP_NAME_reversalOfVoucherId, PROP_ID_reversalOfVoucherId);
      
          PROP_ID_TO_NAME[PROP_ID_docStatus] = PROP_NAME_docStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_docStatus, PROP_ID_docStatus);
      
          PROP_ID_TO_NAME[PROP_ID_postedBy] = PROP_NAME_postedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_postedBy, PROP_ID_postedBy);
      
          PROP_ID_TO_NAME[PROP_ID_postedAt] = PROP_NAME_postedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_postedAt, PROP_ID_postedAt);
      
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
    
    /* 凭证号: CODE */
    private java.lang.String _code;
    
    /* 凭证字: VOUCHER_TYPE */
    private java.lang.Integer _voucherType;
    
    /* 凭证日期: VOUCHER_DATE */
    private java.time.LocalDate _voucherDate;
    
    /* 凭证编号: VOUCHER_NO */
    private java.lang.String _voucherNo;
    
    /* 核算组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 账套: ACCT_SCHEMA_ID */
    private java.lang.Long _acctSchemaId;
    
    /* 会计期间: PERIOD_ID */
    private java.lang.Long _periodId;
    
    /* 借方合计(本位币): TOTAL_DEBIT */
    private java.lang.String _totalDebit;
    
    /* 贷方合计(本位币): TOTAL_CREDIT */
    private java.lang.String _totalCredit;
    
    /* 是否红字冲销凭证: IS_REVERSED */
    private java.lang.Boolean _isReversed;
    
    /* 原冲销凭证ID: REVERSAL_OF_VOUCHER_ID */
    private java.lang.Long _reversalOfVoucherId;
    
    /* 凭证状态: DOC_STATUS */
    private java.lang.Integer _docStatus;
    
    /* 过账人: POSTED_BY */
    private java.lang.Long _postedBy;
    
    /* 过账时间: POSTED_AT */
    private java.time.LocalDateTime _postedAt;
    
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
    

    public _ErpFinVoucher(){
        // for debug
    }

    protected ErpFinVoucher newInstance(){
        ErpFinVoucher entity = new ErpFinVoucher();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpFinVoucher cloneInstance() {
        ErpFinVoucher entity = newInstance();
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
      return "app.erp.fin.dao.entity.ErpFinVoucher";
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
        
            case PROP_ID_voucherType:
               return getVoucherType();
        
            case PROP_ID_voucherDate:
               return getVoucherDate();
        
            case PROP_ID_voucherNo:
               return getVoucherNo();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_acctSchemaId:
               return getAcctSchemaId();
        
            case PROP_ID_periodId:
               return getPeriodId();
        
            case PROP_ID_totalDebit:
               return getTotalDebit();
        
            case PROP_ID_totalCredit:
               return getTotalCredit();
        
            case PROP_ID_isReversed:
               return getIsReversed();
        
            case PROP_ID_reversalOfVoucherId:
               return getReversalOfVoucherId();
        
            case PROP_ID_docStatus:
               return getDocStatus();
        
            case PROP_ID_postedBy:
               return getPostedBy();
        
            case PROP_ID_postedAt:
               return getPostedAt();
        
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
        
            case PROP_ID_voucherType:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_voucherType));
               }
               setVoucherType(typedValue);
               break;
            }
        
            case PROP_ID_voucherDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_voucherDate));
               }
               setVoucherDate(typedValue);
               break;
            }
        
            case PROP_ID_voucherNo:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_voucherNo));
               }
               setVoucherNo(typedValue);
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
        
            case PROP_ID_totalDebit:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_totalDebit));
               }
               setTotalDebit(typedValue);
               break;
            }
        
            case PROP_ID_totalCredit:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_totalCredit));
               }
               setTotalCredit(typedValue);
               break;
            }
        
            case PROP_ID_isReversed:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isReversed));
               }
               setIsReversed(typedValue);
               break;
            }
        
            case PROP_ID_reversalOfVoucherId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_reversalOfVoucherId));
               }
               setReversalOfVoucherId(typedValue);
               break;
            }
        
            case PROP_ID_docStatus:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_docStatus));
               }
               setDocStatus(typedValue);
               break;
            }
        
            case PROP_ID_postedBy:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_postedBy));
               }
               setPostedBy(typedValue);
               break;
            }
        
            case PROP_ID_postedAt:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_postedAt));
               }
               setPostedAt(typedValue);
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
        
            case PROP_ID_voucherType:{
               onInitProp(propId);
               this._voucherType = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_voucherDate:{
               onInitProp(propId);
               this._voucherDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_voucherNo:{
               onInitProp(propId);
               this._voucherNo = (java.lang.String)value;
               
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
        
            case PROP_ID_totalDebit:{
               onInitProp(propId);
               this._totalDebit = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_totalCredit:{
               onInitProp(propId);
               this._totalCredit = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_isReversed:{
               onInitProp(propId);
               this._isReversed = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_reversalOfVoucherId:{
               onInitProp(propId);
               this._reversalOfVoucherId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_docStatus:{
               onInitProp(propId);
               this._docStatus = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_postedBy:{
               onInitProp(propId);
               this._postedBy = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_postedAt:{
               onInitProp(propId);
               this._postedAt = (java.time.LocalDateTime)value;
               
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
     * 凭证号: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 凭证号: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
        }
    }
    
    /**
     * 凭证字: VOUCHER_TYPE
     */
    public final java.lang.Integer getVoucherType(){
         onPropGet(PROP_ID_voucherType);
         return _voucherType;
    }

    /**
     * 凭证字: VOUCHER_TYPE
     */
    public final void setVoucherType(java.lang.Integer value){
        if(onPropSet(PROP_ID_voucherType,value)){
            this._voucherType = value;
            internalClearRefs(PROP_ID_voucherType);
            
        }
    }
    
    /**
     * 凭证日期: VOUCHER_DATE
     */
    public final java.time.LocalDate getVoucherDate(){
         onPropGet(PROP_ID_voucherDate);
         return _voucherDate;
    }

    /**
     * 凭证日期: VOUCHER_DATE
     */
    public final void setVoucherDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_voucherDate,value)){
            this._voucherDate = value;
            internalClearRefs(PROP_ID_voucherDate);
            
        }
    }
    
    /**
     * 凭证编号: VOUCHER_NO
     */
    public final java.lang.String getVoucherNo(){
         onPropGet(PROP_ID_voucherNo);
         return _voucherNo;
    }

    /**
     * 凭证编号: VOUCHER_NO
     */
    public final void setVoucherNo(java.lang.String value){
        if(onPropSet(PROP_ID_voucherNo,value)){
            this._voucherNo = value;
            internalClearRefs(PROP_ID_voucherNo);
            
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
     * 借方合计(本位币): TOTAL_DEBIT
     */
    public final java.lang.String getTotalDebit(){
         onPropGet(PROP_ID_totalDebit);
         return _totalDebit;
    }

    /**
     * 借方合计(本位币): TOTAL_DEBIT
     */
    public final void setTotalDebit(java.lang.String value){
        if(onPropSet(PROP_ID_totalDebit,value)){
            this._totalDebit = value;
            internalClearRefs(PROP_ID_totalDebit);
            
        }
    }
    
    /**
     * 贷方合计(本位币): TOTAL_CREDIT
     */
    public final java.lang.String getTotalCredit(){
         onPropGet(PROP_ID_totalCredit);
         return _totalCredit;
    }

    /**
     * 贷方合计(本位币): TOTAL_CREDIT
     */
    public final void setTotalCredit(java.lang.String value){
        if(onPropSet(PROP_ID_totalCredit,value)){
            this._totalCredit = value;
            internalClearRefs(PROP_ID_totalCredit);
            
        }
    }
    
    /**
     * 是否红字冲销凭证: IS_REVERSED
     */
    public final java.lang.Boolean getIsReversed(){
         onPropGet(PROP_ID_isReversed);
         return _isReversed;
    }

    /**
     * 是否红字冲销凭证: IS_REVERSED
     */
    public final void setIsReversed(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isReversed,value)){
            this._isReversed = value;
            internalClearRefs(PROP_ID_isReversed);
            
        }
    }
    
    /**
     * 原冲销凭证ID: REVERSAL_OF_VOUCHER_ID
     */
    public final java.lang.Long getReversalOfVoucherId(){
         onPropGet(PROP_ID_reversalOfVoucherId);
         return _reversalOfVoucherId;
    }

    /**
     * 原冲销凭证ID: REVERSAL_OF_VOUCHER_ID
     */
    public final void setReversalOfVoucherId(java.lang.Long value){
        if(onPropSet(PROP_ID_reversalOfVoucherId,value)){
            this._reversalOfVoucherId = value;
            internalClearRefs(PROP_ID_reversalOfVoucherId);
            
        }
    }
    
    /**
     * 凭证状态: DOC_STATUS
     */
    public final java.lang.Integer getDocStatus(){
         onPropGet(PROP_ID_docStatus);
         return _docStatus;
    }

    /**
     * 凭证状态: DOC_STATUS
     */
    public final void setDocStatus(java.lang.Integer value){
        if(onPropSet(PROP_ID_docStatus,value)){
            this._docStatus = value;
            internalClearRefs(PROP_ID_docStatus);
            
        }
    }
    
    /**
     * 过账人: POSTED_BY
     */
    public final java.lang.Long getPostedBy(){
         onPropGet(PROP_ID_postedBy);
         return _postedBy;
    }

    /**
     * 过账人: POSTED_BY
     */
    public final void setPostedBy(java.lang.Long value){
        if(onPropSet(PROP_ID_postedBy,value)){
            this._postedBy = value;
            internalClearRefs(PROP_ID_postedBy);
            
        }
    }
    
    /**
     * 过账时间: POSTED_AT
     */
    public final java.time.LocalDateTime getPostedAt(){
         onPropGet(PROP_ID_postedAt);
         return _postedAt;
    }

    /**
     * 过账时间: POSTED_AT
     */
    public final void setPostedAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_postedAt,value)){
            this._postedAt = value;
            internalClearRefs(PROP_ID_postedAt);
            
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
       
    private final OrmEntitySet<app.erp.fin.dao.entity.ErpFinVoucherLine> _lines = new OrmEntitySet<>(this, PROP_NAME_lines,
        null, null,app.erp.fin.dao.entity.ErpFinVoucherLine.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.fin.dao.entity.ErpFinVoucherLine> getLines(){
       return _lines;
    }
       
    private final OrmEntitySet<app.erp.fin.dao.entity.ErpFinVoucherBillR> _billLinks = new OrmEntitySet<>(this, PROP_NAME_billLinks,
        null, null,app.erp.fin.dao.entity.ErpFinVoucherBillR.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.fin.dao.entity.ErpFinVoucherBillR> getBillLinks(){
       return _billLinks;
    }
       
}
// resume CPD analysis - CPD-ON
