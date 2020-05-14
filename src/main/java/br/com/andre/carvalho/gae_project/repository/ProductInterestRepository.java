package br.com.andre.carvalho.gae_project.repository;

import br.com.andre.carvalho.gae_project.exception.ProductInterestNotFoundException;
import br.com.andre.carvalho.gae_project.exception.UserNotFoundException;
import br.com.andre.carvalho.gae_project.model.ProductInterest;
import br.com.andre.carvalho.gae_project.model.User;
import com.google.appengine.api.datastore.*;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

@Repository
public class ProductInterestRepository {
    private static final Logger log = Logger.getLogger("ProductInterestRepository");

    private static final String PRODUCT_INTEREST_KIND = "ProductInterest";
    private static final String PRODUCT_INTEREST_KEY = "productInterestKey";

    private static final String PROPERTY_ID = "id";
    private static final String USER_ID = "userId";
    private static final String PROPERTY_CPF = "cpf";
    private static final String PRODUCT_ID = "productId";
    private static final String PROPERTY_PRICE = "price";

    private void productInterestToEntity(ProductInterest productInterest, Entity productInterestEntity) {
        productInterestEntity.setProperty(PROPERTY_ID, productInterest.getId());
        productInterestEntity.setProperty(USER_ID, productInterest.getUserId());
        productInterestEntity.setProperty(PROPERTY_CPF, productInterest.getCpf());
        productInterestEntity.setProperty(PRODUCT_ID, productInterest.getProductId());
        productInterestEntity.setProperty(PROPERTY_PRICE, productInterest.getPrice());
    }

    public ProductInterest entityToProductInterest(Entity productInterestEntity) {
        ProductInterest productInterest = new ProductInterest();
        productInterest.setId(productInterestEntity.getKey().getId());
        productInterest.setUserId((String) productInterestEntity.getProperty(USER_ID));
        productInterest.setCpf((String) productInterestEntity.getProperty(PROPERTY_CPF));
        productInterest.setPrice((Double) productInterestEntity.getProperty(PROPERTY_PRICE));
        productInterest.setProductId((String) productInterestEntity.getProperty(PRODUCT_ID));
        return productInterest;
    }

    public ProductInterest saveProductInterest(ProductInterest productInterest) throws UserNotFoundException {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        UserRepository userRepository = new UserRepository();
        Optional optUser = userRepository.getByCpf(productInterest.getCpf());
        if (optUser.isPresent()) {
            Entity productInterestVerificationEntity = getByCpfAndProductId(productInterest.getCpf(), productInterest.getProductId());
            if (productInterestVerificationEntity == null) {
                Key productInterestKey = KeyFactory.createKey(PRODUCT_INTEREST_KIND, PRODUCT_INTEREST_KEY);
                Entity productInterestEntity = new Entity(PRODUCT_INTEREST_KIND, productInterestKey);
                productInterestToEntity(productInterest, productInterestEntity);
                datastore.put(productInterestEntity);
                productInterest.setId(productInterestEntity.getKey().getId());
                return productInterest;
            } else {
                productInterestVerificationEntity.setProperty(PROPERTY_PRICE, productInterest.getPrice());
                datastore.put(productInterestVerificationEntity);
                return entityToProductInterest(productInterestVerificationEntity);
            }
        } else {
            throw new UserNotFoundException("Usuário com cpf " + productInterest.getCpf() + " não encontrado!");
        }
    }

    private Entity getByCpfAndProductId(String cpf, String productId) {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query.Filter filterCpf = new Query.FilterPredicate(PROPERTY_CPF, Query.FilterOperator.EQUAL, cpf);
        Query.Filter filterProductId = new Query.FilterPredicate(PRODUCT_ID, Query.FilterOperator.EQUAL, productId);
        Query.Filter filterCpfProductId = Query.CompositeFilterOperator.and(filterCpf, filterProductId);
        Query query = new Query(PRODUCT_INTEREST_KIND).setFilter(filterCpfProductId);
        Entity productInterestEntity = datastore.prepare(query).asSingleEntity();
        return productInterestEntity;
    }

    public List<ProductInterest> getProductInterest(String cpf) {
        List<ProductInterest> productInterests = new ArrayList<>();
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query.Filter filterCpf = new Query.FilterPredicate(PROPERTY_CPF, Query.FilterOperator.EQUAL, cpf);
        Query query = new Query(PRODUCT_INTEREST_KIND).setFilter(filterCpf);
        List<Entity> productInterestEntities = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
        for (Entity productInterestEntity : productInterestEntities) {
            ProductInterest productInterest = entityToProductInterest(productInterestEntity);
            productInterests.add(productInterest);
        }
        return productInterests;
    }

    public ProductInterest deleteProductInterest(String cpf, String productId) throws ProductInterestNotFoundException {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Entity productInterestEntity = getByCpfAndProductId(cpf, productId);
        if (productInterestEntity != null) {
            datastore.delete(productInterestEntity.getKey());
            return entityToProductInterest(productInterestEntity);
        } else {
            throw new ProductInterestNotFoundException("Usuário com cpf " + cpf + " e ProductId " +
                    productId + "não encontrado!");
        }
    }
}
