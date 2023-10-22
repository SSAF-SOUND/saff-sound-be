package com.ssafy.ssafsound.domain.post.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Getter
@Setter
public class GetPostHotSearchReqDto {
    @Size(min = 2)
    @NotBlank
    private String keyword;

    @Builder.Default
    private Long cursor;

    @Min(value = 10, message = "Size가 너무 작습니다.")
    private int size;
}
