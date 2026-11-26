package com.onboardhub.employee.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.onboardhub.employee.entity.Employee;
import com.onboardhub.employee.enums.AssetType;
import com.onboardhub.employee.enums.Department;
import com.onboardhub.employee.enums.OwnerRole;
import com.onboardhub.employee.enums.TaskCategory;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChecklistFactoryTest {
    private final ChecklistFactory factory = new ChecklistFactory(
            Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC));
    private final LocalDate joiningDate = LocalDate.of(2026, 2, 10);

    @Test
    void nullAndEmptyAssetTypesUseLaptopByDefault() {
        assertThat(factory.normalizeAssetTypes(null)).containsExactly(AssetType.LAPTOP);
        assertThat(factory.createTasks(employee(), List.of()).stream().filter(task -> task.getAssetType() != null)
                .map(task -> task.getAssetType())).containsExactly(AssetType.LAPTOP);
    }

    @Test
    void createsSevenTasksWithExpectedOrderOwnersCategoriesAndDates() {
        var tasks = factory.createTasks(employee(), List.of(AssetType.LAPTOP, AssetType.MONITOR));
        assertThat(tasks).hasSize(7);
        assertTask(tasks.get(0), "Collect identity and joining documents", TaskCategory.DOCUMENTATION,
                OwnerRole.HR, 1, joiningDate.minusDays(2));
        assertTask(tasks.get(1), "Sign offer letter and policies", TaskCategory.DOCUMENTATION,
                OwnerRole.HR, 2, joiningDate.minusDays(1));
        assertTask(tasks.get(2), "Create company email account", TaskCategory.ACCOUNT_SETUP,
                OwnerRole.IT, 3, joiningDate.minusDays(1));
        assertTask(tasks.get(3), "Grant system access", TaskCategory.ACCOUNT_SETUP,
                OwnerRole.IT, 4, joiningDate);
        assertTask(tasks.get(4), "Assign LAPTOP", TaskCategory.EQUIPMENT,
                OwnerRole.IT, 5, joiningDate.minusDays(1));
        assertThat(tasks.get(4).getAssetType()).isEqualTo(AssetType.LAPTOP);
        assertTask(tasks.get(5), "Assign MONITOR", TaskCategory.EQUIPMENT,
                OwnerRole.IT, 6, joiningDate.minusDays(1));
        assertThat(tasks.get(5).getAssetType()).isEqualTo(AssetType.MONITOR);
        assertTask(tasks.get(6), "Orientation session", TaskCategory.ORIENTATION,
                OwnerRole.HR, 7, joiningDate.plusDays(1));
    }

    @Test
    void removesDuplicateTypesKeepingFirstOccurrenceOrder() {
        assertThat(factory.normalizeAssetTypes(List.of(AssetType.MONITOR, AssetType.LAPTOP, AssetType.MONITOR)))
                .containsExactly(AssetType.MONITOR, AssetType.LAPTOP);
        assertThat(factory.createTasks(employee(), List.of(AssetType.MONITOR, AssetType.LAPTOP, AssetType.MONITOR)))
                .extracting(task -> task.getAssetType()).filteredOn(type -> type != null)
                .containsExactly(AssetType.MONITOR, AssetType.LAPTOP);
    }

    @Test
    void rejectsMoreThanSixRequestedTypes() {
        List<AssetType> requested = List.of(AssetType.LAPTOP, AssetType.MONITOR, AssetType.KEYBOARD,
                AssetType.MOUSE, AssetType.HEADSET, AssetType.MOBILE, AssetType.LAPTOP);
        assertThatThrownBy(() -> factory.normalizeAssetTypes(requested))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("At most 6 asset types are allowed");
    }

    @Test
    void rejectsNullAssetTypes() {
        assertThatThrownBy(() -> factory.normalizeAssetTypes(java.util.Arrays.asList(AssetType.LAPTOP, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Unknown asset type");
    }

    @Test
    void unknownAssetTypeIsRejectedByEnumParsing() {
        assertThatThrownBy(() -> AssetType.valueOf("PRINTER")).isInstanceOf(IllegalArgumentException.class);
    }

    private void assertTask(com.onboardhub.employee.entity.OnboardingTask task, String title,
            TaskCategory category, OwnerRole owner, int order, LocalDate dueDate) {
        assertThat(task.getTitle()).isEqualTo(title);
        assertThat(task.getCategory()).isEqualTo(category);
        assertThat(task.getOwnerRole()).isEqualTo(owner);
        assertThat(task.getSortOrder()).isEqualTo(order);
        assertThat(task.getDueDate()).isEqualTo(dueDate);
    }

    private Employee employee() {
        return new Employee("Asha Rao", "asha@example.com", Department.ENGINEERING,
                "Backend Engineer", joiningDate, "Ravi HR");
    }
}
