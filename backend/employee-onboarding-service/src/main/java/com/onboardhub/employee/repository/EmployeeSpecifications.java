package com.onboardhub.employee.repository;

import com.onboardhub.employee.entity.Employee;
import com.onboardhub.employee.enums.Department;
import com.onboardhub.employee.enums.EmployeeStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class EmployeeSpecifications {
    private EmployeeSpecifications() {
    }

    public static Specification<Employee> matching(EmployeeStatus status, Department department, String search) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) predicates.add(builder.equal(root.get("status"), status));
            if (department != null) predicates.add(builder.equal(root.get("department"), department));
            addSearch(search, root, builder, predicates);
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static void addSearch(String search, jakarta.persistence.criteria.Root<Employee> root,
            jakarta.persistence.criteria.CriteriaBuilder builder, List<Predicate> predicates) {
        if (search == null || search.isBlank()) return;
        String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        predicates.add(builder.or(builder.like(builder.lower(root.get("fullName")), pattern),
                builder.like(builder.lower(root.get("email")), pattern),
                builder.like(builder.lower(root.get("employeeCode")), pattern)));
    }
}
