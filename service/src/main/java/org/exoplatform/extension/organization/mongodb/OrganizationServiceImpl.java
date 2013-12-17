package org.exoplatform.extension.organization.mongodb;

import org.exoplatform.services.organization.BaseOrganizationService;
import org.picocontainer.Startable;

/**
 * 
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * 
 */
public class OrganizationServiceImpl extends BaseOrganizationService implements Startable {

  public OrganizationServiceImpl(MongoDBManager mongoDBManager) {
    userDAO_ = new UserDAOImpl(mongoDBManager);
    userProfileDAO_ = new UserProfileDAOImpl(mongoDBManager);
    groupDAO_ = new GroupDAOImpl(mongoDBManager);
    membershipTypeDAO_ = new MembershipTypeDAOImpl(mongoDBManager);
    membershipDAO_ = new MembershipDAOImpl(mongoDBManager);
  }

  @Override
  public void start() {
    super.start();
  }
}
