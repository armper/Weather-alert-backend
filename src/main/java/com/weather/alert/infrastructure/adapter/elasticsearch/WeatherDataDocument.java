package com.weather.alert.infrastructure.adapter.elasticsearch;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Document(indexName = "weather-data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeatherDataDocument {
    
    @Id
    private String id;
    
    @Field(type = FieldType.Text)
    private String location;
    
    @Field(type = FieldType.Double)
    private Double latitude;
    
    @Field(type = FieldType.Double)
    private Double longitude;
    
    @Field(type = FieldType.Keyword)
    private String eventType;
    
    @Field(type = FieldType.Keyword)
    private String severity;
    
    @Field(type = FieldType.Text)
    private String headline;
    
    @Field(type = FieldType.Text)
    private String description;
    
    @Field(type = FieldType.Date)
    private Instant onset;
    
    @Field(type = FieldType.Date)
    private Instant expires;
    
    @Field(type = FieldType.Keyword)
    private String status;
    
    @Field(type = FieldType.Date)
    private Instant timestamp;
}
