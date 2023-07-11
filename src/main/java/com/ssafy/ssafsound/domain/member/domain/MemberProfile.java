package com.ssafy.ssafsound.domain.member.domain;

import com.ssafy.ssafsound.domain.member.dto.PutMemberProfileReqDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity(name="member_profile")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberProfile {

    @Id
    @Column(name = "member_id")
    private Long id;

    @Column
    private String introduce;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name="member_id")
    private Member member;

    public void changeIntroduceMyself(PutMemberProfileReqDto putMemberProfileReqDto) {
        this.introduce = putMemberProfileReqDto.getIntroduceMyself();
    }
}
