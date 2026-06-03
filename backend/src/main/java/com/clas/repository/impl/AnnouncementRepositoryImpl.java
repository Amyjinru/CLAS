package com.clas.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.clas.entity.Announcement;
import com.clas.mapper.AnnouncementMapper;
import com.clas.repository.AnnouncementRepository;
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
        return announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
            .eq(Announcement::getStatus, "PUBLISHED")
            .orderByDesc(Announcement::getCreateTime));
    }

    @Override
    public void deleteById(Long id) {
        announcementMapper.deleteById(id);
    }
}
