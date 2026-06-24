package vn.edu.fpt.myfschool.common.dto;

import vn.edu.fpt.myfschool.common.enums.LeaveStatus;

public record ReviewLeaveRequestRequest(LeaveStatus status, String response) {}
