package org.exoplatform.extension.organization.mongodb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.MembershipTypeEventListener;
import org.exoplatform.services.organization.MembershipTypeEventListenerHandler;
import org.exoplatform.services.organization.MembershipTypeHandler;
import org.exoplatform.services.organization.impl.MembershipTypeImpl;

/**
 * 
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * 
 */
public class MembershipTypeDAOImpl implements MembershipTypeHandler, MembershipTypeEventListenerHandler {
  final private MongoDBManager mongoDBManager;

  private List<MembershipTypeEventListener> listeners_ = new ArrayList<MembershipTypeEventListener>();

  public MembershipTypeDAOImpl(MongoDBManager mongoDBManager) {
    this.mongoDBManager = mongoDBManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<MembershipTypeEventListener> getMembershipTypeListeners() {
    return Collections.unmodifiableList(listeners_);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MembershipType createMembershipType(MembershipType mt, boolean broadcast) throws Exception {
    return saveMembershipType(mt, broadcast);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MembershipType saveMembershipType(MembershipType mt, boolean broadcast) throws Exception {
    Date now = new Date();
    mt.setModifiedDate(now);
    if (broadcast) {
      preSave(mt, true);
    }
    mongoDBManager.saveOrUpdate(mt);
    if (broadcast) {
      postSave(mt, true);
    }
    return mt;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MembershipType removeMembershipType(String name, boolean broadcast) throws Exception {
    MembershipType mt = findMembershipType(name);
    if (mt == null) {
      throw new IllegalArgumentException("MembershipType with name=" + name + " doesn't exist.");
    } else {
      if (broadcast) {
        preDelete(mt);
      }
      mongoDBManager.remove(mt);
      if (broadcast) {
        postDelete(mt);
      }
    }
    return mt;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public MembershipType findMembershipType(String name) throws Exception {
    return mongoDBManager.findOne(MembershipType.class, "name", name);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<MembershipType> findMembershipTypes() throws Exception {
    return mongoDBManager.find(MembershipType.class, null, null).getAll();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addMembershipTypeEventListener(MembershipTypeEventListener listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Listener cannot be null");
    }

    listeners_.add(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeMembershipTypeEventListener(MembershipTypeEventListener listener) {
    if (listener == null) {
      throw new IllegalArgumentException("Listener cannot be null");
    }
    listeners_.remove(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final MembershipType createMembershipTypeInstance() {
    return new MembershipTypeImpl();
  }

  private void preSave(MembershipType membershipType, boolean isNew) throws Exception {
    for (int i = 0; i < listeners_.size(); i++) {
      MembershipTypeEventListener listener = (MembershipTypeEventListener) listeners_.get(i);
      listener.preSave(membershipType, isNew);
    }
  }

  private void postSave(MembershipType membershipType, boolean isNew) throws Exception {
    for (int i = 0; i < listeners_.size(); i++) {
      MembershipTypeEventListener listener = (MembershipTypeEventListener) listeners_.get(i);
      listener.postSave(membershipType, isNew);
    }
  }

  private void preDelete(MembershipType membershipType) throws Exception {
    for (int i = 0; i < listeners_.size(); i++) {
      MembershipTypeEventListener listener = (MembershipTypeEventListener) listeners_.get(i);
      listener.preDelete(membershipType);
    }
  }

  private void postDelete(MembershipType membershipType) throws Exception {
    for (int i = 0; i < listeners_.size(); i++) {
      MembershipTypeEventListener listener = (MembershipTypeEventListener) listeners_.get(i);
      listener.postDelete(membershipType);
    }
  }

}
