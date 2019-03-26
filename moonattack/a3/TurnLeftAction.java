package a3;

import ray.input.action.AbstractInputAction;
import ray.rage.game.*;
import net.java.games.input.Event;
import ray.rage.scene.Node;
import ray.rml.*;

public class TurnLeftAction extends AbstractInputAction {
   private Node tank;
   private Angle turnangle;
   private ProtocolClient protClient;
	
	public TurnLeftAction(Node t, Angle a, ProtocolClient p) {
		tank = t;
      turnangle = a;
      protClient = p;
	}
	
	public void performAction(float time, Event event) {
		tank.yaw(turnangle);
      protClient.sendMoveMessage((Vector3f)tank.getWorldPosition());
	}
}