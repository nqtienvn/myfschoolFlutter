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
import vn.edu.fpt.myfschool.service.GradeBookService;
import java.math.*;
import java.time.LocalDateTime;
import java.util.*;

@Service("gradeBookService") @RequiredArgsConstructor @Transactional
public class GradeBookServiceImpl implements GradeBookService {
    private final GradeBookRepository books; private final GradeItemRepository items; private final StudentScoreRepository scores;
    private final EnrollmentRepository enrollments; private final ClassRepository classes; private final SubjectRepository subjects;
    private final SemesterRepository semesters; private final StudentRepository students; private final TeacherRepository teachers;
    private final TeachingAssignmentRepository assignments; private final AcademicYearGradeConfigItemRepository configItems;
    private final AcademicYearSubjectRepository yearSubjects; private final UserRepository users; private final StudentScoreAuditRepository audits;

    @Override @Transactional(readOnly=true)
    public GradeBookDto getByClassSubjectSemester(Long classId,Long subjectId,Long semesterId){GradeBook b=requireBook(classId,subjectId,semesterId);authorizeRead(b);return dto(b);}

    @Override public GradeBookDto getOrCreate(Long classId,Long subjectId,Long semesterId){
        SchoolClass cls=classes.findById(classId).orElseThrow(()->new ResourceNotFoundException("Class","id",classId));
        Subject subject=subjects.findById(subjectId).orElseThrow(()->new ResourceNotFoundException("Subject","id",subjectId));
        Semester semester=semesters.findById(semesterId).orElseThrow(()->new ResourceNotFoundException("Semester","id",semesterId));
        Long yearId=cls.getAcademicYear().getId();
        if(!semester.getAcademicYear().getId().equals(yearId)) throw new BadRequestException("Học kỳ không thuộc năm học của lớp");
        if(!yearSubjects.existsByAcademicYearIdAndSubjectId(yearId,subjectId)) throw new BadRequestException("Môn học không được áp dụng trong năm học đã chọn");
        authorizeAssignment(cls,subject);
        GradeBook existing=books.findByClsIdAndSubjectIdAndSemesterId(classId,subjectId,semesterId).orElse(null);
        if(existing!=null){
            if(existing.getStatus()!=GradeBookStatus.LOCKED
                    && semester.getStatus()!=SemesterStatus.COMPLETED
                    && semester.getAcademicYear().getStatus()!=AcademicYearStatus.COMPLETED) {
                synchronizeGradeItems(existing);
            }
            return dto(existing);
        }
        requireEditable(semester);
        GradeBook book=createBook(cls,subject,semester);
        return dto(book);
    }

    private GradeBook createBook(SchoolClass cls,Subject subject,Semester semester){
        List<AcademicYearGradeConfigItem> configured=configItems.findByConfigAcademicYearIdOrderByDisplayOrderAsc(cls.getAcademicYear().getId());
        if(configured.isEmpty()) throw new ConflictException("Năm học chưa có cấu hình đầu điểm");
        GradeBook book=new GradeBook();book.setCls(cls);book.setSubject(subject);book.setSemester(semester);book=books.save(book);
        synchronizeGradeItems(book);
        return book;
    }

    /** Keeps a pre-existing grade book aligned with the active year configuration without losing saved scores. */
    private void synchronizeGradeItems(GradeBook book){
        List<AcademicYearGradeConfigItem> configured=configItems.findByConfigAcademicYearIdOrderByDisplayOrderAsc(
                book.getCls().getAcademicYear().getId());
        if(configured.isEmpty()) throw new ConflictException("Năm học chưa có cấu hình đầu điểm");

        List<GradeItem> existingItems=new ArrayList<>(items.findByGradeBookIdOrderByOrderAsc(book.getId()));
        Map<Long,List<GradeItem>> itemsByConfig=new HashMap<>();
        for(GradeItem item:existingItems) if(item.getConfigItem()!=null) {
            itemsByConfig.computeIfAbsent(item.getConfigItem().getId(),ignored->new ArrayList<>()).add(item);
        }
        itemsByConfig.values().forEach(group->group.sort(Comparator.comparing(GradeItem::getOrder,
                Comparator.nullsLast(Integer::compareTo)).thenComparing(GradeItem::getId)));
        int nextOrder=existingItems.stream().map(GradeItem::getOrder).filter(Objects::nonNull)
                .max(Integer::compareTo).orElse(-1)+1;

        for(AcademicYearGradeConfigItem config:configured) for(int occurrence=1;occurrence<=config.getQuantity();occurrence++){
            String code=config.getCode()+"_"+occurrence;
            String name=config.getDisplayName()+(config.getQuantity()>1?" "+occurrence:"");
            List<GradeItem> configuredItems=itemsByConfig.getOrDefault(config.getId(),List.of());
            GradeItem item=configuredItems.size()>=occurrence?configuredItems.get(occurrence-1):existingItems.stream()
                    .filter(candidate->candidate.getConfigItem()==null)
                    .filter(candidate->code.equals(candidate.getCode())||name.equals(candidate.getName()))
                    .findFirst().orElse(null);
            if(item==null){
                item=new GradeItem();item.setGradeBook(book);item.setOrder(nextOrder++);existingItems.add(item);
                itemsByConfig.computeIfAbsent(config.getId(),ignored->new ArrayList<>()).add(item);
            }
            item.setConfigItem(config);item.setCode(code);item.setName(name);item.setWeight(config.getWeight());
            item.setMaxScore(10);item.setEntryRole(config.getEntryRole());item.setAssessmentType(config.getAssessmentType());
            item.setRequiredEntry(config.getRequiredEntry());items.save(item);
        }
    }

    @Override public List<StudentScoreDto> updateScores(UpdateStudentScoreRequest request){
        GradeItem item=items.findById(request.gradeItemId()).orElseThrow(()->new ResourceNotFoundException("GradeItem","id",request.gradeItemId()));
        GradeBook book=item.getGradeBook(); if(book.getStatus()==GradeBookStatus.LOCKED) throw new ConflictException("Bảng điểm đã khóa");
        requireEditable(book.getSemester());
        authorizeEntry(item); User actor=users.findById(SecurityUtil.getCurrentUserId()).orElseThrow(()->new UnauthorizedException("Không tìm thấy tài khoản"));
        List<Long> allowed=enrollments.findActiveStudentsByClassAndYear(book.getCls().getId(),book.getCls().getAcademicYear().getId()).stream().map(Student::getId).toList();
        List<StudentScoreDto> result=new ArrayList<>();
        LocalDateTime submittedAt=LocalDateTime.now();
        for(UpdateScoreEntry entry:request.entries()){
            if(entry.studentId()==null) throw new BadRequestException("Thiếu học sinh cần nhập đầu điểm");
            if(!allowed.contains(entry.studentId())) throw new BadRequestException("Học sinh không thuộc lớp/năm học của bảng điểm");
            CanonicalAssessment value=canonicalAssessment(item.getAssessmentType(),entry);
            StudentScore score=scores.findByGradeItemIdAndStudentId(item.getId(),entry.studentId()).orElseGet(()->{StudentScore s=new StudentScore();s.setGradeItem(item);s.setStudent(students.findById(entry.studentId()).orElseThrow(()->new ResourceNotFoundException("Student","id",entry.studentId())));return s;});
            BigDecimal oldScore=score.getScore();String oldComment=score.getComment();Boolean oldIsGraded=score.getIsGraded();
            score.setScore(value.score());score.setIsGraded(value.graded());score.setNote(trimToNull(entry.note()));score.setIsCommentBased(item.getAssessmentType()!=AssessmentType.SCORE);score.setComment(value.comment());score.setEnteredBy(actor);
            boolean changed=!Objects.equals(oldScore,score.getScore())||!Objects.equals(oldComment,score.getComment())||!Objects.equals(oldIsGraded,score.getIsGraded());
            score.setPublishedAt(value.graded()?submittedAt:null);
            score=scores.save(score);
            if(changed){
                StudentScoreAudit audit=new StudentScoreAudit();audit.setStudentScore(score);audit.setOldScore(oldScore);audit.setNewScore(score.getScore());audit.setOldComment(oldComment);audit.setNewComment(score.getComment());audit.setOldIsGraded(oldIsGraded);audit.setNewIsGraded(score.getIsGraded());audit.setChangedBy(actor);audit.setReason(trimToNull(request.reason()));audit.setChangedAt(LocalDateTime.now());audits.save(audit);
            }
            result.add(scoreDto(score));
        }
        if(!request.entries().isEmpty()&&book.getStatus()!=GradeBookStatus.PUBLISHED){book.setStatus(GradeBookStatus.PUBLISHED);books.save(book);}
        return result;
    }

    private CanonicalAssessment canonicalAssessment(AssessmentType type,UpdateScoreEntry entry){
        if(type==AssessmentType.SCORE){
            if(trimToNull(entry.comment())!=null)throw new BadRequestException("Đầu điểm số không nhận nội dung nhận xét");
            if(entry.score()!=null&&(entry.score().compareTo(BigDecimal.ZERO)<0||entry.score().compareTo(BigDecimal.TEN)>0))throw new BadRequestException("Điểm phải từ 0 đến 10");
            return new CanonicalAssessment(entry.score(),null,entry.score()!=null);
        }
        if(entry.score()!=null)throw new BadRequestException(type==AssessmentType.PASS_FAIL?"Đầu điểm Đạt/Chưa đạt không nhận điểm số":"Đầu nhận xét không nhận điểm số");
        String comment=trimToNull(entry.comment());
        if(comment!=null&&comment.length()>255)throw new BadRequestException("Nội dung đánh giá không được vượt quá 255 ký tự");
        if(type==AssessmentType.PASS_FAIL&&comment!=null){
            comment=comment.toUpperCase(Locale.ROOT);
            if(!comment.equals("PASS")&&!comment.equals("FAIL"))throw new BadRequestException("Kết quả Đạt/Chưa đạt phải là PASS hoặc FAIL");
        }
        return new CanonicalAssessment(null,comment,comment!=null);
    }

    private String trimToNull(String value){return value==null||value.trim().isEmpty()?null:value.trim();}

    private record CanonicalAssessment(BigDecimal score,String comment,boolean graded){}

    @Override @Transactional(readOnly=true) public BigDecimal calculateAverage(Long studentId,Long bookId){
        BigDecimal sum=BigDecimal.ZERO;int weights=0;for(GradeItem item:items.findByGradeBookIdOrderByOrderAsc(bookId)){StudentScore score=scores.findByGradeItemIdAndStudentId(item.getId(),studentId).orElse(null);if(score!=null&&score.getScore()!=null&&Boolean.TRUE.equals(score.getIsGraded())&&item.getAssessmentType()==AssessmentType.SCORE){sum=sum.add(score.getScore().multiply(BigDecimal.valueOf(item.getWeight())));weights+=item.getWeight();}}
        return weights==0?null:sum.divide(BigDecimal.valueOf(weights),1,RoundingMode.HALF_UP);
    }

    @Override public void changeStatus(Long id,GradeBookStatus status){
        if(SecurityUtil.getCurrentUserRole()!=UserRole.ADMIN) throw new UnauthorizedException("Chỉ admin được khóa bảng điểm");
        if(status!=GradeBookStatus.LOCKED) throw new BadRequestException("Điểm được công bố ngay khi giáo viên hoặc admin submit; admin chỉ có thể khóa bảng điểm");
        GradeBook book=books.findById(id).orElseThrow(()->new ResourceNotFoundException("GradeBook","id",id));
        requireEditable(book.getSemester());
        requireComplete(book);
        book.setStatus(GradeBookStatus.LOCKED);book.setIsFinalized(true);books.save(book);
    }

    @Override @Transactional(readOnly=true) public List<StudentScoreDto> getStudentScores(Long id){GradeBook book=books.findById(id).orElseThrow(()->new ResourceNotFoundException("GradeBook","id",id));authorizeRead(book);List<StudentScoreDto> out=new ArrayList<>();for(Student student:enrollments.findActiveStudentsByClassAndYear(book.getCls().getId(),book.getCls().getAcademicYear().getId()))for(GradeItem item:items.findByGradeBookIdOrderByOrderAsc(id)){StudentScore score=scores.findByGradeItemIdAndStudentId(item.getId(),student.getId()).orElse(null);out.add(score==null?new StudentScoreDto(null,student.getId(),student.getUser().getName(),student.getStudentCode(),item.getId(),null,false,null,false,null,calculateAverage(student.getId(),id)):scoreDto(score));}return out;}

    @Override @Transactional(readOnly=true)
    public GradeComponentOverviewDto getComponentOverview(Long academicYearId, Long semesterId, Long classId, Long subjectId) {
        Semester semester = semesters.findById(semesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));
        if (!semester.getAcademicYear().getId().equals(academicYearId)) {
            throw new BadRequestException("Học kỳ không thuộc năm học đã chọn");
        }
        if (classId != null) {
            SchoolClass cls = classes.findById(classId)
                    .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
            if (!cls.getAcademicYear().getId().equals(academicYearId)) {
                throw new BadRequestException("Lớp không thuộc năm học đã chọn");
            }
        }
        if (subjectId != null) {
            subjects.findById(subjectId).orElseThrow(() -> new ResourceNotFoundException("Subject", "id", subjectId));
            if (!yearSubjects.existsByAcademicYearIdAndSubjectId(academicYearId, subjectId)) {
                throw new BadRequestException("Môn học không được áp dụng trong năm học đã chọn");
            }
        }

        List<OverviewColumn> overviewColumns = componentColumns(academicYearId);
        List<GradeComponentColumnDto> columns = overviewColumns.stream().map(OverviewColumn::dto).toList();
        List<GradeBook> overviewBooks = books.findBySemesterId(semesterId).stream()
                .filter(book -> book.getCls().getAcademicYear().getId().equals(academicYearId))
                .filter(book -> classId == null || book.getCls().getId().equals(classId))
                .filter(book -> subjectId == null || book.getSubject().getId().equals(subjectId))
                .sorted(Comparator.comparing((GradeBook book) -> book.getCls().getName(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(book -> book.getSubject().getCode(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(book -> book.getSubject().getName(), String.CASE_INSENSITIVE_ORDER))
                .toList();
        if (overviewBooks.isEmpty()) return new GradeComponentOverviewDto(columns, List.of());

        List<GradeItem> overviewItems = items.findByGradeBookIdIn(
                overviewBooks.stream().map(GradeBook::getId).toList());
        Map<Long, Map<String, GradeItem>> itemsByBookCode = new HashMap<>();
        Map<Long, Map<Long, List<GradeItem>>> itemsByBookConfig = new HashMap<>();
        for (GradeItem item : overviewItems) {
            if (item.getCode() != null) {
                itemsByBookCode.computeIfAbsent(item.getGradeBook().getId(), ignored -> new HashMap<>())
                        .put(item.getCode(), item);
            }
            if (item.getConfigItem() != null) {
                itemsByBookConfig.computeIfAbsent(item.getGradeBook().getId(), ignored -> new HashMap<>())
                        .computeIfAbsent(item.getConfigItem().getId(), ignored -> new ArrayList<>())
                        .add(item);
            }
        }
        itemsByBookConfig.values().forEach(byConfig -> byConfig.values().forEach(configItems ->
                configItems.sort(Comparator.comparing(GradeItem::getOrder,
                                Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(GradeItem::getId))));
        Map<Long, List<Student>> studentsByClass = studentsByClass(academicYearId, overviewBooks);
        List<Student> overviewStudents = studentsByClass.values().stream().flatMap(Collection::stream).toList();
        Map<OverviewScoreKey, StudentScore> scoreByItemAndStudent = new HashMap<>();
        if (!overviewItems.isEmpty() && !overviewStudents.isEmpty()) {
            scores.findByGradeItemIdInAndStudentIdIn(overviewItems.stream().map(GradeItem::getId).toList(),
                            overviewStudents.stream().map(Student::getId).toList())
                    .forEach(score -> scoreByItemAndStudent.put(
                            new OverviewScoreKey(score.getGradeItem().getId(), score.getStudent().getId()), score));
        }

        List<GradeComponentOverviewRowDto> rows = new ArrayList<>();
        for (GradeBook book : overviewBooks) {
            Map<String, GradeItem> bookItemsByCode = itemsByBookCode.getOrDefault(book.getId(), Map.of());
            Map<Long, List<GradeItem>> bookItemsByConfig = itemsByBookConfig.getOrDefault(book.getId(), Map.of());
            for (Student student : studentsByClass.getOrDefault(book.getCls().getId(), List.of())) {
                Map<String, GradeComponentCellDto> values = new LinkedHashMap<>();
                for (OverviewColumn column : overviewColumns) {
                    GradeItem item = overviewItem(bookItemsByConfig, bookItemsByCode, column);
                    StudentScore score = item == null ? null
                            : scoreByItemAndStudent.get(new OverviewScoreKey(item.getId(), student.getId()));
                    if (score != null) values.put(column.dto().code(), new GradeComponentCellDto(
                            score.getScore(), score.getComment(), score.getIsGraded()));
                }
                rows.add(new GradeComponentOverviewRowDto(book.getCls().getId(), book.getCls().getName(),
                        book.getSubject().getId(), book.getSubject().getName(), student.getId(),
                        student.getUser().getName(), student.getStudentCode(), values,
                        overviewAverage(bookItemsByCode.values(), student.getId(), scoreByItemAndStudent)));
            }
        }
        return new GradeComponentOverviewDto(columns, rows);
    }

    private List<OverviewColumn> componentColumns(Long academicYearId) {
        List<OverviewColumn> result = new ArrayList<>();
        for (AcademicYearGradeConfigItem config : configItems.findByConfigAcademicYearIdOrderByDisplayOrderAsc(academicYearId)) {
            for (int occurrence = 1; occurrence <= config.getQuantity(); occurrence++) {
                String name = config.getQuantity() > 1 ? config.getDisplayName() + " " + occurrence : config.getDisplayName();
                result.add(new OverviewColumn(new GradeComponentColumnDto(config.getCode() + "_" + occurrence, name,
                        config.getWeight(), config.getAssessmentType()), config.getId(), occurrence));
            }
        }
        return result;
    }

    private GradeItem overviewItem(Map<Long, List<GradeItem>> bookItemsByConfig,
                                   Map<String, GradeItem> bookItemsByCode, OverviewColumn column) {
        List<GradeItem> configuredItems = bookItemsByConfig.get(column.configItemId());
        if (configuredItems != null && configuredItems.size() >= column.occurrence()) {
            return configuredItems.get(column.occurrence() - 1);
        }
        return bookItemsByCode.get(column.dto().code());
    }

    private Map<Long, List<Student>> studentsByClass(Long academicYearId, Collection<GradeBook> overviewBooks) {
        Set<Long> classIds = overviewBooks.stream().map(book -> book.getCls().getId()).collect(java.util.stream.Collectors.toSet());
        Map<Long, List<Student>> result = new HashMap<>();
        for (Enrollment enrollment : enrollments.findByAcademicYearIdAndStatus(academicYearId, EnrollmentStatus.ACTIVE)) {
            if (classIds.contains(enrollment.getCls().getId())) {
                result.computeIfAbsent(enrollment.getCls().getId(), ignored -> new ArrayList<>()).add(enrollment.getStudent());
            }
        }
        result.values().forEach(roster -> roster.sort(Comparator.comparing(Student::getStudentCode, String.CASE_INSENSITIVE_ORDER)));
        return result;
    }

    private BigDecimal overviewAverage(Collection<GradeItem> gradeItems, Long studentId,
                                       Map<OverviewScoreKey, StudentScore> scoreByItemAndStudent) {
        BigDecimal sum = BigDecimal.ZERO;
        int totalWeight = 0;
        for (GradeItem item : gradeItems) {
            StudentScore score = scoreByItemAndStudent.get(new OverviewScoreKey(item.getId(), studentId));
            if (item.getAssessmentType() == AssessmentType.SCORE && score != null
                    && score.getScore() != null && Boolean.TRUE.equals(score.getIsGraded())) {
                sum = sum.add(score.getScore().multiply(BigDecimal.valueOf(item.getWeight())));
                totalWeight += item.getWeight();
            }
        }
        return totalWeight == 0 ? null : sum.divide(BigDecimal.valueOf(totalWeight), 1, RoundingMode.HALF_UP);
    }

    private record OverviewColumn(GradeComponentColumnDto dto, Long configItemId, int occurrence) {}
    private record OverviewScoreKey(Long gradeItemId, Long studentId) {}

    @Override @Transactional(readOnly=true) public List<GradeCalculationDto> calculateSubjectAverages(Long id){if(SecurityUtil.getCurrentUserRole()!=UserRole.ADMIN)throw new UnauthorizedException("Chỉ admin được tính điểm trung bình");GradeBook book=books.findById(id).orElseThrow(()->new ResourceNotFoundException("GradeBook","id",id));requireComplete(book);return enrollments.findActiveStudentsByClassAndYear(book.getCls().getId(),book.getCls().getAcademicYear().getId()).stream().map(student->new GradeCalculationDto(student.getId(),student.getUser().getName(),calculateAverage(student.getId(),id))).toList();}

    private void requireComplete(GradeBook book){
        List<Student> students=enrollments.findActiveStudentsByClassAndYear(book.getCls().getId(),book.getCls().getAcademicYear().getId());
        List<GradeItem> requiredItems=items.findByGradeBookIdOrderByOrderAsc(book.getId()).stream().filter(GradeItem::getRequiredEntry).toList();
        List<String> missing=new ArrayList<>();
        for(Student student:students)for(GradeItem item:requiredItems){
            StudentScore score=scores.findByGradeItemIdAndStudentId(item.getId(),student.getId()).orElse(null);
            if(!isComplete(item,score))missing.add(student.getStudentCode()+" - "+student.getUser().getName()+" - "+item.getName());
        }
        if(!missing.isEmpty()){
            String details=missing.stream().limit(10).collect(java.util.stream.Collectors.joining(", "));
            if(missing.size()>10)details+=", ... và "+(missing.size()-10)+" điểm khác";
            throw new ConflictException("Không thể tính điểm vì còn "+missing.size()+" điểm bắt buộc chưa nhập: "+details);
        }
    }
    private boolean isComplete(GradeItem item,StudentScore score){if(score==null||!Boolean.TRUE.equals(score.getIsGraded()))return false;return item.getAssessmentType()==AssessmentType.SCORE?score.getScore()!=null:trimToNull(score.getComment())!=null;}
    private void requireEditable(Semester semester){if(semester.getStatus()==SemesterStatus.COMPLETED||semester.getAcademicYear().getStatus()==AcademicYearStatus.COMPLETED)throw new ConflictException("Kết quả đã đóng và chỉ còn quyền xem");}
    private void authorizeEntry(GradeItem item){UserRole role=SecurityUtil.getCurrentUserRole();boolean allowed=role==UserRole.ADMIN&&(item.getEntryRole()==GradeEntryRole.ADMIN||item.getEntryRole()==GradeEntryRole.SUBJECT_TEACHER_AND_ADMIN);if(role==UserRole.TEACHER){authorizeAssignment(item.getGradeBook().getCls(),item.getGradeBook().getSubject());allowed=item.getEntryRole()==GradeEntryRole.SUBJECT_TEACHER||item.getEntryRole()==GradeEntryRole.SUBJECT_TEACHER_AND_ADMIN;}if(!allowed)throw new UnauthorizedException("Tài khoản không có quyền nhập đầu điểm này");}
    private void authorizeAssignment(SchoolClass cls,Subject subject){if(SecurityUtil.getCurrentUserRole()==UserRole.ADMIN)return;if(SecurityUtil.getCurrentUserRole()!=UserRole.TEACHER)throw new UnauthorizedException("Không có quyền truy cập bảng điểm");Teacher teacher=teachers.findByUserId(SecurityUtil.getCurrentUserId()).orElseThrow(()->new UnauthorizedException("Không tìm thấy hồ sơ giáo viên"));TeachingAssignment assignment=assignments.findByClsIdAndSubjectId(cls.getId(),subject.getId()).orElseThrow(()->new UnauthorizedException("Giáo viên chưa được phân công lớp/môn này"));if(!assignment.getTeacher().getId().equals(teacher.getId())||assignment.getStatus()!=AssignmentStatus.ACTIVE)throw new UnauthorizedException("Giáo viên chưa được phân công lớp/môn này");}
    private void authorizeRead(GradeBook b){authorizeAssignment(b.getCls(),b.getSubject());}
    private GradeBook requireBook(Long c,Long s,Long m){return books.findByClsIdAndSubjectIdAndSemesterId(c,s,m).orElseThrow(()->new ResourceNotFoundException("GradeBook","scope",c+"/"+s+"/"+m));}
    private GradeBookDto dto(GradeBook b){return new GradeBookDto(b.getId(),b.getCls().getId(),b.getCls().getName(),b.getSubject().getId(),b.getSubject().getName(),b.getSemester().getId(),b.getSemester().getName(),b.getIsFinalized(),b.getStatus(),items.findByGradeBookIdOrderByOrderAsc(b.getId()).stream().map(i->new GradeItemDto(i.getId(),i.getCode(),i.getName(),i.getWeight(),i.getMaxScore(),i.getOrder(),i.getEntryRole(),i.getAssessmentType(),i.getRequiredEntry())).toList());}
    private StudentScoreDto scoreDto(StudentScore s){return new StudentScoreDto(s.getId(),s.getStudent().getId(),s.getStudent().getUser().getName(),s.getStudent().getStudentCode(),s.getGradeItem().getId(),s.getScore(),s.getIsGraded(),s.getNote(),s.getIsCommentBased(),s.getComment(),calculateAverage(s.getStudent().getId(),s.getGradeItem().getGradeBook().getId()));}
}
