package ru.myx.ae3.i3.web;

import java.io.File;

import ru.myx.ae3.Engine;
import ru.myx.ae3.i3.TargetInterfaceAbstract;
import ru.myx.ae3.l2.skin.Skin;
import ru.myx.ae3.l2.skin.SkinImpl;
import ru.myx.ae3.vfs.Entry;

/** @author myx */
public abstract class WebTargetAbstract //
		extends
			TargetInterfaceAbstract //
		implements
			WebTargetActor //
{
	
	/**
	 *
	 */
	protected static final Skin SKIN_WEB_ABSTRACT;
	
	static {
		SKIN_WEB_ABSTRACT = new SkinImpl(
				Skin.SKIN_STANDARD, //
				new File(
						Engine.PATH_PUBLIC, //
						"resources/skin/skin-web-abstract"));
	}
	
	/** @param root
	 */
	protected WebTargetAbstract(final Entry root) {
		
		super(root);
	}

	@Override
	public abstract String getTargetId();
	
}
