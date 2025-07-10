package com.agentica.user.controller;

import com.agentica.user.dto.MemberResponse;
import com.agentica.user.dto.PasswordVerifyRequest;
import com.agentica.user.dto.ProfileUpdateRequest;
import com.agentica.user.response.ApiResponse;
import com.agentica.user.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    // 회원 정보 조회
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MemberResponse>> getMyInfo(Authentication authentication) {
        String email = authentication.getName();
        MemberResponse memberResponse = memberService.getMemberInfo(email);
        return ResponseEntity.ok(ApiResponse.success(memberResponse));
    }

    // 비밀번호 확인 (프로필 수정 전 인증용)
    @PostMapping("/verify-password")
    public ResponseEntity<ApiResponse<String>> verifyPassword(
            @Valid @RequestBody PasswordVerifyRequest request,
            Authentication authentication) {
        
        boolean isValid = memberService.verifyPassword(authentication.getName(), request.getPassword());
        
        if (isValid) {
            return ResponseEntity.ok(ApiResponse.success("비밀번호가 확인되었습니다."));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.error("비밀번호가 일치하지 않습니다."));
        }
    }
    
    // 프로필 수정 (이름, 비밀번호)
    @PutMapping("/update")
    public ResponseEntity<ApiResponse<String>> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request,
            Authentication authentication) {
        
        memberService.updateProfile(authentication.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("프로필이 성공적으로 수정되었습니다."));
    }
}
