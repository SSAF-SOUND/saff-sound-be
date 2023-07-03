package com.ssafy.ssafsound.domain.post.dto;

import com.ssafy.ssafsound.domain.post.domain.HotPost;
import com.ssafy.ssafsound.domain.post.domain.Post;
import com.ssafy.ssafsound.domain.post.domain.PostImage;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class GetHotPostElement {
    private String boardTitle;
    private String title;
    private String content;
    private int likeCount;
    private int commentCount;
    private LocalDateTime createAt;
    private Long memberId;
    private String nickname;
    private Boolean anonymous;
    private String thumbnail;

    public static GetHotPostElement from(HotPost hotPost) {
        Post post = hotPost.getPost();
        String thumbnail = findThumbnailUrl(post);
        Boolean anonymous = post.getAnonymous();

        return GetHotPostElement.builder()
                .boardTitle(post.getBoard().getTitle())
                .title(post.getTitle())
                .content(post.getContent())
                .likeCount(post.getLikes().size())
                .commentCount(post.getComments().size())
                .createAt(post.getCreatedAt())
                .memberId(anonymous ? -1 : post.getMember().getId())
                .nickname(anonymous ? "익명" : post.getMember().getNickname())
                .anonymous(anonymous)
                .thumbnail(thumbnail)
                .build();
    }

    private static String findThumbnailUrl(Post post) {
        List<PostImage> images = post.getImages();
        if (images.size() >= 1)
            return images.get(0).getImageUrl();
        return null;
    }
}