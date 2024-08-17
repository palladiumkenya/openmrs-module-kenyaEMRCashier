package org.openmrs.module.kenyaemr.cashier.api.model;

import org.openmrs.BaseOpenmrsData;

public class InsuranceBenefitsPackageService extends BaseOpenmrsData {

    public static final long serialVersionUID = 0L;
    private int benefitId;
    private String interventionName;
    private String shaCode;
    private String description;
    private String interventionType;
    private Boolean requiresPreAuth;
    private String benefitDistribution;
    private Integer tariff;
    private Integer lowerAgeLimit;
    private Integer upperAgeLimit;
    private Boolean needsReferral;
    private String packageName;
    private String packageCode;
    private Integer insurer;
    private String gender;
    private String accessPoint;
    private String subCategory;

    public int getBenefitId() {
        return benefitId;
    }

    public void setBenefitId(int benefitId) {
        this.benefitId = benefitId;
    }

    public String getShaCode() {
        return shaCode;
    }

    public void setShaCode(String shaCode) {
        this.shaCode = shaCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getInterventionType() {
        return interventionType;
    }

    public void setInterventionType(String interventionType) {
        this.interventionType = interventionType;
    }

    public Boolean getRequiresPreAuth() {
        return requiresPreAuth;
    }

    public void setRequiresPreAuth(Boolean requiresPreAuth) {
        this.requiresPreAuth = requiresPreAuth;
    }

    public String getBenefitDistribution() {
        return benefitDistribution;
    }

    public void setBenefitDistribution(String benefitDistribution) {
        this.benefitDistribution = benefitDistribution;
    }

    public Integer getTariff() {
        return tariff;
    }

    public void setTariff(Integer tariff) {
        this.tariff = tariff;
    }

    @Override
    public Integer getId() {
        return getBenefitId();
    }

    @Override
    public void setId(Integer integer) {
        setBenefitId(integer);
    }


    public Integer getLowerAgeLimit() {
        return lowerAgeLimit;
    }

    public void setLowerAgeLimit(Integer lowerAgeLimit) {
        this.lowerAgeLimit = lowerAgeLimit;
    }

    public Integer getUpperAgeLimit() {
        return upperAgeLimit;
    }

    public void setUpperAgeLimit(Integer upperAgeLimit) {
        this.upperAgeLimit = upperAgeLimit;
    }

    public Boolean getNeedsReferral() {
        return needsReferral;
    }

    public void setNeedsReferral(Boolean needsReferral) {
        this.needsReferral = needsReferral;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageCode() {
        return packageCode;
    }

    public void setPackageCode(String packageCode) {
        this.packageCode = packageCode;
    }

    public Integer getInsurer() {
        return insurer;
    }

    public void setInsurer(Integer insurer) {
        this.insurer = insurer;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getAccessPoint() {
        return accessPoint;
    }

    public void setAccessPoint(String accessPoint) {
        this.accessPoint = accessPoint;
    }

    public String getSubCategory() {
        return subCategory;
    }

    public void setSubCategory(String subCategory) {
        this.subCategory = subCategory;
    }

    public String getInterventionName() {
        return interventionName;
    }

    public void setInterventionName(String interventionName) {
        this.interventionName = interventionName;
    }

    
}
