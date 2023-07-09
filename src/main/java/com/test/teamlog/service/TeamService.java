package com.test.teamlog.service;

import com.test.teamlog.domain.account.model.User;

import com.test.teamlog.domain.account.repository.UserRepository;
import com.test.teamlog.entity.*;
import com.test.teamlog.exception.ResourceForbiddenException;
import com.test.teamlog.exception.ResourceNotFoundException;
import com.test.teamlog.payload.ApiResponse;
import com.test.teamlog.payload.Relation;
import com.test.teamlog.payload.TeamDTO;
import com.test.teamlog.repository.TeamJoinRepository;
import com.test.teamlog.repository.TeamMemberRepository;
import com.test.teamlog.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TeamService {
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamJoinRepository teamJoinRepository;

    // 팀 검색
    public List<TeamDTO.TeamListResponse> searchTeam(String name, User currentUser) {
        List<Team> teamList = teamRepository.searchTeamByName(name);

        List<TeamDTO.TeamListResponse> teams = new ArrayList<>();
        for (Team team : teamList) {
            if (!isUserMemberOfTeam(team, currentUser) && team.getAccessModifier() == AccessModifier.PRIVATE)
                continue;

            long projectCount = team.getProjects().size();

            TeamDTO.TeamListResponse item = TeamDTO.TeamListResponse.builder()
                    .id(team.getId())
                    .name(team.getName())
                    .projectCount(projectCount)
                    .updateTime(team.getUpdateTime())
                    .build();
            teams.add(item);
        }
        return teams;
    }

    // 팀과의 관계
    public Relation getRelation(Team team, User currentUser) {
        if (currentUser == null) return Relation.NONE;
        if (team.getMaster().getIdentification().equals(currentUser.getIdentification())) return Relation.MASTER;
        if (isUserMemberOfTeam(team, currentUser)) return Relation.MEMBER;

        TeamJoin join = teamJoinRepository.findByTeamAndUser(team, currentUser).orElse(null);
        if (join != null) {
            if (join.getIsAccepted() == true && join.getIsInvited() == false) return Relation.APPLIED;
            if (join.getIsAccepted() == false && join.getIsInvited() == true) return Relation.INVITED;
        }
        return Relation.NONE;
    }


    // 팀 조회
    public TeamDTO.TeamResponse getTeam(Long id, User currentUser) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", id));
        // Private 시 검증
        if(team.getAccessModifier() == AccessModifier.PRIVATE) {
            validateUserIsMemberOfTeam(team, currentUser);
        }
        TeamDTO.TeamResponse response = new TeamDTO.TeamResponse(team);
        response.setRelation(getRelation(team, currentUser));
        return response;
    }

    // 사용자 팀 리스트 조회
    public List<TeamDTO.TeamListResponse> getTeamsByUser(String id, User currentUser) {
        User user = null;
        boolean isMyTeamList = false;
        if (currentUser == null) {
            user = userRepository.findByIdentification(id)
                    .orElseThrow(() -> new ResourceNotFoundException("USER", "id", id));
        } else {
            isMyTeamList = currentUser.getIdentification().equals(id);
            if (isMyTeamList)
                user = currentUser;
            else
                user = userRepository.findByIdentification(id)
                        .orElseThrow(() -> new ResourceNotFoundException("USER", "id", id));
        }
        List<Team> teams = teamRepository.getTeamsByUser(user);
        List<Team> sorted =teams.stream().sorted(Comparator.comparing(Team::getUpdateTime).reversed()).collect(Collectors.toList());

        List<TeamDTO.TeamListResponse> teamList = new ArrayList<>();
        for (Team team : sorted) {
            if(!isMyTeamList) {
                // 팀 멤버도 아니고 private면 x
                if(!isUserMemberOfTeam(team,currentUser) && team.getAccessModifier() == AccessModifier.PRIVATE) continue;
            }

            TeamDTO.TeamListResponse item = TeamDTO.TeamListResponse.builder()
                    .id(team.getId())
                    .name(team.getName())
                    .updateTime(team.getUpdateTime())
                    .masterId(team.getMaster().getIdentification())
                    .build();
            teamList.add(item);
        }
        return teamList;
    }

    // 팀 생성
    @Transactional
    public TeamDTO.TeamResponse createTeam(TeamDTO.TeamRequest request, User currentUser) {
        Team team = Team.builder()
                .name(request.getName())
                .introduction(request.getIntroduction())
                .accessModifier(request.getAccessModifier())
                .master(currentUser)
                .build();
        Team newTeam = teamRepository.save(team);

        TeamMember member = TeamMember.builder()
                .user(currentUser)
                .team(team)
                .build();
        teamMemberRepository.save(member);

        return new TeamDTO.TeamResponse(newTeam);
    }

    // 팀 수정 ( 위임 일단 포함 )
    @Transactional
    public TeamDTO.TeamResponse updateTeam(Long id, TeamDTO.TeamRequest request, User currentUser) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", id));
        validateUserIsMaster(team, currentUser);

        team.setName(request.getName());
        team.setIntroduction(request.getIntroduction());
        team.setAccessModifier(request.getAccessModifier());

        if (request.getMasterId() != null) {
            User newMaster = userRepository.findByIdentification(request.getMasterId())
                    .orElseThrow(() -> new ResourceNotFoundException("Project", "id", request.getMasterId()));
            team.setMaster(newMaster);
        }
        Team newTeam = teamRepository.save(team);

        return new TeamDTO.TeamResponse(newTeam);
    }

    // 팀 마스터 위임
    @Transactional
    public ApiResponse delegateTeamMaster(Long id, String newMasterId, User currentUser) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", id));
        validateUserIsMaster(team, currentUser);

        User newMaster = userRepository.findByIdentification(newMasterId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", "id", newMasterId));
        team.setMaster(newMaster);
        teamRepository.save(team);
        return new ApiResponse(Boolean.TRUE, "팀 마스터 위임 성공");
    }

    // 팀 삭제
    @Transactional
    public ApiResponse deleteTeam(Long id, User currentUser) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Team", "id", id));
        validateUserIsMaster(team, currentUser);

        // 팀 내 프로젝트는 팀에서 독립된 프로젝트가 됨
        for(Project project : team.getProjects()) {
            project.setTeam(null);
        }
        teamRepository.delete(team);
        return new ApiResponse(Boolean.TRUE, "팀 삭제 성공");
    }

    // 마스터 검증
    public void validateUserIsMaster(Team team, User currentUser) {
        if (!currentUser.getIdentification().equals(team.getMaster().getIdentification()))
            throw new ResourceForbiddenException("권한이 없습니다.\n(팀 마스터 아님)");
    }

    //
    // 이미 TeamJoin 있을 경우
    public Boolean isJoinAlreadyExist(Team team, User currentUser) {
        return teamJoinRepository.findByTeamAndUser(team, currentUser).isPresent();
    }

    // 팀 멤버인지 아닌지
    public Boolean isUserMemberOfTeam(Team team, User currentUser) {
        return teamMemberRepository.findByTeamAndUser(team, currentUser).isPresent();
    }

    // 팀 멤버 검증
    public void validateUserIsMemberOfTeam(Team team, User currentUser) {
        if (currentUser == null) throw new ResourceForbiddenException("권한이 없습니다.\n로그인 해주세요.");
        teamMemberRepository.findByTeamAndUser(team, currentUser)
                .orElseThrow(() -> new ResourceForbiddenException("권한이 없습니다.\n(팀 멤버 아님)"));
    }

}
