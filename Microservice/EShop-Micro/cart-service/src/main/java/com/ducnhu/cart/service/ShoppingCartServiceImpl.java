package com.ducnhu.cart.service;

import com.ducnhu.cart.dto.AddResult;
import com.ducnhu.cart.entity.CartItem;
import com.ducnhu.cart.repository.CartItemRepository;
import com.ducnhu.common.events.catalog.ProductSnapshot;
import com.ducnhu.common.events.catalog.ProductSnapshotRequest;
import com.ducnhu.common.events.catalog.ProductSnapshotResponse;
import com.ducnhu.common.kafka.RequestReplyClient;
import com.ducnhu.common.kafka.Topics;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ShoppingCartServiceImpl implements ShoppingCartService {
    private final CartItemRepository repository;
    private final RequestReplyClient requestReplyClient;

    private ProductSnapshot loadProduct(Integer productId) {
        ProductSnapshotResponse response = requestReplyClient.request(
                Topics.CATALOG_PROD_SNAPSHOT_REQ,
                Topics.CATALOG_PROD_SNAPSHOT_RESP, ProductSnapshotResponse.class,
                corr -> new ProductSnapshotRequest(corr, Topics.CATALOG_PROD_SNAPSHOT_RESP, List.of(productId)),
                Duration.ofSeconds(50));

        if (response.products() == null || response.products().isEmpty()) {
            throw new IllegalArgumentException("Product not found: " + productId);
        }

        ProductSnapshot productSnapshot = response.products().get(0);
        if (productSnapshot.inStock() == null || !productSnapshot.inStock()) {
            throw new IllegalArgumentException("Product out of stock: " + productId);
        }
        return productSnapshot;
    }

    @Override
    public AddResult addProduct(Integer customerId, Integer productId, Integer quantity) {
        if (quantity == null || quantity <= 1) {
            quantity = 1;
        }

        CartItem cartItem = repository.findByCustomerIdAndProductId(customerId, productId).orElse(null);
        if (cartItem == null) {
            ProductSnapshot productSnapshot = loadProduct(productId);
            cartItem = CartItem.builder().customerId(customerId).productId(productId).quantity(quantity)
                    .name(productSnapshot.name()).alias(productSnapshot.alias()).image(productSnapshot.mainImagePath())
                    .price(productSnapshot.price()).discountPrice(productSnapshot.discountPrice()).cost(productSnapshot.cost())
                    .length(productSnapshot.length()).width(productSnapshot.width()).height(productSnapshot.height())
                    .weight(productSnapshot.weight())
                    .build();
        } else {
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
            if (cartItem.getQuantity() > 10) cartItem.setQuantity(10);
            ProductSnapshot productSnapshot = loadProduct(productId);
            cartItem.setPrice(productSnapshot.price());
            cartItem.setDiscountPrice(productSnapshot.discountPrice());
            cartItem.setCost(productSnapshot.cost());
            cartItem.setLength(productSnapshot.length());
            cartItem.setWidth(productSnapshot.width());
            cartItem.setHeight(productSnapshot.height());
            cartItem.setWeight(productSnapshot.weight());
            cartItem.setName(productSnapshot.name());
            cartItem.setAlias(productSnapshot.alias());
            cartItem.setImage(productSnapshot.mainImagePath());
        }
        repository.save(cartItem);
        return new AddResult(cartItem.getQuantity(), cartItem.getSubtotal());
    }

    @Override
    public float updateQuantity(Integer customerId, Integer productId, Integer quantity) {
        if (quantity == null || quantity < 1) quantity = 1;
        CartItem item = repository.findByCustomerIdAndProductId(customerId, productId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found"));
        item.setQuantity(Math.min(quantity, 10));
        repository.save(item);
        return item.getSubtotal();
    }

    @Override
    public void remove(Integer customerId, Integer productId) {
        repository.deleteByCustomerIdAndProductId(customerId, productId);
    }

    @Override
    public void clear(Integer customerId) {
        repository.deleteByCustomerId(customerId);
    }

    @Override
    public List<CartItem> list(Integer customerId) {
        return repository.findByCustomerIdOrderByIdAsc(customerId);
    }
}
