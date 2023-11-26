package com.test.teamlog.domain.project.dto;

import com.test.teamlog.domain.file.info.entity.FileInfo;
import com.test.teamlog.domain.project.entity.Project;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectReadUserFollowingResult {
    private Long id;
    private String masterId;
    private String name;
    private long postCount;
    private LocalDateTime updateTime; // 마지막 활동 시간
    private String updateTimeStr;
    private String thumbnail; // 대표 이미지

    public static ProjectReadUserFollowingResult from(Project project) {
        ProjectReadUserFollowingResult response = new ProjectReadUserFollowingResult();
        response.setId(project.getId());
        response.setName(project.getName());
        response.setPostCount(project.getPosts().size());
        response.setUpdateTime(project.getUpdateTime());

        final FileInfo thumbnail = project.getThumbnail();
        if (thumbnail != null) response.setThumbnail(thumbnail.getStoredFilePath());

        return response;
    }
}
