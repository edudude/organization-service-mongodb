package org.exoplatform.extension.organization.mongodb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.organization.UserProfileEventListener;
import org.exoplatform.services.organization.UserProfileEventListenerHandler;
import org.exoplatform.services.organization.UserProfileHandler;
import org.exoplatform.services.organization.impl.UserProfileImpl;

/**
 * 
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * 
 */
public class UserProfileDAOImpl implements UserProfileHandler, UserProfileEventListenerHandler {
  final private MongoDBManager mongoDBManager;

  private List<UserProfileEventListener> listeners_ = new ArrayList<UserProfileEventListener>();

  public UserProfileDAOImpl(MongoDBManager mongoDBManager) {
    this.mongoDBManager = mongoDBManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void saveUserProfile(UserProfile profile, boolean broadcast) throws Exception {
    if (broadcast) {
      preSave(profile, true);
    }
    mongoDBManager.saveOrUpdate(profile);
    if (broadcast) {
      postSave(profile, true);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UserProfile removeUserProfile(String userName, boolean broadcast) throws Exception {
    UserProfile profile = findUserProfileByName(userName);
    if (profile != null) {
      if (broadcast) {
        preDelete(profile);
      }
      mongoDBManager.remove(profile);
      if (broadcast) {
        postDelete(profile);
      }
    }
    return profile;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UserProfile findUserProfileByName(String userName) throws Exception {
    return mongoDBManager.findOne(UserProfile.class, "userName", userName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<UserProfile> findUserProfiles() throws Exception {
    return mongoDBManager.find(UserProfile.class, null, null).getAll();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final UserProfile createUserProfileInstance() {
    return new UserProfileImpl();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public UserProfile createUserProfileInstance(String userName) {
    return new UserProfileImpl(userName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addUserProfileEventListener(UserProfileEventListener listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Listener cannot be null");
    }
    listeners_.add(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeUserProfileEventListener(UserProfileEventListener listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Listener cannot be null");
    }
    listeners_.remove(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<UserProfileEventListener> getUserProfileListeners() {
    return Collections.unmodifiableList(listeners_);
  }

  private void preSave(UserProfile profile, boolean isNew) throws Exception {
    for (UserProfileEventListener listener : listeners_) {
      listener.preSave(profile, isNew);
    }
  }

  private void postSave(UserProfile profile, boolean isNew) throws Exception {
    for (UserProfileEventListener listener : listeners_) {
      listener.postSave(profile, isNew);
    }
  }

  private void preDelete(UserProfile profile) throws Exception {
    for (UserProfileEventListener listener : listeners_) {
      listener.preDelete(profile);
    }
  }

  private void postDelete(UserProfile profile) throws Exception {
    for (UserProfileEventListener listener : listeners_) {
      listener.postDelete(profile);
    }
  }

}
