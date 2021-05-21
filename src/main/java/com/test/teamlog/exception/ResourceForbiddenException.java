package com.test.teamlog.exception;

import com.test.teamlog.payload.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class ResourceForbiddenException extends RuntimeException {
    private ApiResponse apiResponse;

    public ResourceForbiddenException(String message) {
        super();
        setApiResponse(message);
    }

    public ApiResponse getApiResponse() {
        return apiResponse;
    }

    private void setApiResponse(String message) {
        apiResponse = new ApiResponse(Boolean.FALSE, message);
    }
}
