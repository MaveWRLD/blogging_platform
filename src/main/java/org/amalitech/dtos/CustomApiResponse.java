package org.amalitech.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
public class CustomApiResponse<T> {
    private HttpStatus status;
    private String message;
    private T data;

    public static <T> CustomApiResponse<T> success(T data) {
        return new CustomApiResponse<>(HttpStatus.OK, null, data);
    }

    public static <T> CustomApiResponse<T> success(String message, T data) {
        return new CustomApiResponse<>(HttpStatus.OK, message, data);
    }

    public static <T> CustomApiResponse<T> success(HttpStatus status, String message, T data) {
        return new CustomApiResponse<>(status, message, data);
    }

    public static <T> CustomApiResponse<T> error(String message) {
        return new CustomApiResponse<>(HttpStatus.BAD_REQUEST, message, null);
    }
}