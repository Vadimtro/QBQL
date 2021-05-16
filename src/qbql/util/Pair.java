package qbql.util;

public class Pair<X, Y> {
  public Pair(final X first, final Y second) {
    super();
    this.first = first;
    this.second = second;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Pair<?, ?> other = (Pair<?, ?>) obj;
    if (first == null) {
      if (other.first != null) {
        return false;
      }
    } else if (!first.equals(other.first)) {
      return false;
    }
    if (second == null) {
      if (other.second != null) {
        return false;
      }
    } else if (!second.equals(other.second)) {
      return false;
    }
    return true;
  }

  public X first() {
    return first;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (first == null ? 0 : first.hashCode());
    result = prime * result + (second == null ? 0 : second.hashCode());
    return result;
  }

  public Y second() {
    return second;
  }

  @Override
  public String toString() {
    return "{" + first + ", " + second + "}";
  }

  public static <X, Y> Pair<X, Y> pair(final X first, final Y second) {
    return new Pair<X, Y>(first, second);
  }

  private final X first;

  private final Y second;
}
