package dmg.cells.applets.spy ;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Properties;

public class      Spy
       extends    Frame
       implements WindowListener,
                  ActionListener ,
                  DomainConnectionListener {

   private static final long serialVersionUID = 2320418362286824232L;
   private DomainConnection _connection;
   private Button _connectButton;
   private Button _closeButton;
   private Label  _messageText;
   private Panel  _domainListPanel;

   private class BorderPanel extends Panel {
       private static final long serialVersionUID = 3231733834533174912L;

       public BorderPanel( Component c ){
         super( new BorderLayout() ) ;
         add( c , "Center" ) ;
      }

      @Override
      public Insets getInsets(){ return new Insets( 10 , 10 ,10 , 10 ) ; }

   }

  public Spy( String host , int port  ){
      super( "DomainSpy" ) ;

      _connection = new DomainConnection( host , port ) ;


      addWindowListener( this ) ;
      _connection.addConnectionListener( this ) ;

      _domainListPanel  = new DomainListPanel( _connection ) ;
      Panel masterPanel = new Panel( new BorderLayout() ) ;
      Panel buttonPanel = new Panel( new FlowLayout( FlowLayout.CENTER ) ) ;
      Panel actionPanel = new BorderPanel( _domainListPanel ) ;

      _connectButton = new Button( "Connect" ) ;
      _closeButton   = new Button( "Close Connection" ) ;
      _messageText   = new Label("Not Connected") ;

      _connectButton.addActionListener( this ) ;
      _closeButton.addActionListener( this ) ;

      buttonPanel.add( _connectButton ) ;
      buttonPanel.add( _closeButton ) ;

      masterPanel.add( buttonPanel , "North" ) ;
      masterPanel.add( actionPanel , "Center" ) ;
      masterPanel.add( _messageText , "South" ) ;

      add( masterPanel ) ;

      connectionActive( false ) ;
      setSize( 750 , 500 ) ;
      pack() ;
      setSize( 750 , 500 ) ;
      setVisible( true ) ;

  }
  //
  // action interface
  //
  @Override
  public void actionPerformed( ActionEvent event ) {
    Object source = event.getSource() ;
    if( source == _connectButton ){
       try{
          _connection.connect() ;
       }catch( Exception e ){
          _messageText.setText( "Not Connected : "+e.getMessage() ) ;
       }

    }else if( source == _closeButton ){
       _connection.close() ;
    }

  }
  //
  // domain connection interfase
  //
  @Override
  public void connectionActivated( DomainConnectionEvent event ){
      connectionActive( true ) ;
      _messageText.setText( "Connected" ) ;
  }
  @Override
  public void connectionDeactivated( DomainConnectionEvent event ){
      System.out.println( "Listener : "+event.getMessage()) ;
      connectionActive( false ) ;
      _messageText.setText( "Connection Closed : "+event.getMessage() ) ;
  }
  @Override
  public void connectionFailed( DomainConnectionEvent event ){
      _messageText.setText( "Connection Failed : "+event.getMessage() ) ;
      connectionActive( false ) ;
  }
  private void connectionActive( boolean enabled ){
      _connectButton.setEnabled( ! enabled ) ;
      _closeButton.setEnabled( enabled ) ;
      _messageText.setBackground( enabled ? Color.green : Color.red ) ;
      _domainListPanel.setEnabled(enabled);
  }
  //
  // window interface
  //
  @Override
  public void windowOpened( WindowEvent event ){}
  @Override
  public void windowClosed( WindowEvent event ){
      System.exit(0);
  }
  @Override
  public void windowClosing( WindowEvent event ){
      System.exit(0);
  }
  @Override
  public void windowActivated( WindowEvent event ){}
  @Override
  public void windowDeactivated( WindowEvent event ){}
  @Override
  public void windowIconified( WindowEvent event ){}
  @Override
  public void windowDeiconified( WindowEvent event ){}
   public static void main( String [] args ){
      if( args.length < 2 ){
         System.err.println( "Usage : ... <hostName> <spyListenPort>" ) ;
         System.exit(4) ;
      }
      if( args.length > 2 ){
         Properties props = System.getProperties() ;
         props.put( "bw" , args[2] ) ;
      }
      int port = Integer.parseInt( args[1] ) ;
      try{

         new Spy( args[0] , port ) ;

      }catch( Exception e ){
         e.printStackTrace() ;
         System.err.println( "Connection failed : "+e.toString() ) ;
         System.exit(4);
      }

   }

}
