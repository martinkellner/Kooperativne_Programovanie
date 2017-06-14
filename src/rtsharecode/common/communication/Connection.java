package rtsharecode.common.communication;

import java.util.regex.Pattern;

import org.eclipse.ecf.core.ContainerConnectException;
import org.eclipse.ecf.core.ContainerCreateException;
import org.eclipse.ecf.core.ContainerFactory;
import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.core.identity.ID;
import org.eclipse.ecf.core.identity.IDFactory;
import org.eclipse.ecf.core.security.ConnectContextFactory;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;

import rtsharecode.common.Activator;
import rtsharecode.common.gui.PreferencesConstants;

/**
 * Trieda Connection - vytv�ranie spojenia s XMPP serverom. 
 * @author Martin Kellner
 *
 */
public class Connection {
	
	private String LOGIN;
	private String PASSWORD;
	private String SERVER;
	private String GROUP;	
	private final String TYPE = "ecf.xmpp.smack";
	private final String POSTFIX = "/ecf";
	private IContainer container;
	private ID myID;
	private ID broadcastID;
	private boolean isLogin = false;
	private boolean isAdmin = false;
	private static Connection instance = null;
	
	/**
	 * Pod�a navrhov�ho vzoru singleton.
	 * @return Connection - in�tacia.
	 */
	public static Connection getInstance() {
		if (instance == null) {
			instance = new Connection();
		}
		return instance;
	}
		
	/**
	 * Ak je pou��vate� u�ite� - true, inak false.
	 * @return boolean
	 */
	public boolean isAdmin() {
		return isAdmin;
	}
	
	/**
	 * Vr�ti pou��vate�sk� meno
	 * @return String user
	 */
	public String getLoginName() {
		return LOGIN;
	}
	
	/**
	 * Vytvor� identifik�tor pou��vate�a pre server.
	 * @return ID id
	 */
	protected ID createMultiCastID() {
		if (broadcastID == null) {
			broadcastID = IDFactory.getDefault().createID("ecf.xmpp" , GROUP + "@broadcast.openfire.server/ ");
		}
		return broadcastID;
	}
	
	/**
	 * Kontroluje spr�vnos� prihlasovac�ch d�t pred prihl�sen�m. True, ak s� d�ta v poriadku, inak false.
	 * @return boolean
	 */
	public boolean controlData() {
		return LOGIN != null && PASSWORD != null && SERVER != null && !LOGIN.isEmpty() && !PASSWORD.isEmpty() && !SERVER.isEmpty();
	}
	
	/**
	 * Vytvor� kontainer, ktor� poskytuje Connection na server.
	 * @throws ContainerCreateException
	 */
	private void createIContainer() throws ContainerCreateException {
		if (container == null) {
			container = ContainerFactory.getDefault().createContainer(TYPE);
		}
	}
	
	/**
	 * Vr�ti identifik�tor pre server.
	 * @return ID myID
	 */
	protected ID getMyID() {
		return myID;
	}
	
	/**
	 * Vytiahne a nastav� prihlasovacie �daje.
	 * @param IPreferenceStore iPreferenceStore - referencia na �lo�isko.
	 */
	public void setPreferences(IPreferenceStore iPreferenceStore) {
		SERVER = iPreferenceStore.getString(PreferencesConstants.XMPP_SERVER);
		LOGIN = iPreferenceStore.getString(PreferencesConstants.XMPP_LOGIN);
		PASSWORD = iPreferenceStore.getString(PreferencesConstants.XMPP_PASSWORD);
		GROUP = iPreferenceStore.getString(PreferencesConstants.XMPP_GROUP);
	}
	
	/**
	 * Vytvor� identifik�tor pre server (myID).
	 */
	private void createTargetID() {
		if (myID == null) {
			if (container != null) {
				myID = IDFactory.getDefault().createID(container.getConnectNamespace(), LOGIN + "@" + SERVER + POSTFIX);
			} else {
				throw new NullPointerException("Container is null");
			}
		}
	}
	
	/**
	 * Vytvor� identifik�tor pre server pod�a mena
	 * @param login - meno
	 * @return ID
	 */
	protected ID createTargetID(String login) {
		if (container != null) {
			 return IDFactory.getDefault().createID(container.getConnectNamespace(), login + "@openfire.server" + POSTFIX);
		} else {
			return null;
		}
	}
	
	/**
	 * Odh�senie zo servera.
	 */
	public void disconnect() {
		if (container != null) {
			if (getMyID() != null && container.getConnectedID().equals(getMyID())) {
				resetIsLogin();
				container.disconnect();
				
			}
		}
	}
	
	/**
	 * Kontrola stavu pre pripojenie. Ak, je v�etko v poriadku prihl�si sa. Vr�ti true, ak sa podarilo prihl�si�, inak false.
	 * @return boolean
	 */
	public boolean tryConnect() {
		resetUserData();
		Activator.getDefault().updatePreferencesStore();
		try {
			connect();
		} catch (ContainerCreateException | ContainerConnectException e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Prihl�senie nebolo �spe�n�.",
					"Pri prihl�sen� nastala chyba, uistite sa, �e m�te spr�vne nastavan� vstupn� parametre.");
		}
		return isLogin();
	}
	
	/**
	 * Pripojenie na server
	 * @throws ContainerCreateException
	 * @throws ContainerConnectException
	 */
	private void connect() throws ContainerCreateException, ContainerConnectException {
		setPreferences(Activator.getDefault().getPreferenceStore());
		if (container == null) {
			createIContainer();
		}
		if (myID == null) {
			createTargetID();
		}
		container.connect(myID, ConnectContextFactory.createPasswordConnectContext(PASSWORD));
		isLogin = true;
		setAdmin();
		Activator.getDefault().initializeMessenger();
	}
	
	/**
	 * Ak je pou��vate�sk� meno v tvare pre u�ite�a, tak nastav� hodnotu isAdmin na true, inak false.
	 */
	private void setAdmin() {
		Pattern pattern = Pattern.compile("^admin[0-9]@" + SERVER + "$"); 
		if (isLogin() && getMyID() != null) 
			isAdmin = pattern.matcher(getMyID().getName()).matches();
	}

	/**
	 * Vr�ti kontainer.
	 * @return Container container
	 */
	protected IContainer getContainer() {
		return container;
	}	
	
	/**
	 * Prema�e prihlasovacie �daje.
	 */
	private void resetUserData() {
		LOGIN = null;
		PASSWORD = null;
		GROUP = null;
		SERVER = null;
		myID = null;
		isLogin = false;
		isAdmin = false;
	}
	
	/**
	 * Nastav� hodnoty isAdmin a isLogin na false.
	 */
	private void resetIsLogin() {
		isAdmin = false;
		isLogin = false;
	}
	
	/**
	 * Vr�ti hodnotu isLogin.
	 * @return boolean isLogin
	 */
	public boolean isLogin() {
		return isLogin;
	}
	
	/**
	 * Vr�ti skupinu.
	 * @return String group
	 */
	public String getGroup() {
		return GROUP;
	}
	
	/**
	 * Zma�e in�tanciu.
	 */
	public void destroy() {
		instance = null;
	}	
}