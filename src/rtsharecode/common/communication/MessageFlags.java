package rtsharecode.common.communication;
/**
 * Trieda MessageFlags - znaková reprezentácia komunikaèných konštánt.
 * @author Martin Kellner
 *
 */
public class MessageFlags {
	
	public static final String SHARE = "s#";
		
	public static final String ID = "i#";
	
	public static final String QUERY = "q#";
	
	public static final String RESPONSE = "r#";
	
	public static final String SYNC = "y#";
	
	public static final String ACK = "o#";

	public static final String FILESYNC = "f#";
	
	public static final String FILECHANGE = "c#";

	public static final String SEPARATOR = "#";

	public static final String NEWFILECONTENT = "n#";

	public static final String NOTIFICATION = "m#";
	
	public static final String TIMESTAMP = "t#";

	public static final String ISSHARE = "p#";

	public static final String INVATE_QUERY = "e#";

	public static final String ACCESS = "a#";
	
	public static final String STOP_SHARE = "x#";
}
