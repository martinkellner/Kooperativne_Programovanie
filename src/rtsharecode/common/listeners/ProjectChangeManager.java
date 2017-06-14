package rtsharecode.common.listeners;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.ITextEditor;

import rtsharecode.common.communication.Connection;
import rtsharecode.common.sharing.CommonSharing;
/**
 * Trieda ProjectChangeManager - odchytavania a aplikovanie zmien obsahu súboru.
 * @author Martin Kellner
 *
 */
public class ProjectChangeManager {

	private IProject project;
	private ArrayList<IFile> files;
	private ArrayList<IDocument> documents;
	private ArrayList<IDocumentListener> listeners;
	private ArrayList<IEditorPart> editors;
	private boolean saveDone = true;
	private boolean freeForListenerChanges = true;
	protected final ArrayList<Integer> resourcesChanges = new ArrayList<>();
	private ResourceChangeBlocker blocker = new ResourceChangeBlocker();
	private Thread blockerThread = new Thread(blocker);
	private ResourceFinalChangesListener checker;
	private boolean interuptChecker;
	private Thread checkerThread;
	private boolean nonSendingChange = false;
	private int request = 0;
	private boolean interuptBlocker;
	
	/**
	 * Vráti true, ak ResourceChangeBlocker nie je aktívny
	 * @return true, ak je aktívny, inak false.
	 */
	public boolean isInteruptBlocker() {
		return interuptBlocker;
	}
	
	/**
	 * Nastaví hodnotu interuptBlocker 
	 * @param boolean interuptBlocker
	 */
	public void setInteruptBlocker(boolean interuptBlocker) {
		this.interuptBlocker = interuptBlocker;
	}

	/**
	 * Vráti hodnotu, èi je aktívny ResourceFinalChangeListener
	 * @return true, ak je aktívny, inak false
	 */
	public boolean isInteruptChecker() {
		return interuptChecker;
	}
	
	/**
	 * Nastaví hodnotu interuptChecker
	 * @param boolean interuptChecker
	 */
	public void setInteruptChecker(boolean interuptChecker) {
		this.interuptChecker = interuptChecker;
	}
	
	/**
	 * Konštruktor
	 * @param IProject project
	 */
	public ProjectChangeManager(IProject project) {
		super();
		this.project = project;
		actualizeSettings();
		createPartListener();
		if (Connection.getInstance().isAdmin()) {
			this.interuptBlocker = false;
			this.interuptChecker = false;
			this.blockerThread.start();	
		}
	}
	
	/**
	 * Nastaví funkcionalitu triedy
	 * @throws WorkbenchException
	 */
	private void setListeners() throws WorkbenchException {
		Display.getDefault().asyncExec(new Runnable() {
			
			@Override
			public void run() {
				freeForListenerChanges = false;
				IWorkbenchWindow[] workbenchWindows = PlatformUI.getWorkbench().getWorkbenchWindows();
				IWorkbenchPage page = workbenchWindows[0].getPages()[0];
				for (IFile file : files) {
					IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(file.getName());
					try {
						IEditorPart editorPart = page.openEditor(new FileEditorInput(file), desc.getId());
						if (editorPart instanceof ITextEditor) {
							IDocument document = (((ITextEditor) editorPart).getDocumentProvider()).getDocument(editorPart.getEditorInput());
							if (!documents.contains(document)) {
								IDocumentListener documentListener = createListener();
								document.addDocumentListener(documentListener);
								documents.add(document);
								listeners.add(documentListener);
								editors.add(editorPart);
							}
						}
						
					} catch (PartInitException e) {
						e.printStackTrace();
					}
				}
				freeForListenerChanges = true;
			}
		});	
	}
	
	/**
	 * Zachytenie otvoreného okna a nastavenie.
	 */
	private void createPartListener () {
		IWorkbenchPage page = PlatformUI.getWorkbench().getWorkbenchWindows()[0].getPages()[0] ;
		if (page != null) {
			page.addPartListener(new IPartListener2() {
				
				@Override
				public void partVisible(IWorkbenchPartReference arg0) {
				}
				
				@Override
				public void partOpened(IWorkbenchPartReference arg0) {
					if (freeForListenerChanges) {
						IEditorPart iEditorPart = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
						ITextEditor textEditor = (ITextEditor) iEditorPart;
						IDocument document = textEditor.getDocumentProvider().getDocument(iEditorPart.getEditorInput());
						IEditorPart same = null;
						boolean found = false;
						for (IEditorPart editorPart : editors) {
							if (editorPart.getEditorInput().equals(iEditorPart.getEditorInput())) {
								same = editorPart;
								found = true;
								break;
							}
						}
						if (found) {
							int index = editors.indexOf(same);
							documents.get(index).removeDocumentListener(listeners.get(index));
							documents.remove(index);
							editors.remove(index);
							editors.add(index, iEditorPart);
							documents.add(index, document);
							document.addDocumentListener(listeners.get(index));
						}							
					}
				}
				
				@Override
				public void partInputChanged(IWorkbenchPartReference arg0) {
				}
				
				@Override
				public void partHidden(IWorkbenchPartReference arg0) {
				}
				
				@Override
				public void partDeactivated(IWorkbenchPartReference arg0) {
				}
				
				@Override
				public void partClosed(IWorkbenchPartReference arg0) {
								
				}
				
				@Override
				public void partBroughtToTop(IWorkbenchPartReference arg0) {
				}
				
				@Override
				public void partActivated(IWorkbenchPartReference arg0) {
				}
			});
		}
	}		 

	/**
	 * Zatvorí okna editora.
	 */
	private void closeAllOpenWindow() {
		Display.getDefault().syncExec(new Runnable() {
			
			@Override
			public void run() {
				IWorkbenchWindow workbenchWindows[] = PlatformUI.getWorkbench().getWorkbenchWindows();
				if (workbenchWindows != null) {
					for (IWorkbenchWindow window : workbenchWindows) {
						IWorkbenchPage[] pages = window.getPages();
						if (pages != null) {
							for (IWorkbenchPage page : pages) {
								IEditorReference[] editorRefs = page.getEditorReferences();
								page.closeEditors(editorRefs, true);
							}
						}
					}
				}		
			}
		});		
	}
	
	/**
	 * Vráti súbory
	 * @return ArrayList<IFile> files.
	 */
	public ArrayList<IFile> getFiles() {
		return files;
	}
	
	/**
	 * Obnoví nastavenia.
	 */
	public void refresh() {
		actualizeSettings();
	}
		
	/**
	 * Obnoví nastavenia 
	 */
	private void actualizeSettings() {
		files = new ArrayList<>();
		try {
			recursiveReceiveFiles(project.getFolder("src"));
		} catch (CoreException e) {
			e.printStackTrace();
		}
		if (editors == null) {
			editors = new ArrayList<>();
		}		
		if (listeners == null) {
			listeners = new ArrayList<>();
		}
		if (documents == null) {
			documents = new ArrayList<>();
		}
		for (int j = 0; j < documents.size(); j++) {
			documents.get(j).removeDocumentListener(listeners.get(j));
		}
		documents.clear();
		listeners.clear();
		editors.clear();
		try {
			saveAll();
		} catch (InvocationTargetException | InterruptedException e1) {
			e1.printStackTrace();
		}
		closeAllOpenWindow();
		try {
			setListeners();
		} catch (WorkbenchException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Získa súbory z src prieèinku projektu.
	 * @param IResource resource 
	 * @throws CoreException
	 */
	private void recursiveReceiveFiles(IResource resource) throws CoreException {
		if (resource != null) {
			if (resource instanceof IContainer) {
				IContainer container = (IContainer) resource;
				for (IResource resource2 : container.members()) {
					recursiveReceiveFiles(resource2);
				}
			} else if (resource instanceof IFile) {
				files.add((IFile) resource);
			}			
		}
	}
		
	/**
	 * Vytvotí funkcionalitu pre poèúvania a zachytávanie zmien obsahu súboru pre kontrétny súbor.
	 * @return
	 */
	private IDocumentListener createListener() {
		return new IDocumentListener() {
			
			@Override
			public void documentChanged(DocumentEvent event) {
				if (Connection.getInstance().isAdmin()) {
					runBlockerIfNeccesary();
				}
				if (CommonSharing.getInstance().hasAccess()) {
					int index = documents.indexOf(event.getDocument());
					CommonSharing.getInstance().handleChangedContent(index, ((StyledText) editors.get(index).getAdapter(Control.class)).getCaretOffset());
				}
			}
			
			@Override
			public void documentAboutToBeChanged(DocumentEvent arg0) {}
		};
	}
	
	/**
	 * Uloží všetko v editore.
	 * @throws InvocationTargetException
	 * @throws InterruptedException
	 */
	public void saveAll() throws InvocationTargetException, InterruptedException {
		saveDone = false;
		IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
		if (windows.length != 0 && windows[0] != null) { 
			IWorkbenchPage[] pages = windows[0].getPages();
			for (IWorkbenchPage page : pages) {
				if (page != null) {
					IEditorReference[] editorReferences = page.getEditorReferences();
					NullProgressMonitor monitor = new NullProgressMonitor();
					if (editorReferences != null){ 
					    for (IEditorReference iEditorReference : editorReferences) {
					        IEditorPart editor = iEditorReference.getEditor(false);
					        if (editor != null && editor.isDirty()) {
				       			Display.getDefault().asyncExec(new Runnable() {
									
									@Override
									public void run() {
										editor.doSave(monitor);
									}
								});        	
							}
					    }
					}
				}
			}
		}
		saveDone = true;
	}
	
	/**
	 * Nastaví nový obsah súboru a pozíciu v dokumente, prípadne okno editora, ak je to potrebné.
	 * @param messageInfo
	 */
	synchronized public void updateContentOfEditor(String messageInfo) {
		if (!CommonSharing.getInstance().hasAccess()) {
				Display.getDefault().asyncExec(new Runnable() {
			
				@Override
				synchronized public void run() {
					if (!messageInfo.isEmpty()) {
						int i = 0;
						while (messageInfo.charAt(i) != '#') {
							i ++;
						}
						String path = messageInfo.substring(0, i);
						String content = messageInfo.substring(i + 1);
						int cut = content.indexOf('|');
						String caretPosition = content.substring(0, cut);
						content = content.substring(cut + 1);
						IWorkbenchWindow window = PlatformUI.getWorkbench().
												  		getActiveWorkbenchWindow();
						int indexOfFile = -1;
						for (IFile file : files) {
							if (path.equals(file.getProjectRelativePath().toString())) {
								indexOfFile = files.indexOf(file);
							}
						}
						if (indexOfFile != -1) {
							IFile file = files.get(indexOfFile);
							try {
								documents.get(indexOfFile).
									replace(0, documents.get(indexOfFile).getLength(), content);
							} catch (BadLocationException e1) {
								e1.printStackTrace();
							}
							try {
								IEditorPart editor = null;
								if (window.getActivePage().findEditor(editors.get(indexOfFile).
										getEditorInput()) == null) {
									IEditorDescriptor desc = PlatformUI.getWorkbench().
											getEditorRegistry().getDefaultEditor(file.getName());
									editor = window.getActivePage().openEditor(
										new FileEditorInput(file), desc.getId());
								} else {
									editor = editors.get(indexOfFile);
									window.getActivePage().activate(editor.getEditorSite().getPart());
								}
								if (editor != null)
								((ITextEditor) editor).selectAndReveal(Integer.valueOf(caretPosition), 0);
								((StyledText) editor.getAdapter(Control.class)).setEditable(false);
							} catch (CoreException e) {
								e.printStackTrace();
							}
						}
					}
				}	
			});
		}
	}
	
	/**
	 * Pridá záznam o zmene štruktúry.
	 */
	synchronized protected void addResourceChanges() {
		if (this.resourcesChanges.isEmpty()) {
			checker = new ResourceFinalChangesListener();
			checkerThread = new Thread(checker);
			this.nonSendingChange = false;
			checkerThread.start();
		}
		this.resourcesChanges.add(1);
	}
	
	/**
	 * Nastaví hodnotu, ktorá signalizuje zmenu, ktorá sa nemá zdie¾a.
	 * @param boolean value
	 */
	synchronized protected void setNonSendingChange(boolean value) {
		this.nonSendingChange = value;
	}
	
	/**
	 * Vráti hodnotu isNonSendingChange
	 * @return boolean isNonSendingChange
	 */
	synchronized protected boolean isNonSendingChange() {
		return this.nonSendingChange;
	}
	
	/**
	 * Pridanie informácie pre zmenu obsahu súboru.
	 */
	synchronized protected void runBlockerIfNeccesary() {
		if (this.request > 30000) {
			this.request = 0;
		}
		this.request ++;
	}
	
	/**
	 * Vráti poèet zmien obsahu.
	 * @return int request
	 */
	synchronized protected int requestForBlocker() {
		return this.request;
	}
	
	/**
	 * Zmaže inštanciu triedy.
	 */
	public void destroy() {
		for (IDocument document : documents) {
			document.removeDocumentListener(listeners.get(documents.indexOf(document)));
		}		
		if (Connection.getInstance().isAdmin()) {
			if (this.blockerThread != null && this.blockerThread.isAlive()) {
				this.interuptBlocker = true;
			}
			if (this.checkerThread != null && this.checkerThread.isAlive()) {
				this.interuptChecker = true;
			}		
		}		
	}	
}