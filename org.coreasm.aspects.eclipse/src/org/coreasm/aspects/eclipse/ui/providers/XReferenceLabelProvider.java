package org.coreasm.aspects.eclipse.ui.providers;

import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.ViewerCell;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.eclipse.swt.graphics.Image;

/**
 * @author Tobias
 *
 * Label provider for the tree of cross references
 */
public class XReferenceLabelProvider extends StyledCellLabelProvider {

	@Override
	public void update(ViewerCell cell) {
	  Object obj = cell.getElement();
	  
	  if (obj instanceof TreeObject) {
		  String icon = ((TreeObject) obj).getIcon();
		  if (icon != null)
			  cell.setImage(getImage(icon));
		  
		  cell.setText(obj.toString());
	  }
	  
	  super.update(cell);
	}
	
	/**
	 * @param icon	Path to the icon location
	 * @return		Image
	 */
	public Image getImage(String icon)
	{
		Bundle bundle = FrameworkUtil.getBundle(XReferenceLabelProvider.class);
	    URL url = FileLocator.find(bundle, new Path("icons/" + icon), null);
	    ImageDescriptor imageDcr = ImageDescriptor.createFromURL(url);
	    return imageDcr.createImage();
	}
}
