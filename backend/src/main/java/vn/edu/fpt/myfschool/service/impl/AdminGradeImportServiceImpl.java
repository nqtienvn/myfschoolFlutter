package vn.edu.fpt.myfschool.service.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.myfschool.common.dto.AdminGradeImportBatchDto;
import vn.edu.fpt.myfschool.common.dto.AdminGradeImportCellDto;
import vn.edu.fpt.myfschool.common.dto.AdminGradeImportContextDto;
import vn.edu.fpt.myfschool.common.dto.AdminGradeImportItemDto;
import vn.edu.fpt.myfschool.common.dto.AdminGradeImportResultDto;
import vn.edu.fpt.myfschool.common.dto.AdminGradeImportRowDto;
import vn.edu.fpt.myfschool.common.dto.AdminGradeImportSubjectDto;
import vn.edu.fpt.myfschool.common.dto.AdminGradeImportTableDto;
import vn.edu.fpt.myfschool.common.dto.GradeBookDto;
import vn.edu.fpt.myfschool.common.dto.UpdateAdminGradeImportCellRequest;
import vn.edu.fpt.myfschool.common.dto.UpdateAdminGradeImportRowRequest;
import vn.edu.fpt.myfschool.common.enums.AcademicYearStatus;
import vn.edu.fpt.myfschool.common.enums.AssessmentType;
import vn.edu.fpt.myfschool.common.enums.EnrollmentStatus;
import vn.edu.fpt.myfschool.common.enums.GradeBookStatus;
import vn.edu.fpt.myfschool.common.enums.GradeEntryRole;
import vn.edu.fpt.myfschool.common.enums.SemesterStatus;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.common.exception.UnauthorizedException;
import vn.edu.fpt.myfschool.common.util.SecurityUtil;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.entity.AcademicYearGradeConfigItem;
import vn.edu.fpt.myfschool.entity.AcademicYearSubject;
import vn.edu.fpt.myfschool.entity.Enrollment;
import vn.edu.fpt.myfschool.entity.GradeBook;
import vn.edu.fpt.myfschool.entity.GradeImportBatch;
import vn.edu.fpt.myfschool.entity.GradeImportRow;
import vn.edu.fpt.myfschool.entity.GradeItem;
import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.entity.Semester;
import vn.edu.fpt.myfschool.entity.Student;
import vn.edu.fpt.myfschool.entity.StudentScore;
import vn.edu.fpt.myfschool.entity.StudentScoreAudit;
import vn.edu.fpt.myfschool.entity.Subject;
import vn.edu.fpt.myfschool.entity.User;
import vn.edu.fpt.myfschool.repository.AcademicYearGradeConfigItemRepository;
import vn.edu.fpt.myfschool.repository.AcademicYearSubjectRepository;
import vn.edu.fpt.myfschool.repository.ClassRepository;
import vn.edu.fpt.myfschool.repository.EnrollmentRepository;
import vn.edu.fpt.myfschool.repository.GradeBookRepository;
import vn.edu.fpt.myfschool.repository.GradeImportBatchRepository;
import vn.edu.fpt.myfschool.repository.GradeImportRowRepository;
import vn.edu.fpt.myfschool.repository.GradeItemRepository;
import vn.edu.fpt.myfschool.repository.SemesterRepository;
import vn.edu.fpt.myfschool.repository.StudentScoreAuditRepository;
import vn.edu.fpt.myfschool.repository.StudentScoreRepository;
import vn.edu.fpt.myfschool.repository.UserRepository;
import vn.edu.fpt.myfschool.service.AdminGradeImportService;
import vn.edu.fpt.myfschool.service.GradeBookService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminGradeImportServiceImpl implements AdminGradeImportService {
    private static final String DATA_SHEET = "Nhap_diem_toan_truong";
    private static final String META_SHEET = "_META";
    private static final String FORMAT = "MYFSCHOOL_ADMIN_GRADE_IMPORT_V2";
    private static final int HEADER_ROW = 3;
    private static final long MAX_FILE_SIZE = 25L * 1024 * 1024;

    private final SemesterRepository semesters;
    private final ClassRepository classes;
    private final EnrollmentRepository enrollments;
    private final AcademicYearSubjectRepository yearSubjects;
    private final AcademicYearGradeConfigItemRepository configItems;
    private final GradeBookRepository gradeBooks;
    private final GradeItemRepository gradeItems;
    private final StudentScoreRepository scores;
    private final StudentScoreAuditRepository audits;
    private final UserRepository users;
    private final GradeImportBatchRepository batches;
    private final GradeImportRowRepository rows;
    private final GradeBookService gradeBookService;

    @Override
    public AdminGradeImportContextDto getContext() {
        Scope scope = activeScope();
        return new AdminGradeImportContextDto(scope.year().getId(), scope.year().getName(),
                scope.semester().getId(), scope.semester().getName());
    }

    @Override
    public List<AdminGradeImportItemDto> getItems() {
        return components(activeScope()).stream().map(this::itemDto).toList();
    }

    @Override
    @Transactional
    public byte[] createTemplate(String itemCode) {
        Scope scope = activeScope();
        Component component = requireComponent(scope, itemCode);
        List<Subject> subjects = subjects(scope);
        List<RosterEntry> roster = roster(scope);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(DATA_SHEET);
            CellStyle title = titleStyle(workbook);
            CellStyle header = headerStyle(workbook);
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("IMPORT " + component.displayName() + " · " + scope.year().getName()
                    + " · " + scope.semester().getName());
            titleCell.setCellStyle(title);
            sheet.createRow(1).createCell(0).setCellValue(
                    "Không đổi sheet, cột hoặc mã học sinh. Ô trống giữ nguyên điểm hiện có; nhập 0 để lưu điểm 0.");
            Row headerRow = sheet.createRow(HEADER_ROW);
            writeHeader(headerRow, 0, "Mã học sinh", header);
            writeHeader(headerRow, 1, "Họ và tên", header);
            writeHeader(headerRow, 2, "Lớp", header);
            for (int index = 0; index < subjects.size(); index++) {
                Subject subject = subjects.get(index);
                writeHeader(headerRow, index + 3, subject.getCode() + " · " + subject.getName(), header);
            }
            int rowIndex = HEADER_ROW + 1;
            for (RosterEntry entry : roster) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(entry.student().getStudentCode());
                row.createCell(1).setCellValue(entry.student().getUser().getName());
                row.createCell(2).setCellValue(entry.cls().getName());
                for (int index = 0; index < subjects.size(); index++) row.createCell(index + 3);
            }
            sheet.createFreezePane(3, HEADER_ROW + 1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                    HEADER_ROW, Math.max(HEADER_ROW, rowIndex - 1), 0, subjects.size() + 2));
            sheet.setColumnWidth(0, 18 * 256);
            sheet.setColumnWidth(1, 30 * 256);
            sheet.setColumnWidth(2, 16 * 256);
            for (int index = 0; index < subjects.size(); index++) sheet.setColumnWidth(index + 3, 20 * 256);
            writeMetadata(workbook, scope, component, subjects, roster);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new ConflictException("Không thể tạo template Excel import điểm");
        }
    }

    @Override
    @Transactional
    public AdminGradeImportResultDto importFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("Vui lòng chọn file Excel .xlsx");
        if (file.getSize() > MAX_FILE_SIZE) throw new BadRequestException("File Excel không được vượt quá 25 MB");
        Scope scope = activeScope();
        List<Subject> subjects = subjects(scope);
        List<RosterEntry> roster = roster(scope);
        Map<String, RosterEntry> rosterByCode = new HashMap<>();
        roster.forEach(entry -> rosterByCode.put(entry.student().getStudentCode(), entry));
        List<String> errors = new ArrayList<>();
        List<ImportedRow> importedRows = new ArrayList<>();
        Component component;

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Map<String, String> metadata = metadata(workbook);
            component = requireComponent(scope, metadata.get("itemCode"));
            validateMetadata(metadata, scope, component, subjects, roster);
            Sheet sheet = workbook.getSheet(DATA_SHEET);
            if (sheet == null) throw new BadRequestException("File không có sheet " + DATA_SHEET);
            DataFormatter formatter = new DataFormatter();
            Map<String, Integer> subjectColumns = subjectColumns(sheet.getRow(HEADER_ROW), formatter);
            for (Subject subject : subjects) {
                if (!subjectColumns.containsKey(subject.getCode())) errors.add("Thiếu cột môn " + subject.getCode());
            }
            Set<String> importedCodes = new HashSet<>();
            for (int rowIndex = HEADER_ROW + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;
                String studentCode = cellValue(formatter, row.getCell(0));
                if (studentCode.isBlank()) continue;
                RosterEntry rosterEntry = rosterByCode.get(studentCode);
                if (rosterEntry == null) {
                    errors.add("Dòng " + (rowIndex + 1) + ": học sinh " + studentCode + " không thuộc danh sách hiện hành");
                    continue;
                }
                if (!importedCodes.add(studentCode)) {
                    errors.add("Dòng " + (rowIndex + 1) + ": mã học sinh " + studentCode + " bị lặp");
                    continue;
                }
                String className = cellValue(formatter, row.getCell(2));
                if (!rosterEntry.cls().getName().equals(className)) {
                    errors.add("Dòng " + (rowIndex + 1) + ": lớp của " + studentCode + " không khớp dữ liệu hiện hành");
                    continue;
                }
                Map<Long, AssessmentValue> values = new LinkedHashMap<>();
                for (Subject subject : subjects) {
                    Integer column = subjectColumns.get(subject.getCode());
                    if (column == null) continue;
                    String raw = cellValue(formatter, row.getCell(column));
                    if (raw.isBlank()) continue;
                    try {
                        values.put(subject.getId(), parseNonBlank(component.assessmentType(), raw));
                    } catch (BadRequestException exception) {
                        errors.add("Dòng " + (rowIndex + 1) + ", môn " + subject.getName() + ": " + exception.getMessage());
                    }
                }
                importedRows.add(new ImportedRow(rosterEntry, rowIndex - HEADER_ROW, values));
            }
            if (importedRows.size() != roster.size() || !importedCodes.equals(rosterByCode.keySet())) {
                errors.add("Template phải giữ đủ " + roster.size() + " học sinh đang học trong năm học hiện hành");
            }
        } catch (IOException exception) {
            throw new BadRequestException("Không đọc được file Excel. Vui lòng dùng đúng template .xlsx");
        }
        if (!errors.isEmpty()) {
            throw new BadRequestException("Import bị hủy để tránh ghi một phần dữ liệu: "
                    + String.join("; ", errors.stream().limit(10).toList()));
        }

        Map<ClassSubjectKey, GradeItem> itemMap = resolveGradeItems(scope,
                importedRows.stream().map(row -> row.entry().cls()).toList(), subjects, component, true);
        User actor = users.findById(SecurityUtil.getCurrentUserId())
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản import"));
        GradeImportBatch batch = new GradeImportBatch();
        batch.setAcademicYear(scope.year());
        batch.setSemester(scope.semester());
        batch.setConfigItem(component.configItem());
        batch.setItemOccurrence(component.occurrence());
        batch.setItemCode(component.itemCode());
        batch.setFileName(Optional.ofNullable(file.getOriginalFilename()).filter(name -> !name.isBlank()).orElse("import-diem.xlsx"));
        batch.setFileHash(hashFile(file));
        batch.setTotalRows(importedRows.size());
        batch.setImportedBy(actor);
        batch = batches.save(batch);

        List<GradeImportRow> batchRows = new ArrayList<>();
        int updatedScores = 0;
        for (ImportedRow imported : importedRows) {
            GradeImportRow batchRow = new GradeImportRow();
            batchRow.setBatch(batch);
            batchRow.setStudent(imported.entry().student());
            batchRow.setCls(imported.entry().cls());
            batchRow.setSourceOrder(imported.sourceOrder());
            batchRows.add(batchRow);
            for (Map.Entry<Long, AssessmentValue> cell : imported.values().entrySet()) {
                GradeItem item = itemMap.get(new ClassSubjectKey(imported.entry().cls().getId(), cell.getKey()));
                if (item == null) throw new ConflictException("Không tìm thấy đầu điểm cho môn đã import");
                if (saveScore(item, imported.entry().student(), cell.getValue(), actor,
                        "Admin import Excel · batch " + batch.getId())) updatedScores++;
            }
        }
        rows.saveAll(batchRows);
        batch.setUpdatedScores(updatedScores);
        return new AdminGradeImportResultDto(batch.getId(), component.itemCode(), component.displayName(),
                importedRows.size(), updatedScores);
    }

    @Override
    public List<AdminGradeImportBatchDto> getBatches() {
        Scope scope = activeScope();
        return batches.findBySemesterIdOrderByCreatedAtDesc(scope.semester().getId()).stream()
                .map(batch -> new AdminGradeImportBatchDto(batch.getId(), batch.getItemCode(),
                        displayName(batch.getConfigItem(), batch.getItemOccurrence()), batch.getFileName(),
                        batch.getTotalRows(), batch.getUpdatedScores(), batch.getCreatedAt()))
                .toList();
    }

    @Override
    public AdminGradeImportTableDto getBatch(Long batchId, Long classId) {
        GradeImportBatch batch = requireBatchInActiveScope(batchId);
        if (classId != null && !classes.findById(classId)
                .filter(cls -> cls.getAcademicYear().getId().equals(batch.getAcademicYear().getId())).isPresent()) {
            throw new BadRequestException("Lớp lọc không thuộc năm học của đợt import");
        }
        List<Subject> subjects = subjects(new Scope(batch.getAcademicYear(), batch.getSemester()));
        List<GradeImportRow> batchRows = classId == null
                ? rows.findByBatchIdOrderBySourceOrderAsc(batchId)
                : rows.findByBatchIdAndClsIdOrderBySourceOrderAsc(batchId, classId);
        Component component = requireComponent(new Scope(batch.getAcademicYear(), batch.getSemester()), batch.getItemCode());
        verifyBatchComponent(batch, component);
        Map<ClassSubjectKey, GradeItem> itemMap = findGradeItems(batch.getSemester(), batch.getAcademicYear(),
                batchRows.stream().map(GradeImportRow::getCls).toList(), subjects, component);
        Map<ScoreKey, StudentScore> scoreMap = scoreMap(itemMap.values(),
                batchRows.stream().map(GradeImportRow::getStudent).toList());
        List<AdminGradeImportRowDto> resultRows = batchRows.stream().map(row -> new AdminGradeImportRowDto(
                row.getStudent().getId(), row.getStudent().getStudentCode(), row.getStudent().getUser().getName(),
                row.getCls().getId(), row.getCls().getName(), row.getSourceOrder(),
                subjects.stream().map(subject -> {
                    GradeItem item = itemMap.get(new ClassSubjectKey(row.getCls().getId(), subject.getId()));
                    StudentScore score = item == null ? null : scoreMap.get(new ScoreKey(item.getId(), row.getStudent().getId()));
                    return new AdminGradeImportCellDto(subject.getId(), score == null ? null : score.getScore(),
                            score == null ? null : score.getComment(), score != null && Boolean.TRUE.equals(score.getIsGraded()));
                }).toList())).toList();
        return new AdminGradeImportTableDto(batch.getId(), batch.getItemCode(), component.displayName(),
                component.assessmentType(), subjects.stream().map(subject -> new AdminGradeImportSubjectDto(
                        subject.getId(), subject.getCode(), subject.getName())).toList(), resultRows);
    }

    @Override
    @Transactional
    public AdminGradeImportTableDto updateRow(Long batchId, Long studentId, UpdateAdminGradeImportRowRequest request) {
        GradeImportBatch batch = requireBatchInActiveScope(batchId);
        GradeImportRow importRow = rows.findByBatchIdAndStudentId(batchId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("GradeImportRow", "studentId", studentId));
        Scope scope = new Scope(batch.getAcademicYear(), batch.getSemester());
        Component component = requireComponent(scope, batch.getItemCode());
        verifyBatchComponent(batch, component);
        List<Subject> subjects = subjects(scope);
        Map<Long, Subject> subjectsById = new HashMap<>();
        subjects.forEach(subject -> subjectsById.put(subject.getId(), subject));
        Set<Long> uniqueSubjects = new HashSet<>();
        for (UpdateAdminGradeImportCellRequest cell : request.cells()) {
            if (!uniqueSubjects.add(cell.subjectId())) throw new BadRequestException("Một môn chỉ được sửa một lần trong yêu cầu");
            if (!subjectsById.containsKey(cell.subjectId())) throw new BadRequestException("Môn học không thuộc năm học hiện hành");
        }
        Map<ClassSubjectKey, GradeItem> itemMap = resolveGradeItems(scope, List.of(importRow.getCls()), subjects,
                component, true);
        User actor = users.findById(SecurityUtil.getCurrentUserId())
                .orElseThrow(() -> new UnauthorizedException("Không tìm thấy tài khoản sửa điểm"));
        for (UpdateAdminGradeImportCellRequest cell : request.cells()) {
            GradeItem item = itemMap.get(new ClassSubjectKey(importRow.getCls().getId(), cell.subjectId()));
            saveScore(item, importRow.getStudent(), parseEditable(component.assessmentType(), cell.value()), actor,
                    "Admin sửa điểm · batch " + batch.getId());
        }
        return getBatch(batchId, null);
    }

    private Scope activeScope() {
        Semester semester = semesters.findFirstByIsCurrentTrueAndAcademicYearStatus(AcademicYearStatus.ACTIVE)
                .orElseThrow(() -> new ConflictException("Chưa có học kỳ hiện hành thuộc năm học đang hoạt động"));
        if (semester.getStatus() != SemesterStatus.ACTIVE) {
            throw new ConflictException("Chỉ được import điểm khi học kỳ hiện hành đang hoạt động");
        }
        return new Scope(semester.getAcademicYear(), semester);
    }

    private GradeImportBatch requireBatchInActiveScope(Long batchId) {
        GradeImportBatch batch = batches.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("GradeImportBatch", "id", batchId));
        Scope active = activeScope();
        if (!batch.getSemester().getId().equals(active.semester().getId())
                || !batch.getAcademicYear().getId().equals(active.year().getId())) {
            throw new ConflictException("Đợt import không thuộc học kỳ đang hoạt động");
        }
        return batch;
    }

    private List<Component> components(Scope scope) {
        List<Component> result = new ArrayList<>();
        for (AcademicYearGradeConfigItem item : configItems.findByConfigAcademicYearIdOrderByDisplayOrderAsc(scope.year().getId())) {
            if (item.getEntryRole() != GradeEntryRole.ADMIN && item.getEntryRole() != GradeEntryRole.SUBJECT_TEACHER_AND_ADMIN) continue;
            for (int occurrence = 1; occurrence <= item.getQuantity(); occurrence++) {
                result.add(new Component(item, occurrence, item.getCode() + "_" + occurrence,
                        displayName(item, occurrence), item.getAssessmentType()));
            }
        }
        if (result.isEmpty()) throw new ConflictException("Năm học chưa có đầu điểm thuộc quyền Admin");
        return result;
    }

    private Component requireComponent(Scope scope, String itemCode) {
        if (itemCode == null || itemCode.isBlank()) throw new BadRequestException("Template thiếu thông tin đầu điểm");
        return components(scope).stream().filter(component -> component.itemCode().equals(itemCode))
                .findFirst().orElseThrow(() -> new BadRequestException("Đầu điểm không thuộc quyền import của Admin"));
    }

    private void verifyBatchComponent(GradeImportBatch batch, Component component) {
        if (!batch.getConfigItem().getId().equals(component.configItem().getId())
                || !batch.getItemOccurrence().equals(component.occurrence())) {
            throw new ConflictException("Cấu hình đầu điểm của đợt import không còn hợp lệ");
        }
    }

    private AdminGradeImportItemDto itemDto(Component component) {
        return new AdminGradeImportItemDto(component.configItem().getId(), component.occurrence(), component.itemCode(),
                component.displayName(), component.assessmentType());
    }

    private String displayName(AcademicYearGradeConfigItem item, int occurrence) {
        return item.getQuantity() > 1 ? item.getDisplayName() + " " + occurrence : item.getDisplayName();
    }

    private List<Subject> subjects(Scope scope) {
        List<Subject> result = yearSubjects.findByAcademicYearId(scope.year().getId()).stream()
                .map(AcademicYearSubject::getSubject)
                .sorted(Comparator.comparing(Subject::getCode, String.CASE_INSENSITIVE_ORDER)).toList();
        if (result.isEmpty()) throw new ConflictException("Năm học chưa áp dụng môn học nào");
        return result;
    }

    private List<RosterEntry> roster(Scope scope) {
        List<RosterEntry> result = new ArrayList<>();
        List<Enrollment> activeEnrollments = enrollments.findByAcademicYearIdAndStatus(scope.year().getId(), EnrollmentStatus.ACTIVE);
        activeEnrollments.stream().sorted(Comparator.comparing((Enrollment enrollment) -> enrollment.getCls().getName(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(enrollment -> enrollment.getStudent().getStudentCode(), String.CASE_INSENSITIVE_ORDER))
                .forEach(enrollment -> result.add(new RosterEntry(enrollment.getStudent(), enrollment.getCls())));
        if (result.isEmpty()) throw new ConflictException("Năm học hiện hành chưa có học sinh đang học để import điểm");
        return result;
    }

    private Map<ClassSubjectKey, GradeItem> resolveGradeItems(Scope scope, Collection<SchoolClass> requestedClasses,
                                                               List<Subject> subjects, Component component,
                                                               boolean createMissingBooks) {
        List<SchoolClass> uniqueClasses = requestedClasses.stream().collect(
                java.util.stream.Collectors.toMap(SchoolClass::getId, cls -> cls, (left, right) -> left,
                        LinkedHashMap::new)).values().stream().toList();
        Map<ClassSubjectKey, GradeItem> result = new HashMap<>();
        for (SchoolClass cls : uniqueClasses) {
            for (Subject subject : subjects) {
                GradeBook book = gradeBooks.findByClsIdAndSubjectIdAndSemesterId(cls.getId(), subject.getId(), scope.semester().getId())
                        .orElseGet(() -> {
                            if (!createMissingBooks) return null;
                            GradeBookDto created = gradeBookService.getOrCreate(cls.getId(), subject.getId(), scope.semester().getId());
                            return gradeBooks.findById(created.id()).orElseThrow();
                        });
                if (book == null) continue;
                if (book.getStatus() == GradeBookStatus.LOCKED) {
                    throw new ConflictException("Bảng điểm " + cls.getName() + " · " + subject.getName() + " đã khóa");
                }
                List<GradeItem> bookItems = gradeItems.findByGradeBookIdOrderByOrderAsc(book.getId());
                GradeItem item = bookItems.stream()
                        .filter(candidate -> candidate.getConfigItem() != null
                                && candidate.getConfigItem().getId().equals(component.configItem().getId())
                                && component.itemCode().equals(candidate.getCode()))
                        .findFirst().orElse(null);
                if (item == null && createMissingBooks) item = repairMissingGradeItem(book, bookItems, component);
                if (item == null) {
                    throw new ConflictException("Không tìm thấy đầu điểm "
                            + component.displayName() + " cho môn " + subject.getName());
                }
                result.put(new ClassSubjectKey(cls.getId(), subject.getId()), item);
            }
        }
        return result;
    }

    private GradeItem repairMissingGradeItem(GradeBook book, List<GradeItem> existingItems, Component component) {
        GradeItem item = existingItems.stream()
                .filter(candidate -> component.itemCode().equals(candidate.getCode())
                        || component.displayName().equals(candidate.getName()))
                .findFirst().orElseGet(GradeItem::new);
        boolean isNew = item.getId() == null;
        item.setGradeBook(book);
        item.setConfigItem(component.configItem());
        item.setCode(component.itemCode());
        item.setName(component.displayName());
        item.setWeight(component.configItem().getWeight());
        item.setMaxScore(10);
        item.setEntryRole(component.configItem().getEntryRole());
        item.setAssessmentType(component.assessmentType());
        item.setRequiredEntry(component.configItem().getRequiredEntry());
        if (isNew) {
            int nextOrder = existingItems.stream().map(GradeItem::getOrder)
                    .filter(Objects::nonNull).max(Integer::compareTo).orElse(-1) + 1;
            item.setOrder(nextOrder);
        }
        return gradeItems.save(item);
    }

    private Map<ClassSubjectKey, GradeItem> findGradeItems(Semester semester, AcademicYear year,
                                                            Collection<SchoolClass> requestedClasses,
                                                            List<Subject> subjects, Component component) {
        Set<Long> classIds = requestedClasses.stream().map(SchoolClass::getId).collect(java.util.stream.Collectors.toSet());
        Set<Long> subjectIds = subjects.stream().map(Subject::getId).collect(java.util.stream.Collectors.toSet());
        List<GradeBook> books = gradeBooks.findBySemesterId(semester.getId()).stream()
                .filter(book -> book.getCls().getAcademicYear().getId().equals(year.getId())
                        && classIds.contains(book.getCls().getId()) && subjectIds.contains(book.getSubject().getId()))
                .toList();
        List<GradeItem> items = books.isEmpty() ? List.of() : gradeItems.findByGradeBookIdIn(books.stream().map(GradeBook::getId).toList());
        Map<Long, GradeItem> itemByBook = new HashMap<>();
        items.stream().filter(item -> item.getConfigItem() != null
                        && item.getConfigItem().getId().equals(component.configItem().getId())
                        && item.getCode().equals(component.itemCode()))
                .forEach(item -> itemByBook.put(item.getGradeBook().getId(), item));
        Map<ClassSubjectKey, GradeItem> result = new HashMap<>();
        for (GradeBook book : books) {
            GradeItem item = itemByBook.get(book.getId());
            if (item != null) result.put(new ClassSubjectKey(book.getCls().getId(), book.getSubject().getId()), item);
        }
        return result;
    }

    private Map<ScoreKey, StudentScore> scoreMap(Collection<GradeItem> items, Collection<Student> students) {
        if (items.isEmpty() || students.isEmpty()) return Map.of();
        Map<ScoreKey, StudentScore> result = new HashMap<>();
        scores.findByGradeItemIdInAndStudentIdIn(items.stream().map(GradeItem::getId).toList(),
                        students.stream().map(Student::getId).toList())
                .forEach(score -> result.put(new ScoreKey(score.getGradeItem().getId(), score.getStudent().getId()), score));
        return result;
    }

    private boolean saveScore(GradeItem item, Student student, AssessmentValue value, User actor, String reason) {
        if (item == null) throw new BadRequestException("Thiếu đầu điểm cần cập nhật");
        if (item.getGradeBook().getStatus() == GradeBookStatus.LOCKED) throw new ConflictException("Bảng điểm đã khóa");
        StudentScore score = scores.findByGradeItemIdAndStudentId(item.getId(), student.getId()).orElseGet(() -> {
            StudentScore created = new StudentScore();
            created.setGradeItem(item);
            created.setStudent(student);
            return created;
        });
        BigDecimal oldScore = score.getScore();
        String oldComment = score.getComment();
        Boolean oldIsGraded = score.getIsGraded();
        score.setScore(value.score());
        score.setComment(value.comment());
        score.setIsGraded(value.isGraded());
        score.setIsCommentBased(item.getAssessmentType() != AssessmentType.SCORE);
        score.setNote("Admin import Excel");
        score.setPublishedAt(null);
        score.setEnteredBy(actor);
        boolean changed = !Objects.equals(oldScore, score.getScore())
                || !Objects.equals(oldComment, score.getComment())
                || !Objects.equals(oldIsGraded, score.getIsGraded());
        score = scores.save(score);
        if (changed) {
            StudentScoreAudit audit = new StudentScoreAudit();
            audit.setStudentScore(score);
            audit.setOldScore(oldScore);
            audit.setNewScore(score.getScore());
            audit.setOldComment(oldComment);
            audit.setNewComment(score.getComment());
            audit.setOldIsGraded(oldIsGraded);
            audit.setNewIsGraded(score.getIsGraded());
            audit.setChangedBy(actor);
            audit.setReason(reason);
            audit.setChangedAt(LocalDateTime.now());
            audits.save(audit);
        }
        return changed;
    }

    private AssessmentValue parseNonBlank(AssessmentType type, String raw) {
        if (type == AssessmentType.SCORE) {
            try {
                BigDecimal value = new BigDecimal(raw.replace(',', '.'));
                if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.TEN) > 0) {
                    throw new BadRequestException("điểm phải từ 0 đến 10");
                }
                return new AssessmentValue(value, null, true);
            } catch (NumberFormatException exception) {
                throw new BadRequestException("điểm không hợp lệ");
            }
        }
        if (type == AssessmentType.PASS_FAIL) {
            String value = normalizePassFail(raw);
            if (value == null) throw new BadRequestException("dùng PASS/FAIL hoặc Đạt/Chưa đạt");
            return new AssessmentValue(null, value, true);
        }
        if (raw.length() > 255) throw new BadRequestException("nhận xét không được vượt quá 255 ký tự");
        return new AssessmentValue(null, raw.trim(), true);
    }

    private AssessmentValue parseEditable(AssessmentType type, String raw) {
        if (raw == null || raw.trim().isEmpty()) return new AssessmentValue(null, null, false);
        return parseNonBlank(type, raw.trim());
    }

    private String normalizePassFail(String raw) {
        String value = raw.trim().toUpperCase(Locale.ROOT);
        if (value.equals("PASS") || value.equals("ĐẠT") || value.equals("DAT")) return "PASS";
        if (value.equals("FAIL") || value.equals("CHƯA ĐẠT") || value.equals("CHUA DAT")) return "FAIL";
        return null;
    }

    private void writeMetadata(Workbook workbook, Scope scope, Component component,
                               List<Subject> subjects, List<RosterEntry> roster) {
        Sheet meta = workbook.createSheet(META_SHEET);
        String[][] values = {
                {"format", FORMAT},
                {"academicYearId", String.valueOf(scope.year().getId())},
                {"semesterId", String.valueOf(scope.semester().getId())},
                {"configItemId", String.valueOf(component.configItem().getId())},
                {"occurrence", String.valueOf(component.occurrence())},
                {"itemCode", component.itemCode()},
                {"configurationFingerprint", configurationFingerprint(component)},
                {"subjectsFingerprint", subjectsFingerprint(subjects)},
                {"rosterFingerprint", rosterFingerprint(roster)}
        };
        for (int index = 0; index < values.length; index++) {
            Row row = meta.createRow(index);
            row.createCell(0).setCellValue(values[index][0]);
            row.createCell(1).setCellValue(values[index][1]);
        }
        workbook.setSheetHidden(workbook.getSheetIndex(meta), true);
    }

    private Map<String, String> metadata(Workbook workbook) {
        Sheet meta = workbook.getSheet(META_SHEET);
        if (meta == null) throw new BadRequestException("File không phải template import điểm của MyFschool");
        DataFormatter formatter = new DataFormatter();
        Map<String, String> values = new HashMap<>();
        for (Row row : meta) values.put(cellValue(formatter, row.getCell(0)), cellValue(formatter, row.getCell(1)));
        return values;
    }

    private void validateMetadata(Map<String, String> metadata, Scope scope, Component component,
                                  List<Subject> subjects, List<RosterEntry> roster) {
        if (!FORMAT.equals(metadata.get("format"))) throw new BadRequestException("File không đúng định dạng template import điểm");
        if (!String.valueOf(scope.year().getId()).equals(metadata.get("academicYearId"))
                || !String.valueOf(scope.semester().getId()).equals(metadata.get("semesterId"))) {
            throw new BadRequestException("Template không thuộc học kỳ hiện hành");
        }
        if (!String.valueOf(component.configItem().getId()).equals(metadata.get("configItemId"))
                || !String.valueOf(component.occurrence()).equals(metadata.get("occurrence"))
                || !component.itemCode().equals(metadata.get("itemCode"))) {
            throw new BadRequestException("Template không khớp đầu điểm đang import");
        }
        if (!configurationFingerprint(component).equals(metadata.get("configurationFingerprint"))) {
            throw new BadRequestException("Cấu hình đầu điểm đã thay đổi. Hãy tải template mới");
        }
        if (!subjectsFingerprint(subjects).equals(metadata.get("subjectsFingerprint"))) {
            throw new BadRequestException("Danh sách môn học đã thay đổi. Hãy tải template mới");
        }
        if (!rosterFingerprint(roster).equals(metadata.get("rosterFingerprint"))) {
            throw new BadRequestException("Danh sách học sinh hoặc lớp đã thay đổi. Hãy tải template mới");
        }
    }

    private Map<String, Integer> subjectColumns(Row header, DataFormatter formatter) {
        if (header == null) throw new BadRequestException("Template thiếu hàng tiêu đề");
        Map<String, Integer> result = new HashMap<>();
        for (Cell cell : header) {
            String value = cellValue(formatter, cell);
            int separator = value.indexOf(" · ");
            if (separator > 0) result.put(value.substring(0, separator), cell.getColumnIndex());
        }
        return result;
    }

    private String cellValue(DataFormatter formatter, Cell cell) {
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private String configurationFingerprint(Component component) {
        AcademicYearGradeConfigItem item = component.configItem();
        return hash(item.getId() + "|" + item.getCode() + "|" + item.getDisplayName() + "|" + item.getQuantity()
                + "|" + item.getEntryRole() + "|" + item.getAssessmentType() + "|" + item.getRequiredEntry()
                + "|" + component.occurrence());
    }

    private String subjectsFingerprint(List<Subject> subjects) {
        return hash(subjects.stream().map(subject -> subject.getId() + "|" + subject.getCode() + "|" + subject.getName())
                .reduce((left, right) -> left + ";" + right).orElse(""));
    }

    private String rosterFingerprint(List<RosterEntry> roster) {
        return hash(roster.stream().map(entry -> entry.student().getId() + "|" + entry.student().getStudentCode()
                        + "|" + entry.cls().getId() + "|" + entry.cls().getName())
                .reduce((left, right) -> left + ";" + right).orElse(""));
    }

    private String hashFile(MultipartFile file) {
        try {
            return hashBytes(file.getBytes());
        } catch (IOException exception) {
            throw new BadRequestException("Không thể đọc file Excel để lưu dấu vết import");
        }
    }

    private String hash(String source) {
        return hashBytes(source.getBytes(StandardCharsets.UTF_8));
    }

    private String hashBytes(byte[] source) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        return style;
    }

    private CellStyle titleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        font.setColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFont(font);
        return style;
    }

    private void writeHeader(Row row, int column, String value, CellStyle style) {
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private record Scope(AcademicYear year, Semester semester) {}
    private record Component(AcademicYearGradeConfigItem configItem, Integer occurrence, String itemCode,
                             String displayName, AssessmentType assessmentType) {}
    private record RosterEntry(Student student, SchoolClass cls) {}
    private record ImportedRow(RosterEntry entry, Integer sourceOrder, Map<Long, AssessmentValue> values) {}
    private record AssessmentValue(BigDecimal score, String comment, Boolean isGraded) {}
    private record ClassSubjectKey(Long classId, Long subjectId) {}
    private record ScoreKey(Long gradeItemId, Long studentId) {}
}
