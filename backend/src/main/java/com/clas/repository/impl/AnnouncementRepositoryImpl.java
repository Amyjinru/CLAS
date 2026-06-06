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
        // id 为空表示新公告；已有 id 时走更新，方便 Service 复用同一个保存入口。
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
        // 只给用户端展示已发布公告，最新发布的排在前面。
        return announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
            .eq(Announcement::getStatus, "PUBLISHED")
            .orderByDesc(Announcement::getCreateTime)
            .orderByDesc(Announcement::getId));
    }

    @Override
    public void deleteById(Long id) {
        announcementMapper.deleteById(id);
    }
}
