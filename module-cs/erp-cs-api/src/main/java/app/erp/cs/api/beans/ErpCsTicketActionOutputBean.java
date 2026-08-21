//__XGEN_FORCE_OVERRIDE__
    package app.erp.cs.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCsTicketActionOutputBean {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _ticketId;

    
        @PropMeta(propId=2)
    
        public String getTicketId(){
            return _ticketId;
        }

        public void setTicketId(String value){
            this._ticketId = value;
        }


        private String _actionType;

    
        @PropMeta(propId=3)
    
        public String getActionType(){
            return _actionType;
        }

        public void setActionType(String value){
            this._actionType = value;
        }


        private String _actionType_label;

    
        public String getActionType_label(){
            return _actionType_label;
        }

        public void setActionType_label(String value){
            this._actionType_label = value;
        }


        private String _fromStatus;

    
        @PropMeta(propId=4)
    
        public String getFromStatus(){
            return _fromStatus;
        }

        public void setFromStatus(String value){
            this._fromStatus = value;
        }


        private String _fromStatus_label;

    
        public String getFromStatus_label(){
            return _fromStatus_label;
        }

        public void setFromStatus_label(String value){
            this._fromStatus_label = value;
        }


        private String _toStatus;

    
        @PropMeta(propId=5)
    
        public String getToStatus(){
            return _toStatus;
        }

        public void setToStatus(String value){
            this._toStatus = value;
        }


        private String _toStatus_label;

    
        public String getToStatus_label(){
            return _toStatus_label;
        }

        public void setToStatus_label(String value){
            this._toStatus_label = value;
        }


        private String _operatorId;

    
        @PropMeta(propId=6)
    
        public String getOperatorId(){
            return _operatorId;
        }

        public void setOperatorId(String value){
            this._operatorId = value;
        }


        private String _content;

    
        @PropMeta(propId=7)
    
        public String getContent(){
            return _content;
        }

        public void setContent(String value){
            this._content = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=8)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=9)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=10)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=11)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=12)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=13)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _ticket;

        public Map<String,Object> getTicket(){
            return _ticket;
        }

        public void setTicket(Map<String,Object> value){
            this._ticket = value;
        }


    }
