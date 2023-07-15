package com.ssafy.ssafsound.domain.post.service;

import com.ssafy.ssafsound.domain.board.exception.BoardErrorInfo;
import com.ssafy.ssafsound.domain.board.exception.BoardException;
import com.ssafy.ssafsound.domain.board.repository.BoardRepository;
import com.ssafy.ssafsound.domain.member.domain.Member;
import com.ssafy.ssafsound.domain.member.exception.MemberErrorInfo;
import com.ssafy.ssafsound.domain.member.exception.MemberException;
import com.ssafy.ssafsound.domain.member.repository.MemberRepository;
import com.ssafy.ssafsound.domain.post.domain.*;
import com.ssafy.ssafsound.domain.post.dto.GetPostDetailResDto;
import com.ssafy.ssafsound.domain.post.dto.GetPostResDto;
import com.ssafy.ssafsound.domain.post.exception.PostErrorInfo;
import com.ssafy.ssafsound.domain.post.exception.PostException;
import com.ssafy.ssafsound.domain.post.repository.*;
import com.ssafy.ssafsound.domain.meta.domain.MetaData;
import com.ssafy.ssafsound.domain.meta.domain.UploadDirectory;
import com.ssafy.ssafsound.domain.meta.dto.UploadFileInfo;
import com.ssafy.ssafsound.domain.post.dto.*;
import com.ssafy.ssafsound.infra.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostService {
    @Value("${spring.constant.post.HOT_POST_LIKES_THRESHOLD}")
    private Long HOT_POST_LIKES_THRESHOLD;

    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;
    private final StorageService awsS3StorageSerive;

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final HotPostRepository hotPostRepository;
    private final PostScrapRepository postScrapRepository;
    private final PostReportRepository postReportRepository;
    private final PostImageRepository postImageRepository;

    @Transactional(readOnly = true)
    public GetPostResDto findPosts(Long boardId, Pageable pageable) {
        if (!boardRepository.existsById(boardId)) {
            throw new BoardException(BoardErrorInfo.NO_BOARD);
        }

        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        List<Post> posts = postRepository.findWithDetailsByBoardId(boardId, pageRequest);

        return GetPostResDto.from(posts);
    }

    @Transactional(readOnly = true)
    public GetPostDetailResDto findPost(Long postId, Long memberId) {
        Post post = postRepository.findWithMemberAndImagesFetchById(postId)
                .orElseThrow(() -> new PostException(PostErrorInfo.NOT_FOUND_POST));

        if (memberId != null) {
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new MemberException(MemberErrorInfo.MEMBER_NOT_FOUND_BY_ID));
            return GetPostDetailResDto.of(post, member);
        }

        return GetPostDetailResDto.of(post, null);
    }

    @Transactional
    public void likePost(Long postId, Long memberId) {
        PostLike postLike = postLikeRepository.findByPostIdAndMemberId(postId, memberId)
                .orElse(null);
        togglePostLike(postId, memberId, postLike);
    }

    private void togglePostLike(Long postId, Long memberId, PostLike postLike) {
        if (postLike != null) {
            deleteLike(postLike);
            return;
        }
        saveLike(postId, memberId);
        if (isSelectedHotPost(postId)) {
            saveHotPost(postId);
        }
    }

    private void saveLike(Long postId, Long memberId) {
        PostLike postLike = PostLike.builder()
                .post(postRepository.getReferenceById(postId))
                .member(memberRepository.getReferenceById(memberId))
                .build();
        postLikeRepository.save(postLike);
    }

    private void deleteLike(PostLike postLike) {
        postLikeRepository.delete(postLike);
    }


    private boolean isSelectedHotPost(Long postId) {
        return postLikeRepository.countByPostId(postId) >= HOT_POST_LIKES_THRESHOLD;
    }

    private void saveHotPost(Long postId) {
        HotPost hotPost = HotPost.builder()
                .post(postRepository.findById(postId).
                        orElseThrow(() -> new PostException(PostErrorInfo.NOT_FOUND_POST)))
                .build();
        hotPostRepository.save(hotPost);
    }

    @Transactional
    public void deleteHotPostsUnderThreshold(Long threshold) {
        hotPostRepository.deleteHotPostsUnderThreshold(threshold);
    }

    @Transactional
    public void scrapPost(Long postId, Long memberId) {
        PostScrap postScrap = postScrapRepository.findByPostIdAndMemberId(postId, memberId)
                .orElse(null);
        togglePostScrap(postId, memberId, postScrap);
    }

    private void togglePostScrap(Long postId, Long memberId, PostScrap postScrap) {
        if (postScrap != null) {
            deleteScrapIfAlreadyExists(postScrap);
            return;
        }
        saveScrap(postId, memberId);
    }

    private void saveScrap(Long postId, Long memberId) {
        PostScrap postScrap = PostScrap.builder()
                .post(postRepository.getReferenceById(postId))
                .member(memberRepository.getReferenceById(memberId))
                .build();
        postScrapRepository.save(postScrap);
    }

    private void deleteScrapIfAlreadyExists(PostScrap postScrap) {
        postScrapRepository.delete(postScrap);
    }

    @Transactional
    public Long reportPost(Long postId, Long memberId, String content) {
        if (postReportRepository.existsByPostIdAndMemberId(postId, memberId)) {
            throw new PostException(PostErrorInfo.DUPLICATE_REPORT);
        }

        if (postRepository.existsByIdAndMemberId(postId, memberId)) {
            throw new PostException(PostErrorInfo.UNABLE_REPORT_MY_POST);
        }

        PostReport postReport = PostReport.builder()
                .post(postRepository.getReferenceById(postId))
                .member(memberRepository.getReferenceById(memberId))
                .content(content)
                .build();

        return postReportRepository.save(postReport).getId();
    }

    @Transactional
    public Long writePost(Long boardId, Long memberId, PostPostWriteReqDto postPostWriteReqDto, List<MultipartFile> images) {
        if (isImageIncluded(images)) {
            // 1. 게시글 등록
            Post post = savePost(boardId, memberId, postPostWriteReqDto);

            // 2. 이미지 s3에 업로드 및 URL 등록
            uploadPostImages(post, memberId, images);
            return post.getId();
        }

        Post post = savePost(boardId, memberId, postPostWriteReqDto);
        return post.getId();
    }

    private boolean isImageIncluded(List<MultipartFile> images) {
        return !images.get(0).isEmpty();
    }

    private void uploadPostImages(Post post, Long memberId, List<MultipartFile> images) {
        MetaData metaData = new MetaData(UploadDirectory.POST);
        List<PostImage> postImages = new ArrayList<>();

        images.stream()
                .map(image -> awsS3StorageSerive.putObject(image, metaData, memberId))
                .map(uploadFileInfo -> generatePostImage(post, uploadFileInfo))
                .forEach(postImages::add);

        postImageRepository.saveAll(postImages);
    }

    private void deletePostImages(List<PostImage> images) {
        images.stream()
                .map(PostImage::getImagePath)
                .forEach(awsS3StorageSerive::deleteObject);
        postImageRepository.deleteAllInBatch(images);
    }

    private PostImage generatePostImage(Post post, UploadFileInfo uploadFileInfo) {
        return PostImage.builder()
                .post(post)
                .imagePath(uploadFileInfo.getFilePath())
                .imageUrl(uploadFileInfo.getFileUrl())
                .build();
    }

    private Post savePost(Long boardId, Long memberId, PostPostWriteReqDto postPostWriteReqDto) {
        return postRepository.save(Post.builder()
                .board(boardRepository.findById(boardId).orElseThrow(
                        () -> new BoardException(BoardErrorInfo.NO_BOARD)
                ))
                .member(memberRepository.getReferenceById(memberId))
                .title(postPostWriteReqDto.getTitle())
                .content(postPostWriteReqDto.getContent())
                .deletedPost(false)
                .anonymous(postPostWriteReqDto.isAnonymous())
                .build());
    }

    @Transactional
    public Long deletePost(Long postId, Long memberId) {
        // 1. 게시글 삭제
        Post post = postRepository.findByIdWithMember(postId)
                .orElseThrow(() -> new PostException(PostErrorInfo.NOT_FOUND_POST));

        if (!post.getMember().getId().equals(memberId)) {
            throw new PostException((PostErrorInfo.UNAUTHORIZED_DELETE_POST));
        }
        postRepository.delete(post);

        // 2. 핫 게시글이 있으면 삭제
        hotPostRepository.findByPostId(postId).ifPresent(hotPostRepository::delete);

        return post.getId();
    }

    @Transactional
    public Long updatePost(Long postId, Long memberId, PostPutUpdateReqDto postPutUpdateReqDto) {
        Post post = postRepository.findByIdWithMemberAndPostImageFetch(postId)
                .orElseThrow(() -> new PostException(PostErrorInfo.NOT_FOUND_POST));

        if (!post.getMember().getId().equals(memberId)) {
            throw new PostException(PostErrorInfo.UNAUTHORIZED_UPDATE_POST);
        }

        // 1. 수정
        post.updatePost(postPutUpdateReqDto.getTitle(), postPutUpdateReqDto.getContent(), postPutUpdateReqDto.isAnonymous());

        // 2. 새 이미지 업로드
        if (!postPutUpdateReqDto.getImages().get(0).isEmpty())
            uploadPostImages(post, memberId, postPutUpdateReqDto.getImages());

        // 3. 기존 이미지 삭제
        if (post.getImages().size() >= 1)
            deletePostImages(post.getImages());

        return post.getId();
    }


    @Transactional(readOnly = true)
    public GetPostHotResDto findHotPosts(Pageable pageable) {
        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        List<HotPost> hotPosts = hotPostRepository.findWithDetailsFetch(pageRequest);

        return GetPostHotResDto.from(hotPosts);
    }

    @Transactional(readOnly = true)
    public GetPostMyResDto findMyPosts(Pageable pageable, Long memberId) {
        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        List<Post> posts = postRepository.findWithDetailsByMemberId(memberId, pageRequest);

        return GetPostMyResDto.from(posts);
    }

    @Transactional(readOnly = true)
    public GetPostResDto searchPosts(Long boardId, String keyword, Pageable pageable) {
        if (!boardRepository.existsById(boardId)) {
            throw new BoardException(BoardErrorInfo.NO_BOARD);
        }

        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        List<Post> posts = postRepository.findWithDetailsFetchByBoardIdAndKeyword(boardId, keyword.replaceAll(" ", ""), pageRequest);

        return GetPostResDto.from(posts);
    }

    @Transactional(readOnly = true)
    public GetPostHotResDto searchHotPosts(String keyword, Pageable pageable) {
        PageRequest pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        List<HotPost> hotPosts = hotPostRepository.findWithDetailsFetchByKeyword(keyword.replaceAll(" ", ""), pageRequest);
        return GetPostHotResDto.from(hotPosts);
    }
}
