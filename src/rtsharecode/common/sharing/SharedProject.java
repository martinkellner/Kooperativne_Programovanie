package rtsharecode.common.sharing;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.LibraryLocation;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import rtsharecode.common.Activator;
import rtsharecode.common.communication.Connection;

/**
 * Trieda SharedProject - vytvorenie projektu.
 * @author Martin Kellner
 */
public class SharedProject {

	/* ---- Variables ---- */
	private IJavaProject javaProject;
	private IProject project;
	private IFolder src;
	private String projectStructure;	
	private Map<String, String> packageStructure = new HashMap<>(); 
	private Map<String, ArrayList<String>> filesStructure = new HashMap<>(); 
	private ArrayList<IFile> files = new ArrayList<>();
	private boolean PROCESS_STATUS = false;
	private final String CHARSET = "UTF-8";
	private String login;
	private boolean creating = false;
		
	/**
	 * Konötruktor
	 * String projectStructure - znakov· reprezent·cia ötrukt˙ry projektu.
	 */
	public SharedProject(String projectStructure) {
		this.projectStructure = projectStructure;
	}
	
	/**
	 * ZÌskanie n·zvu projektu z znakovej reprezent·cie ötrukt˙ry projektu.
	 * @param structure - znakov· reprezent·cia ötrukt˙ry projektu.
	 * @return
	 */
	private String getProjectName(String structure) {
		String projectName = "";
		if (structure != null && structure.length() > 5) {
			for (int i = 6; i < structure.length(); i++) { 
				if (structure.charAt(i) == '|') {
					return projectName;
				}
				projectName += String.valueOf(structure.charAt(i));
			}
		}
		return null;
	}
	
	/**
	 * Vr·ti status, Ëi je projekt uû vytvoren˝
	 * @return boolean true, ak je vytvoren˝, inak false.
	 */
	public boolean getPROCESS_STATUS() {
		return PROCESS_STATUS;
	}
	
	/**
	 * Vr·ti true, ak existuje dan˝ projekt.
	 * @param projectName - n·zov projektu.
	 * @return boolean true, ak existuje, inak false.
	 */
	public boolean existAlreadyProject(String projectName) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		return root.getProject(projectName).exists();
	}
	
	/**
	 * Vytvorenie projektu
	 * @param login - meno
	 */
	public void createProject(String login) {
		this.creating = true;
		this.login = login;
		IProgressMonitor progressMonitor = new NullProgressMonitor();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		project = root.getProject(getProjectName(projectStructure).equals("") ? "" : getProjectName(projectStructure));
		Display.getDefault().syncExec(new Runnable() {
			
			@Override
			public void run() {
				boolean result = MessageDialog.openQuestion(Display.getDefault().getActiveShell(), "Poûiadavka na zdieæanie.", "Bola v·m zaslan· poûiadavka na "
						+ "na zdieæanie projektu " + getProjectName(projectStructure) + ". Chcete zdieæaù?");
				if (result) {
					if (project.exists()) {
						boolean result0 = MessageDialog.openQuestion(Display.getDefault().getActiveShell(), "Projekt uû existuje.", "Vo workspace uû existuje "
								+ "projekt z n·zvom " + getProjectName(projectStructure) + ". Pre uËely zdieæanie sa prepÌöe. Chcete projekt prepÌsaù?");
						if (result0) {
							try {
								project.delete(true, progressMonitor);
								createJavaProject();
								PROCESS_STATUS = true;
							} catch (CoreException e) {
								PROCESS_STATUS = false;
								e.printStackTrace();
							}
						}
					} else {
						try {
							createJavaProject();
							PROCESS_STATUS = true;
						} catch (CoreException e) {
							PROCESS_STATUS = false;
						}
					}
				}
			}
		});
	}
	
	/**
	 * Vytvorenie Java projektu.
	 * @throws CoreException
	 */
	private void createJavaProject() throws CoreException {
		IProjectDescription description;
		project.create(null);
		project.open(null);
		description = project.getDescription();
		description.setNatureIds(new String[] { JavaCore.NATURE_ID });
		project.setDescription(description, null);
		javaProject = JavaCore.create(project);
		
		IFolder binFolder = project.getFolder("bin");
		if (binFolder.exists()) {
				binFolder.delete(true, null);
		}
		binFolder.create(false, true, null);
		javaProject.setOutputLocation(binFolder.getFullPath(), null);
		
		List<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
		IVMInstall vmInstall = JavaRuntime.getDefaultVMInstall();
		LibraryLocation[] locations = JavaRuntime.getLibraryLocations(vmInstall);
			
		for (LibraryLocation element : locations) {
			entries.add(JavaCore.newLibraryEntry(element.getSystemLibraryPath(), null, null));
		}		
			
		javaProject.setRawClasspath(entries.toArray(new IClasspathEntry[ entries.size() ]), null);
		javaProject.setOutputLocation(binFolder.getFullPath(), null);
			
		src = project.getFolder("src");
		if (src.exists()) {
			src.delete(true, null);
		}				
		src.create(false, true, null);

		IPackageFragmentRoot root = javaProject.getPackageFragmentRoot(src);
		IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
		IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
		System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
		newEntries[oldEntries.length] = JavaCore.newSourceEntry(root.getPath());
		javaProject.setRawClasspath(newEntries, null);
			
		cutProjectDescription();
		processProjectDescription();
		createPackages();
		createResources();
		Activator.getDefault().setProject(this.project);
		Activator.getDefault().initializeListenersForCurrentProject();
		
		CommonSharing.getInstance().setSharedProject(this);
		if (creating) {
			CommonSharing.getInstance().sendSync(login);
		} else {
			CommonSharing.getInstance().sendAck(login);
		}
		if (!Connection.getInstance().isAdmin()) {
			Activator.getDefault().getView().addAdminItem(login, false, false);
		}
	}
	
	/**
	 * Vytvorenie zdrojovÈho s˙boru.
	 * @throws CoreException
	 */
	private void createResources() throws CoreException {
		Collection<String> resourceNames = filesStructure.keySet();
		Iterator<String> it = resourceNames.iterator();
		while (it.hasNext()) {
			String resourceName = it.next();
			createResource(resourceName, filesStructure.get(resourceName));
		}
	}
	
	/**
	 * Spracovanie znakovej reprezent·cie ötrukt˙ry projektu.
	 * @return String upraven· reprezent·cia.
	 * @throws JavaModelException
	 */
	private String cutProjectDescription() throws JavaModelException {
		String subString = "";
		String workedString = "";
		boolean c = false;
		for (int i = 0; i < projectStructure.length(); i ++) {
			if (c) 
				subString += String.valueOf(projectStructure.charAt(i));
			else if (projectStructure.charAt(i) == '|') 
				c = true;
			else 
				workedString += String.valueOf(projectStructure.charAt(i));
		}
		projectStructure = subString;
		return workedString;
	}
	
	private void createResource(String name, ArrayList<String> packs) throws CoreException {
		
		for (String pack : packs) {
			IFolder folder = project.getFolder("src/" + packageStructure.get(pack).replace('.', '/'));
			IFile file = folder.getFile(name);
			file.create(null, false, null);
			file.setContents(new ByteArrayInputStream("".getBytes()), 1, null);
			files.add(file);
			project.refreshLocal(IProject.DEPTH_INFINITE, null);
		}		
	}
	
	/**
	 * Spracovanie znakovej reprezent·cie ötrukt˙ry projektu.
	 * @throws JavaModelException
	 */
	private void processProjectDescription() throws JavaModelException {
		String workedString = cutProjectDescription();		
		if (!workedString.isEmpty()) {
			if (workedString.startsWith("rname=")) {
				String[] names = receiveNamesFromString(workedString);
				if (names == null) {
					return;
				}
				String folderName = names[0];
				String resourceName = names[1];
				if (filesStructure.containsKey(resourceName)) {
					filesStructure.get(resourceName).add(folderName); 
				} else {
					filesStructure.put(resourceName, new ArrayList<>());
					filesStructure.get(resourceName).add(folderName);
				}						
				
			} else if (workedString.startsWith("fname=")) {
				String[] names = receiveNamesFromString(workedString);
				if (names == null) {
					return;
				}
				String folderName = names[0];
				String subFolderName = names[1];
				
				if (folderName.equals("src")) {
					packageStructure.put(subFolderName, subFolderName);
				} else {
					packageStructure.put(subFolderName, packageStructure.get(folderName) + "." + subFolderName);
				}				
			}			
			processProjectDescription();
		}
	}
	
	/**
	 * Vytvorenie balÌka zdrojov˝ch s˙borov.
	 * @throws JavaModelException
	 */
	private void createPackages() throws JavaModelException {
		ArrayList<String> packageName = packageStructureFilter();
		IPackageFragmentRoot srcRoot = javaProject.getPackageFragmentRoot(src);
		for (String pack : packageName) {
			srcRoot.createPackageFragment(pack, false, null) ;
		}
	}	
	
	/**
	 * Spracovanie balÌkov, nov· reprezent·cia palÌkov.
	 */
	private ArrayList<String> packageStructureFilter() {
		Collection<String> packagesName = packageStructure.keySet();
		ArrayList<String> filteredPackagesNames = new ArrayList<>();
		Iterator<String> it0 = packagesName.iterator();
		while (it0.hasNext()) {
			Iterator<String> it1 = packagesName.iterator();
			String name0 = it0.next();
			int v = 0;
			while (it1.hasNext()) {
				String name1 = it1.next();
				if (! name0.equals(name1)) {
					if (packageStructure.get(name1).startsWith(packageStructure.get(name0) + ".")) {
						v = 1;
					}
				}
			}
			if (v == 0) {
				filteredPackagesNames.add(packageStructure.get(name0));
			}
		}
		return filteredPackagesNames;
 	}

	/**
	 * ZÌskanie mien s˙borov a balÌkov z reùazca.
	 * @param workedString
	 * @return p·r meno, meno.
	 */
	private String[] receiveNamesFromString(String workedString) {
		String subString = workedString.substring(6);
		String firstName = "";
		String secondName = "";
		boolean separator = false;
		for (int i = 0; i < subString.length(); i ++) {
			if (subString.charAt(i) == '>') {
				separator = true;
			} else if (separator) {
				secondName += String.valueOf(subString.charAt(i));
			} else {
				firstName += String.valueOf(subString.charAt(i));
			}					
		}
		if (firstName.isEmpty() || secondName.isEmpty())
			return null;
		return new String[] { firstName, secondName };
	}
	
	/**
	 * Nastavenie obsahu s˙bora.
	 * @param fileName - n·zov s˙boru
	 * @param content - obsah
	 * @throws CoreException
	 */
	public void setFileContent(String fileName, String content) throws CoreException {
		if (!files.isEmpty()) {
			for (IFile file : files) {
				if (file.getName().equals(fileName)) {
					InputStream is = new ByteArrayInputStream(content.getBytes(Charset.forName(CHARSET)));
					file.setCharset(CHARSET, null);
					file.setContents(is , 1, null);
				}
			}
		} else {
			throw new CoreException(null);
		}
	}
	
	/**
	 * Aktualizovania ötrukt˙ry projektu.
	 * @param structure - nov· znakovan· reprezent·cia projektu.
	 */
	synchronized public void actualizeProject (String structure) {
		try {
			resetProject(structure);
			this.PROCESS_STATUS = true;
		} catch (CoreException e) {
			this.PROCESS_STATUS = false;
		}
	}
	
	/**
	 * Vytvorenie novej ötrikt˙ry projektu.
	 * @param newStringStructure nov· reprezent·cia ötrukt˙ry.
	 * @throws CoreException
	 */
	synchronized private void resetProject(String newStringStructure) throws CoreException {
		if (newStringStructure.equals(projectStructure)) {
			return;
		}
		PROCESS_STATUS = false;
		this.creating = false;
		Activator.getDefault().getView().setStopActionButtonEnabled(true);
		project.delete(true , null);
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		this.projectStructure = newStringStructure;
		project = root.getProject(getProjectName(projectStructure).equals("") ? "" : getProjectName(projectStructure));
		this.files = new ArrayList<>();
		this.filesStructure = new HashMap<>();
		this.packageStructure = new HashMap<>();
		
		createJavaProject();
		PROCESS_STATUS = true;
		Activator.getDefault().refreshListener();
	}
	
	/**
	 * Spracovanie spr·vy o obsahu s˙bora.
	 * @param content - obsah
	 * @throws CoreException
	 */
	synchronized public void actualizeFileContent(String content) throws CoreException {
		if (content != null) {
			int i = 0;
			while (content.length() > i && content.charAt(i) != '#') {
				i ++;
			}
			if (i != content.length()) {
				String relativePath = content.substring(0, i);
				String context = content.substring(i + 1);
				for (IFile file : files) {
					if (file.getProjectRelativePath().toString().equals(relativePath)) { 
						file.setContents(new ByteArrayInputStream(context.getBytes())  , 1, null);
						break;
					}
				}
			}
			project.refreshLocal(IProject.DEPTH_INFINITE, null);
		}
	}

	/**
	 * Vr·ti IProject project
	 * @return IProject project
	 */
	public IProject getProject() {
		return project;
	}
}