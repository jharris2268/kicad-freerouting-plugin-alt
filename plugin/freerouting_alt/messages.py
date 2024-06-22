import socket, json, struct


def recv_exact_xx(sock, ln):
    res = b''
    while len(res) < ln:
        x = sock.recv(ln - len(res))
        if not x:
            raise Exception("socket closed?")
        res+=x
    return res

def read_exact(sock, ln):
    res = b''
    while len(res) < ln:
        x = sock.read(ln - len(res))
        if not x:
            raise Exception("socket closed?")
        res+=x
    return res



class MessageReceiver:
    def __init__(self, board_handler, message_handler, responses, get_process):
        self.board_handler = board_handler
        self.message_handler = message_handler
        self.responses=responses
        
        self.get_process = get_process 
        #self.init_socket()
    
    def init_socket(self):
        
        self.sock=socket.socket()
        self.sock.connect(('localhost',12345))
        print("sock: ", self.sock)
    def read_all(self):
        while True:
            if not self.read_next():
                return
    
    
        
    
    def read_next(self):
        proc = self.get_process()
        if proc is None:
            return False
            
        #a,b = recv_exact(self.sock, 2)
        a,b = read_exact(proc.stdout, 2)
        
        ln = a*256+b
        #msg_bytes = recv_exact(self.sock, ln)
        msg_bytes = read_exact(proc.stdout, ln)
        
        try:
            msg = json.loads(msg_bytes.decode('utf-8'))
            
            if msg['type'] == 'message':
                self.message_handler(msg)
                if msg.get('wait_reply')==True:
                    print("!!!")
                    #self.sock.sendall(b'\1')
                
                return True
                
            elif msg['type'] == 'finished':
                if msg.get('wait_reply')==True:
                    #self.sock.sendall(b'\1')
                    print("!!!")
                self.board_handler(None)
                self.message_handler(None)
                print()
                return False
                
            elif msg['type'] == 'board_notify':
                if msg.get('wait_reply')==True:
                    #self.sock.sendall(b'\1')
                    print("!!!")
                self.board_handler(msg)
                
                return True
                
            elif msg['type'] == 'request':
                
                jj = {}
                if msg['request_type'] in self.responses:
                    rr = self.responses[msg['request_type']]
                    
                    
                    if callable(rr):
                        jj = rr(msg)
                    else:
                        jj = rr
                        
                jjp = json.dumps(jj).encode('utf-8')
                
                print("request", msg, "response", len(jjp), jjp[:50])
                zz = struct.pack('>L', len(jjp))+jjp
                #self.sock.sendall(zz)
                proc.stdin.write(zz)
                proc.stdin.flush()
                
                return True
            else:
                print("??", msg)
                
            
            return True
        except Exception as ex:
            print("problem with ",a,b,ln,msg_bytes,len(msg_bytes))
            raise ex
