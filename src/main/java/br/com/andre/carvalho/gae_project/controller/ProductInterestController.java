package br.com.andre.carvalho.gae_project.controller;

import br.com.andre.carvalho.gae_project.exception.ProductInterestNotFoundException;
import br.com.andre.carvalho.gae_project.exception.UserNotFoundException;
import br.com.andre.carvalho.gae_project.model.ProductInterest;
import br.com.andre.carvalho.gae_project.model.User;
import br.com.andre.carvalho.gae_project.repository.ProductInterestRepository;
import br.com.andre.carvalho.gae_project.repository.UserRepository;
import br.com.andre.carvalho.gae_project.util.CheckRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@RestController
@RequestMapping(path = "/api/interest")
public class ProductInterestController {
    private static final Logger log = Logger.getLogger("ProductInterestController");

    @Autowired
    private ProductInterestRepository productInterestRepository;

    @Autowired
    private UserRepository userRepository;

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<ProductInterest> saveProductInterest(@RequestBody ProductInterest productInterest, Authentication authentication) {
        boolean hasRoleAdmin = CheckRole.hasRoleAdmin(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Optional<User> optUser = userRepository.getByCpf(productInterest.getCpf());
        if (optUser.isPresent()) {
            if (hasRoleAdmin || userDetails.getUsername().equals(optUser.get().getEmail())) {
                try {
                    return new ResponseEntity<ProductInterest>(productInterestRepository.saveProductInterest(productInterest),
                            HttpStatus.OK);
                } catch (UserNotFoundException e) {
                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
                }
            } else {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @GetMapping("/bycpf")
    public ResponseEntity<List<ProductInterest>> getProductInterest(@RequestParam String cpf, Authentication authentication) {
        boolean hasRoleAdmin = CheckRole.hasRoleAdmin(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Optional<User> optUser = userRepository.getByCpf(cpf);
        if (optUser.isPresent()) {
            if (hasRoleAdmin || userDetails.getUsername().equals(optUser.get().getEmail())) {
                return new ResponseEntity<List<ProductInterest>>(productInterestRepository.getProductInterest(cpf), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @DeleteMapping(path = "/bycpf")
    public ResponseEntity<ProductInterest> deleteUser(@RequestParam String cpf, @RequestParam String productId, Authentication authentication) throws ProductInterestNotFoundException {
        try {
            boolean hasRoleAdmin = CheckRole.hasRoleAdmin(authentication);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            Optional<User> optUser = userRepository.getByCpf(cpf);
            if (optUser.isPresent()) {
                if (hasRoleAdmin || userDetails.getUsername().equals(optUser.get().getEmail())) {
                    return new ResponseEntity<ProductInterest>(productInterestRepository.deleteProductInterest(cpf, productId), HttpStatus.OK);
                } else {
                    return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
                }
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (ProductInterestNotFoundException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}
