package com.test.teamlog.domain.account.repository;

import com.test.teamlog.domain.account.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    @Query("SELECT u FROM User u WHERE u.id LIKE concat('%',:id,'%') AND u.name LIKE concat('%',:name,'%')")
    List<User> searchUserByIdAndName(@Param("id") String id, @Param("name") String name);

    @Query("SELECT u FROM User u WHERE u.id not in (select m.user.id from ProjectMember m where m.project.id = :projectId)" +
            "and u.id not in (select j.user.id from ProjectJoin j where j.project.id = :projectId)")
    List<User> getUsersNotInProjectMember(@Param("projectId") Long projectId);

    @Query("SELECT u FROM User u WHERE u.id not in (select m.user.id from TeamMember m where m.team.id = :teamId)" +
            "and u.id not in (select j.user.id from TeamJoin j where j.team.id = :teamId)")
    List<User> getUsersNotInTeamMember(@Param("teamId") Long teamId);
}
