package rtsharecode.common.communication;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import rtsharecode.common.Activator;
import rtsharecode.common.sharing.CommonSharing;
import rtsharecode.common.sharing.MasterSharing;
import rtsharecode.common.sharing.SharedProject;
/**
 * Trieda MessageProcessor - verifikácia a spracovanie správ.
 * @author a641813
 *
 */
public class MessageProcessor {
	
	private static MessageProcessor instance = null;
	
	/**
	 * Inštancia triedy, navrhový vzor singleton.
	 * @return
	 */
	public static MessageProcessor getInstance() {
		if (instance == null)
			instance = new MessageProcessor();
		return instance;
	}
	
	/**
	 * Základné spracovanie správ.
	 * @param String message - text prijatých správ.
	 */
	synchronized protected void parseMessage(String message) {
		message = trimStartOfMassage(message);
		if (message == null) {
			return;
		}
		if (message != null && !message.isEmpty()) {
			int length = message.length();
			if (message.startsWith(MessageFlags.ID)) {
				String login = "";
				int i = 2;
				char ch;
				while (i < message.length() && (ch = message.charAt(i)) != '#') {
					login += String.valueOf(ch);
					i ++;
				}
				if (i + 3 < length) {
					if (message.equals(MessageFlags.ID + Connection.getInstance().getLoginName() + MessageFlags.SEPARATOR + MessageFlags.QUERY + MessageFlags.NOTIFICATION)) {
						CommonSharing.getInstance().sendDocumentContent();
					}
					else if (login.equals(Connection.getInstance().getLoginName())) {
						return;
					}
					i ++;
					String subMessage = message.substring(i);
					if (subMessage.startsWith(MessageFlags.RESPONSE)) { 
						processResponseMessage(login, message.substring(i + 2));
					} else if (subMessage.startsWith(MessageFlags.QUERY)) {
						processQueryMessage(login, message.substring(i + 2));
					}
				}	
			}
		}
	}	
	
	/**
	 * Spracovanie správ typu Požiadavka
	 * @param String login - používate¾, ktorý poslal správu.
	 * @param String message - text správy. 
	 */
	synchronized private void processQueryMessage(String login, String message) {
		if (!message.isEmpty()) {
			if (message.startsWith(MessageFlags.SHARE + "pname=")) {
				SharedProject sharedProject = new SharedProject(message.substring(2));
				sharedProject.createProject(login);
				if (!sharedProject.getPROCESS_STATUS()) {
					Display.getDefault().asyncExec(new Runnable() {
						
						@Override
						public void run() {
							MessageDialog.openError(Activator.getDefault().getShell() , "Chyba pri vytvaraní zdie¾aného projektu", "Pri vytváraní zdie¾aného projektu"
								+ " nastala chyba. Zdie¾anie nie je aktuálne možno. Požiadajte o opätovnú synchonizáciu projektu.");
						}
					});
				}			
			} else if (message.startsWith(MessageFlags.SYNC)) {
				Activator.getDefault().getView().addUser(login, "wait", "add", true, false);
				Activator.getDefault().getView().draw();
			} else if (message.startsWith(MessageFlags.FILECHANGE)) {
				Activator.getDefault().getManagerListener().updateContentOfEditor(message.substring(2));
			} else if (message.startsWith(MessageFlags.NEWFILECONTENT)) {
				Activator.getDefault().getManagerListener().updateContentOfEditor(message.substring(2));
			} else if (message.startsWith(MessageFlags.FILESYNC)) { 
				 try {
					CommonSharing.getInstance().getSharedProject().actualizeFileContent(message.substring(2));
				} catch (CoreException e) {
					e.printStackTrace();
				}
			} else if (message.startsWith(MessageFlags.ISSHARE)) {
				if (Connection.getInstance().isAdmin() && CommonSharing.getInstance().isShared()) {
					Messenger.getInstance().sendMessageToUser(login, MessageFlags.ID + Connection.getInstance().getLoginName() + MessageFlags.SEPARATOR + MessageFlags.RESPONSE + MessageFlags.ISSHARE);
				} else if (CommonSharing.getInstance().getSharedProject() == null) {
					if (!Connection.getInstance().isAdmin()) {
						Activator.getDefault().getView().addAdminItem(login, false, true);
					}
				}
				if (!Connection.getInstance().isAdmin()) {
					CommonSharing.getInstance().increaseVariableActiveSharingListener();
					if (!Messenger.getInstance().isActiveSharingListenerAlive()) {
						Messenger.getInstance().startActiveSharingListener();
					}
				}
			} else if (message.startsWith(MessageFlags.INVATE_QUERY)) {
				if (Connection.getInstance().isAdmin()) {
					Activator.getDefault().getView().addUser(login, "wait", "add", false, false);
				} 
			} else if (message.startsWith(MessageFlags.ACCESS)) {
				if (Connection.getInstance().isAdmin()) {
					Activator.getDefault().getView().addUser(login, null, null, null, true);
				} else {
					if (CommonSharing.getInstance().hasAccess()) {
						CommonSharing.getInstance().setValueAccess(false, null);
					} else {
						CommonSharing.getInstance().setValueAccess(true, login);
					}
				}
			}
		}
	}	
	
	/**
	 * Spracovanie správ typu Odpoveï
	 * @param String login - používate¾, ktorý poslal správu.
	 * @param String message - text správy. 
	 */
	synchronized private void processResponseMessage(String login, String message) {
		if (!message.isEmpty()) {
			if (message.startsWith(MessageFlags.ACK)) {
				if (Connection.getInstance().isAdmin()){
					MasterSharing.getInstance().syncWithUser = login;
					//Activator.getDefault().getView().editUser(login, "sync", "add", false , false);
				}			
			} else if (message.startsWith(MessageFlags.SYNC + "pname=")) {
				if (!CommonSharing.getInstance().getIInit()) {
					SharedProject sharedProject = CommonSharing.getInstance().getSharedProject();
					if (sharedProject != null) {
						try {
							if (Activator.getDefault().getManagerListener() != null) {
								Activator.getDefault().getManagerListener().saveAll();
							}
						} catch (InvocationTargetException  | InterruptedException e) {
							e.printStackTrace();
						}
						try {
							TimeUnit.SECONDS.sleep(1);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						sharedProject.actualizeProject(message.substring(2));
						if (sharedProject.getPROCESS_STATUS()) {
							Messenger.getInstance().sendMessageToUser(login, MessageFlags.ID + Connection.getInstance().getLoginName() + MessageFlags.SEPARATOR + MessageFlags.RESPONSE + MessageFlags.ACK);
							Activator.getDefault().getView().addAdminItem(login, true, false);
						}
					}
				} else {
					if (Activator.getDefault().getProject() == null) {
						Activator.getDefault().setProject(CommonSharing.getInstance().getSharedProject().getProject());
						Activator.getDefault().initializeListenersForCurrentProject();
					} else {
						Activator.getDefault().refreshListener();
					}
				}
			} else if (message.startsWith(MessageFlags.FILESYNC)) { 
				try {
					CommonSharing.getInstance().getSharedProject().actualizeFileContent(message.substring(2));
				} catch (CoreException e) {
					e.printStackTrace();
				}
			} else if (message.startsWith(MessageFlags.ISSHARE)) {
				if (!Connection.getInstance().isAdmin()) {
					CommonSharing.getInstance().setResponse(login);
				}
			} else if (message.startsWith(MessageFlags.ACCESS)){
				if ( !Connection.getInstance().isAdmin() ) {
					Activator.getDefault().getView().modifyAdmin();				
				}
			} else if (message.startsWith(MessageFlags.STOP_SHARE)) {
				if (!Connection.getInstance().isAdmin()) {
					Activator.getDefault().destroy();
				}
			}
		}
	}
	
	synchronized private String trimStartOfMassage(String message) {
		for (int i = 0; i < message.length(); i++) {
			if (message.charAt(i) != 'i') {
				continue;
			}
			else { 
				return message.substring(i);
			}
		}
		return null;
	}	
}