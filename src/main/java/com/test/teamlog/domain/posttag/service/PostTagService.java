package com.test.teamlog.domain.posttag.service;

import com.test.teamlog.domain.posttag.entity.PostTag;
import com.test.teamlog.domain.posttag.repository.PostTagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostTagService {
    private final PostTagRepository postTagRepository;

    public List<PostTag> findAllByPostIdIn(List<Long> postIdList) {
        return postTagRepository.findAllByPostIdIn(postIdList);
    }
}