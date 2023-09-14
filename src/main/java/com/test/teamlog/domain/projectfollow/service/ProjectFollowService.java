package com.test.teamlog.domain.projectfollow.service;

import com.test.teamlog.domain.account.model.User;
import com.test.teamlog.domain.account.service.query.AccountQueryService;
import com.test.teamlog.domain.project.entity.Project;
import com.test.teamlog.domain.project.service.query.ProjectCommandService;
import com.test.teamlog.domain.projectfollow.dto.ProjectFollowerReadResult;
import com.test.teamlog.domain.projectfollow.dto.ProjectFollowerReadUserFollowedResult;
import com.test.teamlog.domain.projectfollow.entity.ProjectFollower;
import com.test.teamlog.domain.projectfollow.repository.ProjectFollowerRepository;
import com.test.teamlog.exception.ResourceAlreadyExistsException;
import com.test.teamlog.exception.ResourceNotFoundException;
import com.test.teamlog.payload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProjectFollowService {
    private final AccountQueryService accountQueryService;
    private final ProjectCommandService projectCommandService;
    private final ProjectFollowerRepository projectFollowerRepository;

    // 유저가 팔로우하는 프로젝트 목록 조회
    public List<ProjectFollowerReadUserFollowedResult> readAllByUserIdentification(String userIdentification) {
        User user = accountQueryService.findByIdentification(userIdentification)
                .orElseThrow(() -> new ResourceNotFoundException("User", "ID", userIdentification));
        List<ProjectFollower> projectFollowerList = projectFollowerRepository.findAllByUser(user);

        return projectFollowerList.stream().map(ProjectFollowerReadUserFollowedResult::of).collect(Collectors.toList());
    }

    // 해당 프로젝트를 팔로우하는 사용자 목록 조회
    public List<ProjectFollowerReadResult> readAll(Long projectId) {
        final Project project = projectCommandService.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "ID", projectId));

        List<ProjectFollower> projectFollowerList = projectFollowerRepository.findAllByProject(project);

        return projectFollowerList.stream().map(ProjectFollowerReadResult::of).collect(Collectors.toList());
    }

    // 프로젝트 팔로우
    @Transactional
    public ApiResponse followProject(Long projectId, User currentUser) {
        final Project project = projectCommandService.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "ID", projectId));

        projectFollowerRepository.findByProjectAndUser(project, currentUser)
                .ifPresent(projectFollower -> {
                    throw new ResourceAlreadyExistsException("이미 해당 프로젝트를 팔로우 하고 있습니다.");
                });

        projectFollowerRepository.save(ProjectFollower.create(project, currentUser));

        return new ApiResponse(Boolean.TRUE, "프로젝트 팔로우 성공");
    }

    // 프로젝트 언팔로우
    @Transactional
    public ApiResponse unfollowProject(Long projectId, User currentUser) {
        final Project project = projectCommandService.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "ID", projectId));

        ProjectFollower projectFollower = projectFollowerRepository.findByProjectAndUser(project, currentUser)
                .orElseThrow(() -> new ResourceNotFoundException("ProjectFollwer", "UserId", currentUser.getIdentification()));

        projectFollowerRepository.delete(projectFollower);
        return new ApiResponse(Boolean.TRUE, "프로젝트 언팔로우 성공");
    }
}
