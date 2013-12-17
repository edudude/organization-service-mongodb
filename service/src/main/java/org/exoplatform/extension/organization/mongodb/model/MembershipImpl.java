package org.exoplatform.extension.organization.mongodb.model;

import org.exoplatform.services.organization.Membership;

/**
 * 
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * 
 */
public class MembershipImpl implements Membership, Comparable<MembershipImpl> {
  private String membershipType = "member";

  private String userName = null;

  private String groupId = null;

  private String id = null;

  public MembershipImpl() {
  }

  public MembershipImpl(String id) {
    String[] fields = id.split(":");
    if (fields[0] != null) {
      membershipType = fields[0];
    }
    if (fields[1] != null) {
      userName = fields[1];
    }
    if (fields[2] != null) {
      groupId = fields[2];
    }
    setId();
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
  public String getMembershipType() {
    return membershipType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setMembershipType(String membershipType) {
    this.membershipType = membershipType;
    setId();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
    setId();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
    setId();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MembershipImpl)) {
      return false;
    }
    return ((MembershipImpl) o).getId().equals(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return id.hashCode();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo(MembershipImpl o) {
    if (!(o instanceof Membership) || userName == null) {
      return 0;
    }
    Membership m = (Membership) o;
    return userName.compareTo(m.getUserName());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return "MembershipImpl{" + "membershipType='" + membershipType + '\'' + ", userName='" + userName + '\'' + ", groupId='" + groupId + '\'' + '}';
  }

  private void setId() {
    this.id = getId(userName, groupId, membershipType);
  }
  
  public static String getId(String userName, String groupId, String membershipType) {
    return membershipType + ":" + userName + ":" + groupId;
  }

}
