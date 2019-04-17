package com.mesilat.confield;

import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.util.IndexValueConverter;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfluenceFieldValueConverter implements IndexValueConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger("com.mesilat.confluence-fields");

    private final long fieldId;
    private final DataService dataService;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String convertToIndexValue(QueryLiteral rawValue) {
        if (rawValue == null){
            return null;
        }

        try {
            DataServiceResult result = dataService.getPage(fieldId, rawValue.asString());
            if (result.getStatus() != DataService.S_OK){
                return null;
            }
            ObjectNode obj = (ObjectNode)mapper.readTree(result.getText());
            if (obj.has("results") && obj.get("results").isArray()){
                ArrayNode arr = (ArrayNode)obj.get("results");
                if (arr.size() == 0){
                    return null;
                }
                ObjectNode o = (ObjectNode)arr.get(0);
                return o.get("id").asText();
            }
        } catch(Throwable ex){
            LOGGER.debug(String.format("Error getting page for %s", rawValue.asString()), ex);
        }

        return rawValue.asString();
    }

    public ConfluenceFieldValueConverter(long fieldId, DataService dataService){
        this.fieldId = fieldId;
        this.dataService = dataService;
    }
}