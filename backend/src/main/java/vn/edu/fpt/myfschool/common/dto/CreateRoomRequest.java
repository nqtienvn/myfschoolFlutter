package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateRoomRequest(
    @NotBlank(message = "Tên phòng không được trống")
    @Size(max = 20, message = "Tên phòng tối đa 20 ký tự")
    String name,

    @NotNull(message = "Sức chứa không được trống")
    @Min(value = 1, message = "Sức chứa tối thiểu là 1")
    Integer capacity,

    @Size(max = 20, message = "Tên tòa nhà tối đa 20 ký tự")
    String building,

    @Size(max = 100, message = "Mô tả thiết bị tối đa 100 ký tự")
    String equipment,

    Boolean isActive
) {}
