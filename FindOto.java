/* 
 * File: FindOto.java
 * Author: Richard Horvath
 * Date: 12/14/18
 * Purpose: Creates a game with two platforms and a wall, 
 *          oto randomly spawns in a location behind the wall. 
 *          Shoot cannonballs to knockdown wall and knock Oto off the platform to win
 */

package findoto;
import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CapsuleCollisionShape;
import com.jme3.bullet.control.CharacterControl;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.effect.ParticleEmitter;
import com.jme3.effect.ParticleMesh;
import com.jme3.effect.shapes.EmitterSphereShape;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import static com.jme3.math.FastMath.rand;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.scene.shape.Sphere.TextureMode;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture.WrapMode;


public class FindOto extends SimpleApplication implements ActionListener{

  public static void main(String args[]) {
    FindOto app = new FindOto();
    app.start();
  }

  
  private BulletAppState bulletAppState;

  /** Prepare Materials */
  Material wall_mat;
  Material stone_mat;
  Material floor_mat;

  /** Prepare geometries and physical nodes for bricks and cannon balls. */
  private RigidBodyControl    brick_phy;
  private static final Box    box;
  private RigidBodyControl    ball_phy;
  private static final Sphere sphere;
  private RigidBodyControl    floor_phy;
  private static final Box    floor;
  private RigidBodyControl    fire_phy;

  /** dimensions used for bricks and wall */
  private static final float brickLength = 0.48f;
  private static final float brickWidth  = 0.24f;
  private static final float brickHeight = 0.12f;
  
  /** Player Control, walk directions and camera directions **/
  private CharacterControl player;
  private Vector3f walkDirection = new Vector3f();
  private boolean left = false, right = false, up = false, down = false;
  private Vector3f camDir = new Vector3f();
  private Vector3f camLeft = new Vector3f();
  
  /*** Enemy geometries control and materials**/
  private Spatial enemyModel;
  private RigidBodyControl enemyControl;
  private Material enemyMat;
  
  //partical emitter
  ParticleEmitter fire;
  
  //sets number of cannonballs 
  private int cannonBallCount = 6;
  //cannonball text
  private BitmapText hudText;

  static {
    /** Initialize the cannon ball geometry */
    sphere = new Sphere(32, 32, 0.4f, true, false);
    sphere.setTextureMode(TextureMode.Projected);
    /** Initialize the brick geometry */
    box = new Box(brickLength, brickHeight, brickWidth);
    box.scaleTextureCoordinates(new Vector2f(1f, .5f));
    /** Initialize the floor geometry */
    floor = new Box(10f, 0.1f, 5f);
    floor.scaleTextureCoordinates(new Vector2f(3, 6));
  }
    

  @Override
  public void simpleInitApp() {
      viewPort.setBackgroundColor(new ColorRGBA (0.7f, 0.8f, 1f, 1f));
    /** Set up Physics Game */
    bulletAppState = new BulletAppState();
    stateManager.attach(bulletAppState);
   

 
    
    /** Initialize the scene, materials, and physics space and HUD */
    initMaterials();
    initWall1();
    initFloor();
    initPlatform();
    initCharacter();
    initEnemy();
    initKeys();
    initCrossHairs();
    cannonBallCount();
    initInstructions();
    
    

  }
  /**Initializes basic movement keys, shoots cannonballs and fireballs also wrote mapp to restart the game **/
  private void initKeys() {
    inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
    inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
    inputManager.addMapping("Up", new KeyTrigger(KeyInput.KEY_W));
    inputManager.addMapping("Down", new KeyTrigger(KeyInput.KEY_S));
    inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
    inputManager.addMapping("Restart", new KeyTrigger(KeyInput.KEY_0));
    inputManager.addMapping("Shoot",
            new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
    inputManager.addMapping("Fireball",
            new MouseButtonTrigger(MouseInput.BUTTON_RIGHT));
    inputManager.addListener(this, "Left");
    inputManager.addListener(this, "Right");
    inputManager.addListener(this, "Up");
    inputManager.addListener(this, "Down");
    inputManager.addListener(this, "Jump");
    inputManager.addListener(this, "Shoot");
    inputManager.addListener(this, "Fireball");
    inputManager.addListener(this, "Restart");

  }

  //initializes character collison, controls bulletappState
  public void initCharacter(){
    CapsuleCollisionShape capsuleShape = new CapsuleCollisionShape(.5f, 6f, 1);
    player = new CharacterControl(capsuleShape, 0.05f);
    player.setJumpSpeed(20);
    player.setFallSpeed(30);
    player.setGravity(new Vector3f(0,-30f,0));
    player.setPhysicsLocation(new Vector3f(0, 1f, 20f));
    bulletAppState.getPhysicsSpace().add(player);
  }
  
  //initializes Oto collision, rigidbody and creates random spawn point
  public void initEnemy(){
      //random int from -9 to 9 in range of platform distance
      int spawnPoint = randomInRange(-9,9);
      enemyModel = assetManager.loadModel("Models/Oto/Oto.mesh.xml");
      enemyModel.setLocalScale(.20f);
      enemyModel.setLocalTranslation(spawnPoint, 0f, 3f);
      enemyControl = new RigidBodyControl(50f);
      enemyModel.addControl(enemyControl);
      bulletAppState.getPhysicsSpace().add(enemyModel);
      rootNode.attachChild(enemyModel);
      
  }
  //random int generator
  int randomInRange(int min, int max) {
    return rand.nextInt((max - min) + 1) + min;
}

  /** Initialize the materials used in tscene. */
  public void initMaterials() {
    wall_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    TextureKey key = new TextureKey("Textures/Terrain/BrickWall/BrickWall.jpg");
    key.setGenerateMips(true);
    Texture tex = assetManager.loadTexture(key);
    wall_mat.setTexture("ColorMap", tex);

    stone_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    TextureKey key2 = new TextureKey("Textures/Terrain/Rock/Rock.PNG");
    key2.setGenerateMips(true);
    Texture tex2 = assetManager.loadTexture(key2);
    stone_mat.setTexture("ColorMap", tex2);

    floor_mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
    TextureKey key3 = new TextureKey("Textures/Terrain/Pond/Pond.jpg");
    key3.setGenerateMips(true);
    Texture tex3 = assetManager.loadTexture(key3);
    tex3.setWrap(WrapMode.Repeat);
    floor_mat.setTexture("ColorMap", tex3);
  }

  /** Make enemy floor solid. */
  public void initFloor() {
    Geometry floor_geo = new Geometry("Floor", floor);
    floor_geo.setMaterial(floor_mat);
    floor_geo.setLocalTranslation(0, -0.1f, 0);
    this.rootNode.attachChild(floor_geo);
    /* Make the floor physical with mass 0.0f! */
    floor_phy = new RigidBodyControl(0.0f);
    floor_geo.addControl(floor_phy);
    bulletAppState.getPhysicsSpace().add(floor_phy);
  }
  
  /*Make player platform solid*/
   public void initPlatform() {
    Geometry floor_geo = new Geometry("Floor", floor);
    floor_geo.setMaterial(floor_mat);
    floor_geo.setLocalTranslation(0, -0.1f, 20);
    this.rootNode.attachChild(floor_geo);
    /* Make the floor physical with mass 0.0f! */
    floor_phy = new RigidBodyControl(0.0f);
    floor_geo.addControl(floor_phy);
    bulletAppState.getPhysicsSpace().add(floor_phy);
  }
  

  /** This loop builds a wall out of individual bricks. */
  public void initWall1() {
    float startpt = brickLength / 4;
    float height = 0;
    for (int j = 0; j < 10; j++) {
      for (int i =-9; i < 10; i++) {
        Vector3f vt =
         new Vector3f(i * brickLength * 2 + startpt, brickHeight + height, 4);
        makeBrick(vt);
      }
      startpt = -startpt;
      height += 2 * brickHeight;
    }
  }
  

  /** This method creates one individual physical brick. */
  public void makeBrick(Vector3f loc) {
    /** Create a brick geometry and attach to scene graph. */
    Geometry brick_geo = new Geometry("brick", box);
    brick_geo.setMaterial(wall_mat);
    rootNode.attachChild(brick_geo);
    /** Position the brick geometry  */
    brick_geo.setLocalTranslation(loc);
    /** Make brick physical with a mass > 0.0f. */
    brick_phy = new RigidBodyControl(2f);
    /** Add physical brick to physics space. */
    brick_geo.addControl(brick_phy);
    bulletAppState.getPhysicsSpace().add(brick_phy);
  }

  //creates cannonball
   public void makeCannonBall() {
    /** Create a cannon ball geometry and attach to scene graph. */
    Geometry ball_geo = new Geometry("cannon ball", sphere);
    ball_geo.setMaterial(stone_mat);
    rootNode.attachChild(ball_geo);
    /** Position the cannon ball  */
    ball_geo.setLocalTranslation(cam.getLocation());
    /** Make the ball physcial with a mass > 0.0f */
    ball_phy = new RigidBodyControl(10f);
    /** Add physical ball to physics space. */
    ball_geo.addControl(ball_phy);
    bulletAppState.getPhysicsSpace().add(ball_phy);
    /** Accelerate the physcial ball to shoot it. */
    ball_phy.setLinearVelocity(cam.getDirection().mult(30));
    cannonBallCount--;
  }
   
   //creates fireball
   public void makeFireBall() {
        fire =
            new ParticleEmitter("Emitter", ParticleMesh.Type.Triangle, 30);
    Material mat_red = new Material(assetManager,
            "Common/MatDefs/Misc/Particle.j3md");
    mat_red.setTexture("Texture", assetManager.loadTexture(
            "Effects/Explosion/flame.png"));
    fire.setMaterial(mat_red);
    fire.setImagesX(2);
    fire.setImagesY(2); // 2x2 texture animation
    fire.setEndColor(  new ColorRGBA(1f, 0f, 0f, 1f));   // red
    fire.setStartColor(new ColorRGBA(1f, 1f, 0f, 0.5f)); // yellow
    fire.getParticleInfluencer().setInitialVelocity(new Vector3f(0, 2, 0));
    fire.setStartSize(1.5f);
    fire.setEndSize(0.1f);
    fire.setGravity(0, 0, 0);
    fire.setLowLife(1f);
    fire.setHighLife(3f);
    fire.getParticleInfluencer().setVelocityVariation(0.3f);
    fire.setShape(new EmitterSphereShape(Vector3f.ZERO,2f));
    rootNode.attachChild(fire);
    fire_phy = new RigidBodyControl(5f);
    fire.setLocalTranslation(cam.getLocation());
    fire.addControl(fire_phy);
    bulletAppState.getPhysicsSpace().add(fire_phy);
    /** Accelerate the physcial ball to shoot it. */
    fire_phy.setLinearVelocity(cam.getDirection().mult(25));
    
   }
   
  /** A plus sign used as crosshairs to help the player with aiming.*/
  protected void initCrossHairs() {
    guiNode.detachAllChildren();
    guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
    BitmapText ch = new BitmapText(guiFont, false);
    ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
    ch.setText("+");        // fake crosshairs :)
    ch.setLocalTranslation( // center
      settings.getWidth() / 2 - guiFont.getCharSet().getRenderedSize() / 3 * 2,
      settings.getHeight() / 2 + ch.getLineHeight() / 2, 0);
    guiNode.attachChild(ch);
  }
  
      @Override
    public void onAction(String binding, boolean isPressed, float tpf) {
    if (binding.equals("Left")) {
      left = isPressed;
    } else if (binding.equals("Right")) {
      right= isPressed;
    } else if (binding.equals("Up")) {
      up = isPressed;
    } else if (binding.equals("Down")) {
      down = isPressed;
    } else if (binding.equals("Jump")) {
    
      if (isPressed) { player.jump(new Vector3f(0,20f,0));}
    } 
    if (binding.equals("Shoot") && !isPressed) {
        makeCannonBall();
      }
    if (binding.equals("Fireball")&& !isPressed){
        makeFireBall();
    }
    if (binding.equals("Restart")&& !isPressed){
//        restart();
        
    }
    
  }
    
    //sets text if player wins
    public void youWin(){
        guiNode.detachAllChildren();
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(guiFont, false);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("You Win \n"
                );         

        ch.setLocalTranslation( // center
                settings.getWidth() / 2 - guiFont.getCharSet().getRenderedSize() / 3 * 2,
                settings.getHeight() / 2 + ch.getLineHeight() / 2, 0);
        guiNode.attachChild(ch);
        
    }
    //sets text if player loses
    public void youLose(){
        guiNode.detachAllChildren();
        guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
        BitmapText ch = new BitmapText(guiFont, false);
        ch.setSize(guiFont.getCharSet().getRenderedSize() * 2);
        ch.setText("You Lose \n"
                + "Try Again");        

        ch.setLocalTranslation( // center
                settings.getWidth() / 2 - guiFont.getCharSet().getRenderedSize() / 3 * 2,
                settings.getHeight() / 2 + ch.getLineHeight() / 2, 0);
        guiNode.attachChild(ch);
        
    }
    
    //initializes instructions to GUI
    public void initInstructions(){
        BitmapText inst = new BitmapText(guiFont, false);
        inst.setSize(30);
        inst.setColor(ColorRGBA.White);
        inst.setText("Push Oto off the platform before you run out of cannonballs\n"
                + "Left Click to shoot");
        inst.setLocalTranslation(1000,1500,0);
        guiNode.attachChild(inst);
        
        
    }
    
    //initializes cannonball count
    public void cannonBallCount(){
        hudText = new BitmapText(guiFont, false);
        hudText.setSize(50f);      // font size
        hudText.setColor(ColorRGBA.Blue);                             // font color
        hudText.setText("Cannonballs left: "+ cannonBallCount);             // the text
        hudText.setLocalTranslation(100, 400, 0); // position
        guiNode.attachChild(hudText); 
    }
    
    //updates cannonball count
    public void cannonBallCountUpdate(){
        guiNode.detachChild(hudText);
        hudText = new BitmapText(guiFont, false);
        hudText.setSize(50f);      // font size
        hudText.setColor(ColorRGBA.Blue);                             // font color
        hudText.setText("Cannonballs left: "+ cannonBallCount);             // the text
        hudText.setLocalTranslation(100, 800, 0); // position
        guiNode.attachChild(hudText); 
    }
     
     
  
    
  @Override
    public void simpleUpdate(float tpf) {
        camDir.set(cam.getDirection()).multLocal(0.6f);
        camLeft.set(cam.getLeft()).multLocal(0.4f);
        walkDirection.set(0, 0, 0);
        if (left) {
            walkDirection.addLocal(camLeft);
        }
        if (right) {
            walkDirection.addLocal(camLeft.negate());
        }
        if (up) {
            walkDirection.addLocal(camDir);
        }
        if (down) {
            walkDirection.addLocal(camDir.negate());
        }
        player.setWalkDirection(walkDirection);
        cam.setLocation(player.getPhysicsLocation());
        if(enemyModel.getLocalTranslation().getY()<-5){
            youWin();
        }
        if(player.getPhysicsLocation().y<-5 || cannonBallCount<1){
            youLose();
        }
        //updates cannonballcount to GUI
        cannonBallCountUpdate();
    }
    


}//end of findOto.java