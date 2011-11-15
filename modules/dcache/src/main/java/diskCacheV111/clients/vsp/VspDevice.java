// $ID$
//
package diskCacheV111.clients.vsp ;

import java.io.* ;
import java.net.* ;
import java.util.* ;

import diskCacheV111.util.* ;
import diskCacheV111.movers.* ;


public class      VspDevice
       implements Runnable {

   private Hashtable _requestHash = new Hashtable() ;
   private ServerSocket _listen   = null ;
   private Socket       _door     = null ;
   private BufferedReader _in     = null ;
   private PrintWriter    _out    = null ;
   private Thread         _commandThread = null ;
   private Thread         _serviceThread = null ;
   private boolean        _debug  = true ;
   private String         _host   = null ;
   private int            _port   = 0 ;
   private int            _sessionId = 1 ;

   private boolean        _online    = false ;
   private boolean        _finished  = false ;
   private boolean        _controlConnectionClosed  = false ;

   private synchronized int nextSessionId(){ return _sessionId++ ; }

   private static final int IDLE  = 0  ;
   private static final int DONE  = 1 ;
   private static final int FAILED = 2 ;
   private static final int CONNECTED = 4 ;

   public static final int IOCMD_WRITE     = 1 ;
   public static final int IOCMD_READ      = 2 ;
   public static final int IOCMD_SEEK      = 3 ;
   public static final int IOCMD_CLOSE     = 4 ;
   public static final int IOCMD_INTERRUPT = 5 ;
   public static final int IOCMD_ACK       = 6 ;
   public static final int IOCMD_FIN       = 7 ;
   public static final int IOCMD_DATA      = 8 ;
   public static final int IOCMD_LOCATE    = 9 ;
   private static final int IOCMD_STATUS        = 10 ;
   private static final int IOCMD_SEEK_AND_READ = 11 ;
   private static final int IOCMD_SEEK_SET      = 0 ;
   private static final int IOCMD_SEEK_CURRENT  = 1 ;
   private static final int IOCMD_SEEK_END      = 2 ;

   private static final int IO_POSIX   =  1 ;
   private static final int IO_STREAM  =  2 ;

   public void say(String x){
      if( _debug )System.out.println(x);
   }
   public void setDebugOutput( boolean debug ){ _debug = debug ; }
   private class VspRequest implements VspConnection, Runnable  {
       private int     _sessionId = 0 ;
       private int     _state     = IDLE ;
       private String  _msg       = "" ;
       private Object  _syncLock  = new Object() ;
       private boolean _pending   = false ;
       private String  _pnfsId    = null ;
       private String  _mode      = null ;
       private byte [] _data      = null ;
       private int     _offset    = 0 ;
       private int     _size      = 0 ;
       private long    _bytesRead = 0 ;
       private int     _ioType    = 0 ;
       private long    _position  = 0 ;
       private long    _length    = 0 ;
       private Thread  _worker    = null ;
       private int     _ioMode    = IO_STREAM ;

       private VspDataTransferrable _transferrable = null ;
       private VspDataOutputStream  _dataOut       = null ;
       private DataInputStream     _dataIn   = null ;
       private VspIoFinishable     _callBack = null ;
       private boolean             _writeInThread  = false ;
       private boolean             _isSynchronous  = true ;
       private String [] _commands = {
          "Unkown" ,
          "IOCMD_WRITE" ,
          "IOCMD_READ" ,
          "IOCMD_SEEK" ,
          "IOCMD_CLOSE" ,
          "IOCMD_INTERRUPT" ,
          "IOCMD_ACK" ,
          "IOCMD_FIN" ,
          "IOCMD_DATA" ,
          "IOCMD_LOCATE" ,
          "IOCMD_STATUS" ,
          "IOCMD_SEEK_AND_READ"
       } ;
       //
       //  The public part ( read write close )
       //
       public void say(String x){
          if( _debug )System.out.println("("+_sessionId+")"+x);
       }
       public void setIoFinishable( VspIoFinishable callBack ){
          _callBack = callBack ;
       }
       public void setSynchronous( boolean sync ){
          _isSynchronous = sync ;
       }
       public void query() throws IOException {
          check() ;

          _ioType = IOCMD_LOCATE ;
          try{
             _dataOut.writeCmdLocate() ;
          }catch( IOException ee ){
             say("Problem in writeCmdLocate : "+ee ) ;
             setFailed( ee.toString() ) ;
          }
          if( _isSynchronous )sync() ;
       }

       public void seek( long position , int whence )
              throws IOException{
          check() ;

          _ioType = IOCMD_SEEK ;
          try{
             _dataOut.writeCmdSeek(position,whence) ;
          }catch( IOException ee ){
             say("Problem in writeCmdSeek : "+ee ) ;
             setFailed( ee.toString() ) ;
          }
          if( _isSynchronous )sync() ;
       }
       public void write( byte [] data , int offset , int size )
              throws IOException {

           check() ;

           _ioType = IOCMD_WRITE ;
           _data   = data ;
           _offset = offset ;
           _size   = size ;
           try{
              _dataOut.writeCmdWrite() ;
           }catch( IOException ee ){
              say("Problem in doTheWrite : "+ee ) ;
              setFailed( ee.toString() ) ;
           }
           if( _isSynchronous )sync() ;
       }
       public long read( long size , VspDataTransferrable consumer )
              throws IOException {

          if( consumer == null )
             throw new
             IllegalArgumentException( "Consumer must be specified" ) ;
          check() ;

          _transferrable = consumer ;

           _ioType = IOCMD_READ ;
           _ioMode = IO_STREAM ;
           _data   = new byte[64*1024] ;
           _offset = 0 ;
           _size   = (int)size ;
           _bytesRead = 0 ;
           say( "New read(stream)");

          try{
             _dataOut.writeCmdRead(size) ;
          }catch( IOException ee ){
             say("Problem in doTheRead : "+ee ) ;
             setFailed( ee.toString() ) ;
          }

          if( _isSynchronous ){
             sync() ;
             return _bytesRead ;
          }else{
             return -1 ;
          }
       }
       public long read( byte [] data , int offset , int size )
              throws IOException {

           check() ;

           _ioType = IOCMD_READ ;
           _ioMode = IO_POSIX ;
           _data   = data ;
           _offset = offset ;
           _size   = size ;
           _bytesRead = 0 ;
           say( "New read");
          try{
             _dataOut.writeCmdRead(size) ;
          }catch( IOException ee ){
             say("Problem in doTheRead : "+ee ) ;
             setFailed( ee.toString() ) ;
          }
          if( _isSynchronous ){
             sync() ;
             return _bytesRead ;
          }else{
             return -1 ;
          }

       }
       public long seek_and_read( byte [] data , int offset ,
                                  long file_offset , int file_whence , int size )
              throws IOException {

           check() ;

           _ioType = IOCMD_SEEK_AND_READ ;
           _ioMode = IO_POSIX ;
           _data   = data ;
           _offset = offset ;
           _size   = size ;
           _bytesRead = 0 ;
           say( "New seek and read read");
          try{
             _dataOut.writeCmdSeekAndRead(file_offset,file_whence,(long)size) ;
          }catch( IOException ee ){
             say("Problem in doTheRead : "+ee ) ;
             setFailed( ee.toString() ) ;
          }
          if( _isSynchronous ){
             sync() ;
             return _bytesRead ;
          }else{
             return -1 ;
          }

       }
       public void sync()throws IOException {
           synchronized( _syncLock ){

               while( ( _state == IDLE  ) || _pending )
                 try{
                    _syncLock.wait() ;
                 }catch( InterruptedException ie ){
                    throw new InterruptedIOException("Sync") ;
                 }

               if( _state == CONNECTED )return ;
               if( _state == FAILED )
                    throw new IOException(_msg) ;

               if( _state == DONE )
                    throw new IOException( "Inactive" ) ;

           }

       }
       public long getPosition(){ return _position ; }
       public long getLength(){ return _length ; }
       public long getBytesRead(){ return _bytesRead ; }
       //
       //   The private part
       //
       private String iocmdToString(int iocmd ){
          if( ( iocmd <= 0 ) || ( iocmd > 9 ) )return "Unkown" ;
          return _commands[iocmd] ;
       }
       private VspRequest( int id , String pnfsId , String rw ){
          _sessionId = id ;
          _pnfsId    = pnfsId ;
          _mode      = rw ;
       }
       public void run(){
          try{
             int chSize = _dataIn.readInt() ;
             say( "Challenge Size is "+chSize ) ;
             _dataIn.skipBytes(chSize) ;
             while( ! Thread.interrupted() ){
                int len = 0 ;
                try{
                    len = _dataIn.readInt() ;
                }catch(InterruptedIOException iioe ){
                    say( "connection interrupted" ) ;
                    break ;
                }catch(EOFException eofe){
                    say( "connection closed" ) ;
                    break  ;
                }
                if( len < 4 )
                   throw new Exception("Protocol violation (len)" ) ;
                int command = _dataIn.readInt() ;
//                say( "Arrived : command = "+command+" (len="+len+")" ) ;
                int rc , iocmd ;
                switch( command ){
                   case IOCMD_DATA :   // DATA
                      while( true ) {
                         int datalen = _dataIn.readInt() ;
                         say( "data : "+datalen) ;
                         if( datalen < 0 )break ;

                         if( datalen == 0 )continue ;
                         DATAarrived( datalen , _dataIn ) ;
                      }
                   break ;
                   case IOCMD_ACK :   // ACK
                   case IOCMD_FIN :   // FIN
                      if( len < 12 )
                         throw new Exception("Protocol violation (len2)" ) ;

                      iocmd = _dataIn.readInt() ;
                      rc    = _dataIn.readInt() ;

                      len -= 12 ;
                      if( rc == 0 ){
                         long [] args = new long[len / 8] ;
                         for( int i = 0 ; len >= 8 ; i++ , len -= 8 ){
                            args[i] = _dataIn.readLong() ;
                         }
                         ACK_FINarrived( command , iocmd , args ) ;
                      }else{
                         //
                         // the following code doesn't support
                         // character encoding above 0x7f.
                         // ( it will run out of sync.
                         //
                         String str = _dataIn.readUTF() ;
                         len -= ( str.length() + 2 ) ;
                         setFailed( str ) ;
                      }
                      _dataIn.skipBytes(len) ;
                   break ;
                   default :
                      say( "Unknown command : "+command ) ;
                      say( "Arrived : command = "+command+" (len="+len+")" ) ;

                }
              }

          }catch(Exception ee ){

             say("Run exception : "+ee ) ;
             if( _debug )ee.printStackTrace() ;
             setFailed(ee.toString()) ;
          }
          try{ _dataIn.close() ;}catch(IOException ee ){}
          try{ _dataOut.close() ;} catch(IOException ee ){}
          say( "Worker finished" ) ;
       }
       private void doTheWriteData(){
          try{
             int out = 0 ;
             _dataOut.writeDATA_HEADER() ;
             while( true ){
                out = Math.min( 4*1024 , _size ) ;
                say( "ACK writeCmdData off="+_offset+" out "+out);
                _dataOut.writeDATA_BLOCK( _data , _offset , out ) ;
                _offset += out ;
                _size   -= out ;
                if( _size <= 0 )break ;
             }
             _dataOut.writeDATA_TRAILER() ;
          }catch( IOException ioe ){
             setFailed(ioe.toString());
          }
       }
       private void ACK_FINarrived( int command , int iocmd , long [] args ){
          if( command == IOCMD_ACK ){
            StringBuffer sb = new StringBuffer() ;
            sb.append( "ack for "+_commands[iocmd]+" args={") ;
            for( int i = 0 ; i < args.length ; i++ )
               sb.append( args[i]+"," ) ;
            sb.append("}") ;
            say(sb.toString());
            if( iocmd == IOCMD_WRITE ){
              if( _writeInThread ){  // only necessary if interrupt is enabled
                 new Thread(
                   new Runnable(){
                     public void run(){ doTheWriteData() ; }
                   }
                 ).start() ;
              }else{
                 doTheWriteData() ;
              }
            }else if( iocmd == IOCMD_SEEK ){
               setOk() ;
            }else if( iocmd == IOCMD_LOCATE ){
               _position = args[1] ;
               _length   = args[0] ;
               setOk() ;
            }
          }else if( command == IOCMD_FIN ){
            StringBuffer sb = new StringBuffer() ;
            sb.append( "fin for "+_commands[iocmd]+" args={") ;
            for( int i = 0 ; i < args.length ; i++ )
               sb.append( args[i]+"," ) ;
            sb.append("}") ;
            say(sb.toString());
            if( iocmd == IOCMD_WRITE ){
               setOk() ;
            }else if( iocmd == IOCMD_READ ){
               setOk() ;
            }
          }
       }
       private void DATAarrived( int len , DataInputStream in ){
          try{
            if( _ioMode == IO_STREAM ){
                say( "stream data total "+len ) ;
                int rest = len ;
                while( rest > 0 ){
                   int diff = rest > _data.length ? _data.length : rest ;
                   say( "stream data p. "+diff ) ;
                   in.readFully( _data , 0 , diff ) ;
                   rest       -= diff ;
                   _bytesRead += diff ;
                   _transferrable.dataArrived( this , _data , 0 , diff ) ;
                }
            }else{
                say( "rf "+_data.length+" "+_offset+" "+len);
                in.readFully(_data,_offset,len) ;
                int rc = len;
                _offset += rc ;
                _size   -= rc ;
                _bytesRead += rc ;
            }
          }catch( Exception ioe ){
             say( "erf "+_data.length+" "+_offset+" "+len);
             ioe.printStackTrace() ;
             setFailed(ioe.toString() ) ;
          }
       }
       private void connectionArrived( DataInputStream in , DataOutputStream out ){
           say("Connection arrived for sessionid : "+_sessionId) ;
           synchronized( _syncLock ){
              _state   = CONNECTED ;
              _pending = false ;
              _dataIn  = in ;
              _dataOut = new VspDataOutputStream(out) ;
              _worker  = new Thread(this) ;
              _worker.start() ;
              _syncLock.notifyAll() ;
           }
       }
       private void infoArrived( VspArgs args ){
           say("Info for "+_sessionId+" : "+args ) ;
           synchronized( _syncLock ){
              if( args.getCommand().equals("failed") ){
                 setFailed( args.toString() ) ;
              }else{
                 setOk();
              }
           }
       }
       private void setFailed( String msg ){
          synchronized( _syncLock ){
             _msg     = msg ;
             _pending = false ;
             _state   = FAILED ;
             _syncLock.notifyAll();
             if( _callBack != null )_callBack.ioFinished(this) ;
             say( "release (failed)" ) ;
          }
       }
       private void setOk(){
          synchronized( _syncLock ){
             _pending = false ;
             _state   = CONNECTED ;
             _syncLock.notifyAll();
             if( _callBack != null )_callBack.ioFinished(this) ;
             say( "release (ok)" ) ;
          }
       }
       private void check() throws IOException {
           synchronized( _syncLock ){
              if( _finished )
                 throw new IOException( "Connection not active" ) ;
              if( _pending )
                 throw new IOException( "IO pending" ) ;

              _pending = true ;
           }
       }
       public void close()throws IOException {
           check() ;
           _ioType = IOCMD_CLOSE ;
           _dataOut.writeCmdClose() ;

       }
   }
   public VspDevice( String host , int port , String replyHost )
          throws UnknownHostException, IOException {

      try{
          _door = new Socket( host , port ) ;
          _in   = new BufferedReader(
                     new InputStreamReader(
                            _door.getInputStream() ) );
          _out  = new PrintWriter(
                     new OutputStreamWriter(
                            _door.getOutputStream() ) ) ;

          _listen = new ServerSocket(0) ;
//          _host   = _listen.getInetAddress().getHostAddress() ;
//          _host   = InetAddress.getLocalHost().getHostName() ;
          _host   = replyHost ;
          _port   = _listen.getLocalPort() ;
          say("Local : open "+_host+":"+_port) ;
      }catch( IOException e ){
           _closeAll() ;
           throw e ;
      }
      _serviceThread = new Thread( this ) ;
      _serviceThread.start() ;
      _commandThread = new Thread( this ) ;
      _commandThread.start() ;
      try{
         synchronized( _requestHash ){
            while( ( ! _online ) && ( ! _controlConnectionClosed ) ){
               say("Create state "+_online+" "+_controlConnectionClosed );
               _requestHash.wait() ;
               say("Create state : Woke up" ) ;
            }
         }
      }catch(InterruptedException ee ){

      }
   }
   public void setHostname( String hostname ){ _host = hostname ; }
   public void run(){
      if( Thread.currentThread() == _commandThread ){
         commandRun() ;
      }else if( Thread.currentThread() == _serviceThread ){
         serviceRun() ;
      }
   }
   private void serviceRun(){
      Socket s = null ;
      int sessionId = 0 ;
      DataInputStream data = null ;
      VspRequest request = null ;
      while( ! Thread.currentThread().interrupted() ){
         try{
             s = _listen.accept() ;
         }catch( IOException ioe ){
             say( "Accept interrupted : "+ioe ) ;
             break ;
         }
         say("Data connection from : "+s ) ;
         try{
            data = new DataInputStream( s.getInputStream() ) ;
            sessionId = data.readInt() ;
            request =
                (VspRequest)
                    _requestHash.get(
                       Integer.valueOf(sessionId));
            if( request == null ){
               say(
                  "Unexpected session id from data stream : "+sessionId) ;
               continue ;
            }else{
               say("Session "+sessionId+" connected" ) ;
            }
            request.connectionArrived( data ,
                                 new DataOutputStream( s.getOutputStream() ) ) ;
         }catch(Exception ie ){
             say("Exception for data : "+sessionId+
                 " : "+ie ) ;
             try{ s.close() ; }catch(Exception xx){}
         }

      }
      say("connection thread terminated" ) ;
   }
   private void commandRun()  {
      String  line  = null ;
      VspArgs args  = null ;
      int     sessionId = 0 ;
      VspRequest request = null ;

      _out.println( "0 0 client hello 0 0 0 0" ) ;
      _out.flush() ;

      while( ! Thread.currentThread().interrupted() ){

         try{
            if( ( line = _in.readLine() ) == null )break ;
         }catch(IOException ioe ){
            say( "commandRun : "+ioe ) ;
            break ;
         }

         args      = new VspArgs( line ) ;
         sessionId = args.getSessionId() ;

         say( "Door : "+args ) ;

         if( sessionId == 0 ){
            //
            //  master session
            //
            masterSession( args ) ;

         }else{
            synchronized( _requestHash ){
                request =
                    (VspRequest)
                       _requestHash.get(
                          Integer.valueOf(sessionId));
            }
            if( request == null ){
               say(
                  "Unexpected session id from command stream : "+sessionId) ;
               continue ;
            }
            request.infoArrived( args ) ;
         }

      }
      synchronized( _requestHash ){
        _controlConnectionClosed = true ;
        _requestHash.notifyAll() ;
      }
      say( "Command thread terminated" ) ;
   }
   private synchronized void masterSession( VspArgs args ) {
      if( args.getCommand().equals("welcome") ){
         synchronized( _requestHash ){
            _online =true ;
            say("Opening master session");
            _requestHash.notifyAll() ;
         }
      }
   }
   public VspConnection open( String pnfsId , String mode ){
       int id = nextSessionId() ;
       VspRequest request = new VspRequest( id , pnfsId , mode ) ;
       synchronized( _requestHash ){
          _requestHash.put( Integer.valueOf(id) , request ) ;
          _out.println( ""+id+" 0 client open "+pnfsId+" "+mode+" "+_host+" "+_port ) ;
          _out.flush();
       }

       return request ;
   }
   public void close(){
      synchronized( _requestHash ){
         _closeAll();
         _finished = true ;
         _requestHash.notifyAll() ;
      }
   }
   private void _closeAll(){
     //
     //   NOTE : first close followed by interrupt causes deadlock
     //
      say( "sending interrupt to command thread" ) ;
      _commandThread.interrupt() ;
      say( "sending interrupt to service thread" ) ;
      _serviceThread.interrupt() ;
      say( "CloseAll done" ) ;
      try{ if(_in!=null)_in.close() ;}catch(IOException ee){}
      if(_out!=null)_out.close();
      try{ if(_door!=null)_door.close() ;}catch(IOException ee ){}
      try{ if(_listen!=null)_listen.close() ;}catch(IOException ee ){}
   }
   public static void main( String [] args ) throws Exception {
       if( args.length < 4 ){
          System.err.println( "Usage : ... <host> <port> <replyHost> <pnfsId>" ) ;
          System.exit(4);
       }
       String hostname   = args[0] ;
       int    port       = Integer.parseInt( args[1] ) ;
       String replyHost  = args[2] ;
       String pnfsId     = args[3] ;
//       String mode     = args[3] ;
       VspDevice vsp = new VspDevice( hostname , port , replyHost ) ;
       System.err.println( "VspDevice created" ) ;
       vsp.setDebugOutput(true);

       VspConnection c1 = vsp.open( pnfsId , "r" ) ;
       c1.sync() ;
       c1.setSynchronous(false);
       c1.setIoFinishable(
           new VspIoFinishable(){
              public void ioFinished( VspConnection connection ){
                 System.out.println( "callback" ) ;
              }
           }
       ) ;
       System.out.println("Open synced");
       long start = System.currentTimeMillis() ;

       long rc = c1.read( 1024*1024*1024 ,
            new VspDataTransferrable(){
                public void dataArrived( VspConnection vsp ,
                             byte [] buffer , int offset , int size ){

//                    System.out.println("Data arrived : "+size ) ;
                }
                public void dataRequested( VspConnection v ,  byte [] b , int o , int s ){}
            }
       ) ;
       System.out.println( "read : "+rc ) ;
       System.out.println( "Syncing read" ) ;
       c1.sync() ;
       long diff = System.currentTimeMillis() - start ;
       if( diff == 0 ){
          System.out.println( "Done : ??? MB/sec" ) ;
       }else{
          double rate =  ((double)c1.getBytesRead()) / ((double)diff) / 1024. / 1024. * 1000.;
          System.out.println( "Done : "+rate+" MB/sec" ) ;
       }


       System.out.println( "close" ) ;
       c1.close() ;
       System.err.println( "Waiting for sync close" ) ;
       c1.sync() ;
       System.err.println( "Sync done" ) ;
       vsp.close() ;
       System.err.println( "Vsp closed" ) ;
   }
}