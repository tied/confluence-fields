package com.mesilat.confield;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.impl.GenericTextCFType;
import com.atlassian.jira.issue.customfields.manager.GenericConfigManager;
import com.atlassian.jira.issue.customfields.persistence.CustomFieldValuePersister;
import com.atlassian.jira.issue.customfields.persistence.PersistenceFieldType;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.TextFieldCharacterLengthValidator;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.webresource.api.assembler.PageBuilderService;
import java.util.Map;
import javax.annotation.Nonnull;

public abstract class FieldBase extends GenericTextCFType {
    private static final String COMMA_REPLACEMENT_PATTERN = "~[$]~";
    private final PageBuilderService pageBuilderService;
    private final DataService dataService;

    public abstract String getFieldType();
    
    @Override
    public Map<String, Object> getVelocityParameters(Issue issue, CustomField field, FieldLayoutItem fieldLayoutItem) {
        //this.pageBuilderService.assembler().resources().requireContext("confluence-fields");
        Map parameters = super.getVelocityParameters(issue, field, fieldLayoutItem);
        parameters.put("templateHelper", new TemplateHelper(issue, field, fieldLayoutItem, dataService));
        parameters.put("fieldType", getFieldType());
        return parameters;
    }
    @Nonnull
    @Override
    protected PersistenceFieldType getDatabaseType(){
        return PersistenceFieldType.TYPE_UNLIMITED_TEXT;
    }
    @Override
    protected Object getDbValueFromObject(String customFieldObject) {
        if (customFieldObject != null){
            return super.getDbValueFromObject(customFieldObject.replace(COMMA_REPLACEMENT_PATTERN, ","));
        } else {
            return super.getDbValueFromObject(customFieldObject);
        }
    }

    public FieldBase(
        CustomFieldValuePersister customFieldValuePersister,
        GenericConfigManager genericConfigManager,
        TextFieldCharacterLengthValidator textFieldCharacterLengthValidator,
        JiraAuthenticationContext jiraAuthenticationContext,
        PageBuilderService pageBuilderService,
        DataService dataService
    ){
        super(customFieldValuePersister, genericConfigManager, textFieldCharacterLengthValidator, jiraAuthenticationContext);
        this.pageBuilderService = pageBuilderService;
        this.dataService = dataService;
    }
}