package pt.novaims.slickcontroller.application;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.example.slickcontroller.R;

public class MainActivity extends ActionBarActivity implements SensorEventListener {

	private final int PORT = 8888;
    private final String udpMessage = "PONG_REQUEST";
    private final String UDP_REPLY_KEYWORD = "PONG_CONNECTING_DATA";
    private final String MOVE_LEFT = "slowLeft";
    private final String HARD_LEFT = "fastLeft";
    private final String MOVE_RIGHT = "slowRight";
    private final String HARD_RIGHT = "fastRight";
    
    //UDP and TCP communication
	private DatagramSocket client_socket;
	private Socket socket = null;
	private OutputStream socketOutput = null;
    private String host;
    private int port;
	byte[] send_data = new byte[1024];
	byte[] receiveData = new byte[1024];
	private String udpReply;
	private boolean connected = false;

	//Accelerometer
	private SensorManager senSensorManager;
	private Sensor senAccelerometer;
	
	//UI-elements
	Button connectButton;
	TextView xValue;
	TextView yValue;
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_main);
        } catch (Resources.NotFoundException e) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            return;
        }
        
        connectButton = (Button)findViewById(R.id.connectButton);
        xValue = (TextView)findViewById(R.id.xValue);
        yValue = (TextView)findViewById(R.id.yValue);
        xValue.setVisibility(View.INVISIBLE);
        yValue.setVisibility(View.INVISIBLE);
        
        connectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				try {
					if(!connected) {
						connected = true;
						UDPclientStart();
						connectButton.setText("Stop");
						xValue.setVisibility(View.VISIBLE);
				        yValue.setVisibility(View.VISIBLE);
					} else {
						connected = false;
						if(socketOutput != null) {
							socketOutput.close();
						}
						if(socket != null) {
							socket.close();
						}
						connectButton.setText("Connect");
						xValue.setVisibility(View.INVISIBLE);
				        yValue.setVisibility(View.INVISIBLE);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
        });    
        senSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = senSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
    
    public void UDPclientStart() throws IOException{ 
	    new UDPClientTask().execute();
    }    
 
    @Override
    protected void onDestroy() {
        super.onDestroy();
         
        if(socket != null & socket.isConnected()){
            try {
            	socketOutput.close();
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
    }

    public void onSensorChange(SensorEvent sensorEvent) {
    	Log.d("SensorChange", "Sensor change");
    }
    
	@Override
	public void onSensorChanged(SensorEvent sensorEvent) {
		System.out.println("Sensor changed");
		
	    Sensor mySensor = sensorEvent.sensor;
	    float x;
	    float y;
	    
	    if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
	        x = sensorEvent.values[0];
	        xValue.setText(String.valueOf(x));
	        y = sensorEvent.values[1];
	        yValue.setText(String.valueOf(y));
	        
	        if(socket != null && socket.isConnected()) {
	        	String msg = "";
	        	if (y < - 1 && y > -4) {
	        		msg =  MOVE_LEFT;
	        	} else if(y < - 4) {
	        		msg = HARD_LEFT;
	        	} else if (y > 1 && y < 4){
	        		msg = MOVE_RIGHT;
	        	} else if (y > 4){
	        		msg = HARD_RIGHT;
	        	}
	        	
	        	msg = msg + "\n";       

				try {
					String sentMsg = new String(msg.getBytes("UTF-8"));
					System.out.println("Sent accelerometer data: " + sentMsg);
			        socketOutput.flush();
			        socketOutput.write(msg.getBytes("UTF-8"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
		        
		    }
	    }    
   	}
	
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		System.out.println("Accuracy changed");
	}
	
	protected void onPause() {
	    super.onPause();
	    senSensorManager.unregisterListener(this);
	}
	
	protected void onResume() {
	    super.onResume();
	    senSensorManager.registerListener(this, senAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	}
	
	class UDPClientTask extends AsyncTask<String, Void, String>{
		
		@Override
		protected String doInBackground(String... params) {
			boolean udpRequestActive = true;
			String success = "false";
		   	 
			try {
				client_socket = new DatagramSocket(2362);
				InetAddress IPAddress =  InetAddress.getByName("255.255.255.255"); 
			    send_data = udpMessage.getBytes();
			     
			    while (udpRequestActive) {
			
			        DatagramPacket send_packet = new DatagramPacket(send_data, udpMessage.length(), IPAddress, PORT);
			        client_socket.send(send_packet);                      
			
			   		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			   		client_socket.receive(receivePacket);
			   		
			   		udpReply = new String(receivePacket.getData()).trim();
			   		System.out.println("UDP processing, received message: " + udpReply);
			   		
			   		if(udpReply.equals(UDP_REPLY_KEYWORD)) {
			   	   		System.out.println("FROM SERVER:" + receivePacket.getAddress() + ": " + receivePacket.getPort());
			   	   		host = receivePacket.getAddress().toString().replace("/", "");
			   	   		port = receivePacket.getPort();
			   	   		udpRequestActive = false;
			   	   		success = "true";
			   	   		client_socket.close();
			   		}
			     }
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return success;
		}
		
		@Override
	     protected void onPostExecute(String result) {
			if (result.equals("true")) {
				TCPConnectionTask();
			}	
		}
	}
	
	public void TCPConnectionTask()  {
		final Runnable tcpRunnable = new Runnable() {
		    public void run() {		
		    	System.out.println("TCP connection task started");
			
				try {
					socket = new Socket(host, port);
				    socketOutput = socket.getOutputStream();
					//PrintWriter pw = new PrintWriter(socketOutput);			
					
				} catch (IOException e) {
					e.printStackTrace();
				}	
		    }
		};
		new Thread(tcpRunnable).start();		
	}
}


