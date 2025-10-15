package com.careup.ordering.domain.member.service;


import com.careup.ordering.domain.member.dto.request.FindCustomerIdRequest;
import com.careup.ordering.domain.member.entity.Member;
import com.careup.ordering.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerIdentityService {

    private final MemberRepository memberRepository;

    /** 이름 + 생년월일 + 닉네임으로 고객 아이디(이메일/휴대폰) 찾기 */
    public Result findCustomerId(FindCustomerIdRequest req) {
        Member m = memberRepository
                .findByNameAndBirthdayAndNickname(
                        req.getName().trim(),
                        req.getBirthday(),
                        req.getNickname().trim()
                )
                .orElseThrow(() -> new IllegalArgumentException("일치하는 회원이 없습니다."));

        if (!"N".equalsIgnoreCase(m.getIsDelYn())) {
            throw new IllegalArgumentException("비활성화된 계정입니다.");
        }
        return new Result(m.getEmail(), m.getPhone());
    }

    public record Result(String email, String phone) {}
}
