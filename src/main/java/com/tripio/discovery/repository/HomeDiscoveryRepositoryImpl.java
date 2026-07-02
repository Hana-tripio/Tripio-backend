package com.tripio.discovery.repository;

import com.tripio.discovery.type.HomeDiscoverySection;
import com.tripio.etf.entity.TravelEtf;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class HomeDiscoveryRepositoryImpl implements HomeDiscoveryRepository {

    private static final String PUBLIC_STATUS = "PUBLIC";
    private static final int MAX_SECTION_SIZE = 10;

    private final EntityManager entityManager;

    @Override
    public List<TravelEtf> findTop(HomeDiscoverySection section, int limit) {
        int resultLimit = Math.min(Math.max(limit, 0), MAX_SECTION_SIZE);
        if (resultLimit == 0) {
            return List.of();
        }

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<TravelEtf> query = criteriaBuilder.createQuery(TravelEtf.class);
        Root<TravelEtf> root = query.from(TravelEtf.class);

        query.select(root)
                .where(criteriaBuilder.equal(root.get("status"), PUBLIC_STATUS))
                .orderBy(buildOrders(section, root, criteriaBuilder));

        return entityManager.createQuery(query)
                .setMaxResults(resultLimit)
                .getResultList();
    }

    private List<Order> buildOrders(
            HomeDiscoverySection section,
            Root<TravelEtf> root,
            CriteriaBuilder criteriaBuilder
    ) {
        return switch (section) {
            case POPULAR -> popularOrders(root, criteriaBuilder);
            case RISING -> risingOrders(root, criteriaBuilder);
            case UNDERVALUED -> undervaluedOrders(root, criteriaBuilder);
        };
    }

    private List<Order> popularOrders(Root<TravelEtf> root, CriteriaBuilder criteriaBuilder) {
        return List.of(
                criteriaBuilder.desc(root.get("likeCount")),
                criteriaBuilder.desc(root.get("scrapCount")),
                criteriaBuilder.desc(root.get("followCount")),
                criteriaBuilder.desc(root.get("verificationCount")),
                criteriaBuilder.desc(root.get("ratingAverage")),
                criteriaBuilder.desc(root.get("id"))
        );
    }

    private List<Order> risingOrders(Root<TravelEtf> root, CriteriaBuilder criteriaBuilder) {
        // TODO: 실제 기간별 반응 증가량 테이블 도입 필요.
        // 현재 MVP에서는 최근 생성된 ETF를 우선하고 현재 반응 수를 보조 기준으로 사용한다.
        return List.of(
                criteriaBuilder.desc(root.get("createdAt")),
                criteriaBuilder.desc(root.get("likeCount")),
                criteriaBuilder.desc(root.get("scrapCount")),
                criteriaBuilder.desc(root.get("followCount")),
                criteriaBuilder.desc(root.get("verificationCount")),
                criteriaBuilder.desc(root.get("ratingAverage")),
                criteriaBuilder.desc(root.get("id"))
        );
    }

    private List<Order> undervaluedOrders(Root<TravelEtf> root, CriteriaBuilder criteriaBuilder) {
        Expression<Long> totalReactionCount = totalReactionCount(root, criteriaBuilder);
        return List.of(
                criteriaBuilder.desc(root.get("regionValueScore")),
                criteriaBuilder.asc(totalReactionCount),
                criteriaBuilder.desc(root.get("ratingAverage")),
                criteriaBuilder.desc(root.get("id"))
        );
    }

    private Expression<Long> totalReactionCount(Root<TravelEtf> root, CriteriaBuilder criteriaBuilder) {
        Expression<Long> likesAndScraps = criteriaBuilder.sum(
                root.get("likeCount").as(Long.class),
                root.get("scrapCount").as(Long.class)
        );
        Expression<Long> followsAndVerifications = criteriaBuilder.sum(
                root.get("followCount").as(Long.class),
                root.get("verificationCount").as(Long.class)
        );
        return criteriaBuilder.sum(likesAndScraps, followsAndVerifications);
    }
}
