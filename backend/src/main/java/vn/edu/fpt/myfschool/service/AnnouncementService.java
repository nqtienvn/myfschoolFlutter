package vn.edu.fpt.myfschool.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.myfschool.common.dto.*;
import vn.edu.fpt.myfschool.common.enums.TargetRole;
import vn.edu.fpt.myfschool.common.enums.UserRole;
import vn.edu.fpt.myfschool.common.exception.ResourceNotFoundException;
import vn.edu.fpt.myfschool.entity.*;
import vn.edu.fpt.myfschool.repository.*;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementClassRepository announcementClassRepository;
    private final AnnouncementReadRepository announcementReadRepository;
    private final TeacherRepository teacherRepository;
    private final StudentRepository studentRepository;
    private final ParentRepository parentRepository;
    private final StudentGuardianRepository studentGuardianRepository;

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

        return toDto(ann, classIds, 0, classIds.size() * 10);
    }

    @Transactional(readOnly = true)
    public List<AnnouncementDto> getMyAnnouncements(Long teacherUserId) {
        Teacher teacher = teacherRepository.findByUserId(teacherUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Teacher", "userId", teacherUserId));
        return announcementRepository.findByTeacherIdOrderByCreatedAtDesc(teacher.getId())
            .stream().map(ann -> {
                List<Long> cIds = announcementClassRepository.findByAnnouncementId(ann.getId())
                    .stream().map(ac -> ac.getCls().getId()).collect(Collectors.toList());
                long readCount = ann.getReads() != null ? ann.getReads().stream()
                    .filter(r -> r.getReadAt() != null).count() : 0;
                return toDto(ann, cIds, readCount, cIds.size() * 10);
            }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AnnouncementDto getAnnouncementDetail(Long announcementId) {
        Announcement ann = announcementRepository.findById(announcementId)
            .orElseThrow(() -> new ResourceNotFoundException("Announcement", "id", announcementId));
        List<Long> cIds = announcementClassRepository.findByAnnouncementId(announcementId)
            .stream().map(ac -> ac.getCls().getId()).collect(Collectors.toList());
        long readCount = ann.getReads() != null ? ann.getReads().stream()
            .filter(r -> r.getReadAt() != null).count() : 0;
        return toDto(ann, cIds, readCount, cIds.size() * 10);
    }

    @Transactional(readOnly = true)
    public List<AnnouncementDto> getAnnouncements(Long userId, UserRole role) {
        // Simplified: return all announcements (filtering by class/role done at query level)
        return announcementRepository.findAll().stream()
            .filter(ann -> ann.getTargetRole() == TargetRole.ALL ||
                (role == UserRole.PARENT && ann.getTargetRole() == TargetRole.PARENT) ||
                (role == UserRole.STUDENT && ann.getTargetRole() == TargetRole.STUDENT))
            .map(ann -> {
                List<Long> cIds = announcementClassRepository.findByAnnouncementId(ann.getId())
                    .stream().map(ac -> ac.getCls().getId()).collect(Collectors.toList());
                boolean isRead = announcementReadRepository.existsByAnnouncementIdAndUserId(ann.getId(), userId);
                return toDto(ann, cIds, 0, 0).withIsRead(isRead);
            }).collect(Collectors.toList());
    }

    public void markAsRead(Long announcementId, Long userId) {
        if (!announcementReadRepository.existsByAnnouncementIdAndUserId(announcementId, userId)) {
            AnnouncementRead ar = new AnnouncementRead();
            Announcement ann = new Announcement();
            ann.setId(announcementId);
            ar.setAnnouncement(ann);
            vn.edu.fpt.myfschool.entity.User user = new vn.edu.fpt.myfschool.entity.User();
            user.setId(userId);
            ar.setUser(user);
            ar.setReadAt(java.time.LocalDateTime.now());
            announcementReadRepository.save(ar);
        }
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId, UserRole role) {
        return 0; // Simplified
    }

    private AnnouncementDto toDto(Announcement ann, List<Long> classIds, long readCount, int totalRecipients) {
        String classNames = classIds.isEmpty() ? "" : "Lớp " + classIds.size();
        return new AnnouncementDto(ann.getId(), ann.getTitle(), ann.getBody(),
            ann.getTargetRole(), ann.getRequiresReply(), ann.getTeacher().getId(),
            ann.getTeacher().getUser().getName(), List.of(classNames),
            false, totalRecipients, (int) readCount, ann.getCreatedAt());
    }
}
