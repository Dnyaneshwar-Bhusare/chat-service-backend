package com.codingworld.service1.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Response {
    private String status;
    private String message;
    private Object data;

    @Override
    public String toString() {
        return "Response{" +
                "status='" + status + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
