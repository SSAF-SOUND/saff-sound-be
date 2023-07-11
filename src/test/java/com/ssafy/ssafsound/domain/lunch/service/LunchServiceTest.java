package com.ssafy.ssafsound.domain.lunch.service;

import com.ssafy.ssafsound.domain.lunch.domain.Lunch;
import com.ssafy.ssafsound.domain.lunch.domain.LunchPoll;
import com.ssafy.ssafsound.domain.lunch.dto.GetLunchListElementResDto;
import com.ssafy.ssafsound.domain.lunch.dto.GetLunchListReqDto;
import com.ssafy.ssafsound.domain.lunch.dto.GetLunchListResDto;
import com.ssafy.ssafsound.domain.lunch.dto.GetLunchResDto;
import com.ssafy.ssafsound.domain.lunch.repository.LunchPollRepository;
import com.ssafy.ssafsound.domain.lunch.repository.LunchRepository;
import com.ssafy.ssafsound.domain.member.domain.Member;
import com.ssafy.ssafsound.domain.meta.domain.Campus;
import com.ssafy.ssafsound.domain.meta.domain.MetaData;
import com.ssafy.ssafsound.domain.meta.domain.MetaDataType;
import com.ssafy.ssafsound.domain.meta.service.MetaDataConsumer;
import com.ssafy.ssafsound.global.util.NullableConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LunchServiceTest {

    @Mock
    private LunchRepository lunchRepository;
    @Mock
    private LunchPollRepository lunchPollRepository;
    @Mock
    private MetaDataConsumer metaDataConsumer;
    @Mock
    private Clock clock;

    @InjectMocks
    private LunchService lunchService;

    private Lunch lunch1;
    private Lunch lunch2;
    private Member member1;
    private Member member2;
    private LunchPoll lunchPoll;
    private String testLocalDateTime = "2023-07-10T10:15:00Z";
    private String testLocalDate = "2023-07-10";
    private LocalDate today = LocalDate.parse(testLocalDate);
    private MetaData testCampus;

    @BeforeEach
    void setUp() {

        testCampus = new MetaData(Campus.SEOUL);

        lunch1 = Lunch.builder()
                .id(1L)
                .campus(testCampus)
                .mainMenu("mainMenu1")
                .createdAt(today)
                .build();

        lunch2 = Lunch.builder()
                .id(2L)
                .campus(testCampus)
                .mainMenu("mainMenu2")
                .createdAt(today)
                .build();

        member1 = Member.builder()
                .id(1L)
                .build();

        member2 = Member.builder()
                .id(2L)
                .build();

        lunchPoll = LunchPoll.builder()
                .lunch(lunch2)
                .member(member1)
                .polledAt(today)
                .build();
    }

    // 점심 메뉴 목록 조회 테스트
    @ParameterizedTest
    @CsvSource({"1, 서울, 2023-07-10, 0", "2, 서울, 2023-07-10, -1", "null, 서울, 2023-07-10, -1"})
    @DisplayName("멤버아이디, 캠퍼스, 날짜로 점심 메뉴 목록을 조회한다.")
    void Given_DateAndCampus_When_FindLunches_Then_Succeed(@ConvertWith(NullableConverter.class)Long memberId, String inputCampus, LocalDate inputDate,
                                                                Long outputPolledAt) {

        // given
        given(clock.instant()).willReturn(Instant.parse(testLocalDateTime));
        given(clock.getZone()).willReturn(ZoneId.of("Asia/Seoul"));

        given(metaDataConsumer.getMetaData(MetaDataType.CAMPUS.name(), inputCampus))
                .willReturn(new MetaData(Campus.SEOUL));

        given(lunchRepository.findAllByCampusAndDate(metaDataConsumer.getMetaData(MetaDataType.CAMPUS.name(), inputCampus), inputDate))
                .willReturn(Optional.of(Arrays.asList(lunch2, lunch1)));

        if (memberId != null) {
            lenient().when(lunchPollRepository.findByMember_IdAndPolledAt(memberId, inputDate))
                    .thenReturn(memberId.equals(1L) ? lunchPoll : null);
        }

        GetLunchListReqDto getLunchListReqDto = GetLunchListReqDto.builder()
                .campus(inputCampus)
                .date(inputDate)
                .build();

        // when
        GetLunchListResDto result = lunchService.findLunches(memberId, getLunchListReqDto);

        // then
        assertAll(
                () -> assertThat(result.getMenus().get(0))
                        .usingRecursiveComparison()
                        .isEqualTo(GetLunchListElementResDto.of(lunch2,1L)),
                () -> assertThat(result.getMenus().get(1))
                        .usingRecursiveComparison()
                        .isEqualTo(GetLunchListElementResDto.of(lunch1,0L)),
                ()-> assertThat(result.getPolledAt()).isEqualTo(outputPolledAt)
        );

        verify(clock).instant();
        verify(clock).getZone();
        verify(metaDataConsumer, times(2)).getMetaData(MetaDataType.CAMPUS.name(), inputCampus);
        verify(lunchRepository).findAllByCampusAndDate(
                metaDataConsumer.getMetaData(MetaDataType.CAMPUS.name(), inputCampus), inputDate);

    }

    @Test
    @DisplayName("유효하지 않은 캠퍼스로 점심 메뉴 목록 조회시 예외를 던진다.")
    void Given_InvalidCampus_When_FindingAllLunches_Then_ThrowException() {
    }

    @Test
    @DisplayName("유효하지 않은 날짜의 점심 메뉴 목록 조회시 예외를 던진다.")
    void Given_InvalidDate_When_FindingAllLunches_Then_ThrowException() {
    }

    // 점심 메뉴 상세 조회 테스트
    @ParameterizedTest
    @CsvSource({"1, mainMenu1","2, mainMenu2"})
    @DisplayName("점심 아이디로 점심 상세 정보를 조회한다.")
    void Given_ValidLunch_When_FindLunchDetail_Then_Succeed(Long lunchId, String outputMainMenu){

        // given
        lenient().when(lunchRepository.findById(1L)).thenReturn(Optional.of(lunch1));

        lenient().when(lunchRepository.findById(2L)).thenReturn(Optional.of(lunch2));

        // when
        GetLunchResDto getLunchResDto = lunchService.findLunchDetail(lunchId);

        // then
        assertThat(getLunchResDto.getMainMenu()).isEqualTo(outputMainMenu);
    }

    @Test
    @DisplayName("유효하지 않은 날짜의 점심 아이디의 점심 메뉴 상세 조회 요청 시 예외를 던진다.")
    void Given_InValidDateLunch_When_FindLunchDetail_Then_ThrowException(){
    }
}