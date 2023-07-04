package com.test.teamlog.payload;

import com.test.teamlog.domain.account.dto.UserRequest;

import lombok.Builder;
import lombok.Data;

public class ProjectJoinDTO {
    @Data
    @Builder
    public static class ProjectJoinForProject {
        private Long id;
        private String projectName;
        private UserRequest.UserSimpleInfo user;
    }

    @Data
    @Builder
    public static class ProjectJoinForUser {
        private Long id;
        private Long projectId;
        private String thumbnail; // 대표 이미지
        private String projectName;
    }
}
