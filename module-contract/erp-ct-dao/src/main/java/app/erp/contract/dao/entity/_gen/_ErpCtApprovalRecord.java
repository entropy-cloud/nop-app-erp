package app.erp.contract.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.contract.dao.entity.ErpCtApprovalRecord;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  审批记录: erp_ct_approval_record
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpCtApprovalRecord extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 合同: CONTRACT_ID BIGINT */
    public static final String PROP_NAME_contractId = "contractId";
    public static final int PROP_ID_contractId = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 审批矩阵: APPROVAL_MATRIX_ID BIGINT */
    public static final String PROP_NAME_approvalMatrixId = "approvalMatrixId";
    public static final int PROP_ID_approvalMatrixId = 4;
    
    /* 顺序号: APPROVAL_ORDER INTEGER */
    public static final String PROP_NAME_approvalOrder = "approvalOrder";
    public static final int PROP_ID_approvalOrder = 5;
    
    /* 审批人: APPROVER_ID VARCHAR */
    public static final String PROP_NAME_approverId = "approverId";
    public static final int PROP_ID_approverId = 6;
    
    /* 审批状态: APPROVAL_STATUS INTEGER */
    public static final String PROP_NAME_approvalStatus = "approvalStatus";
    public static final int PROP_ID_approvalStatus = 7;
    
    /* 审批意见: COMMENT VARCHAR */
    public static final String PROP_NAME_comment = "comment";
    public static final int PROP_ID_comment = 8;
    
    /* 通过时间: APPROVED_AT DATETIME */
    public static final String PROP_NAME_approvedAt = "approvedAt";
    public static final int PROP_ID_approvedAt = 9;
    
    /* 驳回时间: REJECTED_AT DATETIME */
    public static final String PROP_NAME_rejectedAt = "rejectedAt";
    public static final int PROP_ID_rejectedAt = 10;
    
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
    public static final String PROP_NAME_contract = "contract";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    
    /* relation:  */
    public static final String PROP_NAME_approvalMatrix = "approvalMatrix";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[18];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_contractId] = PROP_NAME_contractId;
          PROP_NAME_TO_ID.put(PROP_NAME_contractId, PROP_ID_contractId);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_approvalMatrixId] = PROP_NAME_approvalMatrixId;
          PROP_NAME_TO_ID.put(PROP_NAME_approvalMatrixId, PROP_ID_approvalMatrixId);
      
          PROP_ID_TO_NAME[PROP_ID_approvalOrder] = PROP_NAME_approvalOrder;
          PROP_NAME_TO_ID.put(PROP_NAME_approvalOrder, PROP_ID_approvalOrder);
      
          PROP_ID_TO_NAME[PROP_ID_approverId] = PROP_NAME_approverId;
          PROP_NAME_TO_ID.put(PROP_NAME_approverId, PROP_ID_approverId);
      
          PROP_ID_TO_NAME[PROP_ID_approvalStatus] = PROP_NAME_approvalStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_approvalStatus, PROP_ID_approvalStatus);
      
          PROP_ID_TO_NAME[PROP_ID_comment] = PROP_NAME_comment;
          PROP_NAME_TO_ID.put(PROP_NAME_comment, PROP_ID_comment);
      
          PROP_ID_TO_NAME[PROP_ID_approvedAt] = PROP_NAME_approvedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_approvedAt, PROP_ID_approvedAt);
      
          PROP_ID_TO_NAME[PROP_ID_rejectedAt] = PROP_NAME_rejectedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_rejectedAt, PROP_ID_rejectedAt);
      
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
    
    /* 合同: CONTRACT_ID */
    private java.lang.Long _contractId;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 审批矩阵: APPROVAL_MATRIX_ID */
    private java.lang.Long _approvalMatrixId;
    
    /* 顺序号: APPROVAL_ORDER */
    private java.lang.Integer _approvalOrder;
    
    /* 审批人: APPROVER_ID */
    private java.lang.String _approverId;
    
    /* 审批状态: APPROVAL_STATUS */
    private java.lang.Integer _approvalStatus;
    
    /* 审批意见: COMMENT */
    private java.lang.String _comment;
    
    /* 通过时间: APPROVED_AT */
    private java.time.LocalDateTime _approvedAt;
    
    /* 驳回时间: REJECTED_AT */
    private java.time.LocalDateTime _rejectedAt;
    
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
    

    public _ErpCtApprovalRecord(){
        // for debug
    }

    protected ErpCtApprovalRecord newInstance(){
        ErpCtApprovalRecord entity = new ErpCtApprovalRecord();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpCtApprovalRecord cloneInstance() {
        ErpCtApprovalRecord entity = newInstance();
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
      return "app.erp.contract.dao.entity.ErpCtApprovalRecord";
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
        
            case PROP_ID_contractId:
               return getContractId();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_approvalMatrixId:
               return getApprovalMatrixId();
        
            case PROP_ID_approvalOrder:
               return getApprovalOrder();
        
            case PROP_ID_approverId:
               return getApproverId();
        
            case PROP_ID_approvalStatus:
               return getApprovalStatus();
        
            case PROP_ID_comment:
               return getComment();
        
            case PROP_ID_approvedAt:
               return getApprovedAt();
        
            case PROP_ID_rejectedAt:
               return getRejectedAt();
        
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
        
            case PROP_ID_contractId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_contractId));
               }
               setContractId(typedValue);
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
        
            case PROP_ID_approvalMatrixId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_approvalMatrixId));
               }
               setApprovalMatrixId(typedValue);
               break;
            }
        
            case PROP_ID_approvalOrder:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_approvalOrder));
               }
               setApprovalOrder(typedValue);
               break;
            }
        
            case PROP_ID_approverId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_approverId));
               }
               setApproverId(typedValue);
               break;
            }
        
            case PROP_ID_approvalStatus:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_approvalStatus));
               }
               setApprovalStatus(typedValue);
               break;
            }
        
            case PROP_ID_comment:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_comment));
               }
               setComment(typedValue);
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
        
            case PROP_ID_rejectedAt:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_rejectedAt));
               }
               setRejectedAt(typedValue);
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
        
            case PROP_ID_contractId:{
               onInitProp(propId);
               this._contractId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_approvalMatrixId:{
               onInitProp(propId);
               this._approvalMatrixId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_approvalOrder:{
               onInitProp(propId);
               this._approvalOrder = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_approverId:{
               onInitProp(propId);
               this._approverId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_approvalStatus:{
               onInitProp(propId);
               this._approvalStatus = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_comment:{
               onInitProp(propId);
               this._comment = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_approvedAt:{
               onInitProp(propId);
               this._approvedAt = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_rejectedAt:{
               onInitProp(propId);
               this._rejectedAt = (java.time.LocalDateTime)value;
               
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
     * 合同: CONTRACT_ID
     */
    public final java.lang.Long getContractId(){
         onPropGet(PROP_ID_contractId);
         return _contractId;
    }

    /**
     * 合同: CONTRACT_ID
     */
    public final void setContractId(java.lang.Long value){
        if(onPropSet(PROP_ID_contractId,value)){
            this._contractId = value;
            internalClearRefs(PROP_ID_contractId);
            
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
     * 审批矩阵: APPROVAL_MATRIX_ID
     */
    public final java.lang.Long getApprovalMatrixId(){
         onPropGet(PROP_ID_approvalMatrixId);
         return _approvalMatrixId;
    }

    /**
     * 审批矩阵: APPROVAL_MATRIX_ID
     */
    public final void setApprovalMatrixId(java.lang.Long value){
        if(onPropSet(PROP_ID_approvalMatrixId,value)){
            this._approvalMatrixId = value;
            internalClearRefs(PROP_ID_approvalMatrixId);
            
        }
    }
    
    /**
     * 顺序号: APPROVAL_ORDER
     */
    public final java.lang.Integer getApprovalOrder(){
         onPropGet(PROP_ID_approvalOrder);
         return _approvalOrder;
    }

    /**
     * 顺序号: APPROVAL_ORDER
     */
    public final void setApprovalOrder(java.lang.Integer value){
        if(onPropSet(PROP_ID_approvalOrder,value)){
            this._approvalOrder = value;
            internalClearRefs(PROP_ID_approvalOrder);
            
        }
    }
    
    /**
     * 审批人: APPROVER_ID
     */
    public final java.lang.String getApproverId(){
         onPropGet(PROP_ID_approverId);
         return _approverId;
    }

    /**
     * 审批人: APPROVER_ID
     */
    public final void setApproverId(java.lang.String value){
        if(onPropSet(PROP_ID_approverId,value)){
            this._approverId = value;
            internalClearRefs(PROP_ID_approverId);
            
        }
    }
    
    /**
     * 审批状态: APPROVAL_STATUS
     */
    public final java.lang.Integer getApprovalStatus(){
         onPropGet(PROP_ID_approvalStatus);
         return _approvalStatus;
    }

    /**
     * 审批状态: APPROVAL_STATUS
     */
    public final void setApprovalStatus(java.lang.Integer value){
        if(onPropSet(PROP_ID_approvalStatus,value)){
            this._approvalStatus = value;
            internalClearRefs(PROP_ID_approvalStatus);
            
        }
    }
    
    /**
     * 审批意见: COMMENT
     */
    public final java.lang.String getComment(){
         onPropGet(PROP_ID_comment);
         return _comment;
    }

    /**
     * 审批意见: COMMENT
     */
    public final void setComment(java.lang.String value){
        if(onPropSet(PROP_ID_comment,value)){
            this._comment = value;
            internalClearRefs(PROP_ID_comment);
            
        }
    }
    
    /**
     * 通过时间: APPROVED_AT
     */
    public final java.time.LocalDateTime getApprovedAt(){
         onPropGet(PROP_ID_approvedAt);
         return _approvedAt;
    }

    /**
     * 通过时间: APPROVED_AT
     */
    public final void setApprovedAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_approvedAt,value)){
            this._approvedAt = value;
            internalClearRefs(PROP_ID_approvedAt);
            
        }
    }
    
    /**
     * 驳回时间: REJECTED_AT
     */
    public final java.time.LocalDateTime getRejectedAt(){
         onPropGet(PROP_ID_rejectedAt);
         return _rejectedAt;
    }

    /**
     * 驳回时间: REJECTED_AT
     */
    public final void setRejectedAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_rejectedAt,value)){
            this._rejectedAt = value;
            internalClearRefs(PROP_ID_rejectedAt);
            
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
    public final app.erp.contract.dao.entity.ErpCtContract getContract(){
       return (app.erp.contract.dao.entity.ErpCtContract)internalGetRefEntity(PROP_NAME_contract);
    }

    public final void setContract(app.erp.contract.dao.entity.ErpCtContract refEntity){
   
           if(refEntity == null){
           
                   this.setContractId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_contract, refEntity,()->{
           
                           this.setContractId(refEntity.getId());
                       
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
    public final app.erp.contract.dao.entity.ErpCtApprovalMatrix getApprovalMatrix(){
       return (app.erp.contract.dao.entity.ErpCtApprovalMatrix)internalGetRefEntity(PROP_NAME_approvalMatrix);
    }

    public final void setApprovalMatrix(app.erp.contract.dao.entity.ErpCtApprovalMatrix refEntity){
   
           if(refEntity == null){
           
                   this.setApprovalMatrixId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_approvalMatrix, refEntity,()->{
           
                           this.setApprovalMatrixId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
