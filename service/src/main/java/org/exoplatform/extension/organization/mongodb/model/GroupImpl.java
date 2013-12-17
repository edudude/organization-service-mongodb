package org.exoplatform.extension.organization.mongodb.model;

import java.io.Serializable;

import org.exoplatform.services.organization.Group;

/**
 * 
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * 
 */
public class GroupImpl implements Group, Serializable, Comparable<GroupImpl> {
  private static final long serialVersionUID = 1020514351099165044L;

  private String id;

  private String parentId;

  private String groupName;

  private String label;

  private String desc;

  public GroupImpl() {
  }

  public GroupImpl(String name) {
    groupName = name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getId() {
    return id;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getParentId() {
    return parentId;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
    this.id = (parentId == null ? "" : parentId) + "/" + groupName;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getGroupName() {
    return groupName;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setGroupName(String name) {
    this.groupName = name;
    this.id = (parentId == null ? "" : parentId) + "/" + name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getLabel() {
    return label;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setLabel(String s) {
    label = s;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getDescription() {
    return desc;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setDescription(String s) {
    desc = s;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "Group[" + id + "|" + groupName + "]";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof GroupImpl)) {
      return false;
    }
    GroupImpl extGroup = (GroupImpl) o;
    if (id != null ? !id.equals(extGroup.id) : extGroup.id != null) {
      return false;
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    int result = id != null ? id.hashCode() : 0;
    result = 31 * result + (parentId != null ? parentId.hashCode() : 0);
    result = 31 * result + (groupName != null ? groupName.hashCode() : 0);
    result = 31 * result + (label != null ? label.hashCode() : 0);
    result = 31 * result + (desc != null ? desc.hashCode() : 0);
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo(GroupImpl o) {
    if (!(o instanceof Group)) {
      return 0;
    }

    Group group = (Group) o;
    return groupName.compareTo(group.getGroupName());

  }
}
