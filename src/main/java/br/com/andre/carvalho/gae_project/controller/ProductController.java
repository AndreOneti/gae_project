package br.com.andre.carvalho.gae_project.controller;

import br.com.andre.carvalho.gae_project.model.Product;

import com.google.appengine.api.datastore.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = "/api/products")
public class ProductController {

    private static final Logger log = Logger.getLogger("ProductController");

    private Product createProduct(int code) {
        Product product = new Product();
        product.setProductID(Integer.toString(code));
        product.setCode(code);
        product.setModel("Model " + code);
        product.setName("Name " + code);
        product.setPrice(10 * code);
        return product;
    }

    @GetMapping("/{code}")
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

    @PutMapping(path = "/{code}")
    public ResponseEntity<Product> updateProduct(@RequestBody Product product, @PathVariable("code") int code) {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query.Filter codeFilter = new Query.FilterPredicate("Code", Query.FilterOperator.EQUAL, code);
        Query query = new Query("Products").setFilter(codeFilter);
        Entity productEntity = datastore.prepare(query).asSingleEntity();
        if (productEntity != null) {
            productToEntity(product, productEntity);
            datastore.put(productEntity);
            product.setId(productEntity.getKey().getId());
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

    private Product entityToProduct(Entity productEntity) {
        Product product = new Product();
        product.setId(productEntity.getKey().getId());
        product.setProductID((String) productEntity.getProperty("ProductID"));
        product.setName((String) productEntity.getProperty("Name"));
        product.setCode(Integer.parseInt(productEntity.getProperty("Code").toString()));
        product.setModel((String) productEntity.getProperty("Model"));
        product.setPrice(Float.parseFloat(productEntity.getProperty("Price").toString()));
        return product;
    }
}