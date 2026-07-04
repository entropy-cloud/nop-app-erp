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

import app.erp.hr.dao.entity.ErpHrPayrollBankFile;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  银行代发文件: erp_hr_payroll_bank_file
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpHrPayrollBankFile extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 批次号: BATCH_NO VARCHAR */
    public static final String PROP_NAME_batchNo = "batchNo";
    public static final int PROP_ID_batchNo = 2;
    
    /* 发放日期: PAYMENT_DATE DATE */
    public static final String PROP_NAME_paymentDate = "paymentDate";
    public static final int PROP_ID_paymentDate = 3;
    
    /* 总金额: TOTAL_AMOUNT DECIMAL */
    public static final String PROP_NAME_totalAmount = "totalAmount";
    public static final int PROP_ID_totalAmount = 4;
    
    /* 记录数: RECORD_COUNT INTEGER */
    public static final String PROP_NAME_recordCount = "recordCount";
    public static final int PROP_ID_recordCount = 5;
    
    /* 文件格式: FILE_FORMAT VARCHAR */
    public static final String PROP_NAME_fileFormat = "fileFormat";
    public static final int PROP_ID_fileFormat = 6;
    
    /* 文件内容: FILE_CONTENT CLOB */
    public static final String PROP_NAME_fileContent = "fileContent";
    public static final int PROP_ID_fileContent = 7;
    
    /* 状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 8;
    
    /* 开户银行: BANK_ID BIGINT */
    public static final String PROP_NAME_bankId = "bankId";
    public static final int PROP_ID_bankId = 9;
    
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
    public static final String PROP_NAME_bank = "bank";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[18];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_batchNo] = PROP_NAME_batchNo;
          PROP_NAME_TO_ID.put(PROP_NAME_batchNo, PROP_ID_batchNo);
      
          PROP_ID_TO_NAME[PROP_ID_paymentDate] = PROP_NAME_paymentDate;
          PROP_NAME_TO_ID.put(PROP_NAME_paymentDate, PROP_ID_paymentDate);
      
          PROP_ID_TO_NAME[PROP_ID_totalAmount] = PROP_NAME_totalAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_totalAmount, PROP_ID_totalAmount);
      
          PROP_ID_TO_NAME[PROP_ID_recordCount] = PROP_NAME_recordCount;
          PROP_NAME_TO_ID.put(PROP_NAME_recordCount, PROP_ID_recordCount);
      
          PROP_ID_TO_NAME[PROP_ID_fileFormat] = PROP_NAME_fileFormat;
          PROP_NAME_TO_ID.put(PROP_NAME_fileFormat, PROP_ID_fileFormat);
      
          PROP_ID_TO_NAME[PROP_ID_fileContent] = PROP_NAME_fileContent;
          PROP_NAME_TO_ID.put(PROP_NAME_fileContent, PROP_ID_fileContent);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_bankId] = PROP_NAME_bankId;
          PROP_NAME_TO_ID.put(PROP_NAME_bankId, PROP_ID_bankId);
      
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
    
    /* 批次号: BATCH_NO */
    private java.lang.String _batchNo;
    
    /* 发放日期: PAYMENT_DATE */
    private java.time.LocalDate _paymentDate;
    
    /* 总金额: TOTAL_AMOUNT */
    private java.math.BigDecimal _totalAmount;
    
    /* 记录数: RECORD_COUNT */
    private java.lang.Integer _recordCount;
    
    /* 文件格式: FILE_FORMAT */
    private java.lang.String _fileFormat;
    
    /* 文件内容: FILE_CONTENT */
    private java.lang.String _fileContent;
    
    /* 状态: STATUS */
    private java.lang.String _status;
    
    /* 开户银行: BANK_ID */
    private java.lang.Long _bankId;
    
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
    

    public _ErpHrPayrollBankFile(){
        // for debug
    }

    protected ErpHrPayrollBankFile newInstance(){
        ErpHrPayrollBankFile entity = new ErpHrPayrollBankFile();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpHrPayrollBankFile cloneInstance() {
        ErpHrPayrollBankFile entity = newInstance();
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
      return "app.erp.hr.dao.entity.ErpHrPayrollBankFile";
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
        
            case PROP_ID_batchNo:
               return getBatchNo();
        
            case PROP_ID_paymentDate:
               return getPaymentDate();
        
            case PROP_ID_totalAmount:
               return getTotalAmount();
        
            case PROP_ID_recordCount:
               return getRecordCount();
        
            case PROP_ID_fileFormat:
               return getFileFormat();
        
            case PROP_ID_fileContent:
               return getFileContent();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_bankId:
               return getBankId();
        
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
        
            case PROP_ID_batchNo:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_batchNo));
               }
               setBatchNo(typedValue);
               break;
            }
        
            case PROP_ID_paymentDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_paymentDate));
               }
               setPaymentDate(typedValue);
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
        
            case PROP_ID_recordCount:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_recordCount));
               }
               setRecordCount(typedValue);
               break;
            }
        
            case PROP_ID_fileFormat:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_fileFormat));
               }
               setFileFormat(typedValue);
               break;
            }
        
            case PROP_ID_fileContent:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_fileContent));
               }
               setFileContent(typedValue);
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
        
            case PROP_ID_bankId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_bankId));
               }
               setBankId(typedValue);
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
        
            case PROP_ID_batchNo:{
               onInitProp(propId);
               this._batchNo = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_paymentDate:{
               onInitProp(propId);
               this._paymentDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_totalAmount:{
               onInitProp(propId);
               this._totalAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_recordCount:{
               onInitProp(propId);
               this._recordCount = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_fileFormat:{
               onInitProp(propId);
               this._fileFormat = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_fileContent:{
               onInitProp(propId);
               this._fileContent = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_bankId:{
               onInitProp(propId);
               this._bankId = (java.lang.Long)value;
               
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
     * 批次号: BATCH_NO
     */
    public final java.lang.String getBatchNo(){
         onPropGet(PROP_ID_batchNo);
         return _batchNo;
    }

    /**
     * 批次号: BATCH_NO
     */
    public final void setBatchNo(java.lang.String value){
        if(onPropSet(PROP_ID_batchNo,value)){
            this._batchNo = value;
            internalClearRefs(PROP_ID_batchNo);
            
        }
    }
    
    /**
     * 发放日期: PAYMENT_DATE
     */
    public final java.time.LocalDate getPaymentDate(){
         onPropGet(PROP_ID_paymentDate);
         return _paymentDate;
    }

    /**
     * 发放日期: PAYMENT_DATE
     */
    public final void setPaymentDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_paymentDate,value)){
            this._paymentDate = value;
            internalClearRefs(PROP_ID_paymentDate);
            
        }
    }
    
    /**
     * 总金额: TOTAL_AMOUNT
     */
    public final java.math.BigDecimal getTotalAmount(){
         onPropGet(PROP_ID_totalAmount);
         return _totalAmount;
    }

    /**
     * 总金额: TOTAL_AMOUNT
     */
    public final void setTotalAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_totalAmount,value)){
            this._totalAmount = value;
            internalClearRefs(PROP_ID_totalAmount);
            
        }
    }
    
    /**
     * 记录数: RECORD_COUNT
     */
    public final java.lang.Integer getRecordCount(){
         onPropGet(PROP_ID_recordCount);
         return _recordCount;
    }

    /**
     * 记录数: RECORD_COUNT
     */
    public final void setRecordCount(java.lang.Integer value){
        if(onPropSet(PROP_ID_recordCount,value)){
            this._recordCount = value;
            internalClearRefs(PROP_ID_recordCount);
            
        }
    }
    
    /**
     * 文件格式: FILE_FORMAT
     */
    public final java.lang.String getFileFormat(){
         onPropGet(PROP_ID_fileFormat);
         return _fileFormat;
    }

    /**
     * 文件格式: FILE_FORMAT
     */
    public final void setFileFormat(java.lang.String value){
        if(onPropSet(PROP_ID_fileFormat,value)){
            this._fileFormat = value;
            internalClearRefs(PROP_ID_fileFormat);
            
        }
    }
    
    /**
     * 文件内容: FILE_CONTENT
     */
    public final java.lang.String getFileContent(){
         onPropGet(PROP_ID_fileContent);
         return _fileContent;
    }

    /**
     * 文件内容: FILE_CONTENT
     */
    public final void setFileContent(java.lang.String value){
        if(onPropSet(PROP_ID_fileContent,value)){
            this._fileContent = value;
            internalClearRefs(PROP_ID_fileContent);
            
        }
    }
    
    /**
     * 状态: STATUS
     */
    public final java.lang.String getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 状态: STATUS
     */
    public final void setStatus(java.lang.String value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
        }
    }
    
    /**
     * 开户银行: BANK_ID
     */
    public final java.lang.Long getBankId(){
         onPropGet(PROP_ID_bankId);
         return _bankId;
    }

    /**
     * 开户银行: BANK_ID
     */
    public final void setBankId(java.lang.Long value){
        if(onPropSet(PROP_ID_bankId,value)){
            this._bankId = value;
            internalClearRefs(PROP_ID_bankId);
            
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
    public final app.erp.md.dao.entity.ErpMdBankAccount getBank(){
       return (app.erp.md.dao.entity.ErpMdBankAccount)internalGetRefEntity(PROP_NAME_bank);
    }

    public final void setBank(app.erp.md.dao.entity.ErpMdBankAccount refEntity){
   
           if(refEntity == null){
           
                   this.setBankId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_bank, refEntity,()->{
           
                           this.setBankId(refEntity.getId());
                       
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
