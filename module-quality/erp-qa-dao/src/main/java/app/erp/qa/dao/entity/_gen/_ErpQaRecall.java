package app.erp.qa.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.qa.dao.entity.ErpQaRecall;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  召回事件: erp_qa_recall
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpQaRecall extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 单号: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 召回名称: RECALL_NAME VARCHAR */
    public static final String PROP_NAME_recallName = "recallName";
    public static final int PROP_ID_recallName = 3;
    
    /* 触发类型: TRIGGER_TYPE VARCHAR */
    public static final String PROP_NAME_triggerType = "triggerType";
    public static final int PROP_ID_triggerType = 4;
    
    /* 来源NCR(弱指针): SOURCE_NCR_ID BIGINT */
    public static final String PROP_NAME_sourceNcrId = "sourceNcrId";
    public static final int PROP_ID_sourceNcrId = 5;
    
    /* 召回物料: MATERIAL_ID BIGINT */
    public static final String PROP_NAME_materialId = "materialId";
    public static final int PROP_ID_materialId = 6;
    
    /* 召回批次(弱指针→ErpInvBatch): BATCH_ID BIGINT */
    public static final String PROP_NAME_batchId = "batchId";
    public static final int PROP_ID_batchId = 7;
    
    /* 召回序列号: SERIAL_NO VARCHAR */
    public static final String PROP_NAME_serialNo = "serialNo";
    public static final int PROP_ID_serialNo = 8;
    
    /* 根本原因: ROOT_CAUSE VARCHAR */
    public static final String PROP_NAME_rootCause = "rootCause";
    public static final int PROP_ID_rootCause = 9;
    
    /* 严重程度: SEVERITY_LEVEL VARCHAR */
    public static final String PROP_NAME_severityLevel = "severityLevel";
    public static final int PROP_ID_severityLevel = 10;
    
    /* 业务日期: BUSINESS_DATE DATE */
    public static final String PROP_NAME_businessDate = "businessDate";
    public static final int PROP_ID_businessDate = 11;
    
    /* 已通知客户: NOTIFY_CUSTOMER BOOLEAN */
    public static final String PROP_NAME_notifyCustomer = "notifyCustomer";
    public static final int PROP_ID_notifyCustomer = 12;
    
    /* 召回状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 13;
    
    /* 审核状态: APPROVE_STATUS VARCHAR */
    public static final String PROP_NAME_approveStatus = "approveStatus";
    public static final int PROP_ID_approveStatus = 14;
    
    /* 审批人: APPROVED_BY VARCHAR */
    public static final String PROP_NAME_approvedBy = "approvedBy";
    public static final int PROP_ID_approvedBy = 15;
    
    /* 审批时间: APPROVED_AT DATETIME */
    public static final String PROP_NAME_approvedAt = "approvedAt";
    public static final int PROP_ID_approvedAt = 16;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 17;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 18;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 19;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 20;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 21;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 22;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 23;
    

    private static int _PROP_ID_BOUND = 24;

    
    /* relation:  */
    public static final String PROP_NAME_sourceNcr = "sourceNcr";
    
    /* relation:  */
    public static final String PROP_NAME_material = "material";
    
    /* relation:  */
    public static final String PROP_NAME_targets = "targets";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[24];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_recallName] = PROP_NAME_recallName;
          PROP_NAME_TO_ID.put(PROP_NAME_recallName, PROP_ID_recallName);
      
          PROP_ID_TO_NAME[PROP_ID_triggerType] = PROP_NAME_triggerType;
          PROP_NAME_TO_ID.put(PROP_NAME_triggerType, PROP_ID_triggerType);
      
          PROP_ID_TO_NAME[PROP_ID_sourceNcrId] = PROP_NAME_sourceNcrId;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceNcrId, PROP_ID_sourceNcrId);
      
          PROP_ID_TO_NAME[PROP_ID_materialId] = PROP_NAME_materialId;
          PROP_NAME_TO_ID.put(PROP_NAME_materialId, PROP_ID_materialId);
      
          PROP_ID_TO_NAME[PROP_ID_batchId] = PROP_NAME_batchId;
          PROP_NAME_TO_ID.put(PROP_NAME_batchId, PROP_ID_batchId);
      
          PROP_ID_TO_NAME[PROP_ID_serialNo] = PROP_NAME_serialNo;
          PROP_NAME_TO_ID.put(PROP_NAME_serialNo, PROP_ID_serialNo);
      
          PROP_ID_TO_NAME[PROP_ID_rootCause] = PROP_NAME_rootCause;
          PROP_NAME_TO_ID.put(PROP_NAME_rootCause, PROP_ID_rootCause);
      
          PROP_ID_TO_NAME[PROP_ID_severityLevel] = PROP_NAME_severityLevel;
          PROP_NAME_TO_ID.put(PROP_NAME_severityLevel, PROP_ID_severityLevel);
      
          PROP_ID_TO_NAME[PROP_ID_businessDate] = PROP_NAME_businessDate;
          PROP_NAME_TO_ID.put(PROP_NAME_businessDate, PROP_ID_businessDate);
      
          PROP_ID_TO_NAME[PROP_ID_notifyCustomer] = PROP_NAME_notifyCustomer;
          PROP_NAME_TO_ID.put(PROP_NAME_notifyCustomer, PROP_ID_notifyCustomer);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_approveStatus] = PROP_NAME_approveStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_approveStatus, PROP_ID_approveStatus);
      
          PROP_ID_TO_NAME[PROP_ID_approvedBy] = PROP_NAME_approvedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_approvedBy, PROP_ID_approvedBy);
      
          PROP_ID_TO_NAME[PROP_ID_approvedAt] = PROP_NAME_approvedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_approvedAt, PROP_ID_approvedAt);
      
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
    
    /* 召回名称: RECALL_NAME */
    private java.lang.String _recallName;
    
    /* 触发类型: TRIGGER_TYPE */
    private java.lang.String _triggerType;
    
    /* 来源NCR(弱指针): SOURCE_NCR_ID */
    private java.lang.Long _sourceNcrId;
    
    /* 召回物料: MATERIAL_ID */
    private java.lang.Long _materialId;
    
    /* 召回批次(弱指针→ErpInvBatch): BATCH_ID */
    private java.lang.Long _batchId;
    
    /* 召回序列号: SERIAL_NO */
    private java.lang.String _serialNo;
    
    /* 根本原因: ROOT_CAUSE */
    private java.lang.String _rootCause;
    
    /* 严重程度: SEVERITY_LEVEL */
    private java.lang.String _severityLevel;
    
    /* 业务日期: BUSINESS_DATE */
    private java.time.LocalDate _businessDate;
    
    /* 已通知客户: NOTIFY_CUSTOMER */
    private java.lang.Boolean _notifyCustomer;
    
    /* 召回状态: STATUS */
    private java.lang.String _status;
    
    /* 审核状态: APPROVE_STATUS */
    private java.lang.String _approveStatus;
    
    /* 审批人: APPROVED_BY */
    private java.lang.String _approvedBy;
    
    /* 审批时间: APPROVED_AT */
    private java.time.LocalDateTime _approvedAt;
    
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
    

    public _ErpQaRecall(){
        // for debug
    }

    protected ErpQaRecall newInstance(){
        ErpQaRecall entity = new ErpQaRecall();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpQaRecall cloneInstance() {
        ErpQaRecall entity = newInstance();
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
      return "app.erp.qa.dao.entity.ErpQaRecall";
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
        
            case PROP_ID_recallName:
               return getRecallName();
        
            case PROP_ID_triggerType:
               return getTriggerType();
        
            case PROP_ID_sourceNcrId:
               return getSourceNcrId();
        
            case PROP_ID_materialId:
               return getMaterialId();
        
            case PROP_ID_batchId:
               return getBatchId();
        
            case PROP_ID_serialNo:
               return getSerialNo();
        
            case PROP_ID_rootCause:
               return getRootCause();
        
            case PROP_ID_severityLevel:
               return getSeverityLevel();
        
            case PROP_ID_businessDate:
               return getBusinessDate();
        
            case PROP_ID_notifyCustomer:
               return getNotifyCustomer();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_approveStatus:
               return getApproveStatus();
        
            case PROP_ID_approvedBy:
               return getApprovedBy();
        
            case PROP_ID_approvedAt:
               return getApprovedAt();
        
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
        
            case PROP_ID_recallName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_recallName));
               }
               setRecallName(typedValue);
               break;
            }
        
            case PROP_ID_triggerType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_triggerType));
               }
               setTriggerType(typedValue);
               break;
            }
        
            case PROP_ID_sourceNcrId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_sourceNcrId));
               }
               setSourceNcrId(typedValue);
               break;
            }
        
            case PROP_ID_materialId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_materialId));
               }
               setMaterialId(typedValue);
               break;
            }
        
            case PROP_ID_batchId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_batchId));
               }
               setBatchId(typedValue);
               break;
            }
        
            case PROP_ID_serialNo:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_serialNo));
               }
               setSerialNo(typedValue);
               break;
            }
        
            case PROP_ID_rootCause:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_rootCause));
               }
               setRootCause(typedValue);
               break;
            }
        
            case PROP_ID_severityLevel:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_severityLevel));
               }
               setSeverityLevel(typedValue);
               break;
            }
        
            case PROP_ID_businessDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_businessDate));
               }
               setBusinessDate(typedValue);
               break;
            }
        
            case PROP_ID_notifyCustomer:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_notifyCustomer));
               }
               setNotifyCustomer(typedValue);
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
        
            case PROP_ID_approveStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_approveStatus));
               }
               setApproveStatus(typedValue);
               break;
            }
        
            case PROP_ID_approvedBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_approvedBy));
               }
               setApprovedBy(typedValue);
               break;
            }
        
            case PROP_ID_approvedAt:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_approvedAt));
               }
               setApprovedAt(typedValue);
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
        
            case PROP_ID_recallName:{
               onInitProp(propId);
               this._recallName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_triggerType:{
               onInitProp(propId);
               this._triggerType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_sourceNcrId:{
               onInitProp(propId);
               this._sourceNcrId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_materialId:{
               onInitProp(propId);
               this._materialId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_batchId:{
               onInitProp(propId);
               this._batchId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_serialNo:{
               onInitProp(propId);
               this._serialNo = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_rootCause:{
               onInitProp(propId);
               this._rootCause = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_severityLevel:{
               onInitProp(propId);
               this._severityLevel = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_businessDate:{
               onInitProp(propId);
               this._businessDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_notifyCustomer:{
               onInitProp(propId);
               this._notifyCustomer = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_approveStatus:{
               onInitProp(propId);
               this._approveStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_approvedBy:{
               onInitProp(propId);
               this._approvedBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_approvedAt:{
               onInitProp(propId);
               this._approvedAt = (java.time.LocalDateTime)value;
               
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
     * 召回名称: RECALL_NAME
     */
    public final java.lang.String getRecallName(){
         onPropGet(PROP_ID_recallName);
         return _recallName;
    }

    /**
     * 召回名称: RECALL_NAME
     */
    public final void setRecallName(java.lang.String value){
        if(onPropSet(PROP_ID_recallName,value)){
            this._recallName = value;
            internalClearRefs(PROP_ID_recallName);
            
        }
    }
    
    /**
     * 触发类型: TRIGGER_TYPE
     */
    public final java.lang.String getTriggerType(){
         onPropGet(PROP_ID_triggerType);
         return _triggerType;
    }

    /**
     * 触发类型: TRIGGER_TYPE
     */
    public final void setTriggerType(java.lang.String value){
        if(onPropSet(PROP_ID_triggerType,value)){
            this._triggerType = value;
            internalClearRefs(PROP_ID_triggerType);
            
        }
    }
    
    /**
     * 来源NCR(弱指针): SOURCE_NCR_ID
     */
    public final java.lang.Long getSourceNcrId(){
         onPropGet(PROP_ID_sourceNcrId);
         return _sourceNcrId;
    }

    /**
     * 来源NCR(弱指针): SOURCE_NCR_ID
     */
    public final void setSourceNcrId(java.lang.Long value){
        if(onPropSet(PROP_ID_sourceNcrId,value)){
            this._sourceNcrId = value;
            internalClearRefs(PROP_ID_sourceNcrId);
            
        }
    }
    
    /**
     * 召回物料: MATERIAL_ID
     */
    public final java.lang.Long getMaterialId(){
         onPropGet(PROP_ID_materialId);
         return _materialId;
    }

    /**
     * 召回物料: MATERIAL_ID
     */
    public final void setMaterialId(java.lang.Long value){
        if(onPropSet(PROP_ID_materialId,value)){
            this._materialId = value;
            internalClearRefs(PROP_ID_materialId);
            
        }
    }
    
    /**
     * 召回批次(弱指针→ErpInvBatch): BATCH_ID
     */
    public final java.lang.Long getBatchId(){
         onPropGet(PROP_ID_batchId);
         return _batchId;
    }

    /**
     * 召回批次(弱指针→ErpInvBatch): BATCH_ID
     */
    public final void setBatchId(java.lang.Long value){
        if(onPropSet(PROP_ID_batchId,value)){
            this._batchId = value;
            internalClearRefs(PROP_ID_batchId);
            
        }
    }
    
    /**
     * 召回序列号: SERIAL_NO
     */
    public final java.lang.String getSerialNo(){
         onPropGet(PROP_ID_serialNo);
         return _serialNo;
    }

    /**
     * 召回序列号: SERIAL_NO
     */
    public final void setSerialNo(java.lang.String value){
        if(onPropSet(PROP_ID_serialNo,value)){
            this._serialNo = value;
            internalClearRefs(PROP_ID_serialNo);
            
        }
    }
    
    /**
     * 根本原因: ROOT_CAUSE
     */
    public final java.lang.String getRootCause(){
         onPropGet(PROP_ID_rootCause);
         return _rootCause;
    }

    /**
     * 根本原因: ROOT_CAUSE
     */
    public final void setRootCause(java.lang.String value){
        if(onPropSet(PROP_ID_rootCause,value)){
            this._rootCause = value;
            internalClearRefs(PROP_ID_rootCause);
            
        }
    }
    
    /**
     * 严重程度: SEVERITY_LEVEL
     */
    public final java.lang.String getSeverityLevel(){
         onPropGet(PROP_ID_severityLevel);
         return _severityLevel;
    }

    /**
     * 严重程度: SEVERITY_LEVEL
     */
    public final void setSeverityLevel(java.lang.String value){
        if(onPropSet(PROP_ID_severityLevel,value)){
            this._severityLevel = value;
            internalClearRefs(PROP_ID_severityLevel);
            
        }
    }
    
    /**
     * 业务日期: BUSINESS_DATE
     */
    public final java.time.LocalDate getBusinessDate(){
         onPropGet(PROP_ID_businessDate);
         return _businessDate;
    }

    /**
     * 业务日期: BUSINESS_DATE
     */
    public final void setBusinessDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_businessDate,value)){
            this._businessDate = value;
            internalClearRefs(PROP_ID_businessDate);
            
        }
    }
    
    /**
     * 已通知客户: NOTIFY_CUSTOMER
     */
    public final java.lang.Boolean getNotifyCustomer(){
         onPropGet(PROP_ID_notifyCustomer);
         return _notifyCustomer;
    }

    /**
     * 已通知客户: NOTIFY_CUSTOMER
     */
    public final void setNotifyCustomer(java.lang.Boolean value){
        if(onPropSet(PROP_ID_notifyCustomer,value)){
            this._notifyCustomer = value;
            internalClearRefs(PROP_ID_notifyCustomer);
            
        }
    }
    
    /**
     * 召回状态: STATUS
     */
    public final java.lang.String getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 召回状态: STATUS
     */
    public final void setStatus(java.lang.String value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
        }
    }
    
    /**
     * 审核状态: APPROVE_STATUS
     */
    public final java.lang.String getApproveStatus(){
         onPropGet(PROP_ID_approveStatus);
         return _approveStatus;
    }

    /**
     * 审核状态: APPROVE_STATUS
     */
    public final void setApproveStatus(java.lang.String value){
        if(onPropSet(PROP_ID_approveStatus,value)){
            this._approveStatus = value;
            internalClearRefs(PROP_ID_approveStatus);
            
        }
    }
    
    /**
     * 审批人: APPROVED_BY
     */
    public final java.lang.String getApprovedBy(){
         onPropGet(PROP_ID_approvedBy);
         return _approvedBy;
    }

    /**
     * 审批人: APPROVED_BY
     */
    public final void setApprovedBy(java.lang.String value){
        if(onPropSet(PROP_ID_approvedBy,value)){
            this._approvedBy = value;
            internalClearRefs(PROP_ID_approvedBy);
            
        }
    }
    
    /**
     * 审批时间: APPROVED_AT
     */
    public final java.time.LocalDateTime getApprovedAt(){
         onPropGet(PROP_ID_approvedAt);
         return _approvedAt;
    }

    /**
     * 审批时间: APPROVED_AT
     */
    public final void setApprovedAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_approvedAt,value)){
            this._approvedAt = value;
            internalClearRefs(PROP_ID_approvedAt);
            
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
    public final app.erp.qa.dao.entity.ErpQaNonConformance getSourceNcr(){
       return (app.erp.qa.dao.entity.ErpQaNonConformance)internalGetRefEntity(PROP_NAME_sourceNcr);
    }

    public final void setSourceNcr(app.erp.qa.dao.entity.ErpQaNonConformance refEntity){
   
           if(refEntity == null){
           
                   this.setSourceNcrId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_sourceNcr, refEntity,()->{
           
                           this.setSourceNcrId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdMaterial getMaterial(){
       return (app.erp.md.dao.entity.ErpMdMaterial)internalGetRefEntity(PROP_NAME_material);
    }

    public final void setMaterial(app.erp.md.dao.entity.ErpMdMaterial refEntity){
   
           if(refEntity == null){
           
                   this.setMaterialId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_material, refEntity,()->{
           
                           this.setMaterialId(refEntity.getId());
                       
           });
           }
       
    }
       
    private final OrmEntitySet<app.erp.qa.dao.entity.ErpQaRecallTarget> _targets = new OrmEntitySet<>(this, PROP_NAME_targets,
        null, null,app.erp.qa.dao.entity.ErpQaRecallTarget.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.qa.dao.entity.ErpQaRecallTarget> getTargets(){
       return _targets;
    }
       
}
// resume CPD analysis - CPD-ON
