package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.BillStatus;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.*;
import vn.edu.fpt.myfschool.repository.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public interface TuitionBillService {
    TuitionBillDto createTuitionBill(TuitionBillRequest request);

    List<TuitionBillDto> getStudentBills(Long studentId);

    List<TuitionBillDto> getClassBills(Long classId, Long semesterId, BillStatus status);

    void deleteTuitionBill(Long billId);
}
