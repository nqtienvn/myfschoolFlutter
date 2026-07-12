package vn.edu.fpt.myfschool.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.*;
import vn.edu.fpt.myfschool.common.exception.*;
import vn.edu.fpt.myfschool.common.util.SecurityUtil;
import vn.edu.fpt.myfschool.entity.*;
import vn.edu.fpt.myfschool.repository.*;
import vn.edu.fpt.myfschool.service.TranscriptService;
import java.math.*;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class TranscriptServiceImpl implements TranscriptService {
    private final StudentRepository students; private final ParentRepository parents;
    private final StudentGuardianRepository guardians; private final EnrollmentRepository enrollments;
    private final GradeBookRepository books; private final GradeItemRepository items;
    private final StudentScoreRepository scores;

    @Override public StudentTranscriptDto getMine(Long yearId,Long semesterId) {
        Student student=students.findByUserId(SecurityUtil.getCurrentUserId())
            .orElseThrow(()->new ResourceNotFoundException("Student","userId",SecurityUtil.getCurrentUserId()));
        return get(student.getId(),yearId,semesterId);
    }

    @Override public StudentTranscriptDto get(Long studentId,Long yearId,Long semesterId) {
        Student student=students.findById(studentId).orElseThrow(()->new ResourceNotFoundException("Student","id",studentId));
        authorize(studentId);
        Enrollment enrollment=enrollments.findByStudentIdAndAcademicYearIdAndStatus(studentId,yearId,EnrollmentStatus.ACTIVE)
            .orElseThrow(()->new ResourceNotFoundException("Enrollment","academicYearId",yearId));
        List<TranscriptSubjectDto> subjects=new ArrayList<>();
        for(GradeBook book:books.findByClsIdAndSemesterId(enrollment.getCls().getId(),semesterId)) {
            if(book.getStatus()!=GradeBookStatus.PUBLISHED&&book.getStatus()!=GradeBookStatus.LOCKED) continue;
            List<TranscriptScoreDto> values=new ArrayList<>(); BigDecimal sum=BigDecimal.ZERO; int totalWeight=0; boolean complete=true;
            for(GradeItem item:items.findByGradeBookIdOrderByOrderAsc(book.getId())) {
                StudentScore score=scores.findByGradeItemIdAndStudentId(item.getId(),studentId).orElse(null);
                BigDecimal value=score==null?null:score.getScore();
                values.add(new TranscriptScoreDto(item.getId(),item.getCode(),item.getName(),item.getWeight(),value));
                if(Boolean.TRUE.equals(item.getRequiredEntry())&&value==null) complete=false;
                if(value!=null&&item.getAssessmentType()==AssessmentType.SCORE){sum=sum.add(value.multiply(BigDecimal.valueOf(item.getWeight())));totalWeight+=item.getWeight();}
            }
            BigDecimal average=totalWeight==0?null:sum.divide(BigDecimal.valueOf(totalWeight),1,RoundingMode.HALF_UP);
            subjects.add(new TranscriptSubjectDto(book.getSubject().getId(),book.getSubject().getName(),values,average,complete));
        }
        return new StudentTranscriptDto(studentId,student.getUser().getName(),yearId,semesterId,subjects);
    }

    private void authorize(Long studentId) {
        UserRole role=SecurityUtil.getCurrentUserRole(); Long userId=SecurityUtil.getCurrentUserId();
        if(role==UserRole.STUDENT&&students.findByUserId(userId).map(Student::getId).filter(studentId::equals).isPresent()) return;
        if(role==UserRole.PARENT){Parent parent=parents.findByUserId(userId).orElseThrow(()->new UnauthorizedException("Không tìm thấy hồ sơ phụ huynh"));if(guardians.existsByStudentIdAndGuardianId(studentId,parent.getId()))return;}
        throw new UnauthorizedException("Không có quyền xem bảng điểm học sinh này");
    }
}
