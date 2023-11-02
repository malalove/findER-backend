package com.finder.service;

import com.finder.domain.Bed;
import com.finder.dto.BedDataDto;
import com.finder.repository.BedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BedService {
    private final BedRepository bedRepository;

    // 현재 시간 기준 응급실 병상 수 조회
    public Integer findByNameAndTime(String name) {
        LocalDateTime now = LocalDateTime.now().minusMinutes(1);
        Bed bed = bedRepository.findByNameAndTime(name, now);
        bed = (bed == null) ? bedRepository.findByNameAndTime(name, now.minusMinutes(1)) : bed;

        return (bed == null) ? 0 : bed.getCount();
    }

    // 최근 2시간 기준 응급실 병상 수 조회
    public BedDataDto findByRecent(String name) {
        LocalDateTime currentTime = LocalDateTime.now().minusMinutes(1);
        LocalDateTime twoHourAgoTime = currentTime.minusHours(2);

        // 최근 2시간 기준 병상 수 데이터
        List<Bed> twoHourAgoBeds = bedRepository.findByRecent(name, twoHourAgoTime, currentTime);

        // 시간 순 오름차순 정렬
        Collections.sort(twoHourAgoBeds, (Bed o1, Bed o2) -> o1.getLocalDateTime().compareTo(o2.getLocalDateTime()));

        // 병상 이용 가능 시간(분) 조회
        Integer totalMinute = getTotalMinute(twoHourAgoBeds);
        // 병상 이용 가능 시간 문자열 매핑
        String availableTime = availableTimeToStringMap(totalMinute);
        // 병상 이용 가능 시간 비율 매핑
        Double percent = availableTimeToPercent(totalMinute);
        Double otherPercent = Math.round((100 - percent) * 10.0) / 10.0;

        // 최근 2시간 기준 15분 간격 병상 수 조회
        List<Integer> bedIntervalList = getBedIntervalList(twoHourAgoBeds);

        // 트래커 미 실행 시
        if(bedIntervalList.size() < 8) {
            Integer size = bedIntervalList.size();
            for (int i = 0; i <= 8 - size; i++) {
                bedIntervalList.add(0);
            }
        } else if(bedIntervalList.size() == 8) { // 현재 시간 데이터가 비어있을 때
            // 최근 병상 수 저장
            bedIntervalList.add(Math.max(0, twoHourAgoBeds.get(twoHourAgoBeds.size() - 1).getCount()));
        }

        return new BedDataDto(availableTime, percent, otherPercent, bedIntervalList);
    }

    // 병상을 이용 가능했던 시간 조회 (분 단위)
    private Integer getTotalMinute(List<Bed> twoAgoBeds) {
        Integer totalMinute = 0;
        Integer size = twoAgoBeds.size();
        LocalDateTime oneMinuteAgo = (size > 0) ? twoAgoBeds.get(0).getLocalDateTime().minusMinutes(1) : null;

        for (int i = 0; i < size; i++) {
            Bed bed = twoAgoBeds.get(i);
            if (localDateTimeEq(bed.getLocalDateTime(), oneMinuteAgo.plusMinutes(1)) && bed.getCount() > 0) {
                totalMinute += 1;
            } else if (i - 1 > 0) { // 중간에 데이터가 없을 경우
                // 2분 전 병상 수 저장
                if(twoAgoBeds.get(i - 1).getCount() > 0) totalMinute +=1;
                // 현재 병상 수 저장
                if(bed.getCount() > 0) totalMinute += 1;
            }
            oneMinuteAgo = bed.getLocalDateTime();
        }

        return totalMinute;
    }

    private String availableTimeToStringMap(Integer totalMinute) {
        Integer hour = totalMinute / 60;
        Integer minute = totalMinute % 60;

        return String.format("%d시간 %d분", hour, minute);
    }

    private Double availableTimeToPercent(Integer totalMinute) {
        Double percent = (totalMinute / 120.) * 100;
        percent = Math.round(percent * 10.0) / 10.0;

        return percent;
    }

    // 최근 2시간 기준 15분 간격 병상 수 조회
    private List<Integer> getBedIntervalList(List<Bed> twoAgoBeds) {
        LocalDateTime currentTime = (twoAgoBeds.size() > 0) ? twoAgoBeds.get(0).getLocalDateTime() : null;
        Integer size = twoAgoBeds.size();
        List<Integer> bedIntervalList = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            Bed bed = twoAgoBeds.get(i);
            LocalDateTime oneMinuteAgo = bed.getLocalDateTime().minusMinutes(1);

            // 중간에 데이터가 없을 경우
            if(localDateTimeEq(oneMinuteAgo, currentTime)) {
                if((i - 1 >= 0) && localDateTimeEq(twoAgoBeds.get(i - 1).getLocalDateTime(), oneMinuteAgo.minusMinutes(1))) { // 1분 전 데이터가 있는 경우
                    bedIntervalList.add(Math.max(0, twoAgoBeds.get(i - 1).getCount()));
                } else bedIntervalList.add(0); // 1분 전 데이터가 없을 경우
                currentTime = currentTime.plusMinutes(15);
            }

            // 데이터가 있는 경우
            if(localDateTimeEq(bed.getLocalDateTime(), currentTime)) {
                bedIntervalList.add(Math.max(0, bed.getCount()));
                currentTime = currentTime.plusMinutes(15);
            }
        }

        return bedIntervalList;
    }

    private Boolean localDateTimeEq(LocalDateTime t1, LocalDateTime t2) {
        return (t1.getHour() == t2.getHour() && t1.getMinute() == t2.getMinute()) ? true : false;
    }
}