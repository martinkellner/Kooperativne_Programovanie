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
 * Trieda CommonSharing - služby zdie¾ania spoloèné pre uèite¾a aj študneta 
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
	 * Inštancia triedy (singleton)
	 * @return referencia na inštanciu
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
	 * Vráti hodnotu responseAdmin
	 * @return boolean responceAdmin
	 */
	synchronized public String getResponse() {
		return this.responseAdmin;
	}
	
	/**
	 * Vráti hodnotu, èi sa zdie¾a
	 * @return true, ak sa zdie¾a, inak false.
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
	 * Vrátenie hodnoty iInit
	 * @return true, ak používate¾ inicializoval zdie¾anie, inak false.
	 */
	public boolean getIInit() {
		return iInit;
	}
	
	/**
	 * Získanie obsahu súboru.
	 * @param IFile file - súbor
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
	 * Nastaví hodnotu sharedProject
	 * @param SharedProjects sharedProject
	 */
	public void setSharedProject(SharedProject sharedProject) {
		this.sharedProject = sharedProject;
	}
	
	/**
	 * Vráti SharedProject sharedProject
	 * @return SharedProject sharedProject
	 */
	public SharedProject getSharedProject () {
		return this.sharedProject;
	}
	
	/**
	 * Vráti hodnotu access
	 * @return true, ak má prístup zdie¾at svoje zmeny obsahu, inak false.
	 */
	public boolean hasAccess() {
		return this.access;
	}
	
	/**
	 * Vytvorenie správy pre odoslanie zmeny obsahu a polanie metódy pre zaslanie.
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
	 * Zachytenie zmeny obsahu súboru.
	 * @param IFile file - súbor 
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
	 * Nastav prístup k zdie¾aniu, premenná access
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
	 * Vytvorí správu pre pozvanie a volá metódu pre zaslanie
	 * @param name
	 * @return
	 */
	public boolean sendInvation(String name) {
		return Messenger.getInstance().sendMessageToUser(name, MessageFlags.ID + Connection.getInstance().getLoginName() + MessageFlags.SEPARATOR + MessageFlags.QUERY + MessageFlags.INVATE_QUERY);
	}
	
	/**
	 * Vytvorí správu pre synchonizáciu a volá metódu pre zaslanie
	 * @param adminName
	 * @return
	 */
	public boolean sendSync(String adminName) {
		return Messenger.getInstance().sendMessageToUser(adminName,  MessageFlags.ID + Connection.getInstance().getLoginName() + MessageFlags.SEPARATOR + MessageFlags.QUERY + MessageFlags.SYNC);
	}
	
	/**
	 * Vytvorí správu pre udelenie prístupu a volá metódu pre zaslanie
	 * @param adminName
	 * @return
	 */
	public boolean sendAccess(String adminName) {
		return Messenger.getInstance().sendMessageToUser(adminName,  MessageFlags.ID + Connection.getInstance().getLoginName() + MessageFlags.SEPARATOR + MessageFlags.QUERY + MessageFlags.ACCESS);
	}
	
	/**
	 * Zmazanie inštancie triedy.
	 */
	public void destroy() {
		instance = null;
	}
	
	/**
	 * Zvýší poèet potvrdení o aktívnom zdie¾aní
	 */
	public void increaseVariableActiveSharingListener() {
		if (countOfResponseIsSharing > 30000) {
			countOfResponseIsSharing = 0;
		}
		countOfResponseIsSharing ++;
	}
	
	/**
	 * Vráti poèet potvrdení o aktívnom zdie¾aní.
	 * @return int countOfResponseIsSharing
	 */
	public int getCountOfResponseIsSharing() {
		return countOfResponseIsSharing;
	}
	
	/**
	 * Vytvorí správu pre zaslanie potvrdenia a volá metódu na zaslanie.
	 * @param login - meno
	 */
	public void sendAck(String login) {
		Messenger.getInstance().sendMessageToUser(login, MessageFlags.ID + Connection.getInstance().getLoginName() + MessageFlags.SEPARATOR + MessageFlags.RESPONSE + MessageFlags.ACK);
	}	
}