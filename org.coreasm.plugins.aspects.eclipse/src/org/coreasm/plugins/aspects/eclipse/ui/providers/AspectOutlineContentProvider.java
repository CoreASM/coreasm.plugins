package org.coreasm.plugins.aspects.eclipse.ui.providers;

import java.net.URL;

import org.coreasm.plugins.aspects.pointcutmatching.AdviceASTNode;
import org.coreasm.plugins.aspects.pointcutmatching.AspectASTNode;
import org.coreasm.plugins.aspects.pointcutmatching.NamedPointCutDefinitionASTNode;
import org.coreasm.eclipse.util.OutlineContentProvider;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.osgi.framework.FrameworkUtil;

public class AspectOutlineContentProvider implements OutlineContentProvider {

	@Override
	public URL getImage(String grammarRule) {
		if (AspectASTNode.NODE_TYPE.equals(grammarRule))
			return FileLocator.find(FrameworkUtil.getBundle(getClass()), new Path("icons/aspect.gif"), null);
		if (AdviceASTNode.NODE_TYPE.equals(grammarRule))
			return FileLocator.find(FrameworkUtil.getBundle(getClass()), new Path("icons/advice.gif"), null);
		if (NamedPointCutDefinitionASTNode.NODE_TYPE.equals(grammarRule))
			return FileLocator.find(FrameworkUtil.getBundle(getClass()), new Path("icons/pointcut_def.gif"), null);
		return null;
	}

	@Override
	public URL getGroupImage(String group) {
		return null;
	}

	@Override
	public String getGroup(String grammarRule) {
		if (AspectASTNode.NODE_TYPE.equals(grammarRule))
			return "Aspects";
		if (AdviceASTNode.NODE_TYPE.equals(grammarRule))
			return "Advices";
		if (NamedPointCutDefinitionASTNode.NODE_TYPE.equals(grammarRule))
			return "Pointcuts";
		return null;
	}

	@Override
	public String getSuffix(String grammarRule, String description) {
		return null;
	}

	@Override
	public boolean hasDeclarations(String grammarRule) {
		return AspectASTNode.NODE_TYPE.equals(grammarRule);
	}
}
