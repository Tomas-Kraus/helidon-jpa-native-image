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
