package a3;

import ray.input.action.AbstractInputAction;
import ray.rage.scene.Node;
import a3.MyGame;
import net.java.games.input.Event;

import ray.input.action.AbstractInputAction;
import ray.rage.game.*;
import net.java.games.input.Event;
import ray.rage.scene.*;
import ray.rml.*;

public class HorizontalCamAction extends AbstractInputAction
{
   private Node tank;
   private Angle pos;
   private Angle neg;
   private ProtocolClient protClient; 
   
   public HorizontalCamAction(Node t, Angle p, Angle n, ProtocolClient pc)
   {
      tank = t;
      pos = p;
      neg = n;
      protClient = pc;
   }
   
   public void performAction(float time, Event e)
   {
      if(e.getValue() < -0.5)
      {
         tank.yaw(pos);
         protClient.sendMoveMessage((Vector3f)tank.getWorldPosition());
      }
      else if(e.getValue() > 0.5)
      {
         tank.yaw(neg);
         protClient.sendMoveMessage((Vector3f)tank.getWorldPosition());
      }
      else
      {
         
      }
   }
   
}