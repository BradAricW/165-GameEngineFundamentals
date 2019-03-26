package a3;

import java.util.Vector;
import ray.rage.scene.*;

public class ShipCollection {
	private Vector<SceneNode> theCollection;
	
	public ShipCollection() {
		theCollection = new Vector<SceneNode>();
	}
	
	public void add(SceneNode newShip) {
		theCollection.addElement(newShip);
	}
	
	public int getSize() {
		int s = theCollection.size();
		return s;
	}
	
	public int indexOf(SceneNode p) {
		Object x = (Object)p;
		return theCollection.indexOf(x);
	}
	
	public Object elementAt(int i) {
		Object o = theCollection.elementAt(i);
		return o;
	}
	
	public void remove(int i) {
		theCollection.removeElementAt(i);
	}
	
	public void destroy() {
		theCollection.removeAllElements();
	}
	
	public IIterator getIterator() {
		return new ShipIterator();
	}
	
	private class ShipIterator implements IIterator {
		
		private int currElementIndex;
		
		public ShipIterator() {
			currElementIndex = -1;
		}
		
		public boolean hasNext() {
			if (theCollection.size() <= 0)
				return false;
			if (currElementIndex == theCollection.size() -1)
				return false;
			return true;
		}
		
		public SceneNode getNext() {
			currElementIndex++;
			return(theCollection.elementAt(currElementIndex));
		}

	}
	
}