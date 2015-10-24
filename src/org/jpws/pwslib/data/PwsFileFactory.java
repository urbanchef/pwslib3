/*
 *  file: PwsFileFactory.java
 * 
 *  Project JPasswords
 *  @author Wolfgang Keller
 *  Created 07.08.2005
 *  Version
 * 
 *  Copyright (c) 2005 by Wolfgang Keller, Munich, Germany
 * 
 This program is not freeware software but copyright protected to the author(s)
 stated above. However, you can use, redistribute and/or modify it under the terms 
 of the GNU General Public License as published by the Free Software Foundation, 
 version 2 of the License.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

 You should have received a copy of the GNU General Public License along with
 this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 Place - Suite 330, Boston, MA 02111-1307, USA, or go to
 http://www.gnu.org/copyleft/gpl.html.
 */

package org.jpws.pwslib.data;

import java.awt.event.InputEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.KeyStroke;

import org.jpws.pwslib.exception.ApplicationFailureException;
import org.jpws.pwslib.exception.DuplicateEntryException;
import org.jpws.pwslib.exception.InvalidPassphraseException;
import org.jpws.pwslib.exception.InvalidPassphrasePolicy;
import org.jpws.pwslib.exception.PasswordSafeException;
import org.jpws.pwslib.exception.UnsupportedFileVersionException;
import org.jpws.pwslib.exception.WrongFileVersionException;
import org.jpws.pwslib.global.Global;
import org.jpws.pwslib.global.Log;
import org.jpws.pwslib.global.UUID;
import org.jpws.pwslib.global.Util;
import org.jpws.pwslib.persist.ApplicationAdapter;
import org.jpws.pwslib.persist.StreamFactory;

/**
 * This is a singleton factory class with static methods used to load and save 
 * <i>PasswordSafe</i> files of all known formats.  
 * It is capable of detecting file format versions and handling differences 
 * appropriately. All three major file formats (referred to as V1, V2 and V3) 
 * can be read and written. The user is confronted with unified methods and
 * addresses version differences through file version numbers. As the older
 * versions 1 and 2 are practically out of use, the typical usage is by stating
 * the default version number 0 or the default methods without version 
 * reference.
 * 
 * <p>The file format undergoes minor enhancements through time, which consist
 * mostly in added data fields and are reflected by the "minor" file format 
 * number. Backward compatibility is always sought. The
 * PWS database format additionally seeks forward compatibility by asking 
 * applications to conserve unknown data in the files. This PWS library fully 
 * conforms with this requirement. 
 * 
 * <p>The field type constants provided with this class refer to RECORD FIELDS
 * and are given public for the convenience of applications which attempt to 
 * interpret the block data stream of a PWS file in their own fashions. 
 * Alternatively to this factory, a PWS file can  be interpreted as a stream of
 * data blocks or a stream of data fields by use of {@link PwsFileInputSocket} 
 * and the {@link PwsRawFieldReader} interface for reading and {@link 
 * PwsFileOutputSocket} and the {@link PwsRawFieldWriter} interface for writing.
 * The documents describing the various file formats are supplied in the "doc"
 * section of the developer package of this library.
 * 
 * <p><u>Header Fields as Save Parameter</u>
 * <p>The save methods feature a parameter <code><b>headerFields</b></code>
 * to allow application specific supply of file header data in the context of 
 * V3 databases. User header fields are only made persistent in format V3; the 
 * official canon of header fields is listed as constants in class 
 * <code>PwsFileHeaderV3</code>. For a target file of format version V2 only 
 * field type <code>PwsFileHeaderV3.JPWS_OPTIONS_TYPE</code> is extracted from 
 * the header field list. JPasswords stores file specific properties there.
 * A target file V1 does not interpret the header fields.    
 * 
 * <p>The file and record format of this factory complies with PWS format
 * definition 3.13.
 * 
 * @see HeaderFieldList
 * @see org.jpws.pwslib.persist.ApplicationAdapter
 * @see PwsPassphrase
 * 
 * @author Wolfgang Keller
 */
public final class PwsFileFactory
{
   /** Datafield type for the UUID of a record */
   public static final int RECIDTYPE = 0x01;
   
   /** Datafield type "Group Name" (TEXT) */
   public static final int GROUPTYPE = 0x02;

   /** Datafield type "Record Title" (TEXT) */
   public static final int TITLETYPE = 0x03;

   /** Datafield type "User Name" (TEXT) */
   public static final int UNAMETYPE = 0x04;

   /** Datafield type "Notes" (TEXT) */
   public static final int NOTESTYPE = 0x05;

   /** Datafield type "Password" (TEXT) */
   public static final int PASSWORDTYPE = 0x06;

   /** Datafield type "Creation Time"  (TIME) */
   public static final int CREATIMETYPE = 0x07;
   
   /** Datafield type "Password Modification Time" (TIME) */
   public static final int PASSMODTIMETYPE = 0x08;
   
   /** Datafield type "Password Access Time"  (TIME) */
   public static final int ACCESSTIMETYPE = 0x09;
   
   /** Datafield type "Password Life Time"  (TIME) */
   public static final int PASSLIFETIMETYPE = 0x0a;
   
   /** Datafield type "Password Policy" */
   public static final int PASSPOLICYTYPE = 0x0b;
   
   /** Datafield type "Record Modification Time"  (TIME) */
   public static final int RECORDMODTIMETYPE = 0x0c;

   /** Datafield type "URL" (TEXT, since V3) */
   public static final int URLTYPE = 0x0d;

   /** Datafield type "Autotype" (TEXT, since V3) */
   public static final int AUTOTYPETYPE = 0x0e;

   /** Datafield type "Password History" (TEXT, since V3) */
   public static final int HISTORYTYPE = 0x0f;

   /** Datafield type "Modern Password Policy" */
   public static final int MODERN_POLICYTYPE = 0x10;
   
   /** Datafield type "Expiry Interval" */
   public static final int EXPIRY_INTERVALTYPE = 0x11;
   
   /** Datafield type "Run Command" (unused in JPWS) */
   public static final int RUNCOMMANDTYPE = 0x12;
   
   /** Datafield type "Double-Click Action" (unused in JPWS) */
   public static final int DCLICK_ACTIONTYPE = 0x13;
   
   /** Datafield type "Shift-Double-Click Action" (unused in JPWS) */
   public static final int SHIFT_DCLICK_ACTIONTYPE = 0x17;
   
   /** Datafield type "Email Info" (TEXT, since V3.6) */
   public static final int EMAILTYPE = 0x14;

   /** Datafield type "Protected Entry" (Binary, since V3.9) */
   public static final int PROTECTED_ENTRYTYPE = 0x15;

   /** Datafield type "Own Passpolicy Symbols" (TEXT, since V3.9) */
   public static final int OWNSYMBOLSTYPE = 0x16;

   /** Datafield type "Password Policy Name" (TEXT, since V3.10) */
   public static final int POLICY_NAMETYPE = 0x18;

   /** Datafield type "Password Policy Name" (TEXT, since V3.10) */
   public static final int KEYBOARD_SHORTCUT_TYPE = 0x19;

   /** Datafield type "End of Record" (VOID) */
   public static final int ENDBLOCKTYPE = 0xff;

   private static final int LAST_CANONICAL_BLOCK_FIELD = 0x19;

   /** Database format version of this library (writing). */
   public static final int DEFAULT_FILEVERSION = Global.FILEVERSION_LATEST_MAJOR;

   /** Internal constant for database version identification. */
   public static final String V2_VERSION_IDENTTEXT = " !!!Version 2 File Format!!! Please upgrade to PasswordSafe 2.0 or later";

   /** Internal constant for TEXT value conversions. */
   public static final String UTF_CHARSET   = "UTF-8";

   /** Internal constant for TEXT value conversions. */
   public static final String DEFAULT_CHARSET   = Global.getDefaultCharset();

   /** The milliseconds of 24 hours. */
   public static final int DAY = 86400 * 1000;
   
   /** Field splitting sign used for format version 1 */
   private static final String SPLITCHAR   = "  \u00ad  ";
   
   private static final byte[] ZEROBYTEARRAY = new byte[0];


	/**
	 * Private to prevent instantiation.
	 */
	private PwsFileFactory()
	{}

   /**
    * Loads a PWS file of any format from the standard application adapter
    * (by default the local file system).  
    * 
    * @param filepath    the pathname of the file to open
    * @param passphrase the file access passphrase
    * 
    * @return the opened, fully operable <code>PwsFile</code> object 
    * 
    * @throws NullPointerException if any param is undefined 
    * @throws IllegalArgumentException if filepath is empty 
    * @throws FileNotFoundException if the specified file was not found or
    *         access was denied
    * @throws InvalidPassphraseException if file access could not be verified
    * @throws ApplicationFailureException if the specified IO-context does not
    *         render an input stream
    * @throws IOException if an IO-error occurred
    */
   public static final PwsFile loadFile( 
                              String filepath, 
                              PwsPassphrase passphrase )
   throws IOException, PasswordSafeException
   {
      return loadFile( Global.getStandardApplication(), filepath, passphrase, 0 );
   }
   
   /**
     * Loads a PWS file of a specific format from a file specified by a 
     * <code>ContextFile</code> parameter. 
     * (If a format other than 0 is specified, loading will fail if the referred
     * persistent state is of a different format.)
     * 
     * @param file <code>ContextFile</code> the file in an application context   
     * @param passphrase the user passphrase for the file
     * @param format the file format version (values of <code>Global</code>),
     *        0 for a generic open request (any version)
     * 
     * @return the opened, fully operable <code>PwsFile</code> object 
     *
     * @throws NullPointerException if any param is undefined 
     * @throws IllegalArgumentException if any filepath is empty 
     * @throws FileNotFoundException if the specified file was not found or
     *         access was denied
     * @throws InvalidPassphraseException
     * @throws WrongFileVersionException if a specific format was requested
     *         and not found to match the file
     * @throws UnsupportedFileVersionException if the requested format is unknown
     * @throws ApplicationFailureException if the specified IO-context does not
     *         render an input stream
     * @throws IOException if an IO-error occurred
     */
   public static final PwsFile loadFile(
          ContextFile file,
          PwsPassphrase passphrase,
          int format )
   throws IOException, PasswordSafeException
   {
      // read file
      PwsFile dbf = loadFile( file.getInputStream(), passphrase, format );
      
      // update persistent state definition in file
      dbf.setApplication( file.getAdapter() );
      dbf.setFilePath( file.getFilepath() );
        
      dbf.resetModified();
      return dbf;
   }

 /**
     * Loads a PWS file of any format from a file specified by a 
     * <code>ContextFile</code> parameter. 
     * 
     * @param file <code>ContextFile</code> the file in an application context   
     * @param passphrase the user passphrase for the file
     * 
     * @return the opened, fully operable <code>PwsFile</code> object 
     *
     * @throws NullPointerException if any param is undefined 
     * @throws FileNotFoundException if the specified file was not found or
     *         access was denied
     * @throws InvalidPassphraseException if file could not be opened with passphrase
     * @throws ApplicationFailureException if the specified IO-context does not
     *         render an input stream
     * @throws IOException if an IO-error occurred
     * @since 2-1-0
     */
   public static final PwsFile loadFile(
          ContextFile file,
          PwsPassphrase passphrase )
   throws IOException, PasswordSafeException
   {
      return loadFile( file, passphrase, 0 );  
   }

/**
 * Loads a PWS file of a specific format from a user application context.
 * (If a format other than 0 is specified, loading will fail if the referred
 * pesistent state is of a different format.)
 * 
 * @param application the IO-context of the file   
 * @param filepath   the pathname of the file to open
 * @param passphrase the user passphrase for the file
 * @param format the file format version (values of <code>Global</code>),
 *        0 for a generic open request (any version)
 * 
 * @return the opened, fully operable <code>PwsFile</code> object 
 *
 * @throws NullPointerException if any param is undefined 
 * @throws IllegalArgumentException if filepath is empty 
 * @throws FileNotFoundException if the specified file was not found or
 *         access was denied
 * @throws InvalidPassphraseException
 * @throws WrongFileVersionException if a specific format was requested
 *         and not found to match the file
 * @throws UnsupportedFileVersionException if the requested format is unknown
 * @throws ApplicationFailureException if the specified IO-context does not
 *         render an input stream
 * @throws IOException if an IO-error occurred
 */
 protected static final PwsFile loadFile( 
                           ApplicationAdapter application,
                           String filepath, 
                           PwsPassphrase passphrase,
                           int format )
throws IOException, PasswordSafeException
 {
   // create input services and read file
   InputStream in = StreamFactory.getInputStream( application, filepath );
   PwsFile file = loadFile( in, passphrase, format );

   // update persistent state definition in file
   file.setApplication( application );
   file.setFilePath( filepath );

   file.resetModified();
   return file;
 }  // loadFile

/**
    * Loads a PWS file of a specific format from a user input stream.
    * Note that persistent state definition is void in returned object.
    * (If a format other than 0 is specified, loading will fail if the 
    * pesistent state is of a different format.)
	* 
	* @param in InputStream containing persistent state of the file    
	* @param passphrase the user passphrase for the file
    * @param format the file format version (values of <code>Global</code>),
    *        0 for a generic open request (any version)
	* 
    * @return the opened, fully operable <code>PwsFile</code> object 
	*
    * @throws NullPointerException if any param is undefined 
    * @throws InvalidPassphraseException
    * @throws WrongFileVersionException if a specific format was requested
    *         and not found to match the file
    * @throws UnsupportedFileVersionException if the requested format is unknown
    * @throws ApplicationFailureException if the specified IO-context does not
    *         render an input stream
    * @throws IOException if an IO-error occurred
    */
   public static final PwsFile loadFile( 
                               InputStream in,
                               PwsPassphrase passphrase,
                               int format )
              throws IOException, PasswordSafeException
	{
	  PwsFile       	 file;
      PwsFileInputSocket socket;
      PwsRawFieldReader  reader;
      UUID               uuid;
      String        	 charset, options;
      byte[]        	 hmac, cks1, cks2;
      int           	 version;

      // create input services
      socket = new PwsFileInputSocket( in );

      try {
         // verify the correct user passphrase 
         if ( !socket.attemptOpen( passphrase, format ) )
            throw new InvalidPassphraseException();

         options = socket.getOptions();
         version = socket.getFileVersion();
         uuid = socket.getFileUUID();
         
         // create file object
         file = new PwsFile();
         if ( uuid != null ) {
            file.setUUID( uuid );
         }
         file.setPassphrase( passphrase );
         file.setSourceFormat( version );
         file.setSecurityLoops( socket.getIterations() );
         file.setUserOptions( options );
         file.setHeaderFields( socket.getHeaderFields() );
         Log.debug( 10, "LOADFILE, resulting PWS file format: " + file.getSourceFormat() );
         
         // determine active charset
         charset = (version == Global.FILEVERSION_3 | options.indexOf( "B 24 1" ) > -1)  ? 
                   UTF_CHARSET : DEFAULT_CHARSET;
         Log.debug( 10, "LOADFILE, PWS file charset: " + charset );
      
         // create reader and read fields 
         reader = socket.getRawFieldReader();
   
         switch ( version )
         {
         case Global.FILEVERSION_1:
            readRecordsV1( file, reader );
            break;
            
         case Global.FILEVERSION_2:
         case Global.FILEVERSION_3:
            readRecordsV23( file, reader, charset, version );
            cks1 = socket.getCalcChecksum();
            cks2 = socket.getReadChecksum();
            if ( cks1 != null & cks2 != null ) {
               file.setChecksumResult( Util.equalArrays( cks1, cks2 ));
            }
            break;
            
         default:
            throw new UnsupportedFileVersionException( String.valueOf( version ) );
         }

      } finally {
         in.close();
      }

      // Log reports
      if ( Log.getDebugLevel() > 1 ) {
	     Log.log( 2, "(PwsFileFactory.loadFile) load of PwsFile complete, UUID = " + file.getUUID().toHexString() );
	     Log.debug( 2, "(PwsFileFactory.loadFile) file contains " + file.size() + " records" );
	     if ( (hmac = socket.getReadChecksum()) != null ) 
	        Log.debug( 2, "(PwsFileFactory.loadFile) File HMAC read: " + Util.bytesToHex( hmac ) );
	     if ( (hmac = socket.getCalcChecksum()) != null )
	        Log.debug( 2, "(PwsFileFactory.loadFile) File HMAC calc: " + Util.bytesToHex( hmac ) );
      }
      
      file.resetModified();
      return file;
	}  // loadFile
   
   

   /** Attempts to read the remainder of the file as records in the V2 or V3 blend.
    *  This algorithm tolerates the occurrence of unknown field types as well as
    *  missing fields. The only requirement for a read record is the presence
    *  of a record ENDBLOCK and the uniqueness of the Record-ID, if present. 
    *  Missing or not qualifying UUIDs (Record-IDs) are replaced by newly generated.
    *  Double records are silently ignored, but an error log is written to the 
    *  LOG.err device and the file is marked for conservation as ".old" store 
    *  version.
    *     
    *  @throws IOException
    */
   private static void readRecordsV23 ( PwsFile file, PwsRawFieldReader reader, 
                                        String charset, int version )
   {
      PwsRecord rec;
      PwsPassphrase passphrase;
      PwsPassphrasePolicy intermediatePolicy = null;
      PwsRawField raw;
      char[] symbols = null;
      byte[] data;
      String hstr;
      
      rec = new PwsRecord( 0 );
      rec.setInitialize( true );
      
      // read all fields until next EOR signal (or EOF)
      while ( reader.hasNext() ) {
         // read one FIELD
         raw = (PwsRawField)reader.next();

         // handle FIELD
         switch ( raw.getType() )
         {
         case RECIDTYPE:
            try { rec.setRecordID( new UUID( raw.getData()) ); 
            } catch ( IllegalArgumentException e ) {
               // bad UUID value
               Log.error( 3, "(PwsFileFactory.readRecordsV2) bad record UUID: " 
                     + Util.bytesToHex(raw.getData()) + 
                     "\r\ncreated new UUID: " + rec );
               file.setPreserveOld( true );
            }
            break;
            
         case ENDBLOCKTYPE:
            try {
               // look for intermediated data (policy)
               if ( intermediatePolicy != null ) {
                  if ( symbols != null ) {
                     intermediatePolicy.setOwnSymbols( symbols );
                     symbols = null;
                  }
                  try {
                     rec.setPassPolicy( intermediatePolicy );
                     if ( Log.getDebugLevel() >= 9 )
                        Log.debug( 9, "(PwsFileFactory.readRecordsV2) - adding intermediate POLICY to record " + rec );
                  } catch ( InvalidPassphrasePolicy e ) {
                     System.err.println( "(PwsFileFactory.readRecordsV2) *** invalid intermediate passphrase policy! " + rec );
                  }
                  intermediatePolicy = null;
               }
                  
               // terminate evaluation phase and add record to list
               rec.setInitialize( false );
               try { file.addRecord( rec ); 
               } catch (  DuplicateEntryException e ) {
                  // double record entry 
                  Log.error( 3, "(PwsFileFactory.readRecordsV2) duplicate record (not added): " + rec );
                  file.setPreserveOld( true );
               }

            } catch ( Exception e ) {
               // tolerates unexpected errors for the sake of further readings
            }
            
            // prepare next record  
            // (we always have a nice uuid, even if none is on the file)
            rec = new PwsRecord( 0 );
            rec.setInitialize( true );
            break;
            
         case TITLETYPE:
             rec.setTitle( raw.getString( charset ) );
             break;
             
         case PASSWORDTYPE:
             passphrase = raw.getPassphrase( charset );
             rec.setPassword( passphrase );
             passphrase.clear();
             break;

         case CREATIMETYPE:
             rec.setCreateTime( readTimeField( raw, rec ) );
             break;

         case RECORDMODTIMETYPE:
             rec.setModifyTime( readTimeField( raw, rec ) );
             break;

         case PASSMODTIMETYPE:
             rec.setPassModTime( readTimeField( raw, rec ) );
             break;

         case ACCESSTIMETYPE:
             rec.setAccessTime( readTimeField( raw, rec ) );
             break;
          
         case UNAMETYPE:
             passphrase = raw.getPassphrase( charset );
             rec.setUsername( passphrase );
             passphrase.clear();
             break;
             
         case GROUPTYPE:
            rec.setGroup( raw.getString( charset ) );
            break;
            
         case NOTESTYPE:
            passphrase = raw.getPassphrase( charset );
            rec.setNotes( passphrase );
            passphrase.clear();
            break;
            
         case EMAILTYPE:
            passphrase = raw.getPassphrase( charset );
            rec.setEmail( passphrase );
            passphrase.clear();
            break;

         case URLTYPE:
            if ( version > Global.FILEVERSION_2 ) {
               rec.setUrl( raw.getPassphrase( charset ) );
            } else {
               rec.addUnknownField( raw.getType(), raw.getData() );
            }
            break;
           
         case PASSPOLICYTYPE:
            // this is the "old" policy type and interpreted only if there isn't
            // an intermediate policy already defined  
            // (this excludes overwriting of a prior "modern" definition field)
            if ( intermediatePolicy == null ) {
               data = raw.getData();
               intermediatePolicy = new PwsPassphrasePolicy(Util.readIntLittle( data, 0 ));
               if ( Log.getDebugLevel() >= 9 )
                  Log.debug( 9, "(PwsFileFactory.readRecordsV2) - receiving OLD POLICY for record " + rec );
            }
            break;
            
         case MODERN_POLICYTYPE:
            hstr = raw.getString( charset );
            intermediatePolicy = new PwsPassphrasePolicy( hstr );
            if ( Log.getDebugLevel() >= 9 )
               Log.debug( 9, "(PwsFileFactory.readRecordsV2) - receiving MODERN POLICY for record " + rec );
            break;
            
         case OWNSYMBOLSTYPE:
            hstr = raw.getString( charset );
            symbols = hstr.toCharArray();
            if ( Log.getDebugLevel() >= 9 )
               Log.debug( 9, "(PwsFileFactory.readRecordsV2) - receiving OWN SYMBOLS for POLICY: " + 
                          new String( symbols ) + ", record = " + rec );
            break;
            
         case PASSLIFETIMETYPE:
            rec.setPassLifeTime( readTimeField( raw, rec ) );
            break;

         case EXPIRY_INTERVALTYPE:
            rec.setExpiryInterval( (int)readIntegerField( raw, rec ) );
            break;
         
         case HISTORYTYPE:
            if ( version > Global.FILEVERSION_2 ) {
               rec.setHistory( raw.getPassphrase( charset ) );
            } else {
               rec.addUnknownField( raw.getType(), raw.getData() );
            }
            break;
            
         case AUTOTYPETYPE:
            if ( version > Global.FILEVERSION_2 ) {
               rec.setAutotype( raw.getString( charset ) );
            } else {
               rec.addUnknownField( raw.getType(), raw.getData() );
            }
            break;
            
         case DCLICK_ACTIONTYPE:
            rec.dclickAction = (PwsRawField)raw.clone();
            break;

         case SHIFT_DCLICK_ACTIONTYPE:
            rec.shiftDclickAction = (PwsRawField)raw.clone();
            break;

         case RUNCOMMANDTYPE:
            rec.runCommand = (PwsRawField)raw.clone();
            break;

         case PROTECTED_ENTRYTYPE:
            rec.setProtectedEntry( raw.getLength() > 0 && raw.getData()[0] != 0 );
            break;

         case POLICY_NAMETYPE:
            rec.setPassPolicyName( raw.getString( charset ) );
            break;

         case KEYBOARD_SHORTCUT_TYPE:
        	 data = raw.getData();
        	 KeyStroke ks = readKeyStroke(data);
             rec.setKeyboardShortcut( ks );
             break;

         default: 
            rec.addUnknownField( raw.getType(), raw.getData() );
//System.err.println( "++ reading UNKNOWN FIELD: t=" + raw.getType() + 
//      ", v=" + Util.bytesToHex( raw.getData() ));
         }

         raw.destroy();
      } // while loop
   }  // readRecordsV23
   
   private static KeyStroke readKeyStroke ( byte[] data ) {
  	 int code = data[0];
  	 int mods = 0;
  	 int h = (int)data[3] & 0xFF;
  	 if ( (h & 1) == 1) mods += InputEvent.ALT_MASK;
  	 if ( (h & 2) == 2) mods += InputEvent.CTRL_MASK;
  	 if ( (h & 4) == 4) mods += InputEvent.SHIFT_MASK;
  	 if ( (h & 8) == 8) mods += InputEvent.ALT_GRAPH_MASK;
  	 if ( (h & 16) == 16) mods += InputEvent.META_MASK;
  	 KeyStroke ks = KeyStroke.getKeyStroke(code, mods);
  	 return ks;
   }
   
   /** Attempts to read the remainder of the file as records in the V1 blend.
    *  This algorithm expects a fixed implicit pattern of fields for each record,
    *  as it is typical for the V1 format.
    * 
    * @param file PwsFile, the target PwsFile
    * @param reader RawFieldReader, the reader object functioning as data source
    * @throws IOException
    */
   private static void readRecordsV1 ( PwsFile file, 
                                       PwsRawFieldReader reader )
   {
      PwsRecord     rec;
      PwsPassphrase passphrase;
      PwsRawField   raw;
      int           counter, i, splitLength;
      String        hstr;
      
      rec = null;
      counter = -1;
      splitLength = SPLITCHAR.length();
      
      // read all fields until EOF
      while ( reader.hasNext() ) {

    	 // read one FIELD 
         raw = (PwsRawField)reader.next();
         counter++;

         // handle FIELD
         switch ( counter % 3 )
         {
         // Field 0 (Title)
         case 0:
            // create new record
            rec = new PwsRecord( 0 );

            // read Title and Username (both from field 0)
            hstr = raw.getString( DEFAULT_CHARSET );
            Log.debug( 10, "V1, read TITLE raw: " + Util.bytesToHex( raw.getData() ));
            if ( (i=hstr.indexOf( SPLITCHAR )) > -1 ) {
               rec.setTitle( hstr.substring( 0, i ) );
               rec.setUsername( hstr.substring( i+splitLength ) );
            } else {
               rec.setTitle( hstr );
            }
            break;
            
         // Field 1 (Password)
         case 1:
            passphrase = raw.getPassphrase( DEFAULT_CHARSET );
            rec.setPassword( passphrase );
            passphrase.clear();
            break;

         // Field 2 (Notes)
         case 2:
            // set Notes
            passphrase = raw.getPassphrase( DEFAULT_CHARSET );
            rec.setNotes( passphrase );
            passphrase.clear();
            
            // store record
            try { file.addRecord( rec ); 
            } catch (  DuplicateEntryException e ) {
               // should not happen as we create UUIDs ourselves
            }
            break;
         }
         
         raw.destroy();
      }
   }  // readRecordsV1
   
   /** Creates an empty PWS file of the latest format at the given context file
    * destination. The user's file access key is passed in through a 
    * <code>PwsPassphrase</code>.
    * <p><small>If the file exists prior to writing, overwriting only occurs 
    * after an intermediary copy of the output has been created successfully. 
    * This safety copy has the ending ".temp" trailing the parameter filepath.
    * </small>
    *  
    * @param file <code>ContextFile</code> the target file in an application context   
    * @param passphrase the user's file access key (secret key)
    * @param iterations number of security key calculation iterations
    * @return UUID the file identifier value of the saved file or <b>null</b> if unavailable
    * @throws NullPointerException if any essential parameter is not defined
    * @throws IllegalArgumentException if <code>file</code> is empty
    * @throws ApplicationFailureException if the specified IO-context does not
    *         render an output stream
    * @throws IOException if an IO error occurs
    */   
   public static UUID makeFile ( ContextFile file, 
		                         PwsPassphrase passphrase, 
		                         int iterations )
         throws ApplicationFailureException, IOException
   {
      return saveFile(null, file, passphrase, null, iterations, 0 );
   }
   
   /** Creates a persistent state of a PWS file of a specified format version 
 *  from a record list defined through an iterator whose elements are of type
 *  <code>PwsRecord</code>.
 *  The destination of the file is determined by a filepath in the context of 
 *  the standard application (by default the local file system). 
 *  The user's file access key is passed in through a <code>PwsPassphrase</code>.
 *  <p>If the file exists prior to writing, overwriting only occurs after an
 *  intermediary copy of the output has been created successfully. (This 
 *  safety copy has the ending ".temp" trailing the parameter filepath.)
 *  
 * @param records Iterator with element type <code>PwsRecord</code> or <b>null</b>
 *        for empty database
 * @param filepath the target file path valid in the standard IO-context
 *        (by default the local file system)
 * @param pass the user's file access passphrase (secret key)
 * @param headerFields header field list to be included in the file or <b>null</b>
 * @param iterations number of security key calculation iterations
 * @param format the file format version (values of <code>Global</code>)
 *        (0 defaults to latest version)
 * @return UUID the file identifier value of the saved file or <b>null</b> if unavailable
 * @throws NullPointerException if any essential parameter is not defined
 * @throws IllegalArgumentException if <code>filepath</code> is empty
 * @throws ApplicationFailureException if the IO-context does not
 *         render an output stream
 * @throws IOException if an IO error occurs
 */   
public static final UUID saveFile ( Iterator<PwsRecord> records, 
                                    String filepath,
                                    PwsPassphrase pass,
                                    HeaderFieldList headerFields,
                                    int iterations,
                                    int format )
   throws IOException, ApplicationFailureException
{
   return saveFile( records, Global.getStandardApplication(), filepath, pass, headerFields, iterations, format );
}

/** Creates a persistent state of a PWS file of a specified format version 
  *  from a record list defined through an iterator whose elements are of type <code>PwsRecord</code>.
  *  The destination of the file is determined by an <code>ApplicationAdapter</code> 
  *  and a filepath. The user's file access key is passed in through a <code>PwsPassphrase</code>.
  *  <p>If the file exists prior to writing, overwriting only occurs after an
  *  intermediary copy of the output has been created successfully. (This 
  *  safety copy has the ending ".temp" trailing the parameter filepath.)
  *  
  * @param records Iterator with element type <code>PwsRecord</code> or <b>null</b>
  *        for empty database
  * @param app the <code>ApplicationAdapter</code> IO-context
  * @param filepath the target file path valid in IO-context
  * @param passphrase the user's file access key (secret key)
  * @param headerFields header field list to be included in the file or <b>null</b>
  * @param iterations number of security key calculation iterations
  * @param format the file format version (values of <code>Global</code>)
  *        (0 defaults to latest version)
  * 
  * @return UUID the file identifier value of the saved file or <b>null</b> if unavailable
  * 
  * @throws NullPointerException if any essential parameter is not defined
  * @throws IllegalArgumentException if <code>filepath</code> is empty
  * @throws ApplicationFailureException if the specified IO-context does not
  *         render an output stream
  * @throws IOException if an IO error occurs
  */   
  protected static final UUID saveFile ( Iterator<PwsRecord> records, 
                                      ApplicationAdapter app,
                                      String filepath,
                                      PwsPassphrase passphrase,
                                      HeaderFieldList headerFields,
                                      int iterations,
                                      int format )
         throws IOException, ApplicationFailureException
   {
       return saveFile( records, new ContextFile(app, filepath), passphrase, 
                        headerFields, iterations, format );
   }  // saveFile

/** Creates a persistent state of a PWS file of a specified format version 
    *  from a record list defined through an iterator whose elements are of type <code>PwsRecord</code>.
    *  The destination of the file is determined by a <code>ContextFile</code> 
    *  parameter. The user's file access key is passed in through a <code>PwsPassphrase</code>.
    *  <p>If the file exists prior to writing, overwriting only occurs after an
    *  intermediary copy of the output has been created successfully. (This 
    *  safety copy has the ending ".temp" trailing the parameter filepath.)
    *  
    * @param records Iterator with element type <code>PwsRecord</code> or <b>null</b>
    *        for empty database
    * @param file <code>ContextFile</code> the target file in an application context   
    * @param passphrase the user's file access key (secret key)
    * @param headerFields header field list to be included in the file or <b>null</b>
    * @param iterations number of security key calculation iterations
    * @param format the file format version (values of <code>Global</code>)
    *        (0 defaults to latest version)
    * @return UUID the file identifier value of the saved file or <b>null</b> if unavailable
    * 
    * @throws NullPointerException if any essential parameter is not defined
    * @throws IllegalArgumentException if <code>filepath</code> is empty
    * @throws ApplicationFailureException if the specified IO-context does not
    *         render an output stream
    * @throws IOException if an IO error occurs
    */   
   public static final UUID saveFile ( Iterator<PwsRecord> records, 
                                       ContextFile file,
                                       PwsPassphrase passphrase,
                                       HeaderFieldList headerFields,
                                       int iterations,
                                       int format )
      throws IOException, ApplicationFailureException
{
      UUID fid;
      String swapPath = null;
    
      Log.log( 5, "(PwsFileFactory) saveFile mark A0" );
      if ( file == null | passphrase == null )
         throw new NullPointerException( "missing save parameter(s)" );

      // default empty record list for null parameter
      if ( records == null ) {
         records = new ArrayList<PwsRecord>().iterator();
      }
            
      ApplicationAdapter app = file.getAdapter();
      String filepath = file.getFilepath();

      // perform swapping of file names if file already exists
      // ("swapPath" is an intermediary file to take on the new db state)
//      Log.log( 5, "(PwsFileFactory) saveFile mark A1" );
      boolean swap = app.existsFile( filepath ) && 
    		         app.canDelete( filepath ) &&
    		         app.getFileLength( filepath ) > 0;
      if ( swap ) {
         Log.log( 5, "(PwsFileFactory) saveFile mark A2 (decided for swapping)" );
         swapPath = filepath + ".temp";
      } else {
          Log.log( 5, "(PwsFileFactory) saveFile mark A2 (decided for create file)" );
      }
      
      // create file for (new) content of database 
      OutputStream out = swap ? StreamFactory.getOutputStream( app, swapPath ) 
    		                  : file.getOutputStream();
      try {
         // save all listed records
         Log.log( 5, "(PwsFileFactory) saveFile mark A3 (writing primary file)" );
         fid = saveFile( records, out, passphrase, headerFields, iterations, format );
      } finally {
         Log.log( 5, "(PwsFileFactory) saveFile mark A4 (closing primary output)" );
         out.close();
      }
      
      // swap file names if triggered and delete intermediary file
      if ( swap ) {
         Log.log( 5, "(PwsFileFactory) saveFile mark A5 (swap - removing original file)" );
         app.deleteFile( filepath );
         if ( !app.renameFile( swapPath, filepath ) ) {
        	InputStream in = StreamFactory.getInputStream( app, swapPath );
            Log.log( 5, "(PwsFileFactory) saveFile mark A6 (swapping by copy - input open)" );
            try {
               out = file.getOutputStream();
               Log.log( 5, "(PwsFileFactory) saveFile mark A7 (swapping by copy - output open)" );
               Util.copyStream( in, out );
            } finally {
               Log.log( 5, "(PwsFileFactory) saveFile mark A8 (swapping by copy - closing streams)" );
               in.close();
               out.close();
            }
            Log.log( 5, "(PwsFileFactory) saveFile mark A9 (swapping by copy - removing temp file)" );
            app.deleteFile( swapPath );
         } else {
            Log.log( 5, "(PwsFileFactory) saveFile mark A6 (renamed temp file to original)" );
         }
      }
   
      Log.log( 5, "(PwsFileFactory) saveFile mark A10 (save done)" );
      return fid;
}  // saveFile

/** Creates a persistent state of a PWS file of a specified format version 
 *  from a record list defined through an iterator whose elements are of 
 *  type <code>PwsRecord</code>. 
 *  The destination of the file is the parameter output stream, which is
 *  <i>not</i> closed upon termination of this operation!
 *  The user's file access key is passed in through a <code>PwsPassphrase</code>.
 *  
 * @param records Iterator with element type <code>PwsRecord</code> or <b>null</b>
 *        for empty database
 * @param out a target output stream
 * @param passphrase the user's file access key (secret key)
 * @param headerFields <code>RawFieldList</code> with file specific data (e.g. user options) or <b>null</b>
 * @param iterations number of security key calculation iterations
 * @param format the file format version (values of <code>Global</code>)
 *        (0 defaults to latest version)
 * @return UUID the file identifier value of the saved file or <b>null</b> if unavailable
 * @throws NullPointerException if <code>out</code> or <code>passphrase</code> 
 *         is not defined
 * @throws IOException if an IO error occurs
 */   
public static final UUID saveFile ( Iterator<PwsRecord> records, 
                                    OutputStream out,
                                    PwsPassphrase passphrase,
                                    HeaderFieldList headerFields,
                                    int iterations,
                                    int format )
   throws IOException
{
   PwsFileOutputSocket socket;
   PwsFileOutputSocket.RawFieldWriter writer;
 
   Log.log( 5, "(PwsFileFactory) saveFile mark B0 - open socket to output stream" );
   if ( passphrase == null | out == null )
      throw new NullPointerException( "missing save parameter(s)" );
   
   if ( format == 0 ) {
      format = Global.FILEVERSION_LATEST_MAJOR;
   }
   
   if ( records == null ) {
      records = new ArrayList<PwsRecord>().iterator();
   }
         
   
   // create output socket for file creation
//   Log.log( 5, "(PwsFileFactory) saveFile mark B1" );
   socket = new PwsFileOutputSocket( out, passphrase, headerFields, format );
//   Log.log( 5, "(PwsFileFactory) saveFile mark B1.1" );
   socket.setIterations( iterations );
//   Log.log( 5, "(PwsFileFactory) saveFile mark B1.2" );
   writer = socket.getRawFieldWriter();
   
   // save all listed records
   Log.log( 5, "(PwsFileFactory) saveFile mark B2 - start writing records" );
   switch ( format )
   {
   case Global.FILEVERSION_1:
      saveRecordsV1( records, writer );
      break;
      
   case Global.FILEVERSION_2:
      // determine active charset
      String options = headerFields == null ? "" : 
    	     headerFields.getStringValue( PwsFileHeaderV3.JPWS_OPTIONS_TYPE );
      String charset = options.indexOf( "B 24 1" ) > -1  ? UTF_CHARSET : DEFAULT_CHARSET;
      saveRecordsV2( records, writer, charset );
      break;
      
   case Global.FILEVERSION_3:
      saveRecordsV3( records, writer );
      break;
      
   default:
      throw new IllegalStateException( "unknown save format" );
   }
   
   Log.log( 5, "(PwsFileFactory) saveFile mark B3 - closing output socket" );
   socket.close();
   return socket.getFileID();
}  // saveFile

/** Stores a list of records to an output stream represented by 
 *  the <code>PwsFileOutputSocket.RawFieldWriter</code> in the format version V2. 
 *  Invalid records are silently ignored. 
 * 
 *  @param charset the character set used to encode text data of the record
 *  @throws IOException
 */ 
private static void saveRecordsV2 ( Iterator<PwsRecord> records, 
                                    PwsFileOutputSocket.RawFieldWriter writer, 
                                    String charset )   throws IOException
{
   while ( records.hasNext() ) {
	   
	   PwsRecord rec = (PwsRecord)records.next();
   
      // save pure text fields (always store!) 
      saveTextFieldV2( TITLETYPE, rec.getTitle(), writer, charset );
      saveTextFieldV2( GROUPTYPE, rec.getGroup(), writer, charset );
      
      // treat the password fields (always store!)
      savePasswordFieldV2( PASSWORDTYPE, rec.getPassword(), writer, charset );
      savePasswordFieldV2( NOTESTYPE, rec.getNotesPws(), writer, charset );
      savePasswordFieldV2( UNAMETYPE, rec.getUsernamePws(), writer, charset );
      
      // treat the policy field (store if defined)
      PwsPassphrasePolicy policy = rec.getPassPolicy();
      if ( policy != null ) {
         byte[] block = new byte[4];
         Util.writeIntLittle( policy.getIntForm(), block, 0 );
         saveByteArray( PASSPOLICYTYPE, block, writer );
      }
      
      // save time fields (store if not zero) 
      saveTimeField( CREATIMETYPE, rec.getCreateTime(), writer );
      saveTimeField( ACCESSTIMETYPE, rec.getAccessTime(), writer );
      saveTimeField( RECORDMODTIMETYPE, rec.getModifiedTime(), writer );
      saveTimeField( PASSMODTIMETYPE, rec.getPassModTime(), writer );
      saveTimeField( PASSLIFETIMETYPE, rec.getPassLifeTime(), writer );
      
      // save unknown field types
      saveUnknownFields( rec, writer );
   
      // save record admin data (always store)
      saveByteArray( RECIDTYPE, rec.getRecordID().getBytes(), writer );
      saveByteArray( ENDBLOCKTYPE, ZEROBYTEARRAY, writer );
   }
}  // saveRecordsV2

/** Stores a list of records to an output stream represented by 
 *  the <code>PwsFileOutputSocket.RawFieldWriter</code> in the format version V3. 
 *  Invalid records are silently ignored. 
 * 
 *  @throws IOException
 */ 
private static void saveRecordsV3 ( Iterator<PwsRecord> records, 
		                            PwsFileOutputSocket.RawFieldWriter writer )
            throws IOException
{
   while ( records.hasNext() ) {
	   
	   PwsRecord rec = records.next();
   
      // save pure text fields (always store!) 
      saveTextFieldV3( TITLETYPE, rec.getTitle(), writer );
      saveTextFieldV3( GROUPTYPE, rec.getGroup(), writer );
      
      // treat the encrypted fields (always store!)
      savePasswordFieldV3( PASSWORDTYPE, rec.getPassword(), writer );
      savePasswordFieldV3( NOTESTYPE, rec.getNotesPws(), writer );
      savePasswordFieldV3( UNAMETYPE, rec.getUsernamePws(), writer );
      
      // treat the policy field (store if defined)
      PwsPassphrasePolicy policy = rec.getPassPolicy();
      if ( policy != null ) {
         // always write "new" structure (0x10 as of PWS format 3.6)
         String hstr = policy.getModernForm();
         if ( Log.getDebugLevel() > 8 )
         Log.debug( 9, "(PwsFileFactory.saveRecordV3) store POLICY MODERN: ".concat(hstr) );
         saveTextFieldV3( MODERN_POLICYTYPE, hstr, writer );

         // additionally write "Own Symbols" record field if policy carries
         if ( policy.hasOwnSymbols() ) {
            hstr = new String( policy.getOwnSymbols() );
            if ( Log.getDebugLevel() > 8 )
            Log.debug( 9, "(PwsFileFactory.saveRecordV3) store POLICY OWN SYMBOLS: ".concat( hstr ) );
            saveTextFieldV3( OWNSYMBOLSTYPE, hstr, writer );
         }
      }

      // V3 specific fields
      savePasswordFieldV3( URLTYPE, rec.getUrlPws(), writer );
      savePasswordFieldV3( EMAILTYPE, rec.getEmailPws(), writer );
      saveTextFieldV3( AUTOTYPETYPE, rec.getAutotype(), writer );
      savePasswordFieldV3( HISTORYTYPE, rec.getHistoryPws(), writer );
      saveTextFieldV3( POLICY_NAMETYPE, rec.getPassPolicyName(), writer );
      saveBooleanField( PROTECTED_ENTRYTYPE, rec.getProtectedEntry(), writer );
      
      // save time fields (store if not zero) 
      saveTimeField( CREATIMETYPE, rec.getCreateTime(), writer );
      saveTimeField( ACCESSTIMETYPE, rec.getAccessTime(), writer );
      saveTimeField( RECORDMODTIMETYPE, rec.getModifiedTime(), writer );
      saveTimeField( PASSMODTIMETYPE, rec.getPassModTime(), writer );
      saveTimeField( PASSLIFETIMETYPE, rec.getPassLifeTime(), writer );
      saveIntegerField( EXPIRY_INTERVALTYPE, rec.getExpiryInterval(), writer );
      KeyStroke ks = rec.getKeyboardShortcut(); 
      if ( ks != null ) {
   	      saveByteArray( KEYBOARD_SHORTCUT_TYPE, convertKeyStroke(ks), writer );
      }
     
      // save unused canonical fields
      saveRawField( rec.runCommand, writer );
      saveRawField( rec.dclickAction, writer );
      saveRawField( rec.shiftDclickAction, writer );

      // save unknown field types
      saveUnknownFields( rec, writer );
   
      // save record admin data (always store)
      saveByteArray( RECIDTYPE, rec.getRecordID().getBytes(), writer );
      saveByteArray( ENDBLOCKTYPE, ZEROBYTEARRAY, writer );
   }
}  // saveRecordsV3

private static byte[] convertKeyStroke( KeyStroke ks ) {
	byte[] data = new byte[4];
	data[0] = (byte)ks.getKeyCode();
	int h = ks.getModifiers();
	int mods = 0;
	if ( (h & InputEvent.ALT_MASK) == InputEvent.ALT_MASK ) mods += 1;
	if ( (h & InputEvent.CTRL_MASK) == InputEvent.CTRL_MASK ) mods += 2;
	if ( (h & InputEvent.SHIFT_MASK) == InputEvent.SHIFT_MASK ) mods += 4;
	if ( (h & InputEvent.ALT_GRAPH_MASK) == InputEvent.ALT_GRAPH_MASK ) mods += 8;
	if ( (h & InputEvent.META_MASK) == InputEvent.META_MASK ) mods += 16;
	data[3] = (byte)mods;
	return data;
}

/** Saves a basic raw-field to the writer.
 * Does nothing if raw is <b>null</b> or the content is empty.
 * 
 * @param raw <code>PwsRawField</code>
 * @param writer <code>PwsFileOutputSocket.RawFieldWriter</code>
 * @throws IOException
 */
private static void saveRawField ( PwsRawField raw, 
		                           PwsFileOutputSocket.RawFieldWriter writer )
               throws IOException
{
   if ( raw != null && raw.data != null ) {
      writer.writeRawField( raw );
   }
}

/**
 * Saves an unknown field list of a record, applying field type validity 
 * controls and log reports.
 * 
 * @param rec PwsRecord
 * @param writer PwsFileOutputSocket.RawFieldWriter
 * @throws IOException
 */
private static void saveUnknownFields ( PwsRecord rec, PwsFileOutputSocket.RawFieldWriter writer )
   throws IOException
{
   Iterator<PwsRawField> it = rec.getUnknownFields();
   if ( it != null ) {
      while ( it.hasNext() ) {
    	 PwsRawField ufld = it.next();
         if ( (ufld.type & ~0xff) == 0 & 
        	  !isCanonicalField( ufld.type, writer.getFormat() ) ) {
            saveRawField( ufld, writer );

            // logging
            if ( Log.getDebugLevel() > 5 ) {
               String hstr = "-- saving UNKNOWN FIELD (" + rec + ") id=" + 
                      Util.byteToHex( ufld.type ) + ", val=" + Util.bytesToHex( ufld.data );
               Log.debug(6, hstr);
            }
         
         } else {
            Log.error( 3, "(PwsFileFactory.saveRecord) *** Illegal unknown field type: " 
                  + ufld.type + " for " + rec );
         }
      }
   }
}  // saveUnknownFields

/** Stores a list of records to an output stream represented by 
 *  the <code>PwsFileOutputSocket.RawFieldWriter</code> in the format version V1. 
 *  Invalid records are silently ignored. 
 * 
 *  @throws IOException
 */ 
private static void saveRecordsV1 ( Iterator<PwsRecord> records, 
		                            PwsFileOutputSocket.RawFieldWriter writer )
            throws IOException
{
   String charset = DEFAULT_CHARSET;
   
   while ( records.hasNext() ) {
	  PwsRecord rec = records.next();
      if ( !rec.isValid() ) continue;

      // possible composition of title + username
      String hstr = rec.getTitle();
      String user = rec.getUsername(); 
      if ( user != null ) {
         hstr = hstr + SPLITCHAR + user;
      }
      saveTextFieldV2( 0, hstr, writer, charset );

      // passphrase
      savePasswordFieldV2( 0, rec.getPassword(), writer, charset );
      
      // notes
      savePasswordFieldV2( 0, rec.getNotesPws(), writer, charset );
   }
}  // saveRecordsV1

/** Whether the parameter field type is a canonical field
 *  (conventional meaning) in the context of the given file format.
 *  
 *  @param type the field type in question
 *  @param format the reference file format or 0 for default
 */
public static boolean isCanonicalField (int type, int format)
{
   int boundary = format == 0 | format >= Global.FILEVERSION_3 ? 
      LAST_CANONICAL_BLOCK_FIELD : 0x0c;
   return type > -1 && (type <= boundary || type == 0xff);
}

/** Stores the content of a <code>PwsPassphrase</code> as a data field of the 
 *  specified type into an output stream represented by <code>BlockWriter</code>. 
 *  The underlying cleartext string is represented as a sequence of bytes 
 *  depending on the specified charset. 
 *  This always stores a field and if it is empty.
 *  
 *  @throws IOException
 *  @throws IllegalStateException if <code>charset</code> is not supported
 *          by the running JVM
 */
private static void savePasswordFieldV2 ( int type, 
		                           PwsPassphrase pass, 
                                   PwsFileOutputSocket.RawFieldWriter writer, 
                                   String charset)
      throws IOException
{
   if ( pass == null ) {
      pass = new PwsPassphrase();
   }

   byte[] block = pass.getBytes(charset);
   saveByteArray(type, block, writer);
   Util.destroyBytes(block);
}

/** Stores the content of a <code>PwsPassphrase</code> as a data field of the 
 *  specified type into an output stream represented by <code>BlockWriter</code>. 
 *  The underlying cleartext string is represented as a sequence of bytes 
 *  depending on the specified charset. 
 *  <p>Returns undone if the passphrase is <b>null</b> or its length is 0!
 *  
 *  @throws IOException
 *  @throws IllegalStateException if <code>charset</code> is not supported
 *          by the running JVM
 */
private static void savePasswordFieldV3 ( int type, 
		                       PwsPassphrase pass, 
                               PwsFileOutputSocket.RawFieldWriter writer )
      throws IOException
{
   if ( pass == null || pass.getLength() == 0 ) return;

   savePasswordFieldV2( type, pass, writer, UTF_CHARSET );
}

/** Stores a text <code>String</code> as a data field of the specified type into  
 *  a data sink represented by <code>PwsFileOutputSocket.RawFieldWriter</code>. 
 *  The string is represented as a sequence of bytes encoded for the specified charset. 
 *  <p>Stores as empty field if <code>text</code> is <b>null</b>!
 *  
 *  @throws IOException
 *  @throws IllegalStateException if <code>charset</code> is not supported
 *          by the running JVM
 */
private static void saveTextFieldV2 ( int type, String text, 
            PwsFileOutputSocket.RawFieldWriter writer, String charset )
            throws IOException
{
   if ( text == null ) {
      text = "";
   }
   
   try {
	  byte[] data = text.getBytes(charset);
	  PwsRawField raw = new PwsRawField(type, data);
      Util.destroyBytes(data);
      writer.writeRawField(raw);
      raw.destroy();

   } catch ( UnsupportedEncodingException e ) {
      throw new IllegalStateException("*** JPWS big trouble! unsupported text encoding: " 
            .concat(charset));
   }
}  // saveTextFieldV2

/** Stores a text <code>String</code> as a data field of the specified type into  
 *  an output stream represented by <code>PwsFileOutputSocket.RawFieldWriter</code>. The string is
 *  represented as a sequence of bytes encoded for the specified charset. 
 *  <p>Returns undone if <code>text</code> is <b>null</b> or empty!
 *  
 *  @throws IOException
 *  @throws IllegalStateException if <code>charset</code> is not supported
 *          by the running JVM
 */
private static void saveTextFieldV3 (int type, String text, 
            PwsFileOutputSocket.RawFieldWriter writer)
            throws IOException
{
   if ( text == null || text.isEmpty() )
      return;

   saveTextFieldV2(type, text, writer, UTF_CHARSET);
}  // saveTextFieldV3

/** Stores a time value as a data field of the specified type into  
 *  an output stream represented by <code>PwsFileOutputSocket.RawFieldWriter</code>. 
 *  <p>Returns undone if <code>time</code> is zero!
 * 
 *  @param type the target field type
 *  @param time a long value describing milliseconds since epoch
 *  @throws IOException
 */
private static void saveTimeField (int type, 
		                           long time, 
		                           PwsFileOutputSocket.RawFieldWriter writer)
               throws IOException
{
   if ( time == 0 ) return;
   
//   byte[] barr = new byte[8];
//   Util.writeLongLittle( time/1000, barr, 0 );
   
   // since 2-1-0 we store time values as 32-bit unsigned integers
   // for reasons of compatibility with PWS format definition  
   byte[] barr = new byte[4];
   Util.writeIntLittle( (int)(time/1000), barr, 0 );
   writer.writeRawField( new PwsRawField(type, barr) );
}  // saveTextField

/** Stores a 4-byte integer value in Little-Endian format as a data field 
 * of the specified type into an output stream represented by 
 * <code>PwsFileOutputSocket.RawFieldWriter</code>. 
 *  Does nothing if <code>value</code> is zero!
 * 
 *  @param type the target field type
 *  @param value <b>int</b> value to be stored
 *  @throws IOException
 */
private static void saveIntegerField ( int type, int value, 
		                         PwsFileOutputSocket.RawFieldWriter writer )
               throws IOException
{
   if ( value == 0 ) return;

   byte[] barr = new byte[4];
   Util.writeIntLittle(value, barr, 0);
   writer.writeRawField( new PwsRawField( type, barr ) );
}  // saveTextField

/** Stores a boolean value in a data field. A Boolean is stored
 * in one byte with the convention that zero is <b>false</b>
 * and <b>true</b> otherwise. 
 *  Does nothing if <code>value</code> is <b>false</b>!
 * 
 *  @param type the target field type
 *  @param value boolean 
 *  @throws IOException
 */
private static void saveBooleanField (int type, boolean value, 
               PwsFileOutputSocket.RawFieldWriter writer) 
               throws IOException
{
   if ( !value ) return;
   
   byte[] b = new byte[1];
   b[0] = (byte)(value ? 0xff : 0);
   writer.writeRawField( new PwsRawField(type, b) );
}  // saveBooleanField

/** Extracts an integer value from a rawfield. It is assumed that
 *  binary integer is stored in Little-Endian format (least significant
 *  stored first). Data may be of any length but maximum 8 first bytes
 *  are evaluated. The record reference is for error logging only.
 * 
 *  @return long extracted integer value
 */
private static long readIntegerField ( PwsRawField raw, PwsRecord rec )
{
   try {
      byte[] a = raw.getData();
      if ( a.length < 4 ) {
         a = Util.arraycopy( a, 4 );
      } else if ( a.length > 4 ) {
         a = Util.arraycopy( a, 8 );
      }
      return a.length == 4 ? Util.readUIntLittle( a, 0 ) 
                           : Util.readLongLittle( a, 0 );
   } catch ( Exception e ) {
      Log.error( 3, "(PwsFileFactory.readIntegerField) *** invalid integer data: " + rec );
      return 0;
   }
}
   
/** Extracts a time value from a rawfield. (The record reference is for error
 *  logging only.)
 * 
 *  @return milliseconds after epoch
 */
private static long readTimeField ( PwsRawField raw, PwsRecord rec )
{
   try {
      return (raw.length == 4 ? Util.readUIntLittle( raw.getData(), 0 ) 
             : Util.readLongLittle( raw.getData(), 0 )) * 1000;
   } catch ( Exception e ) {
      Log.error( 3, "(PwsFileFactory.readTimeField) *** invalid time data: " + rec );
      return 0;
   }
}

/** Stores a byte array as a raw data field of the specified type into  
 *  an data sink represented by <code>PwsFileOutputSocket.RawFieldWriter</code>. 
 *  <p>Returns undone if <code>data</code> is <b>null</b>!
 *  
 *  @throws IOException
 */
private static void saveByteArray ( int type, byte[] data, 
		                            PwsFileOutputSocket.RawFieldWriter writer )
         throws IOException
{
   if ( data == null ) return;
   
   PwsRawField raw = new PwsRawField( type, data );
   writer.writeRawField( raw );
   raw.destroy();
}  // saveByteArray

   
}
