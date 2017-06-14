package rtsharecode.common.gui.view;

import java.util.ArrayList;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.part.ViewPart;

import rtsharecode.common.Activator;
import rtsharecode.common.communication.Connection;
import rtsharecode.common.communication.MessageFlags;
import rtsharecode.common.communication.Messenger;
import rtsharecode.common.sharing.MasterSharing;
import rtsharecode.common.sharing.CommonSharing;
/**
 * Trieda CustomView - grafick· plocha (View)
 * @author Martin Kellner
 *
 */
public class CustomView extends ViewPart {
	
	private Action loginAction;
	private Action logoutAction;
	private Action stopAction;
	private Shell shell;
	private Composite parent;
	private ScrolledComposite scrolledComposite;
	private Composite scomposite;
	private ArrayList<SyncUserItem> syncUserItems = new ArrayList<>();
	private ArrayList<WaitUserItem> waitUserItems = new ArrayList<>();
	private ActiveAdminLabel adminLabel;
	
	/**
	 * Vr·ti inötanciu triedy ActiveAdminLabel
	 * @return ActiveAdminLabel adminLabel
	 */
	public ActiveAdminLabel getAdminLabel() {
		return adminLabel;
	}

	/**
	 * Konötruktor
	 */
	public CustomView() {
		super();
	}
	
	/**
	 * Zmaûe obsah grafickej plochy 
	 */
	public void clear() {
		if (this.waitUserItems != null) {
			this.waitUserItems.clear();
		}
		if (this.syncUserItems != null) {
			this.syncUserItems.clear();
		}
		this.adminLabel = null;
		draw();
	}
	
	@Override
	/**
	 * Vytvorenie grafickej plochy
	 */
	public void createPartControl(Composite composite) {
		Activator.getDefault().setView(this);
		this.shell = composite.getShell();
		this.parent = composite;
		createLoginAction();
		createLogoutAction();
		createStopAction();
		getViewSite().getActionBars().getToolBarManager().add(this.loginAction);
		getViewSite().getActionBars().getToolBarManager().add(this.logoutAction);
		getViewSite().getActionBars().getToolBarManager().add(this.stopAction);
		createContent();
		setScroll();
	}
	/**
	 * NastavÌ tlaËidlo na stopnutie zdieæania
	 * @param boolean enabled
	 */
	public void setStopActionButtonEnabled(boolean enabled) {
		this.stopAction.setEnabled(enabled);
	}
	
	/**
	 * VytvorÌ plochu.
	 */
	private void createContent() {
		scrolledComposite = new ScrolledComposite(parent, SWT.V_SCROLL);
	    scomposite = new Composite(scrolledComposite, SWT.NONE);
	    scrolledComposite.setContent(scomposite);
	    scomposite.setBackground(scomposite.getDisplay().getSystemColor(SWT.COLOR_WHITE));
	    GridLayout data = new GridLayout();
	    data.numColumns = 3;
	    scomposite.setLayout(data);
	}
	
	/**
	 * VytvorÌ skrolovaciu plochu.
	 */
	private void setScroll() {
		scrolledComposite.setExpandHorizontal(true);
	    scrolledComposite.setExpandVertical(true);
	    scrolledComposite.setMinSize(parent.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}
	
	/**
	 * VytovrÌ tlaËidlo pre prihl·senie.
	 */
	private void createLoginAction() {
		this.loginAction = new Action() {
			public void run() {
				Activator.getDefault().updatePreferencesStore();
				if (Connection.getInstance().controlData()) {
					if (Connection.getInstance().tryConnect()) {
						logoutAction.setEnabled(true);
						loginAction.setEnabled(false);
						Activator.getDefault().initializeMessenger();
					}							
				} else {
					
					boolean wantSet = MessageDialog.openQuestion(shell , "Nastavenia pre pripojenie", "⁄daje potrebnÈ pre prihl·senie nie s˙ nastavenÈ."
							+ " Chcete prihlasovacie ˙daje nastaviù?");
					if (wantSet) {
						PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(shell , "ShareCode.Plugin.Preferences.page1", null, null);
						if (dialog != null) 
							dialog.open();
					}						
				}
			}								
		};
		this.loginAction.setEnabled(true);
		this.loginAction.setImageDescriptor(loginAction.getImageDescriptor().createFromImageData(
				new ImageData(CustomView.class.getResourceAsStream("images/login.png"))));
		this.loginAction.setText(" Prihl·senie ");
	}
	
	/**
	 * VytovrÌ tlaËidlo pre odhl·senie.
	 */
	private void createLogoutAction() {
		this.logoutAction = new Action() {
			public void run() {
					
				if (stopAction.isEnabled()) {
					stopAction.setEnabled(false);
				}
				loginAction.setEnabled(true);
				logoutAction.setEnabled(false);
				if (Connection.getInstance().isAdmin() && CommonSharing.getInstance().isShared()) {
					Messenger.getInstance().sendMultiCastMessage(MessageFlags.ID + Connection.getInstance().getLoginName() + MessageFlags.SEPARATOR + MessageFlags.RESPONSE + MessageFlags.STOP_SHARE);	
				}				
				Activator.getDefault().destroy();
				if (Connection.getInstance().isLogin()) {
					Messenger.getInstance().destroy();
					Connection.getInstance().disconnect();
					Connection.getInstance().destroy();
				}
			}			
		};
		this.logoutAction.setEnabled(false);
		this.logoutAction.setImageDescriptor(logoutAction.getImageDescriptor().createFromImageData(
				new ImageData(CustomView.class.getResourceAsStream("images/logout.png"))));
		this.logoutAction.setText(" Odhl·senie ");
	}
	
	/**
	 * VytovrÌ tlaËidlo pre zruöenie zdieæania.
	 */
	private void createStopAction() {
		this.stopAction = new Action() {
			public void run() {
				if (Connection.getInstance().isLogin()) {
					if (Connection.getInstance().isAdmin()) {
						if (CommonSharing.getInstance().isShared()) {
							Messenger.getInstance().sendMultiCastMessage(MessageFlags.ID + Connection.getInstance().getLoginName() + MessageFlags.SEPARATOR + MessageFlags.RESPONSE + MessageFlags.STOP_SHARE);
							Activator.getDefault().destroy();
							stopAction.setEnabled(false);
						}
					} else if (Connection.getInstance().isLogin()) {
						if (Activator.getDefault().getProject() != null) {
							Activator.getDefault().destroy();
							stopAction.setEnabled(false);
						}
					}
				}					
			}			
		};
		this.stopAction.setEnabled(false);
		this.stopAction.setImageDescriptor(loginAction.getImageDescriptor().createFromImageData(
				new ImageData(CustomView.class.getResourceAsStream("images/stop.png"))));
		this.stopAction.setText(" UkonËenie zdieÂania ");
	}
	
	@Override
	public void setFocus() {
	}	
	
	/**
	 * VykreslÌ komponenty do View.
	 */
	synchronized public void draw() {
		Display.getDefault().asyncExec(new Runnable() {
			
			@Override
			public void run() {
				scomposite.dispose();
				scrolledComposite.dispose();
				createContent();
				if (Connection.getInstance().isAdmin()) {
					if (!syncUserItems.isEmpty()) {
						Label label = new Label(scomposite, SWT.NONE);
						label.setLayoutData(new GridData(25, 25));
						label.setImage(new Image(label.getDisplay(), new ImageData(CustomView.class.getResourceAsStream("images/sync.png"))));
						addInfoLabel("SynchronizovanÌ pouûÌvatelia", true);
						for (SyncUserItem item : syncUserItems) {
							item.setComposite(scomposite);
							item.draw();
						}
					}					
					if (!waitUserItems.isEmpty()) {
						addGap();
						Label label = new Label(scomposite, SWT.NONE);
						label.setLayoutData(new GridData(25, 25));
						label.setImage(new Image(label.getDisplay(), new ImageData(CustomView.class.getResourceAsStream("images/nonsync.png"))));
						addInfoLabel("»akaj˙ci pouûÌvatelia", false);
						boolean found = false;
						for (WaitUserItem item : waitUserItems) {
							if (!item.isForSync() && !item.isInvated()) {
								addInvateAllButton(true);
								found = true;
								break;
							}
						}
						if (!found) {
							addInvateAllButton(false);
						}
						found = false;
						for (WaitUserItem item : waitUserItems) {
							if (item.isForSync()) {
								addSyncAllButton(true);
								found = true;
								break;
							}
						}
						if (!found) {
							addSyncAllButton(false);
							
						}
						addGap();
						for (WaitUserItem item : waitUserItems) {
							item.setComposite(scomposite);
							item.draw();
						}
					}					
				} else {
					if (adminLabel != null) {
						Label label = new Label(scomposite, SWT.NONE);
						label.setLayoutData(new GridData(25, 25));
						label.setImage(new Image(label.getDisplay(), new ImageData(CustomView.class.getResourceAsStream("images/sync.png"))));
						addInfoLabel("UËiteæ zdieæaj˙ci projekt  ", true);
						adminLabel.setComposite(scomposite);
						adminLabel.draw();
					}
				}
				setScroll();
				parent.layout(true);
			}
		});
	}
	
	/**
	 * VytvorÌ tlaËidlo pre synchronizovanie vöetk˝ch ûiadateÂov.
	 * @param boolean enable - true, ak povoliù stlaËenie tlaËidlo synchroniz·cie. 
	 */
	private void addSyncAllButton(boolean enable) {
		if (Connection.getInstance().isAdmin()) {
			new Label(scomposite, SWT.NONE);
			new Label(scomposite, SWT.NONE);
			Button button = new Button(scomposite, SWT.PUSH);
			button.setLayoutData(new GridData(105, 23));
			button.setEnabled(enable);
			button.setImage(new Image(button.getDisplay(), new ImageData(WaitUserItem.class.getResourceAsStream("images/button_sync.png"))));
			createSyncAllListener(button);
		}
	}
	
	/**
	 * VytvorÌ tlaËidlo pre pozvanie vöetk˝ch ûiadateÂov.
	 * @param boolean enable - true, ak povoliù stlaËenie tlaËidlo pozvania. 
	 */
	private void addInvateAllButton(boolean enable) {
		if (Connection.getInstance().isAdmin()) {
			Button button = new Button(scomposite, SWT.PUSH);
			button.setLayoutData(new GridData(105, 23));
			button.setEnabled(enable);
			button.setImage(new Image(button.getDisplay(), new ImageData(WaitUserItem.class.getResourceAsStream("images/button_invate.png"))));
			createInvateAllListener(button);
		}
	}
	
	private void addInfoLabel(String title, boolean b) {
		Label infoLabel = new Label(scomposite, SWT.NONE);
	    Font font1 = new Font(infoLabel.getDisplay(), "Tahoma", 8, SWT.BOLD);
	    infoLabel.setFont(font1);
		infoLabel.setText(title);
		if (b) {
			Label infoLabel1 = new Label(scomposite, SWT.NONE);	
		}
	}
	
	/**
	 * Prid· SyncUserItem a prekreslÌ
	 * @param login - meno pouûÌvateæa
	 */
	synchronized public void addSyncUserItem(String login) {
		ArrayList<String> names = new ArrayList<>();
		for (SyncUserItem item : syncUserItems) {
			names.add(item.getUserName());
		}
		if (!names.contains(login)) {
			SyncUserItem item = new SyncUserItem();
			item.setUserName(login);
			syncUserItems.add(item);	
		}
		draw();
	}
	
	/**
	 * VytvorÌ medzeru vo View.
	 */
	private void addGap() {
		for (int i = 0; i < 3; i ++) {
			Label label = new Label(scomposite, SWT.NONE);
		}
	}
	
	/**
	 * Odstr·ni WaitUserItem a prekreslÌ
	 * @param user
	 */
	synchronized public void removeWaitingUser(String user) {
		ArrayList<WaitUserItem> items = new ArrayList<>();
		for (WaitUserItem item : waitUserItems) {
			if (!item.getUserName().equals(user)) {
				items.add(item);
			}
		}
		this.waitUserItems = items;
		draw();
	}
	
	/**
	 * Prid· WaitUserItem a prekreslÌ
	 * @param login - meno
	 * @param isInvated - je pozvanÌ
	 */
	synchronized public void addWaitUserItem(String login, boolean isInvated) {
		for (WaitUserItem item : waitUserItems) {
			if (item.getUserName().equals(login)) {
				if (item.isInvated() == isInvated && !item.isForSync()) {
					item.setIsInvated(isInvated);
					item.setForSync();
					draw();
				}
				return;
			}
		}
		WaitUserItem item = new WaitUserItem();
		item.setUserName(login);
		if (isInvated) {
			item.setIsInvated(true);
			item.setForSync();
		}
		waitUserItems.add(item);
		draw();
	}
	
	/**
	 * NastavÌ SyncUserItem tlaËidlo na disable.
	 * @param userName - meno pouûÌvateæa pre ktorÈho to neplatÌ
	 */
	protected void setDisableOtherAccessButton(String userName) {
		for (SyncUserItem item : syncUserItems) {
			if (!item.getUserName().equals(userName)) {
				item.setDisableButton();
			}
		}
	}
	
	/**
	 * NastavÌ SyncUserItem tlaËidlo na enable.
	 * @param userName - meno pouûÌvateæa pre ktorÈho to neplatÌ
	 */
	protected void setEnableOtherAccessButton(String userName) {
		for (SyncUserItem item : syncUserItems) {
			if (!item.getUserName().equals(userName)) {
				item.setEnableButton();
			}
		}
	}
	
	/**
	 * Vytvorenie funkcionalitu pre tlaËidlo SyncAll
	 * @param Button button - referencia na tlaËidlo. 
	 */
	private void createSyncAllListener(Button button) {
		button.addSelectionListener(new SelectionListener() {
			
			@SuppressWarnings("unused")
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				ArrayList<String> names = new ArrayList<>();
				ArrayList<WaitUserItem> copy = (ArrayList<WaitUserItem>) waitUserItems.clone();
				for (WaitUserItem item : copy) {
					names.add(item.getUserName());
				}
				MasterSharing.getInstance().syncUsers(names);
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
	}
	
	/**
	 * Vytvorenie funkcionalitu pre tlaËidlo InvateAll
	 * @param Button button - referencia na tlaËidlo. 
	 */
	private void createInvateAllListener(Button button) {
		button.addSelectionListener(new SelectionListener() {
			
			@SuppressWarnings("unchecked")
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				ArrayList<WaitUserItem> copy = (ArrayList<WaitUserItem>) waitUserItems.clone();
				for (WaitUserItem item : copy) {
					if (!item.isInvated()) {
						item.invate();
					}
				}
				draw();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});
	}
	
	/**
	 * Prid· ActiveAdminItem a prekreslÌ
	 * @param admin - meno
	 * @param boolean sync - true, ak je synchonn˝, inak false.
	 * @param boolean invate - true, ak je pozvan˝, inak false.
	 */
	synchronized public void addAdminItem(String admin, boolean sync, boolean invate) {
		if (this.adminLabel == null) {
			this.adminLabel = new ActiveAdminLabel();
			this.adminLabel.setUserName(admin);
		}
		if (sync) {
			this.adminLabel.setCan(true);
			this.adminLabel.setRequestInvation(true);
			this.adminLabel.setRequestSync(true);
		} else if (invate){
			this.adminLabel.setCan(false);
			this.adminLabel.setRequestSync(false);
		} else {
			this.adminLabel.setCan(false);
			this.adminLabel.setRequestSync(true);
			this.adminLabel.setRequestInvation(true);			
		}
		draw();
	}
	
	/**
	 * Zmazanie adminLabel
	 * @param admin
	 */
	public void removeAdminItem(String admin) {
		this.adminLabel = null;
		draw();
	}

	/**
	 * Nastavenie SyncUserItem hodnotu wantAccess
	 * @param login - meno
	 */
	public void setSyncUserWantAccess(String login) {
		for (SyncUserItem syncUserItem : syncUserItems) {
			if (syncUserItem.getUserName().equals(login)) {
				syncUserItem.setWantAccess(true);
				draw();
				break;
			}
		}
	}
	
	/**
	 * Zmazanie SyncUserItem
	 * @param user - meno
	 */
	public void removeSyncUserItem(String user) {
		SyncUserItem item = null;
		for (SyncUserItem syncUserItem : syncUserItems) {
			if (syncUserItem.getUserName().equals(user)) {
				if (syncUserItem.isHasAccess()) {
					CommonSharing.getInstance().setValueAccess(true,null);
					
				}
				item = syncUserItem;
				break;
			}
		}
		if (item != null) {
			syncUserItems.remove(item);
			draw();
		}
	}
	
	/**
	 * Pridanie poloûky SyncUserItem alebo WaitUserItem
	 * @param user - meno
	 * @param type - sync alebo wait
	 * @param operation - pridanie mazanie
	 * @param isInvated - je pozvan˝
	 * @param access - ma prÌstup
	 */
	synchronized public void addUser(String user ,String type, String operation, Boolean isInvated, Boolean access ) {
		if (access) {
			setSyncUserWantAccess(user);
			return;
		} else {
			if (user == null) {
				for (SyncUserItem item : syncUserItems) {
					item.setWantAccess(false);
					item.setEnableButton();
				}
				draw();
			}
		}
		if ( operation.equals("remove") ) {
			if (type.equals("wait")) {
				removeWaitingUser(user);
			} else if (type.equals("sync")) {
				removeSyncUserItem(user);
			}			
		} else if ( operation.equals("add") ) {
			if (type.equals("wait")){
				removeSyncUserItem(user);
				addWaitUserItem(user, isInvated);
			} else if (type.equals("sync")) {
				removeWaitingUser(user);
				addSyncUserItem(user);
			}
		}		
	}
	
	/**
	 * Nastav hodnotu pre poûiadanie pre prÌstup na true.
	 */
	public void modifyAdmin() {
		if (adminLabel != null) {
			adminLabel.setCan(true);
			draw();
		}
	}
}