package rtsharecode.common.listeners;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;

import rtsharecode.common.Activator;
import rtsharecode.common.communication.Connection;

public class ProjectResourceChangeListener implements IResourceChangeListener {

	private IProject project;
	
	public ProjectResourceChangeListener(IProject project) {
		super();
		this.project = project;
	}

	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		if ( project != null ) {
			if ( Connection.getInstance().isAdmin() ) {
				IResourceDelta rootDelta = event.getDelta();
		        IResourceDelta delta = rootDelta.findMember(new Path(project.getName() + "/") );
		        if (delta == null) 
		        	return;
		        IResourceDeltaVisitor deltaVisitor = new IResourceDeltaVisitor() {
					
					@Override
					public boolean visit(IResourceDelta delta) throws CoreException {
						if (delta.getKind() == IResourceDelta.ADDED || delta.getKind() == IResourceDelta.REMOVED) {
							Activator.getDefault().getManagerListener().addResourceChanges();
						}						
						return true;
					}
				};
				try {
					rootDelta.accept(deltaVisitor);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}
	}
}