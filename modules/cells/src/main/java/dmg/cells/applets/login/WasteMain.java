package dmg.cells.applets.login ;

import java.applet.*;
import java.awt.* ;
import java.awt.event.* ;
import java.lang.reflect.* ;
import java.util.* ;
import dmg.util.* ;
import dmg.protocols.ssh.* ;

public class      WasteMain 
       extends    Frame 
       implements WindowListener, 
                  ActionListener {
       
  //
  // remember to change the following variable to your needs 
  // 
  private SshLoginPanel     _loginPanel     = null ;
   
  private String    _remoteHost     = null ;
  private String    _remotePort     = null ;
  private String    _remoteUser     = null ;
  private String    _remotePassword = null ;
  private String    _userPanel      = null ;
  private String    _title          = null ;
  
  
  private Font      _font = new Font( "TimesRoman" , 0 , 16 ) ;
  
  
  public WasteMain( Args args ){
      super( "CellAlias" ) ;
      setLayout( new BorderLayout() ) ;
//      setLayout( new CenterLayout() ) ;
      addWindowListener( this ) ;
      setLocation( 60 , 60) ;
//
//    get all necessay infos ...
//
      _remoteHost = args.getOpt( "host" ) ;
      _remotePort = args.getOpt( "port" ) ;
      _remoteUser = args.getOpt( "user" ) ;
      _title      = args.getOpt( "title" ) ;
     
      //
      // we allow to set the "panel" :
      //    only this panel is displayed , or
      // we allow to have panel1, panel2 ... 
      //    in this  case the syntax has to be
      //       param name="panel1" value="<name>:<panelClass>"
      //
      _userPanel  = args.getOpt( "panel" ) ;
      
      _remoteHost = _remoteHost==null?"":_remoteHost ;
      _remotePort = _remotePort==null?"":_remotePort ;
      _remoteUser = _remoteUser==null?"":_remoteUser ;
      _title      = _title==null?"Login":_title;
      
      _loginPanel = new SshLoginPanel() ;
      _loginPanel.setHost( _remoteHost , true , true ) ;
      _loginPanel.setPort( _remotePort , true , true ) ;
      _loginPanel.setUser( _remoteUser , true , true ) ;
      _loginPanel.setTitle( _title ) ;
      
      _loginPanel.addActionListener( this ) ;
   
       _loginPanel.ok() ;

      add( _loginPanel , "Center" ) ;   
      setSize( 750 , 500 ) ;
      pack() ;
      setSize( 750 , 500 ) ;
      setVisible( true ) ;
  }
 public synchronized void actionPerformed( ActionEvent event ){
     String command = event.getActionCommand() ;
     System.out.println( "Action Applet : "+command ) ;
     Object obj = event.getSource() ;
     if( command.equals( "connected" ) ){
//         _cardsLayout.show( _switchPanel , "commander" ) ;
     }else if( command.equals( "disconnected" ) ){
//         _cardsLayout.show( _switchPanel , "login" ) ;
     }else if( command.equals( "exit" ) ){
         _loginPanel.logout() ;
     }
  }
  
  //
  // window interface
  //
  public void windowOpened( WindowEvent event ){}
  public void windowClosed( WindowEvent event ){
      System.exit(0);
  }
  public void windowClosing( WindowEvent event ){
      System.exit(0);
  }
  public void windowActivated( WindowEvent event ){}
  public void windowDeactivated( WindowEvent event ){}
  public void windowIconified( WindowEvent event ){}
  public void windowDeiconified( WindowEvent event ){}
  public static void main( String [] args ){
      if( args.length < 1 ){
         System.err.println( "Usage : ... {-host=<hostname> -port=<port>]<panels>" ) ;
         System.exit(4) ;
      }
     try{
            
         new WasteMain( new Args( args ) ) ;
      
      }catch( Exception e ){
         e.printStackTrace() ;
         System.err.println( "Connection failed : "+e.toString() ) ;
         System.exit(4);
      }
      
   }
       
}