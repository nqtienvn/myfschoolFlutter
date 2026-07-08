package vn.edu.fpt.myfschool.service;

import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.BillStatus;

import java.util.List;

public interface TuitionBillService {
    TuitionBillDto createTuitionBill(TuitionBillRequest request);

    List<TuitionBillDto> getStudentBills(Long studentId);

    List<TuitionBillDto> getClassBills(Long classId, Long semesterId, BillStatus status);

    void deleteTuitionBill(Long billId);
}
