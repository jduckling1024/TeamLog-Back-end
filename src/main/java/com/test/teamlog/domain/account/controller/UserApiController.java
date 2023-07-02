package com.test.teamlog.domain.account.controller;

import com.test.teamlog.domain.account.dto.*;
import com.test.teamlog.domain.account.model.User;
import com.test.teamlog.domain.account.service.UserService;
import com.test.teamlog.global.security.UserAdapter;
import com.test.teamlog.payload.ApiResponse;
import com.test.teamlog.payload.CommentDTO;
import com.test.teamlog.payload.PostDTO;
import com.test.teamlog.service.CommentService;
import com.test.teamlog.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;

import static org.springframework.http.HttpHeaders.SET_COOKIE;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "유저 관리")
public class UserApiController {

    @Value("${cookie.domain}")
    private String cookieDomain;
    private final UserService userService;
    private final PostService postService;
    private final CommentService commentService;

    @Operation(summary = "로그인")
    @PostMapping("/sign-in")
    public ResponseEntity<SignInResponse> signIn(@Valid @RequestBody SignInRequest request,
                                                 HttpServletResponse httpServletResponse) {
        final SignInResult result = userService.signIn(request.toInput());

        ResponseCookie cookie = ResponseCookie.from("Refresh-Token", result.getRefreshToken())
                .domain(cookieDomain)
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .build();
        httpServletResponse.addHeader(SET_COOKIE, cookie.toString());

        return new ResponseEntity<>(SignInResponse.of(result), HttpStatus.OK);
    }

    @Operation(summary = "로그인 검증")
    @GetMapping("/validate")
    public ResponseEntity<UserRequest.UserSimpleInfo> validateUser(@Parameter(hidden = true) @AuthenticationPrincipal UserAdapter currentUser) {
        if (currentUser == null) {
            return new ResponseEntity<>(new UserRequest.UserSimpleInfo(), HttpStatus.UNAUTHORIZED);
        } else {
            return new ResponseEntity<>(new UserRequest.UserSimpleInfo(currentUser.getUser()), HttpStatus.OK);
        }
    }

    @Operation(summary = "회원 가입")
    @PostMapping("/users")
    public ResponseEntity<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
        final SignUpResult result = userService.signUp(request.toInput());
        return new ResponseEntity<>(SignUpResponse.from(result), HttpStatus.CREATED);
    }

    @Operation(summary = "회원 정보 조회")
    @GetMapping("/users/{id}")
    public ResponseEntity<UserRequest.UserResponse> getUserById(@PathVariable("id") String id,
                                                                @Parameter(hidden = true) @AuthenticationPrincipal User currentUser) {
        UserRequest.UserResponse userResponse = userService.getUser(id, currentUser);
        return new ResponseEntity<>(userResponse, HttpStatus.OK);
    }

    @Operation(summary = "회원 정보 수정")
    @PutMapping("/users")
    public ResponseEntity<ApiResponse> updateUser(@Valid @RequestPart(value = "key", required = true) UserRequest.UserUpdateRequest userRequest,
                                                  @RequestPart(value = "profileImg", required = false) MultipartFile image,
                                                  @Parameter(hidden = true) @AuthenticationPrincipal User currentUser) {
        ApiResponse apiResponse = userService.updateUser(userRequest, image, currentUser);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @Operation(summary = "회원 삭제")
    @DeleteMapping("/users")
    public ResponseEntity<ApiResponse> deleteUser(@Parameter(hidden = true) @AuthenticationPrincipal User currentUser) {
        ApiResponse apiResponse = userService.deleteUser(currentUser);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @Operation(summary = "프로필 이미지 변경")
    @PutMapping("/users/profile-image")
    public ResponseEntity<ApiResponse> updateUserProfileImage(@RequestPart(value = "profileImg", required = true) MultipartFile image,
                                                              @Parameter(hidden = true) @AuthenticationPrincipal User currentUser) {
        ApiResponse apiResponse = userService.updateUserProfileImage(image, currentUser);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @Operation(summary = "프로필 이미지 삭제")
    @DeleteMapping("/users/profile-image")
    public ResponseEntity<ApiResponse> deleteUserProfileImage(@Parameter(hidden = true) @AuthenticationPrincipal User currentUser) {
        ApiResponse apiResponse = userService.deleteUserProfileImage(currentUser);
        return new ResponseEntity<>(apiResponse, HttpStatus.OK);
    }

    @Operation(summary = "개인 작성 이력 조회 (게시물)")
    @GetMapping("/user/posts")
    public ResponseEntity<List<PostDTO.PostResponse>> getPostsByUser(@Parameter(hidden = true) @AuthenticationPrincipal User currentUser) {
        List<PostDTO.PostResponse> response = null;
        if (currentUser == null) {
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        } else {
            response = postService.getPostsByUser(currentUser);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }

    @Operation(summary = "개인 작성 이력 조회 (댓글)")
    @GetMapping("/user/comments")
    public ResponseEntity<List<CommentDTO.CommentInfo>> getCommentsByUser(@Parameter(hidden = true) @AuthenticationPrincipal User currentUser) {
        List<CommentDTO.CommentInfo> response = null;

        if (currentUser == null) {
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        } else {
            response = commentService.getCommentByUser(currentUser);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
    }

    @Operation(summary = "회원 검색")
    @GetMapping("/users")
    public ResponseEntity<List<UserRequest.UserSimpleInfo>> searchUser(@RequestParam(value = "id", required = false, defaultValue = "") String id,
                                                                       @RequestParam(value = "name", required = false, defaultValue = "") String name,
                                                                       @Parameter(hidden = true) @AuthenticationPrincipal User currentUser) {
        List<UserRequest.UserSimpleInfo> response = userService.searchUser(id, name);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}