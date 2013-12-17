package org.exoplatform.extension.organization.mongodb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.extension.organization.mongodb.MongoDBManager.MongoListAccess;
import org.exoplatform.extension.organization.mongodb.model.MembershipImpl;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipEventListener;
import org.exoplatform.services.organization.MembershipEventListenerHandler;
import org.exoplatform.services.organization.MembershipHandler;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.security.PermissionConstants;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;

/**
 * 
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * 
 */
public class MembershipDAOImpl implements MembershipHandler, MembershipEventListenerHandler {
  final private MongoDBManager mongoDBManager;

  private List<MembershipEventListener> listeners_ = new ArrayList<MembershipEventListener>();

  private final Logger log = LoggerFactory.getLogger(MembershipDAOImpl.class);

  public MembershipDAOImpl(MongoDBManager mongoDBManager) {
    this.mongoDBManager = mongoDBManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  final public MembershipImpl createMembershipInstance() {
    return new MembershipImpl();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createMembership(Membership m, boolean broadcast) throws Exception {
    if (log.isTraceEnabled()) {
      log.trace("Create Membership " + m);
    }
    if (broadcast) {
      preSave(m, true);
    }
    MembershipImpl membership = mongoDBManager.findOne(MembershipImpl.class, "id", m.getId());
    if (membership == null) {
      mongoDBManager.saveOrUpdate(m);
    }
    if (broadcast) {
      postSave(m, true);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void linkMembership(User user, Group group, MembershipType m, boolean broadcast) throws Exception {
    if (user == null) {
      throw new IllegalArgumentException("User is null");
    }
    if (group == null) {
      throw new IllegalArgumentException("Group is null");
    }
    if (m == null) {
      throw new IllegalArgumentException("MembershipType is null");
    }
    MembershipImpl membership = createMembershipInstance();
    membership.setMembershipType(m.getName());
    membership.setGroupId(group.getId());
    membership.setUserName(user.getUserName());
    createMembership(membership, broadcast);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Membership removeMembership(String id, boolean broadcast) throws Exception {
    Membership m = mongoDBManager.findOne(MembershipImpl.class, "id", id);
    return removeMembership(m, broadcast);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<Membership> removeMembershipByUser(String username, boolean broadcast) throws Exception {
    MongoListAccess<Membership> listAccess = mongoDBManager.find(Membership.class, new String[] { "userName" }, new String[] { username });
    Collection<Membership> memberships = listAccess.getAll();
    for (Membership membership : memberships) {
      removeMembership(membership, broadcast);
    }
    return memberships;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Membership findMembership(String id) throws Exception {
    Membership membership = mongoDBManager.findOne(MembershipImpl.class, "id", id);
    if (membership == null) {
      throw new IllegalStateException("Membership not found.");
    }
    return membership;
  }

  @Override
  public Membership findMembershipByUserGroupAndType(String userName, String groupId, String type) throws Exception {
    return mongoDBManager.findOne(MembershipImpl.class, "id", MembershipImpl.getId(userName, groupId, type));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<Membership> findMembershipsByUserAndGroup(String userName, String groupId) throws Exception {
    return mongoDBManager.find(Membership.class, new String[] { "userName", "groupId" }, new String[] { userName, groupId }).getAll();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<Membership> findMembershipsByUser(String userName) throws Exception {
    return mongoDBManager.find(Membership.class, new String[] { "userName" }, new String[] { userName }).getAll();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<Membership> findMembershipsByGroup(Group group) throws Exception {
    return mongoDBManager.find(Membership.class, new String[] { "groupId" }, new String[] { group.getId() }).getAll();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ListAccess<Membership> findAllMembershipsByGroup(Group group) throws Exception {
    return mongoDBManager.find(Membership.class, new String[] { "groupId" }, new String[] { group.getId() });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addMembershipEventListener(MembershipEventListener listener) {
    SecurityHelper.validateSecurityPermission(PermissionConstants.MANAGE_LISTENERS);
    listeners_.add(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeMembershipEventListener(MembershipEventListener listener) {
    SecurityHelper.validateSecurityPermission(PermissionConstants.MANAGE_LISTENERS);
    listeners_.remove(listener);
  }

  /**
   * {@inheritDoc}
   */
  public List<MembershipEventListener> getMembershipListeners() {
    return Collections.unmodifiableList(listeners_);
  }

  private void preSave(Membership membership, boolean isNew) throws Exception {
    for (int i = 0; i < listeners_.size(); i++) {
      MembershipEventListener listener = listeners_.get(i);
      listener.preSave(membership, isNew);
    }
  }

  private void postSave(Membership membership, boolean isNew) throws Exception {
    for (int i = 0; i < listeners_.size(); i++) {
      MembershipEventListener listener = listeners_.get(i);
      listener.postSave(membership, isNew);
    }
  }

  private void preDelete(Membership membership) throws Exception {
    for (int i = 0; i < listeners_.size(); i++) {
      MembershipEventListener listener = listeners_.get(i);
      listener.preDelete(membership);
    }
  }

  private void postDelete(Membership membership) throws Exception {
    for (int i = 0; i < listeners_.size(); i++) {
      MembershipEventListener listener = listeners_.get(i);
      listener.postDelete(membership);
    }
  }

  private Membership removeMembership(Membership m, boolean broadcast) throws Exception {
    if (m == null) {
      return null;
    }
    if (broadcast) {
      preDelete(m);
    }
    boolean removed = mongoDBManager.remove(m);
    if (!removed) {
      throw new RuntimeException("Error while deleting '" + m + "'.");
    }
    if (broadcast) {
      postDelete(m);
    }
    return m;
  }

}
