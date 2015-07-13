import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import processing.serial.*; 
import oscP5.*; 
import netP5.*; 
import ddf.minim.*; 
import ddf.minim.analysis.*; 
import java.util.Map; 
import java.util.concurrent.ConcurrentMap; 
import java.util.concurrent.ConcurrentHashMap; 
import com.leapmotion.leap.Controller; 
import com.leapmotion.leap.Finger; 
import com.leapmotion.leap.Frame; 
import com.leapmotion.leap.Hand; 
import com.leapmotion.leap.Tool; 
import com.leapmotion.leap.Vector; 
import com.leapmotion.leap.processing.LeapMotion; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class poking_main_13jul2015 extends PApplet {















 

Minim minim;
AudioInput in;
AudioBuffer buf;
FFT         fft;

int inpitch, prev_inpitch, delta_inpitch;
int inheight, prev_inheight, delta_inheight;
int v1 = 0;
int v10 = 0;
int v1buf = 0;
LeapMotion leapMotion;
ConcurrentMap<Integer, Integer> fingerColors;
ConcurrentMap<Integer, Integer> toolColors;
ConcurrentMap<Integer, Vector> fingerPositions;
ConcurrentMap<Integer, Vector> toolPositions;
int fingers = 0;
boolean beginRecording;
ArrayList<Float> data = new ArrayList<Float>();

Serial myPort0, myPort1;
OscP5 oscP5;

float p = 0.5f;
float v = 0.5f;
float w = 0.5f;
float h = 0.5f; 
float d = 0.5f;

int filnum = 101;
float process[] = new float[1024];
float filtered[] = new float[1024-filnum];


public void setup()
{
  size(800, 450);
  myPort0 = new Serial(this, "/dev/tty.usbmodem14511", 9600);
  myPort1 = new Serial(this, "/dev/tty.usbmodem14521", 9600);
  
  oscP5 = new OscP5(this, 12001);
  
  minim = new Minim(this);
  in = minim.getLineIn();
  fft = new FFT( 1024, 44100 );
  
  ellipseMode(CENTER);
  leapMotion = new LeapMotion(this);
  fingerColors = new ConcurrentHashMap<Integer, Integer>();
  toolColors = new ConcurrentHashMap<Integer, Integer>();
  fingerPositions = new ConcurrentHashMap<Integer, Vector>();
  toolPositions = new ConcurrentHashMap<Integer, Vector>();
  
  beginRecording = false;
}

public void draw()
{
  background(255);
  fft.forward(in.left);
  int maxfrequency = 0;
  float maxspectrum = 0.0f; 
  inpitch = 0;
  inheight =0;
  v1 = 0;
  for(int i = 0; i < fft.specSize(); i++)
  {
    if(fft.getBand(i)>maxspectrum){
      maxspectrum = fft.getBand(i);
      maxfrequency = i;
    }
    line( i, height, i, height - fft.getBand(i)*8 );
  }
  stroke(255,0,0);
  line(maxfrequency, height, maxfrequency, height - maxspectrum*8);
  stroke(0);
  float mult = 20;
  if(maxspectrum > 40.0f && maxfrequency < 60) {
    inpitch = maxfrequency;
    delta_inpitch = inpitch - prev_inpitch; 
    prev_inpitch = inpitch;
  }
  
  /* audio reactive impl. */
  for(int i=0; i<in.bufferSize(); i++){
    process[i] = in.left.get(i); 
    if(process[i]<0.0f) process[i] *= -1;
  }
  for (int i=0; i<in.bufferSize()-filnum; i++){
  filtered[i] = 0.0f;
    for (int j=0; j<filnum+1; j++){
      filtered[i] += process[i+j]/filnum;
    } 
  }
  for(int i = 0; i < in.bufferSize()-filnum-1; i++){
    line(i, height - filtered[i]*500, i+1, height - filtered[i+1]*500); 
  }
  int output = PApplet.parseInt(filtered[0]*200*v);
  if(output>30) output = 30;
// println(p, v, w, h, d);
 // if(frameCount%5 == 0) myPort0.write(30-output);
  //println(output);
  
  
  /* gesture reactive impl. */ 
  if(fingers==1){
    if(!beginRecording){
      for (int i = data.size() - 1; i >= 0; i--) {
        data.remove(i); 
      }
    }
    beginRecording = true;
  }else{
    beginRecording = false;
  }
  
  int write_ac = 40;
  int write_zc = 0;
  
  for (Map.Entry entry : fingerPositions.entrySet())
  {
    Integer fingerId = (Integer) entry.getKey(); 
    Vector position = (Vector) entry.getValue(); 
    
    if(fingers==1 && fingerId%10==1) {
      // ellipse(leapMotion.leapToSketchX(position.getZ()), leapMotion.leapToSketchY(position.getY()), 24.0, 24.0);
      data.add(leapMotion.leapToSketchX(position.getZ()) );
      int ac = PApplet.parseInt(position.getZ());
      if(ac>50) ac = 50; 
      if(ac<-50) ac = -50;
      write_ac = PApplet.parseInt(map(ac, -50, 50, 0, 40));
      // myPort0.write(write_ac);
      
      int zc = PApplet.parseInt(position.getY());
      inheight = zc;
      delta_inheight = inheight - prev_inheight;
      if(abs(delta_inheight)>40) delta_inheight = 0;
      v10 += delta_inheight;
      prev_inheight = inheight;
      if(zc<40) zc = 40;
      if(zc>120) zc = 120; 
      write_zc = PApplet.parseInt(map(zc, 40, 120, 0, 60));
      //myPort1.write(write_zc);
      
    }
  }
  
  for(int i=0; i<data.size()-1; i++){
    line(width-i, data.get(data.size()-(i+1)), width-(i+1), data.get(data.size()-(i+2)));
  }
  
  
  int v0 = PApplet.parseInt(((30-output)*v + write_ac*d));
  if(v0 > 30) v0 = 30;
  myPort0.write(v0);
  
  println("p", inpitch);
  println("h", inheight);
  // v1 = v1;
  // println("v1", v1);
  if(inpitch != 0) v1 += PApplet.parseInt(inpitch*p);
  if(inheight !=0) v1 += PApplet.parseInt(inheight*0.2f*h);
  println(v1);
  
  if(frameCount % 15 == 0 && v1>0){
    
    myPort1.write(v1);
  }
  
  
}

public void keyPressed()
{
  if(key == '1'){
    myPort1.write(0);
  }
  if(key == '2'){
    myPort1.write(1);
  }
  if(key == '3'){
    myPort1.write(2);
  }
  if(key == '4'){
    myPort1.write(3);
  }
  if(key == '5'){
    myPort1.write(4);
  }
  if(key == '6'){
    myPort1.write(20);
  }
  
  
  if(key == 'z'){
    myPort0.write(0);
  }
  if(key == 'x'){
    myPort0.write(5);
  }
  if(key == 'c'){
    myPort0.write(10);
  }
  if(key == 'v'){
    myPort0.write(15);
  }
  if(key == 'b'){
    myPort0.write(20);
  }
}


public void oscEvent(OscMessage theOscMessage) 
{
  print("### received an osc message.");
  // print(" addrpattern: "+theOscMessage.addrPattern());
  // println(" typetag: "+theOscMessage.typetag());
  
  p = theOscMessage.get(0).floatValue();
  v = theOscMessage.get(1).floatValue();
  w = theOscMessage.get(2).floatValue();
  h = theOscMessage.get(3).floatValue();
  d = theOscMessage.get(4).floatValue();
}



public void onFrame(final Controller controller)
{
  fingers = countExtendedFingers(controller);
  // println(fingers);
  
  Frame frame = controller.frame();
  fingerPositions.clear();
  for (Finger finger : frame.fingers())
  {
    int fingerId = finger.id();
    int c = color(random(0, 255), random(0, 255), random(0, 255));
    fingerColors.putIfAbsent(fingerId, c);
    fingerPositions.put(fingerId, finger.tipPosition());
  }
  toolPositions.clear();
  for (Tool tool : frame.tools())
  {
    int toolId = tool.id();
    int c = color(random(0, 255), random(0, 255), random(0, 255));
    toolColors.putIfAbsent(toolId, c);
    toolPositions.put(toolId, tool.tipPosition());
  }
}


public int countExtendedFingers(final Controller controller)
{
  int fingers = 0;
  if (controller.isConnected())
  {
    Frame frame = controller.frame();
    if (!frame.hands().isEmpty())
    {
      for (Hand hand : frame.hands())
      {
        int extended = 0;
        for (Finger finger : hand.fingers())
        {
          if (finger.isExtended())
          {
            extended++;
          }
        }
        fingers = Math.max(fingers, extended);
      }
    }
  }
  return fingers;
}


  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "poking_main_13jul2015" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
