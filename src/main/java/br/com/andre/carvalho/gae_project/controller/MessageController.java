package br.com.andre.carvalho.gae_project.controller;

import br.com.andre.carvalho.gae_project.model.Product;
import br.com.andre.carvalho.gae_project.model.User;
import br.com.andre.carvalho.gae_project.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
//import com.google.firebase.messaging.FcmOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Logger;

@RestController
@RequestMapping(path = "/api/message")
public class MessageController {
    private static final Logger log = Logger.getLogger("MessageController");

//    @Autowired
//    private UserRepository userRepository;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @PostConstruct
//    public void initialize() {
//        try {
//            FirebaseOptions options = new FirebaseOptions.Builder()
//                    .setCredentials(GoogleCredentials.getApplicationDefault())
//                    .setDatabaseUrl("https://projectdm111ac.firebaseio.com")
//                    .build();
//            FirebaseApp.initializeApp(options);
//            log.info("FirebaseApp configurado");
//        } catch (IOException e) {
//            log.info("Falha ao configurar FirebaseApp");
//        }
//    }
//
//    @PreAuthorize("hasRole('ADMIN')")
//    @PostMapping(path = "/sendproduct")
//    public ResponseEntity<String> sendProduct(
//            @RequestParam("email") String email,
//            @RequestParam("productCode") int productCode) {
//        Optional<User> optUser = userRepository.getByEmail(email);
//        if (optUser.isPresent()) {
//            User user = optUser.get();
//            Product product = findProduct(productCode);
//            if (product != null) {
//                String registrationToken = user.getFcmRegId();
//                try {
//                    Message message = Message.builder()
//                            .putData("product", objectMapper.writeValueAsString(product))
//                            .setToken(registrationToken)
//                            .build();
//                    String response = FirebaseMessaging.getInstance().send(message);
//                    log.info("Mensagem enviada ao produto " + product.getName());
//                    log.info("Reposta do FCM: " + response);
//                    return new ResponseEntity<String>("Mensagem enviada com o produto" + product.getName(), HttpStatus.OK);
//                } catch (FirebaseMessagingException | JsonProcessingException e) {
//                    log.severe("Falha ao enviar mensagem pelo FCM: " + e.getMessage());
//                    return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
//                }
//            } else {
//                log.severe("Produto não encontrado");
//                return new ResponseEntity<String>("Produto não encontrado", HttpStatus.NOT_FOUND);
//            }
//        } else {
//            log.severe("Usuário não encontrado");
//            return new ResponseEntity<String>("Usuário não encontrado", HttpStatus.NOT_FOUND);
//        }
//    }
//
//    private Product findProduct(int code) {
//        DatastoreService datastore = DatastoreServiceFactory
//                .getDatastoreService();
//        Query.Filter codeFilter = new Query.FilterPredicate("Code", Query.FilterOperator.EQUAL, code);
//
//        Query query = new Query("Products").setFilter(codeFilter);
//        Entity productEntity = datastore.prepare(query).asSingleEntity();
//
//        if (productEntity != null) {
//            return ProductController.entityToProduct(productEntity);
//        } else {
//            return null;
//        }
//    }
}
