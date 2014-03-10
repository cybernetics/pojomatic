package org.pojomatic.internal;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.pojomatic.Pojomator;
import org.pojomatic.PropertyElement;

public abstract class BasePojomator<T> implements Pojomator<T> {
  private final Class<?> pojoClass;
  private final ClassProperties classProperties;

  protected BasePojomator(Class<?> pojoClass, ClassProperties classProperties) {
    this.pojoClass = pojoClass;
    this.classProperties = classProperties;
  }

  @Override
  public boolean isCompatibleForEquality(Class<?> otherClass) {
    return classProperties.isCompatibleForEquals(otherClass);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("Pojomator for ").append(pojoClass.getName()).append(" with equals properties ");
    propertiesList(builder, classProperties.getEqualsProperties());
    builder.append(", hashCodeProperties ");
    propertiesList(builder, classProperties.getHashCodeProperties());
    builder.append(", and toStringProperties ");
    propertiesList(builder, classProperties.getToStringProperties());
    return builder.toString();
  }

  private void propertiesList(StringBuilder builder, final Iterable<PropertyElement> properties) {
    builder.append("{");
    boolean firstElement = true;
    for (PropertyElement prop: properties) {
      if (!firstElement) {
        builder.append(",");
      }
      else {
        firstElement = false;
      }
      builder.append(prop.getName());
    }
    builder.append("}");
  }

  /**
   * Construct a call site for a property accessor. Because {@code pojoClass} might not be a public class, the
   * parameter in {@code methodType} cannot be {@code pojoClass}, but instead must be just {@code Object.class}. The
   * {@code pojoClass} parameter will be stored as static field in the Pojomator class, and passed in from it's
   * bootstrap method.
   * @param caller A Lookup from the original call site.
   * @param name the name of the dynamic method. This should either be "field_&lt;fieldName&gt;" or "method_&lt;methodName&gt;".
   * @param methodType the type of the dynamic method; the return type should be the type of the aforementioned field
   *   or method
   * @param pojomatorClass the type of the pojomator class
   * @return a CallSite which invokes the method or gets the field value.
   * @throws NoSuchMethodException
   * @throws NoSuchFieldException
   * @throws IllegalAccessException
   */
  protected static CallSite bootstrap(
      MethodHandles.Lookup caller, String name, MethodType methodType, Class<?> pojomatorClass)
      throws NoSuchMethodException, NoSuchFieldException, IllegalAccessException {
    return new ConstantCallSite(
      MethodHandles.explicitCastArguments(
        getTypedMethod(caller, name, pojomatorClass),
        MethodType.methodType(methodType.returnType(), Object.class)));
  }

  /**
   * Compare two values with a static type a proper sub-type of Object for equality. In particular, it is assumed that
   * neither value is an array.
   * @param instanceValue the first value to compare
   * @param otherValue the second value to compare
   * @return true if {@code instanceValue} and {@code otherValue} are equal to each other.
   */
  protected static boolean areNonArrayValuesEqual(Object instanceValue, Object otherValue) {
    if (instanceValue == otherValue) {
      return true;
    }
    if (instanceValue == null || otherValue == null) {
      return false;
    }
    return instanceValue.equals(otherValue);
  }

  /**
   * Compare two values of static type Object for equality. If both values are arrays of the same primitive component
   * type, or if both values are arrays of non-primitive component type, then the appropriate {@code equals} method
   * on {@link Arrays} is used to determine equality.
   * @param instanceValue the first value to compare
   * @param otherValue the second value to compare
   * @param deepArray whether to do a deep array check for Object arrays
   * @return true if {@code instanceValue} and {@code otherValue} are equal to each other.
   */
  protected static boolean areObjectValuesEqual(Object instanceValue, Object otherValue, boolean deepArray) {
    if (instanceValue == otherValue) {
      return true;
    }
    if (instanceValue == null || otherValue == null) {
      return false;
    }
    else {
      if (!instanceValue.getClass().isArray()) {
        if (!instanceValue.equals(otherValue)) {
          return false;
        }
      }
      else {
        if (!otherValue.getClass().isArray()) {
          return false;
        }
        final Class<?> instanceComponentClass = instanceValue.getClass().getComponentType();
        if (!instanceComponentClass.isPrimitive()) {
          if (otherValue.getClass().getComponentType().isPrimitive()) {
            return false;
          }
          if (deepArray) {
            if (!Arrays.deepEquals((Object[]) instanceValue, (Object[]) otherValue)) {
              return false;
            }
          }
          else {
            if (!Arrays.equals((Object[]) instanceValue, (Object[]) otherValue)) {
              return false;
            }
          }
        }
        else { // instanceComponentClass is primitive
          if (otherValue.getClass().getComponentType() != instanceComponentClass) {
            return false;
          }

          if (Boolean.TYPE == instanceComponentClass) {
            if (!Arrays.equals((boolean[]) instanceValue, (boolean[]) otherValue)) {
              return false;
            }
          }
          else if (Byte.TYPE == instanceComponentClass) {
            if (!Arrays.equals((byte[]) instanceValue, (byte[]) otherValue)) {
              return false;
            }
          }
          else if (Character.TYPE == instanceComponentClass) {
            if (!Arrays.equals((char[]) instanceValue, (char[]) otherValue)) {
              return false;
            }
          }
          else if (Short.TYPE == instanceComponentClass) {
            if (!Arrays.equals((short[]) instanceValue, (short[]) otherValue)) {
              return false;
            }
          }
          else if (Integer.TYPE == instanceComponentClass) {
            if (!Arrays.equals((int[]) instanceValue, (int[]) otherValue)) {
              return false;
            }
          }
          else if (Long.TYPE == instanceComponentClass) {
            if (!Arrays.equals((long[]) instanceValue, (long[]) otherValue)) {
              return false;
            }
          }
          else if (Float.TYPE == instanceComponentClass) {
            if (!Arrays.equals((float[]) instanceValue, (float[]) otherValue)) {
              return false;
            }
          }
          else if (Double.TYPE == instanceComponentClass) {
            if (!Arrays.equals((double[]) instanceValue, (double[]) otherValue)) {
              return false;
            }
          }
          else {
            // should NEVER happen
            throw new IllegalStateException(
              "unknown primitive type " + instanceComponentClass.getName());
          }
        }
      }
    }
    return true;
  }


  /**
   * Given an object which is of array type, compute it's hashCode by calling the appropriate signature of
   * {@link Arrays}{@code .hashCode()}
   * @param array
   * @param deepArray whether to do a deep hashCode for Object arrays.
   * @return the hashCode
   */
  protected static int arrayHashCode(Object array, boolean deepArray) {
    Class<?> componentType = array.getClass().getComponentType();
    if (! componentType.isPrimitive()) {
      return deepArray ? Arrays.deepHashCode((Object[]) array) : Arrays.hashCode((Object[]) array);
    }
    if (componentType == boolean.class) {
      return Arrays.hashCode((boolean[]) array);
    }
    if (componentType == byte.class) {
      return Arrays.hashCode((byte[]) array);
    }
    if (componentType == char.class) {
      return Arrays.hashCode((char[]) array);
    }
    if (componentType == short.class) {
      return Arrays.hashCode((short[]) array);
    }
    if (componentType == int.class) {
      return Arrays.hashCode((int[]) array);
    }
    if (componentType == long.class) {
      return Arrays.hashCode((long[]) array);
    }
    if (componentType == float.class) {
      return Arrays.hashCode((float[]) array);
    }
    if (componentType == double.class) {
      return Arrays.hashCode((double[]) array);
    }
    throw new IllegalStateException("unknown primitive type " + componentType.getName());
  }

  protected static <T> T checkNotNull(T reference) {
    if (reference == null) {
      throw new NullPointerException();
    }
    return reference;
  }

  protected static <T> T checkNotNull(T reference, String message) {
    if (reference == null) {
      throw new NullPointerException(message);
    }
    return reference;
  }

  protected static void checkNotNullPop(Object reference) {
    if (reference == null) {
      throw new NullPointerException();
    }
  }

  protected void checkCompatibleForEquality(T instance, String label) {
    if (!isCompatibleForEquality(instance.getClass())) {
      throw new IllegalArgumentException(
        label + " has type " + instance.getClass().getName()
        + " which is not compatible for equality with " + pojoClass.getName());
    }
  }

  /**
   * Get a method handle to access a field or invoke a no-arg method.
   * @param caller A Lookup from the original call site.
   * @param name the name of the dynamic method. This should be of the form "get_xxx", where "element_xxx" will be a
   * static field containing a {@link PropertyElement} instance referring to the property to be accessed.
   * @param pojomatorClass the type of the pojomator class
   * @return the MethodHandle
   * @throws NoSuchFieldException
   * @throws IllegalAccessException
   * @throws NoSuchMethodException
   */
  private static MethodHandle getTypedMethod(MethodHandles.Lookup caller, String name, Class<?> pojomatorClass)
    throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException {
    String elementName = "element_" + name.substring(4);
    Field elementField = pojomatorClass.getDeclaredField(elementName);
    elementField.setAccessible(true);
    PropertyElement property = (PropertyElement) elementField.get(null);
    AnnotatedElement element = property.getElement();
    if (element instanceof Field) {
      Field field = (Field) element;
      field.setAccessible(true);
      return caller.unreflectGetter(field);
    }
    else if (element instanceof Method) {
      Method method = (Method) element;
      method.setAccessible(true);
      return caller.unreflect(method);
    }
    else {
      throw new IllegalArgumentException("Cannot handle element of type " + element.getClass().getName());
    }

  }

}
