package a3;

import ray.input.action.AbstractInputAction;
import ray.rage.game.*;
import net.java.games.input.Event;
import ray.rage.scene.Node;
import ray.rml.*;

public class MoveBackwardAction extends AbstractInputAction {
   private Node tank;
   private MyGame game;
   private ProtocolClient protClient;
	
	public MoveBackwardAction(Node t, MyGame g, ProtocolClient p) {
		tank = t;
      game = g;
      protClient = p;
	}
	
	public void performAction(float time, Event event) {
		tank.moveBackward(0.1f);
      game.updateVerticalPosition();
      protClient.sendMoveMessage((Vector3f)tank.getWorldPosition());
	}
}