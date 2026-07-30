//__XGEN_FORCE_OVERRIDE__
    package app.erp.cs.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCsTicketActionInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _ticketId;

    
        @PropMeta(propId=2)
    
        public Long getTicketId(){
            return _ticketId;
        }

        public void setTicketId(Long value){
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


        private String _fromStatus;

    
        @PropMeta(propId=4)
    
        public String getFromStatus(){
            return _fromStatus;
        }

        public void setFromStatus(String value){
            this._fromStatus = value;
        }


        private String _toStatus;

    
        @PropMeta(propId=5)
    
        public String getToStatus(){
            return _toStatus;
        }

        public void setToStatus(String value){
            this._toStatus = value;
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


    }
