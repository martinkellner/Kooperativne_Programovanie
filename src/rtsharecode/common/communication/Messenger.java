package rtsharecode.common.communication;

import org.eclipse.ecf.core.IContainer;
import org.eclipse.ecf.core.util.ECFException;
import org.eclipse.ecf.presence.IIMMessageEvent;
import org.eclipse.ecf.presence.IIMMessageListener;
import org.eclipse.ecf.presence.IPresenceContainerAdapter;
import org.eclipse.ecf.presence.im.ChatMessageEvent;
import org.eclipse.ecf.presence.im.IChatManager;
import org.eclipse.ecf.presence.im.IChatMessageSender;
import org.eclipse.jface.dialogs.MessageDialog;

/**
 * Trieda Messenger - odosielanie a prij�manie spr�v
 * @author Martin Kellner
 *
 */
public class Messenger {
	
	private static Messenger instance = null;
	private Thread threadActiveSharingSender;
	private ActiveSharingSender activeSharingSender;
	private Thread threadActiveSharingListener;
	private ActiveSharingListener activeSharingListener;
	private boolean interuptListener;
	private boolean interuptSender;
	private IChatManager chatManager;
	private IChatMessageSender sender;
	
	/**
	 * Singleton
	 * @return vr�ti referenciu na in�tancie triedy
	 */
	public static Messenger getInstance() {
		if (instance == null) {
			instance = new Messenger();
		}			
		return instance;
	}
	
	/**
	 * Inicializuje funckionality pre zachyt�vanie a odosielanie spr�v.
	 */
	public void initializeMessenger() {
		if (sender == null && chatManager == null) {
			IContainer container = Connection.getInstance().getContainer();
			chatManager = ((IPresenceContainerAdapter) container.getAdapter(IPresenceContainerAdapter.class)).getChatManager();
			sender = chatManager.getChatMessageSender();
			chatManager.addMessageListener(new IIMMessageListener() {
				
				@Override
				synchronized public void handleMessageEvent(IIMMessageEvent messageEvent) {
					System.out.println("prislo - " + ((ChatMessageEvent) messageEvent).getChatMessage().getBody());
					MessageProcessor.getInstance().parseMessage(((ChatMessageEvent) messageEvent).getChatMessage().getBody());
		    	}
			});
		}
	}
	
	/**
	 * Posiela spr�vu do skupiny, ale iba on-line pou��vate�om.
	 * @param String message - obsah spr�vy
	 * @return boolean - true, ak sa podar� odosla�, inak false.
	 */
	public synchronized boolean sendMultiCastMessage(String message) {
		if (Connection.getInstance().isLogin() && message != null && !message.isEmpty()) {
			try {
				sender.sendChatMessage(Connection.getInstance().createMultiCastID(), message);
				System.out.println("broadcast - " + message);
				return true;
			} catch (ECFException e) {
				// pass
			}
		}
		return false;
	}
	
	/**
	 * Posiela spr�vu pou��vate�ovi, ale iba on-line pou��vate�ovi.
	 * @param login - meno pou��vatela
	 * @param message - obsah spr�vy
	 * @return boolean - true, ak sa podar� odosta�, inak false.
	 */
	public synchronized boolean sendMessageToUser(String login, String message) {
		if (Connection.getInstance().isLogin() && login != null && !login.isEmpty() && message != null && !message.isEmpty()) {
			try {
				sender.sendChatMessage(Connection.getInstance().createTargetID(login) , message);
				System.out.println("unicast - "  + message);
				return true;
			} catch (
					ECFException e) {
				MessageDialog.openError(rtsharecode.common.Activator.getDefault().getShell() , "Neodoslan� spr�va ", "Spr�va nebola odoslan�, skontrolujte SKUPINU v nastaveniach");
			}
		}
		return false;
	}
	
	/**
	 * Preru�enie �innosti ActiveSharingSender
	 */
	protected void terminateActiveSharingSender() {
		this.activeSharingSender = null;
		this.threadActiveSharingSender = null;
	}	
	
	/**
	 * Spustenie vl�ka ActiveSharingSender
	 */
	public void startActiveSharingSender() {
		this.interuptSender = false;
		this.activeSharingSender = new ActiveSharingSender();
		this.threadActiveSharingSender = new Thread(this.activeSharingSender);
		this.threadActiveSharingSender.start();
	}

	/**
	 * Spustenie vl�kna ActiveSharingListener
	 */
	public void startActiveSharingListener() {
		this.interuptListener = false;
		this.activeSharingListener = new ActiveSharingListener();
		this.threadActiveSharingListener = new Thread(this.activeSharingListener);
		this.threadActiveSharingListener.start();
	}
	
	/**
	 * Preru�enie �innosti ActiveSharingListener
	 */
	protected void terminateActiveSharingListener() {
		this.activeSharingListener = null;
		this.threadActiveSharingListener = null;
	}
	
	/**
	 * Zist�, �i je vl�kno akt�vne.
	 * @return boolean - true, ak je akt�vne, inak false.
	 */
	public boolean isActiveSharingListenerAlive() {
		return (this.threadActiveSharingListener == null ? false : (this.threadActiveSharingListener.isAlive() ? true : false));
	}
	
	/**
	 * 
	 * @return boolean - true, ak je ActiveSharingSender neakt�vny, inak false.
	 */
	protected boolean getInteruptSender() {
		return this.interuptSender;
	}
	
	/**
	 * 
	 * @return @return boolean - true, ak je ActiveSharingListener neakt�vny, inak false.
	 */
	protected boolean getInteruptListner() {
		return this.interuptListener;
	}
	
	/**
	 * Nastav� hodnoti interuptListener na true
	 */
	public void removeListener() {
		this.interuptListener = true;
	}
	
	/**
	 * Nastav� hodnotu interuptSender na true
	 */
	public void removeSender() {
		this.interuptSender = true;
	}
	
	/**
	 * Nastav� in�tanciu (singleton) na null
	 */
	public void destroy() {
		this.sender = null;
		this.chatManager = null;
	}
}