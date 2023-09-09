package com.ssafy.ssafsound.domain.recruit.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ssafy.ssafsound.domain.meta.domain.MetaData;
import com.ssafy.ssafsound.domain.meta.domain.MetaDataType;
import com.ssafy.ssafsound.domain.meta.service.MetaDataConsumer;
import com.ssafy.ssafsound.domain.recruit.domain.Category;
import com.ssafy.ssafsound.domain.recruit.domain.Recruit;
import com.ssafy.ssafsound.domain.recruit.domain.RecruitLimitation;
import com.ssafy.ssafsound.domain.recruit.domain.RecruitSkill;
import com.ssafy.ssafsound.domain.recruit.dto.GetRecruitsReqDto;
import com.ssafy.ssafsound.domain.recruitapplication.domain.MatchStatus;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

import static com.ssafy.ssafsound.domain.recruit.domain.QRecruit.recruit;
import static com.ssafy.ssafsound.domain.recruit.domain.QRecruitSkill.recruitSkill;
import static com.ssafy.ssafsound.domain.recruit.domain.QRecruitLimitation.recruitLimitation;
import static com.ssafy.ssafsound.domain.recruitapplication.domain.QRecruitApplication.recruitApplication;
import static com.ssafy.ssafsound.domain.member.domain.QMember.member;


@Repository
@RequiredArgsConstructor
@Slf4j
public class RecruitDynamicQueryRepositoryImpl implements RecruitDynamicQueryRepository {

    private final MetaDataConsumer metaDataConsumer;

    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public Slice<Recruit> findRecruitByGetRecruitsReqDto(GetRecruitsReqDto dto, Pageable pageable) {
        // cursor base pagination (value -1 or null ignore search condition)
        Long cursor = dto.getCursor();
        BooleanExpression recruitIdLtThanCursor = ((cursor != null) && (cursor != -1)) ? recruit.id.lt(cursor) : null;

        // recruit category (STUDY | PROJECT)
        BooleanExpression categoryEq = recruit.category.eq(Category.valueOf(dto.getCategory().toUpperCase()));

        // recruit title contains search keyword
        String keyword = dto.getKeyword();
        BooleanExpression titleEq = StringUtils.hasText(keyword) ? recruit.title.contains(keyword) : null;

        JPAQuery<Recruit> recruitDynamicQuery = jpaQueryFactory.selectFrom(recruit)
                .where(recruitIdLtThanCursor, categoryEq, titleEq);

        // recruit skill
        List<String> skills = dto.getSkills();
        if(skills!=null && skills.size() > 0) {
            String metaDataType = MetaDataType.SKILL.name();
            List<MetaData> containSkills = skills.stream()
                    .map(skillName->metaDataConsumer.getMetaData(metaDataType, skillName))
                    .collect(Collectors.toList());

            JPQLQuery<Long> recruitSkillContainRecruitIds = JPAExpressions
                    .select(recruitSkill.recruit.id)
                    .from(recruitSkill)
                    .innerJoin(recruitSkill.recruit, recruit)
                    .where(recruitSkill.skill.in(containSkills));

            recruitDynamicQuery.where(recruit.id.in(recruitSkillContainRecruitIds));
        }

        // recruit types limitation
        List<String> recruitTypes = dto.getRecruitTypes();
        if(recruitTypes!=null && recruitTypes.size() > 0) {
            String metaDataType = MetaDataType.RECRUIT_TYPE.name();
            List<MetaData> containRecruitTypes = recruitTypes.stream()
                    .map(recruitType->metaDataConsumer.getMetaData(metaDataType, recruitType))
                    .collect(Collectors.toList());

            JPQLQuery<Long> limitationContainRecruitIds = JPAExpressions
                    .select(recruitLimitation.recruit.id)
                    .from(recruitLimitation)
                    .innerJoin(recruitLimitation.recruit, recruit)
                    .where(recruitLimitation.type.in(containRecruitTypes));

            recruitDynamicQuery.where(recruit.id.in(limitationContainRecruitIds));
        }

        List<Recruit> recruits = recruitDynamicQuery
                .limit(pageable.getPageSize()+1)
                .orderBy(recruit.id.desc())
                .fetch();
        boolean hasNext = pageable.isPaged() && recruits.size() > pageable.getPageSize();
        return new SliceImpl<>(hasNext ? recruits.subList(0, pageable.getPageSize()) : recruits, pageable, hasNext);
    }

    @Override
    public Slice<Recruit> findMemberJoinRecruitWithCursorAndPageable(Long memberId, Long cursorId, Pageable pageable) {
        List<Long> memberJoinRecruitIds = jpaQueryFactory.select(recruitApplication.recruit.id)
                .from(recruitApplication)
                .innerJoin(recruitApplication.recruit, recruit)
                .innerJoin(recruitApplication.member, member)
                .where(recruitApplication.member.id.eq(memberId), recruitApplication.matchStatus.eq(MatchStatus.DONE))
                .fetch();

        List<Recruit> recruits = jpaQueryFactory.selectFrom(recruit)
                .innerJoin(recruit.member, member)
                .where(recruit.id.in(memberJoinRecruitIds), recruit.member.id.eq(memberId))
                .limit(pageable.getPageSize()+1)
                .orderBy(recruit.id.desc())
                .fetch();

        boolean hasNext = pageable.isPaged() && recruits.size() > pageable.getPageSize();
        return new SliceImpl<>(hasNext ? recruits.subList(0, pageable.getPageSize()) : recruits, pageable, hasNext);
    }
}
