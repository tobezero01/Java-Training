package com.ducnhu.customer.kafka;

import com.ducnhu.common.events.customer.AddressDTO;
import com.ducnhu.common.events.customer.AddressQueryRequest;
import com.ducnhu.common.events.customer.AddressQueryResponse;
import com.ducnhu.common.kafka.Topics;
import com.ducnhu.customer.entity.Address;
import com.ducnhu.customer.service.AddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AddressQueryHandler {

    private final AddressService addrService;
    private final KafkaTemplate<String, Object> kafka;

    @KafkaListener(topics = Topics.CUST_ADDR_REQ, groupId = "customer-service")
    public void onReq(AddressQueryRequest req) {
        Address a = (req.addressId() != null)
                ? addrService.get(req.addressId(), req.customerId())
                : addrService.getDefaultAddress(req.customerId());
        AddressDTO dto = (a == null ? null : new AddressDTO(
                a.getId(), a.getFirstName(), a.getLastName(), a.getPhoneNumber(),
                a.getAddressLine1(), a.getAddressLine2(), a.getCity(), a.getState(), a.getPostalCode(),
                a.getCountryId() == null ? null : a.getCountryId(),
                a.getCountryName(), a.isDefaultForShipping()
        ));
        kafka.send(Topics.CUST_ADDR_RESP, new AddressQueryResponse(req.correlationId(), dto));
    }
}
