package com.chatty.chatty.game.service;

import static com.chatty.chatty.game.domain.Phase.COUNTDOWN;
import static com.chatty.chatty.game.domain.Phase.RESULT;

import com.chatty.chatty.common.util.ThreadSleep;
import com.chatty.chatty.config.GlobalMessagingTemplate;
import com.chatty.chatty.game.controller.dto.CountDownResponse;
import com.chatty.chatty.game.controller.dto.DescriptionResponse;
import com.chatty.chatty.game.controller.dto.QuizReadyResponse;
import com.chatty.chatty.game.controller.dto.QuizResponse;
import com.chatty.chatty.game.controller.dto.ScoreResponse;
import com.chatty.chatty.game.controller.dto.ScoreResponse.PlayerScoreDTO;
import com.chatty.chatty.game.controller.dto.SubmitAnswerRequest;
import com.chatty.chatty.game.controller.dto.SubmitAnswerResponse;
import com.chatty.chatty.game.controller.dto.dynamodb.QuizDTO;
import com.chatty.chatty.game.controller.dto.model.MarkRequest;
import com.chatty.chatty.game.controller.dto.model.MarkRequest.AnswerDTO;
import com.chatty.chatty.game.controller.dto.model.MarkResponse;
import com.chatty.chatty.game.domain.AnswerData;
import com.chatty.chatty.game.domain.AnswerData.PlayerAnswerData;
import com.chatty.chatty.game.domain.Phase;
import com.chatty.chatty.game.domain.QuizData;
import com.chatty.chatty.game.domain.ScoreData;
import com.chatty.chatty.game.domain.UserSubmitStatus;
import com.chatty.chatty.game.domain.UsersSubmitStatus;
import com.chatty.chatty.game.repository.AnswerRepository;
import com.chatty.chatty.game.repository.GameRepository;
import com.chatty.chatty.game.repository.PhaseRepository;
import com.chatty.chatty.game.repository.ScoreRepository;
import com.chatty.chatty.game.repository.UserSubmitStatusRepository;
import com.chatty.chatty.game.service.dynamodb.DynamoDBService;
import com.chatty.chatty.game.service.model.ModelService;
import com.chatty.chatty.player.controller.dto.NicknameRequest;
import com.chatty.chatty.player.controller.dto.PlayersStatusDTO;
import com.chatty.chatty.player.domain.PlayersStatus;
import com.chatty.chatty.player.repository.PlayersStatusRepository;
import com.chatty.chatty.quizroom.entity.QuizRoom;
import com.chatty.chatty.quizroom.entity.Status;
import com.chatty.chatty.quizroom.repository.QuizRoomRepository;
import com.chatty.chatty.user.service.UserService;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private static final Integer QUIZ_SIZE = 5;
    private static final Integer QUIZ_COUNT_SECONDS = 3;
    private static final Integer SCORE_COUNT_SECONDS = 7;

    private final QuizRoomRepository quizRoomRepository;
    private final PlayersStatusRepository playersStatusRepository;
    private final UserSubmitStatusRepository userSubmitStatusRepository;
    private final GameRepository gameRepository;
    private final AnswerRepository answerRepository;
    private final ScoreRepository scoreRepository;
    private final DynamoDBService dynamoDBService;
    private final ModelService modelService;
    private final GlobalMessagingTemplate template;
    private final PhaseRepository phaseRepository;
    private final UserService userService;

    public PlayersStatusDTO joinRoom(Long roomId, Long userId, NicknameRequest request) {
        String profileImageUrl = userService.getProfileImageUrl(userId);
        PlayersStatus playersStatus = playersStatusRepository.findByRoomId(roomId)
                .orElse(playersStatusRepository.saveUserToRoom(roomId, userId, request.nickname(), profileImageUrl));
        return buildDTO(roomId, playersStatus);
    }

    public PlayersStatusDTO leaveRoom(Long roomId, Long userId) {
        PlayersStatus playersStatus = playersStatusRepository.leaveRoom(roomId, userId);
        return buildDTO(roomId, playersStatus);
    }

    public PlayersStatusDTO toggleReady(Long roomId, Long userId) {
        PlayersStatus playersStatus = playersStatusRepository.toggleReady(roomId, userId);
        if (playersStatus.isAllReady()) {
            quizRoomRepository.updateStatusById(roomId, Status.STARTED);
        } else {
            quizRoomRepository.updateStatusById(roomId, Status.READY);
        }
        return buildDTO(roomId, playersStatus);
    }

    private PlayersStatusDTO buildDTO(Long roomId, PlayersStatus playersStatus) {
        return PlayersStatusDTO.builder()
                .roomId(roomId)
                .playerStatuses(playersStatus.playerStatusSet())
                .build();
    }

    private synchronized void initQuiz(QuizData quizData) {
        if (quizData.getQuizDTOQueue().isEmpty() && quizData.getCurrentRound() < quizData.getTotalRound()) {
            fillQuiz(quizData);
            log.info("Fill: QuizQueue: {}", quizData.getQuizDTOQueue());
        }
    }

    public QuizResponse sendQuiz(Long roomId) {
        if (phaseRepository.getPhase(roomId) == RESULT) {
            return null;
        }
        QuizData quizData = gameRepository.getQuizData(roomId);
        initQuiz(quizData);
        log.info("Send: Quiz: {}", gameRepository.getQuizData(roomId).getQuiz());
        answerRepository.getAnswerData(roomId);
        return buildQuizResponse(quizData);
    }

    private void fillQuiz(QuizData quizData) {
        Integer currentRound = quizData.getCurrentRound();
        List<QuizDTO> quizDTOList = dynamoDBService.pollQuizzes(quizData.getQuizDocId(), currentRound, QUIZ_SIZE);
        List<QuizDTO> currentQuizzes = quizDTOList.subList(currentRound * QUIZ_SIZE, (currentRound + 1) * QUIZ_SIZE);
        quizData.fillQuiz(currentQuizzes);
        log.info("filled queue: {}", quizData.getQuizDTOQueue());
    }

    public void removeQuiz(Long roomId) {
        QuizData quizData = gameRepository.getQuizData(roomId);
        log.info("Remove: QuizQueue: {}", quizData.getQuizDTOQueue());
        quizData.removeQuiz();
    }

    /*
        subscription URL : /user/{userId}/queue/rooms/{roomId}/quizReady
    */
    @Async
    public void sendQuizReady(Long roomId, Long userId) {
        QuizData quizData = gameRepository.getQuizData(roomId);
        initQuiz(quizData);
        QuizReadyResponse response = QuizReadyResponse.builder()
                .isReady(true)
                .build();
        template.publishQuizReady(userId, roomId, response);
    }

    /*
        subscription URL : /user/{userId}/queue/rooms/{roomId}/description
     */
    @Async
    public void sendDescription(Long roomId, Long userId) {
        QuizData quizData = gameRepository.getQuizData(roomId);
        String description = dynamoDBService.pollDescription(quizData.getQuizDocId());
        DescriptionResponse descriptionResponse = DescriptionResponse.builder()
                .description(description)
                .build();
        log.info("디스크립션: {}", descriptionResponse);
        template.publishDescription(roomId, userId, descriptionResponse);
    }

    private QuizResponse buildQuizResponse(QuizData quizData) {
        QuizDTO quiz = quizData.getQuiz();
        log.info("currentRound: {}", quizData.getCurrentRound());
        if (quiz == null) {
            return null;
        }
        return QuizResponse.builder()
                .totalRound(quizData.getTotalRound())
                .currentRound(quizData.getCurrentRound())
                .quizNumber(quiz.questionNumber())
                .type(quiz.type())
                .quiz(quiz.question())
                .options(quiz.options())
                .build();
    }

    public void addPlayerAnswer(Long roomId, SubmitAnswerRequest request, Long userId) {
        // 플레이어 답안 맵에 답안 추가
        AnswerData answerData = answerRepository.getAnswerData(roomId);
        Boolean isMajority = answerData.addAnswer(userId, request);
        log.info("Add Answer: PlayerAnswers: {}", answerData.getPlayerAnswers());

        // 플레이어 제출 현황 갱신
        UsersSubmitStatus usersSubmitStatus = userSubmitStatusRepository.submit(roomId, userId);
        sendPlayersSubmitStatus(roomId, isMajority, usersSubmitStatus);
        int submitCount = (int) usersSubmitStatus.usersSubmitStatus().stream()
                .filter(UserSubmitStatus::isSolved)
                .count();

        // 과반수 제출일 때
        // 3초 카운트다운
        if (submitCount == answerData.getMajorityNum()) {
            phaseRepository.update(roomId, COUNTDOWN);
            log.info("Phase UPDATED: COUNTDOWN");
            quizCountDown(roomId);
        }
    }

    private void quizCountDown(Long roomId) {
        Integer seconds = QUIZ_COUNT_SECONDS;
        while (seconds >= 0) {
            log.info("Countdown: {}", seconds);
            template.publishQuizCount(roomId, buildCountDownResponse(seconds));
            ThreadSleep.sleep(1000L);
            seconds--;
        }
        // seconds == -1
        // 제출 안 한애들 빈값으로 제출 처리
        AnswerData answerData = answerRepository.getAnswerData(roomId);
        UsersSubmitStatus usersSubmitStatus = userSubmitStatusRepository.findByRoomId(roomId);
        usersSubmitStatus.usersSubmitStatus().stream()
                .filter(userSubmitStatus -> !userSubmitStatus.isSolved())
                .forEach(userSubmitStatus -> {
                    answerData.addAnswer(userSubmitStatus.userId(),
                            SubmitAnswerRequest.builder().playerAnswer("").build());
                    userSubmitStatus.submit();
                });
        log.info("Answer All Submitted: PlayerAnswers: {}", answerData.getPlayerAnswers());
        // 퀴즈 끝났으면 다음 퀴즈 반환 준비
        AnswerData clonedAnswerData = answerData.clone();
        resetState(roomId);
        // 라운드 별 마지막 문제인지 확인
        if (gameRepository.getQuizData(roomId).isQueueEmpty()) {
            markAndUpdateScore(roomId, clonedAnswerData);
            phaseRepository.update(roomId, RESULT);
            log.info("Phase UPDATED: RESULT");
            template.publishQuizCount(roomId, buildCountDownResponse(seconds));
            sendScore(roomId);
        } else {
            phaseRepository.update(roomId, Phase.QUIZ_SOLVING);
            log.info("Phase UPDATED: QUIZ_SOLVING");
            template.publishQuizCount(roomId, buildCountDownResponse(seconds));
            markAndUpdateScore(roomId, clonedAnswerData);
        }
        log.info("Quiz Countdown: {}", seconds);
    }

    private void markAndUpdateScore(Long roomId, AnswerData answerData) {
        // ML에 플레이어들 답안 넘겨서 채점 요청 후 채점 문서 id 저장
        QuizRoom quizRoom = quizRoomRepository.findById(roomId).get();
        MarkResponse markResponse = modelService.requestMark(MarkRequest.builder()
                .id(quizRoom.getQuizDocId())
                .timestamp(quizRoom.getCreatedAt().toString())
                .quiz_id(answerData.getQuizId())
                .quiz_num(answerData.getQuizNum())
                .quiz_type(answerData.getQuizType())
                .correct_answer(answerData.getCorrect())
                .submit_answers(getAnswers(answerData.getPlayerAnswers()))
                .build());

        // 점수 갱신
        ScoreData scoreData = scoreRepository.getScoreData(roomId);
        scoreData.addScore(answerData, markResponse.marked_answers());
        log.info("Updated Score");
    }

    private void resetState(Long roomId) {
        // 직전 문제 삭제
        removeQuiz(roomId);
        // 플레이어 제출 상태, 답안 맵 초기화
        PlayersStatus players = playersStatusRepository.findByRoomId(roomId).get();
        userSubmitStatusRepository.init(players, roomId);
        sendPlayersSubmitStatus(roomId, false, userSubmitStatusRepository.findByRoomId(roomId));
        answerRepository.clearAnswerData(roomId);
        log.info("Reset");
    }

    private void sendScore(Long roomId) {
        ScoreResponse scoreResponse = convertScoreDTO(roomId);
        template.publishScore(roomId, scoreResponse);
        scoreCountDown(roomId);
    }

    private void scoreCountDown(Long roomId) {
        int seconds = SCORE_COUNT_SECONDS;
        while (seconds >= 0) {
            template.publishScoreCount(roomId, buildCountDownResponse(seconds));
            ThreadSleep.sleep(1000L);
            seconds--;
        }
        if (seconds == -1) {
            phaseRepository.update(roomId, Phase.QUIZ_SOLVING);
            log.info("Phase UPDATED: QUIZ_SOLVING");
            template.publishScoreCount(roomId, buildCountDownResponse(seconds));
        }
    }

    private CountDownResponse buildCountDownResponse(Integer second) {
        return CountDownResponse.builder()
                .second(second)
                .build();
    }

    private void sendPlayersSubmitStatus(Long roomId, Boolean isMajority, UsersSubmitStatus status) {
        SubmitAnswerResponse response = SubmitAnswerResponse.builder()
                .isMajority(isMajority)
                .submitStatuses(status.usersSubmitStatus())
                .build();
        template.publishSubmitStatus(roomId, response);
    }

    private List<AnswerDTO> getAnswers(Map<Long, PlayerAnswerData> playerAnswers) {
        return playerAnswers.entrySet().stream()
                .map(entry -> AnswerDTO.builder()
                        .user_id(entry.getKey())
                        .user_answer(entry.getValue().playerAnswer())
                        .build())
                .toList();
    }

    public ScoreResponse convertScoreDTO(Long roomId) {
        ScoreData scoreData = scoreRepository.getScoreData(roomId);
        List<PlayerScoreDTO> scores = scoreData.getPlayersScore().entrySet().stream()
                .map(entry -> PlayerScoreDTO.builder()
                        .playerId(entry.getKey())
                        .nickname(entry.getValue().getNickname())
                        .profileImage(entry.getValue().getProfileImage())
                        .score(entry.getValue().getScore())
                        .build())
                .sorted(Comparator.comparing(PlayerScoreDTO::score).reversed())
                .toList();
        return ScoreResponse.builder()
                .scores(scores)
                .build();
    }

    public void getPhase(Long roomId, Long userId) {
        Phase currentPhase = phaseRepository.getPhase(roomId);
        QuizResponse quizResponse = sendQuiz(roomId);
        switch (currentPhase) {
            case QUIZ_SOLVING, COUNTDOWN -> {
                if (quizResponse != null) {
                    template.publishQuiz(userId, roomId, quizResponse);
                }
                Boolean isMajority = answerRepository.getAnswerData(roomId).checkSubmitStatus();
                sendPlayersSubmitStatus(roomId, isMajority, userSubmitStatusRepository.findByRoomId(roomId));
            }
            case RESULT -> {
                ScoreResponse scoreResponse = convertScoreDTO(roomId);
                template.publishScore(roomId, scoreResponse);
            }
        }
    }
}