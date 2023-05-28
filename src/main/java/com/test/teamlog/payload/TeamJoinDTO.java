package com.test.teamlog.payload;

import com.test.teamlog.domain.account.dto.UserDTO;

import lombok.Builder;
import lombok.Data;

public class TeamJoinDTO {
    @Data
    @Builder
    public static class TeamJoinForTeam {
        private Long id;
        private String teamName;
        private UserDTO.UserSimpleInfo user;
    }

    @Data
    @Builder
    public static class TeamJoinForUser {
        private Long id;
        private Long teamId;
        private String teamName;
    }
}
