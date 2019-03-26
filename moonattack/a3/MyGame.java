package a3;

import java.awt.*;
import java.awt.event.*;
import java.io.*;

import ray.rage.*;
import ray.rage.game.*;
import ray.rage.rendersystem.*;
import ray.rage.rendersystem.Renderable.*;
import ray.rage.scene.*;
import ray.rage.scene.Camera.Frustum.*;
import ray.rage.scene.controllers.*;
import ray.rml.*;
import ray.rage.rendersystem.gl4.GL4RenderSystem;

import ray.rage.rendersystem.states.*;
import ray.rage.asset.texture.*;
import ray.input.*;
import ray.input.action.*;
import ray.rage.util.BufferUtil;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import ray.rage.asset.material.*;
import ray.rage.rendersystem.shader.*;

//added skybox imports
import ray.rage.util.*;
import java.awt.geom.*;
//end skybox imports

//added skeletal Entity
import static ray.rage.scene.SkeletalEntity.EndType.*;
//end skeletal Entity

//added javaScript imports
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.*;
//end javaScript imports

//networking imports
import ray.networking.IGameConnection.ProtocolType;
import java.net.InetAddress;
import java.net.UnknownHostException;


import ray.audio.*;
import com.jogamp.openal.ALFactory;

//start main class
public class MyGame extends VariableFrameRateGame {

   //base variables
   GL4RenderSystem rs;
   float elapsTime = 0.0f;
   float health = 100;
   int elapsTimeSec, points = 0;
   String elapsTimeStr, pointsStr, dispStr, healthStr;
   private Angle rotAmt, rotAmt2, rotAmt3, posAngle, negAngle;
   
   private InputManager im;
   private Entity tankE;
   private SceneNode tankN;
   private SceneNode cameraNode;
   private SceneNode tankCamera;
   private SceneNode tessN;
   private Tessellation tessE;
   private Camera camera;
   
   private SceneNode ghostTank;
   private Entity ghostE;
   
   //Sound variables
   IAudioManager audioMgr;
   Sound oceanSound, ufoSound;
   
   private SceneNode[] nodeArray;
   
   //created class variables
   private IIterator iterate;
   private ShipCollection sColl;
   private Action MoveForwardAction, MoveBackwardAction, MoveLeftAction, MoveRightAction;
   private Action TurnLeftAction, TurnRightAction, HorizontalCamAction, HorizontalMoveAction, VerticalMoveAction;
   private Action sendCloseAction;
   
   //added skybox variables
   private static final String SKYBOX_NAME = "SkyBox";
   private boolean skyBoxVisible = true;
   //end skybox variables
   
   //added networking variables
   private String serverAddress;
   private int serverPort;
   private ProtocolType serverProtocol;
   private ProtocolClient protClient;
   private boolean isClientConnected;
   private Vector<UUID> gameObjectsToRemove;
   private GameServerUDP gameUDP;
   //end networking variables
   
   public MyGame(String serverAddr, int sPort) {
      super();
      this.serverAddress = serverAddr;
      this.serverPort = sPort;
      this.serverProtocol = ProtocolType.UDP;
   }
   
   public static void main(String[] args) {
      Game game = new MyGame(args[0], Integer.parseInt(args[1]));
      /*
      ScriptEngineManager factory = new ScriptEngineManager();
      String scriptFileName = "ufoSize.js";
      
      // get the JavaScript engine
      ScriptEngine jsEngine = factory.getEngineByName("js");
      
      // run the script
      game.executeScript(jsEngine, scriptFileName);
      */
      try {
         game.startup();
         game.run();
      } 
      catch(Exception e) {
         e.printStackTrace(System.err);
      } 
      finally {
         game.shutdown();
         game.exit();
      }
   }
   
   @Override
   protected void setupWindow(RenderSystem rs, GraphicsEnvironment ge) {
      rs.createRenderWindow(new DisplayMode(1000, 700, 24, 60), false);
   } 
   
   @Override
   protected void setupCameras(SceneManager sm, RenderWindow rw) {
   	
   	//create camera
      SceneNode rootNode = sm.getRootSceneNode();
      camera = sm.createCamera("MainCamera", Projection.PERSPECTIVE);
      rw.getViewport(0).setCamera(camera);
   	
   	//set camera
      camera.setRt((Vector3f)Vector3f.createFrom(1.0f, 0.0f, 0.0f));
      camera.setUp((Vector3f)Vector3f.createFrom(0.0f, 1.0f, 0.0f));
      camera.setFd((Vector3f)Vector3f.createFrom(0.0f, 0.0f, -1.0f));
      camera.setPo((Vector3f)Vector3f.createFrom(0.0f, 0.0f, 0.0f));
   	
      cameraNode = rootNode.createChildSceneNode(camera.getName() + "Node");
      cameraNode.attachObject(camera);
   }
   
   @Override
   protected void setupScene(Engine eng, SceneManager sm) throws IOException {
      
      /*
      // prepare the script engine
      ScriptEngineManager factory = new ScriptEngineManager();
      java.util.List<ScriptEngineFactory> list = factory.getEngineFactories();
      jsEngine = factory.getEngineByName("js");
      */
      
      //***************************Skyboxes*******************************
      Configuration conf = eng.getConfiguration();
      TextureManager tm = getEngine().getTextureManager();
      tm.setBaseDirectoryPath(conf.valueOf("assets.skyboxes.path"));
      Texture front = tm.getAssetByPath("toptile.jpg");
      Texture back = tm.getAssetByPath("bottile.jpg");
      Texture left = tm.getAssetByPath("lefttile.jpg");
      Texture right = tm.getAssetByPath("righttile.jpg");
      Texture top = tm.getAssetByPath("fartile.jpg");
      Texture bottom = tm.getAssetByPath("centertile.jpg");
      tm.setBaseDirectoryPath(conf.valueOf("assets.textures.path"));
      
      AffineTransform xform = new AffineTransform();
      xform.translate(0, front.getImage().getHeight());
      xform.scale(1d, -1d);
      
      front.transform(xform);
      back.transform(xform);
      left.transform(xform);
      right.transform(xform);
      top.transform(xform);
      bottom.transform(xform);
      
      SkyBox sb = sm.createSkyBox(SKYBOX_NAME);
      sb.setTexture(front, SkyBox.Face.FRONT);
      sb.setTexture(back, SkyBox.Face.BACK);
      sb.setTexture(left, SkyBox.Face.LEFT);
      sb.setTexture(right, SkyBox.Face.RIGHT);
      sb.setTexture(top, SkyBox.Face.TOP);
      sb.setTexture(bottom, SkyBox.Face.BOTTOM);
      sm.setActiveSkyBox(sb);
      //endskyboxes
   
      //**************************General Tank Attributes************************
      
      //angles
      rotAmt = Degreef.createFrom(180.0f);
      rotAmt2 = Degreef.createFrom(90.0f);
      rotAmt3 = Degreef.createFrom(45.0f);
      negAngle = Degreef.createFrom(-2.0f);
      posAngle = Degreef.createFrom(2.0f);   
      
      //tank entity and node
      tankE = sm.createEntity("myTank", "Tank1.obj");
      tankE.setPrimitive(Primitive.TRIANGLES);
      //tankE.setMaterial(sm.getMaterialManager().getAssetByName("default.mtl"));
      tankN = sm.getRootSceneNode().createChildSceneNode(tankE.getName()+"Node");
      tankN.moveBackward(2.0f);
      tankN.attachObject(tankE);
      
      
      //alter tank
      //tankN.yaw(rotAmt2);
      tankN.scale(.0005f, .0005f, .0005f);
   
      //attach camera
      tankCamera = tankN.createChildSceneNode("tank");
      tankCamera.attachObject(camera);
      tankCamera.translate(-35.0f, 5.0f, 0.0f);
      camera.setMode('n'); 
      tankCamera.yaw(rotAmt2);     
      //tankN.yaw(rotAmt);
      
      //****************************Lights************************************
      sm.getAmbientLight().setIntensity(new Color(.3f, .3f, .3f));
   
      Light plight = sm.createLight("lamp1", Light.Type.POINT);
      plight.setAmbient(new Color(.3f, .3f, .3f));
      plight.setDiffuse(new Color(.7f, .7f, .7f));
      plight.setSpecular(new Color(1.0f, 1.0f, 1.0f));
      plight.setRange(5f);
      
      SceneNode plightNode = sm.getRootSceneNode().createChildSceneNode("plightNode");
      plightNode.attachObject(plight);
      
      Light plight2 = sm.createLight("lamp2", Light.Type.POINT);
      plight.setAmbient(new Color(.3f, .3f, .3f));
      plight.setDiffuse(new Color(.7f, .7f, .7f));
      plight.setSpecular(new Color(1.0f, 1.0f, 1.0f));
      plight.setRange(5f);
      
      SceneNode plight2Node = sm.getRootSceneNode().createChildSceneNode("plight2Node");
      plight2Node.attachObject(plight2);
      
      tankN.attachObject(plight2);
      
      
      
      //****************************Terrain************************************
      tessE = sm.createTessellation("tessE", 9);
      tessE.setSubdivisions(16f);
      
      tessN = sm.getRootSceneNode().createChildSceneNode("tessN");
      tessN.attachObject(tessE);
      tessN.scale(300, 500, 300);
      tessN.translate(Vector3f.createFrom(-5.0f, -2.2f, -2.7f));
      tessE.setHeightMap(this.getEngine(), "terrain.jpg");
      tessE.setTexture(this.getEngine(), "moon.jpeg");   
      
      
      //***************************EARTH****************************************
      Entity earthE = sm.createEntity("myEarth", "earth.obj");
      earthE.setPrimitive(Primitive.TRIANGLES);
      SceneNode earthSceneNode = sm.getRootSceneNode().createChildSceneNode(earthE.getName() + "Node");
      earthSceneNode.moveBackward(10.0f);
      earthSceneNode.moveUp(50.0f);
      earthSceneNode.moveLeft(60.0f);
      //How to set the size
      earthSceneNode.setLocalScale(10.0f,10.0f,10.0f);
      earthSceneNode.attachObject(earthE);
      
      //***************************GreenBox****************************************
      Entity boxGreenE = sm.createEntity("boxGreen", "boxGreen.obj");
      boxGreenE.setPrimitive(Primitive.TRIANGLES);
      SceneNode boxGreenSN = sm.getRootSceneNode().createChildSceneNode(boxGreenE.getName() + "Node");
      boxGreenSN.moveBackward(10.0f);
      boxGreenSN.moveDown(2.0f);
      boxGreenSN.moveLeft(10.0f);
      //How to set the size
      boxGreenSN.setLocalScale(0.25f,0.25f,0.25f);
      boxGreenSN.attachObject(boxGreenE);
      
      
      //***************************GreenBox****************************************
      Entity boxGreenE2 = sm.createEntity("boxGreen2", "boxGreen.obj");
      boxGreenE2.setPrimitive(Primitive.TRIANGLES);
      SceneNode boxGreenSN2 = sm.getRootSceneNode().createChildSceneNode(boxGreenE.getName() + "Node2");
      boxGreenSN2.moveBackward(10.0f);
      boxGreenSN2.moveDown(2.0f);
      boxGreenSN2.moveLeft(10.50f);
      //How to set the size
      boxGreenSN2.setLocalScale(0.25f,0.25f,0.25f);
      boxGreenSN2.attachObject(boxGreenE2);
      
      //***************************GreenBox****************************************
      Entity boxGreenE3 = sm.createEntity("boxGreen3", "boxGreen.obj");
      boxGreenE3.setPrimitive(Primitive.TRIANGLES);
      SceneNode boxGreenSN3 = sm.getRootSceneNode().createChildSceneNode(boxGreenE.getName() + "Node3");
      boxGreenSN3.moveBackward(10.25f);
      boxGreenSN3.moveDown(1.50f);
      boxGreenSN3.moveLeft(10.25f);
      //How to set the size
      boxGreenSN3.setLocalScale(0.25f,0.25f,0.25f);
      boxGreenSN3.attachObject(boxGreenE3);
      
      //***************************GreenBox****************************************
      Entity boxGreenE4 = sm.createEntity("boxGreen4", "boxGreen.obj");
      boxGreenE4.setPrimitive(Primitive.TRIANGLES);
      SceneNode boxGreenSN4 = sm.getRootSceneNode().createChildSceneNode(boxGreenE.getName() + "Node4");
      boxGreenSN4.moveBackward(10.50f);
      boxGreenSN4.moveDown(2.0f);
      boxGreenSN4.moveLeft(10.50f);
      //How to set the size
      boxGreenSN4.setLocalScale(0.25f,0.25f,0.25f);
      boxGreenSN4.attachObject(boxGreenE4);
      
      //***************************GreenBox****************************************
      Entity boxGreenE5 = sm.createEntity("boxGreen5", "boxGreen.obj");
      boxGreenE5.setPrimitive(Primitive.TRIANGLES);
      SceneNode boxGreenSN5 = sm.getRootSceneNode().createChildSceneNode(boxGreenE.getName() + "Node5");
      boxGreenSN5.moveBackward(10.50f);
      boxGreenSN5.moveDown(2.0f);
      boxGreenSN5.moveLeft(10.0f);
      //How to set the size
      boxGreenSN5.setLocalScale(0.25f,0.25f,0.25f);
      boxGreenSN5.attachObject(boxGreenE5);
      
      //***************************RedBox****************************************
      Entity boxRedE = sm.createEntity("boxRed", "boxRed.obj");
      boxRedE.setPrimitive(Primitive.TRIANGLES);
      SceneNode boxRedSN = sm.getRootSceneNode().createChildSceneNode(boxRedE.getName() + "Node");
      boxRedSN.moveBackward(10.0f);
      boxRedSN.moveDown(2.0f);
      boxRedSN.moveRight(10.0f);
      //How to set the size
      boxRedSN.setLocalScale(0.25f,0.25f,0.25f);
      boxRedSN.attachObject(boxRedE);
      
      
      //***************************RedBox****************************************
      Entity boxRedE2 = sm.createEntity("boxRed2", "boxRed.obj");
      boxRedE2.setPrimitive(Primitive.TRIANGLES);
      SceneNode boxRedSN2 = sm.getRootSceneNode().createChildSceneNode(boxRedE.getName() + "Node2");
      boxRedSN2.moveBackward(10.0f);
      boxRedSN2.moveDown(2.0f);
      boxRedSN2.moveRight(10.50f);
      //How to set the size
      boxRedSN2.setLocalScale(0.25f,0.25f,0.25f);
      boxRedSN2.attachObject(boxRedE2);
      
      //***************************RedBox****************************************
      Entity boxRedE3 = sm.createEntity("boxRed3", "boxRed.obj");
      boxRedE3.setPrimitive(Primitive.TRIANGLES);
      SceneNode boxRedSN3 = sm.getRootSceneNode().createChildSceneNode(boxRedE.getName() + "Node3");
      boxRedSN3.moveBackward(10.25f);
      boxRedSN3.moveDown(1.50f);
      boxRedSN3.moveRight(10.25f);
      //How to set the size
      boxRedSN3.setLocalScale(0.25f,0.25f,0.25f);
      boxRedSN3.attachObject(boxRedE3);
      
      //***************************RedBox****************************************
      Entity boxRedE4 = sm.createEntity("boxRed4", "boxRed.obj");
      boxRedE4.setPrimitive(Primitive.TRIANGLES);
      SceneNode boxRedSN4 = sm.getRootSceneNode().createChildSceneNode(boxRedE.getName() + "Node4");
      boxRedSN4.moveBackward(10.50f);
      boxRedSN4.moveDown(2.0f);
      boxRedSN4.moveRight(10.50f);
      //How to set the size
      boxRedSN4.setLocalScale(0.25f,0.25f,0.25f);
      boxRedSN4.attachObject(boxRedE4);
      
      //***************************RedBox****************************************
      Entity boxRedE5 = sm.createEntity("boxRed5", "boxRed.obj");
      boxRedE5.setPrimitive(Primitive.TRIANGLES);
      SceneNode boxRedSN5 = sm.getRootSceneNode().createChildSceneNode(boxRedE.getName() + "Node5");
      boxRedSN5.moveBackward(10.50f);
      boxRedSN5.moveDown(2.0f);
      boxRedSN5.moveRight(10.0f);
      //How to set the size
      boxRedSN5.setLocalScale(0.25f,0.25f,0.25f);
      boxRedSN5.attachObject(boxRedE5);
      
      //***************************BlueBox****************************************
      Entity boxBlueE = sm.createEntity("boxBlue", "boxBlue.obj");
      boxBlueE.setPrimitive(Primitive.TRIANGLES);
      SceneNode boxBlueSN = sm.getRootSceneNode().createChildSceneNode(boxBlueE.getName() + "Node");
      boxBlueSN.moveForward(10.0f);
      boxBlueSN.moveDown(2.0f);
      boxBlueSN.moveRight(10.0f);
      //How to set the size
      boxBlueSN.setLocalScale(0.25f,0.25f,0.25f);
      boxBlueSN.attachObject(boxBlueE);
      
      
      //***************************BlueBox****************************************
      Entity boxBlueE2 = sm.createEntity("boxBlue2", "boxBlue.obj");
      boxBlueE2.setPrimitive(Primitive.TRIANGLES);
      SceneNode boxBlueSN2 = sm.getRootSceneNode().createChildSceneNode(boxBlueE.getName() + "Node2");
      boxBlueSN2.moveForward(10.0f);
      boxBlueSN2.moveDown(2.0f);
      boxBlueSN2.moveRight(10.50f);
      //How to set the size
      boxBlueSN2.setLocalScale(0.25f,0.25f,0.25f);
      boxBlueSN2.attachObject(boxBlueE2);
      
      //***************************BlueBox****************************************
      Entity boxBlueE3 = sm.createEntity("boxBlue3", "boxBlue.obj");
      boxBlueE3.setPrimitive(Primitive.TRIANGLES);
      SceneNode boxBlueSN3 = sm.getRootSceneNode().createChildSceneNode(boxBlueE.getName() + "Node3");
      boxBlueSN3.moveForward(10.25f);
      boxBlueSN3.moveDown(1.50f);
      boxBlueSN3.moveRight(10.25f);
      //How to set the size
      boxBlueSN3.setLocalScale(0.25f,0.25f,0.25f);
      boxBlueSN3.attachObject(boxBlueE3);
      
      //***************************BlueBox****************************************
      Entity boxBlueE4 = sm.createEntity("boxBlue4", "boxBlue.obj");
      boxBlueE4.setPrimitive(Primitive.TRIANGLES);
      SceneNode boxBlueSN4 = sm.getRootSceneNode().createChildSceneNode(boxBlueE.getName() + "Node4");
      boxBlueSN4.moveForward(10.50f);
      boxBlueSN4.moveDown(2.0f);
      boxBlueSN4.moveRight(10.50f);
      //How to set the size
      boxBlueSN4.setLocalScale(0.25f,0.25f,0.25f);
      boxBlueSN4.attachObject(boxBlueE4);
      
      //***************************BlueBox****************************************
      Entity boxBlueE5 = sm.createEntity("boxBlue5", "boxBlue.obj");
      boxBlueE5.setPrimitive(Primitive.TRIANGLES);
      SceneNode boxBlueSN5 = sm.getRootSceneNode().createChildSceneNode(boxBlueE.getName() + "Node5");
      boxBlueSN5.moveForward(10.50f);
      boxBlueSN5.moveDown(2.0f);
      boxBlueSN5.moveRight(10.0f);
      //How to set the size
      boxBlueSN5.setLocalScale(0.25f,0.25f,0.25f);
      boxBlueSN5.attachObject(boxBlueE5);
      
      //////////////////////////////UFO1///////////////////////////////////////////////////////
   
      SkeletalEntity ufoSE = sm.createSkeletalEntity("ufoAv", "ufo.rkm", "ufo.rks");
      Texture ufoTex = sm.getTextureManager().getAssetByPath("ufo.png");
      TextureState ufoTstate = (TextureState) sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
      ufoTstate.setTexture(ufoTex);
      ufoSE.setRenderState(ufoTstate);
      
      SceneNode ufoN = sm.getRootSceneNode().createChildSceneNode("ufoNode");
      ufoN.attachObject(ufoSE);
      ufoN.scale(0.75f,0.75f,0.75f);
      ufoN.moveBackward(20.0f);
      ufoN.moveRight(1.0f);
      ufoN.moveUp(1.0f);
   
      ufoSE.loadAnimation("ArmatureAction", "ufo.rka");
      ufoSE.playAnimation("ArmatureAction", 0.5f, LOOP, 0);
      
      //////////////////////////////UFO2///////////////////////////////////////////////////////
   
      SkeletalEntity ufo2SE = sm.createSkeletalEntity("ufo2Av", "ufo.rkm", "ufo.rks");
      Texture ufo2Tex = sm.getTextureManager().getAssetByPath("ufo.png");
      TextureState ufo2Tstate = (TextureState) sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
      ufo2Tstate.setTexture(ufo2Tex);
      ufo2SE.setRenderState(ufo2Tstate);
      
      SceneNode ufo2N = sm.getRootSceneNode().createChildSceneNode("ufo2Node");
      ufo2N.attachObject(ufo2SE);
      ufo2N.scale(0.75f,0.75f,0.75f);
      ufo2N.moveForward(20.0f);
      ufo2N.moveRight(1.0f);
      ufo2N.moveUp(1.0f);
      
      ufo2SE.loadAnimation("ArmatureAction", "ufo.rka");
      ufo2SE.playAnimation("ArmatureAction", 0.5f, LOOP, 0);
      
      
      //////////////////////////////UFO3///////////////////////////////////////////////////////
   
      SkeletalEntity ufo3SE = sm.createSkeletalEntity("ufo3Av", "ufo.rkm", "ufo.rks");
      Texture ufo3Tex = sm.getTextureManager().getAssetByPath("ufo.png");
      TextureState ufo3Tstate = (TextureState) sm.getRenderSystem().createRenderState(RenderState.Type.TEXTURE);
      ufo3Tstate.setTexture(ufo3Tex);
      ufo3SE.setRenderState(ufo3Tstate);
      
      SceneNode ufo3N = sm.getRootSceneNode().createChildSceneNode("ufo3Node");
      ufo3N.attachObject(ufo3SE);
      ufo3N.scale(0.75f,0.75f,0.75f);
      ufo3N.moveForward(15.0f);
      ufo3N.moveRight(10.0f);
      ufo3N.moveUp(1.0f);
      
      ufo3SE.loadAnimation("ArmatureAction", "ufo.rka");
      ufo3SE.playAnimation("ArmatureAction", 0.5f, LOOP, 0);
      
      /*
      // use spin speed setting from the first script to initialize dolphin rotation
      scriptFile1 = new File("ufoSize.js");
      this.runScript(scriptFile1);
      rc = new RotationController(Vector3f.createUnitVectorY(),((Double)(jsEngine.get("spinSpeed"))).floatValue());
      rc.addNode(earthSceneNode);
      sm.addController(rc);
      */
      setupNetworking();
      setupInputs();
      
      initAudio(sm);
   
   }
   
   @Override
   protected void update(Engine eng) {
      SkeletalEntity ufoSE = (SkeletalEntity) eng.getSceneManager().getEntity("ufoAv");
      //alienSE.playAnimation("waveAnimation", 0.5f, LOOP, 0);
      ufoSE.update();
      
      SkeletalEntity ufo2SE = (SkeletalEntity) eng.getSceneManager().getEntity("ufo2Av");
      //alienSE.playAnimation("waveAnimation", 0.5f, LOOP, 0);
      ufo2SE.update();
      
      SkeletalEntity ufo3SE = (SkeletalEntity) eng.getSceneManager().getEntity("ufo3Av");
      //alienSE.playAnimation("waveAnimation", 0.5f, LOOP, 0);
      ufo3SE.update();
      
      rs = (GL4RenderSystem) eng.getRenderSystem();
      
      //calculate stats
      elapsTime += eng.getElapsedTimeMillis();
      elapsTimeSec = Math.round(elapsTime/1000.0f);
      
      //convert to strings
      elapsTimeStr = Integer.toString(elapsTimeSec);
      pointsStr = Integer.toString(points);
   	
   	//set display strings
      dispStr = "Time: " + elapsTimeStr + "     Score: " + pointsStr + "     Health: " + (int)health;
   	
   	//set HUD
      rs.setHUD(dispStr, 15, 15);
      
      im.update(elapsTime);
      /*
      // run script again in update() to demonstrate dynamic modification
      long modTime = scriptFile1.lastModified();
      if (modTime > fileLastModifiedTime){ 
         fileLastModifiedTime = modTime;
         this.runScript(scriptFile1);
         rc.setSpeed(((Double)(jsEngine.get("spinSpeed"))).floatValue());
      }
      */
      
      processNetworking(eng, elapsTime);
   }
   
   
   protected void setupInputs() {
      im = new GenericInputManager();
      String kbName = im.getKeyboardName();
   
      Entity tank = getEngine().getSceneManager().getEntity("myTank");
      
      Boolean b = isClientConnected;
      ProtocolClient p = protClient;
   	
      TurnLeftAction = new TurnLeftAction(tankN, posAngle, p);
      TurnRightAction = new TurnRightAction(tankN, negAngle, p);
      MoveLeftAction = new MoveLeftAction(tankN, this, p);
      MoveRightAction = new MoveRightAction(tankN, this, p);
      MoveForwardAction = new MoveForwardAction(tankN, this, p);
      MoveBackwardAction = new MoveBackwardAction(tankN, this, p);
      HorizontalCamAction = new HorizontalCamAction(tankN, posAngle, negAngle, p);
      HorizontalMoveAction = new HorizontalMoveAction(tankN, this, p);
      VerticalMoveAction = new VerticalMoveAction(tankN, this, p);
      sendCloseAction = new SendCloseConnectionPacketAction(b, p);
   	
   	//keyboard inputs
   	
      im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.RIGHT, TurnRightAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
      im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.LEFT, TurnLeftAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
      im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.D, MoveForwardAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
      im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.W, MoveRightAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
      im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.A, MoveBackwardAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
      im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.S, MoveLeftAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
      im.associateAction(kbName, net.java.games.input.Component.Identifier.Key.L, sendCloseAction, InputManager.INPUT_ACTION_TYPE.ON_PRESS_ONLY);		
   	
   	//controller inputs
      if (im.getFirstGamepadName() != null) {
         String gpName = im.getFirstGamepadName();
         
         im.associateAction(gpName, net.java.games.input.Component.Identifier.Axis.RX, HorizontalCamAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
         im.associateAction(gpName, net.java.games.input.Component.Identifier.Axis.Y, HorizontalMoveAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
         im.associateAction(gpName, net.java.games.input.Component.Identifier.Axis.X, VerticalMoveAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
      	//im.associateAction(gpName, net.java.games.input.Component.Identifier.Axis.RX, TurnRightAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
      	//im.associateAction(gpName, net.java.games.input.Component.Identifier.Axis.RX, TurnLeftAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
      	//im.associateAction(gpName, net.java.games.input.Component.Identifier.Axis.Y, MoveForwardAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
      	//im.associateAction(gpName, net.java.games.input.Component.Identifier.Axis.X, MoveRightAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
      	//im.associateAction(gpName, net.java.games.input.Component.Identifier.Axis.Y, MoveBackwardAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
      	//im.associateAction(gpName, net.java.games.input.Component.Identifier.Axis.X, MoveLeftAction, InputManager.INPUT_ACTION_TYPE.REPEAT_WHILE_DOWN);
      }
   }
   
   protected void updateVerticalPosition()
   {
      SceneNode tankN = this.getEngine().getSceneManager().getSceneNode("myTankNode");
      SceneNode tessN = this.getEngine().getSceneManager().getSceneNode("tessN");
      Tessellation tessE = ((Tessellation)tessN.getAttachedObject("tessE"));
      
      Vector3 worldAvatarPosition = tankN.getWorldPosition();
      Vector3 localAvatarPosition = tankN.getLocalPosition();
      
      Vector3 newAvatarPosition = Vector3f.createFrom(localAvatarPosition.x(), tessE.getWorldHeight(worldAvatarPosition.x(), worldAvatarPosition.z())+0.5f, localAvatarPosition.z());
      tankN.setLocalPosition(newAvatarPosition);
   }
   
   private void executeScript(ScriptEngine engine, String scriptFileName){
      try{ 
         FileReader fileReader = new FileReader(scriptFileName);
         engine.eval(fileReader); //execute the script statements in the file
         fileReader.close();
      }
      catch (FileNotFoundException e1){ 
         System.out.println(scriptFileName + " not found " + e1); 
      }
      catch (IOException e2){ 
         System.out.println("IO problem with " + scriptFileName + e2); 
      }
      catch (ScriptException e3){ 
         System.out.println("ScriptException in " + scriptFileName + e3); 
      }
      catch (NullPointerException e4){ 
         System.out.println ("Null ptr exception in " + scriptFileName + e4); 
      }
   }
   
   public void initAudio(SceneManager sm){ 
      AudioResource ufoR, resource2;
      audioMgr = AudioManagerFactory.createAudioManager(
         "ray.audio.joal.JOALAudioManager");
      if (!audioMgr.initialize()){ 
         System.out.println("Audio Manager failed to initialize!");
         return;
      }
      resource2 = audioMgr.createAudioResource("bgSong.wav", AudioResourceType.AUDIO_SAMPLE);
      oceanSound = new Sound(resource2,SoundType.SOUND_EFFECT, 100, true);
      oceanSound.initialize(audioMgr);
      oceanSound.setMaxDistance(10.0f);
      oceanSound.setMinDistance(0.5f);
      oceanSound.setRollOff(5.0f);
      SceneNode earthN = sm.getSceneNode("myTankNode");
      oceanSound.setLocation(earthN.getWorldPosition());
      setEarParameters(sm);
      oceanSound.play();
      /*
      ufoR = audioMgr.createAudioResource("ufoNoise.wav", AudioResourceType.AUDIO_SAMPLE);
      ufoSound = new Sound(ufoR,SoundType.SOUND_EFFECT, 100, true);
      ufoSound.initialize(audioMgr);
      ufoSound.setMaxDistance(2.0f);
      ufoSound.setMinDistance(0.5f);
      ufoSound.setRollOff(5.0f);
      SceneNode ufoN = sm.getSceneNode("ufoNode");
      ufoSound.setLocation(ufoN.getWorldPosition());
      setEarParameters(sm);
      ufoSound.play();
      */
   }
   
   public void setEarParameters(SceneManager sm){ 
      SceneNode dolphinNode = sm.getSceneNode("myTankNode");
      Vector3 avDir = dolphinNode.getWorldForwardAxis();
   // note - should get the camera's forward direction
   // - avatar direction plus azimuth
      audioMgr.getEar().setLocation(dolphinNode.getWorldPosition());
      audioMgr.getEar().setOrientation(avDir, Vector3f.createFrom(0,1,0));
   }
   
   private void setupNetworking(){ 
      gameObjectsToRemove = new Vector<UUID>();
      isClientConnected = false;
      try{
         protClient = new ProtocolClient(InetAddress.getByName(serverAddress), serverPort, serverProtocol, this);
      } 
      catch (UnknownHostException e) { 
         e.printStackTrace();
      } 
      catch (IOException e) { 
         e.printStackTrace();
      }
      if (protClient == null){ 
         System.out.println("missing protocol host"); }
      else{ 
         // ask client protocol to send initial join message
         //to server, with a unique identifier for this client
         protClient.sendJoinMessage();
      } 
   }
   
   protected void processNetworking(Engine eng, float elapsTime){
      SceneManager sm = eng.getSceneManager(); 
      // Process packets received by the client from the server
      if (protClient != null) protClient.processPackets();
      // remove ghost avatars for players who have left the game
      Iterator<UUID> it = gameObjectsToRemove.iterator();
      while(it.hasNext())
      { sm.destroySceneNode(it.next().toString());
      }
      gameObjectsToRemove.clear();
   }
   
   public Vector3 getPlayerPosition(){
      Engine eng = getEngine();
      SceneManager sm = eng.getSceneManager();
      SceneNode tankN = sm.getSceneNode("myTankNode");
      return tankN.getWorldPosition();
   }
   
   public void addGhostAvatarToGameWorld(GhostAvatar avatar)throws IOException{ 
      Engine eng = getEngine();
      SceneManager sm = eng.getSceneManager();
      if (avatar != null){ 
         Entity ghostE = sm.createEntity("ghost", "Tank1.obj");
         ghostE.setPrimitive(Primitive.TRIANGLES);
         SceneNode ghostN = sm.getRootSceneNode().createChildSceneNode(avatar.getID().toString());
         ghostN.attachObject(ghostE);
         ghostN.scale(.01f, .01f, .01f);;
        // ghostN.setLocalPosition(desired location...);
         avatar.setNode(ghostN);
         avatar.setEntity(ghostE);
        // avatar.setPosition(node’s position... maybe redundant);
      } 
   }
   
   public void removeGhostAvatarFromGameWorld(GhostAvatar avatar){
      if(avatar != null) gameObjectsToRemove.add(avatar.getID());
   }
  
   
   public void setIsConnected(Boolean b) {
      isClientConnected = b;
   }
   
   public void moveGhostAvatarAroundGameWorld(GhostAvatar g, Vector3 pos) {
      g.getNode().setLocalPosition(pos);
      //gameUDP.sendDetailsMsg(g.getID(), protClient.getID(), str);
      
   }
   
}