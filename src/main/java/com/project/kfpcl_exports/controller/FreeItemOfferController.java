package com.project.kfpcl_exports.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping({"/api/free-item-offers", "/free-item-offers"})
public class FreeItemOfferController {

    @GetMapping({"", "/active"})
    public ResponseEntity<List<Object>> getActiveOffers() {
        return ResponseEntity.ok(Collections.emptyList());
    }
}
