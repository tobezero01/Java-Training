package Core_Basic.OOP_Exception.repo;

import Core_Basic.OOP_Exception.Domain.product.Product;

import java.util.Comparator;
import java.util.List;

public final class ProductRepository extends InMemoryRepository<String, Product> {
    public void save(Product p){ store.put(p.id(), p); }
    public List<Product> findSortedByPriceAsc(){
        return findAll().stream().sorted(Comparator.comparing(Product::price)).toList();
    }
}
