package br.com.andre.carvalho.gae_project.controller;

import br.com.andre.carvalho.gae_project.exception.ProductNotFoundException;
import br.com.andre.carvalho.gae_project.exception.UserNotFoundException;
import br.com.andre.carvalho.gae_project.model.ProductInterest;
import br.com.andre.carvalho.gae_project.repository.ProductInterestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping(path = "/api/interest")
public class ProductInterestController {
    private static final Logger log = Logger.getLogger("ProductInterestController");

    @Autowired
    private ProductInterestRepository productInterestRepository;

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ProductInterest> saveProductInterest(@RequestBody ProductInterest productInterest) {
        try {
            return new ResponseEntity<ProductInterest>(productInterestRepository.saveProductInterest(productInterest), HttpStatus.OK);
        } catch (UserNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (ProductNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @GetMapping
    public List<ProductInterest> getUsers() {
        return productInterestRepository.getProductInterest();
    }
}
