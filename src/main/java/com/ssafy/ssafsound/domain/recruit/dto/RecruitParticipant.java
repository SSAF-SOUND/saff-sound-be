package com.ssafy.ssafsound.domain.recruit.dto;

import com.ssafy.ssafsound.domain.recruit.domain.RecruitLimitation;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class RecruitParticipant {
    private String recruitType;
    private int limit;
    private List<Participant> members;

    public static RecruitParticipant from(RecruitLimitation recruitLimitation) {
        return RecruitParticipant.builder()
                .recruitType(recruitLimitation.getType().getName())
                .limit(recruitLimitation.getLimitation())
                .members(new ArrayList<>())
                .build();
    }

    public void addParticipant(String nickName, Boolean isMajor) {
        members.add(Participant.builder().nickName(nickName).isMajor(isMajor).build());
    }

    public void addRegister(String nickName, Boolean isMajor) {
        limit++;
        this.addParticipant(nickName, isMajor);
    }
}
