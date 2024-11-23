package ru.myx.ae3.i3.web;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

class RuntimeTarget {
	
	final String name;
	
	final Reference<WebTarget> reference;
	
	RuntimeTarget(final String name, final WebTarget target, final ReferenceQueue<WebTarget> queue) {
		this.name = name;
		this.reference = new WeakReference<>(target, queue);
	}
}
