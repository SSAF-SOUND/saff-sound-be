package com.ssafy.ssafsound.domain.recruit.controller;

import com.ssafy.ssafsound.domain.auth.dto.AuthenticatedMember;
import com.ssafy.ssafsound.domain.recruit.dto.GetRecruitDetailResDto;
import com.ssafy.ssafsound.domain.recruit.dto.PostRecruitReqDto;
import com.ssafy.ssafsound.domain.recruit.service.RecruitService;
import com.ssafy.ssafsound.global.common.response.EnvelopeResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/recruits")
@RequiredArgsConstructor
public class RecruitController {

    private final RecruitService recruitService;

    @PostMapping
    public EnvelopeResponse<Void> saveRecruit(AuthenticatedMember memberInfo, @Valid @RequestBody PostRecruitReqDto recruitReqDto) {
        recruitService.saveRecruit(memberInfo, recruitReqDto);
        return EnvelopeResponse.<Void>builder().build();
    }

    @PostMapping("/{recruitId}/scrap")
    public EnvelopeResponse<Void> toggleRecruitScrap(@PathVariable Long recruitId, AuthenticatedMember memberInfo) {
        recruitService.toggleRecruitScrap(recruitId, memberInfo.getMemberId());
        return EnvelopeResponse.<Void>builder().build();
    }

    @GetMapping("/{recruitId}")
    public EnvelopeResponse<GetRecruitDetailResDto> getRecruitDetail(@PathVariable Long recruitId) {
        return EnvelopeResponse.<GetRecruitDetailResDto>builder()
                .data(recruitService.getRecruitDetail(recruitId))
                .build();
    }
}
