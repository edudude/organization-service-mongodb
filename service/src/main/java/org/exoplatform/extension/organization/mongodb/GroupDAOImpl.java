package org.exoplatform.extension.organization.mongodb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.naming.InvalidNameException;

import org.exoplatform.commons.exception.UniqueObjectException;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.SecurityHelper;
import org.exoplatform.extension.organization.mongodb.MongoDBManager.MongoListAccess;
import org.exoplatform.extension.organization.mongodb.model.GroupImpl;
import org.exoplatform.extension.organization.mongodb.model.MembershipImpl;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupEventListener;
import org.exoplatform.services.organization.GroupEventListenerHandler;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.security.PermissionConstants;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;

/**
 * 
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * 
 */
public class GroupDAOImpl implements GroupHandler, GroupEventListenerHandler {
  final private MongoDBManager mongoDBManager;

  private final Logger log = LoggerFactory.getLogger(GroupDAOImpl.class);

  private final List<GroupEventListener> listeners_ = new ArrayList<GroupEventListener>();

  public GroupDAOImpl(MongoDBManager mongoDBManager) {
    this.mongoDBManager = mongoDBManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Group createGroupInstance() {
    return new GroupImpl();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createGroup(Group group, boolean broadcast) throws Exception {
    addChild(null, group, broadcast);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void saveGroup(Group group, boolean broadcast) throws Exception {
    if (log.isTraceEnabled()) {
      log.trace("Save group : " + group.getId());
    }
    if (broadcast) {
      preSave(group, false);
    }
    this.mongoDBManager.saveOrUpdate(group);
    if (broadcast) {
      postSave(group, false);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addChild(Group parent, Group child, boolean broadcast) throws Exception {
    GroupImpl childImpl = (GroupImpl) child;
    if (parent != null) {
      Group parentGroup = findGroupById(parent.getId());
      if (parentGroup == null) {
        throw new InvalidNameException("Can't add node to not existed parent " + parent.getId());
      }
      childImpl.setParentId(parentGroup.getId());
    }

    Object o = findGroupById(child.getId());
    if (o != null) {
      Object[] args = { child.getGroupName() };
      throw new UniqueObjectException("OrganizationService.unique-group-exception", args);
    }

    if (broadcast) {
      preSave(childImpl, true);
    }
    this.mongoDBManager.saveOrUpdate(childImpl);
    if (broadcast) {
      postSave(childImpl, true);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Group removeGroup(Group group, boolean broadcast) throws Exception {
    if (broadcast) {
      preDelete(group);
    }
    this.mongoDBManager.remove(group);
    if (broadcast) {
      postDelete(group);
    }
    return group;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Group findGroupById(String groupId) throws Exception {
    return mongoDBManager.findOne(GroupImpl.class, "id", groupId);
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("rawtypes")
  @Override
  public Collection findGroups(Group parent) throws Exception {
    MongoListAccess<GroupImpl> listAccess = null;
    if (parent == null) {
      listAccess = mongoDBManager.find(GroupImpl.class, new String[] { "parentId" }, new String[] { null });
    } else {
      listAccess = mongoDBManager.find(GroupImpl.class, new String[] { "parentId" }, new String[] { parent.getId() });
    }
    return listAccess.getAll();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<Group> findGroupByMembership(String userName, String membershipType) throws Exception {
    ListAccess<MembershipImpl> listAccess = mongoDBManager.find(MembershipImpl.class, new String[] { "userName", "membershipType" }, new String[] { userName, membershipType });
    Collection<Group> groups = new ArrayList<Group>();
    MembershipImpl[] membershipImpls = listAccess.load(0, listAccess.getSize());
    for (MembershipImpl membershipImpl : membershipImpls) {
      GroupImpl groupImpl = mongoDBManager.findOne(GroupImpl.class, "id", membershipImpl.getGroupId());
      groups.add(groupImpl);
    }
    return groups;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<Group> findGroupsOfUser(String user) throws Exception {
    ListAccess<MembershipImpl> listAccess = mongoDBManager.find(MembershipImpl.class, new String[] { "userName" }, new String[] { user });
    Collection<Group> groups = new ArrayList<Group>();
    Object[] membershipImpls = listAccess.load(0, listAccess.getSize());
    for (Object membershipImpl : membershipImpls) {
      GroupImpl groupImpl = mongoDBManager.findOne(GroupImpl.class, "id", ((MembershipImpl) membershipImpl).getGroupId());
      groups.add(groupImpl);
    }
    return groups;
  }

  /**
   * {@inheritDoc}
   */
  @SuppressWarnings("rawtypes")
  @Override
  public Collection getAllGroups() throws Exception {
    MongoListAccess<GroupImpl> listAccess = mongoDBManager.find(GroupImpl.class, null, null);
    return listAccess.getAll();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addGroupEventListener(GroupEventListener listener) {
    SecurityHelper.validateSecurityPermission(PermissionConstants.MANAGE_LISTENERS);
    listeners_.add(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeGroupEventListener(GroupEventListener listener) {
    SecurityHelper.validateSecurityPermission(PermissionConstants.MANAGE_LISTENERS);
    listeners_.remove(listener);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<GroupEventListener> getGroupListeners() {
    return Collections.unmodifiableList(listeners_);
  }

  private void preSave(Group group, boolean isNew) throws Exception {
    for (GroupEventListener listener : listeners_)
      listener.preSave(group, isNew);
  }

  private void postSave(Group group, boolean isNew) throws Exception {
    for (GroupEventListener listener : listeners_)
      listener.postSave(group, isNew);
  }

  private void preDelete(Group group) throws Exception {
    for (GroupEventListener listener : listeners_)
      listener.preDelete(group);
  }

  private void postDelete(Group group) throws Exception {
    for (GroupEventListener listener : listeners_)
      listener.postDelete(group);
  }

}
