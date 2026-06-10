// 파일: HealthController.java
// 역할: ALB 헬스체크용 엔드포인트 (인증 없이 200 반환)
// 호출: ALB Target Group health check -> GET /api/v1/health
// 주의: DB 등 외부 의존성을 넣지 않는다. "앱이 떠 있는가"만 확인하는 얕은(shallow) 체크.
//       DB 장애가 헬스체크 실패 -> 태스크 강제 종료로 번지는 연쇄 장애를 막기 위함.

package com.mindcompass.api.common.health;

import com.mindcompass.api.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {

    // ALB가 30초마다 호출한다. 무조건 200 OK를 반환하면 타겟이 healthy로 전환된다.
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        return ResponseEntity.ok(ApiResponse.success(Map.of("status", "UP")));
    }
}