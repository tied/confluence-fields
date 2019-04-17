package com.mesilat.confield;

import net.java.ao.Preload;
import net.java.ao.RawEntity;
import net.java.ao.schema.NotNull;
import net.java.ao.schema.PrimaryKey;
import net.java.ao.schema.StringLength;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@Preload
@JsonIgnoreProperties({"entityManager", "entityProxy", "entityType"})
@JsonAutoDetect
public interface FieldSettings extends RawEntity<Long>  {
    @NotNull
    @PrimaryKey(value = "ID")
    Long getId();

    @StringLength(36)
    String getConfluenceId();
    void setConfluenceId(String confluenceId);

    @StringLength(StringLength.UNLIMITED)
    String getFilter();
    void setFilter(String filter);
}