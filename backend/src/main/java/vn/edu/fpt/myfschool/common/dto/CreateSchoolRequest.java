package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSchoolRequest(
    @NotBlank(message = "Tên trường không được trống")
    @Size(max = 100, message = "Tên trường tối đa 100 ký tự")
    String name,

    @NotBlank(message = "Mã trường không được trống")
    @Size(max = 20, message = "Mã trường tối đa 20 ký tự")
    String code,

    @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
    String address,

    @Size(max = 20, message = "Số điện thoại tối đa 20 ký tự")
    String phone,

    @NotBlank(message = "Tên hiển thị không được trống")
    @Size(max = 200, message = "Tên hiển thị tối đa 200 ký tự")
    String schoolName
) {}
