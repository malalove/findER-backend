package com.finder.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class KakaoMobilityService {
    @Value("${kakao.key}")
    private String REST_KEY;

    private Map<String, String> map = new HashMap<>();

    private JSONObject jsonObject = new JSONObject();

    public Map<String, String> requestKakaoMobilityApi(Double originLat, Double originLon, Double destinationLat, Double destinationLon) {
        String urlStr =
                "https://apis-navi.kakaomobility.com/v1/directions?origin=" + originLon + "," + originLat +
                        "&destination=" + destinationLon + "," + destinationLat +
                        "&waypoints=&priority=RECOMMEND&car_fuel=GASOLINE&car_hipass=false&alternatives=false&road_details=false";

        // API 요청
        requestAPI(urlStr);

        // 응답 데이터 내 거리, 도착 예정 시간 정보 매핑
        responseMapping();

        return map;
    }

    // API 요청
    private void requestAPI(String urlStr) {
        BufferedReader br = null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            URL url = new URL(urlStr);

            // API 요청
            URLConnection conn = url.openConnection();
            // 인증키 등록
            conn.setRequestProperty("Authorization", "KakaoAK " + REST_KEY);

            // 응답 데이터를 JSONObject로 변환
            br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            if(br != null) jsonObject = mapper.readValue(br, JSONObject.class);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                br.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 응답 데이터 내 거리, 도착 예정 시간 정보 매핑
    private void responseMapping() {
        ArrayList<LinkedHashMap> routes = (ArrayList) jsonObject.get("routes");
        LinkedHashMap routesMap = routes.get(0);
        LinkedHashMap summaryMap = (LinkedHashMap) routesMap.get("summary");

        // 거리, 소요 시간 조회
        Double distance = (Integer) summaryMap.get("distance") + 0.;
        Integer duration = (Integer) summaryMap.get("duration");

        // 거리, 도착 예정 시간 조회
        calculate(distance, duration);
    }

    // 거리, 도착 예정 시간 조회
    private void calculate(Double distance, Integer duration) {
        LocalDateTime now = LocalDateTime.now();
        int minute = duration / 60;
        int arriveHour = now.getHour();
        int arriveMinute = now.getMinute() + minute;

        if (arriveMinute >= 60) {
            arriveHour += (arriveMinute / 60);
            arriveMinute = (arriveMinute % 60);
        }

        String arriveTime;
        if(arriveHour >= 24) {
            arriveHour -= 24;
            arriveTime = String.format("오전 %d시 %d분", arriveHour, arriveMinute);
        } else if (arriveHour >= 12) {
            arriveTime = (arriveHour == 12) ? String.format("오후 %d시 %d분", arriveHour, arriveMinute)
                    : String.format("오후 %d시 %d분", arriveHour - 12, arriveMinute);
        } else {
            arriveTime = String.format("오전 %d시 %d분", arriveHour, arriveMinute);
        }

        distance = Math.round((distance / 1000) * 10.0) / 10.0;

        map.put("distance", distance.toString());
        map.put("arriveTime", arriveTime);
    }
}