package a3;

import java.io.IOException;
import ray.networking.IGameConnection.ProtocolType;

public class NetworkingServer
{
   private GameServerUDP thisUDPServer;
   //private GameServerTCP thisTCPServer;
   public NetworkingServer(int serverPort, String protocol) { 
      try{ 
         if(protocol.toUpperCase().compareTo("TCP") == 0) { 
            //thisTCPServer = new GameServerTCP(serverPort);
         }
         else { 
            thisUDPServer = new GameServerUDP(serverPort);
         }
      }
      catch (IOException e) { 
         e.printStackTrace();
      }
   }

   public static void main(String[] args) { 
      System.out.println("MAIN TEST");
      if(args.length > 1) { 
         NetworkingServer app = new NetworkingServer(Integer.parseInt(args[0]), args[1]);
      } 
   } 
}