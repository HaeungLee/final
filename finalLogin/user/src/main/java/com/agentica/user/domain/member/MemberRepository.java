package com.agentica.user.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmail(String email);
    boolean existsByEmail(String email);

        // 🔽 이메일 전체 목록 조회용 메서드 추가
    @Query("SELECT m.email FROM Member m")
    List<String> findAllEmails();
}
