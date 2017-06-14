package rtsharecode.common.sharing;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Scanner;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;

import rtsharecode.common.Activator;
import rtsharecode.common.communication.Connection;
import rtsharecode.common.communication.MessageFlags;
import rtsharecode.common.communication.Messenger;

/**
 * Trieda CommonSharing - slu�by zdie�ania spolo�n� pre u�ite�a aj �tudneta 
 * @author Martin Kellner
 *
 */
public class CommonSharing {

	private static CommonSharing instance = null;
	private String responseInitLogin = null;
	private boolean isShared = false;
	private SharedProject sharedProject;
	private boolean iInit = false;
	private String initUser = null;
	private boolean access = false;
	private ArrayList<Integer> filesForSend = new ArrayList<>();
	private ArrayList<Integer> caretPositionsForSend = new ArrayList<>();
	private String responseAdmin;
	private int countOfResponseIsSharing = 0;
	
	/**
	 * In�tancia triedy (singleton)
	 * @return referencia na in�tanciu
	 */
	public static CommonSharing getInstance() {
		if (instance == null) {
			instance = new CommonSharing();
 			if (Connection.getInstance().isAdmin()) {
 				instance.access = true;
 			}
		}
		return instance;
	}
	
	/**
	 * Nastavenie hodnoty responseAdmin
	 * @param boolean value
	 */
	synchronized public void setResponse(String value) {
		this.responseAdmin = value;
	}
	
	/**
	 * Vr�ti hodnotu responseAdmin
	 * @return boolean responceAdmin
	 */
	synchronized public String getResponse() {
		return this.responseAdmin;
	}
	
	/**
	 * Vr�ti hodnotu, �i sa zdie�a
	 * @return true, ak sa zdie�a, inak false.
	 */
	public boolean isShared () {
		return this.isShared;
	}
	
	/**
	 * Nastavenie hodnoty isShared
	 * @param boolean value - true, ak sa zdiela, inak false.
	 */
	protected void setSharing(boolean b) {
		this.isShared = b;		
	}
	
	/**
	 * Vr�tenie hodnoty iInit
	 * @return true, ak pou��vate� inicializoval zdie�anie, inak false.
	 */
	public boolean getIInit() {
		return iInit;
	}
	
	/**
	 * Z�skanie obsahu s�boru.
	 * @param IFile file - s�bor
	 * @return String content - obsah
	 * @throws CoreException
	 * @throws IOException
	 */
	synchronized protected String receiveContentFromFile(IFile file) throws CoreException, IOException {
		try {
			Activator.getDefault().getManagerListener().saveAll();
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
		try {
			this.wait(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		InputStream io = file.getContents();
		Scanner scanner = new Scanner(io);
		String content = "";
		while (scanner.hasNext()) {
			String line = scanner.nextLine();
			for (char ch : line.toCharArray()) {
				if (ch != '\u001D') {
					content += String.valueOf(ch);
				}
			}
			if (scanner.hasNext()) {
				content += String.valueOf('\n');
			}
		}
		io.close();
		scanner.close();
		return content;		
	}
	
	/**
	 * Nastav� hodnotu sharedProject
	 * @param SharedProjects sharedProject
	 */
	public void setSharedProject(SharedProject sharedProject) {
		this.sharedProject = sharedProject;
	}
	
	/**
	 * Vr�ti SharedProject sharedProject
	 * @return SharedProject sharedProject
	 */
	public SharedProject getSharedProject () {
		return this.sharedProject;
	}
	
	/**
	 * Vr�ti hodnotu access
	 * @return true, ak m� pr�stup zdie�at svoje zmeny obsahu, inak false.
	 */
	public boolean hasAccess() {
		return this.access;
	}
	
	/**
	 * Vytvorenie spr�vy pre odoslanie zmeny obsahu a polanie met�dy pre zaslanie.
	 */
	synchronized public void sendDocumentContent() {
		if (this.access) {
			while (!this.filesForSend.isEmpty()) {
				String content = null;
				IFile file = Activator.getDefault().getManagerListener().getFiles().get(filesForSend.remove(0));
				String caretPosition = String.valueOf(caretPositionsForSend.remove(0)); 
				try {
					content = this.receiveContentFromFile(file);
				} catch (CoreException | IOException e) {
					e.printStackTrace();
				}
				if (content != null) {
					Messenger.getInstance().sendMultiCastMessage(MessageFlags.ID + Connection.getInstance().getLoginName() + MessageFlags.SEPARATOR + MessageFlags.QUERY + 
						MessageFlags.FILECHANGE + file.getProjectRelativePath() + MessageFlags.SEPARATOR + caretPosition + "|" + content);
				}
			}
		}
	}

	/**
	 * Zachytenie zmeny obsahu s�boru.
	 * @param IFile file - s�bor 
	 * @param int point - miesto zmeny
	 */
	synchronized public void handleChangedContent(int file, int point) {
		if (this.access) {
			this.filesForSend.add(file);
			this.caretPositionsForSend.add(point);
			Messenger.getInstance().sendMessageToUser(Connection.getInstance().getLoginName(), MessageFlags.ID + Connection.getInstance().getLoginName() + MessageFlags.SEPARATOR + MessageFlags.QUERY + MessageFlags.NOTIFICATION);
		}
	}
	
	/**
	 * Nastav pr�stup k zdie�aniu, premenn� access
	 * @param boolean value
	 * @param String user - meno
	 */
	public void setValueAccess(boolean value, String user) {
		this.access = value;
		if (value) {
			Activator.getDefault().refreshListener();
		}		
		if (user != null) {
			sendAck(user);
		}		
	}
	
	/**
	 * Vytvor� spr�vu pre pozvanie a vol� met�du pre zaslanie
	 * @param name
	 * @return
	 */
	public boolean sendInvation(String name) {
		return Messenger.getInstance().sendMessageToUser(name, MessageFlags.ID + Connection.getInstance().getLoginName() + MessageFlags.SEPARATOR + MessageFlags.QUERY + MessageFlags.INVATE_QUERY);
	}
	
	/**
	 * Vytvor� spr�vu pre synchoniz�ciu a vol� met�du pre zaslanie
	 * @param adminName
	 * @return
	 */
	public boolean sendSync(String adminName) {
		return Messenger.getInstance().sendMessageToUser(adminName,  MessageFlags.ID + Connection.getInstance().getLoginName() + MessageFlags.SEPARATOR + MessageFlags.QUERY + MessageFlags.SYNC);
	}
	
	/**
	 * Vytvor� spr�vu pre udelenie pr�stupu a vol� met�du pre zaslanie
	 * @param adminName
	 * @return
	 */
	public boolean sendAccess(String adminName) {
		return Messenger.getInstance().sendMessageToUser(adminName,  MessageFlags.ID + Connection.getInstance().getLoginName() + MessageFlags.SEPARATOR + MessageFlags.QUERY + MessageFlags.ACCESS);
	}
	
	/**
	 * Zmazanie in�tancie triedy.
	 */
	public void destroy() {
		instance = null;
	}
	
	/**
	 * Zv��� po�et potvrden� o akt�vnom zdie�an�
	 */
	public void increaseVariableActiveSharingListener() {
		if (countOfResponseIsSharing > 30000) {
			countOfResponseIsSharing = 0;
		}
		countOfResponseIsSharing ++;
	}
	
	/**
	 * Vr�ti po�et potvrden� o akt�vnom zdie�an�.
	 * @return int countOfResponseIsSharing
	 */
	public int getCountOfResponseIsSharing() {
		return countOfResponseIsSharing;
	}
	
	/**
	 * Vytvor� spr�vu pre zaslanie potvrdenia a vol� met�du na zaslanie.
	 * @param login - meno
	 */
	public void sendAck(String login) {
		Messenger.getInstance().sendMessageToUser(login, MessageFlags.ID + Connection.getInstance().getLoginName() + MessageFlags.SEPARATOR + MessageFlags.RESPONSE + MessageFlags.ACK);
	}	
}