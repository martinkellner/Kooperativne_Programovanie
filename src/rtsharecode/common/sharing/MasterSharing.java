package rtsharecode.common.sharing;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;

import rtsharecode.common.Activator;
import rtsharecode.common.communication.Connection;
import rtsharecode.common.communication.MessageFlags;
import rtsharecode.common.communication.Messenger;
import rtsharecode.common.communication.ShareProjectMessage;

/**
 * Trieda MasterSharing - sluûby zdieæania pre uËiteæa.
 * @author Martin Kellner
 *
 */
public class MasterSharing {
	
	private static MasterSharing instance;
	public String syncWithUser = "";
	private boolean result = false;
	private boolean error = false;
	
	/**
	 * Vr·ti inötanciu triedy (singleton)
	 * @return
	 */
	public static MasterSharing getInstance() {
		if (instance == null) {
			instance = new MasterSharing();
		}
		return instance;
	}
	
	/**
	 * Odobranie prstupu k zdieæaniu 
	 * @param user - meno
	 */
	public void removeAccess(String user) {
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(Activator.getDefault().getShell());
		try {
			dialog.run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Odoberanie prÌstupu " + user, 100);
					monitor.worked(80);
					Messenger.getInstance().sendMessageToUser(user, MessageFlags.ID + Connection.getInstance().getLoginName() + MessageFlags.SEPARATOR + MessageFlags.QUERY + MessageFlags.ACCESS);
					monitor.worked(100);
					TimeUnit.MILLISECONDS.sleep(1500);
					monitor.done();
					CommonSharing.getInstance().setValueAccess(true, null);
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
		Messenger.getInstance().sendMultiCastMessage(MessageFlags.ID + Connection.getInstance().getLoginName() + MessageFlags.SEPARATOR + MessageFlags.RESPONSE + MessageFlags.ACCESS);
		Activator.getDefault().getView().addUser(null, null, null, null, false);
	}
	
	/**
	 * Udelovanie prÌstupu
	 * @param user - meno
	 * @return true, ak sa podarilo udeliù prÌstup, inak false.
	 */
	public boolean controlAccess(String user) {
		boolean result = syncUser(user);
		if (result) { 
			ProgressMonitorDialog dialog = new ProgressMonitorDialog(Activator.getDefault().getShell());
			try {
				dialog.run(true, false, new IRunnableWithProgress() {
					
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						monitor.beginTask("Odovzd·vanie prÌstupu " + user, 100);
						monitor.worked(80);
						Messenger.getInstance().sendMessageToUser(user, MessageFlags.ID + Connection.getInstance().getLoginName() + MessageFlags.SEPARATOR + MessageFlags.QUERY + MessageFlags.ACCESS);
						monitor.worked(100);
						TimeUnit.MILLISECONDS.sleep(1500);
						monitor.done();
						CommonSharing.getInstance().setValueAccess(false, null);
					}
				});
			} catch (InvocationTargetException | InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			Activator.getDefault().getView().addUser(user, "sync",	"remove", false, true);
			MessageDialog.openError(Activator.getDefault().getShell(), "Odovzd·vanie prÌstupu ne˙speönÈ", "Odovzd·vanie prÌstupu bolo neuspeönÈ, pouûÌvateæ neodpoved·.");
		}
		return result;
	}
	
	/**
	 * Pozvanie pouûÌvateæa do zdieæania.
	 * @param userName - meno
	 */
	public void invate(String userName) {
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(Activator.getDefault().getShell());
		try {
			dialog.run(true, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Poz˝va sa pouûÌvateæ " + userName, 100);
					ShareProjectMessage message = new ShareProjectMessage(Activator.getDefault().getProject());
					monitor.worked(80);
					TimeUnit.SECONDS.sleep(1);
					sendSharedProject(message.toString(), userName);
					monitor.worked(100);
					TimeUnit.MILLISECONDS.sleep(500);
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Vytvorenie spr·vy pre zdieæanie ötrukt˙ryp projektu a odoslanie.
	 * @param text - obsah spr·vy
	 * @param login - meno
	 */
	private void sendSharedProject (String text, String login) {
		if (Connection.getInstance().isLogin() && Connection.getInstance().isAdmin()) {
			if (login != null) {
				Messenger.getInstance().sendMessageToUser(login, MessageFlags.ID + Connection.getInstance().getLoginName() + "#" + MessageFlags.QUERY + MessageFlags.SHARE + text);
			} else {
				Messenger.getInstance().sendMultiCastMessage(MessageFlags.ID + Connection.getInstance().getLoginName() + "#" + MessageFlags.QUERY + MessageFlags.SHARE + text);
			}			
		}
	}
	
	/**
	 * Inicializ·cia zdieæania v skupine.
	 */
	public void initSharing() {
		error = false;
		ShareProjectMessage message = new ShareProjectMessage(Activator.getDefault().getProject());
		String m = message.toString();
		if (m == null) {
			MessageDialog.openError(Activator.getDefault().getShell(), "Nestr·vna ötrukt˙ra pre zdieæanie projektu.", "Projekt nie je moûnÈ zdieæaù, pravdepodobne obsahuje (default package). Premenujte tento package a sk˙ste op‰ù.");
		} else {
			ProgressMonitorDialog progressMonitorDialog = new ProgressMonitorDialog(Activator.getDefault().getShell());
			try {
				progressMonitorDialog.run(true, true, new IRunnableWithProgress() {
				
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						monitor.beginTask(" Inizializ·cia zdieæania v skupine ", 100);
						monitor.setCanceled(true);
						ShareProjectMessage message = new ShareProjectMessage(Activator.getDefault().getProject());
						if (message != null) {
							sendSharedProject(message.toString(), null);
							monitor.worked(100);
							CommonSharing.getInstance().setSharing(true);
							TimeUnit.SECONDS.sleep(1);
							monitor.done();
						} else {
							error = true;
						}					
					}
				});
			} catch (InvocationTargetException | InterruptedException e) {
				e.printStackTrace();
			}
			Messenger.getInstance().startActiveSharingSender();
			Activator.getDefault().getView().setStopActionButtonEnabled(true);
		}
	}
	
	/**
	 * Synchroniz·cia s pouûÌvateæom
	 * @param user - meno 
	 * @return true, ak sa podarilo synchonizovaù, inak true.
	 */
	public boolean syncUser(String user) {
		if (Connection.getInstance().isAdmin() && Connection.getInstance().isLogin()) {
			ShareProjectMessage message = new ShareProjectMessage(Activator.getDefault().getProject());
			String m = message.toString();
			if (m == null) {
				MessageDialog.openError(Activator.getDefault().getShell(), "Nespr·vna ötrukt˙ra projektu.", "Projekt nie je moûnÈ zdieæaù, pravdepodobne obsahuje (default package). Premenujte tento package a sk˙ste op‰ù.");
				return false;
			} else {
				ProgressMonitorDialog progressMonitorDialog = new ProgressMonitorDialog(Activator.getDefault().getShell());
				try {
					progressMonitorDialog.run(true, false, new IRunnableWithProgress() {
						
						@Override
						public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
							result = false;
							monitor.beginTask("Synchoniz·cia s " +  user, 100);
							ShareProjectMessage message = new ShareProjectMessage(Activator.getDefault().getProject());
							monitor.worked(5);
							syncWithUser = "";
							if (Messenger.getInstance().sendMessageToUser(user, MessageFlags.ID + Connection.getInstance().getLoginName() + "#" + MessageFlags.RESPONSE + MessageFlags.SYNC + message.toString())) {
								TimeUnit.SECONDS.sleep(4);
								if (syncWithUser.equals(user)) {
									monitor.worked(15);
									if (Activator.getDefault().getManagerListener() == null) {
										Activator.getDefault().initializeListenersForCurrentProject();
									}
									TimeUnit.SECONDS.sleep(1);
									for (IFile file : Activator.getDefault().getManagerListener().getFiles()) {
										String content = "";
										try {
											 content = CommonSharing.getInstance().receiveContentFromFile(file);
										} catch (CoreException | IOException e) {
											monitor.done();
										}									
										Messenger.getInstance().sendMessageToUser(user, MessageFlags.ID + Connection.getInstance().getLoginName() + MessageFlags.SEPARATOR + MessageFlags.RESPONSE + MessageFlags.FILESYNC + file.getProjectRelativePath() + "#" + content);
									}
									
									monitor.worked(90);
									TimeUnit.SECONDS.sleep(3);
									monitor.worked(100);				
									monitor.done();
									Messenger.getInstance().sendMessageToUser(user , MessageFlags.ID + Connection.getInstance().getLoginName() + MessageFlags.SEPARATOR + MessageFlags.RESPONSE + MessageFlags.ACK);
									result = true;
									Activator.getDefault().getView().addUser(syncWithUser, "sync", "add", false , false);
									syncWithUser = "";
								} else {
									syncWithUser = "";
									result = false;
									monitor.done();
								}
							} else {
								Activator.getDefault().getView().addUser(syncWithUser, "wait", "remove", false , false);
								syncWithUser = "";
								result = false;
								monitor.done();
							}
						}
					});
							
				} catch (InvocationTargetException | InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}
	
	/**
	 * Synchroniz·cia po zmene ötrukt˙ry.
	 * @throws CoreException
	 * @throws IOException
	 */
	public void syncStructure() throws CoreException, IOException {
		ShareProjectMessage message = new ShareProjectMessage(Activator.getDefault().getProject());
		String m = message.toString();
		if (m == null) {
			MessageDialog.openError(Activator.getDefault().getShell(), "Nestr·vna ötrukt˙ra pre zdieæanie projektu.", "Projekt nie je moûnÈ zdieæaù, pravdepodobne obsahuje (default package). Premenujte tento package a sk˙ste op‰ù.");
			
		} else {
			ProgressMonitorDialog progressMonitorDialog = new ProgressMonitorDialog(Activator.getDefault().getShell());
			try {
				progressMonitorDialog.run(true, true, new IRunnableWithProgress() {
					
					@Override
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
						monitor.beginTask("Synchroniz·cia po zmene strukt˙ry projektu.", 100);
						Messenger.getInstance().sendMultiCastMessage(MessageFlags.ID + Connection.getInstance().getLoginName() + MessageFlags.SEPARATOR + MessageFlags.RESPONSE + MessageFlags.SYNC + new ShareProjectMessage(Activator.getDefault().getProject()).toString());  
						monitor.worked(55);
						try {
							TimeUnit.SECONDS.sleep(4);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}					
						for (IFile file : Activator.getDefault().getManagerListener().getFiles()) {
							if (file.exists()) {
								String content = null;
								try {
									content = CommonSharing.getInstance().receiveContentFromFile(file);
								} catch (CoreException | IOException e) {
									e.printStackTrace();
								}
								Messenger.getInstance().sendMultiCastMessage(MessageFlags.ID + Connection.getInstance().getLoginName() + MessageFlags.SEPARATOR + MessageFlags.QUERY + MessageFlags.FILESYNC + file.getProjectRelativePath() + "#" + (content == null ? "" : content));
								try {
									TimeUnit.MILLISECONDS.sleep(500);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
						}
						monitor.worked(90);
						monitor.worked(100);
						monitor.done();
					}
				});
			} catch (InvocationTargetException | InterruptedException e) {
					e.printStackTrace();
			}
		}
	}

	/**
	 * Synchroniz·cia viacer˝ch pouûÌvateæov.
	 * @param ArrayList<String> names 
	 */
	public void syncUsers(ArrayList<String> names) {
		if (Connection.getInstance().isAdmin() && Connection.getInstance().isLogin()) {
			for (String name : names) {
				if (!syncUser(name)){
					Activator.getDefault().getView().addUser(name, "wait", "remove", false , false);
				}
			}
		}
	}

	/**
	 * Vytvorenie spr·vy s inform·ciou o aktÌvnom zdieæanÌ a odoslanie.
	 */
	synchronized public void sendActiveSharingStatus() {
		if (Connection.getInstance().isAdmin() && CommonSharing.getInstance().isShared()) {
			Messenger.getInstance().sendMultiCastMessage(MessageFlags.ID + Connection.getInstance().getLoginName() + MessageFlags.SEPARATOR + MessageFlags.QUERY + MessageFlags.ISSHARE);
		} else if (Connection.getInstance().isAdmin() && !CommonSharing.getInstance().isShared()) {
			Messenger.getInstance().removeSender();
		}
	}
	
	/**
	 * Zmazanie inötancie.
	 */
	public void destroy() {
		instance = null;	
	}
}