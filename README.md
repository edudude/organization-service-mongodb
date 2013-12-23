organization-service-mongodb
============================

eXo Organization Service - MongoDB Services Impl

The eXo platform organization has 5 main components: user , user profile, group, membership type and membership.

 * |-----------------|    |---------------|  |---------------|    |-----------------|
 * |                 |    |               |  |               |    |                 |
 * |                 |    |               |  |               |==| |                 | 
 * |  USER PROFILE   |<=> |     USER      |  |     GROUP     |  | | MEMBERSHIP TYPE |
 * |                 |    |               |  |               |<=| |                 |
 * |                 |    |               |  |               |    |                 |
 * |---------------- |    |---------------|  |---------------|    |-----------------|
 *                              |                    /           /
 *                              |                   /           /
 *                              |                  /           /
 *                              |                 /           /
 *                              |                /           /
 *                              |               /           /
 *                        |----------------------------------|
 *                        |                                  |
 *                        |            MEMBERSHIP            |
 *                        |     (The membership hold the     |
 *                        |     relation of the user, group  |
 *                        |     and membership type)         |
 *                        |                                  |
 *                        |--------------------------------- |

In eXo Platform 4.0, we find several implementations of Organization Service:
* Active Directory
* LDAP
* SQL Relational Database

This implementation intend to allow storing the eXo Organization Model on a NoSQL System.

Build
=====

* Run mvn clean install

Installation
============

* Unzip packaging/target/organization-service-mongodb.zip under your eXo Platform 4 installation.

Configuration
=============

* Open configuration.properties file
* Add those entries:
** mongodb.host=HOST (default is localhost)
** mongodb.port=PORT (default is 27017)
** mongodb.db.name=DB_NAME (default is organization-plf)
