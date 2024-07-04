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
        self.cancel_set=False
        self.replies_sent=0

    def read_all(self):
        while True:
            if not self.read_next():
                return
    
    def cancel(self):
        self.cancel_set=True
    
    def handle_wait_reply(self, proc, msg):
        if msg.get('wait_reply')==True:
            print(f"\rsend reply (cancel_set={self.cancel_set}, replies_sent={self.replies_sent})",flush=True,end='')
            proc.stdin.write(b'\0\0\0\1\0' if self.cancel_set else b'\0\0\0\1\1')
            proc.stdin.flush()
            self.replies_sent+=1
        
    
    def read_next(self):
        proc = self.get_process()
        if proc is None:
            return False

        
        ln,msg_bytes=None,None
        
        
        try:
            
            ln, = struct.unpack('>L',read_exact(proc.stdout, 4))
            msg_bytes = read_exact(proc.stdout, ln)
            
            msg = json.loads(msg_bytes.decode('utf-8'))
            
            if msg['type'] == 'message':
                self.message_handler(msg)
                self.handle_wait_reply(proc, msg)
                return True
                
            elif msg['type'] == 'finished':
                self.handle_wait_reply(proc, msg)
                self.board_handler(None)
                self.message_handler(None)
                print()
                return False
                
            elif msg['type'] == 'board_notify':
                self.handle_wait_reply(proc, msg)
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
            print("problem with ",ln,msg_bytes,len(msg_bytes or []))
            #if not self.get_process() is None:
            #    print("problem with ",ln,msg_bytes,len(msg_bytes or []))
            #    raise ex
        return False
