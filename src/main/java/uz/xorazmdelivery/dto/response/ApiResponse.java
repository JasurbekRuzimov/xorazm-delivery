package uz.xorazmdelivery.dto.response;
import lombok.*;
@Data @Builder @AllArgsConstructor @NoArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    public static <T> ApiResponse<T> ok(T data) { return new ApiResponse<>(true,"OK",data); }
    public static <T> ApiResponse<T> ok(String message, T data) { return new ApiResponse<>(true,message,data); }
    public static ApiResponse<Void> ok(String message) { return new ApiResponse<>(true,message,null); }
}
