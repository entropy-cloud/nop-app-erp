//__XGEN_FORCE_OVERRIDE__
    package app.erp.ct.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCtApprovalRecordOutputBean {

    
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


        private String _approvalStatus_label;

    
        public String getApprovalStatus_label(){
            return _approvalStatus_label;
        }

        public void setApprovalStatus_label(String value){
            this._approvalStatus_label = value;
        }


        private String _comment;

    
        @PropMeta(propId=8)
    
        public String getComment(){
            return _comment;
        }

        public void setComment(String value){
            this._comment = value;
        }


        private java.sql.Timestamp _approvedAt;

    
        @PropMeta(propId=9)
    
        public java.sql.Timestamp getApprovedAt(){
            return _approvedAt;
        }

        public void setApprovedAt(java.sql.Timestamp value){
            this._approvedAt = value;
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


        private Long _delVersion;

    
        @PropMeta(propId=12)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=13)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=14)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=15)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=16)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=17)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _contract;

        public Map<String,Object> getContract(){
            return _contract;
        }

        public void setContract(Map<String,Object> value){
            this._contract = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


        private Map<String,Object> _approvalMatrix;

        public Map<String,Object> getApprovalMatrix(){
            return _approvalMatrix;
        }

        public void setApprovalMatrix(Map<String,Object> value){
            this._approvalMatrix = value;
        }


    }
