package eu.mihosoft.freerouting.logger;

import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
//import java.lang.ClassNotFoundException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.locks.ReentrantLock;
import org.json.JSONStringer;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import eu.mihosoft.freerouting.interactive.InteractiveActionThread;

public class MessageServer {
    // Static variable reference of single_instance
    // of type Singleton
    private static MessageServer single_instance = null;
    
    //private ServerSocket server;
    //private Socket socket;
    //socket server port on which it will listen
    //private int port = 12345;
    // Declaring a variable of type String
    private DataInputStream ois;
    private DataOutputStream oos;
    
    private long _number_calls = 0;
    private long _pack_message_time = 0;
    private long _send_message_time = 0;
    private long _recv_reply_time = 0;
    private long _recv_proc_time = 0;
    
    private long start_time;
    
    private final ReentrantLock lock = new ReentrantLock();
    private WeakReference<InteractiveActionThread> interactive_action_thread=null;
    
    public synchronized void set_interactive_action_thread(InteractiveActionThread thread) {
        try{ 
            send_message("plop","set_interactive_action_thread",false);
        } catch (Exception e) {}
        interactive_action_thread = new WeakReference(thread);
    }
    
    public synchronized  boolean request_stop() {
        
        if (interactive_action_thread!=null) {
            if (interactive_action_thread.get() != null) {
                interactive_action_thread.get().request_stop();
                return true;
            }
        }
        return false;
    }
    
    public synchronized void send_json_no_reply(JSONStringer obj) throws IOException {
        
        lock.lock();
        try {
            send_json_common(obj);
        } catch (IOException ex) {
            System.out.println("MessageServer failed, exit");
            System.exit(1);
        } finally {
            lock.unlock();
        }
        
    }
    
    private void send_json_common(JSONStringer obj) throws IOException {
        
        long t0 = System.nanoTime();
        String msg = obj.toString();
        
        long t1 = System.nanoTime();
        //oos.writeUTF(msg);
        byte[] bytes = msg.getBytes("UTF8");        
        oos.writeInt(bytes.length);
        oos.write(bytes,0,bytes.length);
        
        oos.flush();
        long t2= System.nanoTime();
        
        _pack_message_time += (t1-t0);
        _send_message_time += (t2-t1);
    }
    
    private synchronized byte[] send_json_receive_reply(JSONStringer obj) {
        lock.lock();
        byte[] reply=null;
        
        try {        
            send_json_common(obj);
        
            long t2= System.nanoTime();
            int ln = ois.readInt();
            reply = new byte[ln];
            ois.readFully(reply);
            long t3 = System.nanoTime();
            
            _recv_reply_time += (t3-t2);
        } catch (IOException ex) {
            System.out.println("MessageServer failed, exit");
            System.exit(1);
        } finally {
            lock.unlock();
        }
        
        return reply;
    }
    
    public synchronized byte send_json_expect_one_byte_response(JSONStringer obj) throws Exception {
        byte[] reply = send_json_receive_reply(obj);
        if (reply.length!=1) {
            throw new Exception("recieved "+reply.length+" bytes, expected 1");
        }
        return reply[0];
            
    }
    
    public synchronized JSONObject send_json_expect_json_reply(JSONStringer obj) throws Exception {
        JSONObject reply=null;
        byte[] reply_bytes = send_json_receive_reply(obj);
        try {
            long t3 = System.nanoTime();
            
            String reply_string = new String(reply_bytes, "UTF-8");
            reply = new JSONObject(reply_string);       
            long t4 = System.nanoTime();
            
            _recv_proc_time += (t4-t3);
        } catch (IOException ex) {
            throw new Exception("recieved "+reply_bytes.length+" bytes, expected JSON object");
        }
        
        return reply;
    }
    
    public synchronized boolean send_finished() throws IOException {
        JSONStringer obj = start_message("finished");
                
        //obj.key("wait_reply").value(true);
        obj.endObject();
        send_json_no_reply(obj);
        return true;
        //return send_json_expect_one_byte_response(obj)=='y';
        
    }
    
    public synchronized JSONStringer start_message(String type) {
        JSONStringer message_obj = new JSONStringer();
        
        message_obj.object();
        message_obj.key("index").value(_number_calls);
        message_obj.key("time").value(System.nanoTime() - start_time);
        message_obj.key("type").value(type);
        
        _number_calls += 1;
        return message_obj;
    }
    
    public synchronized void send_message(String type, String msg, boolean wait_reply) throws Exception {
        
        JSONStringer message_obj = start_message("message");
        
        message_obj.key("msg_type").value(type);
        message_obj.key("msg").value(msg);  
        if (wait_reply) {
            message_obj.key("wait_reply").value(true);
        }
        message_obj.endObject();
        
        if (wait_reply) {
            byte reply = send_json_expect_one_byte_response(message_obj);
                    
            
            if (reply != '\1') {
                if (!request_stop()) {
                
                    throw new Exception("incorrect reply " + reply);
                }
            }
        } else {
            send_json_no_reply(message_obj);
        }
        
    }
    public static synchronized String stats() {
        try {
            MessageServer srv =  getInstance();
            double total = (srv._pack_message_time+srv._send_message_time+srv._recv_reply_time)/1000000000.0;
            String res = "Message server: "+srv._number_calls+" calls\n"+
                "  pack message: "+(srv._pack_message_time/1000000000.0)+"s\n"+
                "  send message: "+(srv._send_message_time/1000000000.0)+"s\n"+
                "  recv reply:   "+(srv._recv_reply_time/1000000000.0)+"s\n"+ 
                "  TOTAL:        "+total+"s\n"+
                "    per call:   "+(total/srv._number_calls)+"s";
            
            return res;
        } catch (IOException e) {
            return "Message server stats failed?";
        }
    }
 
    // Constructor
    // Here we will be creating private constructor
    // restricted to this class itself
    private MessageServer() throws IOException
    {
        
        /*server = new ServerSocket(port);
        socket = server.accept();
        
        socket.setSendBufferSize(8*1024*1024);
        socket.setReceiveBufferSize(8*1024*1024);
        ois = new DataInputStream(socket.getInputStream());
        oos = new DataOutputStream(socket.getOutputStream());*/
        
        ois = new DataInputStream(System.in);
        oos = new DataOutputStream(System.out);
        
        /*
        try {
            this.send_message("plop","MessageServer init, SendBufferSize=" + socket.getSendBufferSize() + ", ReceiveBufferSize=" + socket.getReceiveBufferSize(), true);
        } catch (Exception e) {
            //pass
        }*/
        
        start_time = System.nanoTime();
    }
 
    // Static method
    // Static method to create instance of Singleton class
    public static synchronized MessageServer getInstance()  throws IOException
    {
        if (single_instance == null)
            single_instance = new MessageServer();
 
        return single_instance;
    }
}
