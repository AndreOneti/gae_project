package br.com.andre.carvalho.gae_project.controller;

import br.com.andre.carvalho.gae_project.model.Product;

import br.com.andre.carvalho.gae_project.model.ProductInterest;
import br.com.andre.carvalho.gae_project.model.User;
import br.com.andre.carvalho.gae_project.repository.ProductInterestRepository;
import br.com.andre.carvalho.gae_project.repository.UserRepository;
import com.google.appengine.api.datastore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = "/api/products")
public class ProductController {

    private static final Logger log = Logger.getLogger("ProductController");

    private static final String PRODUCT_INTEREST_KIND = "ProductInterest";
    private static final String USER_KIND = "Users";
    private static final String PROPERTY_PRICE = "price";
    private static final String PRODUCT_ID = "productId";
    private static final String PROPERTY_CPF = "cpf";

    @Autowired
    private UserRepository userRepository;

    private Product createProduct(int code) {
        Product product = new Product();
        product.setProductID(Integer.toString(code));
        product.setCode(code);
        product.setModel("Model " + code);
        product.setName("Name " + code);
        product.setPrice(10.0 * code);
        return product;
    }

    @GetMapping("/{code}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Product> getProduct(@PathVariable int code) {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query.Filter codeFilter = new Query.FilterPredicate("Code", Query.FilterOperator.EQUAL, code);
        Query query = new Query("Products").setFilter(codeFilter);
        Entity productEntity = datastore.prepare(query).asSingleEntity();
        if (productEntity != null) {
            Product product = entityToProduct(productEntity);
            return new ResponseEntity<Product>(product, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<Product>> getProducts() {
        List<Product> products = new ArrayList<>();
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query query;
        query = new Query("Products").addSort("Code", Query.SortDirection.ASCENDING);
        List<Entity> productsEntities = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
        for (Entity productEntity : productsEntities) {
            Product product = entityToProduct(productEntity);
            products.add(product);
        }
        return new ResponseEntity<List<Product>>(products, HttpStatus.OK);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> saveProduct(@RequestBody Product product) {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Key productKey = KeyFactory.createKey("Products", "productKey");
        Entity productEntity = new Entity("Products", productKey);
        this.productToEntity(product, productEntity);
        datastore.put(productEntity);
        product.setId(productEntity.getKey().getId());
        return new ResponseEntity<Product>(product, HttpStatus.CREATED);
    }

    @DeleteMapping(path = "/{code}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> deleteProduct(@PathVariable("code") int code) {
        //Mensagem 1 - DEBUG
        log.fine("Tentando apagar produto com código=[" + code + "]");

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query.Filter codeFilter = new Query.FilterPredicate("Code", Query.FilterOperator.EQUAL, code);
        Query query = new Query("Products").setFilter(codeFilter);
        Entity productEntity = datastore.prepare(query).asSingleEntity();
        if (productEntity != null) {
            datastore.delete(productEntity.getKey());
            //Mensagem 2 - INFO
            log.info("Produto com código=[" + code + "] " + "apagado com sucesso");
            Product product = entityToProduct(productEntity);
            return new ResponseEntity<Product>(product, HttpStatus.OK);
        } else {
            //Mensagem 3 - ERROR
            log.severe("Erro ao apagar produto com código=[" + code + "]. Produto não encontrado!");
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping(path = "/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Product> updateProduct(@RequestBody Product product, @PathVariable("productId") String productId) {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query.Filter codeFilter = new Query.FilterPredicate("ProductID", Query.FilterOperator.EQUAL, productId);
        Query query = new Query("Products").setFilter(codeFilter);
        Entity productEntity = datastore.prepare(query).asSingleEntity();
        if (productEntity != null) {
            productToEntity(product, productEntity);
            datastore.put(productEntity);
            product.setId(productEntity.getKey().getId());
            Double price = product.getPrice();
            List<User> users;
            users = getUsersByPriceAndProductId(productId, price);
            if (users.size() > 0) {
                OrderController orderController = new OrderController();
                String response = orderController.sendProductOffer(users, productId);
            }
            return new ResponseEntity<Product>(product, HttpStatus.OK);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    private void productToEntity(Product product, Entity productEntity) {
        productEntity.setProperty("ProductID", product.getProductID());
        productEntity.setProperty("Name", product.getName());
        productEntity.setProperty("Code", product.getCode());
        productEntity.setProperty("Model", product.getModel());
        productEntity.setProperty("Price", product.getPrice());
    }

    public static Product entityToProduct(Entity productEntity) {
        Product product = new Product();
        product.setId(productEntity.getKey().getId());
        product.setProductID((String) productEntity.getProperty("ProductID"));
        product.setName((String) productEntity.getProperty("Name"));
        product.setCode(Integer.parseInt(productEntity.getProperty("Code").toString()));
        product.setModel((String) productEntity.getProperty("Model"));
        product.setPrice(Double.parseDouble(productEntity.getProperty("Price").toString()));
        return product;
    }

    private static List<User> getUsersByPriceAndProductId(String productId, Double price) {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query.Filter priceFilter = new Query.FilterPredicate(PROPERTY_PRICE, Query.FilterOperator.GREATER_THAN_OR_EQUAL, price);
        Query.Filter productIdFilter = new Query.FilterPredicate(PRODUCT_ID, Query.FilterOperator.EQUAL, productId);
        Query.Filter filter = Query.CompositeFilterOperator.and(priceFilter, productIdFilter);

        Query query = new Query(PRODUCT_INTEREST_KIND).setFilter(filter);
        List<Entity> PIEntities = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
        List<User> users = new ArrayList<>();
        ProductInterestRepository pIRepository = new ProductInterestRepository();

        UserRepository uR = new UserRepository();

        for (Entity pIEntity : PIEntities) {
            ProductInterest productInterest = pIRepository.entityToProductInterest(pIEntity);
            String userCpf = productInterest.getCpf();
            Query.Filter userFilter = new Query.FilterPredicate(PROPERTY_CPF, Query.FilterOperator.EQUAL, userCpf);
            Query queryUser = new Query(USER_KIND).setFilter(userFilter);
            Entity userEntity = datastore.prepare(queryUser).asSingleEntity();
            User user = uR.entityToUser(userEntity);
            users.add(user);
        }
        return users;
    }
}