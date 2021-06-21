package our.yurivongella.instagramclone.controller;

import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import our.yurivongella.instagramclone.controller.dto.profile.ProfilePostDto;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import our.yurivongella.instagramclone.service.ProfileService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService profileService;

    @ApiOperation("내 게시글 리스트 조회 (lastPostId 기준으로 최대 12개씩)")
    @GetMapping("/member/posts")
    public ResponseEntity<List<ProfilePostDto>> getMyPosts(@RequestParam(required = false) Long lastPostId) {
        return ResponseEntity.ok(profileService.getMyPosts(lastPostId));
    }

    @ApiOperation("특정 사용자의 게시글 리스트 조회 (lastPostId 기준으로 최대 12개씩)")
    @GetMapping("/members/{displayId}/posts")
    public ResponseEntity<List<ProfilePostDto>> getPosts(@PathVariable String displayId, @RequestParam(required = false) Long lastPostId) {
        return ResponseEntity.ok(profileService.getPosts(displayId, lastPostId));
    }
}
