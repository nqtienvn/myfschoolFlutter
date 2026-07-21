package vn.edu.fpt.myfschool.service.impl;

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
import vn.edu.fpt.myfschool.common.dto.GradeBookDto;
import vn.edu.fpt.myfschool.common.dto.GradeImportResultDto;
import vn.edu.fpt.myfschool.common.dto.ResultSummaryDto;
import vn.edu.fpt.myfschool.common.dto.UpdateScoreEntry;
import vn.edu.fpt.myfschool.common.dto.UpdateStudentScoreRequest;
import vn.edu.fpt.myfschool.common.enums.AssessmentType;
import vn.edu.fpt.myfschool.common.enums.EnrollmentStatus;
import vn.edu.fpt.myfschool.common.enums.GradeEntryRole;
import vn.edu.fpt.myfschool.common.exception.BadRequestException;
import vn.edu.fpt.myfschool.common.exception.ConflictException;
import vn.edu.fpt.myfschool.common.exception.ForbiddenException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.AcademicYear;
import vn.edu.fpt.myfschool.entity.AcademicYearResult;
import vn.edu.fpt.myfschool.entity.GradeBook;
import vn.edu.fpt.myfschool.entity.GradeItem;
import vn.edu.fpt.myfschool.entity.SchoolClass;
import vn.edu.fpt.myfschool.entity.Semester;
import vn.edu.fpt.myfschool.entity.Student;
import vn.edu.fpt.myfschool.entity.StudentScore;
import vn.edu.fpt.myfschool.repository.AcademicYearRepository;
import vn.edu.fpt.myfschool.repository.AcademicYearResultRepository;
import vn.edu.fpt.myfschool.repository.ClassRepository;
import vn.edu.fpt.myfschool.repository.EnrollmentRepository;
import vn.edu.fpt.myfschool.repository.GradeBookRepository;
import vn.edu.fpt.myfschool.repository.GradeItemRepository;
import vn.edu.fpt.myfschool.repository.SemesterRepository;
import vn.edu.fpt.myfschool.repository.StudentScoreRepository;
import vn.edu.fpt.myfschool.service.GradeBookService;
import vn.edu.fpt.myfschool.service.GradeResultExcelService;
import vn.edu.fpt.myfschool.service.SemesterResultService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service("gradeResultExcelService")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GradeResultExcelServiceImpl implements GradeResultExcelService {
    private static final String DATA_SHEET = "Nhap_diem_Admin";
    private static final String META_SHEET = "_META";
    private static final int HEADER_ROW = 3;

    private final AcademicYearRepository academicYears;
    private final SemesterRepository semesters;
    private final ClassRepository classes;
    private final GradeBookRepository gradeBooks;
    private final GradeItemRepository gradeItems;
    private final StudentScoreRepository scores;
    private final EnrollmentRepository enrollments;
    private final AcademicYearResultRepository annualResults;
    private final GradeBookService gradeBookService;
    private final SemesterResultService semesterResultService;

    @Override
    @Transactional
    public byte[] createTemplate(Long academicYearId, Long semesterId, Long classId, Long subjectId) {
        Scope scope = requireScope(academicYearId, semesterId, classId);
        GradeBookDto dto = gradeBookService.getOrCreate(classId, subjectId, semesterId);
        GradeBook book = gradeBooks.findById(dto.id()).orElseThrow();
        List<GradeItem> adminItems = adminItems(book);
        if (adminItems.isEmpty()) {
            throw new ConflictException("Môn học không có đầu điểm do Admin phụ trách trong năm học này");
        }
        List<Student> students = enrollments.findActiveStudentsByClassAndYear(classId, academicYearId).stream()
                .sorted(Comparator.comparing(Student::getStudentCode)).toList();
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(DATA_SHEET);
            CellStyle header = headerStyle(workbook);
            CellStyle title = titleStyle(workbook);
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("IMPORT ĐIỂM ADMIN · " + scope.year().getName() + " · "
                    + scope.semester().getName() + " · " + scope.cls().getName() + " · " + dto.subjectName());
            titleCell.setCellStyle(title);
            sheet.createRow(1).createCell(0).setCellValue(
                    "Không đổi tên cột hoặc sheet. Ô điểm số để trống sẽ được nhập là 0 theo quy ước của nhà trường.");
            Row headerRow = sheet.createRow(HEADER_ROW);
            writeHeader(headerRow, 0, "Mã học sinh", header);
            writeHeader(headerRow, 1, "Họ và tên", header);
            for (int index = 0; index < adminItems.size(); index++) {
                GradeItem item = adminItems.get(index);
                writeHeader(headerRow, index + 2, item.getCode() + " · " + item.getName(), header);
            }
            int rowIndex = HEADER_ROW + 1;
            for (Student student : students) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(student.getStudentCode());
                row.createCell(1).setCellValue(student.getUser().getName());
                for (int itemIndex = 0; itemIndex < adminItems.size(); itemIndex++) {
                    GradeItem item = adminItems.get(itemIndex);
                    StudentScore existing = scores.findByGradeItemIdAndStudentId(item.getId(), student.getId()).orElse(null);
                    Cell cell = row.createCell(itemIndex + 2);
                    if (existing == null || !Boolean.TRUE.equals(existing.getIsGraded())) continue;
                    if (item.getAssessmentType() == AssessmentType.SCORE && existing.getScore() != null) {
                        cell.setCellValue(existing.getScore().doubleValue());
                    } else if (existing.getComment() != null) {
                        cell.setCellValue(existing.getComment());
                    }
                }
            }
            sheet.createFreezePane(2, HEADER_ROW + 1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                    HEADER_ROW, Math.max(HEADER_ROW, rowIndex - 1), 0, adminItems.size() + 1));
            sheet.setColumnWidth(0, 18 * 256);
            sheet.setColumnWidth(1, 30 * 256);
            for (int index = 0; index < adminItems.size(); index++) sheet.setColumnWidth(index + 2, 22 * 256);
            writeMetadata(workbook, scope, book, subjectId, adminItems);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new ConflictException("Không thể tạo template Excel");
        }
    }

    @Override
    @Transactional
    public GradeImportResultDto importScores(Long academicYearId, Long semesterId, Long classId,
                                             Long subjectId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BadRequestException("Vui lòng chọn file Excel .xlsx");
        Scope scope = requireScope(academicYearId, semesterId, classId);
        GradeBook book = gradeBooks.findByClsIdAndSubjectIdAndSemesterId(classId, subjectId, semesterId)
                .orElseThrow(() -> new ConflictException("Hãy tải template trước khi import điểm"));
        List<GradeItem> adminItems = adminItems(book);
        if (adminItems.isEmpty()) {
            throw new ConflictException("Môn học không có đầu điểm do Admin phụ trách trong năm học này");
        }
        Map<Long, List<UpdateScoreEntry>> entriesByItem = new LinkedHashMap<>();
        adminItems.forEach(item -> entriesByItem.put(item.getId(), new ArrayList<>()));
        Map<String, Student> roster = new HashMap<>();
        enrollments.findActiveStudentsByClassAndYear(classId, academicYearId)
                .forEach(student -> roster.put(student.getStudentCode(), student));
        List<String> errors = new ArrayList<>();
        Set<String> importedStudentCodes = new HashSet<>();
        int importedRows = 0;
        int zeroFilled = 0;
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            validateMetadata(workbook, scope, book, subjectId, adminItems);
            Sheet sheet = workbook.getSheet(DATA_SHEET);
            if (sheet == null) throw new BadRequestException("File không có sheet " + DATA_SHEET);
            DataFormatter formatter = new DataFormatter();
            Map<String, Integer> columns = headerColumns(sheet.getRow(HEADER_ROW), formatter);
            for (GradeItem item : adminItems) {
                if (!columns.containsKey(item.getCode())) errors.add("Thiếu cột " + item.getCode());
            }
            for (int rowIndex = HEADER_ROW + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) continue;
                String studentCode = formatter.formatCellValue(row.getCell(0)).trim();
                if (studentCode.isEmpty()) continue;
                Student student = roster.get(studentCode);
                if (student == null) {
                    errors.add("Dòng " + (rowIndex + 1) + ": học sinh " + studentCode
                            + " không thuộc lớp/năm học đã chọn");
                    continue;
                }
                if (!importedStudentCodes.add(studentCode)) {
                    errors.add("Dòng " + (rowIndex + 1) + ": mã học sinh " + studentCode
                            + " bị lặp trong file");
                    continue;
                }
                importedRows++;
                for (GradeItem item : adminItems) {
                    Integer column = columns.get(item.getCode());
                    if (column == null) continue;
                    String raw = formatter.formatCellValue(row.getCell(column)).trim();
                    UpdateScoreEntry entry;
                    if (item.getAssessmentType() == AssessmentType.SCORE) {
                        BigDecimal value;
                        if (raw.isEmpty()) {
                            value = BigDecimal.ZERO;
                            zeroFilled++;
                        } else {
                            try {
                                value = new BigDecimal(raw.replace(',', '.'));
                            } catch (NumberFormatException exception) {
                                errors.add("Dòng " + (rowIndex + 1) + ", cột " + item.getCode()
                                        + ": điểm không hợp lệ");
                                continue;
                            }
                        }
                        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(BigDecimal.TEN) > 0) {
                            errors.add("Dòng " + (rowIndex + 1) + ", cột " + item.getCode()
                                    + ": điểm phải từ 0 đến 10");
                            continue;
                        }
                        entry = new UpdateScoreEntry(student.getId(), value, "Import Excel", true, false, null);
                    } else if (item.getAssessmentType() == AssessmentType.PASS_FAIL) {
                        String value = normalizePassFail(raw);
                        if (!raw.isEmpty() && value == null) {
                            errors.add("Dòng " + (rowIndex + 1) + ", cột " + item.getCode()
                                    + ": dùng PASS/FAIL hoặc Đạt/Chưa đạt");
                            continue;
                        }
                        entry = new UpdateScoreEntry(student.getId(), null, "Import Excel",
                                value != null, true, value);
                    } else {
                        entry = new UpdateScoreEntry(student.getId(), null, "Import Excel",
                                !raw.isEmpty(), true, raw.isEmpty() ? null : raw);
                    }
                    entriesByItem.get(item.getId()).add(entry);
                }
            }
        } catch (IOException exception) {
            throw new BadRequestException("Không đọc được file Excel. Vui lòng dùng đúng template .xlsx");
        }
        if (!errors.isEmpty()) {
            String details = String.join("; ", errors.stream().limit(8).toList());
            throw new BadRequestException("Import bị hủy để tránh ghi một phần dữ liệu: " + details);
        }
        if (importedRows != roster.size() || !importedStudentCodes.containsAll(roster.keySet())) {
            throw new BadRequestException("Template phải có đủ " + roster.size()
                    + " học sinh của lớp; file hiện có " + importedRows);
        }
        int updatedScores = 0;
        for (GradeItem item : adminItems) {
            List<UpdateScoreEntry> entries = entriesByItem.get(item.getId());
            gradeBookService.updateScores(new UpdateStudentScoreRequest(
                    item.getId(), entries, "Admin import Excel · " + scope.year().getName()));
            updatedScores += entries.size();
        }
        return new GradeImportResultDto(roster.size(), importedRows, updatedScores, zeroFilled, List.of());
    }

    @Override
    public byte[] exportResults(Long academicYearId, Long semesterId, Long classId) {
        AcademicYear year = academicYears.findById(academicYearId)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", academicYearId));
        List<Semester> selectedSemesters = semesterId == null
                ? semesters.findByAcademicYearIdOrderByOrderAsc(academicYearId)
                : List.of(requireSemester(semesterId, academicYearId));
        List<SchoolClass> selectedClasses = classId == null
                ? classes.findByAcademicYearId(academicYearId)
                : List.of(requireClass(classId, academicYearId));
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            writeComponentScores(workbook, selectedSemesters, selectedClasses);
            writeSemesterResults(workbook, academicYearId, selectedSemesters, selectedClasses);
            writeAnnualResults(workbook, academicYearId, selectedClasses);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new ConflictException("Không thể xuất báo cáo Excel");
        }
    }

    private void writeComponentScores(Workbook workbook, List<Semester> selectedSemesters,
                                      List<SchoolClass> selectedClasses) {
        Sheet sheet = workbook.createSheet("Diem_thanh_phan");
        String[] headers = {"Học kỳ", "Lớp", "Mã HS", "Họ và tên", "Môn", "Mã đầu điểm",
                "Đầu điểm", "Loại", "Trọng số", "Giá trị", "ĐTB môn", "Trạng thái bảng điểm"};
        Row headerRow = sheet.createRow(0);
        CellStyle header = headerStyle(workbook);
        for (int index = 0; index < headers.length; index++) writeHeader(headerRow, index, headers[index], header);
        int rowIndex = 1;
        for (Semester semester : selectedSemesters) for (SchoolClass cls : selectedClasses) {
            for (GradeBook book : gradeBooks.findByClsIdAndSemesterId(cls.getId(), semester.getId())) {
                List<Student> students = enrollments.findActiveStudentsByClassAndYear(
                        cls.getId(), cls.getAcademicYear().getId());
                List<GradeItem> bookItems = gradeItems.findByGradeBookIdOrderByOrderAsc(book.getId());
                for (Student student : students) {
                    BigDecimal average = gradeBookService.calculateAverage(student.getId(), book.getId());
                    for (GradeItem item : bookItems) {
                        StudentScore score = scores.findByGradeItemIdAndStudentId(
                                item.getId(), student.getId()).orElse(null);
                        Row row = sheet.createRow(rowIndex++);
                        int cell = 0;
                        row.createCell(cell++).setCellValue(semester.getName());
                        row.createCell(cell++).setCellValue(cls.getName());
                        row.createCell(cell++).setCellValue(student.getStudentCode());
                        row.createCell(cell++).setCellValue(student.getUser().getName());
                        row.createCell(cell++).setCellValue(book.getSubject().getName());
                        row.createCell(cell++).setCellValue(item.getCode());
                        row.createCell(cell++).setCellValue(item.getName());
                        row.createCell(cell++).setCellValue(item.getAssessmentType().name());
                        row.createCell(cell++).setCellValue(item.getWeight());
                        if (item.getAssessmentType() == AssessmentType.SCORE) {
                            row.createCell(cell++).setCellValue(score == null || score.getScore() == null
                                    ? 0 : score.getScore().doubleValue());
                        } else {
                            row.createCell(cell++).setCellValue(score == null || score.getComment() == null
                                    ? "" : score.getComment());
                        }
                        if (average != null) row.createCell(cell).setCellValue(average.doubleValue());
                        cell++;
                        row.createCell(cell).setCellValue(book.getStatus().name());
                    }
                }
            }
        }
        finishSheet(sheet, headers.length, rowIndex);
    }

    private void writeSemesterResults(Workbook workbook, Long academicYearId,
                                      List<Semester> selectedSemesters, List<SchoolClass> selectedClasses) {
        Sheet sheet = workbook.createSheet("Tong_ket_hoc_ky");
        String[] headers = {"Học kỳ", "Lớp", "Mã HS", "Họ và tên", "Điểm TB tham khảo",
                "Xếp hạng", "Vi phạm", "Nghỉ có phép", "Nghỉ không phép", "Kết quả học tập",
                "Kết quả rèn luyện", "Danh hiệu", "Nhận xét GVCN", "Trạng thái"};
        Row headerRow = sheet.createRow(0);
        CellStyle header = headerStyle(workbook);
        for (int index = 0; index < headers.length; index++) writeHeader(headerRow, index, headers[index], header);
        int rowIndex = 1;
        for (Semester semester : selectedSemesters) for (SchoolClass cls : selectedClasses) {
            for (ResultSummaryDto result : semesterResultService.getResultSummary(
                    academicYearId, semester.getId(), cls.getId())) {
                Row row = sheet.createRow(rowIndex++);
                int cell = 0;
                row.createCell(cell++).setCellValue(semester.getName());
                row.createCell(cell++).setCellValue(cls.getName());
                row.createCell(cell++).setCellValue(result.studentCode());
                row.createCell(cell++).setCellValue(result.studentName());
                if (result.gpa() != null) row.createCell(cell).setCellValue(result.gpa().doubleValue());
                cell++;
                if (result.rank() != null) row.createCell(cell).setCellValue(result.rank());
                cell++;
                row.createCell(cell++).setCellValue(result.violationCount());
                row.createCell(cell++).setCellValue(result.absentWithLeave());
                row.createCell(cell++).setCellValue(result.absentWithoutLeave());
                row.createCell(cell++).setCellValue(nullToEmpty(result.academicAbility()));
                row.createCell(cell++).setCellValue(nullToEmpty(result.conduct()));
                row.createCell(cell++).setCellValue(nullToEmpty(result.honor()));
                row.createCell(cell++).setCellValue(nullToEmpty(result.generalComment()));
                row.createCell(cell).setCellValue(result.status());
            }
        }
        finishSheet(sheet, headers.length, rowIndex);
    }

    private void writeAnnualResults(Workbook workbook, Long academicYearId, List<SchoolClass> selectedClasses) {
        Sheet sheet = workbook.createSheet("Tong_ket_nam_hoc");
        String[] headers = {"Lớp", "Mã HS", "Họ và tên", "Điểm TB tham khảo", "Xếp hạng",
                "Kết quả học tập", "Kết quả rèn luyện", "Danh hiệu", "Trạng thái"};
        Row headerRow = sheet.createRow(0);
        CellStyle header = headerStyle(workbook);
        for (int index = 0; index < headers.length; index++) writeHeader(headerRow, index, headers[index], header);
        int rowIndex = 1;
        for (SchoolClass cls : selectedClasses) for (AcademicYearResult result :
                annualResults.findByClsIdAndAcademicYearIdOrderByRankAsc(cls.getId(), academicYearId)) {
            Row row = sheet.createRow(rowIndex++);
            int cell = 0;
            row.createCell(cell++).setCellValue(cls.getName());
            row.createCell(cell++).setCellValue(result.getStudent().getStudentCode());
            row.createCell(cell++).setCellValue(result.getStudent().getUser().getName());
            if (result.getGpa() != null) row.createCell(cell).setCellValue(result.getGpa().doubleValue());
            cell++;
            if (result.getRank() != null) row.createCell(cell).setCellValue(result.getRank());
            cell++;
            row.createCell(cell++).setCellValue(result.getAcademicAbility());
            row.createCell(cell++).setCellValue(result.getConduct());
            row.createCell(cell++).setCellValue(result.getHonor());
            row.createCell(cell).setCellValue(result.getPublishedAt() == null ? "DRAFT" : "PUBLISHED");
        }
        finishSheet(sheet, headers.length, rowIndex);
    }

    private Scope requireScope(Long academicYearId, Long semesterId, Long classId) {
        AcademicYear year = academicYears.findById(academicYearId)
                .orElseThrow(() -> new ResourceNotFoundException("AcademicYear", "id", academicYearId));
        Semester semester = requireSemester(semesterId, academicYearId);
        SchoolClass cls = requireClass(classId, academicYearId);
        return new Scope(year, semester, cls);
    }

    private Semester requireSemester(Long semesterId, Long academicYearId) {
        Semester semester = semesters.findById(semesterId)
                .orElseThrow(() -> new ResourceNotFoundException("Semester", "id", semesterId));
        if (!semester.getAcademicYear().getId().equals(academicYearId)) {
            throw new ForbiddenException("Học kỳ không thuộc năm học đã chọn");
        }
        return semester;
    }

    private SchoolClass requireClass(Long classId, Long academicYearId) {
        SchoolClass cls = classes.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Class", "id", classId));
        if (!cls.getAcademicYear().getId().equals(academicYearId)) {
            throw new ForbiddenException("Lớp không thuộc năm học đã chọn");
        }
        return cls;
    }

    private List<GradeItem> adminItems(GradeBook book) {
        return gradeItems.findByGradeBookIdOrderByOrderAsc(book.getId()).stream()
                .filter(item -> item.getEntryRole() == GradeEntryRole.ADMIN
                        || item.getEntryRole() == GradeEntryRole.SUBJECT_TEACHER_AND_ADMIN)
                .toList();
    }

    private void writeMetadata(Workbook workbook, Scope scope, GradeBook book, Long subjectId,
                               List<GradeItem> items) {
        Sheet meta = workbook.createSheet(META_SHEET);
        String[][] values = {
                {"format", "MYFSCHOOL_GRADE_IMPORT_V1"},
                {"academicYearId", String.valueOf(scope.year().getId())},
                {"semesterId", String.valueOf(scope.semester().getId())},
                {"classId", String.valueOf(scope.cls().getId())},
                {"subjectId", String.valueOf(subjectId)},
                {"gradeBookId", String.valueOf(book.getId())},
                {"configurationFingerprint", fingerprint(items)}
        };
        for (int index = 0; index < values.length; index++) {
            Row row = meta.createRow(index);
            row.createCell(0).setCellValue(values[index][0]);
            row.createCell(1).setCellValue(values[index][1]);
        }
        workbook.setSheetHidden(workbook.getSheetIndex(meta), true);
    }

    private void validateMetadata(Workbook workbook, Scope scope, GradeBook book, Long subjectId,
                                  List<GradeItem> items) {
        Sheet meta = workbook.getSheet(META_SHEET);
        if (meta == null) throw new BadRequestException("File không phải template điểm của MyFschool");
        Map<String, String> values = new HashMap<>();
        DataFormatter formatter = new DataFormatter();
        for (Row row : meta) values.put(formatter.formatCellValue(row.getCell(0)),
                formatter.formatCellValue(row.getCell(1)));
        boolean matches = "MYFSCHOOL_GRADE_IMPORT_V1".equals(values.get("format"))
                && String.valueOf(scope.year().getId()).equals(values.get("academicYearId"))
                && String.valueOf(scope.semester().getId()).equals(values.get("semesterId"))
                && String.valueOf(scope.cls().getId()).equals(values.get("classId"))
                && String.valueOf(subjectId).equals(values.get("subjectId"))
                && String.valueOf(book.getId()).equals(values.get("gradeBookId"));
        if (!matches) {
            throw new BadRequestException("Template không khớp năm học, học kỳ, lớp hoặc môn đang chọn");
        }
        if (!fingerprint(items).equals(values.get("configurationFingerprint"))) {
            throw new BadRequestException("Cấu hình đầu điểm đã thay đổi. Hãy tải template mới trước khi import");
        }
    }

    private Map<String, Integer> headerColumns(Row header, DataFormatter formatter) {
        if (header == null) throw new BadRequestException("Template thiếu hàng tiêu đề");
        Map<String, Integer> result = new HashMap<>();
        for (Cell cell : header) {
            String value = formatter.formatCellValue(cell).trim();
            int separator = value.indexOf(" · ");
            if (separator > 0) result.put(value.substring(0, separator), cell.getColumnIndex());
        }
        return result;
    }

    private String fingerprint(List<GradeItem> items) {
        String source = items.stream().map(item -> item.getId() + "|" + item.getCode() + "|"
                + item.getName() + "|" + item.getWeight() + "|" + item.getEntryRole() + "|"
                + item.getAssessmentType() + "|" + item.getRequiredEntry())
                .reduce((left, right) -> left + ";" + right).orElse("");
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String normalizePassFail(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim().toUpperCase();
        if (value.equals("PASS") || value.equals("ĐẠT") || value.equals("DAT")) return "PASS";
        if (value.equals("FAIL") || value.equals("CHƯA ĐẠT") || value.equals("CHUA DAT")) return "FAIL";
        return null;
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

    private void finishSheet(Sheet sheet, int columns, int rows) {
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(0, rows - 1), 0, columns - 1));
        for (int index = 0; index < columns; index++) {
            sheet.autoSizeColumn(index);
            if (sheet.getColumnWidth(index) > 42 * 256) sheet.setColumnWidth(index, 42 * 256);
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record Scope(AcademicYear year, Semester semester, SchoolClass cls) {}
}
