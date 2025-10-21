package com.careup.ordering.domain.member.entity;

import com.careup.ordering.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 50)
    private String email;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "nickname", nullable = false, unique = true, length = 10)
    private String nickname;

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @Column(name= "birthday", nullable = false)
    private LocalDate birthday;

    @Column(name = "phone", length = 15)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;

    /**
     * N: 정상 / Y: 소프트탈퇴(비활성)
     */
    @Column(name = "is_del_yn", nullable = false, length = 1)
    private String isDelYn = "N";

    @Builder
    public Member(String email, String password, String nickname, String name,
                  LocalDate birthday, String phone, Gender gender) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.name = name;
        this.birthday = birthday;
        this.phone = phone;
        this.gender = gender;
    }

    public void changePassword(String encoded) { this.password = encoded; }
    public void changeEmail(String email) { this.email = email; }
    public void changePhone(String phone) { this.phone = phone; }

    /** 프로필 일괄 변경(닉네임/이름/생일/성별) */
    public void changeProfile(String nickname, String name, LocalDate birthday, Gender gender) {
        this.nickname = nickname;
        this.name = name;
        this.birthday = birthday;
        this.gender = gender;
    }

    /** 소프트 삭제(탈퇴) → isDelYn = "Y" */
    public void deactivate() { this.isDelYn = "Y"; }

    /** 재활성화 → isDelYn = "N" */
    public void reactivate() { this.isDelYn = "N"; }

    /** 탈퇴 여부(Y면 true) */
    public boolean isDeactivated() { return "Y".equalsIgnoreCase(this.isDelYn); }
}
