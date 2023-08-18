package com.ssafy.ssafsound.global.util.fixture;

import com.ssafy.ssafsound.domain.recruitcomment.domain.RecruitComment;
import com.ssafy.ssafsound.domain.recruitcomment.dto.GetRecruitCommentsResDto;
import com.ssafy.ssafsound.domain.recruitcomment.dto.PatchRecruitCommentReqDto;
import com.ssafy.ssafsound.domain.recruitcomment.dto.PostRecruitCommentReqDto;
import com.ssafy.ssafsound.domain.recruitcomment.dto.PostRecruitCommentResDto;

import java.util.List;

public class RecruitCommentFixture {

    private static final Long DEFAULT_COMMENT_GROUP = -1L;

    public static final PostRecruitCommentReqDto POST_RECRUIT_COMMENT_REQ_DTO = new PostRecruitCommentReqDto("공모전 수상이 목표인가요?", DEFAULT_COMMENT_GROUP);

    public static final PostRecruitCommentResDto POST_RECRUIT_COMMENT_RES_DTO
            = new PostRecruitCommentResDto(1L, "공모전 수상이 목표인가요?", MemberFixture.MEMBER_TIM.getId(), MemberFixture.MEMBER_TIM.getNickname(), 1L);

    public static final PatchRecruitCommentReqDto PATCH_RECRUIT_COMMENT_REQ_DTO = new PatchRecruitCommentReqDto("덧글 수정");

    public static final RecruitComment RECRUIT_COMMENT_1 = RecruitComment.builder()
            .id(1L)
            .content("공모전 수상이 목표인가요?")
            .deletedComment(false)
            .recruit(RecruitFixture.RECRUIT_1)
            .member(MemberFixture.MEMBER_TIM)
            .build();

    static {
        RECRUIT_COMMENT_1.setCommentGroup(RECRUIT_COMMENT_1);
    }

    public static GetRecruitCommentsResDto GET_RECRUIT_RES_DTO = GetRecruitCommentsResDto.from(List.of(RECRUIT_COMMENT_1));
}
