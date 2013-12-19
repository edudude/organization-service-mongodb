package org.exoplatform.extension.organization.mongodb;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.beanutils.BeanMap;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.extension.organization.mongodb.model.GroupImpl;
import org.exoplatform.extension.organization.mongodb.model.MembershipImpl;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipType;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserProfile;
import org.exoplatform.services.organization.impl.MembershipTypeImpl;
import org.exoplatform.services.organization.impl.UserImpl;
import org.exoplatform.services.organization.impl.UserProfileImpl;
import org.gatein.common.logging.Logger;
import org.gatein.common.logging.LoggerFactory;
import org.picocontainer.Startable;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.ServerAddress;

/**
 * 
 * @author <a href="mailto:boubaker.khanfir@exoplatform.com">Boubaker
 *         Khanfir</a>
 * 
 */
public class MongoDBManager implements Startable {

  private static final String MT_COLLECTION = "mt";

  private static final String PROFILES_COLLECTION = "profiles";

  private static final String GROUPS_COLLECTION = "groups";

  private static final String MEMBERSHIPS_COLLECTION = "memberships";

  private static final String USERS_COLLECTION = "users";

  private final Logger log = LoggerFactory.getLogger(MongoDBManager.class);

  /** . */
  private DB db;

  /** . */
  private final String collectionName;

  /** . */
  private final String host;

  /** . */
  private final int port;

  /** . */
  private final String username;

  /** . */
  private final String password;

  public MongoDBManager(InitParams params) {
    ValueParam hostParam = params.getValueParam("host");
    ValueParam portParam = params.getValueParam("port");
    ValueParam collectionNameParam = params.getValueParam("collection-name");
    ValueParam usernameParam = params.getValueParam("username");
    ValueParam passwordParam = params.getValueParam("password");

    this.host = hostParam != null ? hostParam.getValue().trim() : "localhost";
    this.collectionName = collectionNameParam != null ? collectionNameParam.getValue().trim() : "organization";
    this.port = portParam != null ? Integer.parseInt(portParam.getValue().trim()) : 27017;
    username = usernameParam != null ? usernameParam.getValue().trim() : null;
    password = passwordParam != null ? passwordParam.getValue().trim() : null;
  }

  @Override
  public void start() {
    Mongo mongo = null;
    try {
      log.info("Connecting to MongoDB " + host + ":" + port);
      mongo = new Mongo(new ServerAddress(host, port));
    } catch (Exception e) {
      throw new RuntimeException("Error while connecting to mongo DB server.", e);
    }
    db = mongo.getDB(collectionName);
    if (username != null && password != null) {
      boolean authenticated = db.authenticate(username, password.toCharArray());
      if (!authenticated) {
        throw new IllegalStateException("Cannot connect to MongoDB with credentials.");
      }
    }
    Set<String> collections = db.getCollectionNames();
    if (collections == null || collections.isEmpty()) {
      log.info("Indexing MongoDB Organization Collections.");

      DBCollection collection = getUserDB();
      DBObject dbObject = new BasicDBObject();
      dbObject.put("userName", 1);
      dbObject.put("email", 1);
      collection.ensureIndex(dbObject);

      collection = getUserProfileDB();
      collection.ensureIndex("userName");

      collection = getGroupDB();
      collection.ensureIndex("id");

      collection = getMembershipTypeDB();
      collection.ensureIndex("name");

      collection = getMembershipDB();
      dbObject = new BasicDBObject();
      dbObject.put("userName", 1);
      dbObject.put("groupId", 1);
      dbObject.put("id", 1);
      collection.ensureIndex(dbObject);
    }
  }

  protected void saveOrUpdate(Object object) {
    if (log.isTraceEnabled()) {
      log.trace("Save or update " + object);
    }
    DBCollection collection = getDBCollection(object);
    DBObject key = getKey(object);
    DBCursor cursor = collection.find(key);
    DBObject doc = null;
    if (cursor.count() > 1) {
      throw new IllegalStateException("More than one object was found for key: " + key + ", in collection: " + collection.getName());
    } else if (cursor.count() == 1) {
      doc = cursor.next();
    }
    if (doc == null) {
      doc = new BasicDBObject();
      if (object instanceof Membership) {
        // verify if Membership entries are existing, switch API need
        Membership membership = (Membership) object;
        DBObject dbObject = new BasicDBObject("userName", membership.getUserName());
        long count = getUserDB().count(dbObject);
        if (count != 1) {
          throw new IllegalStateException("Membership " + membership + " can't be saved, user doesn't exist.");
        }
        dbObject = new BasicDBObject("id", membership.getGroupId());
        count = getGroupDB().count(dbObject);
        if (count != 1) {
          throw new IllegalStateException("Membership " + membership + " can't be saved, group doesn't exist.");
        }
        dbObject = new BasicDBObject("name", membership.getMembershipType());
        count = getMembershipTypeDB().count(dbObject);
        if (count != 1) {
          throw new IllegalStateException("Membership " + membership + " can't be saved, membershipType doesn't exist.");
        }
      } else if (object instanceof UserProfile) {
        // verify if User exists, switch API need
        UserProfile userProfile = (UserProfile) object;
        DBObject dbObject = new BasicDBObject("userName", userProfile.getUserName());
        long count = getUserDB().count(dbObject);
        if (count != 1) {
          throw new IllegalStateException("UserProfile " + userProfile + " can't be saved, user doesn't exist.");
        }
      }
      putFields(doc, object);
      String error = collection.save(doc).getError();
      if (error != null && !error.isEmpty()) {
        throw new IllegalStateException("Error while updating entity: " + doc + ", detail:" + error);
      }
    } else {
      putFields(doc, object);
      String error = collection.update(key, doc).getError();
      if (error != null && !error.isEmpty()) {
        throw new IllegalStateException("Error while updating entity: " + doc + ", detail:" + error);
      }
    }
  }

  protected boolean remove(Object object) {
    if (log.isTraceEnabled()) {
      log.trace("Remove " + object);
    }
    int count = getDBCollection(object).find(getKey(object)).count();
    if (count != 1) {
      throw new IllegalStateException(count + " documents was removed instead of one, for " + object);
    }
    if (object instanceof UserImpl) {
      UserImpl user = (UserImpl) object;
      getMembershipDB().remove(new BasicDBObject("userName", user.getUserName()));
      getUserProfileDB().remove(new BasicDBObject("userName", user.getUserName())).getN();
      count = getUserDB().remove(new BasicDBObject("userName", user.getUserName())).getN();
    } else if (object instanceof GroupImpl) {
      GroupImpl group = (GroupImpl) object;

      int childCount = getGroupDB().find(new BasicDBObject("parentId", group.getId())).count();
      if (childCount > 0) {
        throw new RuntimeException("Can't remove a group that have children.");
      }

      getMembershipDB().remove(new BasicDBObject("groupId", group.getId()));
      count = getGroupDB().remove(new BasicDBObject("id", group.getId())).getN();
    } else if (object instanceof MembershipTypeImpl) {
      MembershipTypeImpl membershipType = (MembershipTypeImpl) object;
      getMembershipDB().remove(new BasicDBObject("membershipType", membershipType.getName()));
      count = getMembershipTypeDB().remove(new BasicDBObject("name", membershipType.getName())).getN();
    } else if (object instanceof MembershipImpl) {
      MembershipImpl membership = (MembershipImpl) object;
      DBObject requestScope = new BasicDBObject("id", membership.getId());
      count = getMembershipDB().remove(requestScope).getN();
    } else if (object instanceof UserProfileImpl) {
      UserProfileImpl userProfile = (UserProfileImpl) object;
      count = getUserProfileDB().remove(new BasicDBObject("userName", userProfile.getUserName())).getN();
    } else {
      throw new IllegalArgumentException("Object not supported" + object);
    }
    return true;
  }

  protected <T> MongoListAccess<T> findByRegex(Class<T> class1, String[] keys, String[] values) {
    DBObject dbObjectQuery = null;
    if (keys != null) {
      dbObjectQuery = new BasicDBObject();
      for (int i = 0; i < keys.length; i++) {
        Object value = values[i];
        if (values[i] != null) {
          value = Pattern.compile(values[i].replaceAll("\\*", "(.*)"), Pattern.CASE_INSENSITIVE);
        }
        dbObjectQuery.put(keys[i], value);
      }
    }
    return new MongoListAccess<T>(class1, getDB(class1), dbObjectQuery);
  }

  protected <T> MongoListAccess<T> find(Class<T> class1, String[] keys, String[] values) {
    DBObject dbObjectQuery = null;
    if (keys != null) {
      dbObjectQuery = new BasicDBObject();
      for (int i = 0; i < keys.length; i++) {
        dbObjectQuery.put(keys[i], values[i]);
      }
    }
    return new MongoListAccess<T>(class1, getDB(class1), dbObjectQuery);
  }

  protected ListAccess<User> findUsersByGroupId(String groupId) {
    DBObject dbObjectQuery = new BasicDBObject("groupId", groupId);
    List<?> users = getMembershipDB().distinct("userName", dbObjectQuery);
    return new UsersListAccess(users);
  }

  @SuppressWarnings("unchecked")
  protected <T> T findOne(Class<T> class1, String key, Object value) {
    DBObject dbObjectKey = new BasicDBObject(key, value);
    DBObject dbObject = getDB(class1).findOne(dbObjectKey);

    if (User.class.isAssignableFrom(class1)) {
      return (T) toUser(dbObject);
    } else if (Group.class.isAssignableFrom(class1)) {
      return (T) toGroup(dbObject);
    } else if (MembershipType.class.isAssignableFrom(class1)) {
      return (T) toMembershipType(dbObject);
    } else if (Membership.class.isAssignableFrom(class1)) {
      return (T) toMembership(dbObject);
    } else if (UserProfile.class.isAssignableFrom(class1)) {
      return (T) toUserProfile(dbObject);
    } else {
      throw new IllegalArgumentException("Object not supported" + class1);
    }
  }

  protected Object toObject(Class<?> class1, DBObject dbObject) {
    if (User.class.isAssignableFrom(class1)) {
      return toUser(dbObject);
    } else if (Group.class.isAssignableFrom(class1)) {
      return toGroup(dbObject);
    } else if (MembershipType.class.isAssignableFrom(class1)) {
      return toMembershipType(dbObject);
    } else if (Membership.class.isAssignableFrom(class1)) {
      return toMembership(dbObject);
    } else if (UserProfile.class.isAssignableFrom(class1)) {
      return toUserProfile(dbObject);
    } else {
      throw new IllegalArgumentException("Object not supported" + class1);
    }
  }

  private Object toUser(DBObject dbObject) {
    if (dbObject == null) {
      return null;
    }
    UserImpl userImpl = new UserImpl();
    userImpl.setUserName((String) dbObject.get("userName"));
    userImpl.setEmail((String) dbObject.get("email"));
    userImpl.setFirstName((String) dbObject.get("firstName"));
    userImpl.setFullName((String) dbObject.get("fullName"));
    userImpl.setId((String) dbObject.get("id"));
    userImpl.setLastLoginTime((Date) dbObject.get("lastLoginTime"));
    userImpl.setCreatedDate((Date) dbObject.get("createdDate"));
    userImpl.setLastName((String) dbObject.get("lastName"));
    userImpl.setOrganizationId((String) dbObject.get("organizationId"));
    userImpl.setPassword((String) dbObject.get("password"));

    return userImpl;
  }

  private Object toGroup(DBObject dbObject) {
    if (dbObject == null) {
      return null;
    }
    GroupImpl group = new GroupImpl();
    group.setDescription((String) dbObject.get("description"));
    group.setGroupName((String) dbObject.get("groupName"));
    group.setLabel((String) dbObject.get("label"));
    group.setParentId((String) dbObject.get("parentId"));

    return group;
  }

  private Object toMembership(DBObject dbObject) {
    if (dbObject == null) {
      return null;
    }
    MembershipImpl membership = new MembershipImpl();
    membership.setGroupId((String) dbObject.get("groupId"));
    membership.setMembershipType((String) dbObject.get("membershipType"));
    membership.setUserName((String) dbObject.get("userName"));

    return membership;
  }

  private Object toUserProfile(DBObject dbObject) {
    if (dbObject == null) {
      return null;
    }
    UserProfileImpl userProfile = new UserProfileImpl();
    userProfile.setUserName((String) dbObject.get("userName"));
    Map<?, ?> map = dbObject.toMap();
    for (Entry<?, ?> entry : map.entrySet()) {
      String key = changeKey(((String) entry.getKey()), false);
      if (!key.equals("userName") && !key.equals("_id")) {
        userProfile.getUserInfoMap().put(key, ((String) entry.getValue()));
      }
    }
    userProfile.getUserInfoMap().remove("userName");
    return userProfile;
  }

  private Object toMembershipType(DBObject dbObject) {
    if (dbObject == null) {
      return null;
    }
    MembershipTypeImpl membershipType = new MembershipTypeImpl();
    membershipType.setCreatedDate((Date) dbObject.get("createdDate"));
    membershipType.setDescription((String) dbObject.get("description"));
    membershipType.setModifiedDate((Date) dbObject.get("modifiedDate"));
    membershipType.setName((String) dbObject.get("name"));
    membershipType.setOwner((String) dbObject.get("owner"));

    return membershipType;
  }

  private void putFields(DBObject doc, Object object) {
    if (object instanceof UserProfile) {
      UserProfile userProfile = (UserProfile) object;
      doc.put("userName", userProfile.getUserName());

      // Add UserProfile attributes
      Map<String, String> map = userProfile.getUserInfoMap();
      for (Entry<String, String> entry : map.entrySet()) {
        String key = changeKey(entry.getKey(), true);
        doc.put(key, entry.getValue());
      }

      // Delete keys deleting from UserProfile Map
      Iterator<String> iterator = doc.keySet().iterator();
      while (iterator.hasNext()) {
        String key = changeKey(iterator.next(), false);
        if (!key.equals("userName") && !key.equals("_id") && !userProfile.getUserInfoMap().containsKey(key)) {
          iterator.remove();
        }
      }
    } else {
      BeanMap beanMap = new BeanMap(object);
      Iterator<?> iterator = beanMap.keyIterator();
      while (iterator.hasNext()) {
        String key = (String) iterator.next();
        if (!key.equals("class")) {
          doc.put(key, beanMap.get(key));
        }
      }
    }
  }

  private String changeKey(String key, boolean replace) {
    if (replace) {
      return key.replaceAll("\\.", "@");
    } else {
      return key.replaceAll("@", ".");
    }
  }

  private DBObject getKey(Object object) {
    if (object instanceof UserImpl) {
      UserImpl user = (UserImpl) object;
      return new BasicDBObject("userName", user.getUserName());
    } else if (object instanceof GroupImpl) {
      GroupImpl group = (GroupImpl) object;
      return new BasicDBObject("id", group.getId());
    } else if (object instanceof MembershipTypeImpl) {
      MembershipTypeImpl membershipType = (MembershipTypeImpl) object;
      return new BasicDBObject("name", membershipType.getName());
    } else if (object instanceof MembershipImpl) {
      MembershipImpl membership = (MembershipImpl) object;
      return new BasicDBObject("id", membership.getId());
    } else if (object instanceof UserProfileImpl) {
      UserProfileImpl userProfile = (UserProfileImpl) object;
      return new BasicDBObject("userName", userProfile.getUserName());
    } else {
      throw new IllegalArgumentException("Object not supported" + object.getClass());
    }
  }

  private DBCollection getDB(Class<?> class1) {
    if (User.class.isAssignableFrom(class1)) {
      return getUserDB();
    } else if (Group.class.isAssignableFrom(class1)) {
      return getGroupDB();
    } else if (MembershipType.class.isAssignableFrom(class1)) {
      return getMembershipTypeDB();
    } else if (Membership.class.isAssignableFrom(class1)) {
      return getMembershipDB();
    } else if (UserProfile.class.isAssignableFrom(class1)) {
      return getUserProfileDB();
    } else {
      throw new IllegalArgumentException("Object not supported" + class1);
    }
  }

  private DBCollection getDBCollection(Object object) {
    if (object instanceof UserImpl) {
      return getUserDB();
    } else if (object instanceof Group) {
      return getGroupDB();
    } else if (object instanceof MembershipType) {
      return getMembershipTypeDB();
    } else if (object instanceof Membership) {
      return getMembershipDB();
    } else if (object instanceof UserProfile) {
      return getUserProfileDB();
    } else {
      throw new IllegalArgumentException("Object not supported" + object);
    }
  }

  private DBCollection getUserDB() {
    return db.getCollection(USERS_COLLECTION);
  }

  private DBCollection getMembershipDB() {
    return db.getCollection(MEMBERSHIPS_COLLECTION);
  }

  private DBCollection getGroupDB() {
    return db.getCollection(GROUPS_COLLECTION);
  }

  private DBCollection getUserProfileDB() {
    return db.getCollection(PROFILES_COLLECTION);
  }

  private DBCollection getMembershipTypeDB() {
    return db.getCollection(MT_COLLECTION);
  }

  @Override
  public void stop() {
    
  }

  public class MongoListAccess<T> implements ListAccess<T>, Serializable {
    private static final long serialVersionUID = -3584357817470438575L;

    private final Class<T> class1;
    private final DBCollection db;
    private final DBObject dbObjectQuery;
    private final List<T> tmpArray = new ArrayList<T>();
    private List<T> allArray = null;
    private Integer count;

    public MongoListAccess(Class<T> class12, DBCollection db, DBObject dbObjectQuery) {
      this.class1 = class12;
      this.db = db;
      this.dbObjectQuery = dbObjectQuery;
      if (dbObjectQuery == null) {
        dbObjectQuery = new BasicDBObject();
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T[] load(int index, int length) throws Exception {
      if (index < 0) {
        throw new IllegalArgumentException("Incorrect index: " + index);
      }
      if (allArray != null) {
        return (T[]) allArray.subList(index, index + length).toArray((T[]) Array.newInstance(class1, 0));
      } else {
        DBCursor tmpCursor = db.find(dbObjectQuery).skip(index);
        int i = 0;
        tmpArray.clear();
        while (i++ < length) {
          tmpArray.add((T) toObject(class1, tmpCursor.next()));
        }
        return (T[]) tmpArray.toArray((T[]) Array.newInstance(class1, 0));
      }
    }

    @Override
    public int getSize() throws Exception {
      if (count == null) {
        count = db.find(dbObjectQuery).count();
      }
      return count;
    }

    @SuppressWarnings("unchecked")
    public Collection<T> getAll() {
      DBCursor cursor = db.find(dbObjectQuery);
      if (allArray == null) {
        allArray = new ArrayList<T>();
        while (cursor.hasNext()) {
          allArray.add((T) toObject(class1, cursor.next()));
        }
      }
      return allArray;
    }
  }

  public class UsersListAccess implements ListAccess<User>, Serializable {
    private static final long serialVersionUID = -3584357817470438575L;

    private final List<?> users;
    private final List<User> tmpArray = new ArrayList<User>();

    public UsersListAccess(List<?> users) {
      this.users = users;
    }

    @Override
    public User[] load(int index, int length) throws Exception {
      if (index < 0 || index + length > getSize()) {
        throw new IllegalArgumentException("Incorrect index: " + index + " and length: " + length + ", collection size = " + getSize());
      }
      tmpArray.clear();
      for (int j = index; j < index + length; j++) {
        tmpArray.add(findOne(User.class, "userName", users.get(j)));
      }
      return (User[]) tmpArray.toArray(new User[0]);
    }

    @Override
    public int getSize() throws Exception {
      return users.size();
    }
  }
}