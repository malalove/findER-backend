package com.finder.repository;

import com.finder.domain.Bed;
import com.finder.idClass.BedId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface BedRepository extends JpaRepository<Bed, BedId> {
    @Query(value = "select b from Bed b where b.name = :name " +
            "and EXTRACT(YEAR FROM b.localDateTime) = EXTRACT(YEAR FROM :time) " +
            "and EXTRACT(MONTH FROM b.localDateTime) = EXTRACT(MONTH FROM :time) " +
            "and EXTRACT(DAY FROM b.localDateTime) = EXTRACT(DAY FROM :time) " +
            "and EXTRACT(HOUR FROM b.localDateTime) = EXTRACT(HOUR FROM :time) " +
            "and EXTRACT(MINUTE FROM b.localDateTime) = EXTRACT(MINUTE FROM :time)")
    Bed findByNameAndTime(String name, LocalDateTime time);

    @Query(value = "select b " +
            "from Bed b " +
            "where b.name = :name and " +
            "b.localDateTime between :beforeTime and :currentTime")
    List<Bed> findByRecent(String name, LocalDateTime beforeTime, LocalDateTime currentTime);
}