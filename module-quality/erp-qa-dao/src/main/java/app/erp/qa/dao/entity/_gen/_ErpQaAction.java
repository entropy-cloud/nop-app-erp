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

import app.erp.qa.dao.entity.ErpQaAction;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  纠正预防措施: erp_qa_action
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpQaAction extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 不合格品报告ID: NCR_ID BIGINT */
    public static final String PROP_NAME_ncrId = "ncrId";
    public static final int PROP_ID_ncrId = 2;
    
    /* 措施类型: ACTION_TYPE VARCHAR */
    public static final String PROP_NAME_actionType = "actionType";
    public static final int PROP_ID_actionType = 3;
    
    /* 措施描述: DESCRIPTION VARCHAR */
    public static final String PROP_NAME_description = "description";
    public static final int PROP_ID_description = 4;
    
    /* 负责人: RESPONSIBLE_PERSON BIGINT */
    public static final String PROP_NAME_responsiblePerson = "responsiblePerson";
    public static final int PROP_ID_responsiblePerson = 5;
    
    /* 计划完成日期: DUE_DATE DATE */
    public static final String PROP_NAME_dueDate = "dueDate";
    public static final int PROP_ID_dueDate = 6;
    
    /* 状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 7;
    
    /* 完成人: COMPLETED_BY BIGINT */
    public static final String PROP_NAME_completedBy = "completedBy";
    public static final int PROP_ID_completedBy = 8;
    
    /* 完成时间: COMPLETED_AT TIMESTAMP */
    public static final String PROP_NAME_completedAt = "completedAt";
    public static final int PROP_ID_completedAt = 9;
    
    /* 验证人: VERIFICATION_PERSON BIGINT */
    public static final String PROP_NAME_verificationPerson = "verificationPerson";
    public static final int PROP_ID_verificationPerson = 10;
    
    /* 验证日期: VERIFICATION_DATE DATE */
    public static final String PROP_NAME_verificationDate = "verificationDate";
    public static final int PROP_ID_verificationDate = 11;
    
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
    public static final String PROP_NAME_ncr = "ncr";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[19];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_ncrId] = PROP_NAME_ncrId;
          PROP_NAME_TO_ID.put(PROP_NAME_ncrId, PROP_ID_ncrId);
      
          PROP_ID_TO_NAME[PROP_ID_actionType] = PROP_NAME_actionType;
          PROP_NAME_TO_ID.put(PROP_NAME_actionType, PROP_ID_actionType);
      
          PROP_ID_TO_NAME[PROP_ID_description] = PROP_NAME_description;
          PROP_NAME_TO_ID.put(PROP_NAME_description, PROP_ID_description);
      
          PROP_ID_TO_NAME[PROP_ID_responsiblePerson] = PROP_NAME_responsiblePerson;
          PROP_NAME_TO_ID.put(PROP_NAME_responsiblePerson, PROP_ID_responsiblePerson);
      
          PROP_ID_TO_NAME[PROP_ID_dueDate] = PROP_NAME_dueDate;
          PROP_NAME_TO_ID.put(PROP_NAME_dueDate, PROP_ID_dueDate);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_completedBy] = PROP_NAME_completedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_completedBy, PROP_ID_completedBy);
      
          PROP_ID_TO_NAME[PROP_ID_completedAt] = PROP_NAME_completedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_completedAt, PROP_ID_completedAt);
      
          PROP_ID_TO_NAME[PROP_ID_verificationPerson] = PROP_NAME_verificationPerson;
          PROP_NAME_TO_ID.put(PROP_NAME_verificationPerson, PROP_ID_verificationPerson);
      
          PROP_ID_TO_NAME[PROP_ID_verificationDate] = PROP_NAME_verificationDate;
          PROP_NAME_TO_ID.put(PROP_NAME_verificationDate, PROP_ID_verificationDate);
      
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
    
    /* 不合格品报告ID: NCR_ID */
    private java.lang.Long _ncrId;
    
    /* 措施类型: ACTION_TYPE */
    private java.lang.String _actionType;
    
    /* 措施描述: DESCRIPTION */
    private java.lang.String _description;
    
    /* 负责人: RESPONSIBLE_PERSON */
    private java.lang.Long _responsiblePerson;
    
    /* 计划完成日期: DUE_DATE */
    private java.time.LocalDate _dueDate;
    
    /* 状态: STATUS */
    private java.lang.String _status;
    
    /* 完成人: COMPLETED_BY */
    private java.lang.Long _completedBy;
    
    /* 完成时间: COMPLETED_AT */
    private java.sql.Timestamp _completedAt;
    
    /* 验证人: VERIFICATION_PERSON */
    private java.lang.Long _verificationPerson;
    
    /* 验证日期: VERIFICATION_DATE */
    private java.time.LocalDate _verificationDate;
    
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
    

    public _ErpQaAction(){
        // for debug
    }

    protected ErpQaAction newInstance(){
        ErpQaAction entity = new ErpQaAction();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpQaAction cloneInstance() {
        ErpQaAction entity = newInstance();
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
      return "app.erp.qa.dao.entity.ErpQaAction";
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
        
            case PROP_ID_ncrId:
               return getNcrId();
        
            case PROP_ID_actionType:
               return getActionType();
        
            case PROP_ID_description:
               return getDescription();
        
            case PROP_ID_responsiblePerson:
               return getResponsiblePerson();
        
            case PROP_ID_dueDate:
               return getDueDate();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_completedBy:
               return getCompletedBy();
        
            case PROP_ID_completedAt:
               return getCompletedAt();
        
            case PROP_ID_verificationPerson:
               return getVerificationPerson();
        
            case PROP_ID_verificationDate:
               return getVerificationDate();
        
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
        
            case PROP_ID_ncrId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_ncrId));
               }
               setNcrId(typedValue);
               break;
            }
        
            case PROP_ID_actionType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_actionType));
               }
               setActionType(typedValue);
               break;
            }
        
            case PROP_ID_description:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_description));
               }
               setDescription(typedValue);
               break;
            }
        
            case PROP_ID_responsiblePerson:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_responsiblePerson));
               }
               setResponsiblePerson(typedValue);
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
        
            case PROP_ID_status:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_status));
               }
               setStatus(typedValue);
               break;
            }
        
            case PROP_ID_completedBy:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_completedBy));
               }
               setCompletedBy(typedValue);
               break;
            }
        
            case PROP_ID_completedAt:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_completedAt));
               }
               setCompletedAt(typedValue);
               break;
            }
        
            case PROP_ID_verificationPerson:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_verificationPerson));
               }
               setVerificationPerson(typedValue);
               break;
            }
        
            case PROP_ID_verificationDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_verificationDate));
               }
               setVerificationDate(typedValue);
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
        
            case PROP_ID_ncrId:{
               onInitProp(propId);
               this._ncrId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_actionType:{
               onInitProp(propId);
               this._actionType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_description:{
               onInitProp(propId);
               this._description = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_responsiblePerson:{
               onInitProp(propId);
               this._responsiblePerson = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_dueDate:{
               onInitProp(propId);
               this._dueDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_completedBy:{
               onInitProp(propId);
               this._completedBy = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_completedAt:{
               onInitProp(propId);
               this._completedAt = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_verificationPerson:{
               onInitProp(propId);
               this._verificationPerson = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_verificationDate:{
               onInitProp(propId);
               this._verificationDate = (java.time.LocalDate)value;
               
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
     * 不合格品报告ID: NCR_ID
     */
    public final java.lang.Long getNcrId(){
         onPropGet(PROP_ID_ncrId);
         return _ncrId;
    }

    /**
     * 不合格品报告ID: NCR_ID
     */
    public final void setNcrId(java.lang.Long value){
        if(onPropSet(PROP_ID_ncrId,value)){
            this._ncrId = value;
            internalClearRefs(PROP_ID_ncrId);
            
        }
    }
    
    /**
     * 措施类型: ACTION_TYPE
     */
    public final java.lang.String getActionType(){
         onPropGet(PROP_ID_actionType);
         return _actionType;
    }

    /**
     * 措施类型: ACTION_TYPE
     */
    public final void setActionType(java.lang.String value){
        if(onPropSet(PROP_ID_actionType,value)){
            this._actionType = value;
            internalClearRefs(PROP_ID_actionType);
            
        }
    }
    
    /**
     * 措施描述: DESCRIPTION
     */
    public final java.lang.String getDescription(){
         onPropGet(PROP_ID_description);
         return _description;
    }

    /**
     * 措施描述: DESCRIPTION
     */
    public final void setDescription(java.lang.String value){
        if(onPropSet(PROP_ID_description,value)){
            this._description = value;
            internalClearRefs(PROP_ID_description);
            
        }
    }
    
    /**
     * 负责人: RESPONSIBLE_PERSON
     */
    public final java.lang.Long getResponsiblePerson(){
         onPropGet(PROP_ID_responsiblePerson);
         return _responsiblePerson;
    }

    /**
     * 负责人: RESPONSIBLE_PERSON
     */
    public final void setResponsiblePerson(java.lang.Long value){
        if(onPropSet(PROP_ID_responsiblePerson,value)){
            this._responsiblePerson = value;
            internalClearRefs(PROP_ID_responsiblePerson);
            
        }
    }
    
    /**
     * 计划完成日期: DUE_DATE
     */
    public final java.time.LocalDate getDueDate(){
         onPropGet(PROP_ID_dueDate);
         return _dueDate;
    }

    /**
     * 计划完成日期: DUE_DATE
     */
    public final void setDueDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_dueDate,value)){
            this._dueDate = value;
            internalClearRefs(PROP_ID_dueDate);
            
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
     * 完成人: COMPLETED_BY
     */
    public final java.lang.Long getCompletedBy(){
         onPropGet(PROP_ID_completedBy);
         return _completedBy;
    }

    /**
     * 完成人: COMPLETED_BY
     */
    public final void setCompletedBy(java.lang.Long value){
        if(onPropSet(PROP_ID_completedBy,value)){
            this._completedBy = value;
            internalClearRefs(PROP_ID_completedBy);
            
        }
    }
    
    /**
     * 完成时间: COMPLETED_AT
     */
    public final java.sql.Timestamp getCompletedAt(){
         onPropGet(PROP_ID_completedAt);
         return _completedAt;
    }

    /**
     * 完成时间: COMPLETED_AT
     */
    public final void setCompletedAt(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_completedAt,value)){
            this._completedAt = value;
            internalClearRefs(PROP_ID_completedAt);
            
        }
    }
    
    /**
     * 验证人: VERIFICATION_PERSON
     */
    public final java.lang.Long getVerificationPerson(){
         onPropGet(PROP_ID_verificationPerson);
         return _verificationPerson;
    }

    /**
     * 验证人: VERIFICATION_PERSON
     */
    public final void setVerificationPerson(java.lang.Long value){
        if(onPropSet(PROP_ID_verificationPerson,value)){
            this._verificationPerson = value;
            internalClearRefs(PROP_ID_verificationPerson);
            
        }
    }
    
    /**
     * 验证日期: VERIFICATION_DATE
     */
    public final java.time.LocalDate getVerificationDate(){
         onPropGet(PROP_ID_verificationDate);
         return _verificationDate;
    }

    /**
     * 验证日期: VERIFICATION_DATE
     */
    public final void setVerificationDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_verificationDate,value)){
            this._verificationDate = value;
            internalClearRefs(PROP_ID_verificationDate);
            
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
    public final app.erp.qa.dao.entity.ErpQaNonConformance getNcr(){
       return (app.erp.qa.dao.entity.ErpQaNonConformance)internalGetRefEntity(PROP_NAME_ncr);
    }

    public final void setNcr(app.erp.qa.dao.entity.ErpQaNonConformance refEntity){
   
           if(refEntity == null){
           
                   this.setNcrId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_ncr, refEntity,()->{
           
                           this.setNcrId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
