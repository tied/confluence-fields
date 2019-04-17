package com.mesilat.confield;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.index.indexers.impl.AbstractCustomFieldIndexer;
import com.atlassian.jira.web.FieldVisibilityManager;
import java.io.IOException;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfluenceFieldIndexer extends AbstractCustomFieldIndexer {
    private static final Logger LOGGER = LoggerFactory.getLogger("com.mesilat.confluence-fields");

    @Override
    public void addDocumentFieldsSearchable(Document doc, Issue issue) {
        addDocumentFields(doc, issue, Field.Index.NOT_ANALYZED_NO_NORMS);
    }
    @Override
    public void addDocumentFieldsNotSearchable(Document doc, Issue issue) {
        addDocumentFields(doc, issue, Field.Index.NO);
    }
    
    private void addDocumentFields(Document doc, Issue issue, Field.Index indexType) {
        Object value = customField.getValue(issue);
        
        if (value != null){
            ObjectMapper mapper = new ObjectMapper();
            try {
                ArrayNode arr = (ArrayNode)mapper.readTree(String.format("[%s]", value));
                arr.forEach(n -> {
                    if (n.isObject()){
                        ObjectNode obj = (ObjectNode)n;
                        if (!obj.has("id")){
                            return;
                        }
                        if (obj.get("id").isNumber()){
                            Long id = obj.get("id").asLong();
                            doc.add(new Field(getDocumentFieldId(), Long.toString(id), Field.Store.YES, indexType));
                            LOGGER.debug(String.format("Indexed issue %d with value %d", issue.getId(), id));
                        } else if (obj.get("id").isTextual()){
                            String id = obj.get("id").asText();                            
                            doc.add(new Field(getDocumentFieldId(), id, Field.Store.YES, indexType));
                            LOGGER.debug(String.format("Indexed issue %d with value %s", issue.getId(), id));
                        }
                    }
                });
            } catch (IOException ex) {
                LOGGER.error(String.format("Failed to parse value %s", value), ex);
            }
        }
    }

    public ConfluenceFieldIndexer(FieldVisibilityManager fieldVisibilityManager, CustomField customField){
        super(fieldVisibilityManager, customField);

        //this.customField = customField;
    }
}