package com.onboardhub.asset.repository;

import com.onboardhub.asset.entity.Asset;
import com.onboardhub.asset.enums.AssetStatus;
import com.onboardhub.asset.enums.AssetType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

public final class AssetSpecifications {
    private AssetSpecifications() {
    }

    public static Specification<Asset> matching(AssetType type, AssetStatus status, String search) {
        return (root, query, builder) -> {
            List<Predicate> filters = new ArrayList<>();
            if (type != null) filters.add(builder.equal(root.get("type"), type));
            if (status != null) filters.add(builder.equal(root.get("status"), status));
            addSearchFilter(search, root, builder, filters);
            return builder.and(filters.toArray(Predicate[]::new));
        };
    }

    private static void addSearchFilter(String search, jakarta.persistence.criteria.Root<Asset> root,
            jakarta.persistence.criteria.CriteriaBuilder builder, List<Predicate> filters) {
        if (search == null || search.isBlank()) return;
        String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
        filters.add(builder.or(builder.like(builder.lower(root.get("assetTag")), pattern),
                builder.like(builder.lower(root.get("make")), pattern),
                builder.like(builder.lower(root.get("model")), pattern),
                builder.like(builder.lower(root.get("serialNumber")), pattern)));
    }
}
