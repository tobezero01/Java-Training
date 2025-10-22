package com.ducnhu.catalog.kafka;

import com.ducnhu.catalog.entity.product.Product;
import com.ducnhu.catalog.repository.ProductRepository;
import com.ducnhu.common.events.catalog.ProductSnapshot;
import com.ducnhu.common.events.catalog.ProductSnapshotRequest;
import com.ducnhu.common.events.catalog.ProductSnapshotResponse;
import com.ducnhu.common.kafka.Topics;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductSnapshotHandler {

    private final ProductRepository repo;
    private final KafkaTemplate<String, Object> kafka;

    @KafkaListener(topics = Topics.CATALOG_PROD_SNAPSHOT_REQ, groupId = "catalog-service")
    public void onReq(ProductSnapshotRequest req) {
        List<Product> products = repo.findAllById(req.productIds());
        Map<Integer, Product> map = products.stream().collect(Collectors.toMap(Product::getId, x -> x));
        List<ProductSnapshot> out = new ArrayList<>();
        for (Integer id : req.productIds()) {
            Product p = map.get(id);
            if (p != null) {
                out.add(new ProductSnapshot(
                        p.getId(), p.getName(), p.getAlias(), p.getMainImagePath(),
                        p.getPrice(), p.getDiscountPrice(), p.getCost(),
                        p.getLength(), p.getWidth(), p.getHeight(), p.getWeight(),
                        p.isInStock()
                ));
            }
        }
        ProductSnapshotResponse resp = new ProductSnapshotResponse(req.correlationId(), out);
        kafka.send(Topics.CATALOG_PROD_SNAPSHOT_RESP, resp);
    }
}
