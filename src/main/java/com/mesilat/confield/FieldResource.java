package com.mesilat.confield;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.util.List;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/field")
@Scanned
public class FieldResource {
    public static final int DEFAULT_LIMIT = 25;
    private static final Logger LOGGER = LoggerFactory.getLogger("com.mesilat.confluence-fields");

    @ComponentImport
    private final CustomFieldManager fieldManager;
    private final ObjectMapper mapper = new ObjectMapper();
    private final DataService dataService;

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response get(@PathParam("id") Long id){
        LOGGER.debug(String.format("Get field detail by id: %d", id));

        CustomField cf = fieldManager.getCustomFieldObject(id);
        if (cf == null){
            I18nHelper i18n = ComponentAccessor.getJiraAuthenticationContext().getI18nHelper();
            return Response
                .status(Response.Status.NOT_FOUND)
                .entity(i18n.getText("com.mesilat.confluence-field.err.fieldNotFound", id))
                .build();
        }

        ObjectNode field = mapper.createObjectNode();
        field.put("id", cf.getIdAsLong());
        field.put("name", cf.getFieldName());
        field.put("desc", cf.getDescription());

        ObjectNode type = mapper.createObjectNode();
        type.put("key", cf.getCustomFieldType().getKey());
        type.put("name", cf.getCustomFieldType().getName());
        field.put("type", type);

        return Response.ok(field).build();
    }

    @GET
    @Path("/{id}/pages")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response findPages(@PathParam("id") Long id, @QueryParam("q") String q,  @QueryParam("max-results") Integer limit){
        try {
            DataServiceResult result = dataService.findPages(id, q, limit == null? DEFAULT_LIMIT: limit);
            return Response.status(result.getStatus()).entity(result.getText()).build();
        } catch (DataServiceException ex) {
            LOGGER.debug(String.format("Error looking up pages for field %d", id), ex);
            return Response.status(ex.getStatus()).entity(ex.getMessage()).build();
        }
    }

    @GET
    @Path("/{id}/pages-by-id")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response getPagesById(@PathParam("id") Long id, @QueryParam("page-id") List<String> pageId){
        try {
            DataServiceResult result = dataService.getPagesById(id, pageId);
            return Response.status(result.getStatus()).entity(result.getText()).build();
        } catch (DataServiceException ex) {
            LOGGER.debug(String.format("Error getting pages for field %d", id), ex);
            return Response.status(ex.getStatus()).entity(ex.getMessage()).build();
        }
    }

    @GET
    @Path("/test")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response test(@QueryParam("confluence-id") String confluenceId, @QueryParam("filter") String filter, @QueryParam("q") String q,  @QueryParam("max-results") Integer limit){
        try {
            DataServiceResult result = dataService.test(confluenceId, filter, q, limit == null? DEFAULT_LIMIT: limit);
            return Response.status(result.getStatus()).entity(result.getText()).build();
        } catch (DataServiceException ex) {
            return Response.status(ex.getStatus()).entity(ex.getMessage()).build();
        }
    }

    @Inject
    public FieldResource(CustomFieldManager fieldManager, DataService dataService){
        this.fieldManager = fieldManager;
        this.dataService = dataService;
    }
}