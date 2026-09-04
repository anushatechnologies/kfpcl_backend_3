package com.project.kfpcl_exports.service;

import com.project.kfpcl_exports.dto.AuthDTOs.AddressRequest;
import com.project.kfpcl_exports.dto.AuthDTOs.AddressResponse;
import com.project.kfpcl_exports.model.Address;
import com.project.kfpcl_exports.model.User;
import com.project.kfpcl_exports.repository.AddressRepository;
import com.project.kfpcl_exports.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AddressResponse> getAddressesForUser(Long userId) {
        return addressRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToAddressResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AddressResponse createAddress(Long userId, AddressRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        boolean isDefault = Boolean.TRUE.equals(request.getIsDefault());

        if (isDefault) {
            // Reset previous default addresses to false in transaction
            addressRepository.resetDefaultAddresses(userId);
        }

        Address address = Address.builder()
                .user(user)
                .addressType(request.getAddressType() != null ? request.getAddressType() : "Warehouse")
                .houseNo(request.getHouseNo())
                .streetDetails(request.getStreetDetails())
                .landmark(request.getLandmark())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .isDefault(isDefault)
                .build();

        Address saved = addressRepository.save(address);
        return mapToAddressResponse(saved);
    }

    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        // Enforce tenant isolation: cannot edit another user's address
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found or unauthorized"));

        boolean isDefault = Boolean.TRUE.equals(request.getIsDefault());

        if (isDefault && !Boolean.TRUE.equals(address.getIsDefault())) {
            addressRepository.resetDefaultAddresses(userId);
        }

        if (request.getAddressType() != null) address.setAddressType(request.getAddressType());
        if (request.getHouseNo() != null) address.setHouseNo(request.getHouseNo());
        if (request.getStreetDetails() != null) address.setStreetDetails(request.getStreetDetails());
        if (request.getLandmark() != null) address.setLandmark(request.getLandmark());
        if (request.getCity() != null) address.setCity(request.getCity());
        if (request.getState() != null) address.setState(request.getState());
        if (request.getPincode() != null) address.setPincode(request.getPincode());
        address.setIsDefault(isDefault);

        Address updated = addressRepository.save(address);
        return mapToAddressResponse(updated);
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = addressRepository.findByIdAndUserId(addressId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found or unauthorized"));

        addressRepository.delete(address);
    }

    private AddressResponse mapToAddressResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .userId(address.getUser().getId())
                .addressType(address.getAddressType())
                .houseNo(address.getHouseNo())
                .streetDetails(address.getStreetDetails())
                .landmark(address.getLandmark())
                .city(address.getCity())
                .state(address.getState())
                .pincode(address.getPincode())
                .isDefault(address.getIsDefault())
                .createdAt(address.getCreatedAt())
                .build();
    }
}
