//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrEmployeeOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private String _code;

    
        @PropMeta(propId=2)
    
        public String getCode(){
            return _code;
        }

        public void setCode(String value){
            this._code = value;
        }


        private String _firstName;

    
        @PropMeta(propId=3)
    
        public String getFirstName(){
            return _firstName;
        }

        public void setFirstName(String value){
            this._firstName = value;
        }


        private String _lastName;

    
        @PropMeta(propId=4)
    
        public String getLastName(){
            return _lastName;
        }

        public void setLastName(String value){
            this._lastName = value;
        }


        private String _fullName;

    
        @PropMeta(propId=5)
    
        public String getFullName(){
            return _fullName;
        }

        public void setFullName(String value){
            this._fullName = value;
        }


        private String _gender;

    
        @PropMeta(propId=6)
    
        public String getGender(){
            return _gender;
        }

        public void setGender(String value){
            this._gender = value;
        }


        private String _gender_label;

    
        public String getGender_label(){
            return _gender_label;
        }

        public void setGender_label(String value){
            this._gender_label = value;
        }


        private java.time.LocalDate _birthDate;

    
        @PropMeta(propId=7)
    
        public java.time.LocalDate getBirthDate(){
            return _birthDate;
        }

        public void setBirthDate(java.time.LocalDate value){
            this._birthDate = value;
        }


        private String _idCardType;

    
        @PropMeta(propId=8)
    
        public String getIdCardType(){
            return _idCardType;
        }

        public void setIdCardType(String value){
            this._idCardType = value;
        }


        private String _idCardNo;

    
        @PropMeta(propId=9)
    
        public String getIdCardNo(){
            return _idCardNo;
        }

        public void setIdCardNo(String value){
            this._idCardNo = value;
        }


        private String _email;

    
        @PropMeta(propId=10)
    
        public String getEmail(){
            return _email;
        }

        public void setEmail(String value){
            this._email = value;
        }


        private String _mobilePhone;

    
        @PropMeta(propId=11)
    
        public String getMobilePhone(){
            return _mobilePhone;
        }

        public void setMobilePhone(String value){
            this._mobilePhone = value;
        }


        private String _maritalStatus;

    
        @PropMeta(propId=12)
    
        public String getMaritalStatus(){
            return _maritalStatus;
        }

        public void setMaritalStatus(String value){
            this._maritalStatus = value;
        }


        private String _maritalStatus_label;

    
        public String getMaritalStatus_label(){
            return _maritalStatus_label;
        }

        public void setMaritalStatus_label(String value){
            this._maritalStatus_label = value;
        }


        private String _nationality;

    
        @PropMeta(propId=13)
    
        public String getNationality(){
            return _nationality;
        }

        public void setNationality(String value){
            this._nationality = value;
        }


        private String _emergencyContact;

    
        @PropMeta(propId=14)
    
        public String getEmergencyContact(){
            return _emergencyContact;
        }

        public void setEmergencyContact(String value){
            this._emergencyContact = value;
        }


        private String _emergencyPhone;

    
        @PropMeta(propId=15)
    
        public String getEmergencyPhone(){
            return _emergencyPhone;
        }

        public void setEmergencyPhone(String value){
            this._emergencyPhone = value;
        }


        private Long _departmentId;

    
        @PropMeta(propId=16)
    
        public Long getDepartmentId(){
            return _departmentId;
        }

        public void setDepartmentId(Long value){
            this._departmentId = value;
        }


        private Long _positionId;

    
        @PropMeta(propId=17)
    
        public Long getPositionId(){
            return _positionId;
        }

        public void setPositionId(Long value){
            this._positionId = value;
        }


        private String _jobTitle;

    
        @PropMeta(propId=18)
    
        public String getJobTitle(){
            return _jobTitle;
        }

        public void setJobTitle(String value){
            this._jobTitle = value;
        }


        private Long _superiorId;

    
        @PropMeta(propId=19)
    
        public Long getSuperiorId(){
            return _superiorId;
        }

        public void setSuperiorId(Long value){
            this._superiorId = value;
        }


        private Long _costCenterId;

    
        @PropMeta(propId=20)
    
        public Long getCostCenterId(){
            return _costCenterId;
        }

        public void setCostCenterId(Long value){
            this._costCenterId = value;
        }


        private java.time.LocalDate _hireDate;

    
        @PropMeta(propId=21)
    
        public java.time.LocalDate getHireDate(){
            return _hireDate;
        }

        public void setHireDate(java.time.LocalDate value){
            this._hireDate = value;
        }


        private java.time.LocalDate _probationEndDate;

    
        @PropMeta(propId=22)
    
        public java.time.LocalDate getProbationEndDate(){
            return _probationEndDate;
        }

        public void setProbationEndDate(java.time.LocalDate value){
            this._probationEndDate = value;
        }


        private java.time.LocalDate _regularDate;

    
        @PropMeta(propId=23)
    
        public java.time.LocalDate getRegularDate(){
            return _regularDate;
        }

        public void setRegularDate(java.time.LocalDate value){
            this._regularDate = value;
        }


        private java.time.LocalDate _resignationDate;

    
        @PropMeta(propId=24)
    
        public java.time.LocalDate getResignationDate(){
            return _resignationDate;
        }

        public void setResignationDate(java.time.LocalDate value){
            this._resignationDate = value;
        }


        private String _resignationReason;

    
        @PropMeta(propId=25)
    
        public String getResignationReason(){
            return _resignationReason;
        }

        public void setResignationReason(String value){
            this._resignationReason = value;
        }


        private String _employmentStatus;

    
        @PropMeta(propId=26)
    
        public String getEmploymentStatus(){
            return _employmentStatus;
        }

        public void setEmploymentStatus(String value){
            this._employmentStatus = value;
        }


        private String _employmentStatus_label;

    
        public String getEmploymentStatus_label(){
            return _employmentStatus_label;
        }

        public void setEmploymentStatus_label(String value){
            this._employmentStatus_label = value;
        }


        private String _employeeType;

    
        @PropMeta(propId=27)
    
        public String getEmployeeType(){
            return _employeeType;
        }

        public void setEmployeeType(String value){
            this._employeeType = value;
        }


        private String _employeeType_label;

    
        public String getEmployeeType_label(){
            return _employeeType_label;
        }

        public void setEmployeeType_label(String value){
            this._employeeType_label = value;
        }


        private Long _bankAccountId;

    
        @PropMeta(propId=28)
    
        public Long getBankAccountId(){
            return _bankAccountId;
        }

        public void setBankAccountId(Long value){
            this._bankAccountId = value;
        }


        private String _socialSecurityNo;

    
        @PropMeta(propId=29)
    
        public String getSocialSecurityNo(){
            return _socialSecurityNo;
        }

        public void setSocialSecurityNo(String value){
            this._socialSecurityNo = value;
        }


        private String _taxFileNo;

    
        @PropMeta(propId=30)
    
        public String getTaxFileNo(){
            return _taxFileNo;
        }

        public void setTaxFileNo(String value){
            this._taxFileNo = value;
        }


        private Long _userAccountId;

    
        @PropMeta(propId=31)
    
        public Long getUserAccountId(){
            return _userAccountId;
        }

        public void setUserAccountId(Long value){
            this._userAccountId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=32)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _remark;

    
        @PropMeta(propId=33)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=34)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=35)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=36)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=37)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=38)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=39)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _department;

        public Map<String,Object> getDepartment(){
            return _department;
        }

        public void setDepartment(Map<String,Object> value){
            this._department = value;
        }


        private Map<String,Object> _position;

        public Map<String,Object> getPosition(){
            return _position;
        }

        public void setPosition(Map<String,Object> value){
            this._position = value;
        }


        private Map<String,Object> _superior;

        public Map<String,Object> getSuperior(){
            return _superior;
        }

        public void setSuperior(Map<String,Object> value){
            this._superior = value;
        }


        private Map<String,Object> _costCenter;

        public Map<String,Object> getCostCenter(){
            return _costCenter;
        }

        public void setCostCenter(Map<String,Object> value){
            this._costCenter = value;
        }


        private Map<String,Object> _bankAccount;

        public Map<String,Object> getBankAccount(){
            return _bankAccount;
        }

        public void setBankAccount(Map<String,Object> value){
            this._bankAccount = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
