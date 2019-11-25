package com.expedia.adaptivealerting.modelservice.exception;

import lombok.Data;
import lombok.Generated;

import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

@Generated
@XmlRootElement(name = "error")
@Data
public class ErrorResponse {

    private String message;
    private List<String> details;

    public ErrorResponse(String message, List<String> details) {
        super();
        this.message = message;
        this.details = details;
    }

}