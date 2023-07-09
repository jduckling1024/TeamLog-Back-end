package com.test.teamlog.domain.post.service;

import com.test.teamlog.domain.account.dto.UserRequest;
import com.test.teamlog.domain.account.model.User;
import com.test.teamlog.domain.post.dto.PostCreateInput;
import com.test.teamlog.domain.post.dto.PostUpdateInput;
import com.test.teamlog.domain.post.repository.PostRepository;
import com.test.teamlog.entity.*;
import com.test.teamlog.exception.ResourceAlreadyExistsException;
import com.test.teamlog.exception.ResourceNotFoundException;
import com.test.teamlog.payload.*;
import com.test.teamlog.repository.*;
import com.test.teamlog.service.FileStorageService;
import com.test.teamlog.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PostService {
    private final PostRepository postRepository;
    private final PostTagRepository postTagRepository;
    private final PostMediaRepository postMediaRepository;
    private final PostLikerRepository postLikerRepository;
    private final PostUpdateHistoryRepository postUpdateHistoryRepository;
    private final ProjectRepository projectRepository;
    private final UserFollowRepository userFollowRepository;
    private final FileStorageService fileStorageService;
    private final ProjectService projectService;

    public List<PostDTO.PostResponse> getPostsByUser(User currentUser) {
        List<PostDTO.PostResponse> responses = new ArrayList<>();
        List<Post> posts = postRepository.findAllByWriter(currentUser);

        for (Post post : posts) {
            responses.add(convertToPostResponse(post, currentUser));
        }
        return responses;
    }

    public List<PostDTO.PostResponse> getPostsByFollowingUser(User currentUser) {
        List<UserFollow> userFollowingList = userFollowRepository.findByFromUser(currentUser);
        List<PostDTO.PostResponse> responses = new ArrayList<>();
        if (userFollowingList == null) return responses;

        List<User> userFollowings = new ArrayList<>();
        for (UserFollow userFollow : userFollowingList) {
            userFollowings.add(userFollow.getToUser());
        }
        List<Post> posts = postRepository.findAllByWriters(userFollowings);

        for (Post post : posts) {
            responses.add(convertToPostResponse(post, currentUser));
        }

        return responses;
    }

    // 단일 포스트 조회
    public PostDTO.PostResponse getPost(Long id, User currentUser) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
        if (post.getAccessModifier() == AccessModifier.PRIVATE) {
            projectService.validateUserIsMemberOfProject(post.getProject(), currentUser);
        }
        return convertToPostResponse(post, currentUser);
    }

    // 모든 포스트 조회
    public PagedResponse<PostDTO.PostResponse> getAllPosts(int page, int size, User currentUser) {
        Pageable pageable = PageRequest.of(0, size, Sort.Direction.DESC, "createTime");

        Page<Post> posts = postRepository.findAll(pageable);
        List<PostDTO.PostResponse> responses = new ArrayList<>();
        for (Post post : posts) {
            responses.add(convertToPostResponse(post, currentUser));
        }
        return new PagedResponse<>(responses, posts.getNumber(), posts.getSize(), posts.getTotalElements(),
                posts.getTotalPages(), posts.isLast());
    }

    // 프로젝트 내 포스트 조회
    public PagedResponse<PostDTO.PostResponse> getPostsByProject(Long projectId, Sort.Direction sort, String cop,
                                                                 Long cursor, int size, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        Pageable pageable = PageRequest.of(0, size, sort, "id");
        Boolean isUserMemberOfProject = projectService.isUserMemberOfProject(project, currentUser);

        Slice<Post> posts = null;
        if (cursor == null) {
            if (isUserMemberOfProject)
                posts = postRepository.findAllByProject(project, pageable);
            else
                posts = postRepository.findAllByProject(project, AccessModifier.PUBLIC, pageable);
        } else {
            if (isUserMemberOfProject)
                posts = postRepository.findAllByProjectAndCursor(project, cursor, cop, pageable);
            else
                posts = postRepository.findAllByProjectAndCursor(project, cursor, cop, AccessModifier.PUBLIC, pageable);
        }

        List<PostDTO.PostResponse> responses = new ArrayList<>();
        for (Post post : posts) {
            responses.add(convertToPostResponse(post, currentUser));
        }

        long totalElements = 0;

        if (isUserMemberOfProject)
            totalElements = postRepository.getPostsCount(project);
        else
            totalElements = postRepository.getPostsCount(project, AccessModifier.PUBLIC);

        return new PagedResponse<>(responses, 0, posts.getSize(), totalElements,
                0, posts.isLast());
    }

    // 키워드로 게시물 조회
    public PagedResponse<PostDTO.PostResponse> searchPostsInProject(Long projectId, String keyword, Sort.Direction sort,
                                                                    String cop, Long cursor, int size, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        Boolean isUserMemberOfProject = projectService.isUserMemberOfProject(project, currentUser);
        Pageable pageable = PageRequest.of(0, size, sort, "id");

        Slice<Post> posts = null;
        if (cursor == null) {
            if (isUserMemberOfProject)
                posts = postRepository.searchPostsInProject(project, keyword, pageable);
            else
                posts = postRepository.searchPostsInProject(project, keyword, AccessModifier.PUBLIC, pageable);
        } else {
            if (isUserMemberOfProject)
                posts = postRepository.searchPostsInProjectByCursor(project, cursor, keyword, cop, pageable);
            else
                posts = postRepository.searchPostsInProjectByCursor(project, cursor, keyword, cop, AccessModifier.PUBLIC, pageable);
        }

        List<PostDTO.PostResponse> responses = new ArrayList<>();
        for (Post post : posts) {
            responses.add(convertToPostResponse(post, currentUser));
        }
        long totalElements = 0;
        if (isUserMemberOfProject)
            totalElements = postRepository.getPostsCountByKeyword(project, keyword);
        else
            totalElements = postRepository.getPostsCountByKeyword(project, keyword, AccessModifier.PUBLIC);

        return new PagedResponse<>(responses, 0, posts.getSize(), totalElements,
                0, posts.isLast());
    }

    // 해시태그 선별 조회 + 키워드 검색
    public PagedResponse<PostDTO.PostResponse> searchPostsInProjectByHashtagAndKeyword(Long projectId,
                                                                                       String keyword,
                                                                                       List<String> names,
                                                                                       Sort.Direction sort,
                                                                                       String cop,
                                                                                       Long cursor,
                                                                                       int size,
                                                                                       User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        Boolean isUserMemberOfProject = projectService.isUserMemberOfProject(project, currentUser);
        Pageable pageable = PageRequest.of(0, size, sort, "id");

        Slice<Post> posts = null;
        if (cursor == null) {
            if (isUserMemberOfProject)
                posts = postRepository.searchPostsInProjectByHashtagAndKeyword(project, names, keyword, pageable);
            else
                posts = postRepository.searchPublicPostsInProjectByHashtagAndKeyword(project, names, keyword, AccessModifier.PUBLIC, pageable);
        } else {
            if (isUserMemberOfProject)
                posts = postRepository.searchPostsInProjectByHashtagAndKeywordAndCursor(project, cursor, names,
                        keyword, cop, pageable);
            else
                posts = postRepository.searchPublicPostsInProjectByHashtagAndKeywordAndCursor(project, cursor, names,
                        keyword, cop, AccessModifier.PUBLIC, pageable);
        }
        List<PostDTO.PostResponse> responses = new ArrayList<>();
        for (Post post : posts) {
            responses.add(convertToPostResponse(post, currentUser));
        }
        long totalElements = 0;
        if (isUserMemberOfProject) {
            postRepository.getPostsCountByHashtagAndKeyword(project, names, keyword, pageable).getTotalElements();
        } else {
            postRepository.getPostsCountByHashtagAndKeyword(project, names, keyword, AccessModifier.PUBLIC, pageable).getTotalElements();
        }

        return new PagedResponse<>(responses, 0, posts.getSize(), totalElements,
                0, posts.isLast());
    }

    // 해시태그 선별 조회
    public PagedResponse<PostDTO.PostResponse> getPostsInProjectByHashtag(Long projectId, List<String> names, Sort.Direction sort,
                                                                          String cop, Long cursor, int size, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        Pageable pageable = PageRequest.of(0, size, sort, "id");
        Boolean isUserMemberOfProject = projectService.isUserMemberOfProject(project, currentUser);

        Slice<Post> posts = null;
        if (cursor == null) {
            if (isUserMemberOfProject)
                posts = postRepository.getPostsInProjectByHashTag(project, names, pageable);
            else
                posts = postRepository.getPostsInProjectByHashTag(project, names, AccessModifier.PUBLIC, pageable);
        } else {
            if (isUserMemberOfProject)
                posts = postRepository.getPostsInProjectByHashTagAndCursor(project, cursor, names, cop, pageable);
            else
                posts = postRepository.getPostsInProjectByHashTagAndCursor(project, cursor, names, cop, AccessModifier.PUBLIC, pageable);
        }
        List<PostDTO.PostResponse> responses = new ArrayList<>();
        for (Post post : posts) {
            responses.add(convertToPostResponse(post, currentUser));
        }
        long totalElements = 0;
        if (isUserMemberOfProject)
            totalElements = postRepository.getPostsCountByHashTag(project, names, pageable).getTotalElements();
        else
            totalElements = postRepository.getPostsCountByHashTag(project, names, AccessModifier.PUBLIC, pageable).getTotalElements();


        return new PagedResponse<>(responses, 0, posts.getSize(), totalElements,
                0, posts.isLast());
    }

    // 프로젝트의 해시태그들 조회
    public List<String> getHashTagsInProjectPosts(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));

        List<String> hashtags = postTagRepository.getHashTagsInProjectPosts(project);

        return hashtags;
    }

    // 해시태그 추천
    public List<String> getRecommendedHashTags(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", id));

        List<PostTagInfo> hashtags = postTagRepository.getRecommendedHashTags(id);
        List<String> response = new ArrayList<>();
        for (PostTagInfo tag : hashtags) {
            response.add(tag.getName());
        }
        return response;
    }

    // 위치정보가 있는 Public 포스트들 조회
    public List<PostDTO.PostResponse> getLocationPosts(User currentUser) {
        List<Post> posts = postRepository.findAllByLocationIsNotNullAndAccessModifier(AccessModifier.PUBLIC);

        List<PostDTO.PostResponse> responses = new ArrayList<>();
        for (Post post : posts) {
            responses.add(convertToPostResponse(post, currentUser));
        }
        return responses;
    }

    // 위치정보가 있는 프로젝트의 포스트들 조회
    public List<PostDTO.PostResponse> getLocationPosts(Long projectId, User currentUser) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", projectId));
        Boolean isUserMemberOfProject = projectService.isUserMemberOfProject(project, currentUser);

        List<Post> posts = null;
        if (isUserMemberOfProject)
            posts = postRepository.findAllPostsWithLocationByProject(project);
        else
            posts = postRepository.findAllPostsWithLocationByProject(project, AccessModifier.PUBLIC);

        List<PostDTO.PostResponse> responses = new ArrayList<>();
        for (Post post : posts) {
            responses.add(convertToPostResponse(post, currentUser));
        }

        return responses;
    }


    // 포스트 생성
    @Transactional
    public Long create(PostCreateInput input,
                       MultipartFile[] media,
                       MultipartFile[] files,
                       User currentUser) {
        Project project = projectRepository.findById(input.getProjectId())
                .orElseThrow(() -> new ResourceNotFoundException("Project", "ID", input.getProjectId()));
        projectService.validateUserIsMemberOfProject(project, currentUser);

        input.setLocation(makeLocation(input.getLatitude(), input.getLongitude()));
        Post post = input.toPost(project, currentUser);

        if (!CollectionUtils.isEmpty(input.getHashtags())) {
            final List<PostTag> postTagList
                    = input.getHashtags()
                    .stream().map(hashTag -> PostTag.builder().name(hashTag).build())
                    .collect(Collectors.toList());
            postTagList.forEach(tag -> tag.addPost(post));
        }

        Post newPost = postRepository.save(post);

        storeMediaFiles(media, post);
        storeFiles(files, post);

        createPostUpdateHistory(currentUser, post);

        project.setUpdateTime(LocalDateTime.now());
        return newPost.getId();
    }

    private void storeFiles(MultipartFile[] files, Post post) {
        if (files == null) return;

        for (MultipartFile file : files) {
            fileStorageService.storeFile(file, post, Boolean.FALSE);
        }
    }

    private void storeMediaFiles(MultipartFile[] media, Post post) {
        if (media == null) return;

        for (MultipartFile file : media) {
            fileStorageService.storeFile(file, post, Boolean.TRUE);
        }
    }

    // 포스트 수정
    @Transactional
    public Long update(Long id,
                       PostUpdateInput input,
                       MultipartFile[] media,
                       MultipartFile[] files, User currentUser) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
        projectService.validateUserIsMemberOfProject(post.getProject(), currentUser);

        post.update(input.getContents(), input.getAccessModifier(), input.getCommentModifier(), makeLocation(input.getLatitude(), input.getLongitude()), input.getAddress());

        // 취소한 파일 삭제 후 새로운 파일 저장
        fileStorageService.deleteFileById(input.getDeletedFileIdList());
        storeMediaFiles(media, post);
        storeFiles(files, post);

        updatePostTagList(input.getHashtags(), post);

        createPostUpdateHistory(currentUser, post);

        return post.getId();
    }

    private static Point makeLocation(Double latitude, Double longitude) {
        Point location = null;
        if (latitude != null && longitude != null) {
            location = new GeometryFactory().createPoint(new Coordinate(latitude, longitude));
        }

        return location;
    }

    private void updatePostTagList(List<String> inputHashTagNameList, Post post) {
        List<PostTag> originalHashTags = post.getHashtags();

        if (CollectionUtils.isEmpty(inputHashTagNameList)) {
            post.removeHashTags(originalHashTags);
        } else {
            List<String> maintainedHashTagNameList = new ArrayList<>(); // 기존 해시태그
            List<PostTag> deletedHashTags = new ArrayList<>(); // 삭제된 해시태그

            for (PostTag tag : originalHashTags) {
                final String tagName = tag.getName();

                if (inputHashTagNameList.contains(tagName)) {
                    maintainedHashTagNameList.add(tagName);
                } else {
                    deletedHashTags.add(tag);
                }
            }

            inputHashTagNameList.removeAll(maintainedHashTagNameList);
            List<PostTag> hashtags = inputHashTagNameList.stream()
                    .map(tagName -> PostTag.builder()
                            .name(tagName)
                            .post(post)
                            .build()).collect(Collectors.toList());

            // FIXME: N번 쿼리 날리는 현상. 추후 수정 필요
            post.removeHashTags(deletedHashTags);
            post.addHashTags(hashtags);
        }
    }

    // 포스트 삭제
    @Transactional
    public ApiResponse delete(Long id, User currentUser) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", id));
        projectService.validateUserIsMemberOfProject(post.getProject(), currentUser);

        fileStorageService.deleteFilesByPost(post);
        postRepository.delete(post);
        return new ApiResponse(Boolean.TRUE, "포스트 삭제 성공");
    }

    // Post to PostResponse
    private PostDTO.PostResponse convertToPostResponse(Post post, User currentUser) {
        UserRequest.UserSimpleInfo writer = new UserRequest.UserSimpleInfo(post.getWriter());

        List<String> hashtags = new ArrayList<>();
        List<PostTag> hashtagList = postTagRepository.findAllByPost(post);
        if (hashtagList != null) {
            for (PostTag tag : post.getHashtags())
                hashtags.add(tag.getName());
        }

        List<FileDTO.FileInfo> media = new ArrayList<>();
        List<FileDTO.FileInfo> files = new ArrayList<>();
        List<PostMedia> mediaList = postMediaRepository.findAllByPost(post);
        if (mediaList != null) {
            for (PostMedia temp : mediaList) {
                FileDTO.FileInfo fileInfo = FileDTO.FileInfo.builder()
                        .id(temp.getId())
                        .contentType(temp.getContentType())
                        .fileName(temp.getFileName())
                        .build();
                if (temp.getIsMedia()) {
                    String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                            .path("/resources/")
                            .path(temp.getStoredFileName())
                            .toUriString();
                    fileInfo.setFileDownloadUri(fileDownloadUri);
                    media.add(fileInfo);
                } else {
                    String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                            .path("/api/downloadFile/")
                            .path(temp.getStoredFileName())
                            .toUriString();
                    fileInfo.setFileDownloadUri(fileDownloadUri);
                    files.add(fileInfo);
                }
            }
        }

        int likeCount = 0;
        if (post.getPostLikers() != null) likeCount = post.getPostLikers().size();
        int commentCount = 0;
        if (post.getComments() != null) commentCount = post.getComments().size();

        Boolean isIlikeIt = Boolean.FALSE;
        if (currentUser != null) isIlikeIt = isILikeIt(post, currentUser);
        PostDTO.PostResponse postResponse = PostDTO.PostResponse.builder()
                .isILikeIt(isIlikeIt)
                .id(post.getId())
                .project(new ProjectDTO.ProjectSimpleInfo(post.getProject()))
                .writer(writer)
                .accessModifier(post.getAccessModifier())
                .commentModifier(post.getCommentModifier())
                .contents(post.getContents())
                .hashtags(hashtags)
                .media(media)
                .files(files)
                .likeCount(likeCount)
                .commentCount(commentCount)
                .writeTime(post.getCreateTime())
                .writeTimeStr(post.getCreateTime().toString())
                .build();
        if (post.getLocation() != null) {
            postResponse.setLatitude(post.getLocation().getX());
            postResponse.setLongitude(post.getLocation().getY());
            postResponse.setAddress(post.getAddress());
        }

        return postResponse;
    }

    @Transactional
    public List<PostDTO.PostHistoryInfo> getPostUpdateHistory(Long postId, User currentUser) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));
        projectService.validateUserIsMemberOfProject(post.getProject(), currentUser);

        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        List<PostUpdateHistory> historyList = postUpdateHistoryRepository.findAllByPost(post, sort);
        List<PostDTO.PostHistoryInfo> historyResponse = new ArrayList<>();
        for (PostUpdateHistory history : historyList) {
            historyResponse.add(new PostDTO.PostHistoryInfo(history));
        }
        return historyResponse;
    }


    // -------------------------------
    // ------- 포스트 좋아요 관리 -------
    // -------------------------------
    // 좋아요
    @Transactional
    public ApiResponse likePost(Long postId, User currentUser) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        PostLiker postLiker = PostLiker.builder()
                .post(post)
                .user(currentUser)
                .build();

        try {
            postLikerRepository.saveAndFlush(postLiker);
        } catch (DataIntegrityViolationException e) {
            throw new ResourceAlreadyExistsException("좋아요는 한번만 가능합니다.");
        }

        return new ApiResponse(Boolean.TRUE, "포스트 좋아요 성공");
    }

    // 좋아요 취소
    @Transactional
    public ApiResponse unlikePost(Long postId, User currentUser) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        PostLiker postLiker = postLikerRepository.findByPostAndUser(post, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("PostLiker", "UserId", currentUser.getIdentification()));

        postLikerRepository.delete(postLiker);
        return new ApiResponse(Boolean.TRUE, "포스트 좋아요 취소 성공");
    }

    // 포스트 좋아요 목록 조회
    public List<UserRequest.UserSimpleInfo> getPostLikerList(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        List<PostLiker> postLikers = postLikerRepository.findAllByPost(post);
        List<UserRequest.UserSimpleInfo> response = new ArrayList<>();
        for (PostLiker postLiker : postLikers) {
            UserRequest.UserSimpleInfo temp = new UserRequest.UserSimpleInfo(postLiker.getUser());
            response.add(temp);
        }
        return response;
    }

    public Boolean isILikeIt(Post post, User currentUser) {
        return postLikerRepository.findByPostAndUser(post, currentUser).isPresent();
    }

    // TODO: PostUpdateHistoryService로 이동
    private void createPostUpdateHistory(User currentUser, Post post) {
        PostUpdateHistory history = PostUpdateHistory.builder()
                .post(post)
                .user(currentUser)
                .build();
        postUpdateHistoryRepository.save(history);
    }
}
