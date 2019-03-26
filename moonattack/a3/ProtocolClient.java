package a3;

import ray.networking.client.GameConnectionClient;
import ray.rml.Vector3;
import ray.rml.Vector3f;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;

public class ProtocolClient extends GameConnectionClient { 
   private MyGame game;
   private UUID id;
   private Vector<GhostAvatar> ghostAvatars;
   public ProtocolClient(InetAddress remAddr, int remPort, ProtocolType pType, MyGame game) throws IOException { 
      super(remAddr, remPort, pType);
      this.game = game;
      this.id = UUID.randomUUID();
      this.ghostAvatars = new Vector<GhostAvatar>();
   }
   
   @Override
   protected void processPacket(Object msg) { 
      String strMessage = (String)msg;
      String[] messageTokens = strMessage.split(",");
      if(messageTokens.length > 0) {
         if(messageTokens[0].compareTo("join") == 0) // receive “join” 
         { // format: join, success or join, failure
            if(messageTokens[1].compareTo("success") == 0) { 
               System.out.println("SUCCESS TEST");
               game.setIsConnected(true);
               sendCreateMessage(game.getPlayerPosition());
            }
            if(messageTokens[1].compareTo("failure") == 0) { 
               System.out.println("FAILURE TEST");
               game.setIsConnected(false);
            } 
         }
         if(messageTokens[0].compareTo("bye") == 0) // receive “bye”
         { // format: bye, remoteId
            UUID ghostID = UUID.fromString(messageTokens[1]);
            removeGhostAvatar(ghostID);
         }
         if ((messageTokens[0].compareTo("dsfr") == 0 ) )// receive “dsfr”
         { // format: create, remoteId, x,y,z or dsfr, remoteId, x,y,z
            UUID ghostID = UUID.fromString(messageTokens[1]);
            Vector3 ghostPosition = Vector3f.createFrom(Float.parseFloat(messageTokens[2]), Float.parseFloat(messageTokens[3]), Float.parseFloat(messageTokens[4]));
            //try { 
               createGhostAvatar(ghostID, ghostPosition);
           // } 
           // catch (IOException e) { 
            //   System.out.println("error creating ghost avatar");
           // } 
         }
         if(messageTokens[0].compareTo("create") == 0) // rec. “create…”
         { 
            UUID ghostID = UUID.fromString(messageTokens[1]);
            Vector3 position = Vector3f.createFrom(Float.parseFloat(messageTokens[2]), Float.parseFloat(messageTokens[3]), Float.parseFloat(messageTokens[4]));
            createGhostAvatar(ghostID, position);
         }
         if(messageTokens[0].compareTo("wsds") == 0) // rec. “wants…”
         { 
            UUID ghostID = UUID.fromString(messageTokens[1]);
            sendDetailsForMessage(ghostID, (Vector3f)game.getPlayerPosition()); 
         }
         if(messageTokens[0].compareTo("move") == 0) // rec. “move...”
         { 
            System.out.println("CLIENT MOVE TEST");
            UUID ghostID = UUID.fromString(messageTokens[1]);
            Vector3 position = Vector3f.createFrom(Float.parseFloat(messageTokens[2]), Float.parseFloat(messageTokens[3]), Float.parseFloat(messageTokens[4]));
            moveGhostAvatar(ghostID, position);
         }
      } 
   }
      /*Also need functions to instantiate ghost avatar, remove a ghost avatar,
      look up a ghost in the ghost table, update a ghost’s position, and
      accessors as needed.*/
   
   public void createGhostAvatar(UUID ghostID, Vector3 position)
	{
		GhostAvatar ghostAvatar = new GhostAvatar(ghostID, position);
		ghostAvatars.add(ghostAvatar);
		try
		{
			game.addGhostAvatarToGameWorld(ghostAvatar);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	public void removeGhostAvatar(UUID ghostID)
	{
		Iterator<GhostAvatar> iterator = ghostAvatars.iterator();
		while (iterator.hasNext())
		{
			GhostAvatar ghostAvatar = iterator.next();
			if (ghostAvatar.getID().equals(ghostID))
			{
				ghostAvatars.removeElement(ghostAvatar);
				game.removeGhostAvatarFromGameWorld(ghostAvatar);
				break;
			}
		}
	}
   
   public void moveGhostAvatar(UUID ghostID, Vector3 pos)
	{
		Iterator<GhostAvatar> iterator = ghostAvatars.iterator();
		while (iterator.hasNext())
		{
			GhostAvatar ghostAvatar = iterator.next();
			System.out.println(ghostAvatar.getID() + ", " + ghostID);
			if (ghostAvatar.getID().equals(ghostID))
			{
				ghostAvatar.setPosition(pos);
				game.moveGhostAvatarAroundGameWorld(ghostAvatar, pos);
				break;
			}
		}
	}
   
   public void sendJoinMessage() // format: join, localId
   { 
      try
      { 
         sendPacket(new String("join," + id.toString()));
      } 
      catch (IOException e) 
      { 
         e.printStackTrace();
      } 
   }
   
   public void sendCreateMessage(Vector3 pos)
   { // format: (create, localId, x,y,z)
      try
      { 
         String message = new String("create," + id.toString());
         message += "," + pos.x()+"," + pos.y() + "," + pos.z();
         sendPacket(message);
      }
      catch (IOException e) 
      { 
         e.printStackTrace();
      } 
   }
   
  	public void sendByeMessage()
	{
		try
		{
			String message = new String("bye," + id.toString());
			sendPacket(message);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	public void sendDetailsForMessage(UUID remId, Vector3 pos)
	{
		try
		{
			String message = new String("dsfr," + id.toString() + "," + remId.toString());
			message += "," + pos.x() + "," + pos.y() + "," + pos.z();
			sendPacket(message);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	public void sendMoveMessage(Vector3 pos)
	{
		try
		{
			String message = new String("move," + id.toString());
			message += "," + pos.x() + "," + pos.y() + "," + pos.z();
			sendPacket(message);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
   
   public UUID getID() {
      return id;
   }

}