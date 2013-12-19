package org.exoplatform.extension.organization.benchmark;

import java.util.Calendar;
import java.util.Map;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.StandaloneContainer;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.MembershipHandler;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.MembershipTypeHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.Query;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.organization.UserProfileHandler;

import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.caliper.api.Macrobenchmark;

/**
 * 
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * 
 */
public class UserHandlerBenshmark extends Benchmark {

  @Param({ "/conf/standalone/test-configuration-hibernate.xml", "/conf/standalone/test-configuration-mongodb.xml" })
  String configPath = "/conf/standalone/test-configuration-hibernate.xml";

  protected static GroupHandler gHandler;

  protected static MembershipHandler mHandler;

  protected static UserHandler uHandler;

  protected static MembershipTypeHandler mtHandler;

  protected static UserProfileHandler upHandler;

  protected static String membershipType = "type";

  protected static String userName = "user";

  protected static String newUserName = "newUser";

  protected static String groupName1 = "group1";

  protected static String groupName2 = "group2";

  protected static StandaloneContainer container;

  /**
   * {@inheritDoc}
   */
  // @BeforeExperiment
  public void setUp() throws Exception {
    if (upHandler != null) {
      return;
    }
    if (configPath == null) {
      throw new IllegalStateException("configPath is null.");
    }
    String containerConf = getClass().getResource(configPath).toString();

    StandaloneContainer.addConfigurationURL(containerConf);
    container = StandaloneContainer.getInstance();

    OrganizationService organizationService = (OrganizationService) container.getComponentInstanceOfType(OrganizationService.class);

    gHandler = organizationService.getGroupHandler();
    uHandler = organizationService.getUserHandler();
    mHandler = organizationService.getMembershipHandler();
    mtHandler = organizationService.getMembershipTypeHandler();
    upHandler = organizationService.getUserProfileHandler();
    if (uHandler.findUserByName("tolik") == null) {
      createUser("tolik");
      createUser("rolik");
      createUser("bolik");
      createUser("volik");
    }
  }

  /**
   * {@inheritDoc}
   */
  // @AfterExperiment
  public void tearDown() throws Exception {
    if (container == null) {
      return;
    }
    // // remove all users
    // Iterator<String> iter = users.iterator();
    // while (iter.hasNext()) {
    // String userName = iter.next();
    //
    // if (uHandler.findUserByName(userName) != null) {
    // uHandler.removeUser(userName, true);
    // }
    // iter.remove();
    // }
    //
    // // remove all membership types
    // iter = types.iterator();
    // while (iter.hasNext()) {
    // String type = iter.next();
    //
    // if (mtHandler.findMembershipType(type) != null) {
    // mtHandler.removeMembershipType(type, true);
    // }
    // iter.remove();
    // }
    //
    // // remove all groups
    // iter = groups.iterator();
    // while (iter.hasNext()) {
    // String groupId = iter.next();
    //
    // removeGroups(groupId);
    // iter.remove();
    // }
  }

  /**
   * User authentication.
   */
  // @Benchmark
  @Macrobenchmark
  public void timeAuthenticate() throws Exception {
    uHandler.authenticate("demo", "exo");
  }

  /**
   * Find user by name.
   */
  // @Benchmark
  @Macrobenchmark
  public void timeFindUserByName() throws Exception {
    System.out.println("testFindUserByName");
    uHandler.findUserByName("demo");
  }

  /**
   * Find users by query.
   */
  // @Benchmark
  @Macrobenchmark
  public void timeFindUsersByQuery() throws Exception {
    System.out.println("testFindUsersByQuery");

    uHandler.authenticate("tolik", "pwd");

    Query query = new Query();
    query.setEmail("email@test");

    // try to find user by email
    uHandler.findUsersByQuery(query).getSize();

    // try to find user by name with mask
    query = new Query();
    query.setUserName("*tolik*");
    uHandler.findUsersByQuery(query).getSize();

    // try to find user by name with mask
    query = new Query();
    query.setUserName("tol*");
    uHandler.findUsersByQuery(query).getSize();

    // try to find user by name with mask
    query = new Query();
    query.setUserName("*lik");
    uHandler.findUsersByQuery(query).getSize();

    // try to find user by name explicitly
    query = new Query();
    query.setUserName("tolik");
    uHandler.findUsersByQuery(query).getSize();

    // try to find user by name explicitly, case sensitive search
    query = new Query();
    query.setUserName("Tolik");
    uHandler.findUsersByQuery(query).getSize();

    // try to find user by part of name without mask
    query = new Query();
    query.setUserName("tol");
    uHandler.findUsersByQuery(query).getSize();

    // try to find user by fist and last names, case sensitive search
    query = new Query();
    query.setFirstName("fiRst");
    query.setLastName("lasT");
    uHandler.findUsersByQuery(query).getSize();

    String skipDateTests = System.getProperty("orgservice.test.configuration.skipDateTests");
    if (!"true".equals(skipDateTests)) {
      // try to find user by login date
      Calendar calc = Calendar.getInstance();
      calc.set(Calendar.YEAR, calc.get(Calendar.YEAR) - 1);

      query = new Query();
      query.setFromLoginDate(calc.getTime());
      query.setUserName("tolik");
      uHandler.findUsersByQuery(query).getSize();

      calc = Calendar.getInstance();
      calc.set(Calendar.YEAR, calc.get(Calendar.YEAR) + 1);

      query = new Query();
      query.setFromLoginDate(calc.getTime());
      uHandler.findUsersByQuery(query).getSize();

      calc = Calendar.getInstance();
      calc.set(Calendar.YEAR, calc.get(Calendar.YEAR) - 1);

      query = new Query();
      query.setToLoginDate(calc.getTime());
      uHandler.findUsersByQuery(query).getSize();

      calc = Calendar.getInstance();
      calc.set(Calendar.YEAR, calc.get(Calendar.YEAR) + 1);

      query = new Query();
      query.setToLoginDate(calc.getTime());
      query.setUserName("tolik");
      uHandler.findUsersByQuery(query).getSize();
    }

    query = new Query();
    query.setUserName("olik");

    ListAccess<User> users = uHandler.findUsersByQuery(query);
    users.load(0, users.getSize());
  }

  /**
   * Find users.
   */
  @SuppressWarnings("deprecation")
  // @Benchmark
  @Macrobenchmark
  public void timeFindUsers() throws Exception {
    System.out.println("testFindUsers");
    uHandler.authenticate("tolik", "pwd");

    Query query = new Query();
    query.setEmail("email@test");

    // try to find user by email
    uHandler.findUsers(query).getAll().size();

    // try to find user by name with mask
    query = new Query();
    query.setUserName("*tolik*");
    uHandler.findUsers(query).getAll().size();

    // try to find user by name with mask
    query = new Query();
    query.setUserName("tol*");
    uHandler.findUsers(query).getAll().size();

    // try to find user by name with mask
    query = new Query();
    query.setUserName("*lik");
    uHandler.findUsers(query).getAll().size();

    // try to find user by name explicitly
    query = new Query();
    query.setUserName("tolik");
    uHandler.findUsers(query).getAll().size();

    // try to find user by name explicitly, case sensitive search
    query = new Query();
    query.setUserName("Tolik");
    uHandler.findUsers(query).getAll().size();

    // try to find user by part of name without mask
    query = new Query();
    query.setUserName("tol");
    uHandler.findUsers(query).getAll().size();

    // try to find user by fist and last names, case sensitive search
    query = new Query();
    query.setFirstName("fiRst");
    query.setLastName("lasT");
    uHandler.findUsers(query).getAll().size();

    String skipDateTests = System.getProperty("orgservice.test.configuration.skipDateTests");
    if (!"true".equals(skipDateTests)) {
      // try to find user by login date
      Calendar calc = Calendar.getInstance();
      calc.set(Calendar.YEAR, calc.get(Calendar.YEAR) - 1);

      query = new Query();
      query.setFromLoginDate(calc.getTime());
      query.setUserName("tolik");
      uHandler.findUsers(query).getAll().size();

      calc = Calendar.getInstance();
      calc.set(Calendar.YEAR, calc.get(Calendar.YEAR) + 1);

      query = new Query();
      query.setFromLoginDate(calc.getTime());
      uHandler.findUsers(query).getAll().size();

      calc = Calendar.getInstance();
      calc.set(Calendar.YEAR, calc.get(Calendar.YEAR) - 1);

      query = new Query();
      query.setToLoginDate(calc.getTime());
      uHandler.findUsers(query).getAll().size();

      calc = Calendar.getInstance();
      calc.set(Calendar.YEAR, calc.get(Calendar.YEAR) + 1);

      query = new Query();
      query.setToLoginDate(calc.getTime());
      query.setUserName("tolik");
      uHandler.findUsers(query).getAll().size();
    }
  }

  /**
   * Get users page list.
   */
  @SuppressWarnings("deprecation")
  // @Benchmark
  @Macrobenchmark
  public void timeGetUserPageList() throws Exception {
    System.out.println("testGetUserPageList");
    uHandler.getUserPageList(10).getAll().size();
  }

  /**
   * Find all users.
   */
  // @Benchmark
  @Macrobenchmark
  public void timeFindAllUsers() throws Exception {
    System.out.println("testFindAllUsers");
    ListAccess<User> users = uHandler.findAllUsers();
    users.load(0, users.getSize());
  }

  /**
   * Save user.
   */
  // //@Benchmark
  // public void timeSaveUser() throws Exception {
  // System.out.println("testSaveUser");
  // createUser(userName);
  // User u = uHandler.findUserByName(userName);
  // u.setEmail("new@Email");
  // uHandler.saveUser(u, true);
  // }

  /**
   * Remove user.
   */
  // @Benchmark
  @Macrobenchmark
  public void timeRemoveUser() throws Exception {
    System.out.println("testRemoveUser");
    uHandler.removeUser(userName, true);
  }

  /**
   * Create user.
   */
  // //@Benchmark
  // public void timeCreateUser() throws Exception {
  // System.out.println("testCreateUser");
  // User u = uHandler.createUserInstance(userName);
  // u.setEmail("email@test");
  // u.setFirstName("first");
  // u.setLastName("last");
  // u.setPassword("pwd");
  // uHandler.createUser(u, true);
  // }

  /**
   * Create new user for test purpose only.
   */
  @SuppressWarnings("deprecation")
  protected void createUser(String userName) throws Exception {
    User u = uHandler.createUserInstance(userName);
    u.setEmail("email@test");
    u.setFirstName("first");
    u.setLastLoginTime(Calendar.getInstance().getTime());
    u.setCreatedDate(Calendar.getInstance().getTime());
    u.setLastName("last");
    u.setPassword("pwd");
    uHandler.createUser(u, true);
  }

  /**
   * Create user with profile.
   */
  protected void createUserProfile(String userName) throws Exception {
    UserProfile up = upHandler.createUserProfileInstance(userName);
    Map<String, String> attributes = up.getUserInfoMap();
    attributes.put("key1", "value1");
    attributes.put("key2", "value2");
    upHandler.saveUserProfile(up, true);
  }

  /**
   * Create membership type.
   */
  protected void createMembershipType(String type, String desc) throws Exception {
    MembershipType mt = mtHandler.createMembershipTypeInstance();
    mt.setName(type);
    mt.setDescription(desc);
    mtHandler.createMembershipType(mt, true);
  }

  /**
   * Create new group.
   */
  protected void createGroup(String parentId, String name, String label, String desc) throws Exception {
    Group parent = parentId == null ? null : gHandler.findGroupById(parentId);

    Group child = gHandler.createGroupInstance();
    child.setGroupName(name);
    child.setLabel(label);
    child.setDescription(desc);
    gHandler.addChild(parent, child, true);
  }

  /**
   * Create membership.
   */
  protected void createMembership(String userName, String groupName, String type) throws Exception {
    createUser(userName);
    createGroup(null, groupName, "lable", "desc");
    createMembershipType(type, "desc");

    // link membership
    mHandler.linkMembership(uHandler.findUserByName(userName), gHandler.findGroupById("/" + groupName), mtHandler.findMembershipType(type), true);
  }

  /**
   * Create new group instance.
   */
  protected Group createGroupInstance(String parentId, String name, String label, String desc) throws Exception {
    createGroup(null, name, "lable", "desc");
    return gHandler.removeGroup(gHandler.findGroupById("/" + name), true);
  }

  // private void removeGroups(String parentId) throws Exception {
  // Group group = gHandler.findGroupById(parentId);
  // if (group != null) {
  //
  // @SuppressWarnings("unchecked")
  // Collection<Group> childs = gHandler.findGroups(group);
  // for (Group child : childs) {
  // removeGroups(child.getId());
  // }
  //
  // gHandler.removeGroup(group, true);
  // }
  // }

}
