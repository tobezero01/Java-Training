package Core_Basic.OOP_Exception.repo;

import java.util.List;

public interface repository<ID, T> {
    void save(T t);
    T findById(ID id);
    List<T> findAll();
}
