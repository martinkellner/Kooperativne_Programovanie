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
 * Trieda Connection - vytv·ranie spojenia s XMPP serverom. 
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
	 * Podæa navrhovÈho vzoru singleton.
	 * @return Connection - inötacia.
	 */
	public static Connection getInstance() {
		if (instance == null) {
			instance = new Connection();
		}
		return instance;
	}
		
	/**
	 * Ak je pouûÌvateæ uËiteæ - true, inak false.
	 * @return boolean
	 */
	public boolean isAdmin() {
		return isAdmin;
	}
	
	/**
	 * Vr·ti pouûÌvateÂskÈ meno
	 * @return String user
	 */
	public String getLoginName() {
		return LOGIN;
	}
	
	/**
	 * VytvorÌ identifik·tor pouûÌvateæa pre server.
	 * @return ID id
	 */
	protected ID createMultiCastID() {
		if (broadcastID == null) {
			broadcastID = IDFactory.getDefault().createID("ecf.xmpp" , GROUP + "@broadcast.openfire.server/ ");
		}
		return broadcastID;
	}
	
	/**
	 * Kontroluje spr·vnosù prihlasovacÌch d·t pred prihl·senÌm. True, ak s˙ d·ta v poriadku, inak false.
	 * @return boolean
	 */
	public boolean controlData() {
		return LOGIN != null && PASSWORD != null && SERVER != null && !LOGIN.isEmpty() && !PASSWORD.isEmpty() && !SERVER.isEmpty();
	}
	
	/**
	 * VytvorÌ kontainer, ktor˝ poskytuje Connection na server.
	 * @throws ContainerCreateException
	 */
	private void createIContainer() throws ContainerCreateException {
		if (container == null) {
			container = ContainerFactory.getDefault().createContainer(TYPE);
		}
	}
	
	/**
	 * Vr·ti identifik·tor pre server.
	 * @return ID myID
	 */
	protected ID getMyID() {
		return myID;
	}
	
	/**
	 * Vytiahne a nastavÌ prihlasovacie ˙daje.
	 * @param IPreferenceStore iPreferenceStore - referencia na ˙loûisko.
	 */
	public void setPreferences(IPreferenceStore iPreferenceStore) {
		SERVER = iPreferenceStore.getString(PreferencesConstants.XMPP_SERVER);
		LOGIN = iPreferenceStore.getString(PreferencesConstants.XMPP_LOGIN);
		PASSWORD = iPreferenceStore.getString(PreferencesConstants.XMPP_PASSWORD);
		GROUP = iPreferenceStore.getString(PreferencesConstants.XMPP_GROUP);
	}
	
	/**
	 * VytvorÌ identifik·tor pre server (myID).
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
	 * VytvorÌ identifik·tor pre server podæa mena
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
	 * Odh·senie zo servera.
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
	 * Kontrola stavu pre pripojenie. Ak, je vöetko v poriadku prihl·si sa. Vr·ti true, ak sa podarilo prihl·siù, inak false.
	 * @return boolean
	 */
	public boolean tryConnect() {
		resetUserData();
		Activator.getDefault().updatePreferencesStore();
		try {
			connect();
		} catch (ContainerCreateException | ContainerConnectException e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Prihl·senie nebolo ˙speönÈ.",
					"Pri prihl·senÌ nastala chyba, uistite sa, ûe m·te spr·vne nastavanÈ vstupnÈ parametre.");
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
	 * Ak je pouûÌvateæskÈ meno v tvare pre uËiteæa, tak nastavÌ hodnotu isAdmin na true, inak false.
	 */
	private void setAdmin() {
		Pattern pattern = Pattern.compile("^admin[0-9]@" + SERVER + "$"); 
		if (isLogin() && getMyID() != null) 
			isAdmin = pattern.matcher(getMyID().getName()).matches();
	}

	/**
	 * Vr·ti kontainer.
	 * @return Container container
	 */
	protected IContainer getContainer() {
		return container;
	}	
	
	/**
	 * Premaûe prihlasovacie ˙daje.
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
	 * NastavÌ hodnoty isAdmin a isLogin na false.
	 */
	private void resetIsLogin() {
		isAdmin = false;
		isLogin = false;
	}
	
	/**
	 * Vr·ti hodnotu isLogin.
	 * @return boolean isLogin
	 */
	public boolean isLogin() {
		return isLogin;
	}
	
	/**
	 * Vr·ti skupinu.
	 * @return String group
	 */
	public String getGroup() {
		return GROUP;
	}
	
	/**
	 * Zmaûe inötanciu.
	 */
	public void destroy() {
		instance = null;
	}	
}