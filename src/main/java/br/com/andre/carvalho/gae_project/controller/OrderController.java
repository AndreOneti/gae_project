package br.com.andre.carvalho.gae_project.controller;

import br.com.andre.carvalho.gae_project.model.Order;
import br.com.andre.carvalho.gae_project.model.User;
import br.com.andre.carvalho.gae_project.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@RestController
@RequestMapping(path = "/api/order")
public class OrderController {
    private static final Logger log = Logger.getLogger("OrderController");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void initialize() {
        try {
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .setDatabaseUrl("https://projectdm111ac.firebaseio.com")
                    .build();
            FirebaseApp.initializeApp(options);
            log.info("FirebaseApp configurado");
        } catch (IOException e) {
            log.info("Falha ao configurar FirebaseApp");
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(path = "/sendOrder")
    public ResponseEntity<String> sendOrder(@RequestBody Order order) {
        Optional<User> optUser = userRepository.getByCpf(order.getCpf());
        if (optUser.isPresent()) {
            User user = optUser.get();
            String registrationToken = user.getFcmRegId();
            try {
                Message message = Message.builder()
                        .putData("product", objectMapper.writeValueAsString(order))
                        .setToken(registrationToken)
                        .build();
                String response = FirebaseMessaging.getInstance().send(message);
                log.info("Mensagem com productId: " + order.getOrderId() + " enviada para o usuario: "
                        + user.getEmail());

                log.info("Reposta do FCM: " + response);
                return new ResponseEntity<String>("Mensagem com productId: " + order.getOrderId()
                        + " enviada para o usuario: "
                        + user.getEmail(), HttpStatus.OK);

            } catch (FirebaseMessagingException | JsonProcessingException e) {
                log.severe("Falha ao enviar mensagem pelo FCM: " + e.getMessage());
                return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
            }
        } else {
            log.severe("Usuário não encontrado");
            return new ResponseEntity<String>("Usuário não encontrado", HttpStatus.NOT_FOUND);
        }
    }

    public String sendProductOffer(List<User> users, String productId) {
        try {
            List<Message> messages = new ArrayList<>();
            for (User user : users) {
                String registrationToken = user.getFcmRegId();
                String sendMsg = "Caiu o preço do produdo " + productId + " para o esperado, venha conferir!";
                Message message = Message.builder()
                        .putData("product", sendMsg)
                        .setToken(registrationToken)
                        .build();
                messages.add(message);
            }
            List<SendResponse> responses = FirebaseMessaging.getInstance().sendAll(messages).getResponses();
            log.info("Reposta do FCM: " + responses);
            List<String> responseString = new ArrayList<>();
            for (SendResponse response : responses) {
                responseString.add(response.toString());
            }
            return responseString.toString();

        } catch (FirebaseMessagingException e) {
            log.severe("Falha ao enviar mensagem pelo FCM: " + e.getMessage());
            return e.getMessage();
        }
    }
}
