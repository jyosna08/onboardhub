package com.onboardhub.employee.repository;

import com.onboardhub.employee.entity.OnboardingTask;
import com.onboardhub.employee.enums.TaskStatus;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OnboardingTaskRepository extends JpaRepository<OnboardingTask, Long> {
    List<OnboardingTask> findByEmployee_IdOrderBySortOrderAsc(Long employeeId);
    Optional<OnboardingTask> findByIdAndEmployee_Id(Long taskId, Long employeeId);

    @Query("select t.employee.id as employeeId, "
            + "sum(case when t.status = com.onboardhub.employee.enums.TaskStatus.DONE then 1 else 0 end) as tasksDone, "
            + "count(t) as tasksTotal from OnboardingTask t where t.employee.id in :employeeIds "
            + "group by t.employee.id")
    List<EmployeeTaskCounts> countTasksByEmployeeIds(@Param("employeeIds") Collection<Long> employeeIds);

    long countByStatusNot(TaskStatus status);

    @Query("select count(t) from OnboardingTask t where t.status <> com.onboardhub.employee.enums.TaskStatus.DONE "
            + "and t.dueDate < :today")
    long countOverdueOpenTasks(@Param("today") LocalDate today);
}
