package com.test.teamlog.payload;

import com.test.teamlog.entity.*;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PostDTO {
    @Getter
    public static class PostRequest {
        private String contents;
        private AccessModifier accessModifier;
        private AccessModifier commentModifier;
        private double latitude;
        private double longitude;
        private String writerId;
        private Long projectId;
        private List<String> hashtags;
    }

    @Data
    @Builder
    public static class PostResponse {
        private Long id;
        private String contents;
        private double latitude;
        private double longitude;
        private int likeCount;
        private int commentCount;
        private LocalDateTime writeTime;
        private List<String> hashtags;
        private List<FileDTO.FileInfo> media = new ArrayList<FileDTO.FileInfo>();
        private List<FileDTO.FileInfo> files = new ArrayList<FileDTO.FileInfo>();
    }
}
