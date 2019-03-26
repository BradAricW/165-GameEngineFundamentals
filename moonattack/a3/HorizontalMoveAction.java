package a3;

import ray.input.action.AbstractInputAction;
import ray.rage.scene.Node;
import a3.MyGame;
import net.java.games.input.Event;
import ray.rml.*;

public class HorizontalMoveAction extends AbstractInputAction
{
   private Node tank;
   private MyGame game;
   private ProtocolClient protClient;
   
   public HorizontalMoveAction(Node t, MyGame g, ProtocolClient p)
   {
      tank = t;
      game = g;
      protClient = p;
   }
   
   public void performAction(float time, Event e)
   {
      if(e.getValue() < -0.5)
      {
         tank.moveRight(0.1f);
         protClient.sendMoveMessage((Vector3f)tank.getWorldPosition());
      }
      else if(e.getValue() > 0.5)
      {
         tank.moveLeft(0.1f);
         protClient.sendMoveMessage((Vector3f)tank.getWorldPosition());
      }
      else
      {
         
      }
      game.updateVerticalPosition();
   }
   
}