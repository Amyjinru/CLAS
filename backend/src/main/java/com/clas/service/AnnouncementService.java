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

    public Announcement create(AnnouncementRequest request) {
        Announcement announcement = new Announcement();
        announcement.setTitle(request.title());
        announcement.setContent(request.content());
        announcement.setStatus("PUBLISHED");
        announcement.setCreateTime(LocalDateTime.now());
        return announcementRepository.save(announcement);
    }

    public Announcement update(Long id, AnnouncementRequest request) {
        Announcement announcement = requireAnnouncement(id);
        announcement.setTitle(request.title());
        announcement.setContent(request.content());
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
