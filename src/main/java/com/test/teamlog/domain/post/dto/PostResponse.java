package com.test.teamlog.domain.post.dto;

import com.test.teamlog.domain.postmedia.dto.PostMediaResult;
import com.test.teamlog.global.entity.AccessModifier;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PostResponse {
    private Long id;
    private Boolean isILikeIt;
    private ProjectSimpleInfoResponse project;
    private UserSimpleInfoResponse writer;
    private AccessModifier accessModifier;
    private AccessModifier commentModifier;
    private String contents;
    private Double latitude;
    private Double longitude;
    private String address;
    private int likeCount;
    private int commentCount;
    private LocalDateTime writeTime;
    private String writeTimeStr;
    private List<String> hashtags;
    private List<PostMediaResult> media;
    private List<PostMediaResult> files;

    public static PostResponse from(PostResult result) {
        PostResponse response = new PostResponse();
        response.setId(result.getId());
        response.setIsILikeIt(result.getIsILikeIt());
        response.setProject(ProjectSimpleInfoResponse.from(result.getProject()));
        response.setWriter(UserSimpleInfoResponse.from(result.getWriter()));
        response.setAccessModifier(result.getAccessModifier());
        response.setCommentModifier(result.getCommentModifier());
        response.setContents(result.getContents());
        response.setLatitude(result.getLatitude());
        response.setLongitude(result.getLongitude());
        response.setAddress(result.getAddress());
        response.setLikeCount(result.getLikeCount());
        response.setCommentCount(result.getCommentCount());
        response.setWriteTime(result.getWriteTime());
        response.setWriteTimeStr(result.getWriteTimeStr());
        response.setHashtags(result.getHashtags());
        response.setMedia(result.getMedia());
        response.setFiles(result.getFiles());

        return response;
    }

    @Data
    static class UserSimpleInfoResponse {
        private String id;
        private String name;
        private String profileImgPath;

        static UserSimpleInfoResponse from(PostResult.UserSimpleInfoResult result) {
            UserSimpleInfoResponse userSimpleInfoResult = new UserSimpleInfoResponse();
            userSimpleInfoResult.setId(result.getId());
            userSimpleInfoResult.setName(result.getName());
            userSimpleInfoResult.setProfileImgPath(result.getProfileImgPath());

            return userSimpleInfoResult;
        }
    }

    @Data
    static class ProjectSimpleInfoResponse {
        private Long id;
        private String name;

        static ProjectSimpleInfoResponse from(PostResult.ProjectSimpleInfoResult project) {
            ProjectSimpleInfoResponse projectSimpleInfoResponse = new ProjectSimpleInfoResponse();
            projectSimpleInfoResponse.setId(project.getId());
            projectSimpleInfoResponse.setName(project.getName());

            return projectSimpleInfoResponse;
        }
    }
}