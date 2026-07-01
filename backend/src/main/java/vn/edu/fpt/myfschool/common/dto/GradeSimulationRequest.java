package vn.edu.fpt.myfschool.common.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record GradeSimulationRequest(Long studentId, @NotNull Long semesterId, List<SimulationEntry> simulations) {}
