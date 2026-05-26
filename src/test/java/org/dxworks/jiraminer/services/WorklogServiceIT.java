package org.dxworks.jiraminer.services;

import org.dxworks.jiraminer.TestUtils;
import org.dxworks.jiraminer.dto.response.issues.worklog.WorkLog;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorklogServiceIT {

    private WorklogService worklogService = new WorklogService(TestUtils.getDeploymentContext());

    @Test
    void listWorkLogsModifiedToday() {
        long todayStartOfDay = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli();
        List<Long> workLogIds = worklogService.getWorkLogIdsModifiedSince(todayStartOfDay);
        List<WorkLog> workLogs = worklogService.listWorkLogsForIds(workLogIds);

        assertNotNull(workLogs);
    }
}
