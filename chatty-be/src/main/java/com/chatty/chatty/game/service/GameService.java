package com.chatty.chatty.game.service;

import static com.chatty.chatty.common.util.ThreadSleep.sleep;

import com.chatty.chatty.game.controller.dto.DescriptionResponse;
import com.chatty.chatty.game.controller.dto.QuizResponse;
import com.chatty.chatty.game.controller.dto.dynamodb.Quiz;
import com.chatty.chatty.game.domain.QuizData;
import com.chatty.chatty.game.repository.AnswerRepository;
import com.chatty.chatty.game.repository.GameRepository;
import com.chatty.chatty.game.service.dynamodb.DynamoDBService;
import com.chatty.chatty.game.service.model.ModelService;
import com.chatty.chatty.player.controller.dto.PlayersStatusDTO;
import com.chatty.chatty.player.domain.PlayersStatus;
import com.chatty.chatty.player.repository.PlayersStatusRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameService {

    private static final Integer QUIZ_SIZE = 5;
    private static final Long QUIZ_POLLING_SLEEP_TIME = 5000L;
    private final PlayersStatusRepository playersStatusRepository;
    private final GameRepository gameRepository;
    private final AnswerRepository answerRepository;
    private final DynamoDBService dynamoDBService;
    private final ModelService modelService;
    private final SimpMessagingTemplate template;

    public PlayersStatusDTO joinRoom(Long roomId, Long userId) {
        PlayersStatus playersStatus = playersStatusRepository.saveUserToRoom(roomId, userId);
        return buildDTO(roomId, playersStatus);
    }

    public PlayersStatusDTO leaveRoom(Long roomId, Long userId) {
        PlayersStatus playersStatus = playersStatusRepository.leaveRoom(roomId, userId);
        return buildDTO(roomId, playersStatus);
    }

    public PlayersStatusDTO toggleReady(Long roomId, Long userId) {
        PlayersStatus playersStatus = playersStatusRepository.toggleReady(roomId, userId);
        return buildDTO(roomId, playersStatus);
    }

    public void endGame(Long roomId) {
        playersStatusRepository.clear(roomId);
    }

    private PlayersStatusDTO buildDTO(Long roomId, PlayersStatus playersStatus) {
        return PlayersStatusDTO.builder()
                .roomId(roomId)
                .playerStatuses(playersStatus.playerStatusSet())
                .build();
    }

    public void initQuiz(Long roomId) {
        QuizData quizData = gameRepository.getQuizData(roomId);
        fillQuiz(quizData);
    }

    public QuizResponse sendQuiz(Long roomId) {
        QuizData quizData = gameRepository.getQuizData(roomId);
        while (quizData.getQuizQueue().isEmpty()) {
            sleep(QUIZ_POLLING_SLEEP_TIME);
        }
        return buildQuizResponse(quizData);
    }

    @Async
    private void fillQuiz(QuizData quizData) {
        Integer currentRound = quizData.getCurrentRound();
        List<Quiz> quizzes = dynamoDBService.pollQuizzes(quizData.getQuizDocId(), quizData.getTimestamp(),
                currentRound, QUIZ_SIZE);
        List<Quiz> currentQuizzes = quizzes.subList(currentRound * QUIZ_SIZE, (currentRound + 1) * QUIZ_SIZE);
        quizData.getQuizQueue().addAll(currentQuizzes);
    }

    public void removeQuiz(Long roomId) {
        QuizData quizData = gameRepository.getQuizData(roomId);
        quizData.getQuizQueue().poll();

        if (quizData.getQuizQueue().isEmpty() && quizData.getCurrentRound() < quizData.getTotalRound()) {
            quizData.increaseCurrentRound();
            fillQuiz(quizData);
        }
    }

    /*
        subscription URL : /user/{userId}/queue/rooms/{roodId}/description
     */
    @Async
    public void sendDescription(Long roomId, Long userId) {
        QuizData quizData = gameRepository.getQuizData(roomId);
        String description = dynamoDBService.pollDescription(quizData.getQuizDocId(), quizData.getTimestamp());
        DescriptionResponse descriptionResponse = DescriptionResponse.builder()
                .description(description)
                .build();
        template.convertAndSendToUser(
                userId.toString(),
                "/queue/rooms/" + roomId + "/description",
                descriptionResponse
        );
    }

    private QuizResponse buildQuizResponse(QuizData quizData) {
        Quiz quiz = quizData.getQuizQueue().peek();
        return QuizResponse.builder()
                .quiz(quiz)
                .totalRound(quizData.getTotalRound())
                .currentRound(quizData.getCurrentRound() + 1)
                .build();
    }
}
