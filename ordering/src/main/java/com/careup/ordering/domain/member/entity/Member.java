package com.careup.ordering.domain.member.entity;

import com.careup.ordering.common.domain.BaseTimeEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "member",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_member_phone", columnNames = {"phone"})
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Member extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 100) // 50 → 100
    private String email;

    @Column(name = "password", nullable = false, length = 100)
    private String password;

    @Column(name = "nickname", nullable = false, unique = true, length = 10)
    private String nickname;

    @Column(name = "name", nullable = false, length = 30)
    private String name;

    @Column(name= "birthday", nullable = false)
    private LocalDate birthday;

    @Column(name = "phone", nullable = false, unique = true, length = 15) // NOT NULL + UNIQUE
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;

    @Column(name = "zipcode", nullable = false, length = 10)
    private String zipcode;

    @Column(name = "address", nullable = false, length = 200)
    private String address;

    @Column(name = "address_detail", nullable = false, length = 200)
    private String addressDetail;

    /**
     * N: 정상 / Y: 소프트탈퇴(비활성)
     */
    @Column(name = "is_del_yn", nullable = false, length = 1)
    private String isDelYn = "N";

    @Builder
    public Member(String email, String password, String nickname, String name,
                  LocalDate birthday, String phone, Gender gender,
                  String zipcode, String address, String addressDetail) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.name = name;
        this.birthday = birthday;
        this.phone = phone;
        this.gender = gender;
        this.zipcode = zipcode;
        this.address = address;
        this.addressDetail = addressDetail;
    }

    public void changePassword(String encoded) { this.password = encoded; }
    public void changeEmail(String email) { this.email = email; }
    public void changePhone(String phone) { this.phone = phone; }

    public void changeProfile(String nickname, String name, LocalDate birthday, Gender gender) {
        this.nickname = nickname;
        this.name = name;
        this.birthday = birthday;
        this.gender = gender;
    }

    public void changeAddress(String zipcode, String address, String addressDetail) {
        this.zipcode = zipcode;
        this.address = address;
        this.addressDetail = addressDetail;
    }

    public void deactivate() { this.isDelYn = "Y"; }
    public void reactivate() { this.isDelYn = "N"; }
    public boolean isDeactivated() { return "Y".equalsIgnoreCase(this.isDelYn); }
}
