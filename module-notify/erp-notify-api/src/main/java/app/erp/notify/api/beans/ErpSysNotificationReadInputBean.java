//__XGEN_FORCE_OVERRIDE__
    package app.erp.notify.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpSysNotificationReadInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
            this._id = value;
        }


        private String _notificationId;

    
        @PropMeta(propId=2)
    
        public String getNotificationId(){
            return _notificationId;
        }

        public void setNotificationId(String value){
            this._notificationId = value;
        }


        private String _userId;

    
        @PropMeta(propId=3)
    
        public String getUserId(){
            return _userId;
        }

        public void setUserId(String value){
            this._userId = value;
        }


        private java.sql.Timestamp _readTime;

    
        @PropMeta(propId=4)
    
        public java.sql.Timestamp getReadTime(){
            return _readTime;
        }

        public void setReadTime(java.sql.Timestamp value){
            this._readTime = value;
        }


    }
