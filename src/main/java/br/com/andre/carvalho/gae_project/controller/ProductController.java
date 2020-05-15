package br.com.andre.carvalho.gae_project.controller;

import br.com.andre.carvalho.gae_project.exception.ProductAlreadyExistException;
import br.com.andre.carvalho.gae_project.exception.ProductNotFoundException;
import br.com.andre.carvalho.gae_project.model.Product;

import br.com.andre.carvalho.gae_project.repository.ProductRepository;
import com.google.appengine.api.datastore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

import java.util.List;

@RestController
@RequestMapping(path = "/api/products")
public class ProductController {
    private static final Logger log = Logger.getLogger("ProductController");

    @Autowired
    private ProductRepository productRepository;

    @GetMapping("/{productId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Product> getProduct(@PathVariable String productId) {
        Entity productEntity = productRepository.getByProductId(productId);
        if (productEntity != null) {
            return new ResponseEntity<Product>(productRepository.entityToProduct(productEntity), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<Product> getProducts() {
        return productRepository.getProducts();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> saveProduct(@RequestBody Product product) {
        try {
            return new ResponseEntity<Product>(productRepository.saveProduct(product), HttpStatus.CREATED);
        } catch (ProductAlreadyExistException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping(path = "/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> deleteProduct(@PathVariable("productId") String productId) {
        try {
            return new ResponseEntity<>(productRepository.deleteProduct(productId), HttpStatus.OK);
        } catch (ProductNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping(path = "/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> updateProduct(@RequestBody Product product, @PathVariable("productId") String productId) {
        try {
            return new ResponseEntity<Product>(productRepository.updateProduct(product), HttpStatus.OK);
        } catch (ProductNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }
}