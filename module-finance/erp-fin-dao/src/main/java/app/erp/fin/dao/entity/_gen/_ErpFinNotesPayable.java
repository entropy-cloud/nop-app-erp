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

import app.erp.fin.dao.entity.ErpFinNotesPayable;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  应付票据: erp_fin_notes_payable
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpFinNotesPayable extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 单号: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 核算组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 票据类型: NOTES_TYPE VARCHAR */
    public static final String PROP_NAME_notesType = "notesType";
    public static final int PROP_ID_notesType = 4;
    
    /* 票据号: NOTES_NO VARCHAR */
    public static final String PROP_NAME_notesNo = "notesNo";
    public static final int PROP_ID_notesNo = 5;
    
    /* 收款人: PAYEE_NAME VARCHAR */
    public static final String PROP_NAME_payeeName = "payeeName";
    public static final int PROP_ID_payeeName = 6;
    
    /* 收款行: PAYEE_BANK VARCHAR */
    public static final String PROP_NAME_payeeBank = "payeeBank";
    public static final int PROP_ID_payeeBank = 7;
    
    /* 出票日: ISSUE_DATE DATE */
    public static final String PROP_NAME_issueDate = "issueDate";
    public static final int PROP_ID_issueDate = 8;
    
    /* 到期日: DUE_DATE DATE */
    public static final String PROP_NAME_dueDate = "dueDate";
    public static final int PROP_ID_dueDate = 9;
    
    /* 币种: CURRENCY_ID BIGINT */
    public static final String PROP_NAME_currencyId = "currencyId";
    public static final int PROP_ID_currencyId = 10;
    
    /* 汇率: EXCHANGE_RATE DECIMAL */
    public static final String PROP_NAME_exchangeRate = "exchangeRate";
    public static final int PROP_ID_exchangeRate = 11;
    
    /* 源币种票面金额: AMOUNT_SOURCE DECIMAL */
    public static final String PROP_NAME_amountSource = "amountSource";
    public static final int PROP_ID_amountSource = 12;
    
    /* 本位币票面金额: AMOUNT_FUNCTIONAL DECIMAL */
    public static final String PROP_NAME_amountFunctional = "amountFunctional";
    public static final int PROP_ID_amountFunctional = 13;
    
    /* 收款方往来: PARTNER_ID BIGINT */
    public static final String PROP_NAME_partnerId = "partnerId";
    public static final int PROP_ID_partnerId = 14;
    
    /* 占用授信额度: CREDIT_FACILITY_ID BIGINT */
    public static final String PROP_NAME_creditFacilityId = "creditFacilityId";
    public static final int PROP_ID_creditFacilityId = 15;
    
    /* 来源单据类型: SOURCE_BILL_TYPE VARCHAR */
    public static final String PROP_NAME_sourceBillType = "sourceBillType";
    public static final int PROP_ID_sourceBillType = 16;
    
    /* 来源单据号: SOURCE_BILL_CODE VARCHAR */
    public static final String PROP_NAME_sourceBillCode = "sourceBillCode";
    public static final int PROP_ID_sourceBillCode = 17;
    
    /* 票据状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 18;
    
    /* 是否已过账: POSTED BOOLEAN */
    public static final String PROP_NAME_posted = "posted";
    public static final int PROP_ID_posted = 19;
    
    /* 过账人: POSTED_BY VARCHAR */
    public static final String PROP_NAME_postedBy = "postedBy";
    public static final int PROP_ID_postedBy = 20;
    
    /* 过账时间: POSTED_AT DATETIME */
    public static final String PROP_NAME_postedAt = "postedAt";
    public static final int PROP_ID_postedAt = 21;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 22;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 23;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 24;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 25;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 26;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 27;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 28;
    

    private static int _PROP_ID_BOUND = 29;

    
    /* relation:  */
    public static final String PROP_NAME_partner = "partner";
    
    /* relation:  */
    public static final String PROP_NAME_currency = "currency";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    
    /* relation:  */
    public static final String PROP_NAME_creditFacility = "creditFacility";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[29];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_notesType] = PROP_NAME_notesType;
          PROP_NAME_TO_ID.put(PROP_NAME_notesType, PROP_ID_notesType);
      
          PROP_ID_TO_NAME[PROP_ID_notesNo] = PROP_NAME_notesNo;
          PROP_NAME_TO_ID.put(PROP_NAME_notesNo, PROP_ID_notesNo);
      
          PROP_ID_TO_NAME[PROP_ID_payeeName] = PROP_NAME_payeeName;
          PROP_NAME_TO_ID.put(PROP_NAME_payeeName, PROP_ID_payeeName);
      
          PROP_ID_TO_NAME[PROP_ID_payeeBank] = PROP_NAME_payeeBank;
          PROP_NAME_TO_ID.put(PROP_NAME_payeeBank, PROP_ID_payeeBank);
      
          PROP_ID_TO_NAME[PROP_ID_issueDate] = PROP_NAME_issueDate;
          PROP_NAME_TO_ID.put(PROP_NAME_issueDate, PROP_ID_issueDate);
      
          PROP_ID_TO_NAME[PROP_ID_dueDate] = PROP_NAME_dueDate;
          PROP_NAME_TO_ID.put(PROP_NAME_dueDate, PROP_ID_dueDate);
      
          PROP_ID_TO_NAME[PROP_ID_currencyId] = PROP_NAME_currencyId;
          PROP_NAME_TO_ID.put(PROP_NAME_currencyId, PROP_ID_currencyId);
      
          PROP_ID_TO_NAME[PROP_ID_exchangeRate] = PROP_NAME_exchangeRate;
          PROP_NAME_TO_ID.put(PROP_NAME_exchangeRate, PROP_ID_exchangeRate);
      
          PROP_ID_TO_NAME[PROP_ID_amountSource] = PROP_NAME_amountSource;
          PROP_NAME_TO_ID.put(PROP_NAME_amountSource, PROP_ID_amountSource);
      
          PROP_ID_TO_NAME[PROP_ID_amountFunctional] = PROP_NAME_amountFunctional;
          PROP_NAME_TO_ID.put(PROP_NAME_amountFunctional, PROP_ID_amountFunctional);
      
          PROP_ID_TO_NAME[PROP_ID_partnerId] = PROP_NAME_partnerId;
          PROP_NAME_TO_ID.put(PROP_NAME_partnerId, PROP_ID_partnerId);
      
          PROP_ID_TO_NAME[PROP_ID_creditFacilityId] = PROP_NAME_creditFacilityId;
          PROP_NAME_TO_ID.put(PROP_NAME_creditFacilityId, PROP_ID_creditFacilityId);
      
          PROP_ID_TO_NAME[PROP_ID_sourceBillType] = PROP_NAME_sourceBillType;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceBillType, PROP_ID_sourceBillType);
      
          PROP_ID_TO_NAME[PROP_ID_sourceBillCode] = PROP_NAME_sourceBillCode;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceBillCode, PROP_ID_sourceBillCode);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_posted] = PROP_NAME_posted;
          PROP_NAME_TO_ID.put(PROP_NAME_posted, PROP_ID_posted);
      
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
    
    /* 单号: CODE */
    private java.lang.String _code;
    
    /* 核算组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 票据类型: NOTES_TYPE */
    private java.lang.String _notesType;
    
    /* 票据号: NOTES_NO */
    private java.lang.String _notesNo;
    
    /* 收款人: PAYEE_NAME */
    private java.lang.String _payeeName;
    
    /* 收款行: PAYEE_BANK */
    private java.lang.String _payeeBank;
    
    /* 出票日: ISSUE_DATE */
    private java.time.LocalDate _issueDate;
    
    /* 到期日: DUE_DATE */
    private java.time.LocalDate _dueDate;
    
    /* 币种: CURRENCY_ID */
    private java.lang.Long _currencyId;
    
    /* 汇率: EXCHANGE_RATE */
    private java.math.BigDecimal _exchangeRate;
    
    /* 源币种票面金额: AMOUNT_SOURCE */
    private java.math.BigDecimal _amountSource;
    
    /* 本位币票面金额: AMOUNT_FUNCTIONAL */
    private java.math.BigDecimal _amountFunctional;
    
    /* 收款方往来: PARTNER_ID */
    private java.lang.Long _partnerId;
    
    /* 占用授信额度: CREDIT_FACILITY_ID */
    private java.lang.Long _creditFacilityId;
    
    /* 来源单据类型: SOURCE_BILL_TYPE */
    private java.lang.String _sourceBillType;
    
    /* 来源单据号: SOURCE_BILL_CODE */
    private java.lang.String _sourceBillCode;
    
    /* 票据状态: STATUS */
    private java.lang.String _status;
    
    /* 是否已过账: POSTED */
    private java.lang.Boolean _posted;
    
    /* 过账人: POSTED_BY */
    private java.lang.String _postedBy;
    
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
    

    public _ErpFinNotesPayable(){
        // for debug
    }

    protected ErpFinNotesPayable newInstance(){
        ErpFinNotesPayable entity = new ErpFinNotesPayable();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpFinNotesPayable cloneInstance() {
        ErpFinNotesPayable entity = newInstance();
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
      return "app.erp.fin.dao.entity.ErpFinNotesPayable";
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
        
            case PROP_ID_notesType:
               return getNotesType();
        
            case PROP_ID_notesNo:
               return getNotesNo();
        
            case PROP_ID_payeeName:
               return getPayeeName();
        
            case PROP_ID_payeeBank:
               return getPayeeBank();
        
            case PROP_ID_issueDate:
               return getIssueDate();
        
            case PROP_ID_dueDate:
               return getDueDate();
        
            case PROP_ID_currencyId:
               return getCurrencyId();
        
            case PROP_ID_exchangeRate:
               return getExchangeRate();
        
            case PROP_ID_amountSource:
               return getAmountSource();
        
            case PROP_ID_amountFunctional:
               return getAmountFunctional();
        
            case PROP_ID_partnerId:
               return getPartnerId();
        
            case PROP_ID_creditFacilityId:
               return getCreditFacilityId();
        
            case PROP_ID_sourceBillType:
               return getSourceBillType();
        
            case PROP_ID_sourceBillCode:
               return getSourceBillCode();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_posted:
               return getPosted();
        
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
        
            case PROP_ID_orgId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_orgId));
               }
               setOrgId(typedValue);
               break;
            }
        
            case PROP_ID_notesType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_notesType));
               }
               setNotesType(typedValue);
               break;
            }
        
            case PROP_ID_notesNo:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_notesNo));
               }
               setNotesNo(typedValue);
               break;
            }
        
            case PROP_ID_payeeName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_payeeName));
               }
               setPayeeName(typedValue);
               break;
            }
        
            case PROP_ID_payeeBank:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_payeeBank));
               }
               setPayeeBank(typedValue);
               break;
            }
        
            case PROP_ID_issueDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_issueDate));
               }
               setIssueDate(typedValue);
               break;
            }
        
            case PROP_ID_dueDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_dueDate));
               }
               setDueDate(typedValue);
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
        
            case PROP_ID_exchangeRate:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_exchangeRate));
               }
               setExchangeRate(typedValue);
               break;
            }
        
            case PROP_ID_amountSource:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_amountSource));
               }
               setAmountSource(typedValue);
               break;
            }
        
            case PROP_ID_amountFunctional:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_amountFunctional));
               }
               setAmountFunctional(typedValue);
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
        
            case PROP_ID_creditFacilityId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_creditFacilityId));
               }
               setCreditFacilityId(typedValue);
               break;
            }
        
            case PROP_ID_sourceBillType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_sourceBillType));
               }
               setSourceBillType(typedValue);
               break;
            }
        
            case PROP_ID_sourceBillCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_sourceBillCode));
               }
               setSourceBillCode(typedValue);
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
        
            case PROP_ID_posted:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_posted));
               }
               setPosted(typedValue);
               break;
            }
        
            case PROP_ID_postedBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
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
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_notesType:{
               onInitProp(propId);
               this._notesType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_notesNo:{
               onInitProp(propId);
               this._notesNo = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_payeeName:{
               onInitProp(propId);
               this._payeeName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_payeeBank:{
               onInitProp(propId);
               this._payeeBank = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_issueDate:{
               onInitProp(propId);
               this._issueDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_dueDate:{
               onInitProp(propId);
               this._dueDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_currencyId:{
               onInitProp(propId);
               this._currencyId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_exchangeRate:{
               onInitProp(propId);
               this._exchangeRate = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_amountSource:{
               onInitProp(propId);
               this._amountSource = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_amountFunctional:{
               onInitProp(propId);
               this._amountFunctional = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_partnerId:{
               onInitProp(propId);
               this._partnerId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_creditFacilityId:{
               onInitProp(propId);
               this._creditFacilityId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_sourceBillType:{
               onInitProp(propId);
               this._sourceBillType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_sourceBillCode:{
               onInitProp(propId);
               this._sourceBillCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_posted:{
               onInitProp(propId);
               this._posted = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_postedBy:{
               onInitProp(propId);
               this._postedBy = (java.lang.String)value;
               
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
     * 票据类型: NOTES_TYPE
     */
    public final java.lang.String getNotesType(){
         onPropGet(PROP_ID_notesType);
         return _notesType;
    }

    /**
     * 票据类型: NOTES_TYPE
     */
    public final void setNotesType(java.lang.String value){
        if(onPropSet(PROP_ID_notesType,value)){
            this._notesType = value;
            internalClearRefs(PROP_ID_notesType);
            
        }
    }
    
    /**
     * 票据号: NOTES_NO
     */
    public final java.lang.String getNotesNo(){
         onPropGet(PROP_ID_notesNo);
         return _notesNo;
    }

    /**
     * 票据号: NOTES_NO
     */
    public final void setNotesNo(java.lang.String value){
        if(onPropSet(PROP_ID_notesNo,value)){
            this._notesNo = value;
            internalClearRefs(PROP_ID_notesNo);
            
        }
    }
    
    /**
     * 收款人: PAYEE_NAME
     */
    public final java.lang.String getPayeeName(){
         onPropGet(PROP_ID_payeeName);
         return _payeeName;
    }

    /**
     * 收款人: PAYEE_NAME
     */
    public final void setPayeeName(java.lang.String value){
        if(onPropSet(PROP_ID_payeeName,value)){
            this._payeeName = value;
            internalClearRefs(PROP_ID_payeeName);
            
        }
    }
    
    /**
     * 收款行: PAYEE_BANK
     */
    public final java.lang.String getPayeeBank(){
         onPropGet(PROP_ID_payeeBank);
         return _payeeBank;
    }

    /**
     * 收款行: PAYEE_BANK
     */
    public final void setPayeeBank(java.lang.String value){
        if(onPropSet(PROP_ID_payeeBank,value)){
            this._payeeBank = value;
            internalClearRefs(PROP_ID_payeeBank);
            
        }
    }
    
    /**
     * 出票日: ISSUE_DATE
     */
    public final java.time.LocalDate getIssueDate(){
         onPropGet(PROP_ID_issueDate);
         return _issueDate;
    }

    /**
     * 出票日: ISSUE_DATE
     */
    public final void setIssueDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_issueDate,value)){
            this._issueDate = value;
            internalClearRefs(PROP_ID_issueDate);
            
        }
    }
    
    /**
     * 到期日: DUE_DATE
     */
    public final java.time.LocalDate getDueDate(){
         onPropGet(PROP_ID_dueDate);
         return _dueDate;
    }

    /**
     * 到期日: DUE_DATE
     */
    public final void setDueDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_dueDate,value)){
            this._dueDate = value;
            internalClearRefs(PROP_ID_dueDate);
            
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
     * 汇率: EXCHANGE_RATE
     */
    public final java.math.BigDecimal getExchangeRate(){
         onPropGet(PROP_ID_exchangeRate);
         return _exchangeRate;
    }

    /**
     * 汇率: EXCHANGE_RATE
     */
    public final void setExchangeRate(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_exchangeRate,value)){
            this._exchangeRate = value;
            internalClearRefs(PROP_ID_exchangeRate);
            
        }
    }
    
    /**
     * 源币种票面金额: AMOUNT_SOURCE
     */
    public final java.math.BigDecimal getAmountSource(){
         onPropGet(PROP_ID_amountSource);
         return _amountSource;
    }

    /**
     * 源币种票面金额: AMOUNT_SOURCE
     */
    public final void setAmountSource(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_amountSource,value)){
            this._amountSource = value;
            internalClearRefs(PROP_ID_amountSource);
            
        }
    }
    
    /**
     * 本位币票面金额: AMOUNT_FUNCTIONAL
     */
    public final java.math.BigDecimal getAmountFunctional(){
         onPropGet(PROP_ID_amountFunctional);
         return _amountFunctional;
    }

    /**
     * 本位币票面金额: AMOUNT_FUNCTIONAL
     */
    public final void setAmountFunctional(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_amountFunctional,value)){
            this._amountFunctional = value;
            internalClearRefs(PROP_ID_amountFunctional);
            
        }
    }
    
    /**
     * 收款方往来: PARTNER_ID
     */
    public final java.lang.Long getPartnerId(){
         onPropGet(PROP_ID_partnerId);
         return _partnerId;
    }

    /**
     * 收款方往来: PARTNER_ID
     */
    public final void setPartnerId(java.lang.Long value){
        if(onPropSet(PROP_ID_partnerId,value)){
            this._partnerId = value;
            internalClearRefs(PROP_ID_partnerId);
            
        }
    }
    
    /**
     * 占用授信额度: CREDIT_FACILITY_ID
     */
    public final java.lang.Long getCreditFacilityId(){
         onPropGet(PROP_ID_creditFacilityId);
         return _creditFacilityId;
    }

    /**
     * 占用授信额度: CREDIT_FACILITY_ID
     */
    public final void setCreditFacilityId(java.lang.Long value){
        if(onPropSet(PROP_ID_creditFacilityId,value)){
            this._creditFacilityId = value;
            internalClearRefs(PROP_ID_creditFacilityId);
            
        }
    }
    
    /**
     * 来源单据类型: SOURCE_BILL_TYPE
     */
    public final java.lang.String getSourceBillType(){
         onPropGet(PROP_ID_sourceBillType);
         return _sourceBillType;
    }

    /**
     * 来源单据类型: SOURCE_BILL_TYPE
     */
    public final void setSourceBillType(java.lang.String value){
        if(onPropSet(PROP_ID_sourceBillType,value)){
            this._sourceBillType = value;
            internalClearRefs(PROP_ID_sourceBillType);
            
        }
    }
    
    /**
     * 来源单据号: SOURCE_BILL_CODE
     */
    public final java.lang.String getSourceBillCode(){
         onPropGet(PROP_ID_sourceBillCode);
         return _sourceBillCode;
    }

    /**
     * 来源单据号: SOURCE_BILL_CODE
     */
    public final void setSourceBillCode(java.lang.String value){
        if(onPropSet(PROP_ID_sourceBillCode,value)){
            this._sourceBillCode = value;
            internalClearRefs(PROP_ID_sourceBillCode);
            
        }
    }
    
    /**
     * 票据状态: STATUS
     */
    public final java.lang.String getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 票据状态: STATUS
     */
    public final void setStatus(java.lang.String value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
        }
    }
    
    /**
     * 是否已过账: POSTED
     */
    public final java.lang.Boolean getPosted(){
         onPropGet(PROP_ID_posted);
         return _posted;
    }

    /**
     * 是否已过账: POSTED
     */
    public final void setPosted(java.lang.Boolean value){
        if(onPropSet(PROP_ID_posted,value)){
            this._posted = value;
            internalClearRefs(PROP_ID_posted);
            
        }
    }
    
    /**
     * 过账人: POSTED_BY
     */
    public final java.lang.String getPostedBy(){
         onPropGet(PROP_ID_postedBy);
         return _postedBy;
    }

    /**
     * 过账人: POSTED_BY
     */
    public final void setPostedBy(java.lang.String value){
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
    public final app.erp.md.dao.entity.ErpMdPartner getPartner(){
       return (app.erp.md.dao.entity.ErpMdPartner)internalGetRefEntity(PROP_NAME_partner);
    }

    public final void setPartner(app.erp.md.dao.entity.ErpMdPartner refEntity){
   
           if(refEntity == null){
           
                   this.setPartnerId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_partner, refEntity,()->{
           
                           this.setPartnerId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdCurrency getCurrency(){
       return (app.erp.md.dao.entity.ErpMdCurrency)internalGetRefEntity(PROP_NAME_currency);
    }

    public final void setCurrency(app.erp.md.dao.entity.ErpMdCurrency refEntity){
   
           if(refEntity == null){
           
                   this.setCurrencyId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_currency, refEntity,()->{
           
                           this.setCurrencyId(refEntity.getId());
                       
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
       
    /**
     * 
     */
    public final app.erp.fin.dao.entity.ErpFinCreditFacility getCreditFacility(){
       return (app.erp.fin.dao.entity.ErpFinCreditFacility)internalGetRefEntity(PROP_NAME_creditFacility);
    }

    public final void setCreditFacility(app.erp.fin.dao.entity.ErpFinCreditFacility refEntity){
   
           if(refEntity == null){
           
                   this.setCreditFacilityId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_creditFacility, refEntity,()->{
           
                           this.setCreditFacilityId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
