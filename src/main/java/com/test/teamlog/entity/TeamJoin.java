package com.test.teamlog.entity;

import lombok.*;

import javax.persistence.*;

@Entity
@Builder
@Setter @Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "team_join",
        uniqueConstraints =@UniqueConstraint(columnNames = {"team_id", "user_id"}))
public class TeamJoin extends BaseTimeEntity {
    @Id @GeneratedValue
    private Long id;

    @ManyToOne
    @JoinColumn(name = "team_id",nullable = false)
    private Team team;

    @ManyToOne
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @Column(name = "is_accepted",nullable = false)
    private boolean isAccepted;

    @Column(name = "is_invited",nullable = false)
    private boolean isInvited;
}
