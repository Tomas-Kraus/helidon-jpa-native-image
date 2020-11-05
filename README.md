# Data Persistence with Helidon and Native Image

Last release of Helidon introduced support of JPA with Graal VM native image. This opened way to implement really lightweight microservices using full power of Java Persistence API.

Set of supported implementations contains:

    • Hibernate as JPA implementation
    • and set of JDBC drivers for following databases:
        ◦ H2
        ◦ MySQL
        ◦ MariaDB
        ◦ PostgreSQL

Now let‘s go trough the whole process of implementing sample persistance layer with simple REST application interface.
Hello world application will be modified to store people in database. Each person will have two attributes: nick and (real) name. Nick will be translatd to real name in returned „Hello World“ greeting.

## Step 1: Generate MP project

Execute following Maven command to generate sample microprofile project.
```
mvn -U archetype:generate -DinteractiveMode=false \
    -DarchetypeGroupId=io.helidon.archetypes \
    -DarchetypeArtifactId=helidon-quickstart-mp \
    -DarchetypeVersion=2.1.0 \
    -DgroupId=io.helidon.examples \
    -DartifactId=helidon-jpa-native-image \
    -Dpackage=io.helidon.examples.jpa.ni
```
This command will generate helidon-jpa-native-image directory containing files:
```
helidon-jpa-native-image/.dockerignore
helidon-jpa-native-image/app.yaml
helidon-jpa-native-image/Dockerfile
helidon-jpa-native-image/Dockerfile.jlink
helidon-jpa-native-image/Dockerfile.native
helidon-jpa-native-image/pom.xml
helidon-jpa-native-image/README.md
helidon-jpa-native-image/src/main/java/io/helidon/examples/jpa/ni/GreetingProvider.java
helidon-jpa-native-image/src/main/java/io/helidon/examples/jpa/ni/GreetResource.java
helidon-jpa-native-image/src/main/java/io/helidon/examples/jpa/ni/package-info.java
helidon-jpa-native-image/src/main/resources/logging.properties
helidon-jpa-native-image/src/main/resources/META-INF/beans.xml
helidon-jpa-native-image/src/main/resources/META-INF/microprofile-config.properties
helidon-jpa-native-image/src/main/resources/META-INF/native-image/reflect-config.json
helidon-jpa-native-image/src/test/java/io/helidon/examples/jpa/ni/MainTest.java
```
To verify that initial application works, simply build it:
```
cd helidon-jpa-native-image
mvn clean install
java -jar target/helidon-jpa-native-image.jar
```
This will execute Java MP application with REST interface. Now execute web client to verify whether it works and check response:
```
curl -X GET http://localhost:8080/greet
{"message":"Hello World!"}
```

## Step 2: Add JPA dependencies

JPA implementation require Hibernate as JPA provirer and choosen database JDBC driver. Database choosen for our sample is MySQL. Hibernate is transient dependency of Helidon integration module but MySQL database driver bust be added to dependencies too.
Required Helidon integration modules dependencies:
```
        <dependency>
            <groupId>io.helidon.integrations.cdi</groupId>
            <artifactId>helidon-integrations-cdi-hibernate</artifactId>
        </dependency>
        <dependency>
            <groupId>io.helidon.integrations.cdi</groupId>
            <artifactId>helidon-integrations-cdi-jta</artifactId>
        </dependency>
        <dependency>
            <groupId>io.helidon.integrations.cdi</groupId>
            <artifactId>helidon-integrations-cdi-datasource-hikaricp</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.helidon.integrations.cdi</groupId>
            <artifactId>helidon-integrations-cdi-jpa</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.helidon.integrations.db</groupId>
            <artifactId>mysql</artifactId>
            <version>${project.parent.version}</version>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <scope>runtime</scope>
        </dependency>
```

## Step 3: Configure JPA Persistence Unit

Persistence Unit configuration file contains information required for Hibernate to connect to the database and work with data using ORM. To add this configuration file, create
`src/main/resources/META-INF/persistence.xml`
under project directory with following content:
```
<?xml version="1.0" encoding="UTF-8"?>

<persistence version="2.2"
             xmlns="http://xmlns.jcp.org/xml/ns/persistence"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/persistence
                                 http://xmlns.jcp.org/xml/ns/persistence/persistence_2_2.xsd">
    <persistence-unit name="hello" transaction-type="JTA">
        <properties>
            <property name="hibernate.dialect" value="org.hibernate.dialect.MySQL5Dialect"/>
            <property name="hibernate.hbm2ddl.auto" value="none"/>
            <property name="hibernate.temp.use_jdbc_metadata_defaults" value="false"/>
            <property name="show_sql" value="true"/>
        </properties>
    </persistence-unit>
</persistence>
```
Next, add following lines to the
`src/main/resources/META-INF/microprofile-config.properties`
file:
```
javax.sql.DataSource.test.dataSource.url=jdbc:mysql://localhost/helloworld?useSSL=false&allowPublicKeyRetrieval=true
javax.sql.DataSource.test.dataSource.user=user
javax.sql.DataSource.test.dataSource.password=p4ssw0rd
javax.sql.DataSource.test.dataSourceClassName=com.mysql.cj.jdbc.MysqlDataSource
```

## Step 4: Run and initialize the database

The most easy way to get runing MySQL with required credentials and database is Docker image. Execute following command:
```
docker run --name mysql -e MYSQL_ROOT_PASSWORD=r00tp4ssw0rd \
    -e MYSQL_USER=user -e MYSQL_PASSWORD=p4ssw0rd \
    -e MYSQL_DATABASE=helloworld -p 3306:3306 mysql:8
```
This will run MySQL 8 database with configured credentials and database named helloworld. No database tables are created during server startup. This must be done manually or using JPA schgema generation feature. To use  JPA schgema generation feature, add following property to the properties section of the `src/main/resources/META-INF/persistence.xml` file:
```
<property name="javax.persistence.schema-generation.database.action" value="drop-and-create"/>
```
Database schema will be reset with every start of this example until value of this property is changed to "none".

# Step 5: Data model

Person information, his nick and name, are mapped to Person Entity. It must be added to the `src/main/resources/META-INF/persistence.xml` file as:
```
<class>io.helidon.examples.jpa.ni.Person</class>

Person entity code:
package io.helidon.examples.jpa.ni;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

/**
 * Person JPA entity.
 */
@Entity
public class Person {

    @Id
    @Column(columnDefinition = "VARCHAR(32)", nullable = false)
    private String nick;

    private String name;

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
```

## Step 6: Resource modification

JAX-RS resource class GreetResource needs several changes to work with JPA and database.

### Entity Manager

EntityManager instance must be added to allow JPA code calls. It‘s just another GreetResource class instance private attribute:
```
    @PersistenceContext(unitName = "hello")
    private EntityManager em;
```

### JAX-RS request methods

New JAX-RS POST method is required to allow creation of new Person records:
```
    /**
     * Store a new person for greetings.
     * @param jsonPerson JSON object with person to store
     * @return HTTPrequest result
     */
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Transactional
    @POST
    public Response createPerson(JsonObject jsonPerson) {
        if (jsonPerson == null || !jsonPerson.containsKey("nick")
                || !jsonPerson.containsKey("name")) {
            return Response
                    .status(Response.Status.fromStatusCode(422))
                    .build();
        }
        String nick = jsonPerson.getString("nick");
        String name = jsonPerson.getString("name");
        Person person = new Person();
        person.setNick(nick);
        person.setName(name);
        JsonObjectBuilder entityBuilder = JSON.createObjectBuilder()
                .add("nick", nick)
                .add("name", name);
        try {
            em.persist(person);
            return Response.status(Response.Status.OK)
                    .entity(entityBuilder.build())
                    .build();
        } catch (PersistenceException pe) {
            pe.printStackTrace();
            JsonObject entity = entityBuilder
                    .add("error", pe.getMessage())
                    .build();
            return Response
                    .status(Response.Status.CONFLICT)
                    .entity(entity)
                    .build();
        }
    }
```
Also getMessage method needs to be modified to retrieve name mapped to nick from database request:
```
    /**
     * Return a greeting message using the name that was provided.
     *
     * @param nick the nick to greet
     * @return {@link JsonObject}
     */
    @SuppressWarnings("checkstyle:designforextension")
    @Path("/{nick}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Transactional
    public Response getMessage(@PathParam("nick") String nick) {
        Person entity = em.find(Person.class, nick);
        JsonObjectBuilder entityBuilder = JSON.createObjectBuilder()
                .add("nick", nick);
        if (entity == null) {
            JsonObject responseEntity = entityBuilder
                    .add("error", String.format("Nick %s was not found", nick))
                    .build();
            return Response
                    .status(Response.Status.CONFLICT)
                    .entity(responseEntity)
                    .build();
        }
        JsonObject responseEntity = createResponse(entity.getName());
        return Response
                .status(Response.Status.OK)
                .entity(responseEntity)
                .build();
    }
```

## Step 7: Native image

Byte code for JPA must be generated at compile time and `hibernate.bytecode.provider` must be disabled. This must be configured in a new file `src/main/resources/hibernate.properties`:
```
hibernate.bytecode.provider=none
```
Additional dependency must be added to pom.xml:
```
        <dependency>
            <groupId>io.helidon.integrations.cdi</groupId>
            <artifactId>helidon-integrations-cdi-jta-weld</artifactId>
        </dependency>
```

## Step 8: Tests modification

Last thing to modify is jUnit test. Current test will fail after all the modifications done to the project.
Modified MainTest class is here:
```
package io.helidon.examples.jpa.ni;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.spi.CDI;
import javax.json.Json;
import javax.json.JsonObject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.helidon.microprofile.server.Server;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

class MainTest {

    private static Server server;
    private static String serverUrl;

    @BeforeAll
    public static void startTheServer() throws Exception {
        server = Server.create().start();
        serverUrl = "http://localhost:" + server.port();
    }

    @Test
    @Order(1)
    void testAddPerson() {
        Client client = ClientBuilder.newClient();

        JsonObject jsonObject = Json.createObjectBuilder()
                .add("nick", "joe")
                .add("name", "Joe Brown")
                .build();
        Response r = client
                .target(serverUrl)
                .path("greet")
                .request()
                .post(Entity.entity(jsonObject.toString(), MediaType.APPLICATION_JSON));
        Assertions.assertEquals(200, r.getStatus(), "POST person status code");

        jsonObject = Json.createObjectBuilder()
                .add("nick", "jose")
                .add("name", "Jose Carreras")
                .build();
        r = client
                .target(serverUrl)
                .path("greet")
                .request()
                .post(Entity.entity(jsonObject.toString(), MediaType.APPLICATION_JSON));
        Assertions.assertEquals(200, r.getStatus(), "POST person status code");
    }

    @Test
    @Order(1)
    void testHelloWorld() {
        Client client = ClientBuilder.newClient();

        JsonObject jsonObject = client
                .target(serverUrl)
                .path("greet")
                .request()
                .get(JsonObject.class);
        Assertions.assertEquals("Hello World!", jsonObject.getString("message"),
                "default message");

        jsonObject = client
                .target(serverUrl)
                .path("greet/joe")
                .request()
                .get(JsonObject.class);
        Assertions.assertEquals("Hello Joe Brown!", jsonObject.getString("message"),
                "hello Joe message");

        Response r = client
                .target(serverUrl)
                .path("greet/greeting")
                .request()
                .put(Entity.entity("{\"greeting\" : \"Hola\"}", MediaType.APPLICATION_JSON));
        Assertions.assertEquals(204, r.getStatus(), "PUT status code");

        jsonObject = client
                .target(serverUrl)
                .path("greet/Jose")
                .request()
                .get(JsonObject.class);
        Assertions.assertEquals("Hola Jose Carreras!", jsonObject.getString("message"),
                "hola Jose message");

        r = client
                .target(serverUrl)
                .path("metrics")
                .request()
                .get();
        Assertions.assertEquals(200, r.getStatus(), "GET metrics status code");

        r = client
                .target(serverUrl)
                .path("health")
                .request()
                .get();
        Assertions.assertEquals(200, r.getStatus(), "GET health status code");
    }

    @AfterAll
    static void destroyClass() {
        CDI<Object> current = CDI.current();
        ((SeContainer) current).close();
    }
}
```

## Step 9: Building and testing

First make sure that `graalvm-ce-java11-20.2.0` including native image support is installed in your system.
To build modified project as native image, simply execute:
```
mvn clean install -Pnative-image
```
When build finishes successfully, make sure that database server is running and run:
```
target/helidon-jpa-native-image
```
Now verify simple greeting:
```
curl -X GET http://localhost:8080/greet
```
returned response should be:
```
{"message":"Hello World!"}
```
Add new person to the database
```
curl -X POST -H "Content-Type: application/json" \
     -d '{"nick":"bob","name":"Bobby Fischer"}' \
     http://localhost:8080/greet
```
returned response should be:
```
{"nick":"bob","name":"Bobby Fischer"}
```
Greet new person:
```
curl -X GET http://localhost:8080/greet/bob
```
returned response should be:
```
{"message":"Hello Bobby Fischer!"}
```
