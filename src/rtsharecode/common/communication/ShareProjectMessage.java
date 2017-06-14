package rtsharecode.common.communication;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
/**
 * Trieda ShareProjectMessage - z�ska znakov� reprezent�ciu �trukt�ry projektu.
 * @author Martin Kellner
 * 
 */
public class ShareProjectMessage {

	private IProject project;
	private String textMessage = "";
	private int errorCode;
	private boolean deafultPackage = false;
	
	/**
	 * @return vr�ti znakov� reprezent�ciu �trukt�ry projektu.
	 */
	public String toString() {
		if (deafultPackage) {
			return null;
		}
		checkMessageForm();
		return textMessage;
	}

	/**
	 * Vr�ti ��slo poruchy pri vytv�ran� spr�vy
	 * @return int errorCode
	 */
	public int getErrorCode() {
		return errorCode;
	}
	
	/**
	 * Kon�truktor
	 * @param IProject project
	 */
	public ShareProjectMessage(IProject project) {
		this.project = project;
		this.errorCode = 0;
		receiveMessage();
	}
	
	/**
	 * Vytvor� spr�vu
	 */
	private void receiveMessage() {
		try {
			IContainer src = findSrcFolder(project);
			if (src == null) {
				errorCode = 1;
				textMessage = "";
				return;
			}				
			else {
				recursiveProcessProject(src);
			}
		} catch (CoreException e) {
			textMessage = "";
			errorCode = 2;
			return;
		}
		
	}
	
	/**
	 * N�jde prie�ikon src
	 * @param IContainer container - interface pre prie�inok, s�bor, ...
	 * @return IContainer src
	 * @throws CoreException
	 */
	private IContainer findSrcFolder(IContainer container) throws CoreException {
		
		if (container != null) {
			if (container instanceof IProject) 
				textMessage += "pname=" + container.getName();
			else 
				return null;				
			for (IResource resource : container.members()) {
				if (resource instanceof IFolder && resource.getName().equals("src")) 
					return (IContainer) resource;
			}
		}
		return null;
	}
	
	/**
	 * Rekurz�vne spracuje projekt do stringu
	 * @param IContainer container - interface pre prie�inok, s�bor, ...
	 * @throws CoreException
	 */
	private void recursiveProcessProject(IContainer container) throws CoreException {
		if (container != null) {
           	IResource[] resources =  container.members();
           	for (IResource resource : resources) {
      			if (resource.getName().charAt(0) != '.') {
      				if (resource instanceof IFolder) { 
      					if (resource.getParent().getName().equals(null) || resource.getParent().getName().equals("null")) {
      						deafultPackage = true;
      					}
      					textMessage += (resource.getName().charAt(0) != '.' ? "|fname=" + resource.getParent().getName() + ">" + resource.getName() : "");
          				recursiveProcessProject((IContainer) resource);
      				}
          			else if (resource instanceof IFile) {
          				if (resource.getParent().getName().equals(null) || resource.getParent().getName().equals("null")) {
      						deafultPackage = true;
      					}
          				if (resource.getParent().getName().equals("src")) {
          					deafultPackage = true;
          				}
          				textMessage += (resource.getName().charAt(0) != '.' ? "|rname=" + resource.getParent().getName() + ">" + resource.getName() : "");          			
      				}
      			}
      		}
		}	
	}
	
	/**
	 * Kontrola �trukt�ry, pre zdie�anie nesmie obsahova� projekt default package.
	 */
	private void checkMessageForm() {
		if (!textMessage.contains("|")) {
			textMessage += "|";
		}
	}	
}