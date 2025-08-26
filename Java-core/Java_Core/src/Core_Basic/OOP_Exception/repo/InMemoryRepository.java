package Core_Basic.OOP_Exception.repo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryRepository<ID, T> implements repository<ID, T> {
    protected final Map<ID, T> store = new HashMap<>();
    @Override public void save(T t){ throw new UnsupportedOperationException("Use specialized impl"); }
    @Override public T findById(ID id){ return store.get(id); }
    @Override public List<T> findAll(){ return new ArrayList<>(store.values()); }
}
