package com.mesilat.confield;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.jql.operator.OperatorClasses;
import com.atlassian.jira.jql.validator.ClauseValidator;
import com.atlassian.jira.jql.validator.SupportedOperatorsValidator;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.MultiValueOperand;
import com.atlassian.query.operand.SingleValueOperand;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

public class ConfluenceFieldValidator implements ClauseValidator {
    private final SupportedOperatorsValidator supportedOperatorsValidator;
    private final CustomField field;
    private final DataService dataService;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public MessageSet validate(final ApplicationUser searcher, final TerminalClause terminalClause) {
        I18nHelper i18n = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
        MessageSet errors = supportedOperatorsValidator.validate(searcher, terminalClause);
        if (!errors.hasAnyErrors()) {
            if (terminalClause.getOperand() instanceof SingleValueOperand){
                SingleValueOperand svop = (SingleValueOperand)terminalClause.getOperand();
                String title = svop.getStringValue();
                if (!"-1".equals(title)){
                    try {
                        DataServiceResult result = dataService.getPage(field.getIdAsLong(), title);
                        if (result.getStatus() == DataService.S_OK){
                            ObjectNode obj = (ObjectNode)mapper.readTree(result.getText());
                            if (obj.has("results") && obj.get("results").isArray()){
                                ArrayNode arr = (ArrayNode)obj.get("results");
                                if (arr.size() > 0){
                                    return errors;
                                }
                            }
                        }
                        errors.addErrorMessage(i18n.getText("com.mesilat.confluence-field.err.pageNotFound", title));
                    } catch(Throwable ex){
                        errors.addErrorMessage(ex.getMessage());
                    }
                }
            } else if (terminalClause.getOperand() instanceof MultiValueOperand){
                MultiValueOperand op = (MultiValueOperand)terminalClause.getOperand();
                List<String> titles = new ArrayList<>();
                op.getValues().forEach(v -> {
                    if (v instanceof SingleValueOperand){
                        SingleValueOperand svop = (SingleValueOperand)v;
                        String title = svop.getStringValue();
                        titles.add(title);
                    } else {
                        errors.addErrorMessage(i18n.getText("com.mesilat.confluence-field.err.invalidValueInList", v.getClass().getName()));
                    }
                });
                Map<String, ObjectNode> pages = new HashMap<>();
                try {
                    DataServiceResult result = dataService.getPagesByTitle(field.getIdAsLong(), titles);
                    if (result.getStatus() == DataService.S_OK){
                        ObjectNode obj = (ObjectNode)mapper.readTree(result.getText());
                        if (obj.has("results") && obj.get("results").isArray()){
                            ArrayNode arr = (ArrayNode)obj.get("results");
                            arr.forEach(n -> {
                                if (n instanceof ObjectNode && n.has("title") && n.get("title").isTextual()){
                                    ObjectNode _n = (ObjectNode)n;
                                    pages.put(_n.get("title").asText(), _n);
                                }
                            });
                        }
                    }
                    if (pages.isEmpty()){
                        errors.addErrorMessage(i18n.getText("com.mesilat.confluence-field.err.noPagesFound"));
                    } else {
                        titles.forEach(title -> {
                            if (!pages.containsKey(title) && !"-1".equals(title)){
                                errors.addErrorMessage(i18n.getText("com.mesilat.confluence-field.err.pageNotFound", title));
                            }
                        });
                    }
                } catch(Throwable ex){
                    errors.addErrorMessage(ex.getMessage());
                }
            }
        }
        return errors;
    }

    public ConfluenceFieldValidator(CustomField field, DataService dataService){
        this.supportedOperatorsValidator = new SupportedOperatorsValidator(OperatorClasses.EQUALITY_OPERATORS_WITH_EMPTY);
        this.field = field;
        this.dataService = dataService;
    }
}