package com.mountblue.stackoverflowclone.services;

import com.mountblue.stackoverflowclone.dtos.AnswerFormDto;
import com.mountblue.stackoverflowclone.models.*;
import com.mountblue.stackoverflowclone.repositories.AnswerRepository;
import com.mountblue.stackoverflowclone.repositories.QuestionRepository;
import com.mountblue.stackoverflowclone.repositories.UserRepository;
import com.mountblue.stackoverflowclone.repositories.VoteRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final VoteRepository voteRepository;

    public AnswerService(AnswerRepository answerRepository,
                         UserRepository userRepository,
                         QuestionRepository questionRepository,
                         VoteRepository voteRepository){
        this.answerRepository = answerRepository;
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.voteRepository = voteRepository;
    }

    @Transactional
    public Answer saveAnswer(AnswerFormDto answerFormDto, Long questionId, UserPrincipal currentUser) {

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NoSuchElementException("Question not found"));

        User author = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new NoSuchElementException("Default user not found. Please ensure user with ID 1 exists in your database."));

        if (answerFormDto.body() == null || answerFormDto.body().trim().isEmpty()) {
            throw new IllegalArgumentException("Answer body cannot be empty");
        }

        Answer answer = new Answer();
        answer.setBody(answerFormDto.body().trim());
        answer.setQuestion(question);
        answer.setAuthor(author);
        answer.setScore(0);
        answer.setAccepted(false);

        return answerRepository.save(answer);
    }

    public List<Answer> getAnswers(Long questionId) {
        return answerRepository.getAnswersByQuestionId(questionId);
    }

    @Transactional
    public void voteAnswer(Answer answer, String choice, String postType, UserPrincipal principal, Long id){
        int score = answer.getScore();
        User user = userRepository.findById(principal.getId()).get();
        Vote vote = voteRepository.findByUserIdAndPostIdAndPostType(user.getId(), id, postType).isEmpty()
                ? new Vote()
                : voteRepository.findByUserIdAndPostIdAndPostType(principal.getId(), id, postType).get();
        int newVoteValue = choice.equals("upvote") ? 1 : -1;
        System.out.println(newVoteValue);
        if (vote.getPostId() == null) {
            // New vote - user hasn't voted before
            vote.setUser(user);
            vote.setPostId(id);
            vote.setPostType(postType);
            vote.setVoteValue(newVoteValue);
            score += newVoteValue;
        } else {
            // User has voted before - toggle or change vote
            int oldVoteValue = vote.getVoteValue();

            if (oldVoteValue == newVoteValue) {
                // Clicking same vote - remove vote
                voteRepository.delete(vote);
                score -= oldVoteValue;
                answer.setScore(score);
                answerRepository.save(answer);
                return;
            } else {
                // Changing vote (upvote to downvote or vice versa)
                score -= oldVoteValue;  // Remove old vote
                score += newVoteValue;  // Add new vote
                vote.setVoteValue(newVoteValue);
            }
        }

        voteRepository.save(vote);
        answer.setScore(score);
        answerRepository.save(answer);
    }

    @Transactional
    public Answer editAnswer(AnswerFormDto answerFormDto, Long answerId, Long questionId) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new NoSuchElementException("Answer not found"));

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NoSuchElementException("Question not found"));

        if (!answer.getQuestion().getId().equals(questionId)) {
            throw new IllegalArgumentException("Answer does not belong to the specified question");
        }

        if (answerFormDto.body() == null || answerFormDto.body().trim().isEmpty()) {
            throw new IllegalArgumentException("Answer body cannot be empty");
        }

        answer.setBody(answerFormDto.body().trim());

        return answerRepository.save(answer);
    }

    @Transactional
    public void editAnswerBody(Long answerId, Long questionId, String body) {
        AnswerFormDto formDto = new AnswerFormDto(answerId, body);
        editAnswer(formDto, answerId, questionId);
    }

    public void deleteAnswer(Long answerId) {
        answerRepository.deleteById(answerId);
    }

    public Optional<Answer> findById(Long answerId) {
        return answerRepository.findById(answerId);
    }
}
