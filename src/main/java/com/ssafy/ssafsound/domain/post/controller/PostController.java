package com.ssafy.ssafsound.domain.post.controller;

import com.ssafy.ssafsound.domain.auth.dto.AuthenticatedMember;
import com.ssafy.ssafsound.domain.auth.validator.Authentication;
import com.ssafy.ssafsound.domain.post.dto.GetPostDetailResDto;
import com.ssafy.ssafsound.domain.post.dto.GetPostResDto;
import com.ssafy.ssafsound.domain.post.dto.PostPostReportReqDto;
import com.ssafy.ssafsound.domain.post.dto.PostPostWriteReqDto;
import com.ssafy.ssafsound.domain.post.dto.*;
import com.ssafy.ssafsound.domain.post.service.PostService;
import com.ssafy.ssafsound.global.common.response.EnvelopeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    @GetMapping
    public EnvelopeResponse<GetPostResDto> findPosts(@RequestParam Long boardId, Pageable pageable) {
        return EnvelopeResponse.<GetPostResDto>builder()
                .data(postService.findPosts(boardId, pageable))
                .build();
    }

    @GetMapping("/{postId}")
    public EnvelopeResponse<GetPostDetailResDto> findPost(@PathVariable Long postId, AuthenticatedMember authenticatedMember) {
        return EnvelopeResponse.<GetPostDetailResDto>builder()
                .data(postService.findPost(postId, authenticatedMember.getMemberId()))
                .build();
    }

    @PostMapping("/{postId}/like")
    public EnvelopeResponse<Void> likePost(@PathVariable Long postId, AuthenticatedMember authenticatedMember) {
        postService.likePost(postId, authenticatedMember.getMemberId());

        return EnvelopeResponse.<Void>builder()
                .build();
    }

    @PostMapping("/{postId}/scrap")
    public EnvelopeResponse<Void> scrapPost(AuthenticatedMember authenticatedMember, @PathVariable Long postId) {
        postService.scrapPost(postId, authenticatedMember.getMemberId());

        return EnvelopeResponse.<Void>builder()
                .build();
    }

    @PostMapping("/{postId}/report")
    public EnvelopeResponse<Long> reportPost(AuthenticatedMember authenticatedMember, @PathVariable Long postId,
                                             @Valid @RequestBody PostPostReportReqDto postPostReportReqDto) {
        return EnvelopeResponse.<Long>builder()
                .data(postService.reportPost(postId, authenticatedMember.getMemberId(), postPostReportReqDto.getContent()))
                .build();
    }

    @PostMapping
    public EnvelopeResponse<Long> writePost(@Valid @RequestBody PostPostWriteReqDto postPostWriteReqDto,
                                            @RequestParam Long boardId, AuthenticatedMember authenticatedMember) {
        return EnvelopeResponse.<Long>builder()
                .data(postService.writePost(boardId, authenticatedMember.getMemberId(), postPostWriteReqDto))
                .build();
    }

    @DeleteMapping("/{postId}")
    public EnvelopeResponse<Long> deletePost(@PathVariable Long postId, AuthenticatedMember authenticatedMember) {
        return EnvelopeResponse.<Long>builder()
                .data(postService.deletePost(postId, authenticatedMember.getMemberId()))
                .build();
    }

    @PutMapping("/{postId}")
    public EnvelopeResponse<Long> updatePost(@Valid @RequestBody PostPutUpdateReqDto postPutUpdateReqDto,
                                             @PathVariable Long postId, AuthenticatedMember authenticatedMember) {
        return EnvelopeResponse.<Long>builder()
                .data(postService.updatePost(postId, authenticatedMember.getMemberId(), postPutUpdateReqDto))
                .build();
    }

    @GetMapping("/hot")
    public EnvelopeResponse<GetPostHotResDto> findHotPosts(Pageable pageable) {
        return EnvelopeResponse.<GetPostHotResDto>builder()
                .data(postService.findHotPosts(pageable))
                .build();
    }

    @GetMapping("/my")
    public EnvelopeResponse<GetPostMyResDto> findMyPosts(Pageable pageable, AuthenticatedMember authenticatedMember) {
        return EnvelopeResponse.<GetPostMyResDto>builder()
                .data(postService.findMyPosts(pageable, authenticatedMember.getMemberId()))
                .build();
    }

    @GetMapping("/search")
    public EnvelopeResponse<GetPostResDto> searchPosts(@Valid GetPostSearchReqDto getPostSearchReqDto, Pageable pageable) {
        return EnvelopeResponse.<GetPostResDto>builder()
                .data(postService.searchPosts(getPostSearchReqDto.getBoardId(), getPostSearchReqDto.getKeyword(), pageable))
                .build();
    }

    @GetMapping("/hot/search")
    public EnvelopeResponse<GetPostHotResDto> searchHotPosts(@Valid GetPostHotSearchReqDto getPostHotSearchReqDto, Pageable pageable) {
        return EnvelopeResponse.<GetPostHotResDto>builder()
                .data(postService.searchHotPosts(getPostHotSearchReqDto.getKeyword(), pageable))
                .build();
    }
}