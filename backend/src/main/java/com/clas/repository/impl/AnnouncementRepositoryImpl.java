package com.clas.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.entity.Announcement;
import com.clas.mapper.AnnouncementMapper;
import com.clas.repository.AnnouncementRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class AnnouncementRepositoryImpl implements AnnouncementRepository {
    private final AnnouncementMapper announcementMapper;

    public AnnouncementRepositoryImpl(AnnouncementMapper announcementMapper) {
        this.announcementMapper = announcementMapper;
    }

    @Override
    public Announcement save(Announcement announcement) {
        if (announcement.getId() == null) {
            announcementMapper.insert(announcement);
        } else {
            announcementMapper.updateById(announcement);
        }
        return announcement;
    }

    @Override
    public Optional<Announcement> findById(Long id) {
        return Optional.ofNullable(announcementMapper.selectById(id));
    }

    @Override
    public List<Announcement> findPublishedList() {
        LocalDateTime now = LocalDateTime.now();
        return announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
            .eq(Announcement::getStatus, "PUBLISHED")
            .and(w -> w.isNull(Announcement::getStartAt).or().le(Announcement::getStartAt, now))
            .and(w -> w.isNull(Announcement::getEndAt).or().ge(Announcement::getEndAt, now))
            .orderByDesc(Announcement::getPinned)
            .orderByDesc(Announcement::getCreateTime)
            .orderByDesc(Announcement::getId));
    }

    @Override
    public List<Announcement> findAdminList() {
        return announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
            .orderByDesc(Announcement::getPinned)
            .orderByDesc(Announcement::getCreateTime)
            .orderByDesc(Announcement::getId));
    }

    @Override
    public void deleteById(Long id) {
        announcementMapper.deleteById(id);
    }
}
