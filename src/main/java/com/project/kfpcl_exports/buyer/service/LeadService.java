package com.project.kfpcl_exports.buyer.service;

import com.project.kfpcl_exports.buyer.dto.ContactSellerRequest;
import com.project.kfpcl_exports.buyer.model.ContactLead;
import com.project.kfpcl_exports.buyer.model.Product;
import com.project.kfpcl_exports.buyer.model.Supplier;
import com.project.kfpcl_exports.buyer.repository.ContactLeadRepository;
import com.project.kfpcl_exports.buyer.repository.ProductRepository;
import com.project.kfpcl_exports.buyer.repository.SupplierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LeadService {

    @Autowired
    private ContactLeadRepository contactLeadRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    public ContactLead recordLead(ContactSellerRequest request) {
        Optional<Product> productOpt = productRepository.findById(request.getProductId());
        Optional<Supplier> supplierOpt = supplierRepository.findById(request.getSupplierId());

        if (productOpt.isEmpty()) {
            throw new IllegalArgumentException("Product not found with id: " + request.getProductId());
        }
        if (supplierOpt.isEmpty()) {
            throw new IllegalArgumentException("Supplier not found with id: " + request.getSupplierId());
        }

        Long buyerId = request.getBuyerId() != null ? request.getBuyerId() : 1L; // Fallback without JWT

        ContactLead lead = new ContactLead(
                buyerId,
                productOpt.get(),
                supplierOpt.get(),
                request.getContactType() != null ? request.getContactType().toUpperCase() : "CALL"
        );

        return contactLeadRepository.save(lead);
    }
}
