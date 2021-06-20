package our.yurivongella.instagramclone.controller.dto.post;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import our.yurivongella.instagramclone.controller.dto.comment.CommentResponseDto;
import our.yurivongella.instagramclone.controller.dto.member.MemberDto;
import our.yurivongella.instagramclone.domain.member.Member;
import our.yurivongella.instagramclone.domain.post.MediaUrl;
import our.yurivongella.instagramclone.domain.post.Post;
import our.yurivongella.instagramclone.util.DateTimeUtil;

@Getter
@NoArgsConstructor
public class PostReadResponseDto {
    private Long id;
    private MemberDto author;

    @JsonProperty("images")
    private List<String> mediaUrls;

    @JsonProperty("body")
    private String content;
    private String createdAt;
    private String modifiedAt;
    private Boolean isLike;

    @JsonProperty("likeUser")
    private MemberDto usersWhoLike;

    @JsonProperty("likeLength")
    private Integer likeCount;
    private List<CommentResponseDto> commentPreview;

    @JsonProperty("commentLength")
    private Integer commentCount;
    private Long viewCount;
    //Boolean bookMark;

    public PostReadResponseDto setCommentPreview(List<CommentResponseDto> commentPreview) {
        this.commentPreview = commentPreview;
        return this;
    }

    @Builder
    public PostReadResponseDto(Long id, MemberDto author, List<String> mediaUrls, String content, String createdAt, String modifiedAt, Boolean isLike,
                               MemberDto usersWhoLike, Integer likeCount, List<CommentResponseDto> commentPreview, Integer commentCount, Long viewCount) {
        this.id = id;
        this.author = author;
        this.mediaUrls = mediaUrls;
        this.content = content;
        this.createdAt = createdAt;
        this.modifiedAt = modifiedAt;
        this.isLike = isLike;
        this.usersWhoLike = usersWhoLike;
        this.likeCount = likeCount;
        this.commentPreview = commentPreview;
        this.commentCount = commentCount;
        this.viewCount = viewCount;
    }

    public static PostReadResponseDto of(Post post, Member member) {
        return PostReadResponseDto.builder()
                                  .id(post.getId())
                                  .author(MemberDto.of(post.getMember(), member))
                                  .mediaUrls(post.getMediaUrls().stream().map(MediaUrl::getUrl).collect(Collectors.toList()))
                                  .content(post.getContent())
                                  .isLike(post.getPostLikes().stream().anyMatch(v -> v.getMember().getId().equals(member.getId())))
                                  .usersWhoLike(post.getPostLikes().stream().findFirst().map(v -> MemberDto.of(v.getMember(), member)).orElse(null))
                                  .likeCount(post.getPostLikes().size())
                                  .commentCount(post.getComments().size())
                                  .viewCount(post.getViews())
                                  .createdAt(DateTimeUtil.from(post.getCreatedDate()))
                                  .modifiedAt(DateTimeUtil.from(post.getModifiedDate()))
                                  .build();
    }
}