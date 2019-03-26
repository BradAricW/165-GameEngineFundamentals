package a3;
import a3.MyGame;

import ray.networking.client.GameConnectionClient;
import ray.rml.Vector3;
import ray.rml.Vector3f;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.UUID;
import java.util.Vector;
import ray.rage.scene.*;

public class GhostAvatar{ 
   private UUID id;
   private SceneNode node;
   private Entity entity;
   private Vector3 pos;
   private MyGame game;
   public GhostAvatar(UUID id, Vector3 position){ 
      this.id = id;
      this.pos = position;
   }
   // accessors and setters for id, node, entity, and position
   
   public UUID getID() {
      return id;
   }
   
   public void setNode(SceneNode n) {
      node = n;
   }
   
   public void setEntity(Entity e) {
      entity = e;
   }
   
   public void setPosition(Vector3 posNew) {
      pos = posNew;
   }
   
   public SceneNode getNode() {
      return node;
   }
   
   public Entity getEntity() {
      return entity;
   }
}