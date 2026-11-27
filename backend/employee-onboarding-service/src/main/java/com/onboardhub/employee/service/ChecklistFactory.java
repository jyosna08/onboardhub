package com.onboardhub.employee.service;

import com.onboardhub.employee.entity.Employee;
import com.onboardhub.employee.entity.OnboardingTask;
import com.onboardhub.employee.enums.AssetType;
import com.onboardhub.employee.enums.OwnerRole;
import com.onboardhub.employee.enums.TaskCategory;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class ChecklistFactory {
    private static final int MAX_ASSET_TYPES = 6;
    private final Clock clock;

    public ChecklistFactory(Clock clock) {
        this.clock = clock;
    }

    public List<AssetType> normalizeAssetTypes(List<AssetType> requested) {
        if (requested == null || requested.isEmpty()) return List.of(AssetType.LAPTOP);
        if (requested.size() > MAX_ASSET_TYPES) throw new IllegalArgumentException("At most 6 asset types are allowed");
        if (requested.stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("Unknown asset type");
        List<AssetType> distinct = new ArrayList<>(new LinkedHashSet<>(requested));
        if (distinct.size() > MAX_ASSET_TYPES) throw new IllegalArgumentException("At most 6 asset types are allowed");
        return List.copyOf(distinct);
    }

    public List<OnboardingTask> createTasks(Employee employee, List<AssetType> requestedTypes) {
        List<AssetType> types = normalizeAssetTypes(requestedTypes);
        LocalDate joiningDate = employee.getJoiningDate();
        LocalDateTime now = LocalDateTime.now(clock);
        List<OnboardingTask> tasks = new ArrayList<>();
        add(tasks, employee, "Collect identity and joining documents", TaskCategory.DOCUMENTATION,
                OwnerRole.HR, joiningDate.minusDays(2), null, now);
        add(tasks, employee, "Sign offer letter and policies", TaskCategory.DOCUMENTATION,
                OwnerRole.HR, joiningDate.minusDays(1), null, now);
        add(tasks, employee, "Create company email account", TaskCategory.ACCOUNT_SETUP,
                OwnerRole.IT, joiningDate.minusDays(1), null, now);
        add(tasks, employee, "Grant system access", TaskCategory.ACCOUNT_SETUP,
                OwnerRole.IT, joiningDate, null, now);
        addEquipmentTasks(tasks, employee, types, joiningDate, now);
        add(tasks, employee, "Orientation session", TaskCategory.ORIENTATION,
                OwnerRole.HR, joiningDate.plusDays(1), null, now);
        return List.copyOf(tasks);
    }

    private void addEquipmentTasks(List<OnboardingTask> tasks, Employee employee, List<AssetType> types,
            LocalDate joiningDate, LocalDateTime now) {
        for (AssetType type : types) {
            add(tasks, employee, "Assign " + type.name(), TaskCategory.EQUIPMENT, OwnerRole.IT,
                    joiningDate.minusDays(1), type, now);
        }
    }

    private void add(List<OnboardingTask> tasks, Employee employee, String title, TaskCategory category,
            OwnerRole owner, LocalDate dueDate, AssetType type, LocalDateTime now) {
        tasks.add(new OnboardingTask(employee, title, category, owner, dueDate,
                tasks.size() + 1, type, now));
    }
}
