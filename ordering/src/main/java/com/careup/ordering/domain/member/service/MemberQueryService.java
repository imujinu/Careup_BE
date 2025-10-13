package com.careup.ordering.domain.member.service;

import com.careup.ordering.domain.member.dto.response.MemberDetailDto;
import com.careup.ordering.domain.member.dto.response.MemberListDto;
import com.careup.ordering.domain.member.dto.response.MemberMyPageDto;
import com.careup.ordering.domain.member.entity.Member;
import com.careup.ordering.domain.member.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQueryService {

    private final MemberRepository memberRepository;

    public MemberMyPageDto getMyPage(Long memberId) {
        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("계정을 찾을 수 없습니다."));
        return MemberMyPageDto.from(m);
    }

    public Page<MemberListDto> getMemberList(Pageable pageable) {
        return memberRepository.findAll(pageable)
                .map(MemberListDto::from);
    }

    public MemberDetailDto getMemberDetail(Long memberId) {
        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> new EntityNotFoundException("계정을 찾을 수 없습니다."));
        return MemberDetailDto.from(m);
    }
}
