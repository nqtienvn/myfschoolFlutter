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
    private final StudentScoreRepository scores; private final SemesterRepository semesters;
    private final AcademicYearSubjectRepository yearSubjects;
    private final AcademicYearGradeConfigItemRepository configItems;

    @Override public StudentTranscriptDto getMine(Long yearId,Long semesterId) {
        Student student=students.findByUserId(SecurityUtil.getCurrentUserId())
            .orElseThrow(()->new ResourceNotFoundException("Student","userId",SecurityUtil.getCurrentUserId()));
        return get(student.getId(),yearId,semesterId);
    }

    @Override public StudentTranscriptDto get(Long studentId,Long yearId,Long semesterId) {
        Student student=students.findById(studentId).orElseThrow(()->new ResourceNotFoundException("Student","id",studentId));
        authorize(studentId);
        Semester semester=semesters.findById(semesterId).orElseThrow(()->new ResourceNotFoundException("Semester","id",semesterId));
        if(!semester.getAcademicYear().getId().equals(yearId))throw new BadRequestException("Học kỳ không thuộc năm học đã chọn");
        Enrollment enrollment=enrollments.findByStudentIdAndAcademicYearIdAndStatus(studentId,yearId,EnrollmentStatus.ACTIVE)
            .orElseThrow(()->new ResourceNotFoundException("Enrollment","academicYearId",yearId));
        Map<Long,GradeBook> booksBySubject=new HashMap<>();
        for(GradeBook book:books.findByClsIdAndSemesterId(enrollment.getCls().getId(),semesterId))booksBySubject.put(book.getSubject().getId(),book);
        Map<Long,StudentScore> scoresByItem=new HashMap<>();
        for(StudentScore score:scores.findByStudentIdAndGradeItemGradeBookSemesterId(studentId,semesterId))scoresByItem.put(score.getGradeItem().getId(),score);
        List<ConfiguredColumn> columns=configuredColumns(yearId);
        List<Subject> appliedSubjects=yearSubjects.findByAcademicYearId(yearId).stream().map(AcademicYearSubject::getSubject)
            .sorted(Comparator.comparing(Subject::getName,String.CASE_INSENSITIVE_ORDER).thenComparing(Subject::getId)).toList();
        List<TranscriptSubjectDto> subjects=new ArrayList<>();
        for(Subject subject:appliedSubjects) {
            GradeBook book=booksBySubject.get(subject.getId());
            Map<String,GradeItem> bookItems=new HashMap<>();
            Map<Long,List<GradeItem>> bookItemsByConfig=new HashMap<>();
            if(book!=null)for(GradeItem item:items.findByGradeBookIdOrderByOrderAsc(book.getId())){
                bookItems.put(item.getCode(),item);
                if(item.getConfigItem()!=null)bookItemsByConfig
                    .computeIfAbsent(item.getConfigItem().getId(),ignored->new ArrayList<>()).add(item);
            }
            List<TranscriptScoreDto> values=new ArrayList<>(); BigDecimal sum=BigDecimal.ZERO; int totalWeight=0; boolean complete=true;
            for(ConfiguredColumn column:columns) {
                List<GradeItem> linkedItems=bookItemsByConfig.get(column.configItemId());
                GradeItem item=linkedItems!=null&&linkedItems.size()>=column.instanceIndex()
                    ?linkedItems.get(column.instanceIndex()-1):bookItems.get(column.code());
                StudentScore score=item==null?null:scoresByItem.get(item.getId());
                boolean graded=score!=null&&Boolean.TRUE.equals(score.getIsGraded());
                BigDecimal value=graded?score.getScore():null;
                String comment=graded?score.getComment():null;
                values.add(new TranscriptScoreDto(item==null?null:item.getId(),column.code(),column.name(),column.weight(),column.assessmentType(),value,comment,graded));
                boolean hasValue=graded&&(column.assessmentType()==AssessmentType.SCORE?value!=null:comment!=null&&!comment.isBlank());
                if(column.required()&&!hasValue)complete=false;
                if(value!=null&&column.assessmentType()==AssessmentType.SCORE){sum=sum.add(value.multiply(BigDecimal.valueOf(column.weight())));totalWeight+=column.weight();}
            }
            BigDecimal average=totalWeight==0?null:sum.divide(BigDecimal.valueOf(totalWeight),1,RoundingMode.HALF_UP);
            subjects.add(new TranscriptSubjectDto(subject.getId(),subject.getName(),values,average,complete));
        }
        return new StudentTranscriptDto(studentId,student.getUser().getName(),yearId,semesterId,subjects);
    }

    private List<ConfiguredColumn> configuredColumns(Long yearId){
        List<ConfiguredColumn> columns=new ArrayList<>();
        for(AcademicYearGradeConfigItem config:configItems.findByConfigAcademicYearIdOrderByDisplayOrderAsc(yearId)){
            for(int index=1;index<=config.getQuantity();index++)columns.add(new ConfiguredColumn(
                config.getId(),index,config.getCode()+"_"+index,
                config.getDisplayName()+(config.getQuantity()>1?" "+index:""),
                config.getWeight(),config.getAssessmentType(),Boolean.TRUE.equals(config.getRequiredEntry())));
        }
        return columns;
    }

    private record ConfiguredColumn(Long configItemId,int instanceIndex,String code,String name,Integer weight,AssessmentType assessmentType,boolean required){}

    private void authorize(Long studentId) {
        UserRole role=SecurityUtil.getCurrentUserRole(); Long userId=SecurityUtil.getCurrentUserId();
        if(role==UserRole.STUDENT&&students.findByUserId(userId).map(Student::getId).filter(studentId::equals).isPresent()) return;
        if(role==UserRole.PARENT){Parent parent=parents.findByUserId(userId).orElseThrow(()->new UnauthorizedException("Không tìm thấy hồ sơ phụ huynh"));if(guardians.existsByStudentIdAndGuardianId(studentId,parent.getId()))return;}
        throw new UnauthorizedException("Không có quyền xem bảng điểm học sinh này");
    }
}
