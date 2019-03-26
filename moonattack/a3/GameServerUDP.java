package a3;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;

import ray.networking.server.GameConnectionServer;
import ray.networking.server.IClientInfo;

public class GameServerUDP extends GameConnectionServer<UUID> {

   public GameServerUDP(int localPort) throws IOException { 
      super(localPort, ProtocolType.UDP); 
   }
   
   @Override
 
   public void processPacket(Object o, InetAddress senderIP, int sndPort) {
      String message = (String) o;
      String[] msgTokens = message.split(",");
      if(msgTokens.length > 0) {
         // case where server receives a JOIN message
         // format: join,localid

         if(msgTokens[0].compareTo("join") == 0) { 
            System.out.println("JOIN TEST");
            try { 
               
               IClientInfo ci;
               ci = getServerSocket().createClientInfo(senderIP, sndPort);
               UUID clientID = UUID.fromString(msgTokens[1]);
               addClient(ci, clientID);
               sendJoinedMessage(clientID, true);
               System.out.println("Message Back: " + clientID);
            }
            catch (IOException e) { 
               e.printStackTrace();
            } 
         }
         
         // case where server receives a CREATE message
         // format: create,localid,x,y,z
         if(msgTokens[0].compareTo("create") == 0) { 
            System.out.println("CREATE ASDF***");
            System.out.println("Create Client");
            UUID clientID = UUID.fromString(msgTokens[1]);
            String[] pos = {msgTokens[2], msgTokens[3], msgTokens[4]};
            sendCreateMessages(clientID, pos);
            sendWantsDetailsMessages(clientID);
         }
         
         // case where server receives a BYE message
         // format: bye,localid
         if(msgTokens[0].compareTo("bye") == 0) {
            System.out.println("BYE"); 
            UUID clientID = UUID.fromString(msgTokens[1]);
            sendByeMessages(clientID);
            removeClient(clientID);
         }

         // case where server receives a DETAILS-FOR message
         if(msgTokens[0].compareTo("dsfr") == 0) { 
            UUID clientID = UUID.fromString(msgTokens[1]);
            UUID remoteid = UUID.fromString(msgTokens[2]);
            String[] pass = {msgTokens[3], msgTokens[4], msgTokens[5]};
            sendDetailsMsg(clientID, remoteid, pass);
         }
            
            // case where server receives a MOVE message
            if(msgTokens[0].compareTo("move") == 0) {
               System.out.println("MOVE TEST"); 
               UUID clientID = UUID.fromString(msgTokens[1]);
               String[] pos = {msgTokens[2], msgTokens[3], msgTokens[4]};
               sendMoveMessages(clientID, pos);
            }
         }
      }
         
      public void sendJoinedMessage(UUID clientID, boolean success) { 
         // format: join, success or join, failure
         try { 
            String message = new String("join,");
            if (success) message += "success";
            else message += "failure";
            sendPacket(message, clientID);
         }
         catch (IOException e) { e.printStackTrace(); }
      }
      
      public void sendCreateMessages(UUID clientID, String[] position) { 
         // format: create, remoteId, x, y, z
         try { 
            String message = new String("create," + clientID.toString());
            message += "," + position[0];
            message += "," + position[1];
            message += "," + position[2];
            forwardPacketToAll(message, clientID);
         }
         catch (IOException e) { e.printStackTrace();
         } 
      }
      
      public void sendDetailsMsg(UUID clientID, UUID remoteId, String[] position) { 
         try{
            String details = new String("details," + clientID.toString() + ", " + position[0] + ", " + position[1] + ", " + position[2]);
            sendPacket(details, remoteId);
         }catch (IOException e){
            e.printStackTrace();
         }
      }
      public void sendWantsDetailsMessages(UUID clientID) { 
         try{
            String sendDetails = new String("Send Details: " + clientID.toString());
            forwardPacketToAll(sendDetails, clientID);
         }catch (IOException e){
            e.printStackTrace();
         }
         
      }
      public void sendMoveMessages(UUID clientID, String[] position) { 
         try{
            String moveMessage = new String("move," + clientID.toString());
            moveMessage += "," + position[0];
            moveMessage += "," + position[1];
            moveMessage += "," + position[2];
            forwardPacketToAll(moveMessage, clientID);
         }catch (IOException e){
            e.printStackTrace();
         }
      }
      public void sendByeMessages(UUID clientID) { 
         try{
            String byeMessage = new String("Bye: " + clientID.toString());
            forwardPacketToAll(byeMessage, clientID);
         }catch (IOException e){
            e.printStackTrace();
         }
      }
}