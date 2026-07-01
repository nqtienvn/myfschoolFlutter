package vn.edu.fpt.myfschool.service.impl;

import vn.edu.fpt.myfschool.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.TargetRole;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.exception.ForbiddenException;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.*;
import vn.edu.fpt.myfschool.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service("announcementService")
@RequiredArgsConstructor
@Transactional
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementClassRepository announcementClassRepository;
    private final AnnouncementReadRepository announcementReadRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final StudentGuardianRepository studentGuardianRepository;

    @Override
    public AnnouncementDto createAnnouncement(String title, String body, TargetRole targetRole,
                                               boolean requiresReply, List<Long> classIds, Long teacherUserId) {
        Teacher teacher = teacherRepository.findByUserId(teacherUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Teacher", "userId", teacherUserId));

        Announcement ann = new Announcement();
        ann.setTeacher(teacher);
        ann.setTitle(title);
        ann.setBody(body);
        ann.setTargetRole(targetRole);
        ann.setRequiresReply(requiresReply);
        ann = announcementRepository.save(ann);

        for (Long classId : classIds) {
            AnnouncementClass ac = new AnnouncementClass();
            ac.setAnnouncement(ann);
            SchoolClass cls = new SchoolClass();
            cls.setId(classId);
            ac.setCls(cls);
            announcementClassRepository.save(ac);
        }

        return toDto(ann, false);
    }

    @Transactional(readOnly = true)
    @Override
    public List<AnnouncementDto> getMyAnnouncements(Long teacherUserId) {
        Teacher teacher = teacherRepository.findByUserId(teacherUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Teacher", "userId", teacherUserId));
        return announcementRepository.findByTeacherIdOrderByCreatedAtDesc(teacher.getId())
            .stream().map(ann -> toDto(ann, false)).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public AnnouncementDto getAnnouncementDetail(Long announcementId, Long userId, UserRole role) {
        Announcement ann = announcementRepository.findById(announcementId)
            .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", announcementId));
        if (!canViewAnnouncement(ann, userId, role)) {
            throw new ForbiddenException("Bạn không có quyền xem thông báo này");
        }
        boolean isRead = announcementReadRepository.existsByAnnouncementIdAndUserId(ann.getId(), userId);
        return toDto(ann, isRead);
    }

    @Transactional(readOnly = true)
    @Override
    public List<AnnouncementDto> getAnnouncements(Long userId, UserRole role) {
        List<Long> classIds = getVisibleClassIds(userId, role);
        if (classIds.isEmpty()) return List.of();

        return announcementRepository.findByClassesAndTargetRoles(classIds, targetRolesFor(role)).stream()
            .map(ann -> {
                boolean isRead = announcementReadRepository.existsByAnnouncementIdAndUserId(ann.getId(), userId);
                return toDto(ann, isRead);
            }).collect(Collectors.toList());
    }

    @Override
    public void markAsRead(Long announcementId, Long userId, UserRole role) {
        Announcement ann = announcementRepository.findById(announcementId)
            .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", announcementId));
        if (!canViewAnnouncement(ann, userId, role)) {
            throw new ForbiddenException("Bạn không có quyền đọc thông báo này");
        }

        AnnouncementRead ar = announcementReadRepository.findByAnnouncementIdAndUserId(announcementId, userId)
            .orElseGet(() -> {
                AnnouncementRead read = new AnnouncementRead();
                read.setAnnouncement(ann);
                User user = new User();
                user.setId(userId);
                read.setUser(user);
                return read;
            });
        ar.setReadAt(LocalDateTime.now());
        announcementReadRepository.save(ar);
    }

    @Transactional(readOnly = true)
    @Override
    public long getUnreadCount(Long userId, UserRole role) {
        List<Long> classIds = getVisibleClassIds(userId, role);
        if (classIds.isEmpty()) return 0;
        return announcementRepository.countUnread(classIds, targetRolesFor(role), userId);
    }

    private boolean canViewAnnouncement(Announcement ann, Long userId, UserRole role) {
        if (role == UserRole.TEACHER) {
            Teacher teacher = teacherRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher", "userId", userId));
            return ann.getTeacher().getId().equals(teacher.getId());
        }

        if (!targetRolesFor(role).contains(ann.getTargetRole())) {
            return false;
        }

        List<Long> visibleClassIds = getVisibleClassIds(userId, role);
        if (visibleClassIds.isEmpty()) return false;
        return ann.getAnnouncementClasses().stream()
            .anyMatch(ac -> visibleClassIds.contains(ac.getCls().getId()));
    }

    private List<Long> getVisibleClassIds(Long userId, UserRole role) {
        if (role == UserRole.STUDENT) {
            Student student = studentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Student", "userId", userId));
            return student.getCurrentClass() == null ? List.of() : List.of(student.getCurrentClass().getId());
        }
        if (role == UserRole.PARENT) {
            Parent parent = parentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent", "userId", userId));
            return studentGuardianRepository.findByGuardianId(parent.getId()).stream()
                .map(StudentGuardian::getStudent)
                .filter(student -> student.getCurrentClass() != null)
                .map(student -> student.getCurrentClass().getId())
                .distinct()
                .collect(Collectors.toList());
        }
        return List.of();
    }

    private List<TargetRole> targetRolesFor(UserRole role) {
        if (role == UserRole.PARENT) return List.of(TargetRole.PARENT, TargetRole.ALL);
        if (role == UserRole.STUDENT) return List.of(TargetRole.STUDENT, TargetRole.ALL);
        return List.of();
    }

    private AnnouncementDto toDto(Announcement ann, boolean isRead) {
        List<String> classNames = announcementClassRepository.findByAnnouncementId(ann.getId()).stream()
            .map(ac -> ac.getCls().getName())
            .distinct()
            .collect(Collectors.toList());
        long readCount = announcementReadRepository.countByAnnouncementIdAndReadAtIsNotNull(ann.getId());
        return new AnnouncementDto(ann.getId(), ann.getTitle(), ann.getBody(),
            ann.getTargetRole(), ann.getRequiresReply(), ann.getTeacher().getId(),
            ann.getTeacher().getUser().getName(), classNames,
            isRead, classNames.size() * 10, (int) readCount, ann.getCreatedAt());
    }
}
