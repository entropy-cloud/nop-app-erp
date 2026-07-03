package app.erp.cs.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.cs.dao.entity.ErpCsTicketAction;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  工单操作日志: erp_cs_ticket_action
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpCsTicketAction extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 工单ID: TICKET_ID BIGINT */
    public static final String PROP_NAME_ticketId = "ticketId";
    public static final int PROP_ID_ticketId = 2;
    
    /* 操作类型: ACTION_TYPE VARCHAR */
    public static final String PROP_NAME_actionType = "actionType";
    public static final int PROP_ID_actionType = 3;
    
    /* 起始状态: FROM_STATUS VARCHAR */
    public static final String PROP_NAME_fromStatus = "fromStatus";
    public static final int PROP_ID_fromStatus = 4;
    
    /* 目标状态: TO_STATUS VARCHAR */
    public static final String PROP_NAME_toStatus = "toStatus";
    public static final int PROP_ID_toStatus = 5;
    
    /* 操作人: OPERATOR_ID VARCHAR */
    public static final String PROP_NAME_operatorId = "operatorId";
    public static final int PROP_ID_operatorId = 6;
    
    /* 操作内容: CONTENT VARCHAR */
    public static final String PROP_NAME_content = "content";
    public static final int PROP_ID_content = 7;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 8;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 9;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 10;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 11;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 12;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 13;
    

    private static int _PROP_ID_BOUND = 14;

    
    /* relation:  */
    public static final String PROP_NAME_ticket = "ticket";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[14];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_ticketId] = PROP_NAME_ticketId;
          PROP_NAME_TO_ID.put(PROP_NAME_ticketId, PROP_ID_ticketId);
      
          PROP_ID_TO_NAME[PROP_ID_actionType] = PROP_NAME_actionType;
          PROP_NAME_TO_ID.put(PROP_NAME_actionType, PROP_ID_actionType);
      
          PROP_ID_TO_NAME[PROP_ID_fromStatus] = PROP_NAME_fromStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_fromStatus, PROP_ID_fromStatus);
      
          PROP_ID_TO_NAME[PROP_ID_toStatus] = PROP_NAME_toStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_toStatus, PROP_ID_toStatus);
      
          PROP_ID_TO_NAME[PROP_ID_operatorId] = PROP_NAME_operatorId;
          PROP_NAME_TO_ID.put(PROP_NAME_operatorId, PROP_ID_operatorId);
      
          PROP_ID_TO_NAME[PROP_ID_content] = PROP_NAME_content;
          PROP_NAME_TO_ID.put(PROP_NAME_content, PROP_ID_content);
      
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
    
    /* 工单ID: TICKET_ID */
    private java.lang.Long _ticketId;
    
    /* 操作类型: ACTION_TYPE */
    private java.lang.String _actionType;
    
    /* 起始状态: FROM_STATUS */
    private java.lang.String _fromStatus;
    
    /* 目标状态: TO_STATUS */
    private java.lang.String _toStatus;
    
    /* 操作人: OPERATOR_ID */
    private java.lang.String _operatorId;
    
    /* 操作内容: CONTENT */
    private java.lang.String _content;
    
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
    

    public _ErpCsTicketAction(){
        // for debug
    }

    protected ErpCsTicketAction newInstance(){
        ErpCsTicketAction entity = new ErpCsTicketAction();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpCsTicketAction cloneInstance() {
        ErpCsTicketAction entity = newInstance();
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
      return "app.erp.cs.dao.entity.ErpCsTicketAction";
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
        
            case PROP_ID_ticketId:
               return getTicketId();
        
            case PROP_ID_actionType:
               return getActionType();
        
            case PROP_ID_fromStatus:
               return getFromStatus();
        
            case PROP_ID_toStatus:
               return getToStatus();
        
            case PROP_ID_operatorId:
               return getOperatorId();
        
            case PROP_ID_content:
               return getContent();
        
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
        
            case PROP_ID_ticketId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_ticketId));
               }
               setTicketId(typedValue);
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
        
            case PROP_ID_fromStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_fromStatus));
               }
               setFromStatus(typedValue);
               break;
            }
        
            case PROP_ID_toStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_toStatus));
               }
               setToStatus(typedValue);
               break;
            }
        
            case PROP_ID_operatorId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_operatorId));
               }
               setOperatorId(typedValue);
               break;
            }
        
            case PROP_ID_content:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_content));
               }
               setContent(typedValue);
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
        
            case PROP_ID_ticketId:{
               onInitProp(propId);
               this._ticketId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_actionType:{
               onInitProp(propId);
               this._actionType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_fromStatus:{
               onInitProp(propId);
               this._fromStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_toStatus:{
               onInitProp(propId);
               this._toStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_operatorId:{
               onInitProp(propId);
               this._operatorId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_content:{
               onInitProp(propId);
               this._content = (java.lang.String)value;
               
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
     * 工单ID: TICKET_ID
     */
    public final java.lang.Long getTicketId(){
         onPropGet(PROP_ID_ticketId);
         return _ticketId;
    }

    /**
     * 工单ID: TICKET_ID
     */
    public final void setTicketId(java.lang.Long value){
        if(onPropSet(PROP_ID_ticketId,value)){
            this._ticketId = value;
            internalClearRefs(PROP_ID_ticketId);
            
        }
    }
    
    /**
     * 操作类型: ACTION_TYPE
     */
    public final java.lang.String getActionType(){
         onPropGet(PROP_ID_actionType);
         return _actionType;
    }

    /**
     * 操作类型: ACTION_TYPE
     */
    public final void setActionType(java.lang.String value){
        if(onPropSet(PROP_ID_actionType,value)){
            this._actionType = value;
            internalClearRefs(PROP_ID_actionType);
            
        }
    }
    
    /**
     * 起始状态: FROM_STATUS
     */
    public final java.lang.String getFromStatus(){
         onPropGet(PROP_ID_fromStatus);
         return _fromStatus;
    }

    /**
     * 起始状态: FROM_STATUS
     */
    public final void setFromStatus(java.lang.String value){
        if(onPropSet(PROP_ID_fromStatus,value)){
            this._fromStatus = value;
            internalClearRefs(PROP_ID_fromStatus);
            
        }
    }
    
    /**
     * 目标状态: TO_STATUS
     */
    public final java.lang.String getToStatus(){
         onPropGet(PROP_ID_toStatus);
         return _toStatus;
    }

    /**
     * 目标状态: TO_STATUS
     */
    public final void setToStatus(java.lang.String value){
        if(onPropSet(PROP_ID_toStatus,value)){
            this._toStatus = value;
            internalClearRefs(PROP_ID_toStatus);
            
        }
    }
    
    /**
     * 操作人: OPERATOR_ID
     */
    public final java.lang.String getOperatorId(){
         onPropGet(PROP_ID_operatorId);
         return _operatorId;
    }

    /**
     * 操作人: OPERATOR_ID
     */
    public final void setOperatorId(java.lang.String value){
        if(onPropSet(PROP_ID_operatorId,value)){
            this._operatorId = value;
            internalClearRefs(PROP_ID_operatorId);
            
        }
    }
    
    /**
     * 操作内容: CONTENT
     */
    public final java.lang.String getContent(){
         onPropGet(PROP_ID_content);
         return _content;
    }

    /**
     * 操作内容: CONTENT
     */
    public final void setContent(java.lang.String value){
        if(onPropSet(PROP_ID_content,value)){
            this._content = value;
            internalClearRefs(PROP_ID_content);
            
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
    public final app.erp.cs.dao.entity.ErpCsTicket getTicket(){
       return (app.erp.cs.dao.entity.ErpCsTicket)internalGetRefEntity(PROP_NAME_ticket);
    }

    public final void setTicket(app.erp.cs.dao.entity.ErpCsTicket refEntity){
   
           if(refEntity == null){
           
                   this.setTicketId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_ticket, refEntity,()->{
           
                           this.setTicketId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
