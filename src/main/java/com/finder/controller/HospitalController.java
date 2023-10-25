package com.finder.controller;

import com.finder.dto.HospitalDetailDto;
import com.finder.dto.HospitalPreviewDto;
import com.finder.dto.MapResponseDto;
import com.finder.service.HospitalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/hospitals")
public class HospitalController {
    private final HospitalService hospitalService;

    // 지도 내 병원 위치 조회
    @GetMapping("/map")
    public ResponseEntity<List<MapResponseDto>> findHospitalMap(@RequestParam Double swLat, @RequestParam Double swLon,
                                                                @RequestParam Double neLat, @RequestParam Double neLon) {
        return ResponseEntity.ok(hospitalService.findHospitalMap(swLat, swLon, neLat, neLon));
    }

    // 병원 미리 보기
    @GetMapping("/preview/{id}")
    public ResponseEntity<HospitalPreviewDto> findHospitalPreview(@PathVariable Long id, @RequestParam Double lat, @RequestParam Double lon) {
        return ResponseEntity.ok(hospitalService.findHospitalPreview(id, lat, lon));
    }

    // 병원 목록 조회
    @GetMapping("/list")
    public ResponseEntity<List<HospitalPreviewDto>> findHospitalList(@RequestParam Double lat, @RequestParam Double lon) {
        return ResponseEntity.ok(hospitalService.findHospitalList(lat, lon));
    }

    // 병원 상세 조회
    @GetMapping("/details/{id}")
    public ResponseEntity<HospitalDetailDto> findHospitalDetail(@PathVariable Long id, @RequestParam Double lat, @RequestParam Double lon) {
        return ResponseEntity.ok(hospitalService.findHospitalDetail(id, lat, lon));
    }
}