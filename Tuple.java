
/*
This file is part of Eggshell.
Copyright 2013 George Magiros

Eggshell is free software: you can redistribute it and/or modify it
under the terms of the GNU General Public License as published by the
Free Software Foundation, either version 3 of the License, or (at
your option) any later version.

Eggshell is distributed in the hope that it will be useful, but WITHOUT
ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
License for more details.

You should have received a copy of the GNU General Public License
along with Eggshell.  If not, see <http://www.gnu.org/licenses/>.
*/

import org.mozilla.javascript.*;

import java.io.IOException;

// custom writable
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.Writable;
import java.util.ArrayList;
import java.io.DataOutput;
import java.io.DataInput;


/** Class provides a custom writable implementation of a Tuple.  The
 *  class implements the writable comparable interface so it can be
 *  used for both key and value writables.
 */
class Tuple
  implements WritableComparable<Tuple> 
{
  /* private fields */

  /** The tags that represent the possible data types of the elements
      of the Tuple.  The tag values are taken from the BSON.
      specification */
  private static final byte E00 = 0;
  private static final byte DOUBLE = 1;
  private static final byte STRING = 2;
  private static final byte BOOLEAN = 8;

  /** The internal data structure that holds the tuple */
  public ArrayList<Object> tuple;

  /* constructors */

  /** Create a new Tuple object with no elements initially.
   *  @return The new object
   */
  public Tuple() 
  { 
    tuple = new ArrayList<Object>();
  }

  /* overrided public methods */

  /** Implements the serialization method.
   *  @param out    The output data stream
   */
  @Override
  public void write (DataOutput out) 
    throws IOException 
  {
    for (int i = 0; i < tuple.size(); i++) {  // for each element
      Object ret = tuple.get(i);
      if (ret instanceof Double) {
        out.writeByte(DOUBLE);
        out.writeDouble((Double) ret);
      } else if (ret instanceof Boolean) {
        out.writeByte(BOOLEAN);
        out.writeBoolean((Boolean) ret);
      } else if (ret instanceof String) {
        out.writeByte(STRING);
        out.writeUTF((String) ret);
      } else {
        out.writeByte(STRING);
        out.writeUTF(ret.toString());
      }
    }
    out.writeByte(E00);  // marks end of tuple
  }
  
  /** Implements the deserialization method.
   *  @param in    The input data stream
   */
  @Override
  public void readFields (DataInput in) 
    throws IOException 
  {
    byte key;
    tuple.clear();
    while ((key = in.readByte()) != E00) {
      if (key == DOUBLE) tuple.add(in.readDouble());
      if (key == STRING) tuple.add(in.readUTF());
      if (key == BOOLEAN) tuple.add(in.readBoolean());
    }
  }

  /** Convert the tuple to a string using a comma separated format.
   *  @return The comma separated string
   */
  @Override
  public String toString ()
  {
    String buf = "";
    for (int i = 0; i < tuple.size(); i++) {
      Object ret = tuple.get(i);
      if (i > 0) buf += ",";         // add a comma between elements
      if (ret instanceof Double) {
        Double d = (Double) ret;
        if (d.longValue() == d) buf += d.longValue(); // is it a long
        else buf += d;
      } else {
        buf += ret;
      }
    }
    return buf;
  }

  /** Implements the Writable comparison operation.
   *  @return The comparison result
   */
  @Override
  public int compareTo (Tuple that) 
  {
    int x = this.tuple.size();
    int y = that.tuple.size();

    for (int i = 0; i < x && i < y; i++) {
      int cmp;
      Object a = this.tuple.get(i);
      Object b = that.tuple.get(i);

      if (a instanceof Double && b instanceof Double)  // both doubles
        cmp = ((Double) a).compareTo((Double) b); 
      else if (a instanceof String && b instanceof String)  // both strings
        cmp = ((String) a).compareTo((String) b); 
      else if (a instanceof Boolean && b instanceof Boolean)  // both booleans
        cmp = ((Boolean) a).compareTo((Boolean) b); 
      else                                             // if different classes
        cmp = a.getClass().toString().compareTo(a.getClass().toString());
      if (cmp != 0) return cmp;
    }
    return (x < y ? -1 : (x == y ? 0 : 1));
  }

  /** Hash the tuple and returns the code.
   *  @return The hash code
   */
  @Override
  public int hashCode ()
  {
    return tuple.hashCode();
  }

  /* public methods */

  /** Reset the tuple to zero elements.
   */
  public void clear ()
  {
    tuple.clear();
  }
  
  /** Add an element of type Double to the tuple.
   *  @param d    The element
   */
  public void add (Double d)
  {
    tuple.add(d);
  }
  
  /** Add an element of type String to the tuple.
   *  @param s    The element
   */
  public void add (String s)
  {
    tuple.add(s);
  }
  
  /** Add an element of type Boolean to the tuple.
   *  @param b    The element
   */
  public void add (Boolean b)
  {
    tuple.add(b);
  }

  /** Add a supported Javascript object to the tuple.
   *  @param o    The object
   */
  public void add (Object o)
  {
    // first unwrap any native java objects to the base type
    if (o instanceof NativeJavaObject) o = ((NativeJavaObject) o).unwrap();

    // supported objects
    if (o instanceof String) add((String) o);
    if (o instanceof Double) add((Double) o);
    if (o instanceof Integer) add(new Double((Integer) o));
    if (o instanceof Boolean) add((Boolean) o);
  }

  /** Return the tuple as a list of parameters.  If the tuple has one
   *  element return that element, otherwise return a Javascript array
   *  representation of the tuple.
   *  @return        A list of parameters
   */
  public Object toParams(Script script)
  {
    Object[] array = tuple.toArray();
    Object o;
    if (array.length == 0) o = null;
    else if (array.length == 1) o = array[0];  // if only one element
    else {
      o = script.newArray(array);
    }
    return o;
  }
}

