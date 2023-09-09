package com.test.teamlog.domain.task.dto;

import com.test.teamlog.domain.account.model.User;
import com.test.teamlog.domain.task.entity.Task;
import com.test.teamlog.domain.task.entity.TaskPerformer;
import lombok.Data;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class TaskReadByProjectResult {
    private Long id;
    private String taskName;
    private int status;
    private LocalDateTime deadline;
    private LocalDateTime updateTime;
    private String updateTimeStr;
    private List<TaskReadByProjectResult.TaskPerformerResult> performers;

    public static TaskReadByProjectResult from(Task task) {
        TaskReadByProjectResult result = new TaskReadByProjectResult();
        result.setId(task.getId());
        result.setTaskName(task.getTaskName());
        result.setStatus(task.getStatus().getValue());
        result.setDeadline(task.getDeadline());
        result.setUpdateTime(task.getUpdateTime());
        result.setUpdateTimeStr(task.getUpdateTime().toString());
        result.setPerformers(
                !CollectionUtils.isEmpty(task.getTaskPerformers()) ?
                        task.getTaskPerformers().stream().map(TaskReadByProjectResult.TaskPerformerResult::from).toList() :
                        new ArrayList<>()
        );

        return result;
    }

    @Data
    static class TaskPerformerResult {
        private String id;
        private String name;
        private String profileImgPath;

        public static TaskPerformerResult from(TaskPerformer performer) {
            final User user = performer.getUser();

            final TaskPerformerResult result = new TaskPerformerResult();
            result.setId(user.getIdentification());
            result.setName(user.getName());
            result.setProfileImgPath(user.getProfileImgPath());

            return result;
        }
    }
}
