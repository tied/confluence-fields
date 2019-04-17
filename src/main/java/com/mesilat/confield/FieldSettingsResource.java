package com.mesilat.confield;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import java.io.IOException;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.java.ao.DBParam;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/settings")
@Scanned
public class FieldSettingsResource {
    private static final Logger LOGGER = LoggerFactory.getLogger("com.mesilat.confluence-fields");

    @ComponentImport
    private final ActiveObjects ao;
    private final DataService dataService;
    private final ObjectMapper mapper = new ObjectMapper();

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response get(@PathParam("id") Long id){
        LOGGER.debug(String.format("Get field settings by id: %d", id));
        FieldSettings fs = ao.get(FieldSettings.class, id);
        if (fs == null){
            return Response.ok(mapper.createObjectNode()).build();
        } else {
            return Response.ok(fs).build();
        }
    }

    @POST
    @Path("/")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response post(ObjectNode fieldSettings){
        try {
            LOGGER.debug(String.format("Post field settings: %s", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(fieldSettings)));
        } catch (IOException ignore) {
        }

        FieldSettings fs = ao.get(FieldSettings.class, fieldSettings.get("id").asLong());
        if (fs == null){
            fs = ao.create(FieldSettings.class, new DBParam("ID", fieldSettings.get("id").asLong()));
        }
        fs.setConfluenceId(fieldSettings.get("confluenceId").asText());
        fs.setFilter(fieldSettings.get("filter").asText());
        fs.save();
        dataService.clearConfluenceBaseUrls();
        return Response.status(Response.Status.ACCEPTED).build();
    }

    @Inject
    public FieldSettingsResource(ActiveObjects ao, DataService dataService){
        this.ao = ao;
        this.dataService = dataService;
    }
}