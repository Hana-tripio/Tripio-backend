package com.tripio.etf.repository;

import com.tripio.etf.entity.TravelEtf;
import com.tripio.etf.entity.TravelEtfStyleTag;
import com.tripio.etf.type.EtfSearchSort;
import com.tripio.global.apiPayload.code.GeneralErrorCode;
import com.tripio.global.apiPayload.exception.GeneralException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@RequiredArgsConstructor
public class TravelEtfSearchRepositoryImpl implements TravelEtfSearchRepository {

    private static final String PUBLIC_STATUS = "PUBLIC";
    private static final char LIKE_ESCAPE_CHARACTER = '\\';

    private final EntityManager entityManager;

    @Override
    public Page<TravelEtf> search(EtfSearchCriteria criteria, Pageable pageable) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<TravelEtf> contentQuery = criteriaBuilder.createQuery(TravelEtf.class);
        Root<TravelEtf> contentRoot = contentQuery.from(TravelEtf.class);
        contentQuery.select(contentRoot)
                .where(buildPredicates(criteria, contentQuery, contentRoot, criteriaBuilder))
                .orderBy(buildOrders(criteria.sort(), contentRoot, criteriaBuilder));

        TypedQuery<TravelEtf> typedContentQuery = entityManager.createQuery(contentQuery);
        typedContentQuery.setFirstResult(toFirstResult(pageable));
        typedContentQuery.setMaxResults(pageable.getPageSize());
        List<TravelEtf> content = typedContentQuery.getResultList();

        CriteriaQuery<Long> countQuery = criteriaBuilder.createQuery(Long.class);
        Root<TravelEtf> countRoot = countQuery.from(TravelEtf.class);
        countQuery.select(criteriaBuilder.count(countRoot))
                .where(buildPredicates(criteria, countQuery, countRoot, criteriaBuilder));
        long totalElements = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(content, pageable, totalElements);
    }

    private Predicate[] buildPredicates(
            EtfSearchCriteria criteria,
            CriteriaQuery<?> query,
            Root<TravelEtf> root,
            CriteriaBuilder criteriaBuilder
    ) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(criteriaBuilder.equal(root.get("status"), PUBLIC_STATUS));

        if (criteria.keyword() != null && !criteria.keyword().isBlank()) {
            String escapedKeyword = escapeLikePattern(criteria.keyword().trim().toLowerCase(Locale.ROOT));
            String keywordPattern = "%" + escapedKeyword + "%";
            predicates.add(criteriaBuilder.or(
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("title")),
                            keywordPattern,
                            LIKE_ESCAPE_CHARACTER
                    ),
                    criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("summary")),
                            keywordPattern,
                            LIKE_ESCAPE_CHARACTER
                    )
            ));
        }
        if (criteria.regionId() != null) {
            predicates.add(criteriaBuilder.equal(root.get("regionId"), criteria.regionId()));
        }
        if (criteria.minBudget() != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("totalBudget"), criteria.minBudget()));
        }
        if (criteria.maxBudget() != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("totalBudget"), criteria.maxBudget()));
        }
        if (criteria.minDurationDays() != null) {
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("durationDays"), criteria.minDurationDays()));
        }
        if (criteria.maxDurationDays() != null) {
            predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("durationDays"), criteria.maxDurationDays()));
        }
        if (criteria.styleTagIds() != null && !criteria.styleTagIds().isEmpty()) {
            Subquery<Integer> tagExistsQuery = query.subquery(Integer.class);
            Root<TravelEtfStyleTag> mapping = tagExistsQuery.from(TravelEtfStyleTag.class);
            tagExistsQuery.select(criteriaBuilder.literal(1))
                    .where(
                            criteriaBuilder.equal(mapping.get("travelEtfId"), root.get("id")),
                            mapping.get("styleTagId").in(criteria.styleTagIds())
                    );
            predicates.add(criteriaBuilder.exists(tagExistsQuery));
        }

        return predicates.toArray(Predicate[]::new);
    }

    private List<Order> buildOrders(
            EtfSearchSort sort,
            Root<TravelEtf> root,
            CriteriaBuilder criteriaBuilder
    ) {
        if (sort == EtfSearchSort.LATEST) {
            return List.of(
                    criteriaBuilder.desc(root.get("createdAt")),
                    criteriaBuilder.desc(root.get("id"))
            );
        }

        return List.of(
                criteriaBuilder.desc(root.get("likeCount")),
                criteriaBuilder.desc(root.get("scrapCount")),
                criteriaBuilder.desc(root.get("followCount")),
                criteriaBuilder.desc(root.get("verificationCount")),
                criteriaBuilder.desc(root.get("ratingAverage")),
                criteriaBuilder.desc(root.get("id"))
        );
    }

    private String escapeLikePattern(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }

    private int toFirstResult(Pageable pageable) {
        long offset = pageable.getOffset();
        if (offset > Integer.MAX_VALUE) {
            throw new GeneralException(GeneralErrorCode.BAD_REQUEST);
        }
        return (int) offset;
    }
}
