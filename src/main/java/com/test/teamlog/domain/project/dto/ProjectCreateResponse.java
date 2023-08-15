package com.test.teamlog.domain.project.dto;

import com.test.teamlog.entity.AccessModifier;
import com.test.teamlog.payload.Relation;
import com.test.teamlog.payload.TeamDTO;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectCreateResponse {
    private Relation relation;
    private Long id;
    private TeamDTO.TeamSimpleInfo team;
    private String name;
    private String introduction;
    private String thumbnail;
    private AccessModifier accessModifier;
    private LocalDateTime createTime;
    private String masterId;
    private int memberCount;
    private int followerCount;

    public static ProjectCreateResponse from(ProjectCreateResult result) {
        ProjectCreateResponse response = new ProjectCreateResponse();
        response.setRelation(result.getRelation());
        response.setId(result.getId());
        response.setTeam(result.getTeam());
        response.setName(result.getName());
        response.setIntroduction(result.getIntroduction());
        response.setThumbnail(result.getThumbnail());
        response.setAccessModifier(result.getAccessModifier());
        response.setCreateTime(result.getCreateTime());
        response.setMasterId(result.getMasterId());
        response.setMemberCount(result.getMemberCount());
        response.setFollowerCount(result.getFollowerCount());

        return response;
    }
}
