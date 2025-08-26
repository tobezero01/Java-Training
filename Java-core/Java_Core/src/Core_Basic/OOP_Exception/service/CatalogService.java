package Core_Basic.OOP_Exception.service;

import Core_Basic.OOP_Exception.Domain.product.Book;
import Core_Basic.OOP_Exception.Domain.product.DigitalProduct;
import Core_Basic.OOP_Exception.core.IdGenerator;
import Core_Basic.OOP_Exception.core.Money;
import Core_Basic.OOP_Exception.repo.ProductRepository;

public final class CatalogService {
    private final ProductRepository products;
    public CatalogService(ProductRepository products){ this.products = products; }

    public Book addBook(String name, double price, String author){
        Book b = new Book(IdGenerator.next(), name, Money.of(price), author);
        products.save(b);
        return b;
    }
    public DigitalProduct addDigital(String name, double price){
        DigitalProduct d = new DigitalProduct(IdGenerator.next(), name, Money.of(price));
        products.save(d);
        return d;
    }
}
