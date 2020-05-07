package com.expedia.adaptivealerting.modelservice.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Detector entity
 */
@Data
@Accessors(chain = true)
@Document(indexName = "detectors_new", type = "detector")
//This automatically creates an index if it doesn't exist
public class Detector {

    @Id
    public String id;

    @Field(type = FieldType.Text)
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private UUID uuid;

    @NotNull
    @Field(type = FieldType.Text)
    private String type;

    @Field(type = FieldType.Boolean)
    private boolean enabled;

    @Field(type = FieldType.Boolean)
    private boolean trusted;

    @Field(type = FieldType.Object)
    private Map<String, Object> detectorConfig;

    @Field(type = FieldType.Object)
    private Meta meta;

    @Data
    public static class Meta {

        @Field(type = FieldType.Text)
        private String createdBy;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "yyyy-MM-dd HH:mm:ss")
        private Date dateLastAccessed;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
        @Field(type = FieldType.Date, format = DateFormat.custom, pattern = "yyyy-MM-dd HH:mm:ss")
        private Date dateLastUpdated;
    }

}
