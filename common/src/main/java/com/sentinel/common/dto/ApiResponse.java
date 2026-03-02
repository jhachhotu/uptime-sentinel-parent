package com.sentinel.common.dto;


import  lombok.*;
import java.time.LocalDateTime;

import java.time.LocalDateTime;
@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class ApiResponse <T>{
    private  String status;
    private  String  message;
    private  T data;
    private LocalDateTime timestamp;



}
