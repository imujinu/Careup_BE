package com.careup.ordering.domain.member.entity;

import com.careup.ordering.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "social_account",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_social_provider_id", columnNames = {"provider", "social_id"})
        },
        indexes = {
                @Index(name = "idx_social_member", columnList = "member_id")
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SocialAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_account_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false)
    private SocialProvider provider;

    @Column(name = "social_id", nullable = false, length = 120)
    private String socialId;

    @Column(name = "profile_image_url", length = 300)
    private String profileImageUrl;

    /** Google revocation 용으로 보관(있을 때만). Kakao는 AdminKey로 언링크하므로 불필요. */
    @Column(name = "provider_refresh_token", length = 512) // 300 → 512
    private String providerRefreshToken;

    @Builder
    public SocialAccount(Member member, SocialProvider provider, String socialId, String profileImageUrl, String providerRefreshToken) {
        this.member = member;
        this.provider = provider;
        this.socialId = socialId;
        this.profileImageUrl = profileImageUrl;
        this.providerRefreshToken = providerRefreshToken;
    }

    public void updateRefreshToken(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            this.providerRefreshToken = refreshToken;
        }
    }

    public void clearRefreshToken() {
        this.providerRefreshToken = null;
    }
}
