package rtsharecode.common;

import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import rtsharecode.common.communication.Connection;
import rtsharecode.common.communication.Messenger;
import rtsharecode.common.gui.view.CustomView;
import rtsharecode.common.listeners.ProjectChangeManager;
import rtsharecode.common.listeners.ProjectResourceChangeListener;
import rtsharecode.common.sharing.CommonSharing;
import rtsharecode.common.sharing.MasterSharing;
/**
 * The activator class kontroluje �ivotn� cyklus pluginu.
 */
public class Activator extends AbstractUIPlugin {

	public static final String PLUGIN_ID = "ShareCode"; //$NON-NLS-1$
	private static Activator plugin;
	private IProject project;
	private ProjectChangeManager managerListener;
	private Shell shell;
	private CustomView view = null;
	private IProject projectWithListeners = null;
	private ProjectResourceChangeListener resouseChangeListener;
	
	/**
	 * 
	 * @return CustomView - referencia na grafick� plochu.
	 */
	public CustomView getView() {
		return this.view;
	}
	
	/**
	 * Nastav� funkcionalitu pluginu na p�vodn� nastavenia.
	 */
	public void destroy() {
		if (resouseChangeListener != null) {
			getWorkSpace().removeResourceChangeListener(resouseChangeListener);
		}
		this.resouseChangeListener = null;
		Messenger.getInstance().removeListener();
		Messenger.getInstance().removeSender();
		
		if (this.managerListener != null) {
			this.managerListener.destroy();
		}
		try {
			TimeUnit.SECONDS.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		this.managerListener = null;
		this.projectWithListeners = null;
		this.project = null;
		CommonSharing.getInstance().destroy();
		MasterSharing.getInstance().destroy();
		view.clear();
		view.setStopActionButtonEnabled(false);
	}
	/**
	 * 
	 * @return ProjectChangeManager - vrati referenciu
	 */
	public ProjectChangeManager getManagerListener() {
		return this.managerListener;
	}
	
	/**
	 * Predp�san� met�da, pou��va sa pri sp��tan� pluginu.
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		this.plugin = this;
		this.updatePreferencesStore();
		
	}
	
	/**
	 * Obnov� hodnoty v ukladacom mieste pre nastavenia pluginu.
	 */
	public void updatePreferencesStore() {
		Connection.getInstance().setPreferences(this.getPreferenceStore());
	}
	
	/**
	 * Zastav� beh pluginu v Eclipse, predp�san� met�da.
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}
	
	/**
	 * Komponent potrebn� pri zobrazovan� JFace dial�gov�ch okien
	 * @return Shell shell
	 */
	public Shell getShell () {
		if (this.shell == null)
			this.shell = new Shell();
		return this.shell;
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}
	
	/**
	 * Vytvor� nastroje pre po��vanie zmien (ProjectChangeManager, ProjectResourceChangeListener)
	 */
	@SuppressWarnings("restriction")
	public void initializeListenersForCurrentProject() {
		if (this.project != null && this.projectWithListeners == null) {
			getDefault().managerListener = new ProjectChangeManager(this.project);
			this.resouseChangeListener = new ProjectResourceChangeListener(this.project);
			if (Connection.getInstance().isAdmin()) {
				getWorkSpace().addResourceChangeListener(new ProjectResourceChangeListener(this.project));
			}			
			this.projectWithListeners = project;
		}
	}
	
	/**
	 * Obnov� nastavenia in�tancie ProjectChangeManager
	 */
	public void refreshListener() {
		if (getDefault().managerListener != null) {
			getDefault().managerListener.refresh();
		}
	}
	
	/**
	 * Vr�ti workspace, ten sl��i na z�skanie projektov.
	 * @return Workspace
	 */
	@SuppressWarnings("restriction")
	private static Workspace getWorkSpace() {
		return (Workspace) ResourcesPlugin.getWorkspace();
	}
	
	/**
	 * Nastav� projekt.
	 * @param project - IProject
	 */
	public void setProject(IProject project) {
		this.project = project;		
	}
	
	/**
	 * Vr�ti project atrib�t.
	 * @return IProject project
	 */
	public IProject getProject() {
		return this.project;
	}		
	
	/**
	 * Nastav� referenciu pre View.
	 * @param CustomView view
	 */
	public void setView(CustomView view) {
		this.view = view;
	}
	
	/**
	 * Nainicializuje messenger, za�n� sa pr�jima� spr�vy.
	 */
	public void initializeMessenger() {
		Messenger.getInstance().initializeMessenger();
	}
}
