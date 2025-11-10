package com.ducnhu.customer.controller;

import com.ducnhu.customer.client.AuthClient;
import com.ducnhu.customer.dto.AddressDTO;
import com.ducnhu.customer.dto.CreateAddressRequest;
import com.ducnhu.customer.dto.MeResponse;
import com.ducnhu.customer.dto.UpdateAddressRequest;
import com.ducnhu.customer.entity.Address;
import com.ducnhu.customer.mapper.AddressMapper;
import com.ducnhu.customer.service.AddressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
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
        MeResponse me = auth.me();
        if (me == null || me.id() == null)
            throw new ResponseStatusException(UNAUTHORIZED, "Missing/invalid token");
        return me.id();
    }
    @GetMapping
    public ResponseEntity<List<AddressDTO>> list() {
        return ResponseEntity.ok(addressService.listAddressBook(meId()));
    }

    @GetMapping("/default")
    public ResponseEntity<AddressDTO> getDefault() {
        Address addr = addressService.getDefaultAddress(meId());
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
    public ResponseEntity<AddressDTO> update(@PathVariable("id") Integer id, @RequestBody UpdateAddressRequest req) {
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
    public ResponseEntity<Void> delete(@PathVariable("id") Integer id) {
        addressService.delete(id, meId());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<Void> setDefault(@PathVariable("id") Integer id) {
        addressService.setDefaultAddress(id, meId());
        return ResponseEntity.ok().build();
    }
}
