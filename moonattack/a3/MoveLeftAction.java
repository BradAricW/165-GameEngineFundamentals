package a3;

import ray.input.action.AbstractInputAction;
import ray.rage.game.*;
import net.java.games.input.Event;
import ray.rage.scene.Node;
import ray.rml.*;

public class MoveLeftAction extends AbstractInputAction {
   private Node tank;
   private MyGame game;
   private ProtocolClient protClient;
	
	public MoveLeftAction(Node t, MyGame g, ProtocolClient p) {
		tank = t;
      game = g;
      protClient = p;
	}
	
	public void performAction(float time, Event event) {
		tank.moveLeft(0.1f);
      game.updateVerticalPosition();
      protClient.sendMoveMessage((Vector3f)tank.getWorldPosition());
	}
}