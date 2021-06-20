package our.yurivongella.instagramclone.service;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import our.yurivongella.instagramclone.controller.dto.ProcessStatus;
import our.yurivongella.instagramclone.controller.dto.comment.CommentReqDto;
import our.yurivongella.instagramclone.controller.dto.comment.CommentDto;
import our.yurivongella.instagramclone.controller.dto.comment.CommentResDto;
import our.yurivongella.instagramclone.domain.SliceHelper;
import our.yurivongella.instagramclone.domain.comment.CommentLike;
import our.yurivongella.instagramclone.domain.comment.CommentLikeRepository;
import our.yurivongella.instagramclone.domain.member.Member;
import our.yurivongella.instagramclone.domain.post.Post;
import our.yurivongella.instagramclone.domain.post.PostRepository;
import our.yurivongella.instagramclone.exception.CustomException;
import our.yurivongella.instagramclone.exception.ErrorCode;
import our.yurivongella.instagramclone.util.SecurityUtil;

import com.sun.istack.NotNull;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import our.yurivongella.instagramclone.domain.comment.Comment;
import our.yurivongella.instagramclone.domain.comment.CommentRepository;
import our.yurivongella.instagramclone.domain.member.MemberRepository;

@Transactional(readOnly = true)
@Service
@Slf4j
@RequiredArgsConstructor
public class CommentService {
    private static final int COMMENT_PAGE_SIZE = 12;
    private static final int NESTED_COMMENT_PAGE_SIZE = 3;

    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;

    public CommentResDto getCommentsFromPost(Long postId, Long lastId) {
        Member member = getCurrentMember();
        final List<Comment> content = commentRepository.findCommentsFromPost(postId, lastId, COMMENT_PAGE_SIZE);// 그냥 댓글만 가져옴
        return getCommentResDto(member, content);
    }

    private CommentResDto getCommentResDto(final Member member, final List<Comment> content) {
        CommentResDto commentResDto = new CommentResDto();

        final boolean hasNext = SliceHelper.checkLast(content, COMMENT_PAGE_SIZE);
        commentResDto.setCommentResDtos(SliceHelper.getContents(content, hasNext, COMMENT_PAGE_SIZE)
                                                   .stream()
                                                   .map(comment -> CommentDto.of(comment, member))
                                                   .collect(toList()));
        commentResDto.setHasNext(hasNext);
        return commentResDto;
    }

    @Transactional
    public CommentDto createComment(Long postId, CommentReqDto commentReqDto) {
        Member member = getCurrentMember();

        Post post = postRepository.findById(postId)
                                  .orElseThrow(() -> new NoSuchElementException("해당 게시물이 없습니다."));

        Comment comment = new Comment(member, post, commentReqDto.getContent());
        commentRepository.save(comment);
        return CommentDto.of(comment, member);
    }

    @Transactional
    public ProcessStatus deleteComment(@NotNull Long commentId) {
        Member member = getCurrentMember();
        Comment comment = getCurrentComment(commentId);

        if (!member.equals(comment.getMember())) {
            log.error("유저가 일치하지 않습니다.");
            return ProcessStatus.FAIL;
        }

        try {
            commentRepository.delete(comment);
            comment.getPost().minusCommentCount();
        } catch (Exception e) {
            log.error("삭제 도중 문제가 발생했습니다");
            throw e;
        }
        return ProcessStatus.SUCCESS;
    }

    @Transactional
    public ProcessStatus likeComment(@NotNull Long commentId) {
        Member member = getCurrentMember();
        Comment comment = getCurrentComment(commentId);
        try {
            CommentLike commentLike = createCommentLike(member, comment);
            log.info("{}가 댓글 {}를 좋아요 표시합니다.", member.getDisplayId(), comment.getContent());
            commentLikeRepository.save(commentLike);
        } catch (Exception e) {
            log.error("[댓글 번호:{}] {}가 좋아요 도중 에러가 발생했습니다.", comment.getId(), member.getDisplayId());
            throw e;
        }
        return ProcessStatus.SUCCESS;
    }

    private CommentLike createCommentLike(Member member, Comment comment) {
        commentLikeRepository.findByCommentIdAndMemberId(comment.getId(), member.getId()).ifPresent(commentLike -> {
            log.error("[댓글 번호:{}] {}가 이미 좋아요를 하고 있습니다.", comment.getId(), member.getId());
            throw new CustomException(ErrorCode.ALREADY_LIKE);
        });
        return new CommentLike().like(member, comment);
    }

    @Transactional
    public ProcessStatus unlikeComment(@NotNull Long commentId) {
        Member member = getCurrentMember();
        Comment comment = getCurrentComment(commentId);
        CommentLike commentLike = commentLikeRepository.findByCommentIdAndMemberId(commentId, member.getId()).orElseThrow(() -> new CustomException(ErrorCode.ALREADY_UNLIKE));

        try {
            commentLike.unlike();
            commentLikeRepository.delete(commentLike);
        } catch (Exception e) {
            log.error("[댓글 번호 : {}] {}가 좋아요 취소 도중 에러가 발생했습니다.", comment.getId(), member.getDisplayId());
            return ProcessStatus.FAIL;
        }
        return ProcessStatus.SUCCESS;
    }

    private Member getCurrentMember() {
        return memberRepository.findById(SecurityUtil.getCurrentMemberId())
                               .orElseThrow(() -> new NoSuchElementException("현재 계정 정보가 존재하지 않습니다."));
    }

    private Comment getCurrentComment(Long commentId) {
        return commentRepository.findById(commentId)
                                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
    }

    @Transactional
    public CommentDto createNestedComment(final Long baseCommentId, final CommentReqDto commentReqDto) {
        final Member currentMember = getCurrentMember();
        final Comment baseComment = commentRepository.findById(baseCommentId).orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
        final Comment comment = commentReqDto.toEntity(currentMember, baseComment.getPost());
        comment.addComment(baseComment);

        final Comment savedComment = commentRepository.save(comment);
        return CommentDto.of(savedComment, currentMember);
    }

    public CommentResDto findNestedComments(final Long commentId, final Long lastId) {
        Member member = getCurrentMember();
        List<Comment> content = commentRepository.findNestedCommentsById(commentId, lastId, NESTED_COMMENT_PAGE_SIZE);
        return getCommentResDto(member, content);
    }
}
