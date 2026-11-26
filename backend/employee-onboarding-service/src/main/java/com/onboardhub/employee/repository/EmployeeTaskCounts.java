package com.onboardhub.employee.repository;

public interface EmployeeTaskCounts {
    Long getEmployeeId();
    long getTasksDone();
    long getTasksTotal();
}
