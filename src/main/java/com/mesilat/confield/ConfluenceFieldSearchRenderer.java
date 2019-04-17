package com.mesilat.confield;

import com.atlassian.jira.issue.customfields.CustomFieldValueProvider;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.search.ClauseNames;
import com.atlassian.jira.plugin.customfield.CustomFieldSearcherModuleDescriptor;
import com.atlassian.jira.web.FieldVisibilityManager;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;

import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfluenceFieldSearchRenderer extends AbstractFieldSearchRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("com.mesilat.confluence-fields");

    private final DataService dataService;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected Set<ValueOption> getValidOptions() {
        Set<ValueOption> options = new HashSet<>();
        try {
            DataServiceResult result = dataService.findPages(getField().getIdAsLong(), "", Integer.MAX_VALUE);
            ObjectNode obj = (ObjectNode)mapper.readTree(result.getText());
            if (obj.has("results") && obj.get("results").isArray()){
                ArrayNode arr = (ArrayNode)obj.get("results");
                arr.forEach(n -> {
                    if (n instanceof ObjectNode){
                        options.add(new ValueOption((ObjectNode)n));
                    }
                });
            }
        } catch (DataServiceException | IOException ex) {
            LOGGER.error("Failed to get valid options", ex);
        }
        return options;
    }

    public ConfluenceFieldSearchRenderer(
            ClauseNames clauseNames,
            CustomFieldSearcherModuleDescriptor customFieldSearcherModuleDescriptor,
            CustomField field,
            CustomFieldValueProvider customFieldValueProvider,
            FieldVisibilityManager fieldVisibilityManager,
            DataService dataService
    ) {
        super(clauseNames, customFieldSearcherModuleDescriptor, field, customFieldValueProvider, fieldVisibilityManager);
        this.dataService = dataService;
    }
}