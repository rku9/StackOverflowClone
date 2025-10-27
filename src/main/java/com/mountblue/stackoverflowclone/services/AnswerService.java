package com.mountblue.stackoverflowclone.services;

import com.mountblue.stackoverflowclone.dtos.AnswerFormDto;
import com.mountblue.stackoverflowclone.models.*;
import com.mountblue.stackoverflowclone.repositories.AnswerRepository;
import com.mountblue.stackoverflowclone.repositories.QuestionRepository;
import com.mountblue.stackoverflowclone.repositories.UserRepository;
import com.mountblue.stackoverflowclone.repositories.VoteRepository;
import org.springframework.security.access.AccessDeniedException;
import com.mountblue.stackoverflowclone.workers.EmailQueue;
import com.mountblue.stackoverflowclone.dtos.EmailTaskDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class AnswerService {
    private static final Logger logger = LoggerFactory.getLogger(AnswerService.class);
    private final AnswerRepository answerRepository;
    private final UserRepository userRepository;
    private final QuestionRepository questionRepository;
    private final VoteRepository voteRepository;
    private final FollowService followService;
    private final EmailQueue emailQueue;


    public AnswerService(AnswerRepository answerRepository,
                         UserRepository userRepository,
                         QuestionRepository questionRepository,
                         VoteRepository voteRepository,
                         FollowService followService,
                         EmailQueue emailQueue){
        this.answerRepository = answerRepository;
        this.userRepository = userRepository;
        this.questionRepository = questionRepository;
        this.voteRepository = voteRepository;
        this.followService = followService;
        this.emailQueue = emailQueue;
    }

    @Transactional
    public Answer saveAnswer(AnswerFormDto answerFormDto, Long questionId) {

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NoSuchElementException("Question not found"));
        User author = userRepository.findById(1L)
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

        Answer saved = answerRepository.save(answer);

        // Notify followers asynchronously
        notifyFollowers(question, saved);
        return saved;
    }

    @Transactional
    public Answer saveAnswer(AnswerFormDto answerFormDto, Long questionId, UserPrincipal currentUser) {
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new NoSuchElementException("Question not found"));

        Long authorId = (currentUser != null && currentUser.getId() != null) ? currentUser.getId() : 1L;
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new NoSuchElementException("Author not found"));

        if (answerFormDto.body() == null || answerFormDto.body().trim().isEmpty()) {
            throw new IllegalArgumentException("Answer body cannot be empty");
        }

        Answer answer = new Answer();
        answer.setBody(answerFormDto.body().trim());
        answer.setQuestion(question);
        answer.setAuthor(author);
        answer.setScore(0);
        answer.setAccepted(false);

        Answer saved = answerRepository.save(answer);
        notifyFollowers(question, saved);
        return saved;
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

    @Transactional
    public void acceptAnswer(Long answerId, UserPrincipal principal) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new NoSuchElementException("Answer not found"));

        Question question = answer.getQuestion();

        if (!question.getAuthor().getId().equals(principal.getId())) {
            throw new AccessDeniedException("Only the question author can accept an answer");
        }

        List<Answer> answers = answerRepository.getAnswersByQuestionId(question.getId());
        for (Answer a : answers) {
            if (a.isAccepted()) {
                a.setAccepted(false);
                answerRepository.save(a);
            }
        }

        answer.setAccepted(true);
        answerRepository.save(answer);
    }

    public Optional<Answer> findById(Long answerId) {
        return answerRepository.findById(answerId);
    }

    private void notifyFollowers(Question question, Answer answer) {
        Long questionId = question.getId();
        String title = question.getTitle() != null ? question.getTitle() : ("Question #" + questionId);

        List<User> followers = followService.getFollowersForQuestion(questionId);
        if (followers == null || followers.isEmpty()) return;

        String subject = "New answer on: " + title;
        String link = "/questions/" + questionId;

        String answerAuthorEmail = answer.getAuthor() != null ? answer.getAuthor().getEmail() : null;

        for (User u : followers) {
            if (u == null || u.getEmail() == null) continue;
            if (answerAuthorEmail != null && answerAuthorEmail.equalsIgnoreCase(u.getEmail())) continue; // skip sender
            String personalizedBody = "Hi " + u.getEmail() + ",\n\n" +
                    "An answer has been submitted to a question you follow.\n" +
                    "Title: " + title + "\n" +
                    "Link: " + link + "\n\n" +
                    "You received this because you follow this question.\n--\nStackOverflowClone";
            EmailTaskDto dto = new EmailTaskDto(u.getEmail(), subject, personalizedBody);
            boolean offered = emailQueue.offer(dto);
            logger.info("Email notify queued for follower: {} (offered={}) subject='{}'", u.getEmail(), offered, subject);
            if (!offered) {
                try {
                    emailQueue.enqueue(dto); // block if full to avoid dropping
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.warn("Interrupted while enqueuing email task for {}", u.getEmail());
                }
            }
        }
    }

    public void deleteAnswer(Long answerId) {
        answerRepository.deleteById(answerId);
    }
}
