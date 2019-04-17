package com.mesilat.confield;

import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.jql.values.ClauseValuesGenerator;
import com.atlassian.jira.jql.values.ClauseValuesGenerator.Result;
import com.atlassian.jira.jql.values.ClauseValuesGenerator.Results;
import com.atlassian.jira.user.ApplicationUser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfluencePageValuesGenerator implements ClauseValuesGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger("com.mesilat.confluence-fields");

    private final CustomField field;
    private final DataService dataService;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Results getPossibleValues(final ApplicationUser searcher, final String jqlClauseName, final String valuePrefix, final int maxNumResults) {
        List<Result> pages = new ArrayList<>();
        try {
            DataServiceResult result = dataService.findPages(field.getIdAsLong(), valuePrefix, maxNumResults);
            if (result.getStatus() == DataService.S_OK){
                ObjectNode obj = (ObjectNode)mapper.readTree(result.getText());
                if (obj.has("results") && obj.get("results").isArray()){
                    ArrayNode arr = (ArrayNode)obj.get("results");
                    arr.forEach(n -> {
                        ObjectNode _n = (ObjectNode)n;
                        Result page = new Result(_n.get("title").asText(), _n.get("title").asText());
                        pages.add(page);
                    });
                }
            }
        } catch (DataServiceException | IOException ex) {
            LOGGER.warn(String.format("Failed to get possible values for field: %d", field.getIdAsLong()), ex);
        }
        return new Results(pages);
    }

    public ConfluencePageValuesGenerator(CustomField field, DataService dataService) {
        this.field = field;
        this.dataService = dataService;
    }
}