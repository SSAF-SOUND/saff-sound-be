package com.ssafy.ssafsound.domain.post.dto;

import lombok.*;

import javax.validation.constraints.Min;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetPostMyReqDto {
    @Builder.Default
    private Long cursor = -1L;

    @Min(value = 10, message = "Size가 너무 작습니다.")
    private int size;
}
