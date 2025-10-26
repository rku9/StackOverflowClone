package com.mountblue.stackoverflowclone.services;

import com.mountblue.stackoverflowclone.models.Answer;
import com.mountblue.stackoverflowclone.models.Comment;
import com.mountblue.stackoverflowclone.models.Question;
import com.mountblue.stackoverflowclone.models.User;
import com.mountblue.stackoverflowclone.repositories.CommentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;

    @Autowired
    public CommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    // Create a comment for a question
    public Comment createQuestionComment(String content, Question question, User user) {
        Comment comment = new Comment();
        comment.setBody(content);
        comment.setQuestion(question);
        comment.setAuthor(user);
        comment.setCreatedAt(LocalDateTime.now());
        return commentRepository.save(comment);
    }

    // Create a comment for an answer
    public Comment createAnswerComment(String content, Answer answer, User user) {
        Comment comment = new Comment();
        comment.setBody(content);
        comment.setAnswer(answer);
        comment.setAuthor(user);
        comment.setCreatedAt(LocalDateTime.now());
        return commentRepository.save(comment);
    }

    // Save a comment
    public Comment save(Comment comment) {
        if (comment.getCreatedAt() == null) {
            comment.setCreatedAt(LocalDateTime.now());
        }
        return commentRepository.save(comment);
    }

    // Find comment by ID
    @Transactional(readOnly = true)
    public Optional<Comment> findById(Long id) {
        return commentRepository.findById(id);
    }

    // Get all comments for a question
    @Transactional(readOnly = true)
    public List<Comment> getCommentsByQuestion(Long questionId) {
        return commentRepository.findByQuestionIdWithAuthor(questionId);
    }

    // Get all comments for an answer
    @Transactional(readOnly = true)
    public List<Comment> getCommentsByAnswer(Long answerId) {
        return commentRepository.findByAnswerIdOrderByCreatedAtAsc(answerId);
    }

    // Get all comments by a user
    @Transactional(readOnly = true)
    public List<Comment> getCommentsByUser(Long userId) {
        return commentRepository.findByAuthorId(userId);
    }

    // Update a comment
    public Comment update(Long id, String content) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + id));
        comment.setBody(content);
        comment.setUpdatedAt(LocalDateTime.now());
        return commentRepository.save(comment);
    }

    public void delete(Long id) {
        if (!commentRepository.existsById(id)) {
            throw new RuntimeException("Comment not found with id: " + id);
        }
        commentRepository.deleteById(id);
    }

    public void deleteAllByQuestion(Long questionId) {
        commentRepository.deleteByQuestionId(questionId);
    }

    public void deleteAllByAnswer(Long answerId) {
        commentRepository.deleteByAnswerId(answerId);
    }

    @Transactional(readOnly = true)
    public Long countByQuestion(Long questionId) {
        return commentRepository.countByQuestionId(questionId);
    }

    @Transactional(readOnly = true)
    public Long countByAnswer(Long answerId) {
        return commentRepository.countByAnswerId(answerId);
    }

    @Transactional(readOnly = true)
    public boolean isCommentOwner(Long commentId, String username) {
        Optional<Comment> comment = commentRepository.findById(commentId);
        return comment.isPresent() &&
                comment.get().getAuthor().getEmail().equals(username);
    }
}
