//__XGEN_FORCE_OVERRIDE__
    package app.erp.ct.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCtApprovalRecordInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _contractId;

    
        @PropMeta(propId=2)
    
        public Long getContractId(){
            return _contractId;
        }

        public void setContractId(Long value){
            this._contractId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _approvalMatrixId;

    
        @PropMeta(propId=4)
    
        public Long getApprovalMatrixId(){
            return _approvalMatrixId;
        }

        public void setApprovalMatrixId(Long value){
            this._approvalMatrixId = value;
        }


        private Integer _approvalOrder;

    
        @PropMeta(propId=5)
    
        public Integer getApprovalOrder(){
            return _approvalOrder;
        }

        public void setApprovalOrder(Integer value){
            this._approvalOrder = value;
        }


        private String _approverId;

    
        @PropMeta(propId=6)
    
        public String getApproverId(){
            return _approverId;
        }

        public void setApproverId(String value){
            this._approverId = value;
        }


        private String _approvalStatus;

    
        @PropMeta(propId=7)
    
        public String getApprovalStatus(){
            return _approvalStatus;
        }

        public void setApprovalStatus(String value){
            this._approvalStatus = value;
        }


        private String _comment;

    
        @PropMeta(propId=8)
    
        public String getComment(){
            return _comment;
        }

        public void setComment(String value){
            this._comment = value;
        }


        private java.sql.Timestamp _rejectedAt;

    
        @PropMeta(propId=10)
    
        public java.sql.Timestamp getRejectedAt(){
            return _rejectedAt;
        }

        public void setRejectedAt(java.sql.Timestamp value){
            this._rejectedAt = value;
        }


        private String _remark;

    
        @PropMeta(propId=11)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
