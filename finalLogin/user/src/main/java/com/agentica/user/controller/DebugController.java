package com.agentica.user.controller;

import com.agentica.user.domain.member.Member;
import com.agentica.user.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // 모든 도메인에서 접근 허용 (개발용)
public class DebugController {
    
    private final MemberService memberService;
    
    // 특정 이메일 상태 확인
    @GetMapping("/member-status/{email}")
    public ResponseEntity<Map<String, Object>> debugMemberStatus(@PathVariable String email) {
        try {
            Member member = memberService.findByEmail(email);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "email", email,
                "member", Map.of(
                    "id", member.getId(),
                    "email", member.getEmail(),
                    "name", member.getName(),
                    "emailVerified", member.isEmailVerified(),
                    "provider", member.getProvider(),
                    "canUpdate", member.canUpdate()
                )
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                "success", false,
                "email", email,
                "message", "회원을 찾을 수 없습니다: " + e.getMessage()
            ));
        }
    }
}
