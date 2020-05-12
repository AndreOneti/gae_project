package br.com.andre.carvalho.gae_project.repository;

import br.com.andre.carvalho.gae_project.exception.ProductNotFoundException;
import br.com.andre.carvalho.gae_project.exception.UserAlreadyExistsException;
import br.com.andre.carvalho.gae_project.exception.UserNotFoundException;
import br.com.andre.carvalho.gae_project.model.ProductInterest;
import br.com.andre.carvalho.gae_project.model.User;
import com.google.appengine.api.datastore.*;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

@Repository
public class ProductInterestRepository {
    private static final Logger log = Logger.getLogger("ProductInterestRepository");

    private static final String INTEREST_KIND = "ProductInterest";
    private static final String INTEREST_KEY = "interestKey";
    private static final String PRODUCT_KIND = "Products";
    private static final String USER_KIND = "Users";

    private static final String PROPERTY_USER_CPF = "cpf";
    private static final String PROPERTY_USER_ID = "UserId";
    private static final String PROPERTY_PRODUCT_PRICE = "price";
    private static final String PROPERTY_ID = "ProductInterestId";
    private static final String PROPERTY_PRODUCT_CODE = "Code";
    private static final String PROPERTY_PRODUCT_ID = "ProductId";

    private void productInterestToEntity(ProductInterest productInterest, Entity productInterestEntity) {
        productInterestEntity.setProperty(PROPERTY_ID, productInterest.getId());
        productInterestEntity.setProperty(PROPERTY_USER_CPF, productInterest.getCpf());
        productInterestEntity.setProperty(PROPERTY_USER_ID, productInterest.getUserId());
        productInterestEntity.setProperty(PROPERTY_PRODUCT_PRICE, productInterest.getPrice());
        productInterestEntity.setProperty(PROPERTY_PRODUCT_ID, productInterest.getProductId());
    }

    private ProductInterest entityToProductInterest(Entity productInterestEntity) {
        ProductInterest productInterest = new ProductInterest();
        productInterest.setId(productInterestEntity.getKey().getId());
        productInterest.setCpf((String) productInterestEntity.getProperty(PROPERTY_USER_CPF));
        productInterest.setUserId((String) productInterestEntity.getProperty(PROPERTY_USER_ID));
        productInterest.setPrice((Long) productInterestEntity.getProperty(PROPERTY_PRODUCT_PRICE));
        productInterest.setProductId((String) productInterestEntity.getProperty(PROPERTY_PRODUCT_ID));
        return productInterest;
    }

    private boolean checkIfCpfExist(ProductInterest productInterest) {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query.Filter filter = new Query.FilterPredicate(PROPERTY_USER_CPF, Query.FilterOperator.EQUAL, productInterest.getCpf());
        Query query = new Query(USER_KIND).setFilter(filter);
        Entity userEntity = datastore.prepare(query).asSingleEntity();
        if (userEntity == null) {
            return false;
        } else {
            if (productInterest.getId() == null) {
                return true;
            } else {
                return userEntity.getKey().getId() != productInterest.getId();
            }
        }
    }

    private boolean checkIfProductExist(ProductInterest productInterest) {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query.Filter filter = new Query.FilterPredicate(PROPERTY_PRODUCT_CODE, Query.FilterOperator.EQUAL, Integer.parseInt(productInterest.getProductId()));
        Query query = new Query(PRODUCT_KIND).setFilter(filter);
        Entity productEntity = datastore.prepare(query).asSingleEntity();
        if (productEntity == null) {
            return false;
        } else {
            if (productInterest.getId() == null) {
                return true;
            } else {
                return productEntity.getKey().getId() != productInterest.getId();
            }
        }
    }

    public ProductInterest saveProductInterest(ProductInterest productInterest) throws UserNotFoundException, ProductNotFoundException {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        if (!checkIfCpfExist(productInterest)) {
            throw new UserNotFoundException("Usuário " + productInterest.getCpf() + " não encontrado");
        } else if (!checkIfProductExist(productInterest)) {
            System.out.println("Deu erro de produto");
            throw new ProductNotFoundException("Produto " + productInterest.getProductId() + " não encontrado");
        } else {
            //  Query by User
            Query.Filter userCpfFilter = new Query.FilterPredicate(PROPERTY_USER_CPF, Query.FilterOperator.EQUAL, productInterest.getCpf());
            Query userQuery = new Query(INTEREST_KIND).setFilter(userCpfFilter);
            Entity userEntity = datastore.prepare(userQuery).asSingleEntity();
            //  Query by Product
            Query.Filter emailFilter = new Query.FilterPredicate(PROPERTY_PRODUCT_ID, Query.FilterOperator.EQUAL, productInterest.getProductId());
            Query productQuery = new Query(INTEREST_KIND).setFilter(emailFilter);
            Entity productIdEntity = datastore.prepare(productQuery).asSingleEntity();
//            System.out.println(userEntity);
//            System.out.println(productIdEntity);
            if (userEntity == null && productIdEntity == null) {
                Key userKey = KeyFactory.createKey(INTEREST_KIND, INTEREST_KEY);
                Entity productEntity = new Entity(INTEREST_KIND, userKey);
                productInterestToEntity(productInterest, productEntity);
                datastore.put(productEntity);
                productInterest.setId(productEntity.getKey().getId());
            } else {
                if (userEntity.getKey() != productIdEntity.getKey()) {
                    Key userKey = KeyFactory.createKey(INTEREST_KIND, INTEREST_KEY);
                    Entity productEntity = new Entity(INTEREST_KIND, userKey);
                    productInterestToEntity(productInterest, productEntity);
                    datastore.put(productEntity);
                    productInterest.setId(productEntity.getKey().getId());
                } else {
                    productInterestToEntity(productInterest, userEntity);
                    datastore.put(userEntity);
                    productInterest.setId(userEntity.getKey().getId());
                }
            }
        }
        return productInterest;
    }

    public List<ProductInterest> getProductInterest() {
        List<ProductInterest> productInterest = new ArrayList<>();

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

        Query query;
        query = new Query(INTEREST_KIND).addSort(PROPERTY_USER_CPF, Query.SortDirection.ASCENDING);

        List<Entity> prodEntities = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());

        for (Entity prodEntity : prodEntities) {
            ProductInterest prod = entityToProductInterest(prodEntity);
            productInterest.add(prod);
        }
        return productInterest;
    }
}
