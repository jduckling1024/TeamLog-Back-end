package com.test.teamlog.domain.posttag.repository;

import com.test.teamlog.domain.post.entity.Post;
import com.test.teamlog.domain.posttag.entity.PostTag;
import com.test.teamlog.domain.project.entity.Project;
import com.test.teamlog.payload.PostTagInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostTagRepository extends JpaRepository<PostTag, Long> {
    @Query("SELECT h.name FROM PostTag h WHERE h.post.project = :project GROUP BY h.name")
    public List<String> getHashTagsInProjectPosts(@Param("project") Project project);
    public List<PostTag> findAllByPost(Post post);

    List<PostTag> findAllByPostIdIn(List<Long> postIdList);

    @Query(value = "SELECT h.name as name, count(h.name) as cnt FROM post_tag h where h.post_id in (select p.id from post p where p.project_id=:project ) group by h.name " +
            "order by cnt desc limit 5"
            , nativeQuery = true)
    public List<PostTagInfo> getRecommendedHashTags(@Param("project") Long project_id);
}