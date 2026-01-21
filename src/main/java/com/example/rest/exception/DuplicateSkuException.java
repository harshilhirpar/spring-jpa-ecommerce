package com.example.rest.exception;

public class DuplicateSkuException extends RuntimeException{
//    public DuplicateSkuException(String message) {
//        super(message);
//    }

    public DuplicateSkuException(String sku) {
        super("Product with SKU '" + sku + "' already exists");
    }
}
