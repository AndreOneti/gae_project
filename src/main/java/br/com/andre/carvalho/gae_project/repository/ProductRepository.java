package br.com.andre.carvalho.gae_project.repository;

import br.com.andre.carvalho.gae_project.controller.OrderController;
import br.com.andre.carvalho.gae_project.exception.ProductAlreadyExistException;
import br.com.andre.carvalho.gae_project.exception.ProductNotFoundException;
import br.com.andre.carvalho.gae_project.exception.UserNotFoundException;
import br.com.andre.carvalho.gae_project.model.Product;
import br.com.andre.carvalho.gae_project.model.ProductInterest;
import br.com.andre.carvalho.gae_project.model.User;
import com.google.appengine.api.datastore.*;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Repository
public class ProductRepository {
    private static final Logger log = Logger.getLogger("ProductRepository");

    private static final String PRODUCT_KIND = "Products";
    private static final String PRODUCT_KEY = "productKey";

    private static final String PRODUCT_ID = "ProductID";
    private static final String PROPERTY_NAME = "Name";
    private static final String PROPERTY_CODE = "Code";
    private static final String PROPERTY_MODEL = "Model";
    private static final String PROPERTY_PRICE = "Price";

    private static final String PRODUCT_INTEREST_KIND = "ProductInterest";
    private static final String USER_KIND = "Users";
    private static final String PROPERTY_INTEREST_PRICE = "price";
    private static final String PROPERTY_INTEREST_ID = "productId";
    private static final String PROPERTY_CPF = "cpf";

    public void productToEntity(Product product, Entity productEntity) {
        productEntity.setProperty(PRODUCT_ID, product.getProductID());
        productEntity.setProperty(PROPERTY_NAME, product.getName());
        productEntity.setProperty(PROPERTY_CODE, product.getCode());
        productEntity.setProperty(PROPERTY_MODEL, product.getModel());
        productEntity.setProperty(PROPERTY_PRICE, product.getPrice());
    }

    public Product entityToProduct(Entity productEntity) {
        Product product = new Product();
        product.setId(productEntity.getKey().getId());
        product.setProductID((String) productEntity.getProperty(PRODUCT_ID));
        product.setName((String) productEntity.getProperty(PROPERTY_NAME));
        product.setCode(Integer.parseInt(productEntity.getProperty(PROPERTY_CODE).toString()));
        product.setModel((String) productEntity.getProperty(PROPERTY_MODEL));
        product.setPrice(Double.parseDouble(productEntity.getProperty(PROPERTY_PRICE).toString()));
        return product;
    }

    public Entity getByProductId(String productId) {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query.Filter filterProductId = new Query.FilterPredicate(PRODUCT_ID, Query.FilterOperator.EQUAL, productId);
        Query query = new Query(PRODUCT_KIND).setFilter(filterProductId);
        Entity productEntity = datastore.prepare(query).asSingleEntity();
        return productEntity;
    }

    public List<Product> getProducts() {
        List<Product> products = new ArrayList<>();
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query query = new Query(PRODUCT_KIND).addSort(PRODUCT_ID, Query.SortDirection.ASCENDING);
        List<Entity> productEntities = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
        for (Entity productEntity : productEntities) {
            Product product = entityToProduct(productEntity);
            products.add(product);
        }
        return products;
    }

    public Product saveProduct(Product product) throws ProductAlreadyExistException {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Entity productEntity = getByProductId(product.getProductID());
        if (productEntity == null) {
            Key productKey = KeyFactory.createKey(PRODUCT_KIND, PRODUCT_KEY);
            Entity entityProduct = new Entity(PRODUCT_KIND, productKey);
            productToEntity(product, entityProduct);
            datastore.put(entityProduct);
            product.setId(entityProduct.getKey().getId());
            return product;
        } else {
            throw new ProductAlreadyExistException("Produto " + product.getProductID() + " já cadastrado");
        }
    }

    public Product updateProduct(Product product) throws ProductNotFoundException {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query.Filter emailFilter = new Query.FilterPredicate(PRODUCT_ID, Query.FilterOperator.EQUAL, product.getProductID());
        Query query = new Query(PRODUCT_KIND).setFilter(emailFilter);
        Entity productEntity = datastore.prepare(query).asSingleEntity();
        if (productEntity != null) {
            productToEntity(product, productEntity);
            datastore.put(productEntity);
            product.setId(productEntity.getKey().getId());
            sendMessage(product);
            return product;
        } else {
            throw new ProductNotFoundException("Produto " + product.getProductID() + " não encontrado");
        }
    }

    public Product deleteProduct(String productId) throws ProductNotFoundException {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Entity productEntity = getByProductId(productId);
        if (productEntity != null) {
            datastore.delete(productEntity.getKey());
            return entityToProduct(productEntity);
        } else {
            throw new ProductNotFoundException("Product " + productId + " não encontrado");
        }
    }

    private static List<User> getUsersByPriceAndProductId(String productId, Double price) {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query.Filter priceFilter = new Query.FilterPredicate(PROPERTY_INTEREST_PRICE, Query.FilterOperator.GREATER_THAN_OR_EQUAL, price);
        Query.Filter productIdFilter = new Query.FilterPredicate(PROPERTY_INTEREST_ID, Query.FilterOperator.EQUAL, productId);
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

    public String sendMessage(Product product) {
        Double price = product.getPrice();
        List<User> users;
        users = getUsersByPriceAndProductId(product.getProductID(), price);
        if (users.size() > 0) {
            OrderController orderController = new OrderController();
            String response = orderController.sendProductOffer(users, product.getProductID());
            return response;
        } else {
            return "No messages sended";
        }
    }
}
