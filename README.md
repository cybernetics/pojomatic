Pojomatic
=========

Pojomatic provides configurable implementations of the `equals(Object)`, `hashCode()` and `toString()` methods
inherited from `java.lang.Object`.

For example, the following bean has been "pojomated":

    import org.pojomatic.Pojomatic;
    import org.pojomatic.annotations.AutoProperty;

    @AutoProperty
    public class Person {
      private final String firstName;
      private final String lastName;
      private final int age;

      public Person(String firstName, String lastName, int age) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
      }

      public String getFirstName() { return this.firstName; }
      public String getLastName() { return this.lastName; }
      public int getAge() { return this.age; }

      @Override public boolean equals(Object o) {
        return Pojomatic.equals(this, o);
      }

      @Override public int hashCode() {
        return Pojomatic.hashCode(this);
      }

      @Override public String toString() {
        return Pojomatic.toString(this);
      }
    }

The above class implements equals and hashCode methods following the best practices outlined in Josh Bloch's Efective Java. Moreover, running

    System.out.println(new Person("John", "Doe", 32).toString());

will result in the following output:

    Person{firstName: {John}, lastName: {Doe}, age: {32}}

For more information and examples, see the [Pojomatic site](http://www.pojomatic.org)
