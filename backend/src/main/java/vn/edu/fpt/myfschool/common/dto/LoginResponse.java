package vn.edu.fpt.myfschool.common.dto;

public record LoginResponse(
    String token,
    String tokenType,
    Long expiresIn,
    UserDto user
) {}
