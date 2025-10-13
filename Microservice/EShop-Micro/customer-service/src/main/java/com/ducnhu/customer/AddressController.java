package com.ducnhu.customer;

import com.ducnhu.customer.client.AuthClient;
import com.ducnhu.customer.dto.AddressDTO;
import com.ducnhu.customer.dto.CreateAddressRequest;
import com.ducnhu.customer.dto.UpdateAddressRequest;
import com.ducnhu.customer.entity.Address;
import com.ducnhu.customer.mapper.AddressMapper;
import com.ducnhu.customer.service.AddressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {
    private final AddressService addressService;
    private final AuthClient auth;

    public AddressController(AddressService s, AuthClient a) {
        this.addressService = s;
        this.auth = a;
    }

    private Integer meId() {
        return auth.me().id();
    } // [STEP] lấy id hiện tại từ auth-service (/api/auth/me) :contentReference[oaicite:35]{index=35}

    @GetMapping
    public ResponseEntity<List<AddressDTO>> list() {
        return ResponseEntity.ok(addressService.listAddressBook(meId()));
    }

    @GetMapping("/default")
    public ResponseEntity<AddressDTO> getDefault() {
        var addr = addressService.getDefaultAddress(meId());
        return (addr == null) ? ResponseEntity.noContent().build() : ResponseEntity.ok(AddressMapper.toDto(addr));
    }

    @PostMapping
    public ResponseEntity<AddressDTO> create(@RequestBody CreateAddressRequest req) {
        Address a = AddressMapper.fromCreate(req);
        a.setCustomerId(meId());
        addressService.save(a);
        if (Boolean.TRUE.equals(req.defaultForShipping())) addressService.setDefaultAddress(a.getId(), meId());
        return ResponseEntity.created(URI.create("/api/addresses/" + a.getId())).body(AddressMapper.toDto(a));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressDTO> update(@PathVariable Integer id, @RequestBody UpdateAddressRequest req) {
        Integer me = meId();
        Address existing = addressService.get(id, me);
        if (existing == null) return ResponseEntity.notFound().build();
        AddressMapper.applyUpdate(existing, req);
        existing.setCustomerId(me);
        addressService.save(existing);
        if (Boolean.TRUE.equals(req.defaultForShipping())) addressService.setDefaultAddress(existing.getId(), me);
        return ResponseEntity.ok(AddressMapper.toDto(existing));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        addressService.delete(id, meId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<Void> setDefault(@PathVariable Integer id) {
        addressService.setDefaultAddress(id, meId());
        return ResponseEntity.ok().build();
    }
}
