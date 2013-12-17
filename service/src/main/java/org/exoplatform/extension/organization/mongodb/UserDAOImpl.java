package org.exoplatform.extension.organization.mongodb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.services.organization.ExtendedUserHandler;
import org.exoplatform.services.organization.Query;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserEventListener;
import org.exoplatform.services.organization.UserEventListenerHandler;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.impl.UserImpl;
import org.exoplatform.services.security.PasswordEncrypter;
import org.exoplatform.services.security.PermissionConstants;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;

/**
 * 
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * 
 */
@SuppressWarnings("deprecation")
public class UserDAOImpl implements UserHandler, UserEventListenerHandler, ExtendedUserHandler {
  final private MongoDBManager mongoDBManager;

  private final Logger log = LoggerFactory.getLogger(MembershipDAOImpl.class);

  private List<UserEventListener> listeners_ = new ArrayList<UserEventListener>(3);

  public UserDAOImpl(MongoDBManager mongoDBManager) {
    this.mongoDBManager = mongoDBManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean authenticate(String username, String password) throws Exception {
    return authenticate(username, password, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean authenticate(String username, String password, PasswordEncrypter pe) throws Exception {
    if (username == null || password == null) {
      return false;
    }
    if (pe != null) {
      byte[] bs = pe.encrypt(password.getBytes());
      password = new String(bs);
    }

    ListAccess<UserImpl> listAccess = mongoDBManager.find(UserImpl.class, new String[] { "userName", "password" }, new String[] { username, password });
    return listAccess.getSize() == 1;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<UserEventListener> getUserListeners() {
    return Collections.unmodifiableList(listeners_);
  }

  /**
   * {@inheritDoc}
   */
  public User createUserInstance() {
    return new UserImpl();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public User createUserInstance(String username) {
    return new UserImpl(username);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createUser(User user, boolean broadcast) throws Exception {
    if (broadcast) {
      preSave(user, true);
    }
    mongoDBManager.saveOrUpdate(user);
    if (broadcast) {
      postSave(user, true);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void saveUser(User user, boolean broadcast) throws Exception {
    if (log.isTraceEnabled()) {
      log.trace("Save User instance: " + user);
    }
    if (broadcast) {
      preSave(user, true);
    }
    mongoDBManager.saveOrUpdate(user);
    if (broadcast) {
      postSave(user, true);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public User removeUser(String userName, boolean broadcast) throws Exception {
    if (log.isTraceEnabled()) {
      log.trace("Remove User instance: " + userName);
    }
    User user = findUserByName(userName);
    if (user == null) {
      return null;
    }
    if (broadcast) {
      preDelete(user);
    }
    mongoDBManager.remove(user);
    if (broadcast) {
      postDelete(user);
    }
    return user;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public User findUserByName(String userName) throws Exception {
    return mongoDBManager.findOne(UserImpl.class, "userName", userName);
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated
  @Override
  public PageList<User> findUsersByGroup(String groupId) throws Exception {
    return new LazyPageList<User>(findUsersByGroupId(groupId), 20);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ListAccess<User> findUsersByGroupId(String groupId) throws Exception {
    return mongoDBManager.findUsersByGroupId(groupId);
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated
  @Override
  public PageList<User> getUserPageList(int pageSize) throws Exception {
    return new LazyPageList<User>(findAllUsers(), pageSize);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ListAccess<User> findAllUsers() throws Exception {
    return mongoDBManager.find(User.class, null, null);
  }

  /**
   * {@inheritDoc}
   */
  @Deprecated
  @Override
  public PageList<User> findUsers(Query query) throws Exception {
    return new LazyPageList<User>(findUsersByQuery(query), 20);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ListAccess<User> findUsersByQuery(Query query) throws Exception {
    if (query == null || isQueryEmpty(query)) {
      return findAllUsers();
    } else {
      List<String> keys = new ArrayList<String>();
      List<String> values = new ArrayList<String>();
      if (!isQueryStatementEmpty(query.getEmail())) {
        keys.add("email");
        values.add(query.getEmail());
      }
      if (!isQueryStatementEmpty(query.getFirstName())) {
        keys.add("firstName");
        values.add(query.getFirstName());
      }
      if (!isQueryStatementEmpty(query.getLastName())) {
        keys.add("lastName");
        values.add(query.getLastName());
      }
      if (!isQueryStatementEmpty(query.getUserName())) {
        keys.add("userName");
        values.add(query.getUserName());
      }
      if (!isQueryStatementEmpty(query.getFromLoginDate())) {
        // TODO
      }
      if (!isQueryStatementEmpty(query.getToLoginDate())) {
        // TODO
      }
      return mongoDBManager.findByRegex(User.class, keys.toArray(new String[0]), values.toArray(new String[0]));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addUserEventListener(UserEventListener listener) {
    SecurityHelper.validateSecurityPermission(PermissionConstants.MANAGE_LISTENERS);
    listeners_.add(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeUserEventListener(UserEventListener listener) {
    SecurityHelper.validateSecurityPermission(PermissionConstants.MANAGE_LISTENERS);
    listeners_.remove(listener);
  }

  private boolean isQueryEmpty(Query query) {
    return isQueryStatementEmpty(query.getEmail()) && isQueryStatementEmpty(query.getFirstName()) && isQueryStatementEmpty(query.getLastName()) && isQueryStatementEmpty(query.getUserName())
        && isQueryStatementEmpty(query.getFromLoginDate()) && isQueryStatementEmpty(query.getToLoginDate());
  }

  private void preSave(User user, boolean isNew) throws Exception {
    for (UserEventListener listener : listeners_)
      listener.preSave(user, isNew);
  }

  private void postSave(User user, boolean isNew) throws Exception {
    for (UserEventListener listener : listeners_)
      listener.postSave(user, isNew);
  }

  private void preDelete(User user) throws Exception {
    for (UserEventListener listener : listeners_)
      listener.preDelete(user);
  }

  private void postDelete(User user) throws Exception {
    for (UserEventListener listener : listeners_)
      listener.postDelete(user);
  }

  private boolean isQueryStatementEmpty(Object value) {
    return value == null || value.toString().isEmpty() || value.toString().equals("*");
  }

}
