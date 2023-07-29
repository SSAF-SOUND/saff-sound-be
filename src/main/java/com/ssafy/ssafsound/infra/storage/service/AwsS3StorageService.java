package com.ssafy.ssafsound.infra.storage.service;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.Headers;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.ssafy.ssafsound.domain.member.exception.MemberErrorInfo;
import com.ssafy.ssafsound.domain.member.exception.MemberException;
import com.ssafy.ssafsound.domain.member.repository.MemberRepository;
import com.ssafy.ssafsound.domain.meta.domain.UploadDirectory;
import com.ssafy.ssafsound.infra.exception.InfraErrorInfo;
import com.ssafy.ssafsound.infra.exception.InfraException;
import com.ssafy.ssafsound.infra.storage.dto.PostStoreImageResDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AwsS3StorageService {

    private final AmazonS3 amazonS3;

    private final MemberRepository memberRepository;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.cloudfront.domain}")
    private String cloudFrontDomain;

    @Value("${cloud.aws.s3.expire-in}")
    private Long expireIn;

    private static final String uploadDir = UploadDirectory.POST.getName();

    public PostStoreImageResDto getPreSignedUrl(Long memberId) {

        memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberException(MemberErrorInfo.MEMBER_NOT_FOUND_BY_ID));

        String fileName = makeFileName(uploadDir, memberId);

        try {
            GeneratePresignedUrlRequest generatePresignedUrlRequest = getGeneratePreSignedUrlRequest(
                bucket, fileName);

            return PostStoreImageResDto.builder()
                .imagePath(fileName)
                .imageUrl(makeCDNFileUrl(fileName))
                .preSignedUrl(amazonS3.generatePresignedUrl(generatePresignedUrlRequest).toString())
                .build();

        } catch (AmazonServiceException e) {
            throw new InfraException(InfraErrorInfo.STORAGE_SERVICE_ERROR);
        }
    }

    public void deleteObject(Long memberId, String imagePath) {

        memberRepository.findById(memberId)
            .orElseThrow(() -> new MemberException(MemberErrorInfo.MEMBER_NOT_FOUND_BY_ID));

        // 파일경로의 멤버아이디와 요청 멤버 아이디를 비교
        Long parsedMemberId = Long.parseLong(imagePath.split("/")[1]);

        if (!memberId.equals(parsedMemberId)) {
            throw new InfraException(InfraErrorInfo.STORAGE_DELETE_UNAUTHORIZED);
        }

        try {
            amazonS3.deleteObject(bucket, imagePath);
        } catch (AmazonServiceException e) {
            throw new InfraException(InfraErrorInfo.STORAGE_SERVICE_ERROR);
        }
    }

    private GeneratePresignedUrlRequest getGeneratePreSignedUrlRequest(String bucket, String fileName) {

        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucket, fileName)
                        .withMethod(HttpMethod.PUT)
                        .withExpiration(getPreSignedUrlExpiration());

        generatePresignedUrlRequest.addRequestParameter(
                Headers.S3_CANNED_ACL,
                CannedAccessControlList.PublicReadWrite.toString());

        return generatePresignedUrlRequest;
    }

    private Date getPreSignedUrlExpiration() {
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += expireIn;
        expiration.setTime(expTimeMillis);

        return expiration;
    }

    private String makeFileName(String uploadDir, Long memberId) {

        StringBuffer fileName = new StringBuffer();

        return fileName.append(uploadDir)
                .append("/")
                .append(memberId)
                .append("/")
                .append(UUID.randomUUID()) // 파일명 고유화
                .toString();
    }

    private String makeCDNFileUrl(String filename) {

        StringBuffer CDNFileUrl = new StringBuffer();

        return CDNFileUrl.append(cloudFrontDomain)
                .append("/")
                .append(filename)
                .toString();
    }
}
