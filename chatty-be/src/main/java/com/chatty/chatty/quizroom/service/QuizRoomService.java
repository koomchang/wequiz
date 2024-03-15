package com.chatty.chatty.quizroom.service;

import com.chatty.chatty.quizroom.controller.dto.MakeRoomRequest;
import com.chatty.chatty.quizroom.controller.dto.MakeRoomResponse;
import com.chatty.chatty.quizroom.entity.QuizRoom;
import com.chatty.chatty.quizroom.repository.QuizRoomRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuizRoomService {

    private final String BASE_LINK = "https://localhost:8080/rooms/";

    private final QuizRoomRepository quizRoomRepository;

    private final ModelService modelService;

    public List<QuizRoom> findAll() {
        return quizRoomRepository.findAll();
    }

    public Optional<QuizRoom> findById(Long id) {
        return quizRoomRepository.findById(id);
    }

    @Transactional
    public MakeRoomResponse makeRoom(MakeRoomRequest request) {
        QuizRoom newQuizRoom = QuizRoom.builder()
                .name(request.name())
                .numOfQuiz(request.numOfQuiz())
                .timeLimit(request.timeLimit())
                .playerLimitNum(request.playerLimitNum())
                .code(request.code())
                .link(BASE_LINK)
                .build();
        QuizRoom savedQuizRoom = quizRoomRepository.save(newQuizRoom);

        String link = BASE_LINK + savedQuizRoom.getId();
        savedQuizRoom.setLink(link);

        return MakeRoomResponse.builder()
                .id(savedQuizRoom.getId())
                .link(savedQuizRoom.getLink())
                .build();
    }
}
