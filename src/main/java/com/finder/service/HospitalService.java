package com.finder.service;

import com.finder.domain.Hospital;
import com.finder.dto.BedDataDto;
import com.finder.dto.HospitalDetailDto;
import com.finder.dto.HospitalPreviewDto;
import com.finder.dto.MapResponseDto;
import com.finder.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HospitalService {
    private final HospitalRepository hospitalRepository;

    private final KakaoMobilityService kakaoMobilityService;

    private final BedService bedService;

    // 지도 내 병원 위치 조회
    public List<MapResponseDto> findHospitalMap(Double swLat, Double swLon, Double neLat, Double neLon) {
        List<Hospital> hospitals = hospitalRepository.findHospitalMap(swLat, swLon, neLat, neLon);
        List<MapResponseDto> mapResponseDtos = hospitals.stream()
                .map(hospital -> new MapResponseDto(hospital.getId(), hospital.getLatitude(), hospital.getLongitude()))
                .collect(Collectors.toList());

        return mapResponseDtos;
    }

    // 병원 미리 보기 조회
    public HospitalPreviewDto findHospitalPreview(Long id, Double lat, Double lon) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 병원이 존재하지 않습니다."));
        // 병원의 거리, 도착 예정 시간, 병상 수 조회
        HospitalPreviewDto hospitalPreviewDto = getHospitalPreviewInfo(lat, lon, hospital);

        return hospitalPreviewDto;
    }

    // 병원의 거리, 도착 예정 시간, 병상 수 조회
    private HospitalPreviewDto getHospitalPreviewInfo(Double lat, Double lon, Hospital hospital) {
        // 거리, 도착 예정 시간 조회
        Map<String, String> map = kakaoMobilityService.requestKakaoMobilityApi(lat, lon, hospital.getLatitude(), hospital.getLongitude());
        // 병상 수 조회
        Integer hvec = bedService.findByNameAndTime(hospital.getName());
        hvec = (hvec == null || hvec < 0) ? 0 : hvec;

        return new HospitalPreviewDto(hospital.getId(), hospital.getName(), hospital.getAddress(), hospital.getRepresentativeContact(), hospital.getEmergencyContact(),
                hvec, Double.parseDouble(map.get("distance")), map.get("arriveTime"));
    }

    // 병원 목록 조회
    public List<HospitalPreviewDto> findHospitalList(Double lat, Double lon) {
        // 5km 반경 병원 조회
        List<Hospital> nearbyHospitals = getNearbyHospitals(lat, lon);

        // 병원의 거리, 도착 예정 시간, 병상 수 조회
        List<HospitalPreviewDto> hospitalPreviewDtos = nearbyHospitals.stream()
                .map(nearbyHospital -> getHospitalPreviewInfo(lat, lon, nearbyHospital)).collect(Collectors.toList());

        // 거리 기준 오름차순 정렬
        Collections.sort(hospitalPreviewDtos, (HospitalPreviewDto o1, HospitalPreviewDto o2) -> Double.compare(o1.getDistance(), o2.getDistance()));

        return hospitalPreviewDtos;
    }

    // 5km 반경 병원 조회
    private List<Hospital> getNearbyHospitals(Double lat, Double lon) {
        List<Hospital> hospitals = hospitalRepository.findAll();
        List<Hospital> nearbyHospitals;

        // 병원 필터링
        nearbyHospitals = hospitals.stream()
                .filter(h -> h.getLatitude() != null && h.getLongitude() != null)
                .filter(h -> calculateStraightDistance(lat, lon, h.getLatitude(), h.getLongitude()) / 1000 <= 5)
                .collect(Collectors.toList());

        return nearbyHospitals;
    }

    // 직선 거리 계산
    public double calculateStraightDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        Double earthRadius = 6371.;
        // 위도 및 경도 차이
        Double dLat = Math.toRadians(lat2 - lat1);
        Double dLon = Math.toRadians(lon2 - lon1);

        // Haversine 공식 사용 거리 계산
        Double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        Double distance = earthRadius * c * 1000;

        return distance;
    }

    // 병원 상세 정보 조회
    public HospitalDetailDto findHospitalDetail(Long id, Double lat, Double lon) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("해당 병원이 존재하지 않습니다."));
        // 구급차, CT, MRI 여부 판단
        HashMap<String, Boolean> map = isAMBAndCTAndMRI(hospital);
        // 병원의 거리, 도착 예정 시간, 병상 수, 병상 데이터 조회
        HospitalDetailDto hospitalDetailDto = getHospitalDetailInfo(lat, lon, map.get("isAMB"), map.get("isCT"), map.get("isMRI"), hospital);

        return hospitalDetailDto;
    }


    // 구급차, CT, MRI 여부 판단
    private HashMap<String, Boolean> isAMBAndCTAndMRI(Hospital hospital) {
        HashMap<String, Boolean> map = new HashMap();
        map.put("isAMB", hospital.getAmbulance().equals("Y"));
        map.put("isCT", hospital.getCt().equals("Y"));
        map.put("isMRI", hospital.getMri().equals("Y"));

        return map;
    }

    // 병원의 거리, 도착 예정 시간, 병상 수, 병상 데이터 조회
    private HospitalDetailDto getHospitalDetailInfo(Double lat, Double lon,
                                  Boolean isAMB, Boolean isCT, Boolean isMRI, Hospital hospital) {
        // 거리, 도착 예정 시간 조회
        Map<String, String> map = kakaoMobilityService.requestKakaoMobilityApi(lat, lon, hospital.getLatitude(), hospital.getLongitude());

        // 병상 수, 병상 데이터 조회
        BedDataDto bedDataDto = bedService.findByRecent(hospital.getName());
        int hvec = bedDataDto.getTwoAgoList().get(8);
        hvec = (hvec < 0) ? 0 : hvec;

        String simpleAddress = "";
        simpleAddress = (hospital.getSimpleAddress() != null) ? hospital.getSimpleAddress() : simpleAddress;

        return new HospitalDetailDto(hospital.getName(), hospital.getAddress(), simpleAddress,
                hospital.getRepresentativeContact(), hospital.getEmergencyContact(), isAMB, isCT, isMRI,
                hvec, Double.parseDouble(map.get("distance")), map.get("arriveTime"), hospital.getLatitude(),
                hospital.getLongitude(), bedDataDto);
    }
}