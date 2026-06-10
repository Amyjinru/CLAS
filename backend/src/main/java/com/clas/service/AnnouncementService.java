package com.clas.service;

import com.clas.common.BusinessException;
import com.clas.dto.AnnouncementRequest;
import com.clas.entity.Announcement;
import com.clas.repository.AnnouncementRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AnnouncementService {
    private final AnnouncementRepository announcementRepository;

    public AnnouncementService(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    public List<Announcement> listPublished() {
        return announcementRepository.findPublishedList();
    }

    public List<Announcement> listAdmin() {
        return announcementRepository.findAdminList();
    }

    public Announcement create(AnnouncementRequest request) {
        Announcement announcement = new Announcement();
        announcement.setTitle(request.getTitle());
        announcement.setContent(request.getContent());
        announcement.setStatus("PUBLISHED");
        announcement.setPinned(request.getPinned() != null ? request.getPinned() : false);
        announcement.setStartAt(request.getStartAt() != null ? request.getStartAt() : LocalDateTime.now());
        announcement.setEndAt(request.getEndAt());
        announcement.setCreateTime(LocalDateTime.now());
        return announcementRepository.save(announcement);
    }

    public Announcement update(Long id, AnnouncementRequest request) {
        Announcement announcement = requireAnnouncement(id);
        announcement.setTitle(request.getTitle());
        announcement.setContent(request.getContent());
        announcement.setPinned(request.getPinned() != null ? request.getPinned() : announcement.getPinned());
        announcement.setStartAt(request.getStartAt() != null ? request.getStartAt() : announcement.getStartAt());
        announcement.setEndAt(request.getEndAt());
        return announcementRepository.save(announcement);
    }

    public void delete(Long id) {
        requireAnnouncement(id);
        announcementRepository.deleteById(id);
    }

    private Announcement requireAnnouncement(Long id) {
        return announcementRepository.findById(id)
            .orElseThrow(() -> new BusinessException("公告不存在"));
    }
}
