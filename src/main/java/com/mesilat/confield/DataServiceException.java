package com.mesilat.confield;

import javax.ws.rs.core.Response.Status;

public class DataServiceException extends Exception {
    private final Status status;

    public Status getStatus(){
        return status;
    }

    public DataServiceException(Status status, String message){
        super(message);
        this.status = status;
    }
    public DataServiceException(Status status, String message, Throwable cause){
        super(message, cause);
        this.status = status;
    }
}