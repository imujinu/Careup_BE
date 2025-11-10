package com.careup.ordering.domain.member.service;

import com.careup.ordering.domain.member.dto.response.MemberDetailDto;
import com.careup.ordering.domain.member.dto.response.MemberListDto;
import com.careup.ordering.domain.member.dto.response.MemberMyPageDto;
import com.careup.ordering.domain.member.entity.Member;
import com.careup.ordering.domain.member.repository.MemberRepository;
import com.careup.ordering.domain.order.dto.response.ProductViewCountResDto;
import com.careup.ordering.domain.product.entity.Product;
import com.careup.ordering.domain.product.entity.ProductViewLog;
import com.careup.ordering.domain.product.repository.ProductRepository;
import com.careup.ordering.domain.product.repository.ProductViewLogRepository;
import com.careup.ordering.domain.recomendation.dto.ProductViewResultDto;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MemberQueryService {

    private final MemberRepository memberRepository;
    private final ProductRepository productRepository;
    private final ProductViewLogRepository productViewLogRepository;
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

    @Transactional
    public void buyProduct(Long productId, Long memberId){
        Product product = productRepository.findById(productId).orElseThrow(()-> new EntityNotFoundException("존재 하지 않는 상품입니다."));
        Member member = memberRepository.findById(memberId).orElseThrow(()-> new EntityNotFoundException("존재 하지 않는 유저입니다."));
        ProductViewLog productViewLog = ProductViewLog.toEntity(product,member);
        productViewLogRepository.save(productViewLog);

    }

    @Transactional
    public ProductViewResultDto getProductId(Long memberId){
        Member member = memberRepository.findById(memberId).orElseThrow(()-> new EntityNotFoundException("존재 하지 않는 유저입니다."));

        Optional<ProductViewLog> viewLog = productViewLogRepository.findTopByMemberOrderByCreatedAtDesc(member);

        if(viewLog.isPresent()){

            return new ProductViewResultDto().builder()
                    .productId(viewLog.get().getProduct().getId())
                    .isExist(true)
                    .build();
        }else{
            return new ProductViewResultDto().builder()
                    .productId(null)
                    .isExist(false)
                    .build();
        }
    }

    @Transactional
    public void viewProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new EntityNotFoundException("상품이 존재하지 않습니다."));
        product.plusCount();
        log.info("[member][viewProduct] : 최근 상품 조회수 업데이트 완료");
    }
}
