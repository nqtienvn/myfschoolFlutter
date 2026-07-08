package vn.edu.fpt.myfschool.common.dto;

public record RoomDto(
    Long id,
    String name,
    Integer capacity,
    String building,
    String equipment,
    Boolean isActive
) {}
