package com.clas.repository;

import com.clas.entity.Announcement;
import java.util.List;
import java.util.Optional;

/**
 * 公告数据访问接口，预留 MySQL 持久化实现。
 */
public interface AnnouncementRepository {
    Announcement save(Announcement announcement);

    Optional<Announcement> findById(Long id);

    List<Announcement> findPublishedList();

    void deleteById(Long id);
}
