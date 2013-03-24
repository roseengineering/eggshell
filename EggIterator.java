
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
import org.mozilla.javascript.annotations.JSFunction;
import org.mozilla.javascript.annotations.JSConstructor;

import java.util.Iterator;

/** An instance of this class encapsulates a Java Iterator.  For each
 *  interation of the Java iterator, a Tuple Writable is returned.
 */
public class EggIterator extends ScriptableObject 
{
  private static final long serialVersionUID = 6977601527878565246L;

  /* public static fields */

  /** Holds the Javascript interpreter */
  public static Script script;

  /* private fields */

  /** The Java iterator of the Tuple Writable object */
  private Iterator iterator;

  /* constructors */

  /** Called when first defined as a Javascript class
   *  @return This class
   */
  public EggIterator () {}

  /** The name of this Javascript class as a string
   *  @return  The string name of this class
   */
  public String getClassName() 
  { 
    return "EggIterator"; 
  }

  /** Called when a new object is instantiated from this class.
   *  The new object encapsulates the passed Java iterator object.
   *  @param o    The Java Iterator
   *  @return     The new object
   */
  @JSConstructor
  public EggIterator (Object o)
  {
    iterator = (Iterator) o;
  }

  /* public methods */

  /** Return whether there are more values in the iterator.
   *  @return The result of the hasNext() method applied to the
   *  encapsulated iterator.
   */
  @JSFunction
  public boolean hasNext () 
  { 
    return iterator.hasNext(); 
  }

  /** Return the next Tuple value in the iterator as a list of parameters.
   *  @return The next value of the iterator.
   */
  @JSFunction
  public Object next () 
  { 
    Tuple value = (Tuple) iterator.next();
    return value.toParams(script);
  }
}

 
