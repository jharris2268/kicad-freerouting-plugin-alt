import socket, json, struct


def read_exact(sock, ln):
    res = b''
    while len(res) < ln:
        x = sock.read(ln - len(res))
        if not x:
            raise Exception("read %d bytes from %s failed" % (ln,sock))
        res+=x
    return res



class MessageReceiver:
    def __init__(self, board_handler, message_handler, responses, get_process):
        self.board_handler = board_handler
        self.message_handler = message_handler
        self.responses=responses
        
        self.get_process = get_process 


    def read_all(self):
        while True:
            if not self.read_next():
                return
    
    
        
    
    def read_next(self):
        proc = self.get_process()
        if proc is None:
            return False

        a,b = read_exact(proc.stdout, 2)
        
        ln = a*256+b
        msg_bytes = read_exact(proc.stdout, ln)
        
        try:
            msg = json.loads(msg_bytes.decode('utf-8'))
            
            if msg['type'] == 'message':
                self.message_handler(msg)
                if msg.get('wait_reply')==True:
                    proc.stdin.write(b'\0\1\1')
                    proc.stdin.flush()
                
                return True
                
            elif msg['type'] == 'finished':
                if msg.get('wait_reply')==True:
                    proc.stdin.write(b'\0\1\1')
                    proc.stdin.flush()
                self.board_handler(None)
                self.message_handler(None)
                print()
                return False
                
            elif msg['type'] == 'board_notify':
                if msg.get('wait_reply')==True:
                    proc.stdin.write(b'\0\1\1')
                    proc.stdin.flush()
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
                
                proc.stdin.write(zz)
                proc.stdin.flush()
                
                return True
            else:
                print("??", msg)
                
            
            return True
        except Exception as ex:
            print("problem with ",a,b,ln,msg_bytes,len(msg_bytes))
            raise ex
