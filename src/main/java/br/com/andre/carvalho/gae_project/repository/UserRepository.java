package br.com.andre.carvalho.gae_project.repository;

import br.com.andre.carvalho.gae_project.exception.UserAlreadyExistsException;
import br.com.andre.carvalho.gae_project.exception.UserNotFoundException;
import br.com.andre.carvalho.gae_project.model.User;
import com.google.appengine.api.datastore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheFactory;
import javax.cache.CacheManager;

@Repository
public class UserRepository {
    private static final Logger log = Logger.getLogger("UserRepository");

    private static final String USER_KIND = "Users";
    private static final String USER_KEY = "userKey";

    private static final String PROPERTY_ID = "UserId";
    private static final String PROPERTY_EMAIL = "email";
    private static final String PROPERTY_PASSWORD = "password";
    private static final String PROPERTY_FCM_REG_ID = "fcmRegId";
    private static final String PROPERTY_LAST_LOGIN = "lastLogin";
    private static final String PROPERTY_LAST_FCM_REGISTER = "lastFCMRegister";
    private static final String PROPERTY_ROLE = "role";
    private static final String PROPERTY_ENABLED = "enabled";

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        User adminUser;
        Optional<User> optAdminUser = this.getByEmail("matilde@siecola.com.br");
        try {
            if (optAdminUser.isPresent()) {
                adminUser = optAdminUser.get();
                if (!adminUser.getRole().equals("ROLE_ADMIN")) {
                    adminUser.setRole("ROLE_ADMIN");
                    this.updateUser(adminUser, "matilde@siecola.com.br");
                }
            } else {
                adminUser = new User();
                adminUser.setRole("ROLE_ADMIN");
                adminUser.setEnabled(true);
                adminUser.setPassword("matilde");
                adminUser.setEmail("matilde@siecola.com.br");
                this.saveUser(adminUser);
            }
        } catch (UserAlreadyExistsException | UserNotFoundException e
        ) {
            log.severe("Falha ao criar usuário ADMIN");
        }
    }

    private void userToEntity(User user, Entity userEntity) {
        userEntity.setProperty(PROPERTY_ID, user.getId());
        userEntity.setProperty(PROPERTY_EMAIL, user.getEmail());
        userEntity.setProperty(PROPERTY_PASSWORD, user.getPassword());
        userEntity.setProperty(PROPERTY_FCM_REG_ID, user.getFcmRegId());
        userEntity.setProperty(PROPERTY_LAST_LOGIN, user.getLastLogin());
        userEntity.setProperty(PROPERTY_LAST_FCM_REGISTER, user.getLastFCMRegister());
        userEntity.setProperty(PROPERTY_ROLE, user.getRole());
        userEntity.setProperty(PROPERTY_ENABLED, user.isEnabled());
    }

    private User entityToUser(Entity userEntity) {
        User user = new User();
        user.setId(userEntity.getKey().getId());
        user.setEmail((String) userEntity.getProperty(PROPERTY_EMAIL));
        user.setPassword((String) userEntity.getProperty(PROPERTY_PASSWORD));
        user.setFcmRegId((String) userEntity.getProperty(PROPERTY_FCM_REG_ID));
        user.setLastLogin((Date) userEntity.getProperty(PROPERTY_LAST_LOGIN));
        user.setLastFCMRegister((Date) userEntity.getProperty(PROPERTY_LAST_FCM_REGISTER));
        user.setRole((String) userEntity.getProperty(PROPERTY_ROLE));
        user.setEnabled((Boolean) userEntity.getProperty(PROPERTY_ENABLED));
        return user;
    }

    private boolean checkIfEmailExist(User user) {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query.Filter filter = new Query.FilterPredicate(PROPERTY_EMAIL, Query.FilterOperator.EQUAL, user.getEmail());
        Query query = new Query(USER_KIND).setFilter(filter);
        Entity userEntity = datastore.prepare(query).asSingleEntity();
        if (userEntity == null) {
            return false;
        } else {
            if (user.getId() == null) {
                return true;
            } else {
                return userEntity.getKey().getId() != user.getId();
            }
        }
    }

    public User saveUser(User user) throws UserAlreadyExistsException {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        if (!checkIfEmailExist(user)) {
            Key userKey = KeyFactory.createKey(USER_KIND, USER_KEY);
            Entity userEntity = new Entity(USER_KIND, userKey);
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            userToEntity(user, userEntity);
            datastore.put(userEntity);
            user.setId(userEntity.getKey().getId());
            return user;
        } else {
            throw new UserAlreadyExistsException("Usuário " + user.getEmail() + " já existe");
        }
    }

    public User updateUser(User user, String email) throws UserNotFoundException, UserAlreadyExistsException {
        if (!checkIfEmailExist(user)) {
            DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
            Query.Filter emailFilter = new Query.FilterPredicate(PROPERTY_EMAIL, Query.FilterOperator.EQUAL, email);
            Query query = new Query(USER_KIND).setFilter(emailFilter);
            Entity userEntity = datastore.prepare(query).asSingleEntity();
            if (userEntity != null) {
                userToEntity(user, userEntity);
                datastore.put(userEntity);
                user.setId(userEntity.getKey().getId());
                return user;
            } else {
                throw new UserNotFoundException("Usuário " + email + " não encontrado");
            }
        } else {
            throw new UserAlreadyExistsException("Usuário " + user.getEmail() + " já existe");
        }
    }

    public Optional<User> getByEmail(String email) {
        log.info("User: " + email);
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query.Filter filter = new Query.FilterPredicate(PROPERTY_EMAIL, Query.FilterOperator.EQUAL, email);
        Query query = new Query(USER_KIND).setFilter(filter);
        Entity userEntity = datastore.prepare(query).asSingleEntity();
        if (userEntity != null) {
            return Optional.ofNullable(entityToUser(userEntity));
        } else {
            return Optional.empty();
        }
    }

    public List<User> getUsers() {
        List<User> users = new ArrayList<>();
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query query;
        query = new Query(USER_KIND).addSort(PROPERTY_EMAIL, Query.SortDirection.ASCENDING);
        List<Entity> userEntities = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
        for (Entity userEntity : userEntities) {
            User user = entityToUser(userEntity);
            users.add(user);
        }
        return users;
    }

    public User deleteUser(String email) throws UserNotFoundException {
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        Query.Filter userFilter = new Query.FilterPredicate(PROPERTY_EMAIL, Query.FilterOperator.EQUAL, email);
        Query query = new Query(USER_KIND).setFilter(userFilter);
        Entity userEntity = datastore.prepare(query).asSingleEntity();
        if (userEntity != null) {
            datastore.delete(userEntity.getKey());
            return entityToUser(userEntity);
        } else {
            throw new UserNotFoundException("Usuário " + email + " não encontrado");
        }
    }
}
