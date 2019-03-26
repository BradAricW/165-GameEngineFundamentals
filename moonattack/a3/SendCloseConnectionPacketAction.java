package a3;

import ray.input.action.AbstractInputAction;
import ray.rage.game.*;
import net.java.games.input.Event;
import ray.rage.scene.Node;
import ray.rml.*;

public class SendCloseConnectionPacketAction extends AbstractInputAction{
   
   private ProtocolClient protClient;
   private Boolean isClientConnected;
   
   public SendCloseConnectionPacketAction(Boolean b, ProtocolClient p) {
      isClientConnected = b;
      protClient = p;
   }
   
   public void performAction(float time, Event evt){
      if(protClient != null && isClientConnected == true){ 
         protClient.sendByeMessage();
      } 
   } 
}